package com.alijafari.red.astronomy.data.catalog

/**
 * Aggregates the hand-authored bilingual fact sets for the non-deep-sky objects of the canonical
 * union list (constellations, meteor showers, asterisms, and bright stars) so that
 * [PhysicalData.getCoolFactsEn] / [PhysicalData.getCoolFactsFa] can resolve them as a final
 * fallback before returning an empty list.
 */
internal object ExtendedFactCatalog {

    private val all: Map<String, BilingualFacts> =
        ConstellationFacts.map +
            MeteorShowerFacts.map +
            AsterismFacts.map +
            StarFacts.map

    fun factsFor(canonicalId: String): BilingualFacts? = all[canonicalId]
}
