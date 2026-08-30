package com.alijafari.red.astronomy.sandbox.render.geometry

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GPU Starfield geometry distributing stars across the celestial sphere with spectral color temperatures.
 */
class StarfieldGeometry(
    val starCount: Int = 4000
) {
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null

    fun init() {
        val random = Random(42L) // Deterministic seed for visual stability across app sessions

        // 7 floats per star: position(3), brightness(1), color(3)
        val data = FloatArray(starCount * 7)
        var idx = 0

        // Fibonacci spherical distribution for uniform coverage
        val phi = (1.0 + sqrt(5.0)) / 2.0 // Golden ratio

        // Spectral class color presets (O/B Blue, A White, G Yellow, K Orange, M Red)
        val spectralColors = arrayOf(
            floatArrayOf(0.70f, 0.82f, 1.00f), // Class O/B (Blue-white)
            floatArrayOf(0.92f, 0.95f, 1.00f), // Class A (White)
            floatArrayOf(1.00f, 0.98f, 0.88f), // Class F/G (Warm Yellow-white)
            floatArrayOf(1.00f, 0.80f, 0.55f), // Class K (Orange)
            floatArrayOf(1.00f, 0.55f, 0.40f)  // Class M (Red dwarf)
        )

        for (i in 0 until starCount) {
            val y = 1.0 - (i / (starCount - 1.0).toDouble()) * 2.0 // 1 to -1
            val radius = sqrt(1.0 - y * y)
            val theta = 2.0 * Math.PI * i / phi

            val x = cos(theta) * radius
            val z = sin(theta) * radius

            // Direction on unit sphere
            data[idx++] = x.toFloat()
            data[idx++] = y.toFloat()
            data[idx++] = z.toFloat()

            // Apparent stellar brightness (power law: mostly dim stars, rare bright stars)
            val r = random.nextFloat()
            val brightness = (r * r * r) * 0.85f + 0.15f
            data[idx++] = brightness

            // Color choice weighted towards common stars
            val colorIndex = when {
                r > 0.92f -> 0 // Blue
                r > 0.70f -> 1 // White
                r > 0.40f -> 2 // Yellow
                r > 0.15f -> 3 // Orange
                else -> 4      // Red
            }
            val color = spectralColors[colorIndex]
            data[idx++] = color[0]
            data[idx++] = color[1]
            data[idx++] = color[2]
        }

        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER).apply { uploadData(data) }
        vbo?.bind()

        val stride = 7 * 4 // 28 bytes per star

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // Attribute 1: Brightness (float)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 3 * 4)

        // Attribute 2: Color (vec3)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 4 * 4)

        vao?.unbind()
        vbo?.unbind()
    }

    fun draw() {
        vao?.bind()
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, starCount)
        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
