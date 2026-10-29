package com.alijafari.red.astronomy.ui.rendering

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The wardrobe of the landscape's Paulownia — the design's four seasons.
 *
 * *Paulownia tomentosa* is the one tree in the valley whose year is worth modelling: it flowers
 * before it leafs (April–May, on bare wood), carries huge heart leaves through summer, hangs ripe
 * tan capsules as its sparse autumn leaves go, and stands through winter as a coarse twig network
 * with persistent split capsules and the felted rusty bud panicles that are next spring's flowers.
 *
 * The state follows the scene's own instant (the same Julian date the sky is drawn for), so a Home
 * sky in April shows blossom on the trees below it and a Home sky in January shows the bare
 * architecture. This is an art cue, not a phenology model: the month boundaries are the design's
 * four meshes, nothing more.
 */
enum class PaulowniaSeason {

    /** Terminal panicles of pale-lilac bells on bare wood, a few expanding leaves. */
    SPRING,

    /** Opposite cordate leaves over the whole crown, green capsules, the first rusty buds. */
    SUMMER,

    /** Sparse brown/olive retained leaves, ripe splitting capsules, dense felted buds. */
    AUTUMN,

    /** Bare coarse twigs, dark split capsules, rusty terminal bud panicles, pale leaf scars. */
    WINTER;

    companion object {

        /**
         * The season of the instant [jd] is drawn for — the **Persian** calendar's, not Gregorian.
         *
         * The valley below the sky is dated with the sky. Spring begins on **1 Farvardin**
         * (Nowruz, 20/21 March), so the tree in flower below a mid-Ordibehesht sky is in flower for
         * the Persian spring, and the same tree below a Gregorian March sky is still in its winter
         * wood — which is what an Iranian landscape should do. [PersianCalendar] is the single
         * source of that mapping for both species.
         */
        fun forJulianDay(jd: Double): PaulowniaSeason = forSeason(PersianCalendar.seasonOf(jd))

        /** This species' wardrobe for a Persian season. */
        fun forSeason(season: PersianCalendar.Season): PaulowniaSeason = when (season) {
            PersianCalendar.Season.SPRING -> SPRING
            PersianCalendar.Season.SUMMER -> SUMMER
            PersianCalendar.Season.AUTUMN -> AUTUMN
            PersianCalendar.Season.WINTER -> WINTER
        }
    }
}

/**
 * The design's Paulownia, grown as geometry and projected to the landscape's flat plane.
 *
 * ## Provenance
 *
 * This is the tree from the supplied `Paulownia.kt` design, not a new species recipe. Its measured
 * frame is kept exactly: **13.8 m tall**, a 0.62 m trunk at breast height with root flare and a
 * swollen fork at 2.8 m, **eight hand-placed scaffold limbs** staggered up the leader (2.35–4.25 m),
 * recursive fork growth under an energy budget per axis, current-year shoots and crown-gap fill, all
 * growing toward one flattened crown ellipsoid `C = (0, 8.35, 0)`, `R = (5.90, 4.55, 3.35)` — a
 * 13.4 m crown, as wide as the tree is tall. World axes are the design's: +X east, +Y up, +Z toward
 * the viewer.
 *
 * The same deterministic growth decides the sibling organs' placement, because they live where the
 * design puts them — panicles on the outer terminals, leaves on tips and on the last fork orders,
 * capsules and buds on the terminal wood.
 *
 * ## What changed, and why
 *
 * The design is a 3D mesh scene: four seasonal meshes of 200k–377k vertices, built into one fixed
 * orthographic frame, lit by a shader and drawn by an OpenGL surface. That is the right shape for a
 * hero close-up of one tree. It is the wrong shape for the Home landscape, where the same species is
 * planted along a river and a canal at 2–7 % of the canvas height: a dedicated GL surface, a second
 * EGL context, tens of megabytes of vertex buffers and a per-frame render of ~400k triangles to
 * produce a 70 px tree would cost far more than the whole sky above it.
 *
 * So the tree is **rendered once and projected everywhere**. The geometry below is the design's; it
 * is grown once per process, and [PaulowniaSprite] rasterises it once per season (and per display
 * size) into a single small bitmap — bark plates, heart leaves, throat-lit bells, capsules, buds and
 * all. Every planting in the landscape is then one tinted, mirrored blit of that one render. The
 * valley's 30-odd Paulownias cost 30 image draws a frame and not one vertex of per-tree geometry.
 *
 * ## Scope
 *
 * Pure Kotlin and pure data: no Compose, no Android, no bitmaps. The raster lives in
 * [PaulowniaSprite], so this file can be asserted in a JVM unit test — which is how the design's
 * proportions (trunk clearance, crown spread, pipe-model taper, determinism) are held.
 */
