package com.alijafari.red.astronomy

import androidx.compose.runtime.saveable.SaverScope
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCategory
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.ui.satellites.STARLINK_SCOPE_ID
import com.alijafari.red.astronomy.ui.satellites.SatNights
import com.alijafari.red.astronomy.ui.satellites.SatPassUi
import com.alijafari.red.astronomy.ui.satellites.SatellitesState
import com.alijafari.red.astronomy.ui.satellites.SatellitesUiState
import com.alijafari.red.astronomy.ui.satellites.nextPassInScope
import com.alijafari.red.astronomy.ui.satellites.passKey
import com.alijafari.red.astronomy.ui.satellites.passScopeMatches
import java.io.File
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bottom of the satellites screen is two cards with two jobs: the satellite card owns picking a
 * satellite and its own next pass, the passes card owns "what is next over me, whoever flies it" and
 * the way into the seven-day agenda. These checks pin that split, the scope that narrows it, and the
 * week-long agenda behind it.
 */
class SatPassCardsTest {

    private val tz = TimeZone.getTimeZone("UTC")

    private fun readSource(relative: String): String {
        val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Source not found: $relative")
    }

    private fun readRes(relative: String): String {
        val candidates = listOf(File("src/main/res/$relative"), File("app/src/main/res/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Resource not found: $relative")
    }

    private fun pass(startMs: Long, durationMin: Int = 8): ISSEngine.ISSPass {
        val durationMs = durationMin * 60_000L
        return ISSEngine.ISSPass(
            startTimeMs = startMs, maxTimeMs = startMs + durationMs / 2, endTimeMs = startMs + durationMs,
            maxElevationDeg = 62.0, maxAltitudeKm = 418.0, maxAzimuthDeg = 180.0,
            startAzimuthDeg = 225.0, endAzimuthDeg = 45.0, estimatedMagnitude = -3.2,
            passDurationSec = durationMin * 60L, sunAltitudeDegAtMax = -12.0,
            isObserverInDarkness = true, isIssSunlitAtMax = true,
            classification = ISSEngine.PassClassification.GOOD, visibilityScore = 70,
            summaryReasonEn = "", summaryReasonFa = "", detailedReasonsEn = emptyList(),
            detailedReasonsFa = emptyList(), isVisible = true,
        )
    }

    private fun row(sat: SatelliteItem, startMs: Long, durationMin: Int = 8): SatPassUi {
        val p = pass(startMs, durationMin)
        return SatPassUi(passKey(sat, p), sat, sat.nameEn, p, SatNights.nightKey(p.startTimeMs, tz), null, false)
    }

    private val iss get() = SatelliteCatalog.satellites.first { it.id == "iss_zarya" }
    private val hubble get() = SatelliteCatalog.satellites.first { it.id == "hubble" }
    private val tiangong get() = SatelliteCatalog.satellites.first { it.id == "tiangong" }
    private val train get() = SatelliteCatalog.satellites.first { it.id == "iss_zarya" }.copy(
        id = "starlink_train", nameEn = "Starlink train", category = SatelliteCategory.STARLINK,
        isTrain = true, isConstellation = true,
    )
    private val pearl get() = train.copy(id = "starlink_pearl_60123", isTrain = false)

    @Test
    fun theCardShowsTheSoonestPassWhoeverFliesIt() {
        val now = 1_800_000_000_000L
        val passes = listOf(row(hubble, now + 30 * 60_000L), row(iss, now + 10 * 60_000L), row(train, now + 20 * 60_000L))
        assertEquals("the ISS pass is simply the next one", iss.id + "_" + (now + 10 * 60_000L),
            nextPassInScope(passes, emptySet(), now)?.key)

        // Order in the list must not matter, and a finished pass is never "next".
        val finished = row(hubble, now - 40 * 60_000L)
        val started = row(tiangong, now - 2 * 60_000L, durationMin = 10)
        val mixed = listOf(finished, row(hubble, now + 45 * 60_000L), started)
        assertEquals("a pass already in progress wins, it is happening now",
            started.key, nextPassInScope(mixed, emptySet(), now)?.key)
        assertNull(nextPassInScope(listOf(finished), emptySet(), now))
        assertNull(nextPassInScope(emptyList(), emptySet(), now))
    }

    @Test
    fun theScopeNarrowsToOneSatelliteOrToASet() {
        val now = 1_800_000_000_000L
        val passes = listOf(row(iss, now + 10 * 60_000L), row(hubble, now + 25 * 60_000L), row(tiangong, now + 40 * 60_000L))

        assertEquals("one satellite", hubble.id + "_" + (now + 25 * 60_000L),
            nextPassInScope(passes, setOf(hubble.id), now)?.key)
        assertEquals("a set keeps the soonest of the chosen ones", iss.id + "_" + (now + 10 * 60_000L),
            nextPassInScope(passes, setOf(iss.id, hubble.id), now)?.key)
        assertNull("nothing chosen flies in this window", nextPassInScope(passes, setOf("envisat"), now))
    }

    @Test
    fun theStarlinkChipCoversTheTrainAndEveryPearl() {
        assertTrue(passScopeMatches(train, emptySet()))
        assertTrue(passScopeMatches(pearl, setOf(STARLINK_SCOPE_ID)))
        assertTrue("the train's own id still works", passScopeMatches(train, setOf(train.id)))
        assertFalse("a pearl is not a named satellite id", passScopeMatches(pearl, setOf("iss_zarya")))
        assertFalse(passScopeMatches(iss, setOf(STARLINK_SCOPE_ID)))
        assertEquals("starlink_train", STARLINK_SCOPE_ID)
    }

    @Test
    fun theScopeSurvivesRotationAndKeepsItsOwnSaverSlot() {
        val state = SatellitesState(SatellitesUiState())
        state.togglePassScope("hubble")
        state.togglePassScope("tiangong")
        assertEquals(setOf("hubble", "tiangong"), state.ui.passScope)
        state.togglePassScope("hubble")
        assertEquals(setOf("tiangong"), state.ui.passScope)
        state.togglePassScope("")
        assertEquals("the empty id is the «همه» chip", emptySet<String>(), state.ui.passScope)

        state.togglePassScope("hubble")
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saved = with(SatellitesState.Saver) { scope.save(state) }
        assertNotNull(saved)
        val restored = SatellitesState.Saver.restore(saved!!)
        assertNotNull(restored)
        assertEquals(setOf("hubble"), restored!!.ui.passScope)
        assertEquals("the globe filters keep their own slot", state.ui.globeFilters, restored.ui.globeFilters)
        assertEquals("focus, scope, offset, theme, preview, globe filters, pass scope", 7, saved.size)
    }

    @Test
    fun theBottomIsTwoSeparateCardsWithSeparateJobs() {
        val components = readSource("com/alijafari/red/astronomy/ui/satellites/SatelliteComponents.kt")
        val stage = readSource("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")

        val focusCard = components.substringAfter("fun SatFocusCard(").substringBefore("fun SatNextPassCard(")
        val passCard = components.substringAfter("fun SatNextPassCard(").substringBefore("fun SatPassScopePicker(")

        assertTrue("the satellite card keeps its own pass row", focusCard.contains("\"sat_focus_passes\""))
        assertFalse("the agenda no longer hangs off the satellite card", focusCard.contains("sat_more_passes_button"))
        assertFalse(focusCard.contains("sat_all_passes"))

        assertTrue("the pass card is its own surface", passCard.contains("testTag(\"sat_pass_card\")"))
        assertTrue("tapping the pass opens its page", passCard.contains("onOpenPass"))
        assertTrue("the card carries the way into the agenda", passCard.contains("sat_more_passes_button"))
        assertTrue(passCard.contains("testTag = \"sat_all_passes\""))

        assertTrue("both cards are stacked under the globe", stage.contains("SatFocusCard(presented"))
        assertTrue(stage.contains("SatNextPassCard(next = nextAnyPass"))
        assertTrue("the pass card only knows the next pass in scope", stage.contains("nextPassInScope(it, state.passScope"))
        assertTrue("more passes opens the agenda", stage.contains("onMorePasses = { screen.panel(SatPanel.Passes) }"))
        assertTrue("the two cards reserve their own measured height", stage.contains("readoutHeight"))
    }

    @Test
    fun theAgendaIsAWeekLongAndFiltersBySatellite() {
        val components = readSource("com/alijafari/red/astronomy/ui/satellites/SatelliteComponents.kt")
        val stage = readSource("com/alijafari/red/astronomy/ui/satellites/SatellitesStage.kt")
        val sheet = components.substringAfter("fun SatPassesSheet(").substringBefore("fun SatNightSection(")

        assertTrue(stage.contains("AGENDA_WINDOW_DAYS * DAY"))
        assertTrue(stage.contains("AGENDA_SCAN_DAYS"))
        assertTrue("the agenda obeys the same scope as the card", stage.contains("passScopeMatches(row.sat, state.passScope)"))
        assertTrue("the sheet's title says seven days", sheet.contains("R.string.sat_seven_days"))
        assertTrue("the sheet carries the satellite filter", sheet.contains("SatPassScopePicker(scopeChoices, scopeIds"))
        assertTrue(sheet.contains("R.string.sat_pass_scope_row"))
        assertTrue("an empty week offers the way back to every satellite", sheet.contains("SatPassEmpty(tokens, isFa, scopeLabel"))
    }

    @Test
    fun bothLocalesSayTheSameThingForTheNewCopy() {
        val english = readRes("values/satellites.xml")
        val persian = readRes("values-fa/satellites.xml")
        val keys = listOf(
            "sat_seven_days", "sat_pass_card_title", "sat_pass_card_empty", "sat_pass_in",
            "sat_pass_scope_all", "sat_pass_scope_count", "sat_pass_scope_row",
            "sat_starlink_family", "sat_more_passes_button",
        )
        for (key in keys) {
            assertTrue("missing English $key", english.contains("name=\"$key\""))
            assertTrue("missing Persian $key", persian.contains("name=\"$key\""))
        }
        assertTrue("the placeholder survives the translation", persian.contains("<string name=\"sat_pass_in\">%1\$s دیگر</string>"))
        assertTrue(persian.contains("<string name=\"sat_pass_scope_count\">%1\$s ماهواره</string>"))
        assertFalse("the retired detail-view hint is gone", persian.contains("name=\"sat_seven_day\""))
    }
}
