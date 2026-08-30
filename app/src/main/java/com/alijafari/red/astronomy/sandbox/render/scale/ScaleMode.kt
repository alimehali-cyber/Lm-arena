package com.alijafari.red.astronomy.sandbox.render.scale

/**
 * Visual scaling modes separating physical SI integration coordinates from GPU render space.
 */
enum class ScaleMode {
    /**
     * Direct linear mapping where 1 render unit = [metersPerUnit] meters.
     * Ideal for two-body planetary orbits, close encounters, or local dynamics.
     */
    LINEAR,

    /**
     * Non-linear compression for interplanetary distances that brings outer planets into view
     * while keeping relative directions and order intact.
     */
    SOLAR_SYSTEM_COMPRESSED,

    /**
     * Geared for planet-satellite systems (Earth-Moon, Jupiter moons).
     */
    PLANETARY_SYSTEM,

    /**
     * 1:1 normalized view focused on a single body or tight binary inspection.
     */
    INSPECTION
}
