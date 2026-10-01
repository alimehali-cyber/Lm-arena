package com.zig.chal.render

/**
 * Measures time between admitted frames, not between GLSurfaceView callbacks. Skipped vsyncs must
 * accumulate: resetting the timestamp on every callback permanently stalls a half-rate renderer.
 * The caller resets this clock after lifecycle/pause transitions so background time is not animated.
 */
class ChalFrameClock {
    private var lastFrameMs: Double? = null

    fun reset(nowMs: Double) {
        lastFrameMs = nowMs.takeIf { it.isFinite() }
    }

    fun frameDelta(nowMs: Double, budgetMs: Double, force: Boolean = false): Double? {
        if (!nowMs.isFinite() || !budgetMs.isFinite() || budgetMs <= 0.0) return null
        val last = lastFrameMs
        if (last == null || nowMs < last) {
            lastFrameMs = nowMs
            return 0.0
        }
        val elapsed = nowMs - last
        if (!force && elapsed < budgetMs * 0.95) return null
        lastFrameMs = nowMs
        return elapsed
    }
}
