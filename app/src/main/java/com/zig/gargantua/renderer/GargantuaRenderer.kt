package com.zig.gargantua.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

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
    private var testProgram: ShaderProgram? = null
    private var blitProgram: ShaderProgram? = null
    private var brightPassProgram: ShaderProgram? = null
    private var blurProgram: ShaderProgram? = null
    private var compositeProgram: ShaderProgram? = null
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

    // Stationary frame scheduling cache
    private var lastSceneSignature: SceneSignature? = null
    private var dirtyFramesRemaining: Int = 3

    private var startTimeNanos: Long = 0L
    private var lastFrameTimeNanos: Long = 0L
    private var frameCount: Long = 0L
    private var fpsAccumulatorTimeNanos: Long = 0L
    private var fpsFrames: Int = 0

    private data class SceneSignature(
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
        val useGeodesicShader: Boolean,
        val exposure: Float,
        val enableBloom: Boolean,
        val bloomIntensity: Float,
        val bloomThreshold: Float
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val glVersion = GLES30.glGetString(GLES30.GL_VERSION) ?: "Unknown"
        val glRenderer = GLES30.glGetString(GLES30.GL_RENDERER) ?: "Unknown"
        val glVendor = GLES30.glGetString(GLES30.GL_VENDOR) ?: "Unknown"
        Log.i(TAG, "Gargantua GLES surface created: Version=$glVersion, Renderer=$glRenderer, Vendor=$glVendor")

        val vertSource = ShaderSource.loadVertexShader(context)

        // 1. Compile Phase M4/M5/M6 relativistic photon geodesic shader
        var isGeodesicReady = false
        try {
            val geodesicFragSource = ShaderSource.loadGeodesicFragmentShader(context)
            geodesicProgram?.release()
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

        // Force initial render of swapchain buffer passes
        lastSceneSignature = null
        dirtyFramesRemaining = 3

        // Initialize baseline timers
        val now = System.nanoTime()
        startTimeNanos = now
        lastFrameTimeNanos = now
        fpsAccumulatorTimeNanos = now
        fpsFrames = 0
        frameCount = 0L

        stateHolder.updateTelemetry {
            it.copy(
                glesVersion = glVersion,
                glRenderer = glRenderer,
                isInitialized = true,
                errorMessage = null,
                isGeodesicActive = isGeodesicReady
            )
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        stateHolder.updateState {
            it.copy(viewportWidth = width, viewportHeight = height)
        }
        lastSceneSignature = null
        dirtyFramesRemaining = 3
    }

    override fun onDrawFrame(gl: GL10?) {
        val state = stateHolder.getState()
        if (state.isPaused) return

        val surfaceW = state.viewportWidth
        val surfaceH = state.viewportHeight
        if (surfaceW <= 0 || surfaceH <= 0) return

        val quad = quadGeometry ?: return
        val activeProg = if (state.useGeodesicShader && geodesicProgram != null) {
            geodesicProgram
        } else {
            testProgram
        } ?: return

        val scale = state.renderScale.coerceIn(0.25f, 1.0f)
        val renderW = max(1, (surfaceW * scale).roundToInt())
        val renderH = max(1, (surfaceH * scale).roundToInt())

        // Ensure HDR FBO and downsampled bloom buffers match current resolution
        updateHdrFbo(renderW, renderH)
        if (state.enableBloom) {
            updateBloomFbos(max(1, renderW / 2), max(1, renderH / 2))
        }

        val now = System.nanoTime()
        val deltaNanos = now - lastFrameTimeNanos
        lastFrameTimeNanos = now

        // Calculate elapsed time in seconds
        val elapsedSeconds = (now - startTimeNanos) / 1_000_000_000.0f

        // Check if observer or spacetime state has changed
        val currentSig = SceneSignature(
            width = surfaceW,
            height = surfaceH,
            renderScale = scale,
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
            useGeodesicShader = (activeProg == geodesicProgram),
            exposure = state.exposure,
            enableBloom = state.enableBloom,
            bloomIntensity = state.bloomIntensity,
            bloomThreshold = state.bloomThreshold
        )

        if (currentSig != lastSceneSignature) {
            lastSceneSignature = currentSig
            dirtyFramesRemaining = 3 // Ensure double/triple buffered EGL surfaces are refreshed
        }

        val needsGeodesicRender = (dirtyFramesRemaining > 0) || (activeProg == testProgram)

        if (needsGeodesicRender) {
            // ==========================================
            // Pass 1: Primary HDR Geodesic Raymarching
            // ==========================================
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, hdrFboId)
            GLES30.glViewport(0, 0, renderW, renderH)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            activeProg.use()
            activeProg.setUniform2f("u_Resolution", renderW.toFloat(), renderH.toFloat())
            activeProg.setUniform1f("u_Time", elapsedSeconds)

            if (activeProg == geodesicProgram) {
                // Interactive Observer Position in Kerr-Schild Cartesian coordinates
                val inclRad = Math.toRadians(state.camInclinationDeg.toDouble())
                val azRad = Math.toRadians(state.camAzimuthDeg.toDouble())
                val dist = state.camDist.toDouble()

                val dirX = sin(inclRad) * cos(azRad)
                val dirY = sin(inclRad) * sin(azRad)
                val dirZ = cos(inclRad)

                // Observer target is (camTargetX, camTargetY, camTargetZ)
                val camX = state.camTargetX.toDouble() + dist * dirX
                val camY = state.camTargetY.toDouble() + dist * dirY
                val camZ = state.camTargetZ.toDouble() + dist * dirZ

                // Observer forward points from cam towards target
                val fwdRawX = state.camTargetX.toDouble() - camX
                val fwdRawY = state.camTargetY.toDouble() - camY
                val fwdRawZ = state.camTargetZ.toDouble() - camZ
                val fwdLen = sqrt(fwdRawX * fwdRawX + fwdRawY * fwdRawY + fwdRawZ * fwdRawZ)
                val (fwdX, fwdY, fwdZ) = if (fwdLen > 1e-6) {
                    Triple(fwdRawX / fwdLen, fwdRawY / fwdLen, fwdRawZ / fwdLen)
                } else {
                    Triple(-dirX, -dirY, -dirZ)
                }

                // Observer orthonormal tetrad basis
                val rightRawX = fwdY
                val rightRawY = -fwdX
                val rightRawZ = 0.0
                val rightLen = sqrt(rightRawX * rightRawX + rightRawY * rightRawY)
                val (rX, rY, rZ) = if (rightLen > 1e-6) {
                    Triple(rightRawX / rightLen, rightRawY / rightLen, 0.0)
                } else {
                    Triple(1.0, 0.0, 0.0)
                }

                // up = right x forward
                val upX = rY * fwdZ - rZ * fwdY
                val upY = rZ * fwdX - rX * fwdZ
                val upZ = rX * fwdY - rY * fwdX

                val fovScale = tan(Math.toRadians(45.0 * 0.5)).toFloat()

                val isco = com.zig.gargantua.disk.KerrIsco.compute(
                    state.mass.toDouble(),
                    state.spin.toDouble() * state.mass.toDouble()
                ).toFloat()

                activeProg.setUniform1f("u_Mass", state.mass)
                activeProg.setUniform1f("u_Spin", state.spin * state.mass)
                activeProg.setUniform3f("u_CamPos", camX.toFloat(), camY.toFloat(), camZ.toFloat())
                activeProg.setUniform3f("u_CamForward", fwdX.toFloat(), fwdY.toFloat(), fwdZ.toFloat())
                activeProg.setUniform3f("u_CamRight", rX.toFloat(), rY.toFloat(), rZ.toFloat())
                activeProg.setUniform3f("u_CamUp", upX.toFloat(), upY.toFloat(), upZ.toFloat())
                activeProg.setUniform1f("u_FovScale", fovScale)
                activeProg.setUniform1i("u_MaxSteps", state.maxSteps)
                activeProg.setUniform1f("u_DiskInnerRadius", isco)
                activeProg.setUniform1f("u_DiskOuterRadius", state.diskOuterRadius)
                activeProg.setUniform1i("u_EnableDisk", if (state.enableDisk) 1 else 0)
            }

            quad.draw()

            // ==========================================
            // Pass 2: Restrained Downsampled Bloom Pipeline
            // ==========================================
            if (state.enableBloom && brightPassProgram != null && blurProgram != null && bloomFboA != 0) {
                // 2a. Bright-Pass Extraction & Downsampling
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

                // 2b. Separable Gaussian Blur Horizontal (bloomFboA -> bloomFboB)
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

                // 2c. Separable Gaussian Blur Vertical (bloomFboB -> bloomFboA)
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
            }

            if (dirtyFramesRemaining > 0) {
                dirtyFramesRemaining--
            }
        }

        // ==========================================
        // Pass 3: ACES Filmic Tone Mapping & Display Composite
        // ==========================================
        renderCompositeToDisplay(surfaceW, surfaceH, state, quad)

        // Telemetry calculation
        frameCount++
        fpsFrames++
        val fpsInterval = now - fpsAccumulatorTimeNanos
        if (fpsInterval >= 500_000_000L) { // update every 500ms
            val measuredFps = (fpsFrames * 1_000_000_000.0f) / fpsInterval
            val frameTimeMs = (deltaNanos / 1_000_000.0f)
            val isco = com.zig.gargantua.disk.KerrIsco.compute(
                state.mass.toDouble(),
                state.spin.toDouble() * state.mass.toDouble()
            ).toFloat()

            val resStr = "${renderW}x${renderH}"

            stateHolder.updateTelemetry {
                it.copy(
                    fps = measuredFps,
                    frameTimeMs = frameTimeMs,
                    spin = state.spin,
                    isDiskActive = state.enableDisk,
                    iscoRadius = isco,
                    renderScale = scale,
                    renderResolution = resStr,
                    isHdrActive = isHdrSupported,
                    exposure = state.exposure,
                    camDist = state.camDist,
                    camInclinationDeg = state.camInclinationDeg,
                    camAzimuthDeg = state.camAzimuthDeg,
                    camTargetX = state.camTargetX,
                    camTargetY = state.camTargetY,
                    camTargetZ = state.camTargetZ
                )
            }
            fpsFrames = 0
            fpsAccumulatorTimeNanos = now
        }
    }

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

            if (state.enableBloom && bloomTexA != 0) {
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

    private fun updateHdrFbo(targetWidth: Int, targetHeight: Int) {
        if (hdrWidth == targetWidth && hdrHeight == targetHeight && hdrFboId != 0) {
            return
        }
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

    fun release() {
        deleteHdrFbo()
        deleteBloomFbos()
        geodesicProgram?.release()
        geodesicProgram = null
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
        quadGeometry?.release()
        quadGeometry = null
    }

    companion object {
        private const val TAG = "GargantuaRenderer"
    }
}
