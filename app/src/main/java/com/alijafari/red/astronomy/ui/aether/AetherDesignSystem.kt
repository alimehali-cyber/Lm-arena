package com.alijafari.red.astronomy.ui.aether

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.LocalRedColors
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// 1. DESIGN SYSTEM SPECIFICATION: "AETHER TELEMETRY UI"
// (Edge-Lit Frameless Optical Lenses, Jewel Radial Telemetry & Spatial Blur)
// ============================================================================

/** Base screen background underneath breathing ambient mesh lights. */
val AetherCanvasVoid = Color(0xFF08080C)

/** Center dark void of the Jewel Telemetry Card (19.7:1 high text contrast). */
val AetherCrimsonCoreCenter = Color(0xFF1C020D)

/** Mid-ring radial transition of the Jewel Telemetry Card. */
val AetherCrimsonCoreMid = Color(0xFF5E052A)

/** Outer edge inner glow of the Jewel Telemetry Card. */
val AetherCrimsonCoreEdge = Color(0xFF9E0B45)

/** Translucent 8% white fill for spatial blurred cards. */
val AetherFrostedGlassFill = Color(0x14FFFFFF)

/** Top-left 1.dp specular rim highlight (40% White). */
val AetherSpecularRimStart = Color(0x66FFFFFF)

/** Center 1.dp border fade (5% White). */
val AetherSpecularRimMid = Color(0x0DFFFFFF)

/** Bottom-right 1.dp rim bounce reflection (20% White). */
val AetherSpecularRimEnd = Color(0x33FFFFFF)

/** Active LED dots, oscilloscope troughs, primary numbers. */
val AetherGlowWhite = Color(0xFFFFFFFF)

/** Secondary labels, inactive wave baselines, inactive ticks (50% White). */
val AetherMutedTelemetry = Color(0x80FFFFFF)

/** Signature rose lightning accent inside the Floating Status Capsule. */
val AetherStatusBoltPink = Color(0xFFFFB4D2)

/** Corner geometry constants (Section 1.B — Frameless Squircle System). */
object AetherCornerGeometry {
    val heroCard: Dp = 36.dp
    val dialCard: Dp = 34.dp
    val statusCapsule: Dp = 100.dp
}

/**
 * Deep Jewel-Tone radial gradient presets for Hero Telemetry Cards (Section 1.A & Section 6.1):
 * Near-black in the center (`#1C020D`, `#031724`, `#041E15`) for crisp text readability,
 * glowing richly around the inner perimeter (`#9E0B45`, `#0E5A8A`, `#0D7A5F`).
 */
enum class AetherJewelPreset(
    val centerDark: Color,
    val midDark: Color,
    val edgeDark: Color,
    val centerWine: Color,
    val midWine: Color,
    val edgeWine: Color,
    val centerLight: Color,
    val midLight: Color,
    val edgeLight: Color
) {
    CRIMSON(
        centerDark = AetherCrimsonCoreCenter,
        midDark = AetherCrimsonCoreMid,
        edgeDark = AetherCrimsonCoreEdge,
        centerWine = Color(0xFF1A020B),
        midWine = Color(0xFF6B072D),
        edgeWine = Color(0xFFB5104E),
        centerLight = Color(0xFF2B0516),
        midLight = Color(0xFF750B38),
        edgeLight = Color(0xFFB31454)
    ),
    SAPPHIRE(
        centerDark = Color(0xFF031724),
        midDark = Color(0xFF073554),
        edgeDark = Color(0xFF0E5A8A),
        centerWine = Color(0xFF180412),
        midWine = Color(0xFF4D0D2E),
        edgeWine = Color(0xFF8C1952),
        centerLight = Color(0xFF062133),
        midLight = Color(0xFF0C466E),
        edgeLight = Color(0xFF146DA6)
    ),
    EMERALD(
        centerDark = Color(0xFF041E15),
        midDark = Color(0xFF084736),
        edgeDark = Color(0xFF0D7A5F),
        centerWine = Color(0xFF1C0510),
        midWine = Color(0xFF5C0E2B),
        edgeWine = Color(0xFF9C1949),
        centerLight = Color(0xFF072B1F),
        midLight = Color(0xFF0D5944),
        edgeLight = Color(0xFF148C6E)
    );

    fun colorsFor(themeMode: ThemeMode): List<Color> = when (themeMode) {
        ThemeMode.WINE -> listOf(centerWine, midWine, edgeWine)
        ThemeMode.LIGHT -> listOf(centerLight, midLight, edgeLight)
        ThemeMode.DARK -> listOf(centerDark, midDark, edgeDark)
    }

    fun edgeGlowFor(themeMode: ThemeMode): Color = when (themeMode) {
        ThemeMode.WINE -> edgeWine
        ThemeMode.LIGHT -> edgeLight
        ThemeMode.DARK -> edgeDark
    }
}

/**
 * Glowing Mesh Pill radial gradient presets (Section 3 & Section 5 Component #6).
 */
