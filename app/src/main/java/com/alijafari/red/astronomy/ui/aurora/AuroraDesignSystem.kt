package com.alijafari.red.astronomy.ui.aurora

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawOutline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.LocalRedColors
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.squircleShape

// ============================================================================
// AURORA — LIQUID PRISM DESIGN SYSTEM
// ============================================================================
//
// The fourth interface language of the app, and the one built newest. Where
// SPATIAL frosts visionOS glass and CYBER cuts Nothing-OS crystal, AURORA treats
// every surface as a *refractive lens* floating in a living aurora: light bends
// through it, disperses at its rim, and pools in its belly.
//
// The system is written against the Astryx design specifications rather than
// taste. Each numbered rule below is a requirement that record states:
//
//  - Spatial hierarchy DR1-DR4 .... grouping by proximity, before containment;
//    gaps grow from local (6) to group (14) to section (24) to page (40).
//  - Shape relationships DR1-DR2 .. radius follows role, and nested curves stay
//    concentric: [AuroraRadius.inner] is always the outer radius minus the gap.
//  - Elevation hierarchy DR2-DR4 .. depth stays quiet; a surface leads with
//    either an edge or a shadow, never both at full strength; press and focus
//    rings are never written as elevation.
//  - Color emphasis DR1-DR5 ....... one primary emphasis per action group, and
//    every press tint composites over the surface it sits on instead of
//    replacing it.
//  - Typography hierarchy DR1-DR4 . seven distinguishable roles, adjacent steps
//    visibly different, theme personality never flattens the order.
//  - Motion DR1-DR5 ............... weight decides timing, easing settles
//    instead of bouncing, and every animated change has an immediate form.
//  - System states DR1-DR5 ........ status always pairs colour with an icon and
//    a label; busy placeholders hold the geometry they stand in for.
//  - User states DR1-DR3 .......... press paints the manipulated surface and is
//    transient; disabled never responds; a selected surface keeps its raise.
//
// Nothing here is allowed to be the only signal: colour, shape and motion each
// carry part of a meaning and never the whole of it.

// ----------------------------------------------------------------------------
// 1. SPECTRAL IDENTITY
// ----------------------------------------------------------------------------

/** Oxygen 557.7 nm — the body of an auroral arc. */
val AuroraGreen = Color(0xFF2BE8A5)
/** The low edge of the arc, where the green cools into the sea. */
val AuroraCyan = Color(0xFF35D6FF)
/** Nitrogen blue-violet — the top fringe of a tall arc. */
val AuroraViolet = Color(0xFF9B7BFF)
/** Oxygen 630 nm — the rare red crown that only strong storms reach. */
val AuroraRose = Color(0xFFFF6FA5)
/** The warm counterweight: sun, warning, and the horizon glow. */
val AuroraAmber = Color(0xFFFFC46B)

/** The full auroral spectrum, in the order light travels through it. */
val AURORA_SPECTRUM: List<Color> = listOf(AuroraCyan, AuroraGreen, AuroraViolet, AuroraRose)

// ----------------------------------------------------------------------------
// 2. GEOMETRY — concentric radii (shape relationships DR1-DR2)
// ----------------------------------------------------------------------------

object AuroraRadius {
    /** A page region or the floating navigation bar. */
    val container: Dp = 34.dp
    /** The one card radius: every tile, panel and list row. */
    val card: Dp = 26.dp
    /** Buttons, fields, segmented rails. */
    val control: Dp = 18.dp
    /** Chips, badges, and anything nested one step inside a control. */
    val chip: Dp = 12.dp
    /** Capsules and nav pills. */
    val pill: Dp = 999.dp

    /**
     * The radius of a surface nested inside one of radius [outer], separated by
     * [gap]. Subtracting the gap is what keeps the two curves parallel to the
     * eye, which is the whole of shape-relationships DR2.
     */
    fun inner(outer: Dp, gap: Dp = 10.dp): Dp {
        val value = outer.value - gap.value
        return (if (value < 4f) 4f else value).dp
    }
}

// ----------------------------------------------------------------------------
// 3. SPACE — four tiers, and nothing between them (spatial hierarchy DR2/DR4)
// ----------------------------------------------------------------------------

