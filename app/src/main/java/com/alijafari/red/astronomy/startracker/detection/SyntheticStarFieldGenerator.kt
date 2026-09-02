package com.alijafari.red.astronomy.startracker.detection

import kotlin.math.*
import java.util.Random

/**
 * Ground truth for a single injected star.
 * @param x sub-pixel center x (0..width-1)
 * @param y sub-pixel center y
 * @param amplitude peak amplitude above background
 * @param sigma PSF sigma in pixels
 * @param isStreaked true if this star was rendered as elongated streak
 * @param isSaturated true if peak would exceed saturation and was clipped
 * @param isHotPixel true if this is a hot pixel (single-pixel spike, not a PSF)
 */
data class StarTruth(
    val x: Double,
    val y: Double,
    val amplitude: Double,
    val sigma: Double,
    val isStreaked: Boolean = false,
    val isSaturated: Boolean = false,
    val isHotPixel: Boolean = false
)

data class SyntheticField(
    val image: GrayscaleImage,
    val groundTruth: List<StarTruth>
)

/**
 * Parameters for background.
 * @param baseLevel flat background level
 * @param gradXPerPixel linear gradient per pixel in x direction (simulates light pollution)
 * @param gradYPerPixel linear gradient per pixel in y direction
 */
data class BackgroundParams(
    val baseLevel: Float = 20f,
    val gradXPerPixel: Float = 0f,
    val gradYPerPixel: Float = 0f
)

/**
 * Parameters for noise.
 * @param gaussianSigma sigma of additive Gaussian noise
 * @param seed random seed for reproducibility
 */
data class NoiseParams(
    val gaussianSigma: Float = 2f,
    val seed: Long = 42L
)

data class StarParams(
    val x: Double,
    val y: Double,
    val amplitude: Double,
    val sigma: Double,
    val isStreaked: Boolean = false,
    val streakLength: Double = 0.0, // pixels, only if isStreaked
    val streakAngleDeg: Double = 0.0, // angle of streak, degrees from x-axis
    val isHotPixel: Boolean = false
)

/**
 * Synthetic star field generator — pure Kotlin, no Android dependency.
 * Renders 2D Gaussian PSF at sub-pixel positions with known ground truth.
 */
object SyntheticStarFieldGenerator {

    /**
     * Generate image from explicit star list (deterministic).
     * Returns image and ground truth.
     */
    fun generate(
        width: Int,
        height: Int,
        stars: List<StarParams>,
        background: BackgroundParams = BackgroundParams(),
        noise: NoiseParams? = NoiseParams(),
        saturationMax: Float = 255f,
        hotPixelAmplitude: Float = 200f
    ): SyntheticField {
        val image = GrayscaleImage.create(width, height, 0f)

        // Fill background with gradient
        for (y in 0 until height) {
            for (x in 0 until width) {
                val bg = background.baseLevel +
                        background.gradXPerPixel * x +
                        background.gradYPerPixel * y
                image.set(x, y, bg)
            }
        }

        val groundTruth = mutableListOf<StarTruth>()

        // Render each star
        for (sp in stars) {
            if (sp.isHotPixel) {
                // Hot pixel: single-pixel spike, no PSF
                val ix = sp.x.roundToInt()
                val iy = sp.y.roundToInt()
                if (ix in 0 until width && iy in 0 until height) {
                    image.add(ix, iy, sp.amplitude.toFloat())
                }
                groundTruth.add(
                    StarTruth(
                        x = sp.x,
                        y = sp.y,
                        amplitude = sp.amplitude,
                        sigma = sp.sigma,
                        isStreaked = false,
                        isSaturated = sp.amplitude >= saturationMax,
                        isHotPixel = true
                    )
                )
            } else if (sp.isStreaked && sp.streakLength > 0.0) {
                // Streaked star: Gaussian smeared along line
                // Sample N points along streak line
                val length = sp.streakLength
                val angleRad = Math.toRadians(sp.streakAngleDeg)
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                val numSamples = max(1, (length / (sp.sigma * 0.5)).toInt()) // sample every 0.5 sigma
                val halfLen = length / 2.0
                var peakVal = 0.0
                for (s in 0..numSamples) {
                    val t = -halfLen + (s.toDouble() / numSamples) * length
                    val cx = sp.x + t * cosA
                    val cy = sp.y + t * sinA
                    val ampPerSample = sp.amplitude / (numSamples + 1)
                    peakVal = max(peakVal, addGaussianPSF(image, cx, cy, ampPerSample, sp.sigma))
                }
                groundTruth.add(
                    StarTruth(
                        x = sp.x,
                        y = sp.y,
                        amplitude = sp.amplitude,
                        sigma = sp.sigma,
                        isStreaked = true,
                        isSaturated = peakVal >= saturationMax,
                        isHotPixel = false
                    )
                )
            } else {
                // Normal star: 2D Gaussian PSF
                val peak = addGaussianPSF(image, sp.x, sp.y, sp.amplitude, sp.sigma)
                groundTruth.add(
                    StarTruth(
                        x = sp.x,
                        y = sp.y,
                        amplitude = sp.amplitude,
                        sigma = sp.sigma,
                        isStreaked = false,
                        isSaturated = peak >= saturationMax,
                        isHotPixel = false
                    )
                )
            }
        }

        // Add noise
        if (noise != null && noise.gaussianSigma > 0f) {
            val rnd = Random(noise.seed)
            for (i in image.data.indices) {
                val n = (rnd.nextGaussian() * noise.gaussianSigma).toFloat()
                image.data[i] += n
            }
        }

        // Clip to saturation
        image.clip(0f, saturationMax)

        // After clipping, update isSaturated flag based on whether any star would have exceeded max
        // (already computed via peak, but also need to consider background+peak)
        // For simplicity, keep earlier isSaturated logic; for more accurate, we could check if raw > max before clip.
        // We already clipped, so groundTruth saturated flag remains as computed from peak estimate.

        return SyntheticField(image, groundTruth)
    }

