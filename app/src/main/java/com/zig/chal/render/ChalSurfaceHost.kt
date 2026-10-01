package com.zig.chal.render

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    LIBRARY_LOAD_FAILED,

    /** The native renderer failed after startup, so the screen recovered with GLES. */
    RUNTIME_FAILURE
}

/**
 * Backend-selecting SurfaceView host for Chal in Lab. Prefers the original XAPK Vulkan renderer when
 * the ABI/API/device requirements are met. If the native surface fails at runtime, it replaces it
 * with the disclosed GLES compatibility renderer instead of leaving a permanently blank surface.
 */
class ChalSurfaceHost(
    context: Context,
    initialParams: ChalSimulationParams
) : FrameLayout(context) {
    private var glSurfaceView: ChalSurfaceView? = null
    private var nativeRendererRef: XapkChalRenderer? = null
    private var nativeSurface: XapkNativeSurfaceView? = null
    private var displayRefreshRateHz = 60.0
    private var resumed = false
    private var released = false

    /** Backend currently presenting frames. Replaced in place if a native renderer fails or retries. */
    lateinit var renderer: ChalRendererBackend
        private set

    private var fallbackReasonInternal: ChalFallbackReason = ChalFallbackReason.NONE
    private var fallbackDetailInternal: String? = null

    /** Hard failure in the active renderer; informational fallback details are separate. */
    val errorMessage: String?
        get() = renderer.errorMessage

    val isUsingXapkRenderer: Boolean
        get() = nativeRendererRef != null

    /** Why the GLES fallback is active, or [ChalFallbackReason.NONE] when the native path is live. */
    val fallbackReason: ChalFallbackReason
        get() = if (nativeRendererRef == null) fallbackReasonInternal else ChalFallbackReason.NONE

    /** Optional loader/runtime detail for the bilingual fallback notice. */
    val fallbackDetail: String?
        get() = fallbackDetailInternal

    /** A load failure can be retried; a runtime device/driver failure cannot. */
    val isNativeLoadRetryable: Boolean
        get() = nativeRendererRef == null && fallbackReasonInternal == ChalFallbackReason.LIBRARY_LOAD_FAILED

    /** Root UI refreshes its renderer/status state after a runtime switch or explicit retry. */
    var onBackendChanged: (() -> Unit)? = null

    var onTap: (() -> Unit)? = null
        set(value) {
            field = value
            glSurfaceView?.onTap = value
            nativeSurface?.onTap = value
        }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        displayRefreshRateHz = readDisplayRefreshRateHz()

        val gate = XapkNativeSupport.evaluate(context)
        if (gate.first == ChalFallbackReason.NONE) {
            installNativeRenderer(initialParams)
        } else {
            fallbackReasonInternal = gate.first
            fallbackDetailInternal = gate.second
            installGlesFallback(initialParams)
        }
        renderer.setDisplayRefreshRate(displayRefreshRateHz)
    }

    private fun installNativeRenderer(params: ChalSimulationParams) {
        val native = XapkChalRenderer(params)
        val initialFailure = native.errorMessage
        if (initialFailure != null) {
            fallbackReasonInternal = ChalFallbackReason.LIBRARY_LOAD_FAILED
            fallbackDetailInternal = initialFailure
            installGlesFallback(params)
            return
        }

        native.onFatalFailure = ::switchToGlesFallback
        val surface = XapkNativeSurfaceView(context, native).also { it.onTap = onTap }
        nativeRendererRef = native
        nativeSurface = surface
        renderer = native
        addView(surface, matchParentLayoutParams())
        renderer.setDisplayRefreshRate(displayRefreshRateHz)
    }

    private fun installGlesFallback(params: ChalSimulationParams) {
        val surface = ChalSurfaceView(context)
        surface.renderer.updateParams(params)
        surface.renderer.setDisplayRefreshRate(displayRefreshRateHz)
        surface.onTap = onTap
        glSurfaceView = surface
        renderer = surface.renderer
        addView(surface, matchParentLayoutParams())
        if (resumed) surface.onResume()
    }

    private fun matchParentLayoutParams() =
        LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    /**
     * Retry extracting/loading the native renderer after a transient load failure. On success, replace
     * the GLES surface in place and keep the existing view model/session alive.
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
        native.errorMessage?.let { detail ->
            fallbackReasonInternal = ChalFallbackReason.LIBRARY_LOAD_FAILED
            fallbackDetailInternal = detail
            return false
        }
        val surface = XapkNativeSurfaceView(context, native).also { it.onTap = onTap }
        native.onFatalFailure = ::switchToGlesFallback

        glSurfaceView?.onPause()
        glSurfaceView?.releaseGl()
        removeAllViews()
        glSurfaceView = null
        nativeSurface = surface
        nativeRendererRef = native
        renderer = native
        fallbackReasonInternal = ChalFallbackReason.NONE
        fallbackDetailInternal = null
        addView(surface, matchParentLayoutParams())
        renderer.setDisplayRefreshRate(displayRefreshRateHz)
        if (resumed) native.onResume()
        notifyBackendChanged()
        return true
    }

    private fun readDisplayRefreshRateHz(): Double = try {
        val rate = display?.refreshRate?.toDouble() ?: 60.0
        if (rate.isFinite() && rate > 0.0) rate else 60.0
    } catch (_: Throwable) {
        60.0
    }

    fun setDisplayRefreshRate(refreshRateHz: Double) {
        if (refreshRateHz.isFinite() && refreshRateHz > 0.0) {
            displayRefreshRateHz = refreshRateHz
            renderer.setDisplayRefreshRate(refreshRateHz)
        }
    }

    fun onResume() {
        resumed = true
        if (nativeRendererRef != null) nativeRendererRef?.onResume() else glSurfaceView?.onResume()
    }

    fun onPause() {
        resumed = false
        if (nativeRendererRef != null) nativeRendererRef?.onPause() else glSurfaceView?.onPause()
    }

    fun release() {
        released = true
        onBackendChanged = null
        onTap = null
        nativeRendererRef?.onFatalFailure = null
        glSurfaceView?.releaseGl()
        glSurfaceView = null
        nativeSurface = null
        // The native SurfaceHolder callback owns nativeOnSurfaceDestroyed(), just as in the XAPK.
    }

    fun resetCamera() {
        nativeRendererRef?.resetCamera() ?: glSurfaceView?.resetCamera()
    }

    fun resetScenarioPitch() {
        nativeRendererRef?.resetPitchForScenario() ?: glSurfaceView?.resetScenarioPitch()
    }

    /** Camera framing from whichever renderer is active, normalized to the shared XAPK model. */
    fun captureCamera(): XapkCameraState {
        val native = nativeRendererRef
        if (native != null) return native.cameraState()

        val gl = glSurfaceView?.renderer ?: return renderer.params.cameraState()
        val state = gl.cameraSnapshot()
        val yaw = (state.theta / (2.0 * Math.PI)).toFloat().let {
            if (it.isFinite()) it.mod(1.0f) else XapkCameraState.DEFAULT_YAW
        }
        val pitch = (state.phi / Math.PI).toFloat().let {
            if (it.isFinite()) it.coerceIn(XapkCameraState.MIN_PITCH, XapkCameraState.MAX_PITCH)
            else XapkCameraState.DEFAULT_PITCH
        }
        val distance = gl.params.zoom.toFloat().let {
            if (it.isFinite()) it.coerceIn(XapkCameraState.MIN_DISTANCE, XapkCameraState.MAX_DISTANCE)
            else XapkCameraState.DEFAULT_DISTANCE
        }
        return XapkCameraState(yaw = yaw, pitch = pitch, distance = distance)
    }

    /** Restore persisted framing on the active backend. */
    fun applyCamera(camera: XapkCameraState) {
        val native = nativeRendererRef
        if (native != null) {
            native.updateParams(
                native.params.withCamera(
                    camera.yaw.toDouble(),
                    camera.pitch.toDouble(),
                    camera.distance.toDouble()
                )
            )
        } else {
            glSurfaceView?.let { surface ->
                val updated = surface.renderer.params.withCamera(
                    camera.yaw.toDouble(),
                    camera.pitch.toDouble(),
                    camera.distance.toDouble()
                )
                surface.renderer.updateParams(updated)
                surface.applyCamera(camera.yaw.toDouble() * 2.0 * Math.PI, camera.pitch.toDouble() * Math.PI)
            }
        }
    }

    fun startCinematic(mode: ChalCamera.CinematicMode, reducedMotion: Boolean) {
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

    private fun switchToGlesFallback(detail: String) {
        // Surface/JNI callbacks can arrive during touch or SurfaceHolder dispatch. Defer view removal
        // even when already on main so recovery never mutates the hierarchy re-entrantly.
        Handler(Looper.getMainLooper()).post {
            if (released) return@post
            val native = nativeRendererRef ?: return@post

            // A native drag/pinch mirrors the live framing into params, so this retains the best
            // available session state when a driver failure occurs mid-gesture.
            val fallbackParams = native.params
            nativeRendererRef = null
            native.onFatalFailure = null
            fallbackReasonInternal = ChalFallbackReason.RUNTIME_FAILURE
            fallbackDetailInternal = detail

            runCatching { native.onPause() }
            runCatching { native.onSurfaceDestroyed() }

            nativeSurface?.let { removeView(it) }
            nativeSurface = null
            installGlesFallback(fallbackParams)
            notifyBackendChanged()
        }
    }

    private fun notifyBackendChanged() {
        post { onBackendChanged?.invoke() }
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