internal object PaulowniaTree {

    /** The design's tree height, metres. Everything below is in metres in this frame. */
    const val HEIGHT_M: Float = 13.8f

    /** Deterministic growth seed. One tree, rendered once; the seasoning is per instance. */
    const val SEED: Int = 0x2E5A11

    /** Axes per grown tree. The design uses 1,050 for a hero close-up; this is a landscape tree. */
    private const val MAX_JOBS = 600

    /** The design's crown gap-fill twigs, thinned for the landscape. */
    private const val FILL_TWIGS = 130

    /** Leaf budget for the sprite bake: enough to close the crown at 30–90 px on screen. */
    private const val MAX_LEAVES = 900

    private const val TWO_PI = 6.2831855f

    // Crown ellipsoid from the design (metres): a mature Paulownia tomentosa crown.
    private const val CROWN_CY = 8.35f
    private const val CROWN_RX = 5.90f
    private const val CROWN_RY = 4.55f
    private const val CROWN_RZ = 3.35f

    /** A woody limb: a polyline of `(x,y,z)` triplets with one radius per point. */
    internal class Limb(val pts: FloatArray, val rads: FloatArray, val order: Int)

    /** A growth node (where a fork or a lateral left the axis) or a terminal tip. */
    internal class Node(
        val x: Float, val y: Float, val z: Float,
        val order: Int,
        /** Radius of the wood the node sits on, metres. */
        val r: Float,
        /** The design's crown exposure: 0 deep inside the crown, ~1 on the shell. */
        val exposure: Float,
        /** 0 at the back of the crown, 1 at the front (toward the viewer). */
        val front: Float
    )

    /** The whole woody tree: every limb plus the node sets the organs hang on. */
    internal class Skeleton(
        val limbs: List<Limb>,
        val tips: List<Node>,
        val nodes: List<Node>
    )

    /** What a seasonal organ is, for the raster. */
    internal enum class OrganKind { LEAF, LEAF_AUTUMN, LEAF_UNDER, PANICLE, CAPSULE, CAPSULE_OPEN, BUD_PANICLE, SCAR, COROLLA }

    /**
     * One seasonal organ, projected.
     *
     * [length] is in metres in the tree frame; [ax] / [ay] are the organ's axis in screen space
     * (`y` down, as the sprite draws it), so the raster needs no further projection.
     */
    internal class Organ(
        val kind: OrganKind,
        val x: Float, val y: Float, val z: Float,
        val length: Float,
        val ax: Float, val ay: Float,
        val exposure: Float,
        val front: Float,
        val seed: Int
    )

    private class Job(
        val ox: Float, val oy: Float, val oz: Float,
        val dx: Float, val dy: Float, val dz: Float,
        val px: Float, val py: Float, val pz: Float,
        val len: Float, val energy: Float, val r: Float,
        val order: Int, val root: Int
    )

    /**
     * The design's linear congruential generator, same constants and same consumption order, so the
     * grown tree is the tree the design grows (given the same seeds) and is identical on every run.
     */
    private class Lcg(seed: Int) {
        private var s: Int = seed
        fun next(): Float {
            s = s * 1664525 + 1013904223
            return ((s ushr 8) and 0xFFFFFF).toFloat() / 16777216f
        }
        fun range(a: Float, b: Float): Float = a + (b - a) * next()
    }

    // ---- caching --------------------------------------------------------------------------
    // Growth and organ placement are deterministic and independent of everything but the seed and
    // the season, so both are memoised for the process: a layout change (rotation, resize) re-bakes
    // the sprite at a new size and would otherwise re-grow the tree each time.

    private val skeletonCache = HashMap<Int, Skeleton>(4)
    private val organCache = HashMap<Int, List<Organ>>(8)

    internal fun skeleton(seed: Int = SEED): Skeleton =
        skeletonCache.getOrPut(seed) { grow(seed) }

    internal fun organs(season: PaulowniaSeason, seed: Int = SEED): List<Organ> =
        organCache.getOrPut(seed * 31 + season.ordinal) { placeOrgans(season, skeleton(seed), seed) }

    // ---- crown field ----------------------------------------------------------------------

    /** Distance from the crown centre in crown radii: < 1 inside the crown, 1 on its shell. */
    private fun crownDistance(x: Float, y: Float, z: Float): Float {
        val nx = x / CROWN_RX
        val ny = (y - CROWN_CY) / CROWN_RY
        val nz = z / CROWN_RZ
        return sqrt(nx * nx + ny * ny + nz * nz)
    }

