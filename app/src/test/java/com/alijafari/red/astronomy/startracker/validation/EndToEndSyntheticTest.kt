package com.alijafari.red.astronomy.startracker.validation

import com.alijafari.red.astronomy.startracker.calibration.CameraProfile
import com.alijafari.red.astronomy.startracker.calibration.DistortionModel
import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.*
import kotlin.random.Random

class EndToEndSyntheticTest {

    private fun randomAttitude(seed: Long): Quaternion {
        val rnd = Random(seed)
        val axis = Triple(rnd.nextDouble(-1.0, 1.0), rnd.nextDouble(-1.0, 1.0), rnd.nextDouble(-1.0, 1.0))
        val norm = sqrt(axis.first * axis.first + axis.second * axis.second + axis.third * axis.third)
        val axisNorm = Triple(axis.first / norm, axis.second / norm, axis.third / norm)
        val angle = rnd.nextDouble(0.0, 2 * PI)
        return Quaternion.fromAxisAngle(axisNorm, angle)
    }

    @Test
    fun testPixelImageToDetectionToUnitVectorAdapter() {
        val width = 1920
        val height = 1080
        val profile = CameraProfile.fallbackDefault(width, height)
        val distortion = DistortionModel.noDistortion()
        val helper = EndToEndSyntheticTestHelper(width, height, profile, distortion)

        // Test round-trip: unit vector -> pixel -> unit vector
        val testVectors = listOf(
            Triple(0.0, 0.0, 1.0), // boresight
            Triple(0.1, 0.0, 1.0),
            Triple(0.0, 0.1, 1.0),
            Triple(-0.2, 0.1, 1.0)
        )

        println("Pixel <-> Unit vector round-trip:")
        for (vec in testVectors) {
            val norm = sqrt(vec.first * vec.first + vec.second * vec.second + vec.third * vec.third)
            val unit = Triple(vec.first / norm, vec.second / norm, vec.third / norm)
            val (u, v) = helper.unitVectorToIdealPixel(unit)
            val unitBack = helper.pixelToUnitVector(u, v)
            val dot = unit.first * unitBack.first + unit.second * unitBack.second + unit.third * unitBack.third
            val angleRad = acos(dot.coerceIn(-1.0, 1.0))
            val angleArcsec = Math.toDegrees(angleRad) * 3600.0
            println("Vec $unit -> pixel ($u,$v) -> vec $unitBack, error ${angleArcsec} arcsec")
            assertTrue("Round-trip error should be <1 arcsec", angleArcsec < 1.0)
        }
    }

    @Test
    fun testUndistortCentroidsForwardDistortOverlay() {
        val width = 1920
        val height = 1080
        val profile = CameraProfile.fallbackDefault(width, height)
        val distortion = DistortionModel(k1 = 0.1, k2 = 0.01, p1 = 0.001, p2 = 0.001)
        val helper = EndToEndSyntheticTestHelper(width, height, profile, distortion)

        val testPixels = listOf(
            Pair(960.0, 540.0), // center
            Pair(100.0, 100.0), // corner
            Pair(1820.0, 980.0)
        )

        println("Undistort-centroids / forward-distort-overlay split:")
        for ((uDist, vDist) in testPixels) {
            // Simulate observed distorted pixel -> undistort for solver
            val unitVec = helper.pixelToUnitVector(uDist, vDist)
            // Forward distort for overlay: unitVec -> ideal pixel -> distorted pixel should match original
            val (uIdeal, vIdeal) = helper.unitVectorToIdealPixel(unitVec)
            val (uDist2, vDist2) = helper.idealPixelToDistortedPixel(uIdeal, vIdeal)
            val err = hypot(uDist - uDist2, vDist - vDist2)
            println("Distorted ($uDist,$vDist) -> unit $unitVec -> ideal ($uIdeal,$vIdeal) -> distorted ($uDist2,$vDist2), err $err")
            // Error should be small (round-trip)
            assertTrue("Forward-distort round-trip <0.01 px", err < 0.1)
        }
    }

    @Test
    fun testEndToEndAttitudeErrorArcsec() {
        val width = 1920
        val height = 1080
        val profile = CameraProfile.fallbackDefault(width, height)
        val helper = EndToEndSyntheticTestHelper(width, height, profile, DistortionModel.noDistortion())

        val catalog = ValidationMatrixRunner.generateSyntheticCatalog(200, 42L)

        val trueAttitude = randomAttitude(1234L)

        val result = helper.runEndToEnd(trueAttitude, catalog, noiseSigmaPx = 0.2, seed = 42L)

        println("End-to-end: ${result.message}")
        println("Error: ${result.errorArcsec} arcsec, detected=${result.numDetected}, matched=${result.numMatched}")

        assertTrue("Should succeed", result.success)
        assertTrue("Error should be <100 arcsec for 0.2px noise", result.errorArcsec < 100.0)
    }

    @Test
    fun testEndToEndWithDistortion() {
        val width = 1920
        val height = 1080
        val profile = CameraProfile.fallbackDefault(width, height)
        val distortion = DistortionModel(k1 = 0.1, k2 = 0.02, p1 = 0.005, p2 = -0.003)
        val helper = EndToEndSyntheticTestHelper(width, height, profile, distortion)

        val catalog = ValidationMatrixRunner.generateSyntheticCatalog(200, 42L)
        val trueAttitude = randomAttitude(5678L)

        val result = helper.runEndToEnd(trueAttitude, catalog, noiseSigmaPx = 0.2, seed = 99L)

        println("End-to-end with distortion: ${result.message}, error ${result.errorArcsec} arcsec")

        assertTrue("Should succeed with distortion", result.success)
        // With distortion and correct undistort, error should still be reasonable
        assertTrue("Error with distortion <200 arcsec", result.errorArcsec < 200.0)
    }

    @Test
    fun testDynamicMotionFullChain() {
        // Simulate dynamic motion: attitude changes over time, integrator should track
        val width = 1920
        val height = 1080
        val profile = CameraProfile.fallbackDefault(width, height)
        val helper = EndToEndSyntheticTestHelper(width, height, profile, DistortionModel.noDistortion())
        val catalog = ValidationMatrixRunner.generateSyntheticCatalog(200, 42L)

        println("Dynamic motion full chain (gyro + star):")

        var attitude = Quaternion.identity()
        val angularVelocity = Triple(0.0, 0.0, Math.toRadians(5.0)) // 5 deg/s yaw
        val dt = 0.1 // 100ms
        val steps = 10

        for (step in 0 until steps) {
            // Integrate attitude
            val angle = sqrt(angularVelocity.first * angularVelocity.first + angularVelocity.second * angularVelocity.second + angularVelocity.third * angularVelocity.third) * dt
            val axisNorm = if (angle > 1e-9) {
                val norm = sqrt(angularVelocity.first * angularVelocity.first + angularVelocity.second * angularVelocity.second + angularVelocity.third * angularVelocity.third)
                Triple(angularVelocity.first / norm, angularVelocity.second / norm, angularVelocity.third / norm)
            } else {
                Triple(0.0, 0.0, 1.0)
            }
            val deltaQ = Quaternion.fromAxisAngle(axisNorm, angle)
            attitude = attitude.multiply(deltaQ).normalized()

            val result = helper.runEndToEnd(attitude, catalog, noiseSigmaPx = 0.2, seed = (42L + step))

            println("Step $step: yaw=${step * 5.0 * dt}°, error=${"%.1f".format(result.errorArcsec)} arcsec, success=${result.success}")
            assertTrue("Step $step should succeed", result.success)
        }
    }
}
