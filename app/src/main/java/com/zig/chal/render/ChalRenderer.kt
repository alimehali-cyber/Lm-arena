package com.zig.chal.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.zig.chal.config.ChalFeatureToggles
import com.zig.chal.config.ChalFeatures
import com.zig.chal.config.ChalPresetName
import com.zig.chal.config.ChalPerformanceConfig
import com.zig.chal.config.ChalRayTracingQuality
import com.zig.chal.config.ChalSimulationParams
import com.zig.chal.physics.ChalKerrMetric
import com.zig.chal.physics.ChalPhysicsConstants
import com.zig.chal.shader.ChalGlShaders
import com.zig.chal.shader.ChalShaderManager
import com.zig.chal.shader.ChalShaderSource
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Kerr geodesic ray-marching renderer.
 *
 * Verbatim port of the reference engine's `src/rendering/webgl/renderer.ts` onto Android's GLES 3.0
 * (`GLSurfaceView.Renderer`). The uniform contract, the feature-driven shader recompilation, the
 * blue-noise dither, the bloom/TAA post chain, and the observer-distance parameter mapping are
 * identical to the reference implementation.
 *
 * IMPORTANT: This class is intentionally standalone. It shares nothing with any other renderer in
 * the application.
 */
class ChalRenderer : GLSurfaceView.Renderer {

    /** Live runtime snapshot consumed by the Compose HUD (all fields written on the GL thread). */
    data class ChalSnapshot(
        val params: ChalSimulationParams,
        val currentFps: Int,
        val frameTimeMs: Double,
        val quality: ChalRayTracingQuality,
        val budgetUsage: Double,
        val eventHorizonRadius: Double,
        val photonSphereRadius: Double,
        val iscoRadius: Double,
        val timeDilation: Double,
        val redshift: Double,
        val isCinematic: Boolean,
        val cinematicMode: ChalCamera.CinematicMode?,
        val targetFps: Int,
        val benchmarkState: ChalBenchmark.State = ChalBenchmark.State.IDLE,
        val benchmarkPreset: ChalPresetName? = null,
        val benchmarkProgress: Double = 0.0,
        val benchmarkResults: List<ChalBenchmark.BenchmarkResult> = emptyList(),
        val benchmarkRecommendation: ChalPresetName? = null
    )

    private val shaderManager = ChalShaderManager()
    private val benchmark = ChalBenchmark()
    private val camera = ChalCamera()
    private val monitor = ChalPerformanceMonitor()

    // Multi-threaded hand-off: the UI thread writes params, the GL thread renders them.
    @Volatile
    var params: ChalSimulationParams = ChalSimulationParams.DEFAULT_PARAMS
        private set

    @Volatile
    private var surfaceWidth = 1
    @Volatile
    private var surfaceHeight = 1

    // GL resources (GL thread only)
    private var quadBuffer = 0
    private var noiseTexture = 0
    private var blueNoiseTexture = 0
    private var bloom: ChalBloom? = null
    private var reprojection: ChalReprojection? = null

    private var program = 0
    private var compiledFeatures: ChalFeatureToggles? = null
    private var compiledHasPost = false
    private val uniformLocations = HashMap<String, Int>()

    private var time = 0.0
    private var lastFrameTime = 0.0
    private var lastMetricsUpdate = 0.0
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var cameraMoving = false
    private var cameraMoveTimeout = 0.0

    /**
     * Heavy EMA smoothing: 0.93/0.07, so a single spike needs ~15 consecutive bad frames to move
     * the average. This is what the PID resolution scaler reacts to (`smoothedDeltaTime`).
     */
    private var smoothedDeltaTime = 16.67

    /** `targetFrameTime`: the frame-budget gate value, lowered to the idle throttle when idle. */
    private var targetFrameTime = ChalPerformanceConfig.Scheduler.FRAME_BUDGET_MS

    /** `IdleDetector(PERFORMANCE_CONFIG.scheduler.idleTimeoutMs)`; stamped on every activity. */
    private var lastActivityTime = -1.0

