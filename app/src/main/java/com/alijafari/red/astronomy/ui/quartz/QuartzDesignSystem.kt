package com.alijafari.red.astronomy.ui.quartz

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.LocalRedColors
import com.alijafari.red.astronomy.ui.theme.RedTheme
import java.util.Random
import kotlin.math.max

// ============================================================================
// "OBSIDIAN QUARTZ & CRIMSON TELEMETRY" — UNIFIED ART DIRECTION
// (editorial industrial-design studio language: Teenage Engineering x Nothing x Apple ID)
//
// One atmospheric mood, three surfaces, zero emojis, zero Material icons, zero font files:
//  * the canvas is editorial alabaster mist (or a nocturnal obsidian revision of it) with a
//    soft-focus obsidian & burgundy silhouette breathing through the centre;
//  * the hero surface is a frameless velvet-crimson lens (#170109 void -> #B81450 edge);
//  * secondary surfaces are smoked frosted quartz (#1D242B -> #5D7687) with 6.dp inset dotted
//    perimeter tracks, an 18.dp inner-edge light refraction and tactile anodized grain;
//  * every micro-icon (arrows, crosshair, chevrons, bookmark) is drawn mathematically with
//    0.75–1.3.dp hairlines, and every primary numeric readout is a procedural 5x7 LED
//    dot-matrix (see QuartzDotMatrix.kt) — never a monospace fallback.
// ============================================================================

/** The art-directed inks. Nothing outside this object may invent a colour for this style. */
object QuartzInk {
    // Editorial canvas
    val AlabasterMist = Color(0xFFE8E4DE)
    val AlabasterDeep = Color(0xFFDBD5CC)
    val ObsidianCanvas = Color(0xFF0A0709)
    val NocturneCanvas = Color(0xFF120309)

    // Silhouette aura
    val Obsidian = Color(0xFF090407)
    val BurgundyVoid = Color(0xFF1C0913)
    val CrimsonBleed = Color(0xFF59122B)
    val ChromaticRim = Color(0xFF4E86A3)

    // Surface A — velvet crimson lens
    val VelvetVoid = Color(0xFF170109)
    val VelvetMid = Color(0xFF4B0420)
    val VelvetRuby = Color(0xFF8C093B)
    val VelvetEdge = Color(0xFFB81450)

    // Surface B — smoked frosted quartz
    val QuartzTop = Color(0xFF1D242B)
    val QuartzMid = Color(0xFF283845)
    val QuartzBase = Color(0xFF5D7687)

    // Localized capsule accents
    val RoseQuartz = Color(0xFFD95B8A)
    val MolecularCobalt = Color(0xFF6B85B8)
    val WarmAmber = Color(0xFFD97236)
    val CrimsonSignal = Color(0xFF9E0B45)

    // Inks
    val CardInk = Color(0xFFFFF5F8)
    val CanvasInk = Color(0xFF1A1417)
    val NocturnalInk = Color(0xFFF4EEF1)
}

/**
 * One atmospheric palette. [isDark] describes the *canvas* (not the cards: the lenses are deep
 * in every mode), so the same velvet hero reads identically under alabaster and obsidian light.
 */
@Immutable
data class QuartzPalette(
    val themeMode: ThemeMode,
    val isDark: Boolean,
    /** Editorial canvas fill. */
    val canvas: Color,
    val canvasSecondary: Color,
    /** Silhouette aura: core, bleed and the cyan/slate chromatic aberration on the left edge. */
    val silhouetteCore: Color,
    val silhouetteBleed: Color,
    val chromaticRim: Color,
    /** Canvas inks (chrome written directly on the editorial paper). */
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val inkQuaternary: Color,
    /** 0.75.dp hairlines on the canvas. */
    val hairline: Color,
    val hairlineStrong: Color,
    /** Velvet lens gradient, centre -> perimeter. */
    val velvetCore: Color,
    val velvetMid: Color,
    val velvetRuby: Color,
    val velvetEdge: Color,
    val velvetRefraction: Color,
    /** Smoked quartz gradient, top -> base. */
    val quartzTop: Color,
    val quartzMid: Color,
    val quartzBase: Color,
    val quartzRefraction: Color,
    /** Inks painted on the lenses. */
    val cardInk: Color,
    val cardInkSecondary: Color,
    val cardInkTertiary: Color,
    val rimSpecular: Color,
    /** How strongly the anodized micro-grain reads on this canvas. */
    val grainAlpha: Float,
    // Localized accents
    val accentRose: Color,
    val accentCobalt: Color,
    val accentAmber: Color,
    val accentCrimson: Color,
    val matrixGhost: Color
)

