package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import kotlin.math.sqrt

object CelestialObjectSizes {
    const val STAR_MAGNITUDE_0_SIZE_DP = 14f
    const val STAR_MAGNITUDE_1_SIZE_DP = 11f
    const val STAR_MAGNITUDE_2_SIZE_DP = 9f
    const val STAR_MAGNITUDE_3_SIZE_DP = 7f
    const val STAR_MAGNITUDE_4_SIZE_DP = 5f
    const val STAR_MAGNITUDE_5_SIZE_DP = 4f
    const val LABEL_SHOW_MAGNITUDE_THRESHOLD = 4.0f

    const val PLANET_INNER_SIZE_DP = 18f
    const val PLANET_GLOW_SIZE_DP = 48f
    const val SUN_SIZE_DP = 40f
    const val SUN_GLOW_SIZE_DP = 80f
    const val MOON_SIZE_DP = 36f

    const val DSO_GALAXY_SIZE_DP = 20f
    const val DSO_NEBULA_SIZE_DP = 22f
    const val DSO_CLUSTER_SIZE_DP = 18f

    const val SATELLITE_SIZE_DP = 14f
    const val ISS_SIZE_DP = 20f

    const val CENTER_PROXIMITY_MAX_SCALE = 2.5f
    const val CENTER_PROXIMITY_THRESHOLD_DEG = 3f
    const val CENTER_PROXIMITY_FULL_DEG = 0.5f

    /**
     * Gets the base diameter in Dp for a given Celestial Object.
     */
    fun getBaseSizeDp(obj: CelestialObject): Float {
        return when (obj.type) {
            ObjectType.SUN -> SUN_SIZE_DP
            ObjectType.MOON -> MOON_SIZE_DP
            ObjectType.PLANET, ObjectType.DWARF_PLANET -> PLANET_INNER_SIZE_DP
            ObjectType.SATELLITE -> if (obj.id == "sat_iss") ISS_SIZE_DP else SATELLITE_SIZE_DP
            ObjectType.DEEP_SKY -> {
                when {
                    obj.category.contains("Galaxy", ignoreCase = true) -> DSO_GALAXY_SIZE_DP
                    obj.category.contains("Nebula", ignoreCase = true) -> DSO_NEBULA_SIZE_DP
                    else -> DSO_CLUSTER_SIZE_DP
                }
            }
            ObjectType.STAR -> {
                when {
                    obj.magnitude <= 0.0 -> STAR_MAGNITUDE_0_SIZE_DP
                    obj.magnitude <= 1.0 -> STAR_MAGNITUDE_1_SIZE_DP
                    obj.magnitude <= 2.0 -> STAR_MAGNITUDE_2_SIZE_DP
                    obj.magnitude <= 3.0 -> STAR_MAGNITUDE_3_SIZE_DP
                    obj.magnitude <= 4.0 -> STAR_MAGNITUDE_4_SIZE_DP
                    else -> STAR_MAGNITUDE_5_SIZE_DP
                }
            }
            ObjectType.GALAXY, ObjectType.BLACK_HOLE -> DSO_GALAXY_SIZE_DP
            ObjectType.NEBULA -> DSO_NEBULA_SIZE_DP
            ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER -> DSO_CLUSTER_SIZE_DP
            ObjectType.METEOR_SHOWER -> 16f
            ObjectType.ASTERISM -> 12f
            ObjectType.CONSTELLATION -> 24f
            ObjectType.REFERENCE_POINT -> 14f
        }
    }

    /**
     * Calculates angular distance from center of screen (in degrees) and returns
     * proximity scale multiplier between 1.0f and CENTER_PROXIMITY_MAX_SCALE (2.5f).
     */
    fun calculateProximityScale(
        deltaAzDeg: Float,
        deltaAltDeg: Float
    ): Float {
        val distDeg = sqrt(deltaAzDeg * deltaAzDeg + deltaAltDeg * deltaAltDeg)
        if (distDeg >= CENTER_PROXIMITY_THRESHOLD_DEG) return 1.0f
        if (distDeg <= CENTER_PROXIMITY_FULL_DEG) return CENTER_PROXIMITY_MAX_SCALE

        val fraction = 1.0f - ((distDeg - CENTER_PROXIMITY_FULL_DEG) / (CENTER_PROXIMITY_THRESHOLD_DEG - CENTER_PROXIMITY_FULL_DEG))
        return 1.0f + fraction * (CENTER_PROXIMITY_MAX_SCALE - 1.0f)
    }
}
