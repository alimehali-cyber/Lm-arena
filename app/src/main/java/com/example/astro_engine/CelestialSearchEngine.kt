package com.example.astro_engine

import com.example.data.catalog.AstronomyCatalog
import com.example.domain.CelestialObject

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
     * Searches all catalog celestial objects for a query matching Persian name, English name,
     * category, constellation, or common aliases.
     */
    fun search(
        query: String,
        userLat: Double,
        userLon: Double,
        jd: Double = TimeEngine.getJulianDate()
    ): List<SearchResult> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return emptyList()

        val lastDeg = TimeEngine.getLAST(jd, userLon)
        val allObjects = AstronomyCatalog.getAllObjects(jd)

        val results = mutableListOf<SearchResult>()

        for (obj in allObjects) {
            val nameFa = obj.nameFa.lowercase()
            val nameEn = obj.nameEn.lowercase()
            val constFa = obj.constellationFa.lowercase()
            val constEn = obj.constellationEn.lowercase()
            val category = obj.category.lowercase()

            var score = 0
            when {
                nameFa.startsWith(cleanQuery) || nameEn.startsWith(cleanQuery) -> score = 100
                nameFa.contains(cleanQuery) || nameEn.contains(cleanQuery) -> score = 80
                constFa.contains(cleanQuery) || constEn.contains(cleanQuery) -> score = 60
                category.contains(cleanQuery) -> score = 40
            }

            if (score > 0) {
                val horiz = CoordinateEngine.equatorialToHorizontal(
                    CoordinateEngine.Equatorial(obj.raDeg, obj.decDeg),
                    lastDeg,
                    userLat
                )
                val isVisible = horiz.altitudeDeg > 0.0
                val statusFa = if (isVisible) "قابل مشاهده در آسمان" else "زیر افق (تاریک)"
                val statusEn = if (isVisible) "Visible in sky" else "Below horizon"

                results.add(
                    SearchResult(
                        celestialObject = obj,
                        altitudeDeg = horiz.altitudeDeg,
                        azimuthDeg = horiz.azimuthDeg,
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
}
