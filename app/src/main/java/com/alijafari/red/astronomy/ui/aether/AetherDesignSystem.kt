package com.alijafari.red.astronomy.ui.aether

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.alijafari.red.astronomy.ui.theme.LocalRedColors
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// 1. AETHER TELEMETRY UI — SURFACE & COLOR SPECIFICATION TOKENS
// ============================================================================

/** Base screen background underneath ambient mesh lights (#08080C). */
val AetherCanvasVoid = Color(0xFF08080C)

/** Center dark void of the Jewel Telemetry Card for crisp high-contrast text (#1C020D). */
val AetherCrimsonCoreCenter = Color(0xFF1C020D)

/** Mid-ring radial jewel transition (#5E052A). */
val AetherCrimsonCoreMid = Color(0xFF5E052A)

/** Outer edge inner glow of the Jewel Telemetry Card (#9E0B45). */
val AetherCrimsonCoreEdge = Color(0xFF9E0B45)

/** Secondary Sapphire & Emerald Jewel-Tone radial palettes for orbital & lab cards. */
val AetherSapphireCoreCenter = Color(0xFF031724)
val AetherSapphireCoreMid = Color(0xFF083859)
val AetherSapphireCoreEdge = Color(0xFF0E5A8A)

val AetherEmeraldCoreCenter = Color(0xFF041E15)
val AetherEmeraldCoreMid = Color(0xFF084D3B)
val AetherEmeraldCoreEdge = Color(0xFF0D7A5F)

/** Translucent fill for spatial blurred cards (#14FFFFFF — 8% White). */
val AetherFrostedGlassFill = Color(0x14FFFFFF)

/** Top-left 1.dp specular rim border highlight (#66FFFFFF — 40% White). */
val AetherSpecularRimStart = Color(0x66FFFFFF)

/** Center specular rim border fade (#0DFFFFFF — 5% White). */
val AetherSpecularRimMid = Color(0x0DFFFFFF)

/** Bottom-right specular rim bounce light (#33FFFFFF — 20% White). */
val AetherSpecularRimEnd = Color(0x33FFFFFF)

/** Active LED dots, oscilloscope troughs, and primary dot-matrix numbers (#FFFFFF). */
val AetherGlowWhite = Color(0xFFFFFFFF)

/** Secondary labels, inactive wave baselines, and inactive radial ticks (#80FFFFFF — 50% White). */
val AetherMutedTelemetry = Color(0x80FFFFFF)

/** Signature Pink & Amber Mesh Pill glow colors. */
val AetherPinkMeshStart = Color(0xFFD96B98)
val AetherPinkMeshEnd = Color(0xFF32233B)
val AetherAmberMeshStart = Color(0xFFE07A42)
val AetherAmberMeshEnd = Color(0xFF3D1D14)
val AetherLightningTint = Color(0xFFFFB4D2)

/**
 * Deep jewel-tone radial gradient presets for frameless Hero Telemetry Cards.
 * Near-black in the center for crisp readability, glowing richly around the inner perimeter.
 */
enum class AetherJewelTone(
    val centerColor: Color,
    val midColor: Color,
    val edgeColor: Color
) {
    CRIMSON(
        centerColor = AetherCrimsonCoreCenter,
        midColor = AetherCrimsonCoreMid,
        edgeColor = AetherCrimsonCoreEdge
    ),
    SAPPHIRE(
        centerColor = AetherSapphireCoreCenter,
        midColor = AetherSapphireCoreMid,
        edgeColor = AetherSapphireCoreEdge
    ),
    EMERALD(
        centerColor = AetherEmeraldCoreCenter,
        midColor = AetherEmeraldCoreMid,
        edgeColor = AetherEmeraldCoreEdge
    ),
    WINE_RUBY(
        centerColor = Color(0xFF19010A),
        midColor = Color(0xFF6B062B),
        edgeColor = Color(0xFFB50E4E)
    )
}

/**
 * Glowing radial mesh presets for Mini Metric Pill Cards (`RoundedCornerShape(50)`).
 */
