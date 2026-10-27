package com.dramflix.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dramflix.android.data.local.dao.DramaDao
import com.dramflix.android.data.local.dao.PendingClaimDao
import com.dramflix.android.data.local.dao.ReelDao
import com.dramflix.android.data.local.dao.UserDao
import com.dramflix.android.data.local.entity.DramaCardEntity
import com.dramflix.android.data.local.entity.DramaDetailEntity
import com.dramflix.android.data.local.entity.DramaListEntryEntity
import com.dramflix.android.data.local.entity.EpisodeEntity
import com.dramflix.android.data.local.entity.LikedReelEntity
import com.dramflix.android.data.local.entity.PendingClaimEntity
import com.dramflix.android.data.local.entity.ReelEntity
import com.dramflix.android.data.local.entity.ReelListEntryEntity
import com.dramflix.android.data.local.entity.UserProfileEntity

@Database(
    entities = [
        DramaCardEntity::class,
        DramaListEntryEntity::class,
        DramaDetailEntity::class,
        EpisodeEntity::class,
        ReelEntity::class,
        ReelListEntryEntity::class,
        LikedReelEntity::class,
        UserProfileEntity::class,
        PendingClaimEntity::class,
    ],
    // v2: list membership split out of drama_cards / reels (fixes items
    // vanishing when they appear in more than one list), liked_reels added.
    // v3: pending_claims (duplicate-safe reward retries), Google fields on user_profile.
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dramaDao(): DramaDao
    abstract fun reelDao(): ReelDao
    abstract fun userDao(): UserDao
    abstract fun pendingClaimDao(): PendingClaimDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dramaflix.db",
                )
                    // Everything here is a cache of server data (the user id lives
                    // in SharedPreferences), so a schema change can safely rebuild it.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
    }
}
