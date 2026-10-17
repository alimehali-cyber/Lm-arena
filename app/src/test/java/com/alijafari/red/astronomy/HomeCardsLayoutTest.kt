package com.alijafari.red.astronomy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Structural guards for the Home card board: the big tonight hero just under the location
 * selector, the Moon and sky tiles, and one picture tile per eclipse family — with no leftover
 * section text standing outside the cards.
 */
class HomeCardsLayoutTest {

    @Test
    fun theTonightHeroSitsJustUnderTheLocationSelector() {
        val home = home()
        assertTrue("the tonight hero card must exist", home.contains("testTag(\"home_tonight_card\")"))
        assertTrue(
            "the tonight hero must be composed after the header/location item and before the moon row",
            home.indexOf("testTag(\"home_location_selector_button\")") < home.indexOf("testTag(\"home_tonight_card\")") &&
                home.indexOf("testTag(\"home_tonight_card\")") < home.indexOf("testTag(\"home_moon_card\")")
        )
    }

    @Test
    fun theEclipseFamiliesGetOnePictureTileEach() {
        val home = home()
        assertTrue("the solar eclipse tile must exist", home.contains("testTag(\"home_solar_eclipse_card\")"))
        assertTrue("the lunar eclipse tile must exist", home.contains("testTag(\"home_lunar_eclipse_card\")"))
        assertTrue("the solar tile draws its own picture", home.contains("fun SolarEclipseArtwork("))
        assertTrue("the lunar tile draws its own picture", home.contains("fun LunarEclipseArtwork("))
        assertTrue("the tonight hero draws its rows", home.contains("fun TonightHeroCard("))
    }

    @Test
    fun noSectionTextStandsOutsideTheCards() {
        // The "Tonight's best" and "Upcoming Eclipses" section headers are gone; every remaining
        // text on the feed lives inside a card or the header chrome.
        assertFalse("no section headers on Home", home().contains("RedSectionHeader("))
    }

    private fun home(): String =
        readMain("java/com/alijafari/red/astronomy/ui/screens/HomeScreen.kt")

    private fun readMain(relative: String): String {
        val mainDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "app/src/main") }
            .first { it.isDirectory }
        return File(mainDir, relative).readText()
    }
}
