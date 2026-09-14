package com.zig.museum.core.model

/**
 * Rotation control per §5.9 and M5 task 1
 * Real sidereal period, speeds 1x / 60x / 3600x / hold, driven purely by control, never wall clock per P4
 */

enum class RotationSpeed(val multiplier: Double, val labelEn: String, val labelFa: String) {
    HOLD(0.0, "Hold", "توقف"),
    X1(1.0, "1x", "1x"),
    X60(60.0, "60x", "60x"),
    X3600(3600.0, "3600x", "3600x")
}

data class RotationState(
    val speed: RotationSpeed = RotationSpeed.X1,
    val angleDeg: Double = 0.0, // current rotation angle
    val isPaused: Boolean = false
) {
    /**
     * Advance rotation by deltaTime seconds, using object's sidereal period
     * Pure function of control, never wall clock per P4
     * @param deltaTimeSec time delta from control, not wall clock
     * @param rotationPeriodHours object's sidereal period in hours (signed for retrograde)
     * @return new RotationState with updated angle
     */
    fun advance(deltaTimeSec: Double, rotationPeriodHours: Double): RotationState {
        if (isPaused || speed == RotationSpeed.HOLD) return this
        if (rotationPeriodHours == 0.0) return this
        // Rotation period in seconds
        val periodSec = rotationPeriodHours * 3600.0
        // Angular velocity deg per sec = 360 / periodSec
        val angularVelocityDegPerSec = 360.0 / periodSec
        // Apply speed multiplier
        val deltaAngle = angularVelocityDegPerSec * deltaTimeSec * speed.multiplier
        val newAngle = (angleDeg + deltaAngle) % 360.0
        return copy(angleDeg = newAngle)
    }
}
