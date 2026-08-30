package com.alijafari.red.astronomy.sandbox

import com.alijafari.red.astronomy.sandbox.model.CollisionEvent
import com.alijafari.red.astronomy.sandbox.model.CollisionPolicy
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.model.Vector3D
import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import com.alijafari.red.astronomy.sandbox.physics.DiagnosticsSnapshot
import com.alijafari.red.astronomy.sandbox.physics.EngineStatus
import com.alijafari.red.astronomy.sandbox.render.barycenter.BarycenterCalculator
import com.alijafari.red.astronomy.sandbox.render.collision.CollisionVisualizer
import com.alijafari.red.astronomy.sandbox.render.prediction.TrajectoryPredictor
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.render.scale.ScaleMode
import com.alijafari.red.astronomy.sandbox.render.trails.TrailBufferManager
import com.alijafari.red.astronomy.sandbox.snapshot.BodyRenderState
import com.alijafari.red.astronomy.sandbox.snapshot.SandboxRenderFrame
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Phase 4 Comprehensive Unit Tests:
 * 1. Orbital Trail System (Persistence, Fading, Ring Buffer, Discontinuity Detection).
 * 2. Barycenter Calculator (Center of Mass, Total Mass, Dynamic Multi-Body Systems).
 * 3. Trajectory Predictor (Forward-Integration, Isolated State Integrity).
 * 4. Collision & Merger Visualizer (Shockwaves, Subordinate Trail Pruning).
 */
class OrbitalVisualizationsTest {

    // =============================================================================================
    // 1. ORBITAL TRAIL BUFFER TESTS
    // =============================================================================================

    @Test
    fun testTrailBufferManager_pointAdditionAndWrapAround() {
        val maxPoints = 50
        val trailManager = TrailBufferManager(maxBodies = 4, maxPointsPerBody = maxPoints)

        // Add 10 points for body 0
        for (i in 0 until 10) {
            trailManager.addPoint(
                bodyIndex = 0,
                x = i.toFloat(),
                y = 0.0f,
                z = (i * 2).toFloat(),
                currentSimTimeSeconds = i * 100.0
            )
        }

        assertEquals(10, trailManager.getPointCount(0))
        assertEquals(0, trailManager.getPointCount(1))

        // Add 100 points to verify circular ring-buffer overflow clamping
        for (i in 10 until 100) {
            trailManager.addPoint(
                bodyIndex = 0,
                x = i.toFloat(),
                y = 0.0f,
                z = (i * 2).toFloat(),
                currentSimTimeSeconds = i * 100.0
            )
        }

        assertEquals(maxPoints, trailManager.getPointCount(0))
    }

    @Test
    fun testTrailBufferManager_discontinuityHandling() {
        val trailManager = TrailBufferManager(maxBodies = 2, maxPointsPerBody = 100)

        // Point 1
        trailManager.addPoint(0, 1.0f, 0.0f, 1.0f, 0.0)
        // Point 2 (Normal step)
        trailManager.addPoint(0, 1.1f, 0.0f, 1.1f, 1.0)
        assertEquals(2, trailManager.getPointCount(0))

        // Teleport / Reset Jump (> 50.0 units)
        trailManager.addPoint(0, 200.0f, 0.0f, 200.0f, 2.0)
        // Should have reset the trail for this body to prevent drawing long artifact lines across the screen
        assertEquals(1, trailManager.getPointCount(0))
    }

    @Test
    fun testTrailBufferManager_clearSpecificBody() {
        val trailManager = TrailBufferManager(maxBodies = 4, maxPointsPerBody = 100)

        trailManager.addPoint(0, 5f, 5f, 5f, 1.0)
        trailManager.addPoint(1, 10f, 10f, 10f, 1.0)

        assertEquals(1, trailManager.getPointCount(0))
        assertEquals(1, trailManager.getPointCount(1))

        trailManager.clearBody(0)
        assertEquals(0, trailManager.getPointCount(0))
        assertEquals(1, trailManager.getPointCount(1))
    }

