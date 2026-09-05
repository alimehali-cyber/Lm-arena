package com.alijafari.red.astronomy.fieldtrial.engine

import kotlin.math.abs

/**
 * G-1.3: the field-trial state machine, document, gating and pass evaluation.
 * Pure Kotlin — harness + CI tested. The Android ViewModel (debug UI commit) wraps
 * this and writes [TrialDocument.toJson] to app-private storage on EVERY change, so
 * process death / reboot restore the exact level and partial data ([Pending]).
 *
 * Versioning: every attempt at a level is APPENDED ([LevelRecord.attempt] = 1,2,...);
 * results are never overwritten. The latest attempt is authoritative for ticks.
 */

enum class LevelStatus { AVAILABLE, NOT_NOW, NOT_APPLICABLE, NOT_IN_BUILD }

enum class SkipReason(val label: String) {
    CLOUDS("Clouds"),
    CANT_FIND("Can't find it"),
    NO_TIME("No time"),
    OTHER("Other")
}

enum class LevelOutcome { PENDING, PASS, FAIL, SKIPPED }

/** Availability of one level, computed from location + time + ephemeris + P0 (1.7). */
data class Gating(
    val status: LevelStatus,
    val reason: String? = null,
    /** for NOT_NOW: the event that unblocks it (UTC ms) + kind for display */
    val whenUtcMs: Long? = null,
    val whenKind: String? = null
)

/** One completed (or skipped) attempt at a level. */
data class LevelRecord(
    val level: Int,
    val attempt: Int,
    val outcome: LevelOutcome,
    val skipReason: SkipReason? = null,
    val startedMs: Long,
    val endedMs: Long? = null,
    val measurements: List<TapMeasurement> = emptyList(),
    val yesNo: Boolean? = null,
    /** auto-recorded numbers (L8-L11): key -> JSON value */
    val auto: Map<String, Json> = emptyMap(),
    val note: String? = null
)

/** Partial data of the level currently open (restored verbatim). */
data class Pending(
    val level: Int,
    val startedMs: Long,
    val measurements: List<TapMeasurement> = emptyList(),
    val auto: Map<String, Json> = emptyMap()
)

data class TrialDocument(
    val trialId: String,
    val createdMs: Long,
    val updatedMs: Long,
    val deviceModel: String = "",
    val deviceSdk: Int = 0,
    val lat: Double? = null,
    val lon: Double? = null,
    /** level currently open (with partial data) — null when no level is open */
    val pending: Pending? = null,
    /** next level the guide should offer (advances on complete/skip) */
    val nextLevel: Int = 0,
    /** versioned attempts per level (never overwritten) */
    val levels: Map<Int, List<LevelRecord>> = emptyMap(),
    /** highest level reached (for the summary ticks) */
    val maxLevel: Int = 0
) {
    fun latest(level: Int): LevelRecord? = levels[level]?.lastOrNull()

    fun attempts(level: Int): Int = levels[level]?.size ?: 0

    fun isDone(level: Int): Boolean = latest(level)?.let { it.outcome != LevelOutcome.PENDING } ?: false

    fun ticks(): List<Pair<Int, LevelOutcome>> =
        ((0..12).mapNotNull { n -> latest(n)?.let { n to it.outcome } })
}

object FieldTrialMachine {

    const val LEVEL_COUNT = 13 // L0..L12

    fun newTrial(trialId: String, nowMs: Long, deviceModel: String, deviceSdk: Int, lat: Double?, lon: Double?): TrialDocument =
        TrialDocument(
            trialId = trialId, createdMs = nowMs, updatedMs = nowMs,
            deviceModel = deviceModel, deviceSdk = deviceSdk, lat = lat, lon = lon,
            pending = null, nextLevel = 0, levels = emptyMap(), maxLevel = 0
        )

    /** Open a level (first time or REDO of any level — append semantics on completion). */
    fun open(doc: TrialDocument, level: Int, nowMs: Long): TrialDocument =
        doc.copy(
            pending = Pending(level = level, startedMs = nowMs),
            updatedMs = nowMs,
            maxLevel = maxOf(doc.maxLevel, level)
        )

    fun addMeasurement(doc: TrialDocument, m: TapMeasurement): TrialDocument {
        val p = doc.pending ?: return doc
        return doc.copy(
            pending = p.copy(measurements = p.measurements + m),
            updatedMs = m.epochMs
        )
    }

    fun addAuto(doc: TrialDocument, key: String, value: Json, nowMs: Long): TrialDocument {
        val p = doc.pending ?: return doc
        val map = LinkedHashMap(p.auto); map[key] = value
        return doc.copy(pending = p.copy(auto = map), updatedMs = nowMs)
    }

