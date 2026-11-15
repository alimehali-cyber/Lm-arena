package com.alijafari.red.astronomy.data.repository

import android.content.Context
import android.util.Log
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.astro_engine.SatelliteEngine
import com.alijafari.red.astronomy.astro_engine.StarlinkTrainManager
import com.alijafari.red.astronomy.astro_engine.TleDataSource
import com.alijafari.red.astronomy.astro_engine.TleMetadata
import com.alijafari.red.astronomy.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.math.abs

/**
 * Proxy-ready CelesTrak feed URL constants.
 * Swap the base domain in one place to route through a custom Cloudflare Worker / backend proxy.
 */
const val STARLINK_SUP_GP_URL = "https://celestrak.org/NORAD/elements/supplemental/sup-gp.php?FILE=starlink&FORMAT=tle"
const val STARLINK_LAST_30_DAYS_URL = "https://celestrak.org/NORAD/elements/gp.php?GROUP=last-30-days&FORMAT=tle"
const val STARLINK_GP_URL = "https://celestrak.org/NORAD/elements/gp.php?GROUP=starlink&FORMAT=tle"

private const val STATIONS_GP_URL = "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle"
private const val VISUAL_GP_URL = "https://celestrak.org/NORAD/elements/gp.php?GROUP=visual&FORMAT=tle"
private const val ISS_DIRECT_CATNR_URL = "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=tle"

/**
 * Repository responsible for fetching, validating, deduplicating, persisting, and caching
 * Two-Line Element (TLE) satellite orbital data.
 *
 * - Static tracked satellites (`ISS`, `Tiangong`, `Hubble`, `Envisat`) persist in Room `cached_tle`.
 * - Multi-feed Starlink TLEs (`SUP_GP` + filtered `LAST_30_DAYS` + `STARLINK_GP`) merge by newest
 *   epoch into a persistent disk cache (`starlink_merged_cache.tle`) protected by a strict 2-hour
 *   network cooldown to prevent CelesTrak HTTP 403 rate-limit bans.
 */