/** Editorial Alabaster: the reference art direction, lit by a soft-box. */
val AlabasterQuartzPalette = QuartzPalette(
    themeMode = ThemeMode.LIGHT,
    isDark = false,
    canvas = QuartzInk.AlabasterMist,
    canvasSecondary = QuartzInk.AlabasterDeep,
    silhouetteCore = QuartzInk.Obsidian.copy(alpha = 0.92f),
    silhouetteBleed = QuartzInk.CrimsonBleed.copy(alpha = 0.30f),
    chromaticRim = QuartzInk.ChromaticRim.copy(alpha = 0.30f),
    inkPrimary = QuartzInk.CanvasInk.copy(alpha = 0.92f),
    inkSecondary = QuartzInk.CanvasInk.copy(alpha = 0.58f),
    inkTertiary = QuartzInk.CanvasInk.copy(alpha = 0.40f),
    inkQuaternary = QuartzInk.CanvasInk.copy(alpha = 0.22f),
    hairline = QuartzInk.CanvasInk.copy(alpha = 0.14f),
    hairlineStrong = QuartzInk.CanvasInk.copy(alpha = 0.26f),
    velvetCore = QuartzInk.VelvetVoid,
    velvetMid = QuartzInk.VelvetMid,
    velvetRuby = QuartzInk.VelvetRuby,
    velvetEdge = QuartzInk.VelvetEdge,
    velvetRefraction = Color(0xFFFF7AA8),
    quartzTop = QuartzInk.QuartzTop,
    quartzMid = QuartzInk.QuartzMid,
    quartzBase = QuartzInk.QuartzBase,
    quartzRefraction = Color(0xFFCFE4F0),
    cardInk = QuartzInk.CardInk,
    cardInkSecondary = QuartzInk.CardInk.copy(alpha = 0.62f),
    cardInkTertiary = QuartzInk.CardInk.copy(alpha = 0.38f),
    rimSpecular = Color.White,
    grainAlpha = 0.55f,
    accentRose = QuartzInk.RoseQuartz,
    accentCobalt = QuartzInk.MolecularCobalt,
    accentAmber = QuartzInk.WarmAmber,
    accentCrimson = QuartzInk.CrimsonSignal,
    matrixGhost = Color.White.copy(alpha = 0.14f)
)

/** Nocturnal obsidian: the same studio, lights down. */
val ObsidianQuartzPalette = QuartzPalette(
    themeMode = ThemeMode.DARK,
    isDark = true,
    canvas = QuartzInk.ObsidianCanvas,
    canvasSecondary = Color(0xFF050305),
    silhouetteCore = Color(0xFF020103),
    silhouetteBleed = QuartzInk.CrimsonBleed.copy(alpha = 0.34f),
    chromaticRim = QuartzInk.ChromaticRim.copy(alpha = 0.22f),
    inkPrimary = QuartzInk.NocturnalInk.copy(alpha = 0.94f),
    inkSecondary = QuartzInk.NocturnalInk.copy(alpha = 0.62f),
    inkTertiary = QuartzInk.NocturnalInk.copy(alpha = 0.42f),
    inkQuaternary = QuartzInk.NocturnalInk.copy(alpha = 0.24f),
    hairline = Color.White.copy(alpha = 0.13f),
    hairlineStrong = Color.White.copy(alpha = 0.24f),
    velvetCore = QuartzInk.VelvetVoid,
    velvetMid = QuartzInk.VelvetMid,
    velvetRuby = QuartzInk.VelvetRuby,
    velvetEdge = QuartzInk.VelvetEdge,
    velvetRefraction = Color(0xFFFF7AA8),
    quartzTop = QuartzInk.QuartzTop,
    quartzMid = QuartzInk.QuartzMid,
    quartzBase = QuartzInk.QuartzBase,
    quartzRefraction = Color(0xFFCFE4F0),
    cardInk = QuartzInk.CardInk,
    cardInkSecondary = QuartzInk.CardInk.copy(alpha = 0.62f),
    cardInkTertiary = QuartzInk.CardInk.copy(alpha = 0.38f),
    rimSpecular = Color.White,
    grainAlpha = 0.62f,
    accentRose = QuartzInk.RoseQuartz,
    accentCobalt = QuartzInk.MolecularCobalt,
    accentAmber = QuartzInk.WarmAmber,
    accentCrimson = Color(0xFFE84265),
    matrixGhost = Color.White.copy(alpha = 0.12f)
)

