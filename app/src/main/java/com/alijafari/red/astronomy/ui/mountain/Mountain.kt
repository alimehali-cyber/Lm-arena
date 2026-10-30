// Change this package to your app's package.
package com.alijafari.red.astronomy.ui.mountain

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
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
 * ============================================================================
 * Big mountain — one large two-crest massif in a fixed front view, with
 * green / autumn / snow seasons, for Android / Jetpack Compose.
 * ============================================================================
 * Single self-contained Kotlin file. Geometry is generated procedurally into
 * static OpenGL ES 2.0 VBOs; there are no model/texture assets and no
 * WebView/Filament/SceneView dependency. [MountainScene] is hosted by an in-window TextureView on Home.
 *
 * MODEL
 * -----
 * The silhouette is taken from the supplied green reference photograph: a
 * sky-mask scan of its ridge line is embedded as the back-crest profile, so
 * the level left crest, pointed central back peak, deep saddle notch and
 * high right craggy massif land where they do in the photo. A second, lower
 * front escarpment crest carries the vertically fluted cliff band; below it
 * a concave talus/apron cross-section (steep at the cliff foot, flattening
 * to the base) falls to scrub foothills. The massif is 4,800 m wide and
 * ≈1,000 m high: a 560×220 heightfield with domain-warped fractal relief,
 * downslope ribs displaced in depth as well as height, a hand-placed gully
 * network, broad diagonal drainage basins between spurs, two thermal
 * (talus) relaxation passes, edge skirts, 430 talus boulders and 1,100 scrub
 * domes — 150,731 vertices / 261,280 triangles in five ≤60k-vertex chunks.
 *
 * All three seasons share this one mesh (season is a shader uniform, so the
 * silhouette is identical): green scrub over the aprons, the same cover
 * turned rust/orange in autumn, and slope-controlled snow that holds on
 * moderate slopes and ledges but sheds from the steep cliff faces. Per-vertex
 * data carries material, vegetation density, concavity AO and snow
 * retention; the fragment shader adds rock strata/fluting/cracks, scree,
 * multi-scale scrub mottling and snow sparkle under Sun+Moon lighting with
 * an ACES grade. Transparent outside the mountain. No cast-shadow maps.
 *
 * World axes (metres): +X east (image right), +Y up, +Z south (toward the
 * fixed camera). The camera is orthographic and never moves except for the
 * framing solve: there is intentionally no orbit or zoom. Astronomical
 * azimuth 0°=N, 90°=E, 180°=S, 270°=W.
 *
 * The companion `mountain_preview.html` contains the same builders and GLSL
 * ported to WebGL — what you see there is what this file draws.
 *
 * INTEGRATION
 * -----------
 *   Mountain(
 *       season = MountainSeason.GREEN,
 *       sunAzimuthDeg = 180f, sunAltitudeDeg = 52f,
 *       modifier = Modifier.fillMaxSize(),
 *   )
 * Place it over your sky in a Box; drive season from the date and Sun/Moon
 * from your ephemeris. First build is a substantial procedural generation
 * (~150k vertices), done once per GL context on the GL thread.
 */

// Deterministic RNG / value-noise / indexed mesh builder — a 1:1 port of
// mountain_meshbuilder.js in the HTML preview.

private fun mtnClamp(v: Float, a: Float, b: Float): Float = if (v < a) a else if (v > b) b else v
private fun mtnLerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun mtnSmoothstep(a: Float, b: Float, v: Float): Float {
    val t = mtnClamp((v - a) / (b - a), 0f, 1f)
    return t * t * (3f - 2f * t)
}
private fun mtnGauss(x: Float, c: Float, w: Float): Float {
    val d = (x - c) / w
    return exp((-(d * d)).toDouble()).toFloat()
}

private class MountainRNG(seed: Int) {
    private var s: Int = if (seed == 0) 0x6d2b79f5 else seed
    fun next(): Float {
        var x = s
        x = x xor (x shl 13); x = x xor (x ushr 17); x = x xor (x shl 5)
        s = x
        return ((x.toLong() and 0xffffffffL).toFloat() / 4294967296f)
    }
    fun range(a: Float, b: Float): Float = a + (b - a) * next()
}

private fun mtnHash2(ix: Int, iz: Int, seed: Int): Float {
    var n = ix * 374761393 + iz * 668265263 + seed * 1442695041
    n = (n xor (n ushr 13)) * 1274126177
    return ((n xor (n ushr 16)).toLong() and 0xffffffffL).toFloat() / 4294967295f
}
private fun mtnVnoise(x: Float, z: Float, seed: Int): Float {
    val ix = floor(x.toDouble()).toInt(); val iz = floor(z.toDouble()).toInt()
    val fx = x - ix; val fz = z - iz
    val ux = fx * fx * (3f - 2f * fx); val uz = fz * fz * (3f - 2f * fz)
    val a = mtnHash2(ix, iz, seed); val b = mtnHash2(ix + 1, iz, seed)
    val c = mtnHash2(ix, iz + 1, seed); val d = mtnHash2(ix + 1, iz + 1, seed)
    return mtnLerp(mtnLerp(a, b, ux), mtnLerp(c, d, ux), uz)
}
private fun mtnFbm(x: Float, z: Float, oct: Int, seed: Int): Float {
    var sum = 0f; var amp = 0.5f; var norm = 0f; var fx = x; var fz = z
    for (i in 0 until oct) {
        sum += amp * mtnVnoise(fx, fz, seed + i * 101)
        norm += amp; amp *= 0.5f; fx *= 2.03f; fz *= 1.97f
    }
    return sum / norm
}
private fun mtnFbmC(x: Float, z: Float, oct: Int, seed: Int): Float = mtnFbm(x, z, oct, seed) * 2f - 1f
private fun mtnRidged(x: Float, z: Float, oct: Int, seed: Int): Float {
    var sum = 0f; var amp = 0.55f; var norm = 0f; var fx = x; var fz = z; var w = 1f
    for (i in 0 until oct) {
        val n = 1f - abs(mtnVnoise(fx, fz, seed + i * 57) * 2f - 1f)
        sum += amp * w * n * n
        norm += amp; w = mtnClamp(n * 1.6f, 0f, 1f)
        amp *= 0.5f; fx *= 2.11f; fz *= 2.07f
    }
    return sum / norm
}
// Equally spaced control heights (incl. both ends), Catmull-Rom.
private fun mtnProfileAt(ctrl: FloatArray, u: Float): Float {
    val n = ctrl.size - 1
    val f = mtnClamp(u, 0f, 1f) * n
    val i = min(floor(f.toDouble()).toInt(), n - 1)
    val t = f - i
    val p0 = ctrl[max(0, i - 1)]; val p1 = ctrl[i]; val p2 = ctrl[i + 1]; val p3 = ctrl[min(n, i + 2)]
    return 0.5f * ((2f * p1) + (-p0 + p2) * t + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t * t +
        (-p0 + 3f * p1 - 3f * p2 + p3) * t * t * t)
}
// [x,y] control points with smoothstep interpolation between segments.
private fun mtnPiecewise(pts: Array<FloatArray>, t: Float): Float {
    if (t <= pts[0][0]) return pts[0][1]
    for (i in 1 until pts.size) {
        if (t <= pts[i][0]) {
            val a = pts[i - 1]; val b = pts[i]
            val s = (t - a[0]) / (b[0] - a[0])
            return mtnLerp(a[1], b[1], s * s * (3f - 2f * s))
        }
    }
    return pts[pts.size - 1][1]
}

private const val MAT_CLIFF = 0f
private const val MAT_SCREE = 1f
private const val MAT_SOIL = 2f
private const val MAT_BACK = 3f
private const val MAT_GULLY = 4f
private const val MAT_BOULDER = 5f
private const val MAT_SHRUB = 6f
private const val MAT_SKIRT = 7f

private class MountainMeshBuilder {
    val pos = ArrayList<Float>()
    val nor = ArrayList<Float>()
    val col = ArrayList<Float>()
    val dat = ArrayList<Float>()
    val idx = ArrayList<Int>()
    val vcount: Int get() = pos.size / 3
    val tcount: Int get() = idx.size / 3

    fun vertex(px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float, c: FloatArray, d: FloatArray): Int {
        pos.add(px); pos.add(py); pos.add(pz)
        nor.add(nx); nor.add(ny); nor.add(nz)
        col.add(c[0]); col.add(c[1]); col.add(c[2])
        dat.add(d[0]); dat.add(d[1]); dat.add(d[2]); dat.add(d[3])
        return vcount - 1
    }
    fun tri(a: Int, b: Int, c: Int) { idx.add(a); idx.add(b); idx.add(c) }

    // Flat-shaded triangle from three [x,y,z] points.
    fun face(p1: FloatArray, p2: FloatArray, p3: FloatArray, c: FloatArray, d: FloatArray) {
        val ux = p2[0] - p1[0]; val uy = p2[1] - p1[1]; val uz = p2[2] - p1[2]
        val vx = p3[0] - p1[0]; val vy = p3[1] - p1[1]; val vz = p3[2] - p1[2]
        var nx = uy * vz - uz * vy; var ny = uz * vx - ux * vz; var nz = ux * vy - uy * vx
        val l = sqrt(nx * nx + ny * ny + nz * nz); val ll = if (l == 0f) 1f else l
        nx /= ll; ny /= ll; nz /= ll
        val a = vertex(p1[0], p1[1], p1[2], nx, ny, nz, c, d)
        val b = vertex(p2[0], p2[1], p2[2], nx, ny, nz, c, d)
        val e = vertex(p3[0], p3[1], p3[2], nx, ny, nz, c, d)
        tri(a, b, e)
    }
    fun quadFace(p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray, c: FloatArray, d: FloatArray) {
        face(p1, p2, p3, c, d); face(p1, p3, p4, c, d)
    }

    // Irregular blocky boulder: 8 jittered corners, 12 flat triangles.
    fun rock(cx: Float, cy: Float, cz: Float, rx: Float, ry: Float, rz: Float, rotY: Float, tilt: Float, rng: MountainRNG, c: FloatArray, d: FloatArray) {
        val cs = cos(rotY.toDouble()).toFloat(); val sn = sin(rotY.toDouble()).toFloat()
        val ct = cos(tilt.toDouble()).toFloat(); val st = sin(tilt.toDouble()).toFloat()
        val base = arrayOf(
            floatArrayOf(-1f, -0.62f, -1f), floatArrayOf(1f, -0.62f, -1f), floatArrayOf(1f, -0.62f, 1f), floatArrayOf(-1f, -0.62f, 1f),
            floatArrayOf(-1f, 0.62f, -1f), floatArrayOf(1f, 0.62f, -1f), floatArrayOf(1f, 0.62f, 1f), floatArrayOf(-1f, 0.62f, 1f))
        val pts = Array(8) { FloatArray(3) }
        for (pi in 0..7) {
            val p = base[pi]
            var x = p[0] * rx * rng.range(0.78f, 1.18f)
            var y = p[1] * ry * rng.range(0.75f, 1.22f)
            var z = p[2] * rz * rng.range(0.78f, 1.18f)
            val y2 = y * ct - z * st; val z2 = y * st + z * ct; y = y2; z = z2
            val x2 = x * cs - z * sn; val z3 = x * sn + z * cs
            pts[pi][0] = cx + x2; pts[pi][1] = cy + y; pts[pi][2] = cz + z3
        }
        val f = arrayOf(intArrayOf(0, 1, 2, 3), intArrayOf(4, 7, 6, 5), intArrayOf(0, 4, 5, 1),
            intArrayOf(3, 2, 6, 7), intArrayOf(0, 3, 7, 4), intArrayOf(1, 5, 6, 2))
        for (q in f) quadFace(pts[q[0]], pts[q[1]], pts[q[2]], pts[q[3]], c, d)
    }

    // Low scrub dome: indexed fan, 7 verts / 6 tris.
    fun shrub(cx: Float, cy: Float, cz: Float, r: Float, h: Float, rot: Float, c: FloatArray, d: FloatArray) {
        val top = vertex(cx, cy + h, cz, 0f, 1f, 0f, c, d)
        val rim = IntArray(6)
        for (i in 0 until 6) {
            val a = rot + i * PI.toFloat() / 3f
            val ca = cos(a.toDouble()).toFloat(); val sa = sin(a.toDouble()).toFloat()
            rim[i] = vertex(cx + ca * r, cy, cz + sa * r, ca * 0.72f, 0.69f, sa * 0.72f, c, d)
        }
        for (i in 0 until 6) tri(top, rim[(i + 1) % 6], rim[i])
    }
}

// Photo-baked material fields (see generate_photo_fields.py). 1:1 with the JS.
private const val PHOTO_FIELD_W = 192
private const val PHOTO_FIELD_H = 96
private val PHOTO_VEG_B64: String = StringBuilder(24576).apply {
    append("AAAAAAAAAAAkABAAAAAAAAAALQAAAAAEAAwEHAwtCAwMHAQMFAAIAAAMCAAUGAgAEAAMEBgMAAQIAAAMNRAYMVEYPSgtDAwocQhNfVGCQRQ5PT0QLRQkFAAA")
    append("SRB5KIZdJFEAXQwoBGkcBAAAAAAAAAAAAAAADAQIAAQIBBw5TUFRIBAUOWlhFEUoJAQobQAAAAAACAAAAAAUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgA")
    append("FAAMAAQUCAAAAAAQAAAAAAAAAABZAAAABAAAAAgYEAAYDAgtIBwtAAw1ABQcAAwIPQAIABAUEAwECAwICAAgRRAIKAQgCAAEAAAEABAYXTlJEAAgOQSORXGy")
    append("nm2OonlBBAgIBAAtKAiSRbqmWZ5JFAgIABAAAAQIAAAAAAAAAAAAMQAYGAwQEAAgGNeaEAhRBBQ9AEUkGAAYVQAAAAAAAAAAAAAUAAAAABQAAAAAAAAAAAAA")
    append("AAQAAAAAAAAAAAAARRwIEBAACAAIABgoAAAAAAAAAAA9AD0ACAAACFWKGBhxIDUgCE0gCBwUAAQYCCAEOQAACBAEBAAMAAAgCABVMQAgIAQoBAAAAAAAABwx")
    append("NQwtGAwcBGF5KG2q177OxuNFDAQAAAAoEI6OooauvqpNAC0ACAgAAAAQAAAABAAIAAAAIAAkKCQgEAAACJqqMRySDBBlBBAgBBwQKAAAAAAAAAAAAAAEAAAA")
    append("AHUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAcUkgIDkAAAA1AEUIAAAAAAAAABAEHDEAAAAtdTF9FF0gAAwACF0ABEEEAAAAHAwMEAAtFAQAAAAUEAA5AAA1AAQc")
    append("AAAQAAAAAAAAABg1AAAMDCQEQcYteS1lisKayrIQCAAAAAAQGJZNNXGmmk0oDDEASQgMBAAAAAAABAAMAAAAABwIMSgYDAAAADmuPVUcEAhZAAA5XWFlDAAA")
    append("AAAAAAAAAAAAAAAAALIAAAAAAAAAAAAAAAAABAAAAAAAAAAAKC0MAAAAABwYFAQEAAAAAAAAAAgQIAAAAABtcQg9LS0QAAwEFCgIABgAAAAACAAUBAA5AAAA")
    append("AAAEACAMAAQEDAQQABQAAAAABAAAAAAMAAAICBgAZTFFvhQxglVNoi0AAAAAAAAABKJZPdskQRyKBAAYJAAgBAAAAAQAGAQQABgMCBQEbRAcAAAAAABFFBwc")
    append("JAxRAABlriQQAAAAAAAAAAQAAAAAAAAAAC0AAAAAAAAAAAAAAAAAAAAAAAAAAAAALQwIAAAAABwAEAAkAAAAAAAAEAA1BAAAAARVZRwMTTEYACAUKAhJAAAo")
    append("ABQxAAA5AAAAAAAAAAAAAAQgABAEFCAAABAAAAAAAAAAAAAACCQAABAAKBRZdRBJRU19cQQEIAAAAAAAAC0tiqZxMVlZEAwYAAAkAAAAIAAAAAAEAKYEAAAA")
    append("rgQYAAAAAAAYcQQkGAh1AABhfRQAAAAAAAgIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAALQwQBAAACAQEAAA1AAAAAAAMEAxtAAAAACB5HCgA")
    append("ABAAABgAEBRVAAAkAAhVFAgoAAgAAAAYAAAAAAAQBAwQDAwAAAAAAAAAAAAAEAAADCgAABA5TT0oNX2aqmkoMQAEBAAAAAAAAAAAmhymcTEkIDUEECA9BAAI")
    append("TQAAAAAABIoAAAAAdQQACAAAAAAAGAAcNQBJAAQUPb4IAAAAAAQYAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAGAAAAAAAAAAAGAQAAAAEDAgIAAAQAAAAAAgA")
    append("HII1BBQgAKYYAAwAACAAAAQACFEEAAQAAAAIPRAQABAEBAwIAAAAAAAABBwoCCgAABQUAAAADAAYBAAAAAwQCBQQOV1RWYqCTSQckgAAAAQkAAAAAAAAYQQt")
    append("QUUIBH0IEDUYAAAtRQgAAAA5RRQAAAAACBQMCAAAAAAAAAQ1ngAcCAwYVYIAAAAAAAAABAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAE")
    append("AAAAAAA5AAAAAAAAIIYEBBAYAMIAAAAAGDUAAAAAKEkYHAAAAAAALRwQAAA5CCAABAAAAAAAABBFGHEcAF0oAAAIAAAUAAAABCAcFAAAEBwkTRxlBAAIYQAA")
    append("ABwtAAAAAAAAHAAAABwMDBBVGDEYAFEIDAQADBRpfQAAAAAAECAYAAAAAAAAAAAEZQwQFAhROUEAAAAAAAAAAAAAAAAAAAAACAQAAAAEAAAAAAAAAAAACAQA")
    append("AAAAAAAAAAAAQQAEIAAACAQIAAAAABgAAD0MDBwAFGUAAAAALQgAAAAAHAg5HAgEAAAABBgAAAAtLSgABAAAAAAAAAAkAHkkAD0AAAAAAAAABCgADAgAIAAA")
    append("AAAkPUltCAhVCAAAAAAAAAAAAAAAEAAAAAAINYYYDFFJAEkASQAAdS0IkgAAAAAABAAAAAAAAAAAAAAABAwECAA5AAAAAAAAAAAAAAAAAAAAAAAAQQAAAAAE")
    append("QQAAACgABAAMDAQAAAAAAAAkAAAEAAgUdQAILQQAAAAAACAoin0Egi0tTSgAABAASQAAAAAAAARdAAQIAAAABAgAAE0EXQQEAAAAAAAAAAAAABgQAAAMGAAA")
    append("AAA1QUFFCBQAUQAABAAQLSQMDAxxBAQAEAAAAAAAAAAACAAAAAAAEKogPTW+CCgAggAgtoYUEAQAAAAAAAAAAAAAAAAAAAAAAAAALQAYAAAAAAAAAAAAAAAA")
    append("AAAAAAAAHAAAAAAEVQgAAG0EBBAIAAgAAAAAAABVAAAAAAQQFAAUEAAAAAAANQhhio5JPQAxFCQAAHUEHAAAAAAYABg5CAgAAAAIDBAABKIEHAAEAAAAAAAA")
    append("AAAAAAQEAABJECgEABwUjgAUCBQAFAAACAAECAQtDBQkAAQADAAAAAAAAAAACAAAAAAAABQkjj2KEAAASQgYUZYxABAAAAAAAAAAEAAAAAAAAAAAAAAAngAA")
    append("AAAAAAAAAAAAAAAAAAAAABwAbQAAAAAYKFE9VaYMEE0AAAAAAAAAAABBAAAMAAAAAAAAAAAAAAAASQQoBH3CAAgcADUtAJooORgAAARRAE0ICAgAAAAcIBQA")
    append("IE0QHABZAAAAAAAAAAAAACQIACQgAFExNTUAaQA9EBwAAAAABAAAAAAkBAAAAAAAABgAAAAAAAAAIAAAAAAABAwALRB5dQAAlhw1hjUEAAQAAAAAAAAAKC0A")
    append("AAAAAAAAAAAArgAAAAAAAAAAAAAAAAAAAAAACBgApgAAAAAINTFlEKYUAOcAAAAAAAAAAAAUAAAEAAAAAAAAAAAAAAAQhjUgABQxAElNAEkxAGlhPQAAAABF")
    append("CC0AAAAAAAAcCBAAihBhLQRNAAAAAAAAAAAAADkIAD0ADBRJOSAACABdJD0AAAAAAAAAAAAEAAAEAAAAACQAAAAAAAAADAAAAAAAFCAAAABpbQAAdTmOQQwA")
    append("AAAAAAAAAAAAECQAAAAAAAAAAAAANQAAAAAAAAAAAAAAAAAAAAAAGAgAZQAAAAAEaSBNCOcUALoAAAQABAAAAAAEDAAAFAgAEAAAAABJAAAYdUkQMSgAABQQ")
    append("ACQAAFE5NQAYACAoPQAAAAAAAAggAAgAOQhpAAwMAAAAAAAAAAAAABQEABhNQRwUICQAAAAEDBQAAAAAAAAAAAQAAAAIAAAAAAAAAAAAAAAAAAAAAAAAMSAA")
    append("AAAAIAA9jnFpGAQgAAAAAAAAAAAAAAwAAAAAAAAAAAAADAAAAAAAAAAAAAAAADUAAAAANQAARQAAAAAAGHE9DMokBBQMAAgAHAgAACQIBAAAED0MQQgUABiK")
    append("ACgMNQQEfZIAABAAAAgAEGV9AACOAAgYQQAAACAAABgMAAAACBBFCBAEAAAAAAAAAAAAAAAAED1NDBgENVkAAAAAAAAAAAAAAAAAAAQAAAAIBAAAAAAAAAAA")
    append("AAAAAAAAAAAAGAAAAAAABBRlVTEACAAYAAAAAAAAAAAAAC0AAAAAAAAAAAAAAAAAAAAAAAAAAAwAACgAAABBggAASQAAAAAgALJtEIoQEAAIAAAAWQgYAI4E")
    append("MQAEACgYeWF5HD0kACgAHEUAGEU5UTUAAAQAPX26JCi6BABVJAAAAAgABGkYAAAAaWFNFC0EAAAAAAAAAAAAAAAIUTkgAARJJCQAAAAEAAAAAAAAAAAAAAAA")
    append("AAAQBAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAABBYVEICAAAAAAAAAAAAAAAAF0AAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAkaQAASQAABAwgMTF1AAAU")
    append("AAAAAAAANQAICGEQKBwMEBQEKFWOQW0cAAAEAJIABAAoYQAAAAAAjk2mRRyuGByqHEEAAAgAFEkUAAAAqlkMAAwAAAAAAAAAAAAAAAAMNQBpACgoEBAAAAAE")
    append("AAAAAAAAAAAAAAAgAAAUCAAAAAAAAAAAAAAACE0AAAAAAAAAAAAAAAAUohQIAAAAAABBAAAAAAAAABwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHAAA")
    append("cQAAABAESRBNQTVhAAAABAAIAAAAEC1BUSRJJBRdBCQIRZY1AAAgAH0AAAAACAAAAChFnmHCICDSTWk1aUEAACQAAAAEBAAAcRAYAAAAAAAAAAAAAAAAAAAA")
    append("ABAEACAoFBgAAAAAAAAAAAAAAAAAAARFAAAEDAAAAAAAAAAAAAAADD0AAAAAAAAAAAAAAAAAggAAAAAAAAB5AAAAAAAAAAAUAAAAAAAAAAAAAAAAAAAAAAAA")
    append("AABxAAAAAAAAEAAAbQAABAAYRXVVvgwxJAAQBAAgAAAoCE0UDC1pEByWAAgETTkxAAAUABgAAAAEAAAcAEWCKMaOIJaOZS0MVQAAAAAAAEkMEAAASVFBCBQA")
    append("AAAAAAAAAAAAAAAUAD0QBBRVaQQAAAAAAAAAAAAADAAAAAAIAAAADAAADAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAGAAAAAAAAABZAAAAAAAAAAAEAAAAAAAA")
    append("AAAAAAAAAAAAAAAAAAB5AAAAAAAYFAAAeQgAHAAgFF19pjUcGBwIAAAMPQRBABAMBEVdCAh9AAAASQgAAAAIBAgAAAAMAAAtACQoLftNLWUUSUkACAAAAEUI")
    append("CHkEAAAEST1FHGEAAAAAAAAAAAAAAACKCAg5ICgkQQQAAAAAAAAAAAAAAAAAAAAAAAAABAAEKAQQBAAAAAAAAAAAAAAEUQAAAAAAAAAACAAAAAAAEAAgAAAA")
    append("AAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAMAAAAAABRURAAohRBAAAIAMo9cW0ECHEIAAQkRQQUJAQEEBBlAACqAAAEQQAAAAAcBAgAAAAAFAAUAAAEhs41")
    append("qi0MGBQAAAAAICQIICQcAAAErghBLS0AAAAAAAAAAAAAAABBABAQDBwxNQAAAAAAAAAAAAAABAAAAAAAAAAYEAAAACBJBAAAAAAAAAAAAAAARQAAAAAAAAAA")
    append("AAAAAAAAHAAEAAAAAAAAAARlCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATbTQgAFBxBAAQECJIxaVkEAIoAADUQRT0QYRgUQQRBAACGAD05DAAAAAAtAAAA")
    append("AAAAGAAAAAAEwii6TZoIIC01MWEkCAAAihR9AAAIbQBJAAAAAAAAAAAAAAAAAAAUCDEEFARRJAAAAAAAAAAAAAAABAQAAAAAAAAoIAAAAC05AAAAAAAACAAQ")
    append("HFEABAAABAAAAAAAAAAAABgICAAAAAAAAAAAAAjbNQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFXCAAAEAAggAAAEJFlhSXUAACAEAC0ACCAgIAgUDAAIACQg")
    append("AK4kAAAAAAAMCAAEAAAYAAAAGAAIjmm6FNIACCRNUbYoQQxNfW11BAAMNQBRAAAAAAAAAAAAAAAAAAQcKAwMLRAgcQAAAAAAAAAAAAAAAAAQAAAAACAADAgA")
    append("AAQYAAAAAAAAEAAUMS0AAAAAAAAAAAAAAAAAAC0QEAAAAAAAAAAAAARRDAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAIZJKAAIAF0tBAAAKBB1XTEACAAEEAwM")
    append("AAgkBAAcAAwMCDEETRQEAAAAABQACBwQAByOAAAAHAwopn2GNXEAAAAAAE1BjnllLXVJAAAxLQAcAAAABAAAAAAAAAAAAAAAPRAQPSQIRQAAAAAEAAAAAAAA")
    append("AAAcAAAAACgABAAAAAgYAAAAAAAABAAAJAAAAAAACAAAAAAAAAAAAAg1IAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAcAAAAAH33OQAACL4MIBgE")
    append("FCBpaQgISQBJAABFIAAEAAAEADk5SRwAbTkEAAAAAAAABIoAAGFRAAAAACCm112+qgQEHAAUALpFIG1RIIIgFAB1GAAAAAAAQQAAAAAAAAAAAAAADAQ5eXkc")
    append("FAAAAAAkAAAAAAAAAAAAAAAAAAgAIAAEAAAAAAAAAAAACAAAjgwAAAAABAAAAAAAAAAAAABRBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAABAAAAAAUAAAA")
    append("ABTzFAAAZWUUdRxZBHU1ugAcFDFNKAAAOQAEBAQAFM4tVQQUTWUcBAAAAAAQDIYASYZtAAAQBFF1morKrjkIWQg5ANeSCEGmcX0AFABZCAAQFAAATQAAAAAA")
    append("AAAAAAgAAACKIEUkOQAAAAAIAAAAAAAAAAAoAAAACAAAKAwAAAAAAAAAAAAAPQAAYSgAAAAcAAAAAAAAAAAACABFDAAAAAAAAAAAAAAAAAAAAAAAAAAAAFUA")
    append("AAAAAAAAAAAADAAAAG3vHAAQypoARQAoPbYUaQwkBBAEeQAALQAUAAAMOVldKABhFAQ1IFEAAAAgRYqO14aedXEoEHVpkorXPQAEomUcGF1JQTGqUSAAAAAQ")
    append("ACg9KAAAAAAAAAAAAAAAACAAAG1RAAQYbQAAAAAEAAAAAAAAACBdAAAALQAAAAAAAAgAAAAAAAAAIAAIRSQAAAQgEAAAAAAAAAAAQQBRIAAABAAAAAAAAAAA")
    append("AAAAAAAAAAAAAD0AAAAAAAAAAAAADAAAAIK2IAAxpmEAAAAYaW0AcQAEAAAAJAgcWRwABAwEACCeEAAIIAQQPV0AABhRwm2i//9NWc5tcQiWeY7fogQAccZZ")
    append("HAAMPaJNAAAgAAiOICgoCAAAAAAAAAAAAAAAEFEAAFkQKAAgGAAAAAAYAAAAIAAQIKY5AAAENQAAAAAAAAgACAAAAAAAAAAAKE0AAAAABAAAJAAAAAAAGAAU")
    append("AAAABAAAAAAAACQAAAAAAAAAAAAAAAAAPQAAAAAAAAAAHAAEAKKCCAB9YRgAAAAYCAQApggMIAQABFUYLWkANQgAMcKKAAA5BGEIEAQAAGmKdVFx6/uyzqaK")
    append("vgB9fYb7sgQkQZ7GOQAAPYIkAACOAFFRSQAEAAAEIAAAAAAAAAAAQTEAMSRZZQAkAAAAAAAUAAAAeQA9ebI9AAAgGAAAAAAAAAAAXQAAAAAAAAAAGFkAAAAE")
    append("BFEANQAAAAAENQAAAAAEAAAAAAAAAAgAAAAAAAAAAAAAAAAgFAAAAAAAAAAABAAECIZNAADOeVUAAAAkJC0AugAEppYQBBAEMTkIYQQ10vOSZW0xCCQYABwI")
    append("XWF1WZq6gvua8/+eqhBNorb/igBp2zHXpmFhxsItHABhIHkgFBwEAAgtQQAAAAAAAAAAUQQAUS1RSQBxBAAAAAAAAAA1dQR113UQAAgQAAAAAAQAAAAAKAAA")
    append("AAAcAAB5EAgAAAhBAC0IXRwIAAAcLQAAABwEAAAAAAAAAAAAAAAAAAAAAAAAAAAxCAAAAAAAAAAAAAAAGCiSHEXztrIcIAAAMTWWaQgAbe9ZLTkAbQgYAIaq")
    append("0vvjqpYIAARVAKJxhqaqsrLS7/vOpsI1346GgoK6tgySxl1NTcq694qCNRgICIo5AJIMAE0MPQAAAAAAAAAAEAAALRAcJACCAAAAAAAAAAhpGATSrpIQAAAE")
    append("AAAAAAAAAAAACAAAAABFDACCLQQAAAg5AAAAACQMAAAkDAAADE0ABAAAAAAAADUAAAAAAAAAAAAAAAAkBAQACAA5AAAABAAAHC3zxu/v52HO2wgAGJLCyn1x")
    append("utLvproYPU0Ahuf/9+/73yQAFAwIhnFZaZai49dh1+uOlp4xdTldhtKmxk2KqteWbUWy6+81MaYQRbJ9AE0UJEkIdRQAAAAAAAAUBAAABAQtAAAoAAAAAAwA")
    append("AFFlAACmutIUAAAAABQAAAAAAAAAAAAAAAAoEABNaQgAAAAAKCgMEHUAAAAkAAAAEDkACAAAAAAAAJIEAAAAAAAAAAAAAAwEAAAAIGlFAAAAAAAEYZLK++/n")
    append("5z2enihBqtv7zt/Xrqb/zsaeSdJZ0vf79/u6864tggAQpgQgedKORX0o19soYd89RUU9dcbKfW3C0u+mdQjf/+OOac6Oiq5pAAhNYRgtfRwAAAAACBxZDAQA")
    append("AABRAAAADAAAAGEAQZKmIAimnoYMDAAAAAQAAAAAAAAAAAAAAAAUBACGJAAAAAAAUTE1GFUAAABxAAAASQwAAAQcAAAAAFkAAAAAAAAAAAAAAD0AAAAABIoM")
    append("AAAAAABR22m++/e283lJea7S7+/f97q2mrLr16rOdW3/vu/7477f3/ui76aqlnWKfffGMVmq15ZpBIY5YcZ5gsJNEFWa5/vv65a6+77X56rjui0YFCBhIBxV")
    append("DD0AAAAAdX0tCD0AABQQAAAAIAAAKGUAUXGOqo5Vxi0cEAQAAAAAAAAAAAAAAAAAAAAMAAA9CDkAAARhBAAkSQAAAAB1AAAAVQAcABAYAAAAAAAgAAAAAAAA")
    append("AAAAADEAAAAoACAMHAQAABymnrbC87rG79sk2+/f/+O2qop117LrwuOKda7vhvPGwhwxVevb0tfr2/f7wqbSSXmS0l15FFWWntKavr4tinmqruv/vu9B79d9")
    append("/577ngwABC01CAwMADEAAAAMwnkIXSQAOWUAAAAMBAAxJGEkGMZdksZl/zUQCC0AAAAAAAAAAAAAAAAAAAAAAAwEHEUAABxhAG0QYQQAAAQ1AAAAEABpAAQA")
    append("AAAAAACGGAAAAAAAAAAAAD0AAABxCAgQdXUUQdfCfd9pvsq257aG2//f8//SwiAIQT2+ztKKrsrf6+dtPQBFOV3X/8bz3/f7mn2aHIKKxqoMOV22xteWqqLX")
    append("71XvsoL/ru+Ktoo96+//SRgACAAECEEEAAgAAAAoeTkQXQAEvi0AAAAtAAAoKFllIKJpjr6y26Y5IBQgAAAAAAAEKAAAAAAAAEUAABAUPQAAABAAHKYMBAwA")
    append("AAwxMQAAIAAoAAAAAAAAAABNbQAAAAAAAAAAADUAADU1QYptfXlpYe/3svtt27r3qt/n9//nytf/kgRBIBRp+8rf6+fS5/NhEH3XaUmK/+fb9//3lp5VeUGe")
    append("YaogfQSimrKyeSD3957vvrrz/9fjrkkAmufzUUUMFAgINWFJCAAAAAAtaT0gAABRngAAVSgkBAAIgm0xQUGaNXHvcfM5BAB1AAAAAAAxzi0IAAQAAHkAABAc")
    append("KAwEAAAAHCAoEAAAABRhbQAASQAEAAgAAAAAAAAAPQAAAAAAAAAAAC0AAJIgmrbnNZKqUfPvYf/fccLz79e29/vK79ffABhNMX1R++/O16qq8+vOHIaexq41")
    append("Ycrn9/P/so6atm26tqLKri19KNv7cVGycc7n89uu43nj47YkBHHb/zEoBGFVPaZ5rhQAAAwUOZo9NTExRQA1wpIMBAAUgq4YSWm2AIL/IOMkAABRAAQACByG")
    append("iigYAAQAAAgABAgEaRAgAAAxAAAEAAAAADFBIAwtGCQABGUEAAAAAHkIRQQAAAAAAAAAADUAAFFx19Kmfe+ezu/7uv/fMTHvxp6mrvvv6/dlAH0gIG1l7+/v")
    append("+//72/91JAxt60llCE2m27rXlq7r/9/r6//387aiOZ7jXdvncfPGolG2oqrSZco9AFnK0rKmIE2ymrq6tpJlURwAJFGifU0AACDGTS0AAEW6Pb4Qgs5hENvv")
    append("moJhAAQEDAAckprONQiCAAAkIAAABD0xCAxdABwkNQgQFAAAACQUGD2GHBwACHEIACAAAOMIUVkAAAAAAAAAAI4cAC2u49uaotuS76rb0q41ORjbloZl267G")
    append("844AIGkAJCCu3//vuuvr0pJ9ZUVtcRCSAAw11/eW//Pzpor/xpqu1/+2rnnSUc7//9fCPV33uv/nZftZHL5R39KOLV26401JOd/7klUEGAyyLQAAAHFhBE0A")
    append("AMqGxsJ9bcJxxu/v83GiGGVFaUmm0v/Xyoq6lnlNrgAAAI5dEKKiBEEg1z0EBAwAHIocIAQxBABJFBQUBD0AANsMHCBJAAAAAAAAILIUAIrfnrai77qWwsrC")
    append("siBhfVm2lso5vrpxilkAbQQAQXk1utfn58bv62F90iCmQRQEOSgQosLnupaSnk2CbRAIZfPjYY7Chnnr/+PzXY7j2/fzmtfGNdd9+841aa6m30kEZbrXIKJl")
    append("BAR1AAAMfYpNAMYUSb5d29/jTYrX99L7/7Z5iq6Gupr769+m10GWzt8IcQAAAL4cHFmqACAMwiQACAAAVTExDAAxAAwxEDkodTkoBNsMFAA9AAAAAAAAeUkE")
    append("PeP/uv99yk3jksp1zhzXknF5ssppdUFBLQQ5NQAYaV0Mouf75+fz/++ymhTnsgwAhiA5USS6qoq+vnVNYTkAGGHOSdvSpj1p/+OKJGHroq7nyn1pLYbKxq5x")
    append("uqbb15JNac6mimUxQSgxABSSdX2eRXWKYePv0uuurvvz3+v/47rCtpa++47S7+vO3yBdpraCxj0cANeenhBJHBAc4xwUCCgAihgoJFFpTQwEEFFdkgxxLesQ")
    append("QQAAAAAAAAAEshAItu/vvs7GkjnGlq7XxlWCEHXj69eaijVlAABRBDWWEDU5nsbK7+/b5//jlqrb/zUABCggoghV+z2yor6yspo9WX2KbdLbyqaemtuKfVXj")
    append("moKa55KWwgjSkp55+7au2+dNjnGW61kYeRAMRWGeSbKmZV1pffPv6+dFYfv/+///4+fOssbz0u+aovPzWV1547JRknnGdb6igmUtdU0QVbaKAFkYugxRXUk5")
    append("giQIBGV5dSA5mteuqgAAAAAAABSi+xhZ887Xwobr36a2feeqdZZBMaqu967Otn01MaKCWYZlYdJhyvum6//722nb9/PGbQwABC0geRwgippx2+PKRfu6rp6e")
    append("dee2687fLfeK857bmtvS++Oa80lpuuNV19t9tvscQS3C201VRRRNcWHKbZZtNZaK3+/n25qO2+P/++vz9//3+4rrptLj4//zlsKu1+OyriSG66Y1McqujhgA")
    append("BDm+GARRMT0UDFlhUTUYSV2Oujk1jkX3ZRQEAAAAAFHK94LX/56a77rbtp77ruNVUb51bZLOntLrvkEQqtvv37q2QWG6dZ6e7/fryrbXxvvGCAQkABwAQZo1")
    append("Mc7O533OAIrXttKSacK2uv+iEMqm8/Pr////+8rrqq620sKC28aWSaK+DHW656YUKHm+Maq2fT2a0nFpyvPX55rj6/PS//fG//v3/8LS49vznvvv//+e72Xb")
    append("88qS29uaLcZ1LXUUCFmigmmeEEUxssq2kkmCaVWazmldLRDCYbooAAAADNLb//v/xmnO947CrnmudcpthnVdlrbjpvPXogCq73XOusbXcX15tjl1dd//bfd9")
    append("dd+6wggcAGlBnraqirb/53HnLQyOqrKa0rrKns7/eVme/////+/3vp7f46KKos5dcYrjssbXUXHrlpKSXdvzWZ5psqr30pbb1+/v177Oyv+e+3mG4+/787LX")
    append("7/vXUfv36/vb277S89/32//3OVVRLcZJFKo9NbrCLRRZmk3b147/vihR78oYUQBxnoIgAAAAWZbS4/v/yrb30rLG26I5tuuixraKxvPjwsbz1wzfsiDK0vvG")
    append("US1NwrqSXarj49/GgppVxhAEJMLG75LX72HOqp6WVU2mxqb30r7vsrL/+4aG86rj0qKq0uuO321Jwn22KACi35rjbYr/ljWejoaKrjU1lprCxvvb96q2dcLr")
    append("+9uivlXKwvf/88qi7/P36//33+/vhrLCkr7jvu+OWUk1eVVhLUVlvrqqZWE9KILCqvfCkhQt66KCXQwcthgAAAAAhoL/2//v9+frjrp9+9dN9462zt9h97bf")
    append("vtfj34LrYapx/9uqPSg1wvOeprquxt/752mW40UEituy8+vr5whZNVUMDHmmysL/15LK1+/////z/46mwtKyyrbbusZ9zoK+ghia32maLbbnUTlBWTFpkraq")
    append("8z155/u29759wq7367bG29v/9+//6/em6/////P3df/vmoJlZevvqq55snmiznmWRQg9fee622UgbWGaff+yyoJt67a6QSAIklUAAAAAuu/v7+/f/+vCsnmK")
    append("/8Jpoqb7xveaitK+4/uupt/ONf+W64LGljnG73WC2+f3/9vj/9/7/9sxvtvO7+/GdTG+cT0AAGmi//v//4rbvrLj//+69/uuttvnhuemirJdovfXvmkokvey")
    append("0rrKEABxRYKi+8aS28a6//fX0u++857j1/Pf//f///f////vquP/////rvv7sn2y58KyaWX//8r359uWWSgMNb6W1z2q0m2C17bnZY7OymXStmWWJE0ADAAc")
    append("qvvK79fn79f739vC29eyFOOK16a6OUHXuvthTes5iv//73WKpoJp922Gut/z9//n///O8+PK+32m49vjxsLfykUAADXv/3H/zvfOtt/nvv/j2//b33mu/9+q")
    append("0sKWtvvb/+eKTcrbxvvGAAAcZeeq60Ftvu9J0utVgtfbvt//sv//8/f///f///Pv4/f/////Nffz867SznmadVn3svff/9Ljps6umr5Vupq6566W76bjvlHK")
    append("21FBtjnOGFEMRQBFgvPr9+f/99/339v3lorKosJN78ppphyi3/uWEKoc4/v713FtnopZkr6aSc7r5/v////7+////8LGprr7//vf71UAABDv95bv27J517b7")
    append("lrLj+4b//6K20sb7faaqxvf/48bf56rKnuOmAAAAiq7rvi3CwucM2/u6svO64/PC3/f/4+fv//////P3//vz////QdfvyvP3/+vr357nzrrr0s66xuPznpKu")
    append("ZdvSec6S2+uS+6rG15ooshyGEAwtHBSKnsr/xvf/+9vr456ynuvG29KO++PbTb6C++fbLa6u/+/33/O6ko6SVcLbop7Outvn//vn///////347r74///51EA")
    append("AASq0tLO55Ix24auLev/4wz//9/348Lv16bXpu/3fcL7+22uLXlpAAAA68LCppaustt97/v/rvfv//+q7+f7+//v//f/++vv//vz////6/fKnu/34/PKzvvb")
    append("/9/7ad+S36qWpk2GivvzxpZtwvuy0nXjvqKq72GWAAAQgkHG79f/6/f74/e+tuu6ruvX0m3fzqp1kvPj18bjxuP3zv/748qOtoKGptL387qatrbr///z///v")
    append("6/Pz98Lb1/f/0j0AAACWruOu732GqmWyBMr/64r/9/vbus7rusaOou/rfeeywrLOYSA9AAAAsuN9msr3tuPb/+v7/9L/98br7+f/5/v/9/v/////+/v/////")
    append("0uum29f34/PS7+vr/+vSRff7856m977G64LK57JVzuuWfdfvcev734q6DAQ5jhzvuufX8/fz2/ui0u/O2+/3777fgobO9/v3187zouv/3/fvxqai95q+4++C")
    append("yuf38/P7//////v/6/O618LK5/f/hkUAAAC+huPr0oLnxnHzRa7/7//7ztt936LzvuOCbdvb/+Pvhr7X1xAgRQAt286Gym33qpLr4+/j38r3/9dV/+/v9/f7")
    append("7/v7+//n9///+//745bf+8Lvzuvnrt/35+fGts7b2+/S967r0l1xitIt4//bov/3pvvvsgxVEASKLW3r0q6ewvvj/+u+97a63+vr++vKZcb3+67ntu//9/v/")
    append("1+vXut/S+8LS58pZdbqS/87/8///////0ufjxqam//fXlpoAAAjj39f3xra6js7v7/f/5/v/511h39Lzuq5Nouvn/8b/56aa2zl1ugA1wuPj/2H/+77rypr7")
    append("59/39/8ovv//79/////7+/fv9//v+/v7/9vX/9v/htvrvvvrru/SztL3886O46r/uqaqxs667+/b3+f3////VQQIaQAkUffj96bj49LS/9ey38bn6//z0rKW")
    append("mrrz/+P7ju/j//vvgufn17aqzv/ftrLSmq6m29/39/v////nxvPX676G766m+/+eln3j9/fvtut9wvPb9/f///v7wsLO6773yoIAUeem6+++zpquygQxlgAU")
    append("Vbr3+9//zuv/86r//+f/xv9Zyvf/++f///P//+f7///r//P3+/uO59/76/vr0vPK0vPb79Lf0sKutv/OptJhuqJ53/ey89fS///SAAwAWQAEae/71666/77b")
    append("/8K2ru/z7//j887r+87////7Yf/f///XwtLf9+f3yvuqsoqqzu+K487z////+//316rr48q+69L7//vn////9//j676+0v/f/9/7///zwrbSntLC45YMGLZp")
    append("rqZhjpaK0lUMWQS6GLqW0vd5vu//8/fb69L/xv/j2/v3+//7//fr88bv6/v7///359+6quvv9/fr/+fK38bzzuffpt+yTdLSnpbKqoqG2/uK+9f3/9sxAAgA")
    append("DAwAhv/z89Ky/+vn99v7ebrjwvvv97rr+//z///zxv////fzuorz6/Oa37Kq//fOtlUkkvP3++v78+f7+8bvzvP3rtf3//P3/////////9LKvufX/9vz/9/j")
    append("lu/X36qGnqKOOUl9wp41PUmOzkVRDIrKlooI1/+Kqv/35/fz+8rzuuf7387/99f///fz+/f///v/+++q0tKu5/Pb+/Pn3/fGytLK/+vr6+POYdv3gmlxfW3C")
    append("js7bsuv/+3kAAAAAAAAM7///8/fC6+f76//bWbKO8/v749L7+/v/7+vn//////uuXc73+87Olpbj//f7klVxqs7/9/P738rX/++6ku/z////9/fn+///+///")
    append("//v7/9vv7//7/+O2Ma7Kzr66131ZGDmqTdsYZVm2dWEgUf/n/xwtuv+u0v/b++/3///n4/v//87799v7//P3+/Pn9/v/55qKqsLKruf3++PG2+/778bGzv/z")
    append("tuu6vrbCimVt647f177/mv//+44AAAAAAAAQuv//2+vf9+/78//CmtfX+9fj1/fv//v39///8//3//e6pv//58bOqtL/+/Pr9/vbwv//2/frzmVx86Lb7673")
    append("8/P/po7O38rG6/Pr/+//99/zwu/7/+fKMbKCrvuispIkeZpZebI1NZrKGBxRkvv77wSeZcKizrrC7/v/38L35+v/78rr5/+m/9f7+/vn///r67bK//vnyo7b")
    append("pn1Z1+vz29vz/+P/4/fSwpKuwvPf+++C8/em5//71yQAAAAAAAAo3/v/7+v/0u/718rn3/PX49e68/PS8//z+///8//r/+fz9/v74+uO/77/6/P/7//X9+/3")
    append("yvNhfcKqzoai49Lz5+frQVkxjpq636bf2+P/7/P30vf//9LCunHOur7X39Jt42GWupqKJDWWEEVlluPS35Lz6+Pv+9uG58b7wr7/9/Pr61n38+Pv/7bOws77")
    append("+/vf+8Lv6/vv77JhIBBJ5/P3/+fb99/3/9vjymnr+7bf+/NVhvu2///vmgAAAAAAAADC//fn6+/Xzvfv97rnlufz28r///v76+f7//vX/+vf//f/+///+75h")
    append("/9/v/+Pfrv/3jrbr7/uyzsKGnrq2wqr3uv/jms4YcZa+zsL///Pzzs6W39//+/vjnoJtvrKGus62+6ayumGSRVWOaSBBeefr5/P/4+O2/7Jl74b/3+f38/fj")
    append("1wjn/8rj87rKcdL39//z8/P/6/vj38o5AAzOxvvz++f/6/f36/Prztff11nv8/vrmuv7//+SkgAAAAAAABjf+++i9+/f/+/7ruPvSd/b9///9/vv5/P79/f/")
    append("/9fv////9/v3623Gxoam587vcc66vv///+/rvvPr29fz88Ky39LO631NbUFh9+PG/9/rilFVZfff//u+wu/jsueauuvC6/vr+6a+VSAoUQBNru//kvP/yuu+")
    append("/2GK96b/yvvvktf3/3Ge+7bK+87/lv//9/fvzuf3quP/68ayfar/6/v379L38+P/9+/7//vSptf3977bQWn79/+aSQAAAAAAAF3/5/e+9/Pv17L3vtvvys7/")
    append("/+//2/P/+///4//v//P/+///+/Pz//POwufC+7q+yvv7///f8/Pr39Lr2/fX786e677b96rKtm1537rS79e6221NOYKy//99suf324b7367nyvvz9/e+qrIQ")
    append("YQB92+vGqr7/9+fn99eq3+P/6///ht/nmmV558L7////6/v3++/vvrbS0s73167X/9/r//Pzpsbb9/f79/////dh6/vn89KyPST/8//rDAAAAAAALdv3+9/3")
    append("/+v7yq7X7+fS+/vCzu/b6/vf//fj5/u+9/v////r//v/8/vr58Ku///S9/vj////+/P/++Pv5/vv0tvKztv389/Czqay145huqphaT0Mpp6O//NhmtLOouvr")
    append("stL70tfv+/Pb954URSiCksZNwjW++462vtvK5/v/7+f72+frqnmOutv7////4/v//8aW38rS8//7ytv30vPr5/fSzueyyv/b8/fz567GZevv0vuGBDX///ue")
    append("BAAAAAAIsvf7/9f///f/89Ln49tp3/vGpvuq9//3///78+f7+////+/////7/+ff/8Lj+9v/5/vz/////+Pj25rO69++2+/G9//7xpKStoJh39J516ayoj0Q")
    append("eZKi3+eScarvguPrvsrnxtv79+uy/74oMdK2ooqO29fr24aihsrb4+/7/+vv89L/85auwuv////78/P787q2++e62+/v65bv+///yuey686ayuf/8/Pb67b/")
    append("RfPX88K+Sa62//81EDUAAABN9/ff2+//7///48rCyvtxcf/z++/7//P7///77+//6+vv/+//+//z/+vj4//798r37//3/5rn19Lruq7K97rSsuPb9+/jys7/")
    append("0tfCztvj8/v7+85lVXmmpvvbirKq/9/Czuvr/+/z++Pb5+umDEl10l3v3+v3ymm6qsq29//3///v7/vv+86e1+////v7//P/4+vj7/vb4/uunlnS4/vKus7/")
    append("zvfS87rv8/fS5+PjXdLn/57Gccpx9/cUqpYAAAB51/vb2/fr9/v788qi2/+mnvf/9//79/////P3////9+vr8//3/+/3jrK6+//n//Pzvr66sr7Gsrrfoobf")
    append("rtKqssq+9++e1+/z5+f/zu//1773++vClnmugvPKXbJV9//ChvPK/+/v9+//685tECAUfX3/2//789L7yuPS7//j79/f///v///r+87////r+/f/tv/r8/Pz")
    append("/+95xs7f0uuahq7f////66bj+/fz9+vrmrrn72Vl0nm2uvdhfQgAABTj+/ff3/vj/+v/7///9/e679L78/////////////e6+87z8//39/vv48q64+Pr79LG")
    append("bWWW+/Pz//POwn19gqbC66rX+/f30tvr8+f79/e+yqqW///rxrK2psLXksqy57L3ztvf8/v76+Pr/55RLU0ojsbX0u/f6///1+e259/b0tf7//f7////////")
    append("///r+///4//7//fz/+dF7+frztLjkuum6+P7/87n487j45aG75b/zmVV48JtSfeyOVEAADH7/+/nytLj7/P7//Pr9//b/+P////7////9+v3//vf//P/+///")
    append("//f///P///vr98Lr5////////+u+qmE5irrf733f9+/z1+P79+vS/+vbun0k0uv/++vrmsKy4+vrvufX99+669/n8/Oq0pqOQRwozvvO5//n68b7rtLf/6bb")
    append("vv//9/f7//v/////////+9/3++/n8+P//5oktnXv//+CCLK+ce/O7/fjwqbv787b88LG122i27KaYd91rnkABML//9/v9+f///v////z///36//3///769fr")
    append("3+vz5+vnxv/7///////3///X/9/KcYrb6/v3/+OS256qYRg9aYquws7S6/vf7+vbktv7++v39+PG5/f785Lr+7Kq9//v6+PC67rf79Lrytfjrq51cTUotvPz")
    append("99/n/6rrxs7f//P36///+//7//v/////////97r7887v7/Oi721NVc73/76yTfPjjvf34//X18qa+/v31+eK467z7+vjzqqefX0Ejuv7//vj9/Pj8//74+f/")
    append("//f/+////+/3vm3b19v32/f3////6/////v//++m41UEFEmG79v/77bG7znSWQAkRW3G4/vSzvv39/fXVbLb7++65/f//+/77+P/wuee6///4+P/99+u4//n")
    append("otvn29ttknnGwvf/8/fn+9fnhp7X7+u++/v////7////+//////v++vv77L348J9787Xlv/j85L/377O/9/G5//377LCyrrn5+dx15r/99/jsufXpnkkvv/3")
    append("///z///76//z7+//+/v/////5+f349v/6+fO7/P7///39/////vz++vnun1N3/Pr///v++f/sm3nogQMPcrf9+PfvvP7uve+nsqS+7bv69Lr4+/7///vsvOG")
    append("/+v/2+Pv9//Ovve6urrzrtKmMXGipsr/8+/7+/Prlr7v6+vn///79/f/////+//////z///r88bO65Jtup777+PSrt/KvvvS68rbvtv/kvOy/87z+5JhtvPz")
    append("2++etteazqaKyv/r//v3//vz9/vz5/vz+/P/////6/vO1/fv9/fz//v///////////vj++/f4//z///3///z/+/Szs7GmkllTbbn+8bv79f3zv//ysbf51WC")
    append("+8rv+///+/fj7+ea/9/rqr7z7/v/4+/j69fKwr7zSaaqwrL7/+Pz6+P715r3xu//6+P75//7///3+///+//77//f4//CRUW6nhz/623Sur7OtsbXpnnOvtL7")
    append("otLv7//769Keccrj19eC48qOuvfK8//76/P//9/3/+/77//z/+P/9/////vz567z8///////9/P/+///++/bkoa26+/3tt//58rS0qpxur6Gvj1hTXG249K+")
    append("wvPj9/P/7/f/yk2a+9L///v/7/f79/vj8/+mvlnf6/fz/8r38//74+NVYXGafcb3/+v/7//7ssrv79v/2/P77/v7///3//v3///zzsLb//u+OXl9+6rr87rS")
    append("nuf3po5t0qJ91+vGlsbjzvfv8/O6abrf0rLX60m+//Pn7///6+//9/P3/+f3//P///v/+/////f/89v/3+/O/////+/j4///7/ffxuP3+///osrj6+/v1+/r")
    append("pt/GzoYQDC2u0tvOsu/G//v///v/wpLOwtf7//f38/f/6/v79/v/sk3v9/vX+9Lz9//X/8Zlzqp1PSjG/+vzouP/xvfz0uf////78/v///P/69Lz///375b/")
    append("vuO+3zGq8/vOwmGyno7KwsaieePrXZJJWaLzzt/K6+u+xrrv587rgkmi0tf37+v3///35//C//f//////+/7/+/v5/f/59L76+fn+/////vO5//747rj7/vK")
    append("/+v/887C687r5//rYX1VZY4oJFmW+9fngsLz///7//v7hlFFktf/9/f/++//+/vz+/v34+vz/+/z//v3tsbr95LrysLzogCO/9/fxq7v7//rooL/xvv7////")
    append("+//7wob3///z/+PvpuOyvpbn+/f/phQx0r7O5//vyuOqiop5xv/C3+fXzuue7/Pf97b3rsaC18r7//f3//fr6+ff//////////Pr8/fr7+uy7/fj16Lj//v3")
    append("svPz/8rj+9Lv97bv78bX3+vn68r/9/vz26aGlpJlXXma+9/nlu/38///6//355ZBxv/////K4+P///+649fz9+ey/+//5/v7wrrbyqrj79/7rhxtrvvbjtLr")
    append("/8LfFI7z7/f7//////f78773//////v37/vX6/P35+PKRY5FSZqq7+f7+/vSmt/rgv/Chufr+5aezt/j2/PKko7zzvP7////9+Pz/9f7/+f/////3/P38//z")
    append("/+/f+//v+///98rr9/v3+67z6+vbqm3Sinm+2/Pv//P/9//fxprSulEYYYaS++/3wrr3////+//73+u+1/v///ffvuPn6/O63+fG6+O63+f/+/Pzhu/zttLK")
    append("0rL/0tLOSdtJGLa2ebbGAH3f5/v///v/5/f757br8//779/z9/v79/fr0utBGONhWZ6i2///8/fr3/e6srrG48rzou/G+6bXws66hp59+57/////9/f////r")
    append("////+//bvuv3///76/v72+/n787f38K+5///+///79vK1+ea38bz8+fn4+/30vfvyqaOdTExXaZp18Lz68rO////7+vroueWvv//6/v31/Pf38aukoaa1+fb")
    append("++/jwt+Wgtf789LrovvG6/+ehnkAXWldKPOeMY7f6/P///v36///57L36///++fr8/Pn/+/r3+eaWbK2yuvC6/P/89vjxvfC41W6usb7uu/f877Ozu/Svp6S")
    append("+7L////////7///f+//O99fj///////n++f37/P38//j9//3////+///76ayotuq1/fz6/////vnzvf/79eGZZJ9cbZVqp7SspLC//v/6/+mvobC6///7//7")
    append("/+f78/POzrLb4/f/+/9hVWmeuu/z9//jxr7buv/G91V13645HPPK48KW5/PX//+a9/v/6//n//f/7/vX+/Pr+/vb/+PScdKKbfee4/vv++vXurq2roJNdc7z")
    append("38LvWcaquuvrroau3/v3//////////ff9/fX+7b36////+/b19v359fb9/v79//7+//z97rr48pxwrqSuufS6/vv///7//v377IoIIJhccLC/9f/68rK+/v3")
    append("7/tN/6KOwv////f/+/Pv+//z59//5+/37//Slm1VpsLneePn2/fr+/f717777+NtsvPO21UY5+vCtv9p8+P3nvfn/+v/9/+yzrLr+/fn35rnzt+Oms7n3+v3")
    append("++umhspdfXmqbZLv/8a6KJLnts66robX0u/3///79/vj8/fzuvvf4/v/9///49vSwsrv89vSxr776/P///vv59fz7+O+utKSxqqm8/v3////+/+WhlFtil2K")
    append("vtL37+Pj1/Pv6/vn2+N1xn2Kvv////v////3///75+v/6/P79///967b57ZVENf30tvb9/fbcdL797ay1+Pb8xQg69fbgv9d79+Wwuv////r9/+Kwq6+47bn")
    append("qp7GZevj5+OS8+/39+e2hraOaW3KdefS99LnaYKqXfvKloLv7+f/////+/vb9/fn1//b5///0v/3upa2vrbK7+vK4/fb3/v7///7++P3667n+/f39/v/9+P3")
    append("9//r/+c1HDFJdWm687737//zxs7r6/f/5/f/yrbX1//z////////9//3//v/////8/u+8/PXcdJRdcrj6/Pf7+vO5+PC/2Vpuv/j/xhZ22nr0v/G2+NF+//v")
    append("78rn9/Oy947r367KXdvSrsLjkuO6lsb349fXupJ9upLGvuPr/86q49vOps6afev77/P///v/8/P3/8b3+/P7////2/vn0rKq0uvf4//////j+/////////ff")
    append("89+uts7S/+P79/f/yuui+7qKnpaSRVXb+6qy+/Pz/+/b3+/O//f/2+umvv/7///3//v34/fv+//3///75/O29/Pf18Z9xt/r3//349f/867C/5am/9++68rj")
    append("usL/vv/79/9B///778rr7+/7103z28rGjrKSzo73xsbn29/z5/fv79uy3+Pf89e+77qK83n3Ta6y3/fz6/f7+/PX7/f318L/svv7+////+/Oyu99/8br4/v/")
    append("+//////////35/ff/6aursb76/vj9///485l43W+vtK6Vb7f/47S0srn//fO+//X9+f//9+e7/////v///vv7+/3+/fz///76/f/+9v3/+vbwtLbruvv/9v/")
    append("45Lv/6qe56bz/9L/xvfv4//7++e6++f/29Lv6///dVHfzuOSmmmOulGC+/P/8/fz2+/v5+f33+/S37Lr56pl+8b/0sK6+///3//3yufz7/vr2/fzcdvj9///")
    append("//Pnrn2O/9/z///z//////////vn//f/6/f38/f389/X///jztd9pt++go6iSdff587X3/f/7+ua////+/f//9Lnwu////v//+v359////f//9/r//Pnvu/v")
    append("6//zzuPnytf/8+//657fztLf4+/S+9v72//j//v/9+P3++//yuO+yv/fUba69+91ptv/sqa2787b6/e+59/r3/P/zuPj3+vCurLG3+++yr7G3//76/v7uvP/")
    append("ttLz9//rql3X//f///v3/9fn////+/v//+f/+/////v/9+v7+/v7///nnuv/+/Pjzv/367KWWd/zfYrrltKizqbr8//3///7///38/Pf1+f/////99/3zsrj")
    append("2+//4+PS2+/fyuO+nvf37+Pv8/P/39/O99emxufj89fK//PS4////+v/8/v7+//n65a2ddf3xvff++Oiqt/Kvp7Otsq+67aynoKKwv/nppZtyt/jynnXwu/7")
    append("ipJl+//7//v/////2+P369LjPS3///f3//v7//v3///77//f2+PK+///9/fXusrz/+/////z0rr797bj99/366bbmv//45phptemRa7S4+f7+9/73//30t/f")
    append("0pb3//f/+//75+Pn28736//r+//z8+e20uP3//P78/vf79Ljxvu658Kq39/rrufO9//39+v/5/f/+/fj26LKxuP//+ee1/v/9/umwqK6hsaSlml5LQhhwqaO")
    append("VZItZcbr38rGpufnspqW9/f37/v//7r/6//z5/fzSQTO//v7////+//z///7//tpZZbK8///sr455////+//5/fS9/PKyrrOvrrb+//C98bO/8bG26q6wp5R")
    append("7/f77+//4/fS+7brxpr38/fX5+f76+vzurL7387j1//zwu+++9Ln4/vK+/vf4+Pz78au28KG37rXlr6e377jdev37/fn8//j+9fb2+vS49fK8//b+7a6hnH3")
    append("grZ1ohS2TTFFSRx1oqpJLUHCecrnlqL/+87Cmu/r//v3/+P7zv/369ueki33////////8/P3//f7+/PvPX3Oxv//59ua6/v3xnHr29vOpueqOet5pteO1//r")
    append("xrK2xuv7knkk71F12//71/vr0sJ1+/fO27b/9//X477rwte+nuv738bGruvSmtfr5+vn1+v/7+PS59vS657O78rf8//S49L/16LCqsLb4+/S3+vK1+/X69Ki")
    append("49v///v3ysK2rp7vkqKmsqbO4+fbnk2244Z1UVnC29vrlq7rwsq+nt/f9//////j/8r745qS48L/+//////b++Oa+/P/+8r7eSjr+/fb175Ffe/ztiR5tq6i")
    append("ZXWyzqKi1+Pb34Lv67LCnsLvhsq6vnnf59L/ztvOnsau0uvS46r728K289/jqsrf3+fn77qakr7O847Oqp7Ovrbv68bbsnHG4+Pn77Lr5/fG39f/WeNp387S")
    append("ln2S39/r3/fn/+vf9+f/89v/633OvsLOlq7G14qa7/POqtvv33mufa7O687C+7Lfisrz++v/9//n0tKivtf/2/eusvf/////2/+erpLS//v/74q2vq6m8+vC")
    append("un0cObKuinHCbc7Sso7jul37sob/njX7TS2qgqK+ltvGOZKu2126krLzwt++ea7X5+Ouypaq886i0rLzdaKeyjmyeabXjpq6qqrC29Ln9+fzTQBd59/f56b7")
    append("ppZVtsbO46LbsmVBmkma49v/19vn/+f3/9vf357KosL32++CuqL738r73+eu98rC76JtEIr339L76+PXqn3btuPjyu/vws6y2+///+++tvP72/PKmr6u387/")
    append("////69vfsllt3+NpooaOXYqmRdeG223CUb6WeSC6rpp9ssLnFJ6SNRymxk2qWZ6GtlXS1+P/5+Pjkp6q3//X+8rnysKWztv/gpKa346m65brlr7O9/PSvr6O")
    append("57bCVUUt5+fGlu+ukrYcolWu/67368aKpqrO0uPjYePz99/z69/zyr6G5+fzzrqucVX3/66ObeOq+4Lb0vMgENfCqvfSnobSqoZttq7X1/f7///fwvP7++/O")
    append("//v32/fK47b7/+///+fz+/Pbmobzxsph146KfU1houey267CrsKWrqbO21kgwn2+GJo10l2urmFtxrq+2bP67Pre/+z/+tSz9tmA+tnZiaGSjr3ZxtnQ8b3G")
    append("+pL/9tSA+rPj9qpQvcvGe4BawqrZpcuqpd52oRy91KXC2fq9pVBtxsZ2pY6Owtn63v//+vqX1IT/WnF2XqHj2aF72exo51DQy9l2IXto+qVe50zZ2ZchidTC")
    append("0Pr/+t7///be8ezn//H6+sLQ9kz//+P6//b61K//7Prxjpe4dtltgD0OcVBHiarswsKE9v/GoZJaYzR2jolH/69xY2jUr4nU")
}.toString()

private val PHOTO_DARK_B64: String = StringBuilder(24576).apply {
    append("doCJi4yMgZKDe4GNkpGSjpGRhIyImZCGg492coZ6hJKOg3+CnJGFjJ2Pk5GLgI6OjI2dg5CCkoSHmIiOgpKTiZOEg5B7e4WAhYeEgo19joODdHaSe3+Igm6e")
    append("eY+QlXt9jn5peoJwdIh4eItpcnhxdnVrg3lwenN6dn+MbYuYmY+aiZKHl4qCn5p7h5WbfnCOpWFyfqCGbZB8goJ/jYZhaWRlkJKUmIiWioijlW97gKOOgpCF")
    append("iXh7i4eEjp+RlJWIjoWioZuakqSGkY+msJymo7GonaWbpKKYnKyXoZqaq62ilZSXqq+bkqGolKCQlaemlKqkjKSOl5SeoqGdraWvr6GqlJ6Zm6SQm56NmJGU")
    append("nY+akY6boKGdt4imj7GdpKGNlZmMfZqRk5uSiZeJknWMgY56lIuLk5CNfqGOmJmotZWasa6tu7mmu6iPqKqojIOwwIOirMCjf6+ilZafuKyNwHCJnZiyxLKj")
    append("rZjAxIyTnMK0rrSyoZKer4ahn7GpqLOakYiYpaGcjaOLmpqyt6WsoaOjpq2apq+asqOlpJyYubirlaCbqLacmIupj6eHlbalk5yNkqmUn6KVo66ov561tK+1")
    append("nZ2mmLCSpqCTpJebk5iYjZWUorecwbOWoKqToKimlp+PkJuZprCYoZaPlm+VfZiJmJidl5yUn6aIkKHFtqmUsKyiq7OgurOVsquxkZuvspenq8OmhLqroJir")
    append("wLyUy3mInJPAxr2np5+8w5qRm7a5ub27qJKrzYylnKuZvbOXl5Ceo6Oii6OlnJe8tq+ooListKCQtaWhsqKop5+OtLS9k42nqLmYrpGeoLmnnLKejp6UnKmZ")
    append("qqqPsraxtqGyuLGvsKCtoquVrKGhpKamo5ifmZSaqsCvuM6+npOcsrKkopKNopeZn66hnJSQnn2JjYaPoKaIlYuQmJWKkLzRv6uTpKe1o8GjwcOhrKafl6Cc")
    append("pI+1qbe5hqaroauxy7ya1JqDpqXIz8OisqLBt5qPirW+wb+9rZqr0aOtiq2ct7Cyk5Oiq6uxnKGpmaG7saKtqryqu6efvZ2jr6Chr5yRsLC7iY2frb+gppiY")
    append("oL27sqmcmaymn6mwtKGRsKyztKi4vLmtwaO3o6CPpKemoq+eo6avpaeftcOprNXDv5CnpaOVkpyZh52joLCYkZycpIaVl4OPoqGQkqOXioeNt8bJwLegraC8")
    append("s8WwxsisraSWlJ6StqmvmrW5mKqpoq+80rWe0rmEqavK09KguL/Fo6iUfq+5wce9s6au0buYgamlsK+0lZGsrq23qZWhm6y0wbS7qrS4tq6uxqKuqauhs56Z")
    append("rLSuiJOct72jrI6Wn7rCubifp6SppaS1qrGdrrK6t7PAvbu2vKSzqpSgoqKipaqpq6S0pq6Yqb2oqdDPy7Gro56OlaeXpKOuuLmxnpWjk3CMma+Lrpmfmbyw")
    append("nJenrsjMwcmtoKa+usi1xcS2tK2TjKmXybK0pqvBoKmcrrrCz8Ct1MKRkarI0dOXv8XCibWRgqy3vMXEtKKnyLyVk5uetbiqmJ2wtaqorZamqbbEu7Grs7HB")
    append("wqjAyaO2qLenu6GlsbWch5GivsCZrIWXmLjEvMS8q4mvtKm6n7ulra3Av7W/r764vqSypZSsrqmnqKuwqqzJtLOen7qvq8rU1NS5tqekmKGYrKSgsrOhpJ+z")
    append("kIKZtKWbuKSNj7W9o5/DocnPxsy1s6m3sc6pxsHFtaqbkKyY0LSprKPLnqKYvcbBzb2s0MmdjavNz9ORwMC+ic2MhbK5wsXElJacuMSvkZebr7aypaO1t7Gd")
    append("rpict7a8saKywqO2u6PHw6izrKi7u5mXtbiam5Gnw7WWqYScoL/DtMu8upWptZq+qMOqrrPAvLO9nb+2u6u0qKS4wLO4qqynp76/p62koMuws7/S1da+yriz")
    append("kp2Xnp+fr7SknZyujoqopKiOsLSflrfGvKHCqMrKxc/EvKmrqM+zxbvJsqKpj6uQv7mglJfLqqGWsMfIxa+tzNGXjbjQ0NeJyMO6gs6UjK++wc3GkpOos76y")
    append("j5KbpK6up6KztLOhrauburCtvKG6wqKvu6vDwLG2pam8u5mTu72hqpOjxLmWlpyWpb/HtMy9xa6ut527sre0o7y2vLG5osLAu6nCsK/Eu73BqLOltseysqag")
    append("p7+mtLjR19/C17LNuKmQpI6npLyjo6C7n4+rl56YsJKrj7DO0Ka+tMfDxM/Otri7qsq8wJq9v6WnhKKbrrmakZnUspeRrMvJvay+0drDj6HT0tadxsSmm8yv")
    append("haHEvtLMl5G0qb6/nJubp6fApampsrGttbGhvqWsv6m2uKGkurnAu7eooriuv6eWvcOrsZm1x8COhKSVqcDFwMeyxb68x6musaa/q8W4ta+mprLGuLvOtKvH")
    append("tcu1rqGxus6zt6Whrr6xuLjO1uDH0b3RyMaetKKjt76rmaavlpKzn7WknpyZlLLTzcO3xs/BydDZuci5vL+6tYK/v66jd5+pqrmniaLTqKKTr8rLvLDC0te6")
    append("pJ+51NiGwseegse4iafIw8/AlI60obrEn56apZ+/prCsuaa0oqy6pa2ztbK2uqqporrBvbChpLiuvqacwMOosJ26yLSemLKcrLzBucS0yMXLyLS2urHFpsGu")
    append("tbepsLi8t7rMs7K8vMuxtprLv8y0qp2juLm0uLjO0eHQzbvTxsbLm6eisMeknp6smKitnamvqqKOjqfNytC9yc7Lx8vcwsnMy8rIp3qjhayjkaqzn7CooKfZ")
    append("tK2fqr7Iv7PKztymv7ypxsaMw8qUesW9jqnKwM+2opyor6XKqqSerbO2pqextKyomaOvtr2vu7O6u66vmbfCv6mdmbirwaalwsGYwLS4wauhibmYqsLGuMe0")
    append("zcvUzL62v7G2orSwva3AsNTMubPOsbnDt7y1vJ7Dur67rI6gxbSxo7LLzufUwbjSzMTYqqmhprymp6u8oZC4pJyhq6yDmKDNzdXGw8rJxc/cxMTPx8/GpJ+f")
    append("m7Sdnaicna2qqqfcvLiXrLHNu6zOyNuqs767xL+Lyr6DhcvQiaXIwtGtlqKhm6jHwI+ztrarobayssGvsKemucOru6+3xLKynLrDxKqWpq23vrGkv8aXvLm7")
    append("vaWcj66rrMPGvcq2ysvWyr+5xrGxqLKqt6jCtdHFvbnTtrPNs7+3yKW+x7rCt5a5wrKykrLLy+nVw7bQxsTYx7m0uMGrpbGrpIa+o6aWpKuLm6vKyNjLucDK")
    append("xc3cxsfQws/RqLuprMCjl5aRmrCtsavbvrWXrLPQu7LLydGVrMSty7uLzL99ksvUlJjLwdOblqqVkKHHxp65sLGorLm2rriow7OeqrGkwKy2yLelssDDwLSb")
    append("r6+7uq+zwbyVrbvEtKenm7Whpr/Iv8i6x8vZz7W3xqHCpqm4wbXJw825tsTRxK/Msce+w5euxsO6vpy8vZq+jsDTzujPxK/HxsrWyLLEx8uxr76zqqevqbCZ")
    append("rq2HobXJwtbNxbXLx8zZycnRwdLYuby8ory2jJSita+iqqfcwK2nr7fQtKzGzMGfwMy/trOi0LuLnMrWpJzIwtWTpaSemqHAyaKyrq6gqbSxrLaqvLGtqqis")
    append("trKzyayts7q9yLWRrbC5ubm3wLaZuczMs6iekbyhpcjIt8u7yM/ZzLa3xKe1s7C/x7jIwcW1rNPLvLDRssi1t5enzr2wv6O8tZ7DqsTYzuXOx7nGz9DZzLXH")
    append("x8vPtLS1nqajuMOSsKqJoLbCvNXRy77FysvYysrMyNbZxqPQoq60h5Kgu7aUpKjexLifpcTUrbHQ0a2zt8LVxbGhzdCLss3cvprBx82hpKaSla6zw7Cmm6ay")
    append("urWpqsGmrq3AqLG6vq/AxrWptrGzz6mKq6K5wLu9v7Siv83OwrCgqLmtoNDMucq9yszXzMHCvq64vrfHvq/Kvcq9qc3Kxa3Sr8WvrqGm08CyrqrCr6HGoNTV")
    append("yeHM0LvQ0s/WzLnAyM7Kxa+zk6ypttOVnKmFlbTCvNbY0MDLzMnX0cnOzNja0qrXpqOuiIemqLeXp6vfx76xnsPUrLvR1pq7uLzHxaWVv9WLt9Xauaipuseh")
    append("rKCVjbC5sbKxoJmkuKu2sLiksJ63pbzAy629v7ehrKuxzKeOq6uzv8DExreexMnQwKqorLuto87QwM69w8zZzse5qanAuce/vq7BuMbDprzQx6TTtMO5p6Sa")
    append("0b+xsqnCwKnHpdXSyN3T1LTM0s3PyL66w8rMzsa2k6iztdqMo6mUkqjNv9TVzcXQ0MTV2dHPyNnf1LjSqpmwkoyelbKfkLDgy7y3mcfVqcjPxo6qvcXDy6KK")
    append("q9yJuNfcsq+3pq2exZuRn6aru7itm6OmuLG/uq+ho7C9qs7Dy7e9tLihqbW1xbSis6LAwb7Owq2cwcfRtqimqrO0u9DWwMu9v83bz8Wwt762tsXBwrjDv8W3")
    append("oMPSxKnUtMm/n6Ol1MG1tqS4yavBtMvS0dzW0qHK1cvMxr+0xsjHy9HBnZ+jwNiTnq2ekaXQv9PczsjXy8TV2tLQx9Xh1MXCr6OrnZyci6Wjjrzbz8LInsfW")
    append("udHSvJmnvMfLx6GasNuYtdfZq6/Erq+wu7CalqWp0MOxvq2yvbuswKSvsra5t8S/x7G1rrWisbequri7p6jGwr7NvbCZxMTSuqieqrezucnYxMvGwM7Zzsiw")
    append("wr25urzCyrbFycSzpbfXxavUqsm8nKWnz765saWs0Ki+ssXZ0+PWzK210c3Rv8e0zsfIzNPOrJWZtdWbmq6ihqfIudDd0cbI0MzW2dDXxtDh08+tsqugmJ6W")
    append("g6SchKbZ0cbFpMjasdbSs5y1vcPKvqWrut+oqNTToq3CtqS1sbKgjquixsKexbCrwsW3xKy6wLyyvMK6zrGzwKyur66xusvBsL3Mv8nIu6ycx8bKubCfqri1")
    append("wMfYx83MwtHXzsu1ur63xr62ua/Gxb67qLLVx6fHqcm/oKCdz8K+qKGw1KrFsszd1eHVxazAydfTvcCuxcTLzNTSvrScutSkobGclLHEttLh08TJ1dHV1tHd")
    append("xM/i19Kfn7aekqqanZ+jc5fY1MiynMrTudriq5SwvcLJu4+vytWpqdfYrL2uvJy1tsiYobC6pbuzxcW3xsK7vMK2xcKxuMW90bu9yaytsra6wry+wMDLu7rK")
    append("t62wyMbKs7iloLG6xsjYy8zJx9LX0cqxwLu7yLy3urDDxMG6qbDIyq/Gq8i8lp2h08bAsq2t17W9ucvb2d/Uw7/IzdLUrLyqtsTLx9LXx7OXrtiTqa2ao7XD")
    append("uNXk1rzN1s3T2tPayM3i2dOwkK6nmq+WnpqkdKLd1NKmndHPr9XInqWivbPGxo+lwseiqNPQq8OptKevwMmimbfJu7CxwtPBwcC0w7+3wbO7t7vJzsDFu6+1")
    append("qLzBxMPGxcTFuL/GvrWuxsTEsrm2r8PBx8nYx8/MwdDa073AwsDDw76+sLLDwsStqqvJz77Oq8y3l62w0ce6wKaw4L2zxsvU2tzXz8HNxcXXtcW2tcbAx8/T")
    append("2MObpdSWoaOdpbXFv9Pj072+0M7O2tTXzNDk3tirgamqkrWSkJKue6PQ4durpc/VvtK9m8SxwrfU0ZeexMOXt627pLO3m7ayvcuns7G5yrevo9TCwci0xLS4")
    append("u6zFu7rKz7q9tsusp6/Cxb3FxsK8w8bQs7ynvsfBuLG5v8q+xMvXytHPy8/W1LPDw7bJu8m8wK/Fv72yravM1MjMsL6zm6i6yMnIt66z0siz0MPO3NzY1cm9")
    append("tMDUzM2ytM2+ws/V3ceapc+ZnZ2RkLnEw9Lf1MS3xNfR39jYzNfo5NaxeaqelrWof5a9g5u64cquq9XSvtW4qbe5rrvHz5q2t86ruqKrparKkaSqytCfta2+")
    append("wrGtusHCw8S9w6q5vqrLvMG9yMa9ucCvpqvH0ry+x7bDwM3HrbCsv8q8tKa1xsnEv8fX0NbVytLV2LLKwMLGwMfAvq/Ft8GzsLPL2srHs7e5maG7w8LWssOy")
    append("yNHGzsjX3eLZxtXDusjRz8GdpdHCwszW4c2isLiYoaiIl7jBwtLe1cS9ytnT29raz9ro5s2qfraioK+VhJ62k5PO3cOordHYv8mWxa+2qMS/x42/stSgu7uz")
    append("qrjMmYCj0tKPsbK+w7WZwsPIxbzAuK2wvrG5vMnBysq0tr+8qbPM08jN1ci9trrDsbCryMq3sZ25wb/Cts7Z09TQyNDY27LQwre+ub3KvbvBt8K1tarI3M7D")
    append("s7nBl57Fy7vUtsXBvcW908jb2uHcx9PHv8XSw8GqotPIwM3W38eos8ejn6h9l7vKxtbg08G40d3W1t7c0tXl5M6cjbedrbGQhKCsfpS926OnqdTWxbWjvKmw")
    append("qMW0yZ++tNSbw8HApb69nYyd0M+Vrr7NqaCXq9DTxci+uqi4wLGst8TJ1Meut7+yqsTLyMrHzLO+vb7FsrO7xMiwuKe5v8bDuMrb1NPLxc/Y3a3LwsHFsLnC")
    append("wLfBvsGrt6bQ2M/OvbvAkZ3Ix7zOtry8wMLLx87X2N7dv9PEu8jYyMaqpL3Gx8/V3Miwl9Oen6mCjLLOzNjkzcS6zNvT2+Df1tfl4smZlbebtauSj6Gig5+/")
    append("4b2nqtbZxcGmtbSypbLBvZK6l8uTubq1srWlq6OW1Luap8PEopSjqtDOycitwq61wK2xtcDCx8K6wriurMPMwcq1xq25zMO1sbXAw8i1urW4u73Ku8PZ1tHM")
    append("xM/Y0LHHyMe8ucDAwb3Eu8m6o7LJzdTQxbutj5zSvLXMyba5xcDDx9PU3tzZudrMvsnSzsW8o8LO0NTV3s25oNWfoaR9n6zPzs/jy762w9rR2N3g1drm5MGl")
    append("mKmyoqqVjZSmlo6v3MShtNfYuKe4p7Sms7bLxY+/ks6WtaTGvaCao6CqwL+dr7amo5iquMXPzMSdvbSyqbKytrvFw72/wcC6xMrVu728xsG3yMKsvLK2w8+4")
    append("u8XAs7/Evcja1dLTz8zUyLTAxbbCu8K+u8TAtMS2mLPHxd7Syr2uk5XMrLnTyLm8zsG1xtXa4drWsdrMrtbU2Ma4vMbSz9LV4M+vq9K7pqKIq6rM0MPiysOy")
    append("wtXV2t3i3dnl6LenlJvAlJ2cmomco5Sz4MWmsMvIp5y0l7Wxtbq8tZHVptmgwLXGvbGhqJatwLmYraiZpK6hrLXLzb6qsbq3pqDBxLrCvcu7uLy4v8zbwLO8")
    append("vMjAwrqvtrTGwcm0sLy+v8jEv8nY2NHRy8vWu5/GzbHNu8C7yMi+tbu0nbbNwdrXzKuvk53Aq7/Wy7TB0Lq3xtbh4d7QrdnLuczc2M+4u8TXxsvU4dC5oNzP")
    append("rJmPq7fI0rvj08q9xdLY29zj4dnl5bCmpLO5nZ+np5uPpY+q2b2iqsm1mZSgwKyyvLzDr6DQrsqdxrOwy56pqZevuq2qm7ihpKScrbfC0LiyvsTErKqysr3A")
    append("stS4tsSyvc/TyLq2vMW+xMG5r7W9xbm6r8e8w8fGv8bW2tXIx83ZuqXJxrnCur64xsLCv7mwrbrMrdvUxq+0o7C1tLTR0bfEzL+7u9fh4ODCttbVub3f18+3")
    append("u8HMwsjW39W5n9XguHiSt6zJ0sbh08DCytbc1dvh3dvl5LOnt7qyspeWoI6VrqW00bCwpcuwla6TtqyqtbbKuJzStKyUub+xz6uzopysqq+6tLeTpqWqu63P")
    append("vbu2vLu0v6u9rqu/utG7tMCzw8jFtceztr26vbTItrS0wr22tcevvsG8v8DX3NjLzMvVvLfKw7nCu7y9xsPDx8C6t7fDttjKt6mpn7GpurfQ0bfGzL67w9rY")
    append("2eDBvdPKur/h2svAtL3HwcbS3ta+ndXbs4mWt6vG0sPi1sXM0tXb0tvd497k4rqat7W1rZ6jmIugpLq1w52vrsOho6ysqaahqby6up/QyZmGusStuamquKa9")
    append("v6q0speVrKC0tKu+tKm1srGppqa3vbbMsbq7vMi8usm6u8fMyre3tMC4wrmyxb20ubq7vb/Lv8nS3NPSztPMvL3Iwb++tMDEycjFxby/vra3ut/Ataqjnrqj")
    append("s7jS0LbEy7+wv9TW0trAs9m/w7bU2cm+ybbM3cfU292/n9vbuJiNubDG08Ph1rzO1M3b0Nvb5d/k472evLizraHDk5Seoae0sKmvqrOmprfHuJitwrW+pKjH")
    append("pZicu7ifnqqgr76xr7iuqZaUsI6wqau6u7G7rqq+srC/wrfNv8u/wLa+usC9vrrGyNbCt6i+w7K8vLyywb+/v8HLvMbP29TW0dPBtr7Jxsiztca60tHBvLnD")
    append("uri1uNfAsq6ro72suLfPzb+1wrm5udDU0tW/sdO5tbzX387CzLrX1MDU3OK0tNvcwJCQrbXJ08jgz7nS0tHc0dXb6N/k5LilvLSyrZ+0n66Zm5ioqbCprq+p")
    append("q7nFuam7t7exrrmkqp6stbe2ubewtaaqqbK7uKiloYyqn6TAuam70bnQtLXLxK3Hw8i/wLu7w8GwubW/ydi2t6iwwrW2tbu9ysXDuMPHwMnS4NrSz8eztbzE")
    append("ycGstMm7y9O/ubbAt7+suNPAuK6qnryttL/NzL24yru9uM3Z09fAsta4wsjX1c7BvrLF0MDT3uOyq9vgv5Sim7XD2MXe0L/G0dLf0dbc5ODk4bKcvbiruKuq")
    append("wJyciJ6dnLKrqKu6psSfu7iwuKuwr7uls6O9vrS3ysK0uKynn6WruLSwq6qpqaO9tqW1z767srnF2sHIubSwwLrQ2cm9tbC2ucC7tK+psr22tsrGzcHGv8W/")
    append("08bQ4NfbwMa3xLS+y7q4tsS3x9LAt7zLwb20rrnKsbmunri/uL/R0MDCwsG+t8fe1dG4tdTIvs7a2NS4tLKwvr3W3+a9pdTdxpmkn7C+2MPbzsnC0dPf19bc")
    append("4erm4K+Zu7Oms7S/yoeZgaapoqSrqr+wqrGSpq+pqba5xJmno6W8w7u/y7m2w6iljXqPpLW4ur7Bw62zsLe4w72/s7+/zcS9sLWyucHQx8a8wLmzvbTStLO5")
    append("rbyyw8rJ2s/Jw83G1MXR4NXXsL22x6/IwazBusG2zNG7wbPDz6y4uLvEq7C1m7bGvLvQzL28wr3Assjh086ss8/Rvr/c1dbJtLizt87Y4t67mNPcuaqhnqzK")
    append("2cDaycS209jW3dbd4+bj3bWZxqmivLXGrpuSnJ2uo6KgqLa0p7qcrKywr7K0sZiim6OwvrjBvMLArKKgoIKTqKm8s8G1tbS2ucC9z73AuLnXwMK+q7a2usK4")
    append("us6wwM21wbbLv8DCray1zs3N1dTRzMXHzs/R3tzKubGpvr7NprzBxbm/zta/xLi5za67tLq8raynlrbHuMDMy8C1u7m4rsLZ0dW0uMnLurzg0+LZt7bDx9DW")
    append("49O8os7YrbGZoarM1snZyLyuyNfR3Nfd4uPk27Wk0KuovLW0ssKjr6qtpaCzn7uturWooqClrbmljKWjqq+pr8O1sbawtbGLj5qrtpWurbi6srS1wLvCwcrD")
    append("zLvWvNe/vLW5vtW1uMSzv763tMK7vMDXurSyz8LP0M7Sy8jMzMzN0uK/yLqtz9Csp8/Eu7y8zNTAuLzCwrG+x7yvtqWeqKvDssTJxbmvsrGuuMPV08u6vL+9")
    append("wbDXztzhurK3ttTU27/IrMbYvKqkpKjD1NPSz7u2ztTT2NXb4uPi362jz6+xsrWpxLS1tKOrxK2po7atta65rK6srbKZf5+crauTsLm2v8a+u6WYoJeoq6Gm")
    append("pa+wtLKsu77AvMi8vr64vNS/2qSwwtPCysO0rbTDuc67sbTR1sevr83G0cPJzcLNu8nM0tXBx660ycSpvc+9sMC90NLCv7yxwba4x7mnuqmwq6m1r77LxrWu")
    append("s6ezucrT2sfAuLfAuL7A0tHUvaS1wsnV2LzAp8vTr66iobG709XTzq2zy9DZ2trZ39/e3rOk17uisq+yuaXDsKe0sKyjr6Oxrby6xrK3r6GSoqKet4aft7y6")
    append("ra27taWhnpOtpZ6ei5OosLS0vrm4rrOts6+xtL+zzbKtxLvB0Le3scnEwr+8yrvO0si3tLrAzszAw7/GvcG+yNDTwq60vbXJzs2mwcav1sWyz7mzubezwbCw")
    append("o8K4rq2urL7GvrOrwbS3vcPG1MzAu7nCw8m538zcuaS/w8nf1LXArrm5tsCrpbqz0c7S1a7AxcPZ2OHW4N7h3q6eyr6wqa65s67DsrG9s6OspaK1qKmSuMHA")
    append("rJepqaulpH2lt7K3srO7taCfpYyprZOlaIG9tqvAra2xvcKouLy6travsbixxLizurixwcO5wLS1yabLybzRs7vEzc6+ucvPyrSwwM3bxKuzxdPLz8O5wsC0")
    append("17y/tru4tba4rLOvqMe/rqq/rrmyuqervbnCuLHFxdLDw7WyxcG23cHauqW6vc3Z0re/v7zGvLCsqruz1cjN0LO9w8rJ29/W4Ong25mexbauqLK3q6vCqq25")
    append("qaS0qKeppqa+tr6PpKK1pr2pnpuwxb+4ur2ywKCkqJW3ppOBhobNsLejubq7vsi+xNnWurGuwLOvvse1srKpwr20u7ipv6Srx7m+sLnSyr/EvdHaxLW808XF")
    append("z6OyztrKxMfAtLW1ysHKra23wMewrLarqrXJsK3EusKtprqzu8K5t7re1NW+vKy6u8W328fWvbzBvMXV0L62zL2qvL2sra+zycTQx7eyxczN3N7g4ebezJ2k")
    append("vrSmo6y6qamxsrjNpr+4oa+ysq2+r7SXra+ktbmmnKq2ubG7tbWutLCvtLWnm4SInoytwsuzt8O4uL7LydPWvcaww6q1vtLJrrHI2cmxwb+our3Dz7+ztL3C")
    append("wb+4vcrLybvGxsjLxJyu0NW2xcK+qL25xbWwt7jBxLS0uKWkub63vai8qcK6qqq2sMPAvcHRxNi+uq6uub7A0tvJyLy4v8zQy7Kw0Lutsb6urLS9tsbFy7C+")
    append("ysbV3dzs4tzcvKCrtKujrqeyvLqpo7O5qLW2r7ebq62nsqarybqsoK2Zmqixs8O/t767vKXAoa6rooSSdZimu8jFptC5v7m6u77PwMC5wLe5v8HHwLTEy8u2")
    append("wcO6tMDCvdmztre9tcLBvr3Rxs7KtsLSxK2qyce9wLS6xMvDvLCrtcDVzrirp6WtvLavurS3tqu/x6u5zsK+t8DVy827s6Swub/MvtLD3LKx0L/Purayw8HL")
    append("u8G0pLnCvL7Qx7e1t8rX4OHl5dnFqp2hpZ+htq66sK2uqauwtaOfpKqmrLiwvK+0xqOnpKeklqutsbm8vbK+uMPDr7OqnpV3lpeewsLJuru+t7K2zbO/vLiy")
    append("urDDusG91LjDucO6vLG2sbXGs9bCr7DJuLrKxrPdz9bCvcnIysGkxMS1wLTC1MfArbexuMbEubyooa+zsbGpqsC1xba2rbG3w7m4ubDCyN3JtbTHt7nCtsjC")
    append("5s660ci9ybvIz8PGt6ygq73GuLXSxsWrutHI4uDm6NbDrJ6imJeyrrGvr6qovLWyvZ6qrKi4qK6qqae6vp+en5u3lLKmzbOsvbi4s66zs6ikm36CiqWz0bq+")
    append("zbSwtr+02sK2t6q4w7G8wb/E27i/urK2qKapsLq9xMS0r7nIvbvF0MS/183Cr73Wz72nzLe9w8vEuMrIubC0u7+yuaSepaywtLKotLa4t7a0v7C3tK/Esc3A")
    append("tLrKxL64vLa5vsW12M7CyMi1zay4u8m5r6e7osG5uay/0MO6x73D4+Pj3768rauonaWsrLqwu7mzyMaxwKegqrWzt57AkLeyoKmev6Wfmqu8wa+5rbOtoaXI")
    append("vbCspH5niajAw7y7v7aruMazzM3FvLC3ra+3w762zc2/ta2ppqmssMm2t77Gur3Uy8S1vsS2y8yzv77HyqevyrzFuL2zssSuuKqvuri5sZGktau9tLGrtrev")
    append("ubG52LWzuLa3sbe6tbe1vbWpuMGtybqs1czM1L64v7bMs9CyvKisvbWpvKqpwci/w8bQ3+Xg09C2vKOtna2ttL+wvbjNxL2tr62msK62saLBsayvpaChuayo")
    append("nqi0ubCit7S7oa+8ur2vqYZpkZ+vtcO1rsa0v766xMbBuLeprLK3vbu1tcfItLq0r77EtLnFucLKt8LE09rGwMa0ysqvvdHDwL7Bt8nUxMK7r6y0sr680L60")
    append("o6Kepbm3t6uyuLS7sLK1s6iytbm2wb+6wMO+uLXHtbawtr+6zNnGxcO8xLbKy8Kiwaa406mzt7umvrrGxNbT2+PhyM+mxaCopLeht7HKs7PMucG5sKqzsL+1")
    append("prS7vKiirK+nnq+woZq5t7OuwsDBxKy1tLu4ro9qpq2tsrKxsdnG1cXQzMW+tbKhqLa/vbKvsq6vp7u/sLC+vMXAw8S3pMXFxdjKwc7A2L6yw8/WxtHEu7LA")
    append("sNDLsKW7rbXJu72yr6WhqqOutbaxuLazsbSsq6u0xLKzwMfI0LesurvNs62wtcGyyOHSzcDDu8HQvcm4xq3Curexs724wdHTw8rX3OLht7WitqauqbqivLe8")
    append("s7q9vMKotqSwtK+3q67UyaqwpJ6orKSjpqSuraixure5uLjCuauvsJOJnLS3sr2+wc+7yMXRzL25pqmlmr26wMG3tLG5p6a3ubS1ysDGx7+6pLrFwdLaxre/")
    append("tMCuytvLysSzrLbDub3Cram1t7G9rrizq6q1q7OwsrKpubiswrqxsLGvs7O0tsG9wbe4xMWwqLm2vrazxd3fz8HKu9KxtsLAur28uL6ytJytuMa41c3b3ODa")
    append("xLessKiys7asubG1vLysz7y1tL2rwa2rvqzX1aavrZOko6epsLCspqyhm7O/t6y4tK25tpuVprSxtbqxsraytcLIyL2qncSjoq28wry6wa+ut6u4tcm/s73E")
    append("u7qsvbG7uLrF08HAvK6y0NnayLK6sc3QwrzTurXUyLm+uK+lp6yxtryzrq2ptbK1sK+srrSwx7Owt7+2v8zBzsy0va66s7KtxcXIx7/Vwby6vbW7u7O3uNO/")
    append("sa24wc2v1s3f1+HZ0MKrq6+1t62ysbOytc/AxsHCqa+2urqcuKzK3Z2woqGmn6qoq6yvnKi0rrK8tba3qLaorJ2ep7KstLazo6+xrcLJwr2qoLecpLbEwbu1")
    append("xbmztsSxtb3CusGyzMO1uLK3w8G/ucXCw7W42NrcxsKxuNXCxLbgv6+8wLXBtKeysrCwvL+yq7CmsbGms7Oxs7Sw07y2vbSnqLC0sL+3vru3sK+sw7u1ycTG")
    append("ybrF0b3Lv6bEuMG6rKDLvszH29fT2N/Qy6arqrq5trGur7i1srrAwcLEqKWjx7eop7vE2KyZpaicp6KfrKinnrC0t764trm7s7Syr6ehqK2zubWorqqsqsC7")
    append("usm2q6iio7bJt8C80rKvttm1sK22t8S/vcextre+zce6tdPCyLzA29vap7u4wMXHwbnLuqyrvrWxp6K3s7qvrrC0sbSmqriytba2tq+wsLW7xra1uLW7uLC9")
    append("tbKyv7q4uMjNx9POrL67wLq/xLK+tsOyuLC2r8TK4N/aydHJtpeorbi5uby1w7XAxMHFw9W3nKGcwau1uMbEwqChnpidpaytrZ+aqbCztL/Dt7e0tamztaqt")
    append("taG5tbWusqmys7i6vcuyr52vp7LBvMW+2byttMKtsK25xLzEwcCuubS+zMrGwsK3xdHF2dvTuKvGx8C5wrq6rbKtqbqmp7SztbmqrK2srLOorLK0s7Gvra2w")
    append("vLu0wLqywK65ta20sbS4x7m0sMXGtMHBttC+ucC+xLW5tcG5wa+np8PB3d3XyNjFu56xp7e1wbi+w7e7u7+ytr66sKSkqaWzu8a+v52hm6yxtbipq6KgprTA")
    append("t7CurqKlsrSpq7G3vKq/q62soqixtqzDvbStupunr7Kvvca00b6stqqotbfIvs3Ctruxyb6/t8PDyMK4sdfQzdvRuLDDusq0xsu4tLq2uLiqoavPqrOwt7a4")
    append("t7Oss7C5uK6srK6ttc21qrmzzLK2wbOztrmzur+zsLe6t8m0u8HCyLnNu7CzwrCzvKCyrN3Q1tvM1s/JrrWosLa0s7q8uK64uLq8s76wqa+gqay2x6+tnaGd")
    append("m7K8ubOooKSvrbPKxrq2sb2xr6iso524wbqvsLC0qauuurG+wLuyp6Oosrevw7uxt6ynqqqqsM/Nvry+t7evx761tcS6ssHFs9XHy9zWwbasss+zvsS8v8e1")
    append("vLKoqKPZvauwvrq4tK+urrG1uqyvrrC0qLO8qLiy0L6qvrG7v7uwuraqs7rLvsm4u7q9wLzEuLm1vLOzqJ2kxdXZwt3Y1bfAnrOgs7a6vrrAsMC5p7C8vs2u")
    append("nrKusbC1y7W0mJ+bsbi1t6ehqqu0tLayv7mxs7O8sKaprai8w7uztbq6ubu3ta6streqnqupurbAub27raeso6awv723ubiys6DWx7i8ua26wsTDu9rSxdjh")
    append("z7W0tbe2yLmtssG1uKyqravUvbW1urmxrK+sqK6wrKaqq66wq5/IsrW/t7ewuL3Au7S6uLu5r73Ewba+uKvNyMjPurS6s8W+o56w2NLayNrf0Lyhlraps7u+")
    append("wL3Ft72xqq63t6yxnbelpa+xv6WjnZ6zubS1paGzwbDDvKyxtZ2to6ykr6ywqqerwceysbe1ubi5t7O0sLWhqKurrru6ubq5payvpae4vry0wbSyqrTc0r/L")
    append("w7rMysPIvMrbxdXH2LPBwbLLxL+2rbC4uLuxtbK1tbuztKy1rrOvrLewsK6rp6mut7KxvrW+tbG2r8G9vsC7urG+t7nGxsixt768w8rKv7O6v76qpKHF1tPc")
    append("2dndzaupo66vt7i/u7q1sbi2wLixuaCrpqinpaOtsqSfmqy4tLuvpay+tL+2qqGypZi8u6unrLexr7G1tLWtuK6lv7uytLKqpammq7Kvrri7wbW6r7S0pbW0")
    append("vK+zrsPBt76+w8nKvsXE0tTAwtLQ0cStxrrevavMyLu3qbSxrb6zvrCssLmwqry7srSkp6+rrquip6uvw8S5t7zAtqm6u7q9x7+7tLy9t7291cC1t8rDzr/E")
    append("yr2vs6+qqbrP1NXb4N7bs6CopKK4vL65vrDCore7urm3vKmksbanqa+6pZibq7Oysq+qqbyytbOqsamkkpy9r7qusLGrrrSwrq+nv66rqa6oqqu0sqqxtKuy")
    append("s7i4uLuysKmpqK+3xr64t8O5ssC+zMq8zr3IxsuxytDXy6mkqcbbxbPFwrq6qbK1sa27vrGjrriwqryxt7GxrrK2raGkrb3Ex8DEyLe5rLW9tr2ztcHBwba9")
    append("vbi+wcHBx9HBt869q8alwqWwn77T1tjc3tvbupynobfDuLyysrHCtLq2tcTCtaWlsbezsrKqpZ2nrq2rr6OirrK2t6urqJ6jnqanuKSwrqyqssnHsq2qqLCy")
    append("r6uktK6/wr/Bua23tLO2rbewu62rqai10sfOvLXDwbu7xry/y7/Jy8C60d7QxqaqsdjEz8HCuse0sbCoubu1tbaqs7q2uLO/rq6wsLGzp6+prrq8tLS/wMq8")
    append("t7zItMS6t7W2tLq0uLO+xq3Fw62wsbDMt7e9qqqzq8jU2Njc39rSrKisp766vL23ur3Bw7O4rLK8qaajsLnBvK2qnKajr6+jpqCwqa6zrberq5qepKewo62v")
    append("sq2/wa7Gv8zIuaq0rrCvwcDKyMW6ury5vLOrsLatuLCopba8vcq8vr+zsre8s8i4vL68xNLK3NXMu6y4ucO1q7ezsbiiur+zxLy1sq60utCnr7eusLGnra2s")
    append("t62jsrO5ur+8qszJwsDJsq65tLy+r7+rrri+xMW8sL/BrLvTza26qLGautXS19nb3dvFprSlvbS3wLq3ucjCrrqpprCqp6Wttby6trK1o6+7p6yeoK22oq+3")
    append("sbOrp7azr6yowMGrpq2xtrzMw73EyLO3tKm2vrPCxsS5wLu0s6ysuL+/sLKprra5v8DQv8S7trmxqMDAvsi/vsnJxNfQuqq7u7eypbLDr8O1wMy1ubOyt7C3")
    append("veGvoru2s6yprbyytbCuurbCxby1ub/T1NG4qbHCwcK7s7Out7O8wsWwttPDtrS6v7qqrK+jutHS1tnc29W6qa2itrixtry6x8K6prm3rqKoq6a0sL28u7qa")
    append("u7+woaOqq7Owq8izuLm4tbOwub2+t6alpbCwwLS7u7mwu7Ovr6m7trvCxcvIub/EsrOywcrBw7G4qa26tbLCxLq+taq5trO7scK/w9bVxNzQsKyvxra4sbLH")
    append("stDDuMamu6y1yruxucS+q8C8s7Wmt7KqsKqyu7m81MO+srm/ysO2qK65ubyztrrBuL62tLa2w72ztKy/0c+mqKO4t9Db2dva28Wtq6Wqrraxvsi9xcG3u7uq")
    append("p6+zt6+orbW6vbOxwLusta+vsLWwpLKzwK6xs7C4tKuntq2tr7i0ub2/w7S5s72mvrTCsai4xMrOu8XFr7q/v8PFyMbCp6zAvre8tsyyrLS2xLu4tbe/vb7O")
    append("yeDKs6+4vMWzs7O8trvFq7GmqaW3y7m5y8/Kvb+2taWtsK2ps7a0t7a9x8fCvcnKt8a3qrC8x76vurW0s7e5u7rMwrmyvrO61dGys6CwwdHb3t7c06quq7Wt")
    append("qrW0vsO8uLOnuLK2u7qztq6upbHCvLS5u6+xsLe9sq+nr6+2u62yrLSzraq1ramnq62vtLW4t7Stu72yvLeqo6WzvMPIwMrTv8TKxMfRwcbEr7XBy7K8v6m9")
    append("v7+6wsfCt7e6scDN0tnLsLTHttu7s7rCvLjBnJ+drLC2tLa2v8fMy7O1q6WuuK22t66yuba/ub7AwsLBv7qvprC8vbm7vKu8uLq4xM3Hw723va233ci8sq6a")
    append("wtTb3d3ewKGvqLWzqau3sbS3wK64tbC4v7Wtraymp6a0t7yro6i3vby8sqmvsL+9r7CvqaiprKOurKqmr6+3uru1sbvBv7m7rKukrbO5vMzRxb/FwbW1uMrL")
    append("x8jJs7rCyr23ybm3xcO+vcC7tbq7sL7S1r3ArL68tsm+tbu/w8K+qqmpvLexu8Kssry8wL2uoKm8sL2+u6m8r7zGwsDDvce7tKijpLjGtL7Dt7W0tLnEv8Gz")
    append("ubrBrLaxz8PFsbOZydLZ3NrTpqy0q6+ps7q8vMTBwKe3w666r7WlrbCoq6ururCvp67AuLWlr622sK+ytbCwpaupqby1r72uqKesqrSpp7y+xru5s6Oirqqx")
    append("usPCycCyp6ektLC5xMfHwLS+xMXIt8C9urS6tLOzrbOttLe/28rEssi3vsO/tb+6wMHJoaqwt6+1wbqvrrG9u7yop7Smrrm2q6m6qbnJwauxts/Cu7GXqreh")
    append("u7m+try+tbnAvr/Bwsm5tKbCyMHCubqevcvb3NzJqqylp7O1t7e8vcXKtKeivK6upaOxuqSoqrOxr6+xn7m4uqmrr7mzuaa0qrazpqios7OyvMO7tLCxsK2s")
    append("t7vByLzDr5+prq20sLi3xbKpqaihr6eov8XBx7G+yMHSvr64xLG/sKy4rbevtbvC19HRu8O3xr+3ubezwLe/qqe+sLu7t7G1r7TAubKfo6qdqLOto7C6sri7")
    append("vLezs7i2uLCtwMenpqywtLi4tLjAtr3ExMW7t6vJtMe+xLqwvdfg3dCxpp6gra66sb+yube4s7GdqrSqqLG7t6yrqKywrrO6tcmxsbGysKqws7HCq66no6y2")
    append("z8fHrq6ur7GsqKqmr7q7wszGoaexra6zqbO2urPDvrGyramnrbXCwr27zLvAw8e2vL29srG7ury/ucLKzsbEu7y3xLm9ua6subS9pa66vb6tucKurbOxrqWi")
    append("o6urqK+ms7C6s7W2r6ysrbW9wbe2ubq/t7SooLCnu7u9v8zOwLS8t766trvMzrqvztHg3smoq6WiurvAvrO1rLqvqKGfrrWhs7a6rKWfn6+ppLGrrqyzvbi3")
    append("rrOstrazraWsqcK9vLOtqqipp6qvsqytvrO7vMzGr6+vr6uzr7a8t7m3xcXLvbOwpqWxxcPFua+0wsDAurnIvsDBuMHLwMbJyMzJua67u7CysL24v7myp8TB")
    append("xLG3vbSjqKmko6epqqqmqKqmsravtbi7tpa1t8i2ta+/2MO3y7auq6y0vci/wsG9tsHHvcK8scDDyL7Gt8re3rqaoK2zr7y4urSysKynp6Glua2yrbGmn5um")
    append("rq2otLO1vbK5t7Gwpam1sbSut7G1uMDDvLixoa6ytKO1rqmwsrm+v8nKsbC0sba6uLK4uba6urm8s7C1rK6rrMbKuqqzsL/Cv8O+u8O+wsbDx8zNwMjHwK+3")
    append("tLywrrzDvb6lrK60wsG9raSpqqqvpqCpr6SjraWwsLS3v7vGv669sLutu77A17m6vKeouKywvcLLuq21t7e/t76ypbGyxMrCvM/dyrGnqpm4uby+t7Ozu7ys")
    append("qa2rraSkn66vopq0uLi1wrW1rK+xrqevt7Gvrrq5sLDOxLy9r7iruL63rMDGuKqtt8G6v7rCsbW3tLS2ubq5ur7Etbe0sLSqqKyytL/Kw6yYsLOouMLHvbnF")
    append("0cG/v8TEvsa1ta65t6S5trq6yMCdsbC8uLyxmaavp6qopp6mqqWtq6q2uai/xMbFvqW0s6utwL+2u8TAp7DAvqijr8bCvMXAsKa4s72prbW8ysK8wM7Vwpyt")
    append("pa6surWyt7Cuwb6vt6muqqyeprKyn6O5s7zFv7u9sKynqaynq7u2tLO0vbbLuMKuqaOzucO1vsW6sbCrsa+1uLi5ube1uLqzuL28t8e5sLm5t66epK2tvMG/")
    append("vbO0qLiwtqe1w8DMzcO9v8q+0dDQqbekubCvvrq+t6+msLa9raKup62qsrurn6mqo7Cvr7KvubrAu77Nu5Ksp7i+zLO7wba9srG6vKusw7XKsMa8sqKwqa6v")
    append("sqm8w8LIvsXLr6apoqq3uqu1tbWtwLKvsKWqqqatsbDIv7W9u7bBvb6zq6StoqqpoK23tLe4t6q1paq1rKayr7S4v7S3trOwu7aztMW1trq4sbayt7O9wMzF")
    append("sr+2qqafpqm2vMG/ucK+xMWysqypt7rCtcLBvLu30M/Ks72rqMOvvb69tMGtucGuqrKttbW5tbSspauepqiuvLWstbfIxc3CsZG2usa3ys+9xb28w7evv7us")
    append("v7q4v7yyvb2rtrO5q6u1t8rRucG8maWiuLS6sLi1s7u0uLCkqaqlrq62pqyzvMLAxbOxsKqlp7avt6qqubW0yMa+sbOxqbCjrsHCvsWwtLrAwrCxrbO5tb6/")
    append("va+2tqqmqKy0wczDwL+tqaSmoqm1tLOyubfBts2wrbizq7+0sauouKvQ1dS8v769sbevsLG6t7qwxLeftLW4uLW5r6KuraChnKarxrixtLXHz8fLr7u6uLXB")
    append("ycK7zcjJvcXMtam6yLzDwsC+u7WyrLSzocS0vdnOv7S5rKaetbiuq7WwsLu3tK6epautrq2qsLKtuLi4v7a/qaSlr7e/wLyxs7OyvK+wtKihmrSvtq+zvsKz")
    append("rrS6r7WqrK2zp7O0wa+tr6WeqKy3vsq8xbeypa+0r6y1ta+xur2zuciiqqq5wMG/sqy8rbzNxMjIytTCt6uywKuso6+su7usp7S/ubKtsrCnsba4mqCisba2")
    append("xrzJxMDFu7nBvq+70crByMrHx728wLq+0Mu6w7zBt7+grqqxucGyyNHKvJy0qK6moqyxsbS9sLC2sp6trbGyuL60tbiwub69ub66tLWzrb7DtLCytqeusbK7")
    append("rq6urL6wrbW4trOxvcXSxLyqra2qqLCoyL65raqmsbO2xs3Ix7uto723uaa1tLa0ubzCxbGdqbK1uLPCw7+2vsS4u8mwwd3Iqqarr8Kyq7GwvM+rvrm5tquw")
    append("taeww7iypqOnsbu+z7fJyL29tbi+yLjKw8u7vrnCyMHLvMnMwbq9ucrBybC2tbK1tcC4ybjPuL2qlJyqqqi3ura7sLKyp6yzt73Fvbi4wL7FuLm5ys3Gqau6")
    append("xbawsLK5srmrp6uqobaysrurusOzsKaqurfNyr2psa6qpbG0x7q2pqmtu7e6vMbNwK2ttr3AvbO1sLq8x8nDyMS3sryzwLq/wsW9wq+0tMK0wuHUsqGnxbuy")
    append("t7Wv1tK3uMGvrK+xpKeyvZy0sKOupqaqrq+7uMKuu7bA1sfM1NDHw8C8tLLAu8DA1Lm4wLvFuKuutsC4uKy/0Mi9wqyuop2ksKq5sbqytbyxuLu3xsO8tb23")
    append("ubuqr621t7nAuMa4tKywrrWys6ursa+ovsTBt7Cmramnoq6vvcO5urG8u6msrLyvxsOuq6mxt7fAu8DIwq+0ub++vbzCuMC9w8XCvMTCxsS7vLy4y8C3v6u8")
    append("vMOuu8PEq6262cjGz7C11dHAw7OxrLOstK6vr728tK64sKuxqKalq6+3v7vV1LfK1M7Qxbu3tri7va/Ey8q9tsDHy7apscvExrvHz8rRsJCypaelp66uuKiw")
    append("tKupvbrJzcC2p7Ovr6ywur64xMi2tqi2r62usa6zpqOwoqWqqaiiqqWwtaippbu+wcW6xsfFqq64ucC4vrqzrLG0u729xL3QuK+1wLm8vrW/wcrIy83IwsG+")
    append("vbzDxMHK0cW5uLqtyrW9urXMqcTZ1drO3qzG2cnDvrG5pa69rbGuq8i8vLW5rqu0qKmsrK2utrPJ0sXHvr/Oxb63rrmztanDw9TFwsa7wLW8u8K4xbXPwsnO")
    append("r5iwsqijp7SoqKarrKvBv8PDu7KvpKiurq6ytLayt6+osKGtrLSoqaejnqqpoZ+krqWnsK6srLOwsLWvqJ+kpaqtprXTw8nBw8O4sLmyu7nHxMLJtrW8ubus")
    append("p662vrzFxsLGycC4tLjU0dDHy8G9urS8vsG8xK+9sM7KvsjO363BxMbNurrHqrrUsbasvLm8tLO1tLGysK+2s7K4rK/A073Izb3QxLy1ra+wurfCysfNxMTF")
    append("w8a6u8HGzrjBxs3Lq7GprqWkuLGpqKexobzDucG1vriztLe7uKq1taulsaCvtauyqqWjoKmvtaKup6XAu7Wuua66tquyrairp6WzurW1sbnEr6ysu8C1uLe0")
    append("tLnMvMnLu7i9uLyysrCxs7C1w7++wrarr7G3ysvP0MvG0L26urLAtKi1wMK5s7TKwLDGwdfjurfGyr/Tsr62xre9s7TAvru/w7O0sKy+ube8wrvHxsjBvr60")
    append("vba3u77OzsXEyMq7t8XDxcG7zsXKzNTCq62no6W0v6m3s6eorbnAw7utt7avwbi3t7q6sKzDvcayurepoJ+ipa+nsqq1t7/Jw8XEwa6wsauvsKK2urW+tci+")
    append("srKwsK60vLe0vb3Cw8rLxNPMuba4ubivtrSzrKurxLm5uLWmrbK0tMjAvs3b3MW7ybzCva+42saxsJe0trK1ttvjvMa40LfXsLvUwLqvtLLDwrrFwL2/tbOx")
    append("x7PC1rnGxMDRvL+7wcC0vsHHy87My7rCvcW3u83I28XK0tDArLGmqaOst6empqyurbPFwbmzuK2uvbSzs7y+r7q+uq+uuaWjo6GkrLGnsLesqq+lraOorrS1")
    append("srSwsa+9uqu1tbm4sLajqKOmtrq1t7i+wMXBxczGtby1tKqqtbW2tLGusbKxra6rprjGv7bC1tDX08jIvLe6wLm9v7zBuLHCu7W2t+TayNrAvrrEu7fhua+0")
    append("v8XGvb7KusvIucq017e00dHH18fA0sa6vb25v8PDxcrKv7ywucHIwsjMysfPy6q4sLGluK2ksaaZpbuzsMG9uLOxsq6xsLSxnae3urC3saWhpqSipJeYnKOl")
    append("oZuttaqvsbC0sbSsr7mxr6ysqKClrruqrsHBsbO9srmut7vAtsW8w8PJsbmxsau6qrOwurC2ra2oqqe1uLzBvLO5zM7TzMvCvLStwMe4uMHHw9O+rcLJvsC+")
    append("yce8x7W8taziuK64tcfHvcfAvdjByr7A0cfNz8PAxM/Cw8O+wLK9t7y6t7vFtb3Au77JvcjF3c7FvqyrtbCtubK2p6Wqs7y2y7+1r66jpK6rnaiRnK+qsJun")
    append("np6eoKWjpKKgrKWmm6Kxo5uqrKuurLe5rbS0uL+2tbKpqqSusczAvMC+ub69ub++v8O+u8XHtLWwsbCwpbi1r6+5s6yzsa+wwrOxtrCtv7jIz8LCysG2tMe/")
    append("v825u87Hu8awur+1xLi/v7e3tLjHsbmxwMO6uriwytjBv7rBxtXOzbPUsMS5xMjFx8bFvLu6sbbCuMvGvcjAwsq8xs3IvLGmtqS1oqaxn6G2tbLA18Oxq6al")
    append("qrCsk5WJqLeso6eqq66hoJ+lqK+yqKmtubCiqaapqbKopbi+t8XDwLO2v8HAv7KzwL+8uqi1ucHMtre3w7jGv8q/xq6ttqyvqbW7r7G1tLW5rbmqvbvAvLe/")
    append("v7vBy76/wMW+t7q+wsuxvru+tLe5rLivs7G5rrOvrbq6raytwr/Kyba9z8THqLuvw7W5s7HBwcS9vcPMxsvFx767t7XBwsfAubu/ycLDx8rFqK2xr6urqKOe")
    append("srSptLXB0Nq5qqirqZ6jnqGhqKuroq21ua6lqZ6hpKSiqq6xsqqrrLCssq+suLO+vLSyvL6+wrK6rLi60r3Br7e0u7m1vra2u7vAxcS9uq+xtLe5vLq/vLm6")
    append("wru5uLjEv7rEucXNz727v8K9t7/BxMTEt7zBsLzCuMbAta66qKyoqK+urq2zr6+2xNHK2cLGyMPBrbu2wLmzqLm8wr+9vMXQ09bVzb7Cub62t7+5rb7AxsDE")
    append("08rSnqu1qqykpaejtK6uwMPB4duuq7Wzn6WlnKOloqKfp6zGzrq8oqippLS6vbu8rLesoaSlqK2qtrSvtKyxtquvprK0rcGxxL7EsMLCt7Wqub61v7e3xcSz")
    append("sNG7tMO0ubiztri4ucK6vKy6rK29rrjFuLvBv7y1ub/Iu8O8w7W9rMrFwcLAxbvCurS4tq2utLKus7G/vcbJybe2v6/NqK21pKm6sLqqsbm/wcXQ09vUzM/M")
    append("0r+8u7StrMHAy7vJzMrGuq+4t6W0ocivvbO7vauu2+LOt7Cpp6afl6Opsbeln7nU2NO9squxrcTesK+2u7u9tK+rrq22xcu4xKu9wKzIwcrIuMrFucbLtsbP")
    append("sq60vLO1wMDAv8q2ubK+usO/xMO8u7u9wc2zw8LEvLzEvb/Ku8S8vrrCvLzAwr7Btc3Buc+xwMnIsMrSwsrI2MCytK25t7m9rsrHzMPGxrLDqLLIu83L1ca1")
    append("rLLBu9i5yNTYz8rHwsG4qrKrqb+1v8K5v7TF1rPEpqG7rry3xsDCtL3Izt/CraikpJydn66qq7exrMDA4NbLxLi9scjPwbu/t7fCysu6vcjB4MLPxb/RwrjA")
    append("zM7IwLi8pcPPxtmuxbqvxrm+xs7axcDOxLu+ubvHx8bFyrvAvcTDucnM0MrPzM3Gy8LCwMG9wcPEu77GvM+/wc7CxMLMwMrAxdXO08/AyLa9vcbBxMXBx8rP")
    append("t761s8DAw8nRyMTHwbvJv7nBuLy/vbWvtLiyrqu8q7bAsce5xsTHzbq3s6m+s7HEq8m5v8u8wMK1qaWjqKOws8zJqLKyrNDE3eTJurrFyMrg2L+8ycfPzNLL")
    append("psLNyc/KwsjJyMrDx8nEwK/LwMi1y9a8xs23zcXB087VzcbBq8e8xr6/wbzDy7q9vcPKu8zW2M3NwtbN1dDLx8rFvsy7vNLMv77ExNHFyMnLzM3D38vbzMzF")
    append("1c2/wcC+vb3Ftb26sLu9u8eytNHKxNHN0c7EvrC+sKiwr6+xs7u8vM7FvsjMvcW6zca/vL+0wLTBycfLw8Cst66zy7Kwq6KgusDS0tTDqamzt9fVz9TUubi6")
    append("xdPp58/K0MfM18vJzMTBxMzD0tC/v6m53LbMvsnOzMu4prTAzN7Q08y5zsbO1tHGt7u/uMLAvcbIurrB2dHPx+TR2dPI2NjLy8u7xrbBvcG70/HjyrrIyM3C")
    append("1dXd0szMxNDA0N3j1NPJv764x8W7rrawvsvIw87DzMe3vcvXzc/BtbWZlZaNmMe9wMTEzNK+zczDxb7IwdC6ysrL0L+2x72+tbCnorjDyr7Cv7zB1s3Qx8vA")
    append("rqiktb/M1ODbubvNxNTV4djP28bNzNm+1MmoqqKtt9TVw6fG46allKHUzNy6nLW90N3Hyb+0vr3F1c7UxLe9v8a4sLiwsLu119XHxtDOxtbC2M3MwMK2zcfV")
    append("wc7J1ujlv7bN18PM0szn2N/QwMq9vsrZ0tDLxMPC3cK6vMK1ysq8ysXVwL29xcfP19W9vczIsqCUrMm9xLvT4sbN0L7G0cy/x8jSzM3Kw7u5rq3ByMK6obe8")
    append("x8DFv7vDvMK+sLqzrbm4vMbL09TAvcLLuc3T3eTZwMTAyMrN0cjBv6GivdndspHA5Zt9rqm/vtTasLK1z9C+x7K4ua+9v8DMvLzXxrawyLG3psK8zMnEw8fC")
    append("wM+8ycXWydDMxdDeysrE09jLtMPDyszS0MPXz+rHwcnKw8LRz9zZ0svYztbVx7yxyMCzvLvWyNC01szP1su/xsbLpJ2lq8+3vsDN5dnbucrWxtvEu83l07/S")
    append("xMa5rrCmrbK3t6a2wbixuMe/srWzqbu5ura7yM7BwLvD0Mi707jP2efw1Nrd1sHIyMvQvL+/xsnAmrqvxLKtsLqvq8LHzdDF")
}.toString()

private val PHOTO_WARM_B64: String = StringBuilder(24576).apply {
    append("REUkMzo+STlMTFRBPEQ/RkdLT0tWPkhYYUxnZ1lyYlJdaHBpaVhkWkZgZUheXFhTWUJKbFdcVWReXF1YW1ZNYWNcbF9XY19gZ1RkYG1rWWNbbW9RcmVgT0pC")
    append("Yk1kSWRZTWh0ZldpUGZXXEtdVFJNSVlZT05kWmh1eGxiXFVUVlNRTkVMSFJYR0tlU1VSaVxoWHlmY01dbVplSWNAQVt9S4eCZWBCVVdhX2FOVmhoYEdSRUlS")
    append("XGdgbXRiaF1ST1xuOUYUISs3QTRXSkcuJzYtMDtMQkNTQkVlWFNpV1ZpP0hgYmVdYkZeT1xabGNbWVJTYkRRdVxlaWhjVk9NPz9AQ09Lal9bWUhiXFF2Vmx2")
    append("YXFue2xhgWZqUFZNX0hxUWVtXmtyaVZhVVRZW0hYSV1MUU9lUlZYXWJvfE1pXlZlS2xhQjlLPTNROExlUVBRfG9hVohpVkZcek5dUFdBRV+EWKCNclpIS1xw")
    append("Zm1UTH5vZE1VUUlCdnl7WINicGJdXV5sLjsaFx82QC5SPFMqKjEzOFZfREVdSk5YRlxaTVZdOj5TcWZeaUVaTWlOX2FeSj9UXExqYFdrYF5qVT5GOj8vNkpP")
    append("U1tWVEVYSGFuTGBycXR+hYCJeVJpS1dWUVhoY1Via25tXWlVUkRWSUdTRmlJW0tbSEpGXFZmXlZ3YGhLWV9vTT1ZQz9fOz1gUE9ReWhjWYFkWEZicURQR08/")
    append("SGJ9TZuLeGhJSVNxZG9YVnJ3a1NNTUJIcY13SoNgbmt2UmlfJysaERs2QzFGRlMrKzJCV0peSFdTOUtJRF1HPlNYPT9Ac2FcWEBkS2NWU0VYUz5WRTxYUFRl")
    append("TllmTTFAQz0nMURPQVZQSk5QUXNaXFZbZnVqeIl+aEZcTEpPXl9YT0xrZmhvXnVaW0ZXV0VHQHlTXGRUR0BcVmJiZHVodFRGVl1uUVU7RjhcLy9aYFhnb2ll")
    append("YYlUYU1RbVhYTExFP292R22Qd2xLQ1KBXXBVZm59e09AR0hKYYFxSG5gc2xvW15dKSgYEx0zODVJTzwpKThTXERVS1BHM0tMSlJJPFNUP0NAY15YUUBjTlxW")
    append("RztDUE1ENjRFU09RTWZuSTU9RDwrMC5GOVNGR1hOYVZcaVVcY1tab3NxWUtnUURJUWhdU2tfbFxmZ1tbVkZkX0U/PnxPXWtaRFJhYV5fdnd2YUhGVlJgRE88")
    append("QzVSKjJUY1Rmc2toXHVaZ1RJZ1ZZUkxHQmtxRVSVf3ZJRkp6XF9XcXF+hFNIREZIXXhsQ1ZpdHFpXV1nKSkWGCs4PTdORzEwLDpTXEpBT0tILVhKUEZUP1Bb")
    append("Q0pMW1dlTERVT11VSzk7TUFQLzxCVlRKVl5nRz44QTwwKi02QFY+RGBFWVBbXlZaWltpdml3YVFoVUNFT1lgZnpkZF1gW1NPPUBRTks+XY1aUTtSOmNleFtf")
    append("bG5yYUtCVUReaEs+Qy9TMjRRWE9neWVlUWhfYl1EZlpiTUVLSFZiR0iKknVJSUiAVlhbhmWAgVpTRUVGa3dtTFFmdW1yWF5wKigbHDZCPz9TRTU1MEJbTE89")
    append("PUk9K1tBS0dOQVJeQUpcaV1fS0RdTmZhVD05SD5HOUpBTVhDWUpdRUM3PUE4Py85PlU1R11aXFlPU2RiamxUY2lpYVJjWUJBUEFVbGlwZVdfV1VSRUFfUT87")
    append("Z4FIQEJLNVx1d15VcHFZaUxEVEViY00/TCtTMD4/UGFqc2JjSWNuZGtFZ15hS0NQSlldSUJ6lGtKS0V3VVVeh1CDf1hLSUhHg3FuV01aeGlxWWRhLjAcHjZA")
    append("PlZOQEJANmFEOU9BRE86L1FFS1dDQlhSQEpVaWZaSEplVmlZVUI3QzlCQk1MS15EUEVaRD48RTtDRTA8O1RAUmFVXV9aZm9qaV9fdHRoXU9kWEI/Sj5VWl5f")
    append("bFZhVVtTQ0paTkRFaXA5T0NkUlNzeltFYnBaZk9HT0RJWFdOXjVJMUU5T2JneG1oU1pydW5EVlhdXEVJUGNcTkN1kWZOSEJyUVNjhVJ+fVpGTElFdntrXlBd")
    append("cm1pY2ZpMi4dIDc6QFVDPEVHOWg7PEpHSUo2NkRIU1VKSVhNQUNNWmtdR0VuW1pVWUI5QjdBO0hPVGNLQ1RTST5CQzpHPTE3RmdTYlhSXFxUZWRyWU5gZXR0")
    append("ZlhiXEpDQz1RSmNKVVtlTl5aS0ReTFY+VGs8YU1oW3N2eFlIT2hfXlBKSEQ9V1JKZD9HOVVPSWZrgHdnWlZ0bGFBTVxYVUdMXGZSTERYintLRkNvVVJ8elJp")
    append("gGBQTEREdHhlcllbcmZrb2tdMCghKEI5PE5HN0tCOlg/REpNTUE5OD9NUElVR1dZPztSUGlSQkFoY1RcVj89PzlDN0FNRmRZQ1VHTD5FQz1LQ0g5VWFCYllU")
    append("V1BaZHNtWktoXm10a19dWk5DPj1LTFdLS0xgUGFWRUpUUk5EaWhCXklSeGyDd1pIRVZnUElMQUY4TUlPVkU/SGBSTmJmhXhwY1ltaGY8WFxTVUtPYVxJTj9e")
    append("eXVVSUmCVlKJf1dhe2NRSD9Rc25qdV9YeWZxb29aLiclKktFWlZDV05KS1NGRFBLV0M7NkZRTURWO1JYPT9XTGRMPVRYa0ZeVD0/RzpEN0A/QlFRQUtFVD5K")
    append("RjtaW11XVmREZVhRWVFfYXpQW1JvbWVvZGNkWlNDPztDUVhETE1JYWdVTzxcUU9Dak1MZl1NcWx6e2FMRUdcTUtMSUU3REg/ST81XWxmhWFnfnBsbF9oYWU+")
    append("Ul1ZXFVPXllHUURuZmNlVlZ/W1qAd1dZdWVHQz9maWxvbG1NZmtxZmJXLiUnPEJSYl5QSENQQ1JJSVpMUkU5NUlZT0pOQFNQQ0JeSlpIPWdcaEhaWD0+RzxF")
    append("OkAzQUVIQkpXUUlKQFRSZURNVWlLYlpPZ1pZV29WXV5gaWpvX2NrZFdIRDhBWF89SVFFW2BkYkZdTD4+VV1GVmRobGV7cGRQSkBSUk5LU0Q4QUs+TUVDcXNu")
    append("el5reXJ3bWRjYWVBTlleX2FLaGVJVEdwXWNpZWyCWmWGblVNd3ZDSz9zdGNvfmVGVXpmYlpUMyUtSTVKQV9kQkRMOFJORVtTWUo4MkdiRFdFR0lRSURiUltE")
    append("Sl5obExdVz07RT1EOT8xRkxNQU9NSFNZWW5OYkFTWWVHWWROY1NUT2VdV19bYGhlXV9laU1KTTpAU3BATE9EUlhdWlFaWjo5X2lIX1d5dGp8aV9XTz9RWltK")
    append("V0I5Q0g+UEZEeV9pZ1hleH19aVphZF9BT1taYl9IbW1NVEh2YlhwWHSDVmiCa1FIcINJUD11dV1xfXFIUHFhYV5aMS84U0RSQE1OTlJWNVVPP1ljV0Y6M0Nc")
    append("RUxFSEBNSUlhR1NGXFdpZ0lhVD07ST1BOz00QVtNQFo/UFheWmFKUUBeYVxIVW9OWktWU2ZnWFldXGloX2hXak1KTkBFUHJFTUdGUWJXS0xeVTc3WVNfYmV5")
    append("cGSIZltbVUNNTGRKUkE8R0o/U0VCWlhfcFxgdntxZVloYGg/T2RbXFtIa2VVU1ZuYlJhYXt6Um57b05EZ3VRTEFwbmNpf3xPU2ZqY2BxQjpCU0xOUEs3R05O")
    append("Ok1FPFpaVkdDMEljVUVEP0JPTFFcP0FBVlhpXU9XUUI4UT1EPTs3PE5MPlRXWV1XT15QUUpdbE5NWGtMXFFmXW1rV2FkYG1lZnJRZ1NLVD5LT2JMS0Q/UGRW")
    append("SUlGVUJKYVtmalaAaF+MZF1nZEZFSVhQTEY/SVBAUUJAS15PbmRge3lyaVdrZHQ+S1pnYFdJbFxPUWphYGFSZH16WE9+Z1JEXn9WRUtxcWFyfnVdXWJ1a2lz")
    append("MkFCUEBNZF06QktBN01BPlRkSEZYMUphVEpAOktLTlRVQjw/TVhmW1FXVEU6T0FIQT44OkJNTF1jU11NXm9TU1BYcEpLTmFNYF1wbW9qUGBgcW5fcHRUfFRM")
    append("Wj5TUWJOSUVCUmFaTEdKTVlLZVNpb0J5bl6OeGVnaUpCSFhQTE5ARlZBTD8/QV1LZGpme3ZydV5oaGo9SklwfFhKaVZMU21hXmReWpGDYkmIY05FZ2djUmBs")
    append("aWt4eHdna29xa3dvLUU9Sk1PVFlKVk06NU9FS1prVU9kPUVjVEhDPUJFR1hdQj9AUWNlX1VUUUU5S0ZGQj42Oj5SaGFVVlNXXGpSWVJXdFFHTWlMY15mdmxr")
    append("TFxiZmhZYW5XgE9IV0ZSUm1VSkVKVV5dUEhOSk1NYGFhZ0V6a2GIfWlZaEtHQlpLTVdAQlBBTkM+QFxMYG9odntyf2VxfWA7SEJkhFdKalFPYXVuYFpjVIaO")
    append("akZ4YkxEZlxYa29tWW6BfXBnXm1xcXFtLzU8P2FRSUZKXkI+NkpEVlhoXk1iRkpjUFpEQEBAS1xdQ0VBXGVeWldOSkM3TEJCQT41PDtZXVFjWVVXUFtTVFJf")
    append("dlNEVGxLY1NSdWxpUV1qYGpjVnVhY1hFT0VOUXVYSEdRWGVnUkhaUUVPbXRvXkp9cWB/fmRTbUtDQ1dHUVJAQUs+VkpAQ1dSXXRsdnpxg2l5gVtBRkNQelhO")
    append("Z09OY3JtY1VZV4GNbEdxaktJYV9LbW1oY2Z5hWptUGViYXhrLzNHPV9NRUFDSEFBNlRRW1xvVExqUFJMW1pFRks/R1VXSElDXFphXU9PVUU4TT8/P0EzQDxY")
    append("UlhWV1pWV2JQVlVhbmpCUXBLaVBNcW16Ul5kaXByUnheX15FVUNMVWtlSEhJX2N0WUxdWUVKdnF6ZEpvdl6Eem5bdE1CRFNRT0lCQk4/XE4/QlBoXnRxfHh4")
    append("iGeIh2NJRUhQb1pQakxOXm5mXGRaboN6bkl2b05QY2BUZW9nZW53gGlxXWNnYmZuMTI+PVZLQkFJQ0BHNVJcS25oWmNhVUNCU0VESkQ9Rl5gSkRFU2FkYFFR")
    append("VEo4Tj9CQ0I4PDtbVVpWUF5faWZQXllab25DSm9UclRZa2xzSFhgcnZ2U3VbZlpEWENMWmdTUUlGWF+BaFVVV0xJYWVzWkppcWSEdXBjcU1DRlhTS0lEQk5B")
    append("Xk8+QUt6bHF2f299fXCEi3JIQ0ltfVdMZEpJY3BrWGBfbo52YUtvaU5Lal93X3lyY1d4eG5oa2ZqbU5jMDY8QklQQ0RMTDxKM0hRR25dWFtUUVVDQ0NDTFRC")
    append("TWRaRkRHU2BkX2FQTko6TUBJSUI7N0BrVVNZVGFbYGVTYFxkcGpIR2dZcFJkdGtvRlZibXF2U2loZWBLVENLV11LWEdKZWGAdlpJUkhFVGJzX0p4cWaFc2pk")
    append("aEs/SGJTSk5JQU5CW1I+QktvdnRxeWiCeG5ylIBHQkJ9iFNGcU9aa2ZoWW9YYo2CZGN2b1hUclZobnZ8X1F+eHVlYm56aU9bMjdKPklMREtCUUFDNkdLV2FU")
    append("allOTExBQ0VHUVFIUFdbRkRLY1hdXVZSS0s+UkNHUUA3M01ZUFFSVlteX2ZUYVt1cWpMQltTY05ne2hvSFJoZXRrRWFuWWZPTkRIUFlNYUtNZl5xclFHTEZH")
    append("PlNwZkx1d2mFfGFnXks+S19kSUdLQk1CXFE+Q0tlfHhvfF1/e29smYBNOkCHfE9FXFBjaFtpXWhUXoh8WmJ0YnJodmlgfXpzZE5/bHdrXHmCalVaMjNMP0hE")
    append("Q008TkdBN0tTYUhkY2RMSk9GTVJPTElEYFFmTDtLXVddU0xWSE4/TkRBREE1M11UT11OWU5iVGdQY11sa2lQQE9RYFZoenFqTlJhZW9kUllkUGFRSUVFUFNi")
    append("b2JMUFp1dkhEX0dJP1JyaVl3cWuCiWFuXVBCS1duVUBOPklAW0s9QU5egHd4hGpyfXFrmohfOVOGb0lKXFBkXl9na2ReXI9uX1JqYW5kZ29YgYZ8VUxzaXde")
    append("XoZ7V1dVMjlERElGSE1BR0VEPkRQXFRkWWpIP0xST19NUERRY2VhTz5MWVdgTUpTSEs/RkdAQD84NWhXWVdSXFNbZWhRZ1toZmxQPktYX1dqfHRjVVxSYmJq")
    append("WFBeVFxKR0JGVkxgal9KTWB+dkRIZElHPlJra2x1c3OCgWVwYVRFS1VbV0BPP0ZBU0g+QlFlfm17hXWEf2l2io1VQ2uEdEpPVl11Wl9acF5pZZNicUxwZmFt")
    append("YWFbfX6BUFB+Z3FYZ294WEtHMkRBSUxOTFNXRj1AP0FPXVdbXl5FO0VFPEtOXGBXW2dhTUNVWFtaTFNOWEQ8RkxCQT47NmRPWVlXY1lVYl5TX1prZH5TPUZg")
    append("ZFpjeHBZVWhRZF5ZW1ZmUV1HSkJKV0pSYV1OUFpzczxOWUhGPVRsaVl1dYaPhWNmY05DTFdZWUFNQURATEo/P1J1emuAdnCKe26Cm41nQoKHgE1KVXtxamdd")
    append("e1twbYBkeVCEYWBmcVlme3yKTlWEdWNXg3qCZUZGMjZBRFtJTFlPRjw8Nj9gZVJmZEREQkNIQ2VTUV1YWWFaUUNgWlxWS1FMW0g7SU1DRj9AOWJOVlFbbmZb")
    append("W15UWlp0Y4RVO0ZSYlxedHFbXWVUamFbXlhXWFVJS0VJX0dTaF5UVFRra0hPVElJQ1J0dlB9cX6LjV5hXk1BVmBfX0NPR0I+SkhBPVp4dGd+bGyCdXCAnX9j")
    append("QVuRbU5HZXJ9eWtwcW5hfoNngV+HbmhiZ2F2dHuJVm2AeF9gf4iGbElHLDFOO1lKVFlQSDtCQE5bWlhmX01CTEJYR25cSFNmZGFYU0JcVFtdUVNNWE45RUlD")
    append("TUQ+QGNRVFdhYF9gXlxWYlpmcnNcQ0RKWGFwdHRWamVSXWdgZV9bV0xNSEZHaUlOZl5TT1ptfU9IUklJRFJzdlNzeXmKe11dXVE/WmpraUNRT0NASEZAOW1y")
    append("b3BsdG95e3V7gYhsQlqbblFJdIVvfWhsanZkdIdle1p9c3hUYWqGcHR5bGl9cGWBe3SAbVlGLDZYSFdac11XWFJHS1VWWlZjT0FBV1FWRlZTUlJpX1xdUUFT")
    append("VFRgVFZRTlM7P0I+S08/SGVUWGxbWVhgZVVcbV9remRdSD5GT2VxenpVel9OVmhgW11wV0tMSEdKbEpVc1JOQ15nbl0/TU1KQlB0dFBhd3iObl9aW2A+XWRw")
    append("bkRNUUVBREY/N3RvdH1hfXpwe3h6hJl+RWeVdllYeoNwemFneHBrfYVUZlV0aHBaaWlzcH11ZmiAeGtzfnR5e3BKLzxWZFJXcXRKTmRTUkNcV1ZlXUI8U2Va")
    append("TElOUl9hVldQTEhhXldaT1VQSk1AP0I/RlI+UW5NV25TXlZkWVBZa2NtcV5YUDtGUXVsfH9cemBVUWRZVGBmWklISUVPZ0pTZllJQlhlYl1DUlBJPklqg05L")
    append("Z4KPcGRaVXE/VFdfbkZIU0pBQUZAOGZyhWlgdHhxcXF/i56HS3KKeF1siYB4b2hvamNeeXhdZl5wXXt3X4NqfnhpZ3Z7eWhzdYNwdGVPL05bVk1Iam5iZltX")
    append("YDtaWFVrY0ZJUF5kTEVITldQVVZfSl1cYU9WT1dQU01EQENDQks7V2pPW2NhZFdiWlhRXmJrY1pVYj5NYHBscXZqb2lZTGZZV15jbUhIS0RZXExQamlCP1Rq")
    append("Y2JKUk9JPkdvhk4/Y5KJa2tbUWI+UFNYXkpHWUw/P0U/Nlt0bF9mand8cXt0e4VyVXV0e1mGiXp9dG11a3BdfHdWfYR0YWRuVnJrgWxrf3dycXJ9f390Y3BS")
    append("R0xWV1hhVndianBbWkFWXWJzWUhYZkxnX1VVXWZHWldhUF9XV1RfUVVWWlVGPkNGPkU8X2BbXmFgXVhiWVhOVlthWltbYUJXcWh1amtyamdTRmdaWGN/bEZP")
    append("T0BdWk5bYV8/O1JnamVRV1VMQ0Frfk1HaZCHbW5eUWZCTVNNUFJGVExAPERANVR+Y2Zkb3J4b3psd2VtZ3J6eGmUf3lvend6dWhwaXVZb5h/aF9rcHN3anJr")
    append("aIJ1c394fXt3aIhsV15fZWNpdX5nXWFMZV1cV1doYUxfXFFPUGVhbl9bVVdaS2FZU2JbVFZTWVFOPkVFPkNEVl9hV1xdY1dhWFNRWV1bVlppX0RhZm10aGR2")
    append("cmpRRmVcWmaHYUhSUUFkaExiY2NJOlZrWWZSTFpORz9ecUxGcoyIaW5cU2dETGJLSVpGS09BO0NBN1Z5WWNkcnR4cm5qdWxvenSFh4WQgmZle31wYm9udnpm")
    append("i5KNcHFxc3NyeHJ0e3x5eXF5dYBvin9oUVtdbm1Ma3BXVllDVUxYWGZdY1VVW2NZUURebXlVTmBOWmVkSlxXVldSYFFPPkFBQkFRUGBgTlRqZ1plUEdXYmRa")
    append("VF9sWkpiY3dxaWBtbWxTRWRpXWJlXktQTURfY0xjcmJKPVJoXWtTUFxPST9mZkpGaYd+c2dfUGBETXNKRlVIRlJGO0I+OF9rWmJnfpCLdmNrd3hzgn+QjY2N")
    append("f2Nrdm9se3mAfnOHgJqJdGxrZHBsdYOBgIFvdHd2f352k3NdVWpeQ1pCZGU/SWZKUk1VW2dgWFdkZmdcUz1ucHViV2ZfY2NZRk5ZY1ZVZE9KOj1EREpgUGNg")
    append("SFVuY1hiUkJdaG1aXGJwWUhfZW5vbmBsZmVYSGhmWFxbXU1LTEdaYE1tZVxKRU9mamtWUF5MSDp0ak5DXnp2e2VgR2FGTGtOR1NJRkxKPz07OW9rWlx0b5N9")
    append("bWtteHSBknSMlpd7h2V7cHeFdYF6d3OBfpZ6b3BuXV96dnyLl3qBc3NxgnyHd3lwW3JkRFBaYlpNM1ZHWmRfWGNORVJba2xiZldkb2dqbF5maVVQTVFWVVBZ")
    append("UlRPPEBCW1hbUWZTSVlhXlpoVkJecGZWW2NrbV5WcmNrbWRjZWdZTmVgW11dYVJITEldXk5eX19PR0d1Z2RfXFpKRD1taFFCXXF3eWhcR2NITltSSE9LRkdN")
    append("Qjk4OXtsXF6AZndsZ3Z3d32JjImFjX2Bhn14hYiKhXlraJCBkIJ4c3dqYGt5an6NmHl5e3N0dHRwaICCY15gU1NXZFNRRE5XYmZgXl9IVFVeYWtqYmtIaWRe")
    append("dWJoXFBOQkxQUUdNSFdMOz1Eb2lbXVtMV2ZbWWBsU0dlY2ZdTHFka2Zdf2lmbWlcZmxZUWdkXmdlcFpFREpmXllVZmtNRkdpZ29fa05KQEBpe1NEWml0fmdT")
    append("SWxJVV9uS0pUREdNRDo7P3BrV2eBY2hleX59gJ6DeH6HfHR+fnB+goiAf4BtgYJ8g32AdHdnaGt0eXuCgHh6cXhxeW5zdnl4W1VaQlFZZF82UlFZaWNcXFpp")
    append("Yk5wYVVuXmlVXVpTc3dqUU9PSEdJUU1KR0pFODpLYGlgXVRUc1RbVWVjUkFdWWRoTmhnaW5tdG91dGRfbWhbUWRscGpxbV9CQlRjXmFgbmRCRURNb3lgWFJP")
    append("PkpkfFZBZWVzhWdURl1MWF5vXkdYQUhMRjk8QWtrTnJzc3JwbGp/gpGFe4t3iXKEbX6EjZuPgX6Km3SEeXF+gWpzeHNzc3Z9eYyRd3hygHdzfn92WVpVU0lf")
    append("Tlw3VzxXYF9cWDtwaFttYGRqb2VoYlU9ZGptVFZMSUxJT09URURCNjZQU1tjTlBjakpVX2BdT0FXZGFcWmFsW2R+Z3VycGlqd2NbVWlze250ZV5EQmBeW2pn")
    append("ZGZIRUdJcmxnWVFPQFxdc1JEZGdpc2pcR1NOTl1kW0lSQ0dNRzo8QHFrTXhthnyBX216bZ+DaoqJfnaJeINvhYiNiICbfniPdYCJgnqAdGpndIWNcYOOgYZ+")
    append("enl3f31+XVpZWlNkXlleXEFWQ3BkVEVfUmFsbmpcblRjbGBAR1d0e05QSVdUTllXYkhDOTtNUWhkU1VgX01lc2RfS0hXYGJgX2htW2KBZ3Vnb2tybWJYVW15")
    append("cnptZVtLQk9XX25hcF9gQ05SYGxhVVBORmhlclZUbGprbW5hSlFOSnhtXElLQkBLRjo8P2xnUWeAhoJ4Zn9ndYl6eIeXdH6CgHZxbH1/goeJeIh/boqHfHl7")
    append("gINxeJSCfnuAjYaSeHx4em9yWV9hbWlybWpra1lbSF9sVmVlUGxjYVJdX2NnVG9PQVJycWVfT09eWGFdZF1aS0hGUWVpYFdMUE+BX11nRVZnVWtgZGtnWWx2")
    append("dmJpbGplZl5bb31+ZWtwXlxWR0dZYHVlYl5iRFdPaXJjWU9HS3FhaWpuaGZocnRjTFBGRYZtYltMQ0BJRjk8PnVzUmZ9ioBycXtpd3Fzg4V+eHh8cXN/cG5x")
    append("i4pzeoF0fYCSent6eX90co1/fIx+g4KKdINgb31ncWxnXld4ZV1gZWpfYldsU2d1aWlrS1hqX3V2VH9VS11QbGhaU1NgZ1RPU2pzWU9ETWJuU0hJTVhqV2Fj")
    append("RWVbaG9rY2Vke3KIgV1qam5gbGh0eY5+cHFscWhYYEhWXnl1XGloSmRHcnprU1FITWhlXF5eXWx1cGVkS1VBQn5lYF5aQkFFQjo9RYR2VHt/dnR0gIB4fXx1")
    append("hm9yhXeEbnBmc3Jyhn1nhmxxj5NxbHB3dGp7cYB6kn58eYuBhX1Xbnl+YVhXW01cV0I+V2xsWV1pWllrbnB+U2BqaHF/W3NtU2RabmlNWGFfZk5CV2ZjTFxT")
    append("SGVvSkdPXWBaXG9kVWRTbIBuW112gnCOiGlebW1ma212iHdxcF9ocm1NWEVcZHxpZWZuUFdTa2VfVVNHU2BsVFxoZGVza2tlY1pPQ29rYmBXQkE8PzxAWHJm")
    append("aIJ8dIdyiW+GcHlrj2J4jH+AfG9rcGl1eGp7fmh5jYtpfHl2c3ODgIF8dnCZg4l1knJqYVx0X1RfXlVIVEQ7T1BsVmlkXEtRcWxYRlRvX2F3YFxaUFpmZGRc")
    append("Y19lZ1ZOVWtbXlZQUWprTEpmWl1dZFxlWGpsb3FfZXF6doyQdmhrbGpnfmlohIBtdVthaWleZ01eYX11dGVlVElcbl5wXVlJYGhoUWRqb2dpbmplb1lcUXlm")
    append("W2FQQkA5Pz1CbGlmgIR5boiAfWJ+eHeAknR7c3qgjHZ2bnJ/XmOAe3yTeIZ8dmpxd21ydoVvfXeQloZ9f3NpYFdhckVgW15XXldMVVJcVmVnYVtZXGVWV09u")
    append("WVZeb1xbZkVnYmBba2RhZ2lMVlZdcVdOV2NlUlZjV2NeWVZXYm92c2dMVG+FiY5/bW9sbnFydXZraIJuUl5mdGlUYFlucIN2bWxaYldcVHFyVGdRdGhqYl5a")
    append("bWZkbWplYWBVYG94bV1QQEA4PEJehmt8i4d7doJ5inJ6cop+b4B/fIaLknB9b315a4aHhY2MkouCf35vdXhwdWFsfXmSinl+b4FlXV1aWldXb2liTmZjXVdc")
    append("VXFiZV1lR2haal9rXmxqcWhbc01Tb2pRaXRdYHFDT0dmb1FSUVxuU1dqWF9UTVlad3B9clxWZ22Ii4V0dXB0fGN0YnB2f31uYWdkcnZmZkVbenRiaXd1clZb")
    append("QlZqVFtmYmhXUl9mZW5udGlnal5WXFuDZFRaQEE4PE1oh3ePlX1thoKDenGMc35xa4iDeYSEkIWGgoJzeZCamY+LioCSZoJ7b3l3f3F+coCMcXuHf3hYWHNd")
    append("Q2hra1llO1hqYGZbVm1gX2dfOmZfa3dvdXt4cGhkYlxgcWhaZ29gS1tiR1VjeF5GTGd4TWFhWk9WZE9YbX5wb1trbIt9hn9sdnB8d3FtcXFxZ3twdXZgdVpq")
    append("emtgbnNuaXRvaVdmTFZeYGBuWW5dZ2hzbm5rdGFnc21cWFNtXGVhQj85Pm9siYGOh3F9inCBcmdzX3FvdnmFiH2Id5VvlW+AlIiWiJCdk4lzdHNsdHSHc4Rk")
    append("ZXdviY6AiX5hZHBoUmN3Z1RlREhYYWVfcWZkYGtuUklfdHp8fHp8cVlmal5WZmdUU19tY2FwUVNtYl1XUXFwU11UXmBqZl9zdYN3ZmFpdZ9rd2dkd3R6cW5x")
    append("cndwVnp0dHd2amtpdHN5b3d7ZmxuX3BsTVpXU2dsY2NZbFZ3bXZ7Z2Rje3tVYlFhZmJWQz47TFp2dY6KgneGf3F1fHBUbXlxeH+HhZCOhYRugGuMkn2PjpKX")
    append("iIJ0en+Cb2x4eoNsYGhleoV9g4N4c215Z09mXltZTlBcZmJxcmNuZWRtcVZdcV9xa19jbGdVbltLal1iTEddal1yUFNrYUxdW1tcZFBKWFhnZnhsfmNqV2Bu")
    append("iYd0bWJ0bXt4cW9kfnR2hYh1dndzY2prZmlra29gaGlpa2VqWVBXX2poaGdfWVxwaYVuXWhgdWtmYlJRZVBQQz88W1yCY4+Ii4B/dndmfoRcemh3e4J2hYCM")
    append("hoV8eX2VfYCRoY2FiIJygYqCbWtpbnd4b2RleIiAinRyf4SDZDxQRVRCR1VfbGl2eGBlaXB0b3V2f1tiZXRkamFmaWVjdlpjWkhYa1VfSWBpVUpNVUxZXmZg")
    append("e0xcbn9kemNZaWF1fHeAcICDbXZ7b3Ztf3aEjYJ0XXt3aWVgWmxyZGZecGp1bmFsY0ZUWnFmcGJaaF1oaINmbmpuc2tqXFNLXldNRD88bHd8eYGDhoJtd25k")
    append("fnJfamqFe4t/e4x2g45qa4uOd5yLkZCTiHWJkYd7eXVyenhxeXh1dpuPj3R1fXRwUEZfUlI/R1plf3h5iVlkZWVtbnRpfHdoY3RvWmZgW2BiaWpmY1dFXG9j")
    append("aGZySz5UTlhdbGddcWZnc31tbmpodWh1cHlzeXd+dnd8cnp7aHB+hYR+bnR0a2NnZ29rWl51gHN3aGhnZUpHU2xabFhzcWRkbWxzXWx3cm9wZ2FnSVNJRz5C")
    append("YXN0hH+GgHqKf39xdW94VHVxfHuKZm5/Z5NaWJiDfKaUnpGMgYWLkoiDfXt0fYd9foBubZGkhnV3dG51ZGJqYVFFRlR7ilqBcmZhZGNmZXhyZoBscV5jbWJe")
    append("YmNqZX1ycHVeUWViY35tRDtFVmtbalJWaW5RZnFPWWhtbn19ZHp3c295enuBe352fHx+gIOAXnV6b2ZubV9iXVtza3lsb2xxZmVmW2ZWbWZrb29qcnJyZlxv")
    append("eGxiaFdwSVVMUD9KWGKDiH2EfHqKj3Z+bGp2a29riod8bGd8c5NkUod8kZKWnoqLi5CIjYh+a4B5e35+h3mEf5uch3x+cHF8fG5ua1NHR094iGKEcWBZY2Vr")
    append("XWhua2J5dWdfZF5wWl5vc3t1ampobWFgW2pkRDo9WFxsZFNkYnZDaHhnZ21jcoxua3x4cHByfHmJf3t5d3V3f4d+UnFzbXZ7gHFvcWZwbmVwc3dxaXZ6XmBl")
    append("Wm9sX2pibHxkbWpucXlaa1JkSUdQUERWX2+GgHl5enZ9g3Vva31ugG1zi4qSZHt0kXlzVIGWnI6olZSUio+OioiFd3B+eHN6gX58gYqah4R9cX2FbXJ1cFRR")
    append("SEJid3F7dF9PaVxiSnBwakV2eXBua15ocGRxa21vV2NweVFbTllbQzs8amFrY2hgZHJZbXt8bnFwfYRndHV7fHx0eHmMhXZ1cnJygoZ9d3VsanZzc29oa3Rs")
    append("dW51ZHJmcmhiXlhfZm5zcGVgZ3lpb2KAcGp2dl5hSUBHXU1fboWLgn92fHJ2bIdta3ZweVyBhIOFcId4eGxzbIOdn62imYd/hpiRhoWAem1rdHF9fZB9eX+J")
    append("fH1zeXV+ZHdtZlZPSkBaZHprdl9ZZFhdP2p0blx6dXRpZ2liZWlqYHFuWGdpa19mV1BZQzs6WnNfZWluZWtsfHZ7fWx+fHRueXR9f3GFdH6IgYB4c3t+iIN6")
    append("bG1qbm94cHFraHRye3ZxXnF6emRoe2VneFtvaGdZa3RpaHB0Z32Bd19uTEJPXElnZo6ChYN6dHl0dIN4d3h/gW6BenyDipF/dXR3b42fq5WFfnuFkJyKjIJv")
    append("d36FgZORgIKKhIF6c3hyhnx4e3huXllJUEtjXH93a15rZVhpSGN5cnx9bW5YaFxlZG9pU2dnc2xmXGBscUxSTzpCY3Jga1V2Z1ptdnB2cGiCjHdPf3pxc2x6")
    append("bn2DeYFzdIOBhoKAb2F2d2d/ZnRyZW9wcnNrbWxxdm9reGF4dGBfXHZSeHt5ZYR4bZF3ZkddS0JYTl5ngHV/dIN3g3ZvgnVzfYF8h3yHdH6Tg3d9a4SGhqGj")
    append("oIZ2doaNnZCHi4BoaHp9i3mNg4mKjZN4bW97f391eHNuYWNKTk1odH15ZmVmW2Z0a3V6cIB8cFRYZ2ZqaHBfXWZqdGRscV9lcE5YWTtIYm98blB6cmpvbV95")
    append("bm6JfntJbYJ9bm94d4OCeHlwdYV+hn94gH5td2+BXG54Z3ZuanNtcHJ9eGtXcGt+b2dmaHBtfnN3a352hn+NVUxIWUVNUn10knqGc3t0eXBteGx8c4h9dGx9")
    append("cn6Fgnl6aXuBoKeeeXd5coiXk5CHfnV5c36DhoF6ho2VjJB2anJ7gHV7aGBlcHVrZ155gnl7YWlZZGdpeYB3e4Z4ZWhybWZxdXJKVmxia3FrZF1kY0lJXEFG")
    append("U2dub293anR5dWR5d4KGb31Rbnp9doJ+g4CEf3NzgIyFiX98fYVidHd1eH12aHFobnt0cXB0dGhjaIFxaH5aYWBee3tme2Vsj4J6Sk1FWEtJVXmMi3t1eGx4")
    append("enNtam98doRzfnJ/jnaNk4GAaJSOoZp7enx1iJaFeol1dHJ4fpl1kHiBiouXk5OKb2uAh32BbWttdHh5gHaOg4R4cGJkZmtog3h0hY9yZ2FrX21qc2pLTmNa")
    append("Y2daXFtaZFJFWkNcSGlaa3xfaW5veoBwdnZ5bnx3c3N7fo94gnd7gnN+f4iNj39/dXd7a3J2fX10eGtobG9ranVub3NnWHJzaHFrYF5lb3xhc2eMkHxbSUxE")
    append("SlBEW4aAi3Z5fG10eHh4ZXZ8dXt/hXaFh4WPl42Ed5ieooF8gXSIjYl6iXZykJmHjIhicYGBkn+Qh4SEgHiDeYOLY2txdX2KjIaJgXxzcWhmZHJoe25vinhv")
    append("Y2tnc2heYGNdVFdcaV9YVlJdalBVSlphX2FEc4VfaG5ren9xg3B1bnOLeHF7gHZ3gHh+gHKMh4qTkHtsaWpxcnF2fYJxcm5rZWpsdXB4eHRtVnR8aFtdXlxw")
    append("YWt4ZoSNiGFJRklHRElGd4iHfYR4eXVud4BzY3pud4GKdoOHeHqNjHyAmpuhiXxwbHiMj3yFgH18jZeYjYBtfH6Ih4mOgHaCh4WFd4iTf3WBgH13dYB2cHxv")
    append("aWlrcGlwdX6BinplVmdrb2dmaFxeVFRlWGRVXFRlXlNOU3ltelFNbXlgbG5qcXh6gHxucHSPgWh9h3h8fHZ3dG+EjoSFeWlpaG9nZW98h3ltbG15c2tobnl4")
    append("bnZrZmdtZVdfdWNva2GGZpKGhGNEQ0ZJQkJGdoiMd3tvb3h2fH9ua3R3gnB3cZSCfnN8f4aTjp2Qj4B5cZeYhoWAe4mMk5aSm4+Pg5KLi5GRgGlwgYGFkX+P")
    append("cHSEZ2NnZWdmbXZqbHBueGt3Z3qAhntoV2NZZXBjY2RcXl9eWmJXVVxlUUtSWHJ4dEliWmVjbWZybXqAcXF4eG6NfGd/gX5qhHZ6f4GQjX5ycW95fnhrcGRq")
    append("eGpbbG51dHJwfnB8eHRqZ2xpaIF2fnlib3tojI2BdFBDREZHQkJLfoiLgXZ1cnZ8dnRxeHN0f3Jtiop3fHRweImTm5WGkIGTlKKJgIx4hYKUj5OclZuMl4eH")
    append("h5B5coV0dW5wg4eMZnR0V11VWl1qbGBsaG98cm93bHmEhm1mZFZjY2VsaXFgb1lgYGRiVFBeSk9SWWxvc2JvfXR3eHBpbmpyaHN9e399eFiXg3SAgHR0fH2J")
    append("f4RzenB3d3JzgWVZWVVcfIJ4eHNxgW1+hm5ycF9xhG1ug29aW35llIeFbEhERUdGQUNfi3eRdoF3cXN5fW51fHN6dHCMk4mGeHF2e4WDn4d3lZKmpZaAiX1x")
    append("g4mRk4KCfJSSd3mPlZGOiHpuenp2dXiCaYRvZG9VWmBpZWF4b3h3amZgdXWBenJsXlpUZGVfZW1mdGViYlthWlphWUdQWnRydnBzeXBqfWVbaltud3p5dYd1")
    append("bkSAjm14fXN5cHR8iIJ5dXpwcHp9dm9ZT1J1f3t4dHF9eXmDfH54bW1zc1xzeXh4XXSLj4d5ak1DRUdGQUdyjn55en94fnR7cnJ9cHZzf5GPiXmBf3N4eHmN")
    append("gHWDmqSflouFh2Z9fnOFg4CDa354gpaZj4eGfoR/hH+If3qBcXVobmJYXVNWa2ZkbmtrYVlZWnR1fIBqX21sZHFjZHVnbXRwdGJlWU5SVURUZHt0Y3J0dXZk")
    append("fV5dbWR8dn5vYnp4aVZuiGxveHiFaYSNiHl1dn11ZXZ1fnJuY2t9fX16gneEeG98gnl8dnxnYHWGgW9vUVWSipRuYUpBR0dGPlmEho95hYB7d3J7cXaFdHqO")
    append("k4t9enqRgHt6dYOGdn+UiZeWj3yDmH9/eYWIhHZ3co+PgYqKh4B/fH5+eoyAhnyLZGhre2psYVhYZF1kc2diZlxZVmBneX1eY25udVt0cmxxZW90cW5lZWNN")
    append("Vklbb3xnZGN6eXVndGxjeH6Fhn5vYnhtW1ZgdW91fox6fYuGh3V8fXV2bHFzdWNoenB4h4hxb2yAenqHgXx7enlaZ3eCfHlpUE6FfpN6VElCSEdDRn6Di3yI")
    append("j3N8dnN1fXp1hoqCgXtxf4mFj314e3xvfZSRjot8hIiYjISMgYSAjH97jJmDh46QjYqCgIaAgo6RgX+DZW+KhWpubGFlYFhTZWBWXVVOYmBhd25aYm1qZXxp")
    append("ZmhvZmZtcXNtcGNMUk9ZY3FYbU5pdmNlZ25ld5uNhW92cnF0bl9gZ3F4jJGCeI17f3V7e3Jzdnl4cGxycH+LkYZvd3Btc5F3enaAdWZrW3Z2cn9jR1R+iIB8")
    append("VkpGR0ZEZYiJjnWFkXuAfnp2eXtkhIx3b3dvioqQj4eFgXiJlZeHfnp8i5KNjIB+iYGIkZGOjp6MkpGTjIV/gYN+jYJ+fYB8fXx8cGNkal1YXWlZZ25mZlNP")
    append("Wl5edWpgXGVuW25uZWZqbWdzcm9tbmdRUGRiZltfb2BxcmRmXmZkfY+JdXJ+bmWAd2NpbnKFmIh1gnx4eH15iHlxcX95dWR+f4qKhHdtfG9lcXyBh390dXR8")
    append("WH9qhm1tWGNuhXt0VVBEQUFUeH6EhXyRhH97cXJte4hxaYt8en2Ph4iPj4+NfYmJj4x9hIKbjZSHh4Z3f36Il46SiYKNjn6Mi42JkoaLl4B+dXt7eH9ybWty")
    append("Z2hsYmh3gn50cXVjV11fZ3NnW2BdbWdpcHlve3l8e3V6bXBjS1lgalVpa2pxbWRmY2hgh4mEeH10bXV7hH9nd3WPh3qMhXiFhYp3f4Bye4x3cF13foN6fHeP")
    append("eXx2eW54h4F1dGtsWnF0g2huYGleeXVja15CQEFecIeDhX6BfX54dnNofo55cYWGh46IfZqMkYuGj4+GmH2AgZyQiYuLdIB+jYGKmZSNf4CGfHeAgIiKiYSQ")
    append("h397dnt5enJlZWptcWt5an2MdHZ5dX54YF1kYXVmV2JUaWVtXIFpf4R2iHeBdm1eUU9OXF1qZnBzbnVyZG5tgY9wgG50c4J1iop6fn2Zk4mHi4CLe4l6fn93")
    append("f3trfYKEhYpwbmp+lZCDhG11g4B2gW9tamx1fGRgdGFua3hkZFRGQUV5g4h+gIp5hXeCeXyChYqDi3iOhoV3f5qWlpmKkYtwhm6IjIyPkYyLg352iYyQkox0")
    append("bmt1jo+PjYqNjomMjH93eG96e2xrZmtrc2lydX9lZm9pfH19d2xkZWduW2RiZmF8aHBufYZ5eHZvdmlaWVlSX2tqZnB2fIF8aXJtioBzcGiEdG1/j42PiY6Z")
    append("loeIkIeIfIx7gH97gIBvk4N4fHx9cHprf4CInHqCeHZ1dF9gb2V+dV5hbXJhW3psWFlHQ0+CiX6IeHV3eIOBf3eDi5qQjoaVhoV7j5eZnoyWn46DhYiKfYKN")
    append("kIeQioWLjJaIinp6f4yWl5WVlZCGiIaCf4B4fXB6aGltb3FxcW1pdXRtXl5VanR3fnZvYmVmdHh0YmlwcHNqcnZ1d3JibmleWFBSaXdwdXuBjm1yZXF1i2Zr")
    append("ZXh4cYCPmZGfk5CSiY2bjnyDgoeKfnZ8fHZngWZ5gYdpWG5za355hIV8fWx3dXN6fWt0emJrdWpiXHFfc1pGSGqRk3mDh3OAfYJ9fYWLk4+TiZKFkY+Yj5CC")
    append("fpKekI95eoN9foiNlpCDjIeFhIqDcXV2fomQn5GGgoN5iYaDgn59eHJyZmlnb2ZpYGxscXNpbXFscHZzcGVwf2RfcoV3d21nbmpweHB2b2hrZGddW1JWY3p3")
    append("gnGFhmNtamh7g31ya2tsfJKMlImHkpmSjJmciH2AgHuFeH1qeGxsb3GJenduW4F9doiAeot7fG1ofYmFdnNne3V+gnR5ZmFoY1xEXHOFj4B3eHR4fYR+eXWK")
    append("j4OUlKGZnYyHfoF7dIOHc4WEi4KKhZWMhYaLkX52eXRkZm11j4iXi4SDknZ0goaCfnh+eX15aGtpbWxqWmlnbmhmcnV3dXZvbm5xc2pfbHORe3x9c21lcHly")
    append("YW9ybGVbYlxra3xzf494d2txYGB0enhsd3F5jYmKlo6GkJ6Zl5eNioN7fnR3dm9fe32BdZSMd2yDeXGBj4NvcZKKgmx2eHh4h3hifWyKentzZGpsaF1LaYuB")
    append("kIWAeH97eIeBd3WLgZCOlZijjX1+gYN4fHlveIJ9iYyTlJSajISMi4N+cHhthnqOj5uCin6JfHGDiYWFf4qBgH6AYmhsYWtpZGdfcF5zd3Vxb3t7enRzZ2hh")
    append("cGp4cnF0eIZ6a3FoZW92ZV5tVVtiY26LeoN7c21sYWt1dnBzhI6Df4KOiXuKlZGWm4qGh4V4gH1veGhheIKDgIV4a258dYR4f394c3+RaoFwi3WAimJpc319")
    append("fYdoc2decWJWcYyIkZWCe4R4gHuBeIWEjYeRl5+UjYVtdoN1dn9ze4GJj4qMnZaWlo+LhYZ/hZSIl5KLkJmKkIOGfYSEgYeFfIqJiHmJaGRsZm93d25rZ1de")
    append("e250foGLgXxtZmdjcWhsaGF3f4KIfHhyfWxsZmp0VmRnX2WCjHB8dXVtZWh9cHCFjHqAbIV+gYKMi46djZGBf4N9e4FsY2BtdnKBeHB+Zmh1cG9uaXJ8d3uQ")
    append("c357fIGLf3VyZ3tve4lreWRdbGxtfIyRgYiBiXl7h3OCgY+OmoyclJiHmJGAfGp3coeDiJWbjIGSjZeVf4J/bXN9iouGiYeljXl8eHR7ent4eIqGiYmEhH6C")
    append("aGppa3CKj350aFVicG99gIGKeXlzb3Jyc3Vqbll2d3R4gG+EiISGdXxXWFllWmhshH2LfXxtZXSBcXOSgX9xbYKDjJWJg5qdk5J8cHSAioJvYGpkgnaBgXZ7")
    append("YHd/ZGhgb2tmen54ZnZ1e316fX99Znx1cHR+cVlhbHV0fYuVhnyKi3mEiXyGho+inZeZk5GRiYuHfXaAeYx+lpyPg4J+foGDfYOAfYmOipOVh4yTf4KGeXp/")
    append("fIaAh4aMjI2NkoZ+Xm9neH+VkIF2aVtpaW13fXx6cHR3dXl4d3d6aV17dHhwa2VygIh1h2xaaWRfWldkeoB5ZGluZ4CCc3KLkIVwfYmRj4ePeoCDlpmHj2yB")
    append("cXhxcF9veYd3dH1+ZGd2bnNsYXl9aHNmZG9+dHxue3l4eHZ2cXOGZlpgcXd3jICKl4qEf3h5h4KGhZaTmIyRin+NhYWIeHl3gH2BhYp/iYB7ioiCen+HioN9")
    append("lY2KkHx5fnyHiYaFbXRncHyNjoePlo+QW2dqfYKMhHl2X1dWX2x9dHNybnp2dHN0dnduaXZ3gHZ3dHBpa3R4cmVycWR9ZVBjhXNwbWR4anZ5ZWCId318iZSM")
    append("hZeQcGWFkJeJiH93aHZmcm1+h4F7bGxoeGhtfIt7aWttcGtpcoNycXN2boF3f3l2fXF5aG9ZcGt+nY+Kjoh5fXt/i4GEkYKIjoB7iYiDeXh6gYBzcXR6lo2B")
    append("c4KJjIaDhIeNlIWMk4aCh4OFfHqRkJaShH9qbHqQjYOLlZCKX2xrdYCEdXRyaVxUXnx+eHJpam59bm9oamltZ2pse3SBcm5vaWpwZGt4eWZ9ZlBgbHpwZXBw")
    append("Z294VFxyeneJk4uQjJyVgnWEjJKJjZaSjIt5g3ebhINzXXFkYWZoeoGAhYN5cXmDYoB0ZIVvfXB2em14fIVvXmVsZXl6i5Wbi318h4CBg3+LgXyCeXaAlYWA")
    append("gHySmIR5g4J9hW96hpSDg4OJhYeGhIKAdHR8hIqZk5SQk5KRhXh7gIB3hIuGjoKRY2ZteIeCeXVsa2NgXX2BeWpqZmdvbnFnbWppbWhoaW93c29yYG58YWdx")
    append("cG2DcXFwYG9cUWlnWm50UF5vcoiGkoqagZWQgGuCio97ioiGiZeZjY+RgoxdWoBlYW1kd4CKj4WAeodzbWdvdHxuantviWNtbHxuX2JadXh6kJOak4iLgpF8")
    append("gJGReoFra3iHnI2Ki5GLg318dnKHgXV8k5eHhomOlI6Ci5SDjpCZk4yOj4+SkoKDfXN5cXN0jot4hnyJaGpqhH95dHFuY2ReX4KBbnN2bm1ub2RoY15ha25u")
    append("dXdwampiYWx+eHB7XnZyen1ga2BUY19aTX9jU11ueIqAlo6BlY6KeHWFfoiAkIaBiIyPk5CRkpFoYnlxfHxofICPl4WJhpd4eGJoaWd2bIBvd296cYdlcmtf")
    append("d3N/h5WZlIeOjYp/ho55f3tzf4mPmJaFh4iLiIeHg5CZi42KkouTk5aWk4aNiJOSiZKQjJCYmo+Uj4uXm5iXl4eBi4FqgneEYF9ugXN1cXNlZF5iZ4N1cHiG")
    append("f3R3dG5vaWpnbXd9e3hcXF1hZ3R8en5tamp1bIBxeV1kdGlYSIZubWFlenl1koFpjIiNdo2BjIyHgXuBhoaEjICAlYZ2ZYtsbH9vdYaJk4yCeoJzbWpgYmlu")
    append("b3FxYHhuaoRyZ2dmeHyLi5WVhY2RjoyBi3p5g3SGeoiIiYB7f4CGho+LjKKMfoqIl5mbmJCKhoeBkIaBgImDhY6IjJ2Xm52Tkot/fnx8eYF+l56gamZxd3h4")
    append("dHRbbF9fbH9ud3eDend8eX16cXJzcHp/god2Yl5YYmdwYHJ5dHt0c418dGx0b3pnbYh2dVZNfX9ubnpkhnmEb4+Ej4h8dnV2cnOCi5B5gH9+dHhsZHJ/d4uH")
    append("fYJ6cnJlZWdvY2R4gHBqYmt9aXBuZltxe4SGlZiKhpiBhI6ReoN7dXyOf4d+f3Z6fXd+hYNygHOFfnyPmpSUk4uOjISFhn5vd3x3gomSj5uYkp6FgX92d3F3")
    append("gYaPlJ+aa290bnhucGtdY11ca3xxd3mFent8fn6AcXZ4c36DhYJ8e2htcWVaVGh2a29wa4dyWWh9en9zeH+Lg1FMfXV7ZXlifX1leY+UiI18dIN7dXByhoaE")
    append("dYJ5ZXpyb3tsgYmAdHR8cm1nYmRsZHt1fXJ8Z2F0XW9saWByhoOLlZmUjo+HkIyEh4h2d4KIfY19f358f3l9g4B+fYWJeo2Ump6clpCWhYOLl5CUkJOZkYSI")
    append("ko2Qi4l8doSBfH6Ci4OYnqqlc2t0cXJvbmxtZWZkcXRyfYmBfHx7fHqAfn5+fYaGfXdsdH9uW2JXXGlqdHBwcX90a3F0emted4GGg01UcWR6eXp2cYVYh5yJ")
    append("fn19hIFxgWV3gXR+Y3l9Zm10Z3uDc3qEd3p5c25yaWxydX6Cf3JsdHJqbnNlZIB2hH+ZjJCcioqSj3x/iXl8gYyNi46Dg4CCkI6Hgo6CfpuYkpqamqmkoJiS")
    append("kp2PjZWJlo6PjI2YjISHjoySjI+SiXyMi3x/mZSNd292dGxqdGdyaWthanp9g4l3h3h1b3d6eoV/gYd8dHNxeIR6ZWRXbGxsa3R9d3KAdmtvb2JrjHxwdXV5")
    append("cXR5c4t7hJRTiJeBiX59iXd7g16EeYJ6ZWZpbHR3enaAf3uBeIB6gH59e3lyg3l2eXVoeWl9X3BwdIGBgImZh4WEjY+EfYCFbXmCjpWemo2Gg5SNk4WPjaCL")
    append("laCTn5+cnp+ajpWWnZaOnJmVk5iRko+JjYWChHWCio2PjJCIhHGGiIGJcW1ueW9uc2ttcmRgdYCCf3t/e3R1dXp2d4B3f4CBcnyCf3mCen9ua21pYG56fmh2")
    append("eGZ4cmNtgWyEeHyKdYB0eYiDjoRvkomOgISDh4uVbWKFgo98c2VqbHxpj3+FgXt8cmt2g4iFjoh2gWxteWxucG6Gdm1veX+IgJuHhIaEmI+Bgod9YHaPl52b")
    append("j4iKhZB7jX6MmpmVloaboaGZm5eRnZyZiZCamp2clYqPn42Dg3l7fICAeXl6e4iJen+GhJeUcGtjfn1+b3VmbmNqa32FeX+Be3J0dXZ4gXl5hHh5dnh1a3Nw")
    append("hX11aHBub2p1eXN6am14dXd1jnt7koeVf42EkYqOlYGIkpqdgIJ0e4yIanl7lomNgIiHg4iAhHyHh31wdG98d4CGiIt8cnBxeXx+cXd5d29zeoh/jpCOg4yX")
    append("eniQiYJ8ZWCNl5uXj5eUnZKTlJKOnJiPipWhmqegn6OkmpiRkZqXnZaVjpCMi5CBfoiOiH15dYeBg4qId4Z9jomRb3R8d3x6dXNrbG5qbHWCfXx8d3Z1bGxn")
    append("c3d/f3lxeHh2eXFqdIB5b3R1eHN0cXRybHlqgXhxg3B7i4qFiJOamIyVko+LkpyRfmx0b4F5eH6BlYeFfIWMlYWDgYCChX5rbGxpcoF/iISNh3p4g3d8dXR1")
    append("a3FjkY6Dl4+ZkYySeomNgXt/XVqUkY6Sm5WYop2cm5qZlph8dY2Io5uZnp+WhoiJn5qco5qdlZKOi4aLiIyRkYaLjIeIlIWQioZ5hHyEcXaOeHV4bXp2bG1q")
    append("dWR6fnJ6fHt4bmtoeXeBfId+jYd7hnZyhIl5dnp8dX1xeHZ5a3pwhmprc3SFdoyDf4WGj5SVk5CTj56Ih3N2gImFgH92j5WGjZCCh4KPk4eBen5ubGRnc211")
    append("bX6GiIqKjn15b39zdWpsgo6Fh5eMlHCJeZB+f4uQYFRphJKXl5ukq5+WjomYpY5xaWyFk5iXjpB4nZ6Yl5mQkJSSmZGHe3WJhI+Pipt+gG55kIGFloJ8hX11")
    append("dnt7doB4bnh1c2lxdXJ1d3J1cWxucmprcXGDdnd1f4Z4eHRzhnR7e4B1eIB7c3h2fmpyemtvdXR8gH1xe3R5b4eTmJuMkJaJlHV6dX15d4J+lpF7gXp4bnqE")
    append("ioyDg3GEc2FgbGx1eHaLj4aPin+GfYCKhHNycI97jpyIiHp+dn91iIF4Z1Z6j5ebmZ6enJGXlYKUmY2CYmx5gImPoo2FlJuUpKGQgXmJi3t+Znt0fIN4gY2E")
    append("d299iJGKqId5fXCIbXd7dH9ycnBlc3Zqb3Z+fntuZWdubmlpbHl/enJxc3Rva292dXV6e396dnl7fn9ygGl7fnR2eHt4gXZ7e29zb3OBgJ6RlX+DfHt8cHNz")
    append("g4eQk4SDeX1vdnp2coB9gIGCiIB7f4WEfnuNkJSKjIt7hISTg3l7fYuMipqLjIdzjHF9gHdxcnaNkpWUk5OGgmt1mZmYp32Bbml7ipKPlYx5gqSdjY6EfnR4")
    append("jIB5e3d7e359fH2HhXmDg495iH+Fe3SKdG6Aam9oaGxmbXJuenKIfX95bXpwa2xren95gnpzbXBveG5waGx2dXt3eXOGiHZygHp8enF8enl4dnZ8aXZpdXN4")
    append("c4uZmIqJgId4cnBudXd8en+RhXd3eXFxaW50foh6iIyJgpR+e3mRmoqPiX15joeQfoSIiY6XhpOJe29pc3V/eX11aoiPkpaUgXdqZ2Z2mKuhoXVzeXNzkp6X")
    append("j4RyeY+ViYt7dXx8fX18gH+LfH+MfIaUeIWBeXJyc3h+hI6KcGJsZGxzbXNsZGlwfoB8iXx9e3p2enFvbXN0gWp5cXl4b3N4bnF7gIN5gIGNe11keJB5dnWD")
    append("dXNscnp4fXWBdGpndoKPmJabhoSGfn13e3x2dX+JgIWOhn1vbWxvf4mQgYaBj4iJg36UkoWOf36Eh42Ei4CMgYh/eoWPb3JteoV7gHd0bnp7gIOAam9pcnB+")
    append("naqxoIh6dXF0m5qHjoJ7e4iQgI+BfHGKd3d5fpSbjH9/hJCNcZicjYV4eHF/h5KQb2d3b3uOgX14bXNsfoqBh3eFio+KdXuIbnF8hHR5fnuGc355e4GLfHtx")
    append("gXyEdmFjipl6b4SCdXxmb210gXuHin1xdHmEj42RdIOTj4mSf32HfIJ4jZCMgoN3cXKCgn6DfpKJkoaLio+FdoeBgI2De4WNhYV8goN8h4OAjpGAeYaDkI2G")
    append("d4B8f4x+gX9+hYGPj4+ZkYN+c3mJlJGJnYmAf36Hmpeag4B7d36BhKCnjnl6mrORbaSwj5OUinZzjpeecXJ/dY2SiIt/goJ0ioxzgIaQg5uQfYCQd32BhXyJ")
    append("hYKShYZyeH1/f3Rte3yCdWh2npCIf4KAd4p2dmeEhX6BjYt2c252fXt6fXl+jJimgoOTkIt1hH2Tc312dnmHf3+CnqKcnoCPlIuKbXZ8mIZ7inqLjYBwfIx3")
    append("gX6Kop+nnpOFkJqOgYqSioaFlZSVnImVipWXhYCLhpWQho+YiZuCgXpyiIaBgoqDgIKBlpeVioeWpJ+hkJuXnpSjmH2FgH2S")
}.toString()

private val PHOTO_RGB_B64: String = StringBuilder(73728).apply {
    append("f4yWdYKLZ3iPZneGZ3aEZ3aBdoGJYm9+c4CFfIaPd4CFaHSBYm99ZXB8Y298Z3R9ZXF6ZXB4cX6CanV9cnl/XGh2Z3B8c3p/eH5/aHJ6hoqKiZCMcnt9gYl/")
    append("dX17ZW90a3J1d356f4J+eX98X2ZiaW91dnx7b3R5WmNva3JzaW5tZXF6bnZ2eIGCanN2anN5bXR4aXOAWWVseH95Z3FzeH6CZm9zd358c3p7Ymlsc3h7a3N3")
    append("doCAZnBzY291b3l2ZnBrdH59dn93aHJwe4eIf4eFdH18eIKAdH53cXt+dX96dYF8bXVte4Z9aXR0d398dIB+ho6GhIyDZXB1gIh9fIKBcHp3dn6GipKdV2Ru")
    append("gIqGZnN4Z3NsXW5xe4mBeYaEZXV3e4V+k5iPf4mCdoCCipONhI6UcHt0gIqNgomMbHZ+k5ebiI6WgYiQiI+ZgYqVhoqRkJScdn2HgIeSjY+SgIaLiY6Mg4Z/")
    append("ioqEfoJ+bnV0jZOXbXd6XmtsXGppZ3R1W2lqbHl9YnF4bXuAXGxwanl4dIJ/VWRqWmpsfoaDb3x9Y21wW2hqfIV+jJCWb3JyVVxhoZ6aio+OfoODWGFqdXp+")
    append("kZSRa291foWCdX+Je36BeIGRanOCdXp/oJ+Yj5eiopiUoJiUbW9xam1yY2x6YWhucXl9Z2pucnV6dXd8Vl1nZWtyj5CRg4WFfH+EVVxqbXF8dn6MZnF5dHuD")
    append("cHh6g4mHfoeGc3Vzd3pzdn18bnNxXWFlaW94ZG11ZmtwdHlzZXSCcH2GS2B8TWJ3VWd6WGl2Y3B7Tl5tb318ZnB5Z3J8TFtvPlNlVmZ0S1xuTl9wQlFeTlpg")
    append("WGVvUFxoXWdrUF5oU2BpYmlnXGZoS1VZZGtmWWBlXmdrYGhiSVZjRlZbV19fZmttaWxtZGhtT1hVSVFfYGVoZ214WmBjUVlcaGxpWmFfanB0ZWtwUFpfUltg")
    append("Z2xtTFdjUl9hb3ZrVV1ebnNzZGtmaGxrXWNiV15kWGBoW2RtSFRhUVtqRlNfSVFgVWBmTFdfZm9oWmRiXWpoXmdpUl1nanJwXWZnWWRobnZpX2traHJpZXBh")
    append("WWdga3RpXmpeaXJia3VrXmdkXmFTWmBeYGNiQklScnl/UFxiaXNxQ1JYW2dZT2BeVWRZa3drYW9qX2pgb3Zsf4OCXmdraXFwZm5zXGdqaW50c3d9YGp0cHd8")
    append("Zm55h4qQbHR/eX+JanJ7hIWIZmx1cHV8bnZ6ZW9ubHBycXRvgIR4VmBnbXNvYmlqX2dsU1lYQE1TYnBjXGpjQVJZRFVhRldZOEhSN0pXTl5dN0dUSlxda3Nw")
    append("TVtcTFhdTVtccnVqfXt8TFBTOkBHgXtzXF5eT1RcNz9NWV1jgYB6SlBcWGBhZGt0ZWlxWWFxQEhWT1RXdXJoOz9HnIuAfnNtZGJhZGhuRk5bNTxISU5TXF1b")
    append("UFJUZ2dnOz9JNTxId3NubWxsYWNmOD5KR0pVTVJcQ01XRE5cWmJVbG9lYGRYSlFWe3pvW19hX2JeS09RUVdaU1hcSE1RYWdhYHCDbXmIV2qER113TWF4VWZz")
    append("Z3SATWFwa3h6W2h2XGlqP1BjOUxeTF1uRVZlUWFuU2FgVGBbTlxlR1VdXWhmT1xiRVVWX2drQVBXVWBeVFxfU11lXWVqYmlrO0hXO0tUTFdbaGxmW2FgX2Vo")
    append("UlpUQUtYX2RqYWhycXVzTlhgbHF0U1ladHp8Y2p3PktYUV5fZm5vW2ZtbHZuZ3BvUFhcaG1pW2NjWl5iZmxoVl5jSFNiT1llNEJQWGJyPU1fQktgRlRZQE5R")
    append("W2VpXWNnUVxeYGluRlFcZnByUFxlWGNeZ3BnUV5jYGtoX2lcZXJjYWxeYm1ZbndhY3FZaXBaXF9VQklRYWRkNz9KR01TYWxuVmNlSltVZXFnVmRcS1xYTl9V")
    append("YW9jWmRZbXNta3F0X2dhYGhtUVxfRVJcYmhuV19rYWp2aXJ3YWt3j5CRYmt2foOIYGhzc3d9X2hzX2lzWGRtYWprX2RrZm5qWmJiUltedXlwanBxW2BeNDpH")
    append("Q0pOTVtVZXBkRVNXR1dhUmJeSFdfP1BZVmNeOUlVPlFZZG1rRVBVS1hcRFJUbm9oZGRnT1BUSU1UbGdhV1hcUFRcNT1KV1lee3t5PkZUTlZeWGBsYmhzS1Vl")
    append("OUBNO0dBbWtlLzVBkYN3f3RuZmFga2tvOT9MMjlGPUJLV1lVVlhbYGBfQENLNj1DZmVkcW1sZGNlRUlUQUZSQEdSOkNRPEVRUltQbHFaUVdMKDU7c3ZnV1te")
    append("Y2NiUlVUY2lfPUJMR09JZGlrWmuBYnGHUGN+SWB7S194T2Bta3eCTmBvT11kWGduX2xuNEZYOkxfQ1NkS1xjVWRiPEtPS1hTQE9VVWRhZ3J1QExcUVxjV19r")
    append("QVFYVWFeT1lkTlloWGJmbHJ4QE5aQE1aOURSaW9mbnN1UlpbUllePEhUY2loSlNda29xXGNoWWBoPUdTUVpcXGVrQVBbWWRmZ3N+V2NyZG1wXWRtUFheYmhn")
    append("TlZgUVZcbXFxRk5YPUteRVBePkxVVV9vPlBlPEldQ1FaRVRWRlFfWmBnSlVaVV9oTFddYmxzSldZWGNVV2FiUl9bUFxdUVxbVWFYYWxdWWRbYGtaZnJXZGha")
    append("UldVNkFMTFBWQEhTKjI9OURKWmNjYnFqWmdmQlFTQFNSU2BVVWJZaHBrbnRtV2BgZWphY2duV2RhR1RcV2BjXmVqY2x5Z3B8WGJxg4N9cXZ/bnN3dnp7anJ4")
    append("V2JsT1toc3d9ZGxxcHZ2a3FxYmlnZm1icnd1bXFpPkNMJy49PEBJTldXZXBlUl9hT1xcPU5YUV9mMENNU2BdMEFSLUBPVmJgS1dTUF5cWmRdaGhnX19hYmJm")
    append("WVxgdHBlRUtTVFdbQklUQUdReXl5VVlhUFVcWF9qTlRgR09dKzVEPEk7ZmVhJSs6ZmVmhXpyXFdXW1lcMThDJzI+NzxGXV9SSk5TXl1cOj5HREpIZWRlc29q")
    append("eHRxREtVOUNRNj9LOkFOO0NOTFZTY2hYUlZRJjA7W1xaUFNXd3V0T1RPYmRhQ0lNTFBUSE5SXm+EX26FTF96QlhzQlduQFJgV2Z1UGJvS1pfXGtsU2FuNEha")
    append("PlFkUGFtR1dVSlpUOEZOTVlbOkhMTlxdVmNqNkRWWmRtVF5mRlRZWGJnVWBpRlFhXGVqaG92RFFdRVFcO0VUc3d5b3N3WmFmTFNcNkJPWmJgU1pkZGhtYmhu")
    append("V2BsOENTO0VSR05XTFleWmVxW2h6R1VnUFtmWmJoT1lfSFFYRExWW2Bfb21uSFBcR1VmQk1dQE5XTlhpOkpeNkVYOElbSFRdM0BQVl1kPktUVF5pWWJkaXF6")
    append("VF9bUVpeUl1bVWJXSFNVWmRkVWBbUVxbSVRTVV9UVVpSXmFdRktRND1HVVVZTVRdIiw5Mz5HO0FLaHRrUF1YUF5fUWNTZG1saW9pXGdmXmpidXl4XWNnVl5g")
    append("V2JkRlJbYmpnam9yWmRwWmRzUV1renpyZGt1Ympqen57a3J1U19qV2Fma3ByaW9vV11gZWlscXltdntxb3RrRUlLMzlHMDdFPD9JQ0hSWWJhSFVeV2JoNUdQ")
    append("QFFXKz9KQ1RSKTxOKTpKR1hVSVdPU15hZmtqbWppYWBibG1vRklQVldSTVBXY2VnRUpTPkdSZ2dqUVVeUlZdWF5nSlBcPERRJC87Rk1HYWFfJi48QkdQhnlw")
    append("W1RRWFJTLTZBJC46Ji45X2BYQ0dNP0BHNzpDXFxaWFdXbmplhoB6S1FZP0dUNT9MLzlFOURNRE9NV1tSTlNOJDA8PkVKZmdpgH18U1hSWVtbS1FUTVFWR01K")
    append("XG2CYXGGQVVxP1RuQVZmOkxXSlpkXWx7U2JkWWdvRlZnPE9eLkNSPU9ZOUlHTFpWQU5VPUpVPk1PR1VaSFRcLDtPVmBiSFNcTlleTFdhVmJjQ05cW2NrYGhq")
    append("SlVhQU9UR1RZc3h9aG10X2ZjQUlUOERPWFxlTFVdbXF2Y2pxWWFsOkdXMz5OQEdSPEpUWGNoSlptUF1rS1liU1xgU11hQkxVT1ZcSVBRYmJlSlJeQVBbOUdW")
    append("PktWQ01eM0FVNEVZNkdaPUtbOEZRVF5hQk5cTVdkZ21uV2BtVmBiVl9mVWFfUl1bTVhaTllZTVdXU19dRE9GVF1QT1JSZmlgUVdYO0NLV1ZZUVZfJjE9JzM9")
    append("LzRASFBTTlZWU2FXW2dUanRvZW1qUFxZYGxnVF5eVV9jSFVZO0pWO0lURFNUWmNsYmt1Ul5sZW9uk4+Cb3R6X2dvRlFhbnV+R1NjXWtjXWJhZ2ZhP0NKTE9U")
    append("XGhdZmplVFtST1JVMjhFKzRDOj5ILzdFTVVVV2NbUlplM0ZNOUlQKDxKPlBNLD1MLD9LPU5MQU9NSVRaam1ud3JwVVZZZ2hrMjdBSk9NR0xPV1pdUVRaNj9N")
    append("Xl5iU1ddYWNnS1FcPUZTNz5JJzI8OUFFT1NVJCw5Nj9Ldmxmd2xjV1NTMDhEJy47JC04aWlfO0FINjlCOj1EenZsSUpNcm1ognx3T1NaQ0hSOkRQMTxHMj5I")
    append("RU9HXF9WVVpVLjlBO0ZLaWptbG1oYmViX2NdRkpSREdMUlhQWGp9VGV6P1JsOU1lRVpkS1tjR1ZgXmt3T15dS1phPExbLT9LNUlWQVJYS1lUQ1BVRVJVM0FO")
    append("MUFMTVphNUFPJzlLVl9gQEpYT1liPktST1xgO0ZTV2BmVV1eRlBeQktVW2ZkdHp3am9zVl9eO0NNNkFNYmZsSlVcd3t8ZGpqYmhwPUhXMD1NO0VPMT1LOUdO")
    append("R1dkbXiBRVNdQ01VT1hbPEdTW2FmPEZPV1tfSVNfR1VeM0FRNUNQQktaNUJTRFNeM0RVO0pYNkRQVF1hQk9gUV1nZ2xwTFVXSlRTTllaT1xgT1ldTFlRR1NN")
    append("TllQTVZOLjk8RU5KS01NX2JhXWFjPkdNTlBUUFVbLDdEIC44Jis2Iiw7QUZOQU5CVFpWVGBTYGplWGFkYWlpSldWUV9eWGJnQFFXQFBYVmJfVF1kVmJwQU9c")
    append("aXJsf310X2hzQk1bUVtpXGVvO0paUmBbcnNucXBsSEpQPkNLVmBVXmFcNz1DXV9fMTZDKDE/NTlCLDNCRktLSE1OT1dhO01UQ1JWIjVGTFtaLDxNL0NJLD5G")
    append("QE5PTFpTY2RkcW5uUVNXZWdrKTA9SExOU1hSUFRVW11cLTRDYWFkWl1jZWhsPENOMTtIOT9IKTQ8PURIT1RXKDA8LThFZmFfenFlU1RVKzM/KjE+Iy46b25q")
    append("OkFHOz9JPkFGeHdqLDI9eHFsfnl0SU5VP0hSNT5JMjtHMz1Ia25faGlnYmRiQklOMj5ETVFXbm9oZ2hpY2VhTVFYR0lNSU5PTF1vT15xOk1mN0xiPlNcV2Vw")
    append("RlRgXWxpWWdrO0xUPk1VNkhOP1JdVGNbQlFZMEBNVF5kP0tXOUdPU15kLjpKLj9QUFlfRE5aSlZdTltbO0ZSO0ZSYGlrYWlwQUtaQEdUX2ZsX2dia29vUVpc")
    append("ND1JQkxVZWpqT1hcd3x6XmVpWl9nOEJQMD5OQE5XKjZGOEZQOkhSX25xTVlfQUxUX2hoOENPUVlhMz9JT1daSVJgQk9cNUFROUZPQk1dNkZNWWRvM0NVPUxZ")
    append("OkZVTlZcQU1aT1lfV1xeQUlOOUJBRU9MP0pJT1hTTVdMUltTU1tWO0NDO0JDUl1PUlNPWVxcXF9lLDc8S1BQSE5TNkJOIjA7Iys3Hyo5PUJLLDg3Q0hLR09P")
    append("aHBpWmVmZGprWmRnVmRgWWNoRVRcP1BSVF1fXGJtW2VyR1VdbXNvdHZyTFlpVFxlTllmbHRwRVJUQ05SX2Fca2llRUhPMTpHPkVEW19aOD9CVFhYMTVBLjVD")
    append("NTpGKTFAND1HP0NLUVZdSlhcTVxWIzRCQFBULD5ON0lNJzpEQFNRVGFbVVZYc29uVVRVbm9xO0BKQ0ZOXmBbam1mZ2hnLTVEUVVeWl5mZmpvTVBWMDlGMDhE")
    append("MzxDS1JRTVNXLTQ+JTA8bGdneHFlRUdJKTA6KDA8Hio1d3V1MDhANz1FQkVIgH5zLDI7b2pmd3JuTFBXOUNPNz9JKzI+MDtGbW5obW1lV1hYSE1RO0NLSU5S")
    append("cXBvbW1tY2RlWVtfUFJUTVNPS1trUV90O09nOk9kPFFaUmBuRlZfSVpWWmdxN0lTQ1JaRlVbNkdTVmRZOkhVMUBMVV9oSFJeOkhOS1ZfMT1PNEFSRk9cQUpW")
    append("UV5fTVpaOkVOOkdOYGdrZW13PEZUOkNRV2BoTlhZaG5qV15gMzxJPEhSZW1kZWpvXWZoY2pxVlthOEFPLTpKQU5ZKDZGN0VQLzxKR1RbSFVYQUtPXGVhPEZP")
    append("RE9cQExNQk5RVV5pOUVUP0tXOUZRQ1BfO0pQU19tMEBRM0JRPEZRUVlVNkBESlFRS1FXNjxFP0ZJPERGNkFEUlpXR05MVV1TRUpPMTpCSE9PRVJKWlpWX19c")
    append("V1hbOENEVFtaR0xQQElTJTA8Hio2FiIwOD1HHisxSk5QKzM/QUhPUFhaa3FwUl1jbXN1TlxZUV9kN0dNVl5fVV1oV2NkOkdUWmJobnBvSlZlZGprWWNqYWtk")
    append("RVRQbW1qVFZPcm9sS09WKTI+KDE5V1pZPENDSEtRNDhENj1KNDtJKTA/JzNCRkhSQ0dSPEZPTlhTKTlCN0dMMkJPXWdrNUdGMkZGVVxZWVhagHl2X11aY2Rm")
    append("TlFZQkZPZmVibm9vZGZqJCw8R01ZZmhvam92T1RdLTVCMDZCPEVGUFVUO0JKKC86GycyODxEdHBkYl1aJC43JC86Hys2YmFhNDlBNTxEWFtQY2VcLTU8UFBT")
    append("fnlzW15jNjxGOkJMJC87KDU/aGlkbm5pSUtOUllQPEFHPEFGYmVfY2RnY2RlVllWVlpXOz9ETV1vSVlvRllwPVFjQVNZRVViPU5YQ1JVVGFpM0VQT11iSVZh")
    append("MkROTVpYPkxYPEpSVmBqVF1mO0lMPEhUM0FQOkZXP0lYUFhiVWBkPkpSSVRWNkRLUlteZGtvOURSMj5OTlZeR09XY2hlRUtUMDpHNEFNbXNvdn18VF1iZmtw")
    append("UVdeNkBPMDxLNUFOLDpJQlBaLjxMOEJQOkdMMDlFT1pVSlNVRk9dUltfNkJLTVVfMTxLP0lVQExYRlJhUVtkT1xmQlFXLTxJQUlOP0ZGKDI/R05NUFRbMzhB")
    append("RUtSLjU+Q0xOS1RQW2JYSVJJQEZLKTI8R1BJQktLWFxZXl5bUFFRPkJGSk9TQkhNQEhRKTJAHyw5FSEwMTlEJTE3PENJKC87MThEMzpFXWJkQE9QVWFbVV9i")
    append("PExSNUZJS1dZYWdwT11gR1JeZGxmbG5vQ05aWGRhP01TVF5jW2ZVY2NkaGdcbWtoSk5VJS05KjRAODxER0hLNTlFKjE+OD5LLjhGKTA+Gyk4QUdTMThFP0hQ")
    append("PkRLNkJNOElRP01Uen2BNUVEN0NIT1JVW1xfjYaAYmBcV1ZWVFVaQkZOWVhZdXZ4XF1gIy0+VFdgWl1kZ212TFBZLzZCLzVAPUdES09VNT5JJy44HSs3QkZK")
    append("WF5SYl9eQUdOIi02GysvfHlxOD5FMjlBYWVSfn90Mjk+RUdMeHVxV1hdMjdBND1JJjJANkJGbGtpcnFxSUxLXV9ZQEdIND5AXGVWYGFkZWZiV1xWXWJcPUBH")
    append("TFxvQVJnRFdrNktaTV5gPlBUUmJcSVhWOklRT19bR1ZZQU9VPVBQRFBSQExVO0hRTldeT1hiVGFgO0hTMUFNN0VVR1FdWF9oU11lPkhVR1RUNUNRUVtgXmRq")
    append("NkFQND1NUlheSFFaXmRjP0ZSLTlIQU9PXGJnYmpjRE5aX2RnTlNcOURTND9NPkhUMD1MQk1ZKzpJMTxKKTdDLjhGRE1TQktSPEZVR05aMD1GUltgNEBOSlNe")
    append("QUtXPUpZTllZR1NRQEtJO0dHQ0pQQUdGKjRBRk9LSU5VPURNPkVJLjU9SU9QQ0xJZWdeLTY8O0FFLjU8Rk9FU1ZTX2NkXFxbQ0hIRUdKSktPQ0hOQkdQKDI/")
    append("JDE9EyAvJjA9KzQ6QEVMJC07MzlFMzlELTVBX2dmUF1TV19kRFNVKz1GT2FZXGJrWGVpSlVhYGtiUVhiR1ZaWmZeTVtWR1JZU1dRXV1ecnJrdHBsV1hdKzM9")
    append("LTZDKDA+QEJJMDZCKzI+LzRBMjlFLTRDFyU1NT5MLzZEKjRDLjVCLDZGKjtJUVxahYWGV15bfnlxUFNXW1xecW1oVFZSTExNYWBiTFBUVldaXV9jVlhbHSc2")
    append("R0tWUFJZXWFoUlVcPEJJMjdDOkNCSUtULTZCLDI7GSYvVltWOkQ+P0VFVVZZNDtBLz48dnNsNj5AMDc6am5ghoaBNDxAPkNKcnBuVVZaLjVDNkFNJjJAQ01G")
    append("XF1eY2NjV1hWT1FQWFtYLDg+UVZVW1tcYGJeT1NTSU1PRklSTFtuSVxwPlJlPVBYRldeTFxbXWtkU2BbRFVUPU5RN0VORlRWOEdPQ09SPEhQOUdNSVVTSFJa")
    append("X2luPktUMT9ONEJTTlhiW2VnX2hvPkpRSldcM0BMUVxgVVtlNT5MNj9OY2lrM0RGRE1PQUhVM0FOTFpQWWBjc3h2PkdTYmhtUVZdND9OLztJQElVLjpJQ0xZ")
    append("JjRDKzVEHi4+KzRDOUNPQUpVNkJOR09bQE5MVl9mQk5WR1FbOERRS1RZOEFHSFNNIS03KjY8QEdNSU1MKjI9SlBQQkdMNj1GREpHPkRIRUtQPkVKYWFgNT5C")
    append("QkZLPEJFP0ZGUlNUcHFxXl9eNjw9SUtPT01RWlxfSU1VLDVBKDI/DBspIi07OEBEREhLIi89LTRBNjtFHyg1UFZaUlhZWWBeUF5XNkhLT15YT1piSlZjOUZT")
    append("VGJha3B0OktQUmBeWmlfWWFcUlVTUFRUfnx2aGdnX19kLDI8LDM/ISs6NTpENj1FMDZBMDZDNjpEKDA/FyU1Mz1LNjxIJzJBMzhEKTFAMDtIVGBSYGBdYGBg")
    append("Z2NgR0tQYmJjZGJdV1dVZmJgY2FkUFJVVFVZUlVZV1hcGyQzPkNPREdPZmhuUFRYSFBPLDM/PUhAUFRUKjM+MjdAGycwUVhQRk5POkVAP0dBNT46OEU5dnRr")
    append("Lzg5OkQ/fX9veXp5LjY8KTA6eHVyWlxVLzhGNj5JJDA+TVVKamlmXF5hXV9cZGZbVFdZMDpFOkBIcXFqSk1ORklNRUlQUFReUmByOk1iP1FiQFJUMEJPRlRZ")
    append("RVJdUF1YT19UOkpSMEBHSVhbN0dVR1RWPU1PMD9HRFJNRVFTXGVnO0lOMD9NLj5OS1hgYmxpUFtnSlZWP0tVNkRLRFFYVlxlOkFNMDpHZGpqOkdIQElKO0VS")
    append("OEVMU11bYGRjbHJtS1JdTVhWTlRaMj5NLjtKOURPLDdGQEpXKTdFKzVFHCw9LjVEN0JKP0hRLzxHRlFXRlFXT1lkRFFTTVhYQUtMU1pTNT9HQ05JJDA8MT0/")
    append("PURIQkhGJS06RUtQSkxPLDM9SU1OO0FJQ0lRMThBWVpeOkNDNDlAQUZJOT5DRUlMaGpqRUdJOj5DSE9QS01QbW1vRk5YLjVBLjRACxknISw6ND9DSUlHJjE/")
    append("MzpFNTtGHyg2MTlAQUdNSExQQUlLNkBGSlpVUV5bQ1BfSVZmUmFadXp4NUZKU2BbUF1dampjW1xXUlVUdnRuY2RmUlNaMTU9MThCHSk3MDU/P0hKOkFELzZC")
    append("NzpDKjNBGSU1MDpIMThGJjBAOD1IKjE/Ji89UV1LQkRKV1VaUlNWO0BHW1xfamdkbWhkcm1qZWRoS09WUFJWTE5RUlRZGyU0PUJNR0pSZWlvT1VWR05PKDA8")
    append("PUk9TE5MLjU/MjdAJjA6aWxlTFZTMj4+TFZLLjY5Pko6dnVqLjQ7NkY6hIN4bG1uLTU8Iy03a2tpaWldLTVBOT9JIi88YmdeaWllUlZbamtocHBnXl5cMTlF")
    append("NDpDYWFfREZKTE9TTE5VVFdeRlVnOUlbO01YRVZTOktQTlpdMT5KQk9UWGRqTFldQ1JSUV9eMkJRSldYP01PKzpFP0xKU11YRVFRNUJKMD9MMUJRQE5XXWdn")
    append("RlJcSFJaO0ZROkhPRlNfRkxYNz9LOkVOZWxsSVReO0dKMj1IQlBMUVpeU1tWYGZjQ0pXWWFfU1phN0JRLDlIOEFMLjlIPEdULTpIKjZEGSk4KDFAQk5NQEpS")
    append("LztHV2FiND9NUVthT1lbP0pIN0FCRkxNLjhBNT5FKDRAPkpHREtLND0+Jy87NjxEUU9QLTM+SU9UMTpEPUJKOD1GZ2dqTlJSNTpBOD1DQUVKPUJHYmNkQkRF")
    append("QEFHYWdjPEJIcHFyOUFLJS04KzE8DRkmKDE+Mz1DUk9PMDlGNDpFLTZCISo3MDlASU5PNzxDMjlFLTVARlJORlRTNUNSQE9eS1pWT1tdRlVOUFpWS1JQZmdf")
    append("UFJPT1JUfXhvXF5gR0pRMzY9OT1GISs4LDI+MTxDRUxKLTRAMzhCKzVDHCc3LzdEMDdDJS8+Oj5IJi08Hyk3PklIQUNNQkJJXl1dP0NJR0lOdHJwbmpnX11d")
    append("SEpNTVBYXV1gU1VaVVlYGiQ0Oj5KUlJXVVlfSlNTREpOKDE9RVBGUlNXNDpCLjQ8OD9FX2FeOEM+KzU6OURARkpMRVM+XV5WKjA5PEo8dXVuY2RiLTg/ICs2")
    append("WFxcZWJgMjdBNj9IHyw4a21qWVxYWVxeYGFiZmdcXmBWOUBKMDg+XF1gS05MTlJUT1BWXGJaSllhQE5cQVJZSVhXPE5PTVdeOkZKQ1FWRlVkTFhhTlpfS1Vc")
    append("PkxaRFBVQk9ZKzlGS1ZVS1VVRE9RPEdRN0VOKTpLQU1VaXBuSVZWR1BdPUhUO0hVPElTQ0lUOT9MQkxSYGhqPUhWKDdAKTZCQ1BQUVlcW2Rdam9zPERNWF9k")
    append("VVtkLzlHLDlKQ0hSKjVEPEZTLTlHJTNBGik4KjVFQktSQElTMT1LUVpfQk5ORE9PSVJSOkNGMDtBQklKMDlAN0BGMjtFRUtOUlRTJC41LjVAP0NLUE9SKC86")
    append("SE5SMThBSUlNRklPZ2hoV1lZLTE5P0NGTFBRPEJEXF1cQUNGSkpOYWJeNT5FU1VXNjxFISgzLDE6EBwpKzI+MTpCRkVMMzpFKTE8JzA9HCc2KzU7R0tMNDlB")
    append("MTlFLTVBKDE+Q05QQE5YPk5SWGZfUF5aU2BYQ0lFNj5DbHBiTU9QUlZZe3VrXV9jR0lQPDxAQkNHISs3JS87LDZAOkNGNTtFLzZBLDVCHik3LjZCMDVAKjRD")
    append("MjdCISo5HSc2MTtEWlxiKi86Xl1dUFJVSUtQendzcGxqYV5eQkRERUlRbGptWVxeVVlRGCMyNTtIQ0dOXmFiU11bNzxDJCw4TVZOTE9VKS86KC44T1NRSU1Q")
    append("Q0pLNUE8IC4wNjw7RlU+XWBWLDM3JjM2dXVrS05PKjU5GiUxO0NDZ2ZeOj9GLzpFKDU7X15dWlxYV1pdbm1ramthTVRJSE5QNj8/TFBSV1tTYmRjVVpYSFJF")
    append("NUlWP05XSlthS1lbMkJKUltkSlVPSFdRNEFQTVplRVJYPEdUNkRUSFJZNUFOLD1HP05NT1pUQEtURFFaQlFNIjRETllhcXZ4S1dZVV5pPEhVNEFROkhNO0JO")
    append("O0FNQ05RV2BkN0FPJzRCJzNANEFGSFJUWWJdUllcP0hOTFNYWmBoKDA+KDVEQUdSLTdFO0NPLDdEKTZDHSs6KTVFNj9ONz1IOURLS1RTQUpHO0JJQkpLLztB")
    append("OkRDTFNLLjY9O0NKLzY/PENIV1ZVKzQ+LzZBNTtGUFJXJy45TlFVODtBUU9QUFFRXl9dWFpaJy03PD9DSU5QTlNMVFVUOj5CUlBQX19cMzpBYV9ZJiw0Iys2")
    append("NDY+FCAuLzM9KDA4Q0NKKi87Ji46JzE9ICs4LDU7REZKPEBHMThEKjI/LjZCMztFSVNUP1FSZXBpSldYU1hWRktFJC08aW1iYmNiU1dbf3ltbGpnSkpOPDxA")
    append("RENGIio1HSk1JzE8N0JDLzQ/LTRAMTdCHio3JjA8NDZAKTJBLTQ/Hyg4HCY1JC49UlVbIyk2V1lcXFxdUFFUenZyend2W1lYWVdVQ0pMZ2dqVllYUVVTFiIx")
    append("MjlFOkJOSlFJXWZVODxEJSw4T1ZRQEVNJy86JCozY2ZjQEZGQ0lLO0hALz03NTxAV2FDa2xfO0JBIS01dnZnRUhLIyw0GycyPkpCVFhXUlhXPkdNLz02Xl9e")
    append("T1VSX19gampkc3NtTFJHQkdFSFJKSFFHSFJHXGBeY2leWV1YOUtcSVZfPU1XRVFYPUtPVV1mSFFWWGVkPkxRUV5eOUZLM0JQKTZHSlNbOUNPM0VGPk1JWGJZ")
    append("S1ZYSldbRVRKJjZBT1plbHRxTVZbTFZhQ01aNkJRNEJMMzxKMTtHP0xLXGRlMj1KKzhFJTE+M0VCTllTUVpWT1VWPUdKTFNZVl1lKTJAJDFBOUBMKjM/OkNQ")
    append("Mz5LKTVDGik4JjNDLjpIQEZPUVpUUFlXOUJGQkdPMjpAN0NDO0NETVJPNz9GQkhNNDpDNT5CW1lXPEVMKDE9MjlEW1peJi04SkpQOjxDRUVJWVlUWltbY2Vl")
    append("KC46PEFGSk9QSk5OVVZYOD5DPj9FVlZVMzlAXVlTJCs1Ji47MzdAGiMwKCw2JSw0S0tLLzI9Ji45KjRAKTE9MTg/P0FIQ0VMNz1HLjZCLTM/KzI+MTpDPU5P")
    append("ZXBsT1tWSU5PR0pLHSc0c3RsW1xcVFdbb2tgcG5nV1dZLzM6QEBDJSw4Iys4KTRAMj49KDA7KjA7ODtEISw6Hig1Ki46KDFAMTdCHyc2FiEwIyw7REdNKC46")
    append("UlVaZmVlTk9RbmxqdnJuYmFgb2llSU5OYl9fb3FmTU9UFCEvLTVCOkVSQktHY2tWNDhAJCs3UVhTMjdBKjE6NTo7cHNsUldSPEVEMz09NEA6LjU7XGFOdnhk")
    append("UlVUGSUveHZyRElKISozGSUwR1FLTFBVQUpNVlpXTVZMX2NfMz4+YmVibnBjX2JWV1pWUVVVP0ZHQkpCTVdLYWZfWF9VVltXOklcQ09hNEJQO0dVRlZNWV9o")
    append("VF1oRlFcOEZMTllWJjM+MT9LKDZHQUlUOkRQP1BMP0tKV2NaT1lXQU1TQVBHLz5FQE9TVWJbRE5UVmBhN0BNNUBON0RQKDNAMkFFSlVUXmRnNT9MLzlGJDA8")
    append("PFBGUltXVFtdUlZcRk5SRE1WPUZQJzE+HSs7OUBLLDZEOURRN0FPKTRCGCc3JjJCLzxLSlBWQUtKO0JLQk1IREtPMj0/N0BDNj9GQkhMNj5EOkFINTtERUpM")
    append("YV9dNj1FJC47NjtEV1ZYJSw3S0tQMTZAO0FKYGFcW1xcV1xZJis1Oj9DRkxIRUpMW1tdRUdLMzY/VlRSPD9DSExOMDU8JS47KS86GyQxJCo1Ji41W2JXMTU9")
    append("Iyw3LDZCLzM+NDpAQD9FS0pPNTpEMDlENTc/LjQ+JTA7NUFGW2ZcXmNaXFxbPT9FICkzbG1jYmFgT1JWXmRVcW5oWFpeKi85QUBBJi05GiQyJzM+Lzo8ICo1")
    append("LzQ/NztGICw5HCc0Jy05JjA/NDhBJCs4FR8uJCw7NjtDOD1HTVBWX1xbVFRVZGJfZmNfZGNieXJtWVpcXltYcXNmQERKGyYzKTI+NT9MLzpAX2RYNThAIyo1")
    append("QEpEKC85Ji82P0VEZWdiVllWPUZDMDs+Kzg2MDs8W2JQY2pSTFBMGycwZ2dlR0xJICozHSgxUVVXTVBUMj1FT1NPTVNNSlJNPkdES1FPY2hdaGxbWFtaU1lT")
    append("KDA4OD88S1BRO0VBTVZHSVBKNURXOEZYSFZdNUFQUWBYSlFcRk5bQEpYPUdVQElUMT1JNUNNLDtKR1FVQk1RRlZQQU5KVmNVR1FVP0tSTFtOOklKO0xKOkhM")
    append("T1xZUFlbMTtHND9KOENLKTNCOEVNSFFWYWhtMz1HNTxJJC47O0lFUllcXGJjUlZbQUpSRU5WQUZPLjdEHCk5NTtHKzZELztJNUBOKDJAGSk5KDJBLTpISlBV")
    append("NkBFPUNIQUhOP0dLPUZINkBDLzc7R0pNMzxDMjc/NztFSkxRW1pbSEhKHik2NTtFVVNUJCw3VFVWMTdBPUROY2NhWltaVVtOKzE6PkFGQkdHS09NWlpZVFNR")
    append("Ki86WlVUP0JHSk5SODpAHig0KCw1FB4rIyo2LTU6TlVQSEpMJi86KTM+KC87PUFENzg9TklKLjA5MjlENTc+MTM8Iy45KjI9TlhJaWpoaWViSUlOISs1Y2Vi")
    append("Z2RjTVJWWmJNfHl1WVhZNDc+SEZEKS87GCQwJDA7MjtALjo+KzA7LDRAICs4HSg1Ki87Hyo4NzhBKjA7FSAvJC08KzA7UVJUS01UV1NTYF9eamZhZGFda2ll")
    append("gnpzW1teYWdTgHtyWFleHygzJi88MjtGMT5BWVxYNDc/ICYwSVJKIio0JTA2R05QY2NiRUxKOkVDNEE6LDk2OUc4WF5MUlhLQEhAFyMsVVlQVVhUJC01Iy8z")
    append("WV5fTlNVNUBDRktLV15WR0xLS09PSk9JW2NWcHNmUFZTV2FVNjo/OT4/YGJjMz06SlJOUVdRMEBRLT1OPEtWMT1LS1ZYQEZTN0BOO0NTRU9aPERTMz9MOkhP")
    append("JDRDRFJTQ1FMNENGSVlJSlVNSVNUSFZORVNMO0pIKDc/NEFMRlNUOkNQKzRBN0JLLDhDLThIPEZSTFdUXmVlMDpDMTpILTdDO0pKSVJQW2NgUldZP0lPREtT")
    append("Oz9IMjlFHCo5MjhDKTRCKjVCND5MJTA9HSs6JzJCKTZFRUxPPkdLOkRFQkpPMjtCPUJFQkxKQElCTlBTMzpDODpAPkFJQUVLWFdXTk1OISw4MTlEWVhYMzlA")
    append("VFhRMTc/PUBIXmBfXl9fYWJeKTE+OT1EPkFGVlhTX15aT1BLJSwzV1RTNjtAS01PLzQ6GSQwJyozFh8uJSs3NTxAUVNVOUFHMDc/Hys0Iy44PURJPD5EV09N")
    append("Ojk9NztELzQ9MDM9JSw4Ji06OUU/SUtOZWJhQkVNIyw2WlxcX11dS05QYGdUbWtpT01OOjtASkhIJy45FSEsIi44NjtCLzk9JCs3Jy87ISs4ICo4KS46GiQy")
    append("OTpCKzA7FB8uICk5Jy06YWFaYWBiS0hJY2FecWxnVlVVaGRfZmFeYV9fWWFNkoqCamdnICk0Iiw6LzhESFFIYWVZMTY9Ji04P0pEHScxEyAnUFVVbWprS1FN")
    append("PEVHNkE9LTs1PUo8bnRdTlJKMDc3IS00U1dSVVZXICoxHSowTldQPkRGTVRIQERIY2VdSUxGRklMLjs6ZGteXl9ZTVFNP0lCWVpaQkVGTExNNj02MztERklL")
    append("KzxNMUBPOEdTOUZRNT9JREpUMjtJND5LRlBYQUhTMDxLOEVMITBBOUdNOkVJKjs/SVpJS1VURVFRQU1LPUhLNEFEOkdHOENPNkFNN0BOLDVDPEdOO0lJKjhB")
    append("P0tRS1ZQSVFULzlEMTpILTdCQVFPQEpIVFxaW2FjR1JOP0ZPMzpFMDhEHCk4LjVAKzVDLzdDMTpFJC87HSo5IjBAKjdESVJKOUJGPEZLPUdIMDk+P0RGQktM")
    append("PkhGTU9SNzxFOTpAPD5FQ0VIVlVVT09RLzhDLTZCUFBTNDk/UldQMThAQkNIaWpmYWJiX15eJS06NTpCPUBDS09LUlJQUVRMHywuSUtMP0NBREdIMjQ5HiUw")
    append("IiYvFyEuJCs3Nz1DPUFGMDlDLjM6JC85IS40TVZRQkNJW1NRS0hGODpDLDVAMzlCJi46ISo3Lzs9SkxQaWhmT1FXICkzbWxoVVdTUVJVZGhXXlxaS0pNOzxA")
    append("RkZJJCs3ER4qISs1QERGKzQ6Iis2LTI+JS05HCc0Jyw4HSc1NTc/LjI9FB4tHic2Ji05TVBMcW5sVFBQWVhXaGRhT1BSbmhjY2BeZmVmWlxXlIl/YF1WGSQu")
    append("Iiw6Iy87VV5NYGdRKDA2KDI+SlVIIy0zLjs5YGJhWVpcXF1fPkNJQ1JCMTw8MT04bnVdWV1OOT89MDw2XF9XVlhVJS4xJjM0UFdONz1EVFdXR01HV1pST1NH")
    append("OkFBLTo8WmFRaGZiSUlFLDsxQUVIT09PTk9HN0A6Ji02Oj9EMUBSMkJQQU5UMj5MOEJMQkpUNj9NRU1YO0ZTQEtQOkZTLThEJTRDNkFLMjxEN0tERlZNQk1Q")
    append("T11RPEZHNkFHMT5DMj9ELzxIMT1IMj1IMTxGP0pON0RGLjxDN0VHRExQS1NVMTtGMz1KMz1GQ1NJP0hLQkxLS1NUND9AOD9HMTlEMDdCHSk4NDhCKDI/LDVB")
    append("OD5IJTE+GSg3IC9AOkRMOEJDNj5GOEJHNT5ENj1CO0JFOkRDSVJRS01PODxGPD1DODpCVFJQVlRUVFNVLzdCJzI/PkFILDI7UFZWLTU+SEhLaWlhUVJUUE9P")
    append("Jy87MjlCQkZFO0BAWllWTU9OFSMrPUVESE5HNDpAMzM3Jiw2HyYwGiUxICk0KTM5Oj9FLDM+ODk/Mj1FHywxRE1IODpBS0hJS0pINDpEN0BMMTpDKDE+JC05")
    append("HSo3NzxFZGRjWVldJCw3amlmXGBXW1xeYmVVXVlVSElPOTo9PkBGJi05ER8rIy82PUREOkQ/JjE6KTI+KjI9HSc0Jis4Hyk3MTM7Ky86ER0sGSIxIig1UlVX")
    append("gX15WlVVVVVUcGxoR0pQcWxnc25rbWxsUVJRjoJ2YFxVJzE5EiIuGCgxT1tBV15OKDI5ISs4PEJFJi82O0ZBY2RlNjxATE5POD5BQE1DJC4xJTIuZW1XYGNY")
    append("NT1AM0A5aGhkRUlLTlZLQEZDWV5USE9KQ0pKYWdYR0xASk9GPEZBLDY9VlxMS01LTk9JPks/MTc6RUs/TlRCWl1cJyw0OD5CMUBSKjtKQk5UMT1KQkxXPklV")
    append("PEZTTVRfMDxLO0dMPEdRLDhEJDNBPkdQO0RJPk9EKTk9SlhPUltYSFVNNEBGMD1DOUZKLj5DLz1AMkFBOkdLND5HMDtEJTI8Q1FKPUVMUlxWOUJMLjlJNkBJ")
    append("PkxHSFBVP0lHOUJHLjdAPEFJMzxILjU/Hyk4LzVBJjA9JTI+KzZCJzI/Gyw6HS49Rk5PNT1DNj5GQ0tLLjc/PkVJLTg+PEZDOEJGTlBSNTtFP0BFP0JJTk1O")
    append("UVFTVFRWLTQ9IS06MjhDLjQ+S1FSPENJS01OZGVdWFdWREVGMjhDMDhAMjk5RUlJUFNPSkxPJy81MDo7R05MKTA5OzxCLDE7GyQvGiUwHik0IS4xMTc/PkNE")
    append("SkxJOUI/Iyw1LTQ9LzI6UEtMTUtIKjM+OUJPOz1CKDE9JCs4GCQxMzlCZmVjWVpbKzI2ZmdhYWNfYWJjcG9ldW5mRUZLPDs8OjxDKC44FyIvIi00ND5AQU1B")
    append("Mz5AHys3KC85FSMwIig2Hik3MDM6ISg0DRkoExwrJCk2S05TioV/WFNTZGFebWhhSkpMWFZWg397a2loQENBhnptaWNbQkdKECItKTs3TVc/UVZQIis2JS44")
    append("PkJIIy0zQkpHVFZcRUlNREdHTlNPP0dEMDs4JzUxYmtPRkxGREhMKDU4U1VUQUZIWmBZUlVXWVtcUldRLTg4bnBiW11NVFdLLTg6JjE5XmNbSUtLUFVKOkRD")
    append("OT5BSlQ8UVVJQUVOOj9GNz5ELz5QLj9KOURQMz9KTFZgPUhSOUNOT1ZhKTZBOkVONEBKOERPKjtELzxGOkVKPExFNERCR1ZMU1peSlpMLjtCIjA7OEZLN0ZG")
    append("LTw+Pk9GMj9ENUNGKDQ/Lj09SlZQSFJNTFdTOkFLKzZGO0VNQ09OU1pfQ01KMjtELzhCNjtEOUFNMTlEHyo4JzA9Iio3Hy04LDdEIy89HS06Gio4SU9MLjc6")
    append("OEJENz9DMjtBOkBEMDo/OUFDOkQ/UFBTNjtGSEhLPD5GS0xOTlBSTExNLjU+Gig0LzZCNDhASE1PQ0lQQ0hEaGdgX15cQkRHOD1GN0BAIyoySU5PNz49TE1O")
    append("Mzg+JjA3NDs8LDI5NTY+Iik1GiMvEx8rHig0Mjw/JCw2OD4/QUhCMjk6KC87KzE8PT5EZ2BeXFlXJTA8Nz5LPTxCLDRAIio3FCAuLjM9XV1eTk9QREpEZmli")
    append("XV9aVlhSe3dwbWdiRkZKPz4+Oz1CKC02GSIvIi01NT1CO0VFMDY8HCg1Ji02GSYzHiY0HCc1KzA5HiYyDRknERopLTI8VFVYhIF6SklKYFxYY2BWUk9NcGlj")
    append("f3p1YGFjR0pDc2pjc2piKzI4FyYvM0I1VFtIT1VLJjA5HisuO0BILDw1Z2pjNjpBTVFVRUlQU1lSNzw/OEU6MDw0cXZdPEJCTE5JIi00XWBcQEZEP0ZGSU5J")
    append("U1VaQ0hKLTY3Z2degX94XF9RJTA1IzAzcHJmTE9QSlBHPEJHN0A6SExHZmdgOT1FNj1ILjlDLT1ON0dNNkBPPUpRSFVbR1JYOUJLR09VPEtIOkZQKjlFM0BM")
    append("JzpAKTlBQE9QPk9HNEZBOUhDUFhYQ1BMKTY/IC88LDpDKDY+Hy06Kzw+N0ZIPk5IPEpDMkE/R1FRSVNMTldVMThCLTZEQUtPRlFSXWNnQUhLNz9JO0BKNz1I")
    append("RUlQJjQ9Gyg2JC06JSw3JjE9LThGJTE/HCo4Fyc2Sk5PJzE4NkFBQktNOkNGQUhHPEVHLzY9PURDQkRKOj5IRkhOOj1FSExKSEpNWlRRMThAFyYxKTI/OjxB")
    append("SU1OQkZNOUE/amhkYWFgNjtCMDQ9QEdDJSw1RkpMNzs/Oz5GP0JINDxBPURBJi42NTY+HSYyHiYyFSAsHCQvMjk/JS44NDlDOkNBNjs/KC45Nz1HOz5FV1VU")
    append("XV1aIi49MThDPD9HKzI/ISo3FiEwNDhCV1dYS01NMjk+Wl5VX2JbWVhMiYB3bWdgQkRINDU4ODk9Iyo0FiEtJC42OT9EQ0hOKi41GCQxIio0Hys3GiIwGiUz")
    append("Ji44JCs3EBsqEhwrLDI7ZWNgdHJtSEdKZWJbU1NOTk1NdW1lfnp2YF9fVFVIjH9zcmlgQUJEGSgxV2FLUV9DVFpKIi00HiwyNDxCQlI7XF1ZQEVCU1dWTFBU")
    append("VFpNNDw+Rk9ELzwvYWJWPUNCSE9BIi4yYmhVOD4/OkBCOUI+V1xVPUFIP0RDYmRac3RrZmNWJjE3JjUzam1dTlVJPUNCLjM6U1tHXGJVaGtdU1RXJzE9Iy45")
    append("Lj1OKzpJN0NQOkhQTFxWPkhSN0FJRlJRSFdZQEpVMD1LKzlGHDA6Kj1ER1dOPk5CNEVEQ1RHTVtRMT5HKDc/KztDKzhCLjtDKDZAQVNFNkVFOEVIOEVBMT89")
    append("RVBRQ1FJPUZHNDxELzhGR1NOQUpLU1ldQUdOOUFMNDpENj1IQUlLKzhAGSc1JCw4Ji04LDZDMjtHJzJAHSk2FiUyT1NVLTY+Nz5EN0BGMz09SlNKQEtDOD5C")
    append("OkFERkhOOj5IPUFIOj5FUVdORklMX1dSKjA4Gys3JzE9LDE7P0RFQURLOUFCcG5sY2JhMzc9Njg/QERFLDI6RUtHQUNHQENKPT9FOD5DLjU6NDhALTE6ISg1")
    append("ISgzGCIvGiMuPEJDJS46NzxFPUlAMzg7IicwMjdANDlBVFVWWVxaOkNOMzlENDhCKTA8Iyo3GyQyMzdCUU9NaGljJy42YGRXYWBfWVZQg3xyeHJnSU1SMDE2")
    append("MTM5ISkyEh0qLTM5Nzw/Q0VLMTM5GycyJS02GyYwFiAtFSIvIis1ISk2ERspEx4uMTg7Z2Zia2lnR0dLZmRdSUtKVFRVcWxmcnBtX11dX19UhnpsY2BZP0BE")
    append("EyIrO0ZFUl8+VFdUISsyHSkyND44OEI2WVpQRU4/SE5JSFBGV1xVR1FGOUFAOkkxbm5iQUhEaWpfLTc1a3BdQkhCQkZFRUxLR1BLR0tOWVxUUlZOXF5Ubmlf")
    append("Iy0wPEo8Y2lZVVtPOD1ANj0+WmFRaHBaW19NVFVUJzE7KDM9KDhMKzpMSlVbL0FLRlZRQUxVNERCR1dSQ1NTQUxVNEFQMUBIKj1CMUJCOkpEMkM9O01HRVhJ")
    append("R1lPMkBDJjc+MUNDKjlBQk1NLzxER1tGO0xFKjU/Mz9CQU9FSFJNQU9IOkFFNj5FLzhGQ05NPkZMRkxQQklLPUVNPUNLLzZAPEhHND5GGik3ISo3Jy87KjVB")
    append("ND1FKDI+HSk3JDI9S09QMTlBMzhAMjk+PEdCQUlIOUJBOkFBOEFBP0FJNztEQ0RIMzY+QkdGXVtbT0xMMzc8KTVAIiw5KDA7NDxAQ0RKUFVOcW9tZGNhKS42")
    append("QkRESEpNLjM8Lzk4R0pKQ0ZKOTo+O0BDNz5BNDlAJS04JSw3GiIvGyQxHiczQklEHiYyLDQ9O0Q/MTk5KC02KzI7NztBQEZAXF1UOD5IKTI9KzA6JC04Iys2")
    append("GSIwLjI9R0ZEXWFZJSwzXWJaXl5YXVpWh4J3YWFZUFNYLTA2LjE3KTE5ER4rMDU7P0FBSkhLOzs+HCgzKS44ICgxGCQwFSEuIyw3HiY0EBopEB0tOEM4WlpY")
    append("aGdnVFdTTU1OXl1bVVRUbWlndHJtbGpoW1lUa2phd3FoT1BQFyYvMT88WmZBSU1IICowHSswQUw+Ul5FRkdFVltOSUxNVltXR09JQ04+MDY1ND8wcHJiO0I/")
    append("bW5mKzM1amphRU5CXFtWMjtAPEZDX15gZGhXW11aX2BbVFZOOkI6O0Q+XmZWTlNMRkpLVl5LWl9TZ2hjU1lJREpEMj49JzM9JTVJLz5NXGVnNUdJP1BMQlNN")
    append("TF9IQlRLP1RMPk5KOUhHLj9DMEBDN0hENUVCM0Q+NEVAOktAL0BAKzhDHi42N0tENkZGO0ZILzxFNEJAPkxNLzo+NEBDS1hOPEZER09QRkpPNjxGJzE/QElP")
    append("PkZMMD4+OUJARk5UOkFHNTxFPERNMjlBGSg1ICw5IzA7IS87JzM6LTQ+IS06LjpDR01MOUBHNjtCQ01ENkBAQEVLOD5EPENDPkhCNztEPT9GTEtMOTtBRkpI")
    append("amZjS0xPNTg+MjxHFyQwJi87Ljc/P0NDTlRMbmxnbWlmLjQ7UVZKQ0ZLJiw3MTk+REdHQENHLTE3OUBCSEtHNTpBJCs3HyYxFx8rHSYzIis1S1BMHiUyLTU7")
    append("TlRLISwwJCw2HSk0NTo9Q0lGQEVANTo+IzA8KDI6JDA2JCw3FiAuKzE7UVBNUFhMJi82PkhEWFtTX1xZe3dqUlRSU1ZaLzM6KzA1OD4/EiAsMjU7OT4+Tk1N")
    append("PTw9ISw3Iys1HiYtGSQvEx8sGiQwHyc1ERsrChooQk0/WFhXa2tnZGZbPT9EbmtmZGJdZGJjaGRgeHVzZWNeXF5PcmtaTE1DFSErMj82Vl8/TVFILjY5Ljs6")
    append("VFxMYWdVS0tKaWhiR0pOSlBORk5AQEk/QURCRk89cHBjJCwyV1taHygsX19bO0E+SUxHMzw9PEY+S09NXWBaVldVaWthUVRMPEBAQ0hGY2xYUVVLV1dYZWdi")
    append("WV5QUVJNXGBWUFZIRU1EKzY/JTVHNEVPTFlYQ1VHOEtHOk9GTWFLU2hPL0RDLkA/O0xAMUJANkdFJzk+OkpEO01IN0lEPE5BNUc+KDZAGCczM0VBQFNFO0dF")
    append("OkZNLzlDOEFJNEFDPEpESlNRREpRRk1SMTxBNz9JLjdCQ09KSlJSO0dHO0RFOkJLMjhANTxFOUFMLjhAHSo1HSk3JDA7IzE9KzZALjU9Hiw4PEdLXGNbMjpE")
    append("LjM6SlJJKzQ6PkZGO0BIQUZGMjk9MjdCPUFJSkpLQkRHR01IYmJgR0lPLjI5N0FHGSk0HSszKjY5TVlJTFJNbmxmZmFcOz9FUldLPkBFJSoyLzU+SUxOOj5F")
    append("KjA4QEZHRkhKNDs/Iio2Fx8rFx8rGCMuKDA5UFJTHyYzLjU8QUhFKzc2GyUvHSo1KjE2RUdJQEVHODxBHys2Lz0+LDY9JC04FCAuKDE8RkZHXWJRHCQtJzI5")
    append("U1NWaWVedXBjVFVTRkhLMzg+Jy81RERCER4qJy01Ljg6P0NHPDo7JS86HygzHSUsGyYuEx4rFSAtHyY1EhsrCx4pS1FPWVpXWl9NSkxMQ0ZKY2FfYWBbWVhX")
    append("WVhXZWRjdG9qWV5JdnFbVlZIHSgwOUg3WmNKVFZPMjg7QlBDY2pUbGxhYV9bPj89UVNUSk9JQEVCP0VFOT1BS1VEXmBYKTEyTVJRMDc4YmNgNTo+SFBBTVFH")
    append("LTg1XmZSVVdXVFlLaGpiTlFQQUdFS1dGUFpIY2ZcRkhJW2BYXFxXWF9MZGRiUVRNRkhKNT9GIjJEOU1LQ1JNNUdBL0FALEFASFtKSV1JQlNIQlRGN0lBNEVA")
    append("QlRJHS84PUxIPk5KL0E8QlVDOEg+JTQ7ITA1Kzs7OUxCPk9EOUZLMD1GOERMMD9CM0M/PkhOS1FYRUtROEZAMj1FP0pIPUlGSVNQMjpCPkRKNT1GMzg/MzpD")
    append("OUNHMTxCICs3HCc1ISs4LTlELzpGKTQ7GSk1PUhJVl1XMDdAMzs9QkhHNkE9PklEPEFJQkhJNTo/Oj1FNz1HPUBGREdKTVFOT1NVQUVMLjM7S1ZRFygxHzAx")
    append("Lz83SVZIR05HXFxZT09LSEtKSUxIS0tNKi42KC86R0hLNzxDLzQ7PUFGQ0RIP0c/ISk1Fx8sGSAsFiIsOD5ERUpPIyo1JCw0Q0dFPUU/FSQsHCw2KzA5R0lI")
    append("QEVGOUE/KjU+M0FCMThCISo1FSMvIiw3RUhEXmRQJCs1FCEtRkdMjId4cW1iRklIU1NUMzc9Ji82Nzk9EiAsJS42OUFIOD5EMjU6ISo0GyUwJisxHCYuFCAt")
    append("FyUxHSQzERsrCx8qSE1SV1pTRUpGQkVKTE1QTU1Oamhla2ljYF5ec3Bsa2loUVJKXFtPS0xJJjEzSlRFTFNIW1tULzU6SFc6Z3BYTlRHbmxlSklGUlRSU1dO")
    append("R01IRUtEMTY5QU05YmVdKC80RVE9TllCamxmQ0ZKO0JATE9MJzQ0UVdPSk5LWmFQYmRiT1ZQTltER1dCP0k8R09FQ0xBam5iWltQWV1PU1ZQQkVIUFRPKDI4")
    append("NkdKNklJPk5LOEhFOEpEQFJGNEZBSVxEOEg+RlhISltINUY/OUtEIDI5OkhHQFFJNUY7Q1RAMUI9LDxAL0A6PlFCLD0+QVJEPk9GN0dFO0lJNkg/QlBFLDpB")
    append("Q0tORkxSRE9LNEBEOUdBQktOQ0xPMTo/TFFUOkJKOT9GO0dIOERDOUBGHyk1GiUzHik1LDZCKTZDKDc+Hi05PEZEQ0pLMTU7NUA9QUdINkA9PUdGPkRKO0VB")
    append("NTpAOTxENj1HNDlBPEBGQ0ZKREhORkhPNz5BQk1IGyozKTs1PlA7T1pSVVlQX2BhS1BNVldTREVGR0hKKjA5JjA8R0hLNTlALzQ7P0FFSEM/Nz44HiY0Iicy")
    append("IScxFCItOUBCPUNIJi03LDk2QUdIPUFEESIqFyozLjY9OkI+SUxLPkRCMTpANEJAMjtAJi84GCMvHyo3PUQ/YGVXJCo2GyYxS0xNeHhlbmlgSElIVFRTNjo+")
    append("Jy42PDxAEh8qICsxNDtDKjU8Jy82JSs0GyYxKC42HCYuFyQwEB8rGiIwExwrDSMsP0ZNY2daRUhLSEpMSEpNUVJQYmFgXF1WZ2dmdnNvX19fXFpXQkZHSktK")
    append("Nz87X2RdTFVET1NJNz85WGZCVmJNTVlHUFZPVlhPWFpTXl9XU1lPPkZBP0k9P0lBX2FaKjA1MDsvYW9Jd31tQUdENT49UVJTQEo+VVdSVFhORkhHVF1RO0c8")
    append("NkY5TF1BQ1I/R1JEY21aa2tlU1RKX2JXR05CSUtMUFlBOkU7P1BMS1xTQU9IQ1NHQlVIS11NT2FKUGJHPk5BNkk/PU9EJjg5Q1RIO0pEO0lEOElEKz03Okk8")
    append("OktBKjo9OkxCN0tALD08Jzg4KDs5PU9BPk5EQFJANUQ9PUxHNUFCP0hLSE9SMD5DOEg/Q09OQEhOPUpDP0ZJPUJLOERELDc7OENDLzc/Ji44GiUzJC06JC87")
    append("JzRBIi85KjVAPUVJPkNGNTc8N0FDPEJGO0RERk1MO0BHMz86MTY9MzdBNTtENjpBQERJPUFHPENJRkpQQ0tFQUdJFiMsM0Y5QVBFT1pPWl5XX2JjQkVHXVxY")
    append("TExJRUlHKC43JzA9SElMODtBMTU7Pz9DVk5IPEJDJC05JSo0KC03GygzOkI/SU1MICcyOUQ/Nz89R0pMIS41GCo0MTg+P0NAMTc8RklLLTM8GyQtMTs8JC01")
    append("HSYyGCUxOUNCX2FdHiUwGyYxRUlDZmtVeHBpQ0dFTk9NNjo/Jy01Ozs+FCArISwzPUZDJzU6Ii02LjI5GyUwKDE7HSYuGScxDR4pGCEvEhwqDSErO0RIYGNZ")
    append("PkRJRElKS0xQUVNOX19bOT8zbW1ra2trYGFiX15bV1hYS0pLTVJGU1lQSlc9Tl1BRlQ5U2FAUWFGQkxGLz40QE44aGhfUlJQOD4+Q09DOUc5VGBQU1tMMTw1")
    append("VGBEYm5OXGtKPkg8QUw/X2JcXWRaUVhOX19cS1VFOUg2RlVDR1hAPVA1SFhCTl1HZmtkbGxmTlFKcXJnTlBNU1tDUFlGP0lCOElIQ1NNOkpDR1hHSlxLM0dG")
    append("QlRDRlZENEZAMUM9PE9HJDg7NUZCKDg6NkVBNEVAPlFCNkc/PEtBNERBNUhDNUg/OkxALj85Kj06Gy4yMUQ5PU89T11IOEVGMUBCQ1JKPEVOOkhEOEo8RFNJ")
    append("NkBJOUNCOkNGOENFNEJBLDY7OkdCMTtBKTE5GiY0IS45His3JDA8Ii46NkBGQkpSP0JGMzY7MjtDMjhAR09JR0tLNjpBQUdFJi42Jy88Oj5GQUNHQ0dIODxD")
    append("P0dNQUpJRU5FQ0dNHyszNEY6RFNKSldFUlZQWl1cP0JGVFNUR0hHRUpGKzA5KjRAP0BESUpMOj1DREZKRUZKQ0ZLKDA7Jys3Jy05ISw3O0JDTE9RJi04QElG")
    append("RU5DQENGICs0EyQuKzI6PD0+LTU2QUdCIiowJC00NkM/JSs1HCMwEiArRU9JSktOHSUwGiUwOkE9b3Jgcm9pUVROSUtMMjc6JS01NTg8FSEsKDI5QEs9IzA3")
    append("JDA6KS82GyUvJjA7IywzGiYvChsmFyEvEB4qDB8pQEtIWltbQERKSExOS05PUVVIXmZLSVA5YGFcT1JVZWZmZWRgaGhiWVdWU1tHSlRAUF09Slk5SVc4T14+")
    append("TFxDQUlHMz40QEs8VFhTQUdBQk06QU46RlY7S1dCP0s8VWNHUFtGXmhGTVo+Q1BAQU1BQk1DP0pEQU4/SlJLQlA+UWBFTF1BT15ERFY5Oks5Pk86Ul1MWF1U")
    append("WmNRdHVrVFdPXWdIWlxXOEFBO0xITF1OOklCITM4O0tGIjQ6QFFFP1BEJjlALUFBR1hKLD4/MkFBKzw9NkRDNUQ/Oko+OUpAMkE+NERAQ1dIOk4+P1BDNUY/")
    append("KTs4GCwyPlE9PlA9Tl5KRlNMNEJAP1BDP05HQU9GOUo/OEdDKzdBMjxDNEA/P0xENT5CMDw9N0Q8LDk9JC84EiIuGSkzIjA5JTM8LjtBRVBMQ0xTQURGOTtA")
    append("LjhCOj9HTlZNSEtOMTc9QEZHLTc8JC47PUFGREZHQ01EO0BGQExKOEU+TVdLQUlMIi82NEU8P0xES1VKU1dSX2JfQURIUlJTSktOPkFCLjI5LDVAQkJFRkdK")
    append("MjY9QUVLPkJIREdNLTM+HyYyJiw4Hyk0OUFESk1RIyo1QUtCOUA9Mzc8ICk0Hyw2KjM6Oz4/PUM/SU9KMj0/JzE4OEJAJS03GSIuDyArSFJEU1RVHiUvFSIs")
    append("OURBa2tlX11aZ2RgSEtMNj4+HikzODs/GSMuJzE5O0Q8MjtDJDA5Jy41GCEsJi87ISozGyUuDx8oFCEuDh8pDyMrRlJFY2RkP0JJREhNU1ZPRUlEUVs8VFhL")
    append("PUA9Y2NkYmNieHdyYGJcYGVWXmpKSFFEUFs/UGA6TV03Pko3UmBEMz84XGRVP0k9QU07SFc7Pk06TFxCSFVBSFZCPkk6VWBKR1E+WWNCO0k0Okc5RFBDQE1A")
    append("LDk1NEE8Q1I8P006TFlEUWFBXGhFVGBNTFxCP008Q1FASVRHTVxCUFtIUVxDUVtMWGBRO0c5Pk9IUGFOQU9GIzQ6NUZFOUpEQVNHOkxFLj9AFio2M0RAKjw/")
    append("PUtHQlFHR1RONURBOkxAIzQ1GiowKzw5NklAQFJBRFZFPVBDO00/MkY+OUs+QFJAR1ZKTltPRFNFN0g/PFFAQE9CLDk6Lzw/KDY5NUBFLz48OERGMj1CN0ND")
    append("JC80Lj08KDE5EyIuHis2GScxNEU+Lj45QEtKND1DRk5KO0NKLDZAQEdMQkhJRklPNjxARUpIMjs+JC48O0BDRkpEPUdALzU8NkFAOkZARU9GSVdJPExEKzk4")
    append("RlNEQEhGTlNPX2JfREhJPUBDRkhKP0BDKy82KDE6Pj9DPD1DOT1DOz5EPkFHRklMMzhCGSIuIys2JjA6QkhLR0tPJSw2MTo6PENEKzQzHycwHSoyIC81Q0o+")
    append("SUxMSU5PSlJSOURDPkNJIio1FyItCx0mO0g7WFtbJSw1GCUvMT06Z2VlWlxVY19dTFBPPEJFHSoyOjw/HCUwKDM6Mjc7NEBDJjE6JC01GSIrICo2ISs2HCUu")
    append("EyIqCBokCh4nECQsS1VEZmdnQURLSUxSV1xNSU1NSU1EPkI+Mjg1eXl1aGdif355V1tPUVxCWWNIVWJGUFpCT109OkYySlY/T1xBSFQ/anBkU2BGSVg6T14/")
    append("TV9AQlA7Pkw/NEA3Y2xQVF1JWGNFVWBJO0o2NUEyPEo3OUQ7LTg0Pks/QVE7NEE2T15GVmBEbnlWhId+bnNoWF9RQ1BAQE09O0w4OEg1NUU0MkI1SVo/RFU4")
    append("Q1ZJPU5GPU1EMUBANkhENEZBQVNGNEVCNEVEJTc8L0A/N0hDRlRMQE9ERFFLPEtEM0U7IjQ2LD46Lj87N0o/NEU7PE09QVRDNkg8QFNBHzIzQFNCQlFHPUpG")
    append("SllGOEo+QVVEMUI7Kzk7LDk9GigxJjQ4Kzo7ND9DJzU8MTxDIi02MT49Jy85FCIvHi05His1RVVFO0c+REtOMDs5SlJUMDlCNkFCTlZSOT9EQkZNOj9CRUtG")
    append("LjQ8JjA8P0dEOEE+Rk9KNz5AKDI6TFlKQEtGQExBOko+MUA6TVtES1JNR0tLY2RjREtHNDo+QUNFRUNFKzA2LDQ8QUJFQUNGOj1CQEJEPT9BTE5LMzc+FSAr")
    append("Ii45KDM7UVNVSUxSLDE3KDA2PUNBO0M9GyUtHy00HS4yLzoxSUxNQkpBSE1PQkxDKjM7HygzEiArFCYtPUg/aWdhJy01GSYwP0hKVFVXWmFWZGFbUFRTLzY9")
    append("HCkwP0BAHScxMDg9ODtAQ01CIy81HyoyIyoxGSMvICo1GiQsEiApCx4nDiEpFCctQ09CZWZnNTlBVlZZWmJQQEVGR0xKNTo5TVVHYGhXbW9kYWdXXmtES1dA")
    append("V2BRVmNNXGRNUVxJQk8+RFE7UV5IPko8XmhVTFlBS1s8SFY+SFg+RVQ7QlFDSFQ9ZWpaXF5VYmdWWmBSSlVAOkc2P046N0I6O0g+NkI3N0YzS1pFVmNOXGRQ")
    append("XGJSfn53am9jVFpSUlpMO0k5Q1Q8NkQ1QVE+P1I7QVI7P1E5OkxDNURCOEdDIzQ7NUhFM0U+O05APExEGi05NERDMUJANkdBS1lOQE9FP09IO0tEMUQ7O08+")
    append("OEw+JTY3RVdENEY7JjgzP1I/MUU6PFE+Jjs0NUc9NUNAM0FCSFlFSFtDP1JBJzY2KDY5KDU5IC00IC41IzI3Kjc6Lz9BLzpCKTQ+JzM6JjA8FCUyFiYyLDc/")
    append("PUpESVFLUlhZOkVCO0JJKzM5Ul9PPEVJOj9ENTpCREdIOkJALDI7Hys3OkNDNT5AQEpFQElDKTQ6SlZNPUhBQ1BGPUw8Okk6SlhGTFdLVVtRaWplREtJMzo7")
    append("R0dHPz9CMDQ6LTU9PEBCSUtKQUZAREdGRkhEUFJQOz1CHCg0IzE7IC4uSUtOREdNMzY6LzY3QUdAQURGFCIrITA4ECEmHCkuRUpERE4+Nz4+Mjs+JzE2Iyo0")
    append("ER8qITA1PEdBXV9VLDI5HSo1TlRTTE9RZGlhX19SUlZWLTU7HiwzMjc8HigxMTk9QENITFRMLjw2HiszKS81GCYwICo1GiQsFCApDSEqDSAoFSktQk9DWltb")
    append("KS86UldPVVhSQEY9RU9CR09FR1FIN0I4WWBQTFVBT108TFk/VV9NWWdJR1BCW2dMPkk8S1o+Pko+Q1E6UF5DVmVFWGg/UmNATFhBPk01UWNBcXlYWVtWWl9N")
    append("U1dNTVJLUlpJSVg+M0I1Q1A/R1RBQk8/R1VEQVE+R1RDcXhobnJoYmtST1s8RU5AZ21hS1VJSlpAP006Pkw7RFU7QlQ4QVI8M0Q/OkpEM0FBMkRAKDo8MUI5")
    append("Jjk3OEpCGi04OEhEGiw1NUZBOkhBQFBHO0xEN0ZCGjA2P1M9PE4/L0I5QlNCNkc9Nkg7PFA+QFQ/MkQ2OUw8OEk+NkNDHSw3O0tAQVJCQlVCJjU0NEFBJzM5")
    append("JTI5JzQ5IjA2KTg5LD08KTg3KDU+KTZAKTRBHzA+DiEsN0JHLTw8PUhHTVRTKTI6KTA5TVdTT11RKTE9NjxEPUhFPUVEPUZGLDQ9IS05OUJFQEtFPEdCNz9B")
    append("NUBASFFPOUY6MTs7Okg/Rlk9Qk1GUmBOXmNbV1hWUlVVNEE2Tk1JOTw/Mjc8NDtBQ0dFTlNKR1I/SlFKUFNMRUdJOD1BICw3IS45LDc0QkVKQERKP0FBPkNC")
    append("OUA+TFBQHSwyJzQ9GSYuFSEpQkhASk9LQ0pHQ0tOIy4zJSw1GScyN0RALj03T1dMMzpCHSo0PUVCU1ZXWlxcWl1XVFhWOD1BIS82Ji00Ji83KDI5QEZJRktL")
    append("KDUyIS81JS40HCo0Iis3HSYvFCApDiEqDiIpEiYrTFZJW1xdKTA7S1VFTFBMSFI7RE88UF1GNj49RFBERU8+Rk9GV2Y8TltCNj83S1s7T14/WWBPQ04+S1k8")
    append("Q1E9TFdAPks+TFs/S1k8TVs7S1o8RVU7YG9HgoB4X2FZYWhOT1RLT1lFanBbSFc9Pk03Q1A5OUY1MD4yOUc8PEs5U2JGYm5QXGFaZW1ZVF1CTltEWWRLVl1N")
    append("Vl5OSFZBR1ZCQ1M5RFU8SVtAOUlDN0dBNEQ/N0pCKz05OEk9NUg9NEc/O01DN0lAHi82NEVBGSkxUWJQRFZJMkFAHjE0MEQ4KDo3MEM2QVNCSFpHQVJDMEM5")
    append("PE47JTczOEs+RFVEQFFHIjE7Hi0zLz07RVlBR1k/KDY5Lz0/JDI3MkA+LDo6Jjc3MkM9JzgzOkpBLDpBKTZAIDE9Gy43NUJHLjs+SlZNRU5KLzk9ND5BUVlY")
    append("O0ZDKjE8PUVCR1VEN0Q8PkRGJzI6Iy84Nj9CN0U+OkhASlBRN0E/Q0xFP0xANDo+PUtETWJCQEhDTl1KTFFPVFRTVVZWRU5CUFFQP0JFMDY7MztAR0xHS1ZE")
    append("RlBEVlpPSk1KREdJMDY6JS44GygzMDpBOkBHREdMR0hGO0BBQktBPUNFOUJAIjA5JjE3IS8wP0JGW1tcRktNNT9DLzg/Iiw0Hik0PEc/OEM9VlpTLjY7Iy8w")
    append("TlJOTlNPXF5eXF9cSVJJQEVHIi81IyszJS02KDM7TVdHSU5JLDg1JDM4HikwGicyHCc0HygxFiIsEiUuESYtEiYsRlFHWVtcIykyP0dCWWJPR1M6SlY+R1JB")
    append("QEpEUmFINUA4SFVEUl9DRFE9SFVDSls8V2RATVNJWl9QSFU9TFlCPUc4Pko8MT8yRVQ8QU43SVg+WGZKa3FbXl1YWmJMYGNWRUlFd31nXmRQQU85PEo1Pkw2")
    append("SlpASlo+PEo6RE8+UmM9W2NPX2RWa29jTlZEVGFDYGVSX2dKc3ZtaHBgUVxLRlY+QVJAQlI/NkdBO0xDO05CRllGQFRDR1pFQlRCRFdHQ1ZFQVJCNEY/QVNL")
    append("JjY6QlNKSVhKMUA+Oks+MkY3IjUyPFA9O09BQlRJKzs6MEE5MEQ5NEg6OEo8KDs2O0w6JzQ3IjA6LTs8Pk87QlNAO0s/NkQ+JzU3Jzc4NUU+L0I7NkY9LUA2")
    append("OEg9NEQ8NkdBKz09IzM3Iy84NUBES1RPQ1BGO0ZCQE5NLzhBKjM6KjU7U2BIN0E/Mzw8T1FSHys1MT89RVNIJjU0PUw/SE5PP0tEQE1BR1BKOEBDRVdGQlk9")
    append("VmBSMkI5QUtDUFJQUFNSTVNSUVRUPkJGMzs9OkY5RlI9TFxCOEI8SUxKQ0xBPkNHOD1CMjs/IDA1KzVAOkBFQkVJQ0k/Nz88OT4/MTc6QElGFSQsKjY3GScp")
    append("QUhEWlxXO0JBNT9BLzc/FiQrIy02Rk1EOUFATlNQQUlDQEs/RUpIOkI+UlZVWF1VQEo9SU5OJTE3KDQ4JDA5IC02S1g9PUE9Mz45M0E9HSovHCozEyEsISo1")
    append("FiErEiYvDyQqEicsSldGX2NbLzY+PENASVRBUV1BSldAP0o9RlBESVhCNUA4RlRASFNGPEc5RlI7VmJJUFdHWF5SW2BVRFA7UVtPUVpOanJgQE0+N0M3OEQ2")
    append("TFs8ZW1VVVdSVVlOUVhGWlpXWV5RgYV3V2BCQU46RFU8P1A4RlNARVM7PUo5Qk9AXGVLXWRUV15PcXdgU1tKTlZDbG5hVl9GmJiVfYFxPUREQE9ASlxCNkQ6")
    append("SVpGSFlJQ1RHNkg+MUQ9Tl9IPU5CN0pAOktBP09DP1BARFZMRVRLPUxJRlNHMUA/PU5AQlQ9Ok08O089QVdCMUNDMkE9PE0+NEY8QFM8QlE9Kjs3UGJGKjg3")
    append("Kzo9OElBIzMzQlNDO0s+MUA8KDY3JjY1Nkc9Ok4+KTk3JDU1Kjo5QVJCRVdBM0U+Jjc3GCgvMT5DUFZWRFJEMjxAIi83LDVAKDE5MUE9QEtBOD9EOEI/SktP")
    append("Hys1Okk+NkVAQE9DO0s6QEtBQ05IQE5FP0tESltDQVU+RVo2T15ELj43OEU6TVNPUFhQOkNBTFRNQUpERlNDPUw2UWE/TltEOUg4QUo/NUM2QEw+SVJMMD48")
    append("L0A2Ji86Nj1DOjxCRU8+SVFFNjw/NUQ3QU1EGCUsOEI9GSouPUs6WF1RQkZDPERKKjQ6GykvJDA1QExDO0I/OkJDPkRHMjw6PkVHTFBNT1ZLUldRQUZFSE9N")
    append("IC0zLjs7JjY/JDI8RFI7P0NDNT89Ljg3LDs3GScwFSMtICs2FCMrBxsjECUrFiotYGxUX2RaNTtDRE49S1hCUVtNR1JDQk0+TlpDTllENkMwT1pHTFhEQEs8")
    append("UlxEV11WRk5DU1tHVFxPUVtEU11SUl5QO0M/Qk4/O0c4a3RpV2BJWmFSSEtLVl5IQUNCVlZUX2ZMYWlNSlJIMkAzOUY4P046PUs6OUc7RVM+NkU0W2NRV19Q")
    append("UV49aG1fRE07V1xQbHBcf390eHxrd3xuLDQ4SVRIQU87VWJLO01COUtEOUpFNUg/Kz08NkZAMT89GCowGy01O0pHRFVFSFhINkRBQlFLR1ZKN0ZCLT44P1FA")
    append("QVVBQVVATGBDMkNAOEhAQFJBOks9PU87TV9ENUY/UmJPS1pLLjw8PExBNkZBRVVFPUs/IjE0Kzo2NkU9MUA5OEg8IjMzGSkxMEA9P1FDOEo9ITE0L0A5Lj88")
    append("JDU4WV5dSFBHKTI9GycwLTc+M0A8Lj41N0NCR01RQk9BR0tMLDg5NUM6Kzk5SFhJSlpAPk49N0NBLj02RVhASFw9PlA9Sl44Tl4/QVBDLTo2SFNIS1dLND84")
    append("Pko/NkI2SVlET2JBPkw4RVBDPEo7NkA+P0tCQE49PUs+GCQpIC8sISw3PUJHQ0JITVlFQUdDQEZFMz03QU1AHCYtMDo9IC0vOUg7P0VEOz5DPEVJMzxBHy00")
    append("JTM0OkVBRUtGLTU6PkJIUFdTQERFPkNCUFZNT1RQTFNNR05LLjo0ND4+JTQ1LjtFQU5AS05MNDw7LjQ2JzczFycvFSUuEiItECMpCh4mEicrJzkyX2VdWFxc")
    append("PEM/RVE5Ul9KVmFSTVs+P0o9UFxBUVpQR1Y6R1JFQU06LDctVF9CO0JCQU07W2NKTVVDSFI+SFNASldJO0Q8S1RJRk5IZWthT1VJTlFPWV5RR00+REZHWFtR")
    append("YWhPUlpCRUpIQUw5RlRAPUo4QFE9QlA/Slg+QlM4SFc9S1ZDRVBAR0xGU187X2lTfH1teHh0XmZKcXRuTVZPN0E+Lzc4RVFBPE9EMEI+PE5EOk5CNUZDJzk7")
    append("LDs5HjE1Gy43N0ZHLj8+R1VHMkA/TFtPQFBGN0dAITIzKTs5RllEQldDLD03GiowKzs5Q1ZCM0U7NEc7TV9IOkxBOEdBMkI8JTQ1NEU/QVJFQlFHOkZAM0I9")
    append("NEQ5NUY9PE1BOEg8KDs1KDo5Kjw4Oks7Lj84Lj82Kzw4Kjk6LkE+YGRiTlRPKDA6IC40QE9EMD87M0I8OEZBUVlWOUdDPkpDMT89QFBCRlVHP08+PU47M0Q7")
    append("MkA3P1M9QFQ4PU84UGU/UWU9PU04OEY7QE5BOkc7T11RO0k/T15FNUI4PUs/TV1ATlxDQU5BR1VDNT8/OEM/O0Y+NkI4JTIuMUE1HCwuPUNFQ0VLSlhCS1ZH")
    append("QEo8PENBOUI+JTAzGiguLzk4Lj4uPkVGRklEOkJDLDY4JjE6KjoxSU9LSlFNJzI2P0dETVVPSVFJPkNBT1JRUFVQRk5IPEVBQk1BMjw+MD86Kzg6R1VBPkNC")
    append("LDk2Njo9IS0xFiUuFycvBRgiDiMoEyguFCksN0s4XWBfUFVVRlE8TltBVWJPSlZKU19ER1M9Pkg1QEdGUFxHV2FRRVJAQE01U14/R05DRU09TlJLREw+YG1E")
    append("T1s+S1hHU11OR1FHVVxTUFlHMjc7Q0VIUFdGX2BYUFVIY2xPZGdeVFtHS1JDR1JCNUA2OUU3P085OEc4PEs5Oko2UmJFOEU4WmJRSldEUFs9V2ZBen5tbG9k")
    append("i4uBZGlhVFxWO0hCMjk+Mz46TWFLIjQ4O0xCNEY+OkxDN0xDOUpBNkdCJDU2NERCMkRBPUtGNURCP01EPUtBNkY9MkM8LD04M0U9PlJDL0A8Kjo2KDg5PlE9")
    append("MkQ9MEI8O0pEQ1JCNEQ+MUM7Nkk7GSovQlJFQU5HP01GOUdDP1FAMkQ4M0Q6N0c6N0o6IjM1LT86JjczKjozPlE8MkI+IjEzL0E8TlRUUVZWLTk8LT06OEg9")
    append("NUNCQlBHO0pEMT88Kjk3MkE+OUo/RVdES1tGQVI9NUU4Hy8xJjU0PE87SV89TmI/UGU+SFs+Okk6QE8/SFZIPks+RFBCQE89QlA9TFpHOEU5Lz0yTVxCPUw8")
    append("JjU1NkE+OkU+QU0+OEQ6IS4uKzkyJjcxO0o7RlFFVWFJSVREQEpBOkM7LDY3OUY/JDIwNj8/GCgmR1JFSVJHKTA3OkM+KDM3PUw5RkpJSFBKNkA8N0E/LDc3")
    append("Pkg+OkE9SE1NWFxYQElCN0E7OkhBPERDJjMzLz02P04+Q1A9Pk4+MTY6ISoxEyIsEiIrCx8oCyAmGCwvLUI1Sl88X2NgWmJSVWJFWmZMWGRQQk49TVZBP0s5")
    append("SVY5TVdKS1dEUFpOTls+SlVARU1EWGFNXWRUWV5QUFpDVV9FTls6QEs/SVVAPEg7TFVDSU5CNTw3WGFLVV5HWF9LVV5DWGBHZ25TT1s/T1dDSFM/P002PEc8")
    append("Okg1RFQ+N0g2Pk46NEA7NEE1R1g/Q1Q9UVw9XmZOamtkiIp/Z2pmZmxbXWNiNUE+Nz9ALzk6OktFOEpFNkdCPk8+QlRFPVBDJjc3QlNGNkY8OElBO01IQ1JM")
    append("O0tISFRGM0E5Oks/M0U7OEk9HjAzPE8+MUI8O008MkI6Okw8OUlBRFVEQE9CRVRDQFFDLj84QVQ+HS0vM0JBSVZIRVVHKzw6PU4/PEs4Kzo0Lz83QlRAFiYt")
    append("JTU1Gy4xMkQ2OEk3Kzs7LDw7Kjo6OEFBV15WMEA+MUA9QFFCNUNBQVBJNEFCIC8yLj05NUU/SFtCPlA9RVY9PU87Lz45MEE9PExAOUo5TGE9VGhDSFg6Q1Q/")
    append("RVZBRVRETlxKTFxEN0Q9QVE9Mj84QU9AQVA8SVo/RVY8P08+MkE5PktBQExFP0w9SFVDNUI5Lj0yFScrKjw0QFM5RFBAMzo5REtGQEw6N0IzQ05ALjs7Nz5B")
    append("Dh0mJzU0O0s9JzE1Mjk8PEVAMTk3P0ZBMDo9KDM3NEA8Mj02QkxGT1ZOXGFbUFhMPUU/Mj02Pk1ARE1MJDAxLz45Mj48Sl0+PklDJzA1MTk8ESAqEyMrCh4n")
    append("CBwkHC8tMUQzSV06W2ZWV2VFYXBLY21aR1JHSlg9SVQ+S1Y/SlZDT1lNUGA/PEg5Q1E6SVBHPUU+XWdPUVpFT1dJUlxGQk02VF1BTFg+T1xBUVxFV1tLREZB")
    append("PEY2XGdJXGpDXGhDYGpNQk8yaG9ZSVE/VWBBLDYwR1E8T1hHO0c6P004Pk45RlM8SldIRVI8RVJDUF5GVmFFY2ZhgoN6fIBudXZrWFxSRk1SJjExP0o8OURC")
    append("JTY8QFFDRVZGP1BANEY/P1JEFykzMkI9P1BBPU9ESltOPUxHMkE/RlNGOUhANEQ7NUc4MEE4FikzPE9ANkY9Ok07Q1Q+PlA9TV5IT2JITF5GR1VFO0w+N0g8")
    append("MEI2MUE6QVFISVVGO0w+LDw3OEk8PEo7MEA3IjQ1L0E5NkY8HSwyJjg0MkM4R1hBN0g/HS4zJDU3Okc9U11NJzg5Pk5FN0k+MkA9KDk5MEA9PU5CKTs5LD04")
    append("O008RVg8QlJBOks5NkZARFRFPEw9UmVCWWhRUGNDS1pBRlRHQlI9RFNDUF5IQlM9QE8+Pk0+QE0+QU8/QlJANUc4Rlc+QE4+Q1E/SFdDMkA6R1VCKTc0N0U5")
    append("QlM7Ok07KzsyMkEyOUg2QExAPkVAQk4/QEo/PUQ/Lz87R0tLHSowJzY0M0I7LTw1MDs3Q09DKzU2TlZOQEtJO0k/LTswP0s8SlVJVFxTPkg/WGFVN0E+P0pC")
    append("Pk07TVZNOURCJzMyNEBCO0s7MDw5OEg9Nj89ESApDiAoDCEpESYrNkk1OUs5SFs5SV0+UGA+XWlPVl5UTFlDTFo9P0k9SlU/PUk6QEpERVJCLjs2MT8vSVJI")
    append("OUM0U1xNXGNPUVtCRE88RlI4Q00+XGhGNkY1a3VWR0lGSFI+WmdEUltEXGlGO0csVmFDXWhCY2tOUVpCPkc7N0I0TVNKQElBTVZJRFJBSls8WWJWUmJGMDs2")
    append("O0ZASVVDTFdMVmJHgoRvmJmOd3lnU1tKOEI/NEE5PEk6PEg/M0ZCPk9DSlxFPE9ALT48QVNFJjg9JTc6L0A6N0o+RFZJPk5GSVlHRldKPk9DMUE5OEg6PlA+")
    append("Jjg0JDg4NkY+QFI8R1pAS15DT2JGTV5GS1tDR1VFKjw1PlBCO1A9Nkc/Lj46PkpCOUg9IDAuKTk3MUE5P1E/NUg7L0I2P1E+KTk4KDg2QVRCNkU8N0c/Kz83")
    append("KDs3T15NSFZHKjk5OUlCL0A9PE1FN0g/QlJDQ1RHL0I3R1lDPU45S14/R1dBOkw+PE1DPEw+RFVAZHtJUmBUQVI7TVdROkc/Q1E+RlRCTFtFP1A9QE4/SFZG")
    append("Pkw7RlRBPkw9HC0pQlI7RVFBQU09Qk49Qk49RVNHP05BPEs/QlE+P088QlE7Okg5QlE8TF9FQklGOUE6TVVNMDk4PUw8T1ZQIC4yKDkyKjY3ITAvN0c6QExA")
    append("OkNARUxLKjY1RVBGJjQzR1NBO0k7Ul1NS1pEOkc9RU1KUFpWO0o0TltGT1lcN0I9Lzo+N0U+M0I3MD42JzE0FCQrDR4nDyUqHjMwIjYsP1E6OUs0UmZAS1k7")
    append("XWhRTFdGSlo9RlE8OUU3SFVEPEg0QUs/KjU4M0EzOkkzTFdKSlVETVhDVV5HSlY9S1k5RFEySVQ8WGNMNEUzSFQ9UVVSSlc5V2FCXmJVWWVHQE4tTls5VF49")
    append("X2VQVVpKSE5EQEs5SVQ/WWJOQ0tCRVBFPUo3VmVPSFg8PEk9PklFPEY/SlRPUVtKeXxslpeObHFgW2VPSVZEQ08/NUE2QlA8RVlILT49QFFENUY+N0dBOktG")
    append("L0BALT49M0Q8PE5APU9DTF1KSltHQlNIP04/OEk9OUs+PlI+P1I9Kz44LD01QVI+OUxBQVM/RVdGNkc+MUA3QlFCO00+LUA7Ok46M0I9KTk8QE5CNEI9MkE4")
    append("ITAzGikvLj83NUY3Lj82QlI/KDo4KTo4RVhGOEc/ITMzMEM5M0Y8NkZANEM9QE1FKzs7IDAzMEE7MUM9Oko+RVZJSFtCQFNBQ1U6Nkc8OUo7JTUxN0Y+QVJB")
    append("U2REVWZHWWdVUWBRQElGP087P04/S1xERVU/Pk48Q1FDPEhBR1Y+RFRBQlE9QlU1T2BARlNCRFA/P0w6Qk4/NkI8OUU6Pko/N0Q7M0E2OEg5PU0+QlBCLT41")
    append("RU5IRUxHSlJMQ01EOkQ+P0hBKjY1GysqLz47L0A3M0M0PEg+ND83Q01HLzg3LTc4NEI9VmNVNkQ4UWFEP00/ITErU1hWRk9OP047O0o8Ul1WOkVBPEhLMDw/")
    append("MUE1ICwwJS81GCcvDyApDyMqKj40IzcsT2JEL0E0VmlDUV9CVGJEQk83VmVNQ00+R1NCLjoyRFQ6R1M6KzY1Pk43NkM5QEw7SVRCT1tESFBDSVY8OkYwRFIz")
    append("VF9HRFI4O0szO0k0U1tKV2VCT1dGS1VBVl5CXGo/S1c5S1U+XGFNZGhZQ0lAQ044R1M3TFZBNkE3OEQ5N0M5M0EyTFhHQVE7QlJBPUdCQEtFS1dFbnRflZaL")
    append("Vl9GTFdIS1dJRlM8R1Q6R1Y7QlVHGSo0LD49HS4zLz49IjM6JzY8MD89NUg9P1JAQlREVWVQTV5IQE5HN0Y8OUk7QlVBRVhBQlVBR1pDRlhBT2FFOUtDNUY8")
    append("RFdIRlZCOEY9Okk8MEA3NUY5MkM0MUE2QE1GVGFPLz86MUA2Lz86HCswKjk2NUQ2JjYzNUU9GisuNkY+QlREMUE+IzU2HS4uLT87IjM0MUA8O0lDQ1NHNkQ9")
    append("R1c/IzQ2KjkzRlZGUGNGOEs+SFpBP1FEKzs2Oko8OUdARFVASFdAUmBNVmVMTFxKVGNKR1o7QFBAQFA8RVY+Pk09QlA9RFFERFY7QlI9Sl0+S106TFxAQ1E/")
    append("MkA7RVQ+RVE/OUQ7Mjw2MDs3JzQxP04/SVpGO0tAPEk/KDcwRFJCTFZOSVVEQk9CNkI9RlFGMDs2EiInIzEwKDczNkY0MkE3O0o6OEI+KDIyPEY/Ljs3QExD")
    append("MT40Sls+NEI4PUs9Q0xESVJJQ1JAOkc7P0xBNkFAJTI0IjA0MkI6Kzk3ICoyGCcuECIqDyMqPFA9P1M5VWVMQFA6UmFHSlk+UF1CP0wzVWJVPEk3QkxCOkhA")
    append("QlQ5Pks8OkZCO0g9M0Q1T19CRE88VWJFSlRBRk8/Slg5QU09TVtBS1o5JDElLjwvUFxAS1Y5WF5SXGpCVF1DTlo7V2FEWGNDVV5HWF5UTFg9TVk6VFxGSVJC")
    append("P0o4QE48PUw6QE45QE06NEMyPU03TllISFVESFVDaHNPdnpjX2lMRVBBQk09RlM+Okg3Okc4MkNCJDQ5OEtAKzw6Lz4+ITE6KTU+OEdCOU4+TmNFTF9GUmFL")
    append("Xm5OOEhDO0pANUU6NEQ4Pk8+QFJAQ1Y/PEw+TmFGT2JJP05CPE1CQ1I/QVBAKzo2NUU5Lz83LD42NUY8PklEU2BVO0w9MEA0M0U5IjIuGSkvLj84Pk89NkY8")
    append("QVJENEU4RllEKTk8FygxJzo1KTo6MEE8QFJJSFpJPk9CMkI7PE05N0k7M0M3SVhGTV9EP1JAPk8+RFVFOUg8SVhEPU5AQ1M/TFpLS11EQVE+SlxDQ1M9RVg8")
    append("Q1U+Q1Q+TF9EPE05Pk43S1pDMkQ1Okw5RVY8R1c5RVc5Rlg9RFFDQ1Q/QlE+Qk5ENkM8Okg+NkM5QE0/P01AMUA8Mj47RldBT19FP0w7QFA7OUc8QU5ERFBI")
    append("Mz44FicqEyQoJzQ1NEQ1KTs0O0o7JjAySFNFQk5ANkI7N0Q8O0w7O0c8O0o3PktHOkU7RlJBRFFBXmdeS1dJP0xCMT44P0xCHi4yKDc2GicuFyctESMrFiou")
    append("L0I2PlA6S1lISFc7UV5HRVU4RVQ7Q088Sls6PU00SFQ+QlBAPEk3PEg7TVlHKDI2O0o2RE5CRVE9PUc2UFpBOEE8T1VOTltFOUY9TVw4Hi0pIC8sUmM7TlRC")
    append("TldGZ3ZIVWRAV2U8Vl5CU1xCS1RAS1Q+UFlAVGBAUFhBW2RPX2lWRVI+OUU1QU44S1s9QE43Q1M6S1lBP0s+QU5BXmxKZnJIU2BFRk9CSVNEQ08/PUo9RlVC")
    append("Q1RIPk9EQlREQFBHMkJBLDtCLjpDOUVHS15DV2xIMUE8VGRKVmNUR1hLOUlANEM5OUk+Okw+NUU5RlhAR1hFPU9BSlxAPk0+QlFALDw0Nkc8Q1NDN0k9MEE4")
    append("N0s+O0tATFhNOUg9RlU9O0s5O086O003L0A4IDEwM0U4NEU6OUk/Rlo9QVVBIzM5FyozGCouLD05QVVCOExCRFVGJjg1JDQxM0Q1OUo4ITIwO0s/QFE+HzAv")
    append("LDw3PE0/OEc5P00/SFc/UWFIT11USFtBRVVAP1A8Oko5QVU7R1lBSFtATV9CQFI5RFY7QVI8Rlc+Rlg+SVtASVo/QlM2RVg7MDw5RFJASFZBQE4/N0Y7QFBA")
    append("OEY4KjcyNkM8KTUxKzc1Q1JAO0c8SFlBPEw9Q1JCRlFGS1hIMj82L0A0Kz8vLD42N0c6Hi8rNUQ1O0g/PUo/Okg5Q1BCO0o9O0s5RlFDP048P0xDJC8tOEU3")
    append("RlRATlZOQUpHNkM5KTY2SVVHHi0yKDc2FiQoHSwsESIqGSssIzUvM0Q5S1xATFw9SFhAQlI2QFA4S1hERlY2R1c1RlJBRVQ8Qk9DKTUrN0UzMj8zNkM1OEA5")
    append("Tl4/S1c9RU09Pko/QUdEXmlWP008TFw4KzwwGCYnXGhOTlJGWGREWGZEUWM7XGlBUlpCVF1EUFlAUVk+TVU+YGlPU11ER1A9TFRNR1M9PUk2QlE6QVA4QVA3")
    append("UGBBQU86T19DS1tBWmxAW2tBUGBCR1M/TVhERVBDQ09BRFM8U2RLRldFRVVFSFhJM0JCLDpBMkBHOUVKS11FVGlGPk1EXGpQVGBSQE9HMUA8NUQ6PElAQFFB")
    append("Lz84PEw/Q1NCP1BBMUE3RVY/QlE+OUg8MUM5OUw+MkU6QlVBJzkyM0E7Q1BCP008RVU9P087MkM0M0U1NUg4O047L0A1MkM5MUI7PlJAO05BHCozFyozFScw")
    append("Lj85MUU6QlVDPU1CIC8vM0M4L0E2QFE7EyMqNkc5RlhAOEo8NUU5P1I/NEQ6QlFAUWBARFNERVNGRVg9R1dBOko4N0c1QlU/Sl1CRFc9T2NARVc7RlU+UGBJ")
    append("RFM/QlM+Q1c+QlU4QlQ0RFg7IjExO0g5Qk8/Okc6Q1I+UF9IT15FR1ZFQ1FCSFRGOEY8QU1AOUc5O0o/Pk49SFRFSlZFTFlKNEI2PEs3QFM5KzszMkA5Lz80")
    append("Kzs2PEs7MUAxJDMsOUg7KjowOEY4UGFIMkA3P0w/N0I5PEs7SVpHWmRULTc3OUc7Kjc5MD01GicuHSsyITAyHCssEyUqIjYvKTowUl9RTFw/T1tEPUw2PU03")
    append("QFA4RlRBSlhBSVc9QUw9Q09CRlJHPE0zNkY2NUcrM0UzNUAyTmA+U2JEV2NDMTs2QU46UltOUWE+O0s1MkEuHCwqTllDYmxMVWI8Ul5AXW49U18+WGRDXWdI")
    append("T1hAVFxBVV5DX2ZPS1U8RlE5Qkw9O0U6QU04Qk88P0s8Pks3RFM5RFM7RlQ+SFg9T2BAVmk9T19BTFk/RVI8QEs+RFA8UF9DR1hHS11JSlpFTFxKNURDO0dK")
    append("OkhOKjlCP1BFS1xFTl5MV2NQVWJRQE1IKzo8P05BNkQ/OUk/ITE1RFNCRldFP09BGiswQVE8R1dASVlIP1E/PU8+LkI3NUc4OUk4MD01R1NFQk5DPk8+N0c2")
    append("JzgzLT8yOkw5P1I5HzEvMkQ8Kzs8OUhENUQ/GScxFigyFykyTWBPOEs/PU4+NUU7MkA1LT41M0U4PU06KTk0Okw6SVtCS1xDOEg3QFI+RVVDTmBGVGVIP01C")
    append("Q1NAPEw3SFhAR1k/RVg9QFM9RFY+QlI7UWJATlxBQE07RFRAQVA/QE89P1A8QVE1R1k6RVg9RFc/QFI8PUk9Mj00QU88QVE+P008QlBBOkk+P0xBR1ZDOkg6")
    append("QVE9RFREQ1Q/OkQ/Pks7QEtEP008Lz0yKTgvLT42JDAvKTYxSlhOOEg4Okw3NkY1PEpBOEVAMUE1Q1U9OUc7Qk5BNEA6RFY5P00+RlVIQVA9R1dCMkA7Kzsx")
    append("FSIpEyMsGSkuKzs0IjMzKT0xQVBAYm9VT2A+Slo+Pk40PE43Pk42O0k4Q1A/NUE2QlI0N0U3M0A1NUUwMUAvMkQsIDAqQE81XWlRWWNNYGhTN0M2TVw/Q1A+")
    append("QE06L0EwMkMvM0MzWGZMWmZAXmlDY3FAXWtAVWI9T1pATlZETVdAXmZGYmpOUlxFS1U+RlI8RFI9OUQ6NUE2QU0+Qk1ARFE8QlE5Tl87RVI8QVE7S11CSls7")
    append("Q1A7WGRPP0s8Q089RFBAS1hCQlNHTV5IQ1RDQlJGPktKO0hMOEZLKDdBQlJNR1ZMWmlTSVVKUV9MRVJNNUNAOkg/MT88NUc+GiozOkk8SVlFQVJBMUM6SlpD")
    append("RlZCSFpFO009MUE1OUk7MEE2NUQ6N0Q6S1VNPUtEQVI/N0g3JjkyKjstMD4yM0Q0MkQ5PU9BMD88JTI1MT09GykzFigyHTA5OU5ES1pJLj82Lz4zNUU4PE47")
    append("MkQ3PEw9O0w8R1pARFQ/SVpCTF9EOkw7T2FGT2BHQlE/QlRCQVE6PU05TFxESV0/SVhGSV08SlpGQ1Q6T19ATFtBRVQ6Q1M8Q1NARVY+R1g+SVs6Slo+RVc+")
    append("OUo5Okw6RFBFN0U3PUs8Q1Q9N0U1SVdHPks/QVBESVhGQlI/RVU+Q1I+P009MTs5P0s8RFI9SFZBMT82MD4zQFQ4M0U4NEQ4QVA8JTQvOEg3PUxANkU5OURF")
    append("MEIxQVE+P0tBQ05GNUU0Pk06N0I7R1c/UF9FT2BKMkM5NEUzGCUrFSctHC0tLT02GysvL0EyO0o/W2hJR1Q8UGBDQFE0QVI6NUQyP044O0Y3NUIyQU81PUs3")
    append("PEo3N0cxRVQ8P1E1OEY4PkwzS1NEV2BPVmFLUF5AU2NARlM8PUo5L0ArN0kyNkc1XWpMWWc9Ym1ATls4SFU7RU88Qkw6UltFUFs8WmRBWmVKVGBCRVE6OUM4")
    append("QU08SFU/Slg9Slc/WGVDVGJARVU7QlQ3Tl8+TFw/R1U8QE85PEg5TVtIOkU4T1pBTFdETVlHVGRMTl5IRVVEQE5ISldWMj9GOkVKQk5USVhOO0pEXWxTUV5M")
    append("SFZJRFFNSFZJOkY+Lz47QFJCITM1N0g8SltEQFA+TF1ETl5GQFBAP04+LD03N0g5KDgxMkU2P09EPEo7R1JJKjw4Nkg6Nkc6PVA6MkMyMUM2LT03MkM5PU4+")
    append("RVVDHS0wJjM1JzY4FikzHzM3O09BR1VFMkI5O0s8KTs3QFM7Lz8yKjk1PU88QVI8PEw7QFA7PU08O04+TF5BVmdFS1xFIzY0S11CQlU7RlVEP087QU9BPVA2")
    append("Pk89QlU6Sls+Q1Q9SFc9Pkw7Pk47SFo8Sls/Slw8SVk+Slo/QFA/JzguQ1A+TFxHPUxBRFM6KjkuRFNBQU8+NkI6RFFDQ1NCQVA+PU06RFFFP0o/OEY5RFFC")
    append("SFVDP04+O0s8PlA4KjwxQlM8PEo4NkI9NUI9Ljw3PU04KDU3Okw0Rlc+RFM+NkI6Rlc6RFM+PEc8V2hDRFM/SVhOFyYsJTUuHi0xFikuJjkyHi4vJjYuKz4v")
    append("SVg/QlA+Ul1JSVRFQVE2Q1E9RFU4PUw4PEg7QE41S1ZHQE09QE43PU0yPEo0Q1U1OEkwSVY6UVpOSlVBWWhET15DTVlIQ1A6MT0ySlc+SVs8WmtMWmc9XmtA")
    append("YGtFSFQ6PUg4QUs8RlI5U11BWmdAV2FES1Y8TVg8R1E9LzgyMjw1P0o3RU48R1Y3OkkzSFY2SVg9Tl8/Slw6VGVDXGtHP005N0M3PUo8S1ZDSlVBSlRARVBB")
    append("Tl1ITFpISldJPEpDRVNKN0VLNkJIO0dNQ1RGT19LVGNLUF1JRFJIQE1ER1VLMkA8O0o/RFVBPk8/SVpFT2BITFxLTlxETFxER1ZFJTQzKDczNkc5OEo8N0g4")
    append("P05CQU1ASVNRLT42Nkc6P1FBQFI8MEE1O0w7Q1RBM0Q7MUA1Q1NBHi4vLT45KTo0FScxGy4wNEU6QU9BS1tDQ1VCIzY0QlU7OEc1MkA0OUk4Nkc2Lj42QVI7")
    append("O0k6RFNDT2BAT19GUGVJGisuN0k4SV4+Rlc9OEc4PEw7PlA3QVQ7Rlo6R1o8R1lCRlVAQVBAPUw4SVw8SFg+SFo6R1Y+Q1I9T15FRFM7Okk6T15JPk09RVU7")
    append("JTUvOEg3TV1HOEY7R1RCPEk8OUU6O0o4R1RHPko9QU4+TVxFRVI/PUo+KTo1N0g1LDwtPk81PUk8PklBOkc/NkQ5Oko5MkEyPU80PU06QlE8O0k8Q1M6RFNA")
    append("T2BBWmpRUmRBMD49Hy8xGyovMkM9FSYrGi0sHzAtPlE1NUYyW2lGRFI9WWZLRFJBQlA7Pks7OUgzPUw7OEQ5SVVDOUQ5Pkw2UF9NR1g4OksyOEg0KDkpTFZD")
    append("XGZZR1M+Slk9R1Y8SFRCQ1E8LjgwQlE6QlM3YHFEXGk9YGxFSlNEQEw7RE89Qk4/VF5GW2VEUVw8TVw6RlE3R1E9RE4/R1JBOkQ3QUs4SVQ9RVQ4RFM5PEo0")
    append("SFc6UmFATl84S1s6UGA9PEg3NUE3PUk6RlE+RFA6P0o7QEo5PE0/OklBQE1EQlFAR1hESlhLQU9FQE1ITV1GWWhOTFxFT11IPEpDQE5CNURAPUxCOUc8Oko9")
    append("SVpCT2FFSltFU2RMUmFERlZAN0Y8OUc8QE89PU09PE1AQ1RBQ1M+WmRYICw0Lj07PU0+OUg/O008SFpGO0s8MkM3MEE5MkE3OUo/GCksITE1Lz85GywzEiIn")
    append("JTQzQVBEQFM/QFE/PFA7QVA8Lz0xPkw6SVlDRFU/M0U4QVI7P046S1s/TV4/SlhJSV5AHjIuOEo4QFI5QVI5O002Pk4yRFc7SVw9R1k9Slw+T2BFSFhFRldD")
    append("S1tBUWNATV1AS107SldAR1Y/TFtEWGhLLjw0RFNBQlE9OEYzP1A5QFA4R1ZCPU1AOkc4N0M6PEk7Q1I8PUs5P009PEo7P0w8SVVGO0c+MUE3NUQ4P1I0OUc3")
    append("QEtCTlpFKTYzLT00LD0zJjYvPEw0QlI6PEpAQlQ7MEE0OEc5UmZAWmhOR1ZAHCsvIjEzGSkwLjs6GSosFCQoIzUwOEsyVmhFY3BSQk86T1tLRFM+PEo9Okcz")
    append("N0UwO0g4Mj8zQU1DO0c6RVU9TVxISVk9Pk87PlE1S1lJRlY8W2pJQU48UmNBU2M/SVg+R1U8OkM9T2U6VmREW2w/W2pDRlE/QEs5RlA9RE8/VGFFWWVCR1I6")
    append("OEMxSVc6NkEyPUg5TldMSVNERE87XWlFTVZIVmRDTVlHU2JISFk4TFs7SVgzT146UGE7TVw9N0M2MTwzR1M9SVU7Qk45RVA6PE09Pk4/PU09P087RFM+Q1I8")
    append("R1Y8QlI9VmdET19DTVxATVpHSFdHO0pBPEtBPUxBPEw+PU0/UWNFSVtDRldDUmNFUWE/P048OEY8OEk/QVFDNEQ8QVNBQ1NFTFtJRFBGGicsIjEzNkY8Kzg1")
    append("MkM3PEs/KTg0Kzo0MEI6LDw2OUk+KTs4GSgvMT87HS40LD42GiwvQ1NFM0Q+NUU2RFU9LDk0MkE1OEY4P08+SVlCRVk7Pk49P046O0w2RVU+Q1BCQ1Y6P1Q5")
    append("QlE+O0s4RFM8QVQ4S105P1I5SVk9Q1M9SFdAS1pAQE49RVc8R1Y9SVo6TF07UGE9Tl1ESVhAQE48RlNASVRBOEY5QVE+OUg0QFI4RlY9QFA8SFdCNkM3Okc9")
    append("OEY5N0Q2PEo9PEs+RlRCOkY6QkxBP0s8MT80Lz47LD8pRVZCQU1EO0Y5Okk7MkE5LDo0KzsvN0g2RFQ8PUpENkgzN0c7TV48VGVBV2VPMj49HiwyITAzFycu")
    append("HSowHCosFScsJjkwS10+Tl5DVWRFSVdESlVEQE84P00/OUY1PUo3PUs3QlE7R1FMQEw8Q086OEU0Pk82RFY7Pk8xWWVVTFw/UmFET19CUWE/VGM9VmRFS1k/")
    append("RlNBUWc6WWs+X25CTFlCQEw4RlA7P0k8Slc7VWFETVo+PEY1R1I3OkU1Q05ATl47V2c/SFM6VmBFZWxWPUZDPkg9TFxBUGFFS1w2QE82R1Y0SVc7R1U6Q1I2")
    append("RVI6RFA+TFhBQkw7S1k+U2JCNkY7O0o8Q1RBQVI9RlQ9Tl4+UmNBTV8/UWJBTVtCRVM9SFdESVhHP01CO0o/NUM6Qk9APEw/R1hAQVNBQ1JBUmNCQ1A9QlFB")
    append("O0dBR1dIQlNFSVhFMkM1NEQ9P01GOEY+OEZBMz9ALTs5LDo2OEc6MT84MT89JDExIS8wNEU9NUM2ITEyJTQzIzI2L0E6RllNLz82PEpDFyQtOkg3TVxAKzcz")
    append("LzsyO0o7QE5BTlxHQ1M5RFVCSVs+N0k2Q1M/OUc5RVZBSlw6RlZAPUw6RVg9S11BOUs1OUw0RVU7Q1M9U2JJUF9GSFdFS106SVo7S108VmZBUmE/TVtGSVVK")
    append("NEE2M0E1QEs9QE8+OUo3NkYxQFE3Tl5DO0s5PEo5PUs9Okc7Lz00OEU6PUo+QlI+O0g6PEg2Qk48O0c4O0c7Hy8sNUcxQFM4QUxELTk4M0E7KDYwN0VBMkEx")
    append("KzoyOEg5R1hBRFFIRlg6TF86T11BPUpEJzM7IS82Hy0zGCgtFSIrFiQqFyktQFU6V2hIT19BU2JKVmRJQUw7PUk2Okc2Pks9Okg0Rlc8NUMyVmBcQk07Pkg+")
    append("PUs4P000PlEvPEo3UF1DVGNGRlRAQk87UWFAT14+SVZBPkw0UmU5XnJFXG5ATVw+RVI9R1JGR1BISlVET1w+UF0+Pkg2SVM8RlA8SFE/TltGR1Y1T185VmM+")
    append("anNYYGdWPUY9S1VDPk00SVk7SFc5Rlc3TVw7S1dBRVBASVY9Slk9Slc9U15GO0U2Slg8TV04TV5ESFhDT19ETl1DTVtEQVM8Q1U/TF1CRVVAQlJAS1xDRFRD")
    append("QlJFPk1APk0/P009O0o9RFRCRVZBTV5ETV5CT18/SFZBP05EMT09OUc8Pk0/QE8/M0I3PExBRFNHNUQ/OURCKzY4LDk5O0k/KDUzOUc+Ljs7MD46KTk3RFVJ")
    append("LDo0JDQyHC0uKTk3TV5GUGNQTV5GLzw+GicsMT8yRFQ9MUA4NUI1PEw8PEs+TlxMRFQ+QlE7RVY6R1s/O0o7OUc4Q1Y/UmY/R1o8PU5ARlg9TV4/Oko0RVc9")
    append("P1A3RlVBRVVASFhERFNDQVE1S1s5V2ZKVGNHS1hFO0Y+NEA2Lz4yN0U2MkA2Lj0zP08+PU01S1w9QlI7OUg5QE5BOUc5RFM9QlA/NUQ3NEQ3NUM1QU87O0k1")
    append("PEc7QE07OUU7NEQ4NEU3NUQ1MTw1JTIxNUM9QE88KDYuOUk4SltLLj81UGRDM0I3UWU9Rlg4WWdMOUU+Iy85His1HCoxGCgsFSQtGCgwGCkuO0w2XGtNUGA/")
    append("VmRRQk86NUI0QE0/PEk2RVRARVQ9RlY8NkI1RFBFPUo6QU88QlE2M0AxNEQvQVA/U2I+VGFJRlU9QE48Q1M7RVU7RVU4TV44VGNCW2xAUmA/S1k4TFhBTllI")
    append("SlVIV2Q/V2VATFc+SFM7RE86Qk07UFpBTF07UV88XGlGV2RCXGpDU2BAU19BP08zU2Q/SFc4Slk6TF04UF08R1M9MDkyMjwwR1Q8TVhDT1xCUWA9SVRAR1U0")
    append("SFZGS1pIVGJHQ09GSVZPN0Y7NEM5NkY6NUQ5PU09SVpEPk9AQlFDQ1JCP08/SVlEP05ARlZBO0o+SllDTFtCTl5BT11HQVBEJDExLz01JjcyOUk+QFE/M0I4")
    append("NUM7PEhBPUdHMD45OkhCN0Q/Kjg1OEU+LDo5Kjg4NEQ9O0tAIzIzFiUpIzUzLT85T2FNSV5DRVVBHCswMkA5JTUwNEU4MkM4PUs+MD00Q1FARVRGRlc+T19F")
    append("PE06Oks4QlI8QlE8P1A/S2A5RFQ8Okw+QFE2Pk8zQlQ4Nkc3R1s7SVhER1Y/Rlc8RlY7Q1YwUWE/SVZAT1xMSVdHPkk9PEk2QVM4QlM8N0Y4N0U2LToyOkk8")
    append("Qk08PEc+Ljs4Q1BDM0EzPUw4QU49QlE/QFA/RFM7PUo7Q1I7QE05RVNBOUY6MD8zSlhKMUAzM0E2SlpASFRDRlU9R1VAKTkvPlA9PVE1OUg8TF86S146Q1M4")
    append("TVpJLzo/Ii45HCozHCsxFycsFCQsGSowIDMzS1tCT19BTFw7Ul5HOkc2Pks6PEg5O0k2QU85PUs4O0g4NkM0NUMvRFJBP007TFlCRVNDOkk6TV4+U2FDVWFQ")
    append("SFVAP0w7NUQzOko0Sls6TV85XW1EUWI7VmNISVk2Slc/VWRBVGE/W2c+SVY6UFxFTFg7SFFCS1k+Q004TV03TVw7YG1LW2pBVmNAUmE5SlY5VGY+S1o9SFg6")
    append("R1Q4S1o4PEQ1OUI2TFg/Mz4wOUU1LTgtMDwuPk8yUFxCRlM1SlVNR1ZDSFdFNkFCN0NALDk6LTs3MD85O0w9O0w8OEk/PE09Okk9Q1NCTFxDRlZDQFA/SVpE")
    append("PU4+R1ZBT19DUmJFQE9BOkg+OUc+Kjo3OEo+Nkg7Nkc6QlNCQ1NGQU0/PEdDQlJCLjw5P0xGOUhBOUU+O0hBMj9AIzEzKzo0GCcrHi8uJzk2OUpESllKPU88")
    append("PU06M0E5QVFAS1xCP087QlM8RVZAP00+VmJYPEs7NUc3QlQ/MUI0Oko3QlI5RFU8R1k9QlM6Pkw4JTUwTmM3RVo5PVA5R1o9RVg7SFREUl5OTFhES1lDSVw6")
    append("QE83S1o/VWFSRVQ/RVFDPkw5PEw2N0c1O0o4TlxDKjgvLTo4NkBBOEJFLzo4RlM+Slc/PU03QVI8O0o4OUc3Slc/OUY5TVtESlk8P0w+OUc3M0EyMj46OUs3")
    append("RVg5OEY4NUQ0S1w/PEo7IzEtKDcyR1o/Oks/TWI3RFc2XmxRP0lAISw1Iy86Hio0GiovGigvFCcsFikuL0E3UWJBQ1I9VGJAPEg3RFI6Qk48OEQ2PEs5QE86")
    append("Pk01MDwvNkMyTFdEPUw5Tl1HU19PRlVFTl49UWE8U2JESlo9Q1A9O0k5PEw3QFE5RVY4Q1I3VGc5SFg6PEs3UWI8TVs5Wmw7Wmg7TVo3Qk84VmVGS1ZCQ0xB")
    append("R1Q7R1M4Tls7UWE8Q1A3RlM7S1VDTF03UV89OUQzN0QxTV06UmI8TFo4SVY3QlA0O0k1LTcsNkEvO0c1M0EuMT0sR1FBQU82RFBGTV5BQU9AOkY/RVFEM0FB")
    append("MD46MUA4PEw+NEU5OklAQVI7QlRBS1pFTFlGP01BNkY6NkU9RVZBRVNBTl1DSlhDQVA+Pks+NkU/NEU/JDMyNUY6MEE1O0lDQE9EPkw+RlNLT15LNUU5NUQ8")
    append("OEY/Ljs4N0U+OEVDLDo3LTs0MUA8HC0xJDQ0O0lGTltKPEs5Oks1QE8/QlQ/U2FMRVRDM0I1R1g+MkI3QU9MNUY2KTgzP1I/PU44QlQ7RFQ9PVA5Rlk4QE87")
    append("OUg5EyIpRVk6UmhAOkw6P1E5RFM7TVlJUFtJTVZMO0k3RFQ8Q1I0SFc8SlZEPks6QlA7NUM0MkAwO0o0QlE6P0w6N0Y2JC8vIy0zJjE1Pk46UFxGR1U/NkQw")
    append("NUUxNEQyPEwzRFI+RVI+Slg+QU86RVI8PEo2NkI2MUAxRldCQFE8IzEtM0MwQFE6Q1E8PUw3NkU/PEw4S187SF02R1k5VmJQP0lBJzA5Iy87ICw1GiovGSYv")
    append("FikvHDAxPE05T189SVpAV2NQQk87QE42R1NBQVA4O0o4PUw1MD4tNUMzPUw1VVxUPE43QE49SlhBVGdAUF8+TVw+UV9KRVI6SFY+Okg3Okk1PEs2PEw1Xm5M")
    append("PEsyOUc0Rlk5Vmg+VWc3TV8yTl04RVQ1SFY8TFw+MDs0R1I+QE03QUs9Q002Q1E4RlI8SVY9QEpCO0oyOkc1Qk82UWI7U2Q7U2JASFY6SFY6OUUwRVI4PUo0")
    append("PkoyP001SVc6PUozR1FAS1ZASFZGUFtLPUk/QU9BO0lCM0I/MT45Kjk4LTw5PEw9N0Y7MEE1Q1VCQ1JERVJGNkI8LDo3NEM/MkE9RlRCP007T11HS1pAPUo/")
    append("QE9IQ1RDM0Q0MUA2PEw6OEc+P1FETVxJPktCQU5BQ1I/PEo6RlVCNEI6OEU8Mz8+Hy0wIC4vMD8+FyguJTQyRVRKS1pERlhDLkA0PlE8PE84R1RDRFQ/Lz0z")
    append("Q1U7JTMuM0E8PU49Lz81UGFIPEs3SVxAP1E+KjwxO0s0RlVAO009MEA8OEc3Sl47NUY2Okk5RVI+Q1A9UmFFQkxERFY4TV87SFc5TVtGRlNCPkk6QEw3PEg4")
    append("JDAoM0MuN0kzRlQ8PU07OEU4LDkyNEI1QFE3Tl5FSFhAPk03Pk4yOko1Q1Q3QVA7PUs9N0YuQE40Okg0QFE4QVI8QFA4P09DM0I6OUo1Q1Y1QlQ4S1hKN0c2")
    append("IjIyJDUyTWU5TmA+U2Y+QUxBQUtIJzE7GScxHSgyGSguGygxFikwLUA4SVs9TVw/VGNBTlpISlg9QlA4R1VAOUc0Lz0tO0kzMUAvNkUxQFEzPEo5OkwzTl48")
    append("UWE9SFg3RVM8QU47SVdCUV89S1hBQlE7PE01Okg2RFQ4R1Y5NkYxO00yS102QlIzSFoxSVoySFc2QlE6SFY8U2Q8RlQ8RlI8OUQzS1g+SVQ6RlQ5SFRDQUw8")
    append("Q1FBS105U2BAQk83Slo7TFk8Slc8QE02RFI6P003PUg0OkYwNkIvRVI0QEw2RlM5PEg0VV9FOEc9RVBHNUM2R1U/T1tRP01AMkE5Kzs3JjYxOko/L0A4MEA1")
    append("R1dEO0s/N0U9OEU8M0A8MT89LTs6MD44NUI4UV9KS1pCNkQ/OEY9Pk8+OUs4QFE8JzoxRFVASVpHRFFEQE89Mj83PEo7P007QVE/P08/OEU8Okc+OUY+JzQ4")
    append("LDs5FSIpKzo1Q1JCR1c/Pk5BOEk9LkEzQlQ7QlQ8Q1M/OUk9P1A8O0o8MD82TVpHRVc8UGJDTV9AUGNGPlA8KTowPE42PU09KTozJjUyLDozOUk0OEY3QFA8")
    append("QlI4UmRBSllDR1U/Slw5T11BRVQ3Qk89RVE9Q046Q08/O0g3Lz4wLz4tNEUxOkg2LDwyLDsuP084MT8vQFA6TF4+SVc7OUo3Lj4tOEc4SFk9Pkw2QVE6QlQ1")
    append("RVM6QE84PU41PEs1PUs3KDgzM0M2PE42RVY5OkgyRFI+PUpAHy8vIjMzQlg1RFQ6VmlBRlc/OEBGJTA5GScwGCMtFiQrFyguHjIzSl5BSVk+TFw8Q1E7S1k8")
    append("Tl48RFBBRFI8Okc1NUIyO0k3Pk81RVM+UF9MQE8yRlY2Q084PkszPkw2RFFCQk84Slg7TFg+VGNBR1U+NkQwOkoyQlM6P0s+O00yR1syRlgzR1g1QFAvOkoy")
    append("RVU4SVg6UmE6Slg5Slc+RFAzPkszTVhASVM/TVs8RFE7RlI+TFo7T143RFE5S1o9T189UWA+Tlw8TFo8SVg+RFI5RVE4QU03Qk83RlM0TVo5PkszPEkySFM8")
    append("OUk+QE8/Tl0+VWNIUl9URVFEOkg7MUI4LT0xNEU7Kjs1IDEvOEY8Mj85Kzk4Mz88Ljw8JTI2NEM7Lz43MkE5R1dCQVBBNEI/LTgzRFRFOEs6NEc5TF9EOEg7")
    append("N0Y7N0U6PEw7NEI3Lz4zNUM1P049QE89PEs8RlRFN0M8KDY6IzEyHCosKjk1R1RMQlJALzw7QE9AGCkpOUs7QVQ9PUlDNUI5OUk8Pk09NEQ5XGlXWGs/WWxI")
    append("Sls9RlZFQFE7QVM+PlA8QFE9N0U3MDw4KTgwKzovQ1JCQVI7S106UmM+SVk+QE46S1o6QlA7QE83SlZGSFFAQEs5QU8/N0czPE43OEgyNkYwNkM0NUM2NEYx")
    append("N0Y1PU0zSFg3UmE/SFY6Okk5OEoyPU07Oks7O0k4S104Oko0Pk43PUw5QE82M0IvKjcuMD4xM0A9OUo0P086O0c4SVlAQUxGFyYsLTs6OUwwQ1Y1R1o9X2tX")
    append("Nz9DIi02GyYxGiQvGCUtFCYqM0Y4VWdHSVg6UGA+RFBARFQ4T188TVpFQFA2R1U+RFE9P087N0YxSldCQEpFRFE3SFg4QEw7OUU4Q1A9S1dKSlo6S1s8UmI/")
    append("UmBAUmFEQ1M2P1A0OUo0S108U2Y+TmI3P1ExOkkxO0o0O0szRVY1Tl86SFg3SFc3OkcvO0gySVc6SlU/S1c8UV09U10/UF4+Tls8VmU8TFk7TFs4T107U2I+")
    append("Slc4SVc8Q045QEo1QEk0RFA7SVU3PkozN0QvOkYxQEw1Pko2TFpDTFxEVGJMS1hKRVFKPkpDO0g9KjgzJDMwLkE3Nkc4Lz86NEQ3Qk9BQU9FP0xDKzk6Kjg6")
    append("Lj04Ljw2LTs1Q1M/PEs+NEI7LDk1OUg9PU88Kjw0PE08P08/MD81M0I3OEY6OUg5NUU4O0o4QlE/PUs8PEk8RlVGOUc8IjEyHi0uN0g8NEU6SllONkZAOUhB")
    append("QFA/Kj0xOEg1QlA/PUhDN0U7MkE7M0Q3Nkg7TF1ETl87TV8+O0o3P049RlY+O0s7M0M3SlxARVQ/O0dBOkk8N0U3OUo2R1s7WGpATl4/O0o3R1c7O0k0Okc0")
    append("PUo3UFtIPkg4Slc7O0o0MT8vNEQyN0gtM0MtOkg2MDw1PEszQ1M6TWE9VGVFVWFJQE06MT8xQFQ4OEc3NEE4QE0+QlE6Q1M5Q1I1P001M0EwN0czNkQyRFQ8")
    append("PUpJPEwyNUM3TFs/Qk5CRlNGJTUwM0E4MUAxQ1k2QlU6Y2hgLjg8IzAzHCgzGCUwGCkwIDIuUWFLS1pBRFI3TVtASlZCT148RlQ5PUw0OkozO0k5NEExNEU1")
    append("OEUwUGBBQ0xANUA3SVo5Pkw2R1dBQ1E6VGJCSlo8SFc5UF89Tlw8TVw6Sl05PU00R1c4R1k4T2I9SFs3NkcuP04zQlI3UWY4SFk3S1w2QVEySVc7SVg8R1NC")
    append("Q1A5SFY/SFY6VGM9UFs9UF08UF1BPEowRFIzSlk4PEczS1g6Ul5BUl9BTlo/UVs9R1E6UVxBUmE7Pkk0O0cxMz4vPko2P0w4RFI+VmNMV2NUSlZKTVpORVRC")
    append("PUpBNUE4NEM0Kj0wNEY3RFU/UGBEUV9IVGFQRFFBSFVDP0tFMEA8Lz04Lj02NkQ5QlI/N0g7MUA8MEA3LDw1Pk8+NUY4OUg7PEs7QlM7PUs7Q1M7Q1M9RFM8")
    append("SVlCQ1M/SlpDQlFCP04+NkY8GigtLDk3MkE6Q1NFLTw6PFA/Nkg4MEMyNkc0Qk9COUQ+PEtANkQ7NEQ3KjwyVGhGTF49Rlg5PlA4R1g+QlA/NkQ2PE04R1g/")
    append("SFs7RVc8OUc8Ok00Oks2TmI7UWBDQ1M8UmJBSFo7Pkw4Q1E2TltBUF5APko5T1xFPk00LzwsNkQuS106RlRBQ05CKDQxNUMwPEwzRVY5Ym5aT1pHQU47VmdE")
    append("Okw0PU41Okg1QVE7O0k7OUgzQlI1Pk4zNkYyN0gzNkY4NUU2MkI9Lj0sPk07QlM2Ul1VNUM1LTw1NUM3NEI9O080O003XWNiOUg6KjkzGSgwFicvFigvKzwz")
    append("TFtLS1s9VGBIU19GRVM7QlE3QE83P1A2Oko0Okk1MUAtKzstRFI7UGA+WWJSPEg6SVk8SVo8UmJFVGVBR1Y4Pks1VWQ9UF8/Tl47RFQzRVc3SFo1SFk1RVc3")
    append("WGpBPU40PVAzPE0xTWEzS145SFg5P04vRVQ1QUs9VF5KRlE9T1w9Qk83RlM2U2I7Ul88UV4/R1E9RlI7R1M6PUk1N0ExPko0RlE8SlU7SVU6S1U8TldCT1o8")
    append("Q001P0o1OUMyMT0sPEk0NkIwSVZCWmZXTlpTSVZOSVhLQ1BDSFRGP0xBP1A5MEAyQlc6Tl89TlxKUV1MV2RRSFdEUl5JUlxMN0Q+MT45NUM6Lz02RFVANkg6")
    append("LTw5NEM6IjIwN0g5Nkg7Pk4+MEE6RFc6NkY4RVg8Slw+Pk45Sls7P086SFg9QVI8Oko7NEI9HywvJDI1IzI1OklEM0E8PVA+Lz8yNkc1PlA7PE07QE47RFJC")
    append("NkQ7P04/N0c3TF1CUGI+OEg2RVg7O0s7PUo5Pk87RFc5QVI9Rlo2QlUyN0cwOlAxRFY7V2w/VGU/TF48W2lNUF5ARFM6TFo7VmBPSFg5Pkw3RVQ8QE42PUo4")
    append("O0syP083RlFHRFM7P080QVE1QE8zSFY5S1dJOUM5Lz0xUF9HUWM7S1w4Rlc6QlM2QExAPk46QlM2PU8zNUcwQFI1OEk3MkEyM0E1MUAyOkw1QE84TVdTLjo1")
    append("Q1FAMD02OUY4MkAzPE02R1NMOkc/ICwuEyIpFSYtITQ3RVU+UGFEWmlLWmVQTFhCSVo5PEw1RlY5N0cyRFU5PU42PlA3PlAzRFQ3Rlc2XWdRTV09Q1I9Tl08")
    append("UF9DRlc6PEw2QU83TFwzTl04UGA5TF00R1g3R1w0Q1YzPEs6QlM1LT0tRFg1RVc0Rlc1R1U0SFg1Tl4+SFg3RVM5SFQ/NkIxT1o/S1g6U19AV2NDTlo9Q04/")
    append("KzQqMzw0MT4tSVk4SVo3S1k4Slc5SFU5T1o+U11BUlpCVl9FTVU8QUo3P0k5NkMwLDcrMj8rV2RQUV1SRlNHSldOSVZLRFFFTltLRFJGQFA+PEw4Q1Q6M0I3")
    append("OEc8R1NGRVJHSlhCUFxHUl5KTFhGQlBDM0M5NEI4OEg8OUs5KTkzOks/M0Y5MUI1Lj01QFE4OUo8OUo3OEo4Q1c6RVc3Oks0PEw2Oko1N0Y2PE43M0M2Kzo2")
    append("KTU0MT08MD8/O0lDOkk8Pk9AMUI1PE07OEszPE40R1k8S1pDPUtAQlJAO0g7UmNCSVo+PEs5Okg4OEY7Slo+PU45NEQ0R1k+SFw2Q1YyRVkyRlw3UGQ+VWc9")
    append("U2c9S109TVw/UV8+SVg7UmBERlI+SFc4Pkw3RVQ7Q1A5QU86R1g8S1tBT1ZOS1s2Q1E3OUk0NUUtQk85QlA6P0o+PUw2OEU6QE83RFE5T19AV2k9SFZBUF9F")
    append("O0s1O0w3OUo1N0gzKTgxKDYvNkY1Q1BHOkwxQE48OkVCPklFP09APUk7KjcxKTUzO040SFVJKjU1JTIxFCMpFCUrKjw7Tl9CTV0+U2BKV2RIPks4PEo4N0Yy")
    append("OUczRFQ4QlI4SVs/O002SFg8T2A/VWk8WmhGSVs3QlM0VmhARFQ2QVE0PU02Slw4U2Q8WmhCW2hASlc6TmA4VGY4RFgyS11ASVk8Sl08RFU1O0kyP08zQFAv")
    append("SVo2RFQ2S104QlEzQlE1RVQ1TFo7VGM9TVo+UV1BN0MwO0g0PEoyRVU0S1w0Tl83TmE4UF86U2A9UVw9TFY+SVM6T1hCT1ZEPkY0R1I9Pko4PUo0LjcsMT8r")
    append("R1VKSFVKSFZJSlVJTVlKRFNCSFZGQE9AO0k9P047Pkw7P04/MEA4MT85Kzc4OUc7RFJAR1VCUmBJUmBORlVFMEA3MkI3L0EzPE44R1hBQlI+M0Q5NkU4N0Y1")
    append("O0s6Pk06Lj4vNkg0NUcwNEYwPFA3NEYyKzoxNkU2MD4zLDs0Ljs6KTY5LTo8PEw+R1hCOks5OUs1RVg9QVc3Rlg0N0k4PE46N0U7PE06RVRBTWI9MUA1NkQ3")
    append("M0E3RVY/P085OUk3QVQ3U2VBTmI2TWA5UWc2UWc8TmI7T2A7TV49TF46UWQ4UV8/TltGUmBGR1Q7QlA0Slc6Q1E5QE07PEs0QVE5Y25eRkxKQ044MTs1QFA6")
    append("QFM1R1g6OUQ7ICosNUE1Qk0+LjgvQlA5SldETFs/Slw9Q1M7PEgzLj0vN0gxMkUuM0cwOEsxQFI3NkM3MD4sOkozNUI8PEc9RlRCOEM6NEE6Ljs3N0k1MD04")
    append("QU0+Kzo1FSYrFCUpPEw+XW9KWGlDS1hFRFQ4SFg6PEg5P041Pkw1Q1M4RFQ8Rlc9S1w9T2E+T2I7VWdDUmQ9Pk4vSFw0RVU4SVs1R1c1U2I7WmdHX2tMU2BI")
    append("S1dCTFk4T2I1RFQxQ1UxQFI5OUoyRFY4Pk01QU83R1Y4SFg2UWQ7Tl87Q1E3R1c2RFM2S1g+QFAzSFU5RVA5QkpAOkM2NUEwO0oyQU8yR1Y0Vmk6S1k3SVI7")
    append("RVE6WGFMRE8+T1c/VFpGTVRBSlI+Qks5PEYzOkY1LzssLjosRVNJRlVJQ1BGR1NGP09DPEs/P0tGRVJGP00/PE06QVA9O0w9PEw9Pk08Oko7RFNCR1ZCQlE/")
    append("TFhKSVZOTFpLTFtCLz40KjsyO004TF4/Q1M+RlZBN0Y3M0M3NkY2NEI2N0g1Okw0MkQwNkoxMkUxL0AyM0M1Lzw0KzguKDcxNkRALTs8Lzw7NEY6R1dAPk85")
    append("P1U0OEw2RFk3R1s6OklAMkIyOkc8OEg7UmFKR147R1k+QlFANEM1NEU1OEk4R1o/UWY9Tl8+TF83S148R1o5UGQ8Vmo+TWA5SFk3U2U7VGY7S1k8VWBMSFU+")
    append("R1Y9RVE9QFA0OUczPEszMT8zN0gyTFRMPEU9SlNJPEk6S1o8PEw0Okc0N0Q3HywpQE01Pkw2PEg4UmBDUF9FQE45Sl06R1Y/OkkyNEQ0KTssOk8xSVw5P1Qy")
    append("P086QE48OEU7PVE2OUc1R1M+VGFIR1RDRlNAMkE2KzsyNEM2O0dAJjYvFScsKTwzRFVAUWBETF46Xm1UPk05Pkw4PEg4OUg0QFA3RFQ4RFQ7O0w1Oko2SV05")
    append("TWA7Slk+S142Sls1U2Y3VGU7WWs/Slo6SVg7WGJOYmpXRVE+QU09QE00QVIzNEMxQFMzQVM0SV04R1k7R1c3Slk+UWE7SVg4QFAzSFU6SFg4SVo2Pkw0QUs8")
    append("SFVCSlJHLjIzOD08P0Y/PUY5SVk2QE8xTl43QE8wPEcwQk43TFs4O0I3Mj8uREw4VFhHUFZERUw8OkM0P0o2OkYzPks2N0MxR1NJQlBDQE5CQ1FDQ1FEQU5D")
    append("PklIPUlAPktBO0w7OEc6MUE1QlA/P087QlM8RlVCQ1I+TVtMT1xPSllKRVNDRFBBOEY5KzozM0IzSlpHYHBMSFZARVM9Tl9FPU86NEQ0LT4yN0k2PE81MUEu")
    append("JDQrNEYzN0g0N0Y4MUI1MkA7OEY/MD45QVFCQVBDSFhAOk43PVEzUGY9PE42QFE6PEs9PEs6Lzw3NkU9WWhVQlc8RVc/O0k7Pk84Oko4Q1c9W3FKT2NASFk5")
    append("T2I5TF86TmBBUGM9WGw9T2Q4S180UmQ7TFo6TF08TlxCQk87P042UVxNOEYzNEEvMj4xMT45OEkwU2BLRFI6RlJCTV44TFk7N0YxOkQ7QVE1PEs1NEEyN0Yt")
    append("UGI+SVY9N0Q2OUc2T2E6VWRFSVg+MD8xMkUuOEsyMkEsNUcvRlg4UWBLP0tFQ1Q6O0Y8T10/S1lERFA8PEk5LDsxNEQ2OEs5NUU4KDYxHS8wM0Q1WmxKSlo/")
    append("UmM/Slk9TFpBPkw4Q1E5RVQ+P085R1g5SFg+NEUvOEg0R1k2QFA1T188SVo4TV83TF01Wmw9UmFBRlM9RVQ8WmdPVWJJPU03Q1Q7O0s0MkAxOEcyO00wOUox")
    append("Rlk3S1w6UWI8T186TF03UmE5TVw8PUsxQlExRFMzRVM5RFE7PEY6RE4/MDcwQE0zNUMuS1k5Tl87VWc8RlM7Pk4vNkMtQ1I0OkcyND0zPk0zSlQ7TlJBUldF")
    append("S1JBTFY8RFA5QE02QU04QE02PktEQE1DQU9CP0xFPEo+RVBHP0xEOkc+OkhCP049Lj03PU06R1ZCPU05PU07P04+SVhCWmhSU2JMSVlFS1dHO0c/NkM5N0U+")
    append("OUk3Q1JFQVM7Tl1MPk48SFVEQVA7UGBDQlI7M0M0NkYzKzktKTgtMkMxOUo1N0Y7KjsyN0g3JDIxJTQuJTUrTVtTP04+UWVBO040RVg4R1g/OUg2O0w7OEg5")
    append("P05FR1ZIUF9MRlZBP1A+OEk1R1w6U2dBR1o8T2BFSFo9TF86RVc2OkwzS147VmtATWA5S2A1U2c6RVg2SVc8R1k5RVU4SVdDP001P0s3N0U3PUo4OkU9KzYy")
    append("PUo3aHNcS1s/UF9GQE4zOkc0KzksRVJEPko2NkUyQlE2Okg0RVQ8SVQ/PUw4PEg5TlpETVw5NUA3QlE3KzwqRVo0MEEsOkwvQ1czU2JaSVNMT1xLSVhBSFZA")
    append("RlQ+UF1CPEg/NEMxM0Q3LDw0OUc3MD83KDo1R1ZFUmJBUF1CVmZCTl84QU82Pkw2TVtBQVA8QVI3QVE6Slk/NUYwRFY3SFc8SFc2U2JETl46Tl44U2I4S1s2")
    append("R1Y1SFY6LzwvOUY0Q1I3O0g2O0s3QVE3NkUzOkozOUkuRFU1TF05U2RDSls5Vmc7TV83TmA3WGlBS1s4Q08yRFI3Qk81QE03QU41Tl45RFI1U2Q8TV85QlIy")
    append("S1s4U2I7R1U3SVk2RFI4Qk40OkYxRlI5Q042RU46SlE8TFQ/QEg4RU81R1M3RVI2ND8uRFI1QU9EPUpCP01ARlNKQE8/RVRAQE87RFJEOUg7NkQ5KTg1MEA4")
    append("RVQ9N0Y2QFA9TV1EUmBHWmhJU2BIT11HQU5BOkg+NkQ5N0U9Pkw8NEM3OEc5M0E2MEA2RVRARlM8TFpBT19AQFA4PEw2NEMxQVI4M0QzNEU1OUo9O0s8PlA7")
    append("JjQzKDYtLDovQVJJN0k7Sl4/TGI7M0IySFdAOkg2OEg0OEg5QFFFNUQ4SlpCPU08NkQ0R1s6S186QlU7SFs+QlBCQVMzPE4zQVM2QVQ1SV05UGNASV84WGw9")
    append("UWM/TmI6SVg/OkoxQlI2S1pDQ1A8QFA1MDwxNT07LDYzNkI2SFNDbXBqQVE3Pkw4Mz0yQU84LDowJzUqOkk2MkAxO0c6O0g7NUE4QkxASVZAOUY0Pko2S104")
    append("OUY2PUw0Pk83NkguOksvRFY0OUowOkg2TVlLQ0xGRVI+Pkw9TlpGT1pARE5FQFA5KzswIzMsP0s/NEU1Okk6X21XU2NCVmVDQE41RFM1PUwySFc5P005QlE6")
    append("RFQ2PEs5Q1I3PU8zR1k0VGNCT182T1w+UmQ5Slk1SlozQlA0UWI6S103Q1U5PEo0NkE4OEUzMj8wRFU2Rlc6Rlg4TWA3UWU3UWBBQlA3SFk0QVEwTl43TV43")
    append("P0w1RFA4RlE8MTsxND0xPEYzSVU5RVM1R1U6UFxBSFY6VWc2S1k5N0QxNUMtOkc0ND8wS1NDRlA/QEo5OkQ0OEIyTFM8S1I9UFdBSVE6Qkw1RlA5PEYyPEYw")
    append("Okg9R1ZJQU5EQE9BTVtLUWJBT2A9S1tCQVI+NUQ4KDc1M0I5NkU1OEY3SVpAT15EVWJKU2FDV2NQT1xIQ1E/Q1FCRFNBRlNDP0s8QU48NkM4QU5BKDY0R1ZC")
    append("SlhFQE08RVM/TFtBN0Y2Q1Q3RFc2S10+TmBAPU45TVtFJjQyIS8sIS8sOkg/NkVBN0c6OUg6RFc4QE84SFk4Rlc+RlU+PEs9P05DPE04R1c8MUMvQE89VmtC")
    append("QFQ1QFQ2Pk48Pk0+QVM1PU4xR1o2VGg+SFs4SVo9V2o+WGg+XG1HUGM8TFtEMT8vQE08R1Q8RFM1QlI3MT0wKjIyMTwzLDgxSFc9P0k6PUwzP040Q1A+NkUv")
    append("LDwzNEMvOk0xKTcuMDwzLzo2O0c6Mj40LDcwQ088T11GPkw4LzwzO0o2NUMwNUQuNkcuN0gwOUwxQVM4R1M/TVdRRlE+RVNAV2NWNUAxRVE8Okk4HSsoJTgs")
    append("Nkc2QVI9Pk06TFpDUWJAWGtDQ1I2QE44SFk5TVw9QVE7SVY9SFc5PEs0QFEzQ1Q1SVo2W2s9U2M5TV42SlwySlo1S1s4Tl47SFc5RVY1SFw6P042P006P041")
    append("OEYxQVIwOEgvT2A5UmY5UmM/SFg9Qk82OkYxOUYwPUoyR1U7RlE+RlI6SFM+PUk1Slc7SFc2RlM2UGA7V2ZBYWtSRlE1Slg1Q1A5SVc9RFQ2O0c0N0MwRlI8")
    append("TFVERVE4QEs2S1Y8Rk85U1lDUVdAUFc/SFI3VF9AR1I5RVA8NUM9SFdHS1dOR1hBUmJJWmtETmA8TFtCQE87OUc7Kzo1O0k9MkA1QE5BQ1U+U2FKSVZBRFI8")
    append("SFVGS1lIQlA9Q1A/SVdCR1RBP0o5O0g2RVI+PktBLzs4VmNOTltKTltIP0s9NkU3NEM5N0c1Q1Y5Slw8Oko1SVs7O0g8KTYzMkA0Lzw0LD00LDk3IS8uM0I4")
    append("PVE2TFtCQ1U9NkU7TFlOSllKU2FWR1g9S1tAO0s3OUw2SV05TWM6QlU1OEY3Pk41RFY1SF01RFcyR1c5UGE9RlU/QVE3QU41XXBGWGlAVmVIR1c1Q05DQlE3")
    append("MT4vO0k1LzstNEEyOEJBMz8yPUo2P04wN0QyOUc2TFVDPko1JTQqLTowNUUxLzwuLjsrMT0yMDs3OkkzO0oyOUI7P0o8O0Q/JzMsLDorPUw0M0MvOkoyNEQ0")
    append("PlA3N0cxWWRUSldET1tLR1VCP0s9OEQ1R1Q5LjwyJDMvKTswOUk3XGlXQlI9T2A+SVk/UWFAVWc+S1s7RlU6SFU+Q1I9PEk1SVY6R1c7QVA0RVY4WWtDS1s2")
    append("TFs1Rlc1RVUyQE8wOkkwRFQzQ1E3P08yR1g4Pk04OkgzOko1Pk00OUgxPE0yRFM2QlMyRFM7S1o7OkgvNkIvRVI1SFc5RlM7RE89U11KTFc+SFU4R1Q7PUo0")
    append("TFk2TFg6Slg6TVs6O0czSVRCTFlDQ1A7QU4zQlAzRVM4SVU8PUY9Nj8yKDArNT80P0g2VFpCUFc+T1dBUls/Ul87S1Y4Ul4/LTs3OUY9PktASVpATF1BU2BC")
    append("R1Y6RVM/QU88MD44KDY0LDs5Lzw3O0s7SVpAVGJRO0k3P088PU08Ul9LQVE8QlE+Qk8/Q1BAQEw7PEk3NUM1Mj80R1RCW2hWT11FRlNCQlA9Pk46Q1JCNEM2")
    append("M0I0N0czP1E6N0g1NEA4P048PUk6LDwyRFg8NUM6GCYpLT0zTF4/UGBNTVpLR1ZHNEI5RlNAS1lNRlVBSFZCOUk+JDcrSV46N0kxPU41PU41QFIxTF82Rlk1")
    append("QVM0UGM5SFg1NEIzP0tERVQ5UWM+VmU/Ul9DR1U5Pks1Okc0KTUsP047LTsxLz0sO0c8O0gyQlI0P080OEgxLz0wQ0lEMDgzM0MuKjotPEo8OEgwPU8tNUMu")
    append("MD0yNEU1KzosPkc8LzowLDgvNkQzPU0xO0g4PU07LDwoNUUxLT0tR1Y8RE4+Q1E5RVQ+QlA9Q1E7OEU2Pk44LTwwPk09JDcvP009Okg7TV1EY3ZJXWtKTl0+")
    append("TV08UF5CQk07Pks1Q1A7Pks0SFg4RFU5RVQ5UWE+S1pARVQ2QVAvPEkxM0ArO0osQE8xQE80OEUxOkc0NEAtP080Pk00Pkw7LTsrKzgnMT8qTmA3TF07Pkwz")
    append("Mz8xQk83R1c4R1c2R1M6QU01R1Q6QUwyTls9U2A+T1pCTl09WWZFRU83SFM8R1M5PksyTFw/Pks1NUIuRlQzSVc2U2E9T1w7P0wzQ005LDYuLzgvPkc3VFxB")
    append("TFQ7TlZCUVtAU2I9SFY1RlI2Lz03PUo+QU5CUV9NTl5ES1k/PEs4QE48PUs6O0g+Lz45Jzc1NEQ9SFpASlpBQFA7Okk4N0Q5OUg7RVFEQlE5RlVGPUo8O0g+")
    append("MD0yLjswNUE1LzwzM0A1QU1BRVM9O0o4RFQ5NkY0PUs8OEY3NUM3MT8yOEk3MkM3SFVKQ1I9RFI+MkQ2QVQ5M0M3EyIjITApRFNFVWVOUF1PMUA2O0s5RVND")
    append("Pk1CQlBBSFdCIS0vITMrPVA7PU82NkUwR1k4Sl41R1k3RVkyUmVBUGM2R1YxO0kvXWhZQ1M3R1g2VWVBSlk7U2BBVGE+T1w7S1k6SFg3PEs1P1AzNEMvS1sy")
    append("PUwwQlE2N0U0IS0pMT0vLTYyJDArJjQqLz0yNEMsNkcrO0swRFI3RVQ5N0UxPkg8OEYxN0YrIjEoPk8zP007N0Q8O0wuMUEwPk41TllNTFZHQ088NkU2Pk45")
    append("P043TFs/OEY3JjQuLjwzN0k4M0Q4S1tESFlBVWZFW2tFVWQ8SVc5T1tHP0w4R1Y5P0s1RVQ6QlI2O0sxR1Y3QE41PEw0QE80MT8rNUEwPEkxRFMuOkktQE81")
    append("P001PEo0T107SVoySlk9Q1A8QU81PE8xN0cuQE8zMj4xQE05RFI3TVs3R1Y7SVk9RU85R1M4RlM4TlpAUFpCSlM9TVVBUl1HPUY5Nj8yOEQwQk42SVY5U2I7")
    append("S1o3UF47U18/WGRES1k3SFk1PEkvN0AxQUw5QEo2S1JAQEY7QUg0UltCT1lBTFs6PEowSlc3MD42M0E4SVdJTFtGT15BR1U8QE85QE48N0U4PUk+NUQ7LTw1")
    append("MkM7Rlg9QlM4Pk43OEY6OEY6Okc+O0g+NkI1QE1AN0U0Okc9NEE0Mj80NkI4PEc8NEA3NkE5MD4zMkIxOkw1OUo2O0o6QE09LDkzN0U3QFA4Nkc8S1pPO0k5")
    append("Oko5M0IzSFk8O0o5M0MxMkExTVdUSVhIPkhHHSksKz4uLz4zJzQxRlVGQVA+Ii0yJjMuNUY0M0MwRFU1RVY3SV41RFU0SV4wQ1Q3SVs0SVg2SldAOkc7O0sw")
    append("RFI1S1o4QE44SVc6T1pASFU7UV5CU2I8VGM8TFw7SVk2QFItOUctPE0rIy0sIy4uQE82LjkyIy8qKDcpJjQrMUEsOU0vPlIuQ1ExQU40Pks0PEg0SFg5M0Ew")
    append("KzosLTowO0g5Qk8+N0YvMD8vLTovQk87T1tOR1Y4LDkxMkIyMEAwPUs1MT4wJzUvLDozIzMuRlZCaXRjRVQ+VGJBUGE8VGQ7UmA9TFg9S1k7QE41UWA9SFZA")
    append("Q1M5TV05T148OkkzPE4xLTwuKjcrOEYyQlE0UWI3RFUzSVg5SVg5S1w3R1c3PUwxO0cyQEw4MkEuLz4sQVEzQlA3UVxMRE48Slk2S1w0Slg8RlY5Slg7RlM0")
    append("VGE/V2JGS1U/WGNIVmJBUVpFUl1AU15AV2U/UF07VWJESVc3RFIxUl5AUV09VmFCPUsxOkkuOUQwND8xQEk5ND0yNDsxNz0zU1pBTVY9Q0s9QUwzOUUxQk4z")
    append("OEY7PEs+RFFGS1s/RlU8RFE9PEo4O0g5OUg5Mz84OUc/JjQuPE1ERVk6QlM3NkU2Pkw7Oko1OEc4QlBCOUY5NkM2LDoxMDwzKzkxKTcyLzw1NkI3N0M2O0Y6")
    append("O0g2O0o2NEIyMkE0NEQ2LTo0JjMsMUEyPk01P045PEs6SlpCKjsyQlA9Okk3PUw3QFI3KDkwUFpRMz86HyktIi8oGyojKDYzFyUpS1tBMD42HiosLDs1MkQy")
    append("OEkyRVc1PU8zUGU5SFw2OEotS1w1R1c1SVk5TVpHLz4qPEsvPUo0Q1I1Pk00Slk3TltARVI6UV5CUF4/TVo7TVs4S1o4SVo1QVItQ1UwLjsxJzEtNEAuMD4u")
    append("OkgxN0gvKDcrMUIpN0ktQFItS1k0QUw0R1I4RVA3UF85NUIvNEIuJS4rMz41NUI4MD40PEs3N0U4RFE6PEk7PUs3NUM1Pk83MUEuQ1I0JzYrNUMyLTwvJzcv")
    append("SFdDYW1eR1Y9RlU4UGA6VmU/UmE9RlI3UV8/UF8+UmFCTVxDS1w9TVw7OEQxOEcvNUItNEIwPEsyRVU2SFk2VGQ8UGA6S1c+S1k8Slk8R1U2RVI3RFA2R1Q5")
    append("QFA1SFk1UGE4SFc4WGZHTFk8TVs4RVI0UV88UF87UmA8VmQ/XGlIUVpDUlxBWmRMXGdHV2FDS1c8VmJCUl8/SFY3S1k4Tlw2TV01RlQzSVY0SlY3Q1IyS1k0")
    append("U186XGdFWWFCWF9BUVpDTlZDVl5FRU86Jy8oN0IsMTorOUQtM0E7M0E7P00/R1c8Pk07RVRAPUo7Pkw7MD01M0A4NUE+LDsyP09CQFM0Oko2Pk09PEs2Slw9")
    append("UF9GSFhFQU88OUY2O0k6Mj8xMD8zNkI3MD80LjsuOEQyQE04RFE9P005IjArJTIvJjMvMD02LDkvNEYxOkkyPUw2QlM5Oko7OEc6NkQ3O0o2M0E0Rlg8N0o1")
    append("RldAKDYxLDoxOEg0LzwxKjQ2FSMqSVs7NUQ0MkIzLkA1KDksOk4zOk40Lz8rS183O04wIjAlRlc1QlEzS106O0k2Pk8sPEkxRFM0RFQ0QlI0RFI5R1Q/R1M8")
    append("SVY8Slc9Qk82RVM0RVU6QE81TF02SVc7N0YyJS8qPEksMTsxKzYsOUsvJjUmMkIuOkstQ1M0TFo3S1c7SlU+QEo5QU42N0IzLjouMTwyKzUzMz85NEA2M0Ay")
    append("NUE0Mz4xPUw7PUhFOEQyMT8wKDYqQE80NEYyLj8yKDgrKzovTVpHRlQ+T14/S1k6VGM+VWNAQE8zSFY2UF48UGA+UWBARlY7V2VHPEo1NUMuPk0yOEI0RFI3")
    append("OkczQU4yRVQ2RVM2Qk04Pkk3Qk04TltERFE3RVE3T1o9VWBER1Y2W2g+SVg4QlE5TVs9SFQ5UF05VWI9V2Q+WWhCUlw/SlY7RFE2WGNKTVVCVF5BVmBIO0Qw")
    append("P0o1RFE1TFhAQE4zSlk4Pk0vQFIvTl00SFUwTFozU2A5T1s6Ul8+VmBGSlBAQkk5Rk8+Rk4+SlJDQUw3NUEsSlgzUFs1TlsxO0o9N0U7QVA/P046QE46Q1I9")
    append("Q1E/Pkw6Kzc1Okk6LTs1KjkyO0o8PU8zOkg5P006O0k2RFU4RlM/R1VCSFRARFM+RlY+QVE6NEIyOUY3Okc2NUM0Qk87TVpDS1c/SFU6QE87LDsyKzkzJzUy")
    append("JjUsKjotMT8vJzQuOkg4Pks4Pko7RlQ+NkcyQlI/TmE9QFI4NkYzNEQ1PE04Q1JCRFE9LTowNUc2R1c4MEErNUUwHS0qESAjO04yP1E1MEAwKzwqNkkvIzEo")
    append("RFc2OUczQlE2Mj4xQFAuO0kvRVQzRFM2N0YvOUc0PEo4O0U2NkEzRlFCRVE6SFY4TVs5OkczQEw2Q005O0kyNkIyPUo3Mj0zMT02Lj4rNEYqNkkxOUopQ1M1")
    append("O0gzRE84Q008Pkg8O0Y4KjQvKjUvMz82NEAzMDs2Lzk0PEo2QE82MkAxNEE2ND06NkM3PEs0KDYrMUAvLTwsKTktITEqNEQzTVpGTFk/UmBEVWVAVWI9RlI2")
    append("OkcsT184Qk43RVM4UWA/UF88TFhFP00zOEYwNUExPUs1S1o5QU43QlA0SVdAOUQvQUw8Q048Q006QEo7P0s1SVY8TVpCNkEzPEgxMj8vR1Q5P0w2Qk46UV4/")
    append("WmhCW2hGWWRDVWFBTFY7U19BR1U2T1tDRlA5Q001OkYxMTowNkIxNEAsNEEuNkYrSlk7R1gzRlczTl01S1k0Slc2WGU9RU44QEo2R049PEU4RU9AMjswPUY3")
    append("R1M8SFM7SVc2SVg0TFoxRlQuO0k8P04/QVI9OUg5Okg1NUM0NUIzLjowLDk1MkA4IzErKTgzPUw+QFE3QE09Pk05QE46SFg7QlA8RVI9RlE+TFlCTFtDTFxB")
    append("M0ExPk06Pk05P048Q1A5UmFGTFk+RlQ6Q1I7RFI9LjwxN0U3OUc3KTgtGygmGScnMT8zPUo4LzswOkk5M0QyN0o4R1o5PU46GysmMD8zRlU9RlY/YW5YQ1JA")
    append("QVA7RlQ8QFQwP1IzGikpEiAkOUsxMUAsPk43JTUqPVA3Hy0lRVg8PUs0IjEnOEYxPk0sSVk0Q1M1RlU1NUItNEMwPUwxND8tOUU1PEc6OUU3RVA3SFI6SFQ7")
    append("MT0tR1E8N0MxIS0lPkw3Mj8wM0EyNkYuJTQmOksvOEgpPEsyN0Q0OUQ1RlA+PEQ5N0I3Mjw2LTgzKjUvKzgsLTkyP0s4NkIyO0kyMkAwQU85PUhAKTcwMD0s")
    append("GikjMEIvLDwsJTMnJTUsNkY0TFo+RlU7UmFBUF86VmQ+TVs4Qk8xUWA/U2BFU2JATVs8S1g+TFk+RFQ2M0AvN0QxPk4zRFQ2QU04TFo7SlhCPUczR1A+SFE/")
    append("R1I+Pkg3O0cySVc9Pkw0OkcxPks2SFk7TFg9QEs5VGFCVmRAV2Q+WWc+VmQ8TVs3SlU3VGA+SlU9Q003Tlk9T105Slc4VWI/TFo5VWVAUGA4SVo2RFM3Q1M0")
    append("R1UzRVI0SlU3R1U3S1Y7P0U2Qkg+UlhGRk48Rk8+Q0o6Qk03SVY5RU85V2U/UWE2VmY1VGM2QU8+PUs+QlA+P049P0w9OUc4N0U2Mj8zNUQ2MT82KjguMD81")
    append("QVA/OUo1QlBAQ1M6TF49TFxBQlA6RFA7Q088RFE8R1VASldARlU9RlQ9RVU8SllBSlg9TFw/U2BKQE47MT4yOUY2QVA3NEM0Hy4pJTUsHSwqITIrLT4vLz0x")
    append("Okk3PlA8O006NkY0PU00OEk0NkY3O0o5NkQyP084R1JJNEE8O0s2QlI3P1IyP1EzEB8iGisoLz0tHCkhNkcvOEgxO04zMUItO0s5QFAzFSMhPk4wSVswRFI1")
    append("OEcvMkEpMj8pOkouOUguLjsrPkwzLDkwMD4qQU02LzkrRVA8HywlQU07RVE8JTMqJzQnMD4tIC0kL0AoOEYsJjIjMkArPU0wO0c2PEc1QEs6OkU3N0E2Nz80")
    append("Mz81LzkvLjosOkY2PUk0SVU+P002N0Q1MDwxNUMxLz0sKTorLDssLz8sJTYpKzkwTFxCP046SFc7RlY8VGQ8QU8wTVo6VmQ8SVU5U2BDX25LU2JBP0k2RlI8")
    append("SVc5N0UxO0kzP080RVQ0R1Y1R1U2S1g5SFU8SlU+R1A9SlM/XWlKU19BQ041P0szSFc2QU82R1Y+UmQ5WmZCU2E/VGQ8Vmc+VmQ+Y3FCYnFEXW1BV2VAVmFC")
    append("WWZFYGxGT1k8RlE1Ulw8S1Y8SFcySVc2RVMzSFY3RFMzTlw2TFc8QU01SlQ8Slc4Tlk9UFk8U11CXGVJV2BDT1Y/QUg5UFs/TFg8OEMwOEMvSFcwRlQwPEkr")
    append("RVNAPkw9SldFP087PEk9N0Q5QU89MkA1O0k5NEE2M0E1LTszRVNHPU42RlY+RlY6TF08PUw4T11BRlJASFRDPko9SVVDQ088S1hETFo+UF1GTVxCUWBDQ1E6")
    append("P007O0k4N0Q1PEk2RVM5Pk42KTotJzgsITItKjorKjorM0Q0OUo7QVI9SVpBN0YyMD8tPVAzPk45NUQ2MD4vM0IzJDAqOEc6SVw4NEYsLTwrN0kzNEgwOEky")
    append("LjwsL0AsOUwzMD4tQVUwOUsxQlI2S102EiEgP08wSVsyPk80Q1M0MD8nMEAnO0orMT8rN0UwPUgxHysoNkYqLTwnO0cvOUUyJjIqMD00KjYtKDYpNkAyOEUz")
    append("NEItKTYkNUQrNEMqM0MrOEktNkYxRlQ8OkgzQk44Pkg0QEs3Qk07PUo2M0AwQlE3O0c1OkQ1PUo3OEg0LjsxO0g0MTszMkIpGiYhKDgnMUEwN0g0S1tATFtB")
    append("QlI3SFc5TVs1QE0xR1M6RE84Ul9BVGJCTlxBRlI9PUoyQVA0LDksOEcyQVI2SFk2S1o1V2Y8VWU8TFg6T1pBXWlRU14+a3RaXWxITFY/UVw+SVY3X21DUV9B")
    append("W2lFW2pAWmpFWWk9VGQ4V2U9VmQ7V2Y8WmdBTlo8VWE/VWA/XmxEWmNDSlQ4WmNAYWxJT145TVs5T104TFc5TVs5QVEvQU4xTlk9RlE5SE88Qk02O0Q3RE45")
    append("RlA2SVM4U1xAU1pBVmFETFg9SFU7LTcrOkUsPUsvOUUuOkgrPkw9OUY5Okg6PU42N0Y3OUc4OEc1M0I1OEc4PUo6MT82Lj02Q1E+QlI3R1g7R1Y9R1Y/R1c9")
    append("U2FLQE09QlA+SVZFSlVDP0w7RFM/TFpCRVI/RlU8SVc+SFU9NkMzRFM8R1Y7Qk84SVVDSVo+NUkuPk80Lj0tJzUoNEQ0M0U1KzoyNkQ1QlA7QlM5Lj4xNkcx")
    append("OUYzKTctPU03PEs5KDYuMD4wO0wwLkAvRVg5O001N0guQ1IzM0EvPU8zNkgyN0gxPlIwP1EzRFQyQE40Lj8tR1czPk8vRlc0N0csNEQoPE4wPU0vP08vSVcz")
    append("LzkvICsmNkUqOkYuQE0uOEMwMz4wIi4nKjcrLDcsSE9AJS8nSVY2NEEqPk4xM0MoLz4oM0ErMD8uMT8yM0EuPUkyP0oxQEwySVQ4Q1A1NUMwP080LjkuMj4y")
    append("O0k1LjwvOkM5NUM0LjouO0stMUAsKDkoLj0tO0s0Rlc8UmFDQVE3VWQ8RFI2WGNMU2BGSFU7XGhEWWZGQ084RVA5RVY3NkYuHy0mNEMwRlgzTV02U2M5VGM6")
    append("T108SVY7Tlo+Z3FaZ3BUcXppUV5AQ046Tlo9VWY9U2E7UF06Tl04Slg9Vmg9V2o7WWo9VWM9UmA5S1g0SVQ1UmA4UV83TFo1QE0xS1c3WGVAUl45VGI6UV83")
    append("Ul48SFQ4Ul4/VGQ4QFAvPEgwRE44NT8uOEEwOkQyR1I8RE46PEQ1OUIzOkQzPEQ0SFM5RVM2OEYyOkUwPUkwP0wyUV86Q1QuPkw9NkM4KjgwQFE3QFA3QE83")
    append("M0MyP006MD80N0Y3LDsxN0Y5MD8xSlhBSVo8QVA6SlpBSFg9T1xIQk8/PEo4SVZFSFRDQ1E9RFI6QlE8Pkw3S1o+P005TltIOkg1PEs3N0UzO0g8P048N0Y2")
    append("NkopO0wyN0UzKzouOEg2NUc1NkU1MT8zOUc1QE85PUw7OEkxNEM2LDksR1RBOkc2PEo3OEk0QlUwQFA4PE41TFw4QU4yS1k1RVI7R1c1Pk4ySVs1QlUyR1k1")
    append("S1w1Pks0Pk0vSls2S14yS1wxNUQrN0gsLTwpLz0nQ1EyO0ktKjMsNEEuL0AoUGA5PkwvTFU7N0MsRVA2P04xR1I7SlQ8OUMuOEUsNUEsPEkuO0otNUMsLDcr")
    append("MT8tLTorNEEsMT4rO0cxPkswQ040RVE1OEQvNkMzMT0vOUQ3QEs6Pkk2O0YxLjwsNUMwNEIuMD8qKzwrMkEvUF1JSVo7RlU8SVg4TVs7TVw7UlxGV2RGWmtD")
    append("R1JARU8/UV4+RFI1QVI2NUYtJjUqHSokP08uTl43UWA4Tlw3T149W2lEV2RCXGpCWmVGWWZEUl88T1w7T1w9WGU+TFo0RFMxQE4wSls0U2Q2T2A2Wms7WGc8")
    append("VWQ6VWU4WWc6UF04TFk0SVY1SFU0UF04T1w5Tlw0Slg0T1o5SlM4TFc4TVs9QU4wRlQzO0cwPkk0RVI3SFQ2PUkvPEY0PEU2OUE1SFM6P0w0T1hDQ000P0ww")
    append("JzIiPUgvOUMwTFc6RE41RVMyPEo7PUw6QlI6OEkzQVA5QFA5PEo4O0o4NkU3MUAyMkIyOUc7PEs9SFZDRVY5QlI6QFA4Pk42O0k2Pks5OEY0PEg9QEtAPEo/")
    append("N0I0PUo3P001P082P005M0AxOEYyPU03M0EvP004MUEvKjcsKDckOUsuO0s1N0c2NEMwOkk2P1A5N0Y0NkUyM0ExNEIwMUMuPk8/PEo2NkM4SFY9O0o2M0Ux")
    append("PlAyMT8vN0YvQ1IzS1g7P0wyUV1DTVw4UWA5UWA5SFk3Sls0SFs0Sls4RFQ0SVg1SFovQlIvMkMpJTMmLjsqICsfNkQrL0ApLz0oNEQqNkUrSlw0PUsvRFA3")
    append("OEUwQE0zR1M2U149Qks0PkkzOUMuO0YwPUcyPUkwNEErKDQpJTEmJC0lJC8mKjgnOkcwN0MtQkw0PUcxRk41RE42OkYzP0s5TVhCPUY3OUUwMUAtNkYyMUIt")
    append("IzIkLTsrJTEqWGtFTF46QlI2T144TFs5U2U7VGNBUWBAVWRBRFE9TFc8TFg7OEUuNUMuNUYrFiIfHCgmSlo1TV05Q1IxRlQyWmlBVGI+VGI9XWxAV2U9VWM7")
    append("V2Y+V2Y+XGhEVF89TVw2MkAqKTclP00tPkovV2Y5UWE4UV45V2Q8RlUqQE0pPUgwQEoxPkovTl0zQ1ArTlw0Wmc8VmI9VmI8U109Tlg6UFw+RFA0RlI4S1c7")
    append("RVI3Tlo9SFY1Q1AwT1lCS1c7VV9ERlQ4Q1Q0TVk4OUQsTFM4N0EnPEcvNEAuTVNAOEIwOEMrQU4/QlA9TV47Pk04Okc2QlE9OEU4P085P086MUAyMkIyRFFG")
    append("SFZDJTMqPEw1QlM5M0MxQ1I8Pk01P044RFM/QU5BQE1BP01BP0w5N0MxPkszO0szTFo+PUw0TFs7Slo7OUkySFo7P047MkEwQE40O00sNUYuN0cyO0o0QlE6")
    append("PU45OEcwLz4tPEo2NUIxOko0MkM0QlI7O0c5TFo+LTovMz81N0Q0NUMxOEcrND8vP0suN0QrPkw0RFI3QU8zRFEyS1s2Slo0RFUvRVcySlo1RVQzR1gtOUkq")
    append("O0stMD8tLzwqMD0lQFExQFMzN0ktSVdALDgnUl4/TVo3QlI1VGNCUF49QEo0S1U9QUo1UVw/S1Q2QkwzPEUxOkMyNT8sKTIpKC8pICYlJS8pKzkpKTQpLTcp")
    append("KDAnPEUzQUczQUkyR1E3TFk8Tlo8OEQwOUUzLDosO00xLT4qKzonLTovMT4yPU0ySFg2P08yQk80VGM9Q1QxV2dCLj0sSVg6OUkyRVQzPks1PEgyTlw9Slo2")
    append("HCgjFCAiJzcpQFAzSFg0T184UmE7VGI7W2o8Y3JBVWU6UF46SFY2QU8yVWI9W2o8QE4uJDAiICwiJDEkPUkwSFYzTV02SFcwTlg8NkIjHCUWSlcwSlkwRFIs")
    append("P00oQEwoPEopRlMzS1c2UFs8TFk1TVk5RU83NT8tLTopQk0zNkEqUVs+PUkqOUcpTFwzMD4nOEQuLjstMD0pQU8uLzwkNUEoQk4sND8pLjsmRE83Mz4rLDMn")
    append("RlNBSVhBQ1I7O0o2RVM7Q1E9N0U2NkYwNkUyNkczLDsuQU8/P0w7R1FEOEc0PEw3NEIxOUY1M0IxNEI1Okk6PUo6PEk+OUc7N0M1KzcpRlM6NUIxNkMwMkIu")
    append("PEsyO0stMkIsOUkzOEc0LDspPkwwMkIuOkwyOkcyPkw0NkMxOks1O0swN0YvNUQyOkc0N0UyQlE5KDgqN0M1P0w4JjcnSVJIOEU0LzwrLz4mSlU/LzsmJTQj")
    append("NUQuLjoqMD0pICwfOEYqR1UzQ1QsSlwxP04uQVAuP1AoOkorSlk1LDwoMD8pKzkmNEMsMEAqMUArR1Q8NEMrUWA6R1UzLz4oO0wwLTgmLzopJC8kNT4uRFE4")
    append("UFlASlM5OkMuQEo0JCoiQEs0MzswJi4tICooKzMsMDgxMz0vOEEyOUM0RUw1VFpBSlI8T1o9U1tDOkYwRFA3O0UyOEIuP04vOUgsRFFBNEAxIi4hRlU0MkIr")
    append("UmFAV2g+PE0uSVo6OUwzP082MEErN0YtNUQxRFM1OkowLz4pKDYsFiQiMkUsSlw4UGE5VGU7VWM9XWxCXWtCXGhCS1k4UF45Tl04QU82SFYyTlw2OUcoOEYs")
    append("GCMeIS4iLTonNUIrQE8xO0opS1cuMz4hKzUeOUYjPkwlOkklRVAnRk4qOEUlLzskLjonQEsxP0guMj0qOkMwGSIdN0IvKzUoNUAsO0YvKTMjNkQrQFAvOUcs")
    append("LTkoKzYqMj0pOkYsQU8tPUstWGE4OEEqLTQnMj8pISofTVY+MkAyPUs4SFdBMUAtPk00OUc2MD8tKTcnHSoiMUEtN0cyKDcpMkExPEk4OkgxPk01PUo2MT4w")
    append("Lz40MT80M0AyLjouPEo+OEQ7O0c7NEEtNUMsP0w4LjwrKjkoJzYjLTwpJzYnKjgsKTkoMUAsLTspNEQwM0UsOEUxOEQuO0k0N0QyM0MsM0EsPEo0OkcwMT8u")
    append("O0oxJzUqOUYxNkUtJzkkNUMwMkIsNUQtKzslNkcrLDsnNUcuMkIrJC8jKTcmIzMjKDclN0YtLz4kQ1MoPUopPkooM0ApN0UsM0ErMkIqNkYtMD4uLjsrKTUm")
    append("QU81O0ksQ1MwRFYvN0crNkcqNEMuMDwoKDMnMD0rNkAvMT4sOEM2P0k1MTsoO0YxQUs2N0UrQE4yPEsyOUUyPUczSE86TFZARlA9Qks7SlI5TlY7Ulo9P0gv")
    append("T1o+QlAyOEUxSVM9ND0nQU0sND8nNUEvMj4qLTgkQEwvQFEvRFUzTmE1OUopRVU0Rlc4MkIuS146Lj0rPU81N0ctKzwmO0k5NkY0NEUvQVMwTmA6UWQ8VGY/")
    append("Ul89VWVASFY6RVQ5KjksLjwqUV86SVUxRlYvTVwuKDYfM0IpHCcbFB8WLz0nPU0tP04rNUEiMz4dMDsgGyMUIiwXO0odPkwkMjwhNjwkKzYfLTkkJjIiLTon")
    append("V15GOUIvLDgmMDwpKzUmLzooNkQuMj0oMDwnMD4oMDsoNkMoMj4lMDsqNUEpOkUuS1c4LTsmOkYsMD0nRlA5LjknJC4gPkouMD8tKzgrP1A1KTgrMUAwNUQ4")
    append("IzImKDYpIi8lKTkpMUAuNkQ1TFpFMD4tPkovMj8qPEYyOkY0NUU1PEk2NUIzLTkuPUo+Okc8O0g0NEMrLTsnO0wwLDklIS8eHy0fKjgoKjgpM0QtITAgKjkp")
    append("IS8kJzUoLDsnMT4tLzooM0AuOkczKzkoP0sxPkkvKDIjKzgpOUcvOUcyMkMqMkIrJjUkMEIoLz4oLjwoLDsnKjslKTkkM0QrGCUbLDooHCoeKjomKjonMUEr")
    append("IzAfLjgePUckOkUjOUYpOkgrO0kxO0otMUErQVE+OEk3O0w7RlZCPEs2OkoyPUs2MT8nRVUyRVI4KDMjLjopM0ItJzMlLDgpKDIqKjUqNUExO0YyS1Q9PEY1")
    append("R1c4TmA9SVU6TFVAS1g2SFQ/RlE/QEk6QEksQUooLDccNkAjO0krMD4nLDomPUorNUAnQUwuLTckNEAnOUcqO0wsO0YpRVQuOEcrQ1QuN0YnLj0mLz8uKzot")
    append("M0MwNUcxSltAP085Rlo+RlFBLDotRVY3R1k0Tlw6VWc+V2hCPkwzNkcwJTInJDMoIzAnNEMvT184U18xR1YqQ1EnIS4dIy8gKTciIzEgIzEgQE8sQ08oQkwr")
    append("NkEkKDEbEhoPFBsNKzYZMTscKzQcND0kKzokIi0eLTsmLz0nLTklN0AuOUQxNkErLzgmNkMmJzMfKTUgOUkoO0UzVFpGQE4rHigYRk44LjkjPEY1LjwrKjcn")
    append("LjgrLTknQU04WF1LR1A1O0QrKjkpGiUgJjUnJDEpKzksPU06KDcoMEAtKDcoIS4mJjMoMD8vQU44PEwyO0YzQ04zOEEwOkQxO0g0Mj4rMjwtP0s4P0o9NkQ1")
    append("HysfJjMjKTYlMEAlFB8VJjUgHiscJDIeLT4oHi0dHy4cLDomLDorLDoqPUo1M0AoRFA4N0QwO0kxN0QsP0wtKDAhCBARFR8aLTwoQE0tLj8nLj8pKjgmNEYo")
    append("JC8gIzAgGycbJTMiKzklKzolM0IqKDUjN0csJjUjGigcFSEYJDEgKDEdMjwgO0gkO0olQVAoMj8kM0EnPUwvSVlAQVE5RVZAN0kwLDskLj4pNUEwKzciNkQm")
    append("LTkjMz4mQlAwOkktLjolIi0hKzYrKTUqNkQ0RFA6RVA2YG5NZHBZZG9WbXdiYW5PMz4lPEktOUYtNUEsOUEjMjgdKDMbOkkoKzkhLDojNUMpNEEmO0gqMj0m")
    append("OkQpKzQhP00uLjwkMDwiLTsjJzUgOUgtQlEwLz4uOUg3OUc5QlA7R1c5UF9JVWRLPk85NUEyLTosOUgxNEQtOUcuOksvN0UtIS8iKTgpJzUpMD4uLTorOEcu")
    append("SlowU2ExV2UyRlIqOkcrKzokJDIgGiMXHioaQk0oQEsnLjcgN0ElJzAcJC8dGSISIy0VLDcZICkWNEAhLDghLTglICoePEgsJi8hLzwqU1tMUVpIXGFGT1k1")
    append("RE4zJDEbJDAaNkIpVF5BNEEjGCAWVWA+WGE+aW9YXWFQJTEfLjgoHSgbQEo2YWdTR1EzP0gsJjUmGyceLz8qLTwsN0cvRFQzOkgvOUoxMUIrIzAjKTYmJDEl")
    append("NEIrQlAyO0kwOkgsMj4tQU40S1Y7Q04xS1Q7SVREPko2RFE1IS0hIzAhMT8oMUAlJjUjKDghMEEnIi8aNUYoHywcKzkiKzklN0YvNUUrQ1EzKzkjMT4pIy8h")
    append("N0UsKzcjLz0jJS4gERoYFB0aO0crRVAtKjkjIC8fM0UnLDohJjIiKzojERwVHyweGiUaJzUjN0YsLTslOUorOUopLT0kHywdJTQhKDUiLjohNUQiNkQlOEUl")
    append("GycXNUQpP00qPksrN0QoRFIwLT0jLTwkOk0sLTwlM0EoIzAdOUcoPUoqO0kqNEEoMz4nKjUlIi0gJC4iO0owPEowLTgkMT0lSVFAWmZGZnFXTVs5MzslPUgs")
    append("NUIlPkwrKTEdGSAVMkAkKzkgKDUeO0kpM0AnJzMgLzkiOkcpMz4mMj0lKDIhLDkhLTghLTwlNEUmPEswPk00SVk3S1o3N0UsLz4pNkQoPk0yWGdEQlAwPUkw")
    append("MD8rN0YtM0ErOEguPU0sNUIsPEsxOEMuOEkvR1Y7P0s0RFQyS1o5QE4tQk8qPUsqMkEmLDwjJDIhJDEeN0coPkkpOUQmMToiQk0oLjgeJjIcHSgTGCAOIisU")
    append("O0YiN0IgPEYjMj4mLzslKzgjKDQjLz8mOkQvO0YvXGU/W2U7PUkrICsaHiYXSFUvbXZHO0YoFB8UYGxChIdfTFg5VFxAPEcnPUctJi8hHykcS1Y5SVMzR1It")
    append("JzclJzYkOEkuLz8rRFcyQk4uQE0xSVk5OUsvOEctN0YsKzknO0wsPUstIS4dMkAoQ082S1Y4MzwmS1UyRE4xVF5MN0QtPkkrLDknLj0lM0IoNUQoLz8nNUYm")
    append("NkgpKzYgPE0oMD0jNEAnIi4fLTwnJzYfKjsgMkEpKDQkGiUaLzwnLTwmM0MnJjEiICsjLzgqSFMtOEIlNUQnLT0jKjsfJjMcKDYjNEQkIS4cKDYjEBgTMT8k")
    append("N0UqLj0kLj0jNEQjNUYlJTQgJzckHCgcHysbJjQdLDolISwaKjchIS8bJTAcMz8iPksnSFgpLj8jNUgpQ1cuPEwpPE4rITAdMD4kKTUgRFQwJC4hLjglKzUm")
    append("JC0fLzgoOUgqMj8pND8qLzkkV2M8XGs/WGA/UFw1LDQiRE4xPUkoOkYpLjceFR0VICsbHyoXQE8pLzwhJC4cMkAjHykYNEMkP0wtLDgiFh0UJzIeO0cqJzMh")
    append("M0MoMUAoPk8vTFovS1gwUmMzSlwwRlYyQk41QlAvU2E7RFAxNkYsP08wR1czQE8wMz8lOkYtR1YxSE8zQlYtUGA2P0owP08pPk0uQlItP0woMT0kKzchN0Yn")
    append("N0YqPU0oNUUiKjQcMz0hQUspKDEZQk8pKjYfICwVFBwNDBIKJzEYIisUHicTJC8ZOkUqMT8lMj4nLjsiKjQfPksoOkgjO0cmNT8lMTwjPUUmZGxGREspT1cy")
    append("NkIjS1QyUVg6SVguQkwsTlcxUlo7OEIwMj4lKjkgKjQhNkEi")
}.toString()


private fun mtnPhotoDecode(s: String): ByteArray {
    val table = IntArray(128) { -1 }
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    for (i in chars.indices) table[chars[i].code] = i
    val out = ArrayList<Byte>()
    var buffer = 0; var bits = 0
    for (ch in s) {
        if (ch == '=') break
        val v = if (ch.code < 128) table[ch.code] else -1
        if (v < 0) continue
        buffer = (buffer shl 6) or v; bits += 6
        if (bits >= 8) { bits -= 8; out.add(((buffer shr bits) and 0xff).toByte()) }
    }
    val arr = ByteArray(out.size)
    for (i in out.indices) arr[i] = out[i]
    return arr
}
private val PHOTO_VEG = mtnPhotoDecode(PHOTO_VEG_B64)
private val PHOTO_DARK = mtnPhotoDecode(PHOTO_DARK_B64)
private val PHOTO_WARM = mtnPhotoDecode(PHOTO_WARM_B64)
private val PHOTO_RGB = mtnPhotoDecode(PHOTO_RGB_B64)
private fun mtnPhotoRGB(u: Float, v: Float, out: FloatArray) {
    val fx = mtnClamp(u, 0f, 1f) * (PHOTO_FIELD_W - 1); val fy = mtnClamp(v, 0f, 1f) * (PHOTO_FIELD_H - 1)
    val i = floor(fx.toDouble()).toInt(); val j = floor(fy.toDouble()).toInt()
    val tx = fx - i; val ty = fy - j
    val i1 = min(i + 1, PHOTO_FIELD_W - 1); val j1 = min(j + 1, PHOTO_FIELD_H - 1)
    for (c in 0 until 3) {
        val a = (PHOTO_RGB[(j * PHOTO_FIELD_W + i) * 3 + c].toInt() and 0xff) / 255f
        val b = (PHOTO_RGB[(j * PHOTO_FIELD_W + i1) * 3 + c].toInt() and 0xff) / 255f
        val cc = (PHOTO_RGB[(j1 * PHOTO_FIELD_W + i) * 3 + c].toInt() and 0xff) / 255f
        val d = (PHOTO_RGB[(j1 * PHOTO_FIELD_W + i1) * 3 + c].toInt() and 0xff) / 255f
        out[c] = mtnLerp(mtnLerp(a, b, tx), mtnLerp(cc, d, tx), ty)
    }
}
private fun mtnPhotoField(arr: ByteArray, u: Float, v: Float): Float {
    val fx = mtnClamp(u, 0f, 1f) * (PHOTO_FIELD_W - 1); val fy = mtnClamp(v, 0f, 1f) * (PHOTO_FIELD_H - 1)
    val i = floor(fx.toDouble()).toInt(); val j = floor(fy.toDouble()).toInt()
    val tx = fx - i; val ty = fy - j
    val i1 = min(i + 1, PHOTO_FIELD_W - 1); val j1 = min(j + 1, PHOTO_FIELD_H - 1)
    val a = (arr[j * PHOTO_FIELD_W + i].toInt() and 0xff) / 255f
    val b = (arr[j * PHOTO_FIELD_W + i1].toInt() and 0xff) / 255f
    val c = (arr[j1 * PHOTO_FIELD_W + i].toInt() and 0xff) / 255f
    val d = (arr[j1 * PHOTO_FIELD_W + i1].toInt() and 0xff) / 255f
    return mtnLerp(mtnLerp(a, b, tx), mtnLerp(c, d, tx), ty)
}

// Big mountain — one deterministic two-crest heightfield massif matching the
// green Drive photo's extracted skyline, with talus cross-section, gully
// network, boulders and scrub. One mesh serves all three seasons.
// 1:1 port of mountain_scene.js in the HTML preview.

private const val MTN_SEED = 0x5eed2026
private const val MTN_HALF_W = 2400f
private const val MTN_Z_BACK = -1500f
private const val MTN_Z_FRONT = 1100f
private const val MTN_PEAK = 1000f
private const val MTN_NX = 560
private const val MTN_NZ = 220

// Back-crest skyline from the green photo sky-mask scan: (724 - ridgeY) / 539.
private val MTN_BACK_CTRL = floatArrayOf(0.8280f,0.8275f,0.8219f,0.8182f,0.8208f,0.8237f,0.8256f,0.8200f,0.8163f,0.8163f,0.8193f,0.8256f,0.8275f,0.8330f,0.8430f,0.8471f,0.8497f,0.8534f,0.8512f,0.8497f,0.8434f,0.8293f,0.8293f,0.8301f,0.8323f,0.8293f,0.8263f,0.8293f,0.8375f,0.8471f,0.8460f,0.8382f,0.8278f,0.8219f,0.8219f,0.8319f,0.8297f,0.8360f,0.8367f,0.8404f,0.8471f,0.8553f,0.8627f,0.8716f,0.8672f,0.8720f,0.8742f,0.8824f,0.8994f,0.9043f,0.9221f,0.9295f,0.9440f,0.9633f,0.9770f,0.9755f,0.9551f,0.9369f,0.9180f,0.9043f,0.8924f,0.8779f,0.8642f,0.8516f,0.8553f,0.8501f,0.8367f,0.8312f,0.8289f,0.8327f,0.8349f,0.8378f,0.8853f,0.9239f,0.9581f,0.9744f,0.9863f,0.9852f,0.9718f,0.9647f,0.9751f,0.9896f,0.9885f,0.9759f,0.9800f,0.9889f,0.9814f,1.0015f,0.9944f,0.9655f,0.9417f,0.9280f,0.9265f,0.9191f,0.9032f,0.8868f,0.8738f)
private val MTN_ZBACK_PTS = arrayOf(floatArrayOf(0f,-340f),floatArrayOf(0.35f,-360f),floatArrayOf(0.50f,-520f),floatArrayOf(0.56f,-700f),floatArrayOf(0.65f,-520f),floatArrayOf(0.75f,-380f),floatArrayOf(0.85f,-300f),floatArrayOf(1f,-330f))
private val MTN_ZFRONT_PTS = arrayOf(floatArrayOf(0f,-225f),floatArrayOf(0.50f,-200f),floatArrayOf(0.75f,-215f),floatArrayOf(0.85f,-240f),floatArrayOf(1f,-220f))
private val MTN_BACK_CROSS = arrayOf(floatArrayOf(0f,1f),floatArrayOf(0.30f,0.84f),floatArrayOf(0.65f,0.64f),floatArrayOf(1f,0.46f))
// u, depth m, half-width m at head, phase, meander m, crest-cut 0..1
private val MTN_GULLIES = arrayOf(
    floatArrayOf(0.07f,34f,58f,1.7f,70f,0.05f),floatArrayOf(0.16f,40f,64f,4.1f,85f,0.05f),floatArrayOf(0.25f,52f,72f,2.6f,105f,0.10f),
    floatArrayOf(0.34f,48f,68f,5.3f,95f,0.08f),floatArrayOf(0.43f,40f,60f,0.9f,80f,0.05f),floatArrayOf(0.52f,50f,70f,3.4f,100f,0.10f),
    floatArrayOf(0.60f,46f,66f,5.9f,90f,0.08f),floatArrayOf(0.67f,36f,56f,2.2f,70f,0.05f),floatArrayOf(0.73f,88f,82f,4.7f,115f,0.55f),
    floatArrayOf(0.815f,44f,64f,1.2f,80f,0.08f),floatArrayOf(0.90f,38f,58f,3.9f,75f,0.05f),floatArrayOf(0.965f,32f,54f,5.6f,65f,0.05f))
private val MTN_BROAD_VALLEYS = arrayOf(floatArrayOf(0.28f,0.40f,78f,300f),floatArrayOf(0.55f,0.50f,55f,260f),floatArrayOf(0.73f,0.67f,112f,340f),floatArrayOf(0.90f,0.98f,50f,260f))
private val MTN_ROCK_COOL = floatArrayOf(0.34f,0.32f,0.29f)
private val MTN_ROCK_WARM = floatArrayOf(0.58f,0.45f,0.30f)

private fun mtnMix3(a: FloatArray, b: FloatArray, t: Float): FloatArray =
    floatArrayOf(mtnLerp(a[0], b[0], t), mtnLerp(a[1], b[1], t), mtnLerp(a[2], b[2], t))

// Sample the photo-baked fields at the image position this world point
// projects to in the fixed front view. out = [veg, dark, warm, r, g, b].
private fun mtnPhotoSample(x: Float, h: Float, out: FloatArray) {
    val u = (x + MTN_HALF_W) / (2f * MTN_HALF_W)
    val ridgeY = 724f - 539f * mtnProfileAt(MTN_BACK_CTRL, u)
    val imgY = 724f - (h / 1009.04f) * 539f
    val v = (imgY - ridgeY) / max(1f, 724f - ridgeY)
    out[0] = mtnPhotoField(PHOTO_VEG, u, v)
    out[1] = mtnPhotoField(PHOTO_DARK, u, v)
    out[2] = mtnPhotoField(PHOTO_WARM, u, v)
    val rgb = FloatArray(3); mtnPhotoRGB(u, v, rgb)
    out[3] = rgb[0]; out[4] = rgb[1]; out[5] = rgb[2]
}

// out = [height, gullyMask, frontD(-1 back / 0..1 front), zone(0 back,1 plateau,2 front), zDisp]
private fun mtnTerrainSample(x: Float, z: Float, out: FloatArray) {
    val u = (x + MTN_HALF_W) / (2f * MTN_HALF_W)
    var backH = MTN_PEAK * mtnProfileAt(MTN_BACK_CTRL, u)
    backH += 38f * max(0f, 1f - abs(u - 0.56f) / 0.035f) - 20f * mtnGauss(u, 0.56f, 0.07f)
    val frontH = backH - 135f * mtnGauss(u, 0.56f, 0.075f) - 25f * mtnGauss(u, 0.73f, 0.045f)
    val zB = mtnPiecewise(MTN_ZBACK_PTS, u) + 40f * mtnFbmC(u * 9.1f, 3.7f, 2, 131)
    val zF = mtnPiecewise(MTN_ZFRONT_PTS, u) + 85f * mtnFbmC(u * 6.1f, 8.2f, 3, 149)
    val wx = 70f * mtnFbmC(x * 0.00055f + 13.7f, z * 0.00055f, 3, 71)
    val wz = 45f * mtnFbmC(x * 0.0007f, z * 0.0007f + 7.3f, 3, 97)
    val xw = x + wx; val zw = z + wz

    var h: Float; var zone: Int; var d = -1f; var zdisp = 0f; var dEff = -1f
    if (z <= zB) {
        zone = 0
        val t = mtnClamp((zB - z) / (zB - MTN_Z_BACK), 0f, 1f)
        h = backH * mtnPiecewise(MTN_BACK_CROSS, t)
    } else if (z < zF) {
        zone = 1
        val t = (z - zB) / (zF - zB)
        h = mtnLerp(backH, frontH, t * t * (3f - 2f * t))
        d = t * 0.22f; dEff = d
    } else {
        zone = 2
        d = mtnClamp((z - zF) / (MTN_Z_FRONT - zF), 0f, 1f)
        val contour = mtnFbmC((xw * 0.70f + zw * 0.71f) * 0.0016f, (zw * 0.71f - xw * 0.70f) * 0.0016f, 3, 59)
        dEff = mtnClamp(d + contour * 0.13f * mtnSmoothstep(0.05f, 0.30f, d), 0f, 1f)
        val drop = 0.42f * mtnSmoothstep(0f, 0.22f, dEff) + 0.38f * mtnSmoothstep(0.18f, 0.58f, dEff) + 0.20f * mtnSmoothstep(0.52f, 1f, dEff)
        h = frontH * (1f - drop)
    }

    // Multi-scale relief, faded at the two crests so the skyline stays exact.
    val crestFade = min(mtnSmoothstep(0f, 90f, abs(z - zB)), mtnSmoothstep(0f, 70f, abs(z - zF)))
    val zoneAmp = if (zone == 2) (0.35f + 0.65f * sin(PI * min(d * 1.12f, 1f)).toFloat()) else 0.7f
    h += (26f * mtnFbmC(xw * 0.0011f, zw * 0.0011f, 4, 11) + 10f * mtnFbmC(xw * 0.006f, zw * 0.006f, 3, 23) +
        3f * mtnFbmC(xw * 0.025f, zw * 0.025f, 2, 37)) * zoneAmp * (0.25f + 0.75f * crestFade)
    // Craggy crest teeth.
    val crestBand = max(mtnGauss(z, zB, 55f), mtnGauss(z, zF, 40f) * 0.7f)
    h += crestBand * (48f * (mtnRidged(x * 0.02f, 4.2f, 3, 51) - 0.5f) + 28f * (mtnRidged(x * 0.006f, 9.1f, 2, 53) - 0.5f)) *
        (0.75f + 0.75f * mtnGauss(u, 0.88f, 0.13f) + 0.45f * mtnGauss(u, 0.56f, 0.08f))
    h += mtnGauss(z, zB, 280f) * mtnGauss(u, 0.56f, 0.13f) * (mtnRidged(x * 0.009f, z * 0.009f, 3, 79) - 0.5f) * 42f
    // Downslope ribs/buttresses on the front face, displaced toward/away from
    // the camera as well as in height so the cliff reads as real fluting.
    if (zone == 2) {
        val ribW = mtnSmoothstep(0.0f, 0.04f, d) * (1f - mtnSmoothstep(0.22f, 0.38f, d))
        val rib = mtnRidged(xw * 0.010f + zw * 0.004f, zw * 0.0035f, 3, 61) - 0.5f
        h += rib * 10f * ribW
        zdisp += rib * 45f * ribW
        val spurW = mtnSmoothstep(0.12f, 0.32f, d) * (1f - mtnSmoothstep(0.72f, 0.98f, d))
        val spur = mtnRidged((xw * 0.72f + zw * 0.70f) * 0.0019f, (zw * 0.70f - xw * 0.72f) * 0.0019f, 3, 67) - 0.5f
        h += spur * 18f * spurW
        zdisp += spur * 40f * spurW
        val buttress = mtnRidged(xw * 0.0038f + zw * 0.0012f, zw * 0.0010f, 3, 83) - 0.5f
        val buttressW = mtnSmoothstep(0.0f, 0.05f, d) * (1f - mtnSmoothstep(0.45f, 0.75f, d))
        h += buttress * 34f * buttressW
        zdisp += buttress * 150f * buttressW
        h += 28f * mtnFbmC(xw * 0.0016f, zw * 0.0016f, 3, 103) * mtnSmoothstep(0.55f, 0.85f, d)
        // Photo-derived relief: rock tongues protrude, dark gullies recess.
        val ridgeYph = 724f - 539f * mtnProfileAt(MTN_BACK_CTRL, u)
        val imgYph = 724f - (h / 1009.04f) * 539f
        val vph = (imgYph - ridgeYph) / max(1f, 724f - ridgeYph)
        val photoVegH = mtnPhotoField(PHOTO_VEG, u, vph)
        val photoDarkH = mtnPhotoField(PHOTO_DARK, u, vph)
        val tongueW = mtnSmoothstep(0.10f, 0.24f, d) * (1f - mtnSmoothstep(0.38f, 0.55f, d))
        zdisp += (0.45f - photoVegH) * 110f * tongueW
        zdisp += (0.72f - photoDarkH) * 90f * ribW
        h += (0.45f - photoVegH) * 16f * tongueW
        val chan = mtnSmoothstep(0.60f, 0.86f, mtnRidged(xw * 0.016f - zw * 0.005f, zw * 0.004f, 2, 73))
        h -= chan * 15f * ribW
        zdisp -= chan * 22f * ribW
    }

    // Gully network.
    var gully = 0f
    if (zone >= 1) {
        val gd = d // plateau d already compressed to 0..0.22
        for (i in MTN_GULLIES.indices) {
            val g = MTN_GULLIES[i]
            val center = -MTN_HALF_W + 2f * MTN_HALF_W * g[0] + sin((gd * 4.2f + g[3]).toDouble()).toFloat() * g[4] * (0.25f + gd) +
                sin(g[3].toDouble()).toFloat() * 170f * gd * gd
            val w = g[2] * (0.45f + 1.10f * gd) * (0.82f + 0.30f * sin((gd * 9.0f + g[3] * 2.0f).toDouble()).toFloat())
            val q = max(0f, 1f - abs(xw - center) / w)
            if (q <= 0f) continue
            val shape = q * q * (3f - 2f * q)
            var prof = mtnLerp(g[5], 1f, sin(PI * mtnClamp(0.08f + 0.84f * gd, 0f, 1f)).pow(0.8).toFloat()) *
                (1f - (1f - g[5]) * mtnSmoothstep(0.45f, 0.95f, gd) * 0.78f)
            prof *= 1f - (if (i == 8) 0.35f else 0.85f) * mtnSmoothstep(0.28f, 0.60f, gd)
            val cut = shape * prof
            h -= g[1] * (if (i == 8) 0.55f else 0.32f) * cut * (if (zone == 1) 0.4f else 1f)
            if (cut > gully) gully = cut
        }
    }

    // Broad diagonal drainage basins between the major spurs.
    if (zone == 2) {
        for (i in MTN_BROAD_VALLEYS.indices) {
            val bv = MTN_BROAD_VALLEYS[i]
            val cu = mtnLerp(bv[0], bv[1], d.toDouble().pow(0.8).toFloat())
            val center = -MTN_HALF_W + 2f * MTN_HALF_W * cu
            val q = (xw - center) / bv[3]
            val infl = exp((-(q * q)).toDouble()).toFloat()
            val env = sin(PI * mtnClamp(0.06f + 0.88f * d, 0f, 1f)).pow(0.7).toFloat()
            h -= bv[2] * infl * env
            val gm = infl * env * 0.55f
            if (gm > gully) gully = gm
        }
    }

    // Front/back edge handling: settle to a clean base line.
    if (zone == 2) h = max(h, -6f + 6f * (1f - d))
    out[0] = h; out[1] = gully; out[2] = if (zone == 2) dEff else d; out[3] = zone.toFloat(); out[4] = zdisp
}

/** Geometry totals for the mountain mesh (diagnostics). */
data class MountainCounts(val rocks: Int, val shrubs: Int, val chunks: Int, val verts: Int, val tris: Int)
private class MountainSceneData(val builders: List<MountainMeshBuilder>, val counts: MountainCounts)

private fun buildMountainScene(): MountainSceneData {
    val gx = MTN_NX + 1; val gz = MTN_NZ + 1
    val dx = 2f * MTN_HALF_W / MTN_NX; val dz = (MTN_Z_FRONT - MTN_Z_BACK) / MTN_NZ
    val H = FloatArray(gx * gz); val GU = FloatArray(gx * gz)
    val DD = FloatArray(gx * gz); val ZN = FloatArray(gx * gz); val PZ = FloatArray(gx * gz)
    val out = FloatArray(5)
    for (j in 0 until gz) {
        val z = MTN_Z_BACK + j * dz
        for (i in 0 until gx) {
            val x = -MTN_HALF_W + i * dx
            mtnTerrainSample(x, z, out)
            val k = j * gx + i
            H[k] = out[0]; GU[k] = out[1]; DD[k] = out[2]; ZN[k] = out[3]; PZ[k] = z + out[4]
        }
    }
    // Two thermal-relaxation (talus) passes: material creeps toward the local
    // mean on apron slopes, crests stay put.
    for (pass in 0 until 2) {
        val src = H.copyOf()
        for (j in 1 until gz - 1) for (i in 1 until gx - 1) {
            val k = j * gx + i
            val f = if (ZN[k] == 2f) (if (DD[k] > 0.12f && DD[k] < 0.9f) 0.10f else 0.02f) else 0.04f
            val avg = (src[k - 1] + src[k + 1] + src[k - gx] + src[k + gx]) * 0.25f
            H[k] = src[k] + (avg - src[k]) * f
        }
    }

    val SL = FloatArray(gx * gz); val VG = FloatArray(gx * gz)
    fun sampleArr(a: FloatArray, x: Float, z: Float): Float {
        val fx = mtnClamp((x + MTN_HALF_W) / dx, 0f, MTN_NX - 0.001f)
        val fz = mtnClamp((z - MTN_Z_BACK) / dz, 0f, MTN_NZ - 0.001f)
        val i = floor(fx.toDouble()).toInt(); val j = floor(fz.toDouble()).toInt()
        val tx = fx - i; val tz = fz - j
        val k = j * gx + i
        return mtnLerp(mtnLerp(a[k], a[k + 1], tx), mtnLerp(a[k + gx], a[k + gx + 1], tx), tz)
    }

    val builders = ArrayList<MountainMeshBuilder>()
    val ph = FloatArray(6)
    val bands = arrayOf(intArrayOf(0, 187), intArrayOf(187, 374), intArrayOf(374, MTN_NX))
    for (band in bands) {
        val b = MountainMeshBuilder()
        val i0 = band[0]; val i1 = band[1]
        val local = IntArray((i1 - i0 + 1) * gz)
        for (j in 0 until gz) {
            val z = MTN_Z_BACK + j * dz
            for (i in i0..i1) {
                val x = -MTN_HALF_W + i * dx
                val k = j * gx + i
                val h = H[k]
                val il = max(0, i - 1); val ir = min(MTN_NX, i + 1)
                val ju = max(0, j - 1); val jd = min(MTN_NZ, j + 1)
                val dhx = (H[j * gx + ir] - H[j * gx + il]) / ((ir - il) * dx)
                val dhz = (H[jd * gx + i] - H[ju * gx + i]) / ((jd - ju) * dz)
                // True 3D normal from neighbouring (possibly z-displaced) positions.
                val tx0 = (ir - il) * dx; val tx1 = H[j * gx + ir] - H[j * gx + il]; val tx2 = PZ[j * gx + ir] - PZ[j * gx + il]
                val tz0 = 0f; val tz1 = H[jd * gx + i] - H[ju * gx + i]; val tz2 = PZ[jd * gx + i] - PZ[ju * gx + i]
                var nx = tz1 * tx2 - tz2 * tx1; var ny = tz2 * tx0 - tz0 * tx2; var nz = tz0 * tx1 - tz1 * tx0
                val nl0 = sqrt(nx * nx + ny * ny + nz * nz); val nl = if (nl0 == 0f) 1f else nl0
                nx /= nl; ny /= nl; nz /= nl
                if (ny < 0f) { nx = -nx; ny = -ny; nz = -nz }
                val slope = sqrt(dhx * dhx + dhz * dhz)
                SL[k] = slope
                val avg = (H[j * gx + il] + H[j * gx + ir] + H[ju * gx + i] + H[jd * gx + i]) * 0.25f
                val guAttr = GU[k] * (if (ZN[k] == 2f) (1f - mtnSmoothstep(0.28f, 0.55f, DD[k])) else 1f)
                mtnPhotoSample(x, h, ph)
                val cavity = mtnClamp((avg - h) / 18f, 0f, 1f) * 0.7f + guAttr * 0.16f + max(0f, ph[1] - 0.62f) * 0.55f
                val moist = mtnFbm(x * 0.0015f + 4.4f, z * 0.0015f, 3, 173)
                val patch = mtnFbm(x * 0.008f, z * 0.008f, 3, 181)
                var veg = (1f - mtnSmoothstep(0.72f, 1.30f, slope)) * (1f - 0.38f * mtnSmoothstep(700f, 980f, h)) *
                    (0.55f + 0.70f * moist) * (0.70f + 0.55f * patch) + guAttr * 0.18f
                if (ZN[k] == 2f) veg *= 0.22f + 0.78f * mtnSmoothstep(0.06f, 0.30f, DD[k] + (patch - 0.5f) * 0.22f + (moist - 0.5f) * 0.12f)
                if (slope > 1.05f && mtnFbm(x * 0.02f, z * 0.02f, 2, 191) > 0.58f) veg += 0.34f // ledge scrub
                if (ZN[k] == 0f) veg *= 0.7f
                if (ZN[k] != 2f && h > 860f) veg *= 0.42f // rocky back-peak summit
                if (ZN[k] == 2f && DD[k] > 0.30f && slope < 1.25f) veg = max(veg, 0.55f)
                if (ZN[k] == 2f && DD[k] > 0.55f && slope < 1.05f) veg = max(veg, 0.78f)
                veg = mtnClamp(veg * 0.08f + ph[0] * 1.05f, 0f, 1f)
                if (ZN[k] == 2f && DD[k] > 0.30f && slope < 1.25f) veg = max(veg, 0.55f)
                if (ZN[k] == 2f && DD[k] > 0.55f && slope < 1.05f) veg = max(veg, 0.78f)
                VG[k] = veg
                var mat: Float
                if (ZN[k] != 2f && h > 840f && slope > 0.50f) mat = MAT_CLIFF
                else if (ZN[k] == 0f) mat = MAT_BACK
                else if (guAttr > 0.62f) mat = MAT_GULLY
                else if (slope > 1.12f || (ZN[k] == 2f && DD[k] < 0.18f && slope > 0.78f)) mat = MAT_CLIFF
                else if (slope > 0.72f && veg < 0.42f) mat = MAT_SCREE
                else mat = MAT_SOIL
                if (ph[0] > 0.68f && slope < 1.35f && ZN[k] == 2f) mat = MAT_SOIL
                else if (ph[0] < 0.16f && ZN[k] == 2f && DD[k] < 0.34f && slope > 0.55f) mat = MAT_CLIFF
                var snow = mtnSmoothstep(0.36f, 0.78f, ny) * (1f - 0.78f * mtnSmoothstep(1.02f, 1.55f, slope))
                snow = mtnClamp(snow + cavity * 0.16f + 0.22f * mtnFbm(x * 0.03f, z * 0.03f, 2, 197) - 0.08f, 0f, 1f)
                val warm = ph[2] * 0.78f + mtnFbm(x * 0.002f + 8.8f, z * 0.002f, 3, 211) * 0.22f
                val tint = mtnMix3(MTN_ROCK_COOL, MTN_ROCK_WARM, warm)
                val col = floatArrayOf(mtnLerp(tint[0], ph[3] * 0.80f, 0.94f), mtnLerp(tint[1], ph[4] * 0.80f, 0.94f), mtnLerp(tint[2], ph[5] * 0.80f, 0.94f))
                val shade = 0.92f + 0.16f * patch
                local[j * (i1 - i0 + 1) + (i - i0)] = b.vertex(x, h, PZ[k], nx, ny, nz,
                    floatArrayOf(col[0] * shade, col[1] * shade, col[2] * shade),
                    floatArrayOf(mat, veg, mtnClamp(cavity, 0f, 1f), snow))
            }
        }
        val w = i1 - i0 + 1
        for (j in 0 until MTN_NZ) for (i in 0 until i1 - i0) {
            val a = local[j * w + i]; val bb = local[j * w + i + 1]; val c = local[(j + 1) * w + i]; val dd = local[(j + 1) * w + i + 1]
            b.tri(a, c, bb); b.tri(bb, c, dd)
        }
        builders.add(b)
    }

    // Skirts closing the four edges down to y = -90.
    val skirt = MountainMeshBuilder()
    val skC = floatArrayOf(0.30f, 0.27f, 0.24f); val skD = floatArrayOf(MAT_SKIRT, 0f, 0.25f, 0f)
    fun skirtEdge(pts: List<FloatArray>, n: FloatArray, flip: Boolean) {
        val top = IntArray(pts.size); val bot = IntArray(pts.size)
        for (pi in pts.indices) {
            val p = pts[pi]
            top[pi] = skirt.vertex(p[0], p[1], p[2], n[0], n[1], n[2], skC, skD)
            bot[pi] = skirt.vertex(p[0], -90f, p[2], n[0], n[1], n[2], skC, skD)
        }
        for (i in 0 until pts.size - 1) {
            if (!flip) { skirt.tri(top[i], bot[i], top[i + 1]); skirt.tri(top[i + 1], bot[i], bot[i + 1]) }
            else { skirt.tri(top[i], top[i + 1], bot[i]); skirt.tri(top[i + 1], bot[i + 1], bot[i]) }
        }
    }
    val leftPts = ArrayList<FloatArray>(); val rightPts = ArrayList<FloatArray>()
    val backPts = ArrayList<FloatArray>(); val frontPts = ArrayList<FloatArray>()
    for (j in 0 until gz) {
        leftPts.add(floatArrayOf(-MTN_HALF_W, H[j * gx], PZ[j * gx]))
        rightPts.add(floatArrayOf(MTN_HALF_W, H[j * gx + MTN_NX], PZ[j * gx + MTN_NX]))
    }
    for (i in 0 until gx) {
        val x = -MTN_HALF_W + i * dx
        backPts.add(floatArrayOf(x, H[i], PZ[i]))
        frontPts.add(floatArrayOf(x, H[MTN_NZ * gx + i], PZ[MTN_NZ * gx + i]))
    }
    skirtEdge(leftPts, floatArrayOf(-1f, 0f, 0f), false)
    skirtEdge(rightPts, floatArrayOf(1f, 0f, 0f), true)
    skirtEdge(backPts, floatArrayOf(0f, 0f, -1f), true)
    skirtEdge(frontPts, floatArrayOf(0f, 0f, 1f), false)
    builders.add(skirt)

    // Scatter: boulders + scrub domes.
    val rng = MountainRNG(MTN_SEED)
    var scatter = MountainMeshBuilder(); builders.add(scatter)
    var rocks = 0; var attempts = 0
    while (rocks < 220 && attempts < 6000) {
        attempts++
        val x = rng.range(-MTN_HALF_W, MTN_HALF_W); val z = rng.range(-350f, MTN_Z_FRONT - 20f)
        val d = sampleArr(DD, x, z); val slope = sampleArr(SL, x, z); val gully = sampleArr(GU, x, z)
        if (d < 0.18f || slope > 1.15f) continue
        val h = sampleArr(H, x, z)
        var s = 1.3f + 6.5f * rng.next().toDouble().pow(2.2).toFloat()
        if ((d > 0.16f && d < 0.38f) || gully > 0.3f) s *= rng.range(1.0f, 2.6f)
        val ry = s * rng.range(0.45f, 0.8f)
        val shade = rng.range(0.8f, 1.15f)
        val col = mtnMix3(MTN_ROCK_COOL, MTN_ROCK_WARM, rng.next())
        if (scatter.vcount > 43000) { scatter = MountainMeshBuilder(); builders.add(scatter) }
        scatter.rock(x, h - ry * 0.35f, sampleArr(PZ, x, z), s, ry, s * rng.range(0.7f, 1.2f),
            rng.range(0f, (PI * 2).toFloat()), rng.range(-0.35f, 0.35f), rng,
            floatArrayOf(col[0] * shade * 1.35f, col[1] * shade * 1.35f, col[2] * shade * 1.35f),
            floatArrayOf(MAT_BOULDER, 0f, mtnClamp(gully, 0f, 1f) * 0.5f, 0.4f))
        rocks++
    }
    var shrubs = 0; attempts = 0
    while (shrubs < 0 && attempts < 12000) { // scrub is shader/photo texture at this scale; cone geometry read as artifacts
        attempts++
        val x = rng.range(-MTN_HALF_W, MTN_HALF_W); val z = rng.range(-250f, MTN_Z_FRONT - 10f)
        val d = sampleArr(DD, x, z); val slope = sampleArr(SL, x, z); val veg = sampleArr(VG, x, z)
        if (d < 0.35f || slope > 0.8f || veg < 0.42f) continue
        val h = sampleArr(H, x, z)
        val shade = rng.range(0.75f, 1.2f)
        if (scatter.vcount > 43000) { scatter = MountainMeshBuilder(); builders.add(scatter) }
        scatter.shrub(x, h - 0.15f, sampleArr(PZ, x, z), rng.range(2.2f, 6.5f), rng.range(0.8f, 2.6f), rng.range(0f, PI.toFloat()),
            floatArrayOf(shade, shade, shade), floatArrayOf(MAT_SHRUB, 1f, 0.08f, 0.72f))
        shrubs++
    }

    var verts = 0; var tris = 0
    for (b in builders) { verts += b.vcount; tris += b.tcount }
    return MountainSceneData(builders, MountainCounts(rocks, shrubs, builders.size, verts, tris))
}

/** Mountain vertex shader — identical GLSL to the HTML preview. */
private const val MOUNTAIN_VERTEX_SHADER = """
attribute vec3 aPos; attribute vec3 aNormal; attribute vec3 aColor; attribute vec4 aData;
uniform mat4 uMVP;
varying vec3 vPos; varying vec3 vNor; varying vec3 vCol; varying vec4 vDat;
void main(){
  vPos=aPos; vNor=aNormal; vCol=aColor; vDat=aData;
  gl_Position=uMVP*vec4(aPos,1.0);
}
"""

/** Mountain fragment shader — identical GLSL to the HTML preview:
 *  slope/material splat, rock strata/fluting, seasonal scrub, snow. */
private const val MOUNTAIN_FRAGMENT_SHADER = """
precision highp float;
varying vec3 vPos; varying vec3 vNor; varying vec3 vCol; varying vec4 vDat;
uniform vec3 uSunDir; uniform vec3 uSunCol;
uniform vec3 uMoonDir; uniform vec3 uMoonCol;
uniform vec3 uSky; uniform vec3 uGround; uniform vec3 uCamPos;
uniform float uSeason;

float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7)))*43758.5453123); }
float vnoise(vec2 p){
  vec2 i=floor(p), f=fract(p);
  vec2 u=f*f*(3.0-2.0*f);
  return mix(mix(hash(i),hash(i+vec2(1.0,0.0)),u.x),
             mix(hash(i+vec2(0.0,1.0)),hash(i+vec2(1.0,1.0)),u.x), u.y);
}
float fbm(vec2 p){
  float s=0.0, a=0.5;
  for(int i=0;i<4;i++){ s+=a*vnoise(p); p=p*2.03+vec2(17.3,9.1); a*=0.5; }
  return s/0.9375;
}
// Rounded cellular blobs: scrub crowns / scree stones.
float blob(vec2 p){
  vec2 id=floor(p), f=fract(p);
  float d=8.0;
  for(int i=-1;i<=1;i++) for(int j=-1;j<=1;j++){
    vec2 g=vec2(float(i),float(j));
    vec2 o=vec2(hash(id+g), hash(id+g+19.19));
    d=min(d, length(g+o-f));
  }
  return 1.0-smoothstep(0.05,0.85,d);
}
float detailHeight(vec2 q, float vegF){
  float rockH=0.0;
  if(vegF<0.98) rockH=fbm(q*0.075)*0.62+fbm(q*0.018+4.7)*0.38;
  float vegH=0.0;
  if(vegF>0.02) vegH=blob(q*0.14)*0.62+blob(q*0.035+7.0)*0.38;
  return mix(rockH, vegH, vegF);
}
vec3 aces(vec3 x){ return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0); }

void main(){
  vec3 n=normalize(vNor);
  vec3 p=vPos;
  float mat=vDat.x;
  float veg=vDat.y;
  float cavity=clamp(vDat.z,0.0,1.0);
  float snowRet=clamp(vDat.w,0.0,1.0);
  bool isCliff = mat<0.5 || (mat>2.5&&mat<3.5) || (mat>4.5&&mat<5.5) || mat>6.5;
  bool isScree = mat>0.5&&mat<1.5;
  bool isGully = mat>3.5&&mat<4.5;
  bool isShrub = mat>5.5&&mat<6.5;

  // Triplanar-style detail coordinates + matching tangent frame for bump.
  vec3 an=abs(n);
  vec2 duv; vec3 tang; vec3 bitan;
  if(an.y>=an.x && an.y>=an.z){ duv=p.xz; tang=vec3(1.0,0.0,0.0); bitan=vec3(0.0,0.0,1.0); }
  else if(an.z>=an.x){ duv=p.xy; tang=vec3(1.0,0.0,0.0); bitan=vec3(0.0,1.0,0.0); }
  else { duv=p.zy; tang=vec3(0.0,0.0,1.0); bitan=vec3(0.0,1.0,0.0); }

  float macro=fbm(p.xz*0.0024+3.1);
  float med=fbm(duv*0.018+7.7);
  float fine=fbm(duv*0.085+1.3);
  float micro=vnoise(duv*0.33);

  // --- Rock: broken strata, diagonal fluting, stains, cracks ---
  vec3 rock=vCol;
  if(uSeason>0.5&&uSeason<1.5) rock*=vec3(1.09,0.94,0.79);
  if(uSeason>1.5) rock=mix(rock, vec3(dot(rock,vec3(0.333))), 0.38)*1.06;
  rock*=1.16;
  float rockLum=dot(rock,vec3(0.333));
  rock=mix(rock, vec3(rockLum), 0.16);                       // grey weathered faces
  float strata=0.94+0.11*sin(p.y*0.055+fbm(p.xy*0.006)*10.0+med*3.0);
  float flute=0.90+0.20*fbm(vec2(p.x*0.040+p.y*0.012,p.y*0.004)+2.2);
  float stain=0.86+0.26*fbm(vec2(p.x*0.023,p.y*0.0016)+6.4); // vertical weathering streaks
  float crack1=smoothstep(0.040,0.0,abs(fbm(duv*0.050+11.0)-0.5));
  float crack2=smoothstep(0.030,0.0,abs(fbm(duv*0.013+3.3)-0.5));
  rock*=strata*flute*stain*(0.62+0.55*med)*(0.78+0.38*fine)*(1.0-0.42*crack1)*(1.0-0.30*crack2);

  float stone=blob(duv*0.20);
  vec3 scree=mix(rock, vec3(0.52,0.46,0.38), 0.40)*(0.76+0.40*fine)*(0.84+0.30*stone)*(0.88+0.20*micro);
  vec3 soil=mix(vec3(0.32,0.25,0.16), rock, 0.32)*(0.78+0.36*med)*(0.88+0.22*fine);
  if(!isShrub) soil=mix(soil, vCol, 0.45);

  // --- Seasonal scrub with crown-scale structure ---
  float patch=fbm(duv*0.012+5.5);
  float dry=fbm(duv*0.005+9.9);
  float canopy=blob(duv*0.14)*0.62+blob(duv*0.035+7.0)*0.38;
  float leafSpeck=hash(floor(duv*1.35));
  vec3 vegCol;
  if(uSeason<0.5){
    vegCol=mix(vec3(0.070,0.145,0.050), vec3(0.195,0.335,0.125), patch);
    vegCol=mix(vegCol, vec3(0.335,0.455,0.195), canopy*0.38);
    vegCol=mix(vegCol, vec3(0.36,0.40,0.20), smoothstep(0.58,0.78,dry)*0.32);
  } else if(uSeason<1.5){
    vegCol=mix(vec3(0.230,0.095,0.030), vec3(0.475,0.220,0.070), patch);
    vegCol=mix(vegCol, vec3(0.655,0.375,0.115), canopy*0.36);
    vegCol=mix(vegCol, vec3(0.40,0.37,0.15), smoothstep(0.62,0.82,dry)*0.30);
  } else {
    vegCol=mix(vec3(0.26,0.31,0.25), vec3(0.45,0.50,0.42), patch);
  }
  if(!isShrub){
    if(uSeason<0.5) vegCol=mix(vegCol, vCol*1.03, 0.92);
    else if(uSeason<1.5){ float pl=dot(vCol,vec3(0.333)); vegCol=mix(vegCol, vegCol*(0.45+1.65*pl), 0.60); }
    else vegCol=mix(vegCol, mix(vCol, vec3(dot(vCol,vec3(0.333))), 0.55), 0.45);
  }
  vegCol*=(0.82+0.24*canopy)*(0.90+0.18*fine)*(0.96+0.06*leafSpeck);
  if(uSeason<0.5){ float vegLum=dot(vegCol,vec3(0.333)); vegCol=mix(vegCol,vec3(vegLum),0.14)*0.88; }

  float steep=1.0-n.y;
  float vegF=clamp(veg*1.18+(patch-0.5)*0.22,0.0,1.0);
  vegF*=1.0-smoothstep(0.55,0.82,steep)*0.82;
  if(isShrub) vegF=1.0;

  vec3 albedo=soil;
  albedo=mix(albedo, vegCol, vegF);
  if(isScree) albedo=mix(scree, vegCol, vegF*0.45);
  if(isCliff) albedo=mix(rock, vegCol, vegF*0.75);
  if(isGully){
    vec3 g=mix(rock*0.70, soil*0.78, 0.45);
    albedo=mix(g, vegCol*0.9, clamp(vegF+0.12,0.0,1.0)*0.65);
  }
  if(isShrub) albedo=vegCol*vCol*1.18;
  albedo*=0.86+0.28*macro;

  // --- Snow season: slope/retention snow, scrub showing through ---
  float snowF=0.0;
  if(uSeason>1.5){
    snowF=clamp(snowRet*1.12+(med-0.5)*0.28-steep*0.22-smoothstep(0.62,0.85,fine)*0.16-smoothstep(0.55,0.82,med)*0.10-canopy*vegF*0.10,0.0,1.0);
    if(isShrub) snowF=clamp(0.35+0.5*n.y,0.0,1.0);
    vec3 snowCol=mix(vec3(0.58,0.66,0.76), vec3(0.96,0.98,1.0), fine)*(0.78+0.38*med);
    albedo=mix(albedo, snowCol, snowF);
  }

  // Slight aerial haze on the far back crest only.
  float haze=(1.0-smoothstep(-1400.0,-250.0,p.z))*0.10;
  albedo=mix(albedo, uSky*0.8, haze);

  // --- Bump-mapped detail normal ---
  float eps=1.6;
  float hC=detailHeight(duv, vegF);
  float hX=detailHeight(duv+vec2(eps,0.0), vegF);
  float hY=detailHeight(duv+vec2(0.0,eps), vegF);
  tang=normalize(tang-n*dot(n,tang));
  bitan=normalize(bitan-n*dot(n,bitan)-tang*dot(tang,bitan));
  float bumpAmp=mix(12.0, 6.0, vegF);
  if(uSeason>1.5) bumpAmp=mix(bumpAmp, 6.0, snowF);
  vec3 ln=normalize(n-(tang*((hX-hC)/eps)+bitan*((hY-hC)/eps))*bumpAmp);

  // --- Lighting ---
  float ao=1.0-0.50*cavity;
  vec3 amb=mix(uGround,uSky,ln.y*0.5+0.5)*0.62;
  float sd=clamp((dot(ln,uSunDir)+0.18)/1.18,0.0,1.0);
  float md=clamp((dot(ln,uMoonDir)+0.16)/1.16,0.0,1.0);
  vec3 col=albedo*(amb+uSunCol*sd+uMoonCol*md)*ao;

  vec3 vdir=normalize(uCamPos-p);
  vec3 hv=normalize(uSunDir+vdir);
  float spec=pow(max(dot(ln,hv),0.0),60.0);
  float sparkle=step(0.992,hash(floor(duv*1.7)))*snowF;
  col+=uSunCol*(spec*(0.16*snowF+0.030)+sparkle*0.35*max(dot(n,uSunDir),0.0));

  col=aces(col*1.08);
  float lum=dot(col,vec3(0.299,0.587,0.114));
  col=mix(col,vec3(lum),0.04);
  gl_FragColor=vec4(col,1.0);
}
"""

/** Direction TO a body from astronomical azimuth/altitude. World axes:
 *  +X east, +Y up, +Z south (north is −Z). */
private fun mountainDirFromAzAlt(azimuthDeg: Float, altitudeDeg: Float): FloatArray {
    val az = Math.toRadians(azimuthDeg.toDouble())
    val al = Math.toRadians(altitudeDeg.toDouble())
    return floatArrayOf(
        (sin(az) * cos(al)).toFloat(),
        sin(al).toFloat(),
        (-cos(az) * cos(al)).toFloat(),
    )
}

private fun mountainLerp3(a: FloatArray, b: FloatArray, f: Float): FloatArray =
    floatArrayOf(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f)

// ============================================================================
// Scene: GL resources + light state + static chunked VBOs.
// init()/draw()/release() MUST run on the GL thread; the setters are
// synchronised and may be called from any thread.
// ============================================================================

enum class MountainSeason { GREEN, AUTUMN, SNOW }

class MountainScene {

    /** World width (m) the fixed camera shows. 5200 (default) reproduces the
     *  HTML preview: the 4,800 m massif plus small side margins. */
    @Volatile var visibleWidthMeters: Float = 5200f

    /** Where world y=0 sits in the viewport, as a fraction from the top.
     *  0.985 (default) reproduces the HTML preview. */
    @Volatile var baseFractionFromTop: Float = 0.985f

    /** If true (default), zoom out on very wide/short viewports so the full
     *  ~1,050 m from base to crest is never cropped vertically. */
    @Volatile var autoFitHeight: Boolean = true

    @Volatile var season: MountainSeason = MountainSeason.GREEN
        private set

    /** Select the season. One shared mesh serves all seasons, so this only
     *  changes a shader uniform on the next draw. */
    fun setSeason(season: MountainSeason) { this.season = season }

    /** Geometry totals once the mesh has been built (null before [init]). */
    @Volatile var geometryCounts: MountainCounts? = null
        private set

    private val lock = Any()
    private var sunDir = mountainDirFromAzAlt(180f, 52f)
    private var sunColor = floatArrayOf(0f, 0f, 0f)
    private var moonDir = mountainDirFromAzAlt(120f, 35f)
    private var moonColor = floatArrayOf(0f, 0f, 0f)
    private var skyAmb = floatArrayOf(0.055f, 0.065f, 0.095f)
    private var groundAmb = floatArrayOf(0.03f, 0.03f, 0.035f)

    init { setSun(180f, 52f) }   // bright southerly preview light until the app drives it

    /** Move the Sun. Astronomical azimuth (0°=N, 90°=E, 180°=S, 270°=W).
     *  Colour, intensity and hemisphere ambient are derived from altitude:
     *  saturated orange near the horizon, near-white when high, fading
     *  through twilight to a dim blue night ambient by −12°. */
    fun setSun(azimuthDeg: Float, altitudeDeg: Float) {
        synchronized(lock) {
            sunDir = mountainDirFromAzAlt(azimuthDeg, altitudeDeg)
            if (altitudeDeg > -0.833f) {
                val alt = max(altitudeDeg, 0f)
                val warm = (1.0 - min(alt / 28f, 1f)).pow(1.35).toFloat()
                val inten = 0.25f + 1.05f * min(sin(Math.toRadians(alt.toDouble())).toFloat() * 2.4f, 1f)
                sunColor = floatArrayOf(inten, inten * (1f - 0.38f * warm), inten * (1f - 0.72f * warm))
                val a = 0.30f + 0.26f * min(alt / 40f, 1f)
                skyAmb = floatArrayOf(a * (1.02f + 0.22f * warm), a * (0.98f - 0.05f * warm), a * (1.05f - 0.38f * warm))
                groundAmb = floatArrayOf(a * 0.66f, a * 0.57f, a * 0.44f)
            } else {
                val f = ((altitudeDeg + 12f) / 11.167f).coerceIn(0f, 1f)
                sunColor = floatArrayOf(0.55f * f * f, 0.22f * f * f, 0.06f * f * f)
                skyAmb = mountainLerp3(floatArrayOf(0.055f, 0.065f, 0.095f), floatArrayOf(0.34f, 0.25f, 0.18f), f)
                groundAmb = mountainLerp3(floatArrayOf(0.03f, 0.03f, 0.035f), floatArrayOf(0.20f, 0.15f, 0.10f), f)
            }
        }
    }

    /** Explicit Sun light (colour already multiplied by intensity), bypassing
     *  the derivation in [setSun]. Does not touch the ambient terms. */
    fun setSunLight(azimuthDeg: Float, altitudeDeg: Float, r: Float, g: Float, b: Float) {
        synchronized(lock) {
            sunDir = mountainDirFromAzAlt(azimuthDeg, altitudeDeg)
            sunColor = floatArrayOf(r, g, b)
        }
    }

    /** Move the Moon (second directional light, cool blue-grey). [intensity]
     *  ≈ 0.22 is a bright full Moon; scale by illuminated fraction. */
    fun setMoon(azimuthDeg: Float, altitudeDeg: Float, intensity: Float = 0.22f) {
        synchronized(lock) {
            moonDir = mountainDirFromAzAlt(azimuthDeg, altitudeDeg)
            val fade = (sin(Math.toRadians(max(altitudeDeg, 0f).toDouble())) * 3.0).coerceIn(0.0, 1.0).toFloat()
            val i = if (altitudeDeg <= 0f) 0f else intensity * fade
            moonColor = floatArrayOf(0.68f * i, 0.76f * i, 1.0f * i)
        }
    }

    /** Override hemisphere ambient. Note [setSun] recomputes it; call after. */
    fun setAmbient(skyR: Float, skyG: Float, skyB: Float, groundR: Float, groundG: Float, groundB: Float) {
        synchronized(lock) {
            skyAmb = floatArrayOf(skyR, skyG, skyB)
            groundAmb = floatArrayOf(groundR, groundG, groundB)
        }
    }

    // ---- GL resources (GL thread only) ----
    private class Chunk(val buffers: IntArray, val indexCount: Int)   // pos, normal, colour, data, index
    private class Prog(val id: Int) {
        var uMVP = -1; var uSunDir = -1; var uSunCol = -1
        var uMoonDir = -1; var uMoonCol = -1
        var uSky = -1; var uGround = -1; var uCamPos = -1; var uSeason = -1
    }
    private var prog: Prog? = null
    private var chunks: ArrayList<Chunk>? = null
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val mvp = FloatArray(16)

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(s)
            GLES20.glDeleteShader(s)
            throw RuntimeException("Mountain shader compile failed: $log")
        }
        return s
    }

    private fun floatBuffer(data: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }

    private fun makeProgram(): Prog {
        val vs = compile(GLES20.GL_VERTEX_SHADER, MOUNTAIN_VERTEX_SHADER)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, MOUNTAIN_FRAGMENT_SHADER)
        val id = GLES20.glCreateProgram()
        GLES20.glAttachShader(id, vs)
        GLES20.glAttachShader(id, fs)
        GLES20.glBindAttribLocation(id, 0, "aPos")
        GLES20.glBindAttribLocation(id, 1, "aNormal")
        GLES20.glBindAttribLocation(id, 2, "aColor")
        GLES20.glBindAttribLocation(id, 3, "aData")
        GLES20.glLinkProgram(id)
        GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) throw RuntimeException("Mountain program link failed: ${GLES20.glGetProgramInfoLog(id)}")
        val p = Prog(id)
        p.uMVP = GLES20.glGetUniformLocation(id, "uMVP")
        p.uSunDir = GLES20.glGetUniformLocation(id, "uSunDir")
        p.uSunCol = GLES20.glGetUniformLocation(id, "uSunCol")
        p.uMoonDir = GLES20.glGetUniformLocation(id, "uMoonDir")
        p.uMoonCol = GLES20.glGetUniformLocation(id, "uMoonCol")
        p.uSky = GLES20.glGetUniformLocation(id, "uSky")
        p.uGround = GLES20.glGetUniformLocation(id, "uGround")
        p.uCamPos = GLES20.glGetUniformLocation(id, "uCamPos")
        p.uSeason = GLES20.glGetUniformLocation(id, "uSeason")
        return p
    }

    private fun upload(mesh: MountainMeshBuilder): Chunk {
        require(mesh.vcount < 65536) { "Mountain chunk too large for 16-bit indices: ${mesh.vcount}" }
        val b = IntArray(5)
        GLES20.glGenBuffers(5, b, 0)
        val arrays = arrayOf(mesh.pos.toFloatArray(), mesh.nor.toFloatArray(), mesh.col.toFloatArray(), mesh.dat.toFloatArray())
        for (i in 0..3) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, b[i])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, arrays[i].size * 4, floatBuffer(arrays[i]), GLES20.GL_STATIC_DRAW)
        }
        val sb: ShortBuffer = ByteBuffer.allocateDirect(mesh.idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        for (i in mesh.idx) sb.put(i.toShort())   // bit pattern preserved; read back as UNSIGNED_SHORT
        sb.position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, b[4])
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mesh.idx.size * 2, sb, GLES20.GL_STATIC_DRAW)
        return Chunk(b, mesh.idx.size)
    }

    private fun ensureUploaded(): ArrayList<Chunk> {
        chunks?.let { return it }
        val data = buildMountainScene()
        geometryCounts = data.counts
        val out = ArrayList<Chunk>()
        for (b in data.builders) out.add(upload(b))
        chunks = out
        return out
    }

    /** Build and upload the mesh. Call once per GL context. */
    fun init() {
        prog = makeProgram()
        ensureUploaded()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun drawMesh(p: Prog, list: ArrayList<Chunk>, eye: FloatArray) {
        GLES20.glUseProgram(p.id)
        GLES20.glUniformMatrix4fv(p.uMVP, 1, false, mvp, 0)
        synchronized(lock) {
            GLES20.glUniform3fv(p.uSunDir, 1, sunDir, 0)
            GLES20.glUniform3fv(p.uSunCol, 1, sunColor, 0)
            GLES20.glUniform3fv(p.uMoonDir, 1, moonDir, 0)
            GLES20.glUniform3fv(p.uMoonCol, 1, moonColor, 0)
            GLES20.glUniform3fv(p.uSky, 1, skyAmb, 0)
            GLES20.glUniform3fv(p.uGround, 1, groundAmb, 0)
        }
        GLES20.glUniform3fv(p.uCamPos, 1, eye, 0)
        GLES20.glUniform1f(p.uSeason, season.ordinal.toFloat())
        for (a in 0..3) GLES20.glEnableVertexAttribArray(a)
        for (chunk in list) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunk.buffers[0])
            GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunk.buffers[1])
            GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunk.buffers[2])
            GLES20.glVertexAttribPointer(2, 3, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunk.buffers[3])
            GLES20.glVertexAttribPointer(3, 4, GLES20.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, chunk.buffers[4])
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, chunk.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
        }
        for (a in 0..3) GLES20.glDisableVertexAttribArray(a)
    }

    /** Draw one frame. Does not clear colour unless [clear] is true (for a
     *  standalone transparent surface composited over your sky). */
    fun draw(width: Int, height: Int, clear: Boolean = false) {
        val p = prog ?: return
        if (width <= 0 || height <= 0) return
        val list = ensureUploaded()
        GLES20.glViewport(0, 0, width, height)
        if (clear) {
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        }
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)   // skirts/edges stay correct from the one fixed view

        val aspect = width.toFloat() / height.toFloat()
        var visW = visibleWidthMeters
        if (autoFitHeight) {
            val needW = aspect * (1050f / max(baseFractionFromTop, 0.05f))
            if (needW > visW) visW = needW
        }
        val visH = visW / aspect
        val eye = floatArrayOf(0f, 350f, 6000f)
        Matrix.setLookAtM(view, 0, eye[0], eye[1], eye[2], 0f, 550f, 0f, 0f, 1f, 0f)
        // Ortho bounds live in view space: centre X on the massif centre and
        // solve Y so the front base edge (0,0,1100) sits at baseFractionFromTop.
        val cv = FloatArray(4)
        Matrix.multiplyMV(cv, 0, view, 0, floatArrayOf(0f, 0f, 0f, 1f), 0)
        val centreX = cv[0]
        Matrix.multiplyMV(cv, 0, view, 0, floatArrayOf(0f, 0f, 1100f, 1f), 0)
        val centreY = cv[1] + (2f * baseFractionFromTop - 1f) * visH / 2f
        Matrix.orthoM(proj, 0, centreX - visW / 2f, centreX + visW / 2f, centreY - visH / 2f, centreY + visH / 2f, 100f, 12000f)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        drawMesh(p, list, eye)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    /** Delete GL resources. Call on the GL thread when the context ends. */
    fun release() {
        chunks?.let { for (chunk in it) if (chunk.buffers[0] != 0) GLES20.glDeleteBuffers(5, chunk.buffers, 0) }
        chunks = null
        prog?.let { GLES20.glDeleteProgram(it.id) }; prog = null
    }
}

