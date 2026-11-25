package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.ar.core.ArSkyFrame
import com.alijafari.red.astronomy.ar.core.ArSkySceneComputer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.acos

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
}
