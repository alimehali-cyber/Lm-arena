package com.alijafari.red.astronomy.astro_engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/**
 * High-precision 3D Pinhole Camera Projection Engine for Augmented Reality Sky.
 * Converts Horizontal (Azimuth, Altitude) celestial coordinates into 2D screen pixels
 * using the device's True-North 3D rotation matrix or virtual camera orientation.
 */
object ARProjectionEngine {

    data class ProjectedPoint(
        val offset: Offset,
        val depth: Double,
        val isVisible: Boolean
    )

    /**
     * Projects a celestial object at (azimuthDeg, altitudeDeg) onto screen coordinates.
     *
     * @param azimuthDeg True North azimuth of the object (0°=N, 90°=E, 180°=S, 270°=W)
     * @param altitudeDeg Altitude angle above horizon (-90° to +90°)
     * @param rotationMatrix 3x3 True-North device rotation matrix (if available from sensors)
     * @param currentAzimuth Camera pointing azimuth in degrees
     * @param currentAltitude Camera pointing pitch/altitude in degrees
     * @param currentRoll Camera roll angle around optical axis in degrees
     * @param canvasWidth Screen/Canvas width in pixels
     * @param canvasHeight Screen/Canvas height in pixels
     * @param fovXDeg Camera horizontal field of view in degrees
     * @return Offset in pixels if object is in front of camera, or null if behind camera.
     */
    fun projectAltAz(
        azimuthDeg: Double,
        altitudeDeg: Double,
        rotationMatrix: FloatArray?,
        currentAzimuth: Double,
        currentAltitude: Double,
        currentRoll: Double,
        canvasWidth: Float,
        canvasHeight: Float,
        fovXDeg: Double
    ): Offset? {
        val azRad = Math.toRadians(azimuthDeg)
        val altRad = Math.toRadians(altitudeDeg)

        // Celestial unit vector in World frame (East = +X, North = +Y, Up = +Z)
        val ox = cos(altRad) * sin(azRad)
        val oy = cos(altRad) * cos(azRad)
        val oz = sin(altRad)

        val xc: Double
        val yc: Double
        val zc: Double

        if (rotationMatrix != null && rotationMatrix.size == 9 &&
            (rotationMatrix[0] != 0f || rotationMatrix[1] != 0f || rotationMatrix[2] != 0f)
        ) {
            // Transform directly using calibrated 3D rotation matrix:
            // Xc = Right on screen, Yc = Up on screen, Zc = Front of screen towards user
            xc = ox * rotationMatrix[0] + oy * rotationMatrix[3] + oz * rotationMatrix[6]
            yc = ox * rotationMatrix[1] + oy * rotationMatrix[4] + oz * rotationMatrix[7]
            zc = ox * rotationMatrix[2] + oy * rotationMatrix[5] + oz * rotationMatrix[8]
        } else {
            // Virtual camera frame constructed from (currentAzimuth, currentAltitude, currentRoll)
            val cAzRad = Math.toRadians(currentAzimuth)
            val cAltRad = Math.toRadians(currentAltitude)
            val cRollRad = Math.toRadians(currentRoll)

            val px = cos(cAltRad) * sin(cAzRad)
            val py = cos(cAltRad) * cos(cAzRad)
            val pz = sin(cAltRad)

            val rx0 = cos(cAzRad)
            val ry0 = -sin(cAzRad)
            val rz0 = 0.0

            val ux0 = -sin(cAltRad) * sin(cAzRad)
            val uy0 = -sin(cAltRad) * cos(cAzRad)
            val uz0 = cos(cAltRad)

            val cosR = cos(cRollRad)
            val sinR = sin(cRollRad)

            val rx = rx0 * cosR - ux0 * sinR
            val ry = ry0 * cosR - uy0 * sinR
            val rz = rz0 * cosR - uz0 * sinR

            val ux = rx0 * sinR + ux0 * cosR
            val uy = ry0 * sinR + uy0 * cosR
            val uz = rz0 * sinR + uz0 * cosR

            val fx = -px
            val fy = -py
            val fz = -pz

            xc = ox * rx + oy * ry + oz * rz
            yc = ox * ux + oy * uy + oz * uz
            zc = ox * fx + oy * fy + oz * fz
        }

        // Distance along camera optical axis (in front of camera)
        val depth = -zc
        if (depth <= 0.001) return null

        val fovXRad = Math.toRadians(fovXDeg.coerceIn(10.0, 150.0))
        val focalLength = (canvasWidth / 2.0) / tan(fovXRad / 2.0)

        val px = (canvasWidth / 2.0 + (xc / depth) * focalLength).toFloat()
        val py = (canvasHeight / 2.0 - (yc / depth) * focalLength).toFloat()

        if (!px.isFinite() || !py.isFinite()) return null
        return Offset(px, py)
    }
}
