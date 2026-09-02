package com.alijafari.red.astronomy.startracker.validation

import com.alijafari.red.astronomy.startracker.calibration.CameraProfile
import com.alijafari.red.astronomy.startracker.calibration.DistortionModel
import com.alijafari.red.astronomy.startracker.detection.*
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import kotlin.math.*

/**
 * Helper for EndToEndSyntheticTest: pixel image -> detection -> unit vector adapter + undistort -> solver -> attitude error arcsec
 * Pure Kotlin, no Android dependency.
 */

data class EndToEndResult(
    val trueAttitude: Quaternion,
    val estimatedAttitude: Quaternion?,
    val errorArcsec: Double,
    val success: Boolean,
    val numDetected: Int,
    val numMatched: Int,
    val message: String
)

class EndToEndSyntheticTestHelper(
    val width: Int = 1920,
    val height: Int = 1080,
    val cameraProfile: CameraProfile = CameraProfile.fallbackDefault(width, height),
    val distortionModel: DistortionModel = DistortionModel.noDistortion()
) {

    /**
     * Adapter: pixel to unit vector (pinhole model)
     * x_norm = (u - cx - skew*y_norm)/fx etc, then unit vector = normalize(x_norm, y_norm, 1)
     */
    fun pixelToUnitVector(u: Double, v: Double): Triple<Double, Double, Double> {
        val yNorm = (v - cameraProfile.cy) / cameraProfile.fy
        val xNorm = (u - cameraProfile.cx - cameraProfile.skew * yNorm) / cameraProfile.fx

        // For undistort: first undistort normalized coordinates if distortion present
        val (xUndist, yUndist) = if (distortionModel.isIdentity()) {
            Pair(xNorm, yNorm)
        } else {
            // Distorted pixel -> need to undistort: we have distorted normalized, want ideal
            // Our pixel is observed distorted, so we need to undistort
            distortionModel.undistortDistortedToIdealNormalized(xNorm, yNorm)
        }

        // Unit vector: (x, y, 1) normalized, but careful: camera frame +Z is boresight
        val norm = sqrt(xUndist * xUndist + yUndist * yUndist + 1.0)
        return Triple(xUndist / norm, yUndist / norm, 1.0 / norm)
    }

    /**
     * Forward: unit vector to ideal pixel
     */
    fun unitVectorToIdealPixel(unitVec: Triple<Double, Double, Double>): Pair<Double, Double> {
        // unitVec = (x_norm, y_norm, 1)/norm, so x_norm = x/z, y_norm = y/z
        if (unitVec.third <= 0) return Pair(-1.0, -1.0) // behind camera
        val xNorm = unitVec.first / unitVec.third
        val yNorm = unitVec.second / unitVec.third

        val u = cameraProfile.fx * xNorm + cameraProfile.skew * yNorm + cameraProfile.cx
        val v = cameraProfile.fy * yNorm + cameraProfile.cy

        return Pair(u, v)
    }

    /**
     * Forward with distortion: ideal pixel -> distorted pixel (for overlay)
     */
    fun idealPixelToDistortedPixel(uIdeal: Double, vIdeal: Double): Pair<Double, Double> {
        if (distortionModel.isIdentity()) return Pair(uIdeal, vIdeal)

        val yNorm = (vIdeal - cameraProfile.cy) / cameraProfile.fy
        val xNorm = (uIdeal - cameraProfile.cx - cameraProfile.skew * yNorm) / cameraProfile.fx

        val (xDist, yDist) = distortionModel.distortIdealToDistortedNormalized(xNorm, yNorm)

        val uDist = cameraProfile.fx * xDist + cameraProfile.skew * yDist + cameraProfile.cx
        val vDist = cameraProfile.fy * yDist + cameraProfile.cy

        return Pair(uDist, vDist)
    }

    /**
     * Full chain: generate synthetic image from attitude + catalog, detect, convert to unit vectors, solve, compute error
     * This is a simplified version for testing without full catalog matching.
     */
    fun runEndToEnd(
        trueAttitude: Quaternion,
        catalogStars: List<com.alijafari.red.astronomy.startracker.catalog.CatalogStar>,
        noiseSigmaPx: Double = 0.2,
        seed: Long = 42L
    ): EndToEndResult {
        // For this helper, we simulate detection by directly projecting catalog stars to pixels and adding noise
        // Full image rendering + detection would be more heavy, but we can simulate

        val rnd = kotlin.random.Random(seed)
        val observedPixels = mutableListOf<Pair<Double, Double>>()
        val trueUnitVectors = mutableListOf<Triple<Double, Double, Double>>()

        for (catStar in catalogStars) {
            val catVec = catStar.toUnitVector()
            val camVec = trueAttitude.rotateVector(catVec)

            // Check if in front of camera
            if (camVec.third <= 0.1) continue // behind or too close to edge

            val (uIdeal, vIdeal) = unitVectorToIdealPixel(camVec)

            // Check if within image bounds
            if (uIdeal < 0 || uIdeal >= width || vIdeal < 0 || vIdeal >= height) continue

            // Apply distortion for observed pixel
            val (uDist, vDist) = idealPixelToDistortedPixel(uIdeal, vIdeal)

            // Add pixel noise
            val uObs = uDist + rnd.nextDouble(-noiseSigmaPx, noiseSigmaPx)
            val vObs = vDist + rnd.nextDouble(-noiseSigmaPx, noiseSigmaPx)

            observedPixels.add(Pair(uObs, vObs))
            trueUnitVectors.add(catVec)
        }

        if (observedPixels.size < 2) {
            return EndToEndResult(
                trueAttitude = trueAttitude,
                estimatedAttitude = null,
                errorArcsec = Double.MAX_VALUE,
                success = false,
                numDetected = observedPixels.size,
                numMatched = 0,
                message = "Too few stars projected: ${observedPixels.size}"
            )
        }

        // Convert observed distorted pixels to unit vectors via undistort adapter
        val observedUnitVectors = observedPixels.map { (u, v) -> pixelToUnitVector(u, v) }

        // Solve using true correspondences (skip matching for this bench)
        val solver = com.alijafari.red.astronomy.startracker.solver.AttitudeSolver()
        val correspondences = trueUnitVectors.zip(observedUnitVectors).map { (catVec, obsVec) -> Pair(catVec, obsVec) }
        val weights = List(correspondences.size) { 1.0 }

        val estimatedAttitude = try {
            if (correspondences.size == 2) {
                val (cat1, obs1) = correspondences[0]
                val (cat2, obs2) = correspondences[1]
                solver.solveTriad(cat1, cat2, obs1, obs2)
            } else {
                solver.solveDavenportQMethod(correspondences, weights)
            }
        } catch (e: Exception) {
            return EndToEndResult(
                trueAttitude = trueAttitude,
                estimatedAttitude = null,
                errorArcsec = Double.MAX_VALUE,
                success = false,
                numDetected = observedPixels.size,
                numMatched = correspondences.size,
                message = "Solver failed: ${e.message}"
            )
        }

        // Compute angular error
        val dot = abs(trueAttitude.w * estimatedAttitude.w + trueAttitude.x * estimatedAttitude.x + trueAttitude.y * estimatedAttitude.y + trueAttitude.z * estimatedAttitude.z).coerceIn(-1.0, 1.0)
        val errorRad = 2.0 * acos(dot)
        val errorArcsec = Math.toDegrees(errorRad) * 3600.0

        return EndToEndResult(
            trueAttitude = trueAttitude,
            estimatedAttitude = estimatedAttitude,
            errorArcsec = errorArcsec,
            success = true,
            numDetected = observedPixels.size,
            numMatched = correspondences.size,
            message = "Success with ${observedPixels.size} stars, error ${"%.1f".format(errorArcsec)} arcsec"
        )
    }
}
