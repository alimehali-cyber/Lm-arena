package com.alijafari.red.astronomy.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contracts for the landscape's calendar.
 *
 * Both species' wardrobes hang off [PersianCalendar], so these are the boundary dates the whole
 * valley's foliage depends on. Spring must begin on **1 Farvardin** — Nowruz — not on 1 March: the
 * landscape is Iranian, the sky above it is dated in the Persian calendar, and a tree in April leaf
 * below a sky dated 1 Ordibehesht has to be in the same calendar as the date the user is reading.
 *
 * The Julian days below are noon UT of each civil date, which is how the app hands an instant to the
 * renderer (`_JD` + 0.5 for the observing day).
 */
class PersianCalendarTest {

    // ---- the conversion ----------------------------------------------------------------

    @Test
    fun nowruzIsTheFirstDayOfSpring() {
        // 1405 began on 2026-03-21; the day before is still 29 Esfand 1404.
        assertJalali(1405, 1, 1, 2_461_121)
        assertJalali(1404, 12, 29, 2_461_120)
        // 1403 began on 2024-03-20 (a leap Esfand ended the year before it).
        assertJalali(1403, 1, 1, 2_460_390)
        assertJalali(1402, 12, 29, 2_460_389)
        // 1402 began on 2023-03-21.
        assertJalali(1402, 1, 1, 2_460_025)
    }

    @Test
    fun theYearQuarterDaysAreTheFirstOfTirMehrAndDey() {
        assertJalali(1403, 4, 1, 2_460_483)   // 1 Tir 1403   = 2024-06-21
        assertJalali(1403, 7, 1, 2_460_576)   // 1 Mehr 1403  = 2024-09-22
        assertJalali(1403, 10, 1, 2_460_666)  // 1 Dey 1403   = 2024-12-21
        assertJalali(1403, 3, 31, 2_460_482)  // 31 Khordad   = 2024-06-20
        assertJalali(1403, 6, 31, 2_460_575)  // 31 Shahrivar = 2024-09-21
        assertJalali(1403, 9, 30, 2_460_665)  // 30 Azar      = 2024-12-20
        assertJalali(1403, 10, 12, 2_460_677) // 2025-01-01
    }

    @Test
    fun seasonsSplitTheYearIntoPersianQuarters() {
        // Spring: Farvardin, Ordibehesht, Khordad. It begins at Nowruz, not at an equinox guess.
        assertEquals(PersianCalendar.Season.WINTER, PersianCalendar.seasonOf(2_460_389.5)) // 19 Mar 2024
        assertEquals(PersianCalendar.Season.SPRING, PersianCalendar.seasonOf(2_460_390.5)) // 20 Mar 2024
        assertEquals(PersianCalendar.Season.SPRING, PersianCalendar.seasonOf(2_460_482.5)) // 20 Jun 2024
        // Summer: Tir, Mordad, Shahrivar.
        assertEquals(PersianCalendar.Season.SUMMER, PersianCalendar.seasonOf(2_460_483.5)) // 21 Jun 2024
        assertEquals(PersianCalendar.Season.SUMMER, PersianCalendar.seasonOf(2_460_575.5)) // 21 Sep 2024
        // Autumn: Mehr, Aban, Azar.
        assertEquals(PersianCalendar.Season.AUTUMN, PersianCalendar.seasonOf(2_460_576.5)) // 22 Sep 2024
        assertEquals(PersianCalendar.Season.AUTUMN, PersianCalendar.seasonOf(2_460_665.5)) // 20 Dec 2024
        // Winter: Dey, Bahman, Esfand.
        assertEquals(PersianCalendar.Season.WINTER, PersianCalendar.seasonOf(2_460_666.5)) // 21 Dec 2024
        assertEquals(PersianCalendar.Season.WINTER, PersianCalendar.seasonOf(2_461_120.5)) // 20 Mar 2026
        assertEquals(PersianCalendar.Season.SPRING, PersianCalendar.seasonOf(2_461_121.5)) // 21 Mar 2026
    }

    @Test
    fun theMonthIsTheQuarterOfTheSeasonMapping() {
        // A round trip over a whole Persian year: month m belongs to season (m - 1) / 3.
        val nowruz1403 = 2_460_390
        val seasonByMonth = arrayOf(
            PersianCalendar.Season.SPRING, PersianCalendar.Season.SPRING, PersianCalendar.Season.SPRING,
            PersianCalendar.Season.SUMMER, PersianCalendar.Season.SUMMER, PersianCalendar.Season.SUMMER,
            PersianCalendar.Season.AUTUMN, PersianCalendar.Season.AUTUMN, PersianCalendar.Season.AUTUMN,
            PersianCalendar.Season.WINTER, PersianCalendar.Season.WINTER, PersianCalendar.Season.WINTER
        )
        var day = 0
        for (month in 1..12) {
            assertEquals("month $month", month, PersianCalendar.monthOf(nowruz1403 + day + 0.5))
            assertEquals("month $month season", seasonByMonth[month - 1], PersianCalendar.seasonOf(nowruz1403 + day + 0.5))
            // Walk one day past the last day of this month: 31 through Shahrivar, 30 after, then
            // Esfand's 29/30 (the final month is checked only where the year is not leap).
            day += if (month <= 6) 31 else 30
        }
    }

    private fun assertJalali(year: Int, month: Int, dayOfMonth: Int, jdn: Int) {
        val got = PersianCalendar.jalaliOf(jdn)
        assertEquals("$year/$month/$dayOfMonth year", year, got[0])
        assertEquals("$year/$month/$dayOfMonth month", month, got[1])
        assertEquals("$year/$month/$dayOfMonth day", dayOfMonth, got[2])
    }
}
