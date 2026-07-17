package com.arvin.player.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.arvin.player.data.model.RepeatMode
import com.arvin.player.data.model.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ConnectionState { CONNECTING, CONNECTED, FAILED }

/**
 * App-wide singleton that owns the MediaController connection to PlaybackService.
 * Every screen reads from the StateFlows here rather than talking to ExoPlayer directly.
 *
 * Also centralizes:
 *  - connection-state and playback-error reporting, so the UI can show real feedback
 *    instead of silently doing nothing when something goes wrong
 *  - a lightweight crossfade approximation: since this app uses a single ExoPlayer instance
 *    (not two mixed players), "crossfade" here means fading playback volume down over the
 *    last N ms of a track and back up over the first N ms of the next one, rather than true
 *    overlapping playback of two tracks. That's a real, audible effect and a common
 *    simplification, but it is not the same as a DJ-style overlap crossfade.
 */
class PlayerController private constructor(context: Context) {

    private var controller: MediaController? = null
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTING)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue

    private val _shuffleOn = MutableStateFlow(false)
    val shuffleOn: StateFlow<Boolean> = _shuffleOn

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    @Volatile private var crossfadeMs: Int = 0
    fun setCrossfadeMs(ms: Int) {
        crossfadeMs = ms
        // Real crossfading now happens inside CrossfadePlayer (two internal ExoPlayer instances
        // overlapping tracks), which lives in the PlaybackService process and reads this same
        // in-process singleton — see CrossfadeSettings.kt.
        CrossfadeSettings.crossfadeMs.value = ms
    }

    private var songLookup: (Long) -> Song? = { null }
    fun setSongLookup(lookup: (Long) -> Song?) { songLookup = lookup }

    fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        future.addListener({
            try {
                controller = future.get()
                _connectionState.value = ConnectionState.CONNECTED
                controller?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentSong.value = mediaItem?.mediaId?.toLongOrNull()?.let { songLookup(it) }
                        _durationMs.value = controller?.duration?.coerceAtLeast(0) ?: 0L
                    }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _shuffleOn.value = shuffleModeEnabled
                    }
                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                            else -> RepeatMode.OFF
                        }
                    }
                    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                        _playbackSpeed.value = playbackParameters.speed
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        // Corrupt/unreadable file, unsupported codec, missing file, etc.
                        // We don't crash or silently stall — surface a message and skip on.
                        _lastError.value = describeError(error)
                        controller?.let { c -> if (c.hasNextMediaItem()) c.seekToNext() else c.pause() }
                    }
                })
                startTicker()
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.FAILED
                _lastError.value = "Could not connect to the playback service: ${e.message}"
            }
        }, MoreExecutors.directExecutor())
    }

    private fun describeError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Track file is missing — skipping."
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "No permission to read this track — skipping."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED -> "This track's format isn't supported — skipping."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "This track appears corrupted — skipping."
        else -> "Playback error — skipping this track."
    }

    /** Call once the error has been shown to the user, so it doesn't reappear on recomposition. */
    fun clearError() { _lastError.value = null }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null) {
                    val pos = c.currentPosition
                    val dur = c.duration.coerceAtLeast(0)
                    _positionMs.value = pos
                    if (_durationMs.value != dur) _durationMs.value = dur
                }
                delay(300)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        _queue.value = songs
        val items = songs.map { toMediaItem(it) }
        controller?.setMediaItems(items, startIndex, 0L)
        controller?.prepare()
        controller?.play()
    }

    private fun toMediaItem(song: Song): MediaItem {
        // MediaStore-scanned songs store a raw filesystem path (e.g. /storage/emulated/0/Music/x.mp3)
        // and need an explicit file:// scheme for ExoPlayer to resolve them. Custom-folder songs
        // (added via Storage Access Framework) already carry a full content:// URI string.
        val uri = if (song.path.startsWith("content://") || song.path.startsWith("file://")) {
            android.net.Uri.parse(song.path)
        } else {
            android.net.Uri.fromFile(java.io.File(song.path))
        }
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setExtras(android.os.Bundle().apply { putLong("albumId", song.albumId) })
                    .build()
            )
            .build()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipNext() = controller?.seekToNext()
    fun skipPrevious() = controller?.seekToPrevious()
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs); _positionMs.value = positionMs }

    fun currentDurationMs(): Long = controller?.duration?.coerceAtLeast(0) ?: 0L

    fun toggleShuffle() {
        val newVal = !(controller?.shuffleModeEnabled ?: false)
        controller?.shuffleModeEnabled = newVal
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_ALL
            RepeatMode.ALL -> Player.REPEAT_MODE_ONE
            RepeatMode.ONE -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = next
    }

    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun pause() = controller?.pause()

    companion object {
        @Volatile private var instance: PlayerController? = null
        fun getInstance(context: Context): PlayerController =
            instance ?: synchronized(this) {
                instance ?: PlayerController(context).also { instance = it }
            }
    }
}
