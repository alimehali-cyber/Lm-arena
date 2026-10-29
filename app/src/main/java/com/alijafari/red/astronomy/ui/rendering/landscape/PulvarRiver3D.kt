package com.alijafari.red.astronomy.ui.rendering.landscape

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean
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
 * ============================================================================
 * Pulvar River 3D — a fixed side-view river corridor for Android / Compose.
 * ============================================================================
 * Single self-contained Kotlin file. Geometry is generated procedurally into
 * static OpenGL ES 3.0 VBOs; there are no model/texture assets and no
 * WebView/Filament/SceneView dependency. [PulvarRiver3D] is the Jetpack
 * Compose entry point and hosts [PulvarRiverTextureView] through [AndroidView].
 * It deliberately does not depend on the existing 2D [PulvarRiver],
 * SceneComposition, ScenePalette or LightingState types, so it can be tried
 * beside the current landscape before replacing anything.
 *
 * The mesh is built in metres over a 224 m reach: an alternating
 * pool-riffle-run sequence, an asymmetric thalweg that swings to the outside
 * of bends, gravel point bars on inner bends, a real bed under a transparent
 * water surface, wet/dry banks, 88 boulders with baked wakes, 320 cobbles,
 * reeds and grass tufts. Current totals are 182,430 opaque vertices /
 * 164,600 triangles plus 5,778 water vertices / 10,880 triangles, uploaded
 * in <=60k-vertex chunks so 16-bit indices stay inside one draw call's range.
 *
 * Water shading is physical in structure: Schlick Fresnel with F0 = 0.02,
 * per-channel Beer-Lambert absorption over the baked local depth and
 * refracted view path, two-scale downstream-advected procedural normals,
 * Sun/Moon glitter, animated caustics only on submerged bed, and foam from
 * a baked hydraulic mask (riffle crests, shallow edges and rock wakes)
 * broken up by advected noise. Opaque bed/banks/rocks render first and the
 * water surface second, so shallows genuinely show the bed and submerged
 * rocks underneath.
 *
 * World axes: +X = east/downstream (image right), +Y = up, +Z = south
 * (toward the fixed camera). The camera is a fixed perspective side view;
 * its distance is solved from corridor key points for the current aspect.
 * There is intentionally no orbit and no zoom. Flow is animated by a phase
 * uniform: [PulvarRiverScene.flowSpeed] scales it and setting
 * [PulvarRiverScene.animated] to false (or speed to 0) freezes it for
 * on-demand rendering.
 *
 * The companion `pulvar_river_preview.html` contains the same deterministic
 * geometry builder and byte-identical GLSL in WebGL. The preview's sky is
 * only a backdrop; this component is transparent outside the river corridor.
 *
 * Compose use:
 *
 *   PulvarRiver3D(
 *       modifier = Modifier.fillMaxSize(),
 *       sunAzimuthDeg = sunAzimuthDeg,
 *       sunAltitudeDeg = sunAltitudeDeg,
 *       moonAzimuthDeg = moonAzimuthDeg,
 *       moonAltitudeDeg = moonAltitudeDeg,
 *       moonIllumination = moonIllumination01,
 *       flowSpeed = 1f,
 *       animated = true,
 *   )
 *
 * Azimuth is astronomical: 0° = N, 90° = E, 180° = S, 270° = W. Sun/Moon
 * colour and hemisphere ambient are derived from altitude (and Moon phase)
 * with the same stops as the HTML preview. Alternatively use
 * [PulvarRiverTextureView] directly, or drive [PulvarRiverScene]
 * from your own renderer: call init() on surface creation, draw() after the
 * sky each frame, and release() when the GL context goes away.
 *
 * ---------------------------------------------------------------------------
 * Hosting inside ZIG (this copy)
 * ---------------------------------------------------------------------------
 * The scene, the geometry builder, the palette, the camera solve and the shading
 * are the supplied file's, unchanged. Two host-level adaptations were needed to
 * seat the river in the Home sky canvas, and both are marked in place:
 *
 *  1. **TextureView instead of a window-level GLSurfaceView.** The original
 *     window-level view called setZOrderOnTop(true), which composites above the
 *     whole activity window: the scene readout, the sky-search scrim and the
 *     card row could never cover it, and the sky canvas's clip/scroll would not
 *     apply to it. [PulvarRiverTextureView] rasterises the same frames into a
 *     TextureView — ordinary window content, like every painted layer around
 *     it — following PersepolisTextureView in this app. The render loop runs at
 *     the sky scene's motion cadence while resumed, and on demand when frozen.
 *  2. **Premultiplied water output.** A TextureView is blended into the window
 *     assuming premultiplied alpha, so the water fragment multiplies its colour
 *     by its alpha at the end and the blend is (ONE, ONE_MINUS_SRC_ALPHA).
 *     Straight alpha would brighten every soft water edge over the landscape.
 *     The shading itself is byte-identical to the preview.
 *
 * [PulvarRiverFraming] carries the one number the Home band needs (where the
 * far bank must land for a given viewport aspect); see its documentation.
 */

// ============================================================================
// Growable primitive buffers (avoid boxing during the procedural build)
// ============================================================================

private class PulvarFloatBag(capacity: Int = 1 shl 16) {
    var data = FloatArray(capacity); private set
    var size = 0; private set
    fun add(v: Float) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = v
    }
    fun clear() { size = 0 }
    fun toFloatArray(): FloatArray = data.copyOf(size)
}

private class PulvarIntBag(capacity: Int = 1 shl 16) {
    var data = IntArray(capacity); private set
    var size = 0; private set
    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = v
    }
    fun clear() { size = 0 }
    fun toIntArray(): IntArray = data.copyOf(size)
}

// ── Deterministic RNG (mulberry32 — identical in the HTML preview) ──────────
private var pulvarSeed: Int = -1317134298
private fun pulvarSrand(s: Int) { pulvarSeed = s }
private fun pulvarRnd(): Float {
    pulvarSeed += 0x6D2B79F5
    var t = (pulvarSeed xor (pulvarSeed ushr 15)) * (1 or pulvarSeed)
    t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
    return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
}
private fun pulvarRR(a: Float, b: Float): Float = a + (b - a) * pulvarRnd()

private const val PULVAR_PI: Float = 3.1415927f

