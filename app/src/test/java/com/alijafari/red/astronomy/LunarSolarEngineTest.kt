package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroTime
import com.alijafari.red.astronomy.astro_engine.LunarSolarEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarSolarEngineTest {

    private val engine = LunarSolarEngine()

    @Test
    fun `test moon distance is reasonable`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val moon = engine.calculateMoon(astroTime)

        // Moon distance ranges from 356,500 to 406,700 km
        assertTrue("Moon distance should be between 356k and 407k km",
            moon.distanceKm in 350000.0..410000.0)
    }

    @Test
    fun `test moon RA and Dec are valid`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val moon = engine.calculateMoon(astroTime)

        assertTrue("Moon RA should be in [0, 360)", moon.raDeg in 0.0..360.0)
        assertTrue("Moon Dec should be in [-90, 90]", moon.decDeg in -90.0..90.0)
    }

    @Test
    fun `test moon latitude is within bounds`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val moon = engine.calculateMoon(astroTime)

        // Moon's latitude never exceeds ±5.3°
        assertTrue("Moon latitude should be within ±5.5°",
            Math.abs(moon.geocentricLatitudeDeg) < 5.5)
    }

    @Test
    fun `test sun distance is near 1 AU`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val sun = engine.calculateSun(astroTime)

        assertTrue("Sun distance should be near 1 AU",
            sun.distanceAu in 0.98..1.02)
    }

    @Test
    fun `test sun declination in August`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val sun = engine.calculateSun(astroTime)

        // In August, Sun's declination is between +10° and +20°
        assertTrue("Sun declination in August should be positive",
            sun.decDeg in 10.0..20.0)
    }

    @Test
    fun `test equation of time is reasonable`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val sun = engine.calculateSun(astroTime)

        // Equation of time ranges from -14 to +16 minutes
        assertTrue("Equation of time should be within ±16 minutes",
            Math.abs(sun.equationOfTimeMinutes) < 16.0)
    }

    @Test
    fun `test moon horizontal parallax`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val moon = engine.calculateMoon(astroTime)

        // Moon's horizontal parallax ranges from 54' to 61'
        assertTrue("Moon parallax should be between 0.9° and 1.02°",
            moon.horizontalParallaxDeg in 0.9..1.02)
    }

    @Test
    fun `test sun RA and Dec are valid`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val sun = engine.calculateSun(astroTime)

        assertTrue("Sun RA should be in [0, 360)", sun.raDeg in 0.0..360.0)
        assertTrue("Sun Dec should be in [-90, 90]", sun.decDeg in -90.0..90.0)
    }

    /**
     * Z-V1 (2026-09-04): oracle-backed accuracy assertion (category (a) remediation).
     * Pre-existing tests asserted only RA/Dec range validity, so the VSOP87 time-units
     * defect (phases at 1/10 speed; multi-degree longitude error, fixed in the final
     * pass) stayed within "valid" ranges and passed. MEASURED oracle: Meeus,
     * Astronomical Algorithms 2nd ed., Chapter 25 worked example — 1992-10-13 0h TD:
     * apparent lambda = 199.90895 deg (book), 199.90600 deg (astropy 8.0.1 of-date);
     * engine (VSOP87D truncated, mean frame) gives 199.90737. Tolerances: 0.01 deg on
     * lambda, 0.02 deg on the derived RA/Dec (book value + mean obliquity 23.439591).
     */
    @Test
    fun `test Sun apparent longitude Meeus chapter 25 worked example`() {
        val at = AstroTime.fromUtcDate(1992, 10, 13, 0, 0, 0) // TT ~ UTC + 59 s; negligible at 0.02 deg
        val sun = engine.calculateSun(at)
        assertEquals(199.90895, sun.apparentLongitudeDeg, 0.01)
        assertEquals(198.38088, sun.raDeg, 0.02)
        assertEquals(-7.78495, sun.decDeg, 0.02)
    }
}
