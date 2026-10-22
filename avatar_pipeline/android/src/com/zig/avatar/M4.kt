package com.zig.avatar

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 4x4 matrices in column-major order, matching glTF/OpenGL, in flat FloatArrays.
 *
 * Written out rather than pulled from a library: the whole point of this package is to
 * be an isolated, dependency-free folder that can be deleted without touching the app.
 */
internal object M4 {

    fun identity(out: FloatArray, o: Int = 0) {
        for (i in 0 until 16) out[o + i] = 0f
        out[o + 0] = 1f; out[o + 5] = 1f; out[o + 10] = 1f; out[o + 15] = 1f
    }

    /** out = a * b. Safe when out aliases a or b (copies first). */
    fun mul(out: FloatArray, a: FloatArray, b: FloatArray) {
        val t = FloatArray(16)
        for (col in 0 until 4) {
            val b0 = b[col * 4]; val b1 = b[col * 4 + 1]; val b2 = b[col * 4 + 2]; val b3 = b[col * 4 + 3]
            for (row in 0 until 4) {
                t[col * 4 + row] = a[row] * b0 + a[4 + row] * b1 + a[8 + row] * b2 + a[12 + row] * b3
            }
        }
        System.arraycopy(t, 0, out, 0, 16)
    }

    /** TRS -> matrix. q is (x, y, z, w). */
    fun fromTrs(out: FloatArray, o: Int, t: FloatArray, to: Int, r: FloatArray, ro: Int, s: FloatArray, so: Int) {
        val x = r[ro]; val y = r[ro + 1]; val z = r[ro + 2]; val w = r[ro + 3]
        val n = sqrt(x * x + y * y + z * z + w * w).let { if (it < 1e-8f) 1f else it }
        val qx = x / n; val qy = y / n; val qz = z / n; val qw = w / n
        val xx = qx * qx; val yy = qy * qy; val zz = qz * qz
        val xy = qx * qy; val xz = qx * qz; val yz = qy * qz
        val wx = qw * qx; val wy = qw * qy; val wz = qw * qz
        val sx = s[so]; val sy = s[so + 1]; val sz = s[so + 2]

        out[o + 0] = (1f - 2f * (yy + zz)) * sx
        out[o + 1] = (2f * (xy + wz)) * sx
        out[o + 2] = (2f * (xz - wy)) * sx
        out[o + 3] = 0f

        out[o + 4] = (2f * (xy - wz)) * sy
        out[o + 5] = (1f - 2f * (xx + zz)) * sy
        out[o + 6] = (2f * (yz + wx)) * sy
        out[o + 7] = 0f

        out[o + 8] = (2f * (xz + wy)) * sz
        out[o + 9] = (2f * (yz - wx)) * sz
        out[o + 10] = (1f - 2f * (xx + yy)) * sz
        out[o + 11] = 0f

        out[o + 12] = t[to]
        out[o + 13] = t[to + 1]
        out[o + 14] = t[to + 2]
        out[o + 15] = 1f
    }

    /** Allocation-free TRS used by the per-frame pose path. */
    fun fromTrsF(
        out: FloatArray, o: Int,
        tx: Float, ty: Float, tz: Float,
        qx0: Float, qy0: Float, qz0: Float, qw0: Float,
        sx: Float, sy: Float, sz: Float,
    ) {
        val n = sqrt(qx0 * qx0 + qy0 * qy0 + qz0 * qz0 + qw0 * qw0).let { if (it < 1e-8f) 1f else it }
        val qx = qx0 / n; val qy = qy0 / n; val qz = qz0 / n; val qw = qw0 / n
        val xx = qx * qx; val yy = qy * qy; val zz = qz * qz
        val xy = qx * qy; val xz = qx * qz; val yz = qy * qz
        val wx = qw * qx; val wy = qw * qy; val wz = qw * qz

        out[o + 0] = (1f - 2f * (yy + zz)) * sx
        out[o + 1] = (2f * (xy + wz)) * sx
        out[o + 2] = (2f * (xz - wy)) * sx
        out[o + 3] = 0f

        out[o + 4] = (2f * (xy - wz)) * sy
        out[o + 5] = (1f - 2f * (xx + zz)) * sy
        out[o + 6] = (2f * (yz + wx)) * sy
        out[o + 7] = 0f

        out[o + 8] = (2f * (xz + wy)) * sz
        out[o + 9] = (2f * (yz - wx)) * sz
        out[o + 10] = (1f - 2f * (xx + yy)) * sz
        out[o + 11] = 0f

        out[o + 12] = tx
        out[o + 13] = ty
        out[o + 14] = tz
        out[o + 15] = 1f
    }