enum class AetherMeshPillPreset(
    val darkColors: List<Color>,
    val wineColors: List<Color>,
    val lightColors: List<Color>
) {
    PINK_MESH(
        darkColors = listOf(AetherPinkMeshStart, AetherPinkMeshEnd),
        wineColors = listOf(Color(0xFFE34D82), Color(0xFF360A1B)),
        lightColors = listOf(Color(0xFFE06397), Color(0xFF5E2B4C))
    ),
    AMBER_MESH(
        darkColors = listOf(AetherAmberMeshStart, AetherAmberMeshEnd),
        wineColors = listOf(Color(0xFFE3834B), Color(0xFF3D1418)),
        lightColors = listOf(Color(0xFFDE743C), Color(0xFF6B321C))
    ),
    SAPPHIRE_MESH(
        darkColors = listOf(Color(0xFF429CE0), Color(0xFF142A42)),
        wineColors = listOf(Color(0xFFC84B7A), Color(0xFF2B0B1B)),
        lightColors = listOf(Color(0xFF3387CC), Color(0xFF1E3E63))
    )
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
    val specularRimEnd: Color,
    val glowWhite: Color,
    val mutedTelemetry: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMicro: Color,
    val crimsonGlow: Color,
    val sapphireGlow: Color,
    val emeraldGlow: Color,
    val ambientBlobPrimary: Color,
    val ambientBlobSecondary: Color,
    val dialWarmTint: Color,
    val dialCoolTint: Color,
    val dotMatrixGhost: Color
)

val DarkAetherPalette = AetherPalette(
    themeMode = ThemeMode.DARK,
    isDark = true,
    isWine = false,
    canvasVoid = AetherCanvasVoid,
    frostedGlassFill = AetherFrostedGlassFill,
    frostedOpticalVeil = Color(0x520B0C14),
    specularRimStart = AetherSpecularRimStart,
    specularRimMid = AetherSpecularRimMid,
    specularRimEnd = AetherSpecularRimEnd,
    glowWhite = AetherGlowWhite,
    mutedTelemetry = AetherMutedTelemetry,
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xCCFFFFFF),
    textMicro = Color(0x99FFFFFF),
    crimsonGlow = AetherCrimsonCoreEdge,
    sapphireGlow = AetherSapphireCoreEdge,
    emeraldGlow = AetherEmeraldCoreEdge,
    ambientBlobPrimary = Color(0xFF59092B),
    ambientBlobSecondary = Color(0xFF14385C),
    dialWarmTint = Color(0x529C7865),
    dialCoolTint = Color(0x475A8BB0),
    dotMatrixGhost = Color(0x1FFFFFFF)
)

val WineAetherPalette = AetherPalette(
    themeMode = ThemeMode.WINE,
    isDark = true,
    isWine = true,
    canvasVoid = Color(0xFF0A0206),
    frostedGlassFill = Color(0x17FFD4E2),
    frostedOpticalVeil = Color(0x611D040D),
    specularRimStart = Color(0x73FFE3EC),
    specularRimMid = Color(0x12FF9EB8),
    specularRimEnd = Color(0x38FF85A6),
    glowWhite = Color(0xFFFFF5F8),
    mutedTelemetry = Color(0x8CFFDAE5),
    textPrimary = Color(0xFFFFF5F8),
    textSecondary = Color(0xD6FFE0E9),
    textMicro = Color(0x9EFFE0E9),
    crimsonGlow = Color(0xFFB81252),
    sapphireGlow = Color(0xFF8F1D52),
    emeraldGlow = Color(0xFFD96B4E),
    ambientBlobPrimary = Color(0xFF7D0A38),
    ambientBlobSecondary = Color(0xFF3D0C26),
    dialWarmTint = Color(0x599E3B5A),
    dialCoolTint = Color(0x4D5E1834),
    dotMatrixGhost = Color(0x24FF9EB8)
)

