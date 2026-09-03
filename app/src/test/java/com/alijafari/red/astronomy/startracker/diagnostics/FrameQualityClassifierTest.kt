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

    @Test
    fun testHighNoiseModerateBlobCount() {
        // Audit finding B10 regression test: a MODERATE blob count (2..15) with high
        // background noise used to fall through to GOOD (the high-noise branch required
        // blobCount > 15, and the borderline fallback returned GOOD for any count >= 2).
        // It must classify POOR_HIGH_NOISE.
        val classifier = FrameQualityClassifier()
        for (count in listOf(2, 5, 8, 12, 15)) {
            val stats = BlobStats(
                blobCount = count,
                meanBrightness = 80.0,
                brightnessStd = 15.0,
                meanSize = 4.0,
                sizeStd = 1.0,
                backgroundMean = 25.0,
                backgroundStd = 60.0, // high noise
                maxBrightness = 200.0
            )
            val quality = classifier.classify(stats)
            println("Moderate count $count with high noise: $quality")
            assertEquals("blobCount $count with bgStd 60 must be POOR_HIGH_NOISE (audit B10)",
                FrameQuality.POOR_HIGH_NOISE, quality)
        }
    }

    @Test
    fun testLowBlobCountCleanBackgroundStillGood() {
        // Guard against over-correction: few blobs on a CLEAN background remain GOOD
        // (borderline case), and 1 blob is still POOR_LOW_STARS.
        val classifier = FrameQualityClassifier()
        val borderline = BlobStats(
            blobCount = 3, meanBrightness = 80.0, brightnessStd = 15.0, meanSize = 4.0,
            sizeStd = 1.0, backgroundMean = 25.0, backgroundStd = 10.0, maxBrightness = 200.0
        )
        assertEquals(FrameQuality.GOOD, classifier.classify(borderline))
        val tooFew = borderline.copy(blobCount = 1)
        assertEquals(FrameQuality.POOR_LOW_STARS, classifier.classify(tooFew))
    }
}
