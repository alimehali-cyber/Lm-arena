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
        theme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL
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

        for (pt in galacticPoints) {
            val px = (pt.azimuthDeg / 360.0 * width).toFloat()
            val py = (height - (pt.altitudeDeg / 90.0 * height)).toFloat()
            screenPoints.add(Offset(px, py))
            if (first) {
                mwPath.moveTo(px, py)
                first = false
            } else {
                mwPath.lineTo(px, py)
            }
        }

        // Only draw soft ambient dust dots, no heavy squiggly path lines across the sky canvas
        drawDustDots(drawScope, screenPoints, finalAlpha, frameTimeMs, theme)
    }

    private fun drawDustDots(
        drawScope: DrawScope,
        screenPoints: List<Offset>,
        finalAlpha: Float,
        frameTimeMs: Long,
        theme: SkyCanvasTheme
    ) {
        val step = 3
        for (i in screenPoints.indices step step) {
            val p = screenPoints[i]
            val hash = i * 37
            val offsetX = (hash % 29 - 14).toFloat()
            val offsetY = (hash % 23 - 11).toFloat()
            val particleAlpha = finalAlpha * (0.3f + 0.4f * sin(frameTimeMs * 0.001f + hash).toFloat().coerceIn(0f, 1f))
            val dotRadius = 1.2f + (hash % 3) * 0.6f

            val dotColor = when (theme) {
                SkyCanvasTheme.COSMIC_PREMIUM -> when (hash % 3) {
                    0 -> Color(0xFFC084FC)
                    1 -> Color(0xFF38BDF8)
                    else -> Color(0xFFFDE047)
                }
                SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> Color(0xFF94A3B8)
                SkyCanvasTheme.BLUEPRINT -> Color(0xFF38BDF8)
                SkyCanvasTheme.OBSERVATORY -> Color(0xFFEF4444)
            }

            drawScope.drawCircle(
                color = dotColor.copy(alpha = particleAlpha),
                radius = dotRadius,
                center = Offset(p.x + offsetX, p.y + offsetY)
            )
        }
    }
}
