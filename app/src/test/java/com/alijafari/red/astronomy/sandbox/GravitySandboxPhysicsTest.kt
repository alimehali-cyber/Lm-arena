package com.alijafari.red.astronomy.sandbox

import com.alijafari.red.astronomy.sandbox.model.*
import com.alijafari.red.astronomy.sandbox.physics.*
import com.alijafari.red.astronomy.sandbox.presets.SandboxPresetCatalog
import com.alijafari.red.astronomy.sandbox.snapshot.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Rigorous Numerical & Physical Validation Suite for the Gravity Sandbox Physics Engine.
 */
class GravitySandboxPhysicsTest {

    private val TOLERANCE_FRACTIONAL = 1e-4

    // ---------------------------------------------------------------------------------------------
    // Test 1: Two-Body Circular Orbit - Conservation of Radius and Velocity
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testTwoBodyCircularOrbitRadiusAndVelocity() {
        val engine = GravitySandboxEngine()
        val mSun = AstroPhysicsConstants.SOLAR_MASS_KG
        val r0 = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS
        val v0 = sqrt(AstroPhysicsConstants.G * mSun / r0) // Exact circular speed ~29784.8 m/s

        val sun = SandboxBody(
            id = "sun",
            type = SandboxBodyType.SUN,
            nameEn = "Sun",
            nameFa = "خورشید",
            massKg = mSun,
            radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
            position = Vector3D.ZERO,
            velocity = Vector3D.ZERO,
            isFixed = true
        )

        val planet = SandboxBody(
            id = "test_planet",
            type = SandboxBodyType.EARTH,
            nameEn = "Planet",
            nameFa = "سیاره",
            massKg = 1.0e24,
            radiusMeters = 6.0e6,
            position = Vector3D(r0, 0.0, 0.0),
            velocity = Vector3D(0.0, v0, 0.0)
        )

        engine.loadBodies(listOf(sun, planet))

        // Step through 1/4 of an orbit (approx 91.3 days = 7,889,400 seconds) in 3600s steps
        val dt = 3600.0
        val steps = (7889400 / dt).toInt()
        for (k in 0 until steps) {
            engine.stepSingle(dt)
        }

        val bodies = engine.getActiveBodies()
        val p = bodies.first { it.id == "test_planet" }

        val currentR = p.position.length()
        val currentV = p.velocity.length()

        // Radius should remain approximately r0
        val radiusErrorFraction = abs(currentR - r0) / r0
        val velocityErrorFraction = abs(currentV - v0) / v0

        assertTrue("Radius drift $radiusErrorFraction should be < 0.001", radiusErrorFraction < 1e-3)
        assertTrue("Velocity drift $velocityErrorFraction should be < 0.001", velocityErrorFraction < 1e-3)
    }

