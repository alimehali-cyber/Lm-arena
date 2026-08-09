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
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.UserLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Centralized Notification Engine for all astronomical notifications across the application:
 * - Satellite visible passes (ISS, Starlink, Hubble, Tiangong, etc.)
 * - Celestial object observation reminders (Moon, Planets, Stars, Deep Sky)
 * - Astronomical events & eclipses
 *
 * All scheduling is backed by [AstroNotificationStore] to guarantee zero object-mismatch,
 * zero duplicate notifications, and accurate background/reboot recovery.
 */
object AstroNotificationManager {

    private const val PREFS_NAME = "astro_notification_prefs_v2"
    private const val KEY_LAST_LAT = "last_notif_lat"
    private const val KEY_LAST_LON = "last_notif_lon"
    private const val WORK_NAME = "iss_pass_scheduler_work"

    /**
     * Schedules a specific notification for an individual satellite pass with a selected lead time.
     * Uses stable satellite ID and pass start time to prevent object mismatch.
     */
    fun scheduleSpecificPassAlarm(
        context: Context,
        satellite: SatelliteItem,
        pass: ISSEngine.ISSPass,
        userLocation: UserLocation,
        leadMinutes: Int
    ) {
        val nowMs = System.currentTimeMillis()
        val triggerTimeMs = pass.startTimeMs - (leadMinutes * 60 * 1000L)
        if (triggerTimeMs <= nowMs) return

        val notifIdStr = "pass_${satellite.id}_${pass.startTimeMs}_${leadMinutes}"
        val intId = abs(notifIdStr.hashCode())

        val startDirFa = getAzimuthCardinalFa(pass.startAzimuthDeg)
        val startDirEn = getAzimuthCardinalEn(pass.startAzimuthDeg)
        val endDirFa = getAzimuthCardinalFa(pass.endAzimuthDeg)
        val endDirEn = getAzimuthCardinalEn(pass.endAzimuthDeg)
        val durMinutes = (pass.passDurationSec / 60).coerceAtLeast(1)

        val formattedTimeFa = formatTimeMs(pass.startTimeMs, isFa = true)
        val formattedTimeEn = formatTimeMs(pass.startTimeMs, isFa = false)

        val leadStrFa = if (leadMinutes == 1440) "۱ روز" else "$leadMinutes دقیقه"
        val leadStrEn = if (leadMinutes == 1440) "1 day" else "$leadMinutes min"

        val titleFa = "🛸 ${satellite.nameFa} — $leadStrFa دیگر!"
        val titleEn = "🛸 ${satellite.nameEn} — in $leadStrEn!"

        val contentFa = "📍 شهر: ${userLocation.cityNameFa} | 🕐 زمان: $formattedTimeFa | 📐 حداکثر ارتفاع: ${pass.maxElevationDeg.toInt()}°\n🧭 مسیر: $startDirFa ➔ $endDirFa | ⏱ مدت: $durMinutes دقیقه"
        val contentEn = "📍 City: ${userLocation.cityNameEn} | 🕐 Time: $formattedTimeEn | 📐 Max Elev: ${pass.maxElevationDeg.toInt()}°\n🧭 Pass: $startDirEn ➔ $endDirEn | ⏱ Duration: $durMinutes min"

        val item = ScheduledNotificationItem(
            id = notifIdStr,
            intNotificationId = intId,
            objectId = satellite.id,
            objectNameEn = satellite.nameEn,
            objectNameFa = satellite.nameFa,
            objectType = "SATELLITE",
            passOrEventId = pass.startTimeMs.toString(),
            triggerTimeMs = triggerTimeMs,
            eventTimeMs = pass.startTimeMs,
            leadMinutes = leadMinutes,
            userLat = userLocation.latitude,
            userLon = userLocation.longitude,
            cityNameFa = userLocation.cityNameFa,
            cityNameEn = userLocation.cityNameEn,
            maxElevationDeg = pass.maxElevationDeg,
            startDirFa = startDirFa,
            startDirEn = startDirEn,
            endDirFa = endDirFa,
            endDirEn = endDirEn,
            durationSec = pass.passDurationSec.toInt(),
            titleEn = titleEn,
            titleFa = titleFa,
            contentEn = contentEn,
            contentFa = contentFa,
            deepLinkRoute = "satellite/${satellite.id}"
        )

        AstroNotificationStore.save(context, item)
        setAlarmWithAndroidSystem(context, item)
    }

