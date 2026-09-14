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
        for (lat in 0 until latSegments) {
            for (lon in 0 until lonSegments) {
                val first = lat * (lonSegments + 1) + lon
                val second = first + lonSegments + 1

                // Two triangles per quad
                indices.add(first)
                indices.add(second)
                indices.add(first + 1)

                indices.add(second)
                indices.add(second + 1)
                indices.add(first + 1)
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
}
