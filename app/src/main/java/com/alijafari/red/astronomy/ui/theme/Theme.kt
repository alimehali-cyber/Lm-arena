package com.alijafari.red.astronomy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.ThemeMode

// Material 3 Color Schemes
private val SpaceColorScheme = darkColorScheme(
    primary = RedAccentVermilion,
    onPrimary = Color.White,
    primaryContainer = RedAccentSubtle,
    onPrimaryContainer = TextPrimary,
    secondary = RedCelestialGold,
    onSecondary = Color.White,
    tertiary = RedCelestialBlue,
    onTertiary = Color.White,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF171824),
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val OledSpaceColorScheme = darkColorScheme(
    primary = RedAccentVermilion,
    onPrimary = Color.White,
    primaryContainer = RedAccentSubtle,
    onPrimaryContainer = Color.White,
    secondary = RedCelestialGold,
    onSecondary = Color.White,
    tertiary = RedCelestialBlue,
    onTertiary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF0C0C10),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A)
)

private val LightSpaceColorScheme = lightColorScheme(
    primary = RedAccentLight,
    onPrimary = Color.White,
    primaryContainer = SoftCreamVariant,
    onPrimaryContainer = RedAccentLight,
    secondary = OldMoneyChampagneGold,
    onSecondary = Color.White,
    tertiary = RedCelestialBlue,
    onTertiary = Color.White,
    background = PureWhiteBackground,
    onBackground = OldMoneySlate,
    surface = SoftCreamSurface,
    onSurface = OldMoneySlate,
    surfaceVariant = Color(0xFFF3EFE6),
    onSurfaceVariant = OldMoneySlateMuted,
    outline = SoftCreamCardBorder
)

private val PapercraftPastelColorScheme = lightColorScheme(
    primary = PapercraftPrimary,
    onPrimary = Color.White,
    primaryContainer = PapercraftSurfaceVariant,
    onPrimaryContainer = PapercraftPrimary,
    secondary = PapercraftSecondary,
    onSecondary = PapercraftTextPrimary,
    tertiary = PapercraftTertiary,
    onTertiary = Color.White,
    background = PapercraftBackground,
    onBackground = PapercraftTextPrimary,
    surface = PapercraftSurface,
    onSurface = PapercraftTextPrimary,
    surfaceVariant = PapercraftSurfaceVariant,
    onSurfaceVariant = PapercraftTextSecondary,
    outline = PapercraftOutline
)

private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = c1.red + (c2.red - c1.red) * f,
        green = c1.green + (c2.green - c1.green) * f,
        blue = c1.blue + (c2.blue - c1.blue) * f,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * f
    )
}

