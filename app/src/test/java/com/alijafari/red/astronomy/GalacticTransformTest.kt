package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.CoordinateEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Checks the Galactic <-> equatorial (J2000) transform against standard anchor points.
 *
 * Reference: the Hipparcos ICRS -> Galactic rotation matrix (ESA SP-1200). Galactic centre:
 * RA 266.40499 deg, Dec -28.93617 deg. North Galactic Pole: RA 192.85948 deg, Dec 27.12825 deg.
 * The earlier formula used the ascending-node longitude (32.93 deg) where the north celestial pole's
 * longitude (122.93 deg) is required. That error put the Galactic centre at l = 90 deg.
 */
class GalacticTransformTest {

    private fun angleDeg(raA: Double, decA: Double, raB: Double, decB: Double): Double {
        val a = unit(raA, decA)
        val b = unit(raB, decB)
        return Math.toDegrees(acos((a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0)))
    }

    private fun unit(raDeg: Double, decDeg: Double): DoubleArray {
        val r = Math.toRadians(raDeg)
        val d = Math.toRadians(decDeg)
        return doubleArrayOf(cos(d) * cos(r), cos(d) * sin(r), sin(d))
    }

    @Test
    fun galacticCentreIsAtRa266AndDecMinus29() {
        val eq = CoordinateEngine.galacticToEquatorial(CoordinateEngine.Galactic(0.0, 0.0))
        assertTrue("sky separation ${angleDeg(eq.raDeg, eq.decDeg, 266.40499, -28.93617)} deg",
            angleDeg(eq.raDeg, eq.decDeg, 266.40499, -28.93617) < 0.01)
    }

    @Test
    fun galacticCentreConvertsBackToLongitudeZeroLatitudeZero() {
        val gal = CoordinateEngine.equatorialToGalactic(CoordinateEngine.Equatorial(266.40499, -28.93617))
        val lDiff = minOf(Math.abs(gal.lDeg - 0.0), 360.0 - Math.abs(gal.lDeg - 0.0))
        assertTrue("l=${gal.lDeg}", lDiff < 0.01)
        assertEquals(0.0, gal.bDeg, 0.01)
    }

    @Test
    fun northGalacticPoleIsAtRa192AndDec27() {
        val eq = CoordinateEngine.galacticToEquatorial(CoordinateEngine.Galactic(0.0, 90.0))
        assertEquals(192.85948, eq.raDeg, 0.001)
        assertEquals(27.12825, eq.decDeg, 0.001)
    }

    @Test
    fun anticentreIsOppositeTheGalacticCentre() {
        // l = 180, b = 0 is the point opposite the Galactic centre: RA 86.405 deg, Dec +28.936 deg.
        val eq = CoordinateEngine.galacticToEquatorial(CoordinateEngine.Galactic(180.0, 0.0))
        assertTrue(angleDeg(eq.raDeg, eq.decDeg, 86.40499, 28.93617) < 0.01)
    }

    @Test
    fun galacticPlaneAtL90IsInCygnus() {
        // l = 90, b = 0 is in Cygnus: RA about 21h12m (318 deg), Dec about +48 deg.
        val eq = CoordinateEngine.galacticToEquatorial(CoordinateEngine.Galactic(90.0, 0.0))
        assertTrue("RA ${eq.raDeg}", eq.raDeg > 300.0 && eq.raDeg < 330.0)
        assertTrue("Dec ${eq.decDeg}", eq.decDeg > 30.0 && eq.decDeg < 50.0)
    }

    @Test
    fun equatorialAndGalacticRoundTripsForManyPoints() {
        var worst = 0.0
        for (ra in 0 until 360 step 17) {
            for (dec in -85..85 step 17) {
                val gal = CoordinateEngine.equatorialToGalactic(CoordinateEngine.Equatorial(ra.toDouble(), dec.toDouble()))
                val back = CoordinateEngine.galacticToEquatorial(gal)
                worst = maxOf(worst, angleDeg(ra.toDouble(), dec.toDouble(), back.raDeg, back.decDeg))
            }
        }
        assertTrue("worst round-trip error $worst deg", worst < 1e-5)
    }
}
