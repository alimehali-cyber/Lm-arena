package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.alijafari.red.astronomy.astro_engine.PlanetEngine
import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import kotlin.math.cos
import kotlin.math.sin

object PlanetRenderer {

    fun drawPlanets(
        drawScope: DrawScope,
        planets: List<Triple<PlanetEngine.PlanetType, PlanetEngine.PlanetPosition, CoordinateEngine.Horizontal>>,
        frameTimeMs: Long,
        theme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        planets.forEach { (pType, pPos, horiz) ->
            val px = (horiz.azimuthDeg / 360.0 * width).toFloat()
            val py = (height - ((horiz.altitudeDeg + 2.0) / 92.0 * height)).toFloat()
            val center = Offset(px, py)

            when (theme) {
                SkyCanvasTheme.CELESTIAL -> {
                    when (pType) {
                        PlanetEngine.PlanetType.JUPITER -> drawJupiter(drawScope, center, frameTimeMs)
                        PlanetEngine.PlanetType.SATURN -> drawSaturn(drawScope, center)
                        PlanetEngine.PlanetType.MARS -> drawMars(drawScope, center)
                        PlanetEngine.PlanetType.VENUS -> drawVenus(drawScope, center)
                        PlanetEngine.PlanetType.MERCURY -> drawMercury(drawScope, center)
                        PlanetEngine.PlanetType.URANUS -> drawUranus(drawScope, center)
                        PlanetEngine.PlanetType.NEPTUNE -> drawNeptune(drawScope, center)
                        else -> {}
                    }
                }
                SkyCanvasTheme.MONOCHROME -> drawMonochromePlanet(drawScope, pType, center)
                SkyCanvasTheme.FUN -> drawFunPlanet(drawScope, pType, center)
            }
        }
    }