val LightAetherPalette = AetherPalette(
    themeMode = ThemeMode.LIGHT,
    isDark = false,
    isWine = false,
    canvasVoid = Color(0xFFF3F3F8),
    frostedGlassFill = Color(0xADFFFFFF),
    frostedOpticalVeil = Color(0x47FFFFFF),
    specularRimStart = Color(0xEBFFFFFF),
    specularRimMid = Color(0x40FFFFFF),
    specularRimEnd = Color(0x99FFFFFF),
    glowWhite = Color(0xFFFFFFFF),
    mutedTelemetry = Color(0x85231829),
    textPrimary = Color(0xFF181120),
    textSecondary = Color(0xBA2A1E36),
    textMicro = Color(0x8C3A2B4A),
    crimsonGlow = Color(0xFF9E0B45),
    sapphireGlow = Color(0xFF0E5A8A),
    emeraldGlow = Color(0xFF0D7A5F),
    ambientBlobPrimary = Color(0xFFD96B98),
    ambientBlobSecondary = Color(0xFF6CA8DB),
    dialWarmTint = Color(0x40C99A82),
    dialCoolTint = Color(0x3884B4D9),
    dotMatrixGhost = Color(0x1C181120)
)

val LocalAetherPalette = staticCompositionLocalOf { DarkAetherPalette }

object AetherTheme {
    val palette: AetherPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAetherPalette.current
}

fun aetherPaletteFor(themeMode: ThemeMode): AetherPalette = when (themeMode) {
    ThemeMode.DARK -> DarkAetherPalette
    ThemeMode.WINE -> WineAetherPalette
    ThemeMode.LIGHT -> LightAetherPalette
}

/**
 * Provides Aether Telemetry UI tokens and adapts shared `RedTheme` colors so modals,
 * AR sky controls, and simulation sheets harmonize with Aether's jewel-tone optical lenses.
 */
@Composable
fun AetherThemeProvider(
    enabled: Boolean,
    themeMode: ThemeMode,
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
                backgroundSecondary = Color(0xFF17030B),
                backgroundTertiary = Color(0xFF240613),
                surface = Color(0xEB1A040C),
                surfaceElevated = Color(0xF2260713),
                surfaceGrouped = Color(0xE01C050E),
                surfaceVariant = Color(0xCC330A1A),
                border = Color(0x40FF9EB8),
                separator = Color(0x24FF9EB8),
                accentRed = Color(0xFFD61E62),
                accentRedSubtle = Color(0x33B81252),
                accentGold = Color(0xFFE07A42),
                accentBlue = Color(0xFFD96B98),
                accentPurple = Color(0xFFB55BA6),
                navSurface = Color(0xE618040B),
                navBorder = Color(0x45FF9EB8)
            )
            ThemeMode.DARK -> baseRedColors.copy(
                background = AetherCanvasVoid,
                backgroundSecondary = Color(0xFF101018),
                backgroundTertiary = Color(0xFF181824),
                surface = Color(0xEB120E18),
                surfaceElevated = Color(0xF01A1422),
                surfaceGrouped = Color(0xE014101C),
                surfaceVariant = Color(0xCC241C30),
                border = AetherSpecularRimStart,
                separator = Color(0x1FFFFFFF),
                accentRed = Color(0xFFD41960),
                accentRedSubtle = Color(0x339E0B45),
                accentGold = Color(0xFFE07A42),
                accentBlue = Color(0xFF3E9CE0),
                accentPurple = AetherPinkMeshStart,
                navSurface = Color(0xE0100D16),
                navBorder = AetherSpecularRimStart
            )
            ThemeMode.LIGHT -> baseRedColors.copy(
                background = Color(0xFFF3F3F8),
                backgroundSecondary = Color(0xFFFAFAFD),
                backgroundTertiary = Color(0xFFFFFFFF),
                surface = Color(0xF0FFFFFF),
                surfaceElevated = Color(0xF7FFFFFF),
                surfaceGrouped = Color(0xEBF2EFF6),
                surfaceVariant = Color(0xFFE6E1EE),
                border = Color(0x26181120),
                separator = Color(0x14181120),
                accentRed = AetherCrimsonCoreEdge,
                accentRedSubtle = Color(0x1F9E0B45),
                accentGold = Color(0xFFC96228),
                accentBlue = AetherSapphireCoreEdge,
                accentPurple = Color(0xFF8C2D68),
                navSurface = Color(0xEBFFFFFF),
                navBorder = Color(0x80FFFFFF)
            )
        }
    }

    CompositionLocalProvider(
        LocalAetherPalette provides palette,
        LocalRedColors provides adaptedRedColors,
        content = content
    )
}

