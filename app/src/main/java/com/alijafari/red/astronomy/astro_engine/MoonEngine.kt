package com.alijafari.red.astronomy.astro_engine

import org.shredzone.commons.suncalc.MoonTimes
import java.util.Calendar
import java.util.Date
import kotlin.math.*

/**
 * Moon engine — delegates to LunarSolarEngine (ELP2000-82B).
 */
object MoonEngine {

    private val lunarSolar = LunarSolarEngine()

    data class MoonPosition(
        val raDeg: Double,
        val decDeg: Double,
        val distanceKm: Double,
        val phaseAngleDeg: Double,
        val illuminatedFraction: Double,
        val ageDays: Double,
        val elongationDeg: Double
    )

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

    fun calculateMoon(astroTime: AstroTime): MoonPosition {
        val lunar = lunarSolar.calculateMoon(astroTime)
        val sun = lunarSolar.calculateSun(astroTime)
        val phaseAngle = calculatePhaseAngle(lunar, sun)
        val illuminated = (1.0 + cos(phaseAngle * Math.PI / 180.0)) / 2.0
        val elongation = calculateElongation(lunar, sun)
        val age = (elongation / 360.0) * 29.530588853

        return MoonPosition(
            raDeg = lunar.raDeg,
            decDeg = lunar.decDeg,
            distanceKm = lunar.distanceKm,
            phaseAngleDeg = phaseAngle,
            illuminatedFraction = illuminated,
            ageDays = age,
            elongationDeg = elongation
        )
    }

    fun calculateMoon(
        jd: Double,
        latitude: Double = 30.1141,
        longitude: Double = 51.5217,
        elevationM: Double = 0.0
    ): MoonData {
        val astroTime = AstroTime.fromJd(jd)
        val lunar = lunarSolar.calculateMoon(astroTime)
        val sun = lunarSolar.calculateSun(astroTime)

        val distanceKm = lunar.distanceKm
        val raDeg = lunar.raDeg
        val decDeg = lunar.decDeg

        val lastDeg = TimeEngine.getLAST(jd, longitude)
        val topoEq = CoordinateEngine.geocentricToTopocentric(
            geocentric = CoordinateEngine.Equatorial(raDeg, decDeg),
            geocentricDistanceKm = distanceKm,
            lastDeg = lastDeg,
            latitudeDeg = latitude,
            elevationM = elevationM
        )

        val phaseAngle = calculatePhaseAngle(lunar, sun)
        val illuminatedFraction = (1.0 + cos(phaseAngle * Math.PI / 180.0)) / 2.0
        val illuminationPercent = (illuminatedFraction * 100.0).coerceIn(0.0, 100.0)
        val elongation = calculateElongation(lunar, sun)
        val ageDays = (elongation / 360.0) * 29.530588853

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

        val T = astroTime.jcTt
        val Omega = (125.0445479 - 1934.1362891 * T + 0.0020754 * T * T) % 360.0
        val lGeo = lunar.apparentLongitudeDeg
        val bGeo = lunar.apparentLatitudeDeg
        val bRad = Math.toRadians(bGeo)
        val W = lGeo - Omega
        val W_rad = Math.toRadians(W)
        val I_incl = Math.toRadians(1.54242)
        val sinb = sin(bRad)
        val cosb = cos(bRad)
        val sinL_O = sin(W_rad)
        val cosL_O = cos(W_rad)

        val sinbLibr = sinb * cos(I_incl) - cosb * sin(I_incl) * sinL_O
        val librationLatDeg = Math.toDegrees(asin(sinbLibr.coerceIn(-1.0, 1.0)))
        val librationLonDeg = Math.toDegrees(atan2(-sinL_O * cosb * cos(I_incl) + sinb * sin(I_incl), cosL_O * cosb))

        val sunRaRad = Math.toRadians(sun.raDeg)
        val sunDecRad = Math.toRadians(sun.decDeg)
        val moonRaRad = Math.toRadians(topoEq.raDeg)
        val moonDecRad = Math.toRadians(topoEq.decDeg)

        val numChi = cos(sunDecRad) * sin(sunRaRad - moonRaRad)
        val denChi = sin(sunDecRad) * cos(moonDecRad) - cos(sunDecRad) * sin(moonDecRad) * cos(sunRaRad - moonRaRad)
        var brightLimbAngleDeg = Math.toDegrees(atan2(numChi, denChi))
        if (brightLimbAngleDeg < 0) brightLimbAngleDeg += 360.0

        val earthshinePercent = if (illuminationPercent in 1.0..30.0) {
            (1.0 - (illuminationPercent / 30.0)) * 85.0
        } else 0.0

        val angularDiameterArcmin = (3518.0 / (distanceKm / 384400.0)) / 60.0

        val horiz = CoordinateEngine.equatorialToHorizontal(
            equatorial = topoEq,
            lastDeg = lastDeg,
            latitudeDeg = latitude,
            observerElevationM = elevationM
        )

        val millis = astroTime.utcMs
        val targetDate = Date(millis)

        val (riseMs, setMs) = try {
            val times = MoonTimes.compute().on(targetDate).at(latitude, longitude).execute()
            val rise = times.rise?.toInstant()?.toEpochMilli()
            val set = times.set?.toInstant()?.toEpochMilli()
            Pair(rise, set)
        } catch (e: Throwable) {
            Pair(millis - 36000000L, millis + 14000000L)
        }

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
            phaseAngleRad = phaseAngle * Math.PI / 180.0,
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
                daysFromNow = max(1, daysDiff.roundToInt())
            )
        }.sortedBy { it.daysFromNow }
    }

    private fun calculatePhaseAngle(lunar: LunarSolarEngine.LunarPosition, sun: LunarSolarEngine.SolarPosition): Double {
        val raDiff = (lunar.raDeg - sun.raDeg) * Math.PI / 180.0
        val decMoon = lunar.decDeg * Math.PI / 180.0
        val decSun = sun.decDeg * Math.PI / 180.0

        val cosPhase = cos(decMoon) * cos(decSun) * cos(raDiff) + sin(decMoon) * sin(decSun)
        return acos(cosPhase.coerceIn(-1.0, 1.0)) * 180.0 / Math.PI
    }

    private fun calculateElongation(lunar: LunarSolarEngine.LunarPosition, sun: LunarSolarEngine.SolarPosition): Double {
        var elong = lunar.apparentLongitudeDeg - sun.apparentLongitudeDeg
        while (elong < 0) elong += 360.0
        while (elong >= 360.0) elong -= 360.0
        return elong
    }
}