    /** out = a * b for offsets into flat arrays. */
    fun mulAt(out: FloatArray, o: Int, a: FloatArray, ao: Int, b: FloatArray, bo: Int) {
        for (col in 0 until 4) {
            val b0 = b[bo + col * 4]; val b1 = b[bo + col * 4 + 1]
            val b2 = b[bo + col * 4 + 2]; val b3 = b[bo + col * 4 + 3]
            for (row in 0 until 4) {
                out[o + col * 4 + row] =
                    a[ao + row] * b0 + a[ao + 4 + row] * b1 + a[ao + 8 + row] * b2 + a[ao + 12 + row] * b3
            }
        }
    }

    /** Full 4x4 inverse. Returns false if the matrix is singular. */
    fun invert(out: FloatArray, m: FloatArray): Boolean {
        val a00 = m[0]; val a01 = m[1]; val a02 = m[2]; val a03 = m[3]
        val a10 = m[4]; val a11 = m[5]; val a12 = m[6]; val a13 = m[7]
        val a20 = m[8]; val a21 = m[9]; val a22 = m[10]; val a23 = m[11]
        val a30 = m[12]; val a31 = m[13]; val a32 = m[14]; val a33 = m[15]

        val b00 = a00 * a11 - a01 * a10
        val b01 = a00 * a12 - a02 * a10
        val b02 = a00 * a13 - a03 * a10
        val b03 = a01 * a12 - a02 * a11
        val b04 = a01 * a13 - a03 * a11
        val b05 = a02 * a13 - a03 * a12
        val b06 = a20 * a31 - a21 * a30
        val b07 = a20 * a32 - a22 * a30
        val b08 = a20 * a33 - a23 * a30
        val b09 = a21 * a32 - a22 * a31
        val b10 = a21 * a33 - a23 * a31
        val b11 = a22 * a33 - a23 * a32

        var det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
        if (abs(det) < 1e-12f) return false
        det = 1f / det

        out[0] = (a11 * b11 - a12 * b10 + a13 * b09) * det
        out[1] = (a02 * b10 - a01 * b11 - a03 * b09) * det
        out[2] = (a31 * b05 - a32 * b04 + a33 * b03) * det
        out[3] = (a22 * b04 - a21 * b05 - a23 * b03) * det
        out[4] = (a12 * b08 - a10 * b11 - a13 * b07) * det
        out[5] = (a00 * b11 - a02 * b08 + a03 * b07) * det
        out[6] = (a32 * b02 - a30 * b05 - a33 * b01) * det
        out[7] = (a20 * b05 - a22 * b02 + a23 * b01) * det
        out[8] = (a10 * b10 - a11 * b08 + a13 * b06) * det
        out[9] = (a01 * b08 - a00 * b10 - a03 * b06) * det
        out[10] = (a30 * b04 - a31 * b02 + a33 * b00) * det
        out[11] = (a21 * b02 - a20 * b04 - a23 * b00) * det
        out[12] = (a11 * b07 - a10 * b09 - a12 * b06) * det
        out[13] = (a00 * b09 - a01 * b07 + a02 * b06) * det
        out[14] = (a31 * b01 - a30 * b03 - a32 * b00) * det
        out[15] = (a20 * b03 - a21 * b01 + a22 * b00) * det
        return true
    }

    fun perspective(out: FloatArray, fovYDeg: Float, aspect: Float, near: Float, far: Float) {
        val f = 1f / kotlin.math.tan(Math.toRadians(fovYDeg.toDouble() / 2.0)).toFloat()
        for (i in 0 until 16) out[i] = 0f
        out[0] = f / aspect
        out[5] = f
        out[10] = (far + near) / (near - far)
        out[11] = -1f
        out[14] = 2f * far * near / (near - far)
    }

