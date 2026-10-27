package com.dramflix.android.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.dramflix.android.data.AppContainer
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.api.ReelActionRequest
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.local.entity.LikedReelEntity
import com.dramflix.android.data.local.entity.ReelListEntryEntity
import com.dramflix.android.data.local.toDomain
import com.dramflix.android.data.local.toEntity
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.ReelListResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Offline-first repository for Reels.
 *
 * Same stale-while-revalidate strategy as [DramaRepository]:
 * Room Flow delivers the cache; [refreshReels] writes API data back to Room.
 */
class ReelsRepository private constructor(
    private val dramaApi: DramaApi,
    private val db: AppDatabase,
) {
    private val reelDao = db.reelDao()

    private val feedLocks = ConcurrentHashMap<String, Mutex>()
    private val likeLocks = ConcurrentHashMap<Int, Mutex>()

    // ─── Reel list ──────────────────────────────────────────────

    /** Observe one feed from Room, in server order. */
    fun observeReels(sort: ReelSort = ReelSort.Newest): Flow<List<ReelItem>> =
        reelDao.observeReels(sort.tag).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Fetch one page from the API and cache it.
     * Returns the [ReelListResponse] so callers can read `nextCursor`.
     *
     * Previously the `append = true` path collected an infinite Room Flow to
     * count rows and never returned, so "load more" on the Reels tab hung
     * after the first page. It now uses a one-shot position query.
     */
    suspend fun refreshReels(
        sort: ReelSort = ReelSort.Newest,
        cursor: String? = null,
        limit: Int = DEFAULT_LIMIT,
        dramaId: Int? = null,
        append: Boolean = false,
    ): ReelListResponse {
        val tag = sort.tag
        val response = dramaApi.getReels(limit, cursor, dramaId, tag)

        // Feeds filtered by drama aren't cached as a feed (they'd overwrite the main one).
        if (dramaId != null) {
            reelDao.upsertAll(response.items.map { it.toEntity() })
            return response
        }

        feedLocks.getOrPut(tag) { Mutex() }.withLock {
            db.withTransaction {
                if (!append) reelDao.clearList(tag)
                val start = if (append) reelDao.nextPosition(tag) else 0
                val now = System.currentTimeMillis()
                reelDao.upsertAll(response.items.map { it.toEntity() })
                reelDao.upsertListEntries(
                    response.items.mapIndexed { i, reel ->
                        ReelListEntryEntity(
                            sortTag = tag,
                            reelId = reel.reelId,
                            position = start + i,
                            cachedAt = now,
                        )
                    },
                )
            }
        }
        if (!append) pruneCache()
        return response
    }

    // ─── Single reel ────────────────────────────────────────────

    fun observeReel(reelId: Int): Flow<ReelItem?> =
        reelDao.observeReel(reelId).map { it?.toDomain() }

    /** Network-first, cache fallback. */
    suspend fun getReel(reelId: Int): ReelItem {
        return try {
            val reel = dramaApi.getReel(reelId)
            reelDao.upsert(reel.toEntity())
            reel
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reelDao.getReel(reelId)?.toDomain() ?: throw e
        }
    }

    // ─── Likes ──────────────────────────────────────────────────

    /** IDs of reels liked on this device. */
    fun observeLikedIds(): Flow<Set<Int>> =
        reelDao.observeLikedIds().map { it.toSet() }.distinctUntilChanged()

    fun observeIsLiked(reelId: Int): Flow<Boolean> =
        reelDao.observeIsLiked(reelId).distinctUntilChanged()

    /**
     * Set the like state for a reel. Optimistic: Room (heart + count) updates
     * immediately, the API call follows, and the local change is rolled back
     * if the server rejects it. Calls for the same reel are serialised so rapid
     * taps can't leave the count out of sync with the server.
     *
     * @return true if the server accepted the change (or nothing needed to change).
     */
    suspend fun setLiked(reelId: Int, liked: Boolean): Boolean {
        return likeLocks.getOrPut(reelId) { Mutex() }.withLock {
            val current = reelDao.isLiked(reelId)
            if (current == liked) return@withLock true

            applyLocalLike(reelId, liked)
            val ok = try {
                dramaApi.reelAction(
                    reelId,
                    ReelActionRequest(action = if (liked) "like" else "unlike"),
                ).success
            } catch (e: Exception) {
                // NonCancellable so the rollback still runs if the screen closed mid-request.
                withContext(NonCancellable) { applyLocalLike(reelId, current) }
                throw e
            }
            if (!ok) applyLocalLike(reelId, current)
            ok
        }
    }

    /** Kept for existing callers. */
    suspend fun toggleLike(reelId: Int, isLiked: Boolean): Boolean = setLiked(reelId, isLiked)

    suspend fun likeReel(reelId: Int): Boolean = setLiked(reelId, liked = true)

    private suspend fun applyLocalLike(reelId: Int, liked: Boolean) {
        db.withTransaction {
            if (liked) reelDao.markLiked(LikedReelEntity(reelId)) else reelDao.unmarkLiked(reelId)
            reelDao.updateLikeCount(reelId, increment = liked)
        }
    }

    // ─── Views ──────────────────────────────────────────────────

    /**
     * Record a view: bumps the cached count immediately and reports it to the
     * server. Best-effort – a failed view ping is not shown to the user.
     */
    suspend fun recordView(reelId: Int): Boolean {
        reelDao.incrementViewCount(reelId)
        return try {
            dramaApi.reelAction(reelId, ReelActionRequest(action = "view")).success
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    // ─── Housekeeping ───────────────────────────────────────────

    suspend fun pruneCache() {
        try {
            reelDao.deleteOrphanReels(System.currentTimeMillis() - ORPHAN_TTL_MS)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
        private val ORPHAN_TTL_MS = TimeUnit.DAYS.toMillis(3)

        @Volatile
        private var instance: ReelsRepository? = null

        fun getInstance(
            dramaApi: DramaApi = AppContainer.api,
            context: Context? = null,
        ): ReelsRepository {
            return instance ?: synchronized(this) {
                instance ?: ReelsRepository(
                    dramaApi = dramaApi,
                    db = context?.let { AppDatabase.getInstance(it) } ?: AppContainer.database,
                ).also { instance = it }
            }
        }
    }
}

enum class ReelSort {
    Newest,
    Trending,
    Random,
    ;

    /** Value sent as `sort=` and used as the cache key. */
    val tag: String get() = name.lowercase()
}
