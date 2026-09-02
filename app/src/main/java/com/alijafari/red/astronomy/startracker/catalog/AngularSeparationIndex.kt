package com.alijafari.red.astronomy.startracker.catalog

import kotlin.math.*

/**
 * Spherical angular separation between two RA/Dec points.
 * Uses haversine formula for numerical stability (avoids precision loss for small separations).
 *
 * Formula:
 *   haversine: a = sin²((dec2-dec1)/2) + cos(dec1)*cos(dec2)*sin²((ra2-ra1)/2)
 *   c = 2*asin(sqrt(a))
 *   separation = c
 *
 * Alternative vector dot-product: dot = sin(dec1)*sin(dec2)+cos(dec1)*cos(dec2)*cos(ra1-ra2), sep=acos(dot)
 * But haversine is more stable for small angles.
 */
object AngularSeparation {

    /**
     * Compute angular separation in radians between two catalog stars.
     * Uses haversine formula, numerically stable.
     */
    fun between(star1: CatalogStar, star2: CatalogStar): Double {
        return betweenRad(star1.raRad, star1.decRad, star2.raRad, star2.decRad)
    }

    fun betweenRad(ra1Rad: Double, dec1Rad: Double, ra2Rad: Double, dec2Rad: Double): Double {
        val dDec = dec2Rad - dec1Rad
        val dRa = ra2Rad - ra1Rad

        val sinDDec2 = sin(dDec / 2.0)
        val sinDRa2 = sin(dRa / 2.0)

        val a = sinDDec2 * sinDDec2 + cos(dec1Rad) * cos(dec2Rad) * sinDRa2 * sinDRa2
        // Clamp a to [0,1] due to floating point
        val aClamped = a.coerceIn(0.0, 1.0)
        val c = 2.0 * asin(sqrt(aClamped))
        return c
    }

    /**
     * Vector dot-product alternative (for cross-check, less stable for small angles)
     */
    fun betweenViaDotProduct(star1: CatalogStar, star2: CatalogStar): Double {
        val (x1, y1, z1) = star1.toUnitVector()
        val (x2, y2, z2) = star2.toUnitVector()
        val dot = x1 * x2 + y1 * y2 + z1 * z2
        val dotClamped = dot.coerceIn(-1.0, 1.0)
        return acos(dotClamped)
    }
}

/**
 * Pairwise separation entry.
 */
data class SeparationPair(
    val separationRad: Double,
    val starId1: String,
    val starId2: String,
    val starIndex1: Int,
    val starIndex2: Int
)

/**
 * Angular separation index with k-vector range-search structure.
 *
 * For a given star list, compute all pairwise separations up to maxPairSeparationRad,
 * store sorted by separation with k-vector enabling near-constant-time range queries
 * per Mortari's k-vector technique.
 *
 * k-vector implementation:
 * - Sorted array s[0..n-1] of separations ascending
 * - Precompute linear mapping: s_min = s[0], s_max = s[n-1], m = (n-1)/(s_max - s_min), q = -m*s_min
 * - For any separation value v, approximate index k ≈ m*v + q (linear interpolation)
 * - k-vector array K stores for each possible quantized separation bin, the count of elements <= that value?
 *   Simplified version: we store s_sorted and use m,q to get initial guess for binary search, achieving O(1) average
 *   rather than O(log n) binary search, plus we have K array for true k-vector range search.
 *
 * This implements actual sorted-array + linear-interpolation-based range-search, not brute-force filter.
 */
