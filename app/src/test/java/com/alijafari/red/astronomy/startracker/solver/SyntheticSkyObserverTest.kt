package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class SyntheticSkyObserverTest {

    @Test
    fun testGeneratorRoundTrip() {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            A,0.0,0.0,2.0
            B,10.0,0.0,2.0
            C,0.0,10.0,2.0
            D,10.0,10.0,2.0
            E,20.0,20.0,2.0
        """.trimIndent()

        val stars = CatalogIngestor.parse(csv)
        val observer = SyntheticSkyObserver()

        // Known attitude: identity (camera looks at +Z, which corresponds to catalog direction?)
        // For simplicity, use identity quaternion
        val identity = Quaternion.identity()

        val fov = 30.0 * PI / 180.0 // 30° half-FOV = 60° full

        val result = observer.observe(
            catalogStars = stars,
            groundTruthAttitude = identity,
            fovLimitRad = fov,
            noiseSigmaRad = 0.0,
            numFalseStars = 0,
            seed = 42L
        )

        // With identity attitude, catalog stars near (0,0) should be within FOV if their angle from +Z <30°
        // Catalog star at (0,0) has unit vector (1,0,0) — angle from +Z (0,0,1) is 90°, so outside 30° FOV
        // So we need stars near north pole (0,90) which is (0,0,1) = +Z
        // Let's test with stars near north pole
        val csv2 = """
            id,ra_deg,dec_deg,magnitude
            NP,0.0,90.0,2.0
            NP1,0.0,85.0,2.0
            NP2,10.0,85.0,2.0
            EQ,0.0,0.0,2.0
        """.trimIndent()

        val stars2 = CatalogIngestor.parse(csv2)
        val result2 = observer.observe(
            catalogStars = stars2,
            groundTruthAttitude = identity,
            fovLimitRad = fov,
            noiseSigmaRad = 0.0,
            numFalseStars = 0
        )

        // NP at (0,90) = (0,0,1) = boresight, should be within FOV
        assertTrue("North pole should be visible", result2.observations.size >= 1)

        // Verify pairwise separations match catalog within noise tolerance
        // For zero noise, observed unit vectors should have same pairwise separations as catalog
        for (i in result2.observations.indices) {
            for (j in i + 1 until result2.observations.size) {
                val obs1 = result2.observations[i]
                val obs2 = result2.observations[j]
                val dotObs = (obs1.x * obs2.x + obs1.y * obs2.y + obs1.z * obs2.z).coerceIn(-1.0, 1.0)
                val sepObs = acos(dotObs)

                val cat1 = result2.trueCorrespondences[obs1.id]!!
                val cat2 = result2.trueCorrespondences[obs2.id]!!
                val sepCat = com.alijafari.red.astronomy.startracker.catalog.AngularSeparation.between(cat1, cat2)

                assertEquals("Observed sep should match catalog sep for zero noise", sepCat, sepObs, 1e-6)
            }
        }
    }

    @Test
    fun testFalseStarInjection() {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            A,0.0,90.0,2.0
            B,0.0,85.0,2.0
            C,10.0,85.0,2.0
        """.trimIndent()

        val stars = CatalogIngestor.parse(csv)
        val observer = SyntheticSkyObserver()
        val result = observer.observe(
            catalogStars = stars,
            groundTruthAttitude = Quaternion.identity(),
            fovLimitRad = 30.0 * PI / 180.0,
            noiseSigmaRad = 0.0,
            numFalseStars = 5,
            seed = 123L
        )

        assertEquals(3 + 5, result.observations.size)
        assertEquals(3, result.trueCorrespondences.size)
    }
}
