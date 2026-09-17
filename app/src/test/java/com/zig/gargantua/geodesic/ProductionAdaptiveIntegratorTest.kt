package com.zig.gargantua.geodesic

import com.zig.gargantua.physics.KerrSchildSpacetime
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Validates Requirement 2: Production adaptive integrator verification.
 *
 * Demonstrates the four essential properties of the production adaptive policy
 *   Δλ(r) = clamp(κ · r, Δλ_min, Δλ_max):
 * 1. Step sizes scale with radius: smaller in strong-field / photon sphere, larger asymptotically.
 * 2. Strict numerical stability through high-curvature regions (|H| bounded, zero NaNs).
 * 3. Monotonic convergence under adaptive parameter refinement (κ -> κ/2 -> κ/4).
 *
 * Note: Fixed-step RK4 convergence is validated independently in [IntegratorConvergenceTest].
 */
class ProductionAdaptiveIntegratorTest {

    @Test
    fun adaptiveStepIsSmallerInStrongFieldAndLargerFartherOut() {
        val M = 1.0
        val spacetime = KerrSchildSpacetime(M = M, a = 0.8)
        val integrator = KerrPhotonIntegrator(
            spacetime = spacetime,
            baseStepFactor = 0.08,
            minStepSize = 0.02,
            maxStepSize = 0.45,
            escapeRadius = 45.0,
            maxSteps = 1200
        )

        // Deflecting ray with impact parameter b = 6M starting at X = -35M
        val initialPhoton = CameraModel.createNullStateFromDirection(
            spacetime = spacetime,
            X = -35.0, Y = 6.0, Z = 1.0,
            dx = 1.0, dy = 0.0, dz = 0.0
        )

        val result = integrator.traceRay(initialPhoton, recordPath = true)
        assertTrue("Ray must escape to background", result.isEscaped)
        assertNotNull("Path must be recorded", result.path)
        assertNotNull("Step sizes must be recorded", result.stepSizes)

        val steps = result.stepSizes!!
        val path = result.path!!
        assertTrue("Path must have multiple steps", steps.size > 20)

        // Find step near closest approach (strong field) and step at start (weak field)
        val initialStep = steps.first()
        val minStep = result.minStepSizeTaken
        val maxStep = result.maxStepSizeTaken

        // At r ~ 35, step = clamp(0.08 * 35, 0.02, 0.45) = 0.45
        // At r ~ 5, step = clamp(0.08 * 5, 0.02, 0.45) = 0.40
        // Near photon sphere, step is strictly smaller
        assertTrue("Step size in strong field must be strictly smaller than maximum step", minStep < maxStep)
        assertTrue("Initial weak-field step must be strictly larger than periastron step", initialStep > minStep)

        // Verify that every recorded step strictly followed the adaptive policy
        for (i in 0 until steps.size) {
            val pt = path[i]
            val r = com.zig.gargantua.physics.KerrSchildCoordinates.computeR(spacetime.a, pt.x, pt.y, pt.z)
            val expectedStep = (0.08 * r).coerceIn(0.02, 0.45)
            val actualStep = steps[i]
            assertEquals(
                "Step at r=$r ($actualStep) must follow clamp(0.08*r)",
                expectedStep,
                actualStep,
                1e-12
            )
        }
    }

    @Test
    fun adaptiveIntegratorRemainsNumericallyStableThroughStrongCurvature() {
        val M = 1.0
        // Test high spin retrograde Kerr black hole
        val spacetime = KerrSchildSpacetime(M = M, a = -0.92)
        val integrator = KerrPhotonIntegrator(
            spacetime = spacetime,
            baseStepFactor = 0.05,
            minStepSize = 0.01,
            maxStepSize = 0.35,
            escapeRadius = 50.0,
            maxSteps = 800
        )

        // Near-critical impact parameter skimming very close to photon sphere
        val initialPhoton = CameraModel.createNullStateFromDirection(
            spacetime = spacetime,
            X = -40.0, Y = 5.2, Z = 0.5,
            dx = 1.0, dy = 0.0, dz = 0.0
        )

        val result = integrator.traceRay(initialPhoton, recordPath = true)

        // Assert numerical stability:
        // 1. Min radius reached deep in strong field (r < 5M)
        assertTrue("Ray must penetrate into strong field, got ${result.minRadiusReached}", result.minRadiusReached < 5.0)

        // 2. Hamiltonian residual strictly bounded
        assertTrue(
            "Max Hamiltonian residual in strong field must remain < 5e-5, got ${result.maxHamiltonianResidual}",
            result.maxHamiltonianResidual < 5e-5
        )

        // 3. No NaNs or infinities anywhere in path
        result.path?.forEach { state ->
            assertFalse("x must be finite", state.x.isNaN() || state.x.isInfinite())
            assertFalse("y must be finite", state.y.isNaN() || state.y.isInfinite())
            assertFalse("z must be finite", state.z.isNaN() || state.z.isInfinite())
            assertFalse("px must be finite", state.p_x.isNaN() || state.p_x.isInfinite())
            assertFalse("py must be finite", state.p_y.isNaN() || state.p_y.isInfinite())
            assertFalse("pz must be finite", state.p_z.isNaN() || state.p_z.isInfinite())
        }
    }

    @Test
    fun productionAdaptiveIntegratorDemonstratesMonotonicConvergenceWithParameterRefinement() {
        val M = 1.0
        val spacetime = KerrSchildSpacetime(M = M, a = 0.7)

        val initialPhoton = CameraModel.createNullStateFromDirection(
            spacetime = spacetime,
            X = -30.0, Y = 6.5, Z = 2.0,
            dx = 1.0, dy = 0.0, dz = 0.0
        )

        // Three adaptive policies with successively tightened factor: kappa = 0.08, 0.04, 0.02
        val kappa1 = 0.08 // Production baseline
        val kappa2 = 0.04 // Refined x2
        val kappa3 = 0.02 // Refined x4

        val int1 = KerrPhotonIntegrator(spacetime, baseStepFactor = kappa1, minStepSize = 0.005, maxStepSize = 0.40, escapeRadius = 40.0, maxSteps = 1500)
        val int2 = KerrPhotonIntegrator(spacetime, baseStepFactor = kappa2, minStepSize = 0.0025, maxStepSize = 0.20, escapeRadius = 40.0, maxSteps = 3000)
        val int3 = KerrPhotonIntegrator(spacetime, baseStepFactor = kappa3, minStepSize = 0.00125, maxStepSize = 0.10, escapeRadius = 40.0, maxSteps = 6000)

        val res1 = int1.traceRay(initialPhoton)
        val res2 = int2.traceRay(initialPhoton)
        val res3 = int3.traceRay(initialPhoton)

        assertTrue("All runs must escape", res1.isEscaped && res2.isEscaped && res3.isEscaped)

        // Compare final spatial coordinates upon reaching escape boundary
        fun dist(s1: PhotonState4D, s2: PhotonState4D): Double {
            val dx = s1.x - s2.x
            val dy = s1.y - s2.y
            val dz = s1.z - s2.z
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        val err12 = dist(res1.finalState, res2.finalState)
        val err23 = dist(res2.finalState, res3.finalState)

        // Tightening adaptive parameters must monotonically reduce trajectory error: err23 < err12
        assertTrue(
            "Adaptive refinement must produce monotonic convergence: err23 ($err23) < err12 ($err12)",
            err23 < err12
        )
    }
}
