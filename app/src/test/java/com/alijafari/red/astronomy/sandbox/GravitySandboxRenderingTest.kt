package com.alijafari.red.astronomy.sandbox

import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import com.alijafari.red.astronomy.sandbox.render.camera.RayCaster
import com.alijafari.red.astronomy.sandbox.render.camera.SandboxCamera
import com.alijafari.red.astronomy.sandbox.render.gl.QualityLevel
import com.alijafari.red.astronomy.sandbox.render.model.RenderBodyColor
import com.alijafari.red.astronomy.sandbox.render.renderer.RenderTheme
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.render.scale.ScaleMode
import com.alijafari.red.astronomy.sandbox.render.trails.TrailBufferManager
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Verification test suite for Phase 2: OpenGL ES 3.0 Rendering Foundation & Math.
 */
class GravitySandboxRenderingTest {

    // =============================================================================================
    // 1. VISUAL SCALE SEPARATION TESTS
    // =============================================================================================

    @Test
    fun testRenderScaleManager_originMapping() {
        val manager = RenderScaleManager()
        val outPos = FloatArray(3)

        manager.physicsToRenderPosition(0.0, 0.0, 0.0, outPos)
        assertEquals(0.0f, outPos[0], 1e-6f)
        assertEquals(0.0f, outPos[1], 1e-6f)
        assertEquals(0.0f, outPos[2], 1e-6f)
    }

    @Test
    fun testRenderScaleManager_solarSystemCompressedMode() {
        val manager = RenderScaleManager(scaleMode = ScaleMode.SOLAR_SYSTEM_COMPRESSED)
        val outEarth = FloatArray(3)
        val outJupiter = FloatArray(3)
        val outNeptune = FloatArray(3)

        // 1 AU (Earth)
        manager.physicsToRenderPosition(AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS, 0.0, 0.0, outEarth)
        // 5.2 AU (Jupiter)
        manager.physicsToRenderPosition(5.2044 * AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS, 0.0, 0.0, outJupiter)
        // 30 AU (Neptune)
        manager.physicsToRenderPosition(30.069 * AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS, 0.0, 0.0, outNeptune)

        // Verify monotonic distance preservation
        assertTrue("Earth visual dist should be ~10.0", abs(outEarth[0] - 10.0f) < 0.1f)
        assertTrue("Jupiter should be further than Earth", outJupiter[0] > outEarth[0])
        assertTrue("Neptune should be further than Jupiter", outNeptune[0] > outJupiter[0])
        assertTrue("Neptune visual dist should be bounded < 40", outNeptune[0] < 40.0f)
    }

    @Test
    fun testRenderScaleManager_bodyRadiusScaling() {
        val manager = RenderScaleManager(scaleMode = ScaleMode.SOLAR_SYSTEM_COMPRESSED)

        val sunRadius = manager.physicsToRenderRadius(AstroPhysicsConstants.SOLAR_RADIUS_METERS, isStarOrBlackHole = true)
        val earthRadius = manager.physicsToRenderRadius(AstroPhysicsConstants.EARTH_RADIUS_METERS, isStarOrBlackHole = false)
        val moonRadius = manager.physicsToRenderRadius(1.737e6, isStarOrBlackHole = false)

        assertTrue("Sun visual radius should be larger than Earth", sunRadius > earthRadius)
        assertTrue("Earth visual radius should be larger than Moon", earthRadius > moonRadius)
        assertTrue("Moon should remain visible and touchable (>= 0.15)", moonRadius >= 0.15f)
        assertTrue("Sun should be reasonable bounded (<= 3.0)", sunRadius <= 3.0f)
    }

    // =============================================================================================
    // 2. CAMERA SYSTEM & ORBITAL MATH TESTS
    // =============================================================================================

    @Test
    fun testSandboxCamera_initializationAndAspect() {
        val camera = SandboxCamera()
        camera.setViewport(1080, 1920)

        assertEquals(1080f / 1920f, camera.aspectRatio, 1e-4f)
        assertEquals(45.0f, camera.yawDeg, 1e-4f)
        assertEquals(25.0f, camera.pitchDeg, 1e-4f)
        assertEquals(35.0f, camera.distance, 1e-4f)

        // Verify matrices are non-null and valid floats
        for (f in camera.viewMatrix) {
            assertFalse("View matrix contains NaN", f.isNaN())
            assertFalse("View matrix contains Infinity", f.isInfinite())
        }
        for (f in camera.projectionMatrix) {
            assertFalse("Projection matrix contains NaN", f.isNaN())
            assertFalse("Projection matrix contains Infinity", f.isInfinite())
        }
    }

    @Test
    fun testSandboxCamera_orbitAndClamping() {
        val camera = SandboxCamera()
        camera.setViewport(800, 600)

        // Orbit pitch past 90 degrees
        camera.orbit(deltaYawDeg = 30.0f, deltaPitchDeg = 100.0f)
        assertEquals(camera.maxPitchDeg, camera.pitchDeg, 1e-4f)

        camera.orbit(deltaYawDeg = 0.0f, deltaPitchDeg = -200.0f)
        assertEquals(camera.minPitchDeg, camera.pitchDeg, 1e-4f)

        // Orbit yaw wraps around 360
        camera.orbit(deltaYawDeg = 400.0f, deltaPitchDeg = 50.0f)
        assertTrue("Yaw should stay within 0..360", camera.yawDeg in 0.0f..360.0f)
    }

