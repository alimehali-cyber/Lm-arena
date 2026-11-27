package com.alijafari.red.astronomy.ui.demo

enum class ShowcaseScene { IDLE, HOME, BLACK_HOLE, GRAVITY, SATELLITES, AR, TIME, END }

data class ShowcaseStep(val scene: ShowcaseScene, val durationMs: Long)

/** The recording is 50 seconds; scene durations include their transition time. */
object ShowcasePlan {
    val steps = listOf(
        ShowcaseStep(ShowcaseScene.HOME, 6_000),
        ShowcaseStep(ShowcaseScene.GRAVITY, 7_000),
        ShowcaseStep(ShowcaseScene.SATELLITES, 8_000),
        ShowcaseStep(ShowcaseScene.AR, 8_000),
        ShowcaseStep(ShowcaseScene.TIME, 8_000),
        ShowcaseStep(ShowcaseScene.BLACK_HOLE, 7_000),
        ShowcaseStep(ShowcaseScene.END, 6_000),
    )
}
