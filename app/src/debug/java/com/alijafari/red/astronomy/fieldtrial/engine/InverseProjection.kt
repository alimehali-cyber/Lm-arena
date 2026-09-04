package com.alijafari.red.astronomy.fieldtrial.engine

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * G-2.1: screen (x,y) -> sky (az, alt), inverting EXACTLY the forward math of
 * ARProjectionEngine.projectAltAz (both attitude branches; both sensor->view paths).
 *
 * This file is a faithful pure-Kotlin replica of that forward math (kept in lockstep
 * by InverseProjectionTest's cross-check against the real engine in CI — any constant
 * drift fails the test) plus the analytic inverse. No Android imports.
 *
 * Frames (identical to the engine):
 *  world  = East +X, North +Y, Up +Z;  az/alt the usual.
 *  device = screen right +X, up +Y, toward user +Z (rear camera looks along -Z_dev).
 *  sensor = array columns +X, rows down +Y; rotation from device by SENSOR_ORIENTATION.
 */
object InverseProjection {

    /** Mirror of ARProjectionEngine.CameraIntrinsics (pure). */
    data class Intrinsics(
        val fx: Double,
        val fy: Double,
        val cx: Double,
        val cy: Double,
        val skew: Double,
        val activeArrayWidth: Int,
        val activeArrayHeight: Int,
        val sensorOrientation: Int
    ) {
        companion object {
            /** The 63.5-deg fallback tier expressed at array scale. */
            fun fallbackTier(arrayW: Int, arrayH: Int): Intrinsics {
                // ARProjectionEngine derives fx from the vertical FOV: fy = (H/2)/tan(fovY/2)
                val fy = arrayH / 2.0 / tanDeg(63.5 / 2.0)
                return Intrinsics(
                    fx = fy, fy = fy,
                    cx = arrayW / 2.0, cy = arrayH / 2.0,
                    skew = 0.0,
                    activeArrayWidth = arrayW, activeArrayHeight = arrayH,
                    sensorOrientation = 90
                )
            }
        }
    }

    /**
     * Forward replica: (azDeg, altDeg) -> canvas (px, py); null when behind the camera.
     * Analytical FILL_CENTER path (sensorToViewMatrix == null) or the affine path
     * (a 3x3 row-major affine, the values of the engine's android.graphics.Matrix).
     */
    fun forwardProject(
        azimuthDeg: Double,
        altitudeDeg: Double,
        rotationMatrix: FloatArray?,
        currentAzimuth: Double,
        currentAltitude: Double,
        currentRoll: Double,
        canvasWidth: Double,
        canvasHeight: Double,
        intrinsics: Intrinsics,
        zoomFactor: Double = 1.0,
        affine: DoubleArray? = null,
        displayRotationDegrees: Int = 0
    ): Pair<Double, Double>? {
        val vWorld = worldVector(azimuthDeg, altitudeDeg)
        val vDev = worldToDevice(vWorld, rotationMatrix, currentAzimuth, currentAltitude, currentRoll)
        val zCam = -vDev[2]
        if (zCam <= 0.001) return null

        val thetaRad = Math.toRadians(intrinsics.sensorOrientation.toDouble())
        val cosT = cos(thetaRad); val sinT = sin(thetaRad)
        val xSensor = vDev[0] * cosT - vDev[1] * sinT
        val ySensor = -vDev[0] * sinT - vDev[1] * cosT

        val xNorm = xSensor / zCam
        val yNorm = ySensor / zCam
        val uSensor = intrinsics.fx * xNorm + intrinsics.skew * yNorm + intrinsics.cx
        val vSensor = intrinsics.fy * yNorm + intrinsics.cy

        if (affine != null) {
            val m0 = affine[0]; val m1 = affine[1]; val m2 = affine[2]
            val m3 = affine[3]; val m4 = affine[4]; val m5 = affine[5]
            val ax = m0 * uSensor + m1 * vSensor + m2
            val ay = m3 * uSensor + m4 * vSensor + m5
            val cx = canvasWidth / 2.0
            val cy = canvasHeight / 2.0
            val px = cx + (ax - cx) * zoomFactor
            val py = cy + (ay - cy) * zoomFactor
            if (!px.isFinite() || !py.isFinite()) return null
            return Pair(px, py)
        }

        val arrayW = intrinsics.activeArrayWidth.toDouble()
        val arrayH = intrinsics.activeArrayHeight.toDouble()
        val netRotation = (intrinsics.sensorOrientation - displayRotationDegrees + 360) % 360
        val uRot: Double; val vRot: Double; val wRot: Double; val hRot: Double
        when (netRotation) {
            90 -> { uRot = arrayH - vSensor; vRot = uSensor; wRot = arrayH; hRot = arrayW }
            270 -> { uRot = vSensor; vRot = arrayW - uSensor; wRot = arrayH; hRot = arrayW }
            180 -> { uRot = arrayW - uSensor; vRot = arrayH - vSensor; wRot = arrayW; hRot = arrayH }
            else -> { uRot = uSensor; vRot = vSensor; wRot = arrayW; hRot = arrayH }
        }
        val scale = max(canvasWidth / wRot, canvasHeight / hRot) * zoomFactor
        val px = canvasWidth / 2.0 + (uRot - wRot / 2.0) * scale
        val py = canvasHeight / 2.0 + (vRot - hRot / 2.0) * scale
        if (!px.isFinite() || !py.isFinite()) return null
        return Pair(px, py)
    }