class TleRepository internal constructor(
    private val context: Context,
    okHttpClientOverride: OkHttpClient? = null,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val supGpUrl: String = STARLINK_SUP_GP_URL,
    private val last30DaysUrl: String = STARLINK_LAST_30_DAYS_URL,
    private val starlinkGpUrl: String = STARLINK_GP_URL,
    autoLoadDiskAsync: Boolean = true
) {

    constructor(context: Context) : this(
        context = context,
        okHttpClientOverride = null
    )

    private val appContext = context.applicationContext ?: context
    private val db = AppDatabase.getDatabase(appContext)
    private val prefs by lazy {
        appContext.getSharedPreferences(STARLINK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val memoryCache = ConcurrentHashMap<Int, TLEData>()
    private val starlinkTles = ConcurrentHashMap<Int, TLEData>()
    private val timestampsCache = ConcurrentHashMap<Int, Long>()

    private val starlinkDiskLock = Any()

    @Volatile
    private var starlinkDiskCacheLoaded = false

    private val _starlinkVersion = MutableStateFlow(0L)
    val starlinkVersion: StateFlow<Long> = _starlinkVersion.asStateFlow()

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient: OkHttpClient = okHttpClientOverride ?: OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val bulkStarlinkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        loadFromDatabase()
        if (autoLoadDiskAsync) {
            repoScope.launch {
                ensureStarlinkDiskCacheLoaded()
            }
        }
    }

    fun getStarlinkCacheFile(): File = File(appContext.filesDir, STARLINK_CACHE_FILE_NAME)

    fun getLastStarlinkSyncMs(): Long = prefs.getLong(KEY_LAST_STARLINK_SYNC_MS, 0L)

    /**
     * If `starlink_cache_schema_version` in [prefs] does not match [STARLINK_CACHE_SCHEMA_VERSION],
     * immediately deletes `starlink_merged_cache.tle`, clears in-memory Starlink TLEs, and resets
     * `last_starlink_sync_ms = 0L` so an upgraded APK forces an immediate fresh network download.
     */
    private fun invalidateCacheIfSchemaMismatchLocked(): Boolean {
        val storedVersion = prefs.getInt(KEY_STARLINK_CACHE_SCHEMA_VERSION, 0)
        if (storedVersion != STARLINK_CACHE_SCHEMA_VERSION) {
            val cacheFile = getStarlinkCacheFile()
            if (cacheFile.exists()) {
                runCatching { cacheFile.delete() }
            }
            starlinkTles.clear()
            starlinkDiskCacheLoaded = false
            StarlinkTrainManager.invalidateCaches()
            SatelliteCatalog.clearPrimaryTrain()
            prefs.edit()
                .putLong(KEY_LAST_STARLINK_SYNC_MS, 0L)
                .apply()
            Log.i(
                TAG,
                "Invalidated stale Starlink disk cache on schema upgrade ($storedVersion -> $STARLINK_CACHE_SCHEMA_VERSION)"
            )
            return true
        }
        return false
    }

    /**
     * Loads `starlink_merged_cache.tle` from disk into [starlinkTles] on cold start or offline
     * launch before any network call runs. Thread-safe and idempotent.
     */
    fun ensureStarlinkDiskCacheLoaded(): Boolean {
        synchronized(starlinkDiskLock) {
            if (invalidateCacheIfSchemaMismatchLocked()) {
                starlinkDiskCacheLoaded = true
                return false
            }
            if (starlinkDiskCacheLoaded && starlinkTles.isNotEmpty()) return true
            val cacheFile = getStarlinkCacheFile()
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                starlinkDiskCacheLoaded = true
                return false
            }
            return try {
                val raw = cacheFile.readText(Charsets.UTF_8)
                val parsed = parseTleFeed(raw, allowZeroPlaceholderNorad = true)
                if (parsed.isNotEmpty()) {
                    starlinkTles.clear()
                    starlinkTles.putAll(parsed)
                    StarlinkTrainManager.invalidateCaches()
                    // Pre-register any active physical trains from disk cache immediately
                    StarlinkTrainManager.detectActiveTrains(parsed.values.toList(), clockMs())
                    starlinkDiskCacheLoaded = true
                    _starlinkVersion.value = _starlinkVersion.value + 1L
                    Log.i(TAG, "Loaded ${parsed.size} Starlink TLEs from persistent disk cache (${cacheFile.name})")
                    true
                } else {
                    starlinkDiskCacheLoaded = true
                    false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read Starlink disk cache: ${e.message}")
                false
            }
        }
    }

    /**
     * Enforces the strict 2-hour CelesTrak rate-limit cooldown when a valid current-schema disk cache exists.
     */
    fun shouldFetchStarlinkFromNetwork(nowMs: Long = clockMs()): Boolean {
        if (prefs.getInt(KEY_STARLINK_CACHE_SCHEMA_VERSION, 0) != STARLINK_CACHE_SCHEMA_VERSION) return true
        val cacheFile = getStarlinkCacheFile()
        if (!cacheFile.exists() || cacheFile.length() == 0L) return true
        val lastSync = getLastStarlinkSyncMs()
        if (lastSync <= 0L) return true
        return (nowMs - lastSync) >= STARLINK_SYNC_COOLDOWN_MS
    }

    private fun loadFromDatabase() {
        try {
            val cursor = db.openHelper.readableDatabase.query(
                "SELECT noradId, line1, line2, updatedAt FROM cached_tle"
            )
            cursor.use { c ->
                val idIdx = c.getColumnIndex("noradId")
                val l1Idx = c.getColumnIndex("line1")
                val l2Idx = c.getColumnIndex("line2")
                val timeIdx = c.getColumnIndex("updatedAt")
                while (c.moveToNext()) {
                    val noradId = c.getInt(idIdx)
                    val line1 = c.getString(l1Idx)
                    val line2 = c.getString(l2Idx)
                    val updatedAt = if (timeIdx >= 0) c.getLong(timeIdx) else clockMs()
                    val satName = SatelliteCatalog.satellites.find { it.noradId == noradId }?.nameEn ?: "SAT_$noradId"
                    val tle = TLEData(name = satName, line1 = line1, line2 = line2)
                    memoryCache[noradId] = tle
                    timestampsCache[noradId] = updatedAt
                    if (noradId == 25544) {
                        ISSEngine.cachedTLE = tle
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading cached TLEs from database: ${e.message}")
        }
    }

    /**
     * Returns the latest merged Starlink TLEs (from live network sync or persistent disk cache).
     */
    fun getStarlinkTles(): List<TLEData> {
        if (!starlinkDiskCacheLoaded && starlinkTles.isEmpty()) {
            // Safe fast fallback if called on a background worker before async init finishes
            if (Thread.currentThread().name != "main") {
                ensureStarlinkDiskCacheLoaded()
            }
        }
        return starlinkTles.values.toList()
    }

    /**
     * Gets the most up-to-date TLE along with source provenance and freshness metadata.
     * Looks up: Memory Cache -> Room Database -> Starlink Cache -> Hardcoded Offline Fallback in SatelliteCatalog.
     */
    fun getTleWithMetadata(noradId: Int): TleMetadata {
        val now = clockMs()

        // 1. Check in-memory cache
        val memTle = memoryCache[noradId]
        if (memTle != null) {
            val ts = timestampsCache[noradId] ?: 0L
            val ageMs = if (ts > 0) now - ts else Long.MAX_VALUE
            val epochAge = SatelliteEngine.computeTleAgeDays(memTle.line1)
            val source = if (ageMs < 24 * 3600 * 1000L && ts > 0) {
                TleDataSource.LIVE_NETWORK
            } else if (ts > 0) {
                TleDataSource.PERSISTED_CACHE
            } else {
                TleDataSource.MEMORY_CACHE
            }
            val stale = epochAge > 14.0 || isStale(noradId)
            return TleMetadata(noradId, source, epochAge, ts, stale, memTle)
        }

        // 2. Check Room database
        try {
            val cursor = db.openHelper.readableDatabase.query(
                "SELECT line1, line2, updatedAt FROM cached_tle WHERE noradId = ?",
                arrayOf(noradId.toString())
            )
            cursor.use { c ->
                if (c.moveToFirst()) {
                    val line1 = c.getString(0)
                    val line2 = c.getString(1)
                    val updatedAt = c.getLong(2)
                    val satName = SatelliteCatalog.satellites.find { it.noradId == noradId }?.nameEn ?: "SAT_$noradId"
                    val tle = TLEData(name = satName, line1 = line1, line2 = line2)
                    memoryCache[noradId] = tle
                    timestampsCache[noradId] = updatedAt
                    if (noradId == 25544) {
                        ISSEngine.cachedTLE = tle
                    }
                    val epochAge = SatelliteEngine.computeTleAgeDays(line1)
                    val ageMs = now - updatedAt
                    val source = if (ageMs < 24 * 3600 * 1000L && updatedAt > 0) {
                        TleDataSource.LIVE_NETWORK
                    } else {
                        TleDataSource.PERSISTED_CACHE
                    }
                    val stale = epochAge > 14.0 || (ageMs > 72 * 3600 * 1000L)
                    return TleMetadata(noradId, source, epochAge, updatedAt, stale, tle)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading TLE for $noradId from database: ${e.message}")
        }

        // 3. Check Starlink cache (live network or persistent disk cache)
        val starlinkTle = starlinkTles[noradId]
        if (starlinkTle != null) {
            val epochAge = SatelliteEngine.computeTleAgeDays(starlinkTle.line1)
            val syncMs = getLastStarlinkSyncMs().let { if (it > 0L) it else now }
            val source = if ((now - syncMs) < 24 * 3600 * 1000L) {
                TleDataSource.LIVE_NETWORK
            } else {
                TleDataSource.PERSISTED_CACHE
            }
            val stale = epochAge > 14.0
            return TleMetadata(noradId, source, epochAge, syncMs, stale, starlinkTle)
        }

        // 4. Fallback to hardcoded catalog default (Final offline fallback)
        val fallback = SatelliteCatalog.satellites.find { it.noradId == noradId }?.defaultTle
            ?: ISSEngine.TLEData()
        val epochAge = SatelliteEngine.computeTleAgeDays(fallback.line1)
        return TleMetadata(noradId, TleDataSource.HARDCODED_FALLBACK, epochAge, 0L, true, fallback)
    }

    /**
     * Gets the most up-to-date TLE for a given NORAD satellite ID.
     */
    fun getTle(noradId: Int): TLEData {
        return getTleWithMetadata(noradId).tleData
    }

    /**
     * Checks if the TLE for a given satellite NORAD ID is stale (older than maxAgeHours, default 72h).
     */
    fun isStale(noradId: Int, maxAgeHours: Int = 72): Boolean {
        val lastUpdated = timestampsCache[noradId]
        if (lastUpdated == null) {
            try {
                val cursor = db.openHelper.readableDatabase.query(
                    "SELECT updatedAt FROM cached_tle WHERE noradId = ?",
                    arrayOf(noradId.toString())
                )
                cursor.use { c ->
                    if (c.moveToFirst()) {
                        val ts = c.getLong(0)
                        timestampsCache[noradId] = ts
                        val ageMs = clockMs() - ts
                        return ageMs > (maxAgeHours * 3600 * 1000L)
                    }
                }
            } catch (_: Exception) {
                // Ignore
            }
            return true
        }
        val ageMs = clockMs() - lastUpdated
        return ageMs > (maxAgeHours * 3600 * 1000L)
    }

    /**
     * Saves a validated TLE into the Room database and in-memory cache.
     */
    fun saveTle(noradId: Int, name: String, line1: String, line2: String, timestamp: Long = clockMs()) {
        val tle = TLEData(name = name, line1 = line1, line2 = line2)
        memoryCache[noradId] = tle
        timestampsCache[noradId] = timestamp

        try {
            db.openHelper.writableDatabase.execSQL(
                "INSERT OR REPLACE INTO cached_tle (noradId, line1, line2, updatedAt) VALUES (?, ?, ?, ?)",
                arrayOf(noradId, line1, line2, timestamp)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save TLE for NORAD $noradId: ${e.message}")
        }

        if (noradId == 25544) {
            ISSEngine.cachedTLE = tle
        }
    }

    /**
     * Refreshes static tracked TLEs (`stations`, `visual`) and — when outside the 2-hour cooldown
     * or missing a disk cache — fetches, filters, deduplicates (keeping newest epoch), and persists
     * the 3 Starlink feeds (`STARLINK_SUP_GP_URL`, `STARLINK_LAST_30_DAYS_URL`, `STARLINK_GP_URL`).
     */
    suspend fun refreshTles(refreshStaticCatalog: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val trackedNoradIds = SatelliteCatalog.satellites.map { it.noradId }.toSet()
        var anyUpdated = false

        // Always check schema version first and warm Starlink memory cache from persistent disk if current
        synchronized(starlinkDiskLock) {
            invalidateCacheIfSchemaMismatchLocked()
        }
        val hadEmptyStarlinkMemory = starlinkTles.isEmpty()
        val loadedFromDisk = ensureStarlinkDiskCacheLoaded()
        if (hadEmptyStarlinkMemory && loadedFromDisk) {
            anyUpdated = true
        }

        // 1. Static catalog feeds (ISS, Tiangong, Hubble, Envisat) — gated by 2-hour freshness so we never spam CelesTrak
        val anyStaticNeedsRefresh = refreshStaticCatalog && trackedNoradIds.any { isStale(it, maxAgeHours = 2) }
        if (anyStaticNeedsRefresh) {
            for (endpoint in listOf(STATIONS_GP_URL, VISUAL_GP_URL)) {
                try {
                    val bodyString = fetchRawBody(okHttpClient, endpoint, requestGzip = false)
                    if (bodyString.isNullOrBlank()) continue
                    val parsed = parseTleFeed(bodyString)
                    if (parsed.isEmpty()) continue
                    val now = clockMs()
                    var countStored = 0
                    for ((noradId, tle) in parsed) {
                        if (noradId in trackedNoradIds || noradId == 25544) {
                            saveTle(noradId, tle.name, tle.line1, tle.line2, now)
                            countStored++
                            anyUpdated = true
                        }
                    }
                    Log.i(TAG, "Fetched ${parsed.size} TLEs from $endpoint, saved $countStored tracked static TLEs")
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while fetching static TLEs from $endpoint: ${e.message}")
                }
            }

            if (isStale(25544, 24)) {
                try {
                    val bodyString = fetchRawBody(okHttpClient, ISS_DIRECT_CATNR_URL, requestGzip = false)
                    if (!bodyString.isNullOrBlank()) {
                        val issTle = parseTleFeed(bodyString)[25544]
                        if (issTle != null) {
                            saveTle(25544, issTle.name, issTle.line1, issTle.line2, clockMs())
                            anyUpdated = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Direct ISS TLE fetch failed: ${e.message}")
                }
            }
        }

        // 2. Multi-feed Starlink sync with strict 2-hour cooldown & atomic disk persistence
        val nowMs = clockMs()
        if (!shouldFetchStarlinkFromNetwork(nowMs)) {
            Log.i(TAG, "Skipping Starlink CelesTrak network fetch (within 2-hour cooldown; ${starlinkTles.size} cached TLEs active)")
            return@withContext anyUpdated
        }

        val starlinkSynced = syncStarlinkFeedsInternal()
        if (starlinkSynced) {
            anyUpdated = true
        }

        anyUpdated
    }

    private fun syncStarlinkFeedsInternal(): Boolean {
        // Seed merged map with existing cached TLEs so partial feed failures never lose existing data
        val merged = LinkedHashMap<Int, TLEData>(starlinkTles.size.coerceAtLeast(1024))
        for ((id, tle) in starlinkTles) {
            merged[id] = tle
        }

        var anyStarlinkFeedSucceeded = false

        // Feed A: Supplemental GP (SpaceX launch-day / fresh deployment TLEs, including 70000..99999 & Alpha-5 IDs)
        var freshSupIds: Set<Int> = emptySet()
        try {
            val supBody = fetchRawBody(okHttpClient, supGpUrl, requestGzip = true)
            if (!supBody.isNullOrBlank()) {
                val supParsed = parseTleFeed(supBody, allowZeroPlaceholderNorad = true)
                val validSupValues = supParsed.values.filterNot { isDebrisOrRocketBody(it.name) }
                if (validSupValues.isNotEmpty()) {
                    freshSupIds = supParsed.keys.toSet()
                    // Prune stale (> 7d) temporary 70000..99999 supplemental IDs from prior cache before merging fresh sup-gp
                    val nowJd = (clockMs() / 86_400_000.0) + 2440587.5
                    val staleTempIds = merged.entries
                        .filter { (id, tle) ->
                            id in 70_000..99_999 &&
                                id !in freshSupIds &&
                                (nowJd - StarlinkTrainManager.epochToJulian(
                                    if (tle.epochYear != 0) tle.epochYear else StarlinkTrainManager.extractEpochYear(tle.line1),
                                    if (tle.epochDay != 0.0) tle.epochDay else StarlinkTrainManager.extractEpochDay(tle.line1)
                                )) > 7.0
                        }
                        .map { it.key }
                    for (staleId in staleTempIds) {
                        merged.remove(staleId)
                    }

                    mergePreferNewestEpoch(merged, validSupValues, allowZeroPlaceholderNorad = true)
                    anyStarlinkFeedSucceeded = true
                    Log.i(TAG, "Merged ${validSupValues.size} Supplemental GP Starlink TLEs")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "STARLINK_SUP_GP fetch failed (continuing with remaining feeds): ${e.message}")
        }

        // Feed B: Last 30 Days launches (catches newly cataloged Starlinks, temporary 70000..99999 / Alpha-5 IDs,
        // and multi-satellite OBJECT/TBA launch clusters in Starlink inclination shells)
        try {
            val last30Body = fetchRawBody(okHttpClient, last30DaysUrl, requestGzip = true)
            if (!last30Body.isNullOrBlank()) {
                val last30Parsed = parseTleFeed(last30Body, allowZeroPlaceholderNorad = true)
                val filteredLast30 = filterLast30DaysStarlinkClusterEntries(last30Parsed.values)
                if (filteredLast30.isNotEmpty()) {
                    mergePreferNewestEpoch(merged, filteredLast30, allowZeroPlaceholderNorad = true)
                    anyStarlinkFeedSucceeded = true
                    Log.i(TAG, "Merged ${filteredLast30.size} recent Starlink/OBJECT TLEs from last-30-days")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "STARLINK_LAST_30_DAYS fetch failed (continuing with remaining feeds): ${e.message}")
        }

        // Feed C: Full Starlink GP catalog (30s read timeout + gzip; non-fatal if rate-limited/timed out)
        try {
            val fullGpBody = fetchRawBody(bulkStarlinkHttpClient, starlinkGpUrl, requestGzip = true)
            if (!fullGpBody.isNullOrBlank()) {
                val gpParsed = parseTleFeed(fullGpBody)
                if (gpParsed.isNotEmpty()) {
                    mergePreferNewestEpoch(merged, gpParsed.values)
                    anyStarlinkFeedSucceeded = true
                    Log.i(TAG, "Merged ${gpParsed.size} full Starlink GP TLEs")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "STARLINK_GP full catalog fetch failed (preserving SUP_GP + LAST_30_DAYS + cached TLEs): ${e.message}")
        }

        if (!anyStarlinkFeedSucceeded || merged.isEmpty()) {
            return false
        }

        synchronized(starlinkDiskLock) {
            writeMergedCacheAtomically(merged.values)
            val syncTime = clockMs()
            prefs.edit()
                .putLong(KEY_LAST_STARLINK_SYNC_MS, syncTime)
                .putInt(KEY_STARLINK_CACHE_SCHEMA_VERSION, STARLINK_CACHE_SCHEMA_VERSION)
                .apply()

            starlinkTles.clear()
            starlinkTles.putAll(merged)
            starlinkDiskCacheLoaded = true
            StarlinkTrainManager.invalidateCaches()
            StarlinkTrainManager.detectActiveTrains(merged.values.toList(), syncTime)
            _starlinkVersion.value = _starlinkVersion.value + 1L
        }

        Log.i(TAG, "Persisted ${merged.size} deduplicated Starlink TLEs to $STARLINK_CACHE_FILE_NAME")
        return true
    }

    private fun fetchRawBody(client: OkHttpClient, url: String, requestGzip: Boolean): String? {
        val reqBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "ZIG-Astronomy/1.0")
            .get()
        if (requestGzip) {
            reqBuilder.header("Accept-Encoding", "gzip")
        }

        client.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code} while fetching TLEs from $url")
                return null
            }
            val bytes = response.body?.bytes() ?: return null
            if (bytes.isEmpty()) return null

            val isGzipped = response.header("Content-Encoding")?.equals("gzip", ignoreCase = true) == true ||
                (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte())

            return if (isGzipped) {
                GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                String(bytes, Charsets.UTF_8)
            }
        }
    }

    private fun writeMergedCacheAtomically(tles: Collection<TLEData>) {
        val targetFile = getStarlinkCacheFile()
        targetFile.parentFile?.mkdirs()
        val tempFile = File(targetFile.parentFile, "${STARLINK_CACHE_FILE_NAME}.tmp")
        try {
            tempFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                for (tle in tles) {
                    val safeName = tle.name.trim().ifEmpty { "STARLINK" }
                    writer.append(safeName).append('\n')
                    writer.append(tle.line1.trim()).append('\n')
                    writer.append(tle.line2.trim()).append('\n')
                }
                writer.flush()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed atomic write of $STARLINK_CACHE_FILE_NAME: ${e.message}")
            runCatching { tempFile.delete() }
        }
    }

    companion object {
        private const val TAG = "TleRepository"
        private const val STARLINK_CACHE_SCHEMA_VERSION = 4

        const val STARLINK_CACHE_FILE_NAME = "starlink_merged_cache.tle"
        const val STARLINK_PREFS_NAME = "zig_starlink_tle_prefs"
        const val KEY_LAST_STARLINK_SYNC_MS = "last_starlink_sync_ms"
        const val KEY_STARLINK_CACHE_SCHEMA_VERSION = "starlink_cache_schema_version"
        const val STARLINK_SYNC_COOLDOWN_MS = 2 * 60 * 60 * 1000L // 2 hours

        private val DEB_TOKEN_REGEX = Regex("(^|[^A-Z0-9])DEB($|[^A-Z0-9])")

        @Volatile
        private var INSTANCE: TleRepository? = null

        fun getInstance(context: Context): TleRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TleRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Non-initializing peek for background pipelines (such as AR Sky scene ticks) that do not
         * hold an Android [Context]. Returns `null` if the repository has not been opened yet.
         */
        fun peekInstance(): TleRepository? = INSTANCE

        /**
         * Returns `true` only if an object name is expressly labeled `DEB` (debris) or `R/B` (rocket body).
         */
        fun isDebrisOrRocketBody(rawName: String): Boolean {
            val upper = rawName.trim().uppercase(Locale.US)
            return upper.contains("R/B") || DEB_TOKEN_REGEX.containsMatchIn(upper)
        }

        /**
         * Broad LEO Starlink / Starship V3 deployment inclination envelope (`24.0°..100.0°`),
         * covering Starbase Boca Chica (`~26°..40°`), classic Falcon 9 shells (`43°`, `53°`, `70°`),
         * and polar/SSO shells (`97.6°`).
         */
        fun isInStarlinkInclinationShell(incDeg: Double): Boolean {
            return incDeg in 24.0..100.0
        }

        private fun isClassicFalconStarlinkBand(incDeg: Double): Boolean {
            return incDeg in 42.0..44.0 ||
                incDeg in 52.5..53.5 ||
                incDeg in 69.5..70.5 ||
                incDeg in 96.5..98.5
        }

        private fun isDeploymentPayloadName(upperName: String): Boolean {
            return upperName.startsWith("STARLINK") ||
                upperName.contains("STARLINK") ||
                upperName.startsWith("OBJECT") ||
                upperName.startsWith("TBA") ||
                upperName.startsWith("UNKNOWN") ||
                upperName.startsWith("STARSHIP")
        }

        private fun resolveNoradOrSupplementalId(
            name: String,
            line1: String,
            line2: String,
            allowZeroPlaceholderNorad: Boolean
        ): Int {
            val parsed = StarlinkTrainManager.extractNoradId(line1)
            if (parsed > 0) return parsed
            if (allowZeroPlaceholderNorad && line1.length >= 7 && line1.substring(2, 7).trim() == "00000") {
                val hash = ((31 * name.trim().hashCode() + line2.trim().hashCode()) and 0x0FFFF)
                return 900_000 + hash
            }
            return 0
        }

        /**
         * Single-entry Starlink / Starship / uncataloged LEO payload candidate check for `GROUP=last-30-days`.
         */
        fun isStarlinkFromLast30Days(tle: TLEData): Boolean {
            val upper = tle.name.trim().uppercase(Locale.US)
            if (isDebrisOrRocketBody(upper)) return false
            if (upper.contains("STARLINK") || upper.startsWith("STARSHIP")) return true
            val noradId = StarlinkTrainManager.extractNoradId(tle.line1)
            val isSupplementalOrUncataloged = noradId in 70_000..99_999 ||
                noradId >= 100_000 ||
                isDeploymentPayloadName(upper)
            if (isSupplementalOrUncataloged) {
                val inc = StarlinkTrainManager.extractInclinationDeg(tle.line2)
                val mm = StarlinkTrainManager.extractMeanMotion(tle.line2)
                return isInStarlinkInclinationShell(inc) && mm in 14.90..16.75
            }
            return false
        }

        /**
         * Filters `GROUP=last-30-days` entries down to:
         * 1. Named `STARLINK` / `STARSHIP` payloads (excluding `DEB` and `R/B`).
         * 2. Any multi-satellite launch cluster with `>= 6` coplanar/co-launched LEO payloads
         *    (`meanMotion in 14.90..16.75`, `inclination in 24.0°..100.0°`) whose names start with
         *    `STARLINK`, `OBJECT`, `TBA`, `UNKNOWN`, or `STARSHIP` — regardless of inclination
         *    (capturing Starbase Starship V3 deployments at ~26°..40° as well as 43°/53°/70°/97.6°).
         * 3. Coplanar/co-launched `OBJECT` / `TBA` entries (`>= 2`) in classic Falcon 9 Starlink bands.
         */
        fun filterLast30DaysStarlinkClusterEntries(candidates: Collection<TLEData>): List<TLEData> {
            val list = candidates.toList()
            if (list.isEmpty()) return emptyList()

            data class ShellCandidate(
                val tle: TLEData,
                val isExplicitStarlink: Boolean,
                val cospar5: String,
                val incDeg: Double,
                val raanDeg: Double
            )

            val shellPool = ArrayList<ShellCandidate>()
            val cosparCounts = HashMap<String, Int>()

            for (tle in list) {
                val upper = tle.name.trim().uppercase(Locale.US)
                if (isDebrisOrRocketBody(upper)) continue
                val isExplicit = upper.contains("STARLINK") || upper.startsWith("STARSHIP")
                if (!isExplicit && !isStarlinkFromLast30Days(tle)) continue
                val c5 = StarlinkTrainManager.extractCosparLaunchGroup(tle.line1)
                val inc = StarlinkTrainManager.extractInclinationDeg(tle.line2)
                val raan = StarlinkTrainManager.extractRaanDeg(tle.line2)
                if (c5.length == 5 && c5.all { it.isDigit() } && !c5.endsWith("000")) {
                    cosparCounts[c5] = (cosparCounts[c5] ?: 0) + 1
                }
                shellPool.add(ShellCandidate(tle, isExplicit, c5, inc, raan))
            }

            val accepted = ArrayList<TLEData>(shellPool.size)
            for (i in shellPool.indices) {
                val item = shellPool[i]
                if (item.isExplicitStarlink) {
                    accepted.add(item.tle)
                    continue
                }
                val sameCosparCount = cosparCounts[item.cospar5] ?: 0
                var coplanarCount = 1
                for (j in shellPool.indices) {
                    if (i == j) continue
                    val other = shellPool[j]
                    if (abs(item.incDeg - other.incDeg) <= 0.6 &&
                        StarlinkTrainManager.circularDistanceDeg(item.raanDeg, other.raanDeg) <= 3.5
                    ) {
                        coplanarCount++
                    }
                }
                val clusterSize = kotlin.math.max(sameCosparCount, coplanarCount)
                val keepBroadMultiSatCluster = clusterSize >= 6
                val keepClassicShellCluster = clusterSize >= 2 && isClassicFalconStarlinkBand(item.incDeg)
                if (keepBroadMultiSatCluster || keepClassicShellCluster) {
                    accepted.add(item.tle)
                }
            }
            return accepted
        }

        /**
         * Deduplicates TLEs by NORAD Catalog ID, strictly keeping whichever TLE has the newer epoch.
         */
        fun mergePreferNewestEpoch(
            target: MutableMap<Int, TLEData>,
            incoming: Iterable<TLEData>,
            allowZeroPlaceholderNorad: Boolean = false
        ) {
            for (cand in incoming) {
                val noradId = resolveNoradOrSupplementalId(cand.name, cand.line1, cand.line2, allowZeroPlaceholderNorad)
                if (noradId <= 0) continue
                val existing = target[noradId]
                if (existing == null) {
                    target[noradId] = cand
                } else {
                    val candYr = if (cand.epochYear != 0) cand.epochYear else StarlinkTrainManager.extractEpochYear(cand.line1)
                    val candDay = if (cand.epochDay != 0.0) cand.epochDay else StarlinkTrainManager.extractEpochDay(cand.line1)
                    val existYr = if (existing.epochYear != 0) existing.epochYear else StarlinkTrainManager.extractEpochYear(existing.line1)
                    val existDay = if (existing.epochDay != 0.0) existing.epochDay else StarlinkTrainManager.extractEpochDay(existing.line1)
                    val candJd = StarlinkTrainManager.epochToJulian(candYr, candDay)
                    val existJd = StarlinkTrainManager.epochToJulian(existYr, existDay)
                    if (candJd >= existJd) {
                        target[noradId] = cand
                    }
                }
            }
        }

        /**
         * Validates standard NORAD mod-10 checksum on a TLE line.
         */
        fun validateChecksum(line: String): Boolean {
            val trimmed = line.trim()
            if (trimmed.length < 68) return false
            val expected = trimmed.last().digitToIntOrNull() ?: return false
            var sum = 0
            for (i in 0 until trimmed.length - 1) {
                val c = trimmed[i]
                when {
                    c.isDigit() -> sum += (c - '0')
                    c == '-' -> sum += 1
                }
            }
            return (sum % 10) == expected
        }

        /**
         * Parses 3-line or 2-line TLE text, supporting both 5-digit and Alpha-5 NORAD IDs and
         * keeping the newest epoch when a NORAD ID appears multiple times.
         */
        fun parseTleFeed(rawText: String, allowZeroPlaceholderNorad: Boolean = false): Map<Int, TLEData> {
            val result = LinkedHashMap<Int, TLEData>()
            val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

            var i = 0
            while (i < lines.size) {
                val line = lines[i]
                if (line.startsWith("1 ") && i + 1 < lines.size && lines[i + 1].startsWith("2 ")) {
                    val line1 = line
                    val line2 = lines[i + 1]
                    val name = if (i > 0 && !lines[i - 1].startsWith("1 ") && !lines[i - 1].startsWith("2 ")) {
                        lines[i - 1]
                    } else {
                        "UNKNOWN"
                    }

                    if (validateChecksum(line1) && validateChecksum(line2)) {
                        val noradId = resolveNoradOrSupplementalId(name, line1, line2, allowZeroPlaceholderNorad)
                        if (noradId > 0) {
                            val epochYr = StarlinkTrainManager.extractEpochYear(line1)
                            val epochDay = StarlinkTrainManager.extractEpochDay(line1)
                            val candidate = TLEData(
                                name = name,
                                line1 = line1,
                                line2 = line2,
                                epochYear = epochYr,
                                epochDay = epochDay
                            )
                            val existing = result[noradId]
                            if (existing == null) {
                                result[noradId] = candidate
                            } else {
                                val candJd = StarlinkTrainManager.epochToJulian(epochYr, epochDay)
                                val existJd = StarlinkTrainManager.epochToJulian(existing.epochYear, existing.epochDay)
                                if (candJd >= existJd) {
                                    result[noradId] = candidate
                                }
                            }
                        }
                    }
                    i += 2
                } else {
                    i++
                }
            }
            return result
        }
    }
}
