/*
 * NATIVE GLES 2.0 3D EARTH COMPONENT
 *
 * Adapted from the drop-in Earth3D component for com.alijafari.red.astronomy.
 * Uses bundled local textures in app/src/main/assets/ (no runtime network access):
 *   earth_day_4k.jpg, earth_night_2k.jpg, earth_clouds.jpg, earth_bump_2k.jpg
 *
 * Hosted inside an in-window TextureView with a dedicated EGL14 GLES 2.0 context so it
 * composites seamlessly inside scrolling Compose layouts, respects card clipping and tab
 * crossfades, and renders cleanly beneath interactive Compose overlays (satellite orbits,
 * pins, labels, navigation bars, and modals).
 *
 * Native GLES 2.0 renderer; no WebView, Three.js, network permission, or atmosphere halo.
 */
package com.alijafari.red.astronomy

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.ViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Appearance and motion options for [Earth3D].
 * This class is intentionally small and immutable so it can be driven by Compose state.
 */
@Immutable
data class EarthStyle(
    val showClouds: Boolean = true,
    val showNightLights: Boolean = true,
    val autoRotate: Boolean = true,
    /** Earth turns at this many radians per second. */
    val rotationSpeed: Float = 0.16f,
    /** Surface ambient contribution from 0 (night side is dark) to 1 (uniformly lit). */
    val ambientLight: Float = 0.12f,
    val sunAzimuthDegrees: Float = 38f,
    val sunElevationDegrees: Float = 28f,
    /** Initial camera distance, in Earth radii. */
    val cameraDistance: Float = 3.45f,
    /** Multiplier for the visible cloud layer opacity. */
    val cloudOpacity: Float = 0.48f,
    /** Exposure adjustment in stops, clamped to [-2, 2]. */
    val exposureEV: Float = 0f,
    /** Color saturation multiplier: 0 is monochrome, 1 is neutral, 2 is vivid. */
    val saturation: Float = 1f,
    /**
     * Optional Earth-fixed astronomical sub-solar latitude/longitude (degrees, East positive).
     * When both are non-null, the solar terminator, bump relief shadows, cloud lighting, and night
     * city lights remain locked to Earth's real day/night hemisphere as the user orbits the camera.
     */
    val subSolarLatitudeDegrees: Float? = null,
    val subSolarLongitudeDegrees: Float? = null,
    /**
     * When true, clears outside the globe to premultiplied transparent alpha so the 3D planet
     * floats borderlessly over the surrounding screen backdrop or card.
     */
    val transparentBackground: Boolean = false,
    val viewportProjection: EarthViewportProjection = EarthViewportProjection(),
    val fullViewportGestures: Boolean = false,
) {
    fun sanitized(): EarthStyle = copy(
        rotationSpeed = rotationSpeed.coerceIn(-1.2f, 1.2f),
        ambientLight = ambientLight.coerceIn(0f, 0.8f),
        cloudOpacity = cloudOpacity.coerceIn(0f, 1f),
        exposureEV = exposureEV.coerceIn(-2f, 2f),
        saturation = saturation.coerceIn(0f, 2f),
        cameraDistance = cameraDistance.coerceIn(2.25f, 6f),
    )
}

/** Optical framing shared by the GLES sphere and its CPU-projected annotations. */
@Immutable
data class EarthViewportProjection(val centerY: Float = 0.5f, val focalScale: Float = 1f)

/** Immutable snapshot of the 3D Earth orbit camera, shared with synchronized overlays. */
@Immutable
data class EarthCameraSnapshot(
    val yawRad: Float,
    val pitchRad: Float,
    val distance: Float,
    val projection: EarthViewportProjection = EarthViewportProjection(),
)

/** Result of projecting a geographic `(lat, lon, altKm)` coordinate through the 3D Earth camera. */
@Immutable
data class EarthProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val visible: Boolean,
    /** Smooth 0..1 visibility factor near Earth's perspective horizon limb. */
    val limbFade: Float,
)

/**
 * Pure geographic and camera projection math matching the GLES 2.0 renderer byte-for-byte:
 * - Unit sphere (`R = 1`): `+Y` North, `+Z` `(lat=0°, lon=0°)` Greenwich, `+X` `(lat=0°, lon=90°E)`.
 * - Equirectangular UV: `u = lon/360 + 0.5` (`0 = 180°W`, `0.5 = 0°`, `1 = 180°E`),
 *   `v = 0.5 - lat/180` (`0 = 90°N`, `1 = 90°S`).
 * - Model rotation: `M = Rx(pitch) * Ry(yaw)`, camera at `(0, 0, distance)` looking at origin
 *   with vertical FOV [FOV_Y_DEGREES].
 */
object Earth3DMath {
    const val EARTH_MEAN_RADIUS_KM: Double = 6371.0
    const val FOV_Y_DEGREES: Float = 38f
    const val NEAR_PLANE: Float = 0.1f
    const val FAR_PLANE: Float = 20f
    const val CLOUD_SHELL_SCALE: Float = 1.008f
    const val MIN_PITCH_RAD: Float = -1.35f
    const val MAX_PITCH_RAD: Float = 1.35f
    const val MIN_ZOOM_DISTANCE: Float = 1.7f
    const val MAX_ZOOM_DISTANCE: Float = 6.0f
    const val DEFAULT_YAW_RAD: Float = -0.75f
    const val DEFAULT_PITCH_RAD: Float = 0f
    const val DEFAULT_DISTANCE: Float = 3.45f

    private val TAN_HALF_FOV_Y: Float = tan(Math.toRadians(FOV_Y_DEGREES * 0.5).toFloat().toDouble()).toFloat()
    private const val TWO_PI_F: Float = (2.0 * PI).toFloat()
    private const val PI_F: Float = PI.toFloat()

    val REQUIRED_TEXTURE_ASSETS: List<String> = listOf(
        "earth_day_4k.jpg",
        "earth_night_2k.jpg",
        "earth_clouds.jpg",
        "earth_bump_2k.jpg",
    )

    /** Normalizes an angle in radians into `[-PI, +PI]`. */
    fun normalizeAngleRad(rad: Float): Float {
        var a = rad % TWO_PI_F
        if (a > PI_F) a -= TWO_PI_F
        if (a < -PI_F) a += TWO_PI_F
        return a
    }

    /** Shortest signed angular delta `to - from` in `[-PI, +PI]`. */
    fun shortestAngleDeltaRad(fromRad: Float, toRad: Float): Float =
        normalizeAngleRad(toRad - fromRad)

