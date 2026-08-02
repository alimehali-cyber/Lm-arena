package com.example.domain

enum class ObjectType(val nameEn: String, val nameFa: String) {
    STAR("Star", "ستاره"),
    PLANET("Planet", "سیاره"),
    DEEP_SKY("Deep Sky Object", "جرم اعماق فضا (سحابی/کهکشان)"),
    SATELLITE("Satellite", "ماهواره مصنوعی"),
    MOON("Moon", "ماه"),
    SUN("Sun", "خورشید")
}

data class CelestialObject(
    val id: String,
    val type: ObjectType,
    val nameEn: String,
    val nameFa: String,
    val raDeg: Double,
    val decDeg: Double,
    val magnitude: Double,
    val constellationEn: String,
    val constellationFa: String,
    val distanceLightYears: Double,
    val category: String, // e.g. "Supergiant", "Gas Giant", "Spiral Galaxy", "Emission Nebula", "Open Cluster"
    val descriptionEn: String,
    val descriptionFa: String,
    val observationTipEn: String,
    val observationTipFa: String,
    val spectralType: String = ""
)

data class ConstellationData(
    val code: String,
    val nameEn: String,
    val nameFa: String,
    val latinName: String,
    val mainStars: List<Pair<Double, Double>> // List of (RA, Dec) lines/points
)

data class UserLocation(
    val cityNameEn: String = "Nurabad City (NC)",
    val cityNameFa: String = "نورآباد ممسنی (NC)",
    val latitude: Double = 30.1141,
    val longitude: Double = 51.5217,
    val elevationMeters: Double = 940.0,
    val bortleClass: Int = 3
)

enum class AppLanguage {
    PERSIAN,
    ENGLISH
}

enum class CalendarSystem {
    SOLAR_HIJRI,
    GREGORIAN
}

enum class ThemeMode {
    DARK_NAVY,
    OLED_BLACK,
    LIGHT
}

