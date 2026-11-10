package com.alijafari.red.astronomy.ui.obsidian

import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.LocalRedColors
import com.alijafari.red.astronomy.ui.theme.RedTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// OBSIDIAN QUARTZ & CRIMSON TELEMETRY — DESIGN SYSTEM
// (Industrial-design concept: Teenage Engineering x Nothing x Apple Design Team)
//
// One unified atmospheric mood, never a "dashboard color salad":
//  - Editorial Alabaster & Obsidian Silhouette canvas (`#E8E4DE` in Light mode)
//  - Velvet Crimson Quartz lens (`#170109` void -> `#8C093B` -> `#B81450` rim)
//  - Smoked Slate Quartz (`#1D242B` -> `#5D7687`)
//  - Localized capsule accents only: Rose `#D95B8A`, Cobalt `#6B85B8`, Amber `#D97236`
//
// Anti-slop rules (see docs/OBSIDIAN_QUARTZ_UI.md):
//  - No emojis, no default Material icons, no fake monospace fonts. Every
//    micro-icon is drawn mathematically on Canvas with 1.1-1.3.dp hairlines.
//  - Every card/capsule wears `Modifier.opticalQuartzSurface()` — never a flat
//    `Modifier.border(1.dp, ...)`.
//  - Motion is critically damped precision hardware springs (0.84 / 280).
// ============================================================================

/** Precision hardware spring damping shared by every Obsidian waveform morph. */
const val OBSIDIAN_SPRING_DAMPING = 0.84f

/** Precision hardware spring stiffness shared by every Obsidian waveform morph. */
const val OBSIDIAN_SPRING_STIFFNESS = 280f

/** Velvet Crimson Quartz — the hero lens accent (`#B81450` illuminated perimeter). */
val ObsidianCrimson = Color(0xFFB81450)

/** Deep Velvet Crimson — signal / sync ink (`#9E0B45`). */
val ObsidianDeepCrimson = Color(0xFF9E0B45)

/** Smoked Slate Quartz mid-tone (`#1D242B` void -> `#5D7687` rim), at the palette's 82% opacity. */
val ObsidianSmokedQuartz = Color(0xFF1D242B)
val ObsidianSlateRim = Color(0xFF5D7687)

/** Rose Quartz capsule accent. */
val ObsidianRose = Color(0xFFD95B8A)

/** Molecular Cobalt capsule accent. */
val ObsidianCobalt = Color(0xFF6B85B8)

/** Warm Amber capsule accent. */
val ObsidianAmber = Color(0xFFD97236)

/** Editorial Alabaster Mist canvas. */
val ObsidianAlabaster = Color(0xFFE8E4DE)

@Immutable
data class ObsidianPalette(
    val themeMode: ThemeMode,
    val isDark: Boolean,
    val isWine: Boolean,
    /** Background canvas fill behind the editorial silhouette. */
    val canvasBg: Color,
    /** Obsidian silhouette ink rising through the canvas. */
    val silhouetteInk: Color,
    /** Typography hierarchy (ultra-thin / light geometric styling). */
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Smoked frosted quartz fill for secondary surfaces. */
    val glassFill: Color,
    /** Fine specular hairline colour (drawn as a 0.85.dp gradient rim). */
    val glassBorder: Color,
    /** Nothing-style live signal dot. */
    val signalDot: Color,
    val accentCrimson: Color,
    val accentRose: Color,
    val accentCobalt: Color,
    val accentAmber: Color,
    /** Dot-Matrix counter ink & unlit matrix ghost ink. */
    val matrixLit: Color,
    val matrixGhost: Color
)

/** Light identity: Editorial Alabaster Mist with the Obsidian silhouette. */
val LightObsidianPalette = ObsidianPalette(
    themeMode = ThemeMode.LIGHT,
    isDark = false,
    isWine = false,
    canvasBg = ObsidianAlabaster,
    silhouetteInk = Color(0xFF090407),
    textPrimary = Color(0xFF1A1417),
    textSecondary = Color(0xFF4A3F45),
    textMuted = Color(0xFF8A7D84),
    glassFill = Color(0xFF283845).copy(alpha = 0.82f),
    glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.48f),
    signalDot = ObsidianDeepCrimson,
    accentCrimson = ObsidianCrimson,
    accentRose = ObsidianRose,
    accentCobalt = ObsidianCobalt,
    accentAmber = ObsidianAmber,
    matrixLit = Color(0xFFFFFFFF),
    matrixGhost = Color(0x18FFFFFF)
)

