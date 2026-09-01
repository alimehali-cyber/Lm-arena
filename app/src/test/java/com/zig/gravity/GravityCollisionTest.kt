package com.zig.gravity

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.Collision
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.EngineConstants.M_EARTH
import com.zig.gravity.physics.EngineConstants.M_SUN
import com.zig.gravity.physics.MergeSubtype
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.SimEvent
import com.zig.gravity.physics.Wormhole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * §3.16 tests 14-27, 30, 39, 40 — collisions, special bodies and integrity.
 */
class GravityCollisionTest {

    private val mpd = 1.122e9

    private fun arrays(): SimArrays {
        val s = SimArrays()
        s.setMetersPerDp(mpd)
        return s
    }

    /** Places two bodies already overlapping so the merge path runs deterministically. */
    private fun overlappingPair(
        m1: Double, m2: Double,
        v1x: Double, v2x: Double,
        t1: BodyType = BodyType.PLANET,
        t2: BodyType = BodyType.PLANET,
        dp1: Double = 10.0, dp2: Double = 10.0
    ): SimArrays {
        val s = arrays()
        s.add(t1, m1, dp1, 0.0, 0.0, v1x, 0.0)
        s.add(t2, m2, dp2, dp1 * mpd * 0.5, 0.0, v2x, 0.0)
        return s
    }

    // ---- 14, 15, 16 -----------------------------------------------------------------------------
    @Test
    fun mergeConservesMomentumMassAndVelocityFormula() {
        val m1 = 3.0e24
        val m2 = 1.0e24
        val v1 = 900.0
        val v2 = -300.0
        val s = overlappingPair(m1, m2, v1, v2)
        val ev = ArrayList<SimEvent>()
        val pBefore = m1 * v1 + m2 * v2

        assertTrue(Collision.resolve(s, ev, false))
        assertEquals(1, s.n)
        assertEquals("mass", m1 + m2, s.mass[0], 1.0e-6)
        assertEquals("momentum", pBefore, s.mass[0] * s.vx[0], abs(pBefore) * 1.0e-12)
        assertEquals("velocity formula", pBefore / (m1 + m2), s.vx[0], 1.0e-9)
        assertTrue(ev.any { it is SimEvent.BodyMerged })
    }

    // ---- 17 -------------------------------------------------------------------------------------
    @Test
    fun mergeRadiusVolumeConserving() {
        val s = overlappingPair(2.0e24, 1.0e24, 0.0, 0.0, dp1 = 10.0, dp2 = 8.0)
        Collision.resolve(s, ArrayList(), false)
        val expected = Math.cbrt(10.0 * 10.0 * 10.0 + 8.0 * 8.0 * 8.0)
        assertEquals(expected, s.radiusDp[0], 1.0e-9)
        // The derived scene radius must follow the dp authority exactly.
        assertEquals(s.radiusDp[0] * mpd, s.radius[0], 1.0e-6)
    }

    // ---- 18 -------------------------------------------------------------------------------------
    @Test
    fun extremeMassRatioAbsorb() {
        val star = M_SUN
        val pebble = 1.0e12
        val s = overlappingPair(star, pebble, 0.0, 5.0e4, BodyType.SUN, BodyType.ASTEROID, 26.0, 4.0)
        Collision.resolve(s, ArrayList(), false)
        assertEquals(1, s.n)
        assertEquals(BodyType.SUN, s.typeOf(0))
        // "The pebble vanishes into the star and the star barely moves."
        assertTrue(abs(s.vx[0]) < 1.0e-3)
    }

    // ---- 19 -------------------------------------------------------------------------------------
    @Test
    fun bounceImpulseMath1D() {
        val m1 = 2.0
        val m2 = 3.0
        val vn = -10.0
        val e = Collision.MARBLE_RESTITUTION
        val j = Collision.bounceImpulse(m1, m2, vn, e)
        val expected = -(1.0 + e) * vn / (1.0 / m1 + 1.0 / m2)
        assertEquals(expected, j, 1.0e-12)
        // The impulse changes each velocity; the *resulting* relative normal velocity must be
        // the approach speed reversed and scaled by the restitution.
        val dv1 = -j / m1
        val dv2 = j / m2
        assertEquals(-e * vn, vn + (dv2 - dv1), 1.0e-12)
    }

