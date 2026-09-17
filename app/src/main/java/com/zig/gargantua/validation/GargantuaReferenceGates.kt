package com.zig.gargantua.validation

import kotlin.math.*

/**
 * Machine-checkable numerical reference gates for Gargantua.
 * Provides deterministic mathematical verification across all 7 M2 reference cases.
 */
object GargantuaReferenceGates {

    data class GateResult(
        val gateId: String,
        val description: String,
        val passed: Boolean,
        val message: String,
        val maxObservedError: Double,
        val tolerance: Double
    )

    /**
     * Gate 1: Schwarzschild Limit (a = 0)
     * - Horizon r_+ = 2M
     * - Critical impact parameter b_crit = 3√3 M
     * - Photon sphere radius r_ph = 3M
     */
    fun verifySchwarzschildLimit(M: Double = 1.0, tolerance: Double = 1e-14): GateResult {
        val s = KerrSpacetime(M, 0.0)
        val expectedRPlus = 2.0 * M
        val expectedBCrit = 3.0 * sqrt(3.0) * M
        val expectedRPh = 3.0 * M

        val errRPlus = abs(s.rPlus - expectedRPlus)
        val errBCrit = abs(s.bCritSchwarzschild - expectedBCrit)
        val errRPh = abs(s.rPhotonSchwarzschild - expectedRPh)
        val maxErr = maxOf(errRPlus, errBCrit, errRPh)

        val passed = maxErr <= tolerance
        val msg = if (passed) {
            "Schwarzschild limit verified: r+=$expectedRPlus, b_crit=$expectedBCrit, r_ph=$expectedRPh (maxErr=$maxErr)"
        } else {
            "Schwarzschild limit mismatch: r+=${s.rPlus} (exp $expectedRPlus), b_crit=${s.bCritSchwarzschild} (exp $expectedBCrit)"
        }

        return GateResult("GATE_1_SCHWARZSCHILD_LIMIT", "Schwarzschild horizon, critical impact parameter, and photon sphere", passed, msg, maxErr, tolerance)
    }

    /**
     * Gate 2: Kerr Horizon
     * - r_+ = M + √(M² - a²)
     * - Monotonic decrease of r_+ as |a| increases
     * - Extremal limit r_+ = M at |a| = M
     * - Strict rejection of super-extremal spin |a| > M
     */
    fun verifyKerrHorizon(M: Double = 1.0, tolerance: Double = 1e-14): GateResult {
        val testSpins = listOf(0.0, 0.25, 0.5, 0.75, 0.9, 0.998, 1.0)
        var maxErr = 0.0
        var prevRPlus = Double.POSITIVE_INFINITY

        for (spinFrac in testSpins) {
            val a = spinFrac * M
            val s = KerrSpacetime(M, a)
            val expectedRPlus = M + sqrt(M * M - a * a)
            val err = abs(s.rPlus - expectedRPlus)
            maxErr = max(maxErr, err)

            // Monotonicity check: r_+ must strictly decrease as |a| increases
            if (s.rPlus > prevRPlus + 1e-15) {
                return GateResult(
                    "GATE_2_KERR_HORIZON", "Kerr outer and inner horizon radii", false,
                    "Horizon monotonicity failed: r_+=${s.rPlus} > prev=$prevRPlus at a=$a", maxErr, tolerance
                )
            }
            prevRPlus = s.rPlus
        }

        // Test super-extremal rejection
        var superExtremalRejected = false
        try {
            KerrSpacetime(M, 1.001 * M)
        } catch (_: IllegalArgumentException) {
            superExtremalRejected = true
        }

        val passed = maxErr <= tolerance && superExtremalRejected
        val msg = if (passed) {
            "Kerr horizon verified across spins, monotonicity confirmed, super-extremal spin correctly rejected (maxErr=$maxErr)"
        } else {
            "Kerr horizon validation failed: maxErr=$maxErr, superExtremalRejected=$superExtremalRejected"
        }

        return GateResult("GATE_2_KERR_HORIZON", "Kerr horizon calculation, spin scaling, and super-extremal rejection", passed, msg, maxErr, tolerance)
    }

