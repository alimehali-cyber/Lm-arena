package com.alijafari.red.astronomy.debug

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.alijafari.red.astronomy.startracker.diagnostics.TrialLogLine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * D3 (debug-diagnostics pass, 2026-09-04): app-private JSON-lines trial logger.
 * DEBUG SOURCE SET ONLY — not compiled into release. Files live under
 * filesDir/startracker-trials/ (app-private storage); the share action sends the
 * file CONTENT as text (no file provider, no manifest change, nothing leaves the
 * device unless the tester explicitly shares).
 */
class TrialLogger(private val context: Context) {

    private var file: File? = null
    private var lineCount = 0

    val activeFile: File? get() = file
    val lines: Int get() = lineCount

    fun start(): File {
        val dir = File(context.filesDir, "startracker-trials").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val f = File(dir, "trial-$stamp.jsonl")
        file = f
        lineCount = 0
        return f
    }

    fun append(line: TrialLogLine): Boolean = runCatching {
        val f = file ?: return false
        f.appendText(line.toJson() + "\n")
        lineCount++
        true
    }.getOrDefault(false)

    fun content(): String? = file?.takeIf { it.exists() }?.readText()

    fun shareIntent(): Intent? = content()?.let { text ->
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "startracker trial log (${file?.name})")
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    companion object {
        /** Best last-known location (no permission prompt; null if none/granted-not). */
        fun lastKnownGps(context: Context): Location? {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) return null
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val candidates = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            return candidates.maxByOrNull { it.time }
        }
    }
}
