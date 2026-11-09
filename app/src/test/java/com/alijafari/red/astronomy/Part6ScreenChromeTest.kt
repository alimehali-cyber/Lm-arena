package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Part 6's source-level guards.
 *
 * Every item in this round is either chrome that has to be *absent*, or a piece of plumbing that has to
 * be *present* in the one place that makes it work. Neither can be asserted from a screenshot — the
 * screens are Compose over a GL landscape — so the shape of the code is the guard, alongside the
 * arithmetic in [Part6InkAndSurfaceTest].
 *
 * 1. "What's Up Tonight" has no plate and adapts its ink (see [SkyUiPolishTest] as well);
 * 2. the eclipse card's text sits 8 mm further out;
 * 3. the fire is at the same place on every screen and 20 % brighter;
 * 4. the Moon render is 2.5D;
 * 5. the Moon card's text sits 1.5 cm further out;
 * 6. Home / Lab / Satellite cards are drawn by the one flat surface, over nothing;
 * 7. the glass material, its settings and its theme are gone;
 * 8. no top band, no "Lab"/"Satellites" title and no back button on those screens or their sub-screens.
 */
class Part6ScreenChromeTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private val activity by lazy { source("com/alijafari/red/astronomy/MainActivity.kt") }
    private val home by lazy { source("com/alijafari/red/astronomy/ui/screens/HomeScreen.kt") }
    private val moonRender by lazy { source("com/alijafari/red/astronomy/ui/components/MoonPhaseRender.kt") }
    private val settings by lazy { source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt") }
    private val viewModel by lazy { source("com/alijafari/red/astronomy/ui/MainViewModel.kt") }
    private val lab by lazy { source("com/alijafari/red/astronomy/ui/screens/LabScreen.kt") }
    private val iss by lazy { source("com/alijafari/red/astronomy/ui/screens/ISSScreen.kt") }
    private val surface by lazy { source("com/alijafari/red/astronomy/ui/theme/Surface.kt") }
    private val buildScript by lazy {
        listOf(File("build.gradle.kts"), File("app/build.gradle.kts"))
            .firstOrNull { it.exists() }?.readText().orEmpty()
    }
    private val scene by lazy { source("com/alijafari/red/astronomy/ui/persepolis/PersepolisMilAzhdaha.kt") }
    private val framing by lazy { source("com/alijafari/red/astronomy/ui/rendering/PersepolisFraming.kt") }

    // ---------------------------------------------------------------------------------------------
    // Item 8 — no band, no title, no back button
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theAppHasNoSharedTopBand() {
        assertFalse("the scaffold's top bar is gone", activity.contains("topBar = {"))
        assertFalse("and its back button with it", activity.contains("tab_top_back_button"))
        assertFalse("no page title is rendered for a tab", activity.contains("val pageTitle = when (uiState.selectedTab)"))
        assertTrue("the scaffold is still the root", activity.contains(".testTag(\"main_scaffold\")"))
    }

    @Test
    fun everyLabAndSatelliteScreenInsetsItsOwnStatusBar() {
        // The band used to provide this inset. Each screen now carries it, once.
        val lab = source("com/alijafari/red/astronomy/ui/screens/LabScreen.kt")
        assertTrue("the Lab list starts below the clock", lab.contains(".statusBarsPadding()"))

        val iss = source("com/alijafari/red/astronomy/ui/screens/ISSScreen.kt")
        assertTrue("so does the satellite index", iss.contains(".statusBarsPadding()"))
    }

    @Test
    fun theLabSubScreensHaveNoHeaderOfTheirOwn() {
        val timeDilation = source("com/alijafari/red/astronomy/ui/screens/TimeDilationCalculatorScreen.kt")
        assertFalse("no back chip", timeDilation.contains("time_dilation_back_button"))
        assertFalse("no title band", timeDilation.contains("topBar = {"))
        assertTrue("its own status-bar inset instead", timeDilation.contains(".statusBarsPadding()"))
        assertTrue("and its explanation still reachable", timeDilation.contains("showHowItWorksDialog = true"))

        val gravity = source("com/zig/gravity/ui/GravitySandboxRoot.kt")
        assertFalse("the sandbox has no back chip", gravity.contains("sandbox_back"))
        assertFalse("and no title row", gravity.contains("\"میز گرانش\" else \"Gravity table\""))
    }

    @Test
    fun theSatelliteDetailScreenHasNoHeaderEither() {
        val detail = source("com/alijafari/red/astronomy/ui/screens/SatelliteDetailScreen.kt")
        assertFalse("the back chip is gone", detail.contains("sat_detail_back"))
        assertFalse("and the band with it", detail.contains("topBar = {"))
        assertTrue("its own status-bar inset instead", detail.contains(".statusBarsPadding()"))
        assertTrue("the offline state is still reported", detail.contains("WifiOff"))
    }

    // ---------------------------------------------------------------------------------------------
    // Item 6 — the cards are drawn by the one flat surface, and nothing samples what is behind them
    // ---------------------------------------------------------------------------------------------

    @Test
    fun everyCardIsDrawnByTheOneSurface() {
        assertTrue("the surface exists", surface.contains("fun RedSurface("))
        assertTrue("it paints a fill", surface.contains(".background(color, shape)"))
        assertTrue("a border", surface.contains("Modifier.border(border, shape)"))
        assertTrue("and a clip", surface.contains(".clip(shape)"))
        assertTrue("it is tappable", surface.contains("Modifier.clickable("))
        assertTrue(
            "it hands the ink it resolved to everything inside it",
            surface.contains("LocalRedColors provides surfaceColors")
        )
        assertTrue("Lab's tool cards use it", lab.contains("RedSurface("))
        assertTrue("the satellite cards use it", iss.contains("RedSurface("))
        assertTrue("Home's cards use it", home.contains("private fun MoonCard("))
        assertTrue("including the eclipse card", home.contains("private fun EclipseCard("))
        assertTrue(
            "the small sky buttons are filled, bordered and clipped without a material",
            home.contains(".solidSurface(")
        )
    }

    @Test
    fun nothingSamplesWhatIsBehindIt() {
        // The whole point of the flat surface: a card's fill and its ink are known before it is painted,
        // so nothing in the app reads the pixels underneath it — and no dependency exists to let it.
        listOf(
            "com/alijafari/red/astronomy/MainActivity.kt" to activity,
            "com/alijafari/red/astronomy/ui/screens/HomeScreen.kt" to home,
            "com/alijafari/red/astronomy/ui/screens/LabScreen.kt" to lab,
            "com/alijafari/red/astronomy/ui/screens/ISSScreen.kt" to iss,
            "com/alijafari/red/astronomy/ui/components/SettingsDialog.kt" to settings
        ).forEach { (name, text) ->
            assertFalse("$name samples no backdrop", text.contains("layerBackdrop"))
            assertFalse("$name asks no material for glass", text.contains("LiquidGlassSurface("))
            assertFalse("$name has no optical modifier", text.contains(".liquidGlass("))
        }
        val themeDir = File("src/main/java/com/alijafari/red/astronomy/ui/theme")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/alijafari/red/astronomy/ui/theme")
        assertFalse("the glass material is deleted", File(themeDir, "LiquidGlass.kt").exists())
        assertFalse("and the corner dial it drove", File(themeDir, "CornerSmoothness.kt").exists())
        themeDir.listFiles().orEmpty().filter { it.isFile }.forEach { file ->
            val text = file.readText()
            assertFalse("${file.name} imports no glass library", text.contains("com.kyant"))
            assertFalse("${file.name} names no glass material", text.contains("LiquidGlassConfig"))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Item 7 — the glass settings and the glass theme are gone
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theGlassControlsAreGoneFromSettings() {
        assertFalse("no glass section", settings.contains("settings_section_liquid_glass"))
        assertFalse("no glass switch", settings.contains("settings_liquid_glass_switch"))
        assertFalse("no dial tags", settings.contains("liquid_glass_"))
        assertFalse("no shader presets", settings.contains("GlassShaderProfile"))
        assertFalse("no sheen", settings.contains("GlassSheen"))
        assertFalse("no film presets", settings.contains("GlassFilmPreset"))
        assertFalse("nothing left to expand", settings.contains("isLiquidGlassExpanded"))
        assertTrue("the app backdrop section is still there", settings.contains("settings_section_app_backdrop"))
        assertTrue("and the theme picker still picks", settings.contains("onClick = { viewModel.setThemeMode("))
    }

    @Test
    fun theGlassThemeIsGoneEverywhere() {
        assertFalse("no theme value for it", viewModel.contains("ThemeMode.GLASS"))
        assertFalse("no setting for it", viewModel.contains("liquid_glass_"))
        assertFalse("no glass state to hold", viewModel.contains("LiquidGlassConfig"))
        listOf(
            "com/alijafari/red/astronomy/ui/components/SettingsDialog.kt" to settings,
            "com/alijafari/red/astronomy/MainActivity.kt" to activity
        ).forEach { (name, text) ->
            assertFalse("$name offers no iOS 27 theme", text.contains("ThemeMode.GLASS"))
            assertFalse("$name names no iOS 27 theme", text.contains("iOS 27"))
        }
        assertFalse("and no dependency remains", buildScript.contains("libs.backdrop"))
    }

    // ---------------------------------------------------------------------------------------------
    // Items 2 and 5 — the two card text shifts
    // ---------------------------------------------------------------------------------------------

    @Test
    fun bothHeroCardTextBlocksAreShiftedByTheAskedAmounts() {
        assertTrue("the eclipse details are offset", home.contains(".offset(x = HomeTextShift.eclipseCard)"))
        assertTrue("the Moon readout is offset", home.contains(".offset(x = HomeTextShift.moonCard)"))
        assertTrue("the eclipse shift is 8 mm", home.contains("val eclipseCard: Dp = 50.4.dp"))
        assertTrue("the Moon shift is 1.5 cm", home.contains("val moonCard: Dp = 94.5.dp"))
    }

    // ---------------------------------------------------------------------------------------------
    // Item 3 — the fire's place and its brightness
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theFramingNoLongerDependsOnTheScreensAspectRatio() {
        assertTrue(
            "the window is the fixed reference, not an aspect-grown one",
            framing.contains("val fixedVisibleWidth = DEFAULT_VISIBLE_WIDTH_M / LINEAR_SCALE_FRACTION")
        )
        assertFalse(
            "the aspect term is gone",
            framing.contains("aspect * BUILDING_HEIGHT_M / REFERENCE_MAX_BUILDING_HEIGHT_FRACTION")
        )
        assertTrue(
            "the only widening left is the one the readout guard needs",
            framing.contains("for (attempt in 0 until 3) {")
        )
    }

    @Test
    fun theFireIsTwentyPercentBrighter() {
        assertTrue("one gain, named", scene.contains("private const val FIRE_BRIGHTNESS_GAIN = 1.20f"))
        assertTrue(
            "the light it throws takes it",
            scene.contains("2.25f * FIRE_BRIGHTNESS_GAIN")
        )
        assertTrue(
            "so does the halo",
            scene.contains("1.00f * FIRE_BRIGHTNESS_GAIN")
        )
        assertTrue("and the flame's own emission", scene.contains("col *= 1.20;"))
        assertEquals(
            "the reach is unchanged: brighter, not wider",
            1,
            Regex("FIRE_LIGHT_RANGE_M = 3\\.20f").findAll(scene).count()
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Item 4 — the Moon is 2.5D
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theMoonIsShadedAsASphereRatherThanTintedFlat() {
        assertTrue("the shading is on the Moon and nothing else", moonRender.contains("BlendMode.SrcAtop"))
        assertTrue("a sub-solar highlight", moonRender.contains("val subSolar = Offset("))
        assertTrue(
            "a light that falls off toward the limb",
            moonRender.contains("1.00f to Color.Black.copy(alpha = 0.34f)")
        )
        assertTrue("earthshine with a cold cast", moonRender.contains("MoonEarthshineTint"))
        assertTrue("a limb that darkens before the edge", moonRender.contains("0.93f to MoonNightColor.copy(alpha = 0.26f)"))
        assertTrue("a specular catch on the sunward rim", moonRender.contains("MoonSpecularAlpha"))
        // Libration is a Double from the engine and the layer's translation is a Float, so the resolved
        // fraction is converted where it is computed — asserting both halves keeps the two from drifting
        // apart again (CI 41299691462 caught exactly that).
        assertTrue(
            "a libration parallax",
            moonRender.contains("val parallaxX =") &&
                moonRender.contains("moonData.librationLonDeg / 8.0")
        )
        assertTrue("resolved in Float", moonRender.contains("MoonParallaxMaxFraction).toFloat()"))
        // The disc contract other tests and the AR overlay depend on is untouched.
        assertTrue("the plate zoom is unchanged", moonRender.contains("1.03f / MoonPlateDiscFraction"))
        assertTrue("so is the phase cut-out", moonRender.contains("if (ill < 0.98) {"))
    }
}
