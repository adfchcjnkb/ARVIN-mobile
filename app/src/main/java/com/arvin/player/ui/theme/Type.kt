package com.arvin.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arvin.player.R

/**
 * Two font families are declared as XML font-family resources in res/font/:
 *  - vazir.xml      -> used for fa/ar (RTL) locales
 *  - noto_sans.xml  -> used for every other locale
 * ArvinTypography() picks the right one based on the active locale so Persian/Arabic
 * text renders in Vazir while everything else renders in Noto Sans.
 *
 * The scale is tuned for a modern player: tight tracking on big titles (Apple-ish),
 * comfortable body sizing, and clear muted-label steps.
 *
 * IMPORTANT: the actual .ttf files are NOT bundled (no network access during generation).
 * Download and drop these into app/src/main/res/font/ before building:
 *   - vazir_regular.ttf, vazir_medium.ttf   (Vazirmatn family, SIL OFL license)
 *   - notosans_regular.ttf, notosans_medium.ttf (Noto Sans, SIL OFL license)
 */
fun arvinTypography(isRtlScript: Boolean): Typography {
    val family = if (isRtlScript) FontFamily(androidx.compose.ui.text.font.Font(R.font.vazir_regular))
    else FontFamily(androidx.compose.ui.text.font.Font(R.font.notosans_regular))

    val bold = if (isRtlScript) FontFamily(androidx.compose.ui.text.font.Font(R.font.vazir_medium, FontWeight.Medium))
    else FontFamily(androidx.compose.ui.text.font.Font(R.font.notosans_medium, FontWeight.Medium))

    return Typography(
        displaySmall = TextStyle(fontFamily = bold, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
        headlineLarge = TextStyle(fontFamily = bold, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.4).sp),
        headlineMedium = TextStyle(fontFamily = bold, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
        titleLarge = TextStyle(fontFamily = bold, fontWeight = FontWeight.Medium, fontSize = 19.sp, letterSpacing = (-0.2).sp),
        titleMedium = TextStyle(fontFamily = bold, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = family, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = family, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = family, fontSize = 13.sp),
        labelLarge = TextStyle(fontFamily = bold, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = family, fontSize = 12.sp, letterSpacing = 0.3.sp),
        labelSmall = TextStyle(fontFamily = family, fontSize = 11.sp, letterSpacing = 0.4.sp)
    )
}
