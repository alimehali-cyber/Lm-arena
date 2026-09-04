package org.shredzone.commons.suncalc

/**
 * HARNESS SHIM (tools/kotlin-harness) — NOT the real library.
 * MoonEngine.kt imports org.shredzone.commons.suncalc.MoonTimes for rise/set times only.
 * The offline harness has no Maven access, so this minimal stub makes the file compilable.
 * rise/set are always null → MoonEngine falls back to its own catch-branch defaults.
 * WARNING: never place the real commons-suncalc jar on the harness classpath together with
 * this file (duplicate class org.shredzone.commons.suncalc.MoonTimes).
 */
class MoonTimes private constructor() {
    class Request {
        fun on(date: java.util.Date): Request = this
        fun at(lat: Double, lon: Double): Request = this
        fun execute(): Result = Result
    }
    object Result {
        val rise: java.util.Date? = null
        val set: java.util.Date? = null
    }
    companion object {
        fun compute(): Request = Request()
    }
}
