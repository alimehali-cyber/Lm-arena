package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.obsidian.DarkObsidianPalette
import com.alijafari.red.astronomy.ui.obsidian.LightObsidianPalette
import com.alijafari.red.astronomy.ui.obsidian.OBSIDIAN_SPRING_DAMPING
import com.alijafari.red.astronomy.ui.obsidian.OBSIDIAN_SPRING_STIFFNESS
import com.alijafari.red.astronomy.ui.obsidian.ObsidianAlabaster
import com.alijafari.red.astronomy.ui.obsidian.ObsidianAmber
import com.alijafari.red.astronomy.ui.obsidian.ObsidianCobalt
import com.alijafari.red.astronomy.ui.obsidian.ObsidianCrimson
import com.alijafari.red.astronomy.ui.obsidian.ObsidianRose
import com.alijafari.red.astronomy.ui.obsidian.ObsidianSlateRim
import com.alijafari.red.astronomy.ui.obsidian.ObsidianSmokedQuartz
import com.alijafari.red.astronomy.ui.obsidian.WineObsidianPalette
import com.alijafari.red.astronomy.ui.obsidian.obsidianPaletteFor
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Obsidian Quartz & Crimson Telemetry design language — the fourth entry in the UI selector.
 *
 * These are source-reading tests in the same spirit as `SpatialUiTest`: they pin the *rules* the
 * language is not allowed to break (procedural dot-matrix type, one palette, optical quartz
 * surfaces instead of flat borders, critically damped motion, no Material icons or emoji), because
 * a rendering regression in a Compose Canvas is invisible to a JVM unit test otherwise.
 */
class ObsidianUiTest {

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

    private fun obsidianFile(name: String): String =
        source("com/alijafari/red/astronomy/ui/obsidian/$name")

    // ============================== the selector entry ==========================================

    @Test
    fun obsidianIsASelectableUiStyleAndIsTheFourthOne() {
        assertEquals(
            listOf(AppUiStyle.DEFAULT, AppUiStyle.SPATIAL, AppUiStyle.CYBER, AppUiStyle.OBSIDIAN),
            AppUiStyle.entries
        )
    }

    @Test
    fun everySettingsMenuOffersTheObsidianOption() {
        val menus = listOf(
            source("com/alijafari/red/astronomy/ui/components/SettingsDialog.kt"),
            source("com/alijafari/red/astronomy/ui/spatial/SpatialSettingsModal.kt"),
            source("com/alijafari/red/astronomy/ui/cyber/CyberSettingsModal.kt"),
            source("com/alijafari/red/astronomy/ui/obsidian/ObsidianSettingsModal.kt")
        )
        menus.forEach { src ->
            assertTrue("the selector draws an Obsidian row", src.contains("settings_ui_style_obsidian"))
            assertTrue("and selects it", src.contains("viewModel.setUiStyle(AppUiStyle.OBSIDIAN)"))
            assertTrue("and can name it", src.contains("R.string.ui_style_obsidian"))
        }
    }

    @Test
    fun bothLocalesNameTheNewStyle() {
        val en = resource("values/strings.xml")
        val fa = resource("values-fa/strings.xml")
        listOf(en, fa).forEach { strings ->
            assertTrue(strings.contains("<string name=\"ui_style_obsidian\">"))
            assertTrue(strings.contains("<string name=\"ui_style_obsidian_desc\">"))
        }
        assertTrue(en.contains("Obsidian Quartz"))
    }

    @Test
    fun theStyleIsPersistedAndRoutedLikeEveryOtherStyle() {
        val viewModel = source("com/alijafari/red/astronomy/ui/MainViewModel.kt")
        assertTrue("one preference key", viewModel.contains("app_ui_style"))
        assertTrue("one setter", viewModel.contains("fun setUiStyle("))

        val activity = source("com/alijafari/red/astronomy/MainActivity.kt")
        assertTrue(activity.contains("isObsidianUi"))
        assertTrue(activity.contains("ObsidianThemeProvider("))
        assertTrue(activity.contains("ObsidianHomeScreen("))
        assertTrue(activity.contains("ObsidianLabScreen("))
        assertTrue(activity.contains("ObsidianSatellitesScreen("))
        assertTrue(activity.contains("ObsidianFloatingNavBar("))
        assertTrue(activity.contains("ObsidianSettingsModal("))
        assertTrue(activity.contains("EditorialSilhouetteBackdrop("))
    }

