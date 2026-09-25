package com.zig.gargantua.geodesic

import com.zig.gargantua.disk.AccretionDiskModel
import com.zig.gargantua.disk.KerrIsco
import com.zig.gargantua.physics.KerrSchildSpacetime
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/**
 * Diagnostic test quantifying ray classification across a representative viewport frame:
 * 1. Percentage of pixels captured (black hole shadow)
 * 2. Percentage of pixels escaped (asymptotic background)
 * 3. Percentage of pixels unresolved (must be 0.0%)
 * 4. Percentage of pixels with disk emission
 * 5. Percentage of background pixels
 * 6. Number / percentage of unresolved pixels near the visible ring (must be 0)
 */
class RayClassificationDiagnosticTest {

    enum class RayClassification {
        CAPTURED,
        ESCAPED,
        DISK_EMISSION,
        UNRESOLVED
    }

    data class DiagnosticReport(
        val totalRays: Int,
        val capturedCount: Int,
        val escapedCount: Int,
        val diskEmissionCount: Int,
        val unresolvedCount: Int,
        val ringUnresolvedCount: Int,
        val capturedPercent: Double,
        val escapedPercent: Double,
        val diskEmissionPercent: Double,
        val unresolvedPercent: Double,
        val backgroundPercent: Double,
        val ringUnresolvedPercent: Double
    )

