package com.neonmusic.player.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.neonmusic.player.data.db.dao.FolderDao
import com.neonmusic.player.data.db.dao.HistoryDao
import com.neonmusic.player.data.db.dao.PlaylistDao
import com.neonmusic.player.data.db.dao.SettingDao
import com.neonmusic.player.data.db.dao.SongDao
import com.neonmusic.player.data.db.entity.FolderEntity
import com.neonmusic.player.data.db.entity.PlayHistoryEntity
import com.neonmusic.player.data.db.entity.PlaylistEntity
import com.neonmusic.player.data.db.entity.PlaylistSongEntity
import com.neonmusic.player.data.db.entity.SettingEntity
import com.neonmusic.player.data.db.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        PlayHistoryEntity::class,
        FolderEntity::class,
        SettingEntity::class
    ],
    version = 4,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v2 intentionally keeps the v1 schema unchanged.
                // This establishes a non-destructive migration baseline.
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild relation tables so existing databases gain real FK constraints
                // without losing valid playlist/history data. Orphan rows are intentionally
                // excluded during the copy.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_songs_new (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL DEFAULT 0,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(playlistId, songId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO playlist_songs_new(playlistId, songId, orderIndex, addedAt)
                    SELECT ps.playlistId, ps.songId, ps.orderIndex, ps.addedAt
                    FROM playlist_songs ps
                    INNER JOIN playlists p ON p.id = ps.playlistId
                    INNER JOIN songs s ON s.id = ps.songId
                """.trimIndent())
                db.execSQL("DROP TABLE playlist_songs")
                db.execSQL("ALTER TABLE playlist_songs_new RENAME TO playlist_songs")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId ON playlist_songs(playlistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_songId ON playlist_songs(songId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS play_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        songId INTEGER NOT NULL,
                        playedAt INTEGER NOT NULL,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO play_history_new(id, songId, playedAt)
                    SELECT h.id, h.songId, h.playedAt
                    FROM play_history h
                    INNER JOIN songs s ON s.id = h.songId
                """.trimIndent())
                db.execSQL("DROP TABLE play_history")
                db.execSQL("ALTER TABLE play_history_new RENAME TO play_history")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_history_songId ON play_history(songId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_history_playedAt ON play_history(playedAt)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Query-speed indexes for large music libraries. All are additive and
                // therefore safe for existing user data.
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_title ON songs(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_artist ON songs(artist)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_album ON songs(album)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_dateAdded ON songs(dateAdded)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_playCount ON songs(playCount)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_songs_folderName ON songs(folderName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId_orderIndex ON playlist_songs(playlistId, orderIndex)")
            }
        }

        fun getInstance(context: Context): AppMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppMusicDatabase::class.java,
                    "muzik_calar.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
