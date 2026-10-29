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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * **One tree, rendered once, projected everywhere.**
 *
 * The landscape's Paulownias are thirty-odd copies of a single render. [PaulowniaTree] grows the
 * supplied design's tree (trunk, eight scaffolds, crown ellipsoid, seasonal organs); this class
 * rasterises that tree — once per season, and once per display size — into a small bitmap, and every
 * planting is then one mirrored, tinted blit of it:
 *
 *  - **one render, not thirty**: the ~1,900 limbs/twigs and ~1,000 seasonal organs are drawn into an
 *    `ARGB_8888` bitmap a single time. No per-tree geometry exists anywhere, so a scene doubling its
 *    Paulownia count doubles only the number of image quads;
 *  - **bake at display size, not source size**: the bitmap is sized to 2.2× the tallest planting, so
 *    downscaling never exceeds ~2× and the blits stay crisp without a 4K offscreen buffer;
 *  - **the light is a multiply, not a re-render**: the bake carries the design's own albedo and its
 *    baked crown-AO/exposure shading, and the scene's Sun is applied at draw time as a
 *    [LightingColorFilter] (a per-channel multiply that leaves alpha intact). Dawn, noon, golden hour
 *    and astronomical night are four different multiplies of the same bitmap — nothing is redrawn,
 *    and [LightingColorFilter] is used instead of `PorterDuffColorFilter(MULTIPLY)` because the latter
 *    forces a transparent sprite's alpha to opaque, painting a rectangle over the valley.
 *
 * Mirrored copies are free (`scaleX = -1`), and the same silhouette flipped reads as a different tree
 * in a row of them, which is exactly the per-instance variation the old per-tree path system got from
 * its three skeleton recipes.
 *
 * Main-thread only: [forLayout] reads and fills a small cache, like the landscape's vegetation cache.
 */
