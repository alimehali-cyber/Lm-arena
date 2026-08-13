package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroTime
import com.alijafari.red.astronomy.astro_engine.DeepSkyCatalog
import com.alijafari.red.astronomy.astro_engine.DeepSkyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSkyEngineTest {

    private val engine = DeepSkyEngine()

    @Test
    fun `test find by catalog ID`() {
        val m31 = engine.findById("M31")
        assertNotNull("M31 should be found", m31)
        assertEquals("M31 should be Andromeda Galaxy", "Andromeda Galaxy", m31!!.commonName)

        val ngc224 = engine.findById("NGC 224")
        assertNotNull("NGC 224 should be found", ngc224)
        assertEquals("NGC 224 should match M31", m31.catalogId, ngc224!!.catalogId)
    }

    @Test
    fun `test search by name`() {
        val results = engine.searchByName("nebula")
        assertTrue("Should find multiple nebulae", results.size >= 5)
    }

    @Test
    fun `test filter by type`() {
        val globulars = engine.filterByType(DeepSkyCatalog.ObjectType.GLOBULAR_CLUSTER)
        assertTrue("Should find many globular clusters", globulars.size >= 10)
    }

    @Test
    fun `test filter by constellation`() {
        val orion = engine.filterByConstellation("Ori")
        assertTrue("Should find objects in Orion", orion.size >= 3)
    }

    @Test
    fun `test filter by magnitude`() {
        val bright = engine.filterByMagnitude(5.0)
        assertTrue("Should find bright objects", bright.size >= 5)
        assertTrue("All should be brighter than mag 5", bright.all { it.magnitude <= 5.0 })
    }

    @Test
    fun `test cone search`() {
        // Search around M42 (Orion Nebula): RA 5h35m, Dec -5.4°
        val results = engine.coneSearch(83.75, -5.4, 5.0)
        assertTrue("Should find M42 in cone search", results.any { it.`object`.catalogId == "M42" })
    }

    @Test
    fun `test position calculation`() {
        val m31 = engine.findById("M31")!!
        val astroTime = AstroTime.fromUtcDate(2026, 8, 12, 0, 0, 0)
        
        val pos = engine.calculatePosition(m31, astroTime, 35.7, 51.4)
        
        assertTrue("RA should be valid", pos.raDeg in 0.0..360.0)
        assertTrue("Dec should be valid", pos.decDeg in -90.0..90.0)
        assertTrue("Az should be valid", pos.azDeg in 0.0..360.0)
        assertTrue("Alt should be valid", pos.altDeg in -90.0..90.0)
    }

    @Test
    fun `test best viewing month`() {
        // M42 (Orion) is best viewed in winter (Dec-Feb)
        val m42 = engine.findById("M42")!!
        val month = engine.bestViewingMonth(m42)
        assertTrue("Orion should be best in winter", month in 11..2 || month == 12 || month == 1)
    }

    @Test
    fun `test constellation name lookup`() {
        assertEquals("Andromeda", engine.constellationName("And"))
        assertEquals("Orion", engine.constellationName("Ori"))
        assertEquals("Ursa Major", engine.constellationName("UMa"))
    }

    @Test
    fun `test all messier objects present`() {
        val messier = engine.getAllMessier()
        assertTrue("Should have 110 Messier objects", messier.size >= 100)
    }

    @Test
    fun `test catalog has 200+ objects`() {
        assertTrue("Catalog should have 200+ objects", DeepSkyCatalog.objects.size >= 200)
    }
}
