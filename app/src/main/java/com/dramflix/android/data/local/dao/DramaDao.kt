package com.dramflix.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dramflix.android.data.local.entity.DramaCardEntity
import com.dramflix.android.data.local.entity.DramaDetailEntity
import com.dramflix.android.data.local.entity.DramaListEntryEntity
import com.dramflix.android.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DramaDao {

    // ── Drama lists (home feed / search) ──

    @Query(
        """
        SELECT c.* FROM drama_cards c
        INNER JOIN drama_list_entries e ON e.dramaId = c.dramaId
        WHERE e.queryTag = :tag
        ORDER BY e.position ASC
        """,
    )
    fun observeDramas(tag: String = ""): Flow<List<DramaCardEntity>>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM drama_list_entries WHERE queryTag = :tag")
    suspend fun nextPosition(tag: String): Int

    @Upsert
    suspend fun upsertCards(dramas: List<DramaCardEntity>)

    @Upsert
    suspend fun upsertListEntries(entries: List<DramaListEntryEntity>)

    @Query("DELETE FROM drama_list_entries WHERE queryTag = :tag")
    suspend fun clearList(tag: String)

    // ── Drama detail ──

    @Query("SELECT * FROM drama_details WHERE dramaId = :id")
    fun observeDetail(id: Int): Flow<DramaDetailEntity?>

    @Query("SELECT * FROM drama_details WHERE dramaId = :id")
    suspend fun getDetail(id: Int): DramaDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: DramaDetailEntity)

    // ── Episodes ──

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY ep ASC")
    fun observeEpisodes(dramaId: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY ep ASC")
    suspend fun getEpisodes(dramaId: Int): List<EpisodeEntity>

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM episodes WHERE dramaId = :dramaId")
    suspend fun clearEpisodes(dramaId: Int)

    // ── Cache housekeeping ──

    /** Keep only the [keep] most recently used search/genre lists. */
    @Query(
        """
        DELETE FROM drama_list_entries
        WHERE queryTag != '' AND queryTag NOT IN (
            SELECT queryTag FROM drama_list_entries
            WHERE queryTag != ''
            GROUP BY queryTag
            ORDER BY MAX(cachedAt) DESC
            LIMIT :keep
        )
        """,
    )
    suspend fun pruneSearchLists(keep: Int)

    @Query("DELETE FROM drama_cards WHERE dramaId NOT IN (SELECT dramaId FROM drama_list_entries)")
    suspend fun deleteOrphanCards()

    @Query("DELETE FROM drama_details WHERE cachedAt < :olderThan")
    suspend fun deleteDetailsOlderThan(olderThan: Long)

    @Query("DELETE FROM episodes WHERE dramaId NOT IN (SELECT dramaId FROM drama_details)")
    suspend fun deleteOrphanEpisodes()
}
