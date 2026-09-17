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
 * timing, viewport sizing, and immutable state synchronization between UI and GL threads.
 */
class GargantuaRenderer(
    private val context: Context,
    val stateHolder: RenderStateHolder = RenderStateHolder()
) : GLSurfaceView.Renderer {

    private var geodesicProgram: ShaderProgram? = null
    private var testProgram: ShaderProgram? = null
    private var quadGeometry: QuadGeometry? = null

    private var startTimeNanos: Long = 0L
    private var lastFrameTimeNanos: Long = 0L
    private var frameCount: Long = 0L
    private var fpsAccumulatorTimeNanos: Long = 0L
    private var fpsFrames: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val glVersion = GLES30.glGetString(GLES30.GL_VERSION) ?: "Unknown"
        val glRenderer = GLES30.glGetString(GLES30.GL_RENDERER) ?: "Unknown"
        val glVendor = GLES30.glGetString(GLES30.GL_VENDOR) ?: "Unknown"
        Log.i(TAG, "Gargantua GLES surface created: Version=$glVersion, Renderer=$glRenderer, Vendor=$glVendor")

        val vertSource = ShaderSource.loadVertexShader(context)

        // 1. Attempt compilation of Phase M4 relativistic photon geodesic shader
        var isGeodesicReady = false
        try {
            val geodesicFragSource = ShaderSource.loadGeodesicFragmentShader(context)
            geodesicProgram?.release()
            geodesicProgram = ShaderProgram.create(vertSource, geodesicFragSource)
            isGeodesicReady = (geodesicProgram != null)
            if (isGeodesicReady) {
                Log.i(TAG, "Gargantua M4 relativistic photon geodesic shader compiled and linked successfully.")
            } else {
                Log.w(TAG, "Gargantua M4 geodesic shader failed to link; preparing fallback.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load geodesic shader asset; falling back to test shader", e)
        }

        // 2. Compile M1 baseline test shader as guaranteed fallback
        val testFragSource = ShaderSource.loadFragmentShader(context)
        testProgram?.release()
        testProgram = ShaderProgram.create(vertSource, testFragSource)

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

        // Initialize baseline timers
        val now = System.nanoTime()
        startTimeNanos = now
        lastFrameTimeNanos = now
        fpsAccumulatorTimeNanos = now
        fpsFrames = 0
        frameCount = 0L

        // Clear color (pure black for black hole horizon)
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

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
    }

    override fun onDrawFrame(gl: GL10?) {
        val state = stateHolder.getState()
        if (state.isPaused) return

        val now = System.nanoTime()
        val deltaNanos = now - lastFrameTimeNanos
        lastFrameTimeNanos = now

        // Calculate elapsed time in seconds for procedural sky animation
        val elapsedSeconds = (now - startTimeNanos) / 1_000_000_000.0f

        // Frame rate / timing telemetry
        frameCount++
        fpsFrames++
        val fpsInterval = now - fpsAccumulatorTimeNanos
        if (fpsInterval >= 500_000_000L) { // update every 500ms
            val measuredFps = (fpsFrames * 1_000_000_000.0f) / fpsInterval
            val frameTimeMs = (deltaNanos / 1_000_000.0f)
            stateHolder.updateTelemetry {
                it.copy(
                    fps = measuredFps,
                    frameTimeMs = frameTimeMs,
                    spin = state.spin
                )
            }
            fpsFrames = 0
            fpsAccumulatorTimeNanos = now
        }

        val quad = quadGeometry ?: return
        val activeProg = if (state.useGeodesicShader && geodesicProgram != null) {
            geodesicProgram
        } else {
            testProgram
        } ?: return

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        activeProg.use()

        val w = if (state.viewportWidth > 0) state.viewportWidth.toFloat() else 1.0f
        val h = if (state.viewportHeight > 0) state.viewportHeight.toFloat() else 1.0f
        activeProg.setUniform2f("u_Resolution", w, h)
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

            activeProg.setUniform1f("u_Mass", state.mass)
            activeProg.setUniform1f("u_Spin", state.spin * state.mass)
            activeProg.setUniform3f("u_CamPos", camX.toFloat(), camY.toFloat(), camZ.toFloat())
            activeProg.setUniform3f("u_CamForward", fwdX.toFloat(), fwdY.toFloat(), fwdZ.toFloat())
            activeProg.setUniform3f("u_CamRight", rX.toFloat(), rY.toFloat(), rZ.toFloat())
            activeProg.setUniform3f("u_CamUp", upX.toFloat(), upY.toFloat(), upZ.toFloat())
            activeProg.setUniform1f("u_FovScale", fovScale)
            activeProg.setUniform1i("u_MaxSteps", state.maxSteps)
        }

        quad.draw()
    }

    fun release() {
        geodesicProgram?.release()
        geodesicProgram = null
        testProgram?.release()
        testProgram = null
        quadGeometry?.release()
        quadGeometry = null
    }

    companion object {
        private const val TAG = "GargantuaRenderer"
    }
}
