package com.arvin.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A smooth, self-animated audio-style visualiser.
 *
 * It does NOT capture real audio (that needs the RECORD_AUDIO permission and the platform Visualizer
 * effect, which is unreliable and janky across devices). Instead it draws layered sine waves so the
 * bars feel alive and musical, fading in while [playing] and settling flat when paused. Cheap enough
 * to run at 60fps on any phone.
 */
@Composable
fun VisualizerView(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 44,
    height: androidx.compose.ui.unit.Dp = 120.dp
) {
    val transition = rememberInfiniteTransition(label = "visualizer")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "phase"
    )
    // Energy ramps in/out so the bars swell when playback starts and relax when it stops.
    val energy by animateFloatAsState(if (playing) 1f else 0.12f, tween(600), label = "energy")

    val brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f)))

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val slot = size.width / barCount
        val barWidth = slot * 0.55f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val minH = size.height * 0.10f
        for (i in 0 until barCount) {
            // Two sine layers at different rates + a gentle spatial envelope give an organic, non-repetitive look.
            val a = 0.5f + 0.5f * sin(t + i * 0.55f)
            val b = 0.5f + 0.5f * sin(t * 0.7f + i * 0.21f)
            val envelope = 0.45f + 0.55f * sin(i.toFloat() / barCount * PI.toFloat())
            val wave = (a * 0.65f + b * 0.35f) * envelope
            val h = (minH + energy * wave * (size.height - minH)).coerceIn(minH, size.height)
            val x = i * slot + (slot - barWidth) / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = radius
            )
        }
    }
}
