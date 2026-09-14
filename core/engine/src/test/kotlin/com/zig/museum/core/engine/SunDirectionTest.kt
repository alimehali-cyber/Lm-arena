package com.zig.museum.core.engine

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class SunDirectionTest {

    @Test
    fun testCardinalDirections() {
        // North? Azimuth 0, elevation 0 => should be along +Z (prime meridian toward viewer)
        val north = sunDirection(0f, 0f)
        // Expect (0,0,1) normalized
        assertTrue(abs(north.x) < 0.01f)
        assertTrue(abs(north.y) < 0.01f)
        assertTrue(abs(north.z - 1f) < 0.01f)

        // East: azimuth 90, elevation 0 => +X
        val east = sunDirection(90f, 0f)
        assertTrue(abs(east.x - 1f) < 0.01f)
        assertTrue(abs(east.y) < 0.01f)
        assertTrue(abs(east.z) < 0.01f)

        // South: azimuth 180, elevation 0 => -Z
        val south = sunDirection(180f, 0f)
        assertTrue(abs(south.x) < 0.01f)
        assertTrue(abs(south.y) < 0.01f)
        assertTrue(abs(south.z + 1f) < 0.01f)

        // West: azimuth 270, elevation 0 => -X
        val west = sunDirection(270f, 0f)
        assertTrue(abs(west.x + 1f) < 0.01f)
        assertTrue(abs(west.y) < 0.01f)
        assertTrue(abs(west.z) < 0.01f)
    }

    @Test
    fun testPoles() {
        // Zenith: elevation 90, any azimuth => (0,1,0)
        val zenith = sunDirection(0f, 90f)
        assertTrue(abs(zenith.x) < 0.01f)
        assertTrue(abs(zenith.y - 1f) < 0.01f)
        assertTrue(abs(zenith.z) < 0.01f)

        val zenith2 = sunDirection(123f, 90f)
        assertTrue(abs(zenith2.x) < 0.01f)
        assertTrue(abs(zenith2.y - 1f) < 0.01f)
        assertTrue(abs(zenith2.z) < 0.01f)

        // Horizon at different azimuths should have y ~0
        val horizon = sunDirection(45f, 0f)
        assertTrue(abs(horizon.y) < 0.01f)
    }

    @Test
    fun testNormalization() {
        val dir = sunDirection(30f, 45f)
        val len = kotlin.math.sqrt((dir.x*dir.x + dir.y*dir.y + dir.z*dir.z).toDouble()).toFloat()
        assertTrue(abs(len - 1f) < 0.001f)
    }
}
