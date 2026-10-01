package com.example.bridge

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.MainActivity
import com.example.data.db.AppMusicDatabase
import com.example.data.db.entity.FolderEntity
import com.example.data.db.entity.PlayHistoryEntity
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.PlaylistSongEntity
import com.example.data.db.entity.SettingEntity
import com.example.data.mediastore.MediaStoreScanner
import com.example.data.model.Song
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MusicBridge(
    private val activity: MainActivity,
    private val webView: WebView
) {
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val db = AppMusicDatabase.getInstance(activity)
    private val scanner = MediaStoreScanner(activity)

    init {
        // Listen to service playback state updates and notify WebView
        MusicPlaybackService.onStateChangeListener = { stateJson ->
            notifyWebPlaybackState(stateJson)
        }
    }

    fun notifyWebPlaybackState(stateJson: JSONObject) {
        activity.runOnUiThread {
            val jsonString = stateJson.toString()
            webView.evaluateJavascript("window.onPlaybackStateUpdated && window.onPlaybackStateUpdated($jsonString)", null)
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
    fun scanLibrary(): String {
        bridgeScope.launch {
            try {
                if (!activity.hasAudioPermission()) {
                    activity.runOnUiThread {
                        activity.requestAudioPermission()
                    }
                    return@launch
                }
                val songs = scanner.scanMediaStore()
                activity.runOnUiThread {
                    webView.evaluateJavascript("window.onScanCompleted && window.onScanCompleted(${songs.size})", null)
                }
            } catch (e: Exception) {
                Log.e("MusicBridge", "scanLibrary error: ${e.message}")
                activity.runOnUiThread {
                    webView.evaluateJavascript("window.onScanFailed && window.onScanFailed('${e.message}')", null)
                }
            }
        }
        return "STARTED"
    }

    @JavascriptInterface
    fun getSongs(sortOrder: String): String = runBlocking(Dispatchers.IO) {
        val cleanOrder = sortOrder.trim().lowercase()
        var list = when (cleanOrder) {
            "title_asc", "title" -> db.songDao().getAllSongsAsc()
            "title_desc" -> db.songDao().getAllSongsDesc()
            "artist_asc", "artist" -> db.songDao().getAllSongsByArtistAsc()
            "artist_desc" -> db.songDao().getAllSongsByArtistDesc()
            "album_asc", "album" -> db.songDao().getAllSongsByAlbumAsc()
            "album_desc" -> db.songDao().getAllSongsByAlbumDesc()
            "date_added_desc", "date_added", "date", "date_desc" -> db.songDao().getAllSongsByDateAddedDesc()
            "date_added_asc", "date_asc" -> db.songDao().getAllSongsByDateAddedAsc()
            "duration_desc", "duration" -> db.songDao().getAllSongsByDurationDesc()
            "duration_asc" -> db.songDao().getAllSongsByDurationAsc()
            "size_desc", "size" -> db.songDao().getAllSongsBySizeDesc()
            "size_asc" -> db.songDao().getAllSongsBySizeAsc()
            "play_count_desc", "play_count", "play" -> db.songDao().getAllSongsByPlayCountDesc()
            "play_count_asc" -> db.songDao().getAllSongsByPlayCountAsc()
            "year_desc", "year" -> db.songDao().getAllSongsByYearDesc()
            "year_asc" -> db.songDao().getAllSongsByYearAsc()
            "folder_asc", "folder" -> db.songDao().getAllSongsByFolderAsc()
            "folder_desc" -> db.songDao().getAllSongsByFolderDesc()
            else -> db.songDao().getAllSongsAsc()
        }

        if (list.isEmpty()) {
            com.example.util.SampleMusicInstaller.installSampleMusic(activity)
            list = db.songDao().getAllSongsAsc()
        }

        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun searchSongs(query: String): String = runBlocking(Dispatchers.IO) {
        val results = db.songDao().searchSongs(query)
        val jsonArray = JSONArray()
        results.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun playSong(songId: Long, queueJson: String, index: Int) {
        activity.runOnUiThread {
            activity.ensureServiceStarted()
            val service = MusicPlaybackService.instance
            if (service != null) {
                startQueuePlayback(service, queueJson, index)
            } else {
                bridgeScope.launch {
                    var retries = 0
                    while (MusicPlaybackService.instance == null && retries < 15) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                    activity.runOnUiThread {
                        MusicPlaybackService.instance?.let { s ->
                            startQueuePlayback(s, queueJson, index)
                        }
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun playAllShuffled(queueJson: String) {
        activity.runOnUiThread {
            activity.ensureServiceStarted()
            val service = MusicPlaybackService.instance
            if (service != null) {
                startShuffledPlayback(service, queueJson)
            } else {
                bridgeScope.launch {
                    var retries = 0
                    while (MusicPlaybackService.instance == null && retries < 15) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                    activity.runOnUiThread {
                        MusicPlaybackService.instance?.let { s ->
                            startShuffledPlayback(s, queueJson)
                        }
                    }
                }
            }
        }
    }

    private fun startShuffledPlayback(service: MusicPlaybackService, queueJson: String) {
        try {
            val array = JSONArray(queueJson)
            val songs = mutableListOf<Song>()
            for (i in 0 until array.length()) {
                songs.add(Song.fromJsonObject(array.getJSONObject(i)))
            }
            service.playAllShuffled(songs)
        } catch (e: Exception) {
            Log.e("MusicBridge", "playAllShuffled failed: ${e.message}")
        }
    }

    private fun startQueuePlayback(service: MusicPlaybackService, queueJson: String, index: Int) {
        try {
            val array = JSONArray(queueJson)
            val songs = mutableListOf<Song>()
            for (i in 0 until array.length()) {
                songs.add(Song.fromJsonObject(array.getJSONObject(i)))
            }
            service.playQueue(songs, index)
        } catch (e: Exception) {
            Log.e("MusicBridge", "playSong failed: ${e.message}")
        }
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
            MusicPlaybackService.instance?.seekTo(positionMs)
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
            MusicPlaybackService.instance?.setRepeatMode(mode)
        }
    }

    @JavascriptInterface
    fun setPlaybackSpeed(speed: Float) {
        activity.runOnUiThread {
            MusicPlaybackService.instance?.setPlaybackSpeed(speed)
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
        val playlists = db.playlistDao().getAllPlaylists()
        val jsonArray = JSONArray()
        playlists.forEach {
            val count = db.playlistDao().getPlaylistSongCount(it.id)
            jsonArray.put(it.toJsonObject(count))
        }
        return@runBlocking jsonArray.toString()
    }

    @JavascriptInterface
    fun createPlaylist(name: String): Long = runBlocking(Dispatchers.IO) {
        val playlist = PlaylistEntity(name = name)
        return@runBlocking db.playlistDao().insertPlaylist(playlist)
    }

    @JavascriptInterface
    fun renamePlaylist(id: Long, name: String) = runBlocking(Dispatchers.IO) {
        db.playlistDao().updatePlaylistName(id, name)
    }

    @JavascriptInterface
    fun deletePlaylist(id: Long) = runBlocking(Dispatchers.IO) {
        db.playlistDao().deletePlaylistById(id)
    }

    @JavascriptInterface
    fun addSongToPlaylist(playlistId: Long, songId: Long) = runBlocking(Dispatchers.IO) {
        val count = db.playlistDao().getPlaylistSongCount(playlistId)
        db.playlistDao().addSongToPlaylist(PlaylistSongEntity(playlistId, songId, count))
    }

    @JavascriptInterface
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) = runBlocking(Dispatchers.IO) {
        db.playlistDao().removeSongFromPlaylist(playlistId, songId)
    }

    @JavascriptInterface
    fun getPlaylistSongs(playlistId: Long): String = runBlocking(Dispatchers.IO) {
        val songs = db.playlistDao().getSongsInPlaylist(playlistId)
        val jsonArray = JSONArray()
        songs.forEach { jsonArray.put(it.toSong().toJsonObject()) }
        return@runBlocking jsonArray.toString()
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
    fun getAlbumSongs(album: String): String = runBlocking(Dispatchers.IO) {
        val songs = db.songDao().getSongsByAlbumName(album)
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
        activity.runOnUiThread {
            bridgeScope.launch(Dispatchers.IO) {
                val song = db.songDao().getSongById(songId) ?: return@launch
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = song.mimeType
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(song.uri))
                        putExtra(Intent.EXTRA_SUBJECT, song.title)
                        putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "Şarkıyı Paylaş"))
                }
            }
        }
    }

    @JavascriptInterface
    fun deleteSong(songId: Long): Boolean = runBlocking(Dispatchers.IO) {
        val song = db.songDao().getSongById(songId) ?: return@runBlocking false
        db.songDao().deleteSongById(songId)
        if (song.path.isNotBlank()) {
            try {
                val file = File(song.path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.w("MusicBridge", "Cannot delete physical file: ${e.message}")
            }
        }
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
        return@runBlocking db.settingDao().getSetting(key) ?: defaultVal
    }

    @JavascriptInterface
    fun setSetting(key: String, value: String) = runBlocking(Dispatchers.IO) {
        db.settingDao().setSetting(SettingEntity(key, value))
    }

    @JavascriptInterface
    fun fetchLyrics(track: String, artist: String, album: String, durationSec: Int): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val cleanTrack = track.replace(Regex("\\(.*\\)|\\[.*\\]"), "").trim()
                val cleanArtist = artist.replace(Regex("(?i)feat\\..*|ft\\..*"), "").trim()

                if (cleanTrack.contains("Neon Horizon", ignoreCase = true) || cleanArtist.contains("Cyberwave", ignoreCase = true)) {
                    val lrcJson = JSONObject().apply {
                        put("trackName", "Neon Horizon")
                        put("artistName", "Cyberwave")
                        put("albumName", "Neon City Nights")
                        put("duration", 75)
                        put("syncedLyrics", "[00:00.00]♪ Cyberwave - Neon Horizon ♪\n[00:04.00]Gecenin ışıkları neon ufukta parlıyor\n[00:11.00]Ritim akıyor, şehir canlanıyor\n[00:19.00]Karanlık caddelerde melodiler dans ediyor\n[00:27.00]Neon çizgiler hızla akıp gidiyor\n[00:35.00]Synth dalgaları havayı sarıyor\n[00:43.00]Sonsuz bir ritim, saf enerji\n[00:51.00]Gece hiçbir zaman bitmeyecek gibi\n[00:59.00]Gözlerini kapat ve müziği hisset\n[01:07.00]Neon ufukta parlayan melodiler...\n[01:14.00]♪")
                    }
                    return@runBlocking lrcJson.toString()
                }

                val encodedTrack = java.net.URLEncoder.encode(cleanTrack.ifBlank { track }, "UTF-8")
                val encodedArtist = java.net.URLEncoder.encode(cleanArtist.ifBlank { artist }, "UTF-8")
                val encodedAlbum = java.net.URLEncoder.encode(album.ifBlank { "" }, "UTF-8")

                var urlStr = "https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist"
                if (album.isNotBlank() && !album.equals("Unknown Album", ignoreCase = true) && !album.equals("Bilinmeyen Albüm", ignoreCase = true)) {
                    urlStr += "&album_name=$encodedAlbum"
                }
                if (durationSec > 0) {
                    urlStr += "&duration=$durationSec"
                }

                val exactResult = httpGet(urlStr)
                if (exactResult != null && exactResult.isNotBlank()) {
                    return@runBlocking exactResult
                }

                // Fallback to LRCLIB search endpoint
                val searchUrl = "https://lrclib.net/api/search?track_name=$encodedTrack&artist_name=$encodedArtist"
                val searchResult = httpGet(searchUrl)
                if (searchResult != null && searchResult.isNotBlank()) {
                    val array = JSONArray(searchResult)
                    if (array.length() > 0) {
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            if (item.has("syncedLyrics") && !item.isNull("syncedLyrics") && item.getString("syncedLyrics").isNotBlank()) {
                                return@runBlocking item.toString()
                            }
                        }
                        return@runBlocking array.getJSONObject(0).toString()
                    }
                }

                return@runBlocking JSONObject().apply {
                    put("error", "Lyrics not found")
                }.toString()
            } catch (e: Exception) {
                Log.w("MusicBridge", "fetchLyrics failed: ${e.message}")
                return@runBlocking JSONObject().apply {
                    put("error", e.message ?: "Network error")
                }.toString()
            }
        }
    }

    private fun httpGet(urlStr: String): String? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val url = java.net.URL(urlStr)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.setRequestProperty("User-Agent", "NeonMusicPlayer/1.0 (Android)")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
