package com.dramflix.android.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "episodes",
    primaryKeys = ["dramaId", "ep"],
)
data class EpisodeEntity(
    val dramaId: Int,
    val ep: Int,
    val playUrl: String?,
    val isPublished: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)
