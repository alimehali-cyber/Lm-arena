package com.zig.museum.core.engine

import org.junit.Test
import org.junit.Assert.*

class CameraRigTest {

    @Test
    fun testDefaultState() {
        val rig = CameraRig()
        assertEquals(2.5f, rig.state.radius)
        assertEquals(0f, rig.state.yawDeg)
        assertEquals(0f, rig.state.pitchDeg)
    }

    @Test
    fun testZoomLimits() {
        val rig = CameraRig()
        rig.zoom(0.1f) // zoom in a lot
        assertTrue(rig.state.radius >= rig.minRadius)
        rig.zoom(10f) // zoom out a lot
        assertTrue(rig.state.radius <= rig.maxRadius)
    }

    @Test
    fun testOrbit() {
        val rig = CameraRig()
        rig.orbit(90f, 0f)
        assertEquals(90f, rig.state.yawDeg)
        rig.orbit(0f, 100f) // pitch clamped to 89
        assertTrue(rig.state.pitchDeg <= 89f)
    }

    @Test
    fun testNearFar() {
        val rig = CameraRig()
        val (near, far) = rig.computeNearFar()
        assertTrue(near >= 0.0005f && near <= 0.05f)
        assertEquals(8.0f, far)
    }

    @Test
    fun testReset() {
        val rig = CameraRig()
        rig.orbit(45f, 30f)
        rig.zoom(0.5f)
        rig.reset()
        assertEquals(2.5f, rig.state.radius)
        assertEquals(0f, rig.state.yawDeg)
    }
}
