package com.example.data.model

import org.json.JSONObject

data class Song(
    val id: Long,
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
    var isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val albumArtUri: String = ""
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("artist", artist)
            put("album", album)
            put("albumId", albumId)
            put("duration", duration)
            put("uri", uri)
            put("path", path)
            put("size", size)
            put("dateAdded", dateAdded)
            put("dateModified", dateModified)
            put("mimeType", mimeType)
            put("trackNumber", trackNumber)
            put("discNumber", discNumber)
            put("genre", genre)
            put("year", year)
            put("folderName", folderName)
            put("relativePath", relativePath)
            put("isFavorite", isFavorite)
            put("playCount", playCount)
            put("lastPlayed", lastPlayed)
            val safeArtUri = if (albumArtUri.startsWith("content://") || albumArtUri.isBlank()) {
                "https://music.local/albumart/$id"
            } else {
                albumArtUri
            }
            put("albumArtUri", safeArtUri)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Song {
            return Song(
                id = json.optLong("id"),
                title = json.optString("title", "Bilinmeyen Şarkı"),
                artist = json.optString("artist", "Bilinmeyen Sanatçı"),
                album = json.optString("album", "Bilinmeyen Albüm"),
                albumId = json.optLong("albumId", 0L),
                duration = json.optLong("duration", 0L),
                uri = json.optString("uri", ""),
                path = json.optString("path", ""),
                size = json.optLong("size", 0L),
                dateAdded = json.optLong("dateAdded", 0L),
                dateModified = json.optLong("dateModified", 0L),
                mimeType = json.optString("mimeType", "audio/*"),
                trackNumber = json.optInt("trackNumber", 0),
                discNumber = json.optInt("discNumber", 0),
                genre = json.optString("genre", ""),
                year = json.optInt("year", 0),
                folderName = json.optString("folderName", ""),
                relativePath = json.optString("relativePath", ""),
                isFavorite = json.optBoolean("isFavorite", false),
                playCount = json.optInt("playCount", 0),
                lastPlayed = json.optLong("lastPlayed", 0L),
                albumArtUri = json.optString("albumArtUri", "")
            )
        }
    }
}
