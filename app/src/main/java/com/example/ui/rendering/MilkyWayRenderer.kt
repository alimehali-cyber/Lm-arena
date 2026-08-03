package com.example.ui.rendering

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
        if (galacticPoints.isEmpty() || lightingState.sunAltitudeDeg > -6.0) return

        val width = drawScope.size.width
        val height = drawScope.size.height

        val mwPath = Path()
        var first = true

        val baseAlpha = ((abs(lightingState.sunAltitudeDeg) - 6.0) / 12.0).coerceIn(0.0, 1.0).toFloat() * 0.38f
        val moonDimming = (1.0f - lightingState.moonGlowIntensity * 0.5f).coerceIn(0.2f, 1.0f)
        val finalAlpha = baseAlpha * moonDimming

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

        drawScope.drawPath(
            path = mwPath,
            color = Color(0xFFC084FC).copy(alpha = finalAlpha),
            style = Stroke(
                width = drawScope.run { 28.dp.toPx() },
                cap = StrokeCap.Round,
                pathEffect = PathEffect.cornerPathEffect(40f)
            )
        )
    }
}