    // ---- 20 -------------------------------------------------------------------------------------
    @Test
    fun bounceMomentumConserved2D() {
        val s = arrays()
        s.add(BodyType.TEST_MARBLE, 0.0, 5.0, 0.0, 0.0, 400.0, 120.0)
        s.add(BodyType.TEST_MARBLE, 0.0, 5.0, 4.0 * mpd, 1.0 * mpd, -260.0, -60.0)
        val vx0 = s.vx[0] + s.vx[1]
        val vy0 = s.vy[0] + s.vy[1]
        val ev = ArrayList<SimEvent>()
        assertTrue(Collision.resolve(s, ev, true))
        assertEquals("bounce must not merge", 2, s.n)
        // Equal effective masses: the summed velocity is the conserved quantity.
        assertEquals(vx0, s.vx[0] + s.vx[1], 1.0e-9)
        assertEquals(vy0, s.vy[0] + s.vy[1], 1.0e-9)
        assertTrue(ev.any { it is SimEvent.BodyBounced })
    }

    // ---- 40 -------------------------------------------------------------------------------------
    @Test
    fun bhBhMergeDeterministicSurvivor() {
        // Equal masses -> the lower array slot survives.
        run {
            val s = overlappingPair(
                10.0 * M_SUN, 10.0 * M_SUN, 0.0, 0.0,
                BodyType.BLACK_HOLE, BodyType.BLACK_HOLE, 14.0, 14.0
            )
            val idLow = s.id[0]
            val ev = ArrayList<SimEvent>()
            Collision.resolve(s, ev, false)
            assertEquals(1, s.n)
            assertEquals(idLow, s.id[0])
            assertEquals(Math.cbrt(2.0 * 14.0 * 14.0 * 14.0), s.radiusDp[0], 1.0e-9)
            val merged = ev.filterIsInstance<SimEvent.BodyMerged>().first()
            assertEquals(MergeSubtype.BH_BH, merged.subtype)
            assertEquals(merged.momentumBefore, merged.momentumAfter, 1.0e-6)
        }
        // Unequal masses -> the larger mass survives even from the higher slot.
        run {
            val s = overlappingPair(
                5.0 * M_SUN, 40.0 * M_SUN, 0.0, 0.0,
                BodyType.BLACK_HOLE, BodyType.BLACK_HOLE, 12.0, 18.0
            )
            val idBig = s.id[1]
            Collision.resolve(s, ArrayList(), false)
            assertEquals(1, s.n)
            assertEquals(idBig, s.id[0])
        }
    }

    @Test
    fun blackHoleAlwaysSurvivesOverNonHole() {
        // A tiny hole meeting a whole star must still be the survivor (§3.7).
        val s = overlappingPair(
            M_SUN, 1.0 * M_SUN, 0.0, 0.0,
            BodyType.SUN, BodyType.BLACK_HOLE, 26.0, 14.0
        )
        val holeId = s.id[1]
        Collision.resolve(s, ArrayList(), false)
        assertEquals(1, s.n)
        assertEquals(BodyType.BLACK_HOLE, s.typeOf(0))
        assertEquals(holeId, s.id[0])
    }

    // ---- 21 -------------------------------------------------------------------------------------
    @Test
    fun blackHoleCaptureConservesMomentumAndGrowsMass() {
        val bh = 5.0 * M_SUN
        val victim = M_EARTH
        val s = overlappingPair(bh, victim, 0.0, 3.0e4, BodyType.BLACK_HOLE, BodyType.PLANET, 14.0, 10.0)
        val pBefore = bh * 0.0 + victim * 3.0e4
        val ev = ArrayList<SimEvent>()
        Collision.resolve(s, ev, false)
        assertEquals(1, s.n)
        assertEquals(bh + victim, s.mass[0], 1.0e-6)
        assertEquals(pBefore, s.mass[0] * s.vx[0], abs(pBefore) * 1.0e-12)
        assertNotNull(ev.filterIsInstance<SimEvent.BlackHoleCapture>().firstOrNull())
    }

