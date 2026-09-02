package com.alijafari.red.astronomy.startracker.diagnostics

/**
 * Frame quality classification based on blob stats.
 * Pure Kotlin, no Android dependency.
 */

enum class FrameQuality {
    GOOD,
    POOR_LOW_STARS,
    POOR_HIGH_NOISE,
    POOR_BLUR,
    POOR_OVEREXPOSED,
    UNKNOWN
}

data class BlobStats(
    val blobCount: Int,
    val meanBrightness: Double,
    val brightnessStd: Double,
    val meanSize: Double,
    val sizeStd: Double,
    val backgroundMean: Double,
    val backgroundStd: Double,
    val maxBrightness: Double
)

class FrameQualityClassifier(
    val minStarsForGood: Int = 5,
    val lowStarsThreshold: Int = 2,
    val highNoiseThreshold: Double = 50.0, // background std threshold
    val blurSizeThreshold: Double = 2.0, // if mean blob size too small and low brightness std -> blur
    val overexposedMeanThreshold: Double = 200.0,
    val overexposedMaxThreshold: Double = 250.0
) {

    fun classify(stats: BlobStats): FrameQuality {
        // Overexposed check first
        if (stats.backgroundMean > overexposedMeanThreshold || stats.maxBrightness > overexposedMaxThreshold) {
            // But also need high background mean
            if (stats.backgroundMean > overexposedMeanThreshold && stats.blobCount < minStarsForGood) {
                return FrameQuality.POOR_OVEREXPOSED
            }
        }

        // Low stars
        if (stats.blobCount < lowStarsThreshold) {
            return FrameQuality.POOR_LOW_STARS
        }

        // High noise: background std high, many small blobs but low mean brightness
        if (stats.backgroundStd > highNoiseThreshold && stats.blobCount > minStarsForGood * 3) {
            // Many blobs but noisy background suggests false detections
            return FrameQuality.POOR_HIGH_NOISE
        }

        // Blur: blobs small, low brightness std, low mean brightness
        if (stats.meanSize < blurSizeThreshold && stats.brightnessStd < 10.0 && stats.meanBrightness < 50.0) {
            return FrameQuality.POOR_BLUR
        }

        // Good if enough stars and reasonable stats
        if (stats.blobCount >= minStarsForGood && stats.backgroundStd < highNoiseThreshold) {
            return FrameQuality.GOOD
        }

        // Borderline cases
        return if (stats.blobCount >= lowStarsThreshold) {
            FrameQuality.GOOD
        } else {
            FrameQuality.POOR_LOW_STARS
        }
    }

    fun classifyWithReason(stats: BlobStats): Pair<FrameQuality, FailureReason?> {
        val quality = classify(stats)
        val reason = when (quality) {
            FrameQuality.GOOD -> null
            FrameQuality.POOR_LOW_STARS -> FailureReason.TooFewStars(stats.blobCount, lowStarsThreshold)
            FrameQuality.POOR_HIGH_NOISE -> FailureReason.LowFrameQuality(quality)
            FrameQuality.POOR_BLUR -> FailureReason.LowFrameQuality(quality)
            FrameQuality.POOR_OVEREXPOSED -> FailureReason.LowFrameQuality(quality)
            FrameQuality.UNKNOWN -> FailureReason.Unknown
        }
        return Pair(quality, reason)
    }
}
