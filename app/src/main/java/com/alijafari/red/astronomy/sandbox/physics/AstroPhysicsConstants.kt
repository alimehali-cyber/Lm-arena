package com.alijafari.red.astronomy.sandbox.physics

/**
 * Standard CODATA 2018 / IAU astronomical constants in SI units (kg, m, s, m/s).
 */
object AstroPhysicsConstants {
    /**
     * Newtonian constant of gravitation G in m^3 kg^-1 s^-2 (CODATA 2018).
     */
    const val G: Double = 6.67430e-11

    /**
     * Exact speed of light in vacuum c in m/s (SI definition).
     */
    const val SPEED_OF_LIGHT: Double = 299792458.0

    /**
     * 1 Astronomical Unit (AU) in meters (IAU 2012 exact definition).
     */
    const val ASTRONOMICAL_UNIT_METERS: Double = 149597870700.0

    /**
     * Solar mass (M_sun) in kg.
     */
    const val SOLAR_MASS_KG: Double = 1.98847e30

    /**
     * Solar nominal radius (R_sun) in meters.
     */
    const val SOLAR_RADIUS_METERS: Double = 6.957e8

    /**
     * Earth mass (M_earth) in kg.
     */
    const val EARTH_MASS_KG: Double = 5.9722e24

    /**
     * Earth volumetric mean radius (R_earth) in meters.
     */
    const val EARTH_RADIUS_METERS: Double = 6.371e6

    /**
     * Moon mass (M_moon) in kg.
     */
    const val MOON_MASS_KG: Double = 7.342e22

    /**
     * Moon volumetric mean radius (R_moon) in meters.
     */
    const val MOON_RADIUS_METERS: Double = 1.7374e6

    /**
     * Maximum number of simultaneous simulated bodies supported by the sandbox core.
     */
    const val MAX_BODIES: Int = 20
}
