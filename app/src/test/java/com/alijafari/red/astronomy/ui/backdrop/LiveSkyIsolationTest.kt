package com.alijafari.red.astronomy.ui.backdrop

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rules that the Live Sky backdrop must not break. Each check reads the production source. This is the same
 * approach ContentIntegrityAuditTest uses, and it keeps these rules enforced without any device.
 */
class LiveSkyIsolationTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("Source not found: $relative (searched ${candidates.joinToString()})")
        return file.readText()
    }

    private val backdropFiles = listOf(
        "com/alijafari/red/astronomy/ui/backdrop/LiveSkyBackdrop.kt",
        "com/alijafari/red/astronomy/ui/backdrop/LiveSkyPolicy.kt",
        "com/alijafari/red/astronomy/ui/backdrop/LiveSkyFrameRequest.kt",
        "com/alijafari/red/astronomy/ui/backdrop/SkyBackdropViewport.kt",
        "com/alijafari/red/astronomy/ui/rendering/SkySceneModel.kt",
        "com/alijafari/red/astronomy/ui/rendering/SkySceneRenderer.kt"
    )

    @Test
    fun backdropDoesNotDependOnThePanoramaFeatureGate() {
        for (path in backdropFiles) {
            val text = source(path)
            assertFalse("$path references the panorama feature gate", text.contains("SkyPanoramaFeature"))
            assertFalse("$path imports the skypanorama package", text.contains("ui.skypanorama"))
        }
    }

    @Test
    fun backdropUsesOpenGlEsOnlyAndNoVulkan() {
        for (path in backdropFiles) {
            val text = source(path).lowercase()
            assertFalse("$path mentions Vulkan", text.contains("vulkan"))
        }
    }

    @Test
    fun backdropIsMountedInTheSharedLayerAndSettingsHasTheSwitch() {
        val main = source("com/alijafari/red/astronomy/MainActivity.kt")
        assertTrue("MainActivity must mount LiveSkyBackdrop", main.contains("LiveSkyBackdrop("))
        assertTrue("MainActivity must keep the glass layer", main.contains("Modifier.layerBackdrop(backdrop)"))

        val settings = source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt")
        assertTrue("Settings must expose the Live Sky switch", settings.contains("settings_live_sky_switch"))
        assertTrue("Settings must call the Live Sky setter", settings.contains("setLiveSkyBackdropEnabled"))
    }

    @Test
    fun homeCardStillUsesTheSharedPipelineAndKeepsItsInteraction() {
        val hero = source("com/alijafari/red/astronomy/ui/components/HeroSkyCanvas.kt")
        assertTrue("Home must draw through SkySceneRenderer", hero.contains("SkySceneRenderer.drawSky("))
        assertTrue("Home must keep its tap-target ring overlay", hero.contains("Tapped Celestial Target Ring Overlay"))
    }

    @Test
    fun liveSkyStringsExistInEnglishAndPersian() {
        val en = resource("values/strings.xml")
        val fa = resource("values-fa/strings.xml")
        for (key in listOf("app_backdrop_title", "live_sky_setting", "live_sky_desc")) {
            assertTrue("missing $key in values", en.contains("name=\"$key\""))
            assertTrue("missing $key in values-fa", fa.contains("name=\"$key\""))
        }
    }

    private fun resource(relative: String): String {
        val candidates = listOf("src/main/res/$relative", "app/src/main/res/$relative")
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("Resource not found: $relative")
        return file.readText()
    }
}
