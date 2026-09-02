package com.alijafari.red.astronomy.startracker.detection

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class SyntheticStarFieldGeneratorTest {

    @Test
    fun testSingleStarProducesLocalMaximum() {
        val stars = listOf(
            StarParams(x = 100.3, y = 150.7, amplitude = 100.0, sigma = 1.2)
        )
        val field = SyntheticStarFieldGenerator.generate(
            width = 320,
            height = 240,
            stars = stars,
            background = BackgroundParams(baseLevel = 20f),
            noise = NoiseParams(gaussianSigma = 0f, seed = 42L),
            saturationMax = 255f
        )

        // Find local max near injected position
        var maxVal = -1f
        var maxX = -1
        var maxY = -1
        for (y in 140..160) {
            for (x in 90..110) {
                val v = field.image.get(x, y)
                if (v > maxVal) {
                    maxVal = v
                    maxX = x
                    maxY = y
                }
            }
        }

        // Max should be near 100,150 (within 1 pixel)
        assertTrue("maxX=$maxX should be near 100", abs(maxX - 100) <= 1)
        assertTrue("maxY=$maxY should be near 151", abs(maxY - 151) <= 1)
        assertTrue("maxVal should be > background", maxVal > 50f)
        assertEquals(1, field.groundTruth.size)
        assertEquals(100.3, field.groundTruth[0].x, 0.01)
    }

    @Test
    fun testBackgroundLevel() {
        val bg = BackgroundParams(baseLevel = 30f, gradXPerPixel = 0f, gradYPerPixel = 0f)
        val field = SyntheticStarFieldGenerator.generate(
            width = 100,
            height = 100,
            stars = emptyList(),
            background = bg,
            noise = NoiseParams(gaussianSigma = 0f),
            saturationMax = 255f
        )
        val stats = field.image.stats()
        assertEquals(30f, stats.mean, 0.1f)
        assertEquals(30f, stats.min, 0.1f)
        assertEquals(30f, stats.max, 0.1f)
    }

    @Test
    fun testGradientBackground() {
        val bg = BackgroundParams(baseLevel = 20f, gradXPerPixel = 0.1f, gradYPerPixel = 0.05f)
        val field = SyntheticStarFieldGenerator.generate(
            width = 100,
            height = 100,
            stars = emptyList(),
            background = bg,
            noise = NoiseParams(gaussianSigma = 0f),
            saturationMax = 255f
        )
        // At (0,0): 20, at (99,99): 20+9.9+4.95=34.85
        assertEquals(20f, field.image.get(0, 0), 0.1f)
        assertEquals(34.85f, field.image.get(99, 99), 0.5f)
    }

    @Test
    fun testSaturationClipping() {
        val stars = listOf(
            StarParams(x = 50.0, y = 50.0, amplitude = 300.0, sigma = 1.0)
        )
        val field = SyntheticStarFieldGenerator.generate(
            width = 100,
            height = 100,
            stars = stars,
            background = BackgroundParams(baseLevel = 20f),
            noise = NoiseParams(gaussianSigma = 0f),
            saturationMax = 255f
        )
        val stats = field.image.stats()
        assertTrue("max should be clipped to 255", stats.max <= 255.1f)
        assertTrue("ground truth should be marked saturated", field.groundTruth[0].isSaturated)
    }

    @Test
    fun testHotPixel() {
        val stars = listOf(
            StarParams(x = 10.0, y = 10.0, amplitude = 200.0, sigma = 0.5, isHotPixel = true)
        )
        val field = SyntheticStarFieldGenerator.generate(
            width = 100,
            height = 100,
            stars = stars,
            background = BackgroundParams(baseLevel = 10f),
            noise = NoiseParams(gaussianSigma = 0f),
            saturationMax = 255f
        )
        // Hot pixel should be isolated
        assertEquals(210f, field.image.get(10, 10), 1f)
        assertEquals(10f, field.image.get(11, 10), 1f)
        assertTrue(field.groundTruth[0].isHotPixel)
    }

    @Test
    fun testRandomFieldGeneration() {
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 640,
            height = 480,
            numStars = 20,
            seed = 123L,
            includeHotPixels = 2,
            includeStreaks = 1
        )
        assertEquals(640, field.image.width)
        assertEquals(480, field.image.height)
        assertEquals(23, field.groundTruth.size) // 20 + 2 hot + 1 streak
    }
}
