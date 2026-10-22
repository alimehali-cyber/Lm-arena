package com.zig.avatar

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.provider.Settings
import android.util.AttributeSet
import kotlin.math.abs

/**
 * A drop-in GL surface that plays one character.
 *
 * Isolation rules this class follows:
 *  - it owns its EGL context and its renderer; it does not touch ZIG's renderer,
 *    shaders, camera or GL state outside its own surface
 *  - it never reads or writes app-wide settings
 *  - every public method is safe to call from the UI thread
 *
 * Usage from a host composable/screen:
 *   val view = AvatarView(context).apply { load("avatar.glb") }
 *   view.playClip("Idle_Breathing")
 *   // in onPause/onResume of the host:
 *   view.onHostPause() / view.onHostResume()
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private var renderer: AvatarRenderer? = null
    private var reducedMotion = false
    private var running = false
    private var loaded: AvatarModel? = null

    /**
     * Frames are requested explicitly instead of using RENDERMODE_CONTINUOUSLY: the
     * surface stops drawing as soon as the host pauses, which matters for a character
     * that will live on a home-screen-adjacent widget.
     */
    private val loop = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            requestRender()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        renderMode = RENDERMODE_WHEN_DIRTY
        reducedMotion = readReducedMotion(context)
    }

    /** Loads a GLB from assets and starts the idle loop. */
    fun load(assetName: String = "avatar.glb") {
        val r = AvatarRenderer(context.assets, assetName)
        r.setReducedMotion(reducedMotion)
        r.onLoaded = { m -> loaded = m }
        renderer = r
        setRenderer(r)
    }

    /** Non-null once the GL thread has finished loading the asset. */
    fun currentModel(): AvatarModel? = loaded

    fun playClip(name: String, fadeSeconds: Float = 0.18f) {
        renderer?.playClip(name, fadeSeconds)
    }

    fun oneShot(name: String, fadeSeconds: Float = 0.12f) {
        renderer?.oneShot(name, fadeSeconds)
    }

    fun setSpeed(speed: Float) {
        renderer?.setSpeed(speed.coerceIn(0.25f, 2.5f))
    }

    /** Feed the JSON produced by tools/lipsync.py, or null to stop lip-sync. */
    fun setVisemes(json: String?) {
        renderer?.setVisemes(json)
    }

    /**
     * Re-reads the system animator duration scale. Call from onResume: the user can flip
     * "Remove animations" while the app is backgrounded, and a stale value would keep
     * animating against their setting.
     */
    fun refreshMotionPreference() {
        reducedMotion = readReducedMotion(context)
        renderer?.setReducedMotion(reducedMotion)
        requestRender()
    }

    fun onHostPause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(loop)
        onPause()
    }

    fun onHostResume() {
        refreshMotionPreference()
        onResume()
        startLoop()
    }

    private fun startLoop() {
        if (running) return
        running = true
        Choreographer.getInstance().postFrameCallback(loop)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoop()
    }

    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(loop)
        super.onDetachedFromWindow()
    }

    private fun readReducedMotion(ctx: Context): Boolean {
        val resolver = ctx.contentResolver
        val scale = try {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        } catch (e: Exception) {
            1.0f
        }
        return abs(scale) < 0.01f
    }
}
