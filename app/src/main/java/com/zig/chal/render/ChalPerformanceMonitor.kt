package com.zig.chal.render

import com.zig.chal.config.ChalPerformanceConfig
import com.zig.chal.config.ChalRayTracingQuality
import kotlin.math.abs
import kotlin.math.min

/**
 * Performance Monitor with PID-based adaptive resolution.
 *
 * Verbatim port of the reference engine's `src/performance/monitor.ts` (ring-buffer frame-time
 * statistics, rolling FPS, frame-budget usage, and the damped PID resolution controller).
 */
class ChalPerformanceMonitor(private val nowMillis: () -> Double = { android.os.SystemClock.elapsedRealtime().toDouble() }) {

    /** Performance metrics snapshot (`PerformanceMetrics`). */
    data class PerformanceMetrics(
        val currentFPS: Int = 0,
        val frameTimeMs: Double = 0.0,
        val rollingAverageFPS: Int = 0,
        val quality: ChalRayTracingQuality = ChalRayTracingQuality.HIGH,
        val renderResolution: Double = 1.0
    )

    /** Fixed-size ring buffer for frame times. */
    private class RingBuffer(private val capacity: Int) {
        private val data = DoubleArray(capacity)
        private var index = 0
        private var count = 0

        fun push(value: Double) {
            data[index] = value
            index = (index + 1) % capacity
            if (count < capacity) count++
        }

        fun size(): Int = count

        fun last(): Double = if (count == 0) 0.0 else data[(index - 1 + capacity) % capacity]

        fun average(): Double {
            if (count == 0) return 0.0
            var sum = 0.0
            for (i in 0 until count) sum += data[i]
            return sum / count
        }
    }

    private val window = 60
    private val frameTimes = RingBuffer(window)

    private var cachedAvgTime = 0.0
    private var cachedAvgFPS = 0.0
    private var cacheValid = false

    private var currentQuality: ChalRayTracingQuality = ChalRayTracingQuality.HIGH
    private var renderResolution: Double = 1.0

    // PID Controller State (Phase 4.1: Stabilization)
    private var errorIntegral = 0.0
    private var prevError = 0.0
    private var isStabilized = false
    private var lastResolutionChangeTime = 0.0

    private val metrics = PerformanceMetrics()

    /**
     * Feed one frame's delta time (ms) and return the updated metrics.
     */
    fun updateMetrics(deltaTime: Double): PerformanceMetrics {
        frameTimes.push(deltaTime)
        cacheValid = false

        if (ChalPerformanceConfig.Resolution.ENABLE_DYNAMIC_SCALING) {
            applyPidScaling(deltaTime)
        }

        return getMetrics(deltaTime)
    }

    /** PID-Based Adaptive Scaling (Phase 4.1). */
    private fun applyPidScaling(dt: Double) {
        val target = ChalPerformanceConfig.Scheduler.FRAME_BUDGET_MS * 0.95 // 95% headroom
        val error = target - dt
        val now = nowMillis()

        val kp = ChalPerformanceConfig.Resolution.Pid.KP
        val ki = ChalPerformanceConfig.Resolution.Pid.KI
        val kd = ChalPerformanceConfig.Resolution.Pid.KD
        val deadzone = ChalPerformanceConfig.Resolution.Pid.DEADZONE
        val cooldownMs = ChalPerformanceConfig.Resolution.Pid.COOLDOWN_MS
        val integralClamp = ChalPerformanceConfig.Resolution.Pid.INTEGRAL_CLAMP

        // DEADZONE: If the error is within `deadzone` fraction of target, do nothing.
        // This prevents sub-millisecond oscillation from causing resolution ping-pong.
        val absError = abs(error)
        if (absError < target * deadzone) {
            // We're close enough. Zero the derivative to prevent jitter on re-entry.
            prevError = error
            isStabilized = true
            return
        }

        // COOLDOWN: Don't change resolution more than once per `cooldownMs`.
        // Each resolution change forces the GPU to reconfigure framebuffer state,
        // which itself causes a frame-time spike that feeds back into the PID.
        if (now - lastResolutionChangeTime < cooldownMs) {
            prevError = error
            return
        }

        // 1. Proportional
        val pOut = kp * error

        // 2. Integral (tightly clamped to prevent wind-up)
        errorIntegral = min(
            maxOf(errorIntegral + error, -integralClamp),
            integralClamp
        )
        val iOut = ki * errorIntegral

        // 3. Derivative
        val dOut = kd * (error - prevError)
        prevError = error

        // Total correction
        val adjustment = pOut + iOut + dOut

        // Apply to resolution (damped, with minimum step to avoid float noise)
        val delta = adjustment * 0.01
        if (abs(delta) < 0.001) return // Sub-pixel change -- skip

        val newRes = renderResolution + delta
        setRenderResolution(newRes)
        lastResolutionChangeTime = now

        // If we are consistently within 3% of target, mark as stabilized
        isStabilized = absError < target * 0.03
    }

    private fun ensureCache() {
        if (!cacheValid) {
            cachedAvgTime = frameTimes.average()
            cachedAvgFPS = if (cachedAvgTime > 0.0) 1000.0 / cachedAvgTime else 0.0
            cacheValid = true
        }
    }

    fun getMetrics(currentDeltaTime: Double? = null): PerformanceMetrics {
        ensureCache()

        val dt = currentDeltaTime ?: (if (frameTimes.size() > 0) frameTimes.last() else 0.0)
        val fps = if (dt > 0.0) 1000.0 / dt else 0.0

        return PerformanceMetrics(
            currentFPS = Math.round(fps).toInt(),
            frameTimeMs = Math.round(cachedAvgTime * 100.0) / 100.0,
            rollingAverageFPS = Math.round(cachedAvgFPS).toInt(),
            quality = currentQuality,
            renderResolution = renderResolution
        )
    }

    fun setQuality(quality: ChalRayTracingQuality) {
        currentQuality = quality
    }

    /**
     * Seed the resolution scale from the user-facing `renderScale` parameter.
     *
     * The reference does this in `useAnimation`: `setRenderResolution(params.renderScale || 1.0)`,
     * after which the PID controller owns the value.
     */
    fun seedRenderScale(scale: Double) {
        if (scale.isFinite() && scale > 0.0) setRenderResolution(scale)
    }

    private fun setRenderResolution(res: Double) {
        renderResolution = min(
            maxOf(res, ChalPerformanceConfig.Resolution.MIN_SCALE),
            ChalPerformanceConfig.Resolution.MAX_SCALE
        )
    }

    /** Frame budget usage as a percentage of one 60 FPS frame. */
    fun getFrameTimeBudgetUsage(): Double {
        ensureCache()
        val targetTime = 1000.0 / ChalPerformanceConfig.Scheduler.TARGET_FPS
        return (cachedAvgTime / targetTime) * 100.0
    }

    fun isStabilized(): Boolean = isStabilized
}
