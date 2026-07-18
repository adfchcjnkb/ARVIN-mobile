package com.arvin.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arvin.player.media.AudioVisualizerEngine
import kotlin.math.PI
import kotlin.math.sin

/**
 * Audio-reactive spectrum bars. When the real [AudioVisualizerEngine] is available (permission
 * granted, device/session supports it), the bars follow the track's actual frequency content.
 * Otherwise it falls back to a lively decorative animation rather than sitting flat or crashing.
 *
 * Smoothing runs on its own per-frame (`withFrameNanos`) loop independent of how often new FFT
 * data actually arrives (audio capture callbacks land at roughly 15-20 Hz, well below the 60fps+
 * the screen renders at) — every displayed bar eases toward its latest target every single frame,
 * which is what makes the motion fluid instead of visibly stepping or freezing between updates.
 */
@Composable
fun VisualizerView(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = AudioVisualizerEngine.BAR_COUNT,
    height: androidx.compose.ui.unit.Dp = 120.dp
) {
    val available by AudioVisualizerEngine.available.collectAsState()
    val realMagnitudes by AudioVisualizerEngine.magnitudes.collectAsState()

    // What's actually painted each frame — always non-null, always finite in length, so a Canvas
    // read can never see a stale/mismatched buffer.
    val displayed = remember { FloatArray(barCount) }
    var frameTick by remember { mutableStateOf(0) } // bump to force a Canvas redraw each animation tick
    var phase by remember { mutableStateOf(0f) }

    LaunchedEffect(playing, available) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                val deltaSec = if (lastNanos == 0L) 0f else ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastNanos = nanos
                phase += deltaSec * (2f * PI.toFloat() / 2.6f)

                val useReal = available && playing
                for (i in 0 until barCount) {
                    val target = if (useReal && i < realMagnitudes.size) {
                        realMagnitudes[i]
                    } else {
                        // Decorative fallback: layered sine waves, same shape as before, eased in
                        // the same loop so switching between real/decorative is never a visible jump.
                        val a = 0.5f + 0.5f * sin(phase + i * 0.55f)
                        val b = 0.5f + 0.5f * sin(phase * 0.7f + i * 0.21f)
                        val envelope = 0.45f + 0.55f * sin(i.toFloat() / barCount * PI.toFloat())
                        val energy = if (playing) 1f else 0.12f
                        energy * ((a * 0.65f + b * 0.35f) * envelope)
                    }
                    // Fast attack, slower release — real audio transients read as snappy while the
                    // motion between them stays smooth instead of jittery.
                    val current = displayed[i]
                    val rate = if (target > current) 0.55f else 0.18f
                    displayed[i] = current + (target - current) * rate
                }
                frameTick++
            }
        }
    }

    val brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.55f)))

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        // Reading frameTick here (even unused) ties this draw scope to the per-frame animation loop.
        @Suppress("UNUSED_EXPRESSION") frameTick

        val slot = size.width / barCount
        val barWidth = slot * 0.55f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val minH = size.height * 0.10f
        for (i in 0 until barCount) {
            val wave = displayed.getOrElse(i) { 0f }.coerceIn(0f, 1f)
            val h = (minH + wave * (size.height - minH)).coerceIn(minH, size.height)
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
