package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.astro_engine.PlanetEngine
import com.example.astro_engine.CoordinateEngine
import kotlin.math.cos
import kotlin.math.sin

object PlanetRenderer {

    fun drawPlanets(
        drawScope: DrawScope,
        planets: List<Triple<PlanetEngine.PlanetType, PlanetEngine.PlanetPosition, CoordinateEngine.Horizontal>>
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        planets.forEach { (pType, pPos, horiz) ->
            val px = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val py = (height - ((horiz.altitudeDeg + 2.0) / 92.0 * height)).toFloat()
            val center = Offset(px, py)

            when (pType) {
                PlanetEngine.PlanetType.JUPITER -> drawJupiter(drawScope, center)
                PlanetEngine.PlanetType.SATURN -> drawSaturn(drawScope, center)
                PlanetEngine.PlanetType.MARS -> drawMars(drawScope, center)
                PlanetEngine.PlanetType.VENUS -> drawVenus(drawScope, center)
                PlanetEngine.PlanetType.MERCURY -> drawMercury(drawScope, center)
                PlanetEngine.PlanetType.URANUS -> drawUranus(drawScope, center)
                PlanetEngine.PlanetType.NEPTUNE -> drawNeptune(drawScope, center)
                else -> {}
            }
        }
    }

    private fun drawJupiter(drawScope: DrawScope, center: Offset) {
        val r = 10f
        // Outer Glow
        drawScope.drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.35f), radius = r * 2.2f, center = center)

        // Gas Giant Disk
        drawScope.drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFEF3C7),
                    Color(0xFFD97706), // Great Equatorial Cloud Belt
                    Color(0xFFFEF3C7),
                    Color(0xFFB45309), // South Equatorial Belt
                    Color(0xFFFDE68A)
                ),
                startY = center.y - r,
                endY = center.y + r
            ),
            radius = r,
            center = center
        )

        // Great Red Spot
        drawScope.drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.9f),
            radius = r * 0.28f,
            center = Offset(center.x + r * 0.3f, center.y + r * 0.2f)
        )
    }

    private fun drawSaturn(drawScope: DrawScope, center: Offset) {
        val r = 8.5f

        // Outer Glow
        drawScope.drawCircle(color = Color(0xFFFDE047).copy(alpha = 0.3f), radius = r * 2.2f, center = center)

        // Tilted Saturn Rings
        val ringRect = Rect(center.x - r * 2.4f, center.y - r * 0.7f, center.x + r * 2.4f, center.y + r * 0.7f)
        drawScope.drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFDE047).copy(alpha = 0.8f),
                    Color(0xFFCA8A04).copy(alpha = 0.9f),
                    Color(0xFFFDE047).copy(alpha = 0.8f)
                )
            ),
            topLeft = ringRect.topLeft,
            size = ringRect.size,
            style = Stroke(width = 3.5f)
        )

        // Saturn Body Disk
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFCA8A04)),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
    }

    private fun drawMars(drawScope: DrawScope, center: Offset) {
        val r = 7f
        drawScope.drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.35f), radius = r * 2.2f, center = center)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFCA5A5), Color(0xFFDC2626), Color(0xFF991B1B)),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
        // Polar Ice Cap
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = r * 0.25f,
            center = Offset(center.x, center.y - r * 0.7f)
        )
    }

    private fun drawVenus(drawScope: DrawScope, center: Offset) {
        val r = 8f
        drawScope.drawCircle(color = Color(0xFFFEF08A).copy(alpha = 0.45f), radius = r * 2.5f, center = center)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFEF08A), Color(0xFFFDE047)),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
    }

    private fun drawMercury(drawScope: DrawScope, center: Offset) {
        val r = 5.5f
        drawScope.drawCircle(color = Color(0xFFE2E8F0).copy(alpha = 0.3f), radius = r * 2.0f, center = center)
        drawScope.drawCircle(
            color = Color(0xFFCBD5E1),
            radius = r,
            center = center
        )
    }

    private fun drawUranus(drawScope: DrawScope, center: Offset) {
        val r = 6f
        drawScope.drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.4f), radius = r * 2.0f, center = center)
        drawScope.drawCircle(
            color = Color(0xFF38BDF8),
            radius = r,
            center = center
        )
    }

    private fun drawNeptune(drawScope: DrawScope, center: Offset) {
        val r = 5.5f
        drawScope.drawCircle(color = Color(0xFF60A5FA).copy(alpha = 0.4f), radius = r * 2.0f, center = center)
        drawScope.drawCircle(
            color = Color(0xFF2563EB),
            radius = r,
            center = center
        )
    }
}
