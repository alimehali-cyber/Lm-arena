package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.theme.LocalRedColors
import com.alijafari.red.astronomy.ui.theme.RedShapes
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.squircleShape

// ============================================================================
// VISIONOS SPATIAL GLASSMORPHISM & ETHEREAL BENTO DESIGN TOKENS
// ============================================================================

/** Signature pastel neon rose-pink accent (#FFB0D0) for glowing toggles, active pills, and meters. */
val SpatialNeonRose = Color(0xFFFFB0D0)
val SpatialNeonRoseVibrant = Color(0xFFFF7EB3)
val SpatialNeonRoseDeep = Color(0xFFD94882)
val SpatialWarmPeach = Color(0xFFFFBA94)
val SpatialSunsetCoral = Color(0xFFFF9678)
val SpatialSkyBlue = Color(0xFF68B6FF)
val SpatialCosmicIris = Color(0xFFA882FF)
val SpatialMintGlow = Color(0xFF6EE7C2)

/**
 * Dynamic multi-color linear/radial mesh gradient presets for Bento pills and highlight cards.
 */
enum class SpatialMeshPreset(
    val colors: List<Color>,
    val radialBloomColor: Color,
    val contentColor: Color = Color(0xFFFFFCFE)
) {
    /** Pink-to-orange mesh blur gradient (rose neon -> warm coral -> desert peach). */
    ROSE_TO_ORANGE(
        colors = listOf(Color(0xFFFF88C2), Color(0xFFFF9B83), Color(0xFFFFBE7E)),
        radialBloomColor = Color(0x66FFE3EE),
        contentColor = Color(0xFF23111C)
    ),

    /** Blue-to-purple mesh blur gradient (celestial cyan-blue -> periwinkle -> ethereal violet). */
    BLUE_TO_PURPLE(
        colors = listOf(Color(0xFF56B2FF), Color(0xFF7D85FF), Color(0xFFB372FF)),
        radialBloomColor = Color(0x59DCEEFF),
        contentColor = Color(0xFFFFFCFF)
    ),

    /** Ethereal desert twilight mesh (deep plum -> rose dusk -> warm amber). */
    TWILIGHT_DUNE(
        colors = listOf(Color(0xFF42275A), Color(0xFF883F68), Color(0xFFDE7C73)),
        radialBloomColor = Color(0x47FFB0D0),
        contentColor = Color(0xFFFFFAFD)
    ),

    /** Soft aurora pearl mesh (neon rose -> warm peach -> lavender). */
    AURORA_PEACH(
        colors = listOf(Color(0xFFFFA8CC), Color(0xFFFFC7A2), Color(0xFFC3ACFF)),
        radialBloomColor = Color(0x66FFFFFF),
        contentColor = Color(0xFF221426)
    ),

    /** Wine & Burgundy velvet mesh (vintage burgundy -> claret crimson -> rose garnet). */
    WINE_VELVET(
        colors = listOf(Color(0xFF7A142B), Color(0xFFB82547), Color(0xFFE55B78)),
        radialBloomColor = Color(0x59FFD4DF),
        contentColor = Color(0xFFFFF5F7)
    )
}

