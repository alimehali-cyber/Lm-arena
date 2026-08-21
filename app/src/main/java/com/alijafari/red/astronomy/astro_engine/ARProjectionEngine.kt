package com.alijafari.red.astronomy.astro_engine

import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/**
 * High-Precision Camera2 / CameraX Projection Engine for Augmented Reality Astronomy.
 *
 * Mathematically rigorous transformation pipeline:
 *
 * 1. World Celestial Coordinate Frame (RED True-North ENU):
 *    - +X_world = True East
 *    - +Y_world = True North
 *    - +Z_world = Zenith (Up)
 *    - Unit direction vector: v_world = [cos(Alt)*sin(Az), cos(Alt)*cos(Az), sin(Alt)]^T
 *
 * 2. Device Coordinate Frame (Android Sensor Natural Orientation):
 *    - +X_dev = Screen Right (portrait natural orientation)
 *    - +Y_dev = Screen Top (portrait natural orientation)
 *    - +Z_dev = Out of front screen (towards user)
 *    - Transformation from World to Device: v_dev = R_true^T * v_world
 *      where R_true is the True-North declination-corrected rotation matrix (SO(3)) from OrientationProvider.
 *
 * 3. Rear-Camera Optical Frame (Physical Camera Lens & Optical Axis):
 *    - Camera lens faces out the back of the device (-Z_dev).
 *    - Optical boresight axis: +Z_cam = -z_dev (forward depth into scene).
 *    - Points with Z_cam <= 0 are behind the camera focal plane.
 *    - Sensor orientation angle theta_s = CameraCharacteristics.SENSOR_ORIENTATION (typically 90° clockwise on phones).
 *    - Transformation to Camera Sensor 3D frame:
 *      [ X_sensor ]   [  cos(theta_s) -sin(theta_s) ] [  x_dev ]
 *      [ Y_sensor ] = [ -sin(theta_s) -cos(theta_s) ] [  y_dev ]
 *      Z_sensor = Z_cam = -z_dev
 *
 * 4. Pinhole Intrinsic Projection:
 *    - Uses Camera2 LENS_INTRINSIC_CALIBRATION (fx, fy, cx, cy, skew) or physical sensor geometry.
 *    - x_norm = X_sensor / Z_cam
 *    - y_norm = Y_sensor / Z_cam
 *    - u_sensor = fx * x_norm + skew * y_norm + cx
 *    - v_sensor = fy * y_norm + cy
 *
 * 5. CameraX PreviewView Transformation -> Compose Canvas:
 *    - Primary: CameraX PreviewView.sensorToViewTransform (maps sensor buffer pixels to view bounds,
 *      automatically resolving sensor orientation, window/display rotation, and FILL_CENTER aspect crop).
 *    - Fallback: Exact analytical transformation accounting for net rotation (sensorOrientation - displayRotation)
 *      and FILL_CENTER aspect-fill scaling to Compose Canvas dimensions.
 */
object ARProjectionEngine {

    enum class IntrinsicsSource {
        /** True hardware calibration from CameraCharacteristics.LENS_INTRINSIC_CALIBRATION */
        CALIBRATED_HARDWARE,

        /** Estimated from physical sensor size (SENSOR_INFO_PHYSICAL_SIZE) and focal length */
        ESTIMATED_PHYSICAL_SENSOR,

        /** Clearly documented fallback when device HAL provides no camera metadata */
        FALLBACK_DEFAULT
    }

    data class CameraIntrinsics(
        val fx: Double,
        val fy: Double,
        val cx: Double,
        val cy: Double,
        val skew: Double,
        val activeArrayWidth: Int,
        val activeArrayHeight: Int,
        val sensorOrientation: Int,
        val isLensFacingBack: Boolean,
        val source: IntrinsicsSource
    )

    data class ProjectedPoint(
        val offset: Offset,
        val depth: Double,
        val isVisible: Boolean
    )

