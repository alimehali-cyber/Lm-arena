package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.Vec3
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class QuaternionIntegratorTest {

    @Test
    fun testConstantYawRotation() {
        val integrator = QuaternionIntegrator()
        val initial = Quaternion.identity()

        // Pure yaw rotation at 5°/s for 10 seconds → 50° total yaw
        val rateDegPerSec = 5.0
        val rateRadPerSec = rateDegPerSec * PI / 180.0
        val dt = 0.01 // 100Hz
        val totalTime = 10.0
        val steps = (totalTime / dt).toInt()

        var q = initial
        val angularVel = Vec3(0.0, 0.0, rateRadPerSec)

        for (i in 0 until steps) {
            q = integrator.integrate(q, angularVel, dt)
        }

        // Expected: rotation by 50° about Z
        val expected = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), 50.0 * PI / 180.0)

        val dot = abs(q.w * expected.w + q.x * expected.x + q.y * expected.y + q.z * expected.z).coerceIn(-1.0, 1.0)
        val angleErr = 2 * acos(dot) * 180 / PI

        println("Constant yaw 5°/s for 10s (50° total): error $angleErr°")
        assertTrue("Integration error should be <0.1° for 100Hz", angleErr < 0.1)
    }

    @Test
    fun testIntegrationErrorVsStepSize() {
        val integrator = QuaternionIntegrator()
        val rateDegPerSec = 45.0
        val rateRad = rateDegPerSec * PI / 180.0
        val totalTime = 5.0 // 225° total
        val expected = Quaternion.fromAxisAngle(Triple(0.0, 0.0, 1.0), totalTime * rateRad)

        val stepSizes = listOf(0.02, 0.01, 0.005) // 50Hz, 100Hz, 200Hz

        println("Integration error vs step size for 45°/s for 5s (225° total):")
        for (dt in stepSizes) {
            var q = Quaternion.identity()
            val steps = (totalTime / dt).toInt()
            val angVel = Vec3(0.0, 0.0, rateRad)
            for (i in 0 until steps) {
                q = integrator.integrate(q, angVel, dt)
            }
            val dot = abs(q.w * expected.w + q.x * expected.x + q.y * expected.y + q.z * expected.z).coerceIn(-1.0, 1.0)
            val err = 2 * acos(dot) * 180 / PI
            println("  dt=${"%.3f".format(dt)}s (${(1/dt).toInt()}Hz): error ${"%.6f".format(err)}°")
            assertTrue("Error for dt=$dt should be <0.5°", err < 0.5)
        }
    }

    @Test
    fun testRenormalization() {
        val integrator = QuaternionIntegrator()
        // Deliberately denormalized quaternion
        val denorm = Quaternion(0.9, 0.1, 0.1, 0.1) // norm not 1
        val angVel = Vec3(0.0, 0.0, 0.0)
        val result = integrator.integrate(denorm, angVel, 0.01)

        val norm = sqrt(result.w*result.w + result.x*result.x + result.y*result.y + result.z*result.z)
        println("Renormalization: input norm ${sqrt(denorm.w*denorm.w + denorm.x*denorm.x + denorm.y*denorm.y + denorm.z*denorm.z)}, output norm $norm")
        assertEquals(1.0, norm, 1e-6)
    }
}
