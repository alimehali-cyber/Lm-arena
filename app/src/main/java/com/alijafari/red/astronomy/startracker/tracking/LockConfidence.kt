package com.alijafari.red.astronomy.startracker.tracking

/**
 * Confidence Ladder per roadmap Part 6 Phase 8
 * FULL_LOCK = 4+ verified stars
 * MARGINAL_LOCK = 2-3 stars / TRIAD
 * NO_LOCK = no usable stars, gyro dead-reckoning with decaying confidence
 * AMBIGUOUS = conflicting, discard, never guess
 */
enum class LockConfidence {
    FULL_LOCK,
    MARGINAL_LOCK,
    NO_LOCK,
    AMBIGUOUS
}
