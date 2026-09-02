package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.AngularSeparation
import com.alijafari.red.astronomy.startracker.catalog.CatalogBuildConfig
import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import kotlin.math.*

/**
 * Catalog matcher: queries QuadPatternIndex, applies Pyramid four-star consistency verification.
 * Pyramid: verify ALL 6 pairwise angular separations between observed quad and candidate catalog quad
 * agree within epsilon (pyramidConsistencyToleranceRad).
 */

data class QuadMatch(
    val observedQuad: QuadCandidateBuilder.ObservationQuad,
    val catalogQuad: com.alijafari.red.astronomy.startracker.catalog.CatalogQuad,
    val correspondences: List<Pair<StarObservation, CatalogStar>> // 4 correspondences
)

class CatalogMatcher(
    val quadIndex: QuadPatternIndex,
    val pyramidToleranceRad: Double = CatalogBuildConfig.PYRAMID_CONSISTENCY_TOLERANCE_RAD
) {

    /**
     * For each candidate observed quad, compute descriptor, hash-lookup candidate catalog quads,
     * and apply Pyramid four-star consistency check.
     * Returns list of matches (may include false matches, to be filtered by RANSAC).
     */
    fun matchQuads(
        observedQuads: List<QuadCandidateBuilder.ObservationQuad>,
        catalogStarsById: Map<String, CatalogStar>
    ): List<QuadMatch> {
        val matches = mutableListOf<QuadMatch>()

        for (obsQuad in observedQuads) {
            // Compute descriptor for observed quad from unit vectors
            val unitVectors = obsQuad.observations.map { it.unitVectorCamera }
            val observedDescriptor = quadIndex.computeDescriptorFromUnitVectors(unitVectors)

            // Hash lookup with neighbor bins for noise tolerance
            val candidates = quadIndex.lookupCandidatesWithNeighborBins(observedDescriptor)

            for (catQuad in candidates) {
                // Pyramid verification: check all 6 pairwise separations agree within epsilon
                if (verifyPyramidConsistency(obsQuad, catQuad, catalogStarsById)) {
                    // Build correspondences: need to match which observed star corresponds to which catalog star
                    // For simplicity, assume order correspondence by sorting? Actually need to solve assignment.
                    // For quad, we have 4 observed and 4 catalog stars, but we don't know which maps to which.
                    // We can try all 24 permutations and find best matching (minimum separation error)
                    val bestCorrespondences = findBestCorrespondence(obsQuad, catQuad, catalogStarsById)
                    if (bestCorrespondences != null) {
                        matches.add(
                            QuadMatch(
                                observedQuad = obsQuad,
                                catalogQuad = catQuad,
                                correspondences = bestCorrespondences
                            )
                        )
                    }
                }
            }
        }

        return matches
    }

    private fun verifyPyramidConsistency(
        obsQuad: QuadCandidateBuilder.ObservationQuad,
        catQuad: com.alijafari.red.astronomy.startracker.catalog.CatalogQuad,
        catalogStarsById: Map<String, CatalogStar>
    ): Boolean {
        // Compute 6 separations for observed quad (from unit vectors)
        val obsSeps = mutableListOf<Double>()
        for (i in obsQuad.observations.indices) {
            for (j in i + 1 until obsQuad.observations.size) {
                val v1 = obsQuad.observations[i].unitVectorCamera
                val v2 = obsQuad.observations[j].unitVectorCamera
                val dot = (v1.first * v2.first + v1.second * v2.second + v1.third * v2.third).coerceIn(-1.0, 1.0)
                obsSeps.add(acos(dot))
            }
        }

        // Compute 6 separations for catalog quad
        val catStars = catQuad.starIds.mapNotNull { catalogStarsById[it] }
        if (catStars.size != 4) return false

        val catSeps = mutableListOf<Double>()
        for (i in catStars.indices) {
            for (j in i + 1 until catStars.size) {
                catSeps.add(AngularSeparation.between(catStars[i], catStars[j]))
            }
        }

        // For Pyramid, we need to check if there exists a permutation where all 6 separations match within tolerance
        // Since we have 6 separations but order may differ, we sort both and compare? Actually Pyramid requires
        // checking all 6 pairs correspond, not just sorted list. But for initial filter, we can check sorted lists
        // agree within tolerance, then do detailed permutation check in findBestCorrespondence.

        val obsSorted = obsSeps.sorted()
        val catSorted = catSeps.sorted()

        for (k in obsSorted.indices) {
            if (abs(obsSorted[k] - catSorted[k]) > pyramidToleranceRad) {
                return false
            }
        }

        return true
    }

    private fun findBestCorrespondence(
        obsQuad: QuadCandidateBuilder.ObservationQuad,
        catQuad: com.alijafari.red.astronomy.startracker.catalog.CatalogQuad,
        catalogStarsById: Map<String, CatalogStar>
    ): List<Pair<StarObservation, CatalogStar>>? {
        val catStars = catQuad.starIds.mapNotNull { catalogStarsById[it] }
        if (catStars.size != 4) return null

        // Try all 24 permutations of catalog stars to match observed order
        val perms = permutations(catStars)

        var bestPerm: List<CatalogStar>? = null
        var bestError = Double.MAX_VALUE

        for (perm in perms) {
            var maxError = 0.0
            var totalError = 0.0
            var valid = true

            // For this permutation, check all 6 pairwise separations
            for (i in obsQuad.observations.indices) {
                for (j in i + 1 until obsQuad.observations.size) {
                    val obsV1 = obsQuad.observations[i].unitVectorCamera
                    val obsV2 = obsQuad.observations[j].unitVectorCamera
                    val obsDot = (obsV1.first * obsV2.first + obsV1.second * obsV2.second + obsV1.third * obsV2.third).coerceIn(-1.0, 1.0)
                    val obsSep = acos(obsDot)

                    val catSep = AngularSeparation.between(perm[i], perm[j])
                    val err = abs(obsSep - catSep)
                    totalError += err
                    if (err > maxError) maxError = err
                    if (err > pyramidToleranceRad) {
                        valid = false
                        break
                    }
                }
                if (!valid) break
            }

            if (valid && totalError < bestError) {
                bestError = totalError
                bestPerm = perm
            }
        }

        if (bestPerm == null) return null

        // Build correspondences
        return obsQuad.observations.zip(bestPerm).map { Pair(it.first, it.second) }
    }

    private fun <T> permutations(list: List<T>): List<List<T>> {
        if (list.isEmpty()) return listOf(emptyList())
        val result = mutableListOf<List<T>>()
        for (i in list.indices) {
            val element = list[i]
            val rest = list.filterIndexed { idx, _ -> idx != i }
            for (perm in permutations(rest)) {
                result.add(listOf(element) + perm)
            }
        }
        return result
    }
}
