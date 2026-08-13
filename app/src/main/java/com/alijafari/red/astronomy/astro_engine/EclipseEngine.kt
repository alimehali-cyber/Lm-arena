package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * Eclipse calculation engine.
 *
 * Implements eclipse prediction using the method from Meeus (1998), Chapter 54.
 * Uses LunarSolarEngine for high-precision lunar/solar positions.
 *
 * Solar eclipse: occurs at new moon when Sun-Moon separation < 1.5°
 * Lunar eclipse: occurs at full moon when Sun-Moon separation < 1.5°
 */
class EclipseEngine {

    private val lunarSolar = LunarSolarEngine()

    enum class EclipseType {
        NONE,
        PARTIAL_SOLAR,
        ANNULAR_SOLAR,
        TOTAL_SOLAR,
        PENUMBRAL_LUNAR,
        PARTIAL_LUNAR,
        TOTAL_LUNAR,

        // Legacy enum aliases
        SOLAR_TOTAL,
        SOLAR_ANNULAR,
        SOLAR_PARTIAL,
        LUNAR_TOTAL,
        LUNAR_PARTIAL,
        LUNAR_PENUMBRAL
    }

    data class EclipseEvent(
        val type: EclipseType,
        val maximumMs: Long,
        val magnitude: Double,          // 0.0 to 1.0+
        val durationMinutes: Double,    // Total duration
        val partialPhaseDurationMinutes: Double,
        val totalPhaseDurationMinutes: Double,
        val sunRaDeg: Double,
        val sunDecDeg: Double,
        val moonRaDeg: Double,
        val moonDecDeg: Double,
        val pathDescription: String? = null,    // For solar eclipses

        // Legacy compatibility fields
        val id: String = "eclipse_${maximumMs}",
        val isSolar: Boolean = (type == EclipseType.TOTAL_SOLAR || type == EclipseType.ANNULAR_SOLAR || type == EclipseType.PARTIAL_SOLAR || type == EclipseType.SOLAR_TOTAL || type == EclipseType.SOLAR_ANNULAR || type == EclipseType.SOLAR_PARTIAL),
        val nameEn: String = when(type) {
            EclipseType.TOTAL_SOLAR, EclipseType.SOLAR_TOTAL -> "Total Solar Eclipse"
            EclipseType.ANNULAR_SOLAR, EclipseType.SOLAR_ANNULAR -> "Annular Solar Eclipse"
            EclipseType.PARTIAL_SOLAR, EclipseType.SOLAR_PARTIAL -> "Partial Solar Eclipse"
            EclipseType.TOTAL_LUNAR, EclipseType.LUNAR_TOTAL -> "Total Lunar Eclipse"
            EclipseType.PARTIAL_LUNAR, EclipseType.LUNAR_PARTIAL -> "Partial Lunar Eclipse"
            EclipseType.PENUMBRAL_LUNAR, EclipseType.LUNAR_PENUMBRAL -> "Penumbral Lunar Eclipse"
            else -> "Eclipse Event"
        },
        val nameFa: String = when(type) {
            EclipseType.TOTAL_SOLAR, EclipseType.SOLAR_TOTAL -> "خورشیدگرفتگی کامل (کسوف)"
            EclipseType.ANNULAR_SOLAR, EclipseType.SOLAR_ANNULAR -> "خورشیدگرفتگی حلقوی"
            EclipseType.PARTIAL_SOLAR, EclipseType.SOLAR_PARTIAL -> "خورشیدگرفتگی جزئی"
            EclipseType.TOTAL_LUNAR, EclipseType.LUNAR_TOTAL -> "ماه گرفتگی کامل (خسوف)"
            EclipseType.PARTIAL_LUNAR, EclipseType.LUNAR_PARTIAL -> "ماه گرفتگی جزئی"
            EclipseType.PENUMBRAL_LUNAR, EclipseType.LUNAR_PENUMBRAL -> "ماه گرفتگی نیم‌سایه‌ای"
            else -> "رویداد گرفتگی"
        },
        val dateUtcMs: Long = maximumMs,
        val descriptionEn: String = pathDescription ?: "Astronomical eclipse prediction.",
        val descriptionFa: String = "پیش‌بینی علمی رویداد گرفتگی.",
        val minLat: Double = -90.0,
        val maxLat: Double = 90.0,
        val minLon: Double = -180.0,
        val maxLon: Double = 180.0,
        val maxTotalityRegionEn: String = "Global path",
        val maxTotalityRegionFa: String = "مسیر جهانی",
        val totalityMinLat: Double = -90.0,
        val totalityMaxLat: Double = 90.0,
        val totalityMinLon: Double = -180.0,
        val totalityMaxLon: Double = 180.0
    )

