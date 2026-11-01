package com.alijafari.red.astronomy.ui.theme

/**
 * ============================================================================
 * LIQUID GLASS PLACEMENT RULES
 * ============================================================================
 *
 * 1. WHERE DOES A COMPONENT BELONG?
 *    - Inside Crossfade (Tabs / Screens / Content Cards) — since Part 6:
 *      Cards *do* render real glass. What they must never do is sample a backdrop that is
 *      currently recording their own subtree: `drawBackdrop()` inside a `layerBackdrop()` of the
 *      same instance throws the fatal Kyant assertion "Can not perform this action inside of
 *      drawBackdrop!". MainActivity therefore records **two** layers: the app backdrop (theme
 *      background + live sky + photograph), which is what content cards sample through
 *      `LocalLiquidGlassBackdrop`, and the content layer, which only the root-level overlays
 *      (`FloatingBottomBar`, dialogs) sample because they sit outside the content tree. A card that
 *      samples the backdrop layer is reading a sibling that was already drawn, never itself.
 *    - Root-Level Sibling Overlays (Floating Bars, Modals, Full-Screen Dialogs):
 *      Components that should render real Liquid Glass optical effects (sampling the
 *      live, animated screen content beneath them) MUST sit OUTSIDE the Crossfade tree
 *      as direct siblings in `MainActivity.kt`'s root Box (e.g., `FloatingBottomBar`,
 *      `LocationSelectorDialog`).
 *
 *    - Every `LiquidGlassSurface` ALSO scopes its own content to
 *      `LocalLiquidGlassBackdrop provides null`. A card nested inside a card therefore renders the
 *      fallback material instead of opening a second sampling pass on the same backdrop. Nesting is
 *      what the assertion below is about, and it is now impossible to trigger by accident.
 *
 * 2. EXPLICIT PARAMETER PASSING MANDATE:
 *    - Always pass the root `backdrop: Backdrop?` as an EXPLICIT parameter down into
 *      the overlay composable and forward it to every internal `LiquidGlassSurface(backdrop = backdrop, ...)`.
 *    - NEVER rely on `LocalLiquidGlassBackdrop.current` for components intended to render
 *      real glass, since ambient composition local resolution is intentionally null-scoped
 *      inside the main content tree.
 *
 * 3. CONFIGURATION & CUSTOMIZATION (LocalLiquidGlassConfig / LocalLiquidGlassEnabled):
 *    - `LocalLiquidGlassConfig` (blur, refraction depth/warping, chromatic aberration,
 *      clarity) and `LocalLiquidGlassEnabled` are provided at the root and are NOT scoped
 *      to null anywhere.
 *    - All `LiquidGlassSurface` composables automatically pick up Settings customizations
 *      and gracefully adapt without extra wiring.
 * ============================================================================
 */

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.roundToInt

val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * How much of the theme's own surface a real-glass card lays under its content so the text on it keeps
 * its contrast over an unknown backdrop. Low enough to stay glass, high enough to hold the ink.
 */
private const val LEGIBILITY_PLATE_ALPHA = 0.34f

/**
 * User-configurable Liquid Glass optical characteristics.
 * Persisted in SharedPreferences and dynamically applied across all liquid glass surfaces.
 */
