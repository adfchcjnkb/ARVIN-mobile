package com.arvin.player.media

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Captures live waveform + FFT magnitude data from the currently playing audio session
 * using the platform Visualizer effect. The Compose visualizer screen reads these
 * StateFlows every frame to draw bars/waves that move with the actual audio, not a fake animation.
 */
object VisualizerManager {
    private var visualizer: Visualizer? = null

    private val _waveform = MutableStateFlow(ByteArray(0))
    val waveform: StateFlow<ByteArray> = _waveform

    private val _fft = MutableStateFlow(ByteArray(0))
    val fft: StateFlow<ByteArray> = _fft

    fun attach(audioSessionId: Int) {
        release()
        runCatching {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray, samplingRate: Int) {
                        _waveform.value = waveform
                    }
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray, samplingRate: Int) {
                        _fft.value = fft
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        }
    }

    fun release() {
        runCatching { visualizer?.enabled = false }
        visualizer?.release()
        visualizer = null
    }
}