@Immutable
data class SpatialPalette(
    val isDark: Boolean,
    /** Primary frosted glass fill: rgba(255, 255, 255, 0.12) in dark, milky quartz in light. */
    val glassFill: Color,
    /** Elevated frosted glass fill for prominent floating bars & modals. */
    val glassElevatedFill: Color,
    /** Subtle warm backing tint beneath glassFill to ensure crisp legibility over bright skies/orbs. */
    val glassBackingVeil: Color,
    /** Specular top-edge highlight simulating overhead light catching beveled glass. */
    val rimTopHighlight: Color,
    /** Translucent side border (border-white/20). */
    val rimSideBorder: Color,
    /** Subtle bottom bounce reflection on floating glass edges. */
    val rimBottomReflection: Color,
    /** Inset white top shadow giving glass 3D physical thickness. */
    val innerTopGlow: Color,
    /** Ambient spatial drop shadow beneath floating squircle widgets. */
    val ambientDropShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Soft neon glow color (#FFB0D0) for active indicators, dot meters, and glow toggles. */
    val neonGlow: Color,
    /** Readable accent ink for text/icons sitting directly on frosted glass. */
    val accentInk: Color,
    val secondaryInk: Color,
    val coolInk: Color,
    val successInk: Color,
    val warningInk: Color,
    val dotMatrixInk: Color,
    val dotMatrixGhost: Color
)

val DarkSpatialPalette = SpatialPalette(
    isDark = true,
    glassFill = Color(0x1FFFFFFF),          // rgba(255, 255, 255, 0.12)
    glassElevatedFill = Color(0x29FFFFFF),  // rgba(255, 255, 255, 0.16)
    glassBackingVeil = Color(0x6B191226),   // Translucent twilight veil for guaranteed text contrast
    rimTopHighlight = Color(0x8CFFFFFF),    // 55% white top specular rim
    rimSideBorder = Color(0x33FFFFFF),      // 20% white side border (border-white/20)
    rimBottomReflection = Color(0x47FFFFFF),// 28% white bottom edge catchlight
    innerTopGlow = Color(0x73FFFFFF),       // inset 0 1px 2px rgba(255,255,255,0.45)
    ambientDropShadow = Color(0x5906030D),
    textPrimary = Color(0xFFFFFAFD),
    textSecondary = Color(0xCCEFE6F4),
    textTertiary = Color(0x8FEFE6F4),
    neonGlow = SpatialNeonRose,             // #FFB0D0
    accentInk = SpatialNeonRose,
    secondaryInk = SpatialWarmPeach,
    coolInk = Color(0xFF8CC8FF),
    successInk = SpatialMintGlow,
    warningInk = SpatialWarmPeach,
    dotMatrixInk = Color(0xFFFFF4FA),
    dotMatrixGhost = Color(0x1FFFFFFF)
)

val LightSpatialPalette = SpatialPalette(
    isDark = false,
    glassFill = Color(0xADFFFFFF),          // Frosted quartz white over warm desert sand
    glassElevatedFill = Color(0xCCFFFFFF),
    glassBackingVeil = Color(0x40FFF8F2),
    rimTopHighlight = Color(0xF2FFFFFF),    // 95% crisp white top specular rim
    rimSideBorder = Color(0x7AFFFFFF),      // Translucent pearl border
    rimBottomReflection = Color(0x8CFFFFFF),
    innerTopGlow = Color(0xB8FFFFFF),
    ambientDropShadow = Color(0x26402618),
    textPrimary = Color(0xFF1F1525),
    textSecondary = Color(0xBA3E2D47),
    textTertiary = Color(0x803E2D47),
    neonGlow = SpatialNeonRoseVibrant,
    accentInk = SpatialNeonRoseDeep,
    secondaryInk = Color(0xFFC96032),
    coolInk = Color(0xFF2563EB),
    successInk = Color(0xFF0D9488),
    warningInk = Color(0xFFD97706),
    dotMatrixInk = Color(0xFF24162B),
    dotMatrixGhost = Color(0x1A24162B)
)

val WineSpatialPalette = SpatialPalette(
    isDark = true,
    glassFill = Color(0x22FF8EA6),          // Burgundy-tinted frosted crystal glass
    glassElevatedFill = Color(0x2EFF9EB3),  // Elevated wine-crystal frost
    glassBackingVeil = Color(0x7A240610),   // Deep cabernet-merlot optical backing veil
    rimTopHighlight = Color(0x99FFD6E0),    // Ruby-rose top specular catchlight
    rimSideBorder = Color(0x42F06886),      // Translucent claret side border
    rimBottomReflection = Color(0x52B8294A),// Burgundy bottom reflection
    innerTopGlow = Color(0x78FFE0E8),       // Warm blush inset glass thickness glow
    ambientDropShadow = Color(0x66090104),
    textPrimary = Color(0xFFFFF2F5),
    textSecondary = Color(0xD6F4D7DF),
    textTertiary = Color(0x94F4D7DF),
    neonGlow = Color(0xFFE84A6C),           // Wine-garnet neon glow
    accentInk = Color(0xFFFF8AA5),
    secondaryInk = Color(0xFFE8A878),       // Warm cognac-amber secondary ink
    coolInk = Color(0xFFF09AB8),
    successInk = SpatialMintGlow,
    warningInk = Color(0xFFE8A878),
    dotMatrixInk = Color(0xFFFFEDF2),
    dotMatrixGhost = Color(0x26FF8EA6)
)

val LocalSpatialPalette = staticCompositionLocalOf { DarkSpatialPalette }

object SpatialTheme {
    val palette: SpatialPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSpatialPalette.current
}

fun spatialPaletteFor(themeMode: ThemeMode): SpatialPalette = when (themeMode) {
    ThemeMode.LIGHT -> LightSpatialPalette
    ThemeMode.DARK -> DarkSpatialPalette
    ThemeMode.WINE -> WineSpatialPalette
}

/**
 * Wraps the application tree when [enabled] is true so both Spatial-native Bento screens and
 * shared secondary modals/HUDs inherit harmonious VisionOS Spatial colors and high-contrast ink.
 */
@Composable
fun SpatialThemeProvider(
    enabled: Boolean,
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val palette = remember(themeMode) { spatialPaletteFor(themeMode) }
    val baseRedColors = RedTheme.colors
    val adaptedRedColors = remember(baseRedColors, palette, themeMode) {
        when (themeMode) {
            ThemeMode.WINE -> baseRedColors.copy(
                background = Color(0xFF140409),
                backgroundSecondary = Color(0xFF1F0710),
                backgroundTertiary = Color(0xFF2B0B17),
                surface = Color(0xE61D070F),
                surfaceElevated = Color(0xEB280A15),
                surfaceGrouped = Color(0xD9210811),
                surfaceVariant = Color(0xCC36101E),
                border = Color(0x42FF8EA6),
                separator = Color(0x2BFF8EA6),
                accentRed = Color(0xFFE84A6C),
                accentRedSubtle = Color(0x38B82547),
                accentGold = Color(0xFFE8A878),
                accentBlue = Color(0xFFF09AB8),
                accentPurple = Color(0xFFC86CA2),
                navSurface = Color(0xE0210710),
                navBorder = Color(0x4DFF9EB3)
            )
            ThemeMode.DARK -> baseRedColors.copy(
                background = Color(0xFF140E1E),
                backgroundSecondary = Color(0xFF1E162C),
                backgroundTertiary = Color(0xFF281E3A),
                surface = Color(0xE01B1429),
                surfaceElevated = Color(0xEB231A34),
                surfaceGrouped = Color(0xD91D152B),
                surfaceVariant = Color(0xCC2E2242),
                border = Color(0x38FFFFFF),
                separator = Color(0x24FFFFFF),
                accentRed = SpatialNeonRoseVibrant,
                accentRedSubtle = Color(0x33FFB0D0),
                accentGold = SpatialWarmPeach,
                accentBlue = SpatialSkyBlue,
                accentPurple = SpatialCosmicIris,
                navSurface = Color(0xD91E162D),
                navBorder = Color(0x47FFFFFF)
            )
            ThemeMode.LIGHT -> baseRedColors.copy(
                background = Color(0xFFF5EAE0),
                backgroundSecondary = Color(0xFFFAF2EB),
                backgroundTertiary = Color(0xFFFFFFFF),
                surface = Color(0xEBFAF4EF),
                surfaceElevated = Color(0xF2FFFDFB),
                surfaceGrouped = Color(0xE6F6EDE5),
                surfaceVariant = Color(0xFFEFE3D8),
                border = Color(0x243A2218),
                separator = Color(0x1A3A2218),
                accentRed = SpatialNeonRoseDeep,
                accentRedSubtle = Color(0x24FF7EB3),
                accentGold = Color(0xFFB8622A),
                accentBlue = Color(0xFF2563EB),
                accentPurple = Color(0xFF7C3AED),
                navSurface = Color(0xE6FFFDFB),
                navBorder = Color(0x99FFFFFF)
            )
        }
    }

    CompositionLocalProvider(
        LocalSpatialPalette provides palette,
        LocalRedColors provides adaptedRedColors,
        content = content
    )
}

// ============================================================================
// CORE SPATIAL GLASS CONTAINERS (SQUIRCLE CARDS, CAPSULES & MESH PILLS)
// ============================================================================

/**
 * VisionOS Spatial Glassmorphic Squircle Card.
 *
 * Implements the complete optical stack:
 * 1. Continuous G2 curvature squircle (`rounded-[32px]` via [squircleShape]).
 * 2. Soft ambient spatial drop shadow (`0 16dp 35dp rgba(0,0,0,0.15)`).
 * 3. Translucent frosted surface (`rgba(255,255,255,0.12)` + warm ambient diffusion sheen).
 * 4. Inset top specular highlight (`inset 0 1px 2px rgba(255,255,255,0.5)`) simulating glass thickness.
 * 5. Directional specular rim border (`border-white/20` with bright top catchlight).
 * 6. Tactile fluid spring press feedback.
 */
@Composable
fun SpatialGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    elevation: Dp = 14.dp,
    elevated: Boolean = false,
    glowAccent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = SpatialTheme.palette
    val shape = squircleShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "SpatialCardScale"
    )

    val rimBrush = remember(palette) {
        Brush.verticalGradient(
            0.0f to palette.rimTopHighlight,
            0.35f to palette.rimSideBorder,
            0.80f to palette.rimSideBorder,
            1.0f to palette.rimBottomReflection
        )
    }

    val frostSheen = remember(palette, elevated) {
        val baseGlass = if (elevated) palette.glassElevatedFill else palette.glassFill
        Brush.linearGradient(
            0.0f to baseGlass.copy(alpha = (baseGlass.alpha * 1.30f).coerceAtMost(0.92f)),
            0.55f to baseGlass,
            1.0f to baseGlass.copy(alpha = (baseGlass.alpha * 0.85f).coerceAtMost(0.92f))
        )
    }

    val innerTopHighlightColor = palette.innerTopGlow

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .scale(scale)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = palette.ambientDropShadow,
                        spotColor = palette.ambientDropShadow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(palette.glassBackingVeil)
            .background(frostSheen)
            .drawWithContent {
                // Optional subtle ambient corner neon aura inside the frosted glass
                if (glowAccent != null) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowAccent.copy(alpha = if (palette.isDark) 0.22f else 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.18f),
                            radius = size.maxDimension * 0.68f
                        ),
                        radius = size.maxDimension * 0.68f,
                        center = Offset(size.width * 0.85f, size.height * 0.18f)
                    )
                }
                drawContent()
                // Inset top specular rim shadow (simulates 2mm beveled optical glass thickness)
                val bevelHeight = 2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(innerTopHighlightColor, Color.Transparent),
                        startY = 0f,
                        endY = bevelHeight * 2.2f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, bevelHeight * 2.2f)
                )
            }
            .border(BorderStroke(1.dp, rimBrush), shape)
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

