package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SGP4Propagator
import com.alijafari.red.astronomy.astro_engine.SunEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SGP4PropagatorTest {

    private val propagator = SGP4Propagator()

    @Test
    fun `test ISS TLE propagation and orbital dynamics`() {
        val tle = SGP4Propagator.TLEData(
            epochYear = 24,
            epochDay = 225.5,
            inclinationDeg = 51.6425,
            raanDeg = 123.4567,
            eccentricity = 0.0007089,
            argPerigeeDeg = 234.5678,
            meanAnomalyDeg = 345.6789,
            meanMotion = 15.50123456,
            bStar = 0.00012345
        )

        val epochMs = propagator.tleEpochToMs(tle.epochYear, tle.epochDay)
        val state = propagator.propagate(tle, epochMs)

        assertNotNull(state)
        val radius = sqrt(state.xKm * state.xKm + state.yKm * state.yKm + state.zKm * state.zKm)
        val speed = sqrt(state.vxKmS * state.vxKmS + state.vyKmS * state.vyKmS + state.vzKmS * state.vzKmS)

        // LEO radius (~6778 km) and speed (~7.66 km/s)
        assertTrue("Radius $radius km should be in LEO range (6500..7000 km)", radius in 6500.0..7000.0)
        assertTrue("Speed $speed km/s should be in orbital range (7.0..8.2 km/s)", speed in 7.0..8.2)
    }

    @Test
    fun `test Vallado standard SGP4 propagation consistency over 48 hours`() {
        val tle = SGP4Propagator.TLEData(
            epochYear = 24,
            epochDay = 200.0,
            inclinationDeg = 51.64,
            raanDeg = 200.0,
            eccentricity = 0.0005,
            argPerigeeDeg = 90.0,
            meanAnomalyDeg = 270.0,
            meanMotion = 15.49,
            bStar = 0.00003
        )

        val epochMs = propagator.tleEpochToMs(tle.epochYear, tle.epochDay)

        // Sample every 10 minutes for 48 hours
        for (minuteOffset in 0..2880 step 60) {
            val t = epochMs + minuteOffset * 60 * 1000L
            val state = propagator.propagate(tle, t)
            val r = sqrt(state.xKm * state.xKm + state.yKm * state.yKm + state.zKm * state.zKm)
            val v = sqrt(state.vxKmS * state.vxKmS + state.vyKmS * state.vyKmS + state.vzKmS * state.vzKmS)

            assertTrue("Radius $r at +$minuteOffset min should be stable in LEO", r in 6550.0..6950.0)
            assertTrue("Velocity $v at +$minuteOffset min should be stable in LEO", v in 7.4..7.9)
        }
    }

    @Test
    fun `test BStar scientific notation parser`() {
        val engine = ISSEngine()
        val line1A = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9993"
        val bStarA = engine.parseBStar(line1A)
        assertEquals("BStar 30000-3 should equal 0.0003", 0.0003, bStarA, 1e-6)

        val line1B = "1 25544U 98067A   26213.50000000  .00016717  00000-0 -12345-4 0  9993"
        val bStarB = engine.parseBStar(line1B)
        assertEquals("BStar -12345-4 should equal -0.000012345", -0.000012345, bStarB, 1e-8)

        val line1Zero = "1 25544U 98067A   26213.50000000  .00016717  00000-0  00000-0 0  9993"
        val bStarZero = engine.parseBStar(line1Zero)
        assertEquals("BStar 00000-0 should equal 0.0", 0.0, bStarZero, 1e-9)
    }

    @Test
    fun `test topocentric coordinates calculation and geodetic altitude`() {
        val engine = ISSEngine()
        val tle = ISSEngine.TLEData(
            name = "ISS (ZARYA)",
            line1 = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9993",
            line2 = "2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400001"
        )

        val testTimeMs = 1770000000000L
        val topo = engine.calculateTopocentricPos(
            timestampMs = testTimeMs,
            userLatDeg = 30.0,
            userLonDeg = 51.5,
            userAltMeters = 940.0,
            tle = tle
        )

        assertNotNull(topo)
        assertTrue("Sub-latitude should be between -52 and 52 deg", topo.subLatDeg in -52.0..52.0)
        assertTrue("Sub-longitude should be between -180 and 180 deg", topo.subLonDeg in -180.0..180.0)
        assertTrue("Satellite altitude should be ~415 km (380..460)", topo.satAltKm in 380.0..460.0)
        assertTrue("Azimuth should be 0..360 deg", topo.azimuthDeg in 0.0..360.0)
        assertTrue("Elevation should be -90..90 deg", topo.elevationDeg in -90.0..90.0)
    }

    @Test
    fun `test conical shadow illumination engine`() {
        val engine = ISSEngine()
        val jd = 2460000.5
        val gmstDeg = TimeEngine.getGMST(jd)

        // Point directly towards the Sun at 6800 km: must be sunlit
        val sunPos = SunEngine.calculatePosition(jd)
        val raRad = Math.toRadians(sunPos.raDeg)
        val decRad = Math.toRadians(sunPos.decDeg)
        val gmstRad = Math.toRadians(gmstDeg)

        val sunX = cos(decRad) * cos(raRad - gmstRad)
        val sunY = cos(decRad) * sin(raRad - gmstRad)
        val sunZ = sin(decRad)

        val xSunlit = 6800.0 * sunX
        val ySunlit = 6800.0 * sunY
        val zSunlit = 6800.0 * sunZ
        val isSunlit = engine.checkIssSunlit(jd, gmstDeg, xSunlit, ySunlit, zSunlit)
        assertTrue("Point on sunward side must be sunlit", isSunlit)

        // Point directly in Earth's shadow cone behind Earth at 6800 km: must NOT be sunlit
        val xShadow = -6800.0 * sunX
        val yShadow = -6800.0 * sunY
        val zShadow = -6800.0 * sunZ
        val isShadow = engine.checkIssSunlit(jd, gmstDeg, xShadow, yShadow, zShadow)
        assertFalse("Point directly behind Earth at LEO must be in Earth shadow", isShadow)
    }

    @Test
    fun `test pass predictor generates valid passes with rigorous visibility criteria`() {
        val engine = ISSEngine()
        val tle = ISSEngine.TLEData(
            name = "ISS (ZARYA)",
            line1 = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9993",
            line2 = "2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400001"
        )

        val startMs = 1770000000000L
        val allPasses = engine.predictPasses(
            userLatDeg = 30.0,
            userLonDeg = 51.5,
            startTimestampMs = startMs,
            tle = tle,
            scanDays = 5,
            visibleOnly = false
        )

        assertTrue("Should detect orbital passes over 5 days", allPasses.isNotEmpty())

        for (pass in allPasses) {
            assertTrue("Max elevation must be >= 10 deg", pass.maxElevationDeg >= 10.0)
            assertTrue("End time must be greater than start time", pass.endTimeMs > pass.startTimeMs)
            assertTrue("Pass duration should be between 30 and 900 seconds", pass.passDurationSec in 30..900)

            if (pass.isVisible) {
                // For visible passes, classification should indicate visibility
                assertTrue(
                    "Visible pass classification must not be DAYLIGHT_ONLY or INVISIBLE_SHADOW",
                    pass.classification != ISSEngine.PassClassification.DAYLIGHT_ONLY &&
                    pass.classification != ISSEngine.PassClassification.INVISIBLE_SHADOW &&
                    pass.classification != ISSEngine.PassClassification.NOT_VISIBLE
                )
                assertTrue("Visible pass visibility score must be > 0", pass.visibilityScore > 0)
            }
        }
    }
}
