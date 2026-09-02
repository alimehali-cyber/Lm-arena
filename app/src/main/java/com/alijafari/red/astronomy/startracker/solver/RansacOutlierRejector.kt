package com.alijafari.red.astronomy.startracker.solver

import kotlin.math.*
import java.util.Random

/**
 * RANSAC outlier rejection for star-ID correspondences.
 * Given candidate correspondences (some possibly wrong), iteratively sample minimal subsets,
 * hypothesize rotation, count inliers consistent within angular tolerance, find largest consensus set.
 */

data class Correspondence(
    val observed: StarObservation,
    val catalogStar: com.alijafari.red.astronomy.startracker.catalog.CatalogStar,
    val catalogUnitVector: Triple<Double, Double, Double> // precomputed
)

data class RansacResult(
    val inliers: List<Correspondence>,
    val outliers: List<Correspondence>,
    val bestAttitude: Quaternion?,
    val inlierCount: Int
)

class RansacOutlierRejector(
    val maxIterations: Int = 100,
    val inlierThresholdRad: Double = 0.01, // ~0.57°, conservative, UNVALIDATED
    val minInliers: Int = 4,
    val seed: Long = 42L
) {

    fun rejectOutliers(
        correspondences: List<Correspondence>,
        attitudeSolver: AttitudeSolver = AttitudeSolver()
    ): RansacResult {
        if (correspondences.size < 2) {
            return RansacResult(emptyList(), correspondences, null, 0)
        }

        val rnd = Random(seed)
        var bestInliers: List<Correspondence> = emptyList()
        var bestAttitude: Quaternion? = null

        for (iter in 0 until maxIterations) {
            // Sample minimal subset: 2 stars for TRIAD (minimal for attitude)
            if (correspondences.size < 2) break
            val idx1 = rnd.nextInt(correspondences.size)
            var idx2 = rnd.nextInt(correspondences.size)
            while (idx2 == idx1 && correspondences.size > 1) {
                idx2 = rnd.nextInt(correspondences.size)
            }

            val sample = listOf(correspondences[idx1], correspondences[idx2])

            // Hypothesize rotation via TRIAD
            val v1Cat = sample[0].catalogUnitVector
            val v2Cat = sample[1].catalogUnitVector
            val v1Obs = sample[0].observed.unitVectorCamera
            val v2Obs = sample[1].observed.unitVectorCamera

            // Check if vectors are not collinear (cross product not near zero)
            val crossCat = cross(v1Cat, v2Cat)
            val crossNorm = sqrt(crossCat.first * crossCat.first + crossCat.second * crossCat.second + crossCat.third * crossCat.third)
            if (crossNorm < 1e-6) continue // collinear, skip

            val hypothesizedAttitude = try {
                attitudeSolver.solveTriad(v1Cat, v2Cat, v1Obs, v2Obs)
            } catch (e: Exception) {
                continue
            }

            // Count inliers: correspondences consistent with hypothesized rotation within threshold
            val inliers = mutableListOf<Correspondence>()
            for (corr in correspondences) {
                val catVec = corr.catalogUnitVector
                val obsVec = corr.observed.unitVectorCamera

                // Rotate catalog vector by hypothesized attitude to camera frame
                val rotatedCat = hypothesizedAttitude.rotateVector(catVec)

                // Angular error between rotated catalog and observed
                val dot = (rotatedCat.first * obsVec.first + rotatedCat.second * obsVec.second + rotatedCat.third * obsVec.third).coerceIn(-1.0, 1.0)
                val angleErr = acos(dot)

                if (angleErr <= inlierThresholdRad) {
                    inliers.add(corr)
                }
            }

            if (inliers.size > bestInliers.size) {
                bestInliers = inliers
                bestAttitude = hypothesizedAttitude
            }

            // Early exit if we found all inliers
            if (bestInliers.size == correspondences.size) break
        }

        // Refine best attitude using all inliers via Davenport
        val refinedAttitude = if (bestInliers.size >= 2) {
            try {
                attitudeSolver.solveDavenportQMethod(
                    bestInliers.map { Pair(it.catalogUnitVector, it.observed.unitVectorCamera) },
                    List(bestInliers.size) { 1.0 }
                )
            } catch (e: Exception) {
                bestAttitude
            }
        } else {
            bestAttitude
        }

        val outliers = correspondences.filter { it !in bestInliers }

        return RansacResult(
            inliers = bestInliers,
            outliers = outliers,
            bestAttitude = refinedAttitude,
            inlierCount = bestInliers.size
        )
    }

    private fun cross(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Triple<Double, Double, Double> {
        return Triple(
            a.second * b.third - a.third * b.second,
            a.third * b.first - a.first * b.third,
            a.first * b.second - a.second * b.first
        )
    }
}
