package com.alijafari.red.astronomy.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.domain.UserLocation
import com.alijafari.red.astronomy.notification.AstroNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * WorkManager background worker that automatically fetches and updates the latest
 * ISS (NORAD 25544) Two-Line Element (TLE) orbital elements from CelesTrak.
 *
 * Ensures the SGP4 propagator always has fresh, high-precision orbital elements
 * for live tracking and accurate pass prediction calculations.
 */
class IssTleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting background ISS TLE fetch from CelesTrak (attempt: $runAttemptCount)...")

        try {
            val freshTle = fetchLatestIssTle(applicationContext)

            if (freshTle != null) {
                Log.i(TAG, "Successfully updated ISS TLE for SGP4 propagator: epoch=${freshTle.line1.substring(18, 32).trim()}")

                // Refresh any other tracked satellites in background repository
                try {
                    val repo = TleRepository.getInstance(applicationContext)
                    repo.refreshTles()
                } catch (e: Exception) {
                    Log.w(TAG, "Secondary satellite refresh warning: ${e.message}")
                }

                // If user has ISS notifications enabled, refresh scheduled pass alarms with fresh SGP4 orbit
                reschedulePassesIfEnabled(applicationContext)

                Result.success()
            } else {
                Log.w(TAG, "Failed to parse valid ISS TLE from CelesTrak feeds.")
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during ISS TLE worker execution: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun reschedulePassesIfEnabled(context: Context) {
        try {
            AstroNotificationManager.refreshSatellitePassSchedulesIfEnabled(context)
            Log.i(TAG, "Refreshed satellite pass schedule with updated orbital TLE data.")
        } catch (e: Exception) {
            Log.w(TAG, "Error updating scheduled satellite pass alerts: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "IssTleWorker"
        const val PERIODIC_WORK_NAME = "iss_tle_sync_periodic_work"
        const val ONE_TIME_WORK_NAME = "iss_tle_sync_immediate_work"

        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Fetches latest ISS TLE directly from CelesTrak NORAD GP endpoints.
         * Validates checksums, updates ISSEngine in-memory cache, and persists to Room database.
         */
        suspend fun fetchLatestIssTle(context: Context): TLEData? = withContext(Dispatchers.IO) {
            val urls = listOf(
                "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE",
                "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle"
            )

            for (endpoint in urls) {
                try {
                    val request = Request.Builder()
                        .url(endpoint)
                        .header("User-Agent", "RED-Astronomy/1.0 (Android; ISS-Tracker)")
                        .get()
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.w(TAG, "HTTP ${response.code} from $endpoint")
                            return@use
                        }

                        val body = response.body?.string() ?: return@use
                        val parsed = TleRepository.parseTleFeed(body)
                        val issTle = parsed[25544]

                        if (issTle != null) {
                            // Update ISSEngine live SGP4 cache
                            ISSEngine.cachedTLE = issTle

                            // Persist to Room database & Repository cache
                            val repo = TleRepository.getInstance(context)
                            repo.saveTle(
                                noradId = 25544,
                                name = issTle.name.ifEmpty { "ISS (ZARYA)" },
                                line1 = issTle.line1,
                                line2 = issTle.line2
                            )
                            Log.i(TAG, "Fetched and persisted fresh ISS TLE from $endpoint")
                            return@withContext issTle
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed fetch from $endpoint: ${e.message}")
                }
            }
            null
        }

        /**
         * Schedules periodic background synchronization of ISS TLE data using WorkManager.
         * Recommended interval: 6 hours with unmetered or connected network constraint.
         */
        fun schedulePeriodicSync(context: Context, intervalHours: Long = 6) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<IssTleWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Enqueued periodic $intervalHours-hour ISS TLE WorkManager task.")
        }

        /**
         * Triggers an immediate one-shot ISS TLE synchronization in the background.
         */
        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<IssTleWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Enqueued immediate one-shot ISS TLE WorkManager task.")
        }
    }
}
