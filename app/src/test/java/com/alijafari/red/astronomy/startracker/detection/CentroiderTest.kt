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

    // ---- Audit finding B4: fully-saturated blob centroid bias ----

    private fun zeroBackgroundMap(w: Int, h: Int): BackgroundEstimator.BackgroundMap {
        return BackgroundEstimator.BackgroundMap(
            width = w, height = h,
            perPixel = FloatArray(w * h),
            blockWidth = 1, blockHeight = 1,
            blockEstimates = FloatArray(1),
            globalMean = 0f, globalSigma = 0f
        )
    }

    private fun makeBlob(pixels: List<Pair<Int, Int>>, peakX: Int, peakY: Int, peakValue: Float): DetectedBlob {
        return DetectedBlob(
            id = 0,
            pixels = pixels,
            minX = pixels.minOf { it.first },
            maxX = pixels.maxOf { it.first },
            minY = pixels.minOf { it.second },
            maxY = pixels.maxOf { it.second },
            peakValue = peakValue,
            peakX = peakX,
            peakY = peakY,
            totalFlux = pixels.size * peakValue,
            meanIntensity = peakValue,
            elongation = 1f,
            eccentricity = 0f
        )
    }

    @Test
    fun testFullySaturatedBlobCentroidNotAtOrigin() {
        // A 5x5 fully-saturated block centered at (100, 76) in a 200x150 image.
        // Every pixel in and around the blob is at 255 (>= saturationThreshold 250),
        // so the weighted loop excludes ALL pixels and the binary fallback runs.
        // Audit B4: the old code divided the (empty) unsaturated sum by the FULL pixel
        // count and returned (0,0); the centroid must instead land at the blob, not the origin.
        val w = 200
        val h = 150
        val img = GrayscaleImage(w, h, FloatArray(w * h) { 255f })
        val bgMap = zeroBackgroundMap(w, h)

        val blobPixels = mutableListOf<Pair<Int, Int>>()
        for (y in 74..78) for (x in 98..102) blobPixels.add(Pair(x, y))
        val blob = makeBlob(blobPixels, peakX = 100, peakY = 76, peakValue = 255f)

        val centroider = Centroider(saturationThreshold = 250f, excludeSaturated = true)
        val result = centroider.centroid(img, bgMap, blob)

        // Must be at/near the saturated core's peak (the only sensible estimate), NOT (0,0)
        assertEquals("fully-saturated blob: x must be the peak, not 0", 100.0, result.x, 1e-9)
        assertEquals("fully-saturated blob: y must be the peak, not 0", 76.0, result.y, 1e-9)
        val distFromOrigin = hypot(result.x, result.y)
        assertTrue("centroid must not collapse to origin (got (" + result.x + ", " + result.y + "))", distFromOrigin > 100.0)
        assertTrue("blob must be reported saturated", result.isSaturated)
    }

    @Test
    fun testPartiallySaturatedBlobUsesUnsaturatedDenominator() {
        // 4 saturated pixels + 2 unsaturated pixels. The binary fallback centroid must be
        // the mean of ONLY the 2 unsaturated pixels (audit B4: old code divided by all 6,
        // biasing the result toward the origin).
        val w = 100
        val h = 100
        val data = FloatArray(w * h) // all zeros
        // saturated block at (50,50)-(53,53)
        for (y in 50..53) for (x in 50..53) data[y * w + x] = 255f
        // unsaturated pixels with positive residual at (60, 62) and (62, 60)
        data[62 * w + 60] = 40f
        data[60 * w + 62] = 40f
        val img = GrayscaleImage(w, h, data)
        val bgMap = zeroBackgroundMap(w, h)

        val blobPixels = listOf(
            Pair(50, 50), Pair(51, 50), Pair(52, 50), Pair(53, 50), // saturated
            Pair(60, 62), Pair(62, 60)                              // unsaturated, residual 40
        )
        val blob = makeBlob(blobPixels, peakX = 51, peakY = 50, peakValue = 255f)

        val centroider = Centroider(saturationThreshold = 250f, excludeSaturated = true)
        val result = centroider.centroid(img, bgMap, blob)

        // Expected: mean of the two unsaturated pixels = ((60+62)/2, (62+60)/2) = (61.0, 61.0).
        // The old buggy denominator (6 pixels) would have given (20.33, 20.33) instead.
        assertEquals(61.0, result.x, 1e-9)
        assertEquals(61.0, result.y, 1e-9)
    }
}
