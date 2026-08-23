package com.alijafari.red.astronomy.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alijafari.red.astronomy.domain.UserLocation

class IssPassSchedulerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        try {
            AstroNotificationManager.refreshSatellitePassSchedulesIfEnabled(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.success()
    }
}
