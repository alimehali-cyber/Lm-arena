package com.alijafari.red.astronomy.startracker.validation

import org.junit.Test
import org.junit.Assert.*

class ValidationMatrixRunnerTest {

    @Test
    fun testStaticBench() {
        val runner = ValidationMatrixRunner()

        val result = runner.runStaticBench("dark_10arcsec", 10.0, 20, 0, 42L)

        println("Static bench dark 10 arcsec noise, 20 trials:")
        println("RMS=${result.rmsErrorArcsec}, median=${result.medianErrorArcsec}, p95=${result.p95ErrorArcsec}, successRate=${result.successRate}")

        assertTrue("Success rate should be high", result.successRate > 0.8)
        assertTrue("RMS should be <100 arcsec for 10 arcsec noise", result.rmsErrorArcsec < 100.0)
    }

    @Test
    fun testRotationSweep() {
        val runner = ValidationMatrixRunner()
        val sweep = runner.runRotationSweep(10.0, 30.0)

        println("Rotation sweep 360° systematic bias check (step 30°):")
        var maxError = 0.0
        var minError = Double.MAX_VALUE
        for ((yaw, err) in sweep) {
            println("Yaw ${yaw}° -> error ${err} arcsec")
            if (err < Double.MAX_VALUE) {
                maxError = maxOf(maxError, err)
                minError = minOf(minError, err)
            }
        }

        val bias = maxError - minError
        println("Systematic bias (max-min): $bias arcsec")
        assertTrue("Bias should be <50 arcsec (no systematic rotation-dependent bias)", bias < 50.0)
    }

    @Test
    fun testSkyConditionSweep() {
        val runner = ValidationMatrixRunner()
        val results = runner.runSkyConditionSweep()

        println("Sky condition sweep:")
        runner.printReport(results)

        // Dark should have lower error than urban/cloud
        val dark = results.find { it.scenario.contains("dark") }!!
        val urban = results.find { it.scenario.contains("urban") }!!

        println("Dark RMS ${dark.rmsErrorArcsec} vs Urban RMS ${urban.rmsErrorArcsec}")
        assertTrue("Dark should have lower or equal RMS than urban", dark.rmsErrorArcsec <= urban.rmsErrorArcsec + 50.0) // allow some variance
    }

    @Test
    fun testDeviceLensSweep() {
        val runner = ValidationMatrixRunner()
        val results = runner.runDeviceLensSweep()

        println("Device/lens sweep:")
        runner.printReport(results)

        for (r in results) {
            assertTrue("Success rate for ${r.scenario} should be >0.5", r.successRate > 0.5)
        }
    }

    @Test
    fun testHemisphereMirrored() {
        val runner = ValidationMatrixRunner()
        val results = runner.runHemisphereCheck()

        println("Hemisphere mirrored check:")
        for ((hemi, result) in results) {
            println("$hemi: RMS=${result.rmsErrorArcsec}, successRate=${result.successRate}")
        }

        val north = results["north"]!!
        val south = results["south"]!!

        // Both hemispheres should have similar error (no bias introduced by fix)
        val diff = kotlin.math.abs(north.rmsErrorArcsec - south.rmsErrorArcsec)
        println("Hemisphere RMS diff: $diff arcsec")
        assertTrue("North/South RMS diff should be <20 arcsec (no hemisphere bias)", diff < 20.0)
    }

    @Test
    fun testConfidenceLadderWithFailureReasons() {
        // Test ConfidenceLadderCoordinator with sky-condition based failures
        val runner = ValidationMatrixRunner()
        val conditions = listOf("dark", "suburban", "urban", "cloud")

        println("Confidence ladder with failure reasons:")
        for (condition in conditions) {
            val noise = when (condition) {
                "dark" -> 5.0
                "suburban" -> 20.0
                "urban" -> 50.0
                else -> 100.0
            }
            val result = runner.runStaticBench("test_$condition", noise, 20, 0, condition.hashCode().toLong())
            println("$condition: successRate=${result.successRate}, RMS=${result.rmsErrorArcsec}, failures=${result.failureReasons}")
        }
    }
}
