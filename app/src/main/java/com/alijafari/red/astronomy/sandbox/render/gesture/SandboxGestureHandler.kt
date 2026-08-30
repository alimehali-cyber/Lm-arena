package com.alijafari.red.astronomy.sandbox.render.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.alijafari.red.astronomy.sandbox.render.camera.SandboxCamera
import kotlin.math.abs

/**
 * Gesture processor for 3D orbital camera interactions.
 * Handles 1-finger orbit, 2-finger pan, 2-finger pinch zoom, and single tap picking.
 */
class SandboxGestureHandler(
    context: Context,
    private val camera: SandboxCamera,
    private val onSingleTap: (Float, Float) -> Unit,
    private val onDoubleTap: () -> Unit
) {
    private var lastTouchX = 0.0f
    private var lastTouchY = 0.0f
    private var isTwoFingerPan = false
    private var lastTwoFingerMidX = 0.0f
    private var lastTwoFingerMidY = 0.0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor > 0.01f && scaleFactor < 100.0f) {
                // Invert scale factor so pinch-in zooms out, pinch-out zooms in
                camera.zoomByMultiplier(1.0f / scaleFactor)
            }
            return true
        }
    })

    private val tapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTap(e.x, e.y)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap()
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        tapDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isTwoFingerPan = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isTwoFingerPan = true
                    lastTwoFingerMidX = (event.getX(0) + event.getX(1)) * 0.5f
                    lastTwoFingerMidY = (event.getY(0) + event.getY(1)) * 0.5f
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !scaleDetector.isInProgress && !isTwoFingerPan) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    // Orbit sensitivity (degrees per pixel)
                    val orbitSensitivity = 0.35f
                    camera.orbit(
                        deltaYawDeg = dx * orbitSensitivity,
                        deltaPitchDeg = dy * orbitSensitivity
                    )

                    lastTouchX = event.x
                    lastTouchY = event.y
                } else if (event.pointerCount == 2 && isTwoFingerPan) {
                    val midX = (event.getX(0) + event.getX(1)) * 0.5f
                    val midY = (event.getY(0) + event.getY(1)) * 0.5f

                    val dx = midX - lastTwoFingerMidX
                    val dy = midY - lastTwoFingerMidY

                    if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                        camera.pan(dx, dy)
                    }

                    lastTwoFingerMidX = midX
                    lastTwoFingerMidY = midY
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) {
                    isTwoFingerPan = false
                    // Reset single finger anchor
                    val remainingIdx = if (event.actionIndex == 0) 1 else 0
                    if (remainingIdx < event.pointerCount) {
                        lastTouchX = event.getX(remainingIdx)
                        lastTouchY = event.getY(remainingIdx)
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isTwoFingerPan = false
            }
        }

        return true
    }
}
