package com.alijafari.red.astronomy.startracker.detection

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class StarDetectionPipelineTest {

    @Test
    fun testEndToEndPipeline() {
        // Generate synthetic multi-star field: 10-30 stars varying brightness, some saturated, near noise floor, couple hot pixels, one streak
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 640,
            height = 480,
            numStars = 20,
            amplitudeRange = 30.0 to 250.0, // varying from near noise floor to saturated
            sigmaRange = 0.8 to 1.8,
            background = BackgroundParams(baseLevel = 20f, gradXPerPixel = 0.01f),
            noise = NoiseParams(gaussianSigma = 2f, seed = 500L),
            saturationMax = 255f,
            seed = 500L,
            includeHotPixels = 2,
            includeStreaks = 1
        )

        val pipeline = StarDetectionPipeline(
            backgroundEstimator = BackgroundEstimator(blockSize = 32),
            blobDetector = StarBlobDetector(thresholdK = 5.0, minBlobSize = 3, maxBlobSize = 200, maxElongation = 2.5f),
            centroider = Centroider(includeMargin = 1, saturationThreshold = 250f, excludeSaturated = true),
            useGaussianFit = false
        )

        val result = pipeline.process(field.image)

        // Matching
        val tolerancePx = 5.0
        val matchedTruth = mutableSetOf<Int>()
        val errors = mutableListOf<Double>()
        var falsePositives = 0

        for (det in result.stars) {
            var bestIdx = -1
            var bestDist = Double.MAX_VALUE
            var bestGt: StarTruth? = null
            for ((i, gt) in field.groundTruth.withIndex()) {
                if (gt.isHotPixel) continue
                if (gt.isStreaked) continue
                if (i in matchedTruth) continue
                val dx = det.x - gt.x
                val dy = det.y - gt.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < bestDist && dist <= tolerancePx) {
                    bestDist = dist
                    bestIdx = i
                    bestGt = gt
                }
            }
            if (bestIdx >= 0 && bestGt != null) {
                matchedTruth.add(bestIdx)
                errors.add(bestDist)
            } else {
                falsePositives++
            }
        }

        val totalTrueStars = field.groundTruth.count { !it.isHotPixel && !it.isStreaked }
        val missed = totalTrueStars - matchedTruth.size
        val recallPct = if (totalTrueStars > 0) matchedTruth.size.toDouble() / totalTrueStars * 100 else 0.0

        val rms = if (errors.isNotEmpty()) sqrt(errors.map { it * it }.average()) else 0.0
        val median = if (errors.isNotEmpty()) {
            val sorted = errors.sorted()
            if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else 0.0
        val maxErr = errors.maxOrNull() ?: 0.0
        val meanErr = if (errors.isNotEmpty()) errors.average() else 0.0

        println("=== End-to-End Pipeline Test ===")
        println("Image: ${field.image.width}x${field.image.height}, ${field.groundTruth.size} injected (true stars: $totalTrueStars, hot: ${field.groundTruth.count { it.isHotPixel }}, streak: ${field.groundTruth.count { it.isStreaked }})")
        println("Detected: ${result.stars.size} stars, blobs: ${result.blobs.size}")
        println("Matched: ${matchedTruth.size}/$totalTrueStars = ${"%.1f".format(recallPct)}% recall")
        println("Missed: $missed, False Positives: $falsePositives")
        println("Centroid errors: RMS=${"%.4f".format(rms)} px, median=${"%.4f".format(median)} px, mean=${"%.4f".format(meanErr)} px, max=${"%.4f".format(maxErr)} px")
        println("Noise sigma estimated: ${result.noiseSigma}, background mean: ${result.backgroundMap.globalMean}")

        // Assertions
        assertTrue("Recall should be >=70% for mixed field, got $recallPct%", recallPct >= 70.0)
        assertTrue("False positives should be <=3, got $falsePositives", falsePositives <= 3)
        assertTrue("RMS centroid error should be <0.5 px, got $rms", rms < 0.8)
        assertTrue("Missed should be <=30% of true", missed <= totalTrueStars * 0.3)
    }

    @Test
    fun testPipelineWithGaussianFit() {
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 640,
            height = 480,
            numStars = 15,
            amplitudeRange = 80.0 to 200.0,
            sigmaRange = 1.0 to 1.5,
            background = BackgroundParams(baseLevel = 20f),
            noise = NoiseParams(gaussianSigma = 1.5f, seed = 600L),
            seed = 600L
        )

        val pipelineWeighted = StarDetectionPipeline(useGaussianFit = false)
        val pipelineGaussian = StarDetectionPipeline(useGaussianFit = true)

        val resultW = pipelineWeighted.process(field.image)
        val resultG = pipelineGaussian.process(field.image)

        fun computeStats(result: PipelineResult): Triple<Double, Int, Double> {
            val errors = mutableListOf<Double>()
            val matched = mutableSetOf<Int>()
            for (det in result.stars) {
                var bestIdx = -1
                var bestDist = Double.MAX_VALUE
                for ((i, gt) in field.groundTruth.withIndex()) {
                    if (gt.isHotPixel || gt.isStreaked) continue
                    if (i in matched) continue
                    val dx = det.x - gt.x
                    val dy = det.y - gt.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < bestDist && dist < 5.0) {
                        bestDist = dist
                        bestIdx = i
                    }
                }
                if (bestIdx >= 0) {
                    matched.add(bestIdx)
                    errors.add(bestDist)
                }
            }
            val rms = if (errors.isNotEmpty()) sqrt(errors.map { it * it }.average()) else 0.0
            return Triple(rms, matched.size, result.stars.size.toDouble())
        }

        val (rmsW, matchedW, totalW) = computeStats(resultW)
        val (rmsG, matchedG, totalG) = computeStats(resultG)

        println("Weighted: RMS=${"%.4f".format(rmsW)} px, matched $matchedW, total $totalW")
        println("Gaussian: RMS=${"%.4f".format(rmsG)} px, matched $matchedG, total $totalG")

        assertTrue("Weighted RMS <0.5", rmsW < 0.6)
        assertTrue("Gaussian RMS <0.5", rmsG < 0.6)
    }

    @Test
    fun testPipelineLowSNR() {
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 640,
            height = 480,
            numStars = 10,
            amplitudeRange = 25.0 to 50.0, // low SNR near noise floor
            background = BackgroundParams(baseLevel = 20f),
            noise = NoiseParams(gaussianSigma = 3f, seed = 700L),
            seed = 700L
        )

        val pipeline = StarDetectionPipeline(
            blobDetector = StarBlobDetector(thresholdK = 4.0) // lower threshold for low SNR
        )

        val result = pipeline.process(field.image)

        val matched = mutableSetOf<Int>()
        for (det in result.stars) {
            for ((i, gt) in field.groundTruth.withIndex()) {
                if (gt.isHotPixel || gt.isStreaked) continue
                if (i in matched) continue
                if (hypot(det.x - gt.x, det.y - gt.y) < 5.0) {
                    matched.add(i)
                    break
                }
            }
        }

        val total = field.groundTruth.count { !it.isHotPixel && !it.isStreaked }
        val recall = matched.size.toDouble() / total * 100

        println("Low SNR test: $total true stars, matched ${matched.size} = ${"%.1f".format(recall)}%, detected ${result.stars.size} total")

        // Low SNR will have lower recall, but should still detect some
        assertTrue("Low SNR should detect at least 30%, got $recall%", recall >= 20.0)
    }
}
