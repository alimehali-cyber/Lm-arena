package com.alijafari.red.astronomy.domain

/**
 * Unified dynamic astronomical calculation result for any CanonicalAstroObject.
 *
 * Bridges static metadata stored in CanonicalAstroCatalog/CanonicalAstroObject with real-time
 * dynamic scientific calculations from SunEngine, MoonEngine, PlanetEngine, JupiterMoonsEngine,
 * SatelliteEngine/ISSEngine, and GalacticEngine/CoordinateEngine.
 */
data class CalculatedAstroState(
    val canonicalObject: CanonicalAstroObject,
    val jd: Double,
    val timestampMs: Long,
    val userLatDeg: Double,
    val userLonDeg: Double,
    val elevationM: Double = 940.0,

    // Dynamic Equatorial Coordinates
    val raDeg: Double,
    val decDeg: Double,

    // Dynamic Topocentric Horizon Coordinates
    val altitudeDeg: Double,
    val azimuthDeg: Double,

    // Distance metrics
    val distanceKm: Double? = null,
    val distanceAU: Double? = null,
    val distanceLightYears: Double? = null,

    // Photometric & Physical properties
    val magnitude: Double = canonicalObject.physicalProperties.magnitude,
    val isAboveHorizon: Boolean = altitudeDeg > 0.0,
    val phaseNameEn: String? = null,
    val phaseNameFa: String? = null,
    val illuminationPercent: Double? = null,
    val angularDiameterArcsec: Double? = null,
    val isSunlit: Boolean? = null,

    // Satellite visibility indicators
    val satellitePassStatusEn: String? = null,
    val satellitePassStatusFa: String? = null,

    // Canonical Hierarchy
    val parentCanonicalId: String? = canonicalObject.parentId,
    val childCanonicalIds: List<String> = canonicalObject.childIds,

    // Engine-specific specialized payload (e.g., MoonData, SunPosition, PlanetPosition, MoonPosition, SatelliteLiveState)
    val specializedData: Any? = null
)
