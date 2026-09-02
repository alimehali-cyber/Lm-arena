package com.alijafari.red.astronomy.startracker.calibration

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*
import kotlin.random.Random

class IntrinsicsRefinerTest {

    private fun makeTrueProfile(width: Int = 1920, height: Int = 1080): CameraProfile {
        return CameraProfile(
            fx = 1200.0,
            fy = 1205.0,
            cx = 965.0,
            cy = 535.0,
            skew = 0.5,
            k1 = 0.0,
            k2 = 0.0,
            p1 = 0.0,
            p2 = 0.0,
            sampleCount = 0,
            deviceLensKey = "TEST"
        )
    }

    private fun generateObservations(
        trueProfile: CameraProfile,
        count: Int,
        noisePx: Double,
        seed: Long = 42L
    ): List<ObservationPair> {
        val rng = Random(seed)
        val observations = mutableListOf<ObservationPair>()

        for (i in 0 until count) {
            // Random ideal normalized point in [-0.8, 0.8]
            val xIdeal = rng.nextDouble(-0.8, 0.8)
            val yIdeal = rng.nextDouble(-0.8, 0.8)

            val uPred = trueProfile.fx * xIdeal + trueProfile.skew * yIdeal + trueProfile.cx
            val vPred = trueProfile.fy * yIdeal + trueProfile.cy

            val uObs = uPred + rng.nextDouble(-noisePx, noisePx)
            val vObs = vPred + rng.nextDouble(-noisePx, noisePx)

            observations.add(
                ObservationPair(
                    predictedIdealPixel = Pair(uPred, vPred),
                    observedPixel = Pair(uObs, vObs),
                    idealNormalized = Pair(xIdeal, yIdeal)
                )
            )
        }

        return observations
    }

    @Test
    fun testRecoveryPerfectNoNoise() {
        val trueProfile = makeTrueProfile()
        val initialProfile = CameraProfile.fallbackDefault(1920, 1080).copy(
            deviceLensKey = "TEST"
        )

        val observations = generateObservations(trueProfile, 100, 0.0)

        val refiner = IntrinsicsRefiner()
        val result = refiner.refine(initialProfile, observations)

        println("Perfect recovery: ${result.message}, RMS ${result.rmsError}")
        println("True: fx=${trueProfile.fx}, fy=${trueProfile.fy}, cx=${trueProfile.cx}, cy=${trueProfile.cy}, skew=${trueProfile.skew}")
        println("Refined: fx=${result.refinedProfile.fx}, fy=${result.refinedProfile.fy}, cx=${result.refinedProfile.cx}, cy=${result.refinedProfile.cy}, skew=${result.refinedProfile.skew}")

        assertTrue("Should succeed", result.success)
        assertEquals(trueProfile.fx, result.refinedProfile.fx, 0.1)
        assertEquals(trueProfile.fy, result.refinedProfile.fy, 0.1)
        assertEquals(trueProfile.cx, result.refinedProfile.cx, 0.1)
        assertEquals(trueProfile.cy, result.refinedProfile.cy, 0.1)
        assertTrue("RMS should be near zero", result.rmsError < 0.01)
    }

    @Test
    fun testSweepObsCountAndNoise() {
        val trueProfile = makeTrueProfile()
        val initialProfile = CameraProfile.fallbackDefault(1920, 1080).copy(deviceLensKey = "TEST")

        val obsCounts = listOf(4, 10, 30, 100)
        val noiseLevels = listOf(0.0, 0.2, 0.5, 1.0)

        println("\nIntrinsics refinement sweep: noise vs obs count")
        println("Noise\\Obs | 4 | 10 | 30 | 100 | note")
        println("---|---|---|---|---|---")

        for (noise in noiseLevels) {
            val row = StringBuilder("${noise}px |")
            for (obsCount in obsCounts) {
                val observations = generateObservations(trueProfile, obsCount, noise, seed = (noise * 100 + obsCount).toLong())
                val refiner = IntrinsicsRefiner()
                val result = refiner.refine(initialProfile, observations)

                if (!result.success) {
                    row.append(" FAIL |")
                } else {
                    val errFx = abs(result.refinedProfile.fx - trueProfile.fx)
                    val errFy = abs(result.refinedProfile.fy - trueProfile.fy)
                    val errCx = abs(result.refinedProfile.cx - trueProfile.cx)
                    val errCy = abs(result.refinedProfile.cy - trueProfile.cy)
                    val rms = result.rmsError
                    row.append(" rms=${"%.2f".format(rms)} errFx=${"%.1f".format(errFx)} |")
                }
            }
            println(row.toString())
        }
    }

    @Test
    fun testDegenerateInsufficientData() {
        val trueProfile = makeTrueProfile()
        val initialProfile = CameraProfile.fallbackDefault(1920, 1080).copy(deviceLensKey = "TEST")

        val observations = generateObservations(trueProfile, 3, 0.0) // only 3, need 6 min

        val refiner = IntrinsicsRefiner()
        val result = refiner.refine(initialProfile, observations)

        println("Insufficient data test: ${result.message}")
        assertFalse("Should decline with insufficient data", result.success)
    }

    @Test
    fun testDegenerateClusteredData() {
        val trueProfile = makeTrueProfile()
        val initialProfile = CameraProfile.fallbackDefault(1920, 1080).copy(deviceLensKey = "TEST")

        // All points clustered near same location
        val rng = Random(42L)
        val observations = (0 until 20).map {
            val xIdeal = 0.1 + rng.nextDouble(-0.001, 0.001) // tightly clustered
            val yIdeal = 0.1 + rng.nextDouble(-0.001, 0.001)
            val uPred = trueProfile.fx * xIdeal + trueProfile.skew * yIdeal + trueProfile.cx
            val vPred = trueProfile.fy * yIdeal + trueProfile.cy
            ObservationPair(
                predictedIdealPixel = Pair(uPred, vPred),
                observedPixel = Pair(uPred, vPred),
                idealNormalized = Pair(xIdeal, yIdeal)
            )
        }

        val refiner = IntrinsicsRefiner()
        val result = refiner.refine(initialProfile, observations)

        println("Clustered data test: ${result.message}")
        assertFalse("Should decline with clustered data", result.success)
    }

    @Test
    fun testSkewRecovery() {
        val trueProfile = makeTrueProfile().copy(skew = 2.0)
        val initialProfile = CameraProfile.fallbackDefault(1920, 1080).copy(deviceLensKey = "TEST")

        val observations = generateObservations(trueProfile, 50, 0.0)
        val refiner = IntrinsicsRefiner()
        val result = refiner.refine(initialProfile, observations)

        println("Skew recovery: true skew=${trueProfile.skew}, refined=${result.refinedProfile.skew}, RMS ${result.rmsError}")
        assertTrue("Should succeed", result.success)
        assertEquals(trueProfile.skew, result.refinedProfile.skew, 0.2)
    }
}
