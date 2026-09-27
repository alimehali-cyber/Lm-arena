package com.zig.gargantua.renderer

import com.zig.gargantua.renderer.GargantuaForensicData.DiffStat
import com.zig.gargantua.renderer.GargantuaForensicData.PassRecord
import com.zig.gargantua.renderer.GargantuaForensicData.RecordStat
import com.zig.gargantua.renderer.GargantuaForensicData.SamplerBinding
import com.zig.gargantua.renderer.GargantuaForensicData.Sample
import com.zig.gargantua.renderer.GargantuaForensicRun.WorkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The TEMPORARY one-tap forensic run: frame-driven schedule, overrides, termination, recovery and the
 * first-divergence analysis (pure logic; the GL measurements themselves only exist on a device).
 */
class GargantuaForensicRunTest {

    private val frameNanos = 33_000_000L

    /**
     * Minimal model of the renderer's reaction to the effective state: any change of the animation
     * configuration costs one retrace frame (2 geodesic passes); the material pass is presented after
     * [activationFrames] further frames when animation is requested (never when [animationWorks] is false).
     */
    private class FakeRenderer(val animationWorks: Boolean = true, val activationFrames: Int = 8) {
        private var lastKey: String? = null
        private var framesSinceChange = 0
        var rebuilds = 0

        fun frame(effective: GargantuaRenderState, now: Long): GargantuaForensicRun.FrameStatus {
            val requested = effective.enableAnimation && effective.animationAmplitudePercent > 0
            val key = "${effective.enableAnimation}/${effective.animationAmplitudePercent}/${effective.diagnosticRecordPrecision}/${effective.diagnosticRebuildGeneration}"
            val changed = key != lastKey
            if (changed) { lastKey = key; framesSinceChange = 0; if (requested) rebuilds++ } else framesSinceChange++
            val active = requested && animationWorks && framesSinceChange >= activationFrames
            return GargantuaForensicRun.FrameStatus(
                nowNanos = now, animationActive = active, animationRequested = requested,
                geodesicPasses = if (changed) 2 else 0, materialRan = active, bloomRan = true, compositeRan = true,
                rebuilds = rebuilds, gateAction = if (active) "MODULATE" else if (requested) "NONE" else "PLAIN",
                animationStatus = if (requested && !animationWorks) "ANIM FAILED: test" else "none"
            )
        }
    }

    private class Trace {
        val works = ArrayList<Pair<String, GargantuaForensicRun.Work>>()
        val configs = LinkedHashSet<String>()
        val workTimes = ArrayList<Long>()
    }

    private fun drive(run: GargantuaForensicRun, renderer: FakeRenderer, user: GargantuaRenderState, maxSeconds: Int, trace: Trace): Long {
        var now = 1_000_000_000L
        run.start(now)
        val end = now + maxSeconds * 1_000_000_000L
        while (now < end && run.state == GargantuaForensicRun.State.RUNNING) {
            val effective = GargantuaForensicRun.effectiveState(user, run.activePhase(), run.rebuildGeneration)
            run.activePhase()?.let { trace.configs.add("${it.id}:${effective.enableAnimation}/${effective.animationAmplitudePercent}/${effective.diagnosticRecordPrecision}/${effective.diagnosticRebuildGeneration}") }
            val phaseId = run.activePhase()?.id
            val work = run.onFrame(renderer.frame(effective, now))
            if (work.kind != WorkKind.NONE) {
                trace.works.add(phaseId!! to work)
                trace.workTimes.add(now)
                run.workDone(work, now)
            }
            now += frameNanos
        }
        return now
    }

    @Test
    fun withoutAnActivePhaseTheUserStateIsReturnedUnchanged() {
        val run = GargantuaForensicRun()
        val user = GargantuaRenderState(enableAnimation = true, animationAmplitudePercent = 25, diagnosticView = 7)
        assertNull(run.activePhase())
        assertTrue(user === GargantuaForensicRun.effectiveState(user, run.activePhase(), run.rebuildGeneration))
        assertEquals(GargantuaForensicRun.NO_WORK, run.onFrame(FakeRenderer().frame(user, 1L)))
        assertFalse(run.passCaptureArmed)
    }

