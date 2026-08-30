package com.alijafari.red.astronomy.sandbox.render.geometry

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import kotlin.math.cos
import kotlin.math.sin

/**
 * GPU Geometry for planetary ring systems (e.g. Saturn's rings).
 * Generates an annular disc in the equatorial X-Z plane with radial UV coordinates
 * suitable for multi-band optical density, Cassini division, and shadow sampling.
 */
class RingMesh(
    val innerRadius: Float = 1.22f,
    val outerRadius: Float = 2.38f,
    val segments: Int = 96
) {
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null
    private var ibo: GlBuffer? = null
    private var indexCount: Int = 0

    fun init() {
        val numVertices = (segments + 1) * 2 // Inner and outer ring loops
        // 8 floats per vertex: position (3), normal (3), texCoord (2)
        val vertexData = FloatArray(numVertices * 8)
        val indices = ShortArray(segments * 6)

        val twoPi = (Math.PI * 2.0).toFloat()
        var vIdx = 0

        for (i in 0..segments) {
            val fraction = i.toFloat() / segments
            val angle = fraction * twoPi
            val cosA = cos(angle)
            val sinA = sin(angle)

            // 1. Inner Ring Vertex (u = 0.0)
            val inX = cosA * innerRadius
            val inZ = sinA * innerRadius
            vertexData[vIdx++] = inX
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = inZ
            // Normal (upwards Y)
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = 1.0f
            vertexData[vIdx++] = 0.0f
            // TexCoord: (u = radial fraction 0.0, v = angular fraction)
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = fraction

            // 2. Outer Ring Vertex (u = 1.0)
            val outX = cosA * outerRadius
            val outZ = sinA * outerRadius
            vertexData[vIdx++] = outX
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = outZ
            // Normal
            vertexData[vIdx++] = 0.0f
            vertexData[vIdx++] = 1.0f
            vertexData[vIdx++] = 0.0f
            // TexCoord: (u = radial fraction 1.0, v = angular fraction)
            vertexData[vIdx++] = 1.0f
            vertexData[vIdx++] = fraction
        }

        var iIdx = 0
        for (i in 0 until segments) {
            val inCur = (i * 2).toShort()
            val outCur = (i * 2 + 1).toShort()
            val inNext = ((i + 1) * 2).toShort()
            val outNext = ((i + 1) * 2 + 1).toShort()

            // Triangle 1: inCur -> outCur -> inNext
            indices[iIdx++] = inCur
            indices[iIdx++] = outCur
            indices[iIdx++] = inNext

            // Triangle 2: outCur -> outNext -> inNext
            indices[iIdx++] = outCur
            indices[iIdx++] = outNext
            indices[iIdx++] = inNext
        }

        indexCount = indices.size

        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER).apply { uploadData(vertexData) }
        ibo = GlBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER).apply { uploadData(indices) }

        vbo?.bind()
        ibo?.bind()

        val stride = 8 * 4 // 32 bytes

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