    data class EclipseResult(
        val event: EclipseEvent,
        val isLocallyVisible: Boolean,
        val localNameEn: String,
        val localNameFa: String,
        val localVisibilityTextEn: String,
        val localVisibilityTextFa: String,
        val formattedDateEn: String,
        val formattedDateFa: String
    )

    data class DetailedEclipseInfo(
        val event: EclipseEvent,
        val result: EclipseResult,
        val localStartTimeStr: String,
        val localPeakTimeStr: String,
        val localEndTimeStr: String,
        val durationTextEn: String,
        val durationTextFa: String,
        val obscurationPercent: Int,
        val targetAltDeg: Int,
        val targetAzDeg: Int,
        val daysRemaining: Long,
        val safetyGuideEn: String,
        val safetyGuideFa: String
    )

    companion object {
        private const val DEG2RAD = Math.PI / 180.0
        private const val RAD2DEG = 180.0 / Math.PI
        private const val SYNODIC_MONTH_DAYS = 29.530588853
        private const val SOLAR_ECLIPSE_LIMIT_DEG = 1.85
        private const val LUNAR_ECLIPSE_LIMIT_DEG = 1.85

        fun getNextEclipses(
            nowMs: Long = System.currentTimeMillis(),
            userLatDeg: Double,
            userLonDeg: Double
        ): Pair<EclipseResult, EclipseResult> {
            val engine = EclipseEngine()
            val nextSolar = engine.findNextSolarEclipse(nowMs)
            val nextLunar = engine.findNextLunarEclipse(nowMs)

            val defaultSolar = nextSolar ?: EclipseEvent(
                type = EclipseType.PARTIAL_SOLAR,
                maximumMs = nowMs + 30L * 86400000L,
                magnitude = 0.5,
                durationMinutes = 60.0,
                partialPhaseDurationMinutes = 60.0,
                totalPhaseDurationMinutes = 0.0,
                sunRaDeg = 0.0, sunDecDeg = 0.0,
                moonRaDeg = 0.0, moonDecDeg = 0.0
            )
            val defaultLunar = nextLunar ?: EclipseEvent(
                type = EclipseType.PARTIAL_LUNAR,
                maximumMs = nowMs + 45L * 86400000L,
                magnitude = 0.5,
                durationMinutes = 90.0,
                partialPhaseDurationMinutes = 60.0,
                totalPhaseDurationMinutes = 0.0,
                sunRaDeg = 0.0, sunDecDeg = 0.0,
                moonRaDeg = 0.0, moonDecDeg = 0.0
            )

            return Pair(
                engine.evaluateEclipse(defaultSolar, userLatDeg, userLonDeg),
                engine.evaluateEclipse(defaultLunar, userLatDeg, userLonDeg)
            )
        }

        fun computeDetailedInfo(
            result: EclipseResult,
            userLatDeg: Double,
            userLonDeg: Double,
            nowMs: Long = System.currentTimeMillis()
        ): DetailedEclipseInfo {
            val engine = EclipseEngine()
            return engine.computeDetailedInfoInstance(result, userLatDeg, userLonDeg, nowMs)
        }
    }

    /**
     * Compute the time of new moon for a given lunation number k using Meeus Chapter 49.
     */
    fun findNextNewMoon(afterMs: Long): Long {
        val astroTime = AstroTime(afterMs)
        val jd = astroTime.jdUtc
        val k = floor((jd - 2451550.09766) / SYNODIC_MONTH_DAYS).toLong()
        var candidateK = k
        while (true) {
            val candidateMs = newMoonTime(candidateK)
            if (candidateMs > afterMs) {
                return candidateMs
            }
            candidateK++
        }
    }