    private fun drawMonochromePlanet(drawScope: DrawScope, pType: PlanetEngine.PlanetType, center: Offset) {
        when (pType) {
            PlanetEngine.PlanetType.VENUS -> {
                drawScope.drawCircle(color = Color.White, radius = 7.5f, center = center)
                drawScope.drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 12f, center = center)
            }
            PlanetEngine.PlanetType.MARS -> {
                drawScope.drawCircle(color = Color.White, radius = 6.5f, center = center, style = Stroke(width = 1.2f))
                drawScope.drawCircle(color = Color.White, radius = 2.0f, center = center)
            }
            PlanetEngine.PlanetType.JUPITER -> {
                val r = 9.0f
                drawScope.drawCircle(color = Color.White, radius = r, center = center, style = Stroke(width = 1.2f))
                drawScope.drawLine(color = Color.White, start = Offset(center.x - r * 0.8f, center.y - 2.5f), end = Offset(center.x + r * 0.8f, center.y - 2.5f), strokeWidth = 1.0f)
                drawScope.drawLine(color = Color.White, start = Offset(center.x - r * 0.8f, center.y + 2.5f), end = Offset(center.x + r * 0.8f, center.y + 2.5f), strokeWidth = 1.0f)
            }
            PlanetEngine.PlanetType.SATURN -> {
                val r = 7.0f
                drawScope.drawCircle(color = Color.White, radius = r, center = center, style = Stroke(width = 1.2f))
                drawScope.withTransform({
                    rotate(degrees = -20f, pivot = center)
                }) {
                    drawScope.drawOval(
                        color = Color.White,
                        topLeft = Offset(center.x - 14f, center.y - 4f),
                        size = Size(28f, 8f),
                        style = Stroke(width = 1.2f)
                    )
                }
            }
            PlanetEngine.PlanetType.MERCURY -> {
                drawScope.drawCircle(color = Color.White, radius = 5.0f, center = center, style = Stroke(width = 1.2f))
            }
            PlanetEngine.PlanetType.URANUS -> {
                drawScope.drawCircle(color = Color.White, radius = 7.0f, center = center, style = Stroke(width = 1.0f))
                drawScope.drawCircle(color = Color.White, radius = 4.0f, center = center, style = Stroke(width = 1.0f))
            }
            PlanetEngine.PlanetType.NEPTUNE -> {
                drawScope.drawCircle(color = Color(0xFF334155), radius = 6.5f, center = center)
                drawScope.drawCircle(color = Color.White, radius = 6.5f, center = center, style = Stroke(width = 1.0f))
            }
            else -> {}
        }
    }

    private fun drawFunPlanet(drawScope: DrawScope, pType: PlanetEngine.PlanetType, center: Offset) {
        when (pType) {
            PlanetEngine.PlanetType.VENUS -> {
                drawScope.drawCircle(color = Color(0xFFFEF08A), radius = 7.5f, center = center)
                drawScope.drawCircle(color = Color(0xFFCA8A04), radius = 7.5f, center = center, style = Stroke(width = 1.5f))
            }
            PlanetEngine.PlanetType.MARS -> {
                drawScope.drawCircle(color = Color(0xFFF97316), radius = 6.5f, center = center)
                drawScope.drawCircle(color = Color(0xFFC2410C), radius = 6.5f, center = center, style = Stroke(width = 1.5f))
            }
            PlanetEngine.PlanetType.JUPITER -> {
                val r = 9.0f
                drawScope.drawCircle(color = Color(0xFFFDE047), radius = r, center = center)
                drawScope.drawCircle(color = Color(0xFFCA8A04), radius = r, center = center, style = Stroke(width = 2.0f))
                drawScope.drawLine(color = Color(0xFFEA580C), start = Offset(center.x - r * 0.7f, center.y - 3f), end = Offset(center.x + r * 0.7f, center.y - 3f), strokeWidth = 1.5f)
                drawScope.drawLine(color = Color(0xFFEA580C), start = Offset(center.x - r * 0.7f, center.y + 3f), end = Offset(center.x + r * 0.7f, center.y + 3f), strokeWidth = 1.5f)
            }
            PlanetEngine.PlanetType.SATURN -> {
                val r = 7.0f
                drawScope.drawCircle(color = Color(0xFFFDE047), radius = r, center = center)
                drawScope.drawCircle(color = Color(0xFFB45309), radius = r, center = center, style = Stroke(width = 1.5f))
                drawScope.withTransform({
                    rotate(degrees = -20f, pivot = center)
                }) {
                    drawScope.drawOval(
                        color = Color(0xFFF97316),
                        topLeft = Offset(center.x - 14f, center.y - 4.5f),
                        size = Size(28f, 9f),
                        style = Stroke(width = 2.0f)
                    )
                }
            }
            PlanetEngine.PlanetType.MERCURY -> {
                drawScope.drawCircle(color = Color(0xFFCBD5E1), radius = 5.0f, center = center)
                drawScope.drawCircle(color = Color(0xFF475569), radius = 5.0f, center = center, style = Stroke(width = 1.2f))
            }
            PlanetEngine.PlanetType.URANUS -> {
                drawScope.drawCircle(color = Color(0xFF38BDF8), radius = 7.0f, center = center)
                drawScope.drawCircle(color = Color(0xFF0284C7), radius = 7.0f, center = center, style = Stroke(width = 1.5f))
            }
            PlanetEngine.PlanetType.NEPTUNE -> {
                drawScope.drawCircle(color = Color(0xFF3B82F6), radius = 6.5f, center = center)
                drawScope.drawCircle(color = Color(0xFF1D4ED8), radius = 6.5f, center = center, style = Stroke(width = 1.5f))
            }
            else -> {}
        }
    }

    private fun drawJupiter(drawScope: DrawScope, center: Offset, frameTimeMs: Long) {
        val r = 11f
        // Soft Vector Aura
        drawScope.drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.22f), radius = r * 2.2f, center = center)

        // Core Disk with Horizontal Bands
        drawScope.drawCircle(
            color = Color(0xFFFEF3C7),
            radius = r,
            center = center
        )

        // Minimal Horizontal Vector Band Lines
        val band1Y = center.y - r * 0.35f
        val band2Y = center.y + r * 0.35f
        drawScope.drawLine(
            color = Color(0xFFD97706).copy(alpha = 0.85f),
            start = Offset(center.x - r * 0.85f, band1Y),
            end = Offset(center.x + r * 0.85f, band1Y),
            strokeWidth = 2.2f
        )
        drawScope.drawLine(
            color = Color(0xFFB45309).copy(alpha = 0.85f),
            start = Offset(center.x - r * 0.85f, band2Y),
            end = Offset(center.x + r * 0.85f, band2Y),
            strokeWidth = 2.2f
        )

        // Vector Outline
        drawScope.drawCircle(color = Color(0xFFF59E0B), radius = r, center = center, style = Stroke(width = 1.2f))

        // --- GALILEAN MOONS (Io, Europa, Ganymede, Callisto as tiny vector dots) ---
        val moonDistances = listOf(r * 2.4f, r * 3.6f, r * 5.2f, r * 6.8f)
        val moonSpeeds = listOf(0.0018f, 0.0012f, 0.0008f, 0.0005f)
        val moonColors = listOf(Color(0xFFFDE047), Color(0xFFF1F5F9), Color(0xFFFBBF24), Color(0xFFCBD5E1))

        for (i in 0 until 4) {
            val dist = moonDistances[i]
            val speed = moonSpeeds[i]
            val angle = frameTimeMs * speed + i * 1.57f
            val mx = center.x + dist * cos(angle)
            val my = center.y + (dist * 0.15f) * sin(angle) // Slightly inclined orbit

            drawScope.drawCircle(
                color = moonColors[i],
                radius = 1.8f,
                center = Offset(mx, my)
            )
        }
    }

    private fun drawSaturn(drawScope: DrawScope, center: Offset) {
        val r = 9f

        // Soft Outer Aura
        drawScope.drawCircle(color = Color(0xFFFDE047).copy(alpha = 0.2f), radius = r * 2.2f, center = center)

        // Minimal Tilted Saturn Ring Oval
        val ringRect = Rect(center.x - r * 2.5f, center.y - r * 0.7f, center.x + r * 2.5f, center.y + r * 0.7f)
        drawScope.drawOval(
            color = Color(0xFFFDE047),
            topLeft = ringRect.topLeft,
            size = ringRect.size,
            style = Stroke(width = 2.0f)
        )

        // Saturn Core Disk
        drawScope.drawCircle(
            color = Color(0xFFFEF08A),
            radius = r,
            center = center
        )
        drawScope.drawCircle(
            color = Color(0xFFCA8A04),
            radius = r,
            center = center,
            style = Stroke(width = 1.2f)
        )
    }

    private fun drawMars(drawScope: DrawScope, center: Offset) {
        val r = 7f
        drawScope.drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.25f), radius = r * 2.2f, center = center)
        drawScope.drawCircle(
            color = Color(0xFFEF4444),
            radius = r,
            center = center
        )
        // Vector Polar Ice Cap
        drawScope.drawCircle(
            color = Color.White,
            radius = r * 0.35f,
            center = Offset(center.x, center.y - r * 0.6f)
        )
        drawScope.drawCircle(
            color = Color(0xFFB91C1C),
            radius = r,
            center = center,
            style = Stroke(width = 1.0f)
        )
    }

    private fun drawVenus(drawScope: DrawScope, center: Offset) {
        val r = 8.5f
        drawScope.drawCircle(color = Color(0xFFFEF08A).copy(alpha = 0.35f), radius = r * 2.5f, center = center)
        drawScope.drawCircle(
            color = Color.White,
            radius = r,
            center = center
        )
        drawScope.drawCircle(
            color = Color(0xFFFDE047),
            radius = r,
            center = center,
            style = Stroke(width = 1.2f)
        )
    }

    private fun drawMercury(drawScope: DrawScope, center: Offset) {
        val r = 5.5f
        drawScope.drawCircle(
            color = Color(0xFFE2E8F0),
            radius = r,
            center = center
        )
        drawScope.drawCircle(
            color = Color(0xFF94A3B8),
            radius = r,
            center = center,
            style = Stroke(width = 1.0f)
        )
    }

    private fun drawUranus(drawScope: DrawScope, center: Offset) {
        val r = 6.5f
        drawScope.drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.3f), radius = r * 2.0f, center = center)
        drawScope.drawCircle(
            color = Color(0xFF38BDF8),
            radius = r,
            center = center
        )
        drawScope.drawCircle(
            color = Color(0xFF0284C7),
            radius = r,
            center = center,
            style = Stroke(width = 1.0f)
        )
    }

    private fun drawNeptune(drawScope: DrawScope, center: Offset) {
        val r = 6f
        drawScope.drawCircle(color = Color(0xFF60A5FA).copy(alpha = 0.3f), radius = r * 2.0f, center = center)
        drawScope.drawCircle(
            color = Color(0xFF3B82F6),
            radius = r,
            center = center
        )
        drawScope.drawCircle(
            color = Color(0xFF1D4ED8),
            radius = r,
            center = center,
            style = Stroke(width = 1.0f)
        )
    }
}
