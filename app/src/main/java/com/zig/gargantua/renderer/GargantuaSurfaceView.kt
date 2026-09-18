package com.zig.gargantua.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.*

/**
 * Native Android GLSurfaceView hosting the Gargantua OpenGL ES 3.x renderer.
 * Handles interactive observer navigation:
 * - 1-finger orbit: rotates azimuth and polar inclination.
 * - 2-finger pan/translate: smoothly shifts observer target along camera view plane.
 * - 2-finger pinch: zooms observer distance bounded in [12M, 60M].
 * - Seamless pointer transitions eliminating movement jitter.
 * Strictly requires GLES 3.x.
 */
class GargantuaSurfaceView(
    context: Context,
    val renderer: GargantuaRenderer = GargantuaRenderer(context)
) : GLSurfaceView(context) {

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
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

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    lastFocusX = (event.getX(0) + event.getX(1)) * 0.5f
                    lastFocusY = (event.getY(0) + event.getY(1)) * 0.5f
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y

                    renderer.stateHolder.updateState { current ->
                        var newAzimuth = (current.camAzimuthDeg - dx * 0.35f) % 360.0f
                        if (newAzimuth < 0.0f) newAzimuth += 360.0f
                        val newInclination = (current.camInclinationDeg - dy * 0.35f).coerceIn(5.0f, 175.0f)
                        current.copy(
                            camAzimuthDeg = newAzimuth,
                            camInclinationDeg = newInclination
                        )
                    }
                } else if (event.pointerCount >= 2) {
                    val focusX = (event.getX(0) + event.getX(1)) * 0.5f
                    val focusY = (event.getY(0) + event.getY(1)) * 0.5f
                    val dFocusX = focusX - lastFocusX
                    val dFocusY = focusY - lastFocusY
                    lastFocusX = focusX
                    lastFocusY = focusY

                    // Two-finger camera translation/pan
                    renderer.stateHolder.updateState { current ->
                        val inclRad = Math.toRadians(current.camInclinationDeg.toDouble())
                        val azRad = Math.toRadians(current.camAzimuthDeg.toDouble())

                        val rX = -sin(azRad)
                        val rY = cos(azRad)

                        val upX = -cos(inclRad) * cos(azRad)
                        val upY = -cos(inclRad) * sin(azRad)
                        val upZ = sin(inclRad)

                        val viewDim = max(100, min(width, height))
                        val panFactor = (current.camDist / viewDim) * 0.8f

                        // Translate target opposite to screen drag so scene follows fingers
                        val deltaX = (-rX * dFocusX + upX * dFocusY) * panFactor
                        val deltaY = (-rY * dFocusX + upY * dFocusY) * panFactor
                        val deltaZ = (upZ * dFocusY) * panFactor

                        current.copy(
                            camTargetX = (current.camTargetX + deltaX.toFloat()).coerceIn(-30.0f, 30.0f),
                            camTargetY = (current.camTargetY + deltaY.toFloat()).coerceIn(-30.0f, 30.0f),
                            camTargetZ = (current.camTargetZ + deltaZ.toFloat()).coerceIn(-30.0f, 30.0f)
                        )
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Smooth pointer transition: re-anchor remaining touch point to eliminate jitter
                val liftingIndex = event.actionIndex
                val remainingIndex = if (liftingIndex == 0) 1 else 0
                if (remainingIndex < event.pointerCount) {
                    lastTouchX = event.getX(remainingIndex)
                    lastTouchY = event.getY(remainingIndex)
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
                camDist = 32.0f,
                camInclinationDeg = 80.0f,
                camAzimuthDeg = 0.0f,
                camTargetX = 0.0f,
                camTargetY = 0.0f,
                camTargetZ = 0.0f
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
