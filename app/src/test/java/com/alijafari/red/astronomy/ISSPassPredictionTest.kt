package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine.PassClassification
import com.alijafari.red.astronomy.astro_engine.SunEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ISSPassPredictionTest {

    private val tehranLat = 35.6892
    private val tehranLon = 51.3890

    @Test
    fun testVisiblePassesFilterOutDaylightAndDeepShadow() {
        val startMs = 1755734400000L // Approx August 2025
        val visiblePasses = ISSEngine.predictPasses(
            userLatDeg = tehranLat,
            userLonDeg = tehranLon,
            startTimestampMs = startMs,
            scanDays = 14,
            visibleOnly = true
        )

        for (pass in visiblePasses) {
            assertTrue("Every visible pass must have isVisible == true", pass.isVisible)
            assertTrue("Every visible pass must reach at least 10° elevation", pass.maxElevationDeg >= 10.0)
            assertTrue("Every visible pass must have at least 25s duration", pass.passDurationSec >= 25)

            // Verify observer sky is dark enough at peak (Sun altitude <= -6.0°)
            val sunAlt = SunEngine.getSunAltitude(pass.maxTimeMs, tehranLat, tehranLon)
            assertTrue("Sun altitude at peak of visible pass must be <= -6.0° (civil twilight ended)", sunAlt <= -6.0)

            // Verify satellite is sunlit at max time
            val posAtMax = ISSEngine.calculateTopocentricPos(pass.maxTimeMs, tehranLat, tehranLon, 940.0, ISSEngine.cachedTLE)
            assertTrue("Satellite must be illuminated by Sun at maxTime", posAtMax.isSunlit)

            // Verify valid classification for visible pass
            assertTrue(
                "Pass classification should be visible type",
                pass.classification in listOf(
                    PassClassification.OUTSTANDING,
                    PassClassification.EXCELLENT,
                    PassClassification.VERY_GOOD,
                    PassClassification.GOOD,
                    PassClassification.MARGINAL
                )
            )
        }
    }

    @Test
    fun testAllPassesIncludesDaylightAndEclipsedPassesWithCorrectFlag() {
        val startMs = 1755734400000L
        val allPasses = ISSEngine.predictPasses(
            userLatDeg = tehranLat,
            userLonDeg = tehranLon,
            startTimestampMs = startMs,
            scanDays = 7,
            visibleOnly = false
        )

        assertTrue("Should detect multiple geometric passes in 7 days", allPasses.size >= 10)

        val visiblePasses = allPasses.filter { it.isVisible }
        val nonVisiblePasses = allPasses.filter { !it.isVisible }

        assertTrue("Should contain both visible and non-visible geometric passes", visiblePasses.isNotEmpty() && nonVisiblePasses.isNotEmpty())

        for (nonVis in nonVisiblePasses) {
            assertFalse("Non-visible pass should have isVisible == false", nonVis.isVisible)
            assertTrue(
                "Non-visible pass classification should be non-visible category",
                nonVis.classification in listOf(
                    PassClassification.DAYLIGHT_ONLY,
                    PassClassification.INVISIBLE_SHADOW,
                    PassClassification.NOT_VISIBLE,
                    PassClassification.POOR
                )
            )
        }
    }

    @Test
    fun testConicalShadowModel() {
        // Satellite directly on night side behind center of Earth at 400 km altitude
        // Should be in shadow
        val jd = 2460000.0
        val gmstDeg = 0.0

        // Sub-solar point is at RA 0, Dec 0
        // Point at X = -6778.0, Y = 0, Z = 0 is directly in anti-solar direction behind Earth
        val isNightSideSunlit = ISSEngine.checkIssSunlit(
            jd = jd,
            gmstDeg = gmstDeg,
            xEcef = -6778.0,
            yEcef = 0.0,
            zEcef = 0.0
        )
        assertFalse("Satellite directly behind Earth must be in shadow (eclipsed)", isNightSideSunlit)

        // Point at X = +6778.0, Y = 0, Z = 0 is directly in sub-solar direction towards Sun
        val isDaySideSunlit = ISSEngine.checkIssSunlit(
            jd = jd,
            gmstDeg = gmstDeg,
            xEcef = 6778.0,
            yEcef = 0.0,
            zEcef = 0.0
        )
        assertTrue("Satellite facing Sun must be sunlit", isDaySideSunlit)
    }
}