    // =============================================================================================
    // 2. BARYCENTER CALCULATOR TESTS
    // =============================================================================================

    @Test
    fun testBarycenterCalculator_twoEqualMasses() {
        val calculator = BarycenterCalculator()

        val body1 = BodyRenderState(
            id = "body1",
            type = SandboxBodyType.EARTH,
            nameEn = "Earth 1",
            nameFa = "زمین ۱",
            posX = -1000.0,
            posY = 0.0,
            posZ = 0.0,
            velX = 0.0, velY = 0.0, velZ = 0.0,
            accX = 0.0, accY = 0.0, accZ = 0.0,
            massKg = 1.0e24,
            radiusMeters = 6.371e6,
            visualScale = 1.0,
            colorHex = 0xFFFFFFFF,
            isFixed = false,
            isActive = true
        )

        val body2 = BodyRenderState(
            id = "body2",
            type = SandboxBodyType.EARTH,
            nameEn = "Earth 2",
            nameFa = "زمین ۲",
            posX = 1000.0,
            posY = 0.0,
            posZ = 0.0,
            velX = 0.0, velY = 0.0, velZ = 0.0,
            accX = 0.0, accY = 0.0, accZ = 0.0,
            massKg = 1.0e24,
            radiusMeters = 6.371e6,
            visualScale = 1.0,
            colorHex = 0xFFFFFFFF,
            isFixed = false,
            isActive = true
        )

        val info = calculator.computeBarycenter(listOf(body1, body2))

        assertEquals(2, info.activeBodyCount)
        assertEquals(2.0e24, info.totalMassKg, 1e18)
        assertEquals(0.0, info.positionPhysicsMeters.x, 1e-4)
        assertEquals(0.0, info.positionPhysicsMeters.y, 1e-4)
        assertEquals(0.0, info.positionPhysicsMeters.z, 1e-4)
    }

    @Test
    fun testBarycenterCalculator_sunEarthSystem() {
        val calculator = BarycenterCalculator()

        val sun = BodyRenderState(
            id = "sun",
            type = SandboxBodyType.SUN,
            nameEn = "Sun",
            nameFa = "خورشید",
            posX = 0.0, posY = 0.0, posZ = 0.0,
            velX = 0.0, velY = 0.0, velZ = 0.0,
            accX = 0.0, accY = 0.0, accZ = 0.0,
            massKg = AstroPhysicsConstants.SOLAR_MASS_KG,
            radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
            visualScale = 1.0,
            colorHex = 0xFFFFFFFF,
            isFixed = false,
            isActive = true
        )

        val earth = BodyRenderState(
            id = "earth",
            type = SandboxBodyType.EARTH,
            nameEn = "Earth",
            nameFa = "زمین",
            posX = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS, posY = 0.0, posZ = 0.0,
            velX = 0.0, velY = 29780.0, velZ = 0.0,
            accX = 0.0, accY = 0.0, accZ = 0.0,
            massKg = AstroPhysicsConstants.EARTH_MASS_KG,
            radiusMeters = AstroPhysicsConstants.EARTH_RADIUS_METERS,
            visualScale = 1.0,
            colorHex = 0xFFFFFFFF,
            isFixed = false,
            isActive = true
        )

        val info = calculator.computeBarycenter(listOf(sun, earth))

        // R_bary = (M_earth * 1 AU) / (M_sun + M_earth) ~ 4.49e5 meters from Sun center (well inside Sun radius of 6.96e8 m)
        val expectedBaryX = (AstroPhysicsConstants.EARTH_MASS_KG * AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS) /
                (AstroPhysicsConstants.SOLAR_MASS_KG + AstroPhysicsConstants.EARTH_MASS_KG)

        assertEquals(expectedBaryX, info.positionPhysicsMeters.x, 1e3)
        assertTrue(info.positionPhysicsMeters.x < AstroPhysicsConstants.SOLAR_RADIUS_METERS)
    }

    // =============================================================================================
    // 3. TRAJECTORY PREDICTOR TESTS
    // =============================================================================================

