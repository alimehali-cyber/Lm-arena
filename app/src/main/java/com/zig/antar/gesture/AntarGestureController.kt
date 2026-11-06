package com.zig.antar.gesture

import com.zig.antar.config.AntarCameraMode
import com.zig.antar.config.AntarObjectMode
import com.zig.antar.config.AntarSimulationSettings
import com.zig.antar.physics.AntarKerrMetrics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure-Kotlin, deterministic multi-touch camera controller for Antar Blackhole.
 *
 * Designed so every hand gesture maps 1:1 to screen-space physical intuition with zero user
 * adaptation:
 * - **1-finger drag**: screen-aligned arcball orbit & inclination tilt (compensated for any
 *   camera horizon roll angle, scaled by FOV and viewport size), or local head-look during
 *   free-fall plunge. Releases into a frame-rate-independent inertial glide (`v * e^(-λt)`).
 * - **2-finger pinch**: logarithmic zoom-in (spread) / zoom-out (pinch) on orbital distance $r/M$
 *   (or field-of-view during free-fall plunge).
 * - **2-finger drag**: 1:1 screen-space optical pan in CSS/dp coordinates.
 * - **2-finger twist**: 1:1 clockwise/counter-clockwise horizon roll after a 4° pinch-steadiness
 *   deadzone so casual pinch-zooms never wobble the horizon.
 * - **Double-tap**: re-centers pan, levels roll, and clears look-around offsets without altering
 *   any spacetime or accretion-disk parameters.
 */