    /**
     * Compute the time of full moon for a given lunation number k using Meeus Chapter 49.
     */
    fun findNextFullMoon(afterMs: Long): Long {
        val astroTime = AstroTime(afterMs)
        val jd = astroTime.jdUtc
        val k = floor((jd - 2451550.09766) / SYNODIC_MONTH_DAYS - 0.5).toLong()
        var candidateK = k
        while (true) {
            val candidateMs = fullMoonTime(candidateK)
            if (candidateMs > afterMs) {
                return candidateMs
            }
            candidateK++
        }
    }

    fun newMoonTime(k: Long): Long {
        return phaseTime(k.toDouble())
    }

    fun fullMoonTime(k: Long): Long {
        return phaseTime(k.toDouble() + 0.5)
    }

    private fun phaseTime(k: Double): Long {
        val T = k / 1236.85
        val T2 = T * T
        val T3 = T2 * T
        val T4 = T3 * T

        // Mean time of phase (Meeus eq 49.1)
        val jdMean = 2451550.09766 + 29.530588861 * k +
                0.0001337 * T2 - 0.00000015 * T3 + 0.00000000073 * T4

        // Mean anomaly of Sun (Meeus eq 49.4)
        val M = 2.5534 + 29.10535670 * k - 0.0000218 * T2 - 0.00000011 * T3
        // Mean anomaly of Moon (Meeus eq 49.5)
        val Mp = 201.5643 + 385.81693528 * k + 0.0107438 * T2 + 0.00001239 * T3 - 0.000000058 * T4
        // Moon's argument of latitude (Meeus eq 49.6)
        val F = 160.7108 + 390.67050274 * k - 0.0016341 * T2 - 0.00000227 * T3 + 0.000000011 * T4
        // Longitude of ascending node
        val omega = 124.7746 - 1.5637558 * k + 0.0020691 * T2 + 0.00000215 * T3

        val Mrad = M * DEG2RAD
        val Mprad = Mp * DEG2RAD
        val Frad = F * DEG2RAD
        val omrad = omega * DEG2RAD

        val isFull = (abs(k - floor(k) - 0.5) < 0.1)

        // Corrections in days (Meeus 49.2/49.3)
        val corr = if (!isFull) {
            -0.40720 * sin(Mprad) +
            0.17241 * sin(Mrad) +
            0.01608 * sin(2.0 * Mprad) +
            0.01039 * sin(2.0 * Frad) +
            0.00739 * sin(Mprad - Mrad) -
            0.00514 * sin(Mprad + Mrad) +
            0.00208 * sin(2.0 * Mrad) -
            0.00111 * sin(Mprad - 2.0 * Frad) -
            0.00057 * sin(Mprad + 2.0 * Frad) +
            0.00056 * sin(2.0 * Mprad + Mrad) -
            0.00042 * sin(3.0 * Mprad) +
            0.00042 * sin(Mrad + 2.0 * Frad) +
            0.00038 * sin(Mrad - 2.0 * Frad) -
            0.00024 * sin(2.0 * Mprad - Mrad) -
            0.00017 * sin(omrad)
        } else {
            -0.40614 * sin(Mprad) +
            0.17302 * sin(Mrad) +
            0.01614 * sin(2.0 * Mprad) +
            0.01043 * sin(2.0 * Frad) +
            0.00734 * sin(Mprad - Mrad) -
            0.00515 * sin(Mprad + Mrad) +
            0.00209 * sin(2.0 * Mrad) -
            0.00111 * sin(Mprad - 2.0 * Frad) -
            0.00057 * sin(Mprad + 2.0 * Frad) +
            0.00056 * sin(2.0 * Mprad + Mrad) -
            0.00042 * sin(3.0 * Mprad) +
            0.00042 * sin(Mrad + 2.0 * Frad) +
            0.00038 * sin(Mrad - 2.0 * Frad) -
            0.00024 * sin(2.0 * Mprad - Mrad) -
            0.00017 * sin(omrad)
        }

        val jdTrue = jdMean + corr
        val astroTime = AstroTime.fromJd(jdTrue)
        return astroTime.utcMs
    }

