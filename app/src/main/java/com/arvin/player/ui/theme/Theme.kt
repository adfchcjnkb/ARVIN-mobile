package com.arvin.player.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.arvin.player.data.model.AppTheme

private val ArvinDarkScheme = darkColorScheme(
    primary = AuroraViolet,
    onPrimary = Color.White,
    primaryContainer = ArvinPrimaryVariant,
    secondary = AuroraPink,
    onSecondary = Color.White,
    tertiary = AuroraCyan,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = AuroraError
)

private val ArvinLightScheme = lightColorScheme(
    primary = ArvinPrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    secondary = AuroraPink,
    onSecondary = Color.White,
    tertiary = AuroraIndigo,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    surfaceContainerHigh = LightSurfaceElevated,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = AuroraError
)

@Composable
fun ArvinPlayerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> systemDark
    }
    val colorScheme = if (useDark) ArvinDarkScheme else ArvinLightScheme
    val skin = if (useDark) DarkSkin else LightSkin

    val locale = LocalConfiguration.current.locales[0]
    val isRtlScript = locale.language == "fa" || locale.language == "ar"

    // Keep the status/navigation bar icons legible over our translucent, edge-to-edge backgrounds.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context.findActivity())?.let { activity ->
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalArvinSkin provides skin) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = arvinTypography(isRtlScript),
            content = content
        )
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
