package com.dramflix.android.data.api

import com.dramflix.android.data.model.CheckInResult
import com.dramflix.android.data.model.CreateRedemptionRequest
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.EpisodeListResponse
import com.dramflix.android.data.model.DramaListResponse
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.ReelListResponse
import com.dramflix.android.data.model.RedemptionCreateResponse
import com.dramflix.android.data.model.RedemptionListResponse
import com.dramflix.android.data.model.RewardResult
import com.dramflix.android.data.model.SystemSettingsDto
import com.dramflix.android.data.model.UserProfile
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DramaApi {
    @GET("api/dramas")
    suspend fun getDramas(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
        @Query("q") query: String?,
    ): DramaListResponse

    @GET("api/dramas/{dramaId}")
    suspend fun getDrama(@Path("dramaId") dramaId: Int): DramaDetail

    @GET("api/dramas/{dramaId}/episodes")
    suspend fun getEpisodes(@Path("dramaId") dramaId: Int): EpisodeListResponse

    @GET("api/reels")
    suspend fun getReels(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
        @Query("dramaId") dramaId: Int?,
        @Query("sort") sort: String?,
    ): ReelListResponse

    @GET("api/reels/{reelId}")
    suspend fun getReel(@Path("reelId") reelId: Int): ReelItem

    @POST("api/reels/{reelId}")
    suspend fun reelAction(
        @Path("reelId") reelId: Int,
        @Body body: ReelActionRequest,
    ): ApiActionResult

    @POST("api/user/sync")
    suspend fun syncUser(@Body body: UserIdRequest): UserProfile

    @POST("api/user/checkin")
    suspend fun checkIn(@Body body: UserIdRequest): CheckInResult

    @POST("api/user/reward/ad")
    suspend fun rewardAd(@Body body: RewardAdRequest): RewardResult

    @POST("api/user/reward/episode")
    suspend fun rewardEpisode(@Body body: RewardEpisodeRequest): RewardResult

    @POST("api/user/reward/reel")
    suspend fun rewardReel(@Body body: RewardReelRequest): RewardResult

    @GET("api/user/redeem")
    suspend fun listRedemptions(@Query("userId") userId: String): RedemptionListResponse

    @POST("api/user/redeem")
    suspend fun createRedemption(@Body body: CreateRedemptionRequest): RedemptionCreateResponse

    @GET("api/user/redeem/settings")
    suspend fun getRedemptionSettings(): SystemSettingsDto

    companion object {
        fun getInstance(): DramaApi = NetworkModule.dramaApi
    }
}

@Serializable
data class UserIdRequest(
    val userId: String,
    val clientDate: String? = null,
)

@Serializable
data class RewardAdRequest(
    val userId: String,
    val adType: String = "REWARDED_VIDEO",
    val clientDate: String? = null,
)

@Serializable
data class RewardEpisodeRequest(
    val userId: String,
    val dramaId: Int,
    val ep: Int,
    val clientDate: String? = null,
)

@Serializable
data class RewardReelRequest(
    val userId: String,
    val reelId: Int,
    val clientDate: String? = null,
)

@Serializable
data class ReelActionRequest(
    val action: String,
)

@Serializable
data class ApiActionResult(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
