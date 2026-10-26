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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

interface DramaApi {
    suspend fun getDramas(limit: Int, cursor: String?, query: String?): DramaListResponse
    suspend fun getDrama(dramaId: Int): DramaDetail
    suspend fun getEpisodes(dramaId: Int): EpisodeListResponse
    suspend fun getReels(limit: Int, cursor: String?, dramaId: Int?, sort: String?): ReelListResponse
    suspend fun getReel(reelId: Int): ReelItem
    suspend fun reelAction(reelId: Int, body: ReelActionRequest): ApiActionResult
    suspend fun syncUser(body: UserIdRequest): UserProfile
    suspend fun checkIn(body: UserIdRequest): CheckInResult
    suspend fun rewardAd(body: RewardAdRequest): RewardResult
    suspend fun rewardEpisode(body: RewardEpisodeRequest): RewardResult
    suspend fun rewardReel(body: RewardReelRequest): RewardResult
    suspend fun listRedemptions(userId: String): RedemptionListResponse
    suspend fun createRedemption(body: CreateRedemptionRequest): RedemptionCreateResponse
    suspend fun getRedemptionSettings(): SystemSettingsDto

    companion object {
        fun getInstance(): DramaApi = NetworkModule.dramaApi
    }
}

class DramaApiImpl(private val client: HttpClient) : DramaApi {
    override suspend fun getDramas(limit: Int, cursor: String?, query: String?): DramaListResponse {
        return client.get("api/dramas") {
            parameter("limit", limit)
            parameter("cursor", cursor)
            parameter("q", query)
        }.body()
    }

    override suspend fun getDrama(dramaId: Int): DramaDetail {
        return client.get("api/dramas/$dramaId").body()
    }

    override suspend fun getEpisodes(dramaId: Int): EpisodeListResponse {
        return client.get("api/dramas/$dramaId/episodes").body()
    }

    override suspend fun getReels(limit: Int, cursor: String?, dramaId: Int?, sort: String?): ReelListResponse {
        return client.get("api/reels") {
            parameter("limit", limit)
            parameter("cursor", cursor)
            parameter("dramaId", dramaId)
            parameter("sort", sort)
        }.body()
    }

    override suspend fun getReel(reelId: Int): ReelItem {
        return client.get("api/reels/$reelId").body()
    }

    override suspend fun reelAction(reelId: Int, body: ReelActionRequest): ApiActionResult {
        return client.post("api/reels/$reelId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    override suspend fun syncUser(body: UserIdRequest): UserProfile {
        return client.post("api/user/sync") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    override suspend fun checkIn(body: UserIdRequest): CheckInResult {
        return client.post("api/user/checkin") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    override suspend fun rewardAd(body: RewardAdRequest): RewardResult {
        return client.post("api/user/reward/ad") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    override suspend fun rewardEpisode(body: RewardEpisodeRequest): RewardResult {
        return client.post("api/user/reward/episode") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    override suspend fun rewardReel(body: RewardReelRequest): RewardResult {
        return client.post("api/user/reward/reel") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    override suspend fun listRedemptions(userId: String): RedemptionListResponse {
        return client.get("api/user/redeem") {
            parameter("userId", userId)
        }.body()
    }

    override suspend fun createRedemption(body: CreateRedemptionRequest): RedemptionCreateResponse {
        return client.post("api/user/redeem") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    override suspend fun getRedemptionSettings(): SystemSettingsDto {
        return client.get("api/user/redeem/settings").body()
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
