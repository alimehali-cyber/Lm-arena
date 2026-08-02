package com.example.astro_engine

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
        val distanceKm: Double
    )

    /**
     * Calculates Moon position, phase, illumination and synodic age based on Jean Meeus ELP2000-82 simplified model.
     */
    fun calculateMoon(jd: Double): MoonData {
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
        var ageDays = ((D % 360.0 + 360) % 360.0) / 360.0 * synodicPeriod

        val (phaseEn, phaseFa) = when {
            ageDays < 1.845 -> "New Moon" to "ماه نو (ماه جدید)"
            ageDays < 5.536 -> "Waxing Crescent" to "هلال فزاینده (هلال اول)"
            ageDays < 9.228 -> "First Quarter" to "تربیع اول"
            ageDays < 12.919 -> "Waxing Gibbous" to "تحدب فزاینده (کوژماه)"
            ageDays < 16.610 -> "Full Moon" to "ماه کامل (بدر)"
            ageDays < 20.302 -> "Waning Gibbous" to "تحدب کاهنده"
            ageDays < 23.993 -> "Third Quarter" to "تربیع دوم (آخر)"
            ageDays < 27.685 -> "Waning Crescent" to "هلال کاهنده (هلال آخر)"
            else -> "New Moon" to "ماه نو (ماه جدید)"
        }

        return MoonData(
            raDeg = raDeg,
            decDeg = decDeg,
            phaseNameEn = phaseEn,
            phaseNameFa = phaseFa,
            illuminationPercent = illuminationPercent,
            ageDays = ageDays,
            phaseAngleRad = iRad,
            distanceKm = distanceKm
        )
    }
}
