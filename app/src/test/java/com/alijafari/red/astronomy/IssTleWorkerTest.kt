package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.data.repository.TleRepository
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying ISS TLE parsing, checksum validation, and SGP4 propagator integration.
 */
class IssTleWorkerTest {

    @Test
    fun testTleChecksumValidation() {
        // Line 1: Sum is 127 -> 127 % 10 = 7
        val validLine1 = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9997"
        // Line 2: Sum is 86 -> 86 % 10 = 6
        val validLine2 = "2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400006"

        assertTrue("Line 1 checksum should be valid", TleRepository.validateChecksum(validLine1))
        assertTrue("Line 2 checksum should be valid", TleRepository.validateChecksum(validLine2))

        val invalidLine1 = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9999"
        assertFalse("Corrupted checksum should be rejected", TleRepository.validateChecksum(invalidLine1))
    }

    @Test
    fun testParseTleFeedWithIss() {
        val rawFeed = """
            ISS (ZARYA)
            1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9997
            2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400006
        """.trimIndent()

        val parsed = TleRepository.parseTleFeed(rawFeed)
        assertEquals(1, parsed.size)
        assertTrue(parsed.containsKey(25544))

        val iss = parsed[25544]!!
        assertEquals("ISS (ZARYA)", iss.name)
        assertEquals("1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9997", iss.line1)
    }

    @Test
    fun testSgp4PropagatorUsesUpdatedTle() {
        val originalTle = ISSEngine.cachedTLE

        val updatedTle = ISSEngine.TLEData(
            name = "ISS (ZARYA)",
            line1 = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9997",
            line2 = "2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400006"
        )

        ISSEngine.cachedTLE = updatedTle
        assertEquals(updatedTle.line1, ISSEngine.cachedTLE.line1)

        val timestamp = 1785500000000L // 2026 epoch timestamp
        val pos = ISSEngine.calculateTopocentricPos(
            timestampMs = timestamp,
            userLatDeg = 30.1132,
            userLonDeg = 51.5217,
            userAltMeters = 940.0
        )

        assertNotNull(pos)
        assertTrue("Sub-satellite latitude must be within orbital inclination limits [-52, 52]", pos.subLatDeg in -52.0..52.0)
        assertTrue("Sub-satellite longitude must be in [-180, 180]", pos.subLonDeg in -180.0..180.0)
        assertTrue("Altitude must be in LEO range [350, 500] km", pos.satAltKm in 350.0..500.0)
        assertTrue("Orbital velocity must be around ~7.6 km/s", pos.velocityKmS in 7.0..8.2)

        // Restore original
        ISSEngine.cachedTLE = originalTle
    }
}
