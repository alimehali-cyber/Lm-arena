package com.alijafari.red.astronomy.startracker.catalog

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class QuadPatternIndexTest {

    private fun createTestStars(): List<CatalogStar> {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            TESTSTAR001,0.0,0.0,2.0
            TESTSTAR002,1.0,0.0,2.5
            TESTSTAR003,0.0,1.0,3.0
            TESTSTAR004,1.0,1.0,3.0
            TESTSTAR005,10.0,10.0,4.0
            TESTSTAR006,10.5,10.0,4.0
            TESTSTAR007,10.0,10.5,4.0
            TESTSTAR008,10.5,10.5,4.0
        """.trimIndent()
        return CatalogIngestor.parse(csv)
    }

    @Test
    fun testQuadDescriptorFormation() {
        val stars = createTestStars()
        // Take first 4 stars: (0,0), (1,0), (0,1), (1,1) — forms ~1° square near equator
        val quadStars = stars.subList(0, 4)

        // Compute 6 separations by hand:
        // (0,0)-(1,0): 1°
        // (0,0)-(0,1): 1°
        // (0,0)-(1,1): sqrt(2)° ≈1.414° (approx, actually spherical)
        // (1,0)-(0,1): ~1.414°
        // (1,0)-(1,1): 1°
        // (0,1)-(1,1): 1°
        // So we have 4 sides ~1°, 2 diagonals ~1.414°, max =1.414°
        // Ratios: 1/1.414=0.707 for 4 sides, and 1.0 for the other diagonal? Actually max is diagonal, other diagonal also ~1.414, so ratios: 0.707,0.707,0.707,0.707,1.0?
        // Let's compute via code

        val index = QuadPatternIndex(stars, maxSeparationRad = 5.0 * PI / 180.0, binWidth = 0.01)
        assertTrue("Should have at least 1 quad", index.quads.isNotEmpty())

        // Find quad with first 4 stars
        val quad = index.quads.find { it.starIds.containsAll(listOf("TESTSTAR001","TESTSTAR002","TESTSTAR003","TESTSTAR004")) }
        assertNotNull("Should find quad with first 4 stars", quad)

        quad?.let {
            // Descriptor should have 5 ratios
            assertEquals(5, it.descriptor.ratios.size)
            // All ratios in [0,1]
            for (r in it.descriptor.ratios) {
                assertTrue("Ratio $r in [0,1]", r in 0.0..1.0)
            }
            // Ratios sorted ascending
            val sorted = it.descriptor.ratios.sorted()
            assertEquals(sorted, it.descriptor.ratios)
            println("Quad descriptor for square: ratios=${it.descriptor.ratios}, maxSep=${it.descriptor.maxSeparationRad * 180/PI}°")
        }
    }

    @Test
    fun testHashLookupIdentity() {
        val stars = createTestStars()
        val index = QuadPatternIndex(stars, maxSeparationRad = 5.0 * PI / 180.0, binWidth = 0.01)

        // Pick 4 known fixture stars, compute their observed descriptor directly (zero noise, identity)
        val observedStars = stars.subList(0, 4) // TESTSTAR001-004

        val observedDescriptor = index.computeDescriptorForObserved(observedStars)
        val candidates = index.lookupCandidates(observedDescriptor)

        println("Identity lookup: observed descriptor key=${observedDescriptor.quantizedKey()}, candidates=${candidates.size}")
        // Should retrieve exactly that catalog quad (or at least include it)
        assertTrue("Identity lookup should retrieve at least 1 candidate", candidates.isNotEmpty())

        val found = candidates.any { quad ->
            quad.starIds.containsAll(observedStars.map { it.id })
        }
        assertTrue("Should find exact quad in candidates", found)
    }

    @Test
    fun testHashLookupWithNoiseSweep() {
        val stars = createTestStars()
        val index = QuadPatternIndex(stars, maxSeparationRad = 5.0 * PI / 180.0, binWidth = 0.01)

        val baseStars = stars.subList(0, 4)

        // Sweep noise levels: add Gaussian perturbation to positions (simulate imperfect centroiding)
        // Noise in degrees: 0.001°, 0.01°, 0.05°, 0.1°, 0.2°
        val noiseLevelsDeg = listOf(0.0, 0.001, 0.01, 0.05, 0.1, 0.2)

        println("Noise sweep for quad retrieval:")
        println("Noise(deg) | Retrieved? | CandidateCount | Key")
        for (noiseDeg in noiseLevelsDeg) {
            val noisyStars = baseStars.map { star ->
                // Add small random perturbation
                val rnd = java.util.Random((noiseDeg * 10000).toLong())
                val dRa = (rnd.nextGaussian() * noiseDeg)
                val dDec = (rnd.nextGaussian() * noiseDeg)
                CatalogStar.fromDegrees(
                    id = star.id,
                    raDeg = (star.raDeg + dRa + 360) % 360,
                    decDeg = (star.decDeg + dDec).coerceIn(-90.0, 90.0),
                    magnitude = star.magnitude,
                    sourceCatalog = star.sourceCatalog
                )
            }

            val noisyDescriptor = index.computeDescriptorForObserved(noisyStars)
            val candidates = index.lookupCandidates(noisyDescriptor)
            val candidatesWithNeighbors = index.lookupCandidatesWithNeighborBins(noisyDescriptor)

            val retrieved = candidates.any { it.starIds.containsAll(baseStars.map { s -> s.id }) }
            val retrievedWithNeighbors = candidatesWithNeighbors.any { it.starIds.containsAll(baseStars.map { s -> s.id }) }

            println("${"%.4f".format(noiseDeg)} | $retrieved (neighbors: $retrievedWithNeighbors) | ${candidates.size} (${candidatesWithNeighbors.size} with neighbors) | ${noisyDescriptor.quantizedKey()}")

            if (noiseDeg == 0.0) {
                assertTrue("Zero noise should retrieve", retrieved)
            }
        }

        // Report at what noise level lookup starts missing
        // For binWidth 0.01, expect tolerance up to ~0.05-0.1° noise before missing
        // This is concrete measured sensitivity on fixture, not live data
    }

    @Test
    fun testQuadCountForFixture() {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            TESTSTAR001,0.0,0.0,2.0
            TESTSTAR002,90.0,0.0,2.5
            TESTSTAR003,0.0,90.0,3.0
            TESTSTAR004,0.0,-90.0,3.0
            TESTSTAR005,1.0,0.0,4.0
            TESTSTAR006,180.0,0.0,2.2
            TESTSTAR007,45.0,45.0,3.5
            TESTSTAR008,45.0,46.0,4.5
            TESTSTAR009,10.0,10.0,3.0
            TESTSTAR010,20.0,15.0,4.0
            TESTSTAR011,30.0,20.0,5.0
            TESTSTAR012,15.0,12.0,4.2
            TESTSTAR013,25.0,18.0,3.8
            TESTSTAR014,12.0,8.0,4.8
            TESTSTAR015,0.5,0.5,4.0
        """.trimIndent()

        val stars = CatalogIngestor.parse(csv)

        val index40 = QuadPatternIndex(stars, maxSeparationRad = 40.0 * PI / 180.0)
        val index10 = QuadPatternIndex(stars, maxSeparationRad = 10.0 * PI / 180.0)

        println("Fixture with 15 stars:")
        println("  maxSep 40°: ${index40.quads.size} quads, ${index40.hashTable.size} hash buckets")
        println("  maxSep 10°: ${index10.quads.size} quads, ${index10.hashTable.size} hash buckets")
        println("  Full C(15,4)=1365 possible, but filtered by maxSep")

        // For 15 stars, full combinatorial would be 1365, but with 40° cutoff we get less
        assertTrue("Quad count should be <=1365", index40.quads.size <= 1365)
        assertTrue("10° cutoff should have fewer quads than 40°", index10.quads.size <= index40.quads.size)
    }
}
