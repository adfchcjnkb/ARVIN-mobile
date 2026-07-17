package com.arvin.player.ui.screens.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arvin.player.R
import com.arvin.player.ui.components.GlassCard
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.theme.LocalArvinSkin

private val LABELS = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
private const val BAND_MIN = -1500f
private const val BAND_MAX = 1500f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(navController: NavHostController) {
    val vm: EqualizerViewModel = viewModel()
    val enabled by vm.enabled.collectAsState()
    val bassBoost by vm.bassBoost.collectAsState()
    val virtualizer by vm.virtualizerStrength.collectAsState()

    val levels = remember { mutableStateListOf(*Array(10) { 0f }) }

    fun pushBands() {
        if (!enabled) vm.setEnabled(true)
        vm.setBands(levels.map { it.toInt().toShort() })
    }

    val accent = MaterialTheme.colorScheme.primary
    val effectColors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(stringResource(R.string.equalizer), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.pressScale()) {
                        Icon(ArvinIcons.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { vm.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.presets), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CUSTOM_PRESETS.keys.toList()) { name ->
                    AssistChip(
                        onClick = {
                            CUSTOM_PRESETS[name]?.forEachIndexed { i, v -> levels[i] = v.toFloat() }
                            pushBands()
                        },
                        label = { Text(name) },
                        modifier = Modifier.pressScale()
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            GlassCard(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    levels.forEachIndexed { index, level ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VerticalBandSlider(
                                value = level,
                                onValueChange = { levels[index] = it; pushBands() },
                                accent = accent,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth().height(190.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                LABELS[index],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = {
                    for (i in levels.indices) levels[i] = 0f
                    pushBands()
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text(stringResource(R.string.reset)) }

            EffectSlider(
                title = stringResource(R.string.bass_boost),
                value = bassBoost.toFloat(),
                onValueChange = { vm.setBassBoost(it.toInt()) },
                colors = effectColors
            )
            Spacer(Modifier.height(12.dp))
            EffectSlider(
                title = stringResource(R.string.effect_3d),
                value = virtualizer.toFloat(),
                onValueChange = { vm.setVirtualizer(it.toInt()) },
                colors = effectColors
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * A vertical, draggable EQ band. Touch or drag anywhere on it to set the gain; the round thumb
 * follows your finger. It grabs the gesture on touch-down (consuming it) so it never fights a
 * parent scroll, and its touch target is the full column width so it's always easy to reach.
 */
@Composable
private fun VerticalBandSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    accent: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val fraction = ((value - BAND_MIN) / (BAND_MAX - BAND_MIN)).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            fun emit(y: Float) {
                val f = (1f - (y / size.height.toFloat())).coerceIn(0f, 1f)
                onValueChange(BAND_MIN + f * (BAND_MAX - BAND_MIN))
            }
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                emit(down.position.y)
                down.consume()
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    emit(change.position.y)
                    change.consume()
                }
            }
        }
    ) {
        val cx = size.width / 2f
        val trackW = 6.dp.toPx()
        val thumbR = 9.dp.toPx()
        val top = thumbR
        val bottom = size.height - thumbR
        val usable = bottom - top
        val thumbY = bottom - fraction * usable
        val centerY = top + usable / 2f
        val corner = CornerRadius(trackW, trackW)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(cx - trackW / 2f, top),
            size = Size(trackW, usable),
            cornerRadius = corner
        )
        val fillTop = minOf(centerY, thumbY)
        val fillBottom = maxOf(centerY, thumbY)
        drawRoundRect(
            color = accent,
            topLeft = Offset(cx - trackW / 2f, fillTop),
            size = Size(trackW, fillBottom - fillTop),
            cornerRadius = corner
        )
        drawCircle(color = accent.copy(alpha = 0.30f), radius = thumbR + 3.dp.toPx(), center = Offset(cx, thumbY))
        drawCircle(color = accent, radius = thumbR, center = Offset(cx, thumbY))
        drawCircle(color = Color.White, radius = thumbR * 0.46f, center = Offset(cx, thumbY))
    }
}

@Composable
private fun EffectSlider(title: String, value: Float, onValueChange: (Float) -> Unit, colors: SliderColors) {
    val skin = LocalArvinSkin.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(skin.glassFill)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1000f, colors = colors)
    }
}
