package com.alijafari.red.astronomy.startracker.solver

import kotlin.math.*

/**
 * Attitude solver: Davenport q-method + TRIAD fallback.
 *
 * Davenport q-method: build K matrix from paired unit vectors, find largest-eigenvalue eigenvector.
 *
 * K matrix construction (from Markley & Mortari):
 * Given correspondences (b_i = catalog unit vector, r_i = observed unit vector) with weights w_i:
 *   B = Σ w_i * b_i * r_i^T (3x3)
 *   S = B + B^T
 *   z = Σ w_i * (b_i × r_i)
 *   sigma = trace(B)
 *   K = [ S - sigma*I   z
 *         z^T          sigma ] as 4x4 symmetric
 *
 * Then optimal quaternion is eigenvector of K corresponding to largest eigenvalue.
 *
 * For eigenvalue decomposition of 4x4 symmetric matrix, we implement Jacobi iteration
 * (simple, hand-verifiable, auditable) per Task 0 guidance.
 */

class AttitudeSolver {

    /**
     * Solve Davenport q-method.
     * @param correspondences list of pairs (catalogUnitVector, observedUnitVector)
     * @param weights list of weights (same size)
     * @return quaternion rotating catalog frame to observed (camera) frame
     */
    fun solveDavenportQMethod(
        correspondences: List<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>>,
        weights: List<Double>
    ): Quaternion {
        require(correspondences.isNotEmpty())
        require(correspondences.size == weights.size)

        // Build B matrix 3x3
        val B = Array(3) { DoubleArray(3) { 0.0 } }
        for ((idx, pair) in correspondences.withIndex()) {
            val (b, r) = pair // b = catalog, r = observed
            val w = weights[idx]
            // B += w * b * r^T
            // b is column, r is row? Actually b * r^T means B_ij = b_i * r_j
            B[0][0] += w * b.first * r.first
            B[0][1] += w * b.first * r.second
            B[0][2] += w * b.first * r.third
            B[1][0] += w * b.second * r.first
            B[1][1] += w * b.second * r.second
            B[1][2] += w * b.second * r.third
            B[2][0] += w * b.third * r.first
            B[2][1] += w * b.third * r.second
            B[2][2] += w * b.third * r.third
        }

        // S = B + B^T
        val S = Array(3) { DoubleArray(3) }
        for (i in 0..2) {
            for (j in 0..2) {
                S[i][j] = B[i][j] + B[j][i]
            }
        }

        // z = Σ w_i * (b_i × r_i)
        var zx = 0.0
        var zy = 0.0
        var zz = 0.0
        for ((idx, pair) in correspondences.withIndex()) {
            val (b, r) = pair
            val w = weights[idx]
            // cross = b × r
            val cx = b.second * r.third - b.third * r.second
            val cy = b.third * r.first - b.first * r.third
            val cz = b.first * r.second - b.second * r.first
            zx += w * cx
            zy += w * cy
            zz += w * cz
        }

        val sigma = B[0][0] + B[1][1] + B[2][2] // trace(B)

        // Build K matrix 4x4 symmetric
        // K = [ S - sigma*I   z
        //       z^T          sigma ]
        // Ordering: quaternion [x,y,z,w] or [w,x,y,z]? Davenport typically uses [q1,q2,q3,q4] where q4 is scalar
        // We'll use [x,y,z,w] for K, but our Quaternion class is [w,x,y,z], so need to be careful
        // Standard K with [x,y,z,w] ordering:
        // K = [[S - sigma*I, z],
        //      [z^T, sigma]]
        // So K[0..2][0..2] = S - sigma*I, K[0..2][3] = z, K[3][0..2] = z^T, K[3][3] = sigma

        val K = Array(4) { DoubleArray(4) { 0.0 } }
        for (i in 0..2) {
            for (j in 0..2) {
                K[i][j] = S[i][j]
                if (i == j) K[i][j] -= sigma
            }
        }
        K[0][3] = zx
        K[1][3] = zy
        K[2][3] = zz
        K[3][0] = zx
        K[3][1] = zy
        K[3][2] = zz
        K[3][3] = sigma

        // Find largest eigenvalue eigenvector via Jacobi iteration
        val (eigenvalues, eigenvectors) = jacobiEigenDecomposition(K)

        // Find index of largest eigenvalue
        var maxIdx = 0
        var maxVal = eigenvalues[0]
        for (i in 1 until eigenvalues.size) {
            if (eigenvalues[i] > maxVal) {
                maxVal = eigenvalues[i]
                maxIdx = i
            }
        }

        // Eigenvector corresponding to max eigenvalue is optimal quaternion [x,y,z,w]
        val qVec = DoubleArray(4) { eigenvectors[it][maxIdx] } // column maxIdx

        // Convert to our Quaternion(w,x,y,z) ordering
        // qVec is [x,y,z,w] per K construction, so:
        val q = Quaternion(
            w = qVec[3],
            x = qVec[0],
            y = qVec[1],
            z = qVec[2]
        ).normalized()

        return q
    }