/** Merlot nocturne: wine mode's revision of the same studio. */
val MerlotQuartzPalette = ObsidianQuartzPalette.copy(
    themeMode = ThemeMode.WINE,
    canvas = QuartzInk.NocturneCanvas,
    canvasSecondary = Color(0xFF0B0206),
    silhouetteBleed = Color(0xFF8C1D46).copy(alpha = 0.38f),
    velvetEdge = Color(0xFFC2205A),
    accentCrimson = Color(0xFFE84265),
    grainAlpha = 0.58f
)

val LocalQuartzPalette = staticCompositionLocalOf { AlabasterQuartzPalette }

object QuartzTheme {
    val palette: QuartzPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalQuartzPalette.current
}

fun quartzPaletteFor(themeMode: ThemeMode): QuartzPalette = when (themeMode) {
    ThemeMode.LIGHT -> AlabasterQuartzPalette
    ThemeMode.DARK -> ObsidianQuartzPalette
    ThemeMode.WINE -> MerlotQuartzPalette
}

/**
 * Publishes the Quartz palette and re-tints the shared RED tokens, so every modal this style does
 * not own (object detail, moon window, location picker) inherits the same editorial paper and the
 * same crimson accent instead of falling back to the classic vermilion-on-black app.
 */
@Composable
fun QuartzThemeProvider(
    enabled: Boolean,
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val palette = remember(themeMode) { quartzPaletteFor(themeMode) }
    val baseRedColors = RedTheme.colors
    val adaptedRedColors = remember(baseRedColors, palette) {
        baseRedColors.copy(
            background = palette.canvas,
            backgroundSecondary = palette.canvasSecondary,
            backgroundTertiary = palette.canvas,
            surface = if (palette.isDark) palette.velvetCore.copy(alpha = 0.92f) else Color(0xFFF6F2EC),
            surfaceElevated = if (palette.isDark) palette.quartzTop.copy(alpha = 0.96f) else Color(0xFFFFFCF8),
            surfaceGrouped = if (palette.isDark) palette.velvetCore.copy(alpha = 0.88f) else Color(0xFFEDE7DF),
            surfaceVariant = if (palette.isDark) palette.quartzMid.copy(alpha = 0.85f) else Color(0xFFE3DCD2),
            border = palette.hairlineStrong,
            separator = palette.hairline,
            accentRed = palette.accentCrimson,
            accentRedSubtle = palette.accentCrimson.copy(alpha = 0.18f),
            accentGold = palette.accentAmber,
            accentBlue = palette.accentCobalt,
            accentPurple = palette.accentRose,
            navSurface = if (palette.isDark) palette.quartzTop.copy(alpha = 0.94f) else Color(0xFFF7F3ED),
            navBorder = palette.hairlineStrong,
            textPrimary = if (palette.isDark) palette.cardInk else palette.inkPrimary,
            textSecondary = if (palette.isDark) palette.cardInkSecondary else palette.inkSecondary,
            textTertiary = if (palette.isDark) palette.cardInkTertiary else palette.inkTertiary
        )
    }
    CompositionLocalProvider(
        LocalQuartzPalette provides palette,
        LocalRedColors provides adaptedRedColors,
        content = content
    )
}

// ============================================================================
// ANODIZED MICRO-GRAIN (AGSL on API 33+, a tiled noise shader everywhere else)
// ============================================================================

