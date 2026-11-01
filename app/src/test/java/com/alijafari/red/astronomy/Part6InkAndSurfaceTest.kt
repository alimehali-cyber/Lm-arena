package com.alijafari.red.astronomy

import androidx.compose.ui.graphics.Color
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.theme.appBackdropLuminance
import com.alijafari.red.astronomy.ui.theme.backdropInkFor
import com.alijafari.red.astronomy.ui.theme.liveSkyLuminance
import com.alijafari.red.astronomy.ui.theme.readableInkOn
import com.alijafari.red.astronomy.ui.theme.surfaceInkFor
import com.alijafari.red.astronomy.ui.theme.wcagContrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The measurable half of Part 6: the numbers the eye is supposed to see.
 *
 * Source-shape guards live in [Part6ScreenChromeTest]; what is checked here is the arithmetic behind
 * them — the ink contrast that makes a bare title readable on any backdrop, and the ink every card picks
 * against the fill it actually paints. All of it is pure Kotlin, so none of it needs a device.
 */
class Part6InkAndSurfaceTest {

    // ---------------------------------------------------------------------------------------------
    // Item 1 — the ink that adapts to the backdrop
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theInkIsChosenByMeasuredContrast() {
        fun grey(l: Float) = Color(l, l, l, 1f)

        listOf(0f, 0.02f, 0.1f).forEach { l ->
            val ink = backdropInkFor(l)
            assertFalse("a dark backdrop ($l) takes light ink", ink.isDarkInk)
            assertTrue(
                "and it clears 4.5:1 (${wcagContrast(ink.primary, grey(l))})",
                wcagContrast(ink.primary, grey(l)) >= 4.5f
            )
        }
        listOf(0.85f, 0.95f, 1f).forEach { l ->
            val ink = backdropInkFor(l)
            assertTrue("a bright backdrop ($l) takes dark ink", ink.isDarkInk)
            assertTrue(
                "and it clears 4.5:1 (${wcagContrast(ink.primary, grey(l))})",
                wcagContrast(ink.primary, grey(l)) >= 4.5f
            )
        }
        // The crossover is where the two inks actually swap — about 0.5 in sRGB grey, which is 0.18 in
        // linear luminance — and not a hand-picked threshold. At every brightness, including both sides of
        // it, the ink that was chosen is the one with the higher measured ratio of the two.
        //
        // The two inks the decision is made between, exactly as `BackdropInk` defines them: a near-black
        // and a near-white, not the extremes — which is why the chosen ink's ratio is not simply the
        // theoretical maximum of pure black or pure white.
        val darkInk = Color(8f / 255f, 10f / 255f, 18f / 255f, 1f)
        val lightInk = Color(0xF8 / 255f, 0xFA / 255f, 1f, 1f)
        listOf(0.02f, 0.2f, 0.35f, 0.4f, 0.5f, 0.6f, 0.8f, 0.98f).forEach { l ->
            val ink = backdropInkFor(l)
            val backdrop = grey(l)
            val chosen = wcagContrast(ink.primary, backdrop)
            val bestOfTheTwo = maxOf(
                wcagContrast(lightInk, backdrop),
                wcagContrast(darkInk, backdrop)
            )
            assertTrue(
                "at $l the chosen ink is the better of the two ($chosen vs $bestOfTheTwo)",
                chosen >= bestOfTheTwo - 0.02f
            )
            assertEquals(
                "and it says which one it is",
                ink.isDarkInk,
                wcagContrast(darkInk, backdrop) >= wcagContrast(lightInk, backdrop)
            )
        }
    }

    @Test
    fun theHaloOnlyAppearsWhereItIsNeeded() {
        assertEquals("no halo on a black backdrop", 0f, backdropInkFor(0f).halo.alpha, 0.001f)
        assertEquals("none on a white one", 0f, backdropInkFor(1f).halo.alpha, 0.001f)
        // Dusk is the ambiguous middle: a mid grey — about 0.5 in sRGB, where white and black are equally
        // poor — is where the halo is strongest, and it is already fading at 0.4.
        assertTrue("but there is one at dusk", backdropInkFor(0.5f).halo.alpha > 0.15f)
        assertTrue(
            "and it fades as the winner gets comfortable",
            backdropInkFor(0.5f).halo.alpha > backdropInkFor(0.4f).halo.alpha &&
                backdropInkFor(0.4f).halo.alpha > backdropInkFor(0.2f).halo.alpha
        )
        // The halo is the ink that lost: light text gets a dark shadow and dark text a light one, which is
        // the only direction a shadow can help in.
        val beforeTheCrossover = backdropInkFor(0.45f)
        assertFalse("white ink wins just below the crossover", beforeTheCrossover.isDarkInk)
        assertTrue("so its halo is real", beforeTheCrossover.halo.alpha > 0.1f)
        assertEquals("and black", 0f, beforeTheCrossover.halo.red, 0.01f)
        val afterTheCrossover = backdropInkFor(0.55f)
        assertTrue("dark ink wins just above it", afterTheCrossover.isDarkInk)
        assertTrue("and its halo is white", afterTheCrossover.halo.red > 0.99f)
    }

