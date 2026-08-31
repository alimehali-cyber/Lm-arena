package com.alijafari.red.astronomy.sandbox.worker

import com.alijafari.red.astronomy.sandbox.model.SandboxBody
import com.alijafari.red.astronomy.sandbox.physics.EngineStatus
import com.alijafari.red.astronomy.sandbox.physics.GravitySandboxEngine
import com.alijafari.red.astronomy.sandbox.presets.SandboxPreset
import com.alijafari.red.astronomy.sandbox.snapshot.DoubleBufferSnapshotManager
import com.alijafari.red.astronomy.sandbox.snapshot.SandboxRenderFrame
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker executing the physics simulation loop asynchronously.
 * Completely isolates high-frequency numerical integration from the UI/Compose and OpenGL render threads.
 */
class SandboxPhysicsWorker(
    val engine: GravitySandboxEngine = GravitySandboxEngine(),
    val snapshotManager: DoubleBufferSnapshotManager = DoubleBufferSnapshotManager()
) {
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(true)
    private var workerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Target simulation frame rate for wall-clock pacing (60 Hz tick evaluations).
     */
    var targetUpdateFrequencyHz: Int = 60

    fun start() {
        if (isRunning.getAndSet(true)) return

        isPaused.set(false)
        workerJob = scope.launch {
            var lastTimeNanos = System.nanoTime()
            val targetIntervalNanos = 1_000_000_000L / targetUpdateFrequencyHz

            while (isActive && isRunning.get()) {
                val currentTimeNanos = System.nanoTime()
                val elapsedSeconds = (currentTimeNanos - lastTimeNanos) / 1_000_000_000.0
                lastTimeNanos = currentTimeNanos

                // Clamp frame delta to prevent spiral of death if system hitches
                val clampedElapsedSeconds = elapsedSeconds.coerceIn(0.001, 0.1)

                val physicsStartNanos = System.nanoTime()
                var substepsExecuted = 0

                if (!isPaused.get() && engine.engineStatus !is EngineStatus.Error) {
                    substepsExecuted = engine.advanceWallClock(clampedElapsedSeconds)
                }

                val physicsDurationNanos = System.nanoTime() - physicsStartNanos

                // Publish completed snapshot to render buffer
                snapshotManager.publishSnapshot(
                    engine = engine,
                    physicsDurationNanos = physicsDurationNanos,
                    substepsExecuted = substepsExecuted
                )

                // Sleep / delay for remaining budget to maintain target tick pacing
                val workTimeNanos = System.nanoTime() - currentTimeNanos
                val sleepNanos = targetIntervalNanos - workTimeNanos
                if (sleepNanos > 1_000_000L) {
                    delay(sleepNanos / 1_000_000L)
                } else {
                    yield()
                }
            }
        }
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        if (engine.engineStatus !is EngineStatus.Error) {
            isPaused.set(false)
        }
    }

    fun isPaused(): Boolean = isPaused.get()

    fun stepOnce(dtSeconds: Double? = null) {
        val dt = dtSeconds ?: engine.timestepController.baseTimestepSeconds
        val startNanos = System.nanoTime()
        engine.stepSingle(dt)
        val duration = System.nanoTime() - startNanos
        snapshotManager.publishSnapshot(engine, duration, 1)
    }

    fun loadPreset(preset: SandboxPreset, userMultiplier: Double? = null) {
        val wasRunning = !isPaused.get()
        pause()
        engine.timestepController.baseTimestepSeconds = preset.recommendedBaseTimestepSeconds
        engine.timestepController.presetBaseSpeedMultiplier = preset.recommendedTimeSpeedMultiplier
        if (userMultiplier != null) {
            engine.timestepController.userSpeedMultiplier = userMultiplier
        }
        engine.timestepController.reset()
        engine.loadBodies(preset.bodyFactory(), resetTime = true)
        snapshotManager.publishSnapshot(engine, 0, 0)
        if (wasRunning) {
            resume()
        }
    }

    fun setBodies(bodies: List<SandboxBody>, resetTime: Boolean = true) {
        val wasRunning = !isPaused.get()
        pause()
        engine.loadBodies(bodies, resetTime = resetTime)
        snapshotManager.publishSnapshot(engine, 0, 0)
        if (wasRunning) {
            resume()
        }
    }

    fun setTimeMultiplier(multiplier: Double) {
        engine.timestepController.userSpeedMultiplier = multiplier.coerceIn(0.01, 100_000.0)
    }

    fun getLatestRenderFrame(): SandboxRenderFrame {
        return snapshotManager.getLatestSnapshot()
    }

    fun stop() {
        isRunning.set(false)
        workerJob?.cancel()
        workerJob = null
    }
}