    // ---- 22 -------------------------------------------------------------------------------------
    @Test
    fun blackHoleRingRadiusIsCaptureRadius() {
        val s = arrays()
        val slot = s.add(BodyType.BLACK_HOLE, 5.0 * M_SUN, 14.0, 0.0, 0.0, 0.0, 0.0)
        // One shared constant: the drawn ring radius IS the collision radius, in scene metres.
        assertEquals(s.radiusDp[slot] * s.metersPerDp, s.radius[slot], 0.0)

        // A marble just inside the ring is captured; just outside it is not.
        val inside = arrays()
        inside.add(BodyType.BLACK_HOLE, 5.0 * M_SUN, 14.0, 0.0, 0.0, 0.0, 0.0)
        inside.add(BodyType.TEST_MARBLE, 0.0, 5.0, (14.0 + 5.0) * mpd * 0.9, 0.0, 0.0, 0.0)
        Collision.resolve(inside, ArrayList(), false)
        assertEquals(1, inside.n)

        val outside = arrays()
        outside.add(BodyType.BLACK_HOLE, 5.0 * M_SUN, 14.0, 0.0, 0.0, 0.0, 0.0)
        outside.add(BodyType.TEST_MARBLE, 0.0, 5.0, (14.0 + 5.0) * mpd * 1.1, 0.0, 0.0, 0.0)
        Collision.resolve(outside, ArrayList(), false)
        assertEquals(2, outside.n)
    }

    // ---- 23 -------------------------------------------------------------------------------------
    @Test
    fun schwarzschildRadiusSelfConsistent() {
        val rs = EngineConstants.schwarzschildRadius(M_SUN)
        val direct = 2.0 * EngineConstants.G * M_SUN / (EngineConstants.C * EngineConstants.C)
        assertEquals(direct, rs, abs(direct) * 1.0e-9)
        assertTrue("r_s(1 M_SUN) = $rs", rs in 2900.0..3000.0)
        // 5 M_SUN -> ~14.77 km, the value the inspector shows.
        assertEquals(14770.0, EngineConstants.schwarzschildRadius(5.0 * M_SUN), 20.0)
        // 1 M_EARTH -> ~8.87 mm.
        assertEquals(0.00887, EngineConstants.schwarzschildRadius(M_EARTH), 5.0e-5)
    }

    // ---- 24 -------------------------------------------------------------------------------------
    @Test
    fun wormholeTeleportPreservesVelocity() {
        val s = arrays()
        val a = Wormhole.addPair(s, -50.0 * mpd, 0.0, 50.0 * mpd, 0.0)
        assertTrue(a >= 0)
        val marble = s.add(BodyType.TEST_MARBLE, 0.0, 5.0, -50.0 * mpd, 0.0, 4000.0, 250.0)
        val vx = s.vx[marble]
        val vy = s.vy[marble]
        val ev = ArrayList<SimEvent>()

        assertTrue(Wormhole.resolve(s, ev))
        assertEquals("velocity must be preserved exactly", vx, s.vx[marble], 0.0)
        assertEquals(vy, s.vy[marble], 0.0)
        // Exit placed just outside the partner ring, along the entry direction.
        val d = hypot(s.x[marble] - 50.0 * mpd, s.y[marble])
        assertTrue("exit inside the partner ring", d > s.radius[1])
        assertNotNull(ev.filterIsInstance<SimEvent.WormholeTraversal>().firstOrNull())
    }

