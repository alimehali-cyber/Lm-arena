package com.alijafari.red.astronomy.domain

/**
 * Single Authoritative Canonical Identity Model for Astronomical Objects in RED Astronomy.
 *
 * Separates permanent object identity, localized names, search aliases, scientific identifiers,
 * parent/child relationships, and static physical/observational metadata from dynamic position
 * and ephemeris calculation logic.
 */
data class CanonicalAstroObject(
    val canonicalId: String,                       // Stable permanent canonical ID (e.g. "sun", "sat_25544", "sagittarius_a_star")
    val legacyIds: List<String> = emptyList(),       // Historical/legacy IDs mapped for backward compatibility (e.g. ["sun_main", "sun_sol"])
    val type: ObjectType,                            // Core ObjectType enum
    val nameEn: String,
    val nameFa: String,
    val searchAliasesEn: List<String> = emptyList(),
    val searchAliasesFa: List<String> = emptyList(),
    val parentId: String? = null,                    // Parent relationship ID (e.g. "planet_jupiter" for "jup_io")
    val childIds: List<String> = emptyList(),        // Child relationship IDs
    val scientificIdentifiers: ScientificIdentifiers = ScientificIdentifiers(),
    val staticPosition: StaticPosition? = null,     // Equatorial coordinates for static stars, DSOs, radiants, etc.
    val physicalProperties: PhysicalProperties = PhysicalProperties(),
    val observationalInfo: ObservationalInfo = ObservationalInfo()
)

data class ScientificIdentifiers(
    val noradId: Int? = null,            // NORAD ID for artificial satellites (e.g. 25544 for ISS)
    val messierId: String? = null,        // Messier designation (e.g. "M31", "M42")
    val ngcId: String? = null,            // NGC catalog designation (e.g. "NGC 224")
    val hipId: Int? = null,               // Hipparcos catalog ID
    val hdId: Int? = null,                // Henry Draper catalog ID
    val bayerDesignation: String = "",    // Bayer designation (e.g. "Alpha Canis Majoris")
    val flamsteedNumber: String = "",     // Flamsteed designation
    val constellationCode: String = "",   // IAU 3-letter constellation code (e.g. "ORI", "UMA")
    val spectralType: String = ""         // Stellar spectral classification (e.g. "A1V", "M2Iab")
)

data class StaticPosition(
    val raDeg: Double = 0.0,
    val decDeg: Double = 0.0,
    val distanceLightYears: Double = 0.0
)

data class PhysicalProperties(
    val magnitude: Double = 0.0,
    val diameterKm: Double? = null,
    val massKg: Double? = null,
    val surfaceGravityMS2: Double? = null,
    val temperatureK: Int = 0,
    val rotationPeriodHours: Double? = null,
    val orbitalPeriodDays: Double? = null,
    val semiMajorAxisAu: Double? = null,
    val diameterDisplayEn: String = "",
    val diameterDisplayFa: String = "",
    val massDisplayEn: String = "",
    val massDisplayFa: String = "",
    val gravityDisplayEn: String = "",
    val gravityDisplayFa: String = "",
    val distanceDisplayEn: String = "",
    val distanceDisplayFa: String = "",
    val relativisticGravitationalRatio: Double? = null,
    val relativisticKinematicRatio: Double? = null
)

data class ObservationalInfo(
    val categoryEn: String = "",
    val categoryFa: String = "",
    val descriptionEn: String = "",
    val descriptionFa: String = "",
    val observationTipEn: String = "",
    val observationTipFa: String = "",
    val historicalInfoEn: String = "",
    val historicalInfoFa: String = "",
    val activePeakDateWindowEn: String = "",
    val activePeakDateWindowFa: String = "",
    val zhr: Int = 0
)
