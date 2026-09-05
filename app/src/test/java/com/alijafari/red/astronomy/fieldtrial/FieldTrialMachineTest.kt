package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.FieldTrialMachine
import com.alijafari.red.astronomy.fieldtrial.engine.FieldTrialMachine.evaluate
import com.alijafari.red.astronomy.fieldtrial.engine.FieldTrialMachine.gating
import com.alijafari.red.astronomy.fieldtrial.engine.Json
import com.alijafari.red.astronomy.fieldtrial.engine.LevelOutcome
import com.alijafari.red.astronomy.fieldtrial.engine.LevelStatus
import com.alijafari.red.astronomy.fieldtrial.engine.SkipReason
import com.alijafari.red.astronomy.fieldtrial.engine.TapMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** G-1.3 + 1.7: state machine — gating, skip, redo, versioning, restore-from-JSON. */
class FieldTrialMachineTest {

    private fun tap(target: String, sep: Double, dAz: Double = 0.2, dAlt: Double = 0.1) = TapMeasurement.of(
        1757000000000L, target, 100.0, 40.0, 100.0 + dAz, 40.0 + dAlt,
        500.0, 900.0, 500.0 + 10.0, 900.0 + 5.0,
        100.0, 40.0, 0.0, floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
        35.7, 51.4, 6.0, "FALLBACK_DEFAULT", 800.0, 800.0, 540.0, 1170.0,
        "NONE", null, null, 4.9, 1.0, 0
    ).let { it.copy(separationDeg = sep) } // direct for brevity; math covered by TapMeasurementTest

    @Test
    fun `gating day night hemisphere dependency and not-in-build`() {
        val nightMs = Instant.parse("2026-09-23T02:00:00Z").toEpochMilli() // Frankfurt deep night
        assertEquals(LevelStatus.NOT_NOW, gating(1, nightMs, 50.1, 8.7, trackerWired = true, l3Done = false).status)
        assertEquals("Sun rises", gating(1, nightMs, 50.1, 8.7, true, false).whenKind)
        assertNotNull(gating(1, nightMs, 50.1, 8.7, true, false).whenUtcMs)
        val dayMs = Instant.parse("2026-06-21T14:00:00Z").toEpochMilli()  // Tehran day
        assertEquals(LevelStatus.AVAILABLE, gating(1, dayMs, 35.7, 51.4, true, false).status)
        // L7 hemispheres
        assertEquals(LevelStatus.NOT_APPLICABLE, gating(7, dayMs, 35.7, 51.4, true, false).status)
        assertEquals(LevelStatus.AVAILABLE, gating(7, nightMs, -33.9, 151.2, true, false).status)
        // L8 Part B: wired vs not
        assertEquals(LevelStatus.AVAILABLE, gating(8, nightMs, 50.1, 8.7, trackerWired = true, l3Done = false).status)
        val nb = gating(8, nightMs, 50.1, 8.7, trackerWired = false, l3Done = false)
        assertEquals(LevelStatus.NOT_IN_BUILD, nb.status)
        // L4 depends on L3
        assertEquals(LevelStatus.NOT_NOW, gating(4, nightMs, 50.1, 8.7, true, l3Done = false).status)
        assertEquals(LevelStatus.AVAILABLE, gating(4, nightMs, 50.1, 8.7, true, l3Done = true).status)
        // L3 at night must find a star (Frankfurt Sep night: some bright star up)
        assertEquals(LevelStatus.AVAILABLE, gating(3, nightMs, 50.1, 8.7, true, false).status)
    }

    @Test
    fun `complete skip redo versioning - attempts append never overwrite`() {
        var doc = FieldTrialMachine.newTrial("t1", 1000L, "Pixel 8", 35, 50.1, 8.7)
        // L0 pass
        doc = FieldTrialMachine.open(doc, 0, 1000L)
        doc = FieldTrialMachine.complete(doc, evaluate(0, doc.pending!!, doc), 2000L)
        assertEquals(LevelOutcome.PASS, doc.latest(0)!!.outcome)
        assertEquals(1, doc.attempts(0))
        // L1 fail then redo pass (versioned)
        doc = FieldTrialMachine.open(doc, 1, 3000L)
        doc = FieldTrialMachine.addMeasurement(doc, tap("SUN", 4.0, dAz = 3.5))
        assertEquals(LevelOutcome.FAIL, evaluate(1, doc.pending!!, doc))
        doc = FieldTrialMachine.complete(doc, evaluate(1, doc.pending!!, doc), 4000L)
        doc = FieldTrialMachine.open(doc, 1, 5000L) // REDO
        doc = FieldTrialMachine.addMeasurement(doc, tap("SUN", 0.5, dAz = 0.4))
        doc = FieldTrialMachine.complete(doc, evaluate(1, doc.pending!!, doc), 6000L)
        assertEquals(LevelOutcome.PASS, doc.latest(1)!!.outcome)
        assertEquals(2, doc.attempts(1))                    // both kept
        assertEquals(LevelOutcome.FAIL, doc.levels[1]!![0].outcome) // first attempt intact
        // skip with reason
        doc = FieldTrialMachine.open(doc, 2, 7000L)
        doc = FieldTrialMachine.skip(doc, SkipReason.CLOUDS, 8000L)
        assertEquals(LevelOutcome.SKIPPED, doc.latest(2)!!.outcome)
        assertEquals(SkipReason.CLOUDS, doc.latest(2)!!.skipReason)
        assertEquals(3, doc.nextLevel) // advanced past L2
    }

