package com.alijafari.red.astronomy.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.data.repository.TleRepository
import com.alijafari.red.astronomy.data.worker.IssTleWorker
import com.alijafari.red.astronomy.data.worker.TleSyncWorker
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.UserLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Centralized Notification Engine for all astronomical notifications across RED:
 * - Satellite visible passes (ISS, Tiangong, Starlink, Hubble, etc.) maintaining rolling ~7-day schedules
 * - Completely local, offline-capable AlarmManager exact alarms and BroadcastReceiver execution
 * - Zero duplicate notifications and automatic cancellation of obsolete future alarms on update
 * - Automatic reboot and timezone change recovery
 * - Celestial object observation reminders (Moon, Planets, Stars, Deep Sky)
 * - Astronomical events & eclipses
 */
object AstroNotificationManager {

    private const val TAG = "AstroNotifManager"
    private const val PREFS_NAME = "astro_notification_prefs_v2"
    private const val ASTRO_PREFS = "astro_prefs"
    private const val KEY_LAST_LAT = "last_notif_lat"
    private const val KEY_LAST_LON = "last_notif_lon"
    private const val KEY_AUTO_SATELLITE_ALERTS = "auto_satellite_alerts_enabled"
    private const val KEY_ISS_AUTO_ALERTS = "iss_auto_alerts_enabled"
    private const val KEY_MONITORED_SAT_IDS = "monitored_satellite_ids"
    private const val KEY_ALERT_LEAD_MINUTES = "satellite_alert_lead_minutes"
    private const val KEY_ISS_LEAD_MINUTES = "iss_alert_lead_minutes"
    private const val WORK_NAME = "iss_pass_scheduler_work"
    private const val REFRESH_WORK_NAME = "satellite_pass_refresh"
    // Serialize updates, but do not hold the lock during multi-day orbital prediction.
    private val passLock = Any()
    private var rollingRevision = 0L
    const val STARLINK_TRAIN_ALERT_SCAN_DAYS = 2
    private const val STARLINK_TRAIN_ALERT_HORIZON_MS = STARLINK_TRAIN_ALERT_SCAN_DAYS * 24L * 3_600_000L

    /**
     * Builds a ScheduledNotificationItem for an individual satellite pass with a selected lead time.
     * Uses deterministic notification ID based on satellite ID, start timestamp, and lead time.
     */
    fun createScheduledPassItem(
        satellite: SatelliteItem,
        pass: ISSEngine.ISSPass,
        userLocation: UserLocation,
        leadMinutes: Int,
        nowMs: Long = System.currentTimeMillis()
    ): ScheduledNotificationItem? {
        val triggerTimeMs = pass.startTimeMs - (leadMinutes * 60 * 1000L)
        if (triggerTimeMs <= nowMs) return null

        val notifIdStr = "pass_${satellite.id}_${pass.startTimeMs}_${leadMinutes}"
        val intId = (notifIdStr.hashCode() and 0x7FFFFFFF)

        val startDirFa = getAzimuthCardinalFa(pass.startAzimuthDeg)
        val startDirEn = getAzimuthCardinalEn(pass.startAzimuthDeg)
        val endDirFa = getAzimuthCardinalFa(pass.endAzimuthDeg)
        val endDirEn = getAzimuthCardinalEn(pass.endAzimuthDeg)
        val durMinutes = (pass.passDurationSec / 60).coerceAtLeast(1)

        val zone = TimeEngine.resolveTimeZone(userLocation.timezoneId)
        val formattedTimeEn = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone }.format(Date(pass.startTimeMs))
        val formattedTimeFa = TimeEngine.formatPersianNumbers(formattedTimeEn)

        val leadStrFa = if (leadMinutes == 1440) "۱ روز" else "$leadMinutes دقیقه"
        val leadStrEn = if (leadMinutes == 1440) "1 day" else "$leadMinutes min"

        val titleFa = "${satellite.nameFa} — $leadStrFa دیگر!"
        val titleEn = "${satellite.nameEn} — in $leadStrEn!"

        val contentFa = "شهر: ${userLocation.cityNameFa} | زمان: $formattedTimeFa | حداکثر ارتفاع: ${pass.maxElevationDeg.toInt()}°\nمسیر: $startDirFa ➔ $endDirFa | مدت: $durMinutes دقیقه"
        val contentEn = "City: ${userLocation.cityNameEn} | Time: $formattedTimeEn | Max Elev: ${pass.maxElevationDeg.toInt()}°\nPass: $startDirEn ➔ $endDirEn | Duration: $durMinutes min"

