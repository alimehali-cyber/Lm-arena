package com.zig.gargantua.geodesic

import com.zig.gargantua.physics.KerrSchildSpacetime
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * Validates Requirement 1: Null Hamiltonian constraint H = 1/2 g^μν p_μ p_ν ≡ 0
 * along photon geodesics during RK4 integration.
 */
class KerrNullConstraintTest {

    @Test
    fun nullConstraintPreservedAlongDiverseRays() {
        val testConfigs = listOf(
            Pair(1.0, 0.0),    // Schwarzschild
            Pair(1.0, 0.5),    // Moderate prograde
            Pair(1.0, -0.6),   // Moderate retrograde
            Pair(1.0, 0.9),    // High spin prograde
            Pair(1.0, -0.9),   // High spin retrograde
            Pair(2.0, 1.8)     // Scaled mass
        )

        val cameraAngles = listOf(
            Pair(0.0, 0.0),      // Direct forward
            Pair(0.1, 0.05),     // Deflecting ray
            Pair(-0.15, 0.1),    // Deflecting ray
            Pair(0.02, 0.01)     // Near shadow boundary
        )

        for ((M, a) in testConfigs) {
            val spacetime = KerrSchildSpacetime(M, a)
            val integrator = KerrPhotonIntegrator(spacetime, maxSteps = 400)
            val camera = CameraModel(posX = 30.0 * M, posY = 0.0, posZ = 5.0 * M)

            for ((ndcX, ndcY) in cameraAngles) {
                val initialPhoton = camera.createInitialPhotonState(spacetime, ndcX, ndcY)

                // 1. Initial state has exact zero Hamiltonian
                val initialH = abs(initialPhoton.hamiltonian(spacetime))
                assertTrue("Initial photon state must satisfy H=0 within 1e-14, got $initialH", initialH < 1e-14)

                // 2. Hamiltonian remains bounded along entire numerical trajectory
                val result = integrator.traceRay(initialPhoton)
                assertTrue(
                    "Max Hamiltonian residual along ray must be < 1e-6 for M=$M, a=$a, got ${result.maxHamiltonianResidual}",
                    result.maxHamiltonianResidual < 1e-6
                )
                assertTrue("Ray must take at least 10 steps", result.stepsTaken > 10)
            }
        }
    }
}
