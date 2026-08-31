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
    var predictSelectedOnly: Boolean = false
    var selectedBodyId: String? = null
    var stippleFrequency: Float = 12.0f // Number of dashes along trajectory

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

    /**
     * Calculates the optimal forward integration timestep dt for a specific target body,
     * ensuring the prediction horizon covers approximately 1 full orbital period.
     */
    fun computePredictionDtForBody(targetIdx: Int, bodies: List<BodyRenderState>): Double {
        if (bodies.isEmpty() || targetIdx !in bodies.indices) return 3600.0

        val target = bodies[targetIdx]
        if (target.isFixed || !target.isActive) return 3600.0

        // Find primary gravitational attractor (max G * M / r^2)
        var maxGravity = 0.0
        var primaryAttractorIdx = -1
        var distToAttractor = 1.0e11

        for (j in bodies.indices) {
            if (j == targetIdx || !bodies[j].isActive || bodies[j].massKg <= 0.0) continue
            val dx = target.posX - bodies[j].posX
            val dy = target.posY - bodies[j].posY
            val dz = target.posZ - bodies[j].posZ
            val rSq = (dx * dx + dy * dy + dz * dz).coerceAtLeast(1.0)
            val grav = (AstroPhysicsConstants.G * bodies[j].massKg) / rSq

            if (grav > maxGravity) {
                maxGravity = grav
                primaryAttractorIdx = j
                distToAttractor = Math.sqrt(rSq)
            }
        }

        val periodSeconds = if (primaryAttractorIdx >= 0) {
            val attractor = bodies[primaryAttractorIdx]
            val totalM = (attractor.massKg + target.massKg).coerceAtLeast(1.0e20)
            val gM = AstroPhysicsConstants.G * totalM
            // Kepler's Third Law period T = 2 * pi * sqrt(a^3 / GM)
            2.0 * Math.PI * Math.sqrt((distToAttractor * distToAttractor * distToAttractor) / gM)
        } else {
            // Unbound body: calculate time to cross typical system span based on current velocity
            val v = Math.sqrt(target.velX * target.velX + target.velY * target.velY + target.velZ * target.velZ)
            if (v > 1.0) (distToAttractor / v) else 86400.0 * 30.0
        }

        val targetHorizon = periodSeconds * 1.15
        return (targetHorizon / predictionSteps).coerceIn(10.0, 86400.0 * 30.0)
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

        for (targetIdx in targetIndices) {
            cloneSnapshotToIsolatedState(bodies)
            val dt = computePredictionDtForBody(targetIdx, bodies)

            // Check if this body is a satellite of a parent planet (e.g. Moon of Earth)
            var parentIdx = -1
            val targetMass = isolatedState.mass[targetIdx]
            val isTargetStar = (isolatedState.type[targetIdx] == com.alijafari.red.astronomy.sandbox.model.SandboxBodyType.SUN ||
                    isolatedState.type[targetIdx] == com.alijafari.red.astronomy.sandbox.model.SandboxBodyType.BLACK_HOLE)
            if (!isTargetStar && targetMass < 1.0e29) {
                var bestD = Double.MAX_VALUE
                for (j in 0 until isolatedState.activeCount) {
                    if (j == targetIdx || isolatedState.mass[j] <= targetMass * 5.0) continue
                    val dx = isolatedState.posX[targetIdx] - isolatedState.posX[j]
                    val dy = isolatedState.posY[targetIdx] - isolatedState.posY[j]
                    val dz = isolatedState.posZ[targetIdx] - isolatedState.posZ[j]
                    val d = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                    if (d < 5.0e9 && d < bestD) {
                        bestD = d
                        parentIdx = j
                    }
                }
            }

            var outIdx = 0
            val parentScratch = FloatArray(3)

            for (step in 0 until predictionSteps) {
                integrator.step(isolatedState, dt)

                val mappedX = isolatedState.posX[targetIdx]
                val mappedY = isolatedState.posY[targetIdx]
                val mappedZ = isolatedState.posZ[targetIdx]

                if (parentIdx >= 0) {
                    val pX = isolatedState.posX[parentIdx]
                    val pY = isolatedState.posY[parentIdx]
                    val pZ = isolatedState.posZ[parentIdx]
                    scaleManager.physicsToRenderPosition(pX, pY, pZ, parentScratch)

                    val dx = mappedX - pX
                    val dy = mappedY - pY
                    val dz = mappedZ - pZ
                    val physD = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                    if (physD < 1e-3) {
                        scratchPos[0] = parentScratch[0]
                        scratchPos[1] = parentScratch[1]
                        scratchPos[2] = parentScratch[2]
                    } else {
                        val dirX = (dx / physD).toFloat()
                        val dirY = (dy / physD).toFloat()
                        val dirZ = (dz / physD).toFloat()
                        val visualOffset = (physD / 3.844e8).toFloat() * 1.5f + 0.6f
                        scratchPos[0] = parentScratch[0] + dirX * visualOffset
                        scratchPos[1] = parentScratch[1] + dirY * visualOffset
                        scratchPos[2] = parentScratch[2] + dirZ * visualOffset
                    }
                } else {
                    scaleManager.physicsToRenderPosition(mappedX, mappedY, mappedZ, scratchPos)
                }

                val progress = step.toFloat() / (predictionSteps - 1).coerceAtLeast(1)

                uploadBuffer[outIdx++] = scratchPos[0]
                uploadBuffer[outIdx++] = scratchPos[1]
                uploadBuffer[outIdx++] = scratchPos[2]
                uploadBuffer[outIdx++] = progress
            }

            vbo?.uploadSubData(uploadBuffer, 0, outIdx)

            val color = if (targetIdx < bodyColors.size) bodyColors[targetIdx] else RenderBodyColor.DEFAULT_COLOR
            val isSelected = (bodies[targetIdx].id == selectedBodyId)
            shader.setUniform4f(
                "u_PredictionColor",
                color[0],
                color[1],
                color[2],
                if (isSelected) 1.0f else 0.85f
            )
            shader.setUniform1f("u_StippleFrequency", stippleFrequency)

            GLES30.glLineWidth(if (isSelected) 2.5f else 1.8f)
            GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, predictionSteps)
        }

        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
