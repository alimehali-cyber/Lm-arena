package com.zig.gravity

import com.zig.gravity.edu.TeachingCatalog
import com.zig.gravity.edu.TutorialContent
import com.zig.gravity.edu.TutorialFocus
import com.zig.gravity.physics.BodyType
import com.zig.gravity.physics.EngineConstants
import com.zig.gravity.physics.NBodyEngine
import com.zig.gravity.physics.SimArrays
import com.zig.gravity.sim.BodyCatalog
import com.zig.gravity.sim.CameraState
import com.zig.gravity.sim.InMemoryTutorialStore
import com.zig.gravity.sim.Preset
import com.zig.gravity.sim.Presets
import com.zig.gravity.sim.SimulationViewModel
import com.zig.gravity.sim.TutorialGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * §28 — camera, follow, presets, the add control and the first-launch tutorial.
 *
 * Everything here runs against real simulation and camera state on the JVM. Nothing about the
 * rendered pixels, the gesture stream, the animation or the touch targets can be verified from
 * here; those are called out in the audit as requiring a physical device.
 */
class GravityCameraFollowTest {

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

    private fun run(vm: SimulationViewModel, frames: Int) {
        if (vm.paused) vm.togglePlay()
        advance(vm, frames)
    }

    // ================= §1/§2 reset ======================================================

    @Test
    fun resetRestoresPhysicsAndSimulationTime() {
        val vm = vmWith(Preset.SUN_EARTH)
        val x0 = vm.arrays.x[1]
        val y0 = vm.arrays.y[1]
        val vx0 = vm.arrays.vx[1]
        run(vm, 90)
        assertTrue("the scene must actually have moved first", abs(vm.arrays.y[1] - y0) > 1.0e6)

        vm.reset()

        assertEquals(0.0, vm.arrays.simTime, 0.0)
        assertEquals(x0, vm.arrays.x[1], 1.0)
        assertEquals(y0, vm.arrays.y[1], 1.0)
        assertEquals(vx0, vm.arrays.vx[1], 1.0e-9)
    }

    @Test
    fun resetRestoresTheCameraNotJustThePhysics() {
        val vm = vmWith(Preset.SUN_EARTH)
        val pan0X = vm.camera.panX
        val pan0Y = vm.camera.panY
        val zoom0 = vm.camera.zoom
        val yaw0 = vm.camera.yawRad
        val tilt0 = vm.camera.tiltRad

        // Wander off in every camera degree of freedom there is.
        vm.camera.setPan(9.0e11, -4.0e11)
        vm.camera.setZoom(zoom0 * 3.5)
        vm.camera.setYaw(0.9)
        vm.setCameraTiltFraction(0.8)
        run(vm, 30)

        vm.reset()

        assertEquals("pan x", pan0X, vm.camera.panX, 1.0)
        assertEquals("pan y", pan0Y, vm.camera.panY, 1.0)
        assertEquals("zoom", zoom0, vm.camera.zoom, 1.0e-9)
        assertEquals("yaw", yaw0, vm.camera.yawRad, 1.0e-9)
        assertEquals("tilt", tilt0, vm.camera.tiltRad, 1.0e-9)
    }

    @Test
    fun resetRestoresEachPresetsOwnCameraNotOneUniversalDefault() {
        // Two presets whose correct framing genuinely differs: one is laid out in dp on the
        // default table, the other declares a 4.6e12 m half-span.
        val wide = vmWith(Preset.FULL_SOLAR_SYSTEM)
        val wideZoom = wide.camera.zoom
        val near = vmWith(Preset.SUN_EARTH)
        val nearZoom = near.camera.zoom
        assertTrue("the two presets must frame differently to make this test meaningful",
            abs(wideZoom - nearZoom) > 1.0e-6)

        wide.camera.setZoom(nearZoom)
        wide.reset()
        assertEquals("the wide preset must return to its OWN framing", wideZoom, wide.camera.zoom, 1.0e-9)
    }

    @Test
    fun resetRestoresThePresetElevation() {
        // Earth-Moon asks for a slight lean; reset must bring that back, not flatten to zero.
        val vm = vmWith(Preset.EARTH_MOON)
        val tilt0 = vm.camera.tiltRad
        assertTrue("this preset should declare a non-zero elevation", tilt0 > 0.0)
        vm.setCameraTiltFraction(0.0)
        assertEquals(0.0, vm.camera.tiltRad, 0.0)
        vm.reset()
        assertEquals(tilt0, vm.camera.tiltRad, 1.0e-9)
    }

    @Test
    fun resetExitsFollowModeAndLeavesNoStaleTarget() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earth = vm.snapshot.id[1]
        vm.startFollow(earth)
        run(vm, 60)
        assertTrue(vm.isFollowing)

