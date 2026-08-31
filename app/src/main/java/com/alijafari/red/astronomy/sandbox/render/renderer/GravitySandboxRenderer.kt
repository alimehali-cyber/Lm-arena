package com.alijafari.red.astronomy.sandbox.render.renderer

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.render.barycenter.BarycenterRenderer
import com.alijafari.red.astronomy.sandbox.render.camera.RayCaster
import com.alijafari.red.astronomy.sandbox.render.camera.SandboxCamera
import com.alijafari.red.astronomy.sandbox.render.celestial.CelestialBodyConfig
import com.alijafari.red.astronomy.sandbox.render.celestial.CelestialPropertiesRegistry
import com.alijafari.red.astronomy.sandbox.render.celestial.CelestialRotationManager
import com.alijafari.red.astronomy.sandbox.render.collision.CollisionVisualizer
import com.alijafari.red.astronomy.sandbox.render.diagnostics.SandboxRenderDiagnostics
import com.alijafari.red.astronomy.sandbox.render.geometry.*
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import com.alijafari.red.astronomy.sandbox.render.gl.QualityLevel
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.model.RenderBodyColor
import com.alijafari.red.astronomy.sandbox.render.prediction.TrajectoryPredictor
import com.alijafari.red.astronomy.sandbox.render.relativistic.RelativisticBodyRenderer
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.render.shaders.CelestialShaderSources
import com.alijafari.red.astronomy.sandbox.render.shaders.ShaderSources
import com.alijafari.red.astronomy.sandbox.render.shaders.TrajectoryShaderSources
import com.alijafari.red.astronomy.sandbox.render.trails.TrailBufferManager
import com.alijafari.red.astronomy.sandbox.render.vectors.VectorOverlayRenderer
import com.alijafari.red.astronomy.sandbox.snapshot.DoubleBufferSnapshotManager
import com.alijafari.red.astronomy.sandbox.snapshot.SandboxRenderFrame
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * Production-grade OpenGL ES 3.0 Renderer for the ZIG Gravity Sandbox (Phase 4).
 */
