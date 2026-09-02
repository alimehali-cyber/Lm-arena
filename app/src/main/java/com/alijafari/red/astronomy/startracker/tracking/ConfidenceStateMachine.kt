package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.SolveResult

/**
 * Confidence State Machine with states FULL_LOCK, MARGINAL_LOCK, NO_LOCK, AMBIGUOUS
 * Matching Confidence Ladder design (Part 6 Phase 8)
 *
 * Transition table (documented):
 * Inputs: onSolveResult(SolveResult), onRelockTimeout(), onSustainedDisagreement()
 *
 * Current State | Input | Next State | Action
 * FULL_LOCK | Solve success 4+ inliers, high confidence | FULL_LOCK | update attitude
 * FULL_LOCK | Solve success 2-3 inliers | MARGINAL_LOCK | update, decay confidence
 * FULL_LOCK | Solve fail / timeout | NO_LOCK | start gyro dead-reckoning, decaying confidence
 * FULL_LOCK | Ambiguous/conflicting | AMBIGUOUS | discard, never guess, go to NO_LOCK
 * MARGINAL_LOCK | Solve success 4+ | FULL_LOCK | upgrade
 * MARGINAL_LOCK | Solve success 2-3 | MARGINAL_LOCK | maintain
 * MARGINAL_LOCK | Solve fail/timeout | NO_LOCK | decay
 * MARGINAL_LOCK | Ambiguous | AMBIGUOUS -> NO_LOCK | discard
 * NO_LOCK | Solve success 4+ | FULL_LOCK | re-lock
 * NO_LOCK | Solve success 2-3 | MARGINAL_LOCK | partial re-lock
 * NO_LOCK | Solve fail/timeout | NO_LOCK | continue gyro, confidence decays over time
 * NO_LOCK | Ambiguous | NO_LOCK | discard, stay in NO_LOCK
 * AMBIGUOUS | Any | NO_LOCK | always discard, go to NO_LOCK (never adopt ambiguous attitude)
 *
 * Confidence decay in NO_LOCK: confidence *= exp(-dt / decayTimeConstant) or linear decay
 */

class ConfidenceStateMachine(
    val fakeClock: FakeClock = FakeClock(),
    val decayTimeConstantSeconds: Double = 10.0, // time for confidence to decay to 1/e in NO_LOCK
    val marginalInlierThreshold: Int = 2,
    val fullLockInlierThreshold: Int = 4
) {

    var currentState: LockConfidence = LockConfidence.NO_LOCK
        private set

    var currentConfidence: Double = 0.0 // 0..1
        private set

    var lastLockTimeSeconds: Double = fakeClock.now()
        private set

    var lastAttitudeUpdateTimeSeconds: Double = fakeClock.now()
        private set

    fun onSolveResult(result: SolveResult) {
        val now = fakeClock.now()

        if (!result.success || result.attitude == null) {
            // Solve fail
            if (currentState == LockConfidence.FULL_LOCK || currentState == LockConfidence.MARGINAL_LOCK) {
                currentState = LockConfidence.NO_LOCK
                // Confidence starts decaying from previous confidence
            } else {
                // Stay in NO_LOCK, confidence decays
                decayConfidence(now)
            }
            lastAttitudeUpdateTimeSeconds = now
            return
        }

        // Check ambiguous
        if (result.confidence < 0.1 || result.inlierCount < marginalInlierThreshold) {
            // Consider ambiguous if very low confidence
            currentState = LockConfidence.AMBIGUOUS
            // AMBIGUOUS always discards, goes to NO_LOCK
            currentState = LockConfidence.NO_LOCK
            decayConfidence(now)
            lastAttitudeUpdateTimeSeconds = now
            return
        }

        // Successful solve
        lastLockTimeSeconds = now
        lastAttitudeUpdateTimeSeconds = now

        currentState = when {
            result.inlierCount >= fullLockInlierThreshold && result.confidence >= 0.7 -> LockConfidence.FULL_LOCK
            result.inlierCount >= marginalInlierThreshold -> LockConfidence.MARGINAL_LOCK
            else -> LockConfidence.NO_LOCK
        }

        currentConfidence = result.confidence.coerceIn(0.0, 1.0)
    }

    fun onRelockTimeout() {
        val now = fakeClock.now()
        if (currentState == LockConfidence.FULL_LOCK || currentState == LockConfidence.MARGINAL_LOCK) {
            currentState = LockConfidence.NO_LOCK
        }
        decayConfidence(now)
        lastAttitudeUpdateTimeSeconds = now
    }

    fun onSustainedDisagreement() {
        val now = fakeClock.now()
        // Sustained disagreement triggers AMBIGUOUS -> NO_LOCK, discard
        currentState = LockConfidence.AMBIGUOUS
        // AMBIGUOUS always results in discarding rather than adopting new attitude
        currentState = LockConfidence.NO_LOCK
        currentConfidence = 0.0
        lastAttitudeUpdateTimeSeconds = now
    }

    fun updateWithTime(dtSeconds: Double) {
        fakeClock.advance(dtSeconds)
        if (currentState == LockConfidence.NO_LOCK) {
            decayConfidence(fakeClock.now())
        }
    }

    private fun decayConfidence(now: Double) {
        val dt = now - lastLockTimeSeconds
        // Exponential decay: confidence = initial * exp(-dt / decayConstant)
        // For NO_LOCK, we decay from currentConfidence
        currentConfidence = currentConfidence * exp(-dt / decayTimeConstantSeconds)
        currentConfidence = currentConfidence.coerceIn(0.0, 1.0)
    }

    fun reset() {
        currentState = LockConfidence.NO_LOCK
        currentConfidence = 0.0
        lastLockTimeSeconds = fakeClock.now()
        lastAttitudeUpdateTimeSeconds = fakeClock.now()
    }
}
