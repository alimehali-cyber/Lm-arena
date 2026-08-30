package com.alijafari.red.astronomy.sandbox.snapshot

import com.alijafari.red.astronomy.sandbox.physics.GravitySandboxEngine
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe lock-free double-buffered snapshot publisher.
 * Physics worker updates and publishes frames via atomic pointer exchange.
 * OpenGL render thread / Compose UI read the latest published snapshot without contention.
 */
class DoubleBufferSnapshotManager {
    private val publishedFrame = AtomicReference<SandboxRenderFrame>(SandboxRenderFrame.EMPTY)
    private var sequenceCounter: Long = 0

    /**
     * Publishes a new snapshot from the engine's current state.
     * Invoked exclusively by the physics worker thread.
     */
    fun publishSnapshot(
        engine: GravitySandboxEngine,
        physicsDurationNanos: Long = 0,
        substepsExecuted: Int = 0
    ): SandboxRenderFrame {
        val state = engine.stateBuffer
        val n = state.activeCount
        val bodyStates = ArrayList<BodyRenderState>(n)

        for (i in 0 until n) {
            if (!state.isActive[i]) continue
            bodyStates.add(
                BodyRenderState(
                    id = state.id[i],
                    type = state.type[i],
                    nameEn = state.nameEn[i],
                    nameFa = state.nameFa[i],
                    posX = state.posX[i],
                    posY = state.posY[i],
                    posZ = state.posZ[i],
                    velX = state.velX[i],
                    velY = state.velY[i],
                    velZ = state.velZ[i],
                    accX = state.accX[i],
                    accY = state.accY[i],
                    accZ = state.accZ[i],
                    massKg = state.mass[i],
                    radiusMeters = state.radius[i],
                    visualScale = state.visualScale[i],
                    colorHex = state.colorHex[i],
                    isFixed = state.isFixed[i],
                    isActive = state.isActive[i],
                    theoreticalMetadata = state.theoreticalMetadata[i]
                )
            )
        }

        val frame = SandboxRenderFrame(
            frameSequenceNumber = ++sequenceCounter,
            simulationTimeSeconds = engine.currentSimulationTimeSeconds,
            bodies = bodyStates,
            diagnostics = engine.getCurrentDiagnostics(),
            recentCollisions = engine.getRecentCollisionEvents(),
            engineStatus = engine.engineStatus,
            physicsExecutionDurationNanos = physicsDurationNanos,
            substepsExecutedThisFrame = substepsExecuted
        )

        publishedFrame.set(frame)
        return frame
    }

    /**
     * Retrieves the latest published frame atomically.
     * Safe to call from UI and OpenGL rendering threads.
     */
    fun getLatestSnapshot(): SandboxRenderFrame {
        return publishedFrame.get()
    }
}
