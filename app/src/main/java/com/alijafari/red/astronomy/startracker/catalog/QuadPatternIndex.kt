package com.alijafari.red.astronomy.startracker.catalog

import kotlin.math.*

/**
 * Quad pattern: 4 stars, 6 pairwise separations, reduced to scale/rotation-invariant descriptor.
 *
 * Formulation used (Tetra3-lineage approach, documented):
 * - Given 4 stars (a,b,c,d), compute 6 angular separations:
 *   d_ab, d_ac, d_ad, d_bc, d_bd, d_cd
 * - Find largest separation d_max (baseline)
 * - Compute 5 ratios: d_i / d_max for the other 5 separations
 * - Sort ratios ascending to achieve rotation invariance (order of stars doesn't matter)
 * - Descriptor = sorted list of 5 ratios in [0,1]
 *
 * Why this formulation:
 * - Scale invariant: dividing by d_max removes absolute scale (distance)
 * - Rotation invariant: sorting ratios removes ordering dependency
 * - Translation invariant: uses only relative separations, not absolute positions
 * - Similar to Tetra3 which uses 4 stars and 6 separations normalized by max
 * - Alternative would be 2D geometric hash (choose baseline pair, compute coordinates of other 2 in baseline frame),
 *   but ratio method is simpler, fully rotation invariant, and easier to hand-verify
 *
 * Hash quantization: bin descriptor into grid with bin width = CatalogBuildConfig.HASH_BIN_WIDTH
 * (default 0.01 = 1% tolerance) to tolerate centroiding noise.
 */
data class QuadDescriptor(
    val ratios: List<Double>, // 5 ratios sorted ascending, each in [0,1]
    val maxSeparationRad: Double, // d_max, for reference
    val starIndices: List<Int>, // 4 star indices in original catalog
    val starIds: List<String>
) {
    /**
     * Quantized key for hash table: bin each ratio into integer bin = floor(ratio / binWidth)
     * Key = "bin0-bin1-bin2-bin3-bin4" e.g., "10-20-30-40-50"
     */
    fun quantizedKey(binWidth: Double = CatalogBuildConfig.HASH_BIN_WIDTH): String {
        return ratios.joinToString("-") { r ->
            val bin = floor(r / binWidth).toInt()
            bin.toString()
        }
    }
}

data class CatalogQuad(
    val starIndices: List<Int>, // 4 indices
    val starIds: List<String>,
    val descriptor: QuadDescriptor,
    val quantizedKey: String
)