    fun lookAt(out: FloatArray, ex: Float, ey: Float, ez: Float, cx: Float, cy: Float, cz: Float) {
        var zx = ex - cx; var zy = ey - cy; var zz = ez - cz
        val zl = sqrt(zx * zx + zy * zy + zz * zz).let { if (it < 1e-6f) 1f else it }
        zx /= zl; zy /= zl; zz /= zl
        // up = (0,1,0); x = up x z
        var xx = 1f * zz - 0f * zy
        var xy = 0f * zx - 1f * zz
        val xl = sqrt(xx * xx + xy * xy).let { if (it < 1e-6f) 1f else it }
        xx /= xl; xy /= xl
        val xz = 0f
        val yx = zy * xz - zz * xy
        val yy = zz * xx - zx * xz
        val yz = zx * xy - zy * xx

        out[0] = xx; out[1] = yx; out[2] = zx; out[3] = 0f
        out[4] = xy; out[5] = yy; out[6] = zy; out[7] = 0f
        out[8] = xz; out[9] = yz; out[10] = zz; out[11] = 0f
        out[12] = -(xx * ex + xy * ey + xz * ez)
        out[13] = -(yx * ex + yy * ey + yz * ez)
        out[14] = -(zx * ex + zy * ey + zz * ez)
        out[15] = 1f
    }
}

internal object Quat {
    fun slerp(out: FloatArray, o: Int, a: FloatArray, ao: Int, b: FloatArray, bo: Int, t: Float) {
        var ax = a[ao]; var ay = a[ao + 1]; var az = a[ao + 2]; var aw = a[ao + 3]
        var bx = b[bo]; var by = b[bo + 1]; var bz = b[bo + 2]; var bw = b[bo + 3]
        var dot = ax * bx + ay * by + az * bz + aw * bw
        if (dot < 0f) { bx = -bx; by = -by; bz = -bz; bw = -bw; dot = -dot }
        if (dot > 0.9995f) {
            // nearly parallel: lerp and normalise, cheaper and stable
            out[o] = ax + (bx - ax) * t
            out[o + 1] = ay + (by - ay) * t
            out[o + 2] = az + (bz - az) * t
            out[o + 3] = aw + (bw - aw) * t
            val n = sqrt(out[o] * out[o] + out[o + 1] * out[o + 1] + out[o + 2] * out[o + 2] + out[o + 3] * out[o + 3])
            if (n > 1e-8f) { out[o] /= n; out[o + 1] /= n; out[o + 2] /= n; out[o + 3] /= n }
            return
        }
        val theta = kotlin.math.acos(dot.coerceIn(-1f, 1f))
        val st = sin(theta)
        val wa = sin((1f - t) * theta) / st
        val wb = sin(t * theta) / st
        out[o] = ax * wa + bx * wb
        out[o + 1] = ay * wa + by * wb
        out[o + 2] = az * wa + bz * wb
        out[o + 3] = aw * wa + bw * wb
    }

    fun nlerp(out: FloatArray, o: Int, a: FloatArray, ao: Int, b: FloatArray, bo: Int, t: Float) {
        var bx = b[bo]; var by = b[bo + 1]; var bz = b[bo + 2]; var bw = b[bo + 3]
        val dot = a[ao] * bx + a[ao + 1] * by + a[ao + 2] * bz + a[ao + 3] * bw
        if (dot < 0f) { bx = -bx; by = -by; bz = -bz; bw = -bw }
        var x = a[ao] + (bx - a[ao]) * t
        var y = a[ao + 1] + (by - a[ao + 1]) * t
        var z = a[ao + 2] + (bz - a[ao + 2]) * t
        var w = a[ao + 3] + (bw - a[ao + 3]) * t
        val n = sqrt(x * x + y * y + z * z + w * w).let { if (it < 1e-8f) 1f else it }
        out[o] = x / n; out[o + 1] = y / n; out[o + 2] = z / n; out[o + 3] = w / n
    }
}

/**
 * A cubic Hermite curve evaluated as a smoothed ramp.
 *
 * Used for transitions between clips (fade in/out) and for the breathing "moving hold"
 * offset. Linear fades read mechanical, which is exactly the note the animation review
 * keeps making.
 */
internal object Ease {
    fun smoothStep(t: Float): Float {
        val u = t.coerceIn(0f, 1f)
        return u * u * (3f - 2f * u)
    }

    fun inOutSine(t: Float): Float {
        val u = t.coerceIn(0f, 1f)
        return 0.5f - 0.5f * cos(Math.PI.toFloat() * u)
    }
}
