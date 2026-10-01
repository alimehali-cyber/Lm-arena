package com.zig.chal

import com.zig.chal.config.ChalCameraConfig
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.render.ChalCamera
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChalCameraTimingTest {
    @Test
    fun scenarioPitchResetPreservesYaw() {
        val camera = ChalCamera()
        camera.onPointerDown(0.0, 0.0)
        camera.onPointerMove(40.0, 20.0)
        camera.onPointerUp()
        val yawAfterDrag = camera.snapshot().theta

        camera.resetPitchForScenario()

        assertEquals(yawAfterDrag, camera.snapshot().theta, 0.0)
        assertEquals(ChalCameraConfig.DEFAULT_VERTICAL_ANGLE, camera.snapshot().phi, 0.0)
    }

    @Test
    fun autoSpinTracksElapsedTimeAcrossRefreshRates() {
        val expectedDelta = 0.2
        val results = listOf(30, 60, 120).map { refreshRate ->
            val camera = ChalCamera()
            val params = ChalSimulationParams(autoSpin = expectedDelta)
            val deltaSeconds = 1.0 / refreshRate

            repeat(refreshRate) { frame ->
                camera.update((frame + 1) * deltaSeconds * 1_000.0, deltaSeconds, params) { }
            }
            camera.snapshot().theta - Math.PI
        }

        results.forEach { delta -> assertEquals(expectedDelta, delta, 1e-9) }
    }

    @Test
    fun dragInertiaTracksElapsedTimeAcrossRefreshRates() {
        val results = listOf(30, 60, 120).map { refreshRate ->
            val camera = ChalCamera()
            val params = ChalSimulationParams(autoSpin = 0.0)
            camera.onPointerDown(0.0, 0.0)
            camera.onPointerMove(10.0, 0.0)
            camera.onPointerUp()
            val initialAfterDrag = camera.snapshot().theta
            val deltaSeconds = 1.0 / refreshRate

            repeat(refreshRate) { frame ->
                camera.update((frame + 1) * deltaSeconds * 1_000.0, deltaSeconds, params) { }
            }
            camera.snapshot().theta - initialAfterDrag
        }

        results.drop(1).forEach { delta -> assertEquals(results.first(), delta, 1e-9) }
    }

    @Test
    fun stoppingACinematicAlsoStopsItsRecoveryMotion() {
        val camera = ChalCamera()
        val params = ChalSimulationParams()
        camera.startCinematic(ChalCamera.CinematicMode.ORBIT, params, now = 0.0, reducedMotion = false)
        camera.update(now = 120_001.0, dtSeconds = 0.1, params = params) { }
        assertTrue(camera.isRecovering)

        camera.stopCinematic()

        assertFalse(camera.isRecovering)
    }

    @Test
    fun diveTourPreservesTheConfiguredAutoSpin() {
        val camera = ChalCamera()
        val configuredAutoSpin = 0.012
        var liveParams = ChalSimulationParams(autoSpin = configuredAutoSpin)
        camera.startCinematic(ChalCamera.CinematicMode.DIVE, liveParams, now = 0.0, reducedMotion = false)

        repeat(60) { frame ->
            camera.update((frame + 1) * (1_000.0 / 60.0), 1.0 / 60.0, liveParams) { transform ->
                liveParams = with(liveParams, transform)
            }
        }

        assertEquals(configuredAutoSpin, liveParams.autoSpin, 0.0)
    }

    @Test
    fun orbitTourIsStableAcrossRefreshRates() {
        val params = ChalSimulationParams()
        val finalAngles = listOf(30, 60, 120).map { refreshRate ->
            val camera = ChalCamera()
            var liveParams = params
            camera.startCinematic(ChalCamera.CinematicMode.ORBIT, liveParams, now = 0.0, reducedMotion = false)
            val deltaSeconds = 1.0 / refreshRate

            repeat(refreshRate * 10) { frame ->
                val now = (frame + 1) * deltaSeconds * 1_000.0
                camera.update(now, deltaSeconds, liveParams) { transform ->
                    liveParams = with(liveParams, transform)
                }
            }
            camera.snapshot().theta
        }

        finalAngles.drop(1).forEach { angle ->
            assertEquals(finalAngles.first(), angle, 0.02)
        }
    }
}
