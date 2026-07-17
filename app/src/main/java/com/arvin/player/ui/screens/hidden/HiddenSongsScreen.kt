package com.arvin.player.ui.screens.hidden

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.arvin.player.R
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.icons.ArvinIcons
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

    if (hasPin == true && !unlocked) {
        LockScreen(onUnlocked = { unlocked = true })
        return
    }

    val hidden = remember(refreshKey) { repo.hiddenSongs() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                title = { Text(stringResource(R.string.hidden_songs), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(ArvinIcons.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (hidden.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(stringResource(R.string.no_hidden_songs), style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(hidden, key = { it.id }) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artist) },
                    trailingContent = {
                        IconButton(onClick = {
                            scope.launch {
                                repo.unhideSong(song.id)
                                refreshKey++
                            }
                        }) {
                            Icon(ArvinIcons.Visibility, contentDescription = stringResource(R.string.unhide))
                        }
                    },
                    modifier = Modifier.then(
                        androidx.compose.foundation.clickable { player.playQueue(hidden, hidden.indexOf(song)) }
                    )
                )
            }
        }
    }
}
