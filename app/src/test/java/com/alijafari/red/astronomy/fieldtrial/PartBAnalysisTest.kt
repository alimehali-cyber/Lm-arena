package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.PartBAnalysis
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G-4.1: pure Part B analysis — L10 all-covered NO_LOCK rule, L11 false-lock and
 * relock counting (spec: a false lock is > 0.5 deg off with confidence >= MARGINAL).
 */
class PartBAnalysisTest {

    private fun s(lock: LockConfidence, disc: Double? = null) = PartBAnalysis.Sample(lock, disc)

    @Test
    fun `L10 all no-lock only when every sample is NO_LOCK`() {
        assertTrue(PartBAnalysis.allNoLock(listOf(LockConfidence.NO_LOCK, LockConfidence.NO_LOCK)))
        assertFalse(PartBAnalysis.allNoLock(listOf(LockConfidence.NO_LOCK, LockConfidence.MARGINAL_LOCK)))
        assertTrue(PartBAnalysis.allNoLock(emptyList()))
    }

    @Test
    fun `L11 false locks count only above half a degree`() {
        val samples = listOf(
            s(LockConfidence.NO_LOCK),
            s(LockConfidence.FULL_LOCK, 0.2),   // locked and agrees -> fine
            s(LockConfidence.MARGINAL_LOCK, 0.6),    // lock but > 0.5 deg off -> FALSE
            s(LockConfidence.FULL_LOCK, null),  // lock, no cross-check value -> not ">0.5" -> fine
            s(LockConfidence.MARGINAL_LOCK, 0.5001)  // just over -> FALSE
        )
        assertEquals(2, PartBAnalysis.falseLocks(samples))
        assertEquals(0, PartBAnalysis.falseLocks(emptyList()))
    }

    @Test
    fun `L11 relocks count full-drop-full cycles`() {
        val f = LockConfidence.FULL_LOCK
        val n = LockConfidence.NO_LOCK
        // full -> no -> full -> no -> no -> full = 2 relocks
        assertEquals(2, PartBAnalysis.relocks(listOf(s(f), s(n), s(f), s(n), s(n), s(f))))
        // never locked -> 0; continuous lock -> 0
        assertEquals(0, PartBAnalysis.relocks(listOf(s(n), s(n))))
        assertEquals(0, PartBAnalysis.relocks(listOf(s(f), s(f), s(f))))
        // drop WITHOUT relock (ended unlocked) -> 0
        assertEquals(0, PartBAnalysis.relocks(listOf(s(f), s(n))))
    }
}
