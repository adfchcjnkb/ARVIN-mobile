package com.arvin.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arvin.player.util.AppNotifier

/**
 * Sits once at the top of the composition (see MainActivity). Every screen just calls
 * AppNotifier.notify(R.string.xxx) and this shows it — translated automatically since it goes
 * through context.getString, which respects the user's chosen app language.
 */
@Composable
fun AppNoticeHost() {
    val context = LocalContext.current
    val hostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        AppNotifier.notices.collect { notice ->
            val text = if (notice.args.isEmpty()) {
                context.getString(notice.resId)
            } else {
                context.getString(notice.resId, *notice.args.toTypedArray())
            }
            hostState.showSnackbar(text)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 88.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState) { data ->
            Snackbar { Text(data.visuals.message) }
        }
    }
}
