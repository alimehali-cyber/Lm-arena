package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * S2 (KIND-B): Tetra3-style full-field verification tests.
 *
 * 1) The three S1 zero-noise false-lock trials (deterministic replay of the D ladder,
 *    seed 20260904) must NO LONGER produce a false solve — the gate must reject them.
 * 2) A correct attitude (with 1 px noise) must pass verification with a high fraction.
 * 3) The coordinator must downgrade FULL_LOCK to MARGINAL when the full field is weakly
 *    verified, and keep FULL_LOCK for strongly verified / legacy-default diagnostics.
 */
class FullFieldVerifierTest {

    companion object {
        private val csvPath: String = listOf(
            "data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../../data/startracker/hyg_v36_vle6.5_j2000.csv"
        ).firstOrNull { java.io.File(it).exists() } ?: error("HYG extract not found")

        private val stars by lazy { CatalogIngestor.parse(java.io.File(csvPath).readText(), "HYG_V36_LE6P5") }
        private val quadIndex by lazy { QuadPatternIndex.capped(stars) }
        private val solver by lazy { LostInSpaceSolver(quadIndex, stars) }

        /** Exact replay of the D-ladder attitude draw (SyntheticE2EProbe, seed 20260904). */
        private val ladderAttitudes: List<Quaternion> by lazy {
            val rng = kotlin.random.Random(20260904)
            (0 until 20).map {
                val u1 = rng.nextDouble(); val u2 = rng.nextDouble() * 2 * PI; val u3 = rng.nextDouble() * 2 * PI
                val s1 = sqrt(1.0 - u1); val s2 = sqrt(u1)
                Quaternion(s1 * sin(u2), s1 * cos(u2), s2 * sin(u3), s2 * cos(u3))
            }
        }
    }

    private fun angErrDeg(qTrue: Quaternion, qEst: Quaternion): Double {
        var d = 0.0
        val a = doubleArrayOf(qTrue.w, qTrue.x, qTrue.y, qTrue.z)
        val b = doubleArrayOf(qEst.w, qEst.x, qEst.y, qEst.z)
        for (i in 0 until 4) d += a[i] * b[i]
        return Math.toDegrees(2.0 * acos(abs(d).coerceIn(0.0, 1.0)))
    }

    @Test
    fun `S1 false-lock trials are rejected by the full-field gate`() {
        // trials 2, 14, 18 were the three zero-noise false locks (attitude errors
        // 103.6/122.7/82.4 deg) before S2; each must now end in solve failure
        // (NO_LOCK), never a confident wrong attitude.
        for (t in intArrayOf(2, 14, 18)) {
            val qTrue = ladderAttitudes[t]
            val obsr = SyntheticSkyObserver().observe(
                stars, qTrue, Math.toRadians(31.75), 0.0, 0, 1000L + t)
            val res = solver.solve(obsr.observations)
            if (res.success && res.attitude != null) {
                // if some path still solves, it must at least be CORRECT (no false lock)
                val e = angErrDeg(qTrue, res.attitude!!)
                assertTrue("trial $t solved with attitude error $e deg", e <= 0.5)
            } else {
                assertNotNull("trial $t must fail with a message", res.errorMessage)
                assertTrue(
                    "trial $t failure should be the full-field gate (was: ${res.errorMessage})",
                    res.errorMessage!!.startsWith("Full-field verification failed"))
            }
        }
    }

    @Test
    fun `correct attitude passes verification with high fraction`() {
        val q = ladderAttitudes[0]
        val obsr = SyntheticSkyObserver().observe(
            stars, q, Math.toRadians(31.75), Math.toRadians(57.0 / 3600.0), 0, 1000L) // 1 px noise
        val res = FullFieldVerifier().verify(q, obsr.observations, stars)
        assertTrue("verification must pass, was $res", res.pass)
        assertTrue("matched fraction ${res.fraction} should be >= 0.9", res.fraction >= 0.9)
    }

    @Test
    fun `wrong attitude explains almost nothing`() {
        val q = ladderAttitudes[0]
        val obsr = SyntheticSkyObserver().observe(stars, q, Math.toRadians(31.75), 0.0, 0, 1000L)
        val wrong = ladderAttitudes[5] // a different, unrelated attitude
        val res = FullFieldVerifier().verify(wrong, obsr.observations, stars)
        assertFalse("unrelated attitude must fail verification, was $res", res.pass)
        assertTrue("matched count ${res.matchedDetections} should be far below the gate", res.matchedDetections < 8)
    }