@Immutable
data class LiquidGlassConfig(
    val enabled: Boolean = true,
    val clarity: Float = 1.0f, // 0.0 (frosted) to 1.0 (crystal pure)
    val blurRadiusDp: Float = 0f, // 0 to 24 dp
    val refractionHeightDp: Float = 28f, // 0 to 60 dp
    val refractionAmountDp: Float = 28f, // 0 to 60 dp (refraction warping)
    val chromaticAberration: Boolean = true,
    val hasHighlight: Boolean = true,
    val hasShadow: Boolean = true,
    /**
     * Strength of the soft white tint every glass card wears (0.0 = none, 1.0 = opaque white).
     * The default is deliberately faint: it lifts a card off the sky without hiding it.
     * Surfaces that must stay optically pure opt out through `clearGlass`.
     */
    val tintAlpha: Float = DEFAULT_TINT_ALPHA,

    // ---------------------------------------------------------------------------------------
    // 6. COLOR MIXING — the film the glass wears is a two-colour recipe, not a fixed white.
    // ---------------------------------------------------------------------------------------
    /**
     * Hue of the first film colour, degrees on the colour wheel.
     *
     * The film is mixed in HSV, so one hue dial plus one saturation dial describes every tinted glass
     * there is: leave saturation at 0 and the hue does nothing (white film, the shipped look), push it
     * up and the glass takes that hue at [tintAlpha].
     */
    val tintHueA: Float = DEFAULT_HUE_A,
    /** Hue of the second film colour: the other end of the mix. */
    val tintHueB: Float = DEFAULT_HUE_B,
    /** How far the film leans from [tintHueA] to [tintHueB], 0..1, along the shorter way round. */
    val tintMix: Float = 0f,
    /** How coloured the film is, 0 (neutral white) to 1 (fully saturated at the mixed hue). */
    val tintSaturation: Float = 0f,

    // ---------------------------------------------------------------------------------------
    // 7. SHADER DESIGN — how the glass refracts, and how the light lies on it.
    // ---------------------------------------------------------------------------------------
    /** Which refraction recipe the glass uses (see [GlassShaderProfile]). */
    val shaderProfile: GlassShaderProfile = GlassShaderProfile.BALANCED,
    /** Where the specular sheen is drawn on the glass (see [GlassSheen]). */
    val sheen: GlassSheen = GlassSheen.TOP,
    /** How bright that sheen is, 0 (none) to 1 (strongest the design allows). */
    val sheenStrength: Float = DEFAULT_SHEEN_STRENGTH,

    // ---------------------------------------------------------------------------------------
    // 8. SMOOTH EDGES — the corner geometry the glass is clipped to.
    // ---------------------------------------------------------------------------------------
    /**
     * Corner smoothness, 0 (a circular corner: an exact rounded rectangle) to 1 (the app's squircle).
     * See [CornerSmoothness] — this is the shape itself, not a blur pretending to be one.
     */
    val edgeSmoothing: Float = CornerSmoothness.DEFAULT
) {
    companion object {
        /** The "very soft, slight white tint" the design system ships with. */
        const val DEFAULT_TINT_ALPHA: Float = 0.07f

        /** Two cool hues, far enough apart that the mix dial is visible when it is moved. */
        const val DEFAULT_HUE_A: Float = 205f
        const val DEFAULT_HUE_B: Float = 285f

        /** A sheen you can see, without the card looking wet. */
        const val DEFAULT_SHEEN_STRENGTH: Float = 0.34f
    }
}

/**
 * The refraction recipe: how much blur the glass diffuses with and how hard it bends the backdrop.
 *
 * These are not cosmetic labels. Each profile multiplies the blur and refraction the style asks for, so
 * a [CRYSTAL] card genuinely resolves finer detail behind it and a [FROSTED] one genuinely blurs more;
 * the difference is the shader, not a colour.
 */
enum class GlassShaderProfile(
    val labelEn: String,
    val labelFa: String,
    val blurScale: Float,
    val refractionScale: Float
) {
    /** Sharp, thin, strongly bent: closest to polished crystal, least forgiving of a busy backdrop. */
    CRYSTAL("Crystal", "کریستال", blurScale = 0.45f, refractionScale = 1.30f),

    /** The shipped balance of diffusion and bend. */
    BALANCED("Balanced", "متعادل", blurScale = 1.00f, refractionScale = 1.00f),

    /** Deep diffusion, gentle bend: frosted glass, the most legible over a photograph. */
    FROSTED("Frosted", "مات", blurScale = 1.90f, refractionScale = 0.55f)
}

/**
 * Where the specular sheen lies on the glass.
 *
 * Drawn by the app, inside the glass clip and under the content — it is the light that tells the eye a
 * surface has a surface, which is the part a pure refraction pass cannot do on its own.
 */
enum class GlassSheen(val labelEn: String, val labelFa: String) {
    /** No sheen: the most neutral, and the flattest. */
    NONE("None", "بدون"),

    /** Light entering from the top edge, fading down. */
    TOP("Top light", "نور از بالا"),

    /** Light raking across the card from the top-start corner to the bottom-end corner. */
    DIAGONAL("Diagonal", "قطری"),

    /** Light catching both side rims and leaving the middle clear — reads as a thicker slab. */
    RIM("Rim light", "لبه‌روشن")
}

/**
 * Named film recipes for the colour-mixing dials.
 *
 * The point of the presets is that the three numbers behind them are the *same* three the sliders set —
 * so a user can start from one and keep turning, and the preview and the card are always in agreement.
 */
