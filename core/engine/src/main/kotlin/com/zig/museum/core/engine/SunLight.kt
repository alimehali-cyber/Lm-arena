package com.zig.museum.core.engine

import kotlin.math.cos
import kotlin.math.sin

/**
 * Sun-direction control per §10.3 and §5.6
 * State: azimuthDeg, elevationDeg, exposureEV
 * World sun direction derived from two angles in object-fixed frame: north up, prime meridian toward viewer's default camera.
 * Pure function sunDirection(azimuthDeg, elevationDeg): Vec3 with unit tests covering cardinal directions and poles.
 *
 * Lighting model per §5.6:
 * One directional light = Sun. Intensity in physical Lux scaled by object's real heliocentric distance:
 * relative irradiance factors (Earth=1.0): Mercury 6.7, Venus 1.9, Earth 1.0, Moon 1.0, Mars 0.43, Jupiter 0.037, Saturn 0.011, Uranus 0.0027, Neptune 0.0011
 * Sun object itself has no directional light dependence; emissive.
 * Exposure user-controlled EV slider, per-object default EV.
 */

data class SunState(
    val azimuthDeg: Float = 0f,
    val elevationDeg: Float = 45f,
    val exposureEV: Float = 0f // EV slider, 0 = default exposure
)

data class Vec3(val x: Float, val y: Float, val z: Float) {
    fun normalize(): Vec3 {
        val len = kotlin.math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()
        return if (len > 1e-6f) Vec3(x/len, y/len, z/len) else Vec3(0f,1f,0f)
    }
}

/**
 * Pure function per §10.3
 */
fun sunDirection(azimuthDeg: Float, elevationDeg: Float): Vec3 {
    // Azimuth: 0 = north? Let's define: azimuth 0 = +Z (prime meridian toward viewer), 90 = +X east, etc.
    // Elevation: 0 = horizon, 90 = zenith
    // Convert to direction vector pointing FROM sun TO object? Actually directional light direction is from sun toward object, but we compute sun direction in object's local frame.
    // For simplicity: azimuth around Y axis, elevation from horizon
    val azRad = Math.toRadians(azimuthDeg.toDouble())
    val elRad = Math.toRadians(elevationDeg.toDouble())
    // Spherical: elevation is angle from horizon, so 0 = horizontal, 90 = up
    // Direction from object to sun (for lighting, we need sun direction)
    val cosEl = cos(elRad).toFloat()
    val sinEl = sin(elRad).toFloat()
    val x = cosEl * sin(azRad).toFloat()
    val y = sinEl
    val z = cosEl * cos(azRad).toFloat()
    return Vec3(x, y, z).normalize()
}

object SunIrradiance {
    // Relative irradiance factors per §5.6 (Earth = 1.0)
    const val MERCURY = 6.7f
    const val VENUS = 1.9f
    const val EARTH = 1.0f
    const val MOON = 1.0f
    const val MARS = 0.43f
    const val JUPITER = 0.037f
    const val SATURN = 0.011f
    const val URANUS = 0.0027f
    const val NEPTUNE = 0.0011f
    const val SUN = 0f // Sun itself emissive, no directional

    fun forObjectId(objectId: String): Float = when (objectId) {
        "mercury" -> MERCURY
        "venus" -> VENUS
        "earth" -> EARTH
        "moon" -> MOON
        "mars" -> MARS
        "jupiter" -> JUPITER
        "saturn" -> SATURN
        "uranus" -> URANUS
        "neptune" -> NEPTUNE
        else -> 1.0f
    }

    // Convert to Lux: Earth ~ 120k lux at top of atmosphere, scaled
    fun toLux(relative: Float, exposureEV: Float = 0f): Float {
        val baseLux = 120000f // Earth base
        val lux = baseLux * relative
        // EV exposure: each EV doubles/halves exposure, but for light intensity we keep physical and exposure separate
        // Here we return lux, exposure applied in tone mapping / view exposure
        return lux
    }
}