    /** Set by UI-side parameter pushes so a paused-but-interactive frame still renders. */
    @Volatile
    private var paramsDirty = true

    /** True when EXT_color_buffer_float is available (`hasFloatFramebuffer`). */
    var hdrCapable = false
        private set

    /** Set when the GL objects could not be built; the UI surfaces this instead of a blank screen. */
    @Volatile
    var errorMessage: String? = null
        private set

    private val shadowCurve = FloatArray(128) // 64 points * 2

    @Volatile
    private var lastBenchmarkReport: ChalBenchmark.BenchmarkReport? = null

    private var lastSnapshot = ChalSnapshot(
        params = ChalSimulationParams.DEFAULT_PARAMS,
        currentFps = 0,
        frameTimeMs = 0.0,
        quality = ChalRayTracingQuality.HIGH,
        budgetUsage = 0.0,
        eventHorizonRadius = 0.0,
        photonSphereRadius = 0.0,
        iscoRadius = 0.0,
        timeDilation = 0.0,
        redshift = 0.0,
        isCinematic = false,
        cinematicMode = null,
        targetFps = 60
    )

    /** Snapshot for the UI thread. */
    fun snapshot(): ChalSnapshot = lastSnapshot

    // ---------------------------------------------------------------------------------------
    // UI-thread API
    // ---------------------------------------------------------------------------------------

    /** Replace the simulation parameters (feature changes force a shader recompilation). */
    fun updateParams(newParams: ChalSimulationParams) {
        if (newParams.renderScale != params.renderScale) monitor.seedRenderScale(newParams.renderScale)
        params = newParams
        paramsDirty = true
    }

    /** Mutating variant used by the camera loop (`setParams` equivalent). */
    private fun mutateParams(transform: ChalSimulationParams.() -> ChalSimulationParams) {
        params = params.transform()
    }

    fun resetCamera() {
        camera.reset { transform -> mutateParams(transform) }
    }

    fun startCinematic(mode: ChalCamera.CinematicMode, reducedMotion: Boolean) {
        camera.startCinematic(mode, params, now(), reducedMotion)
        if (mode == ChalCamera.CinematicMode.DIVE) {
            mutateParams { copy(autoSpin = 0.0) } // Disable artificial spin
        }
    }

    fun stopCinematic() = camera.stopCinematic()

    /** Camera input, called on the GL thread through `GLSurfaceView.queueEvent`. */
    fun onPointerDown(x: Double, y: Double) {
        lastActivityTime = now()
        camera.onPointerDown(x, y)
    }
    fun onPointerMove(x: Double, y: Double) {
        lastActivityTime = now()
        camera.onPointerMove(x, y)
    }
    fun onPointerUp() = camera.onPointerUp()
    fun onPan(dx: Double, dy: Double) {
        lastActivityTime = now()
        camera.onPan(dx, dy)
    }
    fun onPinchStart(distance: Double) {
        lastActivityTime = now()
        camera.onPinchStart(distance)
    }
    fun onPinch(distance: Double) = mutateParams { copy(zoom = camera.onPinch(distance, zoom)) }
    fun onScrollZoom(delta: Double) = mutateParams { copy(zoom = camera.onScrollZoom(delta, zoom)) }
    fun nudge(dTheta: Double, dPhi: Double) {
        lastActivityTime = now()
        camera.nudge(dTheta, dPhi)
    }

    /** True while a cinematic owns the camera (drives the ABORT SEQ affordance). */
    fun isCinematic(): Boolean = camera.isCinematic

    /**
     * Start the performance suite (`startBenchmark`).
     *
     * GL thread only: it mutates the simulation parameters as it walks the presets.
     */
    fun startBenchmark() {
        benchmark.start(params.features)
        mutateParams { copy(features = benchmark.featuresFor(ChalBenchmark.PRESETS_TO_TEST.first()),
                             performancePreset = ChalBenchmark.PRESETS_TO_TEST.first()) }
    }

