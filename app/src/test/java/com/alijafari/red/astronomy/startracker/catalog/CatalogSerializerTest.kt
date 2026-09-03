package com.alijafari.red.astronomy.startracker.catalog

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.PI

class CatalogSerializerTest {

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

    @Test
    fun testFileSizeExtrapolation() {
        // Use small fixture to measure size, then extrapolate to 9k-15k stars
        val csv = """
            id,ra_deg,dec_deg,magnitude
            TESTSTAR001,0.0,0.0,2.0
            TESTSTAR002,90.0,0.0,2.5
            TESTSTAR003,0.0,90.0,3.0
            TESTSTAR004,1.0,0.0,4.0
            TESTSTAR005,0.0,1.0,4.0
            TESTSTAR006,10.0,10.0,3.0
            TESTSTAR007,20.0,15.0,4.0
            TESTSTAR008,30.0,20.0,5.0
            TESTSTAR009,15.0,12.0,4.2
            TESTSTAR010,25.0,18.0,3.8
        """.trimIndent()

        val stars = CatalogIngestor.parse(csv)
        val pairIndex = AngularSeparationIndex(stars, maxSeparationRad = 40.0 * PI / 180.0)
        val quadIndex = QuadPatternIndex(stars, maxSeparationRad = 40.0 * PI / 180.0)

        val bytes = CatalogSerializer.serialize(stars, pairIndex.pairs, quadIndex.quads)
        println("Fixture: ${stars.size} stars, ${pairIndex.pairs.size} pairs, ${quadIndex.quads.size} quads, ${bytes.size} bytes")

        // Extrapolate to 9000 and 15000 stars
        val extrap9000 = CatalogSerializer.extrapolateFileSize(
            fixtureStars = stars.size,
            fixturePairs = pairIndex.pairs.size,
            fixtureQuads = quadIndex.quads.size,
            fixtureBytes = bytes.size,
            targetStars = 9000
        )

        val extrap15000 = CatalogSerializer.extrapolateFileSize(
            fixtureStars = stars.size,
            fixturePairs = pairIndex.pairs.size,
            fixtureQuads = quadIndex.quads.size,
            fixtureBytes = bytes.size,
            targetStars = 15000
        )

        println("\nExtrapolation to 9000 stars:")
        println("  Estimated pairs: ${extrap9000.estimatedPairs}")
        println("  Estimated quads (nearby limited): ${extrap9000.estimatedQuadsNearbyLimited}")
        println("  Estimated total: ${extrap9000.estimatedTotalBytes} bytes = ${extrap9000.estimatedTotalKB} KB = ${extrap9000.estimatedTotalMB} MB = ${"%.2f".format(extrap9000.estimatedTotalGB)} GB")

        println("\nExtrapolation to 15000 stars:")
        println("  Estimated pairs: ${extrap15000.estimatedPairs}")
        println("  Estimated quads: ${extrap15000.estimatedQuadsNearbyLimited}")
        println("  Estimated total: ${extrap15000.estimatedTotalBytes} bytes = ${extrap15000.estimatedTotalKB} KB = ${extrap15000.estimatedTotalMB} MB = ${"%.2f".format(extrap15000.estimatedTotalGB)} GB")

        println("\nArithmetic shown:")
        println("  bytesPerStar approx: ${bytes.size}/${stars.size} = ${bytes.size.toDouble()/stars.size}")
        println("  pairs scale as N^2 * (pairs/N^2) where pairs/N^2 = ${pairIndex.pairs.size}/${stars.size*stars.size} = ${pairIndex.pairs.size.toDouble()/(stars.size*stars.size)}")
        println("  quads nearby limited: N * C(50,3)/4 = N*19600/4 (per config MAX_STARS_PER_REGION_FOR_QUADS=50)")

        // Audit finding B1: the old assertions ("9000 stars should be <50 MB", "15000 <100 MB") passed
        // ONLY because Int arithmetic overflowed (e.g. 15,000 * 19,600 / 4 * 84 = 6,174,000,000 wraps
        // negative against Int.MAX_VALUE = 2,147,483,647) and the wrapped-negative MB trivially satisfied
        // the "< 50" checks. The real estimates are in the GIGABYTE range, and the assertions below
        // verify the exact Long-arithmetic values so neither an overflow nor a silent unit change can
        // masquerade as a pass again.

        // Exact quad-term arithmetic (fixture-independent, pure Long):
        assertEquals("9,000 stars quad count must be 9000*19600/4", 44_100_000L, extrap9000.estimatedQuadsNearbyLimited)
        assertEquals("15,000 stars quad count must be 15000*19600/4", 73_500_000L, extrap15000.estimatedQuadsNearbyLimited)

        // Exact total = stars term + pairs term + quads term, recomputed here in Long:
        val expectedBytes9000 = 9_000L * 50L + extrap9000.estimatedPairs * 16L + 44_100_000L * 84L
        val expectedBytes15000 = 15_000L * 50L + extrap15000.estimatedPairs * 16L + 73_500_000L * 84L
        assertEquals(expectedBytes9000, extrap9000.estimatedTotalBytes)
        assertEquals(expectedBytes15000, extrap15000.estimatedTotalBytes)

        // No overflow / no wrapped-negative values anywhere:
        assertTrue(extrap9000.estimatedTotalBytes > 0)
        assertTrue(extrap15000.estimatedTotalBytes > 0)
        assertTrue(extrap9000.estimatedPairs > 0)

        // The honest headline: the quad term ALONE is ~3.7 GB (9k) / ~6.2 GB (15k) — the opposite
        // of the previously claimed "10-30 MB". Assert the magnitude explicitly:
        assertEquals("9k quad-term bytes", 3_704_400_000L, 44_100_000L * 84L)
        assertEquals("15k quad-term bytes", 6_174_000_000L, 73_500_000L * 84L)
        assertTrue("9,000-star estimate must exceed the 3.7 GB quad term alone",
            extrap9000.estimatedTotalBytes > 3_704_400_000L)
        assertTrue("15,000-star estimate must exceed the 6.2 GB quad term alone",
            extrap15000.estimatedTotalBytes > 6_174_000_000L)
        assertTrue("the old '<50 MB' claim is false and must fail: actual is > 3.5 GB",
            extrap9000.estimatedTotalMB > 3_500L)
        assertTrue("the old '<100 MB' claim is false and must fail: actual is > 5.8 GB",
            extrap15000.estimatedTotalMB > 5_800L)
    }
}
