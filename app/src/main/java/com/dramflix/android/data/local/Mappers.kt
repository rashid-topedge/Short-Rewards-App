package com.dramflix.android.data.local

import com.dramflix.android.data.local.entity.DramaCardEntity
import com.dramflix.android.data.local.entity.DramaDetailEntity
import com.dramflix.android.data.local.entity.EpisodeEntity
import com.dramflix.android.data.local.entity.ReelEntity
import com.dramflix.android.data.local.entity.UserProfileEntity
import com.dramflix.android.data.model.CoinTransaction
import com.dramflix.android.data.model.DramaCard
import com.dramflix.android.data.model.DramaDetail
import com.dramflix.android.data.model.EpisodeItem
import com.dramflix.android.data.model.ReelItem
import com.dramflix.android.data.model.StreakDayInfo
import com.dramflix.android.data.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Shared Json instance for entity ↔ domain mapping of embedded list fields.
 * Must match the lenient config used by the network layer.
 */
private val mapperJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// ─── DramaCard ─────────────────────────────────────────────

fun DramaCard.toEntity(queryTag: String = "", sortOrder: Int = 0): DramaCardEntity =
    DramaCardEntity(
        dramaId = dramaId,
        slug = slug,
        title = title,
        cover = cover,
        playCount = playCount,
        episodeCount = episodeCount,
        latestEpisodeLabel = latestEpisodeLabel,
        isPublished = isPublished,
        queryTag = queryTag,
        sortOrder = sortOrder,
    )

fun DramaCardEntity.toDomain(): DramaCard =
    DramaCard(
        dramaId = dramaId,
        slug = slug,
        title = title,
        cover = cover,
        playCount = playCount,
        episodeCount = episodeCount,
        latestEpisodeLabel = latestEpisodeLabel,
        isPublished = isPublished,
    )

// ─── DramaDetail ───────────────────────────────────────────

fun DramaDetail.toEntity(): DramaDetailEntity =
    DramaDetailEntity(
        dramaId = dramaId,
        slug = slug,
        title = title,
        cover = cover,
        synopsis = synopsis,
        tagsJson = mapperJson.encodeToString(tags),
        playCount = playCount,
        likeCount = likeCount,
        episodeCount = episodeCount,
        latestEpisodeLabel = latestEpisodeLabel,
        isPublished = isPublished,
    )

fun DramaDetailEntity.toDomain(): DramaDetail =
    DramaDetail(
        dramaId = dramaId,
        slug = slug,
        title = title,
        cover = cover,
        synopsis = synopsis,
        tags = runCatching { mapperJson.decodeFromString<List<String>>(tagsJson) }
            .getOrDefault(emptyList()),
        playCount = playCount,
        likeCount = likeCount,
        episodeCount = episodeCount,
        latestEpisodeLabel = latestEpisodeLabel,
        isPublished = isPublished,
    )

// ─── Episode ───────────────────────────────────────────────

fun EpisodeItem.toEntity(dramaId: Int): EpisodeEntity =
    EpisodeEntity(
        dramaId = dramaId,
        ep = ep,
        playUrl = playUrl,
        isPublished = isPublished,
    )

fun EpisodeEntity.toDomain(): EpisodeItem =
    EpisodeItem(
        ep = ep,
        playUrl = playUrl,
        isPublished = isPublished,
    )

// ─── Reel ──────────────────────────────────────────────────

fun ReelItem.toEntity(sortTag: String = "newest", sortOrder: Int = 0): ReelEntity =
    ReelEntity(
        reelId = reelId,
        title = title,
        synopsis = synopsis,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        dramaId = dramaId,
        dramaTitle = dramaTitle,
        episodeNumber = episodeNumber,
        tagsJson = mapperJson.encodeToString(tags),
        viewCount = viewCount,
        likeCount = likeCount,
        isPublished = isPublished,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sortTag = sortTag,
        sortOrder = sortOrder,
    )

fun ReelEntity.toDomain(): ReelItem =
    ReelItem(
        reelId = reelId,
        title = title,
        synopsis = synopsis,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        dramaId = dramaId,
        dramaTitle = dramaTitle,
        episodeNumber = episodeNumber,
        tags = runCatching { mapperJson.decodeFromString<List<String>>(tagsJson) }
            .getOrDefault(emptyList()),
        viewCount = viewCount,
        likeCount = likeCount,
        isPublished = isPublished,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

// ─── UserProfile ───────────────────────────────────────────

fun UserProfile.toEntity(): UserProfileEntity =
    UserProfileEntity(
        userId = userId,
        coins = coins,
        streakDay = streakDay,
        lastCheckInDate = lastCheckInDate,
        canCheckInToday = canCheckInToday,
        nextCheckInReward = nextCheckInReward,
        streakScheduleJson = mapperJson.encodeToString(streakSchedule),
        transactionsJson = mapperJson.encodeToString(transactions),
    )

fun UserProfileEntity.toDomain(): UserProfile =
    UserProfile(
        userId = userId,
        coins = coins,
        streakDay = streakDay,
        lastCheckInDate = lastCheckInDate,
        canCheckInToday = canCheckInToday,
        nextCheckInReward = nextCheckInReward,
        streakSchedule = runCatching {
            mapperJson.decodeFromString<List<StreakDayInfo>>(streakScheduleJson)
        }.getOrDefault(emptyList()),
        transactions = runCatching {
            mapperJson.decodeFromString<List<CoinTransaction>>(transactionsJson)
        }.getOrDefault(emptyList()),
    )
