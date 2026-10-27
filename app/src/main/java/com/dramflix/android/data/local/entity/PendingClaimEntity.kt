package com.dramflix.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reward / check-in claim whose result we don't know yet (timeout, no
 * network, app killed). It is retried later with the **same** [claimId] so
 * the server can de-duplicate it, and deleted once the server answers.
 */
@Entity(
    tableName = "pending_claims",
    indices = [Index(value = ["type", "claimKey"])],
)
data class PendingClaimEntity(
    @PrimaryKey val claimId: String,
    /** CHECKIN, AD, EPISODE or REEL. */
    val type: String,
    /** What is being claimed: the date for a check-in, "dramaId:ep", the reel id, or the claim id for ads. */
    val claimKey: String,
    val dramaId: Int? = null,
    val ep: Int? = null,
    val reelId: Int? = null,
    val adType: String? = null,
    val clientDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
)
