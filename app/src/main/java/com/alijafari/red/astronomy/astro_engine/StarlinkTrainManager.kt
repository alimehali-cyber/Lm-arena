package com.alijafari.red.astronomy.astro_engine

import android.util.Log
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData

/**
 * Manages dynamic detection of freshly launched Starlink satellite trains
 * from live CelesTrak TLE feeds using orbital mean motion, COSPAR group clustering,
 * and epoch age validation.
 */
object StarlinkTrainManager {

    private const val TAG = "StarlinkTrainManager"

    fun extractCosparId(line1: String): String {
        return if (line1.length >= 17) line1.substring(10, 17).trim() else ""
    }

    fun extractMeanMotion(line2: String): Double {
        return try {
            if (line2.length >= 63) line2.substring(52, 63).trim().toDouble() else 0.0
        } catch (_: Exception) { 0.0 }
    }

    fun extractNoradId(line1: String): Int {
        return try {
            if (line1.length >= 7) line1.substring(2, 7).trim().toInt() else 0
        } catch (_: Exception) { 0 }
    }

    fun extractEpochYear(line1: String): Int {
        return try {
            if (line1.length >= 20) {
                val yr = line1.substring(18, 20).trim().toInt()
                if (yr < 57) yr + 2000 else yr + 1900
            } else 2026
        } catch (_: Exception) { 2026 }
    }

    fun extractEpochDay(line1: String): Double {
        return try {
            if (line1.length >= 32) line1.substring(20, 32).trim().toDouble() else 0.0
        } catch (_: Exception) { 0.0 }
    }

    fun epochToJulian(year: Int, day: Double): Double {
        // Convert TLE epoch (year + fractional day) to Julian Date
        val y = year
        val d = day.toLong()
        val f = day - d
        val jd = 367 * y - (7 * (y + 5001 + (d - 1) / 12)) / 4 + (275 * (d - 1)) / 9 + 1721028.5 + f
        return jd
    }

