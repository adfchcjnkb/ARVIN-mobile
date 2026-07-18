package com.arvin.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A lot of Android "tablets" are really just wide phones, and a lot of phones are narrow — sizing
 * off screen *width* (not a device-type guess) is what keeps this correct on foldables, Chromebooks,
 * and every phone size in between. Use [scale] to grow icon/button/touch-target sizes on larger
 * screens rather than leaving everything phone-sized on a 12" tablet.
 */
object AdaptiveSize {
    @Composable
    @ReadOnlyComposable
    fun scale(): Float {
        val widthDp = LocalConfiguration.current.screenWidthDp
        return when {
            widthDp >= 900 -> 1.45f  // large tablets, desktop-class Chromebooks
            widthDp >= 600 -> 1.22f  // small/medium tablets, unfolded foldables
            else -> 1f               // phones
        }
    }

    /** Scales a base dp value by the current screen's size class. */
    @Composable
    @ReadOnlyComposable
    fun dp(base: Int): Dp = (base * scale()).dp
}