fun createDynamicSkyColorScheme(sunAltitudeDeg: Double): androidx.compose.material3.ColorScheme {
    // 4 Key Lighting States
    val dayBg = Color(0xFF0F2B48)
    val daySurf = Color(0xFF16375B)
    val daySurfVar = Color(0xFF1E4571)
    val dayPrimary = Color(0xFF38BDF8)
    val daySecondary = Color(0xFF60A5FA)
    val dayText = Color(0xFFF8FAFC)

    val sunsetBg = Color(0xFF2E122D)
    val sunsetSurf = Color(0xFF3F193C)
    val sunsetSurfVar = Color(0xFF5B2252)
    val sunsetPrimary = Color(0xFFF97316)
    val sunsetSecondary = Color(0xFFFB923C)
    val sunsetText = Color(0xFFFFF7ED)

    val twilightBg = Color(0xFF0F172A)
    val twilightSurf = Color(0xFF1E293B)
    val twilightSurfVar = Color(0xFF334155)
    val twilightPrimary = Color(0xFF818CF8)
    val twilightSecondary = Color(0xFFA5B4FC)
    val twilightText = Color(0xFFF8FAFC)

    val nightBg = Color(0xFF0B0F19)
    val nightSurf = Color(0xFF111827)
    val nightSurfVar = Color(0xFF1F2937)
    val nightPrimary = RedAccentVermilion
    val nightSecondary = Color(0xFFC084FC)
    val nightText = Color(0xFFF8FAFC)

    val (bgTriple, accentTriple) = when {
        sunAltitudeDeg >= 6.0 -> {
            val f = ((sunAltitudeDeg - 6.0) / 12.0).toFloat().coerceIn(0f, 1f)
            Triple(
                lerpColor(dayBg, dayBg, f),
                lerpColor(daySurf, daySurf, f),
                lerpColor(daySurfVar, daySurfVar, f)
            ) to Triple(
                lerpColor(dayPrimary, dayPrimary, f),
                lerpColor(daySecondary, daySecondary, f),
                lerpColor(dayText, dayText, f)
            )
        }
        sunAltitudeDeg in 0.0..6.0 -> {
            val f = (sunAltitudeDeg / 6.0).toFloat()
            Triple(
                lerpColor(sunsetBg, dayBg, f),
                lerpColor(sunsetSurf, daySurf, f),
                lerpColor(sunsetSurfVar, daySurfVar, f)
            ) to Triple(
                lerpColor(sunsetPrimary, dayPrimary, f),
                lerpColor(sunsetSecondary, daySecondary, f),
                lerpColor(sunsetText, dayText, f)
            )
        }
        sunAltitudeDeg in -12.0..0.0 -> {
            val f = ((sunAltitudeDeg + 12.0) / 12.0).toFloat()
            Triple(
                lerpColor(twilightBg, sunsetBg, f),
                lerpColor(twilightSurf, sunsetSurf, f),
                lerpColor(twilightSurfVar, sunsetSurfVar, f)
            ) to Triple(
                lerpColor(twilightPrimary, sunsetPrimary, f),
                lerpColor(twilightSecondary, sunsetSecondary, f),
                lerpColor(twilightText, sunsetText, f)
            )
        }
        sunAltitudeDeg in -18.0..-12.0 -> {
            val f = ((sunAltitudeDeg + 18.0) / 6.0).toFloat()
            Triple(
                lerpColor(nightBg, twilightBg, f),
                lerpColor(nightSurf, twilightSurf, f),
                lerpColor(nightSurfVar, twilightSurfVar, f)
            ) to Triple(
                lerpColor(nightPrimary, twilightPrimary, f),
                lerpColor(nightSecondary, twilightSecondary, f),
                lerpColor(nightText, twilightText, f)
            )
        }
        else -> {
            Triple(nightBg, nightSurf, nightSurfVar) to Triple(nightPrimary, nightSecondary, nightText)
        }
    }

    val bg = bgTriple.first
    val surface = bgTriple.second
    val surfaceVar = bgTriple.third
    val primaryColor = accentTriple.first
    val secondaryColor = accentTriple.second
    val textColor = accentTriple.third

    return darkColorScheme(
        primary = primaryColor,
        onPrimary = textColor,
        primaryContainer = primaryColor.copy(alpha = 0.2f),
        onPrimaryContainer = textColor,
        secondary = secondaryColor,
        onSecondary = textColor,
        tertiary = AccentTertiary,
        onTertiary = textColor,
        background = bg,
        onBackground = textColor,
        surface = surface,
        onSurface = textColor,
        surfaceVariant = surfaceVar,
        onSurfaceVariant = TextSecondary,
        outline = primaryColor.copy(alpha = 0.4f)
    )
}

