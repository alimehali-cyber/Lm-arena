package com.zig.gargantua.geodesic

import kotlin.math.*

/**
 * Deterministic software emulation of the GPU GLSL ES 3.0 shader pipeline [gargantua_geodesic.frag].
 *
 * Implements the exact same IEEE 754 single-precision (Float / highp float) mathematical
 * operations as executed on the GPU, enabling deterministic bit-level validation and
 * numerical correspondence checks against the CPU double-precision reference.
 *
 * EXACT CORRESPONDENCE WITH GLSL SHADER:
 * 1. Single-precision 32-bit float arithmetic matching GLSL highp float precision.
 * 2. Kerr-Schild Cartesian metric inversion g^μν = η^μν - 2H l^μ l^ν.
 * 3. Exact analytical spatial metric derivatives ∂_i g^μν.
 * 4. Pinhole camera ray null momentum solving: A_w w² + 2 B_w w + C_w = 0.
 * 5. 4th-order Runge-Kutta (RK4) integration with adaptive step Δλ(r) = clamp(0.08*r, 0.02, 0.35).
 * 6. Horizon capture threshold r ≤ r_+ + 0.05 and escape threshold r ≥ 50.0.
 */
object GpuEquivalentIntegrator {

    data class GpuRayResult(
        val isCaptured: Boolean,
        val isEscaped: Boolean,
        val isDiskHit: Boolean = false,
        val stepsTaken: Int,
        val finalPos: FloatArray,      // [X, Y, Z]
        val finalMomentum: FloatArray, // [pX, pY, pZ]
        val minRadiusReached: Float,
        val maxHamiltonianResidual: Float,
        val intermediateStates: List<FloatArray>, // List of 6D states [X, Y, Z, pX, pY, pZ]
        val frequencyShift: Float = 1.0f,
        val rHit: Float = 0.0f
    )

    fun compute_r_KS(a: Float, X: Float, Y: Float, Z: Float): Float {
        val R2 = X * X + Y * Y + Z * Z
        val a2 = a * a
        val z2 = Z * Z
        val diff = R2 - a2
        val rad = sqrt(max(0.0f, diff * diff + 4.0f * a2 * z2))
        val r2 = 0.5f * (diff + rad)
        return sqrt(max(0.0f, r2))
    }

