package com.arvin.player.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens that Material's ColorScheme can't hold: gradients and the frosted-glass tints.
 * Provided through [LocalArvinSkin] so every component reads light/dark-correct values without
 * threading booleans around.
 */
data class ArvinSkin(
    val isDark: Boolean,
    /** Fill painted on top of a blurred backdrop to get the frosted look. */
    val glassFill: Color,
    /** A slightly stronger glass fill for floating elements (mini-player, sheets). */
    val glassFillStrong: Color,
    /** Hairline highlight along the top/edge of a glass surface — the classic "lit rim". */
    val glassBorder: Color,
    /** Soft shadow/ambient color under elevated elements. */
    val ambient: Color,
    /** Default aurora colors used when no album art has been analysed yet. */
    val auroraTop: Color,
    val auroraBottom: Color
)

val LocalArvinSkin = staticCompositionLocalOf {
    // Sensible dark default; real value is supplied by ArvinPlayerTheme.
    ArvinSkin(
        isDark = true,
        glassFill = Color(0x1FFFFFFF),
        glassFillStrong = Color(0x2BFFFFFF),
        glassBorder = Color(0x33FFFFFF),
        ambient = Color(0x66000000),
        auroraTop = AuroraViolet,
        auroraBottom = AuroraIndigo
    )
}

val DarkSkin = ArvinSkin(
    isDark = true,
    glassFill = Color(0x1FFFFFFF),
    glassFillStrong = Color(0x2EFFFFFF),
    glassBorder = Color(0x40FFFFFF),
    ambient = Color(0x80000000),
    auroraTop = Color(0xFF3B2C63),
    auroraBottom = Color(0xFF0A0A10)
)

val LightSkin = ArvinSkin(
    isDark = false,
    glassFill = Color(0x66FFFFFF),
    glassFillStrong = Color(0x99FFFFFF),
    glassBorder = Color(0x99FFFFFF),
    ambient = Color(0x1A3A2A66),
    auroraTop = Color(0xFFE9E2FB),
    auroraBottom = Color(0xFFF6F5FB)
)

/** The signature violet→pink→indigo brand sweep, used on the play button and accents. */
val AuroraButtonBrush: Brush = Brush.linearGradient(
    colors = listOf(AuroraIndigo, AuroraViolet, AuroraPink)
)

val AuroraTextBrush: Brush = Brush.linearGradient(
    colors = listOf(AuroraMagenta, AuroraViolet)
)

/** Vertical brand gradient used behind splash-y headers. */
fun brandVertical(): Brush = Brush.verticalGradient(
    colors = listOf(AuroraViolet, AuroraIndigo)
)

/**
 * Subtle aurora-tinted app background. Instead of a flat dark/white fill, it lays down a base colour
 * plus two soft off-screen colour glows, so translucent "glass" panels have something to frost and
 * the whole UI stops looking flat. Painted once in the draw phase — effectively free.
 */
fun Modifier.screenBackground(isDark: Boolean): Modifier = this.drawBehind {
    val base = if (isDark) Color(0xFF08080C) else Color(0xFFF6F5FB)
    drawRect(base)
    drawRect(
        Brush.radialGradient(
            colors = listOf(AuroraViolet.copy(alpha = if (isDark) 0.18f else 0.10f), Color.Transparent),
            center = Offset(size.width * 0.92f, size.height * 0.02f),
            radius = size.maxDimension * 0.78f
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(AuroraPink.copy(alpha = if (isDark) 0.12f else 0.07f), Color.Transparent),
            center = Offset(size.width * 0.04f, size.height * 0.55f),
            radius = size.maxDimension * 0.6f
        )
    )
}
