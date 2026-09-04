package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.InverseProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * G-2.1: inverse projection round-trips < 0.5 px across the frame incl. corners,
 * for the 63.5-deg fallback tier and hardware-like intrinsics, both attitude
 * branches, all netRotation cases, zoom, and the affine (sensorToView) path.
 */
class InverseProjectionTest {

    private fun rotationMatrix(azDeg: Double, altDeg: Double, rollDeg: Double): FloatArray {
        // world<-device rotation built like OrientationProvider's R_true: device axes
        // expressed in world columns. Build from the same virtual basis (r,u,f rows are
        // device axes in world coords => R columns = (r,u,f) with z flipped sign handled
        // by construction). Sufficient: an orthonormal R consistent between fwd/inv.
        val (r, u, f) = basisOf(azDeg, altDeg, rollDeg)
        return floatArrayOf(
            r[0].toFloat(), u[0].toFloat(), f[0].toFloat(),
            r[1].toFloat(), u[1].toFloat(), f[1].toFloat(),
            r[2].toFloat(), u[2].toFloat(), f[2].toFloat()
        )
    }

    private fun basisOf(azDeg: Double, altDeg: Double, rollDeg: Double): Triple<DoubleArray, DoubleArray, DoubleArray> {
        val az = Math.toRadians(azDeg); val alt = Math.toRadians(altDeg); val roll = Math.toRadians(rollDeg)
        val rx0 = cos(az); val ry0 = -sin(az); val rz0 = 0.0
        val ux0 = -sin(alt) * sin(az); val uy0 = -sin(alt) * cos(az); val uz0 = cos(alt)
        val cr = cos(roll); val sr = sin(roll)
        val r = doubleArrayOf(rx0 * cr - ux0 * sr, ry0 * cr - uy0 * sr, rz0 * cr - uz0 * sr)
        val u = doubleArrayOf(rx0 * sr + ux0 * cr, ry0 * sr + uy0 * cr, rz0 * sr + uz0 * cr)
        val f = doubleArrayOf(-cos(alt) * sin(az), -cos(alt) * cos(az), -sin(alt))
        return Triple(r, u, f)
    }

    private fun roundTripMaxError(
        intr: InverseProjection.Intrinsics,
        rot: FloatArray?,
        cAz: Double, cAlt: Double, cRoll: Double,
        cw: Double, ch: Double,
        zoom: Double = 1.0,
        displayRot: Int = 0,
        affine: DoubleArray? = null
    ): Double {
        var worst = 0.0
        for (i in 0..8) for (j in 0..8) {          // includes the 4 exact corners
            val px = cw * i / 8.0
            val py = ch * j / 8.0
            val sky = InverseProjection.inverseProject(
                px, py, rot, cAz, cAlt, cRoll, cw, ch, intr, zoom, affine, displayRot
            ) ?: continue
            val back = InverseProjection.forwardProject(
                sky.first, sky.second, rot, cAz, cAlt, cRoll, cw, ch, intr, zoom, affine, displayRot
            ) ?: continue
            val dx = back.first - px
            val dy = back.second - py
            val err = kotlin.math.sqrt(dx * dx + dy * dy)
            if (err > worst) worst = err
        }
        return worst
    }

    @Test
    fun `round trip under 0_5 px for fallback tier both branches all rotations and zoom`() {
        val intr = InverseProjection.Intrinsics.fallbackTier(1920, 1080)
        val cw = 1080.0; val ch = 2340.0
        // rotationMatrix branch
        assertTrue(roundTripMaxError(intr, rotationMatrix(140.0, 62.0, 8.0), 0.0, 0.0, 0.0, cw, ch) < 0.5)
        // virtual-camera branch
        assertTrue(roundTripMaxError(intr, null, 212.0, 55.0, 12.0, cw, ch) < 0.5)
        // display rotations 90/180/270/0 (netRotation cases)
        for (d in intArrayOf(0, 90, 180, 270)) {
            assertTrue("displayRot=$d", roundTripMaxError(intr, null, 40.0, 45.0, 0.0, cw, ch, 1.0, d) < 0.5)
        }
        // zoom 2x
        assertTrue(roundTripMaxError(intr, null, 331.0, 71.0, 4.0, cw, ch, 2.0) < 0.5)
    }

    @Test
    fun `round trip under 0_5 px for hardware-like intrinsics with skew and affine path`() {
        val hw = InverseProjection.Intrinsics(
            fx = 4000.0, fy = 4001.7, cx = 1500.5, cy = 2000.25, skew = 0.7,
            activeArrayWidth = 3000, activeArrayHeight = 4000, sensorOrientation = 90
        )
        assertTrue(roundTripMaxError(hw, rotationMatrix(95.0, 38.0, -6.0), 0.0, 0.0, 0.0, 1440.0, 3080.0) < 0.5)
        // sensorToView affine: scale sensor(3000x4000)->view(1440x3080) centered = FILL_CENTER equivalent
        val s = 1440.0 / 3000.0
        val affine = doubleArrayOf(s, 0.0, (1440.0 - 3000.0 * s) / 2.0, 0.0, 3080.0 / 4000.0, (3080.0 - 4000.0 * 3080.0 / 4000.0) / 2.0)
        assertTrue(roundTripMaxError(hw, rotationMatrix(300.0, 80.0, 3.0), 0.0, 0.0, 0.0, 1440.0, 3080.0, 1.0, 0, affine) < 0.5)
    }

    @Test
    fun `inverse then az-alt wrap is sane and behind camera stays null`() {
        val intr = InverseProjection.Intrinsics.fallbackTier(1920, 1080)
        val r = InverseProjection.inverseProject(540.0, 1170.0, null, 200.0, 60.0, 0.0, 1080.0, 2340.0, intr)
        assertNotNull(r)
        assertTrue(r!!.first in 0.0..360.0)
        assertTrue(r.second in -90.0..90.0)
        // forward of a point behind the camera is null (120 deg from the boresight)
        assertNull(
            InverseProjection.forwardProject(20.0, -60.0, null, 20.0, 60.0, 0.0, 1080.0, 2340.0, intr)
        )
        // degenerate affine rejected
        assertNull(
            InverseProjection.inverseProject(5.0, 5.0, null, 20.0, 60.0, 0.0, 1080.0, 2340.0, intr, 1.0, DoubleArray(9))
        )
    }

    @Test
    fun `mutation guard - key constants shift the result`() {
        val intr = InverseProjection.Intrinsics.fallbackTier(1920, 1080)
        val p = InverseProjection.inverseProject(540.0, 1170.0, null, 200.0, 60.0, 0.0, 1080.0, 2340.0, intr)!!
        // 1 px in cx must move the inverse result (guards against a pass-through stub)
        val shifted = intr.copy(cx = intr.cx + 10.0)
        val q = InverseProjection.inverseProject(540.0, 1170.0, null, 200.0, 60.0, 0.0, 1080.0, 2340.0, shifted)!!
        assertTrue(kotlin.math.abs(p.first - q.first) > 1e-6 || kotlin.math.abs(p.second - q.second) > 1e-9)
    }
}
