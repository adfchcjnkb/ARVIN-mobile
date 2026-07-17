package com.arvin.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arvin.player.media.VisualizerManager
import kotlin.math.abs

enum class VisualizerMode { BARS, WAVE, MIRROR_BARS }

@Composable
fun VisualizerView(mode: VisualizerMode, modifier: Modifier = Modifier) {
    val fft by VisualizerManager.fft.collectAsState()
    val waveform by VisualizerManager.waveform.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val barBrush = Brush.verticalGradient(listOf(secondary, primary))

    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width
        val h = size.height

        when (mode) {
            VisualizerMode.BARS, VisualizerMode.MIRROR_BARS -> {
                if (fft.isEmpty()) return@Canvas
                val magnitudes = (0 until fft.size / 2).map { i ->
                    val re = fft[i * 2].toInt()
                    val im = if (i * 2 + 1 < fft.size) fft[i * 2 + 1].toInt() else 0
                    kotlin.math.sqrt((re * re + im * im).toFloat())
                }
                val barCount = 40
                val step = (magnitudes.size / barCount).coerceAtLeast(1)
                val slot = w / barCount
                val barWidth = slot * 0.62f
                val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                for (i in 0 until barCount) {
                    val mag = magnitudes.getOrNull(i * step) ?: 0f
                    val barHeight = (mag / 128f * h).coerceIn(barWidth, h)
                    val x = i * slot + (slot - barWidth) / 2f
                    if (mode == VisualizerMode.MIRROR_BARS) {
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(x, h / 2 - barHeight / 2),
                            size = Size(barWidth, barHeight),
                            cornerRadius = radius
                        )
                    } else {
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(x, h - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = radius
                        )
                    }
                }
            }
            VisualizerMode.WAVE -> {
                if (waveform.isEmpty()) return@Canvas
                val path = androidx.compose.ui.graphics.Path()
                val stepX = w / waveform.size
                waveform.forEachIndexed { i, byte ->
                    val amplitude = (abs(byte.toInt() - 128) / 128f) * h
                    val x = i * stepX
                    val y = h / 2 - amplitude / 2
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path,
                    brush = Brush.horizontalGradient(listOf(primary, secondary)),
                    style = Stroke(width = 5f)
                )
            }
        }
    }
}