    fun addYesNo(doc: TrialDocument, answer: Boolean, nowMs: Long): TrialDocument {
        val p = doc.pending ?: return doc
        // stored as auto + explicit field on completion
        val map = LinkedHashMap(p.auto); map["yesNo"] = Json.JBool(answer)
        return doc.copy(pending = p.copy(auto = map), updatedMs = nowMs)
    }

    /**
     * Complete the open level with the evaluated outcome (evaluation may be computed
     * by the caller via [evaluate] for tap levels, or passed for auto levels).
     * Appends a NEW versioned attempt — never overwrites.
     */
    fun complete(
        doc: TrialDocument,
        outcome: LevelOutcome,
        nowMs: Long,
        note: String? = null,
        skipReason: SkipReason? = null
    ): TrialDocument {
        val p = doc.pending ?: return doc
        val attemptNo = doc.attempts(p.level) + 1
        val yesNo = p.auto["yesNo"]?.asBoolOrNull()
        val record = LevelRecord(
            level = p.level, attempt = attemptNo, outcome = outcome,
            skipReason = skipReason,
            startedMs = p.startedMs, endedMs = nowMs,
            measurements = p.measurements,
            yesNo = yesNo,
            auto = p.auto, note = note
        )
        val levels = LinkedHashMap(doc.levels)
        levels[p.level] = (levels[p.level] ?: emptyList()) + record
        return doc.copy(
            pending = null,
            levels = levels,
            nextLevel = if (outcome == LevelOutcome.PENDING) doc.nextLevel else minOf(p.level + 1, LEVEL_COUNT - 1),
            updatedMs = nowMs
        )
    }

    /** Skip the open level — a reason is REQUIRED (one-tap list). */
    fun skip(doc: TrialDocument, reason: SkipReason, nowMs: Long): TrialDocument =
        complete(doc, LevelOutcome.SKIPPED, nowMs, skipReason = reason)

    // ---- pass evaluation (thresholds from the level spec) ----

    fun evaluate(level: Int, pending: Pending, doc: TrialDocument): LevelOutcome = when (level) {
        0 -> LevelOutcome.PASS // L0 completes by pressing I'm ready
        1 -> {
            val m = pending.measurements.lastOrNull()
            if (m == null) LevelOutcome.FAIL
            else if (abs(m.dAzDeg) < 3.0 && abs(m.dAltDeg) < 3.0) LevelOutcome.PASS else LevelOutcome.FAIL
        }
        2, 3, 5 -> {
            val ms = pending.measurements
            when {
                ms.isEmpty() -> LevelOutcome.FAIL
                level == 5 -> if (ms.all { it.separationDeg < 1.0 }) LevelOutcome.PASS else LevelOutcome.FAIL
                else -> if (ms.last().separationDeg < 1.0) LevelOutcome.PASS else LevelOutcome.FAIL
            }
        }
        4 -> {
            val m = pending.measurements.lastOrNull()
            val l3 = doc.latest(3)?.measurements?.lastOrNull()
            if (m == null || l3 == null) LevelOutcome.FAIL
            else if (m.separationDeg - l3.separationDeg < 0.5) LevelOutcome.PASS else LevelOutcome.FAIL
        }
        6 -> {
            when (pending.auto["yesNo"]?.asBoolOrNull()) {
                true -> LevelOutcome.PASS
                false -> LevelOutcome.FAIL // expected until the attribution text lands
                null -> LevelOutcome.FAIL
            }
        }
        7 -> when (pending.auto["yesNo"]?.asBoolOrNull()) {
            true -> LevelOutcome.PASS
            false -> LevelOutcome.FAIL
            null -> LevelOutcome.FAIL
        }
        8 -> if (pending.auto["firstFullLockMs"] != null) LevelOutcome.PASS else LevelOutcome.FAIL
        9 -> {
            val blue = pending.auto["blueSeparationDeg"]?.asDoubleOrNull()
            val green = pending.auto["greenSeparationDeg"]?.asDoubleOrNull()
            if (blue != null && green != null && green < blue) LevelOutcome.PASS else LevelOutcome.FAIL
        }
        10 -> if (pending.auto["allNoLock"]?.asBoolOrNull() == true) LevelOutcome.PASS else LevelOutcome.FAIL
        11 -> if ((pending.auto["falseLocks"]?.asDoubleOrNull() ?: 1.0) == 0.0) LevelOutcome.PASS else LevelOutcome.FAIL
        12 -> LevelOutcome.PASS
        else -> LevelOutcome.FAIL
    }

    // ---- availability gating (1.7) ----