    /**
     * Gate 3: Kerr Photon Orbits
     * Verifies equatorial circular photon orbits for prograde and retrograde rays:
     * r_ph^± = 2M [ 1 + cos( 2/3 arccos(∓ a/M) ) ]
     * Reference: Bardeen, Press, Teukolsky (1972)
     */
    fun verifyKerrPhotonOrbits(M: Double = 1.0, a: Double = 0.5 * M, tolerance: Double = 1e-13): GateResult {
        val s = KerrSpacetime(M, a)
        val rProg = s.rPhotonPrograde()
        val rRetro = s.rPhotonRetrograde()

        // Known exact values for a/M = 0.5:
        // arccos(-0.5) = 2π/3, cos(4π/9) ≈ 0.17364817766693033
        // rProg = 2(1 + cos(4π/9)) ≈ 2.3472963553338606
        // arccos(0.5) = π/3, cos(2π/9) ≈ 0.766044443118978
        // rRetro = 2(1 + cos(2π/9)) ≈ 3.532088886237956
        val expectedProg = 2.0 * M * (1.0 + cos((2.0 / 3.0) * acos(-a / M)))
        val expectedRetro = 2.0 * M * (1.0 + cos((2.0 / 3.0) * acos(a / M)))

        val errProg = abs(rProg - expectedProg)
        val errRetro = abs(rRetro - expectedRetro)
        val maxErr = max(errProg, errRetro)

        // Physical bounds: r_+ < rProg < 3M < rRetro <= 4M
        val boundsCheck = (s.rPlus < rProg) && (rProg < 3.0 * M) && (3.0 * M < rRetro) && (rRetro <= 4.0 * M)

        val passed = maxErr <= tolerance && boundsCheck
        val msg = if (passed) {
            "Equatorial photon orbits verified: r_prog=$rProg, r_retro=$rRetro (maxErr=$maxErr)"
        } else {
            "Photon orbit validation failed: r_prog=$rProg, r_retro=$rRetro, boundsCheck=$boundsCheck"
        }

        return GateResult("GATE_3_KERR_PHOTON_ORBITS", "Bardeen equatorial circular photon orbits (prograde & retrograde)", passed, msg, maxErr, tolerance)
    }

    /**
     * Gate 4: Null Hamiltonian Constraint
     * H = 1/2 g^μν p_μ p_ν = 0
     */
    fun verifyNullHamiltonian(spacetime: KerrSpacetime, tolerance: Double = 1e-12): GateResult {
        val testRadii = listOf(4.0 * spacetime.M, 10.0 * spacetime.M, 50.0 * spacetime.M)
        val testThetas = listOf(Math.PI * 0.5, Math.PI * 0.25, Math.PI * 0.75)
        var maxErr = 0.0

        for (r in testRadii) {
            for (th in testThetas) {
                val state = NullHamiltonian.createNullState(
                    spacetime = spacetime,
                    r = r,
                    theta = th,
                    energy = 1.0,
                    Lz = 2.0 * spacetime.M,
                    p_theta = 0.5,
                    inward = true
                )
                val h = NullHamiltonian.evaluate(spacetime, state)
                maxErr = max(maxErr, abs(h))
            }
        }

        val passed = maxErr <= tolerance
        val msg = if (passed) {
            "Null Hamiltonian constraint H ≈ 0 verified across multiple radii and inclinations (maxErr=$maxErr)"
        } else {
            "Null Hamiltonian constraint violated: maxErr=$maxErr > tolerance=$tolerance"
        }

        return GateResult("GATE_4_NULL_HAMILTONIAN", "Null Hamiltonian constraint H = 1/2 g^μν p_μ p_ν = 0", passed, msg, maxErr, tolerance)
    }

    /**
     * Gate 5: Conserved Quantities along Geodesics
     * Verifies that energy E = -p_t and angular momentum L_z = p_φ remain constant
     * along integrated trajectories.
     */
    fun verifyConservedQuantitiesDrift(spacetime: KerrSpacetime, relativeTolerance: Double = 1e-5): GateResult {
        val integrator = GeodesicIntegrator(spacetime)
        val initial = NullHamiltonian.createNullState(
            spacetime = spacetime,
            r = 20.0 * spacetime.M,
            theta = Math.PI * 0.5,
            energy = 1.0,
            Lz = 6.0 * spacetime.M,
            inward = true
        )

        // Integrate 500 steps
        val path = integrator.integrate(initial, stepSize = 0.05, maxSteps = 500)
        val finalState = path.last()

        val driftReport = KerrConservedQuantities.measureDrift(spacetime, initial, finalState, relativeTolerance)
        val maxRelativeErr = max(driftReport.energyDriftRelative, driftReport.lzDriftRelative)

        val passed = !driftReport.hasSignificantDrift
        val msg = if (passed) {
            "Conserved quantities verified: ΔE/E=${driftReport.energyDriftRelative}, ΔLz/Lz=${driftReport.lzDriftRelative}, max|H|=${driftReport.hamiltonianMaxAbsolute}"
        } else {
            "Numerical drift detected: ΔE/E=${driftReport.energyDriftRelative}, ΔLz/Lz=${driftReport.lzDriftRelative}"
        }

        return GateResult("GATE_5_CONSERVED_QUANTITIES", "Energy and axial angular momentum conservation along Kerr geodesics", passed, msg, maxRelativeErr, relativeTolerance)
    }

