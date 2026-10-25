package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drama_details")
data class DramaDetailEntity(
    @PrimaryKey val dramaId: Int,
    val slug: String,
    val title: String?,
    val cover: String?,
    val synopsis: String?,
    val tagsJson: String,
    val playCount: Int,
    val likeCount: Int,
    val episodeCount: Int,
    val latestEpisodeLabel: String?,
    val isPublished: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)
