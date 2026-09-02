package com.alijafari.red.astronomy.startracker.fusion

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class AttitudeBlenderTest {

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

        val result = blender.blend(existing, starSolved, LockConfidence.FULL_LOCK, 0.0, magWeight)

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

        val result = blender.blend(existing, starSolved, LockConfidence.MARGINAL_LOCK, 0.0, magWeight)

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
            val result = blender.blend(existing, starSolved, LockConfidence.FULL_LOCK, age, magWeight)
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
        val starSolved = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 10.0 * PI / 180.0)

        var prevQ = existingBase
        var maxJump = 0.0
        println("Sequential call smoothness (lock arrives → ages → expires):")
        for (age in 0..10) {
            val existing = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), age * 0.5 * PI / 180.0) // existing slowly drifts 0.5°/s
            val result = blender.blend(existing, starSolved, LockConfidence.FULL_LOCK, age.toDouble(), 1.0f)
            val dot = abs(result.outputQuaternion.w * prevQ.w + result.outputQuaternion.x * prevQ.x + result.outputQuaternion.y * prevQ.y + result.outputQuaternion.z * prevQ.z).coerceIn(-1.0, 1.0)
            val jump = 2 * acos(dot) * 180 / PI
            if (jump > maxJump) maxJump = jump
            println("  age $age: jump ${"%.4f".format(jump)}°")
            prevQ = result.outputQuaternion
        }

        println("Max jump between consecutive calls: ${"%.4f".format(maxJump)}°")
        assertTrue("No discontinuous jump >5°", maxJump < 5.0)
    }
}
