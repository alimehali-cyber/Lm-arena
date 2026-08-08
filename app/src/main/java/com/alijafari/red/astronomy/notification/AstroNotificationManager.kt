package com.alijafari.red.astronomy.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.UserLocation
import java.util.concurrent.TimeUnit

object AstroNotificationManager {

    /**
     * Calculates upcoming strictly visible ISS passes for the user's location over next 14 days
     * and sets exact alarms 10 or 30 minutes prior to each pass.
     */
    fun scheduleUpcomingIssPasses(
        context: Context,
        userLocation: UserLocation = UserLocation(),
        leadMinutes: Int = 10
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AstroAlarmReceiver.createChannels(notificationManager)

        val nowMs = System.currentTimeMillis()
        val passes = ISSEngine.predictPasses(
            userLatDeg = userLocation.latitude,
            userLonDeg = userLocation.longitude,
            startTimestampMs = nowMs,
            scanDays = 14,
            visibleOnly = true
        )

        // Filter strict human visibility criteria (Sunlit, observer in darkness, elevation >= 10°)
        val validPasses = passes.filter { pass ->
            pass.isIssSunlitAtMax &&
            pass.sunAltitudeDegAtMax <= -12.0 &&
            pass.maxElevationDeg >= 10.0 &&
            pass.passDurationSec >= 60
        }

        for (pass in validPasses) {
            val alertTimeMs = pass.startTimeMs - leadMinutes * 60 * 1000L
            if (alertTimeMs <= nowMs) continue

            val intent = Intent(context, AstroAlarmReceiver::class.java).apply {
                action = AstroAlarmReceiver.ACTION_TRIGGER_ISS_NOTIFICATION
                putExtra(AstroAlarmReceiver.EXTRA_PASS_TIME_MS, pass.startTimeMs)
                putExtra(AstroAlarmReceiver.EXTRA_MAX_ELEVATION, pass.maxElevationDeg)
                putExtra(AstroAlarmReceiver.EXTRA_START_DIR, getAzimuthCardinal(pass.startAzimuthDeg))
                putExtra(AstroAlarmReceiver.EXTRA_END_DIR, getAzimuthCardinal(pass.endAzimuthDeg))
                putExtra(AstroAlarmReceiver.EXTRA_DURATION_SEC, pass.passDurationSec)
                putExtra(AstroAlarmReceiver.EXTRA_CITY_NAME, userLocation.cityNameFa)
                putExtra(AstroAlarmReceiver.EXTRA_LEAD_MINUTES, leadMinutes)
            }

            val requestCode = (pass.startTimeMs / 1000).toInt() + (leadMinutes * 10)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent)
                }
            } catch (e: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent)
            }
        }

        // Also enqueue WorkManager periodic check
        enqueueIssWorkManager(context)
    }

    /**
     * Schedules a specific notification for an individual satellite pass with a selected lead time (10m, 30m, 1d).
     */
    fun scheduleSpecificPassAlarm(
        context: Context,
        satName: String,
        pass: ISSEngine.ISSPass,
        cityName: String,
        leadMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AstroAlarmReceiver.createChannels(notificationManager)

        val nowMs = System.currentTimeMillis()
        val alertTimeMs = pass.startTimeMs - (leadMinutes * 60 * 1000L)
        if (alertTimeMs <= nowMs) return

        val intent = Intent(context, AstroAlarmReceiver::class.java).apply {
            action = AstroAlarmReceiver.ACTION_TRIGGER_ISS_NOTIFICATION
            putExtra(AstroAlarmReceiver.EXTRA_PASS_TIME_MS, pass.startTimeMs)
            putExtra(AstroAlarmReceiver.EXTRA_MAX_ELEVATION, pass.maxElevationDeg)
            putExtra(AstroAlarmReceiver.EXTRA_START_DIR, getAzimuthCardinal(pass.startAzimuthDeg))
            putExtra(AstroAlarmReceiver.EXTRA_END_DIR, getAzimuthCardinal(pass.endAzimuthDeg))
            putExtra(AstroAlarmReceiver.EXTRA_DURATION_SEC, pass.passDurationSec)
            putExtra(AstroAlarmReceiver.EXTRA_CITY_NAME, cityName)
            putExtra(AstroAlarmReceiver.EXTRA_LEAD_MINUTES, leadMinutes)
        }

        val requestCode = ((pass.startTimeMs / 1000) % 1000000).toInt() + (satName.hashCode() % 1000) + leadMinutes
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alertTimeMs, pendingIntent)
        }
    }

    /**
     * Continuous monitoring for multiple selected satellites' future visible passes.
     */
    fun scheduleMultiSatellitePasses(
        context: Context,
        selectedSatIds: Set<String>,
        userLocation: UserLocation,
        leadMinutes: Int = 10
    ) {
        val satellites = com.alijafari.red.astronomy.astro_engine.SatelliteCatalog.satellites.filter { it.id in selectedSatIds }
        val nowMs = System.currentTimeMillis()

        for (sat in satellites) {
            val passes = ISSEngine.predictPasses(
                userLatDeg = userLocation.latitude,
                userLonDeg = userLocation.longitude,
                startTimestampMs = nowMs,
                tle = sat.defaultTle,
                scanDays = 14,
                visibleOnly = true,
                standardMag = sat.standardMagnitude
            )

            for (pass in passes) {
                scheduleSpecificPassAlarm(
                    context = context,
                    satName = sat.nameFa,
                    pass = pass,
                    cityName = userLocation.cityNameFa,
                    leadMinutes = leadMinutes
                )
            }
        }

        enqueueIssWorkManager(context)
    }

    fun enqueueIssWorkManager(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<IssPassSchedulerWorker>(12, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelIssWorkManager(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private const val WORK_NAME = "iss_pass_scheduler_work"

    /**
     * Schedules a custom observation notification alarm for an individual Celestial Object.
     */
    fun scheduleObjectNotification(
        context: Context,
        obj: CelestialObject,
        targetTimeMs: Long,
        eventTypeFa: String,
        timeStr: String,
        leadTenMinutesBefore: Boolean = true
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AstroAlarmReceiver.createChannels(notificationManager)

        val nowMs = System.currentTimeMillis()

        if (leadTenMinutesBefore) {
            val tenMinTimeMs = targetTimeMs - 10 * 60 * 1000L
            if (tenMinTimeMs > nowMs) {
                scheduleSingleAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    objName = obj.nameFa,
                    eventType = "$eventTypeFa (۱۰ دقیقه دیگر)",
                    timeStr = timeStr,
                    alarmTimeMs = tenMinTimeMs,
                    requestCode = (obj.id.hashCode() + 101)
                )
            }
        }

        if (targetTimeMs > nowMs) {
            scheduleSingleAlarm(
                context = context,
                alarmManager = alarmManager,
                objName = obj.nameFa,
                eventType = eventTypeFa,
                timeStr = timeStr,
                alarmTimeMs = targetTimeMs,
                requestCode = (obj.id.hashCode() + 202)
            )
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        objName: String,
        eventType: String,
        timeStr: String,
        alarmTimeMs: Long,
        requestCode: Int
    ) {
        val intent = Intent(context, AstroAlarmReceiver::class.java).apply {
            action = AstroAlarmReceiver.ACTION_TRIGGER_OBJECT_NOTIFICATION
            putExtra(AstroAlarmReceiver.EXTRA_OBJECT_NAME, objName)
            putExtra(AstroAlarmReceiver.EXTRA_EVENT_TYPE, eventType)
            putExtra(AstroAlarmReceiver.EXTRA_TIME_STR, timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTimeMs, pendingIntent)
        }
    }

    private fun getAzimuthCardinal(azDeg: Double): String {
        val normAz = (azDeg % 360 + 360) % 360
        return when {
            normAz >= 337.5 || normAz < 22.5 -> "شمال"
            normAz < 67.5 -> "شمال‌شرق"
            normAz < 112.5 -> "شرق"
            normAz < 157.5 -> "جنوب‌شرق"
            normAz < 202.5 -> "جنوب"
            normAz < 247.5 -> "جنوب‌غرب"
            normAz < 292.5 -> "غرب"
            else -> "شمال‌غرب"
        }
    }
}
