package com.arvin.player.ui.screens.hidden

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.arvin.player.ui.components.SongListItem
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.screens.lock.LockScreen
import com.arvin.player.util.SecurePinStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenSongsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.getInstance(context) }
    val player = remember { PlayerController.getInstance(context) }
    val scope = rememberCoroutineScope()
    val hasPin by SecurePinStore.hasPinState.collectAsState()
    var unlocked by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    if (hasPin == true && !unlocked) {
        LockScreen(onUnlocked = { unlocked = true })
        return
    }

    val hidden = remember(refreshKey) { repo.hiddenSongs() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.hidden_songs), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.pressScale()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (hidden.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_hidden_songs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(hidden, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { player.playQueue(hidden, hidden.indexOf(song)) },
                    isCurrent = currentSong?.id == song.id,
                    isPlaying = isPlaying && currentSong?.id == song.id,
                    onUnhide = {
                        scope.launch {
                            repo.unhideSong(song.id)
                            refreshKey++
                        }
                    }
                )
            }
        }
    }
}
