package com.alijafari.red.astronomy.sandbox.physics

/**
 * Standard 2nd-Order Symplectic Velocity Verlet Integrator (Kick-Drift-Kick).
 * Conserves phase space area and linear/angular momentum.
 */
class VelocityVerletIntegrator(
    private val forceSolver: GravitationalForceSolver
) {
    /**
     * Advances the physics state buffer by dt seconds using the 2nd-order Velocity Verlet scheme.
     */
    fun step(state: PhysicsStateBuffer, dt: Double) {
        val n = state.activeCount
        val px = state.posX
        val py = state.posY
        val pz = state.posZ
        val vx = state.velX
        val vy = state.velY
        val vz = state.velZ
        val ax = state.accX
        val ay = state.accY
        val az = state.accZ
        val isFixed = state.isFixed
        val isActive = state.isActive

        val halfDt = 0.5 * dt

        // Half kick: v += 0.5 * dt * a(t)
        // Full drift: x += dt * v(t + 0.5 dt)
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                vx[i] += halfDt * ax[i]
                vy[i] += halfDt * ay[i]
                vz[i] += halfDt * az[i]

                px[i] += dt * vx[i]
                py[i] += dt * vy[i]
                pz[i] += dt * vz[i]
            }
        }

        // Recompute accelerations at new positions: a(t + dt)
        forceSolver.computeAccelerations(state)

        // Second half kick: v += 0.5 * dt * a(t + dt)
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                vx[i] += halfDt * ax[i]
                vy[i] += halfDt * ay[i]
                vz[i] += halfDt * az[i]
            }
        }
    }
}
