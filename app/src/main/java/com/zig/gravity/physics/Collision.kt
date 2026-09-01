package com.zig.gravity.physics

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * §3.7 Collision & merger model. Pure functions over [SimArrays] — no Android, no allocation
 * beyond the event objects themselves.
 */
object Collision {

    /** Opt-in marble restitution (§3.7). */
    const val MARBLE_RESTITUTION: Double = 0.4

    /** Positional de-penetration split applied after a bounce (§3.7). */
    const val PENETRATION_SPLIT: Double = 0.8

    /**
     * Deterministic survivor rule (§3.7 / Auditor A3).
     *
     * Called with lo < hi so "lower array slot" is always [lo].
     * - a BLACK_HOLE always survives over a non-hole, regardless of mass ordering;
     * - otherwise the larger mass survives;
     * - exact tie -> the lower array slot survives.
     *
     * @return survivor slot first, absorbed slot second.
     */
    fun survivorOf(s: SimArrays, lo: Int, hi: Int): IntArray {
        val tLo = s.typeOf(lo)
        val tHi = s.typeOf(hi)
        val loHole = tLo == BodyType.BLACK_HOLE
        val hiHole = tHi == BodyType.BLACK_HOLE
        if (loHole && !hiHole) return intArrayOf(lo, hi)
        if (hiHole && !loHole) return intArrayOf(hi, lo)
        return when {
            s.mass[lo] > s.mass[hi] -> intArrayOf(lo, hi)
            s.mass[hi] > s.mass[lo] -> intArrayOf(hi, lo)
            else -> intArrayOf(lo, hi) // tie -> lower slot
        }
    }

    /**
     * Impulse magnitude for a 1D/normal elastic-with-restitution bounce (§3.7):
     * j = -(1+e) * v_n / (1/m1 + 1/m2)
     *
     * Two massless test marbles are the mass-symmetric limit of that expression, which is
     * independent of the (equal) mass, so equal unit inverse masses are used for them. This is
     * the limit of the specified formula, not a new tunable.
     */
    fun bounceImpulse(m1: Double, m2: Double, normalRelativeVelocity: Double, e: Double): Double {
        val invSum = if (m1 <= 0.0 && m2 <= 0.0) 2.0 else (safeInv(m1) + safeInv(m2))
        if (invSum <= 0.0) return 0.0
        return -(1.0 + e) * normalRelativeVelocity / invSum
    }

    private fun safeInv(m: Double): Double = if (m <= 0.0) 1.0 else 1.0 / m

    /**
     * Resolves every overlapping pair, repeating until the configuration is clean so chain
     * reactions settle inside one substep.
     *
     * @return true if any body was merged, bounced or removed (the caller must then recompute
     *         accelerations to keep the Verlet synchronised).
     */
    fun resolve(s: SimArrays, events: MutableList<SimEvent>, marbleBounceEnabled: Boolean): Boolean {
        var mutated = false
        var guard = 0
        while (guard++ < EngineConstants.MAX_BODIES * 2) {
            var found = false
            var i = 0
            outer@ while (i < s.n) {
                var j = i + 1
                while (j < s.n) {
                    if (overlaps(s, i, j)) {
                        val tI = s.typeOf(i)
                        val tJ = s.typeOf(j)
                        val bothMarbles = tI == BodyType.TEST_MARBLE && tJ == BodyType.TEST_MARBLE
                        // §16: a black hole present in the pair always resolves as capture, never
                        // as an ordinary bounce, whatever the other body is.
                        val hole = tI == BodyType.BLACK_HOLE || tJ == BodyType.BLACK_HOLE
                        val willBounce = marbleBounceEnabled && bothMarbles && !hole
                        emitImpact(s, i, j, events, merged = !willBounce)
                        if (willBounce) {
                            bounce(s, i, j, events)
                        } else {
                            merge(s, i, j, events)
                        }
                        mutated = true
                        found = true
                        break@outer
                    }
                    j++
                }
                i++
            }
            if (!found) break
        }
        return mutated
    }

