package com.alijafari.red.astronomy.ui.rendering

import com.alijafari.red.astronomy.domain.SkyCanvasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contracts for [SceneGroundRenderer.bottomEdgeColor] — the colour the sky canvas paints on its
 * bottom row, which is also the colour the tail below the canvas starts from.
 *
 * The point of these tests is the seam the user sees under the Moon and eclipse cards: the scene's
 * foot must be *the landscape's own colour* at every instant, not a fixed near-black band, and it
 * must be fully opaque so that the tail can continue it exactly and dissolve into whatever app
 * backdrop is behind the screen.
 */
class SceneFootColorTest {

    private val photographic = listOf(SkyCanvasTheme.REAL_SKY, SkyCanvasTheme.ATMOSPHERIC_SKY)
    private val stylised = SkyCanvasTheme.entries.filter { it !in photographic }

    private fun lighting(sunAltitudeDeg: Double, moonAltitudeDeg: Double = -30.0, moonPct: Double = 0.0) =
        LightingEngine.computeLightingState(sunAltitudeDeg, moonAltitudeDeg, moonPct)

    private fun luminance(color: androidx.compose.ui.graphics.Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    @Test
    fun footColourIsOpaqueInEveryThemeAndLight() {
        // The foot vignette paints this as its final stop, and the tail's first row must match the
        // canvas's last row exactly; a translucent value would blend with whatever is behind it and
        // re-introduce the seam.
        for (theme in SkyCanvasTheme.entries) {
            for (sun in listOf(-30.0, -18.0, -6.0, 0.0, 6.0, 25.0, 60.0)) {
                val l = lighting(sun, moonAltitudeDeg = 40.0, moonPct = 80.0)
                for (marvdasht in listOf(true, false)) {
                    val c = SceneGroundRenderer.bottomEdgeColor(theme, l, marvdasht)
                    assertEquals("$theme @ $sun marvdasht=$marvdasht alpha", 1f, c.alpha, 1e-4f)
                }
            }
        }
    }

    @Test
    fun stylisedThemesKeepTheirGroundBottomsLastStop() {
        // For the flat themes the bottom row *is* the ground gradient's last stop; nothing else draws
        // down there, so the tail must start from exactly that colour.
        val l = lighting(25.0)
        for (theme in stylised) {
            assertEquals(
                "$theme foot",
                scenePalette(theme, l).groundBottom,
                SceneGroundRenderer.bottomEdgeColor(theme, l, marvdasht = false)
            )
        }
    }

    @Test
    fun photographicFootKeepsTheFieldsColourInsteadOfBlack() {
        // The defect this replaces: both photographic themes ended in the palette's near-black
        // `groundBottom`, so a sunlit valley finished on a black band. At noon the foot must clearly
        // be grass — green above red and blue, and much lighter than that near-black.
        val noon = lighting(60.0)
        for (theme in photographic) {
            val foot = SceneGroundRenderer.bottomEdgeColor(theme, noon, marvdasht = true)
            val paletteBottom = scenePalette(theme, noon).groundBottom
            assertTrue("$theme foot is green (${foot.green})", foot.green > foot.red + 0.10f)
            assertTrue("$theme foot is green (${foot.green})", foot.green > foot.blue + 0.10f)
            assertTrue(
                "$theme foot is lighter than groundBottom: ${foot.green} vs ${paletteBottom.green}",
                foot.green > paletteBottom.green + 0.15f
            )
        }
    }

    @Test
    fun photographicFootFollowsTheSun() {
        // "At any time" means the colour has to track the light, darkening to a moonlit near-black at
        // astronomical night rather than staying a daylight green.
        for (theme in photographic) {
            val noon = SceneGroundRenderer.bottomEdgeColor(theme, lighting(60.0), marvdasht = true)
            val twilight = SceneGroundRenderer.bottomEdgeColor(theme, lighting(-6.0), marvdasht = true)
            val night = SceneGroundRenderer.bottomEdgeColor(
                theme, lighting(-25.0, moonAltitudeDeg = 40.0, moonPct = 100.0), marvdasht = true
            )
            assertTrue("twilight below noon", luminance(twilight) < luminance(noon))
            assertTrue("night below twilight", luminance(night) < luminance(twilight))
            assertTrue("night is dark (${luminance(night)})", luminance(night) < 0.12f)
        }
    }

    @Test
    fun photographicFootRisesWithTheSun() {
        // No reversing step anywhere on the Sun's arc: a non-monotone foot would show up as a colour
        // jump across the canvas edge while the user scrubs time with a finger.
        for (theme in photographic) {
            var previous = -1f
            var sun = -30.0
            while (sun <= 80.0) {
                val l = lighting(sun)
                val value = luminance(SceneGroundRenderer.bottomEdgeColor(theme, l, marvdasht = true))
                assertTrue("$theme monotone at $sun ($value after $previous)", value >= previous - 1e-4f)
                previous = value
                sun += 2.0
            }
        }
    }

    @Test
    fun photographicFootMatchesTheGrassFieldItSitsIn() {
        // The foot is the field's own tone: it must stay within the field's deep..lit range (in
        // luminance, allowing for the vignette's deepening) rather than falling outside it.
        val noon = lighting(60.0)
        val deep = 0.2126f * 0.2196f + 0.7152f * 0.4000f + 0.0722f * 0.2275f // 0xFF38663A
        val lit = 0.2126f * 0.4549f + 0.7152f * 0.6431f + 0.0722f * 0.3765f  // 0xFF74A460
        for (theme in photographic) {
            val foot = luminance(SceneGroundRenderer.bottomEdgeColor(theme, noon, marvdasht = true))
            assertTrue("$theme foot $foot below the field", foot > deep * 0.70f)
            assertTrue("$theme foot $foot above the field", foot < lit * 1.02f)
        }
    }
}
