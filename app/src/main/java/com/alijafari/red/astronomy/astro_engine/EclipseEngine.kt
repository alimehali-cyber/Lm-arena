package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

object EclipseEngine {

    enum class EclipseType {
        SOLAR_TOTAL,
        SOLAR_ANNULAR,
        SOLAR_PARTIAL,
        LUNAR_TOTAL,
        LUNAR_PARTIAL,
        NONE
    }

    data class EclipseEventPreset(
        val id: String,
        val nameEn: String,
        val nameFa: String,
        val type: EclipseType,
        val timestampMs: Long,
        val descriptionEn: String,
        val descriptionFa: String
    )

    data class EclipseData(
        val type: EclipseType,
        val coveragePercentage: Double, // 0.0 to 100.0%
        val sunAltAz: CoordinateEngine.Horizontal,
        val moonAltAz: CoordinateEngine.Horizontal,
        val angularSeparationArcmin: Double,
        val sunRadiusArcmin: Double,
        val moonRadiusArcmin: Double,
        val c1TimeMs: Long,
        val maxEclipseTimeMs: Long,
        val c4TimeMs: Long,
        val isVisibleAboveHorizon: Boolean
    )

    // Notable eclipse presets for 2025-2028
    val PRESET_ECLIPSES = listOf(
        EclipseEventPreset(
            id = "eclipse_2026_solar_total",
            nameEn = "Total Solar Eclipse (Aug 12, 2026)",
            nameFa = "خورشیدگرفتگی کلی (۲۱ مرداد ۱۴۰۵)",
            type = EclipseType.SOLAR_TOTAL,
            timestampMs = 1786551600000L, // Aug 12 2026 ~17:40 UTC
            descriptionEn = "Spectacular total solar eclipse visible across Iceland, Spain, and North Atlantic.",
            descriptionFa = "کسوف کلی بی‌نظیر قابل رویت در اروپا، ایسلند و شمال اقیانوس اطلس."
        ),
        EclipseEventPreset(
            id = "eclipse_2027_solar_total",
            nameEn = "Great North African Total Solar Eclipse (Aug 2, 2027)",
            nameFa = "کسوف کلی بزرگ شمال آفریقا (۱۱ مرداد ۱۴۰۶)",
            type = EclipseType.SOLAR_TOTAL,
            timestampMs = 1817204400000L, // Aug 2 2027 ~10:00 UTC
            descriptionEn = "One of the longest totalities of the 21st century (6 min 23 sec in Luxor, Egypt).",
            descriptionFa = "یکی از طولانی‌ترین خورشیدگرفتگی‌های قرن ۲۱ به مدت ۶ دقیقه و ۲۳ ثانیه در اقصر مصر."
        ),
        EclipseEventPreset(
            id = "eclipse_2025_lunar_total",
            nameEn = "Total Lunar Eclipse (Sep 7, 2025)",
            nameFa = "خسوف کلی (۱۶ شهریور ۱۴۰۴)",
            type = EclipseType.LUNAR_TOTAL,
            timestampMs = 1757268000000L, // Sep 7 2025 ~18:00 UTC
            descriptionEn = "Blood Moon total eclipse visible across Asia, Europe, Africa, and Australia.",
            descriptionFa = "ماه سرخ (خسوف کلی) قابل مشاهده در سراسر ایران، آسیا، اروپا و آفریقا."
        ),
        EclipseEventPreset(
            id = "eclipse_2028_solar_annular",
            nameEn = "Annular Solar Eclipse (Jan 26, 2028)",
            nameFa = "خورشیدگرفتگی حلقوی (۶ بهمن ۱۴۰۶)",
            type = EclipseType.SOLAR_ANNULAR,
            timestampMs = 1832500000000L,
            descriptionEn = "Ring of Fire solar eclipse visible across South America, Atlantic, and Spain.",
            descriptionFa = "کسوف حلقه‌ای «حلقه آتش» قابل دید در آمریکای جنوبی و اقیانوس اطلس."
        )
    )

    /**
     * Calculates offline topocentric eclipse geometry and obscuration percentage for given location and timestamp.
     */
    fun calculateEclipseData(
        timestampMs: Long,
        userLatDeg: Double,
        userLonDeg: Double
    ): EclipseData {
        val jd = TimeEngine.getJulianDate(timestampMs)
        val sunPos = SunEngine.calculatePosition(jd)
        val sunAltAz = SunEngine.getSunAltAz(jd, userLatDeg, userLonDeg)

        val moonData = MoonEngine.calculateMoon(jd)
        val lastDeg = TimeEngine.getLAST(jd, userLonDeg)
        val moonAltAz = CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(moonData.raDeg, moonData.decDeg),
            lastDeg,
            userLatDeg
        )

        // Calculate angular separation between Sun and Moon in degrees then arcminutes
        var dAz = moonAltAz.azimuthDeg - sunAltAz.azimuthDeg
        if (dAz > 180) dAz -= 360
        if (dAz < -180) dAz += 360
        val dAlt = moonAltAz.altitudeDeg - sunAltAz.altitudeDeg
        val sepDeg = sqrt(dAz * dAz + dAlt * dAlt)
        val sepArcmin = sepDeg * 60.0

        val rSunArcmin = sunPos.apparentDiameterArcmin / 2.0
        val rMoonArcmin = moonData.angularDiameterArcmin / 2.0

        // Obscuration Calculation
        val coveragePercent = when {
            sepArcmin >= (rSunArcmin + rMoonArcmin) -> 0.0
            sepArcmin <= abs(rSunArcmin - rMoonArcmin) -> {
                if (rMoonArcmin >= rSunArcmin) 100.0 else (rMoonArcmin * rMoonArcmin) / (rSunArcmin * rSunArcmin) * 100.0
            }
            else -> {
                // Overlapping circles area formula
                val d = sepArcmin
                val r1 = rSunArcmin
                val r2 = rMoonArcmin
                val part1 = r1 * r1 * acos(((d * d + r1 * r1 - r2 * r2) / (2.0 * d * r1)).coerceIn(-1.0, 1.0))
                val part2 = r2 * r2 * acos(((d * d + r2 * r2 - r1 * r1) / (2.0 * d * r2)).coerceIn(-1.0, 1.0))
                val part3 = 0.5 * sqrt(max(0.0, (-d + r1 + r2) * (d + r1 - r2) * (d - r1 + r2) * (d + r1 + r2)))
                val areaOverlap = part1 + part2 - part3
                val sunArea = PI * r1 * r1
                (areaOverlap / sunArea * 100.0).coerceIn(0.0, 100.0)
            }
        }

        val type = when {
            coveragePercent.compareTo(99.5) >= 0 -> if (rMoonArcmin >= rSunArcmin) EclipseType.SOLAR_TOTAL else EclipseType.SOLAR_ANNULAR
            coveragePercent.compareTo(0.5) > 0 -> EclipseType.SOLAR_PARTIAL
            else -> EclipseType.NONE
        }

        val isVisible = sunAltAz.altitudeDeg > -2.0 || moonAltAz.altitudeDeg > -2.0

        return EclipseData(
            type = type,
            coveragePercentage = coveragePercent,
            sunAltAz = sunAltAz,
            moonAltAz = moonAltAz,
            angularSeparationArcmin = sepArcmin,
            sunRadiusArcmin = rSunArcmin,
            moonRadiusArcmin = rMoonArcmin,
            c1TimeMs = timestampMs - 3600000L,
            maxEclipseTimeMs = timestampMs,
            c4TimeMs = timestampMs + 3600000L,
            isVisibleAboveHorizon = isVisible
        )
    }
}
