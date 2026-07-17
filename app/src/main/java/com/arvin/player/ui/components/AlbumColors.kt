package com.arvin.player.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.arvin.player.ui.theme.AuroraViolet
import com.arvin.player.ui.theme.LocalArvinSkin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Colours pulled from the current album art, used to tint the whole player. */
@Immutable
data class AlbumPalette(
    val top: Color,
    val bottom: Color,
    val accent: Color
)

/**
 * Loads [data] (an album-art Uri/model) off the main thread, runs Palette over it, and returns an
 * animated [AlbumPalette] that smoothly crossfades whenever the track changes — the signature
 * Spotify/Apple "the whole screen takes on the cover's colour" effect. Falls back to the brand
 * aurora colours before art is available or if extraction fails.
 */
@Composable
fun rememberAlbumPalette(data: Any?): AlbumPalette {
    val context = LocalContext.current
    val skin = LocalArvinSkin.current
    val fallback = remember(skin.isDark) {
        AlbumPalette(skin.auroraTop, skin.auroraBottom, AuroraViolet)
    }
    var target by remember { mutableStateOf(fallback) }

    LaunchedEffect(data, skin.isDark) {
        target = fallback
        if (data == null) return@LaunchedEffect
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .allowHardware(false) // Palette needs a software bitmap it can read pixels from
                    .size(256)
                    .build()
                val result = context.imageLoader.execute(request)
                val bitmap = (result as? SuccessResult)?.drawable
                    ?.let { it as? BitmapDrawable }?.bitmap ?: return@runCatching null
                val palette = Palette.from(bitmap).maximumColorCount(24).generate()
                paletteToAlbum(palette, skin.isDark)
            }.getOrNull()
        }
        if (extracted != null) target = extracted
    }

    val top by animateColorAsState(target.top, tween(700), label = "auroraTop")
    val bottom by animateColorAsState(target.bottom, tween(700), label = "auroraBottom")
    val accent by animateColorAsState(target.accent, tween(700), label = "auroraAccent")
    return AlbumPalette(top, bottom, accent)
}

private fun paletteToAlbum(palette: Palette, isDark: Boolean): AlbumPalette {
    val vibrant = palette.vibrantSwatch
        ?: palette.lightVibrantSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.dominantSwatch
    val muted = palette.darkMutedSwatch
        ?: palette.mutedSwatch
        ?: palette.dominantSwatch
        ?: vibrant

    val accent = Color(vibrant?.rgb ?: 0xFF8B5CF6.toInt())
    val base = Color(muted?.rgb ?: vibrant?.rgb ?: 0xFF3B2C63.toInt())

    return if (isDark) {
        AlbumPalette(
            top = base.mix(Color.Black, 0.42f),
            bottom = base.mix(Color.Black, 0.88f),
            accent = accent.ensureBright()
        )
    } else {
        AlbumPalette(
            top = accent.mix(Color.White, 0.74f),
            bottom = Color.White.mix(accent, 0.06f),
            accent = accent.mix(Color.Black, 0.12f)
        )
    }
}

/** Linear blend toward [other]; t=0 keeps this colour, t=1 becomes [other]. */
private fun Color.mix(other: Color, t: Float): Color = Color(
    red = red + (other.red - red) * t,
    green = green + (other.green - green) * t,
    blue = blue + (other.blue - blue) * t,
    alpha = 1f
)

/** Nudge very dark accents up so they stay visible as a highlight on a dark backdrop. */
private fun Color.ensureBright(): Color {
    val luma = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luma < 0.35f) mix(Color.White, 0.35f) else this
}
