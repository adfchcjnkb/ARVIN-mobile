package com.arvin.player.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Arvin "Aurora" palette.
 *
 * The brand DNA stays a violet→pink aurora, but the surfaces are now a layered, near-black
 * dark theme (Spotify-style depth) and a soft, cool-white light theme (Apple-style calm).
 * The gradient/glass tokens live in [Gradients.kt].
 */

// ---- Brand aurora accents ----
val AuroraViolet = Color(0xFF8B5CF6)   // primary violet
val AuroraIndigo = Color(0xFF6366F1)   // deep indigo edge of the gradient
val AuroraPink = Color(0xFFEC4899)     // magenta/pink
val AuroraMagenta = Color(0xFFF472B6)  // lighter pink highlight
val AuroraCyan = Color(0xFF22D3EE)     // cool accent used sparingly (glass highlights)

// Back-compat names (referenced by Theme.kt and older call sites)
val ArvinPrimary = AuroraViolet
val ArvinPrimaryVariant = Color(0xFF7C3AED)
val ArvinSecondary = AuroraPink

// ---- Dark theme: layered near-black ----
val DarkBackground = Color(0xFF08080C)      // app backdrop, almost black with a blue undertone
val DarkSurface = Color(0xFF101017)         // cards / sheets
val DarkSurfaceVariant = Color(0xFF1B1B26)  // elevated chips, inputs
val DarkSurfaceElevated = Color(0xFF232331)
val DarkOnSurface = Color(0xFFF3F2F8)
val DarkOnSurfaceMuted = Color(0xFFA9A7B8)  // secondary text
val DarkOutline = Color(0xFF2C2B38)

// ---- Light theme: soft cool white ----
val LightBackground = Color(0xFFF6F5FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEBF7)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF16151F)
val LightOnSurfaceMuted = Color(0xFF6B6879)
val LightOutline = Color(0xFFE1DEEE)

// Shared
val AuroraError = Color(0xFFFF5D6C)
