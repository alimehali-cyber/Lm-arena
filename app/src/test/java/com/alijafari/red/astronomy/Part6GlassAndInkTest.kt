package com.alijafari.red.astronomy

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import com.alijafari.red.astronomy.ui.backdrop.AppBackdropMode
import com.alijafari.red.astronomy.ui.theme.appBackdropLuminance
import com.alijafari.red.astronomy.ui.theme.backdropInkFor
import com.alijafari.red.astronomy.ui.theme.glassFilmColor
import com.alijafari.red.astronomy.ui.theme.hsvColor
import com.alijafari.red.astronomy.ui.theme.liveSkyLuminance
import com.alijafari.red.astronomy.ui.theme.mixHue
import com.alijafari.red.astronomy.ui.theme.wcagContrast
import com.alijafari.red.astronomy.ui.theme.CornerSmoothness
import com.alijafari.red.astronomy.ui.theme.GlassFilmPreset
import com.alijafari.red.astronomy.ui.theme.GlassShaderProfile
import com.alijafari.red.astronomy.ui.theme.GlassSheen
import com.alijafari.red.astronomy.ui.theme.LiquidGlassConfig
import com.alijafari.red.astronomy.ui.theme.SmoothCornerShape
import com.alijafari.red.astronomy.ui.theme.matches
import com.alijafari.red.astronomy.ui.theme.squircleShape
import com.alijafari.red.astronomy.ui.theme.withCornerSmoothing
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The measurable half of Part 6: the numbers the eye is supposed to see.
 *
 * Source-shape guards live in [Part6ScreenChromeTest]; what is checked here is the arithmetic behind
 * them — the ink contrast that makes a bare title readable on any backdrop, the colour mixing that makes
 * the glass film, the geometry of the smooth-edge dial, and the fire's brightness gain. All of it is pure
 * Kotlin, so none of it needs a device.
 */
