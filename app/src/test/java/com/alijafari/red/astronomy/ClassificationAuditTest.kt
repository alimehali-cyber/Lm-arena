package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.CelestialSearchEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.ObjectType
import org.junit.Assert.*
import org.junit.Test

class ClassificationAuditTest {

    @Test
    fun testPlutoClassification() {
        val canonicalPluto = CanonicalAstroCatalog.getCanonicalObject("planet_pluto")
        assertNotNull("Pluto canonical object must exist", canonicalPluto)
        assertEquals("planet_pluto", canonicalPluto?.canonicalId)
        assertEquals("Pluto must be classified as DWARF_PLANET", ObjectType.DWARF_PLANET, canonicalPluto?.type)

        val resolvedId = CanonicalAstroCatalog.resolveCanonicalId("pluto")
        assertEquals("planet_pluto", resolvedId)

        val plutoObject = AstronomyCatalog.getById("planet_pluto")
        assertNotNull("Pluto object should be retrievable from AstronomyCatalog", plutoObject)
        assertEquals(ObjectType.DWARF_PLANET, plutoObject?.type)

        val searchResults = CelestialSearchEngine.search("pluto", 35.6892, 51.3890)
        assertTrue("Search for 'pluto' must return Pluto", searchResults.any { it.celestialObject.id == "planet_pluto" })
    }

    @Test
    fun testSagittariusAStarClassification() {
        val canonicalSgrA = CanonicalAstroCatalog.getCanonicalObject("sagittarius_a_star")
        assertNotNull("Sagittarius A* canonical object must exist", canonicalSgrA)
        assertEquals("sagittarius_a_star", canonicalSgrA?.canonicalId)
        assertEquals("Sagittarius A* must be classified as BLACK_HOLE", ObjectType.BLACK_HOLE, canonicalSgrA?.type)

        val resolvedFromLegacy = CanonicalAstroCatalog.resolveCanonicalId("galaxy_milky_way")
        assertEquals("sagittarius_a_star", resolvedFromLegacy)

        val resolvedFromAlias = CanonicalAstroCatalog.resolveCanonicalId("sgr a*")
        assertEquals("sagittarius_a_star", resolvedFromAlias)

        val sgrAObject = AstronomyCatalog.getById("sagittarius_a_star")
        assertNotNull("Sagittarius A* object should be retrievable from AstronomyCatalog", sgrAObject)
        assertEquals(ObjectType.BLACK_HOLE, sgrAObject?.type)

        val searchResults = CelestialSearchEngine.search("sagittarius a*", 35.6892, 51.3890)
        assertTrue("Search for 'sagittarius a*' must return Sagittarius A*", searchResults.any { it.celestialObject.id == "sagittarius_a_star" })
    }
}
