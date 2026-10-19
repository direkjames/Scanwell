package com.scanwell.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Scanwell's own look. Instead of borrowing the wallpaper colors of the phone, the app keeps one
 * consistent indigo-to-violet identity (the color of the app icon) in both light and dark mode.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E5FF),
    onPrimaryContainer = Color(0xFF17125E),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEE5FF),
    onSecondaryContainer = Color(0xFF2B0B63),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F3FA),
    onTertiaryContainer = Color(0xFF053B49),
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF14142B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14142B),
    surfaceVariant = Color(0xFFEDEDF7),
    onSurfaceVariant = Color(0xFF5A5A75),
    surfaceTint = Color(0xFF4F46E5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFE),
    surfaceContainer = Color(0xFFF1F1F9),
    surfaceContainerHigh = Color(0xFFEBEBF5),
    surfaceContainerHighest = Color(0xFFE4E4F0),
    outline = Color(0xFFB9B9CF),
    outlineVariant = Color(0xFFE2E2EE),
    error = Color(0xFFD92D20),
    onError = Color.White,
    errorContainer = Color(0xFFFDE4E1),
    onErrorContainer = Color(0xFF55120C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9AAFF),
    onPrimary = Color(0xFF16126B),
    primaryContainer = Color(0xFF2E2A93),
    onPrimaryContainer = Color(0xFFE3E2FF),
    secondary = Color(0xFFCDB6FF),
    onSecondary = Color(0xFF330F72),
    secondaryContainer = Color(0xFF4A2A93),
    onSecondaryContainer = Color(0xFFEDE3FF),
    tertiary = Color(0xFF6FD6EE),
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFF0C4F5E),
    onTertiaryContainer = Color(0xFFCDF1FA),
    background = Color(0xFF0B0C16),
    onBackground = Color(0xFFE7E7F5),
    surface = Color(0xFF12131F),
    onSurface = Color(0xFFE7E7F5),
    surfaceVariant = Color(0xFF1E1F31),
    onSurfaceVariant = Color(0xFFA6A7C2),
    surfaceTint = Color(0xFFA9AAFF),
    surfaceContainerLowest = Color(0xFF0A0B14),
    surfaceContainerLow = Color(0xFF10111C),
    surfaceContainer = Color(0xFF171828),
    surfaceContainerHigh = Color(0xFF1D1E32),
    surfaceContainerHighest = Color(0xFF26273D),
    outline = Color(0xFF4A4B66),
    outlineVariant = Color(0xFF2A2B42),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF55120C),
    errorContainer = Color(0xFF6B1D16),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Colors the standard Material scheme has no slot for. Read them with [ScanwellTheme.extras]. */
@Immutable
data class ExtraColors(
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val success: Color,
    val successContainer: Color,
    val warm: Color,
    val warmContainer: Color,
) {
    /** The signature gradient used for the main Scan card and primary buttons. */
    val heroBrush: Brush get() = Brush.linearGradient(listOf(heroStart, heroEnd))
}

private val LightExtras = ExtraColors(
    heroStart = Color(0xFF4F46E5),
    heroEnd = Color(0xFF8B5CF6),
    onHero = Color.White,
    success = Color(0xFF12805C),
    successContainer = Color(0xFFDCF5EA),
    warm = Color(0xFFB45309),
    warmContainer = Color(0xFFFEEBD0),
)

private val DarkExtras = ExtraColors(
    heroStart = Color(0xFF4338CA),
    heroEnd = Color(0xFF7C3AED),
    onHero = Color.White,
    success = Color(0xFF5FD8A8),
    successContainer = Color(0xFF0F3D2D),
    warm = Color(0xFFF7B267),
    warmContainer = Color(0xFF47290A),
)

private val LocalExtras = staticCompositionLocalOf { LightExtras }

private val ScanwellShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

object ScanwellTheme {
    val extras: ExtraColors
        @Composable get() = LocalExtras.current
}

/** Wraps the whole app and gives every screen the same colors, shapes and text styles. */
@Composable
fun ScanwellTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalExtras provides if (dark) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = ScanwellTypography,
            shapes = ScanwellShapes,
            content = content,
        )
    }
}
