package com.alijafari.red.astronomy.ui.screens

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.alijafari.red.astronomy.BuildConfig
import com.alijafari.red.astronomy.startracker.detection.GrayscaleImage
import com.alijafari.red.astronomy.startracker.fusion.StarTrackerDebugFlags
import com.alijafari.red.astronomy.startracker.fusion.StarTrackerPipeline
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Phase 1 Task 3 — Camera Frame Access prerequisite.
 *
 * Encapsulates an ImageAnalysis use case with STRATEGY_KEEP_ONLY_LATEST backpressure
 * and its own dedicated background executor (not the main executor used for Preview).
 *
 * For now, analyzer only reads and logs (debug builds only, rate-limited to ~1/sec):
 * - imageProxy.format, width, height, imageInfo.timestamp, rotationDegrees
 * - Performs NO pixel processing beyond reading metadata in this phase.
 * - ALWAYS calls imageProxy.close() in finally block to avoid buffer starvation.
 *
 * This class is intentionally inert: no star detection, no OpenCV, no ARCore.
 * Later phases will expand the analyzer to access YUV pixels for plate solving.
 *
 * Clock domain note (Task 3.4):
 * - imageProxy.imageInfo.timestamp is in nanoseconds, from same time base as
 *   SensorEvent.timestamp (elapsedRealtimeNanos / uptimeMillis clock).
 *   According to Android docs: Camera2 TIMESTAMP and SensorEvent timestamp share
 *   CLOCK_BOOTTIME / ELAPSED_REALTIME domain, so difference stays roughly constant
 *   and does not drift wildly. This will be verified on device in later phases.
 *
 * Expected format: YUV_420_888 (35) on most devices.
 * Expected frame rate: depends on device, typically 10-30 fps reaching analyzer with KEEP_ONLY_LATEST,
 *   but we rate-limit logging to 1/sec to avoid spam.
 */
