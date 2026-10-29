package com.alijafari.red.astronomy.ui.rendering

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The landscape's sugar maple — the supplied `SugarMaple.kt` design, grown as data.
 *
 * *Acer saccharum*: about 19.6 m at the crown top, a 0.94 m trunk at breast height with a broad
 * root flare, **ten hand-placed scaffold limbs** staggered up the leader between 3.35 and 7.40 m,
 * recursive opposite forking under a per-axis energy budget, current-year shoots on the outer
 * framework and 850 crown-gap twigs, all growing toward one broad oval crown centred at
 * `(0, 11.05, 0)` m with radii `(7.55, 7.35, 4.55)` m.
 *
 * ## Provenance
 *
 * This is the geometry of the supplied file, not a new species recipe: the same 32-bit LCG with
 * the same constants and the same consumption order, the same ten scaffolds, the same step / taper
 * / energy rules, the same crown-shell clamp (`cd > 0.94` bends an axis back along the ellipsoid
 * gradient), the same 2.75 m growth floor, the same current-year shoots and the same gap-fill pass.
 * The design's 2,300-axis budget and 850 fill twigs are kept intact — breadth-first growth with a
 * truncated budget would leave the first limbs half-built.
 *
 * ## Two looks, not four meshes
 *
 * The design ships two heavy seasonal meshes (summer: 807k vertices; peak autumn: 789k). The app
 * needs two *looks* and maps the **Persian calendar** onto them, as instructed:
 *
 * | Persian season | Look |
 * |---|---|
 * | spring, summer | [SugarMapleLook.GREEN] — green five-lobed blades |
 * | autumn, winter | [SugarMapleLook.AUTUMN] — yellow → gold → orange → scarlet |
 *
 * ## Scope
 *
 * Pure Kotlin and pure data: no Compose, no Android, no bitmaps, so a JVM test can assert the
 * proportions and the determinism. The raster lives in [SugarMapleSprite], which bakes this tree
 * once per look and display size and projects that single render at every planting.
 */
internal enum class SugarMapleLook {

    /** Spring and summer: green blades over green interior clusters. */
    GREEN,

    /** Autumn and winter: the design's yellow-through-scarlet crown. */
    AUTUMN;

    companion object {
        /** The look the Persian season calls for. */
        fun forSeason(season: PersianCalendar.Season): SugarMapleLook =
            if (season == PersianCalendar.Season.AUTUMN || season == PersianCalendar.Season.WINTER) {
                AUTUMN
            } else {
                GREEN
            }

        /** The look for the instant [jd] is drawn for. */
        fun forJulianDay(jd: Double): SugarMapleLook = forSeason(PersianCalendar.seasonOf(jd))
    }
}

internal object SugarMapleTree {

    /** Crown top of the design's tree, metres. */
    const val HEIGHT_M: Float = 19.6f

    /** Deterministic growth seed; one tree, rendered once, projected everywhere. */
    const val SEED: Int = 0x5ACE2026

    /** The design's axis budget. Kept whole: growth is breadth-first and must complete. */
    private const val MAX_BRANCHES = 2300

    /** The design's crown-gap twigs. */
    private const val FILL_TWIGS = 850

    /**
     * Deterministic stride over the design's ~15,400 canopy leaves.
     *
     * The design draws every blade at 800k vertices. A landscape tree is 20–95 px tall, where a
     * 12 cm blade is well under a pixel: the sprite keeps every sixth leaf (enlarged 2.2×), spread
     * uniformly over the crown rather than truncating the list, and lets the interior clusters
     * close the canopy between them.
     */
    private const val LEAF_KEEP = 0.16

    /** The same stride over the design's ~6,300 interior clusters. */
    private const val CLUSTER_KEEP = 0.26

    // The design's measured crown (metres).
    private const val CROWN_CY = 11.05f
    private const val CROWN_RX = 7.55f
    private const val CROWN_RY = 7.35f
    private const val CROWN_RZ = 4.55f

    /** A woody limb: `(x, y, z)` triplets in metres plus one radius per point. */
    internal class Limb(val pts: FloatArray, val rads: FloatArray, val order: Int)

    /** A growth node or terminal tip, with the design's crown exposure and depth ordering. */
    internal class Node(
        val x: Float, val y: Float, val z: Float,
        /** Axis heading at the node (unit). */
        val dx: Float, val dy: Float, val dz: Float,
        val r: Float,
        val order: Int,
        val root: Int,
        /** The design's crown exposure: 0 deep inside the crown, ~1 on its shell. */
        val exposure: Float,
        /** 0 at the back of the crown, 1 at the front (toward the viewer). */
        val front: Float
    )

    internal class Skeleton(
        val limbs: List<Limb>,
        val tips: List<Node>,
        val nodes: List<Node>,
        val branchCount: Int
    )

    /** What a seasonal organ is, for the raster. */
    internal enum class OrganKind { LEAF, CLUSTER, SAMARA, BUD }

