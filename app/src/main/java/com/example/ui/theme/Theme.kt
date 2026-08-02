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

@Composable
fun REDTheme(
    themeMode: ThemeMode = ThemeMode.DARK_NAVY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK_NAVY -> SpaceColorScheme
        ThemeMode.OLED_BLACK -> OledSpaceColorScheme
        ThemeMode.LIGHT -> SpaceColorScheme // Prompt requires deep space dark theme throughout
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

