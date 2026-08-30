package com.alijafari.red.astronomy.sandbox.render.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Encapsulates an OpenGL ES 3.0 Vertex Buffer Object (VBO) or Index Buffer Object (IBO).
 */
class GlBuffer(
    val target: Int,
    val usage: Int = GLES30.GL_STATIC_DRAW
) {
    var bufferId: Int = 0
        private set

    init {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        bufferId = ids[0]
    }

    fun bind() {
        GLES30.glBindBuffer(target, bufferId)
    }

    fun unbind() {
        GLES30.glBindBuffer(target, 0)
    }

    fun uploadData(data: FloatArray, usageOverride: Int = usage) {
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data)
        buffer.position(0)

        bind()
        GLES30.glBufferData(target, data.size * 4, buffer, usageOverride)
        unbind()
    }

    fun uploadSubData(data: FloatArray, offsetBytes: Int, count: Int) {
        val buffer = ByteBuffer.allocateDirect(count * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data, 0, count)
        buffer.position(0)

        bind()
        GLES30.glBufferSubData(target, offsetBytes, count * 4, buffer)
        unbind()
    }

    fun uploadData(data: ShortArray, usageOverride: Int = usage) {
        val buffer = ByteBuffer.allocateDirect(data.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        buffer.put(data)
        buffer.position(0)

        bind()
        GLES30.glBufferData(target, data.size * 2, buffer, usageOverride)
        unbind()
    }

    fun destroy() {
        if (bufferId != 0) {
            val ids = intArrayOf(bufferId)
            GLES30.glDeleteBuffers(1, ids, 0)
            bufferId = 0
        }
    }
}