class GravitySandboxRenderer(
    private val snapshotManager: DoubleBufferSnapshotManager,
    var qualityLevel: QualityLevel = QualityLevel.HIGH
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "GravitySandboxRenderer"
        const val MAX_BODIES = 20

        fun getBodyTypeInt(type: SandboxBodyType): Int {
            return when (type) {
                SandboxBodyType.SUN -> 0
                SandboxBodyType.MERCURY -> 1
                SandboxBodyType.VENUS -> 2
                SandboxBodyType.EARTH -> 3
                SandboxBodyType.MOON -> 4
                SandboxBodyType.MARS -> 5
                SandboxBodyType.JUPITER -> 6
                SandboxBodyType.SATURN -> 7
                SandboxBodyType.URANUS -> 8
                SandboxBodyType.NEPTUNE -> 9
                else -> 10
            }
        }
    }

    // --- Systems ---
    val camera = SandboxCamera()
    val scaleManager = RenderScaleManager()
    val diagnostics = SandboxRenderDiagnostics()
    val rotationManager = CelestialRotationManager()
    var trailManager = TrailBufferManager(maxBodies = MAX_BODIES, maxPointsPerBody = qualityLevel.maxTrailPointsPerBody)
    val trajectoryPredictor = TrajectoryPredictor(maxBodies = MAX_BODIES)
    val barycenterRenderer = BarycenterRenderer()
    val vectorOverlayRenderer = VectorOverlayRenderer(maxBodies = MAX_BODIES)
    val collisionVisualizer = CollisionVisualizer()
    val relativisticRenderer = RelativisticBodyRenderer()

    // --- Configuration ---
    var theme: RenderTheme = RenderTheme.DARK
    var isGridVisible: Boolean = true
    var isStarfieldVisible: Boolean = true
    var selectedBodyId: String? = null
        set(value) {
            field = value
            trajectoryPredictor.selectedBodyId = value
            vectorOverlayRenderer.selectedBodyId = value
            camera.focusedBodyId = value
        }

    var onBodySelectedListener: ((String?) -> Unit)? = null

    // --- Shader Programs ---
    private var planetShader: ShaderProgram? = null
    private var cloudShader: ShaderProgram? = null
    private var atmosphereShader: ShaderProgram? = null
    private var ringShader: ShaderProgram? = null
    private var sunCoronaShader: ShaderProgram? = null
    private var starfieldShader: ShaderProgram? = null
    private var trailShader: ShaderProgram? = null
    private var predictionShader: ShaderProgram? = null
    private var vectorShader: ShaderProgram? = null
    private var barycenterShader: ShaderProgram? = null
    private var shockwaveShader: ShaderProgram? = null
    private var selectionRingShader: ShaderProgram? = null
    private var gridShader: ShaderProgram? = null
    private var fullscreenShader: ShaderProgram? = null

    // --- Geometry ---
    private var sphereMesh: SphereMesh? = null
    private var ringMesh: RingMesh? = null
    private var gridGeometry: GridGeometry? = null
    private var starfieldGeometry: StarfieldGeometry? = null
    private var fullscreenQuad: FullscreenQuad? = null

    // Selection ring geometry (equatorial circle)
    private var selectionRingVao: GlVertexArray? = null
    private var selectionRingVbo: GlBuffer? = null
    private val selectionRingSegments = 48

    // --- Scratch Matrices & Vectors (Pre-allocated for Zero GC) ---
    private val modelMatrix = FloatArray(16)
    private val normalMatrix4 = FloatArray(16)
    private val normalMatrix3 = FloatArray(9)
    private val visualOrientationMatrix = FloatArray(16)
    private val cloudOrientationMatrix = FloatArray(16)
    private val viewRotationMatrix = FloatArray(16)
    private val tempVec3 = FloatArray(3)
    private val bodyPositions = FloatArray(MAX_BODIES * 3)
    private val bodyRadii = FloatArray(MAX_BODIES)
    private val bodyColors = Array(MAX_BODIES) { FloatArray(4) }
    private val bodyConfigs = arrayOfNulls<CelestialBodyConfig>(MAX_BODIES)
    private val bodyIdList = ArrayList<String>(MAX_BODIES)

    // Raycast scratch buffers
    private val pickRayOrigin = FloatArray(3)
    private val pickRayDirection = FloatArray(3)

    // --- Frame Timing & Snapshot Buffering ---
    private var lastRenderTimeNs = System.nanoTime()
    private var lastRenderedFrame: SandboxRenderFrame? = null
    private var accumulatedSimTimeSec: Double = 0.0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "OpenGL ES 3.0 Surface Created. Initializing Phase 4 Visualization & Celestial Pipelines...")

        // Configure GL states
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Compile Advanced Celestial Shader Programs
        planetShader = ShaderProgram.build(
            CelestialShaderSources.PLANET_VERTEX_SHADER,
            CelestialShaderSources.PLANET_FRAGMENT_SHADER
        )
        cloudShader = ShaderProgram.build(
            CelestialShaderSources.CLOUD_VERTEX_SHADER,
            CelestialShaderSources.CLOUD_FRAGMENT_SHADER
        )
        atmosphereShader = ShaderProgram.build(
            CelestialShaderSources.ATMOSPHERE_VERTEX_SHADER,
            CelestialShaderSources.ATMOSPHERE_FRAGMENT_SHADER
        )
        ringShader = ShaderProgram.build(
            CelestialShaderSources.RING_VERTEX_SHADER,
            CelestialShaderSources.RING_FRAGMENT_SHADER
        )
        sunCoronaShader = ShaderProgram.build(
            CelestialShaderSources.SUN_CORONA_VERTEX_SHADER,
            CelestialShaderSources.SUN_CORONA_FRAGMENT_SHADER
        )
        starfieldShader = ShaderProgram.build(
            ShaderSources.STARFIELD_VERTEX_SHADER,
            ShaderSources.STARFIELD_FRAGMENT_SHADER
        )
        gridShader = ShaderProgram.build(
            ShaderSources.GRID_VERTEX_SHADER,
            ShaderSources.GRID_FRAGMENT_SHADER
        )
        fullscreenShader = ShaderProgram.build(
            ShaderSources.FULLSCREEN_QUAD_VERTEX_SHADER,
            CelestialShaderSources.TONE_MAPPING_FRAGMENT_SHADER
        )

        // Phase 4 Visualization Shaders
        trailShader = ShaderProgram.build(
            TrajectoryShaderSources.TRAIL_VERTEX_SHADER,
            TrajectoryShaderSources.TRAIL_FRAGMENT_SHADER
        )
        predictionShader = ShaderProgram.build(
            TrajectoryShaderSources.PREDICTION_VERTEX_SHADER,
            TrajectoryShaderSources.PREDICTION_FRAGMENT_SHADER
        )
        vectorShader = ShaderProgram.build(
            TrajectoryShaderSources.VECTOR_VERTEX_SHADER,
            TrajectoryShaderSources.VECTOR_FRAGMENT_SHADER
        )
        barycenterShader = ShaderProgram.build(
            TrajectoryShaderSources.BARYCENTER_VERTEX_SHADER,
            TrajectoryShaderSources.BARYCENTER_FRAGMENT_SHADER
        )
        shockwaveShader = ShaderProgram.build(
            TrajectoryShaderSources.SHOCKWAVE_VERTEX_SHADER,
            TrajectoryShaderSources.SHOCKWAVE_FRAGMENT_SHADER
        )
        selectionRingShader = ShaderProgram.build(
            TrajectoryShaderSources.SELECTION_RING_VERTEX_SHADER,
            TrajectoryShaderSources.SELECTION_RING_FRAGMENT_SHADER
        )

        // Initialize Geometry
        sphereMesh = SphereMesh(rings = qualityLevel.sphereRings, sectors = qualityLevel.sphereSectors).apply { init() }
        ringMesh = RingMesh().apply { init() }
        gridGeometry = GridGeometry().apply { init() }
        starfieldGeometry = StarfieldGeometry(starCount = qualityLevel.starCount).apply { init() }
        fullscreenQuad = FullscreenQuad().apply { init() }

        // Initialize Phase 4 Renderers
        trailManager.init()
        trajectoryPredictor.init()
        barycenterRenderer.init()
        vectorOverlayRenderer.init()
        collisionVisualizer.init()
        relativisticRenderer.qualityLevel = qualityLevel
        relativisticRenderer.init()

        // Build Selection Ring Geometry
        initSelectionRing()

        lastRenderTimeNs = System.nanoTime()
        Log.i(TAG, "Phase 4 Visualization Pipeline Initialized Successfully.")
    }

    private fun initSelectionRing() {
        selectionRingVao = GlVertexArray().apply { bind() }
        selectionRingVbo = GlBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_STATIC_DRAW)
        selectionRingVbo?.bind()

        val ringVerts = FloatArray(selectionRingSegments * 2 * 3)
        var idx = 0
        for (i in 0 until selectionRingSegments) {
            val theta1 = (i.toFloat() / selectionRingSegments) * 2.0f * Math.PI.toFloat()
            val theta2 = ((i + 1).toFloat() / selectionRingSegments) * 2.0f * Math.PI.toFloat()

            ringVerts[idx++] = cos(theta1); ringVerts[idx++] = 0.0f; ringVerts[idx++] = sin(theta1)
            ringVerts[idx++] = cos(theta2); ringVerts[idx++] = 0.0f; ringVerts[idx++] = sin(theta2)
        }

        selectionRingVbo?.uploadData(ringVerts)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0)

        selectionRingVao?.unbind()
        selectionRingVbo?.unbind()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        camera.setViewport(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val frameStartNs = diagnostics.onFrameStart()
        var drawCallCount = 0

        // Calculate delta time
        val nowNs = System.nanoTime()
        val dtSec = ((nowNs - lastRenderTimeNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)
        lastRenderTimeNs = nowNs

        // Fetch latest completed physics snapshot
        val currentSnapshot = snapshotManager.getLatestSnapshot()
        val activeFrame = currentSnapshot ?: lastRenderedFrame
        if (currentSnapshot != null) {
            lastRenderedFrame = currentSnapshot
            accumulatedSimTimeSec = currentSnapshot.simulationTimeSeconds
        } else {
            accumulatedSimTimeSec += dtSec.toDouble()
        }

        // Process body positions and visual scaling
        val bodies = activeFrame?.bodies ?: emptyList()
        val activeCount = bodies.size.coerceAtMost(MAX_BODIES)

        bodyIdList.clear()
        var primaryLightX = 0.0f
        var primaryLightY = 15.0f
        var primaryLightZ = 0.0f
        var hasStarLight = false

        var focusedBodyPosX = Float.NaN
        var focusedBodyPosY = Float.NaN
        var focusedBodyPosZ = Float.NaN
        var selectedBodyIndex = -1

        // Compute render positions and radii (with hierarchical satellite support)
        scaleManager.computeRenderPositions(bodies, bodyPositions, bodyRadii)

        for (i in 0 until activeCount) {
            val b = bodies[i]
            bodyIdList.add(b.id)
            val type = b.type

            val outIdx = i * 3
            val posX = bodyPositions[outIdx]
            val posY = bodyPositions[outIdx + 1]
            val posZ = bodyPositions[outIdx + 2]

            bodyColors[i] = RenderBodyColor.getColorForBodyType(type)
            bodyConfigs[i] = CelestialPropertiesRegistry.getConfig(type)

            // Track primary star light source
            if (type == SandboxBodyType.SUN && !hasStarLight) {
                primaryLightX = posX
                primaryLightY = posY
                primaryLightZ = posZ
                hasStarLight = true
            }

            // Check selected body
            if (selectedBodyId != null && selectedBodyId == b.id) {
                selectedBodyIndex = i
                focusedBodyPosX = posX
                focusedBodyPosY = posY
                focusedBodyPosZ = posZ
            }

            // Record trail history at visual rendered coordinates
            trailManager.addPoint(i, posX, posY, posZ, currentSimTimeSeconds = accumulatedSimTimeSec)
        }

        trailManager.selectedBodyIndex = selectedBodyIndex

        // Process recent collisions
        activeFrame?.recentCollisions?.let { collisions ->
            collisionVisualizer.processCollisionEvents(
                events = collisions,
                currentSimTimeSec = accumulatedSimTimeSec,
                scaleManager = scaleManager,
                trailManager = trailManager,
                bodyIds = bodyIdList
            )
        }

        // Fallback lighting if no active star exists
        if (!hasStarLight) {
            primaryLightX = camera.eyeX + camera.upX * 10.0f
            primaryLightY = camera.eyeY + 20.0f
            primaryLightZ = camera.eyeZ
        }

        // Camera follow focus
        if (!focusedBodyPosX.isNaN()) {
            camera.setTarget(focusedBodyPosX, focusedBodyPosY, focusedBodyPosZ, immediate = false)
        }

        // Update camera matrices
        camera.update(dtSec)

        // Clear Buffers
        GLES30.glClearColor(theme.clearColorR, theme.clearColorG, theme.clearColorB, theme.clearColorA)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // 1. Render Starfield Background (Depth write disabled)
        if (isStarfieldVisible && starfieldShader != null && starfieldGeometry != null) {
            GLES30.glDepthMask(false)
            starfieldShader?.use()

            System.arraycopy(camera.viewMatrix, 0, viewRotationMatrix, 0, 16)
            viewRotationMatrix[12] = 0.0f
            viewRotationMatrix[13] = 0.0f
            viewRotationMatrix[14] = 0.0f

            starfieldShader?.setUniformMatrix4fv("u_ViewRotationMatrix", viewRotationMatrix)
            starfieldShader?.setUniformMatrix4fv("u_ProjectionMatrix", camera.projectionMatrix)
            starfieldShader?.setUniform1f("u_PointSizeScale", 1.0f)
            starfieldShader?.setUniform1f("u_BackgroundAlpha", theme.starfieldAlpha)

            starfieldGeometry?.draw()
            drawCallCount++
            GLES30.glDepthMask(true)
        }

        // 2. Render Reference Grid (Y = 0 orbital plane)
        if (isGridVisible && gridShader != null && gridGeometry != null) {
            GLES30.glDepthMask(false)
            gridShader?.use()
            gridShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            gridShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
            gridGeometry?.draw()
            drawCallCount++
            GLES30.glDepthMask(true)
        }

        // 3. Render Historical Orbital Trails
        if (trailShader != null && activeCount > 0) {
            GLES30.glDepthMask(false)
            trailShader?.use()
            trailShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            trailManager.draw(trailShader!!, activeCount, bodyColors)
            drawCallCount += activeCount
            GLES30.glDepthMask(true)
        }

        // 4. Render Forward Trajectory Predictions (Dashed / Stippled)
        if (predictionShader != null && activeFrame != null && activeCount > 0) {
            GLES30.glDepthMask(false)
            predictionShader?.use()
            predictionShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            trajectoryPredictor.draw(predictionShader!!, activeFrame, scaleManager, bodyColors)
            drawCallCount++
            GLES30.glDepthMask(true)
        }

        // 5. Render Collision & Merger Shockwaves
        if (shockwaveShader != null) {
            shockwaveShader?.use()
            shockwaveShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            collisionVisualizer.draw(shockwaveShader!!, accumulatedSimTimeSec)
            drawCallCount++
        }

        // 6. Render Celestial Bodies (Surface, Oceans, Bands, Craters, Granulation)
        if (planetShader != null && sphereMesh != null && activeCount > 0) {
            planetShader?.use()
            planetShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            planetShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
            planetShader?.setUniform3f("u_LightPosition", primaryLightX, primaryLightY, primaryLightZ)
            planetShader?.setUniform1f("u_SimTime", accumulatedSimTimeSec.toFloat())
            planetShader?.setUniform1f("u_QualityTier", when (qualityLevel) {
                QualityLevel.LOW -> 0.0f
                QualityLevel.MEDIUM -> 1.0f
                QualityLevel.HIGH -> 2.0f
            })

            val normalLoc = planetShader?.getUniformLocation("u_NormalMatrix") ?: -1

            for (i in 0 until activeCount) {
                val b = bodies[i]
                val type = b.type
                if (type == SandboxBodyType.BLACK_HOLE || type == SandboxBodyType.THEORETICAL_WORMHOLE) {
                    continue // Handled by Relativistic Raymarching Pipeline
                }
                val idx = i * 3
                val x = bodyPositions[idx]
                val y = bodyPositions[idx + 1]
                val z = bodyPositions[idx + 2]
                val r = bodyRadii[i]
                val config = bodyConfigs[i] ?: CelestialPropertiesRegistry.getConfig(type)
                val color = bodyColors[i]

                rotationManager.computeOrientationMatrix(
                    bodyType = type,
                    simTimeSeconds = accumulatedSimTimeSec,
                    outMatrix = visualOrientationMatrix,
                    offset = 0,
                    isCloudLayer = false
                )

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, x, y, z)
                Matrix.scaleM(modelMatrix, 0, r, r, r)

                Matrix.invertM(normalMatrix4, 0, modelMatrix, 0)
                Matrix.transposeM(normalMatrix4, 0, normalMatrix4, 0)
                normalMatrix3[0] = normalMatrix4[0]
                normalMatrix3[1] = normalMatrix4[1]
                normalMatrix3[2] = normalMatrix4[2]
                normalMatrix3[3] = normalMatrix4[4]
                normalMatrix3[4] = normalMatrix4[5]
                normalMatrix3[5] = normalMatrix4[6]
                normalMatrix3[6] = normalMatrix4[8]
                normalMatrix3[7] = normalMatrix4[9]
                normalMatrix3[8] = normalMatrix4[10]

                planetShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)
                planetShader?.setUniformMatrix4fv("u_VisualOrientationMatrix", visualOrientationMatrix)
                if (normalLoc >= 0) {
                    GLES30.glUniformMatrix3fv(normalLoc, 1, false, normalMatrix3, 0)
                }

                planetShader?.setUniform1i("u_BodyType", getBodyTypeInt(type))
                planetShader?.setUniform4f("u_BaseColor", color[0], color[1], color[2], color[3])
                planetShader?.setUniform1f("u_SpecularIntensity", config.specularIntensity)
                planetShader?.setUniform1f("u_Shininess", config.shininess)
                planetShader?.setUniform1f("u_Roughness", config.roughness)
                planetShader?.setUniform1f("u_AmbientIntensity", if (theme.isDarkTheme) 0.08f else 0.20f)

                val hasRingShadow = config.hasRings && qualityLevel != QualityLevel.LOW
                planetShader?.setUniform1f("u_HasRingShadow", if (hasRingShadow) 1.0f else 0.0f)
                planetShader?.setUniform1f("u_RingInnerRadius", config.ringInnerRadiusFactor)
                planetShader?.setUniform1f("u_RingOuterRadius", config.ringOuterRadiusFactor)

                sphereMesh?.draw()
                drawCallCount++
            }
        }

        // 7. Render Saturn Rings System
        if (ringShader != null && ringMesh != null && qualityLevel != QualityLevel.LOW && activeCount > 0) {
            GLES30.glDisable(GLES30.GL_CULL_FACE)

            ringShader?.use()
            ringShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            ringShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
            ringShader?.setUniform3f("u_LightPosition", primaryLightX, primaryLightY, primaryLightZ)

            val ringNormalLoc = ringShader?.getUniformLocation("u_NormalMatrix") ?: -1

            for (i in 0 until activeCount) {
                val b = bodies[i]
                val config = bodyConfigs[i] ?: CelestialPropertiesRegistry.getConfig(b.type)
                if (!config.hasRings) continue

                val idx = i * 3
                val x = bodyPositions[idx]
                val y = bodyPositions[idx + 1]
                val z = bodyPositions[idx + 2]
                val r = bodyRadii[i]

                rotationManager.computeOrientationMatrix(
                    bodyType = b.type,
                    simTimeSeconds = accumulatedSimTimeSec,
                    outMatrix = visualOrientationMatrix,
                    offset = 0,
                    isCloudLayer = false
                )

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, x, y, z)
                Matrix.scaleM(modelMatrix, 0, r, r, r)

                Matrix.invertM(normalMatrix4, 0, modelMatrix, 0)
                Matrix.transposeM(normalMatrix4, 0, normalMatrix4, 0)
                normalMatrix3[0] = normalMatrix4[0]
                normalMatrix3[1] = normalMatrix4[1]
                normalMatrix3[2] = normalMatrix4[2]
                normalMatrix3[3] = normalMatrix4[4]
                normalMatrix3[4] = normalMatrix4[5]
                normalMatrix3[5] = normalMatrix4[6]
                normalMatrix3[6] = normalMatrix4[8]
                normalMatrix3[7] = normalMatrix4[9]
                normalMatrix3[8] = normalMatrix4[10]

                ringShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)
                ringShader?.setUniformMatrix4fv("u_VisualOrientationMatrix", visualOrientationMatrix)
                if (ringNormalLoc >= 0) {
                    GLES30.glUniformMatrix3fv(ringNormalLoc, 1, false, normalMatrix3, 0)
                }

                ringShader?.setUniform3f("u_PlanetCenter", x, y, z)
                ringShader?.setUniform1f("u_PlanetRadius", r)

                ringMesh?.draw()
                drawCallCount++
            }

            GLES30.glEnable(GLES30.GL_CULL_FACE)
        }

        // 8. Render Earth Clouds Layer
        if (cloudShader != null && sphereMesh != null && qualityLevel != QualityLevel.LOW && activeCount > 0) {
            cloudShader?.use()
            cloudShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            cloudShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
            cloudShader?.setUniform3f("u_LightPosition", primaryLightX, primaryLightY, primaryLightZ)
            cloudShader?.setUniform1f("u_SimTime", accumulatedSimTimeSec.toFloat())

            val cloudNormalLoc = cloudShader?.getUniformLocation("u_NormalMatrix") ?: -1

            for (i in 0 until activeCount) {
                val b = bodies[i]
                val config = bodyConfigs[i] ?: CelestialPropertiesRegistry.getConfig(b.type)
                if (!config.hasClouds) continue

                val idx = i * 3
                val x = bodyPositions[idx]
                val y = bodyPositions[idx + 1]
                val z = bodyPositions[idx + 2]
                val r = bodyRadii[i]

                rotationManager.computeOrientationMatrix(
                    bodyType = b.type,
                    simTimeSeconds = accumulatedSimTimeSec,
                    outMatrix = cloudOrientationMatrix,
                    offset = 0,
                    isCloudLayer = true
                )

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, x, y, z)
                Matrix.scaleM(modelMatrix, 0, r, r, r)

                Matrix.invertM(normalMatrix4, 0, modelMatrix, 0)
                Matrix.transposeM(normalMatrix4, 0, normalMatrix4, 0)
                normalMatrix3[0] = normalMatrix4[0]
                normalMatrix3[1] = normalMatrix4[1]
                normalMatrix3[2] = normalMatrix4[2]
                normalMatrix3[3] = normalMatrix4[4]
                normalMatrix3[4] = normalMatrix4[5]
                normalMatrix3[5] = normalMatrix4[6]
                normalMatrix3[6] = normalMatrix4[8]
                normalMatrix3[7] = normalMatrix4[9]
                normalMatrix3[8] = normalMatrix4[10]

                cloudShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)
                cloudShader?.setUniformMatrix4fv("u_VisualOrientationMatrix", cloudOrientationMatrix)
                if (cloudNormalLoc >= 0) {
                    GLES30.glUniformMatrix3fv(cloudNormalLoc, 1, false, normalMatrix3, 0)
                }

                cloudShader?.setUniform1f("u_CloudScale", config.cloudScale)

                sphereMesh?.draw()
                drawCallCount++
            }
        }

        // 9. Render Atmospheric Rayleigh/Mie Scattering Shells
        if (atmosphereShader != null && sphereMesh != null && qualityLevel != QualityLevel.LOW && activeCount > 0) {
            atmosphereShader?.use()
            atmosphereShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            atmosphereShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
            atmosphereShader?.setUniform3f("u_LightPosition", primaryLightX, primaryLightY, primaryLightZ)

            val atmNormalLoc = atmosphereShader?.getUniformLocation("u_NormalMatrix") ?: -1

            for (i in 0 until activeCount) {
                val b = bodies[i]
                val config = bodyConfigs[i] ?: CelestialPropertiesRegistry.getConfig(b.type)
                if (!config.hasAtmosphere) continue

                val idx = i * 3
                val x = bodyPositions[idx]
                val y = bodyPositions[idx + 1]
                val z = bodyPositions[idx + 2]
                val r = bodyRadii[i]

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, x, y, z)
                Matrix.scaleM(modelMatrix, 0, r, r, r)

                Matrix.invertM(normalMatrix4, 0, modelMatrix, 0)
                Matrix.transposeM(normalMatrix4, 0, normalMatrix4, 0)
                normalMatrix3[0] = normalMatrix4[0]
                normalMatrix3[1] = normalMatrix4[1]
                normalMatrix3[2] = normalMatrix4[2]
                normalMatrix3[3] = normalMatrix4[4]
                normalMatrix3[4] = normalMatrix4[5]
                normalMatrix3[5] = normalMatrix4[6]
                normalMatrix3[6] = normalMatrix4[8]
                normalMatrix3[7] = normalMatrix4[9]
                normalMatrix3[8] = normalMatrix4[10]

                atmosphereShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)
                if (atmNormalLoc >= 0) {
                    GLES30.glUniformMatrix3fv(atmNormalLoc, 1, false, normalMatrix3, 0)
                }

                atmosphereShader?.setUniform1f("u_AtmosphereScale", config.atmosphereScale)
                atmosphereShader?.setUniform4f(
                    "u_AtmosphereColor",
                    config.atmosphereColor[0],
                    config.atmosphereColor[1],
                    config.atmosphereColor[2],
                    config.atmosphereColor[3]
                )
                atmosphereShader?.setUniform1f("u_AtmosphereDensity", config.atmosphereDensity)

                sphereMesh?.draw()
                drawCallCount++
            }
        }

        // 10. Render Sun Corona Outer Halo
        if (sunCoronaShader != null && sphereMesh != null && qualityLevel != QualityLevel.LOW && activeCount > 0) {
            sunCoronaShader?.use()
            sunCoronaShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            sunCoronaShader?.setUniform3f("u_CameraPosition", camera.eyeX, camera.eyeY, camera.eyeZ)
            sunCoronaShader?.setUniform1f("u_SimTime", accumulatedSimTimeSec.toFloat())
            sunCoronaShader?.setUniform1f("u_CoronaScale", 1.25f)

            for (i in 0 until activeCount) {
                val b = bodies[i]
                if (b.type != SandboxBodyType.SUN) continue

                val idx = i * 3
                val x = bodyPositions[idx]
                val y = bodyPositions[idx + 1]
                val z = bodyPositions[idx + 2]
                val r = bodyRadii[i]

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, x, y, z)
                Matrix.scaleM(modelMatrix, 0, r, r, r)

                sunCoronaShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)

                sphereMesh?.draw()
                drawCallCount++
            }
        }

        // 10.5 Render Relativistic Black Holes & Theoretical Wormholes (Ray-marching Geodesics)
        relativisticRenderer.qualityLevel = qualityLevel
        drawCallCount += relativisticRenderer.draw(
            camera = camera,
            scaleManager = scaleManager,
            bodies = bodies,
            bodyPositions = bodyPositions,
            bodyRadii = bodyRadii,
            simTimeSeconds = accumulatedSimTimeSec
        )

        // 11. Render Selection Halo Ring (around selected body)
        if (selectionRingShader != null && selectionRingVao != null && selectedBodyIndex in 0 until activeCount) {
            val idx = selectedBodyIndex * 3
            val sx = bodyPositions[idx]
            val sy = bodyPositions[idx + 1]
            val sz = bodyPositions[idx + 2]
            val ringRadius = bodyRadii[selectedBodyIndex] * 1.55f

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, sx, sy, sz)
            Matrix.scaleM(modelMatrix, 0, ringRadius, ringRadius, ringRadius)

            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            selectionRingShader?.use()
            selectionRingShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            selectionRingShader?.setUniformMatrix4fv("u_ModelMatrix", modelMatrix)
            selectionRingShader?.setUniform4f("u_RingColor", 0.0f, 0.90f, 1.0f, 0.85f)

            selectionRingVao?.bind()
            GLES30.glLineWidth(2.5f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, selectionRingSegments * 2)
            selectionRingVao?.unbind()
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            drawCallCount++
        }

        // 12. Render Barycenter Marker Reticle (Center of Mass)
        if (barycenterShader != null && activeCount > 1) {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            barycenterShader?.use()
            barycenterShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            barycenterRenderer.draw(barycenterShader!!, bodies, scaleManager, camera.distance)
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            drawCallCount++
        }

        // 13. Render 3D Vector Overlays (Velocity & Gravitational Acceleration)
        if (vectorShader != null && activeCount > 0) {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            vectorShader?.use()
            vectorShader?.setUniformMatrix4fv("u_ViewProjectionMatrix", camera.viewProjectionMatrix)
            vectorOverlayRenderer.draw(
                shader = vectorShader!!,
                bodies = bodies,
                scaleManager = scaleManager,
                cameraDistance = camera.distance,
                bodyRenderPositions = bodyPositions,
                bodyRenderRadii = bodyRadii
            )
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            drawCallCount++
        }

        // Record Diagnostics
        diagnostics.onFrameEnd(
            frameStartNs = frameStartNs,
            drawCalls = drawCallCount,
            activeBodies = activeCount,
            snapshotSeq = activeFrame?.frameSequenceNumber ?: 0L,
            snapshotTimestampMs = activeFrame?.simulationTimeSeconds?.toLong() ?: 0L,
            cameraDistance = camera.distance,
            cameraTargetStr = "(%.1f, %.1f, %.1f)".format(camera.targetX, camera.targetY, camera.targetZ),
            scaleModeStr = scaleManager.scaleMode.name
        )
    }

    fun pickBodyAt(touchX: Float, touchY: Float): String? {
        camera.computePickRay(touchX, touchY, pickRayOrigin, pickRayDirection)
        val activeFrame = lastRenderedFrame ?: return null
        val bodies = activeFrame.bodies
        val activeCount = bodies.size.coerceAtMost(MAX_BODIES)

        var nearestDist = Float.MAX_VALUE
        var selectedId: String? = null

        for (i in 0 until activeCount) {
            val idx = i * 3
            val cx = bodyPositions[idx]
            val cy = bodyPositions[idx + 1]
            val cz = bodyPositions[idx + 2]
            val hitRadius = (bodyRadii[i] * 1.5f).coerceAtLeast(0.8f)

            val hitDist = RayCaster.intersectRaySphere(
                pickRayOrigin[0], pickRayOrigin[1], pickRayOrigin[2],
                pickRayDirection[0], pickRayDirection[1], pickRayDirection[2],
                cx, cy, cz, hitRadius
            )

            if (hitDist < nearestDist) {
                nearestDist = hitDist
                selectedId = bodies[i].id
            }
        }

        return selectedId
    }

    fun fitAllBodies() {
        val activeFrame = lastRenderedFrame ?: return
        camera.fitBodies(bodyPositions, activeFrame.bodies.size)
    }

    fun resetCamera() {
        camera.reset()
    }

    fun clearTrails() {
        trailManager.clear()
    }

    fun destroy() {
        planetShader?.destroy()
        cloudShader?.destroy()
        atmosphereShader?.destroy()
        ringShader?.destroy()
        sunCoronaShader?.destroy()
        starfieldShader?.destroy()
        trailShader?.destroy()
        predictionShader?.destroy()
        vectorShader?.destroy()
        barycenterShader?.destroy()
        shockwaveShader?.destroy()
        selectionRingShader?.destroy()
        gridShader?.destroy()
        fullscreenShader?.destroy()

        sphereMesh?.destroy()
        ringMesh?.destroy()
        gridGeometry?.destroy()
        starfieldGeometry?.destroy()
        fullscreenQuad?.destroy()

        selectionRingVao?.destroy()
        selectionRingVbo?.destroy()
        selectionRingVao = null
        selectionRingVbo = null

        trailManager.destroy()
        trajectoryPredictor.destroy()
        barycenterRenderer.destroy()
        vectorOverlayRenderer.destroy()
        collisionVisualizer.destroy()
        relativisticRenderer.destroy()
    }
}
