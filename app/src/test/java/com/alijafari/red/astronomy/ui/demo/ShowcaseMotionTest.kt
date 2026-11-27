package com.alijafari.red.astronomy.ui.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowcaseMotionTest {
    @Test fun blackHoleDriftsAndZoomsFromAStableWideShot() {
        val start = ShowcaseMotion.blackHole(0.0)
        val middle = ShowcaseMotion.blackHole(0.5)
        val end = ShowcaseMotion.blackHole(1.0)
        assertEquals(56.0, start.distance, 0.001)
        assertEquals(30.0, end.distance, 0.001)
        assertTrue(start.distance > middle.distance && middle.distance > end.distance)
        assertTrue(start.panDp < middle.panDp && middle.panDp < end.panDp)
        assertTrue(start.azimuth < middle.azimuth && middle.azimuth < end.azimuth)
        assertTrue(middle.roll in 0.0..4.0)
        assertEquals(start, ShowcaseMotion.blackHole(-1.0))
        assertEquals(end, ShowcaseMotion.blackHole(2.0))
    }

    @Test fun skyPathIsContinuousAndMovesInBothAxes() {
        val start = ShowcaseMotion.sky(0.0)
        val middle = ShowcaseMotion.sky(4.0)
        val end = ShowcaseMotion.sky(8.0)
        assertEquals(0.0, start.azimuth, 0.0)
        assertEquals(0.0, start.altitude, 0.0)
        assertTrue(start.azimuth < middle.azimuth && middle.azimuth < end.azimuth)
        assertTrue(middle.altitude > start.altitude)
        assertTrue(kotlin.math.abs(ShowcaseMotion.sky(4.001).azimuth - middle.azimuth) < 0.02)
    }
}
