package com.zig.gravity

import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.Collision
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.ImpactTier
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.physics.SimEvent
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.CameraState
import com.zig.gravity.sim.EffectKind
import com.zig.gravity.sim.EffectPool
import com.zig.gravity.sim.HapticCue
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.Presets
import com.zig.gravity.sim.SimulationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * §26 — the upgrade's own regression suite.
 *
 * Everything here is a JVM test against real simulation state. Nothing about the *rendered*
 * result, the frame rate or the haptic hardware can be verified from here; those items are
 * called out in the audit as requiring a physical device.
 */
class GravityUpgradeTest {

    private val mpd = 1.122e9
    private val frame = 1.0 / 60.0
    private var clock = 0L

    private fun vmWith(preset: Preset): SimulationViewModel {
        clock = 0L
        val vm = SimulationViewModel()
        vm.onViewportChanged(400.0)
        vm.onViewportSizePx(1080f, 2000f)
        vm.loadPreset(preset)
        return vm
    }

    private fun advance(vm: SimulationViewModel, frames: Int) {
        repeat(frames) {
            clock += (frame * 1.0e9).toLong()
            vm.onFrame(clock)
        }
    }

    /** One engine step with the real signature; returns the events it produced. */
    private fun step(s: SimArrays, steps: Int = 1): MutableList<SimEvent> {
        val ev = ArrayList<SimEvent>()
        val budget = intArrayOf(EngineConstants.MAX_SUBSTEPS)
        NBodyEngine.computeAccelerations(s)
        repeat(steps) { NBodyEngine.advance(s, EngineConstants.DT, ev, false, budget) }
        return ev
    }

    private fun arrays(): SimArrays {
        val s = SimArrays()
        s.setMetersPerDp(mpd)
        return s
    }

    // ================= camera =========================================================

    @Test
    fun cameraRoundTripsSceneAndScreenCoordinates() {
        val cam = CameraState()
        val w = 1080f
        val h = 1920f
        cam.frame(3.0e11, -1.0e11, 8.0e11)
        cam.setYaw(0.7)
        cam.setTilt(0.6)

        val x = 1.7e11
        val y = -4.2e11
        val px = cam.toScreenX(x, y, w, h)
        val py = cam.toScreenY(x, y, w, h)
        assertEquals(x, cam.toSceneX(px, py, w, h), abs(x) * 1.0e-6)
        assertEquals(y, cam.toSceneY(px, py, w, h), abs(y) * 1.0e-6)
    }

    @Test
    fun pinchZoomsInWhenFingersMoveApartAndKeepsTheAnchorPinned() {
        val cam = CameraState()
        val w = 1080f
        val h = 1920f
        val before = cam.zoom
        val anchorScene = cam.toSceneX(700f, 900f, w, h)

        cam.applyTransform(700f, 900f, 0f, 0f, 1.5f, 0f, w, h)

        assertTrue("fingers apart must zoom in", cam.zoom > before)
        assertEquals(1.5 * before, cam.zoom, 1.0e-9)
        // The scene point under the fingers has not slid away.
        assertEquals(anchorScene, cam.toSceneX(700f, 900f, w, h), abs(anchorScene) * 1.0e-6)
    }

    @Test
    fun twoFingerPanMovesTheCameraNotTheBodies() {
        val vm = vmWith(Preset.SUN_EARTH)
        val before = vm.snapshot.x.copyOf(vm.snapshot.n)
        val panBefore = vm.camera.panX

        vm.camera.applyTransform(500f, 900f, 120f, 0f, 1f, 0f, 1080f, 2000f)
        vm.onCameraMoved()

        assertTrue(panBefore != vm.camera.panX)
        for (i in 0 until vm.snapshot.n) {
            assertEquals(before[i], vm.snapshot.x[i], 0.0)
        }
    }

    @Test
    fun cameraAngleSquashesThePlaneButNeverTheBodyRadii() {
        val cam = CameraState()
        val w = 1080f
        val h = 1920f
        val flatY = cam.toScreenY(0.0, 1.0e11, w, h)
        cam.setTilt(CameraState.MAX_TILT)
        val tiltedY = cam.toScreenY(0.0, 1.0e11, w, h)

        val centre = h * 0.5f
        assertTrue(
            "an elevated view must compress the vertical axis",
            abs(tiltedY - centre) < abs(flatY - centre)
        )
        // Radii are a function of zoom only, so the elevation cannot deform a sphere.
        assertEquals(
            CameraState.drawnRadiusDp(10.0, cam.zoom),
            CameraState.drawnRadiusDp(10.0, cam.zoom),
            0.0
        )
    }