    /** Abort the suite and restore the pre-benchmark feature matrix (`cancelBenchmark`). */
    fun cancelBenchmark() {
        val restore = benchmark.cancel()
        if (restore != null) {
            mutateParams { copy(features = restore, performancePreset = ChalFeatures.matchesPreset(restore)) }
        }
    }

    fun isBenchmarkRunning(): Boolean = benchmark.state == ChalBenchmark.State.RUNNING

    // ---------------------------------------------------------------------------------------
    // GLSurfaceView.Renderer
    // ---------------------------------------------------------------------------------------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // FIX: Set explicit clear color so TAA history starts from a defined state
        // instead of GPU-dependent garbage.
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Enable float textures for HDR. Devices without the extension fall back to LDR
        // rather than shipping a broken framebuffer.
        val extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS) ?: ""
        hdrCapable = extensions.contains("GL_EXT_color_buffer_float")
        if (!hdrCapable) {
            Log.w(ChalGlShaders.TAG, "EXT_color_buffer_float unavailable; HDR pipeline downgraded")
        }

        quadBuffer = ChalGlShaders.createQuadBuffer()
        noiseTexture = ChalGlShaders.createNoiseTexture(256)
        blueNoiseTexture = ChalGlShaders.createBlueNoiseTexture(256)

        // Post-processing chain (bloom owns the scene FBO; reprojection owns the TAA history)
        val bloomPipeline = ChalBloom(hdrCapable)
        val reprojectionPipeline = ChalReprojection(hdrCapable)

        val bloomOk = bloomPipeline.initialize(max(1, surfaceWidth), max(1, surfaceHeight))
        val reprojectionOk = reprojectionPipeline.initialize(max(1, surfaceWidth), max(1, surfaceHeight))

        if (bloomOk) bloom = bloomPipeline else {
            bloomPipeline.cleanup()
            bloom = null
        }
        if (reprojectionOk) reprojection = reprojectionPipeline else {
            reprojectionPipeline.cleanup()
            reprojection = null
        }
        if (!bloomOk || !reprojectionOk) {
            Log.w(ChalGlShaders.TAG, "Post-processing unavailable; rendering directly to the surface")
        }

        compileProgram(force = true)

        if (quadBuffer == 0 || noiseTexture == 0 || blueNoiseTexture == 0 || program == 0) {
            errorMessage = "Chal renderer failed to initialise (shader compile or texture creation)."
        } else {
            errorMessage = null
            // Prevent stutters when new execution paths (branches) are first taken.
            ChalGlShaders.warmupShader(program, quadBuffer, surfaceWidth, surfaceHeight)
        }

        lastFrameTime = now()
        lastMetricsUpdate = lastFrameTime
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = max(1, width)
        surfaceHeight = max(1, height)
        GLES30.glViewport(0, 0, surfaceWidth, surfaceHeight)
        bloom?.resize(surfaceWidth, surfaceHeight)
        reprojection?.ensureSize(surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val frameStart = now()
        val deltaTime = frameStart - lastFrameTime
        lastFrameTime = frameStart

        // Filter out huge spikes from app backgrounding
        val cappedDelta = min(deltaTime, 100.0)

        // Heavy EMA smoothing: a single spike needs ~15 consecutive bad frames to move the average.
        smoothedDeltaTime = smoothedDeltaTime * 0.93 + cappedDelta * 0.07
        val deltaTimeMs = smoothedDeltaTime

        // Idle throttling: 30 FPS once the user has been quiet for idleTimeoutMs.
        if (lastActivityTime < 0.0) lastActivityTime = frameStart
        targetFrameTime = if (frameStart - lastActivityTime > ChalPerformanceConfig.Scheduler.IDLE_TIMEOUT_MS) {
            1000.0 / ChalPerformanceConfig.Scheduler.IDLE_THROTTLE_FPS
        } else {
            ChalPerformanceConfig.Scheduler.FRAME_BUDGET_MS
        }

        // Frame-skip gate: uses the RAW delta, never the EMA, so a past spike cannot make us skip
        // a perfectly good frame.
        if (cappedDelta < targetFrameTime) return

        val dtSeconds = min(cappedDelta * 0.001, 0.1)

        // 1. Camera physics (the reference's requestAnimationFrame loop)
        camera.update(frameStart, dtSeconds, params) { transform -> mutateParams(transform) }

        // 2. Detect camera motion for TAA (mouse delta > 1e-4, debounced by 300 ms)
        val mouse = camera.mouseState()
        if (abs(mouse.x - lastMouseX) > 0.0001 || abs(mouse.y - lastMouseY) > 0.0001) {
            cameraMoving = true
            cameraMoveTimeout = frameStart + 300.0
            lastActivityTime = frameStart
        } else if (frameStart > cameraMoveTimeout) {
            cameraMoving = false
        }
        lastMouseX = mouse.x
        lastMouseY = mouse.y

        // 3. Metrics + adaptive resolution (driven by the smoothed delta time)
        val metrics = monitor.updateMetrics(deltaTimeMs)

        // Performance suite: walk the presets, sampling the live FPS of each.
        if (benchmark.state == ChalBenchmark.State.RUNNING) {
            val nextPreset = benchmark.tick(metrics.currentFPS.toDouble()) { report -> lastBenchmarkReport = report }
            if (nextPreset != null) {
                mutateParams {
                    copy(
                        features = benchmark.featuresFor(nextPreset),
                        performancePreset = nextPreset
                    )
                }
            }
        }

        // PAUSE LOGIC: a paused simulation still renders while the user is interacting with it.
        val isInteractionActive = cameraMoving || paramsDirty
        if (params.paused && !isInteractionActive) {
            if (frameStart - lastMetricsUpdate > 200.0) {
                lastMetricsUpdate = frameStart
                publishSnapshot(metrics)
            }
            return
        }
        paramsDirty = false

        // Throttle UI updates (5 Hz)
        if (frameStart - lastMetricsUpdate > 200.0) {
            lastMetricsUpdate = frameStart
            publishSnapshot(metrics)
        }

        val features = params.features

        // Recompile shader if feature toggles changed (cached if same)
        compileProgram(force = false)
        if (program == 0) {
            publishSnapshot(metrics)
            return
        }

        // 4. Virtual viewport scaling (PID-driven, seeded from params.renderScale)
        val renderScale = if (ChalPerformanceConfig.Resolution.ENABLE_DYNAMIC_SCALING) {
            min(metrics.renderResolution, ChalPerformanceConfig.Resolution.MOBILE_CAP)
        } else {
            params.renderScale
        }

        if (!params.paused) time += 0.01

        val bloomPipeline = bloom
        val reprojectionPipeline = reprojection

        // Update Bloom Config (feature-driven)
        bloomPipeline?.updateConfig(enabled = features.bloom)

        // 5. Offscreen for Bloom/TAA
        val targetFramebuffer = if (bloomPipeline != null) bloomPipeline.beginScene() else 0
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, targetFramebuffer)

        GLES30.glViewport(
            0,
            0,
            max(1, (surfaceWidth * renderScale).toInt()),
            max(1, (surfaceHeight * renderScale).toInt())
        )
        GLES30.glUseProgram(program)

        // Bind Textures
        // NOTE: Sampler uniforms MUST be set with integer calls (uniform1i), not float calls.
        if (noiseTexture != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTexture)
            set1i("u_noiseTex", 2)
        }
        if (blueNoiseTexture != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, blueNoiseTexture)
            set1i("u_blueNoiseTex", 3)
        }

        // Shadow guide uniforms: Schwarzschild fallback (b_crit = 3*sqrt(3)*M), exactly the
        // reference's fallback path when the Rust telemetry stream is unavailable.
        val shadowShiftMin = -(params.mass * 2.0) * 2.6
        val shadowShiftMax = params.mass * 2.0 * 2.6
        val bCrit = 3.0 * sqrt(3.0) * params.mass
        for (i in 0 until 64) {
            val phi = (i / 64.0) * PI * 2.0
            shadowCurve[i * 2] = (kotlin.math.cos(phi) * bCrit).toFloat()
            shadowCurve[i * 2 + 1] = (kotlin.math.sin(phi) * bCrit).toFloat()
        }
        set1f("u_shadowCount", 64.0)
        set2f("u_shadowShift", shadowShiftMin, shadowShiftMax)
        val curveLocation = loc("u_shadowCurve")
        if (curveLocation != -1) {
            GLES30.glUniform2fv(curveLocation, 64, shadowCurve, 0)
        }

        // CAMERA: u_camPos = (0,0,0) triggers the shader's spherical fallback camera, which uses
        // u_mouse + u_zoom so every interaction (drag, auto-spin, cinematic) is reflected.
        set3f("u_camPos", 0.0, 0.0, 0.0)
        set4f("u_camQuat", 0.0, 0.0, 0.0, 1.0)

        // Set Common Uniforms
        set2f("u_resolution", surfaceWidth * renderScale, surfaceHeight * renderScale)
        set1f("u_time", time)
        set1f("u_mass", params.mass)
        set1f("u_spin", params.spin * params.mass)
        set1f("u_zoom", params.zoom * 2.0) // Decoupled from mass so it grows visibly
        set1f("u_disk_size", params.diskSize)
        set1f("u_disk_scale_height", params.diskScaleHeight)
        set1i("u_maxRaySteps", ChalFeatures.getMaxRaySteps(features.rayTracingQuality))
        set1f("u_show_redshift", if (features.gravitationalRedshift) 1.0 else 0.0)
        set1f("u_show_kerr_shadow", if (features.kerrShadow) 1.0 else 0.0)
        set1f("u_lensing_strength", params.lensing)
        set1f("u_frame_dragging_strength", ChalPhysicsConstants.Gravity.FRAME_DRAGGING_STRENGTH)
        set2f("u_mouse", mouse.x, mouse.y)
        set1f("u_disk_density", params.diskDensity)

        // THERMODYNAMICS: T ~ M^-1/4 (Shakura-Sunyaev)
        // Small BH = Hotter (Blue), Large BH = Cooler (Red)
        val massFactor = params.mass.pow(-0.25)
        set1f("u_disk_temp", params.diskTemp * massFactor)
        set1f("u_debug", 0.0)

        if (quadBuffer != 0) {
            ChalGlShaders.setupPositionAttribute(program, "position", quadBuffer)
        }

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)

        // 6. Post-processing pipeline
        if (bloomPipeline != null && reprojectionPipeline != null) {
            val sceneTexture = bloomPipeline.sceneTextureId
            if (sceneTexture != 0) {
                // Resolve TAA (Temporal Anti-Aliasing)
                val resolved = reprojectionPipeline.resolve(sceneTexture, 0.75, cameraMoving, renderScale)

                if (resolved != 0) {
                    if (features.bloom) {
                        bloomPipeline.applyBloomToTexture(resolved, renderScale)
                    } else {
                        // If bloom disabled, just draw the TAA-resolved frame to screen
                        bloomPipeline.drawTextureToScreen(resolved, renderScale)
                    }
                } else if (features.bloom) {
                    bloomPipeline.applyBloom(sceneTexture, renderScale)
                } else {
                    bloomPipeline.drawTextureToScreen(sceneTexture, renderScale)
                }
            }
        } else if (bloomPipeline != null) {
            // Non-TAA path
            if (features.bloom) {
                bloomPipeline.applyBloom(bloomPipeline.sceneTextureId, renderScale)
            }
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    // ---------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------

    private fun compileProgram(force: Boolean) {
        val features = params.features
        val hasPost = bloom != null && reprojection != null

        if (!force && compiledFeatures == features && compiledHasPost == hasPost) return

        val variant = shaderManager.compileShaderVariant(
            ChalShaderSource.VERTEX_SHADER,
            ChalShaderSource.FRAGMENT_SHADER,
            features,
            hasPost
        )

        if (variant != null) {
            program = variant.program
            compiledFeatures = features
            compiledHasPost = hasPost
            uniformLocations.clear()
        } else if (force) {
            program = 0
            errorMessage = "Chal shader compilation failed."
        }
    }

    private fun publishSnapshot(metrics: ChalPerformanceMonitor.PerformanceMetrics) {
        // Physics state (the reference's `usePhysicsState`).
        val normalizedSpin = max(-1.0, min(1.0, params.spin))
        val eventHorizonRadius = ChalKerrMetric.calculateEventHorizon(params.mass, normalizedSpin)
        val photonSphereRadius = ChalKerrMetric.calculatePhotonSphere(params.mass, normalizedSpin)
        val iscoRadius = ChalKerrMetric.calculateIsco(params.mass, normalizedSpin, prograde = true)

        val absoluteZoom = params.zoom * 2.0 * params.mass
        val r = max(absoluteZoom, eventHorizonRadius * 1.01)
        val timeDilation = ChalKerrMetric.calculateTimeDilation(r, params.mass)
        val redshift = timeDilation - 1.0

        lastSnapshot = ChalSnapshot(
            params = params,
            currentFps = metrics.currentFPS,
            frameTimeMs = metrics.frameTimeMs,
            quality = metrics.quality,
            budgetUsage = monitor.getFrameTimeBudgetUsage(),
            eventHorizonRadius = eventHorizonRadius,
            photonSphereRadius = photonSphereRadius,
            iscoRadius = iscoRadius,
            timeDilation = timeDilation,
            redshift = redshift,
            isCinematic = camera.isCinematic,
            cinematicMode = camera.cinematicMode,
            targetFps = ChalPerformanceConfig.Scheduler.TARGET_FPS,
            benchmarkState = benchmark.state,
            benchmarkPreset = benchmark.currentPreset(),
            benchmarkProgress = benchmark.currentProgress(),
            benchmarkResults = benchmark.results.toList(),
            benchmarkRecommendation = lastBenchmarkReport?.recommendedPreset
        )
    }

    private fun now(): Double = android.os.SystemClock.elapsedRealtime().toDouble()

    private fun loc(name: String): Int = uniformLocations.getOrPut(name) {
        if (program == 0) -1 else GLES30.glGetUniformLocation(program, name)
    }

    private fun set1f(name: String, value: Double) {
        val l = loc(name); if (l != -1) GLES30.glUniform1f(l, value.toFloat())
    }

    private fun set1i(name: String, value: Int) {
        val l = loc(name); if (l != -1) GLES30.glUniform1i(l, value)
    }

    private fun set2f(name: String, x: Double, y: Double) {
        val l = loc(name); if (l != -1) GLES30.glUniform2f(l, x.toFloat(), y.toFloat())
    }

    private fun set3f(name: String, x: Double, y: Double, z: Double) {
        val l = loc(name); if (l != -1) GLES30.glUniform3f(l, x.toFloat(), y.toFloat(), z.toFloat())
    }

    private fun set4f(name: String, x: Double, y: Double, z: Double, w: Double) {
        val l = loc(name)
        if (l != -1) GLES30.glUniform4f(l, x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    }

    /** Release every GL object (called from the surface view's detach). */
    fun release() {
        shaderManager.clearCache()
        bloom?.cleanup()
        reprojection?.cleanup()
        bloom = null
        reprojection = null
        if (noiseTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(noiseTexture), 0)
        if (blueNoiseTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(blueNoiseTexture), 0)
        if (quadBuffer != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadBuffer), 0)
        noiseTexture = 0
        blueNoiseTexture = 0
        quadBuffer = 0
        program = 0
        compiledFeatures = null
        uniformLocations.clear()
    }
}