    // ---- 25 -------------------------------------------------------------------------------------
    @Test
    fun wormholeCooldownPreventsOscillation() {
        val s = arrays()
        Wormhole.addPair(s, -50.0 * mpd, 0.0, 50.0 * mpd, 0.0)
        val marble = s.add(BodyType.TEST_MARBLE, 0.0, 5.0, -50.0 * mpd, 0.0, 1.0e6, 0.0)
        val ev = ArrayList<SimEvent>()

        assertTrue(Wormhole.resolve(s, ev))
        val firstTime = s.simTime

        // Immediately place the body back on the other mouth: both cooldown halves must block it.
        s.x[marble] = -50.0 * mpd
        s.y[marble] = 0.0
        assertTrue("spatial gate must be armed", s.gateMouthId[marble] != 0L)
        assertFalse("no second traversal while gated", Wormhole.resolve(s, ev))

        // Satisfy the spatial half only: the temporal half must still block.
        s.x[marble] = 0.0
        s.y[marble] = 0.0
        Wormhole.resolve(s, ev) // clears the spatial gate; body is not inside a mouth here
        s.x[marble] = -50.0 * mpd
        assertFalse("temporal cooldown must still block", Wormhole.resolve(s, ev))

        // Both halves satisfied.
        s.simTime = firstTime + EngineConstants.WORMHOLE_COOLDOWN_SIM_S + 1.0
        assertTrue(Wormhole.resolve(s, ev))
        val traversals = ev.filterIsInstance<SimEvent.WormholeTraversal>()
        assertEquals(2, traversals.size)
        assertTrue(
            "inter-traversal gap must be at least 5e5 sim-s",
            traversals[1].simTime - traversals[0].simTime >= EngineConstants.WORMHOLE_COOLDOWN_SIM_S
        )
    }

    @Test
    fun wormholeMouthsAreMasslessAndNeverCollide() {
        val s = arrays()
        Wormhole.addPair(s, 0.0, 0.0, 2.0 * mpd, 0.0) // deliberately overlapping mouths
        assertEquals(0.0, s.mass[0], 0.0)
        assertEquals(0.0, s.mass[1], 0.0)
        assertFalse("mouths must never interact", Collision.resolve(s, ArrayList(), false))
        NBodyEngine.computeAccelerations(s)
        assertEquals(0.0, s.ax[0], 0.0)
        assertEquals(0.0, s.ay[0], 0.0)
    }

    // ---- 26 -------------------------------------------------------------------------------------
    @Test
    fun bodyCapEnforcedAt20() {
        val s = arrays()
        repeat(EngineConstants.MAX_BODIES) { i ->
            val slot = s.add(BodyType.ASTEROID, 1.0e18, 4.0, i * 3.0 * mpd, 0.0, 0.0, 0.0)
            assertTrue("slot $i should exist", slot >= 0)
        }
        assertEquals(EngineConstants.MAX_BODIES, s.n)
        assertTrue(s.isFull())
        assertEquals(-1, s.add(BodyType.ASTEROID, 1.0e18, 4.0, 0.0, 5.0e11, 0.0, 0.0))
        assertEquals(EngineConstants.MAX_BODIES, s.n)
    }

    // ---- 27 -------------------------------------------------------------------------------------
    @Test
    fun nanGuardRollsBack() {
        val s = arrays()
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.PLANET, M_EARTH, 10.0, EngineConstants.AU, 0.0, 0.0, 3.0e4)
        NBodyEngine.computeAccelerations(s)
        NBodyEngine.resetFailureCounter()

        s.backup()
        val hashBefore = s.stateHash()
        val timeBefore = s.simTime

        // Poison the state exactly as a numerical blow-up would.
        s.x[1] = Double.NaN
        s.simTime += 12345.0

