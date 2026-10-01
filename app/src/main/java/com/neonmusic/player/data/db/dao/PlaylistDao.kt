package com.neonmusic.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.neonmusic.player.data.db.entity.PlaylistEntity
import com.neonmusic.player.data.db.entity.PlaylistSongEntity
import com.neonmusic.player.data.db.entity.SongEntity
import com.neonmusic.player.data.db.model.PlaylistWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT p.id, p.name, p.createdAt, p.customCoverUri,
               (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlistId = p.id) AS songCount
        FROM playlists p
        ORDER BY p.createdAt DESC
    """)
    suspend fun getAllPlaylistsWithCounts(): List<PlaylistWithCount>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun updatePlaylistName(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongEntity)

    @Query("SELECT COALESCE(MAX(orderIndex) + 1, 0) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getNextOrderIndex(playlistId: Long): Int

    @Transaction
    suspend fun addSongToPlaylistIfAbsent(
        playlistId: Long,
        songId: Long,
        addedAt: Long = System.currentTimeMillis()
    ): Long {
        if (playlistId <= 0L || songId <= 0L) return -1L
        val nextOrderIndex = getNextOrderIndex(playlistId)
        return insertPlaylistSongIfAbsent(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = nextOrderIndex,
                addedAt = addedAt
            )
        )
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongIfAbsent(crossRef: PlaylistSongEntity): Long

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: Long): Int

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC, ps.addedAt ASC
    """)
    suspend fun getSongsInPlaylist(playlistId: Long): List<SongEntity>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC, addedAt ASC")
    suspend fun getPlaylistSongEntries(playlistId: Long): List<PlaylistSongEntity>

    @Query("UPDATE playlist_songs SET orderIndex = :orderIndex WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongOrder(playlistId: Long, songId: Long, orderIndex: Int)

    @Transaction
    suspend fun moveSongInPlaylist(playlistId: Long, songId: Long, requestedIndex: Int): Boolean {
        if (playlistId <= 0L || songId <= 0L) return false
        val entries = getPlaylistSongEntries(playlistId).toMutableList()
        val fromIndex = entries.indexOfFirst { it.songId == songId }
        if (fromIndex < 0) return false
        val toIndex = requestedIndex.coerceIn(0, entries.lastIndex)
        if (fromIndex == toIndex) return true

        val moved = entries.removeAt(fromIndex)
        entries.add(toIndex, moved)
        entries.forEachIndexed { index, entry ->
            updateSongOrder(playlistId, entry.songId, index)
        }
        return true
    }
}
