package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.ar.core.AttitudeHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class AttitudeHistoryTest {

    private fun rotZQuat(angleRad: Double): DoubleArray {
        val h = angleRad / 2.0
        return doubleArrayOf(cos(h), 0.0, 0.0, sin(h))
    }

    private fun boresightAngleDeg(m1: DoubleArray, m2: DoubleArray): Double {
        // Boresight is -Z_device = (-m[2], -m[5], -m[8])
        val dot = (m1[2] * m2[2] + m1[5] * m2[5] + m1[8] * m2[8]).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(dot))
    }

    @Test
    fun `empty and boundary queries`() {
        val h = AttitudeHistory(8)
        assertNull(h.at(1000L))
        val q0 = rotZQuat(0.0)
        h.push(100L, q0[0], q0[1], q0[2], q0[3])
        // Non-monotonic ignored
        val qOther = rotZQuat(1.0)
        h.push(100L, qOther[0], qOther[1], qOther[2], qOther[3])
        h.push(50L, qOther[0], qOther[1], qOther[2], qOther[3])

        val older = h.at(0L)!!
        val newer = h.at(500L)!!
        assertEquals(1.0, older[0], 1e-9)
        assertEquals(1.0, newer[0], 1e-9)
    }

    @Test
    fun `rotating at 30 deg per sec with 20 ms samples matches 60 ms earlier within 0_05 deg`() {
        val h = AttitudeHistory(64)
        val omegaRadPerNs = Math.toRadians(30.0) / 1e9
        var t = 0L
        repeat(20) {
            t += 20_000_000L
            val q = rotZQuat(omegaRadPerNs * t)
            // Flip sign on odd samples to test q / -q handling
            val sign = if (it % 2 == 1) -1.0 else 1.0
            h.push(t, sign * q[0], sign * q[1], sign * q[2], sign * q[3])
        }
        val queryT = t - AttitudeHistory.CAMERA_PREVIEW_LATENCY_MS * 1_000_000L
        val m = h.at(queryT)
        assertNotNull(m)
        val expectedAngle = omegaRadPerNs * queryT
        assertEquals(cos(expectedAngle), m!![0], 1e-3)
        assertEquals(sin(expectedAngle), m[3], 1e-3)
    }

    @Test
    fun `meanAndDriftCheck accepts 10 Hz tremor and rejects 5 deg per sec pan`() {
        val h = AttitudeHistory(64)
        var t = 1_000_000_000L
        // 10 Hz, 0.1 deg amplitude tremor over 0.5 s
        for (i in 0 until 25) {
            t += 20_000_000L
            val angle = Math.toRadians(0.1 * sin(2.0 * Math.PI * 10.0 * (i * 0.02)))
            val q = rotZQuat(angle)
            h.push(t, q[0], q[1], q[2], q[3])
        }
        val res = h.meanAndDriftCheck(t)
        assertTrue(res is AttitudeHistory.WindowResult.Ok)

        // 5 deg/s steady pan over 0.5 s -> rejected
        val pan = AttitudeHistory(64)
        var tp = 1_000_000_000L
        for (i in 0 until 25) {
            tp += 20_000_000L
            val angle = Math.toRadians(5.0 * (i * 0.02))
            val q = rotZQuat(angle)
            pan.push(tp, q[0], q[1], q[2], q[3])
        }
        assertTrue(pan.meanAndDriftCheck(tp) is AttitudeHistory.WindowResult.RejectedDrift)
    }
}
