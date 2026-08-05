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

data class HistoricalEventPreset(
    val titleEn: String,
    val titleFa: String,
    val timestampMs: Long,
    val descriptionEn: String,
    val descriptionFa: String
)

object HistoricalEventCatalog {
    val PRESETS = listOf(
        HistoricalEventPreset(
            titleEn = "Total Solar Eclipse (Isfahan)",
            titleFa = "خورشیدگرفتگی کامل اصفهان",
            timestampMs = 1817208900000L, // August 2, 2027 12:15 UTC+3.5
            descriptionEn = "Total Solar Eclipse visible across Iran and Isfahan.",
            descriptionFa = "کسوف کامل بی‌نظیر قابل مشاهده در اصفهان و مناطق مرکزی ایران."
        ),
        HistoricalEventPreset(
            titleEn = "Great Conjunction of Saturn & Jupiter",
            titleFa = "مقارنه بزرگ مشتری و زحل",
            timestampMs = 1608566400000L, // Dec 21, 2020
            descriptionEn = "Closest alignment of Jupiter and Saturn in 800 years.",
            descriptionFa = "نزدیک‌ترین مقارنه مشتری و زحل در ۸۰۰ سال گذشته."
        ),
        HistoricalEventPreset(
            titleEn = "Halley's Comet Perihelion (1986)",
            titleFa = "عبور دنباله‌دار هالی (۱۳۶۴)",
            timestampMs = 508291200000L, // Feb 9, 1986
            descriptionEn = "Last appearance of Halley's Comet in the 20th century.",
            descriptionFa = "آخرین عبور دنباله‌دار هالی از نزدیکی زمین در قرن بیستم."
        ),
        HistoricalEventPreset(
            titleEn = "Perseid Meteor Shower Peak",
            titleFa = "اوج بارش شهابی برساوشی",
            timestampMs = 1786579200000L, // Aug 12, 2026
            descriptionEn = "Annual peak of Perseid meteors with dark moonless sky.",
            descriptionFa = "پیک سالانه بارش شهابی برساوشی در آسمان تاریک."
        ),
        HistoricalEventPreset(
            titleEn = "Total Lunar Eclipse (Blood Moon)",
            titleFa = "ماه گرفتگی کامل (ماه خونین)",
            timestampMs = 1757203200000L, // Sep 7, 2025
            descriptionEn = "Deep total lunar eclipse with reddish moon disc.",
            descriptionFa = "خسوف کامل دسامبر با قرص سرخ‌رنگ ماه."
        )
    )
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
