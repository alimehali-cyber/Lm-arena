package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.EclipseEngine
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
        assertTrue("Should find at least 2 eclipses in 2024", eclipses.size >= 2)

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
}
