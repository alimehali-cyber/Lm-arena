package com.alijafari.red.astronomy.ar.core

import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fixed-size ring buffer of attitude quaternions (w, x, y, z) indexed by monotonic nanosecond
 * timestamps, used to compensate for camera-preview pipeline latency when the live camera is on.
 */
class AttitudeHistory(private val capacity: Int = 64) {

    companion object {
        /** Field-tunable estimate of camera-preview pipeline latency (ms). */
        const val CAMERA_PREVIEW_LATENCY_MS = 60L
    }

    private val timestamps = LongArray(capacity)
    private val qw = DoubleArray(capacity)
    private val qx = DoubleArray(capacity)
    private val qy = DoubleArray(capacity)
    private val qz = DoubleArray(capacity)

    private var head = 0
    private var count = 0
    private var lastTs = Long.MIN_VALUE

    @Synchronized
    fun push(timestampNanos: Long, w: Double, x: Double, y: Double, z: Double) {
        if (count > 0 && timestampNanos <= lastTs) return
        lastTs = timestampNanos
        val idx = (head + count) % capacity
        if (count < capacity) {
            count++
        } else {
            head = (head + 1) % capacity
        }
        timestamps[idx] = timestampNanos
        qw[idx] = w
        qx[idx] = x
        qy[idx] = y
        qz[idx] = z
    }

    @Synchronized
    fun at(timeNanos: Long, outMatrix: DoubleArray? = null): DoubleArray? {
        if (count == 0) return null
        val oldestIdx = head
        val newestIdx = (head + count - 1) % capacity
        if (timeNanos <= timestamps[oldestIdx]) {
            return toMatrix(qw[oldestIdx], qx[oldestIdx], qy[oldestIdx], qz[oldestIdx], outMatrix)
        }
        if (timeNanos >= timestamps[newestIdx]) {
            return toMatrix(qw[newestIdx], qx[newestIdx], qy[newestIdx], qz[newestIdx], outMatrix)
        }

        // Binary search in the ring buffer for bracketing samples [lo, lo + 1]
        var lo = 0
        var hi = count - 1
        while (lo + 1 < hi) {
            val mid = (lo + hi) ushr 1
            val midIdx = (head + mid) % capacity
            if (timestamps[midIdx] <= timeNanos) lo = mid else hi = mid
        }
        val i0 = (head + lo) % capacity
        val i1 = (head + hi) % capacity
        val t0 = timestamps[i0]
        val t1 = timestamps[i1]
        val alpha = if (t1 > t0) (timeNanos - t0).toDouble() / (t1 - t0).toDouble() else 0.0

        val w0 = qw[i0]; val x0 = qx[i0]; val y0 = qy[i0]; val z0 = qz[i0]
        var w1 = qw[i1]; var x1 = qx[i1]; var y1 = qy[i1]; var z1 = qz[i1]
        var dot = w0 * w1 + x0 * x1 + y0 * y1 + z0 * z1
        if (dot < 0.0) {
            w1 = -w1; x1 = -x1; y1 = -y1; z1 = -z1; dot = -dot
        }
        val clamped = dot.coerceIn(0.0, 1.0)
        var rw: Double; var rx: Double; var ry: Double; var rz: Double
        if (clamped < 0.9995) {
            val theta = acos(clamped)
            val st = sin(theta)
            val s0 = sin((1.0 - alpha) * theta) / st
            val s1 = sin(alpha * theta) / st
            rw = s0 * w0 + s1 * w1
            rx = s0 * x0 + s1 * x1
            ry = s0 * y0 + s1 * y1
            rz = s0 * z0 + s1 * z1
        } else {
            rw = w0 + alpha * (w1 - w0)
            rx = x0 + alpha * (x1 - x0)
            ry = y0 + alpha * (y1 - y0)
            rz = z0 + alpha * (z1 - z0)
        }
        val n = sqrt(rw * rw + rx * rx + ry * ry + rz * rz)
        if (n > 1e-9) { rw /= n; rx /= n; ry /= n; rz /= n }
        return toMatrix(rw, rx, ry, rz, outMatrix)
    }

    private fun toMatrix(w: Double, x: Double, y: Double, z: Double, out: DoubleArray?): DoubleArray {
        val m = out ?: DoubleArray(9)
        m[0] = 1.0 - 2.0 * (y * y + z * z)
        m[1] = 2.0 * (x * y - w * z)
        m[2] = 2.0 * (x * z + w * y)
        m[3] = 2.0 * (x * y + w * z)
        m[4] = 1.0 - 2.0 * (x * x + z * z)
        m[5] = 2.0 * (y * z - w * x)
        m[6] = 2.0 * (x * z - w * y)
        m[7] = 2.0 * (y * z + w * x)
        m[8] = 1.0 - 2.0 * (x * x + y * y)
        return m
    }
}