/** Dark identity: the same crimson lens over a deep obsidian void. */
val DarkObsidianPalette = ObsidianPalette(
    themeMode = ThemeMode.DARK,
    isDark = true,
    isWine = false,
    canvasBg = Color(0xFF0B0206),
    silhouetteInk = Color(0xFF000000),
    textPrimary = Color(0xFFFFF2F5),
    textSecondary = Color(0xCCF7D9E0),
    textMuted = Color(0x88F2C4D0),
    glassFill = Color(0xFF1D242B).copy(alpha = 0.86f),
    glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.48f),
    signalDot = Color(0xFFFF3864),
    accentCrimson = ObsidianCrimson,
    accentRose = ObsidianRose,
    accentCobalt = ObsidianCobalt,
    accentAmber = ObsidianAmber,
    matrixLit = Color(0xFFFFEEF2),
    matrixGhost = Color(0x22FF8EA6)
)

/** Wine identity: cabernet-obsidian canvas, same velvet crimson lens. */
val WineObsidianPalette = ObsidianPalette(
    themeMode = ThemeMode.WINE,
    isDark = true,
    isWine = true,
    canvasBg = Color(0xFF140309),
    silhouetteInk = Color(0xFF050102),
    textPrimary = Color(0xFFFFF2F5),
    textSecondary = Color(0xCCF7D9E0),
    textMuted = Color(0x88F2C4D0),
    glassFill = Color(0xFF2A0A16).copy(alpha = 0.86f),
    glassBorder = Color(0xFFFF9EB5).copy(alpha = 0.42f),
    signalDot = Color(0xFFFF3864),
    accentCrimson = ObsidianCrimson,
    accentRose = ObsidianRose,
    accentCobalt = ObsidianCobalt,
    accentAmber = ObsidianAmber,
    matrixLit = Color(0xFFFFEEF2),
    matrixGhost = Color(0x22FF8EA6)
)

val LocalObsidianPalette = staticCompositionLocalOf { DarkObsidianPalette }

object ObsidianTheme {
    val palette: ObsidianPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalObsidianPalette.current
}

fun obsidianPaletteFor(themeMode: ThemeMode): ObsidianPalette = when (themeMode) {
    ThemeMode.LIGHT -> LightObsidianPalette
    ThemeMode.DARK -> DarkObsidianPalette
    ThemeMode.WINE -> WineObsidianPalette
}

/**
 * Provides Obsidian Quartz tokens and harmonizes the shared RedColors (dialogs, modals,
 * immersive chrome) when `AppUiStyle.OBSIDIAN` is active, mirroring the Cyber/Spatial
 * providers. The Velvet Crimson lens identity is mode-independent; only the canvas
 * and ink adapt to [ThemeMode].
 */