enum class GlassFilmPreset(
    val labelEn: String,
    val labelFa: String,
    val hueA: Float,
    val hueB: Float,
    val mix: Float,
    val saturation: Float,
    val alpha: Float
) {
    /** The shipped look: a faint neutral white film that lifts the card off the sky. */
    NEUTRAL("Neutral", "خنثی", 205f, 285f, 0f, 0f, 0.07f),

    /** Cold sea green moving toward blue. */
    SEA_GLASS("Sea glass", "شیشه دریا", 170f, 215f, 0.45f, 0.55f, 0.14f),

    /** Warm pink into violet. */
    ROSE_QUARTZ("Rose quartz", "کوارتز صورتی", 335f, 292f, 0.5f, 0.45f, 0.13f),

    /** Cream into pale gold: the warmest of the set. */
    MOONSTONE("Moonstone", "سنگ ماه", 45f, 20f, 0.55f, 0.30f, 0.12f),

    /** Deep indigo at low alpha: glass at night, dark enough to sit on a bright photograph. */
    OBSIDIAN("Obsidian", "ابسیدین", 250f, 225f, 0.35f, 0.70f, 0.22f)
}

val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }

/**
 * The soft white tint a glass surface wears, straight from Settings.
 * Returns [Color.Transparent] at zero so callers can skip the draw entirely.
 */
@Composable
@ReadOnlyComposable
fun liquidGlassTintColor(
    alphaOverride: Float? = null,
    config: LiquidGlassConfig = LocalLiquidGlassConfig.current
): Color = glassFilmColor(config, alphaOverride)

/**
 * The two-colour film recipe in HSV, resolved to a colour.
 *
 * Pure, non-composable and shared by the modifier, the text-contrast resolver and the settings preview,
 * so the swatch in Settings is literally the value the cards are given rather than a lookalike.
 */
fun glassFilmColor(config: LiquidGlassConfig, alphaOverride: Float? = null): Color {
    val alpha = (alphaOverride ?: config.tintAlpha).coerceIn(0f, 1f)
    if (alpha <= 0.001f) return Color.Transparent
    return hsvColor(
        hueDeg = mixHue(config.tintHueA, config.tintHueB, config.tintMix),
        saturation = config.tintSaturation,
        value = 1f,
        alpha = alpha
    )
}

/**
 * Mixes two hues the short way round the wheel.
 *
 * `350° → 10°` must pass through red, not through the whole colour wheel on the way; taking the signed
 * difference into `[-180°, 180°)` is what makes a hue slider feel like a dial instead of a number line.
 */
fun mixHue(hueA: Float, hueB: Float, t: Float): Float {
    val a = (hueA % 360f + 360f) % 360f
    val b = (hueB % 360f + 360f) % 360f
    val delta = ((b - a + 540f) % 360f) - 180f
    val mixed = a + delta * t.coerceIn(0f, 1f)
    return (mixed % 360f + 360f) % 360f
}

/**
 * HSV → sRGB.
 *
 * Written out rather than delegated so the same function can run on a plain JVM in the unit tests that
 * pin the film recipe; Compose's `Color.hsv` is an Android-backed helper and the colour maths is the
 * part worth testing.
 */
