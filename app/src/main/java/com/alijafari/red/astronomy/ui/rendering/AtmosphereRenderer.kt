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
import kotlin.math.abs
import kotlin.math.hypot

object AtmosphereRenderer {

    private data class SkyGradientAnchor(
        val altDeg: Double,
        val zenith: Color,
        val mid: Color,
        val horizon: Color
    )

    private val REAL_SKY_ANCHORS = listOf(
        SkyGradientAnchor(35.0, Color(0xFF0E2B63), Color(0xFF2358A3), Color(0xFF7CB4E6)),
        SkyGradientAnchor(15.0, Color(0xFF102C60), Color(0xFF285AA0), Color(0xFF88B6DC)),
        SkyGradientAnchor(5.0, Color(0xFF0B1E44), Color(0xFF2E4D7E), Color(0xFFD6A67E)),
        SkyGradientAnchor(0.0, Color(0xFF08142E), Color(0xFF1E3158), Color(0xFF8C5B6E)),
        SkyGradientAnchor(-3.0, Color(0xFF050B1C), Color(0xFF132040), Color(0xFF3E3B63)),
        SkyGradientAnchor(-6.0, Color(0xFF030712), Color(0xFF0B142C), Color(0xFF212F56)),
        SkyGradientAnchor(-12.0, Color(0xFF02040B), Color(0xFF060B1A), Color(0xFF101C38)),
        SkyGradientAnchor(-18.0, Color(0xFF02030A), Color(0xFF040711), Color(0xFF080E1D))
    )

    private data class TwilightDomeAnchor(
        val altDeg: Double,
        val color: Color,
        val alpha: Float
    )

    private val TWILIGHT_DOME_ANCHORS = listOf(
        TwilightDomeAnchor(14.0, Color(0xFFFFF4CC), 0.18f),
        TwilightDomeAnchor(5.0, Color(0xFFFFB066), 0.42f),
        TwilightDomeAnchor(2.0, Color(0xFFFFA252), 0.56f),
        TwilightDomeAnchor(-3.0, Color(0xFFFF7A3D), 0.48f),
        TwilightDomeAnchor(-9.0, Color(0xFF3A4A80), 0.34f),
        TwilightDomeAnchor(-15.0, Color(0xFF0E1A3A), 0.16f),
        TwilightDomeAnchor(-18.0, Color(0xFF0E1A3A), 0.0f)
    )

