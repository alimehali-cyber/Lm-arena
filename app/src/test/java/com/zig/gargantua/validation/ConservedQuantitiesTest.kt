package com.zig.gargantua.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Validates that energy E = -p_t and axial angular momentum L_z = p_φ
 * are conserved along geodesics, and that the drift detection layer catches
 * significant deviations.
 */
class ConservedQuantitiesTest {

    @Test
    fun conservedQuantitiesRemainConstantAlongTrajectory() {
        val s = KerrSpacetime(M = 1.0, a = 0.6)
        val integrator = GeodesicIntegrator(s)

        val initial = NullHamiltonian.createNullState(
            spacetime = s,
            r = 25.0,
            theta = Math.PI * 0.5,
            energy = 1.0,
            Lz = 5.5,
            inward = true
        )

        val path = integrator.integrate(initial, stepSize = 0.05, maxSteps = 400)
        val finalState = path.last()

        val qInit = KerrConservedQuantities.evaluate(s, initial)
        val qFinal = KerrConservedQuantities.evaluate(s, finalState)

        assertEquals("Energy must be strictly conserved", qInit.energy, qFinal.energy, 1e-14)
        assertEquals("L_z must be strictly conserved", qInit.Lz, qFinal.Lz, 1e-14)
        assertTrue("Hamiltonian must stay near 0", abs(qFinal.hamiltonian) < 1e-5)
    }

    @Test
    fun driftDetectorFlagsArtificialPerturbation() {
        val s = KerrSpacetime(M = 1.0, a = 0.5)
        val initial = NullHamiltonian.createNullState(
            spacetime = s,
            r = 15.0,
            theta = Math.PI * 0.5,
            energy = 1.0,
            Lz = 4.0
        )

        // Artificially corrupted state with 5% energy error
        val corruptedState = initial.copy(p_t = initial.p_t * 1.05)
        val report = KerrConservedQuantities.measureDrift(s, initial, corruptedState, relativeTolerance = 1e-4)

        assertTrue("Drift detector must flag corrupted state", report.hasSignificantDrift)
        assertTrue("Energy drift must be approximately 5%", abs(report.energyDriftRelative - 0.05) < 1e-3)
    }

    @Test
    fun referenceGatePassesDeterministically() {
        val s = KerrSpacetime(1.0, 0.7)
        val result = GargantuaReferenceGates.verifyConservedQuantitiesDrift(s)
        assertTrue("Gate 5 (Conserved quantities drift) must pass: ${result.message}", result.passed)
        assertTrue(result.maxObservedError <= result.tolerance)
    }
}
