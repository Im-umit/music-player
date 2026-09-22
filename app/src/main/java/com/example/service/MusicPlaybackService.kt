package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppMusicDatabase
import com.example.data.db.entity.PlayHistoryEntity
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private var sleepTimerJob: Job? = null
    private var sleepTimerRemainingSeconds: Int = 0

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

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
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

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                notifyStateChanged()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    audioEffectManager.attachAudioSession(player.audioSessionId)
                }
                notifyStateChanged()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentMediaItemIndex = player.currentMediaItemIndex
                if (currentMediaItemIndex in _currentQueue.indices && currentMediaItemIndex != _currentIndex) {
                    _currentIndex = currentMediaItemIndex
                    recordPlayHistory()
                }
                notifyStateChanged()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                notifyStateChanged()
            }
        })
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

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return

        _currentQueue.clear()
        _currentQueue.addAll(songs)
        _currentIndex = startIndex.coerceIn(0, songs.size - 1)

        val mediaItems = songs.map { song ->
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

        player.setMediaItems(mediaItems, _currentIndex, 0L)
        player.prepare()
        player.play()

        recordPlayHistory()
        notifyStateChanged()
    }

    fun playSingleSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun addToQueueNext(song: Song) {
        val nextIdx = (_currentIndex + 1).coerceIn(0, _currentQueue.size)
        _currentQueue.add(nextIdx, song)

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(if (song.albumArtUri.isNotBlank()) Uri.parse(song.albumArtUri) else null)
            .build()

        val item = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadata)
            .build()

        player.addMediaItem(nextIdx, item)
        notifyStateChanged()
    }

    fun addToQueueEnd(song: Song) {
        _currentQueue.add(song)

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(if (song.albumArtUri.isNotBlank()) Uri.parse(song.albumArtUri) else null)
            .build()

        val item = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadata)
            .build()

        player.addMediaItem(item)
        notifyStateChanged()
    }

    fun removeFromQueue(index: Int) {
        if (index in _currentQueue.indices) {
            _currentQueue.removeAt(index)
            player.removeMediaItem(index)
            if (index < _currentIndex) {
                _currentIndex--
            }
            notifyStateChanged()
        }
    }

    fun clearQueue() {
        _currentQueue.clear()
        _currentIndex = -1
        player.stop()
        player.clearMediaItems()
        notifyStateChanged()
    }

    fun resume() {
        if (player.playbackState == Player.STATE_IDLE && _currentQueue.isNotEmpty()) {
            player.prepare()
        }
        player.play()
        notifyStateChanged()
    }

    fun pause() {
        player.pause()
        notifyStateChanged()
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.repeatMode == Player.REPEAT_MODE_ALL && _currentQueue.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
        notifyStateChanged()
    }

    fun previous() {
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (_currentQueue.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
        notifyStateChanged()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        notifyStateChanged()
    }

    fun seekBy(deltaMs: Long) {
        val current = player.currentPosition
        val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
        val target = (current + deltaMs).coerceIn(0L, duration)
        player.seekTo(target)
        notifyStateChanged()
    }

    fun toggleShuffle(): Boolean {
        val newShuffle = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newShuffle
        notifyStateChanged()
        return newShuffle
    }

    fun setRepeatMode(mode: Int) {
        player.repeatMode = when (mode) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        notifyStateChanged()
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        player.playbackParameters = PlaybackParameters(safeSpeed)
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
        json.put("isPlaying", player.isPlaying)
        json.put("playbackState", player.playbackState)
        json.put("position", player.currentPosition)
        json.put("duration", player.duration.coerceAtLeast(0L))
        json.put("isShuffle", player.shuffleModeEnabled)
        json.put("repeatMode", player.repeatMode) // 0: OFF, 1: ONE, 2: ALL in ExoPlayer
        json.put("playbackSpeed", player.playbackParameters.speed)
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

    override fun onDestroy() {
        instance = null
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
