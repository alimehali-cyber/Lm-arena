package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.EclipseEngine
import com.alijafari.red.astronomy.domain.CalendarSystem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EclipseEngineTest {

    private lateinit var engine: EclipseEngine

    @Before
    fun setUp() {
        engine = EclipseEngine()
    }

    @Test
    fun testNextSolarEclipseIsFound() {
        val afterMs = 1704067200000L // Jan 1, 2024
        val eclipse = engine.findNextSolarEclipse(afterMs)
        assertNotNull("Solar eclipse should be found", eclipse)
        assertTrue("Eclipse maximum should be after start time", eclipse!!.maximumMs > afterMs)
    }

    @Test
    fun testNextLunarEclipseIsFound() {
        val afterMs = 1704067200000L // Jan 1, 2024
        val eclipse = engine.findNextLunarEclipse(afterMs)
        assertNotNull("Lunar eclipse should be found", eclipse)
        assertTrue("Eclipse maximum should be after start time", eclipse!!.maximumMs > afterMs)
    }

    @Test
    fun testEclipseMagnitudeIsPositive() {
        val afterMs = 1704067200000L // Jan 1, 2024
        val solar = engine.findNextSolarEclipse(afterMs)!!
        val lunar = engine.findNextLunarEclipse(afterMs)!!

        assertTrue("Solar magnitude should be positive", solar.magnitude > 0.0)
        assertTrue("Lunar magnitude should be positive", lunar.magnitude > 0.0)
    }

    @Test
    fun testEclipseTypeIsValid() {
        val afterMs = 1704067200000L // Jan 1, 2024
        val solar = engine.findNextSolarEclipse(afterMs)!!
        val lunar = engine.findNextLunarEclipse(afterMs)!!

        val validSolar = setOf(
            EclipseEngine.EclipseType.PARTIAL_SOLAR,
            EclipseEngine.EclipseType.ANNULAR_SOLAR,
            EclipseEngine.EclipseType.TOTAL_SOLAR,
            EclipseEngine.EclipseType.HYBRID_SOLAR,
            EclipseEngine.EclipseType.SOLAR_PARTIAL,
            EclipseEngine.EclipseType.SOLAR_ANNULAR,
            EclipseEngine.EclipseType.SOLAR_TOTAL
        )
        val validLunar = setOf(
            EclipseEngine.EclipseType.PENUMBRAL_LUNAR,
            EclipseEngine.EclipseType.PARTIAL_LUNAR,
            EclipseEngine.EclipseType.TOTAL_LUNAR,
            EclipseEngine.EclipseType.LUNAR_PENUMBRAL,
            EclipseEngine.EclipseType.LUNAR_PARTIAL,
            EclipseEngine.EclipseType.LUNAR_TOTAL
        )

        assertTrue("Solar eclipse type should be valid solar", solar.type in validSolar)
        assertTrue("Lunar eclipse type should be valid lunar", lunar.type in validLunar)
    }

    @Test
    fun testFindEclipsesInRange() {
        val startMs = 1704067200000L // Jan 1, 2024
        val endMs = 1735689600000L   // Jan 1, 2025

        val eclipses = engine.findEclipses(startMs, endMs)
        assertEquals("Should find all 4 canonical eclipses in 2024", 4, eclipses.size)

        for (e in eclipses) {
            assertTrue("Eclipse time should be in range", e.maximumMs in startMs..endMs)
        }
    }

    @Test
    fun testNewMoonAndFullMoonTimes() {
        val afterMs = 1704067200000L // Jan 1, 2024
        val newMoon = engine.findNextNewMoon(afterMs)
        val fullMoon = engine.findNextFullMoon(afterMs)

        assertTrue("New moon should be after Jan 2024", newMoon > afterMs)
        assertTrue("Full moon should be after Jan 2024", fullMoon > afterMs)

        // New moon and full moon should be roughly half a synodic month apart
        val diffDays = Math.abs(fullMoon - newMoon) / 86400000.0
        assertTrue(
            "New and full moon should be ~14.7 days apart, got $diffDays",
            diffDays in 13.0..16.0
        )
    }

    @Test
    fun testCanonicalTimestampsMatchNasaUtcAndVisibilityInIran() {
        // 2027-08-02 Total Solar Eclipse (10:07 UTC = 1817201220000L)
        val solar2027 = engine.findNextSolarEclipse(1810000000000L)!!
        assertEquals(1817201220000L, solar2027.maximumMs)

        // In Luxor, Egypt (25.6872N, 32.6396E), 2027-08-02 is a Total Solar Eclipse (100% obscuration)
        val luxorEval = engine.evaluateEclipse(solar2027, 25.6872, 32.6396, 80.0, 1810000000000L)
        assertTrue("Luxor must see 2027-08-02 eclipse", luxorEval.isLocallyVisible)
        assertEquals(100, luxorEval.localObscurationPercent)

        // In Nurabad, Iran (30.1141N, 51.5217E), 2027-08-02 is a deep Partial Solar Eclipse (~58% linear mag, ~49% area obscuration)
        val nurabadEval = engine.evaluateEclipse(solar2027, 30.1141, 51.5217, 940.0, 1810000000000L)
        assertTrue("Nurabad must see 2027-08-02 partial eclipse", nurabadEval.isLocallyVisible)
        assertTrue("Nurabad area obscuration should be between 45% and 60%", nurabadEval.localObscurationPercent in 45..60)
        assertTrue("Nurabad linear magnitude should be between 0.50 and 0.65", nurabadEval.localMagnitude in 0.50..0.65)

        // 2028-12-31 Total Lunar Eclipse (16:52 UTC = 1861894320000L) is completely visible across Iran
        val lunar2028 = engine.findNextLunarEclipse(1860000000000L)!!
        assertEquals(1861894320000L, lunar2028.maximumMs)
        val iranLunar2028 = engine.evaluateEclipse(lunar2028, 30.1141, 51.5217, 940.0, 1860000000000L)
        assertTrue("2028-12-31 Total Lunar Eclipse must be visible in Iran", iranLunar2028.isLocallyVisible)
        assertEquals(100, iranLunar2028.localObscurationPercent)
    }

    @Test
    fun testDynamicMeeusSearchPre2024AndPost2030() {
        // Pre-2024 range (2020)
        val start2020 = 1577836800000L // 2020-01-01
        val end2020 = 1609459200000L   // 2021-01-01
        val eclipses2020 = engine.findEclipses(start2020, end2020)
        assertTrue("Should find at least 4 eclipses in 2020 via Meeus Ch. 54", eclipses2020.size >= 4)

        // Post-2030 range (2031-2033)
        val start2031 = 1924992000000L // 2031-01-01
        val end2033 = 2019686400000L   // 2034-01-01
        val eclipsesPost2030 = engine.findEclipses(start2031, end2033)
        assertTrue("Should find eclipses in 2031-2033", eclipsesPost2030.size >= 6)
        assertTrue(
            "Dynamic search must be able to classify Total eclipses",
            eclipsesPost2030.any {
                it.type == EclipseEngine.EclipseType.TOTAL_SOLAR ||
                    it.type == EclipseEngine.EclipseType.TOTAL_LUNAR
            }
        )
    }

    @Test
    fun testOngoingEclipseNotDroppedImmediatelyAfterPeak() {
        // 10 minutes after peak of 2025-09-07 Total Lunar Eclipse (18:22 UTC, while totality is still active until 18:52 UTC)
        val duringTotalityMs = 1757268720000L + 10 * 60 * 1000L
        val (_, lunarRes) = engine.getNextEclipses(
            nowMs = duringTotalityMs,
            userLatDeg = 35.6892,
            userLonDeg = 51.3890,
            elevationM = 1200.0,
            timezoneId = "Asia/Tehran",
            calendarSystem = CalendarSystem.SOLAR_HIJRI
        )
        assertEquals(
            "Ongoing lunar eclipse must remain active until P4 end contact",
            1757268720000L,
            lunarRes.event.maximumMs
        )
    }
}
