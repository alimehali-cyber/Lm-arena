package com.zig.museum.core.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometry generation per §9.2
 * Base geometry: UV sphere or ellipsoid generated in code (not glTF asset)
 * Segment counts per tier: 256x128 (tier0), 192x96 (tier1), 128x64 (tier2), 96x48 (tier3)
 * Equirectangular UVs must match texture convention exactly; unit-test UV of known lat/lon
 * Oblateness: apply flattening ratio to vertical axis before any displacement
 * Poles: pole-aware triangulation or document artifact; if distortion unacceptable at ceiling zoom, use cube-sphere, recorded in DECISIONS.md
 * Unit-radius normalisation per §5.3: object's mean radius maps to 1.0 scene unit, all object-local coords in object radii
 */

data class Vertex(
    val x: Float,
    val y: Float,
    val z: Float,
    val nx: Float,
    val ny: Float,
    val nz: Float,
    val u: Float,
    val v: Float
)

data class Mesh(
    val vertices: List<Vertex>,
    val indices: List<Int>
)

object GeometryGenerator {

    fun generateEllipsoid(
        latSegments: Int = 128,
        lonSegments: Int = 256,
        oblateness: Double = 0.0, // (a-b)/a
        radius: Float = 1.0f // scene radius 1.0 per §5.3 unit-radius normalisation
    ): Mesh {
        val vertices = mutableListOf<Vertex>()
        val indices = mutableListOf<Int>()

        // Oblateness: apply flattening to vertical axis (Y)
        val flattening = oblateness.toFloat()
        val polarScale = 1.0f - flattening

        for (lat in 0..latSegments) {
            val theta = lat * PI / latSegments // 0 to PI
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)

            for (lon in 0..lonSegments) {
                val phi = lon * 2.0 * PI / lonSegments // 0 to 2PI
                val sinPhi = sin(phi)
                val cosPhi = cos(phi)

                // Unit sphere
                var x = (sinTheta * cosPhi).toFloat() * radius
                var y = cosTheta.toFloat() * radius
                var z = (sinTheta * sinPhi).toFloat() * radius

                // Apply oblateness to Y
                y *= polarScale

                // Normal: for ellipsoid, normal is not same as position if oblate
                // For sphere, normal = normalized position, for oblate, need to adjust
                // Simplified: compute normal as normalized (x, y/(polarScale^2), z) then normalize
                val nxRaw = x
                val nyRaw = if (polarScale != 0f) y / (polarScale * polarScale) else y
                val nzRaw = z
                val len = sqrt((nxRaw*nxRaw + nyRaw*nyRaw + nzRaw*nzRaw).toDouble()).toFloat()
                val nx = if (len > 1e-6f) nxRaw/len else 0f
                val ny = if (len > 1e-6f) nyRaw/len else 1f
                val nz = if (len > 1e-6f) nzRaw/len else 0f

                // Equirectangular UVs: u = lon / lonSegments, v = lat / latSegments
                // Convention: y increasing upward in equirectangular per §6.4, so v = 1 - lat/latSegments? Let's use standard
                val u = lon.toFloat() / lonSegments.toFloat()
                val v = lat.toFloat() / latSegments.toFloat()

                vertices.add(Vertex(x, y, z, nx, ny, nz, u, v))
            }
        }

        // Indices
        //
        // Winding fix (Fix Prompt v2, authorized change 1 of 2):
        // The previous winding (first, second, first+1 / second, second+1, first+1) produced
        // triangles whose right-hand-rule face normal pointed INWARD (toward the ellipsoid
        // center) on 896/896 sampled triangles — see docs/investigation/
        // SPACE_MUSEUM_DEBUG_INVESTIGATION.md Task 2. Filament's default RasterState culls
        // CullingMode.BACK using the OpenGL/glTF convention that CCW-as-seen-from-outside is
        // front-facing (Filament Materials Guide "Rasterization: culling", default = back).
        // Swapping the last two indices of each triangle reverses the winding without changing
        // which vertices are used, so the outward-facing side becomes the front face.
        for (lat in 0 until latSegments) {
            for (lon in 0 until lonSegments) {
                val first = lat * (lonSegments + 1) + lon
                val second = first + lonSegments + 1

                // Two triangles per quad, wound so the outward-facing side is CCW (front-facing
                // under Filament/OpenGL's default front-face convention).
                indices.add(first)
                indices.add(first + 1)
                indices.add(second)

                indices.add(second)
                indices.add(first + 1)
                indices.add(second + 1)
            }
        }

