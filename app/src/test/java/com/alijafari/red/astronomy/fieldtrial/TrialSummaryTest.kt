package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.FieldTrialMachine
import com.alijafari.red.astronomy.fieldtrial.engine.Json
import com.alijafari.red.astronomy.fieldtrial.engine.LevelOutcome
import com.alijafari.red.astronomy.fieldtrial.engine.SkipReason
import com.alijafari.red.astronomy.fieldtrial.engine.TapMeasurement
import com.alijafari.red.astronomy.fieldtrial.engine.TrialSummary
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-4.1: summary.md generator — all numbers present, plain English, stable. */
class TrialSummaryTest {

    private fun tap(target: String, sep: Double, dAz: Double) = TapMeasurement.of(
        1757000000000L, target, 100.0, 40.0, 100.0 + dAz, 40.0,
        500.0, 900.0, 530.0, 900.0,
        100.0, 40.0, 0.0, null, 35.7, 51.4, 6.0, "FALLBACK_DEFAULT",
        800.0, 800.0, 540.0, 1170.0, "NONE", null, null, 4.9, 1.0, 0
    ).copy(separationDeg = sep, dAzDeg = dAz, dAltDeg = 0.0)

    @Test
    fun `summary contains every recorded number in plain english`() {
        var doc = FieldTrialMachine.newTrial("trial-x", 1757000000000L, "Pixel 8", 35, 50.1, 8.7)
        doc = FieldTrialMachine.open(doc, 1, 1757000000000L)
        doc = FieldTrialMachine.addMeasurement(doc, tap("SUN", 2.35, 2.1))
        doc = FieldTrialMachine.complete(doc, LevelOutcome.PASS, 1757000060000L)
        doc = FieldTrialMachine.open(doc, 2, 1757000100000L)
        doc = FieldTrialMachine.skip(doc, SkipReason.CLOUDS, 1757000110000L)
        doc = FieldTrialMachine.open(doc, 8, 1757000200000L)
        doc = FieldTrialMachine.addAuto(doc, "firstFullLockMs", Json.JNum(21300.0), 1757000213300L)
        doc = FieldTrialMachine.addAuto(doc, "acquisitionDiscrepancyDeg", Json.JNum(1.2), 1757000213300L)
        doc = FieldTrialMachine.complete(doc, LevelOutcome.PASS, 1757000300000L)

        val md = TrialSummary.generate(doc)
        val checks = listOf(
            "trial-x", "Pixel 8", "L1", "L2", "L8",
            "2.35", "2.10", "21300", "1.20",
            "Skipped (Clouds)", "PASS",
            "Compass correction applied: 4.90", "Time to first solid lock (ms): 21300",
            "Gap between star and compass direction (deg): 1.200"
        )
        for (c in checks) assertTrue("missing <$c> in:\n$md", c in md)
        // deterministic
        assertTrue(md == TrialSummary.generate(doc))
    }

    @Test
    fun `mutation guard - changing a measurement changes the summary`() {
        var doc = FieldTrialMachine.newTrial("t", 0L, "d", 35, 0.0, 0.0)
        doc = FieldTrialMachine.open(doc, 3, 0L)
        doc = FieldTrialMachine.addMeasurement(doc, tap("VEGA", 0.42, 0.1))
        doc = FieldTrialMachine.complete(doc, LevelOutcome.PASS, 10L)
        val md1 = TrialSummary.generate(doc)
        val m2 = doc.latest(3)!!.measurements[0].copy(separationDeg = 9.9)
        var doc2 = FieldTrialMachine.open(doc, 3, 0L) // redo attempt
        doc2 = FieldTrialMachine.addMeasurement(doc2, m2)
        doc2 = FieldTrialMachine.complete(doc2, LevelOutcome.PASS, 20L)
        val md2 = TrialSummary.generate(doc2)
        assertTrue(md1 != md2 && "9.900" in md2 && "0.420" in md1)
    }
}
