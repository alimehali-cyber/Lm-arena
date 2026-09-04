package com.alijafari.red.astronomy.startracker.debug

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.alijafari.red.astronomy.BuildConfig
import com.alijafari.red.astronomy.astro_engine.OrientationProvider

/**
 * D2 (debug-diagnostics pass, 2026-09-04): the seam through which the ONE debug-only
 * Compose overlay is hosted on the AR screen. This object is the ONLY main-source
 * footprint of the debug screen; the screen itself lives in the DEBUG SOURCE SET
 * (app/src/debug/java/com/alijafari/red/astronomy/debug/StarTrackerDebugPanelImpl.kt)
 * and is therefore not compiled into release builds at all (asserted in CI by dex
 * inspection, D4).
 *
 * Release-unreachability argument:
 *  1. The only call sites (CompassARScreen long-press + hosting) are guarded by
 *     `BuildConfig.DEBUG`, a compile-time constant false in release -> eliminated.
 *  2. [open] re-checks `BuildConfig.DEBUG` and the panel class is loaded ONLY via
 *     reflection, so no release variant ever links against the debug class.
 *  3. Even a hypothetical call resolves to a no-op (runCatching -> null -> invisible).
 */
interface StarTrackerDebugPanel {
    @Composable fun Content()
}

object StarTrackerDebugHost {
    private const val TAG = "StarTrackerDebugHost"

    /** Everything the debug panel may read (read-only diagnostics; it never mutates app state). */
    class Access(val context: Context, val orientationProvider: OrientationProvider)

    @Volatile private var panelImpl: StarTrackerDebugPanel? = null
    @Volatile private var accessRef: Access? = null

    val visible = mutableStateOf(false)

    fun access(): Access? = accessRef

    fun open(context: Context, orientationProvider: OrientationProvider) {
        if (!BuildConfig.DEBUG) return
        accessRef = Access(context, orientationProvider)
        if (panelImpl == null) {
            panelImpl = runCatching {
                Class.forName("com.alijafari.red.astronomy.debug.StarTrackerDebugPanelImpl")
                    .kotlin.objectInstance as StarTrackerDebugPanel
            }.onFailure {
                Log.e(TAG, "debug panel failed to load (release build or missing debug class)", it)
            }.getOrNull()
        }
        // Field-test verification aid: logcat filter 'StarTrackerDebugHost' shows every
        // open request and whether the panel resolved. No-op in release (guard above).
        Log.d(TAG, "open requested via ${Throwable().stackTrace.getOrNull(1)?.methodName ?: "?"}; panel=${panelImpl != null}")
        visible.value = panelImpl != null
    }

    fun close() { visible.value = false }

    /** Hosted by CompassARScreen under `if (BuildConfig.DEBUG && visible.value)`. */
    @Composable fun HostContent() { panelImpl?.Content() }
}
