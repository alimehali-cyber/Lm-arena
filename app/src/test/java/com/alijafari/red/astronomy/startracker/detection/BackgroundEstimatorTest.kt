package com.alijafari.red.astronomy.startracker.detection

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class BackgroundEstimatorTest {

    @Test
    fun testFlatBackgroundEstimation() {
        val bg = BackgroundParams(baseLevel = 25f)
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 320,
            height = 240,
            numStars = 10,
            background = bg,
            noise = NoiseParams(gaussianSigma = 1f, seed = 1L),
            seed = 1L
        )

        val estimator = BackgroundEstimator(blockSize = 32)
        val bgMap = estimator.estimate(field.image)
        val noiseSigma = estimator.estimateNoiseSigma(field.image, bgMap)

        // With flat background 25, estimate should be close
        var sumErr = 0.0
        var count = 0
        for (y in 0 until field.image.height step 10) {
            for (x in 0 until field.image.width step 10) {
                // Avoid star regions: check if near any star
                var nearStar = false
                for (gt in field.groundTruth) {
                    if (gt.isHotPixel) continue
                    if (abs(gt.x - x) < 10 && abs(gt.y - y) < 10) {
                        nearStar = true
                        break
                    }
                }
                if (!nearStar) {
                    val est = bgMap.getBackground(x, y)
                    sumErr += abs(est - 25.0)
                    count++
                }
            }
        }
        val mae = sumErr / count
        println("Flat background MAE: $mae, noiseSigma: $noiseSigma")
        assertTrue("MAE should be <2 for flat background, got $mae", mae < 2.0)
        assertTrue("noise sigma should be close to 1", abs(noiseSigma - 1f) < 1.5f)
    }

    @Test
    fun testGradientBackgroundEstimation() {
        val bg = BackgroundParams(baseLevel = 20f, gradXPerPixel = 0.05f, gradYPerPixel = 0.02f)
        val field = SyntheticStarFieldGenerator.generate(
            width = 320,
            height = 240,
            stars = emptyList(),
            background = bg,
            noise = NoiseParams(gaussianSigma = 0.5f, seed = 2L),
            saturationMax = 255f
        )

        val estimator = BackgroundEstimator(blockSize = 32)
        val bgMap = estimator.estimate(field.image)

        var sumErr = 0.0
        var count = 0
        for (y in 0 until field.image.height step 8) {
            for (x in 0 until field.image.width step 8) {
                val trueBg = bg.baseLevel + bg.gradXPerPixel * x + bg.gradYPerPixel * y
                val est = bgMap.getBackground(x, y)
                sumErr += abs(est - trueBg)
                count++
            }
        }
        val mae = sumErr / count
        println("Gradient background MAE: $mae")
        // With gradient, estimator should track reasonably (MAE < 1.5)
        assertTrue("Gradient MAE should be <1.5, got $mae", mae < 1.5)
    }

    @Test
    fun testRobustnessToStars() {
        // Background estimator should not be thrown off by bright stars
        val bg = BackgroundParams(baseLevel = 20f)
        val stars = (0 until 20).map {
            StarParams(
                x = 20.0 + it * 15,
                y = 20.0 + (it % 5) * 30,
                amplitude = 150.0,
                sigma = 1.2
            )
        }
        val field = SyntheticStarFieldGenerator.generate(
            width = 320,
            height = 240,
            stars = stars,
            background = bg,
            noise = NoiseParams(gaussianSigma = 1f, seed = 3L),
            saturationMax = 255f
        )

        val estimator = BackgroundEstimator(blockSize = 32, useMedian = true, sigmaClip = true)
        val bgMap = estimator.estimate(field.image)

        // Check that block estimates are not wildly high due to stars
        var maxBlock = 0f
        for (v in bgMap.blockEstimates) {
            if (v > maxBlock) maxBlock = v
        }
        println("Max block estimate with bright stars: $maxBlock, globalMean: ${bgMap.globalMean}")
        // With median + sigma clipping, max block should be < 30 (close to true bg 20)
        assertTrue("Max block estimate should be <35 despite bright stars, got $maxBlock", maxBlock < 35f)
    }
}
