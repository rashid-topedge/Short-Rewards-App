package com.dramflix.android.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dramflix.android.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user_profile ORDER BY cachedAt DESC LIMIT 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY cachedAt DESC LIMIT 1")
    suspend fun get(): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)

    /** There is only ever one signed-in profile; drop stale rows if the id changed. */
    @Query("DELETE FROM user_profile WHERE userId != :userId")
    suspend fun deleteOthers(userId: String)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}
