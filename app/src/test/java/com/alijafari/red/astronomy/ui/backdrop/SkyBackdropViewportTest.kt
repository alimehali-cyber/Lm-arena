package com.alijafari.red.astronomy.ui.backdrop

import com.alijafari.red.astronomy.ui.rendering.HeroSkyProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyBackdropViewportTest {

    private val observerLat = 35.0

    @Test
    fun portraitPhoneUsesOneScaleForBothAxes() {
        val vp = SkyBackdropViewport.fit(411f, 891f)
        val expectedScale = (0.85f * 891f - 24f) / 90f
        assertEquals(expectedScale, vp.pxPerDeg, 1e-3f)
        assertEquals(360f * expectedScale, vp.virtualWidthPx, 1e-2f)
        assertEquals((411f - vp.virtualWidthPx) / 2f, vp.offsetXPx, 1e-2f)
        assertTrue(vp.isDrawable)
    }

    @Test
    fun projectionIsIsotropicOnTheFullScreenCanvas() {
        // One degree of azimuth and one degree of altitude must cover the same distance in pixels.
        val sizes = listOf(
            360f to 780f, 411f to 891f, 393f to 852f, 800f to 1280f, 1080f to 1920f, 1200f to 800f
        )
        for ((w, h) in sizes) {
            val vp = SkyBackdropViewport.fit(w, h)
            val dx = HeroSkyProjection.project(181.0, 0.0, vp.virtualWidthPx, vp.heightPx, observerLat).x -
                HeroSkyProjection.project(180.0, 0.0, vp.virtualWidthPx, vp.heightPx, observerLat).x
            val dy = HeroSkyProjection.project(180.0, 0.0, vp.virtualWidthPx, vp.heightPx, observerLat).y -
                HeroSkyProjection.project(180.0, 1.0, vp.virtualWidthPx, vp.heightPx, observerLat).y
            assertEquals("dx for ${w}x$h", vp.pxPerDeg, dx, vp.pxPerDeg * 1e-3f)
            assertEquals("dy for ${w}x$h", vp.pxPerDeg, dy, vp.pxPerDeg * 1e-3f)
        }
    }

    @Test
    fun screenCentreLooksSouthInTheNorthernHemisphere() {
        val vp = SkyBackdropViewport.fit(411f, 891f)
        val centreX = HeroSkyProjection.project(180.0, 0.0, vp.virtualWidthPx, vp.heightPx, observerLat).x + vp.offsetXPx
        assertEquals(411f / 2f, centreX, 1e-2f)
    }

    @Test
    fun zenithSitsAtTheSameTopMarginAsTheHomeCard() {
        val vp = SkyBackdropViewport.fit(411f, 891f)
        val zenithY = HeroSkyProjection.project(180.0, 90.0, vp.virtualWidthPx, vp.heightPx, observerLat).y
        assertEquals(HeroSkyProjection.TOP_MARGIN_PX, zenithY, 1e-3f)
    }

    @Test
    fun visibleAzimuthSpanIsTheWindowWidthInDegrees() {
        val vp = SkyBackdropViewport.fit(411f, 891f)
        assertEquals(411f / vp.pxPerDeg, vp.visibleAzimuthSpanDeg, 1e-3f)
        assertTrue(vp.visibleAzimuthSpanDeg > 0f && vp.visibleAzimuthSpanDeg < 360f)
    }

    @Test
    fun degenerateWindowsAreNotDrawable() {
        assertFalse(SkyBackdropViewport.fit(0f, 800f).isDrawable)
        assertFalse(SkyBackdropViewport.fit(400f, 0f).isDrawable)
        assertFalse(SkyBackdropViewport.fit(-1f, -1f).isDrawable)
        // Too short for the horizon margin: the scale would be negative.
        assertFalse(SkyBackdropViewport.fit(400f, 20f).isDrawable)
    }
}
