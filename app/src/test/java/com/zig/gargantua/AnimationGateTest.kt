package com.zig.gargantua

import com.zig.gargantua.renderer.AnimationGate
import com.zig.gargantua.renderer.GargantuaAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationGateTest {
    private fun input(
        resourcesReady: Boolean,
        cacheValid: Boolean,
        sceneDirty: Boolean,
        signatureChanged: Boolean,
        now: Long,
        lastChange: Long,
        amplitude: Int = 15
    ) = AnimationGate.Input(
        animationRequested = true,
        resourcesReady = resourcesReady,
        cacheValid = cacheValid,
        sceneDirty = sceneDirty,
        signatureChangedThisFrame = signatureChanged,
        nowNanos = now,
        lastChangeNanos = lastChange,
        amplitudePercent = amplitude
    )

    private fun assertRebuildThenModulate(gate: AnimationGate) {
        assertEquals(
            AnimationGate.Action.REBUILD,
            gate.decide(input(true, false, true, true, 1_000_000_000L, 1_000_000_000L)).action
        )
        assertEquals(
            AnimationGate.Action.MODULATE,
            gate.decide(input(true, true, false, false, 1_400_000_000L, 1_000_000_000L)).action
        )
    }

    @Test
    fun animationSpeedPeriodsAreNonPersistedSlowNormalFastValues() {
        assertEquals(12.0, GargantuaAnimation.AnimationSpeed.SLOW.periodSeconds, 0.0)
        assertEquals(6.0, GargantuaAnimation.AnimationSpeed.NORMAL.periodSeconds, 0.0)
        assertEquals(3.5, GargantuaAnimation.AnimationSpeed.FAST.periodSeconds, 0.0)
    }

    @Test
    fun flowMapWeightsSumToOne() {
        listOf(0.0, 1.0, 6.0, 12.0, 23.999, 48.0).forEach { seconds ->
            val times = GargantuaAnimation.flowMapTimes(seconds)
            assertEquals(1.0, times.blendA + times.blendB, 1.0e-12)
        }
    }

    @Test
    fun flowMapWeightAIsZeroAtBothCycleEdges() {
        assertEquals(0.0, GargantuaAnimation.flowMapTimes(0.0).blendA, 1.0e-12)
        assertEquals(0.0, GargantuaAnimation.flowMapTimes(GargantuaAnimation.FLOW_MAP_PERIOD_SECONDS).blendA, 1.0e-12)
    }

    @Test
    fun flowMapWrapIsContinuous() {
        val period = GargantuaAnimation.FLOW_MAP_PERIOD_SECONDS
        val before = GargantuaAnimation.flowMapTimes(period - 1.0e-9)
        val after = GargantuaAnimation.flowMapTimes(period + 1.0e-9)
        assertEquals(period, before.timeA + after.timeA, 1.0e-6)
        assertTrue(kotlin.math.abs(before.timeB - after.timeB) < 1.0e-6)
        assertTrue(kotlin.math.abs(before.blendA - after.blendA) < 1.0e-6)
    }

    @Test
    fun flowMapBIsHalfPeriodFromA() {
        val period = GargantuaAnimation.FLOW_MAP_PERIOD_SECONDS
        listOf(0.0, 2.5, 12.0, 23.5, 49.0).forEach { seconds ->
            val times = GargantuaAnimation.flowMapTimes(seconds)
            val expectedB = ((times.timeA + period * 0.5) % period + period) % period
            assertEquals(expectedB, times.timeB, 1.0e-12)
        }
    }

    @Test
    fun dragThenReleaseEndsInModulateWithoutAnotherCameraChange() {
        assertRebuildThenModulate(AnimationGate())
    }

    @Test
    fun backgroundResumeEndsInModulateWithoutAnotherCameraChange() {
        val gate = AnimationGate()
        assertEquals(AnimationGate.Action.REBUILD, gate.decide(input(true, false, true, false, 2_000_000_000L, 2_000_000_000L)).action)
        assertEquals(AnimationGate.Action.MODULATE, gate.decide(input(true, true, false, false, 2_400_000_000L, 2_000_000_000L)).action)
    }

    @Test
    fun resumeCacheInvalidationRequiresRebuildThenSettlesToModulation() {
        val gate = AnimationGate()
        val afterResume = input(true, false, true, false, 5_000_000_000L, 5_000_000_000L)
        assertEquals(AnimationGate.Action.REBUILD, gate.decide(afterResume).action)
        assertEquals(
            AnimationGate.Action.MODULATE,
            gate.decide(afterResume.copy(cacheValid = true, sceneDirty = false, nowNanos = 5_300_000_000L)).action
        )
    }

    @Test
    fun fboDeletionCacheInvalidationRequiresRebuild() {
        val decision = AnimationGate().decide(
            input(true, cacheValid = false, sceneDirty = true, signatureChanged = false, now = 7_000_000_000L, lastChange = 6_600_000_000L)
        )
        assertEquals(AnimationGate.Action.REBUILD, decision.action)
    }

    @Test
    fun resizeEndsInModulateWithoutAnotherCameraChange() {
        assertRebuildThenModulate(AnimationGate())
    }

    @Test
    fun zeroAmplitudeIsAPlainStrictBypassEvenWhenSceneIsDirty() {
        val decision = AnimationGate().decide(
            input(
                resourcesReady = true,
                cacheValid = false,
                sceneDirty = true,
                signatureChanged = true,
                now = 1_400_000_000L,
                lastChange = 1_000_000_000L,
                amplitude = 0
            )
        )
        assertEquals(AnimationGate.Action.PLAIN, decision.action)
        assertEquals(AnimationGate.State.PLAIN, decision.state)
    }

    @Test
    fun transientNotReadyDoesNotConsumeRebuild() {
        val gate = AnimationGate()
        assertEquals(AnimationGate.Action.PLAIN, gate.decide(input(false, false, true, true, 1_000_000_000L, 1_000_000_000L)).action)
        assertEquals(AnimationGate.Action.REBUILD, gate.decide(input(true, false, true, true, 1_100_000_000L, 1_000_000_000L)).action)
        assertEquals(AnimationGate.Action.MODULATE, gate.decide(input(true, true, false, false, 1_500_000_000L, 1_000_000_000L)).action)
    }

    @Test
    fun signatureChangeInvalidatesOtherwiseValidCache() {
        val decision = AnimationGate().decide(
            input(
                resourcesReady = true,
                cacheValid = true,
                sceneDirty = false,
                signatureChanged = true,
                now = 8_000_000_000L,
                lastChange = 7_500_000_000L
            )
        )
        assertEquals(AnimationGate.Action.REBUILD, decision.action)
    }

    @Test
    fun coarseModeChangeEndsInModulateWithoutAnotherCameraChange() {
        assertRebuildThenModulate(AnimationGate())
    }

    @Test
    fun animationOffThenOnEndsInModulateWithoutAnotherCameraChange() {
        val gate = AnimationGate()
        assertEquals(
            AnimationGate.Action.PLAIN,
            gate.decide(AnimationGate.Input(false, true, true, true, true, 1_000_000_000L, 1_000_000_000L, 15)).action
        )
        assertRebuildThenModulate(gate)
    }
}
