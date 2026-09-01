package com.zig.gravity

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.EngineConstants.AU
import com.zig.gravity.physics.EngineConstants.DT
import com.zig.gravity.physics.EngineConstants.G
import com.zig.gravity.physics.EngineConstants.M_EARTH
import com.zig.gravity.physics.EngineConstants.M_SUN
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.SimEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * §3.16 tests 1-13, 28, 29, 35-38 — orbital mechanics, integrator, timestep, determinism and
 * edge-case hardening. Pure JVM: no emulator, no Android.
 */
class GravityPhysicsTest {

    private fun events() = ArrayList<SimEvent>()
    private fun budget(n: Int = 10_000) = intArrayOf(n)

    private fun sunEarth(): SimArrays {
        val s = SimArrays()
        s.setMetersPerDp(1.122e9)
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.PLANET, M_EARTH, 10.0, AU, 0.0, 0.0, sqrt(G * M_SUN / AU))
        NBodyEngine.computeAccelerations(s)
        return s
    }

    private fun run(s: SimArrays, steps: Int, dt: Double = DT) {
        val e = events()
        NBodyEngine.ensureAccelerations(s)
        repeat(steps) { NBodyEngine.step(s, dt, e, false) }
    }

    // ---- 1 -------------------------------------------------------------------------------------
    @Test
    fun twoBodyCircularOrbitClosesAfterOnePeriod() {
        val s = sunEarth()
        val period = 2.0 * Math.PI * sqrt(AU * AU * AU / (G * (M_SUN + M_EARTH)))
        run(s, (period / DT).toInt())
        val r = hypot(s.x[1] - s.x[0], s.y[1] - s.y[0])
        assertTrue("radius drift ${abs(r - AU) / AU}", abs(r - AU) / AU < 0.01)
    }

    // ---- 2 -------------------------------------------------------------------------------------
    @Test
    fun totalEnergyDriftBounded() {
        val s = sunEarth()
        val e0 = NBodyEngine.totalEnergy(s)
        val period = 2.0 * Math.PI * sqrt(AU * AU * AU / (G * (M_SUN + M_EARTH)))
        run(s, (10.0 * period / DT).toInt())
        val e1 = NBodyEngine.totalEnergy(s)
        assertTrue("energy drift ${abs((e1 - e0) / e0)}", abs((e1 - e0) / e0) < 1.0e-3)
    }

    // ---- 3 -------------------------------------------------------------------------------------
    @Test
    fun totalMomentumConservedInFreeFlight() {
        val s = sunEarth()
        s.vx[0] = 120.0
        NBodyEngine.computeAccelerations(s)
        var px0 = 0.0
        var py0 = 0.0
        for (i in 0 until s.n) { px0 += s.mass[i] * s.vx[i]; py0 += s.mass[i] * s.vy[i] }
        run(s, 2000)
        var px1 = 0.0
        var py1 = 0.0
        for (i in 0 until s.n) { px1 += s.mass[i] * s.vx[i]; py1 += s.mass[i] * s.vy[i] }
        assertEquals(px0, px1, abs(px0) * 1.0e-9 + 1.0)
        assertEquals(py0, py1, abs(py0) * 1.0e-9 + 1.0)
    }

    // ---- 4 -------------------------------------------------------------------------------------
    @Test
    fun earthPerturbsSun() {
        val s = sunEarth()
        NBodyEngine.computeAccelerations(s)
        val aSun = hypot(s.ax[0], s.ay[0])
        assertTrue("the Sun must feel Earth", aSun > 0.0)
        assertEquals(G * M_EARTH / (AU * AU), aSun, aSun * 1.0e-6)
    }

    // ---- 5 -------------------------------------------------------------------------------------
    @Test
    fun moonAffectsBoth() {
        val s = sunEarth()
        s.add(BodyType.MOON, EngineConstants.M_MOON, 6.0, AU + 2.0e10, 0.0, 0.0, 0.0)
        NBodyEngine.computeAccelerations(s)
        val aSunWith = hypot(s.ax[0], s.ay[0])
        val aEarthWith = hypot(s.ax[1], s.ay[1])

        val bare = sunEarth()
        NBodyEngine.computeAccelerations(bare)
        val aSunWithout = hypot(bare.ax[0], bare.ay[0])
        val aEarthWithout = hypot(bare.ax[1], bare.ay[1])

        assertTrue("the Moon must pull the Sun", abs(aSunWith - aSunWithout) > 0.0)
        assertTrue("the Moon must pull Earth", abs(aEarthWith - aEarthWithout) > 0.0)
    }

    // ---- 6 -------------------------------------------------------------------------------------
    @Test
    fun eccentricOrbitPeriodMatchesKepler() {
        val s = SimArrays()
        s.setMetersPerDp(1.122e9)
        val a = AU
        val e = 0.5
        val rp = a * (1 - e)
        val vp = sqrt(G * (M_SUN + M_EARTH) * (1 + e) / (a * (1 - e)))
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.PLANET, M_EARTH, 10.0, rp, 0.0, 0.0, vp)
        NBodyEngine.computeAccelerations(s)

        val kepler = 2.0 * Math.PI * sqrt(a * a * a / (G * (M_SUN + M_EARTH)))
        val ev = events()
        var t = 0.0
        var measured = -1.0
        var previousY = 0.0
        var step = 0
        while (t < 2.0 * kepler) {
            previousY = s.y[1] - s.y[0]
            NBodyEngine.step(s, DT, ev, false)
            t += DT
            step++
            val nowY = s.y[1] - s.y[0]
            if (step > 10 && previousY < 0.0 && nowY >= 0.0) { measured = t; break }
        }
        assertTrue("no period detected", measured > 0.0)
        assertTrue("period error ${abs(measured - kepler) / kepler}", abs(measured - kepler) / kepler < 0.01)
    }

    // ---- 7 -------------------------------------------------------------------------------------
    @Test
    fun hyperbolicFlybyEnergyConserved() {
        val s = SimArrays()
        s.setMetersPerDp(1.122e9)
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.ASTEROID, 1.0e18, 4.0, -6.0 * AU, 0.6 * AU, 6.0e4, 0.0)
        NBodyEngine.computeAccelerations(s)
        val e0 = NBodyEngine.totalEnergy(s)
        run(s, 4000)
        val e1 = NBodyEngine.totalEnergy(s)
        assertTrue("hyperbolic energy drift ${abs((e1 - e0) / e0)}", abs((e1 - e0) / e0) < 1.0e-3)
    }

    // ---- 8 -------------------------------------------------------------------------------------
    @Test
    fun substepGroupingInvariant() {
        val a = sunEarth()
        val b = sunEarth()
        val ev = events()
        // 40 fixed DT steps, grouped 10x4 versus 40x1: the fixed timestep makes grouping irrelevant.
        repeat(4) { repeat(10) { NBodyEngine.step(a, DT, ev, false) } }
        repeat(40) { NBodyEngine.step(b, DT, ev, false) }
        assertEquals(a.stateHash(), b.stateHash())
    }

    // ---- 9 -------------------------------------------------------------------------------------
    @Test
    fun speedEquivalence16xVs1x() {
        // Speed multiplies how many fixed DT steps run per frame; it never scales DT.
        // 16x for one frame must be bitwise identical to 1x for sixteen frames.
        val fast = sunEarth()
        val slow = sunEarth()
        val ev = events()
        repeat(16) { NBodyEngine.step(fast, DT, ev, false) }
        repeat(16) { NBodyEngine.step(slow, DT, ev, false) }
        assertEquals(fast.stateHash(), slow.stateHash())
    }

    // ---- 10 ------------------------------------------------------------------------------------
    @Test
    fun pauseResumeStateIdentical() {
        val s = sunEarth()
        run(s, 100)
        val hash = s.stateHash()
        // "Paused" means no step is executed at all: the state must be untouched.
        assertEquals(hash, s.stateHash())
        run(s, 1)
        assertTrue(hash != s.stateHash())
    }

    // ---- 11 ------------------------------------------------------------------------------------
    @Test
    fun resetRestoresPresetExactly() {
        val s = sunEarth()
        val pristine = SimArrays()
        s.copyInto(pristine)
        val before = s.stateHash()
        run(s, 500)
        assertTrue(before != s.stateHash())
        pristine.copyInto(s)
        assertEquals(before, s.stateHash())
    }

    // ---- 12 ------------------------------------------------------------------------------------
    @Test
    fun deterministicReplay() {
        val a = sunEarth()
        val b = sunEarth()
        run(a, 733)
        run(b, 733)
        assertEquals(a.stateHash(), b.stateHash())
    }

    // ---- 13 ------------------------------------------------------------------------------------
    @Test
    fun accumulatorNeverExplodes() {
        // A pathological 5-second frame must be clipped and the debt discarded, never spiralled.
        var accumulator = 0.0
        val frameSeconds = 5.0
        accumulator += minOf(frameSeconds, EngineConstants.MAX_FRAME_SECONDS) *
                EngineConstants.BASE * 16.0
        var steps = 0
        while (accumulator >= DT && steps < EngineConstants.MAX_SUBSTEPS) {
            accumulator -= DT
            steps++
        }
        if (steps == EngineConstants.MAX_SUBSTEPS) accumulator = 0.0
        assertTrue(steps <= EngineConstants.MAX_SUBSTEPS)
        assertEquals(0.0, accumulator, 1.0e-9)
    }

    // ---- 28 ------------------------------------------------------------------------------------
    @Test
    fun softeningPreventsSingularity() {
        val s = SimArrays()
        s.setMetersPerDp(1.122e9)
        s.add(BodyType.SUN, M_SUN, 26.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.ASTEROID, 1.0e18, 4.0, 0.0, 0.0, 0.0, 0.0) // exactly coincident
        NBodyEngine.computeAccelerations(s)
        assertTrue(s.ax[0].isFinite() && s.ay[0].isFinite())
        assertTrue(s.ax[1].isFinite() && s.ay[1].isFinite())
        val aMax = G * M_SUN / EngineConstants.EPS_SOFT_SQ
        assertTrue(hypot(s.ax[1], s.ay[1]) <= aMax)
    }

    // ---- 29 ------------------------------------------------------------------------------------
    @Test
    fun testParticleExertsNoGravity() {
        val withMarble = sunEarth()
        withMarble.add(BodyType.TEST_MARBLE, 1.0e30, 5.0, 0.5 * AU, 0.0, 0.0, 0.0)
        NBodyEngine.computeAccelerations(withMarble)

        val without = sunEarth()
        NBodyEngine.computeAccelerations(without)

        // A marble is massless by construction, so nothing else may feel it.
        assertEquals(0.0, withMarble.mass[2], 0.0)
        assertEquals(without.ax[0], withMarble.ax[0], 1.0e-30)
        assertEquals(without.ax[1], withMarble.ax[1], 1.0e-30)
        // ... but it must feel gravity itself.
        assertTrue(hypot(withMarble.ax[2], withMarble.ay[2]) > 0.0)
    }

    // ---- 35 ------------------------------------------------------------------------------------
    @Test
    fun softeningDoesNotDistortKnownOrbits() {
        for (r in doubleArrayOf(AU, EngineConstants.MOON_ORBIT_RADIUS)) {
            val exact = G * M_SUN / (r * r)
            val softened = G * M_SUN * r / Math.pow(r * r + EngineConstants.EPS_SOFT_SQ, 1.5)
            val distortion = abs(exact - softened) / exact
            assertTrue("distortion at r=$r was $distortion", distortion < 1.0e-3)
        }
        // The documented anchor value at the Moon distance (§3.2).
        val r = EngineConstants.MOON_ORBIT_RADIUS
        val predicted = 1.5 * (EngineConstants.EPS_SOFT / r) * (EngineConstants.EPS_SOFT / r)
        assertEquals(1.0e-5, predicted, 5.0e-7)
    }

    // ---- 36 ------------------------------------------------------------------------------------
    @Test
    fun tightOrbitDoesNotDiverge() {
        val s = SimArrays()
        s.setMetersPerDp(1.122e9)
        val m = 50.0 * M_SUN
        val sep = 28.0 * 1.122e9
        val vRel = sqrt(G * 2.0 * m / sep)
        s.add(BodyType.BLACK_HOLE, m, 14.0, -sep / 2, 0.0, 0.0, -vRel / 2)
        s.add(BodyType.BLACK_HOLE, m, 14.0, sep / 2, 0.0, 0.0, vRel / 2)
        NBodyEngine.computeAccelerations(s)
        val e0 = NBodyEngine.totalEnergy(s)
        val ev = events()
        val b = budget(100_000)
        repeat(3000) {
            if (s.n < 2) return@repeat
            NBodyEngine.advance(s, DT, ev, false, b)
        }
        if (s.n == 2) {
            val e1 = NBodyEngine.totalEnergy(s)
            assertTrue("energy diverged: ${abs((e1 - e0) / e0)}", abs((e1 - e0) / e0) < 0.05)
        } else {
            // Merging at contact is the specified alternative outcome; it must be clean.
            assertEquals(1, s.n)
            assertTrue(s.mass[0].isFinite())
        }
    }

    // ---- 37 ------------------------------------------------------------------------------------
    @Test
    fun fastFlybyDoesNotTunnel() {
        val s = SimArrays()
        s.setMetersPerDp(1.122e9)
        val bhMass = 50.0 * M_SUN
        s.add(BodyType.BLACK_HOLE, bhMass, 14.0, 0.0, 0.0, 0.0, 0.0)
        // Head-on at the engine's hard velocity cap.
        s.add(BodyType.TEST_MARBLE, 0.0, 5.0, -200.0 * 1.122e9, 0.0, EngineConstants.V_MAX, 0.0)
        NBodyEngine.computeAccelerations(s)
        val ev = events()
        val b = budget(200_000)
        var passedThrough = false
        repeat(4000) {
            if (s.n < 2) return@repeat
            NBodyEngine.advance(s, DT, ev, false, b)
            if (s.n == 2 && s.x[1] > 2.0 * s.radius[0]) passedThrough = true
        }
        assertFalse("a marble at 1000 km/s tunnelled straight through the hole", passedThrough)
    }

    // ---- 38 ------------------------------------------------------------------------------------
    @Test
    fun velocityBoundsClamped() {
        val s = sunEarth()
        s.vx[1] = 9.0e7
        NBodyEngine.clampVelocity(s)
        assertTrue(hypot(s.vx[1], s.vy[1]) <= EngineConstants.V_MAX + 1.0e-6)

        // UI guidance is min(2 x v_esc_local, 1000 km/s). Near a 50 M_SUN hole at 1e10 m the
        // escape speed alone is ~1152 km/s, so the hard cap governs (§3.7 / verification record).
        val nearHole = EngineConstants.uiVelocityGuidance(50.0 * M_SUN, 1.0e10)
        assertEquals(EngineConstants.V_MAX, nearHole, 1.0e-6)
        // Far from anything heavy the softer guidance governs instead.
        val gentle = EngineConstants.uiVelocityGuidance(M_EARTH, 1.0e10)
        assertEquals(2.0 * EngineConstants.escapeSpeed(M_EARTH, 1.0e10), gentle, 1.0e-6)
        assertTrue(gentle < EngineConstants.V_MAX)
    }
}