    @Test
    fun theBackdropBrightnessComesFromWhatTheBackdropIs() {
        // The live sky: night is dark, daylight is the bright case, twilight is in between.
        val night = liveSkyLuminance(-30.0, SkyCanvasTheme.REAL_SKY)
        val civil = liveSkyLuminance(-3.0, SkyCanvasTheme.REAL_SKY)
        val day = liveSkyLuminance(30.0, SkyCanvasTheme.REAL_SKY)
        assertTrue("night < twilight < day", night < civil && civil < day)
        assertFalse("night is read with light ink", backdropInkFor(night).isDarkInk)
        assertTrue("daylight is read with dark ink", backdropInkFor(day).isDarkInk)

        // The photographs are measured, and the measurement is used.
        val ngc = appBackdropLuminance(AppBackdropMode.NGC_1929, Color.Black, 40.0)
        val cepheus = appBackdropLuminance(AppBackdropMode.CEPHEUS, Color.Black, 40.0)
        assertEquals("NGC 1929's measured mean", 0.162f, ngc, 0.0001f)
        assertEquals("Cepheus' measured mean", 0.202f, cepheus, 0.0001f)
        assertFalse("both are dark pictures", backdropInkFor(ngc).isDarkInk)

        // With no backdrop the theme's own background is the backdrop.
        val light = appBackdropLuminance(AppBackdropMode.NONE, Color(0.97f, 0.97f, 0.98f, 1f), 0.0)
        assertTrue("a light theme is read with dark ink", backdropInkFor(light).isDarkInk)
    }

    // ---------------------------------------------------------------------------------------------
    // Every card's ink: chosen against the fill the card actually paints
    // ---------------------------------------------------------------------------------------------

    @Test
    fun everyCardPicksItsInkAgainstItsOwnFill() {
        // A card hands in a base colour and, sometimes, a translucent scrim over it. The decisively dark
        // and decisively bright fills are the ones a card actually uses — the theme's own tokens — and on
        // those the resolved ink has to clear AA.
        val dark = listOf(
            Color(0f, 0f, 0f, 1f),
            Color(0.05f, 0.06f, 0.11f, 1f),
            Color(0.08f, 0.09f, 0.14f, 1f)
        )
        val bright = listOf(
            Color(0.94f, 0.93f, 0.90f, 1f),
            Color(0.97f, 0.97f, 0.98f, 1f),
            Color(1f, 1f, 1f, 1f)
        )
        (dark + bright).forEach { fill ->
            val ink = surfaceInkFor(fill)
            val ratio = wcagContrast(ink.primary, fill)
            assertTrue("ink on $fill clears 4.5:1 ($ratio)", ratio >= 4.5f)
            assertEquals(
                "the secondary ink is the same hue, weaker",
                ink.primary.red,
                ink.secondary.red,
                0.001f
            )
            assertTrue(
                "and it fades: primary then secondary then tertiary",
                ink.primary.alpha >= ink.secondary.alpha && ink.secondary.alpha >= ink.tertiary.alpha
            )
        }
        assertTrue(
            "a dark fill takes the light ink",
            surfaceInkFor(Color(0.05f, 0.06f, 0.11f, 1f)).primary.blue > 0.9f
        )
        assertTrue(
            "a bright fill takes the dark ink",
            surfaceInkFor(Color(0.97f, 0.97f, 0.98f, 1f)).primary.blue < 0.1f
        )

        // The ambiguous middle is not a card colour, but the rule still has to be the better of the two
        // inks — never the worse one. (The measured crossover itself is pinned in
        // `theInkIsChosenByMeasuredContrast`.)
        val midGrey = Color(0.5f, 0.5f, 0.5f, 1f)
        val chosen = surfaceInkFor(midGrey).primary
        val lightInk = Color(0xFFF8FAFF)
        val darkInk = Color(0xFF080A12)
        assertTrue("it is still one of the two inks", chosen == lightInk || chosen == darkInk)
        assertTrue(
            "and it is legible even there (${wcagContrast(chosen, midGrey)}:1)",
            wcagContrast(chosen, midGrey) >= 3f
        )
    }

    @Test
    fun aScrimIsCompositedBeforeTheInkIsChosen() {
        // The sky buttons lay a translucent scrim over a base colour. The ink has to be chosen against
        // what the eye sees — the composite — not against the base colour underneath it.
        val base = Color(0f, 0f, 0f, 1f)
        val whiteScrim = Color(1f, 1f, 1f, 0.9f)
        assertEquals(
            "a heavy white scrim over black reads as light, so the ink is dark",
            Color(0xFF080A12),
            surfaceInkFor(base, whiteScrim).primary
        )
        assertEquals(
            "and black under nothing stays light ink",
            Color(0xFFF8FAFF),
            surfaceInkFor(base).primary
        )
        assertEquals(
            "zero alpha is not a scrim at all",
            surfaceInkFor(base).primary,
            surfaceInkFor(base, Color.Transparent).primary
        )
    }

    @Test
    fun theInkIsOneOfTwoPinnedColours() {
        // Readable ink is a decision, not a gradient: near-black or near-white, both of which clear
        // AA on the surface they were chosen for. Nothing in the app paints a mid grey text colour.
        val dark = Color(0xFF080A12)
        val light = Color(0xFFF8FAFF)
        listOf(0f, 0.2f, 0.45f, 0.55f, 0.8f, 1f).forEach { level ->
            val ink = readableInkOn(Color(level, level, level, 1f))
            assertTrue("$level resolves to one of the two inks", ink == dark || ink == light)
        }
        assertEquals("black takes light ink", light, readableInkOn(Color.Black))
        assertEquals("white takes dark ink", dark, readableInkOn(Color.White))
    }

    // ---------------------------------------------------------------------------------------------
    // The radii the two Home cards use, and the fire's brightness — pure constants, so pinned here.
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theCardTextShiftsAreTheDistancesThatWereAskedFor() {
        // 1 mm is 6.299 dp at the 160 dpi baseline the design system uses.
        assertEquals("8 mm to the left", 50.4f, com.alijafari.red.astronomy.ui.screens.HomeTextShift.eclipseCard.value, 0.05f)
        assertEquals("1.5 cm to the left", 94.5f, com.alijafari.red.astronomy.ui.screens.HomeTextShift.moonCard.value, 0.05f)
    }
}
