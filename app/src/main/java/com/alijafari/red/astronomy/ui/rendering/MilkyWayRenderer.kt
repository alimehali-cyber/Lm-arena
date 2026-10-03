package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.astro_engine.GalacticEngine
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import kotlin.math.abs
import kotlin.math.sin

object MilkyWayRenderer {

    fun drawMilkyWay(
        drawScope: DrawScope,
        galacticPoints: List<GalacticEngine.GalacticPlanePoint>,
        lightingState: LightingState,
        frameTimeMs: Long,
        theme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL,
        latitudeDeg: Double = 0.0
    ) {
        if (galacticPoints.size < 2 || lightingState.sunAltitudeDeg > -5.0) return

        val width = drawScope.size.width
        val height = drawScope.size.height

        val mwPath = Path()
        var first = true

        val baseAlpha = ((abs(lightingState.sunAltitudeDeg) - 5.0) / 13.0).coerceIn(0.0, 1.0).toFloat() * 0.45f
        val moonDimming = (1.0f - lightingState.moonGlowIntensity * 0.55f).coerceIn(0.15f, 1.0f)
        val finalAlpha = baseAlpha * moonDimming

        if (finalAlpha <= 0.02f) return

        val screenPoints = mutableListOf<Offset>()
        var prevPos: Offset? = null

        for (pt in galacticPoints) {
            val screenPos = HeroSkyProjection.project(pt.azimuthDeg, pt.altitudeDeg, width, height, latitudeDeg)
            screenPoints.add(screenPos)
            // Break the subpath if consecutive points wrap across the canvas azimuth seam
            val jumpedSeam = prevPos != null && abs(screenPos.x - prevPos.x) > width * 0.35f
            if (first || jumpedSeam) {
                mwPath.moveTo(screenPos.x, screenPos.y)
                first = false
            } else {
                mwPath.lineTo(screenPos.x, screenPos.y)
            }
            prevPos = screenPos
        }

        // Soft diffused galactic band glow
        val bandColor = when (theme) {
            SkyCanvasTheme.ATMOSPHERIC_SKY -> Color(0xFF818CF8).copy(alpha = finalAlpha * 0.14f)
            SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Color(0xFF94A3B8).copy(alpha = finalAlpha * 0.12f)
            SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFFFFC6FF).copy(alpha = finalAlpha * 0.16f)
            SkyCanvasTheme.OBSERVATORY -> Color(0xFF991B1B).copy(alpha = finalAlpha * 0.18f)
            SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFFE9C46A).copy(alpha = finalAlpha * 0.12f)
        }
        drawScope.drawPath(
            path = mwPath,
            color = bandColor,
            style = Stroke(width = 26f, cap = StrokeCap.Round)
        )

        // Draw soft ambient galactic dust dots along the plane
        drawDustDots(drawScope, screenPoints, finalAlpha, frameTimeMs, theme)
    }

    private fun drawDustDots(
        drawScope: DrawScope,
        screenPoints: List<Offset>,
        finalAlpha: Float,
        frameTimeMs: Long,
        theme: SkyCanvasTheme
    ) {
        for (i in screenPoints.indices) {
            val p = screenPoints[i]
            for (sub in 0..1) {
                val hash = (i * 37 + sub * 73)
                val offsetX = (hash % 29 - 14).toFloat()
                val offsetY = (hash % 23 - 11).toFloat()
                val particleAlpha = finalAlpha * (0.3f + 0.4f * sin(frameTimeMs * 0.001f + hash).toFloat().coerceIn(0f, 1f))
                val dotRadius = 1.1f + (hash % 3) * 0.55f

                val dotColor = when (theme) {
                    SkyCanvasTheme.ATMOSPHERIC_SKY -> when (hash % 3) {
                        0 -> Color(0xFFC084FC)
                        1 -> Color(0xFF38BDF8)
                        else -> Color(0xFFFDE047)
                    }
                    SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Color(0xFF94A3B8)
                    SkyCanvasTheme.KIDS_WATERCOLOR -> Color(0xFFFF85A1)
                    SkyCanvasTheme.OBSERVATORY -> Color(0xFFEF4444)
                    SkyCanvasTheme.PAPERCRAFT_DIORAMA -> Color(0xFFF4A261)
                }

                drawScope.drawCircle(
                    color = dotColor.copy(alpha = particleAlpha),
                    radius = dotRadius,
                    center = Offset(p.x + offsetX, p.y + offsetY)
                )
            }
        }
    }
}
