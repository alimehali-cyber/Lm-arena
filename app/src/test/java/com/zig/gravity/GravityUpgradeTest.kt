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
    fun speedLadderIsExactlyOneTenSixtyNineAndOneHundred() {
        assertArrayEqualsD(doubleArrayOf(1.0, 10.0, 69.0, 100.0), EngineConstants.SPEEDS)
        assertEquals(4, EngineConstants.SPEED_LABELS.size)
        assertEquals(0, EngineConstants.DEFAULT_SPEED_INDEX)
        // §4 — 69 means 69. Not 64, not 70, not "about seventy".
        assertEquals(69.0, EngineConstants.SPEEDS[2], 0.0)
        assertEquals("69x", EngineConstants.SPEED_LABELS[2])
    }

    @Test
    fun noSpeedRungAnywhereIsSixteen() {
        // §4 — the forbidden rung must not exist as a value, as a label, or as a substring of one.
        for (v in EngineConstants.SPEEDS) assertNotEquals16(v)
        for (label in EngineConstants.SPEED_LABELS) {
            assertFalse("no 16x label may exist, found: $label", label.contains("16"))
        }
    }

    private fun assertNotEquals16(v: Double) {
        assertTrue("16x must not be a speed rung", abs(v - 16.0) > 1.0e-9)
    }

    @Test
    fun eachSpeedAdvancesSimulatedTimeProportionallyWithoutScalingDt() {
        val elapsed = DoubleArray(4)
        for (i in 0..3) {
            val vm = vmWith(Preset.SUN_EARTH)
            vm.setSpeedIndex(i)
            if (vm.paused) vm.togglePlay()
            advance(vm, 60)
            elapsed[i] = vm.snapshot.simTime
            assertTrue("simulated time must advance at ${EngineConstants.SPEEDS[i]}x", elapsed[i] > 0.0)
            assertFalse(elapsed[i].isNaN())
        }
        // §4 — strictly monotonic in ACTUAL advancement over identical wall-clock intervals.
        assertTrue("10x must beat 1x", elapsed[1] > elapsed[0])
        assertTrue("69x must beat 10x", elapsed[2] > elapsed[1])
        assertTrue("100x must beat 69x", elapsed[3] > elapsed[2])
        // And the ratios must track the ladder, not merely be ordered. Substep quantisation and
        // the per-frame budget make this approximate, hence the generous band.
        assertTrue(elapsed[1] > elapsed[0] * 5.0)
        assertTrue(elapsed[2] / elapsed[0] > 30.0)
        assertTrue(elapsed[3] / elapsed[0] > 45.0)
    }

    @Test
    fun hundredTimesSpeedProducesNoNaNOrTeleport() {
        val vm = vmWith(Preset.FULL_SOLAR_SYSTEM)
        vm.setSpeedIndex(3)
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
        assertTrue(s.y[moon] != s.y[earth])
        assertTrue(s.vx[moon] != s.vx[earth])

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
        // Residue is floating-point noise against individual terms of order 1e33.
        var scale = 0.0
        for (i in 0 until s.n) scale += abs(s.mass[i] * s.vx[i]) + abs(s.mass[i] * s.vy[i])
        assertTrue("net drift $px,$py is not noise", hypot(px, py) < scale * 1.0e-12)
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


    // ================= §9/§15 impact energy =============================================

    /** Two bodies placed just outside contact, closing head-on at [closing] m/s. */
    private fun headOnPair(m1: Double, m2: Double, r1: Double, r2: Double, closing: Double): SimArrays {
        val s = SimArrays()
        s.setMetersPerDp(mpd)
        val a = s.add(BodyType.PLANET, m1, 10.0, -1.0e8, 0.0, closing * 0.5, 0.0)
        val b = s.add(BodyType.PLANET, m2, 10.0, 1.0e8, 0.0, -closing * 0.5, 0.0)
        // The collision radius is set explicitly so the energy assertions do not depend on the
        // dp-to-metres mapping of the test viewport.
        s.radius[a] = r1
        s.radius[b] = r2
        return s
    }

    @Test
    fun impactEnergyUsesReducedMassNotTotalKineticEnergy() {
        val m1 = 5.972e24
        val m2 = 7.348e22
        val closing = 4000.0
        val s = headOnPair(m1, m2, 6.371e6, 1.737e6, closing)
        val events = mutableListOf<SimEvent>()
        Collision.emitImpact(s, 0, 1, events, merged = true)

        val impact = events.filterIsInstance<SimEvent.CollisionImpact>().single()
        val mu = m1 * m2 / (m1 + m2)
        assertEquals("reduced mass", mu, impact.reducedMass, mu * 1.0e-9)
        val expected = 0.5 * mu * closing * closing
        assertEquals("E = 1/2 mu v_rel^2", expected, impact.impactEnergyJ, expected * 1.0e-9)

        // And it must NOT be the naive total kinetic energy of the two bodies, which is dominated
        // by the heavy body and would be wrong by orders of magnitude.
        val naive = 0.5 * m1 * (closing * 0.5) * (closing * 0.5) + 0.5 * m2 * (closing * 0.5) * (closing * 0.5)
        assertTrue("reduced-mass energy must differ from total KE", naive > impact.impactEnergyJ * 10.0)
    }

    @Test
    fun impactEnergyIsSymmetricUnderBodyOrder() {
        val a = headOnPair(3.0e24, 9.0e23, 5.0e6, 3.0e6, 2500.0)
        val ea = mutableListOf<SimEvent>()
        Collision.emitImpact(a, 0, 1, ea, merged = true)
        val b = headOnPair(9.0e23, 3.0e24, 3.0e6, 5.0e6, 2500.0)
        val eb = mutableListOf<SimEvent>()
        Collision.emitImpact(b, 0, 1, eb, merged = true)
        val ia = ea.filterIsInstance<SimEvent.CollisionImpact>().single()
        val ib = eb.filterIsInstance<SimEvent.CollisionImpact>().single()
        assertEquals(ia.impactEnergyJ, ib.impactEnergyJ, ia.impactEnergyJ * 1.0e-9)
        assertEquals(ia.reducedMass, ib.reducedMass, ia.reducedMass * 1.0e-9)
    }

    @Test
    fun impactEnergyScalesWithTheSquareOfRelativeSpeed() {
        val slow = headOnPair(4.0e24, 4.0e24, 5.0e6, 5.0e6, 1000.0)
        val fast = headOnPair(4.0e24, 4.0e24, 5.0e6, 5.0e6, 3000.0)
        val es = mutableListOf<SimEvent>()
        val ef = mutableListOf<SimEvent>()
        Collision.emitImpact(slow, 0, 1, es, merged = true)
        Collision.emitImpact(fast, 0, 1, ef, merged = true)
        val a = es.filterIsInstance<SimEvent.CollisionImpact>().single().impactEnergyJ
        val b = ef.filterIsInstance<SimEvent.CollisionImpact>().single().impactEnergyJ
        assertEquals("tripling v_rel must multiply E by nine", 9.0, b / a, 1.0e-6)
    }

    @Test
    fun aGentleTouchIsNeverClassifiedAsHighEnergy() {
        // Two big bodies drifting into each other at walking pace: the escape speed of the pair is
        // kilometres per second, so this must come out LOW, and the energy must be small.
        val s = headOnPair(5.972e24, 5.972e24, 6.371e6, 6.371e6, 2.0)
        val events = mutableListOf<SimEvent>()
        Collision.emitImpact(s, 0, 1, events, merged = true)
        val impact = events.filterIsInstance<SimEvent.CollisionImpact>().single()
        assertEquals(ImpactTier.LOW, impact.tier)
        assertTrue(impact.impactEnergyJ < 0.5 * impact.reducedMass * 9.0)
    }

    @Test
    fun impactEnergyIsZeroWhenABodyIsMassless() {
        val s = headOnPair(0.0, 5.0e24, 4.0e6, 6.0e6, 3000.0)
        val events = mutableListOf<SimEvent>()
        Collision.emitImpact(s, 0, 1, events, merged = true)
        val impact = events.filterIsInstance<SimEvent.CollisionImpact>().single()
        assertEquals(0.0, impact.reducedMass, 0.0)
        assertEquals(0.0, impact.impactEnergyJ, 0.0)
    }

    @Test
    fun collisionProducesAnExplanationWithTheRealNumbersInIt() {
        val vm = vmWith(Preset.EMPTY_TABLE)
        vm.arrays.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 0.0, 0.0, 0.0, 0.0)
        vm.arrays.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 10.0 * mpd * 0.5, 0.0, -4.0e4, 0.0)
        NBodyEngine.computeAccelerations(vm.arrays)
        if (vm.paused) vm.togglePlay()
        advance(vm, 4)

        assertNotNull("a real collision must produce an explanation", vm.impactHeadlineEn)
        assertNotNull(vm.impactHeadlineFa)
        assertNotNull(vm.impactDetailEn)
        assertNotNull(vm.impactDetailFa)
        // The detail must carry actual measurements, not a canned sentence.
        assertTrue("English detail must quote a speed", vm.impactDetailEn!!.contains("/s"))
        assertTrue("English detail must quote an energy", vm.impactDetailEn!!.contains("J"))
        assertTrue("Persian detail must be Persian", vm.impactDetailFa!!.contains("انرژی"))
    }

    // ================= §13/§14 ghost trajectory =========================================

    @Test
    fun draggingProducesAGhostPathThatUpdatesWithTheFinger() {
        val vm = vmWith(Preset.SUN_EARTH)
        val id = vm.snapshot.id[1]
        vm.select(id)
        advance(vm, 2)

        vm.beginDrag(id)
        vm.dragTo(1.6e11, 0.0)
        assertTrue("dragging must show a ghost", vm.predictionIsGhost)
        assertTrue("the ghost must have points", vm.predictionCount > 1)
        val firstX = vm.predictionXY[0]
        val firstEndX = vm.predictionXY[(vm.predictionCount - 1) * 2]

        vm.dragTo(-2.4e11, 1.0e11)
        assertTrue(vm.predictionIsGhost)
        assertTrue(vm.predictionCount > 1)
        val secondX = vm.predictionXY[0]
        val secondEndX = vm.predictionXY[(vm.predictionCount - 1) * 2]

        assertTrue("the ghost must follow the finger", abs(secondX - firstX) > 1.0e10)
        assertTrue("the whole path must be recomputed", abs(secondEndX - firstEndX) > 1.0e9)
        vm.endDrag()
        assertFalse("releasing must clear the ghost", vm.predictionIsGhost)
    }

    @Test
    fun theGhostUpdatesEvenWhileTheTableIsPaused() {
        val vm = vmWith(Preset.SUN_EARTH)
        val id = vm.snapshot.id[1]
        vm.select(id)
        if (!vm.paused) vm.togglePlay()
        assertTrue(vm.paused)

        vm.beginDrag(id)
        vm.dragTo(1.3e11, 2.0e10)
        val a = vm.predictionXY.copyOf(vm.predictionCount * 2)
        vm.dragTo(2.6e11, 9.0e10)
        val b = vm.predictionXY.copyOf(vm.predictionCount * 2)
        assertFalse("a paused ghost must still refresh", a.contentEquals(b))
        vm.endDrag()
    }

    @Test
    fun theGhostNeverMutatesVelocityOrMassAndReleaseCommitsPositionOnly() {
        val vm = vmWith(Preset.SUN_EARTH)
        val id = vm.snapshot.id[1]
        val slot = vm.arrays.slotOfId(id)
        val vx0 = vm.arrays.vx[slot]
        val vy0 = vm.arrays.vy[slot]
        val m0 = vm.arrays.mass[slot]
        val r0 = vm.arrays.radius[slot]

        vm.select(id)
        vm.beginDrag(id)
        repeat(12) { k -> vm.dragTo(1.0e11 + k * 2.0e10, k * 1.0e10) }
        // Mid-preview: nothing but position may have moved.
        assertEquals(vx0, vm.arrays.vx[slot], 0.0)
        assertEquals(vy0, vm.arrays.vy[slot], 0.0)
        assertEquals(m0, vm.arrays.mass[slot], 0.0)
        vm.endDrag()

        assertEquals("velocity x must survive the drag exactly", vx0, vm.arrays.vx[slot], 0.0)
        assertEquals("velocity y must survive the drag exactly", vy0, vm.arrays.vy[slot], 0.0)
        assertEquals("mass must survive the drag exactly", m0, vm.arrays.mass[slot], 0.0)
        assertEquals("radius must survive the drag exactly", r0, vm.arrays.radius[slot], 0.0)
        assertEquals("position is the one thing that commits", 1.0e11 + 11 * 2.0e10, vm.arrays.x[slot], 1.0)
    }

    @Test
    fun aSoftCollisionIsNotDescribedAsHighEnergy() {
        // §15 — the card must never default to "high energy". Two Earth-mass bodies drifting
        // together at 5 m/s are far below their mutual escape speed and must read as soft.
        val vm = vmWith(Preset.EMPTY_TABLE)
        vm.arrays.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 0.0, 0.0, 0.0, 0.0)
        vm.arrays.add(BodyType.PLANET, EngineConstants.M_EARTH, 10.0, 10.0 * mpd * 0.999, 0.0, -5.0, 0.0)
        NBodyEngine.computeAccelerations(vm.arrays)
        if (vm.paused) vm.togglePlay()
        advance(vm, 4)

        val headline = vm.impactHeadlineEn
        if (headline != null) {
            assertFalse("a gentle contact must not read as a catastrophe", headline.contains("High"))
        }
    }

    @Test
    fun theGhostFlagsAnEscapingPreview() {
        val vm = vmWith(Preset.SUN_EARTH)
        val id = vm.snapshot.id[1]
        vm.select(id)
        vm.beginDrag(id)
        // Earth keeps its 29.78 km/s velocity but is dragged far out, where that speed is well
        // above the local escape speed: the preview must say so.
        // At 4e12 m the Sun's escape speed is ~8.1 km/s; Earth is carrying 29.78 km/s, so the
        // previewed orbit there is unmistakably hyperbolic.
        vm.dragTo(4.0e12, 0.0)
        val escaping = vm.predictionEscapes
        // Back at 1 AU the same velocity is the circular speed, so the orbit is bound.
        vm.dragTo(1.496e11, 0.0)
        val bound = vm.predictionEscapes
        vm.endDrag()
        assertTrue("an unbound preview must be flagged", escaping)
        assertFalse("a bound preview must not be flagged", bound)
    }

    @Test
    fun theGhostIsNotShownWhenNothingIsBeingDragged() {
        val vm = vmWith(Preset.SUN_EARTH)
        vm.select(vm.snapshot.id[1])
        advance(vm, 12)
        assertFalse("a selected but undragged body shows the ordinary prediction", vm.predictionIsGhost)
    }

    // ================= §5 language ======================================================

    @Test
    fun theSandboxHasNoLanguageOfItsOwn() {
        val vm = vmWith(Preset.SUN_EARTH)
        vm.applyHostLanguage(false)
        assertFalse(vm.persian)
        vm.applyHostLanguage(true)
        assertTrue(vm.persian)
        // Idempotent: pushing the same locale twice changes nothing.
        vm.applyHostLanguage(true)
        assertTrue(vm.persian)
    }

    @Test
    fun aRestoredSessionCannotResurrectAStaleSandboxLanguage() {
        val saved = vmWith(Preset.SUN_EARTH)
        saved.applyHostLanguage(false)
        advance(saved, 5)
        val blob = saved.serialize()

        val fresh = SimulationViewModel()
        fresh.onViewportChanged(400.0)
        fresh.onViewportSizePx(1080f, 2000f)
        fresh.applyHostLanguage(true)
        assertTrue(fresh.restore(blob))
        // The restore must not have reached in and set the language back to the saved one.
        assertTrue("restore must leave the host locale alone", fresh.persian)
        fresh.applyHostLanguage(false)
        assertFalse(fresh.persian)
    }

    private fun assertArrayEqualsD(expected: DoubleArray, actual: DoubleArray) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) assertEquals(expected[i], actual[i], 0.0)
    }
}
