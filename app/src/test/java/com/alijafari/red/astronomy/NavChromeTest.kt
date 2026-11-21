package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bottom chrome is a centered capsule dock paired with one circular settings control. */
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
    fun everyStyleUsesTheSharedLargerPillAndCircularControl() {
        assertTrue("all styles share the larger capsule height", defaultBar.contains("val BOTTOM_NAV_HEIGHT: Dp = 56.dp"))
        assertTrue("the settings control matches the capsule height", defaultBar.contains("val BOTTOM_SETTINGS_SIZE: Dp = 56.dp"))
        assertTrue("the controls stay close together", defaultBar.contains("val BOTTOM_NAV_SETTINGS_GAP: Dp = 10.dp"))
        assertTrue("the dock keeps its width proportion while gaining height", defaultBar.contains("const val BOTTOM_NAV_WIDTH_FRACTION = 0.75f"))
        assertTrue("the dock has a tablet width cap", defaultBar.contains("val BOTTOM_NAV_MAX_WIDTH: Dp = 420.dp"))
        assertTrue("the responsive dock leaves room for the settings control", defaultBar.contains("(availableWidth - BOTTOM_SETTINGS_SIZE - BOTTOM_NAV_SETTINGS_GAP).coerceAtLeast(0.dp)"))
        assertTrue("HUD clearance accounts for whichever control is taller", defaultBar.contains("val BOTTOM_CONTROLS_HEIGHT: Dp = maxOf(BOTTOM_NAV_HEIGHT, BOTTOM_SETTINGS_SIZE)"))

        for ((name, bar, settingsName) in listOf(
            Triple("default", defaultBar, "FloatingSettingsBubble"),
            Triple("spatial", spatialBar, "SpatialSettingsBubble"),
            Triple("aether", aetherBar, "AetherSettingsBubble"),
        )) {
            assertTrue("$name dock uses the shared height", bar.contains(".height(BOTTOM_NAV_HEIGHT)"))
            val dockName = when (name) {
                "default" -> "FloatingBottomBar"
                "spatial" -> "SpatialFloatingNavBar"
                else -> "AetherFloatingNavBar"
            }
            val dock = bar.substringAfter("fun $dockName(").substringBefore("fun $settingsName(")
            val usesPill = if (name == "default") {
                dock.contains("bottomNavShape()") &&
                    bar.contains("internal fun bottomNavShape() = squircleShape(RedCornerRadius.pill)")
            } else {
                dock.contains("squircleShape(RedCornerRadius.pill)")
            }
            assertTrue("$name dock wears the squircle capsule", usesPill)
            assertFalse("$name dock does not apply its own navigation inset", bar.contains(".navigationBarsPadding()"))

            val settings = bar.substringAfter("fun $settingsName(")
            assertTrue("$name settings control is circular",
                settings.contains("shape = CircleShape") || settings.contains("val shape = CircleShape"))
            assertTrue("$name settings control uses the shared size", settings.contains(".size(BOTTOM_SETTINGS_SIZE)"))
            assertTrue("$name settings control is addressable", settings.contains("testTag(\"bottom_settings_button\")"))
        }
    }

    @Test
    fun theDockAndSettingsButtonFormACompactCenteredGroup() {
        assertTrue("the group uses a compact gap and centered arrangement",
            activity.contains("Arrangement.spacedBy(") &&
                activity.contains("BOTTOM_NAV_SETTINGS_GAP") &&
                activity.contains("Alignment.CenterHorizontally"))
        assertFalse("the controls are no longer pushed to opposite ends",
            activity.contains("horizontalArrangement = Arrangement.SpaceBetween"))
        assertTrue("the outer layout sizes the dock responsively", activity.contains("BoxWithConstraints(") && activity.contains("bottomNavWidth(maxWidth)"))
        assertTrue("the dock width accounts for its circular companion", defaultBar.contains("BOTTOM_SETTINGS_SIZE - BOTTOM_NAV_SETTINGS_GAP"))

        for ((dock, settingsName) in listOf(
            "AetherFloatingNavBar(" to "AetherSettingsBubble",
            "SpatialFloatingNavBar(" to "SpatialSettingsBubble",
            "FloatingBottomBar(" to "FloatingSettingsBubble",
        )) {
            val dockIndex = activity.indexOf(dock)
            val settingsIndex = activity.indexOf("$settingsName(", dockIndex)
            assertTrue("$dock is followed by its settings control", dockIndex >= 0 && settingsIndex > dockIndex)
            val settingsCall = activity.substring(settingsIndex, activity.indexOf(')', settingsIndex))
            assertTrue("$settingsName opens the shared settings action", settingsCall.contains("onClick = openSettings"))
            assertTrue("$settingsName receives the measured launch origin", settingsCall.contains("settingsButtonModifier"))
        }
    }

    @Test
    fun everySettingsButtonOpensTheSameDialogAndOldHomeButtonsStayRemoved() {
        for (settings in listOf("AetherSettingsBubble(", "SpatialSettingsBubble(", "FloatingSettingsBubble(")) {
            val call = activity.substringAfter(settings).substringBefore(")")
            assertTrue("$settings opens the shared settings action", call.contains("onClick = openSettings"))
        }
        val block = activity.substringAfter("if (!com.zig.gravity.ui.ImmersiveScreenState.active")
        assertTrue("settings remains in the bottom-chrome visibility block",
            block.substringBefore("// Dialogs and Modals").contains("FloatingSettingsBubble("))
        assertTrue("settings opens the same dialog", activity.contains("setShowSettingsDialog(true)"))

        for (home in listOf(
            "com/alijafari/red/astronomy/ui/screens/HomeScreen.kt",
            "com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt",
            "com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt",
        )) {
            assertFalse("$home still has the old top settings button", source(home).contains("top_settings_button"))
        }
    }

    @Test
    fun theHudClearsTheControlsAndPreviewsMatchTheProductionGroup() {
        val stage = source("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        assertTrue("the cards clear the taller control plus the inset and safety gap",
            stage.contains("bottomInset + BOTTOM_CONTROLS_HEIGHT + 20.dp"))
        val previews = file("src/debug", "com/alijafari/red/astronomy/ui/satellites/SatellitePreviews.kt")
        assertTrue("previews use the same responsive dock width", previews.contains("bottomNavWidth(maxWidth)"))
        assertTrue("previews center the controls with the same gap", previews.contains("BOTTOM_NAV_SETTINGS_GAP") && previews.contains("Alignment.CenterHorizontally"))
        assertTrue(previews.contains("FloatingSettingsBubble("))
        assertTrue(previews.contains("AetherSettingsBubble("))
        assertTrue(previews.contains("SpatialSettingsBubble("))
    }
}
