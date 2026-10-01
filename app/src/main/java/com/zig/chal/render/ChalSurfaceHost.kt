package com.zig.chal.render

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import com.orchestrsim.blackhole.NativeBridge
import com.zig.chal.config.ChalRendererBackend
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.config.XapkCameraState

/** Why the native backend is not in use, so the UI can explain it in the user's language. */
enum class ChalFallbackReason {
    /** The native renderer is active. */
    NONE,

    /** Runtime Vulkan version reporting needs Android 10+. */
    API_TOO_OLD,

    /** The extracted renderer is arm64-v8a only. */
    NOT_ARM64,

    /** The device does not report Vulkan 1.1. */
    NO_VULKAN,

    /** The device qualifies, but the library could not be extracted/loaded (retryable). */
    LIBRARY_LOAD_FAILED
}

/**
 * Backend-selecting SurfaceView host for Chal in Lab. Prefers the original XAPK Vulkan renderer when
 * the ABI/API/device requirements are met; otherwise it keeps the Chal GLES renderer alive and reports
 * a structured fallback reason for the UI to explain.
 */
class ChalSurfaceHost(
    context: Context,
    initialParams: ChalSimulationParams
) : FrameLayout(context) {
    private var glSurfaceView: ChalSurfaceView? = null
    private var nativeRendererRef: XapkChalRenderer? = null
    private var nativeSurface: XapkNativeSurfaceView? = null

    /** Backend currently presenting frames. Replaced in place when a failed native load is retried. */
    var renderer: ChalRendererBackend
        private set

    private var fallbackReasonInternal: ChalFallbackReason = ChalFallbackReason.NONE
    private var fallbackDetailInternal: String? = null

    /** Hard failure (shader/program/surface error); the UI treats this as an error, not a notice. */
    val errorMessage: String?
        get() = renderer.errorMessage

    val isUsingXapkRenderer: Boolean
        get() = nativeRendererRef != null

    /** Why the GLES fallback is active, or [ChalFallbackReason.NONE] when the native path is live. */
    val fallbackReason: ChalFallbackReason
        get() = if (nativeRendererRef == null) fallbackReasonInternal else ChalFallbackReason.NONE

    /** Extra detail for [ChalFallbackReason.LIBRARY_LOAD_FAILED] (loader message). */
    val fallbackDetail: String?
        get() = fallbackDetailInternal

    /** True when retrying the native load has a chance of succeeding. */
    val isNativeLoadRetryable: Boolean
        get() = nativeRendererRef == null && fallbackReasonInternal == ChalFallbackReason.LIBRARY_LOAD_FAILED

    var onTap: (() -> Unit)? = null
        set(value) {
            field = value
            // Both backends toggle the overlay chrome on a scene tap, so the gesture is consistent.
            glSurfaceView?.onTap = value
            nativeSurface?.onTap = value
        }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        val gate = XapkNativeSupport.evaluate(context)
        val native = if (gate.first == ChalFallbackReason.NONE) XapkChalRenderer(initialParams) else null
        if (native != null) {
            val surface = XapkNativeSurfaceView(context, native)
            nativeRendererRef = native
            nativeSurface = surface
            renderer = native
            addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        } else {
            val glSurface = ChalSurfaceView(context)
            glSurface.renderer.updateParams(initialParams)
            glSurfaceView = glSurface
            nativeRendererRef = null
            fallbackReasonInternal = gate.first
            fallbackDetailInternal = gate.second
            renderer = glSurface.renderer
            addView(glSurface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        renderer.setDisplayRefreshRate(displayRefreshRateHz())
    }

    /**
     * Retry extracting/loading the native renderer after a transient failure.
     *
     * On success the GLES surface is torn down and replaced by the native one in place, so the rest of
     * the screen (and the persisted session) does not need to be rebuilt.
     */
    fun retryNativeRenderer(initialParams: ChalSimulationParams): Boolean {
        if (nativeRendererRef != null) return true
        if (fallbackReasonInternal != ChalFallbackReason.LIBRARY_LOAD_FAILED) return false

        NativeBridge.resetForRetry()
        val gate = XapkNativeSupport.evaluate(context)
        if (gate.first != ChalFallbackReason.NONE) {
            fallbackReasonInternal = gate.first
            fallbackDetailInternal = gate.second
            return false
        }

        val native = XapkChalRenderer(initialParams)
        val surface = XapkNativeSurfaceView(context, native)
        surface.onTap = onTap

        glSurfaceView?.onPause()
        glSurfaceView = null
        removeAllViews()
        nativeSurface = surface
        nativeRendererRef = native
        renderer = native
        fallbackReasonInternal = ChalFallbackReason.NONE
        fallbackDetailInternal = null
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        renderer.setDisplayRefreshRate(displayRefreshRateHz())
        native.onResume()
        return true
    }

    private fun displayRefreshRateHz(): Double = try {
        val rate = display?.refreshRate?.toDouble() ?: 60.0
        if (rate.isFinite() && rate > 0.0) rate else 60.0
    } catch (_: Throwable) {
        60.0
    }

    fun setDisplayRefreshRate(refreshRateHz: Double) = renderer.setDisplayRefreshRate(refreshRateHz)

    fun onResume() {
        nativeRendererRef?.onResume() ?: glSurfaceView?.onResume()
    }

    fun onPause() {
        nativeRendererRef?.onPause() ?: glSurfaceView?.onPause()
    }

    fun release() {
        glSurfaceView?.releaseGl()
        glSurfaceView = null
        nativeSurface = null
        // The native SurfaceHolder callback owns nativeOnSurfaceDestroyed(), just as in the XAPK.
    }

    fun resetCamera() {
        nativeRendererRef?.resetCamera() ?: glSurfaceView?.resetCamera()
    }

    fun resetScenarioPitch() {
        nativeRendererRef?.resetPitchForScenario()
    }

    /** Live camera framing, for session persistence. */
    fun captureCamera(): XapkCameraState {
        val native = nativeRendererRef
        if (native != null) return native.cameraState()
        val gl = glSurfaceView?.renderer ?: return renderer.params.cameraState()
        val state = gl.cameraSnapshot()
        val params = gl.params
        return XapkCameraState(
            yaw = (state.theta / (2.0 * Math.PI)).toFloat().let { if (it.isFinite()) it.mod(1.0f) else XapkCameraState.DEFAULT_YAW },
            pitch = (state.phi / Math.PI).toFloat().coerceIn(XapkCameraState.MIN_PITCH, XapkCameraState.MAX_PITCH),
            distance = params.zoom.toFloat().coerceIn(XapkCameraState.MIN_DISTANCE, XapkCameraState.MAX_DISTANCE)
        )
    }

    /** Restore a persisted framing on the active backend. */
    fun applyCamera(camera: XapkCameraState) {
        val native = nativeRendererRef
        if (native != null) {
            native.updateParams(native.params.withCamera(camera.yaw.toDouble(), camera.pitch.toDouble(), camera.distance.toDouble()))
        } else {
            glSurfaceView?.applyCamera(
                theta = camera.yaw.toDouble() * 2.0 * Math.PI,
                phi = camera.pitch.toDouble() * Math.PI
            )
        }
    }

    fun startCinematic(mode: ChalCamera.CinematicMode, reducedMotion: Boolean) {
        // Director mode is a Chal GLES feature; the native backend has no equivalent.
        glSurfaceView?.startCinematic(mode, reducedMotion)
    }

    fun stopCinematic() {
        glSurfaceView?.stopCinematic()
    }

    fun startBenchmark() {
        glSurfaceView?.startBenchmark()
    }

    fun cancelBenchmark() {
        glSurfaceView?.cancelBenchmark()
    }
}

/** Runtime gate for the library's ARM64/Vulkan 1.1-only execution contract. */
private object XapkNativeSupport {
    private const val VULKAN_API_VERSION_1_1 = 0x00401000

    /** @return the fallback reason (NONE when supported) plus an optional detail string. */
    fun evaluate(context: Context): Pair<ChalFallbackReason, String?> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ChalFallbackReason.API_TOO_OLD to "runtime Vulkan 1.1 version reporting requires Android 10 or newer"
        }
        if (Build.SUPPORTED_64_BIT_ABIS.none { it == "arm64-v8a" }) {
            return ChalFallbackReason.NOT_ARM64 to "the native renderer is arm64-v8a only"
        }
        val packageManager = context.packageManager
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, VULKAN_API_VERSION_1_1)) {
            return ChalFallbackReason.NO_VULKAN to "Vulkan 1.1 is not reported by this device"
        }
        if (!NativeBridge.ensureLoaded(context.applicationContext)) {
            return ChalFallbackReason.LIBRARY_LOAD_FAILED to NativeBridge.loadFailure
        }
        return ChalFallbackReason.NONE to null
    }
}
