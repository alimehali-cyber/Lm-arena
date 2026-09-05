package com.alijafari.red.astronomy.fieldtrial

import android.content.Context
import android.location.Location
import com.alijafari.red.astronomy.astro_engine.OrientationProvider
import com.alijafari.red.astronomy.startracker.calibration.CameraProfile
import com.alijafari.red.astronomy.startracker.calibration.HardwareDistortionReader
import com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import com.alijafari.red.astronomy.startracker.detection.GrayscaleImage
import com.alijafari.red.astronomy.startracker.diagnostics.FailureReason
import com.alijafari.red.astronomy.startracker.fusion.StarTrackerDebugFlags
import com.alijafari.red.astronomy.startracker.fusion.StarTrackerPipeline
import com.alijafari.red.astronomy.startracker.solver.LostInSpaceSolver
import com.alijafari.red.astronomy.startracker.solver.Quaternion
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import com.alijafari.red.astronomy.ui.screens.CameraFrameObserver
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * G-P0 (debug source set only): runtime switchgear that makes "Turn on the star
 * tracker" (level L8) real. Wires the APPLIED W2 adapters:
 *
 *   CameraFrameObserver --(Y-plane grayscale, drop-not-queue, off-main)--> StarTrackerPipeline
 *        |-- onPipelineResult --> OrientationProvider.onStarTrackerResult (record-only, OD1)
 *        \-- onPipelineResult --> ProbeCollector (2.5 numbers for the guide)
 *
 * The feed itself lives in CameraFrameObserver (main source) gated by
 * StarTrackerDebugFlags.runtime() — release resolves the consts (all false) and the
 * path is dead code there; this class is not compiled into release at all.
 *
 * Catalog: debug-only asset startracker/hyg_v36_vle6.5_j2000.csv (the extract the
 * capped index was always built from — sidecar in data/startracker/). UNEXECUTED on
 * a device until the field trial.
 */
object StarTrackerRuntime {

    /** One probe sample per processed frame (2.5). */
    data class ProbeSample(
        val epochMs: Long,
        val imageTsNanos: Long,
        val rotationDegrees: Int,
        val detections: Int,
        val matched: Int?,
        val lock: LockConfidence,
        val solveMs: Double,
        val rmsPx: Double,
        val failure: FailureReason?,
        val attitude: Quaternion?,
        /** angle deg between this star attitude and the sensor attitude snapshot, or null */
        val acquisitionDiscrepancyDeg: Double?
    )

    /** Captured raw frame + sidecar (2.5 frame capture). */
    data class CapturedFrame(
        val epochMs: Long,
        val imageTsNanos: Long,
        val rotationDegrees: Int,
        val width: Int,
        val height: Int,
        val gray: FloatArray,
        val gps: Location?,
        val attitudeSnapshot: FloatArray?, // OrientationProvider rotation matrix copy
        val acquisitionDiscrepancyDeg: Double?
    )

    class ProbeCollector {
        @Volatile var startedAtMs: Long = 0
        @Volatile var framesProcessed: Long = 0
        @Volatile var firstFullLockMs: Long = -1
        @Volatile var lastSample: ProbeSample? = null
            private set
        val samples = java.util.concurrent.ConcurrentLinkedDeque<ProbeSample>()

        fun onSample(s: ProbeSample) {
            framesProcessed++
            if (s.lock == LockConfidence.FULL_LOCK && firstFullLockMs < 0 && startedAtMs > 0) {
                firstFullLockMs = s.epochMs - startedAtMs
            }
            lastSample = s
            samples.addLast(s)
            while (samples.size > 600) samples.pollFirst() // ~last minute at 10 fps
        }

        /** Analysis fps over the kept window. */
        fun fps(): Double {
            val arr = samples.toList()
            if (arr.size < 2) return 0.0
            val dt = (arr.last().epochMs - arr.first().epochMs) / 1000.0
            return if (dt > 0) (arr.size - 1) / dt else 0.0
        }
    }

