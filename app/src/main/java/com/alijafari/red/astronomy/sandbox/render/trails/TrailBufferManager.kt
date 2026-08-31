package com.alijafari.red.astronomy.sandbox.render.trails

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.model.RenderBodyColor

/**
 * High-performance circular ring-buffer manager for orbital trails.
 * Tracks orbital path history for up to 20 bodies with zero per-frame garbage collection.
 *
 * Supports:
 * - Authoritative snapshot tracking with smooth sample interpolation.
 * - Reset and discontinuity detection (time rewind, spatial teleportation).
 * - Per-body enable/disable and selection accentuation.
 * - Quadratic/exponential age-based alpha fading.
 */
class TrailBufferManager(
    val maxBodies: Int = 20,
    val maxPointsPerBody: Int = 180
) {
    var isEnabled: Boolean = true
    val isBodyTrailEnabled = BooleanArray(maxBodies) { true }
    var selectedBodyIndex: Int = -1

    // Circular ring buffer storage: [bodyIndex][pointIndex * 3]
    private val ringPositions = Array(maxBodies) { FloatArray(maxPointsPerBody * 3) }
    private val pointCounts = IntArray(maxBodies)
    private val headIndices = IntArray(maxBodies)
    private val lastAppendedPositions = Array(maxBodies) { FloatArray(3) { Float.NaN } }
    private var lastRecordedSimTimeSeconds: Double = -1.0

    // Pre-allocated flat buffer for GPU upload (4 floats per vertex: x, y, z, alpha)
    private val uploadBuffer = FloatArray(maxPointsPerBody * 4)

    // GPU resources
    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null

    fun init() {
        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_DYNAMIC_DRAW)
        vbo?.bind()

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

    /**
     * Clears all orbital trail history.
     */
    fun clear() {
        for (i in 0 until maxBodies) {
            clearBody(i)
        }
        lastRecordedSimTimeSeconds = -1.0
    }

    /**
     * Clears orbital trail for an individual body (e.g. on merger, removal, or teleportation).
     */
    fun clearBody(bodyIndex: Int) {
        if (bodyIndex in 0 until maxBodies) {
            pointCounts[bodyIndex] = 0
            headIndices[bodyIndex] = 0
            lastAppendedPositions[bodyIndex][0] = Float.NaN
            lastAppendedPositions[bodyIndex][1] = Float.NaN
            lastAppendedPositions[bodyIndex][2] = Float.NaN
        }
    }

    /**
     * Returns the number of recorded orbital trail points for [bodyIndex].
     */
    fun getPointCount(bodyIndex: Int): Int {
        return if (bodyIndex in 0 until maxBodies) pointCounts[bodyIndex] else 0
    }

    /**
     * Records a new visual position for body at [bodyIndex].
     * Includes automatic time-rewind reset and distance-based clustering prevention.
     */
    fun addPoint(
        bodyIndex: Int,
        x: Float,
        y: Float,
        z: Float,
        currentSimTimeSeconds: Double = 0.0,
        minDistanceSq: Float = 0.0001f,
        maxDiscontinuityDistSq: Float = 400.0f
    ) {
        if (!isEnabled || bodyIndex !in 0 until maxBodies || !isBodyTrailEnabled[bodyIndex]) return

        // Check for simulation rewind / reset
        if (lastRecordedSimTimeSeconds >= 0.0 && currentSimTimeSeconds < lastRecordedSimTimeSeconds - 0.001) {
            clear()
        }
        lastRecordedSimTimeSeconds = currentSimTimeSeconds

        val last = lastAppendedPositions[bodyIndex]
        if (!last[0].isNaN()) {
            val dx = x - last[0]
            val dy = y - last[1]
            val dz = z - last[2]
            val distSq = dx * dx + dy * dy + dz * dz

            // Discontinuity / Teleport detected -> Reset this body's trail
            if (distSq > maxDiscontinuityDistSq) {
                clearBody(bodyIndex)
            } else if (distSq < minDistanceSq) {
                return // Has not moved sufficiently to warrant new segment
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
     * Renders orbital trails for all active bodies with alpha gradients and selection accentuation.
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
            if (!isBodyTrailEnabled[b]) continue
            val numPoints = pointCounts[b]
            if (numPoints < 2) continue

            val isSelected = (b == selectedBodyIndex)
            val head = headIndices[b]
            val posArray = ringPositions[b]

            // Oldest point starts at (head - numPoints + maxPointsPerBody) % maxPointsPerBody
            val startIdx = (head - numPoints + maxPointsPerBody) % maxPointsPerBody

            var outIdx = 0
            for (i in 0 until numPoints) {
                val ringIdx = (startIdx + i) % maxPointsPerBody
                val ringOffset = ringIdx * 3

                val x = posArray[ringOffset]
                val y = posArray[ringOffset + 1]
                val z = posArray[ringOffset + 2]

                // Linear normalized age: 0.0 (oldest/faintest) -> 1.0 (newest/brightest)
                val ageNorm = i.toFloat() / (numPoints - 1).coerceAtLeast(1)

                // Smooth quadratic alpha falloff: f(t) = t^1.6
                val alpha = Math.pow(ageNorm.toDouble(), 1.4).toFloat().coerceIn(0.10f, 1.0f)

                uploadBuffer[outIdx++] = x
                uploadBuffer[outIdx++] = y
                uploadBuffer[outIdx++] = z
                uploadBuffer[outIdx++] = if (isSelected) alpha else alpha * 0.85f
            }

            // Stream upload to dynamic VBO
            vbo?.uploadSubData(uploadBuffer, 0, outIdx)

            // Uniform color matching the body
            val color = if (b < bodyColors.size) bodyColors[b] else RenderBodyColor.DEFAULT_COLOR
            shader.setUniform4f(
                "u_TrailColor",
                color[0],
                color[1],
                color[2],
                if (isSelected) 1.0f else 0.85f
            )
            shader.setUniform1f("u_IsSelected", if (isSelected) 1.0f else 0.0f)

            GLES30.glLineWidth(if (isSelected) 3.0f else 2.0f)
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
