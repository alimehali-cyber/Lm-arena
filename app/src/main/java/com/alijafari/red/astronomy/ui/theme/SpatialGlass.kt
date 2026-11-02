package com.alijafari.red.astronomy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.SpatialPalette
import com.alijafari.red.astronomy.domain.UiStyle

/**
 * ============================================================================
 * THE SPATIAL GLASS MATERIAL
 * ============================================================================
 *
 * The optional second interface. One description ([SpatialGlassSpec]) holds everything the material
 * needs — the world it floats over, the light it refracts, the edge it catches — and everything else
 * (the card surface, the ambient backdrop, the settings preview, the type scale) reads that
 * description rather than hard-coding a colour. Adding a palette is adding one spec below; it cannot
 * half-apply, because there is nothing else to change.
 *
 * ## What is actually being drawn
 *
 * A real backdrop blur samples the pixels behind a panel. That needs a render-surface capture, and
 * the app used to own one for exactly this: it was removed together with the material that needed it,
 * because every panel in the app nevertheless had to *look the same on every device* and its ink had
 * to be known before it was painted (see the note on `RedSurface`).
 *
 * This material gets the same look without sampling anything, by re-lighting instead of re-sampling:
 *
 * 1. **The fill.** A translucent tinted sheet — `background: rgba(255,255,255,0.10)` in CSS terms.
 *    Whatever the ambient backdrop is doing shows through it, exactly as it would through real glass.
 * 2. **The refracted light.** The card paints the *palette's own* mesh blobs inside its outline, at a
 *    much larger scale than the backdrop's and at higher alpha (the shape's own `Path`, filled with a
 *    radial gradient, needs no separate clip). Because the backdrop is itself soft, wide colour
 *    fields, a blur of it and a large-scale re-lighting of it are the same picture — which is why the
 *    cheapest glass in this style is also the most faithful one.
 * 3. **The light-emitting edge.** A linear-gradient hairline around the squircle
 *    (bright top-left → faint → bright bottom-right) plus a second, wider inner stroke that fades
 *    downwards. That is the specular "thick cut glass" rim; a flat `border` cannot express it, which is
 *    why the flat border is skipped while this material is on.
 * 4. **The sheen and the drop.** A diagonal white-to-transparent gradient across the upper half and a
 *    soft black gradient at the bottom, so the sheet reads as a solid piece of material in depth.
 *
 * ## Ink
 *
 * None of the fills are opaque, so "the ink for this surface" cannot be measured against the fill
 * itself — a 10 % white sheet is white, and would demand black text. [spatialSurfaceInk] composites
 * the fill over the palette's own base colour first, which is what the eye actually sees, and measures
 * that. Every glass panel therefore resolves white ink over the dark ambient world, by construction and
 * not by convention.
 */
@Immutable
data class AmbientBlob(
    val color: Color,
    /** Horizontal centre, as a fraction of the surface width. */
    val centerX: Float,
    /** Vertical centre, as a fraction of the surface height. */
    val centerY: Float,
    /** Radius, as a fraction of the surface's longest side. */
    val radius: Float,
    /** How far this blob wanders, as a fraction of the surface's longest side. */
    val driftX: Float,
    val driftY: Float,
    /** Where in the drift cycle this blob starts, 0..1. */
    val phase: Float
)

/**
 * One complete ambient world: what the glass floats over, what it refracts, and how it is lit.
 *
 * Immutable and cheap to compare, so it can live in a composition local and drive every panel in the
 * app without recomposing anything that did not use it.
 */
