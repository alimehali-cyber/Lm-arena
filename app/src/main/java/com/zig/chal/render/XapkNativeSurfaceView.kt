package com.zig.chal.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.hypot

/** Android SurfaceView with the same surface and touch callback contract as the XAPK. */
class XapkNativeSurfaceView(
    context: Context,
    private val xapkRenderer: XapkChalRenderer
) : SurfaceView(context), SurfaceHolder.Callback {
    private var pinchBaseDistance = 100.0f
    private var cumulativePinchScale = 1.0f

    /** Fired on the UI thread when a tap (not a drag) lands, used to toggle the overlay chrome. */
    var onTap: (() -> Unit)? = null

    private var downX = 0f
    private var downY = 0f
    private var touchMoved = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean = true

        override fun onScroll(
            first: MotionEvent?,
            current: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (hypot(distanceX, distanceY) > TAP_SLOP_PX) touchMoved = true
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
                touchMoved = true
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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                touchMoved = false
            }
            MotionEvent.ACTION_UP -> {
                if (!touchMoved && hypot(event.x - downX, event.y - downY) <= TAP_SLOP_PX) {
                    onTap?.invoke()
                }
            }
        }
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) gestureDetector.onTouchEvent(event)
        // One-finger scroll, two-finger scale, and a tap to toggle the chrome: the same gesture set
        // the GLES surface accepts, so both backends feel identical to the user.
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

    private companion object {
        /** Movement beyond this many pixels is a drag, not a tap (matches ViewConfiguration). */
        const val TAP_SLOP_PX = 12f
    }
}
