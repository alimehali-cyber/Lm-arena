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
 * store sorted by separation with a k-vector enabling bracketed range queries
 * per Mortari's k-vector technique. The k-vector array IS used by [queryRange].
 *
 * k-vector implementation:
 * - Sorted array s[0..n-1] of separations ascending
 * - n linearly spaced bins from s_min to s_max; K[bin] = index of the last element
 *   whose separation <= the bin's upper value (built in O(P) with a merge scan)
 * - A range query maps its bounds to bins, reads K[] directly for bracketing indices
 *   (O(1) array reads), then corrects by the minimum number of linear steps and
 *   exact-filters the collected slice.
 *
 * HONEST complexity (audit finding B11 correction — previously claimed flat "O(1)"):
 * - The index lookup itself is O(1); correctness is guaranteed by the correction walk.
 * - The correction walk length grows with how far the true boundary deviates from its
 *   bin-estimated position. For roughly uniform separation distributions it is a few
 *   elements (near O(1) + O(k) to emit the k results in range).
 * - On a NON-UNIFORM (clustered/skewed) separation distribution the linear binning
 *   misestimates boundary positions badly and the correction walk can grow to O(P),
 *   i.e. the WORST CASE is linear in the number of indexed pairs, not O(1).
 * - Real astronomical catalogs sit between these extremes (clustered along the Milky
 *   Way, sparse elsewhere), so treat practical behavior as O(1 + k + correction),
 *   with correction typically small but unbounded in the worst case.
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
     *
     * Audit finding B11: the k-vector array built in init was never read by this query —
     * the previous implementation re-derived approximate indices from the linear mapping
     * (m, q) and linearly walked. The query now actually uses [kVector] per Mortari's
     * k-vector range search: the k-vector bracketing indices are looked up directly, then
     * corrected by the minimum unavoidable number of steps.
     *
     * Returns EXACTLY the pairs within range (no false inclusion/omission) — verified
     * against brute force in the tests on uniform AND clustered (non-uniform) fixtures.
     */
    fun queryRange(lowRad: Double, highRad: Double): List<SeparationPair> {
        if (sortedPairs.isEmpty()) return emptyList()
        if (lowRad > sMax || highRad < sMin) return emptyList()

        val lowClamped = lowRad.coerceAtLeast(sMin)
        val highClamped = highRad.coerceAtMost(sMax)
        val n = sortedSeparations.size

        // k-vector lookup: map the query bounds to bin indices, read the precomputed
        // bracketing counts directly from the k-vector array (O(1) array reads).
        val span = sMax - sMin
        val binLow = if (span > 1e-12) ((lowClamped - sMin) / span * (n - 1)).toInt().coerceIn(0, n - 1) else 0
        val binHigh = if (span > 1e-12) ((highClamped - sMin) / span * (n - 1)).toInt().coerceIn(0, n - 1) else 0

        // K[bin] = index of the LAST element whose separation <= the bin's value.
        // Lower bound: every element at an index <= K[binLow] is <= binLow's value <= lowClamped,
        // but elements EQUAL to lowClamped (including duplicates) may sit at or before K[binLow],
        // so bracket from K[binLow], step BACK over anything still >= lowClamped, then step
        // FORWARD over anything still < lowClamped. Upper bound: elements <= highClamped may
        // extend past K[binHigh], so step FORWARD from it. (The correction steps are the
        // distribution-dependent part documented on the class; correctness is exact.)
        var lowIdx = kVector[binLow].coerceAtLeast(0)
        while (lowIdx > 0 && sortedSeparations[lowIdx - 1] >= lowClamped) {
            lowIdx--
        }
        while (lowIdx < n && sortedSeparations[lowIdx] < lowClamped) {
            lowIdx++
        }

        var highIdx = kVector[binHigh]
        while (highIdx + 1 < n && sortedSeparations[highIdx + 1] <= highClamped) {
            highIdx++
        }

        if (lowIdx > highIdx) return emptyList()
        if (lowIdx >= n) return emptyList()
        if (highIdx < 0) return emptyList()

        // Final filter guarantees exactness regardless of distribution.
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

    /*
     * Complexity summary (honest, audit finding B11):
     * - Pair computation: O(N^2) for N stars, limited by maxSeparation cutoff
     * - Sorting: O(P log P) where P = number of pairs within cutoff (P << N^2 for small cutoff)
     * - k-vector build: O(P)
     * - Range query: O(1) bracketing reads + O(correction) linear steps + O(K) to emit the
     *   K results in range. Correction is small for near-uniform separation distributions but
     *   is NOT bounded by a constant: on skewed/clustered distributions it degrades toward
     *   O(P). Compared to plain binary search (O(log P + K)), the k-vector trades a hard
     *   O(log P) bound for O(1)-typical with a linear worst case. A real 9k-star catalog
     *   benchmark remains an open item (first real JVM execution only became possible
     *   during the 2026-09-03 remediation pass).
     */
}
