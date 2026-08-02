package com.example.astro_engine

import java.util.Calendar
import kotlin.math.*

object SunEngine {

    data class SunPosition(
        val raDeg: Double,
        val decDeg: Double,
        val eclipticLongitudeDeg: Double
    )

    data class TwilightPhase(
        val nameEn: String,
        val nameFa: String,
        val descriptionEn: String,
        val descriptionFa: String,
        val isDaylight: Boolean
    )

    data class SunEvents(
        val sunsetMs: Long?,
        val sunriseMs: Long?,
        val astronomicalDuskMs: Long?, // Peak dark start
        val astronomicalDawnMs: Long?  // Peak dark end
    )

    /**
     * Calculates Sun events for observer's location starting from today's noon.
     */
    fun calculateSunEvents(
        userLatDeg: Double,
        userLonDeg: Double,
        nowMs: Long = System.currentTimeMillis()
    ): SunEvents {
        val cal = Calendar.getInstance(TimeEngine.TEHRAN_TIME_ZONE).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val noonMs = cal.timeInMillis

        var sunsetMs: Long? = null
        var astroDuskMs: Long? = null
        var astroDawnMs: Long? = null
        var sunriseMs: Long? = null

        val stepMs = 2 * 60 * 1000L // 2-min resolution
        var prevAlt = getSunAltitude(noonMs, userLatDeg, userLonDeg)

        // Scan 24 hours from noon today to noon tomorrow
        for (i in 1..(24 * 30)) {
            val t = noonMs + i * stepMs
            val alt = getSunAltitude(t, userLatDeg, userLonDeg)

            if (prevAlt > 0.0 && alt <= 0.0 && sunsetMs == null) sunsetMs = t
            if (prevAlt > -18.0 && alt <= -18.0 && astroDuskMs == null) astroDuskMs = t
            if (prevAlt <= -18.0 && alt > -18.0 && astroDawnMs == null) astroDawnMs = t
            if (prevAlt <= 0.0 && alt > 0.0 && sunriseMs == null) sunriseMs = t

            prevAlt = alt
        }

        return SunEvents(sunsetMs, sunriseMs, astroDuskMs, astroDawnMs)
    }

    fun getSunAltitude(timestampMs: Long, userLatDeg: Double, userLonDeg: Double): Double {
        val jd = TimeEngine.getJulianDate(timestampMs)
        val sunPos = calculatePosition(jd)
        val lastDeg = TimeEngine.getLAST(jd, userLonDeg)
        val horiz = CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg,
            userLatDeg
        )
        return horiz.altitudeDeg
    }

    /**
     * Calculates the Sun's position for a given Julian Date using simplified VSOP87.
     */
    fun calculatePosition(jd: Double): SunPosition {
        val n = jd - 2451545.0 // Days from epoch J2000.0
        val L = (280.460 + 0.9856474 * n) % 360.0 // Mean longitude
        val g = Math.toRadians((357.528 + 0.9856003 * n) % 360.0) // Mean anomaly

        val lambda = L + 1.915 * sin(g) + 0.020 * sin(2 * g) // Ecliptic longitude in deg
        val lambdaRad = Math.toRadians(lambda)

        val epsilon = Math.toRadians(23.439 - 0.0000004 * n) // Obliquity of ecliptic

        val alphaRad = atan2(cos(epsilon) * sin(lambdaRad), cos(lambdaRad))
        var alphaDeg = Math.toDegrees(alphaRad)
        if (alphaDeg < 0) alphaDeg += 360.0

        val deltaRad = asin(sin(epsilon) * sin(lambdaRad))
        val deltaDeg = Math.toDegrees(deltaRad)

        return SunPosition(raDeg = alphaDeg, decDeg = deltaDeg, eclipticLongitudeDeg = (lambda % 360.0 + 360) % 360)
    }

    /**
     * Determines twilight phase based on Sun altitude.
     */
    fun getTwilightPhase(sunAltitudeDeg: Double): TwilightPhase {
        return when {
            sunAltitudeDeg > 0.0 -> TwilightPhase(
                nameEn = "Daylight",
                nameFa = "روز / روشنایی",
                descriptionEn = "Sun is above horizon",
                descriptionFa = "خورشید بالای افق است",
                isDaylight = true
            )
            sunAltitudeDeg > -6.0 -> TwilightPhase(
                nameEn = "Civil Twilight (Golden Hour)",
                nameFa = "گرگ و میش شهری (ساعت طلایی)",
                descriptionEn = "Brightest stars become visible",
                descriptionFa = "درخشان‌ترین ستارگان پدیدار می‌شوند",
                isDaylight = false
            )
            sunAltitudeDeg > -12.0 -> TwilightPhase(
                nameEn = "Nautical Twilight",
                nameFa = "گرگ و میش دریانوردی",
                descriptionEn = "Navigation stars visible, horizon visible",
                descriptionFa = "ستارگان اصلی ناوبری نمایان می‌شوند",
                isDaylight = false
            )
            sunAltitudeDeg > -18.0 -> TwilightPhase(
                nameEn = "Astronomical Twilight",
                nameFa = "گرگ و میش نجومی",
                descriptionEn = "Sky darkening, deep space targets emerging",
                descriptionFa = "آسمان تاریک شده و جرم‌های اعماق فضا قابل رویت می‌شوند",
                isDaylight = false
            )
            else -> TwilightPhase(
                nameEn = "Night (True Dark)",
                nameFa = "شب کامل نجومی",
                descriptionEn = "Optimal darkness for observation",
                descriptionFa = "تاریکی مطلق و شرایط ایده‌آل برای رصد نجومی",
                isDaylight = false
            )
        }
    }
}
