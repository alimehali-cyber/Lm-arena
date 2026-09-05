package com.alijafari.red.astronomy.fieldtrial.engine

import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * G-2.5 (L9): project a catalog star through the TRACKER-solved attitude onto the
 * canvas — the "green ring". Uses the tracker's own conventions (J2000 equatorial
 * catalog vectors, q maps catalog->camera, pinhole with image x right / y down /
 * z boresight) and the SAME array->FILL_CENTER canvas mapping the AR overlay uses
 * (shared with InverseProjection). Pure Kotlin; tested against an independent
 * quaternion-rotation implementation in TrackerProjectorTest.
 */
object TrackerProjector {

    fun unitFromRaDec(raDeg: Double, decDeg: Double): DoubleArray {
        val ra = Math.toRadians(raDeg); val dec = Math.toRadians(decDeg)
        val cd = cos(dec)
        return doubleArrayOf(cd * cos(ra), cd * sin(ra), sin(dec))
    }

    /** Catalog unit vector in the CAMERA frame (or null if behind the camera). */
    fun inCameraFrame(vCat: DoubleArray, q: Quaternion): DoubleArray {
        val r = q.rotateVector(Triple(vCat[0], vCat[1], vCat[2]))
        return doubleArrayOf(r.first, r.second, r.third)
    }

    /**
     * Canvas px for the target given the tracker attitude. Mirrors the sensor/
     * FILL_CENTER sections of ARProjectionEngine.projectAltAz exactly (no attitude
     * branch — the attitude IS q).
     */
    fun project(
        raDeg: Double,
        decDeg: Double,
        q: Quaternion,
        canvasWidth: Double,
        canvasHeight: Double,
        intrinsics: InverseProjection.Intrinsics,
        zoomFactor: Double = 1.0,
        displayRotationDegrees: Int = 0
    ): Pair<Double, Double>? {
        val vCam = inCameraFrame(unitFromRaDec(raDeg, decDeg), q)
        val zCam = vCam[2]
        if (zCam <= 0.001) return null
        val xNorm = vCam[0] / zCam
        val yNorm = vCam[1] / zCam
        val uSensor = intrinsics.fx * xNorm + intrinsics.skew * yNorm + intrinsics.cx
        val vSensor = intrinsics.fy * yNorm + intrinsics.cy

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

    fun catalogRaDecOf(stars: List<CatalogStar>, id: String): Pair<Double, Double>? =
        stars.firstOrNull { it.id == id }?.let { Pair(it.raDeg, it.decDeg) }

    /**
     * Convert a screen-px distance to on-sky degrees for the current tier (pinhole
     * small-angle, exact at the image centre): r_sensor = r_canvas / (scale*zoom),
     * angle = atan(r_sensor / fy). Mirrors the FILL_CENTER scale of [project].
     * Used by the L9 dual-ring comparison.
     */
    fun pxDistanceToDeg(
        rCanvasPx: Double,
        intrinsics: InverseProjection.Intrinsics,
        canvasWidth: Double,
        canvasHeight: Double,
        zoomFactor: Double
    ): Double {
        val arrayW = intrinsics.activeArrayWidth.toDouble()
        val arrayH = intrinsics.activeArrayHeight.toDouble()
        val netRot = intrinsics.sensorOrientation % 180
        val (wRot, hRot) = if (netRot == 90) arrayH to arrayW else arrayW to arrayH
        val scale = max(canvasWidth / wRot, canvasHeight / hRot) * zoomFactor
        if (scale == 0.0 || intrinsics.fy == 0.0) return 0.0
        val rSensor = rCanvasPx / scale
        return Math.toDegrees(kotlin.math.atan(rSensor / intrinsics.fy))
    }
}
