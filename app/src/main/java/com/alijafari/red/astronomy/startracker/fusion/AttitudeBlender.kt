package com.alijafari.red.astronomy.startracker.fusion

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import kotlin.math.*

/**
 * Core blending logic: blends existing fused quaternion (from OrientationProvider rotation-vector + SLERP)
 * with star-solved quaternion based on confidence and age.
 *
 * Safety: no-star-lock passthrough must be numerically identical (within floating-point tolerance) to input.
 */

data class BlendResult(
    val outputQuaternion: Quaternion,
    val recommendedMagWeight: Float
)

class AttitudeBlender {

    /**
     * Blend existing fused quaternion with star-solved quaternion.
     * @param existingFusedQuaternion quaternion from existing rotation-vector + SLERP fusion
     * @param starSolvedQuaternion quaternion from star tracker, null if no lock
     * @param starLockConfidence confidence enum
     * @param starLockAgeSeconds age since last successful lock
     * @param currentMagnetometerWeight current magnetometer weight (from OrientationProvider)
     * @return BlendResult with output quaternion and recommended mag weight
     */
    fun blend(
        existingFusedQuaternion: Quaternion,
        starSolvedQuaternion: Quaternion?,
        starLockConfidence: LockConfidence,
        starLockAgeSeconds: Double,
        currentMagnetometerWeight: Float
    ): BlendResult {

        // If feature flag disabled, passthrough (handled outside, but also safety here)
        if (!StarTrackerConfig.ENABLED) {
            return BlendResult(existingFusedQuaternion, currentMagnetometerWeight)
        }

        // No-star-lock passthrough cases: null, NO_LOCK, AMBIGUOUS, or stale
        if (starSolvedQuaternion == null ||
            starLockConfidence == LockConfidence.NO_LOCK ||
            starLockConfidence == LockConfidence.AMBIGUOUS ||
            starLockAgeSeconds > StarTrackerConfig.STALENESS_THRESHOLD_SECONDS + StarTrackerConfig.STALENESS_DECAY_WINDOW_SECONDS
        ) {
            // Exact passthrough — regression safety case, must be numerically identical
            return BlendResult(existingFusedQuaternion, currentMagnetometerWeight)
        }

        // Compute blend fraction based on confidence and staleness
        val baseBlendFraction = when (starLockConfidence) {
            LockConfidence.FULL_LOCK -> StarTrackerConfig.FULL_LOCK_BLEND_FRACTION
            LockConfidence.MARGINAL_LOCK -> StarTrackerConfig.MARGINAL_LOCK_BLEND_FRACTION
            else -> 0.0 // already handled NO_LOCK/AMBIGUOUS above
        }

        // Staleness decay: smoothly decrease blend weight as age increases past threshold
        val stalenessFactor = if (starLockAgeSeconds <= StarTrackerConfig.STALENESS_THRESHOLD_SECONDS) {
            1.0
        } else {
            val decayProgress = (starLockAgeSeconds - StarTrackerConfig.STALENESS_THRESHOLD_SECONDS) / StarTrackerConfig.STALENESS_DECAY_WINDOW_SECONDS
            (1.0 - decayProgress).coerceIn(0.0, 1.0)
        }

        val blendFraction = (baseBlendFraction * stalenessFactor).coerceIn(0.0, 1.0)

        // If blend fraction ~0, passthrough
        if (blendFraction < 1e-6) {
            return BlendResult(existingFusedQuaternion, currentMagnetometerWeight)
        }

        // SLERP between existing and star-solved
        val outputQuaternion = slerp(existingFusedQuaternion, starSolvedQuaternion, blendFraction)

        // Magnetometer weight recommendation
        val recommendedMagWeight = when (starLockConfidence) {
            LockConfidence.FULL_LOCK -> {
                // Strongly reduce toward floor, but with staleness decay
                val floor = StarTrackerConfig.MAGNETOMETER_WEIGHT_FLOOR
                // Interpolate: fresh = floor, stale = current
                val weight = floor + (currentMagnetometerWeight - floor) * (1.0 - stalenessFactor)
                weight.toFloat()
            }
            LockConfidence.MARGINAL_LOCK -> {
                val marginal = StarTrackerConfig.MAGNETOMETER_WEIGHT_MARGINAL
                val weight = marginal + (currentMagnetometerWeight - marginal) * (1.0 - stalenessFactor)
                weight.toFloat()
            }
            else -> currentMagnetometerWeight
        }

        return BlendResult(outputQuaternion, recommendedMagWeight)
    }

    /**
     * SLERP (spherical linear interpolation) between two quaternions.
     * Smooth, not discontinuous jumps.
     */
    fun slerp(q1: Quaternion, q2: Quaternion, t: Double): Quaternion {
        var q2Copy = q2
        var dot = q1.w * q2.w + q1.x * q2.x + q1.y * q2.y + q1.z * q2.z

        // If dot <0, quaternions are opposite, flip one to take shortest path
        if (dot < 0.0) {
            dot = -dot
            q2Copy = Quaternion(-q2.w, -q2.x, -q2.y, -q2.z)
        }

        // If very close, use linear interpolation to avoid division by zero
        if (dot > 0.9995) {
            val result = Quaternion(
                w = q1.w + t * (q2Copy.w - q1.w),
                x = q1.x + t * (q2Copy.x - q1.x),
                y = q1.y + t * (q2Copy.y - q1.y),
                z = q1.z + t * (q2Copy.z - q1.z)
            ).normalized()
            return result
        }

        val theta0 = acos(dot.coerceIn(-1.0, 1.0))
        val sinTheta0 = sin(theta0)

        val theta = theta0 * t
        val sinTheta = sin(theta)

        val s0 = cos(theta) - dot * sinTheta / sinTheta0
        val s1 = sinTheta / sinTheta0

        return Quaternion(
            w = s0 * q1.w + s1 * q2Copy.w,
            x = s0 * q1.x + s1 * q2Copy.x,
            y = s0 * q1.y + s1 * q2Copy.y,
            z = s0 * q1.z + s1 * q2Copy.z
        ).normalized()
    }
}
