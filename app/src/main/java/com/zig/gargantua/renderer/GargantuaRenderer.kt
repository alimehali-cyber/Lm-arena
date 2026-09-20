package com.zig.gargantua.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

private fun effectiveCoarseSamplingBlockSize(state: GargantuaRenderState): Int =
    GargantuaCoarseSampling.sanitizeBlockSize(state.debugCoarseSamplingBlockSize)

/**
 * OpenGL ES 3.x Renderer for Gargantua.
 * Orchestrates full-screen relativistic photon geodesic tracing through Kerr-Schild spacetime,
 * offscreen HDR floating-point FBO rendering, restrained bloom extraction and separable blur,
 * ACES filmic tone mapping, stationary frame caching, and thread-safe telemetry emission.
 */
class GargantuaRenderer(
    private val context: Context,
    val stateHolder: RenderStateHolder = RenderStateHolder()
) : GLSurfaceView.Renderer {

    private var geodesicProgram: ShaderProgram? = null
    private var workloadGeodesicProgram: ShaderProgram? = null
    private var testProgram: ShaderProgram? = null
    private var blitProgram: ShaderProgram? = null
    private var brightPassProgram: ShaderProgram? = null
    private var blurProgram: ShaderProgram? = null
    private var compositeProgram: ShaderProgram? = null
    private var reduceProgram: ShaderProgram? = null
    private var quadGeometry: QuadGeometry? = null

    // Primary HDR Framebuffer Object (FBO) resources (GL_RGBA16F with GL_RGBA8 fallback)
    private var hdrFboId = 0
    private var hdrTextureId = 0
    private var hdrWidth = 0
    private var hdrHeight = 0
    private var isHdrSupported = false

    // Downsampled Bloom Ping-Pong FBO resources
    private var bloomFboA = 0
    private var bloomTexA = 0
    private var bloomFboB = 0
    private var bloomTexB = 0
    private var bloomWidth = 0
    private var bloomHeight = 0

    // Temporary coarse ray target. The unchanged full internal HDR target is still used for
    // upsampling, bloom, and composite; only the expensive geodesic pass uses this smaller grid.
    private var coarseRayFboId = 0
    private var coarseRayTextureId = 0
    private var coarseRayWidth = 0
    private var coarseRayHeight = 0
    private var coarseRayTargetAvailable = false

    // Debug-only workload instrumentation FBOs. These are never allocated or attached during
    // normal production rendering. A second and third color attachment carry per-pixel counters;
    // reduction passes collapse them to one texel before the debug-only readback.
    private var workloadFboId = 0
    private var workloadTierTextureId = 0
    private var workloadCostTextureId = 0
    private var workloadReduceFboA = 0
    private var workloadReduceTextureA = 0
    private var workloadReduceFboB = 0
    private var workloadReduceTextureB = 0
    private var workloadWidth = 0
    private var workloadHeight = 0
    private var workloadRayTextureId = 0
    private var workloadTelemetrySupported = false
    private var workloadProgramAttempted = false
    private var workloadProgramFailureStatus: String? = null
    private var workloadDiagnosticStatus = "TEL OFF"
    private var workloadReadbackValid = true

    // Dirty/invalidation scheduling. Ray-scene changes, bloom extraction changes, and composite
    // changes are intentionally tracked separately so exposure/bloom-intensity changes do not
    // retrace photons.
    private var lastSceneSignature: SceneSignature? = null
    private var lastRaySceneSignature: RaySceneSignature? = null
    private var lastBloomSignature: BloomSignature? = null
    private var lastCompositeSignature: CompositeSignature? = null
    private var sceneDirty = true
    @Volatile private var presentationInvalidationPending = true
    @Volatile private var renderReadyListener: (() -> Unit)? = null
    private val renderReadyGate = GargantuaRenderReadyGate {
        renderReadyListener?.invoke()
    }

    private var lastWorkloadStats = GargantuaWorkloadStats.unavailable()
    private var lastPassTimings = GargantuaPassTimings()
    private var lastIscoRadius = 2.91f

    private var startTimeNanos: Long = 0L
    private var fpsAccumulatorTimeNanos: Long = 0L
    private var fpsLastSubmittedNanos: Long = 0L
    private var fpsFrames: Int = 0

    internal data class SceneSignature(
        val width: Int,
        val height: Int,
        val renderScale: Float,
        val mass: Float,
        val spin: Float,
        val camDist: Float,
        val camInclinationDeg: Float,
        val camAzimuthDeg: Float,
        val camTargetX: Float,
        val camTargetY: Float,
        val camTargetZ: Float,
        val maxSteps: Int,
        val enableDisk: Boolean,
        val diskOuterRadius: Float,
        val enableObject: Boolean,
        val objectRadius: Float,
        val objectOrbitRadius: Float,
        val objectPhi0: Float,
        val objectZ: Float,
        val useGeodesicShader: Boolean,
        val exposure: Float,
        val enableBloom: Boolean,
        val bloomIntensity: Float,
        val bloomThreshold: Float
    ) {
        companion object {
            fun fromState(state: GargantuaRenderState, w: Int, h: Int): SceneSignature {
                return SceneSignature(
                    width = w,
                    height = h,
                    renderScale = state.renderScale,
                    mass = state.mass,
                    spin = state.spin,
                    camDist = state.camDist,
                    camInclinationDeg = state.camInclinationDeg,
                    camAzimuthDeg = state.camAzimuthDeg,
                    camTargetX = state.camTargetX,
                    camTargetY = state.camTargetY,
                    camTargetZ = state.camTargetZ,
                    maxSteps = state.maxSteps,
                    enableDisk = state.enableDisk,
                    diskOuterRadius = state.diskOuterRadius,
                    enableObject = state.enableObject,
                    objectRadius = state.objectRadius,
                    objectOrbitRadius = state.objectOrbitRadius,
                    objectPhi0 = state.objectPhi0,
                    objectZ = state.objectZ,
                    useGeodesicShader = state.useGeodesicShader,
                    exposure = state.exposure,
                    enableBloom = state.enableBloom,
                    bloomIntensity = state.bloomIntensity,
                    bloomThreshold = state.bloomThreshold
                )
            }
        }
    }

    internal data class RaySceneSignature(
        val width: Int,
        val height: Int,
        val renderScale: Float,
        val mass: Float,
        val spin: Float,
        val camDist: Float,
        val camInclinationDeg: Float,
        val camAzimuthDeg: Float,
        val camTargetX: Float,
        val camTargetY: Float,
        val camTargetZ: Float,
        val maxSteps: Int,
        val enableDisk: Boolean,
        val diskOuterRadius: Float,
        val enableObject: Boolean,
        val objectRadius: Float,
        val objectOrbitRadius: Float,
        val objectPhi0: Float,
        val objectZ: Float,
        val useGeodesicShader: Boolean,
        val debugCoarseSamplingBlockSize: Int,
        val enableWorkloadTelemetry: Boolean
    ) {
        companion object {
            fun fromState(state: GargantuaRenderState, w: Int, h: Int): RaySceneSignature =
                RaySceneSignature(
                    width = w,
                    height = h,
                    renderScale = state.renderScale,
                    mass = state.mass,
                    spin = state.spin,
                    camDist = state.camDist,
                    camInclinationDeg = state.camInclinationDeg,
                    camAzimuthDeg = state.camAzimuthDeg,
                    camTargetX = state.camTargetX,
                    camTargetY = state.camTargetY,
                    camTargetZ = state.camTargetZ,
                    maxSteps = state.maxSteps,
                    enableDisk = state.enableDisk,
                    diskOuterRadius = state.diskOuterRadius,
                    enableObject = state.enableObject,
                    objectRadius = state.objectRadius,
                    objectOrbitRadius = state.objectOrbitRadius,
                    objectPhi0 = state.objectPhi0,
                    objectZ = state.objectZ,
                    useGeodesicShader = state.useGeodesicShader,
                    debugCoarseSamplingBlockSize = effectiveCoarseSamplingBlockSize(state),
                    enableWorkloadTelemetry = state.enableWorkloadTelemetry
                )
        }
    }

    private data class BloomSignature(
        val enableBloom: Boolean,
        val bloomThreshold: Float
    )

    private data class CompositeSignature(
        val exposure: Float,
        val enableBloom: Boolean,
        val bloomIntensity: Float,
        val isPaused: Boolean
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val glVersion = GLES30.glGetString(GLES30.GL_VERSION) ?: "Unknown"
        val glRenderer = GLES30.glGetString(GLES30.GL_RENDERER) ?: "Unknown"
        val glVendor = GLES30.glGetString(GLES30.GL_VENDOR) ?: "Unknown"
        Log.i(TAG, "Gargantua GLES surface created: Version=$glVersion, Renderer=$glRenderer, Vendor=$glVendor")

        // EGL context recreation invalidates all prior object names; do not let stationary-cache
        // dimensions accidentally skip reallocation in the new context. The ready gate also
        // requires a fresh post-resource request for this context generation.
        renderReadyGate.reset()
        resetGpuResourceHandlesForNewContext()

        val vertSource = ShaderSource.loadVertexShader(context)

        // 1. Compile Phase M4/M5/M6 relativistic photon geodesic shader
        var isGeodesicReady = false
        geodesicProgram?.release()
        geodesicProgram = null
        try {
            val geodesicFragSource = ShaderSource.loadGeodesicFragmentShader(context)
            geodesicProgram = ShaderProgram.create(vertSource, geodesicFragSource)
            isGeodesicReady = (geodesicProgram != null)
            if (isGeodesicReady) {
                Log.i(TAG, "Gargantua relativistic photon geodesic shader compiled successfully.")
            } else {
                Log.w(TAG, "Gargantua geodesic shader failed to link; preparing fallback.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load geodesic shader asset; falling back to test shader", e)
        }

        // The workload variant of the canonical geodesic shader is compiled lazily only after
        // the temporary on-device diagnostic is explicitly enabled. The normal render path does
        // not pay the shader/FBO setup cost and continues to use the production program directly.
        workloadGeodesicProgram?.release()
        workloadGeodesicProgram = null
        reduceProgram?.release()
        reduceProgram = null
        workloadProgramAttempted = false
        workloadProgramFailureStatus = null
        workloadDiagnosticStatus = "TEL OFF"
        workloadReadbackValid = true

        // 2. Compile M1 baseline test shader as guaranteed fallback
        val testFragSource = ShaderSource.loadFragmentShader(context)
        testProgram?.release()
        testProgram = ShaderProgram.create(vertSource, testFragSource)

        // 3. Compile M6 cinematic presentation shaders (bright-pass, blur, ACES composite)
        try {
            val brightPassSource = ShaderSource.loadBrightPassFragmentShader(context)
            brightPassProgram?.release()
            brightPassProgram = ShaderProgram.create(vertSource, brightPassSource)

            val blurSource = ShaderSource.loadBlurFragmentShader(context)
            blurProgram?.release()
            blurProgram = ShaderProgram.create(vertSource, blurSource)

            val compositeSource = ShaderSource.loadCompositeFragmentShader(context)
            compositeProgram?.release()
            compositeProgram = ShaderProgram.create(vertSource, compositeSource)
        } catch (e: Exception) {
            Log.w(TAG, "Could not compile bloom/composite shaders; will use direct blit", e)
        }

        // 4. Compile fallback blit fragment shader
        try {
            val blitFragSource = ShaderSource.loadBlitFragmentShader(context)
            blitProgram?.release()
            blitProgram = ShaderProgram.create(vertSource, blitFragSource)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load blit shader", e)
        }

        if (!isGeodesicReady && testProgram == null) {
            val errorMsg = "Failed to compile any Gargantua shader program."
            Log.e(TAG, errorMsg)
            stateHolder.updateTelemetry {
                it.copy(
                    glesVersion = glVersion,
                    glRenderer = glRenderer,
                    isInitialized = false,
                    errorMessage = errorMsg
                )
            }
            return
        }

        // Initialize fullscreen geometry
        quadGeometry?.release()
        quadGeometry = QuadGeometry().apply { init() }

        // Clear color (pure black for black hole horizon)
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Force one initial scene render. Subsequent frames are requested only by state changes.
        lastSceneSignature = null
        lastRaySceneSignature = null
        lastBloomSignature = null
        lastCompositeSignature = null
        sceneDirty = true
        presentationInvalidationPending = true
        lastWorkloadStats = GargantuaWorkloadStats.unavailable()
        lastPassTimings = GargantuaPassTimings()

        // Initialize baseline timers
        val now = System.nanoTime()
        startTimeNanos = now
        fpsAccumulatorTimeNanos = now
        fpsLastSubmittedNanos = 0L
        fpsFrames = 0

        stateHolder.updateTelemetry {
            it.copy(
                fps = 0f,
                glesVersion = glVersion,
                glRenderer = glRenderer,
                isInitialized = true,
                errorMessage = null,
                isGeodesicActive = isGeodesicReady
            )
        }
        // This is the explicit post-resource lifecycle handshake. If the size was already known,
        // it requests the first presentation for this new EGL context; otherwise onSurfaceChanged
        // completes the gate after the non-empty size arrives.
        val currentViewport = stateHolder.getState()
        renderReadyGate.markSurfaceSize(currentViewport.viewportWidth, currentViewport.viewportHeight)
        renderReadyGate.markResourcesReady()
    }

    /**
     * Compiles the existing workload/reduction programs only after the temporary diagnostic is
     * enabled. This keeps the normal release render path identical until the temporary diagnostic
     * control is used on the physical device.
     */
    private fun ensureWorkloadPrograms(): Boolean {
        if (workloadGeodesicProgram != null && reduceProgram != null) {
            workloadDiagnosticStatus = "TEL ON · WORKLOAD READY"
            return true
        }
        if (workloadProgramAttempted) {
            workloadDiagnosticStatus = workloadProgramFailureStatus ?: "TEL ON · WORKLOAD NOT READY"
            return false
        }
        workloadProgramAttempted = true
        workloadProgramFailureStatus = null
        workloadDiagnosticStatus = "TEL ON · WORKLOAD COMPILING"

        var workloadSourceLength = 0
        val glVersion = GLES30.glGetString(GLES30.GL_VERSION) ?: "UNKNOWN"
        val glslVersion = GLES30.glGetString(GLES30.GL_SHADING_LANGUAGE_VERSION) ?: "UNKNOWN"

        return try {
            val vertexSource = ShaderSource.loadVertexShader(context)
            val workloadSource = ShaderSource.loadWorkloadTelemetryGeodesicFragmentShader(context)
            workloadSourceLength = workloadSource.length
            var workloadFailure: ShaderProgram.CreationFailure? = null
            val workload = ShaderProgram.create(
                vertexSource,
                workloadSource,
                onFailure = { workloadFailure = it }
            )
            var reduceFailure: ShaderProgram.CreationFailure? = null
            val reduce = ShaderProgram.create(
                vertexSource,
                ShaderSource.loadReduceFragmentShader(context),
                onFailure = { reduceFailure = it }
            )
            if (workload == null || reduce == null) {
                workload?.release()
                reduce?.release()
                workloadGeodesicProgram = null
                reduceProgram = null
                val failure = if (workload == null) workloadFailure else reduceFailure
                workloadProgramFailureStatus = workloadProgramFailureStatus(
                    failure = failure,
                    sourceLength = workloadSourceLength,
                    glVersion = glVersion,
                    glslVersion = glslVersion
                )
                workloadDiagnosticStatus = workloadProgramFailureStatus!!
                false
            } else {
                workloadGeodesicProgram = workload
                reduceProgram = reduce
                workloadProgramFailureStatus = null
                workloadDiagnosticStatus = "TEL ON · WORKLOAD READY"
                true
            }
        } catch (e: Exception) {
            workloadGeodesicProgram?.release()
            workloadGeodesicProgram = null
            reduceProgram?.release()
            reduceProgram = null
            workloadProgramFailureStatus =
                "TEL ON · WORKLOAD INIT EXCEPTION · ${compactWorkloadDetail(e.message ?: e.javaClass.simpleName)}" +
                    " · SRC_LEN=$workloadSourceLength · GL_VERSION=${compactWorkloadDetail(glVersion)}" +
                    " · GLSL_VERSION=${compactWorkloadDetail(glslVersion)}"
            workloadDiagnosticStatus = workloadProgramFailureStatus!!
            false
        }
    }

    private fun workloadProgramFailureStatus(
        failure: ShaderProgram.CreationFailure?,
        sourceLength: Int,
        glVersion: String,
        glslVersion: String
    ): String {
        val label = failure?.label ?: "UNKNOWN"
        val log = compactWorkloadDetail(failure?.log ?: "NO_INFO_LOG").take(200)
        return "TEL ON · WORKLOAD PROGRAM CREATE FAILED · $label · $log" +
            " · SRC_LEN=$sourceLength · GL_VERSION=${compactWorkloadDetail(glVersion)}" +
            " · GLSL_VERSION=${compactWorkloadDetail(glslVersion)}"
    }

    private fun compactWorkloadDetail(value: String): String =
        value.replace(Regex("\\s+"), " ").trim().ifEmpty { "EMPTY" }

    /** Releases only the temporary diagnostic resources when it is toggled off. */
    private fun releaseWorkloadDiagnosticResources() {
        deleteWorkloadFbos()
        workloadGeodesicProgram?.release()
        workloadGeodesicProgram = null
        reduceProgram?.release()
        reduceProgram = null
        workloadProgramAttempted = false
        workloadProgramFailureStatus = null
        workloadDiagnosticStatus = "TEL OFF"
        workloadReadbackValid = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        stateHolder.updateState {
            it.copy(viewportWidth = width, viewportHeight = height)
        }
        lastSceneSignature = null
        lastRaySceneSignature = null
        lastBloomSignature = null
        lastCompositeSignature = null
        sceneDirty = true
        presentationInvalidationPending = true
        // The state listener covers a changed size. The ready gate additionally covers an
        // unchanged-size callback and guarantees the request occurs after resources are ready.
        renderReadyGate.markSurfaceSize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val frameStartNanos = System.nanoTime()
        val state = stateHolder.getState()
        if (state.isPaused) {
            if (state.enableWorkloadTelemetry) {
                workloadDiagnosticStatus = "TEL ON · GL PAUSED"
                stateHolder.updateTelemetry { it.copy(adaptiveWorkload = workloadDiagnosticStatus) }
            }
            // Resume must re-present the cached HDR image even if the camera state is unchanged.
            lastCompositeSignature = null
            return
        }

        val surfaceW = state.viewportWidth
        val surfaceH = state.viewportHeight
        if (surfaceW <= 0 || surfaceH <= 0) {
            if (state.enableWorkloadTelemetry) {
                workloadDiagnosticStatus = "TEL ON · WAITING FOR SURFACE"
                stateHolder.updateTelemetry { it.copy(adaptiveWorkload = workloadDiagnosticStatus) }
            }
            return
        }

        val quad = quadGeometry ?: run {
            if (state.enableWorkloadTelemetry) {
                workloadDiagnosticStatus = "TEL ON · GL NOT READY"
                stateHolder.updateTelemetry { it.copy(adaptiveWorkload = workloadDiagnosticStatus) }
            }
            return
        }
        if (state.enableWorkloadTelemetry) {
            workloadDiagnosticStatus = "TEL ON · GL FRAME OBSERVED"
        }
        val activeProductionProgram = if (state.useGeodesicShader && geodesicProgram != null) {
            geodesicProgram
        } else {
            testProgram
        } ?: run {
            if (state.enableWorkloadTelemetry) {
                workloadDiagnosticStatus = "TEL ON · PRODUCTION PROGRAM NOT READY"
                stateHolder.updateTelemetry { it.copy(adaptiveWorkload = workloadDiagnosticStatus) }
            }
            return
        }

        val forcePresentation = presentationInvalidationPending
        presentationInvalidationPending = false

        val scale = state.renderScale.coerceIn(0.25f, 1.0f)
        val renderW = max(1, (surfaceW * scale).roundToInt())
        val renderH = max(1, (surfaceH * scale).roundToInt())

        // FBO allocation is dimension-dependent and therefore cheap on a stationary frame: helpers
        // return immediately when their existing attachments already match the target. The main
        // HDR target remains at the existing internal resolution for bloom/composite; only the
        // temporary coarse ray target may use a coarser uniform grid.
        updateHdrFbo(renderW, renderH)
        val requestedSamplingGrid = GargantuaCoarseSampling.grid(
            renderW,
            renderH,
            if (state.useGeodesicShader) effectiveCoarseSamplingBlockSize(state)
            else GargantuaCoarseSampling.BASELINE_BLOCK_SIZE
        )
        val rayGrid = if (
            requestedSamplingGrid.blockSize > GargantuaCoarseSampling.BASELINE_BLOCK_SIZE &&
            ensureCoarseRayFbo(requestedSamplingGrid.rayWidth, requestedSamplingGrid.rayHeight)
        ) {
            requestedSamplingGrid
        } else {
            // Returning to 1x1 does not need the diagnostic target; release it immediately so a
            // later mode change allocates a fresh target with the new ray-grid dimensions.
            if (coarseRayTargetAvailable || requestedSamplingGrid.blockSize > GargantuaCoarseSampling.BASELINE_BLOCK_SIZE) {
                deleteCoarseRayFbo()
            }
            GargantuaCoarseSampling.grid(renderW, renderH, GargantuaCoarseSampling.BASELINE_BLOCK_SIZE)
        }
        val rayTextureId = if (rayGrid.blockSize > 1) coarseRayTextureId else hdrTextureId
        val rayFboId = if (rayGrid.blockSize > 1) coarseRayFboId else hdrFboId

        if (state.enableBloom) {
            updateBloomFbos(max(1, renderW / 2), max(1, renderH / 2))
        }

        val now = System.nanoTime()
        val elapsedSeconds = (now - startTimeNanos) / 1_000_000_000.0f

        // SceneSignature remains the complete public/test-visible equality record. The renderer
        // uses the narrower signatures below to avoid retracing for presentation-only changes.
        val currentSig = SceneSignature.fromState(state, surfaceW, surfaceH)
        if (currentSig != lastSceneSignature) {
            lastSceneSignature = currentSig
        }

        val raySig = RaySceneSignature.fromState(state, surfaceW, surfaceH)
        if (raySig != lastRaySceneSignature) {
            lastRaySceneSignature = raySig
            sceneDirty = true
        }

        val bloomSig = BloomSignature(state.enableBloom, state.bloomThreshold)
        val bloomChanged = bloomSig != lastBloomSignature
        lastBloomSignature = bloomSig

        val compositeSig = CompositeSignature(
            exposure = state.exposure,
            enableBloom = state.enableBloom,
            bloomIntensity = state.bloomIntensity,
            isPaused = state.isPaused
        )
        val compositeChanged = compositeSig != lastCompositeSignature
        lastCompositeSignature = compositeSig

        if (state.enableWorkloadTelemetry) {
            ensureWorkloadPrograms()
        } else if (workloadGeodesicProgram != null || reduceProgram != null || workloadFboId != 0) {
            releaseWorkloadDiagnosticResources()
        }

        val workloadRequested =
            state.useGeodesicShader &&
                state.enableWorkloadTelemetry &&
                workloadGeodesicProgram != null &&
                reduceProgram != null &&
                ensureWorkloadFbo(rayGrid.rayWidth, rayGrid.rayHeight, rayTextureId)
        if (state.enableWorkloadTelemetry) {
            when {
                !state.useGeodesicShader ->
                    workloadDiagnosticStatus = "TEL ON · GEODESIC PATH OFF"
                workloadRequested ->
                    workloadDiagnosticStatus = "TEL ON · MRT/FBO READY"
                workloadGeodesicProgram == null || reduceProgram == null ->
                    workloadDiagnosticStatus = workloadProgramFailureStatus ?: "TEL ON · WORKLOAD NOT READY"
                else -> {
                    if (workloadDiagnosticStatus != "TEL ON · REDUCTION FBO NOT READY") {
                        workloadDiagnosticStatus = "TEL ON · MRT/FBO NOT READY"
                    }
                }
            }
        }

        var geodesicCpuSubmitMs = 0f
        var coarseUpscaleCpuSubmitMs = 0f
        var brightPassCpuSubmitMs = 0f
        var horizontalBlurCpuSubmitMs = 0f
        var verticalBlurCpuSubmitMs = 0f
        var compositeCpuSubmitMs = 0f

        val renderedScene = sceneDirty
        if (state.enableWorkloadTelemetry && workloadRequested && !renderedScene) {
            workloadDiagnosticStatus = if (
                lastWorkloadStats.available && lastWorkloadStats.totalPixels > 0L
            ) {
                "TEL ON · STATS READY"
            } else {
                "TEL ON · NO SCENE RENDER"
            }
        }
        if (renderedScene) {
            if (workloadRequested) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, workloadFboId)
                GLES30.glDrawBuffers(
                    3,
                    intArrayOf(
                        GLES30.GL_COLOR_ATTACHMENT0,
                        GLES30.GL_COLOR_ATTACHMENT1,
                        GLES30.GL_COLOR_ATTACHMENT2
                    ),
                    0
                )
            } else {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, rayFboId)
            }
            GLES30.glViewport(0, 0, rayGrid.rayWidth, rayGrid.rayHeight)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            val activeProg = if (workloadRequested) workloadGeodesicProgram!! else activeProductionProgram
            val geodesicStart = System.nanoTime()
            activeProg.use()
            activeProg.setUniform2f("u_Resolution", rayGrid.rayWidth.toFloat(), rayGrid.rayHeight.toFloat())
            activeProg.setUniform1f("u_Time", elapsedSeconds)

            if (activeProg == geodesicProgram || activeProg == workloadGeodesicProgram) {
                // Interactive observer position in Kerr-Schild Cartesian coordinates.
                val inclRad = Math.toRadians(state.camInclinationDeg.toDouble())
                val azRad = Math.toRadians(state.camAzimuthDeg.toDouble())
                val dist = state.camDist.toDouble()

                val dirX = sin(inclRad) * cos(azRad)
                val dirY = sin(inclRad) * sin(azRad)
                val dirZ = cos(inclRad)

                val camX = state.camTargetX.toDouble() + dist * dirX
                val camY = state.camTargetY.toDouble() + dist * dirY
                val camZ = state.camTargetZ.toDouble() + dist * dirZ

                val fwdRawX = state.camTargetX.toDouble() - camX
                val fwdRawY = state.camTargetY.toDouble() - camY
                val fwdRawZ = state.camTargetZ.toDouble() - camZ
                val fwdLen = sqrt(fwdRawX * fwdRawX + fwdRawY * fwdRawY + fwdRawZ * fwdRawZ)
                val (fwdX, fwdY, fwdZ) = if (fwdLen > 1e-6) {
                    Triple(fwdRawX / fwdLen, fwdRawY / fwdLen, fwdRawZ / fwdLen)
                } else {
                    Triple(-dirX, -dirY, -dirZ)
                }

                // Stable orthonormal camera basis for full 360-degree orbit.
                val rX = -sin(azRad)
                val rY = cos(azRad)
                val rZ = 0.0
                val upX = rY * fwdZ - rZ * fwdY
                val upY = rZ * fwdX - rX * fwdZ
                val upZ = rX * fwdY - rY * fwdX
                val upLen = sqrt(upX * upX + upY * upY + upZ * upZ)
                val (normUpX, normUpY, normUpZ) = if (upLen > 1e-6) {
                    Triple(upX / upLen, upY / upLen, upZ / upLen)
                } else {
                    Triple(0.0, 0.0, 1.0)
                }

                val fovScale = tan(Math.toRadians(45.0 * 0.5)).toFloat()
                val isco = com.zig.gargantua.disk.KerrIsco.compute(
                    state.mass.toDouble(),
                    state.spin.toDouble() * state.mass.toDouble()
                ).toFloat()
                lastIscoRadius = isco

                // M9 is not part of the production baseline. Avoid even calculating its angular
                // velocity unless an explicit experimental/instrumentation state enables it.
                val omegaObj = if (state.enableObject) {
                    val rObj = state.objectOrbitRadius.toDouble()
                    val mBH = state.mass.toDouble()
                    val aBH = state.spin.toDouble() * mBH
                    val denomOmega = rObj.pow(1.5) + aBH * sqrt(mBH)
                    if (abs(denomOmega) > 1e-12) (sqrt(mBH) / denomOmega).toFloat() else 0.0f
                } else {
                    0.0f
                }

                activeProg.setUniform1f("u_Mass", state.mass)
                activeProg.setUniform1f("u_Spin", state.spin * state.mass)
                activeProg.setUniform3f("u_CamPos", camX.toFloat(), camY.toFloat(), camZ.toFloat())
                activeProg.setUniform3f("u_CamForward", fwdX.toFloat(), fwdY.toFloat(), fwdZ.toFloat())
                activeProg.setUniform3f("u_CamRight", rX.toFloat(), rY.toFloat(), rZ.toFloat())
                activeProg.setUniform3f("u_CamUp", normUpX.toFloat(), normUpY.toFloat(), normUpZ.toFloat())
                activeProg.setUniform1f("u_FovScale", fovScale)
                activeProg.setUniform1i("u_MaxSteps", state.maxSteps)
                activeProg.setUniform1f("u_DiskInnerRadius", isco)
                activeProg.setUniform1f("u_DiskOuterRadius", state.diskOuterRadius)
                activeProg.setUniform1i("u_EnableDisk", if (state.enableDisk) 1 else 0)

                activeProg.setUniform1i("u_EnableObject", if (state.enableObject) 1 else 0)
                activeProg.setUniform1f("u_ObjectRadius", state.objectRadius)
                activeProg.setUniform1f("u_ObjectOrbitRadius", state.objectOrbitRadius)
                activeProg.setUniform1f("u_ObjectOmega", omegaObj)
                activeProg.setUniform1f("u_ObjectPhi0", state.objectPhi0)
                activeProg.setUniform1f("u_ObjectZ", state.objectZ)
                activeProg.setUniform3f("u_ObjectBaseColor", 0.15f, 0.85f, 1.0f)
                activeProg.setUniform1f("u_ObjectRadiance", 35.0f)
            }

            quad.draw()
            geodesicCpuSubmitMs = elapsedMilliseconds(geodesicStart)

            lastWorkloadStats = if (workloadRequested) {
                workloadDiagnosticStatus = "TEL ON · WORKLOAD PASS SUBMITTED"
                collectWorkloadStats(renderW, renderH, rayGrid).also { stats ->
                    workloadDiagnosticStatus = when {
                        !workloadReadbackValid -> "TEL ON · REDUCTION READBACK INVALID"
                        stats.available && stats.totalPixels > 0L -> "TEL ON · STATS READY"
                        stats.available -> "TEL ON · REDUCTION READBACK ZERO"
                        else -> "TEL ON · REDUCTION READBACK UNAVAILABLE"
                    }
                }
            } else {
                samplingSummary(rayGrid)
            }

            if (rayGrid.blockSize > GargantuaCoarseSampling.BASELINE_BLOCK_SIZE) {
                val upscaleStart = System.nanoTime()
                upscaleCoarseRayTexture(rayTextureId, renderW, renderH, quad)
                coarseUpscaleCpuSubmitMs = elapsedMilliseconds(upscaleStart)
            }
            sceneDirty = false
        }

        val shouldRunBloom =
            state.enableBloom &&
                brightPassProgram != null &&
                blurProgram != null &&
                bloomFboA != 0 &&
                (renderedScene || bloomChanged)

        if (shouldRunBloom) {
            // Bright-pass extraction and downsampling.
            val brightStart = System.nanoTime()
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboA)
            GLES30.glViewport(0, 0, bloomWidth, bloomHeight)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            brightPassProgram?.let { bp ->
                bp.use()
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, hdrTextureId)
                bp.setUniform1i("u_HdrTexture", 0)
                bp.setUniform1f("u_BloomThreshold", state.bloomThreshold)
                quad.draw()
            }
            brightPassCpuSubmitMs = elapsedMilliseconds(brightStart)

            // Separable Gaussian blur, horizontal.
            val horizontalStart = System.nanoTime()
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboB)
            GLES30.glViewport(0, 0, bloomWidth, bloomHeight)
            blurProgram?.let { blur ->
                blur.use()
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTexA)
                blur.setUniform1i("u_Texture", 0)
                blur.setUniform2f("u_Direction", 1.0f / bloomWidth, 0.0f)
                quad.draw()
            }
            horizontalBlurCpuSubmitMs = elapsedMilliseconds(horizontalStart)

            // Separable Gaussian blur, vertical.
            val verticalStart = System.nanoTime()
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboA)
            GLES30.glViewport(0, 0, bloomWidth, bloomHeight)
            blurProgram?.let { blur ->
                blur.use()
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTexB)
                blur.setUniform1i("u_Texture", 0)
                blur.setUniform2f("u_Direction", 0.0f, 1.0f / bloomHeight)
                quad.draw()
            }
            verticalBlurCpuSubmitMs = elapsedMilliseconds(verticalStart)
        }

        val shouldRunComposite = renderedScene || bloomChanged || compositeChanged || forcePresentation
        if (shouldRunComposite) {
            val compositeStart = System.nanoTime()
            renderCompositeToDisplay(surfaceW, surfaceH, state, quad)
            compositeCpuSubmitMs = elapsedMilliseconds(compositeStart)
        }
        lastPassTimings = GargantuaPassTimings(
            geodesicCpuSubmitMs = geodesicCpuSubmitMs,
            coarseUpscaleCpuSubmitMs = coarseUpscaleCpuSubmitMs,
            brightPassCpuSubmitMs = brightPassCpuSubmitMs,
            horizontalBlurCpuSubmitMs = horizontalBlurCpuSubmitMs,
            verticalBlurCpuSubmitMs = verticalBlurCpuSubmitMs,
            compositeCpuSubmitMs = compositeCpuSubmitMs,
            gpuTimerAvailable = false
        )

        // A WHEN_DIRTY callback is not a continuously ticking display clock. Count only frames
        // that actually submit a composite, and restart the measurement window after an idle gap;
        // otherwise the first frame after a stationary period would be diluted by idle time.
        val submittedAtNanos = System.nanoTime()
        val presentationSubmitted = shouldRunComposite
        var fpsWindowRestarted = false
        if (presentationSubmitted) {
            if (
                fpsLastSubmittedNanos == 0L ||
                submittedAtNanos - fpsLastSubmittedNanos > FPS_IDLE_RESET_NANOS
            ) {
                fpsFrames = 0
                fpsAccumulatorTimeNanos = submittedAtNanos
                fpsWindowRestarted = true
            }
            fpsFrames++
            fpsLastSubmittedNanos = submittedAtNanos
        }
        val fpsInterval = submittedAtNanos - fpsAccumulatorTimeNanos
        val shouldUpdateFps = presentationSubmitted && fpsInterval >= FPS_WINDOW_NANOS
        val measuredFps = when {
            shouldUpdateFps -> (fpsFrames * 1_000_000_000.0f) / fpsInterval
            fpsWindowRestarted -> 0f
            else -> stateHolder.getTelemetry().fps
        }
        val stats = lastWorkloadStats
        val workloadText = if (state.enableWorkloadTelemetry) {
            workloadDiagnosticStatus
        } else {
            when {
                stats.samplingBlockSize > 1 ->
                    "block ${stats.samplingBlockSize}x: unavailable (enable debug telemetry)"
                else ->
                    "Unavailable (debug instrumentation disabled)"
            }
        }

        stateHolder.updateTelemetry {
            it.copy(
                fps = measuredFps,
                frameTimeMs = ((System.nanoTime() - frameStartNanos) / 1_000_000.0f),
                spin = state.spin,
                isDiskActive = state.enableDisk,
                isObjectActive = state.enableObject,
                iscoRadius = lastIscoRadius,
                renderScale = scale,
                renderResolution = "${renderW}x${renderH}",
                isHdrActive = isHdrSupported,
                exposure = state.exposure,
                camDist = state.camDist,
                camInclinationDeg = state.camInclinationDeg,
                camAzimuthDeg = state.camAzimuthDeg,
                camTargetX = state.camTargetX,
                camTargetY = state.camTargetY,
                camTargetZ = state.camTargetZ,
                adaptiveWorkload = workloadText,
                avgRaysPerPixel = stats.averageRaysPerPixel,
                maxRaysPerPixel = stats.maximumRaysPerPixel,
                workloadStats = stats,
                passTimings = lastPassTimings
            )
        }
        if (shouldUpdateFps) {
            fpsFrames = 0
            fpsAccumulatorTimeNanos = submittedAtNanos
        }
    }

    private fun elapsedMilliseconds(startNanos: Long): Float =
        (System.nanoTime() - startNanos) / 1_000_000.0f

    private fun renderCompositeToDisplay(
        dstWidth: Int,
        dstHeight: Int,
        state: GargantuaRenderState,
        quad: QuadGeometry
    ) {
        val composite = compositeProgram
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, dstWidth, dstHeight)

        if (composite != null) {
            composite.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, hdrTextureId)
            composite.setUniform1i("u_HdrTexture", 0)

            val bloomReady =
                state.enableBloom &&
                    brightPassProgram != null &&
                    blurProgram != null &&
                    bloomFboA != 0 &&
                    bloomTexA != 0
            if (bloomReady) {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTexA)
                composite.setUniform1i("u_BloomTexture", 1)
                composite.setUniform1i("u_EnableBloom", 1)
            } else {
                composite.setUniform1i("u_EnableBloom", 0)
            }

            composite.setUniform1f("u_Exposure", state.exposure)
            composite.setUniform1f("u_BloomIntensity", state.bloomIntensity)
            quad.draw()

            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        } else {
            // Direct blit fallback if composite shader unavailable
            val blit = blitProgram
            if (blit != null) {
                blit.use()
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, hdrTextureId)
                blit.setUniform1i("u_Texture", 0)
                quad.draw()
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            } else {
                GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, hdrFboId)
                GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
                GLES30.glBlitFramebuffer(
                    0, 0, hdrWidth, hdrHeight,
                    0, 0, dstWidth, dstHeight,
                    GLES30.GL_COLOR_BUFFER_BIT,
                    GLES30.GL_LINEAR
                )
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            }
        }
    }

    /**
     * Upscales the coarse ray texture into the unchanged internal HDR target. This is a diagnostic
     * texture-filtering pass only; it does not alter the ray shader or any physical calculation.
     */
    private fun upscaleCoarseRayTexture(
        rayTextureId: Int,
        targetWidth: Int,
        targetHeight: Int,
        quad: QuadGeometry
    ) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, hdrFboId)
        GLES30.glViewport(0, 0, targetWidth, targetHeight)
        val blit = blitProgram
        if (blit != null) {
            blit.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, rayTextureId)
            blit.setUniform1i("u_Texture", 0)
            quad.draw()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, coarseRayFboId)
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, hdrFboId)
            GLES30.glBlitFramebuffer(
                0, 0, coarseRayWidth, coarseRayHeight,
                0, 0, targetWidth, targetHeight,
                GLES30.GL_COLOR_BUFFER_BIT,
                GLES30.GL_LINEAR
            )
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, hdrFboId)
        }
    }

    /**
     * Allocates the temporary coarse ray target. The full internal HDR target is intentionally
     * retained so the existing bloom and composite pipeline receives the same-sized input.
     */
    private fun ensureCoarseRayFbo(targetWidth: Int, targetHeight: Int): Boolean {
        if (
            coarseRayTargetAvailable &&
            coarseRayWidth == targetWidth &&
            coarseRayHeight == targetHeight &&
            coarseRayFboId != 0 &&
            coarseRayTextureId != 0
        ) {
            return true
        }

        deleteCoarseRayFbo()
        val fboIds = IntArray(1)
        val textureIds = IntArray(1)
        GLES30.glGenFramebuffers(1, fboIds, 0)
        GLES30.glGenTextures(1, textureIds, 0)
        coarseRayFboId = fboIds[0]
        coarseRayTextureId = textureIds[0]
        coarseRayWidth = targetWidth
        coarseRayHeight = targetHeight

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, coarseRayTextureId)
        val internalFormat = if (isHdrSupported) GLES30.GL_RGBA16F else GLES30.GL_RGBA8
        val formatType = if (isHdrSupported) GLES30.GL_HALF_FLOAT else GLES30.GL_UNSIGNED_BYTE
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            internalFormat,
            coarseRayWidth,
            coarseRayHeight,
            0,
            GLES30.GL_RGBA,
            formatType,
            null
        )
        // Linear filtering is the diagnostic upscale requested by the experiment.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, coarseRayFboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            coarseRayTextureId,
            0
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.w(TAG, "Coarse diagnostic framebuffer unavailable: status=$status")
            deleteCoarseRayFbo()
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            return false
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        coarseRayTargetAvailable = true
        return true
    }

    private fun deleteCoarseRayFbo() {
        if (coarseRayFboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(coarseRayFboId), 0)
        }
        if (coarseRayTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(coarseRayTextureId), 0)
        }
        coarseRayFboId = 0
        coarseRayTextureId = 0
        coarseRayWidth = 0
        coarseRayHeight = 0
        coarseRayTargetAvailable = false
    }

    /**
     * Allocates the debug-only MRT and reduction attachments. RGBA32F is intentionally required
     * here: counters are summed over the internal frame and must not silently overflow/quantize
     * in a half-float debug buffer. If the device cannot render to RGBA32F, telemetry reports
     * unavailable instead of guessing.
     */
    private fun ensureWorkloadFbo(targetWidth: Int, targetHeight: Int, rayTextureId: Int): Boolean {
        if (workloadGeodesicProgram == null || reduceProgram == null) return false
        if (rayTextureId == 0) {
            workloadDiagnosticStatus = "TEL ON · MRT/FBO NOT READY"
            return false
        }
        if (
            workloadTelemetrySupported &&
            workloadWidth == targetWidth &&
            workloadHeight == targetHeight &&
            workloadRayTextureId == rayTextureId &&
            workloadFboId != 0
        ) {
            return true
        }

        deleteWorkloadFbos()
        workloadDiagnosticStatus = "TEL ON · MRT/FBO ALLOCATING"

        val fboIds = IntArray(3)
        val textureIds = IntArray(4)
        GLES30.glGenFramebuffers(3, fboIds, 0)
        GLES30.glGenTextures(4, textureIds, 0)

        workloadFboId = fboIds[0]
        workloadReduceFboA = fboIds[1]
        workloadReduceFboB = fboIds[2]
        workloadTierTextureId = textureIds[0]
        workloadCostTextureId = textureIds[1]
        workloadReduceTextureA = textureIds[2]
        workloadReduceTextureB = textureIds[3]
        workloadWidth = targetWidth
        workloadHeight = targetHeight
        workloadRayTextureId = rayTextureId

        fun initFloatTexture(textureId: Int) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA32F,
                targetWidth,
                targetHeight,
                0,
                GLES30.GL_RGBA,
                GLES30.GL_FLOAT,
                null
            )
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        }

        initFloatTexture(workloadTierTextureId)
        initFloatTexture(workloadCostTextureId)
        initFloatTexture(workloadReduceTextureA)
        initFloatTexture(workloadReduceTextureB)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, workloadFboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            rayTextureId,
            0
        )
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT1,
            GLES30.GL_TEXTURE_2D,
            workloadTierTextureId,
            0
        )
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT2,
            GLES30.GL_TEXTURE_2D,
            workloadCostTextureId,
            0
        )
        GLES30.glDrawBuffers(
            3,
            intArrayOf(
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_COLOR_ATTACHMENT1,
                GLES30.GL_COLOR_ATTACHMENT2
            ),
            0
        )
        val workloadStatus = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (workloadStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.w(TAG, "RGBA32F workload framebuffer unavailable: status=$workloadStatus")
            deleteWorkloadFbos()
            workloadDiagnosticStatus = "TEL ON · MRT/FBO NOT READY"
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            return false
        }

        fun attachReductionFbo(fboId: Int, textureId: Int): Boolean {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                textureId,
                0
            )
            GLES30.glDrawBuffers(1, intArrayOf(GLES30.GL_COLOR_ATTACHMENT0), 0)
            return GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE
        }

        val reductionAComplete = attachReductionFbo(workloadReduceFboA, workloadReduceTextureA)
        val reductionBComplete = attachReductionFbo(workloadReduceFboB, workloadReduceTextureB)
        if (!reductionAComplete || !reductionBComplete) {
            Log.w(TAG, "RGBA32F workload reduction framebuffer unavailable")
            deleteWorkloadFbos()
            workloadDiagnosticStatus = "TEL ON · REDUCTION FBO NOT READY"
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            return false
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        workloadTelemetrySupported = true
        Log.i(TAG, "Debug workload telemetry enabled for ${targetWidth}x${targetHeight} internal pixels")
        workloadDiagnosticStatus = "TEL ON · MRT/FBO READY"
        return true
    }

    private fun samplingSummary(grid: GargantuaCoarseSampling.Grid): GargantuaWorkloadStats =
        GargantuaWorkloadStats(
            available = false,
            internalWidth = grid.internalWidth,
            internalHeight = grid.internalHeight,
            frameWidth = grid.rayWidth,
            frameHeight = grid.rayHeight,
            samplingBlockSize = grid.blockSize,
            shadedBlocks = grid.shadedBlocks,
            primaryRayCalculations = grid.shadedBlocks,
            primaryRayReductionVsBaseline = grid.primaryRayReductionVsBaseline
        )

    private fun collectWorkloadStats(
        internalWidth: Int,
        internalHeight: Int,
        grid: GargantuaCoarseSampling.Grid
    ): GargantuaWorkloadStats {
        if (!workloadTelemetrySupported || reduceProgram == null || quadGeometry == null) {
            return samplingSummary(grid)
        }
        workloadReadbackValid = true

        val width = grid.rayWidth
        val height = grid.rayHeight

        val tierTotals = reduceWorkloadTexture(workloadTierTextureId, width, height, maxChannel = -1)
        val costTotals = reduceWorkloadTexture(workloadCostTextureId, width, height, maxChannel = 2)
        if (!workloadReadbackValid) {
            return samplingSummary(grid)
        }

        val tier0 = tierTotals[0].roundToLong().coerceAtLeast(0L)
        val tier1 = tierTotals[1].roundToLong().coerceAtLeast(0L)
        val tier2 = tierTotals[2].roundToLong().coerceAtLeast(0L)
        val difficult = tierTotals[3].roundToLong().coerceAtLeast(0L)
        val totalPixels = tier0 + tier1 + tier2
        val totalRays = costTotals[0].roundToLong().coerceAtLeast(0L)
        val totalSteps = costTotals[1].roundToLong().coerceAtLeast(0L)
        val maximumSteps = costTotals[2].roundToInt().coerceAtLeast(0)
        val diskIntersections = costTotals[3].roundToLong().coerceAtLeast(0L)
        val averageRays = if (totalPixels > 0L) totalRays.toFloat() / totalPixels.toFloat() else 0f
        val averageSteps = if (totalRays > 0L) totalSteps.toFloat() / totalRays.toFloat() else 0f
        val maximumRays = when {
            tier2 > 0L -> 9
            tier1 > 0L -> 5
            tier0 > 0L -> 1
            else -> 0
        }

        return GargantuaWorkloadStats(
            available = true,
            internalWidth = internalWidth,
            internalHeight = internalHeight,
            frameWidth = width,
            frameHeight = height,
            samplingBlockSize = grid.blockSize,
            shadedBlocks = grid.shadedBlocks,
            primaryRayCalculations = grid.shadedBlocks,
            primaryRayReductionVsBaseline = grid.primaryRayReductionVsBaseline,
            tier0Pixels = tier0,
            tier1Pixels = tier1,
            tier2Pixels = tier2,
            difficultRayCount = difficult,
            totalRaysFrame = totalRays,
            averageRaysPerPixel = averageRays,
            maximumRaysPerPixel = maximumRays,
            totalIntegrationSteps = totalSteps,
            averageIntegrationSteps = averageSteps,
            maximumIntegrationSteps = maximumSteps,
            diskIntersections = diskIntersections
        )
    }

    /** One 1x1 float readback per metric group, only in the explicit debug instrumentation path. */
    private fun reduceWorkloadTexture(
        inputTextureId: Int,
        inputWidth: Int,
        inputHeight: Int,
        maxChannel: Int
    ): FloatArray {
        var sourceTexture = inputTextureId
        var sourceWidth = inputWidth
        var sourceHeight = inputHeight
        var ping = 0
        var lastOutputFbo = workloadReduceFboA

        if (sourceWidth == 1 && sourceHeight == 1) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, workloadReduceFboA)
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                sourceTexture,
                0
            )
        } else {
            while (sourceWidth > 1 || sourceHeight > 1) {
                val targetWidth = max(1, (sourceWidth + 1) / 2)
                val targetHeight = max(1, (sourceHeight + 1) / 2)
                val targetFbo = if (ping == 0) workloadReduceFboA else workloadReduceFboB
                val targetTexture = if (ping == 0) workloadReduceTextureA else workloadReduceTextureB
                lastOutputFbo = targetFbo

                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, targetFbo)
                GLES30.glViewport(0, 0, targetWidth, targetHeight)
                reduceProgram?.use()
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sourceTexture)
                reduceProgram?.setUniform1i("u_Texture", 0)
                reduceProgram?.setUniform2f("u_SourceSize", sourceWidth.toFloat(), sourceHeight.toFloat())
                reduceProgram?.setUniform1i("u_MaxChannel", maxChannel)
                quadGeometry?.draw()

                sourceTexture = targetTexture
                sourceWidth = targetWidth
                sourceHeight = targetHeight
                ping = 1 - ping
            }
        }

        val values = ByteBuffer.allocateDirect(4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, lastOutputFbo)
        GLES30.glViewport(0, 0, 1, 1)
        GLES30.glReadPixels(0, 0, 1, 1, GLES30.GL_RGBA, GLES30.GL_FLOAT, values)
        values.position(0)
        return FloatArray(4).also {
            values.get(it)
            if (it.any { value -> !value.isFinite() }) {
                workloadReadbackValid = false
            }
        }
    }

    private fun resetGpuResourceHandlesForNewContext() {
        hdrFboId = 0
        hdrTextureId = 0
        hdrWidth = 0
        hdrHeight = 0
        isHdrSupported = false
        bloomFboA = 0
        bloomTexA = 0
        bloomFboB = 0
        bloomTexB = 0
        bloomWidth = 0
        bloomHeight = 0
        coarseRayFboId = 0
        coarseRayTextureId = 0
        coarseRayWidth = 0
        coarseRayHeight = 0
        coarseRayTargetAvailable = false
        workloadFboId = 0
        workloadTierTextureId = 0
        workloadCostTextureId = 0
        workloadReduceFboA = 0
        workloadReduceTextureA = 0
        workloadReduceFboB = 0
        workloadReduceTextureB = 0
        workloadWidth = 0
        workloadHeight = 0
        workloadRayTextureId = 0
        workloadTelemetrySupported = false
        workloadProgramAttempted = false
        workloadProgramFailureStatus = null
        workloadDiagnosticStatus = "TEL OFF"
        workloadReadbackValid = true
    }

    private fun deleteWorkloadFbos() {
        if (workloadFboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(workloadFboId), 0)
        }
        if (workloadReduceFboA != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(workloadReduceFboA), 0)
        }
        if (workloadReduceFboB != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(workloadReduceFboB), 0)
        }
        if (workloadTierTextureId != 0 || workloadCostTextureId != 0) {
            GLES30.glDeleteTextures(2, intArrayOf(workloadTierTextureId, workloadCostTextureId), 0)
        }
        if (workloadReduceTextureA != 0 || workloadReduceTextureB != 0) {
            GLES30.glDeleteTextures(2, intArrayOf(workloadReduceTextureA, workloadReduceTextureB), 0)
        }
        workloadFboId = 0
        workloadTierTextureId = 0
        workloadCostTextureId = 0
        workloadReduceFboA = 0
        workloadReduceTextureA = 0
        workloadReduceFboB = 0
        workloadReduceTextureB = 0
        workloadWidth = 0
        workloadHeight = 0
        workloadRayTextureId = 0
        workloadTelemetrySupported = false
    }

    private fun updateHdrFbo(targetWidth: Int, targetHeight: Int) {
        if (hdrWidth == targetWidth && hdrHeight == targetHeight && hdrFboId != 0) {
            return
        }
        // Workload MRT attachment 0 aliases the HDR texture, so it must be released before the
        // HDR attachment is resized or replaced.
        deleteWorkloadFbos()
        deleteHdrFbo()

        val fbos = IntArray(1)
        val texs = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)

        hdrFboId = fbos[0]
        hdrTextureId = texs[0]
        hdrWidth = targetWidth
        hdrHeight = targetHeight

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, hdrTextureId)
        // 1. Attempt GLES 3.0 floating-point HDR format (GL_RGBA16F)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F,
            hdrWidth, hdrHeight, 0,
            GLES30.GL_RGBA, GLES30.GL_HALF_FLOAT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, hdrFboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, hdrTextureId, 0
        )

        var status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            isHdrSupported = true
            Log.i(TAG, "Gargantua HDR GL_RGBA16F framebuffer initialized successfully (${hdrWidth}x${hdrHeight})")
        } else {
            // Graceful fallback to standard RGBA8 if floating-point render targets unsupported
            Log.w(TAG, "GL_RGBA16F not complete (status=$status); falling back to standard GL_RGBA8")
            isHdrSupported = false
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
                hdrWidth, hdrHeight, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
            )
            status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "Fallback RGBA8 framebuffer incomplete: status=$status")
            }
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    private fun updateBloomFbos(targetWidth: Int, targetHeight: Int) {
        if (bloomWidth == targetWidth && bloomHeight == targetHeight && bloomFboA != 0) {
            return
        }
        deleteBloomFbos()

        bloomWidth = targetWidth
        bloomHeight = targetHeight

        val fbos = IntArray(2)
        val texs = IntArray(2)
        GLES30.glGenFramebuffers(2, fbos, 0)
        GLES30.glGenTextures(2, texs, 0)

        bloomFboA = fbos[0]
        bloomTexA = texs[0]
        bloomFboB = fbos[1]
        bloomTexB = texs[1]

        fun initBloomAttachment(fbo: Int, tex: Int) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
            val internalFormat = if (isHdrSupported) GLES30.GL_RGBA16F else GLES30.GL_RGBA8
            val formatType = if (isHdrSupported) GLES30.GL_HALF_FLOAT else GLES30.GL_UNSIGNED_BYTE
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, internalFormat,
                bloomWidth, bloomHeight, 0,
                GLES30.GL_RGBA, formatType, null
            )
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, tex, 0
            )
        }

        initBloomAttachment(bloomFboA, bloomTexA)
        initBloomAttachment(bloomFboB, bloomTexB)

        val bloomAStatus = run {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboA)
            GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        }
        val bloomBStatus = run {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bloomFboB)
            GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        }
        if (bloomAStatus != GLES30.GL_FRAMEBUFFER_COMPLETE || bloomBStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.w(TAG, "Bloom framebuffer unavailable: A=$bloomAStatus, B=$bloomBStatus")
            deleteBloomFbos()
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    private fun deleteHdrFbo() {
        if (hdrFboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(hdrFboId), 0)
            hdrFboId = 0
        }
        if (hdrTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(hdrTextureId), 0)
            hdrTextureId = 0
        }
        hdrWidth = 0
        hdrHeight = 0
    }

    private fun deleteBloomFbos() {
        if (bloomFboA != 0 || bloomFboB != 0) {
            GLES30.glDeleteFramebuffers(2, intArrayOf(bloomFboA, bloomFboB), 0)
            bloomFboA = 0
            bloomFboB = 0
        }
        if (bloomTexA != 0 || bloomTexB != 0) {
            GLES30.glDeleteTextures(2, intArrayOf(bloomTexA, bloomTexB), 0)
            bloomTexA = 0
            bloomTexB = 0
        }
        bloomWidth = 0
        bloomHeight = 0
    }

    /**
     * Installs the surface callback used to request a dirty frame after GLES resources and the
     * non-empty surface size are both ready. The listener is invoked from the GL thread; the view
     * queues the actual request behind the lifecycle callback before asking for a dirty frame.
     */
    fun setRenderReadyListener(listener: (() -> Unit)?) {
        renderReadyListener = listener
    }

    /** Marks the cached image for presentation; the owning GLSurfaceView schedules the dirty frame. */
    fun requestPresentation() {
        presentationInvalidationPending = true
    }

    fun release() {
        deleteWorkloadFbos()
        deleteCoarseRayFbo()
        deleteHdrFbo()
        deleteBloomFbos()
        geodesicProgram?.release()
        geodesicProgram = null
        workloadGeodesicProgram?.release()
        workloadGeodesicProgram = null
        testProgram?.release()
        testProgram = null
        blitProgram?.release()
        blitProgram = null
        brightPassProgram?.release()
        brightPassProgram = null
        blurProgram?.release()
        blurProgram = null
        compositeProgram?.release()
        compositeProgram = null
        reduceProgram?.release()
        reduceProgram = null
        workloadProgramAttempted = false
        workloadProgramFailureStatus = null
        workloadDiagnosticStatus = "TEL OFF"
        workloadReadbackValid = true
        quadGeometry?.release()
        quadGeometry = null
    }

    companion object {
        private const val TAG = "GargantuaRenderer"
        private const val FPS_WINDOW_NANOS = 500_000_000L
        private const val FPS_IDLE_RESET_NANOS = 750_000_000L
    }
}
