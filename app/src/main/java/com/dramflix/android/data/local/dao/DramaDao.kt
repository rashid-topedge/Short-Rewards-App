package com.dramflix.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dramflix.android.data.local.entity.DramaCardEntity
import com.dramflix.android.data.local.entity.DramaDetailEntity
import com.dramflix.android.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DramaDao {

    // ── Drama cards (list) ──

    @Query("SELECT * FROM drama_cards WHERE queryTag = :tag ORDER BY sortOrder ASC")
    fun observeDramas(tag: String = ""): Flow<List<DramaCardEntity>>

    @Query("SELECT * FROM drama_cards WHERE queryTag = :tag ORDER BY sortOrder ASC")
    suspend fun getDramas(tag: String = ""): List<DramaCardEntity>

    @Upsert
    suspend fun upsertCards(dramas: List<DramaCardEntity>)

    @Query("DELETE FROM drama_cards WHERE queryTag = :tag")
    suspend fun clearCardsByTag(tag: String)

    // ── Drama detail ──

    @Query("SELECT * FROM drama_details WHERE dramaId = :id")
    fun observeDetail(id: Int): Flow<DramaDetailEntity?>

    @Upsert
    suspend fun upsertDetail(detail: DramaDetailEntity)

    // ── Episodes ──

    @Query("SELECT * FROM episodes WHERE dramaId = :dramaId ORDER BY ep ASC")
    fun observeEpisodes(dramaId: Int): Flow<List<EpisodeEntity>>

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)
}
