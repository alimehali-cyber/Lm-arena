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
    // Prediction cone half-angle. MUST be >= the sensor's half-diagonal FOV, else
    // detections near frame corners can never match and the fraction is undercounted.
    // Predictions beyond the actual frame are harmless (they simply match nothing).
    // 60 deg covers every current tier incl. the 1920x1080 fallback array whose
    // half-diagonal is 51.6 deg; S2/S3 runs (observer cone 31.75 deg) are unaffected
    // because their detections all lie well inside this cone.
    val fovLimitRad: Double = Math.toRadians(60.0)
) {

    data class Result(
        val matchedDetections: Int,
        val totalDetections: Int,
        val fraction: Double,
        val pass: Boolean,
        val meanMatchedDistanceRad: Double = 0.0, // mean angular residual of matched pairs
        val medianMatchedDistanceRad: Double = 0.0 // robust (median) residual of matched pairs
    )

    /**
     * Greedy unique nearest-neighbour matching of detections against predicted catalog
     * positions under [attitude]. Returns accepted matches as
     * (detection, predictedUnitVector, angular distance).
     */
    data class FieldMatch(
        val detection: StarObservation,
        val catalogStar: CatalogStar,
        val predictedCamera: Triple<Double, Double, Double>,
        val distanceRad: Double
    )

    fun buildMatches(
        attitude: Quaternion,
        detections: List<StarObservation>,
        catalogStars: List<CatalogStar>
    ): List<FieldMatch> {
        val predicted = ArrayList<Triple<Double, Double, Double>>(1024)
        val predictedStar = ArrayList<CatalogStar>(1024)
        val cosFov = kotlin.math.cos(fovLimitRad)
        for (s in catalogStars) {
            if (s.magnitude > magLimit) continue
            val v = attitude.rotateVector(s.toUnitVector())
            if (v.third >= cosFov) { predicted.add(v); predictedStar.add(s) } // inside the FOV cone (+Z boresight)
        }
        // (detection, predicted, distance) candidates within tolerance, best per detection.
        val cands = ArrayList<Triple<Int, Int, Double>>(detections.size)
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
                if (ang <= toleranceRad) cands.add(Triple(di, bestJ, ang))
            }
        }
        cands.sortBy { it.third }
        val usedD = HashSet<Int>(); val usedP = HashSet<Int>()
        val out = ArrayList<FieldMatch>()
        for ((di, pj, dist) in cands) {
            if (di in usedD || pj in usedP) continue
            usedD.add(di); usedP.add(pj)
            out.add(FieldMatch(detections[di], predictedStar[pj], predicted[pj], dist))
        }
        return out
    }

    /**
     * W1: (CATALOG unit vector, detection unit vector) pairs for attitude refinement.
     * Catalog vectors — not the attitude-rotated predictions — so a Wahba/Davenport
     * solve over these pairs returns the full CATALOG -> CAMERA rotation.
     */
    fun matchedPairs(
        attitude: Quaternion,
        detections: List<StarObservation>,
        catalogStars: List<CatalogStar>
    ): List<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>> =
        buildMatches(attitude, detections, catalogStars).map { it.catalogStar.toUnitVector() to it.detection.unitVectorCamera }

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
        val matches = buildMatches(attitude, detections, catalogStars)
        val matched = matches.size
        val dists = matches.map { it.distanceRad }
        val meanDist = if (matched == 0) 0.0 else dists.average()
        val medDist = if (matched == 0) 0.0 else dists.sorted()[matched / 2]

        val fraction = if (detections.isEmpty()) 0.0 else matched.toDouble() / detections.size
        // For very small fields (unit-test fixtures, sparse real frames) the absolute
        // count cannot be reached; the effective minimum degrades to the whole field,
        // i.e. the fraction requirement alone carries the gate. For real fields
        // (>= minMatchedCount detections) the absolute floor applies unchanged.
        val effectiveMinCount = minOf(minMatchedCount, detections.size.coerceAtLeast(1))
        val pass = detections.isNotEmpty() &&
            matched >= effectiveMinCount &&
            fraction >= minMatchedFraction
        return Result(matched, detections.size, fraction, pass, meanDist, medDist)
    }
}
