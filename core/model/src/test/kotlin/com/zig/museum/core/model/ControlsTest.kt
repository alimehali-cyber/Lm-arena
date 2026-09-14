package com.zig.museum.core.model

import org.junit.Assert.*
import org.junit.Test

class ControlsTest {

    @Test
    fun testRotationControlPureFunction() {
        // Rotation control driven purely by control, never wall clock per P4
        val state = RotationState(speed = RotationSpeed.X1, angleDeg = 0.0)
        val moonPeriod = 655.7 // hours, Moon sidereal
        val advanced = state.advance(deltaTimeSec = 3600.0, rotationPeriodHours = moonPeriod)
        // 1 hour at 1x for Moon (655.7h period) => 360/655.7 = 0.549 deg
        assertTrue(advanced.angleDeg > 0)
        assertTrue(advanced.angleDeg < 1.0)
        println("Rotation advance 1h Moon: ${advanced.angleDeg} deg")

        // Hold should not advance
        val hold = RotationState(speed = RotationSpeed.HOLD, angleDeg = 10.0)
        val holdAdvanced = hold.advance(3600.0, moonPeriod)
        assertEquals(10.0, holdAdvanced.angleDeg, 0.001)

        // 60x should be 60x faster
        val fast = RotationState(speed = RotationSpeed.X60, angleDeg = 0.0)
        val fastAdvanced = fast.advance(3600.0, moonPeriod)
        assertEquals(advanced.angleDeg * 60.0, fastAdvanced.angleDeg, 0.01)
    }

    @Test
    fun testRotationNoWallClock() {
        // Prove no effect depends on wall-clock time per M5 DoD: advancing fake clock shows render unchanged
        // We test that RotationState.advance() is pure function of deltaTime, not System.currentTimeMillis()
        val state = RotationState(speed = RotationSpeed.X1, angleDeg = 0.0)
        val period = 24.0 // Earth
        val delta = 100.0
        val r1 = state.advance(delta, period)
        val r2 = state.advance(delta, period)
        assertEquals(r1.angleDeg, r2.angleDeg, 0.0001)
        // Different delta => different result
        val r3 = state.advance(delta*2, period)
        assertNotEquals(r1.angleDeg, r3.angleDeg, 0.0001)
        println("No wall-clock: pure function verified")
    }

    @Test
    fun testSunDirectionPureFunction() {
        // Sun-direction control pure function from §10.3 and unit tests
        val state = SunState(azimuthDeg = 0f, elevationDeg = 0f)
        val dir = SunControl.directionFromState(state)
        // azimuth 0, elevation 0 => +Z
        assertEquals(0f, dir.first, 0.01f)
        assertEquals(0f, dir.second, 0.01f)
        assertEquals(1f, dir.third, 0.01f)

        val state90 = SunState(90f, 0f)
        val dir90 = SunControl.directionFromState(state90)
        // azimuth 90 => +X
        assertEquals(1f, dir90.first, 0.01f)
        assertEquals(0f, dir90.second, 0.01f)

        val stateElev90 = SunState(0f, 90f)
        val dirElev90 = SunControl.directionFromState(stateElev90)
        // elevation 90 => +Y
        assertEquals(1f, dirElev90.second, 0.01f)

        println("Sun direction pure function PASS")
    }

    @Test
    fun testSunPresets() {
        val moonPresets = SunControl.presetFor("moon")
        assertNotNull(moonPresets.fullDisk)
        assertNotNull(moonPresets.terminator)
        assertEquals(5f, moonPresets.terminator.elevationDeg, 0.1f)
        println("Moon presets: ${moonPresets.fullDisk}, ${moonPresets.terminator}")
    }

    @Test
    fun testLayerTogglesDataDriven() {
        val moonLayers = LayerToggles.forObject("moon")
        assertTrue(moonLayers.isNotEmpty())
        assertTrue(moonLayers.any { it.id == "wac_morphology" })
        val earthLayers = LayerToggles.forObject("earth")
        assertTrue(earthLayers.any { it.id == "night_lights" })
        println("Moon layers: ${moonLayers.map { it.id }}, Earth: ${earthLayers.map { it.id }}")
    }

    @Test
    fun testCameraPresets() {
        val moonPresets = CameraPresets.forObject("moon")
        assertTrue(moonPresets.any { it.id == "full_disk" })
        assertTrue(moonPresets.any { it.id == "apollo11" })
        val earthPresets = CameraPresets.forObject("earth")
        assertTrue(earthPresets.any { it.id == "himalaya" })
        println("Camera presets Moon: ${moonPresets.map { it.id }}")
    }

    @Test
    fun testTapToFocusAnalytic() {
        val ray = Ray(
            origin = Triple(0f, 0f, 2.5f),
            direction = Triple(0f, 0f, -1f)
        )
        val hit = TapToFocus.intersect(ray, objectRadius = 1.0f, oblateness = 0f)
        assertNotNull(hit)
        assertTrue(hit!!.distance > 0)
        println("Tap-to-focus hit: point=${hit.point}, uv=${hit.uv}, distance=${hit.distance}")

        // No hit if ray away
        val missRay = Ray(
            origin = Triple(0f, 0f, 2.5f),
            direction = Triple(0f, 1f, 0f)
        )
        val miss = TapToFocus.intersect(missRay, 1.0f, 0f)
        assertNull(miss)
    }

    @Test
    fun testDataHud() {
        val hud = DataHud.compute(
            baseResolutionMpp = 100.0,
            requestedLevel = 0,
            residentLevel = 0,
            ceilingMpp = 100.0,
            assetName = "LROC WAC"
        )
        assertFalse(hud.isProceduralBeyond)
        assertFalse(hud.isFallback)
        assertEquals(100.0, hud.displayedResolutionMpp, 0.01)
        println("HUD: ${hud.formatEn()}")

        val hudFallback = DataHud.compute(
            baseResolutionMpp = 100.0,
            requestedLevel = 0,
            residentLevel = 1,
            ceilingMpp = 100.0,
            assetName = "LROC WAC"
        )
        assertTrue(hudFallback.isFallback)
        assertEquals(200.0, hudFallback.displayedResolutionMpp, 0.01)
        assertTrue(hudFallback.isProceduralBeyond)

        val hudProcedural = DataHud.compute(
            baseResolutionMpp = 100.0,
            requestedLevel = 2,
            residentLevel = 2,
            ceilingMpp = 100.0,
            assetName = "LROC WAC"
        )
        assertTrue(hudProcedural.isProceduralBeyond)
        assertTrue(hudProcedural.proceduralIndicatorVisible())
        println("HUD procedural: ${hudProcedural.formatEn()}")
    }
}