object AuroraSpace {
    /** A label to its value, an icon to its text. */
    val local: Dp = 6.dp
    /** Peer controls inside one group. */
    val group: Dp = 14.dp
    /** Distinct concerns: card to card, section to section. */
    val section: Dp = 24.dp
    /** The screen edge, and the gap between page regions. */
    val page: Dp = 40.dp

    /** The padding every screen holds back from the window edge. */
    val screenEdge: Dp = 20.dp
    /** The inner padding of a card, at the card's own grouping level. */
    val cardPadding: Dp = 18.dp
}

// ----------------------------------------------------------------------------
// 4. MOTION — weight decides timing (motion DR2)
// ----------------------------------------------------------------------------

object AuroraMotion {
    /** Press, toggle, a value snapping in. */
    const val MICRO_MS = 120
    /** A chip appearing, a section expanding, a rail sliding. */
    const val LOCAL_MS = 220
    /** A screen or a modal arriving. */
    const val ENTRANCE_MS = 420
    /** The aurora's own drift: one slow breath, never anything faster. */
    const val AMBIENT_MS = 26_000
    /** The specular highlight crossing a lens. */
    const val SPECULAR_MS = 9_000

    /** Controlled deceleration: no bounce, no elasticity (motion DR3). */
    val settle = FastOutSlowInEasing
    /** Ambient cycles are linear, or the drift visibly stutters at the turn. */
    val ambient = LinearEasing
}

// ----------------------------------------------------------------------------
// 5. TYPE — seven roles, each visibly a step from its neighbour
//    (typography hierarchy DR1-DR2)
// ----------------------------------------------------------------------------

object AuroraType {
    /** The one number or word a screen is about. */
    val display: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp
        )

    val title: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )

    /** Names a region. Must not be mistakable for body copy. */
    val section: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp
        )

    /** Sustained reading. */
    val body: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal
        )

    val label: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )

    val caption: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium
        )

    /**
     * Readings, coordinates and times. Tight tracking and tabular figures keep
     * a column of numbers from shimmering as the values change.
     */
    val data: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )

    /** The small caps eyebrow above a section: the quietest label there is. */
    val eyebrow: TextStyle
        @Composable @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = LocalAppFontFamily.current,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp
        )
}

// ----------------------------------------------------------------------------
// 6. PALETTE
// ----------------------------------------------------------------------------

/**
 * One mode of the Aurora language.
 *
 * Every colour is named for the job it does, not for the hue it happens to be,
 * so a theme can retune the whole personality without renaming anything.
 */
@Immutable
data class AuroraPalette(
    val themeMode: ThemeMode,
    val isDark: Boolean,
    val isWine: Boolean,

    // -- The field the surfaces float in ------------------------------------
    val canvasTop: Color,
    val canvasBottom: Color,

    // -- The aurora itself ---------------------------------------------------
    val ribbonPrimary: Color,
    val ribbonSecondary: Color,
    val ribbonTertiary: Color,
    val horizonGlow: Color,
    val starInk: Color,

    // -- The lens ------------------------------------------------------------
    /** Body fill, top of the gradient. */
    val lensTop: Color,
    /** Body fill, bottom of the gradient. */
    val lensBottom: Color,
    /** Light bending through the upper edge. */
    val refraction: Color,
    /** Light pooling along the lower inner edge. */
    val caustic: Color,
    /** The dispersion stroke that traces the perimeter. */
    val rim: Color,
    /** The rim on a raised or selected surface. */
    val rimStrong: Color,
    /** The travelling specular band. */
    val specular: Color,
    /** Bottom-inner shadow: the thickness of the glass. */
    val innerShadow: Color,
    val dropShadow: Color,

    // -- Ink -----------------------------------------------------------------
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,

    // -- Emphasis ------------------------------------------------------------
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    /** The spectral gradient, used on the single primary emphasis of a group. */
    val spectrum: List<Color>,

    // -- Status (system-states DR2: never colour alone) ----------------------
    val good: Color,
    val warn: Color,
    val bad: Color,
    val info: Color,

    // -- State ---------------------------------------------------------------
    /** Composited over the surface on press; never replaces it (DR5). */
    val pressedOverlay: Color,
    val selectedWash: Color,
    val skeletonBase: Color,
    val skeletonSheen: Color
)

