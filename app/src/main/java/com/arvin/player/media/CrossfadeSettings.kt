package com.arvin.player.media

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * PlaybackService and the rest of the app run in the same process, so a plain in-memory
 * singleton is all that's needed to pass the user's crossfade-duration setting from
 * PlayerController (UI side) to CrossfadePlayer (playback engine side) — no need for
 * MediaSession custom commands or cross-process IPC.
 */
object CrossfadeSettings {
    val crossfadeMs = MutableStateFlow(0)
}
