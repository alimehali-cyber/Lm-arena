package com.zig.gravity.physics

import kotlin.math.sqrt

/**
 * §3.2 Constants charter (Roadmap v5.0 Final).
 *
 * Physical constants are exact CODATA/IAU values and are NEVER tunable.
 * Numerical parameters (EPS_SOFT, DT, MAX_SUBSTEPS, REFINE_TRIGGER) are test-gated
 * stability parameters and are NEVER exposed in the UI.
 *
 * Enforced by [com.alijafari.red.astronomy.gravity.ConstantsCharterTest] (test 31).
 */
object EngineConstants {

    // ---- Exact physical constants (never tunable) -------------------------------------------
    /** CODATA 2018 gravitational constant, m^3 kg^-1 s^-2. */
    const val G: Double = 6.67430e-11

    /** Exact speed of light, m/s. */
    const val C: Double = 2.99792458e8

    const val M_SUN: Double = 1.989e30
    const val R_SUN: Double = 6.957e8
    const val M_EARTH: Double = 5.972e24
    const val R_EARTH: Double = 6.371e6
    const val M_MOON: Double = 7.348e22
    const val R_MOON: Double = 1.737e6
    const val AU: Double = 1.496e11

    /** Reference values (§3.2) used by presets and tests. */
    const val MOON_ORBIT_RADIUS: Double = 3.844e8
    const val MOON_ORBIT_SPEED: Double = 1022.0
    const val EARTH_ORBIT_SPEED: Double = 29780.0

    // ---- Numerical parameters (test-gated, never UI-exposed) --------------------------------
    /** Plummer softening length, m. Force distortion ~ (3/2)(EPS_SOFT/r)^2. */
    const val EPS_SOFT: Double = 1.0e6
    const val EPS_SOFT_SQ: Double = EPS_SOFT * EPS_SOFT

    /** Fixed integration timestep, s. Speed multipliers NEVER scale this. */
    const val DT: Double = 3600.0

    /** Simulated seconds per real second at 1x. Earth year ~= 31.6 s. */
    const val BASE: Double = 1.0e6

    const val MAX_BODIES: Int = 20

    /** Counts refined inner steps (§3.6b). */
    const val MAX_SUBSTEPS: Int = 96

    /** Adaptive Safety Refinement (§3.6c). */
    const val MAX_REFINE_DEPTH: Int = 3
    const val REFINE_TRIGGER: Double = 0.2

    /** Engine hard velocity clamp, m/s (§3.7). */
    const val V_MAX: Double = 1.0e6

    /** Longest frame we are willing to integrate, s (§3.6b). */
    const val MAX_FRAME_SECONDS: Double = 0.1

    // ---- Scene scale (§3.2: derived, never stored) ------------------------------------------
    const val SCENE_WIDTH_AU: Double = 3.0

    /** metersPerDp for a 400 dp viewport is 1.122e9. Derived from the live viewport width. */
    fun metersPerDp(viewportWidthDp: Double): Double =
        if (viewportWidthDp <= 0.0) 1.122e9 else SCENE_WIDTH_AU * AU / viewportWidthDp

    // ---- Wormhole (§3.13) --------------------------------------------------------------------
    /** Temporal half of the dual cooldown, simulated seconds. */
    const val WORMHOLE_COOLDOWN_SIM_S: Double = 5.0e5

    /** Spatial half of the dual cooldown: multiples of the partner ring radius. */
    const val WORMHOLE_SPATIAL_GATE: Double = 1.5

    // ---- Rendering-side budgets that the physics layer owns ----------------------------------
    /** §3.9: preallocated trail ring buffers, one sample per rendered frame. */
    const val TRAIL_CAPACITY: Int = 240

    /** §3.9: slingshot / prediction test-particle horizon. */
    const val PREDICTION_STEPS: Int = 600

    /** §3.6b speed ladder. Multiplies BASE, never DT. */
    val SPEEDS: DoubleArray = doubleArrayOf(0.1, 0.25, 1.0, 4.0, 16.0)
    val SPEED_LABELS: Array<String> = arrayOf("0.1x", "0.25x", "1x", "4x", "16x")
    const val DEFAULT_SPEED_INDEX: Int = 2

    // ---- Derived physics helpers (§3.4) ------------------------------------------------------

    /** Schwarzschild radius, m. Informational only: this sandbox computes Newtonian gravity. */
    fun schwarzschildRadius(massKg: Double): Double = 2.0 * G * massKg / (C * C)

    /** Circular orbital speed at radius r around mass m. */
    fun circularSpeed(centralMass: Double, r: Double): Double =
        if (r <= 0.0) 0.0 else sqrt(G * centralMass / r)

    /** Escape speed at radius r from mass m. */
    fun escapeSpeed(centralMass: Double, r: Double): Double =
        if (r <= 0.0) 0.0 else sqrt(2.0 * G * centralMass / r)

    /** Specific orbital energy. Bound orbit when < 0. */
    fun specificOrbitalEnergy(vRel: Double, centralMass: Double, r: Double): Double =
        0.5 * vRel * vRel - G * centralMass / (r + EPS_SOFT)

    /** UI guidance cap (§3.7). The engine clamp catches anything this misses. */
    fun uiVelocityGuidance(centralMass: Double, r: Double): Double =
        minOf(2.0 * escapeSpeed(centralMass, r), V_MAX)
}
