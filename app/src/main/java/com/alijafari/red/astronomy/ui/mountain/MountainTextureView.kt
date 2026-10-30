package com.alijafari.red.astronomy.ui.mountain

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.TextureView
import java.util.concurrent.atomic.AtomicBoolean

/** In-window transparent, render-on-demand EGL host for the supplied mountain scene. */
class MountainTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    /** The scene, exposed for framing controls and explicit-light APIs. Its init/draw/release
     *  run on the render thread owned below; its setters are synchronised and may be called
     *  from any thread. */
    val scene = MountainScene()

    private var renderer: Renderer? = null
    private var renderScale = 1f

    /** Main thread. Set by [release]; once true, no callback may restart rendering. */
    private var released = false
    private var paused = false

    init {
        // Transparent: the stone shows over the painted ground, its sky shows through.
        isOpaque = false
        isClickable = false
        isFocusable = false
        surfaceTextureListener = this
    }

    /** Astronomical azimuth/altitude; colour & ambient derived automatically by the scene. */
    fun setSun(azimuthDeg: Float, altitudeDeg: Float) {
        scene.setSun(azimuthDeg, altitudeDeg)
        renderer?.requestDraw()
    }

    /** Moon as second light; scale [intensity] by illuminated fraction (≈0.22 = full). */
    fun setMoon(azimuthDeg: Float, altitudeDeg: Float, intensity: Float = 0.22f) {
        scene.setMoon(azimuthDeg, altitudeDeg, intensity)
        renderer?.requestDraw()
    }

    /** Width in world metres and the base row as a fraction of view height. */
    fun setFraming(
        visibleWidthMeters: Float,
        baseFractionFromTop: Float,
    ) {
        scene.visibleWidthMeters = visibleWidthMeters
        scene.baseFractionFromTop = baseFractionFromTop
        renderer?.requestDraw()
    }

    fun setSeason(season: MountainSeason) {
        scene.setSeason(season)
        renderer?.requestDraw()
    }

    /** Main thread. Scale only the mountain's backing buffer, never its layout/framing.
     *  0.7f shades about half as many pixels; 1f restores native resolution. Changing the
     *  scale reuses the EGL context and static mesh. */
    fun setRenderScale(scale: Float) {
        val clamped = clampMountainRenderScale(scale)
        if (renderScale == clamped) return
        renderScale = clamped
        renderer?.setViewport(width, height, renderScale)
    }

    /** Lifecycle, forwarded by the Compose host: resume drawing (and repaint once). The
     *  EGL context is kept across pauses — the static VBOs never re-upload just because the
     *  Activity backgrounded (same intent as the old preserve-on-pause flag). */
    fun onResume() {
        paused = false
        renderer?.setPaused(false)
    }

    /** Lifecycle, forwarded by the Compose host: stop drawing until [onResume]. The GL
     *  thread and context stay alive; [scene]'s state keeps up with the ephemeris meanwhile,
     *  so the first frame after resume is already correct. */
    fun onPause() {
        paused = true
        renderer?.setPaused(true)
    }

    /** Main thread. Releases the scene's GL objects on the render thread, tears the EGL
     *  context down and stops the thread. Terminal for this view instance (the Compose host
     *  creates a fresh view if the layer is composed again). Safe to call repeatedly. */
    fun release() {
        released = true
        stopRenderer()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (released) return
        val owner = Renderer(scene)
        renderer = owner
        owner.attach(surface, width, height, renderScale, paused)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer?.setViewport(width, height, renderScale)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // Returning false: the renderer releases the SurfaceTexture on its GL thread, after
        // the EGL surface that uses it has been destroyed.
        stopRenderer()
        return false
    }

    private fun stopRenderer() {
        renderer?.detachAndStop()
        renderer = null
    }

    /**
     * Owns the GLES 3.0 context and the EGL window surface for one attachment of the view.
     * Every GL and EGL call runs on its dedicated handler thread. The scene draws on demand
     * only: after attach, after a viewport change, after resume, and after a light/framing
     * change posted through [requestDraw] (coalesced, so a scrub that posts faster than the
     * thread renders cannot flood the queue).
     */
    private class Renderer(private val scene: MountainScene) {

        private val thread = HandlerThread("MountainGl").apply { start() }
        private val handler = Handler(thread.looper)
        private val drawPending = AtomicBoolean(false)

        // ---- GL-thread-only state -------------------------------------------------------
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglConfig: EGLConfig? = null
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var surfaceTexture: SurfaceTexture? = null
        private var viewportWidth = 0
        private var viewportHeight = 0
        private var paused = false
        private var failed = false

        /** Creates EGL + the scene's GL resources for [surface], then draws the first frame.
         *  Any state already pushed into the scene (sun/moon/framing set before the surface
         *  existed) is picked up by that frame. */
        fun attach(
            surface: SurfaceTexture,
            width: Int,
            height: Int,
            renderScale: Float,
            initiallyPaused: Boolean,
        ) = handler.post {
            surfaceTexture = surface
            paused = initiallyPaused
            try {
                resizeBuffer(width, height, renderScale)
                initEgl(surface)
                scene.init()
                drawFrame()
            } catch (t: Throwable) {
                Log.w(TAG, "Mountain GL init failed", t)
                failed = true
                releaseSceneQuietly()
                releaseGl()
            }
        }

        fun setViewport(width: Int, height: Int, renderScale: Float) = handler.post {
            if (failed || surfaceTexture == null) return@post
            try {
                resizeBuffer(width, height, renderScale)
                drawFrame()
            } catch (t: Throwable) {
                Log.w(TAG, "Mountain GL resize failed", t)
                failed = true
                releaseSceneQuietly()
                releaseGl()
            }
        }

        /** The EGL drawable AND glViewport must use the scaled size. Reducing just the
         *  viewport would leave a small image in a full-size buffer rather than upscale it.
         *  TextureView resets the default buffer size on layout changes. SurfaceTexture's
         *  GLES contract requires recreating the EGLSurface to guarantee a new default size
         *  takes effect; keep the context, shader and static VBOs alive across that change. */
        private fun resizeBuffer(width: Int, height: Int, renderScale: Float) {
            val surface = surfaceTexture ?: return
            val size = mountainRenderSize(width, height, renderScale)
            val recreateSurface = eglSurface != EGL14.EGL_NO_SURFACE
            if (recreateSurface) {
                check(EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
                    "eglMakeCurrent failed before resize"
                }
                check(EGL14.eglDestroySurface(eglDisplay, eglSurface)) { "eglDestroySurface failed during resize" }
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            surface.setDefaultBufferSize(size.width, size.height)
            viewportWidth = size.width
            viewportHeight = size.height
            if (recreateSurface) createWindowSurface(surface)
        }

        fun setPaused(value: Boolean) = handler.post {
            paused = value
            if (!paused) drawFrame()
        }

        /** Posts one draw; concurrent requests collapse into the queued one. */
        fun requestDraw() {
            if (drawPending.compareAndSet(false, true)) {
                handler.post {
                    drawPending.set(false)
                    drawFrame()
                }
            }
        }

        /** Releases the scene's GL objects while the context is current, then tears down
         *  EGL and stops the thread. The renderer cannot be reused afterwards. */
        fun detachAndStop() {
            handler.post {
                releaseSceneQuietly()
                releaseGl()
                thread.quitSafely()
            }
        }

        private fun drawFrame() {
            if (failed || paused || eglSurface == EGL14.EGL_NO_SURFACE) return
            if (viewportWidth <= 0 || viewportHeight <= 0) return
            try {
                // The scene draws with a transparent clear (draw(clear = true)); it never
                // swaps — the framework surface host swapped on its behalf, and so do we.
                scene.draw(viewportWidth, viewportHeight, clear = true)
                if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                    Log.w(TAG, "eglSwapBuffers failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
                }
            } catch (t: Throwable) {
                // A GL failure must never take Home down: drop the layer and keep the
                // painted landscape underneath (the app's other GL hosts do the same).
                Log.w(TAG, "Mountain draw failed", t)
                failed = true
                releaseSceneQuietly()
                releaseGl()
            }
        }

        private fun initEgl(surface: SurfaceTexture) {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
            eglDisplay = display

            // Same config the original overlay view asked its config chooser for:
            // RGBA8888 for alpha compositing + 16-bit depth (the terrain and boulders
            // must occlude one another correctly, so the depth buffer is not optional).
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            check(
                EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) &&
                    numConfigs[0] > 0 && configs[0] != null
            ) { "No GLES 3.0 EGL config available" }
            val config = configs[0]!!
            eglConfig = config

            val contextAttribs = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed (0x${Integer.toHexString(EGL14.eglGetError())})" }
            eglContext = context
            createWindowSurface(surface)
        }

        /** Connect a new drawable to the existing context; no mesh rebuild/upload. */
        private fun createWindowSurface(surface: SurfaceTexture) {
            val config = checkNotNull(eglConfig) { "Missing EGL config" }
            val windowSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
            check(windowSurface != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }
            eglSurface = windowSurface
            check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }
        }

        /** GL thread. Deletes the scene's programs/buffers; only while a context exists —
         *  with no live context there is nothing of the scene's on the GPU to delete. */
        private fun releaseSceneQuietly() {
            if (eglContext == EGL14.EGL_NO_CONTEXT) return
            try {
                scene.release()
            } catch (t: Throwable) {
                Log.w(TAG, "Mountain GL cleanup failed", t)
            }
        }

        /** GL thread. Destroys the EGL surface/context and releases the SurfaceTexture.
         *  Deliberately no eglTerminate: the display connection is process-wide and the
         *  app's other GL hosts (panorama, AR sky) may be alive on it. */
        private fun releaseGl() {
            val display = eglDisplay
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            }
            surfaceTexture?.release()
            surfaceTexture = null
            eglSurface = EGL14.EGL_NO_SURFACE
            eglContext = EGL14.EGL_NO_CONTEXT
            eglConfig = null
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }

        private companion object {
            /** EGL_OPENGL_ES3_BIT (0x0040), defined here because EGL14 does not expose it. */
            const val EGL_OPENGL_ES3_BIT = 0x0040
            /** EGL_CONTEXT_CLIENT_VERSION (0x3098). */
            const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        }
    }

    private companion object {
        const val TAG = "MountainTextureView"
    }
}
