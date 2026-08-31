package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.astro_engine.*
import org.junit.Assert.*
import org.junit.Test

class GravityTeachingEngineTest {

    @Test
    fun testTeachingObserverCooldownAndDeduplication() {
        val observer = GravityTeachingObserver()
        val bodies = GravitySandboxEngine.getPresetBodies(PresetScenario.SOLAR_SYSTEM).toMutableList()
        val diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)

        val t0 = 1000000L

        // Initial state observation should be quiet (no immediate spike)
        val moment1 = observer.observeSimulation(bodies, diagnostics, t0, isTeachingModeOn = true)
        assertNull("Initial state observation without state change should remain quiet", moment1)

        // Mutate Earth's mass dramatically (x2.0)
        val earth = bodies.first { it.id == "earth" }
        earth.mass *= 2.0

        val moment2 = observer.observeSimulation(bodies, diagnostics, t0 + 100, isTeachingModeOn = true)
        assertNotNull("Dramatic mass increase should trigger mass increase moment", moment2)
        assertEquals("mass_increase", moment2?.id)

        // Rapid subsequent tick (within 6 sec cooldown) should be suppressed to prevent spam
        earth.mass *= 2.0
        val momentSpam = observer.observeSimulation(bodies, diagnostics, t0 + 500, isTeachingModeOn = true)
        assertNull("Subsequent trigger within 6s cooldown must be suppressed to prevent spam", momentSpam)

        // After cooldown passes (e.g. 7000ms later), new state change can trigger
        earth.mass *= 0.2
        val moment3 = observer.observeSimulation(bodies, diagnostics, t0 + 7000, isTeachingModeOn = true)
        assertNotNull("Observation after cooldown window should trigger new moment", moment3)
    }

    @Test
    fun testDisabledTeachingModeRemainsSilent() {
        val observer = GravityTeachingObserver()
        val bodies = GravitySandboxEngine.getPresetBodies(PresetScenario.SOLAR_SYSTEM).toMutableList()
        val diagnostics = GravitySandboxEngine.calculateDiagnostics(bodies)

        // Simulate collision
        bodies.removeAt(bodies.size - 1)

        val moment = observer.observeSimulation(bodies, diagnostics, System.currentTimeMillis(), isTeachingModeOn = false)
        assertNull("When teaching mode is OFF, observer must remain completely silent", moment)
    }

    @Test
    fun testInteractiveExperimentsAndPredictionCatalog() {
        val experiments = GravityTeachingCatalog.experiments
        assertTrue("Interactive experiments catalog should not be empty", experiments.isNotEmpty())

        for (exp in experiments) {
            assertNotNull(exp.id)
            assertTrue(exp.titleEn.isNotEmpty())
            assertTrue(exp.titleFa.isNotEmpty())
            assertTrue(exp.questionEn.isNotEmpty())
            assertTrue(exp.questionFa.isNotEmpty())
            assertTrue(exp.predictionOptions.size >= 2)
            assertNotNull(exp.explanationMoment)

            // Check prediction options have a correct answer marked
            val hasCorrectOption = exp.predictionOptions.any { it.isCorrect }
            assertTrue("Experiment ${exp.id} should have a correct prediction choice", hasCorrectOption)
        }
    }

    @Test
    fun testTeachingMomentsThreeLevelsOfExplanation() {
        val moments = listOf(
            GravityTeachingCatalog.MOMENT_MASS_INCREASE,
            GravityTeachingCatalog.MOMENT_MASS_DECREASE,
            GravityTeachingCatalog.MOMENT_DISTANCE_CLOSE,
            GravityTeachingCatalog.MOMENT_COLLISION,
            GravityTeachingCatalog.MOMENT_ESCAPE,
            GravityTeachingCatalog.MOMENT_BLACK_HOLE,
            GravityTeachingCatalog.MOMENT_WORMHOLE
        )

        for (moment in moments) {
            assertTrue("Level 1 EN should be non-empty for ${moment.id}", moment.level1En.isNotEmpty())
            assertTrue("Level 1 FA should be non-empty for ${moment.id}", moment.level1Fa.isNotEmpty())
            assertTrue("Level 2 EN should be non-empty for ${moment.id}", moment.level2En.isNotEmpty())
            assertTrue("Level 2 FA should be non-empty for ${moment.id}", moment.level2Fa.isNotEmpty())
            assertTrue("Level 3 EN should be non-empty for ${moment.id}", moment.level3En.isNotEmpty())
            assertTrue("Level 3 FA should be non-empty for ${moment.id}", moment.level3Fa.isNotEmpty())
        }
    }
}
