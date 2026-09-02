package com.alijafari.red.astronomy.startracker.detection

import kotlin.math.*

/**
 * Background estimation via coarse grid + robust central estimate per block + bilinear interpolation.
 *
 * Approach:
 * - Divide image into blocks (e.g., 32x32)
 * - For each block, compute robust estimate (median or sigma-clipped mean) to reduce influence of stars
 * - Produce per-pixel background via bilinear interpolation between block centers
 */
class BackgroundEstimator(
    val blockSize: Int = 32,
    val useMedian: Boolean = true,
    val sigmaClip: Boolean = true,
    val sigmaClipK: Double = 3.0,
    val sigmaClipIterations: Int = 2
) {

    data class BackgroundMap(
        val width: Int,
        val height: Int,
        val perPixel: FloatArray, // same size as image, background estimate per pixel
        val blockWidth: Int,
        val blockHeight: Int,
        val blockEstimates: FloatArray, // blockWidth * blockHeight
        val globalMean: Float,
        val globalSigma: Float
    ) {
        fun getBackground(x: Int, y: Int): Float {
            if (x < 0 || x >= width || y < 0 || y >= height) return globalMean
            return perPixel[y * width + x]
        }
    }

    fun estimate(image: GrayscaleImage): BackgroundMap {
        val blocksX = (image.width + blockSize - 1) / blockSize
        val blocksY = (image.height + blockSize - 1) / blockSize
        val blockEstimates = FloatArray(blocksX * blocksY)

        // Compute per-block robust estimate
        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {
                val x0 = bx * blockSize
                val y0 = by * blockSize
                val x1 = min((bx + 1) * blockSize, image.width)
                val y1 = min((by + 1) * blockSize, image.height)

                val values = mutableListOf<Float>()
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        values.add(image.get(x, y))
                    }
                }

                val estimate = if (useMedian) {
                    robustMedian(values)
                } else {
                    robustMean(values)
                }

                val clipped = if (sigmaClip) {
                    sigmaClippedEstimate(values, estimate)
                } else {
                    estimate
                }

                blockEstimates[by * blocksX + bx] = clipped
            }
        }

        // Compute global mean and sigma from block estimates for noise estimation
        var sum = 0.0
        var sumSq = 0.0
        for (v in blockEstimates) {
            sum += v
            sumSq += v * v
        }
        val globalMean = (sum / blockEstimates.size).toFloat()
        val variance = (sumSq / blockEstimates.size) - (sum / blockEstimates.size).pow(2.0)
        val globalSigma = sqrt(max(0.0, variance)).toFloat()

        // Bilinear interpolation to per-pixel background
        val perPixel = FloatArray(image.width * image.height)

        // For each pixel, find 4 surrounding block centers and interpolate
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                // Block center positions: (bx*blockSize + blockSize/2)
                // Convert pixel to block coordinate
                val fx = (x.toDouble() / blockSize) - 0.5
                val fy = (y.toDouble() / blockSize) - 0.5

                val bx0 = floor(fx).toInt()
                val by0 = floor(fy).toInt()
                val bx1 = bx0 + 1
                val by1 = by0 + 1

                val tx = (fx - bx0).toFloat().coerceIn(0f, 1f)
                val ty = (fy - by0).toFloat().coerceIn(0f, 1f)

                val v00 = getBlockClamped(blockEstimates, blocksX, blocksY, bx0, by0, globalMean)
                val v10 = getBlockClamped(blockEstimates, blocksX, blocksY, bx1, by0, globalMean)
                val v01 = getBlockClamped(blockEstimates, blocksX, blocksY, bx0, by1, globalMean)
                val v11 = getBlockClamped(blockEstimates, blocksX, blocksY, bx1, by1, globalMean)

                // Bilinear
                val top = v00 * (1 - tx) + v10 * tx
                val bottom = v01 * (1 - tx) + v11 * tx
                val interp = top * (1 - ty) + bottom * ty

                perPixel[y * image.width + x] = interp
            }
        }

        return BackgroundMap(
            width = image.width,
            height = image.height,
            perPixel = perPixel,
            blockWidth = blocksX,
            blockHeight = blocksY,
            blockEstimates = blockEstimates,
            globalMean = globalMean,
            globalSigma = globalSigma
        )
    }

    private fun getBlockClamped(
        blocks: FloatArray,
        blocksX: Int,
        blocksY: Int,
        bx: Int,
        by: Int,
        fallback: Float
    ): Float {
        val cx = bx.coerceIn(0, blocksX - 1)
        val cy = by.coerceIn(0, blocksY - 1)
        if (bx < 0 || bx >= blocksX || by < 0 || by >= blocksY) {
            // For edges, use nearest block (clamped) — already handled by coerce, but keep fallback for empty
            return blocks[cy * blocksX + cx]
        }
        return blocks[by * blocksX + bx]
    }

    private fun robustMedian(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2f
        }
    }

    private fun robustMean(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        var sum = 0.0
        for (v in values) sum += v
        return (sum / values.size).toFloat()
    }

    private fun sigmaClippedEstimate(values: List<Float>, initial: Float): Float {
        if (values.isEmpty()) return initial
        var currentValues = values
        var estimate = initial

        for (iter in 0 until sigmaClipIterations) {
            // Compute mean and sigma of currentValues
            var sum = 0.0
            var sumSq = 0.0
            for (v in currentValues) {
                sum += v
                sumSq += v * v
            }
            val mean = sum / currentValues.size
            val variance = (sumSq / currentValues.size) - mean * mean
            val sigma = sqrt(max(0.0, variance))

            // If sigma is 0, break
            if (sigma < 1e-6) {
                estimate = mean.toFloat()
                break
            }

            // Clip values beyond k*sigma from mean
            val lower = mean - sigmaClipK * sigma
            val upper = mean + sigmaClipK * sigma
            val clipped = currentValues.filter { it >= lower && it <= upper }

            if (clipped.size < currentValues.size / 2 || clipped.isEmpty()) {
                // Too aggressive, stop
                estimate = mean.toFloat()
                break
            }

            currentValues = clipped
            estimate = mean.toFloat()
        }

        // Final median of clipped set for robustness
        return robustMedian(currentValues)
    }

    /**
     * Estimate noise sigma from background-subtracted residual.
     * Uses MAD or simple std dev on low values.
     */
    fun estimateNoiseSigma(image: GrayscaleImage, background: BackgroundMap): Float {
        // Collect residuals where residual is relatively low (to avoid stars)
        val residuals = mutableListOf<Float>()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val bg = background.perPixel[y * image.width + x]
                val res = image.get(x, y) - bg
                residuals.add(res)
            }
        }
        // Use median absolute deviation for robust sigma
        val median = robustMedian(residuals)
        val absDevs = residuals.map { abs(it - median) }
        val mad = robustMedian(absDevs)
        // For Gaussian, sigma ≈ 1.4826 * MAD
        return (mad * 1.4826f).coerceAtLeast(0.5f)
    }
}
