package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

object SunRenderer {

    fun drawSun(
        drawScope: DrawScope,
        center: Offset,
        sunAltitudeDeg: Double,
        frameTimeMs: Long
    ) {
        if (sunAltitudeDeg < -4.0) return

        val sunRadius = drawScope.run { 24.dp.toPx() }

        // Animated Pulsing Corona
        val coronaPulse = 1.0f + 0.08f * sin(frameTimeMs * 0.0015f).toFloat()
        val coronaRadius = sunRadius * 3.5f * coronaPulse

        // Outer Atmospheric Corona
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = 0.8f),
                    Color(0xFFFBBF24).copy(alpha = 0.4f),
                    Color(0xFFF59E0B).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = coronaRadius
            ),
            radius = coronaRadius,
            center = center
        )

        // Mid Flare Glow
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFBEB),
                    Color(0xFFFDE047),
                    Color(0xFFF59E0B)
                ),
                center = center,
                radius = sunRadius * 1.5f
            ),
            radius = sunRadius * 1.5f,
            center = center
        )

        // Core Solar Disk
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFFEF08A),
                    Color(0xFFFBBF24)
                ),
                center = center,
                radius = sunRadius
            ),
            radius = sunRadius,
            center = center
        )
    }
}
