package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.ObjectType
import org.junit.Assert.*
import org.junit.Test

class SatelliteARConsistencyTest {

    private val testTimestamp = 1717000000000L // Epoch timestamp
    private val observerLat = 35.6892
    private val observerLon = 51.3890
    private val observerAltM = 1200.0

    @Test
    fun testSatelliteIdentityAndNoradResolution() {
        // Test NORAD 25544 (ISS)
        val issFromNorad = SatelliteEngine.resolveSatelliteItem("25544")
        assertNotNull(issFromNorad)
        assertEquals(25544, issFromNorad?.noradId)

        val issFromSatPrefix = SatelliteEngine.resolveSatelliteItem("sat_25544")
        assertNotNull(issFromSatPrefix)
        assertEquals(25544, issFromSatPrefix?.noradId)

        val issFromAlias = SatelliteEngine.resolveSatelliteItem("sat_iss")
        assertNotNull(issFromAlias)
        assertEquals(25544, issFromAlias?.noradId)

        // Test NORAD 48274 (Tiangong)
        val tiangong = SatelliteEngine.resolveSatelliteItem("sat_48274")
        assertNotNull(tiangong)
        assertEquals(48274, tiangong?.noradId)

        // Test NORAD 20580 (Hubble)
        val hubble = SatelliteEngine.resolveSatelliteItem("sat_20580")
        assertNotNull(hubble)
        assertEquals(20580, hubble?.noradId)

        // Test NORAD 27386 (Envisat)
        val envisat = SatelliteEngine.resolveSatelliteItem("sat_27386")
        assertNotNull(envisat)
        assertEquals(27386, envisat?.noradId)
    }

    @Test
    fun testARAndSatellitesScreenPositionsAreIdentical() {
        // Verify for all catalog satellites
        SatelliteCatalog.satellites.forEach { satItem ->
            // Satellites screen calculation:
            val satScreenState = SatelliteEngine.calculateSatelliteState(
                satellite = satItem,
                timestampMs = testTimestamp,
                userLatDeg = observerLat,
                userLonDeg = observerLon,
                userAltMeters = observerAltM
            )

            // AR Screen calculation via calculateAllSatellitePositions:
            val arPositions = SatelliteEngine.calculateAllSatellitePositions(
                timestampMs = testTimestamp,
                userLatDeg = observerLat,
                userLonDeg = observerLon,
                userAltMeters = observerAltM
            )

            val arHorizByNorad = arPositions["sat_${satItem.noradId}"]
            assertNotNull("Position for NORAD ${satItem.noradId} must exist in AR map", arHorizByNorad)

            val arHorizById = arPositions[satItem.id]
            assertNotNull("Position for ID ${satItem.id} must exist in AR map", arHorizById)

            assertEquals(
                "Azimuth between Satellites screen and AR map must match exactly for ${satItem.nameEn}",
                satScreenState.topocentric.azimuthDeg,
                arHorizByNorad!!.azimuthDeg,
                1e-6
            )

            assertEquals(
                "Elevation between Satellites screen and AR map must match exactly for ${satItem.nameEn}",
                satScreenState.topocentric.elevationDeg,
                arHorizByNorad.altitudeDeg,
                1e-6
            )

            assertEquals(
                "Azimuth by item ID and NORAD ID must be identical",
                arHorizById!!.azimuthDeg,
                arHorizByNorad.azimuthDeg,
                1e-6
            )
        }
    }

    @Test
    fun testFinderAndDispatchConsistency() {
        val jd = TimeEngine.getJulianDate(testTimestamp)

        // Test ISS finder
        val issObj = AstronomyCatalog.ISS

        val finderData = FinderEngine.calculateFinderData(
            target = issObj,
            phoneAzimuthDeg = 0.0,
            phoneAltitudeDeg = 0.0,
            userLat = observerLat,
            userLon = observerLon,
            userAltMeters = observerAltM,
            jd = jd
        )

        val satScreenState = SatelliteEngine.calculateSatelliteState(
            satellite = SatelliteEngine.resolveSatelliteItem("25544")!!,
            timestampMs = testTimestamp,
            userLatDeg = observerLat,
            userLonDeg = observerLon,
            userAltMeters = observerAltM
        )

        assertEquals(
            "Finder target azimuth must match SatelliteEngine calculated azimuth",
            satScreenState.topocentric.azimuthDeg,
            finderData.targetAzimuthDeg,
            1e-4
        )

        assertEquals(
            "Finder target altitude must match SatelliteEngine calculated altitude",
            satScreenState.topocentric.elevationDeg,
            finderData.targetAltitudeDeg,
            1e-4
        )
    }

    @Test
    fun testSatelliteOrbitTrajectoryConsistency() {
        val iss = SatelliteEngine.resolveSatelliteItem("25544")!!
        val (pastPoints, futurePoints) = SatelliteEngine.generateOrbitTrajectory(
            satellite = iss,
            currentTimestampMs = testTimestamp,
            userLatDeg = observerLat,
            userLonDeg = observerLon,
            userAltMeters = observerAltM,
            pastMinutes = 45,
            futureMinutes = 45,
            stepSeconds = 30
        )

        // 45 mins * 2 = 90 mins / (0.5 min step) = 91 points
        assertTrue("Past points must not be empty", pastPoints.isNotEmpty())
        assertTrue("Future points must not be empty", futurePoints.isNotEmpty())

        // Current point (end of past and start of future) should match live topocentric state
        val liveState = SatelliteEngine.calculateSatelliteState(
            satellite = iss,
            timestampMs = testTimestamp,
            userLatDeg = observerLat,
            userLonDeg = observerLon,
            userAltMeters = observerAltM
        )

        val currentPast = pastPoints.last()
        val currentFuture = futurePoints.first()

        assertEquals("Trajectory junction azimuth must match live state", liveState.topocentric.azimuthDeg, currentPast.azimuthDeg, 1e-4)
        assertEquals("Trajectory junction altitude must match live state", liveState.topocentric.elevationDeg, currentPast.altitudeDeg, 1e-4)
        assertEquals("Trajectory past and future junction must match", currentPast.azimuthDeg, currentFuture.azimuthDeg, 1e-4)
    }

    @Test
    fun testSatellitePositionVerificationMethod() {
        val verification = SatelliteEngine.verifySatellitePositionConsistency(
            satelliteIdOrNorad = "sat_25544",
            timestampMs = testTimestamp,
            userLatDeg = observerLat,
            userLonDeg = observerLon,
            userAltMeters = observerAltM
        )
        assertNotNull(verification)
        assertTrue("Verification must prove 100% mathematical identity", verification!!.isIdentical)
    }
}
