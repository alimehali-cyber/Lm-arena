package com.alijafari.red.astronomy.fieldtrial.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * G-2.2: one tap measurement with everything needed to re-derive it offline.
 * Pure Kotlin (harness + CI tested).
 */
data class TapMeasurement(
    val epochMs: Long,
    val targetId: String,
    val computedAzDeg: Double,
    val computedAltDeg: Double,
    val tappedAzDeg: Double,
    val tappedAltDeg: Double,
    /** signed, wrapped to (-180, 180]: tapped - computed */
    val dAzDeg: Double,
    val dAltDeg: Double,
    /** great-circle on-sky separation, deg */
    val separationDeg: Double,
    /** |tap point - target marker| on the canvas, px */
    val screenOffsetPx: Double,
    // ---- full context (2.2): everything re-derivable offline ----
    val sensorAzimuthDeg: Double,
    val sensorAltitudeDeg: Double,
    val sensorRollDeg: Double,
    /** sensor attitude as a quaternion derived from the same world basis (w,x,y,z) */
    val sensorQuaternion: DoubleArray,
    val sensorRotationMatrix: FloatArray?,
    val gpsLat: Double?,
    val gpsLon: Double?,
    val gpsAccuracyM: Double?,
    val intrinsicsTier: String,
    val fx: Double?, val fy: Double?, val cx: Double?, val cy: Double?,
    val distortionTier: String,
    val k1: Double?, val k2: Double?,
    val appliedDeclinationDeg: Double,
    val zoomFactor: Double,
    val displayRotationDegrees: Int
) {
    // Data-class generated equals/hashCode use IDENTITY for the two array fields
    // (sensorQuaternion, sensorRotationMatrix) — restore-from-JSON would never compare
    // equal. Explicit content-based overrides fix that (harness-pinned by the machine test).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TapMeasurement) return false
        return sensorQuaternion.contentEquals(other.sensorQuaternion) &&
            (sensorRotationMatrix == null && other.sensorRotationMatrix == null ||
                sensorRotationMatrix != null && other.sensorRotationMatrix != null &&
                sensorRotationMatrix.contentEquals(other.sensorRotationMatrix)) &&
            epochMs == other.epochMs && targetId == other.targetId &&
            computedAzDeg == other.computedAzDeg && computedAltDeg == other.computedAltDeg &&
            tappedAzDeg == other.tappedAzDeg && tappedAltDeg == other.tappedAltDeg &&
            dAzDeg == other.dAzDeg && dAltDeg == other.dAltDeg &&
            separationDeg == other.separationDeg && screenOffsetPx == other.screenOffsetPx &&
            sensorAzimuthDeg == other.sensorAzimuthDeg && sensorAltitudeDeg == other.sensorAltitudeDeg &&
            sensorRollDeg == other.sensorRollDeg &&
            gpsLat == other.gpsLat && gpsLon == other.gpsLon && gpsAccuracyM == other.gpsAccuracyM &&
            intrinsicsTier == other.intrinsicsTier &&
            fx == other.fx && fy == other.fy && cx == other.cx && cy == other.cy &&
            distortionTier == other.distortionTier && k1 == other.k1 && k2 == other.k2 &&
            appliedDeclinationDeg == other.appliedDeclinationDeg &&
            zoomFactor == other.zoomFactor && displayRotationDegrees == other.displayRotationDegrees
    }

    override fun hashCode(): Int {
        var h = epochMs.hashCode()
        h = 31 * h + targetId.hashCode()
        h = 31 * h + sensorQuaternion.contentHashCode()
        h = 31 * h + (sensorRotationMatrix?.contentHashCode() ?: 0)
        return h
    }

    companion object {
        /** Signed wrap to (-180, 180]. */
        fun wrap180(d: Double): Double {
            var x = (d + 180.0) % 360.0
            if (x < 0) x += 360.0
            return x - 180.0
        }

        /** Great-circle separation between two az/alt points, degrees. */
        fun separationDeg(az1: Double, alt1: Double, az2: Double, alt2: Double): Double {
            val a1 = Math.toRadians(az1); val h1 = Math.toRadians(alt1)
            val a2 = Math.toRadians(az2); val h2 = Math.toRadians(alt2)
            val cosD = sin(h1) * sin(h2) + cos(h1) * cos(h2) * cos(a1 - a2)
            return Math.toDegrees(acos(cosD.coerceIn(-1.0, 1.0)))
        }

        /**
         * Build the measurement. Canvas points: the drawn target marker (px, py) and
         * the confirmed tap (tx, ty) — used only for the px offset, sky positions come
         * from the inverse projection at the SAME sensor sample.
         */
        fun of(
            epochMs: Long,
            targetId: String,
            computedAzDeg: Double, computedAltDeg: Double,
            tappedAzDeg: Double, tappedAltDeg: Double,
            targetPx: Double, targetPy: Double,
            tapPx: Double, tapPy: Double,
            sensorAzimuthDeg: Double, sensorAltitudeDeg: Double, sensorRollDeg: Double,
            sensorRotationMatrix: FloatArray?,
            gpsLat: Double?, gpsLon: Double?, gpsAccuracyM: Double?,
            intrinsicsTier: String,
            fx: Double?, fy: Double?, cx: Double?, cy: Double?,
            distortionTier: String, k1: Double?, k2: Double?,
            appliedDeclinationDeg: Double,
            zoomFactor: Double,
            displayRotationDegrees: Int
        ): TapMeasurement {
            val dAz = wrap180(tappedAzDeg - computedAzDeg)
            val dAlt = tappedAltDeg - computedAltDeg
            val dx = tapPx - targetPx
            val dy = tapPy - targetPy
            return TapMeasurement(
                epochMs = epochMs, targetId = targetId,
                computedAzDeg = computedAzDeg, computedAltDeg = computedAltDeg,
                tappedAzDeg = tappedAzDeg, tappedAltDeg = tappedAltDeg,
                dAzDeg = dAz, dAltDeg = dAlt,
                separationDeg = separationDeg(computedAzDeg, computedAltDeg, tappedAzDeg, tappedAltDeg),
                screenOffsetPx = sqrt(dx * dx + dy * dy),
                sensorAzimuthDeg = sensorAzimuthDeg, sensorAltitudeDeg = sensorAltitudeDeg,
                sensorRollDeg = sensorRollDeg,
                sensorQuaternion = quaternionOf(sensorAzimuthDeg, sensorAltitudeDeg, sensorRollDeg),
                sensorRotationMatrix = sensorRotationMatrix?.copyOf(),
                gpsLat = gpsLat, gpsLon = gpsLon, gpsAccuracyM = gpsAccuracyM,
                intrinsicsTier = intrinsicsTier, fx = fx, fy = fy, cx = cx, cy = cy,
                distortionTier = distortionTier, k1 = k1, k2 = k2,
                appliedDeclinationDeg = appliedDeclinationDeg,
                zoomFactor = zoomFactor, displayRotationDegrees = displayRotationDegrees
            )
        }

        /**
         * Attitude quaternion for the world<-camera basis used by the projection:
         * device axes in world = (r, u, -p) where p is the boresight (az, alt) and
         * roll rotates (r,u) in their plane. Rotation that takes the camera frame
         * (x=right, y=up, z=boresight) into the world frame.
         */
        fun quaternionOf(azDeg: Double, altDeg: Double, rollDeg: Double): DoubleArray {
            val az = Math.toRadians(azDeg); val alt = Math.toRadians(altDeg); val roll = Math.toRadians(rollDeg)
            // camera axes in world
            val boresight = doubleArrayOf(cos(alt) * sin(az), cos(alt) * cos(az), sin(alt)) // +Z_cam
            val rightRef = doubleArrayOf(cos(az), -sin(az), 0.0)
            val upRef = cross(boresight, rightRef) // right-handed: up = z × x
            val cr = cos(roll); val sr = sin(roll)
            val right = add(scale(rightRef, cr), scale(upRef, sr))
            val up = add(scale(rightRef, -sr), scale(upRef, cr))
            // R columns = (right, up, boresight) maps camera->world; quaternion from matrix
            return quaternionFromMatrix(right, up, boresight)
        }

        private fun quaternionFromMatrix(x: DoubleArray, y: DoubleArray, z: DoubleArray): DoubleArray {
            val m00 = x[0]; val m10 = x[1]; val m20 = x[2]
            val m01 = y[0]; val m11 = y[1]; val m21 = y[2]
            val m02 = z[0]; val m12 = z[1]; val m22 = z[2]
            val trace = m00 + m11 + m22
            return if (trace > 0) {
                val s = sqrt(trace + 1.0) * 2
                doubleArrayOf(0.25 * s, (m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s)
            } else if (m00 > m11 && m00 > m22) {
                val s = sqrt(1.0 + m00 - m11 - m22) * 2
                doubleArrayOf((m21 - m12) / s, 0.25 * s, (m01 + m10) / s, (m02 + m20) / s)
            } else if (m11 > m22) {
                val s = sqrt(1.0 + m11 - m00 - m22) * 2
                doubleArrayOf((m02 - m20) / s, (m01 + m10) / s, 0.25 * s, (m12 + m21) / s)
            } else {
                val s = sqrt(1.0 + m22 - m00 - m11) * 2
                doubleArrayOf((m10 - m01) / s, (m02 + m20) / s, (m12 + m21) / s, 0.25 * s)
            }
        }

        private fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(
            a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]
        )

        private fun add(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])

        private fun scale(a: DoubleArray, k: Double) = doubleArrayOf(a[0] * k, a[1] * k, a[2] * k)
    }
}

/**
 * G-2.3: Level-1 (Sun) plain-English auto-diagnosis. D = applied declination.
 */
object SunDiagnosis {
    const val MISSING = "It looks like the compass correction is missing."
    const val TWICE = "It looks like the compass correction was applied twice."
    const val TILT = "Phone level/tilt looks off."
    const val OPPOSITE = "It looks like the compass is pointing the opposite way."

    fun diagnose(dAzDeg: Double, dAltDeg: Double, declinationDeg: Double): String? {
        val d = TapMeasurement.wrap180(dAzDeg)
        val dd = TapMeasurement.wrap180(declinationDeg)
        if (abs(TapMeasurement.wrap180(d - dd)) < 1.5) return MISSING
        if (abs(TapMeasurement.wrap180(d + dd)) < 1.5) return TWICE
        if (abs(TapMeasurement.wrap180(d - 180.0)) < 1.5 || abs(TapMeasurement.wrap180(d + 180.0)) < 1.5) return OPPOSITE
        if (abs(dAltDeg) > 2 && abs(d) < 1.5) return TILT
        return null
    }
}