    /** Camera `(yawRad, pitchRad)` that places `(latDeg, lonDeg)` dead-center facing `+Z`. */
    fun cameraAnglesForLatLon(latDeg: Double, lonDeg: Double): Pair<Float, Float> {
        val yaw = normalizeAngleRad(-Math.toRadians(lonDeg).toFloat())
        val pitch = Math.toRadians(latDeg).toFloat().coerceIn(MIN_PITCH_RAD, MAX_PITCH_RAD)
        return yaw to pitch
    }

    /** Converts orbital altitude above mean sea level (km) to sphere radius in Earth radii. */
    fun altitudeKmToRadius(altKm: Double): Float =
        1f + (altKm.coerceAtLeast(0.0) / EARTH_MEAN_RADIUS_KM).toFloat()

    /** Converts geographic latitude/longitude (degrees, East positive) and radius to model space. */
    fun latLonToModel(latDeg: Double, lonDeg: Double, radius: Float = 1f, out: FloatArray = FloatArray(3)): FloatArray {
        val lat = Math.toRadians(latDeg)
        val lon = Math.toRadians(lonDeg)
        val ring = cos(lat)
        out[0] = (radius * ring * sin(lon)).toFloat()
        out[1] = (radius * sin(lat)).toFloat()
        out[2] = (radius * ring * cos(lon)).toFloat()
        return out
    }

    /** Applies `M = Rx(pitchRad) * Ry(yawRad)` to a model-space 3D vector. */
    fun rotateModelToWorld(
        mx: Float,
        my: Float,
        mz: Float,
        yawRad: Float,
        pitchRad: Float,
        out: FloatArray = FloatArray(3),
    ): FloatArray {
        val cy = cos(yawRad)
        val sy = sin(yawRad)
        val cp = cos(pitchRad)
        val sp = sin(pitchRad)
        // Ry(yaw)
        val x1 = mx * cy + mz * sy
        val y1 = my
        val z1 = -mx * sy + mz * cy
        // Rx(pitch)
        out[0] = x1
        out[1] = y1 * cp - z1 * sp
        out[2] = y1 * sp + z1 * cp
        return out
    }

    /** Applies `M^T = Ry(-yawRad) * Rx(-pitchRad)` to transform a world vector into model space. */
    fun rotateWorldToModel(
        wx: Float,
        wy: Float,
        wz: Float,
        yawRad: Float,
        pitchRad: Float,
        out: FloatArray = FloatArray(3),
    ): FloatArray {
        val cy = cos(yawRad)
        val sy = sin(yawRad)
        val cp = cos(pitchRad)
        val sp = sin(pitchRad)
        // Rx(-pitch)
        val y1 = wy * cp + wz * sp
        val z1 = -wy * sp + wz * cp
        // Ry(-yaw)
        out[0] = wx * cy - z1 * sy
        out[1] = y1
        out[2] = wx * sy + z1 * cy
        return out
    }

    /** Computes the world-space unit sun vector corresponding to [style] and camera orientation. */
    fun computeSunDirectionWorld(
        style: EarthStyle,
        yawRad: Float,
        pitchRad: Float,
        out: FloatArray = FloatArray(3),
    ): FloatArray {
        val subLat = style.subSolarLatitudeDegrees
        val subLon = style.subSolarLongitudeDegrees
        if (subLat != null && subLon != null) {
            val lat = Math.toRadians(subLat.toDouble())
            val lon = Math.toRadians(subLon.toDouble())
            val ring = cos(lat)
            val mx = (ring * sin(lon)).toFloat()
            val my = sin(lat).toFloat()
            val mz = (ring * cos(lon)).toFloat()
            return rotateModelToWorld(mx, my, mz, yawRad, pitchRad, out)
        }
        val az = Math.toRadians(style.sunAzimuthDegrees.toDouble()).toFloat()
        val el = Math.toRadians(style.sunElevationDegrees.toDouble()).toFloat()
        out[0] = cos(el) * cos(az)
        out[1] = sin(el)
        out[2] = cos(el) * sin(az)
        return out
    }

    /**
     * Computes the model-space unit sun vector passed to the GLES 2.0 shaders.
     * In astronomical mode ([EarthStyle.subSolarLatitudeDegrees] set), this stays directly pinned
     * to the geographic sub-solar point on Earth regardless of camera yaw/pitch; in standalone
     * mode, it rotates the camera-space sun into model space via `M^T`.
     */
    fun computeSunDirectionModel(
        style: EarthStyle,
        yawRad: Float,
        pitchRad: Float,
        out: FloatArray = FloatArray(3),
    ): FloatArray {
        val subLat = style.subSolarLatitudeDegrees
        val subLon = style.subSolarLongitudeDegrees
        if (subLat != null && subLon != null) {
            return latLonToModel(subLat.toDouble(), subLon.toDouble(), 1f, out)
        }
        val az = Math.toRadians(style.sunAzimuthDegrees.toDouble()).toFloat()
        val el = Math.toRadians(style.sunElevationDegrees.toDouble()).toFloat()
        val wx = cos(el) * cos(az)
        val wy = sin(el)
        val wz = cos(el) * sin(az)
        return rotateWorldToModel(wx, wy, wz, yawRad, pitchRad, out)
    }

    /**
     * Exact perspective ray-sphere occlusion test: returns true when the line segment from camera
     * `C = (0, 0, cameraDistance)` to world point `P = (wx, wy, wz)` intersects the Earth sphere
     * of radius [occluderRadius] centered at the origin.
     */
    fun isOccludedByEarth(
        wx: Float,
        wy: Float,
        wz: Float,
        cameraDistance: Float,
        occluderRadius: Float = 1f,
    ): Boolean {
        val dz = wz - cameraDistance
        val vv = wx * wx + wy * wy + dz * dz
        val rSq = occluderRadius * occluderRadius
        if (vv <= 1e-8f) return false
        val tClosest = -(cameraDistance * dz) / vv
        if (tClosest <= 0f) return false
        if (tClosest >= 1f) {
            // Surface points inside the perspective horizon have tClosest > 1 and |P| >= occluderRadius.
            val pSq = wx * wx + wy * wy + wz * wz
            return pSq < rSq - 1e-5f
        }
        val cx = tClosest * wx
        val cy = tClosest * wy
        val cz = cameraDistance + tClosest * dz
        val minDistSq = cx * cx + cy * cy + cz * cz
        return minDistSq <= rSq
    }

