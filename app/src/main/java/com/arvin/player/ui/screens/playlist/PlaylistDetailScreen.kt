package com.arvin.player.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arvin.player.R
import com.arvin.player.data.model.Song
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.components.EditMetadataDialog
import com.arvin.player.ui.components.MiniPlayer
import com.arvin.player.ui.components.SongListItem
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.navigation.Routes
import com.arvin.player.ui.theme.AuroraButtonBrush
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(navController: NavHostController, playlistId: Long) {
    val vm: PlaylistViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val repo = remember { MusicRepository.getInstance(context) }
    val player = remember { PlayerController.getInstance(context) }
    val songs by vm.songsFor(playlistId).collectAsState()
    val allSongs by repo.allSongs.collectAsState()
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val favoriteIds by repo.favoriteDao.getAllFavoriteIds().collectAsState(initial = emptyList())
    val playlists by repo.playlistDao.getAllPlaylists().collectAsState(initial = emptyList())
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showAddSongs by remember { mutableStateOf(false) }
    var songBeingEdited by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.playlist), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.pressScale()) {
                        Icon(ArvinIcons.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .pressScale(0.9f)
                    .shadow(18.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .clip(CircleShape)
                    .background(AuroraButtonBrush)
                    .clickable { showAddSongs = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(ArvinIcons.Add, contentDescription = stringResource(R.string.add_songs), tint = Color.White, modifier = Modifier.size(28.dp))
            }
        },
        bottomBar = { MiniPlayer(player) { navController.navigate(Routes.PLAYER) } }
    ) { padding ->
        if (songs.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_songs_in_playlist),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                PlaylistHeader(
                    songs = songs,
                    onPlay = { vm.playPlaylist(songs, 0) },
                    onShuffle = { vm.playPlaylist(songs.shuffled(), 0) }
                )
            }
            items(songs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { vm.playPlaylist(songs, songs.indexOf(song)) },
                    isFavorite = favoriteIds.contains(song.id),
                    isCurrent = currentSong?.id == song.id,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    onToggleFavorite = {
                        val nowFav = !favoriteIds.contains(song.id)
                        scope.launch {
                            repo.toggleFavorite(song.id, nowFav)
                            com.arvin.player.util.AppNotifier.notify(
                                if (nowFav) R.string.notif_favorite_added else R.string.notif_favorite_removed
                            )
                        }
                    },
                    onHide = {
                        scope.launch {
                            repo.hideSong(song.id)
                            com.arvin.player.util.AppNotifier.notify(R.string.notif_song_hidden)
                        }
                    },
                    playlists = playlists,
                    onAddToPlaylist = { playlistId ->
                        scope.launch {
                            repo.addToPlaylist(playlistId, song.id)
                            com.arvin.player.util.AppNotifier.notify(R.string.notif_added_to_playlist)
                        }
                    },
                    onCreatePlaylist = { name ->
                        scope.launch {
                            val id = repo.createPlaylist(name)
                            repo.addToPlaylist(id, song.id)
                            com.arvin.player.util.AppNotifier.notify(R.string.notif_playlist_created)
                        }
                    },
                    onRemoveFromPlaylist = { vm.removeSong(playlistId, song.id) },
                    onEditMetadata = { songBeingEdited = song }
                )
            }
        }
    }

    if (showAddSongs) {
        AddSongsDialog(
            allSongs = allSongs,
            alreadyInPlaylist = songs.map { it.id }.toSet(),
            onDismiss = { showAddSongs = false },
            onConfirm = { ids ->
                vm.addSongs(playlistId, ids)
                showAddSongs = false
            }
        )
    }

    songBeingEdited?.let { song ->
        EditMetadataDialog(
            song = song,
            onDismiss = { songBeingEdited = null },
            onSaved = { vm.refreshLibrary() }
        )
    }
}

@Composable
private fun AddSongsDialog(
    allSongs: List<Song>,
    alreadyInPlaylist: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    val candidates = remember(allSongs, alreadyInPlaylist) { allSongs.filter { it.id !in alreadyInPlaylist } }
    val selected = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_songs)) },
        text = {
            if (candidates.isEmpty()) {
                Text(stringResource(R.string.no_music_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(candidates, key = { it.id }) { song ->
                        val isChecked = selected.contains(song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selected.remove(song.id) else selected.add(song.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = {
                                if (it) selected.add(song.id) else selected.remove(song.id)
                            })
                            Spacer(Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected.isNotEmpty(), onClick = { onConfirm(selected.toList()) }) {
                Text(stringResource(R.string.add_to_playlist))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun PlaylistHeader(songs: List<Song>, onPlay: () -> Unit, onShuffle: () -> Unit) {
    val coverUri = android.net.Uri.parse("content://media/external/audio/albumart/${songs.first().albumId}")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = coverUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(160.dp)
                .shadow(26.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.songs_count, songs.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderButton(text = stringResource(R.string.play), filled = true, onClick = onPlay) {
                Icon(ArvinIcons.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            HeaderButton(text = stringResource(R.string.shuffle_all), filled = false, onClick = onShuffle) {
                Icon(ArvinIcons.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeaderButton(text: String, filled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val skin = com.arvin.player.ui.theme.LocalArvinSkin.current
    Row(
        modifier = Modifier
            .pressScale()
            .clip(RoundedCornerShape(50))
            .then(
                if (filled) Modifier.background(AuroraButtonBrush)
                else Modifier.background(skin.glassFill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
