package com.alijafari.red.astronomy.sandbox.render.gl

import android.opengl.GLES30

/**
 * Encapsulates an OpenGL ES 3.0 Program with cached attribute and uniform locations.
 */
class ShaderProgram(val programId: Int) {
    private val uniformLocations = HashMap<String, Int>()
    private val attributeLocations = HashMap<String, Int>()

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            GLES30.glGetUniformLocation(programId, name)
        }
    }

    fun getAttribLocation(name: String): Int {
        return attributeLocations.getOrPut(name) {
            GLES30.glGetAttribLocation(programId, name)
        }
    }

    fun setUniformMatrix4fv(name: String, matrix: FloatArray, offset: Int = 0) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES30.glUniformMatrix4fv(location, 1, false, matrix, offset)
        }
    }

    fun setUniform1f(name: String, value: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES30.glUniform1f(location, value)
        }
    }

    fun setUniform1i(name: String, value: Int) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES30.glUniform1i(location, value)
        }
    }

    fun setUniform2f(name: String, x: Float, y: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES30.glUniform2f(location, x, y)
        }
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES30.glUniform3f(location, x, y, z)
        }
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = getUniformLocation(name)
        if (location >= 0) {
            GLES30.glUniform4f(location, x, y, z, w)
        }
    }

    fun destroy() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            uniformLocations.clear()
            attributeLocations.clear()
        }
    }

    companion object {
        fun build(vertexSource: String, fragmentSource: String): ShaderProgram? {
            val prog = ShaderCompiler.createProgram(vertexSource, fragmentSource)
            return if (prog != 0) ShaderProgram(prog) else null
        }
    }
}
