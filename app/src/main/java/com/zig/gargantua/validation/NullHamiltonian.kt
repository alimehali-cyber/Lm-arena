package com.zig.gargantua.validation

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Null Hamiltonian formulation for photon geodesics in Kerr spacetime.
 *
 * For a massless particle (photon):
 * H = 1/2 g^μν p_μ p_ν = 0
 *
 * Canonical momenta in Boyer-Lindquist coordinates:
 * p_μ = (p_t, p_r, p_θ, p_φ) = (-E, p_r, p_θ, L_z)
 */
object NullHamiltonian {

    /**
     * Complete 8D phase space state of a photon ray in Boyer-Lindquist coordinates.
     */
    data class PhotonState(
        val t: Double,
        val r: Double,
        val theta: Double,
        val phi: Double,
        val p_t: Double,     // -E (energy at infinity)
        val p_r: Double,     // radial canonical momentum
        val p_theta: Double, // polar canonical momentum
        val p_phi: Double    // L_z (axial angular momentum)
    ) {
        val energy: Double get() = -p_t
        val Lz: Double get() = p_phi
        val impactParameter: Double get() = if (energy != 0.0) Lz / energy else Double.NaN
    }

    /**
     * Computes H = 1/2 g^μν p_μ p_ν.
     */
    fun evaluate(spacetime: KerrSpacetime, state: PhotonState): Double {
        val g = spacetime.inverseMetric(state.r, state.theta)
        val term_tt = g.g_tt * state.p_t * state.p_t
        val term_tph = 2.0 * g.g_tph * state.p_t * state.p_phi
        val term_phph = g.g_phph * state.p_phi * state.p_phi
        val term_rr = g.g_rr * state.p_r * state.p_r
        val term_thth = g.g_thth * state.p_theta * state.p_theta

        return 0.5 * (term_tt + term_tph + term_phph + term_rr + term_thth)
    }

    /**
     * Constructs an exact null state by solving H = 0 for the radial momentum p_r.
     *
     * @param inward If true, p_r < 0 (photon directed towards the black hole).
     */
    fun createNullState(
        spacetime: KerrSpacetime,
        t: Double = 0.0,
        r: Double,
        theta: Double,
        phi: Double = 0.0,
        energy: Double = 1.0,
        Lz: Double,
        p_theta: Double = 0.0,
        inward: Boolean = true
    ): PhotonState {
        val p_t = -energy
        val p_phi = Lz
        val g = spacetime.inverseMetric(r, theta)

        val nonRadial = g.g_tt * p_t * p_t +
            2.0 * g.g_tph * p_t * p_phi +
            g.g_phph * p_phi * p_phi +
            g.g_thth * p_theta * p_theta

        val prSquared = -nonRadial / g.g_rr
        require(prSquared >= -1e-12) {
            "Forbidden photon state: p_r² = $prSquared < 0 (classically inaccessible turning region)"
        }

        val prMag = sqrt(prSquared.coerceAtLeast(0.0))
        val pr = if (inward) -prMag else prMag

        return PhotonState(
            t = t,
            r = r,
            theta = theta,
            phi = phi,
            p_t = p_t,
            p_r = pr,
            p_theta = p_theta,
            p_phi = p_phi
        )
    }

    /**
     * Verifies that the state satisfies H ≈ 0 within the specified absolute tolerance.
     */
    fun isNull(spacetime: KerrSpacetime, state: PhotonState, tolerance: Double = 1e-10): Boolean {
        return abs(evaluate(spacetime, state)) <= tolerance
    }
}
