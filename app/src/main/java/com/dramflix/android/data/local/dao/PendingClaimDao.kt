package com.dramflix.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dramflix.android.data.local.entity.PendingClaimEntity

@Dao
interface PendingClaimDao {

    @Query("SELECT * FROM pending_claims WHERE type = :type AND claimKey = :key LIMIT 1")
    suspend fun find(type: String, key: String): PendingClaimEntity?

    @Query("SELECT * FROM pending_claims ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingClaimEntity>

    @Upsert
    suspend fun upsert(claim: PendingClaimEntity)

    @Query("DELETE FROM pending_claims WHERE claimId = :claimId")
    suspend fun delete(claimId: String)

    @Query("DELETE FROM pending_claims")
    suspend fun clear()
}
