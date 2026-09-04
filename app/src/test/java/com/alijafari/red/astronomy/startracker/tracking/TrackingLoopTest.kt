package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.StarObservation
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.Vec3
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.PI

/**
 * TrackingLoop tests for the audit B6 (double clock advance) and B7 (fabricated
 * re-lock result) fixes, executed against the real solver chain.
 */
class TrackingLoopTest {

    private fun northPoleFixture(): Triple<List<CatalogStar>, QuadPatternIndex, List<StarObservation>> {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            NP,0.0,90.0,2.0
            NP1,0.0,85.0,2.0
            NP2,10.0,85.0,2.0
            NP3,350.0,85.0,2.0
            NP4,20.0,83.0,2.1
            NP5,30.0,80.0,2.2
            NP6,200.0,40.0,2.5
        """.trimIndent()
        val stars = CatalogIngestor.parse(csv)
        val quadIndex = QuadPatternIndex(stars, maxSeparationRad = 40.0 * PI / 180.0)
        val observed = SyntheticSkyObserver().observe(
            catalogStars = stars,
            groundTruthAttitude = Quaternion.identity(),
            fovLimitRad = 30.0 * PI / 180.0,
            noiseSigmaRad = 0.0,
            numFalseStars = 0,
            seed = 42L
        )
        return Triple(stars, quadIndex, observed.observations)
    }

    @Test
    fun testClockAdvancesExactlyDtPerGyroSample() {
        // Audit B6: onGyroSample used to advance the shared FakeClock twice per sample
        // (explicit advance + advance inside ConfidenceStateMachine.updateWithTime),
        // so 6 one-second samples moved the clock 12 seconds. It must advance exactly dt.
        val (stars, quadIndex, _) = northPoleFixture()
        val clock = FakeClock(0.0)
        val loop = TrackingLoop(stars, quadIndex, fakeClock = clock)
        loop.initializeWithLock(Quaternion.identity())

        val before = clock.now()
        repeat(6) { loop.onGyroSample(Vec3(0.0, 0.0, 0.0), 1.0) }
        val after = clock.now()

        assertEquals("clock must advance exactly 6 x 1.0s = 6.0s (audit B6: was 12.0s)",
            before + 6.0, after, 1e-12)
    }

    @Test
    fun testRelockUsesRealSolverNumbersNotFabricatedOnes() {
        // Audit B7: the periodic re-lock path used to wrap the local search outcome in a
        // fabricated SolveResult (hardcoded inlierCount=6, confidence=0.8). With the real
        // solver chain on a zero-noise fixture, the actual solve yields confidence 1.0 —
        // a value the fabricated path could never produce.
        val (stars, quadIndex, observations) = northPoleFixture()
        val clock = FakeClock(0.0)
        val loop = TrackingLoop(stars, quadIndex, fakeClock = clock)
        loop.initializeWithLock(Quaternion.identity())
        assertEquals(LockConfidence.FULL_LOCK, loop.currentState.confidence)

        // Let the periodic re-lock policy trigger (default period 5 s)
        repeat(6) { loop.onGyroSample(Vec3(0.0, 0.0, 0.0), 1.0) }

        val state = loop.onNewObservations(observations)

        println("After re-lock: state=${state.confidence}, confidenceValue=${state.confidenceValue}")
        assertEquals(LockConfidence.FULL_LOCK, state.confidence)
        assertEquals("re-lock confidence must be the REAL solver confidence (1.0 on zero-noise fixture), " +
            "not the previously hardcoded 0.8", 1.0, state.confidenceValue, 1e-9)
    }
}
