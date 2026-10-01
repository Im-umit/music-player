package com.neonmusic.player.data.db.model

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val customCoverUri: String?,
    val songCount: Int
)
