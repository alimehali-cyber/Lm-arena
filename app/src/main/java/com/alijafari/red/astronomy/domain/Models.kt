package com.alijafari.red.astronomy.domain

enum class ObjectType(val nameEn: String, val nameFa: String) {
    STAR("Star", "ستاره"),
    PLANET("Planet", "سیاره"),
    DWARF_PLANET("Dwarf Planet", "سیاره کوتوله"),
    BLACK_HOLE("Black Hole", "سیاه‌چاله"),
    DEEP_SKY("Deep Sky Object", "جرم اعماق فضا"),
    SATELLITE("Artificial Satellite", "ماهواره مصنوعی"),
    MOON("Moon", "ماه"),
    SUN("Sun", "خورشید"),
    ASTERISM("Asterism", "صورتواره / چیدمان ستاره‌ای"),
    GALAXY("Galaxy", "کهکشان"),
    STAR_CLUSTER("Star Cluster", "خوشه ستاره‌ای"),
    NEBULA("Nebula", "سحابی"),
    GLOBULAR_CLUSTER("Globular Cluster", "خوشه کروی"),
    METEOR_SHOWER("Meteor Shower Radiant", "کانون بارش شهابی"),
    CONSTELLATION("Constellation", "صورت فلکی"),
    REFERENCE_POINT("Celestial Reference Point", "نقطه مرجع آسمانی")
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
    val spectralType: String = "",
    val hipId: Int? = null,
    val hdId: Int? = null,
    val bayerDesignation: String = "",
    val flamsteedNumber: String = "",
    val temperatureK: Int = 0,
    val activePeakDateWindowEn: String = "",
    val activePeakDateWindowFa: String = "",
    val zhr: Int = 0,
    val catalogDesignations: List<String> = emptyList(),
    val angularSizeArcmin: Double? = null,
    val constellationCode: String = "",
    val bestViewingMonthEn: String = "",
    val bestViewingMonthFa: String = "",
    val funFactsEn: List<String> = emptyList(),
    val funFactsFa: List<String> = emptyList()
)

data class ConstellationData(
    val code: String,
    val nameEn: String,
    val nameFa: String,
    val latinName: String,
    val mainStars: List<Pair<Double, Double>>, // List of (RA, Dec) lines/points
    val starIdsLines: List<Pair<String, String>> = emptyList(),
    val areaSqDeg: Double = 0.0,
    val seasonEn: String = "",
    val seasonFa: String = "",
    val hemisphereEn: String = "",
    val hemisphereFa: String = "",
    val bestViewingMonthEn: String = "",
    val bestViewingMonthFa: String = "",
    val historicalInfoEn: String = "",
    val historicalInfoFa: String = ""
)

data class UserLocation(
    val cityNameEn: String = "Nurabad City (NC)",
    val cityNameFa: String = "نورآباد ممسنی (NC)",
    val latitude: Double = 30.1141,
    val longitude: Double = 51.5217,
    val elevationMeters: Double = 940.0,
    val bortleClass: Int = 3,
    val timezoneId: String = "Asia/Tehran",
    val countryCode: String = "IR",
    val provinceEn: String = "Fars",
    val provinceFa: String = "فارس"
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
    LIGHT,
    DARK
}

/**
 * Which interface the app draws itself with.
 *
 * [CLASSIC] is the app as it has always been: one flat, opaque surface per panel, ink measured against
 * that surface, nothing behind it. [SPATIAL] is the optional second interface — frosted translucent
 * panels floating over an animated ambient mesh, specular edges, dot-matrix readouts — described in
 * `docs/SPATIAL_GLASS_UI.md`. It is a *drawing* choice, not a colour choice: the same token set, the
 * same call sites and the same layout are kept, so switching interfaces never changes what the app
 * says, only how it is lit.
 */
enum class UiStyle(val nameEn: String, val nameFa: String) {
    CLASSIC("Default", "پیش‌فرض"),
    SPATIAL("Spatial Glass", "شیشه فضایی");

    companion object {
        /** The style the app ships with; anything unreadable in preferences falls back to it. */
        val DEFAULT = CLASSIC
    }
}

/**
 * The colour of the [UiStyle.SPATIAL] interface.
 *
 * Each palette is a complete ambient world — the base the glass floats over, the mesh blobs it
 * refracts, the specular edge, the accent — so a palette can never only half-apply. [AURORA] is the
 * coral / peach / cyan / violet mesh the style is known for; [WINE] is the same construction in wine
 * red and burgundy, gold-edged.
 */
enum class SpatialPalette(val nameEn: String, val nameFa: String) {
    AURORA("Aurora", "شفق"),
    WINE("Wine · Mei", "شرابی · می");

    companion object {
        val DEFAULT = AURORA
    }
}

enum class SkyCanvasTheme(val nameEn: String, val nameFa: String) {
    REAL_SKY("Real Sky", "آسمان واقعی"),
    ATMOSPHERIC_SKY("Atmospheric Sky", "آسمان جوی (اتمسفریک)"),
    MONOCHROME_SCIENTIFIC("Monochrome Scientific", "تک‌رنگ علمی (سوئیسی)"),
    KIDS_WATERCOLOR("Kids WaterColor", "آبرنگ جادویی کودکانه"),
    OBSERVATORY("Observatory Night", "شب رصدخانه (سرخ)"),
    PAPERCRAFT_DIORAMA("Papercraft Diorama", "دیوراما هنر کاغذ بریده");

    companion object {
        val CELESTIAL get() = ATMOSPHERIC_SKY
        val MONOCHROME get() = MONOCHROME_SCIENTIFIC
        val FUN get() = KIDS_WATERCOLOR
        val PAPERCRAFT get() = PAPERCRAFT_DIORAMA
    }
}