class CameraFrameObserver {

    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "CameraFrameObserver").apply { isDaemon = true }
    }

    @Volatile
    private var lastLogMs: Long = 0L

    // For clock-domain cross-check (Task 3.4): store latest image timestamp
    @Volatile
    var latestImageTimestampNanos: Long = 0L
        private set

    /**
     * G-P0 (W2 PHASE5 applied): star-tracker pipeline consumer. Attached ONLY by the
     * debug field-trial runtime; the feed branch below is gated by the D1 runtime
     * resolution (release: consts, always false -> this whole path is dead code).
     */
    @Volatile
    var starTrackerPipeline: StarTrackerPipeline? = null

    /** G-P0: result sink (probe collector / OrientationProvider recording), off main thread. */
    @Volatile
    var onPipelineResult: ((StarTrackerPipeline.PipelineResult) -> Unit)? = null

    /** G-P0 (2.5 frame capture): raw grayscale frame tap for the debug field trial. */
    @Volatile
    var onRawFrame: ((GrayscaleImage, Long) -> Unit)? = null

    /** Last analyzed frame's rotationDegrees (for offline camera->device frame conversion). */
    @Volatile
    var latestRotationDegrees: Int = 0
        private set

    /** G-P0 (2.5 probe): wall time of the last pipeline.process() call, ms (0 when none). */
    @Volatile
    var latestPipelineProcessMs: Double = 0.0
        private set

    /** W2 drop-not-queue latch: if a frame arrives while the pipeline is busy it is
     *  DROPPED (closed immediately) — never queued; a stale sky frame is worthless. */
    private val pipelineBusy = AtomicBoolean(false)

    @Volatile
    var latestSensorTimestampNanos: Long = 0L
        private set

    private val imageAnalysis: ImageAnalysis by lazy {
        // T3: the property below is a non-null val (type ExecutorService, from a
        // non-null factory), but this Kotlin/camera-core pairing mis-infers it as
        // platform/nullable in the setAnalyzer argument position (the PR#3-era
        // compileDebugKotlin error). Hoisting it into a local val typed as the
        // declared parameter type (java.util.concurrent.Executor) pins the inference
        // with no runtime assertion at all.
        val executor: java.util.concurrent.Executor = backgroundExecutor
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // default
            .build().apply {
                setAnalyzer(executor) { imageProxy: ImageProxy ->
                    try {
                        // Capture metadata for later clock-domain analysis
                        latestImageTimestampNanos = imageProxy.imageInfo.timestamp
                        latestRotationDegrees = imageProxy.imageInfo.rotationDegrees

                        // Rate-limited debug logging (~once per second)
                        val nowMs = System.currentTimeMillis()
                        if (nowMs - lastLogMs >= 1000L) {
                            lastLogMs = nowMs
                            if (BuildConfig.DEBUG) {
                                // Debug-only logging, per Task 3 requirement
                                Log.d(
                                    TAG,
                                    "format=${imageProxy.format} " +
                                            "(${formatName(imageProxy.format)}) " +
                                            "size=${imageProxy.width}x${imageProxy.height} " +
                                            "tsNanos=${imageProxy.imageInfo.timestamp} " +
                                            "rotDeg=${imageProxy.imageInfo.rotationDegrees} " +
                                            "sensorTsDelta=${latestImageTimestampNanos - latestSensorTimestampNanos}"
                                )
                            }
                        }

                        // G-P0 (W2 PHASE5 applied): feed the star-tracker pipeline when the
                        // RUNTIME gate is on. Off main thread (dedicated analyzer executor),
                        // drop-not-queue via the busy latch; release resolves consts (false).
                        val flags = StarTrackerDebugFlags.runtime()
                        if (flags.enabled && flags.pipelineCameraFeed) {
                            val rawTap = onRawFrame
                            if (rawTap != null && pipelineBusy.compareAndSet(false, true)) {
                                try {
                                    val g = toGrayscale(imageProxy)
                                    if (g != null) rawTap(g, imageProxy.imageInfo.timestamp)
                                } finally {
                                    pipelineBusy.set(false)
                                }
                            }
                            val pipeline = starTrackerPipeline
                            val sink = onPipelineResult
                            if (pipeline != null && sink != null && pipelineBusy.compareAndSet(false, true)) {
                                try {
                                    val gray = toGrayscale(imageProxy)
                                    if (gray != null) {
                                        val t0 = System.nanoTime()
                                        val result = pipeline.process(gray)
                                        latestPipelineProcessMs = (System.nanoTime() - t0) / 1e6
                                        sink(result)
                                    }
                                } catch (e: Exception) {
                                    if (BuildConfig.DEBUG) Log.w(TAG, "pipeline exception", e)
                                } finally {
                                    pipelineBusy.set(false)
                                }
                            } // else: frame dropped by design (KEEP_ONLY_LATEST + busy latch)
                        }

                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Analyzer exception", e)
                        }
                    } finally {
                        // ALWAYS close to avoid buffer starvation
                        try {
                            imageProxy.close()
                        } catch (closeEx: Exception) {
                            // Ignore close errors
                        }
                    }
                }
            }
    }

    fun getUseCase(): ImageAnalysis = imageAnalysis

    /**
     * Called from OrientationProvider or other sensor path to cross-check clock domains.
     * Task 4 adds timestampNanos to SkyOrientation, which can be fed here for comparison.
     */
    fun onSensorTimestamp(sensorTimestampNanos: Long) {
        latestSensorTimestampNanos = sensorTimestampNanos
    }

    /**
     * G-P0 (W2 PHASE5 applied): Y plane -> GrayscaleImage (luminance only). Row-stride
     * and pixel-stride aware copy. Returns null for non-YUV_420_888 formats.
     */
    private fun toGrayscale(imageProxy: ImageProxy): GrayscaleImage? {
        if (imageProxy.format != android.graphics.ImageFormat.YUV_420_888) return null
        val plane = imageProxy.planes[0]   // Y
        val w = imageProxy.width
        val h = imageProxy.height
        val data = FloatArray(w * h)
        val buf = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        for (row in 0 until h) {
            val rowBase = row * rowStride
            val outBase = row * w
            for (col in 0 until w) {
                data[outBase + col] = (buf.get(rowBase + col * pixelStride).toInt() and 0xFF).toFloat()
            }
        }
        return GrayscaleImage(w, h, data)
    }

    fun shutdown() {
        try {
            backgroundExecutor.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun formatName(format: Int): String {
        return when (format) {
            android.graphics.ImageFormat.YUV_420_888 -> "YUV_420_888"
            android.graphics.ImageFormat.YUV_422_888 -> "YUV_422_888"
            android.graphics.ImageFormat.YUV_444_888 -> "YUV_444_888"
            android.graphics.ImageFormat.FLEX_RGB_888 -> "FLEX_RGB_888"
            android.graphics.ImageFormat.FLEX_RGBA_8888 -> "FLEX_RGBA_8888"
            android.graphics.ImageFormat.JPEG -> "JPEG"
            android.graphics.ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
            android.graphics.ImageFormat.RAW10 -> "RAW10"
            android.graphics.ImageFormat.RAW12 -> "RAW12"
            else -> "UNKNOWN_$format"
        }
    }

    companion object {
        private const val TAG = "CameraFrameObserver"
    }
}