    /** Screen-space radius (in pixels) of Earth's perspective horizon disc. */
    fun projectedGlobeRadiusPx(cameraDistance: Float, viewportHeight: Float, focalScale: Float = 1f): Float {
        if (viewportHeight <= 0f) return 0f
        val d = cameraDistance.coerceAtLeast(1.05f)
        val ndcRadiusY = (1f / sqrt(d * d - 1f)) / TAN_HALF_FOV_Y
        return ndcRadiusY * viewportHeight * 0.5f * focalScale
    }

    /**
     * Projects a geographic coordinate `(latDeg, lonDeg)` at [radius] (in Earth radii; `1f` = surface)
     * into viewport pixel coordinates `(screenX, screenY)` and evaluates exact 3D Earth occlusion.
     */
    fun projectLatLon(
        latDeg: Double,
        lonDeg: Double,
        radius: Float,
        camera: EarthCameraSnapshot,
        viewportWidth: Float,
        viewportHeight: Float,
        occluderRadius: Float = 1f,
    ): EarthProjectedPoint {
        if (viewportWidth <= 0f || viewportHeight <= 0f) {
            return EarthProjectedPoint(0f, 0f, 0f, 0f, 0f, visible = false, limbFade = 0f)
        }
        val lat = Math.toRadians(latDeg)
        val lon = Math.toRadians(lonDeg)
        val ring = cos(lat)
        val mx = (radius * ring * sin(lon)).toFloat()
        val my = (radius * sin(lat)).toFloat()
        val mz = (radius * ring * cos(lon)).toFloat()

        val cy = cos(camera.yawRad)
        val sy = sin(camera.yawRad)
        val cp = cos(camera.pitchRad)
        val sp = sin(camera.pitchRad)
        val x1 = mx * cy + mz * sy
        val z1 = -mx * sy + mz * cy
        val wx = x1
        val wy = my.let { it * cp - z1 * sp }
        val wz = my.let { it * sp + z1 * cp }

        val distance = camera.distance
        val depth = distance - wz
        if (depth <= NEAR_PLANE) {
            return EarthProjectedPoint(0f, 0f, wx, wy, wz, visible = false, limbFade = 0f)
        }

        val aspect = viewportWidth / viewportHeight
        val ndcX = (wx / (aspect * TAN_HALF_FOV_Y)) / depth
        val ndcY = (wy / TAN_HALF_FOV_Y) / depth
        val sx = (ndcX * camera.projection.focalScale * 0.5f + 0.5f) * viewportWidth
        val syPx = (camera.projection.centerY - ndcY * camera.projection.focalScale * 0.5f) * viewportHeight

        val occluded = isOccludedByEarth(wx, wy, wz, distance, occluderRadius)
        if (occluded) {
            return EarthProjectedPoint(sx, syPx, wx, wy, wz, visible = false, limbFade = 0f)
        }

        // Smooth fade for surface markers right at the horizon; elevated orbital markers stay crisp
        // right until the Earth limb occludes them.
        val limbFade = if (radius <= 1.012f) {
            val horizonZ = occluderRadius / distance
            ((wz / radius - horizonZ) / 0.14f).coerceIn(0f, 1f)
        } else {
            1f
        }
        return EarthProjectedPoint(
            screenX = sx,
            screenY = syPx,
            worldX = wx,
            worldY = wy,
            worldZ = wz,
            visible = limbFade > 0.001f,
            limbFade = limbFade,
        )
    }
}

/**
 * Interactive camera controller for [Earth3D]. Keeps lock-free `@Volatile` coordinates for the GL
 * render thread alongside a Compose-observable [snapshot] so 3D overlays track globe rotation and
 * zoom frame-accurately without stalling the UI thread.
 */