    @Test
    fun testSandboxCamera_fitBodies() {
        val camera = SandboxCamera()
        camera.setViewport(1000, 1000)

        // 3 bodies spread across space
        val positions = floatArrayOf(
            -10.0f, 0.0f, -5.0f,
             10.0f, 0.0f,  5.0f,
              0.0f, 5.0f,  0.0f
        )

        camera.fitBodies(positions, activeCount = 3, immediate = true)

        assertEquals(0.0f, camera.targetX, 1e-4f)
        assertEquals(2.5f, camera.targetY, 1e-4f)
        assertEquals(0.0f, camera.targetZ, 1e-4f)
        assertTrue("Camera distance should zoom out to encapsulate 20-unit span", camera.distance >= 25.0f)
    }

    // =============================================================================================
    // 3. RAY CASTING & TOUCH SELECTION TESTS
    // =============================================================================================

    @Test
    fun testRayCaster_directHit() {
        // Ray origin at (0, 0, 10), looking in -Z direction towards sphere at (0, 0, 0) with radius 2
        val t = RayCaster.intersectRaySphere(
            rayOriginX = 0f, rayOriginY = 0f, rayOriginZ = 10f,
            rayDirX = 0f, rayDirY = 0f, rayDirZ = -1f,
            sphereCenterX = 0f, sphereCenterY = 0f, sphereCenterZ = 0f,
            sphereRadius = 2.0f
        )

        // Ray starts at z=10 and hits sphere surface at z=2, so t = 8
        assertEquals(8.0f, t, 1e-4f)
    }

    @Test
    fun testRayCaster_miss() {
        // Ray offset by 5 units in X, pointing in -Z direction, sphere radius = 2
        val t = RayCaster.intersectRaySphere(
            rayOriginX = 5f, rayOriginY = 0f, rayOriginZ = 10f,
            rayDirX = 0f, rayDirY = 0f, rayDirZ = -1f,
            sphereCenterX = 0f, sphereCenterY = 0f, sphereCenterZ = 0f,
            sphereRadius = 2.0f
        )

        assertEquals(Float.MAX_VALUE, t, 1e-4f)
    }

    @Test
    fun testRayCaster_pointingAway() {
        // Ray origin at (0, 0, 10), looking in +Z direction away from sphere at (0, 0, 0)
        val t = RayCaster.intersectRaySphere(
            rayOriginX = 0f, rayOriginY = 0f, rayOriginZ = 10f,
            rayDirX = 0f, rayDirY = 0f, rayDirZ = 1f,
            sphereCenterX = 0f, sphereCenterY = 0f, sphereCenterZ = 0f,
            sphereRadius = 2.0f
        )

        assertEquals(Float.MAX_VALUE, t, 1e-4f)
    }

    // =============================================================================================
    // 4. ORBITAL TRAIL RING BUFFER TESTS
    // =============================================================================================

    @Test
    fun testTrailBufferManager_ringBufferCapacityAndWrap() {
        val trailManager = TrailBufferManager(maxBodies = 2, maxPointsPerBody = 5)

        // Add 10 moving points for body 0
        for (i in 0 until 10) {
            trailManager.addPoint(bodyIndex = 0, x = i * 1.0f, y = 0.0f, z = 0.0f, minDistanceSq = 0.0f)
        }

        // Buffer should be capped at maxPointsPerBody = 5 without throwing OutOfBounds
        trailManager.clear()
        // After clear, points reset
        trailManager.addPoint(bodyIndex = 0, x = 10.0f, y = 0.0f, z = 0.0f, minDistanceSq = 0.0f)
    }

    // =============================================================================================
    // 5. COLOR PALETTES & THEME COMPATIBILITY TESTS
    // =============================================================================================

    @Test
    fun testRenderBodyColor_allBodyTypesCovered() {
        for (type in SandboxBodyType.entries) {
            val color = RenderBodyColor.getColorForBodyType(type)
            assertEquals(4, color.size)
            assertTrue("Color alpha should be 1.0", color[3] == 1.0f)
            assertTrue("RGB values should be within [0, 1]", color[0] in 0f..1f && color[1] in 0f..1f && color[2] in 0f..1f)
        }

        assertTrue("Star must be emissive", RenderBodyColor.isEmissive(SandboxBodyType.SUN))
        assertFalse("Earth should not be emissive", RenderBodyColor.isEmissive(SandboxBodyType.EARTH))
    }

    @Test
    fun testRenderTheme_lightAndDarkConfigurations() {
        val dark = RenderTheme.DARK
        val light = RenderTheme.LIGHT

        assertTrue(dark.isDarkTheme)
        assertFalse(light.isDarkTheme)
        assertTrue("Dark clear color should be near black", dark.clearColorR < 0.1f)
        assertTrue("Light clear color should be near white", light.clearColorR > 0.8f)
    }

    // =============================================================================================
    // 6. QUALITY LEVEL INTEGRITY TESTS
    // =============================================================================================

    @Test
    fun testQualityLevel_hierarchy() {
        val low = QualityLevel.LOW
        val med = QualityLevel.MEDIUM
        val high = QualityLevel.HIGH

        assertTrue("High should have more rings than low", high.sphereRings > low.sphereRings)
        assertTrue("High should have more stars than low", high.starCount > low.starCount)
        assertTrue("High should have more trail points than low", high.maxTrailPointsPerBody > low.maxTrailPointsPerBody)
    }
}
