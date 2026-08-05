package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import kotlin.math.cos
import kotlin.math.sin

object SunRenderer {

    fun drawSun(
        drawScope: DrawScope,
        center: Offset,
        sunAltitudeDeg: Double,
        frameTimeMs: Long,
        theme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL
    ) {
        if (sunAltitudeDeg < -5.0) return

        when (theme) {
            SkyCanvasTheme.CELESTIAL -> drawCelestialSun(drawScope, center, sunAltitudeDeg, frameTimeMs)
            SkyCanvasTheme.MONOCHROME -> drawMonochromeSun(drawScope, center, sunAltitudeDeg, frameTimeMs)
            SkyCanvasTheme.FUN -> drawFunSun(drawScope, center, sunAltitudeDeg, frameTimeMs)
        }
    }

    private fun drawCelestialSun(
        drawScope: DrawScope,
        center: Offset,
        sunAltitudeDeg: Double,
        frameTimeMs: Long
    ) {
        val sunRadius = drawScope.run { 20.dp.toPx() }
        val shinePulse = 1.0f + 0.08f * sin(frameTimeMs * 0.0025f).toFloat()
        val rayWiggleTime = frameTimeMs * 0.003f

        val diskColor = Color(0xFFFDE047)
        val strokeColor = Color(0xFFF59E0B)
        val rayColor = Color(0xFFFBBF24)
        val glowColor = Color(0xFFFEF08A).copy(alpha = 0.35f)

        // 1. Soft Outer Glowing Aura
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

        // 2. Vector Rays
        val rayCount = 12
        val baseRayStart = sunRadius + drawScope.run { 4.dp.toPx() }

        for (i in 0 until rayCount) {
            val baseAngle = (i * (2.0 * Math.PI / rayCount)).toFloat()
            val wiggleAngle = baseAngle + 0.03f * sin(rayWiggleTime + i * 1.3f).toFloat()

            val rayLength = if (i % 2 == 0) {
                drawScope.run { 14.dp.toPx() } * (0.9f + 0.2f * sin(rayWiggleTime + i).toFloat())
            } else {
                drawScope.run { 8.dp.toPx() } * (0.9f + 0.2f * cos(rayWiggleTime + i).toFloat())
            }

            val startX = center.x + baseRayStart * cos(wiggleAngle)
            val startY = center.y + baseRayStart * sin(wiggleAngle)
            val endX = center.x + (baseRayStart + rayLength) * cos(wiggleAngle)
            val endY = center.y + (baseRayStart + rayLength) * sin(wiggleAngle)

            drawScope.drawLine(
                color = rayColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = drawScope.run { 2.5.dp.toPx() },
                cap = StrokeCap.Round
            )
        }

        // 3. Disk & Outline
        drawScope.drawCircle(color = diskColor, radius = sunRadius, center = center)
        drawScope.drawCircle(color = strokeColor, radius = sunRadius, center = center, style = Stroke(width = drawScope.run { 2.dp.toPx() }))
    }

    private fun drawMonochromeSun(
        drawScope: DrawScope,
        center: Offset,
        sunAltitudeDeg: Double,
        frameTimeMs: Long
    ) {
        val sunRadius = drawScope.run { 18.dp.toPx() }
        val isDay = sunAltitudeDeg > 0.0

        // Adaptive contrast stroke (black on bright white daytime, white on dark night)
        val baseColor = if (isDay) Color(0xFF18181B) else Color(0xFFFAFAFA)

        // Subtle opacity glow pulse
        val opacityGlow = 0.15f + 0.12f * sin(frameTimeMs * 0.002f).toFloat()
        val glowRadius = sunRadius * 2.2f
        drawScope.drawCircle(
            color = baseColor.copy(alpha = opacityGlow),
            radius = glowRadius,
            center = center
        )

        // Minimal elegant short rays with perfect spacing
        val rayCount = 8
        val rayStart = sunRadius + drawScope.run { 5.dp.toPx() }
        val rayLength = drawScope.run { 6.dp.toPx() }
        for (i in 0 until rayCount) {
            val angle = (i * (2.0 * Math.PI / rayCount)).toFloat()
            val startX = center.x + rayStart * cos(angle)
            val startY = center.y + rayStart * sin(angle)
            val endX = center.x + (rayStart + rayLength) * cos(angle)
            val endY = center.y + (rayStart + rayLength) * sin(angle)

            drawScope.drawLine(
                color = baseColor.copy(alpha = 0.85f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = drawScope.run { 1.5.dp.toPx() },
                cap = StrokeCap.Round
            )
        }

        // Fill during sunset: as altitude drops below 4deg, slowly fills with soft grey
        if (sunAltitudeDeg in -5.0..4.0) {
            val fillAlpha = ((4.0 - sunAltitudeDeg) / 9.0).coerceIn(0.0, 1.0).toFloat() * 0.45f
            val fillColor = if (isDay) Color(0xFF71717A) else Color(0xFFA1A1AA)
            drawScope.drawCircle(
                color = fillColor.copy(alpha = fillAlpha),
                radius = sunRadius,
                center = center
            )
        }

        // Outlined circle: minimal, perfect proportions, no fill during daytime
        drawScope.drawCircle(
            color = baseColor,
            radius = sunRadius,
            center = center,
            style = Stroke(width = drawScope.run { 1.8.dp.toPx() })
        )
    }

    private fun drawFunSun(
        drawScope: DrawScope,
        center: Offset,
        sunAltitudeDeg: Double,
        frameTimeMs: Long
    ) {
        val sunRadius = drawScope.run { 22.dp.toPx() }
        val rayWiggleTime = frameTimeMs * 0.003f

        val diskColor = Color(0xFFFDE047)   // Crayon Bright Yellow
        val strokeColor = Color(0xFFEA580C) // Bold Crayon Orange Outline
        val rayColor = Color(0xFFF59E0B)    // Crayon Golden Rays

        // 1. Playful Radiating Rays (12 wavy hand-drawn crayon rays)
        val rayCount = 12
        val baseRayStart = sunRadius + drawScope.run { 4.dp.toPx() }

        for (i in 0 until rayCount) {
            val baseAngle = (i * (2.0 * Math.PI / rayCount)).toFloat()
            val wiggleAngle = baseAngle + 0.04f * sin(rayWiggleTime + i * 1.3f).toFloat()

            val rayLength = if (i % 2 == 0) {
                drawScope.run { 16.dp.toPx() } * (0.9f + 0.2f * sin(rayWiggleTime + i).toFloat())
            } else {
                drawScope.run { 10.dp.toPx() } * (0.9f + 0.2f * cos(rayWiggleTime + i).toFloat())
            }

            val startX = center.x + baseRayStart * cos(wiggleAngle)
            val startY = center.y + baseRayStart * sin(wiggleAngle)
            val endX = center.x + (baseRayStart + rayLength) * cos(wiggleAngle)
            val endY = center.y + (baseRayStart + rayLength) * sin(wiggleAngle)

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

        // 2. Central Sun Disk
        drawScope.drawCircle(
            color = diskColor,
            radius = sunRadius,
            center = center
        )

        // 3. Bold Crayon Outline
        drawScope.drawCircle(
            color = strokeColor,
            radius = sunRadius,
            center = center,
            style = Stroke(width = drawScope.run { 3.dp.toPx() })
        )

        // 4. Cute Minimalist Vector Face (Kid's drawing style: two eyes + cheerful smile)
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
