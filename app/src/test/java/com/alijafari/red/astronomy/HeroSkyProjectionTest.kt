package com.alijafari.red.astronomy

import androidx.compose.ui.geometry.Offset
import com.alijafari.red.astronomy.ui.rendering.HeroSkyProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HeroSkyProjectionTest {

    @Test
    fun testAzimuthWraparoundDistance() {
        // 359° and 1° are 2° apart, not 358° apart
        val diff1 = HeroSkyProjection.azimuthDistanceDeg(359.0, 1.0)
        assertEquals(2.0, diff1, 1e-6)

        val diff2 = HeroSkyProjection.azimuthDistanceDeg(1.0, 359.0)
        assertEquals(2.0, diff2, 1e-6)

        val diff3 = HeroSkyProjection.azimuthDistanceDeg(350.0, 10.0)
        assertEquals(20.0, diff3, 1e-6)

        val diff4 = HeroSkyProjection.azimuthDistanceDeg(0.0, 360.0)
        assertEquals(0.0, diff4, 1e-6)

        val diff5 = HeroSkyProjection.azimuthDistanceDeg(180.0, 0.0)
        assertEquals(180.0, diff5, 1e-6)
    }

    @Test
    fun testScreenDistanceIsFlatBecauseTheSkyIsDrawnFlat() {
        // The canvas paints each object once, at its own x: the 360° seam simply ends at the two
        // edges, nothing is drawn wrapped. So an object at the left edge is a whole canvas away from
        // a tap at the right edge — this used to be reported as 10 px away, which selected an object
        // on the far side of the screen and put the identify card there instead of at the finger.
        val tapPos = Offset(5f, 100f)
        val objPos = Offset(995f, 100f)
        assertEquals(990f, HeroSkyProjection.screenDistance(tapPos, objPos), 1e-4f)

        // The ordinary case is unchanged: plain straight-line pixel distance.
        assertEquals(
            10f,
            HeroSkyProjection.screenDistance(Offset(500f, 100f), Offset(510f, 100f)),
            1e-4f
        )
        assertEquals(
            5f,
            HeroSkyProjection.screenDistance(Offset(0f, 0f), Offset(3f, 4f)),
            1e-4f
        )
    }

    @Test
    fun testHeroSkyProjectionCoordinates() {
        val width = 1000f
        val height = 500f

        // Horizon at altitude 0° -> y = height * 0.85 = 425
        val horizonPos = HeroSkyProjection.project(0.0, 0.0, width, height)
        assertEquals(0f, horizonPos.x, 1e-4f)
        assertEquals(425f, horizonPos.y, 1e-4f)

        // Zenith at altitude 90° -> y = 24px (TOP_MARGIN_PX)
        val zenithPos = HeroSkyProjection.project(180.0, 90.0, width, height)
        assertEquals(500f, zenithPos.x, 1e-4f)
        assertEquals(24f, zenithPos.y, 1e-4f)

        // Full 360° wraps seamlessly to 0
        val wrapPos = HeroSkyProjection.project(360.0, 0.0, width, height)
        assertEquals(0f, wrapPos.x, 1e-4f)
        assertEquals(425f, wrapPos.y, 1e-4f)
    }

    @Test
    fun testSouthernHemisphereRisesRightSetsLeft() {
        val width = 1000f
        val height = 500f
        val sydneyLat = -33.8688

        // In Southern Hemisphere facing North (0°):
        // East (90°) is on the RIGHT (x = 750), North (0°) is CENTER (x = 500), West (270°) is on the LEFT (x = 250)
        val eastPos = HeroSkyProjection.project(90.0, 0.0, width, height, sydneyLat)
        val northPos = HeroSkyProjection.project(0.0, 0.0, width, height, sydneyLat)
        val westPos = HeroSkyProjection.project(270.0, 0.0, width, height, sydneyLat)

        assertEquals(750f, eastPos.x, 1e-4f)
        assertEquals(500f, northPos.x, 1e-4f)
        assertEquals(250f, westPos.x, 1e-4f)
    }

    @Test
    fun testRealSkyThemeAndAtmosphereLighting() {
        // Verify REAL_SKY is the first theme and has bilingual labels
        val themes = com.alijafari.red.astronomy.domain.SkyCanvasTheme.values()
        assertEquals(com.alijafari.red.astronomy.domain.SkyCanvasTheme.REAL_SKY, themes.first())
        assertEquals("Real Sky", com.alijafari.red.astronomy.domain.SkyCanvasTheme.REAL_SKY.nameEn)
        assertEquals("آسمان واقعی", com.alijafari.red.astronomy.domain.SkyCanvasTheme.REAL_SKY.nameFa)

        // Verify LightingEngine.computeLightingState transitions
        val dayLighting = com.alijafari.red.astronomy.ui.rendering.LightingEngine.computeLightingState(25.0, -10.0, 0.0)
        assertEquals(0.0f, dayLighting.starVisibility, 1e-4f)

        val deepNightDarkMoon = com.alijafari.red.astronomy.ui.rendering.LightingEngine.computeLightingState(-22.0, -15.0, 0.0)
        assertEquals(1.0f, deepNightDarkMoon.starVisibility, 1e-4f)
        assertEquals(0.0f, deepNightDarkMoon.moonGlowIntensity, 1e-4f)

        val deepNightFullMoon = com.alijafari.red.astronomy.ui.rendering.LightingEngine.computeLightingState(-22.0, 45.0, 100.0)
        assertTrue(deepNightFullMoon.moonGlowIntensity > deepNightDarkMoon.moonGlowIntensity)
    }
}
