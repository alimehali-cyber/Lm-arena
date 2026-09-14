package com.zig.museum.core.data

/**
 * Instrumentation counters per §7.3 and §15.3
 */
data class TileStoreInstrumentation(
    var residentTiles: Int = 0,
    var residentBytes: Long = 0L,
    var queuedTiles: Int = 0,
    var decoderQueueDepth: Int = 0,
    var evictionsPerFrame: Int = 0,
    var uploadsPerFrame: Int = 0,
    var fallbackFrames: Int = 0,
    var maxFrameTimeMs: Float = 0f,
    var p95FrameTimeMs: Float = 0f,
    var totalFrames: Long = 0L
) {
    fun snapshot(): TileStoreInstrumentation = copy()

    fun toCsvHeader(): String =
        "frame,residentTiles,residentBytes,queuedTiles,decoderQueueDepth,evictionsPerFrame,uploadsPerFrame,fallbackFrames,frameTimeMs"

    fun toCsvRow(frame: Long, frameTimeMs: Float): String =
        "$frame,$residentTiles,$residentBytes,$queuedTiles,$decoderQueueDepth,$evictionsPerFrame,$uploadsPerFrame,$fallbackFrames,$frameTimeMs"
}
