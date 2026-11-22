package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bottom chrome is one capsule dock and one circular settings button, on every skin: a little
 * bigger than the previous generation, close together in one centred cluster at the bottom of the
 * screen, shown exactly where the dock is shown.
 */
class NavChromeTest {

    private fun file(sourceSet: String, relative: String): String {
        val candidates = listOf(File("$sourceSet/java/$relative"), File("app/$sourceSet/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText() ?: error("Source not found: $relative")
    }

    private fun source(relative: String): String = file("src/main", relative)

    private val defaultBar by lazy { source("com/alijafari/red/astronomy/ui/components/FloatingNavBar.kt") }
    private val spatialBar by lazy { source("com/alijafari/red/astronomy/ui/spatial/SpatialFloatingNavBar.kt") }
    private val aetherBar by lazy { source("com/alijafari/red/astronomy/ui/aether/AetherFloatingNavBar.kt") }
    private val activity by lazy { source("com/alijafari/red/astronomy/MainActivity.kt") }

    @Test
    fun everyDockIsABiggerCapsulePillAndEverySettingsButtonIsACircle() {
        assertTrue("one shared width for all three skins", defaultBar.contains("const val BOTTOM_NAV_WIDTH_FRACTION = 0.72f"))
        // A little bigger than the 48/48/46 generation: 56, 56 and 54.
        assertTrue(defaultBar.contains("val BOTTOM_NAV_HEIGHT: Dp = 56.dp"))
        assertTrue(spatialBar.contains("private val SPATIAL_NAV_HEIGHT = 56.dp"))
        assertTrue(aetherBar.contains("private val AETHER_NAV_HEIGHT = 54.dp"))
        assertTrue("one shared tight gap for the cluster", defaultBar.contains("val BOTTOM_NAV_GAP: Dp = 8.dp"))
        for ((name, bar) in listOf("default" to defaultBar, "spatial" to spatialBar, "aether" to aetherBar)) {
            assertTrue("$name dock wears the squircle pill with fully rounded ends", bar.contains("squircleShape(RedCornerRadius.pill)"))
            assertTrue("$name settings button is a true circle", bar.contains("CircleShape"))
            assertFalse("$name dock no longer spans the screen by itself", bar.contains(".navigationBarsPadding()"))
        }
    }

    @Test
    fun theDockAndSettingsCircleAreCenteredCloseTogether() {
        // One row, one cluster: tight gap, centred as a unit instead of split to the edges.
        val chrome = activity.substringAfter("SplashRevealRole.NavigationBar")
            .substringBefore("// Dialogs and Modals")
        assertTrue("cluster uses a tight spaced-by gap", chrome.contains("Arrangement.spacedBy("))
        assertTrue("cluster is centred", chrome.contains("Alignment.CenterHorizontally"))
        assertTrue("the dock still takes its shared width fraction", chrome.contains("BOTTOM_NAV_WIDTH_FRACTION"))
        // The dock is the first child of that row and the circle the second.
        val navIdx = chrome.indexOf("AppUiStyle.AETHER -> AetherFloatingNavBar(")
        val bubbleIdx = chrome.indexOf("AetherSettingsBubble(")
        assertTrue("the dock comes first", navIdx >= 0 && navIdx < bubbleIdx)
    }

    @Test
    fun theSettingsCircleSitsBesideTheDockOnEverySkin() {
        for ((name, bar) in listOf("default" to defaultBar, "spatial" to spatialBar, "aether" to aetherBar)) {
            assertTrue("$name has its own settings circle", bar.contains("SettingsBubble("))
            assertTrue("$name circle is addressable", bar.contains("testTag(\"bottom_settings_button\")"))
        }
        assertTrue(activity.contains("AetherSettingsBubble(onClick = openSettings)"))
        assertTrue(activity.contains("SpatialSettingsBubble(onClick = openSettings)"))
        assertTrue(activity.contains("FloatingSettingsBubble(onClick = openSettings)"))
        // One condition for both: the circle is drawn inside the dock's own visibility block.
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
    fun theHudClearsTheBiggerDockAndThePreviewsShowTheSameChrome() {
        val stage = source("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        assertTrue("the cards clear the 56 dp pill, not the old 48 dp one", stage.contains("bottomInset + 76.dp"))
        val previews = file("src/debug", "com/alijafari/red/astronomy/ui/satellites/SatellitePreviews.kt")
        assertTrue(previews.contains("BOTTOM_NAV_WIDTH_FRACTION"))
        assertTrue(previews.contains("BOTTOM_NAV_GAP"))
        assertTrue(previews.contains("FloatingSettingsBubble("))
        assertTrue(previews.contains("AetherSettingsBubble("))
        assertTrue(previews.contains("SpatialSettingsBubble("))
    }
}