enum class AetherMeshPillPreset(
    val darkColors: List<Color>,
    val wineColors: List<Color>,
    val lightColors: List<Color>
) {
    /** Exact Pink Mesh Pill (`Flasks/Day` style: `#D96B98` -> `#32233B`). */
    PINK_ROSE(
        darkColors = listOf(Color(0xFFD96B98), Color(0xFF32233B)),
        wineColors = listOf(Color(0xFFE0487B), Color(0xFF380B1C)),
        lightColors = listOf(Color(0xFFC94B80), Color(0xFF472549))
    ),

    /** Exact Amber Mesh Pill (`Hydration` style: `#E07A42` -> `#3D1D14`). */
    AMBER_COPPER(
        darkColors = listOf(Color(0xFFE07A42), Color(0xFF3D1D14)),
        wineColors = listOf(Color(0xFFD96E48), Color(0xFF3B1018)),
        lightColors = listOf(Color(0xFFD1652C), Color(0xFF4D2318))
    ),

    /** Celestial Sapphire-Cyan Mesh Pill (`#45A8E0` -> `#15283D`). */
    CYAN_SAPPHIRE(
        darkColors = listOf(Color(0xFF45A8E0), Color(0xFF15283D)),
        wineColors = listOf(Color(0xFFB84582), Color(0xFF2E0B1E)),
        lightColors = listOf(Color(0xFF2885BF), Color(0xFF1A334D))
    ),

    /** Emerald-Teal Telemetry Mesh Pill (`#34B894` -> `#113028`). */
    EMERALD_MINT(
        darkColors = listOf(Color(0xFF34B894), Color(0xFF113028)),
        wineColors = listOf(Color(0xFFD13B67), Color(0xFF330917)),
        lightColors = listOf(Color(0xFF1F9978), Color(0xFF163D32))
    );

    fun colorsFor(themeMode: ThemeMode): List<Color> = when (themeMode) {
        ThemeMode.WINE -> wineColors
        ThemeMode.LIGHT -> lightColors
        ThemeMode.DARK -> darkColors
    }
}

@Immutable
data class AetherPalette(
    val themeMode: ThemeMode,
    val isDark: Boolean,
    val isWine: Boolean,
    val canvasVoid: Color,
    val frostedGlassFill: Color,
    val frostedOpticalVeil: Color,
    val specularRimStart: Color,
    val specularRimMid: Color,
    val specularRimCenter: Color,
    val specularRimEnd: Color,
    val glowWhite: Color,
    val mutedTelemetry: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMicro: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val sapphireAccent: Color,
    val statusOptimal: Color,
    val statusWarn: Color,
    val dialWarmTint: Color,
    val dialCoolTint: Color,
    val ambientBlobPrimary: Color,
    val ambientBlobSecondary: Color,
    val dotMatrixGhost: Color
)

val DarkAetherPalette = AetherPalette(
    themeMode = ThemeMode.DARK,
    isDark = true,
    isWine = false,
    canvasVoid = AetherCanvasVoid,                         // #08080C
    frostedGlassFill = AetherFrostedGlassFill,             // #14FFFFFF (8% White)
    frostedOpticalVeil = Color(0x660B0C14),                // Crisp legibility over bright live skies
    specularRimStart = Color(0x73FFFFFF),                  // 45% White top-left light catch
    specularRimMid = AetherSpecularRimMid,                 // 5% White mid fade (#0DFFFFFF)
    specularRimCenter = Color(0x08FFFFFF),                 // 3% White center transparency
    specularRimEnd = Color(0x38FFFFFF),                    // 22% White bottom-right bounce light
    glowWhite = AetherGlowWhite,                           // #FFFFFF
    mutedTelemetry = AetherMutedTelemetry,                 // #80FFFFFF (50% White)
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xBFFFFFFF),                     // 75% White
    textMicro = Color(0x99FFFFFF),                         // 60% White (Section 1.C Micro Labels)
    primaryAccent = Color(0xFFE81E68),                     // Luminous Jewel Crimson
    secondaryAccent = Color(0xFFE07A42),                   // Warm Telemetry Amber
    sapphireAccent = Color(0xFF4CA8E0),                    // Spatial Sapphire Blue
    statusOptimal = Color(0xFF4ADE80),
    statusWarn = Color(0xFFFBBF24),
    dialWarmTint = Color(0x529C7865),                      // #9C7865 @ 32%
    dialCoolTint = Color(0x475A8BB0),                      // #5A8BB0 @ 28%
    ambientBlobPrimary = Color(0xFF59092B),                // Crimson mesh blob
    ambientBlobSecondary = Color(0xFF14385C),              // Deep sapphire mesh blob
    dotMatrixGhost = Color(0x1CFFFFFF)
)

val WineAetherPalette = AetherPalette(
    themeMode = ThemeMode.WINE,
    isDark = true,
    isWine = true,
    canvasVoid = Color(0xFF0A0206),
    frostedGlassFill = Color(0x1AFFC4D6),                  // 10% Rose-crystal frost
    frostedOpticalVeil = Color(0x701B040C),
    specularRimStart = Color(0x78FFE0EA),
    specularRimMid = Color(0x14FF9EB8),
    specularRimCenter = Color(0x08FF9EB8),
    specularRimEnd = Color(0x3DFF7AA0),
    glowWhite = Color(0xFFFFF5F8),
    mutedTelemetry = Color(0x85FFD1DF),
    textPrimary = Color(0xFFFFF5F8),
    textSecondary = Color(0xCCFFE0EA),
    textMicro = Color(0x9EFFC8D8),
    primaryAccent = Color(0xFFD91B5C),
    secondaryAccent = Color(0xFFE49B68),
    sapphireAccent = Color(0xFFFF7EB3),
    statusOptimal = Color(0xFF4ADE80),
    statusWarn = Color(0xFFFBBF24),
    dialWarmTint = Color(0x598C1D3B),
    dialCoolTint = Color(0x4D4D142E),
    ambientBlobPrimary = Color(0xFF7A0B34),
    ambientBlobSecondary = Color(0xFF45081E),
    dotMatrixGhost = Color(0x22FF8EA6)
)

