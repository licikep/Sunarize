package com.banglaread.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object BanglaReadColors {
    val Ink900       = Color(0xFF0E0C0A)
    val Ink800       = Color(0xFF1A1713)
    val Ink700       = Color(0xFF252019)
    val Ink600       = Color(0xFF312B22)
    val Gold400      = Color(0xFFD4A847)
    val Gold300      = Color(0xFFE8C06A)
    val Gold200      = Color(0xFFF2D98A)
    val GoldDim      = Color(0x33D4A847)
    val Cream100     = Color(0xFFF5EDD8)
    val Cream200     = Color(0xFFD6C9A8)
    val Cream400     = Color(0xFF8A7D62)
    val ReadDim      = Color(0xFF6B6355)
    val ErrorRed     = Color(0xFFE57373)
    val SuccessGreen = Color(0xFF81C784)
    val ChipPhilosophy = Color(0xFF4A6FA5)
    val ChipMotivation = Color(0xFF6B8E4E)
    val ChipLiterature = Color(0xFF8E4E6B)
    val ChipPsychology = Color(0xFF8E6B4E)
    val ChipBangla     = Color(0xFF4E7A8E)
    val ChipDefault    = Color(0xFF5A5247)
}

private val DarkColorScheme = darkColorScheme(
    primary            = BanglaReadColors.Gold400,
    onPrimary          = BanglaReadColors.Ink900,
    primaryContainer   = BanglaReadColors.GoldDim,
    onPrimaryContainer = BanglaReadColors.Gold200,
    secondary          = BanglaReadColors.Cream200,
    background         = BanglaReadColors.Ink900,
    onBackground       = BanglaReadColors.Cream100,
    surface            = BanglaReadColors.Ink800,
    onSurface          = BanglaReadColors.Cream100,
    surfaceVariant     = BanglaReadColors.Ink700,
    onSurfaceVariant   = BanglaReadColors.Cream200,
    outline            = BanglaReadColors.Ink600,
    outlineVariant     = BanglaReadColors.Ink700,
    error              = BanglaReadColors.ErrorRed,
)

// ── FONTS ──────────────────────────────────────────────────────────────────────
// TODO: After adding font .ttf files to res/font/, replace these with:
//
// val HindSiliguriFamily = FontFamily(
//     Font(R.font.hind_siliguri_regular, FontWeight.Normal),
//     Font(R.font.hind_siliguri_medium,  FontWeight.Medium),
//     Font(R.font.hind_siliguri_bold,    FontWeight.Bold),
// )
// val PlayfairFamily = FontFamily(
//     Font(R.font.playfair_display_regular, FontWeight.Normal),
//     Font(R.font.playfair_display_bold,    FontWeight.Bold),
// )
// Then replace FontFamily.Default below with HindSiliguriFamily / PlayfairFamily

val BanglaReadTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.Bold,   fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.Bold,   fontSize = 22.sp, lineHeight = 30.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.Serif,   fontWeight = FontWeight.Bold,   fontSize = 26.sp, lineHeight = 34.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 30.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 26.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    labelMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
)

object Spacing {
    val xxs: Dp = 4.dp; val xs: Dp = 8.dp; val sm: Dp = 12.dp
    val md: Dp = 16.dp; val lg: Dp = 24.dp; val xl: Dp = 32.dp; val xxl: Dp = 48.dp
}

data class BanglaReadThemeExtras(
    val goldAccent: Color, val goldDim: Color, val cream: Color,
    val creamMuted: Color, val cardBackground: Color, val readTitleColor: Color
)

val LocalThemeExtras = staticCompositionLocalOf {
    BanglaReadThemeExtras(BanglaReadColors.Gold400, BanglaReadColors.GoldDim, BanglaReadColors.Cream100, BanglaReadColors.Cream400, BanglaReadColors.Ink800, BanglaReadColors.ReadDim)
}

val MaterialTheme.extras: BanglaReadThemeExtras @Composable get() = LocalThemeExtras.current

@Composable
fun BanglaReadTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeExtras provides BanglaReadThemeExtras(BanglaReadColors.Gold400, BanglaReadColors.GoldDim, BanglaReadColors.Cream100, BanglaReadColors.Cream400, BanglaReadColors.Ink800, BanglaReadColors.ReadDim)) {
        MaterialTheme(colorScheme = DarkColorScheme, typography = BanglaReadTypography, content = content)
    }
}
