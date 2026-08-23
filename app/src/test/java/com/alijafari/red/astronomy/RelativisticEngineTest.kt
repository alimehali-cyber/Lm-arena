package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.DistanceUnit
import com.alijafari.red.astronomy.astro_engine.RelativisticEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativisticEngineTest {

    @Test
    fun testPersianDigitParsing() {
        // Test parsing standard English decimal numbers
        assertEquals(90.0, RelativisticEngine.parseLocalizedDouble("90")!!, 0.0001)
        assertEquals(0.95, RelativisticEngine.parseLocalizedDouble("0.95")!!, 0.0001)

        // Test parsing Persian digit strings
        assertEquals(90.0, RelativisticEngine.parseLocalizedDouble("۹۰")!!, 0.0001)
        assertEquals(0.95, RelativisticEngine.parseLocalizedDouble("۰.۹۵")!!, 0.0001)
        assertEquals(0.95, RelativisticEngine.parseLocalizedDouble("۰٫۹۵")!!, 0.0001) // Persian momayyez / comma decimal
    }

    @Test
    fun testPhysicsCalculationsIdenticalAcrossLocales() {
        val objects = AstronomyCatalog.getAllObjects()
        val startObj = objects.first { it.id == "planet_earth" }
        val destObj = objects.first { it.id.contains("proxima") || it.id == "star_cma_sirius" }

        val speed90PctC = 0.90 * RelativisticEngine.SPEED_OF_LIGHT_MS

        val englishJourney = RelativisticEngine.calculateJourney(
            startObject = startObj,
            destinationObject = destObj,
            speedMs = speed90PctC,
            isAccelerationOn = false,
            accelerationMs2 = 9.81,
            isLengthContractionOn = true
        )

        val persianJourney = RelativisticEngine.calculateJourney(
            startObject = startObj,
            destinationObject = destObj,
            speedMs = speed90PctC,
            isAccelerationOn = false,
            accelerationMs2 = 9.81,
            isLengthContractionOn = true
        )

        // Mathematical values must be EXACTLY identical
        assertEquals(englishJourney.earthTimeSeconds, persianJourney.earthTimeSeconds, 1e-6)
        assertEquals(englishJourney.travellerTimeSeconds, persianJourney.travellerTimeSeconds, 1e-6)
        assertEquals(englishJourney.timeDifferenceSeconds, persianJourney.timeDifferenceSeconds, 1e-6)
        assertEquals(englishJourney.lorentzFactorPeak, persianJourney.lorentzFactorPeak, 1e-6)
        assertEquals(englishJourney.contractedDistanceMeters, persianJourney.contractedDistanceMeters, 1e-6)
    }

    @Test
    fun testDistanceUnitFormatting() {
        val distMeters = 9.4607e15 // 1 Light Year

        val lyEn = RelativisticEngine.formatDistance(distMeters, DistanceUnit.LIGHT_YEARS, isFa = false)
        val lyFa = RelativisticEngine.formatDistance(distMeters, DistanceUnit.LIGHT_YEARS, isFa = true)

        assertTrue(lyEn.contains("light-years"))
        assertTrue(lyFa.contains("سال نوری"))

        val kmEn = RelativisticEngine.formatDistance(1000.0, DistanceUnit.KM, isFa = false)
        assertTrue(kmEn.contains("1 km"))
    }

    @Test
    fun testDurationFormatting() {
        val oneYearInSeconds = 365.25 * 86400

        val durationEn = RelativisticEngine.formatDuration(oneYearInSeconds, isFa = false)
        val durationFa = RelativisticEngine.formatDuration(oneYearInSeconds, isFa = true)

        assertTrue(durationEn.contains("1 yrs 0 d") || durationEn.contains("yrs"))
        assertTrue(durationFa.contains("سال"))
    }

    @Test
    fun testPlanetaryAndStellarDistanceCalculation() {
        val earth = AstronomyCatalog.getById("planet_earth")
        val mars = AstronomyCatalog.getById("planet_mars")
        val moon = AstronomyCatalog.getById("moon")

        assertNotNull(earth)
        assertNotNull(mars)
        assertNotNull(moon)

        val earthToMars = RelativisticEngine.calculateDistance(earth!!, mars!!)
        assertTrue("Distance to Mars in km should be positive and realistic", earthToMars.distanceKm > 10_000_000.0)
        assertTrue("Distance to Mars in light years should be positive", earthToMars.distanceLightYears > 0.0)

        val earthToMoon = RelativisticEngine.calculateDistance(earth, moon!!)
        assertTrue("Distance to Moon in km should be approx 384,400 km", earthToMoon.distanceKm in 300_000.0..500_000.0)
    }
}
