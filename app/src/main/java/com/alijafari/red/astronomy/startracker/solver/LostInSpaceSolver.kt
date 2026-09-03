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
    val errorMessage: String? = null
)

class LostInSpaceSolver(
    val quadIndex: QuadPatternIndex,
    val catalogStars: List<CatalogStar>,
    val catalogStarsById: Map<String, CatalogStar> = catalogStars.associateBy { it.id },
    val quadBuilder: QuadCandidateBuilder = QuadCandidateBuilder(topN = CatalogBuildConfig.TOP_N_BRIGHTEST_FOR_QUAD_CANDIDATES),
    val matcher: CatalogMatcher = CatalogMatcher(quadIndex),
    val ransac: RansacOutlierRejector = RansacOutlierRejector(),
    val attitudeSolver: AttitudeSolver = AttitudeSolver()
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

        // Step 1: Quad candidate formation
        val quadCandidates = quadBuilder.buildCandidates(observations)
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

        // Confidence based on inlier count and ratio
        val inlierRatio = ransacResult.inlierCount.toDouble() / deduped.size
        val confidence = (inlierRatio * 0.5 + (ransacResult.inlierCount.toDouble() / observations.size).coerceAtMost(1.0) * 0.5).coerceIn(0.0, 1.0)

        return SolveResult(
            success = true,
            attitude = bestAttitude,
            inlierCount = ransacResult.inlierCount,
            confidence = confidence,
            errorMessage = null
        )
    }
}