@Composable
fun ObsidianThemeProvider(
    enabled: Boolean,
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val palette = remember(themeMode) { obsidianPaletteFor(themeMode) }
    val adaptedRedColors = remember(themeMode) {
        val baseRedColors = RedTheme.colors
        when (themeMode) {
            ThemeMode.LIGHT -> baseRedColors.copy(
                isDark = false,
                background = ObsidianAlabaster,
                backgroundSecondary = Color(0xFFEFEBE4),
                backgroundTertiary = Color(0xFFF5F2EC),
                surface = Color(0xEBF2EEE7),
                surfaceElevated = Color(0xF7F5F1EA),
                surfaceGrouped = Color(0xE6EAE4DC),
                surfaceVariant = Color(0xCC283845),
                border = Color(0x38FFFFFF),
                separator = Color(0x24FFFFFF),
                accentRed = ObsidianDeepCrimson,
                accentRedSubtle = Color(0x29B81450),
                accentGold = ObsidianAmber,
                accentBlue = ObsidianCobalt,
                accentPurple = ObsidianRose,
                navSurface = Color(0xE61D242B),
                navBorder = Color(0x40FFFFFF)
            )
            ThemeMode.DARK -> baseRedColors.copy(
                isDark = true,
                background = Color(0xFF0B0206),
                backgroundSecondary = Color(0xFF170109),
                backgroundTertiary = Color(0xFF240712),
                surface = Color(0xEB1D242B),
                surfaceElevated = Color(0xF0283845),
                surfaceGrouped = Color(0xE01D242B),
                surfaceVariant = Color(0xCC283845),
                border = Color(0x38FFFFFF),
                separator = Color(0x24FFFFFF),
                accentRed = ObsidianCrimson,
                accentRedSubtle = Color(0x33B81450),
                accentGold = ObsidianAmber,
                accentBlue = ObsidianCobalt,
                accentPurple = ObsidianRose,
                navSurface = Color(0xE61D242B),
                navBorder = Color(0x40FFFFFF)
            )
            ThemeMode.WINE -> baseRedColors.copy(
                isDark = true,
                background = Color(0xFF140309),
                backgroundSecondary = Color(0xFF1F0710),
                backgroundTertiary = Color(0xFF2B0B17),
                surface = Color(0xEB2A0A16),
                surfaceElevated = Color(0xF036101E),
                surfaceGrouped = Color(0xE0210811),
                surfaceVariant = Color(0xCC3A101E),
                border = Color(0x38FF9EB5),
                separator = Color(0x24FF9EB5),
                accentRed = ObsidianCrimson,
                accentRedSubtle = Color(0x33B81450),
                accentGold = ObsidianAmber,
                accentBlue = ObsidianCobalt,
                accentPurple = ObsidianRose,
                navSurface = Color(0xE61D0A14),
                navBorder = Color(0x40FF9EB5)
            )
        }
    }

    CompositionLocalProvider(
        LocalObsidianPalette provides palette,
        LocalRedColors provides adaptedRedColors,
        content = content
    )
}

// ============================================================================
// OPTICAL QUARTZ SURFACE — INNER REFRACTION & AGSL FROSTED GRAIN
// ============================================================================

private const val FROST_NOISE_AGSL = """
    uniform float2 resolution;
    uniform float intensity;
    half4 main(float2 coord) {
        float2 uv = coord / resolution;
        float noise = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
        float alpha = (noise - 0.5) * intensity;
        return half4(1.0, 1.0, 1.0, clamp(alpha, 0.0, 0.08));
    }
"""

/**
 * The one surface recipe every Obsidian card and capsule wears (never a flat border):
 * 1. An 18.dp blurred inner-perimeter refraction glow (`BlurMaskFilter(16.dp, NORMAL)`)
 *    — light bleeding ~14.dp inside the rounded perimeter while the centre stays deep.
 * 2. An Android 13+ AGSL `RuntimeShader` tactile frosted micro-grain overlay.
 * 3. A directional 0.85.dp linear-gradient specular rim hairline.
 */
fun Modifier.opticalQuartzSurface(
    cornerRadius: Dp,
    innerRefractionColor: Color = Color.White.copy(alpha = 0.22f),
    rimHighlightAlpha: Float = 0.48f
): Modifier = this.drawWithCache {
    val radiusPx = cornerRadius.toPx()

    // Soft inner edge bloom (simulates thick cut quartz refracting studio light)
    val edgeBloomPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 18.dp.toPx()
        color = innerRefractionColor.toArgb()
        maskFilter = BlurMaskFilter(16.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    // Directional 0.85dp hairline specular rim
    val rimBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = rimHighlightAlpha),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = rimHighlightAlpha * 0.55f)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    // Tactile frosted micro-grain shader (Android 13+); created once, uniforms set per draw.
    val grainShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuntimeShader(FROST_NOISE_AGSL)
    } else {
        null
    }

    onDrawWithContent {
        drawContent()

        // 1. Inner perimeter refraction glow
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                radiusPx, radiusPx, edgeBloomPaint
            )
        }

        // 2. Subtle AGSL tactile micro-grain on Android 13+
        if (grainShader != null) {
            grainShader.setFloatUniform("resolution", size.width, size.height)
            grainShader.setFloatUniform("intensity", 0.09f)
            drawRoundRect(
                brush = ShaderBrush(grainShader),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        }

        // 3. Razor-thin specular rim border
        drawRoundRect(
            brush = rimBrush,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = Stroke(width = 0.85.dp.toPx())
        )
    }
}

// ============================================================================
// VECTOR MICRO-ICONS — EVERY GLYPH DRAWN MATHEMATICALLY ON CANVAS
// (No emojis, no `Icons.Default.*`, 1.1-1.3.dp hairlines)
// ============================================================================

