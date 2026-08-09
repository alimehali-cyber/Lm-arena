package com.alijafari.red.astronomy.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alijafari.red.astronomy.MainActivity
import com.alijafari.red.astronomy.domain.AppLanguage

class AstroAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            ACTION_RESCHEDULE_ALL -> {
                // Restore & Reschedule all active alarms after reboot, update, or timezone change
                AstroNotificationManager.rescheduleAllAlarms(context)
            }
            ACTION_TRIGGER_NOTIFICATION -> {
                val notifIdStr = intent.getStringExtra(EXTRA_NOTIFICATION_ID)
                val intId = intent.getIntExtra(EXTRA_INT_NOTIFICATION_ID, 0)
                handleTriggeredNotification(context, notifIdStr, intId)
            }
            // Backward compatibility for legacy actions
            ACTION_TRIGGER_ISS_NOTIFICATION -> {
                val notifIdStr = intent.getStringExtra(EXTRA_NOTIFICATION_ID)
                val intId = intent.getIntExtra(EXTRA_INT_NOTIFICATION_ID, 0)
                handleTriggeredNotification(context, notifIdStr, intId)
            }
            ACTION_TRIGGER_OBJECT_NOTIFICATION -> {
                val notifIdStr = intent.getStringExtra(EXTRA_NOTIFICATION_ID)
                val intId = intent.getIntExtra(EXTRA_INT_NOTIFICATION_ID, 0)
                handleTriggeredNotification(context, notifIdStr, intId)
            }
        }
    }

    private fun handleTriggeredNotification(context: Context, notifIdStr: String?, intId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val item = if (!notifIdStr.isNullOrEmpty()) {
            AstroNotificationStore.getById(context, notifIdStr)
        } else if (intId != 0) {
            AstroNotificationStore.getByIntId(context, intId)
        } else null

        if (item == null) {
            // Fallback if item was already purged or not stored
            return
        }

        val prefs = context.getSharedPreferences("astro_app_prefs", Context.MODE_PRIVATE)
        val langStr = prefs.getString("language", AppLanguage.PERSIAN.name) ?: AppLanguage.PERSIAN.name
        val isFa = langStr == AppLanguage.PERSIAN.name

        val title = if (isFa) item.titleFa else item.titleEn
        val contentText = if (isFa) item.contentFa else item.contentEn
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(item.intNotificationId, notification)

        // Remove delivered item from store
        AstroNotificationStore.remove(context, item.id)
    }

    companion object {
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
