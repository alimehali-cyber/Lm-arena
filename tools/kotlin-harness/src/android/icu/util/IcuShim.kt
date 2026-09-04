package android.icu.util

/**
 * HARNESS SHIM (tools/kotlin-harness) — NOT the real android.icu.
 * TimeEngine.kt uses android.icu.util PersianCalendar ONLY for Solar-Hijri display
 * conversions (toSolarHijri / persianToTimestamp), which are off the coordinate path.
 * This stub makes TimeEngine compilable offline; calendar reads return fixed 1s.
 * WARNING: never compile on an Android classpath together with this file.
 */
class ULocale(language: String?, country: String?) {
    constructor(languageTag: String) : this(languageTag, "")
}

class TimeZone {
    companion object {
        fun getTimeZone(id: String): TimeZone = TimeZone()
    }
}

open class Calendar protected constructor() {
    companion object {
        const val YEAR = 1
        const val MONTH = 2
        const val DAY_OF_MONTH = 3
        const val HOUR_OF_DAY = 4
        const val MINUTE = 5
        const val SECOND = 6
        const val MILLISECOND = 7
        fun getInstance(tz: TimeZone, locale: ULocale): Calendar = Calendar()
    }
    var timeInMillis: Long = 0L
    fun clear() {}
    fun get(field: Int): Int = 1
    fun set(field: Int, value: Int) {}
}
