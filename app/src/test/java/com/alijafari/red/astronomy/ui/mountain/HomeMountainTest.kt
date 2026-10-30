package com.alijafari.red.astronomy.ui.mountain

import com.alijafari.red.astronomy.ui.rendering.PersianCalendar
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeMountainTest {
    @Test fun mountainWardrobeMatchesValleyCalendar() {
        assertEquals(MountainSeason.GREEN, mountainSeason(PersianCalendar.Season.SPRING))
        assertEquals(MountainSeason.GREEN, mountainSeason(PersianCalendar.Season.SUMMER))
        assertEquals(MountainSeason.AUTUMN, mountainSeason(PersianCalendar.Season.AUTUMN))
        assertEquals(MountainSeason.SNOW, mountainSeason(PersianCalendar.Season.WINTER))
    }
}
