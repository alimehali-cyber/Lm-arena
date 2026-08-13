package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroTime
import com.alijafari.red.astronomy.astro_engine.FrameTransformationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameTransformationEngineTest {

    private val engine = FrameTransformationEngine()
    private val tolerance = 0.01  // 0.01° tolerance for most tests

    @Test
    fun `test Polaris altitude at latitude 35 degrees`() {
        // Polaris (α UMi) is at approximately RA 2h 31m 49s, Dec +89° 15' 51"
        // At latitude 35°N, Polaris altitude should be approximately 35°
        val polarisRa = 37.9542  // degrees
        val polarisDec = 89.2642  // degrees

        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val horizontal = engine.equatorialToHorizontal(
            polarisRa, polarisDec, astroTime,
            35.0, 0.0, 0.0
        )

        // Polaris should be within 1° of the latitude
        assertEquals(35.0, horizontal.altDeg, 1.0)
    }

    @Test
    fun `test GAST calculation`() {
        // GAST for a known date should be reasonable
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val gast = engine.calculateGAST(astroTime)

        assertTrue("GAST should be between 0 and 360", gast >= 0.0 && gast < 360.0)
        assertNotNull(gast)
    }

    @Test
    fun `test GMST calculation`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val gmst = engine.calculateGMST(astroTime)

        assertTrue("GMST should be between 0 and 360", gmst >= 0.0 && gmst < 360.0)
    }

    @Test
    fun `test LAST equals GAST plus longitude`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val longitude = 51.0  // Tehran

        val gast = engine.calculateGAST(astroTime)
        val last = engine.calculateLAST(astroTime, longitude)

        val expectedLast = (gast + longitude) % 360.0
        assertEquals(expectedLast, last, 0.001)
    }

    @Test
    fun `test precession of Polaris over 26 years`() {
        // Polaris moves about 0.36° over 26 years due to precession
        val polarisRa = 37.9542
        val polarisDec = 89.2642

        val astroTime2000 = AstroTime.fromUtcDate(2000, 1, 1, 0, 0, 0)
        val astroTime2026 = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)

        val precessed2000 = engine.precessJ2000ToDate(polarisRa, polarisDec, astroTime2000)
        val precessed2026 = engine.precessJ2000ToDate(polarisRa, polarisDec, astroTime2026)

        // Should be different (precession has occurred)
        val raDiff = Math.abs(precessed2000.raDeg - precessed2026.raDeg)
        val decDiff = Math.abs(precessed2000.decDeg - precessed2026.decDeg)

        assertTrue("RA should change due to precession", raDiff > 0.001 || decDiff > 0.001)
    }

    @Test
    fun `test nutation is non-zero`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        val nutation = engine.calculateNutationIAU2000B(astroTime)

        assertTrue("Nutation in longitude should be non-zero", Math.abs(nutation.deltaPsiDeg) > 0.0)
        assertTrue("Nutation in obliquity should be non-zero", Math.abs(nutation.deltaEpsilonDeg) > 0.0)
        assertTrue("True obliquity should be positive", nutation.trueObliquityDeg > 0.0)
    }

    @Test
    fun `test refraction increases altitude`() {
        val altWithoutRefraction = 10.0
        val altWithRefraction = engine.applyRefraction(altWithoutRefraction)

        assertTrue("Refraction should increase altitude", altWithRefraction > altWithoutRefraction)
    }

    @Test
    fun `test refraction is larger at low altitudes`() {
        val lowAlt = 5.0
        val highAlt = 45.0

        val refrLow = engine.applyRefraction(lowAlt) - lowAlt
        val refrHigh = engine.applyRefraction(highAlt) - highAlt

        assertTrue("Refraction should be larger at low altitudes", refrLow > refrHigh)
    }

    @Test
    fun `test horizontal to equatorial roundtrip`() {
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 12, 0, 0)
        val lat = 35.0
        val lon = 51.0

        // Start with known equatorial coordinates
        val originalRa = 100.0
        val originalDec = 30.0

        // Convert to horizontal
        val horizontal = engine.equatorialToHorizontal(
            originalRa, originalDec, astroTime, lat, lon, 0.0
        )

        // Convert back to equatorial
        val equatorial = engine.horizontalToEquatorial(
            horizontal.altDeg, horizontal.azDeg, astroTime, lat, lon
        )

        // Should be close to original (within 0.1°)
        assertEquals(originalRa, equatorial.raDeg, 0.1)
        assertEquals(originalDec, equatorial.decDeg, 0.1)
    }

    @Test
    fun `test angular separation of known stars`() {
        // Betelgeuse (RA 5h 55m, Dec +7° 24') and Rigel (RA 5h 14m, Dec -8° 12')
        val betelgeuseRa = 88.7929
        val betelgeuseDec = 7.4071
        val rigelRa = 78.6345
        val rigelDec = -8.2016

        val separation = engine.calculateAngularSeparationDeg(
            betelgeuseRa, betelgeuseDec,
            rigelRa, rigelDec
        )

        // Betelgeuse and Rigel are about 18° apart
        assertEquals(18.0, separation, 1.0)
    }

    @Test
    fun `test equatorial to galactic roundtrip`() {
        val ra = 100.0
        val dec = 30.0

        val galactic = engine.equatorialToGalactic(ra, dec)
        val equatorial = engine.galacticToEquatorial(galactic.lDeg, galactic.bDeg)

        assertEquals(ra, equatorial.raDeg, 0.1)
        assertEquals(dec, equatorial.decDeg, 0.1)
    }

    @Test
    fun `test rise set transit returns reasonable values`() {
        // For a star at the celestial equator, rise/set should be roughly 12 hours apart
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 12, 0, 0)
        val result = engine.calculateRiseSetTransit(
            0.0, 0.0, astroTime, 35.0, 0.0
        )

        assertNotNull("Transit time should not be null", result.transitTimeMs)
        assertNotNull("Rise time should not be null", result.riseTimeMs)
        assertNotNull("Set time should not be null", result.setTimeMs)
    }

    @Test
    fun `test precession of J2000 coordinates returns same for J2000 epoch`() {
        val ra = 100.0
        val dec = 30.0

        val astroTime = AstroTime.fromUtcDate(2000, 1, 1, 11, 58, 56)
        val precessed = engine.precessJ2000ToDate(ra, dec, astroTime)

        // At J2000 epoch, precession should be nearly zero
        assertEquals(ra, precessed.raDeg, 0.001)
        assertEquals(dec, precessed.decDeg, 0.001)
    }
}