val LightAetherPalette = AetherPalette(
    themeMode = ThemeMode.LIGHT,
    isDark = false,
    isWine = false,
    canvasVoid = Color(0xFFF1F3F9),
    frostedGlassFill = Color(0xB8FFFFFF),
    frostedOpticalVeil = Color(0x47FFFFFF),
    specularRimStart = Color(0xEBFFFFFF),
    specularRimMid = Color(0x59FFFFFF),
    specularRimCenter = Color(0x33CBD5E1),
    specularRimEnd = Color(0x99FFFFFF),
    glowWhite = Color(0xFF141824),
    mutedTelemetry = Color(0x801E293B),
    textPrimary = Color(0xFF111522),
    textSecondary = Color(0xBA242C40),
    textMicro = Color(0x99334155),
    primaryAccent = Color(0xFFB80D4F),
    secondaryAccent = Color(0xFFC95A1E),
    sapphireAccent = Color(0xFF0284C7),
    statusOptimal = Color(0xFF059669),
    statusWarn = Color(0xFFD97706),
    dialWarmTint = Color(0x42D9B39E),
    dialCoolTint = Color(0x3D9EC5E6),
    ambientBlobPrimary = Color(0xFFE88FB4),
    ambientBlobSecondary = Color(0xFF8CC5F2),
    dotMatrixGhost = Color(0x180F172A)
)

val LocalAetherPalette = staticCompositionLocalOf { DarkAetherPalette }

/** Shared [HazeState] published by [AetherThemeProvider] so frosted spatial cards blur the ambient mesh. */
val LocalAetherHazeState = staticCompositionLocalOf<HazeState?> { null }

object AetherTheme {
    val palette: AetherPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current

    val hazeState: HazeState?
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherHazeState.current
}

fun aetherPaletteFor(themeMode: ThemeMode): AetherPalette = when (themeMode) {
    ThemeMode.LIGHT -> LightAetherPalette
    ThemeMode.DARK -> DarkAetherPalette
    ThemeMode.WINE -> WineAetherPalette
}

/**
 * Provides Aether Telemetry UI tokens, optional [HazeState] for real-time spatial background blur,
 * and adapts [LocalRedColors] so shared modals, AR Sky, and Gravity Sandbox inherit Aether's palette.
 */
@Composable
fun AetherThemeProvider(
    enabled: Boolean,
    themeMode: ThemeMode,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val palette = remember(themeMode) { aetherPaletteFor(themeMode) }
    val baseRedColors = RedTheme.colors
    val adaptedRedColors = remember(baseRedColors, themeMode) {
        when (themeMode) {
            ThemeMode.WINE -> baseRedColors.copy(
                background = Color(0xFF0A0206),
                backgroundSecondary = Color(0xFF17040C),
                backgroundTertiary = Color(0xFF240713),
                surface = Color(0xEB18040C),
                surfaceElevated = Color(0xF0240713),
                surfaceGrouped = Color(0xE01C050F),
                surfaceVariant = Color(0xCC330B1C),
                border = Color(0x40FF9EB8),
                separator = Color(0x26FF9EB8),
                accentRed = Color(0xFFD91B5C),
                accentRedSubtle = Color(0x339E0B45),
                accentGold = Color(0xFFE49B68),
                accentBlue = Color(0xFFFF7EB3),
                accentPurple = Color(0xFFD978B0),
                navSurface = Color(0xE618040C),
                navBorder = Color(0x45FF9EB8)
            )
            ThemeMode.DARK -> baseRedColors.copy(
                background = AetherCanvasVoid,
                backgroundSecondary = Color(0xFF10111A),
                backgroundTertiary = Color(0xFF181A28),
                surface = Color(0xEB10111A),
                surfaceElevated = Color(0xF0171824),
                surfaceGrouped = Color(0xE012131E),
                surfaceVariant = Color(0xCC202233),
                border = AetherSpecularRimStart,
                separator = Color(0x1FFFFFFF),
                accentRed = AetherCrimsonCoreEdge,
                accentRedSubtle = Color(0x339E0B45),
                accentGold = Color(0xFFE07A42),
                accentBlue = Color(0xFF4CA8E0),
                accentPurple = Color(0xFFD96B98),
                navSurface = Color(0xE00D0E16),
                navBorder = AetherSpecularRimStart
            )
            ThemeMode.LIGHT -> baseRedColors.copy(
                background = Color(0xFFF1F3F9),
                backgroundSecondary = Color(0xFFF8FAFC),
                backgroundTertiary = Color(0xFFFFFFFF),
                surface = Color(0xF0FFFFFF),
                surfaceElevated = Color(0xF7FFFFFF),
                surfaceGrouped = Color(0xEBF1F5F9),
                surfaceVariant = Color(0xFFE2E8F0),
                border = Color(0x240F172A),
                separator = Color(0x140F172A),
                accentRed = Color(0xFFB80D4F),
                accentRedSubtle = Color(0x1FB80D4F),
                accentGold = Color(0xFFC95A1E),
                accentBlue = Color(0xFF0284C7),
                accentPurple = Color(0xFF9D174D),
                navSurface = Color(0xEBFFFFFF),
                navBorder = Color(0x99FFFFFF)
            )
        }
    }

    CompositionLocalProvider(
        LocalAetherPalette provides palette,
        LocalAetherHazeState provides hazeState,
        LocalRedColors provides adaptedRedColors,
        content = content
    )
}

