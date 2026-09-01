package com.zig.gravity.sim

import com.zig.gravity.physics.EngineConstants
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

import kotlin.math.sin

/**
 * Tabletop camera.
 *
 * Deliberately a **plain Kotlin class**, not Compose state: it is mutated from the gesture layer,
 * read inside the canvas draw lambda (which is already invalidated through the ViewModel's frame
 * tick) and exercised directly by JVM unit tests. No Android, no Compose, no allocation on the
 * hot path.
 *
 * ### Coordinate model
 *
 * The simulation plane is 2D. The camera applies, in order:
 *
 *  1. translation by `-(panX, panY)` — the scene point that sits at the screen centre;
 *  2. rotation by [yawRad] — the tabletop's orientation;
 *  3. uniform scale `basePxPerMeter * zoom`;
 *  4. a vertical squash by `cos(tiltRad)` — an orthographic "look at the table from a lower
 *     angle" elevation. It squashes the *plane*, never the bodies: a sphere projects to a circle
 *     from any viewing angle, so body radii are unaffected.
 *
 * Every mapping in the app goes through [toScreenX]/[toScreenY]/[toSceneX]/[toSceneY]; there is no
 * second copy of this arithmetic anywhere. [toScene] is the exact inverse of [toScreen] and that
 * round-trip is unit-tested.
 */
class CameraState {

    /** Scene metres that sit under the centre of the viewport. */
    var panX: Double = 0.0
        private set
    var panY: Double = 0.0
        private set

    /** 1.0 renders exactly [EngineConstants.SCENE_WIDTH_AU] across the viewport. */
    var zoom: Double = 1.0
        private set

    /** Tabletop orientation, radians, CCW. */
    var yawRad: Double = 0.0
        private set

    /** Elevation, radians. 0 = straight down; [MAX_TILT] = the shallowest allowed angle. */
    var tiltRad: Double = 0.0
        private set

    /** Bumped by every mutation so the renderer and tests can detect a change cheaply. */
    var version: Int = 0
        private set

    val cosTilt: Double get() = cos(tiltRad)

    // ---- mutation ------------------------------------------------------------------------------

    fun setPan(x: Double, y: Double) {
        panX = x
        panY = y
        version++
    }

    fun setZoom(value: Double) {
        zoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
        version++
    }

    fun setYaw(value: Double) {
        yawRad = wrapAngle(value)
        version++
    }

    fun setTilt(value: Double) {
        tiltRad = value.coerceIn(0.0, MAX_TILT)
        version++
    }

    fun reset() {
        panX = 0.0
        panY = 0.0
        zoom = 1.0
        yawRad = 0.0
        tiltRad = 0.0
        version++
    }

    /**
     * Applies one multi-touch transform step.
     *
     * @param centroidX,centroidY the gesture centroid in **pixels**, the anchor the zoom and the
     *        rotation pivot around, so the scene point under the fingers stays under the fingers.
     * @param panPxX,panPxY centroid displacement in pixels since the previous event.
     * @param zoomFactor pinch ratio since the previous event (1.0 = unchanged).
     * @param rotationRad twist since the previous event, radians.
     */
    fun applyTransform(
        centroidX: Float,
        centroidY: Float,
        panPxX: Float,
        panPxY: Float,
        zoomFactor: Float,
        rotationRad: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float
    ) {
        if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return

        // The scene point currently under the centroid. Everything below keeps it pinned there,
        // which is what stops a pinch or a twist from throwing the table sideways.
        val anchorSceneX = toSceneX(centroidX, centroidY, viewportWidthPx, viewportHeightPx)
        val anchorSceneY = toSceneY(centroidX, centroidY, viewportWidthPx, viewportHeightPx)

        if (zoomFactor > 0f && zoomFactor.isFinite() && abs(zoomFactor - 1f) > 1.0e-5f) {
            setZoom(zoom * zoomFactor)
        }
        if (rotationRad.isFinite() && abs(rotationRad) > 1.0e-5f) {
            // Screen y grows downward, so a clockwise finger twist is a negative scene rotation.
            setYaw(yawRad - rotationRad.toDouble())
        }

        // Re-pin the anchor after the scale/rotation change...
        val afterX = toScreenX(anchorSceneX, anchorSceneY, viewportWidthPx, viewportHeightPx)
        val afterY = toScreenY(anchorSceneX, anchorSceneY, viewportWidthPx, viewportHeightPx)
        shiftByPixels(centroidX - afterX, centroidY - afterY, viewportWidthPx, viewportHeightPx)

        // ...then move the camera by the centroid's own travel: a two-finger drag is a pan.
        shiftByPixels(panPxX, panPxY, viewportWidthPx, viewportHeightPx)
    }