/** Boreal night: the default, and the mode the language was drawn in. */
val DarkAuroraPalette = AuroraPalette(
    themeMode = ThemeMode.DARK,
    isDark = true,
    isWine = false,
    canvasTop = Color(0xFF04060F),
    canvasBottom = Color(0xFF080B1A),
    ribbonPrimary = AuroraGreen,
    ribbonSecondary = AuroraCyan,
    ribbonTertiary = AuroraViolet,
    horizonGlow = Color(0x331BE8A5),
    starInk = Color(0x66FFFFFF),
    lensTop = Color(0x1FDDE7FF),
    lensBottom = Color(0x1297B4E0),
    refraction = Color(0x33FFFFFF),
    caustic = Color(0x1E2BE8A5),
    rim = Color(0x2BFFFFFF),
    rimStrong = Color(0x66FFFFFF),
    specular = Color(0x1AFFFFFF),
    innerShadow = Color(0x2E000000),
    dropShadow = Color(0x40000000),
    textPrimary = Color(0xFFF2F7FF),
    textSecondary = Color(0xB8D6E4F5),
    textTertiary = Color(0x7FA9BDD4),
    textDisabled = Color(0x4DA9BDD4),
    accent = AuroraGreen,
    accentSoft = Color(0x2B2BE8A5),
    onAccent = Color(0xFF032018),
    spectrum = AURORA_SPECTRUM,
    good = Color(0xFF3DDBA0),
    warn = AuroraAmber,
    bad = Color(0xFFFF6B7A),
    info = AuroraCyan,
    pressedOverlay = Color(0x1FFFFFFF),
    selectedWash = Color(0x2B2BE8A5),
    skeletonBase = Color(0x14FFFFFF),
    skeletonSheen = Color(0x24FFFFFF)
)

/** Daylight aurora: the same optics over a pale, high-key field. */
val LightAuroraPalette = AuroraPalette(
    themeMode = ThemeMode.LIGHT,
    isDark = false,
    isWine = false,
    canvasTop = Color(0xFFF4F8FC),
    canvasBottom = Color(0xFFEAF1F9),
    ribbonPrimary = Color(0xFF10B981),
    ribbonSecondary = Color(0xFF0EA5E9),
    ribbonTertiary = Color(0xFF7C5CFF),
    horizonGlow = Color(0x2410B981),
    starInk = Color(0x1A0F2E5A),
    lensTop = Color(0xD9FFFFFF),
    lensBottom = Color(0xC2E8F3FF),
    refraction = Color(0xB3FFFFFF),
    caustic = Color(0x2610B981),
    rim = Color(0x73FFFFFF),
    rimStrong = Color(0xB3FFFFFF),
    specular = Color(0x59FFFFFF),
    innerShadow = Color(0x140F2E5A),
    dropShadow = Color(0x1F0F2E5A),
    textPrimary = Color(0xFF0B1B2E),
    textSecondary = Color(0xA31B3247),
    textTertiary = Color(0x7A334D66),
    textDisabled = Color(0x4D334D66),
    accent = Color(0xFF059669),
    accentSoft = Color(0x1F059669),
    onAccent = Color(0xFFFFFFFF),
    spectrum = listOf(Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFF7C5CFF), Color(0xFFEC4899)),
    good = Color(0xFF047857),
    warn = Color(0xFFB45309),
    bad = Color(0xFFBE123C),
    info = Color(0xFF0369A1),
    pressedOverlay = Color(0x1F0B1B2E),
    selectedWash = Color(0x24059669),
    skeletonBase = Color(0x140B1B2E),
    skeletonSheen = Color(0x240B1B2E)
)

