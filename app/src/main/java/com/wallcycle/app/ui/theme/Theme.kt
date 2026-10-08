package com.wallcycle.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta do visual "vidro": texto claro sobre o papel de parede escurecido. */
object GlassPalette {
    val Accent = Color(0xFF8FE9DA)
    val Text = Color(0xFFF7F7FA)
    val TextDim = Color(0xB8F7F7FA)
    val Sheet = Color(0xF01B1C24) // diálogos e menus
}

private fun glassScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF06231F),
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,
    secondary = accent,
    secondaryContainer = Color.White.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,
    background = Color.Transparent,
    onBackground = GlassPalette.Text,
    surface = GlassPalette.Sheet,
    onSurface = GlassPalette.Text,
    surfaceVariant = Color.White.copy(alpha = 0.10f),
    onSurfaceVariant = GlassPalette.TextDim,
    surfaceContainerLowest = GlassPalette.Sheet,
    surfaceContainerLow = GlassPalette.Sheet,
    surfaceContainer = GlassPalette.Sheet,
    surfaceContainerHigh = GlassPalette.Sheet,
    surfaceContainerHighest = Color.White.copy(alpha = 0.14f),
    outline = Color.White.copy(alpha = 0.40f),
    outlineVariant = Color.White.copy(alpha = 0.15f),
)

private val base = Typography()
private val AppTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp),
)

@Composable
fun WallCycleTheme(dynamicColor: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val accent = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context).primary
    } else {
        GlassPalette.Accent
    }
    MaterialTheme(colorScheme = glassScheme(accent), typography = AppTypography, content = content)
}
