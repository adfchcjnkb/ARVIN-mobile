package com.arvin.player.ui.screens.equalizer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arvin.player.data.local.EqPresetEntity
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.media.EqualizerManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Ready-made presets beyond whatever the device's native Equalizer effect exposes. */
val CUSTOM_PRESETS: Map<String, List<Short>> = mapOf
    (
    "Flat" to List(10) { 0 },
    "Bass Boost" to listOf(600, 500, 400, 200, 0, 0, 0, 0, 0, 0),
    "Treble Boost" to listOf(0, 0, 0, 0, 0, 100, 250, 400, 500, 600),
    "Vocal" to listOf(-200, -100, 0, 200, 400, 400, 200, 0, -100, -200),
    "Electronic" to listOf(400, 300, 100, 0, -100, 0, 100, 200, 300, 400),
    "Rock" to listOf(400, 300, 200, 0, -100, -100, 0, 200, 300, 400),
    "Jazz" to listOf(300, 200, 100, 100, -100, -100, 0, 100, 200, 300),
    "Classical" to listOf(300, 300, 200, 100, 0, 0, -100, -100, -100, 300),
    "Pop" to listOf(-100, 100, 300, 300, 200, 0, -100, -100, -100, -100),
    "Hip-Hop" to listOf(500, 400, 200, 100, -100, -100, 0, 100, 200, 300)
)

class EqualizerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.getInstance(app)

    val enabled: StateFlow<Boolean> = EqualizerManager.enabled
    val bands = EqualizerManager.bands
    val bassBoost = EqualizerManager.bassBoostStrength
    val virtualizerStrength = EqualizerManager.virtualizerStrength
    val savedPresets: StateFlow<List<EqPresetEntity>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    fun setEnabled(on: Boolean) = EqualizerManager.setEnabled(on)
    fun setBand(index: Short, level: Short) = EqualizerManager.setBandLevel(index, level)
    fun setBassBoost(strength: Int) = EqualizerManager.setBassBoost(strength)
    fun setVirtualizer(strength: Int) = EqualizerManager.setVirtualizerStrength(strength)

    fun applyCustomPreset(name: String) {
        CUSTOM_PRESETS[name]?.let { EqualizerManager.applyCustomBands(it) }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val csv = bands.value.joinToString(",") { it.levelMb.toString() }
            repo.eqPresetDao.savePreset(EqPresetEntity(name = name, bandLevelsCsv = csv))
        }
    }
}