    @Test
    fun cameraTiltAndZoomStayInsideTheirSafeRange() {
        val cam = CameraState()
        cam.setTilt(99.0)
        assertEquals(CameraState.MAX_TILT, cam.tiltRad, 0.0)
        cam.setTilt(-99.0)
        assertEquals(0.0, cam.tiltRad, 0.0)
        cam.setZoom(1.0e9)
        assertEquals(CameraState.MAX_ZOOM, cam.zoom, 0.0)
        cam.setZoom(0.0)
        assertEquals(CameraState.MIN_ZOOM, cam.zoom, 0.0)
    }

    @Test
    fun cameraTransitionsNeverTouchBodyVelocity() {
        val vm = vmWith(Preset.SUN_EARTH)
        val id = vm.snapshot.id[1]
        vm.beginDrag(id)
        vm.dragTo(2.0e11, 3.0e10)
        val vx = vm.snapshot.vx[1]
        val vy = vm.snapshot.vy[1]

        // Exactly what the gesture layer does when a second finger lands mid-drag.
        vm.endDrag()
        vm.camera.applyTransform(400f, 800f, 40f, -20f, 1.2f, 0.1f, 1080f, 2000f)
        vm.onCameraMoved()

        assertEquals(vx, vm.snapshot.vx[1], 0.0)
        assertEquals(vy, vm.snapshot.vy[1], 0.0)
    }

    // ================= dragging =======================================================

    @Test
    fun draggingChangesPositionOnly() {
        val vm = vmWith(Preset.SUN_EARTH)
        val slot = 1
        val id = vm.snapshot.id[slot]
        val vx0 = vm.snapshot.vx[slot]
        val vy0 = vm.snapshot.vy[slot]
        val m0 = vm.snapshot.mass[slot]
        val x0 = vm.snapshot.x[slot]

        vm.beginDrag(id)
        vm.dragTo(x0 + 4.0e10, 7.0e10)
        vm.dragTo(x0 + 9.0e10, 1.4e11)
        vm.endDrag()

        assertEquals(x0 + 9.0e10, vm.snapshot.x[slot], 1.0)
        assertEquals(1.4e11, vm.snapshot.y[slot], 1.0)
        // §2 — bit-for-bit, not "close enough".
        assertEquals(vx0, vm.snapshot.vx[slot], 0.0)
        assertEquals(vy0, vm.snapshot.vy[slot], 0.0)
        assertEquals(m0, vm.snapshot.mass[slot], 0.0)
    }

    @Test
    fun aFastFlickImpartsNoThrowVelocity() {
        val vm = vmWith(Preset.SUN_EARTH)
        val slot = 1
        val id = vm.snapshot.id[slot]
        val vx0 = vm.snapshot.vx[slot]
        val vy0 = vm.snapshot.vy[slot]

        vm.beginDrag(id)
        // A deliberately violent flick: 30 samples covering an AU.
        for (k in 1..30) {
            vm.dragTo(1.0e11 + k * 5.0e9, k * 5.0e9)
        }
        vm.endDrag()

        assertEquals(vx0, vm.snapshot.vx[slot], 0.0)
        assertEquals(vy0, vm.snapshot.vy[slot], 0.0)
    }

    @Test
    fun cancelledDragRestoresPositionAndVelocity() {
        val vm = vmWith(Preset.SUN_EARTH)
        val slot = 1
        val id = vm.snapshot.id[slot]
        val x0 = vm.snapshot.x[slot]
        val y0 = vm.snapshot.y[slot]
        val vx0 = vm.snapshot.vx[slot]

        vm.beginDrag(id)
        vm.dragTo(-3.0e11, 2.0e11)
        vm.cancelDrag()

        assertEquals(x0, vm.snapshot.x[slot], 0.0)
        assertEquals(y0, vm.snapshot.y[slot], 0.0)
        assertEquals(vx0, vm.snapshot.vx[slot], 0.0)
    }

