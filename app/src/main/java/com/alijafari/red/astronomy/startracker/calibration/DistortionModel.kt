package com.alijafari.red.astronomy.startracker.calibration

import kotlin.math.*

/**
 * Brown-Conrady distortion model: radial k1,k2 + tangential p1,p2
 *
 * Given normalized ideal (undistorted) coordinates (x,y) relative to principal point:
 *   r2 = x^2 + y^2
 *   r4 = r2^2
 *   radial = 1 + k1*r2 + k2*r4
 *   x_dist = x*radial + 2*p1*x*y + p2*(r2 + 2*x^2)
 *   y_dist = y*radial + p1*(r2 + 2*y^2) + 2*p2*x*y
 *
 * Pixel coordinates:
 *   u_dist = fx * x_dist + skew * y_dist + cx
 *   v_dist = fy * y_dist + cy
 *   For simplicity in this phase, skew is often 0, but included for completeness
 *
 * Undistort: given distorted (x_dist, y_dist), find ideal (x,y) via iterative refinement
 * Standard approach: start with x=x_dist, y=y_dist, iterate:
 *   compute distorted of current ideal, compute residual vs observed distorted, update
 *   Repeat fixed iterations (e.g., 10) or until residual < threshold
 */

class DistortionModel(
    val k1: Double = 0.0,
    val k2: Double = 0.0,
    val p1: Double = 0.0,
    val p2: Double = 0.0
) {

    /**
     * Distort ideal normalized coordinates to distorted normalized coordinates
     */
    fun distortIdealToDistortedNormalized(x: Double, y: Double): Pair<Double, Double> {
        val r2 = x * x + y * y
        val r4 = r2 * r2
        val radial = 1.0 + k1 * r2 + k2 * r4

        val xDist = x * radial + 2.0 * p1 * x * y + p2 * (r2 + 2.0 * x * x)
        val yDist = y * radial + p1 * (r2 + 2.0 * y * y) + 2.0 * p2 * x * y

        return Pair(xDist, yDist)
    }

    /**
     * Undistort distorted normalized coordinates to ideal normalized coordinates
     * via iterative refinement (standard approach)
     * @param xDist distorted x
     * @param yDist distorted y
     * @param maxIterations iteration count (default 20, conservative)
     * @param epsilon convergence criterion (default 1e-8)
     * @return ideal (x,y)
     */
    fun undistortDistortedToIdealNormalized(
        xDist: Double,
        yDist: Double,
        maxIterations: Int = 20,
        epsilon: Double = 1e-8
    ): Pair<Double, Double> {
        // Start with distorted as initial guess for ideal
        var x = xDist
        var y = yDist

        for (iter in 0 until maxIterations) {
            val (xDistEst, yDistEst) = distortIdealToDistortedNormalized(x, y)

            val dx = xDist - xDistEst
            val dy = yDist - yDistEst

            // Check convergence
            if (abs(dx) < epsilon && abs(dy) < epsilon) {
                break
            }

            // Update: simple fixed-point iteration, add residual
            // More sophisticated would use Jacobian, but fixed-point works for mild distortion
            x += dx
            y += dy
        }

        return Pair(x, y)
    }

    /**
     * Distort ideal pixel coordinates to distorted pixel coordinates using intrinsics
     * @param uIdeal ideal pixel x
     * @param vIdeal ideal pixel y
     * @param fx,fy,cx,cy,skew intrinsics
     * @return distorted pixel (uDist, vDist)
     */
    fun distortPixel(
        uIdeal: Double,
        vIdeal: Double,
        fx: Double,
        fy: Double,
        cx: Double,
        cy: Double,
        skew: Double = 0.0
    ): Pair<Double, Double> {
        // Convert ideal pixel to ideal normalized
        // u = fx*x + skew*y + cx, v = fy*y + cy
        // Solve for x,y: y = (v - cy)/fy, x = (u - cx - skew*y)/fx
        val yIdeal = (vIdeal - cy) / fy
        val xIdeal = (uIdeal - cx - skew * yIdeal) / fx

        val (xDist, yDist) = distortIdealToDistortedNormalized(xIdeal, yIdeal)

        val uDist = fx * xDist + skew * yDist + cx
        val vDist = fy * yDist + cy

        return Pair(uDist, vDist)
    }

    /**
     * Undistort distorted pixel coordinates to ideal pixel coordinates
     */
    fun undistortPixel(
        uDist: Double,
        vDist: Double,
        fx: Double,
        fy: Double,
        cx: Double,
        cy: Double,
        skew: Double = 0.0
    ): Pair<Double, Double> {
        // Convert distorted pixel to distorted normalized
        val yDistNorm = (vDist - cy) / fy
        val xDistNorm = (uDist - cx - skew * yDistNorm) / fx

        val (xIdeal, yIdeal) = undistortDistortedToIdealNormalized(xDistNorm, yDistNorm)

        val uIdeal = fx * xIdeal + skew * yIdeal + cx
        val vIdeal = fy * yIdeal + cy

        return Pair(uIdeal, vIdeal)
    }

    companion object {
        fun noDistortion(): DistortionModel = DistortionModel(0.0, 0.0, 0.0, 0.0)
    }
}
