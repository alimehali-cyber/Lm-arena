package com.zig.gravity.physics

import kotlin.math.sqrt

/**
 * §3.4–§3.8 — the N-body engine.
 *
 * Pure Kotlin: zero `android.*` imports, fully JVM-testable (§3.3 binding rule).
 *
 * Force law (§3.4):
 *   a_i = SUM_{j != i} G * m_j * (r_j - r_i) / (|r_j - r_i|^2 + eps^2)^{3/2}
 *
 * Integrator (§3.5): velocity Verlet, KDK ordering, one force evaluation per step.
 */
object NBodyEngine {

    /** Consecutive rollbacks before the caller should auto-pause (§3.8). */
    const val FAILURE_LIMIT: Int = 3

    var consecutiveFailures: Int = 0
        private set

    fun resetFailureCounter() {
        consecutiveFailures = 0
    }

    // ---- §3.4 force pass ----------------------------------------------------------------------

    /**
     * Exact pairwise Plummer-softened Newtonian gravity over all n(n-1)/2 pairs.
     * Massless bodies (test marbles, wormhole mouths) contribute nothing because m_j = 0 —
     * there is no special case (test 29).
     */
    fun computeAccelerations(s: SimArrays) {
        val n = s.n
        for (i in 0 until n) {
            s.ax[i] = 0.0
            s.ay[i] = 0.0
        }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = s.x[j] - s.x[i]
                val dy = s.y[j] - s.y[i]
                val d2 = dx * dx + dy * dy + EngineConstants.EPS_SOFT_SQ
                val d = sqrt(d2)
                val f = EngineConstants.G / (d2 * d)
                val fx = f * dx
                val fy = f * dy
                s.ax[i] += fx * s.mass[j]
                s.ay[i] += fy * s.mass[j]
                s.ax[j] -= fx * s.mass[i]
                s.ay[j] -= fy * s.mass[i]
            }
        }
        s.accelerationsValid = true
    }

    /** §3.5 synchronisation rule: call after every mutating intent, before the next substep. */
    fun ensureAccelerations(s: SimArrays) {
        if (!s.accelerationsValid) computeAccelerations(s)
    }

    // ---- §3.7 velocity bounds ------------------------------------------------------------------

    /** Engine hard clamp, 1000 km/s absolute (test 38). */
    fun clampVelocity(s: SimArrays) {
        val vMax = EngineConstants.V_MAX
        for (i in 0 until s.n) {
            val vx = s.vx[i]
            val vy = s.vy[i]
            val v2 = vx * vx + vy * vy
            if (v2 > vMax * vMax) {
                val scale = vMax / sqrt(v2)
                s.vx[i] = vx * scale
                s.vy[i] = vy * scale
            }
        }
    }

    // ---- §3.6c Adaptive Safety Refinement --------------------------------------------------------

    /**
     * Trigger: any pair whose predicted closing displacement over [dt] exceeds
     * [EngineConstants.REFINE_TRIGGER] x its current separation.
     * s = |v_rel| * dt + 0.5 * |a_rel| * dt^2 — free, the values already exist from the force pass.
     */
    fun needsRefinement(s: SimArrays, dt: Double): Boolean {
        val n = s.n
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = s.x[j] - s.x[i]
                val dy = s.y[j] - s.y[i]
                val sep = sqrt(dx * dx + dy * dy)
                if (sep <= 0.0) return true
                val rvx = s.vx[j] - s.vx[i]
                val rvy = s.vy[j] - s.vy[i]
                val rax = s.ax[j] - s.ax[i]
                val ray = s.ay[j] - s.ay[i]
                val closing = sqrt(rvx * rvx + rvy * rvy) * dt +
                        0.5 * sqrt(rax * rax + ray * ray) * dt * dt
                if (closing > EngineConstants.REFINE_TRIGGER * sep) return true
            }
        }
        return false
    }

    /**
     * Advances one whole DT, refining recursively while the trigger fires, to a maximum depth of
     * [EngineConstants.MAX_REFINE_DEPTH]. Every executed inner step is charged against the
     * caller's substep budget (§3.6c). Whole-step refinement is intentional: deterministic,
     * simple, and momentary in practice.
     *
     * @param budget single-element array holding the remaining inner-step allowance.
     * @return number of inner steps executed.
     */
    fun advance(
        s: SimArrays,
        dt: Double,
        events: MutableList<SimEvent>,
        marbleBounceEnabled: Boolean,
        budget: IntArray
    ): Int {
        ensureAccelerations(s)
        return advanceRecursive(s, dt, 0, events, marbleBounceEnabled, budget)
    }

    private fun advanceRecursive(
        s: SimArrays,
        dt: Double,
        depth: Int,
        events: MutableList<SimEvent>,
        marbleBounceEnabled: Boolean,
        budget: IntArray
    ): Int {
        if (budget[0] <= 0) return 0
        if (depth < EngineConstants.MAX_REFINE_DEPTH && needsRefinement(s, dt)) {
            val half = dt * 0.5
            var used = advanceRecursive(s, half, depth + 1, events, marbleBounceEnabled, budget)
            used += advanceRecursive(s, half, depth + 1, events, marbleBounceEnabled, budget)
            return used
        }
        step(s, dt, events, marbleBounceEnabled)
        budget[0] = budget[0] - 1
        return 1
    }

    // ---- §3.5 the step ---------------------------------------------------------------------------

    /**
     * One velocity-Verlet KDK step. Assumes accelerations are already synchronised.
     * Kinematic bodies (held by a finger) skip integration entirely (test 30) but still take part
     * in the force pass as sources, and still collide.
     */
    fun step(
        s: SimArrays,
        dt: Double,
        events: MutableList<SimEvent>,
        marbleBounceEnabled: Boolean
    ) {
        s.backup()
        val half = dt * 0.5

        // KICK 1/2
        for (i in 0 until s.n) {
            if (s.kinematic[i]) continue
            s.vx[i] += s.ax[i] * half
            s.vy[i] += s.ay[i] * half
        }
        // DRIFT
        for (i in 0 until s.n) {
            if (s.kinematic[i]) continue
            s.x[i] += s.vx[i] * dt
            s.y[i] += s.vy[i] * dt
        }
        // FORCE
        computeAccelerations(s)
        // KICK 1/2
        for (i in 0 until s.n) {
            if (s.kinematic[i]) continue
            s.vx[i] += s.ax[i] * half
            s.vy[i] += s.ay[i] * half
        }

        clampVelocity(s)
        s.simTime += dt

        if (!validateState(s, events)) return

        var mutated = Collision.resolve(s, events, marbleBounceEnabled)
        if (Wormhole.resolve(s, events)) mutated = true
        if (mutated) computeAccelerations(s)
    }

    // ---- §3.8 integrity layer 2 & 3 --------------------------------------------------------------

    /**
     * Scans for NaN/Inf. On failure the whole substep is rolled back — arrays AND simTime — and
     * events appended during the failed substep are discarded (Auditor B14, test 27).
     *
     * @return true when the state is healthy.
     */
    fun validateState(s: SimArrays, events: MutableList<SimEvent>): Boolean {
        var badSlot = -1
        for (i in 0 until s.n) {
            if (!s.x[i].isFinite() || !s.y[i].isFinite() ||
                !s.vx[i].isFinite() || !s.vy[i].isFinite() ||
                !s.ax[i].isFinite() || !s.ay[i].isFinite() ||
                !s.mass[i].isFinite()
            ) {
                badSlot = i
                break
            }
        }
        if (badSlot < 0) {
            consecutiveFailures = 0
            return true
        }

        val offenderId = s.id[badSlot]
        s.restore()
        consecutiveFailures++

        var quarantined = 0L
        if (consecutiveFailures > FAILURE_LIMIT) {
            // Layer 3: quarantine the repeat offender rather than let it poison the pair sums.
            val slot = s.slotOfId(offenderId)
            if (slot >= 0) {
                quarantined = offenderId
                s.removeAt(slot)
                computeAccelerations(s)
            }
            consecutiveFailures = 0
        }

        events.add(
            SimEvent.NumericalFailure(
                simTime = s.simTime,
                rolledBack = true,
                consecutiveFailures = consecutiveFailures,
                quarantinedId = quarantined
            )
        )
        return false
    }

    // ---- diagnostics used by detectors and the HUD ------------------------------------------------

    fun kineticEnergy(s: SimArrays): Double {
        var k = 0.0
        for (i in 0 until s.n) k += 0.5 * s.mass[i] * (s.vx[i] * s.vx[i] + s.vy[i] * s.vy[i])
        return k
    }

    fun potentialEnergy(s: SimArrays): Double {
        var u = 0.0
        for (i in 0 until s.n) {
            for (j in i + 1 until s.n) {
                val dx = s.x[j] - s.x[i]
                val dy = s.y[j] - s.y[i]
                val d = sqrt(dx * dx + dy * dy + EngineConstants.EPS_SOFT_SQ)
                u -= EngineConstants.G * s.mass[i] * s.mass[j] / d
            }
        }
        return u
    }

    fun totalEnergy(s: SimArrays): Double = kineticEnergy(s) + potentialEnergy(s)

    /** Barycentre (centre of mass) in scene metres. Returns [x, y]; origin when massless. */
    fun barycenter(s: SimArrays, out: DoubleArray) {
        var mx = 0.0
        var my = 0.0
        var mt = 0.0
        for (i in 0 until s.n) {
            mx += s.mass[i] * s.x[i]
            my += s.mass[i] * s.y[i]
            mt += s.mass[i]
        }
        if (mt > 0.0) {
            out[0] = mx / mt
            out[1] = my / mt
        } else {
            out[0] = 0.0
            out[1] = 0.0
        }
    }

    /**
     * Circular-orbit velocity for [slot] around the dominant attractor (§3.11 orbit helper):
     * sqrt(GM/r) perpendicular to the separation vector, added to the attractor's own velocity.
     * Writes [vx, vy] into [out]; returns the attractor slot or -1.
     */
    fun circularOrbitVelocity(s: SimArrays, slot: Int, out: DoubleArray): Int {
        val a = s.dominantAttractor(exceptSlot = slot)
        if (a < 0 || s.mass[a] <= 0.0) return -1
        val dx = s.x[slot] - s.x[a]
        val dy = s.y[slot] - s.y[a]
        val r = sqrt(dx * dx + dy * dy)
        if (r <= 0.0) return -1
        val v = EngineConstants.circularSpeed(s.mass[a], r)
        // Counter-clockwise perpendicular of the outward radial unit vector.
        out[0] = s.vx[a] + (-dy / r) * v
        out[1] = s.vy[a] + (dx / r) * v
        return a
    }
}