@Immutable
data class SpatialGlassSpec(
    val enabled: Boolean,
    val palette: SpatialPalette,
    val nameEn: String,
    val nameFa: String,
    /** The deepest colour of the ambient world — also the ink's measuring surface. */
    val ambientBase: Color,
    /** The colour the ambient world sinks to at the bottom of the screen. */
    val ambientBaseBottom: Color,
    val blobs: List<AmbientBlob>,
    /** The translucent sheet a normal card is cut from. */
    val glassFill: Color,
    /** A slightly denser sheet, for grouped containers and the floating bar. */
    val glassFillStrong: Color,
    /** The two colours a card re-lights itself with — picked out of [blobs]. */
    val refractionWarm: Color,
    val refractionCool: Color,
    /** Specular edge colour (usually white, tinted per palette). */
    val edge: Color,
    /** Light that falls on the panel's top-left corner. */
    val specular: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentGold: Color,
    val accentBlue: Color,
    /** The colour the panel's drop shadow and outer glow take. */
    val glow: Color,
    /** The dot grid painted over the ambient world. */
    val dot: Color
) {
    companion object {
        /** The spec that means "the default interface": nothing above is drawn. */
        val Disabled = SpatialGlassSpec(
            enabled = false,
            palette = SpatialPalette.DEFAULT,
            nameEn = "None",
            nameFa = "هیچ",
            ambientBase = Color.Black,
            ambientBaseBottom = Color.Black,
            blobs = emptyList(),
            glassFill = Color.Transparent,
            glassFillStrong = Color.Transparent,
            refractionWarm = Color.Transparent,
            refractionCool = Color.Transparent,
            edge = Color.White,
            specular = Color.White,
            accent = Color.White,
            accentSoft = Color.Transparent,
            accentGold = Color.White,
            accentBlue = Color.White,
            glow = Color.Black,
            dot = Color.White
        )
    }
}

/**
 * Aurora — the coral / peach / cyan / violet mesh the style is known for, over a violet-black night.
 */
private val AuroraGlassSpec = SpatialGlassSpec(
    enabled = true,
    palette = SpatialPalette.AURORA,
    nameEn = "Aurora",
    nameFa = "شفق",
    ambientBase = Color(0xFF14101C),
    ambientBaseBottom = Color(0xFF07060C),
    blobs = listOf(
        AmbientBlob(Color(0xFFFF8D6B), centerX = 0.16f, centerY = 0.10f, radius = 0.86f, driftX = 0.06f, driftY = 0.04f, phase = 0.00f),
        AmbientBlob(Color(0xFFFFB38A), centerX = 0.90f, centerY = 0.24f, radius = 0.62f, driftX = 0.05f, driftY = 0.05f, phase = 0.35f),
        AmbientBlob(Color(0xFF63E6E2), centerX = 0.82f, centerY = 0.82f, radius = 0.70f, driftX = 0.07f, driftY = 0.05f, phase = 0.60f),
        AmbientBlob(Color(0xFF8C7BFF), centerX = 0.10f, centerY = 0.72f, radius = 0.78f, driftX = 0.05f, driftY = 0.06f, phase = 0.82f),
        AmbientBlob(Color(0xFFFF6FA5), centerX = 0.50f, centerY = 0.46f, radius = 0.48f, driftX = 0.08f, driftY = 0.05f, phase = 0.18f)
    ),
    glassFill = Color(0x17FFFFFF),
    glassFillStrong = Color(0x22FFFFFF),
    refractionWarm = Color(0xFFFF9E7E),
    refractionCool = Color(0xFF8FD9FF),
    edge = Color.White,
    specular = Color.White,
    accent = Color(0xFFFF9A8B),
    accentSoft = Color(0x33FF9A8B),
    accentGold = Color(0xFFFFD9A0),
    accentBlue = Color(0xFF8FD9FF),
    glow = Color(0xFF9A5BFF),
    dot = Color(0xFFEDE7FF)
)

/**
 * Wine · می — the same construction in wine red and burgundy: a cellar-dark base, burgundy and
 * crimson light, a rose specular and a gold edge. No coral, no cyan: the world is one family of red,
 * which is why the glass reads as wine held up to a lamp rather than as the aurora in red.
 */
private val WineGlassSpec = SpatialGlassSpec(
    enabled = true,
    palette = SpatialPalette.WINE,
    nameEn = "Wine · Mei",
    nameFa = "شرابی · می",
    ambientBase = Color(0xFF160409),
    ambientBaseBottom = Color(0xFF080103),
    blobs = listOf(
        AmbientBlob(Color(0xFF8E1230), centerX = 0.20f, centerY = 0.12f, radius = 0.82f, driftX = 0.06f, driftY = 0.04f, phase = 0.00f),
        AmbientBlob(Color(0xFFD1506F), centerX = 0.92f, centerY = 0.28f, radius = 0.60f, driftX = 0.05f, driftY = 0.05f, phase = 0.32f),
        AmbientBlob(Color(0xFF5A0A1E), centerX = 0.78f, centerY = 0.84f, radius = 0.74f, driftX = 0.07f, driftY = 0.04f, phase = 0.58f),
        AmbientBlob(Color(0xFFB07A3C), centerX = 0.12f, centerY = 0.78f, radius = 0.52f, driftX = 0.05f, driftY = 0.06f, phase = 0.80f),
        AmbientBlob(Color(0xFFA2153A), centerX = 0.52f, centerY = 0.48f, radius = 0.50f, driftX = 0.08f, driftY = 0.05f, phase = 0.16f)
    ),
    glassFill = Color(0x1AFFE9ED),
    glassFillStrong = Color(0x24FFE9ED),
    refractionWarm = Color(0xFFE0708A),
    refractionCool = Color(0xFFC9A227),
    edge = Color(0xFFFFEDE0),
    specular = Color(0xFFFFEDE8),
    accent = Color(0xFFE8677F),
    accentSoft = Color(0x33E8677F),
    accentGold = Color(0xFFD9B26A),
    accentBlue = Color(0xFFC98BA6),
    glow = Color(0xFF8E1230),
    dot = Color(0xFFF6DDE4)
)