@Stable
class EarthCameraController(
    initialYawRad: Float = Earth3DMath.DEFAULT_YAW_RAD,
    initialPitchRad: Float = Earth3DMath.DEFAULT_PITCH_RAD,
    initialDistance: Float = Earth3DMath.DEFAULT_DISTANCE,
) {
    @Volatile var yawRad: Float = Earth3DMath.normalizeAngleRad(initialYawRad)
        private set
    @Volatile var pitchRad: Float = initialPitchRad.coerceIn(Earth3DMath.MIN_PITCH_RAD, Earth3DMath.MAX_PITCH_RAD)
        private set
    @Volatile var distance: Float = initialDistance.coerceIn(Earth3DMath.MIN_ZOOM_DISTANCE, Earth3DMath.MAX_ZOOM_DISTANCE)
        private set
    @Volatile var userZoomed: Boolean = false
        private set

    private var projection = EarthViewportProjection()

    var snapshot: EarthCameraSnapshot by mutableStateOf(
        EarthCameraSnapshot(yawRad = yawRad, pitchRad = pitchRad, distance = distance, projection = projection)
    )
        private set

    internal var onInvalidateRender: (() -> Unit)? = null
    internal var onUserGestureStart: (() -> Unit)? = null

    private fun publishAndInvalidate() {
        snapshot = EarthCameraSnapshot(yawRad = yawRad, pitchRad = pitchRad, distance = distance, projection = projection)
        onInvalidateRender?.invoke()
    }

    /** Orbit drag matching Earth3D's familiar direct-manipulation globe controls. */
    fun drag(dx: Float, dy: Float, viewportWidth: Int, viewportHeight: Int) {
        val h = viewportHeight.coerceAtLeast(1)
        val angularScale = 2f * PI.toFloat() / h
        yawRad = Earth3DMath.normalizeAngleRad(yawRad + dx * angularScale)
        pitchRad = (pitchRad + dy * PI.toFloat() / h)
            .coerceIn(Earth3DMath.MIN_PITCH_RAD, Earth3DMath.MAX_PITCH_RAD)
        publishAndInvalidate()
    }

    /** Pinch / scroll zoom by multiplicative [factor] (`> 1` zooms in, `< 1` zooms out). */
    fun zoomBy(factor: Float) {
        if (factor <= 0f || !factor.isFinite()) return
        userZoomed = true
        distance = (distance / factor).coerceIn(Earth3DMath.MIN_ZOOM_DISTANCE, Earth3DMath.MAX_ZOOM_DISTANCE)
        publishAndInvalidate()
    }

    /** Double-tap toggle between default overview distance and close-up inspection distance. */
    fun toggleZoom(fitDistance: Float = Earth3DMath.DEFAULT_DISTANCE) {
        val clampedFit = fitDistance.coerceIn(2.25f, Earth3DMath.MAX_ZOOM_DISTANCE)
        distance = if (distance <= clampedFit * 0.78f) {
            clampedFit
        } else {
            max(Earth3DMath.MIN_ZOOM_DISTANCE, clampedFit * 0.68f)
        }
        userZoomed = true
        publishAndInvalidate()
    }

    /** Directly sets camera angles and optional distance (used by focus and smooth animations). */
    fun setCamera(newYawRad: Float, newPitchRad: Float, newDistance: Float = distance, markUserZoomed: Boolean = userZoomed) {
        yawRad = Earth3DMath.normalizeAngleRad(newYawRad)
        pitchRad = newPitchRad.coerceIn(Earth3DMath.MIN_PITCH_RAD, Earth3DMath.MAX_PITCH_RAD)
        distance = newDistance.coerceIn(Earth3DMath.MIN_ZOOM_DISTANCE, Earth3DMath.MAX_ZOOM_DISTANCE)
        userZoomed = markUserZoomed
        publishAndInvalidate()
    }

    /** Centers the camera on geographic `(latDeg, lonDeg)` immediately. */
    fun focusOn(latDeg: Double, lonDeg: Double, targetDistance: Float = distance) {
        val (targetYaw, targetPitch) = Earth3DMath.cameraAnglesForLatLon(latDeg, lonDeg)
        setCamera(targetYaw, targetPitch, targetDistance, markUserZoomed = userZoomed)
    }

    /** Resets orientation and zoom to `(latDeg, lonDeg)` at [fitDistance]. */
    fun resetTo(latDeg: Double, lonDeg: Double, fitDistance: Float = Earth3DMath.DEFAULT_DISTANCE) {
        val (targetYaw, targetPitch) = Earth3DMath.cameraAnglesForLatLon(latDeg, lonDeg)
        setCamera(targetYaw, targetPitch, fitDistance.coerceIn(2.25f, Earth3DMath.MAX_ZOOM_DISTANCE), markUserZoomed = false)
    }

    internal fun stepAutoRotation(dtSec: Float, speedRadPerSec: Float) {
        if (dtSec <= 0f || speedRadPerSec == 0f) return
        yawRad = Earth3DMath.normalizeAngleRad(yawRad + speedRadPerSec * dtSec)
        publishAndInvalidate()
    }

    internal fun syncViewportProjection(next: EarthViewportProjection) {
        if (projection != next) {
            projection = next
            publishAndInvalidate()
        }
    }

    internal fun syncStyleCameraDistance(previousDistance: Float, newDistance: Float) {
        if (!userZoomed || newDistance != previousDistance) {
            distance = newDistance.coerceIn(2.25f, Earth3DMath.MAX_ZOOM_DISTANCE)
            snapshot = EarthCameraSnapshot(yawRad = yawRad, pitchRad = pitchRad, distance = distance, projection = projection)
        }
    }
}

@Composable
fun rememberEarthCameraController(
    initialLatDeg: Double? = null,
    initialLonDeg: Double? = null,
    initialDistance: Float = Earth3DMath.DEFAULT_DISTANCE,
): EarthCameraController = remember {
    if (initialLatDeg != null && initialLonDeg != null) {
        val (yaw, pitch) = Earth3DMath.cameraAnglesForLatLon(initialLatDeg, initialLonDeg)
        EarthCameraController(initialYawRad = yaw, initialPitchRad = pitch, initialDistance = initialDistance)
    } else {
        EarthCameraController(initialDistance = initialDistance)
    }
}

/**
 * Embeds the native GLES 2.0 3D Earth renderer in a Jetpack Compose layout.
 *
 * Supports both standalone usage (`Earth3D(Modifier.fillMaxSize(), EarthStyle(...))`) and
 * synchronized interactive usage with [controller] and [onTap] for satellite tracking overlays.
 */
@Composable
fun Earth3D(
    modifier: Modifier = Modifier,
    style: EarthStyle = EarthStyle(),
    controller: EarthCameraController = remember { EarthCameraController(initialDistance = style.cameraDistance) },
    onTap: ((Offset) -> Boolean)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceRef = remember { mutableStateOf<EarthSurface?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> surfaceRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> surfaceRef.value?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surfaceRef.value?.release()
            surfaceRef.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            EarthSurface(context, controller).apply {
                tapHandler = { offset -> currentOnTap?.invoke(offset) == true }
                setStyle(style)
                surfaceRef.value = this
            }
        },
        update = { view ->
            view.setController(controller)
            view.tapHandler = { offset -> currentOnTap?.invoke(offset) == true }
            view.setStyle(style)
        },
        onRelease = { view ->
            view.release()
            if (surfaceRef.value === view) surfaceRef.value = null
        },
    )
}

