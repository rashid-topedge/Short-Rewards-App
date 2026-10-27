package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Reels this device's user has liked, so the heart state survives restarts. */
@Entity(tableName = "liked_reels")
data class LikedReelEntity(
    @PrimaryKey val reelId: Int,
    val likedAt: Long = System.currentTimeMillis(),
)
