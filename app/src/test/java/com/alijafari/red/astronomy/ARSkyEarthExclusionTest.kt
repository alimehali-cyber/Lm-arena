package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ARSkyCatalog
import com.alijafari.red.astronomy.astro_engine.CelestialSearchEngine
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Earth is the observer's own position, so it must never reach an AR-facing consumer.
 * The exclusion is scoped to the AR sky: the canonical catalogue itself still carries Earth for
 * the Lab / time-dilation screen, the relativistic calculator and the object detail pages.
 */
class ARSkyEarthExclusionTest {

    private val jd = TimeEngine.getJulianDate()

    private val rawCatalog by lazy { AstronomyCatalog.getAllObjects(jd) }
    private val arSky by lazy { ARSkyCatalog.arSkyObjects(rawCatalog) }

    private fun fakeObject(
        id: String,
        type: ObjectType = ObjectType.PLANET,
        nameEn: String = "Test body",
        magnitude: Double = 0.0,
        angularSizeArcmin: Double? = null
    ) = CelestialObject(
        id = id,
        type = type,
        nameEn = nameEn,
        nameFa = nameEn,
        raDeg = 0.0,
        decDeg = 0.0,
        magnitude = magnitude,
        constellationEn = "",
        constellationFa = "",
        distanceLightYears = 0.0,
        category = "",
        descriptionEn = "",
        descriptionFa = "",
        observationTipEn = "",
        observationTipFa = "",
        angularSizeArcmin = angularSizeArcmin
    )

    @Test
    fun earthNeverAppearsInTheArFacingObjectList() {
        // The exclusion is applied at the AR boundary, not by deleting Earth from the catalogue.
        assertTrue(
            "Earth must still exist in the master catalogue for non-AR consumers",
            rawCatalog.any { it.id == ARSkyCatalog.EARTH_CANONICAL_ID }
        )

        assertFalse(
            "Earth leaked into the AR-facing object list",
            arSky.any { ARSkyCatalog.isExcludedFromArSky(it) }
        )
        val earthEntry = rawCatalog.first { it.id == ARSkyCatalog.EARTH_CANONICAL_ID }
        assertFalse(
            "Earth's catalogue entry (\"${earthEntry.nameEn}\") must not appear in the AR sky",
            arSky.any { it.nameEn == earthEntry.nameEn && it.type == earthEntry.type }
        )
        assertEquals(
            "Earth is the only object the AR scoping removes",
            rawCatalog.size - 1,
            arSky.size
        )
        // The rule is id-based: unrelated bodies that legitimately mention Earth in their name
        // ("Envisat / Earth Observation Sat", near-Earth objects, ...) keep their place in the sky.
        assertEquals(
            "Objects that merely mention Earth in their name must not be filtered out",
            rawCatalog.count { !ARSkyCatalog.isEarth(it.id) && it.nameEn.contains("Earth", ignoreCase = true) },
            arSky.count { it.nameEn.contains("Earth", ignoreCase = true) }
        )
    }

    @Test
    fun earthIsNotFindableThroughArSearch() {
        val lat = 30.1141
        val lon = 51.5217
        for (query in listOf("earth", "Earth", "terra", "زمین", "گیتی", "home planet", "planet_earth")) {
            val hits = CelestialSearchEngine.search(query, lat, lon, jd)
            assertTrue(
                "AR search must not return Earth for query '$query', got: ${hits.map { it.celestialObject.id }}",
                hits.none { ARSkyCatalog.isEarth(it.celestialObject.id) }
            )
        }
    }

    @Test
    fun neighbouringBodiesOfEarthAreUnaffected() {
        val arSky = ARSkyCatalog.arSkyObjects(AstronomyCatalog.getAllObjects(jd))
        // The Moon and the satellites are Earth's children in the catalogue; hiding the parent
        // must not sweep them away with it.
        assertTrue("Moon must stay in the AR sky", arSky.any { it.id == "moon" })
        assertTrue("ISS must stay in the AR sky", arSky.any { it.id == "sat_25544" })
        assertTrue("Jupiter must stay in the AR sky", arSky.any { it.id == "planet_jupiter" })
    }

    @Test
    fun nonArConsumersStillResolveEarth() {
        assertNotNull(
            "The Lab / time-dilation screen resolves Earth from the catalogue",
            AstronomyCatalog.getById("planet_earth", jd)
        )
        assertNotNull(
            CanonicalAstroCatalog.getCanonicalObject("planet_earth")
        )
        assertEquals(
            "earth", CanonicalAstroCatalog.getCanonicalObject("planet_earth")!!.legacyIds.first()
        )
    }

    @Test
    fun exclusionCoversCanonicalIdsLegacyIdsAndAliases() {
        assertTrue(ARSkyCatalog.isEarth("planet_earth"))
        assertTrue(ARSkyCatalog.isEarth("EARTH"))
        assertTrue(ARSkyCatalog.isEarth("terra"))
        assertTrue(ARSkyCatalog.isEarth(" planet_earth "))
        assertTrue(ARSkyCatalog.isExcludedFromArSky(fakeObject("planet_earth")))
        assertFalse(ARSkyCatalog.isEarth(null))
        assertFalse(ARSkyCatalog.isEarth("planet_mars"))
        assertFalse(ARSkyCatalog.isEarth("star_sol"))
        assertFalse(ARSkyCatalog.isExcludedFromArSky(fakeObject("planet_mars", nameEn = "Mars")))
    }

    /**
     * The exclusion must be id-based: bodies whose metadata legitimately mentions Earth (every
     * planet's "x× Earth" comparison strings) must never be dropped from the AR sky by accident.
     */
    @Test
    fun onlyEarthIsMatchedByTheExclusion() {
        val comparedToEarth = fakeObject("planet_venus", nameEn = "Venus (2nd Planet from the Sun)")
        assertFalse(ARSkyCatalog.isExcludedFromArSky(comparedToEarth))
        val dso = fakeObject("dso_test", type = ObjectType.DEEP_SKY, nameEn = "Nebula near Earth's path")
        assertFalse(ARSkyCatalog.isExcludedFromArSky(dso))
    }
}
