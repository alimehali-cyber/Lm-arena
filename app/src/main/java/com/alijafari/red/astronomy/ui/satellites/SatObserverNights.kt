package com.alijafari.red.astronomy.ui.satellites

import com.alijafari.red.astronomy.astro_engine.SunEngine
import java.util.Calendar
import java.util.TimeZone

/** Real sunset/sunrise at the observer; the local evening date is a stable display/jump key. */
internal class SatObserverNights(private val lat: Double, private val lon: Double, private val tz: TimeZone) {
    data class Window(val sunsetMs: Long, val sunriseMs: Long) {
        operator fun contains(ms: Long) = ms in sunsetMs until sunriseMs
    }

    private val windows = mutableMapOf<Long, Window?>()

    fun dayKey(ms: Long): Long = Calendar.getInstance(tz).apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun plusDays(key: Long, days: Int): Long = Calendar.getInstance(tz).apply {
        timeInMillis = key
        add(Calendar.DATE, days)
    }.timeInMillis

    // In the daytime, label the upcoming night with today's date. Neither noon nor midnight
    // splits a night; in continuous polar darkness, split at noon to keep finite day groups.
    fun key(ms: Long): Long {
        val today = dayKey(ms)
        val previous = plusDays(today, -1)
        return if (window(previous)?.contains(ms) == true) previous else today
    }

    @Synchronized
    fun window(eveningKey: Long): Window? {
        if (windows.containsKey(eveningKey)) return windows[eveningKey]
        if (windows.size >= 24) windows.clear()
        val end = plusDays(eveningKey, 1)
        var left = eveningKey
        var dark = isDark(left)
        var sunset = if (dark) left else null
        var sunrise: Long? = null
        while (left < end) {
            val right = minOf(left + 15 * 60_000L, end)
            val nextDark = isDark(right)
            if (nextDark != dark) {
                val boundary = crossing(left, right, dark)
                if (nextDark) sunset = boundary
                else {
                    sunrise = boundary
                    break
                }
            }
            left = right
            dark = nextDark
        }
        return sunset?.let { Window(it, sunrise ?: end) }.also { windows[eveningKey] = it }
    }

    private fun crossing(start: Long, end: Long, startDark: Boolean): Long {
        var low = start
        var high = end
        while (high - low > 1_000L) {
            val mid = (low + high) / 2
            if (isDark(mid) == startDark) low = mid else high = mid
        }
        return high
    }

    // Apparent sunrise/sunset: refraction plus the solar radius, also used by CoordinateEngine.
    private fun isDark(ms: Long) = SunEngine.getSunAltitude(ms, lat, lon) <= -0.833
}