        return Mesh(vertices, indices)
    }

    fun uvForLatLon(latDeg: Double, lonDeg: Double): Pair<Float, Float> {
        // Unit test: UV of known lat/lon must match texture convention exactly per §9.2
        // lat: -90 to 90, lon: -180 to 180 or 0 to 360
        // Convert to u,v: u = (lon + 180)/360 or lon/360 if 0-360, v = (90 - lat)/180 or lat mapped
        // Using equirectangular: u = lon/360 (0-360), v = lat/180 (0-1) with y increasing upward per §6.4
        // For simplicity: lon 0-360 => u = lon/360, lat -90 to 90 => v = (lat+90)/180
        // But need to document convention in manifest
        val lonNorm = ((lonDeg % 360.0) + 360.0) % 360.0 // 0-360
        val u = (lonNorm / 360.0).toFloat()
        val v = ((latDeg + 90.0) / 180.0).toFloat()
        return Pair(u, v)
    }

    fun tierSegments(tier: Int): Pair<Int, Int> {
        // Per §9.2: 256x128 tier0, 192x96 tier1, 128x64 tier2, 96x48 tier3
        return when (tier) {
            0 -> Pair(128, 256)
            1 -> Pair(96, 192)
            2 -> Pair(64, 128)
            3 -> Pair(48, 96)
            else -> Pair(64, 128)
        }
    }

    /**
     * Packs a per-vertex surface normal into the quaternion form Filament's
     * `VertexAttribute.TANGENTS` expects (see `filament::math::mat3f::packTangentFrame` /
     * `geometry::SurfaceOrientation`, and the `.mat` files under
     * `core/engine/src/main/materials`, which already
     * declare `requires: [uv0, position, tangents]`). Filament has no plain FLOAT3 "NORMAL"
     * vertex attribute for the standard shading path — surface orientation is always supplied as
     * a normalized tangent-bitangent-normal (TBN) frame packed into a single SNORM16 quaternion.
     *
     * The tangent is reconstructed from the equirectangular UV parameterization used by
     * [generateEllipsoid] (derivative of position with respect to longitude, i.e. the
     * "eastward" direction: position = (sinTheta*cosPhi, cosTheta, sinTheta*sinPhi), so
     * d/dPhi is proportional to (-z, 0, x) up to a positive scale removed by normalization),
     * Gram-Schmidt-orthogonalized against the supplied normal. The bitangent is
     * cross(normal, tangent) so the packed quaternion's rotation of +Z / +X reproduces
     * (normal, tangent) exactly, matching Filament's own GLSL
     * `toTangentFrame(quat, out n[, out t])` unpacking.
     *
     * Pure function with no Filament/Android dependency, so it is directly unit-testable on the
     * JVM (see GeometryTest). Returns a ShortArray of length 4 (x, y, z, w) already scaled to
     * SNORM16 range, ready to be written into a SHORT4 TANGENTS buffer built with
     * `.normalized(TANGENTS)`.
     */
    fun buildTangentFrameQuaternion(nx: Float, ny: Float, nz: Float): ShortArray {
        // Normalize the input normal defensively (mesh generator already emits unit normals).
        val nLen = sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
        val n0: Float; val n1: Float; val n2: Float
        if (nLen > 1e-8f) { n0 = nx / nLen; n1 = ny / nLen; n2 = nz / nLen } else { n0 = 0f; n1 = 1f; n2 = 0f }

        var tx = -n2
        var ty = 0f
        var tz = n0
        var tLen = sqrt((tx * tx + ty * ty + tz * tz).toDouble()).toFloat()
        if (tLen < 1e-6f) {
            // Degenerate at the poles (normal ~ +/-Y): fall back to world +X as tangent.
            tx = 1f; ty = 0f; tz = 0f
            tLen = 1f
        }
        tx /= tLen; ty /= tLen; tz /= tLen

        // Gram-Schmidt orthogonalize tangent against normal, then re-normalize.
        val dotNT = n0 * tx + n1 * ty + n2 * tz
        tx -= dotNT * n0; ty -= dotNT * n1; tz -= dotNT * n2
        tLen = sqrt((tx * tx + ty * ty + tz * tz).toDouble()).toFloat()
        if (tLen > 1e-8f) { tx /= tLen; ty /= tLen; tz /= tLen } else { tx = 1f; ty = 0f; tz = 0f }

        // Bitangent = cross(normal, tangent) so that (T, B, N) columns form a right-handed,
        // reflection-free basis — the convention Filament's packTangentFrame assumes when no
        // explicit handedness/reflection is being encoded.
        val bx = n1 * tz - n2 * ty
        val by = n2 * tx - n0 * tz
        val bz = n0 * ty - n1 * tx

        // Build the 3x3 rotation matrix [T | B | N] (columns), convert to quaternion using the
        // standard Shepperd method, matching filament::math::details::TMat33::toQuaternion().
        val m00 = tx; val m01 = bx; val m02 = n0
        val m10 = ty; val m11 = by; val m12 = n1
        val m20 = tz; val m21 = bz; val m22 = n2
        val trace = m00 + m11 + m22

        var qx: Float; var qy: Float; var qz: Float; var qw: Float
        if (trace > 0f) {
            val s = sqrt((trace + 1f).toDouble()).toFloat() * 2f
            qw = 0.25f * s
            qx = (m21 - m12) / s
            qy = (m02 - m20) / s
            qz = (m10 - m01) / s
        } else if (m00 > m11 && m00 > m22) {
            val s = sqrt((1f + m00 - m11 - m22).toDouble()).toFloat() * 2f
            qw = (m21 - m12) / s
            qx = 0.25f * s
            qy = (m01 + m10) / s
            qz = (m02 + m20) / s
        } else if (m11 > m22) {
            val s = sqrt((1f + m11 - m00 - m22).toDouble()).toFloat() * 2f
            qw = (m02 - m20) / s
            qx = (m01 + m10) / s
            qy = 0.25f * s
            qz = (m12 + m21) / s
        } else {
            val s = sqrt((1f + m22 - m00 - m11).toDouble()).toFloat() * 2f
            qw = (m10 - m01) / s
            qx = (m02 + m20) / s
            qy = (m12 + m21) / s
            qz = 0.25f * s
        }

        // Normalize (guards against tiny drift from the branch selection above).
        var qLen = sqrt((qx * qx + qy * qy + qz * qz + qw * qw).toDouble()).toFloat()
        if (qLen < 1e-8f) { qx = 0f; qy = 0f; qz = 0f; qw = 1f; qLen = 1f }
        qx /= qLen; qy /= qLen; qz /= qLen; qw /= qLen

        // Ensure w is positive (Filament's packTangentFrame convention — a unit quaternion and
        // its negation represent the same rotation, so this is a free choice that keeps values
        // in the more numerically stable half of SNORM16 range).
        if (qw < 0f) { qx = -qx; qy = -qy; qz = -qz; qw = -qw }

        // Ensure w is never exactly 0 so the sign bit survives SNORM16 quantization/reflection
        // encoding, mirroring filament::math::details::TMat33::packTangentFrame's bias step.
        val bias = 1f / 32767f
        if (qw < bias) {
            qw = bias
            val factor = sqrt(1.0 - bias.toDouble() * bias.toDouble()).toFloat()
            qx *= factor; qy *= factor; qz *= factor
        }

        fun packSnorm16(f: Float): Short {
            val clamped = f.coerceIn(-1f, 1f)
            return Math.round(clamped * 32767f).toShort()
        }

        return shortArrayOf(packSnorm16(qx), packSnorm16(qy), packSnorm16(qz), packSnorm16(qw))
    }
}
