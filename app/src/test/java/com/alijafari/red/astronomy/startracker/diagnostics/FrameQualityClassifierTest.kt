package com.alijafari.red.astronomy.startracker.diagnostics

import org.junit.Test
import org.junit.Assert.*

class FrameQualityClassifierTest {

    @Test
    fun testGoodFrame() {
        val classifier = FrameQualityClassifier()
        val stats = BlobStats(
            blobCount = 10,
            meanBrightness = 100.0,
            brightnessStd = 20.0,
            meanSize = 5.0,
            sizeStd = 1.0,
            backgroundMean = 20.0,
            backgroundStd = 10.0,
            maxBrightness = 200.0
        )
        val quality = classifier.classify(stats)
        println("Good frame: $quality")
        assertEquals(FrameQuality.GOOD, quality)
    }

    @Test
    fun testLowStars() {
        val classifier = FrameQualityClassifier()
        val stats = BlobStats(
            blobCount = 1,
            meanBrightness = 100.0,
            brightnessStd = 20.0,
            meanSize = 5.0,
            sizeStd = 1.0,
            backgroundMean = 20.0,
            backgroundStd = 10.0,
            maxBrightness = 200.0
        )
        val quality = classifier.classify(stats)
        println("Low stars frame: $quality")
        assertEquals(FrameQuality.POOR_LOW_STARS, quality)
    }

    @Test
    fun testHighNoise() {
        val classifier = FrameQualityClassifier()
        val stats = BlobStats(
            blobCount = 30, // many blobs
            meanBrightness = 30.0,
            brightnessStd = 5.0,
            meanSize = 2.0,
            sizeStd = 0.5,
            backgroundMean = 30.0,
            backgroundStd = 60.0, // high background std
            maxBrightness = 100.0
        )
        val quality = classifier.classify(stats)
        println("High noise frame: $quality")
        assertEquals(FrameQuality.POOR_HIGH_NOISE, quality)
    }

    @Test
    fun testOverexposed() {
        val classifier = FrameQualityClassifier()
        val stats = BlobStats(
            blobCount = 2,
            meanBrightness = 200.0,
            brightnessStd = 10.0,
            meanSize = 5.0,
            sizeStd = 1.0,
            backgroundMean = 210.0,
            backgroundStd = 10.0,
            maxBrightness = 255.0
        )
        val quality = classifier.classify(stats)
        println("Overexposed frame: $quality")
        assertEquals(FrameQuality.POOR_OVEREXPOSED, quality)
    }
}
