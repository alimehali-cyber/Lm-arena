package com.alijafari.red.astronomy.sandbox.physics

/**
 * Status resulting from numerical validation of the physics simulation state.
 */
sealed class FailSafeStatus {
    object Ok : FailSafeStatus()
    data class NumericalAnomaly(
        val bodyId: String,
        val reason: String,
        val details: String
    ) : FailSafeStatus()
}

/**
 * Validates simulation integrity, catching numerical instabilities, NaN/Infinities, and unphysical values.
 */
class SimulationFailSafe {

    fun validateState(state: PhysicsStateBuffer): FailSafeStatus {
        val n = state.activeCount
        for (i in 0 until n) {
            if (!state.isActive[i]) continue

            val id = state.id[i]
            val px = state.posX[i]; val py = state.posY[i]; val pz = state.posZ[i]
            val vx = state.velX[i]; val vy = state.velY[i]; val vz = state.velZ[i]
            val ax = state.accX[i]; val ay = state.accY[i]; val az = state.accZ[i]
            val m = state.mass[i]
            val r = state.radius[i]

            // 1. Check for NaN
            if (px.isNaN() || py.isNaN() || pz.isNaN()) {
                return FailSafeStatus.NumericalAnomaly(id, "POSITION_NAN", "Position contains NaN: ($px, $py, $pz)")
            }
            if (vx.isNaN() || vy.isNaN() || vz.isNaN()) {
                return FailSafeStatus.NumericalAnomaly(id, "VELOCITY_NAN", "Velocity contains NaN: ($vx, $vy, $vz)")
            }
            if (ax.isNaN() || ay.isNaN() || az.isNaN()) {
                return FailSafeStatus.NumericalAnomaly(id, "ACCELERATION_NAN", "Acceleration contains NaN: ($ax, $ay, $az)")
            }

            // 2. Check for Infinite values
            if (px.isInfinite() || py.isInfinite() || pz.isInfinite()) {
                return FailSafeStatus.NumericalAnomaly(id, "POSITION_INFINITE", "Position is infinite: ($px, $py, $pz)")
            }
            if (vx.isInfinite() || vy.isInfinite() || vz.isInfinite()) {
                return FailSafeStatus.NumericalAnomaly(id, "VELOCITY_INFINITE", "Velocity is infinite: ($vx, $vy, $vz)")
            }
            if (ax.isInfinite() || ay.isInfinite() || az.isInfinite()) {
                return FailSafeStatus.NumericalAnomaly(id, "ACCELERATION_INFINITE", "Acceleration is infinite: ($ax, $ay, $az)")
            }

            // 3. Check for Non-positive Mass or Radius
            if (m <= 0.0 || m.isNaN() || m.isInfinite()) {
                return FailSafeStatus.NumericalAnomaly(id, "INVALID_MASS", "Mass must be positive finite: $m kg")
            }
            if (r <= 0.0 || r.isNaN() || r.isInfinite()) {
                return FailSafeStatus.NumericalAnomaly(id, "INVALID_RADIUS", "Radius must be positive finite: $r m")
            }

            // 4. Check for Absurd Position Range (> 1000 Light Years = ~10^19 m)
            val posSq = px * px + py * py + pz * pz
            if (posSq > MAX_POSITION_SQ) {
                return FailSafeStatus.NumericalAnomaly(id, "ABSURD_POSITION_EJECTION", "Body escaped to extreme distance: r^2 = $posSq")
            }

            // 5. Check for Absurd Superluminal Velocity (> 2.0 c)
            val velSq = vx * vx + vy * vy + vz * vz
            if (velSq > MAX_VELOCITY_SQ) {
                return FailSafeStatus.NumericalAnomaly(id, "ABSURD_VELOCITY", "Body exceeds superluminal threshold: v^2 = $velSq")
            }
        }

        return FailSafeStatus.Ok
    }

    companion object {
        // Max position threshold: 10^22 meters (~1 million light years)
        private const val MAX_POSITION_SQ = 1.0e44

        // Max velocity threshold: 2.0 * c (600,000 km/s)
        private const val MAX_VELOCITY_SQ = (2.0 * AstroPhysicsConstants.SPEED_OF_LIGHT) * (2.0 * AstroPhysicsConstants.SPEED_OF_LIGHT)
    }
}
