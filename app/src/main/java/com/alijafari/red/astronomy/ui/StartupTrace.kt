package com.alijafari.red.astronomy.ui

import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * Launch milestones in logcat under the tag `ZigStartup`, each with the time since the process
 * started, so the launch can be measured on a real device:
 *
 *     adb logcat -s ZigStartup
 *
 * Milestones: activity created, splash first frame, warm-up done, app composed, app drawn
 * (= reportFullyDrawn), splash finished.
 */
object StartupTrace {
    const val TAG = "ZigStartup"

    fun mark(milestone: String) {
        try {
            val sinceProcessStart = SystemClock.uptimeMillis() - Process.getStartUptimeMillis()
            Log.i(TAG, "$milestone at +$sinceProcessStart ms")
        } catch (_: RuntimeException) {
            // Plain JVM unit tests have no clock or logcat behind these stubs.
        }
    }
}
