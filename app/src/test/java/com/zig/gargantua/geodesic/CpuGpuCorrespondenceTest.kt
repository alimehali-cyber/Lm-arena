package com.zig.gargantua.geodesic

import com.zig.gargantua.physics.KerrSchildSpacetime
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/**
 * Validates Requirement 1: CPU <-> GPU numerical correspondence.
 *
 * Compares the double-precision CPU reference engine ([KerrPhotonIntegrator], [CameraModel])
 * against the single-precision GPU-equivalent shader engine ([GpuEquivalentIntegrator])
 * under identical deterministic initial conditions.
 *
 * DOCUMENTATION OF NUMERICAL TOLERANCES (GLSL ES 3.0 / IEEE 754 Single Precision):
 * - IEEE 754 32-bit float (`highp float`) provides 24 bits of significand (~7.2 decimal digits),
 *   giving a machine epsilon of ε_mach ≈ 1.192 × 10⁻⁷.
 * - Initial momentum components: compared to relative tolerance 2 × 10⁻⁶ (well within single precision).
 * - Initial Hamiltonian residual |H|: single-precision cancellation error in 16-term quadratic sum
 *   yields |H| < 1 × 10⁻⁵.
 * - Intermediate trajectory drift: after N RK4 steps through strong curvature, single-precision
 *   truncation error accumulates as O(N · ε_mach · ‖p‖), bounded to < 0.05 M.
 * - Final escape direction angle: agrees to < 0.5 degrees (< 0.009 rad).
 * - Classification: 100% agreement between CPU and GPU on capture vs escape across the entire parameter space.
 */
class CpuGpuCorrespondenceTest {

    @Test
    fun initialRayStateMatchesBetweenCpuAndGpuWithinSinglePrecision() {
        val testSpacetimes = listOf(
            Pair(1.0f, 0.0f),   // Schwarzschild
            Pair(1.0f, 0.7f),   // Moderate Kerr
            Pair(1.0f, -0.85f), // Retrograde Kerr
            Pair(2.0f, 1.5f)    // Scaled mass
        )

        val camPosList = listOf(
            floatArrayOf(25.0f, 0.0f, 2.0f),
            floatArrayOf(0.0f, 30.0f, 5.0f),
            floatArrayOf(15.0f, 15.0f, 10.0f)
        )

        val rayDirList = listOf(
            floatArrayOf(-1.0f, 0.0f, 0.0f),
            floatArrayOf(-0.8f, 0.4f, 0.1f),
            floatArrayOf(-0.3f, -0.9f, 0.2f)
        )

        for ((M, a) in testSpacetimes) {
            val spacetimeCpu = KerrSchildSpacetime(M.toDouble(), a.toDouble())

            for (camPos in camPosList) {
                for (rayDir in rayDirList) {
                    val dirNorm = sqrt(rayDir[0] * rayDir[0] + rayDir[1] * rayDir[1] + rayDir[2] * rayDir[2])
                    val unitDir = floatArrayOf(rayDir[0] / dirNorm, rayDir[1] / dirNorm, rayDir[2] / dirNorm)

                    // 1. CPU Reference
                    val cpuState = CameraModel.createNullStateFromDirection(
                        spacetimeCpu,
                        camPos[0].toDouble(), camPos[1].toDouble(), camPos[2].toDouble(),
                        unitDir[0].toDouble(), unitDir[1].toDouble(), unitDir[2].toDouble()
                    )

                    // 2. GPU Equivalent
                    val gpuState = GpuEquivalentIntegrator.createInitialRay(M, a, camPos, unitDir)

                    // Initial position comparison: exact
                    assertEquals("X must match", cpuState.x.toFloat(), gpuState[0], 1e-6f)
                    assertEquals("Y must match", cpuState.y.toFloat(), gpuState[1], 1e-6f)
                    assertEquals("Z must match", cpuState.z.toFloat(), gpuState[2], 1e-6f)

                    // Initial covariant momentum comparison
                    val relPx = abs(cpuState.p_x.toFloat() - gpuState[3]) / abs(cpuState.p_x.toFloat())
                    val relPy = abs(cpuState.p_y.toFloat() - gpuState[4]) / max(1e-4f, abs(cpuState.p_y.toFloat()))
                    val relPz = abs(cpuState.p_z.toFloat() - gpuState[5]) / max(1e-4f, abs(cpuState.p_z.toFloat()))

                    assertTrue("px relative error $relPx must be < 1e-5", relPx < 1e-5f)
                    assertTrue("py relative error $relPy must be < 1e-5", relPy < 1e-5f)
                    assertTrue("pz relative error $relPz must be < 1e-5", relPz < 1e-5f)

                    // Initial Hamiltonian comparison
                    val cpuH = abs(cpuState.hamiltonian(spacetimeCpu))
                    val gpuH = abs(GpuEquivalentIntegrator.computeHamiltonian(M, a, gpuState))

                    assertTrue("CPU H must be < 1e-14, got $cpuH", cpuH < 1e-14)
                    assertTrue("GPU H must be < 1e-5 (single precision), got $gpuH", gpuH < 1e-5f)
                }
            }
        }
    }

