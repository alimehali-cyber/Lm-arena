package com.alijafari.red.astronomy.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.alijafari.red.astronomy.data.repository.TleRepository
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker responsible for periodic background synchronization of
 * Two-Line Element (TLE) satellite orbital data.
 */
class TleSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic TLE orbital data synchronization...")
        return try {
            val repository = TleRepository.getInstance(applicationContext)
            val updated = repository.refreshTles()
            if (updated) {
                com.alijafari.red.astronomy.notification.AstroNotificationManager.refreshSatellitePassSchedulesIfEnabled(applicationContext)
            }
            Log.d(TAG, "TLE synchronization complete. Updated: $updated")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing TLE data: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "TleSyncWorker"
        const val PERIODIC_WORK_NAME = "periodic_tle_sync_work"
        const val ONE_TIME_WORK_NAME = "immediate_tle_sync_work"

        /**
         * Schedules recurring 6-hour orbital TLE synchronization with network constraint.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<TleSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Enqueued periodic 6-hour TLE sync work.")
        }

        /**
         * Triggers an immediate one-shot TLE synchronization.
         */
        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<TleSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Enqueued immediate one-shot TLE sync work.")
        }
    }
}
