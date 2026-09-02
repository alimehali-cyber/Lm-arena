package com.alijafari.red.astronomy.startracker.catalog

/**
 * Data class for a single catalog star for plate-solving.
 * Separate from ZIG's existing display catalogs (StarCatalog.kt has only 43 hand-written stars).
 * This is for the NEW catalog asset for plate-solving, built offline.
 *
 * @param id unique identifier (e.g., "BSC001", "HIP1234", or "TESTSTAR001" for synthetic fixtures)
 * @param raRad right ascension in radians, J2000 equatorial, range [0, 2π)
 * @param decRad declination in radians, J2000 equatorial, range [-π/2, +π/2]
 * @param magnitude apparent magnitude (lower = brighter), e.g., -1.46 for Sirius
 * @param sourceCatalog source catalog name (e.g., "BSC5", "Hipparcos", "TEST_FIXTURE")
 */
data class CatalogStar(
    val id: String,
    val raRad: Double,
    val decRad: Double,
    val magnitude: Double,
    val sourceCatalog: String = "UNKNOWN"
) {
    /** RA in degrees, derived */
    val raDeg: Double get() = raRad * CatalogBuildConfig.RAD_TO_DEG

    /** Dec in degrees, derived */
    val decDeg: Double get() = decRad * CatalogBuildConfig.RAD_TO_DEG

    /** Unit vector in J2000 equatorial frame (for angular separation calculations) */
    fun toUnitVector(): Triple<Double, Double, Double> {
        // RA = longitude, Dec = latitude
        // x = cos(Dec)*cos(RA), y = cos(Dec)*sin(RA), z = sin(Dec)
        val cosDec = kotlin.math.cos(decRad)
        val x = cosDec * kotlin.math.cos(raRad)
        val y = cosDec * kotlin.math.sin(raRad)
        val z = kotlin.math.sin(decRad)
        return Triple(x, y, z)
    }

    companion object {
        fun fromDegrees(
            id: String,
            raDeg: Double,
            decDeg: Double,
            magnitude: Double,
            sourceCatalog: String = "UNKNOWN"
        ): CatalogStar {
            return CatalogStar(
                id = id,
                raRad = raDeg * CatalogBuildConfig.DEG_TO_RAD,
                decRad = decDeg * CatalogBuildConfig.DEG_TO_RAD,
                magnitude = magnitude,
                sourceCatalog = sourceCatalog
            )
        }
    }
}
