package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

object SunRenderer {

    fun drawSun(
        drawScope: DrawScope,
        center: Offset,
        sunAltitudeDeg: Double,
        frameTimeMs: Long
    ) {
        if (sunAltitudeDeg < -5.0) return

        val sunRadius = drawScope.run { 22.dp.toPx() }

        // Continuous breathing & shining pulse factors
        val shinePulse = 1.0f + 0.08f * sin(frameTimeMs * 0.0025f).toFloat()
        val rayWiggleTime = frameTimeMs * 0.003f

        // Colors for clean kid's drawing Sun
        val diskColor = Color(0xFFFDE047) // Cheerful Yellow
        val strokeColor = Color(0xFFEA580C) // Bold Warm Orange Outline
        val rayColor = Color(0xFFF59E0B) // Golden Ray Stroke
        val glowColor = Color(0xFFFEF08A).copy(alpha = 0.35f)

        // 1. Soft Outer Shining Glow Aura
        val glowRadius = sunRadius * 3.2f * shinePulse
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor, glowColor.copy(alpha = 0.1f), Color.Transparent),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )

        // 2. Playful Radiating Rays (12 clean hand-styled vector rays)
        val rayCount = 12
        val baseRayStart = sunRadius + drawScope.run { 4.dp.toPx() }

        for (i in 0 until rayCount) {
            val baseAngle = (i * (2.0 * Math.PI / rayCount)).toFloat()
            // Playful angle wiggle for hand-drawn feel
            val wiggleAngle = baseAngle + 0.04f * sin(rayWiggleTime + i * 1.3f).toFloat()

            // Alternating ray lengths with pulsating bounce
            val rayLength = if (i % 2 == 0) {
                drawScope.run { 16.dp.toPx() } * (0.9f + 0.2f * sin(rayWiggleTime + i).toFloat())
            } else {
                drawScope.run { 10.dp.toPx() } * (0.9f + 0.2f * cos(rayWiggleTime + i).toFloat())
            }

            val startX = center.x + baseRayStart * cos(wiggleAngle)
            val startY = center.y + baseRayStart * sin(wiggleAngle)
            val endX = center.x + (baseRayStart + rayLength) * cos(wiggleAngle)
            val endY = center.y + (baseRayStart + rayLength) * sin(wiggleAngle)

            // Curved hand-drawn feel using quadratic curve path
            val midX = (startX + endX) / 2f + drawScope.run { 2.dp.toPx() } * sin(rayWiggleTime + i * 2f).toFloat()
            val midY = (startY + endY) / 2f + drawScope.run { 2.dp.toPx() } * cos(rayWiggleTime + i * 2f).toFloat()

            val rayPath = Path().apply {
                moveTo(startX, startY)
                quadraticTo(midX, midY, endX, endY)
            }

            drawScope.drawPath(
                path = rayPath,
                color = rayColor,
                style = Stroke(
                    width = drawScope.run { 3.2.dp.toPx() },
                    cap = StrokeCap.Round
                )
            )
        }

        // 3. Central Sun Disk
        drawScope.drawCircle(
            color = diskColor,
            radius = sunRadius,
            center = center
        )

        // 4. Clean Bold Vector Outline
        drawScope.drawCircle(
            color = strokeColor,
            radius = sunRadius,
            center = center,
            style = Stroke(
                width = drawScope.run { 3.dp.toPx() }
            )
        )

        // 5. Cute Minimalist Vector Face (Kid's drawing style: two eyes + cheerful smile)
        val eyeOffset = sunRadius * 0.35f
        val eyeRadius = drawScope.run { 2.2.dp.toPx() }
        val eyeY = center.y - sunRadius * 0.15f

        // Left Eye
        drawScope.drawCircle(
            color = strokeColor,
            radius = eyeRadius,
            center = Offset(center.x - eyeOffset, eyeY)
        )
        // Right Eye
        drawScope.drawCircle(
            color = strokeColor,
            radius = eyeRadius,
            center = Offset(center.x + eyeOffset, eyeY)
        )

        // Cheerful Curved Smile
        val smilePath = Path().apply {
            val smileY = center.y + sunRadius * 0.1f
            val smileWidth = sunRadius * 0.45f
            moveTo(center.x - smileWidth, smileY)
            quadraticTo(
                center.x, smileY + sunRadius * 0.38f,
                center.x + smileWidth, smileY
            )
        }
        drawScope.drawPath(
            path = smilePath,
            color = strokeColor,
            style = Stroke(
                width = drawScope.run { 2.2.dp.toPx() },
                cap = StrokeCap.Round
            )
        )
    }
}
