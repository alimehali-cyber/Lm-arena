package com.zig.gargantua.renderer

import java.util.concurrent.atomic.AtomicReference

/**
 * Immutable render-state snapshot passed from the UI thread to the OpenGL ES render thread.
 * Guarantees race-free updates and prevents state shearing without blocking the render loop.
 */
data class GargantuaRenderState(
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0,
    val renderScale: Float = 0.5f, // Scaled rendering factor in (0.25..1.0), 1.0 = native full resolution ceiling
    val isPaused: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isPersian: Boolean = false,
    val mass: Float = 1.0f,
    val spin: Float = 0.8f,
    val camDist: Float = 32.0f,
    val camInclinationDeg: Float = 80.0f,
    val camAzimuthDeg: Float = 0.0f,
    val camTargetX: Float = 0.0f,
    val camTargetY: Float = 0.0f,
    val camTargetZ: Float = 0.0f,
    val maxSteps: Int = 180,
    val useGeodesicShader: Boolean = true,
    val enableDisk: Boolean = true,
    val diskOuterRadius: Float = 22.0f,
    val exposure: Float = 1.8f,
    val enableBloom: Boolean = true,
    val bloomIntensity: Float = 0.20f,
    val bloomThreshold: Float = 1.0f
)

/**
 * Real-time performance and presentation telemetry emitted by the GL render thread to the UI thread.
 */
data class GargantuaTelemetry(
    val fps: Float = 0f,
    val frameTimeMs: Float = 0f,
    val glesVersion: String = "Detecting...",
    val glRenderer: String = "",
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,
    val spin: Float = 0.8f,
    val isGeodesicActive: Boolean = true,
    val isDiskActive: Boolean = true,
    val iscoRadius: Float = 2.91f,
    val renderScale: Float = 0.5f,
    val renderResolution: String = "",
    val isHdrActive: Boolean = false,
    val exposure: Float = 1.8f,
    val camDist: Float = 32.0f,
    val camInclinationDeg: Float = 80.0f,
    val camAzimuthDeg: Float = 0.0f,
    val camTargetX: Float = 0.0f,
    val camTargetY: Float = 0.0f,
    val camTargetZ: Float = 0.0f
)

/**
 * Thread-safe holder for render state and telemetry using atomic references.
 */
class RenderStateHolder(initial: GargantuaRenderState = GargantuaRenderState()) {

    private val stateRef = AtomicReference(initial)
    private val telemetryRef = AtomicReference(GargantuaTelemetry())

    fun getState(): GargantuaRenderState = stateRef.get()

    fun updateState(transform: (GargantuaRenderState) -> GargantuaRenderState): GargantuaRenderState {
        var current: GargantuaRenderState
        var next: GargantuaRenderState
        do {
            current = stateRef.get()
            next = transform(current)
        } while (!stateRef.compareAndSet(current, next))
        return next
    }

    fun setState(state: GargantuaRenderState) {
        stateRef.set(state)
    }

    fun getTelemetry(): GargantuaTelemetry = telemetryRef.get()

    fun setTelemetry(telemetry: GargantuaTelemetry) {
        telemetryRef.set(telemetry)
    }

    fun updateTelemetry(transform: (GargantuaTelemetry) -> GargantuaTelemetry) {
        var current: GargantuaTelemetry
        var next: GargantuaTelemetry
        do {
            current = telemetryRef.get()
            next = transform(current)
        } while (!telemetryRef.compareAndSet(current, next))
    }
}