    @Test
    fun oneRunExecutesTheWholeMatrixAndTerminates() {
        val run = GargantuaForensicRun()
        val user = GargantuaRenderState(enableAnimation = false, animationAmplitudePercent = 0, diagnosticView = 7)
        val trace = Trace()
        drive(run, FakeRenderer(), user, 300, trace)

        assertEquals(GargantuaForensicRun.State.COMPLETE, run.state)
        assertEquals(listOf("A", "B", "C", "D", "E", "RA", "RB", "RC"), run.records.map { it.phase.id })
        assertTrue(run.records.all { it.finished && it.failures.isEmpty() })
        // Every phase: t0 sample, three cache passes, ring, black pixels, pass graph.
        for (id in run.records.map { it.phase.id }) {
            val kinds = trace.works.filter { it.first == id }.map { it.second.toString() }
            for (expected in listOf("SAMPLE0", "CACHE0", "CACHE1", "CACHE2", "RING", "BLACK", "GRAPH")) {
                assertTrue("$id missing $expected: $kinds", expected in kinds)
            }
            val animated = run.records.first { it.phase.id == id }.phase.expectsAnimation
            assertEquals("$id t1/t2", animated, "SAMPLE1" in kinds && "SAMPLE2" in kinds)
        }
        // Amplitudes 0/15/40/80 and all three record precisions were actually applied to the renderer state,
        // and each REC phase used its own rebuild generation.
        val amps = trace.configs.map { it.substringAfter(':').split('/')[1].toInt() }.toSet()
        assertEquals(setOf(0, 15, 40, 80), amps)
        val rec = trace.configs.filter { it.startsWith("R") }.map { it.substringAfter(':').split('/') }
        assertEquals(setOf(0, 1, 2), rec.map { it[2].toInt() }.toSet())
        assertEquals(3, rec.map { it[3] }.toSet().size)
        // Wall-clock time series: t1 >= t0 + 1 s and t2 >= t0 + 2 s, measured from frame timestamps.
        for (id in listOf("C", "D", "E", "RA", "RB", "RC")) {
            val idx = trace.works.indices.filter { trace.works[it].first == id }
            fun t(name: String) = trace.workTimes[idx.first { trace.works[it].second.toString() == name }]
            assertTrue(t("SAMPLE1") - t("SAMPLE0") >= 1_000_000_000L)
            assertTrue(t("SAMPLE2") - t("SAMPLE0") >= 2_000_000_000L)
        }
        // Finished: overrides are gone and no more work is requested.
        assertNull(run.activePhase())
        val restored = GargantuaForensicRun.effectiveState(user, run.activePhase(), run.rebuildGeneration)
        assertTrue(restored === user)
        assertTrue(run.progressText(0L).startsWith("FORENSIC RUN COMPLETE\nPress COPY REPORT"))
    }

    @Test
    fun aPhaseWhoseAnimationNeverPresentsIsRecordedAsFailedAndTheRunContinues() {
        val run = GargantuaForensicRun()
        val trace = Trace()
        drive(run, FakeRenderer(animationWorks = false), GargantuaRenderState(diagnosticView = 7), 600, trace)
        assertEquals(GargantuaForensicRun.State.COMPLETE, run.state)
        for (id in listOf("C", "D", "E", "RA", "RB", "RC")) {
            val record = run.records.first { it.phase.id == id }
            assertTrue(id, record.failures.single().startsWith("PHASE NOT SETTLED"))
            val kinds = trace.works.filter { it.first == id }.map { it.second.toString() }
            assertTrue("SAMPLE0" in kinds && "SAMPLE1" !in kinds)
        }
        assertTrue(run.records.first { it.phase.id == "A" }.failures.isEmpty())
    }

    @Test
    fun aFrameGapInterruptsTheRunWithoutFurtherOverrides() {
        val run = GargantuaForensicRun()
        val renderer = FakeRenderer()
        val user = GargantuaRenderState(diagnosticView = 7)
        run.start(0L)
        run.onFrame(renderer.frame(GargantuaForensicRun.effectiveState(user, run.activePhase(), 0), frameNanos))
        assertTrue(run.progressText(frameNanos).contains("Phase 1/8"))
        assertTrue(run.progressText(frameNanos).contains("DO NOT TOUCH"))
        run.onFrame(renderer.frame(user, frameNanos + 6_000_000_000L))
        assertEquals(GargantuaForensicRun.State.INTERRUPTED, run.state)
        assertNull(run.activePhase())
        assertTrue(run.progressText(0L).startsWith("FORENSIC RUN INTERRUPTED"))
        run.cancel()
        assertEquals(GargantuaForensicRun.State.IDLE, run.state)
    }

    @Test
    fun feedbackIsDerivedFromSamplersAndEnabledDrawBuffersOnly() {
        val c0 = PassRecord.COLOR_ATTACHMENT0
        val sampled = PassRecord("p", 3, true, 9, intArrayOf(41, 0, 0, 0), intArrayOf(c0, 0, 0, 0), intArrayOf(0, 0, 8, 8),
            listOf(SamplerBinding("u_BuiltEmissionTexture", 1, 41)), listOf(intArrayOf(1, 41)))
        assertEquals(listOf(41), sampled.feedbackSampled)
        assertTrue(sampled.feedbackBoundOnly.isEmpty())
        val notDrawn = PassRecord("p", 3, true, 9, intArrayOf(41, 0, 0, 0), intArrayOf(0, 0, 0, 0), intArrayOf(0, 0, 8, 8),
            listOf(SamplerBinding("u_BuiltEmissionTexture", 1, 41)), listOf(intArrayOf(1, 41)))
        assertTrue(notDrawn.feedbackSampled.isEmpty())
        val boundOnly = PassRecord("p", 3, true, 9, intArrayOf(41, 0, 0, 0), intArrayOf(c0, 0, 0, 0), intArrayOf(0, 0, 8, 8),
            emptyList(), listOf(intArrayOf(5, 41)))
        assertEquals(listOf(41), boundOnly.feedbackBoundOnly)
    }

