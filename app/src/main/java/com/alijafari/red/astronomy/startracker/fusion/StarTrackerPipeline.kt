package com.alijafari.red.astronomy.startracker.fusion

import com.alijafari.red.astronomy.startracker.calibration.CameraProfile
import com.alijafari.red.astronomy.startracker.calibration.DistortionModel
import com.alijafari.red.astronomy.startracker.detection.BackgroundEstimator
import com.alijafari.red.astronomy.startracker.detection.Centroider
import com.alijafari.red.astronomy.startracker.detection.GrayscaleImage
import com.alijafari.red.astronomy.startracker.detection.StarBlobDetector
import com.alijafari.red.astronomy.startracker.diagnostics.ConfidenceLadderCoordinator
import com.alijafari.red.astronomy.startracker.diagnostics.CoordinatorInput
import com.alijafari.red.astronomy.startracker.diagnostics.CoordinatorOutput
import com.alijafari.red.astronomy.startracker.diagnostics.FrameQuality
import com.alijafari.red.astronomy.startracker.diagnostics.FrameQualityClassifier
import com.alijafari.red.astronomy.startracker.diagnostics.FailureReason
import com.alijafari.red.astronomy.startracker.diagnostics.SolverDiagnostics
import com.alijafari.red.astronomy.startracker.solver.FullFieldVerifier
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.StarObservation
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import com.alijafari.red.astronomy.startracker.tracking.TrackingLoop
import kotlin.math.sqrt

/**
 * W1: pure-Kotlin end-to-end star-tracker pipeline (offline-harness executable):
 *
 *   GrayscaleImage
 *     -> BackgroundEstimator -> StarBlobDetector -> Centroider (sub-pixel, moment)
 *     -> observation adapter (unique ids, pinhole + undistort pixel->camera unit vector)
 *     -> LostInSpaceSolver (S2 full-field-verified)   [relock path: TrackingLoop]
 *     -> ConfidenceLadderCoordinator (FrameQuality + FailureReason + SolverDiagnostics)
 *     -> blend RECOMMENDATION for the device fusion layer (AttitudeBlender, PHASE6)
 *
 * No Android imports; every stage is the production class. The pipeline is deliberately
 * synchronous and allocation-simple: the device adapter (W2, UNEXECUTED) owns threading,
 * frame dropping and the feature flag.
 */
