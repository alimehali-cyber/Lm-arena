package com.alijafari.red.astronomy.sandbox.model

import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import kotlin.math.cbrt

/**
 * Immutable domain representation of a celestial, artificial, or theoretical body in the simulation.
 */
data class SandboxBody(
    val id: String,
    val type: SandboxBodyType,
    val nameEn: String,
    val nameFa: String,
    val massKg: Double,
    val radiusMeters: Double,
    val position: Vector3D = Vector3D.ZERO,
    val velocity: Vector3D = Vector3D.ZERO,
    val acceleration: Vector3D = Vector3D.ZERO,
    val isFixed: Boolean = false,
    val isActive: Boolean = true,
    val collisionPolicy: CollisionPolicy = if (type == SandboxBodyType.BLACK_HOLE) {
        CollisionPolicy.BLACK_HOLE_ACCEDE
    } else if (type == SandboxBodyType.THEORETICAL_WORMHOLE) {
        CollisionPolicy.PASS_THROUGH
    } else {
        CollisionPolicy.MERGE_CONSERVE_MOMENTUM
    },
    val visualScaleMultiplier: Double = 1.0,
    val colorHex: Long = type.defaultColorHex,
    val theoreticalMetadata: Map<String, String> = emptyMap()
) {
    /**
     * Schwarzschild event horizon radius: Rs = 2GM / c^2.
     */
    val schwarzschildRadiusMeters: Double
        get() = if (massKg > 0.0) {
            (2.0 * AstroPhysicsConstants.G * massKg) / (AstroPhysicsConstants.SPEED_OF_LIGHT * AstroPhysicsConstants.SPEED_OF_LIGHT)
        } else {
            0.0
        }

    /**
     * Validates whether all numerical state components are finite and positive where required.
     */
    fun isValid(): Boolean {
        return massKg > 0.0 &&
                radiusMeters > 0.0 &&
                massKg.isFinite() &&
                radiusMeters.isFinite() &&
                position.isFinite() &&
                velocity.isFinite() &&
                acceleration.isFinite()
    }

    /**
     * Generates a merged body from this body and another when undergoing inelastic collision.
     */
    fun mergeWith(other: SandboxBody, newId: String = id): SandboxBody {
        val totalMass = this.massKg + other.massKg
        if (totalMass <= 0.0) return this

        val combinedVelocity = (this.velocity * this.massKg + other.velocity * other.massKg) / totalMass
        val combinedPosition = (this.position * this.massKg + other.position * other.massKg) / totalMass
        val combinedRadius = cbrt(this.radiusMeters * this.radiusMeters * this.radiusMeters +
                other.radiusMeters * other.radiusMeters * other.radiusMeters)

        // The dominant body retains the primary identity and visual classification
        val dominant = if (this.massKg >= other.massKg) this else other

        return dominant.copy(
            id = newId,
            massKg = totalMass,
            radiusMeters = combinedRadius,
            position = combinedPosition,
            velocity = combinedVelocity,
            acceleration = Vector3D.ZERO
        )
    }
}
