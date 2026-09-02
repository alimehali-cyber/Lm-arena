package com.alijafari.red.astronomy.startracker.solver

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
     * Report how many quads produced for given N and scaling.
     * C(N,4) = N! / (4! * (N-4)!) = N*(N-1)*(N-2)*(N-3)/24
     */
    fun countQuadsForN(n: Int): Int {
        if (n < 4) return 0
        return n * (n - 1) * (n - 2) * (n - 3) / 24
    }
}
