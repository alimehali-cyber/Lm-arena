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
    primary = OldMoneyBurgundy,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = SoftCreamVariant,
    onPrimaryContainer = OldMoneyBurgundy,
    secondary = OldMoneyChampagneGold,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = OldMoneyNavy,
    onTertiary = Color(0xFFFFFFFF),
    background = PureWhiteBackground,
    onBackground = OldMoneySlate,
    surface = SoftCreamSurface,
    onSurface = OldMoneySlate,
    surfaceVariant = Color(0xFFF3EFE6),
    onSurfaceVariant = OldMoneySlateMuted,
    outline = SoftCreamCardBorder
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

