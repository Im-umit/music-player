package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllSongsAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE DESC")
    suspend fun getAllSongsDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByArtist(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY album COLLATE NOCASE ASC, trackNumber ASC")
    suspend fun getAllSongsByAlbum(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    suspend fun getAllSongsByDateAdded(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY duration DESC")
    suspend fun getAllSongsByDuration(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY size DESC")
    suspend fun getAllSongsBySize(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    suspend fun getFavoriteSongs(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoriteSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    suspend fun searchSongs(query: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE folderName = :folderName OR path LIKE :folderPrefix || '%' ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsByFolder(folderName: String, folderPrefix: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC, trackNumber ASC")
    suspend fun getSongsByArtistName(artist: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY trackNumber ASC, title COLLATE NOCASE ASC")
    suspend fun getSongsByAlbumName(album: String): List<SongEntity>

    @Query("SELECT DISTINCT artist FROM songs WHERE artist != '' ORDER BY artist COLLATE NOCASE ASC")
    suspend fun getAllArtists(): List<String>

    @Query("SELECT DISTINCT album FROM songs WHERE album != '' ORDER BY album COLLATE NOCASE ASC")
    suspend fun getAllAlbums(): List<String>

    @Query("SELECT DISTINCT genre FROM songs WHERE genre != '' ORDER BY genre COLLATE NOCASE ASC")
    suspend fun getAllGenres(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("DELETE FROM songs WHERE id NOT IN (:validIds)")
    suspend fun deleteMissingSongs(validIds: List<Long>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}
