package com.alijafari.red.astronomy.ui.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gate in front of `Canvas.drawVertices`, tested without a device.
 *
 * `Canvas.drawVertices` copies the index array into Skia verbatim and Skia indexes its per-vertex
 * colour buffer with those values without ever comparing them to the vertex count, so a single
 * index equal to the vertex count is a native out-of-bounds read (the tombstone on Android 15 with
 * MTE: `SIGSEGV / SEGV_ACCERR` in `SkTriColorShader::update`). These tests pin the three things that
 * make that unreachable from this app:
 *
 *  1. [VertexMesh.gridIndices] — the generator both river meshes use — never emits an index outside
 *     `[0, vertexCount)` for any grid size, so the data is right to begin with;
 *  2. [VertexMesh.of] drops any triangle that is out of range anyway, and reports it, so bad data
 *     costs a triangle instead of the process;
 *  3. the mesh is immutable, so a baking thread cannot leave a half-written array in the draw path.
 */
class VertexMeshTest {

    private fun positionsFor(vertexCount: Int): FloatArray =
        FloatArray(vertexCount * 2) { i -> (i % 41 - 20).toFloat() }

    private fun colorsFor(vertexCount: Int): IntArray = IntArray(vertexCount) { 0xFF336699.toInt() }

    private fun meshOf(cols: Int, rows: Int, indices: ShortArray? = null): VertexMesh? {
        val vertexCount = VertexMesh.gridVertexCount(cols, rows)
        return VertexMesh.of(
            "test.${cols}x$rows", MeshMode.TRIANGLES, positionsFor(vertexCount), colorsFor(vertexCount),
            indices ?: VertexMesh.gridIndices(cols, rows)
        )
    }

    @Test
    fun `grid indices are inside the grid for every size the river uses and every edge size`() {
        // Grids whose vertex count is exactly 0, 1, 2, 3, 4, 5, 255, 256, 257 and 300, plus the
        // shapes that straddle a cell boundary. `256` is the size the tombstone's arithmetic lands
        // on (4108 = 256·16 + 12), which is why it is called out here.
        val shapes = listOf(
            0 to 0, 0 to 1, 1 to 1, 2 to 1, 1 to 2, 0 to 5, 1 to 3,
            84 to 3, 63 to 4, 0 to 257, 99 to 3, 12 to 2
        )
        val seenVertexCounts = HashSet<Int>()
        for ((cols, rows) in shapes) {
            val vertexCount = VertexMesh.gridVertexCount(cols, rows)
            seenVertexCounts.add(vertexCount)
            val indices = VertexMesh.gridIndices(cols, rows)
            val cells = if (cols < 1 || rows < 2) 0 else cols * (rows - 1)
            assertEquals("cells for ${cols}x$rows", cells * 6, indices.size)
            assertEquals("whole triangles for ${cols}x$rows", 0, indices.size % 3)
            for (i in indices.indices) {
                val v = indices[i].toInt()
                assertTrue(
                    "index $v of ${cols}x$rows outside 0 until $vertexCount",
                    v >= 0 && v < vertexCount
                )
            }
            if (cells > 0) {
                // The far corner of the last cell is always referenced, so a grid that fits uses
                // its whole vertex range — no silent truncation of the last ring either.
                assertEquals(vertexCount - 1, indices.maxOrNull()!!.toInt())
            }
        }
        assertEquals(
            "the sizes this test claims to cover",
            setOf(0, 1, 2, 3, 4, 5, 6, 26, 255, 256, 257, 300),
            seenVertexCounts
        )
    }

    @Test
    fun `the shipped river grids fit the 16-bit index limit`() {
        // The corridor is (480 + 1) × 49 vertices, the water (640 + 1) × 35 — both well inside
        // Short.MAX_VALUE, so no index wraps to a negative short and no mesh is refused.
        val corridor = VertexMesh.gridVertexCount(480, 49)
        val water = VertexMesh.gridVertexCount(640, 35)
        assertEquals(23569, corridor)
        assertEquals(22435, water)
        assertTrue(corridor <= VertexMesh.MAX_VERTICES)
        assertTrue(water <= VertexMesh.MAX_VERTICES)
        assertEquals(corridor - 1, VertexMesh.gridIndices(480, 49).maxOrNull()!!.toInt())
        assertEquals(water - 1, VertexMesh.gridIndices(640, 35).maxOrNull()!!.toInt())
    }

