package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level guards for the Home hero card, which is now **one wide card** under the sky.
 *
 * The card grew sideways into the width the Moon tile and the eclipse tile used to share, and downward by
 * exactly the eclipse strip that moved into it. What must not move or change is the Moon: the same square
 * as the old Moon card, on the same side of the screen, with the render inset by the same 8 dp, so the disc
 * is drawn in the same place at the same size. These assertions hold that shape of the code, because a
 * screenshot test cannot: the Home screen is a LazyColumn over a GL landscape.
 */
class HomeMoonCardLayoutTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private val home by lazy { source("com/alijafari/red/astronomy/ui/screens/HomeScreen.kt") }
    private val hero by lazy { source("com/alijafari/red/astronomy/ui/components/HeroSkyCanvas.kt") }

    @Test
    fun theMoonKeepsItsOwnSquareSoItsRenderCannotChange() {
        // The card's geometry is derived from the Moon's old square, and the Moon is laid out in it.
        assertTrue("the card's geometry is one measured value", home.contains("private data class HeroCardMetrics("))
        assertTrue("the Moon column is exactly the old square wide", home.contains(".width(card.moonSquare)"))
        assertTrue("the Moon is laid out in a square of that size", home.contains(".height(card.moonSquare)"))
        assertTrue(
            "the card is the Moon square plus the gap plus the eclipse strip",
            home.contains("height = square + gap + strip")
        )
        // The render itself is untouched: full size inside the same 8 dp inset.
        assertTrue(
            "the Moon render still fills its square",
            home.contains("MoonPhaseRender(") && home.contains("modifier = Modifier.fillMaxSize()")
        )
        assertTrue("the Moon keeps drag-to-scrub", home.contains("detectHorizontalDragGestures {"))
        assertTrue("the Moon still opens the Moon window", home.contains("viewModel.setShowMoonWindow(true)"))
    }

    @Test
    fun theEclipseSitsAtTheBottomOfTheMoonCardAndStillOpensTheEclipses() {
        assertTrue("the eclipse is inside the card", home.contains("EclipseCard("))
        assertTrue("the eclipse keeps its test tag", home.contains(".testTag(\"home_eclipse_card\")"))
        assertTrue(
            "the eclipse opens the eclipses screen",
            home.contains("onOpenEclipses") && home.contains("viewModel.setShowEclipseWindow(true)")
        )
        // It is rendered square: EclipseRender masks and crops from minDimension, so a wide box would cut
        // the corona away.
        assertTrue("the eclipse is drawn in a square", home.contains(".aspectRatio(1f)"))
    }

    @Test
    fun nothingIsWrittenOnTheSkyAnyMoreAndTheReadoutLivesOnTheCard() {
        assertFalse(
            "the canvas no longer draws the scene readout",
            hero.contains("home_sky_readout")
        )
        assertFalse(
            "the canvas' readout block is gone",
            hero.contains("// --- SCENE READOUT ---")
        )
        assertFalse(
            "the canvas no longer labels the twilight band",
            hero.contains("skyConditionLabel")
        )

        assertTrue("the readout is on the Moon card", home.contains(".testTag(\"home_moon_card_readout\")"))
        assertTrue("it still shows the clock", home.contains("TimeEngine.formatTime24h("))
        assertTrue("it still shows the date", home.contains("TimeEngine.formatDate("))
        assertTrue("it still shows the sky's own state", home.contains("private fun skyConditionLabel("))
        assertTrue("it still shows the Moon", home.contains("Moon below horizon"))
        assertTrue(
            "it follows the canvas' time scrub",
            home.contains("viewModel.skyDragOffsetHours.collectAsState()") &&
                home.contains("SkyTimeModel.effectiveJd(instantMs, dragOffsetHours)")
        )
    }

    @Test
    fun theLocationPillShowsOnlyTheCityOrLiveGps() {
        assertTrue("the pill is still the way into the location window", home.contains(".testTag(\"home_location_selector_button\")"))
        assertTrue("a city shows its own name", home.contains("cityName"))
        assertTrue("a device fix shows Live GPS", home.contains("\"Live GPS\""))
        assertTrue("and its Persian equivalent", home.contains("\"GPS زنده\""))
        assertTrue("the source of the location is state, not a guess", home.contains("uiState.isLiveGpsLocation"))
        assertFalse("the coordinates are gone from the pill", home.contains("coordsText"))
    }

    @Test
    fun theLocationSourceFlagIsSetOnlyByTheDeviceFix() {
        val viewModel = source("com/alijafari/red/astronomy/ui/MainViewModel.kt")
        assertTrue("the state carries the flag", viewModel.contains("val isLiveGpsLocation: Boolean = false"))
        assertTrue("it is persisted", viewModel.contains("\"user_location_is_live_gps\""))
        assertTrue("it survives a restart", viewModel.contains("prefs.getBoolean(\"user_location_is_live_gps\", false)"))
        assertTrue("a device fix sets it", viewModel.contains("fromLiveGps = true"))
        assertTrue("a picked city clears it", viewModel.contains("isLiveGpsLocation = fromLiveGps"))
    }
}