/**
 * In-window TextureView host for [EarthGlThread] and [EarthScene]. Avoids SurfaceView Z-order
 * punch-through so Compose overlays, squircle clipping, tab crossfades, and vertical scroll work
 * natively alongside hardware-accelerated GLES 2.0 rendering.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
private class EarthSurface(
    context: Context,
    private var controller: EarthCameraController,
) : TextureView(context), TextureView.SurfaceTextureListener, Choreographer.FrameCallback {

    private val scene = EarthScene(context.applicationContext)
    private var glThread: EarthGlThread? = null
    private var currentStyle: EarthStyle = EarthStyle()
    private var released = false
    private var paused = false
    private var autoLoopPosted = false
    private var lastChoreographerNs = 0L

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX = 0f
    private var downY = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var isDraggingGlobe = false
    private var capturedGesture = false
    private var lastTapConsumedByOverlay = false
    private var overlayDoubleTap = false

    var tapHandler: ((Offset) -> Boolean)? = null

    private val pinchDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                controller.onUserGestureStart?.invoke()
                parent?.requestDisallowInterceptTouchEvent(true)
                capturedGesture = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                controller.zoomBy(detector.scaleFactor)
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onSingleTapUp(event: MotionEvent): Boolean {
                val consumed = tapHandler?.invoke(Offset(event.x, event.y)) == true
                lastTapConsumedByOverlay = consumed
                performClick()
                return true
            }

            override fun onDoubleTap(event: MotionEvent): Boolean {
                overlayDoubleTap = lastTapConsumedByOverlay
                if (!overlayDoubleTap) {
                    controller.onUserGestureStart?.invoke()
                    controller.toggleZoom(currentStyle.cameraDistance)
                }
                lastTapConsumedByOverlay = false
                return true
            }

            override fun onDoubleTapEvent(event: MotionEvent): Boolean {
                // A double tap on empty space still zooms. A second marker tap cycles overlapping hits.
                if (overlayDoubleTap && event.actionMasked == MotionEvent.ACTION_UP) {
                    tapHandler?.invoke(Offset(event.x, event.y))
                    overlayDoubleTap = false
                }
                return true
            }
        }
    )

    init {
        isOpaque = false
        isFocusable = true
        isFocusableInTouchMode = true
        surfaceTextureListener = this
        controller.onInvalidateRender = ::requestRenderFrame
    }

    fun setController(next: EarthCameraController) {
        if (controller === next) return
        if (controller.onInvalidateRender != null) controller.onInvalidateRender = null
        controller = next
        controller.onInvalidateRender = ::requestRenderFrame
        requestRenderFrame()
    }

    fun setStyle(style: EarthStyle) {
        val sanitized = style.sanitized()
        val prevDistance = currentStyle.cameraDistance
        val changed = sanitized != currentStyle
        currentStyle = sanitized
        controller.syncStyleCameraDistance(prevDistance, sanitized.cameraDistance)
        scene.style = sanitized
        controller.syncViewportProjection(sanitized.viewportProjection)
        updateAutoRotateLoop()
        if (changed) requestRenderFrame()
    }

    fun onResume() {
        if (released) return
        paused = false
        glThread?.setPaused(false)
        updateAutoRotateLoop()
        requestRenderFrame()
    }

    fun onPause() {
        paused = true
        stopAutoRotateLoop()
        glThread?.setPaused(true)
    }

    fun release() {
        if (released) return
        released = true
        stopAutoRotateLoop()
        controller.onInvalidateRender = null
        glThread?.detachAndStop()
        glThread = null
    }

    private fun shouldAutoRotate(): Boolean =
        !released && !paused && glThread != null && currentStyle.autoRotate && currentStyle.rotationSpeed != 0f

    private fun updateAutoRotateLoop() {
        if (shouldAutoRotate()) {
            if (!autoLoopPosted) {
                autoLoopPosted = true
                lastChoreographerNs = 0L
                Choreographer.getInstance().postFrameCallback(this)
            }
        } else {
            stopAutoRotateLoop()
        }
    }

    private fun stopAutoRotateLoop() {
        if (autoLoopPosted) {
            autoLoopPosted = false
            lastChoreographerNs = 0L
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        autoLoopPosted = false
        if (!shouldAutoRotate()) return
        val dt = if (lastChoreographerNs == 0L) {
            0f
        } else {
            ((frameTimeNanos - lastChoreographerNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        }
        lastChoreographerNs = frameTimeNanos
        if (dt > 0f) {
            controller.stepAutoRotation(dt, currentStyle.rotationSpeed)
        } else {
            requestRenderFrame()
        }
        autoLoopPosted = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun requestRenderFrame() {
        if (released || paused) return
        glThread?.requestDraw(controller.yawRad, controller.pitchRad, controller.distance)
    }

    private fun isTouchInsideInteractiveGlobe(x: Float, y: Float): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return true
        if (!currentStyle.transparentBackground || currentStyle.fullViewportGestures) return true
        val globeRadius = Earth3DMath.projectedGlobeRadiusPx(controller.distance, h)
        val distFromCenter = hypot(x - w * 0.5f, y - h * 0.5f)
        // Include full globe plus LEO orbital halo so dragging anywhere on/around Earth orbits smoothly,
        // while wide empty side margins still allow effortless vertical page scrolling.
        return distFromCenter <= globeRadius * 1.24f
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        pinchDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                previousX = event.x
                previousY = event.y
                isDraggingGlobe = false
                capturedGesture = isTouchInsideInteractiveGlobe(event.x, event.y)
                if (capturedGesture) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                capturedGesture = true
                controller.onUserGestureStart?.invoke()
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !pinchDetector.isInProgress) {
                    val totalDx = event.x - downX
                    val totalDy = event.y - downY
                    if (!isDraggingGlobe && hypot(totalDx, totalDy) > touchSlop) {
                        if (!capturedGesture && abs(totalDy) > abs(totalDx) * 1.15f) {
                            // Vertical swipe started outside the globe disc: hand off to page scroll.
                            parent?.requestDisallowInterceptTouchEvent(false)
                            return false
                        }
                        capturedGesture = true
                        isDraggingGlobe = true
                        controller.onUserGestureStart?.invoke()
                        parent?.requestDisallowInterceptTouchEvent(true)
                        previousX = event.x
                        previousY = event.y
                    } else if (isDraggingGlobe) {
                        val dx = event.x - previousX
                        val dy = event.y - previousY
                        controller.drag(dx, dy, width.coerceAtLeast(1), height.coerceAtLeast(1))
                        previousX = event.x
                        previousY = event.y
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                val replacement = if (index == 0) 1 else 0
                if (replacement < event.pointerCount) {
                    previousX = event.getX(replacement)
                    previousY = event.getY(replacement)
                    downX = previousX
                    downY = previousY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingGlobe = false
                capturedGesture = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_SCROLL -> {
                val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                if (scroll != 0f) {
                    controller.onUserGestureStart?.invoke()
                    controller.zoomBy(exp(scroll * 0.12f))
                    return true
                }
            }
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (scroll != 0f) {
                controller.onUserGestureStart?.invoke()
                controller.zoomBy(exp(scroll * 0.12f))
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (released) {
            surface.release()
            return
        }
        val worker = EarthGlThread(scene)
        glThread = worker
        worker.attach(
            surface = surface,
            width = width,
            height = height,
            initiallyPaused = paused,
            initialYaw = controller.yawRad,
            initialPitch = controller.pitchRad,
            initialZoom = controller.distance,
        )
        updateAutoRotateLoop()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        glThread?.setViewport(width, height, controller.yawRad, controller.pitchRad, controller.distance)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopAutoRotateLoop()
        glThread?.detachAndStop()
        glThread = null
        // SurfaceTexture is released on the GL thread after destroying the EGL window surface.
        return false
    }
}

/**
 * Dedicated EGL14 + GLES 2.0 render thread with coalesced frame scheduling.
 */
