package com.dramflix.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DramaCard(
    val dramaId: Int,
    val slug: String,
    val title: String?,
    val cover: String?,
    val playCount: Int,
    val episodeCount: Int,
    val latestEpisodeLabel: String?,
    val isPublished: Boolean = false,
)

@Serializable
data class DramaDetail(
    val dramaId: Int,
    val slug: String = "",
    val title: String? = null,
    val cover: String? = null,
    val synopsis: String? = null,
    val tags: List<String> = emptyList(),
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val episodeCount: Int = 0,
    val latestEpisodeLabel: String? = null,
    val isPublished: Boolean = false,
)

@Serializable
data class EpisodeItem(
    val ep: Int,
    val playUrl: String?,
    val isPublished: Boolean = false,
)

@Serializable
data class ReelItem(
    val reelId: Int,
    val title: String,
    val synopsis: String? = null,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int,
    val dramaId: Int? = null,
    val dramaTitle: String? = null,
    val episodeNumber: Int? = null,
    val tags: List<String> = emptyList(),
    val viewCount: Int,
    val likeCount: Int,
    val isPublished: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ReelListResponse(
    val limit: Int,
    val nextCursor: String?,
    val total: Int? = null,
    val items: List<ReelItem>,
)

@Serializable
data class CoinTransaction(
    val id: Int,
    val userId: String,
    val type: String,
    val amount: Int,
    val description: String,
    val createdAt: Long,
)

@Serializable
data class StreakDayInfo(
    val day: Int,
    val coins: Int,
    val status: String,
)

@Serializable
data class UserProfile(
    val userId: String,
    val coins: Int,
    val streakDay: Int,
    val lastCheckInDate: String?,
    val canCheckInToday: Boolean,
    val nextCheckInReward: Int,
    val streakSchedule: List<StreakDayInfo>,
    val transactions: List<CoinTransaction>,
)

@Serializable
data class CheckInResult(
    val success: Boolean,
    val message: String,
    val coinsEarned: Int,
    val newCoinsBalance: Int,
    val streakDay: Int,
    val isCycleReset: Boolean,
    val user: UserProfile,
)

@Serializable
data class RewardResult(
    val success: Boolean,
    val message: String,
    val coinsEarned: Int,
    val newCoinsBalance: Int,
    val user: UserProfile,
)

@Serializable
data class RedemptionRequest(
    val id: Int,
    val userId: String,
    val coinsAmount: Int,
    val exchangeRate: Double,
    val pkrAmount: Double,
    val paymentMethod: String,
    val accountTitle: String,
    val accountNumber: String,
    val bankName: String? = null,
    val status: String,
    val transactionRef: String? = null,
    val receiptUrl: String? = null,
    val adminNotes: String? = null,
    val createdAt: Long,
    val processedAt: Long? = null,
)

@Serializable
data class SystemSettingsDto(
    val coinToPkrRate: Double,
    val minRedeemCoins: Int,
    val minRedeemPkr: Double,
    val supportedPaymentMethods: List<String>,
)

@Serializable
data class CreateRedemptionRequest(
    val userId: String,
    val coinsAmount: Int,
    val paymentMethod: String,
    val accountTitle: String,
    val accountNumber: String,
    val bankName: String? = null,
)

@Serializable
data class DramaListResponse(
    val limit: Int = 24,
    val nextCursor: String? = null,
    val items: List<DramaCard> = emptyList(),
)

@Serializable
data class EpisodeListResponse(
    val dramaId: Int = 0,
    val episodes: List<EpisodeItem> = emptyList(),
)

@Serializable
data class RedemptionListResponse(
    val items: List<RedemptionRequest> = emptyList(),
)

@Serializable
data class RedemptionCreateResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val redemption: RedemptionRequest? = null,
    val user: UserProfile? = null,
)
