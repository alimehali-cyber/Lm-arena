package com.zig.gargantua.validation

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Validates the Einstein weak-field gravitational light deflection:
 *   α ≈ 4M / b
 * with second-order post-Newtonian correction.
 */
class WeakFieldDeflectionTest {

    @Test
    fun weakFieldApproximationImprovesInverselyWithImpactParameter() {
        val M = 1.0

        val comp100 = WeakFieldDeflection.compare(M, 100.0)
        val comp500 = WeakFieldDeflection.compare(M, 500.0)
        val comp1000 = WeakFieldDeflection.compare(M, 1000.0)

        // At b = 100M: relErr ~ 2.97% (< 3.5%)
        assertTrue(comp100.leadingOrderRelativeError < 0.035)
        // At b = 500M: relErr ~ 0.59% (< 0.8%)
        assertTrue(comp500.leadingOrderRelativeError < 0.008)
        // At b = 1000M: relErr ~ 0.30% (< 0.4%)
        assertTrue(comp1000.leadingOrderRelativeError < 0.004)

        // Monotonic improvement of weak-field approximation as b increases
        assertTrue(comp1000.leadingOrderRelativeError < comp500.leadingOrderRelativeError)
        assertTrue(comp500.leadingOrderRelativeError < comp100.leadingOrderRelativeError)
    }

    @Test
    fun secondOrderPostNewtonianCorrectionMatchesExactQuadrature() {
        val M = 1.0
        val comp200 = WeakFieldDeflection.compare(M, 200.0)

        // Second-order post-Newtonian correction should have relative error < 1% at b = 200M (~0.52%)
        assertTrue(
            "2PN relative error must be < 1%, got ${comp200.secondOrderRelativeError * 100}%",
            comp200.secondOrderRelativeError < 0.01
        )
    }

    @Test
    fun referenceGatePassesDeterministically() {
        val result = GargantuaReferenceGates.verifyWeakFieldDeflection(1.0, 500.0)
        assertTrue("Gate 6 (Weak-field deflection) must pass: ${result.message}", result.passed)
        assertTrue(result.maxObservedError <= result.tolerance)
    }
}
