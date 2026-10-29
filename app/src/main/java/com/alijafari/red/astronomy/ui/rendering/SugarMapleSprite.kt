package com.alijafari.red.astronomy.ui.rendering

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * **One maple, rendered once, projected everywhere** — the second species to use this pattern.
 *
 * [SugarMapleTree] grows the supplied `SugarMaple.kt` design and places its organs; this class
 * rasterises that tree — once per look (green / autumn) and once per display size — into a small
 * bitmap, and every planting in the valley is one mirrored, tinted blit of it. Thirty-five maples
 * cost thirty-five image quads and not one vertex of per-tree geometry, which is what keeps the
 * scene's frame budget free for the sky above it.
 *
 * The painter order is the design's camera: the woody frame first, the canopy mass over it (a
 * crown's interior wood is hidden by its own foliage), then the seasonal organs in front, depth
 * sorted inside each layer.
 *
 * Colours are the design's palette translated to the landscape's finished daylight sRGB — the same
 * translation the Paulownia sprite uses, so both species sit in the same light — and the scene's own
 * Sun is applied at draw time as a per-channel multiply. `PorterDuffColorFilter(MULTIPLY)` is not
 * used: its blend maths takes the filter colour's opaque alpha as the destination alpha, which
 * would paint an opaque rectangle over the valley.
 *
 * Main-thread only: [forLayout] reads and fills a small cache, like the landscape's vegetation cache.
 */
