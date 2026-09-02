package com.alijafari.red.astronomy.startracker.detection

import kotlin.math.*

/**
 * Candidate blob from thresholding + connected components.
 */
data class DetectedBlob(
    val id: Int,
    val pixels: List<Pair<Int, Int>>, // list of (x,y)
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val peakValue: Float,
    val peakX: Int,
    val peakY: Int,
    val totalFlux: Float, // sum of residual intensities
    val meanIntensity: Float,
    val elongation: Float, // ratio of major/minor axis or width/height, 1=circular, >1 elongated
    val eccentricity: Float
)

class StarBlobDetector(
    val thresholdK: Double = 5.0, // mean + k*sigma
    val minBlobSize: Int = 3, // reject single-pixel hot pixels
    val maxBlobSize: Int = 200, // reject large clouds/moon
    val maxElongation: Float = 2.5f, // reject streaks beyond this elongation
    val useLocalNoise: Boolean = true
) {

    /**
     * Detect blobs given image and background map.
     * Returns list of candidate blobs (before centroiding).
     */
    fun detect(
        image: GrayscaleImage,
        backgroundMap: BackgroundEstimator.BackgroundMap,
        noiseSigma: Float
    ): List<DetectedBlob> {
        val width = image.width
        val height = image.height

        // Compute residual and threshold
        val residual = FloatArray(width * height)
        val thresholdMap = FloatArray(width * height)

        val globalThreshold = thresholdK * noiseSigma

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val bg = backgroundMap.perPixel[idx]
                val res = image.data[idx] - bg
                residual[idx] = res
                // Adaptive threshold: use global for now, but could be local
                thresholdMap[idx] = globalThreshold.toFloat()
            }
        }

        // Binary mask: residual > threshold
        val mask = BooleanArray(width * height) { i -> residual[i] > thresholdMap[i] }

        // Connected component labeling (4-connectivity or 8-connectivity? Use 8 for stars)
        val labels = IntArray(width * height) { 0 }
        var nextLabel = 1
        val equivalences = mutableMapOf<Int, Int>() // union-find parent

        // First pass: label
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                if (!mask[idx]) continue

                // Check neighbors: left, top-left, top, top-right (for 8-connectivity)
                val neighborLabels = mutableListOf<Int>()

                // left
                if (x > 0 && mask[idx - 1]) {
                    val lbl = labels[idx - 1]
                    if (lbl != 0) neighborLabels.add(findRoot(lbl, equivalences))
                }
                // top-left
                if (x > 0 && y > 0 && mask[idx - width - 1]) {
                    val lbl = labels[idx - width - 1]
                    if (lbl != 0) neighborLabels.add(findRoot(lbl, equivalences))
                }
                // top
                if (y > 0 && mask[idx - width]) {
                    val lbl = labels[idx - width]
                    if (lbl != 0) neighborLabels.add(findRoot(lbl, equivalences))
                }
                // top-right
                if (x < width - 1 && y > 0 && mask[idx - width + 1]) {
                    val lbl = labels[idx - width + 1]
                    if (lbl != 0) neighborLabels.add(findRoot(lbl, equivalences))
                }

                if (neighborLabels.isEmpty()) {
                    labels[idx] = nextLabel
                    equivalences[nextLabel] = nextLabel
                    nextLabel++
                } else {
                    val minLabel = neighborLabels.minOrNull()!!
                    labels[idx] = minLabel
                    // Union all neighbor labels
                    for (nl in neighborLabels) {
                        union(minLabel, nl, equivalences)
                    }
                }
            }
        }

        // Second pass: resolve labels and collect pixels per label
        val resolvedLabels = mutableMapOf<Int, MutableList<Int>>() // root label -> list of pixel indices
        for (i in labels.indices) {
            if (labels[i] == 0) continue
            val root = findRoot(labels[i], equivalences)
            labels[i] = root
            resolvedLabels.getOrPut(root) { mutableListOf() }.add(i)
        }

        // Build DetectedBlob list
        val blobs = mutableListOf<DetectedBlob>()
        var blobId = 0

        for ((root, pixelIndices) in resolvedLabels) {
            val size = pixelIndices.size

            // Size filtering
            if (size < minBlobSize) continue // reject hot pixels
            if (size > maxBlobSize) continue // reject large objects

            // Compute bounding box, peak, flux
            var minX = width
            var maxX = -1
            var minY = height
            var maxY = -1
            var peakVal = -Float.MAX_VALUE
            var peakX = 0
            var peakY = 0
            var sumFlux = 0f

            for (idx in pixelIndices) {
                val x = idx % width
                val y = idx / width
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y

                val res = residual[idx]
                sumFlux += res
                if (res > peakVal) {
                    peakVal = res
                    peakX = x
                    peakY = y
                }
            }

            val boxW = (maxX - minX + 1)
            val boxH = (maxY - minY + 1)

            // Elongation: max(boxW, boxH) / min(boxW, boxH)
            val elongation = if (min(boxW, boxH) > 0) {
                max(boxW, boxH).toFloat() / min(boxW, boxH).toFloat()
            } else 1f

            // Eccentricity estimate via second moments (simple)
            // Compute centroid of binary mask for shape analysis
            var sumX = 0.0
            var sumY = 0.0
            for (idx in pixelIndices) {
                val x = idx % width
                val y = idx / width
                sumX += x
                sumY += y
            }
            val meanX = sumX / size
            val meanY = sumY / size

            var mxx = 0.0
            var myy = 0.0
            var mxy = 0.0
            for (idx in pixelIndices) {
                val x = idx % width
                val y = idx / width
                val dx = x - meanX
                val dy = y - meanY
                mxx += dx * dx
                myy += dy * dy
                mxy += dx * dy
            }
            mxx /= size
            myy /= size
            mxy /= size

            // Eigenvalues of covariance matrix
            val trace = mxx + myy
            val det = mxx * myy - mxy * mxy
            val discriminant = max(0.0, trace * trace - 4 * det)
            val sqrtDisc = sqrt(discriminant)
            val lambda1 = (trace + sqrtDisc) / 2.0
            val lambda2 = (trace - sqrtDisc) / 2.0
            val major = sqrt(max(lambda1, lambda2))
            val minor = sqrt(max(0.0, min(lambda1, lambda2)))
            val eccentricity = if (major > 1e-6) {
                sqrt(1.0 - (minor * minor) / (major * major)).toFloat()
            } else 0f

            val meanIntensity = sumFlux / size

            // Elongation filtering
            if (elongation > maxElongation) {
                // Flag as elongated but still include? Per task: reject or flag as streak.
                // We reject here for "good star" list, but we could keep with flag.
                // For this implementation, we reject if elongation too high.
                continue
            }

            // Also reject if eccentricity very high (>0.9)
            if (eccentricity > 0.95f) {
                continue
            }

            blobs.add(
                DetectedBlob(
                    id = blobId++,
                    pixels = pixelIndices.map { idx -> Pair(idx % width, idx / width) },
                    minX = minX,
                    maxX = maxX,
                    minY = minY,
                    maxY = maxY,
                    peakValue = peakVal,
                    peakX = peakX,
                    peakY = peakY,
                    totalFlux = sumFlux,
                    meanIntensity = meanIntensity,
                    elongation = elongation,
                    eccentricity = eccentricity
                )
            )
        }

        return blobs
    }

    private fun findRoot(label: Int, parent: Map<Int, Int>): Int {
        var current = label
        var p = parent[current] ?: current
        while (p != current) {
            current = p
            p = parent[current] ?: current
        }
        return current
    }

    private fun union(a: Int, b: Int, parent: MutableMap<Int, Int>) {
        val rootA = findRoot(a, parent)
        val rootB = findRoot(b, parent)
        if (rootA != rootB) {
            parent[rootB] = rootA
        }
    }
}
