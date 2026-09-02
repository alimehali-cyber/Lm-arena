package com.alijafari.red.astronomy.startracker.tracking

/**
 * Test-only injectable time source — DO NOT use real wall-clock time in core logic.
 * Everything driven by injectable clock/timestamp for deterministic testing.
 */
class FakeClock(
    var currentTimeSeconds: Double = 0.0
) {
    fun now(): Double = currentTimeSeconds

    fun advance(dtSeconds: Double) {
        currentTimeSeconds += dtSeconds
    }

    fun set(timeSeconds: Double) {
        currentTimeSeconds = timeSeconds
    }
}