    /**
     * TRIAD method for 2 stars fallback.
     * Standard TRIAD construction:
     * Given v1_cat, v2_cat (catalog) and v1_obs, v2_obs (observed), all unit vectors:
     * - t1 = v1 (reference)
     * - t2 = (v1 × v2) / |v1 × v2|
     * - t3 = t1 × t2
     * Build rotation matrix from catalog triad to observed triad, convert to quaternion.
     */
    fun solveTriad(
        v1Cat: Triple<Double, Double, Double>,
        v2Cat: Triple<Double, Double, Double>,
        v1Obs: Triple<Double, Double, Double>,
        v2Obs: Triple<Double, Double, Double>
    ): Quaternion {
        // Catalog triad
        val t1Cat = normalize(v1Cat)
        val t2Cat = normalize(cross(t1Cat, v2Cat))
        val t3Cat = cross(t1Cat, t2Cat)

        // Observed triad
        val t1Obs = normalize(v1Obs)
        val t2Obs = normalize(cross(t1Obs, v2Obs))
        val t3Obs = cross(t1Obs, t2Obs)

        // Rotation matrix: R = ObsTriad * CatTriad^T
        // Each triad as matrix with columns t1,t2,t3
        // R = [t1Obs t2Obs t3Obs] * [t1Cat t2Cat t3Cat]^T

        val catMatrix = arrayOf(
            doubleArrayOf(t1Cat.first, t2Cat.first, t3Cat.first),
            doubleArrayOf(t1Cat.second, t2Cat.second, t3Cat.second),
            doubleArrayOf(t1Cat.third, t2Cat.third, t3Cat.third)
        )

        val obsMatrix = arrayOf(
            doubleArrayOf(t1Obs.first, t2Obs.first, t3Obs.first),
            doubleArrayOf(t1Obs.second, t2Obs.second, t3Obs.second),
            doubleArrayOf(t1Obs.third, t2Obs.third, t3Obs.third)
        )

        // R = obs * cat^T
        val catTransposed = transpose(catMatrix)
        val R = multiplyMatrices(obsMatrix, catTransposed)

        return Quaternion.fromRotationMatrix(R)
    }

    /**
     * Jacobi eigenvalue decomposition for symmetric matrix.
     * Simple, hand-verifiable, auditable choice for 4x4 symmetric.
     * Returns eigenvalues and eigenvectors (eigenvectors as columns).
     *
     * Worked example embedded in comments for hand-verification:
     * Example 4x4 matrix with known eigenvalues:
     *   M = [[2,1,0,0],
     *        [1,2,0,0],
     *        [0,0,3,0],
     *        [0,0,0,4]]
     *   Eigenvalues: 1,3,3,4 (for top-left 2x2 block, eigenvalues 1 and 3)
     *   Jacobi should converge to these.
     */
    fun jacobiEigenDecomposition(
        matrix: Array<DoubleArray>,
        maxIterations: Int = 100,
        tolerance: Double = 1e-10
    ): Pair<DoubleArray, Array<DoubleArray>> {
        val n = matrix.size
        require(matrix.all { it.size == n })

        // Copy matrix
        val A = Array(n) { i -> matrix[i].clone() }
        // Initialize eigenvectors as identity
        val V = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }

        for (iter in 0 until maxIterations) {
            // Find largest off-diagonal element
            var maxOff = 0.0
            var p = 0
            var q = 1
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val absVal = abs(A[i][j])
                    if (absVal > maxOff) {
                        maxOff = absVal
                        p = i
                        q = j
                    }
                }
            }

            if (maxOff < tolerance) break // converged

            // Compute rotation angle
            val app = A[p][p]
            val aqq = A[q][q]
            val apq = A[p][q]

            val theta = 0.5 * atan2(2 * apq, aqq - app)
            val c = cos(theta)
            val s = sin(theta)

            // Rotate A
            for (i in 0 until n) {
                if (i != p && i != q) {
                    val aip = A[i][p]
                    val aiq = A[i][q]
                    A[i][p] = c * aip - s * aiq
                    A[p][i] = A[i][p]
                    A[i][q] = s * aip + c * aiq
                    A[q][i] = A[i][q]
                }
            }

            val appNew = c * c * app - 2 * s * c * apq + s * s * aqq
            val aqqNew = s * s * app + 2 * s * c * apq + c * c * aqq
            A[p][p] = appNew
            A[q][q] = aqqNew
            A[p][q] = 0.0
            A[q][p] = 0.0

            // Rotate V
            for (i in 0 until n) {
                val vip = V[i][p]
                val viq = V[i][q]
                V[i][p] = c * vip - s * viq
                V[i][q] = s * vip + c * viq
            }
        }

        val eigenvalues = DoubleArray(n) { A[it][it] }
        return Pair(eigenvalues, V)
    }

    private fun normalize(v: Triple<Double, Double, Double>): Triple<Double, Double, Double> {
        val norm = sqrt(v.first * v.first + v.second * v.second + v.third * v.third)
        if (norm < 1e-12) return Triple(0.0, 0.0, 0.0)
        return Triple(v.first / norm, v.second / norm, v.third / norm)
    }

    private fun cross(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Triple<Double, Double, Double> {
        return Triple(
            a.second * b.third - a.third * b.second,
            a.third * b.first - a.first * b.third,
            a.first * b.second - a.second * b.first
        )
    }

    private fun transpose(m: Array<DoubleArray>): Array<DoubleArray> {
        val n = m.size
        val result = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                result[j][i] = m[i][j]
            }
        }
        return result
    }

    private fun multiplyMatrices(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val n = a.size
        val result = Array(n) { DoubleArray(n) { 0.0 } }
        for (i in 0 until n) {
            for (j in 0 until n) {
                for (k in 0 until n) {
                    result[i][j] += a[i][k] * b[k][j]
                }
            }
        }
        return result
    }
}
