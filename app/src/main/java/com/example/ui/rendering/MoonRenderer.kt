package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
                    radius = radius * 4.0f
                ),
                radius = radius * 4.0f,
                center = center
            )
            drawScope.drawCircle(
                color = Color(0xFF0F172A),
                radius = radius,
                center = center
            )
            drawScope.drawCircle(
                color = Color(0xFF60A5FA),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f)
            )
            return
        }

        // 1. Dual-Layer Minimalist Vector Aura
        // Outer Aura: Soft Cool Teal/Silver
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.25f * lightingState.bloomIntensity),
                    Color(0xFF818CF8).copy(alpha = 0.10f * lightingState.bloomIntensity),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 2.6f * moonPulseScale
            ),
            radius = radius * 2.6f * moonPulseScale,
            center = center
        )

        // Inner Aura: Lunar Silver
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF1F5F9).copy(alpha = 0.40f * lightingState.bloomIntensity),
                    Color(0xFFE2E8F0).copy(alpha = 0.15f * lightingState.bloomIntensity),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.6f * moonPulseScale
            ),
            radius = radius * 1.6f * moonPulseScale,
            center = center
        )

        // 2. Base Moon Disk (Silver/Cream, or Crimson Red during Lunar Eclipse)
        val moonBaseColor = if (isLunarEclipse) Color(0xFFDC2626) else Color(0xFFF1F5F9)

        drawScope.drawCircle(
            color = moonBaseColor,
            radius = radius,
            center = center
        )

        // 3. Minimalist Vector Craters (Clean geometric vector stroke rings - NO photorealistic textures)
        val craterStrokeColor = if (isLunarEclipse) Color(0xFF991B1B).copy(alpha = 0.5f) else Color(0xFF94A3B8).copy(alpha = 0.4f)
        val craterFillColor = if (isLunarEclipse) Color(0xFF7F1D1D).copy(alpha = 0.25f) else Color(0xFFCBD5E1).copy(alpha = 0.3f)

        // Mare Serenitatis vector crater
        val c1Center = Offset(center.x - radius * 0.28f, center.y - radius * 0.22f)
        val c1Radius = radius * 0.26f
        drawScope.drawCircle(color = craterFillColor, radius = c1Radius, center = c1Center)
        drawScope.drawCircle(color = craterStrokeColor, radius = c1Radius, center = c1Center, style = Stroke(width = 1.2f))

        // Mare Tranquillitatis vector crater
        val c2Center = Offset(center.x + radius * 0.24f, center.y - radius * 0.28f)
        val c2Radius = radius * 0.22f
        drawScope.drawCircle(color = craterFillColor, radius = c2Radius, center = c2Center)
        drawScope.drawCircle(color = craterStrokeColor, radius = c2Radius, center = c2Center, style = Stroke(width = 1.0f))

        // Mare Imbrium vector crater
        val c3Center = Offset(center.x - radius * 0.12f, center.y + radius * 0.26f)
        val c3Radius = radius * 0.30f
        drawScope.drawCircle(color = craterFillColor, radius = c3Radius, center = c3Center)
        drawScope.drawCircle(color = craterStrokeColor, radius = c3Radius, center = c3Center, style = Stroke(width = 1.2f))

        // Tycho Ray Point (Tiny bright vector accent)
        val tychoCenter = Offset(center.x + radius * 0.25f, center.y + radius * 0.42f)
        drawScope.drawCircle(color = Color.White.copy(alpha = 0.9f), radius = radius * 0.08f, center = tychoCenter)

        // 4. Accurate Phase Shadow Masking Path with Natural Feathered Terminator
        val phaseFrac = (illuminationPercent / 100.0).coerceIn(0.0, 1.0)
        if (phaseFrac < 0.98) {
            val baseShadowColor = Color(0xDD0F172A)
            val numSteps = 12
            val stepAlpha = baseShadowColor.alpha / numSteps
            val feather = radius * 0.10f // Soft penumbra feather width

            for (step in 0 until numSteps) {
                val t = (step.toFloat() / (numSteps - 1) - 0.5f) * 2f
                val offset = t * feather

                val shadowPath = Path()
                val k = (2.0 * phaseFrac - 1.0).toFloat() // Range -1 to +1

                shadowPath.addArc(
                    oval = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 180f
                )

                val stepOvalWidth = (radius * abs(k) + offset).coerceAtLeast(0f)
                if (k > 0) {
                    // Gibbous phase shadow
                    shadowPath.arcTo(
                        rect = Rect(center.x - stepOvalWidth, center.y - radius, center.x + stepOvalWidth, center.y + radius),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                } else {
                    // Crescent phase shadow
                    shadowPath.arcTo(
                        rect = Rect(center.x - stepOvalWidth, center.y - radius, center.x + stepOvalWidth, center.y + radius),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                }
                shadowPath.close()

                drawScope.drawPath(
                    path = shadowPath,
                    color = baseShadowColor.copy(alpha = stepAlpha)
                )
            }
        }

        // 5. Delicate Perimeter Vector Contour Ring
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.2f)
        )
    }
}
