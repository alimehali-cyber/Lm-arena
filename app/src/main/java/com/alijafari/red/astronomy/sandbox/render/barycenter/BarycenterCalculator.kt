package com.alijafari.red.astronomy.sandbox.render.barycenter

import com.alijafari.red.astronomy.sandbox.model.Vector3D
import com.alijafari.red.astronomy.sandbox.snapshot.BodyRenderState
import kotlin.math.sqrt

/**
 * Result data holder for calculated Center of Mass metrics.
 */
data class BarycenterInfo(
    val positionPhysicsMeters: Vector3D = Vector3D.ZERO,
    val totalMassKg: Double = 0.0,
    val activeBodyCount: Int = 0
)

/**
 * Calculates instantaneous Center of Mass (Barycenter) for N-body systems:
 * R_com = (sum m_i * r_i) / (sum m_i)
 */
class BarycenterCalculator {

    /**
     * Computes the system-wide center of mass from active bodies in a render snapshot.
     */
    fun computeBarycenter(bodies: List<BodyRenderState>): BarycenterInfo {
        if (bodies.isEmpty()) {
            return BarycenterInfo()
        }

        var totalMass = 0.0
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        var activeCount = 0

        for (i in bodies.indices) {
            val b = bodies[i]
            if (!b.isActive || b.massKg <= 0.0) continue
            val m = b.massKg
            totalMass += m
            sumX += m * b.posX
            sumY += m * b.posY
            sumZ += m * b.posZ
            activeCount++
        }

        if (totalMass <= 0.0 || activeCount == 0) {
            return BarycenterInfo()
        }

        val invMass = 1.0 / totalMass
        return BarycenterInfo(
            positionPhysicsMeters = Vector3D(sumX * invMass, sumY * invMass, sumZ * invMass),
            totalMassKg = totalMass,
            activeBodyCount = activeCount
        )
    }

    /**
     * Calculates distance from a specific body to the barycenter in meters.
     */
    fun computeDistanceToBarycenter(body: BodyRenderState, barycenter: BarycenterInfo): Double {
        val dx = body.posX - barycenter.positionPhysicsMeters.x
        val dy = body.posY - barycenter.positionPhysicsMeters.y
        val dz = body.posZ - barycenter.positionPhysicsMeters.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
