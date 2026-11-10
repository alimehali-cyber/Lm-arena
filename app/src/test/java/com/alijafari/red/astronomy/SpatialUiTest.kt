package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.cyber.CyberMeshPreset
import com.alijafari.red.astronomy.ui.cyber.cyberPaletteFor
import com.alijafari.red.astronomy.ui.spatial.SpatialMeshPreset
import com.alijafari.red.astronomy.ui.spatial.SpatialNeonRose
import com.alijafari.red.astronomy.ui.spatial.spatialPaletteFor
import com.alijafari.red.astronomy.ui.theme.RedWineColorTokens
import com.alijafari.red.astronomy.ui.theme.wcagContrast
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies all three selectable UI design systems ([AppUiStyle.DEFAULT], [AppUiStyle.SPATIAL],
 * [AppUiStyle.CYBER]), the Wine ("می") burgundy theme across all three UIs, Settings switchers,
 * and MainActivity routing.
 */
class SpatialUiTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private fun resource(relative: String): String {
        val candidates = listOf("src/main/res/$relative", "app/src/main/res/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Resource not found: $relative")
    }

    @Test
    fun defaultUiStateStartsInDefaultStyleAndListsAllUisAndWineTheme() {
        val state = MainUiState()
        assertEquals(AppUiStyle.DEFAULT, state.uiStyle)
        assertEquals(
            listOf(AppUiStyle.DEFAULT, AppUiStyle.SPATIAL, AppUiStyle.CYBER, AppUiStyle.AETHER),
            AppUiStyle.entries
        )
        assertEquals(
            listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.WINE),
            ThemeMode.entries
        )
    }

    @Test
    fun spatialPalettesProvideVisionOsGlassNeonAndWineTokens() {
        assertEquals(Color(0xFFFFB0D0), SpatialNeonRose)

        val dark = spatialPaletteFor(ThemeMode.DARK)
        assertTrue(dark.isDark)
        assertEquals(Color(0x1FFFFFFF), dark.glassFill)
        assertTrue("Top specular rim must be brighter than side border", dark.rimTopHighlight.alpha > dark.rimSideBorder.alpha)

        val light = spatialPaletteFor(ThemeMode.LIGHT)
        assertTrue(!light.isDark)
        assertTrue("Light frosted quartz fill must be translucent", light.glassFill.alpha in 0.5f..0.85f)
        assertTrue("Top specular rim must be brighter than side border", light.rimTopHighlight.alpha > light.rimSideBorder.alpha)

        val wine = spatialPaletteFor(ThemeMode.WINE)
        assertTrue(wine.isDark)
        assertTrue("Wine Spatial backing veil is burgundy-red dominant", wine.glassBackingVeil.red > wine.glassBackingVeil.green)
        assertTrue("Wine Spatial neon glow is claret-crimson dominant", wine.neonGlow.red > wine.neonGlow.blue)

        assertTrue(SpatialMeshPreset.ROSE_TO_ORANGE.colors.size >= 3)
        assertTrue(SpatialMeshPreset.BLUE_TO_PURPLE.colors.size >= 3)
        assertTrue(SpatialMeshPreset.WINE_VELVET.colors.size >= 3)
    }

    @Test
    fun cyberGlassPalettesImplementFourLayerCutGlassAndWineBurgundy() {
        val dark = cyberPaletteFor(ThemeMode.DARK)
        assertTrue(dark.isDark)
        // Exact Section 3A recipe: rgba(255, 255, 255, 0.07) = 0x12FFFFFF, border white/18 = 0x2EFFFFFF
        assertEquals(Color(0x12FFFFFF), dark.glassFill)
        assertEquals(Color(0x2EFFFFFF), dark.glassBorder)
        assertTrue("Top inner refraction highlight present", dark.innerTopHighlight.alpha >= 0.28f)
        assertTrue("Bottom inner shadow present", dark.innerBottomShadow.alpha >= 0.10f)

        val light = cyberPaletteFor(ThemeMode.LIGHT)
        assertTrue(!light.isDark)
        assertTrue(light.glassFill.alpha in 0.55f..0.80f)

        val wine = cyberPaletteFor(ThemeMode.WINE)
        assertTrue(wine.isDark && wine.isWine)
        assertTrue("Wine Cyber canvas is deep cabernet burgundy", wine.canvasBg.red > wine.canvasBg.green)
        assertTrue("Wine Cyber signal dot is crimson", wine.signalDot.red > 0.9f)
        assertTrue(CyberMeshPreset.CORAL_PEACH.wineColors.size >= 3)
        assertTrue(CyberMeshPreset.CYAN_VIOLET.wineColors.size >= 3)
    }

    @Test
    fun defaultUiWineThemeIsBurgundyAndMeetsWcagContrast() {
        assertTrue(RedWineColorTokens.isDark)
        assertTrue(
            "Default Wine surface is burgundy-red dominant",
            RedWineColorTokens.surfaceElevated.red > RedWineColorTokens.surfaceElevated.green &&
                RedWineColorTokens.surfaceElevated.red > RedWineColorTokens.surfaceElevated.blue
        )
        assertTrue(
            "Default Wine accent is rich wine crimson",
            RedWineColorTokens.accentRed.red > 0.75f
        )
        val textContrast = wcagContrast(RedWineColorTokens.textPrimary, RedWineColorTokens.surfaceElevated)
        assertTrue("Wine primary text clears WCAG AA 4.5:1 ($textContrast)", textContrast >= 4.5f)

        val stringsEn = resource("values/strings.xml")
        val stringsFa = resource("values-fa/strings.xml")
        assertTrue(stringsEn.contains("<string name=\"theme_wine\">Wine</string>"))
        assertTrue(stringsFa.contains("<string name=\"theme_wine\">می</string>"))
    }

    @Test
    fun allThreeSettingsMenusExposeAllThreeUiStylesAndWineTheme() {
        val defaultSettings = source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt")
        val spatialSettings = source("com/alijafari/red/astronomy/ui/spatial/SpatialSettingsModal.kt")
        val cyberSettings = source("com/alijafari/red/astronomy/ui/cyber/CyberSettingsModal.kt")

        for (src in listOf(defaultSettings, spatialSettings, cyberSettings)) {
            assertTrue(src.contains("settings_section_ui_style"))
            assertTrue(src.contains("settings_ui_style_default"))
            assertTrue(src.contains("settings_ui_style_spatial"))
            assertTrue(src.contains("settings_ui_style_cyber"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.DEFAULT)"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.SPATIAL)"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.CYBER)"))
            assertTrue(src.contains("settings_theme_wine"))
            assertTrue(src.contains("viewModel.setThemeMode(ThemeMode.WINE)"))
        }
    }

    @Test
    fun mainActivityRoutesAcrossDefaultSpatialAndCyberGlassScreens() {
        val activity = source("com/alijafari/red/astronomy/MainActivity.kt")
        assertTrue(activity.contains("SpatialThemeProvider("))
        assertTrue(activity.contains("SpatialAtmosphereBackground("))
        assertTrue(activity.contains("SpatialHomeScreen("))
        assertTrue(activity.contains("SpatialSatellitesScreen("))
        assertTrue(activity.contains("SpatialLabScreen("))
        assertTrue(activity.contains("SpatialFloatingNavBar("))
        assertTrue(activity.contains("SpatialSettingsModal("))

        assertTrue(activity.contains("CyberThemeProvider("))
        assertTrue(activity.contains("CyberAmbientBackground("))
        assertTrue(activity.contains("CyberHomeScreen("))
        assertTrue(activity.contains("CyberSatellitesScreen("))
        assertTrue(activity.contains("CyberLabScreen("))
        assertTrue(activity.contains("CyberFloatingNavBar("))
        assertTrue(activity.contains("CyberSettingsModal("))
    }

    @Test
    fun spatialAndCyberScreensPreserveInvariantsAndSignatureComponents() {
        val spatialHome = source("com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt")
        assertTrue(spatialHome.contains("presentation = SkyCanvasPresentation.SCENE"))
        assertTrue(spatialHome.contains("bottomTailHeight = controlsBottom"))
        assertTrue(spatialHome.contains("HomeTextShift.moonCard"))
        assertTrue(spatialHome.contains("HomeTextShift.eclipseCard"))

        val cyberDesign = source("com/alijafari/red/astronomy/ui/cyber/CyberDesignSystem.kt")
        assertTrue(cyberDesign.contains("fun CyberGlassWidget("))
        assertTrue(cyberDesign.contains("fun CyberMeshPillCard("))
        assertTrue(cyberDesign.contains("fun CyberFloatingCapsuleBar("))
        assertTrue(cyberDesign.contains("fun CyberCutoutViewportCard("))

        val cyberHome = source("com/alijafari/red/astronomy/ui/cyber/CyberHomeScreen.kt")
        assertTrue(cyberHome.contains("presentation = SkyCanvasPresentation.SCENE"))
        assertTrue(cyberHome.contains("CyberCutoutViewportCard("))
        assertTrue(cyberHome.contains("CyberMeshPillCard("))
        assertTrue(cyberHome.contains("CyberFloatingCapsuleBar("))
        assertTrue(cyberHome.contains("CyberMatrixText("))
    }
}