// ============================================================================
// 2. REUSABLE RIM-LIT GLASS MODIFIER & TACTILE PRESS PHYSICS (NO OUTER BEZEL)
// ============================================================================

/**
 * Razor-thin `1.dp` directional specular rim light (Section 1 & Section 5 Component #1):
 * Bright on the top-left where light hits (`45%` white), fading through `8%` and `3%` white in the
 * middle, with a subtle `22%` bounce reflection on the bottom-right. When [touchPoint] is present,
 * tilts the specular light vector toward the user's finger coordinate (Section 4.3).
 */
fun Modifier.specularRimBorder(
    shape: Shape,
    strokeWidth: Dp = 1.dp,
    touchPoint: Offset? = null,
    highlightStart: Color = Color.White.copy(alpha = 0.45f),
    highlightMid: Color = Color.White.copy(alpha = 0.08f),
    highlightCenter: Color = Color.White.copy(alpha = 0.03f),
    highlightBounce: Color = Color.White.copy(alpha = 0.22f)
): Modifier {
    val startOffset = if (touchPoint != null) {
        Offset(
            x = (touchPoint.x * 0.45f).coerceIn(0f, 600f),
            y = (touchPoint.y * 0.45f).coerceIn(0f, 600f)
        )
    } else {
        Offset.Zero
    }
    val endOffset = if (touchPoint != null) {
        Offset(
            x = (1000f - touchPoint.x * 0.25f).coerceAtLeast(startOffset.x + 240f),
            y = (1000f - touchPoint.y * 0.25f).coerceAtLeast(startOffset.y + 240f)
        )
    } else {
        Offset(1000f, 1000f)
    }
    return this.border(
        width = strokeWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                highlightStart,
                highlightMid,
                highlightCenter,
                highlightBounce
            ),
            start = startOffset,
            end = endOffset
        ),
        shape = shape
    )
}

fun Modifier.specularRimBorder(
    cornerRadius: Dp = 36.dp,
    strokeWidth: Dp = 1.dp,
    touchPoint: Offset? = null
): Modifier = specularRimBorder(
    shape = squircleShape(cornerRadius),
    strokeWidth = strokeWidth,
    touchPoint = touchPoint
)

/**
 * Passive touch tracker that records the current finger coordinate without consuming child
 * pointer events (so nested steppers, sliders, and drag-to-scrub viewports work unimpeded).
 */
private fun Modifier.trackSpecularTouch(
    onTouchPointChanged: (Offset?) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val activeChange = event.changes.firstOrNull { it.pressed }
            onTouchPointChanged(activeChange?.position)
        }
    }
}

// ============================================================================
// 3. FRAMELESS JEWEL-TONE HERO TELEMETRY CARD (36.dp SQUIRCLE — NO OUTER BEZEL)
// ============================================================================

/**
 * Hero Telemetry Card (Section 1.A, Section 5 Component #3, Section 6.1):
 * Single frameless `36.dp` squircle optical lens with deep jewel-tone radial gradient
 * (near-black center for crisp readability, glowing inner perimeter), colored halo drop shadow,
 * `1.dp` directional specular rim light, and tactile `0.97f` bouncy spring press physics.
 */
@Composable
fun AetherJewelTelemetryCard(
    modifier: Modifier = Modifier,
    preset: AetherJewelPreset = AetherJewelPreset.CRIMSON,
    cornerRadius: Dp = AetherCornerGeometry.heroCard,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    elevation: Dp = 26.dp,
    interactivePress: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AetherTheme.palette
    val shape = squircleShape(cornerRadius)
    val jewelColors = remember(preset, palette.themeMode) { preset.colorsFor(palette.themeMode) }
    val edgeGlow = remember(preset, palette.themeMode) { preset.edgeGlowFor(palette.themeMode) }

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isTouching = isPressed || (interactivePress && touchPoint != null)

    val scale by animateFloatAsState(
        targetValue = if (isTouching) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherJewelCardScale"
    )

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = edgeGlow,
                        spotColor = edgeGlow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = jewelColors,
                        center = Offset(size.width * 0.44f, size.height * 0.42f),
                        radius = (size.maxDimension * 0.88f).coerceAtLeast(320f)
                    )
                )
            }
            .specularRimBorder(
                shape = shape,
                touchPoint = touchPoint,
                highlightStart = Color.White.copy(alpha = 0.45f),
                highlightMid = Color.White.copy(alpha = 0.08f),
                highlightCenter = Color.White.copy(alpha = 0.03f),
                highlightBounce = Color.White.copy(alpha = 0.22f)
            )
            .trackSpecularTouch { touchPoint = it }
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
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

// ============================================================================
// 4. SECONDARY SPATIAL FROSTED GLASS CARD (HAZE REAL-TIME BLUR + SPECULAR RIM)
// ============================================================================

