package com.dramflix.android.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.dramflix.android.data.AppContainer
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.local.entity.DramaListEntryEntity
import com.dramflix.android.data.local.toDomain
import com.dramflix.android.data.local.toEntity
import com.dramflix.android.data.model.DramaCard
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.DramaListResponse
import com.dramflix.android.data.model.EpisodeItem
import com.dramflix.android.data.model.EpisodeListResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Offline-first repository for dramas, details, and episodes.
 *
 * **Strategy – Stale-While-Revalidate:**
 * 1. Room Flow emits cached data immediately (instant first render).
 * 2. A background coroutine fetches fresh data from the API.
 * 3. The API response is written back to Room in one transaction; the Flow
 *    auto-pushes a single, consistent update (no empty flash between
 *    "clear" and "insert").
 */
class DramaRepository private constructor(
    private val dramaApi: DramaApi,
    private val db: AppDatabase,
) {
    private val dramaDao = db.dramaDao()

    /** One lock per list so a refresh and a "load more" can't interleave writes. */
    private val listLocks = ConcurrentHashMap<String, Mutex>()

    private fun lockFor(tag: String) = listLocks.getOrPut(tag) { Mutex() }

    // ─── Drama list (catalog) ───────────────────────────────────

    /**
     * Observe the drama card list from Room.
     * Call [refreshDramas] to trigger a network fetch that writes back to Room.
     */
    fun observeDramas(query: String = ""): Flow<List<DramaCard>> =
        dramaDao.observeDramas(tagFor(query)).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Fetch one page from the API and cache it.
     * - `append = false` replaces the cached list (first page / pull-to-refresh).
     * - `append = true` adds the page after the existing items (load more).
     * Returns the [DramaListResponse] so callers can read `nextCursor`.
     */
    suspend fun refreshDramas(
        query: String = "",
        cursor: String? = null,
        limit: Int = PAGE_SIZE,
        append: Boolean = false,
    ): DramaListResponse {
        val tag = tagFor(query)
        val response = dramaApi.getDramas(limit, cursor, tag.ifBlank { null })

        lockFor(tag).withLock {
            db.withTransaction {
                if (!append) dramaDao.clearList(tag)
                val start = if (append) dramaDao.nextPosition(tag) else 0
                val now = System.currentTimeMillis()
                dramaDao.upsertCards(response.items.map { it.toEntity() })
                dramaDao.upsertListEntries(
                    response.items.mapIndexed { i, card ->
                        DramaListEntryEntity(
                            queryTag = tag,
                            dramaId = card.dramaId,
                            position = start + i,
                            cachedAt = now,
                        )
                    },
                )
            }
        }
        if (!append && tag.isNotEmpty()) pruneCache()
        return response
    }

    // ─── Drama detail ───────────────────────────────────────────

    fun observeDetail(dramaId: Int): Flow<DramaDetail?> =
        dramaDao.observeDetail(dramaId).map { it?.toDomain() }

    /** Fetch detail from API and cache it in Room. */
    suspend fun refreshDetail(dramaId: Int): DramaDetail {
        val detail = dramaApi.getDrama(dramaId)
        dramaDao.upsertDetail(detail.toEntity())
        return detail
    }

    /** Network-first, falls back to the cached copy when the request fails. */
    suspend fun getDrama(dramaId: Int): DramaDetail {
        return try {
            refreshDetail(dramaId)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Previously this collected an infinite Room Flow and never returned
            // (the player hung forever when offline). A one-shot query fixes it.
            dramaDao.getDetail(dramaId)?.toDomain() ?: throw e
        }
    }

    // ─── Episodes ───────────────────────────────────────────────

    fun observeEpisodes(dramaId: Int): Flow<List<EpisodeItem>> =
        dramaDao.observeEpisodes(dramaId).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Fetch episodes from the API and replace the cached set, so episodes the
     * server removed or unpublished don't linger in the cache forever.
     */
    suspend fun refreshEpisodes(dramaId: Int): EpisodeListResponse {
        val response = dramaApi.getEpisodes(dramaId)
        db.withTransaction {
            dramaDao.clearEpisodes(dramaId)
            dramaDao.upsertEpisodes(response.episodes.map { it.toEntity(dramaId) })
        }
        return response
    }

    /** Network-first episode list with cache fallback. */
    suspend fun listEpisodes(dramaId: Int): EpisodeListResponse {
        return try {
            refreshEpisodes(dramaId)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val cached = dramaDao.getEpisodes(dramaId).map { it.toDomain() }
            if (cached.isEmpty()) throw e
            EpisodeListResponse(dramaId = dramaId, episodes = cached)
        }
    }

    // ─── Housekeeping ───────────────────────────────────────────

    /**
     * Bounds the cache: keeps the last [MAX_CACHED_SEARCHES] search lists,
     * drops cards no list references, and details/episodes older than
     * [DETAIL_TTL_MS]. Without this every distinct search grew the DB forever.
     */
    suspend fun pruneCache() {
        try {
            db.withTransaction {
                dramaDao.pruneSearchLists(MAX_CACHED_SEARCHES)
                dramaDao.deleteOrphanCards()
                dramaDao.deleteDetailsOlderThan(System.currentTimeMillis() - DETAIL_TTL_MS)
                dramaDao.deleteOrphanEpisodes()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Housekeeping must never break a user-facing flow.
        }
    }

    /** Cache key for a list. Kept identical to what's sent as `q` so results match the server. */
    private fun tagFor(query: String) = query.trim()

    companion object {
        const val PAGE_SIZE = 24
        private const val MAX_CACHED_SEARCHES = 10
        private val DETAIL_TTL_MS = TimeUnit.DAYS.toMillis(14)

        @Volatile
        private var instance: DramaRepository? = null

        fun getInstance(
            dramaApi: DramaApi = AppContainer.api,
            context: Context? = null,
        ): DramaRepository {
            return instance ?: synchronized(this) {
                instance ?: DramaRepository(
                    dramaApi = dramaApi,
                    db = context?.let { AppDatabase.getInstance(it) } ?: AppContainer.database,
                ).also { instance = it }
            }
        }
    }
}