/**
 * Floating Capsule Container with frosted glass diffusion, specular top border, and optional radial glow.
 */
@Composable
fun SpatialGlassCapsule(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    isActive: Boolean = false,
    activeGlowColor: Color = SpatialNeonRose,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val palette = SpatialTheme.palette
    val shape: Shape = RedShapes.capsule
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "SpatialCapsuleScale"
    )

    val rimBrush = remember(palette, isActive, activeGlowColor) {
        if (isActive) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.80f),
                    activeGlowColor.copy(alpha = 0.55f),
                    activeGlowColor.copy(alpha = 0.35f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(palette.rimTopHighlight, palette.rimSideBorder, palette.rimBottomReflection)
            )
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (isActive) {
                    Modifier.drawBehind {
                        // Outer soft neon radial glow (box-shadow: 0 0 20px #FFB0D0)
                        val r = size.maxDimension * 0.72f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeGlowColor.copy(alpha = if (palette.isDark) 0.34f else 0.26f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = r
                            ),
                            radius = r,
                            center = center
                        )
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = palette.ambientDropShadow,
                        spotColor = palette.ambientDropShadow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(palette.glassBackingVeil)
            .background(if (isActive) activeGlowColor.copy(alpha = if (palette.isDark) 0.24f else 0.20f) else palette.glassFill)
            .drawWithContent {
                if (isActive) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeGlowColor.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.2f),
                            radius = size.maxDimension * 0.75f
                        )
                    )
                }
                drawContent()
                val bevel = 1.5.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(palette.innerTopGlow, Color.Transparent),
                        startY = 0f,
                        endY = bevel * 2f
                    ),
                    size = Size(size.width, bevel * 2f)
                )
            }
            .border(BorderStroke(1.dp, rimBrush), shape)
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
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * Mesh Gradient Pill / Widget Card:
 * Filled with dynamic multi-color linear + radial blur gradients (e.g., pink-to-orange and blue-to-purple)
 * with a frosted specular glass rim and inset top highlight.
 */
