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
        const val MAX_ALIGNMENT_DRIFT_DEG_PER_SEC = 2.0
    }

    sealed class WindowResult {
        data class Ok(val matrix: DoubleArray) : WindowResult()
        object RejectedDrift : WindowResult()
        object Empty : WindowResult()
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

    @Synchronized
    fun meanAndDriftCheck(
        nowNanos: Long,
        windowNanos: Long = 500_000_000L,
        maxDriftDegPerSec: Double = MAX_ALIGNMENT_DRIFT_DEG_PER_SEC
    ): WindowResult {
        if (count == 0) return WindowResult.Empty
        val minTs = nowNanos - windowNanos
        val indices = ArrayList<Int>(count)
        for (i in 0 until count) {
            val idx = (head + i) % capacity
            if (timestamps[idx] >= minTs) indices.add(idx)
        }
        if (indices.size < 4) {
            val newestIdx = (head + count - 1) % capacity
            return WindowResult.Ok(toMatrix(qw[newestIdx], qx[newestIdx], qy[newestIdx], qz[newestIdx], null))
        }
        val mid = indices.size / 2
        val q1 = meanQuat(indices.subList(0, mid))
        val q2 = meanQuat(indices.subList(mid, indices.size))
        val t1 = 0.5 * (timestamps[indices[0]] + timestamps[indices[mid - 1]])
        val t2 = 0.5 * (timestamps[indices[mid]] + timestamps[indices.last()])
        val dtSec = (t2 - t1) / 1e9
        if (dtSec > 1e-3) {
            val dot = kotlin.math.abs(q1[0] * q2[0] + q1[1] * q2[1] + q1[2] * q2[2] + q1[3] * q2[3]).coerceIn(0.0, 1.0)
            val angDeg = Math.toDegrees(2.0 * acos(dot))
            if (angDeg / dtSec > maxDriftDegPerSec) return WindowResult.RejectedDrift
        }
        val qAll = meanQuat(indices)
        return WindowResult.Ok(toMatrix(qAll[0], qAll[1], qAll[2], qAll[3], null))
    }

    private fun meanQuat(idxs: List<Int>): DoubleArray {
        val refIdx = idxs[0]
        val rw0 = qw[refIdx]; val rx0 = qx[refIdx]; val ry0 = qy[refIdx]; val rz0 = qz[refIdx]
        var sw = 0.0; var sx = 0.0; var sy = 0.0; var sz = 0.0
        for (idx in idxs) {
            val dot = rw0 * qw[idx] + rx0 * qx[idx] + ry0 * qy[idx] + rz0 * qz[idx]
            val s = if (dot < 0.0) -1.0 else 1.0
            sw += s * qw[idx]; sx += s * qx[idx]; sy += s * qy[idx]; sz += s * qz[idx]
        }
        val n = sqrt(sw * sw + sx * sx + sy * sy + sz * sz)
        return if (n > 1e-9) doubleArrayOf(sw / n, sx / n, sy / n, sz / n) else doubleArrayOf(1.0, 0.0, 0.0, 0.0)
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
