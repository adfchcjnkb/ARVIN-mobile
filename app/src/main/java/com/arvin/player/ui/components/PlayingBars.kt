package com.arvin.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin

/**
 * The little dancing equaliser bars used as a "this is the track that's playing" badge, the way
 * Spotify marks the current row. Sine-driven so the motion is smooth and cheap; when [playing]
 * is false the bars settle into a static low pattern.
 */
@Composable
fun PlayingBars(
    color: Color,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    barCount: Int = 4
) {
    val transition = rememberInfiniteTransition(label = "playingBars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val gap = size.width * 0.12f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val minH = size.height * 0.22f
        for (i in 0 until barCount) {
            val wave = if (playing) {
                0.5f + 0.5f * sin(phase + i * 1.3f)
            } else {
                // static, gently varied heights when paused
                (i % 2) * 0.18f + 0.15f
            }
            val h = (minH + wave * (size.height - minH)).coerceIn(minH, size.height)
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = radius
            )
        }
    }
}