    @Test
    fun `a mesh larger than the index space is refused instead of wrapping`() {
        // 32768 vertices: one past Short.MAX_VALUE. Writing that index into a ShortArray yields
        // -32768, which Skia reads as 32768 — a guaranteed out-of-bounds read.
        assertNull(meshOf(0, VertexMesh.MAX_VERTICES + 1))
        assertNull(meshOf(0, VertexMesh.MAX_VERTICES))
        // A grid that would be too large produces no indices at all, never a wrapped short.
        assertEquals(0, VertexMesh.gridIndices(60000, 2).size)
    }

    @Test
    fun `a triangle that points one past the last vertex is dropped, not drawn`() {
        // Exactly the tombstone's shape: a 256-vertex corridor mesh with one triangle whose third
        // index is 256. Pre-fix this array went straight into Canvas.drawVertices.
        val cols = 63
        val rows = 4
        val vertexCount = VertexMesh.gridVertexCount(cols, rows)
        assertEquals(256, vertexCount)
        val bad = VertexMesh.gridIndices(cols, rows).copyOf()
        bad[2] = vertexCount.toShort()

        // The rest of the mesh is still drawable.
        val mesh = requireNotNull(meshOf(cols, rows, bad))
        assertEquals(1, mesh.droppedTriangles)
        val kept = mesh.indicesCopy()
        assertEquals(bad.size - 3, kept.size)
        for (i in kept.indices) assertTrue(kept[i].toInt() in 0 until vertexCount)
        assertEquals(vertexCount - 1, mesh.maxIndex)
    }

    @Test
    fun `every index outside the mesh is dropped, including negative shorts`() {
        val cols = 10
        val rows = 4
        val vertexCount = VertexMesh.gridVertexCount(cols, rows)
        val bad = VertexMesh.gridIndices(cols, rows).copyOf()
        bad[0] = vertexCount.toShort()          // one past the end
        bad[1] = (-1).toShort()                 // reads as 65535 unsigned
        bad[2] = Short.MIN_VALUE                // reads as 32768 unsigned
        val mesh = requireNotNull(meshOf(cols, rows, bad))
        assertEquals(1, mesh.droppedTriangles)
        val kept = mesh.indicesCopy()
        for (i in kept.indices) assertTrue(kept[i].toInt() in 0 until vertexCount)

        // Nothing usable left: the gate refuses the mesh entirely rather than drawing nothing
        // through the native call.
        val allBad = ShortArray(9) { vertexCount.toShort() }
        assertNull(meshOf(cols, rows, allBad))
    }

    @Test
    fun `a non-finite vertex costs the triangles that touch it, not the whole mesh`() {
        val cols = 8
        val rows = 3
        val vertexCount = VertexMesh.gridVertexCount(cols, rows)
        val positions = positionsFor(vertexCount)
        val poisoned = 5
        positions[poisoned * 2] = Float.NaN
        val indices = VertexMesh.gridIndices(cols, rows)
        var touching = 0
        var k = 0
        while (k + 2 < indices.size) {
            if (indices[k].toInt() == poisoned || indices[k + 1].toInt() == poisoned ||
                indices[k + 2].toInt() == poisoned
            ) {
                touching++
            }
            k += 3
        }
        assertTrue("the poisoned vertex must be referenced", touching > 0)
        val mesh = requireNotNull(
            VertexMesh.of("test.nan", MeshMode.TRIANGLES, positions, colorsFor(vertexCount), indices)
        )
        assertEquals(1, mesh.nonFiniteVertices)
        assertEquals(touching, mesh.droppedTriangles)
        assertEquals(vertexCount, mesh.vertexCount)

        // Every vertex poisoned: there is nothing to draw.
        val allNaN = FloatArray(vertexCount * 2) { Float.POSITIVE_INFINITY }
        assertNull(VertexMesh.of("test.nan-all", MeshMode.TRIANGLES, allNaN, colorsFor(vertexCount), indices))
    }

