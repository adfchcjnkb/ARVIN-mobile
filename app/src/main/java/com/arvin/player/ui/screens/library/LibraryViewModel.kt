package com.arvin.player.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arvin.player.data.local.PlaylistEntity
import com.arvin.player.data.model.Song
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.media.PlayerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.getInstance(app)
    private val player = PlayerController.getInstance(app)

    val allSongs: StateFlow<List<Song>> = repo.allSongs
    val isScanning: StateFlow<Boolean> = repo.isScanning

    val favoriteIds: StateFlow<List<Long>> =
        repo.favoriteDao.getAllFavoriteIds().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> =
        repo.playlistDao.getAllPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        player.setSongLookup { id -> repo.allSongs.value.find { it.id == id } }
        repo.startWatchingMediaStore()
    }

    fun refresh() { viewModelScope.launch { repo.refreshLibrary() } }

    fun albums() = repo.albums()
    fun artists() = repo.artists()
    fun genres() = repo.genres()

    fun playSong(song: Song, within: List<Song> = allSongs.value) {
        val index = within.indexOf(song).coerceAtLeast(0)
        player.playQueue(within, index)
    }

    fun playAllShuffled() {
        val songs = allSongs.value.shuffled()
        if (songs.isNotEmpty()) player.playQueue(songs, 0)
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(songId)
            repo.toggleFavorite(songId, !isFav)
            com.arvin.player.util.AppNotifier.notify(
                if (!isFav) com.arvin.player.R.string.notif_favorite_added else com.arvin.player.R.string.notif_favorite_removed
            )
        }
    }

    fun hideSong(songId: Long) {
        viewModelScope.launch {
            repo.hideSong(songId)
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_song_hidden)
        }
    }

    fun hideSongs(songIds: List<Long>) {
        viewModelScope.launch {
            songIds.forEach { repo.hideSong(it) }
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_song_hidden)
        }
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repo.addToPlaylist(playlistId, songId)
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_added_to_playlist)
        }
    }

    fun createPlaylistAndAdd(name: String, songId: Long) {
        viewModelScope.launch {
            val id = repo.createPlaylist(name)
            repo.addToPlaylist(id, songId)
            com.arvin.player.util.AppNotifier.notify(com.arvin.player.R.string.notif_playlist_created)
        }
    }
}