/** Merlot aurora: the Wine theme, refracted. */
val WineAuroraPalette = AuroraPalette(
    themeMode = ThemeMode.WINE,
    isDark = true,
    isWine = true,
    canvasTop = Color(0xFF0B0207),
    canvasBottom = Color(0xFF16040D),
    ribbonPrimary = Color(0xFFC9306C),
    ribbonSecondary = AuroraRose,
    ribbonTertiary = Color(0xFF8E44AD),
    horizonGlow = Color(0x33C9306C),
    starInk = Color(0x59FFE3EA),
    lensTop = Color(0x21FFD6E0),
    lensBottom = Color(0x14C9789C),
    refraction = Color(0x33FFE9F0),
    caustic = Color(0x22E84265),
    rim = Color(0x33FF9EB5),
    rimStrong = Color(0x66FFC2CE),
    specular = Color(0x1FFFF9FB),
    innerShadow = Color(0x38000000),
    dropShadow = Color(0x4D000000),
    textPrimary = Color(0xFFFFF2F6),
    textSecondary = Color(0xC2F2CFDA),
    textTertiary = Color(0x8AD1A3B5),
    textDisabled = Color(0x59D1A3B5),
    accent = Color(0xFFF2507F),
    accentSoft = Color(0x33E84265),
    onAccent = Color(0xFF28000E),
    spectrum = listOf(Color(0xFFFF8FB0), Color(0xFFC9306C), Color(0xFF8E44AD), Color(0xFFE4A672)),
    good = Color(0xFF4ADE80),
    warn = Color(0xFFE4A672),
    bad = Color(0xFFFF5C77),
    info = Color(0xFFE87A98),
    pressedOverlay = Color(0x24FFD6E0),
    selectedWash = Color(0x33E84265),
    skeletonBase = Color(0x1AFFD6E0),
    skeletonSheen = Color(0x2BFFD6E0)
)

val LocalAuroraPalette = staticCompositionLocalOf { DarkAuroraPalette }

object AuroraTheme {
    val palette: AuroraPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAuroraPalette.current
}

fun auroraPaletteFor(themeMode: ThemeMode): AuroraPalette = when (themeMode) {
    ThemeMode.LIGHT -> LightAuroraPalette
    ThemeMode.DARK -> DarkAuroraPalette
    ThemeMode.WINE -> WineAuroraPalette
}

/**
 * Hands the Aurora palette to everything below it, and retunes the app's own
 * [RedTheme.colors] so the shared dialogs, detail modals and pickers resolve
 * their ink against Aurora surfaces instead of fighting them.
 *
 * This is the same contract [com.alijafari.red.astronomy.ui.cyber.CyberThemeProvider]
 * and the Spatial provider honour: a UI style is a *token* swap first and a set
 * of bespoke screens second, so any screen nobody has redrawn yet still reads
 * as part of the style rather than as a foreign object dropped into it.
 */
@Composable
fun AuroraThemeProvider(
    enabled: Boolean,
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val palette = remember(themeMode) { auroraPaletteFor(themeMode) }
    val baseRedColors = RedTheme.colors
    val adaptedRedColors = remember(baseRedColors, themeMode) {
        when (themeMode) {
            ThemeMode.WINE -> baseRedColors.copy(
                background = Color(0xFF0B0207),
                backgroundSecondary = Color(0xFF16040D),
                backgroundTertiary = Color(0xFF210613),
                surface = Color(0xEB170410),
                surfaceElevated = Color(0xF0210817),
                surfaceGrouped = Color(0xE01C0611),
                surfaceVariant = Color(0xCC330D1B),
                border = Color(0x38FF9EB5),
                separator = Color(0x26FF9EB5),
                accentRed = Color(0xFFE84265),
                accentRedSubtle = Color(0x33B82245),
                accentGold = Color(0xFFE4A672),
                accentBlue = Color(0xFFF2507F),
                accentPurple = Color(0xFF8E44AD),
                navSurface = Color(0xE618050E),
                navBorder = Color(0x45FF9EB5)
            )
            ThemeMode.DARK -> baseRedColors.copy(
                background = Color(0xFF04060F),
                backgroundSecondary = Color(0xFF0B1020),
                backgroundTertiary = Color(0xFF141B2E),
                surface = Color(0xEB0C1322),
                surfaceElevated = Color(0xF0131A2B),
                surfaceGrouped = Color(0xE0101725),
                surfaceVariant = Color(0xCC1B2436),
                border = Color(0x2BFFFFFF),
                separator = Color(0x1FFFFFFF),
                accentRed = Color(0xFFFF6B7A),
                accentRedSubtle = Color(0x2B2BE8A5),
                accentGold = AuroraAmber,
                accentBlue = AuroraCyan,
                accentPurple = AuroraViolet,
                navSurface = Color(0xE00B1120),
                navBorder = Color(0x33FFFFFF)
            )
            ThemeMode.LIGHT -> baseRedColors.copy(
                background = Color(0xFFF4F8FC),
                backgroundSecondary = Color(0xFFFBFDFE),
                backgroundTertiary = Color(0xFFFFFFFF),
                surface = Color(0xF2FFFFFF),
                surfaceElevated = Color(0xF8FFFFFF),
                surfaceGrouped = Color(0xEDF0F6FC),
                surfaceVariant = Color(0xFFE5EBF4),
                border = Color(0x1F0B1B2E),
                separator = Color(0x140B1B2E),
                accentRed = Color(0xFFBE123C),
                accentRedSubtle = Color(0x1F059669),
                accentGold = Color(0xFFB45309),
                accentBlue = Color(0xFF0369A1),
                accentPurple = Color(0xFF7C5CFF),
                navSurface = Color(0xEBFFFFFF),
                navBorder = Color(0x80FFFFFF)
            )
        }
    }

    CompositionLocalProvider(
        LocalAuroraPalette provides palette,
        LocalRedColors provides adaptedRedColors,
        content = content
    )
}

