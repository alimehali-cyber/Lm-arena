package com.zig.avatar

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Draws the avatar with OpenGL ES 3.0 only - no GLES 3.1/3.2 features, no compute, no
 * extensions. ZIG renders with GLES30, and this package must not raise that floor.
 *
 * Skinning and morphing are done on the CPU and uploaded as a dynamic VBO. That is a
 * deliberate trade: it costs one 17k-vertex transform per frame, and in exchange the
 * shader needs no bone-matrix uniform array, no float-texture lookup, and no extension
 * that might be missing on an old device we cannot test on. If profiling on a real phone
 * ever justifies it, moving skinning to the vertex shader is a contained change.
 */
class AvatarRenderer(
    private val assets: AssetManager,
    private val assetName: String,
) : GLSurfaceView.Renderer {

    /** Called on the GL thread after the model has loaded. */
    @Volatile var onLoaded: ((AvatarModel) -> Unit)? = null

    @Volatile private var model: AvatarModel? = null
    @Volatile private var player: AvatarPlayer? = null
    @Volatile private var pendingClip: String? = null
    @Volatile private var pendingSpeed = 1f
    @Volatile private var pendingReduced = false

    var orbitDeg: Float = 18f
    var pitchOffset: Float = 0.16f
    var background: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)

    private var program = 0
    private var uMvp = 0
    private var uColor = 0
    private var uLight = 0
    private var uEyeGloss = 0

    private val gpu = ArrayList<GpuPrimitive>()

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProj = FloatArray(16)
    private var lastNs = 0L

    private class GpuPrimitive(
        val mesh: AvatarModel.Primitive,
        val vbo: Int,
        val ibo: Int,
        val indexCount: Int,
        val outPos: FloatArray,
        val outNor: FloatArray,
    )

    // ---- lifecycle ---------------------------------------------------------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(background[0], background[1], background[2], background[3])
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        uMvp = GLES30.glGetUniformLocation(program, "uMvp")
        uColor = GLES30.glGetUniformLocation(program, "uColor")
        uLight = GLES30.glGetUniformLocation(program, "uLightDir")
        uEyeGloss = GLES30.glGetUniformLocation(program, "uGloss")

        try {
            val bytes = assets.open(assetName).use { it.readBytes() }
            val m = AvatarModel.load(bytes)
            model = m
            val p = AvatarPlayer(m)
            player = p
            buildBuffers(m)
            pendingClip?.let { p.play(it, loop = true) }
            p.speed = pendingSpeed
            p.reducedMotion = pendingReduced
            onLoaded?.invoke(m)
        } catch (e: Exception) {
            // Fail visibly in logcat rather than drawing nothing with no explanation.
            android.util.Log.e("AvatarRenderer", "avatar load failed: ${e.message}", e)
            throw e
        }
        lastNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = if (height == 0) 1f else width.toFloat() / height.toFloat()
        val m = model ?: return
        val y0 = m.bounds[1]
        val y1 = m.bounds[4]
        val height3 = (y1 - y0).coerceAtLeast(0.1f)
        val cy = (y0 + y1) * 0.5f
        // Frame the whole character with a little headroom, the same way the Blender
        // review renderer solves its camera distance.
        val fovY = 30f
        val dist = (height3 * 1.18f * 0.5f) / kotlin.math.tan(Math.toRadians(fovY / 2.0)).toFloat()
        val a = Math.toRadians(orbitDeg.toDouble())
        val ex = (sin(a) * dist).toFloat()
        val ez = (cos(a) * dist).toFloat()
        M4.perspective(proj, fovY, aspect, 0.05f, 40f)
        M4.lookAt(view, ex, cy + height3 * pitchOffset, ez, 0f, cy, 0f)
        M4.mul(viewProj, proj, view)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        val m = model ?: return
        val p = player ?: return

        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.1f)  // clamp after a stall
        lastNs = now
        p.update(dt)

        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uMvp, 1, false, viewProj, 0)
        GLES30.glUniform3f(uLight, 0.35f, 0.72f, 0.60f)

        val morph = p.morphWeights
        for (g in gpu) {
            transform(g, p.boneMatrices, morph)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, g.vbo)
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, g.outPos.size * 4, g.outPos.toDirect())
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, g.outPos.size * 4, g.outNor.size * 4, g.outNor.toDirect())

            val stride = 6 * 4
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)

            val c = g.mesh.material.rgba
            GLES30.glUniform4f(uColor, c[0], c[1], c[2], c[3])
            GLES30.glUniform1f(uEyeGloss, if (g.mesh.material.name == "EyeWhite" || g.mesh.material.name == "Iris") 0.55f else 0.05f)

            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, g.ibo)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, g.indexCount, GLES30.GL_UNSIGNED_INT, 0)
        }
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    // ---- CPU skinning ------------------------------------------------------

    private fun transform(g: GpuPrimitive, bones: FloatArray, morph: FloatArray) {
        val prim = g.mesh
        val pos = prim.positions
        val nor = prim.normals
        val jnt = prim.joints
        val wgt = prim.weights
        val out = g.outPos
        val outN = g.outNor
        val verts = prim.vertexCount

        // Which morph targets are actually contributing this frame.
        val active = ArrayList<Int>(4)
        for (t in prim.morphDeltas.indices) {
            val w = if (t < morph.size) morph[t] else 0f
            if (w > 0.002f) active.add(t)
        }

        for (v in 0 until verts) {
            val o = v * 3
            var px = pos[o]; var py = pos[o + 1]; var pz = pos[o + 2]
            var nx = nor[o]; var ny = nor[o + 1]; var nz = nor[o + 2]

            for (ti in active) {
                val w = morph[ti]
                val d = prim.morphDeltas[ti]
                px += d[o] * w; py += d[o + 1] * w; pz += d[o + 2] * w
            }

            var sx = 0f; var sy = 0f; var sz = 0f
            var tx = 0f; var ty = 0f; var tz = 0f
            val jo = v * 4
            // 4 influences, the glTF limit and what this pipeline exports
            for (k in 0 until 4) {
                val w = wgt[jo + k]
                if (w <= 0f) continue
                val b = jnt[jo + k] * 16
                val m0 = bones[b]; val m1 = bones[b + 1]; val m2 = bones[b + 2]
                val m4 = bones[b + 4]; val m5 = bones[b + 5]; val m6 = bones[b + 6]
                val m8 = bones[b + 8]; val m9 = bones[b + 9]; val m10 = bones[b + 10]
                val m12 = bones[b + 12]; val m13 = bones[b + 13]; val m14 = bones[b + 14]
                sx += w * (m0 * px + m4 * py + m8 * pz + m12)
                sy += w * (m1 * px + m5 * py + m9 * pz + m13)
                sz += w * (m2 * px + m6 * py + m10 * pz + m14)
                tx += w * (m0 * nx + m4 * ny + m8 * nz)
                ty += w * (m1 * nx + m5 * ny + m9 * nz)
                tz += w * (m2 * nx + m6 * ny + m10 * nz)
            }
            out[o] = sx; out[o + 1] = sy; out[o + 2] = sz
            val len = sqrt(tx * tx + ty * ty + tz * tz)
            if (len > 1e-6f) { outN[o] = tx / len; outN[o + 1] = ty / len; outN[o + 2] = tz / len }
            else { outN[o] = 0f; outN[o + 1] = 1f; outN[o + 2] = 0f }
        }
    }

    // ---- setup -------------------------------------------------------------

    private fun buildBuffers(m: AvatarModel) {
        for (p in m.primitives) {
            val interleaved = FloatArray(p.vertexCount * 6)
            val vbo = IntArray(1)
            GLES30.glGenBuffers(1, vbo, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, interleaved.size * 4, null, GLES30.GL_DYNAMIC_DRAW)

            val idx = IntArray(1)
            GLES30.glGenBuffers(1, idx, 0)
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, idx[0])
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, p.indices.size * 4, p.indices.toDirect(), GLES30.GL_STATIC_DRAW)

            gpu.add(GpuPrimitive(p, vbo[0], idx[0], p.indices.size,
                FloatArray(p.vertexCount * 3), FloatArray(p.vertexCount * 3)))
        }
    }

    private fun buildProgram(vs: String, fs: String): Int {
        val v = compile(GLES30.GL_VERTEX_SHADER, vs)
        val f = compile(GLES30.GL_FRAGMENT_SHADER, fs)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, v)
        GLES30.glAttachShader(prog, f)
        GLES30.glBindAttribLocation(prog, 0, "aPos")
        GLES30.glBindAttribLocation(prog, 1, "aNor")
        GLES30.glLinkProgram(prog)
        val status = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(prog)
            throw IllegalStateException("avatar shader link failed: $log")
        }
        GLES30.glDeleteShader(v)
        GLES30.glDeleteShader(f)
        return prog
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, src)
        GLES30.glCompileShader(s)
        val status = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(s)
            GLES30.glDeleteShader(s)
            throw IllegalStateException("avatar shader compile failed: $log")
        }
        return s
    }

    // ---- host API (thread-safe) -------------------------------------------

    fun playClip(name: String, fadeSeconds: Float = 0.18f) {
        pendingClip = name
        player?.play(name, loop = true, fadeSeconds = fadeSeconds)
    }

    fun oneShot(name: String, fadeSeconds: Float = 0.12f) {
        player?.play(name, loop = false, fadeSeconds = fadeSeconds)
    }

    fun setSpeed(speed: Float) {
        pendingSpeed = speed
        player?.speed = speed
    }

    fun setReducedMotion(reduced: Boolean) {
        pendingReduced = reduced
        player?.reducedMotion = reduced
    }

    fun setVisemes(json: String?) {
        val p = player ?: return
        val m = model ?: return
        p.speech = if (json == null) null else VisemeTimeline.parse(json, m).first
    }

    fun currentMorphWeights(): FloatArray? = player?.morphWeights?.copyOf()

    fun currentClip(): String? = player?.clipName

    private fun FloatArray.toDirect(): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(this@toDirect); position(0)
        }

    private fun IntArray.toDirect(): IntBuffer =
        ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().apply {
            put(this@toDirect); position(0)
        }

    companion object {
        private const val VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNor;
            uniform mat4 uMvp;
            out vec3 vNor;
            void main() {
                vNor = aNor;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            #version 300 es
            precision mediump float;
            in vec3 vNor;
            uniform vec4 uColor;
            uniform vec3 uLightDir;
            uniform float uGloss;
            out vec4 fragColor;
            void main() {
                vec3 n = normalize(vNor);
                vec3 l = normalize(uLightDir);
                float ndl = max(dot(n, l), 0.0);
                // half-lambert wrap keeps the shadow side readable on a small widget
                float wrap = 0.5 + 0.5 * dot(n, l);
                float diff = mix(wrap, ndl, 0.55);
                vec3 h = normalize(l + vec3(0.0, 0.0, 1.0));
                float spec = pow(max(dot(n, h), 0.0), 24.0) * uGloss;
                vec3 base = uColor.rgb * (0.38 + 0.72 * diff) + vec3(spec);
                // base colour is linear (glTF), the surface is not an sRGB framebuffer
                fragColor = vec4(pow(base, vec3(1.0 / 2.2)), uColor.a);
            }
        """
    }
}
