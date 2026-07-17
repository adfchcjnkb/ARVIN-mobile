package com.arvin.player.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        EqPresetEntity::class,
        FavoriteEntity::class,
        HiddenSongEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun hiddenSongDao(): HiddenSongDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arvin_player.db"
                )
                    // Pre-release schema is still evolving; once shipped, replace with real
                    // Migration objects instead of wiping user data on version bumps.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
