package com.arvin.player.ui.screens.player

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arvin.player.R
import com.arvin.player.data.model.RepeatMode
import com.arvin.player.media.ConnectionState
import com.arvin.player.ui.components.AlbumPalette
import com.arvin.player.ui.components.VisualizerView
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.components.rememberAlbumPalette
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.navigation.Routes
import com.arvin.player.ui.theme.AuroraButtonBrush
import com.arvin.player.ui.theme.LocalArvinSkin
import com.arvin.player.util.formatDuration
import kotlin.math.abs

private const val SEEK_STEP_MS = 10_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(navController: NavHostController) {
    val vm: PlayerViewModel = viewModel()
    val player = vm.player
    val song by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val positionMs by vm.positionMs.collectAsState()
    val durationMsState by player.durationMs.collectAsState()
    val shuffleOn by player.shuffleOn.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val speed by player.playbackSpeed.collectAsState()
    val connectionState by player.connectionState.collectAsState()
    val lastError by player.lastError.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val lyrics by vm.lyrics.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    LaunchedEffect(lastError) {
        lastError?.let {
            snackbarHostState.showSnackbar(it)
            player.clearError()
        }
    }

    val skin = LocalArvinSkin.current
    val albumArtUri = remember(song?.albumId) {
        android.net.Uri.parse("content://media/external/audio/albumart/${song?.albumId ?: 0}")
    }
    val palette = rememberAlbumPalette(if (song != null) albumArtUri else null)
    val onColor = if (skin.isDark) Color.White else Color(0xFF16151F)
    val mutedColor = onColor.copy(alpha = 0.66f)
    val isFav = song != null && favoriteIds.contains(song!!.id)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        PlayerBackdrop(palette = palette, isDark = skin.isDark)

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = onColor,
                        titleContentColor = onColor,
                        actionIconContentColor = onColor
                    ),
                    title = {
                        Text(
                            stringResource(R.string.now_playing),
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.pressScale()) {
                            Icon(ArvinIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                    },
                    actions = {
                        if (connectionState == ConnectionState.FAILED) {
                            Icon(
                                Icons.Filled.CloudOff,
                                contentDescription = stringResource(R.string.connection_failed),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        IconButton(onClick = { navController.navigate(Routes.EQUALIZER) }, modifier = Modifier.pressScale()) {
                            Icon(ArvinIcons.Tune, contentDescription = stringResource(R.string.equalizer))
                        }
                    }
                )
            }
        ) { padding ->

            val artAndVisual: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                    AlbumArt(
                        art = if (song != null) albumArtUri else null,
                        isPlaying = isPlaying,
                        accent = palette.accent,
                        onSeekBack = { player.seekTo((positionMs - SEEK_STEP_MS).coerceAtLeast(0)) },
                        onSeekForward = { player.seekTo((positionMs + SEEK_STEP_MS).coerceAtMost(durationMsState)) },
                        onSkipNext = { player.skipNext() },
                        onSkipPrevious = { player.skipPrevious() }
                    )
                    Spacer(Modifier.height(18.dp))
                    if (showLyrics) {
                        LyricsPanel(lyrics, onColor, mutedColor)
                    } else {
                        VisualizerView(
                            playing = isPlaying,
                            color = palette.accent,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            val controls: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        song?.title ?: stringResource(R.string.nothing_playing),
                        style = MaterialTheme.typography.headlineMedium,
                        color = onColor,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        song?.artist ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(22.dp))

                    // Seek bar + time stay left-to-right in every locale so progress always fills the
                    // familiar direction and the elapsed time sits on the left.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(Modifier.fillMaxWidth()) {
                            AuroraSeekBar(
                                positionMs = positionMs,
                                durationMs = durationMsState,
                                accent = palette.accent,
                                trackColor = onColor.copy(alpha = 0.18f),
                                thumbColor = onColor,
                                onSeek = { player.seekTo(it) }
                            )
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatDuration(positionMs), style = MaterialTheme.typography.labelSmall, color = mutedColor)
                                Text(formatDuration(durationMsState.coerceAtLeast(0)), style = MaterialTheme.typography.labelSmall, color = mutedColor)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Transport controls are pinned LTR: previous is always on the left and next on the
                    // right, even under Persian/Arabic RTL — matching how Spotify/Apple Music behave.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { player.toggleShuffle() }, modifier = Modifier.pressScale()) {
                                Icon(
                                    ArvinIcons.Shuffle, contentDescription = null,
                                    tint = if (shuffleOn) palette.accent else mutedColor
                                )
                            }
                            IconButton(onClick = { player.skipPrevious() }, modifier = Modifier.pressScale()) {
                                Icon(ArvinIcons.SkipPrevious, contentDescription = stringResource(R.string.skip_previous), tint = onColor, modifier = Modifier.size(38.dp))
                            }

                            PlayButton(isPlaying = isPlaying, accent = palette.accent) { player.togglePlayPause() }

                            IconButton(onClick = { player.skipNext() }, modifier = Modifier.pressScale()) {
                                Icon(ArvinIcons.SkipNext, contentDescription = stringResource(R.string.skip_next), tint = onColor, modifier = Modifier.size(38.dp))
                            }
                            IconButton(onClick = { player.cycleRepeatMode() }, modifier = Modifier.pressScale()) {
                                Icon(
                                    when (repeatMode) {
                                        RepeatMode.ONE -> ArvinIcons.RepeatOne
                                        else -> ArvinIcons.Repeat
                                    },
                                    contentDescription = null,
                                    tint = if (repeatMode != RepeatMode.OFF) palette.accent else mutedColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PillButton(
                            selected = isFav,
                            accent = palette.accent,
                            onColor = onColor,
                            onClick = { song?.let { vm.toggleFavorite(it.id) } }
                        ) {
                            Icon(
                                if (isFav) ArvinIcons.HeartFilled else ArvinIcons.HeartOutline,
                                contentDescription = stringResource(R.string.favorite),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        TextButton(onClick = { showLyrics = !showLyrics }) {
                            Text(
                                stringResource(if (showLyrics) R.string.show_visualizer else R.string.show_lyrics),
                                color = onColor
                            )
                        }
                        PillButton(
                            selected = false,
                            accent = palette.accent,
                            onColor = onColor,
                            onClick = { showSleepTimerSheet = true }
                        ) {
                            Icon(ArvinIcons.Bedtime, contentDescription = stringResource(R.string.sleep_timer), modifier = Modifier.size(18.dp))
                        }
                        PillButton(
                            selected = false,
                            accent = palette.accent,
                            onColor = onColor,
                            onClick = { showSpeedSheet = true }
                        ) {
                            Text("${speed}x", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (isLandscape) {
                Row(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {
                    artAndVisual(Modifier.weight(1f))
                    Spacer(Modifier.width(24.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        controls(Modifier.fillMaxWidth())
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    artAndVisual(Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    controls(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(onDismiss = { showSleepTimerSheet = false }, vm = vm)
    }
    if (showSpeedSheet) {
        SpeedSheet(onDismiss = { showSpeedSheet = false }, current = speed, onSelect = { player.setPlaybackSpeed(it) })
    }
}

/**
 * Full-bleed album-tinted backdrop. Everything is painted once in a single [drawBehind] — a base
 * vertical gradient, one soft accent glow, and a bottom legibility scrim. No blur, no per-frame
 * animation, no extra image decode, so it costs almost nothing and stays smooth on every device.
 * It only repaints when the album colours change (i.e. on track change).
 */
@Composable
private fun PlayerBackdrop(palette: AlbumPalette, isDark: Boolean) {
    val glowAlpha = if (isDark) 0.20f else 0.12f
    val scrim = if (isDark) Color.Black else Color.White
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(palette.top, palette.bottom)))
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(size.width * 0.28f, size.height * 0.24f),
                        radius = size.minDimension * 0.95f
                    )
                )
                drawRect(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to scrim.copy(alpha = 0.35f)
                    )
                )
            }
    )
}

@Composable
private fun AlbumArt(
    art: Any?,
    isPlaying: Boolean,
    accent: Color,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.86f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "artScale"
    )
    var dragAccumX by remember { mutableStateOf(0f) }
    val shape = RoundedCornerShape(30.dp)
    AsyncImage(
        model = art,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = 34.dp, shape = shape, spotColor = accent, ambientColor = Color.Black)
            .clip(shape)
            .background(Color(0x22FFFFFF))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) onSeekBack() else onSeekForward()
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragAccumX) > 120f) {
                            if (dragAccumX < 0) onSkipNext() else onSkipPrevious()
                        }
                        dragAccumX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> dragAccumX += dragAmount }
                )
            }
    )
}