    private fun evaluateRepresentativeFrame(
        samplesX: Int = 40,
        samplesY: Int = 40,
        maxSteps: Int = 180
    ): DiagnosticReport {
        val M = 1.0f
        val a = 0.8f
        val diskInnerRadius = KerrIsco.compute(M.toDouble(), (a * M).toDouble()).toFloat()
        val diskOuterRadius = 22.0f

        val dist = 24.0f
        val inclRad = Math.toRadians(82.0).toFloat()
        val azRad = Math.toRadians(0.0).toFloat()

        val camX = dist * sin(inclRad) * cos(azRad)
        val camY = dist * sin(inclRad) * sin(azRad)
        val camZ = dist * cos(inclRad)

        val fwdLen = sqrt(camX * camX + camY * camY + camZ * camZ)
        val fwdX = -camX / fwdLen
        val fwdY = -camY / fwdLen
        val fwdZ = -camZ / fwdLen

        val rightLen = sqrt(fwdY * fwdY + fwdX * fwdX)
        val rX = fwdY / rightLen
        val rY = -fwdX / rightLen
        val rZ = 0.0f

        val upX = rY * fwdZ - rZ * fwdY
        val upY = rZ * fwdX - rX * fwdZ
        val upZ = rX * fwdY - rY * fwdX

        val fovScale = tan(Math.toRadians(45.0 * 0.5)).toFloat()

        var captured = 0
        var escaped = 0
        var disk = 0
        var unresolved = 0
        var ringUnresolved = 0

        val rCapture = M + sqrt(max(0.0f, M * M - a * a)) + 0.05f
        val rEscape = 50.0f
        // Same near-critical continuation as gargantua_geodesic.frag: past the ordinary budget, rays
        // with minR <= rPhotonShellOuter + 0.5 keep integrating up to MAX_INTEGRATION_STEPS = 1500.
        val rPhotonShellOuter = 2.0f * M * (1.0f + cos((2.0f / 3.0f) * acos((abs(a) / M).coerceIn(0.0f, 1.0f))))

        val classifications = Array(samplesY) { Array(samplesX) { RayClassification.UNRESOLVED } }

        for (row in 0 until samplesY) {
            val stY = 1.0f - row * (2.0f / (samplesY - 1))
            for (col in 0 until samplesX) {
                val stX = -1.0f + col * (2.0f / (samplesX - 1))

                // Initial ray direction
                val rdx = fwdX + rX * (stX * fovScale) + upX * (stY * fovScale)
                val rdy = fwdY + rY * (stX * fovScale) + upY * (stY * fovScale)
                val rdz = fwdZ + rZ * (stX * fovScale) + upZ * (stY * fovScale)
                val rdLen = sqrt(rdx * rdx + rdy * rdy + rdz * rdz)
                val rayDir = floatArrayOf(rdx / rdLen, rdy / rdLen, rdz / rdLen)

                val rInit = GpuEquivalentIntegrator.compute_r_KS(a, camX, camY, camZ)
                val g = GpuEquivalentIntegrator.compute_g_lower(M, a, camX, camY, camZ, rInit)

                val Aw = g[0][0]
                val Bw = g[1][0] * rayDir[0] + g[2][0] * rayDir[1] + g[3][0] * rayDir[2]
                var Cw = 0.0f
                for (i in 0 until 3) {
                    for (j in 0 until 3) {
                        Cw += g[i + 1][j + 1] * rayDir[i] * rayDir[j]
                    }
                }
                val Dw = max(0.0f, Bw * Bw - Aw * Cw)
                // Past-directed root (backward trace), identical to the shader / GpuEquivalentIntegrator.
                val w = (-Bw + sqrt(Dw)) / Aw
                val v = floatArrayOf(w, rayDir[0], rayDir[1], rayDir[2])

                val vLower = FloatArray(4)
                for (i in 0 until 4) {
                    var sum = 0.0f
                    for (j in 0 until 4) {
                        sum += g[i][j] * v[j]
                    }
                    vLower[i] = sum
                }
                val scale = vLower[0] // p_0 = +1
                var pSpatial = floatArrayOf(vLower[1] / scale, vLower[2] / scale, vLower[3] / scale)
                var pos = floatArrayOf(camX, camY, camZ)

                var outcome = RayClassification.UNRESOLVED
                var prevR = rInit
                var movingOutward = false
                var minR = rInit

                for (step in 0 until 1500) {
                    if (step >= maxSteps && minR > rPhotonShellOuter + 0.5f) break
                    val r = GpuEquivalentIntegrator.compute_r_KS(a, pos[0], pos[1], pos[2])
                    if (r < minR) minR = r
                    if (r > prevR) movingOutward = true
                    prevR = r

                    if (r <= rCapture) {
                        outcome = RayClassification.CAPTURED
                        break
                    }

                    if (movingOutward && (r >= rEscape || r >= diskOuterRadius)) {
                        outcome = RayClassification.ESCAPED
                        break
                    }

                    val prevPos = pos.clone()
                    val prevP = pSpatial.clone()

                    val baseStep = 0.08f * r
                    var dlambda = if (r > 10.0f && (movingOutward || r > 20.0f)) {
                        baseStep.coerceIn(0.02f, 0.75f)
                    } else {
                        baseStep.coerceIn(0.02f, 0.35f)
                    }

                    if (abs(pos[2]) < 0.60f && r >= diskInnerRadius - 0.5f && r <= diskOuterRadius + 1.0f) {
                        val vz = abs(pSpatial[2])
                        val stepToDisk = abs(pos[2]) / max(0.15f, vz)
                        dlambda = min(dlambda, max(0.04f, stepToDisk * 0.80f + 0.02f))
                    }
                    // Capture-zone limiter, identical to gargantua_geodesic.frag (causticStep)
                    if (r > rCapture && r < rCapture + 0.95f) {
                        dlambda = min(dlambda, 0.032f + (0.075f - 0.032f) * ((r - rCapture) / 0.95f))
                    }

                    val nextState = GpuEquivalentIntegrator.rk4_step(
                        M, a,
                        floatArrayOf(pos[0], pos[1], pos[2], pSpatial[0], pSpatial[1], pSpatial[2]),
                        dlambda
                    )
                    pos = floatArrayOf(nextState[0], nextState[1], nextState[2])
                    pSpatial = floatArrayOf(nextState[3], nextState[4], nextState[5])

                    if (prevPos[2] * pos[2] <= 0.0f && prevPos[2] != pos[2]) {
                        val tau = (-prevPos[2] / (pos[2] - prevPos[2])).coerceIn(0.0f, 1.0f)
                        val hitX = prevPos[0] + tau * (pos[0] - prevPos[0])
                        val hitY = prevPos[1] + tau * (pos[1] - prevPos[1])
                        val rHit = sqrt(hitX * hitX + hitY * hitY)
                        if (rHit in diskInnerRadius..diskOuterRadius) {
                            outcome = RayClassification.DISK_EMISSION
                            break
                        }
                    }
                }

                if (outcome == RayClassification.UNRESOLVED) {
                    if (movingOutward && prevR > 5.0f) {
                        outcome = RayClassification.ESCAPED
                    } else if (prevR <= rCapture + 0.3f) {
                        outcome = RayClassification.CAPTURED
                    }
                }

                classifications[row][col] = outcome
                when (outcome) {
                    RayClassification.CAPTURED -> captured++
                    RayClassification.ESCAPED -> escaped++
                    RayClassification.DISK_EMISSION -> disk++
                    RayClassification.UNRESOLVED -> unresolved++
                }
            }
        }

        // Identify unresolved pixels neighboring the visible disk ring
        for (r in 0 until samplesY) {
            for (c in 0 until samplesX) {
                if (classifications[r][c] == RayClassification.UNRESOLVED) {
                    var nearDisk = false
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until samplesY && nc in 0 until samplesX) {
                                if (classifications[nr][nc] == RayClassification.DISK_EMISSION) {
                                    nearDisk = true
                                }
                            }
                        }
                    }
                    if (nearDisk) ringUnresolved++
                }
            }
        }

        val total = samplesX * samplesY
        return DiagnosticReport(
            totalRays = total,
            capturedCount = captured,
            escapedCount = escaped,
            diskEmissionCount = disk,
            unresolvedCount = unresolved,
            ringUnresolvedCount = ringUnresolved,
            capturedPercent = captured * 100.0 / total,
            escapedPercent = escaped * 100.0 / total,
            diskEmissionPercent = disk * 100.0 / total,
            unresolvedPercent = unresolved * 100.0 / total,
            backgroundPercent = escaped * 100.0 / total,
            ringUnresolvedPercent = ringUnresolved * 100.0 / total
        )
    }

    @Test
    fun representativeFrameClassificationMeetsDiagnosticGates() {
        val report = evaluateRepresentativeFrame(samplesX = 40, samplesY = 40, maxSteps = 180)

        println("=== GARGANTUA RAY CLASSIFICATION TELEMETRY REPORT ===")
        println("Total rays evaluated: ${report.totalRays}")
        println("Captured (Shadow):     ${report.capturedCount} (${String.format("%.2f", report.capturedPercent)}%)")
        println("Escaped (Background):  ${report.escapedCount} (${String.format("%.2f", report.escapedPercent)}%)")
        println("Disk Emission:         ${report.diskEmissionCount} (${String.format("%.2f", report.diskEmissionPercent)}%)")
        println("Unresolved:            ${report.unresolvedCount} (${String.format("%.2f", report.unresolvedPercent)}%)")
        println("Unresolved Near Ring:  ${report.ringUnresolvedCount} (${String.format("%.2f", report.ringUnresolvedPercent)}%)")
        println("=====================================================")

        // Verification Gates:
        // 1. Unresolved rays must be exactly 0 (no pixel should be unresolved or timeout into starfield)
        assertEquals("Percentage of unresolved pixels must be strictly 0.0%", 0.0, report.unresolvedPercent, 1e-6)
        assertEquals("Number of unresolved pixels near visible ring must be 0", 0, report.ringUnresolvedCount)

        // 2. Both shadow and disk emission must be prominently present in the default framing
        assertTrue("Black hole shadow must cover > 5% of default viewport", report.capturedPercent > 5.0)
        assertTrue("Accretion disk must cover > 30% of default viewport", report.diskEmissionPercent > 30.0)

        // 3. Background must be cleanly identified as escaped
        assertTrue("Escaped background rays must be present", report.escapedPercent > 0.0)
    }
}
