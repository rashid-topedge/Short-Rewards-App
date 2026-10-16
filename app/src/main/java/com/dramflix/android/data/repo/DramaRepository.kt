package com.dramflix.android.data.repo

import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.DramaListResponse
import com.dramflix.android.data.model.EpisodeListResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DramaRepository private constructor(
    private val dramaApi: DramaApi,
) {
    companion object {
        @Volatile
        private var instance: DramaRepository? = null

        @JvmOverloads
        fun getInstance(dramaApi: DramaApi = DramaApi.getInstance()): DramaRepository {
            return instance ?: synchronized(this) {
                instance ?: DramaRepository(dramaApi).also { instance = it }
            }
        }
    }

    fun listDramas(
        cursor: String? = null,
        limit: Int = 24,
        query: String = "",
    ): Flow<DramaListResponse> = flow {
        emit(dramaApi.getDramas(limit, cursor, query.takeIf { it.isNotBlank() }))
    }

    suspend fun getDrama(dramaId: Int): DramaDetail = dramaApi.getDrama(dramaId)

    suspend fun listEpisodes(dramaId: Int): EpisodeListResponse = dramaApi.getEpisodes(dramaId)
}
