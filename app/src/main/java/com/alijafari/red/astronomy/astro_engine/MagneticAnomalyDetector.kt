package com.alijafari.red.astronomy.astro_engine

import java.util.ArrayDeque
import kotlin.math.abs

/**
 * Rolling-window detector for compass disturbances that OS magnetometer accuracy alone may miss.
 *
 * It compares heading change implied by low-drift gyroscope integration with heading change derived
 * from the magnetometer. A sustained divergence while the phone is otherwise still indicates that
 * local metal/electrical interference is probably bending the magnetic vector.
 */
class MagneticAnomalyDetector(
    private val windowMs: Long = 2_500L,
    private val divergenceThresholdDeg: Double = 7.0,
    private val stableGyroThresholdDegPerSec: Double = 3.0
) {
    data class Sample(
        val timestampMs: Long,
        val gyroHeadingDeg: Double,
        val magneticHeadingDeg: Double,
        val gyroSpeedDegPerSec: Double
    )

    private val samples = ArrayDeque<Sample>()
    private var interference = false

    fun reset() {
        samples.clear()
        interference = false
    }

    fun isInterferenceDetected(): Boolean = interference

    fun addHeadingSample(
        timestampMs: Long,
        gyroHeadingDeg: Double,
        magneticHeadingDeg: Double,
        gyroSpeedDegPerSec: Double
    ): Boolean {
        if (!gyroHeadingDeg.isFinite() || !magneticHeadingDeg.isFinite() || !gyroSpeedDegPerSec.isFinite()) {
            return interference
        }

        samples.addLast(
            Sample(
                timestampMs = timestampMs,
                gyroHeadingDeg = normalize360(gyroHeadingDeg),
                magneticHeadingDeg = normalize360(magneticHeadingDeg),
                gyroSpeedDegPerSec = abs(gyroSpeedDegPerSec)
            )
        )

        val minTimestamp = timestampMs - windowMs
        while (samples.isNotEmpty() && samples.peekFirst().timestampMs < minTimestamp) {
            samples.removeFirst()
        }

        val first = samples.peekFirst()
        val last = samples.peekLast()
        if (first == null || last == null || (last.timestampMs - first.timestampMs).toDouble() < windowMs * 0.65) {
            interference = false
            return interference
        }

        val avgGyroSpeed = samples.map { it.gyroSpeedDegPerSec }.average()
        if (avgGyroSpeed > stableGyroThresholdDegPerSec) {
            interference = false
            return interference
        }

        val gyroDelta = normalize180(last.gyroHeadingDeg - first.gyroHeadingDeg)
        val magneticDelta = normalize180(last.magneticHeadingDeg - first.magneticHeadingDeg)
        val divergence = abs(normalize180(magneticDelta - gyroDelta))
        interference = divergence >= divergenceThresholdDeg
        return interference
    }

    companion object {
        fun normalize180(angleDeg: Double): Double {
            var a = (angleDeg + 180.0) % 360.0
            if (a < 0) a += 360.0
            return a - 180.0
        }

        fun normalize360(angleDeg: Double): Double {
            var a = angleDeg % 360.0
            if (a < 0) a += 360.0
            return a
        }
    }
}
