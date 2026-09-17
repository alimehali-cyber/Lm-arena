package com.zig.gargantua.geodesic

import com.zig.gargantua.physics.KerrSchildSpacetime
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

/**
 * Validates Requirement 6: Integrator convergence.
 * Demonstrates that refining the integration step size (h -> h/2 -> h/4) produces
 * the expected 4th-order convergence ratio (~16 for RK4).
 */
class IntegratorConvergenceTest {

    private fun integrateFixedLambda(
        integrator: KerrPhotonIntegrator,
        initialState: PhotonState4D,
        totalLambda: Double,
        h: Double
    ): DoubleArray {
        val steps = (totalLambda / h + 0.5).toInt()
        var st = doubleArrayOf(
            initialState.x,
            initialState.y,
            initialState.z,
            initialState.p_x,
            initialState.p_y,
            initialState.p_z
        )

        for (i in 0 until steps) {
            st = integrator.rk4Step(st, h)
        }
        return st
    }

    @Test
    fun rk4IntegratorDemonstratesFourthOrderConvergence() {
        val M = 1.0
        val spacetime = KerrSchildSpacetime(M = M, a = 0.0)
        val integrator = KerrPhotonIntegrator(spacetime)

        // Trajectory traversing strong curvature: starting at (5.0, 3.0, 0.0)
        val initialPhoton = CameraModel.createNullStateFromDirection(
            spacetime = spacetime,
            X = 5.0, Y = 3.0, Z = 0.0,
            dx = -0.8, dy = -0.2, dz = 0.0
        )

        val totalLambda = 4.0
        val h1 = 0.4
        val h2 = 0.2
        val h3 = 0.1

        val finalState1 = integrateFixedLambda(integrator, initialPhoton, totalLambda, h1)
        val finalState2 = integrateFixedLambda(integrator, initialPhoton, totalLambda, h2)
        val finalState3 = integrateFixedLambda(integrator, initialPhoton, totalLambda, h3)

        // Spatial position differences
        fun distance(a: DoubleArray, b: DoubleArray): Double {
            val dx = a[0] - b[0]
            val dy = a[1] - b[1]
            val dz = a[2] - b[2]
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        val diff12 = distance(finalState1, finalState2)
        val diff23 = distance(finalState2, finalState3)

        // Ratio of errors: for RK4, O(h⁴), halving step size reduces error by ~ 2⁴ = 16
        val convergenceRatio = diff12 / diff23

        assertTrue("Error must decrease with smaller step: diff23 < diff12", diff23 < diff12)
        assertTrue(
            "Convergence ratio must be close to theoretical 16 for RK4, observed $convergenceRatio",
            convergenceRatio in 12.0..18.0
        )
    }
}
