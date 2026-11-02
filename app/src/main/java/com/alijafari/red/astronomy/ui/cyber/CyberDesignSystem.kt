package com.alijafari.red.astronomy.ui.cyber

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
import androidx.compose.foundation.layout.fillMaxSize
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
// SPATIAL GLASSMORPHISM 2.0 & CYBER-MINIMALIST DESIGN SYSTEM
// (Apple visionOS x Nothing OS x Teenage Engineering)
// ============================================================================

val CyberCoral = Color(0xFFFF7A6B)
val CyberPeach = Color(0xFFFFB284)
val CyberCyan = Color(0xFF3CE0F5)
val CyberViolet = Color(0xFFA78BFA)
val CyberNothingRed = Color(0xFFFF3B30)
val CyberWineCrimson = Color(0xFFE84265)
val CyberWineBurgundy = Color(0xFF7D1128)
val CyberWineGarnet = Color(0xFFFF6B8A)
val CyberWineCognac = Color(0xFFE4A672)

/**
 * Multi-color glowing mesh presets visible beneath frosted glass inner pill cards.
 */
enum class CyberMeshPreset(
    val darkColors: List<Color>,
    val wineColors: List<Color>,
    val bloomColor: Color,
    val wineBloomColor: Color
) {
    CORAL_PEACH(
        darkColors = listOf(Color(0xFFFF6E6C), Color(0xFFFF9671), Color(0xFFFFC18C)),
        wineColors = listOf(Color(0xFF8C142E), Color(0xFFC92A4E), Color(0xFFE87A68)),
        bloomColor = Color(0x66FFD9C7),
        wineBloomColor = Color(0x59FFC8D4)
    ),
    CYAN_VIOLET(
        darkColors = listOf(Color(0xFF28C8EE), Color(0xFF637BFF), Color(0xFFA86BFF)),
        wineColors = listOf(Color(0xFF5E1029), Color(0xFF9E1F47), Color(0xFFD65882)),
        bloomColor = Color(0x59CCF4FF),
        wineBloomColor = Color(0x52FFD0E0)
    ),
    VIOLET_PEACH(
        darkColors = listOf(Color(0xFF7C5CFF), Color(0xFFD66BFF), Color(0xFFFF9E80)),
        wineColors = listOf(Color(0xFF6B0F26), Color(0xFFB52448), Color(0xFFE49A6E)),
        bloomColor = Color(0x59F2D9FF),
        wineBloomColor = Color(0x52FFE0CC)
    ),
    EMERALD_CYAN(
        darkColors = listOf(Color(0xFF1AD1A5), Color(0xFF28B8E8), Color(0xFF6C8CFF)),
        wineColors = listOf(Color(0xFF75152E), Color(0xFFB82B52), Color(0xFFE86A88)),
        bloomColor = Color(0x59C8FFF0),
        wineBloomColor = Color(0x52FFD2DE)
    )
}

@Immutable
data class CyberPalette(
    val themeMode: ThemeMode,
    val isDark: Boolean,
    val isWine: Boolean,
    /** Background canvas fill behind ambient mesh spheres. */
    val canvasBg: Color,
    /** Exact Spatial Glass 2.0 fill: rgba(255, 255, 255, 0.07) in dark, sheer crystal in light/wine. */
    val glassFill: Color,
    /** Elevated floating capsule & modal glass fill: rgba(255, 255, 255, 0.11). */
    val glassElevatedFill: Color,
    /** Subtle optical contrast tint (bg-slate-900/30 or merlot-950/40) ensuring crisp typography over any backdrop. */
    val glassReadabilityTint: Color,
    /** Fine border highlight: 1px solid rgba(255, 255, 255, 0.18). */
    val glassBorder: Color,
    /** Top-inner highlight for cut-glass depth: inset 0 1px 1px rgba(255, 255, 255, 0.30). */
    val innerTopHighlight: Color,
    /** Bottom-inner shadow for glass refraction: inset 0 -1px 1px rgba(0, 0, 0, 0.10). */
    val innerBottomShadow: Color,
    /** Soft distance drop shadow: 0 20px 40px rgba(0, 0, 0, 0.20). */
    val distanceDropShadow: Color,
    /** Recessed cutout viewport aperture background. */
    val cutoutViewportBg: Color,
    /** Recessed cutout viewport rim highlight. */
    val cutoutViewportBorder: Color,
    /** Typography hierarchy (ultra-thin / light geometric styling). */
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Nothing OS / Teenage Engineering signature live signal dot. */
    val signalDot: Color,
    /** Primary accent ink for icons, active indicators & glows. */
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryAccent: Color,
    val statusGood: Color,
    val statusWarn: Color,
    /** Dot-Matrix counter ink & unlit matrix grid ink. */
    val matrixLit: Color,
    val matrixGhost: Color,
    val microGridDot: Color
)

