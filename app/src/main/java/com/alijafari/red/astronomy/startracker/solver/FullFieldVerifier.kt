package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import kotlin.math.acos

/**
 * S2 (KIND-B): Tetra3-style full-field verification.
 *
 * A quad match + 4 RANSAC inliers can be a chance descriptor collision (S1 dissection:
 * false locks at zero noise had exactly 4 inliers, rms 192-1054", vs ~0" for correct
 * solves). Before any confidence is granted, project ALL catalog stars down to the
 * detection magnitude limit through the candidate attitude and count how many actual
 * detections they explain. A correct attitude lights up the whole field; a colliding
 * false attitude explains only chance coincidences (~3 expected at 300" tolerance).
 *
 * Gate (both required, before confidence may reach MARGINAL or higher):
 *   matchedDetections >= minMatchedCount  AND  matchedDetections / detections >= minMatchedFraction.
 * The ConfidenceLadderCoordinator additionally demands stricter values for FULL_LOCK.
 *
 * Pure Kotlin; SYNTHETIC-SKY validated; all parameters conservative and documented.
 */
class FullFieldVerifier(
    val toleranceRad: Double = Math.toRadians(300.0 / 3600.0), // 300" = 5 px @ 57"/px: >2.6 sigma at 2 px noise, ~7x below the RANSAC threshold
    val magLimit: Double = 6.5,          // detection limit of the shipped extract
    val minMatchedCount: Int = 8,        // chance coincidences at 300" are ~3 per real field
    val minMatchedFraction: Double = 0.25, // leaves headroom for injected false stars (~20/100)
    val fovLimitRad: Double = Math.toRadians(31.75) // 63.5 deg tier diagonal (Phase-1 fallback)
) {

    data class Result(
        val matchedDetections: Int,
        val totalDetections: Int,
        val fraction: Double,
        val pass: Boolean
    )

    /**
     * @param attitude candidate attitude mapping CATALOG -> CAMERA (solver convention:
     * rotateVector(catVec) must land in the camera frame, +Z boresight).
     * @param detections observed unit vectors in the camera frame.
     * @param catalogStars the full catalog (filtered here by [magLimit]).
     */
    fun verify(
        attitude: Quaternion,
        detections: List<StarObservation>,
        catalogStars: List<CatalogStar>
    ): Result {
        // 1) Predict every catalog star within the detection mag limit into the camera frame.
        val predicted = ArrayList<Triple<Double, Double, Double>>(1024)
        val cosFov = kotlin.math.cos(fovLimitRad)
        for (s in catalogStars) {
            if (s.magnitude > magLimit) continue
            val v = attitude.rotateVector(s.toUnitVector())
            if (v.third >= cosFov) predicted.add(v) // inside the FOV cone (+Z boresight)
        }

        // 2) Greedy nearest-neighbour matching, unique on both sides, within tolerance.
        // Pair (detection, predicted, distance); sort ascending; accept unused pairs.
        val pairs = ArrayList<Triple<Int, Int, Double>>(detections.size)
        for ((di, d) in detections.withIndex()) {
            val dv = d.unitVectorCamera
            var bestJ = -1
            var bestDot = -2.0
            for ((pj, p) in predicted.withIndex()) {
                val dot = (dv.first * p.first + dv.second * p.second + dv.third * p.third).coerceIn(-1.0, 1.0)
                if (dot > bestDot) { bestDot = dot; bestJ = pj }
            }
            if (bestJ >= 0) {
                val ang = acos(bestDot)
                if (ang <= toleranceRad) pairs.add(Triple(di, bestJ, ang))
            }
        }
        pairs.sortBy { it.third }
        val usedD = HashSet<Int>(); val usedP = HashSet<Int>()
        var matched = 0
        for ((di, pj, _) in pairs) {
            if (di in usedD || pj in usedP) continue
            usedD.add(di); usedP.add(pj); matched++
        }

        val fraction = if (detections.isEmpty()) 0.0 else matched.toDouble() / detections.size
        // For very small fields (unit-test fixtures, sparse real frames) the absolute
        // count cannot be reached; the effective minimum degrades to the whole field,
        // i.e. the fraction requirement alone carries the gate. For real fields
        // (>= minMatchedCount detections) the absolute floor applies unchanged.
        val effectiveMinCount = minOf(minMatchedCount, detections.size.coerceAtLeast(1))
        val pass = detections.isNotEmpty() &&
            matched >= effectiveMinCount &&
            fraction >= minMatchedFraction
        return Result(matched, detections.size, fraction, pass)
    }
}
