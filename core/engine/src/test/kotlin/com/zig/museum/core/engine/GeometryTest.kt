package com.zig.museum.core.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

/**
 * Regression test for the winding-order bug documented in
 * docs/investigation/SPACE_MUSEUM_DEBUG_INVESTIGATION.md (Task 2): the previous index order in
 * GeometryGenerator.generateEllipsoid() produced triangles whose right-hand-rule face normal
 * pointed INWARD (toward the ellipsoid center) on every sampled triangle, which Filament's
 * default CullingMode.BACK silently discarded, producing a blue/black "nothing renders" screen.
 *
 * This is the exact check used ad hoc during the investigation (face normal via cross product,
 * dotted against the outward radial direction from the ellipsoid center), promoted to a checked-in
 * JUnit test per Fix Prompt v2 item 3 ("turn that exact script into a checked-in test").
 */
class GeometryTest {

    private data class Vec3(val x: Float, val y: Float, val z: Float) {
        operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
        operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
        fun scale(s: Float) = Vec3(x * s, y * s, z * s)
        fun length(): Float = sqrt(x * x + y * y + z * z)
        fun normalized(): Vec3 {
            val len = length()
            return if (len > 1e-8f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 0f)
        }
    }

    private fun cross(a: Vec3, b: Vec3): Vec3 =
        Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

    private fun dot(a: Vec3, b: Vec3): Float = a.x * b.x + a.y * b.y + a.z * b.z

    /**
     * Walks every triangle of a generated mesh, computes its right-hand-rule face normal from
     * the raw vertex *positions* (independent of whatever normals are stored in the buffer, so
     * this test would still catch the bug even if per-vertex normals were wrong too), and checks
     * it points away from the ellipsoid center (outward).
     *
     * Degenerate triangles (zero area, which legitimately occur at the north/south pole rows of
     * this UV-sphere topology where multiple longitude vertices collapse to the same point) are
     * excluded from the pass/fail count but tallied for visibility.
     */
    private fun classifyTriangleWinding(mesh: Mesh): Triple<Int, Int, Int> {
        var outward = 0
        var inward = 0
        var degenerate = 0
        val verts = mesh.vertices
        var i = 0
        while (i < mesh.indices.size) {
            val i0 = mesh.indices[i]
            val i1 = mesh.indices[i + 1]
            val i2 = mesh.indices[i + 2]
            val a = Vec3(verts[i0].x, verts[i0].y, verts[i0].z)
            val b = Vec3(verts[i1].x, verts[i1].y, verts[i1].z)
            val c = Vec3(verts[i2].x, verts[i2].y, verts[i2].z)
            val faceNormal = cross(b - a, c - a)
            val centroid = (a + b + c).scale(1f / 3f)
            val centroidLen = centroid.length()
            if (centroidLen < 1e-6f || faceNormal.length() < 1e-10f) {
                degenerate++
            } else {
                val outwardDir = centroid.normalized()
                val d = dot(faceNormal, outwardDir)
                when {
                    d > 1e-9f -> outward++
                    d < -1e-9f -> inward++
                    else -> degenerate++
                }
            }
            i += 3
        }
        return Triple(outward, inward, degenerate)
    }

    @Test
    fun `sphere triangles wind outward at tier0 resolution`() {
        val mesh = GeometryGenerator.generateEllipsoid(latSegments = 128, lonSegments = 256, oblateness = 0.0, radius = 1.0f)
        val (outward, inward, degenerate) = classifyTriangleWinding(mesh)
        println("tier0 sphere: outward=$outward inward=$inward degenerate=$degenerate")
        assertEquals("No triangle may wind inward (this is the exact bug that caused the blue/black screen)", 0, inward)
        assertTrue("Expected the overwhelming majority of triangles to be classified outward", outward > 0)
    }

    @Test
    fun `all tier resolutions wind outward on a plain sphere`() {
        val tiers = listOf(0, 1, 2, 3)
        for (tier in tiers) {
            val (latSeg, lonSeg) = GeometryGenerator.tierSegments(tier)
            val mesh = GeometryGenerator.generateEllipsoid(latSeg, lonSeg, oblateness = 0.0, radius = 1.0f)
            val (outward, inward, degenerate) = classifyTriangleWinding(mesh)
            println("tier$tier ($latSeg x $lonSeg) sphere: outward=$outward inward=$inward degenerate=$degenerate")
            assertEquals("tier$tier must have zero inward-winding triangles", 0, inward)
        }
    }