val DarkCyberPalette = CyberPalette(
    themeMode = ThemeMode.DARK,
    isDark = true,
    isWine = false,
    canvasBg = Color(0xFF07090F),
    glassFill = Color(0x12FFFFFF),            // rgba(255, 255, 255, 0.07)
    glassElevatedFill = Color(0x1CFFFFFF),    // rgba(255, 255, 255, 0.11)
    glassReadabilityTint = Color(0x540B101D), // bg-slate-900/33
    glassBorder = Color(0x2EFFFFFF),          // rgba(255, 255, 255, 0.18)
    innerTopHighlight = Color(0x4DFFFFFF),    // inset 0 1px 1px rgba(255, 255, 255, 0.30)
    innerBottomShadow = Color(0x1F000000),    // inset 0 -1px 1px rgba(0, 0, 0, 0.12)
    distanceDropShadow = Color(0x33000000),   // 0 20px 40px rgba(0, 0, 0, 0.20)
    cutoutViewportBg = Color(0xFF04060B),
    cutoutViewportBorder = Color(0x38FFFFFF),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xB8E2E8F0),
    textMuted = Color(0x78CBD5E1),
    signalDot = CyberNothingRed,
    primaryAccent = CyberCyan,
    secondaryAccent = CyberCoral,
    tertiaryAccent = CyberViolet,
    statusGood = Color(0xFF34D399),
    statusWarn = CyberPeach,
    matrixLit = Color(0xFFFFFFFF),
    matrixGhost = Color(0x18FFFFFF),
    microGridDot = Color(0x14FFFFFF)
)

val LightCyberPalette = CyberPalette(
    themeMode = ThemeMode.LIGHT,
    isDark = false,
    isWine = false,
    canvasBg = Color(0xFFF2F5FA),
    glassFill = Color(0xA3FFFFFF),            // Translucent cut quartz over pastel mesh blooms
    glassElevatedFill = Color(0xBDFFFFFF),
    glassReadabilityTint = Color(0x38FFFFFF),
    glassBorder = Color(0x80FFFFFF),
    innerTopHighlight = Color(0xD9FFFFFF),
    innerBottomShadow = Color(0x140F172A),
    distanceDropShadow = Color(0x1F0F172A),
    cutoutViewportBg = Color(0xFF0A0F1D),
    cutoutViewportBorder = Color(0x66FFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xAD1E293B),
    textMuted = Color(0x73334155),
    signalDot = Color(0xFFFF4528),
    primaryAccent = Color(0xFF0284C7),
    secondaryAccent = Color(0xFFE14938),
    tertiaryAccent = Color(0xFF7C3AED),
    statusGood = Color(0xFF059669),
    statusWarn = Color(0xFFD97706),
    matrixLit = Color(0xFF0F172A),
    matrixGhost = Color(0x170F172A),
    microGridDot = Color(0x140F172A)
)

