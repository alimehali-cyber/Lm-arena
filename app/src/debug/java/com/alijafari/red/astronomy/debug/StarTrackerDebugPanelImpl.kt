package com.alijafari.red.astronomy.debug

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alijafari.red.astronomy.astro_engine.ARProjectionEngine
import com.alijafari.red.astronomy.startracker.calibration.DistortionModelSource
import com.alijafari.red.astronomy.startracker.calibration.HardwareDistortionReader
import com.alijafari.red.astronomy.startracker.debug.StarTrackerDebugHost
import com.alijafari.red.astronomy.startracker.fusion.StarTrackerConfig
import com.alijafari.red.astronomy.startracker.fusion.StarTrackerDebugFlags
import java.util.ArrayDeque

/**
 * D2 (debug-diagnostics pass, 2026-09-04): the ONE debug-only overlay. Lives in the
 * DEBUG SOURCE SET — this class is not compiled into release builds at all (CI asserts
 * its absence from the release APK's dex, D4). Opened by a long-press on the AR screen
 * (see CompassARScreen), hosted via StarTrackerDebugHost.
 *
 * UNEXECUTED on a device as of this commit (no phone in this environment); compile +
 * dex-exclusion are CI-verified. First real execution = the device trial.
 *
 * Content is a READ-ONLY diagnostics display. The ONLY interactive elements are the
 * four D1 runtime flag overrides (SharedPreferences) — the declination is shown as
 * text and can never be toggled here (it is applied at the attitude source; Z-V3
 * tombstone). Tracker fields appear as n/a until the W2 adapters are applied in a
 * future pass; with the tracker unwired there is no pipeline state to read.
 */
object StarTrackerDebugPanelImpl : StarTrackerDebugPanel {

    private const val PREFS = "startracker_debug_flags"

