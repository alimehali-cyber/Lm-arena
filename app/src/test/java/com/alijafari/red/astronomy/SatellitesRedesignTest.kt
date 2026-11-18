package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.ui.satellites.*
import com.alijafari.red.astronomy.ui.satellites.core.*
import com.alijafari.red.astronomy.Earth3DMath
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
    private val hud by lazy { source("com/alijafari/red/astronomy/ui/satellites/SatelliteHud.kt") }
    private val focus by lazy { source("com/alijafari/red/astronomy/ui/satellites/FocusHud.kt") }
    private val passes by lazy { source("com/alijafari/red/astronomy/ui/satellites/PassPanels.kt") }
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

    @Test
    fun savedStateRestoresLegacyAndToleratesUnknownFields() {
        val legacy = restoreSatelliteState(listOf("iss_zarya", "ALL", "90", "DARK")).ui
        assertEquals("iss_zarya", legacy.focusId)
        assertEquals(90, legacy.timeOffsetMin)
        assertEquals(MapTheme.DARK, legacy.mapTheme)
        assertEquals(SatPanel.None, legacy.activePanel)
        val damaged = restoreSatelliteState(listOf("", "9999", "future-theme", "future-panel")).ui
        assertEquals(720, damaged.timeOffsetMin)
        assertEquals(MapTheme.AUTO, damaged.mapTheme)
        assertEquals(SatPanel.None, damaged.activePanel)
    }

    // ---------------------------------------------------------------------------------------------
    // Numbers: signs are bidi-safe and there is never a `+-`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun countdownSwitchesToSecondsBelowTenMinutes() {
        assertEquals("2h 14m", SatFmt.countdown(134 * 60_000L, isFa = false))
        assertEquals("9:59", SatFmt.countdown(599_000L, isFa = false))
        assertTrue(SatFmt.countdown(599_000L, isFa = true).all { it !in '0'..'9' })
    }

    @Test
    fun compassWordsAndDirectionVectorHaveSeparateSemantics() {
        assertEquals("SW", SatFmt.compass(225.0, isFa = false))
        assertEquals("NE", SatFmt.compass(45.0, isFa = false))
        assertEquals("جنوب‌غرب", SatFmt.compass(225.0, isFa = true))
        assertEquals("شمال‌شرق", SatFmt.compass(45.0, isFa = true))
        assertTrue(passes.contains("Icons.AutoMirrored.Rounded.ArrowForward"))
        assertTrue(passes.contains("LayoutRole.Mirrors"))
        assertFalse(passes.contains('\u2192'))
        assertFalse(passes.contains('\u2190'))
    }

    // ---------------------------------------------------------------------------------------------
    // Nights and the spotlight
    // ---------------------------------------------------------------------------------------------

    @Test
    fun sunriseSeparatesAnEveningFromTheFollowingDay() {
        val london = TimeZone.getTimeZone("Europe/London")
        val nights = SatObserverNights(51.48, 0.0, london)
        fun at(day: Int, hour: Int) = Calendar.getInstance(london).apply {
            clear(); set(2025, Calendar.NOVEMBER, day, hour, 0)
        }.timeInMillis
        val evening = nights.key(at(17, 21))
        val window = requireNotNull(nights.window(evening))
        assertTrue(window.sunsetMs in at(17, 15)..at(17, 17))
        assertTrue(window.sunriseMs in at(18, 7)..at(18, 8))
        assertEquals(evening, nights.key(at(18, 2)))
        assertNotEquals("after sunrise belongs to the new date, not last night", evening, nights.key(at(18, 9)))
        assertEquals(evening, nights.key(window.sunriseMs - 1))
        assertNotEquals(evening, nights.key(window.sunriseMs))
    }

    @Test
    fun observerNightsRespectDaylightSavingAndPolarDay() {
        val tz = TimeZone.getTimeZone("Europe/Oslo")
        val nights = SatObserverNights(69.65, 18.96, tz)
        fun noon(month: Int, day: Int) = Calendar.getInstance(tz).apply {
            clear(); set(2026, month, day, 12, 0)
        }.timeInMillis
        assertEquals(23 * 3_600_000L, nights.plusDays(noon(Calendar.MARCH, 28), 1) - noon(Calendar.MARCH, 28))
        assertNull(nights.window(noon(Calendar.JUNE, 21)))
    }

    @Test
    fun skyBearingsCrossNorthAlongTheShortArc() {
        assertEquals(0.0, satBearingBetween(350.0, 10.0, .5f), .001)
        assertEquals(0.0, satBearingBetween(10.0, 350.0, .5f), .001)
        assertEquals(180.0, satBearingBetween(130.0, 230.0, .5f), .001)
    }

    @Test
    fun normalTextContrastMeetsAAForEverySkinAndPalette() {
        fun ratio(ink: Color, paper: Color): Float {
            val a = ink.compositeOver(paper).luminance() + .05f
            val b = paper.luminance() + .05f
            return maxOf(a, b) / minOf(a, b)
        }
        for (skin in SatSkin.entries) for (theme in ThemeMode.entries) {
            val t = zigSatTokens(skin, theme)
            for (background in listOf(t.canvas, t.card, t.cardAlt, t.hudFill)) {
                for (ink in listOf(t.inkPrimary, t.inkSecondary, t.inkTertiary, t.accentInk)) {
                    assertTrue("$skin $theme: $ink on $background", ratio(ink, background) >= 4.5f)
                }
            }
            assertTrue(ratio(t.hudInk, t.hudFill) >= 4.5f)
            assertTrue(ratio(Color.White, t.selectedChip) >= 4.5f)
            assertTrue(ratio(t.inkPrimary, t.popoverFill) >= 4.5f)
        }
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
    fun spotlightUpgradeUsesQualityRatherThanMagnitude() {
        val now = 1_800_000_000_000L
        val soon = passUi(now + 60_000L, score = 60)
        val later = passUi(now + 600_000L, score = 84).let { it.copy(pass = it.pass.copy(estimatedMagnitude = 0.5)) }
        assertEquals(later.key, SatNights.spotlight(listOf(soon, later), now, TimeZone.getTimeZone("UTC"), false)?.pass?.key)
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
        assertEquals(SatPanel.Layers, state.ui.activePanel)
        assertEquals(MapTheme.LIGHT, state.ui.mapTheme)
        state.dismiss()
        assertEquals(SatPanel.None, state.ui.activePanel)
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
            assertEquals("focus does not dismiss the catalogue or open Detail", SatPanel.Fleet, state.ui.activePanel)
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
    fun productionAndPureProjectionsShareTheMeasuredFrame() {
        val width = 390f
        val height = 844f
        val layout = HudLayout.measure(width, height, 24f, 80f, 150f, recenter = true)
        val poses = listOf(Camera(0f, 0f, 3.22f), Camera(.8f, -.6f, 2.4f),
            Camera(-2.3f, .4f, 4f), Camera(2.1f, -.9f, 3f))
        val points = listOf(Geo(0.0, 0.0, 420.0), Geo(35.0, 60.0, 600.0), Geo(-30.0, -150.0, 420.0))
        for (pose in poses) {
            val frame = layout.frameAtDistance(SATELLITE_EARTH_DEFAULT_DISTANCE, pose.distance)
            val focal = frame.diameter / (2 * Earth3DMath.projectedGlobeRadiusPx(pose.distance, height))
            val native = EarthCameraSnapshot(pose.yaw, pose.pitch, pose.distance,
                EarthViewportProjection(frame.center.y / height, focal))
            val pure = GlobeProjection(frame, pose)
            for (geo in points) {
                val expected = Earth3DMath.projectLatLon(geo.lat, geo.lon,
                    Earth3DMath.altitudeKmToRadius(geo.altKm), native, width, height)
                val actual = pure.project(geo)
                assertEquals(expected.screenX, actual.point.x, .02f)
                assertEquals(expected.screenY, actual.point.y, .02f)
                assertEquals(expected.visible, !actual.occluded)
            }
            layout.recenter?.let { assertTrue(it.distanceTo(frame.center) > frame.diameter / 2) }
        }
    }

    @Test
    fun theFocusCardAndTimeTrayAreTheOnlyMainScreenReadouts() {
        val pill = components.substringAfter("fun SatStatusPill").substringBefore("fun SatMetric")
        assertFalse(pill.contains("satAltKm"))
        assertFalse(pill.contains("SatScrubber("))
        assertTrue(focus.contains("pos.satAltKm"))
        assertEquals(1, Regex("SatTimeTray\\(").findAll(hud).count())
        assertFalse(stage.contains("satellite_mini_dock"))
        assertTrue(hud.contains("tokens.sim.copy(alpha=.35f)"))
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
        assertTrue(detail.contains("R.string.sat_dossier"))
    }
}
