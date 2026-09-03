package com.alijafari.red.astronomy.startracker.detection

import kotlin.math.*

/**
 * Sub-pixel centroiding.
 * Required baseline: intensity-weighted centroid (center of mass) over blob pixels
 * using background-subtracted intensity as weight, with small margin of surrounding pixels.
 *
 * Saturation handling: if blob contains pixels at/near saturation ceiling, exclude saturated core
 * from weighting (centroid on unsaturated wings only) to avoid bias.
 */
class Centroider(
    val includeMargin: Int = 1, // include 1-pixel margin around blob bounding box
    val saturationThreshold: Float = 250f, // pixel value at which we consider saturated (for 0-255 range)
    val excludeSaturated: Boolean = true
) {

    data class CentroidResult(
        val x: Double, // sub-pixel centroid x
        val y: Double, // sub-pixel centroid y
        val flux: Double, // total flux used
        val numPixels: Int,
        val isSaturated: Boolean,
        val rmsWidth: Double // estimated width (sqrt of second moment)
    )

    /**
     * Compute centroid for a single blob.
     * @param image original image
     * @param backgroundMap background map
     * @param blob detected blob
     * @return centroid result
     */
    fun centroid(
        image: GrayscaleImage,
        backgroundMap: BackgroundEstimator.BackgroundMap,
        blob: DetectedBlob
    ): CentroidResult {
        val width = image.width
        val height = image.height

        // Determine region to include: blob bounding box + margin
        val x0 = max(0, blob.minX - includeMargin)
        val x1 = min(width - 1, blob.maxX + includeMargin)
        val y0 = max(0, blob.minY - includeMargin)
        val y1 = min(height - 1, blob.maxY + includeMargin)

        var sumW = 0.0
        var sumX = 0.0
        var sumY = 0.0
        var sumX2 = 0.0
        var sumY2 = 0.0
        var count = 0
        var isSaturated = false
        var flux = 0.0

        // For saturation handling, we need to know which pixels are saturated
        val saturatedPixels = mutableListOf<Pair<Int, Int>>()

        for (y in y0..y1) {
            for (x in x0..x1) {
                val idx = y * width + x
                val rawVal = image.data[idx]
                val bg = backgroundMap.perPixel[idx]
                val residual = (rawVal - bg).toDouble()

                // Check saturation on raw value
                if (rawVal >= saturationThreshold) {
                    isSaturated = true
                    saturatedPixels.add(Pair(x, y))
                    if (excludeSaturated) {
                        continue // exclude saturated core
                    }
                }

                // Only include positive residuals (above background)
                if (residual <= 0) continue

                // Weight by residual intensity
                val w = residual

                sumW += w
                sumX += w * x
                sumY += w * y
                sumX2 += w * x * x
                sumY2 += w * y * y
                flux += w
                count++
            }
        }

        // If we excluded all pixels due to saturation, fall back to using unsaturated wings only
        // or if sumW is 0, fall back to geometric center of blob
        val cx: Double
        val cy: Double

        if (sumW > 1e-9) {
            cx = sumX / sumW
            cy = sumY / sumW
        } else {
            // Fallback: unweighted centroid of the blob's NON-saturated pixels (binary centroid).
            // Audit finding B4: this previously divided by blob.pixels.size (the FULL pixel count)
            // while summing only unsaturated coordinates, so a fully-saturated blob summed nothing
            // and returned (0,0) — biased toward the origin instead of the blob's location.
            // The denominator now matches the numerator's pixel set.
            var sx = 0.0
            var sy = 0.0
            var unsaturatedCount = 0
            for ((x, y) in blob.pixels) {
                // Skip saturated if excluding
                if (excludeSaturated) {
                    val raw = image.get(x, y)
                    if (raw >= saturationThreshold) continue
                }
                sx += x
                sy += y
                unsaturatedCount++
            }
            if (unsaturatedCount > 0) {
                cx = sx / unsaturatedCount
                cy = sy / unsaturatedCount
            } else {
                // Ultimate fallback: peak position (all blob pixels saturated — the saturated
                // core's peak is the best available estimate of the star's location, NOT (0,0))
                cx = blob.peakX.toDouble()
                cy = blob.peakY.toDouble()
            }
        }

        // Estimate rms width from second moments
        val rmsWidth = if (sumW > 1e-9) {
            val meanX2 = sumX2 / sumW
            val meanY2 = sumY2 / sumW
            val varX = meanX2 - cx * cx
            val varY = meanY2 - cy * cy
            sqrt(max(0.0, (varX + varY) / 2.0))
        } else {
            1.0
        }

        return CentroidResult(
            x = cx,
            y = cy,
            flux = flux,
            numPixels = count,
            isSaturated = isSaturated,
            rmsWidth = rmsWidth
        )
    }

    /**
     * OPTIONAL/STRETCH: 2D Gaussian least-squares fit as higher-precision mode.
     * Simple iterative approach: fit Gaussian to residual data in region.
     * This is a simplified version, not full Levenberg-Marquardt, but gives improved centroid
     * over weighted centroid for high SNR.
     *
     * Model: I(x,y) = A * exp(-((x-cx)^2 + (y-cy)^2)/(2*sigma^2))
     * We fit cx, cy, A, sigma via simple gradient descent / moment refinement.
     */
    fun centroidGaussianFit(
        image: GrayscaleImage,
        backgroundMap: BackgroundEstimator.BackgroundMap,
        blob: DetectedBlob,
        initial: CentroidResult? = null,
        maxIterations: Int = 20
    ): CentroidResult {
        // Start with weighted centroid as initial guess
        val init = initial ?: centroid(image, backgroundMap, blob)

        var cx = init.x
        var cy = init.y
        var sigma = init.rmsWidth.coerceIn(0.5, 3.0)
        var amplitude = blob.peakValue.toDouble()

        val width = image.width
        val height = image.height

        val x0 = max(0, blob.minX - includeMargin)
        val x1 = min(width - 1, blob.maxX + includeMargin)
        val y0 = max(0, blob.minY - includeMargin)
        val y1 = min(height - 1, blob.maxY + includeMargin)

        // Collect data points
        val points = mutableListOf<Triple<Double, Double, Double>>() // x,y,residual
        for (y in y0..y1) {
            for (x in x0..x1) {
                val idx = y * width + x
                val raw = image.data[idx]
                if (excludeSaturated && raw >= saturationThreshold) continue
                val bg = backgroundMap.perPixel[idx]
                val res = (raw - bg).toDouble()
                if (res <= 0) continue
                points.add(Triple(x.toDouble(), y.toDouble(), res))
            }
        }

        if (points.isEmpty()) return init

        // Simple iterative refinement: at each iteration, compute weighted centroid with Gaussian weights
        // This is not true least-squares but improves over plain centroid by down-weighting outliers
        for (iter in 0 until maxIterations) {
            var sumW = 0.0
            var sumX = 0.0
            var sumY = 0.0
            var sumR2 = 0.0
            var sumAmp = 0.0

            for ((x, y, res) in points) {
                val dx = x - cx
                val dy = y - cy
                val r2 = dx * dx + dy * dy
                // Gaussian weight for current estimate
                val gauss = exp(-r2 / (2 * sigma * sigma))
                // Weight by residual * gauss (robust)
                val w = res * gauss

                sumW += w
                sumX += w * x
                sumY += w * y
                sumR2 += w * r2
                sumAmp += res
            }

            if (sumW < 1e-9) break

            val newCx = sumX / sumW
            val newCy = sumY / sumW
            val newSigma = sqrt(max(0.1, sumR2 / (2 * sumW))) // from <r2> = 2*sigma^2

            // Check convergence
            val delta = hypot(newCx - cx, newCy - cy)
            cx = newCx
            cy = newCy
            sigma = (sigma * 0.5 + newSigma * 0.5).coerceIn(0.3, 5.0) // damped update

            if (delta < 1e-4) break
        }

        return CentroidResult(
            x = cx,
            y = cy,
            flux = init.flux,
            numPixels = init.numPixels,
            isSaturated = init.isSaturated,
            rmsWidth = sigma
        )
    }
}
