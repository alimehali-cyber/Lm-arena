package com.alijafari.red.astronomy.sandbox

import android.opengl.Matrix
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.render.celestial.CelestialPropertiesRegistry
import com.alijafari.red.astronomy.sandbox.render.celestial.CelestialRotationManager
import com.alijafari.red.astronomy.sandbox.render.geometry.RingMesh
import com.alijafari.red.astronomy.sandbox.render.gl.QualityLevel
import com.alijafari.red.astronomy.sandbox.render.renderer.GravitySandboxRenderer
import com.alijafari.red.astronomy.sandbox.render.shaders.CelestialShaderSources
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * Phase 3 Celestial Rendering Unit & Algorithm Test Suite.
 */
class CelestialRenderingTest {

    // =========================================================================
    // 1. CELESTIAL PROPERTIES REGISTRY TESTS
    // =========================================================================

    @Test
    fun testCelestialProperties_AllBodiesRegistered() {
        val requiredBodies = listOf(
            SandboxBodyType.SUN,
            SandboxBodyType.MERCURY,
            SandboxBodyType.VENUS,
            SandboxBodyType.EARTH,
            SandboxBodyType.MOON,
            SandboxBodyType.MARS,
            SandboxBodyType.JUPITER,
            SandboxBodyType.SATURN,
            SandboxBodyType.URANUS,
            SandboxBodyType.NEPTUNE
        )

        for (body in requiredBodies) {
            val config = CelestialPropertiesRegistry.getConfig(body)
            assertNotNull("Config should exist for body: ${body.nameEn}", config)
            assertEquals(body, config.bodyType)
        }
    }

    @Test
    fun testEarthSpecificProperties_LayeredStructure() {
        val earth = CelestialPropertiesRegistry.getConfig(SandboxBodyType.EARTH)
        assertTrue("Earth must have an atmosphere", earth.hasAtmosphere)
        assertTrue("Earth must have a dedicated cloud layer", earth.hasClouds)
        assertTrue("Earth ocean must have high specular glint", earth.specularIntensity > 0.5f)
        assertTrue("Earth clouds must rotate faster than surface", earth.cloudRotationMultiplier > 1.0f)
        assertEquals(23.44f, earth.axialTiltDegrees, 0.1f)
    }

    @Test
    fun testSaturnSpecificProperties_RingSystem() {
        val saturn = CelestialPropertiesRegistry.getConfig(SandboxBodyType.SATURN)
        assertTrue("Saturn must have ring system enabled", saturn.hasRings)
        assertTrue("Ring inner radius must be outside planet surface", saturn.ringInnerRadiusFactor > 1.0f)
        assertTrue("Ring outer radius must exceed inner radius", saturn.ringOuterRadiusFactor > saturn.ringInnerRadiusFactor)
        assertEquals(26.73f, saturn.axialTiltDegrees, 0.1f)
    }

    @Test
    fun testRockyPlanets_PhysicalCharacteristics() {
        val mercury = CelestialPropertiesRegistry.getConfig(SandboxBodyType.MERCURY)
        assertFalse("Mercury has no atmosphere", mercury.hasAtmosphere)
        assertFalse("Mercury has no clouds", mercury.hasClouds)
        assertTrue("Mercury has rough terrain", mercury.roughness > 0.8f)

        val venus = CelestialPropertiesRegistry.getConfig(SandboxBodyType.VENUS)
        assertTrue("Venus has thick atmosphere", venus.hasAtmosphere)
        assertTrue("Venus has retrograde rotation or 177 deg tilt", venus.axialTiltDegrees > 170.0f || venus.siderealRotationPeriodHours < 0.0f)

        val moon = CelestialPropertiesRegistry.getConfig(SandboxBodyType.MOON)
        assertFalse("Moon has no atmosphere", moon.hasAtmosphere)
        assertTrue("Moon has rough cratered surface", moon.roughness > 0.8f)

        val mars = CelestialPropertiesRegistry.getConfig(SandboxBodyType.MARS)
        assertTrue("Mars has thin atmosphere", mars.hasAtmosphere)
        assertEquals(25.19f, mars.axialTiltDegrees, 0.1f)
    }

    @Test
    fun testGasGiants_Characteristics() {
        val jupiter = CelestialPropertiesRegistry.getConfig(SandboxBodyType.JUPITER)
        assertTrue("Jupiter has atmosphere", jupiter.hasAtmosphere)
        assertTrue("Jupiter rotates rapidly (< 12 hours)", jupiter.siderealRotationPeriodHours in 8.0f..12.0f)

        val uranus = CelestialPropertiesRegistry.getConfig(SandboxBodyType.URANUS)
        assertTrue("Uranus has extreme sideways tilt (> 90 deg)", uranus.axialTiltDegrees > 90.0f)

        val neptune = CelestialPropertiesRegistry.getConfig(SandboxBodyType.NEPTUNE)
        assertTrue("Neptune has atmosphere", neptune.hasAtmosphere)
    }

    // =========================================================================
    // 2. CELESTIAL ROTATION MANAGER TESTS (Isolated Visual Layer)
    // =========================================================================

