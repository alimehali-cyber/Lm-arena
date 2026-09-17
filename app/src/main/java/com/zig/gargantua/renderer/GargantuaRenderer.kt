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
 * offscreen scaled FBO rendering, stationary frame caching, and thread-safe telemetry emission.
 */
class GargantuaRenderer(
    private val context: Context,
    val stateHolder: RenderStateHolder = RenderStateHolder()
) : GLSurfaceView.Renderer {

    private var geodesicProgram: ShaderProgram? = null
    private var testProgram: ShaderProgram? = null
    private var blitProgram: ShaderProgram? = null
    private var quadGeometry: QuadGeometry? = null

    // Offscreen Framebuffer Object (FBO) resources for resolution scaling
    private var fboId = 0
    private var fboTextureId = 0
    private var fboWidth = 0
    private var fboHeight = 0

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
        val maxSteps: Int,
        val enableDisk: Boolean,
        val diskOuterRadius: Float,
        val useGeodesicShader: Boolean
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val glVersion = GLES30.glGetString(GLES30.GL_VERSION) ?: "Unknown"
        val glRenderer = GLES30.glGetString(GLES30.GL_RENDERER) ?: "Unknown"
        val glVendor = GLES30.glGetString(GLES30.GL_VENDOR) ?: "Unknown"
        Log.i(TAG, "Gargantua GLES surface created: Version=$glVersion, Renderer=$glRenderer, Vendor=$glVendor")

        val vertSource = ShaderSource.loadVertexShader(context)

        // 1. Attempt compilation of Phase M4/M5 relativistic photon geodesic shader
        var isGeodesicReady = false
        try {
            val geodesicFragSource = ShaderSource.loadGeodesicFragmentShader(context)
            geodesicProgram?.release()
            geodesicProgram = ShaderProgram.create(vertSource, geodesicFragSource)
            isGeodesicReady = (geodesicProgram != null)
            if (isGeodesicReady) {
                Log.i(TAG, "Gargantua relativistic photon geodesic shader compiled and linked successfully.")
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

        // 3. Compile blit fragment shader for FBO scaled blitting
        try {
            val blitFragSource = ShaderSource.loadBlitFragmentShader(context)
            blitProgram?.release()
            blitProgram = ShaderProgram.create(vertSource, blitFragSource)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load blit shader; will use glBlitFramebuffer fallback", e)
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

        // Force initial render of 3 swapchain buffer passes
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

        // Ensure scaled offscreen FBO matches target internal render resolution
        updateFbo(renderW, renderH)

        val now = System.nanoTime()
        val deltaNanos = now - lastFrameTimeNanos
        lastFrameTimeNanos = now

        // Calculate elapsed time in seconds
        val elapsedSeconds = (now - startTimeNanos) / 1_000_000_000.0f

        // Check if scene has changed (camera, spacetime, parameters)
        val currentSig = SceneSignature(
            width = surfaceW,
            height = surfaceH,
            renderScale = scale,
            mass = state.mass,
            spin = state.spin,
            camDist = state.camDist,
            camInclinationDeg = state.camInclinationDeg,
            camAzimuthDeg = state.camAzimuthDeg,
            maxSteps = state.maxSteps,
            enableDisk = state.enableDisk,
            diskOuterRadius = state.diskOuterRadius,
            useGeodesicShader = (activeProg == geodesicProgram)
        )

        if (currentSig != lastSceneSignature) {
            lastSceneSignature = currentSig
            dirtyFramesRemaining = 3 // Ensure double/triple buffered EGL surfaces are refreshed
        }

        // Only re-run the heavy geodesic raymarch when state is dirty or using animated test shader
        val needsGeodesicRender = (dirtyFramesRemaining > 0) || (activeProg == testProgram)

        if (needsGeodesicRender) {
            // Pass 1: Render black hole geodesic rays to offscreen FBO
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
            GLES30.glViewport(0, 0, renderW, renderH)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            activeProg.use()
            activeProg.setUniform2f("u_Resolution", renderW.toFloat(), renderH.toFloat())
            activeProg.setUniform1f("u_Time", elapsedSeconds)

            if (activeProg == geodesicProgram) {
                // Camera position in Kerr-Schild Cartesian coordinates
                val inclRad = Math.toRadians(state.camInclinationDeg.toDouble())
                val azRad = Math.toRadians(state.camAzimuthDeg.toDouble())
                val dist = state.camDist.toDouble()

                val camX = dist * sin(inclRad) * cos(azRad)
                val camY = dist * sin(inclRad) * sin(azRad)
                val camZ = dist * cos(inclRad)

                // Target is coordinate origin (0, 0, 0)
                val fwdLen = sqrt(camX * camX + camY * camY + camZ * camZ)
                val fwdX = -camX / fwdLen
                val fwdY = -camY / fwdLen
                val fwdZ = -camZ / fwdLen

                // Camera orthonormal basis
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

            if (dirtyFramesRemaining > 0) {
                dirtyFramesRemaining--
            }
        }

        // Pass 2: Blit FBO texture to native display surface with hardware bilinear filtering
        blitFboToScreen(surfaceW, surfaceH, quad)

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
                    renderResolution = resStr
                )
            }
            fpsFrames = 0
            fpsAccumulatorTimeNanos = now
        }
    }

    private fun blitFboToScreen(dstWidth: Int, dstHeight: Int, quad: QuadGeometry) {
        val blit = blitProgram
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, dstWidth, dstHeight)

        if (blit != null) {
            blit.use()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
            blit.setUniform1i("u_Texture", 0)
            quad.draw()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        } else {
            // Hardware fallback via GLES 3.0 glBlitFramebuffer
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fboId)
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
            GLES30.glBlitFramebuffer(
                0, 0, fboWidth, fboHeight,
                0, 0, dstWidth, dstHeight,
                GLES30.GL_COLOR_BUFFER_BIT,
                GLES30.GL_LINEAR
            )
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }
    }

    private fun updateFbo(targetWidth: Int, targetHeight: Int) {
        if (fboWidth == targetWidth && fboHeight == targetHeight && fboId != 0) {
            return
        }
        deleteFbo()

        val fbos = IntArray(1)
        val texs = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)

        fboId = fbos[0]
        fboTextureId = texs[0]
        fboWidth = targetWidth
        fboHeight = targetHeight

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
            fboWidth, fboHeight, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, fboTextureId, 0
        )

        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Gargantua FBO incomplete: status=$status")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    private fun deleteFbo() {
        if (fboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            fboId = 0
        }
        if (fboTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
            fboTextureId = 0
        }
        fboWidth = 0
        fboHeight = 0
    }

    fun release() {
        deleteFbo()
        geodesicProgram?.release()
        geodesicProgram = null
        testProgram?.release()
        testProgram = null
        blitProgram?.release()
        blitProgram = null
        quadGeometry?.release()
        quadGeometry = null
    }

    companion object {
        private const val TAG = "GargantuaRenderer"
    }
}
