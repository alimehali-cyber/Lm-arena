package com.alijafari.red.astronomy.sandbox.render.trails

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.model.RenderBodyColor

/**
 * High-performance circular ring-buffer manager for orbital trails.
 * Tracks orbital path history for up to 20 bodies with zero per-frame garbage collection.
 */
class TrailBufferManager(
    val maxBodies: Int = 20,
    val maxPointsPerBody: Int = 120
) {
    var isEnabled: Boolean = true

    // Circular ring buffer storage: [bodyIndex][pointIndex * 3]
    private val ringPositions = Array(maxBodies) { FloatArray(maxPointsPerBody * 3) }
    private val pointCounts = IntArray(maxBodies)
    private val headIndices = IntArray(maxBodies)
    private val lastAppendedPositions = Array(maxBodies) { FloatArray(3) }

    // Pre-allocated flat buffer for GPU upload (4 floats per vertex: x, y, z, alpha)
    private val uploadBuffer = FloatArray(maxPointsPerBody * 4)

    // GPU resources
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null

    fun init() {
        vao = GlVertexArray().apply { bind() }
        // Allocate dynamic VBO capable of holding maxPointsPerBody vertices
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_DYNAMIC_DRAW)
        vbo?.bind()
        // Initialize with empty buffer
        val empty = FloatArray(maxPointsPerBody * 4)
        vbo?.uploadData(empty)

        val stride = 4 * 4 // 16 bytes per vertex

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // Attribute 1: Alpha (float)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 3 * 4)

        vao?.unbind()
        vbo?.unbind()
    }

    fun clear() {
        for (i in 0 until maxBodies) {
            pointCounts[i] = 0
            headIndices[i] = 0
            lastAppendedPositions[i][0] = Float.NaN
            lastAppendedPositions[i][1] = Float.NaN
            lastAppendedPositions[i][2] = Float.NaN
        }
    }

    /**
     * Records a new visual position for body at [bodyIndex].
     * Only appends if the body moved at least [minVisualDistanceThreshold] to avoid clustering when stationary.
     */
    fun addPoint(bodyIndex: Int, x: Float, y: Float, z: Float, minDistanceSq: Float = 0.005f) {
        if (!isEnabled || bodyIndex !in 0 until maxBodies) return

        val last = lastAppendedPositions[bodyIndex]
        if (!last[0].isNaN()) {
            val dx = x - last[0]
            val dy = y - last[1]
            val dz = z - last[2]
            val distSq = dx * dx + dy * dy + dz * dz
            if (distSq < minDistanceSq) {
                return // Has not moved enough to warrant new trail segment
            }
        }

        last[0] = x
        last[1] = y
        last[2] = z

        val head = headIndices[bodyIndex]
        val posArray = ringPositions[bodyIndex]

        val offset = head * 3
        posArray[offset] = x
        posArray[offset + 1] = y
        posArray[offset + 2] = z

        headIndices[bodyIndex] = (head + 1) % maxPointsPerBody
        if (pointCounts[bodyIndex] < maxPointsPerBody) {
            pointCounts[bodyIndex]++
        }
    }

    /**
     * Renders trails for all active bodies.
     */
    fun draw(
        shader: ShaderProgram,
        activeBodyCount: Int,
        bodyColors: Array<FloatArray>
    ) {
        if (!isEnabled || vao == null || vbo == null) return

        val count = activeBodyCount.coerceAtMost(maxBodies)
        vao?.bind()
        vbo?.bind()

        for (b in 0 until count) {
            val numPoints = pointCounts[b]
            if (numPoints < 2) continue

            val head = headIndices[b]
            val posArray = ringPositions[b]
            var outIdx = 0

            // Read from oldest to newest so alpha fades from 0.0 to 1.0
            val startIdx = if (numPoints == maxPointsPerBody) head else 0
            for (p in 0 until numPoints) {
                val ringIdx = (startIdx + p) % maxPointsPerBody
                val rOffset = ringIdx * 3

                uploadBuffer[outIdx++] = posArray[rOffset]
                uploadBuffer[outIdx++] = posArray[rOffset + 1]
                uploadBuffer[outIdx++] = posArray[rOffset + 2]

                // Alpha gradient: 0.05 at tail, 0.95 at head
                val alpha = (p.toFloat() / (numPoints - 1)).coerceIn(0.0f, 1.0f)
                uploadBuffer[outIdx++] = alpha * alpha // Smooth quadratic fade
            }

            // Upload to GPU
            vbo?.uploadSubData(uploadBuffer, 0, outIdx)

            // Set trail color uniform
            val color = if (b < bodyColors.size) bodyColors[b] else RenderBodyColor.DEFAULT_COLOR
            shader.setUniform4f("u_TrailColor", color[0], color[1], color[2], 0.75f)

            // Draw as continuous line strip
            GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, numPoints)
        }

        vbo?.unbind()
        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
