package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val path: String,
    val displayName: String,
    val songCount: Int = 0,
    val isSaf: Boolean = false,
    val treeUri: String? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("path", path)
            put("displayName", displayName)
            put("songCount", songCount)
            put("isSaf", isSaf)
            put("treeUri", treeUri ?: "")
        }
    }
}