    /** Moves the camera so that whatever was under a point follows a pixel displacement. */
    fun shiftByPixels(dxPx: Float, dyPx: Float, viewportWidthPx: Float, viewportHeightPx: Float) {
        if (dxPx == 0f && dyPx == 0f) return
        val k = pxPerMeter(viewportWidthPx)
        if (k <= 0.0) return
        val ct = max(cosTilt, MIN_COS_TILT)
        // Undo squash, then undo rotation, to express the pixel travel in scene axes.
        val rx = dxPx / k
        val ry = -dyPx / (k * ct)
        val c = cos(yawRad)
        val s = sin(yawRad)
        val dx = rx * c + ry * s
        val dy = -rx * s + ry * c
        setPan(panX - dx, panY - dy)
    }

    /** Frames [halfSpanMeters] around ([centerX], [centerY]) with a little breathing room. */
    fun frame(centerX: Double, centerY: Double, halfSpanMeters: Double) {
        setPan(centerX, centerY)
        val half = EngineConstants.SCENE_WIDTH_AU * EngineConstants.AU * 0.5
        val target = if (halfSpanMeters <= 0.0) 1.0 else half / (halfSpanMeters * FRAME_MARGIN)
        setZoom(target)
        setYaw(0.0)
    }

    // ---- projection ----------------------------------------------------------------------------

    /** Pixels per scene metre at the current zoom. */
    fun pxPerMeter(viewportWidthPx: Float): Double {
        if (viewportWidthPx <= 0f) return 1.0
        return viewportWidthPx / (EngineConstants.SCENE_WIDTH_AU * EngineConstants.AU) * zoom
    }

    fun toScreenX(sceneX: Double, sceneY: Double, w: Float, h: Float): Float {
        val k = pxPerMeter(w)
        val dx = sceneX - panX
        val dy = sceneY - panY
        val rx = dx * cos(yawRad) - dy * sin(yawRad)
        return (w * 0.5f + (rx * k).toFloat())
    }

    fun toScreenY(sceneX: Double, sceneY: Double, w: Float, h: Float): Float {
        val k = pxPerMeter(w)
        val dx = sceneX - panX
        val dy = sceneY - panY
        val ry = dx * sin(yawRad) + dy * cos(yawRad)
        return (h * 0.5f - (ry * k * max(cosTilt, MIN_COS_TILT)).toFloat())
    }

    fun toSceneX(px: Float, py: Float, w: Float, h: Float): Double {
        val k = pxPerMeter(w)
        if (k <= 0.0) return panX
        val rx = (px - w * 0.5f) / k
        val ry = -(py - h * 0.5f) / (k * max(cosTilt, MIN_COS_TILT))
        return rx * cos(yawRad) + ry * sin(yawRad) + panX
    }

    fun toSceneY(px: Float, py: Float, w: Float, h: Float): Double {
        val k = pxPerMeter(w)
        if (k <= 0.0) return panY
        val rx = (px - w * 0.5f) / k
        val ry = -(py - h * 0.5f) / (k * max(cosTilt, MIN_COS_TILT))
        return -rx * sin(yawRad) + ry * cos(yawRad) + panY
    }

    /** Copies another camera's pose (used when restoring a session). */
    fun copyFrom(other: CameraState) {
        panX = other.panX
        panY = other.panY
        zoom = other.zoom
        yawRad = other.yawRad
        tiltRad = other.tiltRad
        version++
    }

    companion object {
        const val MIN_ZOOM: Double = 0.01
        const val MAX_ZOOM: Double = 40.0

        /** ~62 degrees off vertical. Beyond this the plane collapses and picking gets unusable. */
        const val MAX_TILT: Double = 1.08

        private const val MIN_COS_TILT: Double = 0.35
        private const val FRAME_MARGIN: Double = 1.12

        fun wrapAngle(a: Double): Double {
            var v = a
            while (v > PI) v -= 2.0 * PI
            while (v < -PI) v += 2.0 * PI
            return v
        }

        /**
         * §11 display-size policy — deterministic, documented, and independent of the physical
         * collision radius.
         *
         * At zoom >= 1 the drawn radius is exactly the body's scene radius, so what you see is
         * what collides. Below 1 the radius shrinks only as `zoom^0.35`, so zooming out to frame
         * Neptune keeps the Sun visibly dominant over Jupiter, Jupiter over Earth and Earth over
         * the Moon instead of collapsing every body into the same unreadable dot.
         */
        fun displayScale(zoom: Double): Double =
            if (zoom >= 1.0) zoom else Math.pow(max(zoom, 1.0e-6), 0.35)

        /** Smallest radius any body is drawn at, in dp, so nothing ever disappears completely. */
        const val MIN_DRAW_DP: Double = 1.6

        /**
         * Drawn radius in dp. A body whose scene radius is `radiusDp` occupies `radiusDp * zoom`
         * dp on screen under a plain linear camera; this replaces the linear factor with
         * [displayScale] and applies the visibility floor.
         */
        fun drawnRadiusDp(radiusDp: Double, zoom: Double): Double =
            max(MIN_DRAW_DP, radiusDp * displayScale(zoom))
    }
}
