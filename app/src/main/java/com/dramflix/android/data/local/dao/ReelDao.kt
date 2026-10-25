package com.dramflix.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dramflix.android.data.local.entity.ReelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {

    @Query("SELECT * FROM reels WHERE sortTag = :sort ORDER BY sortOrder ASC")
    fun observeReels(sort: String = "newest"): Flow<List<ReelEntity>>

    @Upsert
    suspend fun upsertAll(reels: List<ReelEntity>)

    @Query("DELETE FROM reels WHERE sortTag = :sort")
    suspend fun clearBySortTag(sort: String)

    @Query("SELECT * FROM reels WHERE reelId = :id LIMIT 1")
    fun observeReel(id: Int): Flow<ReelEntity?>

    @Query("SELECT * FROM reels WHERE reelId = :id LIMIT 1")
    suspend fun getReel(id: Int): ReelEntity?

    @Upsert
    suspend fun upsert(reel: ReelEntity)

    @Query("UPDATE reels SET viewCount = viewCount + 1 WHERE reelId = :reelId")
    suspend fun incrementViewCount(reelId: Int)

    @Query("UPDATE reels SET likeCount = CASE WHEN :increment = 1 THEN likeCount + 1 ELSE max(0, likeCount - 1) END WHERE reelId = :reelId")
    suspend fun updateLikeCount(reelId: Int, increment: Boolean)
}
