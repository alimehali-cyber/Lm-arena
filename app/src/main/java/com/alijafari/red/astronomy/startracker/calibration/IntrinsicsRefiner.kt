package com.alijafari.red.astronomy.startracker.calibration

import kotlin.math.*

/**
 * Least-squares refinement of fx,fy,cx,cy,skew from matched star observations.
 * Given set of (predicted ideal pixel from catalog-direction + solved-attitude + assumed pinhole, observed detected pixel) pairs,
 * minimizes reprojection error.
 *
 * Simple, auditable iterative scheme: linearize around starting estimate, solve normal equations for correction.
 *
 * For this phase, we refine fx,fy,cx,cy first with distortion fixed at zero, then refine k1,k2,p1,p2 separately.
 * This is more numerically stable and easier to verify.
 */

data class ObservationPair(
    val predictedIdealPixel: Pair<Double, Double>, // (u_pred, v_pred) from catalog+attitude+current intrinsics estimate
    val observedPixel: Pair<Double, Double>, // (u_obs, v_obs) detected
    val idealNormalized: Pair<Double, Double> // (x,y) ideal normalized from catalog+attitude (for linear solve)
)

data class IntrinsicsRefineResult(
    val refinedProfile: CameraProfile,
    val rmsError: Double,
    val success: Boolean,
    val message: String
)

