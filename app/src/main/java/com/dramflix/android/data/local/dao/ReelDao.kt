package com.dramflix.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dramflix.android.data.local.entity.LikedReelEntity
import com.dramflix.android.data.local.entity.ReelEntity
import com.dramflix.android.data.local.entity.ReelListEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {

    // ── Feeds ──

    @Query(
        """
        SELECT r.* FROM reels r
        INNER JOIN reel_list_entries e ON e.reelId = r.reelId
        WHERE e.sortTag = :sort
        ORDER BY e.position ASC
        """,
    )
    fun observeReels(sort: String = "newest"): Flow<List<ReelEntity>>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM reel_list_entries WHERE sortTag = :sort")
    suspend fun nextPosition(sort: String): Int

    @Upsert
    suspend fun upsertAll(reels: List<ReelEntity>)

    @Upsert
    suspend fun upsertListEntries(entries: List<ReelListEntryEntity>)

    @Query("DELETE FROM reel_list_entries WHERE sortTag = :sort")
    suspend fun clearList(sort: String)

    // ── Single reel ──

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

    // ── Likes (local, per device) ──

    @Query("SELECT reelId FROM liked_reels")
    fun observeLikedIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_reels WHERE reelId = :reelId)")
    fun observeIsLiked(reelId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_reels WHERE reelId = :reelId)")
    suspend fun isLiked(reelId: Int): Boolean

    @Upsert
    suspend fun markLiked(entry: LikedReelEntity)

    @Query("DELETE FROM liked_reels WHERE reelId = :reelId")
    suspend fun unmarkLiked(reelId: Int)

    /** Likes belong to an account; cleared when the user switches account or signs out. */
    @Query("DELETE FROM liked_reels")
    suspend fun clearLikes()

    // ── Cache housekeeping ──

    /**
     * Delete reels no feed references any more and that weren't refreshed
     * recently (a reel opened via deep link still has a fresh cachedAt).
     */
    @Query(
        """
        DELETE FROM reels
        WHERE reelId NOT IN (SELECT reelId FROM reel_list_entries)
          AND cachedAt < :olderThan
        """,
    )
    suspend fun deleteOrphanReels(olderThan: Long)
}