private const val QUARTZ_GRAIN_AGSL = """
    uniform float2 resolution;
    uniform float intensity;
    half4 main(float2 coord) {
        float n = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
        float lit = step(0.5, n);
        float a = abs(n - 0.5) * intensity;
        half3 rgb = lit > 0.5 ? half3(1.0) : half3(0.0);
        return half4(rgb, clamp(a, 0.0, 0.14));
    }
"""

/**
 * The tactile grain. A 128 px tile of fixed-seed noise (so the surface never shimmers between
 * frames) tinted half light, half dark: anodized quartz, not a digital gradient.
 */
internal object QuartzGrain {

    private const val TILE = 128

    private val tile: Bitmap by lazy {
        val pixels = IntArray(TILE * TILE)
        val random = Random(7411L)
        for (i in pixels.indices) {
            val light = random.nextBoolean()
            val alpha = 6 + random.nextInt(22)
            val v = if (light) 255 else 0
            pixels[i] = (alpha shl 24) or (v shl 16) or (v shl 8) or v
        }
        Bitmap.createBitmap(pixels, TILE, TILE, Bitmap.Config.ARGB_8888)
    }

    /** A fresh native paint; [alpha] is the grain's overall strength on this surface. */
    fun paint(alpha: Float): android.graphics.Paint = android.graphics.Paint().apply {
        isAntiAlias = true
        shader = android.graphics.BitmapShader(
            tile,
            android.graphics.Shader.TileMode.REPEAT,
            android.graphics.Shader.TileMode.REPEAT
        )
        this.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    }
}

private fun DrawScope.drawQuartzGrain(
    cornerRadiusPx: Float,
    alpha: Float
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f || alpha <= 0f) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                shader = RuntimeShader(QUARTZ_GRAIN_AGSL).apply {
                    setFloatUniform("resolution", w, h)
                    setFloatUniform("intensity", 1f)
                }
            }
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, w, h,
                cornerRadiusPx, cornerRadiusPx,
                paint
            )
        }
    } else {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, w, h,
                cornerRadiusPx, cornerRadiusPx,
                QuartzGrain.paint(alpha)
            )
        }
    }
}

// ============================================================================
// OPTICAL QUARTZ SURFACE — inner refraction + grain + specular hairline
// ============================================================================

/**
 * The one surface treatment of this design language.
 *
 * Applied *after* the fill and a matching `clip`, it layers:
 *  1. a soft inner-perimeter refraction (an 18.dp light reach on all four edges, so the centre stays
 *     deep and the rim catches studio light) plus, from API 28, a blurred perimeter bloom;
 *  2. anodized micro-grain (AGSL runtime shader on 13+, tiled noise below it);
 *  3. a directional 0.75.dp linear-gradient specular hairline.
 *
 * It never draws an outer bezel or a flat `border(1.dp, White)`.
 */
