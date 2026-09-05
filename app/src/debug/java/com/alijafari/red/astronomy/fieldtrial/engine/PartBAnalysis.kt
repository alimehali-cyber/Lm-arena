package com.alijafari.red.astronomy.fieldtrial.engine

import com.alijafari.red.astronomy.startracker.tracking.LockConfidence

/**
 * G-2.5/4.1: pure Part B analysis (L10 cover-camera, L11 sweep). Kept free of Android
 * types so the offline harness and CI unit tests pin it directly; the controller
 * adapts [com.alijafari.red.astronomy.fieldtrial.StarTrackerRuntime.ProbeSample] to
 * [Sample].
 */
object PartBAnalysis {

    data class Sample(
        val lock: LockConfidence,
        /** acquisition cross-check when a lock was claimed; null when not computed */
        val acquisitionDiscrepancyDeg: Double?
    )

    /** L10: PASS requires NO_LOCK for the entire covered window. */
    fun allNoLock(locks: List<LockConfidence>): Boolean = locks.all { it == LockConfidence.NO_LOCK }

    /**
     * L11: a FALSE lock is any non-NO_LOCK sample whose acquisition cross-check
     * disagrees with the sensors by more than 0.5 deg (confidence >= MARGINAL).
     * Samples without a cross-check value are not ">0.5 deg" and do not count
     * (spec: zero false locks > 0.5 deg with confidence >= MARGINAL).
     */
    fun falseLocks(samples: List<Sample>): Int =
        samples.count { s ->
            s.lock != LockConfidence.NO_LOCK && (s.acquisitionDiscrepancyDeg ?: 0.0) > 0.5
        }

    /** FULL_LOCK -> dropped below FULL -> FULL again (expected while moving). */
    fun relocks(samples: List<Sample>): Int {
        var relocks = 0
        var wasFull = false
        var dropped = false
        for (s in samples) {
            val full = s.lock == LockConfidence.FULL_LOCK
            if (full) {
                if (wasFull && dropped) relocks++
                wasFull = true
                dropped = false
            } else if (wasFull) {
                dropped = true
            }
        }
        return relocks
    }
}