@Composable
fun SpatialMeshPill(
    preset: SpatialMeshPreset,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    isCapsule: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = if (isCapsule) RedShapes.capsule else squircleShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
        label = "SpatialMeshScale"
    )

    val rimBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.72f),
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.36f)
            )
        )
    }

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = preset.colors.first().copy(alpha = 0.28f),
                spotColor = preset.colors.last().copy(alpha = 0.32f)
            )
            .clip(shape)
            .drawBehind {
                // Multi-stop diagonal linear gradient
                drawRect(
                    brush = Brush.linearGradient(
                        colors = preset.colors,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
                // Overlapping organic mesh radial bloom (top-left warm pearl highlight)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(preset.radialBloomColor, Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.18f),
                        radius = size.maxDimension * 0.75f
                    ),
                    radius = size.maxDimension * 0.75f,
                    center = Offset(size.width * 0.22f, size.height * 0.18f)
                )
                // Secondary soft accent orb (bottom-right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(preset.colors.last().copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.maxDimension * 0.65f
                    ),
                    radius = size.maxDimension * 0.65f,
                    center = Offset(size.width * 0.85f, size.height * 0.85f)
                )
            }
            .drawWithContent {
                drawContent()
                // Inset top specular bevel highlight
                val bevel = 2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.58f), Color.Transparent),
                        startY = 0f,
                        endY = bevel * 2.2f
                    ),
                    size = Size(size.width, bevel * 2.2f)
                )
            }
            .border(BorderStroke(1.dp, rimBrush), shape)
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
        CompositionLocalProvider(LocalContentColor provides preset.contentColor) {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

// ============================================================================
// GLOW TOGGLES, SLIDERS & INTERACTIVE SPATIAL CONTROLS
// ============================================================================

/**
 * VisionOS Glowing Toggle Switch with soft neon radial inner & outer bloom (#FFB0D0)
 * and spring-animated pearl thumb.
 */
@Composable
fun SpatialGlowToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = SpatialNeonRose
) {
    val palette = SpatialTheme.palette
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "SpatialToggleThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) glowColor.copy(alpha = 0.42f) else palette.glassFill,
        animationSpec = tween(200),
        label = "SpatialToggleTrack"
    )

    Box(
        modifier = modifier
            .width(50.dp)
            .height(28.dp)
            .drawBehind {
                if (checked) {
                    val r = size.maxDimension * 0.85f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.45f), Color.Transparent),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )
                }
            }
            .clip(RedShapes.capsule)
            .background(palette.glassBackingVeil)
            .background(trackColor)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (checked) {
                        listOf(Color.White.copy(alpha = 0.85f), glowColor.copy(alpha = 0.65f))
                    } else {
                        listOf(palette.rimTopHighlight, palette.rimSideBorder)
                    }
                ),
                shape = RedShapes.capsule
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        if (checked) {
                            listOf(Color.White, Color(0xFFFFEEF6))
                        } else {
                            listOf(Color.White.copy(alpha = 0.92f), Color(0xFFE8E2EE))
                        }
                    )
                )
                .border(0.5.dp, Color.White, CircleShape)
        )
    }
}

