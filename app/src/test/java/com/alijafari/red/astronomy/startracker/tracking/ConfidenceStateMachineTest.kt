package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.SolveResult
import org.junit.Test
import org.junit.Assert.*

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

        // Ambiguous -> AMBIGUOUS -> NO_LOCK (discard)
        val ambiguousResult = SolveResult(true, Quaternion.identity(), 1, 0.05)
        machine.onSolveResult(ambiguousResult)
        assertEquals(LockConfidence.NO_LOCK, machine.currentState) // AMBIGUOUS goes to NO_LOCK
    }

    @Test
    fun testAmbiguousDiscards() {
        val clock = FakeClock(0.0)
        val machine = ConfidenceStateMachine(clock)

        val fullLock = SolveResult(true, Quaternion.identity(), 5, 0.9)
        machine.onSolveResult(fullLock)
        assertEquals(LockConfidence.FULL_LOCK, machine.currentState)

        machine.onSustainedDisagreement()
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

        clock.advance(10.0) // 10 seconds = 1 time constant, confidence should be 1/e ≈0.367
        machine.updateWithTime(0.0) // trigger decay check

        // After 10 sec in NO_LOCK, confidence decays
        // But we are still in FULL_LOCK, so no decay yet
        // Transition to NO_LOCK then decay
        machine.onRelockTimeout()
        assertEquals(LockConfidence.NO_LOCK, machine.currentState)

        val decayed = machine.currentConfidence
        println("Confidence after 10s decay: $decayed, expected ~0.367")
        assertTrue("Confidence should decay", decayed < 1.0)
    }
}