    /** The design's exposure field: how much daylight a point in the crown sees. */
    private fun exposure(x: Float, y: Float, z: Float): Float =
        (0.10f + 0.72f * crownDistance(x, y, z) +
            0.13f * (z / CROWN_RZ).coerceIn(-1f, 1f) +
            0.08f * ((y - 4f) / 9f).coerceIn(0f, 1f)).coerceIn(0f, 1.15f)

    private fun front(x: Float, y: Float, z: Float): Float =
        (0.5f + z / (2f * CROWN_RZ)).coerceIn(0f, 1f)

    // ---- small vector helpers (pure, allocation-per-call is fine: the tree is grown once) ----

    private fun nrm(x: Float, y: Float, z: Float): FloatArray {
        val l = sqrt(x * x + y * y + z * z)
        if (l < 1e-6f) return floatArrayOf(0f, 1f, 0f)
        return floatArrayOf(x / l, y / l, z / l)
    }

    /** [dir] rotated by [angle] around the axis [phi] turns out of the screen plane. */
    private fun tilt(dir: FloatArray, phi: Float, angle: Float): FloatArray {
        val rx = if (abs(dir[1]) < 0.9f) 0f else 1f
        val ry = if (abs(dir[1]) < 0.9f) 1f else 0f
        // u = normalize(dir × ref), v = dir × u
        var ux = dir[1] * 0f - dir[2] * ry
        var uy = dir[2] * rx - dir[0] * 0f
        var uz = dir[0] * ry - dir[1] * rx
        val ul = sqrt(ux * ux + uy * uy + uz * uz)
        if (ul > 1e-6f) { ux /= ul; uy /= ul; uz /= ul }
        val vx = dir[1] * uz - dir[2] * uy
        val vy = dir[2] * ux - dir[0] * uz
        val vz = dir[0] * uy - dir[1] * ux
        val c = cos(phi)
        val s = sin(phi)
        val tx = ux * c + vx * s
        val ty = uy * c + vy * s
        val tz = uz * c + vz * s
        val ca = cos(angle)
        val sa = sin(angle)
        return nrm(dir[0] * ca + tx * sa, dir[1] * ca + ty * sa, dir[2] * ca + tz * sa)
    }

    // ---- growth ---------------------------------------------------------------------------

