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
        println("  Estimated total: ${extrap9000.estimatedTotalBytes} bytes = ${extrap9000.estimatedTotalKB} KB = ${extrap9000.estimatedTotalMB} MB")

        println("\nExtrapolation to 15000 stars:")
        println("  Estimated pairs: ${extrap15000.estimatedPairs}")
        println("  Estimated quads: ${extrap15000.estimatedQuadsNearbyLimited}")
        println("  Estimated total: ${extrap15000.estimatedTotalBytes} bytes = ${extrap15000.estimatedTotalKB} KB = ${extrap15000.estimatedTotalMB} MB")

        println("\nArithmetic shown:")
        println("  bytesPerStar approx: ${bytes.size}/${stars.size} = ${bytes.size.toDouble()/stars.size}")
        println("  pairs scale as N^2 * (pairs/N^2) where pairs/N^2 = ${pairIndex.pairs.size}/${stars.size*stars.size} = ${pairIndex.pairs.size.toDouble()/(stars.size*stars.size)}")
        println("  quads nearby limited: N * C(50,3)/4 = N*19600/4 (per config MAX_STARS_PER_REGION_FOR_QUADS=50)")

        // Assertions: file size should be reasonable for Android asset (<50 MB)
        assertTrue("9000 stars should be <50 MB", extrap9000.estimatedTotalMB < 50)
        assertTrue("15000 stars should be <100 MB", extrap15000.estimatedTotalMB < 100)
    }
}
