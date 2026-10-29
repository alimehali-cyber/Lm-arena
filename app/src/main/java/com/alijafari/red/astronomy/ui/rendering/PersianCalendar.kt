package com.alijafari.red.astronomy.ui.rendering

import kotlin.math.abs
import kotlin.math.floor

/**
 * The Persian (Solar Hijri) calendar's seasons — the only calendar the landscape's trees use.
 *
 * The valley's foliage is a calendar cue, not a phenology model: the scene is drawn for one
 * instant, and the trees below the sky must agree with the date the sky is showing. That date is
 * the **Persian** one, so spring starts on **1 Farvardin** (Nowruz), summer on 1 Tir, autumn on
 * 1 Mehr and winter on 1 Dey — not on the Gregorian month edges an earlier version of the
 * landscape used.
 *
 * | Season | Persian months | Approx. Gregorian |
 * |---|---|---|
 * | spring | Farvardin, Ordibehesht, Khordad (1–3) | 21 Mar – 21 Jun |
 * | summer | Tir, Mordad, Shahrivar (4–6) | 22 Jun – 22 Sep |
 * | autumn | Mehr, Aban, Azar (7–9) | 23 Sep – 21 Dec |
 * | winter | Dey, Bahman, Esfand (10–12) | 22 Dec – 20 Mar |
 *
 * The conversion is the standard arithmetic Jalali calendar (the `jalaali-js` / Borkowski break
 * table), which is exact for the years this app can show and needs no `Calendar`, no locale and no
 * time zone — so the season of an instant stays a pure function a JVM test can assert.
 */
object PersianCalendar {

    enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

    /** Break years of the Jalali leap cycle, from Borkowski's table (valid 560–3798 AP). */
    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    /** Truncating division, as the reference implementation's `~~(a / b)` behaves. */
    private fun idiv(a: Int, b: Int): Int {
        val q = abs(a) / abs(b)
        return if ((a >= 0) == (b >= 0)) q else -q
    }

    private fun mod(a: Int, b: Int): Int = a - idiv(a, b) * b

    private class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCal {
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        var i = 1
        while (i < BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ = leapJ + idiv(jump, 33) * 8 + idiv(mod(jump, 33), 4)
            jp = jm
            i++
        }
        val gy = jy + 621
        var n = jy - jp
        leapJ = leapJ + idiv(n, 33) * 8 + idiv(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = idiv(gy, 4) - idiv((idiv(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + idiv(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    /** Gregorian date → Julian Day Number (Fliegel–Van Flandern, proleptic Gregorian). */
    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = idiv((gy + idiv(gm - 8, 6) + 100_100) * 1461, 4) +
            idiv(153 * mod(gm + 9, 12) + 2, 5) + gd - 34_840_408
        d = d - idiv(idiv(gy + 100_100 + idiv(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    /** Julian Day Number → Gregorian `[year, month, day]`. */
    private fun d2g(jdn: Int): IntArray {
        var j = 4 * jdn + 139_361_631
        j = j + idiv(idiv(4 * jdn + 183_187_720, 146097) * 3, 4) * 4 - 3908
        val i = idiv(mod(j, 1461), 4) * 5 + 308
        val gd = idiv(mod(i, 153), 5) + 1
        val gm = mod(idiv(i, 153), 12) + 1
        val gy = idiv(j, 1461) - 100_100 + idiv(8 - gm, 6)
        return intArrayOf(gy, gm, gd)
    }

    /**
     * The Jalali year, month and day containing the Julian Day Number [jdn].
     *
     * Month is 1..12 with Farvardin = 1; the six 31-day months come first, then the five 30-day
     * ones, then Esfand.
     */
    fun jalaliOf(jdn: Int): IntArray {
        val gy = d2g(jdn)[0]
        var jy = gy - 621
        val cal = jalCal(jy)
        val jdn1f = g2d(gy, 3, cal.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) return intArrayOf(jy, 1 + idiv(k, 31), mod(k, 31) + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (cal.leap == 1) k += 1
        }
        return intArrayOf(jy, 7 + idiv(k, 30), mod(k, 30) + 1)
    }

    /** Day number of the civil date containing the astronomical [jd]. */
    fun dayNumber(jd: Double): Int = floor(jd + 0.5).toInt()

    /** Jalali month (1..12) of the civil date containing [jd]. */
    fun monthOf(jd: Double): Int = jalaliOf(dayNumber(jd))[1]

    /**
     * The Persian season of the civil date containing [jd].
     *
     * Spring begins at Nowruz, so the season is simply the Jalali month's third of the year.
     */
    fun seasonOf(jd: Double): Season = when ((monthOf(jd) - 1) / 3) {
        0 -> Season.SPRING
        1 -> Season.SUMMER
        2 -> Season.AUTUMN
        else -> Season.WINTER
    }
}
