package com.alijafari.red.astronomy.notification

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Centralized Notification Permission & Status Manager for RED Astronomy Engine.
 * Provides standardized status checks for POST_NOTIFICATIONS, notification channels,
 * user-facing navigation to system notification settings, and diagnostic checks for
 * background execution & battery restrictions.
 */
object NotificationPermissionHelper {

    enum class PostNotificationStatus {
        ALLOWED,         // Notifications permitted by system and granted by user
        DENIED,          // Permission explicitly denied by user, or system app notifications turned off
        NOT_REQUESTED,   // Android 13+ (API 33+) where permission has not been requested yet
        NOT_APPLICABLE   // Android 12 and below (API < 33) where runtime permission POST_NOTIFICATIONS is not applicable
    }

    enum class ChannelStatus {
        ENABLED,         // Channel exists and is enabled by user
        DISABLED,        // Channel exists but blocked by user
        NOT_FOUND,       // Channel has not been created yet
        NOT_APPLICABLE   // Android 7.1 and below (API < 26) where channels are not applicable
    }

    data class DiagnosticBackgroundStatus(
        val isIgnoringBatteryOptimizations: Boolean,
        val canScheduleExactAlarms: Boolean,
        val isBackgroundRestricted: Boolean
    )

    /**
     * Standardized check for POST_NOTIFICATIONS status across the application.
     */
    fun checkPostNotificationStatus(context: Context): PostNotificationStatus {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val areNotificationsEnabled = notificationManager?.areNotificationsEnabled() ?: true

        if (!areNotificationsEnabled) {
            return PostNotificationStatus.DENIED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            return when {
                permissionCheck == PackageManager.PERMISSION_GRANTED -> PostNotificationStatus.ALLOWED
                context is Activity && context.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> PostNotificationStatus.DENIED
                else -> PostNotificationStatus.NOT_REQUESTED
            }
        }

        return if (areNotificationsEnabled) PostNotificationStatus.ALLOWED else PostNotificationStatus.DENIED
    }

    /**
     * Checks the status of a specific Notification Channel (API 26+).
     */
    fun checkChannelStatus(context: Context, channelId: String): ChannelStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return ChannelStatus.NOT_APPLICABLE
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return ChannelStatus.NOT_FOUND

        val channel = notificationManager.getNotificationChannel(channelId) ?: return ChannelStatus.NOT_FOUND
        return if (channel.importance != NotificationManager.IMPORTANCE_NONE) {
            ChannelStatus.ENABLED
        } else {
            ChannelStatus.DISABLED
        }
    }

    /**
     * Provides a standardized user-facing Intent path to Android system Notification Settings.
     * MUST be called from UI context, NOT background code.
     */
    fun openNotificationSettings(context: Context, channelId: String? = null) {
        val intent = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (channelId != null) {
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                    }
                } else {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        } catch (e: Exception) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }

        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Diagnostic status check for background execution & battery restrictions.
     * Kept separate from POST_NOTIFICATIONS status.
     */
    fun checkDiagnosticBackgroundStatus(context: Context): DiagnosticBackgroundStatus {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager != null) {
            alarmManager.canScheduleExactAlarms()
        } else true

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activityManager != null) {
            activityManager.isBackgroundRestricted
        } else false

        return DiagnosticBackgroundStatus(
            isIgnoringBatteryOptimizations = isIgnoringBattery,
            canScheduleExactAlarms = canScheduleExact,
            isBackgroundRestricted = isRestricted
        )
    }
}
