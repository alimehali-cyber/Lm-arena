package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroTime
import com.alijafari.red.astronomy.astro_engine.VSOP87Engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VSOP87EngineTest {

    private val engine = VSOP87Engine()
    private val j2000AstroTime = AstroTime.fromJd(2451545.0) // J2000.0 epoch

    @Test
    fun `test Earth heliocentric coordinates at J2000 epoch`() {
        val earth = engine.calculate(VSOP87Engine.Planet.EARTH, j2000AstroTime)
        assertNotNull(earth)

        // Earth distance at J2000.0 (Jan 1, near perihelion) is ~0.983 AU
        assertEquals(0.9834, earth.distanceAu, 0.03)

        // Earth heliocentric longitude at J2000.0 is ~100.36°
        assertTrue("Earth longitude ${earth.longitudeDeg} should be between 98.0° and 103.0°",
            earth.longitudeDeg in 98.0..103.0)

        // Earth heliocentric latitude near 0.0°
        assertEquals(0.0, earth.latitudeDeg, 0.05)
    }

    @Test
    fun `test Jupiter heliocentric longitude at J2000 epoch`() {
        val jupiter = engine.calculate(VSOP87Engine.Planet.JUPITER, j2000AstroTime)
        assertNotNull(jupiter)

        // Jupiter heliocentric longitude at J2000.0 is ~36.2°
        assertTrue("Jupiter longitude ${jupiter.longitudeDeg} should be between 33.0° and 38.0°",
            jupiter.longitudeDeg in 33.0..38.0)

        // Jupiter distance ~5.20 AU
        assertEquals(5.20, jupiter.distanceAu, 0.3)
    }

    @Test
    fun `test Mercury heliocentric distance range at J2000 epoch`() {
        val mercury = engine.calculate(VSOP87Engine.Planet.MERCURY, j2000AstroTime)
        assertNotNull(mercury)

        // Mercury distance is between perihelion (~0.307 AU) and aphelion (~0.467 AU)
        assertTrue("Mercury distance ${mercury.distanceAu} should be between 0.30 and 0.47 AU",
            mercury.distanceAu in 0.30..0.47)
    }

    @Test
    fun `test Neptune heliocentric distance range at J2000 epoch`() {
        val neptune = engine.calculate(VSOP87Engine.Planet.NEPTUNE, j2000AstroTime)
        assertNotNull(neptune)

        // Neptune distance ~30.1 AU
        assertTrue("Neptune distance ${neptune.distanceAu} should be between 29.7 and 30.4 AU",
            neptune.distanceAu in 29.7..30.4)
    }

    @Test
    fun `test all planets compute without throwing exceptions`() {
        val presentAstroTime = AstroTime.fromUtcDate(2026, 8, 12, 12, 0, 0)

        for (planet in VSOP87Engine.Planet.values()) {
            val coordsJ2000 = engine.calculate(planet, j2000AstroTime)
            assertNotNull("Coordinates at J2000 for $planet should not be null", coordsJ2000)
            assertTrue("Longitude for $planet should be in [0, 360)", coordsJ2000.longitudeDeg in 0.0..360.0)
            assertTrue("Distance for $planet should be positive", coordsJ2000.distanceAu > 0.0)

            val coordsPresent = engine.calculate(planet, presentAstroTime)
            assertNotNull("Coordinates in 2026 for $planet should not be null", coordsPresent)
            assertTrue("Longitude in 2026 for $planet should be in [0, 360)", coordsPresent.longitudeDeg in 0.0..360.0)
            assertTrue("Distance in 2026 for $planet should be positive", coordsPresent.distanceAu > 0.0)
        }
    }

    /**
     * Z-V1 (2026-09-04): oracle-backed accuracy assertion (category (a) remediation).
     * The pre-existing tests only checked "computes without throwing" + range validity,
     * which is exactly why a 20-degree-class planet error (hand-mangled VSOP87 tables,
     * fixed in the final pass) survived them. MEASURED oracle: astropy 8.0.1 (offline,
     * builtin ephemeris) geocentric Sun of-date ecliptic longitude at 2026-09-04T20:00 UTC
     * = 162.30461 deg => Earth HELIOCENTRIC longitude = 342.30461 deg. Engine is geometric
     * mean-of-date (no nutation ~0.003 deg; VSOP87D truncation 0.008 deg) => 0.05 deg
     * tolerance, which still fails loudly on degree-scale table corruption.
     */
    @Test
    fun `test Earth heliocentric longitude vs astropy oracle 2026-09-04`() {
        val at = AstroTime.fromUtcDate(2026, 9, 4, 20, 0, 0)
        val earth = VSOP87Engine().calculate(VSOP87Engine.Planet.EARTH, at)
        assertEquals(342.30461, ((earth.longitudeDeg % 360.0) + 360.0) % 360.0, 0.05)
    }
}
