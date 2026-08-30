package com.alijafari.red.astronomy.sandbox.render.celestial

import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Manages isolated visual rotation and axial tilt orientations for celestial bodies.
 *
 * CRITICAL ARCHITECTURE RULE:
 * This layer is strictly decoupled from the physics engine.
 * Physical orbital trajectories, velocities, and masses remain 100% authoritative and untouched.
 * Visual rotation merely governs texture alignment, axial inclination, and cloud drift.
 */
class CelestialRotationManager {

    /**
     * Calculates the spin angle in degrees around the body's polar axis at simulation time [simTimeSeconds].
     */
    fun calculateSpinAngleDegrees(
        bodyType: SandboxBodyType,
        simTimeSeconds: Double,
        isCloudLayer: Boolean = false
    ): Float {
        val config = CelestialPropertiesRegistry.getConfig(bodyType)
        val periodHours = config.siderealRotationPeriodHours
        if (abs(periodHours) < 1e-4f) return 0.0f

        // Convert simulation seconds to fraction of rotation period
        val periodSeconds = periodHours * 3600.0
        val multiplier = if (isCloudLayer) config.cloudRotationMultiplier else 1.0f

        val rotations = (simTimeSeconds / periodSeconds) * multiplier
        val angleDeg = ((rotations % 1.0) * 360.0).toFloat()
        return if (angleDeg < 0.0f) angleDeg + 360.0f else angleDeg
    }

    /**
     * Computes the 4x4 column-major orientation matrix (Axial Tilt * Polar Spin) for a body.
     * Pure Kotlin math guarantees zero GC allocations and 100% JVM testability without Android stubs.
     * Writes into [outMatrix] starting at [offset].
     */
    fun computeOrientationMatrix(
        bodyType: SandboxBodyType,
        simTimeSeconds: Double,
        outMatrix: FloatArray,
        offset: Int = 0,
        isCloudLayer: Boolean = false
    ) {
        val config = CelestialPropertiesRegistry.getConfig(bodyType)
        val spinAngleDeg = calculateSpinAngleDegrees(bodyType, simTimeSeconds, isCloudLayer)

        val tiltRad = Math.toRadians(config.axialTiltDegrees.toDouble()).toFloat()
        val spinRad = Math.toRadians(spinAngleDeg.toDouble()).toFloat()

        val cz = cos(tiltRad)
        val sz = sin(tiltRad)
        val cy = cos(spinRad)
        val sy = sin(spinRad)

        // Column 0
        outMatrix[offset + 0] = cz * cy
        outMatrix[offset + 1] = sz * cy
        outMatrix[offset + 2] = -sy
        outMatrix[offset + 3] = 0.0f

        // Column 1
        outMatrix[offset + 4] = -sz
        outMatrix[offset + 5] = cz
        outMatrix[offset + 6] = 0.0f
        outMatrix[offset + 7] = 0.0f

        // Column 2
        outMatrix[offset + 8] = cz * sy
        outMatrix[offset + 9] = sz * sy
        outMatrix[offset + 10] = cy
        outMatrix[offset + 11] = 0.0f

        // Column 3
        outMatrix[offset + 12] = 0.0f
        outMatrix[offset + 13] = 0.0f
        outMatrix[offset + 14] = 0.0f
        outMatrix[offset + 15] = 1.0f
    }
}