    /**
     * Inverse: canvas (px, py) -> (azDeg, altDeg) for the SAME inputs; null when the
     * point is not invertible (degenerate affine / non-finite / behind camera).
     */
    fun inverseProject(
        px: Double,
        py: Double,
        rotationMatrix: FloatArray?,
        currentAzimuth: Double,
        currentAltitude: Double,
        currentRoll: Double,
        canvasWidth: Double,
        canvasHeight: Double,
        intrinsics: Intrinsics,
        zoomFactor: Double = 1.0,
        affine: DoubleArray? = null,
        displayRotationDegrees: Int = 0
    ): Pair<Double, Double>? {
        if (!px.isFinite() || !py.isFinite()) return null

        // canvas -> (uSensor, vSensor)
        val uSensor: Double; val vSensor: Double
        if (affine != null) {
            if (zoomFactor == 0.0) return null
            val cx = canvasWidth / 2.0
            val cy = canvasHeight / 2.0
            val ax = (px - cx) / zoomFactor + cx
            val ay = (py - cy) / zoomFactor + cy
            // invert the 3x3 affine [m0 m1 m2; m3 m4 m5; 0 0 1]
            val det = affine[0] * affine[4] - affine[1] * affine[3]
            if (!det.isFinite() || kotlin.math.abs(det) < 1e-12) return null
            val i0 = affine[4] / det; val i1 = -affine[1] / det
            val i3 = -affine[3] / det; val i4 = affine[0] / det
            uSensor = i0 * (ax - affine[2]) + i1 * (ay - affine[5])
            vSensor = i3 * (ax - affine[2]) + i4 * (ay - affine[5])
        } else {
            val arrayW = intrinsics.activeArrayWidth.toDouble()
            val arrayH = intrinsics.activeArrayHeight.toDouble()
            val netRotation = (intrinsics.sensorOrientation - displayRotationDegrees + 360) % 360
            val wRot: Double; val hRot: Double
            when (netRotation) {
                90 -> { wRot = arrayH; hRot = arrayW }
                270 -> { wRot = arrayH; hRot = arrayW }
                180 -> { wRot = arrayW; hRot = arrayH }
                else -> { wRot = arrayW; hRot = arrayH }
            }
            val scale = max(canvasWidth / wRot, canvasHeight / hRot) * zoomFactor
            if (scale == 0.0) return null
            val uRot = (px - canvasWidth / 2.0) / scale + wRot / 2.0
            val vRot = (py - canvasHeight / 2.0) / scale + hRot / 2.0
            when (netRotation) {
                90 -> { uSensor = vRot; vSensor = arrayH - uRot }
                270 -> { uSensor = arrayW - vRot; vSensor = uRot }
                180 -> { uSensor = arrayW - uRot; vSensor = arrayH - vRot }
                else -> { uSensor = uRot; vSensor = vRot }
            }
        }

        // pinhole -> sensor direction (zCam = 1)
        if (intrinsics.fx == 0.0 || intrinsics.fy == 0.0) return null
        val yNorm = (vSensor - intrinsics.cy) / intrinsics.fy
        val xNorm = (uSensor - intrinsics.cx - intrinsics.skew * yNorm) / intrinsics.fx
        if (!xNorm.isFinite() || !yNorm.isFinite()) return null

        // sensor -> device (theta rotation inverse; mapping matrix is its own inverse)
        val thetaRad = Math.toRadians(intrinsics.sensorOrientation.toDouble())
        val cosT = cos(thetaRad); val sinT = sin(thetaRad)
        val xSensor = xNorm
        val ySensor = yNorm
        val xDev = cosT * xSensor - sinT * ySensor
        val yDev = -sinT * xSensor - cosT * ySensor
        val zDev = -1.0

        // device -> world
        val vWorld = deviceToWorld(doubleArrayOf(xDev, yDev, zDev), rotationMatrix, currentAzimuth, currentAltitude, currentRoll)
        val n = sqrt(vWorld[0] * vWorld[0] + vWorld[1] * vWorld[1] + vWorld[2] * vWorld[2])
        if (n == 0.0 || !n.isFinite()) return null
        val ox = vWorld[0] / n; val oy = vWorld[1] / n; val oz = vWorld[2] / n
        val az = ((Math.toDegrees(atan2(ox, oy)) % 360.0) + 360.0) % 360.0
        val alt = Math.toDegrees(asin(oz.coerceIn(-1.0, 1.0)))
        return Pair(az, alt)
    }