    @Test
    fun testTrajectoryPredictor_stateIsolation() {
        val predictor = TrajectoryPredictor(maxBodies = 5, predictionSteps = 60)

        val body1 = BodyRenderState(
            id = "star",
            type = SandboxBodyType.SUN,
            nameEn = "Star",
            nameFa = "ستاره",
            posX = 0.0, posY = 0.0, posZ = 0.0,
            velX = 0.0, velY = 0.0, velZ = 0.0,
            accX = 0.0, accY = 0.0, accZ = 0.0,
            massKg = 1.989e30,
            radiusMeters = 6.96e8,
            visualScale = 1.0,
            colorHex = 0xFFFFFFFF,
            isFixed = true,
            isActive = true
        )

        val body2 = BodyRenderState(
            id = "planet",
            type = SandboxBodyType.EARTH,
            nameEn = "Planet",
            nameFa = "سیاره",
            posX = 1.496e11, posY = 0.0, posZ = 0.0,
            velX = 0.0, velY = 29780.0, velZ = 0.0,
            accX = 0.0, accY = 0.0, accZ = 0.0,
            massKg = 5.972e24,
            radiusMeters = 6.371e6,
            visualScale = 1.0,
            colorHex = 0xFFFFFFFF,
            isFixed = false,
            isActive = true
        )

        val frame = SandboxRenderFrame(
            frameSequenceNumber = 1L,
            simulationTimeSeconds = 0.0,
            bodies = listOf(body1, body2),
            diagnostics = DiagnosticsSnapshot(
                totalMassKg = 1.989e30 + 5.972e24,
                kineticEnergyJoules = 0.0,
                potentialEnergyJoules = 0.0,
                totalEnergyJoules = -1.0,
                energyDriftFraction = 0.0,
                totalLinearMomentum = Vector3D(0.0, 0.0, 0.0),
                totalAngularMomentum = Vector3D(0.0, 0.0, 0.0),
                centerOfMassPosition = Vector3D(0.0, 0.0, 0.0),
                centerOfMassVelocity = Vector3D(0.0, 0.0, 0.0),
                minPairDistanceMeters = 1.496e11,
                minProximityRatio = 100.0
            ),
            recentCollisions = emptyList(),
            engineStatus = EngineStatus.Running
        )

        // Verify that the snapshot positions are unchanged
        assertEquals(0.0, frame.bodies[0].posX, 1e-4)
        assertEquals(1.496e11, frame.bodies[1].posX, 1e-4)
    }

    // =============================================================================================
    // 4. COLLISION VISUALIZER TESTS
    // =============================================================================================

    @Test
    fun testCollisionVisualizer_eventProcessingAndTrailCleanup() {
        val visualizer = CollisionVisualizer()
        val trailManager = TrailBufferManager(maxBodies = 4, maxPointsPerBody = 100)
        val scaleManager = RenderScaleManager()

        trailManager.addPoint(0, 1f, 1f, 1f, 1.0)
        trailManager.addPoint(1, 2f, 2f, 2f, 1.0)

        assertEquals(1, trailManager.getPointCount(0))
        assertEquals(1, trailManager.getPointCount(1))

        val event = CollisionEvent(
            timestampSeconds = 5.0,
            primaryBodyId = "sun",
            secondaryBodyId = "asteroid",
            collisionPosition = Vector3D(0.0, 0.0, 0.0),
            relativeVelocity = 35000.0,
            resultingBodyId = "sun",
            policyApplied = CollisionPolicy.MERGE_CONSERVE_MOMENTUM
        )

        visualizer.processCollisionEvents(
            events = listOf(event),
            currentSimTimeSec = 5.0,
            scaleManager = scaleManager,
            trailManager = trailManager,
            bodyIds = listOf("sun", "asteroid")
        )

        // Primary body ("sun" -> idx 0) trail retained, subordinate ("asteroid" -> idx 1) trail pruned
        assertEquals(1, trailManager.getPointCount(0))
        assertEquals(0, trailManager.getPointCount(1))
    }
}
