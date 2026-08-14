package com.alijafari.red.astronomy.data.repository

import android.content.Context
import android.util.Log
import com.alijafari.red.astronomy.astro_engine.ISSEngine
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository responsible for fetching, validating, persisting, and caching
 * Two-Line Element (TLE) satellite orbital data.
 *
 * Persists live TLEs into Room `cached_tle` table and keeps an in-memory cache
 * for zero-latency frame propagation during rendering.
 */
class TleRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context.applicationContext)
    private val memoryCache = ConcurrentHashMap<Int, TLEData>()
    private val timestampsCache = ConcurrentHashMap<Int, Long>()

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
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading cached TLEs from database: ${e.message}")
        }
    }

    /**
     * Gets the most up-to-date TLE for a given NORAD satellite ID.
     * Looks up: Memory Cache -> Room Database -> Hardcoded Offline Fallback in SatelliteCatalog.
     */
    fun getTle(noradId: Int): TLEData {
        // 1. Check in-memory cache
        memoryCache[noradId]?.let { return it }

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
                    return tle
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading TLE for $noradId from database: ${e.message}")
        }

        // 3. Fallback to hardcoded catalog default
        val fallback = SatelliteCatalog.satellites.find { it.noradId == noradId }?.defaultTle
            ?: ISSEngine.TLEData()
        return fallback
    }

    /**
     * Checks if the TLE for a given satellite NORAD ID is stale (older than maxAgeHours, default 72h).
     */
    fun isStale(noradId: Int, maxAgeHours: Int = 72): Boolean {
        val lastUpdated = timestampsCache[noradId]
        if (lastUpdated == null) {
            // Check DB
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
     * Fetches live TLEs from CelesTrak feeds (visual satellites and space stations),
     * validates mod-10 checksums, and updates local persistence.
     */
    suspend fun refreshTles(): Boolean = withContext(Dispatchers.IO) {
        val trackedNoradIds = SatelliteCatalog.satellites.map { it.noradId }.toSet()
        var anyUpdated = false

        val urls = listOf(
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=TLE",
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=visual&FORMAT=TLE"
        )

        for (endpoint in urls) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "REDAstronomy/1.0")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val content = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseTleFeed(content)
                    val now = System.currentTimeMillis()

                    for ((noradId, tle) in parsed) {
                        if (noradId in trackedNoradIds || noradId == 25544) {
                            saveTle(noradId, tle.name, tle.line1, tle.line2, now)
                            anyUpdated = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch TLEs from $endpoint: ${e.message}")
            }
        }

        // Single fallback fetch for ISS if not updated
        if (isStale(25544, 24)) {
            try {
                val issTle = fetchSingleNoradTle(25544)
                if (issTle != null) {
                    saveTle(25544, issTle.name, issTle.line1, issTle.line2)
                    anyUpdated = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed direct ISS TLE fetch: ${e.message}")
            }
        }

        anyUpdated
    }

    private suspend fun fetchSingleNoradTle(noradId: Int): TLEData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://celestrak.org/NORAD/elements/gp.php?CATNR=$noradId&FORMAT=TLE")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val content = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseTleFeed(content)
                return@withContext parsed[noradId]
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching single NORAD TLE ($noradId): ${e.message}")
        }
        null
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
         * Parses multi-satellite 2-line or 3-line TLE format.
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
                            result[noradId] = TLEData(name = name, line1 = line1, line2 = line2)
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
