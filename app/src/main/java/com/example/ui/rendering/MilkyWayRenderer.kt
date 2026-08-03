package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.astro_engine.GalacticEngine
import kotlin.math.abs

object MilkyWayRenderer {

    fun drawMilkyWay(
        drawScope: DrawScope,
        galacticPoints: List<GalacticEngine.GalacticPlanePoint>,
        lightingState: LightingState
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

        for (pt in galacticPoints) {
            val px = (pt.azimuthDeg / 360.0 * width).toFloat()
            val py = (height - (pt.altitudeDeg / 90.0 * height)).toFloat()
            if (first) {
                mwPath.moveTo(px, py)
                first = false
            } else {
                mwPath.lineTo(px, py)
            }
        }

        // --- LAYER 1: Deep Galactic Background Glow (Indigo/Purple, 50dp wide) ---
        drawScope.drawPath(
            path = mwPath,
            color = Color(0xFF312E81).copy(alpha = finalAlpha * 0.6f),
            style = Stroke(
                width = drawScope.run { 56.dp.toPx() },
                cap = StrokeCap.Round,
                pathEffect = PathEffect.cornerPathEffect(50f)
            )
        )

        // --- LAYER 2: Core Galactic Cloud (Magenta/Purple, 32dp wide) ---
        drawScope.drawPath(
            path = mwPath,
            color = Color(0xFFA855F7).copy(alpha = finalAlpha * 0.75f),
            style = Stroke(
                width = drawScope.run { 34.dp.toPx() },
                cap = StrokeCap.Round,
                pathEffect = PathEffect.cornerPathEffect(40f)
            )
        )

        // --- LAYER 3: Bright Cosmic Dust Lane (Pink/Gold, 18dp wide) ---
        drawScope.drawPath(
            path = mwPath,
            color = Color(0xFFEC4899).copy(alpha = finalAlpha * 0.85f),
            style = Stroke(
                width = drawScope.run { 18.dp.toPx() },
                cap = StrokeCap.Round,
                pathEffect = PathEffect.cornerPathEffect(30f)
            )
        )

        // --- LAYER 4: Bright Core Axis (Warm Gold, 8dp wide) ---
        drawScope.drawPath(
            path = mwPath,
            color = Color(0xFFFDE047).copy(alpha = finalAlpha * 0.95f),
            style = Stroke(
                width = drawScope.run { 8.dp.toPx() },
                cap = StrokeCap.Round,
                pathEffect = PathEffect.cornerPathEffect(20f)
            )
        )
    }
}
