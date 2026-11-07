package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level guards for the Home hero: **two** cards under the sky, the same width, the same height, and
 * the same size of picture inside each.
 *
 * The Moon card grew sideways first and the eclipse card followed it — its own card, directly beneath
 * the Moon's, the plate pinned to the card's far left and that eclipse's circumstances to its right. The pair
 * used to be a tall Moon square over a thin eclipse band; the Moon has since come down and the eclipse grown
 * up until the two pictures are drawn in equal squares. These assertions hold that shape of the code,
 * because a screenshot test cannot: the Home screen is a LazyColumn over a GL landscape.
 */
class HomeMoonCardLayoutTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private val home by lazy { source("com/alijafari/red/astronomy/ui/screens/HomeScreen.kt") }
    private val hero by lazy { source("com/alijafari/red/astronomy/ui/components/HeroSkyCanvas.kt") }
    private val render by lazy { source("com/alijafari/red/astronomy/ui/components/EclipseRender.kt") }

    @Test
    fun theMoonKeepsItsOwnSquareSoItsRenderCannotChange() {
        assertTrue("the geometry is one measured value", home.contains("private data class HeroCardMetrics("))
        assertTrue("the Moon column is exactly the shared square wide", home.contains(".width(renderSquare)"))
        assertTrue("the render still fills that square", home.contains("MoonPhaseRender("))
        assertTrue("the Moon keeps drag-to-scrub", home.contains("detectHorizontalDragGestures {"))
        assertTrue("the Moon still opens the Moon window", home.contains("viewModel.setShowMoonWindow(true)"))
        assertTrue("the Moon has its own card", home.contains(".testTag(\"home_moon_card\")"))
    }

    @Test
    fun theTwoCardsAndTheirRendersAreTheSameSize() {
        // One height for both cards: the pair used to be a Moon square over an eclipse strip, and the two
        // ranges are now one value. The two heights are aliases of it, so they cannot drift apart.
        assertTrue("the cards share one height", home.contains("val cardHeight: Dp"))
        assertTrue("the Moon card is that height", home.contains("val moonCardHeight: Dp get() = cardHeight"))
        assertTrue("the eclipse card is that height", home.contains("val eclipseCardHeight: Dp get() = cardHeight"))
        assertTrue("the stack is two of that height", home.contains("val stackHeight: Dp get() = cardHeight * 2 + gap"))
        assertFalse("the old Moon square is gone", home.contains("moonSquare"))
        assertFalse("the old eclipse strip is gone", home.contains("eclipseStrip"))
        // One square for both pictures: the Moon is laid out in it and the eclipse plate is inscribed in it.
        assertTrue("one square size for both pictures", home.contains("val renderSquare: Dp get() = cardHeight"))
        assertTrue("the Moon card hands that square to the Moon", home.contains("renderSquare = card.renderSquare,"))
        assertTrue("the eclipse plate fills the same height", home.contains(".fillMaxHeight()\n                        .aspectRatio(1f)"))
        // The size is *between* the two old sizes: the Moon shrinks, the eclipse grows.
        assertTrue("the shared height is derived from the old square", home.contains("(square * 0.75f)"))
    }

    @Test
    fun theEclipseHasItsOwnCardUnderTheMoon() {
        assertTrue("the eclipse is its own card", home.contains("private fun EclipseCard("))
        assertTrue("the eclipse card keeps its test tag", home.contains(".testTag(\"home_eclipse_card\")"))
        assertTrue("its plate keeps its test tag", home.contains(".testTag(\"home_eclipse_card_render\")"))
        assertTrue("the Moon is a card of its own", home.contains("private fun MoonCard("))
        // The eclipse card is placed below the Moon card, not inside it.
        assertTrue("the eclipse card is anchored under the Moon", home.contains("padding(bottom = eclipseCardBottom)"))
        assertTrue("the Moon card sits above it", home.contains("padding(bottom = moonCardBottom)"))
        assertTrue(
            "both cards are the same width and the same height",
            home.contains(".width(card.width)\n                .height(card.moonCardHeight)") &&
                home.contains(".width(card.width)\n                .height(card.eclipseCardHeight)")
        )
        assertTrue("tapping the eclipse opens the Eclipses window", home.contains("onOpenEclipses"))
    }

    @Test
    fun thePlateStaysAtTheFarLeftAndTheDetailsFillTheRight() {
        // Composed left-to-right on purpose, so the plate is at the physical left edge in either language.
        assertTrue(
            "the eclipse row is pinned left-to-right",
            home.contains("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)")
        )
        assertTrue(
            "the details restore the app's own direction",
            home.contains("CompositionLocalProvider(LocalLayoutDirection provides appDirection)")
        )
        assertTrue("the details are a card section of their own", home.contains(".testTag(\"home_eclipse_card_details\")"))
        assertTrue("the details name the eclipse", home.contains("if (isFa) eclipse.localNameFa else eclipse.localNameEn"))
        assertTrue("the details date it and count down", home.contains("· \$countdown"))
        assertTrue("the details give the local peak time", home.contains("TimeEngine.formatTime24h(eclipse.localPeakTimeMs"))
        assertTrue("the details say how much is covered", home.contains("localObscurationPercent"))
        assertTrue("the details grade the view", home.contains("eclipse.visibility.grade"))
        // The plate must not be stretched by the card being wide.
        assertTrue("the plate keeps its aspect ratio", home.contains(".aspectRatio(1f)"))
        assertFalse("the plate is never stretched to the card width", home.contains(".fillMaxWidth()\n                        .aspectRatio"))
    }

    @Test
    fun thePlateIsTheEclipseTheObserverActuallySees() {
        assertTrue(
            "the card draws the LOCAL type, not the catalogue type",
            home.contains("val type = eclipse?.localType ?: EclipseEngine.EclipseType.TOTAL_SOLAR")
        )
        assertFalse("the catalogue type is never used for the Home plate", home.contains("type = eclipse.event.type"))
    }

    @Test
    fun theRenderIsCrispAndElevated() {
        assertFalse("the DstIn softening mask is gone", render.contains("BlendMode.DstIn"))
        assertFalse("the plate is no longer dimmed per type", render.contains("plateAlpha"))
        assertTrue("the plate is clipped to a hard circle", render.contains(".clip(CircleShape)"))
        assertTrue("the plate is lifted off the card", render.contains(".shadow("))
        assertTrue("the elevation is a parameter", render.contains("elevationDp: Dp = 10.dp"))
        assertTrue("there is a plate for the penumbral lunar eclipse", render.contains("eclipse_lunar_penumbral"))
        assertTrue(
            "penumbral gets the plate that matches it",
            render.contains(
                "EclipseEngine.EclipseType.PENUMBRAL_LUNAR -> R.drawable.eclipse_lunar_penumbral"
            )
        )
    }

    @Test
    fun nothingIsWrittenOnTheSkyAnyMoreAndTheReadoutLivesOnTheMoonCard() {
        assertFalse("the canvas no longer draws the scene readout", hero.contains("home_sky_readout"))
        assertFalse("the canvas' readout block is gone", hero.contains("// --- SCENE READOUT ---"))
        assertFalse("the canvas no longer labels the twilight band", hero.contains("skyConditionLabel"))

        assertTrue("the readout is on the Moon card", home.contains(".testTag(\"home_moon_card_readout\")"))
        assertTrue("it follows the canvas' time scrub", home.contains("viewModel.skyDragOffsetHours.collectAsState()"))
    }

    @Test
    fun theLocationPillShowsOnlyTheCityOrLiveGps() {
        assertTrue("the pill is still the way into the location window", home.contains(".testTag(\"home_location_selector_button\")"))
        assertTrue("a device fix shows Live GPS", home.contains("\"Live GPS\""))
        assertTrue("and its Persian equivalent", home.contains("\"GPS به‌روز\""))
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
