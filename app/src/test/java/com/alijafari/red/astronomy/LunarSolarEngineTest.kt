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
}
