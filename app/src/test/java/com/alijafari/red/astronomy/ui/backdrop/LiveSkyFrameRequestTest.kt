package com.alijafari.red.astronomy.ui.backdrop

import com.alijafari.red.astronomy.ui.rendering.SkySceneModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSkyFrameRequestTest {

    private val minute = 60_000L
    private val base = 120L * minute // a minute boundary

    private fun request(
        visible: Boolean = true,
        minuteMs: Long = base,
        lat: Double = 35.6892,
        lon: Double = 51.3890,
        elev: Double = 1100.0
    ) = LiveSkyFrameRequest(visible, minuteMs, lat, lon, elev)

    @Test
    fun liveModeFloorsTheWallClockToTheQuantum() {
        val wall = base + 42_000L
        val resolved = LiveSkyFrameRequest.resolveMinuteMs(isSimulation = false, simulationTimeMs = 0L, wallClockMs = wall)
        assertEquals(SkySceneModel.quantizeTimeMs(wall), resolved)
        assertEquals(base, resolved)
        assertEquals(0L, resolved % SkySceneModel.TIME_QUANTUM_MS)
    }

    @Test
    fun simulationModeUsesTheTimeMachineInstantNotTheWallClock() {
        val simulated = 5L * 60 * minute + 30_000L
        val resolved = LiveSkyFrameRequest.resolveMinuteMs(isSimulation = true, simulationTimeMs = simulated, wallClockMs = 9_999_999_999L)
        assertEquals(SkySceneModel.quantizeTimeMs(simulated), resolved)
    }

    @Test
    fun clockTicksInsideOneMinuteProduceAnEqualRequest() {
        // Equal requests do not restart the producer, so there is no recomputation inside one minute.
        val first = request(minuteMs = LiveSkyFrameRequest.resolveMinuteMs(false, 0L, base + 1_000L))
        val tick = request(minuteMs = LiveSkyFrameRequest.resolveMinuteMs(false, 0L, base + 59_000L))
        assertEquals(first, tick)
    }

    @Test
    fun crossingAMinuteBoundaryProducesANewRequest() {
        val before = request(minuteMs = LiveSkyFrameRequest.resolveMinuteMs(false, 0L, base + 59_000L))
        val after = request(minuteMs = LiveSkyFrameRequest.resolveMinuteMs(false, 0L, base + 60_000L))
        assertNotEquals(before, after)
    }

    @Test
    fun observerChangesProduceANewRequest() {
        assertNotEquals(request(), request(lat = 51.5074))
        assertNotEquals(request(), request(lon = 0.1278))
        assertNotEquals(request(), request(elev = 5.0))
    }

    @Test
    fun onlyVisibleRequestsNeedAComputation() {
        assertTrue(request(visible = true).needsCompute)
        assertFalse(request(visible = false).needsCompute)
    }
}
