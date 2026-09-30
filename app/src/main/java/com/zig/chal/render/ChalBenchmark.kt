package com.zig.chal.render

import com.zig.chal.config.ChalFeatureToggles
import com.zig.chal.config.ChalFeatures
import com.zig.chal.config.ChalPresetName

/**
 * Preset benchmark harness.
 *
 * Port of the reference engine's `src/performance/benchmark.ts`: it walks the four performance
 * presets for `TEST_DURATION_MS` each, records the per-preset FPS statistics, and recommends the
 * highest-fidelity preset that holds the frame-rate targets (60 FPS, then 35 FPS).
 *
 * The renderer drives it: `tick()` receives the live FPS once per rendered frame and the harness
 * asks for the next preset itself.
 */
class ChalBenchmark(private val nowMillis: () -> Double = { android.os.SystemClock.elapsedRealtime().toDouble() }) {

    /** `BenchmarkResult`. */
    data class BenchmarkResult(
        val presetName: ChalPresetName,
        val averageFPS: Double,
        val minFPS: Double,
        val maxFPS: Double,
        val averageFrameTimeMs: Double,
        val testDurationSeconds: Double
    )

    /** `BenchmarkReport.hardwareInfo`. */
    data class HardwareInfo(
        val isMobile: Boolean,
        val hasIntegratedGPU: Boolean,
        val devicePixelRatio: Double
    )

    /** `BenchmarkReport`. */
    data class BenchmarkReport(
        val results: List<BenchmarkResult>,
        val recommendedPreset: ChalPresetName,
        val hardwareInfo: HardwareInfo
    )

    /** `BenchmarkState`. */
    enum class State { IDLE, RUNNING, COMPLETED, CANCELLED }

    companion object {
        /** `TEST_DURATION_MS = 5000` -- 5 seconds per preset. */
        const val TEST_DURATION_MS: Double = 5000.0

        /** `PRESETS_TO_TEST`, in ascending quality order. */
        val PRESETS_TO_TEST: List<ChalPresetName> = listOf(
            ChalPresetName.MAXIMUM_PERFORMANCE,
            ChalPresetName.BALANCED,
            ChalPresetName.HIGH_QUALITY,
            ChalPresetName.ULTRA_QUALITY
        )
    }

    var state: State = State.IDLE
        private set

    val results = mutableListOf<BenchmarkResult>()

    private var currentPresetIndex = 0
    private var testStartTime = 0.0
    private var fpsSum = 0.0
    private var fpsCount = 0
    private var fpsMin = Double.MAX_VALUE
    private var fpsMax = 0.0
    private var restoreFeatures: ChalFeatureToggles? = null

    /** `start()`: snapshot the settings the run has to restore, then arm the first preset. */
    fun start(currentFeatures: ChalFeatureToggles) {
        restoreFeatures = currentFeatures
        results.clear()
        currentPresetIndex = 0
        state = State.RUNNING
        startPresetTest()
    }

    /** `cancel()`: stop early and hand back the toggles the caller must restore. */
    fun cancel(): ChalFeatureToggles? {
        state = State.CANCELLED
        val restore = restoreFeatures
        restoreFeatures = null
        return restore
    }

    /** The preset currently under test, or null when nothing is running. */
    fun currentPreset(): ChalPresetName? {
        if (state != State.RUNNING || currentPresetIndex >= PRESETS_TO_TEST.size) return null
        return PRESETS_TO_TEST[currentPresetIndex]
    }

    /** Progress (0..1) through the current preset's test window. */
    fun currentProgress(): Double {
        if (state != State.RUNNING) return 0.0
        val elapsed = nowMillis() - testStartTime
        return minOf(elapsed / TEST_DURATION_MS, 1.0)
    }

    /**
     * Feed one rendered frame's FPS and advance the state machine.
     *
     * @return the preset the renderer must apply next, or null when the currently applied preset
     *   should stay in place.
     */
    fun tick(currentFPS: Double, onComplete: (BenchmarkReport) -> Unit): ChalPresetName? {
        if (state != State.RUNNING) return null

        if (currentFPS > 0.0 && currentFPS.isFinite()) {
            fpsSum += currentFPS
            fpsCount++
            fpsMin = minOf(fpsMin, currentFPS)
            fpsMax = maxOf(fpsMax, currentFPS)
        }

        val elapsed = nowMillis() - testStartTime
        if (elapsed >= TEST_DURATION_MS) {
            finishPresetTest()
            currentPresetIndex++
            if (currentPresetIndex < PRESETS_TO_TEST.size) {
                startPresetTest()
                return PRESETS_TO_TEST[currentPresetIndex]
            }
            completeBenchmark(onComplete)
            return null
        }
        return null
    }

    /** Build the report: `completeBenchmark()` + `findRecommendedPreset()`. */
    private fun completeBenchmark(onComplete: (BenchmarkReport) -> Unit) {
        state = State.COMPLETED
        restoreFeatures = null
        onComplete(
            BenchmarkReport(
                results = results.toList(),
                recommendedPreset = findRecommendedPreset(),
                hardwareInfo = HardwareInfo(
                    isMobile = true, // Every Chal deployment target is a mobile GPU.
                    hasIntegratedGPU = false, // Would need an EGL/GLES query; the reference hardcodes false too.
                    devicePixelRatio = 1.0
                )
            )
        )
    }

    /**
     * Recommendation rule: the highest-quality preset that sustained 60+ FPS, falling back to the
     * highest that sustained 35+ FPS, falling back to the cheapest preset.
     */
    private fun findRecommendedPreset(): ChalPresetName {
        val qualityOrder = listOf(
            ChalPresetName.ULTRA_QUALITY,
            ChalPresetName.HIGH_QUALITY,
            ChalPresetName.BALANCED,
            ChalPresetName.MAXIMUM_PERFORMANCE
        )

        // Pass 1: High Fidelity (60+ FPS)
        for (presetName in qualityOrder) {
            val result = results.firstOrNull { it.presetName == presetName }
            if (result != null && result.averageFPS >= 60.0) return presetName
        }

        // Pass 2: Stable Standard (35+ FPS)
        for (presetName in qualityOrder) {
            val result = results.firstOrNull { it.presetName == presetName }
            if (result != null && result.averageFPS >= 35.0) return presetName
        }

        // Pass 3: whatever survived
        return results.minByOrNull { it.averageFPS }?.presetName ?: ChalPresetName.MAXIMUM_PERFORMANCE
    }

    private fun startPresetTest() {
        testStartTime = nowMillis()
        fpsSum = 0.0
        fpsCount = 0
        fpsMin = Double.MAX_VALUE
        fpsMax = 0.0
    }

    private fun finishPresetTest() {
        val presetName = PRESETS_TO_TEST.getOrNull(currentPresetIndex) ?: return
        val averageFPS = if (fpsCount > 0) fpsSum / fpsCount else 0.0
        val averageFrameTimeMs = if (averageFPS > 0.0) 1000.0 / averageFPS else 0.0

        results += BenchmarkResult(
            presetName = presetName,
            averageFPS = averageFPS,
            minFPS = if (fpsMin == Double.MAX_VALUE) 0.0 else fpsMin,
            maxFPS = fpsMax,
            averageFrameTimeMs = averageFrameTimeMs,
            testDurationSeconds = TEST_DURATION_MS / 1000.0
        )
    }

    /** The feature matrix for a preset, as the UI applies it. */
    fun featuresFor(preset: ChalPresetName): ChalFeatureToggles = ChalFeatures.getPreset(preset)
}
