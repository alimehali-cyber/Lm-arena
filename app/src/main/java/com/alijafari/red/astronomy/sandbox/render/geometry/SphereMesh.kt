package com.alijafari.red.astronomy.sandbox.render.geometry

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable GPU UV Sphere geometry shared across all celestial bodies in the scene.
 * Dynamically configurable tessellation based on device performance quality level.
 */
class SphereMesh(
    val rings: Int = 32,
    val sectors: Int = 32
) {
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null
    private var ibo: GlBuffer? = null
    private var indexCount: Int = 0

    fun init() {
        val numVertices = (rings + 1) * (sectors + 1)
        // 8 floats per vertex: position (3), normal (3), texCoord (2)
        val vertexData = FloatArray(numVertices * 8)
        val indices = ShortArray(rings * sectors * 6)

        val rStep = 1.0f / rings
        val sStep = 1.0f / sectors
        var vIdx = 0

        val pi = Math.PI.toFloat()
        val twoPi = (Math.PI * 2.0).toFloat()

        for (r in 0..rings) {
            val v = r * rStep
            val phi = v * pi // 0 to PI
            val y = cos(phi)
            val sinPhi = sin(phi)

            for (s in 0..sectors) {
                val u = s * sStep
                val theta = u * twoPi // 0 to 2*PI
                val x = sinPhi * sin(theta)
                val z = sinPhi * cos(theta)

                // Position (unit sphere radius = 1.0)
                vertexData[vIdx++] = x
                vertexData[vIdx++] = y
                vertexData[vIdx++] = z

                // Normal (same as position for unit sphere)
                vertexData[vIdx++] = x
                vertexData[vIdx++] = y
                vertexData[vIdx++] = z

                // UV Coordinates
                vertexData[vIdx++] = u
                vertexData[vIdx++] = 1.0f - v
            }
        }

        var iIdx = 0
        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val cur = (r * (sectors + 1) + s).toShort()
                val next = ((r + 1) * (sectors + 1) + s).toShort()

                indices[iIdx++] = cur
                indices[iIdx++] = (cur + 1).toShort()
                indices[iIdx++] = next

                indices[iIdx++] = (cur + 1).toShort()
                indices[iIdx++] = (next + 1).toShort()
                indices[iIdx++] = next
            }
        }

        indexCount = indices.size

        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER).apply { uploadData(vertexData) }
        ibo = GlBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER).apply { uploadData(indices) }

        vbo?.bind()
        ibo?.bind()

        val stride = 8 * 4 // 32 bytes per vertex

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // Attribute 1: Normal (vec3)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)

        // Attribute 2: TexCoord (vec2)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)

        vao?.unbind()
        vbo?.unbind()
        ibo?.unbind()
    }

    fun draw() {
        vao?.bind()
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()
        vao = null
        vbo = null
        ibo = null
    }
}
