package com.arvin.player.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.arvin.player.R
import com.arvin.player.ui.components.pressScale
import com.arvin.player.ui.icons.ArvinIcons
import com.arvin.player.ui.theme.AuroraButtonBrush
import com.arvin.player.ui.theme.LocalArvinSkin
import com.arvin.player.util.BiometricAuthenticator
import com.arvin.player.util.SecurePinStore

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val skin = LocalArvinSkin.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val biometricEnabled = remember { SecurePinStore.isBiometricEnabled(context) }
    val biometricAvailable = remember { activity != null && BiometricAuthenticator.isAvailable(activity) }
    val biometricTitle = stringResource(R.string.unlock_arvin_player)
    val biometricSubtitle = stringResource(R.string.use_biometric_to_unlock)
    val useDifferentMethod = stringResource(R.string.use_pin_instead)

    // Offer biometric unlock automatically as soon as the lock screen appears, if enabled.
    LaunchedEffect(Unit) {
        if (biometricEnabled && biometricAvailable && activity != null) {
            BiometricAuthenticator.authenticate(
                activity, biometricTitle, biometricSubtitle, useDifferentMethod,
                onSuccess = { onUnlocked() },
                onError = { /* user can still fall back to PIN entry below */ }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(24.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    .clip(CircleShape)
                    .background(AuroraButtonBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(ArvinIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.enter_pin),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) { pin = it; error = false } },
                label = { Text(stringResource(R.string.pin)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = skin.glassFill,
                    unfocusedContainerColor = skin.glassFill
                )
            )
            if (error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.wrong_pin),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .pressScale()
                    .clip(RoundedCornerShape(50))
                    .background(AuroraButtonBrush)
                    .clickable {
                        if (SecurePinStore.verifyPin(context, pin)) onUnlocked() else error = true
                    }
                    .padding(horizontal = 44.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.unlock), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }

            if (biometricEnabled && biometricAvailable && activity != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = {
                    BiometricAuthenticator.authenticate(
                        activity, biometricTitle, biometricSubtitle, useDifferentMethod,
                        onSuccess = { onUnlocked() },
                        onError = { }
                    )
                }) {
                    Icon(ArvinIcons.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.try_biometric_again))
                }
            }
        }
    }
}
