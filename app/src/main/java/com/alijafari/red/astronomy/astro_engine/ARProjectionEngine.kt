package com.alijafari.red.astronomy.astro_engine

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
     * Computes the exact camera focal length in screen pixels, dynamically matching
     * CameraX PreviewView.ScaleType.FILL_CENTER geometry and physical sensor characteristics.
     */
    fun computeCameraFocalLengthPx(
        context: Context?,
        screenWidthPx: Float,
        screenHeightPx: Float,
        zoomFactor: Float = 1.0f
    ): Float {
        var baseFocalLengthPx = 0f
        if (context != null) {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                if (cameraManager != null) {
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        val chars = cameraManager.getCameraCharacteristics(id)
                        chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                    }
                    if (cameraId != null) {
                        val chars = cameraManager.getCameraCharacteristics(cameraId)
                        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                        if (focalLengths != null && focalLengths.isNotEmpty() && sensorSize != null) {
                            val fMm = focalLengths[0]
                            val sensorLongDimMm = max(sensorSize.width, sensorSize.height)
                            val fovYRad = 2.0 * atan(sensorLongDimMm / (2.0 * fMm))
                            baseFocalLengthPx = ((screenHeightPx / 2.0) / tan(fovYRad / 2.0)).toFloat()
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to default
            }
        }

        // Standard 63.5° vertical field-of-view for smartphone main wide camera
        if (baseFocalLengthPx <= 0f || !baseFocalLengthPx.isFinite()) {
            val defaultFovYRad = Math.toRadians(63.5)
            baseFocalLengthPx = ((screenHeightPx / 2.0) / tan(defaultFovYRad / 2.0)).toFloat()
        }

        return baseFocalLengthPx * zoomFactor
    }

    /**
     * Computes the effective horizontal field of view in degrees matching the focal length in pixels.
     */
    fun computeEffectiveFovXDeg(
        screenWidthPx: Float,
        focalLengthPx: Float
    ): Double {
        if (focalLengthPx <= 0f) return 55.0
        val halfW = screenWidthPx / 2.0
        return Math.toDegrees(2.0 * atan(halfW / focalLengthPx))
    }

    /**
     * Projects a celestial object at (azimuthDeg, altitudeDeg) onto screen coordinates using exact focal length.
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
        focalLengthPx: Float
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
            // Transform directly using calibrated 3D True-North rotation matrix:
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

        val px = (canvasWidth / 2.0 + (xc / depth) * focalLengthPx).toFloat()
        val py = (canvasHeight / 2.0 - (yc / depth) * focalLengthPx).toFloat()

        if (!px.isFinite() || !py.isFinite()) return null
        return Offset(px, py)
    }

    /**
     * Backward-compatible overload accepting fovXDeg.
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
        val fovXRad = Math.toRadians(fovXDeg.coerceIn(10.0, 150.0))
        val focalLengthPx = ((canvasWidth / 2.0) / tan(fovXRad / 2.0)).toFloat()
        return projectAltAz(
            azimuthDeg = azimuthDeg,
            altitudeDeg = altitudeDeg,
            rotationMatrix = rotationMatrix,
            currentAzimuth = currentAzimuth,
            currentAltitude = currentAltitude,
            currentRoll = currentRoll,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            focalLengthPx = focalLengthPx
        )
    }
}

