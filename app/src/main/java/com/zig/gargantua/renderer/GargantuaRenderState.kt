package com.zig.gargantua.renderer

import java.util.concurrent.atomic.AtomicReference

/**
 * Immutable render-state snapshot passed from the UI thread to the OpenGL ES render thread.
 * Guarantees race-free updates and prevents state shearing without blocking the render loop.
 */
data class GargantuaRenderState(
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0,
    val isPaused: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isPersian: Boolean = false
)

/**
 * Real-time performance telemetry emitted by the GL render thread to the UI thread.
 */
data class GargantuaTelemetry(
    val fps: Float = 0f,
    val frameTimeMs: Float = 0f,
    val glesVersion: String = "Detecting...",
    val glRenderer: String = "",
    val isInitialized: Boolean = false,
    val errorMessage: String? = null
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
