package com.alijafari.red.astronomy.ui.rendering

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The river's own meshes, taken from the real bake.
 *
 * [PulvarRiverSprite.bake] builds the corridor and the water as indexed triangle grids and hands
 * them to `Canvas.drawVertices`. That call is where the app died with
 * `SIGSEGV / SEGV_ACCERR` in `SkTriColorShader::update`: Skia indexes its per-vertex colour buffer
 * with the mesh's own indices and never checks them against the vertex count, so one index equal to
 * the vertex count is a native read past the end of the buffer (the tombstone's
 * `4108 = 256 · 16 + 12` is element 256 of a 256-entry buffer).
 *
 * These tests install [PulvarRiverSprite.meshSink] and run the production bake for a set of layouts,
 * then assert on the exact meshes it produced: whole triangles, every index inside the vertex
 * count, a full-length colour array, finite coordinates, and no dropped triangles — because a
 * dropped triangle means the data was wrong and the gate had to save it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PulvarRiverMeshTest {

    private val captured = ArrayList<VertexMesh>()

    @Before
    fun installSink() {
        captured.clear()
        PulvarRiverSprite.meshSink = { mesh -> captured.add(mesh) }
    }

    @After
    fun removeSink() {
        PulvarRiverSprite.meshSink = null
    }

    private fun bake(widthPx: Float, heightPx: Float, seasonMix: Float = 0f, sunSign: Float = 1f) {
        val frame = PulvarRiver.Frame(widthPx, heightPx, heightPx * 0.93f)
        PulvarRiverSprite.forLayout(frame, seasonMix, sunSign)
    }

    private fun assertClean(mesh: VertexMesh) {
        assertTrue("${mesh.label}: no vertices", mesh.vertexCount > 0)
        assertEquals("${mesh.label}: whole triangles", 0, mesh.indexCount % 3)
        assertTrue("${mesh.label}: at least one triangle", mesh.indexCount >= 3)
        assertEquals("${mesh.label}: unexpected dropped triangles", 0, mesh.droppedTriangles)
        assertEquals("${mesh.label}: non-finite vertices", 0, mesh.nonFiniteVertices)

        val positions = mesh.positionsCopy()
        assertEquals("${mesh.label}: two floats per vertex", mesh.vertexCount * 2, positions.size)
        assertTrue("${mesh.label}: coordinates must be finite", positions.all { it.isFinite() })
        assertEquals("${mesh.label}: one colour per vertex", mesh.vertexCount, mesh.colorsCopy()?.size)

        val indices = mesh.indicesCopy()
        for (i in indices.indices) {
            val v = indices[i].toInt()
            assertTrue("${mesh.label}: index $v outside 0 until ${mesh.vertexCount}", v in 0 until mesh.vertexCount)
        }
        // Every cell is emitted, so the far corner of the grid is always referenced: the mesh uses
        // its whole vertex range and nothing past it.
        assertEquals("${mesh.label}: largest index", mesh.vertexCount - 1, mesh.maxIndex)
    }

    @Test
    fun `the home layout bakes a corridor and a water mesh that are in range`() {
        bake(1080f, 1260f)
        assertEquals("corridor + water", 2, captured.size)
        val corridor = captured.first { it.label == "pulvar.corridor" }
        val water = captured.first { it.label == "pulvar.water" }
        // The design's grids, subdivided once: 481 rings × 49 nodes, 641 rings × 35 nodes.
        assertEquals(23569, corridor.vertexCount)
        assertEquals(22435, water.vertexCount)
        assertClean(corridor)
        assertClean(water)
    }

    @Test
    fun `extreme layouts and both seasons stay in range`() {
        // Narrow, wide, tall and tiny: the frame scales the mesh but must never change its shape.
        bake(320f, 2400f)
        bake(2400f, 320f)
        bake(1080f, 1260f, seasonMix = 1f, sunSign = -1f)
        bake(64f, 64f)
        assertEquals(8, captured.size)
        for (mesh in captured) assertClean(mesh)
    }

    @Test
    fun `a zero-sized layout bakes without throwing and stays in range`() {
        // A first draw before layout measures anything. The frame collapses every coordinate onto
        // x = 0 (canvasX multiplies by widthPx and divides nothing), so the mesh is degenerate — but
        // it must still be a legal mesh, and nothing in the bake may throw. The scene additionally
        // skips the river entirely below 2 px (SceneGroundRenderer), which is the cheaper guard.
        bake(0f, 0f)
        for (mesh in captured) {
            assertTrue("degenerate mesh must still be whole", mesh.indexCount % 3 == 0)
            val positions = mesh.positionsCopy()
            assertTrue("coordinates must be finite", positions.all { it.isFinite() })
            assertEquals(mesh.vertexCount, mesh.colorsCopy()?.size)
            val indices = mesh.indicesCopy()
            for (i in indices.indices) assertTrue(indices[i].toInt() in 0 until mesh.vertexCount)
        }
    }

    @Test
    fun `the mesh the gate would have refused is the one that used to crash`() {
        // Sanity check on the arithmetic behind the fix: 256 vertices, one triangle pointing at
        // vertex 256, byte offset 256·16 + 12 = 4108 — the tombstone's fault address minus the base.
        val cols = 63
        val rows = 4
        val vertexCount = VertexMesh.gridVertexCount(cols, rows)
        assertEquals(256, vertexCount)
        assertEquals(4108, vertexCount * 16 + 12)

        val indices = VertexMesh.gridIndices(cols, rows).copyOf()
        indices[2] = vertexCount.toShort()
        val positions = FloatArray(vertexCount * 2) { it.toFloat() }
        val mesh = requireNotNull(
            VertexMesh.of("test.tombstone", MeshMode.TRIANGLES, positions, IntArray(vertexCount), indices)
        )
        assertEquals(1, mesh.droppedTriangles)
        assertTrue(mesh.maxIndex < mesh.vertexCount)

        // And with nothing salvageable, the gate refuses rather than calling the platform.
        assertNull(
            VertexMesh.of(
                "test.tombstone-all", MeshMode.TRIANGLES, positions, IntArray(vertexCount),
                ShortArray(3) { vertexCount.toShort() }
            )
        )
    }
}
