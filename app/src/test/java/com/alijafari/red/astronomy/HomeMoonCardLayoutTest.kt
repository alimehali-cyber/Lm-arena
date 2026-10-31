package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level guards for the Home hero: **two** cards under the sky, both extended horizontally, neither
 * changing the picture inside it.
 *
 * The Moon card grew sideways first and the eclipse card followed it — its own glass card, directly beneath
 * the Moon's, the plate pinned to the card's far left and that eclipse's circumstances to its right. What
 * must not move or change is the Moon, and what must not be stretched is the eclipse plate. These
 * assertions hold that shape of the code, because a screenshot test cannot: the Home screen is a
 * LazyColumn over a GL landscape.
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
        assertTrue("the Moon column is exactly the old square wide", home.contains(".width(moonSquare)"))
        assertTrue("the render still fills that square", home.contains("MoonPhaseRender("))
        assertTrue("the Moon keeps drag-to-scrub", home.contains("detectHorizontalDragGestures {"))
        assertTrue("the Moon still opens the Moon window", home.contains("viewModel.setShowMoonWindow(true)"))
        assertTrue("the Moon has its own card", home.contains(".testTag(\"home_moon_card\")"))
    }

    @Test
    fun theEclipseHasItsOwnCardUnderTheMoon() {
        assertTrue("the eclipse is its own glass card", home.contains("private fun EclipseCard("))
        assertTrue("the eclipse card keeps its test tag", home.contains(".testTag(\"home_eclipse_card\")"))
        assertTrue("its plate keeps its test tag", home.contains(".testTag(\"home_eclipse_card_render\")"))
        assertTrue("it is a separate LiquidGlassSurface", home.contains("private fun MoonCard("))
        // The eclipse card is placed below the Moon card, not inside it.
        assertTrue("the eclipse card is anchored under the Moon", home.contains("padding(bottom = eclipseCardBottom)"))
        assertTrue("the Moon card sits above it", home.contains("padding(bottom = moonCardBottom)"))
        assertTrue(
            "both cards are the same width",
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