    /**
     * Grows the design's woody tree: trunk, root flare, eight scaffold limbs, their recursive forks,
     * laterals and crown fill. Bounded to [MAX_JOBS] axes — about half the design's 1,050, because
     * at 30–90 px on screen the extra axes only add ink — and deterministic per [seed].
     *
     * Internal rather than private so the JVM contract test can call it twice and prove the
     * determinism the one-render-per-season cache depends on.
     */
    internal fun grow(seed: Int): Skeleton {
        val rnd = Lcg(seed)
        val limbs = ArrayList<Limb>(300)
        val tips = ArrayList<Node>(200)
        val nodes = ArrayList<Node>(900)
        val jobs = ArrayList<Job>(300)

        // Trunk: slightly leaning and twisting, root flare at the base, swollen fork at 2.8 m.
        limbs.add(
            Limb(
                floatArrayOf(
                    0f, 0f, 0f,
                    -0.03f, 0.38f, 0.01f,
                    0.02f, 0.95f, -0.02f,
                    0.08f, 1.65f, 0.02f,
                    0.03f, 2.25f, -0.03f,
                    0f, 2.82f, 0f,
                    0.01f, 3.35f, 0f
                ),
                floatArrayOf(0.58f, 0.39f, 0.315f, 0.30f, 0.270f, 0.205f, 0.145f),
                0
            )
        )
        // Six buttresses splaying the base into the ground.
        for (i in 0 until 6) {
            val a = i.toFloat() / 6f * TWO_PI + rnd.range(-0.16f, 0.16f)
            val cx = cos(a)
            val cz = sin(a)
            val reach = rnd.range(0.55f, 0.82f)
            limbs.add(
                Limb(
                    floatArrayOf(
                        cx * 0.18f, 0.30f, cz * 0.18f,
                        cx * reach * 0.58f, 0.085f, cz * reach * 0.58f,
                        cx * reach, 0.002f, cz * reach
                    ),
                    floatArrayOf(0.165f, 0.110f, 0.045f),
                    1
                )
            )
        }

        // The design's eight hand-placed scaffolds: origin, heading, first segment, energy budget,
        // radius, and which buttress root it belongs to.
        jobs.add(Job(0f, 2.80f, 0f, 0.05f, 0.96f, 0.02f, 0.05f, 0.96f, 0.02f, 1.95f, 15.5f, 0.140f, 1, 0))
        jobs.add(Job(-0.05f, 2.35f, 0.03f, -0.84f, 0.28f, 0.08f, -0.84f, 0.28f, 0.08f, 1.70f, 12.0f, 0.115f, 1, 1))
        jobs.add(Job(0.05f, 2.75f, -0.02f, 0.87f, 0.24f, -0.03f, 0.87f, 0.24f, -0.03f, 1.80f, 12.4f, 0.120f, 1, 2))
        jobs.add(Job(-0.02f, 3.95f, -0.03f, -0.55f, 0.70f, -0.16f, -0.55f, 0.70f, -0.16f, 1.70f, 13.0f, 0.090f, 1, 3))
        jobs.add(Job(0.03f, 4.25f, 0.02f, 0.58f, 0.68f, 0.13f, 0.58f, 0.68f, 0.13f, 1.75f, 13.2f, 0.088f, 1, 4))
        jobs.add(Job(0f, 3.65f, -0.06f, -0.10f, 0.76f, -0.50f, -0.10f, 0.76f, -0.50f, 1.60f, 12.2f, 0.082f, 1, 5))
        jobs.add(Job(-0.04f, 3.05f, 0.08f, -0.65f, 0.34f, 0.39f, -0.65f, 0.34f, 0.39f, 1.55f, 11.8f, 0.095f, 1, 6))
        jobs.add(Job(0.04f, 3.35f, 0.07f, 0.67f, 0.31f, 0.36f, 0.67f, 0.31f, 0.36f, 1.60f, 12.0f, 0.092f, 1, 7))

        var ji = 0
        var branchCount = 0
        while (ji < jobs.size && branchCount < MAX_JOBS) {
            val job = jobs[ji]
            ji++
            branchCount++
            val steps = max(
                3,
                min(8, floor(job.len / (if (job.order < 3) 0.48f else 0.30f) + 0.5f).toInt())
            )
            val stepLen = job.len / steps
            val curvePhi = rnd.range(0f, TWO_PI)
            val curveAmt = rnd.range(0.045f, 0.125f)
            val curveDir = tilt(floatArrayOf(job.dx, job.dy, job.dz), curvePhi, 1.5707964f)

            var px = job.ox
            var py = job.oy
            var pz = job.oz
            var dir = floatArrayOf(job.dx, job.dy, job.dz)

            val pts = ArrayList<Float>(steps * 3 + 3)
            pts.add(px); pts.add(py); pts.add(pz)
            val rads = ArrayList<Float>(steps + 2)
            rads.add(job.r * (if (job.order > 1) 1.22f else 1.06f))

            val energyLeftAtEnd = job.energy - job.len
            val terminalSegment = energyLeftAtEnd <= 0.62f || job.r <= 0.007f || job.order >= 8
            val taperF = when {
                terminalSegment -> 0.72f
                job.order == 1 -> 0.16f
                job.order == 2 -> 0.20f
                job.order == 3 -> 0.26f
                job.order == 4 -> 0.34f
                else -> 0.45f
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
                val f = s.toFloat() / steps
                // Wish: keep heading, remember the preferred axis, ride the curve, and pull up on
                // the outer orders (the design's limbs arch up out of the crown's foot).
                var wx = dir[0] * 0.76f + job.px * 0.24f + curveDir[0] * curveAmt *
                    (0.55f + 0.45f * sin(f * 4.2f + job.root))
                var wy = dir[1] * 0.76f + job.py * 0.24f + curveDir[1] * curveAmt *
                    (0.55f + 0.45f * sin(f * 4.2f + job.root))
                var wz = dir[2] * 0.76f + job.pz * 0.24f + curveDir[2] * curveAmt *
                    (0.55f + 0.45f * sin(f * 4.2f + job.root))
                if (job.order >= 5) wy += 0.065f
                if (f > 0.76f && job.order >= 4) wy += 0.11f
                if (py < 4.8f && job.order <= 2) {
                    val out = nrm(px, 0f, pz)
                    wx += out[0] * 0.07f
                    wz += out[2] * 0.07f
                }
                val jitter = tilt(dir, rnd.range(0f, TWO_PI), rnd.range(0.015f, 0.070f))
                wx += jitter[0]
                wy += jitter[1]
                wz += jitter[2]
                dir = nrm(wx, wy, wz)

                var nx2 = px + dir[0] * stepLen
                var ny2 = py + dir[1] * stepLen
                var nz2 = pz + dir[2] * stepLen
                // The crown shell is a wall, not a suggestion: an axis that reaches it is bent back
                // along the ellipsoid's gradient, and re-steepened when it overshoots.
                val cd = crownDistance(nx2, ny2, nz2)
                if (cd > 0.93f && job.order > 1) {
                    val gx = nx2 / (CROWN_RX * CROWN_RX)
                    val gy = (ny2 - CROWN_CY) / (CROWN_RY * CROWN_RY)
                    val gz = nz2 / (CROWN_RZ * CROWN_RZ)
                    val gl = sqrt(gx * gx + gy * gy + gz * gz)
                    if (gl > 1e-6f) {
                        val ux = gx / gl
                        val uy = gy / gl
                        val uz = gz / gl
                        val radial = dir[0] * ux + dir[1] * uy + dir[2] * uz
                        if (radial > 0f) {
                            val bend = if (cd > 1.05f) 1.12f else 0.72f
                            val bx = dir[0] - ux * radial * bend
                            val by2 = dir[1] - uy * radial * bend + 0.025f
                            val bz = dir[2] - uz * radial * bend
                            dir = nrm(bx, by2, bz)
                            val shrink = if (cd > 1.05f) 0.76f else 0.92f
                            nx2 = px + dir[0] * stepLen * shrink
                            ny2 = py + dir[1] * stepLen * shrink
                            nz2 = pz + dir[2] * stepLen * shrink
                        }
                    }
                }
                if (ny2 < 1.20f) ny2 = 1.20f + 0.10f * f
                px = nx2
                py = ny2
                pz = nz2
                val radiusAt = max(0.0038f, job.r * (1f - taperF * f.pow(0.90f)))
                pts.add(px); pts.add(py); pts.add(pz)
                rads.add(radiusAt)
                if (job.order >= 2) {
                    nodes.add(Node(px, py, pz, job.order, radiusAt, exposure(px, py, pz), front(px, py, pz)))
                }

                if (s >= 2 && s < steps && laterals < maxLaterals && job.order < 8 &&
                    jobs.size < MAX_JOBS + 80
                ) {
                    val chance = when (job.order) {
                        1 -> 0.45f
                        2 -> 0.42f
                        3 -> 0.36f
                        else -> 0.28f
                    }
                    val energyAtNode = job.energy - job.len * f
                    if (energyAtNode > 0.75f && rnd.next() < chance) {
                        val phi = rnd.range(0f, TWO_PI)
                        val pair = job.order >= 3 && rnd.next() < 0.24f
                        val nKids = if (pair) 2 else 1
                        var k = 0
                        while (k < nKids && laterals < maxLaterals) {
                            val tiltAmt = rnd.range(0.55f, 1.00f)
                            var cd2 = tilt(dir, phi + k * 3.1415927f, tiltAmt)
                            val outward = nrm(px * 0.8f, 0.20f, pz * 0.8f)
                            cd2 = nrm(
                                cd2[0] + outward[0] * 0.11f,
                                cd2[1] + outward[1] * 0.11f + 0.075f,
                                cd2[2] + outward[2] * 0.11f
                            )
                            val ce = energyAtNode * rnd.range(0.30f, 0.49f)
                            val cl = min(ce, 0.50f + ce * rnd.range(0.14f, 0.22f))
                            val cr = max(0.0062f, radiusAt * rnd.range(0.58f, 0.75f))
                            jobs.add(
                                Job(
                                    px - dir[0] * stepLen * 0.05f,
                                    py - dir[1] * stepLen * 0.05f,
                                    pz - dir[2] * stepLen * 0.05f,
                                    cd2[0], cd2[1], cd2[2],
                                    cd2[0], cd2[1], cd2[2],
                                    cl, ce, cr, job.order + 1, job.root
                                )
                            )
                            laterals++
                            k++
                        }
                    }
                }
            }

            val endR = rads[rads.size - 1]
            var continued = false
            if (!terminalSegment && energyLeftAtEnd > 0.62f && endR > 0.0058f && job.order < 8 &&
                jobs.size < MAX_JOBS + 80
            ) {
                // Endpoint forks, not tube ends: this is what carries the taper down to twigs.
                val nKids = if (job.order <= 2) 2 else if (rnd.next() < 0.88f) 2 else 1
                val phi = rnd.range(0f, TWO_PI)
                for (k in 0 until nKids) {
                    val tiltAmt = if (job.order <= 2) rnd.range(0.20f, 0.48f) else rnd.range(0.27f, 0.62f)
                    val phiK = phi + k * 3.1415927f + rnd.range(-0.24f, 0.24f)
                    var cd2 = tilt(dir, phiK, tiltAmt)
                    val outward = nrm(px * 0.65f, 0.12f, pz * 0.65f)
                    val up = if (job.order >= 4) 0.115f else 0.055f
                    cd2 = nrm(
                        cd2[0] + outward[0] * 0.055f,
                        cd2[1] + outward[1] * 0.055f + up,
                        cd2[2] + outward[2] * 0.055f
                    )
                    val ce = energyLeftAtEnd * (if (nKids == 1) rnd.range(0.95f, 1.00f) else if (k == 0) rnd.range(0.93f, 0.99f) else rnd.range(0.55f, 0.75f))
                    val cl = min(ce, 0.65f + ce * rnd.range(0.12f, 0.20f))
                    val cr = max(0.0052f, endR * (if (k == 0) rnd.range(0.82f, 0.94f) else rnd.range(0.62f, 0.80f)))
                    jobs.add(
                        Job(
                            px - dir[0] * stepLen * 0.04f,
                            py - dir[1] * stepLen * 0.04f,
                            pz - dir[2] * stepLen * 0.04f,
                            cd2[0], cd2[1], cd2[2],
                            cd2[0], cd2[1], cd2[2],
                            cl, ce, cr, job.order + 1, job.root
                        )
                    )
                    continued = true
                }
            }

            limbs.add(Limb(pts.toFloatArray(), rads.toFloatArray(), job.order))
            if (!continued) {
                tips.add(Node(px, py, pz, job.order, endR, exposure(px, py, pz), front(px, py, pz)))
            }
        }

        // Crown fill: the design's gap-filling twigs, aimed at the crown ellipsoid's shell and
        // grown from the nearest node that already exists. This is what turns a recursive
        // candelabra into a canopy — and every one of them ends in a tip, which is where the
        // season's leaves, panicles and capsules live.
        val pool = ArrayList<Node>(tips.size + nodes.size)
        pool.addAll(tips)
        for (n in nodes) if (n.order >= 3) pool.add(n)
        if (pool.isNotEmpty()) {
            for (i in 0 until FILL_TWIGS) {
                val q = rnd.next()
                val factor = if (q < 0.25f) rnd.range(0.35f, 0.65f)
                else if (q < 0.80f) rnd.range(0.65f, 0.92f)
                else rnd.range(0.92f, 1.05f)
                val zz = if (i % 3 == 0) rnd.range(0.10f, 1.0f) else rnd.range(-1.0f, 1.0f)
                val phi = rnd.range(0f, TWO_PI)
                val horiz = sqrt(max(0f, 1f - zz * zz))
                val tx = cos(phi) * horiz * CROWN_RX * factor
                val ty = CROWN_CY + zz * CROWN_RY * factor
                val tz = sin(phi) * horiz * CROWN_RZ * factor
                val bends = rnd.range(0f, TWO_PI)
                val bendAmt = rnd.range(-0.12f, 0.12f)
                val r0 = rnd.range(0.50f, 0.70f)
                val jitter = rnd.range(0.68f, 0.86f)
                if (ty < 3.05f) continue
                var best: Node? = null
                var bestD2 = Float.MAX_VALUE
                for (cand in pool) {
                    val dx = tx - cand.x
                    val dy = ty - cand.y
                    val dz = tz - cand.z
                    val d2 = dx * dx + dy * dy + dz * dz
                    if (d2 < bestD2) {
                        bestD2 = d2
                        best = cand
                    }
                }
                val node = best ?: continue
                val dist = sqrt(bestD2)
                if (dist < 0.22f || dist > 1.25f) continue
                val dir = nrm(tx - node.x, ty - node.y, tz - node.z)
                val grad = nrm(
                    (node.x) / (CROWN_RX * CROWN_RX),
                    (node.y - CROWN_CY) / (CROWN_RY * CROWN_RY),
                    (node.z) / (CROWN_RZ * CROWN_RZ)
                )
                val aim = nrm(
                    dir[0] * 0.62f + grad[0] * 0.16f,
                    dir[1] * 0.62f + grad[1] * 0.16f + 0.16f,
                    dir[2] * 0.62f + grad[2] * 0.16f
                )
                val len = dist * jitter
                val tangent = tilt(aim, bends, 1.5707964f)
                val twigR = (max(node.r, 0.012f) * r0).coerceIn(0.008f, 0.032f)
                val pts = floatArrayOf(
                    node.x, node.y, node.z,
                    node.x + aim[0] * len * 0.38f + tangent[0] * bendAmt * len,
                    node.y + aim[1] * len * 0.38f + tangent[1] * bendAmt * len,
                    node.z + aim[2] * len * 0.38f + tangent[2] * bendAmt * len,
                    node.x + aim[0] * len,
                    node.y + aim[1] * len + len * 0.075f,
                    node.z + aim[2] * len
                )
                limbs.add(Limb(pts, floatArrayOf(twigR * 1.15f, twigR * 0.62f, twigR * 0.30f), 6))
                tips.add(
                    Node(
                        pts[6], pts[7], pts[8], 6, twigR * 0.30f,
                        exposure(pts[6], pts[7], pts[8]), front(pts[6], pts[7], pts[8])
                    )
                )
            }
        }

        return Skeleton(limbs, tips, nodes)
    }

