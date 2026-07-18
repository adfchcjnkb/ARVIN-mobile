package com.arvin.player.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arvin.player.R
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.components.MiniPlayer
import com.arvin.player.ui.components.SongListItem
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.theme.LocalArvinSkin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.getInstance(context) }
    val player = remember { PlayerController.getInstance(context) }
    val skin = LocalArvinSkin.current
    var query by remember { mutableStateOf("") }
    val results = remember(query) { repo.search(query) }
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.search), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.pressScale()) {
                        Icon(ArvinIcons.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = { MiniPlayer(player) { navController.navigate(com.arvin.player.ui.navigation.Routes.PLAYER) } }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_prompt)) },
                leadingIcon = { Icon(ArvinIcons.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = skin.glassFill,
                    unfocusedContainerColor = skin.glassFill,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (query.isBlank()) {
                SearchEmptyState()
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_music_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.id }) { song ->
                        SongListItem(
                            song = song,
                            onClick = { player.playQueue(results, results.indexOf(song)) },
                            isCurrent = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState() {
    val skin = LocalArvinSkin.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(skin.glassFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ArvinIcons.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.search_prompt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
