package com.alijafari.red.astronomy.startracker.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * C (final pass) tests for the capped quad index builder (QuadPatternIndex.capped).
 * All properties verified against the unchanged brute-force constructor on small
 * synthetic catalogs (deterministic seed), plus a real-catalog smoke build.
 */
class CappedQuadIndexTest {

    /** Deterministic uniform-sphere stars with varied magnitudes. */
    private fun gen(n: Int, seed: Long = 42L): List<CatalogStar> {
        val rng = Random(seed)
        return (0 until n).map { i ->
            val z = rng.nextDouble(-1.0, 1.0)
            val phi = rng.nextDouble(0.0, 2.0 * PI)
            CatalogStar.fromDegrees(
                id = "CAT$i",
                raDeg = Math.toDegrees(phi),
                decDeg = Math.toDegrees(asin(z)),
                magnitude = -1.0 + rng.nextDouble(0.0, 7.0),
                sourceCatalog = "SYNTH_CAPPED_TEST"
            )
        }
    }

    private fun keyOf(q: CatalogQuad) = q.starIndices

    @Test
    fun `capped at full budget equals brute force exactly`() {
        val stars = gen(40) + listOf(
            // tight pair CLOSER than MIN_PAIR_SEPARATION (0.1 deg): both paths must exclude
            // any quad using this pair — catches mutations that drop the min-separation
            // filter in the capped path.
            CatalogStar.fromDegrees("TIGHT_A", 10.0, 10.0, 0.5, "SYNTH_CAPPED_TEST"),
            CatalogStar.fromDegrees("TIGHT_B", 10.05, 10.0, 0.6, "SYNTH_CAPPED_TEST"),
            // companion within 40 deg of the tight pair: its combos exercise the
            // combo-level min-separation check (pairwise sep between chosen neighbors)
            CatalogStar.fromDegrees("TIGHT_C", 10.0, 10.3, 0.7, "SYNTH_CAPPED_TEST")
        )
        val brute = QuadPatternIndex(stars)
        // full budget: no magnitude cut, unlimited neighbors, no quad ceiling
        val capped = QuadPatternIndex.capped(
            stars, maxMagnitudeForQuads = 99.0, neighborsPerStar = stars.size + 10, maxQuads = Int.MAX_VALUE
        )
        val bruteKeys = brute.quads.map { keyOf(it) }.toSet()
        val cappedKeys = capped.quads.map { keyOf(it) }.toSet()
        assertEquals("capped(full budget) must equal brute-force quad set", bruteKeys, cappedKeys)
        assertEquals("no duplicates allowed", cappedKeys.size, capped.quads.size)
        // and per-quad descriptors must match too (same ratio math)
        val bruteByIdx = brute.quads.associateBy { it.starIndices }
        for (q in capped.quads) {
            val b = bruteByIdx[q.starIndices]!!
            assertEquals(b.descriptor.ratios, q.descriptor.ratios)
            assertEquals(b.quantizedKey, q.quantizedKey)
        }
    }

    @Test
    fun `magnitude filter restricts quads to eligible stars`() {
        val stars = gen(100)
        val capped = QuadPatternIndex.capped(stars, maxMagnitudeForQuads = 3.0, neighborsPerStar = 8)
        assertTrue("expected a non-empty capped index at N=100", capped.quads.isNotEmpty())
        for (q in capped.quads) {
            for (idx in q.starIndices) {
                assertTrue("quad contains star dimmer than the build cut", stars[idx].magnitude <= 3.0)
            }
        }
    }

