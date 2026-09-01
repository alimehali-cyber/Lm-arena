package com.zig.gravity

import com.zig.gravity.edu.Challenges
import com.zig.gravity.edu.Glossary
import com.zig.gravity.edu.TeachingCatalog
import com.zig.gravity.edu.detectors.SimulationDetectors
import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.Presets
import com.zig.gravity.sim.SaveState
import com.zig.gravity.sim.SimulationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * §3.16 tests 31-34 plus the regression suite the implementation brief demands: Earth-Moon
 * separation, editing, add/remove, the 20-cap, prediction invalidation, barycentre, trails,
 * reset, simulation speed and drag/throw transitions.
 *
 * These are integration tests: a UI-level intent goes in, real simulation state comes out.
 */
class GravitySandboxIntegrationTest {

    private val mpd = 1.122e9
    private val frame = 1.0 / 60.0
    private var clock = 0L

    private fun vmWith(preset: Preset): SimulationViewModel {
        clock = 0L
        val vm = SimulationViewModel()
        vm.onViewportChanged(400.0)
        vm.loadPreset(preset)
        return vm
    }

    private fun advance(vm: SimulationViewModel, frames: Int) {
        repeat(frames) {
            clock += (frame * 1.0e9).toLong()
            vm.onFrame(clock)
        }
    }

    // ---- 31 --------------------------------------------------------------------------------------
    @Test
    fun constantsAreExactSI() {
        assertEquals(6.67430e-11, EngineConstants.G, 0.0)
        assertEquals(2.99792458e8, EngineConstants.C, 0.0)
        assertEquals(1.989e30, EngineConstants.M_SUN, 0.0)
        assertEquals(6.957e8, EngineConstants.R_SUN, 0.0)
        assertEquals(5.972e24, EngineConstants.M_EARTH, 0.0)
        assertEquals(6.371e6, EngineConstants.R_EARTH, 0.0)
        assertEquals(7.348e22, EngineConstants.M_MOON, 0.0)
        assertEquals(1.737e6, EngineConstants.R_MOON, 0.0)
        assertEquals(1.496e11, EngineConstants.AU, 0.0)
        assertEquals(1.0e6, EngineConstants.EPS_SOFT, 0.0)
        assertEquals(3600.0, EngineConstants.DT, 0.0)
        assertEquals(1.0e6, EngineConstants.BASE, 0.0)
        assertEquals(20, EngineConstants.MAX_BODIES)
        assertEquals(96, EngineConstants.MAX_SUBSTEPS)
        assertEquals(1.0e6, EngineConstants.V_MAX, 0.0)
        // Earth's orbital speed follows from the charter, it is not a separate magic number.
        assertEquals(
            EngineConstants.EARTH_ORBIT_SPEED,
            EngineConstants.circularSpeed(EngineConstants.M_SUN, EngineConstants.AU),
            15.0
        )
        // Scene scale: 3 AU over 400 dp is 1.122e9 m/dp.
        assertEquals(1.122e9, EngineConstants.metersPerDp(400.0), 1.0e6)
        // Speed ladder is exactly the locked set.
        assertTrue(EngineConstants.SPEEDS.contentEquals(doubleArrayOf(0.1, 0.25, 1.0, 4.0, 16.0)))
    }

    // ---- 32 --------------------------------------------------------------------------------------
    @Test
    fun saveRestoreRoundTripIdentical() {
        val vm = vmWith(Preset.INNER_SYSTEM)
        advance(vm, 30)
        val before = vm.arrays.stateHash()
        val text = vm.serialize()

        val restored = SimArrays()
        restored.setMetersPerDp(mpd)
        val session = SaveState.decode(text, restored)
        assertNotNull(session)
        assertEquals(vm.arrays.n, restored.n)
        assertEquals(before, restored.stateHash())
        assertEquals(Preset.INNER_SYSTEM, session!!.preset)
    }