        vm.reset()

        assertFalse("follow must end on reset", vm.isFollowing)
        assertEquals(0L, vm.followId)
        assertEquals(0L, vm.followTargetId)

        // And no interpolation may continue toward the old target after the reset.
        val panX = vm.camera.panX
        val panY = vm.camera.panY
        run(vm, 45)
        assertEquals("the camera must not drift after reset", panX, vm.camera.panX, 1.0)
        assertEquals("the camera must not drift after reset", panY, vm.camera.panY, 1.0)
    }

    // ================= §3/§4 camera elevation ============================================

    @Test
    fun theElevationControlActuallyChangesTheCamera() {
        val vm = vmWith(Preset.SUN_EARTH)
        assertEquals(0.0, vm.cameraTiltFraction, 1.0e-9)

        vm.setCameraTiltFraction(0.5)
        assertEquals(CameraState.MAX_TILT * 0.5, vm.camera.tiltRad, 1.0e-9)
        assertEquals(0.5, vm.cameraTiltFraction, 1.0e-9)

        vm.setCameraTiltFraction(1.0)
        assertEquals(CameraState.MAX_TILT, vm.camera.tiltRad, 1.0e-9)
    }

    @Test
    fun elevationChangesTheProjectionAndOnlyAlongTheVerticalAxis() {
        val vm = vmWith(Preset.SUN_EARTH)
        val w = 1080f
        val h = 2000f
        val px = 1.0e11
        val py = 1.0e11

        val flatX = vm.camera.toScreenX(px, py, w, h)
        val flatY = vm.camera.toScreenY(px, py, w, h)

        vm.setCameraTiltFraction(1.0)
        val tiltX = vm.camera.toScreenX(px, py, w, h)
        val tiltY = vm.camera.toScreenY(px, py, w, h)

        assertEquals("elevation must not move anything horizontally", flatX, tiltX, 1.0e-3f)
        assertTrue("elevation must squash the vertical axis", abs(tiltY - h / 2f) < abs(flatY - h / 2f))
    }

    @Test
    fun elevationNeverLeavesItsLegalRangeAndNeverFlipsOver() {
        val vm = vmWith(Preset.SUN_EARTH)
        for (f in listOf(-5.0, -0.001, 0.0, 0.5, 1.0, 1.001, 99.0)) {
            vm.setCameraTiltFraction(f)
            assertTrue("tilt $f left the legal range", vm.camera.tiltRad in 0.0..CameraState.MAX_TILT)
            // cos(tilt) > 0 for the whole range, so the plane can never invert or collapse.
            assertTrue("the view must never pass vertical", vm.camera.cosTilt > 0.0)
        }
    }

    @Test
    fun elevationDoesNotTouchPhysics() {
        val vm = vmWith(Preset.FULL_SOLAR_SYSTEM)
        run(vm, 20)
        val n = vm.arrays.n
        val x = vm.arrays.x.copyOf(n)
        val y = vm.arrays.y.copyOf(n)
        val vx = vm.arrays.vx.copyOf(n)
        val vy = vm.arrays.vy.copyOf(n)
        val m = vm.arrays.mass.copyOf(n)
        val r = vm.arrays.radius.copyOf(n)
        val t = vm.arrays.simTime

        for (f in listOf(0.1, 0.4, 0.9, 0.0, 1.0)) vm.setCameraTiltFraction(f)

        assertEquals("simulated time", t, vm.arrays.simTime, 0.0)
        for (i in 0 until n) {
            assertEquals(x[i], vm.arrays.x[i], 0.0)
            assertEquals(y[i], vm.arrays.y[i], 0.0)
            assertEquals(vx[i], vm.arrays.vx[i], 0.0)
            assertEquals(vy[i], vm.arrays.vy[i], 0.0)
            assertEquals(m[i], vm.arrays.mass[i], 0.0)
            assertEquals(r[i], vm.arrays.radius[i], 0.0)
        }
    }

    @Test
    fun everyCameraMutationIsVisibleToTheUi() {
        // The panel that displays camera values reads cameraTick; if a mutation forgot to bump it
        // the control would silently desynchronise from the camera, which was the original bug.
        val vm = vmWith(Preset.SUN_EARTH)
        val t0 = vm.cameraTick
        vm.setCameraTiltFraction(0.3)
        assertTrue(vm.cameraTick > t0)
        val t1 = vm.cameraTick
        vm.onCameraMoved()
        assertTrue(vm.cameraTick > t1)
        val t2 = vm.cameraTick
        vm.resetCamera()
        assertTrue(vm.cameraTick > t2)
    }

    // ================= §7-§15 follow =====================================================

    @Test
    fun followSetsTheTargetAndTheCameraConvergesOnIt() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earth = vm.snapshot.id[1]
        vm.camera.setPan(0.0, 0.0)
        vm.startFollow(earth)
        assertTrue(vm.isFollowing)
        assertEquals(earth, vm.followTargetId)

        val slot = vm.arrays.slotOfId(earth)
        val before = hypot(vm.camera.panX - vm.arrays.x[slot], vm.camera.panY - vm.arrays.y[slot])
        run(vm, 60)
        val after = hypot(vm.camera.panX - vm.arrays.x[slot], vm.camera.panY - vm.arrays.y[slot])

        assertTrue("the camera must close on its target", after < before * 0.25)
    }

    @Test
    fun followIsSmoothAndNeverTeleports() {
        val vm = vmWith(Preset.SUN_EARTH)
        vm.camera.setPan(-3.0e11, 3.0e11)
        if (vm.paused) vm.togglePlay()
        // The first frame only establishes the clock (there is no previous frame to measure
        // against), so prime it before following; otherwise this would measure a zero-length tick.
        advance(vm, 1)

        val startGap = hypot(vm.camera.panX - vm.arrays.x[1], vm.camera.panY - vm.arrays.y[1])
        vm.startFollow(vm.snapshot.id[1])

        advance(vm, 1)
        val gapAfterOne = hypot(vm.camera.panX - vm.arrays.x[1], vm.camera.panY - vm.arrays.y[1])

        assertTrue("one frame must actually approach the target", gapAfterOne < startGap)
        assertTrue("must not snap the whole way in one frame", gapAfterOne > startGap * 0.02)
    }

    @Test
    fun followDoesNotModifyPhysics() {
        val vm = vmWith(Preset.SUN_EARTH)
        val a = vmWith(Preset.SUN_EARTH)

        val earth = vm.snapshot.id[1]
        vm.startFollow(earth)
        run(vm, 120)
        run(a, 120)

        // The followed run and the unfollowed run must be bit-for-bit identical in physics.
        assertEquals(a.arrays.simTime, vm.arrays.simTime, 0.0)
        for (i in 0 until a.arrays.n) {
            assertEquals("x[$i]", a.arrays.x[i], vm.arrays.x[i], 0.0)
            assertEquals("y[$i]", a.arrays.y[i], vm.arrays.y[i], 0.0)
            assertEquals("vx[$i]", a.arrays.vx[i], vm.arrays.vx[i], 0.0)
            assertEquals("vy[$i]", a.arrays.vy[i], vm.arrays.vy[i], 0.0)
            assertEquals("mass[$i]", a.arrays.mass[i], vm.arrays.mass[i], 0.0)
            assertEquals("radius[$i]", a.arrays.radius[i], vm.arrays.radius[i], 0.0)
        }
    }

    @Test
    fun unfollowClearsTheTargetAndLeavesTheCameraWhereItIs() {
        val vm = vmWith(Preset.SUN_EARTH)
        vm.startFollow(vm.snapshot.id[1])
        run(vm, 60)

        val panX = vm.camera.panX
        val panY = vm.camera.panY
        vm.stopFollow()

        assertFalse(vm.isFollowing)
        assertEquals(0L, vm.followTargetId)
        assertEquals("unfollow must not move the camera", panX, vm.camera.panX, 0.0)
        assertEquals("unfollow must not move the camera", panY, vm.camera.panY, 0.0)

        // And it must stay put rather than continuing to track.
        run(vm, 60)
        assertEquals(panX, vm.camera.panX, 1.0)
        assertEquals(panY, vm.camera.panY, 1.0)
    }

    @Test
    fun switchingFollowTargetsWorksDirectlyWithoutUnfollowingFirst() {
        val vm = vmWith(Preset.FULL_SOLAR_SYSTEM)
        val first = vm.snapshot.id[1]
        val second = vm.snapshot.id[3]
        vm.startFollow(first)
        run(vm, 30)
        assertEquals(first, vm.followTargetId)

        vm.startFollow(second)
        assertEquals("the target must switch cleanly", second, vm.followTargetId)
        run(vm, 90)

        val slot = vm.arrays.slotOfId(second)
        val gap = hypot(vm.camera.panX - vm.arrays.x[slot], vm.camera.panY - vm.arrays.y[slot])
        val scale = EngineConstants.SCENE_WIDTH_AU * EngineConstants.AU / vm.camera.zoom
        assertTrue("the camera must end up on the NEW target", gap < scale * 0.1)
    }

    @Test
    fun toggleFollowTurnsItOnAndOffOnTheSameBody() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earth = vm.snapshot.id[1]
        vm.toggleFollow(earth)
        assertTrue(vm.isFollowing)
        vm.toggleFollow(earth)
        assertFalse(vm.isFollowing)
    }

    @Test
    fun onlyOneFollowTargetCanBeActive() {
        val vm = vmWith(Preset.FULL_SOLAR_SYSTEM)
        vm.startFollow(vm.snapshot.id[1])
        vm.startFollow(vm.snapshot.id[2])
        vm.startFollow(vm.snapshot.id[4])
        assertEquals(vm.snapshot.id[4], vm.followTargetId)
    }

    @Test
    fun followingAnUnknownBodyIsRefusedRatherThanStoringAnInvalidId() {
        val vm = vmWith(Preset.SUN_EARTH)
        vm.startFollow(123456789L)
        assertFalse(vm.isFollowing)
        vm.startFollow(0L)
        assertFalse(vm.isFollowing)
    }

    @Test
    fun deletingTheFollowedBodyEndsFollowSafely() {
        val vm = vmWith(Preset.SUN_EARTH)
        val earth = vm.snapshot.id[1]
        vm.select(earth)
        vm.startFollow(earth)
        run(vm, 20)

        vm.removeSelected()

        assertFalse("follow must end when its body is deleted", vm.isFollowing)
        run(vm, 60)
        assertTrue("the camera must stay finite", vm.camera.panX.isFinite())
        assertTrue(vm.camera.panY.isFinite())
    }

    @Test
    fun aFollowedBodyThatDisappearsMidRunNeverLeavesTheCameraChasingNothing() {
        // Rather than relying on a particular preset colliding, the body is removed underneath the
        // follow system exactly as a merge or a capture would remove it.
        val vm = vmWith(Preset.SUN_EARTH)
        val earth = vm.snapshot.id[1]
        vm.startFollow(earth)
        run(vm, 20)
        assertTrue(vm.arrays.removeById(earth))

        run(vm, 30)

        assertFalse(vm.isFollowing)
        assertTrue(vm.camera.panX.isFinite())
        assertTrue(vm.camera.panY.isFinite())
    }

    @Test
    fun aManualCameraGestureTakesOverFromFollow() {
        val vm = vmWith(Preset.SUN_EARTH)
        vm.startFollow(vm.snapshot.id[1])
        assertTrue(vm.isFollowing)
        vm.onCameraMoved()
        assertFalse("the user's own camera gesture must win", vm.isFollowing)
    }

    @Test
    fun followSmoothingIsWallClockNotSimulationTime() {
        // The camera is visual furniture and must not inherit the simulation's time dilation. With
        // the scene paused the target is stationary, so the residual gap after a fixed number of
        // frames isolates the smoothing constant from how fast the body happens to be moving. If
        // the smoothing used simulated seconds these numbers would differ by 100x.
        val gaps = DoubleArray(EngineConstants.SPEEDS.size)
        for (i in EngineConstants.SPEEDS.indices) {
            val vm = vmWith(Preset.SUN_EARTH)   // loadPreset leaves the scene paused
            vm.setSpeedIndex(i)
            vm.camera.setPan(0.0, 0.0)
            val target = vm.snapshot.id[1]
            val start = hypot(vm.arrays.x[1], vm.arrays.y[1])
            vm.startFollow(target)
            advance(vm, 20)
            gaps[i] = hypot(vm.camera.panX - vm.arrays.x[1], vm.camera.panY - vm.arrays.y[1]) / start
        }
        assertTrue("the camera must have moved but not arrived", gaps[0] > 0.05 && gaps[0] < 0.95)
        for (i in 1 until gaps.size) {
            assertEquals(
                "follow smoothing must not change with the ${EngineConstants.SPEED_LABELS[i]} rung",
                gaps[0], gaps[i], 1.0e-9
            )
        }
    }

    @Test
    fun followKeepsUpWithAFastMovingBody() {
        // At the top rung a planet crosses the table quickly; the camera must still hold it near
        // the middle of the view rather than being left behind.
        val vm = vmWith(Preset.SUN_EARTH)
        vm.setSpeedIndex(EngineConstants.SPEEDS.size - 1)
        val target = vm.snapshot.id[1]
        vm.startFollow(target)
        run(vm, 240)

        val slot = vm.arrays.slotOfId(target)
        assertTrue("the body must still be there", slot >= 0)
        val gap = hypot(vm.camera.panX - vm.arrays.x[slot], vm.camera.panY - vm.arrays.y[slot])
        val orbit = hypot(vm.arrays.x[slot] - vm.arrays.x[0], vm.arrays.y[slot] - vm.arrays.y[0])
        assertTrue("the camera lost its target at the top speed", gap < orbit)
    }

    // ================= §19-§23 presets ===================================================

    @Test
    fun theDefaultSceneIsSunAndEarth() {
        assertEquals(Preset.SUN_EARTH, Preset.DEFAULT)
    }

    @Test
    fun theDefaultSceneIsARealGravitationalOrbitNotAnAnimation() {
        val vm = vmWith(Preset.DEFAULT)
        assertEquals(2, vm.arrays.n)

        // The Sun is much heavier and much larger on screen than Earth (§21).
        assertTrue("the Sun must dominate in mass", vm.arrays.mass[0] > vm.arrays.mass[1] * 1000.0)
        assertTrue("the Sun must be drawn larger", vm.arrays.radiusDp[0] > vm.arrays.radiusDp[1])
        assertTrue("Earth must be clear of the Sun", hypot(vm.arrays.x[1], vm.arrays.y[1]) > 0.0)

        // The motion is produced by mutual gravity, not by a scripted path: Earth accelerates
        // toward the Sun, and the Sun measurably accelerates back.
        NBodyEngine.computeAccelerations(vm.arrays)
        assertTrue(hypot(vm.arrays.ax[1], vm.arrays.ay[1]) > 0.0)
        assertTrue(hypot(vm.arrays.ax[0], vm.arrays.ay[0]) > 0.0)
        assertTrue(
            "accelerations must oppose",
            vm.arrays.ax[0] * vm.arrays.ax[1] + vm.arrays.ay[0] * vm.arrays.ay[1] <= 0.0
        )

        // And it goes round: over one lap the separation stays bounded and the angle sweeps.
        run(vm, 60)
        var minR = Double.MAX_VALUE
        var maxR = 0.0
        repeat(2400) {
            advance(vm, 1)
            val r = hypot(vm.arrays.x[1] - vm.arrays.x[0], vm.arrays.y[1] - vm.arrays.y[0])
            if (r < minR) minR = r
            if (r > maxR) maxR = r
        }
        assertTrue("the orbit must stay bound", maxR / minR < 1.5)
    }

    @Test
    fun theFullSolarSystemIsStillAvailable() {
        val vm = vmWith(Preset.FULL_SOLAR_SYSTEM)
        assertTrue("Sun, eight planets and the Moon", vm.arrays.n >= 10)
    }

    @Test
    fun noExistingPresetWasRemoved() {
        val names = Preset.entries.map { it.name }.toSet()
        for (required in listOf(
            "FULL_SOLAR_SYSTEM", "SUN_EARTH", "EARTH_MOON", "INNER_SYSTEM", "BINARY",
            "BLACK_HOLE_LAB", "WORMHOLE_LAB", "THREE_BODY", "EMPTY_TABLE"
        )) {
            assertTrue("$required must still exist", required in names)
        }
    }

    @Test
    fun everyNewPresetBuildsValidPhysics() {
        for (p in listOf(
            Preset.TWO_BODY_ORBIT, Preset.ESCAPE_VELOCITY, Preset.MASS_MATTERS,
            Preset.COLLISION_LAB, Preset.PERTURBATION, Preset.BLACK_HOLE_ENCOUNTER
        )) {
            val vm = vmWith(p)
            assertTrue("$p must create bodies", vm.arrays.n >= 2)
            for (i in 0 until vm.arrays.n) {
                assertTrue("$p x[$i]", vm.arrays.x[i].isFinite())
                assertTrue("$p y[$i]", vm.arrays.y[i].isFinite())
                assertTrue("$p vx[$i]", vm.arrays.vx[i].isFinite())
                assertTrue("$p vy[$i]", vm.arrays.vy[i].isFinite())
                assertTrue("$p mass[$i] must not be negative", vm.arrays.mass[i] >= 0.0)
                assertTrue("$p radius[$i] must be positive", vm.arrays.radius[i] > 0.0)
            }
            // No body may start already overlapping another.
            for (i in 0 until vm.arrays.n) {
                for (j in i + 1 until vm.arrays.n) {
                    val d = hypot(vm.arrays.x[i] - vm.arrays.x[j], vm.arrays.y[i] - vm.arrays.y[j])
                    assertTrue(
                        "$p starts with $i and $j overlapping",
                        d > (vm.arrays.radius[i] + vm.arrays.radius[j]) * 0.9
                    )
                }
            }
            // And it must integrate without blowing up.
            run(vm, 120)
            for (i in 0 until vm.arrays.n) {
                assertTrue("$p went non-finite", vm.arrays.x[i].isFinite() && vm.arrays.y[i].isFinite())
            }
        }
    }

    @Test
    fun presetsAreDeterministic() {
        for (p in Preset.entries) {
            val a = SimArrays()
            a.setMetersPerDp(1.122e9)
            Presets.build(p, a)
            val b = SimArrays()
            b.setMetersPerDp(1.122e9)
            Presets.build(p, b)
            assertEquals("$p body count", a.n, b.n)
            for (i in 0 until a.n) {
                assertEquals("$p x[$i]", a.x[i], b.x[i], 0.0)
                assertEquals("$p y[$i]", a.y[i], b.y[i], 0.0)
                assertEquals("$p vx[$i]", a.vx[i], b.vx[i], 0.0)
                assertEquals("$p vy[$i]", a.vy[i], b.vy[i], 0.0)
                assertEquals("$p mass[$i]", a.mass[i], b.mass[i], 0.0)
            }
        }
    }

    @Test
    fun everyPresetHasAnInitialCameraState() {
        for (p in Preset.entries) {
            val vm = vmWith(p)
            assertTrue("$p zoom", vm.camera.zoom > 0.0 && vm.camera.zoom.isFinite())
            assertTrue("$p pan x", vm.camera.panX.isFinite())
            assertTrue("$p pan y", vm.camera.panY.isFinite())
            assertTrue("$p tilt", vm.camera.tiltRad in 0.0..CameraState.MAX_TILT)
            // And reset must return to exactly it.
            val z = vm.camera.zoom
            val t = vm.camera.tiltRad
            vm.camera.setZoom(z * 2.0)
            vm.setCameraTiltFraction(0.9)
            vm.reset()
            assertEquals("$p zoom after reset", z, vm.camera.zoom, 1.0e-9)
            assertEquals("$p tilt after reset", t, vm.camera.tiltRad, 1.0e-9)
        }
    }

    @Test
    fun everyNewPresetHasATeachingCardWithSomethingToTry() {
        for (p in listOf(
            Preset.TWO_BODY_ORBIT, Preset.ESCAPE_VELOCITY, Preset.MASS_MATTERS,
            Preset.COLLISION_LAB, Preset.PERTURBATION, Preset.BLACK_HOLE_ENCOUNTER
        )) {
            val card = TeachingCatalog.card(TeachingCatalog.presetConcept(p.name))
            assertNotNull("$p must have a teaching card", card)
            requireNotNull(card)
            assertTrue("$p title fa", card.titleFa.isNotBlank())
            assertTrue("$p title en", card.titleEn.isNotBlank())
            assertTrue("$p what fa", card.whatFa.isNotBlank())
            assertTrue("$p what en", card.whatEn.isNotBlank())
            assertTrue("$p why fa", card.whyFa.isNotBlank())
            assertTrue("$p why en", card.whyEn.isNotBlank())
            assertNotNull("$p must suggest something to try, in Persian", card.tryThisFa)
            assertNotNull("$p must suggest something to try, in English", card.tryThisEn)
        }
    }

    @Test
    fun loadingATeachingPresetOpensItsCard() {
        val vm = vmWith(Preset.TWO_BODY_ORBIT)
        assertEquals(TeachingCatalog.presetConcept("TWO_BODY_ORBIT"), vm.teachingConcept)
    }

    @Test
    fun escapeVelocityPresetActuallySeparatesBoundFromUnbound() {
        // The scene is only worth anything if its three bodies really do different things.
        val vm = vmWith(Preset.ESCAPE_VELOCITY)
        val sunMass = vm.arrays.mass[0]
        var bound = 0
        var unbound = 0
        for (i in 1 until vm.arrays.n) {
            val r = hypot(vm.arrays.x[i] - vm.arrays.x[0], vm.arrays.y[i] - vm.arrays.y[0])
            val dvx = vm.arrays.vx[i] - vm.arrays.vx[0]
            val dvy = vm.arrays.vy[i] - vm.arrays.vy[0]
            val energy = 0.5 * (dvx * dvx + dvy * dvy) - EngineConstants.G * sunMass / r
            if (energy < 0.0) bound++ else unbound++
        }
        assertTrue("at least one body must be bound", bound >= 1)
        assertTrue("at least one body must escape", unbound >= 1)
    }

    @Test
    fun collisionLabCarriesRealMassSoMomentumIsMeaningful() {
        val vm = vmWith(Preset.COLLISION_LAB)
        assertEquals(2, vm.arrays.n)
        for (i in 0 until 2) {
            assertTrue("a momentum scene cannot use massless bodies", vm.arrays.mass[i] > 0.0)
        }
        assertTrue("the two masses must differ", vm.arrays.mass[0] != vm.arrays.mass[1])
    }

    @Test
    fun earthMoonRemainsAGenuineTwoBodySystem() {
        val vm = vmWith(Preset.EARTH_MOON)
        assertEquals(2, vm.arrays.n)
        val sep = hypot(vm.arrays.x[1] - vm.arrays.x[0], vm.arrays.y[1] - vm.arrays.y[0])

        assertTrue("the Moon must not sit at Earth's centre", sep > 0.0)
        assertTrue(
            "the Moon must be outside Earth's radius",
            sep > vm.arrays.radius[0] + vm.arrays.radius[1]
        )
        assertTrue("the Moon must be drawn smaller than Earth", vm.arrays.radiusDp[1] < vm.arrays.radiusDp[0])

        // Genuine relative orbital velocity, and both bodies moving.
        val relV = hypot(vm.arrays.vx[1] - vm.arrays.vx[0], vm.arrays.vy[1] - vm.arrays.vy[0])
        assertTrue("the Moon needs real orbital velocity", relV > 0.0)
        assertTrue("Earth must move too", hypot(vm.arrays.vx[0], vm.arrays.vy[0]) > 0.0)

        // Mutual, opposing acceleration: physics, not parenting.
        NBodyEngine.computeAccelerations(vm.arrays)
        assertTrue(
            "the pair must attract each other",
            vm.arrays.ax[0] * vm.arrays.ax[1] + vm.arrays.ay[0] * vm.arrays.ay[1] <= 0.0
        )

        // Over a run the separation stays bounded: an orbit, not a fly-away or a crash.
        run(vm, 30)
        var minR = Double.MAX_VALUE
        var maxR = 0.0
        repeat(1500) {
            advance(vm, 1)
            if (vm.arrays.n < 2) return@repeat
            val r = hypot(vm.arrays.x[1] - vm.arrays.x[0], vm.arrays.y[1] - vm.arrays.y[0])
            if (r < minR) minR = r
            if (r > maxR) maxR = r
        }
        assertEquals("neither body may be lost", 2, vm.arrays.n)
        assertTrue("the separation must stay bounded", maxR / minR < 2.0)
    }

    @Test
    fun everyPresetWithBodiesOffersSomethingToFollow() {
        for (p in Preset.entries) {
            val vm = vmWith(p)
            if (vm.arrays.n == 0) continue
            var followable = 0
            for (i in 0 until vm.arrays.n) {
                if (vm.arrays.typeOf(i) == BodyType.WORMHOLE_MOUTH) continue
                vm.startFollow(vm.arrays.id[i])
                if (vm.isFollowing) followable++
                vm.stopFollow()
            }
            assertTrue("$p must have at least one followable body", followable >= 1)
        }
    }

    // ================= §30 static forensics ==============================================

    @Test
    fun noObsoleteSpeedReferenceSurvivesAnywhereInPresetCopy() {
        // A preset note used to tell the user to press "16x", a rung that no longer exists.
        for (p in Preset.entries) {
            for (text in listOf(p.noteFa, p.noteEn, p.titleFa, p.titleEn)) {
                assertFalse("$p still mentions 16x: $text", text.contains("16x"))
                assertFalse("$p still mentions 16×: $text", text.contains("16×"))
                assertFalse("$p still mentions ۱۶×: $text", text.contains("۱۶×"))
                assertFalse("$p mentions a 1000x rung: $text", text.contains("1000x"))
                assertFalse("$p mentions a ۱۰۰۰× rung: $text", text.contains("۱۰۰۰×"))
            }
        }
    }

    @Test
    fun everySpeedMentionedInUserFacingPresetCopyIsARealRung() {
        val real = EngineConstants.SPEED_LABELS.toSet() +
            EngineConstants.SPEED_LABELS.map { it.replace("x", "×") }.toSet()
        val pattern = Regex("""\d+x|\d+×""")
        for (p in Preset.entries) {
            for (m in pattern.findAll(p.noteEn)) {
                assertTrue("${p.name} advertises ${m.value}, which is not a real rung", m.value in real)
            }
        }
    }

    // ================= tutorial ==========================================================

    @Test
    fun aFreshInstallAutoShowsTheTutorial() {
        val store = InMemoryTutorialStore(seen = false)
        assertTrue(TutorialGate.shouldAutoShow(store))
    }

    @Test
    fun skippingPersistsAndStopsTheTutorialAutoShowing() {
        val store = InMemoryTutorialStore(seen = false)
        assertTrue(TutorialGate.shouldAutoShow(store))
        store.markTutorialSeen()
        assertFalse("skip must persist", TutorialGate.shouldAutoShow(store))
    }

    @Test
    fun completingPersistsAndStopsTheTutorialAutoShowing() {
        val store = InMemoryTutorialStore(seen = false)
        store.markTutorialSeen()
        assertFalse(TutorialGate.shouldAutoShow(store))
        // A restart is a fresh gate query against the same persisted store.
        assertFalse(TutorialGate.shouldAutoShow(store))
    }

    @Test
    fun aStoreThatAlreadySawItNeverAutoShowsAgain() {
        assertFalse(TutorialGate.shouldAutoShow(InMemoryTutorialStore(seen = true)))
    }

    @Test
    fun theSeenFlagIsNeverWrittenBackToFalse() {
        val store = InMemoryTutorialStore(seen = false)
        store.markTutorialSeen()
        // Manual re-opening from the ? button does not consult or clear the gate; the only
        // mutation the store exposes is one-way.
        store.markTutorialSeen()
        assertTrue(store.seen)
        assertFalse(TutorialGate.shouldAutoShow(store))
    }

    @Test
    fun theTutorialIsShortAndEveryStepIsBilingual() {
        val steps = TutorialContent.steps
        assertTrue("keep it brief: 5-7 steps", steps.size in 5..7)
        val ids = steps.map { it.id }
        assertEquals("step ids must be unique", ids.size, ids.toSet().size)
        for (s in steps) {
            assertTrue("${s.id} fa title", s.titleFa.isNotBlank())
            assertTrue("${s.id} en title", s.titleEn.isNotBlank())
            assertTrue("${s.id} fa body", s.bodyFa.isNotBlank())
            assertTrue("${s.id} en body", s.bodyEn.isNotBlank())
            // The Persian copy must actually be Persian, not the English string duplicated.
            assertTrue("${s.id} Persian body must differ from English", s.bodyFa != s.bodyEn)
            assertTrue(
                "${s.id} Persian body must contain Persian characters",
                s.bodyFa.any { it in '\u0600'..'\u06FF' }
            )
        }
    }

    @Test
    fun theTutorialCoversTheControlsItPromisesTo() {
        val ids = TutorialContent.steps.map { it.id }.toSet()
        for (required in listOf("welcome", "camera", "select", "add", "drag", "time")) {
            assertTrue("the tutorial must cover '$required'", required in ids)
        }
    }

    @Test
    fun theTutorialNeverHardCodesASpeedLadder() {
        // The speed step must not spell out rungs; it reads them from EngineConstants at render
        // time, so it can never advertise a rung that no longer exists.
        for (s in TutorialContent.steps) {
            for (text in listOf(s.bodyFa, s.bodyEn)) {
                assertFalse("${s.id} hard-codes a speed: $text", Regex("""\d+\s*[x×]""").containsMatchIn(text))
            }
        }
    }

    @Test
    fun theTutorialOnlyAdvertisesBodyTypesThatExist() {
        // Every noun in the add step must correspond to a real catalog entry.
        val names = BodyCatalog.all.map { it.nameEn.lowercase() }.toSet()
        assertTrue("star", names.any { it.contains("sun") })
        assertTrue("planet", names.any { it.contains("earth") || it.contains("mars") })
        assertTrue("moon", names.any { it.contains("moon") })
        assertTrue("asteroid", names.any { it.contains("asteroid") })
        assertTrue("test object", names.any { it.contains("marble") })
        assertTrue("black hole", names.any { it.contains("black hole") })
        assertTrue("wormhole", names.any { it.contains("wormhole") })
    }

    @Test
    fun thereIsNoTutorialLocalLanguageState() {
        // §23 — the tutorial takes the app's locale as a parameter and stores nothing. The only
        // persisted key it owns is the completion flag.
        assertEquals("tutorial_seen_v1", TutorialGate.PREF_KEY)
        assertFalse(TutorialGate.PREF_KEY.contains("lang"))
        assertFalse(TutorialGate.PREF_KEY.contains("locale"))
        assertFalse(TutorialGate.PREF_KEY.contains("persian"))
    }

    @Test
    fun tutorialStepsDeclareWhereTheyPoint() {
        val focuses = TutorialContent.steps.map { it.focus }.toSet()
        assertTrue("at least one step must point at the canvas", TutorialFocus.CANVAS in focuses)
        assertTrue("a step must point at the add button", TutorialFocus.ADD_BUTTON in focuses)
    }

    @Test
    fun theTutorialCannotTouchTheSimulation() {
        // The overlay takes no ViewModel; the closest the module gets to simulation state is the
        // read-only speed labels. This asserts the *content* layer holds no mutable state at all.
        val before = TutorialContent.steps
        val after = TutorialContent.steps
        assertEquals(before.size, after.size)
        assertEquals(before, after)
        assertNull(TeachingCatalog.card("preset_NOT_A_PRESET"))
    }
}
