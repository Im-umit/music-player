package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uri: String,
    val path: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val genre: String = "",
    val year: Int = 0,
    val folderName: String = "",
    val relativePath: String = "",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val albumArtUri: String = ""
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            duration = duration,
            uri = uri,
            path = path,
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified,
            mimeType = mimeType,
            trackNumber = trackNumber,
            discNumber = discNumber,
            genre = genre,
            year = year,
            folderName = folderName,
            relativePath = relativePath,
            isFavorite = isFavorite,
            playCount = playCount,
            lastPlayed = lastPlayed,
            albumArtUri = if (albumArtUri.startsWith("content://") || albumArtUri.isBlank()) {
                "https://music.local/albumart/$id"
            } else {
                albumArtUri
            }
        )
    }

    companion object {
        fun fromSong(song: Song): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                albumId = song.albumId,
                duration = song.duration,
                uri = song.uri,
                path = song.path,
                size = song.size,
                dateAdded = song.dateAdded,
                dateModified = song.dateModified,
                mimeType = song.mimeType,
                trackNumber = song.trackNumber,
                discNumber = song.discNumber,
                genre = song.genre,
                year = song.year,
                folderName = song.folderName,
                relativePath = song.relativePath,
                isFavorite = song.isFavorite,
                playCount = song.playCount,
                lastPlayed = song.lastPlayed,
                albumArtUri = song.albumArtUri
            )
        }
    }
}
