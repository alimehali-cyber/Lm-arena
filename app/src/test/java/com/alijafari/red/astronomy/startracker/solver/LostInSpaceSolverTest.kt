package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

/**
 * End-to-end LostInSpaceSolver tests, including the audit B5 regression test for
 * correspondence deduplication with default (blank) observation ids.
 */
class LostInSpaceSolverTest {

    private fun northPoleFixture(): Pair<List<com.alijafari.red.astronomy.startracker.catalog.CatalogStar>, QuadPatternIndex> {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            NP,0.0,90.0,2.0
            NP1,0.0,85.0,2.0
            NP2,10.0,85.0,2.0
            NP3,350.0,85.0,2.0
            NP4,20.0,83.0,2.1
            NP5,30.0,80.0,2.2
            NP6,200.0,40.0,2.5
        """.trimIndent()
        val stars = CatalogIngestor.parse(csv)
        val quadIndex = QuadPatternIndex(stars, maxSeparationRad = 40.0 * PI / 180.0)
        return Pair(stars, quadIndex)
    }

    @Test
    fun testSolveWithAssignedIds() {
        val (stars, quadIndex) = northPoleFixture()
        val observer = SyntheticSkyObserver()
        val observed = observer.observe(
            catalogStars = stars,
            groundTruthAttitude = Quaternion.identity(),
            fovLimitRad = 30.0 * PI / 180.0,
            noiseSigmaRad = 0.0,
            numFalseStars = 0,
            seed = 42L
        )
        assertTrue("fixture should yield >= 4 observations, got ${observed.observations.size}",
            observed.observations.size >= 4)

        val solver = LostInSpaceSolver(quadIndex, stars)
        val result = solver.solve(observed.observations)

        println("Assigned-id solve: success=${result.success}, inliers=${result.inlierCount}, " +
            "conf=${result.confidence}, err=${result.errorMessage ?: "none"}")
        assertTrue("solve should succeed with assigned ids: ${result.errorMessage}", result.success)
        assertNotNull(result.attitude)
    }

    @Test
    fun testSolveWithBlankDefaultIdsDoesNotCollapseDuringDedup() {
        // Audit finding B5 regression test: StarObservation.id defaults to "". The old
        // deduplication (distinctBy { it.observed.id }) collapsed ALL blank-id observations
        // into one correspondence and the solve failed with
        // "Too few correspondences after deduplication: 1" regardless of how many stars
        // were observed. This is the state any real detection->solver adapter would have
        // produced, since it constructs observations without catalog ids.
        val (stars, quadIndex) = northPoleFixture()
        val observer = SyntheticSkyObserver()
        val observed = observer.observe(
            catalogStars = stars,
            groundTruthAttitude = Quaternion.identity(),
            fovLimitRad = 30.0 * PI / 180.0,
            noiseSigmaRad = 0.0,
            numFalseStars = 0,
            seed = 42L
        )

        // Strip ids exactly like a real adapter path would (default "")
        val blankIdObservations = observed.observations.map { it.copy(id = "") }
        assertTrue(blankIdObservations.all { it.id.isBlank() })

        val solver = LostInSpaceSolver(quadIndex, stars)
        val result = solver.solve(blankIdObservations)

        println("Blank-id solve: success=${result.success}, inliers=${result.inlierCount}, " +
            "conf=${result.confidence}, err=${result.errorMessage ?: "none"}")
        assertTrue("solve with blank default ids must not collapse to one correspondence " +
            "(audit B5): ${result.errorMessage}", result.success)

        // And the solved attitude must actually be correct (identity ground truth)
        val q = result.attitude!!
        val dot = abs(q.w * 1.0 + q.x * 0.0 + q.y * 0.0 + q.z * 0.0)
            .coerceIn(-1.0, 1.0) // vs identity quaternion (1,0,0,0)
        val angleDeg = 2.0 * acos(dot) * 180.0 / PI
        println("Blank-id solve attitude error vs identity ground truth: $angleDeg deg")
        assertTrue("solved attitude should match ground truth within 0.001 deg, got $angleDeg",
            angleDeg < 0.001)
    }
}