    @Test
    fun everyExhaustiveStyleBranchKnowsAboutObsidian() {
        // A `when (AppUiStyle)` that forgets the new entry stops compiling; these files are the ones
        // that branch on it explicitly.
        listOf(
            "com/alijafari/red/astronomy/MainActivity.kt",
            "com/alijafari/red/astronomy/ui/components/SettingsDialog.kt",
            "com/alijafari/red/astronomy/ui/cyber/CyberSettingsModal.kt",
            "com/alijafari/red/astronomy/ui/spatial/SpatialSettingsModal.kt",
            "com/alijafari/red/astronomy/ui/obsidian/ObsidianSettingsModal.kt",
            "com/alijafari/red/astronomy/ui/theme/ImmersiveChrome.kt",
            "com/zig/antar/ui/AntarControlPanel.kt"
        ).forEach { relative ->
            assertTrue(
                "$relative must handle AppUiStyle.OBSIDIAN",
                source(relative).contains("AppUiStyle.OBSIDIAN")
            )
        }
    }

    // ============================== the palette =================================================

    @Test
    fun thePaletteIsOneAtmosphericMoodNotAColorSalad() {
        // The five sanctioned ingredients, exact values from the art direction.
        assertEquals(Color(0xFFE8E4DE), ObsidianAlabaster)
        assertEquals(Color(0xFFB81450), ObsidianCrimson)
        assertEquals(Color(0xFFD95B8A), ObsidianRose)
        assertEquals(Color(0xFF6B85B8), ObsidianCobalt)
        assertEquals(Color(0xFFD97236), ObsidianAmber)

        // Every mode keeps the crimson identity and the Alabaster/smoked-quartz family.
        ThemeMode.entries.forEach { mode ->
            val palette = obsidianPaletteFor(mode)
            assertEquals(mode, palette.themeMode)
            assertTrue("crimson stays crimson", palette.accentCrimson.red > palette.accentCrimson.green)
            assertTrue("the canvas is never chroma-free grey", palette.canvasBg != palette.textPrimary)
        }
        assertTrue(LightObsidianPalette.canvasBg == ObsidianAlabaster)
        assertTrue(!LightObsidianPalette.isDark)
        assertTrue(DarkObsidianPalette.isDark)
        assertTrue(WineObsidianPalette.isWine)
        assertNotEquals(LightObsidianPalette.canvasBg, DarkObsidianPalette.canvasBg)
    }

    @Test
    fun theVelvetCrimsonLensRunsFromVoidToIlluminatedRim() {
        val home = obsidianFile("ObsidianHomeScreen.kt")
        // Exact gradient stops from the art direction: #170109 -> #4B0420 -> #8C093B -> #B81450.
        assertTrue(home.contains("Color(0xFF170109)"))
        assertTrue(home.contains("Color(0xFF4B0420)"))
        assertTrue(home.contains("Color(0xFF8C093B)"))
        assertTrue(home.contains("Color(0xFFB81450)"))
        // Smoked frosted quartz for the secondary card, declared once as a token.
        assertEquals(Color(0xFF1D242B), ObsidianSmokedQuartz)
        assertEquals(Color(0xFF5D7687), ObsidianSlateRim)
        assertTrue(home.contains("ObsidianSmokedQuartz"))
        assertTrue(home.contains("ObsidianSlateRim"))
    }

    // ============================== the surfaces ================================================

