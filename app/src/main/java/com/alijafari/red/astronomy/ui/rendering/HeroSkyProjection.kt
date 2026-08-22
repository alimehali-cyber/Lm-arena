package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.geometry.Offset

/**
 * Shared HeroSky Projection Model for RED Astronomical Canvas.
 *
 * Defines the unified relationship:
 * - Azimuth [0°, 360°]: 0° and 360° map to the same horizontal coordinate (panoramic x = az / 360 * width).
 * - Altitude 0°: Horizon (altitude = 0° maps to horizonY = canvasHeight * 0.85).
 * - Altitude +90°: Zenith (altitude = +90° maps to topMargin = 24dp/px).
 * - Altitudes < 0° scale continuously below the horizon line.
 */
object HeroSkyProjection {

    const val HORIZON_FRACTION = 0.85f
    const val TOP_MARGIN_PX = 24.0f

    /**
     * Projects spherical Horizontal coordinates (azimuth, altitude) into screen space (x, y)
     * for Hero Sky panoramic canvas.
     *
     * @param azimuthDeg Azimuth in degrees [0, 360) where 0 = North, 90 = East, 180 = South, 270 = West
     * @param altitudeDeg Altitude in degrees [-90, +90] where 0 = Horizon, +90 = Zenith
     * @param canvasWidth Width of the drawing canvas in pixels
     * @param canvasHeight Height of the drawing canvas in pixels
     * @return Screen coordinate Offset(x, y)
     */
    fun project(
        azimuthDeg: Double,
        altitudeDeg: Double,
        canvasWidth: Float,
        canvasHeight: Float
    ): Offset {
        val normAz = ((azimuthDeg % 360.0) + 360.0) % 360.0
        val x = (normAz / 360.0 * canvasWidth).toFloat()

        val horizonY = canvasHeight * HORIZON_FRACTION
        val scale = (horizonY - TOP_MARGIN_PX) / 90.0f
        val y = horizonY - (altitudeDeg.toFloat() * scale)

        return Offset(x, y)
    }

    /**
     * Calculates the shortest Euclidean screen distance between a tap position and a projected celestial object,
     * accounting for continuous horizontal cylindrical wraparound at the 0°/360° azimuth boundary.
     */
    fun screenDistance(
        tapPos: Offset,
        projectedPos: Offset,
        canvasWidth: Float
    ): Float {
        val dxDirect = kotlin.math.abs(tapPos.x - projectedPos.x)
        val dxWrapped = if (canvasWidth > 0f) kotlin.math.min(dxDirect, canvasWidth - dxDirect) else dxDirect
        val dy = tapPos.y - projectedPos.y
        return kotlin.math.hypot(dxWrapped, dy)
    }

    /**
     * Angular azimuth shortest distance in degrees between two azimuths, correctly handling 0°/360° wraparound.
     * E.g. distance between 359° and 1° is 2°.
     */
    fun azimuthDistanceDeg(az1: Double, az2: Double): Double {
        val diff = kotlin.math.abs(az1 - az2) % 360.0
        return if (diff > 180.0) 360.0 - diff else diff
    }

    /**
     * Returns the horizon line Y coordinate on the canvas.
     */
    fun getHorizonY(canvasHeight: Float): Float {
        return canvasHeight * HORIZON_FRACTION
    }
}
