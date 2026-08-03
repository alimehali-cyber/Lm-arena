package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.astro_engine.CoordinateEngine
import com.example.domain.CelestialObject
import kotlin.math.absoluteValue
import kotlin.math.sin

object StarRenderer {

    fun drawStars(
        drawScope: DrawScope,
        stars: List<Pair<CelestialObject, CoordinateEngine.Horizontal>>,
        starVisibility: Float,
        frameTimeMs: Long
    ) {
        if (starVisibility <= 0.05f) return

        val width = drawScope.size.width
        val height = drawScope.size.height

        stars.forEach { (star, horiz) ->
            val sx = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val sy = (height - (horiz.altitudeDeg / 90.0 * height)).toFloat()

            // Unique hash for star twinkle frequency and phase shift
            val hash = star.id.hashCode()
            val twinkleFreq = 0.002f + (hash % 10) * 0.0003f
            val twinklePhase = (hash % 100) * 0.1f

            val twinkle = 0.35f + 0.65f * sin(frameTimeMs * twinkleFreq + twinklePhase).toFloat().absoluteValue
            val alpha = (starVisibility * (0.6f + 0.4f * twinkle)).coerceIn(0f, 1f)

            val baseRadius = (3.8f - star.magnitude.toFloat() * 0.5f).coerceAtLeast(1.2f) * (0.8f + 0.3f * twinkle)

            // Spectral classification colors (O/B blue, A white, G yellow, K orange, M red)
            val spectralColor = when {
                star.magnitude < -0.5 -> Color(0xFF93C5FD) // Soft blue (Sirius/Rigel)
                star.magnitude < 0.5 -> Color(0xFFFEF08A)  // Golden (Capella/Arcturus)
                star.magnitude < 1.2 -> Color(0xFFFCA5A5)  // Soft reddish (Betelgeuse/Antares)
                else -> Color(0xFFF8FAFC)                  // Cool white
            }

            val center = Offset(sx, sy)

            // Diffraction Spikes & Soft Halo for ultra-bright stars (Magnitude < 1.0)
            if (star.magnitude < 1.0) {
                // Soft halo
                drawScope.drawCircle(
                    color = spectralColor.copy(alpha = 0.22f * alpha),
                    radius = baseRadius * 4.0f,
                    center = center
                )

                // 4-point Subtle Diffraction Spikes
                val spikeLength = baseRadius * 3.2f * twinkle
                drawScope.drawLine(
                    color = spectralColor.copy(alpha = 0.4f * alpha),
                    start = Offset(sx - spikeLength, sy),
                    end = Offset(sx + spikeLength, sy),
                    strokeWidth = 1.2f
                )
                drawScope.drawLine(
                    color = spectralColor.copy(alpha = 0.4f * alpha),
                    start = Offset(sx, sy - spikeLength),
                    end = Offset(sx, sy + spikeLength),
                    strokeWidth = 1.2f
                )
            }

            // Core Star Point
            drawScope.drawCircle(
                color = spectralColor.copy(alpha = alpha),
                radius = baseRadius,
                center = center
            )
        }
    }
}
