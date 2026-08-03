package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.SkyCanvasTheme

object AtmosphereRenderer {

    fun drawAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?,
        theme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL
    ) {
        when (theme) {
            SkyCanvasTheme.CELESTIAL -> drawCelestialAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.MONOCHROME -> drawMonochromeAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.FUN -> drawFunAtmosphere(drawScope, lightingState, sunPosPx)
        }
    }

    private fun drawCelestialAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Procedural Vertical Gradient
        val skyGradient = Brush.verticalGradient(
            colors = listOf(
                lightingState.skyTone,
                lightingState.horizonTone
            )
        )
        drawScope.drawRect(
            brush = skyGradient,
            size = drawScope.size
        )

        // 2. Solar Atmospheric Scatter Flare
        if (sunPosPx != null && lightingState.sunAltitudeDeg > -10.0) {
            val sunScatterRadius = (height * 0.9f).coerceAtLeast(150f)
            val scatterAlpha = when {
                lightingState.sunAltitudeDeg > 6.0 -> 0.45f
                lightingState.sunAltitudeDeg > 0.0 -> 0.70f
                else -> 0.35f
            }

            val scatterColors = if (lightingState.sunAltitudeDeg > 0.0) {
                listOf(
                    Color(0xFFFEF3C7).copy(alpha = scatterAlpha),
                    Color(0xFFFBBF24).copy(alpha = scatterAlpha * 0.5f),
                    Color(0xFFF59E0B).copy(alpha = scatterAlpha * 0.2f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFFC084FC).copy(alpha = scatterAlpha),
                    Color(0xFF818CF8).copy(alpha = scatterAlpha * 0.4f),
                    Color.Transparent
                )
            }

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = scatterColors,
                    center = sunPosPx,
                    radius = sunScatterRadius
                ),
                radius = sunScatterRadius,
                center = sunPosPx
            )
        }

        // 3. Horizon Haze Layer
        val hazeHeight = height * 0.22f
        val hazeBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                lightingState.horizonTone.copy(alpha = 0.35f)
            ),
            startY = height - hazeHeight,
            endY = height
        )
        drawScope.drawRect(
            brush = hazeBrush,
            topLeft = Offset(0f, height - hazeHeight),
            size = drawScope.size.copy(height = hazeHeight)
        )
    }

    private fun drawMonochromeAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val height = drawScope.size.height
        val sunAlt = lightingState.sunAltitudeDeg

        // Editorial monochrome palette transitions
        val (zenithColor, horizonColor) = when {
            sunAlt > 12.0 -> Pair(Color(0xFFFFFFFF), Color(0xFFF8FAFC)) // Midday: Pure clean white
            sunAlt > 0.0 -> Pair(Color(0xFFF1F5F9), Color(0xFFCBD5E1))  // Golden Hour: Soft warm white into soft grey
            sunAlt > -6.0 -> Pair(Color(0xFF94A3B8), Color(0xFF334155)) // Sunset/Civil Twilight: Soft grey to dark charcoal
            sunAlt > -12.0 -> Pair(Color(0xFF334155), Color(0xFF0F172A))// Nautical Twilight: Dark charcoal to near black
            else -> Pair(Color(0xFF000000), Color(0xFF0F172A))          // Astronomical Night: Pure rich black
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(colors = listOf(zenithColor, horizonColor)),
            size = drawScope.size
        )

        // Subtle soft monochromatic halo around Sun if visible
        if (sunPosPx != null && sunAlt > -6.0) {
            val haloRadius = (height * 0.7f).coerceAtLeast(100f)
            val haloColor = if (sunAlt > 0.0) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor, Color.Transparent),
                    center = sunPosPx,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = sunPosPx
            )
        }
    }

    private fun drawFunAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val sunAlt = lightingState.sunAltitudeDeg

        // Crayon paper background sky colors
        val (topColor, bottomColor) = when {
            sunAlt > 6.0 -> Pair(Color(0xFF38BDF8), Color(0xFFBAE6FD))  // Bright Crayon Sky Blue
            sunAlt > 0.0 -> Pair(Color(0xFFFB923C), Color(0xFFFDE047))  // Warm Orange/Yellow Sunset Crayon
            sunAlt > -6.0 -> Pair(Color(0xFFF472B6), Color(0xFFFB923C)) // Soft Magenta to Orange Dusk Crayon
            else -> Pair(Color(0xFF1E1B4B), Color(0xFF312E81))          // Deep Crayon Night Indigo
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(colors = listOf(topColor, bottomColor)),
            size = drawScope.size
        )

        // Hand-drawn crayon texture cross-hatch stippling lines overlaying sky
        val crayonStrokeColor = if (sunAlt > 0.0) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f)
        val stepY = 28f
        var y = 14f
        while (y < height) {
            val strokePath = Path().apply {
                var x = 0f
                moveTo(x, y)
                while (x < width) {
                    val nextX = (x + 35f).coerceAtMost(width)
                    val wobbleY = y + if (((x / 35).toInt() % 2) == 0) 3f else -3f
                    lineTo(nextX, wobbleY)
                    x = nextX
                }
            }
            drawScope.drawPath(
                path = strokePath,
                color = crayonStrokeColor,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
            y += stepY
        }
    }
}
