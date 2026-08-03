package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.domain.ThemeMode

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
    primaryContainer = Color(0x26A855F7),
    onPrimaryContainer = TextPrimary,
    secondary = AccentSecondary,
    onSecondary = TextPrimary,
    tertiary = AccentTertiary,
    onTertiary = TextPrimary,
    background = Color(0xFF000000),
    onBackground = TextPrimary,
    surface = Color(0xFF0A0A0E),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF14141A),
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val LightSpaceColorScheme = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFFC026D3),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF4F46E5),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
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