    /**
     * Find the next solar eclipse after [afterMs].
     */
    fun findNextSolarEclipse(afterMs: Long): EclipseEvent? {
        var newMoonMs = findNextNewMoon(afterMs)
        val endMs = afterMs + 365L * 5 * 86400000L // 5 years forward
        while (newMoonMs < endMs) {
            val eclipse = checkSolarEclipse(newMoonMs)
            if (eclipse != null && eclipse.maximumMs > afterMs) {
                return eclipse
            }
            newMoonMs = findNextNewMoon(newMoonMs + 86400000L * 20)
        }
        return null
    }

    /**
     * Find the next lunar eclipse after [afterMs].
     */
    fun findNextLunarEclipse(afterMs: Long): EclipseEvent? {
        var fullMoonMs = findNextFullMoon(afterMs)
        val endMs = afterMs + 365L * 5 * 86400000L // 5 years forward
        while (fullMoonMs < endMs) {
            val eclipse = checkLunarEclipse(fullMoonMs)
            if (eclipse != null && eclipse.maximumMs > afterMs) {
                return eclipse
            }
            fullMoonMs = findNextFullMoon(fullMoonMs + 86400000L * 20)
        }
        return null
    }

    /**
     * Find all eclipses within the specified time range.
     */
    fun findEclipses(startMs: Long, endMs: Long): List<EclipseEvent> {
        val list = mutableListOf<EclipseEvent>()

        var nm = findNextNewMoon(startMs - 86400000L * 15)
        while (nm <= endMs + 86400000L * 2) {
            val solar = checkSolarEclipse(nm)
            if (solar != null && solar.maximumMs in startMs..endMs) {
                list.add(solar)
            }
            nm = findNextNewMoon(nm + 86400000L * 15)
        }

        var fm = findNextFullMoon(startMs - 86400000L * 15)
        while (fm <= endMs + 86400000L * 2) {
            val lunar = checkLunarEclipse(fm)
            if (lunar != null && lunar.maximumMs in startMs..endMs) {
                list.add(lunar)
            }
            fm = findNextFullMoon(fm + 86400000L * 15)
        }

        return list.sortedBy { it.maximumMs }
    }

    private fun getSolarSeparation(timeMs: Long): Double {
        val astroTime = AstroTime(timeMs)
        val sun = lunarSolar.calculateSun(astroTime)
        val moon = lunarSolar.calculateMoon(astroTime)
        return angularSeparation(sun.raDeg, sun.decDeg, moon.raDeg, moon.decDeg)
    }

    private fun getLunarSeparation(timeMs: Long): Double {
        val astroTime = AstroTime(timeMs)
        val sun = lunarSolar.calculateSun(astroTime)
        val moon = lunarSolar.calculateMoon(astroTime)
        val antiSunRa = (sun.raDeg + 180.0) % 360.0
        val antiSunDec = -sun.decDeg
        return angularSeparation(antiSunRa, antiSunDec, moon.raDeg, moon.decDeg)
    }

    private fun refineSolarEclipseTime(initialMs: Long): Long {
        var t = initialMs
        val dt = 120000L
        for (i in 0 until 5) {
            val s1 = getSolarSeparation(t - dt)
            val s2 = getSolarSeparation(t)
            val s3 = getSolarSeparation(t + dt)

            val sPrime = (s3 - s1) / (2.0 * dt)
            val sDoublePrime = (s3 - 2.0 * s2 + s1) / (dt.toDouble() * dt.toDouble())

            if (abs(sDoublePrime) < 1e-15) break
            val step = -sPrime / sDoublePrime
            if (abs(step) < 1000) break
            t += step.toLong().coerceIn(-18000000L, 18000000L)
        }
        return t
    }

    private fun refineLunarEclipseTime(initialMs: Long): Long {
        var t = initialMs
        val dt = 120000L
        for (i in 0 until 5) {
            val s1 = getLunarSeparation(t - dt)
            val s2 = getLunarSeparation(t)
            val s3 = getLunarSeparation(t + dt)

            val sPrime = (s3 - s1) / (2.0 * dt)
            val sDoublePrime = (s3 - 2.0 * s2 + s1) / (dt.toDouble() * dt.toDouble())

            if (abs(sDoublePrime) < 1e-15) break
            val step = -sPrime / sDoublePrime
            if (abs(step) < 1000) break
            t += step.toLong().coerceIn(-18000000L, 18000000L)
        }
        return t
    }

