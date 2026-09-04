package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.SunDiagnosis
import com.alijafari.red.astronomy.fieldtrial.engine.TapMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-2.2 + G-2.3: tap measurement math and Sun auto-diagnosis branches. */
class TapMeasurementTest {

    @Test
    fun `daz is signed and wraps across north`() {
        assertEquals(1.0, TapMeasurement.wrap180(361.0), 1e-12)
        assertEquals(-1.0, TapMeasurement.wrap180(-361.0), 1e-12)
        assertEquals(1.0, TapMeasurement.wrap180(1.0 - 360.0), 1e-12)
        val m = TapMeasurement.of(
            0L, "SUN", 359.5, 30.0, 0.5, 30.0,
            540.0, 800.0, 560.0, 800.0,
            10.0, 30.0, 0.0, null, null, null, null, "FALLBACK_DEFAULT",
            800.0, 800.0, 540.0, 1170.0, "NONE", null, null, 4.9, 1.0, 0
        )
        assertEquals(1.0, m.dAzDeg, 1e-9)
        assertEquals(0.0, m.dAltDeg, 1e-9)
        // exact: acos(sin^2(30) + cos^2(30) cos(1 deg)) — small-angle approx is 0.866
        val expected = kotlin.math.acos(
            kotlin.math.sin(Math.toRadians(30.0)).let { it * it } +
                kotlin.math.cos(Math.toRadians(30.0)).let { it * it } * kotlin.math.cos(Math.toRadians(1.0))
        ).let { Math.toDegrees(it) }
        assertEquals(expected, m.separationDeg, 1e-9)
        assertEquals(20.0, m.screenOffsetPx, 1e-9)
    }

    @Test
    fun `separation matches spherical law of cosines including poles and horizon`() {
        assertEquals(90.0, TapMeasurement.separationDeg(0.0, 0.0, 90.0, 0.0), 1e-9)
        assertEquals(90.0, TapMeasurement.separationDeg(0.0, 0.0, 0.0, 90.0), 1e-9)
        assertEquals(0.0, TapMeasurement.separationDeg(123.0, -45.0, 123.0, -45.0), 1e-9)
        // ~1 deg of pure altitude difference at 45 deg altitude
        assertEquals(1.0, TapMeasurement.separationDeg(77.0, 45.0, 77.0, 46.0), 1e-6)
    }

    @Test
    fun `sensor quaternion is unit and rotates boresight to the right world direction`() {
        val q = TapMeasurement.quaternionOf(200.0, 60.0, 15.0)
        val n = kotlin.math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3])
        assertEquals(1.0, n, 1e-12)
        // rotate camera +Z by q -> world boresight (az 200, alt 60)
        val (w, x, y, z) = listOf(q[0], q[1], q[2], q[3])
        val vx = 2 * (x * z + w * y)
        val vy = 2 * (y * z - w * x)
        val vz = 1 - 2 * (x * x + y * y)
        val az = Math.toDegrees(kotlin.math.atan2(vx, vy))
        val alt = Math.toDegrees(kotlin.math.asin(vz))
        assertEquals(200.0, ((az % 360.0) + 360.0) % 360.0, 1e-9)
        assertEquals(60.0, alt, 1e-9)
    }

    @Test
    fun `diagnosis branches with D = 4_9`() {
        val D = 4.9
        assertEquals(SunDiagnosis.MISSING, SunDiagnosis.diagnose(4.9, 0.0, D))
        assertEquals(SunDiagnosis.MISSING, SunDiagnosis.diagnose(4.0, 0.0, D))       // within 1.5
        assertEquals(SunDiagnosis.TWICE, SunDiagnosis.diagnose(-4.9, 0.0, D))
        assertEquals(SunDiagnosis.TWICE, SunDiagnosis.diagnose(-6.0, 0.0, D))
        assertEquals(SunDiagnosis.TILT, SunDiagnosis.diagnose(0.2, 5.0, D))
        assertNull(SunDiagnosis.diagnose(0.2, 1.0, D))                                // clean
        assertNull(SunDiagnosis.diagnose(20.0, 0.0, D))                               // unexplained, no diagnosis
        // boundary: exactly 1.5 is OUTSIDE (< 1.5 per spec)
        assertNull(SunDiagnosis.diagnose(6.4, 0.0, D))
        assertNotNull(SunDiagnosis.diagnose(6.39, 0.0, D))
        // missing wins over tilt when both apply (spec order)
        assertEquals(SunDiagnosis.MISSING, SunDiagnosis.diagnose(4.9, 9.0, D))
    }
}
