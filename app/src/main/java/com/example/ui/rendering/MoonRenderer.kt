package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
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
        lightingState: LightingState,
        frameTimeMs: Long = System.currentTimeMillis(),
        isWaxing: Boolean = true
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

        val shinePulse = 1.0f + 0.08f * sin(frameTimeMs * 0.0022f).toFloat() * moonPulseScale

        // 1. Soft Outer Shining Glow Aura (Pulsating)
        val auraRadius = radius * 3.2f * shinePulse
        val auraColor1 = if (isLunarEclipse) Color(0xFFEF4444).copy(alpha = 0.30f) else Color(0xFFFEF9C3).copy(alpha = 0.35f * lightingState.bloomIntensity)
        val auraColor2 = if (isLunarEclipse) Color(0xFF991B1B).copy(alpha = 0.10f) else Color(0xFF38BDF8).copy(alpha = 0.12f * lightingState.bloomIntensity)

        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(auraColor1, auraColor2, Color.Transparent),
                center = center,
                radius = auraRadius
            ),
            radius = auraRadius,
            center = center
        )

        // 2. Playful Radiating Shining Beamlets / Sparkles (Kid's drawing style)
        val rayCount = 8
        val rayStart = radius + drawScope.run { 4.dp.toPx() }
        val rayColor = if (isLunarEclipse) Color(0xFFF87171) else Color(0xFFBAE6FD)

        for (i in 0 until rayCount) {
            val angleRad = (i * (2.0 * Math.PI / rayCount) + frameTimeMs * 0.0005).toFloat()
            val rayLen = drawScope.run { 8.dp.toPx() } * (0.8f + 0.3f * sin(frameTimeMs * 0.003f + i).toFloat())

            val startX = center.x + rayStart * cos(angleRad)
            val startY = center.y + rayStart * sin(angleRad)
            val endX = center.x + (rayStart + rayLen) * cos(angleRad)
            val endY = center.y + (rayStart + rayLen) * sin(angleRad)

            drawScope.drawLine(
                color = rayColor.copy(alpha = 0.65f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = drawScope.run { 2.dp.toPx() },
                cap = StrokeCap.Round
            )
        }

        // 3. Central Kid's Moon Disk
        val diskColor = if (isLunarEclipse) Color(0xFFDC2626) else Color(0xFFFEF9C3) // Soft Cream Yellow
        val strokeColor = if (isLunarEclipse) Color(0xFF7F1D1D) else Color(0xFF0284C7) // Clean Sky Blue Outline

        drawScope.drawCircle(
            color = diskColor,
            radius = radius,
            center = center
        )

        // 4. Cute Kid's Craters & Cute Face
        val craterColor = if (isLunarEclipse) Color(0xFF991B1B).copy(alpha = 0.4f) else Color(0xFFE2E8F0).copy(alpha = 0.7f)
        val craterBorder = if (isLunarEclipse) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFBAE6FD)

        // Crater 1
        val c1Center = Offset(center.x - radius * 0.32f, center.y - radius * 0.25f)
        val c1Radius = radius * 0.22f
        drawScope.drawCircle(color = craterColor, radius = c1Radius, center = c1Center)
        drawScope.drawCircle(color = craterBorder, radius = c1Radius, center = c1Center, style = Stroke(width = 1.2f))

        // Crater 2
        val c2Center = Offset(center.x + radius * 0.30f, center.y + radius * 0.28f)
        val c2Radius = radius * 0.18f
        drawScope.drawCircle(color = craterColor, radius = c2Radius, center = c2Center)
        drawScope.drawCircle(color = craterBorder, radius = c2Radius, center = c2Center, style = Stroke(width = 1.0f))

        // Cute Face (Eyes + Smile)
        val eyeOffset = radius * 0.30f
        val eyeRadius = drawScope.run { 1.8.dp.toPx() }
        val eyeY = center.y - radius * 0.10f

        // Left Eye
        drawScope.drawCircle(color = strokeColor, radius = eyeRadius, center = Offset(center.x - eyeOffset, eyeY))
        // Right Eye
        drawScope.drawCircle(color = strokeColor, radius = eyeRadius, center = Offset(center.x + eyeOffset, eyeY))

        // Cheerful Smile
        val smilePath = Path().apply {
            val smileY = center.y + radius * 0.12f
            val smileWidth = radius * 0.38f
            moveTo(center.x - smileWidth, smileY)
            quadraticTo(
                center.x, smileY + radius * 0.32f,
                center.x + smileWidth, smileY
            )
        }
        drawScope.drawPath(
            path = smilePath,
            color = strokeColor,
            style = Stroke(width = drawScope.run { 1.8.dp.toPx() }, cap = StrokeCap.Round)
        )

        // 5. Accurate Phase Shadow Masking Path with Feathered Penumbra
        val phaseFrac = (illuminationPercent / 100.0).coerceIn(0.0, 1.0)
        if (phaseFrac < 0.98) {
            val baseShadowColor = Color(0xEE070A14)
            val numSteps = 14
            val stepAlpha = baseShadowColor.alpha / numSteps
            val feather = radius * 0.08f // Soft penumbra feather width

            for (step in 0 until numSteps) {
                val t = (step.toFloat() / (numSteps - 1) - 0.5f) * 2f
                val offset = t * feather

                val shadowPath = Path()
                val sweepAngle = 180f
                val startAngle = if (isWaxing) 90f else -90f

                shadowPath.addArc(
                    Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                    startAngle,
                    sweepAngle
                )

                val k = (2.0 * phaseFrac - 1.0).toFloat()
                val stepInnerWidth = (abs(k) * radius + offset).coerceAtLeast(0f)
                val innerRect = Rect(center.x - stepInnerWidth, center.y - radius, center.x + stepInnerWidth, center.y + radius)

                if (k >= 0) {
                    shadowPath.arcTo(innerRect, if (isWaxing) 270f else 90f, -sweepAngle, false)
                } else {
                    shadowPath.arcTo(innerRect, if (isWaxing) 90f else 270f, sweepAngle, false)
                }
                shadowPath.close()

                drawScope.drawPath(
                    path = shadowPath,
                    color = baseShadowColor.copy(alpha = stepAlpha)
                )
            }
        }

        // 6. Clean Bold Kid's Vector Outline
        drawScope.drawCircle(
            color = strokeColor,
            radius = radius,
            center = center,
            style = Stroke(width = drawScope.run { 2.5.dp.toPx() })
        )
    }
}
