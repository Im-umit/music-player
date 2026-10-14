package com.neonmusic.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Upsert
import androidx.room.Query
import androidx.room.Update
import com.neonmusic.player.data.db.entity.SongEntity
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
    suspend fun getAllSongsByArtistAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY artist COLLATE NOCASE DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByArtistDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByAlbumAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY album COLLATE NOCASE DESC, trackNumber ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByAlbumDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    suspend fun getAllSongsByDateAddedDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY dateAdded ASC")
    suspend fun getAllSongsByDateAddedAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY duration DESC")
    suspend fun getAllSongsByDurationDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY duration ASC")
    suspend fun getAllSongsByDurationAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY size DESC")
    suspend fun getAllSongsBySizeDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY size ASC")
    suspend fun getAllSongsBySizeAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY playCount DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByPlayCountDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY playCount ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByPlayCountAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY year DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByYearDesc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY year ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByYearAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY folderName COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByFolderAsc(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY folderName COLLATE NOCASE DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSongsByFolderDesc(): List<SongEntity>


    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<SongEntity>
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE albumId = :albumId LIMIT 1")
    suspend fun getSongByAlbumId(albumId: Long): SongEntity?

    @Query("UPDATE songs SET albumArtUri = 'https://music.local/albumart/' || id WHERE albumArtUri LIKE 'content://%'")
    suspend fun updateLegacyAlbumArtUris()

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

    @Query("SELECT * FROM songs WHERE album = :album AND (:artist = '' OR artist = :artist) ORDER BY discNumber ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    suspend fun getSongsByAlbumName(album: String, artist: String): List<SongEntity>

    @Query("SELECT DISTINCT artist FROM songs WHERE artist != '' ORDER BY artist COLLATE NOCASE ASC")
    suspend fun getAllArtists(): List<String>

    @Query("SELECT DISTINCT album FROM songs WHERE album != '' ORDER BY album COLLATE NOCASE ASC")
    suspend fun getAllAlbums(): List<String>

    @Query("SELECT DISTINCT genre FROM songs WHERE genre != '' ORDER BY genre COLLATE NOCASE ASC")
    suspend fun getAllGenres(): List<String>

    @Upsert
    suspend fun insertSongs(songs: List<SongEntity>)

    @Upsert
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("SELECT id FROM songs")
    suspend fun getAllSongIds(): List<Long>

    @Query("DELETE FROM songs WHERE id NOT IN (:validIds)")
    suspend fun deleteMissingSongs(validIds: List<Long>)

    @Query("DELETE FROM songs WHERE duration < 8000 AND duration > 0")
    suspend fun deleteShortSongs()

    @Query("DELETE FROM songs WHERE id = -999999")
    suspend fun deleteDemoSongs()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}