// ============================================================================
// 2. REUSABLE RIM-LIT GLASS MODIFIER (FRAMELESS — NO OUTER BEZEL)
// ============================================================================

/**
 * Razor-thin 1.dp directional specular rim light (`RoundedCornerShape(cornerRadius)`):
 * Bright on the top-left where light hits (40-45% white), fading to near-transparent
 * in the middle (3-8% white), with a subtle bounce reflection on the bottom-right (20-22% white).
 *
 * Supports dynamic touch-coordinate rim angle tilting when `tiltTarget` is active!
 */
fun Modifier.specularRimBorder(
    cornerRadius: Dp = 36.dp,
    strokeWidth: Dp = 1.dp,
    tiltTarget: Offset? = null
): Modifier {
    val startOffset = if (tiltTarget != null) {
        Offset(
            x = (tiltTarget.x * 0.35f).coerceIn(0f, 360f),
            y = (tiltTarget.y * 0.35f).coerceIn(0f, 360f)
        )
    } else {
        Offset(0f, 0f)
    }
    val endOffset = if (tiltTarget != null) {
        Offset(
            x = 1000f - (tiltTarget.x * 0.2f).coerceIn(0f, 250f),
            y = 1000f - (tiltTarget.y * 0.2f).coerceIn(0f, 250f)
        )
    } else {
        Offset(1000f, 1000f)
    }

    return this.border(
        width = strokeWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f), // Top-left light catch
                Color.White.copy(alpha = 0.08f), // Mid fade
                Color.White.copy(alpha = 0.03f), // Center transparency
                Color.White.copy(alpha = 0.22f)  // Bottom-right bounce light
            ),
            start = startOffset,
            end = endOffset
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
}

// ============================================================================
// 3. FRAMELESS JEWEL-TONE HERO TELEMETRY CARD (NO OUTER BEZEL)
// ============================================================================

/**
 * Frameless floating optical jewel lens (`RoundedCornerShape(36.dp)`):
 * - Deep high-contrast dark center (`#1C020D`) -> mid jewel transition (`#5E052A`) ->
 *   glowing outer perimeter (`#9E0B45`).
 * - Tactile bouncy spring press physics (`0.975f` via `graphicsLayer`) + specular rim tilt
 *   toward the touch coordinate.
 */
@Composable
fun AetherJewelCard(
    modifier: Modifier = Modifier,
    jewelTone: AetherJewelTone = AetherJewelTone.CRIMSON,
    cornerRadius: Dp = 36.dp,
    contentPadding: PaddingValues = PaddingValues(26.dp),
    elevation: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AetherTheme.palette
    val resolvedTone = if (palette.isWine && jewelTone == AetherJewelTone.CRIMSON) {
        AetherJewelTone.WINE_RUBY
    } else {
        jewelTone
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var touchPoint by remember { mutableStateOf<Offset?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AetherJewelCardScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = resolvedTone.edgeColor,
                spotColor = resolvedTone.edgeColor
            )
            .clip(shape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        resolvedTone.centerColor,
                        resolvedTone.midColor,
                        resolvedTone.edgeColor
                    ),
                    radius = 680f
                )
            )
            .specularRimBorder(
                cornerRadius = cornerRadius,
                tiltTarget = if (isPressed) touchPoint else null
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchPoint = down.position
                }
            }
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
            .padding(contentPadding)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

// ============================================================================
// 4. FRAMELESS SPATIAL FROSTED GLASS CARD
// ============================================================================

/**
 * Secondary frameless spatial card (`Color.White.copy(alpha = 0.08f)` `#14FFFFFF`)
 * floating over the breathing ambient background mesh with a 1.dp specular rim border.
 */
