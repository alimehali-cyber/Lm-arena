package com.alijafari.red.astronomy.startracker.calibration

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * T4(a): tier selection (HARDWARE_DISTORTION > SELF_CALIBRATED > NONE) and metadata
 * parsing, tested OFFLINE against the harness CameraCharacteristics/Build shims.
 * The reader is UNEXECUTED against real camera2 HAL metadata until the device trial.
 */
class HardwareDistortionReaderTest {

    private fun withSdk(sdk: Int, block: () -> Unit) {
        val old = Build.VERSION.SDK_INT
        Build.VERSION.SDK_INT = sdk
        try {
            block()
        } finally {
            Build.VERSION.SDK_INT = old
        }
    }

    private fun chars(vararg pairs: Pair<CameraCharacteristics.Key<FloatArray>, FloatArray?>): CameraCharacteristics {
        val map = HashMap<CameraCharacteristics.Key<*>, Any?>()
        for ((k, v) in pairs) map[k] = v
        return CameraCharacteristics(map)
    }

    @Test
    fun `tier order is hardware above self-calibrated above none`() {
        val hw = DistortionModel(k1 = -0.02)
        val sc = DistortionModel(k1 = -0.05)
        // hardware present -> hardware wins even when self-calibration exists
        val a = HardwareDistortionReader.selectDistortionModel(hw, sc)
        assertEquals(DistortionModelSource.HARDWARE_DISTORTION, a.second)
        assertEquals(-0.02, a.first.k1, 0.0)
        // no hardware -> self-calibrated
        assertEquals(
            DistortionModelSource.SELF_CALIBRATED,
            HardwareDistortionReader.selectDistortionModel(null, sc).second
        )
        // self-calibrated but identity -> treated as absent -> NONE (T4b allowance path)
        assertEquals(
            DistortionModelSource.NONE,
            HardwareDistortionReader.selectDistortionModel(null, DistortionModel()).second
        )
        // nothing at all -> NONE
        assertEquals(
            DistortionModelSource.NONE,
            HardwareDistortionReader.selectDistortionModel(null, null).second
        )
    }

    @Test
    fun `lens distortion fk1 fk2 map to brown-conrady k1 k2`() {
        val c = chars(
            CameraCharacteristics.LENS_DISTORTION to floatArrayOf(-0.03f, 0.001f, 0.0f, 0.02f, -0.001f)
        )
        withSdk(31) {
            val m = HardwareDistortionReader.read(c)
            assertEquals(-0.03, m!!.k1, 1e-7)
            assertEquals(0.001, m.k2, 1e-7)
            assertEquals(0.0, m.p1, 0.0)
            assertEquals(0.0, m.p2, 0.0)
        }
    }

    @Test
    fun `api gates and degenerate metadata fall through to null`() {
        val lensOnly = chars(CameraCharacteristics.LENS_DISTORTION to floatArrayOf(-0.03f, 0.0f, 0.0f, 0.0f, 0.0f))
        // below API 30 -> null even with metadata present
        withSdk(29) {
            assertNull(HardwareDistortionReader.read(lensOnly))
        }
        // all-zero (identity) model -> null (caller takes the T4b allowance path)
        withSdk(31) {
            assertNull(
                HardwareDistortionReader.read(
                    chars(CameraCharacteristics.LENS_DISTORTION to floatArrayOf(0f, 0f, 0f, 0f, 0f))
                )
            )
            assertNull(HardwareDistortionReader.read(chars()))
        }
    }

    @Test
    fun `api 33 prefers lens radial distortion when present`() {
        val both = chars(
            CameraCharacteristics.LENS_DISTORTION to floatArrayOf(-0.03f, 0.0f, 0.0f, 0.0f, 0.0f),
            CameraCharacteristics.LENS_RADIAL_DISTORTION to floatArrayOf(-0.04f, 0.002f, 0f, 0f, 0f, 0f)
        )
        withSdk(34) {
            val m = HardwareDistortionReader.read(both)
            assertEquals(-0.04, m!!.k1, 1e-7)
            assertEquals(0.002, m.k2, 1e-7)
        }
        // below 33, radial metadata is invisible -> falls back to LENS_DISTORTION
        withSdk(31) {
            val m = HardwareDistortionReader.read(both)
            assertEquals(-0.03, m!!.k1, 1e-7)
        }
    }
}
