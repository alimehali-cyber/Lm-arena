package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

object StarRenderer {

    fun drawStars(
        drawScope: DrawScope,
        objects: List<Pair<CelestialObject, CoordinateEngine.Horizontal>>,
        starVisibility: Float,
        frameTimeMs: Long,
        theme: SkyCanvasTheme = SkyCanvasTheme.COSMIC_PREMIUM
    ) {
        if (starVisibility <= 0.05f) return

        when (theme) {
            SkyCanvasTheme.COSMIC_PREMIUM -> drawCelestialStars(drawScope, objects, starVisibility, frameTimeMs)
            SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> drawMonochromeStars(drawScope, objects, starVisibility, frameTimeMs)
            SkyCanvasTheme.BLUEPRINT -> drawMonochromeStars(drawScope, objects, starVisibility, frameTimeMs)
            SkyCanvasTheme.OBSERVATORY -> drawMonochromeStars(drawScope, objects, starVisibility, frameTimeMs)
        }
    }

    private fun drawCelestialStars(
        drawScope: DrawScope,
        objects: List<Pair<CelestialObject, CoordinateEngine.Horizontal>>,
        starVisibility: Float,
        frameTimeMs: Long
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        objects.forEach { (celestialObj, horiz) ->
            val sx = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val sy = (height - (horiz.altitudeDeg / 90.0 * height)).toFloat()
            val center = Offset(sx, sy)

            if (celestialObj.type == ObjectType.DEEP_SKY) {
                drawAndromedaCelestial(drawScope, center, starVisibility, frameTimeMs)
            } else if (celestialObj.type == ObjectType.STAR) {
                val hash = celestialObj.id.hashCode()
                val twinkleFreq = 0.002f + (hash % 10) * 0.0003f
                val twinklePhase = (hash % 100) * 0.1f

                val twinkle = 0.35f + 0.65f * sin(frameTimeMs * twinkleFreq + twinklePhase).toFloat().absoluteValue
                val alpha = (starVisibility * (0.6f + 0.4f * twinkle)).coerceIn(0f, 1f)

                val baseRadius = (3.6f - celestialObj.magnitude.toFloat() * 0.5f).coerceAtLeast(1.2f) * (0.85f + 0.25f * twinkle)

                val spectralColor = when {
                    celestialObj.magnitude < -0.5 -> Color(0xFF93C5FD)
                    celestialObj.magnitude < 0.5 -> Color(0xFFFEF08A)
                    celestialObj.magnitude < 1.2 -> Color(0xFFFCA5A5)
                    else -> Color(0xFFF8FAFC)
                }

                if (celestialObj.magnitude < 1.2) {
                    drawScope.drawCircle(
                        color = spectralColor.copy(alpha = 0.18f * alpha),
                        radius = baseRadius * 3.5f,
                        center = center
                    )

                    val spikeLength = baseRadius * 2.8f * twinkle
                    drawScope.drawLine(
                        color = spectralColor.copy(alpha = 0.5f * alpha),
                        start = Offset(sx - spikeLength, sy),
                        end = Offset(sx + spikeLength, sy),
                        strokeWidth = 1.0f
                    )
                    drawScope.drawLine(
                        color = spectralColor.copy(alpha = 0.5f * alpha),
                        start = Offset(sx, sy - spikeLength),
                        end = Offset(sx, sy + spikeLength),
                        strokeWidth = 1.0f
                    )
                }

                drawScope.drawCircle(
                    color = spectralColor.copy(alpha = alpha),
                    radius = baseRadius,
                    center = center
                )
            }
        }
    }

    private fun drawMonochromeStars(
        drawScope: DrawScope,
        objects: List<Pair<CelestialObject, CoordinateEngine.Horizontal>>,
        starVisibility: Float,
        frameTimeMs: Long
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        objects.forEach { (celestialObj, horiz) ->
            val sx = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val sy = (height - (horiz.altitudeDeg / 90.0 * height)).toFloat()
            val center = Offset(sx, sy)

            if (celestialObj.type == ObjectType.DEEP_SKY) {
                // Faint elliptical outline with soft translucent center
                val alpha = (starVisibility * 0.45f).coerceIn(0f, 1f)
                if (alpha > 0.05f) {
                    drawScope.withTransform({
                        rotate(degrees = -35f, pivot = center)
                    }) {
                        val gWidth = 32f
                        val gHeight = 14f
                        // Translucent center
                        drawScope.drawOval(
                            color = Color.White.copy(alpha = 0.12f * alpha),
                            topLeft = Offset(center.x - gWidth / 2f, center.y - gHeight / 2f),
                            size = Size(gWidth, gHeight)
                        )
                        // Faint outline
                        drawScope.drawOval(
                            color = Color.White.copy(alpha = 0.35f * alpha),
                            topLeft = Offset(center.x - gWidth / 2f, center.y - gHeight / 2f),
                            size = Size(gWidth, gHeight),
                            style = Stroke(width = 1.0f)
                        )
                    }
                }
            } else if (celestialObj.type == ObjectType.STAR) {
                val hash = celestialObj.id.hashCode().absoluteValue
                val twinkleFreq = 0.002f + (hash % 10) * 0.0003f
                val twinklePhase = (hash % 100) * 0.1f

                // Twinkle modulating OPACITY ONLY (no scaling, no colors, no glow)
                val twinkle = 0.30f + 0.70f * sin(frameTimeMs * twinkleFreq + twinklePhase).toFloat().absoluteValue
                val alpha = (starVisibility * twinkle).coerceIn(0f, 1f)
                val starColor = Color.White.copy(alpha = alpha)

                val symbolType = hash % 4
                when (symbolType) {
                    0 -> { // Tiny Dot
                        drawScope.drawCircle(
                            color = starColor,
                            radius = 1.8f,
                            center = center
                        )
                    }
                    1 -> { // 4-Point Small Sparkle
                        val r = 4.5f
                        drawScope.drawLine(color = starColor, start = Offset(sx - r, sy), end = Offset(sx + r, sy), strokeWidth = 1.0f)
                        drawScope.drawLine(color = starColor, start = Offset(sx, sy - r), end = Offset(sx, sy + r), strokeWidth = 1.0f)
                    }
                    2 -> { // Tiny Cross
                        val r = 3.0f
                        drawScope.drawLine(color = starColor, start = Offset(sx - r, sy - r), end = Offset(sx + r, sy + r), strokeWidth = 1.0f)
                        drawScope.drawLine(color = starColor, start = Offset(sx - r, sy + r), end = Offset(sx + r, sy - r), strokeWidth = 1.0f)
                    }
                    else -> { // Tiny Diamond
                        val dPath = Path().apply {
                            moveTo(sx, sy - 3.5f)
                            lineTo(sx + 2.5f, sy)
                            lineTo(sx, sy + 3.5f)
                            lineTo(sx - 2.5f, sy)
                            close()
                        }
                        drawScope.drawPath(path = dPath, color = starColor, style = Stroke(width = 1.0f))
                    }
                }
            }
        }
    }