val WineCyberPalette = CyberPalette(
    themeMode = ThemeMode.WINE,
    isDark = true,
    isWine = true,
    canvasBg = Color(0xFF0E0206),             // Deep Cabernet-Obsidian
    glassFill = Color(0x14FFD0DC),            // Wine-tinted 8% sheer spatial crystal
    glassElevatedFill = Color(0x21FFD6E0),    // Elevated claret frost
    glassReadabilityTint = Color(0x6321040D), // Merlot cellar optical backing
    glassBorder = Color(0x36FF9EB5),          // Fine burgundy-blush highlight border
    innerTopHighlight = Color(0x52FFE3EA),    // inset 0 1px 1px blush-white refraction
    innerBottomShadow = Color(0x33000000),    // inset 0 -1px 1px deep cellar shadow
    distanceDropShadow = Color(0x47050002),
    cutoutViewportBg = Color(0xFF080104),
    cutoutViewportBorder = Color(0x45FF8CA6),
    textPrimary = Color(0xFFFFF2F5),
    textSecondary = Color(0xCCF7D9E0),
    textMuted = Color(0x88F2C4D0),
    signalDot = Color(0xFFFF3864),            // Luminous Pinot-Crimson signal dot
    primaryAccent = CyberWineGarnet,
    secondaryAccent = CyberWineCognac,
    tertiaryAccent = Color(0xFFD978B0),
    statusGood = Color(0xFF4ADE80),
    statusWarn = CyberWineCognac,
    matrixLit = Color(0xFFFFEEF2),
    matrixGhost = Color(0x22FF8EA6),
    microGridDot = Color(0x1AFF8CA6)
)

val LocalCyberPalette = staticCompositionLocalOf { DarkCyberPalette }

object CyberTheme {
    val palette: CyberPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalCyberPalette.current
}

fun cyberPaletteFor(themeMode: ThemeMode): CyberPalette = when (themeMode) {
    ThemeMode.LIGHT -> LightCyberPalette
    ThemeMode.DARK -> DarkCyberPalette
    ThemeMode.WINE -> WineCyberPalette
}

/**
 * Provides Cyber-Minimalist / Spatial Glass 2.0 tokens and harmonizes shared dialogs/modals
 * (ObjectDetailModal, MoonDetailWindow, LocationSelectorDialog) when `AppUiStyle.CYBER` is active.
 */
@Composable
fun CyberThemeProvider(
    enabled: Boolean,
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val palette = remember(themeMode) { cyberPaletteFor(themeMode) }
    val baseRedColors = RedTheme.colors
    val adaptedRedColors = remember(baseRedColors, themeMode) {
        when (themeMode) {
            ThemeMode.WINE -> baseRedColors.copy(
                background = Color(0xFF0E0206),
                backgroundSecondary = Color(0xFF19050C),
                backgroundTertiary = Color(0xFF240812),
                surface = Color(0xEB1A050D),
                surfaceElevated = Color(0xF0250914),
                surfaceGrouped = Color(0xE01E0710),
                surfaceVariant = Color(0xCC330D1B),
                border = Color(0x38FF9EB5),
                separator = Color(0x24FF9EB5),
                accentRed = CyberWineCrimson,
                accentRedSubtle = Color(0x33B82245),
                accentGold = CyberWineCognac,
                accentBlue = CyberWineGarnet,
                accentPurple = Color(0xFFD978B0),
                navSurface = Color(0xE61C060E),
                navBorder = Color(0x40FF9EB5)
            )
            ThemeMode.DARK -> baseRedColors.copy(
                background = Color(0xFF07090F),
                backgroundSecondary = Color(0xFF0E131F),
                backgroundTertiary = Color(0xFF161E2E),
                surface = Color(0xEB0E131F),
                surfaceElevated = Color(0xF0151C2C),
                surfaceGrouped = Color(0xE0111726),
                surfaceVariant = Color(0xCC1E293B),
                border = Color(0x2EFFFFFF),
                separator = Color(0x1FFFFFFF),
                accentRed = CyberNothingRed,
                accentRedSubtle = Color(0x29FF3B30),
                accentGold = CyberPeach,
                accentBlue = CyberCyan,
                accentPurple = CyberViolet,
                navSurface = Color(0xE00D121D),
                navBorder = Color(0x33FFFFFF)
            )
            ThemeMode.LIGHT -> baseRedColors.copy(
                background = Color(0xFFF2F5FA),
                backgroundSecondary = Color(0xFFF8FAFC),
                backgroundTertiary = Color(0xFFFFFFFF),
                surface = Color(0xF0FFFFFF),
                surfaceElevated = Color(0xF7FFFFFF),
                surfaceGrouped = Color(0xEBF1F5F9),
                surfaceVariant = Color(0xFFE2E8F0),
                border = Color(0x1F0F172A),
                separator = Color(0x140F172A),
                accentRed = Color(0xFFE14938),
                accentRedSubtle = Color(0x1FE14938),
                accentGold = Color(0xFFD97706),
                accentBlue = Color(0xFF0284C7),
                accentPurple = Color(0xFF7C3AED),
                navSurface = Color(0xEBFFFFFF),
                navBorder = Color(0x80FFFFFF)
            )
        }
    }

    CompositionLocalProvider(
        LocalCyberPalette provides palette,
        LocalRedColors provides adaptedRedColors,
        content = content
    )
}