/**
 * Secondary Spatial Card (Section 1.A & Section 6.1):
 * Translucent `FrostedGlassFill` (`Color.White.copy(alpha = 0.08f)`) blurred over ambient
 * background color blobs using Chris Banes' Haze (`Modifier.hazeChild`) + `1.dp` specular rim.
 */
@Composable
fun AetherFrostedSpatialCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AetherCornerGeometry.heroCard,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    enableHazeBlur: Boolean = true,
    ambientTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AetherTheme.palette
    val hazeState = AetherTheme.hazeState
    val shape = squircleShape(cornerRadius)

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherFrostedCardScale"
    )

    val hazeStyle = remember(palette) {
        HazeStyle(
            backgroundColor = palette.canvasVoid,
            tint = HazeTint(palette.frostedGlassFill),
            blurRadius = 24.dp,
            noiseFactor = 0.08f
        )
    }

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (enableHazeBlur && hazeState != null) {
                    Modifier.hazeChild(state = hazeState, style = hazeStyle)
                } else {
                    Modifier
                }
            )
            .background(palette.frostedOpticalVeil)
            .background(palette.frostedGlassFill)
            .drawBehind {
                if (ambientTint != null) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientTint.copy(alpha = if (palette.isDark) 0.16f else 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.20f),
                            radius = size.maxDimension * 0.75f
                        ),
                        radius = size.maxDimension * 0.75f,
                        center = Offset(size.width * 0.85f, size.height * 0.20f)
                    )
                }
            }
            .specularRimBorder(
                shape = shape,
                touchPoint = touchPoint,
                highlightStart = palette.specularRimStart,
                highlightMid = palette.specularRimMid,
                highlightCenter = palette.specularRimCenter,
                highlightBounce = palette.specularRimEnd
            )
            .trackSpecularTouch { touchPoint = it }
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
        CompositionLocalProvider(LocalContentColor provides palette.textPrimary) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

// ============================================================================
// 5. OSCILLOSCOPE WAVEFORM CANVAS WITH SPRING DRAW & NEON TROUGH GLOW
// ============================================================================

/**
 * Custom Synthesizer Oscilloscope Waveform Canvas (Section 4.2 & Section 5 Component #4):
 * - Smooth cubic bezier arches (`path.cubicTo`) with flat bottom troughs between arches.
 * - Left-to-right spring reveal via [PathMeasure.getSegment] (`spring(dampingRatio = 0.8f, stiffness = 200f)`).
 * - Thick white bottom troughs emitting a soft `BlurMaskFilter(12f, Blur.NORMAL)` neon glow
 *   that subtly breathes in opacity (`0.70f` <-> `1.0f`).
 */
@Composable
fun AetherOscilloscopeCanvas(
    activeWaves: Int,
    modifier: Modifier = Modifier,
    maxWaves: Int = 5,
    waveColor: Color = Color.White.copy(alpha = 0.45f),
    troughGlowColor: Color = Color.White
) {
    val clampedWaves = activeWaves.coerceIn(1, maxWaves.coerceAtLeast(1))
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }

    val drawFraction by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "oscilloscopeDrawProgress"
    )
    val animatedWaveProgress by animateFloatAsState(
        targetValue = clampedWaves.toFloat() / maxWaves.coerceAtLeast(1).toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "oscilloscopeMetricProgress"
    )

    val infiniteGlow = rememberInfiniteTransition(label = "aetherTroughGlow")
    val glowAlpha by infiniteGlow.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Canvas(modifier = modifier) {
        val baselineY = size.height * 0.82f
        val peakHeight = size.height * (0.58f + 0.16f * animatedWaveProgress)
        val archWidth = size.width * 0.072f
        val troughWidth = size.width * 0.054f

        val fullPath = Path()
        fullPath.moveTo(0f, baselineY)

        var currentX = 0f
        val troughs = mutableListOf<Pair<Float, Float>>()

        for (i in 0 until clampedWaves) {
            fullPath.cubicTo(
                currentX, baselineY - peakHeight,
                currentX + archWidth, baselineY - peakHeight,
                currentX + archWidth, baselineY
            )
            currentX += archWidth

            if (i < clampedWaves - 1) {
                val troughStart = currentX
                val troughEnd = currentX + troughWidth
                fullPath.lineTo(troughEnd, baselineY)
                troughs.add(troughStart to troughEnd)
                currentX = troughEnd
            }
        }

        fullPath.lineTo(size.width, baselineY)

        val measure = PathMeasure()
        measure.setPath(fullPath, false)
        val revealedPath = Path()
        measure.getSegment(0f, measure.length * drawFraction.coerceIn(0f, 1f), revealedPath, true)

        drawPath(
            path = revealedPath,
            color = waveColor,
            style = Stroke(width = 1.45.dp.toPx(), cap = StrokeCap.Round)
        )

        val revealLimitX = size.width * drawFraction
        val glowPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (glowAlpha * 255f).roundToInt().coerceIn(0, 255),
                (troughGlowColor.red * 255f).roundToInt(),
                (troughGlowColor.green * 255f).roundToInt(),
                (troughGlowColor.blue * 255f).roundToInt()
            )
            strokeWidth = 3.8.dp.toPx()
            style = android.graphics.Paint.Style.STROKE
            runCatching {
                maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
            }
        }

        troughs.forEach { (startX, endX) ->
            if (startX <= revealLimitX) {
                val clippedEndX = endX.coerceAtMost(revealLimitX)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawLine(startX, baselineY, clippedEndX, baselineY, glowPaint)
                }
                drawLine(
                    color = troughGlowColor,
                    start = Offset(startX, baselineY),
                    end = Offset(clippedEndX, baselineY),
                    strokeWidth = 3.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Smooth Cubic Spline Telemetry Curve with Glowing Current-Value Indicator Orb (Section 6.3).
 */
@Composable
fun AetherSplineTelemetryCanvas(
    samples: List<Float>,
    currentFraction: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White.copy(alpha = 0.65f),
    orbColor: Color = Color.White,
    glowColor: Color = AetherTheme.palette.primaryAccent
) {
    if (samples.size < 2) return
    val clampedFrac = currentFraction.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padY = h * 0.18f
        val usableH = (h - padY * 2f).coerceAtLeast(4f)

        val pts = samples.mapIndexed { idx, v ->
            val x = (idx.toFloat() / (samples.size - 1).toFloat()) * w
            val y = h - padY - v.coerceIn(0f, 1f) * usableH
            Offset(x, y)
        }

        val spline = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.lastIndex) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val midX = (p0.x + p1.x) * 0.5f
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        drawPath(
            path = spline,
            color = lineColor,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        val exactIdx = clampedFrac * (pts.size - 1)
        val low = exactIdx.toInt().coerceIn(0, pts.lastIndex)
        val high = (low + 1).coerceAtMost(pts.lastIndex)
        val t = exactIdx - low
        val orbPos = Offset(
            x = pts[low].x + (pts[high].x - pts[low].x) * t,
            y = pts[low].y + (pts[high].y - pts[low].y) * t
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.65f), Color.Transparent),
                center = orbPos,
                radius = 12.dp.toPx()
            ),
            radius = 12.dp.toPx(),
            center = orbPos
        )
        drawCircle(
            color = orbColor,
            radius = 3.8.dp.toPx(),
            center = orbPos
        )
    }
}