    private fun mainFile(relative: String): File {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            listOf("app/src/main/$relative", "src/main/$relative").map { File(dir, it) }.firstOrNull { it.isFile }?.let { return it }
            dir = dir.parentFile
        }
        throw AssertionError("main source not found: $relative")
    }

    @Test
    fun programDescriptionReportsTheDeclaredPrecisionsOfTheRealAnimationShader() {
        val material = ShaderSource.buildGeodesicVariant(
            mainFile("assets/shaders/gargantua_geodesic.frag").readText(),
            listOf("GARGANTUA_ANIMATION_SEMANTIC_CACHE", "GARGANTUA_ANIMATION_MATERIAL_PASS")
        )
        val shipped = GargantuaForensicData.ProgramInfo.describe(material)
        assertEquals("mediump (fragment default)", shipped.first)
        assertEquals("lowp (fragment default)", shipped.second)
        assertEquals("highp", shipped.third)
        val high = GargantuaForensicData.ProgramInfo.describe(
            GargantuaGpuDiagnostics.applyRecordPrecision(material, GargantuaGpuDiagnostics.RecordPrecision.HIGHP_INT_SAMPLER)
        )
        assertEquals("highp", high.first)
        assertEquals("highp", high.second)
    }

    private fun recordStat(valid: Long) = RecordStat(
        "r0", 11, "RGBA32UI", 10, 10, 50, if (valid > 0) 40 else 0, if (valid > 0) 10 else 50, 0, 0,
        valid, 0, 0, 3f, 20f, -3f, 3f, 0.5f, 1.5f, LongArray(4), LongArray(4)
    )

    private fun sample(index: Int, diskChangedVsStatic: Long, diskChangedTemporal: Long, finalChanged: Long) = Sample(
        index, index * 1000L, emptyList(), 0, 0f, 0f, 0, 0,
        DiffStat(0.1f, 1f, diskChangedVsStatic, 100, diskChangedVsStatic, 0),
        if (index == 0) null else DiffStat(0.1f, 1f, diskChangedTemporal, 100, diskChangedTemporal, 0),
        if (index == 0) null else DiffStat(0.1f, 1f, finalChanged, 100, finalChanged, 0),
        null, null, null
    )

    @Test
    fun analysisNamesTheFirstFailingStageFromMeasuredDataWithoutClaimingARootCause() {
        val run = GargantuaForensicRun()
        val phases = GargantuaForensicRun.PHASES
        fun add(id: String, valid: Long, animated: Boolean) {
            val record = GargantuaForensicRun.PhaseRecord(phases.first { it.id == id }, run.records.size)
            record.animationActiveAtCapture = true
            record.cache.add(recordStat(valid))
            val changed = if (animated) 50L else 0L
            (0..2).forEach { record.samples.add(sample(it, changed, changed, changed)) }
            run.records.add(record)
        }
        add("D", 0, false)
        add("RA", 0, false)
        add("RB", 250, true)
        add("RC", 240, true)
        val analysis = GargantuaForensicReport.analyse(run)
        assertTrue(analysis.animationStage, analysis.animationStage.startsWith("CACHE GENERATION"))
        assertEquals(true, analysis.cacheDivergence)
        assertEquals(true, analysis.materialDivergence)
        assertEquals(true, analysis.compositeDivergence)
        assertFalse(analysis.feedback)

        val report = GargantuaForensicReport.build(run, "GL test\nMAX_DRAW_BUFFERS=8", "config", 0L)
        for (section in listOf("DEVICE", "GL LIMITS", "TEST CONFIGURATION", "PHASE RESULTS", "ANIMATION A/B/C RESULTS", "CACHE CONTENT",
            "FRAMEBUFFER PROVENANCE", "FEEDBACK HAZARDS", "RING RESULTS", "BLACK PIXEL RESULTS", "PERFORMANCE", "FIRST-DIVERGENCE ANALYSIS", "FINAL CONCLUSION")) {
            assertTrue(section, report.contains("== $section =="))
        }
        assertTrue(report.contains("ROOT CAUSE NOT YET PROVEN"))
        assertTrue(report.contains("FIRST FAILING STAGE (animation) = CACHE GENERATION"))
    }

    @Test
    fun analysisSeparatesMaterialAndDownstreamFailures() {
        val run = GargantuaForensicRun()
        val record = GargantuaForensicRun.PhaseRecord(GargantuaForensicRun.PHASES.first { it.id == "D" }, 0)
        record.animationActiveAtCapture = true
        record.cache.add(recordStat(100))
        (0..2).forEach { record.samples.add(sample(it, 30, 30, 0)) }
        run.records.add(record)
        assertTrue(GargantuaForensicReport.analyse(run).animationStage.startsWith("DOWNSTREAM OF MATERIAL"))
        record.samples.clear()
        (0..2).forEach { record.samples.add(sample(it, 0, 0, 0)) }
        assertTrue(GargantuaForensicReport.analyse(run).animationStage.startsWith("MATERIAL PASS"))
    }
}