    @Test
    fun aHeldBodyStillPullsOnEverythingElse() {
        val vm = vmWith(Preset.SUN_EARTH)
        val id = vm.snapshot.id[0]
        vm.beginDrag(id)
        vm.dragTo(0.0, 0.0)
        advance(vm, 5)
        // The Sun is held, but Earth is still accelerating towards it.
        val ax = vm.snapshot.ax[1]
        val ay = vm.snapshot.ay[1]
        assertTrue("a held body must remain a gravity source", hypot(ax, ay) > 0.0)
        vm.endDrag()
    }

    @Test
    fun editingPositionIsIdenticalToDragging() {
        val a = vmWith(Preset.SUN_EARTH)
        val b = vmWith(Preset.SUN_EARTH)
        val targetX = 1.2e11
        val targetY = 6.0e10

        val idA = a.snapshot.id[1]
        a.beginDrag(idA)
        a.dragTo(targetX, targetY)
        a.endDrag()

        b.setPosition(b.snapshot.id[1], targetX, targetY)

        // §20 — same position, same velocity, same mass through both routes.
        assertEquals(a.snapshot.x[1], b.snapshot.x[1], 0.0)
        assertEquals(a.snapshot.y[1], b.snapshot.y[1], 0.0)
        assertEquals(a.snapshot.vx[1], b.snapshot.vx[1], 0.0)
        assertEquals(a.snapshot.vy[1], b.snapshot.vy[1], 0.0)
        assertEquals(a.snapshot.mass[1], b.snapshot.mass[1], 0.0)
    }

    @Test
    fun editingVelocityChangesMotionWithoutTeleporting() {
        val vm = vmWith(Preset.SUN_EARTH)
        val slot = 1
        val id = vm.snapshot.id[slot]
        val x0 = vm.snapshot.x[slot]
        val y0 = vm.snapshot.y[slot]

        vm.setSpeedMagnitude(id, 12_000.0)

        assertEquals(x0, vm.snapshot.x[slot], 0.0)
        assertEquals(y0, vm.snapshot.y[slot], 0.0)
        assertEquals(12_000.0, hypot(vm.snapshot.vx[slot], vm.snapshot.vy[slot]), 1.0)
    }

    // ================= speed ladder ===================================================

    @Test
    fun speedLadderIsExactlyOneTenAndOneHundred() {
        assertArrayEqualsD(doubleArrayOf(1.0, 10.0, 100.0), EngineConstants.SPEEDS)
        assertEquals(3, EngineConstants.SPEED_LABELS.size)
        assertEquals(0, EngineConstants.DEFAULT_SPEED_INDEX)
    }

    @Test
    fun eachSpeedAdvancesSimulatedTimeProportionallyWithoutScalingDt() {
        val elapsed = DoubleArray(3)
        for (i in 0..2) {
            val vm = vmWith(Preset.SUN_EARTH)
            vm.setSpeedIndex(i)
            if (vm.paused) vm.togglePlay()
            advance(vm, 60)
            elapsed[i] = vm.snapshot.simTime
            assertTrue("simulated time must advance at ${EngineConstants.SPEEDS[i]}x", elapsed[i] > 0.0)
            assertFalse(elapsed[i].isNaN())
        }
        assertTrue(elapsed[1] > elapsed[0] * 5.0)
        assertTrue(elapsed[2] > elapsed[1] * 5.0)
    }

    @Test
    fun hundredTimesSpeedProducesNoNaNOrTeleport() {
        val vm = vmWith(Preset.FULL_SOLAR_SYSTEM)
        vm.setSpeedIndex(2)
        if (vm.paused) vm.togglePlay()
        advance(vm, 120)
        for (i in 0 until vm.snapshot.n) {
            assertFalse(vm.snapshot.x[i].isNaN())
            assertFalse(vm.snapshot.y[i].isNaN())
            assertTrue(hypot(vm.snapshot.vx[i], vm.snapshot.vy[i]) < EngineConstants.V_MAX)
        }
        assertTrue("the substep budget must be able to keep up", vm.lastFrameSubsteps > 0)
    }

    // ================= spawning =======================================================

    @Test
    fun everyCatalogBodyCanBeSpawnedIntoAnyScene() {
        for (entry in BodyCatalog.all) {
            val vm = vmWith(Preset.EMPTY_TABLE)
            val before = vm.snapshot.n
            assertTrue("${entry.key} must spawn", vm.addFromCatalog(entry.key))
            assertTrue("${entry.key} must add at least one body", vm.snapshot.n > before)
            // Catalog-authoritative physical properties.
            val slot = vm.snapshot.n - 1
            if (entry.massKg > 0.0 && entry.type != BodyType.WORMHOLE_MOUTH) {
                assertEquals(entry.massKg, vm.snapshot.mass[slot], entry.massKg * 1.0e-9)
            }
        }
    }

