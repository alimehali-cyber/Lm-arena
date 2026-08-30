package com.alijafari.red.astronomy.sandbox.physics

import kotlin.math.sqrt

/**
 * Exact Newtonian Pairwise Gravitational Force Solver.
 * Evaluates accelerations using symmetric mutual pair evaluations (F_ij = -F_ji).
 * Employs an explicit, configurable Plummer softening length to prevent numerical division-by-zero
 * during close encounters without claiming softened gravity is physical.
 */
class GravitationalForceSolver(
    var softeningMeters: Double = DEFAULT_SOFTENING_METERS,
    var gravitationalConstant: Double = AstroPhysicsConstants.G
) {
    /**
     * Diagnostic metric: minimum separation distance in meters among all active body pairs.
     */
    var lastMinPairDistanceMeters: Double = Double.MAX_VALUE
        private set

    /**
     * Diagnostic metric: minimum ratio of center distance to sum of radii (r / (R1 + R2)).
     * Values < 1.0 indicate physical surface contact.
     */
    var lastMinProximityRatio: Double = Double.MAX_VALUE
        private set

    /**
     * Accumulated gravitational potential energy computed during the force pass.
     */
    var lastPotentialEnergyJoules: Double = 0.0
        private set

    fun computeAccelerations(state: PhysicsStateBuffer) {
        val n = state.activeCount
        val px = state.posX
        val py = state.posY
        val pz = state.posZ
        val m = state.mass
        val rad = state.radius
        val isFixed = state.isFixed
        val isActive = state.isActive

        val ax = state.accX
        val ay = state.accY
        val az = state.accZ

        // Reset acceleration accumulators
        for (i in 0 until n) {
            ax[i] = 0.0
            ay[i] = 0.0
            az[i] = 0.0
        }

        val eps2 = softeningMeters * softeningMeters
        val g = gravitationalConstant

        var minDistance = Double.MAX_VALUE
        var minProximityRatio = Double.MAX_VALUE
        var totalEp = 0.0

        for (i in 0 until n) {
            if (!isActive[i]) continue
            val pxi = px[i]
            val pyi = py[i]
            val pzi = pz[i]
            val mi = m[i]
            val radi = rad[i]
            val fixedI = isFixed[i]

            for (j in i + 1 until n) {
                if (!isActive[j]) continue
                val mj = m[j]
                val radj = rad[j]
                val fixedJ = isFixed[j]

                val dx = px[j] - pxi
                val dy = py[j] - pyi
                val dz = pz[j] - pzi

                val r2 = dx * dx + dy * dy + dz * dz
                val r = sqrt(r2)

                if (r < minDistance) {
                    minDistance = r
                }

                val sumRadii = radi + radj
                if (sumRadii > 0.0) {
                    val ratio = r / sumRadii
                    if (ratio < minProximityRatio) {
                        minProximityRatio = ratio
                    }
                }

                val softenedR2 = r2 + eps2
                val softenedR = sqrt(softenedR2)
                val invDist3 = 1.0 / (softenedR * softenedR2)

                // Potential energy Ep = - G * m_i * m_j / r_softened
                totalEp -= (g * mi * mj) / softenedR

                val factor = g * invDist3
                val fx = dx * factor
                val fy = dy * factor
                val fz = dz * factor

                // a_i += G * m_j * delta_r / r^3
                if (!fixedI) {
                    ax[i] += mj * fx
                    ay[i] += mj * fy
                    az[i] += mj * fz
                }

                // a_j -= G * m_i * delta_r / r^3 (Newton's 3rd law symmetry)
                if (!fixedJ) {
                    ax[j] -= mi * fx
                    ay[j] -= mi * fy
                    az[j] -= mi * fz
                }
            }
        }

        lastMinPairDistanceMeters = minDistance
        lastMinProximityRatio = minProximityRatio
        lastPotentialEnergyJoules = totalEp
    }

    companion object {
        /**
         * Default minimal Plummer softening length: 1,000 meters.
         * Softens mathematical point singularities at r -> 0 while being negligible
         * compared to planetary scales (> 1,000,000 meters).
         */
        const val DEFAULT_SOFTENING_METERS = 1000.0
    }
}
