package com.alijafari.red.astronomy.sandbox.snapshot

import com.alijafari.red.astronomy.sandbox.model.CollisionEvent
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import com.alijafari.red.astronomy.sandbox.physics.DiagnosticsSnapshot
import com.alijafari.red.astronomy.sandbox.physics.EngineStatus

/**
 * Immutable snapshot of a single body's state prepared for thread-safe consumption by OpenGL/Compose.
 */
data class BodyRenderState(
    val id: String,
    val type: SandboxBodyType,
    val nameEn: String,
    val nameFa: String,
    val posX: Double,
    val posY: Double,
    val posZ: Double,
    val velX: Double,
    val velY: Double,
    val velZ: Double,
    val accX: Double,
    val accY: Double,
    val accZ: Double,
    val massKg: Double,
    val radiusMeters: Double,
    val visualScale: Double,
    val colorHex: Long,
    val isFixed: Boolean,
    val isActive: Boolean,
    val theoreticalMetadata: Map<String, String> = emptyMap()
) {
    val isBlackHole: Boolean
        get() = type == SandboxBodyType.BLACK_HOLE

    val isTheoreticalWormhole: Boolean
        get() = type == SandboxBodyType.THEORETICAL_WORMHOLE

    val schwarzschildRadiusMeters: Double
        get() = if (massKg > 0.0) {
            (2.0 * AstroPhysicsConstants.G * massKg) / (AstroPhysicsConstants.SPEED_OF_LIGHT * AstroPhysicsConstants.SPEED_OF_LIGHT)
        } else {
            0.0
        }
}

/**
 * Complete immutable snapshot frame containing all body kinematics, diagnostics, and events.
 */
data class SandboxRenderFrame(
    val frameSequenceNumber: Long,
    val simulationTimeSeconds: Double,
    val bodies: List<BodyRenderState>,
    val diagnostics: DiagnosticsSnapshot,
    val recentCollisions: List<CollisionEvent>,
    val engineStatus: EngineStatus,
    val physicsExecutionDurationNanos: Long = 0,
    val substepsExecutedThisFrame: Int = 0
) {
    companion object {
        val EMPTY = SandboxRenderFrame(
            frameSequenceNumber = 0,
            simulationTimeSeconds = 0.0,
            bodies = emptyList(),
            diagnostics = DiagnosticsSnapshot(
                totalMassKg = 0.0,
                kineticEnergyJoules = 0.0,
                potentialEnergyJoules = 0.0,
                totalEnergyJoules = 0.0,
                energyDriftFraction = 0.0,
                totalLinearMomentum = com.alijafari.red.astronomy.sandbox.model.Vector3D.ZERO,
                totalAngularMomentum = com.alijafari.red.astronomy.sandbox.model.Vector3D.ZERO,
                centerOfMassPosition = com.alijafari.red.astronomy.sandbox.model.Vector3D.ZERO,
                centerOfMassVelocity = com.alijafari.red.astronomy.sandbox.model.Vector3D.ZERO,
                minPairDistanceMeters = Double.MAX_VALUE,
                minProximityRatio = Double.MAX_VALUE
            ),
            recentCollisions = emptyList(),
            engineStatus = EngineStatus.Initialized
        )
    }
}