    class State(
        @Volatile var observer: CameraFrameObserver? = null,
        @Volatile var orientationProvider: OrientationProvider? = null,
        val probes: ProbeCollector = ProbeCollector(),
        @Volatile var captures: MutableList<CapturedFrame> = mutableListOf(),
        @Volatile var sensorSnapshot: FloatArray? = null, // latest R_true copy from the UI
        @Volatile var gps: Location? = null,
        @Volatile var gpsUpdatedMs: Long = 0,
        /** catalog the solver actually matched against (L9 green ring uses exactly this) */
        @Volatile var catalogStars: List<CatalogStar> = emptyList()
    )

    val state = State()
    val isOn = AtomicBoolean(false)

    private val buildExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "StarTrackerRuntime").apply { isDaemon = true }
    }

    /** Latest acquisition discrepancy: angle between star attitude and sensor snapshot (deg). */
    fun acquisitionDiscrepancyDeg(star: Quaternion?, snapshot: FloatArray?): Double? {
        if (star == null || snapshot == null || snapshot.size != 9) return null
        // Sensor snapshot maps device->world (v_world = R v_dev). Star attitude maps
        // catalog->camera. Common frame: build the world vector of the camera boresight
        // from the sensor R (boresight in device frame = (0,0,-1), camera looks out the
        // back: +Z_cam = -Z_dev) and compare with the star attitude's boresight in world
        // (catalog +Z rotated by q, treating catalog Z as the boresight axis).
        val r = snapshot
        // boresight world direction from sensor: v_world = R * (0,0,-1)
        val bx = -r[2]; val by = -r[5]; val bz = -r[8]
        // star boresight in catalog frame = +Z; rotate by star attitude (w,x,y,z)
        val qw = star.w; val qx = star.x; val qy = star.y; val qz = star.z
        // rotate (0,0,1): v' = q * v * q^-1 for unit quaternion
        val vx = 2 * (qx * qz + qw * qy)
        val vy = 2 * (qy * qz - qw * qx)
        val vz = 1 - 2 * (qx * qx + qy * qy)
        val dot = (bx * vx + by * vy + bz * vz).coerceIn(-1.0, 1.0)
        return Math.toDegrees(kotlin.math.acos(dot))
    }

    /**
     * Turn the tracker ON: writes the D1 overrides (ENABLED + analysis gate), rebuilds
     * the pipeline from the debug asset catalog on a background thread, and attaches
     * the sinks. The pipeline is created at the FIRST analyzed frame size.
     */
    fun turnOn(
        context: Context,
        observer: CameraFrameObserver,
        orientationProvider: OrientationProvider,
        gps: Location?,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isOn.compareAndSet(false, true)) { onReady(); return }
        val appContext = context.applicationContext
        state.observer = observer
        state.orientationProvider = orientationProvider
        state.gps = gps
        state.gpsUpdatedMs = System.currentTimeMillis()
        state.probes.startedAtMs = System.currentTimeMillis()
        state.probes.framesProcessed = 0
        state.probes.firstFullLockMs = -1

        setFlags(appContext, enabled = true)

        buildExecutor.execute {
            try {
                val csv = appContext.assets.open("startracker/hyg_v36_vle6.5_j2000.csv")
                    .bufferedReader().readText()
                val stars = CatalogIngestor.parse(csv, "HYG_V36_LE6P5")
                state.catalogStars = stars
                val index = QuadPatternIndex.capped(stars)
                val solver = LostInSpaceSolver(index, stars)

                // Distortion from device metadata (T4a reader, first live use); pipeline
                // applies the T4(b) radial allowance itself when this is identity.
                val distortion = readDeviceDistortion(appContext)

                // Pipeline built at default size; re-created once at the first real frame.
                val pipelineHolder = arrayOfNulls<StarTrackerPipeline>(1)
                val sized = AtomicBoolean(false)
                val probes = state.probes

                val sink: (StarTrackerPipeline.PipelineResult) -> Unit = { result ->
                    val provider = state.orientationProvider
                    provider?.onStarTrackerResult(result)
                    val snap = state.sensorSnapshot
                    val disc = acquisitionDiscrepancyDeg(result.attitude, snap)
                    probes.onSample(
                        ProbeSample(
                            epochMs = System.currentTimeMillis(),
                            imageTsNanos = observer.latestImageTimestampNanos,
                            rotationDegrees = observer.latestRotationDegrees,
                            detections = result.numDetections,
                            matched = result.solverDiagnostics?.fullFieldMatched,
                            lock = result.lockConfidence,
                            solveMs = observer.latestPipelineProcessMs,
                            rmsPx = result.rmsErrorPx,
                            failure = result.coordinatorOutput.failureReason,
                            attitude = result.attitude,
                            acquisitionDiscrepancyDeg = disc
                        )
                    )
                    if (result.lockConfidence == LockConfidence.FULL_LOCK) maybeCaptureAtFirstLock()
                }

                val buildAtSize: (Int, Int) -> StarTrackerPipeline = { w, h ->
                    StarTrackerPipeline(
                        solver = solver,
                        cameraProfile = CameraProfile.fallbackDefault(w, h),
                        distortionModel = distortion ?: com.alijafari.red.astronomy.startracker.calibration.DistortionModel.noDistortion()
                    )
                }
                pipelineHolder[0] = buildAtSize(1920, 1080)
                sized.set(false)

                observer.onPipelineResult = sink
                observer.onRawFrame = { img, ts ->
                    if (!sized.getAndSet(true)) {
                        // first frame: rebuild with the real analysis resolution
                        pipelineHolder[0] = buildAtSize(img.width, img.height)
                        observer.starTrackerPipeline = pipelineHolder[0]
                    }
                    // capture taps (auto at first lock handled via sink; on-demand via requestCapture)
                    val n = captureRequests.getAndUpdate { if (it > 0) it - 1 else 0 }
                    if (n > 0) state.captures.add(frameOf(img, ts, observer))
                }
                observer.starTrackerPipeline = pipelineHolder[0]
                onReady()
            } catch (t: Throwable) {
                isOn.set(false)
                onError(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private val captureRequests = java.util.concurrent.atomic.AtomicInteger(0)

    /** Request N raw frames to be captured on the next analyzed frames. */
    fun requestCapture(n: Int) { captureRequests.addAndGet(n) }

    private var firstLockCaptured = false
    private fun maybeCaptureAtFirstLock() {
        if (!firstLockCaptured) {
            firstLockCaptured = true
            captureRequests.addAndGet(3) // 2.5: three raw frames at first lock
        }
    }

    private fun frameOf(img: GrayscaleImage, ts: Long, observer: CameraFrameObserver): CapturedFrame =
        CapturedFrame(
            epochMs = System.currentTimeMillis(), imageTsNanos = ts,
            rotationDegrees = observer.latestRotationDegrees,
            width = img.width, height = img.height,
            gray = img.data.copyOf(),
            gps = state.gps, attitudeSnapshot = state.sensorSnapshot?.copyOf(),
            acquisitionDiscrepancyDeg = state.probes.lastSample?.acquisitionDiscrepancyDeg
        )

    /** Turn OFF: flags down, pipeline detached. Probe data is kept for the report. */
    fun turnOff(context: Context) {
        val obs = state.observer
        obs?.starTrackerPipeline = null
        obs?.onPipelineResult = null
        obs?.onRawFrame = null
        setFlags(context, enabled = false)
        isOn.set(false)
        firstLockCaptured = false
    }

    private fun setFlags(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("startracker_debug_flags", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(StarTrackerDebugFlags.KEY_ENABLED, if (enabled) "true" else "false")
            .putString(StarTrackerDebugFlags.KEY_PIPELINE_CAMERA_FEED, if (enabled) "true" else "false")
            .apply()
        StarTrackerDebugFlags.installOverridesProvider({ prefs.getString(it, null) })
    }

    private fun readDeviceDistortion(context: Context) =
        com.alijafari.red.astronomy.fieldtrial.FieldTrialSupport.readHardwareDistortion(context)

    /** Directory for trial outputs (app-private). */
    fun trialDir(context: Context): File = File(context.filesDir, "fieldtrial").apply { mkdirs() }
}