    /** 4x4 covariant metric g_μν in single precision. */
    fun compute_g_lower(M: Float, a: Float, X: Float, Y: Float, Z: Float, r: Float): Array<FloatArray> {
        val r2 = r * r
        val a2 = a * a
        val z2 = Z * Z
        val denom = r2 * r2 + a2 * z2
        val H = if (denom > 1e-12f) (M * r * r2) / denom else 0.0f
        val twoH = 2.0f * H

        val denomXY = r2 + a2
        val lx = if (denomXY > 1e-12f) (r * X + a * Y) / denomXY else 0.0f
        val ly = if (denomXY > 1e-12f) (r * Y - a * X) / denomXY else 0.0f
        val lz = if (r > 1e-12f) Z / r else 0.0f

        val l = floatArrayOf(1.0f, lx, ly, lz)
        val g = Array(4) { FloatArray(4) }
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                var eta = 0.0f
                if (i == j) {
                    eta = if (i == 0) -1.0f else 1.0f
                }
                g[i][j] = eta + twoH * l[i] * l[j]
            }
        }
        return g
    }

    /** 4x4 contravariant metric g^μν in single precision. */
    fun compute_g_inv(M: Float, a: Float, X: Float, Y: Float, Z: Float, r: Float): Array<FloatArray> {
        val r2 = r * r
        val a2 = a * a
        val z2 = Z * Z
        val denom = r2 * r2 + a2 * z2
        val H = if (denom > 1e-12f) (M * r * r2) / denom else 0.0f
        val twoH = 2.0f * H

        val denomXY = r2 + a2
        val lx = if (denomXY > 1e-12f) (r * X + a * Y) / denomXY else 0.0f
        val ly = if (denomXY > 1e-12f) (r * Y - a * X) / denomXY else 0.0f
        val lz = if (r > 1e-12f) Z / r else 0.0f

        val lContra = floatArrayOf(-1.0f, lx, ly, lz)
        val gInv = Array(4) { FloatArray(4) }
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                var eta = 0.0f
                if (i == j) {
                    eta = if (i == 0) -1.0f else 1.0f
                }
                gInv[i][j] = eta - twoH * lContra[i] * lContra[j]
            }
        }
        return gInv
    }

    /** Exact analytical spatial derivatives of g^μν with respect to X, Y, Z. */
    fun compute_dg_inv(
        M: Float,
        a: Float,
        X: Float,
        Y: Float,
        Z: Float,
        r: Float
    ): Triple<Array<FloatArray>, Array<FloatArray>, Array<FloatArray>> {
        val r2 = r * r
        val r3 = r2 * r
        val r4 = r2 * r2
        val a2 = a * a
        val z2 = Z * Z
        val denomSigma = r4 + a2 * z2
        val denomSigma2 = denomSigma * denomSigma
        val H = if (denomSigma > 1e-12f) (M * r3) / denomSigma else 0.0f

        val dr_dX = if (denomSigma > 1e-12f) (r3 * X) / denomSigma else 0.0f
        val dr_dY = if (denomSigma > 1e-12f) (r3 * Y) / denomSigma else 0.0f
        val dr_dZ = if (denomSigma > 1e-12f) (Z * r * (r2 + a2)) / denomSigma else 0.0f

        val dH_dr = if (denomSigma2 > 1e-12f) M * r2 * (3.0f * a2 * z2 - r4) / denomSigma2 else 0.0f
        val dH_dZ_expl = if (denomSigma2 > 1e-12f) -2.0f * M * a2 * r3 * Z / denomSigma2 else 0.0f

        val dH_dX = dH_dr * dr_dX
        val dH_dY = dH_dr * dr_dY
        val dH_dZ = dH_dr * dr_dZ + dH_dZ_expl

        val denomV = r2 + a2
        val denomV2 = denomV * denomV

        val lx = if (denomV > 1e-12f) (r * X + a * Y) / denomV else 0.0f
        val ly = if (denomV > 1e-12f) (r * Y - a * X) / denomV else 0.0f
        val lz = if (r > 1e-12f) Z / r else 0.0f
        val l = floatArrayOf(-1.0f, lx, ly, lz)

        fun diff_l(dr_di: Float, isX: Boolean, isY: Boolean, isZ: Boolean): FloatArray {
            val dv = 2.0f * r * dr_di
            val duX = dr_di * X + (if (isX) r else 0.0f) + (if (isY) a else 0.0f)
            val dlx = (duX * denomV - (r * X + a * Y) * dv) / denomV2

            val duY = dr_di * Y + (if (isY) r else 0.0f) - (if (isX) a else 0.0f)
            val dly = (duY * denomV - (r * Y - a * X) * dv) / denomV2

            val dlz = if (r > 1e-12f) ((if (isZ) 1.0f else 0.0f) * r - Z * dr_di) / r2 else 0.0f

            return floatArrayOf(0.0f, dlx, dly, dlz)
        }

        val dl_dX = diff_l(dr_dX, isX = true, isY = false, isZ = false)
        val dl_dY = diff_l(dr_dY, isX = false, isY = true, isZ = false)
        val dl_dZ = diff_l(dr_dZ, isX = false, isY = false, isZ = true)

        fun assemble_dg(dH_di: Float, dl_di: FloatArray): Array<FloatArray> {
            val res = Array(4) { FloatArray(4) }
            for (i in 0 until 4) {
                for (j in 0 until 4) {
                    res[i][j] = -2.0f * dH_di * l[i] * l[j] - 2.0f * H * (dl_di[i] * l[j] + l[i] * dl_di[j])
                }
            }
            return res
        }

        return Triple(assemble_dg(dH_dX, dl_dX), assemble_dg(dH_dY, dl_dY), assemble_dg(dH_dZ, dl_dZ))
    }

    /** Evaluates right-hand side of Hamilton's equations for 6D state. */
    fun evaluate_derivatives(M: Float, a: Float, state: FloatArray): FloatArray {
        val X = state[0]
        val Y = state[1]
        val Z = state[2]
        val px = state[3]
        val py = state[4]
        val pz = state[5]
        val p = floatArrayOf(-1.0f, px, py, pz)

        val r = compute_r_KS(a, X, Y, Z)
        val gInv = compute_g_inv(M, a, X, Y, Z, r)
        val (dg_dX, dg_dY, dg_dZ) = compute_dg_inv(M, a, X, Y, Z, r)

        var dX = 0.0f
        var dY = 0.0f
        var dZ = 0.0f
        for (nu in 0 until 4) {
            dX += gInv[1][nu] * p[nu]
            dY += gInv[2][nu] * p[nu]
            dZ += gInv[3][nu] * p[nu]
        }

        var sumX = 0.0f
        var sumY = 0.0f
        var sumZ = 0.0f
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val pTerm = p[i] * p[j]
                sumX += dg_dX[i][j] * pTerm
                sumY += dg_dY[i][j] * pTerm
                sumZ += dg_dZ[i][j] * pTerm
            }
        }

        return floatArrayOf(
            dX,
            dY,
            dZ,
            -0.5f * sumX,
            -0.5f * sumY,
            -0.5f * sumZ
        )
    }

    fun rk4_step(M: Float, a: Float, state: FloatArray, dlambda: Float): FloatArray {
        val k1 = evaluate_derivatives(M, a, state)

        val s2 = FloatArray(6) { i -> state[i] + 0.5f * dlambda * k1[i] }
        val k2 = evaluate_derivatives(M, a, s2)

        val s3 = FloatArray(6) { i -> state[i] + 0.5f * dlambda * k2[i] }
        val k3 = evaluate_derivatives(M, a, s3)

        val s4 = FloatArray(6) { i -> state[i] + dlambda * k3[i] }
        val k4 = evaluate_derivatives(M, a, s4)

        val next = FloatArray(6)
        val sixth = dlambda / 6.0f
        for (i in 0 until 6) {
            next[i] = state[i] + sixth * (k1[i] + 2.0f * k2[i] + 2.0f * k3[i] + k4[i])
        }
        return next
    }

    /** Constructs initial null momentum p_μ matching the GLSL main() ray initialization. */
    fun createInitialRay(M: Float, a: Float, camPos: FloatArray, rayDir: FloatArray): FloatArray {
        val rInit = compute_r_KS(a, camPos[0], camPos[1], camPos[2])
        val g = compute_g_lower(M, a, camPos[0], camPos[1], camPos[2], rInit)

        val Aw = g[0][0]
        var Bw = 0.0f
        for (i in 0 until 3) {
            Bw += g[0][i + 1] * rayDir[i]
        }

        var Cw = 0.0f
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                Cw += g[i + 1][j + 1] * rayDir[i] * rayDir[j]
            }
        }

        val Dw = max(0.0f, Bw * Bw - Aw * Cw)
        val w = (-Bw - sqrt(Dw)) / Aw
        val v = floatArrayOf(w, rayDir[0], rayDir[1], rayDir[2])

        val vLower = FloatArray(4)
        for (i in 0 until 4) {
            var sum = 0.0f
            for (j in 0 until 4) {
                sum += g[i][j] * v[j]
            }
            vLower[i] = sum
        }

        val scale = -vLower[0]
        return floatArrayOf(
            camPos[0],
            camPos[1],
            camPos[2],
            vLower[1] / scale,
            vLower[2] / scale,
            vLower[3] / scale
        )
    }

    fun computeHamiltonian(M: Float, a: Float, state: FloatArray): Float {
        val r = compute_r_KS(a, state[0], state[1], state[2])
        val gInv = compute_g_inv(M, a, state[0], state[1], state[2], r)
        val p = floatArrayOf(-1.0f, state[3], state[4], state[5])
        var H = 0.0f
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                H += gInv[i][j] * p[i] * p[j]
            }
        }
        return 0.5f * H
    }

    fun traceRay(
        M: Float,
        a: Float,
        camPos: FloatArray,
        rayDir: FloatArray,
        maxSteps: Int = 150,
        enableDisk: Boolean = false,
        diskInnerRadius: Float = 6.0f,
        diskOuterRadius: Float = 22.0f
    ): GpuRayResult {
        var state = createInitialRay(M, a, camPos, rayDir)
        val rPlus = M + sqrt(max(0.0f, M * M - a * a))
        val rCapture = rPlus + 0.05f

        val rInitCam = compute_r_KS(a, camPos[0], camPos[1], camPos[2])
        val rEscape = max(50.0f, rInitCam + 15.0f)

        var minR = rInitCam
        var maxH = abs(computeHamiltonian(M, a, state))
        val intermediateList = mutableListOf(state.clone())

        var prevR = minR
        var movingOutward = false
        var isEscaped = false
        var isCaptured = false
        var isDiskHit = false
        var rHitResult = 0.0f
        var gShiftResult = 1.0f
        var stepCount = 0

        val gCam = compute_g_lower(M, a, camPos[0], camPos[1], camPos[2], rInitCam)
        val uObs0 = 1.0f / sqrt(max(1.0e-6f, -gCam[0][0]))

        for (step in 0 until maxSteps) {
            stepCount++
            val r = compute_r_KS(a, state[0], state[1], state[2])
            if (r < minR) minR = r
            if (r > prevR) {
                movingOutward = true
            }

            val h = abs(computeHamiltonian(M, a, state))
            if (h > maxH) maxH = h

            if (r <= rCapture) {
                isCaptured = true
                break
            }
            if (movingOutward && (r >= rEscape || (enableDisk && r >= diskOuterRadius))) {
                isEscaped = true
                break
            }
            prevR = r

            val baseStep = 0.08f * r
            var dlambda = baseStep.coerceIn(0.02f, 0.35f)
            if (enableDisk && abs(state[2]) < 0.60f && r >= diskInnerRadius - 0.5f && r <= diskOuterRadius + 1.0f) {
                val vz = abs(state[5])
                val stepToDisk = abs(state[2]) / max(0.15f, vz)
                dlambda = min(dlambda, max(0.04f, stepToDisk * 0.80f + 0.02f))
            }
            val prevState = state.clone()
            state = rk4_step(M, a, state, dlambda)
            intermediateList.add(state.clone())

            if (enableDisk && prevState[2] * state[2] <= 0.0f && prevState[2] != state[2]) {
                val tau = -prevState[2] / (state[2] - prevState[2])
                if (tau in 0.0f..1.0f) {
                    val hitX = prevState[0] + tau * (state[0] - prevState[0])
                    val hitY = prevState[1] + tau * (state[1] - prevState[1])
                    val rHit = sqrt(hitX * hitX + hitY * hitY)

                    if (rHit in diskInnerRadius..diskOuterRadius) {
                        val hitPx = prevState[3] + tau * (state[3] - prevState[3])
                        val hitPy = prevState[4] + tau * (state[4] - prevState[4])

                        val omega = sqrt(M) / (rHit.pow(1.5f) + a * sqrt(M))
                        val gHit = compute_g_lower(M, a, hitX, hitY, 0.0f, rHit)
                        val vEmit = floatArrayOf(1.0f, -omega * hitY, omega * hitX, 0.0f)

                        var denomContract = 0.0f
                        for (i in 0 until 4) {
                            for (j in 0 until 4) {
                                denomContract += gHit[i][j] * vEmit[i] * vEmit[j]
                            }
                        }
                        val u0 = if (denomContract < 0.0f) 1.0f / sqrt(-denomContract) else 1.0f

                        val lz = hitX * hitPy - hitY * hitPx
                        val denomG = u0 * (1.0f + omega * lz)
                        val gShift = if (abs(denomG) > 1.0e-6f) uObs0 / denomG else 1.0f

                        isDiskHit = true
                        rHitResult = rHit
                        gShiftResult = gShift
                        break
                    }
                }
            }
        }

        return GpuRayResult(
            isCaptured = isCaptured,
            isEscaped = isEscaped,
            isDiskHit = isDiskHit,
            stepsTaken = stepCount,
            finalPos = floatArrayOf(state[0], state[1], state[2]),
            finalMomentum = floatArrayOf(state[3], state[4], state[5]),
            minRadiusReached = minR,
            maxHamiltonianResidual = maxH,
            intermediateStates = intermediateList,
            frequencyShift = gShiftResult,
            rHit = rHitResult
        )
    }
}