fun hsvColor(hueDeg: Float, saturation: Float, value: Float, alpha: Float = 1f): Color {
    val h = (hueDeg % 360f + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val sector = (h / 60f).toInt() % 6
    val r: Float
    val g: Float
    val b: Float
    when (sector) {
        0 -> { r = c; g = x; b = 0f }
        1 -> { r = x; g = c; b = 0f }
        2 -> { r = 0f; g = c; b = x }
        3 -> { r = 0f; g = x; b = c }
        4 -> { r = x; g = 0f; b = c }
        else -> { r = c; g = 0f; b = x }
    }
    return Color(r + m, g + m, b + m, alpha.coerceIn(0f, 1f))
}

/**
 * The sheen brush for a glass surface of [size], or `null` when the design has none.
 *
 * Returned as a brush so the caller stays a single `drawBehind`; the shapes are deliberately simple
 * linear/edge gradients — a specular reflection is a gradient, and anything more elaborate reads as a
 * picture of a reflection rather than as one.
 */
internal fun sheenBrush(
    sheen: GlassSheen,
    strength: Float,
    size: androidx.compose.ui.geometry.Size
): Brush? {
    val a = (strength.coerceIn(0f, 1f) * 0.26f)
    if (sheen == GlassSheen.NONE || a <= 0.002f) return null
    val white = Color.White
    return when (sheen) {
        GlassSheen.NONE -> null
        GlassSheen.TOP -> Brush.verticalGradient(
            0f to white.copy(alpha = a),
            0.22f to white.copy(alpha = a * 0.55f),
            0.55f to white.copy(alpha = 0f),
            1f to white.copy(alpha = 0f),
            startY = 0f,
            endY = size.height
        )
        GlassSheen.DIAGONAL -> Brush.linearGradient(
            0f to white.copy(alpha = a),
            0.45f to white.copy(alpha = a * 0.25f),
            1f to white.copy(alpha = 0f),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
        )
        GlassSheen.RIM -> Brush.horizontalGradient(
            0f to white.copy(alpha = a),
            0.10f to white.copy(alpha = a * 0.15f),
            0.5f to white.copy(alpha = 0f),
            0.90f to white.copy(alpha = a * 0.15f),
            1f to white.copy(alpha = a),
            startX = 0f,
            endX = size.width
        )
    }
}

/**
 * True when this config's film *is* [preset] — so the chip can show as selected.
 *
 * Compared with the tolerances the sliders themselves have (whole degrees on hue, one percent on the
 * ratios) rather than exactly, because a value that came back from `SharedPreferences` is a `Float`
 * round-trip away from the constant, and a preset that does not light up after you tap it is worse than
 * no preset at all.
 */
fun LiquidGlassConfig.matches(preset: GlassFilmPreset): Boolean {
    fun near(a: Float, b: Float, tolerance: Float) = abs(a - b) <= tolerance
    fun hueNear(a: Float, b: Float) = near((a % 360f + 360f) % 360f, (b % 360f + 360f) % 360f, 0.75f)
    return hueNear(tintHueA, preset.hueA) &&
        hueNear(tintHueB, preset.hueB) &&
        near(tintMix, preset.mix, 0.01f) &&
        near(tintSaturation, preset.saturation, 0.01f) &&
        near(tintAlpha, preset.alpha, 0.005f)
}

/**
 * The film a glass card wears over its own base color.
 *
 * A call site that designed its own `glassTint` gets exactly that and nothing else — it already chose how
 * opaque its card should be. Everything else takes the soft white tint from Settings, which is the
 * `tintAlpha`. Clear glass (`clearGlass`) wears no film at all.
 */
internal fun resolveGlassFilm(
    fallbackColor: Color,
    glassTint: Color,
    tintColor: Color,
    clearGlass: Boolean
): Color = when {
    clearGlass -> Color.Transparent
    glassTint != Color.Transparent -> glassTint
    tintColor.alpha <= 0.001f -> Color.Transparent
    else -> tintColor
}

/**
 * The plate a real-glass card lays under its content so the card keeps its own material — and therefore
 * its text contrast — over a backdrop whose pixels it cannot know. It is the card's own base color at
 * [LEGIBILITY_PLATE_ALPHA], so compositing it changes nothing: the card still reads as `base` plus the
 * film, and the ink resolved below stays true on both the glass and the fallback render path.
 *
 * Surfaces that must stay optically pure opt out: `clearGlass`, or a call site that brought its own film.
 */
internal fun resolveLegibilityPlate(
    fallbackColor: Color,
    glassTint: Color,
    clearGlass: Boolean
): Color = if (clearGlass || glassTint != Color.Transparent) {
    Color.Transparent
} else {
    fallbackColor.copy(alpha = LEGIBILITY_PLATE_ALPHA)
}

/**
 * Text colors that stay legible on top of glass, whatever the theme, the backdrop or the tint setting.
 *
 * Glass is translucent, so a fixed text color can end up on top of a bright day sky in a dark theme or on
 * top of a heavy white film in a light one — and a card whose base color is deliberately dark (the AR
 * overlays) would get dark text from a naive "is the theme light?" check. This composites the film the
 * card actually wears over the card's own base color and picks near-black or near-white ink from the
 * result's luminance, so contrast never depends on the caller guessing.
 */
@Immutable
data class GlassTextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

@Composable
@ReadOnlyComposable
fun liquidGlassTextColors(
    base: Color = RedTheme.colors.surfaceElevated,
    config: LiquidGlassConfig = LocalLiquidGlassConfig.current,
    tint: Color = Color.Transparent,
    tintAlphaOverride: Float? = null,
    clearGlass: Boolean = false
): GlassTextColors {
    val film = resolveGlassFilm(
        fallbackColor = base,
        glassTint = tint,
        tintColor = glassFilmColor(config, tintAlphaOverride),
        clearGlass = clearGlass
    )

    val effective = if (film == Color.Transparent) {
        base.copy(alpha = 1f)
    } else {
        Color(
            red = base.red + (film.red - base.red) * film.alpha,
            green = base.green + (film.green - base.green) * film.alpha,
            blue = base.blue + (film.blue - base.blue) * film.alpha,
            alpha = 1f
        )
    }

    val luminance = inkedLuminance(effective)
    val ink = if (luminance > 0.55f) Color(0xFF080A12) else Color(0xFFF8FAFF)
    return GlassTextColors(
        primary = ink,
        secondary = ink.copy(alpha = 0.74f),
        tertiary = ink.copy(alpha = 0.56f)
    )
}

/**
 * Relative luminance of a color, from the same weights the ink decision has always used. Public because
 * the AR/composited surfaces need to make the same call for their own fixed chrome.
 */
fun inkedLuminance(color: Color): Float =
    0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

/**
 * The readable ink for a given background — near-black or near-white, never a mid gray that dies on both.
 */
fun readableInkOn(background: Color): Color =
    if (inkedLuminance(background) > 0.55f) Color(0xFF080A12) else Color(0xFFF8FAFF)

/**
 * Android version capability check for native Backdrop shaders.
 * Liquid Glass physical refraction requires Android 13+ (API 33+).
 */
fun isLiquidGlassSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/**
 * RED Liquid Glass Optical Specification
 *
 * Real Liquid Glass material specification based on Kyant0 Backdrop architecture:
 * - Real-time background pixel sampling & optical refraction
 * - Spatial vibrancy enhancement
 * - Configurable Gaussian blur diffusion
 * - Convex / concave lens distortion with optional chromatic dispersion
 * - Ambient highlight, external ambient shadow, and internal bevel shadow
 */
@Immutable
data class LiquidGlassStyle(
    val blurRadius: Dp = 8.dp,
    val refractionHeight: Dp = 24.dp,
    val refractionAmount: Dp = 24.dp,
    val chromaticAberration: Boolean = false,
    val hasHighlight: Boolean = true,
    val shadowRadius: Dp = 12.dp,
    val innerShadowRadius: Dp = 2.dp
)

object LiquidGlassDefaults {
    val NavigationBar = LiquidGlassStyle(
        blurRadius = 0.dp,
        refractionHeight = 28.dp,
        refractionAmount = 28.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 12.dp,
        innerShadowRadius = 2.dp
    )

    val Header = LiquidGlassStyle(
        blurRadius = 0.dp,
        refractionHeight = 22.dp,
        refractionAmount = 22.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 8.dp,
        innerShadowRadius = 1.5.dp
    )

    val Pill = LiquidGlassStyle(
        blurRadius = 0.dp,
        refractionHeight = 16.dp,
        refractionAmount = 16.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 6.dp,
        innerShadowRadius = 1.5.dp
    )

    val Card = LiquidGlassStyle(
        blurRadius = 1.dp,
        refractionHeight = 22.dp,
        refractionAmount = 22.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 8.dp,
        innerShadowRadius = 2.dp
    )

    val Modal = LiquidGlassStyle(
        blurRadius = 2.dp,
        refractionHeight = 28.dp,
        refractionAmount = 28.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 16.dp,
        innerShadowRadius = 3.dp
    )

    val Window = LiquidGlassStyle(
        blurRadius = 1.dp, // 1 / 24 blur intensity for floating sheets and dialog backdrop
        refractionHeight = 16.dp,
        refractionAmount = 16.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 16.dp,
        innerShadowRadius = 2.dp
    )

    val LocationCard = LiquidGlassStyle(
        blurRadius = 12.dp, // 12 / 24 blur intensity for location cards
        refractionHeight = 24.dp,
        refractionAmount = 24.dp,
        chromaticAberration = true,
        hasHighlight = true,
        shadowRadius = 8.dp,
        innerShadowRadius = 1.5.dp
    )
}

/**
 * Reusable Liquid Glass Modifier.
 *
 * Applies real-time backdrop sampling, lens refraction, vibrancy, blur,
 * highlights, and shadows when enabled & supported.
 * Falls back cleanly to premium RED design system surface tokens on Android 12-
 * or when disabled by user preference.
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current,
    shape: Shape = squircleShape(RedCornerRadius.window),
    style: LiquidGlassStyle = LiquidGlassDefaults.NavigationBar,
    fallbackColor: Color = RedTheme.colors.surfaceElevated,
    fallbackBorder: BorderStroke = BorderStroke(1.dp, RedTheme.colors.border),
    fallbackShadowElevation: Dp = RedElevation.floating,
    glassTint: Color = Color.Transparent,
    glassBorder: BorderStroke? = BorderStroke(0.75.dp, RedTheme.colors.border.copy(alpha = 0.5f)),
    clearGlass: Boolean = false,
    enabled: Boolean = LocalLiquidGlassEnabled.current
): Modifier {
    val config = LocalLiquidGlassConfig.current
    val isGlassActive = enabled && config.enabled && isLiquidGlassSupported() && backdrop != null

    // The user's corner geometry, resolved once: at the default dial position this *is* the shape the
    // call site passed, so nothing in the app changes unless the dial was moved.
    val effectiveShape = shape.withCornerSmoothing(config.edgeSmoothing)

    // One film decision for both render paths (an explicit `glassTint` wins, `clearGlass` wears none,
    // everything else takes the two-colour recipe from Settings) and one plate that keeps the card's
    // own material under its content so the ink its children were given stays true over any backdrop.
    val activeTint = resolveGlassFilm(
        fallbackColor = fallbackColor,
        glassTint = glassTint,
        tintColor = glassFilmColor(config),
        clearGlass = clearGlass
    )
    // A sheen is a cacheable brush: it depends on the size, not on the frame, so it is built once per
    // layout instead of once per draw.
    val sheenModifier = if (config.sheen != GlassSheen.NONE && config.sheenStrength > 0.002f) {
        Modifier.drawWithCache {
            val brush = sheenBrush(config.sheen, config.sheenStrength, size)
            onDrawBehind { brush?.let { drawRect(it) } }
        }
    } else {
        Modifier
    }
    val legibilityPlate = resolveLegibilityPlate(
        fallbackColor = fallbackColor,
        glassTint = glassTint,
        clearGlass = clearGlass
    )

    return if (isGlassActive) {
        // Dynamic blur radius, scaled by the shader profile: a frosted recipe diffuses more of the
        // backdrop, a crystal one barely touches it.
        val profile = config.shaderProfile
        val activeBlur = ((config.blurRadiusDp + style.blurRadius.value) * profile.blurScale)
            .coerceAtLeast(0f).dp

        // Dynamic refraction depth & warping amount scaled by user preferences and the profile
        val baseRefractionHeight = style.refractionHeight.value
        val activeRefractionHeight = (config.refractionHeightDp * profile.refractionScale *
            (if (baseRefractionHeight > 0f) baseRefractionHeight / 28f else 1f)).coerceAtLeast(0f).dp

        val baseRefractionAmount = style.refractionAmount.value
        val activeRefractionAmount = (config.refractionAmountDp * profile.refractionScale *
            (if (baseRefractionAmount > 0f) baseRefractionAmount / 28f else 1f)).coerceAtLeast(0f).dp

        val activeChromatic = config.chromaticAberration && style.chromaticAberration
        val activeHighlight = config.hasHighlight && style.hasHighlight
        val activeShadow = if (config.hasShadow) style.shadowRadius else 0.dp
        val activeInnerShadow = if (config.hasShadow) style.innerShadowRadius else 0.dp

        // Clarity frosted tint: when clarity < 1.0f, introduce frosted tint opacity
        val clarityFrostedColor = if (config.clarity < 0.99f) {
            RedTheme.colors.surfaceElevated.copy(alpha = (1.0f - config.clarity) * 0.45f)
        } else {
            Color.Transparent
        }

        this
            .drawBackdrop(
                backdrop = backdrop!!,
                shape = { effectiveShape },
                effects = {
                    vibrancy()
                    if (activeBlur > 0.dp) {
                        blur(activeBlur.toPx())
                    }
                    if (activeRefractionHeight > 0.dp || activeRefractionAmount > 0.dp) {
                        lens(
                            refractionHeight = activeRefractionHeight.toPx(),
                            refractionAmount = activeRefractionAmount.toPx(),
                            chromaticAberration = activeChromatic
                        )
                    }
                },
                highlight = if (activeHighlight) { { Highlight.Ambient } } else null,
                shadow = if (activeShadow > 0.dp) { { Shadow(radius = activeShadow) } } else null,
                innerShadow = if (activeInnerShadow > 0.dp) { { InnerShadow(radius = activeInnerShadow) } } else null
            )
            .then(if (clarityFrostedColor != Color.Transparent) Modifier.background(clarityFrostedColor, effectiveShape) else Modifier)
            .then(if (legibilityPlate != Color.Transparent) Modifier.background(legibilityPlate, effectiveShape) else Modifier)
            .then(if (activeTint != Color.Transparent) Modifier.background(activeTint, effectiveShape) else Modifier)
            // The sheen is drawn *over* the sampled backdrop and *under* the content, which is where a
            // reflection physically is: light on the outside of the surface, not on the text.
            .then(sheenModifier)
            .then(if (glassBorder != null) Modifier.border(glassBorder, effectiveShape) else Modifier)
            .clip(effectiveShape)
    } else {
        this
            .then(if (fallbackShadowElevation > 0.dp) Modifier.shadow(fallbackShadowElevation, effectiveShape) else Modifier)
            .background(fallbackColor, effectiveShape)
            .then(if (activeTint != Color.Transparent) Modifier.background(activeTint, effectiveShape) else Modifier)
            .then(sheenModifier)
            .border(fallbackBorder, effectiveShape)
            .clip(effectiveShape)
    }
}

/**
 * Reusable Native Liquid Glass Surface Composable with built-in responsive touch feedback,
 * high-contrast styling, and automatic RED premium fallback.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current,
    style: LiquidGlassStyle = LiquidGlassDefaults.Card,
    shape: Shape = squircleShape(RedCornerRadius.window),
    fallbackColor: Color = RedTheme.colors.surfaceElevated,
    fallbackBorder: BorderStroke = BorderStroke(1.dp, RedTheme.colors.border),
    fallbackShadowElevation: Dp = RedElevation.floating,
    glassTint: Color = Color.Transparent,
    glassBorder: BorderStroke? = BorderStroke(0.75.dp, RedTheme.colors.border.copy(alpha = 0.5f)),
    clearGlass: Boolean = false,
    enabled: Boolean = LocalLiquidGlassEnabled.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (() -> Unit)? = null,
    contentColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "LiquidGlassPress"
    )

    // Readability is solved once, here, instead of at every call site: the token set this surface hands
    // down carries text colors resolved against the glass it is actually drawn on (surface + frost + the
    // user's tint). Every child that reads RedTheme.colors.textPrimary/Secondary/Tertiary — section
    // headers, badges, metric rows — inherits a contrast-safe color without knowing glass exists.
    val glassText = liquidGlassTextColors(
        base = fallbackColor,
        tint = glassTint,
        clearGlass = clearGlass
    )
    val surfaceColors = RedTheme.colors.copy(
        textPrimary = glassText.primary,
        textSecondary = glassText.secondary,
        textTertiary = glassText.tertiary
    )

    // Surface's own semantic (Material 3 boxes its content with propagateMinConstraints) is preserved
    // here: a card that is given a width passes that width down, so a Row with SpaceBetween inside it
    // still spreads instead of collapsing to the left. Converted cards keep their exact layout.
    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .scale(pressScale)
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                style = style,
                fallbackColor = fallbackColor,
                fallbackBorder = fallbackBorder,
                fallbackShadowElevation = fallbackShadowElevation,
                glassTint = glassTint,
                glassBorder = glassBorder,
                clearGlass = clearGlass,
                enabled = enabled
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        CompositionLocalProvider(
            LocalRedColors provides surfaceColors,
            // A card that used to declare `contentColor` on its Surface keeps that intent: the token set
            // above carries the contrast-safe ink, and this keeps any child reading LocalContentColor
            // (Material's Text/Icon default) on the same footing.
            LocalContentColor provides if (contentColor == Color.Unspecified) {
                glassText.primary
            } else {
                contentColor
            },
            // And a card nested inside a card never samples the same backdrop pass again — the rule the
            // placement notes at the top of this file exist for. Inside here, nested surfaces take the
            // fallback path (same tint, same ink, no second refraction pass).
            LocalLiquidGlassBackdrop provides null
        ) {
            content()
        }
    }
}