/** Every micro-icon the Obsidian language is allowed to draw. */
enum class ObsidianVectorIcon {
    LAB,
    AR,
    SATELLITE,
    HOME,
    SETTINGS,
    LANGUAGE,
    CALENDAR,
    PALETTE,
    SKY,
    BACKDROP,
    BORTLE,
    CHECK,
    EXPAND,
    ARROW_LEFT,
    ARROW_RIGHT,
    ARROW_DIAGONAL,
    PLUS,
    CROSSHAIR
}

/**
 * Draws an [ObsidianVectorIcon] as precision hairline geometry on a Canvas.
 * Stroke width defaults to the 1.2.dp hardware hairline.
 */
@Composable
fun ObsidianVectorIconGlyph(
    icon: ObsidianVectorIcon,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    strokeWidth: Dp = 1.2.dp
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth.toPx()
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) / 2f

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(x1, y1), Offset(x2, y2), sw, StrokeCap.Round)
        }

        fun circle(radius: Float, center: Offset = Offset(cx, cy)) {
            drawCircle(tint, radius, center, style = Stroke(sw))
        }

        fun dot(radius: Float, center: Offset) {
            drawCircle(tint, radius, center)
        }

        when (icon) {
            ObsidianVectorIcon.LAB -> {
                // Precision crosshair with an orbital ring — the physics lab mark.
                circle(r * 0.62f)
                line(cx - r, cy, cx - r * 0.62f, cy)
                line(cx + r * 0.62f, cy, cx + r, cy)
                line(cx, cy - r, cx, cy - r * 0.62f)
                line(cx, cy + r * 0.62f, cx, cy + r)
                dot(r * 0.14f, Offset(cx, cy))
            }
            ObsidianVectorIcon.AR -> {
                // Compass diamond with a north tick.
                val diamond = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.72f, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.72f, cy)
                    close()
                }
                drawPath(diamond, tint, style = Stroke(sw, join = StrokeJoin.Round))
                dot(r * 0.13f, Offset(cx, cy))
                line(cx, cy - r * 0.45f, cx, cy - r * 0.22f)
            }
            ObsidianVectorIcon.SATELLITE -> {
                // Body, twin solar panels, antenna.
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - r * 0.22f, cy - r * 0.22f),
                    size = Size(r * 0.44f, r * 0.44f),
                    cornerRadius = CornerRadius(r * 0.08f, r * 0.08f),
                    style = Stroke(sw)
                )
                line(cx - r * 0.22f, cy, cx - r * 0.95f, cy)
                line(cx + r * 0.22f, cy, cx + r * 0.95f, cy)
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - r * 0.95f, cy - r * 0.3f),
                    size = Size(r * 0.34f, r * 0.6f),
                    cornerRadius = CornerRadius(r * 0.06f, r * 0.06f),
                    style = Stroke(sw)
                )
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx + r * 0.61f, cy - r * 0.3f),
                    size = Size(r * 0.34f, r * 0.6f),
                    cornerRadius = CornerRadius(r * 0.06f, r * 0.06f),
                    style = Stroke(sw)
                )
                line(cx, cy - r * 0.22f, cx, cy - r * 0.7f)
                dot(r * 0.1f, Offset(cx, cy - r * 0.82f))
            }
            ObsidianVectorIcon.HOME -> {
                // House outline: roof chevron + walls.
                val roof = Path().apply {
                    moveTo(cx - r * 0.85f, cy - r * 0.05f)
                    lineTo(cx, cy - r * 0.85f)
                    lineTo(cx + r * 0.85f, cy - r * 0.05f)
                }
                drawPath(roof, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                line(cx - r * 0.6f, cy - r * 0.18f, cx - r * 0.6f, cy + r * 0.85f)
                line(cx + r * 0.6f, cy - r * 0.18f, cx + r * 0.6f, cy + r * 0.85f)
                line(cx - r * 0.6f, cy + r * 0.85f, cx + r * 0.6f, cy + r * 0.85f)
            }
            ObsidianVectorIcon.SETTINGS -> {
                // Gear: hub circle + eight spokes.
                circle(r * 0.34f)
                for (i in 0 until 8) {
                    val rad = (i * PI / 4.0).toFloat()
                    val dx = cos(rad)
                    val dy = sin(rad)
                    line(
                        cx + dx * r * 0.52f, cy + dy * r * 0.52f,
                        cx + dx * r * 0.92f, cy + dy * r * 0.92f
                    )
                }
            }
            ObsidianVectorIcon.LANGUAGE -> {
                // Globe: meridian circle + equator + polar axis.
                circle(r * 0.85f)
                drawArc(
                    tint, 90f, 180f, false,
                    topLeft = Offset(cx - r * 0.42f, cy - r * 0.85f),
                    size = Size(r * 0.84f, r * 1.7f),
                    style = Stroke(sw)
                )
                line(cx - r * 0.85f, cy, cx + r * 0.85f, cy)
            }
            ObsidianVectorIcon.CALENDAR -> {
                // Calendar card: binder ticks + header rule.
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - r * 0.8f, cy - r * 0.7f),
                    size = Size(r * 1.6f, r * 1.5f),
                    cornerRadius = CornerRadius(r * 0.14f, r * 0.14f),
                    style = Stroke(sw)
                )
                line(cx - r * 0.8f, cy - r * 0.25f, cx + r * 0.8f, cy - r * 0.25f)
                line(cx - r * 0.4f, cy - r * 0.95f, cx - r * 0.4f, cy - r * 0.5f)
                line(cx + r * 0.4f, cy - r * 0.95f, cx + r * 0.4f, cy - r * 0.5f)
            }
            ObsidianVectorIcon.PALETTE -> {
                // Palette disc with three pigment dots.
                circle(r * 0.85f)
                dot(r * 0.13f, Offset(cx - r * 0.4f, cy - r * 0.25f))
                dot(r * 0.13f, Offset(cx + r * 0.35f, cy - r * 0.4f))
                dot(r * 0.13f, Offset(cx + r * 0.1f, cy + r * 0.35f))
            }
            ObsidianVectorIcon.SKY -> {
                // Crescent arc + star field dots.
                drawArc(
                    tint, 115f, 230f, false,
                    topLeft = Offset(cx - r * 0.85f, cy - r * 0.85f),
                    size = Size(r * 1.7f, r * 1.7f),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
                dot(r * 0.12f, Offset(cx + r * 0.45f, cy - r * 0.45f))
                dot(r * 0.1f, Offset(cx + r * 0.7f, cy + r * 0.1f))
                dot(r * 0.09f, Offset(cx + r * 0.3f, cy + r * 0.55f))
            }
            ObsidianVectorIcon.BACKDROP -> {
                // Framed backdrop with a mountain polyline.
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - r * 0.9f, cy - r * 0.7f),
                    size = Size(r * 1.8f, r * 1.4f),
                    cornerRadius = CornerRadius(r * 0.12f, r * 0.12f),
                    style = Stroke(sw)
                )
                val ridge = Path().apply {
                    moveTo(cx - r * 0.9f, cy + r * 0.7f)
                    lineTo(cx - r * 0.35f, cy - r * 0.05f)
                    lineTo(cx + r * 0.05f, cy + r * 0.35f)
                    lineTo(cx + r * 0.45f, cy - r * 0.2f)
                    lineTo(cx + r * 0.9f, cy + r * 0.7f)
                }
                drawPath(ridge, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ObsidianVectorIcon.BORTLE -> {
                // Five ascending micro-dots — the dark-sky scale.
                val radii = floatArrayOf(0.08f, 0.11f, 0.14f, 0.17f, 0.2f)
                for (i in radii.indices) {
                    val x = cx - r * 0.72f + i * r * 0.36f
                    dot(r * radii[i], Offset(x, cy + r * 0.25f - i * r * 0.12f))
                }
            }
            ObsidianVectorIcon.CHECK -> {
                val check = Path().apply {
                    moveTo(cx - r * 0.7f, cy + r * 0.05f)
                    lineTo(cx - r * 0.15f, cy + r * 0.6f)
                    lineTo(cx + r * 0.75f, cy - r * 0.55f)
                }
                drawPath(check, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ObsidianVectorIcon.EXPAND -> {
                val chevron = Path().apply {
                    moveTo(cx - r * 0.6f, cy - r * 0.25f)
                    lineTo(cx, cy + r * 0.35f)
                    lineTo(cx + r * 0.6f, cy - r * 0.25f)
                }
                drawPath(chevron, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            ObsidianVectorIcon.ARROW_LEFT -> {
                line(0f, cy, w, cy)
                line(0f, cy, r * 0.5f, r * 0.12f)
                line(0f, cy, r * 0.5f, h - r * 0.12f)
            }
            ObsidianVectorIcon.ARROW_RIGHT -> {
                line(0f, cy, w, cy)
                line(w, cy, w - r * 0.5f, r * 0.12f)
                line(w, cy, w - r * 0.5f, h - r * 0.12f)
            }
            ObsidianVectorIcon.ARROW_DIAGONAL -> {
                line(0f, h, w, 0f)
                line(w * 0.3f, 0f, w, 0f)
                line(w, 0f, w, h * 0.7f)
            }
            ObsidianVectorIcon.PLUS -> {
                line(0f, cy, w, cy)
                line(cx, 0f, cx, h)
            }
            ObsidianVectorIcon.CROSSHAIR -> {
                circle(r * 0.55f)
                line(cx - r, cy, cx - r * 0.8f, cy)
                line(cx + r * 0.8f, cy, cx + r, cy)
                line(cx, cy - r, cx, cy - r * 0.8f)
                line(cx, cy + r * 0.8f, cx, cy + r)
                dot(r * 0.12f, Offset(cx, cy))
            }
        }
    }
}

/** Nothing-style live signal dot (filled, optionally haloed). */
@Composable
fun ObsidianSignalDot(
    modifier: Modifier = Modifier,
    color: Color = ObsidianTheme.palette.signalDot,
    glowing: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (glowing) {
            Box(
                modifier = Modifier
                    .size(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
            )
        }
    }
}

// ============================================================================
// HAIRLINE SLIDER — 0.85.dp track, crimson loupe thumb (Bortle / telemetry)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianHairlineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    accentColor: Color = ObsidianTheme.palette.accentCrimson
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val active = isPressed || isDragged

    val thumbScale by animateFloatAsState(
        targetValue = if (active) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = OBSIDIAN_SPRING_DAMPING,
            stiffness = OBSIDIAN_SPRING_STIFFNESS
        ),
        label = "ObsidianSliderThumb"
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            thumbColor = Color.Transparent,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        ),
        thumb = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .scale(thumbScale)
            ) {
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f),
                        radius = 12.dp.toPx()
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = 12.dp.toPx(),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.6.dp.toPx()
                    )
                }
            }
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val trackY = size.height / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(0f, trackY),
                    end = Offset(size.width, trackY),
                    strokeWidth = 0.85.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                )
                val span = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                val fraction = if (span > 0f) {
                    ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val activeWidth = size.width * fraction
                if (activeWidth > 1f) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.9f),
                        start = Offset(0f, trackY),
                        end = Offset(activeWidth, trackY),
                        strokeWidth = 1.35.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                // Hairline end ticks.
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(0f, trackY - 3.dp.toPx()),
                    end = Offset(0f, trackY + 3.dp.toPx()),
                    strokeWidth = 0.8.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(size.width, trackY - 3.dp.toPx()),
                    end = Offset(size.width, trackY + 3.dp.toPx()),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
        }
    )
}

