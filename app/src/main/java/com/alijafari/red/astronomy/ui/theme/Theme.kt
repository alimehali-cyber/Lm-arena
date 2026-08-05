package com.alijafari.red.astronomy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.ThemeMode

private val SpaceColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = TextPrimary,
    primaryContainer = Color(0x26A855F7),
    onPrimaryContainer = TextPrimary,
    secondary = AccentSecondary,
    onSecondary = TextPrimary,
    tertiary = AccentTertiary,
    onTertiary = TextPrimary,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1A1726),
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val OledSpaceColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = TextPrimary,
    primaryContainer = Color(0x33A855F7),
    onPrimaryContainer = TextPrimary,
    secondary = AccentSecondary,
    onSecondary = TextPrimary,
    tertiary = AccentTertiary,
    onTertiary = TextPrimary,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF080808),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A)
)

private val LightSpaceColorScheme = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDD6FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFFA21CAF),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF3730A3),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8)
)

@Composable
fun REDTheme(
    themeMode: ThemeMode = ThemeMode.DARK_NAVY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK_NAVY -> SpaceColorScheme
        ThemeMode.OLED_BLACK -> OledSpaceColorScheme
        ThemeMode.LIGHT -> LightSpaceColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

