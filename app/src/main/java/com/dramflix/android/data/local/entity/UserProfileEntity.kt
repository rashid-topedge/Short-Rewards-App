package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val coins: Int,
    val streakDay: Int,
    val lastCheckInDate: String?,
    val canCheckInToday: Boolean,
    val nextCheckInReward: Int,
    val streakScheduleJson: String,
    val transactionsJson: String,
    val googleLinked: Boolean = false,
    val googleEmail: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)
