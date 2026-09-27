package com.zig.gargantua.renderer

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * TEMPORARY physical-GPU pipeline diagnostics.
 *
 * Active only while [GargantuaRenderState.diagnosticView] is not [View.OFF]; with the view OFF the
 * renderer never calls into this class (no programs, textures, FBOs, queries or readbacks exist) and
 * normal rendering is unchanged. Every number reported here is measured from the real GL textures of
 * the running renderer: GPU reductions/probes (texelFetch) write raw float bits or counts into an
 * RGBA32UI target, which is read back with GL_RGBA_INTEGER / GL_UNSIGNED_INT (a small readback of the
 * GPU result, not a CPU re-computation of the image).
 */
internal class GargantuaGpuDiagnostics(private val context: Context) {

    enum class View(val label: String) {
        OFF("OFF"),
        D0_STATIC_HDR("D0 static HDR cache"),
        D1_RAY_RECORDS("D1 ray records (R=r G=phi B=valid, magenta=truncated)"),
        D2_MATERIAL("D2 material-pass output"),
        D3_BRIGHTPASS("D3 brightpass"),
        D4_BLOOM("D4 bloom"),
        D5_PRE_TONEMAP("D5 composite before ACES"),
        D6_FINAL("D6 final (real composite)"),
        D7_HIGHER_ORDER("D7 higher-order / total");

        companion object {
            fun fromIndex(index: Int): View = values().getOrElse(index) { OFF }
        }
    }

    /** A/B switch of the integer/sampler precision of the animation build + material programs. */
    enum class RecordPrecision(val label: String) {
        AS_SHIPPED("as shipped"),
        HIGHP_INT("highp int"),
        HIGHP_INT_SAMPLER("highp int+sampler");

        companion object {
            fun fromIndex(index: Int): RecordPrecision = values().getOrElse(index) { AS_SHIPPED }
        }
    }

    /** Everything the diagnostics read from one renderer frame (texture/FBO names are the real ones). */
    class Frame(
        val nowNanos: Long,
        val view: View,
        val precision: RecordPrecision,
        val surfaceW: Int,
        val surfaceH: Int,
        val renderW: Int,
        val renderH: Int,
        val rayW: Int,
        val rayH: Int,
        val blockSize: Int,
        val hdrTexture: Int,
        val presentationTexture: Int,
        val modulatedTexture: Int,
        val recordTexture0: Int,
        val bloomTexture: Int,
        val bloomW: Int,
        val bloomH: Int,
        val bloomEnabled: Boolean,
        val bloomThreshold: Float,
        val bloomIntensity: Float,
        val exposure: Float,
        val brightPass: ShaderProgram?,
        val composite: ShaderProgram?,
        val quad: QuadGeometry,
        val animationActive: Boolean,
        val animationRequested: Boolean,
        val amplitudePercent: Int,
        val gateAction: String,
        val geodesicPasses: Int,
        val rebuilds: Int,
        val animationStatus: String,
        val camera: String,
        val fbos: List<Pair<String, Int>>,
        val drawRealComposite: () -> Unit
    )

    private class Series(val amplitude: Int, val startNanos: Long) {
        val material = arrayOfNulls<FloatArray>(3)
        val static = arrayOfNulls<FloatArray>(3)
        val times = LongArray(3)
    }

    private class AmpResult(
        val rmsVsStatic: Float,
        val changedPixels: Int,
        val diskPixels: Int,
        val diskChanged: Int,
        val shadowChanged: Int,
        val probeRms01: Float,
        val probeRms12: Float,
        val skyDelta: Float,
        val shadowDelta: Float
    )

    private var viewProgram: ShaderProgram? = null
    private var statsProgram: ShaderProgram? = null
    private var probeProgram: ShaderProgram? = null
    private var preToneProgram: ShaderProgram? = null
    private var programFailure: String? = null
    private var programsAttempted = false

    private var statsFbo = 0
    private var statsTex = 0
    private var probeFbo = 0
    private var probeTex = 0
    private var brightFbo = 0
    private var brightTex = 0
    private var brightW = 0
    private var brightH = 0
    private var preFbo = 0
    private var preTex = 0
    private var finalFbo = 0
    private var finalTex = 0
    private var stageW = 0
    private var stageH = 0

    private var capsText: String? = null
    private var hasTimer = false
    private val timerQueries = HashMap<String, Int>()
    private val timerPending = HashMap<String, Boolean>()
    private val timerMs = LinkedHashMap<String, Float>()
    private var timerOpen: String? = null

    private val passLog = LinkedHashMap<String, String>()
    private var passLogArmed = false
    private var lastMeasureNanos = 0L
    private var lastView = View.OFF
    private var series: Series? = null
    private val ampResults = sortedMapOf<Int, AmpResult>()
    private var report = ""
    private var lastLogNanos = 0L

    private val statsBuffer: IntBuffer =
        ByteBuffer.allocateDirect(STATS_GRID * STATS_GRID * 16).order(ByteOrder.nativeOrder()).asIntBuffer()
    private val probeBuffer: IntBuffer =
        ByteBuffer.allocateDirect(SCAN_SAMPLES * PROBE_ROWS * 16).order(ByteOrder.nativeOrder()).asIntBuffer()

    val reportText: String get() = report

    // ------------------------------------------------------------------ renderer hooks

