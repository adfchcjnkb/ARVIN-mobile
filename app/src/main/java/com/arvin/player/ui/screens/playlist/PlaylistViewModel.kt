package com.arvin.player.ui.screens.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arvin.player.data.local.PlaylistEntity
import com.arvin.player.data.model.Song
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.media.PlayerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.getInstance(app)
    private val player = PlayerController.getInstance(app)

    val playlists: StateFlow<List<PlaylistEntity>> =
        repo.playlistDao.getAllPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repo.createPlaylist(name)
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_playlist_created)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repo.playlistDao.deletePlaylist(playlist)
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_playlist_deleted)
        }
    }

    fun songsFor(playlistId: Long): StateFlow<List<Song>> =
        repo.playlistDao.getSongsForPlaylist(playlistId)
            .map { refs -> refs.mapNotNull { ref -> repo.allSongs.value.find { it.id == ref.songId } } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSong(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repo.addToPlaylist(playlistId, songId)
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_added_to_playlist)
        }
    }

    fun addSongs(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            songIds.forEach { repo.addToPlaylist(playlistId, it) }
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_added_to_playlist)
        }
    }

    fun removeSong(playlistId: Long, songId: Long) {
        viewModelScope.launch { repo.playlistDao.removeSongFromPlaylist(playlistId, songId) }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) player.playQueue(songs, startIndex)
    }

    /** Called after the edit-metadata dialog saves, so titles/art refresh without a manual pull-to-refresh. */
    fun refreshLibrary() {
        viewModelScope.launch { repo.refreshLibrary() }
    }
}
