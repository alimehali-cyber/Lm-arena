package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.InverseProjection
import com.alijafari.red.astronomy.fieldtrial.engine.TrackerProjector
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** G-2.5 (L9): tracker-path projection, checked against an independent rotation impl. */
class TrackerProjectorTest {

    private val intr = InverseProjection.Intrinsics.fallbackTier(1920, 1080)

    /** Independent quaternion rotation (w,x,y,z), Hamilton product — differs from impl path. */
    private fun rotateIndep(v: DoubleArray, w: Double, x: Double, y: Double, z: Double): DoubleArray {
        val conj = doubleArrayOf(w, -x, -y, -z)
        fun mul(a: DoubleArray, b: DoubleArray): DoubleArray {
            val (aw, ax, ay, az) = a.toList()
            val (bw, bx, by, bz) = b.toList()
            return doubleArrayOf(
                aw * bw - ax * bx - ay * by - az * bz,
                aw * bx + ax * bw + ay * bz - az * by,
                aw * by - ax * bz + ay * bw + az * bx,
                aw * bz + ax * by - ay * bx + az * bw
            )
        }
        val p = doubleArrayOf(0.0, v[0], v[1], v[2])
        val r = mul(mul(doubleArrayOf(w, x, y, z), p), conj)
        return doubleArrayOf(r[1], r[2], r[3])
    }

    @Test
    fun `matches an independent rotation plus pinhole across several attitudes`() {
        val q = Quaternion(0.918558, 0.176777, 0.306186, 0.176777) // arbitrary unit-ish
        val ra = 130.5; val dec = -12.3
        val got = TrackerProjector.project(ra, dec, q, 1080.0, 2340.0, intr, 1.0, 0)
        // independent: rotate catalog vector by q, then the same pinhole+FILL_CENTER math
        val v = TrackerProjector.unitFromRaDec(ra, dec)
        val vc = rotateIndep(v, q.w, q.x, q.y, q.z)
        val xN = vc[0] / vc[2]; val yN = vc[1] / vc[2]
        val u = intr.fx * xN + intr.cx; val vv = intr.fy * yN + intr.cy
        // netRotation = sensorOrientation 90, displayRot 0 -> case 90
        val arrayW = 1920.0; val arrayH = 1080.0
        val uRot = arrayH - vv; val vRot = u
        val scale = maxOf(1080.0 / arrayH, 2340.0 / arrayW)
        val px = 1080.0 / 2 + (uRot - arrayH / 2) * scale
        val py = 2340.0 / 2 + (vRot - arrayW / 2) * scale
        assertEquals(px, got!!.first, 1e-6)
        assertEquals(py, got.second, 1e-6)
    }

