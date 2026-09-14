package com.zig.museum.core.model

/**
 * ObjectSpec — single source of truth for all thirteen museum objects.
 * Values taken from IAU Working Group on Cartographic Coordinates and Rotational Elements
 * report (Archinal et al. 2018) and NASA Planetary Fact Sheets.
 * Each numeric literal is commented with its source.
 */

data class ObjectSpec(
    val id: String,
    val displayNameEn: String,
    val displayNameFa: String,
    val sceneRadiusMetres: Double, // mean radius, metres
    val oblateness: Double, // (a - b) / a, dimensionless
    val axialTiltDeg: Double, // degrees, IAU WGCCRE
    val rotationPeriodHours: Double, // signed for retrograde (Venus, Uranus), sidereal
    val packId: String,
    val dataCeilingTextEn: String,
    val dataCeilingTextFa: String,
    val qualityTiers: List<QualityTier> = QualityTier.entries
)

enum class QualityTier {
    TIER_0, // 16K albedo where available, displacement on, full atmosphere
    TIER_1, // 8K albedo, displacement on, half-res atmosphere
    TIER_2, // 4K albedo, normal-mapped, single-scattering
    TIER_3  // 2K albedo, static lit sphere
}