    // ---- 33 --------------------------------------------------------------------------------------
    @Test
    fun orbitDetectorFiresOnClosedSweep() {
        val vm = vmWith(Preset.SUN_EARTH)
        val detectors = SimulationDetectors()
        var stabilised = false
        var t = 1_000_000L
        // One Earth year is ~31.6 s of real time at 1x; step the detector across a full lap.
        repeat(2600) {
            advance(vm, 1)
            t += 5000L
            val d = detectors.observe(vm.snapshot, emptyList(), t)
            if (d != null && d.concept == SimulationDetectors.ORBIT_STABILIZED) stabilised = true
        }
        assertTrue("a full lap must be recognised as a stable orbit", stabilised)
    }

    // ---- 34 --------------------------------------------------------------------------------------
    @Test
    fun escapeDetectorFiresOnUnbound() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earthId = vm.snapshot.id[1]
        // Well past escape speed, pointing outward.
        vm.setVelocity(earthId, 1.0e5, 1.0e5)
        val detectors = SimulationDetectors()
        var escaped = false
        var t = 1_000_000L
        repeat(400) {
            advance(vm, 5)
            t += 5000L
            val d = detectors.observe(vm.snapshot, emptyList(), t)
            if (d != null && d.concept == SimulationDetectors.BODY_ESCAPED) escaped = true
        }
        assertTrue("an unbound body must be detected as escaped", escaped)
    }

    // ==== Earth + Moon regression (the audit's critical bug area) ==================================

    @Test
    fun moonIsAnIndependentBodyAndNeverCollapsesIntoEarth() {
        val vm = vmWith(Preset.EARTH_MOON)
        val s = vm.arrays
        assertEquals(2, s.n)
        val earth = 0
        val moon = 1
        assertEquals(BodyType.PLANET, s.typeOf(earth))
        assertEquals(BodyType.MOON, s.typeOf(moon))

        // Independent state.
        assertTrue(s.x[earth] != s.x[moon])
        assertTrue(s.vy[earth] != s.vy[moon])

        // Mutual attraction: each pulls the other, in opposite directions.
        NBodyEngine.computeAccelerations(s)
        assertTrue(hypot(s.ax[earth], s.ay[earth]) > 0.0)
        assertTrue(hypot(s.ax[moon], s.ay[moon]) > 0.0)
        assertTrue("accelerations must oppose", s.ax[earth] * s.ax[moon] <= 0.0)

        // Visually separate at the locked scene scale, at every moment of a full lap.
        vm.setSpeedIndex(4) // 16x, so one lap fits inside the test budget
        val contact = s.radius[earth] + s.radius[moon]
        var minSeparation = Double.MAX_VALUE
        var maxSeparation = 0.0
        var moved = false
        val startX = s.x[moon]
        repeat(4000) {
            advance(vm, 1)
            if (s.n < 2) return@repeat
            val d = hypot(s.x[1] - s.x[0], s.y[1] - s.y[0])
            minSeparation = minOf(minSeparation, d)
            maxSeparation = maxOf(maxSeparation, d)
            if (abs(s.x[1] - startX) > contact) moved = true
        }
        assertEquals("the Moon must never be absorbed", 2, s.n)
        assertTrue("the Moon must actually move (real physics, not a decorative path)", moved)
        assertTrue(
            "Earth and Moon must never visually merge: min separation ${minSeparation / mpd} dp vs contact ${contact / mpd} dp",
            minSeparation > contact
        )
        // Separation is readable on screen: at least 12 dp apart at all times.
        assertTrue("separation must be readable", minSeparation / vm.arrays.metersPerDp > 15.0)
        // A real orbit, not a fixed radius offset: it varies a little but stays bound.
        assertTrue(maxSeparation / minSeparation < 3.0)
    }

    @Test
    fun barycenterIsNotAtTheHeavierBodyCentre() {
        val em = vmWith(Preset.EARTH_MOON)
        val s = em.arrays
        val earthToBary = hypot(s.barycenter(0) - s.x[0], s.barycenter(1) - s.y[0])
        val moonToBary = hypot(s.barycenter(0) - s.x[1], s.barycenter(1) - s.y[1])
        assertTrue("barycentre must not sit exactly at Earth's centre", earthToBary > 0.0)
        assertTrue("barycentre must be much closer to Earth than to the Moon", earthToBary < moonToBary)

        // Sun + Earth: close to, but not exactly at, the Sun's centre.
        val se = vmWith(Preset.SUN_EARTH)
        val s2 = se.arrays
        val d = hypot(s2.barycenter(0) - s2.x[0], s2.barycenter(1) - s2.y[0])
        assertTrue("must be offset from the Sun's centre", d > 0.0)
        assertTrue("but well inside the Sun's drawn body", d < s2.radius[0])
    }

    private fun SimArrays.barycenter(axis: Int): Double {
        val out = DoubleArray(2)
        NBodyEngine.barycenter(this, out)
        return out[axis]
    }

    @Test
    fun changingMassMovesTheBarycenter() {
        val vm = vmWith(Preset.EARTH_MOON)
        val out = DoubleArray(2)
        NBodyEngine.barycenter(vm.arrays, out)
        val before = out[0]
        vm.setMass(vm.arrays.id[1], vm.arrays.mass[1] * 10.0)
        NBodyEngine.barycenter(vm.arrays, out)
        assertTrue("a mass edit must move the barycentre", abs(out[0] - before) > 0.0)
    }

    // ==== catalog, add / remove, cap ==================================================================

    @Test
    fun everyCatalogEntryCanBeAddedToALiveSimulation() {
        for (entry in BodyCatalog.all) {
            val vm = vmWith(Preset.SUN_EARTH)
            val before = vm.arrays.n
            assertTrue("${entry.key} must be addable", vm.addFromCatalog(entry.key))
            val expected = before + if (entry.isPair) 2 else 1
            assertEquals("${entry.key} body count", expected, vm.arrays.n)

            val slot = vm.arrays.slotOfId(vm.selectedId)
            assertTrue(slot >= 0)
            assertEquals(entry.type, vm.arrays.typeOf(slot))
            assertTrue("id must be unique and non-zero", vm.arrays.id[slot] != 0L)
            assertEquals(if (entry.type.massless) 0.0 else entry.massKg, vm.arrays.mass[slot], 1.0e9)
            assertTrue("must have a real collision radius", vm.arrays.radius[slot] > 0.0)
            assertTrue("must take part in the force pass", vm.arrays.accelerationsValid)

            // A newly added massive body must actually feel gravity.
            if (!entry.isPair) {
                NBodyEngine.computeAccelerations(vm.arrays)
                assertTrue(
                    "${entry.key} must feel the field",
                    hypot(vm.arrays.ax[slot], vm.arrays.ay[slot]) > 0.0
                )
            }
        }
    }

    @Test
    fun wormholeAddsALinkedPair() {
        val vm = vmWith(Preset.SUN_EARTH)
        assertTrue(vm.addFromCatalog(BodyCatalog.WORMHOLE.key))
        val mouths = (0 until vm.arrays.n).filter { vm.arrays.typeOf(it) == BodyType.WORMHOLE_MOUTH }
        assertEquals(2, mouths.size)
        assertEquals(vm.arrays.id[mouths[1]], vm.arrays.partnerId[mouths[0]])
        assertEquals(vm.arrays.id[mouths[0]], vm.arrays.partnerId[mouths[1]])
        // Removing one mouth removes its partner.
        vm.select(vm.arrays.id[mouths[0]])
        vm.removeSelected()
        assertEquals(0, (0 until vm.arrays.n).count { vm.arrays.typeOf(it) == BodyType.WORMHOLE_MOUTH })
    }

    @Test
    fun twentyBodyCapIsEnforcedThroughTheUiIntent() {
        val vm = vmWith(Preset.SUN_EARTH)
        var added = 0
        while (vm.addFromCatalog(BodyCatalog.ASTEROID.key)) added++
        assertEquals(EngineConstants.MAX_BODIES, vm.arrays.n)
        assertNotNull("a friendly notice must appear when the table is full", vm.notice)
        assertTrue(vm.notice!!.isNotBlank())
        assertTrue(added > 0)
        // A wormhole needs two slots and must also be refused at the cap.
        assertFalse(vm.addFromCatalog(BodyCatalog.WORMHOLE.key))
        assertEquals(EngineConstants.MAX_BODIES, vm.arrays.n)
    }

    @Test
    fun duplicateAndRemoveChangeTheNBodySolution() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earthId = vm.arrays.id[1]
        vm.select(earthId)
        NBodyEngine.computeAccelerations(vm.arrays)
        val sunAxBefore = vm.arrays.ax[0]

        vm.duplicateSelected()
        assertEquals(3, vm.arrays.n)
        assertTrue("duplicating must change the field", abs(vm.arrays.ax[0] - sunAxBefore) > 0.0)

        vm.select(earthId)
        vm.removeSelected()
        assertEquals(2, vm.arrays.n)
        assertEquals(0L, vm.selectedId)
    }

    // ==== editing propagates into physics ================================================================

    @Test
    fun massEditChangesGravityAndTrajectory() {
        val vm = vmWith(Preset.SUN_EARTH)
        val sunId = vm.arrays.id[0]
        NBodyEngine.computeAccelerations(vm.arrays)
        val earthAccBefore = hypot(vm.arrays.ax[1], vm.arrays.ay[1])

        vm.setMass(sunId, vm.arrays.mass[0] * 4.0)
        val earthAccAfter = hypot(vm.arrays.ax[1], vm.arrays.ay[1])
        assertTrue("gravity must scale with mass", earthAccAfter > earthAccBefore * 3.5)
        assertTrue("accelerations must be recomputed immediately", vm.arrays.accelerationsValid)

        // And the trajectory must genuinely differ.
        val reference = vmWith(Preset.SUN_EARTH)
        advance(vm, 120)
        advance(reference, 120)
        assertTrue(abs(vm.arrays.x[1] - reference.arrays.x[1]) > 0.0)
    }

    @Test
    fun sizeEditChangesRenderAndCollisionRadiusTogether() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earthId = vm.arrays.id[1]
        vm.setRadiusDp(earthId, 16.0)
        val slot = vm.arrays.slotOfId(earthId)
        assertEquals(16.0, vm.arrays.radiusDp[slot], 1.0e-9)
        assertEquals(
            "collision radius must equal the visual radius in scene metres",
            16.0 * vm.arrays.metersPerDp, vm.arrays.radius[slot], 1.0e-6
        )
        // Clamped to the §3.6a band for the type.
        vm.setRadiusDp(earthId, 999.0)
        assertEquals(BodyType.PLANET.maxDp, vm.arrays.radiusDp[slot], 1.0e-9)
    }

    @Test
    fun velocityAndDirectionEditsChangeTheTrajectory() {
        val vm = vmWith(Preset.SUN_EARTH)
        val reference = vmWith(Preset.SUN_EARTH)
        val earthId = vm.arrays.id[1]

        vm.setSpeedMagnitude(earthId, 45000.0)
        assertEquals(45000.0, hypot(vm.arrays.vx[1], vm.arrays.vy[1]), 1.0)
        vm.setDirection(earthId, Math.PI)
        assertEquals(-45000.0, vm.arrays.vx[1], 1.0)
        assertEquals(0.0, vm.arrays.vy[1], 1.0)

        advance(vm, 60)
        advance(reference, 60)
        assertTrue(abs(vm.arrays.y[1] - reference.arrays.y[1]) > 0.0)
    }

    @Test
    fun positionEditChangesTheTrajectory() {
        val vm = vmWith(Preset.SUN_EARTH)
        val reference = vmWith(Preset.SUN_EARTH)
        vm.setPosition(vm.arrays.id[1], 0.6 * EngineConstants.AU, 0.0)
        assertTrue(vm.arrays.accelerationsValid)
        advance(vm, 60)
        advance(reference, 60)
        assertTrue(abs(vm.arrays.x[1] - reference.arrays.x[1]) > 0.0)
    }

    @Test
    fun orbitHelperProducesABoundCircularOrbit() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earthId = vm.arrays.id[1]
        vm.setVelocity(earthId, 0.0, 0.0)
        assertTrue(vm.applyOrbitHelper(earthId))
        val r = hypot(vm.arrays.x[1] - vm.arrays.x[0], vm.arrays.y[1] - vm.arrays.y[0])
        val expected = EngineConstants.circularSpeed(vm.arrays.mass[0], r)
        val actual = hypot(vm.arrays.vx[1] - vm.arrays.vx[0], vm.arrays.vy[1] - vm.arrays.vy[0])
        assertEquals(expected, actual, expected * 1.0e-6)
    }

    // ==== prediction ==========================================================================================

    @Test
    fun predictionExistsForTheSelectedBodyAndInvalidatesOnEdits() {
        val vm = vmWith(Preset.SUN_EARTH)
        advance(vm, 2)
        assertEquals("nothing selected -> nothing predicted", 0, vm.predictionCount)

        vm.select(vm.arrays.id[1])
        advance(vm, 2)
        assertTrue("a selected body must have a predicted path", vm.predictionCount > 5)

        val firstX = vm.predictionXY[(vm.predictionCount - 1) * 2]
        val firstY = vm.predictionXY[(vm.predictionCount - 1) * 2 + 1]

        vm.setSpeedMagnitude(vm.arrays.id[1], 12000.0)
        advance(vm, 2)
        val newX = vm.predictionXY[(vm.predictionCount - 1) * 2]
        val newY = vm.predictionXY[(vm.predictionCount - 1) * 2 + 1]
        assertTrue(
            "the prediction must change when the velocity changes",
            abs(newX - firstX) > 0.0 || abs(newY - firstY) > 0.0
        )
    }

    @Test
    fun predictionNeverMutatesTheSimulation() {
        val vm = vmWith(Preset.INNER_SYSTEM)
        vm.select(vm.arrays.id[3])
        vm.togglePlay() // pause
        val before = vm.arrays.stateHash()
        advance(vm, 30)
        assertEquals("a paused simulation must be untouched by prediction", before, vm.arrays.stateHash())
        assertTrue(vm.predictionCount > 5)
    }

    // ==== trails ==================================================================================================

    @Test
    fun trailsFollowRealPositionsAndClearOnReset() {
        val vm = vmWith(Preset.SUN_EARTH)
        advance(vm, 40)
        val ring = vm.arrays.trails[1]
        assertTrue("a trail must accumulate", ring.count > 5)
        val last = ring.count - 1
        assertEquals("the newest sample must be the body's real position", vm.arrays.x[1], ring.xAt(last), 1.0e-6)
        assertTrue("the trail must be a history, not a fixed circle", abs(ring.xAt(0) - ring.xAt(last)) > 0.0)

        vm.reset()
        assertEquals(0, vm.arrays.trails[1].count)
    }

    // ==== reset ======================================================================================================

    @Test
    fun resetRestoresEverything() {
        val vm = vmWith(Preset.INNER_SYSTEM)
        val pristine = vm.arrays.stateHash()
        vm.select(vm.arrays.id[2])
        vm.setMass(vm.arrays.id[2], vm.arrays.mass[2] * 3.0)
        vm.addFromCatalog(BodyCatalog.JUPITER.key)
        advance(vm, 60)
        assertTrue(vm.arrays.stateHash() != pristine)

        vm.reset()
        assertEquals("bodies, masses, positions and velocities", pristine, vm.arrays.stateHash())
        assertEquals("simulation time", 0.0, vm.arrays.simTime, 0.0)
        assertEquals("trails", 0, vm.arrays.trails[0].count)
        assertEquals("selection", 0L, vm.selectedId)
        assertEquals("prediction", 0, vm.predictionCount)
        assertNull("teaching card", vm.teachingConcept)
        for (i in 0 until vm.arrays.n) {
            assertEquals("wormhole cooldown", 0.0, vm.arrays.cooldownUntil[i], 0.0)
            assertEquals("wormhole spatial gate", 0L, vm.arrays.gateMouthId[i])
            assertFalse("kinematic flags", vm.arrays.kinematic[i])
        }
    }

    // ==== simulation speed ==============================================================================================

    @Test
    fun speedButtonsChangeSimulatedTimeNotTheTimestep() {
        val results = DoubleArray(EngineConstants.SPEEDS.size)
        for (i in EngineConstants.SPEEDS.indices) {
            val vm = vmWith(Preset.SUN_EARTH)
            vm.setSpeedIndex(i)
            advance(vm, 60) // one wall-clock second
            results[i] = vm.arrays.simTime
            // The integration timestep must always be exactly DT.
            val steps = vm.arrays.simTime / EngineConstants.DT
            assertEquals(
                "simulated time must be a whole number of fixed DT steps at speed ${EngineConstants.SPEEDS[i]}",
                Math.round(steps).toDouble(), steps, 1.0e-6
            )
        }
        // Each rung must advance measurably more simulated time than the one below it.
        for (i in 1 until results.size) {
            assertTrue(
                "speed ${EngineConstants.SPEEDS[i]} must outpace ${EngineConstants.SPEEDS[i - 1]}",
                results[i] > results[i - 1] * 1.5
            )
        }
        // 1x should deliver about BASE simulated seconds per wall-clock second.
        val oneX = results[EngineConstants.DEFAULT_SPEED_INDEX]
        assertTrue("1x delivered $oneX sim-s in one second", oneX in 0.8e6..1.2e6)
        // 16x is 16x more, within the substep cap (74.1 substeps/frame < 96).
        assertTrue(results[4] / oneX > 12.0)
    }

    @Test
    fun pauseStopsSimulatedTime() {
        val vm = vmWith(Preset.SUN_EARTH)
        advance(vm, 10)
        vm.togglePlay()
        val t = vm.arrays.simTime
        advance(vm, 60)
        assertEquals("a paused table must not advance", t, vm.arrays.simTime, 0.0)
    }

    // ==== drag / throw ====================================================================================================

    @Test
    fun dragMakesBodyKinematicAndReleaseThrowsIt() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earthId = vm.arrays.id[1]
        vm.beginDrag(earthId)
        val slot = vm.arrays.slotOfId(earthId)
        assertTrue("held bodies must be kinematic", vm.arrays.kinematic[slot])

        // Move it 3e10 m in 0.1 s of pointer time -> 3e11 m/s, which the engine must clamp.
        vm.dragTo(1.2e11, 0.0, 0.0)
        vm.dragTo(1.2e11 + 1.5e10, 0.0, 0.05)
        vm.dragTo(1.2e11 + 3.0e10, 0.0, 0.10)
        assertEquals("the body must follow the finger exactly", 1.2e11 + 3.0e10, vm.arrays.x[slot], 1.0)

        vm.endDrag()
        assertFalse(vm.arrays.kinematic[slot])
        assertEquals(0L, vm.draggingId)
        val v = hypot(vm.arrays.vx[slot], vm.arrays.vy[slot])
        assertTrue("release must impart a throw", v > 0.0)
        assertTrue("and the engine clamp must hold", v <= EngineConstants.V_MAX + 1.0)
        assertTrue(vm.arrays.accelerationsValid)
    }

    @Test
    fun slingshotLaunchesOppositeToTheDragAndRespectsTheCap() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earthId = vm.arrays.id[1]
        vm.armSlingshot(earthId)
        // Drag "down-left" from the body: the launch must go up-right.
        vm.updateSlingshot(-50.0 * mpd, -50.0 * mpd)
        assertTrue(vm.slingshotActive)
        assertTrue(vm.slingshotVx > 0.0)
        assertTrue(vm.slingshotVy > 0.0)
        val guidance = vm.velocityGuidance(earthId)
        assertTrue(hypot(vm.slingshotVx, vm.slingshotVy) <= guidance + 1.0)

        vm.releaseSlingshot()
        val slot = vm.arrays.slotOfId(earthId)
        assertTrue(vm.arrays.vx[slot] > 0.0)
        assertTrue(vm.arrays.vy[slot] > 0.0)
        assertEquals(0L, vm.slingshotArmedId)
        assertTrue(vm.arrays.accelerationsValid)
    }

    // ==== teaching content ==========================================================================================

    @Test
    fun everyDetectorConceptHasAThreeTierCard() {
        val concepts = listOf(
            SimulationDetectors.ORBIT_STABILIZED,
            SimulationDetectors.BODY_ESCAPED,
            SimulationDetectors.BODY_MERGED,
            SimulationDetectors.BH_CAPTURE,
            SimulationDetectors.WORMHOLE_TRAVERSAL,
            SimulationDetectors.ORBIT_DECAYED,
            SimulationDetectors.TWO_BODY_DANCE,
            SimulationDetectors.MASS_CHANGED,
            SimulationDetectors.MOON_QUESTION
        )
        for (concept in concepts) {
            val card = TeachingCatalog.card(concept)
            assertNotNull("missing card for $concept", card)
            assertTrue(card!!.whatFa.isNotBlank() && card.whyFa.isNotBlank() && card.moreFa.isNotBlank())
            assertTrue(card.whatEn.isNotBlank() && card.whyEn.isNotBlank() && card.moreEn.isNotBlank())
        }
    }

    @Test
    fun allEightChallengesExistAndAreReachable() {
        assertEquals(8, Challenges.all.size)
        for (ch in Challenges.all) {
            assertTrue(ch.options.size >= 3)
            assertEquals(1, ch.options.count { it.correct })
            assertNotNull("challenge ${ch.kind} needs an explanation card", TeachingCatalog.card(ch.explainConcept))
            // Its preset must build without error.
            val s = SimArrays()
            s.setMetersPerDp(mpd)
            Presets.build(ch.preset, s)
            assertTrue("preset ${ch.preset} must produce bodies", s.n > 0)
        }
    }

    @Test
    fun challengeOutcomesComeFromTheSimulationNotTheAnswerKey() {
        // Collision-momentum: the runner must read the real merge event.
        val vm = vmWith(Preset.EARTH_MOON)
        val challenge = Challenges.all.first { it.kind == com.zig.gravity.edu.ChallengeKind.COLLISION_MOMENTUM }
        vm.startChallenge(challenge)
        vm.submitPrediction("lost") // deliberately the wrong guess
        // Throw the Moon straight into Earth.
        val moonId = vm.arrays.id[1]
        vm.setVelocity(moonId, -3000.0, 0.0)
        var resolved: String? = null
        repeat(600) {
            advance(vm, 1)
            if (vm.challengeResultOptionId != null) resolved = vm.challengeResultOptionId
        }
        assertEquals("momentum is conserved, whatever the user guessed", "same", resolved)
        assertTrue("and the user's wrong guess is preserved for the explanation", vm.challengePrediction == "lost")
    }

    @Test
    fun glossaryCoversTheLockedTerms() {
        val fa = Glossary.terms.map { it.fa }
        for (term in listOf("گرانش", "مدار", "جرم", "شعاع", "سرعت", "سرعت گریز", "تکانه", "برخورد", "ادغام", "سیاه‌چاله", "افق رویداد", "کرم‌چاله", "شبیه‌سازی", "پیش‌بینی", "جسم آزمایشی", "سیارک")) {
            assertTrue("glossary must define $term", fa.contains(term))
        }
        assertTrue(Glossary.terms.all { it.meaningFa.isNotBlank() && it.meaningEn.isNotBlank() })
    }

    @Test
    fun persianDigitsAreUsedForNumbers() {
        val out = com.zig.gravity.util.SandboxFormat.speed(29780.0, persian = true)
        assertTrue("expected Persian digits in \"$out\"", out.any { it in '۰'..'۹' })
        assertTrue("and no Latin digits", out.none { it in '0'..'9' })
        val latin = com.zig.gravity.util.SandboxFormat.speed(29780.0, persian = false)
        assertTrue(latin.any { it in '0'..'9' })
    }
}
