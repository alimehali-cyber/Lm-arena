package com.alijafari.red.astronomy.ui.rendering

import android.graphics.Canvas
import android.graphics.Paint

/**
 * The only route from this app to [Canvas.drawVertices].
 *
 * **Why this exists.** `Canvas.drawVertices` validates the *lengths* of the arrays it is handed
 * (`BaseCanvas.checkRange`) and nothing about the *values* inside them. The values are copied
 * straight into a Skia `SkVertices` as `uint16_t`, and Skia's `VertState` walks them without a range
 * check against the vertex count, while `SkDraw::drawFixedVertices` allocates the per-vertex colour
 * buffer at exactly `vertexCount` entries. One index equal to `vertexCount` therefore reads past the
 * end of that buffer — inside native code, past the reach of a JVM `catch`, which on a device with
 * MTE enabled is a `SIGSEGV / SEGV_ACCERR` in `SkTriColorShader::update` and an instant process
 * death. There is no way to ask the platform to be careful, so the app checks its own data first and
 * simply does not make the call when the data is wrong.
 *
 * A mesh is immutable: [of] takes copies at construction, so the baking thread cannot hand a
 * half-written array to the draw pass, and the arrays a caller keeps cannot be mutated underneath a
 * baked sprite.
 *
 * Invariants enforced here, all of them in the JVM:
 *  - `positions.size` is even, at least 2, and the resulting vertex count is in `1..[MAX_VERTICES]`;
 *  - `colors`, when present, has one entry per vertex (Skia copies `vertexCount` ints — the Java
 *    side's looser `vertexCount / 2` check is not the limit that matters);
 *  - `indices` is a whole number of triangles for [MeshMode.TRIANGLES], and every index is in
 *    `[0, vertexCount)`; a triangle that is not is dropped, counted and reported, never drawn;
 *  - every vertex coordinate is finite. A NaN reaches Skia as a non-invertible matrix and undefined
 *    rasterisation, so a triangle touching one is dropped for the same reason as a bad index.
 */
internal enum class MeshMode(val minIndexCount: Int) {
    TRIANGLES(3),
    TRIANGLE_STRIP(3),
    TRIANGLE_FAN(3);

    internal val platform: Canvas.VertexMode
        get() = when (this) {
            TRIANGLES -> Canvas.VertexMode.TRIANGLES
            TRIANGLE_STRIP -> Canvas.VertexMode.TRIANGLE_STRIP
            TRIANGLE_FAN -> Canvas.VertexMode.TRIANGLE_FAN
        }
}

