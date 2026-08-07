package com.alijafari.red.astronomy.astro_engine

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
        val librationLonDeg: Double = 0.0,
        val librationLatDeg: Double = 0.0,
        val brightLimbAngleDeg: Double = 0.0,
        val earthshinePercent: Double = 0.0,
        val angularDiameterArcmin: Double = 31.0,
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

    fun calculateMoon(
        jd: Double,
        latitude: Double = 30.1141,
        longitude: Double = 51.5217,
        elevationM: Double = 0.0
    ): MoonData {
        val T = TimeEngine.getJulianCenturiesTT(jd)

        // Moon's mean longitude L'
        val Lprime = (218.3164477 + 481267.88123421 * T - 0.0015786 * T * T) % 360.0
        // Moon's mean elongation D
        val D = (297.8501921 + 445267.1114034 * T - 0.0018819 * T * T) % 360.0
        // Sun's mean anomaly M
        val M = (357.5291092 + 35999.0502909 * T - 0.0001536 * T * T) % 360.0
        // Moon's mean anomaly M'
        val Mprime = (134.9633964 + 477198.8675055 * T + 0.0087414 * T * T) % 360.0
        // Moon's argument of latitude F
        val F = (93.2720950 + 483202.0175233 * T - 0.0036539 * T * T) % 360.0
        // Longitude of ascending node Omega
        val Omega = (125.0445479 - 1934.1362891 * T + 0.0020754 * T * T) % 360.0

        val D_rad = Math.toRadians(D)
        val M_rad = Math.toRadians(M)
        val Mprime_rad = Math.toRadians(Mprime)
        val F_rad = Math.toRadians(F)
        val Omega_rad = Math.toRadians(Omega)

        // Ecliptic Longitude perturbations (Meeus Ch. 47)
        val lGeo = Lprime + 6.2886 * sin(Mprime_rad) + 1.2740 * sin(2 * D_rad - Mprime_rad) +
                0.6583 * sin(2 * D_rad) + 0.2136 * sin(2 * Mprime_rad) -
                0.1851 * sin(M_rad) - 0.1143 * sin(2 * F_rad) +
                0.0588 * sin(2 * D_rad - 2 * Mprime_rad) + 0.0572 * sin(2 * D_rad - M_rad - Mprime_rad)

        // Ecliptic Latitude perturbations
        val bGeo = 5.1282 * sin(F_rad) + 0.2806 * sin(Mprime_rad + F_rad) +
                0.2777 * sin(Mprime_rad - F_rad) + 0.1732 * sin(2 * D_rad - F_rad) +
                0.0554 * sin(2 * D_rad - Mprime_rad + F_rad)

        // Distance in km
        val distanceKm = 385001.0 - 20905.0 * cos(Mprime_rad) - 3699.0 * cos(2 * D_rad - Mprime_rad) -
                2956.0 * cos(2 * D_rad) - 569.0 * cos(2 * Mprime_rad) - 158.0 * cos(M_rad)

        val lRad = Math.toRadians(lGeo)
        val bRad = Math.toRadians(bGeo)
        val nutation = CoordinateEngine.calculateNutation(jd)
        val epsRad = Math.toRadians(nutation.trueObliquityDeg)

        // Geocentric RA / Dec
        val sinDec = sin(bRad) * cos(epsRad) + cos(bRad) * sin(epsRad) * sin(lRad)
        val decRad = asin(sinDec.coerceIn(-1.0, 1.0))
        val decDeg = Math.toDegrees(decRad)

        val y = sin(lRad) * cos(epsRad) - tan(bRad) * sin(epsRad)
        val x = cos(lRad)
        var raRad = atan2(y, x)
        var raDeg = Math.toDegrees(raRad)
        if (raDeg < 0) raDeg += 360.0

        // Apply Topocentric Parallax Correction for exact observer elevation & lat/lon!
        val lastDeg = TimeEngine.getLAST(jd, longitude)
        val topoEq = CoordinateEngine.geocentricToTopocentric(
            geocentric = CoordinateEngine.Equatorial(raDeg, decDeg),
            geocentricDistanceKm = distanceKm,
            lastDeg = lastDeg,
            latitudeDeg = latitude,
            elevationM = elevationM
        )

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

        // Optical Librations in longitude & latitude (Meeus Ch. 53)
        val W = lGeo - Omega
        val W_rad = Math.toRadians(W)
        val I_incl = Math.toRadians(1.54242) // Inclination of lunar equator
        val sinb = sin(bRad)
        val cosb = cos(bRad)
        val sinL_O = sin(W_rad)
        val cosL_O = cos(W_rad)

        val sinbLibr = sinb * cos(I_incl) - cosb * sin(I_incl) * sinL_O
        val librationLatDeg = Math.toDegrees(asin(sinbLibr.coerceIn(-1.0, 1.0)))

        val librationLonDeg = Math.toDegrees(atan2(-sinL_O * cosb * cos(I_incl) + sinb * sin(I_incl), cosL_O * cosb))

        // Position angle of bright limb chi
        val sunPos = SunEngine.calculatePosition(jd)
        val sunRaRad = Math.toRadians(sunPos.raDeg)
        val sunDecRad = Math.toRadians(sunPos.decDeg)
        val moonRaRad = Math.toRadians(topoEq.raDeg)
        val moonDecRad = Math.toRadians(topoEq.decDeg)

        val numChi = cos(sunDecRad) * sin(sunRaRad - moonRaRad)
        val denChi = sin(sunDecRad) * cos(moonDecRad) - cos(sunDecRad) * sin(moonDecRad) * cos(sunRaRad - moonRaRad)
        var brightLimbAngleDeg = Math.toDegrees(atan2(numChi, denChi))
        if (brightLimbAngleDeg < 0) brightLimbAngleDeg += 360.0

        // Earthshine visibility estimation (highest near crescent phases when ageDays is 1-5 or 25-28)
        val earthshinePercent = if (illuminationPercent in 1.0..30.0) {
            (1.0 - (illuminationPercent / 30.0)) * 85.0
        } else 0.0

        val angularDiameterArcmin = (3518.0 / (distanceKm / 384400.0)) / 60.0

        // Calculate Horizontal position (Alt/Az)
        val horiz = CoordinateEngine.equatorialToHorizontal(
            equatorial = topoEq,
            lastDeg = lastDeg,
            latitudeDeg = latitude,
            observerElevationM = elevationM
        )

        // Calculate SunCalc moonrise & moonset
        val millis = ((jd - 2440587.5) * 86400000.0).toLong()
        val targetDate = Date(millis)

        val (riseMs, setMs) = try {
            val times = MoonTimes.compute().on(targetDate).at(latitude, longitude).execute()
            val rise = times.rise?.toInstant()?.toEpochMilli()
            val set = times.set?.toInstant()?.toEpochMilli()
            Pair(rise, set)
        } catch (e: Throwable) {
            Pair(millis - 36000000L, millis + 14000000L)
        }

        // Calculate NASA Dial-a-moon frame number
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        val frame = (((dayOfYear - 1) * 24) + hourOfDay + 1).coerceIn(1, 8784)

        return MoonData(
            raDeg = topoEq.raDeg,
            decDeg = topoEq.decDeg,
            phaseNameEn = phaseEn,
            phaseNameFa = phaseFa,
            illuminationPercent = illuminationPercent,
            ageDays = ageDays,
            phaseAngleRad = iRad,
            distanceKm = distanceKm,
            altitudeDeg = horiz.altitudeDeg,
            azimuthDeg = horiz.azimuthDeg,
            librationLonDeg = librationLonDeg,
            librationLatDeg = librationLatDeg,
            brightLimbAngleDeg = brightLimbAngleDeg,
            earthshinePercent = earthshinePercent,
            angularDiameterArcmin = angularDiameterArcmin,
            moonriseTimeMs = riseMs,
            moonsetTimeMs = setMs,
            nasaFrameNumber = frame
        )
    }

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


