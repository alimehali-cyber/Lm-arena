package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import kotlin.math.*

/**
 * Re-lock trigger policy with three independent conditions:
 * (a) periodic timer (re-lock every N seconds since last successful lock)
 * (b) drift-threshold (re-lock if integrated attitude has rotated more than threshold since last lock)
 * (c) sustained disagreement (if lightweight local re-lock repeatedly disagrees beyond tolerance across N attempts, trigger full blind re-solve)
 */

class RelockPolicy(
    val fakeClock: FakeClock = FakeClock(),
    val periodicIntervalSeconds: Double = 5.0, // re-lock every N seconds
    val driftThresholdRad: Double = 1.0 * PI / 180.0, // 1° drift threshold, engineering estimate ~0.1-1°/min, conservative default unvalidated
    val disagreementToleranceRad: Double = 2.0 * PI / 180.0, // 2° tolerance for local vs gyro disagreement
    val sustainedDisagreementCount: Int = 3 // trigger full blind after N consecutive disagreements
) {

    var lastLockTimeSeconds: Double = fakeClock.now()
        private set

    var lastLockAttitude: Quaternion? = null
        private set

    var consecutiveDisagreements: Int = 0
        private set

    var cumulativeRotationRad: Double = 0.0
        private set

    fun onSuccessfulLock(attitude: Quaternion) {
        lastLockTimeSeconds = fakeClock.now()
        lastLockAttitude = attitude
        consecutiveDisagreements = 0
        cumulativeRotationRad = 0.0
    }

    fun onGyroIntegration(deltaRotationRad: Double) {
        cumulativeRotationRad += abs(deltaRotationRad)
    }

    /**
     * Check if periodic timer trigger fires
     */
    fun shouldTriggerPeriodic(): Boolean {
        val elapsed = fakeClock.now() - lastLockTimeSeconds
        return elapsed >= periodicIntervalSeconds
    }

    /**
     * Check if drift-threshold trigger fires
     */
    fun shouldTriggerDrift(): Boolean {
        return cumulativeRotationRad >= driftThresholdRad
    }

    /**
     * Check sustained disagreement: called when local re-lock disagrees with gyro-predicted beyond tolerance
     * @param disagreementRad angular disagreement between local re-lock and gyro-predicted
     * @return true if should trigger full blind re-solve
     */
    fun checkDisagreement(disagreementRad: Double): Boolean {
        if (disagreementRad > disagreementToleranceRad) {
            consecutiveDisagreements++
        } else {
            consecutiveDisagreements = 0
        }

        return consecutiveDisagreements >= sustainedDisagreementCount
    }

    /**
     * Combined policy: whichever trigger fires first wins
     */
    fun shouldTriggerRelock(): RelockTrigger? {
        if (shouldTriggerPeriodic()) return RelockTrigger.PERIODIC
        if (shouldTriggerDrift()) return RelockTrigger.DRIFT
        if (consecutiveDisagreements >= sustainedDisagreementCount) return RelockTrigger.SUSTAINED_DISAGREEMENT
        return null
    }

    fun reset() {
        lastLockTimeSeconds = fakeClock.now()
        lastLockAttitude = null
        consecutiveDisagreements = 0
        cumulativeRotationRad = 0.0
    }

    enum class RelockTrigger {
        PERIODIC,
        DRIFT,
        SUSTAINED_DISAGREEMENT
    }
}
