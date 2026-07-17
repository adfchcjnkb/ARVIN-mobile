package com.arvin.player.media

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * A genuine dual-ExoPlayer crossfade engine, exposed as a single [Player] (via [SimpleBasePlayer])
 * so it can be bound directly to a [androidx.media3.session.MediaSession] like any other player.
 *
 * How it actually crossfades: two real [ExoPlayer] instances ("A" and "B") are held internally.
 * Only one is "active" (audible, driving the reported playback position) at a time. When the
 * active player gets within `crossfadeMs` of the end of the current track, the standby player is
 * primed with the *next* track and starts playing **at the same time** as the active player is
 * still finishing the current one — actual overlapping audio, not just a volume ramp on one
 * stream. Over that window both players' volumes are cross-faded (one down, one up). At the
 * end of that window we flip which player is "active" and continue.
 *
 * This is meaningfully more complex than a single-player volume fade (the earlier approximation),
 * and it is the least-verified file in this project — it was written against the documented
 * SimpleBasePlayer/ForwardingSimpleBasePlayer API surface but has not been compiled or run on a
 * device. If something in playback behaves oddly (queue advancement, shuffle order, seeking
 * across a track boundary during an in-progress crossfade), this is the first file to check.
 *
 * Known simplifications:
 *  - Shuffle mode uses a simple pre-shuffled index list, not ExoPlayer's exact shuffle-order
 *    semantics.
 *  - Seeking to an arbitrary item while a crossfade is already in progress cancels the crossfade
 *    and hard-cuts to the new item, rather than trying to blend three players.
 *  - Ad playback, DRM, and video-specific commands are not implemented (this is an audio player).
 */
