package com.zig.museum.core.data

/**
 * State machine per §7.1
 */
enum class TileState {
    REQUESTED,
    DECODING,
    RESIDENT,
    EVICTABLE
}
