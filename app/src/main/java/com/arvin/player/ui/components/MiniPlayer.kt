package com.arvin.player.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arvin.player.R
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.theme.AdaptiveSize
import com.arvin.player.ui.theme.AuroraButtonBrush

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(player: PlayerController, onExpand: () -> Unit) {
    val context = LocalContext.current
    val song by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val current = song
    var menuExpanded by remember { mutableStateOf(false) }

    // Slides up when a track exists, slides away when the queue empties.
    AnimatedVisibility(
        visible = current != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (current == null) return@AnimatedVisibility
        val scale = AdaptiveSize.scale()
        val iconSize = AdaptiveSize.dp(24)
        val shape = RoundedCornerShape(22.dp)
        val artUri = android.net.Uri.parse("content://media/external/audio/albumart/${current.albumId}")
        val duration = durationMs.coerceAtLeast(1L)
        var dragging by remember { mutableStateOf(false) }
        var dragFraction by remember { mutableStateOf(0f) }
        val liveFraction = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        val fraction = if (dragging) dragFraction else liveFraction

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp) // a mini player shouldn't stretch edge-to-edge on a big tablet
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .pressScale(0.98f)
                .clickable(onClick = onExpand),
            shape = shape,
            strong = true
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = artUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(AdaptiveSize.dp(48))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22FFFFFF))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            current.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().basicMarquee()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayingBars(
                                color = MaterialTheme.colorScheme.primary,
                                playing = isPlaying,
                                modifier = Modifier.size(width = 13.dp * scale, height = 11.dp * scale)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                current.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // Keep prev → play → next → menu in a fixed left-to-right order even under RTL.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { player.skipPrevious() }, modifier = Modifier.pressScale()) {
                                Icon(ArvinIcons.SkipPrevious, contentDescription = stringResource(R.string.skip_previous), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(iconSize))
                            }
                            IconButton(onClick = { player.togglePlayPause() }, modifier = Modifier.pressScale()) {
                                Icon(
                                    if (isPlaying) ArvinIcons.Pause else ArvinIcons.Play,
                                    contentDescription = stringResource(R.string.play_pause),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            IconButton(onClick = { player.skipNext() }, modifier = Modifier.pressScale()) {
                                Icon(ArvinIcons.SkipNext, contentDescription = stringResource(R.string.skip_next), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(iconSize))
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.pressScale()) {
                                    Icon(ArvinIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(iconSize))
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share)) },
                                        leadingIcon = { Icon(ArvinIcons.Share, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    context.getString(R.string.share_song_text, current.title, current.artist)
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, context.getString(R.string.share))
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.remove_from_queue)) },
                                        leadingIcon = { Icon(ArvinIcons.Delete, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            player.removeCurrentFromQueue()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                // Thin gradient progress line hugging the bottom of the pill — draggable to scrub.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 10.dp)
                            .height(if (dragging) 16.dp else 10.dp)
                            .pointerInput(duration) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        dragging = true
                                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    },
                                    onDragEnd = {
                                        player.seekTo((dragFraction * duration).toLong())
                                        dragging = false
                                    },
                                    onDragCancel = { dragging = false },
                                    onHorizontalDrag = { change, delta ->
                                        change.consume()
                                        dragFraction = (dragFraction + delta / size.width.toFloat()).coerceIn(0f, 1f)
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cy = size.height / 2f
                            val trackH = 3.dp.toPx()
                            val corner = CornerRadius(trackH, trackH)
                            drawRoundRect(
                                color = Color(0x22FFFFFF),
                                topLeft = Offset(0f, cy - trackH / 2f),
                                size = Size(size.width, trackH),
                                cornerRadius = corner
                            )
                            drawRoundRect(
                                brush = AuroraButtonBrush,
                                topLeft = Offset(0f, cy - trackH / 2f),
                                size = Size(size.width * fraction, trackH),
                                cornerRadius = corner
                            )
                            if (dragging) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.dp.toPx(),
                                    center = Offset(size.width * fraction, cy)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
