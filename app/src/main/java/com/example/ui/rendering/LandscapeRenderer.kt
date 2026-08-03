package com.example.ui.rendering

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

        // Bottom 28% of canvas reserved for layered mountain landscape
        val horizonY = height * 0.72f

        // Atmospheric Horizon Mist / Fog Layer (Glows warm during sunset/twilight, cosmic navy at night)
        val mistColor = when {
            lightingState.sunAltitudeDeg > 6.0 -> Color(0xFF93C5FD).copy(alpha = 0.25f)
            lightingState.sunAltitudeDeg in 0.0..6.0 -> Color(0xFFF59E0B).copy(alpha = 0.45f)
            lightingState.sunAltitudeDeg in -6.0..0.0 -> Color(0xFFC084FC).copy(alpha = 0.35f)
            lightingState.sunAltitudeDeg in -12.0..-6.0 -> Color(0xFF818CF8).copy(alpha = 0.25f)
            else -> Color(0xFF38BDF8).copy(alpha = 0.12f)
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, mistColor, mistColor.copy(alpha = 0.6f)),
                startY = horizonY - 40f,
                endY = horizonY + 30f
            ),
            topLeft = Offset(0f, horizonY - 40f),
            size = drawScope.size.copy(height = 70f)
        )

        // --- LAYER 1: Distant Mountain Range (Soft atmospheric perspective, 30% opacity) ---
        val layer1Path = Path()
        layer1Path.moveTo(0f, height)
        layer1Path.lineTo(0f, horizonY)

        val layer1Points = 20
        val stepX1 = width / layer1Points
        for (i in 0..layer1Points) {
            val x = i * stepX1
            // Procedural mountain peak heights
            val peakH = sin(i * 0.5f) * 28f + sin(i * 1.3f) * 16f
            val y = horizonY - peakH
            layer1Path.lineTo(x, y)
        }
        layer1Path.lineTo(width, height)
        layer1Path.close()

        val layer1Color = lightingState.horizonTone.copy(alpha = 0.45f)
        drawScope.drawPath(path = layer1Path, color = layer1Color)

        // --- LAYER 2: Midground Mountain Ridge (Sharper silhouettes, 65% opacity) ---
        val layer2Path = Path()
        layer2Path.moveTo(0f, height)
        val startY2 = horizonY + 15f
        layer2Path.lineTo(0f, startY2)

        val layer2Points = 16
        val stepX2 = width / layer2Points
        for (i in 0..layer2Points) {
            val x = i * stepX2
            val peakH = sin(i * 0.8f + 1.2f) * 22f + sin(i * 2.1f) * 12f
            val y = startY2 - peakH
            layer2Path.lineTo(x, y)
        }
        layer2Path.lineTo(width, height)
        layer2Path.close()

        val layer2Color = lightingState.horizonTone.copy(alpha = 0.75f)
        drawScope.drawPath(path = layer2Path, color = layer2Color)

        // --- LAYER 3: Foreground Landscape Silhouette (Dark solid silhouette at bottom) ---
        val layer3Path = Path()
        layer3Path.moveTo(0f, height)
        val startY3 = horizonY + 35f
        layer3Path.lineTo(0f, startY3)

        val layer3Points = 12
        val stepX3 = width / layer3Points
        for (i in 0..layer3Points) {
            val x = i * stepX3
            val peakH = sin(i * 1.1f + 2.5f) * 18f + sin(i * 3.2f) * 8f
            val y = startY3 - peakH
            layer3Path.lineTo(x, y)
        }
        layer3Path.lineTo(width, height)
        layer3Path.close()

        val darkSilhouetteColor = Color(0xFF030712)
        drawScope.drawPath(path = layer3Path, color = darkSilhouetteColor)

        // --- CITY LIGHTS / HORIZON GLOW AT NIGHT ---
        if (lightingState.sunAltitudeDeg < -6.0) {
            val cityGlowX1 = width * 0.28f
            val cityGlowX2 = width * 0.72f

            // Faint pulsing amber/gold city ambient light
            val glowPulse = 0.85f + 0.15f * sin(frameTimeMs * 0.001f).toFloat()

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.18f * glowPulse),
                        Color(0xFFFBBF24).copy(alpha = 0.06f * glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(cityGlowX1, startY3),
                    radius = 80f
                ),
                radius = 80f,
                center = Offset(cityGlowX1, startY3)
            )

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.15f * glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(cityGlowX2, startY3 + 10f),
                    radius = 70f
                ),
                radius = 70f,
                center = Offset(cityGlowX2, startY3 + 10f)
            )
        }
    }
}
