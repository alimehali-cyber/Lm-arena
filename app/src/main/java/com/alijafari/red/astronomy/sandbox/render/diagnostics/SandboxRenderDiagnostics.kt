package com.alijafari.red.astronomy.sandbox.render.diagnostics

import java.util.concurrent.atomic.AtomicReference

/**
 * Real-time performance and rendering metrics.
 */
data class RenderDiagnosticsSnapshot(
    val fps: Float = 0.0f,
    val frameTimeMs: Float = 0.0f,
    val activeBodyCount: Int = 0,
    val drawCallCount: Int = 0,
    val snapshotSequence: Long = 0L,
    val snapshotAgeMs: Long = 0L,
    val cameraDistance: Float = 0.0f,
    val cameraTarget: String = "(0, 0, 0)",
    val scaleMode: String = "SOLAR_SYSTEM"
)

class SandboxRenderDiagnostics {
    private var frameCount = 0
    private var lastFpsTimestampNs = System.nanoTime()
    private var currentFps = 60.0f

    private var lastDrawCalls = 0
    private var lastFrameTimeMs = 16.6f

    private val snapshotRef = AtomicReference(RenderDiagnosticsSnapshot())

    fun onFrameStart(): Long {
        return System.nanoTime()
    }

    fun onFrameEnd(
        frameStartNs: Long,
        drawCalls: Int,
        activeBodies: Int,
        snapshotSeq: Long,
        snapshotTimestampMs: Long,
        cameraDistance: Float,
        cameraTargetStr: String,
        scaleModeStr: String
    ) {
        val nowNs = System.nanoTime()
        val frameDurationNs = nowNs - frameStartNs
        lastFrameTimeMs = (frameDurationNs / 1_000_000.0f).coerceIn(0.1f, 100.0f)
        lastDrawCalls = drawCalls

        frameCount++
        val elapsedFromFpsNs = nowNs - lastFpsTimestampNs
        if (elapsedFromFpsNs >= 500_000_000L) { // Update FPS every 500ms
            currentFps = (frameCount * 1_000_000_000.0f) / elapsedFromFpsNs
            frameCount = 0
            lastFpsTimestampNs = nowNs
        }

        val ageMs = if (snapshotTimestampMs > 0) (System.currentTimeMillis() - snapshotTimestampMs) else 0L

        snapshotRef.set(
            RenderDiagnosticsSnapshot(
                fps = currentFps,
                frameTimeMs = lastFrameTimeMs,
                activeBodyCount = activeBodies,
                drawCallCount = lastDrawCalls,
                snapshotSequence = snapshotSeq,
                snapshotAgeMs = ageMs,
                cameraDistance = cameraDistance,
                cameraTarget = cameraTargetStr,
                scaleMode = scaleModeStr
            )
        )
    }

    fun getLatestSnapshot(): RenderDiagnosticsSnapshot = snapshotRef.get()
}
