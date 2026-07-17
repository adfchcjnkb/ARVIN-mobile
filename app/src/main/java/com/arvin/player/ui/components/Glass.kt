package com.arvin.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.arvin.player.ui.theme.ArvinSkin
import com.arvin.player.ui.theme.LocalArvinSkin

/**
 * Frosted-glass surface.
 *
 * True backdrop blur needs a 3rd-party library or API-31 RenderEffect wiring that isn't portable
 * to minSdk 26, so this fakes the frosted look the reliable way: a translucent fill, a top-lit
 * hairline border, and a soft vertical sheen. Layered over the player's blurred album-art
 * backdrop (or any colourful gradient) it reads convincingly as glass on every device.
 */
fun Modifier.glassSurface(
    skin: ArvinSkin,
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false
): Modifier {
    val fill = if (strong) skin.glassFillStrong else skin.glassFill
    val sheenTop = if (skin.isDark) Color(0x24FFFFFF) else Color(0x40FFFFFF)
    return this
        .clip(shape)
        .background(fill, shape)
        .background(
            Brush.verticalGradient(listOf(sheenTop, Color.Transparent)),
            shape
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(skin.glassBorder, skin.glassBorder.copy(alpha = skin.glassBorder.alpha * 0.25f))
                )
            ),
            shape
        )
}

/** Convenience container that paints a glass surface and lays out [content] inside it. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val skin = LocalArvinSkin.current
    Box(
        modifier = modifier.glassSurface(skin, shape, strong),
        content = content
    )
}
