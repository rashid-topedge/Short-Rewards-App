package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Position of a drama inside one cached list. [queryTag] is "" for the
 * home feed, otherwise the search/genre query.
 */
@Entity(
    tableName = "drama_list_entries",
    primaryKeys = ["queryTag", "dramaId"],
    indices = [Index("dramaId"), Index(value = ["queryTag", "position"])],
)
data class DramaListEntryEntity(
    val queryTag: String,
    val dramaId: Int,
    val position: Int,
    val cachedAt: Long = System.currentTimeMillis(),
)