    @Test
    fun `attitude centered on the target puts it at canvas center`() {
        // build q that maps v_cat(target) -> (0,0,1): rotation about axis = cross(z, v)
        val v = TrackerProjector.unitFromRaDec(271.0, 38.8)
        val zAxis = doubleArrayOf(0.0, 0.0, 1.0)
        // axis = v x z (rotating v ABOUT v x z by the angle between them lands v on +z)
        var axis = doubleArrayOf(
            v[1] * zAxis[2] - v[2] * zAxis[1],
            v[2] * zAxis[0] - v[0] * zAxis[2],
            v[0] * zAxis[1] - v[1] * zAxis[0]
        )
        val n = kotlin.math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2])
        axis = doubleArrayOf(axis[0] / n, axis[1] / n, axis[2] / n)
        val dot = zAxis[0] * v[0] + zAxis[1] * v[1] + zAxis[2] * v[2]
        val ang = kotlin.math.acos(dot.coerceIn(-1.0, 1.0))
        val half = ang / 2
        val q = Quaternion(cos(half), sin(half) * axis[0], sin(half) * axis[1], sin(half) * axis[2])
        val p = TrackerProjector.project(271.0, 38.8, q, 1080.0, 2340.0, intr, 1.0, 0)
        assertEquals(540.0, p!!.first, 1e-6)
        assertEquals(1170.0, p.second, 1e-6)
        // behind the camera (q pointing away) -> null
        val qAway = Quaternion(0.0, 1.0, 0.0, 0.0) // 180 deg about x: flips z
        assertNull(TrackerProjector.project(271.0, 38.8, qAway, 1080.0, 2340.0, intr, 1.0, 0))
    }

    @Test
    fun `mutation guard - wrong ra moves the projection`() {
        val q = Quaternion(0.918558, 0.176777, 0.306186, 0.176777)
        val a = TrackerProjector.project(130.5, -12.3, q, 1080.0, 2340.0, intr)!!
        val b = TrackerProjector.project(131.5, -12.3, q, 1080.0, 2340.0, intr)!!
        assertTrue(kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second) > 1.0)
    }

    @Test
    fun `pxDistanceToDeg - FILL_CENTER scale, zoom and rotation swap`() {
        // fx=fy=1000, 2000x1000 array, sensorOrientation 0, canvas 1000x500:
        // scale = max(1000/2000, 500/1000) = 0.5 -> rSensor = 50/0.5 = 100 px
        // angle = atan(100/1000) = 5.710593 deg
        val i0 = InverseProjection.Intrinsics(
            fx = 1000.0, fy = 1000.0, cx = 1000.0, cy = 500.0, skew = 0.0,
            activeArrayWidth = 2000, activeArrayHeight = 1000, sensorOrientation = 0
        )
        assertEquals(5.710593, TrackerProjector.pxDistanceToDeg(50.0, i0, 1000.0, 500.0, 1.0), 1e-4)
        // zoom 2x: rSensor = 50 -> atan(50/1000) = 2.862405 deg
        assertEquals(2.862405, TrackerProjector.pxDistanceToDeg(50.0, i0, 1000.0, 500.0, 2.0), 1e-4)
        // sensorOrientation 90 swaps the rotated array dims: wRot=1000, hRot=2000 ->
        // scale = max(1000/1000, 500/2000) = 1.0 -> rSensor = 50 -> 2.862405 deg
        val i90 = InverseProjection.Intrinsics(
            fx = 1000.0, fy = 1000.0, cx = 1000.0, cy = 500.0, skew = 0.0,
            activeArrayWidth = 2000, activeArrayHeight = 1000, sensorOrientation = 90
        )
        assertEquals(2.862405, TrackerProjector.pxDistanceToDeg(50.0, i90, 1000.0, 500.0, 1.0), 1e-4)
        // zero fy / zero scale is degenerate but must not throw
        assertEquals(0.0, TrackerProjector.pxDistanceToDeg(50.0, i0.copy(fy = 0.0), 1000.0, 500.0, 1.0), 0.0)
    }

    @Test
    fun `pxDistanceToDeg agrees with project near the boresight`() {
        // small-angle consistency: a star 0.4 deg off the boresight projects
        // r_canvas px away; pxDistanceToDeg(r_canvas) must return ~0.4 deg
        val v = TrackerProjector.unitFromRaDec(271.0, 38.8)
        val zAxis = doubleArrayOf(0.0, 0.0, 1.0)
        var axis = doubleArrayOf(
            v[1] * zAxis[2] - v[2] * zAxis[1],
            v[2] * zAxis[0] - v[0] * zAxis[2],
            v[0] * zAxis[1] - v[1] * zAxis[0]
        )
        val n = kotlin.math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2])
        axis = doubleArrayOf(axis[0] / n, axis[1] / n, axis[2] / n)
        val dot = zAxis[0] * v[0] + zAxis[1] * v[1] + zAxis[2] * v[2]
        val ang = kotlin.math.acos(dot.coerceIn(-1.0, 1.0))
        val half = ang / 2
        val q = Quaternion(cos(half), sin(half) * axis[0], sin(half) * axis[1], sin(half) * axis[2])
        val boresight = TrackerProjector.project(271.0, 38.8, q, 1080.0, 2340.0, intr, 1.0, 0)!!
        val off = TrackerProjector.project(271.0, 38.8 + 0.4, q, 1080.0, 2340.0, intr, 1.0, 0)!!
        val rPx = kotlin.math.hypot(off.first - boresight.first, off.second - boresight.second)
        assertEquals(0.4, TrackerProjector.pxDistanceToDeg(rPx, intr, 1080.0, 2340.0, 1.0), 0.02)
    }
}
