package com.arvin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arvin.player.R
import com.arvin.player.data.local.PlaylistEntity
import com.arvin.player.data.model.Song
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.util.formatDuration

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    isFavorite: Boolean = false,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    onUnhide: (() -> Unit)? = null,
    playlists: List<PlaylistEntity> = emptyList(),
    onAddToPlaylist: ((Long) -> Unit)? = null,
    onCreatePlaylist: ((String) -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    // Long-press multi-select (used by the library's "select several songs to hide at once" mode).
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    // legacy no-op fallback so older call sites without the richer menu still compile
    onMoreClick: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val accent = MaterialTheme.colorScheme.primary
    val titleColor = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface
    val artSize = com.arvin.player.ui.theme.AdaptiveSize.dp(54)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val artUri = android.net.Uri.parse("content://media/external/audio/albumart/${song.albumId}")
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = artUri,
                contentDescription = null,
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            if (isCurrent && !selectionMode) {
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    PlayingBars(
                        color = Color.White,
                        playing = isPlaying,
                        modifier = Modifier.size(width = 20.dp, height = 16.dp)
                    )
                }
            }
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSelected) ArvinIcons.CheckCircle else ArvinIcons.CircleOutline,
                        contentDescription = null,
                        tint = if (isSelected) accent else Color.White
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${song.artist} · ${formatDuration(song.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selectionMode) return@Row

        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.pressScale()) {
                Icon(
                    if (isFavorite) ArvinIcons.HeartFilled else ArvinIcons.HeartOutline,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.pressScale()) {
                Icon(ArvinIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_playlist)) },
                        leadingIcon = { Icon(ArvinIcons.PlaylistAdd, contentDescription = null) },
                        onClick = { menuExpanded = false; showAddToPlaylist = true }
                    )
                }
                if (onEditMetadata != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_info)) },
                        leadingIcon = { Icon(ArvinIcons.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEditMetadata() }
                    )
                }
                if (onHide != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.hide_song)) },
                        leadingIcon = { Icon(ArvinIcons.VisibilityOff, contentDescription = null) },
                        onClick = { menuExpanded = false; onHide() }
                    )
                }
                if (onUnhide != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.unhide)) },
                        leadingIcon = { Icon(ArvinIcons.Visibility, contentDescription = null) },
                        onClick = { menuExpanded = false; onUnhide() }
                    )
                }
                if (onRemoveFromPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove)) },
                        leadingIcon = { Icon(ArvinIcons.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onRemoveFromPlaylist() }
                    )
                }
                if (onMoreClick != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove)) },
                        leadingIcon = { Icon(ArvinIcons.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onMoreClick() }
                    )
                }
            }
        }
    }

    if (showAddToPlaylist && onAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylist = false },
            title = { Text(stringResource(R.string.add_to_playlist)) },
            text = {
                Column {
                    playlists.forEach { pl ->
                        TextButton(onClick = {
                            onAddToPlaylist(pl.id)
                            showAddToPlaylist = false
                        }) { Text(pl.name) }
                    }
                    TextButton(onClick = {
                        showAddToPlaylist = false
                        showCreatePlaylistDialog = true
                    }) { Text(stringResource(R.string.new_playlist)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylist = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showCreatePlaylistDialog && onCreatePlaylist != null) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text(stringResource(R.string.new_playlist)) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.playlist_name)) })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) onCreatePlaylist(name)
                    showCreatePlaylistDialog = false
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