    /**
     * One seasonal organ.
     *
     * [length] is in metres; [ax] / [ay] are the organ's axis in screen space (`y` down, as the
     * sprite draws it), so the raster needs no further projection. [tone] is the design's autumn
     * hue coordinate (0 lingering green → 1 scarlet), shared by both looks so spring and autumn
     * agree about which leaves are where.
     */
    internal class Organ(
        val kind: OrganKind,
        val x: Float, val y: Float, val z: Float,
        val length: Float,
        val ax: Float, val ay: Float,
        val exposure: Float,
        val front: Float,
        val seed: Int,
        val tone: Float,
        /** The design's blade detail tier (0 centre fan, 1–2 folded blades). */
        val detail: Int
    )

    /** The design's linear congruential generator, same constants, same consumption order. */
    private class Lcg(seed: Int) {
        private var s: Int = seed
        fun next(): Double {
            s = s * 1664525 + 1013904223
            return ((s ushr 8) and 0xFFFFFF).toDouble() / 16777216.0
        }
        fun range(a: Double, b: Double): Double = a + (b - a) * next()
    }

    // ---- caching --------------------------------------------------------------------------

    private var skeletonCache: Skeleton? = null
    private var organCache: List<Organ>? = null

    internal fun skeleton(seed: Int = SEED): Skeleton {
        skeletonCache?.let { if (seed == SEED) return it }
        val built = grow(seed)
        if (seed == SEED) skeletonCache = built
        return built
    }

    /** Every organ site of the design's tree, independent of look (the look only recolours). */
    internal fun organs(seed: Int = SEED): List<Organ> {
        organCache?.let { if (seed == SEED) return it }
        val built = placeOrgans(skeleton(seed), seed)
        if (seed == SEED) organCache = built
        return built
    }

    // ---- vector helpers, in the design's own frame -----------------------------------------

    private class V(val x: Double, val y: Double, val z: Double)

    private fun v(x: Double, y: Double, z: Double) = V(x, y, z)
    private fun V.plus(o: V) = V(x + o.x, y + o.y, z + o.z)
    private fun V.minus(o: V) = V(x - o.x, y - o.y, z - o.z)
    private fun V.times(s: Double) = V(x * s, y * s, z * s)
    private fun V.dot(o: V) = x * o.x + y * o.y + z * o.z
    private fun V.cross(o: V) = V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    private fun V.norm(): V {
        var l = sqrt(x * x + y * y + z * z)
        if (l == 0.0) l = 1.0
        return V(x / l, y / l, z / l)
    }

    private fun clamp(x: Double, a: Double, b: Double) = if (x < a) a else if (x > b) b else x

    private fun basis(d: V): Pair<V, V> {
        val up = if (abs(d.y) < 0.9) V(0.0, 1.0, 0.0) else V(1.0, 0.0, 0.0)
        val e1 = up.cross(d).norm()
        return Pair(e1, d.cross(e1))
    }

    private fun tilt(d: V, phi: Double, angle: Double): V {
        val (e1, e2) = basis(d)
        val side = e1.times(cos(phi)).plus(e2.times(sin(phi)))
        return d.times(cos(angle)).plus(side.times(sin(angle))).norm()
    }

    private fun crownDistance(p: V): Double {
        val nx = (p.x - 0.0) / CROWN_RX
        val ny = (p.y - CROWN_CY) / CROWN_RY
        val nz = (p.z - 0.0) / CROWN_RZ
        return sqrt(nx * nx + ny * ny + nz * nz)
    }

    /** The design's crown exposure at a world point: 0 deep in the crown, ~1 on its shell. */
    internal fun exposureAt(x: Float, y: Float, z: Float): Double =
        exposureOf(V(x.toDouble(), y.toDouble(), z.toDouble()))

    private fun exposureOf(p: V): Double = clamp(
        0.10 + 0.72 * crownDistance(p) +
            0.13 * clamp(p.z / CROWN_RZ, -1.0, 1.0) +
            0.08 * clamp((p.y - 4.0) / 15.0, 0.0, 1.0),
        0.0, 1.15
    )

    private fun frontOf(p: V): Double = clamp(0.5 + p.z / (2.0 * CROWN_RZ), 0.0, 1.0)

    // ---- growth ---------------------------------------------------------------------------

    private class Job(
        val o: V, val d: V, val pref: V,
        val l: Double, val e: Double, val r: Double,
        val order: Int, val root: Int
    )

    /** Catmull-style corner cutting, exactly as the design refines its paths. */
    private fun smoothPath(pts: List<V>, rads: DoubleArray, iters: Int): Pair<List<V>, DoubleArray> {
        var p = pts.toList()
        var r = rads.copyOf()
        repeat(iters) {
            val outP = ArrayList<V>(p.size * 2)
            val outR = DoubleArray(r.size * 2)
            outP.add(p[0]); outR[0] = r[0]
            var w = 1
            for (i in 0 until p.size - 1) {
                val a = p[i]
                val b = p[i + 1]
                outP.add(a.times(0.75).plus(b.times(0.25)))
                outR[w++] = r[i] * 0.75 + r[i + 1] * 0.25
                outP.add(a.times(0.25).plus(b.times(0.75)))
                outR[w++] = r[i] * 0.25 + r[i + 1] * 0.75
            }
            outP.add(p[p.size - 1]); outR[w] = r[r.size - 1]
            p = outP
            r = outR
        }
        return Pair(p, r)
    }

    private fun jsRound(x: Double): Int = floor(x + 0.5).toInt()

