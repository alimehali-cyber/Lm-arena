package com.zig.gargantua

import com.zig.gargantua.renderer.GargantuaRenderState
import com.zig.gargantua.renderer.GargantuaTelemetry
import com.zig.gargantua.renderer.RenderStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Pure JVM unit tests for Gargantua render state, atomic snapshot transfer,
 * and thread-safe telemetry emission.
 */
class GargantuaRenderStateTest {

    @Test
    fun defaultStateHasExpectedInitialValues() {
        val state = GargantuaRenderState()
        assertEquals(0, state.viewportWidth)
        assertEquals(0, state.viewportHeight)
        assertEquals(0.5f, state.renderScale, 0.001f)
        assertEquals(24.0f, state.camDist, 0.001f)
        assertEquals(82.0f, state.camInclinationDeg, 0.001f)
        assertEquals(0.0f, state.camAzimuthDeg, 0.001f)
        assertEquals(1.25f, state.exposure, 0.001f)
        assertTrue(state.enableBloom)
        assertEquals(0.20f, state.bloomIntensity, 0.001f)
        assertFalse(state.isPaused)
        assertTrue(state.isDarkTheme)
        assertFalse(state.isPersian)
    }

    @Test
    fun renderScaleCanBeConfiguredUpToScientificCeiling() {
        val state = GargantuaRenderState(renderScale = 1.0f)
        assertEquals(1.0f, state.renderScale, 0.001f)

        val holder = RenderStateHolder(state)
        holder.updateState { it.copy(renderScale = 0.75f) }
        assertEquals(0.75f, holder.getState().renderScale, 0.001f)
    }

    @Test
    fun stateHolderPerformsAtomicUpdatesWithoutShearing() {
        val holder = RenderStateHolder()

        holder.updateState {
            it.copy(viewportWidth = 1080, viewportHeight = 2400)
        }

        val updated = holder.getState()
        assertEquals(1080, updated.viewportWidth)
        assertEquals(2400, updated.viewportHeight)
        assertFalse(updated.isPaused)

        holder.updateState { it.copy(isPaused = true) }
        assertTrue(holder.getState().isPaused)
        assertEquals(1080, holder.getState().viewportWidth)
    }

    @Test
    fun stateHolderHandlesConcurrentUpdatesRaceFree() {
        val holder = RenderStateHolder()
        val threadCount = 8
        val iterationsPerThread = 500
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        for (t in 0 until threadCount) {
            executor.submit {
                try {
                    for (i in 0 until iterationsPerThread) {
                        holder.updateState {
                            it.copy(viewportWidth = it.viewportWidth + 1)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue("Concurrent updates must complete within timeout", latch.await(5, TimeUnit.SECONDS))
        executor.shutdown()

        assertEquals(threadCount * iterationsPerThread, holder.getState().viewportWidth)
    }

    @Test
    fun telemetryHolderEmitsExpectedDiagnostics() {
        val holder = RenderStateHolder()
        val initialTelemetry = holder.getTelemetry()

        assertEquals(0f, initialTelemetry.fps, 0.001f)
        assertEquals("Detecting...", initialTelemetry.glesVersion)
        assertNull(initialTelemetry.errorMessage)

        holder.setTelemetry(
            GargantuaTelemetry(
                fps = 59.8f,
                frameTimeMs = 16.7f,
                glesVersion = "OpenGL ES 3.2",
                glRenderer = "Adreno 740",
                isInitialized = true,
                renderScale = 0.5f,
                renderResolution = "540x1200",
                isHdrActive = true,
                exposure = 1.25f,
                camDist = 24.0f,
                camInclinationDeg = 82.0f,
                camAzimuthDeg = 0.0f
            )
        )

        val updated = holder.getTelemetry()
        assertEquals(59.8f, updated.fps, 0.01f)
        assertEquals(16.7f, updated.frameTimeMs, 0.01f)
        assertEquals("OpenGL ES 3.2", updated.glesVersion)
        assertEquals(0.5f, updated.renderScale, 0.001f)
        assertEquals("540x1200", updated.renderResolution)
        assertTrue(updated.isHdrActive)
        assertEquals(1.25f, updated.exposure, 0.001f)
        assertEquals(24.0f, updated.camDist, 0.001f)
        assertTrue(updated.isInitialized)
    }
}
