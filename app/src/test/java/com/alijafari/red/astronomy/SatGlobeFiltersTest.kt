package com.alijafari.red.astronomy

import androidx.compose.runtime.saveable.SaverScope
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.ui.satellites.SatGlobeFilters
import com.alijafari.red.astronomy.ui.satellites.SatGlobeLayer
import com.alijafari.red.astronomy.ui.satellites.SatPanel
import com.alijafari.red.astronomy.ui.satellites.SatellitesState
import com.alijafari.red.astronomy.ui.satellites.SatellitesUiState
import com.alijafari.red.astronomy.ui.satellites.showsOnGlobe
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The top-of-globe Filters button: the filter set's semantics, its persistence, and the fact that a
 * hidden family is pruned from the globe markers without touching the Focus card, Fleet or agenda.
 */
class SatGlobeFiltersTest {

    private fun readSource(relative: String): String {
        val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
        return candidates.map(::File).firstOrNull { it.isFile }?.readText()
            ?: error("Source not found: $relative")
    }

    private fun readRes(relative: String): String {
        val candidates = listOf(File("src/main/res/$relative"), File("app/src/main/res/$relative"))
        return candidates.map(::File).firstOrNull { it.isFile }?.readText()
            ?: error("Resource not found: $relative")
    }

    @Test
    fun everyFamilyIsPaintedByDefault() {
        val filters = SatGlobeFilters()
        for (layer in SatGlobeLayer.entries) {
            assertTrue("$layer must be visible by default", filters.shows(layer))
        }
        assertTrue(filters.hidden.isEmpty())
        assertTrue(filters.anyStarlink)
        assertEquals(filters, SatGlobeFilters.All)
    }

    @Test
    fun hidingOneFamilyLeavesTheOthersAlone() {
        val filters = SatGlobeFilters().toggled(SatGlobeLayer.STARLINK_CONSTELLATION).toggled(SatGlobeLayer.HUBBLE)
        assertFalse(filters.shows(SatGlobeLayer.STARLINK_CONSTELLATION))
        assertFalse(filters.shows(SatGlobeLayer.HUBBLE))
        assertTrue(filters.shows(SatGlobeLayer.STARLINK_OVERHEAD))
        assertTrue(filters.shows(SatGlobeLayer.ISS))
        assertTrue("the Starlink shell is still partly on", filters.anyStarlink)

        val restored = filters.toggled(SatGlobeLayer.HUBBLE)
        assertTrue(restored.shows(SatGlobeLayer.HUBBLE))
        assertEquals(setOf(SatGlobeLayer.STARLINK_CONSTELLATION), restored.hidden)
    }

    @Test
    fun filtersSurviveBothPersistencePaths() {
        val filters = SatGlobeFilters().toggled(SatGlobeLayer.STARLINK_TRAINS).toggled(SatGlobeLayer.TIANGONG)
        assertEquals(filters, SatGlobeFilters.fromPersisted(filters.persisted()))
        assertEquals(setOf("STARLINK_TRAINS", "TIANGONG"), filters.persisted())
        // Unknown or stale names from an older build are ignored instead of crashing the screen.
        assertEquals(emptySet<String>(), SatGlobeFilters.fromPersisted(setOf("RETIRED_LAYER")).persisted())
        assertEquals(emptySet<String>(), SatGlobeFilters.fromPersisted(null).persisted())

        val state = SatellitesState(SatellitesUiState())
        state.toggleFilter(SatGlobeLayer.EARTH_OBSERVATION)
        assertFalse(state.ui.globeFilters.shows(SatGlobeLayer.EARTH_OBSERVATION))
        val saved = with(SatellitesState.Saver) { SaverScope { true }.save(state) }
        assertNotNull(saved)
        val restored = SatellitesState.Saver.restore(saved!!)
        assertNotNull(restored)
        assertEquals(state.ui.globeFilters, restored!!.ui.globeFilters)
    }

    @Test
    fun hiddenFamiliesLeaveTheGlobeButStayInTheLists() {
        val iss = SatelliteCatalog.getById("iss_zarya")
        val hubble = SatelliteCatalog.getById("hubble")
        val starlink = iss.copy(id = "starlink_61000", category = SatelliteCategory.STARLINK)
        val train = starlink.copy(id = "starlink_train", isTrain = true)

        assertTrue(showsOnGlobe(iss, SatGlobeFilters()))
        assertTrue(showsOnGlobe(starlink, SatGlobeFilters()))
        assertTrue(showsOnGlobe(train, SatGlobeFilters()))

        val withoutIss = SatGlobeFilters(setOf(SatGlobeLayer.ISS))
        assertFalse(showsOnGlobe(iss, withoutIss))
        assertTrue("other stations are unaffected", showsOnGlobe(hubble, withoutIss))
        assertTrue(showsOnGlobe(starlink, withoutIss))

        val withoutStarlinks = SatGlobeFilters(
            setOf(SatGlobeLayer.STARLINK_OVERHEAD, SatGlobeLayer.STARLINK_CONSTELLATION),
        )
        assertFalse(showsOnGlobe(starlink, withoutStarlinks))
        assertFalse(withoutStarlinks.anyStarlink)
        assertTrue("trains are its own family", showsOnGlobe(train, withoutStarlinks))

        val withoutTrains = SatGlobeFilters(setOf(SatGlobeLayer.STARLINK_TRAINS))
        assertFalse(showsOnGlobe(train, withoutTrains))
        assertTrue(showsOnGlobe(starlink, withoutTrains))
    }

    @Test
    fun hudAndStageExposeTheFiltersButton() {
        val stage = readSource("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        assertTrue("the Filters button lives on the top header", stage.contains("testTag = \"sat_filters\""))
        assertTrue(stage.contains("SatPanel.Filters"))
        assertTrue("a popover destination", stage.contains("SatFiltersPopover("))
        assertTrue("the globe prunes by family", stage.contains("showsOnGlobe("))
        assertTrue(stage.contains("globeFleetVisible"))
        assertTrue(stage.contains("globeOverheadStates"))
        assertTrue(stage.contains("globeTrainStates"))
        assertTrue(stage.contains("globeInteractiveStates"))
        assertTrue("filters are persisted", stage.contains("\"sat_globe_hidden_layers\""))
        assertTrue("the whole shell obeys its own switch", stage.contains("shows(SatGlobeLayer.STARLINK_CONSTELLATION)"))

        val sheets = readSource("com/alijafari/red/astronomy/ui/satellites/SatelliteSheets.kt")
        assertTrue(sheets.contains("testTag(\"sat_filters_menu\")"))
        assertTrue("each row is addressable", sheets.contains("\"globe_filter_\" + layer.name.lowercase()"))
        assertTrue("one switch per family", sheets.contains("Switch(shown"))

        val english = readRes("values/satellites.xml")
        val persian = readRes("values-fa/satellites.xml")
        val filterStrings = listOf(
            "sat_filters", "sat_filters_subtitle", "sat_filters_show_all",
            "sat_filter_starlink_all", "sat_filter_starlink_overhead", "sat_filter_starlink_trains",
            "sat_filter_iss", "sat_filter_tiangong", "sat_filter_hubble", "sat_filter_earth_observation",
        )
        assertEquals("title, subtitle and show-everything stay in sync with the families",
            SatGlobeLayer.entries.size + 3, filterStrings.size)
        for (name in filterStrings) {
            assertTrue("missing English $name", english.contains("name=\"$name\""))
            assertTrue("missing Persian $name", persian.contains("name=\"$name\""))
        }
        assertTrue(SatPanel.entries.contains(SatPanel.Filters))
    }
}