    @Test
    fun testCelestialRotation_EarthFullRotation() {
        val manager = CelestialRotationManager()
        val earthConfig = CelestialPropertiesRegistry.getConfig(SandboxBodyType.EARTH)
        val periodSec = earthConfig.siderealRotationPeriodHours * 3600.0

        val angle0 = manager.calculateSpinAngleDegrees(SandboxBodyType.EARTH, 0.0)
        val angleHalf = manager.calculateSpinAngleDegrees(SandboxBodyType.EARTH, periodSec * 0.5)
        val angleFull = manager.calculateSpinAngleDegrees(SandboxBodyType.EARTH, periodSec)

        assertEquals(0.0f, angle0, 1e-3f)
        assertEquals(180.0f, angleHalf, 0.5f)
        assertEquals(0.0f, angleFull, 0.5f)
    }

    @Test
    fun testCelestialRotation_CloudLayerDrift() {
        val manager = CelestialRotationManager()
        val timeSec = 3600.0 // 1 hour

        val surfaceAngle = manager.calculateSpinAngleDegrees(SandboxBodyType.EARTH, timeSec, isCloudLayer = false)
        val cloudAngle = manager.calculateSpinAngleDegrees(SandboxBodyType.EARTH, timeSec, isCloudLayer = true)

        assertTrue("Clouds must drift ahead of surface", cloudAngle > surfaceAngle)
    }

    @Test
    fun testCelestialRotation_OrientationMatrixComputation() {
        val manager = CelestialRotationManager()
        val matrix = FloatArray(16)

        manager.computeOrientationMatrix(
            bodyType = SandboxBodyType.EARTH,
            simTimeSeconds = 0.0,
            outMatrix = matrix,
            offset = 0
        )

        // Validate matrix is non-zero, orthonormal rotation
        var diagonalSum = matrix[0] + matrix[5] + matrix[10] + matrix[15]
        assertTrue("Trace of orientation matrix must be valid", diagonalSum in 1.0f..4.0f)
    }

    // =========================================================================
    // 3. RING MESH GEOMETRY CONFIGURATION
    // =========================================================================

    @Test
    fun testRingMesh_Parameters() {
        val ring = RingMesh(innerRadius = 1.25f, outerRadius = 2.35f, segments = 64)
        assertEquals(1.25f, ring.innerRadius, 1e-4f)
        assertEquals(2.35f, ring.outerRadius, 1e-4f)
        assertEquals(64, ring.segments)
    }

    // =========================================================================
    // 4. BODY TYPE INTEGER MAPPING
    // =========================================================================

    @Test
    fun testBodyTypeIntMapping() {
        assertEquals(0, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.SUN))
        assertEquals(1, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.MERCURY))
        assertEquals(2, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.VENUS))
        assertEquals(3, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.EARTH))
        assertEquals(4, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.MOON))
        assertEquals(5, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.MARS))
        assertEquals(6, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.JUPITER))
        assertEquals(7, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.SATURN))
        assertEquals(8, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.URANUS))
        assertEquals(9, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.NEPTUNE))
        assertEquals(10, GravitySandboxRenderer.getBodyTypeInt(SandboxBodyType.ASTEROID))
    }

    // =========================================================================
    // 5. SHADER SOURCES VALIDATION (GLSL ES 3.00)
    // =========================================================================

    @Test
    fun testCelestialShaders_HeadersAndKeywords() {
        val shaders = listOf(
            CelestialShaderSources.PLANET_VERTEX_SHADER,
            CelestialShaderSources.PLANET_FRAGMENT_SHADER,
            CelestialShaderSources.CLOUD_VERTEX_SHADER,
            CelestialShaderSources.CLOUD_FRAGMENT_SHADER,
            CelestialShaderSources.ATMOSPHERE_VERTEX_SHADER,
            CelestialShaderSources.ATMOSPHERE_FRAGMENT_SHADER,
            CelestialShaderSources.RING_VERTEX_SHADER,
            CelestialShaderSources.RING_FRAGMENT_SHADER,
            CelestialShaderSources.SUN_CORONA_VERTEX_SHADER,
            CelestialShaderSources.SUN_CORONA_FRAGMENT_SHADER,
            CelestialShaderSources.TONE_MAPPING_FRAGMENT_SHADER
        )

        for (src in shaders) {
            assertTrue("Shader must start with #version 300 es", src.trim().startsWith("#version 300 es"))
        }

        // Validate Planet Fragment Shader contains essential procedural algorithms
        assertTrue(CelestialShaderSources.PLANET_FRAGMENT_SHADER.contains("snoise"))
        assertTrue(CelestialShaderSources.PLANET_FRAGMENT_SHADER.contains("fbm"))
        assertTrue(CelestialShaderSources.PLANET_FRAGMENT_SHADER.contains("u_BodyType"))
        assertTrue(CelestialShaderSources.PLANET_FRAGMENT_SHADER.contains("u_SpecularIntensity"))
        assertTrue(CelestialShaderSources.PLANET_FRAGMENT_SHADER.contains("u_HasRingShadow"))

        // Validate Saturn Ring shader contains shadow & density profiles
        assertTrue(CelestialShaderSources.RING_FRAGMENT_SHADER.contains("u_PlanetRadius"))
        assertTrue(CelestialShaderSources.RING_FRAGMENT_SHADER.contains("u_PlanetCenter"))
        assertTrue(CelestialShaderSources.RING_FRAGMENT_SHADER.contains("density"))

        // Validate ACES Filmic tone mapping
        assertTrue(CelestialShaderSources.TONE_MAPPING_FRAGMENT_SHADER.contains("acesFilmic"))
    }
}