// ============================================================================
// 6. FROSTED 60-TICK RADIAL DIAL CARD (34.dp SQUIRCLE — SECTION 5 COMPONENT #5)
// ============================================================================

/**
 * Frosted Radial Tick-Mark Dial Card (`34.dp` squircle):
 * - Warm-amber-to-azure translucent frosted wash + Haze real-time background blur + `1.dp` specular rim.
 * - Custom 60-tick circular gauge with diagonal elongated highlight zone.
 * - Rolling Dot-Matrix central score readout + status micro-label.
 */
@Composable
fun AetherRadialDialCard(
    scoreText: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
    highlightFraction: Float = 0.37f,
    captionTop: String? = null,
    onClick: (() -> Unit)? = null
) {
    val palette = AetherTheme.palette
    val hazeState = AetherTheme.hazeState
    val shape = squircleShape(AetherCornerGeometry.dialCard)
    val appFont = LocalAppFontFamily.current

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherDialScale"
    )

    val animatedFraction by animateFloatAsState(
        targetValue = highlightFraction.coerceIn(0.05f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "aetherDialSweep"
    )

    val hazeStyle = remember(palette) {
        HazeStyle(
            backgroundColor = palette.canvasVoid,
            tint = HazeTint(palette.frostedGlassFill),
            blurRadius = 22.dp,
            noiseFactor = 0.08f
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState, style = hazeStyle)
                } else {
                    Modifier
                }
            )
            .background(palette.frostedOpticalVeil)
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.dialWarmTint, palette.dialCoolTint)
                )
            )
            .specularRimBorder(
                shape = shape,
                touchPoint = touchPoint,
                highlightStart = palette.specularRimStart,
                highlightMid = palette.specularRimMid,
                highlightCenter = palette.specularRimCenter,
                highlightBounce = palette.specularRimEnd
            )
            .trackSpecularTouch { touchPoint = it }
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
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalTicks = 60
            val radius = size.minDimension * 0.43f
            val center = Offset(size.width / 2f, size.height / 2f)
            val activeZoneCenter = (animatedFraction * 48f).roundToInt().coerceIn(5, 52)
            val zoneRange = (activeZoneCenter - 3)..(activeZoneCenter + 3)

            for (i in 0 until totalTicks) {
                val angleDeg = (i * 360f / totalTicks) - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isHighlighted = i in 6..11 || i in zoneRange
                val innerRadius = if (isHighlighted) radius * 0.72f else radius * 0.91f

                val start = Offset(
                    x = center.x + (innerRadius * cos(angleRad)).toFloat(),
                    y = center.y + (innerRadius * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = center.x + (radius * cos(angleRad)).toFloat(),
                    y = center.y + (radius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = palette.glowWhite.copy(alpha = if (isHighlighted) 0.95f else 0.45f),
                    start = start,
                    end = end,
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (captionTop != null) {
                Text(
                    text = captionTop,
                    style = RedTypographyTokens.caption.copy(
                        fontFamily = appFont,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = palette.textMicro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
            }
            AetherRollingDotMatrixCounter(
                text = scoreText,
                dotSize = 3.1.dp,
                dotGap = 1.05.dp,
                charGap = 3.2.dp,
                activeColor = palette.glowWhite,
                glowColor = palette.primaryAccent
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = statusLabel,
                style = RedTypographyTokens.caption.copy(
                    fontFamily = appFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light
                ),
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 7. GLOWING MESH PILL CARD (FULL CAPSULE — SECTION 5 COMPONENT #6)
// ============================================================================

/**
 * Mini Metric Glowing Mesh Pill Card (`50%` full capsule, `82.dp` height):
 * Radial mesh gradient glow + `1.dp` directional specular rim light + centered micro-title +
 * bottom-left Dot-Matrix value + unit + bottom-right `1.dp` minimal pill badge.
 */
@Composable
fun AetherGlowingMeshPillCard(
    title: String,
    value: String,
    unit: String,
    badge: String,
    preset: AetherMeshPillPreset,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = AetherTheme.palette
    val shape = RedShapes.capsule
    val appFont = LocalAppFontFamily.current
    val glowColors = remember(preset, palette.themeMode) { preset.colorsFor(palette.themeMode) }

    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherMeshPillScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = glowColors.first().copy(alpha = 0.35f),
                spotColor = glowColors.first().copy(alpha = 0.35f)
            )
            .clip(shape)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = glowColors,
                        center = Offset(size.width * 0.30f, size.height * 0.38f),
                        radius = (size.width * 0.92f).coerceAtLeast(180f)
                    )
                )
            }
            .specularRimBorder(
                shape = shape,
                touchPoint = touchPoint,
                highlightStart = Color.White.copy(alpha = 0.45f),
                highlightMid = Color.White.copy(alpha = 0.08f),
                highlightCenter = Color.White.copy(alpha = 0.03f),
                highlightBounce = Color.White.copy(alpha = 0.22f)
            )
            .trackSpecularTouch { touchPoint = it }
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = RedTypographyTokens.caption.copy(
                    fontFamily = appFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    AetherRollingDotMatrixCounter(
                        text = value,
                        dotSize = 2.15.dp,
                        dotGap = 0.75.dp,
                        charGap = 2.1.dp,
                        activeColor = Color.White,
                        glowColor = glowColors.first()
                    )
                    if (unit.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = RedTypographyTokens.caption.copy(
                                fontFamily = appFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }

                AetherMinimalPillBadge(text = badge)
            }
        }
    }
}

// ============================================================================
// 8. FLOATING TOP STATUS CAPSULE (100.dp CAPSULE — SECTION 5 COMPONENT #7)
// ============================================================================

/**
 * Floating Top Status Capsule (`[ ✦ Capsule  •  29%  •  25 min left  ⚡ ]`):
 * Rim-lit frosted spatial capsule blurred via Haze (`Modifier.hazeChild`).
 */
@Composable
fun AetherFloatingStatusCapsule(
    statusText: String,
    modifier: Modifier = Modifier,
    leadingGlyph: String = "✦",
    trailingGlyph: String = "⚡",
    onClick: (() -> Unit)? = null
) {
    val palette = AetherTheme.palette
    val hazeState = AetherTheme.hazeState
    val shape = RedShapes.capsule
    val appFont = LocalAppFontFamily.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherStatusCapsuleScale"
    )

    val hazeStyle = remember(palette) {
        HazeStyle(
            backgroundColor = palette.canvasVoid,
            tint = HazeTint(palette.frostedGlassFill),
            blurRadius = 22.dp,
            noiseFactor = 0.08f
        )
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState, style = hazeStyle)
                } else {
                    Modifier
                }
            )
            .background(palette.frostedOpticalVeil)
            .background(palette.frostedGlassFill)
            .specularRimBorder(
                shape = shape,
                highlightStart = palette.specularRimStart,
                highlightMid = palette.specularRimMid,
                highlightCenter = palette.specularRimCenter,
                highlightBounce = palette.specularRimEnd
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(palette.glowWhite.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leadingGlyph,
                style = RedTypographyTokens.caption.copy(
                    fontFamily = appFont,
                    fontSize = 11.sp
                ),
                color = palette.glowWhite
            )
        }
        Text(
            text = statusText,
            style = RedTypographyTokens.bodySecondary.copy(
                fontFamily = appFont,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Light
            ),
            color = palette.textPrimary.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = trailingGlyph,
            style = RedTypographyTokens.caption.copy(
                fontFamily = appFont,
                fontSize = 11.5.sp
            ),
            color = if (palette.isDark) AetherStatusBoltPink else palette.primaryAccent
        )
    }
}