@Composable
private fun PlayButton(isPlaying: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .pressScale(0.9f)
            .shadow(elevation = 24.dp, shape = CircleShape, spotColor = accent, ambientColor = accent)
            .clip(CircleShape)
            .background(AuroraButtonBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isPlaying, animationSpec = tween(200), label = "playIcon") { playing ->
            Icon(
                if (playing) ArvinIcons.Pause else ArvinIcons.Play,
                contentDescription = stringResource(R.string.play_pause),
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun PillButton(
    selected: Boolean,
    accent: Color,
    onColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val skin = LocalArvinSkin.current
    Box(
        modifier = Modifier
            .pressScale()
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.22f) else skin.glassFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (selected) accent else onColor) {
            content()
        }
    }
}

/** Thin, glowing seek bar with scrub-to-seek and tap-to-seek. */
@Composable
private fun AuroraSeekBar(
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    trackColor: Color,
    thumbColor: Color,
    onSeek: (Long) -> Unit
) {
    val duration = durationMs.coerceAtLeast(1L)
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    val liveFraction = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val fraction = if (dragging) dragFraction else liveFraction

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek((f * duration).toLong())
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = { onSeek((dragFraction * duration).toLong()); dragging = false },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { _, delta ->
                        dragFraction = (dragFraction + delta / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val cy = size.height / 2f
        val trackH = 5.dp.toPx()
        val corner = CornerRadius(trackH, trackH)
        val fillW = size.width * fraction
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, cy - trackH / 2f),
            size = Size(size.width, trackH),
            cornerRadius = corner
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(0f, cy - trackH / 2f),
            size = Size(fillW, trackH),
            cornerRadius = corner
        )
        // glow + thumb
        drawCircle(color = accent.copy(alpha = 0.35f), radius = if (dragging) 16.dp.toPx() else 11.dp.toPx(), center = Offset(fillW, cy))
        drawCircle(color = thumbColor, radius = if (dragging) 9.dp.toPx() else 7.dp.toPx(), center = Offset(fillW, cy))
    }
}

@Composable
private fun LyricsPanel(lyrics: String?, onColor: Color, mutedColor: Color) {
    val skin = LocalArvinSkin.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(skin.glassFill)
            .padding(18.dp),
        contentAlignment = if (lyrics == null) Alignment.Center else Alignment.TopStart
    ) {
        androidx.compose.foundation.lazy.LazyColumn {
            item {
                Text(
                    lyrics ?: stringResource(R.string.no_lyrics_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lyrics == null) mutedColor else onColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(onDismiss: () -> Unit, vm: PlayerViewModel) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.sleep_timer), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            listOf(10, 15, 30, 45, 60, 90).forEach { minutes ->
                TextButton(onClick = { vm.startSleepTimer(minutes); onDismiss() }) {
                    Text("$minutes " + stringResource(R.string.minutes))
                }
            }
            TextButton(onClick = { vm.cancelSleepTimer(); onDismiss() }) {
                Text(stringResource(R.string.cancel_timer))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(onDismiss: () -> Unit, current: Float, onSelect: (Float) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.playback_speed), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { s ->
                TextButton(onClick = { onSelect(s); onDismiss() }) {
                    Text("${s}x", fontWeight = if (s == current) FontWeight.Bold else null)
                }
            }
        }
    }
}
