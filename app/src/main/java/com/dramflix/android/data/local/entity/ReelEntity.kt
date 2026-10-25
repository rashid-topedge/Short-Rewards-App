package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey val reelId: Int,
    val title: String,
    val synopsis: String?,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int,
    val dramaId: Int?,
    val dramaTitle: String?,
    val episodeNumber: Int?,
    val tagsJson: String,
    val viewCount: Int,
    val likeCount: Int,
    val isPublished: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val cachedAt: Long = System.currentTimeMillis(),
    val sortTag: String = "newest",
    val sortOrder: Int = 0,
)
