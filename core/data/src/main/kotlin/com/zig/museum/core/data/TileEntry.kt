package com.zig.museum.core.data

/**
 * TileEntry per §7.1
 * textureHandle is Any? to keep core:data free of Filament types (engine owns Filament per §4.2).
 * In real engine integration, handle will be cast to Texture instance inside :core:engine.
 */
data class TileEntry(
    val key: TileKey,
    var state: TileState = TileState.REQUESTED,
    var byteSize: Long = 0L,
    var lastUsedFrame: Long = 0L,
    var textureHandle: Any? = null,
    var decodeStartMs: Long = 0L,
    var residentSinceMs: Long = 0L
)