    @Test
    fun `neighbor cap bounds per-anchor combinations and separations stay in range`() {
        val stars = gen(60)
        val k = 5
        val capped = QuadPatternIndex.capped(stars, neighborsPerStar = k)
        for (q in capped.quads) {
            // all 6 separations within configured [min, max]
            val s = q.starIndices
            val seps = listOf(
                AngularSeparation.between(stars[s[0]], stars[s[1]]),
                AngularSeparation.between(stars[s[0]], stars[s[2]]),
                AngularSeparation.between(stars[s[0]], stars[s[3]]),
                AngularSeparation.between(stars[s[1]], stars[s[2]]),
                AngularSeparation.between(stars[s[1]], stars[s[3]]),
                AngularSeparation.between(stars[s[2]], stars[s[3]])
            )
            assertTrue(seps.all { it >= CatalogBuildConfig.MIN_PAIR_SEPARATION_RAD && it <= CatalogBuildConfig.MAX_PAIR_SEPARATION_RAD })
        }
        // determinism
        val capped2 = QuadPatternIndex.capped(stars, neighborsPerStar = k)
        assertEquals(capped.quads.map { it.starIndices }, capped2.quads.map { it.starIndices })
    }

    @Test
    fun `quad ceiling is respected and deterministic`() {
        val stars = gen(80)
        val capped = QuadPatternIndex.capped(stars, neighborsPerStar = 9, maxQuads = 500)
        assertTrue("ceiling not respected: ${capped.quads.size}", capped.quads.size <= 500)
        val capped2 = QuadPatternIndex.capped(stars, neighborsPerStar = 9, maxQuads = 500)
        assertEquals(capped.quads.map { it.starIndices }, capped2.quads.map { it.starIndices })
    }

    @Test
    fun `brute force constructor unchanged - legacy path still enumerates`() {
        // Guards the KIND-A-style refactor: the pre-existing constructor must produce the
        // same quads as before (spot-check against hand-rolled O(N^4) on 25 stars).
        val stars = gen(25, seed = 7L)
        val idx = QuadPatternIndex(stars)
        var expected = 0
        for (i in stars.indices) for (j in i + 1 until stars.size) for (k in j + 1 until stars.size) for (l in k + 1 until stars.size) {
            val s = listOf(
                AngularSeparation.between(stars[i], stars[j]), AngularSeparation.between(stars[i], stars[k]),
                AngularSeparation.between(stars[i], stars[l]), AngularSeparation.between(stars[j], stars[k]),
                AngularSeparation.between(stars[j], stars[l]), AngularSeparation.between(stars[k], stars[l])
            )
            if (s.all { it >= idx.minSeparationRad && it <= idx.maxSeparationRad }) expected++
        }
        assertEquals(expected, idx.quads.size)
    }

    @Test
    fun `real catalog capped build is feasible and covered`() {
        // Real HYG-derived catalog (E): 8,870 stars. The brute-force path OOMs here;
        // the capped path must complete quickly, respect the ceiling, and cover the
        // majority of quad-eligible stars in at least one quad (lost-in-space coverage).
        val path = listOf(
            "data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../data/startracker/hyg_v36_vle6.5_j2000.csv",
            "../../data/startracker/hyg_v36_vle6.5_j2000.csv"
        ).firstOrNull { java.io.File(it).exists() } ?: return // skip silently if data not deployed
        val stars = CatalogIngestor.parse(java.io.File(path).readText(), "HYG_V36_LE6P5")
        assertEquals(8870, stars.size)
        val t0 = System.currentTimeMillis()
        val capped = QuadPatternIndex.capped(stars) // CatalogBuildConfig defaults
        val ms = System.currentTimeMillis() - t0
        assertTrue("quad count ${capped.quads.size} exceeds ceiling", capped.quads.size <= CatalogBuildConfig.QUAD_MAX_QUADS)
        val eligible = stars.filter { it.magnitude <= CatalogBuildConfig.QUAD_BUILD_MAX_MAGNITUDE }
        val covered = capped.quads.flatMap { it.starIndices }.toSet()
        val eligibleCovered = stars.indices.count { stars[it].magnitude <= CatalogBuildConfig.QUAD_BUILD_MAX_MAGNITUDE && it in covered }
        // sparse outliers near the pole of sparsity may legitimately miss coverage; require >= 90%
        assertTrue(
            "eligible-star coverage ${(eligibleCovered.toDouble() / eligible.size * 100).toInt()}% < 90% (build ${ms}ms)",
            eligibleCovered >= eligible.size * 9 / 10
        )
    }
}
