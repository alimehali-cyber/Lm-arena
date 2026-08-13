package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.domain.CalendarSystem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object TimeEngine {

    val TEHRAN_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Tehran")

    /**
     * Converts a UTC timestamp (ms) to Julian Date (JD).
     */
    fun getJulianDate(timestampMs: Long = System.currentTimeMillis()): Double {
        return (timestampMs / 86400000.0) + 2440587.5
    }

    /**
     * Converts Julian Date (JD) back to UTC timestamp (ms).
     */
    fun getTimestampFromJulianDate(jd: Double): Long {
        return ((jd - 2440587.5) * 86400000.0).toLong()
    }

    /**
     * Calculates Delta T (TT - UT1) in seconds based on Espenak & Meeus polynomial approximations.
     */
    fun getDeltaTSeconds(jd: Double): Double {
        return AstroTime.fromJd(jd).deltaT
    }

    /**
     * Calculates Terrestrial Time (TT) Julian Ephemeris Day (JDE).
     */
    fun getJDE(jd: Double): Double {
        val deltaT = getDeltaTSeconds(jd)
        return jd + (deltaT / 86400.0)
    }

    /**
     * Returns Julian Centuries T from J2000.0 in Universal Time (UTC).
     */
    fun getJulianCenturiesUTC(jd: Double): Double {
        return (jd - 2451545.0) / 36525.0
    }

    /**
     * Returns Julian Centuries Te from J2000.0 in Terrestrial Time (TT).
     */
    fun getJulianCenturiesTT(jd: Double): Double {
        val jde = getJDE(jd)
        return (jde - 2451545.0) / 36525.0
    }

    /**
     * Calculates Greenwich Mean Sidereal Time (GMST) in degrees (IAU 2006 formula).
     */
    fun getGMST(jd: Double): Double {
        return FrameTransformationEngine().calculateGMST(AstroTime.fromJd(jd))
    }

    /**
     * Calculates Greenwich Apparent Sidereal Time (GAST) in degrees including Equation of Equinoxes.
     */
    fun getGAST(jd: Double): Double {
        return FrameTransformationEngine().calculateGAST(AstroTime.fromJd(jd))
    }

    /**
     * Calculates Local Apparent Sidereal Time (LAST) in degrees given observer longitude (+East).
     */
    fun getLAST(jd: Double, longitudeDeg: Double): Double {
        return FrameTransformationEngine().calculateLAST(AstroTime.fromJd(jd), longitudeDeg)
    }

    /**
     * Calculates Equation of Time (EoT) in minutes.
     */
    fun getEquationOfTimeMinutes(jd: Double): Double {
        val T = getJulianCenturiesTT(jd)
        val L0 = (280.466456 + 36000.76983 * T + 0.0003032 * T * T) % 360.0
        val M = Math.toRadians((357.52911 + 35999.05029 * T - 0.0001537 * T * T) % 360.0)
        val e = 0.016708634 - 0.000042037 * T
        val y = tan(Math.toRadians(23.439291 / 2.0)).let { it * it }

        val L0rad = Math.toRadians(L0)
        val eotRad = y * sin(2 * L0rad) - 2 * e * sin(M) + 4 * e * y * sin(M) * cos(2 * L0rad) - 0.5 * y * y * sin(4 * L0rad) - 1.25 * e * e * sin(2 * M)
        return Math.toDegrees(eotRad) * 4.0 // 1 deg = 4 minutes
    }

    /**
     * Simple Persian (Solar Hijri) Date representation.
     */
    data class SolarHijriDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val monthNameFa: String,
        val monthNameEn: String
    )

    private val PERSIAN_MONTHS_FA = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val PERSIAN_MONTHS_EN = arrayOf(
        "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"
    )

    /**
     * Converts timestamp directly to Solar Hijri (Jalali) date accurately using ICU PersianCalendar.
     */
    fun toSolarHijri(
        timestampMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TEHRAN_TIME_ZONE
    ): SolarHijriDate {
        val icuTz = android.icu.util.TimeZone.getTimeZone(timeZone.id)
        val pCal = android.icu.util.Calendar.getInstance(
            icuTz,
            android.icu.util.ULocale("fa_IR@calendar=persian")
        ).apply {
            timeInMillis = timestampMs
        }
        val jy = pCal.get(android.icu.util.Calendar.YEAR)
        val jm = pCal.get(android.icu.util.Calendar.MONTH) + 1 // 1..12
        val jd = pCal.get(android.icu.util.Calendar.DAY_OF_MONTH)

        val monthIdx = (jm - 1).coerceIn(0, 11)
        return SolarHijriDate(
            year = jy,
            month = jm,
            day = jd,
            monthNameFa = PERSIAN_MONTHS_FA[monthIdx],
            monthNameEn = PERSIAN_MONTHS_EN[monthIdx]
        )
    }

    /**
     * Converts Gregorian Year, Month (1-12), Day (1-31) to Solar Hijri (Jalali) date.
     */
    fun toSolarHijri(year: Int, month: Int, day: Int): SolarHijriDate {
        val cal = Calendar.getInstance(TEHRAN_TIME_ZONE).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return toSolarHijri(cal.timeInMillis, TEHRAN_TIME_ZONE)
    }

    /**
     * Converts Persian digits to English digits if needed or vice-versa.
     */
    fun formatPersianNumbers(str: String): String {
        val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(faDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Formats timestamp in 24-hour HH:mm format in Iran Time (Asia/Tehran).
     */
    fun formatTime24h(timestampMs: Long = System.currentTimeMillis(), isFa: Boolean = false): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TEHRAN_TIME_ZONE
        }
        val formatted = sdf.format(Date(timestampMs))
        return if (isFa) formatPersianNumbers(formatted) else formatted
    }

    /**
     * Formats timestamp in 24-hour HH:mm:ss format in Iran Time (Asia/Tehran).
     */
    fun formatTimeWithSeconds24h(timestampMs: Long = System.currentTimeMillis(), isFa: Boolean = false): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TEHRAN_TIME_ZONE
        }
        val formatted = sdf.format(Date(timestampMs))
        return if (isFa) formatPersianNumbers(formatted) else formatted
    }

    /**
     * Formats timestamp into full date (e.g. 15 Mordad 1405 or 6 August 2026) obeying calendarSystem and isFa.
     */
    fun formatDate(
        timestampMs: Long = System.currentTimeMillis(),
        calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI,
        isFa: Boolean = false,
        timeZone: TimeZone = TEHRAN_TIME_ZONE
    ): String {
        return if (calendarSystem == CalendarSystem.SOLAR_HIJRI) {
            val sh = toSolarHijri(timestampMs, timeZone)
            if (isFa) {
                "${formatPersianNumbers(sh.day.toString())} ${sh.monthNameFa} ${formatPersianNumbers(sh.year.toString())}"
            } else {
                "${sh.day} ${sh.monthNameEn} ${sh.year}"
            }
        } else {
            val sdf = SimpleDateFormat("d MMMM yyyy", if (isFa) Locale("fa") else Locale.ENGLISH).apply {
                this.timeZone = timeZone
            }
            val formatted = sdf.format(Date(timestampMs))
            if (isFa) formatPersianNumbers(formatted) else formatted
        }
    }

    /**
     * Formats timestamp as 24-hour EEE, dd MMM • HH:mm in Iran Time (Asia/Tehran).
     */
    fun formatDateTime24h(timestampMs: Long, isFa: Boolean = false): String {
        return formatDateTime24h(timestampMs, CalendarSystem.SOLAR_HIJRI, isFa, TEHRAN_TIME_ZONE)
    }

    /**
     * Formats timestamp as 24-hour EEE, dd MMM • HH:mm obeying calendarSystem and isFa.
     */
    fun formatDateTime24h(
        timestampMs: Long,
        calendarSystem: CalendarSystem,
        isFa: Boolean = false,
        timeZone: TimeZone = TEHRAN_TIME_ZONE
    ): String {
        val timePart = SimpleDateFormat("HH:mm", Locale.US).apply { this.timeZone = timeZone }.format(Date(timestampMs))
        val timeFormatted = if (isFa) formatPersianNumbers(timePart) else timePart

        val datePart = if (calendarSystem == CalendarSystem.SOLAR_HIJRI) {
            val sh = toSolarHijri(timestampMs, timeZone)
            if (isFa) {
                "${formatPersianNumbers(sh.day.toString())} ${sh.monthNameFa}"
            } else {
                "${sh.day} ${sh.monthNameEn}"
            }
        } else {
            val sdf = SimpleDateFormat("dd MMM", if (isFa) Locale("fa") else Locale.ENGLISH).apply {
                this.timeZone = timeZone
            }
            val formatted = sdf.format(Date(timestampMs))
            if (isFa) formatPersianNumbers(formatted) else formatted
        }

        val dayOfWeek = SimpleDateFormat("EEE", if (isFa) Locale("fa") else Locale.ENGLISH).apply {
            this.timeZone = timeZone
        }.format(Date(timestampMs))
        val dowFormatted = if (isFa) formatPersianNumbers(dayOfWeek) else dayOfWeek

        return "$dowFormatted، $datePart • $timeFormatted"
    }

    /**
     * Formats timestamp as 24-hour yyyy/MM/dd HH:mm in Iran Time (Asia/Tehran).
     */
    fun formatFullDateTime24h(timestampMs: Long, isFa: Boolean = false): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).apply {
            timeZone = TEHRAN_TIME_ZONE
        }
        val formatted = sdf.format(Date(timestampMs))
        return if (isFa) formatPersianNumbers(formatted) else formatted
    }
}