fun Modifier.opticalQuartzSurface(
    cornerRadius: Dp,
    refractionColor: Color,
    refractionReach: Dp = 18.dp,
    refractionAlpha: Float = 0.26f,
    rimHighlightAlpha: Float = 0.5f,
    grainAlpha: Float = 0.5f,
    bloom: Boolean = true
): Modifier = drawWithCache {
    val radiusPx = cornerRadius.toPx()
    val reachPx = refractionReach.toPx()
    val w = size.width
    val h = size.height

    val horizontalReach = (reachPx / max(w, 1f)).coerceIn(0.03f, 0.42f)
    val verticalReach = (reachPx / max(h, 1f)).coerceIn(0.03f, 0.42f)

    val edgeGlowHorizontal = Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to refractionColor.copy(alpha = refractionAlpha),
            0.55f * horizontalReach to refractionColor.copy(alpha = refractionAlpha * 0.32f),
            horizontalReach to Color.Transparent,
            1f - horizontalReach to Color.Transparent,
            1f - 0.55f * horizontalReach to refractionColor.copy(alpha = refractionAlpha * 0.32f),
            1.0f to refractionColor.copy(alpha = refractionAlpha)
        ),
        startX = 0f,
        endX = w
    )
    val edgeGlowVertical = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to refractionColor.copy(alpha = refractionAlpha * 0.92f),
            0.6f * verticalReach to refractionColor.copy(alpha = refractionAlpha * 0.28f),
            verticalReach to Color.Transparent,
            1f - verticalReach to Color.Transparent,
            1f - 0.6f * verticalReach to refractionColor.copy(alpha = refractionAlpha * 0.28f),
            1.0f to refractionColor.copy(alpha = refractionAlpha * 0.92f)
        ),
        startY = 0f,
        endY = h
    )

    val rimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = (rimHighlightAlpha * 1.6f).coerceAtMost(0.85f)),
            Color.White.copy(alpha = rimHighlightAlpha * 0.16f),
            Color.White.copy(alpha = rimHighlightAlpha * 0.08f),
            Color.White.copy(alpha = rimHighlightAlpha * 0.85f)
        ),
        start = Offset.Zero,
        end = Offset(w, h)
    )
    val rimWidth = 0.85.dp.toPx()

    val bloomPaint = if (bloom && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 20.dp.toPx()
            color = refractionColor.copy(alpha = reflectionBloomAlpha(refractionAlpha)).toArgb()
            maskFilter = BlurMaskFilter(15.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    } else {
        null
    }

    onDrawWithContent {
        drawContent()

        // 1. Inner-edge light refraction (the centre stays deep obsidian).
        drawRoundRect(
            brush = edgeGlowHorizontal,
            cornerRadius = CornerRadius(radiusPx, radiusPx)
        )
        drawRoundRect(
            brush = edgeGlowVertical,
            cornerRadius = CornerRadius(radiusPx, radiusPx)
        )

        // 1b. Blurred perimeter bloom: thick cut quartz catching the key light.
        if (bloomPaint != null) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRoundRect(
                    ripInset,
                    ripInset,
                    size.width - ripInset,
                    size.height - ripInset,
                    radiusPx,
                    radiusPx,
                    bloomPaint
                )
            }
        }

        // 2. Tactile anodized grain.
        drawQuartzGrain(cornerRadiusPx = radiusPx, alpha = grainAlpha)

        // 3. Razor-thin specular rim, brightest top-left.
        drawRoundRect(
            brush = rimBrush,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = Stroke(width = rimWidth)
        )
    }
}

private const val ripInset = 8f

private fun reflectionBloomAlpha(refractionAlpha: Float): Float =
    (refractionAlpha * 0.55f).coerceIn(0.05f, 0.35f)

/**
 * The 6.dp inset dashed perimeter track worn by every capsule pill (and, optionally, by a lens card):
 * the small "precision machined" cue lifted from the reference hardware.
 */
fun Modifier.quartzInnerDottedTrack(
    cornerRadius: Dp,
    color: Color,
    inset: Dp = 6.dp,
    dash: FloatArray = floatArrayOf(3f, 7f)
): Modifier = drawWithCache {
    val insetPx = inset.toPx()
    val radiusPx = cornerRadius.toPx()
    val effect = PathEffect.dashPathEffect(dash, 0f)
    val stroke = 0.8.dp.toPx()
    onDrawWithContent {
        drawContent()
        val insetRadius = (radiusPx - insetPx).coerceAtLeast(0f)
        drawRoundRect(
            color = color,
            topLeft = Offset(insetPx, insetPx),
            size = Size(
                (size.width - insetPx * 2f).coerceAtLeast(0f),
                (size.height - insetPx * 2f).coerceAtLeast(0f)
            ),
            cornerRadius = CornerRadius(insetRadius, insetRadius),
            style = Stroke(width = stroke, pathEffect = effect)
        )
    }
}

// ============================================================================
// PRECISION VECTOR GLYPHS (no icons, no emojis — hairline geometry only)
// ============================================================================

enum class QuartzGlyph {
    ChevronLeft,
    ChevronRight,
    ArrowLeft,
    ArrowRight,
    ArrowUpRight,
    Plus,
    Crosshair,
    Bookmark,
    Search,
    Moon,
    Satellite,
    Flask
}