    /**
     * @param trackerWired P0 verdict (true in this build); Part B levels render
     * "Not available in this build" when false.
     */
    fun gating(level: Int, nowMs: Long, lat: Double, lon: Double, trackerWired: Boolean, l3Done: Boolean): Gating {
        val sunAlt = TargetPicker.sunAltAz(nowMs, lat, lon).altitudeDeg
        return when (level) {
            0, 6, 12 -> Gating(LevelStatus.AVAILABLE)
            1 -> if (sunAlt > 0.0) Gating(LevelStatus.AVAILABLE)
                else Gating(
                    LevelStatus.NOT_NOW, "The Sun is down.",
                    SunEvents.nextSunriseUtcMs(nowMs, lat, lon), "Sun rises"
                )
            2 -> {
                val t = TargetPicker.l2Alternative(nowMs, lat, lon)
                if (t != null) Gating(LevelStatus.AVAILABLE)
                else Gating(LevelStatus.NOT_NOW, "The Moon, Jupiter and Saturn are all below the horizon.",
                    nextMoonriseUtcMs(nowMs, lat, lon), "Moon rises")
            }
            3 -> {
                val t = TargetPicker.brightestStarNow(nowMs, lat, lon)
                when {
                    t != null -> Gating(LevelStatus.AVAILABLE)
                    sunAlt > -6.0 -> Gating(LevelStatus.NOT_NOW, "The sky is still too bright for stars.",
                        SunEvents.nextCivilDuskUtcMs(nowMs, lat, lon), "Gets dark")
                    else -> Gating(LevelStatus.NOT_NOW, "No single bright star qualifies right now (too close to the Moon or too low).")
                }
            }
            4 -> if (l3Done) Gating(LevelStatus.AVAILABLE)
                else Gating(LevelStatus.NOT_NOW, "Finish Level 3 first.")
            5 -> {
                val (up, _) = TargetPicker.sevenStarsNow(nowMs, lat, lon)
                if (up.isNotEmpty()) Gating(LevelStatus.AVAILABLE)
                else Gating(LevelStatus.NOT_NOW, "None of the seven guide stars are high enough tonight.",
                    SunEvents.nextSunriseUtcMs(nowMs, lat, lon), "Try again after")
            }
            7 -> if (lat < 0.0) Gating(LevelStatus.AVAILABLE)
                else Gating(LevelStatus.NOT_APPLICABLE, "You're in the northern hemisphere - this check applies south of the equator.")
            8, 9, 10, 11 -> if (trackerWired) Gating(LevelStatus.AVAILABLE)
                else Gating(LevelStatus.NOT_IN_BUILD, "The star tracker is not available in this build.")
            else -> Gating(LevelStatus.AVAILABLE)
        }
    }

    /** Next moonrise by scan+bisection on the real Moon engine (bracket semantics). */
    fun nextMoonriseUtcMs(nowMs: Long, lat: Double, lon: Double): Long? {
        val stepMs = 10L * 60_000L
        var t = nowMs
        var prev = TargetPicker.moonAltAz(t, lat, lon).altitudeDeg
        for (i in 1..(24 * 6)) {
            t += stepMs
            val now = TargetPicker.moonAltAz(t, lat, lon).altitudeDeg
            if (prev <= 0.0 && now > 0.0) {
                var lo = t - stepMs; var hi = t
                repeat(10) {
                    val mid = (lo + hi) / 2
                    if (TargetPicker.moonAltAz(mid, lat, lon).altitudeDeg <= 0.0) lo = mid else hi = mid
                }
                return (lo + hi) / 2
            }
            prev = now
        }
        return null
    }

    // ---- persistence (restore must be exact) ----

    fun toJson(doc: TrialDocument): Json.JObj = jobjOf(
        "schema" to Json.JNum(1.0),
        "trialId" to jstr(doc.trialId),
        "createdMs" to jnum(doc.createdMs),
        "updatedMs" to jnum(doc.updatedMs),
        "deviceModel" to jstr(doc.deviceModel),
        "deviceSdk" to jnum(doc.deviceSdk.toLong()),
        "lat" to jnum(doc.lat),
        "lon" to jnum(doc.lon),
        "nextLevel" to jnum(doc.nextLevel.toDouble()),
        "maxLevel" to jnum(doc.maxLevel.toDouble()),
        "pending" to (doc.pending?.let { pendingToJobj(it) } ?: Json.JNull),
        "levels" to Json.JObj(LinkedHashMap<String, Json>().apply {
            for ((lvl, records) in doc.levels.toSortedMap()) {
                put(lvl.toString(), Json.JArr(records.map { recordToJobj(it) }))
            }
        })
    )