@Composable
fun REDTheme(
    themeMode: ThemeMode = ThemeMode.DARK_NAVY,
    sunAltitudeDeg: Double = -20.0,
    userLatitude: Double = 30.1141,
    userLongitude: Double = 51.5217,
    timestampMs: Long = System.currentTimeMillis(),
    content: @Composable () -> Unit
) {
    val celestialLighting = remember(timestampMs, userLatitude, userLongitude) {
        calculateCelestialLighting(timestampMs, userLatitude, userLongitude)
    }

    val colorScheme = when (themeMode) {
        ThemeMode.DARK_NAVY -> SpaceColorScheme
        ThemeMode.OLED_BLACK -> OledSpaceColorScheme
        ThemeMode.LIGHT -> LightSpaceColorScheme
        ThemeMode.PAPERCRAFT_PASTEL -> PapercraftPastelColorScheme
        ThemeMode.DYNAMIC_SKY -> createDynamicSkyColorScheme(sunAltitudeDeg)
        ThemeMode.DYNAMIC_SILK -> createDynamicSilkColorScheme(celestialLighting)
    }

    val redColorTokens = when (themeMode) {
        ThemeMode.DARK_NAVY -> RedDarkColorTokens
        ThemeMode.OLED_BLACK -> RedDarkColorTokens.copy(
            background = Color.Black,
            backgroundSecondary = Color(0xFF060608),
            backgroundTertiary = Color(0xFF0C0C10),
            surface = Color.Black,
            surfaceElevated = Color(0xFF0E0E12),
            surfaceGrouped = Color(0xFF14141A),
            surfaceVariant = Color(0xFF0A0A0E),
            border = Color(0xFF27272A),
            separator = Color(0xFF18181B)
        )
        ThemeMode.LIGHT -> RedLightColorTokens
        ThemeMode.PAPERCRAFT_PASTEL -> RedLightColorTokens.copy(
            background = PapercraftBackground,
            backgroundSecondary = PapercraftSurfaceVariant,
            backgroundTertiary = PapercraftSurface,
            surface = PapercraftSurface,
            surfaceElevated = PapercraftSurfaceVariant,
            surfaceGrouped = PapercraftSurfaceVariant,
            textPrimary = PapercraftTextPrimary,
            textSecondary = PapercraftTextSecondary,
            border = PapercraftOutline,
            separator = PapercraftOutline.copy(alpha = 0.5f)
        )
        ThemeMode.DYNAMIC_SKY -> if (sunAltitudeDeg >= 0.0) {
            RedLightColorTokens.copy(
                background = colorScheme.background,
                surface = colorScheme.surface,
                surfaceElevated = colorScheme.surfaceVariant,
                textPrimary = colorScheme.onSurface,
                textSecondary = colorScheme.onSurfaceVariant
            )
        } else {
            RedDarkColorTokens.copy(
                background = colorScheme.background,
                surface = colorScheme.surface,
                surfaceElevated = colorScheme.surfaceVariant,
                textPrimary = colorScheme.onSurface,
                textSecondary = colorScheme.onSurfaceVariant
            )
        }
        ThemeMode.DYNAMIC_SILK -> if (celestialLighting.isDark) {
            RedDarkColorTokens.copy(
                background = celestialLighting.backgroundColor,
                surface = celestialLighting.surfaceColor,
                surfaceElevated = celestialLighting.surfaceVariantColor,
                textPrimary = celestialLighting.textPrimaryColor,
                textSecondary = celestialLighting.textSecondaryColor,
                textTertiary = celestialLighting.textTertiaryColor,
                border = celestialLighting.outlineColor
            )
        } else {
            RedLightColorTokens.copy(
                background = celestialLighting.backgroundColor,
                surface = celestialLighting.surfaceColor,
                surfaceElevated = celestialLighting.surfaceVariantColor,
                textPrimary = celestialLighting.textPrimaryColor,
                textSecondary = celestialLighting.textSecondaryColor,
                textTertiary = celestialLighting.textTertiaryColor,
                border = celestialLighting.outlineColor
            )
        }
    }

    CompositionLocalProvider(
        LocalCelestialLighting provides celestialLighting,
        LocalRedColors provides redColorTokens,
        LocalRedSpacing provides RedSpacing,
        LocalRedRadius provides RedCornerRadius,
        LocalRedElevation provides RedElevation
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
