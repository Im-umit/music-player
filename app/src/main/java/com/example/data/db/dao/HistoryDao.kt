package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.db.entity.PlayHistoryEntity
import com.example.data.db.entity.SongEntity

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlayHistoryEntity)

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN play_history h ON s.id = h.songId
        ORDER BY h.playedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentlyPlayedSongs(limit: Int = 50): List<SongEntity>

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()
}