    @Test
    fun `coordinator downgrades FULL_LOCK when full field weakly verified`() {
        val coordinator = com.alijafari.red.astronomy.startracker.diagnostics.ConfidenceLadderCoordinator()
        val base = { matched: Int, frac: Double ->
            com.alijafari.red.astronomy.startracker.diagnostics.CoordinatorInput(
                frameQuality = com.alijafari.red.astronomy.startracker.diagnostics.FrameQuality.GOOD,
                failureReason = null,
                ambiguityResult = com.alijafari.red.astronomy.startracker.diagnostics.AmbiguityResult(
                    com.alijafari.red.astronomy.startracker.diagnostics.AmbiguityDecision.CLEAR_WINNER,
                    com.alijafari.red.astronomy.startracker.diagnostics.Hypothesis(0, 0.9, 10, 0.9),
                    null, 0.0, null),
                solverDiagnostics = com.alijafari.red.astronomy.startracker.diagnostics.SolverDiagnostics(
                    10, 0.9, 0.2, true, matched, frac))
        }
        // weakly verified (matched 5 < 20): must NOT be FULL_LOCK
        assertEquals(
            com.alijafari.red.astronomy.startracker.tracking.LockConfidence.MARGINAL_LOCK,
            coordinator.coordinate(base(5, 0.9)).lockConfidence)
        // strongly verified: FULL_LOCK
        assertEquals(
            com.alijafari.red.astronomy.startracker.tracking.LockConfidence.FULL_LOCK,
            coordinator.coordinate(base(30, 0.8)).lockConfidence)
        // legacy defaults (no full-field data): FULL_LOCK preserved
        assertEquals(
            com.alijafari.red.astronomy.startracker.tracking.LockConfidence.FULL_LOCK,
            coordinator.coordinate(
                base(5, 0.9).copy(solverDiagnostics =
                    com.alijafari.red.astronomy.startracker.diagnostics.SolverDiagnostics(10, 0.9, 0.2, true))
            ).lockConfidence)
    }

    @Test
    fun `T4b radial tolerance curve is base plus c tan2 and envelopes the D5 cubic`() {
        val v = FullFieldVerifier.withUnmodelledDistortionAllowance()
        val c = FullFieldVerifier.unmodelledDistortionAllowanceC()
        // boresight: exactly the flat base tolerance
        assertEquals(v.toleranceRad, v.toleranceFor(Triple(0.0, 0.0, 1.0)), 1e-12)
        // exact form at several radii, and monotone
        var prev = v.toleranceRad
        for (deg in intArrayOf(10, 20, 30, 31)) {
            val th = Math.toRadians(deg.toDouble())
            val uv = Triple(sin(th), 0.0, cos(th))
            val expected = v.toleranceRad + c * tan(th) * tan(th)
            assertEquals(expected, v.toleranceFor(uv), 1e-12)
            assertTrue(v.toleranceFor(uv) > prev)
            prev = v.toleranceFor(uv)
        }
        // envelope property: c*r^2 >= |k1|max * r^3 for r <= tan(31.75 deg)
        val edge = tan(Math.toRadians(31.75))
        for (i in 1..10) {
            val r = edge * i / 10.0
            assertTrue(c * r * r + 1e-12 >= 0.08 * r * r * r)
        }
        // default c=0 keeps the flat S2 behavior everywhere (regression guard)
        val flat = FullFieldVerifier()
        assertEquals(flat.toleranceRad, flat.toleranceFor(Triple(0.0, 0.0, 1.0)), 0.0)
        val corner = Triple(0.5, 0.5, 0.7071067811865476)
        assertEquals(flat.toleranceRad, flat.toleranceFor(corner), 0.0)
    }

    @Test
    fun `T4b allowance rescues an uncalibrated k1 barrel field that flat tolerance rejects`() {
        // dense-field boresight (galactic center) so the cone holds many stars
        val ra = Math.toRadians(266.4); val dec = Math.toRadians(-29.0)
        val target = Triple(cos(dec) * cos(ra), cos(dec) * sin(ra), sin(dec))
        val axis = run {
            var a = Triple(target.second, -target.first, 0.0)
            val n = sqrt(a.first * a.first + a.second * a.second)
            if (n < 1e-9) Triple(1.0, 0.0, 0.0) else Triple(a.first / n, a.second / n, 0.0)
        }
        val q = Quaternion.fromAxisAngle(axis, Math.toRadians(60.0)) // tilt GC field toward boresight
        fun distort(v: Triple<Double, Double, Double>, k1: Double): Triple<Double, Double, Double> {
            if (k1 == 0.0) return v
            val x = v.first / v.third; val y = v.second / v.third
            val f = 1.0 + k1 * (x * x + y * y)
            val xd = x * f; val yd = y * f
            val n = sqrt(xd * xd + yd * yd + 1.0)
            return Triple(xd / n, yd / n, 1.0 / n)
        }
        val cone = Math.toRadians(31.75)
        val obs = stars.asSequence()
            .map { q.rotateVector(it.toUnitVector()) }
            .filter { it.third >= cos(cone) }
            .map { StarObservation(distort(it, -0.08), 1.0, false, "D") }
            .toList()
        assertTrue("field should hold >= 60 stars (found ${obs.size})", obs.size >= 60)
        val flat = FullFieldVerifier().verify(q, obs, stars)
        val allow = FullFieldVerifier.withUnmodelledDistortionAllowance().verify(q, obs, stars)
        // flat: edge stars displaced by up to 0.08*tan^3(31.75 deg) = 3911" >> 300" -> gate FAILS
        assertTrue("flat should fail the uncalibrated field (was $flat)", !flat.pass)
        // allowance: tolerance >= displacement by construction -> gate PASSES. In a field
        // this dense a 65' inward displacement can make a NEIGHBOR the nearest prediction,
        // so exact-total matching is not asserted — gate pass and strict dominance are.
        assertTrue("allowance should pass (was $allow)", allow.pass)
        assertTrue("allowance should dominate flat (allow ${allow.matchedDetections} vs flat ${flat.matchedDetections})",
            allow.matchedDetections > flat.matchedDetections)
    }
}
