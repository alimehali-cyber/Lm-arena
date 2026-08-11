package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroDispatchEngine
import com.alijafari.red.astronomy.astro_engine.CelestialSearchEngine
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import org.junit.Assert.*
import org.junit.Test

class Phase4MigrationTest {

    @Test
    fun testPhase4SearchMigrationAndCanonicalResolution() {
        val report = CelestialSearchEngine.verifyPhase4Search()
        assertTrue("Sun should be found and resolved to canonical ID 'sun'", report.sunFound)
        assertTrue("Moon should be found and resolved to canonical ID 'moon'", report.moonFound)
        assertTrue("Jupiter should be found and resolved to canonical ID 'planet_jupiter'", report.jupiterFound)
        assertTrue("Elara should be found and resolved to canonical ID 'jup_elara'", report.elaraFound)
        assertTrue("ISS should be found and resolved to canonical ID 'sat_25544'", report.issFound)
        assertTrue("Sirius star should be found and resolved to canonical ID 'star_cma_sirius'", report.starFound)
        assertTrue("Orion constellation should be found and resolved to canonical ID 'const_ori'", report.constellationFound)
        assertTrue("Andromeda galaxy should be found and resolved to canonical ID 'dso_m31_andromeda'", report.galaxyFound)
        assertTrue("Satellite should be found and resolved to a canonical satellite ID", report.satelliteFound)
        assertTrue("All representative objects must resolve to canonical IDs", report.isPassed)
    }

    @Test
    fun testSearchDynamicAstroDispatchIntegration() {
        val lat = 35.6892
        val lon = 51.3890
        val results = CelestialSearchEngine.search("مشتری", lat, lon)

        assertFalse("Search for Jupiter should return at least 1 result", results.isEmpty())
        val jupiterResult = results.first()
        assertEquals("planet_jupiter", jupiterResult.celestialObject.id)

        val state = AstroDispatchEngine.calculateState("planet_jupiter", System.currentTimeMillis(), lat, lon)
        assertNotNull("Calculated state for Jupiter should not be null", state)
        assertEquals("planet_jupiter", state!!.canonicalObject.canonicalId)
        assertTrue("Magnitude should be valid", state.magnitude < 5.0)
    }

    @Test
    fun testLegacyIdResolutionInMainViewModelGateway() {
        val canonicalSunId = CanonicalAstroCatalog.resolveCanonicalId("sun_sol")
        assertEquals("sun", canonicalSunId)

        val canonicalIssId = CanonicalAstroCatalog.resolveCanonicalId("sat_iss")
        assertEquals("sat_25544", canonicalIssId)

        val canonicalIoId = CanonicalAstroCatalog.resolveCanonicalId("jupiter_io")
        assertEquals("jup_io", canonicalIoId)
    }
}
