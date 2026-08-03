package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.pow

object AtmosphereRenderer {

    fun drawAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Procedural Vertical Gradient (Zenith to Horizon Rayleigh scattering)
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

        // 2. Solar Atmospheric Scatter Flare (Rayleigh/Mie Scattering around Sun position)
        if (sunPosPx != null && lightingState.sunAltitudeDeg > -10.0) {
            val sunScatterRadius = (height * 0.9f).coerceAtLeast(150f)
            val scatterAlpha = when {
                lightingState.sunAltitudeDeg > 6.0 -> 0.45f
                lightingState.sunAltitudeDeg > 0.0 -> 0.70f // Golden hour warm haze
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

        // 3. Horizon Haze Layer (Mie Scattering near ground)
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
}