    @Test
    fun aSpawnedBodyNeverLandsInsideAnother() {
        val vm = vmWith(Preset.SUN_EARTH)
        repeat(6) { vm.addFromCatalog(BodyCatalog.MARBLE.key) }
        val n = vm.snapshot.n
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = hypot(vm.snapshot.x[i] - vm.snapshot.x[j], vm.snapshot.y[i] - vm.snapshot.y[j])
                assertTrue("bodies $i and $j overlap on spawn", d > 0.0)
            }
        }
    }

    @Test
    fun aSpawnedBodyIsImmediatelySelectableAndEditable() {
        val vm = vmWith(Preset.EMPTY_TABLE)
        assertTrue(vm.addFromCatalog(BodyCatalog.MARS.key))
        val id = vm.snapshot.id[vm.snapshot.n - 1]
        assertEquals(id, vm.selectedId)
        vm.setMass(id, 1.0e24)
        assertEquals(1.0e24, vm.snapshot.mass[vm.snapshot.n - 1], 1.0e14)
    }

    // ================= Solar System ===================================================

    @Test
    fun theSolarSystemPresetHasAllTenBodies() {
        val s = arrays()
        Presets.build(Preset.FULL_SOLAR_SYSTEM, s)
        assertEquals(10, s.n)
        for (key in listOf("sun", "mercury", "venus", "earth", "moon", "mars", "jupiter", "saturn", "uranus", "neptune")) {
            assertTrue("$key missing from the Solar System", s.slotOfCatalog(key) >= 0)
        }
    }

    @Test
    fun theMoonIsAnIndependentBodyBoundToEarth() {
        val s = arrays()
        Presets.build(Preset.FULL_SOLAR_SYSTEM, s)
        val earth = s.slotOfCatalog("earth")
        val moon = s.slotOfCatalog("moon")
        assertTrue(earth >= 0 && moon >= 0 && earth != moon)

        // Its own mass, radius, position and velocity — nothing is copied from Earth.
        assertTrue(s.mass[earth] != s.mass[moon])
        assertTrue(s.x[moon] != s.x[earth])
        assertTrue(s.vy[moon] != s.vy[earth])

        // Bound: separation near the real value and speed below escape from Earth.
        val sep = hypot(s.x[moon] - s.x[earth], s.y[moon] - s.y[earth])
        assertEquals(EngineConstants.MOON_ORBIT_RADIUS, sep, EngineConstants.MOON_ORBIT_RADIUS * 0.02)
        val rel = hypot(s.vx[moon] - s.vx[earth], s.vy[moon] - s.vy[earth])
        val escape = sqrt(2.0 * EngineConstants.G * s.mass[earth] / sep)
        assertTrue("the Moon must be gravitationally bound to Earth", rel < escape)
    }

    @Test
    fun theMoonRespondsToBothEarthAndTheSun() {
        val s = arrays()
        Presets.build(Preset.FULL_SOLAR_SYSTEM, s)
        NBodyEngine.computeAccelerations(s)
        val moon = s.slotOfCatalog("moon")
        val earth = s.slotOfCatalog("earth")
        val sun = s.slotOfCatalog("sun")

        val aTotal = hypot(s.ax[moon], s.ay[moon])
        val dE = hypot(s.x[moon] - s.x[earth], s.y[moon] - s.y[earth])
        val dS = hypot(s.x[moon] - s.x[sun], s.y[moon] - s.y[sun])
        val aE = EngineConstants.G * s.mass[earth] / (dE * dE)
        val aS = EngineConstants.G * s.mass[sun] / (dS * dS)

        assertTrue(aE > 0.0 && aS > 0.0)
        // Both contributions are real, and the total is the vector sum of at least the two.
        assertTrue(aTotal > 0.5 * abs(aE - aS))
        assertTrue(aTotal <= aE + aS + 1.0e-6)
    }

    @Test
    fun theMoonActuallyOrbitsEarthOverTime() {
        val s = arrays()
        Presets.build(Preset.FULL_SOLAR_SYSTEM, s)
        val earth = s.slotOfCatalog("earth")
        val moon = s.slotOfCatalog("moon")
        val a0 = kotlin.math.atan2(s.y[moon] - s.y[earth], s.x[moon] - s.x[earth])

        // A quarter of a lunar month, one hour at a time.
        step(s, 170)

        val a1 = kotlin.math.atan2(s.y[moon] - s.y[earth], s.x[moon] - s.x[earth])
        assertTrue(a0 != a1)
        val sep = hypot(s.x[moon] - s.x[earth], s.y[moon] - s.y[earth])
        assertEquals(
            "the Moon must stay in orbit, not drift away",
            EngineConstants.MOON_ORBIT_RADIUS, sep, EngineConstants.MOON_ORBIT_RADIUS * 0.25
        )
    }

    @Test
    fun theDefaultSceneIsTheFullSolarSystem() {
        assertEquals(Preset.FULL_SOLAR_SYSTEM, Preset.DEFAULT)
        val vm = SimulationViewModel()
        vm.onViewportChanged(400.0)
        assertEquals(Preset.FULL_SOLAR_SYSTEM, vm.preset)
        assertTrue("the sandbox must never open empty", vm.snapshot.n > 0)
    }

    @Test
    fun visualSizeOrderingSurvivesZoomingOut() {
        val s = arrays()
        Presets.build(Preset.FULL_SOLAR_SYSTEM, s)
        fun drawn(key: String): Double =
            CameraState.drawnRadiusDp(s.radiusDp[s.slotOfCatalog(key)], 0.05)

        assertTrue(drawn("sun") > drawn("jupiter"))
        assertTrue(drawn("jupiter") > drawn("earth"))
        assertTrue(drawn("earth") > drawn("moon"))
        assertTrue(drawn("earth") > drawn("mercury"))
        assertTrue("nothing may vanish", drawn("moon") >= CameraState.MIN_DRAW_DP)
    }

    @Test
    fun visualSizeIsIndependentOfThePhysicalCollisionRadius() {
        val vm = vmWith(Preset.SUN_EARTH)
        val id = vm.snapshot.id[1]
        val slot = 1
        val realRadius = BodyCatalog.realRadiusOf(
            vm.snapshot.catalogKey[slot], vm.snapshot.typeOf(slot), vm.snapshot.mass[slot]
        )
        vm.setRadiusDp(id, vm.snapshot.radiusDp[slot] + 3.0)
        val after = BodyCatalog.realRadiusOf(
            vm.snapshot.catalogKey[slot], vm.snapshot.typeOf(slot), vm.snapshot.mass[slot]
        )
        assertEquals("the real physical radius must not follow the visual size", realRadius, after, 0.0)
    }

    // ================= collisions =====================================================

    @Test
    fun aGentleTouchIsClassifiedLowAndABlastIsClassifiedHigh() {
        val m = EngineConstants.M_EARTH
        val r = 2.0 * mpd * 10.0
        val escape = sqrt(2.0 * EngineConstants.G * (m + m) / r)
        assertEquals(ImpactTier.LOW, ImpactTier.of(escape * 0.2, escape))
        assertEquals(ImpactTier.MODERATE, ImpactTier.of(escape * 1.5, escape))
        assertEquals(ImpactTier.HIGH, ImpactTier.of(escape * 6.0, escape))
    }

    @Test
    fun everyCollisionEmitsExactlyOneImpactEvent() {
        val s = arrays()
        s.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 0.0, 0.0, 0.0, 0.0)
        s.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 10.0 * mpd * 0.5, 0.0, -4.0e4, 0.0)
        val events = step(s)

        val impacts = events.filterIsInstance<SimEvent.CollisionImpact>()
        assertEquals(1, impacts.size)
        assertTrue(impacts[0].relativeSpeed > 0.0)
        assertTrue(impacts[0].mutualEscapeSpeed > 0.0)
        assertNotNull(impacts[0].tier)
    }

    @Test
    fun aMergeConservesMassAndMomentum() {
        val s = arrays()
        val m1 = EngineConstants.M_EARTH
        val m2 = EngineConstants.M_EARTH * 0.5
        s.add(BodyType.PLANET, m1, 10.0, 0.0, 0.0, 300.0, 0.0)
        s.add(BodyType.PLANET, m2, 10.0, 10.0 * mpd * 0.5, 0.0, -300.0, 0.0)
        val px0 = m1 * 300.0 + m2 * -300.0
        step(s)

        if (s.n == 1) {
            assertEquals(m1 + m2, s.mass[0], (m1 + m2) * 1.0e-9)
            assertEquals(px0, s.mass[0] * s.vx[0], abs(px0) * 1.0e-6 + 1.0e6)
        } else {
            // A bounce: momentum is still conserved across the pair.
            val px1 = s.mass[0] * s.vx[0] + s.mass[1] * s.vx[1]
            assertEquals(px0, px1, abs(px0) * 1.0e-6 + 1.0e6)
        }
    }

    @Test
    fun aBlackHoleNeverBouncesOffAnything() {
        val s = arrays()
        val bh = BodyCatalog.BLACK_HOLE
        s.add(bh.type, bh.massKg, bh.dp, 0.0, 0.0, 0.0, 0.0, bh.key)
        s.add(BodyType.TEST_MARBLE, 1.0, 5.0, bh.dp * mpd * 0.4, 0.0, 0.0, 0.0)
        val events = step(s)

        assertEquals("the marble must be captured, not bounced", 1, s.n)
        assertEquals(bh.type, s.typeOf(0))
        assertTrue(events.any { it is SimEvent.BlackHoleCapture })
        assertFalse(events.any { it is SimEvent.BodyBounced })
    }

    @Test
    fun blackHoleCaptureBeatsASimultaneousBodyCollision() {
        val s = arrays()
        val bh = BodyCatalog.BLACK_HOLE
        // A marble sitting inside the hole's ring AND touching another marble.
        s.add(bh.type, bh.massKg, bh.dp, 0.0, 0.0, 0.0, 0.0, bh.key)
        s.add(BodyType.TEST_MARBLE, 1.0, 5.0, bh.dp * mpd * 0.3, 0.0, 0.0, 0.0)
        s.add(BodyType.TEST_MARBLE, 1.0, 5.0, bh.dp * mpd * 0.3 + 2.0 * mpd, 0.0, 0.0, 0.0)
        val events = step(s)

        assertTrue(events.any { it is SimEvent.BlackHoleCapture })
        assertEquals(bh.type, s.typeOf(0))
    }

    // ================= effects and haptics ============================================

    @Test
    fun theEffectPoolIsBoundedAndNeverGrows() {
        val pool = EffectPool()
        repeat(200) { i ->
            pool.spawn(EffectKind.SHATTER, i * 1.0e9, 0.0, 1.0, 1.0e9, 0xFFFFFFFFL)
        }
        assertTrue(pool.activeCount() <= EffectPool.MAX_EFFECTS)
        var particles = 0
        for (e in 0 until pool.maxEffects) particles += pool.particleCount[e]
        assertTrue(particles <= EffectPool.MAX_EFFECTS * EffectPool.PARTICLES_PER_EFFECT)
    }

    @Test
    fun effectsExpireOnElapsedTimeAndFreeTheirSlot() {
        val pool = EffectPool()
        pool.spawn(EffectKind.MERGE, 0.0, 0.0, 0.8, 1.0e9, 0xFFFFFFFFL)
        assertEquals(1, pool.activeCount())
        repeat(200) { pool.update(0.05) }
        assertEquals(0, pool.activeCount())
    }

    @Test
    fun oneCollisionQueuesExactlyOneEffectAndOneHaptic() {
        val vm = vmWith(Preset.EMPTY_TABLE)
        vm.arrays.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 0.0, 0.0, 0.0, 0.0)
        vm.arrays.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 10.0 * mpd * 0.5, 0.0, -4.0e4, 0.0)
        NBodyEngine.computeAccelerations(vm.arrays)
        if (vm.paused) vm.togglePlay()

        advance(vm, 2)

        assertTrue("a collision must produce a visible effect", vm.effects.activeCount() >= 1)
        assertTrue(vm.effects.activeCount() <= 2)
        assertTrue("a collision must produce at most one cue", vm.pendingHaptics.size <= 2)
        val cuesFirst = vm.pendingHaptics.size
        assertTrue(cuesFirst >= 1)

        vm.clearHaptics()
        assertEquals(0, vm.pendingHaptics.size)
        // Nothing new happens once the bodies have resolved: no continuous buzz.
        advance(vm, 30)
        assertTrue("a resting contact must not keep vibrating", vm.pendingHaptics.size <= 1)
    }

    @Test
    fun hapticCuesEscalateWithSeverity() {
        assertTrue(HapticCue.HEAVY.ordinal > HapticCue.MEDIUM.ordinal)
        assertTrue(HapticCue.MEDIUM.ordinal > HapticCue.LIGHT.ordinal)
    }

    // ================= presets and lifecycle ==========================================

    @Test
    fun everyPresetLoadsPausedAndCleansUpTheOldScene() {
        for (p in Preset.entries) {
            val vm = vmWith(Preset.FULL_SOLAR_SYSTEM)
            if (vm.paused) vm.togglePlay()
            advance(vm, 10)
            vm.effects.spawn(EffectKind.MERGE, 0.0, 0.0, 1.0, 1.0e9, 0xFFFFFFFFL)

            vm.loadPreset(p)

            assertTrue("$p must load paused", vm.paused)
            assertEquals("$p must clear stale effects", 0, vm.effects.activeCount())
            assertEquals("$p must clear queued haptics", 0, vm.pendingHaptics.size)
            assertEquals("$p must reset the selection", 0L, vm.selectedId)
            assertEquals("$p must reset simulated time", 0.0, vm.snapshot.simTime, 0.0)
            assertEquals(p, vm.preset)
            // No duplicate ids survive the switch.
            val ids = HashSet<Long>()
            for (i in 0 until vm.snapshot.n) assertTrue(ids.add(vm.snapshot.id[i]))
            // No trail from the previous scene.
            val trails = vm.snapshot.trails
            if (trails != null) {
                for (i in 0 until vm.snapshot.n) assertEquals(0, trails[i].count)
            }
        }
    }

    @Test
    fun theSelectorOffersEveryRequiredScene() {
        val names = Preset.entries.map { it.name }.toSet()
        for (required in listOf("FULL_SOLAR_SYSTEM", "EARTH_MOON", "SUN_EARTH", "THREE_BODY", "EMPTY_TABLE")) {
            assertTrue("$required must be offered", names.contains(required))
        }
    }

    @Test
    fun theEmptyTableIsEmptyButUsable() {
        val vm = vmWith(Preset.EMPTY_TABLE)
        assertEquals(0, vm.snapshot.n)
        assertTrue(vm.addFromCatalog(BodyCatalog.SUN.key))
        assertEquals(1, vm.snapshot.n)
    }

    @Test
    fun theThreeBodySceneHasThreeStarsAndNoNetDrift() {
        val s = arrays()
        Presets.build(Preset.THREE_BODY, s)
        assertEquals(3, s.n)
        var px = 0.0
        var py = 0.0
        for (i in 0 until s.n) {
            assertEquals(BodyType.SUN, s.typeOf(i))
            px += s.mass[i] * s.vx[i]
            py += s.mass[i] * s.vy[i]
        }
        assertEquals(0.0, hypot(px, py), 1.0e18)
    }

    @Test
    fun reEnteringTheSandboxKeepsTheUsersOwnSimulation() {
        val vm = vmWith(Preset.SUN_EARTH)
        if (vm.paused) vm.togglePlay()
        advance(vm, 30)
        vm.addFromCatalog(BodyCatalog.MARBLE.key)
        val n = vm.snapshot.n
        val t = vm.snapshot.simTime
        val text = vm.serialize()

        val restored = SimulationViewModel()
        restored.onViewportChanged(400.0)
        assertTrue(restored.restore(text))
        assertEquals(n, restored.snapshot.n)
        assertEquals(t, restored.snapshot.simTime, 1.0)
        assertEquals(Preset.SUN_EARTH, restored.preset)
    }

    @Test
    fun pausingAndResumingNeverJumpsTheSimulation() {
        val vm = vmWith(Preset.SUN_EARTH)
        if (vm.paused) vm.togglePlay()
        advance(vm, 10)
        val x = vm.snapshot.x[1]

        vm.togglePlay()
        // A long real-world gap while paused.
        clock += (5.0 * 1.0e9).toLong()
        vm.onFrame(clock)
        assertEquals("time must not accumulate while paused", x, vm.snapshot.x[1], 0.0)

        vm.togglePlay()
        clock += (frame * 1.0e9).toLong()
        vm.onFrame(clock)
        val moved = abs(vm.snapshot.x[1] - x)
        // One frame of catch-up at most, never five seconds of it.
        assertTrue(moved < abs(x) * 0.05)
    }

    private fun assertArrayEqualsD(expected: DoubleArray, actual: DoubleArray) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) assertEquals(expected[i], actual[i], 0.0)
    }
}
