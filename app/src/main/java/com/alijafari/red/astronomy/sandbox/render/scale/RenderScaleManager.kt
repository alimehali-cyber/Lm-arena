package com.alijafari.red.astronomy.sandbox.render.scale

import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Transforms physical SI coordinates (meters) to GPU rendering coordinates.
 *
 * 3D COORDINATE SYSTEM CONVENTION (Right-Handed):
 * - +X: Rightward on the primary orbital plane (Vernal Equinox reference axis)
 * - +Y: Upward normal vector perpendicular to the orbital plane
 * - +Z: Forward/towards-viewer axis completing the right-handed basis
 * - Camera forward direction: towards target point
 * - Up vector: (0, 1, 0)
 *
 * CRITICAL ARCHITECTURAL BOUNDARY:
 * Physics calculation operates exclusively in SI units. This class strictly maps
 * physical values to visual coordinates for OpenGL rendering without mutating physics state.
 */
class RenderScaleManager(
    var scaleMode: ScaleMode = ScaleMode.SOLAR_SYSTEM_COMPRESSED
) {
    /** Reference distance in SI meters representing 10.0 visual units in linear mode. */
    var referenceDistanceMeters: Double = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS

    /** Global magnification multiplier for visual body radii. */
    var bodyRadiusScaleMultiplier: Float = 1.0f

    /**
     * Converts a 3D physical position in SI meters to a 3D visual position in OpenGL world coordinates.
     * Output values are written directly into [outPosition] (indices 0, 1, 2 = x, y, z) to avoid per-frame allocations.
     */
    fun physicsToRenderPosition(
        physX: Double,
        physY: Double,
        physZ: Double,
        outPosition: FloatArray
    ) {
        val distMeters = sqrt(physX * physX + physY * physY + physZ * physZ)
        if (distMeters < 1e-6) {
            outPosition[0] = 0f
            outPosition[1] = 0f
            outPosition[2] = 0f
            return
        }

        val dirX = (physX / distMeters).toFloat()
        val dirY = (physY / distMeters).toFloat()
        val dirZ = (physZ / distMeters).toFloat()

        val visualDist: Float = when (scaleMode) {
            ScaleMode.LINEAR -> {
                val scale = 10.0 / max(referenceDistanceMeters, 1.0)
                (distMeters * scale).toFloat()
            }
            ScaleMode.SOLAR_SYSTEM_COMPRESSED -> {
                // Smooth logarithmic distance compression:
                // Map 1 AU (~1.496e11 m) to ~10.0 render units, and 30 AU (Neptune, 4.5e12 m) to ~35.0 render units
                val auRatio = distMeters / AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS
                if (auRatio <= 1.0) {
                    (auRatio * 10.0).toFloat()
                } else {
                    (10.0 + 16.0 * ln(auRatio) / ln(30.0)).toFloat()
                }
            }
            ScaleMode.PLANETARY_SYSTEM -> {
                // Geared for lunar / planetary distances (e.g., Earth-Moon: 3.84e8 m -> 10 render units)
                val ref = max(referenceDistanceMeters, 1.0e6)
                val ratio = distMeters / ref
                (ratio * 10.0).toFloat()
            }
            ScaleMode.INSPECTION -> {
                (distMeters / max(referenceDistanceMeters, 1.0)).toFloat() * 5.0f
            }
        }

        outPosition[0] = dirX * visualDist
        outPosition[1] = dirY * visualDist
        outPosition[2] = dirZ * visualDist
    }

    /**
     * Converts a physical body radius in SI meters to a visible OpenGL sphere radius.
     * Ensures small bodies remain visibly distinct and touch-selectable while larger bodies
     * maintain aesthetic hierarchy without occluding their entire orbital path.
     */
    fun physicsToRenderRadius(
        radiusMeters: Double,
        isStarOrBlackHole: Boolean = false
    ): Float {
        if (radiusMeters <= 0.0) return 0.1f

        return when (scaleMode) {
            ScaleMode.LINEAR -> {
                val base = (radiusMeters / referenceDistanceMeters).toFloat() * 10f
                base.coerceIn(0.1f, 5.0f) * bodyRadiusScaleMultiplier
            }
            ScaleMode.SOLAR_SYSTEM_COMPRESSED -> {
                // Logarithmic visual sizing for celestial bodies
                // Sun (7e8 m) -> ~1.6 visual radius
                // Earth (6.37e6 m) -> ~0.45 visual radius
                // Moon (1.7e6 m) -> ~0.25 visual radius
                val logRadius = log10(max(radiusMeters, 1.0e3))
                val normalized = ((logRadius - 3.0) / 6.0).toFloat() // 1km to 1,000,000km range
                val visualRadius = 0.2f + normalized * 1.4f
                visualRadius.coerceIn(0.15f, 2.5f) * bodyRadiusScaleMultiplier
            }
            ScaleMode.PLANETARY_SYSTEM -> {
                val logRadius = log10(max(radiusMeters, 1.0e3))
                val visualRadius = 0.25f + ((logRadius - 4.0) / 5.0).toFloat() * 1.2f
                visualRadius.coerceIn(0.2f, 2.0f) * bodyRadiusScaleMultiplier
            }
            ScaleMode.INSPECTION -> {
                1.5f * bodyRadiusScaleMultiplier
            }
        }
    }

    /**
     * Dynamically adapts reference distance based on current active bodies envelope.
     */
    fun fitEnvelope(maxPhysicalDistanceMeters: Double) {
        referenceDistanceMeters = max(maxPhysicalDistanceMeters, 1.0e6)
    }
}
