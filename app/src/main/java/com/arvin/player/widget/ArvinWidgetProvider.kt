package com.arvin.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.arvin.player.R
import com.arvin.player.media.PlaybackService

/**
 * A simple, always-on-demand-updated home screen widget (no polling timer — PlaybackService
 * pushes a fresh RemoteViews update whenever the current song or play/pause state changes).
 * Button taps send explicit broadcasts to this same provider, which forwards them to
 * PlaybackService as plain Intent actions; PlaybackService.onStartCommand interprets those and
 * calls the corresponding ExoPlayer transport control.
 */
class ArvinWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.arvin.player.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.arvin.player.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.arvin.player.widget.ACTION_PREV"

        /** Called by PlaybackService whenever song/play-state changes, to push a fresh widget UI.
         *  Takes plain fields rather than a Song so PlaybackService doesn't need a repository
         *  dependency just to update a widget — it already has this info on the current MediaItem. */
        fun updateAll(context: Context, title: String?, artist: String?, albumId: Long?, isPlaying: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, ArvinWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            for (id in ids) {
                manager.updateAppWidget(id, buildViews(context, title, artist, albumId, isPlaying))
            }
        }

        private fun buildViews(context: Context, title: String?, artist: String?, albumId: Long?, isPlaying: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, title ?: context.getString(R.string.nothing_playing))
            views.setTextViewText(R.id.widget_artist, artist ?: "")
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            if (albumId != null) {
                runCatching {
                    val artUri = Uri.parse("content://media/external/audio/albumart/$albumId")
                    views.setImageViewUri(R.id.widget_album_art, artUri)
                }
            }

            views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, ACTION_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, ACTION_NEXT))
            views.setOnClickPendingIntent(R.id.widget_prev, actionPendingIntent(context, ACTION_PREV))
            return views
        }

        private fun actionPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, ArvinWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context, action.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV -> {
                // Forward to PlaybackService as a plain command Intent; it interprets these
                // in onStartCommand and drives its ExoPlayer instance directly.
                val serviceIntent = Intent(context, PlaybackService::class.java).setAction(intent.action)
                context.startService(serviceIntent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // No cached "current song" to show yet on first placement — PlaybackService will push
        // a real update as soon as something plays. Show the idle state in the meantime.
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, null, null, null, false))
        }
    }
}
