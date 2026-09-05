package com.alijafari.red.astronomy.fieldtrial

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.alijafari.red.astronomy.fieldtrial.engine.FieldTrialMachine
import com.alijafari.red.astronomy.fieldtrial.engine.Json
import com.alijafari.red.astronomy.fieldtrial.engine.LevelOutcome
import com.alijafari.red.astronomy.fieldtrial.engine.TapMeasurement
import com.alijafari.red.astronomy.fieldtrial.engine.TrialSummary
import com.alijafari.red.astronomy.startracker.tracking.LockConfidence
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * G-1.3 (debug source set): the trial controller — the ViewModel-equivalent store.
 * Holds the [engine.TrialDocument], persists it to app-private storage on EVERY
 * change (filesDir/fieldtrial/trial-<id>.json), restores the newest trial on
 * startup (process death / reboot resume the exact level + partial data), and owns
 * the tracker runtime lifecycle for Part B, the shot/frame stores, and the Share zip.
 *
 * Deviation from the letter of 1.3 (documented): a plain controller owned by the
 * guide singleton instead of an AndroidX ViewModel — the required BEHAVIORS
 * (survive close/background/process-death/reboot via file restore on next open)
 * hold, and are what the UI tests assert.
 */
class FieldTrialController(private val context: Context) {

    val dir: File get() = File(context.filesDir, "fieldtrial").apply { mkdirs() }

    var document = loadNewest() ?: newDocument()
        private set

    /** UI state: card collapsed vs expanded (persisted separately, non-critical). */
    var cardCollapsed = false

    /** Compose revision: bumped on every document mutation so the guide recomposes. */
    val revision = androidx.compose.runtime.mutableStateOf(0)

    val shots = LinkedHashMap<String, ByteArray>()     // name -> PNG bytes (1.8 evidence)
    private var shotCounter = 0

    // ---- document persistence ----

    private fun fileFor(trialId: String) = File(dir, "trial-$trialId.json")

    private fun persist() {
        runCatching {
            fileFor(document.trialId).writeText(Json.write(FieldTrialMachine.toJson(document)))
        }
    }

    private fun loadNewest(): com.alijafari.red.astronomy.fieldtrial.engine.TrialDocument? =
        dir.listFiles { f -> f.name.startsWith("trial-") && f.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() }
            ?.let { f ->
                runCatching {
                    FieldTrialMachine.fromJson(Json.parse(f.readText()))
                }.getOrNull()
            }

    fun newDocument() {
        val stamp = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        document = FieldTrialMachine.newTrial(
            trialId = stamp,
            nowMs = System.currentTimeMillis(),
            deviceModel = android.os.Build.MODEL ?: "unknown",
            deviceSdk = android.os.Build.VERSION.SDK_INT,
            lat = StarTrackerRuntime.state.gps?.latitude,
            lon = StarTrackerRuntime.state.gps?.longitude
        )
        shots.clear()
        shotCounter = 0
        persist()
    }

    private fun <T> mutate(block: () -> T): T {
        val r = block()
        persist()
        revision.value = revision.value + 1
        return r
    }

    fun open(level: Int) = mutate { document = FieldTrialMachine.open(document, level, System.currentTimeMillis()) }

    fun addMeasurement(m: TapMeasurement) = mutate { document = FieldTrialMachine.addMeasurement(document, m) }

    fun addAuto(key: String, value: Json) =
        mutate { document = FieldTrialMachine.addAuto(document, key, value, System.currentTimeMillis()) }

    fun addYesNo(answer: Boolean) =
        mutate { document = FieldTrialMachine.addYesNo(document, answer, System.currentTimeMillis()) }

    fun complete(outcome: LevelOutcome, note: String? = null) =
        mutate {
            document = FieldTrialMachine.complete(
                document, outcome, System.currentTimeMillis(),
                note = note, skipReason = null
            )
        }

    fun skip(reason: com.alijafari.red.astronomy.fieldtrial.engine.SkipReason) =
        mutate { document = FieldTrialMachine.skip(document, reason, System.currentTimeMillis()) }

    fun evaluateOpen(): LevelOutcome =
        document.pending?.let { FieldTrialMachine.evaluate(it.level, it, document) } ?: LevelOutcome.FAIL

    // ---- evidence shots (1.8) ----

    fun addShot(prefix: String, png: ByteArray): String {
        val name = "%s-%02d.png".format(prefix, ++shotCounter)
        shots[name] = png
        return name
    }

    // ---- Part B lifecycle (L8+) ----