// ----------------------------------------------------------------------------
// 7. MOTION ACCESS
// ----------------------------------------------------------------------------

/**
 * Whether the device has asked for less animation.
 *
 * Motion DR5 is unconditional: every transition Aurora animates has an
 * immediate form, and this is how the system finds out which form to use.
 */
@Composable
fun rememberAuroraReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (t: Throwable) {
            false
        }
    }
}

/**
 * The aurora's own clock: one linear [AuroraMotion.AMBIENT_MS] cycle shared by
 * every drifting surface on screen, so the whole field breathes together
 * instead of each element keeping its own time.
 *
 * Under reduced motion the clock stops at a fixed, composed phase — the field
 * is still drawn, it simply no longer travels (motion DR1, DR5).
 */
@Composable
fun rememberAuroraPhase(): Float {
    val reduceMotion = rememberAuroraReducedMotion()
    val transition = rememberInfiniteTransition(label = "AuroraFieldPhase")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AuroraMotion.AMBIENT_MS, easing = AuroraMotion.ambient),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraFieldPhaseValue"
    )
    return if (reduceMotion) 0.34f else phase.value
}

// ----------------------------------------------------------------------------
// 8. THE LENS
// ----------------------------------------------------------------------------

/**
 * How far a surface sits above the field.
 *
 * The tier chooses the fill, the rim and the shadow as one decision, because
 * elevation-hierarchy DR3 says a surface leads with either an edge or a
 * shadow — never both at full strength.
 */
enum class AuroraSurfaceTier {
    /** No paint at all: pure layout. */
    CANVAS,
    /** Recessed into the page: a viewport, a well, a captured frame. */
    INSET,
    /** The default card. */
    CARD,
    /** Floats above the content: the navigation bar, a sheet. */
    RAISED,
    /** Above everything transient: modals and dialogs. */
    OVERLAY
}

private data class AuroraLensPaint(
    val top: Color,
    val bottom: Color,
    val rimColor: Color,
    val rimWidth: Dp,
    val shadow: Dp,
    val specularAlpha: Float,
    val causticAlpha: Float
)

