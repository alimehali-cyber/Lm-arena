package com.alijafari.red.astronomy.ui.rendering

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * **The Pulvar river** — the supplied `PulvarRiver3D.kt` design, brought into this landscape.
 *
 * The design is a self-contained GL ES 2.0 scene: a 224 m pool–riffle–run reach of the Pulvar with
 * an asymmetric thalweg, point bars, boulders with wakes, cobbles, reed clumps and grass tufts, lit
 * by a physical-ish model (Schlick Fresnel, Beer–Lambert absorption over a real bed, hydraulic foam
 * masks, an ACES tone map) and rendered through a fixed side camera. This file keeps the design's
 * *river* — every hydraulic law, every procedural placement, every shading term — and replaces its
 * *camera* with the one this landscape already uses: a flat, art-directed Compose canvas.
 *
 * ### What is the design's, unchanged
 *  - The twelve-knot control reach (`X0 = −112 m … X1 = +112 m`), smoothed with the design's
 *    own smoothstep interpolation, so pool/riffle spacing, depths, speeds, foam and the water's
 *    fall are the designed ones.
 *  - `centerZ` (three meander harmonics), `centerDzDx`, `halfWidth` (8.2–15.9 m), `thalwegT`,
 *    `profileDepth` (the sin^1.28 cross-section), `pointBarRaise` (inner bend, Gaussian at
 *    |t| = 0.74), `bedHeight`, the bank zonation of `terrainPoint`, and every albedo of
 *    `terrainColour`.
 *  - The scatter: 88 boulders (three riffle clusters 14/21/13, 16 in-channel, 24 on the banks),
 *    320 cobbles, up to 230 reed clumps of 7–12 blades with seed heads, 420 grass tufts — from the
 *    design's mulberry32 stream and seed, in its consumption order.
 *  - `rockWake` (bow collar, diverging bands, centre bore, lift), `surfaceSpeed01`, and the
 *    shading: `shadeOpaque` (terrain / rock / reed materials, moisture, submergence, caustics,
 *    back-light, wet sheen) and `shadeWater` (Schlick, sky/ground hemisphere reflection,
 *    Beer–Lambert `exp(−σ·path)` transmittance, waterGradient ripple field, foam, the
 *    `alpha = max(foam·0.96, transmittance·fresnel)` composite) with the design's ACES + γ 1/2.2
 *    tone map.
 *
 * ### What is this landscape's
 *  - **Projection.** The design's camera is replaced by the band this scene has always painted its
 *    river in: downstream x runs across the canvas, the channel's lateral axis runs *down* the
 *    canvas, and relief is drawn at a fixed pixel-per-metre. `PulvarRiverFrame` owns that mapping;
 *    the river's centreline waves inside the same band the old stream used, so the trees, the
 *    monuments and the planting aprons keep their relationship with the water.
 *  - **Light.** The design's `setSun` / `setMoon` / `setAmbient` inputs come from this app's own
 *    [LightingState] and the sky renderer's real Sun/Moon azimuths (see [of]).
 *  - **Season.** Reeds and grass follow the Persian calendar like both tree species: green through
 *    spring and summer, straw through autumn and winter.
 *
 * Everything here is deterministic and allocation-free in the hot paths, so the scene never
 * shimmers between clock ticks and the bake is reproducible.
 */
internal object PulvarRiver {

    // ── Reach ────────────────────────────────────────────────────────────────────────────────

    /** Upstream end of the reach, metres. East/downstream is +x, matching the design. */
    const val X0 = -112f

    /** Downstream end of the reach, metres. */
    const val X1 = 112f

    /** Length of the reach, metres. */
    const val LEN = X1 - X0

    const val PI_F = 3.1415927f

    /**
     * The design's control reach: x, depth, speed, foam, width multiplier, water level.
     * A pool → riffle → run → pool → rapids → run → pool → riffle sequence.
     */
    val REACH: Array<FloatArray> = arrayOf(
        floatArrayOf(-112f, 1.70f, 0.48f, 0.05f, 1.02f, 3.00f),
        floatArrayOf(-92f, 2.75f, 0.27f, 0.02f, 1.14f, 2.84f),
        floatArrayOf(-72f, 0.66f, 1.28f, 0.58f, 0.82f, 2.30f),
        floatArrayOf(-50f, 1.50f, 0.72f, 0.12f, 0.98f, 2.12f),
        floatArrayOf(-28f, 2.45f, 0.34f, 0.04f, 1.11f, 1.96f),
        floatArrayOf(-8f, 0.54f, 1.58f, 0.88f, 0.78f, 1.34f),
        floatArrayOf(12f, 1.42f, 0.76f, 0.18f, 0.96f, 1.16f),
        floatArrayOf(34f, 2.95f, 0.25f, 0.02f, 1.17f, 1.02f),
        floatArrayOf(58f, 0.80f, 1.16f, 0.47f, 0.86f, 0.62f),
        floatArrayOf(78f, 1.60f, 0.66f, 0.10f, 1.00f, 0.48f),
        floatArrayOf(100f, 2.25f, 0.36f, 0.03f, 1.09f, 0.36f),
        floatArrayOf(112f, 1.80f, 0.50f, 0.05f, 1.01f, 0.28f)
    )

    /** One interpolated cross-section of the reach. */
    class Reach(val depth: Float, val speed: Float, val foam: Float, val widthMul: Float, val waterY: Float)

    fun reachAt(x: Float): Reach {
        val r = REACH
        if (x <= r[0][0]) return Reach(r[0][1], r[0][2], r[0][3], r[0][4], r[0][5])
        for (i in 0 until r.size - 1) {
            if (x <= r[i + 1][0]) {
                val raw = (x - r[i][0]) / (r[i + 1][0] - r[i][0])
                val t = raw * raw * (3f - 2f * raw)
                return Reach(
                    lerp(r[i][1], r[i + 1][1], t),
                    lerp(r[i][2], r[i + 1][2], t),
                    lerp(r[i][3], r[i + 1][3], t),
                    lerp(r[i][4], r[i + 1][4], t),
                    lerp(r[i][5], r[i + 1][5], t)
                )
            }
        }
        val l = r[r.size - 1]
        return Reach(l[1], l[2], l[3], l[4], l[5])
    }

    fun progress(x: Float): Float = (x - X0) / LEN

    /** +1 where the meander bulges toward the viewer, −1 where it swings away. */
    fun bendPhase(x: Float): Float = sin((progress(x) * 4.6f + 0.7f).toDouble()).toFloat()

    /** The design's three-harmonic meander, metres of lateral offset from the reach axis. */
    fun centerZ(x: Float): Float {
        val p = progress(x)
        return -3f + (p - 0.5f) * 9f +
            10.5f * sin((p * 4.6f + 0.7f).toDouble()).toFloat() +
            5f * sin((p * 9.3f - 1.15f).toDouble()).toFloat() +
            1.8f * sin((p * 17.7f + 2.1f).toDouble()).toFloat()
    }

    fun centerDzDx(x: Float): Float {
        val p = progress(x)
        return (9f + 10.5f * 4.6f * cos((p * 4.6f + 0.7f).toDouble()).toFloat() +
            5f * 9.3f * cos((p * 9.3f - 1.15f).toDouble()).toFloat() +
            1.8f * 17.7f * cos((p * 17.7f + 2.1f).toDouble()).toFloat()) / LEN
    }

    /** Channel half-width in metres: 8.2–15.9 m, widened through pools, pinched at riffles. */
    fun halfWidth(x: Float): Float {
        val p = progress(x)
        val reach = reachAt(x)
        val base = 11.3f + 1.5f * sin((p * 3.1f - 0.4f).toDouble()).toFloat() +
            0.8f * sin((p * 7.7f + 1.8f).toDouble()).toFloat()
        return clamp(base * reach.widthMul, 8.2f, 15.9f)
    }

    fun waterY(x: Float): Float = reachAt(x).waterY