// ============================================================================
// 9. AETHER MICRO-CONTROLS: STEPPERS, LED ARRAYS, BADGES & RIM CAPSULES
// ============================================================================

/**
 * Micro-Stepper Control (`←  ━  →`) with tactile haptic feedback (`HapticFeedbackType.TextHandleMove`).
 * Tapping the center bar optionally resets to default/live state!
 */
@Composable
fun AetherMicroStepper(
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null,
    tint: Color = Color.White
) {
    val haptics = LocalHapticFeedback.current
    val appFont = LocalAppFontFamily.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "←",
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontFamily = appFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light
                ),
                color = tint.copy(alpha = 0.80f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDecrement()
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .background(tint, CircleShape)
                    .then(
                        if (onReset != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReset()
                            }
                        } else {
                            Modifier
                        }
                    )
            )
            Text(
                text = "→",
                style = RedTypographyTokens.bodyPrimary.copy(
                    fontFamily = appFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light
                ),
                color = tint.copy(alpha = 0.80f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onIncrement()
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * 5-LED Telemetry Status Dot Array (`● ● ● ● ○`) with soft radial halo on active dots.
 */
@Composable
fun AetherLedStatusArray(
    litCount: Int,
    totalDots: Int = 5,
    modifier: Modifier = Modifier,
    dotSize: Dp = 7.dp,
    litColor: Color = Color.White
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalDots) {
            val isLit = i <= litCount
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .then(
                        if (isLit) {
                            Modifier.drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(litColor.copy(alpha = 0.55f), Color.Transparent),
                                        center = center,
                                        radius = size.minDimension * 1.45f
                                    ),
                                    radius = size.minDimension * 1.45f,
                                    center = center
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(if (isLit) litColor else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isLit) litColor else litColor.copy(alpha = 0.32f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 *Glowing 6.dp LED status dot + micro telemetry label (`● Poured` / `○ Planned`).
 */
@Composable
fun AetherLegendDotItem(
    label: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White
) {
    val appFont = LocalAppFontFamily.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (filled) dotColor else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (filled) dotColor else dotColor.copy(alpha = 0.42f),
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            style = RedTypographyTokens.caption.copy(
                fontFamily = appFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            ),
            color = dotColor.copy(alpha = if (filled) 0.88f else 0.50f),
            maxLines = 1
        )
    }
}

/**
 * Minimal `1.dp` rim-bordered telemetry pill badge (`[ ↑ 1% ]`, `[ LIVE ]`, `[ Launch → ]`) —
 * never chunky Material 3 filled buttons (Section 6.2).
 */
@Composable
fun AetherMinimalPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    borderColor: Color = Color.White.copy(alpha = 0.35f),
    fillColor: Color = Color.White.copy(alpha = 0.06f),
    onClick: (() -> Unit)? = null
) {
    val appFont = LocalAppFontFamily.current
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(RedShapes.capsule)
            .background(fillColor)
            .border(1.dp, borderColor, RedShapes.capsule)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = RedTypographyTokens.caption.copy(
                fontFamily = appFont,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Frameless rim-lit capsule chip for floating filters, location pills, and minimal actions.
 */
@Composable
fun AetherRimCapsule(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val palette = AetherTheme.palette
    val hazeState = AetherTheme.hazeState
    val haptics = LocalHapticFeedback.current
    val shape = RedShapes.capsule

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherRimCapsuleScale"
    )

    val hazeStyle = remember(palette) {
        HazeStyle(
            backgroundColor = palette.canvasVoid,
            tint = HazeTint(palette.frostedGlassFill),
            blurRadius = 18.dp,
            noiseFactor = 0.06f
        )
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState, style = hazeStyle)
                } else {
                    Modifier
                }
            )
            .background(palette.frostedOpticalVeil)
            .background(
                if (isActive) palette.primaryAccent.copy(alpha = if (palette.isDark) 0.28f else 0.16f)
                else palette.frostedGlassFill
            )
            .specularRimBorder(
                shape = shape,
                highlightStart = if (isActive) Color.White.copy(alpha = 0.62f) else palette.specularRimStart,
                highlightMid = palette.specularRimMid,
                highlightCenter = palette.specularRimCenter,
                highlightBounce = if (isActive) palette.primaryAccent.copy(alpha = 0.45f) else palette.specularRimEnd
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        content = content
    )
}

/**
 * Frameless 1.dp rim-lit circular optical icon disc for floating header actions.
 */
@Composable
fun AetherMicroIconDisc(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 18.dp,
    isActive: Boolean = false
) {
    val palette = AetherTheme.palette
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherIconDiscScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(palette.frostedOpticalVeil)
            .background(
                if (isActive) palette.primaryAccent.copy(alpha = 0.26f)
                else palette.frostedGlassFill
            )
            .specularRimBorder(
                shape = CircleShape,
                highlightStart = palette.specularRimStart,
                highlightMid = palette.specularRimMid,
                highlightCenter = palette.specularRimCenter,
                highlightBounce = palette.specularRimEnd
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) palette.primaryAccent else palette.textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Aether Telemetry Slider with glowing current-value indicator orb & haptic ticks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherTelemetrySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val palette = AetherTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val active = isPressed || isDragged

    val thumbSize by animateDpAsState(
        targetValue = if (active) 22.dp else 18.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "aetherSliderThumb"
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
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
                    .size(30.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    palette.primaryAccent.copy(alpha = if (active) 0.65f else 0.38f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.78f
                            ),
                            radius = size.minDimension * 0.78f,
                            center = center
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .border(BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.55f)), CircleShape)
                )
            }
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            ) {
                val trackH = size.height
                val r = CornerRadius(trackH / 2f, trackH / 2f)
                drawRoundRect(
                    color = palette.mutedTelemetry.copy(alpha = 0.25f),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = r
                )
                val span = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                val fraction = if (span > 0f) {
                    ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
                } else 0f
                val activeW = size.width * fraction
                if (activeW > 1f) {
                    val startX = if (isRtl) size.width - activeW else 0f
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(palette.primaryAccent, palette.secondaryAccent),
                            startX = startX,
                            endX = startX + activeW
                        ),
                        topLeft = Offset(startX, 0f),
                        size = Size(activeW, trackH),
                        cornerRadius = r
                    )
                }
            }
        }
    )
}
