package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.ui.satellites.*
import com.alijafari.red.astronomy.Earth3DMath
import com.alijafari.red.astronomy.ui.components.satelliteViewport
import com.alijafari.red.astronomy.ui.components.SATELLITE_EARTH_DEFAULT_DISTANCE
import com.alijafari.red.astronomy.ui.screens.MapTheme
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
        assertTrue(stage.contains("SatelliteHud("))
        assertFalse("Only Detail or panel content scrolls", stage.contains("LazyColumn("))
        assertTrue(stage.contains("SatelliteEarth3D("))
    }

    @Test
    fun aSecondPanelReplacesTheFirstAndDismissPreservesTheOffset() {
        val state = SatellitesState(SatellitesUiState())
        state.panel(SatPanel.Time)
        state.offset(150)
        state.panel(SatPanel.Layers)
        assertEquals(SatPanel.Layers, state.ui.activePanel)
        state.lighting(MapTheme.LIGHT)
        assertEquals(SatPanel.None, state.ui.activePanel)
        assertEquals(MapTheme.LIGHT, state.ui.mapTheme)
        assertEquals(150, state.ui.timeOffsetMin)
        state.offset(-900)
        assertEquals(-720, state.ui.timeOffsetMin)
    }

    @Test
    fun selectingThreeTimesCannotOpenADetailOrPanel() {
        val state = SatellitesState(SatellitesUiState())
        state.panel(SatPanel.Fleet)
        listOf("hubble", "tiangong", "iss_zarya").forEach { id ->
            state.focus(id)
            assertEquals(id, state.ui.focusId)
            assertEquals(SatPanel.None, state.ui.activePanel)
        }
    }

    @Test
    fun timeDetentsAndJumpStayInsideTheTwelveHourWindow() {
        val markers = listOf(SatTimeMarker(2, SatQuality.GOOD), SatTimeMarker(93, SatQuality.EXCELLENT), SatTimeMarker(750, SatQuality.GOOD))
        assertEquals(0, snapSatelliteTime(4f, markers))
        assertEquals(93, snapSatelliteTime(90f, markers))
        assertEquals(89, snapSatelliteTime(89f, markers))
        assertEquals(720, snapSatelliteTime(1000f, markers))
        assertEquals(93, nextSatelliteTime(markers, 10))
        assertNull(nextSatelliteTime(markers, 93))
    }

    @Test
    fun portraitViewportFitsBetweenTheHeaderAndCard() {
        val width = 390f
        val height = 844f
        val top = 92f
        val bottom = 609f
        val projection = satelliteViewport(width, height, top, bottom)
        val radius = Earth3DMath.projectedGlobeRadiusPx(SATELLITE_EARTH_DEFAULT_DISTANCE, height, projection.focalScale)
        assertTrue(radius * 2 >= .86f * width)
        assertEquals((top + bottom) / 2f, projection.centerY * height, .001f)
        assertTrue(projection.centerY * height + radius < bottom)
    }

    @Test
    fun theFocusCardAndTimeTrayAreTheOnlyMainScreenReadouts() {
        val pill = components.substringAfter("fun SatStatusPill").substringBefore("fun SatAvatar")
        assertFalse(pill.contains("satAltKm"))
        assertFalse(pill.contains("SatScrubber("))
        assertTrue(components.substringAfter("fun SatFocusCard").substringBefore("fun SatMetric").contains("satAltKm"))
        assertEquals(1, Regex("SatTimeTray\\(").findAll(stage).count())
        assertFalse(stage.contains("satellite_mini_dock"))
        assertTrue(stage.contains("tokens.sim.copy(alpha = 0.35f)"))
    }

    @Test
    fun remindersAreAnchoredAndTheDetailUsesTheSharedMaterial() {
        assertTrue(sheets.contains("PopupPositionProvider"))
        assertTrue(sheets.contains("listOf(10, 30, 1440)"))
        assertTrue(stage.contains("scheduleSpecificPassAlarm("))
        assertTrue(stage.contains("cancelSpecificPassAlarm("))
        assertTrue(stage.contains("SharedTransitionLayout("))
        assertTrue(detail.contains("detail_floating_back"))
        assertFalse(detail.contains("Color(0xFF0F172A)"))
        assertTrue(detail.contains("Mission dossier"))
    }
}