class AntarGestureController(
    private val inertiaDamping: Double = 4.6,
    private val twistDeadzoneDeg: Double = 4.0
) {

    var panOffsetXDp: Double = 0.0
        private set
    var panOffsetYDp: Double = 0.0
        private set
    var lookYawRad: Double = 0.0
        private set
    var lookPitchRad: Double = 0.0
        private set

    var velAzDegPerSec: Double = 0.0
        private set
    var velInclDegPerSec: Double = 0.0
        private set
    var velYawRadPerSec: Double = 0.0
        private set
    var velPitchRadPerSec: Double = 0.0
        private set

    private var activePointerCount: Int = 0
    private var multiTouchUsedInGesture: Boolean = false
    private var accumulatedTwistDeg: Double = 0.0
    private var twistUnlocked: Boolean = false
    private var idleSinceLastMoveSec: Double = 0.0

    val hasActiveInertia: Boolean
        get() = activePointerCount == 0 && (
            abs(velAzDegPerSec) > 0.08 ||
                abs(velInclDegPerSec) > 0.08 ||
                abs(velYawRadPerSec) > 1e-3 ||
                abs(velPitchRadPerSec) > 1e-3
            )

    /**
     * Returns true when a touch starts inside the left/right system-gesture edge zone so Android's
     * back swipe never competes with black-hole rotation.
     */
    fun shouldIgnoreEdgeTouch(
        startXPx: Float,
        viewportWidthPx: Float,
        edgeGuardPx: Float
    ): Boolean {
        if (viewportWidthPx <= edgeGuardPx * 3f) return false
        return startXPx < edgeGuardPx || startXPx > (viewportWidthPx - edgeGuardPx)
    }

    /**
     * Called when the first finger touches the viewport. Stops any ongoing inertial glide and,
     * if the camera was in cinematic drift or guided tour, hands off seamlessly to manual orbit
     * at the exact instantaneous pose `(liveAzimuthDeg, liveInclinationDeg, liveDistanceM)`.
     */
    fun onGestureStart(
        settings: AntarSimulationSettings,
        liveAzimuthDeg: Double = settings.azimuthDeg,
        liveInclinationDeg: Double = settings.inclinationDeg,
        liveDistanceM: Double = settings.distanceM
    ): AntarSimulationSettings {
        stopInertia()
        activePointerCount = 1
        multiTouchUsedInGesture = false
        accumulatedTwistDeg = 0.0
        twistUnlocked = false
        idleSinceLastMoveSec = 0.0

        return if (settings.cameraMode == AntarCameraMode.CINEMATIC ||
            settings.cameraMode == AntarCameraMode.TOUR
        ) {
            settings.copy(
                cameraMode = AntarCameraMode.ORBIT,
                azimuthDeg = liveAzimuthDeg,
                inclinationDeg = liveInclinationDeg,
                distanceM = liveDistanceM
            ).normalize()
        } else {
            settings
        }
    }

    /** Updates active pointer count so 2->1 finger lift transitions never cause a 1-finger snap. */
    fun onPointerCountChanged(count: Int) {
        activePointerCount = count.coerceAtLeast(0)
        if (count >= 2) {
            multiTouchUsedInGesture = true
            stopInertia()
        }
        if (count < 2) {
            accumulatedTwistDeg = 0.0
            twistUnlocked = false
        }
    }

    /**
     * Applies a single-finger drag step `(dxPx, dyPx)` over `dtSec`.
     */
    fun onSingleFingerDrag(
        settings: AntarSimulationSettings,
        dxPx: Float,
        dyPx: Float,
        dtSec: Double,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        plungeActive: Boolean = settings.cameraMode == AntarCameraMode.PLUNGE
    ): AntarSimulationSettings {
        if (multiTouchUsedInGesture) {
            // Ignore residual single-finger tail after lifting one finger from a pinch/pan gesture.
            return settings
        }
        val minDim = min(viewportWidthPx, viewportHeightPx).coerceAtLeast(180f).toDouble()
        if (abs(dxPx) < 0.01f && abs(dyPx) < 0.01f) {
            idleSinceLastMoveSec += dtSec.coerceAtLeast(0.0)
            if (idleSinceLastMoveSec > 0.055) stopInertia()
            return settings
        }
        idleSinceLastMoveSec = 0.0

        val rollRad = Math.toRadians(settings.rollDeg)
        val c = cos(rollRad)
        val s = sin(rollRad)
        // Un-rotate screen delta into the black hole's unrolled camera basis so dragging right/down
        // always moves the disk right/down on screen regardless of horizon roll (e.g. M87* 90°).
        val dxWorld = dxPx * c - dyPx * s
        val dyWorld = dxPx * s + dyPx * c

        val degPerPx = (settings.fovDeg / minDim) * 1.60 * settings.gestureSensitivity
        val safeDt = dtSec.coerceIn(1.0 / 240.0, 0.12)

        if (plungeActive) {
            val dYaw = Math.toRadians(dxWorld * degPerPx * 0.78)
            val dPitch = -Math.toRadians(dyWorld * degPerPx * 0.78)
            lookYawRad = (lookYawRad + dYaw).coerceIn(-PI, PI)
            lookPitchRad = (lookPitchRad + dPitch).coerceIn(-1.40, 1.40)
            if (settings.inertialGlide) {
                val instYaw = (dYaw / safeDt).coerceIn(-6.0, 6.0)
                val instPitch = (dPitch / safeDt).coerceIn(-6.0, 6.0)
                velYawRadPerSec = velYawRadPerSec * 0.35 + instYaw * 0.65
                velPitchRadPerSec = velPitchRadPerSec * 0.35 + instPitch * 0.65
            }
            return settings
        }

        val dAz = -dxWorld * degPerPx
        val dIncl = -dyWorld * degPerPx
        val nextAz = wrapAngleDeg(settings.azimuthDeg + dAz)
        val rawIncl = settings.inclinationDeg + dIncl
        val nextIncl = rawIncl.coerceIn(1.5, 178.5)

        if (settings.inertialGlide) {
            val instAz = (dAz / safeDt).coerceIn(-540.0, 540.0)
            val instIncl = if (rawIncl in 1.5..178.5) {
                (dIncl / safeDt).coerceIn(-540.0, 540.0)
            } else {
                0.0
            }
            velAzDegPerSec = velAzDegPerSec * 0.35 + instAz * 0.65
            velInclDegPerSec = velInclDegPerSec * 0.35 + instIncl * 0.65
        } else {
            stopInertia()
        }

        return settings.copy(
            cameraMode = if (settings.cameraMode == AntarCameraMode.ORBIT) {
                AntarCameraMode.ORBIT
            } else {
                AntarCameraMode.ORBIT
            },
            azimuthDeg = nextAz,
            inclinationDeg = nextIncl
        )
    }

    /**
     * Applies a two-finger transform step:
     * - [zoomChange]: ratio of current finger span to previous span (`> 1` zooms in, `< 1` zooms out).
     * - [panDxDp], [panDyDp]: centroid translation in dp/CSS px (right > 0, down > 0).
     * - [rotationDeg]: clockwise screen rotation delta in degrees (`> 0` clockwise).
     */
    fun onTwoFingerTransform(
        settings: AntarSimulationSettings,
        zoomChange: Float,
        panDxDp: Float,
        panDyDp: Float,
        rotationDeg: Float,
        viewportWidthDp: Float,
        viewportHeightDp: Float,
        plungeActive: Boolean = settings.cameraMode == AntarCameraMode.PLUNGE
    ): AntarSimulationSettings {
        multiTouchUsedInGesture = true
        stopInertia()

        val maxPanX = (viewportWidthDp * 0.45f).coerceAtLeast(120f).toDouble()
        val maxPanY = (viewportHeightDp * 0.45f).coerceAtLeast(120f).toDouble()
        panOffsetXDp = (panOffsetXDp + panDxDp.toDouble()).coerceIn(-maxPanX, maxPanX)
        panOffsetYDp = (panOffsetYDp + panDyDp.toDouble()).coerceIn(-maxPanY, maxPanY)

        val clampedZoom = zoomChange.toDouble().takeIf { it.isFinite() && it > 0.01 }?.coerceIn(0.4, 2.5) ?: 1.0
        val isMerger = settings.objectMode == AntarObjectMode.BINARY_MERGER
        val minDist = AntarKerrMetrics.minOrbitDistanceM(settings.spin, isMerger, settings.binarySeparationM)

        var nextDist = settings.distanceM
        var nextFov = settings.fovDeg
        if (plungeActive) {
            nextFov = (settings.fovDeg / clampedZoom).coerceIn(18.0, 110.0)
        } else {
            nextDist = (settings.distanceM / clampedZoom).coerceIn(minDist, 220.0)
        }

        var nextRoll = settings.rollDeg
        if (settings.twoFingerRollEnabled && rotationDeg.isFinite()) {
            var rot = rotationDeg.toDouble()
            // Guard against ±360° atan2 wrap spikes across frames.
            if (rot > 180.0) rot -= 360.0
            if (rot < -180.0) rot += 360.0
            if (!twistUnlocked) {
                accumulatedTwistDeg += rot
                if (abs(accumulatedTwistDeg) >= twistDeadzoneDeg) {
                    twistUnlocked = true
                    val residual = accumulatedTwistDeg - twistDeadzoneDeg * kotlin.math.sign(accumulatedTwistDeg)
                    // Subtract clockwise screen rotation so the accretion disk rotates clockwise with fingers.
                    nextRoll = wrapAngleDeg(nextRoll - residual * settings.gestureSensitivity.coerceIn(0.7, 1.3))
                }
            } else {
                nextRoll = wrapAngleDeg(nextRoll - rot * settings.gestureSensitivity.coerceIn(0.7, 1.3))
            }
        }

        return settings.copy(
            cameraMode = if (plungeActive) AntarCameraMode.PLUNGE else AntarCameraMode.ORBIT,
            distanceM = nextDist,
            fovDeg = nextFov,
            rollDeg = nextRoll
        )
    }

    /** Called when all fingers leave the viewport. */
    fun onGestureEnd(settings: AntarSimulationSettings, reducedMotion: Boolean = false) {
        activePointerCount = 0
        if (!settings.inertialGlide || reducedMotion || multiTouchUsedInGesture || idleSinceLastMoveSec > 0.055) {
            stopInertia()
        }
        multiTouchUsedInGesture = false
        accumulatedTwistDeg = 0.0
        twistUnlocked = false
    }

    /**
     * Advances post-drag inertial glide by [dtSec] using the exact closed-form integral of
     * $v(t) = v_0 e^{-\lambda t}$ so total travel distance is invariant across 30/60/120 Hz.
     */
    fun tickInertia(
        settings: AntarSimulationSettings,
        dtSec: Double,
        plungeActive: Boolean = settings.cameraMode == AntarCameraMode.PLUNGE,
        reducedMotion: Boolean = false
    ): AntarSimulationSettings {
        if (reducedMotion || !settings.inertialGlide) {
            stopInertia()
            return settings
        }
        if (!hasActiveInertia || dtSec <= 0.0 || !dtSec.isFinite()) return settings

        val clampedDt = dtSec.coerceAtMost(0.25)
        val decay = exp(-inertiaDamping * clampedDt)
        val integral = (1.0 - decay) / inertiaDamping

        if (plungeActive) {
            lookYawRad = (lookYawRad + velYawRadPerSec * integral).coerceIn(-PI, PI)
            val rawPitch = lookPitchRad + velPitchRadPerSec * integral
            lookPitchRad = rawPitch.coerceIn(-1.40, 1.40)
            velYawRadPerSec *= decay
            velPitchRadPerSec = if (rawPitch in -1.40..1.40) velPitchRadPerSec * decay else 0.0
            if (abs(velYawRadPerSec) <= 1e-3 && abs(velPitchRadPerSec) <= 1e-3) stopInertia()
            return settings
        }

        val nextAz = wrapAngleDeg(settings.azimuthDeg + velAzDegPerSec * integral)
        val rawIncl = settings.inclinationDeg + velInclDegPerSec * integral
        val nextIncl = rawIncl.coerceIn(1.5, 178.5)

        velAzDegPerSec *= decay
        velInclDegPerSec = if (rawIncl in 1.5..178.5) velInclDegPerSec * decay else 0.0
        if (abs(velAzDegPerSec) <= 0.08 && abs(velInclDegPerSec) <= 0.08) {
            stopInertia()
        }

        return settings.copy(
            azimuthDeg = nextAz,
            inclinationDeg = nextIncl
        )
    }

    /**
     * Double-tap action:
     * 1. If panned, rolled, or looking off-axis, re-centers pan/roll/look immediately.
     * 2. If already centered and level, restores comfortable default distance & FOV for the scene
     *    while preserving all spacetime, mass, disk, and visual settings.
     */
    fun onDoubleTap(settings: AntarSimulationSettings): AntarSimulationSettings {
        stopInertia()
        val hadOffset = abs(panOffsetXDp) > 1.0 ||
            abs(panOffsetYDp) > 1.0 ||
            abs(settings.rollDeg) > 0.5 ||
            abs(lookYawRad) > 0.01 ||
            abs(lookPitchRad) > 0.01

        panOffsetXDp = 0.0
        panOffsetYDp = 0.0
        lookYawRad = 0.0
        lookPitchRad = 0.0

        return if (hadOffset) {
            settings.copy(rollDeg = 0.0).normalize()
        } else {
            val reset = settings.resetCameraView()
            settings.copy(
                cameraMode = if (settings.cameraMode == AntarCameraMode.PLUNGE) AntarCameraMode.PLUNGE else AntarCameraMode.ORBIT,
                distanceM = reset.distanceM,
                fovDeg = reset.fovDeg,
                rollDeg = 0.0
            ).normalize()
        }
    }

    /** Resets transient pan/look/inertia offsets (called on explicit Reset View or preset load). */
    fun clearTransientOffsets() {
        stopInertia()
        panOffsetXDp = 0.0
        panOffsetYDp = 0.0
        lookYawRad = 0.0
        lookPitchRad = 0.0
    }

    fun stopInertia() {
        velAzDegPerSec = 0.0
        velInclDegPerSec = 0.0
        velYawRadPerSec = 0.0
        velPitchRadPerSec = 0.0
    }

    companion object {
        fun wrapAngleDeg(deg: Double): Double {
            if (!deg.isFinite()) return 0.0
            var r = deg % 360.0
            if (r > 180.0) r -= 360.0
            if (r < -180.0) r += 360.0
            return r
        }
    }
}