class Part6GlassAndInkTest {

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
        val nearBlack = Color(8f / 255f, 10f / 255f, 18f / 255f, 1f)
        listOf(0.02f, 0.2f, 0.35f, 0.4f, 0.5f, 0.6f, 0.8f, 0.98f).forEach { l ->
            val ink = backdropInkFor(l)
            val backdrop = grey(l)
            val chosen = wcagContrast(ink.primary, backdrop)
            val bestOfTheTwo = maxOf(
                wcagContrast(Color.White, backdrop),
                wcagContrast(nearBlack, backdrop)
            )
            assertTrue(
                "at $l the chosen ink is the better of the two ($chosen vs $bestOfTheTwo)",
                chosen >= bestOfTheTwo - 0.02f
            )
            assertEquals(
                "and it says which one it is",
                ink.isDarkInk,
                wcagContrast(nearBlack, backdrop) >= wcagContrast(Color.White, backdrop)
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
        assertTrue("and it is the opposite ink", backdropInkFor(0.05f).halo.red > 0.5f)
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
    // Item 7 — colour mixing, shader design, smooth edges
    // ---------------------------------------------------------------------------------------------

    @Test
    fun theFilmIsATwoColourRecipe() {
        // Saturation zero is the shipped neutral white film, whatever the hues are.
        val neutral = LiquidGlassConfig(tintAlpha = 0.2f, tintSaturation = 0f)
        val white = glassFilmColor(neutral)
        assertTrue("neutral film is white", white.red > 0.99f && white.green > 0.99f && white.blue > 0.99f)
        assertEquals("at the tint's alpha", 0.2f, white.alpha, 0.001f)

        // Hue only matters once there is saturation, and then it is the mixed hue that shows.
        val teal = LiquidGlassConfig(
            tintAlpha = 0.3f,
            tintSaturation = 1f,
            tintHueA = 180f,
            tintHueB = 180f,
            tintMix = 0f
        )
        val tealColor = glassFilmColor(teal)
        assertTrue("cyan has no red", tealColor.red < 0.05f)
        assertTrue("and full green and blue", tealColor.green > 0.9f && tealColor.blue > 0.9f)

        // Zero alpha means no film at all, so the caller can skip the draw.
        assertEquals(Color.Transparent, glassFilmColor(LiquidGlassConfig(tintAlpha = 0f)))
    }

    @Test
    fun theMixTakesTheShorterWayRoundTheWheel() {
        assertEquals("halfway from 350 to 10 is 0, through red", 0f, mixHue(350f, 10f, 0.5f), 0.001f)
        assertEquals("and not the long way", 0f, mixHue(350f, 10f, 0.5f), 0.001f)
        assertEquals("ends are exact", 350f, mixHue(350f, 10f, 0f), 0.001f)
        assertEquals("at the other end", 10f, mixHue(350f, 10f, 1f), 0.001f)
        assertEquals("hue wraps down", 300f, mixHue(300f, -60f, 0f), 0.001f)

        // Every preset is reachable, i.e. the chip can light up after it is tapped.
        GlassFilmPreset.entries.forEach { preset ->
            val config = LiquidGlassConfig(
                tintHueA = preset.hueA,
                tintHueB = preset.hueB,
                tintMix = preset.mix,
                tintSaturation = preset.saturation,
                tintAlpha = preset.alpha
            )
            assertTrue("${preset.name} matches itself", config.matches(preset))
            assertFalse(
                "${preset.name} does not match a different alpha",
                config.copy(tintAlpha = (preset.alpha + 0.05f).coerceAtMost(0.9f)).matches(preset)
            )
        }
    }

    @Test
    fun hsvIsTheRealConversion() {
        val red = hsvColor(0f, 1f, 1f)
        assertTrue("hue 0 is red", red.red > 0.99f && red.green < 0.01f && red.blue < 0.01f)
        val green = hsvColor(120f, 1f, 1f)
        assertTrue("hue 120 is green", green.green > 0.99f && green.red < 0.01f)
        val blue = hsvColor(240f, 1f, 1f)
        assertTrue("hue 240 is blue", blue.blue > 0.99f && blue.red < 0.01f)
        val grey = hsvColor(200f, 0f, 0.5f)
        assertEquals("zero saturation is neutral", grey.red, grey.blue, 0.001f)
    }

    @Test
    fun everyShaderAndSheenOptionIsARealSetting() {
        assertEquals("three refraction recipes", 3, GlassShaderProfile.entries.size)
        assertTrue(
            "a crystal profile diffuses less and bends more than a frosted one",
            GlassShaderProfile.CRYSTAL.blurScale < GlassShaderProfile.FROSTED.blurScale &&
                GlassShaderProfile.CRYSTAL.refractionScale > GlassShaderProfile.FROSTED.refractionScale
        )
        assertEquals("four places for the light", 4, GlassSheen.entries.size)
        assertTrue("including none", GlassSheen.NONE in GlassSheen.entries)
        assertEquals("and the default is the shipped one", GlassSheen.TOP, LiquidGlassConfig().sheen)
        assertEquals(
            "the default shader recipe is the shipped balance",
            GlassShaderProfile.BALANCED,
            LiquidGlassConfig().shaderProfile
        )
    }

    @Test
    fun theSmoothEdgeDialRunsFromACircleToTheAppsSquircle() {
        assertEquals("circular end: n = 2", 2f, CornerSmoothness.exponent(0f), 0.0001f)
        assertEquals("circular end: reach 1.0 r", 1f, CornerSmoothness.extent(0f), 0.0001f)
        assertEquals("continuous end: n = 4", 4f, CornerSmoothness.exponent(1f), 0.0001f)
        assertEquals("continuous end: reach 1.6 r", 1.6f, CornerSmoothness.extent(1f), 0.0001f)
        assertTrue("the dial is monotone", CornerSmoothness.exponent(0.25f) < CornerSmoothness.exponent(0.75f))
        assertTrue("and the default is the app's own corner", CornerSmoothness.isDefault(CornerSmoothness.DEFAULT))
        assertFalse("anything else is not", CornerSmoothness.isDefault(0.5f))
    }

    @Test
    fun theCircularEndIsActuallyACircle() {
        // At smoothness 0 the family's equation is x² + (1−y)² = 1, an exact circular arc of radius 1
        // centred on (0, 1) in the canonical corner box. Sampling the generated cubic chain must stay on
        // that circle — this is what separates "the dial starts at a rounded rectangle" from "the dial
        // starts at something that looks a bit round".
        val controls = CornerSmoothness.cornerControls(0f)
        assertEquals("eight cubic segments", 8 * 6, controls.size)

        var previous = Pair(0f, 0f)
        for (segment in 0 until 8) {
            val i = segment * 6
            val p0 = previous
            val p1 = controls[i] to controls[i + 1]
            val p2 = controls[i + 2] to controls[i + 3]
            val p3 = controls[i + 4] to controls[i + 5]
            for (step in 0..16) {
                val t = step / 16f
                val u = 1f - t
                val x = u * u * u * p0.first + 3f * u * u * t * p1.first +
                    3f * u * t * t * p2.first + t * t * t * p3.first
                val y = u * u * u * p0.second + 3f * u * u * t * p1.second +
                    3f * u * t * t * p2.second + t * t * t * p3.second
                val distance = kotlin.math.hypot(x.toDouble(), (y - 1f).toDouble())
                assertTrue(
                    "point ($x, $y) is on the unit circle about (0, 1): $distance",
                    abs(distance - 1.0) < 0.01
                )
            }
            previous = p3
        }
        assertEquals("the corner ends on its outgoing edge", 1f, previous.first, 0.0001f)
        assertEquals("at the far end of the box", 1f, previous.second, 0.0001f)
    }

    @Test
    fun everyDialPositionProducesATangentCorner() {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { smoothing ->
            val controls = CornerSmoothness.cornerControls(smoothing)
            assertEquals("$smoothing: eight segments", 48, controls.size)
            // The chain begins at the corner's origin by construction; its first handle therefore
            // lies *on* the incoming edge, one handle-length along it.
            assertTrue("$smoothing: the first handle lies on the incoming edge", abs(controls[1]) < 1e-5f)
            assertTrue("$smoothing: and points along it", controls[0] > 0f)
            // ...and ends exactly at the far corner of the box, arriving on the outgoing edge.
            assertTrue("$smoothing: ends on the outgoing edge", abs(controls[46] - 1f) < 0.0001f)
            assertTrue("$smoothing: at the far corner of the box", abs(controls[47] - 1f) < 0.0001f)
            assertTrue(
                "$smoothing: and arrives along the edge",
                (controls[47] - controls[45]) > 0f && abs(controls[46] - controls[44]) < 1e-5f
            )
            // The curve only ever moves deeper into the box.
            var last = 0f
            for (i in 1 until 8) {
                val x = controls[i * 6 + 4]
                val y = controls[i * 6 + 5]
                assertTrue("$smoothing: x grows monotonically at node $i", x >= last - 0.0001f)
                last = x
                assertTrue("$smoothing: y stays inside the corner box at node $i", y in -0.0001f..1.0001f)
            }
        }
    }

    @Test
    fun theShapeOnlyChangesWhenTheDialIsMoved() {
        val squircle = squircleShape(20.dp)
        assertSame("at the default the shape is untouched", squircle, squircle.withCornerSmoothing(1f))
        val tuned = squircle.withCornerSmoothing(0.4f)
        assertTrue("moving the dial produces the tuned shape", tuned is SmoothCornerShape)
        val capsule: Shape = RoundedCornerShape(percent = 50)
        assertSame("a pill is never smoothed", capsule, capsule.withCornerSmoothing(0f))
        val zero = squircleShape(0.dp)
        assertSame("a zero radius is not worth smoothing", zero, zero.withCornerSmoothing(0.4f))
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
