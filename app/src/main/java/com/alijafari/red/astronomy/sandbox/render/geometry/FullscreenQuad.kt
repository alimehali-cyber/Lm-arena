package com.alijafari.red.astronomy.sandbox.render.geometry

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray

/**
 * Screen-aligned Fullscreen Quad for post-processing and Phase 5 Relativistic Black Hole ray tracing.
 */
class FullscreenQuad {
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null

    fun init() {
        val quadVertices = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )

        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER).apply { uploadData(quadVertices) }
        vbo?.bind()

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 2 * 4, 0)

        vao?.unbind()
        vbo?.unbind()
    }

    fun draw() {
        vao?.bind()
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
