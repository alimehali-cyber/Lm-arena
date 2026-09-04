package com.alijafari.red.astronomy.startracker.fusion

/**
 * Feature flag + tunable thresholds, default DISABLED.
 * Safety contract: flag false must result in ZERO behavioral difference anywhere in app.
 */
object StarTrackerConfig {

    /**
     * Master feature flag — MUST default to disabled/false.
     * When false, existing app behavior must be provably identical to before this phase.
     */
    const val ENABLED: Boolean = false

    /**
     * OD4 (final pass B1): apply magnetic declination to the compass azimuth, DEFAULT ON.
     * The rotation-vector sensor azimuth is MAGNETIC-north referenced while every sky
     * azimuth in the app is TRUE-north referenced; this flag enables the correction at
     * the single true-azimuth entry point (CompassARScreen.currentAzimuth, sensor branch).
     *
     * Guardrails (see MagneticDeclination.kt):
     *  - flag false            -> correction 0.0 exactly (identical to pre-fix behavior)
     *  - no GPS location       -> correction 0.0 exactly (identical to pre-fix behavior)
     *  - applied ONCE at the single entry point, never compounded
     *  - legacy user yaw calibration is rebased ONCE at upgrade (versioned marker)
     * UNEXECUTED on device (offline harness only); declination source on device is
     * android.hardware.GeomagneticField (WMM).
     */
    const val APPLY_MAGNETIC_DECLINATION: Boolean = true

    /**
     * Staleness threshold: if star lock age exceeds this, return passthrough (no star correction).
     * Default 5 seconds, conservative.
     * UNVALIDATED pending real tracking data, but based on gyro drift ~0.1-1°/min engineering estimate.
     */
    const val STALENESS_THRESHOLD_SECONDS: Double = 5.0

    /**
     * Magnetometer weight floor: when FULL_LOCK and fresh, reduce magnetometer weight toward this floor,
     * not necessarily hard zero, to avoid total blindness if star tracker wrong.
     * Default 0.1 (10% of original weight), conservative.
     * UNVALIDATED.
     */
    const val MAGNETOMETER_WEIGHT_FLOOR: Float = 0.1f

    /**
     * Magnetometer weight for MARGINAL_LOCK: moderate reduction.
     * Default 0.5 (50% of original).
     * UNVALIDATED.
     */
    const val MAGNETOMETER_WEIGHT_MARGINAL: Float = 0.5f

    /**
     * Blend fraction for FULL_LOCK fresh: how much star-solved quaternion dominates.
     * 1.0 = fully star, 0.0 = fully existing fused.
     * Default 0.9 = 90% star, 10% existing (strong dominance).
     * UNVALIDATED.
     */
    const val FULL_LOCK_BLEND_FRACTION: Double = 0.9

    /**
     * Blend fraction for MARGINAL_LOCK: partial blend.
     * Default 0.5 = 50% each.
     * UNVALIDATED.
     */
    const val MARGINAL_LOCK_BLEND_FRACTION: Double = 0.5

    /**
     * Staleness decay: blend weight smoothly decreases as age increases past threshold, down to 0 (full passthrough).
     * We use linear decay over STALENESS_DECAY_WINDOW seconds after threshold.
     * Default window 3 seconds.
     * UNVALIDATED.
     */
    const val STALENESS_DECAY_WINDOW_SECONDS: Double = 3.0
}