/**
 * The one place a (style, palette) pair becomes a spec. [UiStyle.CLASSIC] always answers
 * [SpatialGlassSpec.Disabled], so a stale palette preference can never light the default interface.
 */
fun spatialGlassSpecFor(style: UiStyle, palette: SpatialPalette): SpatialGlassSpec =
    if (style != UiStyle.SPATIAL) {
        SpatialGlassSpec.Disabled
    } else {
        when (palette) {
            SpatialPalette.AURORA -> AuroraGlassSpec
            SpatialPalette.WINE -> WineGlassSpec
        }
    }

/** Every palette, in the order Settings offers them, for previews built from real specs. */
val SpatialGlassPalettes: List<SpatialGlassSpec> = listOf(AuroraGlassSpec, WineGlassSpec)

/** The material the composition is currently drawn with. [SpatialGlassSpec.Disabled] by default. */
val LocalSpatialGlass = staticCompositionLocalOf { SpatialGlassSpec.Disabled }

/** Accessor, matching [RedTheme]: `SpatialGlassTheme.spec` / `SpatialGlassTheme.enabled`. */
object SpatialGlassTheme {
    val spec: SpatialGlassSpec
        @Composable
        @ReadOnlyComposable
        get() = LocalSpatialGlass.current

    val enabled: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalSpatialGlass.current.enabled
}

/**
 * The ink for a glass panel: the fill is composited over the ambient world first, because that is the
 * colour the eye reads, and only then measured. Anything else would call a 10 % white sheet "white"
 * and paint black text on it.
 */
fun spatialSurfaceInk(spec: SpatialGlassSpec, base: Color, tint: Color = Color.Transparent): SurfaceInk {
    if (!spec.enabled) return surfaceInkFor(base, tint)
    val overAmbient = resolvedFill(spec.ambientBase, base)
    return surfaceInkFor(resolvedFill(overAmbient, tint))
}

/**
 * Paints the glass material into whatever shape it is applied to: refraction, sheen, drop and the
 * specular rim. Draw order is declaration order in Compose, so this goes *after* the translucent fill
 * and *before* the clip that bounds the content.
 */
