package com.alijafari.red.astronomy.astro_engine

import android.util.Log
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Locale
import kotlin.math.*

/**
 * High-precision physical Starlink "String of Pearls" train detector and fast
 * two-stage SGP4 pre-selector for visible individual Starlink passes and real-time
 * overhead constellation tracking.
 *
 * Strictly preserves 60 FPS UI performance by pre-filtering the ~7,000-satellite
 * Starlink catalog on background worker threads into compact shortlists fed into
 * the canonical [SatelliteEngine] and [ISSEngine] pipelines.
 */
object StarlinkTrainManager {

    private const val TAG = "StarlinkTrainManager"

    const val MAX_ACTIVE_TRAINS = 3
    const val TOP_RECENT_COSPAR_LAUNCH_LIMIT = 8
    const val MIN_TRAIN_SATELLITES = 5
    const val MAX_TRAIN_SPAN_DEG = 110.0
    const val MAX_MEDIAN_PEARL_SPACING_DEG = 8.0
    const val MIN_TRAIN_MEAN_MOTION = 15.00
    const val MAX_TRAIN_MEAN_MOTION = 16.75
    const val FALLBACK_MIN_MEAN_MOTION = 15.05
    const val MAX_SUP_EPOCH_AGE_DAYS = 5.0
    const val DEFAULT_TOP_VISIBLE_PASS_LIMIT = 18
    const val DEFAULT_REALTIME_OVERHEAD_LIMIT = 28
    const val REALTIME_SNAPSHOT_TTL_MS = 12_000L

    private const val WGS84_A_KM = 6378.137
    private const val WGS84_E2 = 0.00669437999014
    private const val EARTH_SHADOW_RADIUS_SQ = 6371.0 * 6371.0
    private val SIN_40_SQ = sin(Math.toRadians(40.0)).let { it * it }
    private const val MU_EARTH_KM3_S2 = 398600.4418

    /**
     * Represents a verified physical Starlink "String of Pearls" deployment train
     * (`angularSpanDeg <= 110.0°` and median pearl spacing `<= 8.0°`, or newest-launch fallback),
     * ordered from Leader (first to rise) to Trailer.
     */
    data class StarlinkTrain(
        val id: String,
        val groupKey: String,
        val displayLabelEn: String,
        val displayLabelFa: String,
        val angularSpanDeg: Double,
        val medianRaanDeg: Double,
        val avgAltitudeKm: Double,
        val memberTles: List<TLEData>,
        val leaderItem: SatelliteItem,
        val memberItems: List<SatelliteItem>,
        val launchKey: Int? = null,
        val medianSpacingDeg: Double = 0.0
    ) {
        val satelliteCount: Int get() = memberTles.size
    }

    /**
     * Real-time background shortlist refreshed every ~12s for Live Map and AR Sky.
     */
    data class StarlinkRealtimeSnapshot(
        val computedAtMs: Long,
        val observerLatDeg: Double,
        val observerLonDeg: Double,
        val activeTrains: List<StarlinkTrain>,
        val overheadStarlinks: List<SatelliteItem>
    )

    internal data class ParsedStarlinkOrbital(
        val tle: TLEData,
        val noradId: Int,
        val cosparLaunch5: String,
        val cosparFull: String,
        val epochYear: Int,
        val epochDay: Double,
        val epochJd: Double,
        val inclinationDeg: Double,
        val raanDeg: Double,
        val eccentricity: Double,
        val argPerigeeDeg: Double,
        val meanAnomalyDeg: Double,
        val meanMotion: Double,
        val sgp4Tle: SGP4Propagator.TLEData
    )

    @Volatile
    private var cachedParsedKey: Triple<Int, String, String>? = null

    @Volatile
    private var cachedParsedList: List<ParsedStarlinkOrbital> = emptyList()

    @Volatile
    private var cachedRealtimeSnapshot: StarlinkRealtimeSnapshot? = null

    private val realtimeLock = Any()

    /**
     * Parses standard 5-digit NORAD IDs ("25544") as well as Space-Track / CelesTrak
     * Alpha-5 6-digit NORAD IDs ("A0001" = 100001 .. "Z9999" = 339999) used for
     * 2024–2026+ Starlink launches.
     */
    fun parseNoradId(rawToken: String): Int? {
        val token = rawToken.trim()
        if (token.isEmpty()) return null
        token.toIntOrNull()?.let { return if (it > 0) it else null }
        if (token.length == 5) {
            val prefixChar = token[0].uppercaseChar()
            val suffix = token.substring(1).toIntOrNull() ?: return null
            val prefixVal = when (prefixChar) {
                in 'A'..'H' -> 10 + (prefixChar - 'A')          // 10..17
                in 'J'..'N' -> 18 + (prefixChar - 'J')          // 18..22 (skips 'I')
                in 'P'..'Z' -> 23 + (prefixChar - 'P')          // 23..33 (skips 'O')
                'I' -> 34                                       // synthetic / pre-launch fallback
                'O' -> 35
                else -> return null
            }
            return prefixVal * 10_000 + suffix
        }
        return null
    }

    fun extractNoradId(line1: String): Int {
        return if (line1.length >= 7) {
            parseNoradId(line1.substring(2, 7)) ?: 0
        } else {
            0
        }
    }

    /**
     * Extracts full COSPAR international designator from TLE Line 1 cols 10–17 (0-based 9..17),
     * e.g. "98067A" or "26042AA".
     */
    fun extractCosparId(line1: String): String {
        return if (line1.length >= 17) line1.substring(9, 17).trim() else ""
    }

    /**
     * Extracts 5-digit COSPAR launch batch designator (YYNNN) from TLE Line 1 cols 10–14 (0-based 9..14),
     * e.g. "26042" for launch 2026-042.
     */
    fun extractCosparLaunchGroup(line1: String): String {
        return if (line1.length >= 14) line1.substring(9, 14).trim() else ""
    }

    /**
     * Parses a real US Space Force 5-digit COSPAR launch designator (`YYNNN`, where `NNN in 001..499`,
     * e.g. `"26068"` -> `2026068`, `"25034"` -> `2025034`).
     *
     * Returns `null` for blank, `"UNKNOWN"`, `"TBA"`, placeholder (`000`), OR CelesTrak 500-series
     * synthetic supplemental launch numbers (`NNN >= 500`, e.g. `26501..26999`) so synthetic `sup-gp`
     * IDs never consume slots in `topRecentLaunchKeys` and instead route into the uncataloged
     * supplemental proximity cluster pipeline.
     */
    fun parseCosparLaunchKey(cospar5: String, cosparFull: String = ""): Int? {
        val fullUpper = cosparFull.trim().uppercase(Locale.US)
        if (fullUpper == "UNKNOWN" || fullUpper.startsWith("TBA")) return null
        val trimmed = cospar5.trim()
        if (trimmed.length != 5 || !trimmed.all { it.isDigit() }) return null
        val yy = trimmed.substring(0, 2).toIntOrNull() ?: return null
        val nnn = trimmed.substring(2, 5).toIntOrNull() ?: return null
        if (nnn !in 1..499) return null
        val fullYear = if (yy < 57) 2000 + yy else 1900 + yy
        return fullYear * 1000 + nnn
    }

    fun formatCosparLaunchDisplay(cospar5: String): String {
        if (cospar5.length == 5 && cospar5.all { it.isDigit() }) {
            val yy = cospar5.substring(0, 2).toIntOrNull() ?: return cospar5
            val nnn = cospar5.substring(2, 5)
            val fullYear = if (yy < 57) 2000 + yy else 1900 + yy
            return "$fullYear-$nnn"
        }
        return cospar5
    }

