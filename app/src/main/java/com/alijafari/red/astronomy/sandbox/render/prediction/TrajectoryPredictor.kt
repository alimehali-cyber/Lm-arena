package com.alijafari.red.astronomy.sandbox.render.prediction

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import com.alijafari.red.astronomy.sandbox.physics.GravitationalForceSolver
import com.alijafari.red.astronomy.sandbox.physics.PhysicsStateBuffer
import com.alijafari.red.astronomy.sandbox.physics.VelocityVerletIntegrator
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.model.RenderBodyColor
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.snapshot.BodyRenderState
import com.alijafari.red.astronomy.sandbox.snapshot.SandboxRenderFrame

/**
 * High-performance, isolated forward-integration trajectory predictor.
 *
 * Runs non-destructive numerical prediction in an isolated PhysicsStateBuffer clone,
 * projecting deterministic future orbital paths for selected and active bodies.
 */
class TrajectoryPredictor(
    val maxBodies: Int = 20,
    val predictionSteps: Int = 120,
    private val forceSolver: GravitationalForceSolver = GravitationalForceSolver(),
    private val integrator: VelocityVerletIntegrator = VelocityVerletIntegrator(forceSolver)
) {
    var isEnabled: Boolean = true
    var predictSelectedOnly: Boolean = true
    var selectedBodyId: String? = null
    var stippleFrequency: Float = 14.0f // Number of dashes along trajectory

    // Isolated prediction state buffer (never touches live physics state)
    private val isolatedState = PhysicsStateBuffer(maxBodies)

    // Pre-allocated vertex buffer for GPU upload (4 floats per vertex: x, y, z, stepProgress)
    private val uploadBuffer = FloatArray(predictionSteps * 4)
    private val scratchPos = FloatArray(3)

    // GPU resources
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null

    fun init() {
        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_DYNAMIC_DRAW)
        vbo?.bind()

        val empty = FloatArray(predictionSteps * 4)
        vbo?.uploadData(empty)

        val stride = 4 * 4 // 16 bytes per vertex

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // Attribute 1: Step Progress (float: 0.0 -> 1.0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 3 * 4)

        vao?.unbind()
        vbo?.unbind()
    }

    private fun cloneSnapshotToIsolatedState(bodies: List<BodyRenderState>) {
        isolatedState.clear()
        val count = minOf(bodies.size, maxBodies)
        for (i in 0 until count) {
            val b = bodies[i]
            isolatedState.id[i] = b.id
            isolatedState.type[i] = b.type
            isolatedState.nameEn[i] = b.nameEn
            isolatedState.nameFa[i] = b.nameFa
            isolatedState.posX[i] = b.posX
            isolatedState.posY[i] = b.posY
            isolatedState.posZ[i] = b.posZ
            isolatedState.velX[i] = b.velX
            isolatedState.velY[i] = b.velY
            isolatedState.velZ[i] = b.velZ
            isolatedState.accX[i] = b.accX
            isolatedState.accY[i] = b.accY
            isolatedState.accZ[i] = b.accZ
            isolatedState.mass[i] = b.massKg
            isolatedState.radius[i] = b.radiusMeters
            isolatedState.visualScale[i] = b.visualScale
            isolatedState.colorHex[i] = b.colorHex
            isolatedState.isFixed[i] = b.isFixed
            isolatedState.isActive[i] = b.isActive
            isolatedState.theoreticalMetadata[i] = b.theoreticalMetadata
        }
        isolatedState.activeCount = count
        forceSolver.computeAccelerations(isolatedState)
    }

    private fun computePredictionDt(bodies: List<BodyRenderState>): Double {
        if (bodies.isEmpty()) return 3600.0

        var maxMass = 0.0
        var centralIdx = 0
        for (i in bodies.indices) {
            if (bodies[i].massKg > maxMass) {
                maxMass = bodies[i].massKg
                centralIdx = i
            }
        }

        val c = bodies[centralIdx]
        var minOrbitDist = Double.MAX_VALUE
        for (i in bodies.indices) {
            if (i == centralIdx || !bodies[i].isActive) continue
            val dx = bodies[i].posX - c.posX
            val dy = bodies[i].posY - c.posY
            val dz = bodies[i].posZ - c.posZ
            val dist = Math.sqrt(dx * dx + dy * dy + dz * dz)
            if (dist in 1.0..minOrbitDist) {
                minOrbitDist = dist
            }
        }

        if (minOrbitDist == Double.MAX_VALUE || minOrbitDist <= 0.0) {
            return 3600.0
        }

        val gM = AstroPhysicsConstants.G * maxMass.coerceAtLeast(1e20)
        val period = 2.0 * Math.PI * Math.sqrt((minOrbitDist * minOrbitDist * minOrbitDist) / gM)
        
        val targetSpan = period * 1.5
        val dt = (targetSpan / predictionSteps).coerceIn(60.0, 86400.0 * 15.0)
        return dt
    }

    fun draw(
        shader: ShaderProgram,
        snapshot: SandboxRenderFrame,
        scaleManager: RenderScaleManager,
        bodyColors: Array<FloatArray>
    ) {
        if (!isEnabled || vao == null || vbo == null) return
        val bodies = snapshot.bodies
        if (bodies.isEmpty()) return

        val count = minOf(bodies.size, maxBodies)

        val targetIndices = ArrayList<Int>()
        if (predictSelectedOnly && selectedBodyId != null) {
            val idx = bodies.indexOfFirst { it.id == selectedBodyId }
            if (idx in 0 until count) {
                targetIndices.add(idx)
            }
        } else {
            for (i in 0 until count) {
                if (bodies[i].isActive && !bodies[i].isFixed) {
                    targetIndices.add(i)
                }
            }
        }

        if (targetIndices.isEmpty()) return

        vao?.bind()
        vbo?.bind()

        val dt = computePredictionDt(bodies)

        for (targetIdx in targetIndices) {
            cloneSnapshotToIsolatedState(bodies)

            var outIdx = 0

            for (step in 0 until predictionSteps) {
                integrator.step(isolatedState, dt)

                val mappedX = isolatedState.posX[targetIdx]
                val mappedY = isolatedState.posY[targetIdx]
                val mappedZ = isolatedState.posZ[targetIdx]

                scaleManager.physicsToRenderPosition(mappedX, mappedY, mappedZ, scratchPos)

                val progress = step.toFloat() / (predictionSteps - 1).coerceAtLeast(1)

                uploadBuffer[outIdx++] = scratchPos[0]
                uploadBuffer[outIdx++] = scratchPos[1]
                uploadBuffer[outIdx++] = scratchPos[2]
                uploadBuffer[outIdx++] = progress
            }

            vbo?.uploadSubData(uploadBuffer, 0, outIdx)

            val color = if (targetIdx < bodyColors.size) bodyColors[targetIdx] else RenderBodyColor.DEFAULT_COLOR
            shader.setUniform4f("u_PredictionColor", color[0], color[1], color[2], 0.80f)
            shader.setUniform1f("u_StippleFrequency", stippleFrequency)

            GLES30.glLineWidth(1.8f)
            GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, predictionSteps)
        }

        vbo?.unbind()
        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
