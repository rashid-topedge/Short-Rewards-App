package com.dramflix.android.data.repo

import android.content.Context
import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.local.AppDatabase
import com.dramflix.android.data.local.dao.DramaDao
import com.dramflix.android.data.local.toDomain
import com.dramflix.android.data.local.toEntity
import com.dramflix.android.data.model.DramaCard
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.DramaListResponse
import com.dramflix.android.data.model.EpisodeItem
import com.dramflix.android.data.model.EpisodeListResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Offline-first repository for dramas, details, and episodes.
 *
 * **Strategy – Stale-While-Revalidate:**
 * 1. Room Flow emits cached data immediately (instant first render).
 * 2. A background coroutine fetches fresh data from the API.
 * 3. The API response is written back to Room; the Flow auto-pushes the update.
 */
class DramaRepository private constructor(
    private val dramaApi: DramaApi,
    private val dramaDao: DramaDao,
) {
    // ─── Drama list (catalog) ───────────────────────────────────

    /**
     * Observe the drama card list from Room.
     * Call [refreshDramas] to trigger a network fetch that writes back to Room.
     */
    fun observeDramas(query: String = ""): Flow<List<DramaCard>> {
        val tag = query.ifBlank { "" }
        return dramaDao.observeDramas(tag).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Force a network fetch, cache to Room, and let the Flow auto-update.
     * Returns the [DramaListResponse] so callers can read `nextCursor`.
     */
    suspend fun refreshDramas(
        query: String = "",
        cursor: String? = null,
        limit: Int = 24,
        append: Boolean = false,
    ): DramaListResponse {
        val tag = query.ifBlank { "" }
        val response = dramaApi.getDramas(limit, cursor, query.takeIf { it.isNotBlank() })

        val existingCount = if (append) dramaDao.getDramas(tag).size else 0
        if (!append) {
            dramaDao.clearCardsByTag(tag)
        }
        dramaDao.upsertCards(
            response.items.mapIndexed { i, card ->
                card.toEntity(tag, existingCount + i)
            },
        )
        return response
    }

    // ─── Drama detail ───────────────────────────────────────────

    /**
     * Observe a single drama's detail from Room.
     * Call [refreshDetail] to fetch fresh data from the API.
     */
    fun observeDetail(dramaId: Int): Flow<DramaDetail?> =
        dramaDao.observeDetail(dramaId).map { it?.toDomain() }

    /** Fetch detail from API and cache it in Room. */
    suspend fun refreshDetail(dramaId: Int): DramaDetail {
        val detail = dramaApi.getDrama(dramaId)
        dramaDao.upsertDetail(detail.toEntity())
        return detail
    }

    /**
     * Backward-compatible: get a drama detail (tries network, falls back to cache).
     */
    suspend fun getDrama(dramaId: Int): DramaDetail {
        return try {
            refreshDetail(dramaId)
        } catch (e: Exception) {
            dramaDao.observeDetail(dramaId)
                .map { it?.toDomain() }
                .let { flow ->
                    var cached: DramaDetail? = null
                    flow.collect { cached = it; return@collect }
                    cached ?: throw e
                }
        }
    }

    // ─── Episodes ───────────────────────────────────────────────

    /**
     * Observe episodes for a drama from Room.
     * Call [refreshEpisodes] to fetch fresh data from the API.
     */
    fun observeEpisodes(dramaId: Int): Flow<List<EpisodeItem>> =
        dramaDao.observeEpisodes(dramaId).map { entities ->
            entities.map { it.toDomain() }
        }

    /** Fetch episodes from API and cache them in Room. */
    suspend fun refreshEpisodes(dramaId: Int): EpisodeListResponse {
        val response = dramaApi.getEpisodes(dramaId)
        dramaDao.upsertEpisodes(
            response.episodes.map { it.toEntity(dramaId) },
        )
        return response
    }

    /**
     * Backward-compatible: get episode list (tries network, falls back to cache).
     */
    suspend fun listEpisodes(dramaId: Int): EpisodeListResponse {
        return try {
            refreshEpisodes(dramaId)
        } catch (_: Exception) {
            val cached = dramaDao.observeEpisodes(dramaId)
                .map { list -> list.map { it.toDomain() } }
            var result: List<EpisodeItem> = emptyList()
            cached.collect { result = it; return@collect }
            EpisodeListResponse(dramaId = dramaId, episodes = result)
        }
    }

    companion object {
        @Volatile
        private var instance: DramaRepository? = null

        fun getInstance(
            dramaApi: DramaApi = DramaApi.getInstance(),
            context: Context? = null,
        ): DramaRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val dao = context?.let { AppDatabase.getInstance(it).dramaDao() }
                        ?: throw IllegalStateException(
                            "DramaRepository.getInstance() requires a Context on first call",
                        )
                    DramaRepository(dramaApi, dao).also { instance = it }
                }
            }
        }
    }
}