/** Every micro-icon in this style. Stroke widths stay between 1.0 and 1.3 dp. */
@Composable
fun QuartzVectorGlyph(
    glyph: QuartzGlyph,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.2.dp
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth.toPx()
        val cx = w / 2f
        val cy = h / 2f
        when (glyph) {
            QuartzGlyph.ChevronLeft -> {
                val path = Path().apply {
                    moveTo(w * 0.66f, h * 0.16f)
                    lineTo(w * 0.34f, cy)
                    lineTo(w * 0.66f, h * 0.84f)
                }
                drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round))
            }
            QuartzGlyph.ChevronRight -> {
                val path = Path().apply {
                    moveTo(w * 0.34f, h * 0.16f)
                    lineTo(w * 0.66f, cy)
                    lineTo(w * 0.34f, h * 0.84f)
                }
                drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round))
            }
            QuartzGlyph.ArrowLeft, QuartzGlyph.ArrowRight -> {
                val left = glyph == QuartzGlyph.ArrowLeft
                val tip = if (left) 0f else w
                val tail = if (left) w else 0f
                drawLine(color, Offset(tip, cy), Offset(tail, cy), sw, cap = StrokeCap.Round)
                val headX = if (left) w * 0.32f else w * 0.68f
                drawLine(color, Offset(tip, cy), Offset(headX, cy - h * 0.22f), sw, cap = StrokeCap.Round)
                drawLine(color, Offset(tip, cy), Offset(headX, cy + h * 0.22f), sw, cap = StrokeCap.Round)
            }
            QuartzGlyph.ArrowUpRight -> {
                drawLine(color, Offset(w * 0.18f, h * 0.82f), Offset(w * 0.82f, h * 0.18f), sw, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.44f, h * 0.18f), Offset(w * 0.82f, h * 0.18f), sw, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.82f, h * 0.18f), Offset(w * 0.82f, h * 0.56f), sw, cap = StrokeCap.Round)
            }
            QuartzGlyph.Plus -> {
                drawLine(color, Offset(0f, cy), Offset(w, cy), sw)
                drawLine(color, Offset(cx, 0f), Offset(cx, h), sw)
            }
            QuartzGlyph.Crosshair -> {
                drawCircle(color, radius = w * 0.34f, center = Offset(cx, cy), style = Stroke(width = sw * 0.8f))
                drawCircle(color, radius = w * 0.07f, center = Offset(cx, cy))
                drawLine(color, Offset(cx, 0f), Offset(cx, cy - w * 0.36f), sw * 0.8f)
                drawLine(color, Offset(cx, cy + w * 0.36f), Offset(cx, h), sw * 0.8f)
                drawLine(color, Offset(0f, cy), Offset(cx - w * 0.36f, cy), sw * 0.8f)
                drawLine(color, Offset(cx + w * 0.36f, cy), Offset(w, cy), sw * 0.8f)
            }
            QuartzGlyph.Bookmark -> {
                val path = Path().apply {
                    moveTo(w * 0.26f, h * 0.14f)
                    lineTo(w * 0.74f, h * 0.14f)
                    lineTo(w * 0.74f, h * 0.86f)
                    lineTo(cx, h * 0.66f)
                    lineTo(w * 0.26f, h * 0.86f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round))
            }
            QuartzGlyph.Search -> {
                drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.42f, h * 0.42f), style = Stroke(width = sw))
                drawLine(color, Offset(w * 0.64f, h * 0.64f), Offset(w * 0.88f, h * 0.88f), sw, cap = StrokeCap.Round)
            }
            QuartzGlyph.Moon -> {
                drawCircle(
                    color = color,
                    radius = w * 0.38f,
                    center = Offset(w * 0.46f, h * 0.54f),
                    style = Stroke(width = sw)
                )
                // The terminator: a second, offset hairline ring reads as the shadowed limb.
                drawCircle(
                    color = color.copy(alpha = 0.42f),
                    radius = w * 0.30f,
                    center = Offset(w * 0.62f, h * 0.42f),
                    style = Stroke(width = sw * 0.85f)
                )
            }
            QuartzGlyph.Satellite -> {
                drawLine(color, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.2f), sw, cap = StrokeCap.Round)
                drawRect(
                    color = color,
                    topLeft = Offset(w * 0.46f, h * 0.38f),
                    size = Size(w * 0.2f, h * 0.2f),
                    style = Stroke(width = sw * 0.85f)
                )
                drawLine(color, Offset(w * 0.22f, h * 0.52f), Offset(w * 0.46f, h * 0.52f), sw * 0.85f)
                drawLine(color, Offset(w * 0.22f, h * 0.68f), Offset(w * 0.46f, h * 0.62f), sw * 0.85f)
                drawLine(color, Offset(w * 0.78f, h * 0.48f), Offset(w * 0.56f, h * 0.48f), sw * 0.85f)
                drawLine(color, Offset(w * 0.78f, h * 0.32f), Offset(w * 0.56f, h * 0.38f), sw * 0.85f)
            }
            QuartzGlyph.Flask -> {
                val path = Path().apply {
                    moveTo(w * 0.4f, h * 0.14f)
                    lineTo(w * 0.6f, h * 0.14f)
                    lineTo(w * 0.6f, h * 0.4f)
                    lineTo(w * 0.84f, h * 0.86f)
                    lineTo(w * 0.16f, h * 0.86f)
                    lineTo(w * 0.4f, h * 0.4f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = sw))
                drawLine(color, Offset(w * 0.34f, h * 0.6f), Offset(w * 0.66f, h * 0.6f), sw * 0.8f)
            }
        }
    }
}