    @Test
    fun `oblate ellipsoids also wind outward, including Saturn's real oblateness`() {
        // 0.0 = perfect sphere, 0.0649 ~= Earth's real oblateness, 0.09796 = Saturn's real
        // oblateness (the most extreme body in the museum's registry) — covers the "does the
        // winding fix behave correctly for non-spherical bodies" concern raised for this pass.
        val oblatenessValues = listOf(0.0, 0.0649, 0.09796)
        for (oblateness in oblatenessValues) {
            val mesh = GeometryGenerator.generateEllipsoid(latSegments = 64, lonSegments = 128, oblateness = oblateness, radius = 1.0f)
            val (outward, inward, degenerate) = classifyTriangleWinding(mesh)
            println("oblateness=$oblateness: outward=$outward inward=$inward degenerate=$degenerate")
            assertEquals("oblateness=$oblateness must have zero inward-winding triangles", 0, inward)
            assertTrue(outward > 0)
        }
    }

    @Test
    fun `per-vertex stored normal roughly agrees with outward radial direction away from poles`() {
        // Sanity check that the already-correct per-vertex normal computation (Task 2's
        // secondary finding: normals were computed correctly but never bound to a vertex
        // attribute) still points outward, independent of triangle winding.
        val mesh = GeometryGenerator.generateEllipsoid(latSegments = 64, lonSegments = 128, oblateness = 0.0, radius = 1.0f)
        var checked = 0
        for (v in mesh.vertices) {
            val pos = Vec3(v.x, v.y, v.z)
            val posLen = pos.length()
            if (posLen < 1e-6f) continue
            val outwardDir = pos.normalized()
            val storedNormal = Vec3(v.nx, v.ny, v.nz)
            val d = dot(storedNormal, outwardDir)
            assertTrue("Stored normal must point outward (dot=$d) at vertex ($v)", d > 0.99f)
            checked++
        }
        assertTrue(checked > 0)
    }

    @Test
    fun `buildTangentFrameQuaternion reconstructs the input normal after SNORM16 packing`() {
        // Verifies the GeometryGenerator.buildTangentFrameQuaternion() helper added for the
        // NORMAL/tangent vertex-attribute fix: pack a normal into the quaternion form Filament's
        // VertexAttribute.TANGENTS expects, then unpack with Filament's own GLSL formula
        // (toTangentFrame, transcribed here) and confirm we get the original normal back within
        // SNORM16 quantization tolerance.
        val testNormals = listOf(
            Triple(0f, 1f, 0f),
            Triple(0f, -1f, 0f),
            Triple(1f, 0f, 0f),
            Triple(0f, 0f, 1f),
            Triple(0.5773503f, 0.5773503f, 0.5773503f),
            Triple(-0.2672612f, 0.5345225f, 0.8017837f)
        )
        for ((nx, ny, nz) in testNormals) {
            val q = GeometryGenerator.buildTangentFrameQuaternion(nx, ny, nz)
            val unpacked = unpackSnorm16Quat(q)
            val reconstructedNormal = toTangentFrameNormal(unpacked)
            val dx = reconstructedNormal[0] - nx
            val dy = reconstructedNormal[1] - ny
            val dz = reconstructedNormal[2] - nz
            val err = sqrt(dx * dx + dy * dy + dz * dz)
            println("normal=($nx,$ny,$nz) reconstructed=(${reconstructedNormal[0]},${reconstructedNormal[1]},${reconstructedNormal[2]}) err=$err")
            assertTrue("Reconstruction error too large: $err", err < 0.01f)
        }
    }

    /** From-scratch transcription of Filament's SNORM16 unpack, used only to verify round-trip. */
    private fun unpackSnorm16Quat(q: ShortArray): FloatArray {
        fun unpack(v: Short): Float = (v / 32767f).coerceIn(-1f, 1f)
        return floatArrayOf(unpack(q[0]), unpack(q[1]), unpack(q[2]), unpack(q[3]))
    }

    /** From-scratch transcription of Filament's GLSL toTangentFrame() normal-extraction formula. */
    private fun toTangentFrameNormal(q: FloatArray): FloatArray {
        val x = q[0]; val y = q[1]; val z = q[2]; val w = q[3]
        val nx = 0f + (2f * x * z) + (-2f * y * w)
        val ny = 0f + (-2f * x * w) + (2f * y * z)
        val nz = 1f + (-2f * x * x) + (-2f * y * y)
        return floatArrayOf(nx, ny, nz)
    }
}
