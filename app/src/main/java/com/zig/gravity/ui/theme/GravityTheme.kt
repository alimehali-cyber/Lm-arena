package com.zig.gravity.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual direction — "a precision instrument on a desk".
 *
 * The desk now has seven designed material finishes ([TableSurfaces]) instead of a binary
 * dark/light theme. A star print on a surface is a *printed fabric*: static dots on material, with
 * bodies still sitting on it and casting shadows on it. It is never a void, never animated, and
 * never a sky.
 *
 * Typography is deliberately absent: the app's existing default font is used everywhere and no
 * font resource, dependency or typography override is introduced (locked decision 11).
 */

/**
 * §4 — the chrome token set, as raw ARGB literals.
 *
 * One accent per chrome mode; the retired brass accent no longer appears anywhere in the tree.
 * Every piece of chrome (HUD, sheets, cards, dialogs, rings, selection, previews, pulses) reads
 * these through [GravityColors], so the accent stays a single change point.
 */
object GravityChrome {
    const val ACCENT_DARK = 0xFF2DD4BFL
    const val ACCENT_LIGHT = 0xFF0E9F8FL

    const val ON_SURFACE_DARK = 0xFFF1F4F7L
    const val ON_SURFACE_LIGHT = 0xFF23272EL
    const val ON_SURFACE_VARIANT_DARK = 0xFFABB6C2L
    const val ON_SURFACE_VARIANT_LIGHT = 0xFF5D6672L

    const val GLASS_CONTAINER_DARK = 0xB3161A22L
    const val GLASS_CONTAINER_LIGHT = 0xA6FFFFFFL
    const val GLASS_STROKE_DARK = 0x0FFFFFFFL
    const val GLASS_STROKE_LIGHT = 0x1425282EL

    /** §2 — the black-hole disk keeps its dual value: near-black on dark, dark graphite on light. */
    const val BLACK_HOLE_DISK_DARK = 0xFF0A0A0CL
    const val BLACK_HOLE_DISK_LIGHT = 0xFF26262BL

    /** §3 — two-layer cast shadow, light from the top-left so the shadow falls bottom-right. */
    const val SHADOW_UMBRA_ALPHA_DARK = 0.32f
    const val SHADOW_UMBRA_ALPHA_LIGHT = 0.26f
    const val SHADOW_PENUMBRA_ALPHA_DARK = 0.20f
    const val SHADOW_PENUMBRA_ALPHA_LIGHT = 0.16f

    /** Trail hue; the alpha comes from the surface's own trail pair. */
    const val TRAIL_HUE_DARK = 0xFFFFFFFFL
    const val TRAIL_HUE_LIGHT = 0xFF2B2A27L

    /** Diagnostic tints — unchanged by this refresh, and deliberately distinct from the accent. */
    const val VELOCITY_DARK = 0xFF8FC7D8L
    const val VELOCITY_LIGHT = 0xFF2E6E86L
    const val ACCELERATION_DARK = 0xFFD98F6EL
    const val ACCELERATION_LIGHT = 0xFFB05A38L
    const val BARYCENTER_DARK = 0xFFE0C88AL
    const val BARYCENTER_LIGHT = 0xFF7A6326L
    const val WORMHOLE_COOL_DARK = 0xFF6FA3B0L
    const val WORMHOLE_COOL_LIGHT = 0xFF5B7C99L

    /** The prediction path is the accent at 80%, exactly as the brass one used to be. */
    const val PREDICTION_ALPHA = 0.8f
}

@Immutable
data class GravityColors(
    /** The surface this palette was built from: gradient, print, vignette and trail alphas. */
    val surface: GravitySurface,
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
    val trailOld: Color,
    val prediction: Color,
    val velocity: Color,
    val acceleration: Color,
    val barycenter: Color,
    val shadow: Color,
    val shadowSoft: Color,
    val selection: Color,
    val blackHoleBody: Color,
    val blackHoleRing: Color,
    val wormholeWarm: Color,
    val wormholeCool: Color,
    val isDark: Boolean
) {
    /** Light chrome deepens every body tone slightly, so marbles keep their weight on paper. */
    fun bodyTone(argb: Long): Color {
        val base = Color(argb)
        return if (isDark) base else lerp(base, Color.Black, 0.18f)
    }

    fun highlightOf(base: Color): Color = lerp(base, Color.White, if (isDark) 0.42f else 0.34f)
    fun shadeOf(base: Color): Color = lerp(base, Color.Black, if (isDark) 0.55f else 0.40f)
}

