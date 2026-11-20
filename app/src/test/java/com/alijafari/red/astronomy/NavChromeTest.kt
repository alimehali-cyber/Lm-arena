package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bottom chrome is one dock and one settings bubble, on every skin: three quarters of the old
 * bar, a squircle pill, on the leading side — the right in Persian, the left in English — with the
 * settings button in the space it leaves, shown exactly where the dock is shown.
 */
class NavChromeTest {

    private fun source(relative: String): String {
        val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText() ?: error("Source not found: $relative")
    }

    private val defaultBar by lazy { source("com/alijafari/red/astronomy/ui/components/FloatingNavBar.kt") }
    private val spatialBar by lazy { source("com/alijafari/red/astronomy/ui/spatial/SpatialFloatingNavBar.kt") }
    private val aetherBar by lazy { source("com/alijafari/red/astronomy/ui/aether/AetherFloatingNavBar.kt") }
    private val activity by lazy { source("com/alijafari/red/astronomy/MainActivity.kt") }

    @Test
    fun everyDockIsAQuarterSmallerAndAPill() {
        assertTrue("one shared width for all three skins", defaultBar.contains("const val BOTTOM_NAV_WIDTH_FRACTION = 0.75f"))
        // 64 -> 48, 64 -> 48 and 62 -> 46: the old height, three quarters of it.
        assertTrue(defaultBar.contains("val BOTTOM_NAV_HEIGHT: Dp = 48.dp"))
        assertTrue(spatialBar.contains("private val SPATIAL_NAV_HEIGHT = 48.dp"))
        assertTrue(aetherBar.contains("private val AETHER_NAV_HEIGHT = 46.dp"))
        for ((name, bar) in listOf("default" to defaultBar, "spatial" to spatialBar, "aether" to aetherBar)) {
            assertTrue("$name dock wears the squircle pill", bar.contains("squircleShape(RedCornerRadius.pill)"))
            assertFalse("$name dock no longer spans the screen by itself", bar.contains(".navigationBarsPadding()"))
        }
    }

    @Test
    fun theDockTakesThreeQuartersOfTheWidthOnTheLeadingSide() {
        assertTrue("the dock and the bubble share one row", activity.contains("horizontalArrangement = Arrangement.SpaceBetween"))
        assertTrue(activity.contains("BOTTOM_NAV_WIDTH_FRACTION"))
        // The dock is the first child of that row, so it takes the start edge: right in Persian,
        // left in English. The bubble is the second, so it takes the opposite one.
        val row = activity.substringAfter("horizontalArrangement = Arrangement.SpaceBetween")
        val navIdx = row.indexOf("AppUiStyle.AETHER -> AetherFloatingNavBar(")
        val bubbleIdx = row.indexOf("AetherSettingsBubble(")
        assertTrue("the dock comes first", navIdx >= 0 && navIdx < bubbleIdx)
    }

    @Test
    fun theSettingsBubbleSitsBesideTheDockOnEverySkin() {
        for ((name, bar) in listOf("default" to defaultBar, "spatial" to spatialBar, "aether" to aetherBar)) {
            assertTrue("$name has its own settings bubble", bar.contains("SettingsBubble("))
            assertTrue("$name bubble is addressable", bar.contains("testTag(\"bottom_settings_button\")"))
        }
        assertTrue(activity.contains("AetherSettingsBubble(onClick = openSettings)"))
        assertTrue(activity.contains("SpatialSettingsBubble(onClick = openSettings)"))
        assertTrue(activity.contains("FloatingSettingsBubble(onClick = openSettings)"))
        // One condition for both: the bubble is drawn inside the dock's own visibility block.
        val block = activity.substringAfter("if (!com.zig.gravity.ui.ImmersiveScreenState.active")
        assertTrue(block.substringBefore("// Dialogs and Modals").contains("FloatingSettingsBubble("))
        assertTrue("settings opens the same dialog", activity.contains("setShowSettingsDialog(true)"))

        // The Home headers no longer own a settings button: it moved down to the chrome.
        for (home in listOf(
            "com/alijafari/red/astronomy/ui/screens/HomeScreen.kt",
            "com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt",
            "com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt",
        )) {
            assertFalse("$home still has the old top settings button", source(home).contains("top_settings_button"))
        }
    }

    @Test
    fun theHudClearsTheShorterDockAndThePreviewsShowTheSameChrome() {
        val stage = source("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        assertTrue("the cards clear the 48 dp pill, not the old 64 dp bar", stage.contains("bottomInset + 68.dp"))
        val previews = File("app/src/debug/java/com/alijafari/red/astronomy/ui/satellites/SatellitePreviews.kt")
            .takeIf { it.isFile }?.readText().orEmpty()
        assertTrue(previews.contains("BOTTOM_NAV_WIDTH_FRACTION"))
        assertTrue(previews.contains("FloatingSettingsBubble("))
        assertTrue(previews.contains("AetherSettingsBubble("))
        assertTrue(previews.contains("SpatialSettingsBubble("))
    }
}