    /**
     * §12 — publishes the energy of a contact before its outcome is applied.
     *
     * The closing speed is taken along the contact normal (the component that actually collides);
     * it is compared with the pair's mutual escape speed, which is the only velocity scale the
     * configuration itself provides.
     */
    fun emitImpact(s: SimArrays, i: Int, j: Int, events: MutableList<SimEvent>, merged: Boolean) {
        var dx = s.x[j] - s.x[i]
        var dy = s.y[j] - s.y[i]
        var dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0.0) {
            dx = 1.0; dy = 0.0; dist = 1.0
        }
        val nx = dx / dist
        val ny = dy / dist
        val rvx = s.vx[j] - s.vx[i]
        val rvy = s.vy[j] - s.vy[i]
        val closing = -(rvx * nx + rvy * ny)
        val relSpeed = if (closing > 0.0) closing else sqrt(rvx * rvx + rvy * rvy)

        val contact = s.radius[i] + s.radius[j]
        val mSum = s.mass[i] + s.mass[j]
        val vEsc = if (mSum > 0.0 && contact > 0.0) {
            sqrt(2.0 * EngineConstants.G * mSum / contact)
        } else {
            0.0
        }

        events.add(
            SimEvent.CollisionImpact(
                simTime = s.simTime,
                aId = s.id[i],
                bId = s.id[j],
                x = 0.5 * (s.x[i] + s.x[j]),
                y = 0.5 * (s.y[i] + s.y[j]),
                relativeSpeed = relSpeed,
                mutualEscapeSpeed = vEsc,
                contactRadius = contact,
                tier = ImpactTier.of(relSpeed, vEsc),
                merged = merged
            )
        )
    }

    private fun overlaps(s: SimArrays, i: Int, j: Int): Boolean {
        // Wormhole mouths never collide (§3.7): they trigger teleport instead, and mouth+mouth
        // never interacts at all.
        if (s.typeOf(i) == BodyType.WORMHOLE_MOUTH || s.typeOf(j) == BodyType.WORMHOLE_MOUTH) return false
        val dx = s.x[j] - s.x[i]
        val dy = s.y[j] - s.y[i]
        val contact = s.radius[i] + s.radius[j]
        return dx * dx + dy * dy < contact * contact
    }

    /** Mass-, momentum- and volume-conserving merge (§3.7). */
    fun merge(s: SimArrays, lo: Int, hi: Int, events: MutableList<SimEvent>) {
        val pick = survivorOf(s, lo, hi)
        val sv = pick[0]
        val ab = pick[1]

        val m1 = s.mass[sv]
        val m2 = s.mass[ab]
        val mSum = m1 + m2

        val pxBefore = m1 * s.vx[sv] + m2 * s.vx[ab]
        val pyBefore = m1 * s.vy[sv] + m2 * s.vy[ab]

        val newVx: Double
        val newVy: Double
        val newX: Double
        val newY: Double
        if (mSum > 0.0) {
            newVx = pxBefore / mSum
            newVy = pyBefore / mSum
            newX = (m1 * s.x[sv] + m2 * s.x[ab]) / mSum
            newY = (m1 * s.y[sv] + m2 * s.y[ab]) / mSum
        } else {
            // Two massless bodies: the mass-weighted formulas degenerate; use the symmetric limit.
            newVx = 0.5 * (s.vx[sv] + s.vx[ab])
            newVy = 0.5 * (s.vy[sv] + s.vy[ab])
            newX = 0.5 * (s.x[sv] + s.x[ab])
            newY = 0.5 * (s.y[sv] + s.y[ab])
        }

        // Volume conservation under the equal-density assumption (§3.4 assumption 3).
        val newDp = Math.cbrt(s.radiusDp[sv] * s.radiusDp[sv] * s.radiusDp[sv] +
                s.radiusDp[ab] * s.radiusDp[ab] * s.radiusDp[ab])

        val survivorType = s.typeOf(sv)
        val absorbedType = s.typeOf(ab)
        val survivorId = s.id[sv]
        val absorbedId = s.id[ab]

        s.mass[sv] = mSum
        s.vx[sv] = newVx
        s.vy[sv] = newVy
        s.x[sv] = newX
        s.y[sv] = newY
        s.setRadiusDpRaw(sv, newDp)

        // The absorbed body's trail disappears with it; the survivor's trail is cut at the merge
        // point because the centre-of-mass jump is a genuine discontinuity (§9 of the brief).
        s.trails[sv].clear()

        val subtype = when {
            survivorType == BodyType.BLACK_HOLE && absorbedType == BodyType.BLACK_HOLE -> MergeSubtype.BH_BH
            survivorType == BodyType.BLACK_HOLE -> MergeSubtype.BH_ABSORB
            else -> MergeSubtype.NORMAL
        }

        events.add(
            SimEvent.BodyMerged(
                simTime = s.simTime,
                survivorId = survivorId,
                absorbedId = absorbedId,
                survivorType = survivorType,
                absorbedType = absorbedType,
                momentumBefore = sqrt(pxBefore * pxBefore + pyBefore * pyBefore),
                momentumAfter = sqrt(
                    (mSum * newVx) * (mSum * newVx) + (mSum * newVy) * (mSum * newVy)
                ),
                massBefore = mSum,
                massAfter = mSum,
                subtype = subtype
            )
        )
        if (subtype == MergeSubtype.BH_ABSORB || subtype == MergeSubtype.BH_BH) {
            events.add(
                SimEvent.BlackHoleCapture(
                    simTime = s.simTime,
                    holeId = survivorId,
                    capturedId = absorbedId,
                    holeMassAfter = mSum
                )
            )
        }
        s.removeAt(ab)
    }

    /** Opt-in marble bounce (§3.7), restitution 0.4 plus an 0.8x positional de-penetration split. */
    fun bounce(s: SimArrays, i: Int, j: Int, events: MutableList<SimEvent>) {
        var dx = s.x[j] - s.x[i]
        var dy = s.y[j] - s.y[i]
        var dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0.0) {
            dx = 1.0; dy = 0.0; dist = 1.0
        }
        val nx = dx / dist
        val ny = dy / dist

        val rvx = s.vx[j] - s.vx[i]
        val rvy = s.vy[j] - s.vy[i]
        val vn = rvx * nx + rvy * ny
        if (vn > 0.0) return // already separating

        val jImp = bounceImpulse(s.mass[i], s.mass[j], vn, MARBLE_RESTITUTION)
        val invI = safeInv(s.mass[i])
        val invJ = safeInv(s.mass[j])

        s.vx[i] -= jImp * nx * invI
        s.vy[i] -= jImp * ny * invI
        s.vx[j] += jImp * nx * invJ
        s.vy[j] += jImp * ny * invJ

        val penetration = (s.radius[i] + s.radius[j]) - dist
        if (penetration > 0.0) {
            val invSum = invI + invJ
            val corr = PENETRATION_SPLIT * penetration / invSum
            s.x[i] -= corr * invI * nx
            s.y[i] -= corr * invI * ny
            s.x[j] += corr * invJ * nx
            s.y[j] += corr * invJ * ny
        }

        events.add(
            SimEvent.BodyBounced(
                simTime = s.simTime,
                aId = s.id[i],
                bId = s.id[j],
                restitution = MARBLE_RESTITUTION
            )
        )
    }

    /** Total linear momentum magnitude — used by conservation tests and the teaching layer. */
    fun totalMomentum(s: SimArrays): DoubleArray {
        var px = 0.0
        var py = 0.0
        for (i in 0 until s.n) {
            px += s.mass[i] * s.vx[i]
            py += s.mass[i] * s.vy[i]
        }
        return doubleArrayOf(px, py)
    }

    fun approximately(a: Double, b: Double, relTolerance: Double): Boolean {
        val scale = maxOf(abs(a), abs(b), 1.0e-30)
        return abs(a - b) / scale <= relTolerance
    }
}
