package com.example.ui.rendering

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

    fun computeLightingState(
        sunAltDeg: Double,
        moonAltDeg: Double,
        moonIlluminationPercent: Double
    ): LightingState {
        // Calculate ambient light level
        val dayFactor = ((sunAltDeg + 12.0) / 18.0).coerceIn(0.0, 1.0).toFloat()
        val ambient = dayFactor.pow(1.5f)

        // Stars visible when Sun is below -6 degrees (Civil Twilight)
        val starVis = when {
            sunAltDeg > 0.0 -> 0.0f
            sunAltDeg in -6.0..0.0 -> ((0.0 - sunAltDeg) / 6.0).toFloat() * 0.3f
            sunAltDeg in -12.0..-6.0 -> 0.3f + (( -6.0 - sunAltDeg) / 6.0).toFloat() * 0.5f
            else -> 1.0f
        }

        // Moon glow stronger at night when high and illuminated
        val moonHeightFactor = if (moonAltDeg > 0.0) (moonAltDeg / 90.0).coerceIn(0.0, 1.0).toFloat() else 0.0f
        val moonGlow = (moonIlluminationPercent / 100.0).toFloat() * moonHeightFactor * (1.0f - dayFactor)

        // Bloom intensity
        val bloom = (0.2f + 0.8f * (1.0f - dayFactor)).coerceIn(0.2f, 1.0f)

        // Sky procedural tones
        val (sky, horizon) = getSkyColors(sunAltDeg)

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

    private fun getSkyColors(sunAltDeg: Double): Pair<Color, Color> {
        return when {
            // Day
            sunAltDeg > 6.0 -> Pair(Color(0xFF1E88E5), Color(0xFF64B5F6))
            // Golden Hour / Sunset
            sunAltDeg in 0.0..6.0 -> {
                val t = (sunAltDeg / 6.0).toFloat()
                Pair(
                    Color(0xFF2563EB).compositeOver(Color(0xFFD97706)),
                    Color(0xFFF59E0B)
                )
            }
            // Civil Twilight
            sunAltDeg in -6.0..0.0 -> Pair(Color(0xFF3B0764), Color(0xFFC084FC))
            // Nautical Twilight
            sunAltDeg in -12.0..-6.0 -> Pair(Color(0xFF1E1B4B), Color(0xFF312E81))
            // Astronomical Twilight
            sunAltDeg in -18.0..-12.0 -> Pair(Color(0xFF0F172A), Color(0xFF1E1B4B))
            // Night
            else -> Pair(Color(0xFF020617), Color(0xFF0F172A))
        }
    }
}
