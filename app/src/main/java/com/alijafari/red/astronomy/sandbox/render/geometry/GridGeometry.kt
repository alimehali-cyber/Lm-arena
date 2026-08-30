package com.alijafari.red.astronomy.sandbox.render.geometry

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural orbital reference plane grid and distance rings on the XZ plane (Y = 0).
 */
class GridGeometry {
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null
    private var vertexCount: Int = 0

    fun init() {
        val ringRadii = floatArrayOf(2.5f, 5.0f, 10.0f, 15.0f, 20.0f, 30.0f, 40.0f, 60.0f)
        val segments = 72
        val radialLines = 12

        // Vertices: each vertex has position (3 floats) and color (4 floats) -> 7 floats
        val totalVertices = ringRadii.size * segments * 2 + radialLines * 2 + 4 // +4 for axis markers
        val vertexData = FloatArray(totalVertices * 7)
        var vIdx = 0

        val twoPi = (Math.PI * 2.0).toFloat()

        // 1. Concentric Range Rings
        for (radius in ringRadii) {
            val alpha = when (radius) {
                10.0f -> 0.35f // 1 AU marker emphasis
                else -> 0.15f
            }
            for (s in 0 until segments) {
                val theta1 = (s.toFloat() / segments) * twoPi
                val theta2 = ((s + 1).toFloat() / segments) * twoPi

                val x1 = radius * cos(theta1)
                val z1 = radius * sin(theta1)
                val x2 = radius * cos(theta2)
                val z2 = radius * sin(theta2)

                // Point 1
                vertexData[vIdx++] = x1
                vertexData[vIdx++] = 0.0f
                vertexData[vIdx++] = z1
                vertexData[vIdx++] = 0.3f
                vertexData[vIdx++] = 0.4f
                vertexData[vIdx++] = 0.6f
                vertexData[vIdx++] = alpha

                // Point 2
                vertexData[vIdx++] = x2
                vertexData[vIdx++] = 0.0f
                vertexData[vIdx++] = z2
                vertexData[vIdx++] = 0.3f
                vertexData[vIdx++] = 0.4f
                vertexData[vIdx++] = 0.6f
                vertexData[vIdx++] = alpha
            }
        }

        // 2. Radial Guide Rays
        val maxRayRadius = 60.0f
        for (r in 0 until radialLines) {
            val angle = (r.toFloat() / radialLines) * twoPi
            val rx = maxRayRadius * cos(angle)
            val rz = maxRayRadius * sin(angle)

            // Center
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = 0.25f
            vertexData[vIdx++] = 0.35f
            vertexData[vIdx++] = 0.5f
            vertexData[vIdx++] = 0.08f

            // Outer
            vertexData[vIdx++] = rx
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = rz
            vertexData[vIdx++] = 0.25f
            vertexData[vIdx++] = 0.35f
            vertexData[vIdx++] = 0.5f
            vertexData[vIdx++] = 0.08f
        }

        // 3. Primary Reference Axes (+X: Vernal Equinox Red line, +Z: Blue line)
        // +X Axis (Red)
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.9f
        vertexData[vIdx++] = 0.2f
        vertexData[vIdx++] = 0.2f
        vertexData[vIdx++] = 0.6f

        vertexData[vIdx++] = 65.0f
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.9f
        vertexData[vIdx++] = 0.2f
        vertexData[vIdx++] = 0.2f
        vertexData[vIdx++] = 0.6f

        // +Z Axis (Blue)
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.2f
        vertexData[vIdx++] = 0.4f
        vertexData[vIdx++] = 0.9f
        vertexData[vIdx++] = 0.6f

        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 0.0f
        vertexData[vIdx++] = 65.0f
        vertexData[vIdx++] = 0.2f
        vertexData[vIdx++] = 0.4f
        vertexData[vIdx++] = 0.9f
        vertexData[vIdx++] = 0.6f

        vertexCount = vIdx / 7

        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER).apply { uploadData(vertexData) }
        vbo?.bind()

        val stride = 7 * 4 // 28 bytes per vertex

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // Attribute 1: Color (vec4)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 3 * 4)

        vao?.unbind()
        vbo?.unbind()
    }

    fun draw() {
        vao?.bind()
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, vertexCount)
        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
