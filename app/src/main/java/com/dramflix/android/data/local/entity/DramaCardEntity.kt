package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val queryTag: String = "",
    val sortOrder: Int = 0,
)