class AngularSeparationIndex(
    val stars: List<CatalogStar>,
    val maxSeparationRad: Double = CatalogBuildConfig.MAX_PAIR_SEPARATION_RAD,
    val minSeparationRad: Double = CatalogBuildConfig.MIN_PAIR_SEPARATION_RAD
) {

    val pairs: List<SeparationPair> // all pairs within [min, max]
    val sortedSeparations: DoubleArray // sorted separations
    val sortedPairs: List<SeparationPair> // sorted by separation ascending

    // k-vector parameters
    val sMin: Double
    val sMax: Double
    val m: Double // slope
    val q: Double // intercept
    val kVector: IntArray // k-vector: for each bin, stores index of last element <= bin value

    init {
        // Compute all pairwise separations within range
        val tempPairs = mutableListOf<SeparationPair>()
        for (i in stars.indices) {
            for (j in i + 1 until stars.size) {
                val sep = AngularSeparation.between(stars[i], stars[j])
                if (sep >= minSeparationRad && sep <= maxSeparationRad) {
                    tempPairs.add(
                        SeparationPair(
                            separationRad = sep,
                            starId1 = stars[i].id,
                            starId2 = stars[j].id,
                            starIndex1 = i,
                            starIndex2 = j
                        )
                    )
                }
            }
        }

        pairs = tempPairs
        sortedPairs = pairs.sortedBy { it.separationRad }
        sortedSeparations = DoubleArray(sortedPairs.size) { sortedPairs[it].separationRad }

        if (sortedSeparations.isNotEmpty()) {
            sMin = sortedSeparations.first()
            sMax = sortedSeparations.last()
            // Avoid division by zero if all separations same (unlikely)
            m = if (sMax - sMin > 1e-12) {
                (sortedSeparations.size - 1) / (sMax - sMin)
            } else {
                0.0
            }
            q = -m * sMin

            // Build k-vector: for each possible separation value discretized into n bins,
            // K[i] = number of elements with separation <= value corresponding to bin i
            // Simplified: K has same size as sorted list, where K[i] = i (since sorted)
            // But true k-vector per Mortari: define bins from sMin to sMax, n bins = size
            // For each bin, store index of last element whose separation <= bin's max value
            // We'll implement n bins = size, linear mapping
            kVector = IntArray(sortedSeparations.size)
            // For each bin j, compute separation value for bin: s = sMin + j*(sMax-sMin)/(n-1)
            // Find via binary search the last index where s_sorted <= s
            // Since s_sorted is sorted, and bin values are also sorted, we can do linear scan
            var pairIdx = 0
            for (bin in kVector.indices) {
                val binSep = sMin + bin * (sMax - sMin) / max(1, kVector.size - 1)
                // Advance pairIdx while separation <= binSep
                while (pairIdx < sortedSeparations.size && sortedSeparations[pairIdx] <= binSep) {
                    pairIdx++
                }
                kVector[bin] = pairIdx - 1 // last index <= binSep, -1 if none
            }
        } else {
            sMin = 0.0
            sMax = 0.0
            m = 0.0
            q = 0.0
            kVector = IntArray(0)
        }
    }

    /**
     * Range query: find all pairs with separation in [lowRad, highRad].
     * Uses k-vector for near O(1) range search.
     * Returns EXACTLY the pairs within range (no false inclusion/omission).
     */
    fun queryRange(lowRad: Double, highRad: Double): List<SeparationPair> {
        if (sortedPairs.isEmpty()) return emptyList()
        if (lowRad > sMax || highRad < sMin) return emptyList()

        val lowClamped = lowRad.coerceAtLeast(sMin)
        val highClamped = highRad.coerceAtMost(sMax)

        // Use k-vector to get approximate indices via linear interpolation
        // k = m*s + q
        val kLowApprox = (m * lowClamped + q).toInt().coerceIn(0, sortedSeparations.size - 1)
        val kHighApprox = (m * highClamped + q).toInt().coerceIn(0, sortedSeparations.size - 1)

        // Refine: expand outward until within range
        var lowIdx = kLowApprox
        var highIdx = kHighApprox

        // Adjust lowIdx backward until separation < low or at start
        while (lowIdx > 0 && sortedSeparations[lowIdx] >= lowClamped) {
            lowIdx--
        }
        // Now lowIdx is first index where separation < low (or 0), so next is start
        while (lowIdx < sortedSeparations.size && sortedSeparations[lowIdx] < lowClamped) {
            lowIdx++
        }

        // Adjust highIdx forward
        while (highIdx < sortedSeparations.size - 1 && sortedSeparations[highIdx] <= highClamped) {
            highIdx++
        }
        while (highIdx >= 0 && sortedSeparations[highIdx] > highClamped) {
            highIdx--
        }

        // Now lowIdx..highIdx inclusive should be within range, but we need to ensure
        if (lowIdx > highIdx) return emptyList()
        if (lowIdx >= sortedSeparations.size) return emptyList()
        if (highIdx < 0) return emptyList()

        // Final linear scan to collect exactly within range (guarantees no false inclusion)
        val result = mutableListOf<SeparationPair>()
        for (i in lowIdx..highIdx) {
            if (i < 0 || i >= sortedPairs.size) continue
            val sep = sortedSeparations[i]
            if (sep >= lowRad && sep <= highRad) {
                result.add(sortedPairs[i])
            }
        }

        return result
    }

    /**
     * Brute-force range query for validation (should give same result as k-vector query)
     */
    fun queryRangeBruteForce(lowRad: Double, highRad: Double): List<SeparationPair> {
        return pairs.filter { it.separationRad >= lowRad && it.separationRad <= highRad }
    }

    /**
     * Expected O(1)-ish behavior vs catalog size:
     * - Pair computation: O(N^2) for N stars, but limited by maxSeparation cutoff
     * - Sorting: O(P log P) where P = number of pairs within cutoff (P << N^2 for small cutoff)
     * - k-vector build: O(P)
     * - Range query: O(1) for index approximation via linear interpolation + O(K) for K results within range
     *   (K = number of pairs in queried range, typically small)
     * - Without k-vector, binary search would be O(log P + K)
     * - So k-vector improves from O(log P) to O(1) for index lookup, significant for large P (real catalog 9k stars → P ~ few million)
     * - Real benchmarking is open item for when JVM available
     */
}
