package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.astro_engine.CoordinateEngine
import com.example.domain.CelestialObject
import com.example.domain.ObjectType
import kotlin.math.absoluteValue
import kotlin.math.sin

object StarRenderer {

    fun drawStars(
        drawScope: DrawScope,
        objects: List<Pair<CelestialObject, CoordinateEngine.Horizontal>>,
        starVisibility: Float,
        frameTimeMs: Long
    ) {
        if (starVisibility <= 0.05f) return

        val width = drawScope.size.width
        val height = drawScope.size.height

        objects.forEach { (celestialObj, horiz) ->
            val sx = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val sy = (height - (horiz.altitudeDeg / 90.0 * height)).toFloat()
            val center = Offset(sx, sy)

            if (celestialObj.type == ObjectType.DEEP_SKY) {
                // --- ANDROMEDA GALAXY / DEEP SKY (Faint elegant tilted vector oval glow) ---
                drawAndromedaGalaxy(drawScope, center, starVisibility, frameTimeMs)
            } else if (celestialObj.type == ObjectType.STAR) {
                // --- PROCEDURAL VECTOR STAR ---
                val hash = celestialObj.id.hashCode()
                val twinkleFreq = 0.002f + (hash % 10) * 0.0003f
                val twinklePhase = (hash % 100) * 0.1f

                val twinkle = 0.35f + 0.65f * sin(frameTimeMs * twinkleFreq + twinklePhase).toFloat().absoluteValue
                val alpha = (starVisibility * (0.6f + 0.4f * twinkle)).coerceIn(0f, 1f)

                val baseRadius = (3.6f - celestialObj.magnitude.toFloat() * 0.5f).coerceAtLeast(1.2f) * (0.85f + 0.25f * twinkle)

                // Spectral colors
                val spectralColor = when {
                    celestialObj.magnitude < -0.5 -> Color(0xFF93C5FD) // Soft blue (Sirius/Rigel)
                    celestialObj.magnitude < 0.5 -> Color(0xFFFEF08A)  // Golden (Capella/Arcturus)
                    celestialObj.magnitude < 1.2 -> Color(0xFFFCA5A5)  // Soft reddish (Betelgeuse/Antares)
                    else -> Color(0xFFF8FAFC)                  // Cool white
                }

                // Bright stars (Magnitude < 1.2) get soft halo and 4-point vector sparkle cross
                if (celestialObj.magnitude < 1.2) {
                    // Soft Halo
                    drawScope.drawCircle(
                        color = spectralColor.copy(alpha = 0.18f * alpha),
                        radius = baseRadius * 3.5f,
                        center = center
                    )

                    // 4-Point Delicate Vector Sparkle
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

                // Core Star Vector Point
                drawScope.drawCircle(
                    color = spectralColor.copy(alpha = alpha),
                    radius = baseRadius,
                    center = center
                )
            }
        }
    }

    private fun drawAndromedaGalaxy(
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
            // Outer Faint Galaxy Halo
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

            // Bright Galactic Core
            drawScope.drawOval(
                color = Color(0xFFF1F5F9).copy(alpha = 0.7f * alpha),
                topLeft = Offset(center.x - width * 0.18f, center.y - height * 0.25f),
                size = Size(width * 0.36f, height * 0.5f)
            )
        }
    }
}
