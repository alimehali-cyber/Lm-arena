package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ARProjectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.*

/**
 * Z-P2: proves the intrinsics projection path of ARProjectionEngine is an exact PINHOLE
 * (gnomonic) projection — radial pixel offset must equal f_view * tan(theta) — and that a
 * LINEAR plate-scale model (f * theta) is measurably wrong at 15/30/45 deg off-axis.
 * Fall back tier: getCameraIntrinsics(null) -> FALLBACK_DEFAULT (fx = fy = (1080/2)/tan(63.5/2),
 * 1920x1080 array, sensorOrientation=90). Portrait canvas 1080x2400, displayRotation=0 ->
 * FILL_CENTER scale = max(1080/1080, 2400/1920) = 1.25 -> f_view = fx * 1.25 = 1089.63 px.
 */
class ARProjectionPinholeTest {

    private fun attitude(azDeg: Double, altDeg: Double): FloatArray {
        // Same construction as SkyOrientationProjectionTest.createRotationMatrix(roll=0)
        val az = Math.toRadians(azDeg); val alt = Math.toRadians(altDeg)
        val px = cos(alt) * sin(az); val py = cos(alt) * cos(az); val pz = sin(alt)
        val rx = cos(az); val ry = -sin(az); val rz = 0.0
        val ux = -sin(alt) * sin(az); val uy = -sin(alt) * cos(az); val uz = cos(alt)
        val fx = -px; val fy = -py; val fz = -pz
        return floatArrayOf(rx.toFloat(), ux.toFloat(), fx.toFloat(),
            ry.toFloat(), uy.toFloat(), fy.toFloat(), rz.toFloat(), uz.toFloat(), fz.toFloat())
    }

    private fun radialPx(thetaDeg: Double): Double {
        // Boresight az 0 alt 0 (north, horizon); target az theta alt 0. On the horizon
        // circle the central angle between boresight and target is EXACTLY theta, along
        // the roll-free sensor horizontal axis -> radial px must be f_view * tan(theta).
        val intr = ARProjectionEngine.getCameraIntrinsics(null)   // FALLBACK_DEFAULT tier
        val boresight = attitude(0.0, 0.0)
        val p = ARProjectionEngine.projectAltAz(
            azimuthDeg = thetaDeg, altitudeDeg = 0.0,
            rotationMatrix = boresight, currentAzimuth = 0.0, currentAltitude = 0.0, currentRoll = 0.0,
            canvasWidth = 1080f, canvasHeight = 2400f, intrinsics = intr, zoomFactor = 1.0f,
            sensorToViewMatrix = null, displayRotationDegrees = 0)
        assertNotNull(p)
        return hypot((p!!.x - 1080f / 2).toDouble(), (p.y - 2400f / 2).toDouble())
    }

    @Test
    fun `pinhole exactness at 15 30 45 degrees off-axis fallback tier`() {
        val fView = ((1080.0 / 2.0) / tan(Math.toRadians(63.5) / 2.0)) * 1.25  // 1089.63 px
        for (theta in doubleArrayOf(15.0, 30.0, 45.0)) {
            val expected = fView * tan(Math.toRadians(theta))
            val measured = radialPx(theta)
            // measured == f*tan(theta) within 0.5 px (float pipeline, 1e-6 deg rounding)
            assertEquals("pinhole radial px at $theta deg off-axis", expected, measured, 0.5)
            // and a LINEAR model (f*theta) would deviate far beyond that same tolerance
            val linear = fView * Math.toRadians(theta)
            assert(abs(linear - measured) > 1.0) { "linear model unexpectedly matches at $theta deg" }
        }
    }
}
