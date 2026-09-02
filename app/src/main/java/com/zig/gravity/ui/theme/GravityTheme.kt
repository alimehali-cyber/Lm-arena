package com.zig.gravity.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * §3.9 visual direction — "a precision instrument on a desk", never space.
 *
 * Typography is deliberately absent: the app's existing default font is used everywhere and no
 * font resource, dependency or typography override is introduced (locked decision 11).
 */
@Immutable
data class GravityColors(
    val tableTop: Color,
    val tableBottom: Color,
    val vignette: Color,
    val grain: Color,
    val accent: Color,
    val onSurface: Color,
    val onSurfaceDim: Color,
    val chrome: Color,
    val chromeBorder: Color,
    val trail: Color,
    val prediction: Color,
    val velocity: Color,
    val acceleration: Color,
    val barycenter: Color,
    val shadow: Color,
    val selection: Color,
    val blackHoleBody: Color,
    val blackHoleRing: Color,
    val wormholeWarm: Color,
    val wormholeCool: Color,
    val isDark: Boolean
) {
    /** §3.9: light theme uses the same palette, slightly deepened. */
    fun bodyTone(argb: Long): Color {
        val base = Color(argb)
        return if (isDark) base else lerp(base, Color.Black, 0.18f)
    }

    fun highlightOf(base: Color): Color = lerp(base, Color.White, if (isDark) 0.42f else 0.34f)
    fun shadeOf(base: Color): Color = lerp(base, Color.Black, if (isDark) 0.55f else 0.40f)
}

// §21 — this is a TABLETOP, not space. The dark theme used to sit at ~12% lightness, which on an
// OLED panel is indistinguishable from a black sky and made the sandbox read as an astronomy
// renderer. It is now a neutral slate felt at ~24% lightness: still calm and low-contrast, still
// dark-theme comfortable, but unmistakably a surface with objects resting on it. The vignette was
// softened to match, so the corners no longer fall back to black.
val DarkTabletop = GravityColors(
    tableTop = Color(0xFF3A414B),
    tableBottom = Color(0xFF2E343D),
    vignette = Color(0x3D000000),
    grain = Color(0x0AFFFFFF),
    accent = Color(0xFFD4A853),
    onSurface = Color(0xFFE9E5DC),
    onSurfaceDim = Color(0xFF9A968E),
    chrome = Color(0xCC232830),
    chromeBorder = Color(0x33FFFFFF),
    trail = Color(0x66FFFFFF),
    prediction = Color(0xCCD4A853),
    velocity = Color(0xFF8FC7D8),
    acceleration = Color(0xFFD98F6E),
    barycenter = Color(0xFFE0C88A),
    shadow = Color(0x59000000),
    selection = Color(0xFFD4A853),
    blackHoleBody = Color(0xFF0A0A0C),
    blackHoleRing = Color(0xFFD4A853),
    wormholeWarm = Color(0xFFD4A853),
    wormholeCool = Color(0xFF6FA3B0),
    isDark = true
)

val LightTabletop = GravityColors(
    tableTop = Color(0xFFF4F1EA),
    tableBottom = Color(0xFFE9E4D9),
    vignette = Color(0x1A5B5344),
    grain = Color(0x0A000000),
    accent = Color(0xFF2F6B63),
    onSurface = Color(0xFF23262B),
    onSurfaceDim = Color(0xFF6B6862),
    chrome = Color(0xE6FBF9F4),
    chromeBorder = Color(0x1A000000),
    trail = Color(0x4D2B2A27),
    prediction = Color(0xCC2F6B63),
    velocity = Color(0xFF2E6E86),
    acceleration = Color(0xFFB05A38),
    barycenter = Color(0xFF7A6326),
    shadow = Color(0x33000000),
    selection = Color(0xFF2F6B63),
    blackHoleBody = Color(0xFF26262B),
    blackHoleRing = Color(0xFF2F6B63),
    wormholeWarm = Color(0xFFB07C24),
    wormholeCool = Color(0xFF2F6B63),
    isDark = false
)

val LocalGravityColors: ProvidableCompositionLocal<GravityColors> =
    staticCompositionLocalOf { DarkTabletop }

@Composable
fun ZigGravityTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGravityColors provides if (dark) DarkTabletop else LightTabletop,
        content = content
    )
}
