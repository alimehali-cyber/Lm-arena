package com.alijafari.red.astronomy.startracker.solver

import com.alijafari.red.astronomy.startracker.catalog.CatalogBuildConfig

/**
 * Quad candidate formation from list of StarObservations.
 * Since full combinatorial explosion is expensive, select from top-N brightest.
 *
 * For N observations, number of quads = C(N,4)
 * N=8 → 70 quads, N=10 → 210 quads, N=15 → 1365 quads
 * Tradeoff: larger N more chances to include true quad, but more computation.
 */
class QuadCandidateBuilder(
    val topN: Int = 10 // configurable, default 8-10 per CatalogBuildConfig
) {

    data class ObservationQuad(
        val observations: List<StarObservation>, // 4 observations
        val indices: List<Int> // indices in original list
    )

    fun buildCandidates(observations: List<StarObservation>): List<ObservationQuad> {
        if (observations.size < 4) return emptyList()

        // Select top-N brightest by flux
        val sortedByFlux = observations.withIndex().sortedByDescending { it.value.flux }
        val selected = if (observations.size > topN) {
            sortedByFlux.take(topN)
        } else {
            sortedByFlux
        }

        val selectedIndices = selected.map { it.index }
        val selectedObs = selected.map { it.value }

        // Enumerate C(N,4)
        val quads = mutableListOf<ObservationQuad>()
        for (i in selectedObs.indices) {
            for (j in i + 1 until selectedObs.size) {
                for (k in j + 1 until selectedObs.size) {
                    for (l in k + 1 until selectedObs.size) {
                        quads.add(
                            ObservationQuad(
                                observations = listOf(selectedObs[i], selectedObs[j], selectedObs[k], selectedObs[l]),
                                indices = listOf(selectedIndices[i], selectedIndices[j], selectedIndices[k], selectedIndices[l])
                            )
                        )
                    }
                }
            }
        }

        return quads
    }

    /**
     * D (final pass): LOCAL candidate formation — mirrors the capped index construction
     * (QuadPatternIndex.capped) so that observed quads and indexed quads are drawn from
     * the same structural family: anchor + 3 of its nearest neighbors within the bright
     * pool, all pairwise separations in [minSep, maxSep].
     *
     * Why: the global top-N-brightest C(N,4) candidates span the whole FOV (10-40 deg
     * mutual separations) while the capped index stores nearest-neighbor groups — the
     * two families are nearly DISJOINT, which measured as 0/20 lost-in-space solves on
     * the real catalog (D1 finding, docs/startracker/D_SYNTHETIC_E2E.md). Local mode
     * restores the overlap.
     *
     * Deterministic: anchors ordered by flux (brightest first), neighbors by angular
     * separation (nearest first), then by original index.
     */
    fun buildLocalCandidates(
        observations: List<StarObservation>,
        poolSize: Int = CatalogBuildConfig.CANDIDATE_POOL_SIZE,
        neighborsPerAnchor: Int = CatalogBuildConfig.QUAD_NEIGHBORS_PER_STAR,
        maxSeparationRad: Double = CatalogBuildConfig.MAX_PAIR_SEPARATION_RAD,
        minSeparationRad: Double = CatalogBuildConfig.MIN_PAIR_SEPARATION_RAD
    ): List<ObservationQuad> {
        if (observations.size < 4) return emptyList()

        // bright pool: flux is inversely related to magnitude; deterministic order
        val pool = observations.withIndex()
            .map { it.index to it.value }
            .sortedWith(compareByDescending<Pair<Int, StarObservation>> { it.second.flux }.thenBy { it.first })
            .take(poolSize)
        if (pool.size < 4) return emptyList()

        fun uv(o: StarObservation) = o.unitVectorCamera
        fun sep(a: StarObservation, b: StarObservation): Double {
            val u = uv(a); val v = uv(b)
            val d = (u.first * v.first + u.second * v.second + u.third * v.third).coerceIn(-1.0, 1.0)
            return kotlin.math.acos(d)
        }

        val quads = mutableListOf<ObservationQuad>()
        val seen = HashSet<Set<Int>>()
        for (anchor in pool) {
            val neighbors = pool
                .filter { it.first != anchor.first }
                .map { it to sep(anchor.second, it.second) }
                .filter { it.second in minSeparationRad..maxSeparationRad }
                .sortedWith(compareBy<Pair<Pair<Int, StarObservation>, Double>> { it.second }.thenBy { it.first.first })
                .take(neighborsPerAnchor)
            if (neighbors.size < 3) continue
            for (x in neighbors.indices) for (y in x + 1 until neighbors.size) for (z in y + 1 until neighbors.size) {
                val members = listOf(anchor, neighbors[x].first, neighbors[y].first, neighbors[z].first)
                val idxSet = members.map { it.first }.toSet()
                if (!seen.add(idxSet)) continue
                // verify ALL six separations within bounds (mirror of index combo check)
                var ok = true
                outer@ for (a in 0..3) for (b in a + 1..3) {
                    val s = sep(members[a].second, members[b].second)
                    if (s < minSeparationRad || s > maxSeparationRad) { ok = false; break@outer }
                }
                if (!ok) continue
                quads.add(
                    ObservationQuad(
                        observations = members.map { it.second },
                        indices = members.map { it.first }
                    )
                )
            }
        }
        return quads
    }

    /**
     * Report how many quads produced for given N and scaling.
     * C(N,4) = N! / (4! * (N-4)!) = N*(N-1)*(N-2)*(N-3)/24
     */
    fun countQuadsForN(n: Int): Int {
        if (n < 4) return 0
        return n * (n - 1) * (n - 2) * (n - 3) / 24
    }
}
