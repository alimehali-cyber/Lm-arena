package com.alijafari.red.astronomy.sandbox.render.view

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.alijafari.red.astronomy.sandbox.render.gesture.SandboxGestureHandler
import com.alijafari.red.astronomy.sandbox.render.gl.QualityLevel
import com.alijafari.red.astronomy.sandbox.render.renderer.GravitySandboxRenderer
import com.alijafari.red.astronomy.sandbox.snapshot.DoubleBufferSnapshotManager

/**
 * Dedicated OpenGL ES 3.0 SurfaceView hosting the Gravity Sandbox rendering surface.
 */
@SuppressLint("ViewConstructor")
class GravitySandboxSurfaceView(
    context: Context,
    val snapshotManager: DoubleBufferSnapshotManager,
    qualityLevel: QualityLevel = QualityLevel.HIGH
) : GLSurfaceView(context) {

    val renderer = GravitySandboxRenderer(snapshotManager, qualityLevel)

    private val gestureHandler = SandboxGestureHandler(
        context = context,
        camera = renderer.camera,
        onSingleTap = { x, y ->
            // Queue raycast picking on GL thread or evaluate with latest cached positions
            val selectedBodyId = renderer.pickBodyAt(x, y)
            renderer.onBodySelectedListener?.invoke(selectedBodyId)
        },
        onDoubleTap = {
            renderer.fitAllBodies()
        }
    )

    init {
        // Request OpenGL ES 3.0 Context
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gestureHandler.onTouchEvent(event)
        return handled || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        renderer.destroy()
    }
}
