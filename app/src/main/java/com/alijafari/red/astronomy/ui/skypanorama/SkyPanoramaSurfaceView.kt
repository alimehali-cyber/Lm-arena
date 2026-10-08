package com.alijafari.red.astronomy.ui.skypanorama

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosts the GLES 3.0 panorama inside the Home hero.
 *
 * A [TextureView] is used instead of a `SurfaceView`: a SurfaceView is punched through the window
 * and ignores the Compose `clip(RoundedCornerShape)` applied to the hero, whereas a TextureView is
 * composited as a normal view and honours the rounded clipping.
 *
 * Status is reported on the main thread. [SkyPanoramaStatus.READY] is reported only after the
 * first panorama frame has been swapped, so the legacy sky remains visible until the panorama is
 * actually on screen, which avoids a black or empty flash.
 */
class SkyPanoramaSurfaceView(
    context: Context,
    private val config: SkyPanoramaConfig = SkyPanoramaConfig()
) : TextureView(context), TextureView.SurfaceTextureListener {

    /** Main-thread callback. */
    var onStatusChanged: ((SkyPanoramaStatus) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(status)
        }

    private val appContext: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var renderer: SkyPanoramaRenderer? = null
    private var decodeJob: Job? = null
    private var latestState: SkyPanoramaState? = null
    private var status = SkyPanoramaStatus.LOADING

    init {
        // Transparent until the first frame, so the legacy sky underneath shows through.
        isOpaque = false
        isClickable = false
        isFocusable = false
        surfaceTextureListener = this
    }

    /** Main thread. Only changed states reach the GL thread, so an unchanged sky never redraws. */
    fun updateState(state: SkyPanoramaState) {
        if (latestState == state) return
        latestState = state
        renderer?.setState(state)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        // The listener is invoked only on the GL thread after construction, so `owner` is assigned
        // before any callback can read it.
        lateinit var owner: SkyPanoramaRenderer
        owner = SkyPanoramaRenderer(object : SkyPanoramaRenderer.Listener {
            override fun onGlReady(maxTextureSize: Int) {
                mainHandler.post { startDecode(owner, maxTextureSize) }
            }

            override fun onFrameShown() {
                mainHandler.post { setStatus(SkyPanoramaStatus.READY) }
            }

            override fun onGlFailure(reason: String) {
                Log.w(TAG, "Panorama unavailable: $reason")
                mainHandler.post { setStatus(SkyPanoramaStatus.UNAVAILABLE) }
            }
        })
        renderer = owner
        owner.setConfig(config)
        owner.attach(surface, width, height)
        val pending = latestState
        if (pending != null) owner.setState(pending)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer?.setViewport(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Intentionally empty: the panorama is drawn on demand, not on every SurfaceTexture update.
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // Returning false: the renderer releases the SurfaceTexture on its GL thread, after the EGL
        // surface that uses it has been destroyed.
        stopRenderer()
        setStatus(SkyPanoramaStatus.LOADING)
        return false
    }

    /** Main thread. Cancels pending work and releases GL resources. Safe to call repeatedly. */
    fun release() {
        stopRenderer()
        scope.cancel()
    }

    private fun stopRenderer() {
        decodeJob?.cancel()
        decodeJob = null
        renderer?.detachAndStop()
        renderer = null
    }

    private fun startDecode(owner: SkyPanoramaRenderer, maxTextureSize: Int) {
        if (renderer !== owner) return
        decodeJob?.cancel()
        val tier = SkyPanoramaTextureLoader.chooseTier(config.qualityTier, maxTextureSize)
        decodeJob = scope.launch {
            try {
                val bitmap = SkyPanoramaTextureLoader.decode(appContext, config.assetPath, tier)
                if (renderer === owner) {
                    owner.uploadTexture(bitmap)
                } else {
                    bitmap.recycle()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Panorama texture unavailable: ${e.message}")
                setStatus(SkyPanoramaStatus.UNAVAILABLE)
            }
        }
    }

    private fun setStatus(next: SkyPanoramaStatus) {
        if (status == next) return
        status = next
        onStatusChanged?.invoke(next)
    }

    private companion object {
        const val TAG = "SkyPanoramaView"
    }
}
