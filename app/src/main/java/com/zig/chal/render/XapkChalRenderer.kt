package com.zig.chal.render

import android.view.Surface
import com.orchestrsim.blackhole.NativeBridge
import com.zig.chal.config.ChalRayTracingQuality
import com.zig.chal.config.ChalRendererBackend
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.config.XapkCameraState
import com.zig.chal.config.XapkParameterBlock
import com.zig.chal.config.XapkRendererContract
import com.zig.chal.physics.ChalKerrMetric
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * UI adapter over the XAPK's Vulkan/Rust renderer. The renderer itself remains the extracted native
 * library; this class only mirrors the JNI lifecycle, 16-float/four-int state contract, camera
 * gestures, and telemetry consumed by Chal's existing Lab UI.
 */
class XapkChalRenderer(initialParams: ChalSimulationParams) : ChalRendererBackend {
    private val lock = Any()

    @Volatile
    override var params: ChalSimulationParams = initialParams
        private set

    @Volatile
    override var errorMessage: String? = null
        private set

    /** The host uses this callback to switch to GLES if the native render path becomes unhealthy. */
    @Volatile
    var onFatalFailure: ((String) -> Unit)? = null

    @Volatile
    private var surfaceReady = false

    @Volatile
    private var surfaceConfigured = false

    private var cameraYaw = XapkCameraState.DEFAULT_YAW
    private var cameraPitch = XapkCameraState.DEFAULT_PITCH
    private var cameraDistance = initialParams.zoom.toFloat()
    private var targetFps = 60
    private val telemetry = FloatArray(3)
    private var telemetryFps = 0
    private var telemetryFrameTimeMs = 0.0
    private var telemetryScale = XapkRendererContract.qualityFor(initialParams.features.rayTracingQuality).renderScale.toDouble()

    init {
        cameraYaw = initialParams.cameraYaw.toFloat().let { if (it.isFinite()) it.mod(1.0f) else XapkCameraState.DEFAULT_YAW }
        cameraPitch = initialParams.cameraPitch.toFloat()
            .coerceIn(XapkCameraState.MIN_PITCH, XapkCameraState.MAX_PITCH)
        cameraDistance = initialParams.zoom.toFloat()
            .coerceIn(XapkCameraState.MIN_DISTANCE, XapkCameraState.MAX_DISTANCE)
        if (!NativeBridge.ensureLoaded()) {
            errorMessage = "Could not load the XAPK Vulkan renderer: ${NativeBridge.loadFailure.orEmpty()}"
        }
    }

    override fun updateParams(newParams: ChalSimulationParams) {
        synchronized(lock) {
            val previous = params
            params = newParams
            if (newParams.cameraYaw != previous.cameraYaw || newParams.cameraPitch != previous.cameraPitch) {
                cameraYaw = newParams.cameraYaw.toFloat()
                    .let { if (it.isFinite()) it.mod(1.0f) else XapkCameraState.DEFAULT_YAW }
                cameraPitch = newParams.cameraPitch.toFloat()
                    .coerceIn(XapkCameraState.MIN_PITCH, XapkCameraState.MAX_PITCH)
            }
            if (newParams.zoom != previous.zoom) {
                cameraDistance = newParams.zoom.toFloat()
                    .coerceIn(XapkCameraState.MIN_DISTANCE, XapkCameraState.MAX_DISTANCE)
            }
            // The reference only pauses on lifecycle; this port exposes a pause control, so a user
            // pause has to reach the native scheduler too.
            if (newParams.paused != previous.paused && surfaceReady && NativeBridge.ensureLoaded()) {
                try {
                    NativeBridge.nativeOnPause(newParams.paused)
                } catch (failure: Throwable) {
                    reportFatalFailure("XAPK Vulkan pause failed: ${failure.message ?: failure.javaClass.simpleName}")
                }
            }
            if (surfaceConfigured && NativeBridge.ensureLoaded()) pushState()
        }
    }

    override fun setDisplayRefreshRate(refreshRateHz: Double) {
        if (refreshRateHz.isFinite() && refreshRateHz > 0.0) targetFps = refreshRateHz.roundToInt().coerceAtLeast(1)
    }

