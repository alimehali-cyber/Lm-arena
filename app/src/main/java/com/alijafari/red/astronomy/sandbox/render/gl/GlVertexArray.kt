package com.alijafari.red.astronomy.sandbox.render.gl

import android.opengl.GLES30

/**
 * Encapsulates an OpenGL ES 3.0 Vertex Array Object (VAO).
 */
class GlVertexArray {
    var vaoId: Int = 0
        private set

    init {
        val ids = IntArray(1)
        GLES30.glGenVertexArrays(1, ids, 0)
        vaoId = ids[0]
    }

    fun bind() {
        GLES30.glBindVertexArray(vaoId)
    }

    fun unbind() {
        GLES30.glBindVertexArray(0)
    }

    fun destroy() {
        if (vaoId != 0) {
            val ids = intArrayOf(vaoId)
            GLES30.glDeleteVertexArrays(1, ids, 0)
            vaoId = 0
        }
    }
}