@Composable
fun AetherFrostedCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 36.dp,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AetherTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var touchPoint by remember { mutableStateOf<Offset?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AetherFrostedCardScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(palette.frostedOpticalVeil)
            .background(palette.frostedGlassFill)
            .then(
                if (accentGlow != null) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentGlow.copy(alpha = if (palette.isDark) 0.20f else 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.2f),
                                radius = size.maxDimension * 0.75f
                            ),
                            radius = size.maxDimension * 0.75f,
                            center = Offset(size.width * 0.85f, size.height * 0.2f)
                        )
                    }
                } else {
                    Modifier
                }
            )
            .specularRimBorder(
                cornerRadius = cornerRadius,
                tiltTarget = if (isPressed) touchPoint else null
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchPoint = down.position
                }
            }
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
            .padding(contentPadding)
    ) {
        CompositionLocalProvider(LocalContentColor provides palette.textPrimary) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

// ============================================================================
// 5. OSCILLOSCOPE WAVEFORM CANVAS WITH PATHMEASURE & NEON TROUGH GLOW
// ============================================================================

/**
 * Custom synthesizer oscilloscope telemetry waveform:
 * - Smooth cubic bezier arches (`path.cubicTo`) for active pulses.
 * - Left-to-right progressive wave draw via `PathMeasure.getSegment(0f, progress * length)`
 *   with `spring(dampingRatio = 0.8f, stiffness = 200f)`.
 * - Thick glowing white bottom troughs emitting a soft `BlurMaskFilter(12f, Blur.NORMAL)`
 *   neon bloom that subtly breathes in opacity (`0.7f` <-> `1.0f`).
 */
@Composable
fun AetherOscilloscopeWaveformCanvas(
    activeWaves: Int,
    progress: Float = 1f,
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White.copy(alpha = 0.48f),
    troughGlowColor: Color = AetherGlowWhite
) {
    val clampedWaves = activeWaves.coerceIn(1, 6)
    val animatedDrawProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.15f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "AetherWaveDrawProgress"
    )

    val infiniteGlow = rememberInfiniteTransition(label = "AetherTroughGlow")
    val glowAlpha by infiniteGlow.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AetherGlowAlpha"
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val baselineY = size.height * 0.82f
        val peakHeight = size.height * 0.72f
        val archWidth = size.width * 0.068f
        val troughWidth = size.width * 0.052f

        val fullPath = Path()
        fullPath.moveTo(0f, baselineY)

        var currentX = 0f
        val troughs = mutableListOf<Pair<Float, Float>>()

        for (i in 0 until clampedWaves) {
            fullPath.cubicTo(
                currentX,
                baselineY - peakHeight,
                currentX + archWidth,
                baselineY - peakHeight,
                currentX + archWidth,
                baselineY
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

        // Progressive left-to-right segment draw via PathMeasure
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(fullPath, false)
        val segmentedPath = Path()
        val stopDist = pathMeasure.length * animatedDrawProgress.coerceIn(0f, 1f)
        pathMeasure.getSegment(0f, stopDist, segmentedPath, true)

        // Subtle muted full baseline reference
        drawLine(
            color = waveColor.copy(alpha = 0.22f),
            start = Offset(currentX, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.1.dp.toPx()
        )

        // Continuous thin wave path
        drawPath(
            path = segmentedPath,
            color = waveColor,
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Thick glowing white bottom troughs with BlurMaskFilter(12f, Blur.NORMAL)
        val glowPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = (glowAlpha * 255).toInt().coerceIn(0, 255)
            strokeWidth = 3.5.dp.toPx()
            style = android.graphics.Paint.Style.STROKE
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }

        val visibleLimitX = size.width * animatedDrawProgress.coerceIn(0.25f, 1f)
        troughs.forEach { (startX, endX) ->
            if (startX <= visibleLimitX) {
                val clampedEnd = endX.coerceAtMost(visibleLimitX)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawLine(startX, baselineY, clampedEnd, baselineY, glowPaint)
                }
                drawLine(
                    color = troughGlowColor,
                    start = Offset(startX, baselineY),
                    end = Offset(clampedEnd, baselineY),
                    strokeWidth = 3.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// ============================================================================
// 6. FROSTED 60-TICK RADIAL DIAL CARD (`34.dp` SQUIRCLE)
// ============================================================================

/**
 * Square Bento Frosted Radial Tick-Mark Gauge (`RoundedCornerShape(34.dp)`):
 * 60 precision radial ticks around a central Rolling Dot-Matrix score readout and status caption,
 * featuring an elongated diagonal active gauge zone.
 */
@Composable
fun AetherRadialDialCard(
    scoreText: String,
    status: String,
    modifier: Modifier = Modifier,
    highlightFraction: Float = 0.62f,
    title: String? = null,
    onClick: (() -> Unit)? = null
) {
    val palette = AetherTheme.palette
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AetherDialScale"
    )

    val animatedFraction by animateFloatAsState(
        targetValue = highlightFraction.coerceIn(0.05f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f),
        label = "AetherDialFraction"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(34.dp))
            .background(palette.frostedOpticalVeil)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        palette.dialWarmTint,
                        palette.dialCoolTint
                    )
                )
            )
            .specularRimBorder(cornerRadius = 34.dp)
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
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalTicks = 60
            val radius = size.minDimension * 0.43f
            val center = Offset(size.width / 2f, size.height / 2f)
            val highlightCenterTick = ((animatedFraction * 24f).toInt() + 4).coerceIn(5, 52)
            val highlightRange = (highlightCenterTick - 3)..(highlightCenterTick + 3)

            for (i in 0 until totalTicks) {
                val angleDeg = (i * 360f / totalTicks) - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isHighlighted = i in highlightRange
                val innerRadius = if (isHighlighted) radius * 0.72f else radius * 0.91f
                val outerRadius = radius

                val start = Offset(
                    x = center.x + (innerRadius * cos(angleRad)).toFloat(),
                    y = center.y + (innerRadius * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = center.x + (outerRadius * cos(angleRad)).toFloat(),
                    y = center.y + (outerRadius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (palette.isDark) {
                        Color.White.copy(alpha = if (isHighlighted) 0.96f else 0.50f)
                    } else {
                        palette.textPrimary.copy(alpha = if (isHighlighted) 0.92f else 0.40f)
                    },
                    start = start,
                    end = end,
                    strokeWidth = if (isHighlighted) 1.6.dp.toPx() else 1.25.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (title != null) {
                Text(
                    text = title,
                    style = RedTypographyTokens.caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = if (palette.isDark) Color.White.copy(alpha = 0.6f) else palette.textMicro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            AetherRollingDotMatrixCounter(
                valueText = scoreText,
                dotSize = 2.55.dp,
                dotGap = 0.95.dp,
                charGap = 3.2.dp,
                color = if (palette.isDark) Color.White else palette.textPrimary,
                glowColor = palette.crimsonGlow
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = status,
                style = RedTypographyTokens.caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light
                ),
                color = if (palette.isDark) Color.White.copy(alpha = 0.78f) else palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// 7. GLOWING MESH PILL CARD (`RoundedCornerShape(50)` CAPSULE)
// ============================================================================

/**
 * Mini Metric Pill Card (`RoundedCornerShape(50)` full capsule, `82.dp` height):
 * Radial glowing mesh gradient (`#D96B98 -> #32233B` pink or `#E07A42 -> #3D1D14` amber)
 * with 1.dp specular rim, centered micro-title, Dot-Matrix readout + unit, and 1.dp pill badge.
 */
@Composable
fun AetherGlowingMeshPillCard(
    title: String,
    value: String,
    unit: String,
    badge: String,
    preset: AetherMeshPillPreset = AetherMeshPillPreset.PINK_MESH,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = AetherTheme.palette
    val haptics = LocalHapticFeedback.current
    val glowColors = when {
        palette.isWine -> preset.wineColors
        palette.isDark -> preset.darkColors
        else -> preset.lightColors
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AetherMeshPillScale"
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
                shape = RoundedCornerShape(50),
                ambientColor = glowColors.first().copy(alpha = 0.35f),
                spotColor = glowColors.first().copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(50))
            .background(
                Brush.radialGradient(
                    colors = glowColors,
                    center = Offset(120f, 60f),
                    radius = 300f
                )
            )
            .specularRimBorder(cornerRadius = 50.dp)
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = RedTypographyTokens.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AetherRollingDotMatrixCounter(
                        valueText = value,
                        dotSize = 1.9.dp,
                        dotGap = 0.72.dp,
                        charGap = 2.4.dp,
                        color = Color.White,
                        glowColor = glowColors.first()
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = RedTypographyTokens.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }

                AetherPillBadge(text = badge)
            }
        }
    }
}

// ============================================================================
// 8. FLOATING TOP STATUS CAPSULE (`100.dp` / `RoundedCornerShape(50)`)
// ============================================================================

/**
 * Floating Status Capsule (`[ ✦  ZIG • 89% • Bortle 3  ⚡ ]`):
 * Translucent `FrostedGlassFill` (`8%` White) with 1.dp specular rim border.
 */
@Composable
fun AetherFloatingStatusCapsule(
    text: String,
    modifier: Modifier = Modifier,
    leadingGlyph: String = "✦",
    trailingGlyph: String = "⚡",
    onClick: (() -> Unit)? = null
) {
    val palette = AetherTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AetherCapsuleScale"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0x5408080C))
            .background(Color.White.copy(alpha = 0.08f))
            .specularRimBorder(cornerRadius = 100.dp)
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
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leadingGlyph,
                style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                color = Color.White
            )
        }
        Text(
            text = text,
            style = RedTypographyTokens.caption.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.2).sp
            ),
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailingGlyph.isNotEmpty()) {
            Text(
                text = trailingGlyph,
                style = RedTypographyTokens.caption.copy(fontSize = 11.sp),
                color = if (palette.isWine) Color(0xFFFF9EB8) else AetherLightningTint
            )
        }
    }
}

// ============================================================================
// 9. SMOOTH SPLINE CURVE TELEMETRY CANVAS WITH GLOWING ORB
// ============================================================================

/**
 * Smooth cubic-bezier spline telemetry curve with a glowing current-value indicator orb,
 * built purely with Jetpack Compose `Canvas` (zero external chart libraries).
 */
@Composable
fun AetherSplineCurveCanvas(
    points: List<Float>,
    activeFraction: Float,
    modifier: Modifier = Modifier,
    curveColor: Color = Color.White.copy(alpha = 0.72f),
    glowColor: Color = AetherCrimsonCoreEdge
) {
    Canvas(modifier = modifier) {
        if (points.size < 2 || size.width <= 0f || size.height <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        val padY = h * 0.18f
        val usableH = (h - padY * 2f).coerceAtLeast(1f)

        fun ptOffset(idx: Int): Offset {
            val x = (idx.toFloat() / (points.size - 1).toFloat()) * w
            val y = padY + (1f - points[idx].coerceIn(0f, 1f)) * usableH
            return Offset(x, y)
        }

        val path = Path()
        var prev = ptOffset(0)
        path.moveTo(prev.x, prev.y)
        for (i in 1 until points.size) {
            val curr = ptOffset(i)
            val midX = (prev.x + curr.x) * 0.5f
            path.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            prev = curr
        }

        // Subtle baseline
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(0f, h - padY * 0.5f),
            end = Offset(w, h - padY * 0.5f),
            strokeWidth = 1.dp.toPx()
        )

        drawPath(
            path = path,
            color = curveColor,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Interpolate glowing current-value indicator orb along the spline
        val clampedF = activeFraction.coerceIn(0f, 1f)
        val exactIdx = clampedF * (points.size - 1)
        val lo = exactIdx.toInt().coerceIn(0, points.size - 1)
        val hi = (lo + 1).coerceIn(0, points.size - 1)
        val frac = exactIdx - lo
        val p0 = ptOffset(lo)
        val p1 = ptOffset(hi)
        val orbCenter = Offset(
            x = p0.x + (p1.x - p0.x) * frac,
            y = p0.y + (p1.y - p0.y) * frac
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.72f), Color.Transparent),
                center = orbCenter,
                radius = 12.dp.toPx()
            ),
            radius = 12.dp.toPx(),
            center = orbCenter
        )
        drawCircle(
            color = AetherGlowWhite,
            radius = 3.5.dp.toPx(),
            center = orbCenter
        )
    }
}

