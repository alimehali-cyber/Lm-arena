package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.domain.ThemeMode

private val NavyColorScheme = darkColorScheme(
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = NavyOnPrimaryContainer,
    secondary = AccentCyan,
    onSecondary = NavyOnPrimary,
    background = NavyBackground,
    onBackground = NavyTextPrimary,
    surface = NavySurface,
    onSurface = NavyTextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = NavyTextSecondary,
    outline = NavyOutline
)

private val OledColorScheme = darkColorScheme(
    primary = OledPrimary,
    onPrimary = OledOnPrimary,
    primaryContainer = OledPrimaryContainer,
    onPrimaryContainer = OledOnPrimaryContainer,
    secondary = AccentCyan,
    onSecondary = OledOnPrimary,
    background = OledBackground,
    onBackground = OledTextPrimary,
    surface = OledSurface,
    onSurface = OledTextPrimary,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledTextSecondary,
    outline = OledOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = AccentCyan,
    onSecondary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

@Composable
fun REDTheme(
    themeMode: ThemeMode = ThemeMode.DARK_NAVY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK_NAVY -> NavyColorScheme
        ThemeMode.OLED_BLACK -> OledColorScheme
        ThemeMode.LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