    /**
     * Calculates upcoming strictly visible ISS passes for the user's location over next 14 days
     * and schedules exact alarms.
     */
    fun scheduleUpcomingIssPasses(
        context: Context,
        userLocation: UserLocation = UserLocation(),
        leadMinutes: Int = 10
    ) {
        val issSat = SatelliteCatalog.satellites.find { it.id == "iss_zarya" } ?: SatelliteCatalog.satellites.first()
        val nowMs = System.currentTimeMillis()
        val passes = ISSEngine.predictPasses(
            userLatDeg = userLocation.latitude,
            userLonDeg = userLocation.longitude,
            startTimestampMs = nowMs,
            scanDays = 14,
            visibleOnly = true
        )

        // Strict human visibility criteria
        val validPasses = passes.filter { pass ->
            pass.isIssSunlitAtMax &&
            pass.sunAltitudeDegAtMax <= -12.0 &&
            pass.maxElevationDeg >= 10.0 &&
            pass.passDurationSec >= 60
        }

        for (pass in validPasses) {
            scheduleSpecificPassAlarm(
                context = context,
                satellite = issSat,
                pass = pass,
                userLocation = userLocation,
                leadMinutes = leadMinutes
            )
        }

        updateStoredLocation(context, userLocation)
        enqueueIssWorkManager(context)
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
        val satellites = if (selectedSatIds.isEmpty()) {
            SatelliteCatalog.satellites.filter { it.isNakedEyeCandidate }
        } else {
            SatelliteCatalog.satellites.filter { it.id in selectedSatIds }
        }

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

            val validPasses = passes.filter { pass ->
                pass.isIssSunlitAtMax &&
                pass.sunAltitudeDegAtMax <= -12.0 &&
                pass.maxElevationDeg >= 10.0 &&
                pass.passDurationSec >= 60
            }

            for (pass in validPasses) {
                scheduleSpecificPassAlarm(
                    context = context,
                    satellite = sat,
                    pass = pass,
                    userLocation = userLocation,
                    leadMinutes = leadMinutes
                )
            }
        }

