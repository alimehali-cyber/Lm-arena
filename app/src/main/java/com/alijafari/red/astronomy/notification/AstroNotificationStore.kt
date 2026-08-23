package com.alijafari.red.astronomy.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data class representing a persisted scheduled notification item.
 * Guarantees every notification is tied to a stable object ID and explicit event data,
 * eliminating object-mismatch and lost notifications when the app is closed or rebooted.
 */
data class ScheduledNotificationItem(
    val id: String,                  // Unique notification string ID e.g. "pass_iss_zarya_1723456780000_10"
    val intNotificationId: Int,      // Stable Int ID for AlarmManager & NotificationManager
    val objectId: String,            // STABLE OBJECT ID e.g. "iss_zarya", "starlink_train_g7", "moon", "m31"
    val objectNameEn: String,        // "ISS (International Space Station)"
    val objectNameFa: String,        // "ایستگاه فضایی بین‌المللی"
    val objectType: String,          // "SATELLITE", "CELESTIAL", "ECLIPSE", "METEOR"
    val passOrEventId: String,       // Unique pass/event ID
    val triggerTimeMs: Long,         // Timestamp when alarm should fire
    val eventTimeMs: Long,           // Timestamp when pass or event starts
    val leadMinutes: Int,            // Lead time in minutes
    val userLat: Double,             // Latitude used for pass calculation
    val userLon: Double,             // Longitude used for pass calculation
    val cityNameFa: String,          // Persian city name
    val cityNameEn: String,          // English city name
    val maxElevationDeg: Double = 0.0,
    val startDirFa: String = "",
    val startDirEn: String = "",
    val endDirFa: String = "",
    val endDirEn: String = "",
    val durationSec: Int = 0,
    val titleEn: String = "",
    val titleFa: String = "",
    val contentEn: String = "",
    val contentFa: String = "",
    val deepLinkRoute: String = ""   // e.g. "satellite/iss_zarya" or "object/moon"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("intNotificationId", intNotificationId)
            put("objectId", objectId)
            put("objectNameEn", objectNameEn)
            put("objectNameFa", objectNameFa)
            put("objectType", objectType)
            put("passOrEventId", passOrEventId)
            put("triggerTimeMs", triggerTimeMs)
            put("eventTimeMs", eventTimeMs)
            put("leadMinutes", leadMinutes)
            put("userLat", userLat)
            put("userLon", userLon)
            put("cityNameFa", cityNameFa)
            put("cityNameEn", cityNameEn)
            put("maxElevationDeg", maxElevationDeg)
            put("startDirFa", startDirFa)
            put("startDirEn", startDirEn)
            put("endDirFa", endDirFa)
            put("endDirEn", endDirEn)
            put("durationSec", durationSec)
            put("titleEn", titleEn)
            put("titleFa", titleFa)
            put("contentEn", contentEn)
            put("contentFa", contentFa)
            put("deepLinkRoute", deepLinkRoute)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ScheduledNotificationItem {
            return ScheduledNotificationItem(
                id = json.optString("id", ""),
                intNotificationId = json.optInt("intNotificationId", 0),
                objectId = json.optString("objectId", ""),
                objectNameEn = json.optString("objectNameEn", ""),
                objectNameFa = json.optString("objectNameFa", ""),
                objectType = json.optString("objectType", "SATELLITE"),
                passOrEventId = json.optString("passOrEventId", ""),
                triggerTimeMs = json.optLong("triggerTimeMs", 0L),
                eventTimeMs = json.optLong("eventTimeMs", 0L),
                leadMinutes = json.optInt("leadMinutes", 10),
                userLat = json.optDouble("userLat", 0.0),
                userLon = json.optDouble("userLon", 0.0),
                cityNameFa = json.optString("cityNameFa", ""),
                cityNameEn = json.optString("cityNameEn", ""),
                maxElevationDeg = json.optDouble("maxElevationDeg", 0.0),
                startDirFa = json.optString("startDirFa", ""),
                startDirEn = json.optString("startDirEn", ""),
                endDirFa = json.optString("endDirFa", ""),
                endDirEn = json.optString("endDirEn", ""),
                durationSec = json.optInt("durationSec", 0),
                titleEn = json.optString("titleEn", ""),
                titleFa = json.optString("titleFa", ""),
                contentEn = json.optString("contentEn", ""),
                contentFa = json.optString("contentFa", ""),
                deepLinkRoute = json.optString("deepLinkRoute", "")
            )
        }
    }
}

/**
 * Thread-safe persistent JSON store for all scheduled notifications across the app.
 */
object AstroNotificationStore {
    private const val PREFS_NAME = "astro_notification_engine_prefs"
    private const val KEY_NOTIFICATIONS = "scheduled_notifications_json"

    @Synchronized
    fun getAll(context: Context): List<ScheduledNotificationItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val list = mutableListOf<ScheduledNotificationItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(ScheduledNotificationItem.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @Synchronized
    fun getById(context: Context, id: String): ScheduledNotificationItem? {
        return getAll(context).find { it.id == id }
    }

    @Synchronized
    fun getByIntId(context: Context, intId: Int): ScheduledNotificationItem? {
        return getAll(context).find { it.intNotificationId == intId }
    }

    @Synchronized
    fun save(context: Context, item: ScheduledNotificationItem) {
        val current = getAll(context).toMutableList()
        current.removeAll { it.id == item.id }
        current.add(item)
        saveAllInternal(context, current)
    }

    @Synchronized
    fun saveAll(context: Context, items: List<ScheduledNotificationItem>) {
        val current = getAll(context).toMutableList()
        for (item in items) {
            current.removeAll { it.id == item.id }
            current.add(item)
        }
        saveAllInternal(context, current)
    }

    @Synchronized
    fun remove(context: Context, id: String) {
        val current = getAll(context).toMutableList()
        if (current.removeAll { it.id == id }) {
            saveAllInternal(context, current)
        }
    }

    @Synchronized
    fun removeByIds(context: Context, ids: Set<String>) {
        if (ids.isEmpty()) return
        val current = getAll(context).toMutableList()
        if (current.removeAll { it.id in ids }) {
            saveAllInternal(context, current)
        }
    }

    @Synchronized
    fun removeByObjectId(context: Context, objectId: String) {
        val current = getAll(context).toMutableList()
        if (current.removeAll { it.objectId == objectId }) {
            saveAllInternal(context, current)
        }
    }

    @Synchronized
    fun purgeExpired(context: Context, nowMs: Long = System.currentTimeMillis()): List<ScheduledNotificationItem> {
        val current = getAll(context)
        val expired = current.filter { it.triggerTimeMs <= nowMs }
        val remaining = current.filter { it.triggerTimeMs > nowMs }
        saveAllInternal(context, remaining)
        return expired
    }

    @Synchronized
    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    private fun saveAllInternal(context: Context, items: List<ScheduledNotificationItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        items.forEach { jsonArray.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
    }
}
