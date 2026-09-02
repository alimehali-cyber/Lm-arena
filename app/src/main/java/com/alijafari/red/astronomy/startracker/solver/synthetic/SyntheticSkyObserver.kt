package com.alijafari.red.astronomy.startracker.solver.synthetic

import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.StarObservation
import kotlin.math.*
import java.util.Random

/**
 * Synthetic sky observer — ground-truth generator for solver phase.
 * Given catalog stars, ground-truth attitude quaternion, FOV, noise, false stars,
 * generates synthetic StarObservations with known ground truth.
 */
class SyntheticSkyObserver {

    data class ObservationResult(
        val observations: List<StarObservation>,
        val trueCorrespondences: Map<String, CatalogStar> // observation id -> true catalog star
    )

    /**
     * Observe catalog stars from given attitude.
     * @param catalogStars list of catalog stars
     * @param groundTruthAttitude quaternion rotating from catalog (J2000) frame to camera frame: v_cam = q * v_cat * q_conj
     * @param fovLimitRad FOV limit: max angle from boresight (e.g., 30° for 60° FOV), stars beyond this are not visible
     * @param noiseSigmaRad angular noise sigma in radians to simulate imperfect centroiding
     * @param numFalseStars number of false stars to inject at random directions within FOV
     * @param seed random seed
     * @return observations + true correspondences
     */
    fun observe(
        catalogStars: List<CatalogStar>,
        groundTruthAttitude: Quaternion,
        fovLimitRad: Double,
        noiseSigmaRad: Double = 0.0,
        numFalseStars: Int = 0,
        seed: Long = 42L
    ): ObservationResult {
        val rnd = Random(seed)
        val observations = mutableListOf<StarObservation>()
        val correspondences = mutableMapOf<String, CatalogStar>()

        // Camera boresight in camera frame is +Z (0,0,1) typically
        val boresight = Triple(0.0, 0.0, 1.0)

        var obsId = 0

        for (catStar in catalogStars) {
            val catVec = catStar.toUnitVector()
            // Rotate to camera frame: v_cam = q * v_cat * q_conj
            val camVec = groundTruthAttitude.rotateVector(catVec)

            // Check if within FOV: angle between camVec and boresight < fovLimit
            val dot = (camVec.first * boresight.first + camVec.second * boresight.second + camVec.third * boresight.third).coerceIn(-1.0, 1.0)
            val angleFromBoresight = acos(dot)
            if (angleFromBoresight > fovLimitRad) continue // outside FOV

            // Add angular noise: perturb unit vector by small rotation
            var noisyVec = camVec
            if (noiseSigmaRad > 1e-12) {
                // Random small rotation axis perpendicular to camVec
                // Generate random perpendicular vector
                val randomVec = Triple(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5)
                // Cross with camVec to get perpendicular
                val perp = cross(camVec, randomVec)
                val perpNorm = sqrt(perp.first * perp.first + perp.second * perp.second + perp.third * perp.third)
                if (perpNorm > 1e-9) {
                    val perpUnit = Triple(perp.first / perpNorm, perp.second / perpNorm, perp.third / perpNorm)
                    val noiseAngle = rnd.nextGaussian() * noiseSigmaRad
                    val noiseQuat = Quaternion.fromAxisAngle(perpUnit, noiseAngle)
                    noisyVec = noiseQuat.rotateVector(camVec)
                    // Renormalize
                    val norm = sqrt(noisyVec.first * noisyVec.first + noisyVec.second * noisyVec.second + noisyVec.third * noisyVec.third)
                    noisyVec = Triple(noisyVec.first / norm, noisyVec.second / norm, noisyVec.third / norm)
                }
            }

            val obs = StarObservation(
                unitVectorCamera = noisyVec,
                flux = 1.0 / (1.0 + catStar.magnitude), // brighter = higher flux, simple
                isSaturated = false,
                id = "OBS${obsId++}"
            )
            observations.add(obs)
            correspondences[obs.id] = catStar
        }

        // Inject false stars at random directions within FOV
        for (i in 0 until numFalseStars) {
            // Random direction within FOV cone around boresight
            // Sample random angle from boresight within fovLimit, and random azimuth
            val angle = rnd.nextDouble() * fovLimitRad // uniform in angle (not area, but ok for test)
            val azimuth = rnd.nextDouble() * 2 * PI

            // Convert to unit vector: spherical coordinates with boresight as pole
            // boresight = +Z, so we want vector with polar angle = angle, azimuth = azimuth
            val sinAngle = sin(angle)
            val x = sinAngle * cos(azimuth)
            val y = sinAngle * sin(azimuth)
            val z = cos(angle)

            val falseVec = Triple(x, y, z)

            val falseObs = StarObservation(
                unitVectorCamera = falseVec,
                flux = rnd.nextDouble() * 0.5,
                isSaturated = false,
                id = "FALSE${obsId++}"
            )
            observations.add(falseObs)
            // No correspondence for false stars
        }

        return ObservationResult(observations, correspondences)
    }

    private fun cross(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Triple<Double, Double, Double> {
        return Triple(
            a.second * b.third - a.third * b.second,
            a.third * b.first - a.first * b.third,
            a.first * b.second - a.second * b.first
        )
    }
}