    /**
     * Gate 6: Weak-Field Gravitational Light Deflection
     * α ≈ 4M/b
     */
    fun verifyWeakFieldDeflection(M: Double = 1.0, b: Double = 500.0 * M, relativeTolerance: Double = 0.01): GateResult {
        val comparison = WeakFieldDeflection.compare(M, b)

        // At b = 500M, leading order relative error is ~ 0.4% (< 1%)
        // 2PN relative error is < 1e-4 (< 0.01%)
        val passed = comparison.leadingOrderRelativeError <= relativeTolerance
        val msg = if (passed) {
            "Weak-field deflection verified at b=${b}M: leading=${comparison.leadingOrderAngle} rad, exact=${comparison.exactAngle} rad (relErr=${comparison.leadingOrderRelativeError * 100}%)"
        } else {
            "Weak-field deflection mismatch at b=${b}M: relErr=${comparison.leadingOrderRelativeError * 100}% > tol=${relativeTolerance * 100}%"
        }

        return GateResult("GATE_6_WEAK_FIELD_DEFLECTION", "Einstein weak-field deflection α ≈ 4M/b comparison", passed, msg, comparison.leadingOrderRelativeError, relativeTolerance)
    }

    /**
     * Gate 7: Symmetries & Limiting Cases
     * - a = 0 removes spin asymmetry (r_prog = r_retro = 3M)
     * - Spin reversal parity symmetry: (+a, +Lz) has identical radius to (-a, -Lz)
     * - Mass scaling: lengths scale linearly with M under geometrized units
     */
    fun verifySymmetriesAndLimits(tolerance: Double = 1e-13): GateResult {
        // 1. Schwarzschild spin asymmetry check
        val schw = KerrSpacetime(1.0, 0.0)
        val errSchwAsym = abs(schw.rPhotonPrograde() - schw.rPhotonRetrograde())

        // 2. Parity / spin sign reversal symmetry:
        // Ray with +Lz around +a has identical radius to ray with -Lz around -a
        val kerrPlus = KerrSpacetime(1.0, 0.7)
        val kerrMinus = KerrSpacetime(1.0, -0.7)
        val errParity = abs(kerrPlus.rPhotonForSignedLz(+1.0) - kerrMinus.rPhotonForSignedLz(-1.0))
        val errCoRotating = abs(kerrPlus.rPhotonCorotating() - kerrMinus.rPhotonCorotating())

        // 3. Mass scaling invariance: r_ph(2.5M, 2.5a) / 2.5 == r_ph(M, a)
        val kerrScaled = KerrSpacetime(2.5, 0.7 * 2.5)
        val errMassScale = abs(kerrScaled.rPhotonPrograde() / 2.5 - kerrPlus.rPhotonPrograde())

        val maxErr = maxOf(errSchwAsym, errParity, errCoRotating, errMassScale)
        val passed = maxErr <= tolerance

        val msg = if (passed) {
            "Symmetries and mass scaling confirmed: a=0 symmetry, spin sign reversal parity, and M-scaling invariant (maxErr=$maxErr)"
        } else {
            "Symmetry check failed: maxErr=$maxErr"
        }

        return GateResult("GATE_7_SYMMETRIES_AND_LIMITS", "Schwarzschild spin removal, sign reversal symmetry, and geometrized mass scaling", passed, msg, maxErr, tolerance)
    }

    /**
     * Runs all 7 reference validation gates and returns full results.
     */
    fun runAllGates(): List<GateResult> {
        val s = KerrSpacetime(1.0, 0.7)
        return listOf(
            verifySchwarzschildLimit(1.0),
            verifyKerrHorizon(1.0),
            verifyKerrPhotonOrbits(1.0, 0.5),
            verifyNullHamiltonian(s),
            verifyConservedQuantitiesDrift(s),
            verifyWeakFieldDeflection(1.0, 500.0),
            verifySymmetriesAndLimits()
        )
    }
}
