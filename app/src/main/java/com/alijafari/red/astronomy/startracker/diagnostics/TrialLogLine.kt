package com.alijafari.red.astronomy.startracker.diagnostics

/**
 * D3 (debug-diagnostics pass, 2026-09-04): one timestamped JSON line of the device
 * trial log. Pure Kotlin (no Android imports) so the serialization is unit-tested in
 * the offline harness AND in CI; the Android side (file append, GPS, share) lives in
 * the debug source set only.
 *
 * Schema is FIXED-ORDER and every field is always present (nulls as JSON null) so a
 * trial file can be parsed line-by-line without schema negotiation. Strings are
 * sanitized to [A-Za-z0-9_.:+/ -] — inside JSON string literals no escaping is then
 * possible, keeping the serializer trivially auditable.
 */
data class TrialLogLine(
    val epochMs: Long,
    val iso8601: String,
    /** start | step | stop */
    val event: String,
    /** step number, null unless event == "step" */
    val step: Int?,
    val enabled: Boolean,
    val pipelineCameraFeed: Boolean,
    val trackerToOrientationPhase6: Boolean,
    val projectionSelfCalibratedPhase7: Boolean,
    val declinationDeg: Double?,
    val intrinsicsTier: String?,
    val fx: Double?,
    val fy: Double?,
    val cx: Double?,
    val cy: Double?,
    val distortionTier: String,
    val k1: Double?,
    val k2: Double?,
    val sensorHz: Double?,
    val sensorTsDeltaMs: Double?,
    val trackerState: String,
    val lockConfidence: String?,
    val matched: Int?,
    val detected: Int?,
    val solveMs: Double?,
    val discrepancyDeg: Double?,
    val failureReason: String?,
    val gpsLat: Double?,
    val gpsLon: Double?,
    val gpsAccuracyM: Double?,
    val deviceModel: String,
    val deviceSdk: Int
) {
    fun toJson(): String = StringBuilder(512).apply {
        a("ts", epochMs); s("iso", iso8601); s("event", event); n("step", step)
        a("enabled", enabled); a("pipelineCameraFeed", pipelineCameraFeed)
        a("trackerToOrientationPhase6", trackerToOrientationPhase6)
        a("projectionSelfCalibratedPhase7", projectionSelfCalibratedPhase7)
        d("declinationDeg", declinationDeg)
        s("intrinsicsTier", intrinsicsTier); d("fx", fx); d("fy", fy); d("cx", cx); d("cy", cy)
        s("distortionTier", distortionTier); d("k1", k1); d("k2", k2)
        d("sensorHz", sensorHz); d("sensorTsDeltaMs", sensorTsDeltaMs)
        s("trackerState", trackerState); s("lockConfidence", lockConfidence)
        n("matched", matched); n("detected", detected)
        d("solveMs", solveMs); d("discrepancyDeg", discrepancyDeg); s("failureReason", failureReason)
        d("gpsLat", gpsLat); d("gpsLon", gpsLon); d("gpsAccuracyM", gpsAccuracyM)
        s("deviceModel", deviceModel); a("deviceSdk", deviceSdk)
    }.toString().let { "{$it}" }

    private fun StringBuilder.a(k: String, v: Boolean) { if (length > 0) append(','); append("\"$k\":$v") }
    private fun StringBuilder.a(k: String, v: Long) { if (length > 0) append(','); append("\"$k\":$v") }
    private fun StringBuilder.a(k: String, v: Int) { if (length > 0) append(','); append("\"$k\":$v") }
    private fun StringBuilder.n(k: String, v: Int?) { if (length > 0) append(','); append("\"$k\":"); append(v?.toString() ?: "null") }
    private fun StringBuilder.d(k: String, v: Double?) {
        if (length > 0) append(',')
        append("\"$k\":")
        append(if (v == null) "null" else if (v.isNaN() || v.isInfinite()) "\"${if (v.isNaN()) "NaN" else if (v > 0) "Infinity" else "-Infinity"}\"" else trimDouble(v))
    }
    private fun StringBuilder.s(k: String, v: String?) {
        if (length > 0) append(',')
        append("\"$k\":")
        append(if (v == null) "null" else "\"" + sanitize(v) + "\"")
    }

    private fun trimDouble(v: Double): String {
        val s = "%.6f".format(v)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
    }

    companion object {
        /** Keeps [A-Za-z0-9_.:+/ -]; everything else becomes '_' (auditable, escape-free). */
        fun sanitize(v: String): String = buildString(v.length) {
            for (c in v) append(if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '_' || c == '.' || c == ':' || c == '+' || c == '/' || c == ' ' || c == '-') c else '_')
        }
    }
}
