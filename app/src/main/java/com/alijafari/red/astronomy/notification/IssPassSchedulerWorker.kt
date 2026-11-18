package com.alijafari.red.astronomy.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class IssPassSchedulerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result = try {
        AstroNotificationManager.refreshSatellitePassSchedulesIfEnabled(context)
        Result.success()
    } catch (e: Exception) {
        Log.w("SatellitePassWorker", "Could not refresh pass alarms", e)
        Result.retry()
    }
}
