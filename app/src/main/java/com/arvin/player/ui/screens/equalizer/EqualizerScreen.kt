package com.arvin.player.ui.screens.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arvin.player.R
import com.arvin.player.ui.components.GlassCard
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.icons.ArvinIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(navController: NavHostController) {
    val vm: EqualizerViewModel = viewModel()
    val enabled by vm.enabled.collectAsState()
    val bassBoost by vm.bassBoost.collectAsState()
    val virtualizer by vm.virtualizerStrength.collectAsState()

    // 10 UI sliders regardless of native band count — mapped in EqualizerManager.applyCustomBands
    val sliderLevels = remember { mutableStateListOf(*Array(10) { 0f }) }
    val accentColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    Switch(checked = enabled, onCheckedChange = { vm.setEnabled(it) })
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text(stringResource(R.string.presets), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            LazyRow {
                items(CUSTOM_PRESETS.keys.toList()) { name ->
                    AssistChip(
                        onClick = {
                            vm.applyCustomPreset(name)
                            CUSTOM_PRESETS[name]?.forEachIndexed { i, v -> sliderLevels[i] = v.toFloat() }
                        },
                        label = { Text(name) },
                        modifier = Modifier.padding(end = 8.dp).pressScale()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            GlassCard(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(300.dp).padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val labels = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
                    sliderLevels.forEachIndexed { index, level ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
                            Slider(
                                value = level,
                                onValueChange = {
                                    sliderLevels[index] = it
                                    vm.setBand(index.toShort(), it.toInt().toShort())
                                },
                                valueRange = -1500f..1500f,
                                colors = accentColors,
                                modifier = Modifier
                                    .fillMaxHeight(0.82f)
                                    .width(40.dp)
                                    .rotate(-90f)
                            )
                            Text(labels[index], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            EffectSlider(
                title = stringResource(R.string.bass_boost),
                value = bassBoost.toFloat(),
                onValueChange = { vm.setBassBoost(it.toInt()) },
                colors = accentColors
            )
            Spacer(Modifier.height(16.dp))
            EffectSlider(
                title = stringResource(R.string.effect_3d),
                value = virtualizer.toFloat(),
                onValueChange = { vm.setVirtualizer(it.toInt()) },
                colors = accentColors
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EffectSlider(title: String, value: Float, onValueChange: (Float) -> Unit, colors: SliderColors) {
    val skin = com.arvin.player.ui.theme.LocalArvinSkin.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(skin.glassFill, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1000f, colors = colors)
    }
}
