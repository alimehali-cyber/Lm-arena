package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.GravitySandboxEngine
import com.alijafari.red.astronomy.astro_engine.PresetScenario
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class GravitySandboxEngineTest {

    @Test
    fun testPresetInitialization() {
        for (scenario in PresetScenario.entries) {
            val bodies = GravitySandboxEngine.getPresetBodies(scenario)
            if (scenario == PresetScenario.EMPTY_CANVAS) {
                assertTrue("Empty canvas should have 0 bodies", bodies.isEmpty())
            } else {
                assertTrue("Preset scenario ${scenario.name} should have bodies", bodies.isNotEmpty())
            }
        }
    }

    @Test
    fun testPhysicsLessonsCatalog() {
        val lessons = GravitySandboxEngine.physicsLessons
        assertTrue("Physics lessons catalog should contain educational guided experiments", lessons.isNotEmpty())
        for (lesson in lessons) {
            assertNotNull(lesson.id)
            assertTrue(lesson.titleEn.isNotEmpty())
            assertTrue(lesson.titleFa.isNotEmpty())
            assertTrue(lesson.formulaSymbol.isNotEmpty())
            assertNotNull(lesson.presetScenario)
            assertTrue(lesson.experimentStepsEn.isNotEmpty())
            assertTrue(lesson.experimentStepsFa.isNotEmpty())
        }
    }

    @Test
    fun testEnergyConservationInTwoBodyOrbit() {
        val bodies = GravitySandboxEngine.getPresetBodies(PresetScenario.SOLAR_SYSTEM).toMutableList()
        val initialDiag = GravitySandboxEngine.calculateDiagnostics(bodies)
        val initialTotalEnergy = initialDiag.totalEnergy

        // Step simulation 200 substeps
        val dt = 3600.0 * 6.0 // 6 hours per step
        for (step in 0 until 200) {
            GravitySandboxEngine.stepSimulation(
                bodies = bodies,
                dt = dt,
                enableCollisions = false,
                substepCount = 10
            )
        }

        val finalDiag = GravitySandboxEngine.calculateDiagnostics(bodies)
        val finalTotalEnergy = finalDiag.totalEnergy

        // Velocity Verlet integration conserves total energy within small numerical tolerance (< 1.5%)
        val relativeEnergyError = abs((finalTotalEnergy - initialTotalEnergy) / initialTotalEnergy)
        assertTrue(
            "Total energy relative error should be < 1.5% ($relativeEnergyError)",
            relativeEnergyError < 0.015
        )
    }

    @Test
    fun testInelasticCollisionMomentumConservation() {
        val bodies = GravitySandboxEngine.getPresetBodies(PresetScenario.SOLAR_SYSTEM).toMutableList()

        // Create two colliding bodies moving towards each other
        val m1 = 1.0e24
        val m2 = 2.0e24
        val v1 = 1000.0
        val v2 = -500.0

        val b1 = com.alijafari.red.astronomy.astro_engine.CelestialBody(
            id = "c1",
            nameEn = "Body 1",
            nameFa = "جرم ۱",
            mass = m1,
            radius = 1.0e6,
            posX = 0.0,
            posY = 0.0,
            velX = v1,
            velY = 0.0,
            colorHex = 0xFFFF0000,
            bodyType = com.alijafari.red.astronomy.astro_engine.BodyType.TERRESTRIAL_PLANET
        )

        val b2 = com.alijafari.red.astronomy.astro_engine.CelestialBody(
            id = "c2",
            nameEn = "Body 2",
            nameFa = "جرم ۲",
            mass = m2,
            radius = 1.0e6,
            posX = 1.0e6, // Overlapping distance < r1 + r2
            posY = 0.0,
            velX = v2,
            velY = 0.0,
            colorHex = 0xFF00FF00,
            bodyType = com.alijafari.red.astronomy.astro_engine.BodyType.TERRESTRIAL_PLANET
        )

        val testBodies = mutableListOf(b1, b2)
        val initialMomentumX = m1 * v1 + m2 * v2

        GravitySandboxEngine.handleInelasticCollisions(testBodies)

        assertEquals("After collision, 2 bodies should merge into 1", 1, testBodies.size)
        val merged = testBodies.first()
        val finalMomentumX = merged.mass * merged.velX

        assertEquals("Total mass must equal m1 + m2", m1 + m2, merged.mass, 1e-6)
        assertEquals("Linear momentum X must be strictly conserved", initialMomentumX, finalMomentumX, 1e-4)
    }

    @Test
    fun testFigureEightChoreographyStability() {
        val bodies = GravitySandboxEngine.getPresetBodies(PresetScenario.FIGURE_EIGHT).toMutableList()
        assertEquals(3, bodies.size)

        val dt = 3600.0 * 2.0 // 2 hours
        for (step in 0 until 50) {
            GravitySandboxEngine.stepSimulation(
                bodies = bodies,
                dt = dt,
                enableCollisions = false,
                substepCount = 10
            )
        }

        assertEquals("All 3 bodies in Figure-8 should remain intact", 3, bodies.size)
    }
}
