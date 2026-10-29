package com.alijafari.red.astronomy.ui.rendering

import android.util.Log

/**
 * The one place the renderer reports a problem it survived.
 *
 * A decorative layer that fails must never take the app down: the landscape is a background, and a
 * user staring at a broken river is still better served than a user staring at a crash dialog. Every
 * guard in the drawing code routes here, so a fault is reported once per site (a draw pass runs 60
 * times a second and a repeated warning would flood logcat) with the numbers needed to fix it.
 */
internal object RenderDiagnostics {

    private const val TAG = "ZIG/render"

    private val reported = HashSet<String>(8)

    /** Logs [message] under [label] the first time that label reports anything, then stays quiet. */
    fun warnOnce(label: String, message: String) {
        val fresh = synchronized(reported) { reported.add(label) }
        if (fresh) Log.w(TAG, "$label: $message")
    }

    /**
     * Runs [block], reporting and swallowing a recoverable failure so the rest of the scene keeps
     * drawing.
     *
     * `OutOfMemoryError` is caught as well as `RuntimeException`: both mean "this decoration could
     * not be produced", and neither is improved by reaching the default handler. Other errors are
     * left alone — a stack overflow or a `LinkageError` is not this layer's to hide.
     */
    inline fun <T> attempt(label: String, block: () -> T): T? =
        try {
            block()
        } catch (t: RuntimeException) {
            warnOnce(label, "skipped: ${t.javaClass.simpleName}: ${t.message}")
            null
        } catch (o: OutOfMemoryError) {
            warnOnce(label, "skipped: out of memory building this layer")
            null
        }
}
