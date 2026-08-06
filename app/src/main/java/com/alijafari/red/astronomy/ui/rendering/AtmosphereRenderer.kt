package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.alijafari.red.astronomy.domain.SkyCanvasTheme

object AtmosphereRenderer {

    fun drawAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?,
        theme: SkyCanvasTheme = SkyCanvasTheme.COSMIC_PREMIUM
    ) {
        when (theme) {
            SkyCanvasTheme.COSMIC_PREMIUM -> drawCelestialAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> drawMonochromeAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.KIDS_WATERCOLOR -> drawKidsWatercolorAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.OBSERVATORY -> drawObservatoryAtmosphere(drawScope, lightingState, sunPosPx)
        }
    }

    private fun drawCelestialAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Procedural Vertical Gradient
        val skyGradient = Brush.verticalGradient(
            colors = listOf(
                lightingState.skyTone,
                lightingState.horizonTone
            )
        )
        drawScope.drawRect(
            brush = skyGradient,
            size = drawScope.size
        )

        // 2. Solar Atmospheric Scatter Flare
        if (sunPosPx != null && lightingState.sunAltitudeDeg > -10.0) {
            val sunScatterRadius = (height * 0.9f).coerceAtLeast(150f)
            val scatterAlpha = when {
                lightingState.sunAltitudeDeg > 6.0 -> 0.45f
                lightingState.sunAltitudeDeg > 0.0 -> 0.70f
                else -> 0.35f
            }

            val scatterColors = if (lightingState.sunAltitudeDeg > 0.0) {
                listOf(
                    Color(0xFFFEF3C7).copy(alpha = scatterAlpha),
                    Color(0xFFFBBF24).copy(alpha = scatterAlpha * 0.5f),
                    Color(0xFFF59E0B).copy(alpha = scatterAlpha * 0.2f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFFC084FC).copy(alpha = scatterAlpha),
                    Color(0xFF818CF8).copy(alpha = scatterAlpha * 0.4f),
                    Color.Transparent
                )
            }

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = scatterColors,
                    center = sunPosPx,
                    radius = sunScatterRadius
                ),
                radius = sunScatterRadius,
                center = sunPosPx
            )
        }

        // 3. Horizon Haze Layer
        val hazeHeight = height * 0.22f
        val hazeBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                lightingState.horizonTone.copy(alpha = 0.35f)
            ),
            startY = height - hazeHeight,
            endY = height
        )
        drawScope.drawRect(
            brush = hazeBrush,
            topLeft = Offset(0f, height - hazeHeight),
            size = drawScope.size.copy(height = hazeHeight)
        )
    }

    private fun drawMonochromeAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val height = drawScope.size.height
        val sunAlt = lightingState.sunAltitudeDeg

        // Editorial monochrome palette transitions
        val (zenithColor, horizonColor) = when {
            sunAlt > 12.0 -> Pair(Color(0xFFFFFFFF), Color(0xFFF8FAFC)) // Midday: Pure clean white
            sunAlt > 0.0 -> Pair(Color(0xFFF1F5F9), Color(0xFFCBD5E1))  // Golden Hour: Soft warm white into soft grey
            sunAlt > -6.0 -> Pair(Color(0xFF94A3B8), Color(0xFF334155)) // Sunset/Civil Twilight: Soft grey to dark charcoal
            sunAlt > -12.0 -> Pair(Color(0xFF334155), Color(0xFF0F172A))// Nautical Twilight: Dark charcoal to near black
            else -> Pair(Color(0xFF000000), Color(0xFF0F172A))          // Astronomical Night: Pure rich black
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(colors = listOf(zenithColor, horizonColor)),
            size = drawScope.size
        )

        // Subtle soft monochromatic halo around Sun if visible
        if (sunPosPx != null && sunAlt > -6.0) {
            val haloRadius = (height * 0.7f).coerceAtLeast(100f)
            val haloColor = if (sunAlt > 0.0) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor, Color.Transparent),
                    center = sunPosPx,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = sunPosPx
            )
        }
    }

    private fun drawKidsWatercolorAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // Whimsical child-like watercolor background gradient
        val sunAlt = lightingState.sunAltitudeDeg
        val isDay = sunAlt > 0.0

        val skyColors = if (isDay) {
            listOf(
                Color(0xFF70D6FF), // Soft sky cyan
                Color(0xFFFFD670), // Warm pastel sun yellow
                Color(0xFFFF85A1)  // Soft pink horizon wash
            )
        } else {
            listOf(
                Color(0xFF1A1B4B), // Deep indigo ink
                Color(0xFF2E2A72), // Soft violet wash
                Color(0xFF3B1F52)  // Deep plum horizon
            )
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(colors = skyColors),
            size = drawScope.size
        )

        // Soft animated watercolor wash blobs
        val washColor = if (isDay) Color(0xFFFFC6FF).copy(alpha = 0.25f) else Color(0xFF6C5CE7).copy(alpha = 0.2f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(washColor, Color.Transparent),
                center = Offset(width * 0.3f, height * 0.4f),
                radius = width * 0.6f
            ),
            radius = width * 0.6f,
            center = Offset(width * 0.3f, height * 0.4f)
        )
    }

    private fun drawObservatoryAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        // Deep astronomical night vision preserving red mode
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1F0000), Color(0xFF0D0000))
            ),
            size = drawScope.size
        )

        if (sunPosPx != null && lightingState.sunAltitudeDeg > -10.0) {
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFDC2626).copy(alpha = 0.25f), Color.Transparent),
                    center = sunPosPx,
                    radius = 200f
                ),
                radius = 200f,
                center = sunPosPx
            )
        }
    }

    private fun drawCrayonDoodleCloud(
        drawScope: DrawScope,
        center: Offset,
        scale: Float,
        fillColor: Color,
        outlineColor: Color
    ) {
        val r = 20f * scale
        // Soft fluffy kid-drawn cloud circles
        drawScope.drawCircle(color = fillColor, radius = r * 1.3f, center = center)
        drawScope.drawCircle(color = fillColor, radius = r * 1.0f, center = Offset(center.x - r * 1.4f, center.y + r * 0.2f))
        drawScope.drawCircle(color = fillColor, radius = r * 0.9f, center = Offset(center.x + r * 1.4f, center.y + r * 0.3f))
        drawScope.drawCircle(color = fillColor, radius = r * 1.1f, center = Offset(center.x - r * 0.7f, center.y - r * 0.5f))
        drawScope.drawCircle(color = fillColor, radius = r * 1.0f, center = Offset(center.x + r * 0.7f, center.y - r * 0.4f))

        // Crayon outline around cloud
        val cloudPath = Path().apply {
            val r1 = r * 1.3f
            addOval(Rect(center.x - r1, center.y - r1, center.x + r1, center.y + r1))
            val r2 = r * 1.0f
            val c2 = Offset(center.x - r * 1.4f, center.y + r * 0.2f)
            addOval(Rect(c2.x - r2, c2.y - r2, c2.x + r2, c2.y + r2))
            val r3 = r * 0.9f
            val c3 = Offset(center.x + r * 1.4f, center.y + r * 0.3f)
            addOval(Rect(c3.x - r3, c3.y - r3, c3.x + r3, c3.y + r3))
        }
        drawScope.drawPath(path = cloudPath, color = outlineColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round))

        // Cute smile on the main cloud center
        val smilePath = Path().apply {
            moveTo(center.x - r * 0.4f, center.y + r * 0.2f)
            quadraticTo(center.x, center.y + r * 0.6f, center.x + r * 0.4f, center.y + r * 0.2f)
        }
        drawScope.drawPath(path = smilePath, color = outlineColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round))

        // Two cute little eyes
        drawScope.drawCircle(color = outlineColor, radius = 2.2f * scale, center = Offset(center.x - r * 0.35f, center.y - r * 0.1f))
        drawScope.drawCircle(color = outlineColor, radius = 2.2f * scale, center = Offset(center.x + r * 0.35f, center.y - r * 0.1f))
    }
}
