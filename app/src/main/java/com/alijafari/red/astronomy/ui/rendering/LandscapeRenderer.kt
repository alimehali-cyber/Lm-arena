package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

object LandscapeRenderer {

    fun drawHorizonLandscape(
        drawScope: DrawScope,
        lightingState: LightingState,
        frameTimeMs: Long
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // Bottom 28% reserved for vector mountain landscape
        val horizonY = height * 0.72f

        // 1. Atmospheric Horizon Mist / Fog Layer
        val mistColor = when {
            lightingState.sunAltitudeDeg > 6.0 -> Color(0xFF38BDF8).copy(alpha = 0.22f)
            lightingState.sunAltitudeDeg in 0.0..6.0 -> Color(0xFFF59E0B).copy(alpha = 0.40f)
            lightingState.sunAltitudeDeg in -6.0..0.0 -> Color(0xFFC084FC).copy(alpha = 0.30f)
            lightingState.sunAltitudeDeg in -12.0..-6.0 -> Color(0xFF818CF8).copy(alpha = 0.20f)
            else -> Color(0xFF38BDF8).copy(alpha = 0.10f)
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, mistColor, mistColor.copy(alpha = 0.5f)),
                startY = horizonY - 45f,
                endY = horizonY + 25f
            ),
            topLeft = Offset(0f, horizonY - 45f),
            size = drawScope.size.copy(height = 70f)
        )

        // --- LAYER 1: Distant Mountain Range (Soft atmospheric perspective, 45% opacity) ---
        val layer1Path = Path().apply {
            moveTo(0f, height)
            lineTo(0f, horizonY)

            var prevX = 0f
            var prevY = horizonY
            val points = 16
            val stepX = width / points

            for (i in 1..points) {
                val x = i * stepX
                val peakH = sin(i * 0.6f) * 26f + sin(i * 1.4f) * 14f
                val y = horizonY - peakH
                val midX = (prevX + x) / 2f
                val midY = (prevY + y) / 2f
                quadraticTo(prevX, prevY, midX, midY)
                prevX = x
                prevY = y
            }
            lineTo(width, horizonY)
            lineTo(width, height)
            close()
        }

        val layer1Color = lightingState.horizonTone.copy(alpha = 0.45f)
        drawScope.drawPath(path = layer1Path, color = layer1Color)

        // --- LAYER 2: Midground Mountain Ridge (Medium silhouette, 75% opacity) ---
        val layer2Path = Path().apply {
            moveTo(0f, height)
            val startY2 = horizonY + 14f
            lineTo(0f, startY2)

            var prevX = 0f
            var prevY = startY2
            val points = 12
            val stepX = width / points

            for (i in 1..points) {
                val x = i * stepX
                val peakH = sin(i * 0.9f + 1.2f) * 20f + sin(i * 2.2f) * 10f
                val y = startY2 - peakH
                val midX = (prevX + x) / 2f
                val midY = (prevY + y) / 2f
                quadraticTo(prevX, prevY, midX, midY)
                prevX = x
                prevY = y
            }
            lineTo(width, height)
            close()
        }

        val layer2Color = lightingState.horizonTone.copy(alpha = 0.75f)
        drawScope.drawPath(path = layer2Path, color = layer2Color)

        // --- LAYER 3: Foreground Landscape Silhouette (Dark solid vector silhouette) ---
        val layer3Path = Path().apply {
            moveTo(0f, height)
            val startY3 = horizonY + 32f
            lineTo(0f, startY3)

            var prevX = 0f
            var prevY = startY3
            val points = 10
            val stepX = width / points

            for (i in 1..points) {
                val x = i * stepX
                val peakH = sin(i * 1.2f + 2.4f) * 16f + sin(i * 3.1f) * 7f
                val y = startY3 - peakH
                val midX = (prevX + x) / 2f
                val midY = (prevY + y) / 2f
                quadraticTo(prevX, prevY, midX, midY)
                prevX = x
                prevY = y
            }
            lineTo(width, height)
            close()
        }

        val darkSilhouetteColor = Color(0xFF020617)
        drawScope.drawPath(path = layer3Path, color = darkSilhouetteColor)

        // --- CITY AMBIENT LIGHTS AT NIGHT ---
        if (lightingState.sunAltitudeDeg < -6.0) {
            val cityGlowX1 = width * 0.30f
            val cityGlowX2 = width * 0.70f
            val glowPulse = 0.85f + 0.15f * sin(frameTimeMs * 0.0012f).toFloat()

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.18f * glowPulse),
                        Color(0xFFFBBF24).copy(alpha = 0.05f * glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(cityGlowX1, horizonY + 30f),
                    radius = 75f
                ),
                radius = 75f,
                center = Offset(cityGlowX1, horizonY + 30f)
            )

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.14f * glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(cityGlowX2, horizonY + 35f),
                    radius = 65f
                ),
                radius = 65f,
                center = Offset(cityGlowX2, horizonY + 35f)
            )
        }
    }
}
