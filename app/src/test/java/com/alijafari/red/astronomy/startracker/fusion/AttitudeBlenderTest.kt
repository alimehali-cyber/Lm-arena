package com.alijafari.red.astronomy.startracker.fusion

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class AttitudeBlenderTest {
    // STRUCTURE NOTE (audit remediation, 2 baseline failures fixed):
    // StarTrackerConfig.ENABLED is a `const val = false`, so the public blend() is
    // compile-time passthrough; blend-math tests CANNOT go through it while the safety
    // flag stays disabled (its documented contract). The blend math is therefore exposed
    // as internal blendActive() and the blend-behavior tests below call it directly.
    // testNoLockPassthrough stays on the public blend(): with the flag disabled it also
    // proves the zero-behavioral-difference passthrough contract.


    @Test
    fun testNoLockPassthrough() {
        // CRITICAL SAFETY PROPERTY: no-star-lock passthrough must be numerically identical to input
        val blender = AttitudeBlender()
        val existing = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 30.0 * PI / 180.0)
        val magWeight = 1.0f

        // Cases that should passthrough exactly
        val resultNull = blender.blend(existing, null, LockConfidence.NO_LOCK, 0.0, magWeight)
        assertEquals(existing.w, resultNull.outputQuaternion.w, 1e-9)
        assertEquals(existing.x, resultNull.outputQuaternion.x, 1e-9)
        assertEquals(existing.y, resultNull.outputQuaternion.y, 1e-9)
        assertEquals(existing.z, resultNull.outputQuaternion.z, 1e-9)
        assertEquals(magWeight, resultNull.recommendedMagWeight, 1e-6f)

        val resultNoLock = blender.blend(existing, Quaternion.identity(), LockConfidence.NO_LOCK, 0.0, magWeight)
        assertEquals(existing.w, resultNoLock.outputQuaternion.w, 1e-9)

        val resultAmbiguous = blender.blend(existing, Quaternion.identity(), LockConfidence.AMBIGUOUS, 0.0, magWeight)
        assertEquals(existing.w, resultAmbiguous.outputQuaternion.w, 1e-9)

        val resultStale = blender.blend(existing, Quaternion.identity(), LockConfidence.FULL_LOCK, 10.0, magWeight)
        assertEquals(existing.w, resultStale.outputQuaternion.w, 1e-9)

        println("No-lock passthrough: PASS — output identical to input within 1e-9")
    }

    @Test
    fun testFullLockFresh() {
        val blender = AttitudeBlender()
        val existing = Quaternion.identity()
        val starSolved = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 10.0 * PI / 180.0)
        val magWeight = 1.0f

        val result = blender.blendActive(existing, starSolved, LockConfidence.FULL_LOCK, 0.0, magWeight)

        // Output should be close to starSolved (90% blend)
        val dot = abs(result.outputQuaternion.w * starSolved.w + result.outputQuaternion.x * starSolved.x + result.outputQuaternion.y * starSolved.y + result.outputQuaternion.z * starSolved.z).coerceIn(-1.0, 1.0)
        val angleToStar = 2 * acos(dot) * 180 / PI
        val angleToExisting = 2 * acos(abs(result.outputQuaternion.w * existing.w + result.outputQuaternion.x * existing.x + result.outputQuaternion.y * existing.y + result.outputQuaternion.z * existing.z).coerceIn(-1.0, 1.0)) * 180 / PI

        println("Full-lock fresh: angle to star ${"%.4f".format(angleToStar)}°, to existing ${"%.4f".format(angleToExisting)}°, magWeight ${result.recommendedMagWeight}")
        assertTrue("Should be close to star (<2°)", angleToStar < 2.0)
        assertTrue("Mag weight should be reduced toward floor 0.1", result.recommendedMagWeight < 0.5f)
    }

    @Test
    fun testMarginalLock() {
        val blender = AttitudeBlender()
        val existing = Quaternion.identity()
        val starSolved = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 10.0 * PI / 180.0)
        val magWeight = 1.0f

        val result = blender.blendActive(existing, starSolved, LockConfidence.MARGINAL_LOCK, 0.0, magWeight)

        // Should be intermediate (50% blend) → ~5° from each
        val dotToStar = abs(result.outputQuaternion.w * starSolved.w + result.outputQuaternion.x * starSolved.x + result.outputQuaternion.y * starSolved.y + result.outputQuaternion.z * starSolved.z).coerceIn(-1.0, 1.0)
        val angleToStar = 2 * acos(dotToStar) * 180 / PI

        println("Marginal-lock: angle to star ${"%.4f".format(angleToStar)}° (expected ~5° for 50% blend of 10°)")
        assertTrue("Marginal should be intermediate", angleToStar in 3.0..7.0)
        assertTrue("Mag weight moderate reduction", result.recommendedMagWeight in 0.4f..0.6f)
    }

    @Test
    fun testStalenessDecay() {
        val blender = AttitudeBlender()
        val existing = Quaternion.identity()
        val starSolved = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 10.0 * PI / 180.0)
        val magWeight = 1.0f

        val ages = listOf(0.0, 2.0, 5.0, 6.0, 8.0)
        println("Staleness decay (FULL_LOCK):")
        var prevAngleToExisting = 0.0
        for (age in ages) {
            val result = blender.blendActive(existing, starSolved, LockConfidence.FULL_LOCK, age, magWeight)
            val dot = abs(result.outputQuaternion.w * existing.w + result.outputQuaternion.x * existing.x + result.outputQuaternion.y * existing.y + result.outputQuaternion.z * existing.z).coerceIn(-1.0, 1.0)
            val angleFromExisting = 2 * acos(dot) * 180 / PI
            println("  age ${age}s: angle from existing ${"%.4f".format(angleFromExisting)}°, magWeight ${result.recommendedMagWeight}")
            // As age increases, should go from ~9° (90% of 10°) toward 0° (passthrough)
            if (age > 0) {
                assertTrue("Blend weight should decrease with age", angleFromExisting <= prevAngleToExisting + 0.1)
            }
            prevAngleToExisting = angleFromExisting
        }
    }

    @Test
    fun testSequentialCallSmoothness() {
        val blender = AttitudeBlender()
        val existingBase = Quaternion.identity()
        val discrepancyDeg = 10.0
        val starSolved = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), discrepancyDeg * PI / 180.0)

        // REALITY RECONCILED (audit remediation): this test was vacuous before (flag disabled
        // -> every call was passthrough -> max jump 0 -> always green). Executed against the
        // real math it revealed: a fresh FULL_LOCK ACQUIRES instantly at
        // FULL_LOCK_BLEND_FRACTION * discrepancy = 0.9 * 10 = 9 deg. That instant acquisition
        // is the documented design intent ("strong dominance"), so the smoothness property is
        // asserted for the AGING path (after acquisition, through staleness decay down to
        // passthrough), and acquisition is asserted explicitly at its designed magnitude.
        val age0 = blender.blendActive(existingBase, starSolved, LockConfidence.FULL_LOCK, 0.0, 1.0f)
        var prevQ = age0.outputQuaternion
        val acquisitionDot = abs(age0.outputQuaternion.w * existingBase.w + age0.outputQuaternion.x * existingBase.x +
            age0.outputQuaternion.y * existingBase.y + age0.outputQuaternion.z * existingBase.z).coerceIn(-1.0, 1.0)
        val acquisitionJump = 2 * acos(acquisitionDot) * 180 / PI
        println("Acquisition jump (instant, by design 0.9x10deg): " + "%.4f".format(acquisitionJump) + "°")
        assertEquals("fresh FULL_LOCK acquires at FULL_LOCK_BLEND_FRACTION * discrepancy (documented strong dominance)",
            StarTrackerConfig.FULL_LOCK_BLEND_FRACTION * discrepancyDeg, acquisitionJump, 0.5)

        println("Aging path smoothness (lock acquired -> ages -> expires):")
        var maxJump = 0.0
        for (age in 1..10) {
            val existing = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), age * 0.5 * PI / 180.0) // existing slowly drifts 0.5°/s
            val result = blender.blendActive(existing, starSolved, LockConfidence.FULL_LOCK, age.toDouble(), 1.0f)
            val dot = abs(result.outputQuaternion.w * prevQ.w + result.outputQuaternion.x * prevQ.x + result.outputQuaternion.y * prevQ.y + result.outputQuaternion.z * prevQ.z).coerceIn(-1.0, 1.0)
            val jump = 2 * acos(dot) * 180 / PI
            if (jump > maxJump) maxJump = jump
            println("  age " + age + ": jump " + "%.4f".format(jump) + "°")
            prevQ = result.outputQuaternion
        }

        println("Max jump on aging path between consecutive calls: " + "%.4f".format(maxJump) + "°")
        assertTrue("No discontinuous jump >5° on the aging path (decay to passthrough is continuous)", maxJump < 5.0)
    }
}