    // ---- shared frame helpers (mirror of the engine's Step 1/2) ----

    internal fun worldVector(azimuthDeg: Double, altitudeDeg: Double): DoubleArray {
        val azRad = Math.toRadians(azimuthDeg)
        val altRad = Math.toRadians(altitudeDeg)
        return doubleArrayOf(
            cos(altRad) * sin(azRad),
            cos(altRad) * cos(azRad),
            sin(altRad)
        )
    }

    internal fun worldToDevice(
        v: DoubleArray,
        rotationMatrix: FloatArray?,
        currentAzimuth: Double,
        currentAltitude: Double,
        currentRoll: Double
    ): DoubleArray {
        if (rotationMatrix != null && rotationMatrix.size == 9 &&
            (rotationMatrix[0] != 0f || rotationMatrix[1] != 0f || rotationMatrix[2] != 0f)
        ) {
            return doubleArrayOf(
                v[0] * rotationMatrix[0] + v[1] * rotationMatrix[3] + v[2] * rotationMatrix[6],
                v[0] * rotationMatrix[1] + v[1] * rotationMatrix[4] + v[2] * rotationMatrix[7],
                v[0] * rotationMatrix[2] + v[1] * rotationMatrix[5] + v[2] * rotationMatrix[8]
            )
        }
        val (r, u, f) = virtualBasis(currentAzimuth, currentAltitude, currentRoll)
        return doubleArrayOf(
            v[0] * r[0] + v[1] * r[1] + v[2] * r[2],
            v[0] * u[0] + v[1] * u[1] + v[2] * u[2],
            v[0] * f[0] + v[1] * f[1] + v[2] * f[2]
        )
    }

    internal fun deviceToWorld(
        v: DoubleArray,
        rotationMatrix: FloatArray?,
        currentAzimuth: Double,
        currentAltitude: Double,
        currentRoll: Double
    ): DoubleArray {
        if (rotationMatrix != null && rotationMatrix.size == 9 &&
            (rotationMatrix[0] != 0f || rotationMatrix[1] != 0f || rotationMatrix[2] != 0f)
        ) {
            // v_dev = R^T v_world  =>  v_world = R v_dev (R rows)
            return doubleArrayOf(
                rotationMatrix[0] * v[0] + rotationMatrix[1] * v[1] + rotationMatrix[2] * v[2],
                rotationMatrix[3] * v[0] + rotationMatrix[4] * v[1] + rotationMatrix[5] * v[2],
                rotationMatrix[6] * v[0] + rotationMatrix[7] * v[1] + rotationMatrix[8] * v[2]
            )
        }
        val (r, u, f) = virtualBasis(currentAzimuth, currentAltitude, currentRoll)
        return doubleArrayOf(
            v[0] * r[0] + v[1] * u[0] + v[2] * f[0],
            v[0] * r[1] + v[1] * u[1] + v[2] * f[1],
            v[0] * r[2] + v[1] * u[2] + v[2] * f[2]
        )
    }

    /** The virtual-camera basis rows (r, u, f) exactly as projectAltAz builds them. */
    private fun virtualBasis(cAz: Double, cAlt: Double, cRoll: Double): Triple<DoubleArray, DoubleArray, DoubleArray> {
        val cAzRad = Math.toRadians(cAz)
        val cAltRad = Math.toRadians(cAlt)
        val cRollRad = Math.toRadians(cRoll)

        val px = cos(cAltRad) * sin(cAzRad)
        val py = cos(cAltRad) * cos(cAzRad)
        val pz = sin(cAltRad)

        val rx0 = cos(cAzRad); val ry0 = -sin(cAzRad); val rz0 = 0.0
        val ux0 = -sin(cAltRad) * sin(cAzRad)
        val uy0 = -sin(cAltRad) * cos(cAzRad)
        val uz0 = cos(cAltRad)

        val cosR = cos(cRollRad); val sinR = sin(cRollRad)
        val r = doubleArrayOf(rx0 * cosR - ux0 * sinR, ry0 * cosR - uy0 * sinR, rz0 * cosR - uz0 * sinR)
        val u = doubleArrayOf(rx0 * sinR + ux0 * cosR, ry0 * sinR + uy0 * cosR, rz0 * sinR + uz0 * cosR)
        val f = doubleArrayOf(-px, -py, -pz)
        return Triple(r, u, f)
    }

    private fun tanDeg(deg: Double): Double = kotlin.math.tan(Math.toRadians(deg))
}