internal class SugarMapleSprite private constructor(
    private val bitmap: Bitmap,
    /** Bitmap width / bitmap height. */
    val aspect: Float,
    /** Where the trunk's base line sits in the bitmap, as a fraction of its width. */
    val baseXFraction: Float,
    /** Where the ground line sits in the bitmap, as a fraction of its height. */
    val baseYFraction: Float,
    /** Bitmap height / the design's 19.6 m tree height. */
    val spanFraction: Float
) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private var filterArgb: Int = Int.MIN_VALUE

    /**
     * Projects the one render at one planting.
     *
     * @param baseX screen x of the trunk's foot.
     * @param baseY screen y of the ground line (the tree's foot).
     * @param treeHeightPx the design's 19.6 m in scene pixels — the tree's true height, so every
     * planting keeps the caller's perspective scale.
     * @param flip mirror the render (free per-instance variation on a bitmap).
     * @param tint 0xRRGGBB multiply applied to the whole render for this frame's light.
     * @param alpha extra fade (aerial perspective between the monuments).
     */
    fun draw(
        drawScope: DrawScope,
        baseX: Float,
        baseY: Float,
        treeHeightPx: Float,
        flip: Boolean,
        tint: Int,
        alpha: Float
    ) {
        val height = treeHeightPx * spanFraction
        if (height < 2f) return
        val width = height * aspect
        if (width < 1f) return
        // The bake lives at its own resolution; this is the one scale that lands it on this
        // planting's height. Nothing is re-rasterised.
        val k = height / bitmap.height
        if (filterArgb != tint) {
            paint.colorFilter = LightingColorFilter((tint and 0x00FFFFFF) or 0xFF000000.toInt(), 0)
            filterArgb = tint
        }
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
        if (paint.alpha == 0) return
        val left = baseX - width * baseXFraction
        val top = baseY - height * baseYFraction
        drawScope.withTransform({
            translate(left = if (flip) left + width else left, top = top)
            scale(scaleX = if (flip) -k else k, scaleY = k, pivot = Offset.Zero)
        }) {
            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint) }
        }
    }

    companion object {

        /** Bakes at 2.2× the tallest planting, quantised to 32 px shelves (192–640 px). */
        private const val OVERSAMPLE = 2.2f
        private const val SHELF = 32
        private const val MIN_SPAN = 192
        private const val MAX_SPAN = 640

        private class Entry(val look: SugarMapleLook, val spanPx: Int, val sprite: SugarMapleSprite)

        /** Two looks at one size; a rotation or resize evicts the oldest, never grows unbounded. */
        private val cache = ArrayList<Entry>(2)

        /** The render for [look], baked for a layout whose tallest maple is [tallestTreePx]. */
        fun forLayout(look: SugarMapleLook, tallestTreePx: Float): SugarMapleSprite {
            val spanPx = (ceil(tallestTreePx * OVERSAMPLE / SHELF).toInt() * SHELF)
                .coerceIn(MIN_SPAN, MAX_SPAN)
            for (entry in cache) {
                if (entry.look == look && entry.spanPx == spanPx) return entry.sprite
            }
            val sprite = bake(look, spanPx)
            if (cache.size >= 2) cache.removeAt(0)
            cache.add(Entry(look, spanPx, sprite))
            return sprite
        }

        // ---- the bake ---------------------------------------------------------------------

        private class Prim(val z: Float, val draw: (AndroidCanvas) -> Unit)

        private fun bake(look: SugarMapleLook, spanPx: Int): SugarMapleSprite {
            val skel = SugarMapleTree.skeleton()
            val organs = SugarMapleTree.organs()

            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            fun include(x: Float, y: Float) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
            for (limb in skel.limbs) {
                var i = 0
                while (i < limb.rads.size) {
                    val x = limb.pts[i * 3]
                    val y = limb.pts[i * 3 + 1]
                    val r = limb.rads[i]
                    include(x - r, y - r)
                    include(x + r, y + r)
                    i++
                }
            }
            for (o in organs) {
                // Every organ's length reaches out from its base — a blade's tip, a samara's wing,
                // a cluster's diameter — so the full length bounds it, perpendicular one included.
                val ex = o.ax * o.length
                val ey = o.ay * o.length
                val px = -o.ay * o.length
                val py = o.ax * o.length
                include(o.x, o.y)
                include(o.x + ex, o.y + ey)
                include(o.x + px, o.y + py)
                include(o.x - px, o.y - py)
            }
            if (minX > maxX || minY > maxY) {
                val empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                return SugarMapleSprite(empty, 1f, 0.5f, 1f, 1f)
            }
            minX -= 0.10f
            maxX += 0.10f
            minY -= 0.05f
            maxY += 0.10f

            val spanM = (maxY - minY).coerceAtLeast(0.5f)
            val spanX = (maxX - minX).coerceAtLeast(0.5f)
            val k = spanPx / spanM
            val widthPx = ceil(spanX * k).toInt().coerceIn(1, 4096)
            val bitmap = Bitmap.createBitmap(widthPx, spanPx, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bitmap)

            fun sx(v: Float): Float = (v - minX) * k
            fun sy(v: Float): Float = (maxY - v) * k

            val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            fill.style = Paint.Style.FILL
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            stroke.style = Paint.Style.STROKE
            stroke.strokeCap = Paint.Cap.ROUND
            stroke.strokeJoin = Paint.Join.ROUND

            // ---- the woody frame -----------------------------------------------------------
            val wood = ArrayList<Prim>(1200)
            for (limb in skel.limbs) {
                val n = limb.rads.size
                if (n < 2) continue
                val zMid = limb.pts[(n / 2) * 3 + 2]
                val pts = FloatArray(n * 2)
                val rads = FloatArray(n)
                var i = 0
                while (i < n) {
                    pts[i * 2] = sx(limb.pts[i * 3])
                    pts[i * 2 + 1] = sy(limb.pts[i * 3 + 1])
                    rads[i] = limb.rads[i] * k
                    i++
                }
                val path = ribbon(pts, rads)
                val barkColour = if (limb.order <= 1) BARK else TWIG
                val midX = limb.pts[(n / 2) * 3]
                val midY = limb.pts[(n / 2) * 3 + 1]
                val exp = 0.62f + 0.40f * SugarMapleTree.exposureAt(midX, midY, zMid).toFloat()
                    .coerceIn(0f, 1.15f)
                wood.add(
                    Prim(zMid) { c ->
                        fill.color = shift(barkColour, exp)
                        c.drawPath(path, fill)
                    }
                )
                // Bark plates on anything thick enough to show them.
                if (limb.order <= 1 && rads[0] > 3f) {
                    wood.add(
                        Prim(zMid + 0.01f) { c ->
                            for (f in BARK_GROOVES) {
                                stroke.color = if (f < 0f) shift(BARK_GROOVE, exp * 0.9f) else shift(BARK_RIDGE, exp)
                                stroke.strokeWidth = max(0.7f, rads[0] * 0.12f)
                                c.drawPath(offsetPolyline(pts, rads, f), stroke)
                            }
                        }
                    )
                }
            }

            // ---- the canopy mass -----------------------------------------------------------
            val mass = ArrayList<Prim>(700)
            val massAlpha = if (look == SugarMapleLook.GREEN) 226 else 210
            for (o in organs) {
                if (o.kind != SugarMapleTree.OrganKind.CLUSTER) continue
                val rad = o.length * 0.5f * k
                val cx = sx(o.x)
                val cy = sy(o.y)
                val lit = shield(o.x, o.y + o.length * 0.3f, o.z)
                val colour = if (look == SugarMapleLook.GREEN) {
                    blend(LEAF_SHADE, blend(LEAF_SUN, LEAF_UNDER, 0.35f), 0.62f * lit)
                } else {
                    shift(blend(AUTUMN_BROWN, autumnColour(o.tone), 0.45f), 0.45f + 0.55f * lit)
                }
                val pts = LandscapeProfile.clusterPolygon(rad, 0.86f, 7717 + o.seed, o.tone.toRawBits(), 9)
                val poly = AndroidPath()
                poly.moveTo(cx + pts[0], cy + pts[1])
                var q = 2
                while (q < pts.size) {
                    poly.lineTo(cx + pts[q], cy + pts[q + 1])
                    q += 2
                }
                poly.close()
                mass.add(
                    Prim(o.z) { c ->
                        fill.color = withAlpha(colour, massAlpha)
                        c.drawPath(poly, fill)
                    }
                )
            }

            // ---- the seasonal organs -------------------------------------------------------
            val organsOut = ArrayList<Prim>(2600)
            for (o in organs) {
                val ox = sx(o.x)
                val oy = sy(o.y)
                val len = max(1.2f, o.length * k)
                val deg = (atan2(o.ax.toDouble(), -o.ay.toDouble()) * 180.0 / PI).toFloat()
                val expo = (o.exposure * 1.15f).coerceIn(0f, 1.15f)
                val frontF = 0.80f + 0.30f * o.front
                val z = o.z + o.length * 0.05f
                val lit = shield(o.x, o.y, o.z)
                when (o.kind) {
                    SugarMapleTree.OrganKind.LEAF -> {
                        val blade = leafPath(len)
                        val colour: Int
                        val edge: Int
                        if (look == SugarMapleLook.GREEN) {
                            var base = blend(LEAF_SHADE, LEAF_SUN, (0.05f + 1.05f * lit).coerceIn(0f, 1f))
                            base = blend(base, LEAF_UNDER, 0.22f * (1f - o.exposure.coerceIn(0f, 1f)))
                            colour = shift(base, frontF * (0.78f + 0.34f * lit))
                            edge = shift(LEAF_UNDER, frontF * 0.9f)
                        } else {
                            val base = blend(autumnColour(o.tone), AUTUMN_YELLOW, 0.30f * (1f - lit))
                            colour = shift(base, frontF * (0.78f + 0.34f * lit))
                            edge = shift(AUTUMN_BROWN, frontF * 0.9f)
                        }
                        val vein = shift(colour, 0.72f)
                        organsOut.add(
                            Prim(z) { c ->
                                c.save()
                                c.translate(ox, oy)
                                c.rotate(deg)
                                fill.color = colour
                                c.drawPath(blade, fill)
                                stroke.color = withAlpha(edge, 170)
                                stroke.strokeWidth = max(0.45f, len * 0.035f)
                                c.drawPath(blade, stroke)
                                // Palmate venation: five veins out of the petiole junction.
                                stroke.color = withAlpha(vein, 200)
                                stroke.strokeWidth = max(0.35f, len * 0.022f)
                                for (v in PALMATE) {
                                    c.drawLine(0f, 0f, v[0] * len, v[1] * len, stroke)
                                }
                                c.restore()
                            }
                        )
                    }

                    SugarMapleTree.OrganKind.SAMARA -> {
                        val colour = shift(if (look == SugarMapleLook.GREEN) SAMARA_GREEN else SAMARA_BROWN, frontF)
                        organsOut.add(
                            Prim(z) { c ->
                                c.save()
                                c.translate(ox, oy)
                                c.rotate(deg)
                                fill.color = colour
                                for (wing in SAMARA_WINGS) {
                                    val path = AndroidPath()
                                    path.moveTo(wing[0] * len, wing[1] * len)
                                    var i = 2
                                    while (i < wing.size) {
                                        path.lineTo(wing[i] * len, wing[i + 1] * len)
                                        i += 2
                                    }
                                    path.close()
                                    c.drawPath(path, fill)
                                }
                                c.restore()
                            }
                        )
                    }

                    SugarMapleTree.OrganKind.BUD -> {
                        val r = max(0.6f, len * 0.5f)
                        organsOut.add(
                            Prim(z) { c ->
                                fill.color = BUD_RED
                                c.drawCircle(ox, oy, r, fill)
                            }
                        )
                    }

                    SugarMapleTree.OrganKind.CLUSTER -> Unit
                }
            }

            // Three layers, not one z-sort: wood, then canopy mass over it, then organs in front.
            wood.sortBy { it.z }
            mass.sortBy { it.z }
            organsOut.sortBy { it.z }
            for (p in wood) p.draw(canvas)
            for (p in mass) p.draw(canvas)
            for (p in organsOut) p.draw(canvas)

            return SugarMapleSprite(
                bitmap = bitmap,
                aspect = widthPx.toFloat() / spanPx.toFloat(),
                baseXFraction = if (widthPx > 1) (0f - minX) / spanX else 0.5f,
                baseYFraction = (maxY - 0f) / spanM,
                spanFraction = spanM / SugarMapleTree.HEIGHT_M
            )
        }

        /** The bake's own sun: high, slightly left and toward the viewer. */
        private const val SUN_X = -0.42f
        private const val SUN_Y = 0.78f
        private const val SUN_Z = 0.46f

        /** Crown-normal light term: 0 deep in shade, 1 on the sun-facing shell. */
        private fun shield(x: Float, y: Float, z: Float): Float {
            val nx = x / CROWN_RX
            val ny = (y - CROWN_CY) * 0.85f / CROWN_RY
            val nz = z / CROWN_RZ
            val l = sqrt(nx * nx + ny * ny + nz * nz)
            if (l < 1e-4f) return 0.5f
            val d = (nx / l) * SUN_X + (ny / l) * SUN_Y + (nz / l) * SUN_Z
            return (0.5f + 0.5f * d).coerceIn(0f, 1f)
        }

        // The design's crown, duplicated here so the raster needs no tree instance to shade with.
        private const val CROWN_CY = 11.05f
        private const val CROWN_RX = 7.55f
        private const val CROWN_RY = 7.35f
        private const val CROWN_RZ = 4.55f

        /** One limb's outline: a tapered ribbon through the projected points. */
        private fun ribbon(pts: FloatArray, rads: FloatArray): AndroidPath {
            val n = rads.size
            val nx = FloatArray(n)
            val ny = FloatArray(n)
            for (i in 0 until n) {
                val a = if (i > 0) i - 1 else 0
                val b = if (i < n - 1) i + 1 else n - 1
                var dx = pts[b * 2] - pts[a * 2]
                var dy = pts[b * 2 + 1] - pts[a * 2 + 1]
                val l = sqrt(dx * dx + dy * dy)
                if (l < 1e-3f) {
                    dx = 0f
                    dy = 1f
                } else {
                    dx /= l
                    dy /= l
                }
                nx[i] = -dy
                ny[i] = dx
            }
            val path = AndroidPath()
            path.moveTo(pts[0] + nx[0] * rads[0], pts[1] + ny[0] * rads[0])
            for (i in 1 until n) path.lineTo(pts[i * 2] + nx[i] * rads[i], pts[i * 2 + 1] + ny[i] * rads[i])
            for (i in n - 1 downTo 0) path.lineTo(pts[i * 2] - nx[i] * rads[i], pts[i * 2 + 1] - ny[i] * rads[i])
            path.close()
            return path
        }

        /** A polyline parallel to a limb at [f] of its radius, for bark fissures and ridges. */
        private fun offsetPolyline(pts: FloatArray, rads: FloatArray, f: Float): AndroidPath {
            val n = rads.size
            val path = AndroidPath()
            for (i in 0 until n) {
                val a = if (i > 0) i - 1 else 0
                val b = if (i < n - 1) i + 1 else n - 1
                var dx = pts[b * 2] - pts[a * 2]
                var dy = pts[b * 2 + 1] - pts[a * 2 + 1]
                val l = sqrt(dx * dx + dy * dy)
                if (l < 1e-3f) {
                    dx = 0f
                    dy = 1f
                } else {
                    dx /= l
                    dy /= l
                }
                val x = pts[i * 2] - dy * rads[i] * f
                val y = pts[i * 2 + 1] + dx * rads[i] * f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        /**
         * The design's sugar-maple blade: a palmately five-lobed outline with rounded sinuses and
         * a pointed terminal lobe, drawn in local space with the tip at `(0, -len)`.
         *
         * The lobe tips are the outline's extreme points, exactly as the design's blade polygon has
         * them, so the silhouette reads as *Acer saccharum* and not as a generic leaf.
         */
        private fun leafPath(len: Float): AndroidPath {
            val path = AndroidPath()
            path.moveTo(0f, -len * 0.015f)
            path.lineTo(0.52f * len * 0.5f, -len * 0.54f)     // right lateral lobe tip
            path.lineTo(0.285f * len * 0.5f, -len * 0.59f)    // sinus
            path.lineTo(0.205f * len * 0.5f, -len * 0.785f)
            path.lineTo(0f, -len)                             // terminal lobe tip
            path.lineTo(-0.205f * len * 0.5f, -len * 0.785f)
            path.lineTo(-0.285f * len * 0.5f, -len * 0.59f)
            path.lineTo(-0.52f * len * 0.5f, -len * 0.54f)
            path.lineTo(-0.305f * len * 0.5f, -len * 0.075f)
            path.lineTo(-0.15f * len * 0.5f, -len * 0.005f)
            path.close()
            return path
        }

        private fun withAlpha(color: Int, alpha: Int): Int =
            (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

        /** [color] × [factor] per channel, alpha untouched. */
        private fun shift(color: Int, factor: Float): Int {
            val f = factor.coerceIn(0f, 1.60f)
            val r = (((color shr 16) and 0xFF) * f).toInt().coerceIn(0, 255)
            val g = (((color shr 8) and 0xFF) * f).toInt().coerceIn(0, 255)
            val b = ((color and 0xFF) * f).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        private fun blend(a: Int, b: Int, t: Float): Int {
            val f = t.coerceIn(0f, 1f)
            val ar = (a shr 16) and 0xFF
            val ag = (a shr 8) and 0xFF
            val ab = a and 0xFF
            val br = (b shr 16) and 0xFF
            val bg = (b shr 8) and 0xFF
            val bb = b and 0xFF
            val r = (ar + (br - ar) * f).toInt().coerceIn(0, 255)
            val g = (ag + (bg - ag) * f).toInt().coerceIn(0, 255)
            val bl = (ab + (bb - ab) * f).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
        }

        /** The design's autumn crown, yellow through scarlet by the blade's own hue coordinate. */
        private fun autumnColour(tone: Float): Int = when {
            tone < 0.28f -> AUTUMN_GREEN
            tone < 0.48f -> AUTUMN_YELLOW
            tone < 0.68f -> AUTUMN_GOLD
            tone < 0.86f -> AUTUMN_ORANGE
            else -> AUTUMN_SCARLET
        }

        /** Five palmate veins out of the petiole junction, in blade-local units. */
        private val PALMATE = arrayOf(
            floatArrayOf(0f, -1f),
            floatArrayOf(0.62f, -0.62f),
            floatArrayOf(-0.62f, -0.62f),
            floatArrayOf(0.95f, -0.08f),
            floatArrayOf(-0.95f, -0.08f)
        )

        /** Both winged halves of a paired samara, in organ-local units. */
        private val SAMARA_WINGS = arrayOf(
            floatArrayOf(
                0.10f, -0.16f, 0.34f, -0.20f, 0.52f, -0.72f, 0.30f, -1.00f, 0.16f, -0.62f, 0.06f, -0.30f
            ),
            floatArrayOf(
                -0.10f, -0.16f, -0.34f, -0.20f, -0.52f, -0.72f, -0.30f, -1.00f, -0.16f, -0.62f, -0.06f, -0.30f
            )
        )

        private val BARK_GROOVES = floatArrayOf(-0.45f, -0.05f, 0.48f)

        // ---- the design's palette, at the landscape's daylight ------------------------------
        private val BARK_GROOVE = 0xFF241F1B.toInt()
        private val BARK = 0xFF4A423A.toInt()
        private val BARK_RIDGE = 0xFF8A8072.toInt()
        private val TWIG = 0xFF6B4A33.toInt()
        private val LEAF_SHADE = 0xFF1B3312.toInt()
        private val LEAF_SUN = 0xFF538428.toInt()
        private val LEAF_UNDER = 0xFF556B3A.toInt()
        private val AUTUMN_BROWN = 0xFF7A5230.toInt()
        private val AUTUMN_GREEN = 0xFF8A7A20.toInt()
        private val AUTUMN_YELLOW = 0xFFB08A18.toInt()
        private val AUTUMN_GOLD = 0xFFB4550F.toInt()
        private val AUTUMN_ORANGE = 0xFF9A2B0E.toInt()
        private val AUTUMN_SCARLET = 0xFF8A1410.toInt()
        private val SAMARA_GREEN = 0xFF4E6B24.toInt()
        private val SAMARA_BROWN = 0xFF6E4520.toInt()
        private val BUD_RED = 0xFF55301E.toInt()
    }
}
