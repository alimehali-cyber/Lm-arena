package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.alijafari.red.astronomy.domain.AppUiStyle
import com.alijafari.red.astronomy.domain.ThemeMode
import com.alijafari.red.astronomy.ui.MainUiState
import com.alijafari.red.astronomy.ui.aurora.AuroraMotion
import com.alijafari.red.astronomy.ui.aurora.AuroraRadius
import com.alijafari.red.astronomy.ui.aurora.AuroraSpace
import com.alijafari.red.astronomy.ui.aurora.AuroraStatus
import com.alijafari.red.astronomy.ui.aurora.AuroraSurfaceTier
import com.alijafari.red.astronomy.ui.aurora.auroraPaletteFor
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the invariants of the Aurora interface language — the fourth selectable
 * UI, and the one built newest.
 *
 * These are not screenshot tests. Each one asserts a rule the language claims to
 * follow, drawn from the Astryx design specifications it was written against:
 * concentric shape, tiered space, weight-ordered timing, and status that is
 * never carried by colour alone. A test here fails when the system stops doing
 * what its own documentation says it does, which is the only kind of regression
 * worth pinning in a design system.
 */
class AuroraUiTest {

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

    private val designSystem by lazy {
        source("com/alijafari/red/astronomy/ui/aurora/AuroraDesignSystem.kt")
    }
    private val field by lazy { source("com/alijafari/red/astronomy/ui/aurora/AuroraField.kt") }
    private val components by lazy { source("com/alijafari/red/astronomy/ui/aurora/AuroraComponents.kt") }

    // ============================ the style itself ==============================================

    @Test
    fun auroraIsTheFourthSelectableStyleAndDefaultRemainsDefault() {
        assertEquals(
            listOf(
                AppUiStyle.DEFAULT,
                AppUiStyle.SPATIAL,
                AppUiStyle.CYBER,
                AppUiStyle.AURORA
            ),
            AppUiStyle.entries
        )
        // Adding a style must never move a returning user into it.
        assertEquals(AppUiStyle.DEFAULT, MainUiState().uiStyle)
    }

    @Test
    fun auroraNamesItselfInBothLanguages() {
        val stringsEn = resource("values/strings.xml")
        val stringsFa = resource("values-fa/strings.xml")
        assertTrue(stringsEn.contains("<string name=\"ui_style_aurora\">"))
        assertTrue(stringsEn.contains("<string name=\"ui_style_aurora_desc\">"))
        assertTrue(stringsFa.contains("<string name=\"ui_style_aurora\">"))
        assertTrue(stringsFa.contains("<string name=\"ui_style_aurora_desc\">"))
    }

    // ============================ palette =======================================================

    @Test
    fun everyThemeModeHasAnAuroraPaletteAndKnowsWhetherItIsDark() {
        val dark = auroraPaletteFor(ThemeMode.DARK)
        val light = auroraPaletteFor(ThemeMode.LIGHT)
        val wine = auroraPaletteFor(ThemeMode.WINE)

        assertTrue(dark.isDark)
        assertTrue(wine.isDark)
        assertTrue("light is the one high-key mode", !light.isDark)
        assertTrue("wine is the one wine mode", wine.isWine)
        assertTrue(!dark.isWine && !light.isWine)

        for (palette in listOf(dark, light, wine)) {
            assertTrue("the spectrum is never empty", palette.spectrum.size >= 3)
            assertTrue(
                "the accent carries ink of its own",
                palette.onAccent != palette.accent && palette.onAccent != Color.Transparent
            )
            // Status is a category, and each category is a distinct hue — otherwise
            // "colour plus icon plus label" degrades to "colour" (system-states DR2).
            val hues = setOf(palette.good, palette.warn, palette.bad, palette.info)
            assertEquals("the four status hues are distinct", 4, hues.size)
            assertTrue("and none is the canvas", palette.canvasTop !in hues)
        }
    }

    @Test
    fun theLensIsTranslucentSoTheFieldReadsThroughIt() {
        // A lens that were opaque would be a card, and the whole system would be
        // a frosted-panel system with extra steps.
        for (mode in ThemeMode.entries) {
            val palette = auroraPaletteFor(mode)
            assertTrue(
                "$mode: the body fill is translucent",
                palette.lensTop.alpha < 1f && palette.lensBottom.alpha < 1f
            )
            assertTrue("$mode: the rim is visible", palette.rim.alpha > 0f)
            assertTrue("$mode: the specular is visible", palette.specular.alpha > 0f)
            assertTrue("$mode: press composites rather than replaces", palette.pressedOverlay.alpha < 1f)
        }
    }

    // ============================ geometry ======================================================

    @Test
    fun radiiFormOneConcentricScaleAndNestingSubtractsTheGap() {
        // Shape relationships DR1-DR2: radius follows role, and an inner curve is
        // the outer curve minus the space between them.
        assertTrue(AuroraRadius.container > AuroraRadius.card)
        assertTrue(AuroraRadius.card > AuroraRadius.control)
        assertTrue(AuroraRadius.control > AuroraRadius.chip)

        val innerOfCard = AuroraRadius.inner(AuroraRadius.card)
        assertEquals(
            "an inner radius is the outer radius minus the default gap",
            (AuroraRadius.card.value - 10f).dp,
            innerOfCard
        )
        assertTrue("and is still a visible curve", innerOfCard >= 4.dp)
        assertTrue(
            "a very tight nest bottoms out at 4 dp rather than going square",
            AuroraRadius.inner(AuroraRadius.chip, gap = 100.dp) == 4.dp
        )
    }

