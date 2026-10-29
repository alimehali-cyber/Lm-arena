package com.alijafari.red.astronomy.ui.rendering

import android.graphics.Bitmap
import android.graphics.LightingColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * **One Pulvar river, baked once, lit every frame.**
 *
 * [PulvarRiver] holds the supplied design's hydraulics, scatter and shading; this class runs that
 * shading over the design's own mesh resolutions (240 × 28 corridor, 320 × 18 water) into a single
 * bitmap, then blits it once per draw with the same per-channel light filter the two tree species
 * use. Nothing is rebuilt per frame: the whole river costs one image quad plus its glitter.
 *
 * Why a bake rather than per-frame geometry: this landscape is static by design — only the light
 * moves — and the design's fragment work (a four-octave fbm per vertex, the Beer–Lambert path, the
 * ripple field) is expensive enough that doing it per frame would spend the sky's budget on a
 * foreground strip. The bake also lets the water keep the design's *transparency*: its per-vertex
 * alpha composites over the bed mesh underneath, in the same pass, exactly as the GL depth buffer
 * did.
 *
 * Main-thread only, like [SugarMapleSprite.forLayout] and the landscape's vegetation cache.
 */
internal class PulvarRiverSprite private constructor(
    val frame: PulvarRiver.Frame,
    private val bitmap: Bitmap,
    /** Canvas y of the bitmap's first row. */
    private val topPx: Float,
    /** The water surface, in canvas coordinates — the clip for the live glitter. */
    private val waterClip: Path,
    /** Per-column water edges (canvas y) and reach foam, for placing the glitter. */
    private val farEdge: FloatArray,
    private val nearEdge: FloatArray,
    private val foamAt: FloatArray,
    private val speedAt: FloatArray
) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private var filterArgb: Int = Int.MIN_VALUE

    /**
     * Projects the bake onto the scene and lights it.
     *
     * @param tint 0xRRGGBB multiply for this frame's light — the same value the trees are given.
     * @param sunX glint column centre (canvas x) for this frame's Sun, or the Moon's.
     * @param glint 0..1 specular strength at that column.
     * @param glintRgb packed 0xRRGGBB glitter colour.
     */
    fun draw(
        drawScope: DrawScope,
        tint: Int,
        sunX: Float,
        glint: Float,
        glintRgb: Int,
        lightLevel: Float
    ) {
        if (filterArgb != tint) {
            paint.colorFilter = LightingColorFilter((tint and 0x00FFFFFF) or 0xFF000000.toInt(), 0)
            filterArgb = tint
        }
        paint.alpha = 255
        drawScope.drawIntoCanvas { canvas -> canvas.nativeCanvas.drawBitmap(bitmap, 0f, topPx, paint) }
        if (glint <= 0.01f) return
        glitter(drawScope, sunX, glint, glintRgb, lightLevel)
    }

    /** The design's Sun/Moon glitter: a Fresnel column of broken sparkles on the water. */
    private fun glitter(drawScope: DrawScope, sunX: Float, glint: Float, glintRgb: Int, lightLevel: Float) {
        val width = frame.widthPx
        val height = frame.heightPx
        val red = ((glintRgb shr 16) and 0xFF) / 255f
        val green = ((glintRgb shr 8) and 0xFF) / 255f
        val blue = (glintRgb and 0xFF) / 255f
        val columns = farEdge.size - 1
        // The broad Fresnel sheen under the sparkles: grazing water mirrors the sky in a band,
        // not in points. Two nested soft passes, as the old band did.
        drawScope.clipPath(waterClip) {
            val sheenColour = Color(red, green, blue, 1f)
            drawCircle(
                color = sheenColour.copy(alpha = 0.075f * glint),
                radius = width * 0.20f,
                center = Offset(sunX, frame.centerLineY)
            )
            drawCircle(
                color = sheenColour.copy(alpha = 0.085f * glint),
                radius = width * 0.10f,
                center = Offset(sunX, frame.centerLineY)
            )
            val lineWidth = (height * 0.0018f).coerceIn(1f, 3.5f)
            for (i in 0 until 96) {
                val fx = LandscapeProfile.hash01(137, i * 7 + 2)
                val x = width * (0.02f + 0.96f * fx)
                val dxS = (x - sunX) / (width * 0.17f)
                if (abs(dxS) > 2.6f) continue
                val glitter = LandscapeProfile.sunGlitter(dxS, 0f)
                val column = ((x / width).coerceIn(0f, 1f) * columns).toInt().coerceIn(0, columns)
                val foam = foamAt[column]
                // Riffles scatter the reflection; glassy pools hold it in one column.
                val density = 0.10f + 0.90f * glitter * (0.55f + 0.45f * speedAt[column])
                if (LandscapeProfile.hash01(193, i * 11 + 8) > density) continue
                if (LandscapeProfile.hash01(199, i * 13 + 5) < foam * 0.55f) continue
                val yFrac = 0.18f + 0.64f * LandscapeProfile.hash01(79, i * 5 + 3)
                val y = farEdge[column] + (nearEdge[column] - farEdge[column]) * yFrac
                val alpha = (glint * glitter * (0.22f + 0.78f * LandscapeProfile.hash01(31, i * 3 + 1)))
                    .coerceIn(0f, 0.94f) * (0.35f + 0.65f * lightLevel)
                if (alpha < 0.02f) continue
                val len = width * (0.004f + 0.014f * LandscapeProfile.hash01(57, i * 9 + 5)) *
                    (0.35f + 0.85f * glitter)
                drawLine(
                    color = Color(red, green, blue, alpha),
                    start = Offset(x - len * 0.5f, y),
                    end = Offset(x + len * 0.5f, y),
                    strokeWidth = lineWidth * (0.7f + 0.6f * glitter),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    companion object {

        private class Entry(
            val widthPx: Int,
            val heightPx: Int,
            val centerLineY: Int,
            val seasonMix: Int,
            val sunSign: Int,
            val sprite: PulvarRiverSprite
        )

        /** Two layouts; a rotation or resize evicts the oldest, never grows unbounded. */
        private val cache = ArrayList<Entry>(2)

        /**
         * The river for [frame], in this valley's season.
         *
         * @param seasonMix 0 = spring/summer greens, 1 = autumn/winter straw.
         * @param sunSign +1 when the scene's Sun is on the right of frame (the bake's light and
         *   every wet sheen then come from that side), −1 otherwise.
         */
        fun forLayout(frame: PulvarRiver.Frame, seasonMix: Float, sunSign: Float): PulvarRiverSprite {
            val wKey = ceil(frame.widthPx).toInt()
            val hKey = ceil(frame.heightPx).toInt()
            val yKey = frame.centerLineY.toInt()
            val sKey = if (seasonMix > 0.5f) 1 else 0
            val sunKey = if (sunSign >= 0f) 1 else 0
            for (entry in cache) {
                if (entry.widthPx == wKey && entry.heightPx == hKey && entry.centerLineY == yKey &&
                    entry.seasonMix == sKey && entry.sunSign == sunKey
                ) {
                    return entry.sprite
                }
            }
            val sprite = bake(frame, sKey == 1, sunSign)
            if (cache.size >= 2) cache.removeAt(0)
            cache.add(Entry(wKey, hKey, yKey, sKey, sunKey, sprite))
            return sprite
        }

        // ── The bake ────────────────────────────────────────────────────────────────────────

        /**
         * The design's corridor grid (28 lateral nodes, the floodplain clipped to this band),
         * subdivided once. The design gets its fine detail from a fragment shader; a bake has only
         * its vertices, so each of the design's cells is halved to keep the foam and the
         * waterline's grain instead of losing it to Gouraud interpolation.
         */
        private val Q_NODES = subdivide(
            floatArrayOf(
                -1.85f, -1.68f, -1.5f, -1.34f, -1.21f, -1.12f, -1.02f, -0.94f, -0.8f, -0.62f,
                -0.42f, -0.22f, 0f, 0.22f, 0.42f, 0.62f, 0.8f, 0.94f, 1.02f, 1.12f,
                1.21f, 1.34f, 1.44f, 1.54f, 1.62f
            )
        )

        /** The design's water grid (18 lateral nodes), likewise subdivided once. */
        private val T_NODES = subdivide(
            floatArrayOf(
                -1f, -0.93f, -0.84f, -0.73f, -0.60f, -0.46f, -0.32f, -0.18f, -0.06f,
                0.06f, 0.18f, 0.32f, 0.46f, 0.60f, 0.73f, 0.84f, 0.93f, 1f
            )
        )

        private const val TERRAIN_COLUMNS = 480
        private const val WATER_COLUMNS = 640

        /** Inserts a midpoint into every cell, keeping the design's own node placement. */
        private fun subdivide(nodes: FloatArray): FloatArray {
            val out = FloatArray(nodes.size * 2 - 1)
            for (i in nodes.indices) {
                out[i * 2] = nodes[i]
                if (i < nodes.size - 1) out[i * 2 + 1] = (nodes[i] + nodes[i + 1]) * 0.5f
            }
            return out
        }
        private const val EDGE_SAMPLES = 96

        private fun bake(frame: PulvarRiver.Frame, autumn: Boolean, sunSign: Float): PulvarRiverSprite {
            val width = ceil(frame.widthPx).toInt().coerceAtLeast(2)
            val top = floor(frame.corridorTopPx - 3f)
            val bottom = min(ceil(frame.corridorBottomPx + 3f), frame.heightPx)
            val height = (bottom - top).toInt().coerceAtLeast(2)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bitmap)
            canvas.translate(0f, -top)

            // The design's reference sun (its own defaults, mirrored to this scene's side), so a
            // bake is one look and the frame's lighting is a multiply on top of it.
            val sunDir = PulvarRiver.dirFromAzAlt(205f, 52f)
            if (sunSign < 0f) sunDir[0] = -sunDir[0]
            val light = PulvarRiver.Light(
                sunDir = sunDir,
                sunCol = PulvarRiver.sunColourFor(52f),
                moonDir = PulvarRiver.dirFromAzAlt(128f, 34f),
                moonCol = floatArrayOf(0f, 0f, 0f),
                skyAmb = floatArrayOf(0.30f, 0.42f, 0.58f),
                groundAmb = floatArrayOf(0.16f, 0.17f, 0.13f)
            )
            val seasonMix = if (autumn) 1f else 0f
            val scatter = PulvarRiver.scatter(seasonMix)

            drawCorridor(canvas, frame, light, seasonMix, scatter)
            drawCobbles(canvas, frame, light, scatter)
            drawRocks(canvas, frame, light, scatter)
            drawWater(canvas, frame, light, scatter)
            drawPlants(canvas, frame, light, scatter)

            val profile = waterProfile(frame)
            return PulvarRiverSprite(
                frame, bitmap, top, profile.clip, profile.far, profile.near, profile.foam, profile.speed
            )
        }

        /** The design's corridor mesh, from the bed under the water to the floodplain banks. */
        private fun drawCorridor(
            canvas: AndroidCanvas,
            frame: PulvarRiver.Frame,
            light: PulvarRiver.Light,
            seasonMix: Float,
            scatter: PulvarRiver.Scatter
        ) {
            val qn = Q_NODES.size
            val cols = TERRAIN_COLUMNS
            val vertexCount = (cols + 1) * qn
            val positions = FloatArray(vertexCount * 2)
            val colours = IntArray(vertexCount)
            val p = FloatArray(3)
            val probe = FloatArray(3)
            val albedo = FloatArray(3)
            var v = 0
            for (i in 0..cols) {
                val x = PulvarRiver.X0 + PulvarRiver.LEN * i / cols
                val half = PulvarRiver.halfWidth(x)
                val centerY = frame.centerYAtWorld(x)
                val wy = PulvarRiver.waterY(x)
                val cx = frame.canvasX(x)
                for (j in 0 until qn) {
                    val q = Q_NODES[j]
                    PulvarRiver.terrainPoint(x, q, p)
                    PulvarRiver.terrainColour(x, q, p, seasonMix, albedo)
                    val submerged = max(0f, wy - p[1])
                    val moisture = PulvarRiver.clamp(
                        1.12f - max(0f, p[1] - wy) * 1.05f - max(0f, abs(q) - 1f) * 0.16f, 0f, 1f
                    )
                    // Local normal of the cross-section: the bank rises and the bed dips.
                    val dq = 0.04f
                    // `probe`, not `p`: these two samples would otherwise leave the shaded point
                    // standing at q + dq.
                    val ya = PulvarRiver.terrainPointY(x, q - dq, probe)
                    val yb = PulvarRiver.terrainPointY(x, q + dq, probe)
                    val slope = (yb - ya) / (2f * dq * half)
                    val nx = 0f
                    val ny = 1f
                    val nz = -slope
                    val packed = PulvarRiver.shadeOpaque(
                        albedo[0], albedo[1], albedo[2], nx, ny, nz,
                        p[0], p[1], p[2], 0, moisture, submerged, light
                    )
                    // Fade the corridor's outer edge into the plain: a hard edge across the whole
                    // canvas would read as a cut-out strip, not a river in a valley.
                    val fadeFar = 1f - PulvarRiver.smooth(
                        PulvarRiver.FAR_EDGE_Q - 0.18f, PulvarRiver.FAR_EDGE_Q, -q
                    )
                    val fadeNear = 1f - PulvarRiver.smooth(
                        PulvarRiver.NEAR_EDGE_Q - 0.24f, PulvarRiver.NEAR_EDGE_Q, q
                    )
                    val alpha = (min(fadeFar, fadeNear) * 255f).toInt().coerceIn(0, 255)
                    colours[v] = (alpha shl 24) or (packed and 0x00FFFFFF)
                    positions[v * 2] = cx
                    positions[v * 2 + 1] = centerY + q * half * frame.pxPerMetreZ
                    v++
                }
            }
            val indices = ShortArray(cols * (qn - 1) * 6)
            var k = 0
            for (i in 0 until cols) {
                for (j in 0 until qn - 1) {
                    val a = (i * qn + j).toShort()
                    val b = ((i + 1) * qn + j).toShort()
                    val c = (i * qn + j + 1).toShort()
                    val d = ((i + 1) * qn + j + 1).toShort()
                    indices[k++] = a; indices[k++] = b; indices[k++] = c
                    indices[k++] = c; indices[k++] = b; indices[k++] = d
                }
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            // The offset form: the only drawVertices signature on every API level this app ships
            // to (the short one is API 34+), and the one that takes per-vertex alpha.
            canvas.drawVertices(
                AndroidCanvas.VertexMode.TRIANGLES,
                vertexCount, positions, 0,
                null, 0,
                colours, 0,
                indices, 0, indices.size,
                paint
            )
        }

        /** Cobbles on the bars, in the shallows and along the banks. */
        private fun drawCobbles(
            canvas: AndroidCanvas,
            frame: PulvarRiver.Frame,
            light: PulvarRiver.Light,
            scatter: PulvarRiver.Scatter
        ) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val albedo = floatArrayOf(0f, 0f, 0f)
            for (c in scatter.cobbles) {
                val q = (c.cz - PulvarRiver.centerZ(c.cx)) / max(PulvarRiver.halfWidth(c.cx), 0.1f)
                if (q < -PulvarRiver.FAR_EDGE_Q || q > PulvarRiver.NEAR_EDGE_Q) continue
                val submerged = max(0f, c.waterY - c.cy)
                val moisture = 1f - PulvarRiver.smooth(c.waterY - 0.03f, c.waterY + 0.22f, c.cy)
                PulvarRiver.mix3(PulvarRiver.BED_PEBBLE, PulvarRiver.GRAVEL, c.seed / 100f, albedo)
                if (c.seed / 100f > 0.78f) PulvarRiver.mixInto(albedo, PulvarRiver.BED_SAND, 0.5f)
                PulvarRiver.mixInto(albedo, PulvarRiver.ROCK_DARK, moisture * 0.5f)
                val packed = PulvarRiver.shadeOpaque(
                    albedo[0], albedo[1], albedo[2], 0.2f, 0.94f, 0.28f,
                    c.cx, c.cy, c.cz, 1, moisture, submerged, light
                )
                val cx = frame.canvasX(c.cx)
                val cy = frame.centerYAtWorld(c.cx) + q * PulvarRiver.halfWidth(c.cx) * frame.pxPerMetreZ
                val rx = max(0.6f, c.rx * frame.pxPerMetreX)
                val ry = max(0.5f, c.ry * frame.pxPerMetreY)
                paint.color = packed
                canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), paint)
            }
        }

        /**
         * Boulders: the design's displaced icospheres, projected to their silhouettes and filled
         * with a sun-side-to-wet-base gradient. At this band's size that is the whole read of a
         * boulder — facets smaller than a pixel would only shimmer.
         */
        private fun drawRocks(
            canvas: AndroidCanvas,
            frame: PulvarRiver.Frame,
            light: PulvarRiver.Light,
            scatter: PulvarRiver.Scatter
        ) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val scratch = FloatArray(3)
            val hullX = FloatArray(256)
            val hullY = FloatArray(256)
            val ptsX = FloatArray(256)
            val ptsY = FloatArray(256)
            for (r in scatter.rocks) {
                if (r.q < -PulvarRiver.FAR_EDGE_Q || r.q > PulvarRiver.NEAR_EDGE_Q) continue
                val sphere = Sphere.of(if (r.radius > 0.92f) 2 else 1)
                val cy = frame.centerYAtWorld(r.x)
                val centerCanvasY = cy + (r.cz - PulvarRiver.centerZ(r.x)) * frame.pxPerMetreZ
                val cYaw = cos(r.yaw)
                val sYaw = sin(r.yaw)
                var n = 0
                for (vi in sphere.verts.indices step 3) {
                    val dx = sphere.verts[vi]
                    val dy = sphere.verts[vi + 1]
                    val dz = sphere.verts[vi + 2]
                    // The design's displacement: faceted fbm plus a finer octave.
                    val d1 = PulvarRiver.fbm3(
                        dx * 1.25f + r.seed, dy * 1.25f + r.seed * 0.7f, dz * 1.25f - r.seed * 0.4f, 3
                    )
                    val facet = floor((d1 * 5f).toDouble()).toFloat() / 5f
                    val d2 = PulvarRiver.noise3(dx * 4.6f - r.seed, dy * 4.6f, dz * 4.6f + r.seed)
                    val rad = 0.69f + 0.38f * facet + 0.17f * d2
                    var lx = dx * rad * r.rx
                    var ly = dy * rad * r.ry
                    val lz = dz * rad * r.rz
                    lx += ly * ((r.seed % 1f) - 0.5f) * 0.22f
                    if (ly < -r.ry * 0.62f) ly = -r.ry * 0.62f
                    val wx = lx * cYaw - lz * sYaw
                    val wz = lx * sYaw + lz * cYaw
                    if (n < ptsX.size) {
                        ptsX[n] = frame.canvasX(r.x + wx)
                        ptsY[n] = centerCanvasY + wz * frame.pxPerMetreZ - (ly + r.ry) * frame.pxPerMetreY
                        n++
                    }
                }
                if (n < 3) continue
                val hull = convexHull(ptsX, ptsY, n, hullX, hullY)
                if (hull < 3) continue
                val path = AndroidPath()
                path.moveTo(hullX[0], hullY[0])
                for (i in 1 until hull) path.lineTo(hullX[i], hullY[i])
                path.close()
                // Light: the design's rock palette, sun side lit, the base dark and wet.
                val moisture = 1f - PulvarRiver.smooth(r.waterY - 0.04f, r.waterY + 0.40f, r.cy)
                val submerged = max(0f, r.waterY - r.cy)
                // Per-stone tone from the design's stream, then the design's moss on the crown
                // of stones that stand above the water: at ten pixels a stone is its colour.
                PulvarRiver.mix3(PulvarRiver.ROCK_MID, PulvarRiver.ROCK_LIGHT, 0.22f + 0.62f * (r.seed % 1f), scratch)
                PulvarRiver.mixInto(scratch, PulvarRiver.ROCK_DARK, 0.18f * (1f - (r.seed % 1f)))
                if (r.cy + r.ry > r.waterY + 0.05f) {
                    PulvarRiver.mixInto(scratch, PulvarRiver.ROCK_MOSS, 0.22f * r.seed % 1f)
                }
                val lit = PulvarRiver.shadeOpaque(
                    scratch[0], scratch[1], scratch[2], -0.45f * sunSign, 0.82f, 0.35f,
                    r.cx, r.cy + r.ry * 0.6f, r.cz, 1, moisture * 0.4f, submerged, light
                )
                val dark = PulvarRiver.shadeOpaque(
                    scratch[0] * 0.62f, scratch[1] * 0.62f, scratch[2] * 0.62f, 0.5f * sunSign, 0.25f, 0.3f,
                    r.cx, r.cy - r.ry * 0.5f, r.cz, 1, moisture, submerged, light
                )
                var minY = hullY[0]
                var maxY = hullY[0]
                for (i in 1 until hull) {
                    if (hullY[i] < minY) minY = hullY[i]
                    if (hullY[i] > maxY) maxY = hullY[i]
                }
                paint.shader = LinearGradient(
                    0f, minY, 0f, max(maxY, minY + 1f),
                    if (sunSign >= 0f) lit else dark, if (sunSign >= 0f) dark else lit,
                    Shader.TileMode.CLAMP
                )
                canvas.drawPath(path, paint)
                paint.shader = null
            }
        }

        /**
         * The water surface: the design's `320 × 18` mesh with per-vertex colour *and* alpha, so
         * the bed baked underneath shows through by exactly the transmittance the design computes.
         */
        private fun drawWater(
            canvas: AndroidCanvas,
            frame: PulvarRiver.Frame,
            light: PulvarRiver.Light,
            scatter: PulvarRiver.Scatter
        ) {
            val tn = T_NODES.size
            val cols = WATER_COLUMNS
            val vertexCount = (cols + 1) * tn
            val positions = FloatArray(vertexCount * 2)
            val colours = IntArray(vertexCount)
            val p = FloatArray(3)
            val wake = FloatArray(2)
            val grad = FloatArray(2)
            var v = 0
            for (i in 0..cols) {
                val x = PulvarRiver.X0 + PulvarRiver.LEN * i / cols
                val reach = PulvarRiver.reachAt(x)
                val half = PulvarRiver.halfWidth(x)
                val centerY = frame.centerYAtWorld(x)
                val cx = frame.canvasX(x)
                for (j in 0 until tn) {
                    val t = T_NODES[j]
                    val lateral = t * half
                    val m = PulvarRiver.centerDzDx(x)
                    val inv = 1f / sqrt(1f + m * m)
                    val wx = x - m * inv * lateral
                    val wz = PulvarRiver.centerZ(x) + inv * lateral
                    val bed = PulvarRiver.bedHeight(x, t)
                    val depth = max(0.012f, reach.waterY - bed)
                    PulvarRiver.rockWake(x, t, wx, wz, scatter.rocks, wake)
                    val u = x * 1.015f + PulvarRiver.centerZ(x) * 0.07f
                    val edgeEnv = max(0f, sin(((t + 1f) * PulvarRiver.PI_F * 0.5f).toDouble()).toFloat()).pow(0.65f)
                    val amp = (0.010f + 0.082f * reach.foam + 0.105f * wake[1]) * edgeEnv
                    val phase = u * 2.35f - abs(lateral) * 0.48f +
                        1.7f * (PulvarRiver.noise2(u * 0.11f, lateral * 0.11f) - 0.5f)
                    val crest = PulvarRiver.clamp(
                        (sin(phase.toDouble()).toFloat() - 0.25f) * 1.4f, 0f, 1f
                    )
                    var foam = reach.foam * (0.08f + 0.25f * crest + 0.12f * PulvarRiver.surfaceSpeed01(x, t))
                    foam += (1f - PulvarRiver.smooth(0.03f, 0.30f, depth)) * 0.09f
                    // The design's boulder collar is a *ring* around a stone seen in 3D; flattened
                    // into this band the same ring spans the whole cross-section, which painted a
                    // white bar across each riffle. Half strength keeps the bow wave and the
                    // downstream wakes and loses the bar.
                    foam += wake[0] * 0.34f
                    foam *= 0.72f + 0.28f * PulvarRiver.fbm2(u * 0.20f + 3f, lateral * 0.20f, 2)
                    val speed = PulvarRiver.surfaceSpeed01(x, t)
                    val turb = PulvarRiver.clamp(
                        reach.foam * 1.15f + speed * 0.35f + wake[0] * 0.20f, 0f, 1f
                    )
                    // View elevation across the band: grazing at the far bank, steeper near —
                    // the design's camera sweep, which is what gives its water its vertical
                    // Fresnel grade instead of one flat sheen.
                    val viewY = PulvarRiver.lerp(0.12f, 0.60f, (t + 1f) * 0.5f)
                    colours[v] = PulvarRiver.shadeWater(
                        u, lateral, depth, PulvarRiver.clamp(foam, 0f, 1f), speed, turb, viewY, grad, light
                    )
                    positions[v * 2] = cx
                    positions[v * 2 + 1] = centerY + lateral * frame.pxPerMetreZ
                    v++
                }
            }
            val indices = ShortArray(cols * (tn - 1) * 6)
            var k = 0
            for (i in 0 until cols) {
                for (j in 0 until tn - 1) {
                    val a = (i * tn + j).toShort()
                    val b = ((i + 1) * tn + j).toShort()
                    val c = (i * tn + j + 1).toShort()
                    val d = ((i + 1) * tn + j + 1).toShort()
                    indices[k++] = a; indices[k++] = b; indices[k++] = c
                    indices[k++] = c; indices[k++] = b; indices[k++] = d
                }
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            // The offset form: the only drawVertices signature on every API level this app ships
            // to (the short one is API 34+), and the one that takes per-vertex alpha.
            canvas.drawVertices(
                AndroidCanvas.VertexMode.TRIANGLES,
                vertexCount, positions, 0,
                null, 0,
                colours, 0,
                indices, 0, indices.size,
                paint
            )
        }

        /** Reeds on the waterline and grass over the banks, in eight tone buckets. */
        private fun drawPlants(
            canvas: AndroidCanvas,
            frame: PulvarRiver.Frame,
            light: PulvarRiver.Light,
            scatter: PulvarRiver.Scatter
        ) {
            val buckets = Array(8) { AndroidPath() }
            val bucketColour = IntArray(8)
            val bucketCount = IntArray(8)
            for (clump in scatter.reeds) {
                for (b in clump) addBlade(buckets, bucketColour, bucketCount, frame, light, b, PulvarRiver.REED_BASE, PulvarRiver.REED_TIP, 2)
            }
            for (b in scatter.grasses) addBlade(buckets, bucketColour, bucketCount, frame, light, b, PulvarRiver.GRASS_DEEP, if (b.tipDry) PulvarRiver.GRASS_DRY else PulvarRiver.GRASS_LUSH, 2)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            paint.style = Paint.Style.FILL
            for (i in 0 until 8) {
                if (bucketCount[i] == 0) continue
                paint.color = bucketColour[i]
                canvas.drawPath(buckets[i], paint)
            }
            // Reed seed heads, the design's two crossed plumes at the tip: two tiny ovals each.
            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            for (clump in scatter.reeds) {
                for (b in clump) {
                    if (!b.seedHead) continue
                    val cx = frame.canvasX(b.x)
                    val hw = PulvarRiver.halfWidth(b.x)
                    val q = (b.z - PulvarRiver.centerZ(b.x)) / max(hw, 0.1f)
                    if (q < -PulvarRiver.FAR_EDGE_Q || q > PulvarRiver.NEAR_EDGE_Q) continue
                    val baseY = frame.centerYAtWorld(b.x) + q * hw * frame.pxPerMetreZ
                    val tipY = baseY - b.height * frame.pxPerMetreY
                    val tipX = cx + cos(b.yaw + PI / 2.0).toFloat() * b.lean * frame.pxPerMetreX
                    headPaint.color = PulvarRiver.shadeOpaque(
                        PulvarRiver.REED_SEED[0], PulvarRiver.REED_SEED[1], PulvarRiver.REED_SEED[2],
                        0f, 0.9f, 0.3f, b.x, b.y + b.height, b.z, 2, b.moisture, 0f, light
                    )
                    val w = max(0.7f, b.seedSize * 0.16f * frame.pxPerMetreX)
                    val h = max(1.2f, b.seedSize * frame.pxPerMetreY)
                    canvas.drawOval(RectF(tipX - w, tipY - h, tipX + w, tipY), headPaint)
                }
            }
        }

        /** One tapered blade into its tone bucket. */
        private fun addBlade(
            buckets: Array<AndroidPath>,
            bucketColour: IntArray,
            bucketCount: IntArray,
            frame: PulvarRiver.Frame,
            light: PulvarRiver.Light,
            b: PulvarRiver.Blade,
            colBase: FloatArray,
            colTip: FloatArray,
            mat: Int
        ) {
            val hw = PulvarRiver.halfWidth(b.x)
            val q = (b.z - PulvarRiver.centerZ(b.x)) / max(hw, 0.1f)
            if (q < -PulvarRiver.FAR_EDGE_Q || q > PulvarRiver.NEAR_EDGE_Q) return
            val baseX = frame.canvasX(b.x)
            val baseY = frame.centerYAtWorld(b.x) + q * hw * frame.pxPerMetreZ
            val submerged = 0f
            val t = 0.45f
            val ar = PulvarRiver.lerp(colBase[0], colTip[0], t)
            val ag = PulvarRiver.lerp(colBase[1], colTip[1], t)
            val ab = PulvarRiver.lerp(colBase[2], colTip[2], t)
            val packed = PulvarRiver.shadeOpaque(
                ar, ag, ab, 0f, 0.9f, 0.3f, b.x, b.y + b.height * 0.5f, b.z, mat, b.moisture, submerged, light
            )
            // Eight tone buckets keep the blade count irrelevant: one fill each.
            val lum = ((packed shr 16 and 0xFF) * 3 + (packed shr 8 and 0xFF) * 5 + (packed and 0xFF) * 2) / 10
            val bucket = (lum * 8 / 256).coerceIn(0, 7)
            if (bucketCount[bucket] == 0) bucketColour[bucket] = packed
            bucketCount[bucket]++
            val path = buckets[bucket]
            val heightPx = b.height * frame.pxPerMetreY
            if (heightPx < 0.9f) {
                // Sub-pixel grass: a dot of its own tone, so a tuft still reads as a tuft.
                path.addCircle(baseX, baseY - heightPx * 0.5f, max(0.45f, b.width * frame.pxPerMetreX), AndroidPath.Direction.CW)
                return
            }
            val widthPx = max(0.55f, b.width * frame.pxPerMetreX)
            val leanPx = b.lean * b.height * frame.pxPerMetreX
            val segs = 3
            var prevLx = 0f
            var prevLy = 0f
            var prevRx = 0f
            var prevRy = 0f
            for (i in 0..segs) {
                val f = i.toFloat() / segs
                val bend = f * f
                val cx = baseX + leanPx * bend
                val cy = baseY - heightPx * f + heightPx * 0.16f * bend * abs(b.lean)
                val half = widthPx * (1f - f * 0.88f) * 0.5f
                val lx = cx - half
                val rx = cx + half
                if (i > 0) {
                    path.moveTo(prevLx, prevLy)
                    path.lineTo(prevRx, prevRy)
                    path.lineTo(rx, cy)
                    path.lineTo(lx, cy)
                    path.close()
                }
                prevLx = lx
                prevLy = cy
                prevRx = rx
                prevRy = cy
            }
        }

        /**
         * Per-column water edges, foam and speed, in canvas coordinates — what the live glitter
         * needs to place sparkles inside the water and to know how broken the surface is.
         */
        private fun waterProfile(frame: PulvarRiver.Frame): WaterProfile {
            val n = EDGE_SAMPLES
            val far = FloatArray(n + 1)
            val near = FloatArray(n + 1)
            val foam = FloatArray(n + 1)
            val speed = FloatArray(n + 1)
            for (i in 0..n) {
                val x = PulvarRiver.X0 + PulvarRiver.LEN * i / n
                val cy = frame.centerYAtWorld(x)
                val halfPx = PulvarRiver.halfWidth(x) * frame.pxPerMetreZ
                far[i] = cy - halfPx
                near[i] = cy + halfPx
                foam[i] = PulvarRiver.reachAt(x).foam
                speed[i] = PulvarRiver.surfaceSpeed01(x, 0f)
            }
            val path = Path()
            path.moveTo(frame.canvasX(PulvarRiver.X0), far[0])
            for (i in 1..n) path.lineTo(frame.canvasX(PulvarRiver.X0 + PulvarRiver.LEN * i / n), far[i])
            for (i in n downTo 0) path.lineTo(frame.canvasX(PulvarRiver.X0 + PulvarRiver.LEN * i / n), near[i])
            path.close()
            return WaterProfile(path, far, near, foam, speed)
        }

        private class WaterProfile(
            val clip: Path,
            val far: FloatArray,
            val near: FloatArray,
            val foam: FloatArray,
            val speed: FloatArray
        )

        // ── Geometry helpers ────────────────────────────────────────────────────────────────

        /** The design's icosphere, cached per level: `verts` are flat xyz triples. */
        private class Sphere(val verts: FloatArray) {

            companion object {
                private val cache = arrayOfNulls<Sphere>(4)

                fun of(level: Int): Sphere {
                    val l = level.coerceIn(0, 3)
                    cache[l]?.let { return it }
                    val sphere = build(l)
                    cache[l] = sphere
                    return sphere
                }

                private fun build(level: Int): Sphere {
                    var verts = ArrayList<FloatArray>(642)
                    verts.add(floatArrayOf(1f, 0f, 0f)); verts.add(floatArrayOf(-1f, 0f, 0f))
                    verts.add(floatArrayOf(0f, 1f, 0f)); verts.add(floatArrayOf(0f, -1f, 0f))
                    verts.add(floatArrayOf(0f, 0f, 1f)); verts.add(floatArrayOf(0f, 0f, -1f))
                    var faces = ArrayList<IntArray>(1280)
                    faces.add(intArrayOf(0, 2, 4)); faces.add(intArrayOf(4, 2, 1))
                    faces.add(intArrayOf(1, 2, 5)); faces.add(intArrayOf(5, 2, 0))
                    faces.add(intArrayOf(4, 3, 0)); faces.add(intArrayOf(1, 3, 4))
                    faces.add(intArrayOf(5, 3, 1)); faces.add(intArrayOf(0, 3, 5))
                    for (s in 0 until level) {
                        val midCache = HashMap<Long, Int>()
                        fun mid(a: Int, b: Int): Int {
                            val key = if (a < b) a * 100000L + b else b * 100000L + a
                            midCache[key]?.let { return it }
                            val va = verts[a]
                            val vb = verts[b]
                            var x = (va[0] + vb[0]) * 0.5f
                            var y = (va[1] + vb[1]) * 0.5f
                            var z = (va[2] + vb[2]) * 0.5f
                            val l = sqrt(x * x + y * y + z * z)
                            if (l > 1e-9f) {
                                x /= l; y /= l; z /= l
                            }
                            verts.add(floatArrayOf(x, y, z))
                            val id = verts.size - 1
                            midCache[key] = id
                            return id
                        }
                        val next = ArrayList<IntArray>(faces.size * 4)
                        for (f in faces) {
                            val ab = mid(f[0], f[1])
                            val bc = mid(f[1], f[2])
                            val ca = mid(f[2], f[0])
                            next.add(intArrayOf(f[0], ab, ca))
                            next.add(intArrayOf(ab, f[1], bc))
                            next.add(intArrayOf(ca, bc, f[2]))
                            next.add(intArrayOf(ab, bc, ca))
                        }
                        faces = next
                    }
                    val flat = FloatArray(verts.size * 3)
                    for (i in verts.indices) {
                        flat[i * 3] = verts[i][0]
                        flat[i * 3 + 1] = verts[i][1]
                        flat[i * 3 + 2] = verts[i][2]
                    }
                    return Sphere(flat)
                }
            }
        }

        /** Monotone-chain hull; writes into [outX]/[outY] and returns the vertex count. */
        private fun convexHull(
            xs: FloatArray, ys: FloatArray, n: Int, outX: FloatArray, outY: FloatArray
        ): Int {
            if (n < 3) return 0
            // Insertion-by-x sort of indices (n is small: ≤ 162).
            val idx = IntArray(n) { it }
            var i = 1
            while (i < n) {
                val key = idx[i]
                val kx = xs[key]
                var j = i - 1
                while (j >= 0 && xs[idx[j]] > kx) {
                    idx[j + 1] = idx[j]
                    j--
                }
                idx[j + 1] = key
                i++
            }
            var h = 0
            for (k in 0 until n) {
                val id = idx[k]
                while (h >= 2 && cross(outX, outY, h - 2, h - 1, xs[id], ys[id]) <= 0f) h--
                outX[h] = xs[id]
                outY[h] = ys[id]
                h++
            }
            var lower = h + 1
            for (k in n - 2 downTo 0) {
                val id = idx[k]
                while (h >= lower && cross(outX, outY, h - 2, h - 1, xs[id], ys[id]) <= 0f) h--
                outX[h] = xs[id]
                outY[h] = ys[id]
                h++
            }
            return h - 1
        }

        private fun cross(px: FloatArray, py: FloatArray, a: Int, b: Int, x: Float, y: Float): Float =
            (px[b] - px[a]) * (y - py[a]) - (py[b] - py[a]) * (x - px[a])
    }
}
