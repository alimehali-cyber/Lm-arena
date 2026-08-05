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
            sunAltDeg in -6.0..0.0 -> ((0.0 - sunAltDeg) / 6.0).toFloat() * 0.35f
            sunAltDeg in -12.0..-6.0 -> 0.35f + ((-6.0 - sunAltDeg) / 6.0).toFloat() * 0.5f
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
        return when {
            // Midday (Solid calm sky blue to soft cyan horizon)
            sunAltDeg > 12.0 -> Pair(Color(0xFF0284C7), Color(0xFF38BDF8))

            // Late Afternoon / Golden Hour Start (Sky blue to warm golden tint)
            sunAltDeg in 6.0..12.0 -> {
                val t = ((12.0 - sunAltDeg) / 6.0).toFloat()
                Pair(
                    Color(0xFF0369A1),
                    Color(0xFF38BDF8).compositeOver(Color(0xFFFBBF24).copy(alpha = t * 0.6f))
                )
            }

            // Golden Hour / Sunset (Soft warm yellow to coral/orange gradient)
            sunAltDeg in 0.0..6.0 -> {
                val t = ((6.0 - sunAltDeg) / 6.0).toFloat()
                Pair(
                    Color(0xFF1E3A8A).compositeOver(Color(0xFF9333EA).copy(alpha = t * 0.5f)),
                    Color(0xFFF59E0B).compositeOver(Color(0xFFE11D48).copy(alpha = t * 0.6f))
                )
            }

            // Civil Twilight (Lavender into navy & coral horizon)
            sunAltDeg in -6.0..0.0 -> {
                val t = ((-sunAltDeg) / 6.0).toFloat()
                Pair(
                    Color(0xFF311B92).compositeOver(Color(0xFF1E1B4B).copy(alpha = t)),
                    Color(0xFFC084FC).compositeOver(Color(0xFF818CF8).copy(alpha = t))
                )
            }

            // Nautical Twilight (Deep blue into indigo)
            sunAltDeg in -12.0..-6.0 -> {
                val t = ((-sunAltDeg - 6.0) / 6.0).toFloat()
                Pair(
                    Color(0xFF1E1B4B),
                    Color(0xFF312E81).compositeOver(Color(0xFF1E293B).copy(alpha = t))
                )
            }

            // Astronomical Twilight / Night (Rich dark indigo into deep navy)
            else -> {
                if (moonGlow > 0.25f) {
                    // Moonlit Night: Dark navy with subtle cool tint
                    Pair(Color(0xFF0B132B), Color(0xFF1C2541))
                } else {
                    // Deep Night
                    Pair(Color(0xFF020617), Color(0xFF0F172A))
                }
            }
        }
    }
}
