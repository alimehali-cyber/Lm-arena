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
}