private class EarthGlThread(private val scene: EarthScene) {
    private val thread = HandlerThread("Earth3DGl").apply { start() }
    private val handler = Handler(thread.looper)
    private val drawPending = AtomicBoolean(false)

    @Volatile private var pendingYaw = Earth3DMath.DEFAULT_YAW_RAD
    @Volatile private var pendingPitch = Earth3DMath.DEFAULT_PITCH_RAD
    @Volatile private var pendingZoom = Earth3DMath.DEFAULT_DISTANCE

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglConfig: EGLConfig? = null
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var surfaceTexture: SurfaceTexture? = null
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var paused = false
    private var failed = false

    fun attach(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
        initiallyPaused: Boolean,
        initialYaw: Float,
        initialPitch: Float,
        initialZoom: Float,
    ) = handler.post {
        surfaceTexture = surface
        paused = initiallyPaused
        pendingYaw = initialYaw
        pendingPitch = initialPitch
        pendingZoom = initialZoom
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        try {
            surface.setDefaultBufferSize(viewportWidth, viewportHeight)
            initEgl(surface)
            scene.onSurfaceCreated()
            scene.onSurfaceChanged(viewportWidth, viewportHeight)
            drawFrame()
        } catch (t: Throwable) {
            Log.w(TAG, "Earth3D GL initialization failed", t)
            failed = true
            releaseSceneQuietly()
            releaseEgl()
        }
    }

    fun setViewport(width: Int, height: Int, yaw: Float, pitch: Float, zoom: Float) = handler.post {
        if (failed || surfaceTexture == null) return@post
        pendingYaw = yaw
        pendingPitch = pitch
        pendingZoom = zoom
        val nextW = width.coerceAtLeast(1)
        val nextH = height.coerceAtLeast(1)
        if (nextW == viewportWidth && nextH == viewportHeight) {
            drawFrame()
            return@post
        }
        try {
            val surface = surfaceTexture ?: return@post
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            viewportWidth = nextW
            viewportHeight = nextH
            surface.setDefaultBufferSize(viewportWidth, viewportHeight)
            createWindowSurface(surface)
            scene.onSurfaceChanged(viewportWidth, viewportHeight)
            drawFrame()
        } catch (t: Throwable) {
            Log.w(TAG, "Earth3D GL viewport resize failed", t)
            failed = true
            releaseSceneQuietly()
            releaseEgl()
        }
    }

    fun setPaused(value: Boolean) = handler.post {
        paused = value
        if (!paused) drawFrame()
    }

    fun requestDraw(yaw: Float, pitch: Float, zoom: Float) {
        pendingYaw = yaw
        pendingPitch = pitch
        pendingZoom = zoom
        if (drawPending.compareAndSet(false, true)) {
            handler.post {
                drawPending.set(false)
                drawFrame()
            }
        }
    }

    fun detachAndStop() {
        handler.post {
            releaseSceneQuietly()
            releaseEgl()
            thread.quitSafely()
        }
    }

    private fun drawFrame() {
        if (failed || paused || eglSurface == EGL14.EGL_NO_SURFACE) return
        if (viewportWidth <= 0 || viewportHeight <= 0) return
        try {
            scene.draw(pendingYaw, pendingPitch, pendingZoom)
            if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                Log.w(TAG, "Earth3D eglSwapBuffers failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Earth3D GL draw failed", t)
            failed = true
            releaseSceneQuietly()
            releaseEgl()
        }
    }

    private fun initEgl(surface: SurfaceTexture) {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
        eglDisplay = display

        // Prefer 4x MSAA for a crisp sphere silhouette; fall back gracefully when unavailable.
        val msaaAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_SAMPLE_BUFFERS, 1,
            EGL14.EGL_SAMPLES, 4,
            EGL14.EGL_NONE,
        )
        val baseAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        val chosen = (EGL14.eglChooseConfig(display, msaaAttribs, 0, configs, 0, 1, numConfigs, 0) &&
            numConfigs[0] > 0 && configs[0] != null) ||
            (EGL14.eglChooseConfig(display, baseAttribs, 0, configs, 0, 1, numConfigs, 0) &&
                numConfigs[0] > 0 && configs[0] != null)
        check(chosen) { "No GLES 2.0 EGLConfig available" }
        val config = configs[0]!!
        eglConfig = config

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        check(context != EGL14.EGL_NO_CONTEXT) {
            "eglCreateContext failed (0x${Integer.toHexString(EGL14.eglGetError())})"
        }
        eglContext = context
        createWindowSurface(surface)
    }

    private fun createWindowSurface(surface: SurfaceTexture) {
        val config = checkNotNull(eglConfig) { "Missing EGLConfig" }
        val winSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(winSurface != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }
        eglSurface = winSurface
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }
    }

    private fun releaseSceneQuietly() {
        if (eglContext == EGL14.EGL_NO_CONTEXT) return
        try {
            scene.release()
        } catch (t: Throwable) {
            Log.w(TAG, "Earth3D GL resource release failed", t)
        }
    }

    private fun releaseEgl() {
        val display = eglDisplay
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
        }
        surfaceTexture?.release()
        surfaceTexture = null
        eglSurface = EGL14.EGL_NO_SURFACE
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }

    private companion object {
        const val TAG = "Earth3D"
    }
}

/**
 * Native GLES 2.0 Earth shader & sphere mesh renderer (lit Blue Marble surface with topographic
 * bump mapping, smooth solar terminator, night-side city lights, and translucent sunlit cloud layer).
 */
private class EarthScene(private val context: Context) {
    @Volatile var style: EarthStyle = EarthStyle()

    private var surfaceProgram = 0
    private var cloudProgram = 0
    private var mesh: SphereMesh? = null
    private var dayTexture = 0
    private var nightTexture = 0
    private var cloudTexture = 0
    private var bumpTexture = 0
    private var maxTextureSize = 4096
    private var viewportWidth = 1
    private var viewportHeight = 1

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val temp = FloatArray(16)
    private val sun = FloatArray(3)

    fun onSurfaceCreated() {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        // Premultiplied-safe alpha blending so cloud wisps over transparent edges have no dark fringe.
        GLES20.glBlendFuncSeparate(
            GLES20.GL_SRC_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA,
            GLES20.GL_ONE,
            GLES20.GL_ONE_MINUS_SRC_ALPHA,
        )
        val maxTex = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTex, 0)
        if (maxTex[0] >= 512) maxTextureSize = maxTex[0]

