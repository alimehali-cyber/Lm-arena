package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.CelestialObject

/**
 * AR-sky scoping for the canonical catalogue.
 *
 * Earth is the observer's own position, not an object in the sky. It has no meaningful
 * altitude/azimuth for the AR projection ([AstroDispatchEngine] pins it to the zenith by
 * definition), and rendering it would paint a permanent "planet" disc over the sky, let it be
 * found by the AR search field, and let it take part in label clustering and off-screen edge
 * indicators. So it is excluded from every AR-facing consumer.
 *
 * The exclusion is deliberately scoped to AR only: Earth stays in [CanonicalAstroCatalog]
 * because the Lab / time-dilation screen, the relativistic calculator, the object detail pages
 * and the parent/child catalogue relations all use it as the reference body. Use
 * [AstronomyCatalog.getAllObjects] (or [CanonicalAstroCatalog.getAllCanonicalObjects]) for those
 * consumers, and [arSkyObjects] wherever the AR screen needs its list.
 */
object ARSkyCatalog {

    /** Canonical id of the object that never enters the AR-facing lists. */
    const val EARTH_CANONICAL_ID = "planet_earth"

    /** Canonical, legacy and alias spellings that all resolve to Earth. */
    private val earthIds = setOf(EARTH_CANONICAL_ID, "earth", "terra")

    /**
     * True when [idOrAlias] is Earth, either literally or through the canonical id resolver, so
     * legacy ids ("earth") and search aliases ("Terra") cannot smuggle it back into the AR sky.
     */
    fun isEarth(idOrAlias: String?): Boolean {
        if (idOrAlias.isNullOrBlank()) return false
        val clean = idOrAlias.trim().lowercase()
        if (earthIds.contains(clean)) return true
        return earthIds.contains(CanonicalAstroCatalog.resolveCanonicalId(clean))
    }

    /** True when [obj] must not reach any AR-facing consumer. */
    fun isExcludedFromArSky(obj: CelestialObject): Boolean = isEarth(obj.id)

    /**
     * The one place the AR screen reads its object list from: the canvas render loop, the tap
     * hit-test, the visibility filters, the off-screen edge indicators and label clustering all
     * consume this filtered list. The AR search field applies the same rule inside
     * [CelestialSearchEngine.search].
     */
    fun arSkyObjects(objects: List<CelestialObject>): List<CelestialObject> =
        objects.filterNot { isExcludedFromArSky(it) }
}
