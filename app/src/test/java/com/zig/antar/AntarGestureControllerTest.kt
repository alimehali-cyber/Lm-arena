package com.zig.antar

import com.zig.antar.config.AntarCameraMode
import com.zig.antar.config.AntarSimulationSettings
import com.zig.antar.gesture.AntarGestureController
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AntarGestureControllerTest {

    @Test
    fun singleFingerDragMovesWithScreenIntuitionAndUnrotatesHorizonRoll() {
        val controller = AntarGestureController()
        val base = AntarSimulationSettings.DEFAULT.copy(
            azimuthDeg = 0.0,
            inclinationDeg = 80.0,
            rollDeg = 0.0,
            fovDeg = 50.0
        )
        controller.onGestureStart(base)

        // Dragging right (dx > 0) decreases azimuth so the front of the disk moves right with the finger.
        val draggedRight = controller.onSingleFingerDrag(
            settings = base,
            dxPx = 40f,
            dyPx = 0f,
            dtSec = 1.0 / 60.0,
            viewportWidthPx = 1080f,
            viewportHeightPx = 2200f
        )
        assertTrue(draggedRight.azimuthDeg < base.azimuthDeg)
        assertEquals(base.inclinationDeg, draggedRight.inclinationDeg, 1e-6)

        // With 90-degree horizon roll (e.g. M87* EHT preset), dragging right on screen un-rotates into dyWorld > 0,
        // tilting inclination instead of rotating orthogonal to the finger!
        val rolled = base.copy(rollDeg = 90.0)
        controller.onGestureStart(rolled)
        val draggedRolled = controller.onSingleFingerDrag(
            settings = rolled,
            dxPx = 40f,
            dyPx = 0f,
            dtSec = 1.0 / 60.0,
            viewportWidthPx = 1080f,
            viewportHeightPx = 2200f
        )
        assertEquals(0.0, draggedRolled.azimuthDeg, 1e-4)
        assertTrue(draggedRolled.inclinationDeg < rolled.inclinationDeg)
    }

    @Test
    fun grabbingDuringCinematicOrTourHandsOffSeamlesslyToOrbitAtLivePose() {
        val controller = AntarGestureController()
        val cinematic = AntarSimulationSettings.DEFAULT.copy(cameraMode = AntarCameraMode.CINEMATIC)
        val handedOff = controller.onGestureStart(
            settings = cinematic,
            liveAzimuthDeg = 123.4,
            liveInclinationDeg = 67.8,
            liveDistanceM = 24.5
        )
        assertEquals(AntarCameraMode.ORBIT, handedOff.cameraMode)
        assertEquals(123.4, handedOff.azimuthDeg, 1e-6)
        assertEquals(67.8, handedOff.inclinationDeg, 1e-6)
        assertEquals(24.5, handedOff.distanceM, 1e-6)
    }

    @Test
    fun inertialGlideIsFrameRateIndependentAndDwellSuppressesFling() {
        val base = AntarSimulationSettings.DEFAULT.copy(azimuthDeg = 0.0, inclinationDeg = 80.0)

        fun simulateInertia(hz: Int): Double {
            val c = AntarGestureController()
            var s = c.onGestureStart(base)
            repeat(5) {
                s = c.onSingleFingerDrag(s, dxPx = 18f, dyPx = 0f, dtSec = 1.0 / 60.0, viewportWidthPx = 1080f, viewportHeightPx = 1920f)
            }
            c.onGestureEnd(s)
            assertTrue(c.hasActiveInertia)
            val dt = 1.0 / hz
            repeat(hz * 2) {
                s = c.tickInertia(s, dt)
            }
            return s.azimuthDeg
        }

        val az60 = simulateInertia(60)
        val az120 = simulateInertia(120)
        assertEquals(az60, az120, 0.05)

        // Holding finger stationary (>55ms) before lifting cancels fling velocity.
        val cDwell = AntarGestureController()
        var s = cDwell.onGestureStart(base)
        s = cDwell.onSingleFingerDrag(s, dxPx = 25f, dyPx = 0f, dtSec = 1.0 / 60.0, viewportWidthPx = 1080f, viewportHeightPx = 1920f)
        s = cDwell.onSingleFingerDrag(s, dxPx = 0f, dyPx = 0f, dtSec = 0.070, viewportWidthPx = 1080f, viewportHeightPx = 1920f)
        cDwell.onGestureEnd(s)
        assertFalse(cDwell.hasActiveInertia)
    }

    @Test
    fun twoFingerPinchPanAndClockwiseTwistMatchNaturalHandMotion() {
        val controller = AntarGestureController()
        var s = AntarSimulationSettings.DEFAULT.copy(distanceM = 20.0, rollDeg = 0.0)
        controller.onGestureStart(s)
        controller.onPointerCountChanged(2)

        // Spread fingers (zoomChange = 1.25 > 1) -> closer orbital radius (magnified black hole),
        // pan right (+30dp) & down (+15dp), clockwise twist (+10 deg beyond 4 deg deadzone -> negative rollDeg).
        s = controller.onTwoFingerTransform(
            settings = s,
            zoomChange = 1.25f,
            panDxDp = 30f,
            panDyDp = 15f,
            rotationDeg = 10f,
            viewportWidthDp = 412f,
            viewportHeightDp = 892f
        )
        assertEquals(16.0, s.distanceM, 1e-6)
        assertEquals(30.0, controller.panOffsetXDp, 1e-6)
        assertEquals(15.0, controller.panOffsetYDp, 1e-6)
        assertEquals(-6.0, s.rollDeg, 1e-6)

        // Lifting one finger after pinch must NOT snap azimuth via residual 1-finger drag.
        controller.onPointerCountChanged(1)
        val beforeAz = s.azimuthDeg
        s = controller.onSingleFingerDrag(s, dxPx = 50f, dyPx = 30f, dtSec = 0.016, viewportWidthPx = 1080f, viewportHeightPx = 2200f)
        assertEquals(beforeAz, s.azimuthDeg, 1e-9)

        // Double-tap clears pan & levels horizon roll first while preserving orbital distance.
        val recentered = controller.onDoubleTap(s)
        assertEquals(0.0, controller.panOffsetXDp, 1e-9)
        assertEquals(0.0, controller.panOffsetYDp, 1e-9)
        assertEquals(0.0, recentered.rollDeg, 1e-9)
        assertEquals(16.0, recentered.distanceM, 1e-9)
    }

    @Test
    fun systemBackEdgeTouchesAreIgnored() {
        val controller = AntarGestureController()
        assertTrue(controller.shouldIgnoreEdgeTouch(startXPx = 12f, viewportWidthPx = 1080f, edgeGuardPx = 54f))
        assertTrue(controller.shouldIgnoreEdgeTouch(startXPx = 1060f, viewportWidthPx = 1080f, edgeGuardPx = 54f))
        assertFalse(controller.shouldIgnoreEdgeTouch(startXPx = 540f, viewportWidthPx = 1080f, edgeGuardPx = 54f))
        assertTrue(abs(AntarGestureController.wrapAngleDeg(540.0)) <= 180.0)
    }
}
