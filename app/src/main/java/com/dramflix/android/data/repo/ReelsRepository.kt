package com.dramflix.android.data.repo

import android.content.Context
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.api.ReelActionRequest
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.local.dao.ReelDao
import com.dramflix.android.data.local.toDomain
import com.dramflix.android.data.local.toEntity
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.ReelListResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first repository for Reels.
 *
 * Same stale-while-revalidate strategy as [DramaRepository]:
 * Room Flow delivers the cache; [refreshReels] writes API data back to Room.
 */
class ReelsRepository private constructor(
    private val dramaApi: DramaApi,
    private val reelDao: ReelDao,
) {
    // ─── Reel list ──────────────────────────────────────────────

    /**
     * Observe the reel list from Room, filtered by [sort] tag.
     */
    fun observeReels(sort: ReelSort = ReelSort.Newest): Flow<List<ReelItem>> {
        val tag = sort.name.lowercase()
        return reelDao.observeReels(tag).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Fetch reels from the API, cache in Room.
     * Returns the [ReelListResponse] so callers can read `nextCursor`.
     */
    suspend fun refreshReels(
        sort: ReelSort = ReelSort.Newest,
        cursor: String? = null,
        limit: Int = DEFAULT_LIMIT,
        dramaId: Int? = null,
        append: Boolean = false,
    ): ReelListResponse {
        val tag = sort.name.lowercase()
        val response = dramaApi.getReels(limit, cursor, dramaId, tag)

        if (!append) {
            reelDao.clearBySortTag(tag)
        }
        val existingCount = if (append) {
            // Count current cached items so appended items get a higher sortOrder
            reelDao.observeReels(tag).let { flow ->
                var count = 0
                flow.collect { count = it.size; return@collect }
                count
            }
        } else {
            0
        }
        reelDao.upsertAll(
            response.items.mapIndexed { i, reel ->
                reel.toEntity(tag, existingCount + i)
            },
        )
        return response
    }

    // ─── Single reel ────────────────────────────────────────────

    /** Observe a single reel from Room. */
    fun observeReel(reelId: Int): Flow<ReelItem?> =
        reelDao.observeReel(reelId).map { it?.toDomain() }

    /**
     * Backward-compatible: get a single reel (network-first, cache fallback).
     */
    suspend fun getReel(reelId: Int): ReelItem {
        return try {
            val reel = dramaApi.getReel(reelId)
            reelDao.upsert(reel.toEntity())
            reel
        } catch (e: Exception) {
            reelDao.getReel(reelId)?.toDomain() ?: throw e
        }
    }

    // ─── Actions ────────────────────────────────────────────────

    /**
     * Record a view for a reel.
     * Increments the view count in local Room DB and syncs with the remote API.
     */
    suspend fun recordView(reelId: Int): Boolean {
        reelDao.incrementViewCount(reelId)
        return try {
            val result = dramaApi.reelAction(reelId, ReelActionRequest(action = "view"))
            result.success
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Toggle like state for a reel.
     * Updates like count in local Room DB and syncs with the remote API.
     */
    suspend fun toggleLike(reelId: Int, isLiked: Boolean): Boolean {
        reelDao.updateLikeCount(reelId, increment = isLiked)
        return try {
            val result = dramaApi.reelAction(reelId, ReelActionRequest(action = if (isLiked) "like" else "unlike"))
            result.success
        } catch (e: Exception) {
            false
        }
    }

    suspend fun likeReel(reelId: Int): Boolean {
        return toggleLike(reelId, isLiked = true)
    }

    companion object {
        private const val DEFAULT_LIMIT = 20

        @Volatile
        private var instance: ReelsRepository? = null

        fun getInstance(
            dramaApi: DramaApi = DramaApi.getInstance(),
            context: Context? = null,
        ): ReelsRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val dao = context?.let { AppDatabase.getInstance(it).reelDao() }
                        ?: throw IllegalStateException(
                            "ReelsRepository.getInstance() requires a Context on first call",
                        )
                    ReelsRepository(dramaApi, dao).also { instance = it }
                }
            }
        }
    }
}

enum class ReelSort {
    Newest,
    Trending,
    Random,
}
