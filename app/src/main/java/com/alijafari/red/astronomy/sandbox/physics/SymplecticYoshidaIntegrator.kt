package com.alijafari.red.astronomy.sandbox.physics

import kotlin.math.pow

/**
 * 4th-Order Symplectic Yoshida Integrator for N-body Gravitational Systems.
 *
 * Mathematically derived by composing three 2nd-order symplectic maps with optimal weights
 * w1 = 1 / (2 - 2^(1/3)), w0 = - 2^(1/3) / (2 - 2^(1/3)).
 *
 * Properties:
 * - Symplectic: Conserves phase-space volume and Poincaré invariants.
 * - Time-reversible: Exactly symmetric under time inversion.
 * - Bounded, zero-mean oscillatory energy drift over thousands of orbital periods.
 * - Zero heap allocations during step execution.
 */
class SymplecticYoshidaIntegrator(
    private val forceSolver: GravitationalForceSolver
) {
    // Yoshida 4th-order composition coefficients
    private val w1: Double = 1.0 / (2.0 - 2.0.pow(1.0 / 3.0))
    private val w0: Double = 1.0 - 2.0 * w1

    private val c1: Double = w1 / 2.0
    private val c2: Double = (w0 + w1) / 2.0
    private val c3: Double = c2
    private val c4: Double = c1

    private val d1: Double = w1
    private val d2: Double = w0
    private val d3: Double = w1

    /**
     * Advances the physics state buffer by dt seconds using the 4th-order symplectic scheme.
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

        // Sub-stage 1: Drift by c1 * dt
        val dtC1 = c1 * dt
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                px[i] += dtC1 * vx[i]
                py[i] += dtC1 * vy[i]
                pz[i] += dtC1 * vz[i]
            }
        }

        // Sub-stage 1: Kick by d1 * dt
        forceSolver.computeAccelerations(state)
        val dtD1 = d1 * dt
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                vx[i] += dtD1 * ax[i]
                vy[i] += dtD1 * ay[i]
                vz[i] += dtD1 * az[i]
            }
        }

        // Sub-stage 2: Drift by c2 * dt
        val dtC2 = c2 * dt
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                px[i] += dtC2 * vx[i]
                py[i] += dtC2 * vy[i]
                pz[i] += dtC2 * vz[i]
            }
        }

        // Sub-stage 2: Kick by d2 * dt
        forceSolver.computeAccelerations(state)
        val dtD2 = d2 * dt
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                vx[i] += dtD2 * ax[i]
                vy[i] += dtD2 * ay[i]
                vz[i] += dtD2 * az[i]
            }
        }

        // Sub-stage 3: Drift by c3 * dt
        val dtC3 = c3 * dt
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                px[i] += dtC3 * vx[i]
                py[i] += dtC3 * vy[i]
                pz[i] += dtC3 * vz[i]
            }
        }

        // Sub-stage 3: Kick by d3 * dt
        forceSolver.computeAccelerations(state)
        val dtD3 = d3 * dt
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                vx[i] += dtD3 * ax[i]
                vy[i] += dtD3 * ay[i]
                vz[i] += dtD3 * az[i]
            }
        }

        // Sub-stage 4: Final Drift by c4 * dt
        val dtC4 = c4 * dt
        for (i in 0 until n) {
            if (isActive[i] && !isFixed[i]) {
                px[i] += dtC4 * vx[i]
                py[i] += dtC4 * vy[i]
                pz[i] += dtC4 * vz[i]
            }
        }

        // Final acceleration sync for diagnostics and subsequent step initialization
        forceSolver.computeAccelerations(state)
    }
}
