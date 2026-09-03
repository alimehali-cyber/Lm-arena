package com.alijafari.red.astronomy.startracker.calibration

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*
import kotlin.random.Random

class DistortionRefinerTest {

    private fun generateDistortionObservations(
        trueModel: DistortionModel,
        count: Int,
        noiseNorm: Double,
        seed: Long = 42L,
        range: Double = 0.8,
        centerBias: Boolean = false
    ): List<DistortionObservation> {
        val rng = Random(seed)
        val obs = mutableListOf<DistortionObservation>()

        for (i in 0 until count) {
            val xIdeal = if (centerBias) {
                rng.nextDouble(-0.1, 0.1)
            } else {
                rng.nextDouble(-range, range)
            }
            val yIdeal = if (centerBias) {
                rng.nextDouble(-0.1, 0.1)
            } else {
                rng.nextDouble(-range, range)
            }

            val (xDist, yDist) = trueModel.distortIdealToDistortedNormalized(xIdeal, yIdeal)

            // Guard: nextDouble(a, a) throws IllegalArgumentException; zero-noise case stays clean
            val xDistNoisy = xDist + (if (noiseNorm > 0.0) rng.nextDouble(-noiseNorm, noiseNorm) else 0.0)
            val yDistNoisy = yDist + (if (noiseNorm > 0.0) rng.nextDouble(-noiseNorm, noiseNorm) else 0.0)

            obs.add(
                DistortionObservation(
                    idealNormalized = Pair(xIdeal, yIdeal),
                    distortedNormalized = Pair(xDistNoisy, yDistNoisy)
                )
            )
        }

        return obs
    }

    @Test
    fun testRecoveryPerfectNoNoise() {
        val trueModel = DistortionModel(k1 = 0.1, k2 = 0.02, p1 = 0.005, p2 = -0.003)
        val initialModel = DistortionModel.noDistortion()

        val observations = generateDistortionObservations(trueModel, 100, 0.0)

        val refiner = DistortionRefiner()
        val result = refiner.refine(initialModel, observations)

        println("Distortion perfect recovery: ${result.message}")
        println("True: k1=${trueModel.k1}, k2=${trueModel.k2}, p1=${trueModel.p1}, p2=${trueModel.p2}")
        println("Refined: k1=${result.refinedModel.k1}, k2=${result.refinedModel.k2}, p1=${result.refinedModel.p1}, p2=${result.refinedModel.p2}")

        assertTrue("Should succeed", result.success)
        assertEquals(trueModel.k1, result.refinedModel.k1, 1e-6)
        assertEquals(trueModel.k2, result.refinedModel.k2, 1e-6)
        assertEquals(trueModel.p1, result.refinedModel.p1, 1e-6)
        assertEquals(trueModel.p2, result.refinedModel.p2, 1e-6)
    }

    @Test
    fun testSweepObsCountAndNoise() {
        val trueModel = DistortionModel(k1 = 0.1, k2 = 0.02, p1 = 0.005, p2 = -0.003)
        val initialModel = DistortionModel.noDistortion()

        val obsCounts = listOf(10, 30, 100)
        val noiseLevels = listOf(0.0, 0.001, 0.005)

        println("\nDistortion refinement sweep:")
        println("Noise\\Obs | 10 | 30 | 100")
        for (noise in noiseLevels) {
            val row = StringBuilder("${noise} |")
            for (obsCount in obsCounts) {
                val observations = generateDistortionObservations(
                    trueModel, obsCount, noise,
                    seed = (noise * 1000 + obsCount).toLong()
                )
                val refiner = DistortionRefiner()
                val result = refiner.refine(initialModel, observations)

                if (!result.success) {
                    row.append(" FAIL |")
                } else {
                    val errK1 = abs(result.refinedModel.k1 - trueModel.k1)
                    val errK2 = abs(result.refinedModel.k2 - trueModel.k2)
                    row.append(" k1Err=${"%.4f".format(errK1)} k2Err=${"%.4f".format(errK2)} rms=${"%.5f".format(result.rmsError)} |")
                }
            }
            println(row.toString())
        }
    }

    @Test
    fun testDeclineClusteredNearCenter() {
        val trueModel = DistortionModel(k1 = 0.1, k2 = 0.02, p1 = 0.005, p2 = -0.003)
        val initialModel = DistortionModel.noDistortion()

        // All observations near center where distortion near-zero
        val observations = generateDistortionObservations(
            trueModel, 30, 0.0, centerBias = true
        )

        val refiner = DistortionRefiner()
        val result = refiner.refine(initialModel, observations)

        println("Clustered near center test: ${result.message}")
        assertFalse("Should decline when clustered near center", result.success)
    }

    @Test
    fun testInsufficientData() {
        val trueModel = DistortionModel(k1 = 0.1, k2 = 0.02, p1 = 0.005, p2 = -0.003)
        val initialModel = DistortionModel.noDistortion()

        val observations = generateDistortionObservations(trueModel, 3, 0.0)

        val refiner = DistortionRefiner()
        val result = refiner.refine(initialModel, observations)

        println("Insufficient data distortion test: ${result.message}")
        assertFalse("Should decline with insufficient data", result.success)
    }
}
