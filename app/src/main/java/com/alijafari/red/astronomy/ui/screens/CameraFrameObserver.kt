package com.alijafari.red.astronomy.ui.screens

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.alijafari.red.astronomy.BuildConfig
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

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

    @Volatile
    var latestSensorTimestampNanos: Long = 0L
        private set

    private val imageAnalysis: ImageAnalysis by lazy {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // default
            .build().apply {
                setAnalyzer(backgroundExecutor) { imageProxy: ImageProxy ->
                    try {
                        // Capture metadata for later clock-domain analysis
                        latestImageTimestampNanos = imageProxy.imageInfo.timestamp

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

                        // NO pixel processing beyond metadata in this phase
                        // Future phases will access imageProxy.planes[0].buffer etc.

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
