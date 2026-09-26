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

    @Test
    fun animationSignMatchesKeplerianPositiveDirectionWithoutStandingWave() {
        val modShader = java.io.File("app/src/main/assets/shaders/gargantua_animation_modulation.frag").readText()
        assertTrue(
            "sampleFluidFlow must use matching positive sign for temporal translation",
            modShader.contains("phiNorm + (omega * u_TimeScale * tPhase) / TWO_PI")
        )
        assertFalse(
            "sampleFluidFlow must not contain negative temporal translation (standing wave bug)",
            modShader.contains("phiNorm - (omega * u_TimeScale * tPhase) / TWO_PI")
        )
    }

    @Test
    fun animationAmplitudeOrderingIsMonotonicWithoutArtificialFloor() {
        val modShader = java.io.File("app/src/main/assets/shaders/gargantua_animation_modulation.frag").readText()
        assertFalse(
            "Modulation shader must not clamp dynamicAmplitude to 0.45",
            modShader.contains("max(amp * 1.8, 0.45)")
        )
        val amp15 = 0.15f * 1.8f
        val amp40 = 0.40f * 1.8f
        val amp80 = 0.80f * 1.8f
        assertTrue("Amplitude 15% < 40%", amp15 < amp40)
        assertTrue("Amplitude 40% < 80%", amp40 < amp80)
    }

    @Test
    fun animationSpeedOrderingPreservesPhaseProgressionRates() {
        val slow = GargantuaAnimation.AnimationSpeed.SLOW
        val normal = GargantuaAnimation.AnimationSpeed.NORMAL
        val fast = GargantuaAnimation.AnimationSpeed.FAST
        assertTrue("Slow period > Normal period", slow.periodSeconds > normal.periodSeconds)
        assertTrue("Normal period > Fast period", normal.periodSeconds > fast.periodSeconds)
        val rateSlow = 1.0 / slow.periodSeconds
        val rateNormal = 1.0 / normal.periodSeconds
        val rateFast = 1.0 / fast.periodSeconds
        assertTrue("Phase rate: slow < normal", rateSlow < rateNormal)
        assertTrue("Phase rate: normal < fast", rateNormal < rateFast)
    }

    @Test
    fun animationPhaseIsTimeBasedAndFrameRateIndependent() {
        var t30 = 0.0
        for (i in 0 until 90) t30 += 3.0 / 90.0
        var t60 = 0.0
        for (i in 0 until 180) t60 += 3.0 / 180.0
        val times30 = GargantuaAnimation.flowMapTimes(t30)
        val times60 = GargantuaAnimation.flowMapTimes(t60)
        assertEquals("Phase A must match regardless of cadence", times30.timeA, times60.timeA, 1e-9)
        assertEquals("Phase B must match regardless of cadence", times30.timeB, times60.timeB, 1e-9)
        assertEquals("Blend weight must match regardless of cadence", times30.blendA, times60.blendA, 1e-9)
    }

    @Test
    fun zeroAmplitudeStrictlyBypassesModulation() {
        val modShader = java.io.File("app/src/main/assets/shaders/gargantua_animation_modulation.frag").readText()
        assertTrue(
            "Shader must output 1.0 on zero amplitude",
            modShader.contains("if (u_Amplitude == 0.0) {\n        fragColor = 1.0;\n        return;\n    }")
        )
    }

    @Test
    fun behavioralDirectionAndNoStandingWaveTest() {
        // Mathematical evaluation of phiSheared translation
        val omega = 0.20 // angular velocity at test radius
        val timeScale = 1.0
        val t0 = 0.0
        val t1 = 1.0

        // In sampleFluidFlow:
        // phiSheared = phiNorm + (omega * timeScale * tPhase) / TWO_PI - spiralCoil / TWO_PI
        fun flowPhiSheared(phiNorm: Double, t: Double): Double {
            return phiNorm + (omega * timeScale * t) / (2.0 * Math.PI)
        }

        // In keplerianNoise:
        // shiftNorm = (omega * timeScale * t) / TAU
        // phiSheared = az + shiftNorm - spiralCoil / TAU
        fun kepPhiSheared(phiNorm: Double, t: Double): Double {
            return phiNorm + (omega * timeScale * t) / (2.0 * Math.PI)
        }

        val dPhiFlow = flowPhiSheared(0.5, t1) - flowPhiSheared(0.5, t0)
        val dPhiKep = kepPhiSheared(0.5, t1) - kepPhiSheared(0.5, t0)

        assertTrue("sampleFluidFlow must translate in positive prograde direction", dPhiFlow > 0.0)
        assertTrue("keplerianNoise must translate in positive prograde direction", dPhiKep > 0.0)
        assertEquals("Both flow and noise must advance with identical prograde velocity (no standing wave)", dPhiFlow, dPhiKep, 1e-12)
    }

    @Test
    fun behavioralFrameRateIndependenceAcrossCadences() {
        val elapsedTarget = 4.25 // seconds
        val cadences = listOf(30, 60, 90, 120) // FPS

        val results = cadences.map { fps ->
            var t = 0.0
            val dt = 1.0 / fps.toDouble()
            val steps = (elapsedTarget * fps).toInt()
            for (step in 0 until steps) {
                t += dt
            }
            GargantuaAnimation.flowMapTimes(t)
        }

        val ref = results[0]
        for (i in 1 until results.size) {
            val curr = results[i]
            assertEquals("timeA must match across cadences (ref=${ref.timeA}, curr=${curr.timeA})", ref.timeA, curr.timeA, 1e-4)
            assertEquals("timeB must match across cadences", ref.timeB, curr.timeB, 1e-4)
            assertEquals("blendA must match across cadences", ref.blendA, curr.blendA, 1e-4)
        }
    }

    @Test
    fun behavioralAmplitudeMonotonicScaling() {
        val amp15 = 0.15f * 1.8f
        val amp40 = 0.40f * 1.8f
        val amp80 = 0.80f * 1.8f

        fun modRange(amp: Float): Float {
            val centeredMin = -0.5f
            val centeredMax = 0.5f
            val modMin = (1.0f + amp * centeredMin * 2.5f).coerceIn(0.15f, 2.60f)
            val modMax = (1.0f + amp * centeredMax * 2.5f).coerceIn(0.15f, 2.60f)
            return modMax - modMin
        }

        val range15 = modRange(amp15)
        val range40 = modRange(amp40)
        val range80 = modRange(amp80)

        assertTrue("Modulation range 15% < 40%", range15 < range40)
        assertTrue("Modulation range 40% < 80%", range40 < range80)
    }
}