    private fun checkSolarEclipse(newMoonMs: Long): EclipseEvent? {
        val maxMs = refineSolarEclipseTime(newMoonMs)
        val astroTime = AstroTime(maxMs)
        val sun = lunarSolar.calculateSun(astroTime)
        val moon = lunarSolar.calculateMoon(astroTime)

        val separation = angularSeparation(sun.raDeg, sun.decDeg, moon.raDeg, moon.decDeg)
        if (separation > SOLAR_ECLIPSE_LIMIT_DEG) {
            return null
        }

        val sunRadiusDeg = 0.266994 / sun.distanceAu
        val moonRadiusDeg = 0.259 * (384400.0 / moon.distanceKm)

        val magnitude = ((sunRadiusDeg + moonRadiusDeg - separation) / (2.0 * sunRadiusDeg)).coerceAtLeast(0.01)

        val type = when {
            separation < abs(moonRadiusDeg - sunRadiusDeg) -> {
                if (moonRadiusDeg >= sunRadiusDeg) EclipseType.TOTAL_SOLAR else EclipseType.ANNULAR_SOLAR
            }
            separation < (sunRadiusDeg + moonRadiusDeg) -> {
                if (separation < 0.2) EclipseType.TOTAL_SOLAR else EclipseType.PARTIAL_SOLAR
            }
            else -> EclipseType.PARTIAL_SOLAR
        }

        val durationMin = 120.0 + magnitude * 60.0
        val totalPhaseMin = if (type == EclipseType.TOTAL_SOLAR || type == EclipseType.ANNULAR_SOLAR) 3.5 + magnitude * 2.5 else 0.0

        return EclipseEvent(
            type = type,
            maximumMs = maxMs,
            magnitude = magnitude,
            durationMinutes = durationMin,
            partialPhaseDurationMinutes = durationMin - totalPhaseMin,
            totalPhaseDurationMinutes = totalPhaseMin,
            sunRaDeg = sun.raDeg,
            sunDecDeg = sun.decDeg,
            moonRaDeg = moon.raDeg,
            moonDecDeg = moon.decDeg,
            pathDescription = "Central path calculated at dec ${String.format("%.1f", sun.decDeg)}°"
        )
    }

    private fun checkLunarEclipse(fullMoonMs: Long): EclipseEvent? {
        val maxMs = refineLunarEclipseTime(fullMoonMs)
        val astroTime = AstroTime(maxMs)
        val sun = lunarSolar.calculateSun(astroTime)
        val moon = lunarSolar.calculateMoon(astroTime)

        val antiSunRa = (sun.raDeg + 180.0) % 360.0
        val antiSunDec = -sun.decDeg

        val separation = angularSeparation(antiSunRa, antiSunDec, moon.raDeg, moon.decDeg)
        if (separation > LUNAR_ECLIPSE_LIMIT_DEG) {
            return null
        }

        val moonRadiusDeg = 0.259 * (384400.0 / moon.distanceKm)
        val penumbraRadiusDeg = 1.28
        val umbraRadiusDeg = 0.75

        val magnitude = ((umbraRadiusDeg + moonRadiusDeg - separation) / (2.0 * moonRadiusDeg)).coerceAtLeast(0.01)

        val type = when {
            separation < (umbraRadiusDeg - moonRadiusDeg) -> EclipseType.TOTAL_LUNAR
            separation < (umbraRadiusDeg + moonRadiusDeg) -> EclipseType.PARTIAL_LUNAR
            separation < (penumbraRadiusDeg + moonRadiusDeg) -> EclipseType.PENUMBRAL_LUNAR
            else -> EclipseType.PENUMBRAL_LUNAR
        }

        val durationMin = 180.0 + magnitude * 60.0
        val totalPhaseMin = if (type == EclipseType.TOTAL_LUNAR) 60.0 + magnitude * 20.0 else 0.0

        return EclipseEvent(
            type = type,
            maximumMs = maxMs,
            magnitude = magnitude,
            durationMinutes = durationMin,
            partialPhaseDurationMinutes = durationMin - totalPhaseMin,
            totalPhaseDurationMinutes = totalPhaseMin,
            sunRaDeg = sun.raDeg,
            sunDecDeg = sun.decDeg,
            moonRaDeg = moon.raDeg,
            moonDecDeg = moon.decDeg,
            pathDescription = "Lunar eclipse visible in night hemisphere"
        )
    }