/**
 * VisionOS Spatial Glow Slider:
 * Frosted capsule track with illuminated neon rose-to-peach gradient fill and radial glow thumb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialGlowSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    glowColor: Color = SpatialNeonRose,
    secondaryGlowColor: Color = SpatialWarmPeach
) {
    val palette = SpatialTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val active = isPressed || isDragged

    val thumbSize by animateDpAsState(
        targetValue = if (active) 26.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SpatialSliderThumb"
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
                    .size(34.dp)
                    .drawBehind {
                        val bloomRadius = if (active) size.minDimension * 0.85f else size.minDimension * 0.62f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = if (active) 0.62f else 0.40f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = bloomRadius
                            ),
                            radius = bloomRadius,
                            center = center
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .shadow(6.dp, CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White, Color(0xFFFFEDF5))
                            ),
                            CircleShape
                        )
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val trackHeight = size.height
                val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                // Frosted glass inactive capsule track
                drawRoundRect(
                    color = if (palette.isDark) Color(0x2EFFFFFF) else Color(0x26382218),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = radius
                )

                val span = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                val fraction = if (span > 0f) {
                    ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
                } else 0f

                val activeWidth = size.width * fraction
                if (activeWidth > 1f) {
                    val startX = if (isRtl) size.width - activeWidth else 0f
                    // Soft glow aura along active track
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(glowColor.copy(alpha = 0.35f), secondaryGlowColor.copy(alpha = 0.35f)),
                            startX = startX,
                            endX = startX + activeWidth
                        ),
                        topLeft = Offset(startX, -3.dp.toPx()),
                        size = Size(activeWidth, trackHeight + 6.dp.toPx()),
                        cornerRadius = CornerRadius(trackHeight, trackHeight)
                    )
                    // Illuminated active capsule fill
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(glowColor, secondaryGlowColor),
                            startX = startX,
                            endX = startX + activeWidth
                        ),
                        topLeft = Offset(startX, 0f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = radius
                    )
                }
            }
        }
    )
}

/**
 * Circular Frosted Glass Action Button with Specular Rim & optional Neon Glow.
 */
