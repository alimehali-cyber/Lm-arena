package com.zig.museum.core.engine

/**
 * Instrumentation per §15.3
 * - Frame timing via Choreographer/FrameMetrics or engine's own timing, ring buffer last 300 frames
 * - Tile store counters from §7.3
 * - GPU memory estimate: sum of resident texture bytes by format and size
 * - Debug overlay (debug builds only) showing fps, p50/p95, tier, resident tiles and bytes, uploads per frame, evictions, fallback frames
 * - Headless benchmark mode: --benchmark <object> --tier <n> --seconds <n> that runs scripted camera path and writes CSV
 *
 * Foundational Rebuild Phase 0.1: every field below must reflect something the engine actually
 * measured. There is no headless benchmark mode implemented yet (that is still an open gap, not
 * silently claimed here) -- see RenderLoopCounters for the render-loop honesty counters added in
 * this pass, which replace the previously-hardcoded tile counters as the primary on-screen signal
 * for "is Filament actually presenting frames".
 */

data class FrameTiming(
    val frameTimeMs: Float,
    val timestampNs: Long
)

class FrameTimingRingBuffer(private val capacity: Int = 300) {
    private val buffer = mutableListOf<FrameTiming>()
    private var index = 0

    fun add(timing: FrameTiming) {
        if (buffer.size < capacity) {
            buffer.add(timing)
        } else {
            buffer[index] = timing
            index = (index + 1) % capacity
        }
    }

    fun getAll(): List<FrameTiming> = buffer.toList()

    fun p50(): Float {
        if (buffer.isEmpty()) return 0f
        val sorted = buffer.map { it.frameTimeMs }.sorted()
        return sorted[sorted.size / 2]
    }

    fun p95(): Float {
        if (buffer.isEmpty()) return 0f
        val sorted = buffer.map { it.frameTimeMs }.sorted()
        val idx = (sorted.size * 0.95).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }

    fun fps(): Float {
        if (buffer.isEmpty()) return 0f
        val avg = buffer.map { it.frameTimeMs }.average().toFloat()
        return if (avg > 0f) 1000f / avg else 0f
    }

    fun max(): Float = buffer.maxOfOrNull { it.frameTimeMs } ?: 0f
}

data class TileStoreCounters(
    val residentTiles: Int = 0,
    val residentBytes: Long = 0,
    val uploadsPerFrame: Int = 0,
    val evictions: Int = 0,
    val fallbackFrames: Int = 0,
    val requests: Int = 0
)

data class GpuMemoryEstimate(
    val totalBytes: Long = 0,
    val textureBytes: Map<String, Long> = emptyMap()
)

/**
 * Foundational Rebuild Phase 0.1: honest render-loop counters.
 *
 * These count what actually happened in InspectorEngine.doFrame(), one increment per real event,
 * with no fallback/estimate/placeholder values mixed in. They exist specifically to answer "is
 * Filament presenting frames at all" without inferring it from fps math, because fps computed
 * from doFrame()'s wall-clock duration cannot distinguish "beginFrame returned false and nothing
 * was drawn" from "a real frame was rendered" -- both take under a millisecond of CPU time if the
 * GPU work is asynchronous, which is the leading hypothesis for the previously-reported
 * 899-1156 fps / sub-millisecond frame times (see docs/audit/MILESTONE_AUDIT.md, M11).
 */