        mesh = createSphere(128, 96)
        surfaceProgram = linkProgram(VERTEX_SHADER, EARTH_FRAGMENT_SHADER)
        cloudProgram = linkProgram(VERTEX_SHADER, CLOUD_FRAGMENT_SHADER)
        dayTexture = loadTexture("earth_day_4k.jpg")
        bumpTexture = loadTexture("earth_bump_2k.jpg", fullColor = true)
        nightTexture = loadTexture("earth_night_2k.jpg")
        cloudTexture = loadTexture("earth_clouds.jpg")
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        Matrix.perspectiveM(
            projection,
            0,
            Earth3DMath.FOV_Y_DEGREES,
            viewportWidth.toFloat() / viewportHeight,
            Earth3DMath.NEAR_PLANE,
            Earth3DMath.FAR_PLANE,
        )
    }

    fun draw(yawRad: Float, pitchRad: Float, distance: Float) {
        val options = style
        val framing = options.viewportProjection
        val focal = framing.focalScale / tan(Math.toRadians(Earth3DMath.FOV_Y_DEGREES / 2.0)).toFloat()
        projection[0] = focal * viewportHeight / viewportWidth
        projection[5] = focal
        projection[9] = framing.centerY * 2f - 1f
        if (options.transparentBackground) {
            GLES20.glClearColor(0f, 0f, 0f, 0f)
        } else {
            GLES20.glClearColor(0.008f, 0.018f, 0.045f, 1f)
        }

        Matrix.setLookAtM(view, 0, 0f, 0f, distance, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(model, 0)
        // Coordinate convention: +Y is north, +Z faces the camera, +X is screen-right.
        // Pitch first, then yaw => model = Rx(pitch) * Ry(yaw). This keeps the
        // north/south axis in the vertical screen plane instead of rolling diagonally.
        Matrix.rotateM(model, 0, Math.toDegrees(pitchRad.toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.rotateM(model, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)

        Earth3DMath.computeSunDirectionModel(options, yawRad, pitchRad, sun)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        // Lit surface followed by the translucent cloud shell. No atmosphere shell/halo.
        drawEarth(options)
        if (options.showClouds && options.cloudOpacity > 0f) drawClouds(options)
    }

    fun release() {
        val textures = intArrayOf(dayTexture, nightTexture, cloudTexture, bumpTexture)
        GLES20.glDeleteTextures(textures.size, textures, 0)
        dayTexture = 0
        nightTexture = 0
        cloudTexture = 0
        bumpTexture = 0
        if (surfaceProgram != 0) {
            GLES20.glDeleteProgram(surfaceProgram)
            surfaceProgram = 0
        }
        if (cloudProgram != 0) {
            GLES20.glDeleteProgram(cloudProgram)
            cloudProgram = 0
        }
        mesh = null
    }

    private fun drawEarth(options: EarthStyle) {
        GLES20.glUseProgram(surfaceProgram)
        bindMatrices(surfaceProgram, 1f)
        bindTexture(surfaceProgram, "uDayMap", dayTexture, 0)
        bindTexture(surfaceProgram, "uNightMap", nightTexture, 1)
        bindTexture(surfaceProgram, "uBumpMap", bumpTexture, 2)
        uniform2(surfaceProgram, "uBumpTexel", 1f / 2048f, 1f / 1024f)
        uniform1f(surfaceProgram, "uBumpStrength", 1.25f)
        uniform3(surfaceProgram, "uSunDirection", sun[0], sun[1], sun[2])
        uniform1f(surfaceProgram, "uAmbient", options.ambientLight)
        uniform1f(surfaceProgram, "uNightEnabled", if (options.showNightLights) 1f else 0f)
        uniform1f(surfaceProgram, "uExposure", options.exposureEV)
        uniform1f(surfaceProgram, "uSaturation", options.saturation)
        drawMesh(surfaceProgram)
    }

    private fun drawClouds(options: EarthStyle) {
        GLES20.glUseProgram(cloudProgram)
        bindMatrices(cloudProgram, Earth3DMath.CLOUD_SHELL_SCALE)
        bindTexture(cloudProgram, "uCloudMap", cloudTexture, 0)
        uniform1f(cloudProgram, "uOpacity", options.cloudOpacity)
        uniform3(cloudProgram, "uSunDirection", sun[0], sun[1], sun[2])
        // Use depth testing but do not write depth so the lit surface remains visible through gaps.
        GLES20.glDepthMask(false)
        drawMesh(cloudProgram)
        GLES20.glDepthMask(true)
    }

    private fun bindMatrices(program: Int, scale: Float) {
        Matrix.setIdentityM(temp, 0)
        Matrix.scaleM(temp, 0, scale, scale, scale)
        Matrix.multiplyMM(mvp, 0, model, 0, temp, 0)
        Matrix.multiplyMM(temp, 0, view, 0, mvp, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)
        val loc = GLES20.glGetUniformLocation(program, "uMvp")
        if (loc >= 0) GLES20.glUniformMatrix4fv(loc, 1, false, mvp, 0)
    }

    private fun drawMesh(program: Int) {
        val sphere = mesh ?: return
        sphere.vertices.position(0)
        val pos = GLES20.glGetAttribLocation(program, "aPosition")
        val normal = GLES20.glGetAttribLocation(program, "aNormal")
        val uv = GLES20.glGetAttribLocation(program, "aUv")
        val stride = 8 * 4
        if (pos >= 0) {
            GLES20.glEnableVertexAttribArray(pos)
            GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, stride, sphere.vertices)
        }
        if (normal >= 0) {
            sphere.vertices.position(3)
            GLES20.glEnableVertexAttribArray(normal)
            GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, stride, sphere.vertices)
        }
        if (uv >= 0) {
            sphere.vertices.position(6)
            GLES20.glEnableVertexAttribArray(uv)
            GLES20.glVertexAttribPointer(uv, 2, GLES20.GL_FLOAT, false, stride, sphere.vertices)
        }
        sphere.indices.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphere.indexCount, GLES20.GL_UNSIGNED_SHORT, sphere.indices)
        if (pos >= 0) GLES20.glDisableVertexAttribArray(pos)
        if (normal >= 0) GLES20.glDisableVertexAttribArray(normal)
        if (uv >= 0) GLES20.glDisableVertexAttribArray(uv)
    }

    private fun bindTexture(program: Int, name: String, texture: Int, unit: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        val loc = GLES20.glGetUniformLocation(program, name)
        if (loc >= 0) GLES20.glUniform1i(loc, unit)
    }

    private fun uniform1f(program: Int, name: String, value: Float) {
        val l = GLES20.glGetUniformLocation(program, name)
        if (l >= 0) GLES20.glUniform1f(l, value)
    }

    private fun uniform2(program: Int, name: String, x: Float, y: Float) {
        val l = GLES20.glGetUniformLocation(program, name)
        if (l >= 0) GLES20.glUniform2f(l, x, y)
    }

    private fun uniform3(program: Int, name: String, x: Float, y: Float, z: Float) {
        val l = GLES20.glGetUniformLocation(program, name)
        if (l >= 0) GLES20.glUniform3f(l, x, y, z)
    }

    private fun loadTexture(file: String, fullColor: Boolean = false): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(file).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxTextureSize || bounds.outHeight / sampleSize > maxTextureSize) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            // RGB_565 halves the GPU footprint for color maps; keep 8-bit precision for height gradients.
            inPreferredConfig = if (fullColor) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            inDither = true
            inSampleSize = sampleSize
        }
        val bitmap = checkNotNull(
            context.assets.open(file).use { BitmapFactory.decodeStream(it, null, options) }
        ) { "Failed to decode Earth3D asset: $file" }
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bitmap.recycle()
        return ids[0]
    }

    private fun linkProgram(vertex: String, fragment: String): Int {
        fun compile(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("Earth3D shader compile failed: $log")
            }
            return shader
        }
        val vs = compile(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            error("Earth3D shader link failed: $log")
        }
        return program
    }

    private data class SphereMesh(val vertices: FloatBuffer, val indices: ShortBuffer, val indexCount: Int)

    private fun createSphere(longitudeSegments: Int, latitudeSegments: Int): SphereMesh {
        val vertexCount = (longitudeSegments + 1) * (latitudeSegments + 1)
        val data = FloatArray(vertexCount * 8)
        var p = 0
        // Equirectangular convention: u=0 is 180°W, u=.5 is Greenwich,
        // u=1 is 180°E; v=0 is the north/top edge and v=1 the south/bottom edge.
        for (y in 0..latitudeSegments) {
            val v = y.toFloat() / latitudeSegments
            val latitude = (0.5f - v) * PI.toFloat()
            val ring = cos(latitude)
            val sy = sin(latitude)
            for (x in 0..longitudeSegments) {
                val u = x.toFloat() / longitudeSegments
                val longitude = (u - 0.5f) * 2f * PI.toFloat()
                val sx = ring * sin(longitude)
                val sz = ring * cos(longitude)
                data[p++] = sx; data[p++] = sy; data[p++] = sz
                data[p++] = sx; data[p++] = sy; data[p++] = sz
                data[p++] = u; data[p++] = v
            }
        }
        val idx = ShortArray(longitudeSegments * latitudeSegments * 6)
        var q = 0
        for (y in 0 until latitudeSegments) {
            for (x in 0 until longitudeSegments) {
                val a = y * (longitudeSegments + 1) + x
                val b = a + longitudeSegments + 1
                idx[q++] = a.toShort(); idx[q++] = b.toShort(); idx[q++] = (a + 1).toShort()
                idx[q++] = (a + 1).toShort(); idx[q++] = b.toShort(); idx[q++] = (b + 1).toShort()
            }
        }
        val vb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(data).also { it.position(0) }
        val ib = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            .put(idx).also { it.position(0) }
        return SphereMesh(vb, ib, idx.size)
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aUv;
            varying vec2 vUv;
            varying vec3 vNormal;
            void main() {
                vUv = aUv;
                vNormal = aNormal;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """

        private const val EARTH_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uDayMap;
            uniform sampler2D uNightMap;
            uniform sampler2D uBumpMap;
            uniform vec2 uBumpTexel;
            uniform float uBumpStrength;
            uniform vec3 uSunDirection;
            uniform float uAmbient;
            uniform float uNightEnabled;
            uniform float uExposure;
            uniform float uSaturation;
            varying vec2 vUv;
            varying vec3 vNormal;
            void main() {
                vec3 n = normalize(vNormal);
                float height = texture2D(uBumpMap, vUv).r;
                float slopeU = texture2D(uBumpMap, vUv + vec2(uBumpTexel.x, 0.0)).r - height;
                float slopeV = texture2D(uBumpMap, vUv + vec2(0.0, uBumpTexel.y)).r - height;
                vec3 tangent = vec3(n.z, 0.0, -n.x);
                if (length(tangent) < 0.001) tangent = vec3(1.0, 0.0, 0.0);
                tangent = normalize(tangent);
                vec3 bitangent = normalize(cross(tangent, n));
                n = normalize(n - tangent * slopeU * uBumpStrength - bitangent * slopeV * uBumpStrength);
                float ndl = dot(n, normalize(uSunDirection));
                float day = smoothstep(-0.16, 0.20, ndl);
                vec3 surface = texture2D(uDayMap, vUv).rgb;
                vec3 city = texture2D(uNightMap, vUv).rgb;
                vec3 lit = surface * (uAmbient + (1.0 - uAmbient) * max(ndl, 0.0) * 1.10);
                lit = mix(lit, surface * (uAmbient + 0.94 * max(ndl, 0.0)), day);
                lit += city * (1.0 - day) * uNightEnabled * 1.30;
                float luminance = dot(lit, vec3(0.2126, 0.7152, 0.0722));
                lit = mix(vec3(luminance), lit, uSaturation);
                lit *= exp2(uExposure);
                gl_FragColor = vec4(clamp(lit, 0.0, 1.0), 1.0);
            }
        """

        private const val CLOUD_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uCloudMap;
            uniform vec3 uSunDirection;
            uniform float uOpacity;
            varying vec2 vUv;
            varying vec3 vNormal;
            void main() {
                vec3 tex = texture2D(uCloudMap, vUv).rgb;
                float amount = smoothstep(0.26, 0.78, dot(tex, vec3(0.299, 0.587, 0.114)));
                float light = 0.32 + 0.68 * max(dot(normalize(vNormal), normalize(uSunDirection)), 0.0);
                float alpha = amount * uOpacity;
                gl_FragColor = vec4(clamp(vec3(0.78, 0.88, 1.0) * light, 0.0, 1.0), alpha);
            }
        """
    }
}
