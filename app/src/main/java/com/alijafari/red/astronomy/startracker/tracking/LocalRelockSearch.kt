package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.StarObservation
import kotlin.math.*

/**
 * Local re-lock vs full blind search.
 * Given current predicted attitude (prior) and new observations, narrow catalog search
 * to only candidate stars/quads near predicted pointing direction, before falling back
 * to full blind LostInSpaceSolver if constrained search fails.
 */

class LocalRelockSearch(
    val quadIndex: QuadPatternIndex,
    val catalogStars: List<CatalogStar>,
    val catalogStarsById: Map<String, CatalogStar> = catalogStars.associateBy { it.id },
    val fullBlindSolver: LostInSpaceSolver,
    val searchRadiusRad: Double = 20.0 * PI / 180.0 // 20° radius around predicted pointing, conservative
) {

    data class LocalSearchResult(
        val success: Boolean,
        val attitude: Quaternion?,
        val candidateSetSizeReduction: String, // e.g., "full N=1000, local M=50"
        val usedLocal: Boolean,
        val fallbackToFull: Boolean,
        /**
         * The REAL underlying SolveResult from whichever solver actually ran (local or full
         * blind fallback), with its actual inlier count and confidence. Audit finding B7:
         * callers previously had to invent these numbers because they were discarded here.
         */
        val solveResult: com.alijafari.red.astronomy.startracker.solver.SolveResult? = null
    )

    /**
     * Attempt local re-lock with prior.
     * If fails, fallback to full blind solver.
     */
    fun search(
        observations: List<StarObservation>,
        predictedAttitude: Quaternion
    ): LocalSearchResult {
        // Step 1: Narrow catalog to stars near predicted pointing
        // Predicted boresight in catalog frame = inverse rotate camera boresight (0,0,1) by predicted attitude
        val boresightCamera = Triple(0.0, 0.0, 1.0)
        val boresightCatalog = predictedAttitude.conjugate().rotateVector(boresightCamera)

        val nearbyStars = catalogStars.filter { star ->
            val starVec = star.toUnitVector()
            val dot = (starVec.first * boresightCatalog.first + starVec.second * boresightCatalog.second + starVec.third * boresightCatalog.third).coerceIn(-1.0, 1.0)
            val angle = acos(dot)
            angle <= searchRadiusRad
        }

        // If too few nearby stars, fallback to full
        if (nearbyStars.size < 4) {
            val fullResult = fullBlindSolver.solve(observations)
            return LocalSearchResult(
                success = fullResult.success,
                attitude = fullResult.attitude,
                candidateSetSizeReduction = "full blind (nearby too few: ${nearbyStars.size})",
                usedLocal = false,
                fallbackToFull = true,
                solveResult = fullResult
            )
        }

        // Build local quad index from nearby stars only (for candidate set size comparison)
        val localQuadIndex = QuadPatternIndex(nearbyStars, maxSeparationRad = 40.0 * PI / 180.0)
        val fullQuadCount = quadIndex.quads.size
        val localQuadCount = localQuadIndex.quads.size

        // Attempt local solve using nearby stars only
        val localSolver = LostInSpaceSolver(
            quadIndex = localQuadIndex,
            catalogStars = nearbyStars,
            catalogStarsById = nearbyStars.associateBy { it.id }
        )

        val localResult = localSolver.solve(observations)

        if (localResult.success) {
            return LocalSearchResult(
                success = true,
                attitude = localResult.attitude,
                candidateSetSizeReduction = "full N=$fullQuadCount, local M=$localQuadCount, reduction ${(1 - localQuadCount.toDouble()/fullQuadCount)*100}%",
                usedLocal = true,
                fallbackToFull = false,
                solveResult = localResult
            )
        }

        // Local failed, fallback to full blind
        val fullResult = fullBlindSolver.solve(observations)
        return LocalSearchResult(
            success = fullResult.success,
            attitude = fullResult.attitude,
            candidateSetSizeReduction = "local failed (M=$localQuadCount), full N=$fullQuadCount, fallback",
            usedLocal = false,
            fallbackToFull = true,
            solveResult = fullResult
        )
    }
}
