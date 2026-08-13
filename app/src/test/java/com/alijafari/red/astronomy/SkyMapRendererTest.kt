package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.AstroTime
import com.alijafari.red.astronomy.astro_engine.SkyMapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyMapRendererTest {

    private val renderer = SkyMapRenderer()

    private fun settings(): SkyMapRenderer.RenderSettings {
        return SkyMapRenderer.RenderSettings(
            canvasWidth = 1000.0f,
            canvasHeight = 1000.0f,
            centerAzDeg = 180.0,
            centerAltDeg = 90.0,
            fieldOfViewDeg = 120.0
        )
    }

    @Test
    fun `test zenith projects to center`() {
        val s = settings()
        val point = renderer.projectAltAz(90.0, 0.0, s)
        assertEquals("Zenith X should be center", 500.0f, point.x, 1.0f)
        assertEquals("Zenith Y should be center", 500.0f, point.y, 1.0f)
    }

    @Test
    fun `test horizon projects to edge`() {
        val s = settings()
        val point = renderer.projectAltAz(0.0, 0.0, s)
        // Horizon should be at edge of canvas
        assertTrue("Horizon X should be near edge", point.x < 100.0f || point.x > 900.0f)
    }

    @Test
    fun `test render produces stars`() {
        val s = settings()
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        val result = renderer.render(astroTime, 35.7, 51.4, s)
        assertTrue("Should render stars", result.stars.isNotEmpty())
    }

    @Test
    fun `test render produces constellation lines`() {
        val s = settings()
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        val result = renderer.render(astroTime, 35.7, 51.4, s)
        assertTrue("Should render constellation lines", result.constellationLines.isNotEmpty())
    }

    @Test
    fun `test render produces deep sky objects`() {
        val s = settings()
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        val result = renderer.render(astroTime, 35.7, 51.4, s)
        assertTrue("Should render deep sky objects", result.deepSkyObjects.isNotEmpty())
    }

    @Test
    fun `test render produces grid`() {
        val s = settings()
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        val result = renderer.render(astroTime, 35.7, 51.4, s)
        assertTrue("Should render grid", result.gridLines.isNotEmpty())
    }

    @Test
    fun `test render produces labels`() {
        val s = settings()
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        val result = renderer.render(astroTime, 35.7, 51.4, s)
        assertTrue("Should render labels", result.labels.isNotEmpty())
    }

    @Test
    fun `test identify star at center`() {
        val s = settings()
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        // Tap at center (zenith)
        val star = renderer.identifyStarAt(500.0f, 500.0f, astroTime, 35.7, 51.4, s)
        // May be null if no star exactly at zenith, but should not crash
        assertNotNull("Should not crash", star)
    }

    @Test
    fun `test identify deep sky at center`() {
        val s = settings()
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        val obj = renderer.identifyDeepSkyAt(500.0f, 500.0f, astroTime, 35.7, 51.4, s)
        assertNotNull("Should not crash", obj)
    }

    @Test
    fun `test star radius by magnitude`() {
        // Brighter stars should have larger radius
        val bright = renderer.starRadiusForTest(0.5)
        val dim = renderer.starRadiusForTest(5.5)
        assertTrue("Bright star should be larger", bright > dim)
    }

    @Test
    fun `test deep sky radius by type`() {
        val galaxy = renderer.deepSkyRadiusForTest("Spiral Galaxy", 8.0)
        val cluster = renderer.deepSkyRadiusForTest("Open Cluster", 8.0)
        assertTrue("Galaxy should be larger than cluster", galaxy > cluster)
    }

    @Test
    fun `test render with all layers disabled`() {
        val s = SkyMapRenderer.RenderSettings(
            canvasWidth = 1000.0f,
            canvasHeight = 1000.0f,
            centerAzDeg = 180.0,
            centerAltDeg = 90.0,
            fieldOfViewDeg = 120.0,
            showStars = false,
            showConstellations = false,
            showDeepSky = false,
            showGrid = false,
            showLabels = false
        )
        val astroTime = AstroTime.fromUtcDate(2026, 1, 15, 22, 0, 0)
        val result = renderer.render(astroTime, 35.7, 51.4, s)
        assertTrue("No stars", result.stars.isEmpty())
        assertTrue("No constellation lines", result.constellationLines.isEmpty())
        assertTrue("No deep sky", result.deepSkyObjects.isEmpty())
        assertTrue("No grid", result.gridLines.isEmpty())
        assertTrue("No labels", result.labels.isEmpty())
    }
}
