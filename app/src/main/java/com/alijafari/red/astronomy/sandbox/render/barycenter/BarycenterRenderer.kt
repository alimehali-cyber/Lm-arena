package com.alijafari.red.astronomy.sandbox.render.barycenter

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.snapshot.BodyRenderState
import kotlin.math.cos
import kotlin.math.sin

/**
 * OpenGL ES 3.0 renderer for the system Barycenter (Center of Mass) reticle.
 * Renders an orbital-plane concentric marker and crosshair reticle at the exact barycenter.
 */
class BarycenterRenderer(
    val calculator: BarycenterCalculator = BarycenterCalculator()
) {
    var isEnabled: Boolean = true
    var latestBarycenterInfo = BarycenterInfo()
        private set

    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null
    private var totalVertexCount: Int = 0
    private val scratchPos = FloatArray(3)

    fun init() {
        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_STATIC_DRAW)
        vbo?.bind()

        // Build geometric reticle: 2 concentric rings + 4 crosshair arms
        val ringSegments = 32
        val vertexList = ArrayList<Float>()

        // Inner ring (radius = 0.5)
        for (i in 0 until ringSegments) {
            val theta1 = (i.toFloat() / ringSegments) * 2.0f * Math.PI.toFloat()
            val theta2 = ((i + 1).toFloat() / ringSegments) * 2.0f * Math.PI.toFloat()

            // Segment start
            vertexList.add(cos(theta1) * 0.5f)
            vertexList.add(0.0f)
            vertexList.add(sin(theta1) * 0.5f)

            // Segment end
            vertexList.add(cos(theta2) * 0.5f)
            vertexList.add(0.0f)
            vertexList.add(sin(theta2) * 0.5f)
        }

        // Outer ring (radius = 1.0)
        for (i in 0 until ringSegments) {
            val theta1 = (i.toFloat() / ringSegments) * 2.0f * Math.PI.toFloat()
            val theta2 = ((i + 1).toFloat() / ringSegments) * 2.0f * Math.PI.toFloat()

            vertexList.add(cos(theta1))
            vertexList.add(0.0f)
            vertexList.add(sin(theta1))

            vertexList.add(cos(theta2))
            vertexList.add(0.0f)
            vertexList.add(sin(theta2))
        }

        // Crosshair arm 1: X axis (-1.4 to 1.4)
        vertexList.add(-1.4f); vertexList.add(0.0f); vertexList.add(0.0f)
        vertexList.add(1.4f); vertexList.add(0.0f); vertexList.add(0.0f)

        // Crosshair arm 2: Z axis (-1.4 to 1.4)
        vertexList.add(0.0f); vertexList.add(0.0f); vertexList.add(-1.4f)
        vertexList.add(0.0f); vertexList.add(0.0f); vertexList.add(1.4f)

        // Crosshair arm 3: Y axis vertical tick (-0.6 to 0.6)
        vertexList.add(0.0f); vertexList.add(-0.6f); vertexList.add(0.0f)
        vertexList.add(0.0f); vertexList.add(0.6f); vertexList.add(0.0f)

        val array = vertexList.toFloatArray()
        totalVertexCount = array.size / 3

        vbo?.uploadData(array)

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0)

        vao?.unbind()
        vbo?.unbind()
    }

    /**
     * Renders barycenter marker in 3D world space.
     */
    fun draw(
        shader: ShaderProgram,
        bodies: List<BodyRenderState>,
        scaleManager: RenderScaleManager,
        cameraDistance: Float
    ) {
        if (!isEnabled || vao == null) return

        latestBarycenterInfo = calculator.computeBarycenter(bodies)
        if (latestBarycenterInfo.activeBodyCount < 2) return

        val p = latestBarycenterInfo.positionPhysicsMeters
        scaleManager.physicsToRenderPosition(p.x, p.y, p.z, scratchPos)

        // Scale marker adaptively with camera distance so it remains sharp and visible
        val markerScale = (cameraDistance * 0.05f).coerceIn(0.8f, 15.0f)

        vao?.bind()

        shader.setUniform3f("u_BarycenterWorldPos", scratchPos[0], scratchPos[1], scratchPos[2])
        shader.setUniform1f("u_MarkerScale", markerScale)
        // High-visibility Gold / Amber scientific color
        shader.setUniform4f("u_BarycenterColor", 1.0f, 0.84f, 0.0f, 0.95f)

        GLES30.glLineWidth(2.5f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, totalVertexCount)

        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