class CrossfadePlayer(
    private val context: Context,
    looper: Looper
) : SimpleBasePlayer(looper) {

    private val handler = Handler(looper)

    private val playerA = buildInternalPlayer()
    private val playerB = buildInternalPlayer()
    private var activeIsA = true
    private val activePlayer get() = if (activeIsA) playerA else playerB
    private val standbyPlayer get() = if (activeIsA) playerB else playerA

    private var mediaItems: List<MediaItem> = emptyList()
    private var currentIndex: Int = 0
    private var shuffleOrder: List<Int> = emptyList()
    private var shuffleEnabled = false
    private var repeatMode: Int = Player.REPEAT_MODE_OFF
    private var playWhenReadyState = false
    private var playbackParams: PlaybackParameters = PlaybackParameters.DEFAULT
    private var userVolume: Float = 1f

    private var crossfadeInProgress = false
    private var crossfadeStartElapsedMs = 0L
    private var activeCrossfadeMs = 0

    /** Invoked whenever the audibly-active internal player's audio session id becomes known or
     *  changes (e.g. right after a crossfade swap) so EqualizerManager/VisualizerManager can be
     *  re-attached to whichever engine is actually producing sound. */
    var onActiveAudioSessionIdChanged: ((Int) -> Unit)? = null

    private val availableCommands = Player.Commands.Builder()
        .addAll(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_PREPARE,
            Player.COMMAND_STOP,
            Player.COMMAND_RELEASE,
            Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            Player.COMMAND_SET_SPEED_AND_PITCH,
            Player.COMMAND_SET_SHUFFLE_MODE,
            Player.COMMAND_SET_REPEAT_MODE,
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            Player.COMMAND_GET_TIMELINE,
            Player.COMMAND_GET_METADATA,
            Player.COMMAND_SET_MEDIA_ITEM,
            Player.COMMAND_CHANGE_MEDIA_ITEMS,
            Player.COMMAND_GET_VOLUME,
            Player.COMMAND_SET_VOLUME
        )
        .build()

    private fun buildInternalPlayer(): ExoPlayer {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        return ExoPlayer.Builder(context)
            // Only one of the two internal players (the active one) should be perceived as
            // "the" player by the system; audio focus is handled conceptually at this
            // CrossfadePlayer level rather than per internal instance, so we keep this simple.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)
            .build().also { player ->
                player.addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (player === activePlayer) onActiveAudioSessionIdChanged?.invoke(audioSessionId)
                    }
                })
            }
    }

    init {
        startTicker()
    }

    private fun startTicker() {
        handler.post(object : Runnable {
            override fun run() {
                tick()
                handler.postDelayed(this, 250L)
            }
        })
    }

    private fun tick() {
        if (mediaItems.isEmpty()) return
        val active = activePlayer
        val duration = active.duration
        val position = active.currentPosition
        activeCrossfadeMs = CrossfadeSettings.crossfadeMs.value

        if (crossfadeInProgress) {
            advanceCrossfade()
        } else if (
            activeCrossfadeMs > 0 &&
            duration != C.TIME_UNSET &&
            duration - position in 0..activeCrossfadeMs.toLong() &&
            hasNextIndex()
        ) {
            beginCrossfade()
        } else if (
            active.playbackState == Player.STATE_ENDED ||
            (duration != C.TIME_UNSET && position >= duration && activeCrossfadeMs <= 0)
        ) {
            // No crossfade configured (or none possible): hard-cut to the next item, same as
            // ExoPlayer's own gapless behavior would, once the current one truly ends.
            if (hasNextIndex()) {
                advanceToIndex(nextIndex(), startPositionMs = 0L, keepPlaying = playWhenReadyState)
            } else if (repeatMode == Player.REPEAT_MODE_ALL && mediaItems.isNotEmpty()) {
                advanceToIndex(orderedIndices().first(), startPositionMs = 0L, keepPlaying = playWhenReadyState)
            } else {
                active.pause()
            }
        }

        invalidateState()
    }

    private fun beginCrossfade() {
        val next = nextIndex()
        val nextItem = mediaItems.getOrNull(next) ?: return
        crossfadeInProgress = true
        crossfadeStartElapsedMs = android.os.SystemClock.elapsedRealtime()
        standbyPlayer.apply {
            setMediaItem(nextItem)
            volume = 0f
            prepare()
            playWhenReady = playWhenReadyState
            playbackParameters = playbackParams
        }
    }

    private fun advanceCrossfade() {
        val elapsed = android.os.SystemClock.elapsedRealtime() - crossfadeStartElapsedMs
        val fadeMs = activeCrossfadeMs.coerceAtLeast(1)
        val fraction = (elapsed.toFloat() / fadeMs).coerceIn(0f, 1f)
        activePlayer.volume = (1f - fraction) * userVolume
        standbyPlayer.volume = fraction * userVolume
        if (fraction >= 1f) {
            // Handoff complete: swap roles. The old active player is now standby — stop and
            // clear it so it's ready to be primed for the *next* transition.
            val finishedPlayer = activePlayer
            activeIsA = !activeIsA
            currentIndex = nextIndex(fromIndex = currentIndex)
            crossfadeInProgress = false
            finishedPlayer.pause()
            finishedPlayer.volume = userVolume
            activePlayer.volume = userVolume
            onActiveAudioSessionIdChanged?.invoke(activePlayer.audioSessionId)
        }
    }

    private fun orderedIndices(): List<Int> =
        if (shuffleEnabled && shuffleOrder.size == mediaItems.size) shuffleOrder else mediaItems.indices.toList()

    private fun hasNextIndex(): Boolean {
        if (mediaItems.isEmpty()) return false
        val order = orderedIndices()
        val pos = order.indexOf(currentIndex)
        return when {
            pos == -1 -> false
            pos < order.size - 1 -> true
            repeatMode == Player.REPEAT_MODE_ALL -> true
            else -> false
        }
    }

    private fun nextIndex(fromIndex: Int = currentIndex): Int {
        val order = orderedIndices()
        val pos = order.indexOf(fromIndex)
        if (pos == -1) return fromIndex
        return if (pos < order.size - 1) order[pos + 1] else order.first()
    }

    private fun previousIndex(fromIndex: Int = currentIndex): Int {
        val order = orderedIndices()
        val pos = order.indexOf(fromIndex)
        if (pos <= 0) return order.lastOrNull() ?: fromIndex
        return order[pos - 1]
    }

    private fun advanceToIndex(index: Int, startPositionMs: Long, keepPlaying: Boolean) {
        crossfadeInProgress = false
        standbyPlayer.stop()
        standbyPlayer.volume = userVolume
        currentIndex = index
        val item = mediaItems.getOrNull(index) ?: return
        activePlayer.apply {
            setMediaItem(item, startPositionMs)
            volume = userVolume
            prepare()
            playWhenReady = keepPlaying
            playbackParameters = playbackParams
        }
    }

    // ---- SimpleBasePlayer required/overridden methods ----

    override fun getState(): State {
        val active = activePlayer
        val playbackState = when {
            mediaItems.isEmpty() -> Player.STATE_IDLE
            active.playbackState == Player.STATE_ENDED && hasNextIndex() -> Player.STATE_READY
            else -> active.playbackState
        }

        val playlist = mediaItems.mapIndexed { index, item ->
            MediaItemData.Builder(index)
                .setMediaItem(item)
                .setMediaMetadata(item.mediaMetadata)
                .setIsSeekable(true)
                .build()
        }

        val builder = State.Builder()
            .setAvailableCommands(availableCommands)
            .setPlayWhenReady(playWhenReadyState, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            .setRepeatMode(repeatMode)
            .setShuffleModeEnabled(shuffleEnabled)
            .setPlaybackParameters(playbackParams)
            .setVolume(userVolume)
            .setIsLoading(active.playbackState == Player.STATE_BUFFERING)

        if (playlist.isNotEmpty()) {
            builder.setPlaylist(playlist)
                .setCurrentMediaItemIndex(currentIndex)
                .setContentPositionMs(active.currentPosition)
        }

        return builder.build()
    }

    override fun handleSetMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> {
        this.mediaItems = mediaItems.toList()
        this.shuffleOrder = this.mediaItems.indices.shuffled()
        val index = if (startIndex == C.INDEX_UNSET) 0 else startIndex.coerceIn(0, this.mediaItems.size - 1)
        val startPos = if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs
        advanceToIndex(index, startPos, keepPlaying = playWhenReadyState)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        playWhenReadyState = playWhenReady
        activePlayer.playWhenReady = playWhenReady
        if (crossfadeInProgress) standbyPlayer.playWhenReady = playWhenReady
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        activePlayer.prepare()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        playerA.stop()
        playerB.stop()
        crossfadeInProgress = false
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        playerA.release()
        playerB.release()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val targetIndex = if (mediaItemIndex == C.INDEX_UNSET) currentIndex else mediaItemIndex
        val targetPos = if (positionMs == C.TIME_UNSET) 0L else positionMs
        if (targetIndex == currentIndex && !crossfadeInProgress) {
            activePlayer.seekTo(targetPos)
        } else {
            advanceToIndex(targetIndex, targetPos, keepPlaying = playWhenReadyState)
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        this.repeatMode = repeatMode
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        shuffleEnabled = shuffleModeEnabled
        if (shuffleModeEnabled && shuffleOrder.size != mediaItems.size) {
            shuffleOrder = mediaItems.indices.shuffled()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        playbackParams = playbackParameters
        playerA.playbackParameters = playbackParameters
        playerB.playbackParameters = playbackParameters
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        userVolume = volume
        if (!crossfadeInProgress) activePlayer.volume = volume
        invalidateState()
        return Futures.immediateVoidFuture()
    }
}
