package com.alijafari.red.astronomy.sandbox.physics

import com.alijafari.red.astronomy.sandbox.model.Vector3D
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Diagnostic metrics snapshot capturing physical conservation laws and system invariants.
 */
data class DiagnosticsSnapshot(
    val totalMassKg: Double,
    val kineticEnergyJoules: Double,
    val potentialEnergyJoules: Double,
    val totalEnergyJoules: Double,
    val energyDriftFraction: Double,
    val totalLinearMomentum: Vector3D,
    val totalAngularMomentum: Vector3D,
    val centerOfMassPosition: Vector3D,
    val centerOfMassVelocity: Vector3D,
    val minPairDistanceMeters: Double,
    val minProximityRatio: Double
)

/**
 * High-precision diagnostics engine measuring real numerical drift rather than assuming conservation.
 */
class PhysicsDiagnostics(
    var gravitationalConstant: Double = AstroPhysicsConstants.G,
    var softeningMeters: Double = GravitationalForceSolver.DEFAULT_SOFTENING_METERS
) {
    private var initialEnergyJoules: Double? = null

    fun resetInitialEnergy() {
        initialEnergyJoules = null
    }

    fun setInitialEnergy(energyJoules: Double) {
        initialEnergyJoules = energyJoules
    }

    fun computeDiagnostics(
        state: PhysicsStateBuffer,
        potentialEnergyFromSolver: Double? = null,
        minDistanceMeters: Double = Double.MAX_VALUE,
        minProximityRatio: Double = Double.MAX_VALUE
    ): DiagnosticsSnapshot {
        val n = state.activeCount
        val px = state.posX
        val py = state.posY
        val pz = state.posZ
        val vx = state.velX
        val vy = state.velY
        val vz = state.velZ
        val m = state.mass
        val isActive = state.isActive

        var totalMass = 0.0
        var totalEk = 0.0
        var pX = 0.0; var pY = 0.0; var pZ = 0.0
        var lX = 0.0; var lY = 0.0; var lZ = 0.0
        var cmX = 0.0; var cmY = 0.0; var cmZ = 0.0

        for (i in 0 until n) {
            if (!isActive[i]) continue
            val mi = m[i]
            val pxi = px[i]; val pyi = py[i]; val pzi = pz[i]
            val vxi = vx[i]; val vyi = vy[i]; val vzi = vz[i]

            totalMass += mi

            // Kinetic energy Ek = 0.5 * m * v^2
            val v2 = vxi * vxi + vyi * vyi + vzi * vzi
            totalEk += 0.5 * mi * v2

            // Linear momentum P = m * v
            val pxVal = mi * vxi
            val pyVal = mi * vyi
            val pzVal = mi * vzi
            pX += pxVal; pY += pyVal; pZ += pzVal

            // Center of mass accumulation
            cmX += mi * pxi
            cmY += mi * pyi
            cmZ += mi * pzi

            // Angular momentum L = r x (m * v)
            lX += (pyi * pzVal - pzi * pyVal)
            lY += (pzi * pxVal - pxi * pzVal)
            lZ += (pxi * pyVal - pyi * pxVal)
        }

        val totalEp = potentialEnergyFromSolver ?: computePotentialEnergy(state)
        val totalE = totalEk + totalEp

        if (initialEnergyJoules == null && totalMass > 0.0 && abs(totalE) > 1e-15) {
            initialEnergyJoules = totalE
        }

        val e0 = initialEnergyJoules
        val energyDrift = if (e0 != null && abs(e0) > 1e-15) {
            (totalE - e0) / abs(e0)
        } else {
            0.0
        }

        val cmPos = if (totalMass > 0.0) Vector3D(cmX / totalMass, cmY / totalMass, cmZ / totalMass) else Vector3D.ZERO
        val cmVel = if (totalMass > 0.0) Vector3D(pX / totalMass, pY / totalMass, pZ / totalMass) else Vector3D.ZERO

        return DiagnosticsSnapshot(
            totalMassKg = totalMass,
            kineticEnergyJoules = totalEk,
            potentialEnergyJoules = totalEp,
            totalEnergyJoules = totalE,
            energyDriftFraction = energyDrift,
            totalLinearMomentum = Vector3D(pX, pY, pZ),
            totalAngularMomentum = Vector3D(lX, lY, lZ),
            centerOfMassPosition = cmPos,
            centerOfMassVelocity = cmVel,
            minPairDistanceMeters = minDistanceMeters,
            minProximityRatio = minProximityRatio
        )
    }

    private fun computePotentialEnergy(state: PhysicsStateBuffer): Double {
        val n = state.activeCount
        val px = state.posX
        val py = state.posY
        val pz = state.posZ
        val m = state.mass
        val isActive = state.isActive
        val eps2 = softeningMeters * softeningMeters
        val g = gravitationalConstant

        var ep = 0.0
        for (i in 0 until n) {
            if (!isActive[i]) continue
            for (j in i + 1 until n) {
                if (!isActive[j]) continue
                val dx = px[j] - px[i]
                val dy = py[j] - py[i]
                val dz = pz[j] - pz[i]
                val rSoftened = sqrt(dx * dx + dy * dy + dz * dz + eps2)
                ep -= (g * m[i] * m[j]) / rSoftened
            }
        }
        return ep
    }
}
