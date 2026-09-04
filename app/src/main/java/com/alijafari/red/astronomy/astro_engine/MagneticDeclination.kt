package com.alijafari.red.astronomy.astro_engine

/**
 * Magnetic declination correction (final pass B1 / OD4).
 *
 * The compass azimuth entering the app comes from TYPE_ROTATION_VECTOR, whose world
 * frame is referenced to MAGNETIC north (SensorManager.getRotationMatrix contract:
 * world Y axis points to magnetic north). All catalog/ephemeris azimuths in this app
 * are TRUE-north referenced, so the sensor azimuth must be corrected by the local
 * magnetic declination D (east-positive) exactly once, at the single point where the
 * sensor azimuth enters the render chain (CompassARScreen.currentAzimuth).
 *
 *      true = magnetic + D        (D > 0 east, D < 0 west)
 *
 * The declination VALUE is acquired on-device from android.hardware.GeomagneticField
 * (WMM-based); this object holds only the pure, unit-testable math so the harness can
 * verify it without Android.
 */
object MagneticDeclination {

    /** Wrap an angle to [0, 360). */
    fun wrap360(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

    /**
     * Convert a magnetic-north azimuth to a true-north azimuth.
     * [declinationDeg] is east-positive (add), west-negative (subtract).
     * Pure function: no state, no platform calls. Wrap-around safe.
     * declination == 0.0 returns the input BIT-IDENTICAL (guardrail semantics:
     * flag off / no location must be a provable no-op, not just an equal value).
     */
    fun trueAzimuth(magneticAzimuthDeg: Double, declinationDeg: Double): Double =
        if (declinationDeg == 0.0) magneticAzimuthDeg
        else wrap360(magneticAzimuthDeg + declinationDeg)

    /**
     * OD4 one-time rebase of a legacy stored yaw calibration offset.
     *
     * Before the declination fix, the app's azimuth was magnetic-referenced, so a user
     * yaw offset calibrated against a star of known TRUE position absorbed the local
     * declination (yaw_legacy ≈ D). With the fix active the declination is already
     * applied upstream, so the legacy offset would double-correct; it must be reduced
     * by D once at upgrade:
     *
     *      yaw_rebased = yaw_legacy - D
     */
    fun rebaseLegacyYawOffset(yawOffsetDeg: Float, declinationDeg: Float): Float =
        yawOffsetDeg - declinationDeg
}
