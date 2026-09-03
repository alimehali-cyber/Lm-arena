package com.alijafari.red.astronomy.startracker.catalog

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.PI

class CatalogSerializerTest {

    // NOTE (2026-09-03 pass 2, item B1): the numbers asserted below are ESTIMATOR OUTPUT
    // (CatalogSerializer.estimateSizeOf under the Task-4 "N*19600/4" quad model), NOT
    // measurements of a serialized catalog. Real measurements now exist and show the quad
    // model is ~2,900x optimistic: docs/startracker/evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt
    // (pairs at 9,110 stars measured 4,851,922 / 74.5 MiB; quads measured-extrapolated
    // ~1.28e11 = ~10.2 TB). This test still guards the estimator's Long arithmetic.


    @Test
    fun testRoundTripSerialization() {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            TESTSTAR001,0.0,0.0,2.0
            TESTSTAR002,90.0,0.0,2.5
            TESTSTAR003,0.0,90.0,3.0
            TESTSTAR004,1.0,0.0,4.0
            TESTSTAR005,0.0,1.0,4.0
        """.trimIndent()

        val stars = CatalogIngestor.parse(csv)
        val pairIndex = AngularSeparationIndex(stars, maxSeparationRad = 100.0 * PI / 180.0)
        val quadIndex = QuadPatternIndex(stars, maxSeparationRad = 100.0 * PI / 180.0)

        val bytes = CatalogSerializer.serialize(stars, pairIndex.pairs, quadIndex.quads)
        println("Serialized fixture: ${stars.size} stars, ${pairIndex.pairs.size} pairs, ${quadIndex.quads.size} quads, ${bytes.size} bytes")

        val deserialized = CatalogSerializer.deserialize(bytes)

        assertEquals(stars.size, deserialized.stars.size)
        assertEquals(pairIndex.pairs.size, deserialized.pairs.size)
        assertEquals(quadIndex.quads.size, deserialized.quads.size)

        // Check positions to full precision
        for (i in stars.indices) {
            assertEquals(stars[i].id, deserialized.stars[i].id)
            assertEquals(stars[i].raRad, deserialized.stars[i].raRad, 1e-9)
            assertEquals(stars[i].decRad, deserialized.stars[i].decRad, 1e-9)
            assertEquals(stars[i].magnitude, deserialized.stars[i].magnitude, 1e-9)
        }

        // Check pair integrity
        for (i in pairIndex.pairs.indices) {
            assertEquals(pairIndex.pairs[i].separationRad, deserialized.pairs[i].separationRad, 1e-9)
            assertEquals(pairIndex.pairs[i].starIndex1, deserialized.pairs[i].starIndex1)
            assertEquals(pairIndex.pairs[i].starIndex2, deserialized.pairs[i].starIndex2)
        }
    }

        // R3-A3 rewrite: extrapolateFileSize() (with its hardcoded N*19600/4 quad model,
    // ~2,900x optimistic per pass-2 measurements) is REMOVED from the serializer.
    // estimateFileSizeBytes() is now a model-free byte calculator over GIVEN counts.
    // Assertions below use the MEASURED counts from
    // docs/startracker/evidence/CATALOG_SIZE_MEASURED_2026-09-03.txt:
    //   9,110 uniform stars -> 4,851,922 pairs, stars+pairs serialize to 78,094,272 B
    //   (= 74.5 MiB; star section 463,504 B = 50.88 B/star with "CATn" ids).
    // History only (no longer asserted anywhere): the removed model produced
    // 4,067,730,000 B ("3.79 GB") for 9k stars.
    @Test
    fun testEstimateFromFileCountsMatchesMeasured() {
        // Exact arithmetic from named constants (guard against silent unit change):
        val expected9110PairsOnly = CatalogSerializer.HEADER_BYTES +
            9_110L * CatalogSerializer.STAR_BYTES_APPROX +
            4_851_922L * CatalogSerializer.PAIR_BYTES
        assertEquals(expected9110PairsOnly, CatalogSerializer.estimateFileSizeBytes(9_110, 4_851_922L, 0L))

        // Against the MEASURED serialized size (real serializer, probe run):
        val measured = 78_094_272L
        val estimate = CatalogSerializer.estimateFileSizeBytes(9_110, 4_851_922L, 0L)
        println("9,110 stars + 4,851,922 pairs: estimate $estimate B vs MEASURED $measured B " +
            "(delta ${measured - estimate} B = star-id length variance; 50 vs 50.88 B/star)")
        assertTrue("estimate must be within 0.05% of the measured 78,094,272 B",
            kotlin.math.abs(estimate - measured) < measured / 2_000) // 0.05% = 39,047 B; actual delta 8,000 B
        assertEquals("measured total is 74.5 MiB", 74.5, measured / (1024.0 * 1024.0), 0.05)

        // Pair unit cost is exact: measured pair section = 16 B * 4,851,922 = 77,630,752 B
        assertEquals(77_630_752L, 4_851_922L * CatalogSerializer.PAIR_BYTES)
    }

    @Test
    fun testCappedQuadIndexCostArithmetic() {
        // Capped (Tetra3-style k-NN) index costs at 9,110 stars, quads/star = C(k,3)
        // exactly; assert the byte arithmetic a reader would do by hand (evidence file
        // addendum: 7.3 / 14.6 / 40.8 MB for k = 5 / 6 / 8 on top of the 74.5 MiB pairs).
        val pairsBytes = CatalogSerializer.estimateFileSizeBytes(9_110, 4_851_922L, 0L)
        for ((k, quadsPerStar) in listOf(5 to 10L, 6 to 20L, 8 to 56L)) {
            val quads = 9_110L * quadsPerStar
            val total = CatalogSerializer.estimateFileSizeBytes(9_110, 4_851_922L, quads)
            val quadTerm = quads * CatalogSerializer.QUAD_BYTES
            println("k=$k: quads=$quads, quad term ${quadTerm / 1_000_000} MB, total with pairs " +
                "${"%.1f".format(total / (1024.0 * 1024.0))} MiB")
            assertEquals(pairsBytes + quadTerm, total)
        }
        // k=6 lands in the "10-30 MB" band the original docs guessed (for a CAPPED index):
        val k6 = CatalogSerializer.estimateFileSizeBytes(9_110, 4_851_922L, 9_110L * 20L)
        assertTrue("k=6 capped quad term is 14-15 MB",
            9_110L * 20L * CatalogSerializer.QUAD_BYTES in 14_000_000L..15_000_000L)
        println("k=6 total (pairs+quads): ${"%.1f".format(k6 / (1024.0 * 1024.0))} MiB")
    }
}
