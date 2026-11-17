package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.ui.satellites.SatFmt
import com.alijafari.red.astronomy.ui.satellites.SatNights
import com.alijafari.red.astronomy.ui.satellites.SatPassUi
import com.alijafari.red.astronomy.ui.satellites.SatQuality
import com.alijafari.red.astronomy.ui.satellites.SatSkin
import com.alijafari.red.astronomy.ui.satellites.matchesScope
import com.alijafari.red.astronomy.ui.satellites.passKey
import com.alijafari.red.astronomy.ui.satellites.qualityForScore
import com.alijafari.red.astronomy.ui.satellites.satWidthClass
import com.alijafari.red.astronomy.ui.satellites.zigSatTokens
import com.alijafari.red.astronomy.domain.ThemeMode
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Satellites redesign's guards.
 *
 * Two halves, as with the other source-level suites in this project:
 *
 * - the arithmetic the redesign depends on (signed-number formatting, night grouping, the spotlight
 *   selection rule) is unit-tested directly, because it is plain Kotlin;
 * - the parts that only exist as Compose structure (one stage, three skins, one time control, 48 dp
 *   targets, no `+-`) are asserted against the source, because a screenshot over a GL landscape
 *   cannot prove them.
 */
class SatellitesRedesignTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private val stage by lazy { source("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt") }
    private val design by lazy { source("com/alijafari/red/astronomy/ui/satellites/SatelliteDesign.kt") }
    private val components by lazy { source("com/alijafari/red/astronomy/ui/satellites/SatelliteComponents.kt") }
    private val sheets by lazy { source("com/alijafari/red/astronomy/ui/satellites/SatelliteSheets.kt") }
    private val detail by lazy { source("com/alijafari/red/astronomy/ui/screens/SatelliteDetailScreen.kt") }

    private fun pass(
        startMs: Long,
        durationMin: Int = 6,
        maxElevation: Double = 68.0,
        score: Int = 72,
    ): ISSEngine.ISSPass {
        val durationMs = durationMin * 60_000L
        return ISSEngine.ISSPass(
            startTimeMs = startMs,
            maxTimeMs = startMs + durationMs / 2,
            endTimeMs = startMs + durationMs,
            maxElevationDeg = maxElevation,
            maxAltitudeKm = 418.0,
            maxAzimuthDeg = 180.0,
            startAzimuthDeg = 225.0,
            endAzimuthDeg = 45.0,
            estimatedMagnitude = -3.4,
            passDurationSec = durationMin * 60L,
            sunAltitudeDegAtMax = -12.0,
            isObserverInDarkness = true,
            isIssSunlitAtMax = true,
            classification = ISSEngine.PassClassification.GOOD,
            visibilityScore = score,
            summaryReasonEn = "",
            summaryReasonFa = "",
            detailedReasonsEn = emptyList(),
            detailedReasonsFa = emptyList(),
            isVisible = true,
        )
    }

    private fun passUi(
        startMs: Long,
        score: Int = 72,
        maxElevation: Double = 68.0,
    ): SatPassUi {
        val sat = SatelliteCatalog.satellites.first { it.id == "iss_zarya" }
        val p = pass(startMs, maxElevation = maxElevation, score = score)
        return SatPassUi(
            key = passKey(sat, p),
            sat = sat,
            satName = sat.nameEn,
            pass = p,
            nightKey = SatNights.nightKey(p.startTimeMs, TimeZone.getTimeZone("UTC")),
            reminderLeadMinutes = null,
            isAutoCovered = false,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Numbers: signs are bidi-safe and there is never a `+-`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun signedNumbersUseARealMinusAndAnExplicitPlus() {
        val negative = SatFmt.signed(-3.4, 1, "", isFa = false)
        val positive = SatFmt.signed(1.2, 1, "", isFa = false)

        assertTrue("negative carries U+2212", negative.contains('\u2212'))
        assertFalse("negative never uses a hyphen", negative.contains('-'))
        assertTrue("positive carries an explicit plus", positive.contains('+'))
        // The isolate pair is what stops a sign jumping to the other side in RTL.
        assertTrue("the number is isolated", negative.contains('\u2068') && negative.contains('\u2069'))
        assertFalse("no ASCII plus-before-minus anywhere", negative.contains("+-"))
    }

    @Test
    fun persianNumbersUsePersianDigitsAndTheArabicDecimalSeparator() {
        val fa = SatFmt.signed(-3.4, 1, "", isFa = true)
        assertTrue("persian digits", fa.any { it in '\u06F0'..'\u06F9' })
        assertTrue("arabic decimal separator", fa.contains('\u066B'))
        assertFalse("no latin digits", fa.any { it in '0'..'9' })
    }

    @Test
    fun countdownSwitchesToSecondsBelowTenMinutes() {
        assertEquals("2h 14m", SatFmt.countdown(134 * 60_000L, isFa = false))
        assertEquals("9:59", SatFmt.countdown(599_000L, isFa = false))
        assertTrue(SatFmt.countdown(599_000L, isFa = true).all { it !in '0'..'9' })
    }

    @Test
    fun directionTextMirrorsInPersianAndKeepsTheCompassWords() {
        val en = SatFmt.direction(225.0, 45.0, isFa = false)
        assertEquals("SW \u2192 NE", en)
        val fa = SatFmt.direction(225.0, 45.0, isFa = true)
        // Persian word order is the source order; the arrow points the other way so the reading
        // direction still runs rise → set.
        assertTrue("rise sits at the start edge in RTL", fa.startsWith("جنوب\u200Cغرب"))
        assertTrue("and the arrow flips", fa.contains('\u2190'))
    }

    // ---------------------------------------------------------------------------------------------
    // Nights and the spotlight
    // ---------------------------------------------------------------------------------------------

    @Test
    fun nightsAreObserverNightsFromNoonToNoon() {
        val utc = TimeZone.getTimeZone("UTC")
        fun keyOf(y: Int, m: Int, d: Int, h: Int): Long {
            val cal = Calendar.getInstance(utc).apply {
                set(y, m, d, h, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        // 02:00 and 21:00 of the same calendar day belong to different observer nights.
        val evening = SatNights.nightKey(keyOf(2026, 10, 17, 21), utc)
        val afterMidnight = SatNights.nightKey(keyOf(2026, 10, 18, 2), utc)
        assertEquals("evening and post-midnight share one night", evening, afterMidnight)

        val nextEvening = SatNights.nightKey(keyOf(2026, 10, 18, 21), utc)
        assertNotEquals(evening, nextEvening)
    }

    @Test
    fun spotlightTakesTheSoonestGoodPass() {
        val now = 1_800_000_000_000L
        val poorSoon = passUi(now + 10 * 60_000L, score = 40)
        val goodLater = passUi(now + 60 * 60_000L, score = 72)

        val chosen = SatNights.spotlight(listOf(poorSoon, goodLater), now, TimeZone.getTimeZone("UTC"), false)
        assertEquals("a poor pass is skipped for a good one", goodLater.key, chosen?.pass?.key)
        assertEquals(60, chosen?.remainingMinutes)
    }

    @Test
    fun spotlightPrefersABrighterPassWithinSixHours() {
        val now = 1_800_000_000_000L
        val good = passUi(now + 30 * 60_000L, score = 60)
        val veryGood = passUi(now + 90 * 60_000L, score = 80)

        val chosen = SatNights.spotlight(listOf(good, veryGood), now, TimeZone.getTimeZone("UTC"), false)
        assertEquals(veryGood.key, chosen?.pass?.key)
    }

    @Test
    fun spotlightReportsAPassInProgress() {
        val now = 1_800_000_000_000L
        val running = passUi(now - 2 * 60_000L)
        val chosen = SatNights.spotlight(listOf(running), now, TimeZone.getTimeZone("UTC"), false)
        assertTrue("the running pass is the spotlight", chosen?.isInProgress == true)
        assertEquals(4, chosen?.remainingMinutes)
    }

    @Test
    fun spotlightIgnoresFinishedPassesEntirely() {
        val now = 1_800_000_000_000L
        val finished = passUi(now - 3 * 60 * 60_000L)
        assertNull(SatNights.spotlight(listOf(finished), now, TimeZone.getTimeZone("UTC"), false))
    }

    @Test
    fun passKeysAreStableAcrossRecomputation() {
        val sat = SatelliteCatalog.satellites.first { it.id == "iss_zarya" }
        val a = pass(1_800_000_000_000L)
        val b = a.copy(visibilityScore = 12)
        assertEquals("the key is (satId, riseTime) only", passKey(sat, a), passKey(sat, b))
    }

    // ---------------------------------------------------------------------------------------------
    // Scope, quality, width classes
    // ---------------------------------------------------------------------------------------------

    @Test
    fun scopeFiltersMatchTheChips() {
        val satellites = SatelliteCatalog.satellites
        assertTrue(matchesScope(satellites.first(), SatelliteCategory.ALL))
        assertTrue(satellites.filter { matchesScope(it, SatelliteCategory.ISS) }.all {
            it.category == SatelliteCategory.ISS
        })
        assertTrue(satellites.filter { matchesScope(it, SatelliteCategory.VISIBLE) }.all { it.isNakedEyeCandidate })
    }

    @Test
    fun qualityIsAlwaysPairedWithAScoreOrLabel() {
        assertEquals(SatQuality.OUTSTANDING, qualityForScore(90))
        assertEquals(SatQuality.GOOD, qualityForScore(55))
        assertEquals(SatQuality.POOR, qualityForScore(0))
        assertTrue(SatQuality.entries.all { it.labelEn.isNotBlank() && it.labelFa.isNotBlank() })
    }

    @Test
    fun widthClassesFollowTheSpec() {
        assertEquals("compact <360dp", "COMPACT", satWidthClass(359).name)
        assertEquals("medium 360–411dp", "MEDIUM", satWidthClass(360).name)
        assertEquals("large >=412dp", "LARGE", satWidthClass(412).name)
    }

    // ---------------------------------------------------------------------------------------------
    // Skin tokens: semantic names, per theme, no hard-coded navy
    // ---------------------------------------------------------------------------------------------

    @Test
    fun everySkinResolvesSemanticTokensForAllThreeThemes() {
        for (skin in SatSkin.entries) {
            for (theme in listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.WINE)) {
                val tokens = zigSatTokens(skin, theme)
                assertEquals(skin, tokens.skin)
                assertEquals(theme, tokens.themeMode)
                assertEquals("Default never blurs or glows", skin != SatSkin.DEFAULT, tokens.glassy)
            }
        }
        // Light mode uses the darkened blue / amber the spec requires for AA contrast. Colour is
        // packed as ULong in Compose, so the pair is asserted at the source rather than by value.
        assertTrue(
            "light-mode starlink is the darkened blue",
            design.contains("starlink = if (isDark) Color(0xFF38BDF8) else Color(0xFF0B7BB5)")
        )
        assertTrue(
            "light-mode amber is the darkened gold",
            design.contains("else -> Color(0xFF8A6A1F)")
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Acceptance, asserted against the source
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theThreeSkinsAreOneStage() {
        val screens = listOf(
            source("com/alijafari/red/astronomy/ui/screens/ISSScreen.kt") to "SatSkin.DEFAULT",
            source("com/alijafari/red/astronomy/ui/spatial/SpatialSatellitesScreen.kt") to "SatSkin.SPATIAL",
            source("com/alijafari/red/astronomy/ui/aether/AetherSatellitesScreen.kt") to "SatSkin.AETHER",
        )
        screens.forEach { (src, skin) ->
            assertTrue(src.contains("SatellitesStage("))
            assertTrue(src.contains(skin))
        }
        // The stage owns the anatomy order in one place.
        listOf("\"header\"", "\"globe\"", "\"dock\"", "\"timestrip\"", "\"scope\"", "\"fleet\"", "\"spotlight\"", "\"passes\"").forEach {
            assertTrue("the stage orders $it", stage.contains("item(key = $it") || stage.contains("item(key = $it)"))
        }
    }

    @Test
    fun everyInteractiveControlIsAtLeastFortyEightDp() {
        assertTrue("the grid sets the target once", design.contains("val minTouchTarget: Dp = 48.dp"))
        // Any visual smaller than 48 dp is wrapped in a 48 dp hit area (the globe controls and
        // the icon buttons are 40/44 dp visuals inside a full-size target).
        assertTrue(components.contains(".size(44.dp)") && components.contains(".size(SatGrid.minTouchTarget)"))
        assertTrue(design.contains(".size(40.dp)") && design.contains(".size(SatGrid.minTouchTarget)"))
        // The chip, the icon button, the bell, the fleet sheet row and the step buttons all size from it.
        assertTrue(design.contains("ZigSatChip"))
        assertTrue(design.contains(".height(SatGrid.minTouchTarget)"))
        assertTrue(components.contains(".size(SatGrid.minTouchTarget)"))
        assertTrue(design.contains("sizeIn(minWidth = SatGrid.minTouchTarget"))
        assertTrue(sheets.contains(".height(56.dp)"))
    }

    @Test
    fun thereIsNoPlusMinusSignConcatentationLeftAnywhere() {
        listOf(stage, design, components, sheets, detail).forEach { text ->
            assertFalse("a formatted sign is never prefixed by hand", text.contains("\"+-\""))
            assertFalse(text.contains("\"+-"))
            assertFalse("not via a %+.1f format either", text.contains("%+.1f"))
        }
        assertTrue("the one formatter does it", design.contains("val MINUS = '\\u2212'"))
    }

    @Test
    fun theTimeStripIsTheOnlyTimeControlAndTheDockIsTheOnlyAltitudeHome() {
        // Time: one strip, its Live capsule and its steppers — no second scrubber anywhere.
        assertEquals(
            "exactly one time strip in the app",
            1,
            Regex("SatTimeStrip\\(").findAll(stage).count()
        )
        assertEquals(
            "exactly one scrubber implementation",
            1,
            Regex("private fun SatScrubber\\(").findAll(components).count()
        )
        // The status chip is read-only state: it carries no altitude and no scrub affordance.
        val chip = components.substring(
            components.indexOf("fun SatStatusChip("),
            components.indexOf("// 2. GLOBE STAGE"),
        )
        assertFalse("the status chip carries no altitude", chip.contains("altKm"))
        assertFalse("and no scrub control", chip.contains("Slider"))
        // The dock is where altitude lives on the main screen.
        assertTrue(components.contains("altLabel to"))
    }

    @Test
    fun focusIsNotOpenAndEveryEntryPointObeysIt() {
        // Rail token, popover and pass card all go through the same rule: focus first, open second.
        assertTrue("a second tap on the same rail token opens detail", stage.contains("if (tokenUi.id == selectedSatelliteId)"))
        assertTrue("a second tap on the same marker opens detail", stage.contains("if (state.satellite.id == selectedSatelliteId)"))
        assertTrue("the explicit dock control opens detail", components.contains("testTag = \"dock_details\""))
    }

    @Test
    fun everyPreservedFeatureStillHasAHome() {
        assertTrue("globe", stage.contains("SatelliteEarth3D("))
        assertTrue("reset", components.contains("map_reset_view_stage"))
        assertTrue("map theme Auto/Dark/Light", stage.contains("MapTheme.AUTO -> MapTheme.DARK"))
        assertTrue("categories", components.contains("SatelliteCategory.entries"))
        assertTrue("±12h one-minute scrub", components.contains("1440f") && components.contains("(-720, 720)"))
        assertTrue("live detent", components.contains("if (kotlin.math.abs(raw) <= 4f) 0f else raw"))
        assertTrue("pass markers on the scrubber", components.contains("passMarkers.forEach"))
        assertTrue("offline transparency", components.contains("CloudOff"))
        assertTrue("TLE transparency", stage.contains("formatTleStalenessBadge("))
        assertTrue("7-day rolling alerts", sheets.contains("Rolling 7 days") && sheets.contains("۷ روزه"))
        assertTrue("lead times 10m / 30m / 1d", sheets.contains("1440 to"))
        assertTrue("reminder states on the card", components.contains("isAutoCovered"))
        assertTrue("train badge", components.contains("STRING OF PEARLS"))
        assertTrue("legend", components.contains("globe_legend_panel"))
        assertTrue("guide", sheets.contains("guide_sheet"))
    }

    @Test
    fun theDetailScreenRendersInTheActiveSkinWithNoHardCodedNavy() {
        assertTrue(detail.contains("skin: SatSkin = SatSkin.DEFAULT"))
        assertTrue("the host passes the real theme", stage.contains("themeMode = uiState.themeMode"))
        assertFalse("no navy hero anymore", detail.contains("Color(0xFF0F172A)"))
        assertTrue("a floating back button, not an app bar", detail.contains("detail_floating_back"))
        assertFalse("no top bar", detail.contains("topBar = {"))
        assertTrue("live telemetry panel", detail.contains("Live orbital telemetry"))
        assertTrue("visibility assessment", detail.contains("conditions met"))
        assertTrue("mission dossier", detail.contains("Mission dossier"))
        assertTrue("pass preview", detail.contains("Back to next"))
    }

    @Test
    fun theMiniDockAndSimulationVignetteExist() {
        assertTrue(stage.contains("satellite_mini_dock"))
        assertTrue(stage.contains("derivedStateOf { listState.firstVisibleItemIndex > 1 }"))
        assertTrue("the mini dock can always exit simulation", stage.contains("onReturnToLive"))
        assertTrue("the globe shows the sim vignette", components.contains("tokens.sim.copy(alpha = 0.35f)"))
    }
}