    private fun drawFunStars(
        drawScope: DrawScope,
        objects: List<Pair<CelestialObject, CoordinateEngine.Horizontal>>,
        starVisibility: Float,
        frameTimeMs: Long
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        objects.forEach { (celestialObj, horiz) ->
            val sx = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val sy = (height - (horiz.altitudeDeg / 90.0 * height)).toFloat()
            val center = Offset(sx, sy)

            if (celestialObj.type == ObjectType.DEEP_SKY) {
                drawAndromedaCelestial(drawScope, center, starVisibility, frameTimeMs)
            } else if (celestialObj.type == ObjectType.STAR) {
                val hash = celestialObj.id.hashCode().absoluteValue
                val twinkleFreq = 0.002f + (hash % 10) * 0.0003f
                val twinkle = 0.4f + 0.6f * sin(frameTimeMs * twinkleFreq + hash).toFloat().absoluteValue
                val alpha = (starVisibility * twinkle).coerceIn(0f, 1f)

                val pastelColor = when (hash % 3) {
                    0 -> Color(0xFFFEF08A) // Soft Crayon Yellow
                    1 -> Color(0xFFE9D5FF) // Crayon Pastel Lavender
                    else -> Color(0xFFF8FAFC) // Crayon White
                }.copy(alpha = alpha)

                val symbolType = hash % 3
                if (symbolType == 0) {
                    // Playful 5-Point Star
                    val sPath = Path()
                    val outerR = 6.0f
                    val innerR = 2.5f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outerR else innerR
                        val angle = (i * Math.PI / 5 - Math.PI / 2).toFloat()
                        val px = sx + r * cos(angle)
                        val py = sy + r * sin(angle)
                        if (i == 0) sPath.moveTo(px, py) else sPath.lineTo(px, py)
                    }
                    sPath.close()
                    drawScope.drawPath(path = sPath, color = pastelColor)
                    drawScope.drawPath(path = sPath, color = Color(0xFFD97706).copy(alpha = alpha), style = Stroke(width = 1.0f))
                } else if (symbolType == 1) {
                    // Crayon X
                    val r = 4f
                    drawScope.drawLine(color = pastelColor, start = Offset(sx - r, sy - r), end = Offset(sx + r, sy + r), strokeWidth = 2.0f, cap = StrokeCap.Round)
                    drawScope.drawLine(color = pastelColor, start = Offset(sx - r, sy + r), end = Offset(sx + r, sy - r), strokeWidth = 2.0f, cap = StrokeCap.Round)
                } else {
                    // Crayon Dot
                    drawScope.drawCircle(color = pastelColor, radius = 2.8f, center = center)
                }
            }
        }
    }

    private fun drawAndromedaCelestial(
        drawScope: DrawScope,
        center: Offset,
        starVisibility: Float,
        frameTimeMs: Long
    ) {
        val alpha = (starVisibility * 0.55f).coerceIn(0f, 1f)
        if (alpha <= 0.05f) return

        val width = 36f
        val height = 16f
        val pulse = 1.0f + 0.05f * sin(frameTimeMs * 0.0008f).toFloat()

        drawScope.withTransform({
            rotate(degrees = -35f, pivot = center)
        }) {
            drawScope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFC084FC).copy(alpha = 0.30f * alpha * pulse),
                        Color(0xFF818CF8).copy(alpha = 0.12f * alpha * pulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = width * 0.6f
                ),
                topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
                size = Size(width, height)
            )

            drawScope.drawOval(
                color = Color(0xFFF1F5F9).copy(alpha = 0.7f * alpha),
                topLeft = Offset(center.x - width * 0.18f, center.y - height * 0.25f),
                size = Size(width * 0.36f, height * 0.5f)
            )
        }
    }
}
