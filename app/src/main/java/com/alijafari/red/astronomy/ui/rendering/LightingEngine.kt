package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.abs
import kotlin.math.pow

data class LightingState(
    val sunAltitudeDeg: Double,
    val moonIlluminationPercent: Double,
    val ambientBrightness: Float, // 0.0 (darkest night) to 1.0 (noon)
    val starVisibility: Float,    // 1.0 (clear night) to 0.0 (daylight)
    val moonGlowIntensity: Float,
    val bloomIntensity: Float,
    val skyTone: Color,
    val horizonTone: Color,
    val shadowOpacity: Float
)

object LightingEngine {

    private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
        val r = c1.red + (c2.red - c1.red) * t
        val g = c1.green + (c2.green - c1.green) * t
        val b = c1.blue + (c2.blue - c1.blue) * t
        val a = c1.alpha + (c2.alpha - c1.alpha) * t
        return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
    }

    private fun interpolateColorKnots(alt: Double, knots: List<Pair<Double, Pair<Color, Color>>>): Pair<Color, Color> {
        if (alt <= knots.first().first) return knots.first().second
        if (alt >= knots.last().first) return knots.last().second

        for (i in 0 until knots.size - 1) {
            val (alt1, colors1) = knots[i]
            val (alt2, colors2) = knots[i + 1]
            if (alt in alt1..alt2) {
                val t = ((alt - alt1) / (alt2 - alt1)).toFloat().coerceIn(0f, 1f)
                val s = t * t * (3f - 2f * t) // smoothstep
                val zenith = lerpColor(colors1.first, colors2.first, s)
                val horizon = lerpColor(colors1.second, colors2.second, s)
                return Pair(zenith, horizon)
            }
        }
        return knots.last().second
    }

    private val ATMOSPHERIC_KNOTS = listOf(
        -18.0 to Pair(Color(0xFF020617), Color(0xFF070D1E)), // Astronomical Night
        -12.0 to Pair(Color(0xFF080D26), Color(0xFF131B3E)), // Nautical Twilight
        -6.0  to Pair(Color(0xFF1B1B4B), Color(0xFF382056)), // Civil Twilight
        -2.0  to Pair(Color(0xFF1E2965), Color(0xFF991B1B)), // Pre-Sunrise Crimson
        0.0   to Pair(Color(0xFF1E3A8A), Color(0xFFEA580C)), // Sunrise / Sunset Golden Coral
        3.0   to Pair(Color(0xFF0369A1), Color(0xFFF59E0B)), // Golden Hour Amber
        8.0   to Pair(Color(0xFF0284C7), Color(0xFF38BDF8)), // Morning Sky Cyan
        15.0  to Pair(Color(0xFF0284C7), Color(0xFF7DD3FC)), // Midday Sky Blue
        90.0  to Pair(Color(0xFF0369A1), Color(0xFFBAE6FD))  // Zenith High Noon
    )

    fun getAtmosphericSkyColors(sunAltDeg: Double): Pair<Color, Color> {
        return interpolateColorKnots(sunAltDeg, ATMOSPHERIC_KNOTS)
    }

    fun computeLightingState(
        sunAltDeg: Double,
        moonAltDeg: Double,
        moonIlluminationPercent: Double
    ): LightingState {
        // Calculate ambient light level continuously
        val dayFactor = ((sunAltDeg + 12.0) / 18.0).coerceIn(0.0, 1.0).toFloat()
        val ambient = dayFactor.pow(1.5f)

        // Stars visibility smoothly fading out as solar altitude increases
        val starVis = when {
            sunAltDeg > 0.0 -> 0.0f
            sunAltDeg in -18.0..0.0 -> {
                val t = ((-sunAltDeg) / 18.0).toFloat().coerceIn(0f, 1f)
                t * t // quadratic smooth fade
            }
            else -> 1.0f
        }

        // Moon glow stronger at night when high and illuminated
        val moonHeightFactor = if (moonAltDeg > 0.0) (moonAltDeg / 90.0).coerceIn(0.0, 1.0).toFloat() else 0.0f
        val moonGlow = (moonIlluminationPercent / 100.0).toFloat() * moonHeightFactor * (1.0f - dayFactor)

        val bloom = (0.2f + 0.8f * (1.0f - dayFactor)).coerceIn(0.2f, 1.0f)

        val (sky, horizon) = getSkyColors(sunAltDeg, moonGlow)

        val shadowOpacity = (0.3f + 0.6f * dayFactor).coerceIn(0.3f, 0.9f)

        return LightingState(
            sunAltitudeDeg = sunAltDeg,
            moonIlluminationPercent = moonIlluminationPercent,
            ambientBrightness = ambient,
            starVisibility = starVis,
            moonGlowIntensity = moonGlow,
            bloomIntensity = bloom,
            skyTone = sky,
            horizonTone = horizon,
            shadowOpacity = shadowOpacity
        )
    }

    private fun getSkyColors(sunAltDeg: Double, moonGlow: Float): Pair<Color, Color> {
        return interpolateColorKnots(sunAltDeg, ATMOSPHERIC_KNOTS)
    }
}
