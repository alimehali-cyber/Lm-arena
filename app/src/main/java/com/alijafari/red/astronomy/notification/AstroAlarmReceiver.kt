package com.alijafari.red.astronomy.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.data.catalog.AstronomyCatalog
import com.alijafari.red.astronomy.domain.UserLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AstroAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_RESCHEDULE_ALL -> {
                // Restore & Reschedule all ISS passes & active object alarms
                rescheduleAllAlarms(context)
            }
            ACTION_TRIGGER_ISS_NOTIFICATION -> {
                val passTimeMs = intent.getLongExtra(EXTRA_PASS_TIME_MS, 0L)
                val maxElev = intent.getDoubleExtra(EXTRA_MAX_ELEVATION, 0.0)
                val startDir = intent.getStringExtra(EXTRA_START_DIR) ?: "غرب"
                val endDir = intent.getStringExtra(EXTRA_END_DIR) ?: "شرق"
                val durationSec = intent.getIntExtra(EXTRA_DURATION_SEC, 300)
                val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "نورآباد ممسنی"
                val leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 10)

                showIssNotification(
                    context = context,
                    passTimeMs = passTimeMs,
                    maxElev = maxElev,
                    startDir = startDir,
                    endDir = endDir,
                    durationSec = durationSec,
                    cityName = cityName,
                    leadMinutes = leadMinutes
                )
            }
            ACTION_TRIGGER_OBJECT_NOTIFICATION -> {
                val objName = intent.getStringExtra(EXTRA_OBJECT_NAME) ?: "جرم آسمانی"
                val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: "گذر / طلوع"
                val timeStr = intent.getStringExtra(EXTRA_TIME_STR) ?: ""

                showObjectNotification(
                    context = context,
                    objName = objName,
                    eventType = eventType,
                    timeStr = timeStr
                )
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        val prefs = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
        val isIssAutoAlertEnabled = prefs.getBoolean("iss_auto_alerts_enabled", false)
        val leadMinutes = prefs.getInt("iss_alert_lead_minutes", 10)

        if (isIssAutoAlertEnabled) {
            AstroNotificationManager.scheduleUpcomingIssPasses(context, leadMinutes = leadMinutes)
        }
    }

    private fun showIssNotification(
        context: Context,
        passTimeMs: Long,
        maxElev: Double,
        startDir: String,
        endDir: String,
        durationSec: Int,
        cityName: String,
        leadMinutes: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = if (passTimeMs > 0) timeFormat.format(Date(passTimeMs)) else "$leadMinutes دقیقه دیگر"
        val durMinutes = (durationSec / 60).coerceAtLeast(1)

        val title = "🛸 ایستگاه فضایی بین‌المللی — $leadMinutes دقیقه دیگر!"
        val contentText = "📍 شهر: $cityName | 🕐 زمان: $formattedTime | 📐 حداکثر ارتفاع: ${maxElev.toInt()}°\n🧭 مسیر: $startDir ➔ $endDir | ⏱ مدت: $durMinutes دقیقه"

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ISS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = (passTimeMs / 1000).toInt()
        notificationManager.notify(notifId, notification)
    }

    private fun showObjectNotification(
        context: Context,
        objName: String,
        eventType: String,
        timeStr: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val title = "🔭 یادآوری رصد: $objName"
        val contentText = "زمان $eventType $objName فرا رسیده است ($timeStr). شرایط رصدپذیری را بررسی کنید."

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            2002,
            launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_OBJECTS)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ISS = "channel_iss_passes"
        const val CHANNEL_OBJECTS = "channel_object_observations"

        const val ACTION_RESCHEDULE_ALL = "com.alijafari.red.astronomy.ACTION_RESCHEDULE_ALL"
        const val ACTION_TRIGGER_ISS_NOTIFICATION = "com.alijafari.red.astronomy.ACTION_TRIGGER_ISS_NOTIFICATION"
        const val ACTION_TRIGGER_OBJECT_NOTIFICATION = "com.alijafari.red.astronomy.ACTION_TRIGGER_OBJECT_NOTIFICATION"

        const val EXTRA_PASS_TIME_MS = "extra_pass_time_ms"
        const val EXTRA_MAX_ELEVATION = "extra_max_elevation"
        const val EXTRA_START_DIR = "extra_start_dir"
        const val EXTRA_END_DIR = "extra_end_dir"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_CITY_NAME = "extra_city_name"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"

        const val EXTRA_OBJECT_NAME = "extra_object_name"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
        const val EXTRA_TIME_STR = "extra_time_str"

        fun createChannels(notificationManager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val issChannel = NotificationChannel(
                    CHANNEL_ISS,
                    "هشدار گذر ایستگاه فضایی (ISS)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اطلاع‌رسانی زمان گذرهای قابل مشاهده ISS با چشم غیرمسلح"
                    enableVibration(true)
                }

                val objChannel = NotificationChannel(
                    CHANNEL_OBJECTS,
                    "اطلاع‌رسانی زمان رصد اجرام",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "هشدار طلوع و اوج ارتفاع سیارات، ستاره‌ها و اجرام اعماق فضا"
                    enableVibration(true)
                }

                notificationManager.createNotificationChannel(issChannel)
                notificationManager.createNotificationChannel(objChannel)
            }
        }
    }
}
