package com.arvin.player.ui.screens.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arvin.player.R
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.components.MiniPlayer
import com.arvin.player.ui.components.SongListItem
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.navigation.Routes
import com.arvin.player.util.AppNotifier
import kotlinx.coroutines.launch

/** "Liked songs" — every song the user has hearted, shown just like the normal library list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.getInstance(context) }
    val player = remember { PlayerController.getInstance(context) }
    val scope = rememberCoroutineScope()
    val allSongs by repo.allSongs.collectAsState()
    val favoriteIds by repo.favoriteDao.getAllFavoriteIds().collectAsState(initial = emptyList())
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val playlists by repo.playlistDao.getAllPlaylists().collectAsState(initial = emptyList())

    val liked = remember(allSongs, favoriteIds) { repo.favoriteSongs(favoriteIds) }
    var songBeingEdited by remember { mutableStateOf<com.arvin.player.data.model.Song?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.liked_songs), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.pressScale()) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = { MiniPlayer(player) { navController.navigate(Routes.PLAYER) } }
    ) { padding ->
        if (liked.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_liked_songs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
        ) {
            items(liked, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { player.playQueue(liked, liked.indexOf(song)) },
                    isFavorite = true,
                    isCurrent = currentSong?.id == song.id,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    onToggleFavorite = {
                        AppNotifier.notify(
                            if (favoriteIds.contains(song.id)) R.string.notif_favorite_removed else R.string.notif_favorite_added
                        )
                        scope.launch { repo.toggleFavorite(song.id, !favoriteIds.contains(song.id)) }
                    },
                    playlists = playlists,
                    onAddToPlaylist = { playlistId ->
                        scope.launch { repo.addToPlaylist(playlistId, song.id) }
                        AppNotifier.notify(R.string.notif_added_to_playlist)
                    },
                    onCreatePlaylist = { name ->
                        scope.launch {
                            val id = repo.createPlaylist(name)
                            repo.addToPlaylist(id, song.id)
                        }
                        AppNotifier.notify(R.string.notif_playlist_created)
                    },
                    onEditMetadata = { songBeingEdited = song },
                    onHide = {
                        scope.launch {
                            repo.hideSong(song.id)
                            AppNotifier.notify(R.string.notif_song_hidden)
                        }
                    }
                )
            }
        }
    }

    songBeingEdited?.let { song ->
        com.arvin.player.ui.components.EditMetadataDialog(
            song = song,
            onDismiss = { songBeingEdited = null },
            onSaved = { scope.launch { repo.refreshLibrary() } }
        )
    }
}