    // ---------------------------------------------------------------------------------------------
    // Test 2: Two-Body Elliptical Orbit - Vis-Viva Equation Verification
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testTwoBodyEllipticalOrbitVisViva() {
        val engine = GravitySandboxEngine()
        val mSun = AstroPhysicsConstants.SOLAR_MASS_KG
        val rPeri = 0.5 * AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS // Pericenter at 0.5 AU
        val rApo = 1.5 * AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS  // Apocenter at 1.5 AU
        val a = (rPeri + rApo) * 0.5 // Semi-major axis = 1.0 AU

        // Velocity at pericenter: v_p = sqrt(GM * (2/r_p - 1/a))
        val vPeri = sqrt(AstroPhysicsConstants.G * mSun * (2.0 / rPeri - 1.0 / a))

        val sun = SandboxBody(
            id = "sun",
            type = SandboxBodyType.SUN,
            nameEn = "Sun",
            nameFa = "خورشید",
            massKg = mSun,
            radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
            position = Vector3D.ZERO,
            velocity = Vector3D.ZERO,
            isFixed = true
        )

        val comet = SandboxBody(
            id = "comet",
            type = SandboxBodyType.CUSTOM_BODY,
            nameEn = "Comet",
            nameFa = "دنباله‌دار",
            massKg = 1.0e15,
            radiusMeters = 1.0e4,
            position = Vector3D(rPeri, 0.0, 0.0),
            velocity = Vector3D(0.0, vPeri, 0.0)
        )

        engine.loadBodies(listOf(sun, comet))

        // Step for 1000 steps of 600 seconds
        for (i in 0 until 1000) {
            engine.stepSingle(600.0)

            val currentComet = engine.getActiveBodies().first { it.id == "comet" }
            val r = currentComet.position.length()
            val v = currentComet.velocity.length()

            // Vis-Viva theoretical velocity: v_theo = sqrt(GM * (2/r - 1/a))
            val vTheo = sqrt(AstroPhysicsConstants.G * mSun * (2.0 / r - 1.0 / a))
            val error = abs(v - vTheo) / vTheo

            assertTrue("Vis-Viva speed deviation at step $i ($error) should be < 0.005", error < 5e-3)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Test 3: Earth–Sun Preset Orbit Period & Long-Term Energy Conservation
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testSunEarthOrbitEnergyConservation() {
        val engine = GravitySandboxEngine()
        val preset = SandboxPresetCatalog.SUN_EARTH
        engine.loadBodies(preset.bodyFactory())

        val diag0 = engine.getCurrentDiagnostics()
        val e0 = diag0.totalEnergyJoules
        assertTrue("Initial energy must be negative (bound orbit)", e0 < 0.0)

        // Simulate 1 full orbital year (365.25 days = 31,557,600 s) in 3600s steps (~8766 steps)
        val dt = 3600.0
        val steps = (31557600.0 / dt).toInt()

        for (k in 0 until steps) {
            val ok = engine.stepSingle(dt)
            assertTrue("Step $k must succeed", ok)
        }

        val diagFinal = engine.getCurrentDiagnostics()
        val eFinal = diagFinal.totalEnergyJoules
        val fractionalDrift = abs((eFinal - e0) / e0)

        assertTrue(
            "Symplectic Yoshida energy drift over 1 year ($fractionalDrift) must be < 1e-4",
            fractionalDrift < 1e-4
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Test 4: Earth–Moon Center of Mass Preservation
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testEarthMoonCenterOfMassPreservation() {
        val engine = GravitySandboxEngine()
        val preset = SandboxPresetCatalog.EARTH_MOON
        engine.loadBodies(preset.bodyFactory())

        val diag0 = engine.getCurrentDiagnostics()
        val cmPos0 = diag0.centerOfMassPosition
        val cmVel0 = diag0.centerOfMassVelocity

        // Step for 1 lunar month (27.3 days = 2,358,720 s) in 300s steps
        val dt = 300.0
        val steps = (2358720.0 / dt).toInt()

        for (i in 0 until steps) {
            engine.stepSingle(dt)
        }

        val diagFinal = engine.getCurrentDiagnostics()
        val cmPosFinal = diagFinal.centerOfMassPosition
        val cmVelFinal = diagFinal.centerOfMassVelocity

        val posDrift = (cmPosFinal - cmPos0).length()
        val velDrift = (cmVelFinal - cmVel0).length()

        assertTrue("Center of mass position drift ($posDrift m) must be < 1.0 meter", posDrift < 1.0)
        assertTrue("Center of mass velocity drift ($velDrift m/s) must be < 1e-6 m/s", velDrift < 1e-6)
    }

    // ---------------------------------------------------------------------------------------------
    // Test 5: Three-Body System (Figure-Eight Solution Stability)
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testThreeBodyFigureEightStability() {
        val engine = GravitySandboxEngine()
        val preset = SandboxPresetCatalog.FIGURE_EIGHT_THREE_BODY
        engine.loadBodies(preset.bodyFactory())

        val initialDiag = engine.getCurrentDiagnostics()

        // Step for 500 steps
        for (i in 0 until 500) {
            engine.stepSingle(1800.0)
        }

        val finalDiag = engine.getCurrentDiagnostics()
        val drift = abs(finalDiag.energyDriftFraction)

        assertTrue("Figure-8 energy drift ($drift) must be < 1e-3", drift < 1e-3)
        assertEquals(3, engine.getActiveBodies().size)
    }

    // ---------------------------------------------------------------------------------------------
    // Test 6 & 7: Conservation of Total Mass and Linear Momentum During Inelastic Collision
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testConservationOfMassAndLinearMomentumDuringCollision() {
        val engine = GravitySandboxEngine()

        val m1 = 1.0e24
        val m2 = 2.0e24
        val v1 = Vector3D(1000.0, 0.0, 0.0)
        val v2 = Vector3D(-500.0, 0.0, 0.0)

        val body1 = SandboxBody(
            id = "body_1",
            type = SandboxBodyType.CUSTOM_BODY,
            nameEn = "Body 1",
            nameFa = "جرم ۱",
            massKg = m1,
            radiusMeters = 1.0e6,
            position = Vector3D(-0.5e6, 0.0, 0.0),
            velocity = v1
        )

        val body2 = SandboxBody(
            id = "body_2",
            type = SandboxBodyType.CUSTOM_BODY,
            nameEn = "Body 2",
            nameFa = "جرم ۲",
            massKg = m2,
            radiusMeters = 1.0e6,
            position = Vector3D(0.5e6, 0.0, 0.0),
            velocity = v2
        )

        val expectedTotalMass = m1 + m2
        val expectedMomentum = (v1 * m1) + (v2 * m2)

        engine.loadBodies(listOf(body1, body2))

        // Direct contact (separation 1.0e6 <= sum of radii 2.0e6) triggers inelastic merger
        engine.stepSingle(0.01)

        val active = engine.getActiveBodies()
        assertEquals("Two colliding bodies should merge into 1", 1, active.size)

        val merged = active.first()
        assertEquals("Total mass must be conserved", expectedTotalMass, merged.massKg, 1e-5)

        val actualMomentum = merged.velocity * merged.massKg
        assertEquals("Px must be conserved", expectedMomentum.x, actualMomentum.x, 1e-3)
        assertEquals("Py must be conserved", expectedMomentum.y, actualMomentum.y, 1e-3)
        assertEquals("Pz must be conserved", expectedMomentum.z, actualMomentum.z, 1e-3)
    }

    // ---------------------------------------------------------------------------------------------
    // Test 8: Centre-of-Mass Velocity in Isolated Multi-Body System
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testIsolatedSystemZeroNetForceCenterOfMassVelocity() {
        val engine = GravitySandboxEngine()
        val preset = SandboxPresetCatalog.FULL_SOLAR_SYSTEM
        engine.loadBodies(preset.bodyFactory())

        val diag0 = engine.getCurrentDiagnostics()
        val vCm0 = diag0.centerOfMassVelocity

        // Advance by 100 steps
        for (i in 0 until 100) {
            engine.stepSingle(3600.0)
        }

        val diagFinal = engine.getCurrentDiagnostics()
        val vCmFinal = diagFinal.centerOfMassVelocity

        val diff = (vCmFinal - vCm0).length()
        assertTrue("Net internal gravitational forces cannot change V_cm: diff=$diff", diff < 1e-8)
    }

    // ---------------------------------------------------------------------------------------------
    // Test 9: Black Hole Schwarzschild Radius and Mass Accretion
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testBlackHoleSchwarzschildRadiusAndAccretion() {
        val mBh = 10.0 * AstroPhysicsConstants.SOLAR_MASS_KG
        val bh = SandboxBody(
            id = "bh",
            type = SandboxBodyType.BLACK_HOLE,
            nameEn = "Black Hole",
            nameFa = "سیاه‌چاله",
            massKg = mBh,
            radiusMeters = 2.953e4
        )

        // Analytical Rs = 2GM / c^2
        val expectedRs = (2.0 * AstroPhysicsConstants.G * mBh) / (AstroPhysicsConstants.SPEED_OF_LIGHT * AstroPhysicsConstants.SPEED_OF_LIGHT)
        assertEquals("Rs calculation must match 2GM/c^2", expectedRs, bh.schwarzschildRadiusMeters, 1.0)

        // Test Accretion
        val engine = GravitySandboxEngine()
        val victim = SandboxBody(
            id = "victim",
            type = SandboxBodyType.ASTEROID,
            nameEn = "Asteroid",
            nameFa = "سیارک",
            massKg = 1.0e22,
            radiusMeters = 1.0e5,
            position = Vector3D(5.0e4, 0.0, 0.0), // In contact range (50 km < 29.5 km + 100 km)
            velocity = Vector3D(5000.0, 0.0, 0.0)
        )

        engine.loadBodies(listOf(bh, victim))
        engine.stepSingle(0.0001)

        val active = engine.getActiveBodies()
        assertEquals("Victim body absorbed into Black Hole", 1, active.size)
        val accretedBh = active.first()
        assertEquals(mBh + 1.0e22, accretedBh.massKg, 1e-5)
    }

    // ---------------------------------------------------------------------------------------------
    // Test 10: Close Encounter Numerical Stability with Softening
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testCloseEncounterNumericalStability() {
        val engine = GravitySandboxEngine()
        // Two massive bodies crossing at close range with tiny distance
        val b1 = SandboxBody(
            id = "b1",
            type = SandboxBodyType.CUSTOM_BODY,
            nameEn = "Body 1",
            nameFa = "جرم ۱",
            massKg = 1.0e25,
            radiusMeters = 100.0, // Small radius so they don't immediately merge
            position = Vector3D(-500.0, 10.0, 0.0),
            velocity = Vector3D(50000.0, 0.0, 0.0)
        )

        val b2 = SandboxBody(
            id = "b2",
            type = SandboxBodyType.CUSTOM_BODY,
            nameEn = "Body 2",
            nameFa = "جرم ۲",
            massKg = 1.0e25,
            radiusMeters = 100.0,
            position = Vector3D(500.0, -10.0, 0.0),
            velocity = Vector3D(-50000.0, 0.0, 0.0)
        )

        engine.loadBodies(listOf(b1, b2))

        // Step through close encounter
        for (i in 0 until 50) {
            val ok = engine.stepSingle(0.01)
            assertTrue("Close encounter step must remain numerically stable", ok)
        }

        for (body in engine.getActiveBodies()) {
            assertTrue("Positions must remain finite", body.position.isFinite())
            assertTrue("Velocities must remain finite", body.velocity.isFinite())
            assertFalse("No NaNs permitted", body.position.hasNaN())
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Test 11: Absolute Determinism
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testSimulationDeterminism() {
        val engine1 = GravitySandboxEngine()
        val engine2 = GravitySandboxEngine()

        val preset = SandboxPresetCatalog.CHAOTIC_THREE_BODY

        engine1.loadBodies(preset.bodyFactory())
        engine2.loadBodies(preset.bodyFactory())

        val dt = 100.0
        for (i in 0 until 200) {
            engine1.stepSingle(dt)
            engine2.stepSingle(dt)
        }

        val bodies1 = engine1.getActiveBodies()
        val bodies2 = engine2.getActiveBodies()

        assertEquals(bodies1.size, bodies2.size)
        for (i in bodies1.indices) {
            assertEquals("PosX must be bit-identical", bodies1[i].position.x, bodies2[i].position.x, 0.0)
            assertEquals("PosY must be bit-identical", bodies1[i].position.y, bodies2[i].position.y, 0.0)
            assertEquals("PosZ must be bit-identical", bodies1[i].position.z, bodies2[i].position.z, 0.0)
            assertEquals("VelX must be bit-identical", bodies1[i].velocity.x, bodies2[i].velocity.x, 0.0)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Test 12: Fail-Safe Protection against NaNs and Infinities
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testFailSafeDetectsCorruptedState() {
        val engine = GravitySandboxEngine()
        val corruptBody = SandboxBody(
            id = "corrupted",
            type = SandboxBodyType.CUSTOM_BODY,
            nameEn = "Corrupted",
            nameFa = "خراب",
            massKg = Double.NaN,
            radiusMeters = 1000.0,
            position = Vector3D.ZERO,
            velocity = Vector3D.ZERO
        )

        engine.loadBodies(listOf(corruptBody))
        val stepSuccess = engine.stepSingle(1.0)

        assertFalse("Engine must reject corrupted state step", stepSuccess)
        assertTrue("Engine must report Error status", engine.engineStatus is EngineStatus.Error)
    }

    // ---------------------------------------------------------------------------------------------
    // Test 13: Double-Buffered Snapshot Decoupling
    // ---------------------------------------------------------------------------------------------
    @Test
    fun testSnapshotManagerProducesImmutableRenderFrames() {
        val snapshotManager = DoubleBufferSnapshotManager()
        val engine = GravitySandboxEngine()
        engine.loadBodies(SandboxPresetCatalog.SUN_EARTH.bodyFactory())

        val frame1 = snapshotManager.publishSnapshot(engine, 1000000L, 1)
        assertEquals(1L, frame1.frameSequenceNumber)
        assertEquals(2, frame1.bodies.size)

        engine.stepSingle(3600.0)
        val frame2 = snapshotManager.publishSnapshot(engine, 1200000L, 1)

        assertEquals(2L, frame2.frameSequenceNumber)
        assertTrue("Frame 2 time must exceed Frame 1", frame2.simulationTimeSeconds > frame1.simulationTimeSeconds)
        // Frame 1 must remain completely unaffected (immutable)
        assertEquals(0.0, frame1.simulationTimeSeconds, 0.0)
    }
}
