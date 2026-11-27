package com.alijafari.red.astronomy.ui.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowcasePlanTest {
    @Test fun recordingIsFiftySecondsInTheRequestedOrder() {
        val steps = ShowcasePlan.steps
        assertEquals(50_000L, steps.sumOf { it.durationMs })
        assertEquals(listOf(
            ShowcaseScene.HOME, ShowcaseScene.BLACK_HOLE, ShowcaseScene.GRAVITY,
            ShowcaseScene.SATELLITES, ShowcaseScene.AR, ShowcaseScene.TIME, ShowcaseScene.END
        ), steps.map { it.scene })
        assertTrue(steps.all { it.durationMs > 0 })
    }
}