// ── Small vector helpers (plain FloatArray [x,y,z]) ─────────────────────────
private fun pulvarV3(x: Float, y: Float, z: Float): FloatArray = floatArrayOf(x, y, z)
private fun pulvarAdd(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])
private fun pulvarSub(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
private fun pulvarScale(a: FloatArray, s: Float): FloatArray = floatArrayOf(a[0] * s, a[1] * s, a[2] * s)
private fun pulvarCross(a: FloatArray, b: FloatArray): FloatArray =
    floatArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
private fun pulvarLen(a: FloatArray): Float = sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
private fun pulvarNorm(a: FloatArray): FloatArray {
    val l = pulvarLen(a)
    return if (l > 1e-9f) pulvarScale(a, 1f / l) else floatArrayOf(0f, 1f, 0f)
}
private fun pulvarClamp(x: Float, a: Float, b: Float): Float = if (x < a) a else if (x > b) b else x
private fun pulvarLerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun pulvarSmooth(a: Float, b: Float, x: Float): Float {
    val t = pulvarClamp((x - a) / (b - a), 0f, 1f)
    return t * t * (3f - 2f * t)
}
private fun pulvarMix3(a: FloatArray, b: FloatArray, t: Float): FloatArray =
    floatArrayOf(pulvarLerp(a[0], b[0], t), pulvarLerp(a[1], b[1], t), pulvarLerp(a[2], b[2], t))

// ── Integer value noise (bit-identical formulation in the preview) ──────────
private fun pulvarHash3(x: Int, y: Int, z: Int): Float {
    var h = x * 374761393 + y * 668265263 + z * 1274126177
    h = (h xor (h ushr 13)) * 1103515245
    return ((h xor (h ushr 16)).toLong() and 0xFFFFFFFFL).toFloat() / 4294967295f
}
private fun pulvarNoise3(x: Float, y: Float, z: Float): Float {
    val xi = floor(x.toDouble()).toInt(); val yi = floor(y.toDouble()).toInt(); val zi = floor(z.toDouble()).toInt()
    val xf = x - xi; val yf = y - yi; val zf = z - zi
    val u = xf * xf * (3f - 2f * xf); val v = yf * yf * (3f - 2f * yf); val w = zf * zf * (3f - 2f * zf)
    val a = pulvarLerp(pulvarHash3(xi, yi, zi), pulvarHash3(xi + 1, yi, zi), u)
    val b = pulvarLerp(pulvarHash3(xi, yi + 1, zi), pulvarHash3(xi + 1, yi + 1, zi), u)
    val c = pulvarLerp(pulvarHash3(xi, yi, zi + 1), pulvarHash3(xi + 1, yi, zi + 1), u)
    val d = pulvarLerp(pulvarHash3(xi, yi + 1, zi + 1), pulvarHash3(xi + 1, yi + 1, zi + 1), u)
    return pulvarLerp(pulvarLerp(a, b, v), pulvarLerp(c, d, v), w)
}
private fun pulvarFbm3(x: Float, y: Float, z: Float, oct: Int): Float {
    var s = 0f; var amp = 0.5f; var norm = 0f
    var fx = x; var fy = y; var fz = z
    for (i in 0 until oct) {
        s += amp * pulvarNoise3(fx, fy, fz); norm += amp
        fx = fx * 2.03f + 11.7f; fy = fy * 2.01f + 7.3f; fz = fz * 2.05f + 5.1f; amp *= 0.5f
    }
    return s / norm
}
private fun pulvarNoise2(x: Float, z: Float): Float = pulvarNoise3(x, 0f, z)
private fun pulvarFbm2(x: Float, z: Float, oct: Int): Float = pulvarFbm3(x, 0f, z, oct)

// ── Albedo palette (sRGB-ish values, converted in the shader) ───────────────
private object PulvarPal {
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
    val SHRUB = floatArrayOf(0.14f, 0.32f, 0.12f)
    val SHRUB_LIT = floatArrayOf(0.28f, 0.46f, 0.16f)
}

// ── Mesh builder ─────────────────────────────────────────────────────────────
private class PulvarMeshBuilder {
    val pos = PulvarFloatBag()
    val nor = PulvarFloatBag()
    val col = PulvarFloatBag()
    val dat = PulvarFloatBag()
    val idx = PulvarIntBag()
    var vertCount: Int = 0
        private set

    fun vertex(p: FloatArray, n: FloatArray, c: FloatArray, d: FloatArray): Int {
        pos.add(p[0]); pos.add(p[1]); pos.add(p[2])
        val nn = pulvarNorm(n); nor.add(nn[0]); nor.add(nn[1]); nor.add(nn[2])
        col.add(c[0]); col.add(c[1]); col.add(c[2])
        dat.add(d[0]); dat.add(d[1]); dat.add(d[2]); dat.add(d[3])
        return vertCount++
    }
    fun tri(a: Int, b: Int, c: Int, doubleSide: Boolean = false) {
        idx.add(a); idx.add(b); idx.add(c)
        if (doubleSide) { idx.add(a); idx.add(c); idx.add(b) }
    }
    fun quad(a: Int, b: Int, c: Int, d: Int, doubleSide: Boolean = false) {
        tri(a, b, c, doubleSide); tri(a, c, d, doubleSide)
    }
    val vertexTotal: Int get() = vertCount
    val triangleTotal: Int get() = idx.size / 3
}

// ── Cached subdivided-octahedron sphere topology (for boulders) ─────────────
private class PulvarSphereData(val verts: Array<FloatArray>, val faces: Array<IntArray>)
private val pulvarSphereCache = HashMap<Int, PulvarSphereData>()
private fun pulvarSphere(level: Int): PulvarSphereData {
    pulvarSphereCache[level]?.let { return it }
    val verts = ArrayList<FloatArray>()
    verts.add(floatArrayOf(1f, 0f, 0f)); verts.add(floatArrayOf(-1f, 0f, 0f))
    verts.add(floatArrayOf(0f, 1f, 0f)); verts.add(floatArrayOf(0f, -1f, 0f))
    verts.add(floatArrayOf(0f, 0f, 1f)); verts.add(floatArrayOf(0f, 0f, -1f))
    var faces = ArrayList<IntArray>()
    faces.add(intArrayOf(0, 2, 4)); faces.add(intArrayOf(4, 2, 1)); faces.add(intArrayOf(1, 2, 5)); faces.add(intArrayOf(5, 2, 0))
    faces.add(intArrayOf(4, 3, 0)); faces.add(intArrayOf(1, 3, 4)); faces.add(intArrayOf(5, 3, 1)); faces.add(intArrayOf(0, 3, 5))
    for (s in 0 until level) {
        val cache = HashMap<Long, Int>()
        fun mid(a: Int, b: Int): Int {
            val key = if (a < b) a * 100000L + b else b * 100000L + a
            cache[key]?.let { return it }
            val m = pulvarNorm(pulvarScale(pulvarAdd(verts[a], verts[b]), 0.5f))
            verts.add(m); val id = verts.size - 1; cache[key] = id; return id
        }
        val next = ArrayList<IntArray>()
        for (f in faces) {
            val ab = mid(f[0], f[1]); val bc = mid(f[1], f[2]); val ca = mid(f[2], f[0])
            next.add(intArrayOf(f[0], ab, ca)); next.add(intArrayOf(ab, f[1], bc))
            next.add(intArrayOf(ca, bc, f[2])); next.add(intArrayOf(ab, bc, ca))
        }
        faces = next
    }
    val out = PulvarSphereData(verts.toTypedArray(), faces.toTypedArray())
    pulvarSphereCache[level] = out
    return out
}

// Flat-shaded displaced boulder.
private fun pulvarAddRock(
    b: PulvarMeshBuilder,
    center: FloatArray,
    radii: FloatArray,
    yaw: Float,
    level: Int,
    seedOff: Float,
    colourFn: (FloatArray, FloatArray, Float) -> FloatArray,
    dataFn: (FloatArray, FloatArray, Float) -> FloatArray,
) {
    val sph = pulvarSphere(level)
    val cy = cos(yaw); val sy = sin(yaw)
    fun disp(d: FloatArray): Float {
        val n1 = pulvarFbm3(d[0] * 1.25f + seedOff, d[1] * 1.25f + seedOff * 0.7f, d[2] * 1.25f - seedOff * 0.4f, 3)
        val facet = floor((n1 * 5.0f).toDouble()).toFloat() / 5f
        val n2 = pulvarNoise3(d[0] * 4.6f - seedOff, d[1] * 4.6f, d[2] * 4.6f + seedOff)
        return 0.69f + 0.38f * facet + 0.17f * n2
    }
    fun place(d: FloatArray): FloatArray {
        val r = disp(d)
        var lx = d[0] * r * radii[0]; var ly = d[1] * r * radii[1]; val lz = d[2] * r * radii[2]
        lx += ly * ((seedOff % 1f) - 0.5f) * 0.22f
        if (ly < -radii[1] * 0.62f) ly = -radii[1] * 0.62f
        val wx = lx * cy - lz * sy; val wz = lx * sy + lz * cy
        return floatArrayOf(center[0] + wx, center[1] + ly, center[2] + wz)
    }
    var faceNo = 0
    for (f in sph.faces) {
        val d0 = sph.verts[f[0]]; val d1 = sph.verts[f[1]]; val d2 = sph.verts[f[2]]
        val p0 = place(d0); val p1 = place(d1); val p2 = place(d2)
        var n = pulvarNorm(pulvarCross(pulvarSub(p1, p0), pulvarSub(p2, p0)))
        val centroid = pulvarScale(pulvarAdd(pulvarAdd(p0, p1), p2), 1f / 3f)
        if ((centroid[0] - center[0]) * n[0] + (centroid[1] - center[1]) * n[1] + (centroid[2] - center[2]) * n[2] < 0f) {
            n = pulvarScale(n, -1f)
        }
        val fv = pulvarHash3(faceNo * 7 + (seedOff * 91f).toInt(), faceNo * 3, 17)
        val a = b.vertex(p0, n, colourFn(p0, d0, fv), dataFn(p0, d0, fv))
        val c = b.vertex(p1, n, colourFn(p1, d1, fv), dataFn(p1, d1, fv))
        val d = b.vertex(p2, n, colourFn(p2, d2, fv), dataFn(p2, d2, fv))
        b.tri(a, c, d, true)
        faceNo++
    }
}

// One tapered, gently arcing reed/grass blade (double-sided ribbon).
private fun pulvarAddBlade(
    b: PulvarMeshBuilder,
    base: FloatArray,
    yaw: Float,
    lean: Float,
    height: Float,
    width: Float,
    colBase: FloatArray,
    colTip: FloatArray,
    moisture: Float,
    seedVar: Float,
) {
    val side = floatArrayOf(cos(yaw), 0f, sin(yaw))
    val leanVec = floatArrayOf(cos(yaw + PULVAR_PI / 2f) * lean, 0f, sin(yaw + PULVAR_PI / 2f) * lean)
    val segs = 4
    var prevL = -1; var prevR = -1
    for (i in 0..segs) {
        val t = i.toFloat() / segs
        val bend = t * t
        val cx = base[0] + leanVec[0] * bend
        val cz = base[2] + leanVec[2] * bend
        val cy = base[1] + height * t - height * 0.16f * bend * abs(lean)
        val hw = width * (1f - t * 0.88f) * 0.5f
        val col = pulvarMix3(colBase, colTip, t)
        val n = pulvarNorm(floatArrayOf(-side[2] * 0.55f, 0.75f, side[0] * 0.55f))
        val d = floatArrayOf(2f, moisture, 0f, seedVar)
        val l = b.vertex(floatArrayOf(cx - side[0] * hw, cy, cz - side[2] * hw), n, col, d)
        val r = b.vertex(floatArrayOf(cx + side[0] * hw, cy, cz + side[2] * hw), n, col, d)
        if (prevL >= 0) b.quad(prevL, prevR, r, l, true)
        prevL = l; prevR = r
    }
}

// A small elongated seed head at a reed tip (two crossed tapered quads).
private fun pulvarAddSeedHead(b: PulvarMeshBuilder, tip: FloatArray, yaw: Float, size: Float, moisture: Float, seedVar: Float) {
    for (k in 0 until 2) {
        val a = yaw + k * PULVAR_PI / 2f
        val side = floatArrayOf(cos(a), 0f, sin(a))
        val w0 = size * 0.16f; val w1 = size * 0.05f
        val n = pulvarNorm(floatArrayOf(-side[2], 0.35f, side[0]))
        val d = floatArrayOf(2f, moisture, 0f, seedVar)
        val c = PulvarPal.REED_SEED
        val bl = b.vertex(floatArrayOf(tip[0] - side[0] * w0, tip[1], tip[2] - side[2] * w0), n, c, d)
        val br = b.vertex(floatArrayOf(tip[0] + side[0] * w0, tip[1], tip[2] + side[2] * w0), n, c, d)
        val tr = b.vertex(floatArrayOf(tip[0] + side[0] * w1, tip[1] + size, tip[2] + side[2] * w1), n, c, d)
        val tl = b.vertex(floatArrayOf(tip[0] - side[0] * w1, tip[1] + size, tip[2] - side[2] * w1), n, c, d)
        b.quad(bl, br, tr, tl, true)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Pulvar river scene — hydraulic layout and geometry.
//  Units are metres. x = downstream/valley direction, y = up, z = toward the
//  fixed side camera. The centreline, pool/riffle sequence, asymmetric depth,
//  point bars, rock wakes and bank zonation are all built here so this file
//  reproduces the HTML preview mesh.
// ════════════════════════════════════════════════════════════════════════════

private const val PULVAR_X0: Float = -112f
private const val PULVAR_X1: Float = 112f
private const val PULVAR_LEN: Float = PULVAR_X1 - PULVAR_X0

// Pool → riffle → run → pool → rapids → run → pool → riffle control reach.
//              x       depth  speed  foam  width×  waterY
private val PULVAR_REACH: Array<FloatArray> = arrayOf(
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
    floatArrayOf(112f, 1.80f, 0.50f, 0.05f, 1.01f, 0.28f),
)

private class PulvarReachInfo(val depth: Float, val speed: Float, val foam: Float, val width: Float, val waterY: Float)

private fun pulvarReachAt(x: Float): PulvarReachInfo {
    val r = PULVAR_REACH
    if (x <= r[0][0]) return PulvarReachInfo(r[0][1], r[0][2], r[0][3], r[0][4], r[0][5])
    for (i in 0 until r.size - 1) {
        if (x <= r[i + 1][0]) {
            val raw = (x - r[i][0]) / (r[i + 1][0] - r[i][0])
            val t = raw * raw * (3f - 2f * raw)
            return PulvarReachInfo(
                pulvarLerp(r[i][1], r[i + 1][1], t),
                pulvarLerp(r[i][2], r[i + 1][2], t),
                pulvarLerp(r[i][3], r[i + 1][3], t),
                pulvarLerp(r[i][4], r[i + 1][4], t),
                pulvarLerp(r[i][5], r[i + 1][5], t),
            )
        }
    }
    val l = r[r.size - 1]
    return PulvarReachInfo(l[1], l[2], l[3], l[4], l[5])
}

private fun pulvarProgress(x: Float): Float = (x - PULVAR_X0) / PULVAR_LEN
private fun pulvarBendPhase(x: Float): Float = sin(pulvarProgress(x) * 4.6f + 0.7f)

private fun pulvarCenterZ(x: Float): Float {
    val p = pulvarProgress(x)
    return -3f + (p - 0.5f) * 9f +
        10.5f * sin(p * 4.6f + 0.7f) +
        5f * sin(p * 9.3f - 1.15f) +
        1.8f * sin(p * 17.7f + 2.1f)
}
private fun pulvarCenterDzDx(x: Float): Float {
    val p = pulvarProgress(x)
    return (9f + 10.5f * 4.6f * cos(p * 4.6f + 0.7f) +
        5f * 9.3f * cos(p * 9.3f - 1.15f) +
        1.8f * 17.7f * cos(p * 17.7f + 2.1f)) / PULVAR_LEN
}
private fun pulvarHalfWidth(x: Float): Float {
    val p = pulvarProgress(x)
    val reach = pulvarReachAt(x)
    val base = 11.3f + 1.5f * sin(p * 3.1f - 0.4f) + 0.8f * sin(p * 7.7f + 1.8f)
    return pulvarClamp(base * reach.width, 8.2f, 15.9f)
}
private fun pulvarWaterY(x: Float): Float = pulvarReachAt(x).waterY
private fun pulvarThalwegT(x: Float): Float =
    0.46f * pulvarBendPhase(x) + 0.08f * sin(pulvarProgress(x) * 11f + 0.4f)
private fun pulvarChannelPointXZ(x: Float, lateralMetres: Float): FloatArray {
    val m = pulvarCenterDzDx(x)
    val inv = 1f / sqrt(1f + m * m)
    return floatArrayOf(x - m * inv * lateralMetres, pulvarCenterZ(x) + inv * lateralMetres)
}

private fun pulvarProfileDepth(x: Float, t: Float): Float {
    val reach = pulvarReachAt(x)
    val t0 = pulvarThalwegT(x)
    val denom = if (t < t0) (1f + t0) else (1f - t0)
    val q = pulvarClamp(1f - abs(t - t0) / max(denom, 0.05f), 0f, 1f)
    return reach.depth * (0.05f + 0.95f * sin(q * PULVAR_PI * 0.5f).pow(1.28f))
}

private fun pulvarPointBarRaise(x: Float, t: Float, depth: Float): Float {
    val bend = pulvarBendPhase(x)
    val innerSign = if (bend >= 0f) -1 else 1
    if (t * innerSign <= 0f) return 0f
    val reach = pulvarReachAt(x)
    val bendStrength = pulvarSmooth(0.38f, 1f, abs(bend))
    val poolStrength = pulvarSmooth(1.15f, 2.5f, reach.depth)
    val lat = exp(-((abs(t) - 0.74f).pow(2) / (2f * 0.20f * 0.20f)).toDouble()).toFloat()
    val patch = 0.78f + 0.28f * pulvarFbm2(x * 0.045f + 4f, t * 2f, 2)
    return bendStrength * poolStrength * lat * patch * (depth + 0.48f) * 1.08f
}

private fun pulvarBedHeight(x: Float, t: Float): Float {
    val waterY = pulvarWaterY(x)
    val depth = pulvarProfileDepth(x, t)
    val bar = pulvarPointBarRaise(x, t, depth)
    val half = pulvarHalfWidth(x)
    val bedNoise = (pulvarFbm2(x * 0.31f + 8f, t * half * 0.31f - 3f, 3) - 0.5f) * 0.16f
    val edgeCalm = pulvarSmooth(1f, 0.82f, abs(t))
    return waterY - depth + bar + bedNoise * (0.25f + 0.75f * edgeCalm)
}

private fun pulvarTerrainPoint(x: Float, q: Float): FloatArray {
    val half = pulvarHalfWidth(x)
    val xz = pulvarChannelPointXZ(x, q * half)
    val wx = xz[0]; val wz = xz[1]
    val waterY = pulvarWaterY(x)
    val aq = abs(q)
    val y: Float
    if (aq <= 1f) {
        y = pulvarBedHeight(x, q)
    } else {
        val sign = if (q < 0f) -1 else 1
        val edgeY = pulvarBedHeight(x, sign * 1f)
        val bend = pulvarBendPhase(x)
        val outer = if (sign == (if (bend >= 0f) 1 else -1)) pulvarSmooth(0.2f, 0.9f, abs(bend)) else 0f
        val baseBank = if (sign < 0) 0.82f else 0.58f
        val bankH = baseBank + 0.28f * outer - 0.08f * (1f - outer)
        val immediate = (0.15f + 0.30f * outer) * pulvarSmooth(1f, 1.20f, aq)
        val rise = pulvarSmooth(1.02f, 1.75f, aq).pow(1.25f)
        var bankY = waterY - 0.04f + immediate + bankH * rise
        bankY += (pulvarFbm2(wx * 0.035f + 2f, wz * 0.035f - 6f, 3) - 0.5f) * 0.38f * pulvarSmooth(1.2f, 3.2f, aq)
        bankY += (pulvarFbm2(wx * 0.13f - 4f, wz * 0.13f + 1f, 2) - 0.5f) * 0.12f * pulvarSmooth(1f, 1.5f, aq)
        y = pulvarLerp(edgeY, bankY, pulvarSmooth(1f, 1.28f, aq))
    }
    return floatArrayOf(wx, y, wz)
}

private fun pulvarTerrainColour(x: Float, q: Float, p: FloatArray): FloatArray {
    val waterY = pulvarWaterY(x)
    val reach = pulvarReachAt(x)
    val aq = abs(q)
    val submerged = max(0f, waterY - p[1])
    val above = max(0f, p[1] - waterY)
    val n1 = pulvarFbm2(p[0] * 0.16f + 7f, p[2] * 0.16f - 2f, 3)
    val n2 = pulvarNoise2(p[0] * 0.85f - 3f, p[2] * 0.85f + 5f)
    var col: FloatArray
    if (aq <= 1.03f && submerged > 0.03f) {
        col = pulvarMix3(PulvarPal.BED_PEBBLE, PulvarPal.BED_SAND, n1)
        val algae = pulvarSmooth(0.12f, 0.7f, submerged) * (1f - pulvarSmooth(1.2f, 2.6f, submerged)) * (1f - reach.foam * 0.75f)
        col = pulvarMix3(col, PulvarPal.BED_ALGAE, algae * (0.35f + 0.45f * n1))
        col = pulvarMix3(col, PulvarPal.BED_DARK, pulvarSmooth(1f, 3f, submerged) * 0.72f)
    } else if (aq <= 1.10f) {
        col = pulvarMix3(PulvarPal.GRAVEL, PulvarPal.SAND_DRY, n1)
        col = pulvarMix3(col, PulvarPal.WET_EARTH, (1f - pulvarSmooth(0.02f, 0.18f, above)) * 0.68f)
        if (n2 > 0.80f) col = pulvarMix3(col, PulvarPal.BED_PEBBLE, 0.45f)
        col = pulvarMix3(col, PulvarPal.SEDGE, pulvarSmooth(0.58f, 0.88f, n1) * pulvarSmooth(0.05f, 0.22f, above) * 0.38f)
    } else {
        col = pulvarMix3(PulvarPal.GRAVEL, PulvarPal.EARTH, pulvarSmooth(1.10f, 1.34f, aq) * 0.60f)
        var grass = pulvarMix3(PulvarPal.SEDGE, PulvarPal.GRASS_LUSH, n1)
        grass = pulvarMix3(grass, PulvarPal.GRASS_DEEP, pulvarSmooth(0.45f, 0.80f, n1) * 0.62f)
        val dryPatch = pulvarFbm2(p[0] * 0.028f - 6f, p[2] * 0.028f + 3f, 3)
        grass = pulvarMix3(grass, PulvarPal.GRASS_DRY, pulvarSmooth(0.46f, 0.78f, dryPatch) * 0.55f)
        grass = pulvarMix3(grass, PulvarPal.EARTH, pulvarSmooth(0.72f, 0.94f, pulvarFbm2(p[0] * 0.075f + 8f, p[2] * 0.075f, 2)) * 0.22f)
        col = pulvarMix3(col, grass, pulvarSmooth(1.16f, 1.48f, aq))
        if (n2 > 0.86f && aq < 1.75f) col = pulvarMix3(col, PulvarPal.EARTH, 0.38f)
    }
    val v = 0.90f + 0.20f * n2
    return floatArrayOf(col[0] * v, col[1] * v, col[2] * v)
}

private fun pulvarSurfaceSpeed01(x: Float, t: Float): Float {
    val reach = pulvarReachAt(x)
    val t0 = pulvarThalwegT(x)
    val width = pulvarLerp(0.46f, 0.78f, reach.foam)
    val cross = 0.38f + 0.78f * exp(-((t - t0).pow(2) / (2f * width * width)).toDouble()).toFloat()
    return pulvarClamp(reach.speed * cross / 1.72f, 0.03f, 1f)
}

private class PulvarGridPoint(val p: FloatArray, val c: FloatArray, val d: FloatArray)

// Grid writer with smooth central-difference normals.
private fun pulvarAddGrid(b: PulvarMeshBuilder, rows: Array<Array<PulvarGridPoint>>) {
    val ids = Array(rows.size) { IntArray(rows[it].size) }
    for (i in rows.indices) {
        for (j in rows[i].indices) {
            val i0 = max(0, i - 1); val i1 = min(rows.size - 1, i + 1)
            val j0 = max(0, j - 1); val j1 = min(rows[i].size - 1, j + 1)
            val tx = pulvarSub(rows[i1][j].p, rows[i0][j].p)
            val tz = pulvarSub(rows[i][j1].p, rows[i][j0].p)
            var n = pulvarNorm(pulvarCross(tz, tx))
            if (n[1] < 0f) n = pulvarScale(n, -1f)
            ids[i][j] = b.vertex(rows[i][j].p, n, rows[i][j].c, rows[i][j].d)
        }
    }
    for (i in 0 until rows.size - 1) {
        for (j in 0 until rows[i].size - 1) {
            b.quad(ids[i][j], ids[i][j + 1], ids[i + 1][j + 1], ids[i + 1][j])
        }
    }
}

private class PulvarRockDesc(
    val x: Float,
    val q: Float,
    val radius: Float,
    val channel: Boolean,
    val center: FloatArray,
    val radii: FloatArray,
    val yaw: Float,
    val seed: Float,
    val level: Int,
    val waterY: Float,
)

private fun pulvarMakeRockDescriptors(): ArrayList<PulvarRockDesc> {
    val rocks = ArrayList<PulvarRockDesc>()
    fun addRock(x: Float, q: Float, radius: Float, channel: Boolean) {
        val ry = radius * pulvarRR(0.52f, 0.86f)
        val rz = radius * pulvarRR(0.65f, 1.25f)
        val yaw = pulvarRR(0f, PULVAR_PI * 2f)
        val seed = pulvarRR(0f, 100f)
        val level = if (radius > 0.92f) 3 else 2
        val half = pulvarHalfWidth(x)
        val xz = pulvarChannelPointXZ(x, q * half)
        val waterY = pulvarWaterY(x)
        var centerY: Float
        if (channel) {
            val bed = pulvarBedHeight(x, q)
            centerY = bed + ry * 0.22f
            if (radius > 1.15f) centerY = max(centerY, waterY - ry * 0.34f)
        } else {
            val ground = pulvarTerrainPoint(x, q)
            centerY = ground[1] + ry * 0.02f
        }
        rocks.add(PulvarRockDesc(x, q, radius, channel, floatArrayOf(xz[0], centerY, xz[1]), floatArrayOf(radius, ry, rz), yaw, seed, level, waterY))
    }

    val riffles = arrayOf(floatArrayOf(-72f, 14f), floatArrayOf(-8f, 21f), floatArrayOf(58f, 13f))
    for (riffle in riffles) {
        val cx = riffle[0]; val count = riffle[1].toInt()
        for (i in 0 until count) {
            val spread = ((pulvarRnd() + pulvarRnd() + pulvarRnd()) / 3f - 0.5f) * 2f
            val x = cx + spread * 11f
            val big = pulvarRnd()
            val radius = if (big > 0.86f) pulvarRR(1.7f, 2.7f) else pulvarRR(0.45f, 1.55f)
            addRock(x, pulvarRR(-0.86f, 0.86f), radius, true)
        }
    }
    for (i in 0 until 16) {
        addRock(pulvarRR(-106f, 106f), pulvarRR(-0.82f, 0.82f), pulvarRR(0.5f, 1.5f), true)
    }
    for (i in 0 until 24) {
        val side = if (pulvarRnd() < 0.5f) -1f else 1f
        addRock(pulvarRR(-108f, 108f), side * pulvarRR(0.96f, 1.32f), pulvarRR(0.65f, 2.6f), false)
    }
    return rocks
}

private fun pulvarRockWake(x: Float, t: Float, wx: Float, wz: Float, rocks: ArrayList<PulvarRockDesc>): FloatArray {
    var foam = 0f; var lift = 0f
    for (r in rocks) {
        if (!r.channel) continue
        val dx = wx - r.center[0]
        if (dx < -r.radius * 2f || dx > r.radius * 10f) continue
        val dz = wz - r.center[2]
        if (abs(dz) > r.radius * 4.5f + max(0f, dx) * 0.45f) continue
        val speed = pulvarSurfaceSpeed01(x, t)
        val strength = pulvarClamp(0.35f + speed * 0.95f, 0.35f, 1.25f)
        val rr = sqrt(dx * dx + dz * dz)
        val collar = exp(-(max(0f, rr - r.radius * 0.72f).pow(2) / (2f * (r.radius * 0.42f).pow(2))).toDouble()).toFloat()
        foam += collar * 0.85f * strength
        if (dx > 0f) {
            val vOffset = r.radius * 0.58f + dx * 0.30f
            val band = exp(-((abs(dz) - vOffset).pow(2) / (2f * (0.30f + dx * 0.055f).pow(2))).toDouble()).toFloat()
            val decay = exp((-(dx / (r.radius * 5.2f))).toDouble()).toFloat()
            foam += band * decay * 0.72f * strength
            val centre = exp((-(dz * dz) / (2f * (r.radius * 0.62f).pow(2))).toDouble()).toFloat() * exp((-(dx / (r.radius * 3.1f))).toDouble()).toFloat()
            foam += centre * 0.58f * strength
            lift += exp(-((dx - r.radius * 1.35f).pow(2) / (2f * (r.radius * 0.85f).pow(2))).toDouble()).toFloat() *
                exp((-(dz * dz) / (2f * (r.radius * 0.95f).pow(2))).toDouble()).toFloat() * strength
        } else {
            lift += collar * 0.35f * strength
        }
    }
    return floatArrayOf(pulvarClamp(foam, 0f, 1.15f), pulvarClamp(lift, 0f, 1.2f))
}

class PulvarRiverCounts(
    val rocks: Int,
    val pebbles: Int,
    val reedClumps: Int,
    val reedBlades: Int,
    val grassTufts: Int,
    val opaqueVerts: Int,
    val opaqueTris: Int,
    val waterVerts: Int,
    val waterTris: Int,
)

private class PulvarRiverGeometry(
    val opaque: PulvarMeshBuilder,
    val water: PulvarMeshBuilder,
    val counts: PulvarRiverCounts,
)

private fun buildPulvarRiverScene(): PulvarRiverGeometry {
    pulvarSrand(-1317134298)
    val opaque = PulvarMeshBuilder()
    val water = PulvarMeshBuilder()
    val rocks = pulvarMakeRockDescriptors()

    // ── Terrain: bed + point bars + wet banks + grassy floodplain ─────────────
    val qNodes = floatArrayOf(-1.9f, -1.68f, -1.5f, -1.34f, -1.21f, -1.12f, -1.02f, -0.94f, -0.8f, -0.62f, -0.42f, -0.22f, 0f,
        0.22f, 0.42f, 0.62f, 0.8f, 0.94f, 1.02f, 1.12f, 1.21f, 1.34f, 1.5f, 1.68f, 1.9f, 2.2f, 2.55f, 3.1f)
    val TX = 240
    val terrainRows = Array(TX + 1) { i ->
        val x = pulvarLerp(PULVAR_X0, PULVAR_X1, i.toFloat() / TX)
        val waterY = pulvarWaterY(x)
        Array(qNodes.size) { j ->
            val q = qNodes[j]
            val p = pulvarTerrainPoint(x, q)
            val c = pulvarTerrainColour(x, q, p)
            val submerged = max(0f, waterY - p[1])
            val moisture = pulvarClamp(1.12f - max(0f, p[1] - waterY) * 1.05f - max(0f, abs(q) - 1f) * 0.16f, 0f, 1f)
            val variation = pulvarNoise2(p[0] * 0.43f + 9f, p[2] * 0.43f - 4f)
            PulvarGridPoint(p, c, floatArrayOf(0f, moisture, submerged, variation))
        }
    }
    pulvarAddGrid(opaque, terrainRows)

    // ── Water surface: depth/speed/foam are baked per-vertex ──────────────────
    val tNodes = floatArrayOf(-1f, -0.93f, -0.84f, -0.73f, -0.60f, -0.46f, -0.32f, -0.18f, -0.06f, 0.06f, 0.18f, 0.32f, 0.46f, 0.60f, 0.73f, 0.84f, 0.93f, 1f)
    val WX = 320
    val waterRows = Array(WX + 1) { i ->
        val x = pulvarLerp(PULVAR_X0, PULVAR_X1, i.toFloat() / WX)
        val reach = pulvarReachAt(x)
        val half = pulvarHalfWidth(x)
        val waterY = reach.waterY
        Array(tNodes.size) { j ->
            val t = tNodes[j]
            val lateral = t * half
            val xz = pulvarChannelPointXZ(x, lateral)
            val bed = pulvarBedHeight(x, t)
            val depth = max(0.012f, waterY - bed)
            val wake = pulvarRockWake(x, t, xz[0], xz[1], rocks)
            val u = x * 1.015f + pulvarCenterZ(x) * 0.07f
            val edgeEnv = max(0f, sin((t + 1f) * PULVAR_PI * 0.5f)).pow(0.65f)
            val amp = (0.010f + 0.082f * reach.foam + 0.105f * wake[1]) * edgeEnv
            val phase = u * 2.35f - abs(lateral) * 0.48f + 1.7f * (pulvarNoise2(u * 0.11f, lateral * 0.11f) - 0.5f)
            val y = waterY + 0.018f * sin(u * 0.33f + lateral * 0.16f) +
                0.011f * sin(u * 0.83f - lateral * 0.30f) +
                sin(phase) * amp + sin(u * 4.7f + lateral * 1.3f) * amp * 0.32f
            val crest = pulvarClamp((sin(phase) - 0.25f) * 1.4f, 0f, 1f)
            var foam = reach.foam * (0.08f + 0.25f * crest + 0.12f * pulvarSurfaceSpeed01(x, t))
            foam += (1f - pulvarSmooth(0.03f, 0.30f, depth)) * 0.09f
            foam += wake[0] * 0.68f
            foam *= 0.72f + 0.28f * pulvarFbm2(u * 0.20f + 3f, lateral * 0.20f, 2)
            val speed = pulvarSurfaceSpeed01(x, t)
            PulvarGridPoint(
                floatArrayOf(xz[0], y, xz[1]),
                floatArrayOf(speed, pulvarClamp(reach.foam * 1.15f + speed * 0.35f + wake[0] * 0.35f, 0f, 1f), pulvarNoise2(u * 0.07f, lateral * 0.07f)),
                floatArrayOf(u, lateral, depth, pulvarClamp(foam, 0f, 1f)),
            )
        }
    }
    pulvarAddGrid(water, waterRows)

    // ── Boulders ───────────────────────────────────────────────────────────────
    for (r in rocks) {
        val colourFn: (FloatArray, FloatArray, Float) -> FloatArray = { p, dir, fv ->
            var col = pulvarMix3(PulvarPal.ROCK_MID, PulvarPal.ROCK_LIGHT, fv)
            col = pulvarMix3(col, PulvarPal.ROCK_DARK, pulvarSmooth(0.62f, 0.92f, pulvarNoise3(p[0] * 0.35f, p[1] * 0.35f, p[2] * 0.35f)) * 0.55f)
            val moss = pulvarSmooth(0.18f, 0.72f, dir[1]) * pulvarSmooth(0.52f, 0.82f, pulvarNoise3(p[0] * 0.5f + 4f, p[1] * 0.5f, p[2] * 0.5f))
            col = pulvarMix3(col, PulvarPal.ROCK_MOSS, moss * 0.58f)
            val wet = 1f - pulvarSmooth(r.waterY - 0.04f, r.waterY + 0.40f, p[1])
            col = pulvarMix3(col, PulvarPal.ROCK_DARK, wet * 0.62f)
            col
        }
        val dataFn: (FloatArray, FloatArray, Float) -> FloatArray = { p, _, _ ->
            val wet = 1f - pulvarSmooth(r.waterY - 0.04f, r.waterY + 0.40f, p[1])
            floatArrayOf(1f, wet, max(0f, r.waterY - p[1]), pulvarNoise3(p[0] * 0.7f, p[1] * 0.7f, p[2] * 0.7f))
        }
        pulvarAddRock(opaque, r.center, r.radii, r.yaw, r.level, r.seed, colourFn, dataFn)
    }

    // ── Pebbles/cobbles on bars, shallow edges and banks ─────────────────────
    var pebbleCount = 0
    for (i in 0 until 320) {
        val x = pulvarRR(-109f, 109f)
        val side = if (pulvarRnd() < 0.5f) -1f else 1f
        val mode = pulvarRnd()
        val q = if (mode < 0.46f) side * pulvarRR(0.68f, 1.04f) else side * pulvarRR(1.02f, 1.58f)
        val ground = pulvarTerrainPoint(x, q)
        val radius = pulvarRR(0.17f, 0.62f)
        val ry = radius * pulvarRR(0.5f, 0.82f)
        val center = floatArrayOf(ground[0], ground[1] + ry * 0.28f, ground[2])
        val waterY = pulvarWaterY(x)
        val seed = pulvarRR(0f, 100f)
        val yaw = pulvarRR(0f, PULVAR_PI * 2f)
        val rz = radius * pulvarRR(0.7f, 1.25f)
        val colourFn: (FloatArray, FloatArray, Float) -> FloatArray = { p, _, fv ->
            var col = pulvarMix3(PulvarPal.BED_PEBBLE, PulvarPal.GRAVEL, fv)
            if (fv > 0.78f) col = pulvarMix3(col, PulvarPal.BED_SAND, 0.5f)
            val wet = 1f - pulvarSmooth(waterY - 0.03f, waterY + 0.22f, p[1])
            pulvarMix3(col, PulvarPal.ROCK_DARK, wet * 0.5f)
        }
        val dataFn: (FloatArray, FloatArray, Float) -> FloatArray = { p, _, _ ->
            floatArrayOf(1f, 1f - pulvarSmooth(waterY - 0.03f, waterY + 0.22f, p[1]), max(0f, waterY - p[1]), seed / 100f)
        }
        pulvarAddRock(opaque, center, floatArrayOf(radius, ry, rz), yaw, 1, seed, colourFn, dataFn)
        pebbleCount++
    }

    // ── Reeds and sedges at the waterline ────────────────────────────────────
    var reedClumps = 0; var reedBlades = 0
    for (i in 0 until 230) {
        val x = pulvarRR(-110f, 110f)
        val reach = pulvarReachAt(x)
        if (pulvarRnd() < reach.foam * 0.72f) continue
        val side = if (pulvarRnd() < 0.58f) 1f else -1f
        val q = side * pulvarRR(0.94f, 1.16f)
        val base = pulvarTerrainPoint(x, q)
        if (base[1] < pulvarWaterY(x) - 0.48f) continue
        val blades = 7 + floor((pulvarRnd() * 6f).toDouble()).toInt()
        reedClumps++
        for (j in 0 until blades) {
            val bx = base[0] + pulvarRR(-0.58f, 0.58f)
            val bz = base[2] + pulvarRR(-0.58f, 0.58f)
            val by = base[1] - 0.05f
            val yaw = pulvarRR(0f, PULVAR_PI * 2f)
            val lean = pulvarRR(-0.38f, 0.38f)
            val height = pulvarRR(0.92f, 2.15f)
            val width = pulvarRR(0.075f, 0.135f)
            val moisture = pulvarClamp(1.1f - (by - pulvarWaterY(x)), 0.25f, 1f)
            val varn = pulvarRnd()
            pulvarAddBlade(opaque, floatArrayOf(bx, by, bz), yaw, lean, height, width, PulvarPal.REED_BASE, PulvarPal.REED_TIP, moisture, varn)
            reedBlades++
            if (pulvarRnd() < 0.20f) {
                val tip = floatArrayOf(
                    bx + cos(yaw + PULVAR_PI / 2f) * lean,
                    by + height - height * 0.16f * abs(lean),
                    bz + sin(yaw + PULVAR_PI / 2f) * lean,
                )
                pulvarAddSeedHead(opaque, tip, yaw, pulvarRR(0.34f, 0.56f), moisture, varn)
            }
        }
    }

    // ── Short grass tufts over the banks ─────────────────────────────────────
    var grassTufts = 0
    for (i in 0 until 420) {
        val x = pulvarRR(-110f, 110f)
        val side = if (pulvarRnd() < 0.55f) 1f else -1f
        val q = side * (if (side > 0f) pulvarRR(1.12f, 2.95f) else pulvarRR(1.12f, 1.82f))
        val base = pulvarTerrainPoint(x, q)
        val blades = 3 + floor((pulvarRnd() * 4f).toDouble()).toInt()
        grassTufts++
        for (j in 0 until blades) {
            val bx = base[0] + pulvarRR(-0.28f, 0.28f)
            val bz = base[2] + pulvarRR(-0.28f, 0.28f)
            val yaw = pulvarRR(0f, PULVAR_PI * 2f)
            val lean = pulvarRR(-0.28f, 0.28f)
            val height = pulvarRR(0.24f, 0.62f)
            val width = pulvarRR(0.045f, 0.082f)
            val tip = if (pulvarRnd() < 0.28f) PulvarPal.GRASS_DRY else PulvarPal.GRASS_LUSH
            val varn = pulvarRnd()
            pulvarAddBlade(opaque, floatArrayOf(bx, base[1] - 0.03f, bz), yaw, lean, height, width, PulvarPal.GRASS_DEEP, tip, 0.55f, varn)
        }
    }

    return PulvarRiverGeometry(
        opaque,
        water,
        PulvarRiverCounts(
            rocks.size,
            pebbleCount,
            reedClumps,
            reedBlades,
            grassTufts,
            opaque.vertexTotal,
            opaque.triangleTotal,
            water.vertexTotal,
            water.triangleTotal,
        ),
    )
}
// ============================================================================
// Shaders — byte-identical to pulvar_river_preview.html.
// ============================================================================

private const val PULVAR_VERTEX_SHADER = """
attribute vec3 aPos;
attribute vec3 aNormal;
attribute vec3 aColor;
attribute vec4 aData;
uniform mat4 uMVP;
varying vec3 vN;
varying vec3 vC;
varying vec3 vW;
varying vec4 vData;
void main() {
  vN = aNormal;
  vC = aColor;
  vW = aPos;
  vData = aData;
  gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

private const val PULVAR_FRAGMENT_SHADER = """
precision highp float;
varying vec3 vN;
varying vec3 vC;
varying vec3 vW;
varying vec4 vData;
uniform vec3 uSunDir;
uniform vec3 uSunCol;
uniform vec3 uMoonDir;
uniform vec3 uMoonCol;
uniform vec3 uSkyAmb;
uniform vec3 uGroundAmb;
uniform vec3 uCamPos;
uniform float uTime;
uniform float uWaterPass;

float prHash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float prNoise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(mix(prHash(i), prHash(i + vec2(1.0, 0.0)), u.x),
             mix(prHash(i + vec2(0.0, 1.0)), prHash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float prFbm(vec2 p) {
  float s = 0.0;
  float a = 0.5;
  for (int i = 0; i < 4; i++) {
    s += a * prNoise(p);
    p = p * 2.03 + vec2(17.3, 9.1);
    a *= 0.5;
  }
  return s;
}
vec3 prAces(vec3 x) { return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0); }
vec3 prTone(vec3 col) {
  vec3 t = prAces(col * 1.02);
  float lum = dot(t, vec3(0.2126, 0.7152, 0.0722));
  return pow(mix(t, vec3(lum), 0.10), vec3(0.4545));
}
float prWrap(float nd, float w) { return clamp((nd + w) / (1.0 + w), 0.0, 1.0); }

vec4 shadeOpaque(vec3 V) {
  float mat = vData.x;
  float moisture = vData.y;
  float submerged = vData.z;
  vec3 alb = pow(vC, vec3(2.2));
  float nMid = prFbm(vW.xz * 1.25 + vec2(3.0, 7.0));
  float nMicro = prNoise(vW.xz * 7.0 + vW.yy * 3.0);
  float nFine = prNoise(vW.xz * 23.0 - vW.yy * 5.0);
  alb *= 0.84 + 0.24 * nMid + 0.10 * nMicro;

  if (mat < 0.5) {
    // Terrain/bed: pebble speckle, grass streaks and submerged caustics.
    float pebble = smoothstep(0.68, 0.82, prNoise(vW.xz * 3.1 + 11.0));
    alb = mix(alb, vec3(0.16, 0.15, 0.13), pebble * 0.22 * (0.35 + moisture));
    float greenMask = smoothstep(0.015, 0.09, alb.g - max(alb.r, alb.b));
    float streak = prFbm(vW.xz * 0.72) * 0.55 + prNoise(vW.xz * 3.1) * 0.45;
    alb *= 1.0 + greenMask * (streak - 0.5) * 0.16;
    float grassPatchA = prFbm(vW.xz * 0.33 + 4.0);
    float grassPatchB = prNoise(vW.xz * 2.6 - 2.0);
    alb = mix(alb, vec3(0.018, 0.052, 0.010), greenMask * smoothstep(0.42, 0.72, grassPatchA) * 0.55);
    alb = mix(alb, vec3(0.105, 0.115, 0.032), greenMask * smoothstep(0.48, 0.82, grassPatchB) * 0.42);
    alb = mix(alb, alb * vec3(0.72, 0.82, 0.78), clamp(submerged * 0.45, 0.0, 0.55));
  } else if (mat < 1.5) {
    // Rock: strata, lichen-like speckle and a dark wet band.
    float strata = sin(vW.y * 4.2 + nMid * 4.0) * 0.5 + 0.5;
    alb *= 0.92 + strata * 0.13;
    float speck = smoothstep(0.74, 0.92, nFine);
    alb = mix(alb, vec3(0.28, 0.27, 0.23), speck * 0.28);
    alb *= 1.0 - moisture * 0.34;
  } else {
    // Reeds, grass and shrubs.
    alb *= 0.82 + 0.30 * nMicro + 0.12 * nFine;
    float dryMask = smoothstep(0.03, 0.14, alb.r - alb.b);
    alb = mix(alb, alb * vec3(1.05, 0.96, 0.72), dryMask * nMid * 0.35);
  }

  vec3 N = normalize(vN);
  if (!gl_FrontFacing) N = -N;
  float wrap = mat > 1.5 ? 0.38 : 0.08;
  vec3 amb = mix(uGroundAmb, uSkyAmb, N.y * 0.5 + 0.5);
  vec3 col = alb * amb;
  float nds = prWrap(dot(N, uSunDir), wrap);
  float ndm = prWrap(dot(N, uMoonDir), wrap);
  col += alb * uSunCol * nds;
  col += alb * uMoonCol * ndm;

  // Wet stone / wet earth sun sheen.
  float gloss = mat < 1.5 ? clamp(moisture * 0.75 + submerged * 0.5, 0.0, 1.0) : 0.06;
  vec3 H = normalize(uSunDir + V);
  col += uSunCol * pow(max(dot(N, H), 0.0), 72.0) * gloss * 0.16;
  vec3 Hm = normalize(uMoonDir + V);
  col += uMoonCol * pow(max(dot(N, Hm), 0.0), 72.0) * gloss * 0.20;
  if (submerged > 0.01) col *= exp(-submerged * vec3(0.22, 0.09, 0.06));

  // Animated caustic light, only where the bed is actually under water.
  if (mat < 0.5 && submerged > 0.01) {
    float c1 = prNoise(vec2(vW.x * 1.05 - uTime * 0.55, vW.z * 1.05 + uTime * 0.16));
    float c2 = prNoise(vec2(vW.x * 1.95 + uTime * 0.24, vW.z * 1.95 - uTime * 0.42));
    float caust = pow(clamp(c1 + c2 - 0.92, 0.0, 1.0), 2.0);
    float depthFade = smoothstep(0.02, 0.25, submerged) * (1.0 - smoothstep(1.1, 2.8, submerged));
    col += uSunCol * caust * depthFade * 0.018 * max(N.y, 0.0);
    col += uMoonCol * caust * depthFade * 0.05 * max(N.y, 0.0);
  }

  // Translucent back-light through reeds/grass.
  if (mat > 1.5) {
    float back = pow(max(dot(V, -uSunDir), 0.0), 3.0);
    col += alb * uSunCol * back * 0.30;
    float backM = pow(max(dot(V, -uMoonDir), 0.0), 3.0);
    col += alb * uMoonCol * backM * 0.22;
  }
  return vec4(prTone(col), 1.0);
}

vec2 waterGradient(vec2 p, float speed, float turb) {
  vec2 g = vec2(0.0);
  float adv = 0.72 + 1.65 * speed;
  float ph;
  ph = dot(p, vec2(1.0, 0.18)) * 2.05 - uTime * 2.15 * adv;
  g += vec2(1.0, 0.18) * (0.036 * 2.05 * cos(ph));
  ph = dot(p, vec2(1.0, -0.33)) * 3.65 - uTime * 3.05 * adv;
  g += vec2(1.0, -0.33) * (0.020 * 3.65 * cos(ph));
  ph = dot(p, vec2(0.86, 0.51)) * 7.4 - uTime * 4.35 * adv;
  g += vec2(0.86, 0.51) * (0.009 * 7.4 * cos(ph));
  ph = dot(p, vec2(0.94, -0.62)) * 12.8 - uTime * 5.9 * adv;
  g += vec2(0.94, -0.62) * (0.0045 * 12.8 * cos(ph));

  vec2 np = vec2(p.x * 0.55 - uTime * (0.48 + 1.05 * speed), p.y * 1.05);
  float e = 0.20;
  float n0 = prFbm(np);
  g.x += (prFbm(np + vec2(e, 0.0)) - n0) / e * 0.022 * (0.45 + turb);
  g.y += (prFbm(np + vec2(0.0, e)) - n0) / e * 0.022 * (0.45 + turb);

  vec2 mp = vec2(p.x * 3.1 - uTime * (1.7 + 3.2 * speed), p.y * 5.0);
  float m0 = prNoise(mp);
  g.x += (prNoise(mp + vec2(0.14, 0.0)) - m0) / 0.14 * 0.0035;
  g.y += (prNoise(mp + vec2(0.0, 0.14)) - m0) / 0.14 * 0.0035;
  return g;
}

vec4 shadeWater(vec3 V) {
  float u = vData.x;
  float v = vData.y;
  float depth = vData.z;
  float foamBase = vData.w;
  float speed = vC.x;
  float turb = vC.y;
  vec2 grad = waterGradient(vec2(u, v), speed, turb);
  float gradScale = 0.18 + 0.42 * turb;
  vec3 N = normalize(normalize(vN) + vec3(-grad.x, 0.0, -grad.y) * gradScale);
  float ndv = max(dot(N, V), 0.0);
  float fres = 0.02 + 0.98 * pow(1.0 - ndv, 5.0);

  // Sky/ground reflection from the scene's own hemisphere lighting.
  vec3 R = reflect(-V, N);
  float ry = clamp(R.y, -0.12, 1.0);
  vec3 skyRef = mix(uGroundAmb * 0.95, uSkyAmb * 1.22, smoothstep(-0.08, 0.58, ry));
  skyRef += uSunCol * pow(max(dot(R, uSunDir), 0.0), 16.0) * 0.16;
  skyRef += uMoonCol * pow(max(dot(R, uMoonDir), 0.0), 16.0) * 0.22;

  // Beer-Lambert body colour over the real bed rendered underneath.
  vec3 sigma = mix(vec3(0.20, 0.075, 0.050), vec3(0.38, 0.16, 0.10), turb * 0.55);
  float refrCos = sqrt(max(0.05, 1.0 - (1.0 - ndv * ndv) / 1.7689));
  float path = depth / max(refrCos, 0.55);
  vec3 trans = exp(-sigma * path);
  float deepMix = 1.0 - exp(-depth * 0.62);
  vec3 scatter = mix(vec3(0.008, 0.045, 0.046), vec3(0.0008, 0.014, 0.032), deepMix);
  vec3 waterLight = uSkyAmb * 1.18 + uGroundAmb * 0.34
    + uSunCol * max(uSunDir.y, 0.0) * 0.30 + uMoonCol * max(uMoonDir.y, 0.0) * 0.30;
  vec3 body = scatter * waterLight * 1.22;

  float flowStreak = prNoise(vec2(u * 0.10 - uTime * (0.05 + 0.10 * speed), v * 1.05)) * 0.62
    + prNoise(vec2(u * 0.38 - uTime * (0.16 + 0.30 * speed), v * 2.7)) * 0.38;
  vec3 col = (body * (1.0 - fres) + skyRef * fres) * (0.96 + 0.08 * flowStreak);

  // Sun and Moon glitter: a sharp lobe plus a broken sparkle field.
  vec3 Hs = normalize(uSunDir + V);
  float ndhs = max(dot(N, Hs), 0.0);
  float specS = pow(ndhs, mix(620.0, 105.0, turb)) * 2.4 + pow(ndhs, 54.0) * 0.08;
  vec3 Hm = normalize(uMoonDir + V);
  float ndhm = max(dot(N, Hm), 0.0);
  float specM = pow(ndhm, mix(620.0, 105.0, turb)) * 2.4 + pow(ndhm, 54.0) * 0.08;
  float spark = prNoise(vec2(u * 7.0 - uTime * (2.2 + 4.5 * speed), v * 11.0 + uTime * 0.35));
  float sparkle = smoothstep(0.92, 0.99, spark);
  col += uSunCol * (specS * fres * 1.55 + sparkle * pow(ndhs, 70.0) * 0.05);
  col += uMoonCol * (specM * fres * 1.55 + sparkle * pow(ndhm, 70.0) * 0.05);

  // Foam: baked hydraulic mask broken by downstream-advected noise.
  vec2 fp1 = vec2(u * 0.62 - uTime * (0.40 + 0.95 * speed), v * 1.45 + vC.z * 17.0);
  vec2 fp2 = vec2(u * 1.95 - uTime * (0.80 + 1.75 * speed), v * 3.65 - vC.z * 9.0);
  float foamNoise = prFbm(fp1) * 0.66 + prNoise(fp2) * 0.34;
  float crest = smoothstep(0.30, 0.86, length(grad) * (0.75 + turb));
  float foamField = foamBase * (0.72 + 0.58 * foamNoise) + crest * foamBase * 0.22;
  float foam = smoothstep(0.60, 0.82, foamField + (foamNoise - 0.5) * 0.36);
  vec3 foamAlb = vec3(0.52, 0.55, 0.54);
  vec3 foamCol = foamAlb * (uSkyAmb * 0.60 + uGroundAmb * 0.24
    + uSunCol * prWrap(dot(N, uSunDir), 0.42) * 0.58
    + uMoonCol * prWrap(dot(N, uMoonDir), 0.42) * 0.58);
  col = mix(col, foamCol, foam * 0.92);

  float transAvg = dot(trans, vec3(0.3333));
  float alpha = (1.0 - transAvg) * 0.88 + fres * 0.82;
  alpha *= 0.52 + 0.48 * smoothstep(0.0, 0.30, depth);
  alpha = max(alpha, foam * 0.96);
  alpha = clamp(alpha, 0.18, 0.97);
  return vec4(prTone(col), alpha);
}

void main() {
  vec3 V = normalize(uCamPos - vW);
  if (uWaterPass > 0.5) {
    // Premultiplied alpha for Android's compositor (the surface is a TextureView, which
    // is blended into the window as premultiplied). The shading is unchanged: the colour
    // is simply multiplied by its own coverage at the last moment.
    vec4 w = shadeWater(V);
    gl_FragColor = vec4(w.rgb * w.a, w.a);
  } else {
    gl_FragColor = shadeOpaque(V);
  }
}
"""

// ============================================================================
// Lighting helpers — same stops as the HTML preview.
// ============================================================================

/** Direction TO a body from astronomical azimuth/altitude in world axes
 *  (+X east, +Y up, +Z south). */
private fun pulvarDirFromAzAlt(azimuthDeg: Float, altitudeDeg: Float): FloatArray {
    val az = Math.toRadians(azimuthDeg.toDouble())
    val al = Math.toRadians(altitudeDeg.toDouble())
    return floatArrayOf(
        (sin(az) * cos(al)).toFloat(),
        sin(al).toFloat(),
        (-cos(az) * cos(al)).toFloat(),
    )
}

private fun pulvarLerp3(a: FloatArray, b: FloatArray, f: Float): FloatArray =
    floatArrayOf(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f)

private fun pulvarPin12(x: Float): Float = x.coerceIn(0f, 1.2f)

private fun pulvarSunColorFor(altitudeDeg: Float): FloatArray {
    val d = max(0f, sin(Math.toRadians(max(altitudeDeg, 0f).toDouble())).toFloat())
    val c: FloatArray = when {
        altitudeDeg < 0f -> floatArrayOf(0f, 0f, 0f)
        altitudeDeg < 8f -> pulvarLerp3(floatArrayOf(1f, 0.36f, 0.10f), floatArrayOf(1f, 0.62f, 0.28f), altitudeDeg / 8f)
        altitudeDeg < 25f -> pulvarLerp3(floatArrayOf(1f, 0.62f, 0.28f), floatArrayOf(1f, 0.88f, 0.66f), (altitudeDeg - 8f) / 17f)
        else -> pulvarLerp3(floatArrayOf(1f, 0.88f, 0.66f), floatArrayOf(1f, 0.96f, 0.88f), min(1f, (altitudeDeg - 25f) / 35f))
    }
    val s = 2.9f * (0.12f + 0.88f * d.pow(0.65f)) * if (altitudeDeg < 0f) 0f else 1f
    return floatArrayOf(c[0] * s, c[1] * s, c[2] * s)
}

private fun pulvarMoonColorFor(altitudeDeg: Float, phase01: Float): FloatArray {
    if (altitudeDeg < 0f) return floatArrayOf(0f, 0f, 0f)
    val i = 0.34f * phase01 * (0.25f + 0.75f * sin(Math.toRadians(max(altitudeDeg, 0f).toDouble())).toFloat())
    return floatArrayOf(0.62f * i, 0.72f * i, 0.95f * i)
}

// ============================================================================
// Scene: GL resources + light/flow state + static chunked VBOs.
// init()/draw()/release() MUST run on the GL thread; setters are synchronised.
// ============================================================================

class PulvarRiverScene {

    /** Nominal world width (m) framed in a landscape viewport. 104 m
     *  reproduces the HTML preview; narrower aspects crop the ends instead
     *  of shrinking the river to a line. */
    @Volatile var visibleWidthMeters: Float = 104f

    /** Where the near-foreground reference point sits as a fraction of the
     *  viewport height from the top. 1.01 (default) reproduces the preview
     *  and lets the near grass leave the bottom edge. */
    @Volatile var baseFractionFromTop: Float = 1.01f

    /** Flow phase speed multiplier. 1 = the preview's normal current;
     *  0 freezes the water phase. */
    @Volatile var flowSpeed: Float = 1f

    /** When false the flow phase does not advance; pair with
     *  [PulvarRiverTextureView.setAnimated] to a single frame. */
    @Volatile var animated: Boolean = true

    /** Set the absolute flow phase in seconds (deterministic screenshots). */
    fun setFlowPhase(seconds: Float) { flowTimeSeconds = seconds }

    private val lock = Any()
    private var sunAzimuthDeg = 205f
    private var sunAltitudeDeg = 52f
    private var moonAzimuthDeg = 128f
    private var moonAltitudeDeg = 34f
    private var moonIllumination01 = 0.72f
    private var sunDir = pulvarDirFromAzAlt(205f, 52f)
    private var sunColor = floatArrayOf(0f, 0f, 0f)
    private var moonDir = pulvarDirFromAzAlt(128f, 34f)
    private var moonColor = floatArrayOf(0f, 0f, 0f)
    private var skyAmb = floatArrayOf(0.018f, 0.030f, 0.065f)
    private var groundAmb = floatArrayOf(0.010f, 0.014f, 0.020f)
    private var sunExplicit = false
    private var ambientExplicit = false

    @Volatile private var flowTimeSeconds: Float = 2.4f
    private var lastFrameNanos: Long = 0L

    init {
        setSun(205f, 52f)
        setMoon(128f, 34f, 0.72f)
    }

    private fun recomputeLightingLocked() {
        sunDir = pulvarDirFromAzAlt(sunAzimuthDeg, sunAltitudeDeg)
        if (!sunExplicit) sunColor = pulvarSunColorFor(sunAltitudeDeg)
        moonDir = pulvarDirFromAzAlt(moonAzimuthDeg, moonAltitudeDeg)
        moonColor = pulvarMoonColorFor(moonAltitudeDeg, moonIllumination01)
        if (!ambientExplicit) {
            val day = pulvarPin12((sunAltitudeDeg + 2f) / 14f)
            val duskArg = (sunAltitudeDeg + 1f) / 5f
            val dusk = exp((-(duskArg * duskArg)).toDouble()).toFloat()
            var sky = pulvarLerp3(floatArrayOf(0.018f, 0.030f, 0.065f), floatArrayOf(0.30f, 0.42f, 0.58f), day)
            sky = pulvarLerp3(sky, floatArrayOf(0.30f, 0.20f, 0.16f), dusk * 0.45f)
            var gnd = pulvarLerp3(floatArrayOf(0.010f, 0.014f, 0.020f), floatArrayOf(0.16f, 0.17f, 0.13f), day)
            gnd = pulvarLerp3(gnd, floatArrayOf(0.14f, 0.10f, 0.07f), dusk * 0.35f)
            val ml = if (moonAltitudeDeg > 0f) 0.035f * moonIllumination01 else 0f
            skyAmb = floatArrayOf(sky[0] + ml * 0.5f, sky[1] + ml * 0.65f, sky[2] + ml)
            groundAmb = gnd
        }
    }

    /** Move the Sun. Astronomical azimuth (0°=N, 90°E, 180°S, 270°W);
     *  colour/intensity/ambient are derived from altitude with the preview's stops. */
    fun setSun(azimuthDeg: Float, altitudeDeg: Float) {
        synchronized(lock) {
            sunAzimuthDeg = azimuthDeg; sunAltitudeDeg = altitudeDeg
            sunExplicit = false; ambientExplicit = false
            recomputeLightingLocked()
        }
    }

    /** Explicit Sun light (colour already multiplied by intensity). */
    fun setSunLight(azimuthDeg: Float, altitudeDeg: Float, r: Float, g: Float, b: Float) {
        synchronized(lock) {
            sunAzimuthDeg = azimuthDeg; sunAltitudeDeg = altitudeDeg
            sunExplicit = true
            sunColor = floatArrayOf(r, g, b)
            recomputeLightingLocked()
        }
    }

    /** Move the Moon. [illumination01] is the illuminated fraction, 0..1. */
    fun setMoon(azimuthDeg: Float, altitudeDeg: Float, illumination01: Float = 0.72f) {
        synchronized(lock) {
            moonAzimuthDeg = azimuthDeg; moonAltitudeDeg = altitudeDeg
            moonIllumination01 = illumination01.coerceIn(0f, 1f)
            recomputeLightingLocked()
        }
    }

    /** Override hemisphere ambient. Note [setSun] recomputes it. */
    fun setAmbient(skyR: Float, skyG: Float, skyB: Float, groundR: Float, groundG: Float, groundB: Float) {
        synchronized(lock) {
            ambientExplicit = true
            skyAmb = floatArrayOf(skyR, skyG, skyB)
            groundAmb = floatArrayOf(groundR, groundG, groundB)
        }
    }

    // ---- GL resources (GL thread only) ----
    private class Chunk(val buffers: IntArray, val indexCount: Int) // pos, normal, colour, data, index
    private class Prog(val id: Int) {
        var uMVP = -1; var uSunDir = -1; var uSunCol = -1
        var uMoonDir = -1; var uMoonCol = -1
        var uSkyAmb = -1; var uGroundAmb = -1; var uCamPos = -1
        var uTime = -1; var uWaterPass = -1
    }
    private var prog: Prog? = null
    private var opaqueChunks: ArrayList<Chunk>? = null
    private var waterChunks: ArrayList<Chunk>? = null

    /** Geometry counts after the first build (null until init/draw). */
    @Volatile var geometryCounts: PulvarRiverCounts? = null
        private set

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val mvp = FloatArray(16)
    private var cameraEye = floatArrayOf(0f, 40f, 120f)
    private var cachedWidth = -1
    private var cachedHeight = -1
    private var cachedVisibleWidth = Float.NaN
    private var cachedBaseFraction = Float.NaN

    private fun compile(type: Int, src: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, src)
        GLES30.glCompileShader(s)
        val ok = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(s)
            GLES30.glDeleteShader(s)
            throw RuntimeException("PulvarRiver shader compile failed: $log")
        }
        return s
    }

    private fun floatBuffer(data: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }

    private fun makeProgram(): Prog {
        val vs = compile(GLES30.GL_VERTEX_SHADER, PULVAR_VERTEX_SHADER)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, PULVAR_FRAGMENT_SHADER)
        val id = GLES30.glCreateProgram()
        GLES30.glAttachShader(id, vs)
        GLES30.glAttachShader(id, fs)
        GLES30.glBindAttribLocation(id, 0, "aPos")
        GLES30.glBindAttribLocation(id, 1, "aNormal")
        GLES30.glBindAttribLocation(id, 2, "aColor")
        GLES30.glBindAttribLocation(id, 3, "aData")
        GLES30.glLinkProgram(id)
        GLES30.glDeleteShader(vs); GLES30.glDeleteShader(fs)
        val ok = IntArray(1)
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("PulvarRiver program link failed: ${GLES30.glGetProgramInfoLog(id)}")
        val p = Prog(id)
        p.uMVP = GLES30.glGetUniformLocation(id, "uMVP")
        p.uSunDir = GLES30.glGetUniformLocation(id, "uSunDir")
        p.uSunCol = GLES30.glGetUniformLocation(id, "uSunCol")
        p.uMoonDir = GLES30.glGetUniformLocation(id, "uMoonDir")
        p.uMoonCol = GLES30.glGetUniformLocation(id, "uMoonCol")
        p.uSkyAmb = GLES30.glGetUniformLocation(id, "uSkyAmb")
        p.uGroundAmb = GLES30.glGetUniformLocation(id, "uGroundAmb")
        p.uCamPos = GLES30.glGetUniformLocation(id, "uCamPos")
        p.uTime = GLES30.glGetUniformLocation(id, "uTime")
        p.uWaterPass = GLES30.glGetUniformLocation(id, "uWaterPass")
        return p
    }

    private fun upload(mesh: PulvarMeshBuilder, out: ArrayList<Chunk>) {
        val positions = mesh.pos.toFloatArray()
        val normals = mesh.nor.toFloatArray()
        val colours = mesh.col.toFloatArray()
        val data = mesh.dat.toFloatArray()
        val indices = mesh.idx.toIntArray()

        val map = HashMap<Int, Int>()
        val cPos = PulvarFloatBag(); val cNor = PulvarFloatBag(); val cCol = PulvarFloatBag(); val cDat = PulvarFloatBag(); val cIdx = PulvarIntBag()
        fun flushChunk() {
            if (cIdx.size == 0) return
            val b = IntArray(5)
            GLES30.glGenBuffers(5, b, 0)
            val p = cPos.toFloatArray(); val n = cNor.toFloatArray(); val c = cCol.toFloatArray(); val d = cDat.toFloatArray()
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, b[0])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, p.size * 4, floatBuffer(p), GLES30.GL_STATIC_DRAW)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, b[1])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, n.size * 4, floatBuffer(n), GLES30.GL_STATIC_DRAW)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, b[2])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, c.size * 4, floatBuffer(c), GLES30.GL_STATIC_DRAW)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, b[3])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, d.size * 4, floatBuffer(d), GLES30.GL_STATIC_DRAW)
            val ib = cIdx.toIntArray()
            val sb: ShortBuffer = ByteBuffer.allocateDirect(ib.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            for (i in ib) sb.put(i.toShort())
            sb.position(0)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, b[4])
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, ib.size * 2, sb, GLES30.GL_STATIC_DRAW)
            out.add(Chunk(b, ib.size))
            map.clear(); cPos.clear(); cNor.clear(); cCol.clear(); cDat.clear(); cIdx.clear()
        }
        var ti = 0
        while (ti < indices.size) {
            var fresh = 0
            for (k in 0..2) if (!map.containsKey(indices[ti + k])) fresh++
            if (cIdx.size > 0 && map.size + fresh > 60000) flushChunk()
            for (k in 0..2) {
                val vi = indices[ti + k]
                var ni = map[vi]
                if (ni == null) {
                    ni = map.size; map[vi] = ni
                    for (c in 0..2) {
                        cPos.add(positions[vi * 3 + c]); cNor.add(normals[vi * 3 + c]); cCol.add(colours[vi * 3 + c])
                    }
                    for (c in 0..3) cDat.add(data[vi * 4 + c])
                }
                cIdx.add(ni)
            }
            ti += 3
        }
        flushChunk()
    }

    private fun ensureUploaded() {
        if (opaqueChunks != null && waterChunks != null) return
        val geometry = buildPulvarRiverScene()
        geometryCounts = geometry.counts
        val oc = ArrayList<Chunk>(); upload(geometry.opaque, oc); opaqueChunks = oc
        val wc = ArrayList<Chunk>(); upload(geometry.water, wc); waterChunks = wc
    }

    /** Build/upload the river for this GL context. Call in onSurfaceCreated. */
    fun init() {
        prog = makeProgram()
        ensureUploaded()
        lastFrameNanos = 0L
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun projectPoint(m: FloatArray, p: FloatArray, out: FloatArray) {
        Matrix.multiplyMV(out, 0, m, 0, floatArrayOf(p[0], p[1], p[2], 1f), 0)
        val w = out[3]
        if (w != 0f) { out[0] /= w; out[1] /= w; out[2] /= w }
    }

    // Fixed panoramic side camera: same target, elevation, fit points and
    // binary distance solve as the HTML preview's computeCamera().
    private fun computeCamera(width: Int, height: Int) {
        if (width == cachedWidth && height == cachedHeight &&
            visibleWidthMeters == cachedVisibleWidth && baseFractionFromTop == cachedBaseFraction) return
        val aspect = width.toFloat() / height.toFloat()
        val target = floatArrayOf(0f, 0.7f, -3f)
        val dirLen = sqrt(0.36f * 0.36f + 1f)
        val dir = floatArrayOf(0f, 0.36f / dirLen, 1f / dirLen)
        val xFit = visibleWidthMeters * 0.5f * when {
            aspect >= 1.45f -> 1f
            aspect >= 1.0f -> 48f / 52f
            else -> 38f / 52f
        }
        val pts = arrayOf(
            floatArrayOf(-xFit, 1f, -40f), floatArrayOf(xFit, 1f, -40f),
            floatArrayOf(-xFit, 1f, 18f), floatArrayOf(xFit, 1f, 18f),
            floatArrayOf(0f, 4f, -20f),
            floatArrayOf(-xFit, 0f, 3f), floatArrayOf(xFit, 0f, 3f),
        )
        val tmpProj = FloatArray(16); val tmpView = FloatArray(16); val tmpMvp = FloatArray(16); val q = FloatArray(4)
        fun fits(dist: Float): Boolean {
            val eye = floatArrayOf(target[0] + dir[0] * dist, target[1] + dir[1] * dist, target[2] + dir[2] * dist)
            Matrix.perspectiveM(tmpProj, 0, 35.521f, aspect, 1f, 900f)
            Matrix.setLookAtM(tmpView, 0, eye[0], eye[1], eye[2], target[0], target[1], target[2], 0f, 1f, 0f)
            Matrix.multiplyMM(tmpMvp, 0, tmpProj, 0, tmpView, 0)
            for (p in pts) {
                projectPoint(tmpMvp, p, q)
                if (q[3] <= 0f || abs(q[0]) > 0.985f || q[1] < -0.94f || q[1] > 0.94f) return false
            }
            return true
        }
        var lo = 35f; var hi = 560f
        for (i in 0 until 28) {
            val mid = (lo + hi) * 0.5f
            if (fits(mid)) hi = mid else lo = mid
        }
        val dist = hi
        cameraEye = floatArrayOf(target[0] + dir[0] * dist, target[1] + dir[1] * dist, target[2] + dir[2] * dist)
        Matrix.perspectiveM(proj, 0, 35.521f, aspect, 1f, 900f)
        Matrix.setLookAtM(view, 0, cameraEye[0], cameraEye[1], cameraEye[2], target[0], target[1], target[2], 0f, 1f, 0f)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        projectPoint(mvp, floatArrayOf(11f, 1.8f, 24.8f), q)
        val desiredY = 1f - 2f * baseFractionFromTop
        proj[9] -= desiredY - q[1]
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        cachedWidth = width; cachedHeight = height
        cachedVisibleWidth = visibleWidthMeters; cachedBaseFraction = baseFractionFromTop
    }

    private fun drawChunks(p: Prog, chunks: ArrayList<Chunk>, waterPass: Float) {
        GLES30.glUniform1f(p.uWaterPass, waterPass)
        for (chunk in chunks) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, chunk.buffers[0])
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, chunk.buffers[1])
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, chunk.buffers[2])
            GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, 0, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, chunk.buffers[3])
            GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, 0, 0)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, chunk.buffers[4])
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, chunk.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
        }
    }

    /**
     * Draw one frame into the current viewport. Does not clear colour unless
     * [clear] is true, so the app sky/landscape behind shows through outside
     * the corridor. Opaque geometry is drawn with depth writes; the water is
     * then alpha-blended with depth test on and depth writes off.
     */
    fun draw(width: Int, height: Int, clear: Boolean = false) {
        val p = prog ?: return
        if (width <= 0 || height <= 0) return
        ensureUploaded()
        val oc = opaqueChunks ?: return
        val wc = waterChunks ?: return

        val now = System.nanoTime()
        if (lastFrameNanos == 0L) lastFrameNanos = now
        val dt = ((now - lastFrameNanos).toDouble() / 1.0e9).coerceIn(0.0, 0.1).toFloat()
        lastFrameNanos = now
        if (animated) flowTimeSeconds += dt * flowSpeed

        GLES30.glViewport(0, 0, width, height)
        if (clear) {
            GLES30.glClearColor(0f, 0f, 0f, 0f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        }
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glFrontFace(GLES30.GL_CCW)

        computeCamera(width, height)
        GLES30.glUseProgram(p.id)
        GLES30.glUniformMatrix4fv(p.uMVP, 1, false, mvp, 0)
        synchronized(lock) {
            GLES30.glUniform3fv(p.uSunDir, 1, sunDir, 0)
            GLES30.glUniform3fv(p.uSunCol, 1, sunColor, 0)
            GLES30.glUniform3fv(p.uMoonDir, 1, moonDir, 0)
            GLES30.glUniform3fv(p.uMoonCol, 1, moonColor, 0)
            GLES30.glUniform3fv(p.uSkyAmb, 1, skyAmb, 0)
            GLES30.glUniform3fv(p.uGroundAmb, 1, groundAmb, 0)
        }
        GLES30.glUniform3fv(p.uCamPos, 1, cameraEye, 0)
        GLES30.glUniform1f(p.uTime, flowTimeSeconds)
        for (i in 0..3) GLES30.glEnableVertexAttribArray(i)

        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glDepthMask(true)
        drawChunks(p, oc, 0f)

        GLES30.glEnable(GLES30.GL_BLEND)
        // ONE, not SRC_ALPHA: the water fragment is already premultiplied (see the shader's
        // main), which is what the window compositor expects of a TextureView.
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        drawChunks(p, wc, 1f)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)

        for (i in 0..3) GLES30.glDisableVertexAttribArray(i)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    /** Delete GL resources. Call on the GL thread when the context is lost/finished. */
    fun release() {
        opaqueChunks?.let { chunks -> for (chunk in chunks) if (chunk.buffers[0] != 0) GLES30.glDeleteBuffers(5, chunk.buffers, 0) }
        waterChunks?.let { chunks -> for (chunk in chunks) if (chunk.buffers[0] != 0) GLES30.glDeleteBuffers(5, chunk.buffers, 0) }
        opaqueChunks = null; waterChunks = null
        prog?.let { GLES30.glDeleteProgram(it.id) }; prog = null
        cachedWidth = -1; cachedHeight = -1
    }
}

// ============================================================================
// Transparent GL host.
//
// The supplied file hosted this scene in a GLSurfaceView flagged
// setZOrderOnTop(true). That is the wrong surface for this app's Home canvas,
// for the two reasons PersepolisTextureView already documents at length: a
// window-level surface composites above the *entire activity window*, so no
// Compose sibling (the scene readout, the sky search scrim, the card row) can
// cover it; and its rectangle is not subject to the sky canvas's clip, scroll
// or dimming. So the frames are rasterised into a TextureView instead —
// ordinary window content, exactly like the painted layers around it — while
// the scene, its geometry, its GLSL and its camera stay untouched. Only the
// surface type and the EGL lifecycle live here.
//
// Unlike the relief (one frame per ephemeris change) the water moves, so the
// loop is continuous while the composition is resumed, paced at
// [FRAME_INTERVAL_MS] to match the sky scene's motion cadence, and drops to one
// frame per request when the host sets [setAnimated] to false.
//
// The view is non-clickable and non-focusable and consumes no touches, so
// tap-to-identify and finger time travel on the surrounding canvas are
// unaffected. Lifecycle is forwarded by the Compose host below.
// ============================================================================

class PulvarRiverTextureView(context: Context) :
    TextureView(context), TextureView.SurfaceTextureListener {

    /** The scene, exposed for framing/flow controls and explicit-light APIs. Its
     *  init/draw/release run on the render thread owned below; its setters are synchronized
     *  and may be called from any thread. */
    val scene = PulvarRiverScene()

    private var renderer: Renderer? = null

    /** Main thread. Set by [release]; once true, no callback may restart rendering. */
    private var released = false

    /** Main thread. The desired render state, mirrored into every renderer at construction: the
     *  surface can arrive after the host has already resumed (or paused) this view, and a renderer
     *  built from defaults would then sit idle with the water frozen. */
    private var paused = false
    private var animated = true

    init {
        // Transparent: the painted landscape shows through wherever the corridor has no
        // geometry, and the sky/ground composite correctly behind it.
        isOpaque = false
        isClickable = false
        isFocusable = false
        surfaceTextureListener = this
    }

    /** Astronomical azimuth/altitude; colour, intensity and ambient are derived by the scene. */
    fun setSun(azimuthDeg: Float, altitudeDeg: Float) {
        scene.setSun(azimuthDeg, altitudeDeg)
        renderer?.requestDraw()
    }

    /** The Moon as the second light; [illumination01] is the illuminated fraction, 0..1. */
    fun setMoon(azimuthDeg: Float, altitudeDeg: Float, illumination01: Float = 0.72f) {
        scene.setMoon(azimuthDeg, altitudeDeg, illumination01)
        renderer?.requestDraw()
    }

    /** Flow phase speed multiplier: 1 = the preview's normal current, 0 freezes the water. */
    fun setFlowSpeed(speed: Float) {
        scene.flowSpeed = speed
        renderer?.requestDraw()
    }

    /** False = one frame per request (frozen water, no render loop). */
    fun setAnimated(animated: Boolean) {
        this.animated = animated
        scene.animated = animated
        renderer?.setContinuous(animated)
    }

    /** Framing, straight through to the scene's fixed camera (see [PulvarRiverFraming]). */
    fun setFraming(visibleWidthMeters: Float, baseFractionFromTop: Float) {
        scene.visibleWidthMeters = visibleWidthMeters
        scene.baseFractionFromTop = baseFractionFromTop
        renderer?.requestDraw()
    }

    /** Lifecycle, forwarded by the Compose host: resume the render loop (and repaint once).
     *  The EGL context and the uploaded mesh are kept across pauses. */
    fun onResume() {
        paused = false
        renderer?.setPaused(false)
    }

    /** Lifecycle, forwarded by the Compose host: stop the render loop until [onResume]. The
     *  GL thread and context stay alive; the scene's light/flow state keeps up meanwhile, so
     *  the first frame after resume is already correct. */
    fun onPause() {
        paused = true
        renderer?.setPaused(true)
    }

    /** Main thread. Releases the scene's GL objects on the render thread, tears the EGL
     *  context down and stops the thread. Terminal for this view instance. Safe to repeat. */
    fun release() {
        released = true
        stopRenderer()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (released) return
        val owner = Renderer(scene, pausedInitially = paused, continuousInitially = animated)
        renderer = owner
        owner.attach(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer?.setViewport(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // Returning false: the renderer releases the SurfaceTexture on its GL thread, after
        // the EGL surface that uses it has been destroyed.
        stopRenderer()
        return false
    }

    private fun stopRenderer() {
        renderer?.detachAndStop()
        renderer = null
    }

    /**
     * Owns the GLES 3.0 context and the EGL window surface for one attachment of the view.
     * Every GL and EGL call runs on its dedicated handler thread. While [continuous] the
     * thread draws one frame every [FRAME_INTERVAL_MS]; when it is false (or the view is
     * paused) it draws only on [requestDraw].
     */
    private class Renderer(
        private val scene: PulvarRiverScene,
        pausedInitially: Boolean,
        continuousInitially: Boolean,
    ) {

        private val thread = HandlerThread("PulvarRiverGl").apply { start() }
        private val handler = Handler(thread.looper)
        private val drawPending = AtomicBoolean(false)

        // ---- GL-thread-only state -------------------------------------------------------
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var surfaceTexture: SurfaceTexture? = null
        private var viewportWidth = 0
        private var viewportHeight = 0
        private var paused = pausedInitially
        private var continuous = continuousInitially
        private var looping = false
        private var failed = false

        /** One turn of the render loop: draw, then reschedule only while it should run. */
        private val loop = object : Runnable {
            override fun run() {
                looping = false
                if (failed) return
                drawFrame()
                if (continuous && !paused && eglSurface != EGL14.EGL_NO_SURFACE) {
                    looping = true
                    handler.postDelayed(this, FRAME_INTERVAL_MS)
                }
            }
        }

        /** Creates EGL + the scene's GL resources for [surface], then draws the first frame.
         *  Any state already pushed into the scene (light/framing set before the surface
         *  existed) is picked up by that frame. */
        fun attach(surface: SurfaceTexture, width: Int, height: Int) = handler.post {
            surfaceTexture = surface
            viewportWidth = width
            viewportHeight = height
            try {
                initEgl(surface)
                scene.init()
                drawFrame()
                // The first frame above is the composed one; the loop takes over here when the
                // host is already resumed (it will not call setPaused(false) again).
                kick()
            } catch (t: Throwable) {
                Log.w(TAG, "Pulvar river GL init failed", t)
                failed = true
                releaseSceneQuietly()
                releaseGl()
            }
        }

        fun setViewport(width: Int, height: Int) = handler.post {
            viewportWidth = width
            viewportHeight = height
            kick()
        }

        fun setPaused(value: Boolean) = handler.post {
            paused = value
            if (!paused) kick()
        }

        fun setContinuous(value: Boolean) = handler.post {
            continuous = value
            kick()
        }

        /** Posts a draw (and restarts the loop if the host asks for one shot at a time). */
        fun requestDraw() {
            if (drawPending.compareAndSet(false, true)) {
                handler.post {
                    drawPending.set(false)
                    kick()
                }
            }
        }

        /** Releases the scene's GL objects while the context is current, then tears down
         *  EGL and stops the thread. The renderer cannot be reused afterwards. */
        fun detachAndStop() {
            handler.post {
                releaseSceneQuietly()
                releaseGl()
                thread.quitSafely()
            }
        }

        private fun kick() {
            if (looping || paused || failed) return
            if (eglSurface == EGL14.EGL_NO_SURFACE) return
            if (viewportWidth <= 0 || viewportHeight <= 0) return
            looping = true
            handler.post(loop)
        }

        private fun drawFrame() {
            if (failed || paused || eglSurface == EGL14.EGL_NO_SURFACE) return
            if (viewportWidth <= 0 || viewportHeight <= 0) return
            try {
                // The scene draws with a transparent clear (draw(clear = true)); it never
                // swaps, so this host swaps explicitly after each frame.
                scene.draw(viewportWidth, viewportHeight, clear = true)
                if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                    Log.w(TAG, "eglSwapBuffers failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
                }
            } catch (t: Throwable) {
                // A GL failure must never take Home down: drop the layer and keep the painted
                // landscape underneath — which still contains the 2D Pulvar.
                Log.w(TAG, "Pulvar river draw failed", t)
                failed = true
                releaseSceneQuietly()
                releaseGl()
            }
        }

        private fun initEgl(surface: SurfaceTexture) {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
            eglDisplay = display

            // The supplied view's config chooser, spelled out: RGBA8888 for alpha compositing
            // plus a 16-bit depth buffer (the banks occlude the water behind them, so depth is
            // not optional), at the app's GL level — the manifest declares ES 3.0 and the
            // scene's GL calls use the 3.0 entry points.
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            check(
                EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) &&
                    numConfigs[0] > 0 && configs[0] != null
            ) { "No ES 3.0 EGL config available" }
            val eglConfig = configs[0]!!

            val contextAttribs = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(display, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed (0x${Integer.toHexString(EGL14.eglGetError())})" }
            eglContext = context

            val windowSurface = EGL14.eglCreateWindowSurface(display, eglConfig, surface, intArrayOf(EGL14.EGL_NONE), 0)
            check(windowSurface != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }
            eglSurface = windowSurface

            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }
        }

        /** GL thread. Deletes the scene's programs/buffers; only while a context exists. */
        private fun releaseSceneQuietly() {
            if (eglContext == EGL14.EGL_NO_CONTEXT) return
            try {
                scene.release()
            } catch (t: Throwable) {
                Log.w(TAG, "Pulvar river GL cleanup failed", t)
            }
        }

        /** GL thread. Destroys the EGL surface/context and releases the SurfaceTexture.
         *  Deliberately no eglTerminate: the display connection is process-wide and the
         *  app's other GL hosts (relief, panorama, AR sky) may be alive on it. */
        private fun releaseGl() {
            val display = eglDisplay
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            }
            surfaceTexture?.release()
            surfaceTexture = null
            eglSurface = EGL14.EGL_NO_SURFACE
            eglContext = EGL14.EGL_NO_CONTEXT
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }

        private companion object {
            /** EGL_OPENGL_ES3_BIT (0x0040), defined here because EGL14 does not expose it. */
            const val EGL_OPENGL_ES3_BIT = 0x0040

            /** EGL_CONTEXT_CLIENT_VERSION (0x3098). */
            const val EGL_CONTEXT_CLIENT_VERSION = 0x3098

            /** ~24 fps: the cadence the sky scene's own motion layer runs at. */
            const val FRAME_INTERVAL_MS = 42L

            const val TAG = "PulvarRiverGl"
        }
    }
}

// ============================================================================
// Framing for the Home scene's foreground band.
//
// The camera is not free: the scene solves its distance so seven corridor
// reference points fit the viewport (computeCamera), and [PulvarRiverScene.baseFractionFromTop]
// then pins the near-foreground reference point (11, 1.8, 24.8) to a given
// fraction of the view height. What the caller must choose is where the *far
// bank* lands, because on Home the corridor is painted into a wide, short band
// at the bottom of the sky canvas: the band must be filled edge to edge, with
// no transparent strip at its top that would let the painted 2D river show
// through.
//
// The numbers below were solved against that exact camera: for a viewport
// aspect a, projecting the corridor's highest bank vertex back to the top edge
// of the viewport gives
//
//     baseFractionFromTop ≈ 0.048 + 0.141 · a          (2.0 ≤ a ≤ 9.0)
//
// solved at a = 2.86, 3.85, 4.44, 4.76, 5.00, 5.63 and 6.67, each to within
// ±0.01 of the band height. The result puts the far-bank crest on the band's
// top edge and the water surface ≈12–14 % of the band below it, with the near
// floodplain running off the bottom — the preview's own composition, cropped to
// the band. The 104 m default visible width reproduces the preview's lateral
// scale and keeps the reach's ends (and so the mesh's transparent ends) outside
// the frame; smaller widths pull the camera in until near-side geometry crosses
// the near plane, so it is not a free parameter.
// ============================================================================

object PulvarRiverFraming {

    /** Nominal world width framed by the corridor: the preview's 104 m. */
    const val VISIBLE_WIDTH_M: Float = 104f

    /** The preview's own vertical framing, for a full-canvas viewport. */
    const val PREVIEW_BASE_FRACTION_FROM_TOP: Float = 1.01f

    /**
     * [PulvarRiverScene.baseFractionFromTop] that puts the corridor's far-bank crest on the
     * viewport's top edge for a viewport of [viewportAspect] (width / height, both in pixels).
     */
    fun baseFractionFromTop(viewportAspect: Float): Float {
        val a = viewportAspect.coerceIn(2f, 9f)
        return (0.048f + 0.141f * a).coerceIn(0.24f, 1.32f)
    }
}

// ============================================================================
// Jetpack Compose entry point. AndroidView hosts the fixed-camera GL view;
// recomposition forwards light/flow/framing state without rebuilding the
// cached mesh. Light and framing changes are keyed on their values, so the
// 1 Hz sky clock and the cloud motion never touch the GL view. The GL view is
// paused when this composable leaves composition, and its render thread is
// released with the view.
// ============================================================================

@Composable
fun PulvarRiver3D(
    modifier: Modifier = Modifier,
    sunAzimuthDeg: Float = 205f,
    sunAltitudeDeg: Float = 52f,
    moonAzimuthDeg: Float = 128f,
    moonAltitudeDeg: Float = 34f,
    moonIllumination: Float = 0.72f,
    flowSpeed: Float = 1f,
    animated: Boolean = true,
    visibleWidthMeters: Float = PulvarRiverFraming.VISIBLE_WIDTH_M,
    baseFractionFromTop: Float = PulvarRiverFraming.PREVIEW_BASE_FRACTION_FROM_TOP,
) {
    var view by remember { mutableStateOf<PulvarRiverTextureView?>(null) }

    LaunchedEffect(view, sunAzimuthDeg, sunAltitudeDeg) {
        view?.setSun(sunAzimuthDeg, sunAltitudeDeg)
    }
    LaunchedEffect(view, moonAzimuthDeg, moonAltitudeDeg, moonIllumination) {
        view?.setMoon(moonAzimuthDeg, moonAltitudeDeg, moonIllumination)
    }
    LaunchedEffect(view, flowSpeed, animated) {
        view?.apply {
            setFlowSpeed(flowSpeed)
            setAnimated(animated)
        }
    }
    LaunchedEffect(view, visibleWidthMeters, baseFractionFromTop) {
        view?.setFraming(visibleWidthMeters, baseFractionFromTop)
    }

    // Render-thread lifecycle: gate drawing with the host's lifecycle, resume with it. Adding
    // the observer syncs to the current state, so a view created while already RESUMED is
    // resumed too.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, view) {
        val currentView = view
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> currentView?.onResume()
                Lifecycle.Event.ON_PAUSE -> currentView?.onPause()
                else -> Unit
            }
        }
        if (currentView != null) lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { context ->
            PulvarRiverTextureView(context).apply {
                // Light, flow and framing are set before the surface exists, so the first
                // drawn frame is already the composed and lit one; the effects above keep
                // them current afterwards.
                setSun(sunAzimuthDeg, sunAltitudeDeg)
                setMoon(moonAzimuthDeg, moonAltitudeDeg, moonIllumination)
                setFlowSpeed(flowSpeed)
                setAnimated(animated)
                setFraming(visibleWidthMeters, baseFractionFromTop)
                view = this
            }
        },
        // All state flows through the keyed effects above; nothing to push per recomposition.
        update = {},
        onRelease = { released ->
            // Terminal for this view instance: frees the scene's GL objects on its render
            // thread and stops that thread. If the surface is already gone, the texture
            // callback covers the renderer's teardown instead.
            released.release()
            if (view === released) view = null
        },
        modifier = modifier
    )
}