    private fun addLimb(limbs: MutableList<Limb>, pts: List<V>, rads: DoubleArray, order: Int) {
        val pa = FloatArray(pts.size * 3)
        for (i in pts.indices) {
            pa[i * 3] = pts[i].x.toFloat()
            pa[i * 3 + 1] = pts[i].y.toFloat()
            pa[i * 3 + 2] = pts[i].z.toFloat()
        }
        limbs.add(Limb(pa, FloatArray(rads.size) { rads[it].toFloat() }, order))
    }

    /**
     * The design's `growSugarMaple`, with its `tubePath` mesh calls replaced by limb records.
     *
     * The random sequence is consumed in exactly the design's order, so the tree is the tree the
     * design grows — all the way down to which twig the crown fill chose.
     */
    internal fun grow(seed: Int): Skeleton {
        val rnd = Lcg(seed)
        val limbs = ArrayList<Limb>(5200)
        val tips = ArrayList<Node>(4800)
        val nodes = ArrayList<Node>(9000)
        val jobs = ArrayList<Job>(2400)

        fun node(p: V, d: V, r: Double, order: Int, root: Int) = Node(
            p.x.toFloat(), p.y.toFloat(), p.z.toFloat(), d.x.toFloat(), d.y.toFloat(), d.z.toFloat(),
            r.toFloat(), order, root, exposureOf(p).toFloat(), frontOf(p).toFloat()
        )

        // Trunk: a nearly straight leader with a broad root flare.
        val trunkPts = listOf(
            v(0.0, 0.0, 0.0), v(-0.02, 0.45, 0.01), v(0.03, 1.10, -0.01), v(0.05, 2.00, 0.015),
            v(-0.01, 3.00, -0.02), v(0.02, 4.10, 0.0), v(0.0, 5.15, 0.0)
        )
        val (tp, tr) = smoothPath(trunkPts, doubleArrayOf(0.82, 0.56, 0.475, 0.455, 0.430, 0.370, 0.290), 2)
        addLimb(limbs, tp, tr, 0)
        for (i in 0 until 6) {
            val a = i / 6.0 * PI * 2.0 + rnd.range(-0.16, 0.16)
            val dir = v(cos(a), 0.0, sin(a))
            val reach = rnd.range(0.80, 1.15)
            addLimb(
                limbs,
                listOf(
                    v(dir.x * 0.18, 0.30, dir.z * 0.18),
                    v(dir.x * reach * 0.58, 0.085, dir.z * reach * 0.58),
                    v(dir.x * reach, 0.002, dir.z * reach)
                ),
                doubleArrayOf(0.210, 0.130, 0.050), 0
            )
        }

        fun scaffold(o: V, d: V, l: Double, e: Double, r: Double, root: Int) {
            val nd = d.norm()
            jobs.add(Job(o, nd, nd, l, e, r, 1, root))
        }
        // The design's ten hand-placed scaffolds.
        scaffold(v(0.0, 5.00, 0.0), v(-0.03, 0.99, 0.01), 2.40, 21.0, 0.250, 0)
        scaffold(v(-0.04, 3.35, 0.03), v(-0.82, 0.38, 0.08), 2.20, 21.5, 0.205, 1)
        scaffold(v(0.04, 3.55, -0.02), v(0.86, 0.34, -0.02), 2.30, 19.5, 0.200, 2)
        scaffold(v(-0.02, 5.20, -0.03), v(-0.55, 0.72, -0.16), 2.10, 21.5, 0.160, 3)
        scaffold(v(0.03, 5.55, 0.02), v(0.55, 0.70, 0.15), 2.15, 19.0, 0.148, 4)
        scaffold(v(0.0, 4.70, -0.06), v(-0.10, 0.73, -0.50), 2.00, 19.5, 0.148, 5)
        scaffold(v(-0.03, 4.05, 0.06), v(-0.58, 0.38, 0.42), 2.05, 20.5, 0.165, 6)
        scaffold(v(0.03, 4.45, 0.05), v(0.58, 0.36, 0.39), 2.05, 18.5, 0.152, 7)
        scaffold(v(-0.01, 7.00, -0.01), v(-0.48, 0.82, -0.04), 1.90, 19.0, 0.115, 8)
        scaffold(v(0.01, 7.40, 0.01), v(0.36, 0.87, 0.04), 1.85, 16.5, 0.104, 9)

        var branchCount = 0
        var ji = 0
        while (ji < jobs.size && branchCount < MAX_BRANCHES) {
            val job = jobs[ji]
            ji++
            branchCount++
            val steps = max(3, min(8, jsRound(job.l / (if (job.order < 3) 0.62 else 0.38))))
            val stepLen = job.l / steps
            val curvePhi = rnd.range(0.0, PI * 2.0)
            val curveAmt = rnd.range(0.080, 0.200)
            val curveDir = tilt(job.d, curvePhi, PI / 2.0)
            var p = job.o
            var dir = job.d
            val pts = ArrayList<V>(steps + 2)
            val rads = DoubleArray(steps + 2)
            var nPts = 0
            pts.add(p); rads[nPts++] = job.r * (if (job.order > 1) 1.22 else 1.06)
            val energyLeft = job.e - job.l
            val terminal = energyLeft <= 0.62 || job.r <= 0.007 || job.order >= 8
            val taper = when {
                terminal -> 0.72
                job.order == 1 -> 0.28
                job.order == 2 -> 0.32
                job.order == 3 -> 0.38
                job.order == 4 -> 0.44
                else -> 0.52
            }
            var laterals = 0
            val maxLaterals = when {
                job.order <= 2 -> 4
                job.order == 3 -> 3
                job.order <= 5 -> 2
                job.order == 6 -> 1
                else -> 0
            }
            for (s in 1..steps) {
                val f = s.toDouble() / steps
                var wish = dir.times(0.76).plus(
                    job.pref.times(0.24).plus(curveDir.times(curveAmt * (0.55 + 0.45 * sin(f * 4.2 + job.root))))
                )
                if (job.order >= 5) wish = wish.plus(V(0.0, 0.065, 0.0))
                if (f > 0.76 && job.order >= 4) wish = wish.plus(V(0.0, 0.11, 0.0))
                if (p.y < 6.2 && job.order <= 2) {
                    wish = wish.plus(v(p.x, 0.0, p.z).norm().times(0.07))
                }
                val jitterPhi = rnd.range(0.0, PI * 2.0)
                val jitterAmt = rnd.range(0.030, 0.100)
                wish = wish.plus(tilt(dir, jitterPhi, PI / 2.0).times(jitterAmt))
                dir = wish.norm()
                var next = p.plus(dir.times(stepLen))
                val cd = crownDistance(next)
                if (cd > 0.94 && job.order > 1) {
                    val grad = v(
                        (next.x - 0.0) / (CROWN_RX * CROWN_RX),
                        (next.y - CROWN_CY) / (CROWN_RY * CROWN_RY),
                        (next.z - 0.0) / (CROWN_RZ * CROWN_RZ)
                    ).norm()
                    val radial = dir.dot(grad)
                    if (radial > 0.0) {
                        dir = dir.minus(grad.times(radial * (if (cd > 1.05) 1.12 else 0.72)))
                            .plus(V(0.0, 0.025, 0.0)).norm()
                        next = p.plus(dir.times(stepLen * (if (cd > 1.05) 0.76 else 0.92)))
                    }
                }
                if (next.y < 2.75) next = v(next.x, 2.75 + 0.10 * f, next.z)
                p = next
                val radiusAt = max(0.0038, job.r * (1.0 - taper * f.pow(0.90)))
                pts.add(p); rads[nPts++] = radiusAt
                nodes.add(node(p, dir, radiusAt, job.order, job.root))
                if (s >= 2 && s < steps && laterals < maxLaterals && job.order < 8 &&
                    jobs.size < MAX_BRANCHES + 90
                ) {
                    val chance = when (job.order) {
                        1 -> 0.50
                        2 -> 0.48
                        3 -> 0.42
                        else -> 0.34
                    }
                    val energyAtNode = job.e - job.l * f
                    if (energyAtNode > 0.75 && rnd.next() < chance) {
                        val phi = rnd.range(0.0, PI * 2.0)
                        val pair = job.order >= 2 && rnd.next() < 0.48
                        val nKids = if (pair) 2 else 1
                        var k = 0
                        while (k < nKids && laterals < maxLaterals) {
                            val t = rnd.range(0.42, 0.78)
                            val outward = v(p.x * 0.8, 0.20, p.z * 0.8).norm()
                            val cd2 = tilt(dir, phi + k * PI, t)
                                .plus(outward.times(0.11)).plus(V(0.0, 0.075, 0.0)).norm()
                            val ce = energyAtNode * rnd.range(0.30, 0.49)
                            val cl = min(ce, 0.50 + ce * rnd.range(0.14, 0.22))
                            val cr = max(0.0062, radiusAt * rnd.range(0.50, 0.68))
                            jobs.add(Job(p.plus(dir.times(-stepLen * 0.05)), cd2, cd2, cl, ce, cr, job.order + 1, job.root))
                            laterals++
                            k++
                        }
                    }
                }
            }
            val endR = rads[nPts - 1]
            var continued = false
            if (!terminal && energyLeft > 0.62 && endR > 0.0058 && job.order < 8 &&
                jobs.size < MAX_BRANCHES + 90
            ) {
                val nKids = if (job.order <= 2) 2 else if (rnd.next() < 0.88) 2 else 1
                val phi = rnd.range(0.0, PI * 2.0)
                for (k in 0 until nKids) {
                    val t = if (job.order <= 2) rnd.range(0.16, 0.36) else rnd.range(0.20, 0.48)
                    val phiK = phi + k * PI + rnd.range(-0.24, 0.24)
                    val outward = v(p.x * 0.65, 0.12, p.z * 0.65).norm()
                    val cd2 = tilt(dir, phiK, t).plus(outward.times(0.055))
                        .plus(V(0.0, if (job.order >= 4) 0.160 else 0.075, 0.0)).norm()
                    val ce = energyLeft * when {
                        nKids == 1 -> rnd.range(0.95, 1.00)
                        k == 0 -> rnd.range(0.93, 0.99)
                        else -> rnd.range(0.55, 0.75)
                    }
                    val cl = min(ce, 0.65 + ce * rnd.range(0.12, 0.20))
                    val cr = max(0.0052, endR * (if (k == 0) rnd.range(0.74, 0.86) else rnd.range(0.55, 0.72)))
                    jobs.add(Job(p.plus(dir.times(-stepLen * 0.04)), cd2, cd2, cl, ce, cr, job.order + 1, job.root))
                    continued = true
                }
            }
            val (sp, sr) = smoothPath(pts.subList(0, nPts), rads.copyOf(nPts), 1)
            addLimb(limbs, sp, sr, job.order)
            if (!continued) tips.add(node(p, dir, endR, job.order, job.root))
        }

        // ---- current-year shoots on the outer framework --------------------------------
        fun addShoot(origin: V, parentDir: V, parentR: Double, root: Int, order: Int) {
            var grad = v(
                (origin.x - 0.0) / (CROWN_RX * CROWN_RX),
                (origin.y - CROWN_CY) / (CROWN_RY * CROWN_RY),
                (origin.z - 0.0) / (CROWN_RZ * CROWN_RZ)
            ).norm()
            grad = v(grad.x, max(grad.y, -0.04), grad.z)
            val phi = rnd.range(0.0, PI * 2.0)
            val tangent = tilt(parentDir, phi, PI / 2.0)
            val tangentAmt = rnd.range(-0.36, 0.36)
            val upAmt = rnd.range(0.14, 0.34)
            val d = grad.times(0.48).plus(parentDir.times(0.34)).plus(tangent.times(tangentAmt))
                .plus(V(0.0, upAmt, 0.0)).norm()
            val len = rnd.range(0.30, 0.68) * (if (order > 8) 0.68 else 1.0)
            val r0 = clamp(parentR * rnd.range(0.26, 0.40), 0.0028, 0.0095)
            val sideBend = rnd.range(-0.05, 0.05)
            val p0 = origin.plus(d.times(-0.035))
            val p1 = origin.plus(d.times(len * 0.48))
                .plus(tangent.times(sideBend * len)).plus(V(0.0, len * 0.035, 0.0))
            val p2 = origin.plus(d.times(len)).plus(V(0.0, len * 0.105, 0.0))
            rnd.range(0.0, 30.0)                       // the design's tube seed
            addLimb(limbs, listOf(p0, p1, p2), doubleArrayOf(r0 * 1.18, r0 * 0.72, r0 * 0.38), order)
            val endDir = p2.minus(p1).norm()
            nodes.add(node(p1, endDir, r0 * 0.72, order, root))
            tips.add(node(p2, endDir, r0 * 0.38, order, root))
            if (order == 8 && rnd.next() < 0.24) addShoot(p1, endDir, r0, root, 9)
        }

        val baseTips = tips.toList()
        val baseNodes = nodes.toList()
        for (tip in baseTips) {
            if (tip.exposure > 0.58) {
                val r = max(tip.r.toDouble(), 0.012)
                addShoot(v(tip.x.toDouble(), tip.y.toDouble(), tip.z.toDouble()),
                    v(tip.dx.toDouble(), tip.dy.toDouble(), tip.dz.toDouble()), r, tip.root, 8)
                if (tip.exposure > 0.90 && rnd.next() < 0.35) {
                    addShoot(v(tip.x.toDouble(), tip.y.toDouble(), tip.z.toDouble()),
                        v(tip.dx.toDouble(), tip.dy.toDouble(), tip.dz.toDouble()), r, tip.root, 8)
                }
            }
        }
        for (n in baseNodes) {
            if (n.order >= 5 && rnd.next() < clamp(0.10 + 0.34 * n.exposure, 0.0, 0.46)) {
                val r = max(n.r.toDouble(), 0.012)
                addShoot(v(n.x.toDouble(), n.y.toDouble(), n.z.toDouble()),
                    v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble()), r, n.root, 8)
                if (n.exposure > 0.88 && rnd.next() < 0.16) {
                    addShoot(v(n.x.toDouble(), n.y.toDouble(), n.z.toDouble()),
                        v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble()), r, n.root, 8)
                }
            }
        }
        for (n in baseNodes) {
            if ((n.order == 2 || n.order == 3) && rnd.next() < 0.28 + 0.22 * n.exposure) {
                addShoot(v(n.x.toDouble(), n.y.toDouble(), n.z.toDouble()),
                    v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble()),
                    max(n.r.toDouble(), 0.014), n.root, 8)
            }
        }
        for (n in baseNodes) {
            if (n.order == 1 && rnd.next() < 0.38) {
                addShoot(v(n.x.toDouble(), n.y.toDouble(), n.z.toDouble()),
                    v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble()),
                    max(n.r.toDouble(), 0.014), n.root, 8)
            }
        }

        // ---- crown gap fill -------------------------------------------------------------
        val fillPool = ArrayList<Node>(baseTips.size + baseNodes.size + 2 * FILL_TWIGS)
        fillPool.addAll(baseTips)
        fillPool.addAll(baseNodes)
        val wedgeFrom = (FILL_TWIGS * 0.66).toInt()
        for (i in 0 until FILL_TWIGS) {
            val target: V
            if (i >= wedgeFrom) {
                // The lower/front crown wedge between diverging scaffolds.
                val yy = rnd.range(3.8, 8.6)
                val yn = (yy - CROWN_CY) / CROWN_RY
                val zn = rnd.range(-0.15, 0.95)
                val allow = sqrt(max(0.04, 1.0 - yn * yn - zn * zn))
                target = v(rnd.range(-1.0, 1.0) * CROWN_RX * allow * 0.94, yy, zn * CROWN_RZ * 0.94)
            } else {
                val q = rnd.next()
                val factor = if (q < 0.25) rnd.range(0.35, 0.65) else if (q < 0.80) {
                    rnd.range(0.65, 0.92)
                } else {
                    rnd.range(0.92, 1.05)
                }
                val zz = if (i % 3 == 0) rnd.range(0.10, 1.0) else rnd.range(-1.0, 1.0)
                val phi = rnd.range(0.0, PI * 2.0)
                val horiz = sqrt(max(0.0, 1.0 - zz * zz))
                target = v(
                    cos(phi) * horiz * CROWN_RX * factor,
                    CROWN_CY + zz * CROWN_RY * factor,
                    sin(phi) * horiz * CROWN_RZ * factor
                )
            }
            if (target.y < 3.60) continue
            var best: Node? = null
            var bestD2 = 1e9
            for (cand in fillPool) {
                val dx = target.x - cand.x
                val dy = target.y - cand.y
                val dz = target.z - cand.z
                val d2 = dx * dx + dy * dy + dz * dz
                if (d2 < bestD2) {
                    bestD2 = d2
                    best = cand
                }
            }
            val bn = best ?: continue
            val dist = sqrt(bestD2)
            if (dist < 0.22 || dist > (if (i >= wedgeFrom) 2.30 else 1.60)) continue
            val bp = v(bn.x.toDouble(), bn.y.toDouble(), bn.z.toDouble())
            val bd = v(bn.dx.toDouble(), bn.dy.toDouble(), bn.dz.toDouble())
            val targetDir = target.minus(bp).norm()
            val fillGrad = v(
                (bp.x - 0.0) / (CROWN_RX * CROWN_RX),
                (bp.y - CROWN_CY) / (CROWN_RY * CROWN_RY),
                (bp.z - 0.0) / (CROWN_RZ * CROWN_RZ)
            ).norm()
            val dir = targetDir.times(0.62).plus(bd.times(0.26)).plus(fillGrad.times(0.16))
                .plus(V(0.0, 0.16, 0.0)).norm()
            val len = dist * rnd.range(0.68, 0.86)
            val bendPhi = rnd.range(0.0, PI * 2.0)
            val bendAmt = rnd.range(-0.12, 0.12)
            val tangent = tilt(dir, bendPhi, PI / 2.0)
            val r0 = clamp(max(bn.r.toDouble(), 0.012) * rnd.range(0.50, 0.70), 0.008, 0.032)
            val p0 = bp.plus(dir.times(-0.04))
            val p1 = bp.plus(dir.times(len * 0.38)).plus(tangent.times(bendAmt * len))
            val p2 = bp.plus(dir.times(len * 0.72)).plus(tangent.times(-bendAmt * len * 0.45))
                .plus(V(0.0, len * 0.035, 0.0))
            val p3 = bp.plus(dir.times(len)).plus(V(0.0, len * 0.075, 0.0))
            val (fp, fr) = smoothPath(
                listOf(p0, p1, p2, p3), doubleArrayOf(r0 * 1.15, r0 * 0.76, r0 * 0.52, r0 * 0.30), 1
            )
            rnd.range(0.0, 30.0)                       // the design's tube seed
            val endDir = p3.minus(p2).norm()
            addLimb(limbs, fp, fr, 10)
            val rec1 = node(p1, endDir, r0 * 0.76, 10, bn.root)
            val rec2 = node(p2, endDir, r0 * 0.52, 10, bn.root)
            val tipRec = node(p3, endDir, r0 * 0.30, 10, bn.root)
            nodes.add(rec1)
            nodes.add(rec2)
            tips.add(tipRec)
            fillPool.add(rec2)
            fillPool.add(tipRec)
            if (len > 0.62 && rnd.next() < 0.28) {
                val forkPhi = rnd.range(0.0, PI * 2.0)
                val forkTilt = rnd.range(0.28, 0.52)
                val fd = tilt(endDir, forkPhi, forkTilt).plus(endDir.times(0.22))
                    .plus(V(0.0, 0.08, 0.0)).norm()
                val fl = rnd.range(0.18, 0.38)
                val fe = p2.plus(fd.times(fl)).plus(V(0.0, fl * 0.08, 0.0))
                rnd.range(0.0, 30.0)                   // the design's fork seed
                addLimb(limbs, listOf(p2, fe), doubleArrayOf(r0 * 0.42, r0 * 0.16), 11)
                val forkRec = node(fe, fd, r0 * 0.16, 11, bn.root)
                tips.add(forkRec)
                fillPool.add(forkRec)
            }
        }
        return Skeleton(limbs, tips, nodes, branchCount)
    }

    // ---- organ placement ------------------------------------------------------------------

    /** Radial direction in the horizontal plane, as the design uses for petiole lean. */
    private fun radialOf(p: V): V {
        val l = sqrt(p.x * p.x + p.z * p.z)
        return if (l > 0.15) V(p.x / l, 0.0, p.z / l) else V(0.0, 0.0, 1.0)
    }

    /**
     * The design's autumn hue coordinate: 0 is lingering green, 1 is scarlet.
     *
     * Kept for both looks so a spring blade and an autumn blade of the same leaf site agree about
     * where in the crown they sit.
     */
    private fun hueOf(node: Node, colorR: Double): Double {
        val heightF = clamp((node.y - 3.5) / 15.0, 0.0, 1.0)
        val rootTint = 0.07 * sin(node.root * 2.71 + 0.4)
        return clamp(
            0.10 + 0.58 * node.exposure + 0.22 * heightF + 0.20 * (colorR - 0.5) + rootTint,
            0.0, 1.18
        )
    }

    /** Screen-space unit axis of a 3-D direction; organs that point at the camera bias upward. */
    private fun axisTo(x: Double, y: Double): FloatArray {
        val sx = x
        val sy = -y
        val l = sqrt(sx * sx + sy * sy)
        if (l < 1e-4 || l < 0.22) return floatArrayOf(0f, -1f)
        return floatArrayOf((sx / l).toFloat(), (sy / l).toFloat())
    }

    private fun hash01(seed: Int, index: Int): Float = LandscapeProfile.hash01(seed, index)

    /**
     * The design's canopy: opposite leaf pairs, terminal bunches, interior clusters, samaras and
     * buds, at the sites and in the order the design places them.
     */
    internal fun placeOrgans(skel: Skeleton, seed: Int): List<Organ> {
        val rnd = Lcg(seed * 7919 + 17)
        val out = ArrayList<Organ>(3400)
        var leaves = 0
        var clusters = 0

        fun addLeaf(n: Node, sign: Int, phi: Double) {
            val (e1, e2) = basis(v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble()))
            val nd = v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble())
            val np = v(n.x.toDouble(), n.y.toDouble(), n.z.toDouble())
            val leafAxis = e1.times(cos(phi)).plus(e2.times(sin(phi)))
            val radial = radialOf(np)
            val petDir = leafAxis.times(sign * 0.78).plus(radial.times(0.30))
                .plus(nd.times(0.25)).plus(V(0.0, 0.30, 0.0)).norm()
            val petLen = rnd.range(0.055, 0.105)
            rnd.range(0.0, 20.0)                       // petiole seed
            val base = np.plus(petDir.times(petLen)).plus(V(0.0, petLen * 0.08, 0.0))
            val droopR = rnd.range(0.0, 1.0)
            val droopDir = 0.10 + 0.22 * droopR
            val bladeDir = radial.times(0.18).plus(petDir.times(0.22)).plus(nd.times(0.34))
                .plus(leafAxis.times(sign * 0.38)).plus(V(0.0, -droopDir, 0.0)).norm()
            rnd.range(0.0, PI * 2.0)                   // blade phi
            rnd.range(-0.34, 0.34)                     // blade roll
            rnd.range(-0.75, 0.75)                     // up-hint roll
            val lenR = rnd.range(0.0, 1.0)
            val length = (0.092 + 0.058 * lenR.pow(1.15)) * (0.90 + 0.22 * n.exposure) * 2.2
            rnd.range(0.92, 1.08)                      // width ratio
            rnd.range(0.88, 1.10)                      // leaf shade
            val colorR = rnd.range(0.0, 1.0)
            val orgSeed = rnd.range(0.0, 40.0)
            rnd.range(0.90, 1.10)                      // asym
            rnd.range(0.0, 1.0)                        // arch
            rnd.range(0.0, 1.0)                        // fold
            rnd.range(0.0, 1.0)                        // blade droop
            rnd.range(0.0, 1.0)                        // ripple
            val detail = if (n.front > 0.96f && n.exposure > 1.00f) 2 else
                if (n.front > 0.86f && n.exposure > 0.88f) 1 else 0
            leaves++
            // A deterministic stride over the crown, uniform rather than a truncated prefix.
            if (hash01(seed, leaves * 7 + 3) >= LEAF_KEEP) return
            val ax = axisTo(bladeDir.x, bladeDir.y)
            out.add(
                Organ(
                    OrganKind.LEAF,
                    base.x.toFloat(), base.y.toFloat(), base.z.toFloat(),
                    length.toFloat(), ax[0], ax[1],
                    n.exposure, n.front, (orgSeed * 1000).toInt(),
                    hueOf(n, colorR).toFloat(), detail
                )
            )
        }

        fun addCluster(n: Node) {
            val colorR = rnd.range(0.0, 1.0)
            val radiusR = rnd.range(0.0, 1.0)
            val orgSeed = rnd.range(0.0, 40.0)
            val radius = (0.32 + 0.34 * radiusR) * (0.74 + 0.36 * n.exposure) *
                (1.0 + 0.30 * clamp((n.y - 13.5) / 4.5, 0.0, 1.0))
            val np = v(n.x.toDouble(), n.y.toDouble(), n.z.toDouble())
            val nd = v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble())
            val radial = radialOf(np)
            val centre = np.plus(radial.times(-radius * 0.08)).plus(nd.times(radius * 0.16))
                .plus(V(0.0, radius * 0.05, 0.0))
            // Nothing of the canopy mass may leave the crown shell: a cluster whose rim pokes
            // out reads as a detached island of leaves floating above the tree. The bound uses
            // the tightest crown radius, so a rim can never cross any face of the ellipsoid.
            if (crownDistance(centre) + radius / CROWN_RZ > 0.98) return
            clusters++
            if (hash01(seed, clusters * 11 + 5) >= CLUSTER_KEEP) return
            val ax = axisTo(centre.x, centre.y)
            out.add(
                Organ(
                    OrganKind.CLUSTER,
                    centre.x.toFloat(), centre.y.toFloat(), centre.z.toFloat(),
                    (radius * 2.0).toFloat(), ax[0], ax[1],
                    n.exposure, n.front, (orgSeed * 1000).toInt(),
                    hueOf(n, colorR).toFloat(), 0
                )
            )
        }

        fun addSamara(n: Node) {
            val (e1, e2) = basis(v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble()))
            val np = v(n.x.toDouble(), n.y.toDouble(), n.z.toDouble())
            val nd = v(n.dx.toDouble(), n.dy.toDouble(), n.dz.toDouble())
            val count = 2 + (if (rnd.next() < 0.55) 1 else 0) + (if (rnd.next() < 0.22) 1 else 0)
            for (i in 0 until count) {
                val phi = rnd.range(0.0, PI * 2.0)
                val axis = e1.times(cos(phi)).plus(e2.times(sin(phi)))
                val hang = nd.times(0.22).plus(radialOf(np).times(0.30)).plus(axis.times(0.38))
                    .plus(V(0.0, -0.88, 0.0)).norm()
                val stalk = rnd.range(0.035, 0.078)
                val origin = np.plus(hang.times(stalk))
                val orgSeed = rnd.range(0.0, 20.0)
                val samLen = rnd.range(0.024, 0.034)
                rnd.range(0.52, 0.78)                  // half angle
                rnd.range(0.90, 1.05)                  // shade
                out.add(
                    Organ(
                        OrganKind.SAMARA,
                        origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat(),
                        (samLen * 1.9).toFloat(), hang.x.toFloat(), (-hang.y).toFloat(),
                        n.exposure, n.front, (orgSeed * 1000).toInt(), 0f, 0
                    )
                )
            }
        }

        fun addBud(n: Node) {
            val length = rnd.range(0.010, 0.016)
            rnd.range(0.92, 1.05)
            out.add(
                Organ(
                    OrganKind.BUD,
                    n.x, n.y, n.z,
                    (max(length, 0.012) * 2.2).toFloat(), n.dx, -n.dy,
                    n.exposure, n.front, 0, 0f, 0
                )
            )
        }

        // Opposite canopy leaves: the site sequence is the design's, in its order.
        for (n in skel.nodes) {
            if (n.order >= 3) {
                val pairChance = if (n.order == 3) 0.16 + 0.16 * n.exposure else 0.48 + 0.25 * n.exposure
                if (rnd.next() < pairChance) {
                    val phi = rnd.range(0.0, PI * 2.0)
                    addLeaf(n, -1, phi)
                    if (rnd.next() < 0.88) addLeaf(n, 1, phi)
                } else if (rnd.next() < 0.24) {
                    val phi = rnd.range(0.0, PI * 2.0)
                    addLeaf(n, if (rnd.next() < 0.5) -1 else 1, phi)
                }
            }
        }
        for (tip in skel.tips) {
            if (tip.order >= 4 && rnd.next() < 0.42 + 0.30 * tip.exposure) {
                val phi = rnd.range(0.0, PI * 2.0)
                val sign = if (rnd.next() < 0.5) -1 else 1
                addLeaf(tip, sign, phi)
                if (rnd.next() < 0.42) addLeaf(tip, -sign, phi)
            }
        }
        // Interior leaf mass under the individually modelled blades.
        for (n in skel.nodes) {
            if (n.order >= 3 && rnd.next() < (if (n.order == 3) 0.12 + 0.16 * n.exposure
                else 0.26 + 0.22 * n.exposure)) {
                addCluster(n)
            }
        }
        for (tip in skel.tips) {
            if (tip.order >= 4 && rnd.next() < 0.34 + 0.26 * tip.exposure) addCluster(tip)
        }
        // The last leader tier has fewer recorded sites: one extra pass keeps it leafy.
        for (n in skel.nodes) {
            if (n.order >= 4 && n.y > 15.0 && rnd.next() < 0.20) addCluster(n)
        }
        for (tip in skel.tips) {
            if (tip.order >= 4 && tip.y > 14.0 && rnd.next() < 0.30) addCluster(tip)
        }
        for (tip in skel.tips) {
            if (tip.order >= 4 && tip.front > 0.55 && rnd.next() < 0.10) addBud(tip)
        }
        for (tip in skel.tips) {
            if (tip.order >= 4 && rnd.next() < 0.009 + 0.013 * tip.exposure) addSamara(tip)
        }
        for (n in skel.nodes) {
            if (n.order >= 7 && rnd.next() < 0.002 + 0.003 * n.exposure) addSamara(n)
        }
        return out
    }
}