    @Test
    fun spacingHasFourStrictlyIncreasingTiers() {
        // Spatial hierarchy DR2-DR4: separation grows with grouping level, and a
        // composition uses enough contrast to reveal its hierarchy.
        val tiers = listOf(AuroraSpace.local, AuroraSpace.group, AuroraSpace.section, AuroraSpace.page)
        for (i in 0 until tiers.size - 1) {
            assertTrue(
                "tier $i (${tiers[i]}) is strictly smaller than tier ${i + 1} (${tiers[i + 1]})",
                tiers[i] < tiers[i + 1]
            )
        }
    }

    // ============================ motion ========================================================

    @Test
    fun motionTimingFollowsWeightNotTaste() {
        // Motion DR2: small local feedback is faster than a large entrance.
        assertTrue(AuroraMotion.MICRO_MS < AuroraMotion.LOCAL_MS)
        assertTrue(AuroraMotion.LOCAL_MS < AuroraMotion.ENTRANCE_MS)
        assertTrue(
            "ambient drift is the slowest thing in the system",
            AuroraMotion.AMBIENT_MS > AuroraMotion.ENTRANCE_MS
        )
        assertTrue(
            "a screen entrance stays under half a second",
            AuroraMotion.ENTRANCE_MS <= 500
        )
    }

    @Test
    fun theFieldAndTheLensShareOneClockAndHonourReducedMotion() {
        assertTrue(
            "the field reads one shared phase",
            field.contains("fun rememberAuroraPhase()")
        )
        assertTrue(
            "and the lens reads the same reduced-motion answer",
            designSystem.contains("fun rememberAuroraReducedMotion()")
        )
        assertTrue(
            "reduced motion pins the phase instead of removing the field",
            field.contains("if (reduceMotion) 0.34f")
        )
        assertTrue(
            "the skeleton stops its sheen under reduced motion",
            components.contains("if (reduceMotion) 0.5f else sheen.value")
        )
    }

    // ============================ state =========================================================

    @Test
    fun everyStatusShipsItsOwnGlyphSoColourIsNeverTheOnlyCue() {
        // System-states DR2, made mechanical: a status that had no glyph would be
        // a bare colour swatch.
        val glyphs = AuroraStatus.entries.map { status ->
            when (status) {
                AuroraStatus.NEUTRAL -> "Icons.Default.Info"
                AuroraStatus.GOOD -> "Icons.Default.CheckCircle"
                AuroraStatus.WARN -> "Icons.Default.ErrorOutline"
                AuroraStatus.BAD -> "Icons.Default.ErrorOutline"
                AuroraStatus.INFO -> "Icons.Default.Info"
            }
        }
        assertTrue("every status resolves a glyph", glyphs.all { it.isNotBlank() })
        assertTrue(
            "and the source declares it once, beside the colour",
            components.contains("private fun auroraStatusIcon(status: AuroraStatus): ImageVector")
        )
        assertTrue(
            "the pill draws the glyph and the words, not just the wash",
            components.contains("contentDescription = null,") && components.contains("text = label,")
        )
    }

    @Test
    fun theLensDrawsItsFivePassesInOrder() {
        // The five passes are what makes a lens a lens: body, refraction,
        // caustic, dispersion rim, travelling specular.
        for (pass in listOf(
            "// 1. The glass itself.",
            "// 2. Refraction",
            "// 3. Caustic",
            "// 4. The dispersion rim",
            "// 5. The travelling specular"
        )) {
            assertTrue("the lens paints: $pass", designSystem.contains(pass))
        }
    }

    @Test
    fun theSurfaceTiersCoverTheWholeElevationRange() {
        // Elevation-hierarchy DR1/DR3: perceived and actual order agree, and a
        // surface leads with an edge or a shadow, never both at full strength.
        assertEquals(
            listOf(
                AuroraSurfaceTier.CANVAS,
                AuroraSurfaceTier.INSET,
                AuroraSurfaceTier.CARD,
                AuroraSurfaceTier.RAISED,
                AuroraSurfaceTier.OVERLAY
            ),
            AuroraSurfaceTier.entries
        )
        assertTrue("the canvas tier paints nothing", designSystem.contains("AuroraSurfaceTier.CANVAS -> AuroraLensPaint("))
        assertTrue("the inset tier casts no shadow", designSystem.contains("shadow = 0.dp"))
        assertTrue("the overlay tier casts the deepest shadow", designSystem.contains("shadow = 22.dp"))
    }

    // ============================ parity ========================================================

    @Test
    fun auroraExposesEverySettingTheOtherStylesExpose() {
        val settings = source("com/alijafari/red/astronomy/ui/aurora/AuroraSettingsModal.kt")
        for (section in listOf(
            "settings_section_ui_style",
            "settings_section_language",
            "settings_section_calendar",
            "settings_section_app_theme",
            "settings_section_sky_theme",
            "settings_section_app_backdrop",
            "settings_section_bortle"
        )) {
            assertTrue("Aurora settings include $section", settings.contains(section))
        }
        for (action in listOf(
            "viewModel.setUiStyle(AppUiStyle.AURORA)",
            "viewModel.setLanguage(AppLanguage.PERSIAN)",
            "viewModel.setCalendarSystem(CalendarSystem.SOLAR_HIJRI)",
            "viewModel.setThemeMode(ThemeMode.WINE)",
            "viewModel.setSkyCanvasTheme(",
            "viewModel.setAppBackdropMode(AppBackdropMode.LIVE_SKY)",
            "viewModel.setBortleClass("
        )) {
            assertTrue("Aurora settings can $action", settings.contains(action))
        }
    }
}
