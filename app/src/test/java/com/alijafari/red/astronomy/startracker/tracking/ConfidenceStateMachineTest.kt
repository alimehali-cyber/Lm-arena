package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.SolveResult
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.exp

class ConfidenceStateMachineTest {

    @Test
    fun testTransitions() {
        val clock = FakeClock(0.0)
        val machine = ConfidenceStateMachine(clock)

        assertEquals(LockConfidence.NO_LOCK, machine.currentState)

        // Solve success 4+ inliers -> FULL_LOCK
        val fullLockResult = SolveResult(true, Quaternion.identity(), 5, 0.9)
        machine.onSolveResult(fullLockResult)
        assertEquals(LockConfidence.FULL_LOCK, machine.currentState)
        assertEquals(0.9, machine.currentConfidence, 1e-6)

        // Solve success 2-3 inliers -> MARGINAL_LOCK
        val marginalResult = SolveResult(true, Quaternion.identity(), 3, 0.6)
        machine.onSolveResult(marginalResult)
        assertEquals(LockConfidence.MARGINAL_LOCK, machine.currentState)

        // Solve fail -> NO_LOCK
        val failResult = SolveResult(false, null, 0, 0.0, "fail")
        machine.onSolveResult(failResult)
        assertEquals(LockConfidence.NO_LOCK, machine.currentState)

        // Ambiguous input -> observable AMBIGUOUS (audit B3: previously unreachable)
        val ambiguousResult = SolveResult(true, Quaternion.identity(), 1, 0.05)
        machine.onSolveResult(ambiguousResult)
        assertEquals(LockConfidence.AMBIGUOUS, machine.currentState)

        // Next input resolves AMBIGUOUS -> NO_LOCK first; a good solve then re-locks
        machine.onSolveResult(fullLockResult)
        assertEquals(LockConfidence.FULL_LOCK, machine.currentState)
    }

    @Test
    fun testAmbiguousDiscards() {
        val clock = FakeClock(0.0)
        val machine = ConfidenceStateMachine(clock)

        val fullLock = SolveResult(true, Quaternion.identity(), 5, 0.9)
        machine.onSolveResult(fullLock)
        assertEquals(LockConfidence.FULL_LOCK, machine.currentState)

        // Sustained disagreement enters the observable AMBIGUOUS discard state (audit B3)
        machine.onSustainedDisagreement()
        assertEquals(LockConfidence.AMBIGUOUS, machine.currentState)
        assertEquals(0.0, machine.currentConfidence, 1e-6)

        // ...and resolves to NO_LOCK on the next input (per documented table)
        machine.onRelockTimeout()
        assertEquals(LockConfidence.NO_LOCK, machine.currentState)
        assertEquals(0.0, machine.currentConfidence, 1e-6)
    }

    @Test
    fun testConfidenceDecay() {
        val clock = FakeClock(0.0)
        val machine = ConfidenceStateMachine(clock, decayTimeConstantSeconds = 10.0)

        val fullLock = SolveResult(true, Quaternion.identity(), 5, 1.0)
        machine.onSolveResult(fullLock)
        assertEquals(1.0, machine.currentConfidence, 1e-6)

        // Lose the lock at t=0, then let 10 s (one time constant) pass in NO_LOCK
        machine.onSolveResult(SolveResult(false, null, 0, 0.0, "fail"))
        assertEquals(LockConfidence.NO_LOCK, machine.currentState)
        assertEquals(1.0, machine.currentConfidence, 1e-6) // no decay yet at loss-of-lock instant

        clock.advance(10.0)
        machine.updateWithTime(0.0) // trigger decay evaluation

        // Audit B2: assert the SPECIFIC correct value 1/e = 0.36787944117144233, not merely "< 1.0",
        // so any decay regression (e.g. reintroducing time-since-last-lock multiplication) fails loudly.
        assertEquals("confidence after exactly one decay time constant must be 1/e",
            exp(-1.0), machine.currentConfidence, 1e-12)
        assertEquals(0.36787944117144233, machine.currentConfidence, 1e-12)
    }

    @Test
    fun testDecayIsCallRateIndependent() {
        // Audit B2: decay must depend only on elapsed time, not on how many evaluations happen.
        // Two 5 s evaluations must exactly equal one 10 s evaluation (both = 1/e for tau = 10 s).
        val machineA = ConfidenceStateMachine(FakeClock(0.0), decayTimeConstantSeconds = 10.0)
        val machineB = ConfidenceStateMachine(FakeClock(0.0), decayTimeConstantSeconds = 10.0)

        for (m in listOf(machineA, machineB)) {
            m.onSolveResult(SolveResult(true, Quaternion.identity(), 5, 1.0))
            m.onSolveResult(SolveResult(false, null, 0, 0.0, "fail")) // NO_LOCK at t=0, confidence 1.0
        }

        machineA.updateWithTime(5.0)
        machineA.updateWithTime(5.0)   // two evaluations
        machineB.updateWithTime(10.0)  // one evaluation

        assertEquals(exp(-1.0), machineA.currentConfidence, 1e-12)
        assertEquals(exp(-1.0), machineB.currentConfidence, 1e-12)
        assertEquals("call rate must not affect decay", machineB.currentConfidence, machineA.currentConfidence, 1e-12)

        // A pathological 1 ms tick loop must also converge to the same value over 10 s
        val machineC = ConfidenceStateMachine(FakeClock(0.0), decayTimeConstantSeconds = 10.0)
        machineC.onSolveResult(SolveResult(true, Quaternion.identity(), 5, 1.0))
        machineC.onSolveResult(SolveResult(false, null, 0, 0.0, "fail"))
        repeat(10_000) { machineC.updateWithTime(0.001) }
        assertEquals(exp(-1.0), machineC.currentConfidence, 1e-9)
    }
}
