package com.arvin.player.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.arvin.player.R
import com.arvin.player.data.model.AppTheme
import com.arvin.player.data.repository.SUPPORTED_LANGUAGES
import com.arvin.player.data.repository.SettingsRepository
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.navigation.Routes
import com.arvin.player.util.BiometricAuthenticator
import com.arvin.player.util.SecurePinStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val settings = remember { SettingsRepository.getInstance(context) }
    val player = remember { PlayerController.getInstance(context) }
    val scope = rememberCoroutineScope()
    val theme by settings.theme.collectAsState(initial = AppTheme.SYSTEM)
    // Language is owned by AppCompat's per-app locales (which actually re-render the UI), not DataStore.
    val language = AppCompatDelegate.getApplicationLocales().let { if (it.isEmpty) "en" else it.get(0)?.language ?: "en" }
    val gapless by settings.gaplessEnabled.collectAsState(initial = true)
    val crossfadeMs by settings.crossfadeMs.collectAsState(initial = 0)
    val customFolders by settings.customFolders.collectAsState(initial = emptySet())
    val hasPin by SecurePinStore.hasPinState.collectAsState()
    var biometricOn by remember { mutableStateOf(SecurePinStore.isBiometricEnabled(context)) }
    val biometricAvailable = remember { activity != null && BiometricAuthenticator.isAvailable(activity) }

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch {
                settings.addCustomFolder(uri.toString())
                com.arvin.player.data.repository.MusicRepository.getInstance(context).refreshLibrary()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(ArvinIcons.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {

            item {
                ListItem(headlineContent = { Text(stringResource(R.string.theme)) })
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppTheme.entries.forEach { t ->
                        FilterChip(
                            selected = theme == t,
                            onClick = { scope.launch { settings.setTheme(t) } },
                            label = { Text(t.name) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language)) },
                    supportingContent = { Text(SUPPORTED_LANGUAGES.find { it.code == language }?.nativeName ?: language) },
                    modifier = Modifier.clickable { showLanguageSheet = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.gapless_playback)) },
                    trailingContent = {
                        Switch(checked = gapless, onCheckedChange = { scope.launch { settings.setGapless(it) } }, colors = arvinSwitchColors())
                    }
                )
            }

            item {
                ListItem(headlineContent = { Text(stringResource(R.string.crossfade)) },
                    supportingContent = { Text(stringResource(R.string.crossfade_description)) })
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Slider(
                        value = crossfadeMs.toFloat(),
                        onValueChange = { ms ->
                            scope.launch { settings.setCrossfadeMs(ms.toInt()) }
                            player.setCrossfadeMs(ms.toInt())
                        },
                        valueRange = 0f..8000f
                    )
                    Text(
                        if (crossfadeMs == 0) stringResource(R.string.crossfade_off) else "${crossfadeMs / 1000.0}s",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.custom_folders)) },
                    supportingContent = { Text(stringResource(R.string.custom_folders_description)) },
                    trailingContent = {
                        IconButton(onClick = { folderPicker.launch(null) }) {
                            Icon(ArvinIcons.Folder, contentDescription = stringResource(R.string.add_folder))
                        }
                    }
                )
            }
            items(customFolders.toList()) { folderUri ->
                ListItem(
                    headlineContent = {
                        Text(
                            android.net.Uri.parse(folderUri).lastPathSegment ?: folderUri,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            scope.launch {
                                settings.removeCustomFolder(folderUri)
                                com.arvin.player.data.repository.MusicRepository.getInstance(context).refreshLibrary()
                            }
                        }) {
                            Icon(ArvinIcons.Delete, contentDescription = null)
                        }
                    }
                )
            }

            item { Divider(modifier = Modifier.padding(vertical = 12.dp)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.hidden_songs)) },
                    leadingContent = { Icon(ArvinIcons.VisibilityOff, contentDescription = null) },
                    modifier = Modifier.clickable { navController.navigate(Routes.HIDDEN_SONGS) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_lock_pin)) },
                    supportingContent = {
                        Text(if (hasPin == true) stringResource(R.string.pin_enabled) else stringResource(R.string.pin_disabled))
                    },
                    modifier = Modifier.clickable {
                        if (hasPin == true) showRemovePinDialog = true else showSetPinDialog = true
                    }
                )
            }

            if (hasPin == true && biometricAvailable) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.biometric_unlock)) },
                        supportingContent = { Text(stringResource(R.string.biometric_unlock_description)) },
                        trailingContent = {
                            Switch(checked = biometricOn, onCheckedChange = {
                                SecurePinStore.setBiometricEnabled(context, it)
                                biometricOn = it
                            }, colors = arvinSwitchColors())
                        }
                    )
                }
            }

            item { Divider(modifier = Modifier.padding(vertical = 12.dp)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text(stringResource(R.string.credits_line)) }
                )
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            LazyColumn {
                items(SUPPORTED_LANGUAGES) { lang ->
                    ListItem(
                        headlineContent = { Text(lang.nativeName) },
                        modifier = Modifier.clickable {
                            showLanguageSheet = false
                            // Actually applies the locale (and recreates the UI in that language + font).
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.code))
                        }
                    )
                }
            }
        }
    }

    if (showSetPinDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text(stringResource(R.string.set_pin)) },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it },
                    label = { Text(stringResource(R.string.pin)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pin.length >= 4) SecurePinStore.setPin(context, pin)
                    showSetPinDialog = false
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = { TextButton(onClick = { showSetPinDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showRemovePinDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePinDialog = false },
            title = { Text(stringResource(R.string.remove_pin_title)) },
            text = { Text(stringResource(R.string.remove_pin_body)) },
            confirmButton = {
                TextButton(onClick = {
                    SecurePinStore.setPin(context, null)
                    biometricOn = false
                    showRemovePinDialog = false
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { showRemovePinDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** High-contrast switch colours so on/off state is obvious in both light and dark themes. */
@Composable
private fun arvinSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)