    /** Call right after a draw while its state is still bound. Pure GL state queries, no GL errors. */
    fun passSnapshot(name: String, inputUnits: IntArray) {
        // Rare passes (cache build/completion) are captured whenever they run; frequent ones when armed.
        if (!passLogArmed && passLog.containsKey(name)) return
        val v = IntArray(4)
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, v, 0)
        val fbo = v[0]
        val attachments = IntArray(4)
        if (fbo != 0) {
            for (i in 0 until 4) {
                GLES30.glGetFramebufferAttachmentParameteriv(
                    GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 + i,
                    GLES30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, v, 0
                )
                if (v[0] == GLES30.GL_TEXTURE) {
                    GLES30.glGetFramebufferAttachmentParameteriv(
                        GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 + i,
                        GLES30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, v, 0
                    )
                    attachments[i] = v[0]
                }
            }
        }
        val drawBuffers = IntArray(4)
        for (i in 0 until 4) {
            GLES30.glGetIntegerv(GLES30.GL_DRAW_BUFFER0 + i, v, 0)
            drawBuffers[i] = v[0]
        }
        GLES30.glGetIntegerv(GLES30.GL_VIEWPORT, v, 0)
        val viewport = "${v[0]},${v[1]},${v[2]}x${v[3]}"
        GLES30.glGetIntegerv(GLES30.GL_ACTIVE_TEXTURE, v, 0)
        val activeUnit = v[0]
        val inputs = StringBuilder()
        var hazard = false
        for (unit in inputUnits) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
            GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, v, 0)
            inputs.append(" u").append(unit).append('=').append(v[0])
            if (v[0] != 0 && attachments.contains(v[0])) hazard = true
        }
        GLES30.glActiveTexture(activeUnit)
        val db = drawBuffers.joinToString(",") { drawBufferName(it) }
        passLog[name] = String.format(
            Locale.US, "%s: fbo=%d att=[%s] draw=[%s] vp=%s in:%s%s",
            name, fbo, attachments.joinToString(","), db, viewport, inputs.toString(),
            if (hazard) "  ** READ/WRITE FEEDBACK HAZARD **" else " (no feedback)"
        )
    }

    fun beginTimer(name: String) {
        if (!hasTimer || timerOpen != null || timerPending[name] == true) return
        val id = timerQueries.getOrPut(name) {
            val q = IntArray(1)
            GLES30.glGenQueries(1, q, 0)
            q[0]
        }
        GLES30.glBeginQuery(GL_TIME_ELAPSED_EXT, id)
        timerOpen = name
    }

    fun endTimer(name: String) {
        if (timerOpen != name) return
        GLES30.glEndQuery(GL_TIME_ELAPSED_EXT)
        timerPending[name] = true
        timerOpen = null
    }

    private fun pollTimers() {
        if (!hasTimer) return
        val v = IntArray(1)
        GLES30.glGetIntegerv(GL_GPU_DISJOINT_EXT, v, 0)
        val disjoint = v[0] != 0
        for ((name, id) in timerQueries) {
            if (timerPending[name] != true) continue
            GLES30.glGetQueryObjectuiv(id, GLES30.GL_QUERY_RESULT_AVAILABLE, v, 0)
            if (v[0] == 0) continue
            GLES30.glGetQueryObjectuiv(id, GLES30.GL_QUERY_RESULT, v, 0)
            timerPending[name] = false
            if (!disjoint) timerMs[name] = (v[0].toLong() and 0xFFFFFFFFL) / 1.0e6f
        }
    }

    /** Runs after the normal frame. Draws the selected view to the default framebuffer. */
    fun afterFrame(frame: Frame): String {
        if (!ensurePrograms()) {
            report = "DIAG: programs failed: $programFailure"
            return report
        }
        lastFrameQuad = frame.quad
        if (capsText == null) capsText = collectCaps()
        pollTimers()
        ensureStageTargets(frame)
        renderStages(frame)

        val viewChanged = frame.view != lastView
        lastView = frame.view
        val measure = viewChanged || frame.nowNanos - lastMeasureNanos >= MEASURE_INTERVAL_NANOS
        val seriesCaptured = updateSeries(frame)
        if (measure || seriesCaptured) {
            lastMeasureNanos = frame.nowNanos
            report = buildReport(frame)
            passLogArmed = true // capture the pass state of the next frame
            if (frame.nowNanos - lastLogNanos >= LOG_INTERVAL_NANOS) {
                lastLogNanos = frame.nowNanos
                report.lineSequence().forEach { Log.i(TAG, it) }
            }
        } else {
            passLogArmed = false
        }
        drawView(frame)
        return report
    }

    fun release() {
        listOf(viewProgram, statsProgram, probeProgram, preToneProgram).forEach { it?.release() }
        viewProgram = null; statsProgram = null; probeProgram = null; preToneProgram = null
        programsAttempted = false
        programFailure = null
        deleteTargets()
        if (timerQueries.isNotEmpty()) {
            val ids = timerQueries.values.toIntArray()
            GLES30.glDeleteQueries(ids.size, ids, 0)
        }
        timerQueries.clear(); timerPending.clear(); timerMs.clear(); timerOpen = null
        capsText = null
        series = null
        passLog.clear()
        report = ""
    }

    /** GL object names are invalid after an EGL context loss; forget them without deleting. */
    fun forgetContext() {
        viewProgram = null; statsProgram = null; probeProgram = null; preToneProgram = null
        programsAttempted = false
        statsFbo = 0; statsTex = 0; probeFbo = 0; probeTex = 0
        brightFbo = 0; brightTex = 0; brightW = 0; brightH = 0
        preFbo = 0; preTex = 0; finalFbo = 0; finalTex = 0; stageW = 0; stageH = 0
        timerQueries.clear(); timerPending.clear(); timerOpen = null
        capsText = null
    }

    // ------------------------------------------------------------------ resources

    private fun ensurePrograms(): Boolean {
        if (viewProgram != null && statsProgram != null && probeProgram != null && preToneProgram != null) return true
        if (programsAttempted) return false
        programsAttempted = true
        val vertex = ShaderSource.loadVertexShader(context)
        fun create(label: String, fragment: String): ShaderProgram? =
            ShaderProgram.create(vertex, fragment) { programFailure = "$label ${it.label}: ${it.log.take(200)}" }
        viewProgram = create("view", ShaderSource.loadAsset(context, "shaders/gargantua_diag_view.frag"))
        statsProgram = create("stats", ShaderSource.loadAsset(context, "shaders/gargantua_diag_stats.frag"))
        probeProgram = create("probe", ShaderSource.loadAsset(context, "shaders/gargantua_diag_probe.frag"))
        preToneProgram = create("preTone", preToneMappingCompositeSource(ShaderSource.loadCompositeFragmentShader(context)))
        return viewProgram != null && statsProgram != null && probeProgram != null && preToneProgram != null
    }

    private fun texture(internal: Int, w: Int, h: Int, format: Int, type: Int, filter: Int): Int {
        val t = IntArray(1)
        GLES30.glGenTextures(1, t, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, t[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, internal, w, h, 0, format, type, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, filter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, filter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return t[0]
    }

    private fun fbo(tex: Int): Int {
        val f = IntArray(1)
        GLES30.glGenFramebuffers(1, f, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, f[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, tex, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return f[0]
    }

    private fun ensureStageTargets(frame: Frame) {
        if (statsFbo == 0) {
            statsTex = texture(GLES30.GL_RGBA32UI, STATS_GRID, STATS_GRID, GLES30.GL_RGBA_INTEGER, GLES30.GL_UNSIGNED_INT, GLES30.GL_NEAREST)
            statsFbo = fbo(statsTex)
            probeTex = texture(GLES30.GL_RGBA32UI, SCAN_SAMPLES, PROBE_ROWS, GLES30.GL_RGBA_INTEGER, GLES30.GL_UNSIGNED_INT, GLES30.GL_NEAREST)
            probeFbo = fbo(probeTex)
        }
        val bw = max(1, frame.bloomW)
        val bh = max(1, frame.bloomH)
        if (brightW != bw || brightH != bh) {
            deleteFboTex(brightFbo, brightTex)
            brightTex = texture(GLES30.GL_RGBA16F, bw, bh, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, GLES30.GL_LINEAR)
            brightFbo = fbo(brightTex)
            brightW = bw; brightH = bh
        }
        if (stageW != frame.renderW || stageH != frame.renderH) {
            deleteFboTex(preFbo, preTex)
            deleteFboTex(finalFbo, finalTex)
            preTex = texture(GLES30.GL_RGBA16F, frame.renderW, frame.renderH, GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, GLES30.GL_LINEAR)
            preFbo = fbo(preTex)
            finalTex = texture(GLES30.GL_RGBA8, frame.renderW, frame.renderH, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, GLES30.GL_LINEAR)
            finalFbo = fbo(finalTex)
            stageW = frame.renderW; stageH = frame.renderH
        }
    }

    private fun deleteFboTex(f: Int, t: Int) {
        if (f != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(f), 0)
        if (t != 0) GLES30.glDeleteTextures(1, intArrayOf(t), 0)
    }

    private fun deleteTargets() {
        deleteFboTex(statsFbo, statsTex); statsFbo = 0; statsTex = 0
        deleteFboTex(probeFbo, probeTex); probeFbo = 0; probeTex = 0
        deleteFboTex(brightFbo, brightTex); brightFbo = 0; brightTex = 0; brightW = 0; brightH = 0
        deleteFboTex(preFbo, preTex); preFbo = 0; preTex = 0
        deleteFboTex(finalFbo, finalTex); finalFbo = 0; finalTex = 0; stageW = 0; stageH = 0
    }

    // ------------------------------------------------------------------ stage capture

    /**
     * D3: the production brightpass program on the presented HDR texture (identical program, input and
     * threshold as the frame's bloom pass; its own output target because the bloom chain overwrites it).
     * D5: the production composite source truncated just before ACES. D6: the production composite
     * program into an RGBA8 target at the render resolution.
     */
    private fun renderStages(frame: Frame) {
        val quad = frame.quad
        frame.brightPass?.let { bp ->
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, brightFbo)
            GLES30.glViewport(0, 0, brightW, brightH)
            bp.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frame.presentationTexture)
            bp.setUniform1i("u_HdrTexture", 0)
            bp.setUniform1f("u_BloomThreshold", frame.bloomThreshold)
            quad.draw()
        }
        fun composite(program: ShaderProgram, target: Int) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target)
            GLES30.glViewport(0, 0, stageW, stageH)
            program.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frame.presentationTexture)
            program.setUniform1i("u_HdrTexture", 0)
            val bloomReady = frame.bloomEnabled && frame.bloomTexture != 0
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (bloomReady) frame.bloomTexture else 0)
            program.setUniform1i("u_BloomTexture", 1)
            program.setUniform1i("u_EnableBloom", if (bloomReady) 1 else 0)
            program.setUniform1f("u_Exposure", frame.exposure)
            program.setUniform1f("u_BloomIntensity", frame.bloomIntensity)
            program.setUniform2f("u_TexelSize", 1.0f / max(1, frame.renderW), 1.0f / max(1, frame.renderH))
            quad.draw()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        }
        preToneProgram?.let { composite(it, preFbo) }
        frame.composite?.let { composite(it, finalFbo) }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun drawView(frame: Frame) {
        if (frame.view == View.D6_FINAL) {
            frame.drawRealComposite()
            return
        }
        val program = viewProgram ?: return
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, frame.surfaceW, frame.surfaceH)
        program.use()
        var mode = 0
        var tex = 0
        var size = intArrayOf(frame.renderW, frame.renderH)
        when (frame.view) {
            View.D0_STATIC_HDR -> tex = frame.hdrTexture
            View.D1_RAY_RECORDS -> { mode = 1; size = intArrayOf(frame.rayW, frame.rayH) }
            View.D2_MATERIAL -> tex = frame.modulatedTexture
            View.D3_BRIGHTPASS -> tex = brightTex
            View.D4_BLOOM -> tex = frame.bloomTexture
            View.D5_PRE_TONEMAP -> tex = preTex
            View.D7_HIGHER_ORDER -> { mode = 2; tex = frame.hdrTexture }
            else -> Unit
        }
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (mode == 1) 0 else tex)
        program.setUniform1i("u_A", 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (mode == 1) frame.recordTexture0 else 0)
        program.setUniform1i("u_R", 1)
        program.setUniform1i("u_Mode", mode)
        program.setUniform1f("u_Scale", 1.0f)
        GLES30.glUniform2i(program.getUniformLocation("u_Size"), size[0], size[1])
        program.setUniform2f("u_Screen", frame.surfaceW.toFloat(), frame.surfaceH.toFloat())
        val markers = markerPoints(frame)
        GLES30.glUniform2fv(program.getUniformLocation("u_Markers"), markers.size / 2, markers, 0)
        program.setUniform1i("u_MarkerCount", markers.size / 2)
        frame.quad.draw()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    private fun markerPoints(frame: Frame): FloatArray {
        val out = ArrayList<Float>()
        for (p in ANIMATION_PROBES_ST) {
            val t = stToTexel(p[0], p[1], frame.renderW, frame.renderH)
            out.add((t[0] + 0.5f) / frame.renderW); out.add((t[1] + 0.5f) / frame.renderH)
        }
        for (p in lastRingPeaks.take(20)) {
            out.add((p[0] + 0.5f) / frame.renderW); out.add((p[1] + 0.5f) / frame.renderH)
        }
        return out.toFloatArray()
    }

    // ------------------------------------------------------------------ GPU reductions / probes

    private fun runStats(mode: Int, a: Int, b: Int, c: Int, r: Int, w: Int, h: Int): IntArray? {
        val program = statsProgram ?: return null
        if (w <= 0 || h <= 0) return null
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, statsFbo)
        GLES30.glViewport(0, 0, STATS_GRID, STATS_GRID)
        program.use()
        val units = intArrayOf(a, b, c)
        val names = arrayOf("u_A", "u_B", "u_C")
        for (i in 0 until 3) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, units[i])
            program.setUniform1i(names[i], i)
        }
        GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, r)
        program.setUniform1i("u_R", 3)
        program.setUniform1i("u_Mode", mode)
        GLES30.glUniform2i(program.getUniformLocation("u_Size"), w, h)
        GLES30.glUniform2i(program.getUniformLocation("u_Grid"), STATS_GRID, STATS_GRID)
        lastFrameQuad?.draw()
        statsBuffer.clear()
        GLES30.glReadPixels(0, 0, STATS_GRID, STATS_GRID, GLES30.GL_RGBA_INTEGER, GLES30.GL_UNSIGNED_INT, statsBuffer)
        for (i in 3 downTo 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        }
        val out = IntArray(STATS_GRID * STATS_GRID * 4)
        statsBuffer.position(0)
        statsBuffer.get(out)
        return out
    }

    /** Point probes (row 0) and, when [scan] is set, the 12 fixed radial scan lines. Raw uint bits. */
    private fun runProbe(kindRecords: Boolean, tex: Int, w: Int, h: Int, pointsTexel: FloatArray, scan: Boolean, frame: Frame): IntArray? {
        val program = probeProgram ?: return null
        if (tex == 0 || w <= 0 || h <= 0) return null
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, probeFbo)
        val rows = if (scan) PROBE_ROWS else 1
        GLES30.glViewport(0, 0, SCAN_SAMPLES, rows)
        program.use()
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (kindRecords) 0 else tex)
        program.setUniform1i("u_A", 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (kindRecords) tex else 0)
        program.setUniform1i("u_R", 1)
        program.setUniform1i("u_Kind", if (kindRecords) 1 else 0)
        GLES30.glUniform2i(program.getUniformLocation("u_Size"), w, h)
        val n = min(pointsTexel.size / 2, MAX_POINTS)
        program.setUniform1i("u_PointCount", n)
        if (n > 0) GLES30.glUniform2fv(program.getUniformLocation("u_Points"), n, pointsTexel, 0)
        program.setUniform2f("u_ScanCenterSt", RING_CENTER_ST[0], RING_CENTER_ST[1])
        program.setUniform1f("u_ScanR0", SCAN_R0)
        program.setUniform1f("u_ScanDr", SCAN_DR)
        program.setUniform2f("u_Res", frame.renderW.toFloat(), frame.renderH.toFloat())
        frame.quad.draw()
        probeBuffer.clear()
        GLES30.glReadPixels(0, 0, SCAN_SAMPLES, rows, GLES30.GL_RGBA_INTEGER, GLES30.GL_UNSIGNED_INT, probeBuffer)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        val out = IntArray(SCAN_SAMPLES * rows * 4)
        probeBuffer.position(0)
        probeBuffer.get(out)
        return out
    }

    private var lastFrameQuad: QuadGeometry? = null
    private var lastRingPeaks: List<IntArray> = emptyList()

    private fun f(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)
    private fun lum(r: Float, g: Float, b: Float) = 0.2126f * r + 0.7152f * g + 0.0722f * b

    /** Samples a float stage at render-resolution texel points (scaled to the stage size). */
    private fun probeStage(tex: Int, w: Int, h: Int, pointsRender: List<IntArray>, frame: Frame): List<FloatArray>? {
        if (tex == 0) return null
        val pts = FloatArray(pointsRender.size * 2)
        pointsRender.forEachIndexed { i, p ->
            pts[2 * i] = (p[0] + 0.5f) * w / frame.renderW
            pts[2 * i + 1] = (p[1] + 0.5f) * h / frame.renderH
        }
        val raw = runProbe(false, tex, w, h, pts, false, frame) ?: return null
        return pointsRender.indices.map { i -> FloatArray(4) { c -> f(raw[i * 4 + c]) } }
    }

    private fun probeRecords(tex: Int, frame: Frame, pointsRender: List<IntArray>): List<IntArray>? {
        if (tex == 0) return null
        val pts = FloatArray(pointsRender.size * 2)
        pointsRender.forEachIndexed { i, p ->
            pts[2 * i] = (p[0] + 0.5f) * frame.rayW / frame.renderW
            pts[2 * i + 1] = (p[1] + 0.5f) * frame.rayH / frame.renderH
        }
        val raw = runProbe(true, tex, frame.rayW, frame.rayH, pts, false, frame) ?: return null
        return pointsRender.indices.map { i -> IntArray(4) { c -> raw[i * 4 + c] } }
    }

    private fun animationProbeTexels(frame: Frame): List<IntArray> =
        ANIMATION_PROBES_ST.map { stToTexel(it[0], it[1], frame.renderW, frame.renderH) }

    private fun updateSeries(frame: Frame): Boolean {
        lastFrameQuad = frame.quad
        if (!frame.animationActive || frame.modulatedTexture == 0) {
            series = null
            return false
        }
        val s = series?.takeIf { it.amplitude == frame.amplitudePercent } ?: Series(frame.amplitudePercent, frame.nowNanos).also { series = it }
        val elapsed = frame.nowNanos - s.startNanos
        var captured = false
        for (k in 0 until 3) {
            if (s.material[k] == null && elapsed >= k * 1_000_000_000L) {
                val pts = animationProbeTexels(frame)
                val m = probeStage(frame.modulatedTexture, frame.renderW, frame.renderH, pts, frame) ?: return false
                val st = probeStage(frame.hdrTexture, frame.renderW, frame.renderH, pts, frame) ?: return false
                s.material[k] = m.flatMap { it.toList() }.toFloatArray()
                s.static[k] = st.flatMap { it.toList() }.toFloatArray()
                s.times[k] = frame.nowNanos
                captured = true
                if (k == 2) finishAmplitude(frame, s)
                break
            }
        }
        return captured
    }

    private fun finishAmplitude(frame: Frame, s: Series) {
        val diff = runStats(1, frame.modulatedTexture, frame.hdrTexture, 0, 0, frame.renderW, frame.renderH) ?: return
        var sum = 0.0; var changed = 0; var shadowChanged = 0; var disk = 0; var diskChanged = 0
        for (i in 0 until STATS_GRID * STATS_GRID) {
            sum += f(diff[i * 4])
            changed += diff[i * 4 + 2] and 0xFFFF; shadowChanged += diff[i * 4 + 2] ushr 16
            disk += diff[i * 4 + 3] and 0xFFFF; diskChanged += diff[i * 4 + 3] ushr 16
        }
        val n = frame.renderW.toDouble() * frame.renderH
        fun probeLum(arr: FloatArray, i: Int) = lum(arr[i * 4], arr[i * 4 + 1], arr[i * 4 + 2])
        var s01 = 0.0; var s12 = 0.0
        for (i in 0 until DISK_PROBES) {
            val d01 = probeLum(s.material[1]!!, i) - probeLum(s.material[0]!!, i)
            val d12 = probeLum(s.material[2]!!, i) - probeLum(s.material[1]!!, i)
            s01 += d01 * d01; s12 += d12 * d12
        }
        val sky = ANIMATION_PROBES_ST.size - 1
        val shadow = ANIMATION_PROBES_ST.size - 2
        ampResults[s.amplitude] = AmpResult(
            rmsVsStatic = sqrt(sum / n).toFloat(),
            changedPixels = changed, diskPixels = disk, diskChanged = diskChanged, shadowChanged = shadowChanged,
            probeRms01 = sqrt(s01 / DISK_PROBES).toFloat(), probeRms12 = sqrt(s12 / DISK_PROBES).toFloat(),
            skyDelta = kotlin.math.abs(probeLum(s.material[2]!!, sky) - probeLum(s.material[0]!!, sky)),
            shadowDelta = kotlin.math.abs(probeLum(s.material[2]!!, shadow) - probeLum(s.material[0]!!, shadow))
        )
    }

    // ------------------------------------------------------------------ report

    private fun buildReport(frame: Frame): String {
        val sb = StringBuilder()
        fun line(s: String) { sb.append(s).append('\n') }
        fun g(v: Float) = String.format(Locale.US, "%.4g", v)
        fun rgb(v: FloatArray?) = if (v == null) "n/a" else String.format(Locale.US, "(%.4g %.4g %.4g a%.4g)", v[0], v[1], v[2], v[3])

        line("DIAG ${frame.view.label} · records=${frame.precision.label} · t=${String.format(Locale.US, "%.1f", frame.nowNanos / 1e9)}s")
        line(capsText ?: "")
        line("render ${frame.renderW}x${frame.renderH} ray ${frame.rayW}x${frame.rayH} block ${frame.blockSize} bloom ${frame.bloomW}x${frame.bloomH} surface ${frame.surfaceW}x${frame.surfaceH}")
        line("camera ${frame.camera}")
        line("anim: ${frame.animationStatus} · action=${frame.gateAction} · geodesicPassesThisFrame=${frame.geodesicPasses} · rebuilds=${frame.rebuilds} · materialOutput=${if (frame.animationActive) "presented" else "not presented"}")
        line("FBOs: " + fboReport(frame.fbos))

        // Records (cache generation/storage).
        runStats(2, 0, 0, 0, frame.recordTexture0, frame.rayW, frame.rayH)?.let { r ->
            var nz = 0L; var hi = 0L; var trunc = 0L; var tier = 0L
            for (i in 0 until STATS_GRID * STATS_GRID) { nz += r[i * 4]; hi += r[i * 4 + 1]; trunc += r[i * 4 + 2]; tier += r[i * 4 + 3] }
            line("D1 records[0] ${frame.rayW}x${frame.rayH}: nonzero=$nz validCrossing(high16 set)=$hi truncatedSignature=$trunc M7>=5=$tier")
        } ?: line("D1 records: not allocated (animation not ready)")

        // Per-stage stats.
        fun stats(name: String, tex: Int, w: Int, h: Int) {
            if (tex == 0) { line("$name: n/a"); return }
            val s = runStats(0, tex, 0, 0, 0, w, h) ?: return
            var mn = Float.MAX_VALUE; var mx = 0f; var sum = 0.0; var nz = 0L
            for (i in 0 until STATS_GRID * STATS_GRID) {
                mn = min(mn, f(s[i * 4])); mx = max(mx, f(s[i * 4 + 1])); sum += f(s[i * 4 + 2]); nz += s[i * 4 + 3]
            }
            line("$name ${w}x$h: minL=${g(mn)} maxL=${g(mx)} meanL=${g((sum / (w.toDouble() * h)).toFloat())} nonzero=$nz")
        }
        stats("D0 static HDR", frame.hdrTexture, frame.renderW, frame.renderH)
        stats("D2 material", frame.modulatedTexture, frame.renderW, frame.renderH)
        stats("D3 brightpass", brightTex, brightW, brightH)
        stats("D4 bloom", frame.bloomTexture, frame.bloomW, frame.bloomH)
        stats("D5 pre-ACES", preTex, stageW, stageH)
        stats("D6 final", finalTex, stageW, stageH)
        runStats(3, frame.hdrTexture, 0, 0, 0, frame.renderW, frame.renderH)?.let { s ->
            var sum = 0.0; var mx = 0f; var cnt = 0L; var half = 0L
            for (i in 0 until STATS_GRID * STATS_GRID) { sum += f(s[i * 4 + 2]); mx = max(mx, f(s[i * 4 + 1])); cnt += s[i * 4 + 3] and 0xFFFF; half += s[i * 4 + 3] ushr 16 }
            line("D7 HO/total on D0: mean=${g(if (cnt > 0) (sum / cnt).toFloat() else 0f)} max=${g(mx)} px=$cnt px(HO>50%)=$half")
        }

        // Animation: material vs static now, and per-amplitude results (t=0,1,2 s after activation).
        if (frame.animationActive && frame.modulatedTexture != 0) {
            runStats(1, frame.modulatedTexture, frame.hdrTexture, 0, 0, frame.renderW, frame.renderH)?.let { d ->
                var sum = 0.0; var mx = 0f; var ch = 0L; var sh = 0L; var disk = 0L; var dch = 0L
                for (i in 0 until STATS_GRID * STATS_GRID) {
                    sum += f(d[i * 4]); mx = max(mx, f(d[i * 4 + 1]))
                    ch += d[i * 4 + 2] and 0xFFFF; sh += d[i * 4 + 2] ushr 16; disk += d[i * 4 + 3] and 0xFFFF; dch += d[i * 4 + 3] ushr 16
                }
                line("D2-D0 now: RMS(L)=${g(sqrt(sum / (frame.renderW.toDouble() * frame.renderH)).toFloat())} max|dL|=${g(mx)} changed=$ch diskPx=$disk diskChanged=$dch shadowChanged=$sh")
            }
        }
        line("amp | RMS(D2-D0) | changed | disk changed/disk | shadowChanged | probe RMS dL 0-1s | 1-2s | |dL| sky | |dL| shadow")
        for ((amp, r) in ampResults) {
            line("$amp% | ${g(r.rmsVsStatic)} | ${r.changedPixels} | ${r.diskChanged}/${r.diskPixels} | ${r.shadowChanged} | ${g(r.probeRms01)} | ${g(r.probeRms12)} | ${g(r.skyDelta)} | ${g(r.shadowDelta)}")
        }
        series?.let { s ->
            line("probes (render texel; static | material t0 | t1 | t2), amp ${s.amplitude}%:")
            val pts = animationProbeTexels(frame)
            for (i in pts.indices) {
                val name = if (i < DISK_PROBES) "P$i" else if (i == DISK_PROBES) "S(shadow)" else "K(sky)"
                fun lumAt(arr: FloatArray?) = if (arr == null) "-" else g(lum(arr[i * 4], arr[i * 4 + 1], arr[i * 4 + 2]))
                line(" $name ${pts[i][0]},${pts[i][1]}: ${lumAt(s.static[0])} | ${lumAt(s.material[0])} | ${lumAt(s.material[1])} | ${lumAt(s.material[2])}")
            }
        }

        ringReport(frame, ::line, ::g, ::rgb)
        blackReport(frame, ::line, ::rgb)

        line("passes (last armed frame):")
        passLog.values.forEach { line(" $it") }
        line("GPU ms (EXT_disjoint_timer_query): " + if (!hasTimer) "unavailable" else timerMs.entries.joinToString(" ") { "${it.key}=${g(it.value)}" })
        return sb.toString()
    }

    private fun ringReport(frame: Frame, line: (String) -> Unit, g: (Float) -> String, rgb: (FloatArray?) -> String) {
        val raw = runProbe(false, frame.hdrTexture, frame.renderW, frame.renderH, FloatArray(0), true, frame) ?: return
        val px = SCAN_DR * min(frame.renderW, frame.renderH) * 0.5f
        val peaks = ArrayList<IntArray>()
        val outers = ArrayList<IntArray>()
        val rows = ArrayList<String>()
        val hoByDir = FloatArray(SCAN_DIRECTIONS)
        for (d in 0 until SCAN_DIRECTIONS) {
            fun at(i: Int) = FloatArray(4) { c -> f(raw[((d + 1) * SCAN_SAMPLES + i) * 4 + c]) }
            var boundary = -1
            for (i in 0 until SCAN_SAMPLES) {
                val v = at(i)
                if (v[3] <= 0.5f && v[0] * v[0] + v[1] * v[1] + v[2] * v[2] <= 1e-7f) boundary = i
            }
            val angle = 15 + 30 * d
            if (boundary < 0 || boundary >= SCAN_SAMPLES - 2) { rows.add(" ${angle}deg: no shadow on scan line"); continue }
            var best = boundary + 1
            var bestHo = -1f
            for (i in boundary + 1 until min(SCAN_SAMPLES, boundary + 1 + PEAK_WINDOW)) {
                val ho = max(0f, at(i)[3] - 1f)
                if (ho > bestHo) { bestHo = ho; best = i }
            }
            val outer = min(SCAN_SAMPLES - 1, best + OUTER_OFFSET)
            val pk = at(best)
            val tl = lum(pk[0], pk[1], pk[2])
            hoByDir[d] = bestHo
            val peakTexel = scanTexel(d, best, frame)
            peaks.add(peakTexel); outers.add(scanTexel(d, outer, frame))
            rows.add(String.format(Locale.US, " %3ddeg boundary@%s peak@%d,%d dR=%.2fpx HO(a-1)=%s total=%s HO/total=%s",
                angle, scanTexel(d, boundary, frame).joinToString(","), peakTexel[0], peakTexel[1], (best - boundary) * px,
                g(bestHo), g(tl), g(if (tl > 1e-4f) bestHo / tl else 0f)))
        }
        lastRingPeaks = peaks
        line("RING scan on D0 (centre st ${RING_CENTER_ST[0]},${RING_CENTER_ST[1]}, 12 lines at 15+30k deg, GL angles, y up):")
        rows.forEach(line)
        val right = listOf(0, 11).map { hoByDir[it] }.average()
        val left = listOf(5, 6).map { hoByDir[it] }.average()
        line(String.format(Locale.US, " HO(a-1) right(15,345deg)=%.4g left(165,195deg)=%.4g right/left=%.3g", right, left, if (left > 0) right / left else 0.0))
        if (peaks.isEmpty()) return
        // Same physical pixels through every stage: peak and outer (+%d samples) luminance.
        val stages = listOf(
            "D0" to Triple(frame.hdrTexture, frame.renderW, frame.renderH),
            "D2" to Triple(frame.modulatedTexture, frame.renderW, frame.renderH),
            "D3" to Triple(brightTex, brightW, brightH),
            "D4" to Triple(frame.bloomTexture, frame.bloomW, frame.bloomH),
            "D5" to Triple(preTex, stageW, stageH),
            "D6" to Triple(finalTex, stageW, stageH)
        )
        line(" per stage L(peak)/L(outer) for each found line:")
        for ((name, t) in stages) {
            val pk = probeStage(t.first, t.second, t.third, peaks, frame)
            val ou = probeStage(t.first, t.second, t.third, outers, frame)
            if (pk == null || ou == null) { line("  $name: n/a"); continue }
            line("  $name: " + pk.indices.joinToString(" ") { i ->
                val a = lum(pk[i][0], pk[i][1], pk[i][2]); val b = lum(ou[i][0], ou[i][1], ou[i][2])
                "${g(a)}/${g(b)}"
            })
        }
    }

    private fun blackReport(frame: Frame, line: (String) -> Unit, rgb: (FloatArray?) -> String) {
        val s = runStats(4, finalTex, frame.hdrTexture, preTex, 0, stageW, stageH) ?: return
        var a = 0L; var b = 0L; var c = 0L; var d = 0L; var e = 0L; var ez = 0L
        val examples = ArrayList<IntArray>()
        for (i in 0 until STATS_GRID * STATS_GRID) {
            a += s[i * 4] and 0xFFFF; b += s[i * 4] ushr 16; c += s[i * 4 + 1] and 0xFFFF; d += s[i * 4 + 1] ushr 16
            e += s[i * 4 + 2] and 0xFFFF; ez += s[i * 4 + 2] ushr 16
            val pos = s[i * 4 + 3]
            if (pos != 0 && examples.size < 3) examples.add(intArrayOf(pos and 0xFFFF, pos ushr 16))
        }
        line("BLACK final px (max<3/255) by D0 provenance: A captured=$a B unresolved=$b C empty-space=$c D sampling-hole=$d E post-processing=$e (zeroed before ACES=$ez)")
        if (examples.isEmpty()) return
        val d0 = probeStage(frame.hdrTexture, frame.renderW, frame.renderH, examples, frame)
        val rec = probeRecords(frame.recordTexture0, frame, examples)
        val d3 = probeStage(brightTex, brightW, brightH, examples, frame)
        val d4 = probeStage(frame.bloomTexture, frame.bloomW, frame.bloomH, examples, frame)
        val d5 = probeStage(preTex, stageW, stageH, examples, frame)
        val d6 = probeStage(finalTex, stageW, stageH, examples, frame)
        examples.forEachIndexed { i, p ->
            val r = rec?.get(i)
            val recText = if (r == null) "records n/a" else String.format(
                Locale.US, "M7=%d crossing0=%s crossing1=%s",
                if ((r[3] and 0x8000) != 0) 9 else if ((r[1] and 0x8000) != 0) 5 else 1,
                if ((r[1] ushr 16) != 0) "yes" else "no", if ((r[3] ushr 16) != 0) "yes" else "no"
            )
            line(" px ${p[0]},${p[1]}: HDR${rgb(d0?.get(i))} $recText bright${rgb(d3?.get(i))} bloom${rgb(d4?.get(i))} preACES(post-sharpen)${rgb(d5?.get(i))} final${rgb(d6?.get(i))}")
        }
    }

    private fun scanTexel(direction: Int, sample: Int, frame: Frame): IntArray {
        val angle = Math.toRadians(15.0 + 30.0 * direction)
        val r = SCAN_R0 + SCAN_DR * sample
        return stToTexel(
            (RING_CENTER_ST[0] + kotlin.math.cos(angle) * r).toFloat(),
            (RING_CENTER_ST[1] + kotlin.math.sin(angle) * r).toFloat(),
            frame.renderW, frame.renderH
        )
    }

    private fun fboReport(fbos: List<Pair<String, Int>>): String {
        val v = IntArray(1)
        val parts = ArrayList<String>()
        for ((name, id) in fbos) {
            if (id == 0) { parts.add("$name=none"); continue }
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, id)
            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            val atts = ArrayList<String>()
            for (i in 0 until 4) {
                GLES30.glGetFramebufferAttachmentParameteriv(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 + i, GLES30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, v, 0)
                if (v[0] != GLES30.GL_TEXTURE) continue
                GLES30.glGetFramebufferAttachmentParameteriv(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 + i, GLES30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, v, 0)
                val tex = v[0]
                GLES30.glGetFramebufferAttachmentParameteriv(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 + i, GLES30.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE, v, 0)
                val type = when (v[0]) { GLES30.GL_FLOAT -> "F"; GLES30.GL_UNSIGNED_INT -> "UI"; GLES30.GL_INT -> "I"; GLES30.GL_UNSIGNED_NORMALIZED -> "UN"; else -> "0x" + Integer.toHexString(v[0]) }
                GLES30.glGetFramebufferAttachmentParameteriv(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0 + i, GLES30.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE, v, 0)
                atts.add("c$i:tex$tex/$type${v[0]}")
            }
            parts.add("$name(id$id)=${if (status == GLES30.GL_FRAMEBUFFER_COMPLETE) "COMPLETE" else "0x" + Integer.toHexString(status)}[${atts.joinToString(" ")}]")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return parts.joinToString("; ")
    }

    private fun collectCaps(): String {
        val v = IntArray(2)
        GLES30.glGetIntegerv(GLES30.GL_MAX_DRAW_BUFFERS, v, 0)
        GLES30.glGetIntegerv(GLES30.GL_MAX_COLOR_ATTACHMENTS, v, 1)
        val ext = GLES30.glGetString(GLES30.GL_EXTENSIONS) ?: ""
        hasTimer = ext.contains("GL_EXT_disjoint_timer_query")
        fun precision(type: Int, name: String): String {
            val range = IntArray(2)
            val prec = IntArray(1)
            GLES30.glGetShaderPrecisionFormat(GLES30.GL_FRAGMENT_SHADER, type, range, 0, prec, 0)
            return "$name[${range[0]},${range[1]};p${prec[0]}]"
        }
        return "GL ${GLES30.glGetString(GLES30.GL_VERSION)} | ${GLES30.glGetString(GLES30.GL_RENDERER)} | ${GLES30.glGetString(GLES30.GL_VENDOR)} | " +
            "GLSL ${GLES30.glGetString(GLES30.GL_SHADING_LANGUAGE_VERSION)}\n" +
            "MAX_DRAW_BUFFERS=${v[0]} MAX_COLOR_ATTACHMENTS=${v[1]} (animation cache uses 4) " +
            "ext color_buffer_float=${ext.contains("GL_EXT_color_buffer_float")} color_buffer_half_float=${ext.contains("GL_EXT_color_buffer_half_float")} timer=$hasTimer\n" +
            "fragment precision: " + listOf(
                precision(GLES30.GL_LOW_INT, "lowInt"), precision(GLES30.GL_MEDIUM_INT, "mediumInt"), precision(GLES30.GL_HIGH_INT, "highInt"),
                precision(GLES30.GL_LOW_FLOAT, "lowFloat"), precision(GLES30.GL_MEDIUM_FLOAT, "mediumFloat"), precision(GLES30.GL_HIGH_FLOAT, "highFloat")
            ).joinToString(" ")
    }

    private fun drawBufferName(v: Int): String = when (v) {
        GLES30.GL_NONE -> "NONE"
        GLES30.GL_BACK -> "BACK"
        in GLES30.GL_COLOR_ATTACHMENT0..(GLES30.GL_COLOR_ATTACHMENT0 + 15) -> "C${v - GLES30.GL_COLOR_ATTACHMENT0}"
        else -> "0x" + Integer.toHexString(v)
    }

    companion object {
        private const val TAG = "GargantuaDiag"
        private const val VERSION_LINE = "#version 300 es"
        private const val GL_TIME_ELAPSED_EXT = 0x88BF
        private const val GL_GPU_DISJOINT_EXT = 0x8FBB
        private const val MEASURE_INTERVAL_NANOS = 500_000_000L
        private const val LOG_INTERVAL_NANOS = 2_000_000_000L
        const val STATS_GRID = 32
        const val SCAN_SAMPLES = 256
        const val SCAN_DIRECTIONS = 12
        const val PROBE_ROWS = SCAN_DIRECTIONS + 1
        const val MAX_POINTS = 48
        const val DISK_PROBES = 10
        private const val PEAK_WINDOW = 32
        private const val OUTER_OFFSET = 6

        /** Photon-ring scan centre in shader st coordinates (Kerr shadow centre, default camera). */
        val RING_CENTER_ST = floatArrayOf(0.1215f, 0.0f)
        const val SCAN_R0 = 0.20f
        const val SCAN_DR = 0.00255f

        /**
         * Fixed animation probes in shader st coordinates, st = (2*fragCoord - res) / min(res), for the
         * default camera: P0..P9 disk, then the shadow probe S and the sky probe K.
         */
        val ANIMATION_PROBES_ST: List<FloatArray> = listOf(
            floatArrayOf(-0.90f, 0.02f), floatArrayOf(-0.70f, 0.02f), floatArrayOf(-0.50f, -0.06f),
            floatArrayOf(0.50f, -0.06f), floatArrayOf(0.70f, 0.02f), floatArrayOf(0.90f, 0.02f),
            floatArrayOf(-0.25f, -0.20f), floatArrayOf(0.00f, -0.22f), floatArrayOf(0.25f, -0.20f),
            floatArrayOf(0.12f, 0.52f),
            floatArrayOf(0.12f, 0.22f),
            floatArrayOf(-0.80f, 1.50f)
        )

        fun stToTexel(stX: Float, stY: Float, w: Int, h: Int): IntArray {
            val m = min(w, h).toFloat()
            val x = ((stX * m + w) * 0.5f).toInt().coerceIn(0, w - 1)
            val y = ((stY * m + h) * 0.5f).toInt().coerceIn(0, h - 1)
            return intArrayOf(x, y)
        }

        /** Record-precision variant of an animation program source (AS_SHIPPED returns it unchanged). */
        fun applyRecordPrecision(source: String, precision: RecordPrecision): String {
            val extra = when (precision) {
                RecordPrecision.AS_SHIPPED -> return source
                RecordPrecision.HIGHP_INT -> "precision highp int;"
                RecordPrecision.HIGHP_INT_SAMPLER -> "precision highp int;\nprecision highp sampler2D;"
            }
            require(source.startsWith(VERSION_LINE)) { "shader must begin with $VERSION_LINE" }
            return source.replaceFirst(VERSION_LINE, "$VERSION_LINE\n$extra")
        }

        /** The production composite source, returning the exposed colour just before the ACES curve. */
        fun preToneMappingCompositeSource(composite: String): String {
            val anchor = "    float inputLuma = dot(exposed,"
            require(composite.contains(anchor)) { "composite ACES anchor not found" }
            return composite.replaceFirst(anchor, "    fragColor = vec4(exposed, 1.0);\n    return;\n$anchor")
        }
    }
}