class QuadPatternIndex(
    val stars: List<CatalogStar>,
    val maxSeparationRad: Double = CatalogBuildConfig.MAX_PAIR_SEPARATION_RAD,
    val minSeparationRad: Double = CatalogBuildConfig.MIN_PAIR_SEPARATION_RAD,
    val binWidth: Double = CatalogBuildConfig.HASH_BIN_WIDTH
) {

    val quads: List<CatalogQuad>
    val hashTable: Map<String, List<CatalogQuad>> // quantized key -> list of quads

    init {
        val tempQuads = mutableListOf<CatalogQuad>()

        // Offline index construction: enumerate quads formed from stars within maxSeparation of each other
        // Do NOT do full combinatorial explosion over whole catalog — use maxSeparation to limit
        // For each combination of 4 stars, check if all 6 pairwise separations <= maxSeparation
        // For small test fixture (10-20 stars), this is feasible. For real catalog 9k stars, need smarter:
        // - For each star, find nearby stars within maxSeparation using AngularSeparationIndex
        // - Only combine nearby stars

        // For this phase, implement brute-force with maxSeparation filter (works for test fixture)
        // For larger catalog, would need optimization, but we document quad count for fixture

        for (i in stars.indices) {
            for (j in i + 1 until stars.size) {
                val sepIJ = AngularSeparation.between(stars[i], stars[j])
                if (sepIJ < minSeparationRad || sepIJ > maxSeparationRad) continue

                for (k in j + 1 until stars.size) {
                    val sepIK = AngularSeparation.between(stars[i], stars[k])
                    val sepJK = AngularSeparation.between(stars[j], stars[k])
                    if (sepIK < minSeparationRad || sepIK > maxSeparationRad) continue
                    if (sepJK < minSeparationRad || sepJK > maxSeparationRad) continue

                    for (l in k + 1 until stars.size) {
                        val sepIL = AngularSeparation.between(stars[i], stars[l])
                        val sepJL = AngularSeparation.between(stars[j], stars[l])
                        val sepKL = AngularSeparation.between(stars[k], stars[l])
                        if (sepIL < minSeparationRad || sepIL > maxSeparationRad) continue
                        if (sepJL < minSeparationRad || sepJL > maxSeparationRad) continue
                        if (sepKL < minSeparationRad || sepKL > maxSeparationRad) continue

                        // All 6 separations within [min, max], so this quad is within region
                        val indices = listOf(i, j, k, l)
                        val ids = listOf(stars[i].id, stars[j].id, stars[k].id, stars[l].id)

                        val separations = listOf(
                            sepIJ, sepIK, sepIL, sepJK, sepJL, sepKL
                        )
                        val maxSep = separations.maxOrNull() ?: 0.0
                        if (maxSep < 1e-9) continue

                        val ratios = separations.filter { it != maxSep || separations.count { s -> s == maxSep } > 1 }
                            // If multiple separations equal max, we need to handle: take 5 smallest ratios excluding one max
                            // Simpler: sort separations descending, take 5 smallest / max
                        val sortedSeps = separations.sortedDescending()
                        val dMax = sortedSeps[0]
                        val otherSeps = sortedSeps.drop(1) // 5 other separations
                        val ratioList = otherSeps.map { it / dMax }.sorted()

                        val descriptor = QuadDescriptor(
                            ratios = ratioList,
                            maxSeparationRad = dMax,
                            starIndices = indices,
                            starIds = ids
                        )

                        val key = descriptor.quantizedKey(binWidth)

                        tempQuads.add(
                            CatalogQuad(
                                starIndices = indices,
                                starIds = ids,
                                descriptor = descriptor,
                                quantizedKey = key
                            )
                        )
                    }
                }
            }
        }

        quads = tempQuads

        // Build hash table
        val table = mutableMapOf<String, MutableList<CatalogQuad>>()
        for (quad in quads) {
            table.getOrPut(quad.quantizedKey) { mutableListOf() }.add(quad)
        }
        hashTable = table
    }

    /**
     * Compute descriptor for observed quad (4 observed unit vectors or RA/Dec).
     * Same formulation as catalog quads.
     */
    fun computeDescriptorForObserved(
        observedStars: List<CatalogStar> // 4 stars with RA/Dec (or unit vectors) representing observed
    ): QuadDescriptor {
        require(observedStars.size == 4) { "Need exactly 4 stars for quad" }

        val seps = mutableListOf<Double>()
        for (i in observedStars.indices) {
            for (j in i + 1 until observedStars.size) {
                seps.add(AngularSeparation.between(observedStars[i], observedStars[j]))
            }
        }

        val maxSep = seps.maxOrNull() ?: 0.0
        val sortedSeps = seps.sortedDescending()
        val dMax = sortedSeps[0]
        val otherSeps = sortedSeps.drop(1)
        val ratios = otherSeps.map { it / dMax }.sorted()

        return QuadDescriptor(
            ratios = ratios,
            maxSeparationRad = maxSep,
            starIndices = emptyList(), // observed, not catalog indices
            starIds = observedStars.map { it.id }
        )
    }

    /**
     * Alternative: compute descriptor from unit vectors directly (for solver phase)
     */
    fun computeDescriptorFromUnitVectors(
        unitVectors: List<Triple<Double, Double, Double>> // 4 unit vectors
    ): QuadDescriptor {
        require(unitVectors.size == 4)

        val seps = mutableListOf<Double>()
        for (i in unitVectors.indices) {
            for (j in i + 1 until unitVectors.size) {
                val (x1, y1, z1) = unitVectors[i]
                val (x2, y2, z2) = unitVectors[j]
                val dot = (x1 * x2 + y1 * y2 + z1 * z2).coerceIn(-1.0, 1.0)
                val sep = acos(dot)
                seps.add(sep)
            }
        }

        val maxSep = seps.maxOrNull() ?: 0.0
        val sortedSeps = seps.sortedDescending()
        val dMax = sortedSeps[0]
        val otherSeps = sortedSeps.drop(1)
        val ratios = otherSeps.map { it / dMax }.sorted()

        return QuadDescriptor(
            ratios = ratios,
            maxSeparationRad = maxSep,
            starIndices = emptyList(),
            starIds = emptyList()
        )
    }

    /**
     * Hash lookup: given observed descriptor, quantize and retrieve candidate catalog quads
     */
    fun lookupCandidates(observedDescriptor: QuadDescriptor): List<CatalogQuad> {
        val key = observedDescriptor.quantizedKey(binWidth)
        return hashTable[key] ?: emptyList()
    }

    /**
     * Lookup with tolerance: check neighboring bins as well to handle quantization edge cases
     * For each ratio, check bin-1, bin, bin+1
     * This increases tolerance to binWidth, important for noisy centroiding
     */
    fun lookupCandidatesWithNeighborBins(observedDescriptor: QuadDescriptor): List<CatalogQuad> {
        val baseBins = observedDescriptor.ratios.map { r -> floor(r / binWidth).toInt() }

        // Generate all combinations of neighboring bins (3^5 = 243 combinations for 5 ratios)
        // For efficiency, we can limit to checking base key plus immediate neighbors
        // For this phase, implement full neighbor search for small fixture

        val candidates = mutableSetOf<CatalogQuad>()
        val resultKeys = mutableSetOf<String>()

        // Recursive generation of neighbor keys
        fun generateKeys(index: Int, currentBins: MutableList<Int>) {
            if (index == baseBins.size) {
                val key = currentBins.joinToString("-")
                resultKeys.add(key)
                return
            }
            for (delta in -1..1) {
                currentBins.add(baseBins[index] + delta)
                generateKeys(index + 1, currentBins)
                currentBins.removeAt(currentBins.size - 1)
            }
        }

        generateKeys(0, mutableListOf())

        for (key in resultKeys) {
            hashTable[key]?.let { candidates.addAll(it) }
        }

        return candidates.toList()
    }
}
