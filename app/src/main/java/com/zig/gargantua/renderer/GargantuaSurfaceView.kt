package com.zig.gargantua.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log

/**
 * Native Android GLSurfaceView hosting the Gargantua OpenGL ES 3.x renderer.
 * Strictly requires GLES 3.x — capability detection occurs prior to view creation
 * in GargantuaRoot.
 */
class GargantuaSurfaceView(
    context: Context,
    val renderer: GargantuaRenderer = GargantuaRenderer(context)
) : GLSurfaceView(context) {

    init {
        Log.i(TAG, "Initializing Gargantua GLSurfaceView for OpenGL ES 3.x")

        // Strictly request client version 3 (no GLES2 fallback)
        setEGLContextClientVersion(3)

        // Standard 8-8-8-8 RGBA framebuffer configuration
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)

        // Preserve EGL context across onPause to avoid costly shader recompilation on transient pauses
        preserveEGLContextOnPause = true

        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onPause() {
        renderer.stateHolder.updateState { it.copy(isPaused = true) }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        renderer.stateHolder.updateState { it.copy(isPaused = false) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Queue release on GL thread
        queueEvent {
            renderer.release()
        }
    }

    companion object {
        private const val TAG = "GargantuaSurfaceView"
    }
}
