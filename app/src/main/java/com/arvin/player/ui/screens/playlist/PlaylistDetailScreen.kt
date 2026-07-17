package com.arvin.player.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.components.SongListItem
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.theme.AuroraButtonBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(navController: NavHostController, playlistId: Long) {
    val vm: PlaylistViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val player = remember { PlayerController.getInstance(context) }
    val songs by vm.songsFor(playlistId).collectAsState()
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (songs.isNotEmpty()) {
                item {
                    PlaylistHeader(
                        songs = songs,
                        onPlay = { vm.playPlaylist(songs, 0) },
                        onShuffle = { vm.playPlaylist(songs.shuffled(), 0) }
                    )
                }
            }
            items(songs, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { vm.playPlaylist(songs, songs.indexOf(song)) },
                    isCurrent = currentSong?.id == song.id,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    onMoreClick = { vm.removeSong(playlistId, song.id) }
                )
            }
        }
    }
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
