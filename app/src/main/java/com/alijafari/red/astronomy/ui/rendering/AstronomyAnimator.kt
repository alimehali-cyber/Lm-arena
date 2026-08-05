package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.sin
import kotlin.math.cos

/**
 * RED Mathematical Astronomy Engine (RMAE)
 * AstronomyAnimator: Unified timing & spring physics animation system.
 * Targets smooth 60-120 FPS procedural animation.
 */
object AstronomyAnimator {

    // Spring physics configuration for Apple/Linear design feels
    val PremiumSpringSpec = spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioLowBouncy
    )

    val PreciseSpringSpec = spring<Float>(
        stiffness = Spring.StiffnessHigh,
        dampingRatio = Spring.DampingRatioNoBouncy
    )

    /**
     * Mathematical sine-wave pulse helper [minVal, maxVal] over periodMs.
     */
    fun computePulse(frameTimeMs: Long, periodMs: Float = 3000f, minVal: Float = 0.85f, maxVal: Float = 1.15f): Float {
        val cycle = (frameTimeMs % periodMs.toLong()) / periodMs
        val sinVal = (sin(cycle * 2.0 * Math.PI) + 1.0) / 2.0
        return (minVal + (maxVal - minVal) * sinVal).toFloat()
    }

    /**
     * Unsynchronized star twinkle opacity calculation based on star ID hash.
     */
    fun computeStarTwinkle(starIdHash: Int, frameTimeMs: Long, baseAlpha: Float): Float {
        val speed = 0.001f + (starIdHash % 7) * 0.0003f
        val phase = starIdHash * 1.37f
        val factor = 0.75f + 0.25f * sin(frameTimeMs * speed + phase).toFloat()
        return (baseAlpha * factor).coerceIn(0.05f, 1.0f)
    }

    /**
     * Smoothly interpolates an offset towards a target position.
     */
    fun interpolateOffset(current: Offset, target: Offset, fraction: Float): Offset {
        return Offset(
            x = current.x + (target.x - current.x) * fraction,
            y = current.y + (target.y - current.y) * fraction
        )
    }
}
