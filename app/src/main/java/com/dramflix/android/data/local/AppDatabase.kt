package com.dramflix.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dramflix.android.data.local.dao.DramaDao
import com.dramflix.android.data.local.dao.ReelDao
import com.dramflix.android.data.local.dao.UserDao
import com.dramflix.android.data.local.entity.DramaCardEntity
import com.dramflix.android.data.local.entity.DramaDetailEntity
import com.dramflix.android.data.local.entity.EpisodeEntity
import com.dramflix.android.data.local.entity.ReelEntity
import com.dramflix.android.data.local.entity.UserProfileEntity

@Database(
    entities = [
        DramaCardEntity::class,
        DramaDetailEntity::class,
        EpisodeEntity::class,
        ReelEntity::class,
        UserProfileEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dramaDao(): DramaDao
    abstract fun reelDao(): ReelDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dramaflix.db",
                ).build().also { instance = it }
            }
    }
}