class StarTrackerPipeline(
    solver: LostInSpaceSolver,
    val cameraProfile: CameraProfile,
    val distortionModel: DistortionModel = DistortionModel.noDistortion(),
    val backgroundEstimator: BackgroundEstimator = BackgroundEstimator(),
    val blobDetector: StarBlobDetector = StarBlobDetector(),
    val centroider: Centroider = Centroider(),
    val frameQualityClassifier: FrameQualityClassifier = FrameQualityClassifier(),
    val coordinator: ConfidenceLadderCoordinator = ConfidenceLadderCoordinator(),
    val trackingLoop: TrackingLoop = TrackingLoop(solver.catalogStars, solver.quadIndex)
) {

    /** Solver actually used in process(): T4(b)-upgraded when no distortion model. */
    val solver: LostInSpaceSolver =
        if (distortionModel.isIdentity()) upgradeSolverForUnmodelledDistortion(solver) else solver

    companion object {
        /**
         * T4(b): when NO distortion model is available (identity), the solver's verifier
         * is upgraded to the radial tolerance envelope (|k1| <= 0.08 over the tier), so
         * a correct solve on an uncalibrated phone is not rejected at the field edge.
         * When a distortion model IS present, the flat tolerance stands (it must: the
         * pipeline undistorts detections before solving).
         */
        fun upgradeSolverForUnmodelledDistortion(solver: LostInSpaceSolver): LostInSpaceSolver {
            val v = solver.fullFieldVerifier ?: return solver
            if (v.radialToleranceCoefficientC != 0.0) return solver
            return LostInSpaceSolver(
                quadIndex = solver.quadIndex,
                catalogStars = solver.catalogStars,
                catalogStarsById = solver.catalogStarsById,
                quadBuilder = solver.quadBuilder,
                matcher = solver.matcher,
                ransac = solver.ransac,
                attitudeSolver = solver.attitudeSolver,
                fullFieldVerifier = FullFieldVerifier.withUnmodelledDistortionAllowance()
            )
        }
    }


    enum class BlendRecommendation {
        /** Star tracker attitude should dominate the fusion (tracker healthy). */
        PREFER_TRACKER,
        /** Sensor attitude should dominate; tracker assists (partial/noisy lock). */
        PREFER_SENSOR,
        /** Ignore the tracker this frame; pure sensor attitude. */
        SENSOR_ONLY
    }

    data class PipelineResult(
        val lockConfidence: LockConfidence,
        val attitude: Quaternion?,          // star-solved attitude (null when no solve)
        val blendRecommendation: BlendRecommendation,
        val coordinatorOutput: CoordinatorOutput,
        val numDetections: Int,
        val solverDiagnostics: SolverDiagnostics?,
        val rmsErrorPx: Double,             // full-field mean residual of matched detections, px
        val message: String
    )

    /** Pixel (u,v) -> camera-frame unit vector via pinhole + undistortion. */
    fun pixelToUnitVector(u: Double, v: Double): Triple<Double, Double, Double> {
        val yNorm = (v - cameraProfile.cy) / cameraProfile.fy
        val xNorm = (u - cameraProfile.cx - cameraProfile.skew * yNorm) / cameraProfile.fx
        val (xu, yu) = if (distortionModel.isIdentity()) Pair(xNorm, yNorm)
        else distortionModel.undistortDistortedToIdealNormalized(xNorm, yNorm)
        val n = sqrt(xu * xu + yu * yu + 1.0)
        return Triple(xu / n, yu / n, 1.0 / n) // camera frame: +Z boresight
    }

    fun process(frame: GrayscaleImage): PipelineResult {
        // 1) background + noise + detection
        val bg = backgroundEstimator.estimate(frame)
        val noiseSigma = backgroundEstimator.estimateNoiseSigma(frame, bg)
        val blobs = blobDetector.detect(frame, bg, noiseSigma)

        // 2) frame quality + failure reasons
        val meanInt = if (blobs.isEmpty()) 0.0 else blobs.map { it.meanIntensity.toDouble() }.average()
        val stats = com.alijafari.red.astronomy.startracker.diagnostics.BlobStats(
            blobCount = blobs.size,
            meanBrightness = meanInt,
            brightnessStd = if (blobs.isEmpty()) 0.0 else sqrt(blobs.map { (it.meanIntensity.toDouble() - meanInt) * (it.meanIntensity.toDouble() - meanInt) }.average()),
            meanSize = if (blobs.isEmpty()) 0.0 else blobs.map { it.pixels.size.toDouble() }.average(),
            sizeStd = 0.0,
            backgroundMean = bg.globalMean.toDouble(),
            backgroundStd = bg.globalSigma.toDouble(),
            maxBrightness = if (blobs.isEmpty()) 0.0 else blobs.maxOf { it.peakValue.toDouble() }
        )
        val (quality, qualityReason) = frameQualityClassifier.classifyWithReason(stats)

        // 3) no stars -> straight to the coordinator (lens-cap case)
        if (blobs.isEmpty()) {
            val out = coordinator.coordinate(
                CoordinatorInput(
                    frameQuality = quality,
                    failureReason = FailureReason.NoStarsDetected,
                    ambiguityResult = null,
                    solverDiagnostics = null))
            return PipelineResult(out.lockConfidence, null, recommend(out.lockConfidence), out, 0, null,
                0.0, "no detections (lens cap / blank sky)")
        }

        // 4) observation adapter: centroid -> pixel -> camera unit vector, UNIQUE ids
        val observations = ArrayList<StarObservation>(blobs.size)
        for ((i, b) in blobs.withIndex()) {
            val c = centroider.centroid(frame, bg, b)
            val uv = pixelToUnitVector(c.x, c.y)
            observations.add(StarObservation(uv, c.flux, b.peakValue >= 250f, "DET$i"))
        }

        // 5) S2-verified lost-in-space solve
        val res = solver.solve(observations)

        // Full-field MEDIAN residual of matched detections in px (57"/px scale).
        // Median, not mean: a handful of near-tolerance matches (merged blob centroids)
        // skew the mean far above the typical residual; the coordinator's Rule 4
        // compares a robust central-tendency statistic against its 1 px threshold.
        val rmsPx = if (res.success) res.fullFieldMedianResidualArcsec / 57.0 else 0.0
        val diag = SolverDiagnostics(
            inlierCount = res.inlierCount,
            confidence = res.confidence,
            rmsError = rmsPx,
            success = res.success,
            fullFieldMatched = res.fullFieldMatched,
            fullFieldFraction = res.fullFieldFraction)

        // 6) coordinator decision
        val failureReason = when {
            !res.success -> FailureReason.SolverFailed
            qualityReason != null -> qualityReason
            else -> null
        }
        val out = coordinator.coordinate(
            CoordinatorInput(
                frameQuality = quality,
                failureReason = failureReason,
                ambiguityResult = null, // single-hypothesis pipeline; AmbiguityDetector wiring is device-side (W2)
                solverDiagnostics = diag))

        // 7) tracking loop bookkeeping + blend recommendation
        val attitude = if (res.success) res.attitude else null
        if (out.lockConfidence == LockConfidence.FULL_LOCK && attitude != null) {
            trackingLoop.initializeWithLock(attitude)
        }
        return PipelineResult(
            lockConfidence = out.lockConfidence,
            attitude = attitude,
            blendRecommendation = recommend(out.lockConfidence),
            coordinatorOutput = out,
            numDetections = observations.size,
            solverDiagnostics = diag,
            rmsErrorPx = rmsPx,
            message = if (res.success) "solved inliers=${res.inlierCount} ff=${res.fullFieldMatched}/${observations.size}"
                      else (res.errorMessage ?: "solver failed"))
    }

    private fun recommend(state: LockConfidence): BlendRecommendation = when (state) {
        LockConfidence.FULL_LOCK -> BlendRecommendation.PREFER_TRACKER
        LockConfidence.MARGINAL_LOCK -> BlendRecommendation.PREFER_SENSOR
        else -> BlendRecommendation.SENSOR_ONLY
    }

    /** 57 arcsec per pixel — the D/S3 synthetic-chain pixel scale (63.5 deg tier). */
    val arcsecPerPx: Double = 57.0
}
