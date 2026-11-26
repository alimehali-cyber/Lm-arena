package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.ar.core.ArSkyFrame
import com.alijafari.red.astronomy.ar.core.ArSkySceneComputer
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SGP4Propagator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class SatelliteExtrapolationTest {

    @Test
    fun `extrapolateSatelliteEnu handles null, zero dt, and clamping`() {
        val uNow = ArSkyFrame.enuFromAltAz(45.0, 90.0)
        val uPrev = ArSkyFrame.enuFromAltAz(40.0, 85.0)

        assertNull(ArSkySceneComputer.extrapolateSatelliteEnu(uNow, null, 500L))
        assertArrayEquals(uNow, ArSkySceneComputer.extrapolateSatelliteEnu(uNow, uPrev, 0L)!!, 1e-12)
        assertArrayEquals(uNow, ArSkySceneComputer.extrapolateSatelliteEnu(uNow, uPrev, -100L)!!, 1e-12)

        val at2500 = ArSkySceneComputer.extrapolateSatelliteEnu(uNow, uPrev, 2500L)!!
        val at5000 = ArSkySceneComputer.extrapolateSatelliteEnu(uNow, uPrev, 5000L)!!
        assertArrayEquals(at2500, at5000, 1e-12)

        val dot = (uNow[0] * at2500[0] + uNow[1] * at2500[1] + uNow[2] * at2500[2]).coerceIn(-1.0, 1.0)
        assertTrue(Math.toDegrees(acos(dot)) > 0.5)
    }

    @Test
    fun `iss pass extrapolation accuracy across dt values`() {
        val tle = ISSEngine.TLEData(
            name = "ISS (ZARYA)",
            line1 = "1 25544U 98067A   26317.50000000  .00005580  00000+0  10980-3 0  9992",
            line2 = "2 25544  51.6317 281.2275 0006755  11.7355 180.0110 15.48879905594899"
        )
        val lat = 32.7767
        val lon = -96.7970
        val altM = 130.0
        val baseMs = 1_794_600_000_000L
        val dts = listOf(250L, 500L, 1000L, 1500L)

        for (step in 0..30) {
            val tMs = baseMs + step * 10_000L
            val pNow = ISSEngine.calculateTopocentricPos(tMs, lat, lon, altM, tle)
            val pPrev = ISSEngine.calculateTopocentricPos(tMs - 10_000L, lat, lon, altM, tle)
            val uNow = ArSkyFrame.enuFromAltAz(pNow.elevationDeg, pNow.azimuthDeg)
            val uPrev = ArSkyFrame.enuFromAltAz(pPrev.elevationDeg, pPrev.azimuthDeg)

            for (dtMs in dts) {
                val uExtrap = ArSkySceneComputer.extrapolateSatelliteEnu(uNow, uPrev, dtMs)!!
                val pTrue = ISSEngine.calculateTopocentricPos(tMs + dtMs, lat, lon, altM, tle)
                val uTrue = ArSkyFrame.enuFromAltAz(pTrue.elevationDeg, pTrue.azimuthDeg)
                val errDeg = Math.toDegrees(acos((uExtrap[0] * uTrue[0] + uExtrap[1] * uTrue[1] + uExtrap[2] * uTrue[2]).coerceIn(-1.0, 1.0)))
                val unextrapDeg = Math.toDegrees(acos((uNow[0] * uTrue[0] + uNow[1] * uTrue[1] + uNow[2] * uTrue[2]).coerceIn(-1.0, 1.0)))
                assertTrue("dt=$dtMs err=$errDeg", errDeg < 0.15)
                if (unextrapDeg > 0.2) {
                    assertTrue("dt=$dtMs err=$errDeg >= unextrap=$unextrapDeg", errDeg < unextrapDeg)
                }
            }
        }
    }
}
