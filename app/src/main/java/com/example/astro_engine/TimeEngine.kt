package com.example.astro_engine

import com.example.domain.CalendarSystem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

object TimeEngine {

    val TEHRAN_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Tehran")

    /**
     * Converts a UTC timestamp (ms) to Julian Date (JD).
     */
    fun getJulianDate(timestampMs: Long = System.currentTimeMillis()): Double {
        return (timestampMs / 86400000.0) + 2440587.5
    }

    /**
     * Calculates Greenwich Mean Sidereal Time (GMST) in degrees.
     */
    fun getGMST(jd: Double): Double {
        val d = jd - 2451545.0 // Days from J2000.0
        var gmst = 280.46061837 + 360.98564736629 * d
        gmst %= 360.0
        if (gmst < 0) gmst += 360.0
        return gmst
    }

    /**
     * Calculates Local Apparent Sidereal Time (LAST) in degrees given longitude in degrees (+East).
     */
    fun getLAST(jd: Double, longitudeDeg: Double): Double {
        val gmst = getGMST(jd)
        var last = gmst + longitudeDeg
        last %= 360.0
        if (last < 0) last += 360.0
        return last
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
     * Converts Gregorian Year, Month (1-12), Day (1-31) to Solar Hijri (Jalali) date.
     */
    fun toSolarHijri(year: Int, month: Int, day: Int): SolarHijriDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gy = year
        if (gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)) {
            gDaysInMonth[2] = 29
        }

        var gDayNo = 0
        for (i in 1 until month) {
            gDayNo += gDaysInMonth[i]
        }
        gDayNo += day - 1

        var jy = gy - 621
        val gDayNoJ21 = if ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0) 79 else 80

        var jDayNo: Int
        if (gDayNo >= gDayNoJ21) {
            jDayNo = gDayNo - gDayNoJ21
        } else {
            jy -= 1
            jDayNo = gDayNo + 365 + (if ((gy - 1) % 4 == 0 && ((gy - 1) % 100 != 0 || (gy - 1) % 400 == 0)) 1 else 0) - gDayNoJ21
        }

        var jm = 0
        var jd = 0
        if (jDayNo < 186) {
            jm = 1 + (jDayNo / 31)
            jd = 1 + (jDayNo % 31)
        } else {
            jDayNo -= 186
            jm = 7 + (jDayNo / 30)
            jd = 1 + (jDayNo % 30)
        }

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
     * Formats timestamp into full date (e.g. 11 Mordad 1405 or 1 August 2026) obeying calendarSystem and isFa.
     */
    fun formatDate(
        timestampMs: Long = System.currentTimeMillis(),
        calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI,
        isFa: Boolean = false
    ): String {
        val cal = Calendar.getInstance(TEHRAN_TIME_ZONE).apply { timeInMillis = timestampMs }
        return if (calendarSystem == CalendarSystem.SOLAR_HIJRI) {
            val sh = toSolarHijri(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            if (isFa) {
                "${formatPersianNumbers(sh.day.toString())} ${sh.monthNameFa} ${formatPersianNumbers(sh.year.toString())}"
            } else {
                "${sh.day} ${sh.monthNameEn} ${sh.year}"
            }
        } else {
            val sdf = SimpleDateFormat("d MMMM yyyy", if (isFa) Locale("fa") else Locale.ENGLISH).apply {
                timeZone = TEHRAN_TIME_ZONE
            }
            val formatted = sdf.format(Date(timestampMs))
            if (isFa) formatPersianNumbers(formatted) else formatted
        }
    }

    /**
     * Formats timestamp as 24-hour EEE, dd MMM • HH:mm in Iran Time (Asia/Tehran).
     */
    fun formatDateTime24h(timestampMs: Long, isFa: Boolean = false): String {
        return formatDateTime24h(timestampMs, CalendarSystem.SOLAR_HIJRI, isFa)
    }

    /**
     * Formats timestamp as 24-hour EEE, dd MMM • HH:mm obeying calendarSystem and isFa.
     */
    fun formatDateTime24h(
        timestampMs: Long,
        calendarSystem: CalendarSystem,
        isFa: Boolean = false
    ): String {
        val cal = Calendar.getInstance(TEHRAN_TIME_ZONE).apply { timeInMillis = timestampMs }
        val timePart = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TEHRAN_TIME_ZONE }.format(Date(timestampMs))
        val timeFormatted = if (isFa) formatPersianNumbers(timePart) else timePart

        val datePart = if (calendarSystem == CalendarSystem.SOLAR_HIJRI) {
            val sh = toSolarHijri(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            if (isFa) {
                "${formatPersianNumbers(sh.day.toString())} ${sh.monthNameFa}"
            } else {
                "${sh.day} ${sh.monthNameEn}"
            }
        } else {
            val sdf = SimpleDateFormat("dd MMM", if (isFa) Locale("fa") else Locale.ENGLISH).apply {
                timeZone = TEHRAN_TIME_ZONE
            }
            val formatted = sdf.format(Date(timestampMs))
            if (isFa) formatPersianNumbers(formatted) else formatted
        }

        val dayOfWeek = SimpleDateFormat("EEE", if (isFa) Locale("fa") else Locale.ENGLISH).apply {
            timeZone = TEHRAN_TIME_ZONE
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