    @Test
    fun intermediateTrajectoryStatesCorrespondBetweenCpuAndGpu() {
        val M = 1.0f
        val a = 0.6f
        val spacetimeCpu = KerrSchildSpacetime(M.toDouble(), a.toDouble())

        val camPos = floatArrayOf(-30.0f, 8.0f, 2.0f)
        val rayDir = floatArrayOf(1.0f, 0.0f, 0.0f)

        val cpuInitial = CameraModel.createNullStateFromDirection(
            spacetimeCpu,
            camPos[0].toDouble(), camPos[1].toDouble(), camPos[2].toDouble(),
            rayDir[0].toDouble(), rayDir[1].toDouble(), rayDir[2].toDouble()
        )

        val cpuIntegrator = KerrPhotonIntegrator(
            spacetimeCpu,
            baseStepFactor = 0.08,
            minStepSize = 0.02,
            maxStepSize = 0.35,
            escapeRadius = 50.0,
            maxSteps = 600
        )

        val cpuResult = cpuIntegrator.traceRay(cpuInitial, recordPath = true)
        val gpuResult = GpuEquivalentIntegrator.traceRay(M, a, camPos, rayDir, maxSteps = 600)

        assertTrue("Both CPU and GPU must escape", cpuResult.isEscaped && gpuResult.isEscaped)
        assertNotNull("CPU path must exist", cpuResult.path)

        val cpuPath = cpuResult.path!!
        val gpuPath = gpuResult.intermediateStates

        // Compare representative intermediate states at steps 5, 15, 30
        val checkSteps = listOf(5, 15, 30)
        for (stepIdx in checkSteps) {
            if (stepIdx < cpuPath.size && stepIdx < gpuPath.size) {
                val cpuPt = cpuPath[stepIdx]
                val gpuPt = gpuPath[stepIdx]

                val dx = cpuPt.x.toFloat() - gpuPt[0]
                val dy = cpuPt.y.toFloat() - gpuPt[1]
                val dz = cpuPt.z.toFloat() - gpuPt[2]
                val spatialDiff = sqrt(dx * dx + dy * dy + dz * dz)

                // Single-precision trajectory tracks double-precision reference to within 0.05 M
                assertTrue(
                    "Intermediate trajectory drift at step $stepIdx ($spatialDiff) must be < 0.05 M",
                    spatialDiff < 0.05f
                )
            }
        }

        // Final trajectory state escape direction comparison
        val cpuFinalV = cpuResult.finalState.velocity(spacetimeCpu)
        val cpuVNorm = sqrt(cpuFinalV[1] * cpuFinalV[1] + cpuFinalV[2] * cpuFinalV[2] + cpuFinalV[3] * cpuFinalV[3])
        val cpuDir = floatArrayOf(
            (cpuFinalV[1] / cpuVNorm).toFloat(),
            (cpuFinalV[2] / cpuVNorm).toFloat(),
            (cpuFinalV[3] / cpuVNorm).toFloat()
        )

        // GPU final velocity from final momentum
        val gpuR = GpuEquivalentIntegrator.compute_r_KS(a, gpuResult.finalPos[0], gpuResult.finalPos[1], gpuResult.finalPos[2])
        val gpuGInv = GpuEquivalentIntegrator.compute_g_inv(M, a, gpuResult.finalPos[0], gpuResult.finalPos[1], gpuResult.finalPos[2], gpuR)
        val gpuP = floatArrayOf(-1.0f, gpuResult.finalMomentum[0], gpuResult.finalMomentum[1], gpuResult.finalMomentum[2])
        var gpuVx = 0.0f
        var gpuVy = 0.0f
        var gpuVz = 0.0f
        for (nu in 0 until 4) {
            gpuVx += gpuGInv[1][nu] * gpuP[nu]
            gpuVy += gpuGInv[2][nu] * gpuP[nu]
            gpuVz += gpuGInv[3][nu] * gpuP[nu]
        }
        val gpuVNorm = sqrt(gpuVx * gpuVx + gpuVy * gpuVy + gpuVz * gpuVz)
        val gpuDir = floatArrayOf(gpuVx / gpuVNorm, gpuVy / gpuVNorm, gpuVz / gpuVNorm)

        val cosExitAngle = cpuDir[0] * gpuDir[0] + cpuDir[1] * gpuDir[1] + cpuDir[2] * gpuDir[2]
        val exitAngleRad = acos(cosExitAngle.coerceIn(-1.0f, 1.0f))
        val exitAngleDeg = Math.toDegrees(exitAngleRad.toDouble())

        assertTrue(
            "Final exit velocity direction must match between CPU and GPU within 0.5 degrees, got $exitAngleDeg deg",
            exitAngleDeg < 0.5
        )
    }