        return ScheduledNotificationItem(
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
            titleFa = TimeEngine.formatPersianNumbers(titleFa),
            contentEn = contentEn,
            contentFa = TimeEngine.formatPersianNumbers(contentFa),
            deepLinkRoute = "satellite/${satellite.id}",
            satelliteNoradId = satellite.noradId
        )
    }

    /**
     * Schedules a specific notification for an individual satellite pass with a selected lead time.
     * Prevents duplicate alarms and persists locally.
     */
    fun scheduleSpecificPassAlarm(
        context: Context,
        satellite: SatelliteItem,
        pass: ISSEngine.ISSPass,
        userLocation: UserLocation,
        leadMinutes: Int
    ): Boolean = synchronized(passLock) {
        val item = createScheduledPassItem(satellite, pass, userLocation, leadMinutes)?.copy(manualPass = true)
            ?: return@synchronized false
        // A manual lead replaces rolling coverage instead of sending two notifications for one rise.
        removePassAlarms(context, AstroNotificationStore.getAll(context).filter { it.coversSatellitePass(satellite, pass.startTimeMs) })
        AstroNotificationStore.save(context, item)
        setAlarmWithAndroidSystem(context, item)
        true
    }

    fun cancelSpecificPassAlarm(context: Context, satellite: SatelliteItem, riseMs: Long) = synchronized(passLock) {
        removePassAlarms(context, AstroNotificationStore.getAll(context).filter { it.manualPass && it.coversSatellitePass(satellite, riseMs) })
    }

    /** Persist before dispatch, so leaving the tab or a process restart cannot undo Save. */
    fun configureSatelliteMonitoring(context: Context, enabled: Boolean, userLocation: UserLocation,
        selectedIds: Set<String>, leadMinutes: Int) = synchronized(passLock) {
        rollingRevision++
        val actuallyEnabled = enabled && selectedIds.isNotEmpty()
        savePassMonitoringPreferences(context, userLocation, selectedIds, leadMinutes, actuallyEnabled)
        updateStoredLocation(context, userLocation)
        removePassAlarms(context, AstroNotificationStore.getAll(context).filter { it.objectType == "SATELLITE" && !it.manualPass })
        if (actuallyEnabled) {
            enqueueIssWorkManager(context)
            requestRollingPassRefresh(context)
        } else cancelIssWorkManager(context)
    }

    private fun removePassAlarms(context: Context, items: List<ScheduledNotificationItem>) {
        items.forEach { cancelAlarmOnly(context, it) }
        AstroNotificationStore.removeByIds(context, items.map { it.id }.toSet())
    }

    /**
     * Schedules rolling ~7-day visible passes for the International Space Station (ISS).
     */
    fun scheduleUpcomingIssPasses(
        context: Context,
        userLocation: UserLocation = UserLocation(),
        leadMinutes: Int = 10
    ) {
        scheduleRollingSatellitePasses(
            context = context,
            userLocation = userLocation,
            selectedSatIds = setOf("iss_zarya"),
            leadMinutes = leadMinutes
        )
    }

    /**
     * Backward-compatible alias for multi-satellite scheduling.
     */
    fun scheduleMultiSatellitePasses(
        context: Context,
        selectedSatIds: Set<String>,
        userLocation: UserLocation,
        leadMinutes: Int = 10
    ) {
        scheduleRollingSatellitePasses(
            context = context,
            userLocation = userLocation,
            selectedSatIds = selectedSatIds,
            leadMinutes = leadMinutes
        )
    }

    /**
     * Maintains a rolling ~7-day schedule of visible satellite passes for static monitored satellites,
     * and a capped 48-hour (2-day) schedule for `"starlink_train"` (since actively thrusting Starlink
     * trains shift pass timings beyond 48 hours).
     * Cancels obsolete future alarms for the target satellites and schedules newly predicted passes
     * with zero duplicates and complete offline persistence.
     */
    fun scheduleRollingSatellitePasses(
        context: Context,
        userLocation: UserLocation,
        selectedSatIds: Set<String>,
        leadMinutes: Int = 10,
        scanDays: Int = 7,
        nowMs: Long = System.currentTimeMillis(),
        starlinkTlesOverride: List<ISSEngine.TLEData>? = null
    ) {
        val requested = selectedSatIds.ifEmpty {
            (SatelliteCatalog.satellites.filter { it.isNakedEyeCandidate }.map { it.id } + "starlink_train").toSet()
        }.map { if (it.startsWith("starlink_train_")) "starlink_train" else it }.toSet()
        val revision = synchronized(passLock) {
            savePassMonitoringPreferences(context, userLocation, requested, leadMinutes, enabled = true)
            updateStoredLocation(context, userLocation)
            enqueueIssWorkManager(context)
            ++rollingRevision
        }
        replaceRollingPasses(context, userLocation, requested, leadMinutes, scanDays, nowMs, revision, starlinkTlesOverride)
    }

    internal fun predictRollingPasses(context: Context, location: UserLocation, ids: Set<String>, nowMs: Long,
        scanDays: Int = 7, starlinkTlesOverride: List<ISSEngine.TLEData>? = null): List<Pair<SatelliteItem, ISSEngine.ISSPass>> {
        val targets = SatelliteCatalog.satellites.filter { it.id in ids }.toMutableList()
        if ("starlink_train" in ids) {
            val tles = starlinkTlesOverride ?: TleRepository.getInstance(context).run {
                ensureStarlinkDiskCacheLoaded()
                getStarlinkTles()
            }
            StarlinkTrainManager.detectActiveTrains(tles, nowMs).firstOrNull()?.leaderItem?.copy(id = "starlink_train")?.let { train ->
                SatelliteCatalog.registerDynamicStarlinks(listOf(train), primaryTrain = train)
                targets.add(train)
            }
        }
        return targets.flatMap { sat ->
            val days = if (sat.isTrain) scanDays.coerceAtMost(STARLINK_TRAIN_ALERT_SCAN_DAYS) else scanDays
            ISSEngine.predictPasses(location.latitude, location.longitude, nowMs, tle = SatelliteEngine.getEffectiveTle(sat),
                scanDays = days, visibleOnly = true, standardMag = sat.standardMagnitude).asSequence()
                .filter { it.isVisible && it.maxElevationDeg >= 10.0 && it.passDurationSec >= 30 &&
                    (!sat.isTrain || it.startTimeMs <= nowMs + STARLINK_TRAIN_ALERT_HORIZON_MS) }
                .map { sat to it }.toList()
        }.sortedBy { it.second.startTimeMs }
    }

    private fun replaceRollingPasses(context: Context, location: UserLocation, ids: Set<String>, leadMinutes: Int,
        scanDays: Int, nowMs: Long, revision: Long, starlinkTlesOverride: List<ISSEngine.TLEData>? = null) {
        val predicted = predictRollingPasses(context, location, ids, nowMs, scanDays, starlinkTlesOverride)
        synchronized(passLock) {
            // A newer settings change/refresh supersedes this prediction, including switching off.
            if (revision != rollingRevision) return
            val existing = AstroNotificationStore.getAll(context).filter { it.objectType == "SATELLITE" }
            val manual = existing.filter { it.manualPass }
            val next = predicted.mapNotNull { (sat, pass) ->
                if (manual.any { it.coversSatellitePass(sat, pass.startTimeMs) }) null
                else createScheduledPassItem(sat, pass, location, leadMinutes, nowMs)
            }
            val nextIds = next.map { it.id }.toSet()
            removePassAlarms(context, existing.filter { !it.manualPass && it.id !in nextIds })
            if (next.isNotEmpty()) AstroNotificationStore.saveAll(context, next)
            next.forEach { setAlarmWithAndroidSystem(context, it) }
        }
    }

    private data class RollingSettings(val revision: Long, val location: UserLocation, val ids: Set<String>, val lead: Int)

    /** A refresh (from TLE sync or WorkManager) never changes the user's enabling preference. */
    fun refreshSatellitePassSchedulesIfEnabled(context: Context) {
        val settings = synchronized(passLock) {
            val prefs = context.getSharedPreferences(ASTRO_PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_AUTO_SATELLITE_ALERTS, prefs.getBoolean(KEY_ISS_AUTO_ALERTS, false))) return
            val ids = prefs.getStringSet(KEY_MONITORED_SAT_IDS, null)?.toSet() ?: setOf("iss_zarya")
            val location = UserLocation(
                cityNameFa = prefs.getString("user_city_name_fa", null) ?: "نورآباد ممسنی",
                cityNameEn = prefs.getString("user_city_name_en", null) ?: "Noorabad Mamasani",
                latitude = prefs.getFloat("user_lat", 30.1132f).toDouble(),
                longitude = prefs.getFloat("user_lon", 51.5217f).toDouble(),
                timezoneId = prefs.getString("user_timezone_id", null) ?: java.util.TimeZone.getDefault().id,
            )
            RollingSettings(++rollingRevision, location, ids, prefs.getInt(KEY_ALERT_LEAD_MINUTES, prefs.getInt(KEY_ISS_LEAD_MINUTES, 10)))
        }
        replaceRollingPasses(context, settings.location, settings.ids, settings.lead, 7, System.currentTimeMillis(), settings.revision)
    }

    fun requestRollingPassRefresh(context: Context) {
        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(REFRESH_WORK_NAME, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<IssPassSchedulerWorker>().build())
        }.onFailure { Log.w(TAG, "Could not enqueue pass refresh", it) }
    }

    /**
     * Called on device boot, app update, timezone change, or system time set.
     * Reloads all stored passes, purges expired ones, and re-registers Android system alarms.
     * Also refreshes the rolling 7-day schedule using local cached TLEs completely offline.
     */
    fun onDeviceRebootOrTimeChanged(context: Context) {
        Log.i(TAG, "Handling device reboot or system time change...")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AstroAlarmReceiver.createChannels(notificationManager)

        // 1. Purge expired items
        val nowMs = System.currentTimeMillis()
        val expired = AstroNotificationStore.purgeExpired(context, nowMs)
        Log.d(TAG, "Purged ${expired.size} expired notifications")

        // 2. Re-register all remaining future alarms with AlarmManager
        val remaining = AstroNotificationStore.getAll(context)
        for (item in remaining) {
            setAlarmWithAndroidSystem(context, item)
        }
        Log.i(TAG, "Re-registered ${remaining.size} future alarms from local persistence")

        // 3. If auto satellite monitoring is enabled, recalculate and extend rolling 7-day schedule offline
        refreshSatellitePassSchedulesIfEnabled(context)

        // 4. Ensure periodic background workers are scheduled
        IssTleWorker.schedulePeriodicSync(context)
        TleSyncWorker.schedulePeriodicSync(context)
        enqueueIssWorkManager(context)
    }

    /**
     * Backward-compatible alias for rescheduleAllAlarms.
     */
    fun rescheduleAllAlarms(context: Context) {
        onDeviceRebootOrTimeChanged(context)
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
                    intNotificationId = (idStr.hashCode() and 0x7FFFFFFF),
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
                    titleFa = "یادآوری رصد: ${obj.nameFa} (۱۰ دقیقه دیگر)",
                    titleEn = "Observation Reminder: ${obj.nameEn} (in 10 min)",
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
                intNotificationId = (idStr.hashCode() and 0x7FFFFFFF),
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
                titleFa = "آغاز رصد: ${obj.nameFa}",
                titleEn = "Observation Event: ${obj.nameEn}",
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
            intNotificationId = (idStr.hashCode() and 0x7FFFFFFF),
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
     * Detects meaningful location changes (> 10 km) and refreshes location-sensitive passes.
     */
    fun handleLocationChanged(context: Context, newLocation: UserLocation) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastLat = prefs.getFloat(KEY_LAST_LAT, 999f).toDouble()
        val lastLon = prefs.getFloat(KEY_LAST_LON, 999f).toDouble()

        if (lastLat != 999.0 && lastLon != 999.0) {
            val distKm = calculateDistanceKm(lastLat, lastLon, newLocation.latitude, newLocation.longitude)
            if (distKm > 10.0) {
                updateStoredLocation(context, newLocation)
                refreshSatellitePassSchedulesIfEnabled(context)
            }
        } else {
            updateStoredLocation(context, newLocation)
        }
    }

    /**
     * Cancels a specific scheduled notification by ID from AlarmManager and Store.
     */
    fun cancelNotification(context: Context, notificationIdStr: String) {
        val item = AstroNotificationStore.getById(context, notificationIdStr) ?: return
        cancelAlarmOnly(context, item)
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

    /**
     * Cancels all scheduled satellite pass notifications across all satellites.
     */
    fun cancelAllPassNotifications(context: Context) = synchronized(passLock) {
        rollingRevision++
        val items = AstroNotificationStore.getAll(context).filter { it.objectType == "SATELLITE" }
        for (item in items) {
            cancelAlarmOnly(context, item)
        }
        if (items.isNotEmpty()) {
            AstroNotificationStore.removeByIds(context, items.map { it.id }.toSet())
            Log.i(TAG, "Cancelled all ${items.size} satellite pass notifications.")
        }
        cancelIssWorkManager(context)
    }

    private fun cancelAlarmOnly(context: Context, item: ScheduledNotificationItem) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AstroAlarmReceiver::class.java).apply {
                action = AstroAlarmReceiver.ACTION_TRIGGER_NOTIFICATION
                putExtra(AstroAlarmReceiver.EXTRA_NOTIFICATION_ID, item.id)
                putExtra(AstroAlarmReceiver.EXTRA_INT_NOTIFICATION_ID, item.intNotificationId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.intNotificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling alarm for ${item.id}: ${e.message}")
        }
    }

    /**
     * Sets an exact alarm with the Android AlarmManager using modern Android exact-alarm best practices.
     * Carries all metadata in intent extras as well as in persistent storage for 100% offline reliability.
     */
    fun setAlarmWithAndroidSystem(context: Context, item: ScheduledNotificationItem) {
        val triggerMs = item.triggerTimeMs
        if (triggerMs <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AstroAlarmReceiver::class.java).apply {
            action = AstroAlarmReceiver.ACTION_TRIGGER_NOTIFICATION
            putExtra(AstroAlarmReceiver.EXTRA_NOTIFICATION_ID, item.id)
            putExtra(AstroAlarmReceiver.EXTRA_INT_NOTIFICATION_ID, item.intNotificationId)
            putExtra(AstroAlarmReceiver.EXTRA_OBJECT_ID, item.objectId)
            putExtra(AstroAlarmReceiver.EXTRA_OBJECT_NAME_EN, item.objectNameEn)
            putExtra(AstroAlarmReceiver.EXTRA_OBJECT_NAME_FA, item.objectNameFa)
            putExtra(AstroAlarmReceiver.EXTRA_OBJECT_TYPE, item.objectType)
            putExtra(AstroAlarmReceiver.EXTRA_TITLE_EN, item.titleEn)
            putExtra(AstroAlarmReceiver.EXTRA_TITLE_FA, item.titleFa)
            putExtra(AstroAlarmReceiver.EXTRA_CONTENT_EN, item.contentEn)
            putExtra(AstroAlarmReceiver.EXTRA_CONTENT_FA, item.contentFa)
            putExtra(AstroAlarmReceiver.EXTRA_DEEP_LINK_ROUTE, item.deepLinkRoute)
            putExtra(AstroAlarmReceiver.EXTRA_EVENT_TIME_MS, item.eventTimeMs)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.intNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "SecurityException while scheduling alarm: ${ex.message}")
            }
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Exception while scheduling alarm: ${ex.message}")
            }
        }
    }

    fun enqueueIssWorkManager(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<IssPassSchedulerWorker>(12, TimeUnit.HOURS)
                .setInitialDelay(12, TimeUnit.HOURS).build()
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
            WorkManager.getInstance(context).also {
                it.cancelUniqueWork(WORK_NAME)
                it.cancelUniqueWork(REFRESH_WORK_NAME)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePassMonitoringPreferences(context: Context, location: UserLocation,
        selectedSatIds: Set<String>, leadMinutes: Int, enabled: Boolean) {
        listOf(ASTRO_PREFS, "astro_app_prefs").forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_AUTO_SATELLITE_ALERTS, enabled)
                .putBoolean(KEY_ISS_AUTO_ALERTS, enabled)
                .putStringSet(KEY_MONITORED_SAT_IDS, selectedSatIds)
                .putInt(KEY_ALERT_LEAD_MINUTES, leadMinutes)
                .putInt(KEY_ISS_LEAD_MINUTES, leadMinutes)
                .putString("user_timezone_id", location.timezoneId)
                .apply()
        }
    }

    private fun updateStoredLocation(context: Context, loc: UserLocation) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_LAST_LAT, loc.latitude.toFloat())
            .putFloat(KEY_LAST_LON, loc.longitude.toFloat())
            .apply()

        val astroPrefs = context.getSharedPreferences(ASTRO_PREFS, Context.MODE_PRIVATE)
        astroPrefs.edit()
            .putString("user_city_name_fa", loc.cityNameFa)
            .putString("user_city_name_en", loc.cityNameEn)
            .putString("user_timezone_id", loc.timezoneId)
            .putFloat("user_lat", loc.latitude.toFloat())
            .putFloat("user_lon", loc.longitude.toFloat())
            .apply()

        val appPrefs = context.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE)
        appPrefs.edit()
            .putString("user_city_name_fa", loc.cityNameFa)
            .putString("user_city_name_en", loc.cityNameEn)
            .putFloat("user_latitude", loc.latitude.toFloat())
            .putFloat("user_longitude", loc.longitude.toFloat())
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