class IntrinsicsRefiner(
    val maxIterations: Int = 10,
    val convergenceThreshold: Double = 1e-6,
    val minObservations: Int = 6 // need at least 4 params, but more for robustness
) {

    fun refine(
        initialProfile: CameraProfile,
        observations: List<ObservationPair>
    ): IntrinsicsRefineResult {
        if (observations.size < minObservations) {
            return IntrinsicsRefineResult(
                refinedProfile = initialProfile,
                rmsError = Double.MAX_VALUE,
                success = false,
                message = "Insufficient data: ${observations.size} < $minObservations"
            )
        }

        // Check distribution: observations should be spread across frame, not clustered near center
        // Simple check: compute bounding box of observed pixels, if too small, decline to refine
        val minU = observations.minOf { it.observedPixel.first }
        val maxU = observations.maxOf { it.observedPixel.first }
        val minV = observations.minOf { it.observedPixel.second }
        val maxV = observations.maxOf { it.observedPixel.second }
        val spanU = maxU - minU
        val spanV = maxV - minV

        // If span < 20% of image size (estimated from cx,cy,fx), decline
        val estimatedWidth = initialProfile.fx * 2 // rough estimate, FOV ~60° => width ~2*fx*tan(30°)
        if (spanU < estimatedWidth * 0.1 || spanV < estimatedWidth * 0.1) {
            return IntrinsicsRefineResult(
                refinedProfile = initialProfile,
                rmsError = Double.MAX_VALUE,
                success = false,
                message = "Poorly distributed: spanU=$spanU, spanV=$spanV < 10% of estimated width"
            )
        }

        var fx = initialProfile.fx
        var fy = initialProfile.fy
        var cx = initialProfile.cx
        var cy = initialProfile.cy
        var skew = initialProfile.skew

        var prevRms = Double.MAX_VALUE

        for (iter in 0 until maxIterations) {
            // Build normal equations for linear least squares:
            // Model: u_obs = fx * x + skew * y + cx
            //        v_obs = fy * y + cy
            // For each observation, we have ideal normalized (x,y) and observed (u_obs, v_obs)
            // Residual: du = u_obs - (fx*x + skew*y + cx), dv = v_obs - (fy*y + cy)
            // Jacobian w.r.t params [fx, fy, cx, cy, skew]:
            //   du/dfx = x, du/dfy=0, du/dcx=1, du/dcy=0, du/dskew=y
            //   dv/dfx=0, dv/dfy=y, dv/dcx=0, dv/dcy=1, dv/dskew=0

            // For simplicity, solve separately for u and v:
            // u: params [fx, cx, skew] with model u = fx*x + skew*y + cx
            // v: params [fy, cy] with model v = fy*y + cy

            // Solve for u params via linear least squares
            // Build A matrix and b vector for u: A * [fx, cx, skew]^T = b (u_obs)
            val n = observations.size
            // For u: 3 params
            val Au = Array(n) { DoubleArray(3) }
            val bu = DoubleArray(n)
            for (i in observations.indices) {
                val (x, y) = observations[i].idealNormalized
                val (uObs, _) = observations[i].observedPixel
                Au[i][0] = x // fx
                Au[i][1] = 1.0 // cx
                Au[i][2] = y // skew
                bu[i] = uObs
            }

            val solU = solveLinearLeastSquares(Au, bu)
            if (solU != null) {
                fx = solU[0]
                cx = solU[1]
                skew = solU[2]
            }

            // For v: 2 params [fy, cy]
            val Av = Array(n) { DoubleArray(2) }
            val bv = DoubleArray(n)
            for (i in observations.indices) {
                val (_, y) = observations[i].idealNormalized
                val (_, vObs) = observations[i].observedPixel
                Av[i][0] = y // fy
                Av[i][1] = 1.0 // cy
                bv[i] = vObs
            }

            val solV = solveLinearLeastSquares(Av, bv)
            if (solV != null) {
                fy = solV[0]
                cy = solV[1]
            }

            // Compute RMS error
            var sumSq = 0.0
            for (obs in observations) {
                val (x, y) = obs.idealNormalized
                val (uObs, vObs) = obs.observedPixel
                val uPred = fx * x + skew * y + cx
                val vPred = fy * y + cy
                val du = uObs - uPred
                val dv = vObs - vPred
                sumSq += du * du + dv * dv
            }
            val rms = sqrt(sumSq / (2 * n))

            // Check convergence
            if (abs(prevRms - rms) < convergenceThreshold) {
                break
            }
            prevRms = rms
        }

        // Final RMS
        var sumSq = 0.0
        for (obs in observations) {
            val (x, y) = obs.idealNormalized
            val (uObs, vObs) = obs.observedPixel
            val uPred = fx * x + skew * y + cx
            val vPred = fy * y + cy
            val du = uObs - uPred
            val dv = vObs - vPred
            sumSq += du * du + dv * dv
        }
        val finalRms = sqrt(sumSq / (2 * observations.size))

        val refined = initialProfile.copy(
            fx = fx,
            fy = fy,
            cx = cx,
            cy = cy,
            skew = skew,
            sampleCount = initialProfile.sampleCount + observations.size
        )

        return IntrinsicsRefineResult(
            refinedProfile = refined,
            rmsError = finalRms,
            success = true,
            message = "Refined with ${observations.size} observations, RMS $finalRms"
        )
    }

    /**
     * Solve linear least squares A * x = b via normal equations: (A^T A) x = A^T b
     * For small matrices (3x3, 2x2), use direct solve.
     * Returns null if singular.
     */
    private fun solveLinearLeastSquares(A: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val m = A.size
        val n = A[0].size

        // Compute A^T A (n x n) and A^T b (n)
        val AtA = Array(n) { DoubleArray(n) { 0.0 } }
        val Atb = DoubleArray(n) { 0.0 }

        for (i in 0 until n) {
            for (j in 0 until n) {
                var sum = 0.0
                for (k in 0 until m) {
                    sum += A[k][i] * A[k][j]
                }
                AtA[i][j] = sum
            }
            var sum = 0.0
            for (k in 0 until m) {
                sum += A[k][i] * b[k]
            }
            Atb[i] = sum
        }

        // Solve AtA * x = Atb via Gaussian elimination for small n
        return solveLinearSystem(AtA, Atb)
    }

    private fun solveLinearSystem(A: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = A.size
        val M = Array(n) { i -> A[i].clone() }
        val v = b.clone()

        // Gaussian elimination with partial pivoting
        for (col in 0 until n) {
            // Find pivot
            var maxRow = col
            var maxVal = abs(M[col][col])
            for (row in col + 1 until n) {
                val absVal = abs(M[row][col])
                if (absVal > maxVal) {
                    maxVal = absVal
                    maxRow = row
                }
            }

            if (maxVal < 1e-12) return null // singular

            // Swap rows
            if (maxRow != col) {
                val tmpM = M[col]
                M[col] = M[maxRow]
                M[maxRow] = tmpM
                val tmpV = v[col]
                v[col] = v[maxRow]
                v[maxRow] = tmpV
            }

            // Eliminate
            for (row in col + 1 until n) {
                val factor = M[row][col] / M[col][col]
                for (j in col until n) {
                    M[row][j] -= factor * M[col][j]
                }
                v[row] -= factor * v[col]
            }
        }

        // Back substitution
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = v[i]
            for (j in i + 1 until n) {
                sum -= M[i][j] * x[j]
            }
            if (abs(M[i][i]) < 1e-12) return null
            x[i] = sum / M[i][i]
        }

        return x
    }
}
