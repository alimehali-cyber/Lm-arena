package com.alijafari.red.astronomy.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alijafari.red.astronomy.MainActivity
import com.alijafari.red.astronomy.domain.AppLanguage

class AstroAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "AstroAlarmReceiver received action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            "android.intent.action.TIME_SET",
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            ACTION_RESCHEDULE_ALL -> {
                // Restore & Reschedule all active alarms after reboot, update, or timezone change
                AstroNotificationManager.onDeviceRebootOrTimeChanged(context)
            }
            ACTION_TRIGGER_NOTIFICATION,
            ACTION_TRIGGER_ISS_NOTIFICATION,
            ACTION_TRIGGER_OBJECT_NOTIFICATION -> {
                val notifIdStr = intent.getStringExtra(EXTRA_NOTIFICATION_ID)
                val intId = intent.getIntExtra(EXTRA_INT_NOTIFICATION_ID, 0)
                handleTriggeredNotification(context, intent, notifIdStr, intId)
            }
        }
    }

    private fun handleTriggeredNotification(
        context: Context,
        intent: Intent,
        notifIdStr: String?,
        intId: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        // Retrieve from local store
        var item = if (!notifIdStr.isNullOrEmpty()) {
            AstroNotificationStore.getById(context, notifIdStr)
        } else if (intId != 0) {
            AstroNotificationStore.getByIntId(context, intId)
        } else null

        // Offline fallback: If item wasn't in store, construct from explicit intent extras
        if (item == null && !notifIdStr.isNullOrEmpty()) {
            val objId = intent.getStringExtra(EXTRA_OBJECT_ID) ?: "unknown"
            val titleEn = intent.getStringExtra(EXTRA_TITLE_EN) ?: ""
            val titleFa = intent.getStringExtra(EXTRA_TITLE_FA) ?: ""
            val contentEn = intent.getStringExtra(EXTRA_CONTENT_EN) ?: ""
            val contentFa = intent.getStringExtra(EXTRA_CONTENT_FA) ?: ""
            val objType = intent.getStringExtra(EXTRA_OBJECT_TYPE) ?: "SATELLITE"
            val route = intent.getStringExtra(EXTRA_DEEP_LINK_ROUTE) ?: "satellite/$objId"
            val effectiveIntId = if (intId != 0) intId else (notifIdStr.hashCode() and 0x7FFFFFFF)

            if (titleEn.isNotEmpty() || titleFa.isNotEmpty() || contentEn.isNotEmpty() || contentFa.isNotEmpty()) {
                item = ScheduledNotificationItem(
                    id = notifIdStr,
                    intNotificationId = effectiveIntId,
                    objectId = objId,
                    objectNameEn = titleEn,
                    objectNameFa = titleFa,
                    objectType = objType,
                    passOrEventId = notifIdStr,
                    triggerTimeMs = System.currentTimeMillis(),
                    eventTimeMs = intent.getLongExtra(EXTRA_EVENT_TIME_MS, System.currentTimeMillis()),
                    leadMinutes = 10,
                    userLat = 0.0,
                    userLon = 0.0,
                    cityNameFa = "",
                    cityNameEn = "",
                    titleEn = titleEn,
                    titleFa = titleFa,
                    contentEn = contentEn,
                    contentFa = contentFa,
                    deepLinkRoute = route
                )
            }
        }

        if (item == null) {
            Log.w(TAG, "Notification trigger received but no stored or extra item data found (id: $notifIdStr, intId: $intId)")
            return
        }

        val prefs = context.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE)
        val langStr = prefs.getString("language", AppLanguage.PERSIAN.name) ?: AppLanguage.PERSIAN.name
        val isFa = langStr == AppLanguage.PERSIAN.name

        val title = (if (isFa) item.titleFa.ifEmpty { item.titleEn } else item.titleEn.ifEmpty { item.titleFa }).ifEmpty { "RED Astronomy" }
        val contentText = (if (isFa) item.contentFa.ifEmpty { item.contentEn } else item.contentEn.ifEmpty { item.contentFa })
        val channelId = when (item.objectType) {
            "SATELLITE" -> CHANNEL_ISS
            "CELESTIAL" -> CHANNEL_OBJECTS
            else -> CHANNEL_EVENTS
        }

        // Build deep link Intent for MainActivity targeting exact object ID
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TARGET_OBJECT_ID, item.objectId)
            putExtra(MainActivity.EXTRA_TARGET_TYPE, item.objectType)
            putExtra(MainActivity.EXTRA_TARGET_ROUTE, item.deepLinkRoute)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            item.intNotificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, false)
            .build()

        try {
            notificationManager.notify(item.intNotificationId, notification)
            Log.i(TAG, "Successfully displayed offline notification ID ${item.intNotificationId}: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification: ${e.message}", e)
        }

        // Remove delivered item from store
        AstroNotificationStore.remove(context, item.id)
    }

    companion object {
        private const val TAG = "AstroAlarmReceiver"

        const val CHANNEL_ISS = "channel_iss_passes"
        const val CHANNEL_OBJECTS = "channel_object_observations"
        const val CHANNEL_EVENTS = "channel_astronomy_events"

        const val ACTION_RESCHEDULE_ALL = "com.alijafari.red.astronomy.ACTION_RESCHEDULE_ALL"
        const val ACTION_TRIGGER_NOTIFICATION = "com.alijafari.red.astronomy.ACTION_TRIGGER_NOTIFICATION"
        const val ACTION_TRIGGER_ISS_NOTIFICATION = "com.alijafari.red.astronomy.ACTION_TRIGGER_ISS_NOTIFICATION"
        const val ACTION_TRIGGER_OBJECT_NOTIFICATION = "com.alijafari.red.astronomy.ACTION_TRIGGER_OBJECT_NOTIFICATION"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_INT_NOTIFICATION_ID = "extra_int_notification_id"
        const val EXTRA_OBJECT_ID = "extra_object_id"
        const val EXTRA_OBJECT_NAME_EN = "extra_object_name_en"
        const val EXTRA_OBJECT_NAME_FA = "extra_object_name_fa"
        const val EXTRA_OBJECT_TYPE = "extra_object_type"
        const val EXTRA_TITLE_EN = "extra_title_en"
        const val EXTRA_TITLE_FA = "extra_title_fa"
        const val EXTRA_CONTENT_EN = "extra_content_en"
        const val EXTRA_CONTENT_FA = "extra_content_fa"
        const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"
        const val EXTRA_EVENT_TIME_MS = "extra_event_time_ms"

        fun createChannels(notificationManager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val issChannel = NotificationChannel(
                    CHANNEL_ISS,
                    "هشدار گذر ماهواره‌ها (Satellite Passes)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اطلاع‌رسانی زمان گذرهای قابل مشاهده ماهواره‌ها با چشم غیرمسلح"
                    enableVibration(true)
                }

                val objChannel = NotificationChannel(
                    CHANNEL_OBJECTS,
                    "اطلاع‌رسانی رصد اجرام (Celestial Observations)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "هشدار طلوع، اوج ارتفاع و غروب سیارات، ستاره‌ها و اجرام اعماق فضا"
                    enableVibration(true)
                }

                val evtChannel = NotificationChannel(
                    CHANNEL_EVENTS,
                    "رویدادهای نجومی و گرفت‌ها (Astronomy Events)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "هشدار خورشیدگرفتگی، ماه گرفتگی و بارش‌های شهابی"
                    enableVibration(true)
                }

                notificationManager.createNotificationChannel(issChannel)
                notificationManager.createNotificationChannel(objChannel)
                notificationManager.createNotificationChannel(evtChannel)
            }
        }
    }
}
