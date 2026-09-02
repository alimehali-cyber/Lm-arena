package com.alijafari.red.astronomy.startracker.catalog

import org.junit.Test
import org.junit.Assert.*

class CatalogIngestorTest {

    private val validCsv = """
        id,ra_deg,dec_deg,magnitude
        TESTSTAR001,0.0,0.0,2.0
        TESTSTAR002,90.0,0.0,2.5
        TESTSTAR003,0.0,90.0,3.0
        TESTSTAR004,0.0,-90.0,3.0
        TESTSTAR005,1.0,0.0,4.0
    """.trimIndent()

    private val malformedCsv = """
        id,ra_deg,dec_deg,magnitude
        TESTSTAR001,0.0,0.0,2.0
        BADROW,not_a_number,0.0,3.0
    """.trimIndent()

    @Test
    fun testParseValidCsv() {
        val stars = CatalogIngestor.parse(validCsv, "TEST_FIXTURE")
        assertEquals(5, stars.size)
        assertEquals("TESTSTAR001", stars[0].id)
        assertEquals(0.0, stars[0].raDeg, 1e-6)
        assertEquals(0.0, stars[0].decDeg, 1e-6)
        assertEquals(2.0, stars[0].magnitude, 1e-6)
        assertEquals(90.0, stars[1].raDeg, 1e-6)
        assertEquals(0.0, stars[1].decDeg, 1e-6)
        // Check conversion to radians
        assertEquals(0.0, stars[0].raRad, 1e-6)
        assertEquals(90.0 * Math.PI / 180.0, stars[1].raRad, 1e-6)
    }

    @Test
    fun testParseWithCommentsAndEmptyLines() {
        val csvWithComments = """
            # This is a comment
            id,ra_deg,dec_deg,magnitude

            TESTSTAR001,0.0,0.0,2.0

            # Another comment
            TESTSTAR002,90.0,0.0,2.5

        """.trimIndent()
        val stars = CatalogIngestor.parse(csvWithComments)
        assertEquals(2, stars.size)
    }

    @Test(expected = CatalogIngestor.ParseError::class)
    fun testRejectMalformedRow() {
        CatalogIngestor.parse(malformedCsv)
    }

    @Test(expected = CatalogIngestor.ParseError::class)
    fun testRejectInvalidRaRange() {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            BAD,400.0,0.0,2.0
        """.trimIndent()
        CatalogIngestor.parse(csv)
    }

    @Test(expected = CatalogIngestor.ParseError::class)
    fun testRejectInvalidDecRange() {
        val csv = """
            id,ra_deg,dec_deg,magnitude
            BAD,0.0,100.0,2.0
        """.trimIndent()
        CatalogIngestor.parse(csv)
    }

    @Test
    fun testParseFixtureFile() {
        // Load fixture from resource string (embedded)
        val fixtureCsv = """
            id,ra_deg,dec_deg,magnitude
            TESTSTAR001,0.0,0.0,2.0
            TESTSTAR002,90.0,0.0,2.5
            TESTSTAR003,0.0,90.0,3.0
            TESTSTAR004,0.0,-90.0,3.0
            TESTSTAR005,1.0,0.0,4.0
            TESTSTAR006,180.0,0.0,2.2
            TESTSTAR007,0.0,0.0,5.0
            TESTSTAR008,45.0,45.0,3.5
            TESTSTAR009,45.0,46.0,4.5
            TESTSTAR010,10.0,10.0,3.0
            TESTSTAR011,20.0,15.0,4.0
            TESTSTAR012,30.0,20.0,5.0
            TESTSTAR013,15.0,12.0,4.2
            TESTSTAR014,25.0,18.0,3.8
            TESTSTAR015,12.0,8.0,4.8
        """.trimIndent()

        val stars = CatalogIngestor.parse(fixtureCsv, "TEST_FIXTURE")
        assertEquals(15, stars.size)

        // Verify specific expected separations by hand (documented in fixture):
        // TESTSTAR001 (0,0) and TESTSTAR002 (90,0): exactly 90° = π/2 rad
        val sep1 = AngularSeparation.between(stars[0], stars[1])
        assertEquals(90.0 * Math.PI / 180.0, sep1, 1e-6)

        // TESTSTAR001 (0,0) and TESTSTAR003 (0,90): North pole, 90° apart
        val sep2 = AngularSeparation.between(stars[0], stars[2])
        assertEquals(90.0 * Math.PI / 180.0, sep2, 1e-6)

        // TESTSTAR001 (0,0) and TESTSTAR005 (1,0): 1° apart
        val sep3 = AngularSeparation.between(stars[0], stars[4])
        assertEquals(1.0 * Math.PI / 180.0, sep3, 1e-6)

        // TESTSTAR001 (0,0) and TESTSTAR006 (180,0): antipodal 180° apart
        val sep4 = AngularSeparation.between(stars[0], stars[5])
        assertEquals(180.0 * Math.PI / 180.0, sep4, 1e-6)

        // TESTSTAR001 and TESTSTAR007 both at (0,0): 0° apart
        val sep5 = AngularSeparation.between(stars[0], stars[6])
        assertEquals(0.0, sep5, 1e-6)

        // TESTSTAR008 (45,45) and TESTSTAR009 (45,46): 1° apart in Dec
        val sep6 = AngularSeparation.between(stars[7], stars[8])
        assertEquals(1.0 * Math.PI / 180.0, sep6, 0.01) // approximate, not exactly 1° due to spherical geometry but close at high dec?
        // Actually for same RA, different Dec, separation = |dec2-dec1| = 1°, so should be exactly 1°
        assertEquals(1.0 * Math.PI / 180.0, sep6, 1e-3)
    }
}