    fun extractMeanMotion(line2: String): Double {
        return try {
            if (line2.length >= 63) line2.substring(52, 63).trim().toDouble() else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    fun extractInclinationDeg(line2: String): Double {
        return try {
            if (line2.length >= 16) line2.substring(8, 16).trim().toDouble() else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    fun extractRaanDeg(line2: String): Double {
        return try {
            if (line2.length >= 25) line2.substring(17, 25).trim().toDouble() else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    fun extractEccentricity(line2: String): Double {
        return try {
            if (line2.length >= 33) ("0." + line2.substring(26, 33).trim()).toDouble() else 0.0001
        } catch (_: Exception) {
            0.0001
        }
    }

    fun extractArgPerigeeDeg(line2: String): Double {
        return try {
            if (line2.length >= 42) line2.substring(34, 42).trim().toDouble() else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    fun extractMeanAnomalyDeg(line2: String): Double {
        return try {
            if (line2.length >= 51) line2.substring(43, 51).trim().toDouble() else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    fun extractEpochYear(line1: String): Int {
        return try {
            if (line1.length >= 20) {
                val yr = line1.substring(18, 20).trim().toInt()
                if (yr < 57) yr + 2000 else yr + 1900
            } else {
                2026
            }
        } catch (_: Exception) {
            2026
        }
    }

    fun extractEpochDay(line1: String): Double {
        return try {
            if (line1.length >= 32) line1.substring(20, 32).trim().toDouble() else 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * Exact TLE epoch (year + 1-based fractional dayOfYear) to Julian Date (UTC).
     *
     * Verified reference points:
     * - `epochToJulian(2026, 1.0)` == `2461041.5` (Jan 1, 2026 00:00 UTC)
     * - `epochToJulian(2026, 317.5)` == `2461358.0` (Nov 13, 2026 12:00 UTC)
     */
    fun epochToJulian(year: Int, dayOfYear: Double): Double {
        val fullYear = when {
            year < 57 -> 2000 + year
            year < 100 -> 1900 + year
            else -> year
        }
        val y = fullYear - 1
        val a = y / 100
        val b = 2 - a + (a / 4)
        val jdJan0 = floor(365.25 * y) + floor(30.6001 * 14) + 1720994.5 + b
        return jdJan0 + dayOfYear
    }

    /**
     * Shortest angular distance on a 360° circle in [0, 180].
     */
    fun circularDistanceDeg(aDeg: Double, bDeg: Double): Double {
        val diff = abs(aDeg - bDeg) % 360.0
        return if (diff > 180.0) 360.0 - diff else diff
    }

    private fun estimateMeanAltitudeKm(meanMotionRevPerDay: Double): Double {
        if (meanMotionRevPerDay <= 0.1) return 550.0
        val nRadSec = meanMotionRevPerDay * (2.0 * Math.PI) / 86400.0
        val semiMajorAxisKm = (MU_EARTH_KM3_S2 / (nRadSec * nRadSec)).pow(1.0 / 3.0)
        return (semiMajorAxisKm - WGS84_A_KM).coerceIn(220.0, 1200.0)
    }

    internal fun getParsedOrbitals(starlinkTles: List<TLEData>): List<ParsedStarlinkOrbital> {
        if (starlinkTles.isEmpty()) return emptyList()
        var contentHash = starlinkTles.size
        for (tle in starlinkTles) {
            contentHash = 31 * contentHash + tle.line1.hashCode()
            contentHash = 31 * contentHash + tle.line2.hashCode()
        }
        val key = Triple(
            contentHash,
            starlinkTles.first().line1,
            starlinkTles.last().line1
        )
        val existingKey = cachedParsedKey
        if (existingKey == key) {
            val cached = cachedParsedList
            if (cached.isNotEmpty()) return cached
        }

        val bStarParser = ISSEngine.defaultEngine
        val parsed = ArrayList<ParsedStarlinkOrbital>(starlinkTles.size)
        for (tle in starlinkTles) {
            val l1 = tle.line1
            val l2 = tle.line2
            if (l1.length < 53 || l2.length < 63) continue
            var noradId = extractNoradId(l1)
            if (noradId <= 0 && l1.length >= 7 && l1.substring(2, 7).trim() == "00000") {
                noradId = 900_000 + ((31 * tle.name.trim().hashCode() + l2.trim().hashCode()) and 0x0FFFF)
            }
            if (noradId <= 0) continue

            val epochYr = if (tle.epochYear != 0) tle.epochYear else extractEpochYear(l1)
            val epochDay = if (tle.epochDay != 0.0) tle.epochDay else extractEpochDay(l1)
            if (epochDay <= 0.0) continue

            val inc = extractInclinationDeg(l2)
            val raan = extractRaanDeg(l2)
            val ecc = extractEccentricity(l2)
            val argp = extractArgPerigeeDeg(l2)
            val ma = extractMeanAnomalyDeg(l2)
            val mm = extractMeanMotion(l2)
            if (mm <= 0.0) continue
            val bStar = bStarParser.parseBStar(l1)

            val epochJd = epochToJulian(epochYr, epochDay)
            parsed.add(
                ParsedStarlinkOrbital(
                    tle = if (tle.epochYear == epochYr && tle.epochDay == epochDay) {
                        tle
                    } else {
                        tle.copy(epochYear = epochYr, epochDay = epochDay)
                    },
                    noradId = noradId,
                    cosparLaunch5 = extractCosparLaunchGroup(l1),
                    cosparFull = extractCosparId(l1),
                    epochYear = epochYr,
                    epochDay = epochDay,
                    epochJd = epochJd,
                    inclinationDeg = inc,
                    raanDeg = ((raan % 360.0) + 360.0) % 360.0,
                    eccentricity = ecc,
                    argPerigeeDeg = argp,
                    meanAnomalyDeg = ma,
                    meanMotion = mm,
                    sgp4Tle = SGP4Propagator.TLEData(
                        epochYear = epochYr,
                        epochDay = epochDay,
                        inclinationDeg = inc,
                        raanDeg = raan,
                        eccentricity = ecc,
                        argPerigeeDeg = argp,
                        meanAnomalyDeg = ma,
                        meanMotion = mm,
                        bStar = bStar
                    )
                )
            )
        }

        cachedParsedList = parsed
        cachedParsedKey = key
        return parsed
    }

    /**
     * Proximity orbital-plane clustering for uncataloged `sup-gp` / `last-30-days` / `YY500+` candidates
     * (`abs(inc1 - inc2) <= 0.6°` and `circularDistanceDeg(raan1, raan2) <= 3.5°`).
     * Eliminates integer-bucket boundary splitting (e.g. a train straddling `123.9°..124.1°` RAAN).
     */
    private fun clusterUncatalogedOrbitals(
        uncataloged: List<ParsedStarlinkOrbital>
    ): List<Pair<String, List<ParsedStarlinkOrbital>>> {
        if (uncataloged.size < MIN_TRAIN_SATELLITES) return emptyList()
        val n = uncataloged.size
        val visited = BooleanArray(n)
        val result = ArrayList<Pair<String, List<ParsedStarlinkOrbital>>>()
        val usedKeys = HashSet<String>()

        for (i in 0 until n) {
            if (visited[i]) continue
            val component = ArrayList<ParsedStarlinkOrbital>()
            val queue = ArrayDeque<Int>()
            visited[i] = true
            queue.addLast(i)

            while (queue.isNotEmpty()) {
                val curIdx = queue.removeFirst()
                val cur = uncataloged[curIdx]
                component.add(cur)
                for (j in 0 until n) {
                    if (!visited[j]) {
                        val other = uncataloged[j]
                        if (abs(cur.inclinationDeg - other.inclinationDeg) <= 0.6 &&
                            circularDistanceDeg(cur.raanDeg, other.raanDeg) <= 3.5
                        ) {
                            visited[j] = true
                            queue.addLast(j)
                        }
                    }
                }
            }

            if (component.size >= MIN_TRAIN_SATELLITES) {
                val sortedInc = component.map { it.inclinationDeg }.sorted()
                val medianInc = sortedInc[sortedInc.size / 2]
                val medianRaan = component.minByOrNull { center ->
                    component.sumOf { circularDistanceDeg(center.raanDeg, it.raanDeg) }
                }?.raanDeg ?: component.first().raanDeg

                val tightPlane = component.filter {
                    abs(it.inclinationDeg - medianInc) <= 0.6 &&
                        circularDistanceDeg(it.raanDeg, medianRaan) <= 3.5
                }
                if (tightPlane.size >= MIN_TRAIN_SATELLITES) {
                    val baseKey = "SUP-${medianInc.roundToInt()}-${medianRaan.roundToInt()}"
                    var uniqueKey = baseKey
                    var suffix = 2
                    while (!usedKeys.add(uniqueKey)) {
                        uniqueKey = "$baseKey-$suffix"
                        suffix++
                    }
                    result.add(uniqueKey to tightPlane)
                }
            }
        }
        return result
    }

    private fun buildStarlinkTrainInstance(
        rawGroupKey: String,
        groupLaunchKey: Int?,
        medianRaan: Double,
        clusterSpanDeg: Double,
        medianSpacingDeg: Double,
        orderedMembers: List<ParsedStarlinkOrbital>
    ): StarlinkTrain {
        val displayGroup = if (rawGroupKey.startsWith("SUP-")) {
            rawGroupKey
        } else {
            formatCosparLaunchDisplay(rawGroupKey)
        }
        val safeIdSuffix = displayGroup.lowercase(Locale.US).replace(Regex("[^a-z0-9_-]"), "_")
        val trainId = "starlink_train_$safeIdSuffix"
        val memberCount = orderedMembers.size
        val avgMeanMotion = orderedMembers.map { it.meanMotion }.average()
        val avgAltKm = estimateMeanAltitudeKm(avgMeanMotion)
        val leaderOrb = orderedMembers.first()
        val spanFormatted = String.format(Locale.US, "%.0f°", clusterSpanDeg)
        val memberCountFa = memberCount.toString().toPersianDigits()
        // The launch-group code is an identifier inside Persian prose, not a translated word.
        val displayGroupFa = "\u2066${displayGroup.toPersianDigits()}\u2069"

        val leaderItem = SatelliteItem(
            id = trainId,
            noradId = leaderOrb.noradId,
            nameEn = "Starlink Train ($displayGroup • $memberCount sats)",
            nameFa = "قطار استارلینک ($displayGroupFa • $memberCountFa ماهواره)",
            category = SatelliteCategory.STARLINK,
            designation = leaderOrb.cosparFull.ifEmpty { displayGroup },
            defaultTle = leaderOrb.tle,
            isConstellation = true,
            isTrain = true,
            trainCount = memberCount,
            starlinkTrainCount = memberCount,
            standardMagnitude = 1.6,
            isNakedEyeCandidate = true,
            descriptionEn = "Active SpaceX deployment train ($displayGroup) of $memberCount Starlink satellites flying in a $spanFormatted string-of-pearls arc.",
            descriptionFa = "قطار فعال استقرار اسپیس‌ایکس ($displayGroupFa) شامل $memberCountFa ماهواره استارلینک در کمان متراکم ${spanFormatted.toPersianDigits()}.",
            launchDate = "Recent deployment ($displayGroup)",
            operatorEn = "SpaceX",
            operatorFa = "اسپیس‌ایکس",
            missionPurposeEn = "Low-Earth-orbit deployment formation prior to ion-thruster orbit raising.",
            missionPurposeFa = "آرایش خطی استقرار در مدار پایین زمین پیش از صعود با پیشران یونی.",
            scientificSignificanceEn = "Observable as a bright 'string of pearls' gliding in unison shortly after dusk or before dawn.",
            scientificSignificanceFa = "قابل مشاهده به صورت «رشته مروارید» درخشان پس از غروب یا پیش از طلوع خورشید.",
            verifiedFactsEn = listOf(
                "Cluster spans $spanFormatted along orbital plane ${String.format(Locale.US, "%.1f°", medianRaan)} RAAN at ~${avgAltKm.roundToInt()} km altitude.",
                "Ordered from leading satellite (${cleanStarlinkNameEn(leaderOrb.tle.name, leaderOrb.noradId)}) to trailing pearl.",
                "Brightest in the first days after launch while solar arrays fly in low-drag 'open-book' attitude."
            ),
            verifiedFactsFa = listOf(
                "گستردگی زاویه‌ای خوشه تنها ${spanFormatted.toPersianDigits()} در ارتفاع حدود ${avgAltKm.roundToInt().toString().toPersianDigits()} کیلومتر است.",
                "مرتب‌شده از ماهواره پیشرو تا آخرین مروارید دنباله‌رو در جهت حرکت مداری.",
                "در روزهای نخست پس از پرتاب به دلیل حالت کم‌پسا بیشترین درخشندگی را دارد."
            )
        )

        val memberItems = orderedMembers.mapIndexed { idx, orb ->
            val cleanEn = cleanStarlinkNameEn(orb.tle.name, orb.noradId)
            val cleanFa = cleanStarlinkNameFa(orb.tle.name, orb.noradId)
            val rank = idx + 1
            val roleEn = if (idx == 0) "Leader #1" else "#$rank/$memberCount"
            val roleFa = if (idx == 0) "پیشرو ۱" else "${rank.toString().toPersianDigits()} از $memberCountFa"
            SatelliteItem(
                id = "starlink_pearl_${orb.noradId}",
                noradId = orb.noradId,
                nameEn = if (idx == 0) "Starlink Train ($displayGroup) • $cleanEn" else "$cleanEn (Train $roleEn)",
                nameFa = if (idx == 0) "قطار استارلینک ($displayGroupFa) • $cleanFa" else "$cleanFa (قطار $roleFa)",
                category = SatelliteCategory.STARLINK,
                designation = orb.cosparFull.ifEmpty { displayGroup },
                defaultTle = orb.tle,
                isConstellation = true,
                isTrain = idx == 0,
                trainCount = memberCount,
                starlinkTrainCount = memberCount,
                standardMagnitude = 1.8,
                isNakedEyeCandidate = true,
                descriptionEn = "Member #$rank of $memberCount in active Starlink Train $displayGroup ($spanFormatted arc).",
                descriptionFa = "عضو شماره ${rank.toString().toPersianDigits()} از $memberCountFa در قطار استارلینک $displayGroupFa.",
                launchDate = "Recent deployment ($displayGroup)",
                operatorEn = "SpaceX",
                operatorFa = "اسپیس‌ایکس",
                missionPurposeEn = "Low-Earth-orbit broadband constellation deployment.",
                missionPurposeFa = "منظومه اینترنت پهن‌باند مدار پایین زمین."
            )
        }

        return StarlinkTrain(
            id = trainId,
            groupKey = displayGroup,
            displayLabelEn = "Starlink Train ($displayGroup)",
            displayLabelFa = "قطار استارلینک ($displayGroupFa)",
            angularSpanDeg = clusterSpanDeg,
            medianRaanDeg = medianRaan,
            avgAltitudeKm = avgAltKm,
            memberTles = orderedMembers.map { it.tle },
            leaderItem = leaderItem,
            memberItems = memberItems,
            launchKey = groupLaunchKey,
            medianSpacingDeg = medianSpacingDeg
        )
    }

    /**
     * Detects up to [maxTrains] genuine physical Starlink / Starship V3 "String of Pearls" deployment trains,
     * sorted primarily by newest launch batch (`SUP` launch-day batches and highest `YYYY-NNN`
     * COSPAR launch designator first) and secondarily by tightest [StarlinkTrain.angularSpanDeg].
     *
     * Enforces:
     * 1. Strict COSPAR Launch Recency Gate (top 8 real US Space Force `001..499` COSPAR launch batches,
     *    routing `>= 500` synthetic `sup-gp` IDs to supplemental clustering, and hard-rejecting prior-year
     *    launches such as 2024/2025 Direct-to-Cell shells).
     * 2. Physical train cluster window (`>= 5` coplanar satellites, `angularSpanDeg <= 110.0°`,
     *    median inter-pearl spacing `<= 8.0°`, `meanMotion in 15.00..16.75`).
     * 3. Guaranteed Newest-Launch Fallback: if no batch fits `<= 110.0°`, selects the newest current-year
     *    launch batch (or newest fresh `SUP` cluster with `>= 5` coplanar satellites and `meanMotion >= 15.05`)
     *    and returns its densest contiguous `5..26` satellite arc.
     */
    fun detectActiveTrains(
        starlinkTles: List<TLEData>,
        nowMs: Long = System.currentTimeMillis(),
        maxTrains: Int = MAX_ACTIVE_TRAINS
    ): List<StarlinkTrain> {
        if (starlinkTles.isEmpty()) return emptyList()

        val nowJd = (nowMs / 86_400_000.0) + 2440587.5
        val orbitals = getParsedOrbitals(starlinkTles)
        if (orbitals.isEmpty()) return emptyList()

        val calUtc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = nowMs
        }
        val currentYear = calUtc.get(java.util.Calendar.YEAR)
        val currentDayOfYear = calUtc.get(java.util.Calendar.DAY_OF_YEAR)

        // Step 1: Compute the Top 8 Most Recent Real US Space Force COSPAR Launch Batches (NNN in 001..499)
        val launchBatchCounts = HashMap<Int, Int>()
        for (orb in orbitals) {
            val lk = parseCosparLaunchKey(orb.cosparLaunch5, orb.cosparFull) ?: continue
            val launchYr = lk / 1000
            if (launchYr <= currentYear + 1) {
                launchBatchCounts[lk] = (launchBatchCounts[lk] ?: 0) + 1
            }
        }
        val multiSatLaunchKeys = launchBatchCounts.entries
            .filter { it.value >= MIN_TRAIN_SATELLITES }
            .map { it.key }
            .sortedDescending()
        val distinctCatalogLaunchKeys = if (multiSatLaunchKeys.isNotEmpty()) {
            multiSatLaunchKeys
        } else {
            launchBatchCounts.keys.sortedDescending()
        }
        val topRecentLaunchKeys = distinctCatalogLaunchKeys
            .take(TOP_RECENT_COSPAR_LAUNCH_LIMIT)
            .toSet()
        val newestCatalogYear = distinctCatalogLaunchKeys.firstOrNull()?.div(1000) ?: currentYear
        val referenceYear = max(newestCatalogYear, currentYear)

        fun passesLaunchRecencyGate(orb: ParsedStarlinkOrbital, launchKey: Int): Boolean {
            // Hard Reject #1: Must be among the top 8 most recent cataloged Starlink launch batches
            if (launchKey !in topRecentLaunchKeys) return false

            val launchYear = launchKey / 1000
            val launchNumber = launchKey % 1000

            // Hard Reject #2: Reject prior-year launches (e.g. 2024/2025 when in 2026) unless
            // we are in early January (day-of-year <= 20) AND the launch was in late December of Y - 1.
            if (launchYear < referenceYear) {
                val isEarlyJanRollover = launchYear == referenceYear - 1 &&
                    currentDayOfYear <= 20 &&
                    (launchNumber >= 180 || (orb.epochYear == referenceYear - 1 && orb.epochDay >= 344.0))
                if (!isEarlyJanRollover) return false
            }
            return true
        }

        // Step 2: Filter candidates by fresh epoch age and LEO deployment mean motion [15.00, 16.75] rev/day,
        // routing real COSPAR (001..499) through Top-8 Recency Gate and uncataloged / YY500+ through SUP (epochAge <= 5.0d).
        val catalogedByCospar = LinkedHashMap<String, MutableList<ParsedStarlinkOrbital>>()
        val uncatalogedCandidates = ArrayList<ParsedStarlinkOrbital>()

        for (orb in orbitals) {
            val epochAgeDays = nowJd - orb.epochJd
            if (orb.meanMotion !in MIN_TRAIN_MEAN_MOTION..MAX_TRAIN_MEAN_MOTION) continue

            val launchKey = parseCosparLaunchKey(orb.cosparLaunch5, orb.cosparFull)
            if (launchKey != null) {
                if (epochAgeDays in -1.0..10.0 && passesLaunchRecencyGate(orb, launchKey)) {
                    catalogedByCospar.getOrPut(orb.cosparLaunch5) { ArrayList() }.add(orb)
                }
            } else if (epochAgeDays in -1.0..MAX_SUP_EPOCH_AGE_DAYS) {
                uncatalogedCandidates.add(orb)
            }
        }

        val candidateGroups = ArrayList<Triple<String, Int?, List<ParsedStarlinkOrbital>>>()
        for ((cospar5, list) in catalogedByCospar) {
            if (list.size >= MIN_TRAIN_SATELLITES) {
                candidateGroups.add(Triple(cospar5, parseCosparLaunchKey(cospar5), list))
            }
        }
        for ((supKey, list) in clusterUncatalogedOrbitals(uncatalogedCandidates)) {
            candidateGroups.add(Triple(supKey, null, list))
        }
        if (candidateGroups.isEmpty()) {
            SatelliteCatalog.clearPrimaryTrain()
            return emptyList()
        }

        data class ProjectedMember(
            val orbital: ParsedStarlinkOrbital,
            val uDeg: Double
        )

        val detected = ArrayList<StarlinkTrain>()

        for ((rawGroupKey, groupLaunchKey, group) in candidateGroups) {
            if (group.size < MIN_TRAIN_SATELLITES) continue

            // Step 3: Coplanar check — circular median RAAN + >= 5 satellites within 3.5°
            val medianRaan = group.minByOrNull { center ->
                group.sumOf { circularDistanceDeg(center.raanDeg, it.raanDeg) }
            }?.raanDeg ?: continue

            val coplanar = group.filter { circularDistanceDeg(it.raanDeg, medianRaan) <= 3.5 }
            if (coplanar.size < MIN_TRAIN_SATELLITES) continue

            // Step 4: Project along-track Argument of Latitude u to nowJd
            val projected = coplanar.map { orb ->
                val dtDays = nowJd - orb.epochJd
                val u = ((orb.argPerigeeDeg + orb.meanAnomalyDeg + orb.meanMotion * 360.0 * dtDays) % 360.0 + 360.0) % 360.0
                ProjectedMember(orb, u)
            }.sortedBy { it.uDeg }

            // Step 5: Trim up to 15% outliers by finding the densest contiguous core of max(5, floor(0.85 * N)) satellites
            val n = projected.size
            val coreCount = max(MIN_TRAIN_SATELLITES, (n * 0.85).toInt()).coerceAtMost(n)
            var bestStartIdx = 0
            var minSpanDeg = Double.POSITIVE_INFINITY

            for (startIdx in 0 until n) {
                val endIdx = (startIdx + coreCount - 1) % n
                val spanDeg = if (coreCount <= 1) {
                    0.0
                } else if (startIdx + coreCount - 1 < n) {
                    projected[endIdx].uDeg - projected[startIdx].uDeg
                } else {
                    (360.0 - projected[startIdx].uDeg) + projected[endIdx].uDeg
                }
                if (spanDeg < minSpanDeg) {
                    minSpanDeg = spanDeg
                    bestStartIdx = startIdx
                }
            }

            // Physical Train Span Rule: 85% core cluster must fit within <= 110° of orbital arc
            if (minSpanDeg > MAX_TRAIN_SPAN_DEG) {
                Log.d(TAG, "Rejected dispersed group $rawGroupKey: count=${coplanar.size}, coreSpan=${"%.1f".format(Locale.US, minSpanDeg)}° > ${MAX_TRAIN_SPAN_DEG}°")
                continue
            }

            // Pearl Density Rule: Median adjacent inter-satellite spacing along core cluster must be <= 8.0°
            val coreGaps = DoubleArray((coreCount - 1).coerceAtLeast(0)) { k ->
                val curU = projected[(bestStartIdx + k) % n].uDeg
                val nxtU = projected[(bestStartIdx + k + 1) % n].uDeg
                ((nxtU - curU) % 360.0 + 360.0) % 360.0
            }.sortedArray()
            val medianSpacingDeg = when {
                coreGaps.isEmpty() -> 0.0
                coreGaps.size % 2 == 1 -> coreGaps[coreGaps.size / 2]
                else -> 0.5 * (coreGaps[coreGaps.size / 2 - 1] + coreGaps[coreGaps.size / 2])
            }
            if (medianSpacingDeg > MAX_MEDIAN_PEARL_SPACING_DEG) {
                Log.d(
                    TAG,
                    "Rejected low-density sector $rawGroupKey: medianSpacing=${"%.2f".format(Locale.US, medianSpacingDeg)}° > ${MAX_MEDIAN_PEARL_SPACING_DEG}°"
                )
                continue
            }

            // Expand contiguously from the verified core to retain genuine adjacent pearls (gap <= 12.0°)
            // while still trimming straggler/debris outliers and keeping total span <= 110°.
            var clusterStartIdx = bestStartIdx
            var clusterCount = coreCount
            var clusterSpanDeg = minSpanDeg
            while (clusterCount < n) {
                val lastIdx = (clusterStartIdx + clusterCount - 1) % n
                val nextIdx = (clusterStartIdx + clusterCount) % n
                val fwdGap = ((projected[nextIdx].uDeg - projected[lastIdx].uDeg) % 360.0 + 360.0) % 360.0

                val prevIdx = (clusterStartIdx - 1 + n) % n
                val bwdGap = ((projected[clusterStartIdx].uDeg - projected[prevIdx].uDeg) % 360.0 + 360.0) % 360.0

                if (fwdGap <= bwdGap && fwdGap <= 12.0 && clusterSpanDeg + fwdGap <= MAX_TRAIN_SPAN_DEG) {
                    clusterCount++
                    clusterSpanDeg += fwdGap
                } else if (bwdGap <= 12.0 && clusterSpanDeg + bwdGap <= MAX_TRAIN_SPAN_DEG) {
                    clusterStartIdx = prevIdx
                    clusterCount++
                    clusterSpanDeg += bwdGap
                } else {
                    break
                }
            }

            val orderedMembers = (clusterCount - 1 downTo 0).map { offset ->
                projected[(clusterStartIdx + offset) % n].orbital
            }

            detected.add(
                buildStarlinkTrainInstance(
                    rawGroupKey = rawGroupKey,
                    groupLaunchKey = groupLaunchKey,
                    medianRaan = medianRaan,
                    clusterSpanDeg = clusterSpanDeg,
                    medianSpacingDeg = medianSpacingDeg,
                    orderedMembers = orderedMembers
                )
            )
        }

        // Step 7: Guaranteed Newest-Launch Fallback — if no train met the 110° gate, select the single
        // newest current-year launch batch (max launchKey or newest fresh SUP cluster) with >= 5 coplanar
        // orbit-raising satellites (meanMotion >= 15.05) and extract its densest contiguous 5..26 satellite arc.
        if (detected.isEmpty()) {
            val fallbackCandidates = candidateGroups
                .mapNotNull { (rawKey, lk, group) ->
                    val raising = group.filter { it.meanMotion >= FALLBACK_MIN_MEAN_MOTION }
                    if (raising.size < MIN_TRAIN_SATELLITES) return@mapNotNull null
                    val medRaan = raising.minByOrNull { center ->
                        raising.sumOf { circularDistanceDeg(center.raanDeg, it.raanDeg) }
                    }?.raanDeg ?: return@mapNotNull null
                    val coplanar = raising.filter { circularDistanceDeg(it.raanDeg, medRaan) <= 3.5 }
                    if (coplanar.size < MIN_TRAIN_SATELLITES) return@mapNotNull null
                    Triple(rawKey, lk, medRaan to coplanar)
                }
                .sortedWith(
                    compareByDescending<Triple<String, Int?, Pair<Double, List<ParsedStarlinkOrbital>>>> {
                        it.second ?: Int.MAX_VALUE
                    }.thenByDescending { entry ->
                        entry.third.second.maxOfOrNull { it.epochJd } ?: 0.0
                    }
                )

            val newestBatch = fallbackCandidates.firstOrNull()
            if (newestBatch != null) {
                val (rawKey, lk, planePair) = newestBatch
                val (medRaan, coplanar) = planePair
                val projected = coplanar.map { orb ->
                    val dtDays = nowJd - orb.epochJd
                    val u = ((orb.argPerigeeDeg + orb.meanAnomalyDeg + orb.meanMotion * 360.0 * dtDays) % 360.0 + 360.0) % 360.0
                    ProjectedMember(orb, u)
                }.sortedBy { it.uDeg }

                val n = projected.size
                val maxWindow = n.coerceIn(MIN_TRAIN_SATELLITES, 26)
                var chosenCount = MIN_TRAIN_SATELLITES
                var chosenStartIdx = 0
                var chosenSpanDeg = Double.POSITIVE_INFINITY

                // Prefer the largest contiguous 5..26 satellite arc fitting within <= 180°,
                // or the tightest 5-satellite contiguous sub-arc if spread wider than 180°.
                for (winSize in maxWindow downTo MIN_TRAIN_SATELLITES) {
                    var bestIdx = 0
                    var bestSpan = Double.POSITIVE_INFINITY
                    for (startIdx in 0 until n) {
                        val endIdx = (startIdx + winSize - 1) % n
                        val span = if (startIdx + winSize - 1 < n) {
                            projected[endIdx].uDeg - projected[startIdx].uDeg
                        } else {
                            (360.0 - projected[startIdx].uDeg) + projected[endIdx].uDeg
                        }
                        if (span < bestSpan) {
                            bestSpan = span
                            bestIdx = startIdx
                        }
                    }
                    if (bestSpan <= 180.0 || winSize == MIN_TRAIN_SATELLITES) {
                        chosenCount = winSize
                        chosenStartIdx = bestIdx
                        chosenSpanDeg = bestSpan
                        break
                    }
                }

                val gaps = DoubleArray((chosenCount - 1).coerceAtLeast(0)) { k ->
                    val curU = projected[(chosenStartIdx + k) % n].uDeg
                    val nxtU = projected[(chosenStartIdx + k + 1) % n].uDeg
                    ((nxtU - curU) % 360.0 + 360.0) % 360.0
                }.sortedArray()
                val medGap = when {
                    gaps.isEmpty() -> 0.0
                    gaps.size % 2 == 1 -> gaps[gaps.size / 2]
                    else -> 0.5 * (gaps[gaps.size / 2 - 1] + gaps[gaps.size / 2])
                }

                val orderedMembers = (chosenCount - 1 downTo 0).map { offset ->
                    projected[(chosenStartIdx + offset) % n].orbital
                }

                detected.add(
                    buildStarlinkTrainInstance(
                        rawGroupKey = rawKey,
                        groupLaunchKey = lk,
                        medianRaan = medRaan,
                        clusterSpanDeg = chosenSpanDeg,
                        medianSpacingDeg = medGap,
                        orderedMembers = orderedMembers
                    )
                )
            }
        }

        // Sort primarily by newest launch batch (uncataloged SUP launch-day batches = Int.MAX_VALUE first,
        // then highest YYYYNNN launchKey descending), and secondarily by tightest angularSpanDeg ascending.
        val topTrains = detected
            .sortedWith(
                compareByDescending<StarlinkTrain> { it.launchKey ?: Int.MAX_VALUE }
                    .thenBy { it.angularSpanDeg }
            )
            .take(maxTrains.coerceAtLeast(1))

        if (topTrains.isNotEmpty()) {
            val allItems = topTrains.flatMap { listOf(it.leaderItem) + it.memberItems }
            SatelliteCatalog.registerDynamicStarlinks(
                items = allItems,
                primaryTrain = topTrains.first().leaderItem
            )
            Log.i(
                TAG,
                "Detected ${topTrains.size} active Starlink train(s): " +
                    topTrains.joinToString { "${it.groupKey} (${it.satelliteCount} sats, span=${"%.1f".format(Locale.US, it.angularSpanDeg)}°, spacing=${"%.2f".format(Locale.US, it.medianSpacingDeg)}°)" }
            )
        } else {
            SatelliteCatalog.clearPrimaryTrain()
        }

        return topTrains
    }

    /**
     * Backward-compatible primary train detector returning the tightest active physical
     * Starlink train's leader [SatelliteItem] (with alias support for `"starlink_train"`),
     * or `null` if no physical train (`<= 90°` span) is currently in orbit.
     */
    fun detectTrain(starlinkTles: List<TLEData>): SatelliteItem? {
        val primary = detectActiveTrains(starlinkTles).firstOrNull()?.leaderItem ?: return null
        return primary.copy(id = "starlink_train").also { aliased ->
            SatelliteCatalog.registerDynamicStarlinks(listOf(aliased), primaryTrain = aliased)
        }
    }

    /**
     * Fast two-stage SGP4 pre-selector (designed for [kotlinx.coroutines.Dispatchers.Default])
     * that scans the upcoming dusk and dawn twilight visibility windows (`sunAlt <= -6°`),
     * scores sunlit Starlink passes reaching `elevation >= 40°`, deduplicates by orbital plane
     * and twilight window, and returns the top [maxCount] (15–20) individual [SatelliteItem]s
     * to feed into [ISSEngine.predictPasses].
     */
    fun selectTopVisibleStarlinks(
        starlinkTles: List<TLEData>,
        activeTrains: List<StarlinkTrain> = emptyList(),
        userLatDeg: Double,
        userLonDeg: Double,
        startTimeMs: Long = System.currentTimeMillis(),
        userAltMeters: Double = 0.0,
        maxCount: Int = DEFAULT_TOP_VISIBLE_PASS_LIMIT
    ): List<SatelliteItem> {
        if (starlinkTles.isEmpty() || maxCount <= 0) return emptyList()

        val orbitals = getParsedOrbitals(starlinkTles)
        if (orbitals.isEmpty()) return emptyList()

        val excludedNorads = HashSet<Int>()
        for (train in activeTrains) {
            for (m in train.memberItems) excludedNorads.add(m.noradId)
        }

        // Step 1: Discover upcoming twilight visibility timestamps (Sun in [-26°, -6°], fallback <= -6°)
        val sampleTimestamps = selectTwilightSampleTimestamps(
            userLatDeg = userLatDeg,
            userLonDeg = userLonDeg,
            startTimeMs = startTimeMs
        )
        if (sampleTimestamps.isEmpty()) return emptyList()

        val observer = buildObserverFrame(userLatDeg, userLonDeg, userAltMeters)
        val sampleFrames = sampleTimestamps.map { ts -> buildTimestampFrame(ts) }
        val sgp4 = SGP4Propagator()
        val absUserLat = abs(userLatDeg)

        data class ScoredCandidate(
            val orbital: ParsedStarlinkOrbital,
            val peakElevDeg: Double,
            val score: Double,
            val bestSampleIndex: Int,
            val planeBucket: String
        )

        val candidates = ArrayList<ScoredCandidate>(128)

        for (orb in orbitals) {
            if (orb.noradId in excludedNorads) continue
            // Operational & low-insertion LEO Starlink shells (up to ~180 km Starship/V3 deployment)
            if (orb.meanMotion !in 14.7..MAX_TRAIN_MEAN_MOTION) continue
            // Geometric latitude reachability bound for >= 40° elevation at ~350–600 km
            if (orb.inclinationDeg + 6.5 < absUserLat && (180.0 - orb.inclinationDeg) + 6.5 < absUserLat) {
                continue
            }

            var bestElev = -90.0
            var bestIdx = -1

            for (i in sampleFrames.indices) {
                val sf = sampleFrames[i]
                val tsinceMin = (sf.jd - orb.epochJd) * SGP4Propagator.MINUTES_PER_DAY
                // Skip severely stale TLEs (> 30 days) for pass accuracy
                if (abs(tsinceMin) > 30.0 * 1440.0) continue

                val teme = sgp4.propagateMinutesSinceEpoch(orb.sgp4Tle, tsinceMin)
                val xEcef = teme.xKm * sf.cosGmst + teme.yKm * sf.sinGmst
                val yEcef = -teme.xKm * sf.sinGmst + teme.yKm * sf.cosGmst
                val zEcef = teme.zKm

                val dx = xEcef - observer.xKm
                val dy = yEcef - observer.yKm
                val dz = zEcef - observer.zKm
                val rangeSq = dx * dx + dy * dy + dz * dz
                if (rangeSq <= 1.0 || !rangeSq.isFinite()) continue

                val up = dx * observer.upX + dy * observer.upY + dz * observer.upZ
                // Fast >= 40° elevation pre-check before sqrt/asin/umbra
                if (up <= 0.0 || (up * up) < SIN_40_SQ * rangeSq) continue

                if (!isSunlitFast(xEcef, yEcef, zEcef, sf.sunUnitX, sf.sunUnitY, sf.sunUnitZ)) continue

                val elevDeg = Math.toDegrees(asin((up / sqrt(rangeSq)).coerceIn(-1.0, 1.0)))
                if (elevDeg > bestElev) {
                    bestElev = elevDeg
                    bestIdx = i
                }
            }

            if (bestElev >= 40.0 && bestIdx >= 0) {
                // Prefer zenith passes (closest to 90°) and brighter lower-altitude v2 Mini shells
                val lowOrbitBonus = if (orb.meanMotion >= 15.55) 4.0 else if (orb.meanMotion >= 15.15) 1.5 else 0.0
                val score = bestElev + lowOrbitBonus
                val planeBucket = "${(orb.inclinationDeg / 5.0).toInt()}_${(orb.raanDeg / 10.0).toInt()}"
                candidates.add(
                    ScoredCandidate(
                        orbital = orb,
                        peakElevDeg = bestElev,
                        score = score,
                        bestSampleIndex = bestIdx,
                        planeBucket = planeBucket
                    )
                )
            }
        }

        if (candidates.isEmpty()) return emptyList()

        candidates.sortByDescending { it.score }

        // Step 3: Deduplicate across orbital planes & sample time windows so passes are diverse
        val selected = ArrayList<ScoredCandidate>(maxCount)
        val bucketCounts = HashMap<String, Int>()
        for (cand in candidates) {
            if (selected.size >= maxCount) break
            val key = "${cand.bestSampleIndex}:${cand.planeBucket}"
            val count = bucketCounts[key] ?: 0
            if (count < 2) {
                selected.add(cand)
                bucketCounts[key] = count + 1
            }
        }
        if (selected.size < maxCount) {
            val selectedIds = selected.map { it.orbital.noradId }.toHashSet()
            for (cand in candidates) {
                if (selected.size >= maxCount) break
                if (selectedIds.add(cand.orbital.noradId)) {
                    selected.add(cand)
                }
            }
        }

        val items = selected.map { buildIndividualStarlinkItem(it.orbital) }
        SatelliteCatalog.registerDynamicStarlinks(items)
        return items
    }

    /**
     * Computes the real-time overhead Starlink shortlist (`elevation > 0°`, prioritizing sunlit
     * satellites first and capping at [maxOverhead]) alongside active [StarlinkTrain]s.
     */
    fun selectRealtimeOverheadStarlinks(
        starlinkTles: List<TLEData>,
        userLatDeg: Double,
        userLonDeg: Double,
        userAltMeters: Double = 0.0,
        nowMs: Long = System.currentTimeMillis(),
        maxOverhead: Int = DEFAULT_REALTIME_OVERHEAD_LIMIT
    ): StarlinkRealtimeSnapshot {
        val activeTrains = detectActiveTrains(starlinkTles, nowMs)
        if (starlinkTles.isEmpty() || maxOverhead <= 0) {
            return StarlinkRealtimeSnapshot(nowMs, userLatDeg, userLonDeg, activeTrains, emptyList())
        }

        val orbitals = getParsedOrbitals(starlinkTles)
        val excludedNorads = HashSet<Int>()
        for (train in activeTrains) {
            for (m in train.memberItems) excludedNorads.add(m.noradId)
        }

        val observer = buildObserverFrame(userLatDeg, userLonDeg, userAltMeters)
        val sf = buildTimestampFrame(nowMs)
        val sgp4 = SGP4Propagator()
        val absUserLat = abs(userLatDeg)

        data class OverheadHit(
            val orbital: ParsedStarlinkOrbital,
            val elevDeg: Double,
            val sunlit: Boolean
        )

        val hits = ArrayList<OverheadHit>(96)

        for (orb in orbitals) {
            if (orb.noradId in excludedNorads) continue
            if (orb.meanMotion !in 14.5..MAX_TRAIN_MEAN_MOTION) continue
            // Above-horizon geometric latitude bound (~24° max ground radius at 600 km)
            if (orb.inclinationDeg + 24.0 < absUserLat && (180.0 - orb.inclinationDeg) + 24.0 < absUserLat) {
                continue
            }

            val tsinceMin = (sf.jd - orb.epochJd) * SGP4Propagator.MINUTES_PER_DAY
            if (abs(tsinceMin) > 30.0 * 1440.0) continue

            val teme = sgp4.propagateMinutesSinceEpoch(orb.sgp4Tle, tsinceMin)
            val xEcef = teme.xKm * sf.cosGmst + teme.yKm * sf.sinGmst
            val yEcef = -teme.xKm * sf.sinGmst + teme.yKm * sf.cosGmst
            val zEcef = teme.zKm

            val dx = xEcef - observer.xKm
            val dy = yEcef - observer.yKm
            val dz = zEcef - observer.zKm
            val up = dx * observer.upX + dy * observer.upY + dz * observer.upZ
            if (up <= 0.0) continue

            val rangeSq = dx * dx + dy * dy + dz * dz
            if (rangeSq <= 1.0 || !rangeSq.isFinite()) continue

            val elevDeg = Math.toDegrees(asin((up / sqrt(rangeSq)).coerceIn(-1.0, 1.0)))
            if (elevDeg <= 0.0) continue

            val sunlit = isSunlitFast(xEcef, yEcef, zEcef, sf.sunUnitX, sf.sunUnitY, sf.sunUnitZ)
            hits.add(OverheadHit(orb, elevDeg, sunlit))
        }

        // Prioritize sunlit Starlinks first, then highest elevation above horizon
        hits.sortWith(
            compareByDescending<OverheadHit> { it.sunlit }
                .thenByDescending { it.elevDeg }
        )

        val overheadItems = hits.take(maxOverhead).map { buildIndividualStarlinkItem(it.orbital) }
        SatelliteCatalog.registerDynamicStarlinks(
            items = overheadItems,
            primaryTrain = activeTrains.firstOrNull()?.leaderItem
        )

        return StarlinkRealtimeSnapshot(
            computedAtMs = nowMs,
            observerLatDeg = userLatDeg,
            observerLonDeg = userLonDeg,
            activeTrains = activeTrains,
            overheadStarlinks = overheadItems
        )
    }

    /**
     * Thread-safe 12-second TTL wrapper around [selectRealtimeOverheadStarlinks] so 1 Hz AR Sky
     * ticks and Live Map ticks reuse the overhead shortlist with zero redundant catalog scans.
     */
    fun getCachedOrRefreshRealtimeSnapshot(
        starlinkTles: List<TLEData>,
        userLatDeg: Double,
        userLonDeg: Double,
        userAltMeters: Double = 0.0,
        nowMs: Long = System.currentTimeMillis(),
        maxOverhead: Int = DEFAULT_REALTIME_OVERHEAD_LIMIT
    ): StarlinkRealtimeSnapshot {
        val current = cachedRealtimeSnapshot
        if (current != null &&
            starlinkTles.isNotEmpty() &&
            abs(nowMs - current.computedAtMs) < REALTIME_SNAPSHOT_TTL_MS &&
            abs(userLatDeg - current.observerLatDeg) < 0.25 &&
            abs(userLonDeg - current.observerLonDeg) < 0.25
        ) {
            return current
        }

        synchronized(realtimeLock) {
            val recheck = cachedRealtimeSnapshot
            if (recheck != null &&
                starlinkTles.isNotEmpty() &&
                abs(nowMs - recheck.computedAtMs) < REALTIME_SNAPSHOT_TTL_MS &&
                abs(userLatDeg - recheck.observerLatDeg) < 0.25 &&
                abs(userLonDeg - recheck.observerLonDeg) < 0.25
            ) {
                return recheck
            }
            val fresh = selectRealtimeOverheadStarlinks(
                starlinkTles = starlinkTles,
                userLatDeg = userLatDeg,
                userLonDeg = userLonDeg,
                userAltMeters = userAltMeters,
                nowMs = nowMs,
                maxOverhead = maxOverhead
            )
            cachedRealtimeSnapshot = fresh
            return fresh
        }
    }

    fun invalidateCaches() {
        cachedParsedKey = null
        cachedParsedList = emptyList()
        cachedRealtimeSnapshot = null
    }

    fun cleanStarlinkNameEn(rawName: String, noradId: Int): String {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty() || trimmed.equals("UNKNOWN", ignoreCase = true)) {
            return "Starlink ($noradId)"
        }
        val upper = trimmed.uppercase(Locale.US)
        return when {
            upper.startsWith("STARLINK-") -> {
                val suffix = trimmed.substring("STARLINK-".length).trim()
                "Starlink-$suffix"
            }
            upper.startsWith("STARLINK ") -> {
                val suffix = trimmed.substring("STARLINK ".length).trim()
                "Starlink $suffix"
            }
            else -> trimmed
        }
    }

    fun cleanStarlinkNameFa(rawName: String, noradId: Int): String {
        val trimmed = rawName.trim()
        val upper = trimmed.uppercase(Locale.US)
        return when {
            upper.startsWith("STARLINK-") -> {
                val suffix = trimmed.substring("STARLINK-".length).trim().toPersianDigits()
                "استارلینک $suffix"
            }
            upper.startsWith("STARLINK ") -> {
                val suffix = trimmed.substring("STARLINK ".length).trim().toPersianDigits()
                "استارلینک $suffix"
            }
            else -> "استارلینک ${noradId.toString().toPersianDigits()}"
        }
    }

    internal fun buildIndividualStarlinkItem(orb: ParsedStarlinkOrbital): SatelliteItem {
        val nameEn = cleanStarlinkNameEn(orb.tle.name, orb.noradId)
        val nameFa = cleanStarlinkNameFa(orb.tle.name, orb.noradId)
        val altKm = estimateMeanAltitudeKm(orb.meanMotion).roundToInt()
        val isLowerV2Shell = orb.meanMotion >= 15.55
        val stdMag = if (isLowerV2Shell) 2.8 else 3.4
        val cosparDisplay = if (orb.cosparFull.isNotEmpty()) orb.cosparFull else formatCosparLaunchDisplay(orb.cosparLaunch5)
        return SatelliteItem(
            id = "starlink_${orb.noradId}",
            noradId = orb.noradId,
            nameEn = nameEn,
            nameFa = nameFa,
            category = SatelliteCategory.STARLINK,
            designation = cosparDisplay,
            defaultTle = orb.tle,
            isConstellation = true,
            isTrain = false,
            trainCount = 1,
            starlinkTrainCount = 1,
            standardMagnitude = stdMag,
            isNakedEyeCandidate = true,
            descriptionEn = "$nameEn orbiting in SpaceX's low-Earth-orbit constellation at ~${altKm} km (${String.format(Locale.US, "%.1f°", orb.inclinationDeg)} inclination).",
            descriptionFa = "$nameFa در منظومه مدار پایین زمین اسپیس‌ایکس در ارتفاع حدود ${altKm.toString().toPersianDigits()} کیلومتری.",
            launchDate = formatCosparLaunchDisplay(orb.cosparLaunch5),
            operatorEn = "SpaceX",
            operatorFa = "اسپیس‌ایکس",
            missionPurposeEn = "Global low-latency broadband and direct-to-cell LEO satellite.",
            missionPurposeFa = "ماهواره اینترنت پهن‌باند کم‌تاخیر در مدار نزدیک زمین.",
            scientificSignificanceEn = "Uses autonomous Hall-effect ion propulsion and optical inter-satellite laser mesh links.",
            scientificSignificanceFa = "مجهز به پیشران یونی اثر هال و ارتباط لیزری مستقیم بین‌ماهواره‌ای."
        )
    }

    private data class ObserverFrame(
        val xKm: Double,
        val yKm: Double,
        val zKm: Double,
        val upX: Double,
        val upY: Double,
        val upZ: Double
    )

    private data class TimestampFrame(
        val timeMs: Long,
        val jd: Double,
        val cosGmst: Double,
        val sinGmst: Double,
        val sunUnitX: Double,
        val sunUnitY: Double,
        val sunUnitZ: Double
    )

    private fun buildObserverFrame(latDeg: Double, lonDeg: Double, altMeters: Double): ObserverFrame {
        val latRad = Math.toRadians(latDeg)
        val lonRad = Math.toRadians(lonDeg)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)
        val altKm = altMeters / 1000.0
        val n = WGS84_A_KM / sqrt(1.0 - WGS84_E2 * sinLat * sinLat)
        val x = (n + altKm) * cosLat * cosLon
        val y = (n + altKm) * cosLat * sinLon
        val z = (n * (1.0 - WGS84_E2) + altKm) * sinLat
        return ObserverFrame(
            xKm = x,
            yKm = y,
            zKm = z,
            upX = cosLat * cosLon,
            upY = cosLat * sinLon,
            upZ = sinLat
        )
    }

    private fun buildTimestampFrame(timeMs: Long): TimestampFrame {
        val jd = (timeMs / 86_400_000.0) + 2440587.5
        val gmstRad = Math.toRadians(TimeEngine.getGMST(jd))
        val cosG = cos(gmstRad)
        val sinG = sin(gmstRad)
        val sunEq = SunEngine.calculatePosition(jd)
        val raRad = Math.toRadians(sunEq.raDeg)
        val decRad = Math.toRadians(sunEq.decDeg)
        val sxEci = cos(decRad) * cos(raRad)
        val syEci = cos(decRad) * sin(raRad)
        val szEci = sin(decRad)
        val sxEcef = sxEci * cosG + syEci * sinG
        val syEcef = -sxEci * sinG + syEci * cosG
        val szEcef = szEci
        val norm = sqrt(sxEcef * sxEcef + syEcef * syEcef + szEcef * szEcef).coerceAtLeast(1e-9)
        return TimestampFrame(
            timeMs = timeMs,
            jd = jd,
            cosGmst = cosG,
            sinGmst = sinG,
            sunUnitX = sxEcef / norm,
            sunUnitY = syEcef / norm,
            sunUnitZ = szEcef / norm
        )
    }

    private fun isSunlitFast(
        xKm: Double,
        yKm: Double,
        zKm: Double,
        sunUnitX: Double,
        sunUnitY: Double,
        sunUnitZ: Double
    ): Boolean {
        val proj = xKm * sunUnitX + yKm * sunUnitY + zKm * sunUnitZ
        if (proj >= 0.0) return true
        val perpSq = (xKm * xKm + yKm * yKm + zKm * zKm) - (proj * proj)
        return perpSq > EARTH_SHADOW_RADIUS_SQ
    }

    /**
     * Picks up to 8 representative sample timestamps across upcoming dusk and dawn twilight
     * windows (`sunAlt in [-26°, -6°]`, falling back to any dark sky `sunAlt <= -6°` within 24h).
     */
    private fun selectTwilightSampleTimestamps(
        userLatDeg: Double,
        userLonDeg: Double,
        startTimeMs: Long
    ): List<Long> {
        val stepMs = 10 * 60_000L // 10-minute coarse solar scan over 24 hours (144 samples)
        val twilightSlots = ArrayList<Long>(36)
        val darkFallbackSlots = ArrayList<Long>(72)

        for (step in 0..144) {
            val t = startTimeMs + step * stepMs
            val sunAlt = ISSEngine.getObserverSunAltitude(t, userLatDeg, userLonDeg)
            if (sunAlt <= -6.0) {
                darkFallbackSlots.add(t)
                if (sunAlt >= -26.0) {
                    twilightSlots.add(t)
                }
            }
        }

        val pool = if (twilightSlots.isNotEmpty()) twilightSlots else darkFallbackSlots
        if (pool.isEmpty()) return emptyList()
        if (pool.size <= 8) return pool

        // Split contiguous windows (e.g. evening dusk window vs morning dawn window) and sample 4 from each
        val windows = ArrayList<MutableList<Long>>()
        for (t in pool) {
            val currentWin = windows.lastOrNull()
            if (currentWin == null || (t - currentWin.last()) > 90 * 60_000L) {
                windows.add(mutableListOf(t))
            } else {
                currentWin.add(t)
            }
        }

        val sampled = ArrayList<Long>(8)
        val perWindow = (8 / windows.size.coerceAtLeast(1)).coerceIn(2, 4)
        for (win in windows.take(3)) {
            if (win.size <= perWindow) {
                sampled.addAll(win)
            } else {
                for (k in 0 until perWindow) {
                    val idx = ((k.toDouble() / (perWindow - 1).coerceAtLeast(1)) * (win.size - 1)).roundToInt()
                    sampled.add(win[idx])
                }
            }
        }
        return sampled.distinct().take(8)
    }
}
