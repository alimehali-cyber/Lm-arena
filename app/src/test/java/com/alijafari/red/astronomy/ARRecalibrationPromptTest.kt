package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ARCalibrationManager
import com.alijafari.red.astronomy.astro_engine.ARCalibrationOffsets
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ARRecalibrationPromptTest {

    private val now = 1_800_000_000_000L

    @Test
    fun neverCalibratedDoesNotShowRecalibrationBanner() {
        val decision = ARCalibrationManager.shouldSuggestRecalibration(
            offsets = ARCalibrationOffsets(),
            currentTimeMs = now,
            currentLatitude = 35.6892,
            currentLongitude = 51.3890
        )

        assertFalse(decision.shouldPrompt)
        assertFalse(decision.isTimeStale)
        assertFalse(decision.isLocationStale)
    }

    @Test
    fun recentNearbyCalibrationDoesNotPrompt() {
        val offsets = ARCalibrationOffsets(
            yawOffsetDeg = 1.5f,
            lastCalibratedTimeMs = now - 2L * 86_400_000L,
            referenceStarName = "Sirius",
            lastLocationLat = 35.6892,
            lastLocationLon = 51.3890
        )

        val decision = ARCalibrationManager.shouldSuggestRecalibration(
            offsets = offsets,
            currentTimeMs = now,
            currentLatitude = 35.70,
            currentLongitude = 51.40
        )

        assertFalse(decision.shouldPrompt)
        assertFalse(decision.isTimeStale)
        assertFalse(decision.isLocationStale)
    }

    @Test
    fun calibrationOlderThanFourteenDaysPrompts() {
        val offsets = ARCalibrationOffsets(
            yawOffsetDeg = 1.5f,
            lastCalibratedTimeMs = now - 15L * 86_400_000L,
            referenceStarName = "Sirius",
            lastLocationLat = 35.6892,
            lastLocationLon = 51.3890
        )

        val decision = ARCalibrationManager.shouldSuggestRecalibration(
            offsets = offsets,
            currentTimeMs = now,
            currentLatitude = 35.6892,
            currentLongitude = 51.3890
        )

        assertTrue(decision.shouldPrompt)
        assertTrue(decision.isTimeStale)
        assertFalse(decision.isLocationStale)
    }

    @Test
    fun calibrationMoreThanFiftyKilometersAwayPrompts() {
        val offsets = ARCalibrationOffsets(
            yawOffsetDeg = 1.5f,
            lastCalibratedTimeMs = now - 1L * 86_400_000L,
            referenceStarName = "Sirius",
            lastLocationLat = 35.6892,
            lastLocationLon = 51.3890
        )

        val decision = ARCalibrationManager.shouldSuggestRecalibration(
            offsets = offsets,
            currentTimeMs = now,
            currentLatitude = 36.6892,
            currentLongitude = 51.3890
        )

        assertTrue(decision.shouldPrompt)
        assertFalse(decision.isTimeStale)
        assertTrue(decision.isLocationStale)
        assertTrue((decision.distanceKm ?: 0.0) > 50.0)
    }
}
