package com.alijafari.red.astronomy.sandbox.physics

import com.alijafari.red.astronomy.sandbox.model.CollisionEvent
import com.alijafari.red.astronomy.sandbox.model.SandboxBody
import com.alijafari.red.astronomy.sandbox.model.Vector3D

/**
 * Operating status of the Gravity Sandbox physics engine.
 */
sealed class EngineStatus {
    object Initialized : EngineStatus()
    object Running : EngineStatus()
    object Paused : EngineStatus()
    data class Error(val reason: String, val details: String) : EngineStatus()
}

/**
 * Core Newtonian N-body Gravitational Physics Engine.
 *
 * Provides deterministic 4th-order symplectic integration, collision detection,
 * fail-safe anomaly detection, and exact diagnostics measurement.
 */
class GravitySandboxEngine(
    val forceSolver: GravitationalForceSolver = GravitationalForceSolver(),
    val collisionHandler: CollisionHandler = CollisionHandler(),
    val failSafe: SimulationFailSafe = SimulationFailSafe(),
    val diagnostics: PhysicsDiagnostics = PhysicsDiagnostics()
) {
    val stateBuffer = PhysicsStateBuffer(AstroPhysicsConstants.MAX_BODIES)
    val yoshidaIntegrator = SymplecticYoshidaIntegrator(forceSolver)
    val verletIntegrator = VelocityVerletIntegrator(forceSolver)
    val timestepController = SimulationTimestepController()

    var useYoshida4thOrder: Boolean = true
    var handleCollisions: Boolean = true

    var currentSimulationTimeSeconds: Double = 0.0
        private set

    var totalStepsExecuted: Long = 0
        private set

    var engineStatus: EngineStatus = EngineStatus.Initialized
        private set

    private var latestDiagnostics: DiagnosticsSnapshot? = null

    /**
     * Initializes the simulation with a given list of bodies.
     */
    fun loadBodies(bodies: List<SandboxBody>, resetTime: Boolean = true) {
        stateBuffer.loadFromBodies(bodies)
        if (resetTime) {
            currentSimulationTimeSeconds = 0.0
            totalStepsExecuted = 0
            timestepController.reset()
            diagnostics.resetInitialEnergy()
        }
        forceSolver.computeAccelerations(stateBuffer)
        collisionHandler.clearEvents()
        latestDiagnostics = diagnostics.computeDiagnostics(
            stateBuffer,
            potentialEnergyFromSolver = forceSolver.lastPotentialEnergyJoules,
            minDistanceMeters = forceSolver.lastMinPairDistanceMeters,
            minProximityRatio = forceSolver.lastMinProximityRatio
        )
        engineStatus = EngineStatus.Initialized
    }

    /**
     * Executes a single discrete integration step of duration dt seconds.
     * Guaranteed deterministic for identical state and dt.
     */
    fun stepSingle(dt: Double): Boolean {
        if (engineStatus is EngineStatus.Error) return false

        // 1. Numerical Integration Step
        if (useYoshida4thOrder) {
            yoshidaIntegrator.step(stateBuffer, dt)
        } else {
            verletIntegrator.step(stateBuffer, dt)
        }

        currentSimulationTimeSeconds += dt
        totalStepsExecuted++

        // 2. Collision Processing (if enabled)
        if (handleCollisions) {
            val collisionOccurred = collisionHandler.processCollisions(stateBuffer, currentSimulationTimeSeconds)
            if (collisionOccurred) {
                // Recompute forces after mass/position mergers
                forceSolver.computeAccelerations(stateBuffer)
            }
        }

        // 3. Fail-Safe Verification
        when (val validation = failSafe.validateState(stateBuffer)) {
            is FailSafeStatus.NumericalAnomaly -> {
                engineStatus = EngineStatus.Error(validation.reason, validation.details)
                return false
            }
            FailSafeStatus.Ok -> { /* Normal execution */ }
        }

        // 4. Update Diagnostics
        latestDiagnostics = diagnostics.computeDiagnostics(
            stateBuffer,
            potentialEnergyFromSolver = forceSolver.lastPotentialEnergyJoules,
            minDistanceMeters = forceSolver.lastMinPairDistanceMeters,
            minProximityRatio = forceSolver.lastMinProximityRatio
        )

        return true
    }

    /**
     * Advances the simulation given wall-clock frame duration in seconds.
     * Manages fixed substepping and time scaling.
     */
    fun advanceWallClock(dtWallClockSeconds: Double): Int {
        if (engineStatus is EngineStatus.Error) return 0

        val currentProximity = forceSolver.lastMinProximityRatio
        val steps = timestepController.computeSteps(dtWallClockSeconds, currentProximity)

        var stepsCompleted = 0
        for (stepDt in steps) {
            val success = stepSingle(stepDt)
            if (!success) break
            stepsCompleted++
        }

        return stepsCompleted
    }

    fun getCurrentDiagnostics(): DiagnosticsSnapshot {
        return latestDiagnostics ?: diagnostics.computeDiagnostics(stateBuffer)
    }

    fun getActiveBodies(): List<SandboxBody> {
        return stateBuffer.toBodyList()
    }

    fun getRecentCollisionEvents(): List<CollisionEvent> {
        return ArrayList(collisionHandler.recentCollisionEvents)
    }
}
