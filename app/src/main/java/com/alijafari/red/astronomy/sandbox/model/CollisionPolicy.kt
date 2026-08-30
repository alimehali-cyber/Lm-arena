package com.alijafari.red.astronomy.sandbox.model

/**
 * Defines interaction behaviour when two bodies' physical radii contact/overlap.
 */
enum class CollisionPolicy {
    /**
     * Perfectly inelastic merger conserving total mass, center-of-mass momentum,
     * and calculating combined volume radius assuming constant mean density.
     */
    MERGE_CONSERVE_MOMENTUM,

    /**
     * Black Hole accretion: Absorbs incoming body's mass and linear momentum,
     * recalculates Schwarzschild radius Rs = 2GM/c^2.
     */
    BLACK_HOLE_ACCEDE,

    /**
     * Non-interacting ghost pass-through (used for theoretical wormholes or test particles).
     */
    PASS_THROUGH,

    /**
     * Destroys both colliding bodies upon impact.
     */
    DESTROY_BOTH
}

/**
 * Event record dispatched when a physical contact or accretion occurs.
 */
data class CollisionEvent(
    val timestampSeconds: Double,
    val primaryBodyId: String,
    val secondaryBodyId: String,
    val collisionPosition: Vector3D,
    val relativeVelocity: Double,
    val resultingBodyId: String?,
    val policyApplied: CollisionPolicy
)
