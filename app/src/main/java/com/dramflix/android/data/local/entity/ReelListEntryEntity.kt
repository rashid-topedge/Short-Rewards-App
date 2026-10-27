package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/** Position of a reel inside one cached feed ([sortTag] = newest / trending / random). */
@Entity(
    tableName = "reel_list_entries",
    primaryKeys = ["sortTag", "reelId"],
    indices = [Index("reelId"), Index(value = ["sortTag", "position"])],
)
data class ReelListEntryEntity(
    val sortTag: String,
    val reelId: Int,
    val position: Int,
    val cachedAt: Long = System.currentTimeMillis(),
)