// ============================================================================
// EDITORIAL MICRO-TYPOGRAPHY — hairline tracking, never a fake monospace
// ============================================================================

/**
 * The one label style the Obsidian language uses for eyebrow text ("TELEMETRY // 06.12",
 * "DARKNESS CLASS", "AVAILABLE SCIENTIFIC TOOLS"). Medium weight, wide tracking, low alpha —
 * engineering annotation, not a heading.
 */
@Composable
fun ObsidianEyebrowLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.55f),
    fontSize: androidx.compose.ui.unit.TextUnit = 10.sp
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
        maxLines = 1
    )
}

/**
 * A hairline telemetry row: a 1.1.dp vector glyph, an eyebrow label, and an optional right-hand
 * readout. Used above sections so a screen reads as instrumentation rather than a settings list.
 */
@Composable
fun ObsidianTelemetryRow(
    icon: ObsidianVectorIcon,
    label: String,
    value: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.55f)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ObsidianVectorIconGlyph(
                icon = icon,
                modifier = Modifier.size(11.dp),
                tint = tint,
                strokeWidth = 1.1.dp
            )
            ObsidianEyebrowLabel(text = label, color = tint)
        }
        if (value != null) {
            ObsidianEyebrowLabel(text = value, color = tint)
        }
    }
}