/** Builds the chrome palette for one surface. Called once per surface change, never per frame. */
fun colorsFor(surface: GravitySurface): GravityColors {
    val dark = surface.isDark
    val accentArgb = if (dark) GravityChrome.ACCENT_DARK else GravityChrome.ACCENT_LIGHT
    val trailHue = if (dark) GravityChrome.TRAIL_HUE_DARK else GravityChrome.TRAIL_HUE_LIGHT
    val accent = Color(accentArgb)
    return GravityColors(
        surface = surface,
        tableTop = Color(surface.gradient.startArgb),
        tableBottom = Color(surface.gradient.endArgb),
        vignette = Color(surface.vignetteArgb),
        grain = Color(surface.sheenArgb),
        accent = accent,
        onSurface = Color(if (dark) GravityChrome.ON_SURFACE_DARK else GravityChrome.ON_SURFACE_LIGHT),
        onSurfaceDim = Color(
            if (dark) GravityChrome.ON_SURFACE_VARIANT_DARK else GravityChrome.ON_SURFACE_VARIANT_LIGHT
        ),
        chrome = Color(if (dark) GravityChrome.GLASS_CONTAINER_DARK else GravityChrome.GLASS_CONTAINER_LIGHT),
        chromeBorder = Color(if (dark) GravityChrome.GLASS_STROKE_DARK else GravityChrome.GLASS_STROKE_LIGHT),
        trail = Color(trailHue).copy(alpha = surface.trailAlphaNew),
        trailOld = Color(trailHue).copy(alpha = surface.trailAlphaOld),
        prediction = accent.copy(alpha = GravityChrome.PREDICTION_ALPHA),
        velocity = Color(if (dark) GravityChrome.VELOCITY_DARK else GravityChrome.VELOCITY_LIGHT),
        acceleration = Color(if (dark) GravityChrome.ACCELERATION_DARK else GravityChrome.ACCELERATION_LIGHT),
        barycenter = Color(if (dark) GravityChrome.BARYCENTER_DARK else GravityChrome.BARYCENTER_LIGHT),
        shadow = Color.Black.copy(
            alpha = if (dark) GravityChrome.SHADOW_UMBRA_ALPHA_DARK else GravityChrome.SHADOW_UMBRA_ALPHA_LIGHT
        ),
        shadowSoft = Color.Black.copy(
            alpha = if (dark) GravityChrome.SHADOW_PENUMBRA_ALPHA_DARK else GravityChrome.SHADOW_PENUMBRA_ALPHA_LIGHT
        ),
        selection = accent,
        blackHoleBody = Color(if (dark) GravityChrome.BLACK_HOLE_DISK_DARK else GravityChrome.BLACK_HOLE_DISK_LIGHT),
        blackHoleRing = accent,
        wormholeWarm = accent,
        wormholeCool = Color(if (dark) GravityChrome.WORMHOLE_COOL_DARK else GravityChrome.WORMHOLE_COOL_LIGHT),
        isDark = dark
    )
}

/** The new default surface, pre-built so the composition local has a sane default. */
val MidnightTabletop: GravityColors = colorsFor(TableSurfaces.MIDNIGHT)

/** The two pre-refresh themes, kept addressable: they live on as the charcoal and paper surfaces. */
val DarkTabletop: GravityColors = colorsFor(TableSurfaces.CHARCOAL)
val LightTabletop: GravityColors = colorsFor(TableSurfaces.PAPER)

val LocalGravityColors: ProvidableCompositionLocal<GravityColors> =
    staticCompositionLocalOf { MidnightTabletop }

/**
 * Builds the surface's background brush for any canvas size — the full table or a 64 dp swatch in
 * the table-colour sheet. One implementation, so a swatch can never disagree with the table.
 */
fun SurfaceGradient.brushFor(size: Size): Brush {
    val start = Color(startArgb)
    val end = Color(endArgb)
    return if (type == SurfaceGradientType.RADIAL) {
        Brush.radialGradient(
            colors = listOf(start, end),
            center = Offset(size.width * centerX, size.height * centerY),
            radius = (size.maxDimension * radiusFraction).coerceAtLeast(1f)
        )
    } else {
        // CSS convention: 0deg = to top, 90deg = to right, 180deg = to bottom.
        val rad = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val half = (abs(dx) * size.width + abs(dy) * size.height) * 0.5f
        Brush.linearGradient(
            colors = listOf(start, end),
            start = Offset(cx - dx * half, cy - dy * half),
            end = Offset(cx + dx * half, cy + dy * half)
        )
    }
}

@Composable
fun ZigGravityTheme(surfaceKey: String, content: @Composable () -> Unit) {
    val surface = remember(surfaceKey) { TableSurfaces.resolve(surfaceKey) }
    val colors = remember(surface) { colorsFor(surface) }
    CompositionLocalProvider(
        LocalGravityColors provides colors,
        content = content
    )
}
