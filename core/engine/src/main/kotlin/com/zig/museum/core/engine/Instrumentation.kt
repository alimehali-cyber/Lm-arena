package com.zig.museum.core.engine

/**
 * Instrumentation per §15.3
 * - Frame timing via Choreographer/FrameMetrics or engine's own timing, ring buffer last 300 frames
 * - Tile store counters from §7.3
 * - GPU memory estimate: sum of resident texture bytes by format and size
 * - Debug overlay (debug builds only) showing fps, p50/p95, tier, resident tiles and bytes, uploads per frame, evictions, fallback frames
 * - Headless benchmark mode: --benchmark <object> --tier <n> --seconds <n> that runs scripted camera path and writes CSV
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

data class DebugOverlayData(
    val fps: Float,
    val p50Ms: Float,
    val p95Ms: Float,
    val tier: Int,
    val residentTiles: Int,
    val residentBytes: Long,
    val uploadsPerFrame: Int,
    val evictions: Int,
    val fallbackFrames: Int
)

class Instrumentation {
    val frameTimings = FrameTimingRingBuffer(300)
    var tileCounters = TileStoreCounters()
    var gpuMemory = GpuMemoryEstimate()
    var currentTier: Int = 0

    fun toDebugOverlay(): DebugOverlayData {
        return DebugOverlayData(
            fps = frameTimings.fps(),
            p50Ms = frameTimings.p50(),
            p95Ms = frameTimings.p95(),
            tier = currentTier,
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
