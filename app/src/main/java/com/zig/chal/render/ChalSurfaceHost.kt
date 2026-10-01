package com.zig.chal.render

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import com.orchestrsim.blackhole.NativeBridge
import com.zig.chal.config.ChalRendererBackend
import com.zig.chal.config.ChalSimulationParams

/**
 * Backend-selecting SurfaceView host for Chal in Lab. Prefer the original XAPK Vulkan renderer when
 * the exact ABI/API/device requirements are met; otherwise keep the existing GLES renderer alive
 * and expose a clear fallback notice to the user.
 */
class ChalSurfaceHost(
    context: Context,
    initialParams: ChalSimulationParams
) : FrameLayout(context) {
    private var glSurfaceView: ChalSurfaceView? = null
    private var nativeRenderer: XapkChalRenderer? = null
    private var fallbackNotice: String? = null

    val renderer: ChalRendererBackend
    val isUsingXapkRenderer: Boolean
        get() = nativeRenderer != null

    val errorMessage: String?
        get() = renderer.errorMessage ?: fallbackNotice

    var onTap: (() -> Unit)? = null
        set(value) {
            field = value
            // XAPK's SurfaceView has no tap-to-hide action; only Chal's GLES fallback uses it.
            glSurfaceView?.onTap = value
        }

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        val unsupportedReason = XapkNativeSupport.unsupportedReason(context)
        if (unsupportedReason == null) {
            val native = XapkChalRenderer(initialParams)
            val surface = XapkNativeSurfaceView(context, native)
            nativeRenderer = native
            renderer = native
            addView(surface, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            val glSurface = ChalSurfaceView(context)
            glSurface.renderer.updateParams(initialParams)
            glSurfaceView = glSurface
            renderer = glSurface.renderer
            fallbackNotice = "XAPK Vulkan renderer unavailable ($unsupportedReason); using the Chal GLES fallback."
            addView(glSurface, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        renderer.setDisplayRefreshRate(displayRefreshRateHz())
    }

    private fun displayRefreshRateHz(): Double = try {
        val rate = display?.refreshRate?.toDouble() ?: 60.0
        if (rate.isFinite() && rate > 0.0) rate else 60.0
    } catch (_: Throwable) {
        60.0
    }

    fun setDisplayRefreshRate(refreshRateHz: Double) = renderer.setDisplayRefreshRate(refreshRateHz)

    fun onResume() {
        nativeRenderer?.onResume() ?: glSurfaceView?.onResume()
    }

    fun onPause() {
        nativeRenderer?.onPause() ?: glSurfaceView?.onPause()
    }

    fun release() {
        glSurfaceView?.releaseGl()
        // The native SurfaceHolder callback owns nativeOnSurfaceDestroyed(), just as in the XAPK.
    }

    fun resetCamera() {
        nativeRenderer?.resetCamera() ?: glSurfaceView?.resetCamera()
    }

    fun resetScenarioPitch() {
        nativeRenderer?.resetPitchForScenario()
    }

    fun startCinematic(mode: ChalCamera.CinematicMode, reducedMotion: Boolean) {
        if (nativeRenderer != null) nativeRenderer?.startCinematic()
        else glSurfaceView?.startCinematic(mode, reducedMotion)
    }

    fun stopCinematic() {
        if (nativeRenderer != null) nativeRenderer?.stopCinematic()
        else glSurfaceView?.stopCinematic()
    }

    fun startBenchmark() {
        if (nativeRenderer != null) nativeRenderer?.startBenchmark()
        else glSurfaceView?.startBenchmark()
    }

    fun cancelBenchmark() {
        if (nativeRenderer != null) nativeRenderer?.cancelBenchmark()
        else glSurfaceView?.cancelBenchmark()
    }
}

/** Runtime gate for the library's ARM64/Vulkan 1.1-only execution contract. */
private object XapkNativeSupport {
    private const val VULKAN_API_VERSION_1_1 = 0x00401000

    fun unsupportedReason(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return "runtime Vulkan 1.1 version reporting requires Android 10 or newer"
        }
        if (Build.SUPPORTED_64_BIT_ABIS.none { it == "arm64-v8a" }) {
            return "the native renderer is arm64-v8a only"
        }
        val packageManager = context.packageManager
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, VULKAN_API_VERSION_1_1)) {
            return "Vulkan 1.1 is not reported by this device"
        }
        if (!NativeBridge.ensureLoaded(context.applicationContext)) {
            return "the ARM64 native libraries could not be loaded${NativeBridge.loadFailure?.let { ": $it" }.orEmpty()}"
        }
        return null
    }
}
