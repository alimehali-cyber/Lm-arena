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
        val prefs = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("iss_auto_alerts_enabled", false)

        if (!isEnabled) {
            return Result.success()
        }

        val leadMinutes = prefs.getInt("iss_alert_lead_minutes", 10)
        val cityNameFa = prefs.getString("user_city_name_fa", "نورآباد ممسنی") ?: "نورآباد ممسنی"
        val cityNameEn = prefs.getString("user_city_name_en", "Noorabad Mamasani") ?: "Noorabad Mamasani"
        val lat = prefs.getFloat("user_lat", 30.1132f).toDouble()
        val lon = prefs.getFloat("user_lon", 51.5217f).toDouble()

        val userLocation = UserLocation(
            cityNameFa = cityNameFa,
            cityNameEn = cityNameEn,
            latitude = lat,
            longitude = lon
        )

        AstroNotificationManager.scheduleUpcomingIssPasses(
            context = context,
            userLocation = userLocation,
            leadMinutes = leadMinutes
        )

        return Result.success()
    }
}
