package com.alijafari.red.astronomy.startracker.calibration

import org.junit.Test
import org.junit.Assert.*
import kotlin.random.Random

/**
 * SelfCalibrationEngine accumulation lifecycle tests (audit finding B8):
 * buffers must clear after a successful refine so repeated selfCalibrate()
 * cycles never re-merge the same data.
 */
class SelfCalibrationEngineTest {

    private fun syntheticIntrinsicsBatch(
        trueFx: Double, trueCx: Double, count: Int, noisePx: Double, seed: Long
    ): List<ObservationPair> {
        val rng = Random(seed)
        return List(count) {
            val x = rng.nextDouble(-0.8, 0.8)
            val y = rng.nextDouble(-0.8, 0.8)
            val uPred = trueFx * x + trueCx
            val vPred = trueFx * y + 540.0
            val n = { if (noisePx > 0) rng.nextDouble(-noisePx, noisePx) else 0.0 }
            ObservationPair(
                predictedIdealPixel = Pair(uPred, vPred),
                observedPixel = Pair(uPred + n(), vPred + n()),
                idealNormalized = Pair(x, y)
            )
        }
    }

    @Test
    fun testBuffersClearAfterSuccessfulRefineAcrossCycles() {
        val engine = SelfCalibrationEngine(
            cache = InMemoryCameraProfileCache(),
            minSamplesForIntrinsics = 20,
            minSamplesForDistortion = 50,
            deviceLensKey = "B8TEST"
        )
        val fallback = CameraProfile.fallbackDefault(1920, 1080).copy(deviceLensKey = "B8TEST")

        // Cycle 1: accumulate 30 intrinsics observations, refine -> success, buffer must clear to 0
        engine.accumulateIntrinsicsBatch(syntheticIntrinsicsBatch(1000.0, 960.0, 30, 0.0, 1L))
        assertEquals(Pair(30, 0), engine.getAccumulatedCounts())

        val afterFirst = engine.selfCalibrate(fallback)
        assertTrue("first refine must succeed (zero noise, well-distributed)",
            afterFirst.fx != fallback.fx) // refined away from fallback
        assertEquals("intrinsics buffer must be EMPTY after successful refine (audit B8)",
            Pair(0, 0), engine.getAccumulatedCounts())

        // Cycle 2 with NO new data: selfCalibrate must be a no-op (buffers empty),
        // not a re-refinement/re-merge of cycle 1's data.
        val cachedAfterCycle1 = (engine.getCachedProfile()!!.sampleCount)
        val afterSecond = engine.selfCalibrate(afterFirst)
        assertEquals("second cycle with no new data must not re-accumulate",
            Pair(0, 0), engine.getAccumulatedCounts())
        assertEquals("second cycle must return the same profile (nothing new to refine)",
            afterFirst.fx, afterSecond.fx, 1e-12)
        assertEquals("cache sample count must not grow without new data (audit B8: was re-merging same batch)",
            cachedAfterCycle1, engine.getCachedProfile()!!.sampleCount)

        // Cycle 3: a genuinely NEW batch accumulates fresh counts only
        engine.accumulateIntrinsicsBatch(syntheticIntrinsicsBatch(1000.0, 960.0, 25, 0.0, 2L))
        assertEquals(Pair(25, 0), engine.getAccumulatedCounts())
        engine.selfCalibrate(afterSecond)
        assertEquals(Pair(0, 0), engine.getAccumulatedCounts())
    }

    @Test
    fun testBuffersRetainedWhenRefineDeclined() {
        // Below the min-sample gate nothing should be cleared or merged
        val engine = SelfCalibrationEngine(
            cache = InMemoryCameraProfileCache(),
            minSamplesForIntrinsics = 20,
            minSamplesForDistortion = 50,
            deviceLensKey = "B8TEST2"
        )
        val fallback = CameraProfile.fallbackDefault(1920, 1080).copy(deviceLensKey = "B8TEST2")

        engine.accumulateIntrinsicsBatch(syntheticIntrinsicsBatch(1000.0, 960.0, 5, 0.0, 3L))
        val out = engine.selfCalibrate(fallback)
        assertEquals("declined refine returns current profile unchanged", fallback.fx, out.fx, 1e-12)
        assertEquals("buffer must be RETAINED when gate not met (waiting for more samples)",
            Pair(5, 0), engine.getAccumulatedCounts())
        assertNull("cache must be untouched when gate not met", engine.getCachedProfile())
    }
}
