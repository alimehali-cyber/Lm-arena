package com.alijafari.red.astronomy.ui.spatial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.alijafari.red.astronomy.domain.ThemeMode
import kotlin.math.sin

/**
 * VisionOS Ethereal Desert Dreamscape & Ambient Mesh Atmosphere.
 *
 * When the user has no custom Live Sky / Photo backdrop active ([isOpaqueDreamscape] = true),
 * paints a serene, multi-layered warm desert horizon beneath diffused pastel neon mesh spheres
 * (`#FFB0D0` rose, warm peach, celestial iris) so translucent frosted glass cards float with
 * genuine spatial depth.
 *
 * When Live Sky or a Photo backdrop IS active ([isOpaqueDreamscape] = false), overlays only a
 * delicate ambient color-bleed veil so the live sky / photograph refracts through the cards.
 */
@Composable
fun SpatialAtmosphereBackground(
    themeMode: ThemeMode,
    isOpaqueDreamscape: Boolean,
    sunAltitudeDeg: Double = -18.0,
    modifier: Modifier = Modifier
) {
    val isWine = themeMode == ThemeMode.WINE
    val isDark = themeMode != ThemeMode.LIGHT

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (!isOpaqueDreamscape) {
            val primaryBleed = if (isWine) Color(0xFFE03E60) else SpatialNeonRose
            val secondaryBleed = if (isWine) Color(0xFF9E1B38) else SpatialWarmPeach
            // Subtle ethereal ambient mesh glow over LiveSky / PhotoBackdrop
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryBleed.copy(alpha = if (isWine) 0.22f else if (isDark) 0.14f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.82f, h * 0.16f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.82f, h * 0.16f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryBleed.copy(alpha = if (isWine) 0.18f else if (isDark) 0.11f else 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.18f, h * 0.62f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * 0.18f, h * 0.62f)
            )
            return@Canvas
        }

        // 1. Sky Zenith-to-Horizon Dreamscape Gradient
        val twilightFactor = ((sunAltitudeDeg + 18.0) / 24.0).coerceIn(0.0, 1.0).toFloat()
        val skyColors = when (themeMode) {
            ThemeMode.WINE -> listOf(
                Color(0xFF0E0206),                                  // Cabernet midnight zenith
                Color(0xFF1F060F),                                  // Deep merlot upper sky
                Color(0xFF380B1A),                                  // Rich burgundy mid-sky
                lerpAtmo(Color(0xFF591329), Color(0xFF7A1C38), twilightFactor), // Claret wine horizon glow
                Color(0xFF21060E)                                   // Burgundy cellar shadow base
            )
            ThemeMode.DARK -> listOf(
                Color(0xFF0E0B1A),                                  // Deep cosmic zenith
                Color(0xFF1A132B),                                  // Indigo-plum upper sky
                Color(0xFF2D1C3B),                                  // Warm amethyst mid-sky
                lerpAtmo(Color(0xFF4B2846), Color(0xFF6B3552), twilightFactor), // Rose-dusk horizon glow
                Color(0xFF241529)                                   // Desert shadow base
            )
            ThemeMode.LIGHT -> listOf(
                Color(0xFFF6ECE6),                                  // Warm alabaster sky
                Color(0xFFF9E4DC),                                  // Soft blush quartz
                Color(0xFFFCD8C8),                                  // Warm peach horizon mist
                Color(0xFFF2C6B2),                                  // Sunlit terracotta haze
                Color(0xFFE5B8A1)                                   // Warm desert sand base
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = skyColors,
                startY = 0f,
                endY = h
            )
        )

        // 2. Ambient Pastel Neon Mesh Glow Orbs (behind frosted glass widgets)
        val topRightBloom = if (isWine) Color(0xFFE8486B) else SpatialNeonRose
        val midLeftBloom = if (isWine) Color(0xFF9C1D3B) else SpatialCosmicIris
        val horizonPrimaryBloom = if (isWine) Color(0xFFD93B5C) else SpatialWarmPeach
        val horizonSecondaryBloom = if (isWine) Color(0xFFE29B6B) else SpatialSunsetCoral

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    topRightBloom.copy(alpha = if (isWine) 0.28f else if (isDark) 0.24f else 0.34f),
                    Color.Transparent
                ),
                center = Offset(w * 0.84f, h * 0.18f),
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = Offset(w * 0.84f, h * 0.18f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    midLeftBloom.copy(alpha = if (isWine) 0.30f else 0.20f),
                    Color.Transparent
                ),
                center = Offset(w * 0.14f, h * 0.38f),
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = Offset(w * 0.14f, h * 0.38f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    horizonPrimaryBloom.copy(alpha = if (isWine) 0.28f else if (isDark) 0.26f else 0.44f),
                    horizonSecondaryBloom.copy(alpha = if (isWine) 0.12f else if (isDark) 0.10f else 0.18f),
                    Color.Transparent
                ),
                center = Offset(w * 0.56f, h * 0.68f),
                radius = w * 0.82f
            ),
            radius = w * 0.82f,
            center = Offset(w * 0.56f, h * 0.68f)
        )

        // 3. Delicate Starfield Pinpoints in Dark & Wine Dreamscape Modes
        if (isDark) {
            val starCount = 34
            val starColor = if (isWine) Color(0xFFFFE6EC) else Color(0xFFFFF5FA)
            for (i in 0 until starCount) {
                val fx = ((sin(i * 12.9898 + 4.1414) * 43758.5453) % 1.0 + 1.0) % 1.0
                val fy = (((sin(i * 78.233 + 1.732) * 24634.6345) % 1.0 + 1.0) % 1.0) * 0.62
                val alpha = (0.16f + (i % 5) * 0.08f) * (1f - fy.toFloat() * 0.9f)
                val radius = if (i % 7 == 0) 1.6f else 1.05f
                drawCircle(
                    color = starColor.copy(alpha = alpha.coerceIn(0.05f, 0.55f)),
                    radius = radius,
                    center = Offset((fx * w).toFloat(), (fy * h).toFloat())
                )
            }
        }

        // 4. Organic Minimalist Desert Sand Dunes (3 soft continuous Bezier ridges)
        val distantDune = Path().apply {
            moveTo(0f, h * 0.73f)
            cubicTo(
                w * 0.28f, h * 0.67f,
                w * 0.62f, h * 0.76f,
                w, h * 0.70f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = distantDune,
            brush = Brush.verticalGradient(
                colors = when (themeMode) {
                    ThemeMode.WINE -> listOf(Color(0x994A0F22), Color(0xD9240711))
                    ThemeMode.DARK -> listOf(Color(0x883A2139), Color(0xCC1E1326))
                    ThemeMode.LIGHT -> listOf(Color(0xCCEAB69E), Color(0xFFDDA287))
                },
                startY = h * 0.67f,
                endY = h
            )
        )

        val midDune = Path().apply {
            moveTo(0f, h * 0.80f)
            cubicTo(
                w * 0.35f, h * 0.85f,
                w * 0.68f, h * 0.74f,
                w, h * 0.79f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = midDune,
            brush = Brush.verticalGradient(
                colors = when (themeMode) {
                    ThemeMode.WINE -> listOf(Color(0xB8360A19), Color(0xEE19040C))
                    ThemeMode.DARK -> listOf(Color(0xAA2B182E), Color(0xEE160F20))
                    ThemeMode.LIGHT -> listOf(Color(0xD9E2A58B), Color(0xFFCF9076))
                },
                startY = h * 0.74f,
                endY = h
            )
        )

        val foregroundDune = Path().apply {
            moveTo(0f, h * 0.87f)
            cubicTo(
                w * 0.25f, h * 0.83f,
                w * 0.72f, h * 0.90f,
                w, h * 0.85f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = foregroundDune,
            brush = Brush.verticalGradient(
                colors = when (themeMode) {
                    ThemeMode.WINE -> listOf(Color(0xD9260712), Color(0xFF120308))
                    ThemeMode.DARK -> listOf(Color(0xCC211327), Color(0xFF120B1B))
                    ThemeMode.LIGHT -> listOf(Color(0xFFE7B39C), Color(0xFFD59A80))
                },
                startY = h * 0.83f,
                endY = h
            )
        )
    }
}

private fun lerpAtmo(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)