    /** SurfaceHolder.Callback: XAPK sends the Android Surface directly to JNI. */
    fun onSurfaceCreated(surface: Surface) {
        synchronized(lock) {
            if (!NativeBridge.ensureLoaded()) {
                reportFatalFailure("Could not load the XAPK Vulkan renderer: ${NativeBridge.loadFailure.orEmpty()}")
                return
            }
            try {
                NativeBridge.nativeOnSurfaceCreated(surface)
                surfaceReady = true
                surfaceConfigured = false
                errorMessage = null
            } catch (failure: Throwable) {
                surfaceReady = false
                reportFatalFailure("XAPK Vulkan surface initialization failed: ${failure.message ?: failure.javaClass.simpleName}")
            }
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        synchronized(lock) {
            if (!surfaceReady) return
            try {
                NativeBridge.nativeOnSurfaceChanged(width, height)
                surfaceConfigured = true
                errorMessage = null
                // The reference calls its parameter and camera setters after every size change.
                pushState()
            } catch (failure: Throwable) {
                reportFatalFailure("XAPK Vulkan surface resize failed: ${failure.message ?: failure.javaClass.simpleName}")
            }
        }
    }

    fun onSurfaceDestroyed() {
        synchronized(lock) {
            if (!surfaceReady) return
            try {
                NativeBridge.nativeOnSurfaceDestroyed()
            } catch (failure: Throwable) {
                errorMessage = "XAPK Vulkan surface teardown failed: ${failure.message ?: failure.javaClass.simpleName}"
            } finally {
                surfaceReady = false
                surfaceConfigured = false
            }
        }
    }

    fun onResume() {
        if (!NativeBridge.ensureLoaded()) return
        synchronized(lock) {
            try {
                // Resume the engine only if the user had not parked the simulation on purpose.
                NativeBridge.nativeOnPause(params.paused)
            } catch (failure: Throwable) {
                reportFatalFailure("XAPK Vulkan resume failed: ${failure.message ?: failure.javaClass.simpleName}")
            }
        }
    }

    fun onPause() {
        if (!NativeBridge.ensureLoaded()) return
        synchronized(lock) {
            try {
                NativeBridge.nativeOnPause(true)
            } catch (failure: Throwable) {
                reportFatalFailure("XAPK Vulkan pause failed: ${failure.message ?: failure.javaClass.simpleName}")
            }
        }
    }

    /** Exact GestureDetector scroll mapping from the XAPK's f3.a listener. */
    fun onDrag(distanceX: Float, distanceY: Float, viewWidth: Int, viewHeight: Int) {
        val minimumDimension = max(1, minOf(viewWidth, viewHeight)).toFloat()
        synchronized(lock) {
            cameraYaw = wrapYaw(cameraYaw - distanceX / minimumDimension)
            cameraPitch = (cameraPitch - distanceY / minimumDimension * XapkCameraState.DRAG_PITCH_SENSITIVITY)
                .coerceIn(XapkCameraState.MIN_PITCH, XapkCameraState.MAX_PITCH)
            mirrorCameraIntoParams()
            sendCamera()
        }
    }

    /** XAPK scale listener accumulates detector factors and uses baseDistance / scale. */
    fun onPinch(baseDistance: Float, cumulativeScale: Float) {
        synchronized(lock) {
            val safeScale = cumulativeScale.coerceAtLeast(0.1f)
            cameraDistance = (baseDistance / safeScale)
                .coerceIn(XapkCameraState.MIN_DISTANCE, XapkCameraState.MAX_DISTANCE)
            mirrorCameraIntoParams()
            sendCamera()
        }
    }

    /** Full framing reset (orientation, distance, pause), matching `ChalCamera.reset`. */
    fun resetCamera() {
        synchronized(lock) {
            cameraYaw = XapkCameraState.DEFAULT_YAW
            cameraPitch = XapkCameraState.DEFAULT_PITCH
            cameraDistance = XapkCameraState.DEFAULT_DISTANCE
                .coerceIn(XapkCameraState.MIN_DISTANCE, XapkCameraState.MAX_DISTANCE)
            params = params.withCamera(
                cameraYaw.toDouble(),
                cameraPitch.toDouble(),
                cameraDistance.toDouble()
            ).copy(paused = false)
            if (surfaceConfigured && NativeBridge.ensureLoaded()) pushState()
        }
    }

    /** Live camera framing, in the XAPK's own normalized coordinate system. */
    fun cameraState(): XapkCameraState = synchronized(lock) {
        XapkCameraState(cameraYaw, cameraPitch, cameraDistance)
    }

    /** The persisted params carry the live camera so a session restore resumes the same framing. */
    private fun mirrorCameraIntoParams() {
        params = params.withCamera(
            cameraYaw.toDouble(),
            cameraPitch.toDouble(),
            cameraDistance.toDouble()
        )
    }

    /** A scenario selection in the XAPK resets pitch and distance but leaves yaw untouched. */
    fun resetPitchForScenario() {
        synchronized(lock) {
            cameraPitch = XapkCameraState.DEFAULT_PITCH
            mirrorCameraIntoParams()
            if (surfaceConfigured) pushState()
        }
    }

    /** Read XAPK's three-float telemetry block and its Rust Kerr helper values. */
    override fun snapshot(): ChalRenderer.ChalSnapshot = synchronized(lock) {
        if (surfaceConfigured) {
            try {
                NativeBridge.nativeGetTelemetry(telemetry)
                telemetryFps = if (telemetry[0].isFinite()) telemetry[0].roundToInt().coerceAtLeast(0) else 0
                // The native block is [fps, frameTimeMs, renderScale]; prefer its own frame time and
                // only derive one when the engine reports nothing usable.
                telemetryFrameTimeMs = when {
                    telemetry[1].isFinite() && telemetry[1] > 0.0f -> telemetry[1].toDouble()
                    telemetryFps > 0 -> 1_000.0 / telemetryFps
                    else -> 0.0
                }
                if (telemetry[2].isFinite() && telemetry[2] > 0.0f) telemetryScale = telemetry[2].toDouble()
            } catch (failure: Throwable) {
                errorMessage = "XAPK Vulkan telemetry failed: ${failure.message ?: failure.javaClass.simpleName}"
            }
        }

        val mass = params.mass.coerceIn(0.1, 20.0)
        val spin = params.spin.coerceIn(0.0, 0.99)
        val horizon = nativeMetricOrFallback {
            NativeBridge.nativeKerrHorizon(mass, spin)
        } ?: ChalKerrMetric.calculateEventHorizon(mass, spin)
        val photonSphere = nativeMetricOrFallback {
            NativeBridge.nativeKerrPhotonSphere(mass, spin)
        } ?: ChalKerrMetric.calculatePhotonSphere(mass, spin)
        val isco = nativeMetricOrFallback {
            NativeBridge.nativeKerrIsco(mass, spin, true)
        } ?: ChalKerrMetric.calculateIsco(mass, spin, prograde = true)

        val radius = max(params.zoom * mass, horizon * 1.01)
        val timeDilation = ChalKerrMetric.calculateTimeDilation(radius, mass)
        val redshift = if (timeDilation > 0.0) (1.0 / timeDilation) - 1.0 else Double.POSITIVE_INFINITY
        val frameBudget = if (telemetryFrameTimeMs > 0.0) {
            telemetryFrameTimeMs / (1_000.0 / targetFps) * 100.0
        } else {
            0.0
        }
        val quality = params.features.rayTracingQuality
        val nativeQuality = XapkRendererContract.qualityFor(quality)

        ChalRenderer.ChalSnapshot(
            params = params.copy(renderScale = telemetryScale),
            currentFps = telemetryFps,
            frameTimeMs = telemetryFrameTimeMs,
            quality = quality,
            budgetUsage = frameBudget,
            raySteps = nativeQuality.steps,
            effectiveRenderScale = telemetryScale,
            eventHorizonRadius = horizon,
            photonSphereRadius = photonSphere,
            iscoRadius = isco,
            timeDilation = timeDilation,
            redshift = redshift,
            isCinematic = false,
            cinematicMode = null,
            targetFps = targetFps
        )
    }

    /** Native XAPK has no Chal-only benchmark/cinematic modes. */
    fun startBenchmark() = Unit
    fun cancelBenchmark() = Unit
    fun startCinematic() = Unit
    fun stopCinematic() = Unit

    private fun pushState() {
        val block: XapkParameterBlock = XapkRendererContract.build(
            params = params,
            camera = XapkCameraState(cameraYaw, cameraPitch, cameraDistance)
        )
        try {
            NativeBridge.nativeSetParams(block.floats, block.integers)
            sendCamera()
        } catch (failure: Throwable) {
            reportFatalFailure("XAPK Vulkan parameter update failed: ${failure.message ?: failure.javaClass.simpleName}")
        }
    }

    private fun sendCamera() {
        if (!surfaceConfigured || !NativeBridge.ensureLoaded()) return
        try {
            NativeBridge.nativeSetCamera(cameraYaw, cameraPitch, cameraDistance)
        } catch (failure: Throwable) {
            reportFatalFailure("XAPK Vulkan camera update failed: ${failure.message ?: failure.javaClass.simpleName}")
        }
    }

    private fun reportFatalFailure(message: String) {
        errorMessage = message
        onFatalFailure?.invoke(message)
    }

    private inline fun nativeMetricOrFallback(block: () -> Double): Double? {
        if (!NativeBridge.ensureLoaded()) return null
        return try {
            block().takeIf { it.isFinite() }
        } catch (failure: Throwable) {
            errorMessage = "XAPK Kerr telemetry failed: ${failure.message ?: failure.javaClass.simpleName}"
            null
        }
    }

    private fun wrapYaw(yaw: Float): Float {
        val wrapped = yaw % 1.0f
        return if (wrapped < 0.0f) wrapped + 1.0f else wrapped
    }
}
