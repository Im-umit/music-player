package com.neonmusic.player.bridge

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.core.content.FileProvider
import android.webkit.WebView
import com.neonmusic.player.MainActivity
import com.neonmusic.player.data.db.AppMusicDatabase
import com.neonmusic.player.data.db.entity.FolderEntity
import com.neonmusic.player.data.db.entity.PlayHistoryEntity
import com.neonmusic.player.data.db.entity.PlaylistEntity
import com.neonmusic.player.data.db.entity.PlaylistSongEntity
import com.neonmusic.player.data.db.entity.SettingEntity
import com.neonmusic.player.data.mediastore.MediaStoreScanner
import com.neonmusic.player.data.model.Song
import com.neonmusic.player.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.LinkedHashMap
import java.text.Normalizer
import java.util.Locale

class MusicBridge(
    private val activity: MainActivity,
    private val webView: WebView
) {
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val db = AppMusicDatabase.getInstance(activity)
    private val scanner = MediaStoreScanner(activity)
    @Volatile private var destroyed = false
    private var lyricsJob: Job? = null
    private var scanJob: Job? = null

    // Warm an in-memory library snapshot so synchronous JavaScript bridge calls do not
    // repeatedly block the WebView thread on Room for large libraries. The DB remains the
    // source of truth; this cache is refreshed after scans and invalidated on destructive edits.
    @Volatile private var libraryCacheReady = false
    @Volatile private var libraryCache: List<Song> = emptyList()

    // Small bounded cache: lyrics requests are common when rapidly switching tracks,
    // but an unbounded map would keep every fetched lyric in memory for the activity lifetime.
    private val lyricsCache = object : LinkedHashMap<String, String>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 32
    }
    private val lyricsCacheLock = Any()

    private val playbackStateListener: (JSONObject) -> Unit = { stateJson ->
        notifyWebPlaybackState(stateJson)
    }

    init {
        MusicPlaybackService.onStateChangeListener = playbackStateListener
        bridgeScope.launch(Dispatchers.IO) {
            refreshLibraryCache()
        }
    }

    private suspend fun refreshLibraryCache() {
        try {
            val songs = db.songDao().getAllSongsAsc().map { it.toSong() }
            libraryCache = songs
            libraryCacheReady = true
        } catch (e: Exception) {
            Log.w("MusicBridge", "Library cache warm-up failed: ${e.message}")
        }
    }

    fun invalidateLibraryCache() {
        libraryCacheReady = false
    }

    fun destroy() {
        destroyed = true
        lyricsJob?.cancel()
        lyricsJob = null
        scanJob?.cancel()
        scanJob = null
        synchronized(lyricsCacheLock) { lyricsCache.clear() }
        if (MusicPlaybackService.onStateChangeListener === playbackStateListener) {
            MusicPlaybackService.onStateChangeListener = null
        }
        bridgeScope.cancel()
    }

    fun notifyWebPlaybackState(stateJson: JSONObject) {
        if (destroyed) return
        activity.runOnUiThread {
            if (destroyed) return@runOnUiThread
            try {
                val jsonString = stateJson.toString()
                webView.evaluateJavascript("window.onPlaybackStateUpdated && window.onPlaybackStateUpdated($jsonString)", null)
            } catch (e: Exception) {
                Log.w("MusicBridge", "Playback state delivery failed: ${e.message}")
            }
        }
    }

    private fun evalJs(script: String) {
        if (destroyed) return
        activity.runOnUiThread {
            if (destroyed) return@runOnUiThread
            try { webView.evaluateJavascript(script, null) }
            catch (e: Exception) { Log.w("MusicBridge", "JS callback failed: ${e.message}") }
        }
    }

    @JavascriptInterface
    fun checkPermissions(): Boolean {
        return activity.hasAudioPermission()
    }

    @JavascriptInterface
    fun requestPermissions() {
        activity.runOnUiThread {
            activity.requestAudioPermission()
        }
    }

    @JavascriptInterface
    fun cancelScan() {
        scanJob?.cancel()
    }

    @JavascriptInterface
    fun scanLibrary(): String {
        scanJob?.cancel()
        scanJob = bridgeScope.launch {
            try {
                if (!activity.hasAudioPermission()) {
                    activity.runOnUiThread {
                        activity.requestAudioPermission()
                    }
                    return@launch
                }
                val songs = scanner.scanMediaStore()
                ensureActive()
                evalJs("window.onScanCompleted && window.onScanCompleted(${songs.size})")
            } catch (e: kotlinx.coroutines.CancellationException) {
                // A newer scan superseded this one; do not surface a false error.
            } catch (e: Exception) {
                Log.e("MusicBridge", "scanLibrary error: ${e.message}")
                val message = JSONObject.quote(e.message ?: "Unknown scan error")
                evalJs("window.onScanFailed && window.onScanFailed($message)")
            }
        }
        return "STARTED"
    }

    @JavascriptInterface
    fun getSongs(sortOrder: String): String = runBlocking(Dispatchers.IO) {
        val cleanOrder = sortOrder.trim().lowercase()
        val useCache = libraryCacheReady && cleanOrder !in setOf("play_count", "play_count_desc", "play")
        val songs = if (useCache) {
            libraryCache
        } else {
            db.songDao().getAllSongsAsc().map { it.toSong() }.also {
                libraryCache = it
                libraryCacheReady = true
            }
        }
        val list = songs.sortedWith { a, b ->
            fun text(v: String) = v.lowercase(Locale.ROOT)
            when (cleanOrder) {
                "title_desc" -> text(b.title).compareTo(text(a.title))
                "artist_asc", "artist" -> compareValuesBy(a, b, { text(it.artist) }, { text(it.title) })
                "artist_desc" -> text(b.artist).compareTo(text(a.artist)).takeIf { it != 0 } ?: text(a.title).compareTo(text(b.title))
                "album_asc", "album" -> compareValuesBy(a, b, { text(it.album) }, { it.trackNumber }, { text(it.title) })
                "album_desc" -> text(b.album).compareTo(text(a.album)).takeIf { it != 0 } ?: compareValuesBy(a, b, { it.trackNumber }, { text(it.title) })
                "date_added_asc", "date_asc" -> compareValuesBy(a, b, { it.dateAdded }, { text(it.title) })
                "date_added_desc", "date_added", "date", "date_desc" -> b.dateAdded.compareTo(a.dateAdded).takeIf { it != 0 } ?: text(a.title).compareTo(text(b.title))
                "duration_asc" -> compareValuesBy(a, b, { it.duration }, { text(it.title) })
                "duration_desc", "duration" -> b.duration.compareTo(a.duration).takeIf { it != 0 } ?: text(a.title).compareTo(text(b.title))
                "size_asc" -> compareValuesBy(a, b, { it.size }, { text(it.title) })
                "size_desc", "size" -> b.size.compareTo(a.size).takeIf { it != 0 } ?: text(a.title).compareTo(text(b.title))
                "play_count_asc" -> compareValuesBy(a, b, { it.playCount }, { text(it.title) })
                "play_count_desc", "play_count", "play" -> b.playCount.compareTo(a.playCount).takeIf { it != 0 } ?: text(a.title).compareTo(text(b.title))
                "year_asc" -> compareValuesBy(a, b, { it.year }, { text(it.title) })
                "year_desc", "year" -> b.year.compareTo(a.year).takeIf { it != 0 } ?: text(a.title).compareTo(text(b.title))
                "folder_asc", "folder" -> compareValuesBy(a, b, { text(it.folderName) }, { text(it.title) })
                "folder_desc" -> text(b.folderName).compareTo(text(a.folderName)).takeIf { it != 0 } ?: text(a.title).compareTo(text(b.title))
                else -> text(a.title).compareTo(text(b.title))
            }
        }

        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun searchSongs(query: String): String = runBlocking(Dispatchers.IO) {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isBlank()) return@runBlocking JSONArray().toString()

        // Room's LIKE is useful for the fast path, but it is not accent-insensitive
        // and cannot handle common metadata variants such as "feat." vs "ft".
        // Score the already-local Room dataset instead of adding a fragile schema/FTS
        // migration just for search. Results are capped so large libraries stay cheap.
        val queryTokens = normalizedQuery.split(' ').filter { it.isNotBlank() }.distinct()
        val candidates = if (libraryCacheReady) libraryCache else db.songDao().getAllSongsAsc().map { it.toSong() }.also { libraryCache = it; libraryCacheReady = true }
        val seen = HashSet<Long>()
        val ranked = ArrayList<Pair<Int, Song>>()

        for (entity in candidates) {
            if (!seen.add(entity.id)) continue
            val title = normalizeSearchText(entity.title)
            val artist = normalizeSearchText(entity.artist)
            val album = normalizeSearchText(entity.album)
            val folder = normalizeSearchText(entity.folderName)
            val genre = normalizeSearchText(entity.genre)
            val fields = listOf(title, artist, album, folder, genre)

            var score = 0
            val exactFields = fields.count { it == normalizedQuery }
            if (exactFields > 0) score += 1000 * exactFields
            if (title == normalizedQuery) score += 900
            if (artist == normalizedQuery) score += 800
            if (album == normalizedQuery) score += 700

            for (field in fields) {
                if (field.isBlank()) continue
                if (field.startsWith(normalizedQuery)) score = maxOf(score, 650)
                if (field.contains(normalizedQuery)) score = maxOf(score, 500)
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

            // Common artist metadata variants: "feat", "ft", and punctuation are
            // normalized to the same searchable token.
            if (matchedTokens == queryTokens.size) score += 180

            if (score > 0) ranked.add(score to entity)
        }

        val results = ranked
            .sortedWith(compareByDescending<Pair<Int, Song>> { it.first }
                .thenBy { normalizeSearchText(it.second.title) })
            .take(100)
            .map { it.second }

        val jsonArray = JSONArray()
        results.forEach { jsonArray.put(it.toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    private fun normalizeSearchText(value: String?): String {
        if (value.isNullOrBlank()) return ""
        var text = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\u0131", "i")
            .replace("\u00f0", "d")
            .replace("\u00f8", "o")
            .replace("\u00fe", "th")
            .replace("\u00f0", "d")
        text = text.replace(Regex("\\bfeat(?:uring)?\\b|\\bft\\.?\\b"), " feat ")
        return text
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun levenshteinAtMost(a: String, b: String, maxDistance: Int): Boolean {
        if (kotlin.math.abs(a.length - b.length) > maxDistance) return false
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

    @JavascriptInterface
    fun playSong(songId: Long, queueJson: String, index: Int) {
        activity.runOnUiThread {
            activity.ensureServiceStarted()
        }

        bridgeScope.launch {
            val service = MusicPlaybackService.instance
            if (service != null) {
                startQueuePlayback(service, queueJson, index, songId)
            } else {
                var retries = 0
                while (MusicPlaybackService.instance == null && retries < 15) {
                    kotlinx.coroutines.delay(100)
                    retries++
                }
                val readyService = MusicPlaybackService.instance ?: return@launch
                startQueuePlayback(readyService, queueJson, index, songId)
            }
        }
    }

    @JavascriptInterface
    fun playAllShuffled(queueJson: String) {
        activity.runOnUiThread {
            activity.ensureServiceStarted()
        }

        bridgeScope.launch {
            val service = MusicPlaybackService.instance
            if (service != null) {
                startShuffledPlayback(service, queueJson)
            } else {
                var retries = 0
                while (MusicPlaybackService.instance == null && retries < 15) {
                    kotlinx.coroutines.delay(100)
                    retries++
                }
                val readyService = MusicPlaybackService.instance ?: return@launch
                startShuffledPlayback(readyService, queueJson)
            }
        }
    }

    private suspend fun startShuffledPlayback(service: MusicPlaybackService, queueJson: String) {
        try {
            val songs = decodeQueueSongs(queueJson)
            if (songs.isNotEmpty()) service.playAllShuffled(songs)
        } catch (e: Exception) {
            Log.e("MusicBridge", "playAllShuffled failed: ${e.message}")
        }
    }

    private suspend fun startQueuePlayback(service: MusicPlaybackService, queueJson: String, index: Int, selectedSongId: Long) {
        try {
            val songs = decodeQueueSongs(queueJson)
            if (songs.isNotEmpty()) {
                val resolvedIndex = songs.indexOfFirst { it.id == selectedSongId }
                    .takeIf { it >= 0 } ?: index.coerceIn(0, songs.lastIndex)
                service.playQueue(songs, resolvedIndex)
            }
        } catch (e: Exception) {
            Log.e("MusicBridge", "playSong failed: ${e.message}")
        }
    }

    /**
     * Queues are transmitted as compact ID arrays from the WebView. Older clients may
     * still send full Song objects, so object-array payloads remain supported.
     */
    private suspend fun decodeQueueSongs(queueJson: String): List<Song> {
        if (queueJson.length > 2_000_000) throw IllegalArgumentException("Queue payload too large")
        val array = JSONArray(queueJson)
        if (array.length() == 0) return emptyList()

        val first = array.opt(0)
        if (first is JSONObject) {
            return buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(Song.fromJsonObject(obj))
                }
            }
        }

        val ids = buildList(array.length()) {
            for (i in 0 until array.length()) {
                val id = array.optLong(i, -1L)
                if (id > 0) add(id)
            }
        }
        if (ids.isEmpty()) return emptyList()

        val byId = HashMap<Long, Song>(ids.size)
        ids.chunked(900).forEach { chunk ->
            db.songDao().getSongsByIds(chunk).forEach { entity ->
                byId[entity.id] = entity.toSong()
            }
        }
        return ids.mapNotNull { byId[it] }
    }

    @JavascriptInterface
    fun pause() {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.pause()
        }
    }

    @JavascriptInterface
    fun resume() {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.resume()
        }
    }

    @JavascriptInterface
    fun next() {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.next()
        }
    }

    @JavascriptInterface
    fun previous() {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.previous()
        }
    }

    @JavascriptInterface
    fun seek(positionMs: Long) {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.let { service ->
                service.seekTo(positionMs.coerceAtLeast(0L))
            }
        }
    }

    @JavascriptInterface
    fun seekBy(deltaMs: Long) {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.seekBy(deltaMs)
        }
    }

    @JavascriptInterface
    fun setKeepScreenOn(enabled: Boolean) {
        activity.runOnUiThread {
            if (enabled) {
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    @JavascriptInterface
    fun toggleShuffle(): Boolean {
        val service = MusicPlaybackService.instance ?: return false
        val nextShuffle = !service.cachedShuffle
        activity.runOnUiThread {
            service.toggleShuffle()
        }
        return nextShuffle
    }

    @JavascriptInterface
    fun setRepeatMode(mode: Int) {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.setRepeatMode(mode.coerceIn(0, 2))
        }
    }

    @JavascriptInterface
    fun setPlaybackSpeed(speed: Float) {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.setPlaybackSpeed(speed.coerceIn(0.25f, 3.0f))
        }
    }

    @JavascriptInterface
    fun addToQueueNext(songJson: String) {
        activity.runOnUiThread {
            try {
                val song = Song.fromJsonObject(JSONObject(songJson))
                MusicPlaybackService.instance?.addToQueueNext(song)
            } catch (e: Exception) {
                Log.e("MusicBridge", "addToQueueNext error: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun addToQueueEnd(songJson: String) {
        activity.runOnUiThread {
            try {
                val song = Song.fromJsonObject(JSONObject(songJson))
                MusicPlaybackService.instance?.addToQueueEnd(song)
            } catch (e: Exception) {
                Log.e("MusicBridge", "addToQueueEnd error: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun removeFromQueue(index: Int) {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.removeFromQueue(index)
        }
    }

    @JavascriptInterface
    fun clearQueue() {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.clearQueue()
        }
    }

    @JavascriptInterface
    fun getCurrentState(): String {
        val service = MusicPlaybackService.instance
        return service?.getCurrentPlaybackStateJson()?.toString() ?: JSONObject().apply {
            put("hasSong", false)
            put("isPlaying", false)
            put("position", 0)
            put("duration", 0)
        }.toString()
    }

    @JavascriptInterface
    fun toggleFavorite(songId: Long): Boolean = runBlocking(Dispatchers.IO) {
        val song = db.songDao().getSongById(songId) ?: return@runBlocking false
        val newFav = !song.isFavorite
        db.songDao().updateFavorite(songId, newFav)
        if (libraryCacheReady) {
            libraryCache.find { it.id == songId }?.isFavorite = newFav
        }
        return@runBlocking newFav
    }

    @JavascriptInterface
    fun getFavorites(): String = runBlocking(Dispatchers.IO) {
        val list = db.songDao().getFavoriteSongs()
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun getHistory(): String = runBlocking(Dispatchers.IO) {
        val list = db.historyDao().getRecentlyPlayedSongs(50)
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun clearHistory() = runBlocking(Dispatchers.IO) {
        db.historyDao().clearHistory()
    }

    @JavascriptInterface
    fun getPlaylists(): String = runBlocking(Dispatchers.IO) {
        val playlists = db.playlistDao().getAllPlaylistsWithCounts()
        val jsonArray = JSONArray()
        playlists.forEach {
            jsonArray.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("createdAt", it.createdAt)
                put("customCoverUri", it.customCoverUri ?: "")
                put("songCount", it.songCount)
            })
        }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun createPlaylist(name: String): Long = runBlocking(Dispatchers.IO) {
        val cleanName = name.trim().take(120)
        if (cleanName.isBlank()) return@runBlocking 0L
        val playlist = PlaylistEntity(name = cleanName)
        return@runBlocking db.playlistDao().insertPlaylist(playlist)
    }

    @JavascriptInterface
    fun renamePlaylist(id: Long, name: String): Boolean = runBlocking(Dispatchers.IO) {
        if (id <= 0) return@runBlocking false
        val cleanName = name.trim().take(120)
        if (cleanName.isBlank()) return@runBlocking false
        if (db.playlistDao().getPlaylistById(id) == null) return@runBlocking false
        db.playlistDao().updatePlaylistName(id, cleanName)
        true
    }

    @JavascriptInterface
    fun deletePlaylist(id: Long) = runBlocking(Dispatchers.IO) {
        db.playlistDao().deletePlaylistById(id)
    }

    @JavascriptInterface
    fun addSongToPlaylist(playlistId: Long, songId: Long): Boolean = runBlocking(Dispatchers.IO) {
        if (playlistId <= 0 || songId <= 0) return@runBlocking false
        if (db.playlistDao().getPlaylistById(playlistId) == null) return@runBlocking false
        if (db.songDao().getSongById(songId) == null) return@runBlocking false
        db.playlistDao().addSongToPlaylistIfAbsent(playlistId, songId) > 0
    }

    @JavascriptInterface
    fun removeSongFromPlaylist(playlistId: Long, songId: Long): Boolean = runBlocking(Dispatchers.IO) {
        if (playlistId <= 0L || songId <= 0L) return@runBlocking false
        if (db.playlistDao().getPlaylistById(playlistId) == null) return@runBlocking false
        db.playlistDao().removeSongFromPlaylist(playlistId, songId) > 0
    }

    @JavascriptInterface
    fun getPlaylistSongs(playlistId: Long): String = runBlocking(Dispatchers.IO) {
        val songs = db.playlistDao().getSongsInPlaylist(playlistId)
        val jsonArray = JSONArray()
        songs.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun moveSongInPlaylist(playlistId: Long, songId: Long, newIndex: Int): Boolean = runBlocking(Dispatchers.IO) {
        if (playlistId <= 0L || songId <= 0L) return@runBlocking false
        db.playlistDao().moveSongInPlaylist(playlistId, songId, newIndex)
    }

    @JavascriptInterface
    fun getFolders(): String = runBlocking(Dispatchers.IO) {
        val folders = db.folderDao().getAllFolders()
        val jsonArray = JSONArray()
        folders.forEach { jsonArray.put(it.toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun getFolderSongs(folderPath: String): String = runBlocking(Dispatchers.IO) {
        val folderName = folderPath.trim('/').substringAfterLast('/')
        val songs = db.songDao().getSongsByFolder(folderName, folderPath)
        val jsonArray = JSONArray()
        songs.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun getArtists(): String = runBlocking(Dispatchers.IO) {
        val artists = db.songDao().getAllArtists()
        val jsonArray = JSONArray()
        artists.forEach { jsonArray.put(it) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun getArtistSongs(artist: String): String = runBlocking(Dispatchers.IO) {
        val songs = db.songDao().getSongsByArtistName(artist)
        val jsonArray = JSONArray()
        songs.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun getAlbums(): String = runBlocking(Dispatchers.IO) {
        val albums = db.songDao().getAllAlbums()
        val jsonArray = JSONArray()
        albums.forEach { jsonArray.put(it) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun getAlbumSongs(album: String, artist: String = ""): String = runBlocking(Dispatchers.IO) {
        val cleanAlbum = album.trim().take(300)
        val cleanArtist = artist.trim().take(300)
        if (cleanAlbum.isBlank()) return@runBlocking JSONArray().toString()
        val songs = db.songDao().getSongsByAlbumName(cleanAlbum, cleanArtist)
        val jsonArray = JSONArray()
        songs.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun pickFolder() {
        activity.runOnUiThread {
            activity.launchSafFolderPicker()
        }
    }

    @JavascriptInterface
    fun pickSafFolder() {
        activity.runOnUiThread {
            activity.launchSafFolderPicker()
        }
    }

    @JavascriptInterface
    fun shareSong(songId: Long) {
        bridgeScope.launch(Dispatchers.IO) {
            val song = try { db.songDao().getSongById(songId) } catch (_: Exception) { null } ?: return@launch
            val shareUri = try {
                val sourceUri = Uri.parse(song.uri)
                if (sourceUri.scheme == "file") {
                    val sourceFile = File(sourceUri.path.orEmpty())
                    if (!sourceFile.exists() || !sourceFile.canRead()) return@launch
                    val shareDir = File(activity.cacheDir, "shared").apply { mkdirs() }
                    shareDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(7)?.forEach { it.delete() }
                    val safeName = sourceFile.name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(180).ifBlank { "audio_file" }
                    val targetFile = File(shareDir, "${song.id}_$safeName")
                    sourceFile.inputStream().use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", targetFile)
                } else {
                    sourceUri
                }
            } catch (e: Exception) {
                Log.w("MusicBridge", "Share URI creation failed: ${e.message}")
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (destroyed || activity.isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)) return@withContext
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = song.mimeType.ifBlank { "audio/*" }
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, song.title)
                    putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    activity.startActivity(Intent.createChooser(shareIntent, "Şarkıyı Paylaş"))
                } catch (e: Exception) {
                    Log.w("MusicBridge", "Share activity launch failed: ${e.message}")
                }
            }
        }
    }

    @JavascriptInterface
    fun deleteSong(songId: Long): Boolean = runBlocking(Dispatchers.IO) {
        val song = db.songDao().getSongById(songId) ?: return@runBlocking false

        var mediaDeleted = false
        try {
            if (song.uri.startsWith("content://")) {
                mediaDeleted = activity.contentResolver.delete(Uri.parse(song.uri), null, null) > 0
            }
        } catch (e: SecurityException) {
            Log.w("MusicBridge", "MediaStore delete permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.w("MusicBridge", "MediaStore delete failed: ${e.message}")
        }

        // File deletion is only a fallback for legacy/file:// entries.
        if (!mediaDeleted && song.path.isNotBlank() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val file = File(song.path)
                mediaDeleted = !file.exists() || file.delete()
            } catch (e: Exception) {
                Log.w("MusicBridge", "Legacy file delete failed: ${e.message}")
            }
        }

        if (!mediaDeleted && song.uri.startsWith("content://")) return@runBlocking false

        db.songDao().deleteSongById(songId)
        if (libraryCacheReady) libraryCache = libraryCache.filterNot { it.id == songId }
        return@runBlocking true
    }

    @JavascriptInterface
    fun getEqualizerData(): String {
        val service = MusicPlaybackService.instance
        return service?.audioEffectManager?.getEqualizerData()?.toString() ?: JSONObject().apply {
            put("supported", false)
        }.toString()
    }

    @JavascriptInterface
    fun setEqualizerBandLevel(band: Int, level: Int) {
        MusicPlaybackService.instance?.audioEffectManager?.setBandLevel(band.toShort(), level.toShort())
    }

    @JavascriptInterface
    fun setEqualizerPreset(presetIndex: Int) {
        MusicPlaybackService.instance?.audioEffectManager?.usePreset(presetIndex.toShort())
    }

    @JavascriptInterface
    fun setBassBoost(strength: Int) {
        MusicPlaybackService.instance?.audioEffectManager?.setBassStrength(strength.toShort())
    }

    @JavascriptInterface
    fun setSleepTimer(minutes: Int) {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.startSleepTimer(minutes)
        }
    }

    @JavascriptInterface
    fun cancelSleepTimer() {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.cancelSleepTimer()
        }
    }

    @JavascriptInterface
    fun getSleepTimerRemaining(): Int {
        return MusicPlaybackService.instance?.getSleepTimerRemaining() ?: 0
    }

    @JavascriptInterface
    fun getDeviceAudioInfo(): String {
        val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "44100"
        val framesPerBuffer = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "256"

        val json = JSONObject().apply {
            put("sampleRate", sampleRate)
            put("framesPerBuffer", framesPerBuffer)
            put("isBluetoothA2dpOn", audioManager.isBluetoothA2dpOn)
            put("isWiredHeadsetOn", audioManager.isWiredHeadsetOn)
            put("isSpeakerphoneOn", audioManager.isSpeakerphoneOn)
            put("supportedFormats", JSONArray(listOf("MP3", "M4A", "AAC", "FLAC", "WAV", "OGG", "OPUS", "AMR", "3GP")))
        }
        return json.toString()
    }

    @JavascriptInterface
    fun getSetting(key: String, defaultVal: String): String = runBlocking(Dispatchers.IO) {
        val cleanKey = key.trim().takeIf { it.isNotEmpty() && it.length <= 128 } ?: return@runBlocking defaultVal
        return@runBlocking db.settingDao().getSetting(cleanKey) ?: defaultVal
    }

    @JavascriptInterface
    fun setSetting(key: String, value: String) = runBlocking(Dispatchers.IO) {
        val cleanKey = key.trim().takeIf { it.isNotEmpty() && it.length <= 128 } ?: return@runBlocking
        db.settingDao().setSetting(SettingEntity(cleanKey, value.take(4096)))
    }

    @JavascriptInterface
    fun fetchLyricsAsync(track: String, artist: String, album: String, durationSec: Int, requestId: Int) {
        lyricsJob?.cancel()
        lyricsJob = bridgeScope.launch(Dispatchers.IO) {
            val result = try {
                val cleanTrack = track.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
                val cleanArtist = artist.replace(Regex("(?i)feat\\..*|ft\\..*"), "").trim()
                if (cleanTrack.isBlank() || cleanArtist.isBlank()) {
                    JSONObject().put("error", "Lyrics not found").toString()
                } else {
                    val encodedTrack = java.net.URLEncoder.encode(cleanTrack, "UTF-8")
                    val encodedArtist = java.net.URLEncoder.encode(cleanArtist, "UTF-8")
                    val encodedAlbum = java.net.URLEncoder.encode(album.ifBlank { "" }, "UTF-8")
                    var url = "https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist"
                    if (album.isNotBlank() && !album.equals("Unknown Album", true) && !album.equals("Bilinmeyen Albüm", true)) {
                        url += "&album_name=$encodedAlbum"
                    }
                    if (durationSec > 0) url += "&duration=${durationSec.coerceIn(1, 86400)}"
                    val cacheKey = buildLyricsCacheKey(cleanTrack, cleanArtist, album, durationSec)
                    val cached = synchronized(lyricsCacheLock) { lyricsCache[cacheKey] }
                    if (cached != null) {
                        cached
                    } else {
                        val exact = httpGet(url)
                        val exactValidated = exact?.let {
                            try {
                                val item = JSONObject(it)
                                if (lyricsResultMatches(item, cleanTrack, cleanArtist, album, durationSec)) it else null
                            } catch (_: Exception) {
                                null
                            }
                        }

                        val selected = exactValidated ?: run {
                            val search = httpGet("https://lrclib.net/api/search?track_name=$encodedTrack&artist_name=$encodedArtist")
                            if (!search.isNullOrBlank()) {
                                try {
                                    val array = JSONArray(search)
                                    selectBestLyricsResult(array, cleanTrack, cleanArtist, album, durationSec)
                                } catch (_: Exception) {
                                    null
                                }
                            } else null
                        }

                        val finalResult = selected
                            ?: JSONObject().put("error", "Lyrics not found").toString()
                        if (!finalResult.contains("\"error\"")) {
                            synchronized(lyricsCacheLock) { lyricsCache[cacheKey] = finalResult }
                        }
                        finalResult
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                return@launch
            } catch (e: Exception) {
                Log.w("MusicBridge", "fetchLyrics failed: ${e.message}")
                JSONObject().put("error", e.message ?: "Network error").toString()
            }
            ensureActive()
            val quoted = JSONObject.quote(result)
            evalJs("window.onLyricsResult && window.onLyricsResult($requestId, $quoted)")
        }
    }


    private fun buildLyricsCacheKey(track: String, artist: String, album: String, durationSec: Int): String {
        return listOf(
            normalizeLyricsText(track),
            normalizeLyricsText(artist),
            normalizeLyricsText(album),
            durationSec.coerceAtLeast(0)
        ).joinToString("|")
    }

    private fun normalizeLyricsText(value: String): String {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(java.util.Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("\\b(feat|ft|featuring)\\.?\\b.*$"), "")
            .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun lyricsResultMatches(
        item: JSONObject,
        track: String,
        artist: String,
        album: String,
        durationSec: Int
    ): Boolean {
        val itemTrack = normalizeLyricsText(item.optString("trackName"))
        val itemArtist = normalizeLyricsText(item.optString("artistName"))
        if (itemTrack.isBlank() || itemArtist.isBlank()) return false

        val requestedTrack = normalizeLyricsText(track)
        val requestedArtist = normalizeLyricsText(artist)
        val trackMatches = itemTrack == requestedTrack ||
            itemTrack.contains(requestedTrack) || requestedTrack.contains(itemTrack)
        val artistMatches = itemArtist == requestedArtist ||
            itemArtist.contains(requestedArtist) || requestedArtist.contains(itemArtist)

        if (!trackMatches || !artistMatches) return false

        if (durationSec > 0) {
            val remoteDuration = item.optDouble("duration", -1.0)
            if (remoteDuration > 0 && kotlin.math.abs(remoteDuration - durationSec) > 8.0) return false
        }
        return item.optString("plainLyrics").isNotBlank() || item.optString("syncedLyrics").isNotBlank()
    }

    private fun selectBestLyricsResult(
        array: JSONArray,
        track: String,
        artist: String,
        album: String,
        durationSec: Int
    ): String? {
        val requestedTrack = normalizeLyricsText(track)
        val requestedArtist = normalizeLyricsText(artist)
        val requestedAlbum = normalizeLyricsText(album)
        var best: JSONObject? = null
        var bestScore = Int.MIN_VALUE

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (!lyricsResultMatches(item, track, artist, album, durationSec)) continue

            val remoteTrack = normalizeLyricsText(item.optString("trackName"))
            val remoteArtist = normalizeLyricsText(item.optString("artistName"))
            val remoteAlbum = normalizeLyricsText(item.optString("albumName"))
            var score = 0
            score += if (remoteTrack == requestedTrack) 60 else 35
            score += if (remoteArtist == requestedArtist) 35 else 20
            if (requestedAlbum.isNotBlank() && remoteAlbum.isNotBlank()) {
                score += if (remoteAlbum == requestedAlbum) 15 else if (
                    remoteAlbum.contains(requestedAlbum) || requestedAlbum.contains(remoteAlbum)
                ) 7 else -5
            }
            if (durationSec > 0) {
                val remoteDuration = item.optDouble("duration", -1.0)
                if (remoteDuration > 0) {
                    val diff = kotlin.math.abs(remoteDuration - durationSec)
                    score += when {
                        diff <= 2.0 -> 15
                        diff <= 5.0 -> 10
                        diff <= 10.0 -> 5
                        else -> -10
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score
                best = item
            }
        }

        return best?.toString()
    }

    private fun httpGet(urlString: String): String? {
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
