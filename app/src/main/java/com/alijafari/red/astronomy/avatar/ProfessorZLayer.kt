package com.alijafari.red.astronomy.avatar

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Transparent `TextureView` hosting the GLES 3.0 [ProfessorZRenderer] on ZIG's Home Sky Scene.
 *
 * Non-clickable (`isClickable = false`, `isFocusable = false`) so every horizontal sky drag and
 * celestial tap gesture passes straight through to `HeroSkyCanvas` without interference.
 */
class ProfessorZSurfaceView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    var onStatusChanged: ((ProfessorZStatus) -> Unit)? = null
        set(value) {
            field = value
            if (!released) value?.invoke(status)
        }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var renderer: ProfessorZRenderer? = null
    private var latestState: ProfessorZSceneState? = null
    private var status = ProfessorZStatus.LOADING
    private var released = false

    init {
        isOpaque = false
        isClickable = false
        isFocusable = false
        surfaceTextureListener = this
    }

    fun updateState(state: ProfessorZSceneState) {
        if (latestState == state) return
        latestState = state
        renderer?.updateSceneState(state)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        lateinit var owner: ProfessorZRenderer
        owner = ProfessorZRenderer(
            appContext = appContext,
            listener = object : ProfessorZRenderer.Listener {
                override fun onFrameShown() {
                    mainHandler.post {
                        if (!released && renderer === owner) {
                            setStatus(ProfessorZStatus.READY)
                        }
                    }
                }

                override fun onGlFailure(reason: String) {
                    mainHandler.post {
                        if (!released && renderer === owner) {
                            renderer?.detachAndStop()
                            renderer = null
                            setStatus(ProfessorZStatus.UNAVAILABLE)
                        }
                    }
                }
            },
        )
        renderer = owner
        owner.attach(surface, width, height)
        latestState?.let { owner.updateSceneState(it) }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer?.setViewport(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderer()
        setStatus(ProfessorZStatus.LOADING)
        return false
    }

    fun release() {
        released = true
        stopRenderer()
    }

    private fun stopRenderer() {
        renderer?.detachAndStop()
        renderer = null
    }

    private fun setStatus(next: ProfessorZStatus) {
        if (released || status == next) return
        status = next
        onStatusChanged?.invoke(next)
    }
}

@Composable
fun ProfessorZLayer(
    state: ProfessorZSceneState,
    modifier: Modifier = Modifier,
    onStatusChanged: (ProfessorZStatus) -> Unit = {},
) {
    AndroidView(
        factory = { context ->
            ProfessorZSurfaceView(context).apply {
                this.onStatusChanged = onStatusChanged
                updateState(state)
            }
        },
        update = { view ->
            view.onStatusChanged = onStatusChanged
            view.updateState(state)
        },
        onRelease = { view -> view.release() },
        modifier = modifier,
    )
}