// ============================================================================
// 1. FROSTED GLASS CONTAINER (SPATIAL GLASS 2.0 WIDGET — 4-LAYER RECIPE)
// ============================================================================

/**
 * Exact 4-Layer Spatial Glassmorphism 2.0 Container (`rounded-[36px]`, high padding):
 * - Semi-transparent fill (`rgba(255, 255, 255, 0.07)`) over optical readability tint
 * - Fine top-weighted highlight border (`1px solid rgba(255, 255, 255, 0.18)`)
 * - Soft distance drop shadow (`0 20px 40px rgba(0, 0, 0, 0.20)`)
 * - Top-inner highlight (`inset 0 1px 1px rgba(255, 255, 255, 0.30)`) +
 *   Bottom-inner shadow (`inset 0 -1px 1px rgba(0, 0, 0, 0.10)`) mimicking thick cut glass.
 */
@Composable
fun CyberGlassWidget(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 36.dp,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    elevation: Dp = 18.dp,
    elevated: Boolean = false,
    ambientTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = CyberTheme.palette
    val shape = squircleShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 400f),
        label = "CyberWidgetScale"
    )

    val borderBrush = remember(palette) {
        Brush.verticalGradient(
            0.0f to palette.innerTopHighlight,
            0.30f to palette.glassBorder,
            0.85f to palette.glassBorder,
            1.0f to palette.glassBorder.copy(alpha = palette.glassBorder.alpha * 0.65f)
        )
    }

    val baseFill = if (elevated) palette.glassElevatedFill else palette.glassFill

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .scale(scale)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = palette.distanceDropShadow,
                        spotColor = palette.distanceDropShadow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(palette.glassReadabilityTint)
            .background(baseFill)
            .drawWithContent {
                // Saturated ambient mesh color bleed inside frosted cut glass
                if (ambientTint != null) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientTint.copy(alpha = if (palette.isDark) 0.18f else 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.86f, size.height * 0.16f),
                            radius = size.maxDimension * 0.72f
                        ),
                        radius = size.maxDimension * 0.72f,
                        center = Offset(size.width * 0.86f, size.height * 0.16f)
                    )
                }
                drawContent()

                // Top-inner refraction highlight: inset 0 1px 1px rgba(255, 255, 255, 0.30)
                val topInset = 1.6.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(palette.innerTopHighlight, Color.Transparent),
                        startY = 0f,
                        endY = topInset * 2.2f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, topInset * 2.2f)
                )

                // Bottom-inner depth shadow: inset 0 -1px 1px rgba(0, 0, 0, 0.10)
                val bottomInset = 2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, palette.innerBottomShadow),
                        startY = size.height - bottomInset * 2f,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, size.height - bottomInset * 2f),
                    size = Size(size.width, bottomInset * 2f)
                )
            }
            .border(BorderStroke(1.dp, borderBrush), shape)
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
// 2. MESH GRADIENT PILL CARDS (SUB-FROSTED VIBRANT INNER ACTION CARDS)
// ============================================================================

/**
 * Smaller inner action card / pill with vibrant multi-color glowing mesh gradients
 * visible *beneath* a frosted glass glaze layer (`rgba(255, 255, 255, 0.14)` glaze + top refraction).
 */
