package com.zig.museum.core.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/**
 * Camera rig per §5.4 — orbit camera: spherical coordinates around object centre with radius, yaw, pitch, damping.
 * Pinch to change radius logarithmically.
 * Tap-to-focus stub, double-tap reset, near/far from zoom per §5.3.
 *
 * Units: object radii. Camera distances expressed in object radii.
 * Near plane: 0.0005 radii when inspecting surface patch, up to 0.05 when framing whole body.
 * Far plane: 8.0 radii.
 * Zoom limits: min = 2 * data resolution in radii (so user can reach CEILING but not clip), max = 3.0 radii.
 */
data class CameraState(
    val radius: Float = 2.5f, // object radii, default framing full disk
    val yawDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val targetZ: Float = 0f
)

class CameraRig(
    initialState: CameraState = CameraState()
) {
    var state: CameraState = initialState

    // Damping factor for smooth interpolation
    var dampingFactor: Float = 0.15f

    // Zoom limits per §5.4
    var minRadius: Float = 1.01f // just outside surface (radius 1.0)
    var maxRadius: Float = 3.0f

    // For tap-to-focus: recentre target to surface point while preserving orientation
    fun focusOn(x: Float, y: Float, z: Float) {
        state = state.copy(targetX = x, targetY = y, targetZ = z)
    }

    fun reset() {
        state = CameraState()
    }

    fun orbit(deltaYawDeg: Float, deltaPitchDeg: Float) {
        var newYaw = state.yawDeg + deltaYawDeg
        var newPitch = state.pitchDeg + deltaPitchDeg
        // Clamp pitch to avoid gimbal lock, -89 to 89
        newPitch = max(-89f, min(89f, newPitch))
        // Wrap yaw
        newYaw = newYaw % 360f
        state = state.copy(yawDeg = newYaw, pitchDeg = newPitch)
    }

    fun zoom(factor: Float) {
        // Pinch to change radius logarithmically per §5.4
        // factor <1 zoom in, >1 zoom out
        val newRadius = (state.radius * factor).coerceIn(minRadius, maxRadius)
        state = state.copy(radius = newRadius)
    }

    fun computePosition(): Triple<Float, Float, Float> {
        // Spherical to cartesian, with target offset
        val yawRad = Math.toRadians(state.yawDeg.toDouble())
        val pitchRad = Math.toRadians(state.pitchDeg.toDouble())
        val r = state.radius
        // Orbit around target
        val x = r * cos(pitchRad) * sin(yawRad)
        val y = r * sin(pitchRad)
        val z = r * cos(pitchRad) * cos(yawRad)
        return Triple(
            (x + state.targetX).toFloat(),
            (y + state.targetY).toFloat(),
            (z + state.targetZ).toFloat()
        )
    }

    fun computeNearFar(): Pair<Float, Float> {
        // Near plane: 0.0005 radii when inspecting surface, up to 0.05 when framing whole body
        // Compute from current zoom per §5.3 within bounds
        // When radius close to surface (1.01), near = 0.0005, when far (3.0), near = 0.05
        // Linear interpolation
        val t = (state.radius - minRadius) / (maxRadius - minRadius).coerceAtLeast(0.001f)
        val near = 0.0005f + t * (0.05f - 0.0005f)
        val far = 8.0f
        return Pair(near, far)
    }

    companion object {
        fun fromPresets(): Map<String, CameraState> {
            // Per-object camera presets: full disk, pole-on, terminator view, hero region (data, not code per §5.4)
            return mapOf(
                "full_disk" to CameraState(radius = 2.5f, yawDeg = 0f, pitchDeg = 0f),
                "pole_on" to CameraState(radius = 2.5f, yawDeg = 0f, pitchDeg = 89f),
                "terminator" to CameraState(radius = 2.5f, yawDeg = 90f, pitchDeg = 0f)
            )
        }
    }
}
