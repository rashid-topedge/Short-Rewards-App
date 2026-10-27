package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Card content for one drama, stored once regardless of how many lists
 * (home feed, search results, genre filters) it appears in.
 * List membership/order lives in [DramaListEntryEntity].
 */
@Entity(tableName = "drama_cards")
data class DramaCardEntity(
    @PrimaryKey val dramaId: Int,
    val slug: String,
    val title: String?,
    val cover: String?,
    val playCount: Int,
    val episodeCount: Int,
    val latestEpisodeLabel: String?,
    val isPublished: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)