    @Test
    fun everyCardWearsTheOpticalQuartzSurface() {
        val design = obsidianFile("ObsidianDesignSystem.kt")
        assertTrue(
            "the recipe lives in one place",
            design.contains("fun Modifier.opticalQuartzSurface(")
        )
        assertTrue("an 18.dp inner-perimeter bloom", design.contains("strokeWidth = 18.dp.toPx()"))
        assertTrue("blurred, not a hairline", design.contains("BlurMaskFilter(16.dp.toPx()"))
        assertTrue("AGSL grain is API-gated", design.contains("Build.VERSION_CODES.TIRAMISU"))
        assertTrue("the shader is the frosted noise", design.contains("FROST_NOISE_AGSL"))
        assertTrue("a 0.85.dp specular rim", design.contains("Stroke(width = 0.85.dp.toPx())"))

        listOf("ObsidianHomeScreen.kt", "ObsidianSettingsModal.kt", "ObsidianLabScreen.kt").forEach { file ->
            val src = obsidianFile(file)
            assertTrue("$file uses the optic", src.contains("opticalQuartzSurface("))
            assertFalse(
                "$file must not fall back to a flat 1.dp white border",
                src.contains(".border(1.dp, Color.White")
            )
            assertFalse(
                "$file must not draw an outer double bezel",
                src.contains("BorderStroke(2.dp")
            )
        }
    }

    @Test
    fun capsulePillsKeepTheirSixDpInnerDottedTrack() {
        val nav = obsidianFile("ObsidianFloatingNavBar.kt")
        assertTrue(nav.contains("fun Modifier.obsidianInnerDottedTrack("))
        assertTrue("six dp inset", nav.contains("inset: Dp = 6.dp"))
        assertTrue("a dotted, not solid, track", nav.contains("PathEffect.dashPathEffect(floatArrayOf(3f, 7f)"))
        assertTrue("hairline", nav.contains("width = 0.8.dp.toPx()"))

        val home = obsidianFile("ObsidianHomeScreen.kt")
        assertTrue(
            "the refracting pills share the same track",
            home.contains("obsidianInnerDottedTrack(cornerRadius = 47.dp")
        )
    }

    // ============================== the type ====================================================

    @Test
    fun theDotMatrixEngineIsProceduralAndNotAFont() {
        val dot = obsidianFile("ObsidianDotMatrix.kt")
        assertTrue(dot.contains("fun DotMatrixNumber("))
        assertTrue("real 5x7 glyph tables", dot.contains("\"01110\", \"10001\", \"10001\", \"01110\""))

        // Every digit must exist, or a readout silently loses a character.
        ('0'..'9').forEach { digit ->
            assertTrue("glyph for $digit", dot.contains("'$digit' to listOf("))
        }
        assertTrue("decimal point present", dot.contains("'.' to listOf("))

        assertTrue("micro-dots are circles", dot.contains("drawCircle("))
        assertTrue("with optical bloom", dot.contains("BlurMaskFilter"))
        assertTrue("FA digits are mapped, not dropped", dot.contains("normalizeDotMatrixChar"))
    }

    @Test
    fun telemetryReadoutsUseTheDotMatrixInsteadOfMonospace() {
        listOf("ObsidianHomeScreen.kt", "ObsidianSettingsModal.kt", "ObsidianLabScreen.kt").forEach { file ->
            val src = obsidianFile(file)
            assertTrue("$file renders a dot-matrix readout", src.contains("DotMatrixNumber("))
            assertFalse(
                "$file must not substitute a monospace font",
                src.contains("FontFamily.Monospace")
            )
        }
    }

    // ============================== the anti-slop rules =========================================

    @Test
    fun noEmojisAndNoMaterialIconsAnywhereInTheLanguage() {
        val files = listOf(
            "ObsidianDesignSystem.kt",
            "ObsidianDotMatrix.kt",
            "ObsidianAmbientBackground.kt",
            "ObsidianFloatingNavBar.kt",
            "ObsidianHomeScreen.kt",
            "ObsidianLabScreen.kt",
            "ObsidianSatellitesScreen.kt",
            "ObsidianSettingsModal.kt"
        )
        // Emoji blocks only. Arrow glyphs are *required* by the micro-stepper rule, and
        // `Icon(` is a substring of `ObsidianVectorIcon(`, so both are checked precisely instead.
        val emoji = Regex("[\\u2600-\\u27BF\\u2B00-\\u2BFF]")
        val iconComposable = Regex("(?<![A-Za-z])Icon\\s*\\(")
        files.forEach { file ->
            val src = obsidianFile(file)
            assertFalse("$file must not import Material icons", src.contains("androidx.compose.material.icons"))
            assertFalse("$file must not draw a Material Icon(", iconComposable.containsMatchIn(src))
            assertFalse("$file must not use an emoji", emoji.containsMatchIn(src))
        }

        // The one place glyphs *are* allowed is the vector micro-icon canvas, drawn mathematically.
        val design = obsidianFile("ObsidianDesignSystem.kt")
        assertTrue(design.contains("fun ObsidianVectorIconGlyph("))
        assertTrue("hairline strokes only", design.contains("strokeWidth: Dp = 1.2.dp"))
    }

