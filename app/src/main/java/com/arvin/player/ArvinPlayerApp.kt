package com.arvin.player

import android.app.Application
import com.arvin.player.media.PlayerController

class ArvinPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Warms up the singleton connection to PlaybackService as soon as the process starts.
        PlayerController.getInstance(this).connect()
    }
}
