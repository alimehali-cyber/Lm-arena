package com.alijafari.red.astronomy.sandbox.render.collision

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.model.CollisionEvent
import com.alijafari.red.astronomy.sandbox.model.CollisionPolicy
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.render.trails.TrailBufferManager
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Internal state for an active shockwave ripple.
 */
class ActiveShockwave {
    var isActive: Boolean = false
    var originX: Float = 0.0f
    var originY: Float = 0.0f
    var originZ: Float = 0.0f
    var startSimTimeSec: Double = 0.0
    var lifetimeSec: Double = 3.0
    var expansionSpeed: Float = 2.5f
    var baseRadius: Float = 0.5f
    var isBlackHoleAccretion: Boolean = false
}

/**
 * Visualizer for physical body mergers and collision shockwaves.
 * Renders expanding luminous rings and coordinates trail cleanup on body mergers.
 */
class CollisionVisualizer(
    val maxConcurrentShockwaves: Int = 8
) {
    var isEnabled: Boolean = true

    private val shockwavePool = Array(maxConcurrentShockwaves) { ActiveShockwave() }
    private var lastObservedCollisionTimeSec: Double = -1.0
    private val scratchPos = FloatArray(3)

    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null
    private val ringSegments = 36
    private var ringVertexCount = ringSegments * 2

    fun init() {
        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_STATIC_DRAW)
        vbo?.bind()

        val ringVertices = FloatArray(ringVertexCount * 3)
        var idx = 0
        for (i in 0 until ringSegments) {
            val theta1 = (i.toFloat() / ringSegments) * 2.0f * Math.PI.toFloat()
            val theta2 = ((i + 1).toFloat() / ringSegments) * 2.0f * Math.PI.toFloat()

            // Line segment (theta1 -> theta2) on XZ plane
            ringVertices[idx++] = cos(theta1); ringVertices[idx++] = 0.0f; ringVertices[idx++] = sin(theta1)
            ringVertices[idx++] = cos(theta2); ringVertices[idx++] = 0.0f; ringVertices[idx++] = sin(theta2)
        }

        vbo?.uploadData(ringVertices)

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0)

        vao?.unbind()
        vbo?.unbind()
    }

    /**
     * Ingests recent collisions from snapshot, triggering shockwave animations and cleaning merged trails.
     */
    fun processCollisionEvents(
        events: List<CollisionEvent>,
        currentSimTimeSec: Double,
        scaleManager: RenderScaleManager,
        trailManager: TrailBufferManager?,
        bodyIds: List<String>
    ) {
        if (!isEnabled || events.isEmpty()) return

        for (event in events) {
            if (event.timestampSeconds <= lastObservedCollisionTimeSec) continue

            // 1. Clear trail of the subordinate body that was consumed
            if (trailManager != null) {
                val subordinateId = if (event.resultingBodyId == event.primaryBodyId) event.secondaryBodyId else event.primaryBodyId
                val subIdx = bodyIds.indexOf(subordinateId)
                if (subIdx >= 0) {
                    trailManager.clearBody(subIdx)
                }
            }

            // 2. Spawn a shockwave in pool
            val slot = shockwavePool.firstOrNull { !it.isActive } ?: shockwavePool[0]
            scaleManager.physicsToRenderPosition(
                event.collisionPosition.x,
                event.collisionPosition.y,
                event.collisionPosition.z,
                scratchPos
            )

            slot.isActive = true
            slot.originX = scratchPos[0]
            slot.originY = scratchPos[1]
            slot.originZ = scratchPos[2]
            slot.startSimTimeSec = currentSimTimeSec
            slot.lifetimeSec = 3.5
            slot.isBlackHoleAccretion = (event.policyApplied == CollisionPolicy.BLACK_HOLE_ACCEDE)
            slot.expansionSpeed = (event.relativeVelocity / 1000.0).coerceIn(1.5, 8.0).toFloat()
            slot.baseRadius = 0.8f

            lastObservedCollisionTimeSec = maxOf(lastObservedCollisionTimeSec, event.timestampSeconds)
        }
    }

    /**
     * Renders active shockwave ripples.
     */
    fun draw(
        shader: ShaderProgram,
        currentSimTimeSec: Double
    ) {
        if (!isEnabled || vao == null) return

        vao?.bind()

        for (i in 0 until maxConcurrentShockwaves) {
            val s = shockwavePool[i]
            if (!s.isActive) continue

            val elapsed = currentSimTimeSec - s.startSimTimeSec
            if (elapsed < 0.0 || elapsed > s.lifetimeSec) {
                s.isActive = false
                continue
            }

            val progress = (elapsed / s.lifetimeSec).toFloat()
            val radius = s.baseRadius + s.expansionSpeed * elapsed.toFloat()
            val alphaDecay = exp(-2.5f * progress)

            shader.setUniform3f("u_ShockwaveOrigin", s.originX, s.originY, s.originZ)
            shader.setUniform1f("u_CurrentRadius", radius)
            shader.setUniform1f("u_AlphaDecay", alphaDecay)

            if (s.isBlackHoleAccretion) {
                // Violet / Electric Cyan for relativistic accretion
                shader.setUniform4f("u_ShockwaveColor", 0.70f, 0.20f, 1.0f, 0.95f)
            } else {
                // Fiery Orange / Gold for planetary impact merger
                shader.setUniform4f("u_ShockwaveColor", 1.0f, 0.45f, 0.10f, 0.95f)
            }

            GLES30.glLineWidth(3.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, ringVertexCount)
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