fun Modifier.spatialGlassMaterial(
    shape: Shape,
    spec: SpatialGlassSpec,
    edgeWidth: Dp = 1.dp
): Modifier {
    if (!spec.enabled) return this
    return this.drawWithCache {
        // The card's own outline as a path: filling it paints the material's light inside the squircle
        // and stroking it draws the rim, both without a separate clip layer.
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        val longest = size.maxDimension.coerceAtLeast(1f)

        val warm = Brush.radialGradient(
            colors = listOf(spec.refractionWarm.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(size.width * 0.06f, -size.height * 0.22f),
            radius = longest * 1.05f
        )
        val cool = Brush.radialGradient(
            colors = listOf(spec.refractionCool.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width * 1.04f, size.height * 1.10f),
            radius = longest * 0.92f
        )
        // The specular sweep: light falling across the top-left of a thick sheet.
        val sheen = Brush.linearGradient(
            colors = listOf(
                spec.specular.copy(alpha = 0.20f),
                spec.specular.copy(alpha = 0.045f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.85f, size.height * 0.95f)
        )
        // Weight at the bottom, so the sheet has a lower edge to sit on.
        val drop = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)),
            startY = size.height * 0.35f,
            endY = size.height
        )
        // The rim: bright where the light source is, faint opposite it.
        val rim = Brush.linearGradient(
            colors = listOf(
                spec.edge.copy(alpha = 0.72f),
                spec.edge.copy(alpha = 0.12f),
                spec.edge.copy(alpha = 0.40f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        // The inner bevel, fading away from the top edge — refraction inside the material.
        val bevel = Brush.verticalGradient(
            colors = listOf(spec.specular.copy(alpha = 0.26f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.42f
        )

        onDrawBehind {
            drawPath(path, warm)
            drawPath(path, cool)
            drawPath(path, sheen)
            drawPath(path, drop)
            drawPath(path, rim, style = Stroke(width = edgeWidth.toPx()))
            drawPath(path, bevel, style = Stroke(width = edgeWidth.toPx() * 2.2f))
        }
    }
}

/**
 * An [Outline] as a [Path].
 *
 * `drawOutline` would express the same thing, but it is an extension that has moved between packages
 * across Compose releases; the three-subclass conversion below is the whole of what it does for the
 * shapes this app uses (squircles and rounded rects), written against `Path` members only.
 */
private fun Outline.toPath(): Path {
    val path = Path()
    when (this) {
        is Outline.Rectangle -> path.addRect(rect)
        is Outline.Rounded -> path.addRoundRect(roundRect)
        is Outline.Generic -> path.addPath(this.path)
    }
    return path
}

/**
 * The light the panel throws onto the world around it. Applied as a shadow whose colours come from
 * the palette, so a wine card glows burgundy and an aurora card glows violet, on API 28+.
 */
fun Modifier.spatialGlassGlow(elevation: Dp, shape: Shape, spec: SpatialGlassSpec): Modifier =
    if (!spec.enabled || elevation <= 0.dp) {
        this
    } else {
        shadow(
            elevation = elevation * 2f,
            shape = shape,
            clip = false,
            ambientColor = spec.glow.copy(alpha = 0.55f),
            spotColor = spec.glow.copy(alpha = 0.75f)
        )
    }

/**
 * The colour set the default interface's tokens become while glass is on.
 *
 * The structure is deliberately identical to [RedDarkColorTokens] — same roles, same meaning — so
 * every call site that reads `RedTheme.colors.surfaceElevated` keeps working untouched; only the
 * *material* changes: the surfaces become translucent sheets instead of opaque plates, and the ink
 * becomes the light off-white a glass panel carries.
 *
 * Nothing here goes through the contrast repair in `Theme.kt`, and that is not an oversight: the
 * repair measures text against an opaque surface, and these surfaces are transparent by design. Their
 * legibility is established instead by [spatialSurfaceInk], which measures against the ambient world
 * the sheet is composited over.
 */
fun spatialGlassColorTokens(spec: SpatialGlassSpec): RedColorTokens {
    val ink = Color(0xFFF8F5FF)
    return RedColorTokens(
        isDark = true,
        background = spec.ambientBase,
        backgroundSecondary = spec.ambientBaseBottom,
        backgroundTertiary = spec.ambientBase,
        surface = spec.glassFill,
        surfaceElevated = spec.glassFill,
        surfaceGrouped = spec.glassFillStrong,
        surfaceVariant = spec.glassFillStrong,
        textPrimary = ink,
        textSecondary = ink.copy(alpha = 0.76f),
        textTertiary = ink.copy(alpha = 0.56f),
        textQuaternary = ink.copy(alpha = 0.38f),
        border = spec.edge.copy(alpha = 0.26f),
        separator = spec.edge.copy(alpha = 0.14f),
        separatorOpaque = spec.ambientBaseBottom,
        accentRed = spec.accent,
        accentRedSubtle = spec.accentSoft,
        accentGold = spec.accentGold,
        accentBlue = spec.accentBlue,
        accentPurple = spec.refractionCool,
        statusSuccess = Color(0xFF7BE2A8),
        statusSuccessContainer = Color(0x267BE2A8),
        statusWarning = Color(0xFFFFC777),
        statusWarningContainer = Color(0x26FFC777),
        statusError = Color(0xFFFF8A8A),
        statusErrorContainer = Color(0x26FF8A8A),
        statusInfo = spec.accentBlue,
        statusInfoContainer = Color(0x2638BDF8),
        navSurface = spec.glassFillStrong,
        navBorder = spec.edge.copy(alpha = 0.30f)
    )
}