    /** Lateral position of the deep thread, as a fraction of the half-width. */
    fun thalwegT(x: Float): Float =
        0.46f * bendPhase(x) + 0.08f * sin((progress(x) * 11f + 0.4f).toDouble()).toFloat()

    fun profileDepth(x: Float, t: Float): Float {
        val reach = reachAt(x)
        val t0 = thalwegT(x)
        val denom = if (t < t0) (1f + t0) else (1f - t0)
        val q = clamp(1f - abs(t - t0) / max(denom, 0.05f), 0f, 1f)
        return reach.depth * (0.05f + 0.95f * sin((q * PI_F * 0.5f).toDouble()).toFloat().pow(1.28f))
    }

    /** The inner-bend gravel bar, metres of bed raised toward the water line. */
    fun pointBarRaise(x: Float, t: Float, depth: Float): Float {
        val bend = bendPhase(x)
        val innerSign = if (bend >= 0f) -1 else 1
        if (t * innerSign <= 0f) return 0f
        val reach = reachAt(x)
        val bendStrength = smooth(0.38f, 1f, abs(bend))
        val poolStrength = smooth(1.15f, 2.5f, reach.depth)
        val lat = exp(-((abs(t) - 0.74f).pow(2) / (2f * 0.20f * 0.20f)).toDouble()).toFloat()
        val patch = 0.78f + 0.28f * fbm2(x * 0.045f + 4f, t * 2f, 2)
        return bendStrength * poolStrength * lat * patch * (depth + 0.48f) * 1.08f
    }

    fun bedHeight(x: Float, t: Float): Float {
        val wy = waterY(x)
        val depth = profileDepth(x, t)
        val bar = pointBarRaise(x, t, depth)
        val half = halfWidth(x)
        val bedNoise = (fbm2(x * 0.31f + 8f, t * half * 0.31f - 3f, 3) - 0.5f) * 0.16f
        val edgeCalm = smooth(1f, 0.82f, abs(t))
        return wy - depth + bar + bedNoise * (0.25f + 0.75f * edgeCalm)
    }

    /**
     * The corridor surface at downstream [x] and normalised lateral [q] (`|q| ≤ 1` is the channel).
     * Returns `[x, y, z]` in the design's own world space — used for shading noise, not placement.
     */
    fun terrainPoint(x: Float, q: Float, out: FloatArray) {
        val half = halfWidth(x)
        val lateral = q * half
        val m = centerDzDx(x)
        val inv = 1f / sqrt(1f + m * m)
        val wx = x - m * inv * lateral
        val wz = centerZ(x) + inv * lateral
        val wy = waterY(x)
        val aq = abs(q)
        val y: Float
        if (aq <= 1f) {
            y = bedHeight(x, q)
        } else {
            val sign = if (q < 0f) -1f else 1f
            val edgeY = bedHeight(x, sign)
            val bend = bendPhase(x)
            val outer = if (sign == (if (bend >= 0f) 1f else -1f)) smooth(0.2f, 0.9f, abs(bend)) else 0f
            val baseBank = if (sign < 0f) 0.82f else 0.58f
            val bankH = baseBank + 0.28f * outer - 0.08f * (1f - outer)
            val immediate = (0.15f + 0.30f * outer) * smooth(1f, 1.20f, aq)
            val rise = smooth(1.02f, 1.75f, aq).pow(1.25f)
            var bankY = wy - 0.04f + immediate + bankH * rise
            bankY += (fbm2(wx * 0.035f + 2f, wz * 0.035f - 6f, 3) - 0.5f) * 0.38f * smooth(1.2f, 3.2f, aq)
            bankY += (fbm2(wx * 0.13f - 4f, wz * 0.13f + 1f, 2) - 0.5f) * 0.12f * smooth(1f, 1.5f, aq)
            y = lerp(edgeY, bankY, smooth(1f, 1.28f, aq))
        }
        out[0] = wx
        out[1] = y
        out[2] = wz
    }

    /** Just the corridor surface's height at [x]/[q] — for cross-section slopes. */
    fun terrainPointY(x: Float, q: Float, scratch: FloatArray): Float {
        terrainPoint(x, q, scratch)
        return scratch[1]
    }

    /** The design's albedo for the corridor surface, written into [out] as `[r, g, b]`. */
    fun terrainColour(x: Float, q: Float, p: FloatArray, seasonMix: Float, out: FloatArray) {
        val wy = waterY(x)
        val reach = reachAt(x)
        val aq = abs(q)
        val submerged = max(0f, wy - p[1])
        val above = max(0f, p[1] - wy)
        val n1 = fbm2(p[0] * 0.16f + 7f, p[2] * 0.16f - 2f, 3)
        val n2 = noise2(p[0] * 0.85f - 3f, p[2] * 0.85f + 5f)
        if (aq <= 1.03f && submerged > 0.03f) {
            mix3(BED_PEBBLE, BED_SAND, n1, out)
            val algae = smooth(0.12f, 0.7f, submerged) * (1f - smooth(1.2f, 2.6f, submerged)) *
                (1f - reach.foam * 0.75f)
            // The design lets algae green the bed between 0.12 m and 2.6 m of depth. Seen through
            // this band's transparent water that tint dominates every pool, so it is halved: the
            // Pulvar here reads as clear water over gravel, with algae only in the deep slack.
            mixInto(out, BED_ALGAE, algae * (0.35f + 0.45f * n1) * 0.50f)
            mixInto(out, BED_DARK, smooth(1f, 3f, submerged) * 0.72f)
        } else if (aq <= 1.10f) {
            mix3(GRAVEL, SAND_DRY, n1, out)
            mixInto(out, WET_EARTH, (1f - smooth(0.02f, 0.18f, above)) * 0.68f)
            if (n2 > 0.80f) mixInto(out, BED_PEBBLE, 0.45f)
            mixInto(out, SEDGE, smooth(0.58f, 0.88f, n1) * smooth(0.05f, 0.22f, above) * 0.38f)
        } else {
            mix3(GRAVEL, EARTH, smooth(1.10f, 1.34f, aq) * 0.60f, out)
            val grassR: Float
            val grassG: Float
            val grassB: Float
            val scratch = SCRATCH_A
            mix3(SEDGE, GRASS_LUSH, n1, scratch)
            mixInto(scratch, GRASS_DEEP, smooth(0.45f, 0.80f, n1) * 0.62f)
            val dryPatch = fbm2(p[0] * 0.028f - 6f, p[2] * 0.028f + 3f, 3)
            mixInto(scratch, GRASS_DRY, smooth(0.46f, 0.78f, dryPatch) * 0.55f)
            mixInto(scratch, EARTH, smooth(0.72f, 0.94f, fbm2(p[0] * 0.075f + 8f, p[2] * 0.075f, 2)) * 0.22f)
            // Season: the valley's own calendar, not the design's fixed summer.
            if (seasonMix > 0f) {
                val strawR = lerp(scratch[0], GRASS_DRY[0], 0.78f)
                val strawG = lerp(scratch[1], GRASS_DRY[1], 0.72f)
                val strawB = lerp(scratch[2], GRASS_DRY[2], 0.78f)
                scratch[0] = lerp(scratch[0], strawR, seasonMix)
                scratch[1] = lerp(scratch[1], strawG, seasonMix)
                scratch[2] = lerp(scratch[2], strawB, seasonMix)
            }
            grassR = scratch[0]
            grassG = scratch[1]
            grassB = scratch[2]
            val g = smooth(1.16f, 1.48f, aq)
            out[0] = lerp(out[0], grassR, g)
            out[1] = lerp(out[1], grassG, g)
            out[2] = lerp(out[2], grassB, g)
            if (n2 > 0.86f && aq < 1.75f) mixInto(out, EARTH, 0.38f)
        }
        val v = 0.90f + 0.20f * n2
        out[0] *= v
        out[1] *= v
        out[2] *= v
    }

