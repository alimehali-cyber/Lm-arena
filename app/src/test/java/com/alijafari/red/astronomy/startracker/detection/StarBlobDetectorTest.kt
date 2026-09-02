package com.alijafari.red.astronomy.startracker.detection

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class StarBlobDetectorTest {

    private fun matchDetectionsToTruth(
        detections: List<DetectedBlob>,
        truth: List<StarTruth>,
        tolerancePx: Double = 5.0
    ): Pair<Int, Int> {
        // Returns Pair(recallCount, falsePositiveCount)
        // Simple greedy matching: each truth can be matched at most once to closest detection within tolerance
        val matchedTruth = mutableSetOf<Int>()
        var falsePositives = 0

        for (det in detections) {
            var bestIdx = -1
            var bestDist = Double.MAX_VALUE
            for ((i, gt) in truth.withIndex()) {
                if (gt.isHotPixel) continue // hot pixels should not be counted as true stars for recall
                if (gt.isStreaked) continue // streaks should be rejected
                if (i in matchedTruth) continue
                val dx = det.peakX - gt.x
                val dy = det.peakY - gt.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < bestDist && dist <= tolerancePx) {
                    bestDist = dist
                    bestIdx = i
                }
            }
            if (bestIdx >= 0) {
                matchedTruth.add(bestIdx)
            } else {
                falsePositives++
            }
        }

        val trueStars = truth.count { !it.isHotPixel && !it.isStreaked }
        val recall = matchedTruth.size
        return Pair(recall, falsePositives)
    }

    @Test
    fun testDetectionBrightStars() {
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 320,
            height = 240,
            numStars = 15,
            amplitudeRange = 80.0 to 200.0,
            sigmaRange = 1.0 to 1.5,
            background = BackgroundParams(baseLevel = 20f),
            noise = NoiseParams(gaussianSigma = 2f, seed = 10L),
            seed = 10L
        )

        val estimator = BackgroundEstimator(blockSize = 32)
        val bgMap = estimator.estimate(field.image)
        val noiseSigma = estimator.estimateNoiseSigma(field.image, bgMap)

        val detector = StarBlobDetector(thresholdK = 5.0, minBlobSize = 3, maxBlobSize = 200, maxElongation = 2.5f)
        val blobs = detector.detect(field.image, bgMap, noiseSigma)

        val (recall, fp) = matchDetectionsToTruth(blobs, field.groundTruth)
        val totalTrue = field.groundTruth.count { !it.isHotPixel && !it.isStreaked }
        val recallPct = recall.toDouble() / totalTrue * 100.0

        println("Bright stars: detected ${blobs.size} blobs, recall $recall/$totalTrue = ${"%.1f".format(recallPct)}%, FP=$fp, noiseSigma=$noiseSigma")

        assertTrue("Recall should be >=80% for bright stars, got $recallPct%", recallPct >= 80.0)
        assertTrue("FP should be <=2 for bright stars, got $fp", fp <= 2)
    }

    @Test
    fun testHotPixelRejection() {
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 200,
            height = 200,
            numStars = 5,
            amplitudeRange = 80.0 to 150.0,
            background = BackgroundParams(baseLevel = 15f),
            noise = NoiseParams(gaussianSigma = 1f, seed = 11L),
            seed = 11L,
            includeHotPixels = 5,
            includeStreaks = 0
        )

        val estimator = BackgroundEstimator()
        val bgMap = estimator.estimate(field.image)
        val noiseSigma = estimator.estimateNoiseSigma(field.image, bgMap)

        val detector = StarBlobDetector(minBlobSize = 3)
        val blobs = detector.detect(field.image, bgMap, noiseSigma)

        // Hot pixels are single-pixel, should be rejected by minBlobSize=3
        // So blobs should not include hot pixels
        val hotPixelTruth = field.groundTruth.filter { it.isHotPixel }
        var hotPixelDetectedAsStar = 0
        for (gt in hotPixelTruth) {
            for (blob in blobs) {
                val dx = blob.peakX - gt.x
                val dy = blob.peakY - gt.y
                if (sqrt(dx * dx + dy * dy) < 2.0) {
                    hotPixelDetectedAsStar++
                }
            }
        }

        println("Hot pixel test: ${hotPixelTruth.size} hot pixels injected, $hotPixelDetectedAsStar detected as stars (should be 0)")
        assertEquals("Hot pixels should be rejected", 0, hotPixelDetectedAsStar)
    }

    @Test
    fun testStreakRejection() {
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 300,
            height = 300,
            numStars = 5,
            amplitudeRange = 80.0 to 150.0,
            background = BackgroundParams(baseLevel = 15f),
            noise = NoiseParams(gaussianSigma = 1f, seed = 12L),
            seed = 12L,
            includeHotPixels = 0,
            includeStreaks = 3
        )

        val estimator = BackgroundEstimator()
        val bgMap = estimator.estimate(field.image)
        val noiseSigma = estimator.estimateNoiseSigma(field.image, bgMap)

        val detector = StarBlobDetector(maxElongation = 2.5f)
        val blobs = detector.detect(field.image, bgMap, noiseSigma)

        val streakTruth = field.groundTruth.filter { it.isStreaked }
        var streakDetectedAsStar = 0
        for (gt in streakTruth) {
            for (blob in blobs) {
                val dx = blob.peakX - gt.x
                val dy = blob.peakY - gt.y
                if (sqrt(dx * dx + dy * dy) < 10.0) {
                    streakDetectedAsStar++
                }
            }
        }

        println("Streak test: ${streakTruth.size} streaks injected, $streakDetectedAsStar detected as good stars (should be 0)")
        // Streaks should be rejected due to elongation
        assertTrue("Streaks should be mostly rejected, got $streakDetectedAsStar detected", streakDetectedAsStar <= 1)
    }

    @Test
    fun testVaryingSNR() {
        val snrConditions = listOf(
            Triple("Low SNR", 30.0 to 50.0, 2f),
            Triple("Medium SNR", 60.0 to 100.0, 2f),
            Triple("High SNR", 120.0 to 200.0, 2f)
        )

        for ((label, ampRange, noiseSigma) in snrConditions) {
            val field = SyntheticStarFieldGenerator.generateRandomField(
                width = 320,
                height = 240,
                numStars = 10,
                amplitudeRange = ampRange,
                background = BackgroundParams(baseLevel = 20f),
                noise = NoiseParams(gaussianSigma = noiseSigma, seed = 20L),
                seed = 20L
            )

            val estimator = BackgroundEstimator()
            val bgMap = estimator.estimate(field.image)
            val estNoise = estimator.estimateNoiseSigma(field.image, bgMap)

            val detector = StarBlobDetector(thresholdK = 5.0)
            val blobs = detector.detect(field.image, bgMap, estNoise)

            val (recall, fp) = matchDetectionsToTruth(blobs, field.groundTruth)
            val total = field.groundTruth.count { !it.isHotPixel && !it.isStreaked }
            val recallPct = if (total > 0) recall.toDouble() / total * 100 else 0.0

            println("$label: amp ${ampRange.first}-${ampRange.second}, noise $noiseSigma, estNoise $estNoise, recall $recall/$total=${"%.1f".format(recallPct)}%, FP=$fp")
        }
    }
}
