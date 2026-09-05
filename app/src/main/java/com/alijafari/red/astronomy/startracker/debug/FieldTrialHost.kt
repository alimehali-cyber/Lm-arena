package com.alijafari.red.astronomy.startracker.debug

import android.content.Context
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.alijafari.red.astronomy.BuildConfig
import com.alijafari.red.astronomy.astro_engine.ARProjectionEngine
import com.alijafari.red.astronomy.astro_engine.OrientationProvider

/**
 * G-pass (2026-09-05): the seam hosting the DEBUG-ONLY field-trial guide on the AR
 * screen (retires the D-pass overlay). This object + interface is the ONLY
 * main-source footprint; the guide lives in the debug source set
 * (app/src/debug/java/com/alijafari/red/astronomy/fieldtrial/GuideHost.kt), which is
 * not compiled into release at all (CI dex-inspects the fieldtrial package).
 *
 * Release-unreachability: the only call sites are guarded by BuildConfig.DEBUG
 * (compile-time false in release -> eliminated); the guide class loads only via
 * reflection inside that guard; a stray call no-ops (runCatching -> null -> hidden).
 */
interface FieldTrialGuide {
    @Composable fun Content()
}

object FieldTrialHost {

    /** Everything the guide may read from the AR screen (read-only). */
    class Access(
        val context: Context,
        val orientationProvider: OrientationProvider
    )

    /**
     * The live camera frame observer, handed over by the AR screen (debug only) so the
     * guide's tracker can bind the analysis use case. Typed with the main-source class.
     */
    @Volatile var observer: com.alijafari.red.astronomy.ui.screens.CameraFrameObserver? = null

    /** Live projection-frame snapshot, written by the AR screen, read by the guide. */
    class FrameState(
        val azimuthDeg: Double,
        val altitudeDeg: Double,
        val rollDeg: Double,
        val rotationMatrix: FloatArray?,   // R_true (device->world) when sensor active
        val sensorActive: Boolean,
        val zoomFactor: Float,
        val intrinsics: ARProjectionEngine.CameraIntrinsics?,
        val sensorToViewValues: FloatArray?, // android.graphics.Matrix 9 values, or null
        val displayRotationDegrees: Int,
        val gps: Location?
    )

    @Volatile private var guideImpl: FieldTrialGuide? = null
    @Volatile private var accessRef: Access? = null

    @Volatile var frame: FrameState? = null

    val active = mutableStateOf(false)

    fun access(): Access? = accessRef

    fun open(context: Context, orientationProvider: OrientationProvider) {
        if (!BuildConfig.DEBUG) return
        accessRef = Access(context, orientationProvider)
        if (guideImpl == null) {
            guideImpl = runCatching {
                Class.forName("com.alijafari.red.astronomy.fieldtrial.GuideHost")
                    .kotlin.objectInstance as FieldTrialGuide
            }.onFailure {
                android.util.Log.e("FieldTrialHost", "guide failed to load (release build or missing debug class)", it)
            }.getOrNull()
        }
        active.value = guideImpl != null
    }

    fun close() { active.value = false }

    /** Hosted by CompassARScreen under `if (BuildConfig.DEBUG && FieldTrialHost.active.value)`. */
    @Composable fun HostContent() { guideImpl?.Content() }
}
