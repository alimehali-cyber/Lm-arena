package com.alijafari.red.astronomy.startracker.diagnostics

import kotlin.math.*

/**
 * Phase 9 Task 2: RelativeBearing isolated formula.
 * General formula: relAz = wrap180(objAz - facingAz)
 * Facing: north hemisphere -> 180° (south), south hemisphere -> 0° (north)
 * Pure Kotlin, no Android dependency.
 */

object RelativeBearing {

    /**
     * Normalizes angle to [-180, +180]
     */
    fun wrap180(deg: Double): Double {
        return ((deg % 360.0 + 540.0) % 360.0) - 180.0
    }

    /**
     * Computes relative bearing: object azimuth relative to facing azimuth.
     * Positive = object to the right of facing, Negative = to the left.
     * @param objectAz object azimuth in degrees [0,360)
     * @param facingAz facing azimuth in degrees [0,360) — direction viewer is looking
     * @return relative azimuth in [-180,180]
     */
    fun relativeBearing(objectAz: Double, facingAz: Double): Double {
        return wrap180(objectAz - facingAz)
    }

    /**
     * Computes facing azimuth from latitude per HeroSkyProjection convention:
     * - Northern hemisphere (lat >=0): facing South = 180°
     * - Southern hemisphere (lat <0): facing North = 0°
     * - Equator (lat==0): facing South = 180° (stable default)
     */
    fun facingFromLatitude(latitudeDeg: Double): Double {
        return if (latitudeDeg >= 0.0) 180.0 else 0.0
    }

    /**
     * Convenience: relative bearing directly from object az and observer latitude.
     */
    fun relativeBearingFromLatitude(objectAz: Double, latitudeDeg: Double): Double {
        val facing = facingFromLatitude(latitudeDeg)
        return relativeBearing(objectAz, facing)
    }

    /**
     * Converts relative bearing to normalized screen X coordinate [0,1] where 0.5=center.
     * Formula: x = 0.5 + relAz/360
     */
    fun toScreenX(relAz: Double): Double {
        return 0.5 + relAz / 360.0
    }

    /**
     * Full projection: objectAz + latitude -> screen X
     */
    fun projectToScreenX(objectAz: Double, latitudeDeg: Double): Double {
        val relAz = relativeBearingFromLatitude(objectAz, latitudeDeg)
        return toScreenX(relAz)
    }
}
