package com.zig.gravity.sim

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants

/**
 * Named catalog entries.
 *
 * §3.3 locks [BodyType] to seven members; the named solar-system objects are catalog *entries*
 * that instantiate one of those types with that object's real mass and a display size inside the
 * §3.6a dp band for its type. The enum is not extended.
 *
 * Masses are real (§3.1 honesty requirement): distances and radii in presets are not to scale,
 * masses / G / velocities are.
 */
data class CatalogEntry(
    val key: String,
    val nameFa: String,
    val nameEn: String,
    val type: BodyType,
    val massKg: Double,
    val dp: Double,
    /** Desaturated premium tone, 0xAARRGGBB (§3.9 palette). */
    val colorArgb: Long,
    /** Real physical radius in metres, shown as an info line in the inspector (§3.11). */
    val realRadiusM: Double,
    val isPair: Boolean = false
)

object BodyCatalog {

    private const val ME = EngineConstants.M_EARTH
    private const val MS = EngineConstants.M_SUN

    val SUN = CatalogEntry("sun", "خورشید", "Sun", BodyType.SUN, MS, 26.0, 0xFFE8C87A, EngineConstants.R_SUN)
    val MERCURY = CatalogEntry("mercury", "عطارد", "Mercury", BodyType.PLANET, 0.0553 * ME, 8.0, 0xFF9A928A, 2.4397e6)
    val VENUS = CatalogEntry("venus", "زهره", "Venus", BodyType.PLANET, 0.815 * ME, 10.0, 0xFFD6BE92, 6.0518e6)
    val EARTH = CatalogEntry("earth", "زمین", "Earth", BodyType.PLANET, ME, 10.0, 0xFF6E88A6, EngineConstants.R_EARTH)
    val MOON = CatalogEntry("moon", "ماه", "Moon", BodyType.MOON, EngineConstants.M_MOON, 6.0, 0xFFCFC7B8, EngineConstants.R_MOON)
    val MARS = CatalogEntry("mars", "مریخ", "Mars", BodyType.PLANET, 0.107 * ME, 9.0, 0xFFA6705C, 3.3895e6)
    val JUPITER = CatalogEntry("jupiter", "مشتری", "Jupiter", BodyType.PLANET, 317.8 * ME, 16.0, 0xFFC2A57B, 6.9911e7)
    val SATURN = CatalogEntry("saturn", "زحل", "Saturn", BodyType.PLANET, 95.16 * ME, 15.0, 0xFFD2C08F, 5.8232e7)
    val URANUS = CatalogEntry("uranus", "اورانوس", "Uranus", BodyType.PLANET, 14.54 * ME, 12.0, 0xFF8FB3B0, 2.5362e7)
    val NEPTUNE = CatalogEntry("neptune", "نپتون", "Neptune", BodyType.PLANET, 17.15 * ME, 12.0, 0xFF7183A6, 2.4622e7)
    val ASTEROID = CatalogEntry("asteroid", "سیارک", "Asteroid", BodyType.ASTEROID, 1.0e18, 4.0, 0xFF8C8378, 5.0e5)
    val MARBLE = CatalogEntry("marble", "جسم آزمایشی", "Test marble", BodyType.TEST_MARBLE, 0.0, 5.0, 0xFFE4E0D8, 0.0)
    val BLACK_HOLE = CatalogEntry("black_hole", "سیاه‌چاله", "Black hole", BodyType.BLACK_HOLE, 5.0 * MS, 14.0, 0xFF0A0A0C, EngineConstants.schwarzschildRadius(5.0 * MS))
    val WORMHOLE = CatalogEntry("wormhole", "کرم‌چاله (فرضی)", "Wormhole (hypothetical)", BodyType.WORMHOLE_MOUTH, 0.0, 12.0, 0xFFD4A853, 0.0, isPair = true)

    /** Everything the Add sheet offers, in presentation order. */
    val all: List<CatalogEntry> = listOf(
        SUN, MERCURY, VENUS, EARTH, MOON, MARS, JUPITER, SATURN, URANUS, NEPTUNE,
        ASTEROID, MARBLE, BLACK_HOLE, WORMHOLE
    )

    private val byKey: Map<String, CatalogEntry> = all.associateBy { it.key }

    fun byKey(key: String?): CatalogEntry? = if (key == null) null else byKey[key]

    /** Colour for a body, falling back to a per-type tone for bodies created without a catalog key. */
    fun colorOf(key: String?, type: BodyType): Long = byKey(key)?.colorArgb ?: when (type) {
        BodyType.SUN -> SUN.colorArgb
        BodyType.PLANET -> EARTH.colorArgb
        BodyType.MOON -> MOON.colorArgb
        BodyType.ASTEROID -> ASTEROID.colorArgb
        BodyType.TEST_MARBLE -> MARBLE.colorArgb
        BodyType.BLACK_HOLE -> BLACK_HOLE.colorArgb
        BodyType.WORMHOLE_MOUTH -> WORMHOLE.colorArgb
    }

    fun nameOf(key: String?, type: BodyType, isFa: Boolean): String {
        val e = byKey(key)
        if (e != null) return if (isFa) e.nameFa else e.nameEn
        return if (isFa) typeNameFa(type) else typeNameEn(type)
    }

    fun typeNameFa(type: BodyType): String = when (type) {
        BodyType.SUN -> "ستاره"
        BodyType.PLANET -> "سیاره"
        BodyType.MOON -> "قمر"
        BodyType.ASTEROID -> "سیارک"
        BodyType.TEST_MARBLE -> "جسم آزمایشی"
        BodyType.BLACK_HOLE -> "سیاه‌چاله"
        BodyType.WORMHOLE_MOUTH -> "دهانه کرم‌چاله (فرضی)"
    }

    fun typeNameEn(type: BodyType): String = when (type) {
        BodyType.SUN -> "Star"
        BodyType.PLANET -> "Planet"
        BodyType.MOON -> "Moon"
        BodyType.ASTEROID -> "Asteroid"
        BodyType.TEST_MARBLE -> "Test marble"
        BodyType.BLACK_HOLE -> "Black hole"
        BodyType.WORMHOLE_MOUTH -> "Wormhole mouth"
    }

    /**
     * Real physical radius for the inspector info line. Catalog bodies report their true radius;
     * anything else is estimated from mass at Earth density so the line is never blank.
     */
    fun realRadiusOf(key: String?, type: BodyType, massKg: Double): Double {
        val e = byKey(key)
        if (e != null && e.realRadiusM > 0.0) {
            // Scale with mass^(1/3) so an edited mass still reports a sensible physical radius.
            val ratio = if (e.massKg > 0.0) massKg / e.massKg else 1.0
            return e.realRadiusM * Math.cbrt(if (ratio > 0.0) ratio else 1.0)
        }
        if (type == BodyType.BLACK_HOLE) return EngineConstants.schwarzschildRadius(massKg)
        if (massKg <= 0.0) return 0.0
        val density = 5514.0 // kg/m^3, Earth
        return Math.cbrt(3.0 * massKg / (4.0 * Math.PI * density))
    }
}