    @Test
    fun motionIsCriticallyDampedHardwareSprings() {
        assertEquals(0.84f, OBSIDIAN_SPRING_DAMPING)
        assertEquals(280f, OBSIDIAN_SPRING_STIFFNESS)

        val files = listOf(
            "ObsidianDesignSystem.kt",
            "ObsidianFloatingNavBar.kt",
            "ObsidianHomeScreen.kt",
            "ObsidianSettingsModal.kt"
        )
        files.forEach { file ->
            val src = obsidianFile(file)
            assertFalse(
                "$file must not use a bouncy cartoon spring",
                src.contains("DampingRatioHighBouncy") || src.contains("DampingRatioMediumBouncy")
            )
        }

        val home = obsidianFile("ObsidianHomeScreen.kt")
        assertTrue("the pulse waveform is damped", home.contains("dampingRatio = OBSIDIAN_SPRING_DAMPING"))
        assertTrue(
            "and haptic on every stepper click",
            home.contains("HapticFeedbackType.TextHandleMove")
        )
        assertTrue(
            "and the curve scrub is wired to haptics",
            home.contains("onScrubDay = { newIdx ->")
        )
    }

    // ============================== the components ==============================================

    @Test
    fun theSpecCompositionIsPresent() {
        val home = obsidianFile("ObsidianHomeScreen.kt")
        // Daily Intake hero, Flasks-per-day curve card, the 2x2 bento and the vector micro-stepper.
        assertTrue(home.contains("VelvetCrimsonLunarCard("))
        assertTrue(home.contains("SmokedQuartzAltitudeCard("))
        assertTrue(home.contains("SynthesizerPulseWaveform("))
        assertTrue(home.contains("VectorMicroStepper("))
        assertTrue(home.contains("GaussianTelemetryCurveCanvas("))
        assertTrue(home.contains("fun RefractingTelemetryPill("))
        assertTrue(home.contains("fun RefractingActionPill("))
        assertTrue("a 7-day selector", home.contains("selectedDayIndex"))

        val backdrop = obsidianFile("ObsidianAmbientBackground.kt")
        assertTrue(backdrop.contains("fun EditorialSilhouetteBackdrop("))
        assertTrue("a slow breathing silhouette", backdrop.contains("tween(9000"))

        val design = obsidianFile("ObsidianDesignSystem.kt")
        assertTrue(design.contains("fun ObsidianThemeProvider("))
        assertTrue(design.contains("fun ObsidianSignalDot("))
        assertTrue(design.contains("fun ObsidianHairlineSlider("))
    }

    @Test
    fun theHomeScreenReadsRealTelemetryRatherThanPlaceholders() {
        val home = obsidianFile("ObsidianHomeScreen.kt")
        // Same memoized Moon pipeline as every other Home variant — no fabricated numbers.
        assertTrue(home.contains("HomeAstroMemo.get("))
        assertTrue(home.contains("MoonKey("))
        assertTrue(home.contains("rememberReadyTimeMs("))
        assertTrue(home.contains("viewModel.skyLiveClock.timeMs.collectAsState()"))
        assertTrue(home.contains("moonData.illuminationPercent.roundToInt()"))
        assertFalse("no leftover placeholder content", home.contains("drinks today"))
    }

    @Test
    fun theLanguageHasItsOwnDocs() {
        val docs = listOf(
            File("docs/OBSIDIAN_QUARTZ_UI.md"),
            File("../docs/OBSIDIAN_QUARTZ_UI.md"),
            File("app/docs/OBSIDIAN_QUARTZ_UI.md")
        ).firstOrNull { it.exists() }
        assertTrue("the art direction is written down", docs != null)
        val text = docs!!.readText()
        assertTrue(text.contains("#170109"))
        assertTrue(text.contains("opticalQuartzSurface"))
        assertTrue(text.contains("DotMatrixNumber"))
    }
}
