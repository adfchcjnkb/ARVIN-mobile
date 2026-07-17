package com.arvin.player.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.arvin.player.MainActivity
import com.arvin.player.widget.ArvinWidgetProvider

/**
 * Media3's MediaSessionService gives us, essentially for free:
 *  - a system media notification with play/pause/next/prev
 *  - lock screen transport controls
 *  - Bluetooth/wired headset button + AVRCP metadata
 *  - Android Auto browsing (declared in the manifest)
 *  - foreground-service lifecycle so playback survives backgrounding
 *
 * The bound player is [CrossfadePlayer] — a custom Player (via SimpleBasePlayer) that runs two
 * internal ExoPlayer instances to do a *real* overlapping crossfade between tracks, rather than a
 * single-player volume fade. See CrossfadePlayer's class doc for details and caveats.
 *
 * The Equalizer and Visualizer attach themselves to whichever internal player is currently
 * audible (CrossfadePlayer reports this via onActiveAudioSessionIdChanged, since which physical
 * ExoPlayer is "active" changes every crossfade handoff). This service also pushes updates to the
 * home-screen widget (ArvinWidgetProvider) and accepts its button-tap commands.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var crossfadePlayer: CrossfadePlayer

    override fun onCreate() {
        super.onCreate()

        crossfadePlayer = CrossfadePlayer(this, android.os.Looper.getMainLooper()).apply {
            onActiveAudioSessionIdChanged = { audioSessionId ->
                EqualizerManager.attach(audioSessionId)
                VisualizerManager.attach(audioSessionId)
            }
        }

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, crossfadePlayer)
            .setSessionActivity(sessionActivityIntent)
            .build()

        crossfadePlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                pushWidgetUpdate()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pushWidgetUpdate()
            }
        })
    }

    private fun pushWidgetUpdate() {
        val metadata = crossfadePlayer.currentMediaItem?.mediaMetadata
        val albumId = metadata?.extras?.getLong("albumId")
        ArvinWidgetProvider.updateAll(
            context = this,
            title = metadata?.title?.toString(),
            artist = metadata?.artist?.toString(),
            albumId = albumId,
            isPlaying = crossfadePlayer.isPlaying
        )
    }

    /** Handles button taps forwarded from the home-screen widget. Media button / notification
     *  taps still go through the MediaSession as usual — this path is specifically for the
     *  plain broadcast Intents ArvinWidgetProvider sends. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ArvinWidgetProvider.ACTION_PLAY_PAUSE -> {
                if (crossfadePlayer.isPlaying) crossfadePlayer.pause() else crossfadePlayer.play()
            }
            ArvinWidgetProvider.ACTION_NEXT -> if (crossfadePlayer.hasNextMediaItem()) crossfadePlayer.seekToNext()
            ArvinWidgetProvider.ACTION_PREV -> if (crossfadePlayer.hasPreviousMediaItem()) crossfadePlayer.seekToPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        EqualizerManager.release()
        VisualizerManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
