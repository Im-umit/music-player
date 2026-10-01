package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppMusicDatabase
import com.example.data.db.entity.PlayHistoryEntity
import com.example.data.model.Song
import com.example.util.AlbumArtManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set
    lateinit var audioEffectManager: AudioEffectManager
        private set

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sleepTimerJob: Job? = null
    private var progressJob: Job? = null
    private var sleepTimerRemainingSeconds: Int = 0
    private var currentArtworkRequestId: Long = 0L

    // Thread-safe cached player state
    @Volatile
    private var cachedPosition: Long = 0L

    @Volatile
    private var cachedDuration: Long = 0L

    @Volatile
    private var cachedIsPlaying: Boolean = false

    @Volatile
    private var cachedPlaybackState: Int = Player.STATE_IDLE

    @Volatile
    var cachedShuffle: Boolean = false

    @Volatile
    var cachedRepeat: Int = Player.REPEAT_MODE_OFF

    @Volatile
    private var cachedSpeed: Float = 1.0f

    // Playback state cache
    private val _currentQueue = mutableListOf<Song>()
    val currentQueue: List<Song> get() = _currentQueue

    private var _currentIndex: Int = -1
    val currentIndex: Int get() = _currentIndex

    val currentSong: Song?
        get() = if (_currentIndex in _currentQueue.indices) _currentQueue[_currentIndex] else null

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var instance: MusicPlaybackService? = null
            private set

        private val _playbackStateFlow = MutableStateFlow<JSONObject?>(null)
        val playbackStateFlow = _playbackStateFlow.asStateFlow()

        var onStateChangeListener: ((JSONObject) -> Unit)? = null
    }

    private fun updateCachedPlayerState() {
        if (!::player.isInitialized) return
        cachedIsPlaying = player.isPlaying
        cachedPlaybackState = player.playbackState
        cachedPosition = player.currentPosition.coerceAtLeast(0L)
        val pDur = player.duration
        val sDur = currentSong?.duration ?: 0L
        cachedDuration = if (pDur > 0) pDur else sDur.coerceAtLeast(0L)
        cachedShuffle = player.shuffleModeEnabled
        cachedRepeat = player.repeatMode
        cachedSpeed = player.playbackParameters.speed
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive && ::player.isInitialized && player.isPlaying) {
                updateCachedPlayerState()
                notifyStateChanged()
                delay(250L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
        updateCachedPlayerState()
        notifyStateChanged()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        audioEffectManager = AudioEffectManager(this)

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: @Player.Command Int
            ): Int {
                when (playerCommand) {
                    Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        mainHandler.post { next() }
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        mainHandler.post { previous() }
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_PLAY_PAUSE -> {
                        mainHandler.post {
                            if (player.isPlaying) pause() else resume()
                        }
                        return SessionResult.RESULT_SUCCESS
                    }
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                @Suppress("DEPRECATION")
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            mainHandler.post { next() }
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            mainHandler.post { previous() }
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                            mainHandler.post {
                                if (player.isPlaying) pause() else resume()
                            }
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            mainHandler.post { resume() }
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                            mainHandler.post { pause() }
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateCachedPlayerState()
                if (isPlaying) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                    savePlaybackState()
                }
                notifyStateChanged()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    audioEffectManager.attachAudioSession(player.audioSessionId)
                    updateCachedPlayerState()
                } else if (playbackState == Player.STATE_ENDED) {
                    stopProgressUpdates()
                }
                savePlaybackState()
                notifyStateChanged()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentMediaItemIndex = player.currentMediaItemIndex
                if (currentMediaItemIndex in _currentQueue.indices && currentMediaItemIndex != _currentIndex) {
                    _currentIndex = currentMediaItemIndex
                    recordPlayHistory()
                }
                updateCachedPlayerState()
                savePlaybackState()
                notifyStateChanged()
                syncCurrentTrackArtworkAsync()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updateCachedPlayerState()
                notifyStateChanged()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MusicPlaybackService", "PlaybackException: ${error.errorCodeName} (${error.errorCode}): ${error.message}", error)
                if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
                ) {
                    try {
                        val currentPos = player.currentPosition
                        player.prepare()
                        player.seekTo(currentPos)
                    } catch (e: Exception) {
                        Log.w("MusicPlaybackService", "Recovery after player error failed: ${e.message}")
                    }
                }
                updateCachedPlayerState()
                notifyStateChanged()
            }
        })

        // Restore last playback state if available
        restoreSavedPlaybackState()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }
    private val localBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        val superBinder = super.onBind(intent)
        return superBinder ?: localBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int, startWithShuffle: Boolean = false) {
        if (songs.isEmpty()) return

        _currentQueue.clear()
        _currentQueue.addAll(songs)
        _currentIndex = startIndex.coerceIn(0, songs.size - 1)

        val currentArtBytes = AlbumArtManager.getArtworkBytes(this, songs[_currentIndex].id)

        val mediaItems = songs.mapIndexed { idx, song ->
            val albumTitle = if (song.album.isNotBlank()) song.album else "Unknown Album"
            val artistName = if (song.artist.isNotBlank()) song.artist else "Unknown Artist"
            val songTitle = if (song.title.isNotBlank()) song.title else "Unknown Track"

            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(songTitle)
                .setArtist(artistName)
                .setAlbumTitle(albumTitle)

            if (idx == _currentIndex && currentArtBytes != null && currentArtBytes.isNotEmpty()) {
                metadataBuilder.setArtworkData(currentArtBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }

            if (song.albumId > 0) {
                metadataBuilder.setArtworkUri(
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
                )
            } else if (song.albumArtUri.startsWith("content://") || song.albumArtUri.startsWith("file://")) {
                metadataBuilder.setArtworkUri(Uri.parse(song.albumArtUri))
            }

            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(metadataBuilder.build())
                .build()
        }

        if (startWithShuffle) {
            player.shuffleModeEnabled = true
        }
        player.setMediaItems(mediaItems, _currentIndex, 0L)
        player.prepare()
        player.play()

        val song = songs[_currentIndex]
        cachedPosition = 0L
        cachedDuration = song.duration.coerceAtLeast(0L)
        cachedIsPlaying = true
        cachedPlaybackState = Player.STATE_READY
        cachedShuffle = player.shuffleModeEnabled

        recordPlayHistory()
        startProgressUpdates()
        notifyStateChanged()
        syncCurrentTrackArtworkAsync()
    }

    private fun syncCurrentTrackArtworkAsync() {
        val song = currentSong ?: return
        val songId = song.id
        val reqId = ++currentArtworkRequestId

        serviceScope.launch(Dispatchers.IO) {
            val bytes = AlbumArtManager.getArtworkBytes(this@MusicPlaybackService, songId)
            withContext(Dispatchers.Main) {
                // Ensure the artwork still belongs to the currently playing track
                if (reqId == currentArtworkRequestId && currentSong?.id == songId && ::player.isInitialized) {
                    val activeItem = player.currentMediaItem
                    if (activeItem != null && activeItem.mediaId == songId.toString()) {
                        val currentMeta = activeItem.mediaMetadata
                        val hasArtwork = currentMeta.artworkData != null && currentMeta.artworkData!!.isNotEmpty()
                        if (!hasArtwork && bytes != null && bytes.isNotEmpty()) {
                            val newMeta = currentMeta.buildUpon()
                                .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                .build()
                            player.playlistMetadata = newMeta
                        }
                    }
                }
            }
        }
    }

    fun playAllShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        val randomIndex = (0 until songs.size).random()
        playQueue(songs, randomIndex, startWithShuffle = true)
    }

    fun playSingleSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun addToQueueNext(song: Song) {
        val nextIdx = (_currentIndex + 1).coerceIn(0, _currentQueue.size)
        _currentQueue.add(nextIdx, song)

        val albumTitle = if (song.album.isNotBlank()) song.album else "Unknown Album"
        val artistName = if (song.artist.isNotBlank()) song.artist else "Unknown Artist"
        val songTitle = if (song.title.isNotBlank()) song.title else "Unknown Track"

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(songTitle)
            .setArtist(artistName)
            .setAlbumTitle(albumTitle)

        if (song.albumId > 0) {
            metadataBuilder.setArtworkUri(
                ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
            )
        } else if (song.albumArtUri.startsWith("content://") || song.albumArtUri.startsWith("file://")) {
            metadataBuilder.setArtworkUri(Uri.parse(song.albumArtUri))
        }

        val item = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        player.addMediaItem(nextIdx, item)
        updateCachedPlayerState()
        notifyStateChanged()
    }

    fun addToQueueEnd(song: Song) {
        _currentQueue.add(song)

        val albumTitle = if (song.album.isNotBlank()) song.album else "Unknown Album"
        val artistName = if (song.artist.isNotBlank()) song.artist else "Unknown Artist"
        val songTitle = if (song.title.isNotBlank()) song.title else "Unknown Track"

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(songTitle)
            .setArtist(artistName)
            .setAlbumTitle(albumTitle)

        if (song.albumId > 0) {
            metadataBuilder.setArtworkUri(
                ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
            )
        } else if (song.albumArtUri.startsWith("content://") || song.albumArtUri.startsWith("file://")) {
            metadataBuilder.setArtworkUri(Uri.parse(song.albumArtUri))
        }

        val item = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        player.addMediaItem(item)
        updateCachedPlayerState()
        notifyStateChanged()
    }

    fun removeFromQueue(index: Int) {
        if (index in _currentQueue.indices) {
            _currentQueue.removeAt(index)
            player.removeMediaItem(index)
            if (index < _currentIndex) {
                _currentIndex--
            }
            updateCachedPlayerState()
            notifyStateChanged()
        }
    }

    fun clearQueue() {
        stopProgressUpdates()
        _currentQueue.clear()
        _currentIndex = -1
        player.stop()
        player.clearMediaItems()
        cachedPosition = 0L
        cachedDuration = 0L
        cachedIsPlaying = false
        notifyStateChanged()
    }

    fun resume() {
        if (player.playbackState == Player.STATE_IDLE && _currentQueue.isNotEmpty()) {
            player.prepare()
        } else if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0, 0L)
            player.prepare()
        }
        player.play()
        updateCachedPlayerState()
        startProgressUpdates()
        notifyStateChanged()
    }

    fun pause() {
        player.pause()
        stopProgressUpdates()
        notifyStateChanged()
    }

    fun next() {
        if (_currentQueue.isEmpty()) return

        if (player.repeatMode == Player.REPEAT_MODE_ONE) {
            player.seekTo(0L)
            player.play()
        } else if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        } else if (player.repeatMode == Player.REPEAT_MODE_ALL) {
            val firstIdx = if (player.shuffleModeEnabled && player.currentTimeline.windowCount > 0) {
                player.currentTimeline.getFirstWindowIndex(true)
            } else 0
            player.seekToDefaultPosition(firstIdx)
            player.play()
        }
        updateCachedPlayerState()
        savePlaybackState()
        notifyStateChanged()
    }

    fun previous() {
        if (_currentQueue.isEmpty()) return

        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            player.play()
        } else if (player.repeatMode == Player.REPEAT_MODE_ONE) {
            player.seekTo(0L)
            player.play()
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else if (player.repeatMode == Player.REPEAT_MODE_ALL) {
            val lastIdx = if (player.shuffleModeEnabled && player.currentTimeline.windowCount > 0) {
                player.currentTimeline.getLastWindowIndex(true)
            } else (_currentQueue.size - 1).coerceAtLeast(0)
            player.seekToDefaultPosition(lastIdx)
            player.play()
        } else {
            player.seekTo(0L)
        }
        updateCachedPlayerState()
        savePlaybackState()
        notifyStateChanged()
    }

    fun seekTo(positionMs: Long) {
        val sDur = currentSong?.duration ?: 0L
        val maxDur = if (player.duration > 0) player.duration else if (cachedDuration > 0) cachedDuration else sDur
        val safeTarget = if (maxDur > 0) positionMs.coerceIn(0L, maxDur) else positionMs.coerceAtLeast(0L)
        player.seekTo(safeTarget)
        cachedPosition = safeTarget
        updateCachedPlayerState()
        notifyStateChanged()
    }

    fun seekBy(deltaMs: Long) {
        val current = player.currentPosition.coerceAtLeast(0L)
        val sDur = currentSong?.duration ?: 0L
        val maxDur = if (player.duration > 0) player.duration else if (cachedDuration > 0) cachedDuration else sDur
        val safeMax = if (maxDur > 0) maxDur else Long.MAX_VALUE
        val safeTarget = (current + deltaMs).coerceIn(0L, safeMax)
        player.seekTo(safeTarget)
        cachedPosition = safeTarget
        updateCachedPlayerState()
        notifyStateChanged()
    }

    fun toggleShuffle(): Boolean {
        val newShuffle = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newShuffle
        updateCachedPlayerState()
        savePlaybackState()
        notifyStateChanged()
        return newShuffle
    }

    fun setRepeatMode(mode: Int) {
        player.repeatMode = when (mode) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        updateCachedPlayerState()
        savePlaybackState()
        notifyStateChanged()
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        player.playbackParameters = PlaybackParameters(safeSpeed)
        updateCachedPlayerState()
        notifyStateChanged()
    }

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return

        sleepTimerRemainingSeconds = minutes * 60
        sleepTimerJob = serviceScope.launch {
            while (sleepTimerRemainingSeconds > 0) {
                delay(1000L)
                sleepTimerRemainingSeconds--

                // Fade out in last 10 seconds
                if (sleepTimerRemainingSeconds in 1..10) {
                    val volume = sleepTimerRemainingSeconds / 10.0f
                    player.volume = volume
                }

                if (sleepTimerRemainingSeconds == 0) {
                    player.pause()
                    player.volume = 1.0f
                    break
                }
            }
            notifyStateChanged()
        }
        notifyStateChanged()
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerRemainingSeconds = 0
        player.volume = 1.0f
        notifyStateChanged()
    }

    fun getSleepTimerRemaining(): Int {
        return sleepTimerRemainingSeconds
    }

    private fun recordPlayHistory() {
        val song = currentSong ?: return
        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppMusicDatabase.getInstance(this@MusicPlaybackService)
                db.historyDao().insertHistory(PlayHistoryEntity(songId = song.id))
                db.songDao().incrementPlayCount(song.id, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e("MusicService", "Error recording play history: ${e.message}")
            }
        }
    }

    fun getCurrentPlaybackStateJson(): JSONObject {
        val json = JSONObject()
        val song = currentSong
        json.put("hasSong", song != null)
        json.put("currentSong", song?.toJsonObject() ?: JSONObject.NULL)

        if (Looper.myLooper() == Looper.getMainLooper() && ::player.isInitialized) {
            updateCachedPlayerState()
        }

        val pos = cachedPosition
        val dur = if (cachedDuration > 0) cachedDuration else (song?.duration?.coerceAtLeast(0L) ?: 0L)

        json.put("isPlaying", cachedIsPlaying)
        json.put("playbackState", cachedPlaybackState)
        json.put("position", pos)
        json.put("duration", dur)
        val uiRepeat = when (if (::player.isInitialized) player.repeatMode else cachedRepeat) {
            Player.REPEAT_MODE_ALL -> 1
            Player.REPEAT_MODE_ONE -> 2
            else -> 0
        }
        json.put("isShuffle", cachedShuffle)
        json.put("repeatMode", uiRepeat) // 0: OFF, 1: ALL, 2: ONE
        json.put("playbackSpeed", cachedSpeed)
        json.put("currentIndex", _currentIndex)
        json.put("queueLength", _currentQueue.size)
        json.put("sleepTimerRemaining", sleepTimerRemainingSeconds)

        val queueArray = JSONArray()
        _currentQueue.forEach { queueArray.put(it.toJsonObject()) }
        json.put("queue", queueArray)

        return json
    }

    private fun notifyStateChanged() {
        val state = getCurrentPlaybackStateJson()
        _playbackStateFlow.value = state
        onStateChangeListener?.invoke(state)
    }

    fun savePlaybackState() {
        try {
            val prefs = getSharedPreferences("music_player_state", Context.MODE_PRIVATE)
            val queueJson = JSONArray().apply {
                _currentQueue.forEach { put(it.toJsonObject()) }
            }.toString()
            val pos = if (cachedPosition > 0) cachedPosition else if (::player.isInitialized && player.currentPosition > 0) player.currentPosition else 0L
            val uiRepeat = when (if (::player.isInitialized) player.repeatMode else cachedRepeat) {
                Player.REPEAT_MODE_ALL -> 1
                Player.REPEAT_MODE_ONE -> 2
                else -> 0
            }
            prefs.edit()
                .putString("last_queue", queueJson)
                .putInt("last_index", _currentIndex)
                .putLong("last_position", pos)
                .putBoolean("last_shuffle", cachedShuffle)
                .putInt("last_repeat", uiRepeat)
                .apply()
        } catch (e: Exception) {
            Log.e("MusicService", "Error saving playback state: ${e.message}")
        }
    }

    fun restoreSavedPlaybackState() {
        try {
            val prefs = getSharedPreferences("music_player_state", Context.MODE_PRIVATE)
            val queueStr = prefs.getString("last_queue", null) ?: return
            if (queueStr.isBlank()) return
            val array = JSONArray(queueStr)
            if (array.length() == 0) return
            val restoredSongs = mutableListOf<Song>()
            for (i in 0 until array.length()) {
                restoredSongs.add(Song.fromJsonObject(array.getJSONObject(i)))
            }
            if (restoredSongs.isEmpty()) return

            val savedIndex = prefs.getInt("last_index", 0).coerceIn(0, restoredSongs.size - 1)
            val savedPos = prefs.getLong("last_position", 0L)
            val savedShuffle = prefs.getBoolean("last_shuffle", false)
            val savedRepeat = prefs.getInt("last_repeat", 0)

            _currentQueue.clear()
            _currentQueue.addAll(restoredSongs)
            _currentIndex = savedIndex

            val mediaItems = restoredSongs.map { song ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(if (song.albumArtUri.isNotBlank()) Uri.parse(song.albumArtUri) else null)
                    .build()

                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(song.uri)
                    .setMediaMetadata(metadata)
                    .build()
            }

            player.repeatMode = when (savedRepeat) {
                1 -> Player.REPEAT_MODE_ALL
                2 -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            player.shuffleModeEnabled = savedShuffle
            player.setMediaItems(mediaItems, _currentIndex, savedPos)
            player.prepare()
            player.pause()

            cachedPosition = savedPos
            val song = restoredSongs[savedIndex]
            cachedDuration = song.duration.coerceAtLeast(0L)
            cachedIsPlaying = false
            cachedShuffle = savedShuffle
            cachedRepeat = player.repeatMode
            cachedPlaybackState = Player.STATE_READY

            notifyStateChanged()
        } catch (e: Exception) {
            Log.e("MusicService", "Error restoring saved playback state: ${e.message}")
        }
    }

    override fun onDestroy() {
        stopProgressUpdates()
        instance = null
        savePlaybackState()
        cancelSleepTimer()
        audioEffectManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
