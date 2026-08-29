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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Repository responsible for fetching, validating, persisting, and caching
 * Two-Line Element (TLE) satellite orbital data using raw OkHttpClient plain text requests.
 *
 * Persists live TLEs into Room `cached_tle` table and keeps an in-memory cache
 * for zero-latency frame propagation during rendering.
 */
class TleRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context.applicationContext)
    private val memoryCache = ConcurrentHashMap<Int, TLEData>()
    private val starlinkTles = ConcurrentHashMap<Int, TLEData>()
    private val timestampsCache = ConcurrentHashMap<Int, Long>()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        loadFromDatabase()
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
                    val updatedAt = if (timeIdx >= 0) c.getLong(timeIdx) else System.currentTimeMillis()
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
     * Returns the latest parsed Starlink TLEs fetched from CelesTrak.
     */
    fun getStarlinkTles(): List<TLEData> {
        return starlinkTles.values.toList()
    }

    /**
     * Gets the most up-to-date TLE along with source provenance and freshness metadata.
     * Looks up: Memory Cache -> Room Database -> Starlink Cache -> Hardcoded Offline Fallback in SatelliteCatalog.
     */
    fun getTleWithMetadata(noradId: Int): TleMetadata {
        val now = System.currentTimeMillis()

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

        // 3. Check Starlink cache
        val starlinkTle = starlinkTles[noradId]
        if (starlinkTle != null) {
            val epochAge = SatelliteEngine.computeTleAgeDays(starlinkTle.line1)
            val stale = epochAge > 14.0
            return TleMetadata(noradId, TleDataSource.LIVE_NETWORK, epochAge, now, stale, starlinkTle)
        }

        // 4. Fallback to hardcoded catalog default (Final offline fallback)
        val fallback = SatelliteCatalog.satellites.find { it.noradId == noradId }?.defaultTle
            ?: ISSEngine.TLEData()
        val epochAge = SatelliteEngine.computeTleAgeDays(fallback.line1)
        return TleMetadata(noradId, TleDataSource.HARDCODED_FALLBACK, epochAge, 0L, true, fallback)
    }

    /**
     * Gets the most up-to-date TLE for a given NORAD satellite ID.
     * Looks up: Memory Cache -> Room Database -> Starlink Cache -> Hardcoded Offline Fallback in SatelliteCatalog.
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
                        val ageMs = System.currentTimeMillis() - ts
                        return ageMs > (maxAgeHours * 3600 * 1000L)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            return true // Not in DB -> stale
        }
        val ageMs = System.currentTimeMillis() - lastUpdated
        return ageMs > (maxAgeHours * 3600 * 1000L)
    }

    /**
     * Saves a validated TLE into the Room database and in-memory cache.
     */
    fun saveTle(noradId: Int, name: String, line1: String, line2: String, timestamp: Long = System.currentTimeMillis()) {
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

        // If ISS was updated, sync ISSEngine cached TLE
        if (noradId == 25544) {
            ISSEngine.cachedTLE = tle
        }
    }

    /**
     * Fetches live TLEs from CelesTrak feeds (stations, visual satellites, and starlink) using raw OkHttpClient,
     * validates mod-10 checksums, updates local persistence, and populates Starlink TLE memory cache.
     */
    suspend fun refreshTles(): Boolean = withContext(Dispatchers.IO) {
        val trackedNoradIds = SatelliteCatalog.satellites.map { it.noradId }.toSet()
        var anyUpdated = false

        val urls = listOf(
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle",
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=visual&FORMAT=tle",
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=starlink&FORMAT=tle"
        )

        for (endpoint in urls) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", "ZIG-Astronomy/1.0")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    if (!response.isSuccessful) {
                        Log.w(TAG, "HTTP error $responseCode while fetching TLEs from $endpoint")
                        return@use
                    }

                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrBlank()) {
                        Log.w(TAG, "parsed 0 TLEs — response format unexpected from $endpoint (empty body)")
                        return@use
                    }

                    val parsed = parseTleFeed(bodyString)
                    val countParsed = parsed.size
                    if (countParsed == 0) {
                        Log.w(TAG, "parsed 0 TLEs — response format unexpected from $endpoint")
                        return@use
                    }

                    var countStored = 0
                    val now = System.currentTimeMillis()

                    if (endpoint.contains("GROUP=starlink")) {
                        starlinkTles.putAll(parsed)
                        Log.i(TAG, "Stored ${parsed.size} Starlink TLEs in Starlink memory cache")
                    }

                    for ((noradId, tle) in parsed) {
                        if (noradId in trackedNoradIds || noradId == 25544) {
                            saveTle(noradId, tle.name, tle.line1, tle.line2, now)
                            countStored++
                            anyUpdated = true
                        }
                    }

                    Log.i(TAG, "Successfully fetched and parsed $countParsed TLEs from $endpoint, stored $countStored tracked TLEs into database")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception ${e.javaClass.simpleName}: ${e.message} while fetching TLEs from $endpoint", e)
            }
        }

        // Single fallback fetch for ISS if not updated or stale
        if (isStale(25544, 24)) {
            try {
                val issEndpoint = "https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=tle"
                val request = Request.Builder()
                    .url(issEndpoint)
                    .header("User-Agent", "ZIG-Astronomy/1.0")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank()) {
                            val parsed = parseTleFeed(bodyString)
                            val issTle = parsed[25544]
                            if (issTle != null) {
                                saveTle(25544, issTle.name, issTle.line1, issTle.line2)
                                anyUpdated = true
                                Log.i(TAG, "Direct ISS TLE fetch succeeded and stored for NORAD 25544")
                            } else {
                                Log.w(TAG, "parsed 0 TLEs — response format unexpected from $issEndpoint")
                            }
                        } else {
                            Log.w(TAG, "parsed 0 TLEs — response format unexpected from $issEndpoint (empty body)")
                        }
                    } else {
                        Log.w(TAG, "HTTP error ${response.code} while fetching direct ISS TLE from $issEndpoint")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception ${e.javaClass.simpleName}: ${e.message} while direct fetching ISS TLE", e)
            }
        }

        anyUpdated
    }

    companion object {
        private const val TAG = "TleRepository"

        @Volatile
        private var INSTANCE: TleRepository? = null

        fun getInstance(context: Context): TleRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TleRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        /**
         * Validates standard NORAD mod-10 checksum on a TLE line.
         * Characters 0-9 add their value, minus sign adds 1, all others add 0.
         * Checksum is stored in the last position (index 68 for 69-character lines).
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
         * Parses repeating 3-line blocks (Satellite Name, Line 1 starting with "1 ", Line 2 starting with "2 ")
         * or 2-line TLE format.
         */
        fun parseTleFeed(rawText: String): Map<Int, TLEData> {
            val result = mutableMapOf<Int, TLEData>()
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
                        val noradStr = line1.substring(2, 7).trim()
                        val noradId = noradStr.toIntOrNull()
                        if (noradId != null) {
                            val epochYr = StarlinkTrainManager.extractEpochYear(line1)
                            val epochDay = StarlinkTrainManager.extractEpochDay(line1)
                            result[noradId] = TLEData(
                                name = name,
                                line1 = line1,
                                line2 = line2,
                                epochYear = epochYr,
                                epochDay = epochDay
                            )
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
