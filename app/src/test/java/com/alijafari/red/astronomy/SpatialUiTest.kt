package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.spatial.SpatialMeshPreset
import com.alijafari.red.astronomy.ui.spatial.SpatialNeonRose
import com.alijafari.red.astronomy.ui.spatial.spatialPaletteFor
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the VisionOS Spatial Glassmorphism UI design system, Settings UI switcher,
 * and root MainActivity routing between [AppUiStyle.DEFAULT] and [AppUiStyle.SPATIAL].
 */
class SpatialUiTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    @Test
    fun defaultUiStateStartsInDefaultStyle() {
        val state = MainUiState()
        assertEquals(AppUiStyle.DEFAULT, state.uiStyle)
        assertEquals(listOf(AppUiStyle.DEFAULT, AppUiStyle.SPATIAL), AppUiStyle.entries)
    }

    @Test
    fun spatialPalettesProvideVisionOsGlassAndNeonTokens() {
        assertEquals(Color(0xFFFFB0D0), SpatialNeonRose)

        val dark = spatialPaletteFor(ThemeMode.DARK)
        assertTrue(dark.isDark)
        // rgba(255, 255, 255, 0.12) = 0x1FFFFFFF frosted translucent surface
        assertEquals(Color(0x1FFFFFFF), dark.glassFill)
        assertTrue("Top specular rim must be brighter than side border", dark.rimTopHighlight.alpha > dark.rimSideBorder.alpha)

        val light = spatialPaletteFor(ThemeMode.LIGHT)
        assertTrue(!light.isDark)
        assertTrue("Light frosted quartz fill must be translucent", light.glassFill.alpha in 0.5f..0.85f)
        assertTrue("Top specular rim must be brighter than side border", light.rimTopHighlight.alpha > light.rimSideBorder.alpha)

        assertTrue(SpatialMeshPreset.ROSE_TO_ORANGE.colors.size >= 3)
        assertTrue(SpatialMeshPreset.BLUE_TO_PURPLE.colors.size >= 3)
    }

    @Test
    fun bothSettingsMenusExposeDefaultAndSpatialUiSwitcher() {
        val defaultSettings = source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt")
        val spatialSettings = source("com/alijafari/red/astronomy/ui/spatial/SpatialSettingsModal.kt")

        for (src in listOf(defaultSettings, spatialSettings)) {
            assertTrue(src.contains("settings_section_ui_style"))
            assertTrue(src.contains("settings_ui_style_default"))
            assertTrue(src.contains("settings_ui_style_spatial"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.DEFAULT)"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.SPATIAL)"))
        }
    }

    @Test
    fun mainActivityRoutesBetweenDefaultAndSpatialScreens() {
        val activity = source("com/alijafari/red/astronomy/MainActivity.kt")
        assertTrue(activity.contains("SpatialThemeProvider("))
        assertTrue(activity.contains("SpatialAtmosphereBackground("))
        assertTrue(activity.contains("SpatialHomeScreen("))
        assertTrue(activity.contains("SpatialSatellitesScreen("))
        assertTrue(activity.contains("SpatialLabScreen("))
        assertTrue(activity.contains("SpatialFloatingNavBar("))
        assertTrue(activity.contains("SpatialSettingsModal("))
    }

    @Test
    fun spatialScreensPreserveDefaultLayoutsAndUntouchedFullBleedSkyCanvas() {
        val spatialHome = source("com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt")
        // Sky canvas must remain full-bleed edge-to-edge SCENE with bottomTailHeight = controlsBottom
        assertTrue(spatialHome.contains("presentation = SkyCanvasPresentation.SCENE"))
        assertTrue(spatialHome.contains("bottomTailHeight = controlsBottom"))
        assertTrue(spatialHome.contains("HomeTextShift.moonCard"))
        assertTrue(spatialHome.contains("HomeTextShift.eclipseCard"))
        assertTrue(spatialHome.contains("home_moon_card"))
        assertTrue(spatialHome.contains("home_eclipse_card"))
        assertTrue(spatialHome.contains("home_highlights_card"))
        assertTrue(spatialHome.contains("sky_canvas_fullscreen"))

        val spatialSats = source("com/alijafari/red/astronomy/ui/spatial/SpatialSatellitesScreen.kt")
        assertTrue(spatialSats.contains("aspectRatio(2.0f)"))
        assertTrue(spatialSats.contains("satellite_time_scrubber_card"))
        assertTrue(spatialSats.contains("sat_card_"))

        val spatialLab = source("com/alijafari/red/astronomy/ui/spatial/SpatialLabScreen.kt")
        assertTrue(spatialLab.contains("LabFeatureType.entries"))
        assertTrue(spatialLab.contains("lab_feature_card_"))
    }
}
