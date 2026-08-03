package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

object MoonRenderer {

    fun drawMoon(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        illuminationPercent: Double,
        phaseAngleRad: Double,
        isLunarEclipse: Boolean,
        isSolarEclipse: Boolean,
        moonPulseScale: Float,
        lightingState: LightingState
    ) {
        if (isSolarEclipse) {
            // Solar Corona Flare during Eclipse
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFF60A5FA).copy(alpha = 0.6f), Color.Transparent),
                    center = center,
                    radius = radius * 4.2f
                ),
                radius = radius * 4.2f,
                center = center
            )
            drawScope.drawCircle(
                color = Color(0xFF0F172A),
                radius = radius,
                center = center
            )
            return
        }

        // Dual-Layer Gentle Pulsing Glow Aura
        // Outer Glow: Soft Pink (#D08AC2)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD08AC2).copy(alpha = 0.35f * lightingState.bloomIntensity),
                    Color(0xFFD08AC2).copy(alpha = 0.12f * lightingState.bloomIntensity),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 2.8f * moonPulseScale
            ),
            radius = radius * 2.8f * moonPulseScale,
            center = center
        )

        // Inner Glow: Warm Gold (#F7B731)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF7B731).copy(alpha = 0.5f * lightingState.bloomIntensity),
                    Color(0xFFF7B731).copy(alpha = 0.18f * lightingState.bloomIntensity),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.8f * moonPulseScale
            ),
            radius = radius * 1.8f * moonPulseScale,
            center = center
        )

        // Moon Base Tint (Blood red during lunar eclipse)
        val moonBaseColor = if (isLunarEclipse) Color(0xFFB91C1C) else Color(0xFFE2E8F0)

        // Base Moon Disk
        drawScope.drawCircle(
            color = moonBaseColor,
            radius = radius,
            center = center
        )

        // Maria Basins / Crater Relief (Physical texture features)
        val craterColor = if (isLunarEclipse) Color(0xFF7F1D1D) else Color(0xFF94A3B8)
        drawScope.drawCircle(
            color = craterColor.copy(alpha = 0.45f),
            radius = radius * 0.32f,
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.2f)
        )
        drawScope.drawCircle(
            color = craterColor.copy(alpha = 0.40f),
            radius = radius * 0.28f,
            center = Offset(center.x + radius * 0.2f, center.y - radius * 0.3f)
        )
        drawScope.drawCircle(
            color = craterColor.copy(alpha = 0.35f),
            radius = radius * 0.38f,
            center = Offset(center.x - radius * 0.1f, center.y + radius * 0.25f)
        )

        // Tycho crater bright ray spot
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = radius * 0.08f,
            center = Offset(center.x + radius * 0.22f, center.y + radius * 0.4f)
        )

        // Earthshine (faint unlit portion visibility during dark night)
        if (illuminationPercent < 80.0 && lightingState.ambientBrightness < 0.2f) {
            drawScope.drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                radius = radius,
                center = center
            )
        }

        // Phase Shadow Masking Path
        val phaseFrac = (illuminationPercent / 100.0).coerceIn(0.0, 1.0)
        if (phaseFrac < 0.98) {
            val darkColor = Color(0xDD0F172A)
            val shadowPath = Path()

            val isWaxing = sin(phaseAngleRad) >= 0.0
            val k = (2.0 * phaseFrac - 1.0).toFloat() // Range -1 to +1

            shadowPath.addArc(
                oval = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f
            )

            val ovalWidth = radius * abs(k)
            if (k > 0) {
                // Gibbous phase shadow
                shadowPath.arcTo(
                    rect = Rect(center.x - ovalWidth, center.y - radius, center.x + ovalWidth, center.y + radius),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
            } else {
                // Crescent phase shadow
                shadowPath.arcTo(
                    rect = Rect(center.x - ovalWidth, center.y - radius, center.x + ovalWidth, center.y + radius),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }

            drawScope.drawPath(
                path = shadowPath,
                color = darkColor
            )
        }

        // 3D Sphere Specular Edge Depth
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}