@Composable
fun CyberMeshPillCard(
    preset: CyberMeshPreset,
    modifier: Modifier = Modifier,
    isCapsule: Boolean = true,
    cornerRadius: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val palette = CyberTheme.palette
    val shape: Shape = if (isCapsule) RedShapes.capsule else squircleShape(cornerRadius)
    val colors = if (palette.isWine) preset.wineColors else preset.darkColors
    val bloom = if (palette.isWine) preset.wineBloomColor else preset.bloomColor

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "CyberMeshPillScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = colors.first().copy(alpha = 0.26f),
                spotColor = colors.last().copy(alpha = 0.28f)
            )
            .clip(shape)
            .drawBehind {
                // Underlying vibrant multi-color mesh gradient
                drawRect(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(bloom, Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.20f),
                        radius = size.maxDimension * 0.78f
                    ),
                    radius = size.maxDimension * 0.78f,
                    center = Offset(size.width * 0.22f, size.height * 0.20f)
                )
                // Frosted glass glaze veiled *over* the mesh gradient
                drawRect(color = Color.White.copy(alpha = 0.12f))
                drawRect(color = Color.Black.copy(alpha = if (palette.isWine) 0.14f else 0.10f))
            }
            .drawWithContent {
                drawContent()
                val topH = 1.5.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                        startY = 0f,
                        endY = topH * 2f
                    ),
                    size = Size(size.width, topH * 2f)
                )
            }
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.52f),
                            Color.White.copy(alpha = 0.22f)
                        )
                    )
                ),
                shape
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
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

// ============================================================================
// 3. SPATIAL FLOATING CAPSULE BAR & PILL CHIPS
// ============================================================================

/**
 * Seamless rounded pill container for dynamic status indicators, floating top bars,
 * and segmented capsule controls.
 */
@Composable
fun CyberFloatingCapsuleBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    elevation: Dp = 12.dp,
    isActive: Boolean = false,
    accentColor: Color = CyberTheme.palette.primaryAccent,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val palette = CyberTheme.palette
    val shape = RedShapes.capsule
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 430f),
        label = "CyberCapsuleScale"
    )

    val borderBrush = remember(palette, isActive, accentColor) {
        if (isActive) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.58f),
                    accentColor.copy(alpha = 0.45f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(palette.innerTopHighlight, palette.glassBorder)
            )
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = palette.distanceDropShadow,
                        spotColor = palette.distanceDropShadow
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(palette.glassReadabilityTint)
            .background(
                if (isActive) accentColor.copy(alpha = if (palette.isDark) 0.22f else 0.16f)
                else palette.glassElevatedFill
            )
            .drawWithContent {
                drawContent()
                val topH = 1.4.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(palette.innerTopHighlight, Color.Transparent),
                        startY = 0f,
                        endY = topH * 2f
                    ),
                    size = Size(size.width, topH * 2f)
                )
            }
            .border(BorderStroke(1.dp, borderBrush), shape)
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

// ============================================================================
// 4. CUTOUT VIEWPORT WINDOW CARD (CRISP UNBLURRED WINDOW + FROSTED FRAME)
// ============================================================================

/**
 * Signature Cutout Viewport Window Card:
 * Thick-cut `36.dp` Spatial Glass 2.0 widget housing a crisp, unblurred Oval / Capsule / Squircle
 * Cutout Viewport Window at the top with a recessed optical aperture rim, while framing ultra-thin
 * geometric typography, dot-matrix counters, and mesh gradient pills below it in frosted glass.
 */
@Composable
fun CyberCutoutViewportCard(
    modifier: Modifier = Modifier,
    viewportHeight: Dp = 128.dp,
    isCapsuleCutout: Boolean = true,
    viewportCornerRadius: Dp = 26.dp,
    ambientTint: Color? = null,
    onClick: (() -> Unit)? = null,
    viewportContent: @Composable BoxScope.() -> Unit,
    bodyContent: @Composable ColumnScope.() -> Unit
) {
    val palette = CyberTheme.palette
    val cutoutShape: Shape = if (isCapsuleCutout) RedShapes.capsule else squircleShape(viewportCornerRadius)

    CyberGlassWidget(
        modifier = modifier,
        cornerRadius = 36.dp,
        contentPadding = PaddingValues(16.dp),
        elevation = 18.dp,
        ambientTint = ambientTint,
        onClick = onClick
    ) {
        // Recessed Crisp Cutout Viewport Window
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewportHeight)
                .clip(cutoutShape)
                .background(palette.cutoutViewportBg)
                .drawWithContent {
                    drawContent()
                    // Inner aperture top shadow + bottom glass rim catchlight
                    val rimPx = 3.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent),
                            startY = 0f,
                            endY = rimPx * 2.5f
                        ),
                        size = Size(size.width, rimPx * 2.5f)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.14f)),
                            startY = size.height - rimPx * 1.6f,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - rimPx * 1.6f),
                        size = Size(size.width, rimPx * 1.6f)
                    )
                }
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                palette.cutoutViewportBorder,
                                Color.White.copy(alpha = 0.35f)
                            )
                        )
                    ),
                    cutoutShape
                ),
            contentAlignment = Alignment.Center,
            content = viewportContent
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Frosted Glass Framed Typography & Dot-Matrix Body Below the Cutout Window
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            content = bodyContent
        )
    }
}

