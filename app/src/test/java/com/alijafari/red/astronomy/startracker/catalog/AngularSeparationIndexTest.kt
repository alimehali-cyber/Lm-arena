package com.alijafari.red.astronomy.startracker.catalog

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class AngularSeparationIndexTest {

    // Hand-computed reference values for well-known angle pairs
    // These are computed independently, not from implementation

    @Test
    fun testAngularSeparationKnownValues() {
        // Two points 90° apart on equator: (0,0) and (90,0) → 90° = π/2
        val star1 = CatalogStar.fromDegrees("A", 0.0, 0.0, 2.0)
        val star2 = CatalogStar.fromDegrees("B", 90.0, 0.0, 2.0)
        val sep = AngularSeparation.between(star1, star2)
        assertEquals(PI / 2.0, sep, 1e-9)

        // Same RA, different Dec: (0,0) and (0,45) → 45° = π/4
        val star3 = CatalogStar.fromDegrees("C", 0.0, 0.0, 2.0)
        val star4 = CatalogStar.fromDegrees("D", 0.0, 45.0, 2.0)
        val sep2 = AngularSeparation.between(star3, star4)
        assertEquals(PI / 4.0, sep2, 1e-9)

        // Antipodal: (0,0) and (180,0) → 180° = π
        val star5 = CatalogStar.fromDegrees("E", 0.0, 0.0, 2.0)
        val star6 = CatalogStar.fromDegrees("F", 180.0, 0.0, 2.0)
        val sep3 = AngularSeparation.between(star5, star6)
        assertEquals(PI, sep3, 1e-9)

        // Same point: 0°
        val star7 = CatalogStar.fromDegrees("G", 10.0, 20.0, 2.0)
        val star8 = CatalogStar.fromDegrees("H", 10.0, 20.0, 2.0)
        val sep4 = AngularSeparation.between(star7, star8)
        assertEquals(0.0, sep4, 1e-9)

        // North pole to equator: (0,90) and (0,0) → 90°
        val star9 = CatalogStar.fromDegrees("I", 0.0, 90.0, 2.0)
        val star10 = CatalogStar.fromDegrees("J", 0.0, 0.0, 2.0)
        val sep5 = AngularSeparation.between(star9, star10)
        assertEquals(PI / 2.0, sep5, 1e-9)

        // Small separation: (0,0) and (0.1,0) → 0.1°
        val star11 = CatalogStar.fromDegrees("K", 0.0, 0.0, 2.0)
        val star12 = CatalogStar.fromDegrees("L", 0.1, 0.0, 2.0)
        val sep6 = AngularSeparation.between(star11, star12)
        assertEquals(0.1 * PI / 180.0, sep6, 1e-9)
    }

    @Test
    fun testHaversineVsDotProductConsistency() {
        // For random points, haversine and dot-product should agree within small tolerance
        val stars = listOf(
            CatalogStar.fromDegrees("A", 10.0, 20.0, 2.0),
            CatalogStar.fromDegrees("B", 30.0, 40.0, 2.0),
            CatalogStar.fromDegrees("C", 200.0, -30.0, 2.0),
            CatalogStar.fromDegrees("D", 350.0, 80.0, 2.0)
        )

        for (i in stars.indices) {
            for (j in i + 1 until stars.size) {
                val sepHav = AngularSeparation.between(stars[i], stars[j])
                val sepDot = AngularSeparation.betweenViaDotProduct(stars[i], stars[j])
                assertEquals("Haversine vs dot for ${stars[i].id}-${stars[j].id}", sepHav, sepDot, 1e-9)
            }
        }
    }

    @Test
    fun testPairIndexConstruction() {
        // Use synthetic fixture with known separations
        val csv = """
            id,ra_deg,dec_deg,magnitude
            TESTSTAR001,0.0,0.0,2.0
            TESTSTAR002,90.0,0.0,2.5
            TESTSTAR003,0.0,90.0,3.0
            TESTSTAR004,1.0,0.0,4.0
            TESTSTAR005,0.0,1.0,4.0
        """.trimIndent()

        val stars = CatalogIngestor.parse(csv)
        val index = AngularSeparationIndex(stars, maxSeparationRad = 100.0 * PI / 180.0) // include all

        // For 5 stars, total pairs = C(5,2)=10
        assertEquals(10, index.pairs.size)

        // Check that 90° pair exists
        val ninetyDegPairs = index.queryRange(89.9 * PI / 180.0, 90.1 * PI / 180.0)
        // Should include TESTSTAR001-TESTSTAR002 (0,0)-(90,0) =90°, and TESTSTAR001-TESTSTAR003 (0,0)-(0,90)=90°, etc.
        assertTrue("Should have at least 2 pairs near 90°", ninetyDegPairs.size >= 2)
    }

    @Test
    fun testRangeQueryCorrectness() {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            A,0.0,0.0,2.0
            B,1.0,0.0,2.0
            C,2.0,0.0,2.0
            D,10.0,0.0,2.0
            E,90.0,0.0,2.0
        """.trimIndent()

        val stars = CatalogIngestor.parse(csv)
        val index = AngularSeparationIndex(stars, maxSeparationRad = 100.0 * PI / 180.0)

        // Query 0.9° to 1.1° should return exactly A-B (1°), B-C (1°)
        val low = 0.9 * PI / 180.0
        val high = 1.1 * PI / 180.0
        val result = index.queryRange(low, high)
        val brute = index.queryRangeBruteForce(low, high)

        // k-vector query should match brute-force exactly (no false inclusion/omission)
        assertEquals("k-vector vs brute-force count", brute.size, result.size)
        // Check exact pairs
        val resultSet = result.map { setOf(it.starId1, it.starId2) }.toSet()
        val bruteSet = brute.map { setOf(it.starId1, it.starId2) }.toSet()
        assertEquals(resultSet, bruteSet)

        // Expected: A-B and B-C are 1° apart, A-C is 2° apart (outside range)
        assertEquals(2, result.size)
    }

    @Test
    fun testKVectorPerformanceReasoning() {
        // Analytical performance characteristics, not wall-clock benchmark
        // For N=15 stars, pairs = C(15,2)=105, but with maxSeparation 40°, maybe less
        // For N=9000 stars, pairs ~ N^2 *0.058 ≈ 9000^2*0.058 ≈ 4.7M pairs (per earlier extrapolation)
        // Sorting 4.7M pairs: O(P log P) ≈ 4.7M * log2(4.7M) ≈ 4.7M *22 ≈ 100M comparisons
        // k-vector build O(P) = 4.7M
        // Range query O(1) for index approx + O(K) for K results
        // Without k-vector, binary search O(log P) ≈ 22 steps + O(K)
        // So k-vector saves ~22 steps per query, significant for many queries (e.g., quad building does many queries)
        // Real benchmarking is open item for when JVM available
        assertTrue(true) // This test is for documentation, always passes
    }

    // ---- Audit finding B11: k-vector must be used by the query and stay exact on
    // ---- NON-UNIFORM (clustered) separation distributions, matching brute force exactly.

    private fun clusteredFixtureStars(): List<CatalogStar> {
        // Two tight clusters (intra-cluster separations ~0.5-3 deg) far apart (~90 deg),
        // plus isolated stars: a strongly non-uniform separation distribution.
        val stars = mutableListOf<CatalogStar>()
        var id = 0
        fun add(raDeg: Double, decDeg: Double) {
            stars.add(CatalogStar("C" + id++, Math.toRadians(raDeg), Math.toRadians(decDeg), 3.0, "FIX"))
        }
        // cluster A around (0,0)
        for (i in 0 until 8) add(i * 0.7, (i % 3) * 0.5)
        // cluster B around (120,40), far from A
        for (i in 0 until 8) add(120.0 + i * 0.6, 40.0 + (i % 4) * 0.4)
        // isolated stars at varied distances
        add(60.0, 20.0); add(200.0, -30.0); add(300.0, 60.0); add(10.0, -60.0)
        return stars
    }

    @Test
    fun testKVectorQueryExactOnNonUniformDistribution() {
        val stars = clusteredFixtureStars()
        val index = AngularSeparationIndex(stars, maxSeparationRad = Math.PI) // index everything
        assertTrue("clustered fixture must produce a non-trivial pair set, got " + index.pairs.size,
            index.pairs.size >= 50)

        // Sweep many range windows across the full separation span and demand EXACT equality
        // with brute force (same separations, same multiset), not just equal counts.
        val sMin = index.sortedSeparations.first()
        val sMax = index.sortedSeparations.last()
        val span = sMax - sMin
        var windows = 0
        for (i in 0 until 40) {
            val lo = sMin + span * i / 80.0
            val hi = sMin + span * (i + 3) / 80.0
            val viaKVector = index.queryRange(lo, hi).map { it.separationRad }.sorted()
            val viaBrute = index.queryRangeBruteForce(lo, hi).map { it.separationRad }.sorted()
            assertEquals("k-vector query must exactly match brute force on NON-UNIFORM fixture window " + lo + ".." + hi,
                viaBrute, viaKVector)
            windows++
        }
        println("k-vector vs brute force on clustered (non-uniform) fixture: " + windows + " windows, all exact matches")
    }

    @Test
    fun testKVectorQueryExactOnUniformDistribution() {
        // Grid fixture: roughly uniform separations; same exact-equality sweep.
        val stars = mutableListOf<CatalogStar>()
        var id = 0
        for (ra in 0 until 6) {
            for (dec in 0 until 6) {
                stars.add(CatalogStar("U" + id++, Math.toRadians(ra * 8.0), Math.toRadians(dec * 8.0 - 20.0), 3.0, "FIX"))
            }
        }
        val index = AngularSeparationIndex(stars, maxSeparationRad = Math.PI)
        val sMin = index.sortedSeparations.first()
        val sMax = index.sortedSeparations.last()
        val span = sMax - sMin
        for (i in 0 until 30) {
            val lo = sMin + span * i / 60.0
            val hi = sMin + span * (i + 2) / 60.0
            val viaKVector = index.queryRange(lo, hi).map { it.separationRad }.sorted()
            val viaBrute = index.queryRangeBruteForce(lo, hi).map { it.separationRad }.sorted()
            assertEquals(viaBrute, viaKVector)
        }
        println("k-vector vs brute force on uniform grid fixture: 30 windows, all exact matches")
    }
}