    @Test
    fun `restore from json is exact including pending partial data`() {
        var doc = FieldTrialMachine.newTrial("t2", 1000L, "Pixel 8", 35, 50.1, 8.7)
        doc = FieldTrialMachine.open(doc, 0, 1000L)
        doc = FieldTrialMachine.complete(doc, LevelOutcome.PASS, 2000L)
        doc = FieldTrialMachine.open(doc, 1, 3000L)
        doc = FieldTrialMachine.addMeasurement(doc, tap("SUN", 0.4))
        doc = FieldTrialMachine.addAuto(doc, "fps", Json.JNum(9.5), 3500L)
        val json = Json.write(FieldTrialMachine.toJson(doc))
        val restored = FieldTrialMachine.fromJson(Json.parse(json))
        assertEquals(doc, restored) // full equality — pending measurements + auto included
        // and it survives a second cycle
        assertEquals(json, Json.write(FieldTrialMachine.toJson(restored)))
    }

    @Test
    fun `evaluation thresholds per level`() {
        var doc = FieldTrialMachine.newTrial("t3", 0L, "d", 35, 50.1, 8.7)
        doc = FieldTrialMachine.open(doc, 3, 0L)
        doc = FieldTrialMachine.addMeasurement(doc, tap("VEGA", 0.9))
        assertEquals(LevelOutcome.PASS, evaluate(3, doc.pending!!, doc))
        doc = FieldTrialMachine.addMeasurement(doc, tap("VEGA", 1.1))
        assertEquals(LevelOutcome.FAIL, evaluate(3, doc.pending!!, doc)) // last tap counts
        // L4 vs L3 separation growth
        doc = FieldTrialMachine.complete(doc, LevelOutcome.PASS, 10L)
        doc = FieldTrialMachine.open(doc, 4, 20L)
        doc = FieldTrialMachine.addMeasurement(doc, tap("VEGA", 1.2))
        assertEquals(LevelOutcome.PASS, evaluate(4, doc.pending!!, doc)) // grew by 0.1 < 0.5
        doc = FieldTrialMachine.addMeasurement(doc, tap("VEGA", 2.0))
        assertEquals(LevelOutcome.FAIL, evaluate(4, doc.pending!!, doc)) // grew by 0.9
        // L6/L7 yes/no
        doc = FieldTrialMachine.open(doc, 6, 30L)
        assertEquals(LevelOutcome.FAIL, evaluate(6, doc.pending!!, doc)) // no answer yet = fail
        doc = FieldTrialMachine.addYesNo(doc, false, 31L)
        assertEquals(LevelOutcome.FAIL, evaluate(6, doc.pending!!, doc))
        // L8 first-lock gate
        doc = FieldTrialMachine.open(doc, 8, 40L)
        assertEquals(LevelOutcome.FAIL, evaluate(8, doc.pending!!, doc))
        doc = FieldTrialMachine.addAuto(doc, "firstFullLockMs", Json.JNum(21000.0), 41L)
        assertEquals(LevelOutcome.PASS, evaluate(8, doc.pending!!, doc))
        // L9 green vs blue
        doc = FieldTrialMachine.open(doc, 9, 50L)
        doc = FieldTrialMachine.addAuto(doc, "blueSeparationDeg", Json.JNum(2.0), 51L)
        doc = FieldTrialMachine.addAuto(doc, "greenSeparationDeg", Json.JNum(1.0), 52L)
        assertEquals(LevelOutcome.PASS, evaluate(9, doc.pending!!, doc))
        // L10/L11
        doc = FieldTrialMachine.open(doc, 10, 60L)
        doc = FieldTrialMachine.addAuto(doc, "allNoLock", Json.JBool(true), 61L)
        assertEquals(LevelOutcome.PASS, evaluate(10, doc.pending!!, doc))
        doc = FieldTrialMachine.open(doc, 11, 70L)
        doc = FieldTrialMachine.addAuto(doc, "falseLocks", Json.JNum(0.0), 71L)
        assertEquals(LevelOutcome.PASS, evaluate(11, doc.pending!!, doc))
    }
}
