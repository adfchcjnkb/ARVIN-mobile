package com.arvin.player.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.data.repository.SettingsRepository
import com.arvin.player.media.PlayerController
import com.arvin.player.util.LyricsExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    val player: PlayerController = PlayerController.getInstance(app)
    private val repo = MusicRepository.getInstance(app)
    private val settings = SettingsRepository.getInstance(app)

    // positionMs/durationMs now come straight from PlayerController's own ticker — no duplicate polling here.
    val positionMs: StateFlow<Long> = player.positionMs

    val favoriteIds: StateFlow<List<Long>> =
        repo.favoriteDao.getAllFavoriteIds().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics

    init {
        // Keep the crossfade duration in sync with what the user set in Settings.
        viewModelScope.launch {
            settings.crossfadeMs.collect { player.setCrossfadeMs(it) }
        }
        // Refresh lyrics whenever the current song changes.
        viewModelScope.launch {
            player.currentSong.collect { song ->
                _lyrics.value = null
                if (song != null) {
                    _lyrics.value = withContext(Dispatchers.IO) {
                        LyricsExtractor.extractEmbeddedLyrics(song.path)
                    }
                }
            }
        }
    }

    fun durationMs() = player.currentDurationMs()

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(songId)
            repo.toggleFavorite(songId, !isFav)
        }
    }

    fun startSleepTimer(minutes: Int) {
        com.arvin.player.util.SleepTimerManager.start(minutes, viewModelScope) {
            player.pause()
        }
    }

    fun cancelSleepTimer() = com.arvin.player.util.SleepTimerManager.cancel()
}