    @Test
    fun `malformed arrays are refused before they reach the platform`() {
        val positions = positionsFor(4)
        val indices = VertexMesh.gridIndices(1, 2)
        // Odd float count: Skia would divide it in half and read a truncated vertex.
        assertNull(VertexMesh.of("t.odd", MeshMode.TRIANGLES, FloatArray(7), null, indices))
        // Empty geometry.
        assertNull(VertexMesh.of("t.empty", MeshMode.TRIANGLES, FloatArray(0), null, indices))
        // Fewer colours than vertices: Skia copies vertexCount ints whatever the Java check allows.
        assertNull(VertexMesh.of("t.colors", MeshMode.TRIANGLES, positions, IntArray(3), indices))
        // No colours at all is legal (a flat-filled mesh).
        assertNotNull(VertexMesh.of("t.nocolors", MeshMode.TRIANGLES, positions, null, indices))
        // Fewer than three indices cannot form a triangle.
        assertNull(VertexMesh.of("t.short", MeshMode.TRIANGLES, positions, null, ShortArray(2)))
        // A trailing partial triangle is reported and dropped, the rest survives.
        val partial = ShortArray(indices.size + 1)
        for (i in indices.indices) partial[i] = indices[i]
        val mesh = requireNotNull(VertexMesh.of("t.partial", MeshMode.TRIANGLES, positions, null, partial))
        assertEquals(1, mesh.droppedTriangles)
        assertEquals(indices.size, mesh.indexCount)
    }

    @Test
    fun `strips and fans are checked against their own minimum`() {
        val positions = positionsFor(3)
        val strip = shortArrayOf(0, 1, 2, 2, 1, 0)
        val fan = shortArrayOf(0, 1, 2)
        assertNotNull(VertexMesh.of("t.strip", MeshMode.TRIANGLE_STRIP, positions, null, strip))
        assertNotNull(VertexMesh.of("t.fan", MeshMode.TRIANGLE_FAN, positions, null, fan))
        // One bad index in a strip cannot be repaired by dropping a triangle, so the mesh goes.
        val badStrip = shortArrayOf(0, 1, 3, 3, 1, 0)
        assertNull(VertexMesh.of("t.stripbad", MeshMode.TRIANGLE_STRIP, positions, null, badStrip))
        assertNull(VertexMesh.of("t.fanbad", MeshMode.TRIANGLE_FAN, positions, null, shortArrayOf(0, 1, 7)))
    }

    @Test
    fun `a mesh is frozen against the arrays it was built from`() {
        val vertexCount = VertexMesh.gridVertexCount(4, 3)
        val positions = positionsFor(vertexCount)
        val colors = colorsFor(vertexCount)
        val indices = VertexMesh.gridIndices(4, 3)
        val mesh = VertexMesh.of("t.frozen", MeshMode.TRIANGLES, positions, colors, indices)!!
        val beforePositions = mesh.positionsCopy()
        val beforeColors = mesh.colorsCopy()!!
        val beforeIndices = mesh.indicesCopy()

        // A baking thread (or a caller reusing its scratch arrays) mutating after construction.
        for (i in positions.indices) positions[i] = Float.NaN
        for (i in colors.indices) colors[i] = 0
        for (i in indices.indices) indices[i] = 32000

        assertTrue(beforePositions.contentEquals(mesh.positionsCopy()))
        assertTrue(beforeColors.contentEquals(mesh.colorsCopy()!!))
        assertTrue(beforeIndices.contentEquals(mesh.indicesCopy()))
        assertTrue(mesh.positionsCopy().all { it.isFinite() })
        assertTrue(mesh.maxIndex < mesh.vertexCount)
    }
}
