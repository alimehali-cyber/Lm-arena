package com.example.astro_engine

import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import java.util.Calendar
import java.util.Date
import kotlin.math.*

object MoonEngine {

    data class MoonData(
        val raDeg: Double,
        val decDeg: Double,
        val phaseNameEn: String,
        val phaseNameFa: String,
        val illuminationPercent: Double,
        val ageDays: Double,
        val phaseAngleRad: Double,
        val distanceKm: Double,
        val altitudeDeg: Double = 0.0,
        val azimuthDeg: Double = 0.0,
        val moonriseTimeMs: Long? = null,
        val moonsetTimeMs: Long? = null,
        val nasaFrameNumber: Int = 1
    )

    data class UpcomingPhaseInfo(
        val phaseNameEn: String,
        val phaseNameFa: String,
        val dateMs: Long,
        val daysFromNow: Int
    )

    fun calculateMoon(jd: Double, latitude: Double = 30.1141, longitude: Double = 51.5217): MoonData {
        val T = (jd - 2451545.0) / 36525.0 // Julian centuries from J2000.0

        // Moon's mean longitude L'
        val Lprime = (218.3164477 + 481267.88123421 * T) % 360.0
        // Moon's mean elongation D
        val D = (297.8501921 + 445267.1114034 * T) % 360.0
        // Sun's mean anomaly M
        val M = (357.5291092 + 35999.0502909 * T) % 360.0
        // Moon's mean anomaly M'
        val Mprime = (134.9633964 + 477198.8675055 * T) % 360.0
        // Moon's argument of latitude F
        val F = (93.2720950 + 483202.0175233 * T) % 360.0

        val D_rad = Math.toRadians(D)
        val M_rad = Math.toRadians(M)
        val Mprime_rad = Math.toRadians(Mprime)
        val F_rad = Math.toRadians(F)

        // Ecliptic Longitude perturbations
        val lGeo = Lprime + 6.2886 * sin(Mprime_rad) + 1.2740 * sin(2 * D_rad - Mprime_rad) +
                0.6583 * sin(2 * D_rad) + 0.2136 * sin(2 * Mprime_rad) -
                0.1851 * sin(M_rad) - 0.1143 * sin(2 * F_rad)

        // Ecliptic Latitude perturbations
        val bGeo = 5.1282 * sin(F_rad) + 0.2806 * sin(Mprime_rad + F_rad) +
                0.2777 * sin(Mprime_rad - F_rad) + 0.1732 * sin(2 * D_rad - F_rad)

        // Distance in km
        val distanceKm = 385001.0 - 20905.0 * cos(Mprime_rad) - 3699.0 * cos(2 * D_rad - Mprime_rad) -
                2956.0 * cos(2 * D_rad) - 569.0 * cos(2 * Mprime_rad)

        val lRad = Math.toRadians(lGeo)
        val bRad = Math.toRadians(bGeo)
        val epsRad = Math.toRadians(23.4392911)

        val sinDec = sin(bRad) * cos(epsRad) + cos(bRad) * sin(epsRad) * sin(lRad)
        val decRad = asin(sinDec.coerceIn(-1.0, 1.0))
        val decDeg = Math.toDegrees(decRad)

        val y = sin(lRad) * cos(epsRad) - tan(bRad) * sin(epsRad)
        val x = cos(lRad)
        var raRad = atan2(y, x)
        var raDeg = Math.toDegrees(raRad)
        if (raDeg < 0) raDeg += 360.0

        // Phase angle i and illumination frac
        val iRad = Math.toRadians(180.0 - D - 6.289 * sin(Mprime_rad))
        val illuminationPercent = ((1.0 + cos(iRad)) / 2.0 * 100.0).coerceIn(0.0, 100.0)

        // Synodic Month = 29.53058867 days
        val synodicPeriod = 29.53058867
        val ageDays = ((D % 360.0 + 360) % 360.0) / 360.0 * synodicPeriod

        val (phaseEn, phaseFa) = when {
            ageDays < 1.845 -> "New Moon" to "ماه نو"
            ageDays < 5.536 -> "Waxing Crescent" to "هلال فزاینده"
            ageDays < 9.228 -> "First Quarter" to "تربیع اول"
            ageDays < 12.919 -> "Waxing Gibbous" to "احدب فزاینده"
            ageDays < 16.610 -> "Full Moon" to "بدر کامل"
            ageDays < 20.302 -> "Waning Gibbous" to "احدب کاهنده"
            ageDays < 23.993 -> "Last Quarter" to "تربیع دوم"
            ageDays < 27.685 -> "Waning Crescent" to "هلال کاهنده"
            else -> "New Moon" to "ماه نو"
        }

        // Calculate SunCalc positions & times
        val millis = ((jd - 2440587.5) * 86400000.0).toLong()
        val targetDate = Date(millis)

        val (alt, az, riseMs, setMs) = try {
            val pos = MoonPosition.compute().on(targetDate).at(latitude, longitude).execute()
            val times = MoonTimes.compute().on(targetDate).at(latitude, longitude).execute()
            val rise = times.rise?.toInstant()?.toEpochMilli()
            val set = times.set?.toInstant()?.toEpochMilli()
            Quadruple(pos.altitude, pos.azimuth, rise, set)
        } catch (e: Throwable) {
            Quadruple(25.0, 180.0, millis - 36000000L, millis + 14000000L)
        }

        // Calculate NASA Dial-a-moon frame number
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        val frame = (((dayOfYear - 1) * 24) + hourOfDay + 1).coerceIn(1, 8784)

        return MoonData(
            raDeg = raDeg,
            decDeg = decDeg,
            phaseNameEn = phaseEn,
            phaseNameFa = phaseFa,
            illuminationPercent = illuminationPercent,
            ageDays = ageDays,
            phaseAngleRad = iRad,
            distanceKm = distanceKm,
            altitudeDeg = alt,
            azimuthDeg = az,
            moonriseTimeMs = riseMs,
            moonsetTimeMs = setMs,
            nasaFrameNumber = frame
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun getUpcomingMajorPhases(baseJd: Double): List<UpcomingPhaseInfo> {
        val currentMoon = calculateMoon(baseJd)
        val age = currentMoon.ageDays
        val synodic = 29.53058867
        val baseMillis = ((baseJd - 2440587.5) * 86400000.0).toLong()

        val phases = listOf(
            Triple("Full Moon", "بدر کامل", 14.765),
            Triple("Last Quarter", "تربیع دوم", 22.148),
            Triple("New Moon", "ماه نو", 0.0),
            Triple("First Quarter", "تربیع اول", 7.382)
        )

        return phases.map { (en, fa, targetAge) ->
            var daysDiff = targetAge - age
            if (daysDiff <= 0.2) daysDiff += synodic
            val phaseMillis = baseMillis + (daysDiff * 86400000.0).toLong()
            UpcomingPhaseInfo(
                phaseNameEn = en,
                phaseNameFa = fa,
                dateMs = phaseMillis,
                daysFromNow = kotlin.math.max(1, daysDiff.roundToInt())
            )
        }.sortedBy { it.daysFromNow }
    }
}