/** The glowing Nothing-style status dot: solid core + 2 halo rings. */
@Composable
fun QuartzSignalDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
    glowing: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        if (glowing) {
            drawCircle(color.copy(alpha = 0.20f), radius = r)
            drawCircle(color.copy(alpha = 0.42f), radius = r * 0.66f)
        }
        drawCircle(color, radius = r * 0.38f)
    }
}

/** A 0.75.dp hairline that fades out at both ends. The only divider this style uses. */
@Composable
fun QuartzHairline(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawLine(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.12f to color,
                0.88f to color,
                1f to Color.Transparent
            ),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 0.75.dp.toPx()
        )
    }
}

/** A circled hairline affordance with a vector glyph inside (38.dp by default). */
@Composable
fun QuartzGlyphDisc(
    glyph: QuartzGlyph,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 38.dp,
    glyphSize: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(diameter)
            .drawWithCache {
                val stroke = 0.8.dp.toPx()
                onDrawBehind {
                    drawCircle(color = color.copy(alpha = 0.28f), style = Stroke(width = stroke))
                    drawCircle(
                        brush = Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.10f),
                            1f to Color.Transparent
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        QuartzVectorGlyph(
            glyph = glyph,
            color = color,
            modifier = Modifier.size(glyphSize),
            strokeWidth = 1.1.dp
        )
    }
}

/** The ←  ━  → micro-stepper of the reference hardware, drawn entirely from hairlines. */
@Composable
fun QuartzMicroStepper(
    color: Color,
    enabledDecrement: Boolean,
    enabledIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .quartzTapTarget(onDecrement, enabledDecrement),
            contentAlignment = Alignment.Center
        ) {
            QuartzVectorGlyph(
                glyph = QuartzGlyph.ArrowLeft,
                color = color.copy(alpha = if (enabledDecrement) 0.86f else 0.22f),
                modifier = Modifier.size(13.dp, 9.dp),
                strokeWidth = 1.2.dp
            )
        }
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 2.2.dp)
                .drawWithCache {
                    onDrawBehind {
                        drawRoundRect(
                            color = color.copy(alpha = 0.9f),
                            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                }
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .quartzTapTarget(onIncrement, enabledIncrement),
            contentAlignment = Alignment.Center
        ) {
            QuartzVectorGlyph(
                glyph = QuartzGlyph.ArrowRight,
                color = color.copy(alpha = if (enabledIncrement) 0.86f else 0.22f),
                modifier = Modifier.size(13.dp, 9.dp),
                strokeWidth = 1.2.dp
            )
        }
    }
}

/** Press-through clickable with no ripple: this style answers with haptics and a spring, not ink. */
fun Modifier.quartzTapTarget(
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    enabled = enabled,
    onClick = onClick
)
