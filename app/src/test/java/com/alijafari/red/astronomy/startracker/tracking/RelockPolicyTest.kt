package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.PI

class RelockPolicyTest {

    @Test
    fun testPeriodicTrigger() {
        val clock = FakeClock(0.0)
        val policy = RelockPolicy(clock, periodicIntervalSeconds = 5.0)

        policy.onSuccessfulLock(Quaternion.identity())
        assertFalse(policy.shouldTriggerPeriodic())

        clock.advance(4.9)
        assertFalse("Should not trigger at 4.9s", policy.shouldTriggerPeriodic())

        clock.advance(0.1) // now 5.0
        assertTrue("Should trigger at exactly 5.0s", policy.shouldTriggerPeriodic())

        clock.advance(1.0)
        assertTrue("Should still trigger after 6s", policy.shouldTriggerPeriodic())
    }

    @Test
    fun testDriftTrigger() {
        val clock = FakeClock(0.0)
        val policy = RelockPolicy(clock, driftThresholdRad = 1.0 * PI / 180.0)

        policy.onSuccessfulLock(Quaternion.identity())
        assertFalse(policy.shouldTriggerDrift())

        policy.onGyroIntegration(0.5 * PI / 180.0)
        assertFalse("0.5° <1° threshold", policy.shouldTriggerDrift())

        policy.onGyroIntegration(0.5 * PI / 180.0) // total 1°
        assertTrue("Should trigger at exactly 1°", policy.shouldTriggerDrift())
    }

    @Test
    fun testSustainedDisagreement() {
        val clock = FakeClock(0.0)
        val policy = RelockPolicy(clock, disagreementToleranceRad = 2.0 * PI / 180.0, sustainedDisagreementCount = 3)

        assertFalse(policy.checkDisagreement(1.0 * PI / 180.0)) // 1° <2°, no disagreement
        assertEquals(0, policy.consecutiveDisagreements)

        assertFalse(policy.checkDisagreement(3.0 * PI / 180.0)) // 1st disagreement
        assertEquals(1, policy.consecutiveDisagreements)

        assertFalse(policy.checkDisagreement(3.0 * PI / 180.0)) // 2nd
        assertEquals(2, policy.consecutiveDisagreements)

        assertTrue("Should trigger after 3 consecutive", policy.checkDisagreement(3.0 * PI / 180.0))
        assertEquals(3, policy.consecutiveDisagreements)
    }

    @Test
    fun testCombinedPolicy() {
        val clock = FakeClock(0.0)
        val policy = RelockPolicy(clock, periodicIntervalSeconds = 5.0, driftThresholdRad = 10.0 * PI / 180.0)

        policy.onSuccessfulLock(Quaternion.identity())

        clock.advance(3.0)
        policy.onGyroIntegration(5.0 * PI / 180.0)

        assertEquals(null, policy.shouldTriggerRelock())

        clock.advance(2.0) // total 5s, periodic fires
        assertEquals(RelockPolicy.RelockTrigger.PERIODIC, policy.shouldTriggerRelock())

        // Reset
        policy.onSuccessfulLock(Quaternion.identity())
        clock.set(0.0)
        policy.reset()
        policy.onSuccessfulLock(Quaternion.identity())

        policy.onGyroIntegration(11.0 * PI / 180.0) // drift 11° >10°
        assertEquals(RelockPolicy.RelockTrigger.DRIFT, policy.shouldTriggerRelock())
    }
}