        updateStoredLocation(context, userLocation)
        enqueueIssWorkManager(context)
    }

    /**
     * Schedules observation notifications for an individual Celestial Object.
     */
    fun scheduleObjectNotification(
        context: Context,
        obj: CelestialObject,
        targetTimeMs: Long,
        eventTypeFa: String,
        eventTypeEn: String = "Observation Event",
        timeStr: String,
        leadTenMinutesBefore: Boolean = true
    ) {
        val nowMs = System.currentTimeMillis()

        if (leadTenMinutesBefore) {
            val tenMinTimeMs = targetTimeMs - 10 * 60 * 1000L
            if (tenMinTimeMs > nowMs) {
                val idStr = "obs_lead_${obj.id}_$targetTimeMs"
                val item = ScheduledNotificationItem(
                    id = idStr,
                    intNotificationId = abs(idStr.hashCode()),
                    objectId = obj.id,
                    objectNameEn = obj.nameEn,
                    objectNameFa = obj.nameFa,
                    objectType = "CELESTIAL",
                    passOrEventId = targetTimeMs.toString(),
                    triggerTimeMs = tenMinTimeMs,
                    eventTimeMs = targetTimeMs,
                    leadMinutes = 10,
                    userLat = 0.0,
                    userLon = 0.0,
                    cityNameFa = "",
                    cityNameEn = "",
                    titleFa = "🔭 یادآوری رصد: ${obj.nameFa} (۱۰ دقیقه دیگر)",
                    titleEn = "🔭 Observation Reminder: ${obj.nameEn} (in 10 min)",
                    contentFa = "زمان $eventTypeFa ${obj.nameFa} فرا خواهد رسید ($timeStr). شرایط رصدپذیری را بررسی کنید.",
                    contentEn = "Time for $eventTypeEn ${obj.nameEn} is approaching ($timeStr). Check viewing conditions.",
                    deepLinkRoute = "object/${obj.id}"
                )
                AstroNotificationStore.save(context, item)
                setAlarmWithAndroidSystem(context, item)
            }
        }

        if (targetTimeMs > nowMs) {
            val idStr = "obs_exact_${obj.id}_$targetTimeMs"
            val item = ScheduledNotificationItem(
                id = idStr,
                intNotificationId = abs(idStr.hashCode()),
                objectId = obj.id,
                objectNameEn = obj.nameEn,
                objectNameFa = obj.nameFa,
                objectType = "CELESTIAL",
                passOrEventId = targetTimeMs.toString(),
                triggerTimeMs = targetTimeMs,
                eventTimeMs = targetTimeMs,
                leadMinutes = 0,
                userLat = 0.0,
                userLon = 0.0,
                cityNameFa = "",
                cityNameEn = "",
                titleFa = "🔭 آغاز رصد: ${obj.nameFa}",
                titleEn = "🔭 Observation Event: ${obj.nameEn}",
                contentFa = "هم‌اکنون $eventTypeFa ${obj.nameFa} فرا رسیده است ($timeStr).",
                contentEn = "$eventTypeEn for ${obj.nameEn} is occurring now ($timeStr).",
                deepLinkRoute = "object/${obj.id}"
            )
            AstroNotificationStore.save(context, item)
            setAlarmWithAndroidSystem(context, item)
        }
    }

    /**
     * Schedules a notification for an eclipse or major astronomical event.
     */
    fun scheduleEclipseOrEventNotification(
        context: Context,
        eventId: String,
        targetObjectId: String,
        titleFa: String,
        titleEn: String,
        descriptionFa: String,
        descriptionEn: String,
        eventTimeMs: Long,
        leadMinutes: Int = 60
    ) {
        val nowMs = System.currentTimeMillis()
        val triggerTimeMs = eventTimeMs - (leadMinutes * 60 * 1000L)
        if (triggerTimeMs <= nowMs) return

        val idStr = "event_${eventId}_${eventTimeMs}_$leadMinutes"
        val item = ScheduledNotificationItem(
            id = idStr,
            intNotificationId = abs(idStr.hashCode()),
            objectId = targetObjectId,
            objectNameEn = titleEn,
            objectNameFa = titleFa,
            objectType = "ECLIPSE",
            passOrEventId = eventId,
            triggerTimeMs = triggerTimeMs,
            eventTimeMs = eventTimeMs,
            leadMinutes = leadMinutes,
            userLat = 0.0,
            userLon = 0.0,
            cityNameFa = "",
            cityNameEn = "",
            titleFa = titleFa,
            titleEn = titleEn,
            contentFa = descriptionFa,
            contentEn = descriptionEn,
            deepLinkRoute = "event/$eventId"
        )
        AstroNotificationStore.save(context, item)
        setAlarmWithAndroidSystem(context, item)
    }

    /**
     * Called on device boot, app update, timezone change, or app start to restore all active alarms.
     */
    fun rescheduleAllAlarms(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AstroAlarmReceiver.createChannels(notificationManager)

        // 1. Purge expired items
        val nowMs = System.currentTimeMillis()
        AstroNotificationStore.purgeExpired(context, nowMs)

        // 2. Re-register all remaining future alarms
        val remaining = AstroNotificationStore.getAll(context)
        for (item in remaining) {
            setAlarmWithAndroidSystem(context, item)
        }

        // 3. Keep satellite passes fresh if auto alerts are enabled
        val prefs = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
        val isIssAutoAlertEnabled = prefs.getBoolean("iss_auto_alerts_enabled", false) || prefs.getBoolean("auto_satellite_alerts_enabled", false)
        val leadMinutes = prefs.getInt("iss_alert_lead_minutes", 10)
        val cityNameFa = prefs.getString("user_city_name_fa", "نورآباد ممسنی") ?: "نورآباد ممسنی"
        val cityNameEn = prefs.getString("user_city_name_en", "Noorabad Mamasani") ?: "Noorabad Mamasani"
        val lat = prefs.getFloat("user_lat", 30.1132f).toDouble()
        val lon = prefs.getFloat("user_lon", 51.5217f).toDouble()

        if (isIssAutoAlertEnabled) {
            scheduleUpcomingIssPasses(
                context = context,
                userLocation = UserLocation(cityNameFa = cityNameFa, cityNameEn = cityNameEn, latitude = lat, longitude = lon),
                leadMinutes = leadMinutes
            )
        }
    }

    /**
     * Detects meaningful location changes and invalidates old location-sensitive passes.
     */
    fun handleLocationChanged(context: Context, newLocation: UserLocation) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastLat = prefs.getFloat(KEY_LAST_LAT, 999f).toDouble()
        val lastLon = prefs.getFloat(KEY_LAST_LON, 999f).toDouble()

        if (lastLat != 999.0 && lastLon != 999.0) {
            val distKm = calculateDistanceKm(lastLat, lastLon, newLocation.latitude, newLocation.longitude)
            if (distKm > 10.0) {
                // Cancel old satellite passes and recalculate for new location
                val allItems = AstroNotificationStore.getAll(context)
                val satelliteItems = allItems.filter { it.objectType == "SATELLITE" }
                for (item in satelliteItems) {
                    cancelNotification(context, item.id)
                }

                updateStoredLocation(context, newLocation)

                val appPrefs = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
                val isIssAutoAlertEnabled = appPrefs.getBoolean("iss_auto_alerts_enabled", false) || appPrefs.getBoolean("auto_satellite_alerts_enabled", false)
                val leadMinutes = appPrefs.getInt("iss_alert_lead_minutes", 10)
                if (isIssAutoAlertEnabled) {
                    scheduleUpcomingIssPasses(context, newLocation, leadMinutes)
                }
            }
        } else {
            updateStoredLocation(context, newLocation)
        }
    }

    /**
     * Cancels a specific scheduled notification by ID.
     */
    fun cancelNotification(context: Context, notificationIdStr: String) {
        val item = AstroNotificationStore.getById(context, notificationIdStr) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AstroAlarmReceiver::class.java).apply {
            action = AstroAlarmReceiver.ACTION_TRIGGER_NOTIFICATION
            putExtra(AstroAlarmReceiver.EXTRA_NOTIFICATION_ID, item.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.intNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        AstroNotificationStore.remove(context, notificationIdStr)
    }

    /**
     * Cancels all notifications for a specific object ID.
     */
    fun cancelNotificationsForObject(context: Context, objectId: String) {
        val items = AstroNotificationStore.getAll(context).filter { it.objectId == objectId }
        for (item in items) {
            cancelNotification(context, item.id)
        }
    }

    private fun setAlarmWithAndroidSystem(context: Context, item: ScheduledNotificationItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AstroAlarmReceiver::class.java).apply {
            action = AstroAlarmReceiver.ACTION_TRIGGER_NOTIFICATION
            putExtra(AstroAlarmReceiver.EXTRA_NOTIFICATION_ID, item.id)
            putExtra(AstroAlarmReceiver.EXTRA_INT_NOTIFICATION_ID, item.intNotificationId)
            putExtra(AstroAlarmReceiver.EXTRA_OBJECT_ID, item.objectId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.intNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMs = item.triggerTimeMs
        if (triggerMs <= System.currentTimeMillis()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback for devices without exact alarm permission
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } catch (ex: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        }
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

    private fun updateStoredLocation(context: Context, loc: UserLocation) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_LAST_LAT, loc.latitude.toFloat())
            .putFloat(KEY_LAST_LON, loc.longitude.toFloat())
            .apply()
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun formatTimeMs(timeMs: Long, isFa: Boolean): String {
        val locale = if (isFa) Locale("fa") else Locale.ENGLISH
        val sdf = SimpleDateFormat("HH:mm", locale)
        return sdf.format(Date(timeMs))
    }

    private fun getAzimuthCardinalFa(azDeg: Double): String {
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

    private fun getAzimuthCardinalEn(azDeg: Double): String {
        val normAz = (azDeg % 360 + 360) % 360
        return when {
            normAz >= 337.5 || normAz < 22.5 -> "N"
            normAz < 67.5 -> "NE"
            normAz < 112.5 -> "E"
            normAz < 157.5 -> "SE"
            normAz < 202.5 -> "S"
            normAz < 247.5 -> "SW"
            normAz < 292.5 -> "W"
            else -> "NW"
        }
    }
}