    /**
     * Helper: add Gaussian PSF to image, return peak added value at center (for saturation check).
     * Renders within 3*sigma radius (covers 99.7% of flux).
     */
    private fun addGaussianPSF(
        image: GrayscaleImage,
        cx: Double,
        cy: Double,
        amplitude: Double,
        sigma: Double
    ): Double {
        if (sigma <= 0) return 0.0
        val radius = (3.0 * sigma).toInt() + 1
        val x0 = max(0, (cx - radius).toInt())
        val x1 = min(image.width - 1, (cx + radius).toInt())
        val y0 = max(0, (cy - radius).toInt())
        val y1 = min(image.height - 1, (cy + radius).toInt())

        var peak = 0.0
        val sigma2 = sigma * sigma
        val twoSigma2 = 2.0 * sigma2

        for (y in y0..y1) {
            for (x in x0..x1) {
                val dx = x - cx
                val dy = y - cy
                val r2 = dx * dx + dy * dy
                val value = amplitude * exp(-r2 / twoSigma2)
                if (value > 0.01) { // threshold to avoid adding negligible tails
                    image.add(x, y, value.toFloat())
                    if (abs(dx) < 0.5 && abs(dy) < 0.5) {
                        // approximate peak at nearest pixel to center
                        if (value > peak) peak = value
                    }
                }
            }
        }
        // More accurate peak at exact center (not pixel center)
        // For saturation check, use amplitude directly (peak of continuous PSF)
        return amplitude
    }

    /**
     * Convenience: generate random field with N stars.
     */
    fun generateRandomField(
        width: Int = 640,
        height: Int = 480,
        numStars: Int = 20,
        amplitudeRange: Pair<Double, Double> = 50.0 to 200.0,
        sigmaRange: Pair<Double, Double> = 0.8 to 1.8,
        background: BackgroundParams = BackgroundParams(baseLevel = 20f),
        noise: NoiseParams? = NoiseParams(gaussianSigma = 2f, seed = 42L),
        saturationMax: Float = 255f,
        margin: Int = 20,
        seed: Long = 1234L,
        includeHotPixels: Int = 0,
        includeStreaks: Int = 0
    ): SyntheticField {
        val rnd = Random(seed)
        val stars = mutableListOf<StarParams>()

        for (i in 0 until numStars) {
            val x = margin + rnd.nextDouble() * (width - 2 * margin)
            val y = margin + rnd.nextDouble() * (height - 2 * margin)
            val amp = amplitudeRange.first + rnd.nextDouble() * (amplitudeRange.second - amplitudeRange.first)
            val sigma = sigmaRange.first + rnd.nextDouble() * (sigmaRange.second - sigmaRange.first)
            stars.add(StarParams(x, y, amp, sigma))
        }

        for (i in 0 until includeHotPixels) {
            val x = rnd.nextInt(width).toDouble()
            val y = rnd.nextInt(height).toDouble()
            stars.add(StarParams(x, y, 200.0, 0.5, isHotPixel = true))
        }

        for (i in 0 until includeStreaks) {
            val x = margin + rnd.nextDouble() * (width - 2 * margin)
            val y = margin + rnd.nextDouble() * (height - 2 * margin)
            val amp = amplitudeRange.first + rnd.nextDouble() * (amplitudeRange.second - amplitudeRange.first)
            val sigma = sigmaRange.first + rnd.nextDouble() * (sigmaRange.second - sigmaRange.first)
            val length = 5.0 + rnd.nextDouble() * 15.0
            val angle = rnd.nextDouble() * 180.0
            stars.add(StarParams(x, y, amp, sigma, isStreaked = true, streakLength = length, streakAngleDeg = angle))
        }

        return generate(width, height, stars, background, noise, saturationMax)
    }
}
