package com.alijafari.red.astronomy.ui.demo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pure camera paths, parameterised by elapsed time rather than frame count. */
object ShowcaseMotion {
    data class BlackHolePose(val distance: Double, val azimuth: Double, val roll: Double, val panDp: Double)
    data class SkyPose(val azimuth: Double, val altitude: Double)

    fun blackHole(progress: Double): BlackHolePose {
        val t = progress.coerceIn(0.0, 1.0)
        val eased = t * t * (3.0 - 2.0 * t)
        return BlackHolePose(
            distance = 56.0 - 26.0 * eased,
            azimuth = 38.0 + 14.0 * eased,
            roll = 3.5 * sin(PI * t),
            panDp = -38.0 + 76.0 * eased
        )
    }

    fun sky(seconds: Double): SkyPose {
        val t = seconds.coerceAtLeast(0.0)
        // Continuous velocity and a gentle rise/fall, without per-frame time clamping.
        return SkyPose(azimuth = 5.5 * t + 2.5 * sin(t * 0.45), altitude = 4.0 * (1.0 - cos(t * 0.38)))
    }
}