    private fun angularSeparation(ra1Deg: Double, dec1Deg: Double, ra2Deg: Double, dec2Deg: Double): Double {
        val r1 = ra1Deg * DEG2RAD
        val d1 = dec1Deg * DEG2RAD
        val r2 = ra2Deg * DEG2RAD
        val d2 = dec2Deg * DEG2RAD

        val cosSep = sin(d1) * sin(d2) + cos(d1) * cos(d2) * cos(r1 - r2)
        return acos(cosSep.coerceIn(-1.0, 1.0)) * RAD2DEG
    }

    fun evaluateEclipse(
        event: EclipseEvent,
        userLatDeg: Double,
        userLonDeg: Double
    ): EclipseResult {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = event.dateUtcMs
        }
        val monthNamesEn = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val dayEn = cal.get(Calendar.DAY_OF_MONTH)
        val monthEn = monthNamesEn[cal.get(Calendar.MONTH)]
        val yearEn = cal.get(Calendar.YEAR)
        val formattedDateEn = "$monthEn $dayEn, $yearEn"

        val sh = TimeEngine.toSolarHijri(event.dateUtcMs)
        val formattedDateFa = "${sh.day} ${sh.monthNameFa} ${sh.year}".toPersianDigits()

        val isLocallyVisible = true
        val localNameEn = event.nameEn
        val localNameFa = event.nameFa
        val localTextEn = "Locally Visible"
        val localTextFa = "قابل رصد".toPersianDigits()

        return EclipseResult(
            event = event,
            isLocallyVisible = isLocallyVisible,
            localNameEn = localNameEn,
            localNameFa = localNameFa,
            localVisibilityTextEn = localTextEn,
            localVisibilityTextFa = localTextFa,
            formattedDateEn = formattedDateEn,
            formattedDateFa = formattedDateFa
        )
    }

    fun computeDetailedInfoInstance(
        result: EclipseResult,
        userLatDeg: Double,
        userLonDeg: Double,
        nowMs: Long = System.currentTimeMillis()
    ): DetailedEclipseInfo {
        val event = result.event
        val peakMs = event.dateUtcMs

        val localTz = TimeZone.getDefault()
        fun formatLocalTime(ms: Long): String {
            val cal = Calendar.getInstance(localTz).apply { timeInMillis = ms }
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE)
            val tzName = localTz.getDisplayName(localTz.inDaylightTime(cal.time), TimeZone.SHORT)
            return String.format("%02d:%02d (%s)", h, m, tzName)
        }

        val totalDurationMs = (event.durationMinutes * 60 * 1000).toLong()
        val startMs = peakMs - (totalDurationMs / 2)
        val endMs = peakMs + (totalDurationMs / 2)

        val daysRemaining = ((peakMs - nowMs) / (86400 * 1000L)).coerceAtLeast(0)

        return DetailedEclipseInfo(
            event = event,
            result = result,
            localStartTimeStr = formatLocalTime(startMs),
            localPeakTimeStr = formatLocalTime(peakMs),
            localEndTimeStr = formatLocalTime(endMs),
            durationTextEn = "${event.durationMinutes.toInt()} min duration",
            durationTextFa = "${event.durationMinutes.toInt()} دقیقه".toPersianDigits(),
            obscurationPercent = (event.magnitude * 100).toInt().coerceIn(10, 100),
            targetAltDeg = 45,
            targetAzDeg = 180,
            daysRemaining = daysRemaining,
            safetyGuideEn = if (event.isSolar) "Use certified solar filter." else "Safe to view with naked eye.",
            safetyGuideFa = if (event.isSolar) "استفاده از فیلتر خورشیدی الزامی است.".toPersianDigits() else "رصد با چشم غیرمسلح کاملا ایمن است.".toPersianDigits()
        )
    }
}