// ============================================================================
// 5. NOTHING / TEENAGE ENGINEERING MICRO-CONTROLS (SWITCH, SLIDER, ICON PILL)
// ============================================================================

@Composable
fun CyberSignalDot(
    color: Color = CyberTheme.palette.signalDot,
    size: Dp = 7.dp,
    glowing: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (glowing) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
                                center = center,
                                radius = this.size.minDimension * 1.35f
                            ),
                            radius = this.size.minDimension * 1.35f,
                            center = center
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun CyberMicroIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 18.dp,
    isActive: Boolean = false,
    accentColor: Color = CyberTheme.palette.primaryAccent
) {
    val palette = CyberTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "CyberIconBtnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(palette.glassReadabilityTint)
            .background(
                if (isActive) accentColor.copy(alpha = 0.24f) else palette.glassElevatedFill
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        if (isActive) listOf(Color.White.copy(alpha = 0.65f), accentColor.copy(alpha = 0.45f))
                        else listOf(palette.innerTopHighlight, palette.glassBorder)
                    )
                ),
                CircleShape
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
            tint = if (isActive) accentColor else palette.textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CyberGlowSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = CyberTheme.palette.primaryAccent
) {
    val palette = CyberTheme.palette
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "CyberSwitchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.36f) else palette.glassFill,
        animationSpec = tween(180),
        label = "CyberSwitchTrack"
    )

    Box(
        modifier = modifier
            .width(50.dp)
            .height(28.dp)
            .clip(RedShapes.capsule)
            .background(palette.glassReadabilityTint)
            .background(trackColor)
            .border(
                1.dp,
                Brush.verticalGradient(
                    if (checked) listOf(Color.White.copy(alpha = 0.75f), accentColor.copy(alpha = 0.55f))
                    else listOf(palette.innerTopHighlight, palette.glassBorder)
                ),
                RedShapes.capsule
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(palette.signalDot)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color = CyberTheme.palette.primaryAccent,
    secondaryColor: Color = CyberTheme.palette.secondaryAccent
) {
    val palette = CyberTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val active = isPressed || isDragged

    val thumbSize by animateDpAsState(
        targetValue = if (active) 25.dp else 21.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "CyberSliderThumb"
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
                    .size(32.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = if (active) 0.55f else 0.32f),
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
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(thumbSize)
                        .shadow(5.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(palette.signalDot)
                    )
                }
            }
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
            ) {
                val trackHeight = size.height
                val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                drawRoundRect(
                    color = if (palette.isDark) Color(0x26FFFFFF) else Color(0x220F172A),
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
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(accentColor, secondaryColor),
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

@Composable
fun CyberSectionHeader(
    title: String,
    subtitle: String? = null,
    tagText: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = CyberTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberSignalDot(size = 6.dp)
                Text(
                    text = title,
                    style = RedTypographyTokens.sectionTitle.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 17.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = palette.textPrimary
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp
                    ),
                    color = palette.textSecondary
                )
            }
        }
        if (tagText != null) {
            CyberFloatingCapsuleBar(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                elevation = 4.dp,
                isActive = true
            ) {
                CyberMatrixText(
                    text = tagText,
                    dotSize = 1.65.dp,
                    dotGap = 0.65.dp,
                    charGap = 2.0.dp,
                    activeColor = palette.matrixLit,
                    glowColor = palette.primaryAccent
                )
            }
        }
    }
}