    fun fromJson(j: Json): TrialDocument {
        val o = j.asObjOrNull() ?: throw Json.JsonError("trial document must be an object")
        val pending = (o["pending"]?.asObjOrNull())?.let { p ->
            Pending(
                level = p["level"]!!.asLongOrNull()!!.toInt(),
                startedMs = p["startedMs"]!!.asLongOrNull()!!,
                measurements = (p["measurements"]?.asArrOrNull() ?: Json.JArr(emptyList())).items.map { measurementFromJobj(it) },
                auto = LinkedHashMap<String, Json>().apply {
                    (p["auto"]?.asArrOrNull() ?: Json.JArr(emptyList())).items.forEach { entry ->
                        val e = entry.asObjOrNull()!!
                        put(e["k"]!!.asStringOrNull()!!, e["v"]!!)
                    }
                }
            )
        }
        val levels = LinkedHashMap<Int, List<LevelRecord>>()
        val lv = o["levels"]?.asObjOrNull() ?: Json.JObj(LinkedHashMap())
        for ((k, v) in lv.fields) {
            levels[k.toInt()] = (v.asArrOrNull() ?: Json.JArr(emptyList())).items.map { recordFromJobj(it) }
        }
        return TrialDocument(
            trialId = o["trialId"]!!.asStringOrNull()!!,
            createdMs = o["createdMs"]!!.asLongOrNull()!!,
            updatedMs = o["updatedMs"]!!.asLongOrNull()!!,
            deviceModel = o["deviceModel"]?.asStringOrNull() ?: "",
            deviceSdk = o["deviceSdk"]?.asLongOrNull()?.toInt() ?: 0,
            lat = o["lat"]?.asDoubleOrNull(),
            lon = o["lon"]?.asDoubleOrNull(),
            pending = pending,
            nextLevel = o["nextLevel"]?.asLongOrNull()?.toInt() ?: 0,
            maxLevel = o["maxLevel"]?.asLongOrNull()?.toInt() ?: 0,
            levels = levels
        )
    }

    private fun pendingToJobj(p: Pending): Json.JObj = jobjOf(
        "level" to jnum(p.level.toLong()),
        "startedMs" to jnum(p.startedMs),
        "measurements" to Json.JArr(p.measurements.map { measurementToJobj(it) }),
        "auto" to Json.JArr(p.auto.entries.map { jobjOf("k" to jstr(it.key), "v" to it.value) })
    )

    private fun recordToJobj(r: LevelRecord): Json.JObj = jobjOf(
        "level" to jnum(r.level.toLong()),
        "attempt" to jnum(r.attempt.toLong()),
        "outcome" to jstr(r.outcome.name),
        "skipReason" to jstr(r.skipReason?.name),
        "startedMs" to jnum(r.startedMs),
        "endedMs" to jnum(r.endedMs),
        "measurements" to Json.JArr(r.measurements.map { measurementToJobj(it) }),
        "yesNo" to jbool(r.yesNo),
        "auto" to Json.JArr(r.auto.entries.map { jobjOf("k" to jstr(it.key), "v" to it.value) }),
        "note" to jstr(r.note)
    )

    private fun recordFromJobj(j: Json): LevelRecord {
        val o = j.asObjOrNull() ?: throw Json.JsonError("record must be an object")
        return LevelRecord(
        level = o["level"]!!.asLongOrNull()!!.toInt(),
        attempt = o["attempt"]!!.asLongOrNull()!!.toInt(),
        outcome = LevelOutcome.valueOf(o["outcome"]!!.asStringOrNull()!!),
        skipReason = o["skipReason"]?.asStringOrNull()?.let { SkipReason.valueOf(it) },
        startedMs = o["startedMs"]!!.asLongOrNull()!!,
        endedMs = o["endedMs"]?.asLongOrNull(),
        measurements = (o["measurements"]?.asArrOrNull() ?: Json.JArr(emptyList())).items.map { measurementFromJobj(it) },
        yesNo = o["yesNo"]?.asBoolOrNull(),
        auto = LinkedHashMap<String, Json>().apply {
            (o["auto"]?.asArrOrNull() ?: Json.JArr(emptyList())).items.forEach { entry ->
                val e = entry.asObjOrNull()!!
                put(e["k"]!!.asStringOrNull()!!, e["v"]!!)
            }
        },
        note = o["note"]?.asStringOrNull()
    )
    }

