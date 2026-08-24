package com.alijafari.red.astronomy.domain

import com.alijafari.red.astronomy.astro_engine.TimeEngine

enum class TimeMachineMode {
    LIVE,
    SIMULATION
}

enum class TimeSimulationSpeed(
    val multiplier: Double, // Seconds of sky simulation per 1 real second
    val labelEn: String,
    val labelFa: String
) {
    SPEED_1M(60.0, "1 min/s", "۱ دقیقه/ثانیه"),
    SPEED_10M(600.0, "10 min/s", "۱۰ دقیقه/ثانیه"),
    SPEED_1H(3600.0, "1 hr/s", "۱ ساعت/ثانیه"),
    SPEED_6H(21600.0, "6 hr/s", "۶ ساعت/ثانیه"),
    SPEED_12H(43200.0, "12 hr/s", "۱۲ ساعت/ثانیه"),
    SPEED_1D(86400.0, "1 day/s", "۱ روز/ثانیه")
}

data class TimeMachineState(
    val mode: TimeMachineMode = TimeMachineMode.LIVE,
    val simulationTimeMs: Long = System.currentTimeMillis(),
    val isPlaying: Boolean = false,
    val isReverse: Boolean = false,
    val speed: TimeSimulationSpeed = TimeSimulationSpeed.SPEED_1H,
    val isExpanded: Boolean = false,
    val eventName: String? = null,
    val isBirthdayMode: Boolean = false,
    val userBirthDateMs: Long? = null
) {
    val activeTimeMs: Long
        get() = if (mode == TimeMachineMode.LIVE) System.currentTimeMillis() else simulationTimeMs

    val activeJulianDate: Double
        get() = TimeEngine.getJulianDate(activeTimeMs)

    companion object {
        // Range 1900-01-01 to 2100-12-31 in ms
        val MIN_TIMESTAMP_MS: Long = -2208988800000L // Jan 1, 1900
        val MAX_TIMESTAMP_MS: Long = 4133894400000L  // Dec 31, 2100
    }
}
