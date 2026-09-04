package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.astro_engine.ARProjectionEngine
import com.alijafari.red.astronomy.fieldtrial.engine.InverseProjection
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G-2.1 mutation lockstep (CI only — compiles the real engine): the pure replica
 * must match ARProjectionEngine.projectAltAz to a fraction of a pixel. If anyone
 * changes the engine's math or constants without updating the replica, this fails.
 */
class ProjectionLockstepTest {

    private fun assertLockstep(
        az: Double, alt: Double,
        rot: FloatArray?, cAz: Double, cAlt: Double, cRoll: Double,
        cw: Float, ch: Float,
        intr: InverseProjection.Intrinsics,
        source: ARProjectionEngine.IntrinsicsSource,
        zoom: Float = 1f,
        displayRot: Int = 0
    ) {
        val real = ARProjectionEngine.projectAltAz(
            az, alt, rot, cAz, cAlt, cRoll, cw, ch,
            ARProjectionEngine.CameraIntrinsics(
                fx = intr.fx, fy = intr.fy, cx = intr.cx, cy = intr.cy, skew = intr.skew,
                activeArrayWidth = intr.activeArrayWidth, activeArrayHeight = intr.activeArrayHeight,
                sensorOrientation = intr.sensorOrientation, isLensFacingBack = true, source = source
            ),
            zoom, null, displayRot
        )
        val mine = InverseProjection.forwardProject(
            az, alt, rot, cAz, cAlt, cRoll, cw.toDouble(), ch.toDouble(), intr, zoom.toDouble(), null, displayRot
        )
        if (real == null || mine == null) {
            assertTrue("null mismatch at az=$az", real == null && mine == null)
            return
        }
        val dx = real.x - mine.first
        val dy = real.y - mine.second
        assertTrue(
            "lockstep drift ${dx * dx + dy * dy} px^2 at az=$az alt=$alt",
            dx * dx + dy * dy < 1e-6
        )
    }

    @Test
    fun `replica matches the engine on the fallback tier across the sky`() {
        val intr = InverseProjection.Intrinsics.fallbackTier(1920, 1080)
        for (dAz in -60..60 step 20) {
            for (dAlt in -25..25 step 10) {
                assertLockstep(180.0 + dAz, 55.0 + dAlt, null, 180.0, 55.0, 0.0, 1080f, 2340f, intr, ARProjectionEngine.IntrinsicsSource.FALLBACK_DEFAULT)
            }
        }
    }

    @Test
    fun `replica matches the engine on hardware tier with roll zoom and rotations`() {
        val hw = InverseProjection.Intrinsics(
            fx = 4000.0, fy = 4001.7, cx = 1500.5, cy = 2000.25, skew = 0.7,
            activeArrayWidth = 3000, activeArrayHeight = 4000, sensorOrientation = 90
        )
        val az = 95.0; val alt = 38.0
        val r = floatArrayOf(0.866f, 0.0f, 0.5f, 0.0f, 1.0f, 0.0f, -0.5f, 0.0f, 0.866f)
        assertLockstep(az, alt, r, 0.0, 0.0, 0.0, 1440f, 3080f, hw, ARProjectionEngine.IntrinsicsSource.CALIBRATED_HARDWARE)
        assertLockstep(az, alt, null, 95.0, 38.0, -6.0, 1440f, 3080f, hw, ARProjectionEngine.IntrinsicsSource.CALIBRATED_HARDWARE, 2f)
        for (d in intArrayOf(90, 180, 270)) {
            assertLockstep(az, alt, null, 95.0, 38.0, 0.0, 1440f, 3080f, hw, ARProjectionEngine.IntrinsicsSource.CALIBRATED_HARDWARE, 1f, d)
        }
    }
}
