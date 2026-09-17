package com.zig.gargantua.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 3.x Renderer for the Gargantua laboratory foundation.
 * Orchestrates full-screen shader pipeline, timing, viewport sizing,
 * and immutable state synchronization between UI and GL threads.
 */
class GargantuaRenderer(
    private val context: Context,
    val stateHolder: RenderStateHolder = RenderStateHolder()
) : GLSurfaceView.Renderer {

    private var program: ShaderProgram? = null
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

        // Load shader source code
        val vertSource = ShaderSource.loadVertexShader(context)
        val fragSource = ShaderSource.loadFragmentShader(context)

        // Compile and link shader program
        program?.release()
        val newProgram = ShaderProgram.create(vertSource, fragSource)
        program = newProgram

        if (newProgram == null) {
            val errorMsg = "Failed to compile/link Gargantua shader program."
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

        // Clear color (deep space slate)
        GLES30.glClearColor(0.04f, 0.05f, 0.09f, 1.0f)

        stateHolder.updateTelemetry {
            it.copy(
                glesVersion = glVersion,
                glRenderer = glRenderer,
                isInitialized = true,
                errorMessage = null
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

        // Calculate elapsed time in seconds for procedural animation
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
                    frameTimeMs = frameTimeMs
                )
            }
            fpsFrames = 0
            fpsAccumulatorTimeNanos = now
        }

        val prog = program
        val quad = quadGeometry
        if (prog != null && quad != null) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            prog.use()

            val w = if (state.viewportWidth > 0) state.viewportWidth.toFloat() else 1.0f
            val h = if (state.viewportHeight > 0) state.viewportHeight.toFloat() else 1.0f
            prog.setUniform2f("u_Resolution", w, h)
            prog.setUniform1f("u_Time", elapsedSeconds)

            quad.draw()
        }
    }

    fun release() {
        program?.release()
        program = null
        quadGeometry?.release()
        quadGeometry = null
    }

    companion object {
        private const val TAG = "GargantuaRenderer"
    }
}
