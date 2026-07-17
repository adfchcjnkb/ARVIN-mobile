package com.arvin.player.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.arvin.player.MainActivity
import com.arvin.player.widget.ArvinWidgetProvider

/**
 * Media3's MediaSessionService gives us, essentially for free:
 *  - a system media notification with play/pause/next/prev + album art
 *  - lock-screen transport controls (also with album art)
 *  - Bluetooth/wired headset button + AVRCP metadata
 *  - foreground-service lifecycle so playback survives backgrounding
 *
 * The bound player is a single, plain [ExoPlayer]. It handles queue navigation, seeking, repeat,
 * shuffle, speed and gapless playback correctly out of the box — which is exactly what a music
 * player needs to be reliable on every device. (An earlier build used a custom dual-ExoPlayer
 * "crossfade" player; it was the source of the stuck seek-bar and dead next/prev buttons, so it
 * was removed in favour of correctness.)
 *
 * The Equalizer attaches to the player's single, stable audio-session id, so effects actually
 * apply to what you hear.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        attachEqualizer(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachEqualizer(audioSessionId)
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                pushWidgetUpdate()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pushWidgetUpdate()
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    private fun attachEqualizer(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        EqualizerManager.attach(audioSessionId)
    }

    private fun pushWidgetUpdate() {
        val metadata = player.currentMediaItem?.mediaMetadata
        val albumId = metadata?.extras?.getLong("albumId")
        ArvinWidgetProvider.updateAll(
            context = this,
            title = metadata?.title?.toString(),
            artist = metadata?.artist?.toString(),
            albumId = albumId,
            isPlaying = player.isPlaying
        )
    }

    /** Handles button taps forwarded from the home-screen widget's broadcast Intents. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ArvinWidgetProvider.ACTION_PLAY_PAUSE ->
                if (player.isPlaying) player.pause() else player.play()
            ArvinWidgetProvider.ACTION_NEXT ->
                if (player.hasNextMediaItem()) player.seekToNext()
            ArvinWidgetProvider.ACTION_PREV ->
                if (player.hasPreviousMediaItem()) player.seekToPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        EqualizerManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
