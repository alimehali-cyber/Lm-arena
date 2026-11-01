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
 * screens are Compose over a GL landscape, and the glass is a shader — so the shape of the code is the
 * guard, alongside the arithmetic in [Part6GlassAndInkTest].
 *
 * 1. "What's Up Tonight" has no plate and adapts its ink (see [SkyUiPolishTest] as well);
 * 2. the eclipse card's text sits 8 mm further out;
 * 3. the fire is at the same place on every screen and 20 % brighter;
 * 4. the Moon render is 2.5D;
 * 5. the Moon card's text sits 1.5 cm further out;
 * 6. Home / Lab / Satellite cards get the real glass and the real backdrop;
 * 7. the glass has colour mixing, shader design and smooth-edge controls;
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
    private val glass by lazy { source("com/alijafari/red/astronomy/ui/theme/LiquidGlass.kt") }
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

        val chal = source("com/zig/chal/ui/ChalRoot.kt")
        assertTrue("Chal's header survives", chal.contains("private fun ChalHeader("))
        val chalHeader = chal.substring(
            chal.indexOf("private fun ChalHeader("),
            chal.indexOf("private fun ChalChromeButton(")
        )
        assertFalse("without a back button", chalHeader.contains("ArrowBack"))
        assertFalse("and without the CHAL title", chalHeader.contains("\"CHAL\""))

        val gargantua = source("com/zig/gargantua/ui/GargantuaRoot.kt")
        assertFalse("Gargantua's top bar is gone", gargantua.contains("GargantuaTopBar"))
        assertFalse("and its back button with it", gargantua.contains("gargantua_back_button"))
        assertTrue("the camera reset stays, as a control", gargantua.contains("gargantua_reset_camera_button"))
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
    // Item 6 — the cards get real glass over the real backdrop
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theCardsSampleTheAppBackdropInsteadOfNullingIt() {
        assertFalse(
            "nothing nulls the backdrop for the content tree any more",
            activity.contains("LocalLiquidGlassBackdrop provides null")
        )
        assertTrue(
            "the content tree is handed the recorded app backdrop",
            activity.contains("LocalLiquidGlassBackdrop provides glassBackdrop")
        )
        assertTrue(
            "which is its own layer over the backdrop only",
            activity.contains("Modifier.layerBackdrop(glassBackdrop)")
        )
        // Two layers: one the overlays sample (content), one the cards sample (backdrop). Sampling the
        // content layer from inside it is the Kyant assertion, so the split is the fix, not a refactor.
        assertTrue(
            "two layers, two jobs",
            activity.split("rememberLayerBackdrop()").size - 1 >= 2
        )
        assertTrue(
            "the backdrop layer includes the theme's own surface",
            activity.contains(".background(com.alijafari.red.astronomy.ui.theme.RedTheme.colors.background)")
        )
    }

    @Test
    fun theCardsAreStillGlassSurfaces() {
        // The plumbing only matters if the cards still ask for glass; a fallback-only card would render
        // its own surface no matter what the backdrop does.
        val lab = source("com/alijafari/red/astronomy/ui/screens/LabScreen.kt")
        assertTrue("Lab's tool cards are glass", lab.contains("LiquidGlassSurface("))
        val iss = source("com/alijafari/red/astronomy/ui/screens/ISSScreen.kt")
        assertTrue("the satellite cards are glass", iss.contains("LiquidGlassSurface("))
        assertTrue("Home's cards are glass", home.contains("private fun MoonCard("))
        assertTrue("including the eclipse card", home.contains("private fun EclipseCard("))
    }

    // ---------------------------------------------------------------------------------------------
    // Item 7 — the controls exist, are wired, and persist
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theGlassRecipeControlsExistInSettings() {
        val tags = listOf(
            "liquid_glass_hue_a_slider",
            "liquid_glass_hue_b_slider",
            "liquid_glass_mix_slider",
            "liquid_glass_saturation_slider",
            "liquid_glass_sheen_strength_slider",
            "liquid_glass_edge_smoothing_slider",
            "liquid_glass_swatch"
        )
        tags.forEach { tag ->
            assertTrue("Settings exposes $tag", settings.contains(tag))
        }
        // The chips are generated from the enums, so their tags are templates over every entry — one
        // chip per profile, per sheen and per named recipe, with no option that can be added to the enum
        // and forgotten in the dialog.
        assertTrue(
            "one chip per shader profile",
            settings.contains("GlassShaderProfile.entries.forEach { profile ->") &&
                settings.contains("liquid_glass_profile_\${profile.name.lowercase()}")
        )
        assertTrue(
            "one chip per sheen",
            settings.contains("GlassSheen.entries.forEach { sheen ->") &&
                settings.contains("liquid_glass_sheen_\${sheen.name.lowercase()}")
        )
        assertTrue(
            "one chip per named film",
            settings.contains("liquid_glass_preset_\${preset.name.lowercase()}")
        )
        assertTrue("the swatch is the live film, not a lookalike", settings.contains("val film = glassFilmColor(glassConfig)"))
        assertTrue("every control writes the config", settings.contains("viewModel.updateLiquidGlassConfig(glassConfig.copy("))
    }

    @Test
    fun everyGlassSettingIsPersistedAndRestored() {
        listOf(
            "liquid_glass_tint_hue_a",
            "liquid_glass_tint_hue_b",
            "liquid_glass_tint_mix",
            "liquid_glass_tint_saturation",
            "liquid_glass_shader_profile",
            "liquid_glass_sheen",
            "liquid_glass_sheen_strength",
            "liquid_glass_edge_smoothing"
        ).forEach { key ->
            assertTrue("$key is written", viewModel.contains(".putFloat(\"$key\"") ||
                viewModel.contains(".putString(\"$key\""))
            assertTrue("$key is read back", viewModel.contains("prefs.getFloat(\"$key\"") ||
                viewModel.contains("prefs.getString(\"$key\""))
        }
        assertTrue("a preset writes the same numbers the sliders do", viewModel.contains("fun applyGlassFilmPreset("))
    }

    @Test
    fun theGlassModifierUsesEveryOneOfThem() {
        assertTrue("the film is the mixed colour", glass.contains("tintColor = glassFilmColor(config)"))
        assertTrue("the shader profile scales the blur", glass.contains("* profile.blurScale"))
        assertTrue("and the refraction", glass.contains("* profile.refractionScale"))
        assertTrue("the sheen is drawn under the content", glass.contains("val sheenModifier = if (config.sheen != GlassSheen.NONE"))
        assertTrue("the corner dial reaches the shape", glass.contains("shape.withCornerSmoothing(config.edgeSmoothing)"))
        assertTrue("and the outline is the one clipped and refracts", glass.contains("clip(effectiveShape)"))
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
