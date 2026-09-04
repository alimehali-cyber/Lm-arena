package com.alijafari.red.astronomy.startracker.fusion

import com.alijafari.red.astronomy.startracker.calibration.CameraProfile
import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.detection.BackgroundParams
import com.alijafari.red.astronomy.startracker.detection.NoiseParams
import com.alijafari.red.astronomy.startracker.detection.StarParams
import com.alijafari.red.astronomy.startracker.detection.SyntheticStarFieldGenerator
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * W1: harness E2E of the pure-Kotlin StarTrackerPipeline on synthetic FRAMES rendered
 * from the real catalog (image pixels -> detection -> adapter -> S2-verified solver ->
 * coordinator -> blend recommendation). SYNTHETIC-SKY.
 *
 * Case 1: clean starfield frame (dense field, PSF sigma 1.2 px, noise sigma 1 ADU)
 *         must reach FULL_LOCK with an arcminute-grade attitude.
 * Case 2: lens-cap frame (background + noise only) must end NO_LOCK, SENSOR_ONLY.
 */
class StarTrackerPipelineTest {

    companion object {
        private val csvPath: String = listOf(
            "data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../../data/startracker/hyg_v36_vle6.5_j2000.csv"
        ).firstOrNull { java.io.File(it).exists() } ?: error("HYG extract not found")

        private val stars by lazy { CatalogIngestor.parse(java.io.File(csvPath).readText(), "HYG_V36_LE6P5") }
        private val quadIndex by lazy { QuadPatternIndex.capped(stars) }

        /** Attitude whose boresight (+Z camera) points at the given equatorial unit vector. */
        private fun boresightAt(target: Triple<Double, Double, Double>): Quaternion {
            val z = Triple(0.0, 0.0, 1.0)
            val dot = (target.first * z.first + target.second * z.second + target.third * z.third).coerceIn(-1.0, 1.0)
            val angle = acos(dot)
            var axis = Triple(target.second * z.third - target.third * z.second,
                target.third * z.first - target.first * z.third,
                target.first * z.second - target.second * z.first)
            val n = sqrt(axis.first * axis.first + axis.second * axis.second + axis.third * axis.third)
            if (n < 1e-9) axis = Triple(1.0, 0.0, 0.0) else axis = Triple(axis.first / n, axis.second / n, axis.third / n)
            return Quaternion.fromAxisAngle(axis, angle)
        }
    }

    private fun angErrArcmin(qTrue: Quaternion, qEst: Quaternion): Double {
        var d = 0.0
        val a = doubleArrayOf(qTrue.w, qTrue.x, qTrue.y, qTrue.z)
        val b = doubleArrayOf(qEst.w, qEst.x, qEst.y, qEst.z)
        for (i in 0 until 4) d += a[i] * b[i]
        return Math.toDegrees(2.0 * acos(abs(d).coerceIn(0.0, 1.0))) * 60.0
    }

    /** Render a synthetic camera frame of the sky under qTrue (1080x1920 portrait sensor
     *  is emulated as 1920x1080 landscape array, matching CameraProfile.fallbackDefault). */
    private fun renderFrame(qTrue: Quaternion): com.alijafari.red.astronomy.startracker.detection.GrayscaleImage {
        val profile = CameraProfile.fallbackDefault(1920, 1080)
        val sps = ArrayList<StarParams>()
        for (s in stars) {
            if (s.magnitude > 5.5) continue
            val v = qTrue.rotateVector(s.toUnitVector())
            if (v.third <= 0.1) continue
            val xN = v.first / v.third; val yN = v.second / v.third
            val u = profile.fx * xN + profile.cx
            val vv = profile.fy * yN + profile.cy
            if (u < 8 || u > 1912 || vv < 8 || vv > 1072) continue
            // amplitude from magnitude with a ~1:25 dynamic range (V=0 -> 1000 ADU,
            // floor 40 ADU keeps the full index range V<=5.5 above the 5-sigma detector).
            // Realistic diversity matters: a flat floor makes merged blob PAIRS the
            // brightest "stars", which corrupts flux-ranked anchor selection (W1 finding).
            val amp = maxOf(1000.0 * Math.pow(10.0, -0.4 * s.magnitude), 40.0)
            sps.add(StarParams(u, vv, amp, 1.2))
        }
        assertTrue("synthetic frame should contain >= 25 rendered stars (found ${sps.size})", sps.size >= 25)
        // saturationMax raised to 4096 ADU (synthetic sensor well depth) so bright
        // stars are not clipped at 8-bit levels in this SYNTHETIC frame model.
        val field = SyntheticStarFieldGenerator.generate(
            1920, 1080, sps,
            BackgroundParams(baseLevel = 10.0f),
            NoiseParams(gaussianSigma = 1.0f),
            saturationMax = 4096f)
        return field.image
    }

    @Test
    fun `clean frame reaches FULL_LOCK with arcmin attitude`() {
        // dense field: near the galactic center (Sgr) in equatorial coordinates
        val ra = Math.toRadians(266.4); val dec = Math.toRadians(-29.0)
        val qTrue = boresightAt(Triple(cos(dec) * cos(ra), cos(dec) * sin(ra), sin(dec)))
        val pipeline = StarTrackerPipeline(
            solver = LostInSpaceSolver(quadIndex, stars),
            cameraProfile = CameraProfile.fallbackDefault(1920, 1080))
        val res = pipeline.process(renderFrame(qTrue))
        assertEquals("expected FULL_LOCK, got ${res.lockConfidence} (${res.message})",
            LockConfidence.FULL_LOCK, res.lockConfidence)
        assertEquals(StarTrackerPipeline.BlendRecommendation.PREFER_TRACKER, res.blendRecommendation)
        assertNotNull(res.attitude)
        val err = angErrArcmin(qTrue, res.attitude!!)
        assertTrue("attitude error $err arcmin should be < 5 arcmin", err < 5.0)
        assertTrue("detections ${res.numDetections} should be >= 20", res.numDetections >= 20)
        assertTrue("full-field matched should be >= 20", res.solverDiagnostics!!.fullFieldMatched >= 20)
    }

    @Test
    fun `lens-cap blank frame ends NO_LOCK with SENSOR_ONLY recommendation`() {
        val pipeline = StarTrackerPipeline(
            solver = LostInSpaceSolver(quadIndex, stars),
            cameraProfile = CameraProfile.fallbackDefault(1920, 1080))
        val blank = SyntheticStarFieldGenerator.generate(
            1920, 1080, emptyList(),
            BackgroundParams(baseLevel = 10.0f),
            NoiseParams(gaussianSigma = 1.0f)).image
        val res = pipeline.process(blank)
        assertEquals(LockConfidence.NO_LOCK, res.lockConfidence)
        assertEquals(StarTrackerPipeline.BlendRecommendation.SENSOR_ONLY, res.blendRecommendation)
        assertEquals(0, res.numDetections)
        assertTrue("message should say no detections: ${res.message}", res.message.contains("no detections"))
    }
}
