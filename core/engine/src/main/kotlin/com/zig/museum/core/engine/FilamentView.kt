package com.zig.museum.core.engine

import android.view.Surface
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.filament.android.UiHelper

/**
 * FilamentView — Compose surface that hosts Filament view and survives configuration changes
 * Properly uses UiHelper to manage SurfaceView lifecycle and swap chain
 */
@Composable
fun FilamentView(
    objectId: String,
    modifier: Modifier = Modifier,
    tier: Int = 0,
    sunState: SunState = SunState(),
    onSurfaceReady: (() -> Unit)? = null,
    onCameraChange: ((CameraState) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current.applicationContext
    val engine = remember { InspectorEngine.getInstance() }

    // UiHelper for managing SurfaceView
    val uiHelper = remember {
        UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(surface: Surface) {
                    engine.createSwapChain(surface)
                    // Don't set viewport here with hashCode (was bug causing black screen)
                    // Viewport will be set correctly in onResized
                    onSurfaceReady?.invoke()
                }

                override fun onDetachedFromSurface() {
                    engine.destroySwapChain()
                }

                override fun onResized(width: Int, height: Int) {
                    if (width > 0 && height > 0) {
                        engine.setViewport(width, height)
                    }
                }
            }
        }
    }

    // No gesture handling here - handled by parent SpaceMuseumViewerScreen to avoid double orbit
    // Opaque SurfaceView with black background so real 3D is visible, not blue screen
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.setFormat(android.graphics.PixelFormat.OPAQUE)
                setZOrderOnTop(false)
                setBackgroundColor(android.graphics.Color.BLACK)
                uiHelper.attachTo(this)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { _ -> }
    )

    DisposableEffect(lifecycleOwner, objectId, tier) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    engine.startFrameLoop()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    engine.stopFrameLoop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    engine.destroySwapChain()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Load object when id or tier changes. Foundational Rebuild Phase 1.1: pass the real
        // Android Context through so loadEllipsoidObject() can load the one offline-compiled
        // m1SurfaceLit.filamat + its placeholder albedo/normal assets for the Phase 1 object
        // (earth) -- see InspectorEngine.loadEllipsoidObject()'s Phase 1 material branch.
        engine.loadEllipsoidObject(objectId, tier, context = context)
        engine.startFrameLoop()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.stopFrameLoop()
            // Don't release object here, keep for quick switch, release on destroy
            try {
                uiHelper.detach()
            } catch (e: Exception) {
            }
        }
    }
}
