package com.alijafari.red.astronomy.sandbox.render.camera

import android.opengl.Matrix
import kotlin.math.*

/**
 * High-precision orbital camera system for the Gravity Sandbox.
 *
 * Supports:
 * - Orbit around dynamic target (yaw/pitch)
 * - Screen-space panning along camera right/up basis vectors
 * - Smooth exponential distance zooming
 * - Automatic framing / fitting of active bodies
 * - Focus tracking on specific celestial bodies
 * - Screen-to-world ray casting for object selection
 * - Completely zero-allocation update loop
 */
class SandboxCamera {

    // --- Matrices (Pre-allocated to prevent GC churn) ---
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)
    val invertedViewProjectionMatrix = FloatArray(16)

    // --- Camera Transform State ---
    var yawDeg: Float = 45.0f
    var pitchDeg: Float = 25.0f
    var distance: Float = 35.0f

    // Target point in visual render space
    var targetX: Float = 0.0f
    var targetY: Float = 0.0f
    var targetZ: Float = 0.0f

    // Smooth interpolation targets
    private var desiredTargetX: Float = 0.0f
    private var desiredTargetY: Float = 0.0f
    private var desiredTargetZ: Float = 0.0f
    private var desiredDistance: Float = 35.0f

    // Computed Eye Position in visual render space
    var eyeX: Float = 0.0f
        private set
    var eyeY: Float = 0.0f
        private set
    var eyeZ: Float = 0.0f
        private set

    // Camera basis vectors in world space
    var rightX: Float = 1.0f
        private set
    var rightY: Float = 0.0f
        private set
    var rightZ: Float = 0.0f
        private set

    var upX: Float = 0.0f
        private set
    var upY: Float = 1.0f
        private set
    var upZ: Float = 0.0f
        private set

    var forwardX: Float = 0.0f
        private set
    var forwardY: Float = 0.0f
        private set
    var forwardZ: Float = -1.0f
        private set

    // Perspective Parameters
    var fovDegrees: Float = 45.0f
    var nearClip: Float = 0.1f
    var farClip: Float = 1000.0f
    var aspectRatio: Float = 1.0f
        private set

    var viewportWidth: Int = 1
        private set
    var viewportHeight: Int = 1
        private set

    // Constraints
    val minDistance: Float = 0.2f
    val maxDistance: Float = 250.0f
    val minPitchDeg: Float = -88.0f
    val maxPitchDeg: Float = 88.0f

    // Body follow target ID (if any)
    var focusedBodyId: String? = null

    init {
        reset()
    }

    fun reset() {
        yawDeg = 45.0f
        pitchDeg = 25.0f
        distance = 35.0f
        desiredDistance = 35.0f
        targetX = 0.0f
        targetY = 0.0f
        targetZ = 0.0f
        desiredTargetX = 0.0f
        desiredTargetY = 0.0f
        desiredTargetZ = 0.0f
        focusedBodyId = null
        updateMatrices()
    }

    fun setViewport(width: Int, height: Int) {
        viewportWidth = max(width, 1)
        viewportHeight = max(height, 1)
        aspectRatio = viewportWidth.toFloat() / viewportHeight.toFloat()
        updateProjection()
    }

    /**
     * Updates camera interpolation and matrices. Called once per render frame.
     */
    fun update(deltaTimeSec: Float = 0.016f) {
        // Smooth target interpolation
        val targetDamping = (1.0f - exp(-12.0f * deltaTimeSec)).coerceIn(0.01f, 1.0f)
        targetX += (desiredTargetX - targetX) * targetDamping
        targetY += (desiredTargetY - targetY) * targetDamping
        targetZ += (desiredTargetZ - targetZ) * targetDamping

        // Smooth distance zoom interpolation
        val zoomDamping = (1.0f - exp(-15.0f * deltaTimeSec)).coerceIn(0.01f, 1.0f)
        distance += (desiredDistance - distance) * zoomDamping

        updateMatrices()
    }

    /**
     * Orbits camera around current target.
     */
    fun orbit(deltaYawDeg: Float, deltaPitchDeg: Float) {
        yawDeg = (yawDeg + deltaYawDeg) % 360.0f
        if (yawDeg < 0f) yawDeg += 360.0f
        pitchDeg = (pitchDeg + deltaPitchDeg).coerceIn(minPitchDeg, maxPitchDeg)
    }

    /**
     * Pans target position across screen-space right and up planes.
     */
    fun pan(deltaScreenX: Float, deltaScreenY: Float) {
        // Scale pan sensitivity with distance
        val factor = (distance / max(viewportHeight.toFloat(), 1.0f)) * 1.5f
        val panX = -deltaScreenX * factor
        val panY = deltaScreenY * factor

        desiredTargetX += rightX * panX + upX * panY
        desiredTargetY += rightY * panX + upY * panY
        desiredTargetZ += rightZ * panX + upZ * panY

        targetX = desiredTargetX
        targetY = desiredTargetY
        targetZ = desiredTargetZ

        // Cancel body focus lock on manual pan
        focusedBodyId = null
    }

    /**
     * Multiplicative zoom (pinch or wheel).
     */
    fun zoomByMultiplier(multiplier: Float) {
        desiredDistance = (desiredDistance * multiplier).coerceIn(minDistance, maxDistance)
    }

    fun setTarget(x: Float, y: Float, z: Float, immediate: Boolean = false) {
        desiredTargetX = x
        desiredTargetY = y
        desiredTargetZ = z
        if (immediate) {
            targetX = x
            targetY = y
            targetZ = z
        }
    }

    fun setDistance(dist: Float, immediate: Boolean = false) {
        desiredDistance = dist.coerceIn(minDistance, maxDistance)
        if (immediate) {
            distance = desiredDistance
        }
    }

    fun focusOnPosition(x: Float, y: Float, z: Float, zoomDistance: Float? = null) {
        setTarget(x, y, z, immediate = false)
        if (zoomDistance != null) {
            setDistance(zoomDistance, immediate = false)
        }
    }

    fun fitBodies(renderedPositions: FloatArray, activeCount: Int, immediate: Boolean = false) {
        if (activeCount <= 0) {
            reset()
            return
        }

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (i in 0 until activeCount) {
            val idx = i * 3
            val x = renderedPositions[idx]
            val y = renderedPositions[idx + 1]
            val z = renderedPositions[idx + 2]

            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
        }

        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        val centerZ = (minZ + maxZ) * 0.5f

        val spanX = maxX - minX
        val spanY = maxY - minY
        val spanZ = maxZ - minZ
        val maxSpan = max(max(spanX, spanY), spanZ)

        setTarget(centerX, centerY, centerZ, immediate = immediate)
        val fitDistance = max(maxSpan * 1.8f, 5.0f)
        setDistance(fitDistance, immediate = immediate)
        focusedBodyId = null
    }

    private fun updateProjection() {
        Matrix.perspectiveM(
            projectionMatrix, 0,
            fovDegrees,
            aspectRatio,
            nearClip,
            farClip
        )
    }

    private fun updateMatrices() {
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)
        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)

        // Position on orbital sphere
        val offsetX = distance * cosPitch * sinYaw
        val offsetY = distance * sinPitch
        val offsetZ = distance * cosPitch * cosYaw

        eyeX = targetX + offsetX
        eyeY = targetY + offsetY
        eyeZ = targetZ + offsetZ

        // Forward vector (from eye to target)
        var fx = targetX - eyeX
        var fy = targetY - eyeY
        var fz = targetZ - eyeZ
        val fLen = sqrt(fx * fx + fy * fy + fz * fz)
        if (fLen > 1e-6f) {
            fx /= fLen
            fy /= fLen
            fz /= fLen
        }
        forwardX = fx
        forwardY = fy
        forwardZ = fz

        // Right vector (forward x worldUp(0, 1, 0))
        var rx = -fz
        var ry = 0.0f
        var rz = fx
        val rLen = sqrt(rx * rx + rz * rz)
        if (rLen > 1e-6f) {
            rx /= rLen
            rz /= rLen
        } else {
            rx = 1.0f
            rz = 0.0f
        }
        rightX = rx
        rightY = ry
        rightZ = rz

        // True Up vector (right x forward)
        upX = ry * fz - rz * fy
        upY = rz * fx - rx * fz
        upZ = rx * fy - ry * fx

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            targetX, targetY, targetZ,
            0.0f, 1.0f, 0.0f
        )

        Matrix.multiplyMM(
            viewProjectionMatrix, 0,
            projectionMatrix, 0,
            viewMatrix, 0
        )

        Matrix.invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0)
    }

    /**
     * Computes a 3D ray in world space from screen touch coordinates for object picking.
     * Ray origin is written to [outOrigin] (0..2), direction to [outDirection] (0..2).
     */
    fun computePickRay(
        touchX: Float,
        touchY: Float,
        outOrigin: FloatArray,
        outDirection: FloatArray
    ) {
        val ndcX = (2.0f * touchX) / viewportWidth - 1.0f
        val ndcY = 1.0f - (2.0f * touchY) / viewportHeight

        val nearPointNdc = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)
        val farPointNdc = floatArrayOf(ndcX, ndcY, 1.0f, 1.0f)

        val nearPointWorld = FloatArray(4)
        val farPointWorld = FloatArray(4)

        Matrix.multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0)
        Matrix.multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0)

        if (abs(nearPointWorld[3]) > 1e-6f) {
            nearPointWorld[0] /= nearPointWorld[3]
            nearPointWorld[1] /= nearPointWorld[3]
            nearPointWorld[2] /= nearPointWorld[3]
        }

        if (abs(farPointWorld[3]) > 1e-6f) {
            farPointWorld[0] /= farPointWorld[3]
            farPointWorld[1] /= farPointWorld[3]
            farPointWorld[2] /= farPointWorld[3]
        }

        outOrigin[0] = nearPointWorld[0]
        outOrigin[1] = nearPointWorld[1]
        outOrigin[2] = nearPointWorld[2]

        var dx = farPointWorld[0] - nearPointWorld[0]
        var dy = farPointWorld[1] - nearPointWorld[1]
        var dz = farPointWorld[2] - nearPointWorld[2]
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len > 1e-6f) {
            dx /= len
            dy /= len
            dz /= len
        }

        outDirection[0] = dx
        outDirection[1] = dy
        outDirection[2] = dz
    }
}
