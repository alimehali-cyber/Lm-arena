package com.zig.chal.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView

/** Android SurfaceView with the same surface and touch callback contract as the XAPK. */
class XapkNativeSurfaceView(
    context: Context,
    private val xapkRenderer: XapkChalRenderer
) : SurfaceView(context), SurfaceHolder.Callback {
    private var pinchBaseDistance = 100.0f
    private var cumulativePinchScale = 1.0f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean = true

        override fun onScroll(
            first: MotionEvent?,
            current: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            xapkRenderer.onDrag(distanceX, distanceY, width, height)
            return true
        }
    })

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                pinchBaseDistance = xapkRenderer.params.zoom.toFloat()
                cumulativePinchScale = 1.0f
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                cumulativePinchScale *= detector.scaleFactor
                xapkRenderer.onPinch(pinchBaseDistance, cumulativePinchScale)
                return true
            }
        }
    )

    init {
        holder.setFormat(PixelFormat.OPAQUE)
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) gestureDetector.onTouchEvent(event)
        // Like the XAPK, taps do not toggle an overlay; one-finger scroll and two-finger scale are
        // the only scene gestures handled by the SurfaceView.
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        xapkRenderer.onSurfaceCreated(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        xapkRenderer.onSurfaceChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        xapkRenderer.onSurfaceDestroyed()
    }
}