data class RenderLoopCounters(
    /** Every Choreographer.FrameCallback invocation, regardless of whether swapChain/renderer/view were non-null. */
    val doFrameCalls: Long = 0,
    /** How many times doFrame() found swapChain, renderer, and view all non-null and attempted beginFrame(). */
    val beginFrameAttempts: Long = 0,
    /** beginFrame() returned true (frame should be drawn). */
    val beginFrameTrue: Long = 0,
    /** beginFrame() returned false (frame skipped per Filament's own frame-pacing logic). */
    val beginFrameFalse: Long = 0,
    /** beginFrame() itself threw. */
    val beginFrameThrew: Long = 0,
    /** render(view) was called (only possible when beginFrame() returned true). */
    val renderCalls: Long = 0,
    /** render(view) threw. */
    val renderThrew: Long = 0,
    /** endFrame() was called (only after a successful render()). */
    val endFrameCalls: Long = 0,
    /** endFrame() threw. */
    val endFrameThrew: Long = 0,
    /** doFrame() ran while swapChain was null (surface not yet created / already destroyed). */
    val swapChainNullCount: Long = 0,
    /** doFrame() ran while renderer was null. */
    val rendererNullCount: Long = 0,
    /** doFrame() ran while view was null. */
    val viewNullCount: Long = 0,
    /** Width/height passed to the most recent setViewport() call, i.e. the SurfaceView's measured size when the SwapChain was (re)created. 0 means never set. */
    val lastViewportWidth: Int = 0,
    val lastViewportHeight: Int = 0
) {
    /** True once at least one endFrame() has actually completed -- the only honest "did we present a frame" signal. */
    fun hasPresentedAtLeastOneFrame(): Boolean = endFrameCalls > 0

    fun summary(): String {
        return "doFrame=$doFrameCalls begin=$beginFrameAttempts(true=$beginFrameTrue false=$beginFrameFalse threw=$beginFrameThrew) " +
            "render=$renderCalls(threw=$renderThrew) end=$endFrameCalls(threw=$endFrameThrew) " +
            "nullSwapChain=$swapChainNullCount nullRenderer=$rendererNullCount nullView=$viewNullCount " +
            "viewport=${lastViewportWidth}x$lastViewportHeight"
    }
}

data class DebugOverlayData(
    val fps: Float,
    val p50Ms: Float,
    val p95Ms: Float,
    val tier: Int,
    val renderLoop: RenderLoopCounters,
    val tileStoreActive: Boolean,
    val residentTiles: Int,
    val residentBytes: Long,
    val uploadsPerFrame: Int,
    val evictions: Int,
    val fallbackFrames: Int
)

/**
 * "Render succeeds, screen stays black" investigation (post Phase 0+1): real-device telemetry
 * proved doFrame/beginFrame/render/endFrame all complete with zero exceptions (820/820), so the
 * bug is downstream of a successful render() call. These four data classes hold ONLY values read
 * back from Filament's own getters (LightManager.getIntensity/getColor/getDirection,
 * Camera.getPosition/getNear/getCullingFar/getFieldOfViewInDegrees, the actual generated mesh's
 * vertex positions, and android.view.Surface.isValid()/identityHashCode) -- never literals typed
 * here, so the debug overlay and logcat report what the engine actually did, not what the code
 * intended to do. Per owner's ordered investigation: (1) light, (3) camera/transform, (4)
 * swapchain/surface identity. (Step 2 -- the forced-unlit/no-cull diagnostic build -- is
 * InspectorEngine.diagnosticForceUnlitBrightNoCull, a runtime toggle, not a data class.)
 */
data class LightDiagnostics(
    val intensity: Float = 0f,
    val colorR: Float = 0f,
    val colorG: Float = 0f,
    val colorB: Float = 0f,
    val dirX: Float = 0f,
    val dirY: Float = 0f,
    val dirZ: Float = 0f,
    val valid: Boolean = false
) {
    fun summary(): String =
        if (!valid) "light: NOT YET READ BACK"
        else "light: intensity=${"%.1f".format(intensity)} color=(${"%.2f".format(colorR)},${"%.2f".format(colorG)},${"%.2f".format(colorB)}) dir=(${"%.2f".format(dirX)},${"%.2f".format(dirY)},${"%.2f".format(dirZ)})"
}

data class CameraDiagnostics(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val targetZ: Float = 0f,
    val near: Float = 0f,
    val far: Float = 0f,
    val fovDeg: Double = 0.0,
    val valid: Boolean = false
) {
    fun summary(): String =
        if (!valid) "camera: NOT YET READ BACK"
        else "camera: pos=(${"%.2f".format(posX)},${"%.2f".format(posY)},${"%.2f".format(posZ)}) target=(${"%.2f".format(targetX)},${"%.2f".format(targetY)},${"%.2f".format(targetZ)}) near=${"%.4f".format(near)} far=${"%.2f".format(far)} fov=${"%.1f".format(fovDeg)}deg"
}