// ============================================================================
// 10. MICRO-STEPPERS (`←  ━  →`), LED ARRAYS, LEGEND DOTS & MINIMAL PILL BADGES
// ============================================================================

/**
 * Tactile Aether Micro-Stepper (`←  ━  →`) replacing chunky Material buttons inside cards.
 * Fires `HapticFeedbackType.TextHandleMove` on every step interaction.
 */
@Composable
fun AetherMicroStepper(
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
    tint: Color = Color.White
) {
    val haptics = LocalHapticFeedback.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canDecrement
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDecrement()
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = tint.copy(alpha = if (canDecrement) 0.85f else 0.30f)
                )
            }

            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .background(tint.copy(alpha = 0.92f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canIncrement
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onIncrement()
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    style = RedTypographyTokens.bodyPrimary.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = tint.copy(alpha = if (canIncrement) 0.85f else 0.30f)
                )
            }
        }
    }
}

/**
 * Glowing 6.dp–7.dp LED status dot item for telemetry legends (`● Poured` / `○ Planned`).
 */
@Composable
fun AetherLegendDotItem(
    label: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (filled) dotColor else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (filled) dotColor else dotColor.copy(alpha = 0.40f),
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            style = RedTypographyTokens.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            ),
            color = dotColor.copy(alpha = if (filled) 0.85f else 0.48f),
            maxLines = 1
        )
    }
}

