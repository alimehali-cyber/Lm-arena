package com.alijafari.red.astronomy.sandbox.render.vectors

import android.opengl.GLES30
import com.alijafari.red.astronomy.sandbox.model.SandboxBodyType
import com.alijafari.red.astronomy.sandbox.render.gl.GlBuffer
import com.alijafari.red.astronomy.sandbox.render.gl.GlVertexArray
import com.alijafari.red.astronomy.sandbox.render.gl.ShaderProgram
import com.alijafari.red.astronomy.sandbox.render.scale.RenderScaleManager
import com.alijafari.red.astronomy.sandbox.snapshot.BodyRenderState
import kotlin.math.sqrt

/**
 * High-performance 3D vector overlay renderer for:
 * 1. Velocity Vectors (Cyan / Aqua)
 * 2. Gravitational Acceleration Vectors (Amber / Gold)
 */
class VectorOverlayRenderer(
    val maxBodies: Int = 20
) {
    var showVelocityVectors: Boolean = true
    var showAccelerationVectors: Boolean = true
    var selectedOnly: Boolean = false
    var selectedBodyId: String? = null

    private val maxVertsPerVector = 10
    private val maxTotalVerts = maxBodies * 2 * maxVertsPerVector
    private val uploadBuffer = FloatArray(maxTotalVerts * 4)
    private val scratchOrigin = FloatArray(3)

    private var vao: GlVertexArray? = null
    private var vbo: GlBuffer? = null

    fun init() {
        vao = GlVertexArray().apply { bind() }
        vbo = GlBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_DYNAMIC_DRAW)
        vbo?.bind()

        val empty = FloatArray(maxTotalVerts * 4)
        vbo?.uploadData(empty)

        val stride = 4 * 4 // 16 bytes

        // Attribute 0: Position (vec3)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        // Attribute 1: Alpha (float)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 3 * 4)

        vao?.unbind()
        vbo?.unbind()
    }

    private fun buildArrowGeometry(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        arrowLength: Float,
        buffer: FloatArray,
        startIndex: Int
    ): Int {
        var idx = startIndex
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-6f || arrowLength <= 1e-4f) return idx

        val nx = dx / len
        val ny = dy / len
        val nz = dz / len

        val tipX = ox + nx * arrowLength
        val tipY = oy + ny * arrowLength
        val tipZ = oz + nz * arrowLength

        // 1. Stem Line
        buffer[idx++] = ox; buffer[idx++] = oy; buffer[idx++] = oz; buffer[idx++] = 0.4f
        buffer[idx++] = tipX; buffer[idx++] = tipY; buffer[idx++] = tipZ; buffer[idx++] = 1.0f

        var px = -ny
        var py = nx
        var pz = 0.0f
        var pLen = sqrt(px * px + py * py + pz * pz)
        if (pLen < 1e-4f) {
            px = 0.0f
            py = -nz
            pz = ny
            pLen = sqrt(px * px + py * py + pz * pz)
        }
        px /= pLen; py /= pLen; pz /= pLen

        val qx = ny * pz - nz * py
        val qy = nz * px - nx * pz
        val qz = nx * py - ny * px

        val headSize = arrowLength * 0.22f
        val baseHeadX = tipX - nx * headSize
        val baseHeadY = tipY - ny * headSize
        val baseHeadZ = tipZ - nz * headSize

        val wingRadius = headSize * 0.45f

        // Fin 1 (+p)
        buffer[idx++] = tipX; buffer[idx++] = tipY; buffer[idx++] = tipZ; buffer[idx++] = 1.0f
        buffer[idx++] = baseHeadX + px * wingRadius
        buffer[idx++] = baseHeadY + py * wingRadius
        buffer[idx++] = baseHeadZ + pz * wingRadius
        buffer[idx++] = 0.8f

        // Fin 2 (-p)
        buffer[idx++] = tipX; buffer[idx++] = tipY; buffer[idx++] = tipZ; buffer[idx++] = 1.0f
        buffer[idx++] = baseHeadX - px * wingRadius
        buffer[idx++] = baseHeadY - py * wingRadius
        buffer[idx++] = baseHeadZ - pz * wingRadius
        buffer[idx++] = 0.8f

        // Fin 3 (+q)
        buffer[idx++] = tipX; buffer[idx++] = tipY; buffer[idx++] = tipZ; buffer[idx++] = 1.0f
        buffer[idx++] = baseHeadX + qx * wingRadius
        buffer[idx++] = baseHeadY + qy * wingRadius
        buffer[idx++] = baseHeadZ + qz * wingRadius
        buffer[idx++] = 0.8f

        // Fin 4 (-q)
        buffer[idx++] = tipX; buffer[idx++] = tipY; buffer[idx++] = tipZ; buffer[idx++] = 1.0f
        buffer[idx++] = baseHeadX - qx * wingRadius
        buffer[idx++] = baseHeadY - qy * wingRadius
        buffer[idx++] = baseHeadZ - qz * wingRadius
        buffer[idx++] = 0.8f

        return idx
    }

    fun draw(
        shader: ShaderProgram,
        bodies: List<BodyRenderState>,
        scaleManager: RenderScaleManager,
        cameraDistance: Float,
        bodyRenderPositions: FloatArray? = null,
        bodyRenderRadii: FloatArray? = null
    ) {
        if ((!showVelocityVectors && !showAccelerationVectors) || vao == null || vbo == null || bodies.isEmpty()) {
            return
        }

        val count = minOf(bodies.size, maxBodies)

        val targetIndices = ArrayList<Int>()
        if (selectedOnly && selectedBodyId != null) {
            val idx = bodies.indexOfFirst { it.id == selectedBodyId }
            if (idx in 0 until count) targetIndices.add(idx)
        } else {
            for (i in 0 until count) {
                if (bodies[i].isActive) targetIndices.add(i)
            }
        }

        if (targetIndices.isEmpty()) return

        vao?.bind()

        val baseArrowLength = (cameraDistance * 0.09f).coerceIn(2.0f, 15.0f)

        // 1. Draw Velocity Vectors (Cyan / Aqua)
        if (showVelocityVectors) {
            var outIdx = 0
            for (idx in targetIndices) {
                val b = bodies[idx]
                val speed = sqrt(b.velX * b.velX + b.velY * b.velY + b.velZ * b.velZ)
                if (speed < 1e-3) continue

                val ox: Float
                val oy: Float
                val oz: Float
                val bodyRadius: Float

                if (bodyRenderPositions != null && idx * 3 + 2 < bodyRenderPositions.size) {
                    ox = bodyRenderPositions[idx * 3]
                    oy = bodyRenderPositions[idx * 3 + 1]
                    oz = bodyRenderPositions[idx * 3 + 2]
                    bodyRadius = bodyRenderRadii?.getOrNull(idx) ?: 0.3f
                } else {
                    scaleManager.physicsToRenderPosition(b.posX, b.posY, b.posZ, scratchOrigin)
                    ox = scratchOrigin[0]
                    oy = scratchOrigin[1]
                    oz = scratchOrigin[2]
                    val isStar = b.type == SandboxBodyType.SUN || b.type == SandboxBodyType.BLACK_HOLE
                    bodyRadius = scaleManager.physicsToRenderRadius(b.radiusMeters, isStar)
                }

                val logSpeedFactor = (Math.log10(1.0 + speed) / 4.5).coerceIn(0.6, 2.2).toFloat()
                val arrowLen = baseArrowLength * logSpeedFactor + bodyRadius

                outIdx = buildArrowGeometry(
                    ox = ox, oy = oy, oz = oz,
                    dx = b.velX.toFloat(), dy = b.velY.toFloat(), dz = b.velZ.toFloat(),
                    arrowLength = arrowLen,
                    buffer = uploadBuffer,
                    startIndex = outIdx
                )
            }

            if (outIdx > 0) {
                vbo?.uploadSubData(uploadBuffer, 0, outIdx)
                shader.setUniform4f("u_VectorColor", 0.0f, 0.95f, 1.0f, 0.95f)
                GLES30.glLineWidth(2.5f)
                GLES30.glDrawArrays(GLES30.GL_LINES, 0, outIdx / 4)
            }
        }

        // 2. Draw Gravitational Acceleration Vectors (Amber / Orange)
        if (showAccelerationVectors) {
            var outIdx = 0
            for (idx in targetIndices) {
                val b = bodies[idx]
                var accX = b.accX
                var accY = b.accY
                var accZ = b.accZ
                var accMag = sqrt(accX * accX + accY * accY + accZ * accZ)

                // If physics worker has zero recorded acc (e.g. initial frame), compute instantaneous Newtonian sum
                if (accMag < 1e-12) {
                    var sumAx = 0.0
                    var sumAy = 0.0
                    var sumAz = 0.0
                    for (j in bodies.indices) {
                        if (j == idx || !bodies[j].isActive) continue
                        val bj = bodies[j]
                        val dx = bj.posX - b.posX
                        val dy = bj.posY - b.posY
                        val dz = bj.posZ - b.posZ
                        val distSq = (dx * dx + dy * dy + dz * dz).coerceAtLeast(1.0)
                        val dist = sqrt(distSq)
                        val forceMag = com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants.G * bj.massKg / distSq
                        sumAx += forceMag * (dx / dist)
                        sumAy += forceMag * (dy / dist)
                        sumAz += forceMag * (dz / dist)
                    }
                    accX = sumAx
                    accY = sumAy
                    accZ = sumAz
                    accMag = sqrt(accX * accX + accY * accY + accZ * accZ)
                }

                if (accMag < 1e-12) continue

                val ox: Float
                val oy: Float
                val oz: Float
                val bodyRadius: Float

                if (bodyRenderPositions != null && idx * 3 + 2 < bodyRenderPositions.size) {
                    ox = bodyRenderPositions[idx * 3]
                    oy = bodyRenderPositions[idx * 3 + 1]
                    oz = bodyRenderPositions[idx * 3 + 2]
                    bodyRadius = bodyRenderRadii?.getOrNull(idx) ?: 0.3f
                } else {
                    scaleManager.physicsToRenderPosition(b.posX, b.posY, b.posZ, scratchOrigin)
                    ox = scratchOrigin[0]
                    oy = scratchOrigin[1]
                    oz = scratchOrigin[2]
                    val isStar = b.type == SandboxBodyType.SUN || b.type == SandboxBodyType.BLACK_HOLE
                    bodyRadius = scaleManager.physicsToRenderRadius(b.radiusMeters, isStar)
                }

                val logAccFactor = (Math.log10(1.0 + accMag * 1e5) / 4.0).coerceIn(0.7, 2.5).toFloat()
                val arrowLen = baseArrowLength * 0.95f * logAccFactor + bodyRadius

                outIdx = buildArrowGeometry(
                    ox = ox, oy = oy, oz = oz,
                    dx = accX.toFloat(), dy = accY.toFloat(), dz = accZ.toFloat(),
                    arrowLength = arrowLen,
                    buffer = uploadBuffer,
                    startIndex = outIdx
                )
            }

            if (outIdx > 0) {
                vbo?.uploadSubData(uploadBuffer, 0, outIdx)
                shader.setUniform4f("u_VectorColor", 1.0f, 0.65f, 0.0f, 0.95f)
                GLES30.glLineWidth(2.5f)
                GLES30.glDrawArrays(GLES30.GL_LINES, 0, outIdx / 4)
            }
        }

        vao?.unbind()
    }

    fun destroy() {
        vao?.destroy()
        vbo?.destroy()
        vao = null
        vbo = null
    }
}
