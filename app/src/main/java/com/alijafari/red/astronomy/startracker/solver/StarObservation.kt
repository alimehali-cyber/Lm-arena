package com.alijafari.red.astronomy.startracker.solver

/**
 * Minimal input type for solver, decoupled from Phase 2's DetectedStar.
 * Defined as unit vector in camera frame, plus flux and flags.
 * This keeps solver independently testable with synthetic inputs.
 *
 * @param unitVectorCamera unit vector in camera frame (x,y,z), normalized
 * @param flux estimated flux (brightness)
 * @param isSaturated flag from detection
 * @param id optional observation id (e.g., "OBS001")
 */
data class StarObservation(
    val unitVectorCamera: Triple<Double, Double, Double>,
    val flux: Double = 1.0,
    val isSaturated: Boolean = false,
    val id: String = ""
) {
    val x: Double get() = unitVectorCamera.first
    val y: Double get() = unitVectorCamera.second
    val z: Double get() = unitVectorCamera.third
}

/**
 * Quaternion for attitude representation.
 * Convention: w + xi + yj + zk, where w is scalar part, (x,y,z) vector part.
 * Unit quaternion: w^2 + x^2 + y^2 + z^2 = 1
 * Rotation: v' = q * v * q_conj, where v is pure quaternion (0, vx, vy, vz)
 */
data class Quaternion(
    val w: Double,
    val x: Double,
    val y: Double,
    val z: Double
) {
    fun normalized(): Quaternion {
        val norm = kotlin.math.sqrt(w*w + x*x + y*y + z*z)
        if (norm < 1e-12) return Quaternion(1.0, 0.0, 0.0, 0.0)
        return Quaternion(w/norm, x/norm, y/norm, z/norm)
    }

    fun conjugate(): Quaternion = Quaternion(w, -x, -y, -z)

    fun multiply(other: Quaternion): Quaternion {
        // Hamilton product: q1 * q2
        return Quaternion(
            w = w*other.w - x*other.x - y*other.y - z*other.z,
            x = w*other.x + x*other.w + y*other.z - z*other.y,
            y = w*other.y - x*other.z + y*other.w + z*other.x,
            z = w*other.z + x*other.y - y*other.x + z*other.w
        )
    }

    /** Rotate vector by this quaternion: v' = q * v * q_conj */
    fun rotateVector(v: Triple<Double, Double, Double>): Triple<Double, Double, Double> {
        val (vx, vy, vz) = v
        val qv = Quaternion(0.0, vx, vy, vz)
        val result = this.multiply(qv).multiply(this.conjugate())
        return Triple(result.x, result.y, result.z)
    }

    /** Inverse rotation: rotate by conjugate */
    fun inverseRotateVector(v: Triple<Double, Double, Double>): Triple<Double, Double, Double> {
        return this.conjugate().rotateVector(v)
    }

    /** Convert to rotation matrix 3x3 (row-major) */
    fun toRotationMatrix(): Array<DoubleArray> {
        val ww = w*w
        val xx = x*x
        val yy = y*y
        val zz = z*z
        val wx = w*x
        val wy = w*y
        val wz = w*z
        val xy = x*y
        val xz = x*z
        val yz = y*z

        return arrayOf(
            doubleArrayOf(ww + xx - yy - zz, 2*(xy - wz), 2*(xz + wy)),
            doubleArrayOf(2*(xy + wz), ww - xx + yy - zz, 2*(yz - wx)),
            doubleArrayOf(2*(xz - wy), 2*(yz + wx), ww - xx - yy + zz)
        )
    }

    companion object {
        fun identity(): Quaternion = Quaternion(1.0, 0.0, 0.0, 0.0)

        /** From axis-angle: axis unit vector, angle in radians */
        fun fromAxisAngle(axis: Triple<Double, Double, Double>, angleRad: Double): Quaternion {
            val (ax, ay, az) = axis
            val half = angleRad / 2.0
            val sinHalf = kotlin.math.sin(half)
            val cosHalf = kotlin.math.cos(half)
            return Quaternion(cosHalf, ax*sinHalf, ay*sinHalf, az*sinHalf).normalized()
        }

        /** From rotation matrix (3x3) to quaternion — Shepperd's method */
        fun fromRotationMatrix(m: Array<DoubleArray>): Quaternion {
            val trace = m[0][0] + m[1][1] + m[2][2]
            return if (trace > 0) {
                val s = kotlin.math.sqrt(trace + 1.0) * 2
                Quaternion(
                    w = 0.25 * s,
                    x = (m[2][1] - m[1][2]) / s,
                    y = (m[0][2] - m[2][0]) / s,
                    z = (m[1][0] - m[0][1]) / s
                ).normalized()
            } else if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
                val s = kotlin.math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]) * 2
                Quaternion(
                    w = (m[2][1] - m[1][2]) / s,
                    x = 0.25 * s,
                    y = (m[0][1] + m[1][0]) / s,
                    z = (m[0][2] + m[2][0]) / s
                ).normalized()
            } else if (m[1][1] > m[2][2]) {
                val s = kotlin.math.sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]) * 2
                Quaternion(
                    w = (m[0][2] - m[2][0]) / s,
                    x = (m[0][1] + m[1][0]) / s,
                    y = 0.25 * s,
                    z = (m[1][2] + m[2][1]) / s
                ).normalized()
            } else {
                val s = kotlin.math.sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]) * 2
                Quaternion(
                    w = (m[1][0] - m[0][1]) / s,
                    x = (m[0][2] + m[2][0]) / s,
                    y = (m[1][2] + m[2][1]) / s,
                    z = 0.25 * s
                ).normalized()
            }
        }
    }
}

/** Vec3 for angular velocity */
data class Vec3(val x: Double, val y: Double, val z: Double)
