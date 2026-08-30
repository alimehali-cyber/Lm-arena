package com.alijafari.red.astronomy.sandbox.render.gl

import android.opengl.GLES30
import android.util.Log

/**
 * Robust OpenGL ES 3.0 shader compilation and program linkage helper.
 */
object ShaderCompiler {
    private const val TAG = "ShaderCompiler"

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Error creating shader of type: $type")
            return 0
        }

        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            Log.e(TAG, "Compilation error in shader type $type:\n$log\nSource:\n$shaderCode")
            GLES30.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0

        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            GLES30.glDeleteShader(vertexShader)
            return 0
        }

        val program = GLES30.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Could not create GL program")
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return 0
        }

        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            Log.e(TAG, "Error linking program:\n$log")
            GLES30.glDeleteProgram(program)
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return 0
        }

        // Shaders can be safely deleted once linked into program
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)

        return program
    }

    fun checkGlError(op: String) {
        var error = GLES30.glGetError()
        while (error != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "GL error after $op: 0x${Integer.toHexString(error)}")
            error = GLES30.glGetError()
        }
    }
}
