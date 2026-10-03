package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.db.dao.DownloadDao
import com.example.data.db.dao.PlaylistDao
import com.example.data.db.dao.SearchHistoryDao
import com.example.data.db.dao.SongDao
import com.example.data.db.entity.DownloadEntity
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.PlaylistSongCrossRef
import com.example.data.db.entity.SearchHistoryEntity
import com.example.data.db.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        DownloadEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "musichub.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
