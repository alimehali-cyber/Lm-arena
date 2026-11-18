package com.alijafari.red.astronomy.ui.satellites.core

import kotlin.math.*

internal fun interface Clock {
    fun nowMs(): Long

    companion object {
        val System = Clock(java.lang.System::currentTimeMillis)
    }
}

internal fun interface Ephemeris {
    fun at(timeMs: Long): Geo
}

internal data class Geo(val lat: Double, val lon: Double, val altKm: Double)

internal data class Camera(val yaw: Float, val pitch: Float, val distance: Float)

internal data class Projected(val point: Point, val occluded: Boolean)

/**
 * The GLES camera is at (0,0,d); Greenwich is +Z. f = projected horizon radius * sqrt(d²−1). Supplying the actual disc
 * (in dp OR px) eliminates any second guessed FOV in annotations.
 */
internal class GlobeProjection(val frame: GlobeFrame, val camera: Camera) {
    private val cy = cos(camera.yaw)
    private val sy = sin(camera.yaw)
    private val cp = cos(camera.pitch)
    private val sp = sin(camera.pitch)
    private val focal = frame.diameter * .5f * sqrt(camera.distance * camera.distance - 1)

    fun project(geo: Geo): Projected {
        val lat = Math.toRadians(geo.lat)
        val lon = Math.toRadians(geo.lon)
        val r = 1.0 + geo.altKm / 6371.0
        val ring = r * cos(lat)
        val x = ring * sin(lon)
        val y = r * sin(lat)
        val z = ring * cos(lon)
        val xx = x * cy + z * sy
        val zz = z * cy - x * sy
        val yy = y * cp - zz * sp
        val depth = camera.distance - (y * sp + zz * cp)
        if (depth <= .1) return Projected(frame.center, true)
        val t = (camera.distance * depth / (xx * xx + yy * yy + depth * depth)).coerceIn(0.0, 1.0)
        val occluded = t * t * (xx * xx + yy * yy) + (camera.distance - t * depth).pow(2) < 1 - 1e-7
        return Projected(
            Point(frame.center.x + (focal * xx / depth).toFloat(), frame.center.y - (focal * yy / depth).toFloat()),
            occluded,
        )
    }
}

internal object MotionCues {
    /** Degrees clockwise from screen +X. A scrub towards the past never changes the sampling sign. */
    fun heading(now: Point, tenSecondsLater: Point, thirtySecondsLater: Point, last: Float?, minLength: Float): Float? {
        for (next in arrayOf(tenSecondsLater, thirtySecondsLater)) {
            val dx = next.x - now.x
            val dy = next.y - now.y
            if (hypot(dx, dy) >= minLength) return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        }
        return last
    }

    fun bearing(from: Geo, to: Geo): Double {
        val a = Math.toRadians(from.lat)
        val b = Math.toRadians(to.lat)
        val delta = Math.toRadians(to.lon - from.lon)
        return (Math.toDegrees(atan2(sin(delta) * cos(b), cos(a) * sin(b) - sin(a) * cos(b) * cos(delta))) + 360) % 360
    }
}
