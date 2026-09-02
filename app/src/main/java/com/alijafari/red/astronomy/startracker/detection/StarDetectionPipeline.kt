package com.alijafari.red.astronomy.startracker.detection

/**
 * Pipeline orchestration: background estimation -> blob detection -> centroiding.
 * Pure Kotlin, no Android dependency, operates on GrayscaleImage only.
 */

data class DetectedStar(
    val x: Double, // sub-pixel centroid x
    val y: Double, // sub-pixel centroid y
    val flux: Double, // estimated flux
    val peakValue: Float,
    val elongation: Float,
    val eccentricity: Float,
    val isSaturated: Boolean,
    val isElongated: Boolean = false,
    val rmsWidth: Double,
    val blobId: Int
)

data class PipelineResult(
    val stars: List<DetectedStar>,
    val backgroundMap: BackgroundEstimator.BackgroundMap,
    val noiseSigma: Float,
    val blobs: List<DetectedBlob>
)

class StarDetectionPipeline(
    val backgroundEstimator: BackgroundEstimator = BackgroundEstimator(blockSize = 32),
    val blobDetector: StarBlobDetector = StarBlobDetector(),
    val centroider: Centroider = Centroider(),
    val useGaussianFit: Boolean = false // optional stretch
) {

    fun process(image: GrayscaleImage): PipelineResult {
        // Step 1: Background estimation
        val bgMap = backgroundEstimator.estimate(image)
        val noiseSigma = backgroundEstimator.estimateNoiseSigma(image, bgMap)

        // Step 2: Blob detection
        val blobs = blobDetector.detect(image, bgMap, noiseSigma)

        // Step 3: Centroiding
        val stars = mutableListOf<DetectedStar>()
        for (blob in blobs) {
            val centroid = if (useGaussianFit) {
                val weighted = centroider.centroid(image, bgMap, blob)
                centroider.centroidGaussianFit(image, bgMap, blob, weighted)
            } else {
                centroider.centroid(image, bgMap, blob)
            }

            stars.add(
                DetectedStar(
                    x = centroid.x,
                    y = centroid.y,
                    flux = centroid.flux,
                    peakValue = blob.peakValue,
                    elongation = blob.elongation,
                    eccentricity = blob.eccentricity,
                    isSaturated = centroid.isSaturated,
                    isElongated = blob.elongation > 2.0f,
                    rmsWidth = centroid.rmsWidth,
                    blobId = blob.id
                )
            )
        }

        return PipelineResult(
            stars = stars,
            backgroundMap = bgMap,
            noiseSigma = noiseSigma,
            blobs = blobs
        )
    }
}
