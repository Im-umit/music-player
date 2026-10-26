package com.neonmusic.player.util

import com.neonmusic.player.data.model.Song
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Pure, thread-safe search/ranking helpers. Kept outside MusicBridge so the ranking
 * rules can be unit-tested without Android/WebView dependencies.
 */
object SearchEngine {

    fun normalize(value: String?): String {
        if (value.isNullOrBlank()) return ""
        var text = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\u0131", "i")
            .replace("\u00f0", "d")
            .replace("\u00f8", "o")
            .replace("\u00fe", "th")
        text = text.replace(Regex("\\bfeat(?:uring)?\\b|\\bft\\.?\\b"), " feat ")
        // Strip combining marks after NFD, but preserve letters and numbers from
        // every script. The previous ASCII-only class silently made Cyrillic,
        // Arabic, Greek and CJK metadata impossible to search.
        return text
            // Remove accents from Latin letters (e.g. ç -> c) without destroying
            // meaningful marks in other scripts (Cyrillic й must not become и).
            .replace(Regex("(\\p{IsLatin})\\p{M}+"), "$1")
            .replace(Regex("[^\\p{L}\\p{N}\\p{M}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun search(query: String, songs: List<Song>, limit: Int = 100): List<Song> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank() || songs.isEmpty() || limit <= 0) return emptyList()

        val queryTokens = normalizedQuery.split(' ').filter { it.isNotBlank() }.distinct()
        return songs.asSequence()
            .mapIndexed { index, song ->
                SearchResult(song, score(normalizedQuery, queryTokens, song), index)
            }
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<SearchResult> { it.score }.thenBy { it.index })
            .take(limit)
            .map { it.song }
            .toList()
    }

    fun score(query: String, queryTokens: List<String>, song: Song): Int {
        val title = normalize(song.title)
        val artist = normalize(song.artist)
        val album = normalize(song.album)
        val folder = normalize(song.folderName)
        val genre = normalize(song.genre)
        val fields = listOf(title, artist, album, folder, genre)

        var score = fields.count { it == query } * 1000
        if (title == query) score += 900
        if (artist == query) score += 800
        if (album == query) score += 700

        for (field in fields) {
            if (field.isBlank()) continue
            if (field.startsWith(query)) score = maxOf(score, 650)
            if (field.contains(query)) score = maxOf(score, 500)
        }

        val matchedTokens = queryTokens.count { token ->
            fields.any { field ->
                field.split(' ').any { word ->
                    word == token || word.startsWith(token) ||
                        (token.length >= 4 && levenshteinAtMost(word, token, 2))
                }
            }
        }
        score += matchedTokens * 120
        if (matchedTokens == queryTokens.size) score += 180
        return score
    }

    fun levenshteinAtMost(a: String, b: String, maxDistance: Int): Boolean {
        if (a == b) return true
        if (maxDistance < 0 || abs(a.length - b.length) > maxDistance) return false
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            var rowMin = current[0]
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(
                    previous[j + 1] + 1,
                    current[j] + 1,
                    previous[j] + cost
                )
                rowMin = minOf(rowMin, current[j + 1])
            }
            if (rowMin > maxDistance) return false
            previous = current
        }
        return previous[b.length] <= maxDistance
    }

    private data class SearchResult(
        val song: Song,
        val score: Int,
        val index: Int
    )
}