    private fun measurementToJobj(m: TapMeasurement): Json.JObj = jobjOf(
        "epochMs" to jnum(m.epochMs),
        "targetId" to jstr(m.targetId),
        "computedAzDeg" to jnum(m.computedAzDeg), "computedAltDeg" to jnum(m.computedAltDeg),
        "tappedAzDeg" to jnum(m.tappedAzDeg), "tappedAltDeg" to jnum(m.tappedAltDeg),
        "dAzDeg" to jnum(m.dAzDeg), "dAltDeg" to jnum(m.dAltDeg),
        "separationDeg" to jnum(m.separationDeg), "screenOffsetPx" to jnum(m.screenOffsetPx),
        "sensorAzimuthDeg" to jnum(m.sensorAzimuthDeg),
        "sensorAltitudeDeg" to jnum(m.sensorAltitudeDeg),
        "sensorRollDeg" to jnum(m.sensorRollDeg),
        "sensorQuaternion" to Json.JArr(m.sensorQuaternion.map { Json.JNum(it) }),
        "sensorRotationMatrix" to (m.sensorRotationMatrix?.let { rm -> Json.JArr(rm.map { Json.JNum(it.toDouble()) }) } ?: Json.JNull),
        "gpsLat" to jnum(m.gpsLat), "gpsLon" to jnum(m.gpsLon), "gpsAccuracyM" to jnum(m.gpsAccuracyM),
        "intrinsicsTier" to jstr(m.intrinsicsTier),
        "fx" to jnum(m.fx), "fy" to jnum(m.fy), "cx" to jnum(m.cx), "cy" to jnum(m.cy),
        "distortionTier" to jstr(m.distortionTier),
        "k1" to jnum(m.k1), "k2" to jnum(m.k2),
        "appliedDeclinationDeg" to jnum(m.appliedDeclinationDeg),
        "zoomFactor" to jnum(m.zoomFactor),
        "displayRotationDegrees" to jnum(m.displayRotationDegrees.toLong())
    )

    private fun measurementFromJobj(j: Json): TapMeasurement {
        val o = j.asObjOrNull() ?: throw Json.JsonError("measurement must be an object")
        return TapMeasurement(
        epochMs = o["epochMs"]!!.asLongOrNull()!!,
        targetId = o["targetId"]!!.asStringOrNull()!!,
        computedAzDeg = o["computedAzDeg"]!!.asDoubleOrNull()!!,
        computedAltDeg = o["computedAltDeg"]!!.asDoubleOrNull()!!,
        tappedAzDeg = o["tappedAzDeg"]!!.asDoubleOrNull()!!,
        tappedAltDeg = o["tappedAltDeg"]!!.asDoubleOrNull()!!,
        dAzDeg = o["dAzDeg"]!!.asDoubleOrNull()!!,
        dAltDeg = o["dAltDeg"]!!.asDoubleOrNull()!!,
        separationDeg = o["separationDeg"]!!.asDoubleOrNull()!!,
        screenOffsetPx = o["screenOffsetPx"]!!.asDoubleOrNull()!!,
        sensorAzimuthDeg = o["sensorAzimuthDeg"]!!.asDoubleOrNull()!!,
        sensorAltitudeDeg = o["sensorAltitudeDeg"]!!.asDoubleOrNull()!!,
        sensorRollDeg = o["sensorRollDeg"]!!.asDoubleOrNull()!!,
        sensorQuaternion = DoubleArray(4) { o["sensorQuaternion"]!!.asArrOrNull()!!.items[it].asDoubleOrNull()!! },
        sensorRotationMatrix = o["sensorRotationMatrix"]?.asArrOrNull()?.let { a -> FloatArray(a.items.size) { a.items[it].asDoubleOrNull()!!.toFloat() } },
        gpsLat = o["gpsLat"]?.asDoubleOrNull(),
        gpsLon = o["gpsLon"]?.asDoubleOrNull(),
        gpsAccuracyM = o["gpsAccuracyM"]?.asDoubleOrNull(),
        intrinsicsTier = o["intrinsicsTier"]?.asStringOrNull() ?: "",
        fx = o["fx"]?.asDoubleOrNull(), fy = o["fy"]?.asDoubleOrNull(),
        cx = o["cx"]?.asDoubleOrNull(), cy = o["cy"]?.asDoubleOrNull(),
        distortionTier = o["distortionTier"]?.asStringOrNull() ?: "",
        k1 = o["k1"]?.asDoubleOrNull(), k2 = o["k2"]?.asDoubleOrNull(),
        appliedDeclinationDeg = o["appliedDeclinationDeg"]?.asDoubleOrNull() ?: 0.0,
        zoomFactor = o["zoomFactor"]?.asDoubleOrNull() ?: 1.0,
        displayRotationDegrees = o["displayRotationDegrees"]?.asLongOrNull()?.toInt() ?: 0
    )
    }
}