    /**
     * Queries CameraManager for the rear camera's intrinsic calibration and sensor geometry.
     */
    fun getCameraIntrinsics(context: Context?): CameraIntrinsics {
        if (context != null) {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                if (cameraManager != null) {
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        val chars = cameraManager.getCameraCharacteristics(id)
                        chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                    } ?: cameraManager.cameraIdList.firstOrNull()

                    if (cameraId != null) {
                        val chars = cameraManager.getCameraCharacteristics(cameraId)
                        val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_BACK
                        val isBack = facing == CameraCharacteristics.LENS_FACING_BACK
                        val sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

                        val activeArray: Rect? = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                            ?: chars.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)

                        val arrayW = activeArray?.width()?.coerceAtLeast(640) ?: 1920
                        val arrayH = activeArray?.height()?.coerceAtLeast(480) ?: 1080

                        // 1. Primary: Hardware intrinsic calibration (API 23+)
                        val intrinsicCal = chars.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                        if (intrinsicCal != null && intrinsicCal.size >= 5 && intrinsicCal[0] > 0f && intrinsicCal[1] > 0f) {
                            return CameraIntrinsics(
                                fx = intrinsicCal[0].toDouble(),
                                fy = intrinsicCal[1].toDouble(),
                                cx = intrinsicCal[2].toDouble(),
                                cy = intrinsicCal[3].toDouble(),
                                skew = intrinsicCal[4].toDouble(),
                                activeArrayWidth = arrayW,
                                activeArrayHeight = arrayH,
                                sensorOrientation = sensorOrientation,
                                isLensFacingBack = isBack,
                                source = IntrinsicsSource.CALIBRATED_HARDWARE
                            )
                        }

                        // 2. Secondary: Physical sensor size & optical focal lengths
                        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                        if (focalLengths != null && focalLengths.isNotEmpty() && sensorSize != null &&
                            sensorSize.width > 0f && sensorSize.height > 0f
                        ) {
                            val fMm = focalLengths[0].toDouble()
                            val sensorWidthMm = sensorSize.width.toDouble()
                            val sensorHeightMm = sensorSize.height.toDouble()

                            val fx = fMm * (arrayW.toDouble() / sensorWidthMm)
                            val fy = fMm * (arrayH.toDouble() / sensorHeightMm)
                            val cx = arrayW.toDouble() / 2.0
                            val cy = arrayH.toDouble() / 2.0

                            return CameraIntrinsics(
                                fx = fx,
                                fy = fy,
                                cx = cx,
                                cy = cy,
                                skew = 0.0,
                                activeArrayWidth = arrayW,
                                activeArrayHeight = arrayH,
                                sensorOrientation = sensorOrientation,
                                isLensFacingBack = isBack,
                                source = IntrinsicsSource.ESTIMATED_PHYSICAL_SENSOR
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to documented default
            }
        }

        // 3. Documented Fallback Model (Identified clearly as FALLBACK_DEFAULT)
        val defaultArrayW = 1920
        val defaultArrayH = 1080
        val fallbackFovYRad = Math.toRadians(63.5)
        val fallbackFy = (defaultArrayH / 2.0) / tan(fallbackFovYRad / 2.0)
        val fallbackFx = fallbackFy

        return CameraIntrinsics(
            fx = fallbackFx,
            fy = fallbackFy,
            cx = defaultArrayW / 2.0,
            cy = defaultArrayH / 2.0,
            skew = 0.0,
            activeArrayWidth = defaultArrayW,
            activeArrayHeight = defaultArrayH,
            sensorOrientation = 90,
            isLensFacingBack = true,
            source = IntrinsicsSource.FALLBACK_DEFAULT
        )
    }

    /**
     * Projects a celestial coordinate (Azimuth, Altitude) to Compose Canvas pixel coordinates
     * using the full camera intrinsics, sensor-to-camera coordinate transformation, and CameraX pipeline.
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
        intrinsics: CameraIntrinsics,
        zoomFactor: Float = 1.0f,
        sensorToViewMatrix: Matrix? = null,
        displayRotationDegrees: Int = 0
    ): Offset? {
        val azRad = Math.toRadians(azimuthDeg)
        val altRad = Math.toRadians(altitudeDeg)

        // Step 1: Celestial unit vector in World frame (East = +X, North = +Y, Up = +Z)
        val ox = cos(altRad) * sin(azRad)
        val oy = cos(altRad) * cos(azRad)
        val oz = sin(altRad)

        // Step 2: Transform into Device coordinate frame (+X_dev Right, +Y_dev Up, +Z_dev Front/User)
        // v_dev = R_true^T * v_world
        val xDev: Double
        val yDev: Double
        val zDev: Double

        if (rotationMatrix != null && rotationMatrix.size == 9 &&
            (rotationMatrix[0] != 0f || rotationMatrix[1] != 0f || rotationMatrix[2] != 0f)
        ) {
            // Transpose of rotationMatrix (Row i is Column i of R)
            xDev = ox * rotationMatrix[0] + oy * rotationMatrix[3] + oz * rotationMatrix[6]
            yDev = ox * rotationMatrix[1] + oy * rotationMatrix[4] + oz * rotationMatrix[7]
            zDev = ox * rotationMatrix[2] + oy * rotationMatrix[5] + oz * rotationMatrix[8]
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

            xDev = ox * rx + oy * ry + oz * rz
            yDev = ox * ux + oy * uy + oz * uz
            zDev = ox * fx + oy * fy + oz * fz
        }

        // Step 3: Rear-Camera Optical Frame (+Z_cam pointing out of the rear camera into the world)
        val zCam = -zDev
        if (zCam <= 0.001) return null // Object is behind the camera plane

        // Map device transverse coordinates (xDev, yDev) into camera sensor active array coordinates
        // using SENSOR_ORIENTATION theta_s (the clockwise rotation from sensor frame to device frame).
        // Device frame: +X_dev is Screen Right, +Y_dev is Screen Up, +Z_dev is towards user (+Z_cam = -zDev into scene).
        // Sensor frame: +X_sensor is columns (right on sensor), +Y_sensor is rows (downwards on sensor).
        // At theta_s = 0: X_sensor = +xDev, Y_sensor = -yDev
        // At theta_s = 90: X_sensor = -yDev, Y_sensor = -xDev
        // At theta_s = 180: X_sensor = -xDev, Y_sensor = +yDev
        // At theta_s = 270: X_sensor = +yDev, Y_sensor = +xDev
        val thetaRad = Math.toRadians(intrinsics.sensorOrientation.toDouble())
        val cosT = cos(thetaRad)
        val sinT = sin(thetaRad)

        // Rigorous SO(2) mapping from device axes to sensor image active array axes:
        val xSensor = xDev * cosT - yDev * sinT
        val ySensor = -xDev * sinT - yDev * cosT

        // Step 4: Camera Intrinsic Projection (Pinhole model with fx, fy, cx, cy, skew)
        val xNorm = xSensor / zCam
        val yNorm = ySensor / zCam

        val uSensor = intrinsics.fx * xNorm + intrinsics.skew * yNorm + intrinsics.cx
        val vSensor = intrinsics.fy * yNorm + intrinsics.cy

        // Step 5: CameraX PreviewView Transformation -> Compose Canvas
        // If CameraX provided a live sensorToView transformation matrix, use it directly
        if (sensorToViewMatrix != null) {
            val pts = floatArrayOf(uSensor.toFloat(), vSensor.toFloat())
            sensorToViewMatrix.mapPoints(pts)

            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            val px = centerX + (pts[0] - centerX) * zoomFactor
            val py = centerY + (pts[1] - centerY) * zoomFactor

            if (!px.isFinite() || !py.isFinite()) return null
            return Offset(px, py)
        }

        // Analytical transformation matching CameraX PreviewView.ScaleType.FILL_CENTER
        val arrayW = intrinsics.activeArrayWidth.toDouble()
        val arrayH = intrinsics.activeArrayHeight.toDouble()

        val netRotation = (intrinsics.sensorOrientation - displayRotationDegrees + 360) % 360

        val uRot: Double
        val vRot: Double
        val wRot: Double
        val hRot: Double

        when (netRotation) {
            90 -> {
                uRot = arrayH - vSensor
                vRot = uSensor
                wRot = arrayH
                hRot = arrayW
            }
            270 -> {
                uRot = vSensor
                vRot = arrayW - uSensor
                wRot = arrayH
                hRot = arrayW
            }
            180 -> {
                uRot = arrayW - uSensor
                vRot = arrayH - vSensor
                wRot = arrayW
                hRot = arrayH
            }
            else -> {
                uRot = uSensor
                vRot = vSensor
                wRot = arrayW
                hRot = arrayH
            }
        }

        val scale = max(canvasWidth / wRot, canvasHeight / hRot) * zoomFactor
        val px = (canvasWidth / 2.0 + (uRot - wRot / 2.0) * scale).toFloat()
        val py = (canvasHeight / 2.0 + (vRot - hRot / 2.0) * scale).toFloat()

        if (!px.isFinite() || !py.isFinite()) return null
        return Offset(px, py)
    }

    /**
     * Backward-compatible overload with focalLengthPx.
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
        val defaultIntrinsics = CameraIntrinsics(
            fx = focalLengthPx.toDouble(),
            fy = focalLengthPx.toDouble(),
            cx = canvasWidth.toDouble() / 2.0,
            cy = canvasHeight.toDouble() / 2.0,
            skew = 0.0,
            activeArrayWidth = canvasWidth.toInt().coerceAtLeast(1),
            activeArrayHeight = canvasHeight.toInt().coerceAtLeast(1),
            sensorOrientation = 0,
            isLensFacingBack = true,
            source = IntrinsicsSource.FALLBACK_DEFAULT
        )
        return projectAltAz(
            azimuthDeg = azimuthDeg,
            altitudeDeg = altitudeDeg,
            rotationMatrix = rotationMatrix,
            currentAzimuth = currentAzimuth,
            currentAltitude = currentAltitude,
            currentRoll = currentRoll,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            intrinsics = defaultIntrinsics,
            zoomFactor = 1.0f,
            sensorToViewMatrix = null,
            displayRotationDegrees = 0
        )
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

    /**
     * Computes the effective camera focal length in screen pixels using CameraIntrinsics.
     */
    fun computeCameraFocalLengthPx(
        context: Context?,
        screenWidthPx: Float,
        screenHeightPx: Float,
        zoomFactor: Float = 1.0f
    ): Float {
        val intrinsics = getCameraIntrinsics(context)
        val wRot = if (intrinsics.sensorOrientation == 90 || intrinsics.sensorOrientation == 270) {
            intrinsics.activeArrayHeight.toDouble()
        } else {
            intrinsics.activeArrayWidth.toDouble()
        }
        val hRot = if (intrinsics.sensorOrientation == 90 || intrinsics.sensorOrientation == 270) {
            intrinsics.activeArrayWidth.toDouble()
        } else {
            intrinsics.activeArrayHeight.toDouble()
        }
        val scale = max(screenWidthPx / wRot, screenHeightPx / hRot)
        val baseFocalLengthPx = (intrinsics.fy * scale).toFloat()
        return baseFocalLengthPx * zoomFactor
    }

    /**
     * Computes effective horizontal field of view in degrees matching camera intrinsics and canvas size.
     */
    fun computeEffectiveFovXDeg(
        screenWidthPx: Float,
        focalLengthPx: Float
    ): Double {
        if (focalLengthPx <= 0f) return 55.0
        val halfW = screenWidthPx / 2.0
        return Math.toDegrees(2.0 * atan(halfW / focalLengthPx))
    }

    fun computeEffectiveFovXDeg(
        screenWidthPx: Float,
        screenHeightPx: Float,
        intrinsics: CameraIntrinsics,
        zoomFactor: Float = 1.0f
    ): Double {
        val focalLengthPx = computeCameraFocalLengthPx(
            context = null,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            zoomFactor = zoomFactor
        )
        return computeEffectiveFovXDeg(screenWidthPx, focalLengthPx)
    }
}
