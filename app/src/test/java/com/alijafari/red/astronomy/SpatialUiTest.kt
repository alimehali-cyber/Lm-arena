package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.spatial.SpatialMeshPreset
import com.alijafari.red.astronomy.ui.spatial.SpatialNeonRose
import com.alijafari.red.astronomy.ui.spatial.spatialPaletteFor
import com.alijafari.red.astronomy.ui.theme.RedWineColorTokens
import com.alijafari.red.astronomy.ui.theme.wcagContrast
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the selectable UI design systems ([AppUiStyle.DEFAULT], [AppUiStyle.SPATIAL],
 * [AppUiStyle.AETHER]), the Wine ("می") burgundy theme, Settings switchers,
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
    fun defaultUiStateStartsInDefaultStyleAndListsAllThreeUisAndWineTheme() {
        val state = MainUiState()
        assertEquals(AppUiStyle.DEFAULT, state.uiStyle)
        assertEquals(
            listOf(AppUiStyle.DEFAULT, AppUiStyle.SPATIAL, AppUiStyle.AETHER),
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
    fun allRemainingSettingsMenusExposeAllUiStylesAndWineTheme() {
        val defaultSettings = source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt")
        val spatialSettings = source("com/alijafari/red/astronomy/ui/spatial/SpatialSettingsModal.kt")
        val aetherSettings = source("com/alijafari/red/astronomy/ui/aether/AetherSettingsModal.kt")

        for (src in listOf(defaultSettings, spatialSettings, aetherSettings)) {
            assertTrue(src.contains("settings_section_ui_style"))
            assertTrue(src.contains("settings_ui_style_default"))
            assertTrue(src.contains("settings_ui_style_spatial"))
            assertTrue(src.contains("settings_ui_style_aether"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.DEFAULT)"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.SPATIAL)"))
            assertTrue(src.contains("viewModel.setUiStyle(AppUiStyle.AETHER)"))
            assertTrue(src.contains("settings_theme_wine"))
            assertTrue(src.contains("viewModel.setThemeMode(ThemeMode.WINE)"))
        }
    }

    @Test
    fun mainActivityRoutesOnlyRemainingDefaultSpatialAndAetherScreens() {
        val activity = source("com/alijafari/red/astronomy/MainActivity.kt")
        assertTrue(activity.contains("SpatialThemeProvider("))
        assertTrue(activity.contains("SpatialAtmosphereBackground("))
        assertTrue(activity.contains("SpatialHomeScreen("))
        assertTrue(activity.contains("SpatialSatellitesScreen("))
        assertTrue(activity.contains("SpatialLabScreen("))
        assertTrue(activity.contains("SpatialFloatingNavBar("))
        assertTrue(activity.contains("SpatialSettingsModal("))


        assertTrue(activity.contains("AetherThemeProvider("))
        assertTrue(activity.contains("AetherAmbientBackground("))
        assertTrue(activity.contains("AetherHomeScreen("))
        assertTrue(activity.contains("AetherSatellitesScreen("))
        assertTrue(activity.contains("AetherLabScreen("))
        assertTrue(activity.contains("AetherFloatingNavBar("))
        assertTrue(activity.contains("AetherSettingsModal("))
    }

    @Test
    fun spatialAndAetherDotMatricesShareGlyphsAndRenderingStyle() {
        val spatial = source("com/alijafari/red/astronomy/ui/spatial/SpatialDotMatrix.kt")
        val aether = source("com/alijafari/red/astronomy/ui/aether/AetherDotMatrix.kt")

        // The two non-default styles share the seven-row circular-dot bitmap family.
        assertTrue(spatial.contains("dotSize: Dp = 2.6.dp"))
        assertTrue(spatial.contains("dotGap: Dp = 1.05.dp"))
        assertTrue(spatial.contains("charGap: Dp = 3.2.dp"))
        assertTrue(aether.contains("dotSize: Dp = 2.6.dp"))
        assertTrue(aether.contains("dotGap: Dp = 1.05.dp"))
        assertTrue(aether.contains("charGap: Dp = 3.2.dp"))
        assertTrue(aether.contains("private const val AETHER_GLYPH_ROWS = 7"))
        assertTrue(aether.contains("private fun glyph5("))
        assertFalse(aether.contains("g6("))

        // Active-dot halos and unlit-cell sizes match; palette colors remain style-specific.
        for (renderer in listOf(spatial, aether)) {
            assertTrue(renderer.contains("copy(alpha = 0.24f)"))
            assertTrue(renderer.contains("radius = r * 2.0f") || renderer.contains("radius = radius * 2.0f"))
            assertTrue(renderer.contains("radius = r * 0.58f") || renderer.contains("radius = radius * 0.58f"))
        }

        fun glyphEntries(source: String, functionName: String): Map<String, String> =
            source.substringAfter("private fun $functionName")
                .lineSequence()
                .map(String::trim)
                .filter { " -> " in it }
                .associate { line ->
                    val normalized = line.replace("glyph5(", "g5(").replace("glyph3(", "g3(")
                    normalized.substringBefore(" -> ") to normalized
                }

        val spatialGlyphs = glyphEntries(spatial, "glyphFor")
        val aetherGlyphs = glyphEntries(aether, "aetherGlyphFor")
        assertEquals(spatialGlyphs, aetherGlyphs.filterKeys(spatialGlyphs::containsKey))

        // Pin the approved Persian forms explicitly, not merely parity between the UIs.
        val expectedPersianFour = "g5(0x07, 0x14, 0x1F, 0x10, 0x10, 0x10, 0x10)"
        val expectedPersianSix = "g5(0x06, 0x08, 0x07, 0x04, 0x08, 0x10, 0x10)"
        for (glyphs in listOf(spatialGlyphs, aetherGlyphs)) {
            assertEquals(expectedPersianFour, glyphs["'۴', '٤'"]?.substringAfter(" -> "))
            assertEquals(expectedPersianSix, glyphs["'۶', '٦'"]?.substringAfter(" -> "))
        }
    }

    @Test
    fun spatialScreenPreservesItsSkyCanvasInvariants() {
        val spatialHome = source("com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt")
        assertTrue(spatialHome.contains("presentation = SkyCanvasPresentation.SCENE"))
        assertTrue(spatialHome.contains("bottomTailHeight = controlsBottom"))
        assertTrue(spatialHome.contains("HomeTextShift.moonCard"))
        assertTrue(spatialHome.contains("HomeTextShift.eclipseCard"))

    }

    @Test
    fun satelliteWorldMapsAreBorderlessFullBleedAndKeepTheGeographicAspectRatio() {
        val stage = source("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        val components = source("com/alijafari/red/astronomy/ui/satellites/SatelliteComponents.kt")
        val iss = source("com/alijafari/red/astronomy/ui/screens/ISSScreen.kt")
        val spatial = source("com/alijafari/red/astronomy/ui/spatial/SpatialSatellitesScreen.kt")
        val aether = source("com/alijafari/red/astronomy/ui/aether/AetherSatellitesScreen.kt")

        // The globe is full-bleed on the shared stage and framed at the geographic 1.16:1 ratio.
        assertTrue(stage.contains("SatelliteEarth3D("))
        assertTrue(components.contains(".aspectRatio(1.16f)"))
        assertTrue(stage.contains("globeModifier"))
        listOf(iss, spatial, aether).forEach { screen ->
            assertTrue(screen.contains("globeModifier"))
            assertFalse(screen.contains("WorldGeographyData.normalizedPaths"))
        }
        assertTrue(iss.contains("offset(x = (-16).dp)"))
        assertTrue(spatial.contains("offset(x = (-16).dp)"))
        assertTrue(aether.contains("offset(x = (-20).dp)"))
        // Default is flat by contract: the stage never paints a shadow or a blur on a card.
        assertFalse(components.contains("Modifier.shadow("))
        assertFalse(components.contains("blur("))
        assertFalse(aether.contains("viewportShape"))
    }

    @Test
    fun savedCyberStyleMigratesToDefault() {
        val viewModel = source("com/alijafari/red/astronomy/ui/MainViewModel.kt")
        assertTrue(viewModel.contains("if (saved == \"CYBER\")"))
        assertTrue(viewModel.contains("putString(\"app_ui_style\", AppUiStyle.DEFAULT.name)"))
        assertTrue(viewModel.contains("return AppUiStyle.DEFAULT"))
    }
}
