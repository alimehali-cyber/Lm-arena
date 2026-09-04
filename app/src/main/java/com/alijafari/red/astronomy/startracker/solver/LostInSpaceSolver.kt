package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.CatalogBuildConfig
import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex

/**
 * End-to-end lost-in-space solver wiring quad matching, RANSAC, attitude solve.
 * Returns either successful attitude + confidence + inlier count, or explicit "no solution".
 */

data class SolveResult(
    val success: Boolean,
    val attitude: Quaternion?,
    val inlierCount: Int,
    val confidence: Double, // 0..1
    val errorMessage: String? = null,
    // S2 full-field verification result (0.0/0 when the verifier did not run)
    val fullFieldMatched: Int = 0,
    val fullFieldFraction: Double = 0.0,
    val fullFieldMeanResidualArcsec: Double = 0.0,
    val fullFieldMedianResidualArcsec: Double = 0.0
)

class LostInSpaceSolver(
    val quadIndex: QuadPatternIndex,
    val catalogStars: List<CatalogStar>,
    val catalogStarsById: Map<String, CatalogStar> = catalogStars.associateBy { it.id },
    val quadBuilder: QuadCandidateBuilder = QuadCandidateBuilder(topN = CatalogBuildConfig.TOP_N_BRIGHTEST_FOR_QUAD_CANDIDATES),
    val matcher: CatalogMatcher = CatalogMatcher(quadIndex),
    val ransac: RansacOutlierRejector = RansacOutlierRejector(),
    val attitudeSolver: AttitudeSolver = AttitudeSolver(),
    // S2 (KIND-B): Tetra3-style full-field verification gate; null disables (legacy behavior).
    val fullFieldVerifier: FullFieldVerifier? = FullFieldVerifier()
) {

    fun solve(
        observations: List<StarObservation>,
        minStarsForSolve: Int = 4
    ): SolveResult {
        if (observations.size < minStarsForSolve) {
            return SolveResult(
                success = false,
                attitude = null,
                inlierCount = 0,
                confidence = 0.0,
                errorMessage = "Too few stars: ${observations.size} < $minStarsForSolve"
            )
        }

        // Step 1: Quad candidate formation.
        // D (final pass): local candidates mirror the capped index construction
        // (anchor + nearest neighbors from the bright pool). The legacy global
        // C(N,4)-of-top-N builder produces quads nearly DISJOINT from the capped
        // index (D1 finding: 0/20 lost-in-space solves on the real catalog).
        // Config-gated: USE_LOCAL_QUAD_CANDIDATES=false restores the legacy builder.
        val quadCandidates = if (com.alijafari.red.astronomy.startracker.catalog.CatalogBuildConfig.USE_LOCAL_QUAD_CANDIDATES) {
            quadBuilder.buildLocalCandidates(observations)
        } else {
            quadBuilder.buildCandidates(observations)
        }
        if (quadCandidates.isEmpty()) {
            return SolveResult(false, null, 0, 0.0, "No quad candidates from ${observations.size} observations")
        }

        // Step 2: Catalog matching with Pyramid verification
        val quadMatches = matcher.matchQuads(quadCandidates, catalogStarsById)
        if (quadMatches.isEmpty()) {
            return SolveResult(false, null, 0, 0.0, "No quad matches found from ${quadCandidates.size} candidates")
        }

        // Collect all correspondences from quad matches
        val allCorrespondences = mutableListOf<Correspondence>()
        for (match in quadMatches) {
            for ((obs, catStar) in match.correspondences) {
                allCorrespondences.add(
                    Correspondence(
                        observed = obs,
                        catalogStar = catStar,
                        catalogUnitVector = catStar.toUnitVector()
                    )
                )
            }
        }

        // Deduplicate correspondences (keep first).
        // Audit finding B5: this previously keyed on observed.id alone. StarObservation.id defaults
        // to "", so on any adapter path that does not assign ids (e.g. a future detection->solver
        // adapter), EVERY observation shared the key "" and the whole correspondence set collapsed
        // to a single entry ("Too few correspondences after deduplication: 1") even with a sky full
        // of stars. Key is now: explicit non-blank id when present (a star genuinely observed twice
        // still collapses to one correspondence), otherwise per-object identity, which cannot
        // collide by construction.
        val deduped = allCorrespondences.distinctBy { corr ->
            val oid = corr.observed.id
            if (oid.isNotBlank()) "id:$oid" else "ref:${System.identityHashCode(corr.observed)}"
        }

        if (deduped.size < 2) {
            return SolveResult(false, null, 0, 0.0, "Too few correspondences after deduplication: ${deduped.size}")
        }

        // Step 3: RANSAC outlier rejection
        val ransacResult = ransac.rejectOutliers(deduped, attitudeSolver)

        if (ransacResult.inlierCount < minStarsForSolve) {
            return SolveResult(false, null, ransacResult.inlierCount, 0.0, "RANSAC inliers ${ransacResult.inlierCount} < $minStarsForSolve")
        }

        val bestAttitude = ransacResult.bestAttitude
        if (bestAttitude == null) {
            return SolveResult(false, null, ransacResult.inlierCount, 0.0, "RANSAC failed to produce attitude")
        }

        // S2 (KIND-B): full-field verification before ANY confidence is granted.
        // Rationale: S1 showed 4-inlier chance collisions passing RANSAC at zero noise.
        var ff = fullFieldVerifier?.verify(bestAttitude, observations, catalogStars)
        var attitude = bestAttitude
        if (ff != null && ff.pass) {
            // W1: verify -> REFIT -> re-verify (Tetra3-style). The RANSAC attitude's
            // accuracy is bounded by its sloppy 2063-arcsec inlier threshold; a Davenport
            // q-method refit over ALL full-field matched pairs tightens it to the
            // centroid-noise scale. The refit is accepted only if the re-verification
            // still passes, so this can only improve accuracy, never smuggle a reject
            // through as a pass.
            // Convention: solveDavenportQMethod(first, second) returns R with
            // R*first ~= second; matchedPairs yields (catalog, detection), so the
            // refined rotation is CATALOG -> CAMERA, the solver's convention.
            val pairs = fullFieldVerifier!!.matchedPairs(attitude, observations, catalogStars)
            if (pairs.size >= 2) {
                val refined = attitudeSolver.solveDavenportQMethod(pairs, List(pairs.size) { 1.0 })
                val ff2 = fullFieldVerifier.verify(refined, observations, catalogStars)
                if (ff2.pass) { attitude = refined; ff = ff2 }
            }
        }
        if (ff != null && !ff.pass) {
            return SolveResult(
                success = false, attitude = null, inlierCount = ransacResult.inlierCount, confidence = 0.0,
                errorMessage = "Full-field verification failed: matched=${ff.matchedDetections}/${ff.totalDetections} fraction=${"%.3f".format(ff.fraction)}",
                fullFieldMatched = ff.matchedDetections, fullFieldFraction = ff.fraction,
                fullFieldMeanResidualArcsec = ff.meanMatchedDistanceRad * 206264.806,
                fullFieldMedianResidualArcsec = ff.medianMatchedDistanceRad * 206264.806
            )
        }

        // Confidence based on inlier count and ratio (legacy formula)
        val inlierRatio = ransacResult.inlierCount.toDouble() / deduped.size
        val legacyConfidence = (inlierRatio * 0.5 + (ransacResult.inlierCount.toDouble() / observations.size).coerceAtMost(1.0) * 0.5).coerceIn(0.0, 1.0)
        // W1 (KIND-B): when the S2 full-field verification PASSES, the verified fraction
        // of detections explained by the attitude is the direct correctness signal
        // (S1/S3: it separated every true from every false hypothesis; RANSAC inlier
        // count alone under-counts because correspondences come only from matched quads,
        // which cluster on a few anchor stars). Take the MAX with the legacy value -
        // verification can only strengthen, never weaken, a passing solve.
        val confidence = if (ff != null && ff.pass)
            maxOf(legacyConfidence, ff.fraction.coerceIn(0.0, 1.0))
        else legacyConfidence

        return SolveResult(
            success = true,
            attitude = attitude,
            inlierCount = ransacResult.inlierCount,
            confidence = confidence,
            errorMessage = null,
            fullFieldMatched = ff?.matchedDetections ?: 0,
            fullFieldFraction = ff?.fraction ?: 0.0,
            fullFieldMeanResidualArcsec = (ff?.meanMatchedDistanceRad ?: 0.0) * 206264.806,
            fullFieldMedianResidualArcsec = (ff?.medianMatchedDistanceRad ?: 0.0) * 206264.806
        )
    }
}
