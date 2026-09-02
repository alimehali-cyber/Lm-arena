package com.alijafari.red.astronomy.startracker.tracking

import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.solver.Vec3
import kotlin.math.*

/**
 * Pure gyro-integration math.
 * Integrates current attitude quaternion given angular velocity rad/s and dt.
 * Uses exponential map: delta_q = [cos(|w|*dt/2), (w/|w|)*sin(|w|*dt/2)]
 * Then new_q = current * delta_q, with renormalization.
 */
class QuaternionIntegrator {

    fun integrate(
        currentAttitude: Quaternion,
        angularVelocityRadPerSec: Vec3,
        dtSeconds: Double
    ): Quaternion {
        val wx = angularVelocityRadPerSec.x
        val wy = angularVelocityRadPerSec.y
        val wz = angularVelocityRadPerSec.z

        val omegaMag = sqrt(wx*wx + wy*wy + wz*wz)

        val deltaQ = if (omegaMag < 1e-12) {
            // No rotation
            Quaternion.identity()
        } else {
            val halfAngle = omegaMag * dtSeconds / 2.0
            val sinHalf = sin(halfAngle)
            val cosHalf = cos(halfAngle)
            val invMag = 1.0 / omegaMag
            Quaternion(
                w = cosHalf,
                x = wx * invMag * sinHalf,
                y = wy * invMag * sinHalf,
                z = wz * invMag * sinHalf
            )
        }

        // new = current * delta
        val newQ = currentAttitude.multiply(deltaQ).normalized()

        // Explicit renormalization to avoid numerical drift
        return newQ.normalized()
    }
}
