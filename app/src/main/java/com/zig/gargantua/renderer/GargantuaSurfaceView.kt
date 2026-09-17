package com.zig.gargantua.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 * Native Android GLSurfaceView hosting the Gargantua OpenGL ES 3.x renderer.
 * Handles interactive observer navigation: 1-finger orbit/pan and 2-finger pinch-to-zoom.
 * Strictly requires GLES 3.x.
 */
class GargantuaSurfaceView(
    context: Context,
    val renderer: GargantuaRenderer = GargantuaRenderer(context)
) : GLSurfaceView(context) {

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor > 0f) {
                renderer.stateHolder.updateState { current ->
                    current.copy(
                        camDist = (current.camDist / scaleFactor).coerceIn(12.0f, 60.0f)
                    )
                }
            }
            return true
        }
    })

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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y

                    renderer.stateHolder.updateState { current ->
                        val newAzimuth = (current.camAzimuthDeg - dx * 0.25f) % 360f
                        val newInclination = (current.camInclinationDeg - dy * 0.25f).coerceIn(5.0f, 175.0f)
                        current.copy(
                            camAzimuthDeg = newAzimuth,
                            camInclinationDeg = newInclination
                        )
                    }
                } else if (event.pointerCount > 1) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    /**
     * Resets the observer camera to the cinematic default view.
     */
    fun resetCamera() {
        renderer.stateHolder.updateState {
            it.copy(
                camDist = 24.0f,
                camInclinationDeg = 82.0f,
                camAzimuthDeg = 0.0f
            )
        }
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
