package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.MagneticDeclination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B1 / OD4 unit tests for the pure magnetic-declination math.
 * Reference declination VALUES (commented below) are MEASURED with pygeomag
 * (WMM2025 + IGRF-14 cross-check) at the A2 grid, 2026-09-04 — see
 * docs/startracker/evidence/DECLINATION_TABLE_2026-09-04.txt.
 */
class MagneticDeclinationTest {

    @Test
    fun `zero declination is identity`() {
        assertEquals(137.25, MagneticDeclination.trueAzimuth(137.25, 0.0), 1e-12)
        assertEquals(0.0, MagneticDeclination.trueAzimuth(0.0, 0.0), 1e-12)
    }

    @Test
    fun `east declination adds`() {
        // Tehran: D ≈ +4.9°E (2026, WMM2025). Magnetic 350° + 4.9° = 354.9°
        assertEquals(354.9, MagneticDeclination.trueAzimuth(350.0, 4.9), 1e-9)
    }

    @Test
    fun `west declination subtracts`() {
        // Frankfurt: D ≈ +2.7°E; Cape Town: D ≈ −25.2°W (2026). 10° − 25.2° → wrap
        assertEquals(344.8, MagneticDeclination.trueAzimuth(10.0, -25.2), 1e-9)
    }

    @Test
    fun `wrap-around at 360 boundary`() {
        assertEquals(5.0, MagneticDeclination.trueAzimuth(355.0, 10.0), 1e-12)
        assertEquals(350.0, MagneticDeclination.trueAzimuth(5.0, -15.0), 1e-12)
        assertEquals(359.5, MagneticDeclination.trueAzimuth(0.5, -1.0), 1e-12)
        // declination 0.0 = guardrail passthrough: returns the input BIT-IDENTICAL
        // (360.0 in -> 360.0 out), which is the stronger no-op contract.
        assertEquals(360.0, MagneticDeclination.trueAzimuth(360.0, 0.0), 0.0)
        // with a nonzero declination the result is wrapped into [0, 360)
        assertEquals(0.0, MagneticDeclination.trueAzimuth(355.0, 5.0), 1e-12)
    }

    @Test
    fun `accepts out-of-range inputs harmlessly`() {
        // [-360, 720) inputs still land in [0, 360)
        val out = MagneticDeclination.trueAzimuth(-10.0, 5.0)
        assertTrue(out in 0.0..360.0)
        assertEquals(355.0, out, 1e-12)
        assertTrue(MagneticDeclination.trueAzimuth(725.0, 5.0) in 0.0..360.0)
    }

    @Test
    fun `legacy yaw rebase subtracts declination once`() {
        // Legacy yaw absorbed D while azimuth was magnetic-referenced: rebased = legacy - D
        assertEquals(0.0f, MagneticDeclination.rebaseLegacyYawOffset(4.9f, 4.9f), 1e-6f)
        assertEquals(-3.0f, MagneticDeclination.rebaseLegacyYawOffset(1.9f, 4.9f), 1e-6f)
        // D=0 must be a no-op
        assertEquals(12.5f, MagneticDeclination.rebaseLegacyYawOffset(12.5f, 0.0f), 1e-6f)
    }

    @Test
    fun `guardrail semantics - flag off or no location means declination zero and identity`() {
        // The screen-level guardrails (flag off / no GPS) feed declination=0.0 into the
        // same pure function; assert that yields bit-identical azimuths for sample values.
        for (az in listOf(0.0, 0.25, 90.0, 179.999, 359.75)) {
            assertEquals(az, MagneticDeclination.trueAzimuth(az, 0.0), 0.0)
        }
    }
}
