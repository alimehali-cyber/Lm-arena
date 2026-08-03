package com.example.astro_engine

import java.util.Calendar
import kotlin.math.*

object SunEngine {

    data class SunPosition(
        val raDeg: Double,
        val decDeg: Double,
        val eclipticLongitudeDeg: Double,
        val distanceAU: Double = 1.0,
        val apparentDiameterArcmin: Double = 32.0
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
        return getSunAltAz(jd, userLatDeg, userLonDeg).altitudeDeg
    }

    fun getSunAltAz(jd: Double, userLatDeg: Double, userLonDeg: Double): CoordinateEngine.Horizontal {
        val sunPos = calculatePosition(jd)
        val lastDeg = TimeEngine.getLAST(jd, userLonDeg)
        return CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg,
            userLatDeg
        )
    }

    /**
     * Calculates the Sun's high-precision position for Julian Ephemeris Date (Meeus Chapter 25).
     */
    fun calculatePosition(jd: Double): SunPosition {
        val T = TimeEngine.getJulianCenturiesTT(jd)

        // Geometric Mean Longitude of Sun L0
        val L0 = (280.46646 + 36000.76983 * T + 0.0003032 * T * T) % 360.0

        // Mean Anomaly of Sun M
        val M = (357.52911 + 35999.05029 * T - 0.0001537 * T * T) % 360.0
        val Mrad = Math.toRadians(M)

        // Eccentricity of Earth's Orbit e
        val e = 0.016708634 - 0.000042037 * T - 0.0000001267 * T * T

        // Sun's Equation of Center C
        val C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(Mrad) +
                (0.019993 - 0.000101 * T) * sin(2 * Mrad) +
                0.000289 * sin(3 * Mrad)

        // True Longitude
        val trueLong = L0 + C

        // Distance in AU
        val vRad = Mrad + Math.toRadians(C) // True anomaly
        val R = (1.000001018 * (1 - e * e)) / (1 + e * cos(vRad))

        // Nutation and Aberration
        val nutation = CoordinateEngine.calculateNutation(jd)
        val OmegaRad = Math.toRadians((125.04452 - 1934.136261 * T) % 360.0)

        // Apparent Longitude lambda = trueLong + nutationInLong - aberration (-20.4892" / R)
        val apparentLongDeg = trueLong + nutation.deltaPsiDeg - (0.00569 + 0.00478 * sin(OmegaRad))
        val apparentLongRad = Math.toRadians(apparentLongDeg)

        // True Obliquity
        val epsRad = Math.toRadians(nutation.trueObliquityDeg)

        // Right Ascension alpha and Declination delta
        val alphaRad = atan2(cos(epsRad) * sin(apparentLongRad), cos(apparentLongRad))
        var alphaDeg = Math.toDegrees(alphaRad)
        if (alphaDeg < 0) alphaDeg += 360.0

        val deltaRad = asin(sin(epsRad) * sin(apparentLongRad))
        val deltaDeg = Math.toDegrees(deltaRad)

        val diamArcmin = 32.00 / R

        return SunPosition(
            raDeg = alphaDeg,
            decDeg = deltaDeg,
            eclipticLongitudeDeg = (apparentLongDeg % 360.0 + 360.0) % 360.0,
            distanceAU = R,
            apparentDiameterArcmin = diamArcmin
        )
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

