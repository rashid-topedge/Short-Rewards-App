package com.dramflix.android.data.repo

import com.dramflix.android.data.api.DramaApi
import com.dramflix.android.data.api.ReelActionRequest
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.ReelListResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReelsRepository private constructor(
    private val dramaApi: DramaApi,
) {
    companion object {
        private const val DEFAULT_LIMIT = 20

        @Volatile
        private var instance: ReelsRepository? = null

        @JvmOverloads
        fun getInstance(dramaApi: DramaApi = DramaApi.getInstance()): ReelsRepository {
            return instance ?: synchronized(this) {
                instance ?: ReelsRepository(dramaApi).also { instance = it }
            }
        }
    }

    fun listReels(
        cursor: String? = null,
        limit: Int = DEFAULT_LIMIT,
        dramaId: Int? = null,
        sort: ReelSort = ReelSort.Newest,
    ): Flow<ReelListResponse> = flow {
        emit(dramaApi.getReels(limit, cursor, dramaId, sort.name.lowercase()))
    }

    suspend fun getReel(reelId: Int): ReelItem = dramaApi.getReel(reelId)

    suspend fun likeReel(reelId: Int): Boolean {
        val result = dramaApi.reelAction(reelId, ReelActionRequest(action = "like"))
        return result.success
    }
}

enum class ReelSort {
    Newest,
    Trending,
    Random,
}
