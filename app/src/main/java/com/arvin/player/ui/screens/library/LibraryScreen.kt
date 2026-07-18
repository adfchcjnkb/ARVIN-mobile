package com.arvin.player.ui.screens.library

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arvin.player.R
import com.arvin.player.data.model.Album
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.components.MiniPlayer
import com.arvin.player.ui.components.SongListItem
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.navigation.Routes
import com.arvin.player.ui.theme.AuroraButtonBrush
import com.arvin.player.ui.theme.LocalArvinSkin
import com.arvin.player.util.PermissionState
import kotlin.math.abs

private enum class LibraryTab { SONGS, ALBUMS, ARTISTS, GENRES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val vm: LibraryViewModel = viewModel()
    val player = remember { PlayerController.getInstance(context) }
    val songs by vm.allSongs.collectAsState()
    val isScanning by vm.isScanning.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val audioGranted by PermissionState.audioPermissionGranted.collectAsState()
    var tab by remember { mutableStateOf(LibraryTab.SONGS) }
    var songBeingEdited by remember { mutableStateOf<com.arvin.player.data.model.Song?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBatchHideConfirm by remember { mutableStateOf(false) }

    fun exitSelection() {
        selectionMode = false
        selectedIds.clear()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text(stringResource(R.string.songs_count, selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { exitSelection() }, modifier = Modifier.pressScale()) {
                            Icon(ArvinIcons.ArrowBack, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { if (selectedIds.isNotEmpty()) showBatchHideConfirm = true },
                            modifier = Modifier.pressScale()
                        ) {
                            Icon(ArvinIcons.VisibilityOff, contentDescription = stringResource(R.string.hide_song))
                        }
                    }
                )
            } else {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SEARCH) }, modifier = Modifier.pressScale()) {
                        Icon(ArvinIcons.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { navController.navigate(Routes.PLAYLISTS) }, modifier = Modifier.pressScale()) {
                        Icon(ArvinIcons.QueueMusic, contentDescription = stringResource(R.string.playlists))
                    }
                    IconButton(onClick = { navController.navigate(Routes.EQUALIZER) }, modifier = Modifier.pressScale()) {
                        Icon(ArvinIcons.Tune, contentDescription = stringResource(R.string.equalizer))
                    }
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }, modifier = Modifier.pressScale()) {
                        Icon(ArvinIcons.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
            }
        },
        floatingActionButton = {
            if (audioGranted && songs.isNotEmpty()) {
                ShuffleFab(onClick = { vm.playAllShuffled() })
            }
        },
        bottomBar = { MiniPlayer(player) { navController.navigate(Routes.PLAYER) } }
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isScanning,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                if (!audioGranted) {
                    PermissionDeniedState(onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    })
                    return@Column
                }

                SegmentedTabs(
                    tabs = listOf(
                        stringResource(R.string.tab_songs),
                        stringResource(R.string.tab_albums),
                        stringResource(R.string.tab_artists),
                        stringResource(R.string.tab_genres)
                    ),
                    selected = tab.ordinal,
                    onSelect = { tab = LibraryTab.entries[it] }
                )

                if (isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (songs.isEmpty() && !isScanning) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.no_music_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                    return@Column
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var dragAccum = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (abs(dragAccum) > 80f) {
                                        val dir = if (dragAccum < 0) 1 else -1
                                        val newIndex = (tab.ordinal + dir).coerceIn(0, LibraryTab.entries.size - 1)
                                        tab = LibraryTab.entries[newIndex]
                                    }
                                    dragAccum = 0f
                                },
                                onDragCancel = { dragAccum = 0f },
                                onHorizontalDrag = { _, delta -> dragAccum += delta }
                            )
                        }
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            (slideInHorizontally(tween(260)) { w -> if (forward) w else -w })
                                .togetherWith(slideOutHorizontally(tween(260)) { w -> if (forward) -w else w })
                        },
                        label = "libraryTab"
                    ) { currentTab ->
                        when (currentTab) {
                            LibraryTab.SONGS -> LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)) {
                                items(songs, key = { it.id }) { song ->
                                    SongListItem(
                                        song = song,
                                        onClick = {
                                            if (selectionMode) {
                                                if (selectedIds.contains(song.id)) selectedIds.remove(song.id) else selectedIds.add(song.id)
                                            } else {
                                                vm.playSong(song)
                                            }
                                        },
                                        onLongClick = {
                                            if (!selectionMode) {
                                                selectionMode = true
                                                selectedIds.add(song.id)
                                            }
                                        },
                                        selectionMode = selectionMode,
                                        isSelected = selectedIds.contains(song.id),
                                        isFavorite = favoriteIds.contains(song.id),
                                        isCurrent = currentSong?.id == song.id,
                                        isPlaying = isPlaying && currentSong?.id == song.id,
                                        onToggleFavorite = { vm.toggleFavorite(song.id) },
                                        onHide = { vm.hideSong(song.id) },
                                        playlists = playlists,
                                        onAddToPlaylist = { playlistId -> vm.addToPlaylist(playlistId, song.id) },
                                        onCreatePlaylist = { name -> vm.createPlaylistAndAdd(name, song.id) },
                                        onEditMetadata = { songBeingEdited = song }
                                    )
                                }
                            }
                            LibraryTab.ALBUMS -> LazyVerticalGrid(
                                // Adaptive so phones get 2-3 columns and tablets get 4-6 automatically.
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(vm.albums(), key = { it.id }) { album ->
                                    AlbumCard(album = album, onClick = {
                                        val albumSongs = songs.filter { it.album == album.name }
                                        if (albumSongs.isNotEmpty()) vm.playSong(albumSongs.first(), albumSongs)
                                    })
                                }
                            }
                            LibraryTab.ARTISTS -> LazyColumn(contentPadding = PaddingValues(vertical = 6.dp)) {
                                items(vm.artists()) { artist ->
                                    RoundRow(
                                        title = artist.name,
                                        subtitle = "${artist.albumCount} · ${artist.songCount}",
                                        onClick = {
                                            val artistSongs = songs.filter { it.artist == artist.name }
                                            if (artistSongs.isNotEmpty()) vm.playSong(artistSongs.first(), artistSongs)
                                        }
                                    )
                                }
                            }
                            LibraryTab.GENRES -> LazyColumn(contentPadding = PaddingValues(vertical = 6.dp)) {
                                items(vm.genres()) { genre ->
                                    RoundRow(
                                        title = genre,
                                        subtitle = null,
                                        onClick = {
                                            val genreSongs = songs.filter { it.genre == genre }
                                            if (genreSongs.isNotEmpty()) vm.playSong(genreSongs.first(), genreSongs)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    songBeingEdited?.let { song ->
        com.arvin.player.ui.components.EditMetadataDialog(
            song = song,
            onDismiss = { songBeingEdited = null },
            onSaved = { vm.refresh() }
        )
    }

    if (showBatchHideConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchHideConfirm = false },
            title = { Text(stringResource(R.string.hide_song)) },
            text = { Text(stringResource(R.string.songs_count, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.hideSongs(selectedIds.toList())
                    showBatchHideConfirm = false
                    exitSelection()
                }) { Text(stringResource(R.string.hide_song)) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchHideConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SegmentedTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val skin = LocalArvinSkin.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(CircleShape)
            .background(skin.glassFill)
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { i, label ->
            val isSel = i == selected
            val textColor by animateColorAsState(
                if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .then(if (isSel) Modifier.background(AuroraButtonBrush) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 4.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    val artUri = android.net.Uri.parse("content://media/external/audio/albumart/${album.id}")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.97f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        AsyncImage(
            model = artUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            album.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoundRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(title.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ShuffleFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .pressScale(0.9f)
            .shadow(18.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
            .clip(CircleShape)
            .background(AuroraButtonBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(ArvinIcons.Shuffle, contentDescription = stringResource(R.string.shuffle_all), tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun PermissionDeniedState(onOpenSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.permission_needed_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permission_needed_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}
