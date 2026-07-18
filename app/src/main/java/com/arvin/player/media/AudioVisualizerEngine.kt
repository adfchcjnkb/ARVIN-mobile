package com.arvin.player.media

import android.media.audiofx.Visualizer
import androidx.media3.common.C
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Captures real FFT (frequency-spectrum) data from the ExoPlayer's audio session via Android's
 * platform [Visualizer] effect — the bars genuinely follow what's playing, not a decorative
 * animation. Bound to the same stable audio session id the Equalizer attaches to (see
 * [PlaybackService]), so it survives track changes within the same queue.
 *
 * Bucketed into [BAR_COUNT] bars with a log-weighted frequency split (more bars devoted to
 * bass/low-mid, like a real spectrum analyzer) and normalized with a slow-decaying auto-gain so
 * quiet and loud tracks both look lively rather than needing a fixed, wrong-for-half-your-library
 * scale.
 */
object AudioVisualizerEngine {
    const val BAR_COUNT = 44

    private var visualizer: Visualizer? = null

    private val _magnitudes = MutableStateFlow(FloatArray(BAR_COUNT))
    val magnitudes: StateFlow<FloatArray> = _magnitudes

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available

    // Slow-decaying running peak so the bars auto-scale to how loud the current track is,
    // instead of looking dead on quiet tracks or clipped on loud ones.
    private var runningPeak = 1f

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        runCatching {
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            // Not used — FFT gives us real per-frequency data instead.
                        }
                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            fft ?: return
                            _magnitudes.value = decodeAndBucket(fft)
                        }
                    },
                    (Visualizer.getMaxCaptureRate() * 0.75f).toInt().coerceAtLeast(10_000),
                    /* waveform = */ false,
                    /* fft = */ true
                )
                enabled = true
            }
            _available.value = true
        }.onFailure {
            // Missing permission, engine busy, or unsupported device — the UI falls back to its
            // decorative animation rather than crashing or freezing.
            _available.value = false
        }
    }

    private fun decodeAndBucket(fft: ByteArray): FloatArray {
        val n = fft.size
        val bins = n / 2
        if (bins < 2) return FloatArray(BAR_COUNT)

        val raw = FloatArray(bins)
        raw[0] = abs(fft[0].toInt()).toFloat() // DC component (packed specially per the Android docs)
        for (k in 1 until bins) {
            val re = fft[2 * k].toInt()
            val im = if (2 * k + 1 < n) fft[2 * k + 1].toInt() else 0
            raw[k] = sqrt((re * re + im * im).toFloat())
        }

        var frameMax = 1f
        val bars = FloatArray(BAR_COUNT)
        for (i in 0 until BAR_COUNT) {
            // Squared fractional split concentrates more bars on the bass/low-mid range, which is
            // both more visually interesting and matches how real spectrum analyzers look.
            val startFrac = (i.toDouble() / BAR_COUNT) * (i.toDouble() / BAR_COUNT)
            val endFrac = ((i + 1.0) / BAR_COUNT) * ((i + 1.0) / BAR_COUNT)
            val start = (startFrac * bins).toInt().coerceIn(0, bins - 1)
            val end = (endFrac * bins).toInt().coerceIn(start + 1, bins)
            var sum = 0f
            for (j in start until end) sum += raw[j]
            val avg = sum / (end - start)
            bars[i] = avg
            if (avg > frameMax) frameMax = avg
        }

        // Auto-gain: rise fast to a new peak, decay slowly, so the scale adapts to the track's
        // loudness without letting one loud moment permanently flatten everything after it.
        runningPeak = if (frameMax > runningPeak) frameMax else (runningPeak * 0.97f).coerceAtLeast(1f)

        for (i in bars.indices) bars[i] = (bars[i] / runningPeak).coerceIn(0f, 1f)
        return bars
    }

    fun release() {
        runCatching { visualizer?.enabled = false; visualizer?.release() }
        visualizer = null
        _available.value = false
    }
}
