package com.arvin.player.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class EqBand(val index: Short, val centerFreqHz: Int, val minMb: Short, val maxMb: Short, val levelMb: Short)

/**
 * Wraps Android's platform audio effects (Equalizer, BassBoost, Virtualizer, PresetReverb)
 * bound to the ExoPlayer's audio session. This gives a genuine hardware/software-mixer EQ,
 * not a fake UI — the device typically exposes 5-6 native bands; we present those to the
 * user as up to 10 sliders by interpolating between adjacent native bands so the UI always
 * shows a full 10-band control regardless of device.
 */
object EqualizerManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands

    private val _bassBoostStrength = MutableStateFlow(0) // 0-1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength

    private val _virtualizerStrength = MutableStateFlow(0) // 0-1000, drives the "3D" effect
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength

    val presets: List<String>
        get() = equalizer?.let { eq -> (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) } } ?: emptyList()

    fun attach(audioSessionId: Int) {
        release()
        runCatching {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = _enabled.value }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = false }
            presetReverb = PresetReverb(0, audioSessionId).apply { enabled = false }
            refreshBands()
        }
    }

    private fun refreshBands() {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange
        _bands.value = (0 until eq.numberOfBands).map { i ->
            val idx = i.toShort()
            EqBand(
                index = idx,
                centerFreqHz = eq.getCenterFreq(idx) / 1000,
                minMb = range[0],
                maxMb = range[1],
                levelMb = eq.getBandLevel(idx)
            )
        }
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        runCatching { equalizer?.enabled = on }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        val eq = equalizer ?: return
        runCatching {
            if (bandIndex < eq.numberOfBands) {
                val range = eq.bandLevelRange
                val clamped = levelMb.toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                eq.setBandLevel(bandIndex, clamped)
            }
        }
        refreshBands()
    }

    fun applyPreset(presetIndex: Short) {
        equalizer?.usePreset(presetIndex)
        refreshBands()
    }

    /** Apply a custom 10-value gain array (-1500..1500 mb) saved by the user, mapped onto native bands. */
    fun applyCustomBands(tenBandMb: List<Short>) {
        val eq = equalizer ?: return
        val nativeBandCount = eq.numberOfBands.toInt()
        if (nativeBandCount == 0) return
        runCatching {
            val range = eq.bandLevelRange
            val mapped = com.arvin.player.util.EqualizerBandMapper.mapToNativeBands(tenBandMb, nativeBandCount)
            mapped.forEachIndexed { i, level ->
                val clamped = level.toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                eq.setBandLevel(i.toShort(), clamped)
            }
        }
        refreshBands()
    }

    fun setBassBoost(strength: Int) {
        _bassBoostStrength.value = strength
        runCatching {
            bassBoost?.apply {
                enabled = strength > 0
                setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
    }

    /** "3D / surround" effect — backed by the platform Virtualizer effect. */
    fun setVirtualizerStrength(strength: Int) {
        _virtualizerStrength.value = strength
        runCatching {
            virtualizer?.apply {
                enabled = strength > 0
                setStrength(strength.coerceIn(0, 1000).toShort())
            }
        }
    }

    /** "Ambient / echo" effect via preset reverb (0 = NONE .. 6 = PLATE). */
    fun setReverbPreset(preset: Short) {
        presetReverb?.apply {
            enabled = preset.toInt() != PresetReverb.PRESET_NONE.toInt()
            this.preset = preset
        }
    }

    fun release() {
        equalizer?.release(); equalizer = null
        bassBoost?.release(); bassBoost = null
        virtualizer?.release(); virtualizer = null
        presetReverb?.release(); presetReverb = null
    }
}
