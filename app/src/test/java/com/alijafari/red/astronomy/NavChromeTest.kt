package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bottom chrome is the fluid pill design on every skin: a capsule whose CPU stable-fluid dye
 * lens flows between tabs, plus one circular settings button, close together in one centred
 * cluster at the bottom of the screen. The fluid is primarily #660033 on every UI and lives
 * continuously; each skin stays unique through its chrome.
 */
class NavChromeTest {

    private fun file(sourceSet: String, relative: String): String {
        val candidates = listOf(File("$sourceSet/java/$relative"), File("app/$sourceSet/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText() ?: error("Source not found: $relative")
    }

    private fun source(relative: String): String = file("src/main", relative)

    private val engine by lazy { source("com/alijafari/red/astronomy/ui/components/FluidPillNavBar.kt") }
    private val defaultBar by lazy { source("com/alijafari/red/astronomy/ui/components/FloatingNavBar.kt") }
    private val spatialBar by lazy { source("com/alijafari/red/astronomy/ui/spatial/SpatialFloatingNavBar.kt") }
    private val aetherBar by lazy { source("com/alijafari/red/astronomy/ui/aether/AetherFloatingNavBar.kt") }
    private val activity by lazy { source("com/alijafari/red/astronomy/MainActivity.kt") }

    @Test
    fun theFluidEngineIsSharedAndTheLensFlowsBetweenTabs() {
        // Jos Stam stable fluids on the CPU, the heart of the design.
        assertTrue("the solver ships in the shared engine", engine.contains("class StableFluid"))
        assertTrue("the design is documented as Jos Stam stable fluids", engine.contains("Jos Stam"))
        // The two-edge spring: stretch toward the new tab, then the trailing edge snaps shut.
        assertTrue("spring stiffness", engine.contains("K_LENS"))
        assertTrue("spring damping", engine.contains("ZETA_LENS"))
        assertTrue("the trailing edge waits before snapping shut", engine.contains("FLOW_DELAY_MS"))
        // The dye is clipped to a pill and the flow pushes dye along the direction of travel.
        assertTrue("lens is a pill clip", engine.contains("clipPath(pill)"))
        assertTrue("flow pushes dye", engine.contains("val push = if (dirRight) 8f else -8f"))
        assertTrue("the capsule is seeded full of dye", engine.contains("fun seed()"))
        assertTrue("the dye stays alive with ambient splats", engine.contains("fun ambientSplat("))
    }

    @Test
    fun theFluidIsPrimarilyBurgundyAndTheSimLives() {
        // The primary dye — the design's #660033 — lives in the shared engine.
        assertTrue("the shared burgundy dye ships with the engine", engine.contains("val BurgundyFluidDye"))
        assertTrue("primary #660033", engine.contains("0xFF660033"))
        assertTrue("deep companion #2E0019", engine.contains("0xFF2E0019"))
        // The sim lives continuously on devices with the design's 380 ms fill pulses.
        assertTrue("the dye keeps alive with ambient splats", engine.contains("fun ambientSplat("))
        assertTrue("live on devices, settled under the harness", engine.contains("fluidRunsLive"))
        // The design's flow: Jos Stam stable fluids on the CPU — cone splats, an 18-iteration
        // Jacobi pressure solve behind the divergence-free projection, and a deep-burgundy to
        // #660033 render with white only at the rim, exactly as the design codes.
        assertTrue("cone splats", engine.contains("1f - d2.toFloat() / r2"))
        assertTrue("pressure-projected divergence-free field", engine.contains("private fun project()"))
        assertTrue("jacobi pressure solve", engine.contains("linSolve(p, div, 1f, 4f, 18)"))
        assertTrue("deep-burgundy to #660033 with white only at the rim",
            engine.contains("smoothstep(0.03f, 0.55f, t)"))
        for ((name, bar) in listOf("default" to defaultBar, "spatial" to spatialBar, "aether" to aetherBar)) {
            assertTrue("$name runs the shared fluid lens", bar.contains("rememberFluidNavLensHost("))
            assertTrue("$name draws the fluid layer", bar.contains("FluidNavLensLayer("))
            assertTrue("$name lays its cells on the fluid", bar.contains("FluidNavItemsRow("))
            assertTrue("$name wears the primarily-#660033 dye", bar.contains("BurgundyFluidDye"))
        }
        // Each UI stays unique through its chrome, not its dye.
        assertTrue("default keeps the wine surface capsule", defaultBar.contains("RedSurface("))
        assertTrue("aether keeps the specular rim glass", aetherBar.contains("specularRimBorder("))
        assertTrue("spatial keeps the rim-lit spatial glass", spatialBar.contains("glassElevatedFill"))
    }

    @Test
    fun everyDockIsTheFluidCapsuleAndEverySettingsButtonIsACircle() {
        assertTrue("one shared width for all three skins", defaultBar.contains("const val BOTTOM_NAV_WIDTH_FRACTION = 0.72f"))
        assertTrue("the capsule is the design's 64 dp", defaultBar.contains("val BOTTOM_NAV_HEIGHT: Dp = 64.dp"))
        assertTrue("the cluster gap is the design's 10 dp", defaultBar.contains("val BOTTOM_NAV_GAP: Dp = 10.dp"))
        assertTrue("the settings circle is the design's 56 dp", defaultBar.contains("val BOTTOM_SETTINGS_SIZE: Dp = 56.dp"))
        for ((name, bar) in listOf("default" to defaultBar, "spatial" to spatialBar, "aether" to aetherBar)) {
            assertTrue("$name dock wears the true capsule", bar.contains("RedShapes.capsule"))
            assertTrue("$name settings button is a true circle", bar.contains("CircleShape"))
            assertTrue("$name circle is addressable", bar.contains("testTag(\"bottom_settings_button\")"))
            assertTrue("$name uses the shared capsule height", bar.contains("BOTTOM_NAV_HEIGHT"))
            assertFalse("$name dock does not span the screen by itself", bar.contains(".navigationBarsPadding()"))
            for (tag in listOf("nav_item_home", "nav_item_arsky", "nav_item_satellites", "nav_item_lab")) {
                assertTrue("$name addresses $tag", bar.contains("\"$tag\""))
            }
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
        }
        assertTrue(activity.contains("AetherSettingsBubble(onClick = openSettings)"))
        assertTrue(activity.contains("SpatialSettingsBubble(onClick = openSettings)"))
        assertTrue(activity.contains("FloatingSettingsBubble(onClick = openSettings)"))
        // One condition for both: the circle is drawn inside the dock's own visibility block.
        val block = activity.substringAfter("if (!com.zig.gravity.ui.ImmersiveScreenState.active")
        assertTrue(block.substringBefore("// Dialogs and Modals").contains("FloatingSettingsBubble("))
        assertTrue("settings opens the same dialog", activity.contains("setShowSettingsDialog(true)"))

        // The Home headers no longer own a settings button: it lives in the chrome.
        for (home in listOf(
            "com/alijafari/red/astronomy/ui/screens/HomeScreen.kt",
            "com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt",
            "com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt",
        )) {
            assertFalse("$home still has the old top settings button", source(home).contains("top_settings_button"))
        }
    }

    @Test
    fun theHomeSkyExpandButtonIsACircleLikeTheSearchButton() {
        for ((name, home) in listOf(
            "default" to source("com/alijafari/red/astronomy/ui/screens/HomeScreen.kt"),
            "spatial" to source("com/alijafari/red/astronomy/ui/spatial/SpatialHomeScreen.kt"),
            "aether" to source("com/alijafari/red/astronomy/ui/aether/AetherHomeScreen.kt"),
        )) {
            assertTrue("$name: search keeps its tag", home.contains("top_search_button"))
            assertTrue("$name: the expand button keeps its tag", home.contains("home_sky_expand_button"))
            assertTrue("$name: the expand button is the FitScreen glyph", home.contains("Icons.Outlined.FitScreen"))
            assertFalse("$name: no capsule glyph left", home.contains("Icons.Rounded.Fullscreen") || home.contains("Icons.Outlined.OpenInFull"))
            // Around the tag the button must be a circle: the circle surface on default/spatial,
            // the same AetherOpticalIconLens circle as the search button on aether.
            val around = home.substring(maxOf(0, home.indexOf("home_sky_expand_button") - 700), home.indexOf("home_sky_expand_button"))
            val circle = if (name == "aether") "AetherOpticalIconLens(" else "CircleShape"
            assertTrue("$name: the expand button is a $circle circle", around.contains(circle))
        }
    }

    @Test
    fun theHudClearsTheDockAndThePreviewsShowTheSameChrome() {
        val stage = source("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        assertTrue("the cards clear the 64 dp fluid capsule", stage.contains("bottomInset + 76.dp"))
        val previews = file("src/debug", "com/alijafari/red/astronomy/ui/satellites/SatellitePreviews.kt")
        assertTrue(previews.contains("BOTTOM_NAV_WIDTH_FRACTION"))
        assertTrue(previews.contains("BOTTOM_NAV_GAP"))
        assertTrue(previews.contains("FloatingSettingsBubble("))
        assertTrue(previews.contains("AetherSettingsBubble("))
        assertTrue(previews.contains("SpatialSettingsBubble("))
    }
}