        val ev = ArrayList<SimEvent>()
        val healthy = NBodyEngine.validateState(s, ev)
        assertFalse(healthy)
        assertEquals("arrays must be restored", hashBefore, s.stateHash())
        assertEquals("simTime must be restored", timeBefore, s.simTime, 0.0)
        assertNotNull(ev.filterIsInstance<SimEvent.NumericalFailure>().firstOrNull())
    }

    // ---- 30 -------------------------------------------------------------------------------------
    @Test
    fun kinematicDragBypassesIntegration() {
        val s = arrays()
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        val held = s.add(BodyType.PLANET, M_EARTH, 10.0, EngineConstants.AU, 0.0, 0.0, 0.0)
        s.kinematic[held] = true
        NBodyEngine.computeAccelerations(s)

        val x0 = s.x[held]
        val ev = ArrayList<SimEvent>()
        repeat(50) { NBodyEngine.step(s, EngineConstants.DT, ev, false) }

        assertEquals("a held body must not be integrated", x0, s.x[held], 0.0)
        assertEquals(0.0, s.vx[held], 0.0)
        // The Sun still feels it, though: it is a gravity source while held.
        assertTrue(hypot(s.ax[0], s.ay[0]) > 0.0)
    }

    // ---- 39 -------------------------------------------------------------------------------------
    @Test
    fun kinematicReleaseUsesFreshForces() {
        val s = arrays()
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        val held = s.add(BodyType.PLANET, M_EARTH, 10.0, EngineConstants.AU, 0.0, 0.0, 0.0)
        NBodyEngine.computeAccelerations(s)

        s.kinematic[held] = true
        // Drag it to a completely different distance: the stale acceleration is now wrong by 4x.
        val staleAx = s.ax[held]
        s.x[held] = 0.5 * EngineConstants.AU
        s.accelerationsValid = false

        NBodyEngine.ensureAccelerations(s)
        val freshAx = s.ax[held]
        assertTrue("acceleration must be recomputed after the move", abs(freshAx) > abs(staleAx) * 3.0)

        s.kinematic[held] = false
        val ev = ArrayList<SimEvent>()
        val vxBefore = s.vx[held]
        NBodyEngine.step(s, EngineConstants.DT, ev, false)
        val dv = s.vx[held] - vxBefore
        // Velocity Verlet applies half of the fresh acceleration before the drift and half after,
        // so over one whole step dv ~= a_fresh * DT.
        assertEquals(freshAx * EngineConstants.DT, dv, abs(freshAx * EngineConstants.DT) * 0.02)
        // And it is unmistakably NOT the stale value the body carried before the move.
        assertTrue(
            "the first kick used a stale acceleration",
            abs(dv - staleAx * EngineConstants.DT) > abs(freshAx * EngineConstants.DT) * 0.5
        )
    }

    @Test
    fun mergedTrailIsCleared() {
        val s = overlappingPair(3.0e24, 1.0e24, 0.0, 0.0)
        s.pushTrailSample()
        s.pushTrailSample()
        assertTrue(s.trails[0].count > 0)
        Collision.resolve(s, ArrayList(), false)
        assertEquals("the merge point is a discontinuity", 0, s.trails[0].count)
    }

    @Test
    fun removingABodyKeepsTrailsWithTheirOwners() {
        val s = arrays()
        s.add(BodyType.PLANET, M_EARTH, 10.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.PLANET, M_EARTH, 10.0, 40.0 * mpd, 0.0, 0.0, 0.0)
        s.add(BodyType.PLANET, M_EARTH, 10.0, 80.0 * mpd, 0.0, 0.0, 0.0)
        s.pushTrailSample()
        val thirdX = s.x[2]
        val thirdTrailX = s.trails[2].xAt(0)
        assertEquals(thirdX, thirdTrailX, 0.0)

        s.removeAt(0)
        assertEquals(2, s.n)
        // The former third body is now slot 1 and must still own its own trail sample.
        assertEquals(s.x[1], s.trails[1].xAt(0), 0.0)
    }

    @Test
    fun quarantineRemovesRepeatOffender() {
        val s = arrays()
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.PLANET, M_EARTH, 10.0, EngineConstants.AU, 0.0, 0.0, 3.0e4)
        NBodyEngine.computeAccelerations(s)
        NBodyEngine.resetFailureCounter()
        val ev = ArrayList<SimEvent>()
        var guard = 0
        while (s.n == 2 && guard++ < 10) {
            s.backup()
            s.x[1] = Double.POSITIVE_INFINITY
            NBodyEngine.validateState(s, ev)
        }
        assertEquals("the offender must eventually be quarantined", 1, s.n)
        assertNull(ev.filterIsInstance<SimEvent.NumericalFailure>().firstOrNull { !it.rolledBack })
    }
}