    @Composable
    override fun Content() {
        val access = StarTrackerDebugHost.access() ?: return
        val context = access.context

        val prefs = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
        var resolution by remember {
            mutableStateOf(StarTrackerDebugFlags.resolve({ prefs.getString(it) }))
        }
        val intrinsics = remember(context) { runCatching { ARProjectionEngine.getCameraIntrinsics(context) }.getOrNull() }
        val distortion = remember(context) { readDistortion(context) }
        val distortionTier = remember(distortion) {
            HardwareDistortionReader.selectDistortionModel(distortion, null).second
        }

        // Sensor rate estimate: collect the dedicated timestamp flow and measure.
        val sensor = remember { SensorRateEstimator() }
        LaunchedEffect(Unit) {
            access.orientationProvider.sensorTimestampNanos.collect { sensor.onTimestamp(it) }
        }

        Dialog(onDismissRequest = { StarTrackerDebugHost.close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xF0101018)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("StarTracker diagnostics", color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Button(onClick = { StarTrackerDebugHost.close() }) { Text("Close") }
                    }
                    Text("debug build only — long-press the AR view to reopen", color = Color(0xFF9AA4B2), fontSize = 11.sp)

                    Section("Attitude / sensors (read-only)") {
                        DiagRow("Applied declination", "%.2f°".format(access.orientationProvider.appliedDeclinationDeg) + "  (applied at source — read-only, never toggled here)")
                        DiagRow("Sensor rate (measured)", if (sensor.hz.isNaN()) "measuring…" else "%.1f Hz".format(sensor.hz))
                        DiagRow("sensorTsDelta (mean)", if (sensor.meanDeltaMs.isNaN()) "measuring…" else "%.1f ms".format(sensor.meanDeltaMs))
                    }

                    Section("Camera intrinsics (read-only)") {
                        DiagRow("Tier", intrinsics?.source?.name ?: "unavailable")
                        DiagRow("fx / fy", intrinsics?.let { "%.1f / %.1f".format(it.fx, it.fy) } ?: "—")
                        DiagRow("cx / cy", intrinsics?.let { "%.1f / %.1f".format(it.cx, it.cy) } ?: "—")
                        DiagRow("FOV fallback", "%.1f° (vertical, when no device intrinsics)".format(63.5))
                    }

                    Section("Distortion (read-only — HardwareDistortionReader, first live use)") {
                        DiagRow("Tier", when (distortionTier) {
                            DistortionModelSource.HARDWARE_DISTORTION -> "HARDWARE_DISTORTION"
                            DistortionModelSource.SELF_CALIBRATED -> "SELF_CALIBRATED"
                            DistortionModelSource.NONE -> "NONE (identity) → T4(b) radial allowance in verifier"
                        })
                        DiagRow("k1 / k2", distortion?.let { "%.5f / %.5f".format(it.k1, it.k2) } ?: "— (no camera2 distortion metadata / API<30)")
                    }

                    Section("Star tracker (read-only)") {
                        DiagRow("State", "NOT WIRED — W2 adapters unapplied; pipeline has no camera feed in this build")
                        DiagRow("LockConfidence", "n/a")
                        DiagRow("matched / detected", "n/a")
                        DiagRow("solve ms", "n/a")
                        DiagRow("acquisition discrepancy", "n/a deg")
                        DiagRow("last FailureReason", "n/a")
                    }

                    Section("Runtime flag overrides (D1 — persist in SharedPreferences; release reads consts)") {
                        FlagRow("ENABLED", StarTrackerConfig.ENABLED, resolution.enabled) { v ->
                            prefs.edit().putString(StarTrackerDebugFlags.KEY_ENABLED, if (v) "true" else "false").apply()
                            resolution = StarTrackerDebugFlags.resolve({ prefs.getString(it) })
                        }
                        FlagRow("PIPELINE_CAMERA_FEED (analysis gate)", StarTrackerConfig.PIPELINE_CAMERA_FEED, resolution.pipelineCameraFeed) { v ->
                            prefs.edit().putString(StarTrackerDebugFlags.KEY_PIPELINE_CAMERA_FEED, if (v) "true" else "false").apply()
                            resolution = StarTrackerDebugFlags.resolve({ prefs.getString(it) })
                        }
                        FlagRow("TRACKER_TO_ORIENTATION_PHASE6", StarTrackerConfig.TRACKER_TO_ORIENTATION_PHASE6, resolution.trackerToOrientationPhase6) { v ->
                            prefs.edit().putString(StarTrackerDebugFlags.KEY_TRACKER_TO_ORIENTATION_PHASE6, if (v) "true" else "false").apply()
                            resolution = StarTrackerDebugFlags.resolve({ prefs.getString(it) })
                        }
                        FlagRow("PROJECTION_SELF_CALIBRATED_PHASE7", StarTrackerConfig.PROJECTION_SELF_CALIBRATED_PHASE7, resolution.projectionSelfCalibratedPhase7) { v ->
                            prefs.edit().putString(StarTrackerDebugFlags.KEY_PROJECTION_SELF_CALIBRATED_PHASE7, if (v) "true" else "false").apply()
                            resolution = StarTrackerDebugFlags.resolve({ prefs.getString(it) })
                        }
                        Text(
                            "Note: the W2 adapters that would consume these flags are not applied in this build, " +
                                "so toggles persist + display but do not yet change runtime wiring. Flag use begins " +
                                "with the PHASE5/6/7 adapter pass.",
                            color = Color(0xFF9AA4B2), fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    /** Reads device distortion metadata directly (the reader's first live call path). */
    private fun readDistortion(context: Context): com.alijafari.red.astronomy.startracker.calibration.DistortionModel? =
        runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
            val id = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull() ?: return null
            HardwareDistortionReader.read(cm.getCameraCharacteristics(id))
        }.getOrNull()

    /** Rolling-window sensor rate estimator over the dedicated timestamp flow. */
    private class SensorRateEstimator(window: Int = 32) {
        private val deltas = ArrayDeque<Long>()
        private var last: Long = 0L
        var hz by mutableStateOf(Double.NaN)
        var meanDeltaMs by mutableStateOf(Double.NaN)
        fun onTimestamp(ts: Long) {
            if (ts <= 0L) return
            if (last != 0L && ts > last) {
                deltas.addLast(ts - last)
                if (deltas.size > window) deltas.removeFirst()
                if (deltas.size >= 4) {
                    var sum = 0.0
                    for (d in deltas) sum += d
                    meanDeltaMs = sum / deltas.size / 1e6
                    hz = 1e3 / meanDeltaMs
                }
            }
            last = ts
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Color(0xFF8AB4F8), fontSize = 13.sp)
    Surface(color = Color(0x14FFFFFF), shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color(0xFFC7CDD6), fontSize = 12.sp, modifier = Modifier.width(170.dp))
        Text(value, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun FlagRow(label: String, constDefault: Boolean, resolved: Boolean, onToggle: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color(0xFFC7CDD6), fontSize = 12.sp)
            Text("const=$constDefault  resolved=$resolved", color = Color(0xFF9AA4B2), fontSize = 10.sp)
        }
        Switch(checked = resolved, onCheckedChange = onToggle)
    }
}
