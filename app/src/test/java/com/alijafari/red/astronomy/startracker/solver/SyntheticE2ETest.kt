package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.solver.synthetic.SyntheticSkyObserver
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * D (final pass): SYNTHETIC-SKY end-to-end lost-in-space test on the REAL catalog.
 * Chain under test: real HYG-derived CSV -> CatalogIngestor -> capped quad index (C
 * defaults) -> SyntheticSkyObserver (0.3 px centroid noise) -> LostInSpaceSolver with
 * LOCAL quad candidates (D fix) -> attitude error vs ground truth.
 *
 * All numbers produced/validated here are SYNTHETIC-SKY (no device data).
 * Evidence and full 8-cell ladder: docs/startracker/D_SYNTHETIC_E2E.md,
 * evidence/D_SYNTHETIC_E2E_2026-09-04.txt.
 */
class SyntheticE2ETest {

    private fun loadRealCatalog(): List<com.alijafari.red.astronomy.startracker.catalog.CatalogStar>? {
        val path = listOf(
            "data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../../data/startracker/hyg_v36_vle6.5_j2000.csv"
        ).firstOrNull { java.io.File(it).exists() } ?: return null
        return CatalogIngestor.parse(java.io.File(path).readText(), "HYG_V36_LE6P5")
    }

    @Test
    fun `lost in space solve on real catalog with local candidates`() {
        val stars = loadRealCatalog() ?: return // skip when catalog not deployed
        assertTrue("real catalog expected 8870 stars, got ${stars.size}", stars.size == 8870)
        val quadIndex = QuadPatternIndex.capped(stars)
        val solver = LostInSpaceSolver(quadIndex, stars)

        // deterministic attitude #1 from seed 20260904 (see SyntheticE2EProbe)
        val rng = Random(20260904)
        var qTrue = Quaternion(1.0, 0.0, 0.0, 0.0)
        repeat(2) { t ->
            val u1 = rng.nextDouble(); val u2 = rng.nextDouble() * 2 * PI; val u3 = rng.nextDouble() * 2 * PI
            val s1 = sqrt(1.0 - u1); val s2 = sqrt(u1)
            qTrue = Quaternion(s1 * sin(u2), s1 * cos(u2), s2 * sin(u3), s2 * cos(u3))
        }
        val obs = SyntheticSkyObserver().observe(
            catalogStars = stars,
            groundTruthAttitude = qTrue,
            fovLimitRad = Math.toRadians(31.75),
            noiseSigmaRad = Math.toRadians(0.3 * 57.0 / 3600.0), // 0.3 px @ ~4000px/63.5deg
            numFalseStars = 0,
            seed = 1001L
        )
        val res = solver.solve(obs.observations)
        assertTrue("solve failed: ${res.errorMessage}", res.success)
        val est = res.attitude!!
        var d = qTrue.w * est.w + qTrue.x * est.x + qTrue.y * est.y + qTrue.z * est.z
        val errArcmin = Math.toDegrees(2.0 * acos(abs(d).coerceIn(0.0, 1.0))) * 60.0
        assertTrue(
            // D ladder: median 0.31' @0.3px; generous 30' bound keeps the test about
            // correct-lock vs false-lock, not about exact accuracy
            "attitude error $errArcmin' looks like a FALSE LOCK (D ladder median 0.31' @0.3px)",
            errArcmin < 30.0
        )
    }
}
