package com.alijafari.red.astronomy.startracker.calibration

import kotlin.math.*

/**
 * Least-squares refinement of k1,k2,p1,p2 given fixed intrinsics and matched observations.
 * Same approach as IntrinsicsRefiner but solving for distortion coefficients.
 *
 * Model (Brown-Conrady):
 *   Given ideal normalized (x,y) and observed distorted normalized (x_d, y_d):
 *   x_d = x*(1 + k1*r2 + k2*r4) + 2*p1*x*y + p2*(r2 + 2*x^2)
 *   y_d = y*(1 + k1*r2 + k2*r4) + p1*(r2 + 2*y^2) + 2*p2*x*y
 *   where r2 = x^2 + y^2, r4 = r2^2
 *
 * This is LINEAR in k1,k2,p1,p2 given x,y,r2,r4 and observed x_d,y_d:
 *   x_d = x + x*r2*k1 + x*r4*k2 + 2*x*y*p1 + (r2+2*x^2)*p2
 *   y_d = y + y*r2*k1 + y*r4*k2 + (r2+2*y^2)*p1 + 2*x*y*p2
 *
 * So we can solve linear least squares for [k1,k2,p1,p2]
 */

data class DistortionObservation(
    val idealNormalized: Pair<Double, Double>, // (x,y) ideal
    val distortedNormalized: Pair<Double, Double> // (x_d, y_d) observed distorted
)

data class DistortionRefineResult(
    val refinedModel: DistortionModel,
    val rmsError: Double,
    val success: Boolean,
    val message: String
)

class DistortionRefiner(
    val minObservations: Int = 10, // need at least 4 params, but more for robustness
    val minSpan: Double = 0.5 // minimum span in normalized coordinates to reliably estimate distortion
) {

    fun refine(
        initialModel: DistortionModel,
        observations: List<DistortionObservation>
    ): DistortionRefineResult {
        if (observations.size < minObservations) {
            return DistortionRefineResult(
                refinedModel = initialModel,
                rmsError = Double.MAX_VALUE,
                success = false,
                message = "Insufficient data: ${observations.size} < $minObservations"
            )
        }

        // Check if observations clustered near center (where distortion near-zero unobservable)
        val xs = observations.map { it.idealNormalized.first }
        val ys = observations.map { it.idealNormalized.second }
        val minX = xs.minOrNull() ?: 0.0
        val maxX = xs.maxOrNull() ?: 0.0
        val minY = ys.minOrNull() ?: 0.0
        val maxY = ys.maxOrNull() ?: 0.0
        val spanX = maxX - minX
        val spanY = maxY - minY
        val maxR2 = observations.maxOf { it.idealNormalized.first * it.idealNormalized.first + it.idealNormalized.second * it.idealNormalized.second }

        if (spanX < minSpan || spanY < minSpan) {
            return DistortionRefineResult(
                refinedModel = initialModel,
                rmsError = Double.MAX_VALUE,
                success = false,
                message = "Poorly distributed near center: spanX=$spanX, spanY=$spanY < $minSpan, maxR2=$maxR2, distortion unobservable"
            )
        }

        if (maxR2 < 0.1) {
            // All points near center, r2 small, distortion effect near-zero
            return DistortionRefineResult(
                refinedModel = initialModel,
                rmsError = Double.MAX_VALUE,
                success = false,
                message = "Observations clustered near center maxR2=$maxR2 <0.1, cannot reliably estimate distortion"
            )
        }

        // Build linear system: A * [k1,k2,p1,p2]^T = b
        // For each observation, we have 2 equations (x and y)
        val m = observations.size * 2
        val n = 4
        val A = Array(m) { DoubleArray(n) }
        val b = DoubleArray(m)

        for ((idx, obs) in observations.withIndex()) {
            val (x, y) = obs.idealNormalized
            val (xD, yD) = obs.distortedNormalized
            val r2 = x * x + y * y
            val r4 = r2 * r2

            // Equation for x_d: x_d - x = x*r2*k1 + x*r4*k2 + 2*x*y*p1 + (r2+2*x^2)*p2
            val rowX = idx * 2
            A[rowX][0] = x * r2 // k1
            A[rowX][1] = x * r4 // k2
            A[rowX][2] = 2.0 * x * y // p1
            A[rowX][3] = r2 + 2.0 * x * x // p2
            b[rowX] = xD - x

            // Equation for y_d: y_d - y = y*r2*k1 + y*r4*k2 + (r2+2*y^2)*p1 + 2*x*y*p2
            val rowY = idx * 2 + 1
            A[rowY][0] = y * r2 // k1
            A[rowY][1] = y * r4 // k2
            A[rowY][2] = r2 + 2.0 * y * y // p1
            A[rowY][3] = 2.0 * x * y // p2
            b[rowY] = yD - y
        }

        val solution = solveLinearLeastSquares(A, b)
        if (solution == null) {
            return DistortionRefineResult(
                refinedModel = initialModel,
                rmsError = Double.MAX_VALUE,
                success = false,
                message = "Singular matrix, cannot solve"
            )
        }

        val k1 = solution[0]
        val k2 = solution[1]
        val p1 = solution[2]
        val p2 = solution[3]

        // Check for overfitting: large spurious coefficients
        if (abs(k1) > 1.0 || abs(k2) > 1.0 || abs(p1) > 0.1 || abs(p2) > 0.1) {
            return DistortionRefineResult(
                refinedModel = initialModel,
                rmsError = Double.MAX_VALUE,
                success = false,
                message = "Overfitting detected: large coefficients k1=$k1, k2=$k2, p1=$p1, p2=$p2, decline to refine"
            )
        }

        val refinedModel = DistortionModel(k1, k2, p1, p2)

        // Compute RMS error
        var sumSq = 0.0
        for (obs in observations) {
            val (x, y) = obs.idealNormalized
            val (xDObs, yDObs) = obs.distortedNormalized
            val (xDPred, yDPred) = refinedModel.distortIdealToDistortedNormalized(x, y)
            val dx = xDObs - xDPred
            val dy = yDObs - yDPred
            sumSq += dx * dx + dy * dy
        }
        val rms = sqrt(sumSq / m)

        return DistortionRefineResult(
            refinedModel = refinedModel,
            rmsError = rms,
            success = true,
            message = "Refined with ${observations.size} observations, RMS $rms"
        )
    }

    private fun solveLinearLeastSquares(A: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val m = A.size
        val n = A[0].size

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

        return solveLinearSystem(AtA, Atb)
    }

    private fun solveLinearSystem(A: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = A.size
        val M = Array(n) { i -> A[i].clone() }
        val v = b.clone()

        for (col in 0 until n) {
            var maxRow = col
            var maxVal = abs(M[col][col])
            for (row in col + 1 until n) {
                val absVal = abs(M[row][col])
                if (absVal > maxVal) {
                    maxVal = absVal
                    maxRow = row
                }
            }

            if (maxVal < 1e-12) return null

            if (maxRow != col) {
                val tmpM = M[col]
                M[col] = M[maxRow]
                M[maxRow] = tmpM
                val tmpV = v[col]
                v[col] = v[maxRow]
                v[maxRow] = tmpV
            }

            for (row in col + 1 until n) {
                val factor = M[row][col] / M[col][col]
                for (j in col until n) {
                    M[row][j] -= factor * M[col][j]
                }
                v[row] -= factor * v[col]
            }
        }

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