    /** Normalised local speed, 0..1 — riffles fast and white, pools slow and glassy. */
    fun surfaceSpeed01(x: Float, t: Float): Float {
        val reach = reachAt(x)
        val t0 = thalwegT(x)
        val width = lerp(0.46f, 0.78f, reach.foam)
        val cross = 0.38f + 0.78f * exp(-((t - t0).pow(2) / (2f * width * width)).toDouble()).toFloat()
        return clamp(reach.speed * cross / 1.72f, 0.03f, 1f)
    }

    // ── Palette (the design's own albedos; sRGB, linearised in the shading) ──────────────────

    val BED_SAND = floatArrayOf(0.48f, 0.40f, 0.29f)
    val BED_PEBBLE = floatArrayOf(0.34f, 0.32f, 0.28f)
    val BED_ALGAE = floatArrayOf(0.20f, 0.31f, 0.18f)
    val BED_DARK = floatArrayOf(0.16f, 0.19f, 0.15f)
    val WET_EARTH = floatArrayOf(0.20f, 0.16f, 0.12f)
    val GRAVEL = floatArrayOf(0.46f, 0.43f, 0.36f)
    val SAND_DRY = floatArrayOf(0.55f, 0.48f, 0.35f)
    val EARTH = floatArrayOf(0.32f, 0.24f, 0.16f)
    val GRASS_LUSH = floatArrayOf(0.32f, 0.40f, 0.18f)
    val GRASS_DEEP = floatArrayOf(0.22f, 0.31f, 0.13f)
    val GRASS_DRY = floatArrayOf(0.55f, 0.52f, 0.26f)
    val SEDGE = floatArrayOf(0.35f, 0.43f, 0.20f)
    val ROCK_LIGHT = floatArrayOf(0.46f, 0.45f, 0.41f)
    val ROCK_MID = floatArrayOf(0.29f, 0.29f, 0.27f)
    val ROCK_DARK = floatArrayOf(0.15f, 0.16f, 0.15f)
    val ROCK_MOSS = floatArrayOf(0.24f, 0.36f, 0.18f)
    val REED_BASE = floatArrayOf(0.22f, 0.34f, 0.15f)
    val REED_TIP = floatArrayOf(0.39f, 0.46f, 0.20f)
    val REED_SEED = floatArrayOf(0.58f, 0.46f, 0.27f)
    val FOAM_ALBEDO = floatArrayOf(0.52f, 0.55f, 0.54f)

    /** The valley's own muted olive — where the design's spring greens are pulled toward. */
    val VEG_MUTE = floatArrayOf(0.33f, 0.36f, 0.24f)

    // Reused scratch so the bake allocates nothing per vertex.
    private val SCRATCH_A = FloatArray(3)

    // ── The design's mulberry32 stream and integer value noise ───────────────────────────────

    private const val SEED = -1317134298

    private var rngState: Int = SEED

    private fun srand(s: Int) {
        rngState = s
    }

    private fun rnd(): Float {
        rngState += 0x6D2B79F5
        var t = (rngState xor (rngState ushr 15)) * (1 or rngState)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
    }

    private fun rr(a: Float, b: Float): Float = a + (b - a) * rnd()