    private fun lerpColor(a: Color, b: Color, t: Float): Color {
        val f = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * f,
            green = a.green + (b.green - a.green) * f,
            blue = a.blue + (b.blue - a.blue) * f,
            alpha = a.alpha + (b.alpha - a.alpha) * f
        )
    }

    fun drawAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?,
        theme: SkyCanvasTheme = SkyCanvasTheme.ATMOSPHERIC_SKY,
        sunAzimuthDeg: Double = 180.0,
        latitudeDeg: Double = 0.0
    ) {
        when (theme) {
            SkyCanvasTheme.REAL_SKY -> drawRealSkyAtmosphere(drawScope, lightingState, sunPosPx, sunAzimuthDeg, latitudeDeg)
            SkyCanvasTheme.ATMOSPHERIC_SKY -> drawAtmosphericSky(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.MONOCHROME_SCIENTIFIC -> drawMonochromeAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.KIDS_WATERCOLOR -> drawKidsWatercolorAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.OBSERVATORY -> drawObservatoryAtmosphere(drawScope, lightingState, sunPosPx)
            SkyCanvasTheme.PAPERCRAFT_DIORAMA -> drawPapercraftAtmosphere(drawScope, lightingState, sunPosPx)
        }
    }

    private fun drawRealSkyAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?,
        sunAzimuthDeg: Double,
        latitudeDeg: Double
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val horizonY = height * HeroSkyProjection.HORIZON_FRACTION
        val sunAlt = lightingState.sunAltitudeDeg

        // 1. Smoothly interpolated Zenith -> Mid -> Horizon Preetham/Schaefer gradient
        var zenithColor = REAL_SKY_ANCHORS.last().zenith
        var midColor = REAL_SKY_ANCHORS.last().mid
        var horizonColor = REAL_SKY_ANCHORS.last().horizon

        if (sunAlt >= REAL_SKY_ANCHORS.first().altDeg) {
            zenithColor = REAL_SKY_ANCHORS.first().zenith
            midColor = REAL_SKY_ANCHORS.first().mid
            horizonColor = REAL_SKY_ANCHORS.first().horizon
        } else if (sunAlt > REAL_SKY_ANCHORS.last().altDeg) {
            for (i in 0 until REAL_SKY_ANCHORS.size - 1) {
                val upper = REAL_SKY_ANCHORS[i]
                val lower = REAL_SKY_ANCHORS[i + 1]
                if (sunAlt <= upper.altDeg && sunAlt >= lower.altDeg) {
                    val t = ((upper.altDeg - sunAlt) / (upper.altDeg - lower.altDeg)).toFloat()
                    zenithColor = lerpColor(upper.zenith, lower.zenith, t)
                    midColor = lerpColor(upper.mid, lower.mid, t)
                    horizonColor = lerpColor(upper.horizon, lower.horizon, t)
                    break
                }
            }
        }

        // Apply subtle Rayleigh moonlight brightening on dark nights when the Moon is above the horizon
        if (sunAlt < -6.0 && lightingState.moonGlowIntensity > 0.02f) {
            val moonWash = (lightingState.moonGlowIntensity * 0.16f).coerceIn(0f, 0.16f)
            val moonSkyTint = Color(0xFF182A4A)
            zenithColor = lerpColor(zenithColor, moonSkyTint, moonWash * 0.55f)
            midColor = lerpColor(midColor, moonSkyTint, moonWash * 0.80f)
            horizonColor = lerpColor(horizonColor, moonSkyTint, moonWash)
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(
                0.0f to zenithColor,
                0.55f to midColor,
                0.86f to horizonColor,
                1.0f to horizonColor,
                startY = 0f,
                endY = height
            ),
            size = drawScope.size
        )

        // 2. Sun-centered radial twilight dome (tracks Sun down to -18° astronomical twilight)
        if (sunPosPx != null && sunAlt > -18.0 && sunAlt <= 14.0) {
            var domeColor = TWILIGHT_DOME_ANCHORS.first().color
            var domeAlpha = 0f
            for (i in 0 until TWILIGHT_DOME_ANCHORS.size - 1) {
                val upper = TWILIGHT_DOME_ANCHORS[i]
                val lower = TWILIGHT_DOME_ANCHORS[i + 1]
                if (sunAlt <= upper.altDeg && sunAlt >= lower.altDeg) {
                    val t = ((upper.altDeg - sunAlt) / (upper.altDeg - lower.altDeg)).toFloat()
                    domeColor = lerpColor(upper.color, lower.color, t)
                    domeAlpha = upper.alpha + (lower.alpha - upper.alpha) * t
                    break
                }
            }

            if (domeAlpha > 0.005f) {
                val outerRadius = (hypot(width, height) * 0.78f).coerceAtLeast(220f)
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to domeColor.copy(alpha = domeAlpha),
                        0.38f to domeColor.copy(alpha = domeAlpha * 0.45f),
                        0.72f to domeColor.copy(alpha = domeAlpha * 0.12f),
                        1.0f to Color.Transparent,
                        center = sunPosPx,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = sunPosPx
                )
            }
        }

        // 3. Opposite-horizon Belt of Venus & Earth's Shadow arch during sunrise/sunset (-5° <= sunAlt <= +4°)
        if (sunAlt in -5.0..4.0) {
            val beltEnvelope = (1.0 - abs(sunAlt + 0.5) / 4.5).coerceIn(0.0, 1.0).toFloat()
            if (beltEnvelope > 0.02f) {
                val antiSunAz = (sunAzimuthDeg + 180.0) % 360.0
                val antiCenter = HeroSkyProjection.project(antiSunAz, 3.0, width, height, latitudeDeg)
                val beltRadius = (width * 0.55f).coerceAtLeast(180f)
                // Rose-lavender Belt of Venus arch
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFFD88CA8).copy(alpha = 0.15f * beltEnvelope),
                        0.55f to Color(0xFF9A6B9A).copy(alpha = 0.07f * beltEnvelope),
                        1.0f to Color.Transparent,
                        center = Offset(antiCenter.x, horizonY - 18f),
                        radius = beltRadius
                    ),
                    radius = beltRadius,
                    center = Offset(antiCenter.x, horizonY - 18f)
                )
            }
        }

        // 4. Subtle natural airglow / atmospheric extinction horizon band at night
        if (sunAlt < -12.0) {
            val airglowAlpha = (((-sunAlt - 12.0) / 6.0).coerceIn(0.0, 1.0) * 0.14).toFloat()
            val bandHeight = height * 0.22f
            drawScope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF142038).copy(alpha = airglowAlpha)
                    ),
                    startY = horizonY - bandHeight,
                    endY = horizonY
                ),
                topLeft = Offset(0f, horizonY - bandHeight),
                size = drawScope.size.copy(height = bandHeight)
            )
        }
    }

    private fun drawAtmosphericSky(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Continuous Algorithmic Atmospheric Gradient
        val (zenithColor, horizonColor) = LightingEngine.getAtmosphericSkyColors(lightingState.sunAltitudeDeg)
        val skyGradient = Brush.verticalGradient(
            colors = listOf(zenithColor, horizonColor)
        )
        drawScope.drawRect(
            brush = skyGradient,
            size = drawScope.size
        )

        // 2. Continuous Solar Scatter & Atmospheric Flare
        val sunAlt = lightingState.sunAltitudeDeg
        if (sunPosPx != null && sunAlt > -12.0) {
            val sunScatterRadius = (height * 0.95f).coerceAtLeast(160f)
            val scatterAlpha = when {
                sunAlt > 12.0 -> 0.40f
                sunAlt > 0.0 -> 0.40f + 0.35f * ((12.0 - sunAlt) / 12.0).toFloat() // peak warm flare near horizon
                else -> 0.35f * ((sunAlt + 12.0) / 12.0).toFloat().coerceIn(0f, 1f)
            }

            val flareColors = if (sunAlt > 0.0) {
                listOf(
                    Color(0xFFFEF3C7).copy(alpha = scatterAlpha),
                    Color(0xFFFBBF24).copy(alpha = scatterAlpha * 0.6f),
                    Color(0xFFF59E0B).copy(alpha = scatterAlpha * 0.25f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFFF97316).copy(alpha = scatterAlpha),
                    Color(0xFFC084FC).copy(alpha = scatterAlpha * 0.5f),
                    Color(0xFF818CF8).copy(alpha = scatterAlpha * 0.2f),
                    Color.Transparent
                )
            }

            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = flareColors,
                    center = sunPosPx,
                    radius = sunScatterRadius
                ),
                radius = sunScatterRadius,
                center = sunPosPx
            )
        }

        // 3. Subtle Night Horizon Skyglow / City Lights
        if (sunAlt < -12.0) {
            val hazeHeight = height * 0.25f
            val nightHaze = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF0F172A).copy(alpha = 0.5f),
                    Color(0xFF1E1B4B).copy(alpha = 0.35f)
                ),
                startY = height - hazeHeight,
                endY = height
            )
            drawScope.drawRect(
                brush = nightHaze,
                topLeft = Offset(0f, height - hazeHeight),
                size = drawScope.size.copy(height = hazeHeight)
            )
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

        // Whimsical crayon doodle clouds
        val cloudFill = if (isDay) Color(0xFFFFFDF8).copy(alpha = 0.90f) else Color(0xFF3D348B).copy(alpha = 0.72f)
        val cloudOutline = if (isDay) Color(0xFF4A4E69).copy(alpha = 0.75f) else Color(0xFFB8B8FF).copy(alpha = 0.65f)
        drawCrayonDoodleCloud(
            drawScope = drawScope,
            center = Offset(width * 0.22f, height * 0.24f),
            scale = 1.0f,
            fillColor = cloudFill,
            outlineColor = cloudOutline
        )
        drawCrayonDoodleCloud(
            drawScope = drawScope,
            center = Offset(width * 0.78f, height * 0.20f),
            scale = 0.85f,
            fillColor = cloudFill,
            outlineColor = cloudOutline
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

    private fun drawPapercraftAtmosphere(
        drawScope: DrawScope,
        lightingState: LightingState,
        sunPosPx: Offset?
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val sunAlt = lightingState.sunAltitudeDeg

        // 1. Layered Pastel Cardstock Sky Canvas Gradient
        val skyColors = when {
            sunAlt > 6.0 -> listOf(
                Color(0xFFD6E4F0), // Soft pastel sky blue cardstock
                Color(0xFFE8ECEF), // Pastel mist cream
                Color(0xFFF7F3E9)  // Soft matte cardstock horizon
            )
            sunAlt in 0.0..6.0 -> listOf(
                Color(0xFFF3C5B6), // Soft pastel peach
                Color(0xFFE8B4B8), // Pastel rose blush
                Color(0xFFEFE3C8)  // Soft warm cardstock horizon
            )
            sunAlt in -12.0..0.0 -> listOf(
                Color(0xFF4A4E69), // Pastel lavender twilight
                Color(0xFF9A8C98), // Soft cardstock purple
                Color(0xFFC9ADA7)  // Pastel rose gold
            )
            else -> listOf(
                Color(0xFF1F2432), // Dark matte indigo cardstock
                Color(0xFF2C3446), // Deep pastel slate
                Color(0xFF3D4A5D)  // Soft horizon cardstock
            )
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(skyColors),
            size = drawScope.size
        )

        // 2. Soft Natural Solar Halo Paper Cutouts
        if (sunPosPx != null && sunAlt > -6.0) {
            val sunRadius = 140f
            // Outer paper aura shadow
            drawScope.drawCircle(
                color = Color(0x20221A16),
                radius = sunRadius + 12f,
                center = sunPosPx.copy(x = sunPosPx.x + 4f, y = sunPosPx.y + 6f)
            )
            // Outer pastel ring
            drawScope.drawCircle(
                color = Color(0x33FFF3B0),
                radius = sunRadius,
                center = sunPosPx
            )
            // Inner soft cream ring
            drawScope.drawCircle(
                color = Color(0x44FFEAA7),
                radius = sunRadius * 0.6f,
                center = sunPosPx
            )
        }

        // 3. Layered Papercraft Cutout Clouds with Physical Shadow Offsets
        val cloudColor1 = Color(0xFFFFFDF8) // Off-white top cardstock
        val cloudColor2 = Color(0xFFF2EBE1) // Shadowed lower cardstock
        val shadowColor = Color(0x302C2320)

        // Upper cloud layer
        drawPapercraftCloud(
            drawScope = drawScope,
            center = Offset(width * 0.22f, height * 0.22f),
            scale = 1.2f,
            fillColor = cloudColor1,
            shadowColor = shadowColor
        )

        // Mid cloud layer
        drawPapercraftCloud(
            drawScope = drawScope,
            center = Offset(width * 0.78f, height * 0.18f),
            scale = 0.95f,
            fillColor = cloudColor2,
            shadowColor = shadowColor
        )

        // Lower horizon cloud layer
        drawPapercraftCloud(
            drawScope = drawScope,
            center = Offset(width * 0.52f, height * 0.35f),
            scale = 0.80f,
            fillColor = cloudColor1.copy(alpha = 0.90f),
            shadowColor = shadowColor
        )
    }

    private fun drawPapercraftCloud(
        drawScope: DrawScope,
        center: Offset,
        scale: Float,
        fillColor: Color,
        shadowColor: Color
    ) {
        val r = 24f * scale
        val shadowOffset = Offset(5f * scale, 7f * scale)

        // Shadow Path (Layer behind)
        val shadowCenter = center + shadowOffset
        val shadowPath = Path().apply {
            addOval(Rect(shadowCenter.x - r * 1.5f, shadowCenter.y - r * 1.1f, shadowCenter.x + r * 1.5f, shadowCenter.y + r * 1.1f))
            addOval(Rect(shadowCenter.x - r * 2.2f, shadowCenter.y - r * 0.3f, shadowCenter.x - r * 0.4f, shadowCenter.y + r * 1.2f))
            addOval(Rect(shadowCenter.x + r * 0.4f, shadowCenter.y - r * 0.2f, shadowCenter.x + r * 2.2f, shadowCenter.y + r * 1.2f))
        }
        drawScope.drawPath(path = shadowPath, color = shadowColor)

        // Main Cut Paper Path
        val mainPath = Path().apply {
            addOval(Rect(center.x - r * 1.5f, center.y - r * 1.1f, center.x + r * 1.5f, center.y + r * 1.1f))
            addOval(Rect(center.x - r * 2.2f, center.y - r * 0.3f, center.x - r * 0.4f, center.y + r * 1.2f))
            addOval(Rect(center.x + r * 0.4f, center.y - r * 0.2f, center.x + r * 2.2f, center.y + r * 1.2f))
        }
        drawScope.drawPath(path = mainPath, color = fillColor)

        // Subtle Cardstock Cut Edge Stroke Highlight
        drawScope.drawPath(
            path = mainPath,
            color = Color(0x22000000),
            style = Stroke(width = 1.2f)
        )
    }
}