internal class PaulowniaSprite private constructor(
    private val bitmap: Bitmap,
    /** Bitmap width / bitmap height. */
    val aspect: Float,
    /** Where the trunk's base line sits in the bitmap, as a fraction of its width. */
    val baseXFraction: Float,
    /** Where the ground line sits in the bitmap, as a fraction of its height. */
    val baseYFraction: Float,
    /** Bitmap height / the design's 13.8 m tree height. */
    val spanFraction: Float
) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private var filterArgb: Int = Int.MIN_VALUE

    /**
     * Projects the one render at one planting.
     *
     * @param baseX screen x of the trunk's foot.
     * @param baseY screen y of the ground line (the tree's foot).
     * @param treeHeightPx the design's 13.8 m in scene pixels — the tree's true height, so every
     * planting keeps the caller's perspective scale.
     * @param flip mirror the render (the free per-instance variation).
     * @param tint 0xRRGGBB multiply applied to the whole render for this frame's light.
     * @param alpha extra fade (aerial perspective for the far bank).
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
        // The bake lives at its own resolution (2.2× the tallest planting); this is the one scale
        // that lands it on this planting's height. Nothing is re-rasterised.
        val k = height / bitmap.height
        if (filterArgb != tint) {
            // Channel-wise multiply, alpha untouched: the sprite's own silhouette stays the
            // silhouette. (`PorterDuffColorFilter(..., MULTIPLY)` is the wrong tool here — its blend
            // maths takes the filter colour's opaque alpha as the destination alpha, which turns a
            // transparent sprite into an opaque rectangle over the valley.)
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

        /** Bakes at 2.2× the tallest planting, quantised to 32 px shelves (192–576 px). */
        private const val OVERSAMPLE = 2.2f
        private const val SHELF = 32
        private const val MIN_SPAN = 192
        private const val MAX_SPAN = 576

        private class Entry(val season: PaulowniaSeason, val spanPx: Int, val sprite: PaulowniaSprite)

        /** Four seasons at one size; a rotation or resize evicts the oldest, never grows unbounded. */
        private val cache = ArrayList<Entry>(4)

        /** The render for [season], baked for a layout whose tallest paulownia is [tallestTreePx]. */
        fun forLayout(season: PaulowniaSeason, tallestTreePx: Float): PaulowniaSprite {
            val spanPx = (ceil(tallestTreePx * OVERSAMPLE / SHELF).toInt() * SHELF)
                .coerceIn(MIN_SPAN, MAX_SPAN)
            for (entry in cache) {
                if (entry.season == season && entry.spanPx == spanPx) return entry.sprite
            }
            val sprite = bake(season, spanPx)
            if (cache.size >= 4) cache.removeAt(0)
            cache.add(Entry(season, spanPx, sprite))
            return sprite
        }

        // ---- the bake ---------------------------------------------------------------------

        private class Prim(val z: Float, val draw: (AndroidCanvas) -> Unit)

        /**
         * Draws the whole tree into one bitmap.
         *
         * Painter order is the design's camera: every primitive carries its own depth and the list
         * is sorted back-to-front, so near twigs cross in front of far leaves and the crown has the
         * same layered interior a real canopy has.
         */
        private fun bake(season: PaulowniaSeason, spanPx: Int): PaulowniaSprite {
            val skel = PaulowniaTree.skeleton()
            val organs = PaulowniaTree.organs(season)

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
                for (i in limb.rads.indices) {
                    val x = limb.pts[i * 3]
                    val y = limb.pts[i * 3 + 1]
                    val r = limb.rads[i]
                    include(x - r, y - r)
                    include(x + r, y + r)
                }
            }
            for (o in organs) {
                include(o.x, o.y)
                include(o.x + o.ax * o.length, o.y + o.ay * o.length)
                include(o.x - o.ay * o.length * 0.45f, o.y + o.ax * o.length * 0.45f)
                include(o.x + o.ay * o.length * 0.45f, o.y - o.ax * o.length * 0.45f)
            }
            if (minX > maxX || minY > maxY) {
                // Degenerate growth: hand back a 1×1 transparent sprite rather than crash a bake.
                val empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                return PaulowniaSprite(empty, 1f, 0.5f, 1f, 1f)
            }
            // Breathing room, and the frame's own ground line stays exactly at y = 0.
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

            val prims = ArrayList<Prim>(2600)

            // ---- 1. crown under-mass: the shaded interior that closes the gaps between organs.
            // It is the 2D descendant of the design's inner-crown foliage, and the reason a 60 px
            // tree still reads as a mass instead of a dotted silhouette.
            //
            // Anchored to the leaves themselves, not to a disc: one lobed cluster under every Nth
            // leaf, sized to the leaf's own scale. A stack of blobs on an ellipse reads as a flat
            // grey plate showing past the foliage (the first three versions of this all did); a
            // cluster per leaf is mottled where the crown is dense and thin where it is not, which
            // is exactly what the canopy's own ambient occlusion does.
            if (season == PaulowniaSeason.SUMMER || season == PaulowniaSeason.AUTUMN) {
                val massAlpha = if (season == PaulowniaSeason.SUMMER) 214 else 196
                // The shade is the season's own: green under green leaves, brown under the retained
                // autumn ones (a green interior under a brown crown read as teal gaps).
                val massColor = if (season == PaulowniaSeason.SUMMER) LEAF_DEEP else MASS_AUTUMN
                // Not one disc: overlapping lobed clusters (LandscapeProfile.clusterPolygon — the
                // same frayed outline the valley's other crowns use), drawn under the organs.
                val every = if (season == PaulowniaSeason.SUMMER) 3 else 2
                val leavesOfMass = ArrayList<Prim>(260)
                var li = 0
                for (o in organs) {
                    val leafy = o.kind == PaulowniaTree.OrganKind.LEAF ||
                        o.kind == PaulowniaTree.OrganKind.LEAF_UNDER ||
                        o.kind == PaulowniaTree.OrganKind.LEAF_AUTUMN
                    if (!leafy) continue
                    val n = li++
                    if (n % every != 0) continue
                    // Roughly the leaf's own footprint, so the cluster hides behind the blade that
                    // placed it and only its fringes fill the gaps between blades.
                    val rad = (o.length * 0.62f + 0.05f) * k
                    val cx = sx(o.x)
                    val cy = sy(o.y) + rad * 0.10f
                    val pts = LandscapeProfile.clusterPolygon(rad, 0.86f, 4409 + n, n, 9)
                    val poly = AndroidPath()
                    poly.moveTo(cx + pts[0], cy + pts[1])
                    var q = 2
                    while (q < pts.size) {
                        poly.lineTo(cx + pts[q], cy + pts[q + 1])
                        q += 2
                    }
                    poly.close()
                    leavesOfMass.add(
                        Prim(o.z - 0.55f) { c ->
                            fill.color = withAlpha(massColor, massAlpha)
                            c.drawPath(poly, fill)
                        }
                    )
                }
                prims.addAll(leavesOfMass)
            }

            // ---- 2. wood ------------------------------------------------------------------
            for (limb in skel.limbs) {
                val n = limb.rads.size
                if (n < 2) continue
                val zMid = limb.pts[(n / 2) * 3 + 2]
                val pts = FloatArray(n * 2)
                for (i in 0 until n) {
                    pts[i * 2] = sx(limb.pts[i * 3])
                    pts[i * 2 + 1] = sy(limb.pts[i * 3 + 1])
                }
                val rads = FloatArray(n) { limb.rads[it] * k }
                val path = ribbon(pts, rads)
                val exp = 0.55f + 0.55f * (
                    (0.10f + 0.72f * crownDistanceOf(limb.pts[(n / 2) * 3], limb.pts[(n / 2) * 3 + 1], limb.pts[(n / 2) * 3 + 2]))
                        .coerceIn(0f, 1.15f)
                    )
                val barkColor = if (limb.order <= 1) BARK else TWIG
                prims.add(
                    Prim(zMid) { c ->
                        fill.color = shift(barkColor, exp)
                        c.drawPath(path, fill)
                    }
                )
                // Bark plates on everything thick enough to show them: two fissures on the shade
                // side and one weathered ridge on the lit side, along the limb's own tangent.
                if (limb.order <= 1 && rads[0] > 4f) {
                    val grooves = floatArrayOf(-0.45f, -0.05f, 0.48f)
                    prims.add(
                        Prim(zMid + 0.01f) { c ->
                            for (gi in grooves.indices) {
                                val f = grooves[gi]
                                stroke.color = if (f < 0f) shift(BARK_GROOVE, exp * 0.9f) else shift(BARK_RIDGE, exp)
                                stroke.strokeWidth = max(0.7f, rads[0] * 0.13f)
                                val gp = offsetPolyline(pts, rads, f)
                                c.drawPath(gp, stroke)
                            }
                        }
                    )
                }
            }

            // ---- 3. seasonal organs ---------------------------------------------------------
            for (o in organs) {
                val ox = sx(o.x)
                val oy = sy(o.y)
                val len = max(1.2f, o.length * k)
                val ax = o.ax
                val ay = o.ay
                val deg = (kotlin.math.atan2(ax.toDouble(), -ay.toDouble()) * 180.0 / Math.PI).toFloat()
                val expo = (o.exposure * 1.15f).coerceIn(0f, 1.15f)
                val frontF = 0.80f + 0.30f * o.front
                val z = o.z + o.length * 0.05f
                when (o.kind) {
                    PaulowniaTree.OrganKind.LEAF,
                    PaulowniaTree.OrganKind.LEAF_AUTUMN,
                    PaulowniaTree.OrganKind.LEAF_UNDER -> {
                        val blade = leafPath(len)
                        val colour = when (o.kind) {
                            PaulowniaTree.OrganKind.LEAF_UNDER -> blend(LEAF_BACK, LEAF_FRESH, 0.15f * expo)
                            PaulowniaTree.OrganKind.LEAF_AUTUMN -> blend(LEAF_BROWN, LEAF_OLIVE, 0.35f + 0.45f * expo)
                            else -> blend(LEAF_DARK, LEAF_FRESH, expo)
                        }
                        val leaf = shift(colour, frontF * (0.86f + 0.22f * expo))
                        val vein = shift(VEIN, frontF * 0.85f)
                        prims.add(
                            Prim(z) { c ->
                                c.save()
                                c.translate(ox, oy)
                                c.rotate(deg)
                                fill.color = leaf
                                c.drawPath(blade, fill)
                                stroke.color = vein
                                stroke.strokeWidth = max(0.45f, len * 0.045f)
                                c.drawLine(0f, 0f, 0f, -len * 0.86f, stroke)
                                stroke.strokeWidth = max(0.35f, len * 0.026f)
                                c.drawLine(0f, -len * 0.30f, -len * 0.20f, -len * 0.56f, stroke)
                                c.drawLine(0f, -len * 0.40f, len * 0.19f, -len * 0.66f, stroke)
                                c.restore()
                            }
                        )
                    }

                    PaulowniaTree.OrganKind.PANICLE -> {
                        // A lax terminal panicle: a stem and a row of tubular lilac bells, each with
                        // a paler ruffled rim — the design's 4–7 cm corollas, at 1.7× legibility.
                        val bells = 5 + (o.seed % 3)
                        val stem = shift(PANICLE_STEM, frontF)
                        val tube = shift(FLOWER_TUBE, frontF * (0.80f + 0.28f * expo))
                        val rim = shift(FLOWER_RIM, frontF * (0.86f + 0.24f * expo))
                        val throat = shift(THROAT, frontF)
                        val px = -ay
                        val py = ax
                        prims.add(
                            Prim(z) { c ->
                                stroke.color = stem
                                stroke.strokeWidth = max(0.55f, len * 0.030f)
                                c.drawLine(ox, oy, ox + ax * len, oy + ay * len, stroke)
                                for (b in 0 until bells) {
                                    val t = 0.26f + 0.74f * (b + 0.5f) / bells
                                    val wob = ((((b * 37 + o.seed) % 100) / 100f) - 0.5f) * len * 0.30f
                                    val bx = ox + ax * len * t + px * wob
                                    val by = oy + ay * len * t + py * wob
                                    val bl = len * (0.32f - 0.10f * t)
                                    c.save()
                                    c.translate(bx, by)
                                    c.rotate(deg)
                                    fill.color = tube
                                    c.drawOval(-bl * 0.20f, -bl, bl * 0.20f, 0f, fill)
                                    fill.color = rim
                                    c.drawCircle(0f, -bl * 0.92f, max(0.45f, bl * 0.15f), fill)
                                    if (bl > 2.2f) {
                                        fill.color = withAlpha(throat, 190)
                                        c.drawCircle(0f, -bl * 0.78f, max(0.35f, bl * 0.07f), fill)
                                    }
                                    c.restore()
                                }
                            }
                        )
                    }

                    PaulowniaTree.OrganKind.CAPSULE,
                    PaulowniaTree.OrganKind.CAPSULE_OPEN -> {
                        // The design's woody capsule: a tapered pod, brown-tan when ripe and split
                        // open at the tip once the seed has gone.
                        val open = o.kind == PaulowniaTree.OrganKind.CAPSULE_OPEN
                        val colour = if (open) CAPSULE_OLD else blend(CAPSULE, CAPSULE_RIPE, 0.55f)
                        val pod = shift(colour, frontF * (0.80f + 0.30f * expo))
                        val tip = shift(CAPSULE_OLD, 0.65f)
                        prims.add(
                            Prim(z) { c ->
                                c.save()
                                c.translate(ox, oy)
                                c.rotate(deg)
                                fill.color = pod
                                c.drawOval(-len * 0.26f, -len, len * 0.26f, 0f, fill)
                                if (open) {
                                    fill.color = tip
                                    c.drawOval(-len * 0.13f, -len * 0.98f, len * 0.13f, -len * 0.72f, fill)
                                }
                                c.restore()
                            }
                        )
                    }

                    PaulowniaTree.OrganKind.BUD_PANICLE -> {
                        // Felted rusty buds: next spring's flowers, standing all winter.
                        val bud = shift(CALYX, frontF * (0.82f + 0.26f * expo))
                        val stemC = shift(PANICLE_STEM, frontF * 0.9f)
                        val count = 6 + (o.seed % 3)
                        val px = -ay
                        val py = ax
                        prims.add(
                            Prim(z) { c ->
                                stroke.color = stemC
                                stroke.strokeWidth = max(0.5f, len * 0.030f)
                                c.drawLine(ox, oy, ox + ax * len, oy + ay * len, stroke)
                                for (b in 0 until count) {
                                    val t = 0.18f + 0.82f * (b + 0.5f) / count
                                    val wob = ((((b * 53 + o.seed) % 100) / 100f) - 0.5f) * len * 0.34f
                                    val bx = ox + ax * len * t + px * wob
                                    val by = oy + ay * len * t + py * wob
                                    fill.color = if (b % 3 == 0) shift(bud, 1.15f) else bud
                                    c.drawCircle(bx, by, max(0.5f, len * 0.085f), fill)
                                }
                            }
                        )
                    }

                    PaulowniaTree.OrganKind.SCAR -> {
                        val pale = shift(SCAR, 0.9f)
                        prims.add(
                            Prim(z) { c ->
                                fill.color = withAlpha(pale, 170)
                                c.drawCircle(ox, oy, max(0.35f, len * 0.30f), fill)
                            }
                        )
                    }

                    PaulowniaTree.OrganKind.COROLLA -> {
                        val fall = shift(FLOWER_RIM, 0.85f)
                        prims.add(
                            Prim(z - 5f) { c ->
                                c.save()
                                c.translate(ox, oy)
                                c.rotate(deg + 90f)
                                fill.color = withAlpha(fall, 200)
                                c.drawOval(-len * 0.5f, -len * 0.22f, len * 0.5f, len * 0.22f, fill)
                                c.restore()
                            }
                        )
                    }
                }
            }

            prims.sortBy { it.z }
            for (p in prims) p.draw(canvas)

            return PaulowniaSprite(
                bitmap = bitmap,
                aspect = widthPx.toFloat() / spanPx.toFloat(),
                baseXFraction = if (widthPx > 1) (0f - minX) / spanX else 0.5f,
                baseYFraction = (maxY - 0f) / spanM,
                spanFraction = spanM / PaulowniaTree.HEIGHT_M
            )
        }

        private fun crownDistanceOf(x: Float, y: Float, z: Float): Float {
            val nx = x / 5.90f
            val ny = (y - 8.35f) / 4.55f
            val nz = z / 3.35f
            return sqrt(nx * nx + ny * ny + nz * nz)
        }

        /**
         * One limb's outline: a tapered ribbon through the projected points, with each point's
         * normal taken from its neighbours so joints turn instead of creasing.
         */
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
         * A cordate *Paulownia* leaf: broad blade, two basal lobes and the sinus between them,
         * drawn in local space with the tip at `(0, -len)` so it can be rotated onto its branch.
         */
        private fun leafPath(len: Float): AndroidPath {
            val w = len * 0.36f
            val path = AndroidPath()
            // Cordate: a broad blade whose widest point sits about a third of the way up, two
            // rounded basal lobes and the sinus between them where the petiole enters.
            path.moveTo(0f, -len * 0.04f)
            path.cubicTo(-w * 0.30f, 0f, -w * 0.78f, -len * 0.02f, -w * 1.00f, -len * 0.26f)
            path.cubicTo(-w * 1.20f, -len * 0.54f, -w * 0.72f, -len * 0.82f, 0f, -len)
            path.cubicTo(w * 0.72f, -len * 0.82f, w * 1.20f, -len * 0.54f, w * 1.00f, -len * 0.26f)
            path.cubicTo(w * 0.78f, -len * 0.02f, w * 0.30f, 0f, 0f, -len * 0.04f)
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
            val r = (((a shr 16) and 0xFF) + (((b shr 16) and 0xFF) - ((a shr 16) and 0xFF)) * f).toInt()
            val g = (((a shr 8) and 0xFF) + (((b shr 8) and 0xFF) - ((a shr 8) and 0xFF)) * f).toInt()
            val bl = ((a and 0xFF) + ((b and 0xFF) - (a and 0xFF)) * f).toInt()
            return (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or bl.coerceIn(0, 255)
        }

        // ---- the design's palette, at the landscape's daylight ------------------------------
        // The supplied design stores linear albedo and lets its shader light and tone-map it. The
        // landscape's trees are painted in finished sRGB — the pistachio canopy is #415232 at noon —
        // so the same organs are given their daylight value here, and the scene's Sun multiplies it.

        private val BARK_GROOVE = 0xFF241A12.toInt()
        private val BARK = 0xFF4A3828.toInt()
        private val BARK_RIDGE = 0xFF6B5136.toInt()
        private val TWIG = 0xFF513E2A.toInt()
        private val LEAF_DEEP = 0xFF1B2A12.toInt()
        private val LEAF_DARK = 0xFF25391B.toInt()
        private val LEAF_FRESH = 0xFF4B6B2A.toInt()
        private val LEAF_BACK = 0xFF7C8A61.toInt()
        private val VEIN = 0xFF6E7340.toInt()
        private val FLOWER_TUBE = 0xFF5B4691.toInt()
        private val FLOWER_RIM = 0xFFB79CDB.toInt()
        private val THROAT = 0xFFE6D69B.toInt()
        private val CALYX = 0xFF8A5A2E.toInt()
        private val CAPSULE = 0xFF62763A.toInt()
        private val CAPSULE_RIPE = 0xFFA3834C.toInt()
        private val CAPSULE_OLD = 0xFF59422F.toInt()
        private val LEAF_BROWN = 0xFF6E4B28.toInt()
        /** The crown's own interior shade in autumn: warm dark brown, the leaves' retention tone. */
        private val MASS_AUTUMN = 0xFF33271A.toInt()
        private val LEAF_OLIVE = 0xFF7D6434.toInt()
        private val SCAR = 0xFFA4937A.toInt()
        private val PANICLE_STEM = 0xFF7C5C3A.toInt()
    }
}