    // ---- seasonal organs -------------------------------------------------------------------

    /**
     * Places the season's organs on the grown wood, exactly where the design does.
     *
     * Two art-directed departures from botanical scale, both forced by the target: the valley's trees
     * are 25–90 px tall, where a true 5 cm bell is a third of a pixel. Panicles are drawn at 1.15× and
     * leaves at about 1.2× their measured size — enough to read as lilac spikes and heart leaves at
     * landscape distance, small enough that the crown keeps the design's proportions.
     */
    internal fun placeOrgans(season: PaulowniaSeason, skel: Skeleton, seed: Int): List<Organ> {
        val rnd = Lcg(seed * 7919 + season.ordinal * 131 + 17)
        val out = ArrayList<Organ>(900)

        // Screen-space direction of a node's heading: the orthographic projection of the design's
        // camera, +x right, +y up, +z toward the viewer.
        fun axisTo(x: Float, y: Float, z: Float): FloatArray {
            val sx = x
            val sy = -y
            val l = sqrt(sx * sx + sy * sy)
            if (l < 1e-4f) return floatArrayOf(0f, -1f)
            // An organ that points at the camera projects to nothing; bias it upward, which is what
            // the panicles actually do (they stand off the terminals).
            if (l < 0.22f) return floatArrayOf(0f, -1f)
            return floatArrayOf(sx / l, sy / l)
        }

        fun seedInt(source: Lcg): Int = (source.next() * 1_000_000f).toInt()

        fun leafAt(node: Node, scale: Float, kind: OrganKind) {
            val dir = axisTo(
                (node.x - 0f) * 0.25f + rnd.range(-0.55f, 0.55f),
                rnd.range(0.55f, 1.15f),
                (node.z - 0f) * 0.25f + rnd.range(-0.55f, 0.55f)
            )
            val len = (0.26f + 0.16f * rnd.next()) * scale
            out.add(
                Organ(
                    kind, node.x, node.y, node.z, len, dir[0], dir[1],
                    node.exposure, node.front, seedInt(rnd)
                )
            )
        }

        fun panicleAt(node: Node, scale: Float) {
            // The design's panicle axis: mostly the terminal's own heading, pulled outward and up.
            val up = rnd.range(0.55f, 1.0f)
            val dir = axisTo(
                node.x * 0.35f + rnd.range(-0.3f, 0.3f),
                up,
                node.z * 0.30f + rnd.range(-0.3f, 0.3f)
            )
            val len = (0.26f + 0.20f * rnd.next() * (0.79f + 0.34f * node.exposure)) * scale
            out.add(
                Organ(
                    OrganKind.PANICLE, node.x, node.y, node.z, len, dir[0], dir[1],
                    node.exposure, node.front, seedInt(rnd)
                )
            )
        }

        fun capsuleAt(node: Node, kind: OrganKind) {
            val dir = axisTo(
                node.x * 0.6f + rnd.range(-0.2f, 0.2f),
                rnd.range(0.25f, 0.85f),
                node.z * 0.6f + rnd.range(-0.2f, 0.2f)
            )
            val len = 0.036f + 0.022f * rnd.next()
            out.add(
                Organ(
                    kind, node.x, node.y, node.z, len, dir[0], dir[1],
                    node.exposure, node.front, seedInt(rnd)
                )
            )
        }

        fun budsAt(node: Node) {
            val dir = axisTo(
                node.x * 0.5f + rnd.range(-0.25f, 0.25f),
                rnd.range(0.6f, 1.0f),
                node.z * 0.5f + rnd.range(-0.25f, 0.25f)
            )
            out.add(
                Organ(
                    OrganKind.BUD_PANICLE, node.x, node.y, node.z, 0.16f + 0.09f * rnd.next(),
                    dir[0], dir[1], node.exposure, node.front, seedInt(rnd)
                )
            )
        }

        when (season) {
            PaulowniaSeason.SPRING -> {
                // Flowers before leaves: terminal panicles on bare wood, and a few expanding leaves.
                for (tip in skel.tips) {
                    if (rnd.next() < 0.74f) panicleAt(tip, 1.15f)
                }
                // The crown's interior blooms too: a paulownia in flower is lilac through its
                // whole mass, not just along its outline, and the inner panicles sit on shorter
                // stems (less sky to reach for inside the wood).
                for (node in skel.nodes) {
                    if (node.order >= 4 && rnd.next() < 0.46f) panicleAt(node, 0.82f)
                    if (node.order >= 6 && rnd.next() < 0.06f) leafAt(node, 0.62f, OrganKind.LEAF)
                }
                // Fallen corollas: the litter a flowering paulownia leaves under itself.
                for (i in 0 until 16) {
                    val x = rnd.range(-5.6f, 5.6f)
                    val z = rnd.range(-3.2f, 3.2f)
                    out.add(
                        Organ(
                            OrganKind.COROLLA, x, 0.02f, z, 0.055f,
                            0f, -1f, 0.9f, front(x, 0f, z), rnd.next().toBits()
                        )
                    )
                }
            }

            PaulowniaSeason.SUMMER -> {
                // Opposite cordate leaves over the whole crown; green capsules; the first rusty
                // flower-bud panicles for next spring.
                var leaves = 0
                // Legibility, not botany: a 42 cm leaf on a 13.8 m tree is right, but at 80 px on
                // screen it is under a pixel. These scales put the blades at ~1.45× so the crown
                // closes into a mass instead of dissolving into green specks.
                for (tip in skel.tips) {
                    leafAt(tip, 1.62f, OrganKind.LEAF)
                    leafAt(tip, 1.46f, if (rnd.next() < 0.14f) OrganKind.LEAF_UNDER else OrganKind.LEAF)
                    leaves += 2
                    if (rnd.next() < 0.16f) capsuleAt(tip, OrganKind.CAPSULE)
                    if (rnd.next() < 0.10f) budsAt(tip)
                }
                for (node in skel.nodes) {
                    if (leaves >= MAX_LEAVES) break
                    if (node.order < 4) continue
                    if (rnd.next() < 0.62f) {
                        leafAt(node, 1.50f, if (rnd.next() < 0.14f) OrganKind.LEAF_UNDER else OrganKind.LEAF)
                        leaves++
                    }
                }
            }

            PaulowniaSeason.AUTUMN -> {
                // The species has no golden fall colour: sparse brown/olive retained leaves, ripe
                // tan capsules splitting on the tree, and dense felted buds for the coming spring.
                var leaves = 0
                for (tip in skel.tips) {
                    if (rnd.next() < 0.42f) {
                        leafAt(tip, 1.42f, OrganKind.LEAF_AUTUMN)
                        leaves++
                    }
                    val roll = rnd.next()
                    if (roll < 0.46f) capsuleAt(tip, OrganKind.CAPSULE_OPEN)
                    else if (roll < 0.60f) budsAt(tip)
                }
                for (node in skel.nodes) {
                    if (leaves >= MAX_LEAVES / 2) break
                    if (node.order < 5) continue
                    if (rnd.next() < 0.30f) {
                        leafAt(node, 1.30f, OrganKind.LEAF_AUTUMN)
                        leaves++
                    }
                    if (rnd.next() < 0.22f) capsuleAt(node, OrganKind.CAPSULE_OPEN)
                }
            }

            PaulowniaSeason.WINTER -> {
                for (tip in skel.tips) {
                    val roll = rnd.next()
                    if (roll < 0.40f) capsuleAt(tip, OrganKind.CAPSULE_OPEN)
                    else if (roll < 0.72f) budsAt(tip)
                }
                for (node in skel.nodes) {
                    if (node.order < 5) continue
                    if (rnd.next() < 0.30f) budsAt(node)
                    if (rnd.next() < 0.22f) {
                        out.add(
                            Organ(
                                OrganKind.SCAR, node.x, node.y, node.z, 0.022f,
                                0f, -1f, node.exposure, node.front, seedInt(rnd)
                            )
                        )
                    }
                }
            }
        }
        return out
    }
}