private fun auroraLensPaint(
    palette: AuroraPalette,
    tier: AuroraSurfaceTier,
    selected: Boolean
): AuroraLensPaint = when (tier) {
    AuroraSurfaceTier.CANVAS -> AuroraLensPaint(
        top = Color.Transparent,
        bottom = Color.Transparent,
        rimColor = Color.Transparent,
        rimWidth = 0.dp,
        shadow = 0.dp,
        specularAlpha = 0f,
        causticAlpha = 0f
    )
    AuroraSurfaceTier.INSET -> AuroraLensPaint(
        top = palette.innerShadow,
        bottom = palette.innerShadow.copy(alpha = palette.innerShadow.alpha * 0.35f),
        rimColor = palette.rim.copy(alpha = palette.rim.alpha * 0.7f),
        rimWidth = 1.dp,
        shadow = 0.dp,
        specularAlpha = 0.25f,
        causticAlpha = 0f
    )
    AuroraSurfaceTier.CARD -> AuroraLensPaint(
        top = palette.lensTop,
        bottom = palette.lensBottom,
        rimColor = palette.rim,
        rimWidth = 1.2.dp,
        shadow = 2.dp,
        specularAlpha = 1f,
        causticAlpha = 1f
    )
    AuroraSurfaceTier.RAISED -> AuroraLensPaint(
        top = palette.lensTop.copy(alpha = (palette.lensTop.alpha * 1.55f).coerceAtMost(1f)),
        bottom = palette.lensBottom.copy(alpha = (palette.lensBottom.alpha * 1.5f).coerceAtMost(1f)),
        rimColor = if (selected) palette.rimStrong else palette.rim,
        rimWidth = 1.4.dp,
        shadow = 10.dp,
        specularAlpha = 1.25f,
        causticAlpha = 1.3f
    )
    AuroraSurfaceTier.OVERLAY -> AuroraLensPaint(
        top = palette.lensTop.copy(alpha = (palette.lensTop.alpha * 1.9f).coerceAtMost(1f)),
        bottom = palette.lensBottom.copy(alpha = (palette.lensBottom.alpha * 1.8f).coerceAtMost(1f)),
        rimColor = palette.rimStrong,
        rimWidth = 1.4.dp,
        shadow = 22.dp,
        specularAlpha = 1.4f,
        causticAlpha = 1.5f
    )
}

/**
 * The one surface every Aurora screen is built from: a refractive lens, not a
 * frosted panel.
 *
 * Drawn in five passes, each doing one job:
 *  1. the body gradient — the glass itself;
 *  2. the top refraction — light bending as it enters;
 *  3. the belly caustic — light pooling as it leaves;
 *  4. the dispersion rim — the prism edge, spectral on emphasis;
 *  5. the travelling specular — the proof that it is glass and not paper.
 *
 * Press composites [AuroraPalette.pressedOverlay] over all of it rather than
 * swapping the fill, so the surface keeps its identity while it is being
 * touched (color-emphasis DR5), and a disabled surface paints none of it
 * (user-states DR2).
 */
