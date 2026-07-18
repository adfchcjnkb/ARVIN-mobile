package com.arvin.player.util

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A message to show once, already carrying its own format args so the composable that
 *  displays it doesn't need to know anything about where it came from. */
data class AppNotice(@StringRes val resId: Int, val args: List<Any> = emptyList())

/**
 * App-wide "toast, but in-app and themed" bus. Any ViewModel/screen calls [notify] when the user
 * flips a toggle (shuffle, repeat, sleep timer, favorite, hide, playlist add/remove, metadata
 * saved, etc.) and a single Snackbar host mounted once at the root of the app (see MainActivity /
 * ArvinNavHost) shows it, translated through the normal Android string-resource system — so it
 * automatically follows the user's chosen app language like everything else.
 *
 * Uses a SharedFlow with buffering rather than a plain StateFlow so that firing the same
 * notification twice in a row (e.g. toggling shuffle on/off/on quickly) always shows every event
 * instead of the second one silently coalescing with the first.
 */
object AppNotifier {
    private val _notices = MutableSharedFlow<AppNotice>(extraBufferCapacity = 8)
    val notices = _notices.asSharedFlow()

    fun notify(@StringRes resId: Int, vararg args: Any) {
        _notices.tryEmit(AppNotice(resId, args.toList()))
    }
}