    fun trackerIsOn(): Boolean = StarTrackerRuntime.isOn.get()

    fun trackerTurnOn(onError: (String) -> Unit) {
        val host = com.alijafari.red.astronomy.startracker.debug.FieldTrialHost.access() ?: return
        val obs = com.alijafari.red.astronomy.startracker.debug.FieldTrialHost.observer
            ?: StarTrackerRuntime.state.observer
        if (obs == null) { onError("camera not available yet - open the AR screen first"); return }
        StarTrackerRuntime.turnOn(
            context = host.context,
            observer = obs,
            orientationProvider = host.orientationProvider,
            gps = com.alijafari.red.astronomy.startracker.debug.FieldTrialHost.frame?.gps,
            onReady = {},
            onError = onError
        )
    }

    fun trackerTurnOff() = StarTrackerRuntime.turnOff(context)

    // ---- Share zip (L12) ----

    /** Build trial.json + summary.md + shots/ + frames/ into a zip in cacheDir; returns it. */
    fun buildZip(): File {
        val zip = File(context.cacheDir, "fieldtrial-${document.trialId}.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { zos ->
            zos.putNextEntry(ZipEntry("trial.json"))
            zos.write(Json.write(FieldTrialMachine.toJson(document)).toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("summary.md"))
            zos.write(TrialSummary.generate(document).toByteArray())
            zos.closeEntry()
            for ((name, png) in shots) {
                zos.putNextEntry(ZipEntry("shots/$name"))
                zos.write(png)
                zos.closeEntry()
            }
            StarTrackerRuntime.state.captures.forEachIndexed { i, cap ->
                val png = com.alijafari.red.astronomy.fieldtrial.engine.GrayPng.encode(cap.width, cap.height, cap.gray)
                zos.putNextEntry(ZipEntry("frames/frame-%02d.png".format(i + 1)))
                zos.write(png)
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("frames/frame-%02d.json".format(i + 1)))
                zos.write(frameSidecar(cap).toByteArray())
                zos.closeEntry()
            }
        }
        return zip
    }

    private fun frameSidecar(cap: StarTrackerRuntime.CapturedFrame): String = Json.write(
        com.alijafari.red.astronomy.fieldtrial.engine.jobjOf(
            "epochMs" to Json.JNum(cap.epochMs.toDouble()),
            "imageTsNanos" to Json.JNum(cap.imageTsNanos.toDouble()),
            "rotationDegrees" to Json.JNum(cap.rotationDegrees.toDouble()),
            "width" to Json.JNum(cap.width.toDouble()),
            "height" to Json.JNum(cap.height.toDouble()),
            "gpsLat" to com.alijafari.red.astronomy.fieldtrial.engine.jnum(cap.gps?.latitude),
            "gpsLon" to com.alijafari.red.astronomy.fieldtrial.engine.jnum(cap.gps?.longitude),
            "acquisitionDiscrepancyDeg" to com.alijafari.red.astronomy.fieldtrial.engine.jnum(cap.acquisitionDiscrepancyDeg),
            "attitudeSnapshot" to (cap.attitudeSnapshot?.let { a ->
                Json.JArr(a.map { Json.JNum(it.toDouble()) })
            } ?: Json.JNull)
        )
    )

    fun shareIntent(): Intent? = runCatching {
        val zip = buildZip()
        val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fieldtrial_files", zip)
        Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()
}

/**
 * Pure Part-B run analysis lives in engine.PartBAnalysis (harness/CI unit-tested);
 * these adapters convert the runtime's ProbeSample to the pure Sample shape.
 */
object PartBAnalysisAdapter {
    fun allNoLock(locks: List<LockConfidence>): Boolean = com.alijafari.red.astronomy.fieldtrial.engine.PartBAnalysis.allNoLock(locks)

    fun falseLocks(samples: List<StarTrackerRuntime.ProbeSample>): Int =
        com.alijafari.red.astronomy.fieldtrial.engine.PartBAnalysis.falseLocks(
            samples.map { com.alijafari.red.astronomy.fieldtrial.engine.PartBAnalysis.Sample(it.lock, it.acquisitionDiscrepancyDeg) }
        )

    fun relocks(samples: List<StarTrackerRuntime.ProbeSample>): Int =
        com.alijafari.red.astronomy.fieldtrial.engine.PartBAnalysis.relocks(
            samples.map { com.alijafari.red.astronomy.fieldtrial.engine.PartBAnalysis.Sample(it.lock, it.acquisitionDiscrepancyDeg) }
        )
}
