package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

import com.alijafari.red.astronomy.domain.SkyCanvasTheme

object LandscapeRenderer {

    private fun buildMountainPath(width: Float, height: Float, baseHorizonY: Float, peakMaxHeight: Float, phaseShift: Float): Path {
        return Path().apply {
            moveTo(0f, height)
            val points = 32
            val stepX = width / points

            val y0 = baseHorizonY - (sin(phaseShift) * peakMaxHeight * 0.4f).coerceAtLeast(0f)
            lineTo(0f, y0)

            var prevX = 0f
            var prevY = y0

            for (i in 1..points) {
                val x = i * stepX
                val progress = x / width
                val angle1 = progress * Math.PI.toFloat() * 4f + phaseShift
                val angle2 = progress * Math.PI.toFloat() * 8.5f + phaseShift * 1.5f
                val h = (sin(angle1) * 0.65f + sin(angle2) * 0.35f).coerceAtLeast(-0.1f) * peakMaxHeight
                val y = baseHorizonY - h
                val midX = (prevX + x) / 2f
                val midY = (prevY + y) / 2f
                quadraticTo(prevX, prevY, midX, midY)
                prevX = x
                prevY = y
            }
            lineTo(width, prevY)
            lineTo(width, height)
            close()
        }
    }

    fun drawHorizonLandscape(
        drawScope: DrawScope,
        lightingState: LightingState,
        frameTimeMs: Long,
        theme: SkyCanvasTheme = SkyCanvasTheme.ATMOSPHERIC_SKY
    ) {
        if (theme == SkyCanvasTheme.PAPERCRAFT_DIORAMA) {
            drawPapercraftDioramaLandscape(drawScope, lightingState)
            return
        }

        val width = drawScope.size.width
        val height = drawScope.size.height

        // Reduced visible height: Repositioned lower at 85% of screen height
        val baseHorizonY = height * 0.85f

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
                startY = baseHorizonY - 25f,
                endY = baseHorizonY + 15f
            ),
            topLeft = Offset(0f, baseHorizonY - 25f),
            size = drawScope.size.copy(height = 40f)
        )

        // --- LAYER 1: Distant Mountain Range (Soft atmospheric perspective) ---
        val layer1Path = buildMountainPath(width, height, baseHorizonY, peakMaxHeight = 15f, phaseShift = 0f)
        val layer1Color = lightingState.horizonTone.copy(alpha = 0.45f)
        drawScope.drawPath(path = layer1Path, color = layer1Color)

        // --- LAYER 2: Midground Mountain Ridge ---
        val layer2Path = buildMountainPath(width, height, baseHorizonY + 10f, peakMaxHeight = 11f, phaseShift = 1.3f)
        val layer2Color = lightingState.horizonTone.copy(alpha = 0.75f)
        drawScope.drawPath(path = layer2Path, color = layer2Color)

        // --- LAYER 3: Foreground Landscape Silhouette (Dark solid vector) ---
        val layer3Path = buildMountainPath(width, height, baseHorizonY + 20f, peakMaxHeight = 8f, phaseShift = 2.5f)
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
                    center = Offset(cityGlowX1, baseHorizonY + 20f),
                    radius = 50f
                ),
                radius = 50f,
                center = Offset(cityGlowX1, baseHorizonY + 20f)
            )

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.14f * glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(cityGlowX2, baseHorizonY + 25f),
                    radius = 45f
                ),
                radius = 45f,
                center = Offset(cityGlowX2, baseHorizonY + 25f)
            )
        }
    }

    private fun drawPapercraftDioramaLandscape(
        drawScope: DrawScope,
        lightingState: LightingState
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val baseHorizonY = height * 0.82f

        val shadowColor = Color(0x38221B19)
        val shadowOffset = Offset(4f, 6f)

        // LAYER 1: Far Background Mountain Ridge (Soft Pastel Lavender/Sage)
        val layer1Path = buildMountainPath(width, height, baseHorizonY - 18f, peakMaxHeight = 28f, phaseShift = 0.5f)
        val layer1Color = Color(0xFFA3B18A) // Soft pastel sage
        // Shadow
        drawScope.drawPath(
            path = layer1Path,
            color = shadowColor,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        // Cardstock
        drawScope.drawPath(path = layer1Path, color = layer1Color)
        drawScope.drawPath(path = layer1Path, color = Color(0x22000000), style = Stroke(width = 1.2f))

        // LAYER 2: Midground Mountain Ridge (Warm Pastel Terracotta / Sand)
        val layer2Path = buildMountainPath(width, height, baseHorizonY - 5f, peakMaxHeight = 22f, phaseShift = 1.8f)
        val layer2Color = Color(0xFFD4A373) // Warm muted terracotta sand
        // Shadow offset
        drawScope.drawPath(
            path = layer2Path,
            color = shadowColor
        )
        // Cardstock
        drawScope.drawPath(path = layer2Path, color = layer2Color)
        drawScope.drawPath(path = layer2Path, color = Color(0x22000000), style = Stroke(width = 1.2f))

        // LAYER 3: Rolling Foreground Paper Hills (Pastel Forest Green)
        val layer3Path = buildMountainPath(width, height, baseHorizonY + 12f, peakMaxHeight = 16f, phaseShift = 3.2f)
        val layer3Color = Color(0xFF588157) // Soft pastel eucalyptus forest
        // Shadow offset
        drawScope.drawPath(
            path = layer3Path,
            color = shadowColor
        )
        // Cardstock
        drawScope.drawPath(path = layer3Path, color = layer3Color)
        drawScope.drawPath(path = layer3Path, color = Color(0x22000000), style = Stroke(width = 1.2f))

        // LAYER 4: Front Base Frame Cardstock Edge (Rich Dark Sepia/Slate)
        val layer4Path = Path().apply {
            moveTo(0f, baseHorizonY + 28f)
            lineTo(width, baseHorizonY + 28f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        val layer4Color = Color(0xFF3A405A) // Dark slate paper base
        drawScope.drawPath(path = layer4Path, color = layer4Color)
        drawScope.drawPath(path = layer4Path, color = Color(0x33000000), style = Stroke(width = 1.5f))
    }
}