    /**
     * Detects freshly deployed Starlink train from a list of Starlink TLEs.
     *
     * Algorithm:
     * 1. Extract COSPAR ID, meanMotion, and epochDay for each TLE.
     * 2. Skip invalid entries (empty, blank, or 7 spaces).
     * 3. Group TLEs by cosparId.
     * 4. For each group, check:
     *    - group size >= 2
     *    - average meanMotion > 15.7 (fresh low deployment orbit)
     *    - average meanMotion < 16.5 (skip decayed / garbage orbits)
     *    - epochAge <= 7.0 days
     * 5. Select qualifying group with the largest size.
     * 6. Return SatelliteItem configured with the first TLE in the group.
     */
    fun detectTrain(starlinkTles: List<TLEData>): SatelliteItem? {
        if (starlinkTles.isEmpty()) {
            Log.d(TAG, "detectTrain: starlinkTles is empty, returning null")
            return null
        }

        val currentJulianDay = (System.currentTimeMillis() / 86400000.0) + 2440587.5

        // Step 1 & 2: Parse and filter out invalid cosparId
        val validTles = starlinkTles.filter { tle ->
            val rawCospar = if (tle.line1.length >= 17) tle.line1.substring(10, 17) else ""
            val cospar = rawCospar.trim()
            cospar.isNotEmpty() && rawCospar != "       "
        }

        if (validTles.isEmpty()) {
            Log.d(TAG, "detectTrain: No valid TLEs after COSPAR filtering")
            return null
        }

        // Step 3: Group TLEs by cosparId
        val groups = validTles.groupBy { extractCosparId(it.line1) }

        // Step 4: Check conditions for each group
        val qualifyingGroups = groups.filter { (cosparId, groupList) ->
            val groupSize = groupList.size
            if (groupSize < 2) return@filter false

            val meanMotions = groupList.map { extractMeanMotion(it.line2) }
            val avgMeanMotion = if (meanMotions.isNotEmpty()) meanMotions.average() else 0.0
            if (avgMeanMotion <= 15.7 || avgMeanMotion >= 16.5) return@filter false

            val first = groupList.first()
            val epochYr = if (first.epochYear != 0) first.epochYear else extractEpochYear(first.line1)
            val epochDay = if (first.epochDay != 0.0) first.epochDay else extractEpochDay(first.line1)
            val epochJd = epochToJulian(epochYr, epochDay)
            val epochAge = currentJulianDay - epochJd

            val qualifies = epochAge <= 7.0 && epochAge >= -1.0
            Log.d(TAG, "Group $cosparId: count=$groupSize, avgMeanMotion=$avgMeanMotion, epochAge=$epochAge, qualifies=$qualifies")
            qualifies
        }

        if (qualifyingGroups.isEmpty()) {
            Log.d(TAG, "detectTrain: No qualifying Starlink train groups found")
            return null
        }

        // Step 5: Pick the one with the largest group size
        val bestEntry = qualifyingGroups.maxByOrNull { it.value.size } ?: return null
        val bestGroup = bestEntry.value
        val groupSize = bestGroup.size
        val firstTle = bestGroup.first()
        val noradId = extractNoradId(firstTle.line1)
        val epochYr = if (firstTle.epochYear != 0) firstTle.epochYear else extractEpochYear(firstTle.line1)
        val epochDay = if (firstTle.epochDay != 0.0) firstTle.epochDay else extractEpochDay(firstTle.line1)
        val cospar = extractCosparId(firstTle.line1)

        Log.i(TAG, "Detected dynamic Starlink train: $groupSize satellites, NORAD $noradId, COSPAR $cospar")

        // Step 6: Create and return SatelliteItem
        return SatelliteItem(
            id = "starlink_train",
            noradId = noradId,
            nameEn = "Starlink Train ($groupSize sats)",
            nameFa = "قطار استارلینک ($groupSize ماهواره)",
            category = SatelliteCategory.STARLINK,
            designation = cospar,
            defaultTle = TLEData(
                name = "Starlink Train",
                line1 = firstTle.line1,
                line2 = firstTle.line2,
                epochYear = epochYr,
                epochDay = epochDay
            ),
            isConstellation = true,
            isTrain = true,
            trainCount = groupSize,
            starlinkTrainCount = groupSize,
            standardMagnitude = 2.0,
            isNakedEyeCandidate = true,
            descriptionEn = "A freshly launched train of $groupSize Starlink satellites flying in tight formation shortly after deployment.",
            descriptionFa = "صفی از $groupSize ماهواره استارلینک تازه‌پرتاب‌شده که در آرایه‌ای خطی و متراکم در آسمان حرکت می‌کنند.",
            launchDate = "Recent launch",
            operatorEn = "SpaceX",
            operatorFa = "اسپیس‌ایکس (SpaceX)",
            missionPurposeEn = "Deployment formation of low-Earth-orbit broadband internet satellites.",
            missionPurposeFa = "استقرار خطی ماهواره‌های اینترنت پهن‌باند در مدار نزدیک زمین.",
            scientificSignificanceEn = "Observable as a luminous 'pearl necklace' crossing the night sky before satellites raise to operational altitudes.",
            scientificSignificanceFa = "پدیده رصدی چشم‌نواز رشته مروارید در گرگ‌ومیش پیش از صعود ماهواره‌ها به مدارهای نهایی.",
            verifiedFactsEn = listOf(
                "Satellites fly in a tight line before slowly raising their orbits with electric ion thrusters.",
                "Visible as a luminous 'pearl necklace' crossing the night sky shortly after dusk or before dawn.",
                "Equipped with inter-satellite optical laser links for high-speed global mesh routing."
            ),
            verifiedFactsFa = listOf(
                "ماهواره‌ها پیش از اوج‌گیری با پیشران‌های یونی، مانند رشته مروارید حرکت می‌کنند.",
                "در ساعات اولیه پس از غروب یا پیش از طلوع به صورت یک خط درخشان رؤیت می‌شوند.",
                "مجهز به لیزرهای فضایی برای تبادل مستقیم داده بین ماهواره‌ای بدون نیاز به ایستگاه زمینی."
            )
        )
    }
}