data class ObjectBoundsDiagnostics(
    val objectId: String = "",
    val boundingRadius: Float = 0f,
    val vertexCount: Int = 0,
    val valid: Boolean = false
) {
    fun summary(): String =
        if (!valid) "object: NOT YET LOADED"
        else "object: id=$objectId boundingRadius=${"%.4f".format(boundingRadius)} vertices=$vertexCount"
}

data class SurfaceDiagnostics(
    val swapChainSurfaceHash: Int = 0,
    val swapChainSurfaceValid: Boolean = false,
    val surfaceViewInstanceHash: Int = 0,
    val androidViewFactoryInvocations: Int = 0
) {
    fun summary(): String =
        "surface: swapChainSurfaceHash=$swapChainSurfaceHash swapChainSurfaceValid=$swapChainSurfaceValid " +
            "surfaceViewInstanceHash=$surfaceViewInstanceHash androidViewFactoryCalls=$androidViewFactoryInvocations"
}

class Instrumentation {
    val frameTimings = FrameTimingRingBuffer(300)
    var tileCounters = TileStoreCounters()
    var gpuMemory = GpuMemoryEstimate()
    var currentTier: Int = 0

    /**
     * Foundational Rebuild Phase 0.1: real render-loop counters, mutated only from
     * InspectorEngine.doFrame()/setViewport() as the corresponding Filament calls actually
     * happen. Nothing here is a hardcoded literal.
     */
    var renderLoop = RenderLoopCounters()

    /**
     * Foundational Rebuild Phase 0.1: whether a real tile/asset system is wired into the render
     * path for the object currently loaded. As of this pass, nothing is wired (TileStoreBridge is
     * still not instantiated anywhere in app code -- see docs/audit/MILESTONE_AUDIT.md, M3), so
     * this is always false and the overlay must say so honestly instead of showing fabricated
     * tile counters.
     */
    var tileStoreActive: Boolean = false

    /**
     * "Render succeeds, screen stays black" investigation: real values read back from Filament's
     * own getters after each relevant call (see InspectorEngine.updateSunLight/
     * updateCameraFromRig/loadEllipsoidObject/createSwapChain), never hand-typed literals.
     */
    var lightDiagnostics = LightDiagnostics()
    var cameraDiagnostics = CameraDiagnostics()
    var objectBoundsDiagnostics = ObjectBoundsDiagnostics()
    var surfaceDiagnostics = SurfaceDiagnostics()

    fun toDebugOverlay(): DebugOverlayData {
        return DebugOverlayData(
            fps = frameTimings.fps(),
            p50Ms = frameTimings.p50(),
            p95Ms = frameTimings.p95(),
            tier = currentTier,
            renderLoop = renderLoop,
            tileStoreActive = tileStoreActive,
            residentTiles = tileCounters.residentTiles,
            residentBytes = tileCounters.residentBytes,
            uploadsPerFrame = tileCounters.uploadsPerFrame,
            evictions = tileCounters.evictions,
            fallbackFrames = tileCounters.fallbackFrames
        )
    }

    fun toCsv(): String {
        // CSV header: timestampNs,frameTimeMs,fps,p50,p95,residentTiles,residentBytes,uploads,evictions
        val sb = StringBuilder()
        sb.appendLine("timestampNs,frameTimeMs,fps,p50,p95,residentTiles,residentBytes,uploads,evictions,fallback")
        val overlay = toDebugOverlay()
        for (timing in frameTimings.getAll()) {
            sb.appendLine("${timing.timestampNs},${timing.frameTimeMs},${overlay.fps},${overlay.p50Ms},${overlay.p95Ms},${overlay.residentTiles},${overlay.residentBytes},${overlay.uploadsPerFrame},${overlay.evictions},${overlay.fallbackFrames}")
        }
        return sb.toString()
    }
}
