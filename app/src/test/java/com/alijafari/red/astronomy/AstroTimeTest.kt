package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AstroTimeTest {

    private val tolerance = 1e-6  // 6 decimal places for JD comparisons

    @Test
    fun `test J2000 epoch`() {
        // J2000.0 = 2000-01-01 12:00:00 TT = JD 2451545.0 TT
        // In UTC, this is approximately 2000-01-01 11:58:55.816 UTC
        // JD_UTC should be approximately 2451545.0 - ΔT/86400
        // ΔT for 2000 is approximately 63.86 seconds
        // So JD_UTC ≈ 2451545.0 - 63.86/86400 ≈ 2451544.99926

        val astroTime = AstroTime.fromUtcDate(2000, 1, 1, 11, 58, 56)
        val expectedJdUtc = 2451545.0 - 63.86 / 86400.0

        assertEquals(expectedJdUtc, astroTime.jdUtc, 0.0001)
    }

    @Test
    fun `test Delta T for year 2024`() {
        // For 2024, ΔT should be approximately 69.3 seconds
        // t ≈ 2024 - 2000 = 24 at start of year 2024
        // ΔT = 63.86 + 0.3345*24 - 0.006037*24^2 + 0.001727*24^3
        // ΔT = 63.86 + 8.028 - 3.477 + 23.85 = 69.26

        val astroTime = AstroTime.fromUtcDate(2024, 1, 1, 0, 0, 0)
        // Z-V1 (2026-09-04): corrected oracle = Espenak-Meeus 2005-2050 segment
        // (t = year - 2000): dT = 62.92 + 0.32217 t + 0.005589 t^2. The old expectation
        // applied the 1986-2005 CUBIC with t = year-2000 (wrong segment AND wrong epoch
        // origin), giving 92.28 s; the pre-final-pass engine's invented cubic matched it.
        // Real IERS dT(2024) ~ 69.2 s; EM model residual <= 6 s is documented in
        // docs/startracker/A_ORACLE_CHAIN_CONCLUSIONS.md — this test pins the implemented
        // EM model (the accuracy-optimal offline model chosen in the final pass).
        val expectedDeltaT = 62.92 + 0.32217 * 24.0 + 0.005589 * 24.0 * 24.0

        assertEquals(expectedDeltaT, astroTime.deltaT, 0.5)  // 0.5 second tolerance
    }

    @Test
    fun `test Delta T for year 2026`() {
        // For 2026, t ≈ 26 at start of year 2026
        // ΔT = 63.86 + 0.3345*26 - 0.006037*26^2 + 0.001727*26^3

        val astroTime = AstroTime.fromUtcDate(2026, 1, 1, 0, 0, 0)
        // Z-V1: corrected oracle = EM 2005-2050 segment (see 2024 test note); old
        // misapplied-cubic expectation was 98.83 s. Real IERS dT(2026) ~ 69.5 s.
        val t = 26.0
        val expectedDeltaT = 62.92 + 0.32217 * t + 0.005589 * t * t

        assertEquals(expectedDeltaT, astroTime.deltaT, 0.5)
    }

    @Test
    fun `test JD to AstroTime roundtrip`() {
        val originalJd = 2460000.5
        val astroTime = AstroTime.fromJd(originalJd)
        val roundtrippedJd = astroTime.jdUtc

        assertEquals(originalJd, roundtrippedJd, tolerance)
    }

    @Test
    fun `test jdTt is greater than jdUtc`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        assertTrue("jdTt must be greater than jdUtc because ΔT > 0", astroTime.jdTt > astroTime.jdUtc)
    }

    @Test
    fun `test jcTt for J2000 epoch`() {
        // At J2000.0, jcTt should be approximately 0
        val astroTime = AstroTime.fromUtcDate(2000, 1, 1, 11, 58, 56)
        assertEquals(0.0, astroTime.jcTt, 0.0001)
    }

    @Test
    fun `test jcTt for 2026`() {
        // 2026.0 - 2000.0 = 26 years = 0.26 centuries
        val astroTime = AstroTime.fromUtcDate(2026, 1, 1, 0, 0, 0)
        assertEquals(0.26, astroTime.jcTt, 0.01)
    }

    @Test
    fun `test now() returns non-null`() {
        val now = AstroTime.now()
        assertNotNull(now)
        assertTrue("JD for current time should be > 2450000", now.jdUtc > 2450000.0)
    }

    @Test
    fun `test fromUtcDate creates correct date`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 17, 47, 0)
        assertEquals(2026, astroTime.getYear())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test Delta T throws for year 1000`() {
        val astroTime = AstroTime.fromUtcDate(1000, 1, 1, 0, 0, 0)
        // Accessing deltaT should throw
        astroTime.deltaT
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test Delta T throws for year 2200`() {
        val astroTime = AstroTime.fromUtcDate(2200, 1, 1, 0, 0, 0)
        // Accessing deltaT should throw
        astroTime.deltaT
    }

    @Test
    fun `test Delta T for year 1970`() {
        // 1970 is in the 1950-2000 range
        // t = 1970 - 2000 = -30
        // ΔT = 63.86 + 0.3345*(-30) - 0.006037*900 + 0.001727*(-27000)
        val astroTime = AstroTime.fromUtcDate(1970, 1, 1, 0, 0, 0)
        val t = -30.0
        val expectedDeltaT = 63.86 + 0.3345 * t - 0.006037 * t * t + 0.001727 * t * t * t

        assertEquals(expectedDeltaT, astroTime.deltaT, 1.0)
    }

    @Test
    fun `test toString contains expected info`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val str = astroTime.toString()
        assertTrue(str.contains("JD_UTC"))
        assertTrue(str.contains("JD_TT"))
        assertTrue(str.contains("ΔT"))
    }

    @Test
    fun `test equals and hashCode`() {
        val t1 = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val t2 = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val t3 = AstroTime.fromUtcDate(2026, 8, 13, 0, 0, 0)

        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
        assertTrue(t1 != t3)
    }
}