@Composable
fun AuroraLiquidSurface(
    modifier: Modifier = Modifier,
    shape: Shape = squircleShape(AuroraRadius.card),
    tier: AuroraSurfaceTier = AuroraSurfaceTier.CARD,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = AuroraTheme.palette
    val reduceMotion = rememberAuroraReducedMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val interactive = onClick != null && enabled

    // The specular crosses the lens on its own slow clock, and a press snaps it
    // to the touch so the response is immediate rather than whenever the cycle
    // happens to arrive (motion DR2: local feedback is the fastest thing there is).
    val transition = rememberInfiniteTransition(label = "AuroraSpecular")
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AuroraMotion.SPECULAR_MS, easing = AuroraMotion.ambient),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuroraSpecularSweep"
    )
    val sweepPosition = if (reduceMotion) 0.42f else sweep.value

    val pressAmount by animateFloatAsState(
        targetValue = if (interactive && pressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else AuroraMotion.MICRO_MS),
        label = "AuroraLensPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (interactive && pressed) 0.978f else 1f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else AuroraMotion.MICRO_MS,
            easing = AuroraMotion.settle
        ),
        label = "AuroraLensPressScale"
    )

    val paint = auroraLensPaint(palette, tier, selected)
    val dimAlpha = if (enabled) 1f else 0.42f

    Box(
        modifier = modifier
            .then(
                if (interactive) {
                    Modifier.scale(pressScale)
                } else {
                    Modifier
                }
            )
            .then(
                if (paint.shadow > 0.dp) {
                    Modifier.shadow(
                        elevation = paint.shadow,
                        shape = shape,
                        clip = false,
                        ambientColor = palette.dropShadow,
                        spotColor = palette.dropShadow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .drawBehind {
                drawAuroraLens(
                    palette = palette,
                    shape = shape,
                    paint = paint,
                    selected = selected,
                    sweepPosition = sweepPosition,
                    pressAmount = pressAmount,
                    dimAlpha = dimAlpha
                )
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * One lens, painted. Kept out of the composable so the five passes read as the
 * list they are rather than as an indentation.
 */
private fun DrawScope.drawAuroraLens(
    palette: AuroraPalette,
    shape: Shape,
    paint: AuroraLensPaint,
    selected: Boolean,
    sweepPosition: Float,
    pressAmount: Float,
    dimAlpha: Float
) {
    val outline = shape.createOutline(size, layoutDirection, this)
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    // 1. The glass itself.
    if (paint.top != Color.Transparent) {
        drawOutline(
            outline = outline,
            brush = Brush.linearGradient(
                colors = listOf(paint.top, paint.bottom),
                start = Offset.Zero,
                end = Offset(0f, h)
            )
        )
    }

    // 2. Refraction: light entering across the upper edge.
    if (paint.specularAlpha > 0f) {
        drawOutline(
            outline = outline,
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.refraction.copy(alpha = palette.refraction.alpha * paint.specularAlpha),
                    Color.Transparent
                ),
                center = Offset(w * 0.24f, -h * 0.12f),
                radius = h * 0.95f
            )
        )
    }

    // 3. Caustic: light pooling along the lower inner edge.
    if (paint.causticAlpha > 0f) {
        drawOutline(
            outline = outline,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    palette.caustic.copy(alpha = palette.caustic.alpha * paint.causticAlpha)
                ),
                center = Offset(w * 0.5f, h * 1.02f),
                radius = h * 0.8f
            )
        )
    }

    // 4. The dispersion rim. Spectral on the one emphasised surface of a group,
    //    a plain highlight everywhere else (color-emphasis DR3).
    if (paint.rimWidth > 0.dp) {
        val rimBrush = if (selected) {
            Brush.linearGradient(
                colors = palette.spectrum.map { it.copy(alpha = 0.85f) },
                start = Offset.Zero,
                end = Offset(w, h)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    paint.rimColor,
                    paint.rimColor.copy(alpha = paint.rimColor.alpha * 0.35f),
                    paint.rimColor.copy(alpha = paint.rimColor.alpha * 0.8f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w * 0.35f, h)
            )
        }
        drawOutline(
            outline = outline,
            brush = rimBrush,
            style = Stroke(width = paint.rimWidth.toPx())
        )
    }

    // 5. The travelling specular: the band that says "glass".
    if (paint.specularAlpha > 0f) {
        val bandWidth = w * 0.52f
        val x = -bandWidth + (w + bandWidth) * sweepPosition
        drawOutline(
            outline = outline,
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, palette.specular, Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + bandWidth, h * 0.92f)
            )
        )
    }

    // Selected surfaces keep their raise for the whole gesture (user-states DR3),
    // so the wash sits *under* the press rather than fighting it.
    if (selected) {
        drawOutline(outline = outline, color = palette.selectedWash)
    }

    // Press composites over the lens: it never replaces it.
    if (pressAmount > 0f) {
        drawOutline(
            outline = outline,
            color = palette.pressedOverlay.copy(alpha = palette.pressedOverlay.alpha * pressAmount)
        )
    }

    // Disabled dims the paint rather than the content, so text keeps its own
    // contrast ratio against the surface it sits on instead of inheriting a
    // blanket alpha it was never measured against.
    if (dimAlpha < 1f) {
        drawOutline(
            outline = outline,
            color = palette.canvasBottom.copy(alpha = (1f - dimAlpha) * 0.35f)
        )
    }
}

// ----------------------------------------------------------------------------
// 9. PRIMITIVES BUILT ON THE LENS
// ----------------------------------------------------------------------------

/** A page region: no paint, just the grouping the field provides. */
@Composable
fun AuroraRegion(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AuroraLiquidSurface(
        modifier = modifier,
        tier = AuroraSurfaceTier.CANVAS,
        content = content
    )
}

/** A card. The default surface, and the one almost every tile uses. */
@Composable
fun AuroraCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = AuroraSpace.cardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    AuroraLiquidSurface(
        modifier = modifier,
        shape = squircleShape(AuroraRadius.card),
        tier = AuroraSurfaceTier.CARD,
        selected = selected,
        enabled = enabled,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(
                start = contentPadding,
                end = contentPadding,
                top = contentPadding,
                bottom = contentPadding
            ),
            content = content
        )
    }
}

/** A recessed well: a captured frame, a viewport, a readout the field sits behind. */
@Composable
fun AuroraInset(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AuroraLiquidSurface(
        modifier = modifier,
        shape = squircleShape(AuroraRadius.card),
        tier = AuroraSurfaceTier.INSET,
        content = content
    )
}
