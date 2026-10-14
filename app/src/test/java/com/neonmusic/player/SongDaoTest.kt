package com.neonmusic.player

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.neonmusic.player.data.db.AppMusicDatabase
import com.neonmusic.player.data.db.entity.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SongDaoTest {
    private lateinit var db: AppMusicDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppMusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun song(id: Long, title: String, artist: String, favorite: Boolean = false): SongEntity = SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = "Album $id",
        albumId = id,
        duration = 180_000,
        uri = "content://media/external/audio/media/$id",
        path = "",
        size = 1000,
        dateAdded = id,
        dateModified = id,
        mimeType = "audio/mpeg",
        isFavorite = favorite
    )

    @Test
    fun searchMatchesTitleArtistAlbumAndFolder() = runBlocking {
        db.songDao().insertSongs(
            listOf(
                song(1, "Nightcall", "Kavinsky"),
                song(2, "Other Track", "Someone Else")
                    .copy(album = "OutRun", folderName = "Synthwave")
            )
        )

        assertEquals(1, db.songDao().searchSongs("Nightcall").size)
        assertEquals(1, db.songDao().searchSongs("Kavinsky").size)
        assertEquals(1, db.songDao().searchSongs("OutRun").size)
        assertEquals(1, db.songDao().searchSongs("Synthwave").size)
    }

    @Test
    fun favoriteAndPlayCountUpdatesPersist() = runBlocking {
        db.songDao().insertSong(song(1, "Track", "Artist"))
        db.songDao().updateFavorite(1, true)
        db.songDao().incrementPlayCount(1, 12345L)

        val saved = db.songDao().getSongById(1)!!
        assertTrue(saved.isFavorite)
        assertEquals(1, saved.playCount)
        assertEquals(12345L, saved.lastPlayed)
    }

    @Test
    fun largeLibrarySearchReturnsAtMostExpectedNativeLimit() {
        val songEntities = (1L..5000L).map { id -> song(id, "Track $id", "Artist $id") }
        val songs = songEntities.map(SongEntity::toSong)
        runBlocking { db.songDao().insertSongs(songEntities) }
        // This verifies the database can hold a realistically sized library without
        // changing the native SearchEngine's 100-result contract.
        assertEquals(100, com.neonmusic.player.util.SearchEngine.search("track", songs, 100).size)
    }
}
