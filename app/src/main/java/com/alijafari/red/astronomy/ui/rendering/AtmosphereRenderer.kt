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
        theme: SkyCanvasTheme = SkyCanvasTheme.CELESTIAL
    ) {
        when (theme) {
            SkyCanvasTheme.CELESTIAL -> drawCelestialAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.MONOCHROME -> drawMonochromeAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.FUN -> drawFunAtmosphere(drawScope, lightingState, sunPosPx)
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

    private fun drawFunAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val sunAlt = lightingState.sunAltitudeDeg

        // 1. Crayon paper background sky colors
        val (topColor, bottomColor) = when {
            sunAlt > 6.0 -> Pair(Color(0xFF38BDF8), Color(0xFFBAE6FD))  // Bright Crayon Sky Blue
            sunAlt > 0.0 -> Pair(Color(0xFFFB923C), Color(0xFFFDE047))  // Warm Orange/Yellow Sunset Crayon
            sunAlt > -6.0 -> Pair(Color(0xFFF472B6), Color(0xFFFB923C)) // Soft Magenta to Orange Dusk Crayon
            else -> Pair(Color(0xFF1E1B4B), Color(0xFF312E81))          // Deep Crayon Night Indigo
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(colors = listOf(topColor, bottomColor)),
            size = drawScope.size
        )

        // 2. Layered Kid Crayon Diagonal Scribble Lines (Texture simulating wax crayon strokes on paper)
        val crayonStrokeColor = if (sunAlt > 0.0) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.06f)
        val scribbleStep = 18f
        var diag = -height
        while (diag < width + height) {
            val strokePath = Path().apply {
                moveTo(diag, 0f)
                lineTo(diag + height * 0.7f, height)
            }
            drawScope.drawPath(
                path = strokePath,
                color = crayonStrokeColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            diag += scribbleStep
        }

        // 3. Whimsical Hand-Drawn Crayon Doodle Clouds (during daylight & sunset)
        if (sunAlt > -4.0) {
            val cloudColor = Color.White.copy(alpha = 0.85f)
            val cloudOutline = Color(0xFF0284C7).copy(alpha = 0.6f)

            // Cloud 1 (Top Left)
            drawCrayonDoodleCloud(drawScope, Offset(width * 0.22f, height * 0.25f), scale = 1.0f, cloudColor, cloudOutline)

            // Cloud 2 (Top Right)
            drawCrayonDoodleCloud(drawScope, Offset(width * 0.78f, height * 0.32f), scale = 0.82f, cloudColor, cloudOutline)
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
