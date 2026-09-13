package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.MagneticAnomalyDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MagneticAnomalyDetectorTest {

    @Test
    fun sustainedMagneticGyroDivergenceWhileStableTriggersInterference() {
        val detector = MagneticAnomalyDetector(windowMs = 2_500L, divergenceThresholdDeg = 7.0, stableGyroThresholdDegPerSec = 3.0)

        assertFalse(detector.addHeadingSample(0L, gyroHeadingDeg = 10.0, magneticHeadingDeg = 10.0, gyroSpeedDegPerSec = 0.2))
        assertFalse(detector.addHeadingSample(1_000L, gyroHeadingDeg = 10.2, magneticHeadingDeg = 12.0, gyroSpeedDegPerSec = 0.2))
        assertTrue(detector.addHeadingSample(2_500L, gyroHeadingDeg = 10.4, magneticHeadingDeg = 22.0, gyroSpeedDegPerSec = 0.2))
    }

    @Test
    fun fastDeviceMovementSuppressesFalsePositive() {
        val detector = MagneticAnomalyDetector(windowMs = 2_500L, divergenceThresholdDeg = 7.0, stableGyroThresholdDegPerSec = 3.0)

        detector.addHeadingSample(0L, gyroHeadingDeg = 10.0, magneticHeadingDeg = 10.0, gyroSpeedDegPerSec = 24.0)
        detector.addHeadingSample(1_000L, gyroHeadingDeg = 25.0, magneticHeadingDeg = 45.0, gyroSpeedDegPerSec = 18.0)
        assertFalse(detector.addHeadingSample(2_500L, gyroHeadingDeg = 42.0, magneticHeadingDeg = 75.0, gyroSpeedDegPerSec = 16.0))
    }

    @Test
    fun matchingMagnetometerAndGyroDeltasStayClear() {
        val detector = MagneticAnomalyDetector(windowMs = 2_500L, divergenceThresholdDeg = 7.0, stableGyroThresholdDegPerSec = 3.0)

        detector.addHeadingSample(0L, gyroHeadingDeg = 350.0, magneticHeadingDeg = 350.0, gyroSpeedDegPerSec = 0.4)
        detector.addHeadingSample(1_000L, gyroHeadingDeg = 354.0, magneticHeadingDeg = 354.5, gyroSpeedDegPerSec = 0.4)
        assertFalse(detector.addHeadingSample(2_500L, gyroHeadingDeg = 2.0, magneticHeadingDeg = 2.2, gyroSpeedDegPerSec = 0.4))
    }
}