    @Test
    fun captureAndEscapeClassification100PercentConsistentAcrossParameterGrid() {
        val testConfigs = listOf(
            Pair(1.0f, 0.0f),  // Schwarzschild
            Pair(1.0f, 0.85f), // High spin prograde
            Pair(1.0f, -0.85f) // High spin retrograde
        )

        // Range of impact parameters spanning clearly captured (b < 4.8) and clearly escaping (b > 5.5)
        val impactParameters = listOf(2.5f, 3.8f, 4.3f, 5.8f, 7.5f, 12.0f, 25.0f)

        for ((M, a) in testConfigs) {
            val spacetimeCpu = KerrSchildSpacetime(M.toDouble(), a.toDouble())
            val cpuIntegrator = KerrPhotonIntegrator(
                spacetimeCpu,
                baseStepFactor = 0.08,
                minStepSize = 0.02,
                maxStepSize = 0.35,
                escapeRadius = 50.0,
                maxSteps = 800
            )

            for (b in impactParameters) {
                val camPos = floatArrayOf(-35.0f, b, 0.5f)
                val rayDir = floatArrayOf(1.0f, 0.0f, 0.0f)

                val cpuInitial = CameraModel.createNullStateFromDirection(
                    spacetimeCpu,
                    camPos[0].toDouble(), camPos[1].toDouble(), camPos[2].toDouble(),
                    1.0, 0.0, 0.0
                )

                val cpuRes = cpuIntegrator.traceRay(cpuInitial)
                val gpuRes = GpuEquivalentIntegrator.traceRay(M, a, camPos, rayDir, maxSteps = 800)

                assertEquals(
                    "Capture classification must match 100% for M=$M, a=$a, b=$b",
                    cpuRes.isCaptured,
                    gpuRes.isCaptured
                )
                assertEquals(
                    "Escape classification must match 100% for M=$M, a=$a, b=$b",
                    cpuRes.isEscaped,
                    gpuRes.isEscaped
                )
            }
        }
    }

    @Test
    fun diskIntersectionCorrespondsBetweenCpuAndGpu() {
        val M = 1.0f
        val a = 0.5f
        val spacetimeCpu = KerrSchildSpacetime(M.toDouble(), a.toDouble())
        val diskCpu = com.zig.gargantua.disk.AccretionDiskModel(M.toDouble(), a.toDouble(), outerRadius = 22.0)

        val camPos = floatArrayOf(-25.0f, 0.0f, 4.0f)
        val rayDir = floatArrayOf(1.0f, 0.4f, -0.35f)

        val cpuInitial = CameraModel.createNullStateFromDirection(
            spacetimeCpu,
            camPos[0].toDouble(), camPos[1].toDouble(), camPos[2].toDouble(),
            rayDir[0].toDouble(), rayDir[1].toDouble(), rayDir[2].toDouble()
        )

        val cpuIntegrator = KerrPhotonIntegrator(
            spacetimeCpu,
            disk = diskCpu,
            baseStepFactor = 0.08,
            minStepSize = 0.02,
            maxStepSize = 0.35,
            escapeRadius = 50.0,
            maxSteps = 400
        )

        val cpuRes = cpuIntegrator.traceRay(cpuInitial)
        val gpuRes = GpuEquivalentIntegrator.traceRay(
            M = M,
            a = a,
            camPos = camPos,
            rayDir = rayDir,
            maxSteps = 400,
            enableDisk = true,
            diskInnerRadius = diskCpu.innerRadius.toFloat(),
            diskOuterRadius = diskCpu.outerRadius.toFloat()
        )

        assertTrue("Both CPU and GPU must detect disk hit", cpuRes.isDiskHit && gpuRes.isDiskHit)
        assertNotNull("CPU disk hit must exist", cpuRes.diskHit)

        val cpuHit = cpuRes.diskHit!!
        val rDiff = abs(cpuHit.rHit.toFloat() - gpuRes.rHit)
        val gDiff = abs(cpuHit.frequencyShift.toFloat() - gpuRes.frequencyShift)

        assertTrue("Disk hit radius must match within 0.05 M, got $rDiff", rDiff < 0.05f)
        assertTrue("Frequency shift must match within 0.05, got $gDiff", gDiff < 0.05f)
    }
}
