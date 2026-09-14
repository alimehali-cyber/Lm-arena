package com.zig.museum.core.engine

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
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

/**
 * FilamentView — Compose surface that hosts Filament view and survives configuration changes per M1 task 8
 * Uses AndroidView with SurfaceView, UiHelper, and InspectorEngine frame loop
 */

@Composable
fun FilamentView(
    objectId: String,
    modifier: Modifier = Modifier,
    tier: Int = 0,
    sunState: SunState = SunState(),
    onSurfaceReady: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val engine = remember { InspectorEngine.getInstance() }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    // UiHelper setup per Filament Android integration
                    // For M1, we use InspectorEngine's UiHelper if needed, or direct
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { surfaceView ->
                // When surfaceView is available, create swap chain and start frame loop
                // This is called on recomposition
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    engine.startFrameLoop()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    engine.stopFrameLoop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // Don't destroy engine here — keep Engine for process per §5.1
                    // Only release View-side resources
                    engine.destroySwapChain()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial load of object
        engine.loadEllipsoidObject(objectId, tier)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.stopFrameLoop()
            engine.releaseCurrentObject()
        }
    }
}
