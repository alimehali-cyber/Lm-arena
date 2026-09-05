package com.alijafari.red.astronomy.fieldtrial.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * G-1.3/L12: summary.md generator — plain English, all numbers, auto-written.
 * Pure Kotlin (harness + CI tested). The tester never writes it; Share zip includes
 * it verbatim next to trial.json.
 */
object TrialSummary {

    private val utc = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun fmt(v: Double?, digits: Int = 2): String =
        v?.let { "%.${digits}f".format(it) } ?: "n/a"

    fun generate(doc: TrialDocument): String {
        val sb = StringBuilder()
        sb.append("# Star-app field trial ", doc.trialId, "\n\n")
        sb.append("- Device: ", doc.deviceModel.ifBlank { "unknown" }, " (Android API ", doc.deviceSdk.toString(), ")\n")
        sb.append("- Started: ", utc.format(Date(doc.createdMs)), "\n")
        sb.append("- Last update: ", utc.format(Date(doc.updatedMs)), "\n")
        sb.append("- Location: ", fmt(doc.lat, 4), ", ", fmt(doc.lon, 4), "\n\n")

        sb.append("## Level results\n\n")
        sb.append("| Level | Result | Attempts |\n|---|---|---|\n")
        for (n in 0..12) {
            val rec = doc.latest(n)
            if (rec != null) {
                val mark = when (rec.outcome) {
                    LevelOutcome.PASS -> "PASS"
                    LevelOutcome.FAIL -> "FAIL"
                    LevelOutcome.SKIPPED -> "Skipped (" + (rec.skipReason?.label ?: "?") + ")"
                    LevelOutcome.PENDING -> "In progress"
                }
                sb.append("| L", n, " | ", mark, " | ", doc.attempts(n), " |\n")
            }
        }
        sb.append('\n')

        for (n in 0..12) {
            val records = doc.levels[n] ?: continue
            for (rec in records) {
                sb.append("## L", n, " — attempt ", rec.attempt, " (", rec.outcome.name, ")\n\n")
                if (rec.skipReason != null) sb.append("Skipped: ", rec.skipReason.label, ".  \n")
                if (rec.yesNo != null) sb.append("Answer: ", if (rec.yesNo) "Yes" else "No", ".  \n")
                for (m in rec.measurements) {
                    sb.append(
                        "### Tap on ", m.targetId, "\n",
                        "- Computed position: az ", fmt(m.computedAzDeg), "°, alt ", fmt(m.computedAltDeg), "°\n",
                        "- Tapped position: az ", fmt(m.tappedAzDeg), "°, alt ", fmt(m.tappedAltDeg), "°\n",
                        "- Difference: dAz ", fmt(m.dAzDeg), "°, dAlt ", fmt(m.dAltDeg), "°; on-sky separation ",
                        fmt(m.separationDeg, 3), "° (", fmt(m.screenOffsetPx, 0), " px on screen)\n",
                        "- Phone attitude at the tap: az ", fmt(m.sensorAzimuthDeg), "°, alt ", fmt(m.sensorAltitudeDeg),
                        "°, roll ", fmt(m.sensorRollDeg), "°\n",
                        "- Camera tier ", m.intrinsicsTier, " (fx ", fmt(m.fx, 1), ", fy ", fmt(m.fy, 1),
                        ", cx ", fmt(m.cx, 1), ", cy ", fmt(m.cy, 1), "); distortion ", m.distortionTier,
                        " (k1 ", fmt(m.k1, 5), ", k2 ", fmt(m.k2, 5), ")\n",
                        "- Compass correction applied: ", fmt(m.appliedDeclinationDeg), "°; zoom ", fmt(m.zoomFactor), "\n",
                        "- GPS: ", fmt(m.gpsLat, 5), ", ", fmt(m.gpsLon, 5), " (±", fmt(m.gpsAccuracyM, 0), " m)\n",
                        "- Time: ", utc.format(Date(m.epochMs)), "\n"
                    )
                }
                if (rec.auto.isNotEmpty()) {
                    sb.append("### Recorded automatically\n")
                    for ((k, v) in rec.auto) {
                        if (k == "yesNo") continue
                        sb.append("- ", prettyKey(k), ": ", prettyValue(v), "\n")
                    }
                }
                if (rec.note != null) sb.append("Note: ", rec.note, ".\n")
                sb.append('\n')
            }
        }
        sb.append("## What the results mean\n\n")
        sb.append(
            "Small differences (under about 1 degree) mean the app's sky picture matches " +
                "what you saw. Bigger differences are listed above with all the numbers so " +
                "they can be checked offline. 'Compass correction' is the local magnetic " +
                "adjustment the app applies automatically.\n"
        )
        return sb.toString()
    }

    private fun prettyKey(k: String): String = when (k) {
        "firstFullLockMs" -> "Time to first solid lock (ms)"
        "fps" -> "Analysis speed (frames/s)"
        "detections" -> "Stars seen in the last frame"
        "matched" -> "Stars matched to the star map"
        "solveMs" -> "Solve time (ms)"
        "acquisitionDiscrepancyDeg" -> "Gap between star and compass direction (deg)"
        "failureReason" -> "Last problem reported"
        "blueSeparationDeg" -> "Compass ring error (deg)"
        "greenSeparationDeg" -> "Star-tracker ring error (deg)"
        "allNoLock" -> "Stayed unlocked while covered"
        "falseLocks" -> "False locks during the sweep"
        "relocks" -> "Lock drops and re-locks during the sweep"
        "maxJumpDeg" -> "Biggest jump while locking again (deg)"
        else -> k
    }

    private fun prettyValue(v: Json): String = when (v) {
        is Json.JStr -> v.v
        is Json.JNum -> if (v.v == v.v.toLong().toDouble()) v.v.toLong().toString() else fmt(v.v, 3)
        is Json.JBool -> if (v.v) "Yes" else "No"
        Json.JNull -> "n/a"
        else -> Json.write(v)
    }
}
