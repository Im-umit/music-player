package com.neonmusic.player

import com.neonmusic.player.data.model.Song
import com.neonmusic.player.util.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private fun song(id: Long, title: String, artist: String, album: String = "Albüm") = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = id,
        duration = 180_000L,
        uri = "content://media/external/audio/media/$id",
        path = "",
        size = 1_000_000L,
        dateAdded = id,
        dateModified = id,
        mimeType = "audio/mpeg"
    )

    @Test
    fun normalizesTurkishCharactersAndFeaturingVariants() {
        assertEquals("istanbul feat ceza", SearchEngine.normalize("İstanbul ft. Ceza"))
        assertEquals("cagri", SearchEngine.normalize("Çağrı"))
    }


    @Test
    fun preservesNonLatinMetadataForSearch() {
        val russian = song(7, "Ночной звонок", "Кавинский", "Москва")
        assertTrue(SearchEngine.normalize("Ночной звонок").isNotBlank())
        assertEquals(7L, SearchEngine.search("кавинский", listOf(russian)).first().id)
        assertEquals(7L, SearchEngine.search("москва", listOf(russian)).first().id)
    }

    @Test
    fun blankQueriesReturnNoResults() {
        val target = song(1, "Nightcall", "Kavinsky")
        assertTrue(SearchEngine.search("   ", listOf(target)).isEmpty())
        assertTrue(SearchEngine.search("nightcall", listOf(target), limit = 0).isEmpty())
    }

    @Test
    fun exactTitleRanksAbovePartialMatches() {
        val exact = song(1, "Nightcall", "Kavinsky")
        val partial = song(2, "Nightcall Remix", "Other Artist")

        val results = SearchEngine.search("nightcall", listOf(partial, exact))

        assertEquals(1L, results.first().id)
    }

    @Test
    fun artistAndAlbumAreSearchable() {
        val target = song(1, "Track", "Kavinsky", "OutRun")
        val other = song(2, "Track 2", "Someone Else", "Other Album")

        assertEquals(1L, SearchEngine.search("kavinsky", listOf(other, target)).first().id)
        assertEquals(1L, SearchEngine.search("outrun", listOf(other, target)).first().id)
    }

    @Test
    fun fuzzyMatchingHandlesSmallTypos() {
        val target = song(1, "Nightcall", "Kavinsky")
        val results = SearchEngine.search("nightcal", listOf(target))

        assertTrue(results.any { it.id == 1L })
    }

    @Test
    fun levenshteinHonorsDistanceLimit() {
        assertTrue(SearchEngine.levenshteinAtMost("nightcall", "nightcal", 1))
        assertTrue(!SearchEngine.levenshteinAtMost("nightcall", "something", 2))
    }
}
