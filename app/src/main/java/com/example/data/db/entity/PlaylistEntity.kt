package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val customCoverUri: String? = null
) {
    fun toJsonObject(songCount: Int = 0): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("createdAt", createdAt)
            put("customCoverUri", customCoverUri ?: "")
            put("songCount", songCount)
        }
    }
}