@Composable
fun SpatialGlowIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp,
    isActive: Boolean = false,
    glowColor: Color = SpatialNeonRose
) {
    val palette = SpatialTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f),
        label = "SpatialIconBtnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (isActive) {
                    Modifier.drawBehind {
                        val r = this.size.minDimension * 0.85f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = 0.48f), Color.Transparent),
                                center = center,
                                radius = r
                            ),
                            radius = r,
                            center = center
                        )
                    }
                } else {
                    Modifier
                }
            )
            .shadow(8.dp, CircleShape, ambientColor = palette.ambientDropShadow, spotColor = palette.ambientDropShadow)
            .clip(CircleShape)
            .background(palette.glassBackingVeil)
            .background(
                if (isActive) glowColor.copy(alpha = if (palette.isDark) 0.30f else 0.24f)
                else palette.glassElevatedFill
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    if (isActive) listOf(Color.White.copy(alpha = 0.85f), glowColor.copy(alpha = 0.55f))
                    else listOf(palette.rimTopHighlight, palette.rimSideBorder)
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) palette.accentInk else palette.textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Spatial Bento Section Header with ethereal low-contrast subtitle and optional capsule badge.
 */
@Composable
fun SpatialSectionHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = SpatialTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = RedTypographyTokens.sectionTitle.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = palette.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption.copy(fontSize = 12.sp),
                    color = palette.textSecondary
                )
            }
        }
        if (badgeText != null) {
            SpatialGlassCapsule(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                isActive = true,
                elevation = 4.dp
            ) {
                Text(
                    text = badgeText,
                    style = RedTypographyTokens.badge.copy(fontSize = 11.sp),
                    color = palette.accentInk
                )
            }
        }
    }
}