/**
 * 5-LED Telemetry Status Array (`● ● ○ ○ ●`).
 */
@Composable
fun AetherLedDotArray(
    totalDots: Int = 5,
    litCount: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = AetherGlowWhite
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalDots) {
            val isLit = i <= litCount
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .then(
                        if (isLit) {
                            Modifier.drawBehind {
                                drawCircle(
                                    color = activeColor.copy(alpha = 0.38f),
                                    radius = size.minDimension * 1.15f
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(if (isLit) activeColor else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isLit) activeColor else activeColor.copy(alpha = 0.32f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Minimal 1.dp pill badge (`border(1.dp, Color.White.copy(0.35f), RoundedCornerShape(50))`).
 */
@Composable
fun AetherPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color = Color.White,
    onClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) tint.copy(alpha = 0.18f) else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = tint.copy(alpha = if (selected) 0.72f else 0.35f),
                shape = RoundedCornerShape(50)
            )
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
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Light
            ),
            color = tint.copy(alpha = if (selected) 0.98f else 0.88f),
            maxLines = 1
        )
    }
}

/**
 * Rolling Secondary Telemetry Value (`420 mg` style: `30.sp`, `FontWeight.Light`, `(-0.8).sp`).
 */
@Composable
fun AetherRollingSecondaryMetric(
    valueText: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.95f),
    fontSizeSp: Float = 30f
) {
    AnimatedContent(
        targetState = valueText,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()).togetherWith(
                slideOutVertically { -it / 2 } + fadeOut()
            )
        },
        label = "AetherSecondaryMetric",
        modifier = modifier
    ) { text ->
        Text(
            text = text,
            style = RedTypographyTokens.heroDisplay.copy(
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 4f).sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.8).sp,
                fontFeatureSettings = "tnum"
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Rim-lit circular optical icon button for floating top bars and compact actions.
 */
@Composable
fun AetherOpticalIconLens(
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
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AetherIconLensScale"
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
            .background(Color(0x5208080C))
            .background(
                if (isActive) palette.crimsonGlow.copy(alpha = 0.36f)
                else Color.White.copy(alpha = 0.08f)
            )
            .specularRimBorder(cornerRadius = 50.dp)
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
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Precision optical telemetry slider for orbital time scrubbing & Bortle scale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherTelemetrySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    accentColor: Color = AetherCrimsonCoreEdge
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
        label = "AetherSliderThumb"
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

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
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (active) 0.65f else 0.36f),
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
                        .background(AetherGlowWhite, CircleShape)
                        .border(1.dp, accentColor.copy(alpha = 0.65f), CircleShape)
                )
            }
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            ) {
                val r = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(
                    color = if (palette.isDark) Color.White.copy(alpha = 0.16f) else Color(0x22181120),
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
                            colors = listOf(accentColor, AetherGlowWhite),
                            startX = startX,
                            endX = startX + activeW
                        ),
                        topLeft = Offset(startX, 0f),
                        size = Size(activeW, size.height),
                        cornerRadius = r
                    )
                }
            }
        }
    )
}
