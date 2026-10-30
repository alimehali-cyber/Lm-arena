package com.alijafari.red.astronomy.ui.mountain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** CPU-side mesh contracts; no EGL context, device or screenshot is required. */
class MountainGeometryTest {
    @Test
    fun primitiveArraysGrowWithoutLosingVerticesOrUploadingCapacityPadding() {
        val mesh = MountainMeshBuilder()
        val color = floatArrayOf(0.2f, 0.4f, 0.6f)
        val data = floatArrayOf(2f, 0.7f, 0.1f, 0.3f)
        // Exceeds the initial capacity of every attribute/index array and signed shorts.
        val vertexCount = 40_000
        repeat(vertexCount) { i ->
            assertEquals(i, mesh.vertex(i.toFloat(), -i.toFloat(), i * 0.5f, 0f, 1f, 0f, color, data))
            if (i >= 2) mesh.tri(i - 2, i - 1, i)
        }
        assertEquals(vertexCount, mesh.vcount)
        assertEquals(vertexCount - 2, mesh.tcount)
        val pos = mesh.posArray()
        val nor = mesh.norArray()
        val col = mesh.colArray()
        val dat = mesh.datArray()
        val idx = mesh.idxArray()
        assertEquals(vertexCount * 3, pos.size)
        assertEquals(pos.size, nor.size)
        assertEquals(pos.size, col.size)
        assertEquals(vertexCount * 4, dat.size)
        assertEquals((vertexCount - 2) * 3, idx.size)
        repeat(vertexCount) { i ->
            assertEquals(i.toFloat(), pos[i * 3], 0f)
            assertEquals(-i.toFloat(), pos[i * 3 + 1], 0f)
            assertEquals(i * 0.5f, pos[i * 3 + 2], 0f)
            assertEquals(0f, nor[i * 3], 0f)
            assertEquals(1f, nor[i * 3 + 1], 0f)
            assertEquals(0f, nor[i * 3 + 2], 0f)
            for (k in color.indices) assertEquals(color[k], col[i * 3 + k], 0f)
            for (k in data.indices) assertEquals(data[k], dat[i * 4 + k], 0f)
        }
        for (t in 0 until mesh.tcount) for (corner in 0..2) {
            val index = idx[t * 3 + corner]
            assertEquals(t + corner, index)
            assertEquals("16-bit upload preserves unsigned index", index, index.toShort().toInt() and 0xffff)
        }
    }

    @Test
    fun optimizedMeshStaysWithinTheMobileBudgetAndUnsignedShortIndices() {
        // Previously 135,471 vertices / 252,160 triangles (560 x 220 terrain).
        assertEquals(MountainCounts(220, 0, 5, 75_051, 132_880), geometry.counts)
        assertEquals(geometry.counts.verts, geometry.builders.sumOf { it.vcount })
        assertEquals(geometry.counts.tris, geometry.builders.sumOf { it.tcount })
        for (mesh in geometry.builders) {
            assertTrue("every chunk fits unsigned shorts", mesh.vcount in 1..65_535)
            assertEquals(mesh.vcount * 3, mesh.posArray().size)
            assertEquals(mesh.vcount * 3, mesh.norArray().size)
            assertEquals(mesh.vcount * 3, mesh.colArray().size)
            assertEquals(mesh.vcount * 4, mesh.datArray().size)
            assertEquals(mesh.tcount * 3, mesh.idxArray().size)
            assertTrue("all indices reference real vertices", mesh.idxArray().all { it in 0 until mesh.vcount })
            for (array in arrayOf(mesh.posArray(), mesh.norArray(), mesh.colArray(), mesh.datArray())) {
                assertTrue("no NaN/infinity reaches the GPU", array.all { it.isFinite() })
            }
        }
    }

    @Test
    fun terrainBandsMeetWithoutPositionNormalOrMaterialSeams() {
        val terrain = geometry.builders.take(3)
        val rows = 161
        assertEquals(listOf(134, 134, 135), terrain.map { it.vcount / rows })
        for (band in 0..1) {
            val left = terrain[band]
            val right = terrain[band + 1]
            val leftWidth = left.vcount / rows
            val rightWidth = right.vcount / rows
            val leftArrays = arrayOf(left.posArray(), left.norArray(), left.colArray(), left.datArray())
            val rightArrays = arrayOf(right.posArray(), right.norArray(), right.colArray(), right.datArray())
            for (row in 0 until rows) {
                val a = (row + 1) * leftWidth - 1
                val b = row * rightWidth
                for (attribute in leftArrays.indices) {
                    val stride = if (attribute == 3) 4 else 3
                    for (component in 0 until stride) {
                        assertEquals(
                            "band $band, row $row, attribute $attribute/$component",
                            leftArrays[attribute][a * stride + component],
                            rightArrays[attribute][b * stride + component],
                            0f,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun massifKeepsItsWorldSizeAndNormalizedTerrainNormals() {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (mesh in geometry.builders.take(3)) {
            val pos = mesh.posArray()
            val nor = mesh.norArray()
            for (i in 0 until mesh.vcount) {
                minX = minOf(minX, pos[i * 3])
                maxX = maxOf(maxX, pos[i * 3])
                maxY = maxOf(maxY, pos[i * 3 + 1])
                val nx = nor[i * 3]; val ny = nor[i * 3 + 1]; val nz = nor[i * 3 + 2]
                assertEquals(1f, nx * nx + ny * ny + nz * nz, 0.0001f)
            }
        }
        assertEquals(-2400f, minX, 0f)
        assertEquals(2400f, maxX, 0f)
        assertTrue("crest remains about 1,000 m high ($maxY)", maxY in 970f..1100f)
    }

    @Test
    fun rebuildingForANewContextProducesTheSameGeometry() {
        val rebuilt = buildMountainScene()
        assertEquals(geometry.counts, rebuilt.counts)
        for (i in geometry.builders.indices) {
            val a = geometry.builders[i]
            val b = rebuilt.builders[i]
            assertArrayEquals(a.posArray(), b.posArray(), 0f)
            assertArrayEquals(a.norArray(), b.norArray(), 0f)
            assertArrayEquals(a.colArray(), b.colArray(), 0f)
            assertArrayEquals(a.datArray(), b.datArray(), 0f)
            assertArrayEquals(a.idxArray(), b.idxArray())
        }
    }

    private companion object {
        val geometry by lazy { buildMountainScene() }
    }
}