internal class VertexMesh private constructor(
    val label: String,
    val mode: MeshMode,
    private val positions: FloatArray,
    private val colors: IntArray?,
    private val indices: ShortArray,
    /** Triangles the validation pass threw away. Non-zero means the mesh data was wrong. */
    val droppedTriangles: Int,
    /** Vertices with a NaN or infinite coordinate. Non-zero means the geometry was wrong. */
    val nonFiniteVertices: Int
) {

    /** Vertices in the mesh: exactly what is reported to the platform as the vertex count. */
    val vertexCount: Int get() = positions.size / 2

    /** Indices that will actually be submitted (the sanitised ones). */
    val indexCount: Int get() = indices.size

    val isEmpty: Boolean get() = indexCount < mode.minIndexCount

    /** The largest index that survived validation; -1 when nothing did. */
    val maxIndex: Int
        get() {
            var m = -1
            for (i in indices.indices) {
                val v = indices[i].toInt()
                if (v > m) m = v
            }
            return m
        }

    /** A copy of the submitted coordinates: 2 floats (x, y) per vertex, already frozen. */
    internal fun positionsCopy(): FloatArray = positions.copyOf()

    /** A copy of the submitted colours, one packed ARGB int per vertex, or null. */
    internal fun colorsCopy(): IntArray? = colors?.copyOf()

    /** A copy of the submitted indices, every one of them already within `[0, vertexCount)`. */
    internal fun indicesCopy(): ShortArray = indices.copyOf()

    /**
     * Submits the mesh.
     *
     * The count passed to the platform is the vertex count (not the float count: the offset
     * overload takes vertices and reads `2 · vertexCount` floats), and the arrays are the frozen
     * copies — the same arrays that were validated. Nothing can change between validation and the
     * call.
     */
    fun draw(canvas: Canvas, paint: Paint) {
        if (isEmpty) return
        canvas.drawVertices(
            mode.platform,
            vertexCount, positions, 0,
            null, 0,
            colors, 0,
            indices, 0, indices.size,
            paint
        )
    }

    companion object {

        /**
         * Largest mesh this app will submit. Indices travel to Skia as unsigned 16-bit shorts and
         * the app stores them in a signed [ShortArray], so 32767 is the representable ceiling; a
         * larger grid has to be split rather than wrapped (`Short.toShort()` would turn 32768 into
         * −32768 and Skia would read it as an index far outside the mesh).
         */
        val MAX_VERTICES: Int = Short.MAX_VALUE.toInt()

        /**
         * Validates and freezes [positions] / [colors] / [indices].
         *
         * @return the drawable mesh, or null when nothing about this data can be drawn safely — in
         *   which case the caller skips the layer. Never throws for bad data.
         */
        fun of(
            label: String,
            mode: MeshMode,
            positions: FloatArray,
            colors: IntArray?,
            indices: ShortArray
        ): VertexMesh? {
            val frozenPositions = positions.copyOf()
            val frozenColors = colors?.copyOf()
            val frozenIndices = indices.copyOf()

            if (frozenPositions.size < 2 || frozenPositions.size % 2 != 0) {
                RenderDiagnostics.warnOnce(
                    "$label.positions",
                    "mesh not drawn: ${frozenPositions.size} floats is not a whole number of vertices"
                )
                return null
            }
            val vertexCount = frozenPositions.size / 2
            if (vertexCount > MAX_VERTICES) {
                RenderDiagnostics.warnOnce(
                    "$label.vertices",
                    "mesh not drawn: $vertexCount vertices exceeds the 16-bit index limit $MAX_VERTICES"
                )
                return null
            }
            if (frozenColors != null && frozenColors.size < vertexCount) {
                RenderDiagnostics.warnOnce(
                    "$label.colors",
                    "mesh not drawn: ${frozenColors.size} colours for $vertexCount vertices"
                )
                return null
            }

            // Non-finite geometry: one NaN coordinate poisons its whole row of triangles.
            val finite = BooleanArray(vertexCount)
            var nonFinite = 0
            for (v in 0 until vertexCount) {
                val x = frozenPositions[v * 2]
                val y = frozenPositions[v * 2 + 1]
                val ok = x.isFinite() && y.isFinite()
                finite[v] = ok
                if (!ok) nonFinite++
            }

            val keptValues = IntArray(frozenIndices.size)
            var kept = 0
            var dropped = 0
            var worst = -1
            if (mode == MeshMode.TRIANGLES) {
                var k = 0
                while (k + 2 < frozenIndices.size) {
                    val a = frozenIndices[k].toInt()
                    val b = frozenIndices[k + 1].toInt()
                    val c = frozenIndices[k + 2].toInt()
                    if (usable(a, vertexCount, finite) && usable(b, vertexCount, finite) &&
                        usable(c, vertexCount, finite)
                    ) {
                        keptValues[kept++] = a
                        keptValues[kept++] = b
                        keptValues[kept++] = c
                    } else {
                        dropped++
                        worst = maxOf(worst, a and 0xFFFF, b and 0xFFFF, c and 0xFFFF)
                    }
                    k += 3
                }
                // A trailing partial triangle is a data bug too, and gets the same treatment.
                if (k != frozenIndices.size) dropped++
            } else {
                var allUsable = frozenIndices.size >= mode.minIndexCount
                for (k in frozenIndices.indices) {
                    val v = frozenIndices[k].toInt()
                    if (!usable(v, vertexCount, finite)) {
                        allUsable = false
                        worst = maxOf(worst, v and 0xFFFF)
                        break
                    }
                }
                if (allUsable) {
                    for (k in frozenIndices.indices) keptValues[kept++] = frozenIndices[k].toInt()
                } else {
                    dropped = 1
                }
            }

            if (dropped > 0 || nonFinite > 0) {
                RenderDiagnostics.warnOnce(
                    "$label.dropped",
                    "dropped $dropped triangle(s): vertexCount=$vertexCount indexCount=${frozenIndices.size} " +
                        "maxBadIndex=$worst nonFiniteVertices=$nonFinite"
                )
            }

            val sanitised = ShortArray(kept)
            for (i in 0 until kept) sanitised[i] = keptValues[i].toShort()
            if (sanitised.size < mode.minIndexCount) {
                RenderDiagnostics.warnOnce(
                    "$label.empty",
                    "mesh not drawn: ${sanitised.size} usable of ${frozenIndices.size} indices " +
                        "(vertexCount=$vertexCount)"
                )
                return null
            }
            return VertexMesh(label, mode, frozenPositions, frozenColors, sanitised, dropped, nonFinite)
        }

        private fun usable(index: Int, vertexCount: Int, finite: BooleanArray): Boolean {
            // Indices travel as unsigned 16-bit; a negative Short is a value >= 32768, which is
            // outside any mesh this app builds, so it fails here exactly like any other overflow.
            if (index < 0 || index >= vertexCount) return false
            return finite[index]
        }

        /**
         * Vertices of the `(cols + 1) × rows` grid indexed as `v = i * rows + j`.
         *
         * Every reader of a grid mesh — the corridor and the water both — sizes its arrays with
         * this, so the count that is pushed and the count that is reported cannot drift apart.
         */
        fun gridVertexCount(cols: Int, rows: Int): Int = if (cols < 0 || rows < 0) 0 else (cols + 1) * rows

        /**
         * Two triangles per cell of that grid.
         *
         * The loops stop at `cols - 1` and `rows - 2` because a grid of `cols + 1` vertex rings and
         * `rows` cross-section nodes has `cols × (rows − 1)` cells: emitting a ring for `i = cols`
         * or reading node `j + 1 == rows` would reference a vertex that was never pushed. The
         * highest index this can produce is `(cols + 1) · rows − 1`, i.e. `vertexCount − 1`.
         */
        fun gridIndices(cols: Int, rows: Int): ShortArray {
            if (cols < 1 || rows < 2) return ShortArray(0)
            val vertexCount = gridVertexCount(cols, rows)
            if (vertexCount > MAX_VERTICES) {
                RenderDiagnostics.warnOnce(
                    "grid.${cols}x$rows",
                    "grid not built: $vertexCount vertices exceeds the 16-bit index limit $MAX_VERTICES"
                )
                return ShortArray(0)
            }
            val out = ShortArray(cols * (rows - 1) * 6)
            var k = 0
            for (i in 0 until cols) {
                val rowA = i * rows
                val rowB = rowA + rows
                for (j in 0 until rows - 1) {
                    val a = rowA + j
                    val b = rowB + j
                    val c = a + 1
                    val d = b + 1
                    out[k++] = a.toShort(); out[k++] = b.toShort(); out[k++] = c.toShort()
                    out[k++] = c.toShort(); out[k++] = b.toShort(); out[k++] = d.toShort()
                }
            }
            return out
        }
    }
}
