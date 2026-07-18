package com.arvin.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arvin.player.data.model.AppTheme
import com.arvin.player.data.repository.MusicRepository
import com.arvin.player.data.repository.SettingsRepository
import com.arvin.player.media.PlayerController
import com.arvin.player.ui.components.AppNoticeHost
import com.arvin.player.ui.navigation.ArvinNavHost
import com.arvin.player.ui.screens.lock.LockScreen
import com.arvin.player.ui.theme.ArvinPlayerTheme
import com.arvin.player.ui.theme.LocalArvinSkin
import com.arvin.player.ui.theme.screenBackground
import com.arvin.player.util.PermissionState
import com.arvin.player.util.SecurePinStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Extends AppCompatActivity (a FragmentActivity subclass) so that BiometricPrompt has its required
 * FragmentActivity host AND per-app language via AppCompatDelegate.setApplicationLocales works on
 * every API level. Compose works the same way on top of it.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        PermissionState.refresh(this)
        SecurePinStore.hasPin(this) // primes the reactive hasPinState flow

        val permissions = mutableListOf(PermissionState.audioPermissionName(), Manifest.permission.RECORD_AUDIO).apply {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val requestPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val granted = results[PermissionState.audioPermissionName()] ?: false
            PermissionState.setGranted(granted)
            if (granted) {
                val repo = MusicRepository.getInstance(applicationContext)
                lifecycleScope.launch { repo.refreshLibrary() }
            }
        }

        setContent {
            val settingsRepo = remember { SettingsRepository.getInstance(applicationContext) }
            val theme by settingsRepo.theme.collectAsState(initial = AppTheme.SYSTEM)
            val hasPin by SecurePinStore.hasPinState.collectAsState()
            var unlocked by remember { mutableStateOf(false) }
            val audioGranted by PermissionState.audioPermissionGranted.collectAsState()

            LaunchedEffect(Unit) {
                requestPermissions.launch(permissions.toTypedArray())
                val repo = MusicRepository.getInstance(applicationContext)
                val settings = SettingsRepository.getInstance(applicationContext)
                val player = PlayerController.getInstance(applicationContext)
                player.setCrossfadeMs(settings.crossfadeMs.first())
                if (audioGranted) launch { repo.refreshLibrary() }
            }

            ArvinPlayerTheme(appTheme = theme) {
                Box(modifier = Modifier.fillMaxSize().screenBackground(LocalArvinSkin.current.isDark)) {
                    if (hasPin == true && !unlocked) {
                        LockScreen(onUnlocked = { unlocked = true })
                    } else {
                        ArvinNavHost()
                        AppNoticeHost()
                    }
                }
            }
        }
    }
}
