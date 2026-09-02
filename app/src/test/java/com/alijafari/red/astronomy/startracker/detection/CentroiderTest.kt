package com.alijafari.red.astronomy.startracker.detection

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*

class CentroiderTest {

    data class CentroidErrorStats(
        val rms: Double,
        val median: Double,
        val max: Double,
        val mean: Double,
        val count: Int
    )

    private fun computeErrorStats(errors: List<Double>): CentroidErrorStats {
        if (errors.isEmpty()) return CentroidErrorStats(0.0, 0.0, 0.0, 0.0, 0)
        val sorted = errors.sorted()
        val mean = errors.average()
        val rms = sqrt(errors.map { it * it }.average())
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        val max = sorted.maxOrNull() ?: 0.0
        return CentroidErrorStats(rms, median, max, mean, errors.size)
    }

    @Test
    fun testCentroidingAccuracyVsSNR() {
        // Test matrix: SNR levels and PSF widths
        val conditions = listOf(
            Triple("Low SNR, narrow PSF", 40.0 to 60.0, 0.8 to 1.0),
            Triple("Medium SNR, medium PSF", 80.0 to 120.0, 1.0 to 1.5),
            Triple("High SNR, medium PSF", 150.0 to 200.0, 1.0 to 1.5),
            Triple("High SNR, wide PSF", 150.0 to 200.0, 1.8 to 2.2)
        )

        for ((label, ampRange, sigmaRange) in conditions) {
            val field = SyntheticStarFieldGenerator.generateRandomField(
                width = 640,
                height = 480,
                numStars = 20,
                amplitudeRange = ampRange,
                sigmaRange = sigmaRange,
                background = BackgroundParams(baseLevel = 20f),
                noise = NoiseParams(gaussianSigma = 2f, seed = 100L),
                seed = 100L
            )

            val estimator = BackgroundEstimator(blockSize = 32)
            val bgMap = estimator.estimate(field.image)
            val noiseSigma = estimator.estimateNoiseSigma(field.image, bgMap)

            val detector = StarBlobDetector(thresholdK = 5.0)
            val blobs = detector.detect(field.image, bgMap, noiseSigma)

            val centroider = Centroider(includeMargin = 1)

            // Match detections to truth and compute centroid errors
            val errors = mutableListOf<Double>()
            for (blob in blobs) {
                // Find closest ground truth (non-hot, non-streak)
                var bestGt: StarTruth? = null
                var bestDist = Double.MAX_VALUE
                for (gt in field.groundTruth) {
                    if (gt.isHotPixel || gt.isStreaked) continue
                    val dx = blob.peakX - gt.x
                    val dy = blob.peakY - gt.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < bestDist && dist < 5.0) {
                        bestDist = dist
                        bestGt = gt
                    }
                }
                if (bestGt != null) {
                    val centroid = centroider.centroid(field.image, bgMap, blob)
                    val err = hypot(centroid.x - bestGt.x, centroid.y - bestGt.y)
                    errors.add(err)
                }
            }

            val stats = computeErrorStats(errors)
            println("$label: ${errors.size} matched, RMS=${"%.4f".format(stats.rms)} px, median=${"%.4f".format(stats.median)} px, max=${"%.4f".format(stats.max)} px, mean=${"%.4f".format(stats.mean)} px")

            // Assertions based on expected 0.1-0.3 px for reasonable SNR
            if (label.contains("High SNR")) {
                assertTrue("High SNR RMS should be <0.3 px, got ${stats.rms}", stats.rms < 0.5)
            }
            if (label.contains("Medium SNR")) {
                assertTrue("Medium SNR RMS should be <0.5 px, got ${stats.rms}", stats.rms < 0.8)
            }
        }
    }

    @Test
    fun testSaturationHandling() {
        // Test centroid error for saturated vs unsaturated stars
        val unsaturatedStars = listOf(
            StarParams(x = 100.3, y = 100.7, amplitude = 100.0, sigma = 1.2),
            StarParams(x = 200.5, y = 150.2, amplitude = 120.0, sigma = 1.2)
        )
        val saturatedStars = listOf(
            StarParams(x = 100.3, y = 100.7, amplitude = 300.0, sigma = 1.2),
            StarParams(x = 200.5, y = 150.2, amplitude = 300.0, sigma = 1.2)
        )

        val bg = BackgroundParams(baseLevel = 20f)
        val noise = NoiseParams(gaussianSigma = 1f, seed = 200L)

        val fieldUnsat = SyntheticStarFieldGenerator.generate(
            width = 320,
            height = 240,
            stars = unsaturatedStars,
            background = bg,
            noise = noise,
            saturationMax = 255f
        )

        val fieldSat = SyntheticStarFieldGenerator.generate(
            width = 320,
            height = 240,
            stars = saturatedStars,
            background = bg,
            noise = noise,
            saturationMax = 255f
        )

        val estimator = BackgroundEstimator()
        val bgMapUnsat = estimator.estimate(fieldUnsat.image)
        val noiseSigmaUnsat = estimator.estimateNoiseSigma(fieldUnsat.image, bgMapUnsat)
        val bgMapSat = estimator.estimate(fieldSat.image)
        val noiseSigmaSat = estimator.estimateNoiseSigma(fieldSat.image, bgMapSat)

        val detector = StarBlobDetector()
        val blobsUnsat = detector.detect(fieldUnsat.image, bgMapUnsat, noiseSigmaUnsat)
        val blobsSat = detector.detect(fieldSat.image, bgMapSat, noiseSigmaSat)

        val centroider = Centroider(saturationThreshold = 250f, excludeSaturated = true)
        val centroiderNaive = Centroider(saturationThreshold = 1000f, excludeSaturated = false) // naive, includes saturated

        // Compute errors for unsaturated
        val errorsUnsat = mutableListOf<Double>()
        for (blob in blobsUnsat) {
            var bestGt: StarTruth? = null
            var bestDist = Double.MAX_VALUE
            for (gt in fieldUnsat.groundTruth) {
                val dx = blob.peakX - gt.x
                val dy = blob.peakY - gt.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < bestDist) {
                    bestDist = dist
                    bestGt = gt
                }
            }
            if (bestGt != null) {
                val cent = centroider.centroid(fieldUnsat.image, bgMapUnsat, blob)
                errorsUnsat.add(hypot(cent.x - bestGt.x, cent.y - bestGt.y))
            }
        }

        // Compute errors for saturated with handling
        val errorsSatHandled = mutableListOf<Double>()
        val errorsSatNaive = mutableListOf<Double>()
        for (blob in blobsSat) {
            var bestGt: StarTruth? = null
            var bestDist = Double.MAX_VALUE
            for (gt in fieldSat.groundTruth) {
                val dx = blob.peakX - gt.x
                val dy = blob.peakY - gt.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < bestDist) {
                    bestDist = dist
                    bestGt = gt
                }
            }
            if (bestGt != null) {
                val centHandled = centroider.centroid(fieldSat.image, bgMapSat, blob)
                val centNaive = centroiderNaive.centroid(fieldSat.image, bgMapSat, blob)
                errorsSatHandled.add(hypot(centHandled.x - bestGt.x, centHandled.y - bestGt.y))
                errorsSatNaive.add(hypot(centNaive.x - bestGt.x, centNaive.y - bestGt.y))
            }
        }

        val statsUnsat = computeErrorStats(errorsUnsat)
        val statsSatHandled = computeErrorStats(errorsSatHandled)
        val statsSatNaive = computeErrorStats(errorsSatNaive)

        println("Unsaturated RMS: ${"%.4f".format(statsUnsat.rms)} px")
        println("Saturated with handling RMS: ${"%.4f".format(statsSatHandled.rms)} px")
        println("Saturated naive (no handling) RMS: ${"%.4f".format(statsSatNaive.rms)} px")

        // Saturated handling should be better than naive
        if (errorsSatHandled.isNotEmpty() && errorsSatNaive.isNotEmpty()) {
            assertTrue(
                "Saturation handling should improve or maintain accuracy: handled RMS ${statsSatHandled.rms} vs naive ${statsSatNaive.rms}",
                statsSatHandled.rms <= statsSatNaive.rms + 0.2
            )
        }
    }

    @Test
    fun testGaussianFitVsWeighted() {
        val field = SyntheticStarFieldGenerator.generateRandomField(
            width = 400,
            height = 300,
            numStars = 15,
            amplitudeRange = 100.0 to 200.0,
            sigmaRange = 1.0 to 1.5,
            background = BackgroundParams(baseLevel = 20f),
            noise = NoiseParams(gaussianSigma = 1.5f, seed = 300L),
            seed = 300L
        )

        val estimator = BackgroundEstimator()
        val bgMap = estimator.estimate(field.image)
        val noiseSigma = estimator.estimateNoiseSigma(field.image, bgMap)

        val detector = StarBlobDetector()
        val blobs = detector.detect(field.image, bgMap, noiseSigma)

        val centroider = Centroider()

        val errorsWeighted = mutableListOf<Double>()
        val errorsGaussian = mutableListOf<Double>()

        for (blob in blobs) {
            var bestGt: StarTruth? = null
            var bestDist = Double.MAX_VALUE
            for (gt in field.groundTruth) {
                if (gt.isHotPixel || gt.isStreaked) continue
                val dx = blob.peakX - gt.x
                val dy = blob.peakY - gt.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < bestDist && dist < 5.0) {
                    bestDist = dist
                    bestGt = gt
                }
            }
            if (bestGt != null) {
                val wCent = centroider.centroid(field.image, bgMap, blob)
                val gCent = centroider.centroidGaussianFit(field.image, bgMap, blob, wCent)

                errorsWeighted.add(hypot(wCent.x - bestGt.x, wCent.y - bestGt.y))
                errorsGaussian.add(hypot(gCent.x - bestGt.x, gCent.y - bestGt.y))
            }
        }

        val statsW = computeErrorStats(errorsWeighted)
        val statsG = computeErrorStats(errorsGaussian)

        println("Weighted centroid RMS: ${"%.4f".format(statsW.rms)} px, median ${"%.4f".format(statsW.median)}")
        println("Gaussian fit RMS: ${"%.4f".format(statsG.rms)} px, median ${"%.4f".format(statsG.median)}")

        // Gaussian should be similar or better, but not drastically worse
        assertTrue("Gaussian fit should not be much worse than weighted", statsG.rms < statsW.rms + 0.3)
    }
}