    fun hash3(x: Int, y: Int, z: Int): Float {
        var h = x * 374761393 + y * 668265263 + z * 1274126177
        h = (h xor (h ushr 13)) * 1103515245
        return ((h xor (h ushr 16)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967295f
    }

    fun noise3(x: Float, y: Float, z: Float): Float {
        val xi = floor(x.toDouble()).toInt()
        val yi = floor(y.toDouble()).toInt()
        val zi = floor(z.toDouble()).toInt()
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
        val u = xf * xf * (3f - 2f * xf)
        val v = yf * yf * (3f - 2f * yf)
        val w = zf * zf * (3f - 2f * zf)
        val a = lerp(hash3(xi, yi, zi), hash3(xi + 1, yi, zi), u)
        val b = lerp(hash3(xi, yi + 1, zi), hash3(xi + 1, yi + 1, zi), u)
        val c = lerp(hash3(xi, yi, zi + 1), hash3(xi + 1, yi, zi + 1), u)
        val d = lerp(hash3(xi, yi + 1, zi + 1), hash3(xi + 1, yi + 1, zi + 1), u)
        return lerp(lerp(a, b, v), lerp(c, d, v), w)
    }

    fun fbm3(x: Float, y: Float, z: Float, oct: Int): Float {
        var s = 0f
        var amp = 0.5f
        var norm = 0f
        var fx = x
        var fy = y
        var fz = z
        for (i in 0 until oct) {
            s += amp * noise3(fx, fy, fz)
            norm += amp
            fx = fx * 2.03f + 11.7f
            fy = fy * 2.01f + 7.3f
            fz = fz * 2.05f + 5.1f
            amp *= 0.5f
        }
        return s / norm
    }

    fun noise2(x: Float, z: Float): Float = noise3(x, 0f, z)

    fun fbm2(x: Float, z: Float, oct: Int): Float = fbm3(x, 0f, z, oct)

    // ── Small maths ─────────────────────────────────────────────────────────────────────────

    fun clamp(x: Float, a: Float, b: Float): Float = if (x < a) a else if (x > b) b else x

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun smooth(a: Float, b: Float, x: Float): Float {
        val t = clamp((x - a) / (b - a), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun mix3(a: FloatArray, b: FloatArray, t: Float, out: FloatArray) {
        out[0] = lerp(a[0], b[0], t)
        out[1] = lerp(a[1], b[1], t)
        out[2] = lerp(a[2], b[2], t)
    }

    /** `out = mix(out, b, t)`, in place — the design's repeated `pulvarMix3`. */
    fun mixInto(out: FloatArray, b: FloatArray, t: Float) {
        out[0] = lerp(out[0], b[0], t)
        out[1] = lerp(out[1], b[1], t)
        out[2] = lerp(out[2], b[2], t)
    }

    // ── Light ───────────────────────────────────────────────────────────────────────────────

    /**
     * The design's three light rigs, filled from this app's own sky.
     *
     * `sunCol` is the design's altitude-graded sun (`pulvarSunColorFor`: deep orange below 8°,
     * gold to 25°, near-white above, scaled `2.9 · (0.12 + 0.88 · sin^0.65 alt)`), the moon its
     * `0.34 · phase · (0.25 + 0.75 sin alt)` moonlight, and the hemisphere ambient its
     * `(0.018, 0.030, 0.065) → (0.30, 0.42, 0.58)` day/night ramp with the dusk warm mix.
     */
    class Light(
        val sunDir: FloatArray,
        val sunCol: FloatArray,
        val moonDir: FloatArray,
        val moonCol: FloatArray,
        val skyAmb: FloatArray,
        val groundAmb: FloatArray
    )

    /** The design's `pulvarDirFromAzAlt`: +x east, +y up, +z toward the camera. */
    fun dirFromAzAlt(azimuthDeg: Float, altitudeDeg: Float): FloatArray {
        val az = (azimuthDeg.toDouble() * PI / 180.0)
        val al = (altitudeDeg.toDouble() * PI / 180.0)
        return floatArrayOf(
            (sin(az) * cos(al)).toFloat(),
            sin(al).toFloat(),
            (-cos(az) * cos(al)).toFloat()
        )
    }

    fun sunColourFor(altitudeDeg: Float): FloatArray {
        if (altitudeDeg < 0f) return floatArrayOf(0f, 0f, 0f)
        val d = sin((max(altitudeDeg, 0f).toDouble() * PI / 180.0)).toFloat()
        val c: FloatArray = when {
            altitudeDeg < 8f -> lerp3(floatArrayOf(1f, 0.36f, 0.10f), floatArrayOf(1f, 0.62f, 0.28f), altitudeDeg / 8f)
            altitudeDeg < 25f -> lerp3(
                floatArrayOf(1f, 0.62f, 0.28f), floatArrayOf(1f, 0.88f, 0.66f), (altitudeDeg - 8f) / 17f
            )
            else -> lerp3(
                floatArrayOf(1f, 0.88f, 0.66f), floatArrayOf(1f, 0.96f, 0.88f), min(1f, (altitudeDeg - 25f) / 35f)
            )
        }
        val s = 2.9f * (0.12f + 0.88f * d.pow(0.65f))
        return floatArrayOf(c[0] * s, c[1] * s, c[2] * s)
    }

    fun moonColourFor(altitudeDeg: Float, phase01: Float): FloatArray {
        if (altitudeDeg < 0f) return floatArrayOf(0f, 0f, 0f)
        val i = 0.34f * phase01 * (0.25f + 0.75f * sin((max(altitudeDeg, 0f).toDouble() * PI / 180.0)).toFloat())
        return floatArrayOf(0.62f * i, 0.72f * i, 0.95f * i)
    }

    private fun lerp3(a: FloatArray, b: FloatArray, f: Float): FloatArray =
        floatArrayOf(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f)

    // ── Shading: the design's fragment shaders, at the design's reference daylight ───────────

    private const val MAT_TERRAIN = 0
    private const val MAT_ROCK = 1
    private const val MAT_PLANT = 2

    private fun wrap(nd: Float, w: Float): Float = clamp((nd + w) / (1f + w), 0f, 1f)

    private fun aces(x0: Float): Float {
        val x = clamp(x0 * 1.02f, 0f, 60f)
        return clamp((x * (2.51f * x + 0.03f)) / (x * (2.43f * x + 0.59f) + 0.14f), 0f, 1f)
    }

    /** `prTone`: ACES, 10% luminance mix, γ 1/2.2 — then packed as opaque ARGB. */
    private fun tonePacked(r: Float, g: Float, b: Float): Int {
        val tr = aces(r)
        val tg = aces(g)
        val tb = aces(b)
        val lum = 0.2126f * tr + 0.7152f * tg + 0.0722f * tb
        val er = (lerp(tr, lum, 0.10f).coerceIn(0f, 1f).pow(0.4545f) * 255f).toInt().coerceIn(0, 255)
        val eg = (lerp(tg, lum, 0.10f).coerceIn(0f, 1f).pow(0.4545f) * 255f).toInt().coerceIn(0, 255)
        val eb = (lerp(tb, lum, 0.10f).coerceIn(0f, 1f).pow(0.4545f) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (er shl 16) or (eg shl 8) or eb
    }

    /**
     * `shadeOpaque`: one corridor surface vertex or one rock/reed vertex.
     *
     * @param nx,ny,nz the surface normal (the design's `vN`).
     * @param mat 0 terrain/bed, 1 rock, 2 reed/grass.
     */
    fun shadeOpaque(
        albR: Float, albG: Float, albB: Float,
        nx: Float, ny: Float, nz: Float,
        wx: Float, wy: Float, wz: Float,
        mat: Int, moisture: Float, submerged: Float,
        light: Light
    ): Int {
        // Vegetation is pulled a third of the way to the valley's own olive: the design's greens
        // are a riverbank's saturated spring green, and at this band's size they read as neon
        // against the steppe. Boulders and terrain keep their design albedos untouched.
        val mute = if (mat > 1) 0.38f else 0f
        val sr = lerp(albR, VEG_MUTE[0], mute)
        val sg = lerp(albG, VEG_MUTE[1], mute)
        val sb = lerp(albB, VEG_MUTE[2], mute)
        var ar = sr.coerceIn(0f, 1f).pow(2.2f)
        var ag = sg.coerceIn(0f, 1f).pow(2.2f)
        var ab = sb.coerceIn(0f, 1f).pow(2.2f)
        val nMid = fbm2(wx * 1.25f + 3f, wz * 1.25f + 7f, 4)
        val nMicro = noise2(wx * 7f + wy * 3f, wz * 7f + wy * 3f)
        val nFine = noise2(wx * 23f - wy * 5f, wz * 23f - wy * 5f)
        val k = 0.84f + 0.24f * nMid + 0.10f * nMicro
        ar = min(1.6f, ar * k)
        ag = min(1.6f, ag * k)
        ab = min(1.6f, ab * k)

        if (mat == MAT_TERRAIN) {
            val pebble = smooth(0.68f, 0.82f, noise2(wx * 3.1f + 11f, wz * 3.1f + 11f))
            val pk = pebble * 0.22f * (0.35f + moisture)
            ar = lerp(ar, 0.16f, pk)
            ag = lerp(ag, 0.15f, pk)
            ab = lerp(ab, 0.13f, pk)
            val green = smooth(0.015f, 0.09f, ag - max(ar, ab))
            val streak = fbm2(wx * 0.72f, wz * 0.72f, 4) * 0.55f + noise2(wx * 3.1f, wz * 3.1f) * 0.45f
            val gk = 1f + green * (streak - 0.5f) * 0.16f
            ar *= gk
            ag *= gk
            ab *= gk
            val gpa = fbm2(wx * 0.33f + 4f, wz * 0.33f + 4f, 4)
            val gpb = noise2(wx * 2.6f - 2f, wz * 2.6f - 2f)
            val p1 = green * smooth(0.42f, 0.72f, gpa) * 0.55f
            ar = lerp(ar, 0.018f, p1)
            ag = lerp(ag, 0.052f, p1)
            ab = lerp(ab, 0.010f, p1)
            val p2 = green * smooth(0.48f, 0.82f, gpb) * 0.42f
            ar = lerp(ar, 0.105f, p2)
            ag = lerp(ag, 0.115f, p2)
            ab = lerp(ab, 0.032f, p2)
            val sk = clamp(submerged * 0.45f, 0f, 0.55f)  // wet tint only, no absorption
            ar = lerp(ar, ar * 0.72f, sk)
            ag = lerp(ag, ag * 0.82f, sk)
            ab = lerp(ab, ab * 0.78f, sk)
        } else if (mat == MAT_ROCK) {
            val strata = sin((wy * 4.2f + nMid * 4f).toDouble()).toFloat() * 0.5f + 0.5f
            val s1 = 0.92f + strata * 0.13f
            ar *= s1
            ag *= s1
            ab *= s1
            val speck = smooth(0.74f, 0.92f, nFine) * 0.28f
            ar = lerp(ar, 0.28f, speck)
            ag = lerp(ag, 0.27f, speck)
            ab = lerp(ab, 0.23f, speck)
            val wetK = 1f - moisture * 0.34f
            ar *= wetK
            ag *= wetK
            ab *= wetK
        } else {
            val p0 = 0.82f + 0.30f * nMicro + 0.12f * nFine
            ar *= p0
            ag *= p0
            ab *= p0
            val dryMask = smooth(0.03f, 0.14f, ar - ab)
            val dm = dryMask * nMid * 0.35f
            ar = lerp(ar, ar * 1.05f, dm)
            ag = lerp(ag, ag * 0.96f, dm)
            ab = lerp(ab, ab * 0.72f, dm)
        }

        // Normal.
        val nl = sqrt(nx * nx + ny * ny + nz * nz)
        val Nx = if (nl > 1e-9f) nx / nl else 0f
        val Ny = if (nl > 1e-9f) ny / nl else 1f
        val Nz = if (nl > 1e-9f) nz / nl else 0f
        val wrapAmt = if (mat > 1) 0.38f else 0.08f

        var colR: Float
        var colG: Float
        var colB: Float
        val ambMix = Ny * 0.5f + 0.5f
        val ambR = lerp(light.groundAmb[0], light.skyAmb[0], ambMix)
        val ambG = lerp(light.groundAmb[1], light.skyAmb[1], ambMix)
        val ambB = lerp(light.groundAmb[2], light.skyAmb[2], ambMix)
        val nds = wrap(Nx * light.sunDir[0] + Ny * light.sunDir[1] + Nz * light.sunDir[2], wrapAmt)
        val ndm = wrap(Nx * light.moonDir[0] + Ny * light.moonDir[1] + Nz * light.moonDir[2], wrapAmt)
        colR = ar * ambR + ar * light.sunCol[0] * nds + ar * light.moonCol[0] * ndm
        colG = ag * ambG + ag * light.sunCol[1] * nds + ag * light.moonCol[1] * ndm
        colB = ab * ambB + ab * light.sunCol[2] * nds + ab * light.moonCol[2] * ndm

        // Wet stone / wet earth sun and moon sheen.
        val gloss = if (mat < 2) clamp(moisture * 0.75f + submerged * 0.5f, 0f, 1f) else 0.06f
        val viewX = 0f
        val viewY = 0.322f
        val viewZ = 0.947f
        var hx = light.sunDir[0] + viewX
        var hy = light.sunDir[1] + viewY
        var hz = light.sunDir[2] + viewZ
        var hl = sqrt(hx * hx + hy * hy + hz * hz)
        if (hl > 1e-9f) {
            hx /= hl
            hy /= hl
            hz /= hl
            val ndh = max(Nx * hx + Ny * hy + Nz * hz, 0f).pow(72f) * gloss * 0.16f
            colR += light.sunCol[0] * ndh
            colG += light.sunCol[1] * ndh
            colB += light.sunCol[2] * ndh
        }
        hx = light.moonDir[0] + viewX
        hy = light.moonDir[1] + viewY
        hz = light.moonDir[2] + viewZ
        hl = sqrt(hx * hx + hy * hy + hz * hz)
        if (hl > 1e-9f) {
            hx /= hl
            hy /= hl
            hz /= hl
            val ndh = max(Nx * hx + Ny * hy + Nz * hz, 0f).pow(72f) * gloss * 0.20f
            colR += light.moonCol[0] * ndh
            colG += light.moonCol[1] * ndh
            colB += light.moonCol[2] * ndh
        }
        // The design darkens the bed by its own absorption *and* composites the water over it;
        // that is deliberate — it is what keeps a 2.5 m pool from reading as bright gravel behind
        // a 33%-alpha sheet. The water mesh's own Beer–Lambert term then adds the body light.
        if (submerged > 0.01f) {
            val absorb = submerged
            colR *= exp((-absorb * 0.22f).toDouble()).toFloat()
            colG *= exp((-absorb * 0.09f).toDouble()).toFloat()
            colB *= exp((-absorb * 0.06f).toDouble()).toFloat()
        }

        // Animated caustic light on the bed (the design's uTime, held at the bake's instant).
        if (mat == MAT_TERRAIN && submerged > 0.01f) {
            val c1 = noise2(wx * 1.05f, wz * 1.05f)
            val c2 = noise2(wx * 1.95f, wz * 1.95f)
            val caust = clamp(c1 + c2 - 0.92f, 0f, 1f).pow(2f)
            val depthFade = smooth(0.02f, 0.25f, submerged) * (1f - smooth(1.1f, 2.8f, submerged))
            val kS = caust * depthFade * 0.018f * max(Ny, 0f)
            val kM = caust * depthFade * 0.05f * max(Ny, 0f)
            colR += light.sunCol[0] * kS + light.moonCol[0] * kM
            colG += light.sunCol[1] * kS + light.moonCol[1] * kM
            colB += light.sunCol[2] * kS + light.moonCol[2] * kM
        }

        // Translucent back-light through reeds and grass.
        if (mat > 1) {
            val backSun = max(-(light.sunDir[0] * viewX + light.sunDir[1] * viewY + light.sunDir[2] * viewZ), 0f)
                .pow(3f) * 0.30f
            val backMoon = max(-(light.moonDir[0] * viewX + light.moonDir[1] * viewY + light.moonDir[2] * viewZ), 0f)
                .pow(3f) * 0.22f
            colR += ar * light.sunCol[0] * backSun + ar * light.moonCol[0] * backMoon
            colG += ag * light.sunCol[1] * backSun + ag * light.moonCol[1] * backMoon
            colB += ab * light.sunCol[2] * backSun + ab * light.moonCol[2] * backMoon
        }
        return tonePacked(colR, colG, colB)
    }

    /** The design's `waterGradient` at the bake's instant — the ripple slope field. */
    private fun waterGradient(
        u: Float, v: Float, speed: Float, turb: Float, out: FloatArray
    ) {
        // The design's ripple field is authored for a close-up 3D view (wavelengths of 0.5 down
        // to 0.08 m). This band is ~4.8 px per metre, where those are sub-pixel and would only
        // alias into noise. The field is evaluated 3.3× coarser and scaled back up, which keeps
        // the design's slope statistics — the same wind on the water — at wavelengths (1.6 m
        // down to 0.25 m) this canvas can actually resolve.
        val k = RIPPLE_SCALE
        val uu = u * k
        val vv = v * k
        var gx = 0f
        var gz = 0f
        // Four advected sines. uTime is the bake's instant (0), so only the spatial phase remains.
        var ph = (uu * 1f + vv * 0.18f) * 2.05f
        gx += 1f * (0.036f * 2.05f * cos(ph.toDouble()).toFloat())
        gz += 0.18f * (0.036f * 2.05f * cos(ph.toDouble()).toFloat())
        ph = (uu * 1f - vv * 0.33f) * 3.65f
        gx += 1f * (0.020f * 3.65f * cos(ph.toDouble()).toFloat())
        gz += -0.33f * (0.020f * 3.65f * cos(ph.toDouble()).toFloat())
        ph = (uu * 0.86f + vv * 0.51f) * 7.4f
        gx += 0.86f * (0.009f * 7.4f * cos(ph.toDouble()).toFloat())
        gz += 0.51f * (0.009f * 7.4f * cos(ph.toDouble()).toFloat())
        ph = (uu * 0.94f - vv * 0.62f) * 12.8f
        gx += 0.94f * (0.0045f * 12.8f * cos(ph.toDouble()).toFloat())
        gz += -0.62f * (0.0045f * 12.8f * cos(ph.toDouble()).toFloat())
        // Two advected noise scales, as finite differences.
        val e = 0.20f
        val n0 = fbm2(uu * 0.55f, vv * 1.05f, 4)
        val turbK = 0.022f * (0.45f + turb)
        gx += (fbm2(uu * 0.55f + e, vv * 1.05f, 4) - n0) / e * turbK
        gz += (fbm2(uu * 0.55f, vv * 1.05f + e, 4) - n0) / e * turbK
        val mn0 = noise2(uu * 3.1f, vv * 5f)
        val fine = 0.0035f / 0.14f
        gx += (noise2(uu * 3.1f + 0.14f, vv * 5f) - mn0) * fine
        gz += (noise2(uu * 3.1f, vv * 5f + 0.14f) - mn0) * fine
        val inv = 1f / k
        out[0] = gx * inv
        out[1] = gz * inv
    }

    /**
     * `shadeWater`: the finished water surface colour **and** its alpha over the bed.
     *
     * @param viewY the view's elevation in this projection (grazing at the far bank, steeper at
     *   the near bank — the design's camera sweep, per vertex).
     * @return packed ARGB (`alpha` is the design's composite, 0.18…0.97).
     */
    fun shadeWater(
        u: Float, v: Float, depth: Float, foamBase: Float, speed: Float, turb: Float,
        viewY: Float, grad: FloatArray, light: Light
    ): Int {
        waterGradient(u, v, speed, turb, grad)
        val gradScale = 0.18f + 0.42f * turb
        // N = normalize(up + (-grad.x, 0, -grad.y) * gradScale)
        val nx = -grad[0] * gradScale
        var ny = 1f
        val nz = -grad[1] * gradScale
        val nl = sqrt(nx * nx + ny * ny + nz * nz)
        val Nx = nx / nl
        ny /= nl
        val Nz = nz / nl
        // V: elevation viewY, azimuth toward the camera (+z).
        val vz = sqrt(max(0f, 1f - viewY * viewY))
        val ndv = max(Nx * 0f + ny * viewY + Nz * vz, 0f)
        val fres = 0.02f + 0.98f * (1f - ndv).pow(5f)

        // Mirror direction R = -V + 2·(N·V)·N.
        val rx = -0f + 2f * ndv * Nx
        val ry = -viewY + 2f * ndv * ny
        val rz = -vz + 2f * ndv * Nz
        val ryClamped = clamp(ry, -0.12f, 1f)
        val sh = smooth(-0.08f, 0.58f, ryClamped)
        // The mirror is a little warmer than a pure sky model: this valley's haze is dusty, and
        // an untinted Rayleigh sky reflected in the water read as a swimming pool.
        var skyR = lerp(light.groundAmb[0] * 0.95f, light.skyAmb[0] * 1.30f, sh)
        var skyG = lerp(light.groundAmb[1] * 0.95f, light.skyAmb[1] * 1.26f, sh)
        var skyB = lerp(light.groundAmb[2] * 0.95f, light.skyAmb[2] * 1.18f, sh)
        val sunRefl = max(rx * light.sunDir[0] + ry * light.sunDir[1] + rz * light.sunDir[2], 0f).pow(16f)
        val moonRefl = max(rx * light.moonDir[0] + ry * light.moonDir[1] + rz * light.moonDir[2], 0f).pow(16f)
        skyR += light.sunCol[0] * sunRefl * 0.16f + light.moonCol[0] * moonRefl * 0.22f
        skyG += light.sunCol[1] * sunRefl * 0.16f + light.moonCol[1] * moonRefl * 0.22f
        skyB += light.sunCol[2] * sunRefl * 0.16f + light.moonCol[2] * moonRefl * 0.22f

        // Beer–Lambert body colour over the real bed.
        val turbK = turb * 0.55f
        val sigR = lerp(0.20f, 0.38f, turbK)
        val sigG = lerp(0.075f, 0.16f, turbK)
        val sigB = lerp(0.050f, 0.10f, turbK)
        val refrCos = sqrt(max(0.05f, 1f - (1f - ndv * ndv) / 1.7689f))
        val path = depth / max(refrCos, 0.55f)
        val trR = exp((-sigR * path).toDouble()).toFloat()
        val trG = exp((-sigG * path).toDouble()).toFloat()
        val trB = exp((-sigB * path).toDouble()).toFloat()
        val deepMix = 1f - exp((-depth * 0.62f).toDouble()).toFloat()
        val scR = lerp(0.008f, 0.0008f, deepMix)
        val scG = lerp(0.045f, 0.014f, deepMix)
        val scB = lerp(0.046f, 0.032f, deepMix)
        val wlR = light.skyAmb[0] * 1.18f + light.groundAmb[0] * 0.34f +
            light.sunCol[0] * max(light.sunDir[1], 0f) * 0.30f + light.moonCol[0] * max(light.moonDir[1], 0f) * 0.30f
        val wlG = light.skyAmb[1] * 1.18f + light.groundAmb[1] * 0.34f +
            light.sunCol[1] * max(light.sunDir[1], 0f) * 0.30f + light.moonCol[1] * max(light.moonDir[1], 0f) * 0.30f
        val wlB = light.skyAmb[2] * 1.18f + light.groundAmb[2] * 0.34f +
            light.sunCol[2] * max(light.sunDir[1], 0f) * 0.30f + light.moonCol[2] * max(light.moonDir[1], 0f) * 0.30f
        val bodyR = scR * wlR * 1.22f
        val bodyG = scG * wlG * 1.22f
        val bodyB = scB * wlB * 1.22f

        val flowStreak = noise2(u * 0.10f, v * 1.05f) * 0.62f + noise2(u * 0.38f, v * 2.7f) * 0.38f
        val streakK = 0.96f + 0.08f * flowStreak
        var colR = (bodyR * (1f - fres) + skyR * fres) * streakK
        var colG = (bodyG * (1f - fres) + skyG * fres) * streakK
        var colB = (bodyB * (1f - fres) + skyB * fres) * streakK

        // The broad glitter lobe (the sharp lobe and the sparkle field are drawn live).
        val hsx = light.sunDir[0]
        val hsy = light.sunDir[1] + viewY
        val hsz = light.sunDir[2] + vz
        var hsl = sqrt(hsx * hsx + hsy * hsy + hsz * hsz)
        if (hsl > 1e-9f) {
            val ndhs = max(Nx * (hsx / hsl) + ny * (hsy / hsl) + Nz * (hsz / hsl), 0f)
            val spec = ndhs.pow(54f) * 0.08f * fres * 1.55f
            colR += light.sunCol[0] * spec
            colG += light.sunCol[1] * spec
            colB += light.sunCol[2] * spec
        }

        // Hydraulic foam over the baked mask.
        val fog = abs(grad[0]) * abs(grad[0]) + abs(grad[1]) * abs(grad[1])
        val crest = smooth(0.30f, 0.86f, sqrt(fog) * (0.75f + turb))
        val foamNoise = fbm2(u * 0.62f, v * 1.45f, 4) * 0.66f + noise2(u * 1.95f, v * 3.65f) * 0.34f
        val foamField = foamBase * (0.72f + 0.58f * foamNoise) + crest * foamBase * 0.22f
        val foam = smooth(0.60f, 0.82f, foamField + (foamNoise - 0.5f) * 0.36f)
        if (foam > 0.001f) {
            val ndsWrap = wrap(Nx * light.sunDir[0] + ny * light.sunDir[1] + Nz * light.sunDir[2], 0.42f)
            val foamLightR = light.skyAmb[0] * 0.60f + light.groundAmb[0] * 0.24f + light.sunCol[0] * ndsWrap * 0.58f
            val foamLightG = light.skyAmb[1] * 0.60f + light.groundAmb[1] * 0.24f + light.sunCol[1] * ndsWrap * 0.58f
            val foamLightB = light.skyAmb[2] * 0.60f + light.groundAmb[2] * 0.24f + light.sunCol[2] * ndsWrap * 0.58f
            val fk = foam * 0.92f
            colR = lerp(colR, FOAM_ALBEDO[0] * foamLightR, fk)
            colG = lerp(colG, FOAM_ALBEDO[1] * foamLightG, fk)
            colB = lerp(colB, FOAM_ALBEDO[2] * foamLightB, fk)
        }

        val transAvg = (trR + trG + trB) * 0.3333f
        var alpha = (1f - transAvg) * 0.88f + fres * 0.82f
        alpha *= 0.52f + 0.48f * smooth(0f, 0.30f, depth)
        // The design composites this against a fragment-lit bed at a grazing angle. In this band
        // the bed's own texture is smoothed away by the mesh, so a 36% sheet over pale gravel read
        // as fog rather than water: deep water is held opaque enough to be *water*, while the
        // shallows keep the design's transparency and show their bed.
        alpha = max(alpha, 0.30f + 0.55f * smooth(0.35f, 1.20f, depth))
        alpha = max(alpha, foam * 0.96f)
        alpha = clamp(alpha, 0.18f, 0.97f)

        val tr = aces(colR)
        val tg = aces(colG)
        val tb = aces(colB)
        val lum = 0.2126f * tr + 0.7152f * tg + 0.0722f * tb
        // The design's tone map mixes 10% of luminance back in; water takes a little more, because
        // a pure Rayleigh mirror is a far more saturated blue than anything else in this valley.
        val er = (lerp(tr, lum, 0.17f).coerceIn(0f, 1f).pow(0.4545f) * 255f).toInt().coerceIn(0, 255)
        val eg = (lerp(tg, lum, 0.17f).coerceIn(0f, 1f).pow(0.4545f) * 255f).toInt().coerceIn(0, 255)
        val eb = (lerp(tb, lum, 0.17f).coerceIn(0f, 1f).pow(0.4545f) * 255f).toInt().coerceIn(0, 255)
        val ea = (alpha * 255f).toInt().coerceIn(0, 255)
        return (ea shl 24) or (er shl 16) or (eg shl 8) or eb
    }

    // ── Details: boulders, cobbles, reeds, grass (the design's own scatter) ─────────────────

    /** One boulder. `cx/cy/cz` are its centre in world metres. */
    class Rock(
        val x: Float, val q: Float, val radius: Float, val channel: Boolean,
        val cx: Float, val cy: Float, val cz: Float,
        val rx: Float, val ry: Float, val rz: Float,
        val yaw: Float, val seed: Float, val level: Int, val waterY: Float
    )

    /** One cobble on a bar, in the shallows or on the bank. */
    class Cobble(val cx: Float, val cy: Float, val cz: Float, val rx: Float, val ry: Float, val rz: Float, val yaw: Float, val seed: Float, val waterY: Float)

    /** One reed blade. */
    class Blade(
        val x: Float, val y: Float, val z: Float, val yaw: Float, val lean: Float,
        val height: Float, val width: Float, val tipDry: Boolean, val seedVar: Float,
        val moisture: Float, val seedHead: Boolean, val seedSize: Float
    )

    class Scatter(
        val rocks: List<Rock>,
        val cobbles: List<Cobble>,
        val reeds: List<List<Blade>>,
        val reedCount: Int,
        val grasses: List<Blade>
    )

    private val scratchPoint = FloatArray(3)

    /**
     * The design's scatter, in its own RNG order: rocks (three riffle clusters, in-channel, bank),
     * cobbles, reed clumps, grass tufts.
     *
     * [seasonMix] 0 = spring/summer (green reeds, lush grass), 1 = autumn/winter (straw).
     */
    fun scatter(seasonMix: Float): Scatter {
        srand(SEED)
        val rocks = ArrayList<Rock>(96)
        fun addRock(x: Float, q: Float, radius: Float, channel: Boolean) {
            val ry = radius * rr(0.52f, 0.86f)
            val rz = radius * rr(0.65f, 1.25f)
            val yaw = rr(0f, PI_F * 2f)
            val seed = rr(0f, 100f)
            val level = if (radius > 0.92f) 3 else 2
            val half = halfWidth(x)
            val m = centerDzDx(x)
            val inv = 1f / sqrt(1f + m * m)
            val lateral = q * half
            val wx = x - m * inv * lateral
            val wz = centerZ(x) + inv * lateral
            val wy = waterY(x)
            val cy = if (channel) {
                val bed = bedHeight(x, q)
                var c = bed + ry * 0.22f
                if (radius > 1.15f) c = max(c, wy - ry * 0.34f)
                c
            } else {
                terrainPoint(x, q, scratchPoint)
                scratchPoint[1] + ry * 0.02f
            }
            rocks.add(Rock(x, q, radius, channel, wx, cy, wz, radius, ry, rz, yaw, seed, level, wy))
        }

        val riffles = arrayOf(floatArrayOf(-72f, 14f), floatArrayOf(-8f, 21f), floatArrayOf(58f, 13f))
        for (riffle in riffles) {
            val cx = riffle[0]
            val count = riffle[1].toInt()
            for (i in 0 until count) {
                val spread = ((rnd() + rnd() + rnd()) / 3f - 0.5f) * 2f
                val x = cx + spread * 11f
                val big = rnd()
                val radius = if (big > 0.86f) rr(1.7f, 2.7f) else rr(0.45f, 1.55f)
                addRock(x, rr(-0.86f, 0.86f), radius, true)
            }
        }
        for (i in 0 until 16) {
            addRock(rr(-106f, 106f), rr(-0.82f, 0.82f), rr(0.5f, 1.5f), true)
        }
        for (i in 0 until 24) {
            val side = if (rnd() < 0.5f) -1f else 1f
            addRock(rr(-108f, 108f), side * rr(0.96f, 1.32f), rr(0.65f, 2.6f), false)
        }

        val cobbles = ArrayList<Cobble>(340)
        for (i in 0 until 320) {
            val x = rr(-109f, 109f)
            val side = if (rnd() < 0.5f) -1f else 1f
            val mode = rnd()
            val q = if (mode < 0.46f) side * rr(0.68f, 1.04f) else side * rr(1.02f, 1.58f)
            terrainPoint(x, q, scratchPoint)
            val cx = scratchPoint[0]
            val groundY = scratchPoint[1]
            val cz = scratchPoint[2]
            val radius = rr(0.17f, 0.62f)
            val ry = radius * rr(0.5f, 0.82f)
            val seed = rr(0f, 100f)
            val yaw = rr(0f, PI_F * 2f)
            val rz = radius * rr(0.7f, 1.25f)
            cobbles.add(Cobble(cx, groundY + ry * 0.28f, cz, radius, ry, rz, yaw, seed, waterY(x)))
        }

        val reeds = ArrayList<List<Blade>>(240)
        var reedCount = 0
        for (i in 0 until 230) {
            val x = rr(-110f, 110f)
            val reach = reachAt(x)
            if (rnd() < reach.foam * 0.72f) continue
            val side = if (rnd() < 0.58f) 1f else -1f
            val q = side * rr(0.94f, 1.16f)
            terrainPoint(x, q, scratchPoint)
            val baseX = scratchPoint[0]
            val baseY = scratchPoint[1]
            val baseZ = scratchPoint[2]
            val wy = waterY(x)
            if (baseY < wy - 0.48f) continue
            val blades = 7 + floor((rnd() * 6f).toDouble()).toInt()
            reedCount++
            val list = ArrayList<Blade>(blades)
            for (j in 0 until blades) {
                val bx = baseX + rr(-0.58f, 0.58f)
                val bz = baseZ + rr(-0.58f, 0.58f)
                val by = baseY - 0.05f
                val yaw = rr(0f, PI_F * 2f)
                val lean = rr(-0.38f, 0.38f)
                val height = rr(0.92f, 2.15f)
                val width = rr(0.075f, 0.135f)
                val moisture = clamp(1.1f - (by - wy), 0.25f, 1f)
                val seedVar = rnd()
                val head = rnd() < 0.20f
                val headSize = if (head) rr(0.34f, 0.56f) else 0f
                list.add(
                    Blade(bx, by, bz, yaw, lean, height, width, false, seedVar, moisture,
                        head && seasonMix < 0.62f, headSize)
                )
            }
            reeds.add(list)
        }

        // The design scatters 420 tufts over the whole 35 m floodplain; this band shows only the
        // 1.12–1.62 half-width fringe of it, where the same count piles into a solid mat. The
        // distribution, sizes and blade counts are the design's — only the count is scaled to the
        // area actually in frame.
        val grasses = ArrayList<Blade>(1200)
        for (i in 0 until 260) {
            val x = rr(-110f, 110f)
            val side = if (rnd() < 0.55f) 1f else -1f
            val q = side * (if (side > 0f) rr(1.12f, 2.95f) else rr(1.12f, 1.82f))
            terrainPoint(x, q, scratchPoint)
            val baseX = scratchPoint[0]
            val baseY = scratchPoint[1]
            val baseZ = scratchPoint[2]
            val blades = 3 + floor((rnd() * 4f).toDouble()).toInt()
            for (j in 0 until blades) {
                val bx = baseX + rr(-0.28f, 0.28f)
                val bz = baseZ + rr(-0.28f, 0.28f)
                val yaw = rr(0f, PI_F * 2f)
                val lean = rr(-0.28f, 0.28f)
                val height = rr(0.24f, 0.62f)
                val width = rr(0.045f, 0.082f)
                val dry = rnd() < 0.28f
                val seedVar = rnd()
                grasses.add(Blade(bx, baseY - 0.03f, bz, yaw, lean, height, width, dry, seedVar, 0.55f, false, 0f))
            }
        }
        return Scatter(rocks, cobbles, reeds, reedCount, grasses)
    }

    /** The design's `rockWake`: bow collar, diverging bands, centre bore and lift, 0..1.2. */
    fun rockWake(x: Float, t: Float, wx: Float, wz: Float, rocks: List<Rock>, out: FloatArray) {
        var foam = 0f
        var lift = 0f
        for (r in rocks) {
            if (!r.channel) continue
            val dx = wx - r.cx
            if (dx < -r.radius * 2f || dx > r.radius * 10f) continue
            val dz = wz - r.cz
            if (abs(dz) > r.radius * 4.5f + max(0f, dx) * 0.45f) continue
            val speed = surfaceSpeed01(x, t)
            val strength = clamp(0.35f + speed * 0.95f, 0.35f, 1.25f)
            val rr2 = sqrt(dx * dx + dz * dz)
            val collarD = max(0f, rr2 - r.radius * 0.72f)
            val collar = exp(-((collarD * collarD) / (2f * (r.radius * 0.42f) * (r.radius * 0.42f))).toDouble()).toFloat()
            foam += collar * 0.85f * strength
            if (dx > 0f) {
                val vOffset = r.radius * 0.58f + dx * 0.30f
                val bandW = 0.30f + dx * 0.055f
                val band = exp(-(((abs(dz) - vOffset).pow(2)) / (2f * bandW * bandW)).toDouble()).toFloat()
                val decay = exp((-dx / (r.radius * 5.2f)).toDouble()).toFloat()
                foam += band * decay * 0.72f * strength
                val centre = exp((-(dz * dz) / (2f * (r.radius * 0.62f) * (r.radius * 0.62f))).toDouble()).toFloat() *
                    exp((-dx / (r.radius * 3.1f)).toDouble()).toFloat()
                foam += centre * 0.58f * strength
                lift += exp((-(dx - r.radius * 1.35f).pow(2) / (2f * (r.radius * 0.85f) * (r.radius * 0.85f))).toDouble()).toFloat() *
                    exp((-(dz * dz) / (2f * (r.radius * 0.95f) * (r.radius * 0.95f))).toDouble()).toFloat() * strength
            } else {
                lift += collar * 0.35f * strength
            }
        }
        out[0] = clamp(foam, 0f, 1.15f)
        out[1] = clamp(lift, 0f, 1.2f)
    }

    // ── The band this landscape draws the river in ──────────────────────────────────────────

    /**
     * Where the reach sits on the canvas.
     *
     * The old stream here was a ribbon whose half-width was `height · (0.021 … 0.046)` and whose
     * centreline waved inside `[0.895, 0.945] · height`; the whole planting code (tree exclusion,
     * the near-bank field, the gaps between the monument aprons) is built around that band. So the
     * frame keeps the band and maps the *design* into it:
     *
     *  - downstream x: the 224 m reach spans the canvas width exactly, so every riffle, boulder
     *    cluster and point bar lands at its designed fraction of the frame;
     *  - lateral z: scaled by height only, so the channel's width is constant in scene terms on
     *    every aspect ratio (`halfWidth · pxPerMetreZ` ≈ `0.0305 · height` at 11.3 m);
     *  - height y: drawn at [PX_PER_METRE_Y] — slightly above the plan scale, so the shore's
     *    stones and reeds read at this band's size instead of vanishing;
     *  - the meander: normalised to a fixed ±[SWING_FRACTION] of the canvas height, so the design's
     *    three harmonics keep their shape without ever leaving the band.
     */
    class Frame(val widthPx: Float, val heightPx: Float, anchorY: Float) {

        /**
         * Canvas y of the reach's mean centreline — the old `riverY`, a band lower.
         *
         * The old ribbon allowed its centreline up to 0.895·H. The design's channel is wider (its
         * half-width runs to 15.9 m where the old ribbon's did not), so keeping the same ceiling
         * would lift the far bank into the tree line and the planting pass would start refusing
         * groves it used to accept. 0.905·H keeps every planting below its old clearance and still
         * leaves the near bank inside the frame.
         */
        val centerLineY: Float = anchorY.coerceIn(heightPx * 0.905f, heightPx * 0.950f)

        /** Half the peak-to-peak meander, in canvas pixels. */
        val swingPx: Float = heightPx * SWING_FRACTION

        val pxPerMetreX: Float = widthPx / LEN

        /** Lateral scale: the channel's half-width is [HALF_WIDTH_FRACTION] of the canvas height. */
        val pxPerMetreZ: Float = heightPx * HALF_WIDTH_FRACTION / 11.3f

        /** Height scale for standing detail (stones, reeds, grass). */
        val pxPerMetreY: Float = heightPx * PX_PER_METRE_Y_FRACTION

        private val meanZ: Float
        private val peakZ: Float

        init {
            var sum = 0f
            var peak = 0f
            val n = 96
            for (i in 0..n) {
                val z = centerZ(X0 + LEN * i / n)
                sum += z
            }
            meanZ = sum / (n + 1)
            for (i in 0..n) {
                val d = abs(centerZ(X0 + LEN * i / n) - meanZ)
                if (d > peak) peak = d
            }
            peakZ = max(peak, 1f)
        }

        fun worldX(canvasX: Float): Float = X0 + (canvasX / widthPx) * LEN

        fun canvasX(wx: Float): Float = (wx - X0) / LEN * widthPx

        /** Canvas y of the channel's centreline at canvas x. */
        fun centerY(canvasX: Float): Float = centerYAtWorld(worldX(canvasX))

        fun centerYAtWorld(wx: Float): Float =
            centerLineY + (centerZ(wx) - meanZ) / peakZ * swingPx

        /** Canvas y at world (wx, lateral metres) — the plan projection. */
        fun lateralY(wx: Float, lateralMetres: Float): Float =
            centerYAtWorld(wx) + lateralMetres * pxPerMetreZ

        /** Channel half-width at canvas x, in pixels. */
        fun halfWidthPx(canvasX: Float): Float = halfWidth(worldX(canvasX)) * pxPerMetreZ

        /** `riverBend` for the planting code: +1 bulges toward the viewer. */
        fun bend(canvasX: Float): Float = bendPhase(worldX(canvasX))

        /** The band the corridor may occupy: `top` above the centreline, `bottom` below it. */
        val corridorTopPx: Float get() = centerLineY - swingPx - FAR_EDGE_Q * pxPerMetreZ * 11.3f
        val corridorBottomPx: Float get() = centerLineY + swingPx + NEAR_EDGE_Q * pxPerMetreZ * 11.3f
    }

    /** Meander amplitude as a fraction of canvas height (the old river used ±0.017…0.025). */
    const val SWING_FRACTION = 0.0185f

    /**
     * Channel half-width at the design's nominal 11.3 m, as a fraction of canvas height.
     *
     * The design's own half-widths run 8.2–15.9 m against the old ribbon's 6.7–14.5 m equivalent;
     * at 0.0305 the wide pools clipped the planting pass's river exclusion and the valley started
     * losing groves it had always had. 0.0280 keeps the design's *variation* (pools wide, riffles
     * pinched) while holding the corridor inside the band the trees and the near-bank field were
     * laid out against.
     */
    const val HALF_WIDTH_FRACTION = 0.0280f

    /** Ripple-field scale: the design's wavelengths, stretched to this band's resolution. */
    const val RIPPLE_SCALE = 0.30f

    /** Standing detail's vertical scale, as a fraction of canvas height per metre (≈4.5 px/m @1260). */
    const val PX_PER_METRE_Y_FRACTION = 0.0036f

    /** The corridor's visible extent, in half-widths: a sliver of far bank, a broad near one. */
    const val FAR_EDGE_Q = 1.85f
    const val NEAR_EDGE_Q = 1.62f

    /** The water's own edge, in half-widths. */
    const val WATER_EDGE_Q = 1.0f
}
