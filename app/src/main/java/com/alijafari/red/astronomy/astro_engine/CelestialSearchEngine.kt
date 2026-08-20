package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.data.catalog.CanonicalAstroCatalog
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType

data class SearchResult(
    val celestialObject: CelestialObject,
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val isVisibleNow: Boolean,
    val statusFa: String,
    val statusEn: String,
    val matchScore: Int
)

object CelestialSearchEngine {

    /**
     * Searches all canonical celestial objects for a query matching Persian name, English name,
     * category, constellation, scientific identifiers, or search aliases.
     */
    fun search(
        query: String,
        userLat: Double,
        userLon: Double,
        jd: Double = TimeEngine.getJulianDate()
    ): List<SearchResult> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return emptyList()

        val timestampMs = TimeEngine.getTimestampFromJulianDate(jd)
        val canonicalObjects = CanonicalAstroCatalog.getAllCanonicalObjects()

        val results = mutableListOf<SearchResult>()

        for (canonObj in canonicalObjects) {
            val nameFa = canonObj.nameFa.lowercase()
            val nameEn = canonObj.nameEn.lowercase()
            val categoryEn = canonObj.observationalInfo.categoryEn.lowercase()
            val categoryFa = canonObj.observationalInfo.categoryFa.lowercase()
            val constCode = canonObj.scientificIdentifiers.constellationCode.lowercase()
            val bayer = canonObj.scientificIdentifiers.bayerDesignation?.lowercase() ?: ""
            val noradStr = canonObj.scientificIdentifiers.noradId?.toString() ?: ""
            val hipStr = canonObj.scientificIdentifiers.hipId?.toString() ?: ""
            val hdStr = canonObj.scientificIdentifiers.hdId?.toString() ?: ""

            val cleanNameEn = nameEn.replace("the ", "").trim()

            var score = 0
            when {
                canonObj.canonicalId.lowercase() == cleanQuery ||
                        canonObj.legacyIds.any { it.lowercase() == cleanQuery } ||
                        nameFa.startsWith(cleanQuery) || nameEn.startsWith(cleanQuery) ||
                        cleanNameEn.startsWith(cleanQuery) -> score = 100
                canonObj.searchAliasesFa.any { it.lowercase() == cleanQuery } ||
                        canonObj.searchAliasesEn.any { it.lowercase() == cleanQuery } -> score = 98
                canonObj.searchAliasesFa.any { it.lowercase().startsWith(cleanQuery) } ||
                        canonObj.searchAliasesEn.any { it.lowercase().startsWith(cleanQuery) } -> score = 95
                noradStr == cleanQuery || hipStr == cleanQuery || hdStr == cleanQuery -> score = 90
                nameFa.contains(cleanQuery) || nameEn.contains(cleanQuery) ||
                        bayer.contains(cleanQuery) -> score = 80
                canonObj.searchAliasesFa.any { it.lowercase().contains(cleanQuery) } ||
                        canonObj.searchAliasesEn.any { it.lowercase().contains(cleanQuery) } -> score = 75
                constCode.contains(cleanQuery) -> score = 60
                categoryEn.contains(cleanQuery) || categoryFa.contains(cleanQuery) -> score = 40
            }

            if (score >= 90 && canonObj.type == ObjectType.CONSTELLATION) {
                score += 1
            }

            if (score > 0) {
                val state = AstroDispatchEngine.calculateState(
                    idOrAlias = canonObj.canonicalId,
                    timestampMs = timestampMs,
                    userLatDeg = userLat,
                    userLonDeg = userLon
                )

                val altDeg = state?.altitudeDeg ?: 0.0
                val azDeg = state?.azimuthDeg ?: 0.0
                val isVisible = state?.isAboveHorizon ?: (altDeg > 0.0)
                val statusFa = if (isVisible) "قابل مشاهده در آسمان" else "زیر افق (تاریک)"
                val statusEn = if (isVisible) "Visible in sky" else "Below horizon"

                val celestialObj = CanonicalAstroCatalog.toCelestialObject(
                    canonicalObj = canonObj,
                    dynamicRa = state?.raDeg ?: 0.0,
                    dynamicDec = state?.decDeg ?: 0.0,
                    dynamicMag = state?.magnitude ?: canonObj.physicalProperties.magnitude
                )

                results.add(
                    SearchResult(
                        celestialObject = celestialObj,
                        altitudeDeg = altDeg,
                        azimuthDeg = azDeg,
                        isVisibleNow = isVisible,
                        statusFa = statusFa,
                        statusEn = statusEn,
                        matchScore = score
                    )
                )
            }
        }

        return results.sortedWith(
            compareByDescending<SearchResult> { it.matchScore }
                .thenByDescending { it.isVisibleNow }
                .thenBy { it.celestialObject.nameFa }
        )
    }

    /**
     * Gets default suggestion chips for quick access when search bar is focused.
     */
    fun getQuickSuggestions(): List<String> {
        return listOf("ماه", "خورشید", "مریخ", "زهره", "مشتری", "زحل", "شباهنگ", "آندرومدا", "جبار", "ISS")
    }

    data class Phase4VerificationReport(
        val sunFound: Boolean,
        val moonFound: Boolean,
        val jupiterFound: Boolean,
        val elaraFound: Boolean,
        val issFound: Boolean,
        val starFound: Boolean,
        val constellationFound: Boolean,
        val galaxyFound: Boolean,
        val satelliteFound: Boolean,
        val allResolvedToCanonical: Boolean,
        val isPassed: Boolean
    )

    fun verifyPhase4Search(): Phase4VerificationReport {
        val lat = 30.1141
        val lon = 51.5217

        val sunRes = search("خورشید", lat, lon).firstOrNull()
        val moonRes = search("moon", lat, lon).firstOrNull()
        val jupRes = search("مشتری", lat, lon).firstOrNull()
        val elaraRes = search("elara", lat, lon).firstOrNull()
        val issRes = search("ISS", lat, lon).firstOrNull()
        val starRes = search("شباهنگ", lat, lon).firstOrNull()
        val constRes = search("جبار", lat, lon).firstOrNull()
        val galaxyRes = search("آندرومدا", lat, lon).firstOrNull()
        val satRes = search("Hubble", lat, lon).firstOrNull()

        val sunOk = sunRes?.celestialObject?.id == "sun"
        val moonOk = moonRes?.celestialObject?.id == "moon"
        val jupOk = jupRes?.celestialObject?.id == "planet_jupiter"
        val elaraOk = elaraRes?.celestialObject?.id == "jup_elara"
        val issOk = issRes?.celestialObject?.id == "sat_25544"
        val starOk = starRes?.celestialObject?.id == "star_cma_sirius"
        val constOk = constRes?.celestialObject?.id == "const_ori"
        val galaxyOk = galaxyRes?.celestialObject?.id == "dso_m31_andromeda"
        val satOk = satRes?.celestialObject?.id != null && satRes.celestialObject.id.startsWith("sat_")

        val allCanonical = sunOk && moonOk && jupOk && elaraOk && issOk && starOk && constOk && galaxyOk && satOk

        return Phase4VerificationReport(
            sunFound = sunOk,
            moonFound = moonOk,
            jupiterFound = jupOk,
            elaraFound = elaraOk,
            issFound = issOk,
            starFound = starOk,
            constellationFound = constOk,
            galaxyFound = galaxyOk,
            satelliteFound = satOk,
            allResolvedToCanonical = allCanonical,
            isPassed = allCanonical
        )
    }
}
