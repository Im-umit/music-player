package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.dao.FolderDao
import com.example.data.db.dao.HistoryDao
import com.example.data.db.dao.PlaylistDao
import com.example.data.db.dao.SettingDao
import com.example.data.db.dao.SongDao
import com.example.data.db.entity.FolderEntity
import com.example.data.db.entity.PlayHistoryEntity
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.PlaylistSongEntity
import com.example.data.db.entity.SettingEntity
import com.example.data.db.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        PlayHistoryEntity::class,
        FolderEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun folderDao(): FolderDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppMusicDatabase? = null

        fun getInstance(context: Context): AppMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppMusicDatabase::class.java,
                    "muzik_calar.db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            db.execSQL("UPDATE songs SET albumArtUri = 'https://music.local/albumart/' || id WHERE albumArtUri LIKE 'content://%'")
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
