package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bottom chrome is one design on every skin: a capsule whose selected button is a jet-black
 * deep-space sky — a real nebula photograph with blue ribbons, violet clouds, star sprinkles and
 * a live twinkle, springing in on selection — no simulation, no fluid — plus one circular settings
 * button, close together in one centred cluster at the bottom of the screen. The object information
 * page shows the live observatory score as a dot-matrix readout, in Persian digits with ٪ in Persian mode.
 */
class NavChromeTest {

    private fun file(sourceSet: String, relative: String): String {
        val candidates = listOf(File("$sourceSet/java/$relative"), File("app/$sourceSet/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText() ?: error("Source not found: $relative")
    }

    private fun source(relative: String): String = file("src/main", relative)

    private val spaceSkyPill by lazy { source("com/alijafari/red/astronomy/ui/components/SpaceSkyNavTabPill.kt") }
    private val dotMatrix by lazy { source("com/alijafari/red/astronomy/ui/components/DotMatrixText.kt") }
    private val objectDetailModal by lazy { source("com/alijafari/red/astronomy/ui/components/ObjectDetailModal.kt") }
    private val defaultBar by lazy { source("com/alijafari/red/astronomy/ui/components/FloatingNavBar.kt") }
    private val spatialBar by lazy { source("com/alijafari/red/astronomy/ui/spatial/SpatialFloatingNavBar.kt") }
    private val aetherBar by lazy { source("com/alijafari/red/astronomy/ui/aether/AetherFloatingNavBar.kt") }
    private val activity by lazy { source("com/alijafari/red/astronomy/MainActivity.kt") }

    @Test
    fun theSelectedTabWearsJetBlackSpaceSky() {
        // The jet-black sky: a real nebula photograph under the capsule, cover-cropped to fit —
        // blue ribbons of light, violet clouds and star sprinkles, edges falling to black.
        assertTrue("the nebula photograph ships with the app", File("src/main/res/drawable-nodpi/tab_nebula.jpg").isFile || File("app/src/main/res/drawable-nodpi/tab_nebula.jpg").isFile)
        assertTrue("the capsule draws the nebula photograph", spaceSkyPill.contains("ImageBitmap.imageResource(R.drawable.tab_nebula)"))
        assertTrue("the nebula is cover-cropped to the capsule", spaceSkyPill.contains("srcOffset"))
        assertTrue("the sky is drawn inside the capsule", spaceSkyPill.contains("clipPath(pill)"))
        assertTrue("the rim falls to jet black at the edges", spaceSkyPill.contains("Brush.radialGradient("))
        assertTrue("a single blade of incident light", spaceSkyPill.contains("Brush.linearGradient("))
        assertTrue("a faint lacquer along the top edge", spaceSkyPill.contains("Brush.verticalGradient("))
        // Star sprinkles + a live twinkle, deterministic from a fixed seed.
        assertTrue("star sprinkle field", spaceSkyPill.contains("StarSprinkles"))
        assertTrue("bright twinkling stars", spaceSkyPill.contains("TwinklingStars"))
        assertTrue("the twinkle breathes", spaceSkyPill.contains("rememberInfiniteTransition"))
        assertTrue("the twinkle loops", spaceSkyPill.contains("infiniteRepeatable("))
        // The spring animation: the capsule springs in on selection, the icon pops with it.
        assertTrue("spring-reveal on selection", spaceSkyPill.contains("spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)"))
        assertTrue("the icon keeps its bouncy pop", spaceSkyPill.contains("spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)"))
        assertTrue("selection reveals with a spring", spaceSkyPill.contains("SpaceReveal"))
        // The burgundy is gone from the selected button for good.
        assertFalse("no burgundy body remains", spaceSkyPill.contains("Burgundy"))
        assertFalse("no grain tooth remains", spaceSkyPill.contains("GrainSpeckles"))
    }

    @Test
    fun noFluidAnimationRemainsAnywhereInTheChrome() {
        for ((name, bar) in listOf("default" to defaultBar, "spatial" to spatialBar, "aether" to aetherBar)) {
            assertTrue("$name runs the shared space-sky tab row", bar.contains("SpaceSkyNavTabItemsRow("))
            assertTrue("$name addresses cells through NavTabSpec", bar.contains("NavTabSpec("))
            assertFalse("$name has no simulation host", bar.contains("rememberFluidNavLensHost"))
            assertFalse("$name has no fluid canvas layer", bar.contains("FluidNavLensLayer("))
        }
        // The fluid engine itself is gone — the capsule is pure composition now.
        assertFalse("no solver ships in the shared component", spaceSkyPill.contains("StableFluid"))
        assertFalse("no simulation loop remains", spaceSkyPill.contains("fluidRunsLive"))
        assertFalse("no per-frame frame loop remains", spaceSkyPill.contains("withFrameNanos"))
        // Every skin keeps its own chrome; the selected tab is the shared space-sky on all of them.
        assertTrue("default keeps the wine surface capsule", defaultBar.contains("RedSurface("))
        assertTrue("aether keeps the specular rim glass", aetherBar.contains("specularRimBorder("))
        assertTrue("spatial keeps the rim-lit spatial glass", spatialBar.contains("glassElevatedFill"))
    }

    @Test
    fun everyDockIsTheCapsuleAndEverySettingsButtonIsACircle() {
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
    fun theLiveScoreIsAPersianDotMatrixReadout() {
        // The score renders as a dot-matrix readout, not a raw English percentage.
        assertTrue("the modal renders the score through the dot-matrix readout", objectDetailModal.contains("DotMatrixScore("))
        assertFalse("the raw English percentage is gone", objectDetailModal.contains("\${obs.scorePercent}%"))
        // The glyphs ship as a 5x7 matrix — Persian digits with ٪, Latin digits with %.
        assertTrue("the dot-matrix glyph table ships with the component", dotMatrix.contains("DotMatrixGlyphs"))
        assertTrue("Persian digits are set in the matrix", dotMatrix.contains("'۰' to intArrayOf("))
        assertTrue("the Persian percent glyph is set in the matrix", dotMatrix.contains("'٪' to intArrayOf("))
        assertTrue("Latin digits are set in the matrix", dotMatrix.contains("'0' to intArrayOf("))
        assertTrue("Latin percent glyph is set in the matrix", dotMatrix.contains("'%' to intArrayOf("))
        assertTrue("Persian mode renders Persian digits", dotMatrix.contains("formatPersianNumbers(scorePercent.toString())"))
        assertTrue("Persian mode appends the Persian percent sign", dotMatrix.contains("+ \"٪\""))
        assertTrue("English mode renders Latin digits with %", dotMatrix.contains("\"\$scorePercent%\""))
        assertTrue("the Persian score reads itself aloud in Persian", dotMatrix.contains("امتیاز رصدپذیری"))
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
        assertTrue("the cards clear the 64 dp capsule", stage.contains("bottomInset + 76.dp"))
        val previews = file("src/debug", "com/alijafari/red/astronomy/ui/satellites/SatellitePreviews.kt")
        assertTrue(previews.contains("BOTTOM_NAV_WIDTH_FRACTION"))
        assertTrue(previews.contains("BOTTOM_NAV_GAP"))
        assertTrue(previews.contains("FloatingSettingsBubble("))
        assertTrue(previews.contains("AetherSettingsBubble("))
        assertTrue(previews.contains("SpatialSettingsBubble("))
    }
}
