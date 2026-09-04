package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.SolveResult
import kotlin.math.exp

/**
 * Confidence State Machine with states FULL_LOCK, MARGINAL_LOCK, NO_LOCK, AMBIGUOUS
 * Matching Confidence Ladder design (Part 6 Phase 8)
 *
 * Transition table (documented — and implemented, see audit remediation B2/B3):
 * Inputs: onSolveResult(SolveResult), onRelockTimeout(), onSustainedDisagreement(), updateWithTime(dt)
 *
 * Current State | Input | Next State | Action
 * FULL_LOCK | Solve success 4+ inliers, high confidence | FULL_LOCK | update attitude
 * FULL_LOCK | Solve success 2-3 inliers | MARGINAL_LOCK | update, decay confidence
 * FULL_LOCK | Solve fail / timeout | NO_LOCK | start gyro dead-reckoning, decaying confidence
 * FULL_LOCK | Ambiguous/conflicting | AMBIGUOUS | discard, never guess
 * MARGINAL_LOCK | Solve success 4+ | FULL_LOCK | upgrade
 * MARGINAL_LOCK | Solve success 2-3 | MARGINAL_LOCK | maintain
 * MARGINAL_LOCK | Solve fail/timeout | NO_LOCK | decay
 * MARGINAL_LOCK | Ambiguous | AMBIGUOUS | discard
 * NO_LOCK | Solve success 4+ | FULL_LOCK | re-lock
 * NO_LOCK | Solve success 2-3 | MARGINAL_LOCK | partial re-lock
 * NO_LOCK | Solve fail/timeout | NO_LOCK | continue gyro, confidence decays over time
 * NO_LOCK | Ambiguous | AMBIGUOUS | discard (stay in discard state)
 * AMBIGUOUS | Any | NO_LOCK (then that input's NO_LOCK rule applies) | always discard, never adopt ambiguous attitude
 *
 * AMBIGUOUS reachability (audit finding B3): AMBIGUOUS *is* an externally observable state. It is
 * entered on an ambiguous/discarded input and remains visible until the NEXT input, which first
 * transitions to NO_LOCK and then applies that input's rule from NO_LOCK (e.g. a subsequent good
 * solve re-locks to FULL_LOCK). Consumers (TrackingLoop, AttitudeBlender) branch on AMBIGUOUS and
 * must be able to observe it. The previous implementation set AMBIGUOUS and immediately overwrote
 * it with NO_LOCK in the same call, making it unreachable/observable-never; the documented table
 * above was the correct intent, and the code now matches it.
 *
 * Confidence decay in NO_LOCK (audit finding B2): clean exponential decay anchored to the LAST
 * DECAY EVALUATION time, not to time-since-last-lock. confidence *= exp(-dt_since_last_evaluation / tau).
 * This makes the decay independent of how often updateWithTime()/decay is called (call-rate
 * independent): evaluating twice at 5 s yields exactly the same confidence as evaluating once at 10 s.
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

    /**
     * Anchor for exponential decay: the time at which the confidence value in
     * [currentConfidence] was last valid. Decay always uses elapsed time since this anchor,
     * so multiple evaluations between locks compound correctly regardless of call rate.
     */
    private var lastDecayEvaluationSeconds: Double = fakeClock.now()

    fun onSolveResult(result: SolveResult) {
        val now = fakeClock.now()

        // Per table: any input while AMBIGUOUS first transitions to NO_LOCK,
        // then this input's NO_LOCK rule applies below.
        if (currentState == LockConfidence.AMBIGUOUS) {
            currentState = LockConfidence.NO_LOCK
        }

        if (!result.success || result.attitude == null) {
            // Solve fail
            if (currentState == LockConfidence.FULL_LOCK || currentState == LockConfidence.MARGINAL_LOCK) {
                currentState = LockConfidence.NO_LOCK
                // Confidence starts decaying from the moment the lock was lost
                lastDecayEvaluationSeconds = now
            } else {
                // Stay in NO_LOCK, confidence decays
                decayConfidence(now)
            }
            lastAttitudeUpdateTimeSeconds = now
            return
        }

        // Check ambiguous: very low confidence or below marginal inlier threshold
        if (result.confidence < 0.1 || result.inlierCount < marginalInlierThreshold) {
            // Discard the solve entirely; enter observable AMBIGUOUS state (resolves to NO_LOCK on next input)
            currentState = LockConfidence.AMBIGUOUS
            decayConfidence(now)
            lastAttitudeUpdateTimeSeconds = now
            return
        }

        // Successful solve
        lastLockTimeSeconds = now
        lastAttitudeUpdateTimeSeconds = now
        lastDecayEvaluationSeconds = now

        currentState = when {
            result.inlierCount >= fullLockInlierThreshold && result.confidence >= 0.7 -> LockConfidence.FULL_LOCK
            result.inlierCount >= marginalInlierThreshold -> LockConfidence.MARGINAL_LOCK
            else -> LockConfidence.NO_LOCK
        }

        currentConfidence = result.confidence.coerceIn(0.0, 1.0)
    }

    fun onRelockTimeout() {
        val now = fakeClock.now()
        if (currentState == LockConfidence.AMBIGUOUS) {
            currentState = LockConfidence.NO_LOCK
        }
        if (currentState == LockConfidence.FULL_LOCK || currentState == LockConfidence.MARGINAL_LOCK) {
            currentState = LockConfidence.NO_LOCK
            lastDecayEvaluationSeconds = now
        }
        decayConfidence(now)
        lastAttitudeUpdateTimeSeconds = now
    }

    fun onSustainedDisagreement() {
        val now = fakeClock.now()
        // Sustained disagreement enters AMBIGUOUS (observable discard state, per table);
        // it resolves to NO_LOCK on the next input. The disputed attitude is never adopted.
        currentState = LockConfidence.AMBIGUOUS
        currentConfidence = 0.0
        lastDecayEvaluationSeconds = now
        lastAttitudeUpdateTimeSeconds = now
    }

    fun updateWithTime(dtSeconds: Double) {
        fakeClock.advance(dtSeconds)
        if (currentState == LockConfidence.NO_LOCK) {
            decayConfidence(fakeClock.now())
        }
    }

    /**
     * Exponential decay of [currentConfidence] by the time elapsed since the last decay
     * evaluation. Rate-independent: the product of decays over a partition of an interval
     * equals the single decay over the whole interval.
     */
    private fun decayConfidence(now: Double) {
        val dt = (now - lastDecayEvaluationSeconds).coerceAtLeast(0.0)
        if (dt > 0.0 && currentConfidence > 0.0) {
            currentConfidence = (currentConfidence * exp(-dt / decayTimeConstantSeconds)).coerceIn(0.0, 1.0)
        }
        lastDecayEvaluationSeconds = now
    }

    fun reset() {
        currentState = LockConfidence.NO_LOCK
        currentConfidence = 0.0
        lastLockTimeSeconds = fakeClock.now()
        lastAttitudeUpdateTimeSeconds = fakeClock.now()
        lastDecayEvaluationSeconds = fakeClock.now()
    }
}
