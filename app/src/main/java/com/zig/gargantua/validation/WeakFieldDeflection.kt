package com.zig.gargantua.validation

import kotlin.math.*

/**
 * Computes gravitational light deflection in Schwarzschild spacetime
 * and validates numerical geodesic integration against the Einstein weak-field prediction:
 *
 *   α ≈ 4M / b
 *
 * With second-order post-Newtonian correction (Bodenner & Will 2003, Keeton & Petters 2005):
 *   α = 4M/b + (15π - 16)/4 * (M/b)² + O((M/b)³)
 */
object WeakFieldDeflection {

    /**
     * Leading-order Einstein weak-field deflection angle in radians:
     * α_leading = 4M / b
     */
    fun leadingOrderDeflection(M: Double, b: Double): Double {
        require(b > 0.0) { "Impact parameter b must be positive, got b=$b" }
        return 4.0 * M / b
    }

    /**
     * Second-order Post-Newtonian (2PN) deflection angle in radians:
     * α_2PN = 4M/b + (15π - 16)/4 * (M/b)²
     */
    fun secondOrderDeflection(M: Double, b: Double): Double {
        val mOverB = M / b
        val c2 = (15.0 * Math.PI - 16.0) / 4.0 // ≈ 7.78097245
        return 4.0 * mOverB + c2 * mOverB * mOverB
    }

    /**
     * Computes the exact relativistic deflection angle by numerical quadrature of:
     * Δφ = 2 ∫_0^{u_0} du / √( 1/b² - u²(1 - 2Mu) ) - π
     *
     * where u = 1/r, and u_0 is the closest approach root: 1/b² - u_0²(1 - 2Mu_0) = 0.
     */
    fun exactSchwarzschildDeflection(M: Double, b: Double, intervals: Int = 2000): Double {
        require(b > 3.0 * sqrt(3.0) * M) {
            "Impact parameter b=$b must exceed critical impact parameter b_crit = ${3.0 * sqrt(3.0) * M}"
        }

        // Find closest approach u_0 = 1/r_0 using Newton-Raphson
        var u0 = 1.0 / b
        val invB2 = 1.0 / (b * b)
        for (iter in 0 until 50) {
            val f = invB2 - u0 * u0 * (1.0 - 2.0 * M * u0)
            val fPrime = -2.0 * u0 + 6.0 * M * u0 * u0
            val deltaU = f / fPrime
            u0 -= deltaU
            if (abs(deltaU) < 1e-14) break
        }

        // Midpoint numerical quadrature of elliptic integral:
        // Regularize singularity at upper bound u -> u_0 using substitution u = u_0 * sin²(w), du = 2 u_0 sin(w) cos(w) dw
        val nw = intervals
        val dw = (Math.PI * 0.5) / nw
        var integralSum = 0.0

        for (i in 0 until nw) {
            val w = (i + 0.5) * dw
            val sinW = sin(w)
            val cosW = cos(w)
            val u = u0 * sinW * sinW

            // Radicand: 1/b² - u²(1 - 2Mu) = u0²(1 - 2Mu0) - u²(1 - 2Mu)
            // = (u0² - u²) - 2M(u0³ - u³)
            // = (u0 - u) [ (u0 + u) - 2M(u0² + u0 u + u²) ]
            // Since (u0 - u) = u0 (1 - sin²w) = u0 cos²w, the cos(w) in du cancels the singularity!
            val factor = (u0 + u) - 2.0 * M * (u0 * u0 + u0 * u + u * u)
            if (factor > 0.0) {
                val integrand = (2.0 * u0 * sinW) / sqrt(u0 * factor)
                integralSum += integrand * dw
            }
        }

        val totalPhi = 2.0 * integralSum
        return totalPhi - Math.PI
    }

    data class DeflectionComparison(
        val impactParameter: Double,
        val leadingOrderAngle: Double,
        val secondOrderAngle: Double,
        val exactAngle: Double,
        val leadingOrderRelativeError: Double,
        val secondOrderRelativeError: Double
    )

    fun compare(M: Double, b: Double): DeflectionComparison {
        val leading = leadingOrderDeflection(M, b)
        val second = secondOrderDeflection(M, b)
        val exact = exactSchwarzschildDeflection(M, b)

        val errLeading = abs(exact - leading) / exact
        val errSecond = abs(exact - second) / exact

        return DeflectionComparison(
            impactParameter = b,
            leadingOrderAngle = leading,
            secondOrderAngle = second,
            exactAngle = exact,
            leadingOrderRelativeError = errLeading,
            secondOrderRelativeError = errSecond
        )
    }
}
