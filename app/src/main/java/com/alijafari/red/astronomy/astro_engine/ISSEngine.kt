package com.alijafari.red.astronomy.astro_engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.*

/**
 * ACCEPTANCE TEST:
 * Observer 30.11 N, 51.52 E, 2026-08-15, fresh TLE:
 * The ~04:04 local (UTC+3:30) pass with max elevation ~21° must be flagged isVisible = true;
 * The ~19:44 local twilight pass must be isVisible = false.
 *
 * ISS (International Space Station) tracking engine.
 * Uses SGP4 propagator for accurate TLE-based orbit prediction.
 */
class ISSEngine {

    enum class PassClassification(
        val labelEn: String,
        val labelFa: String,
        val colorHex: Long
    ) {
        OUTSTANDING("Outstanding Pass", "گذر فوق‌العاده استثنایی", 0xFF2DC653),
        EXCELLENT("Excellent Pass", "گذر عالی", 0xFF38B000),
        VERY_GOOD("Very Good Pass", "گذر بسیار خوب", 0xFF70E000),
        GOOD("Good Pass", "گذر خوب", 0xFF9EF01A),
        MARGINAL("Marginal Pass", "گذر حاشیه‌ای / کم‌نور", 0xFFFFB703),
        POOR("Poor Pass", "گذر ضعیف (ارتفاع پایین)", 0xFFFF8C00),
        NOT_VISIBLE("Not Visible", "قابل مشاهده نیست", 0xFFE63946),
        INVISIBLE_SHADOW("Invisible (In Shadow)", "پنهان در سایه زمین (تاریک)", 0xFF6C757D),
        DAYLIGHT_ONLY("Daylight Only", "گذر در روز (روشنایی خورشید)", 0xFF4A90E2)
    }

    data class TLEData(
        val name: String = "ISS (ZARYA)",
        val line1: String = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9993",
        val line2: String = "2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400001"
    )

    data class ISSPass(
        val startTimeMs: Long,
        val maxTimeMs: Long,
        val endTimeMs: Long,
        val maxElevationDeg: Double,
        val maxAltitudeKm: Double,
        val maxAzimuthDeg: Double,
        val startAzimuthDeg: Double,
        val endAzimuthDeg: Double,
        val estimatedMagnitude: Double,
        val passDurationSec: Long,
        val sunAltitudeDegAtMax: Double,
        val isObserverInDarkness: Boolean,
        val isIssSunlitAtMax: Boolean,
        val shadowEntryMs: Long? = null,
        val shadowExitMs: Long? = null,
        val classification: PassClassification,
        val visibilityScore: Int,
        val summaryReasonEn: String,
        val summaryReasonFa: String,
        val detailedReasonsEn: List<String>,
        val detailedReasonsFa: List<String>,
        val isVisible: Boolean = false
    )

    data class TopocentricPosition(
        val elevationDeg: Double,
        val azimuthDeg: Double,
        val rangeKm: Double,
        val subLatDeg: Double,
        val subLonDeg: Double,
        val satAltKm: Double,
        val velocityKmS: Double,
        val isSunlit: Boolean,
        val xEcef: Double,
        val yEcef: Double,
        val zEcef: Double
    )

    private val sgp4 = SGP4Propagator()
    var cachedTLE: TLEData = TLEData()

    /**
     * Fetches live TLE data from CelesTrak for NORAD ID 25544 (ISS).
     */
    suspend fun fetchLatestTLE(): TLEData = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://celestrak.org/NORAD/elements/gp.php?CATNR=25544&FORMAT=TLE")
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val text = connection.getInputStream().bufferedReader().use { it.readText() }
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

            if (lines.size >= 2) {
                val line1 = lines.find { it.startsWith("1 25544") }
                val line2 = lines.find { it.startsWith("2 25544") }
                if (line1 != null && line2 != null) {
                    val newTle = TLEData(
                        name = if (lines.size >= 3 && !lines[0].startsWith("1 ")) lines[0] else "ISS (ZARYA)",
                        line1 = line1,
                        line2 = line2
                    )
                    cachedTLE = newTle
                    return@withContext newTle
                }
            }
        } catch (e: Exception) {
            // Fallback to cached default TLE
        }
        return@withContext cachedTLE
    }

    /**
     * Calculates topocentric position using SGP4 propagator and frame transformations.
     */
    fun calculateTopocentricPos(
        timestampMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        userAltMeters: Double = 940.0,
        tle: TLEData = cachedTLE
    ): TopocentricPosition {
        val sgp4Tle = parseToSgp4(tle)
        val teme = sgp4.propagate(sgp4Tle, timestampMs)

        val currentJD = TimeEngine.getJulianDate(timestampMs)
        val gmstDeg = TimeEngine.getGMST(currentJD)
        val gmstRad = Math.toRadians(gmstDeg)

        // Rotate TEME (ECI) position to ECEF
        val xEcef = teme.xKm * cos(gmstRad) + teme.yKm * sin(gmstRad)
        val yEcef = -teme.xKm * sin(gmstRad) + teme.yKm * cos(gmstRad)
        val zEcef = teme.zKm

        val subLonRad = atan2(yEcef, xEcef)
        val subLatRad = atan2(zEcef, sqrt(xEcef * xEcef + yEcef * yEcef))
        val subLatDeg = Math.toDegrees(subLatRad)
        val subLonDeg = Math.toDegrees(subLonRad)

        val r = sqrt(xEcef * xEcef + yEcef * yEcef + zEcef * zEcef)
        val satAltKm = r - 6378.137
        val velKmS = sqrt(teme.vxKmS * teme.vxKmS + teme.vyKmS * teme.vyKmS + teme.vzKmS * teme.vzKmS)

        val isSunlit = checkIssSunlit(currentJD, gmstDeg, xEcef, yEcef, zEcef)

        // Observer ECEF position
        val obsLatRad = Math.toRadians(userLatDeg)
        val obsLonRad = Math.toRadians(userLonDeg)
        val obsR = 6378.137 + (userAltMeters / 1000.0)

        val obsXEcef = obsR * cos(obsLatRad) * cos(obsLonRad)
        val obsYEcef = obsR * cos(obsLatRad) * sin(obsLonRad)
        val obsZEcef = obsR * sin(obsLatRad)

        val dX = xEcef - obsXEcef
        val dY = yEcef - obsYEcef
        val dZ = zEcef - obsZEcef

        val east = -sin(obsLonRad) * dX + cos(obsLonRad) * dY
        val north = -sin(obsLatRad) * cos(obsLonRad) * dX - sin(obsLatRad) * sin(obsLonRad) * dY + cos(obsLatRad) * dZ
        val up = cos(obsLatRad) * cos(obsLonRad) * dX + cos(obsLatRad) * sin(obsLonRad) * dY + sin(obsLatRad) * dZ

        val range = sqrt(east * east + north * north + up * up)
        val elevRad = asin((up / range).coerceIn(-1.0, 1.0))
        val azRad = (atan2(east, north) + 2.0 * PI) % (2.0 * PI)

        return TopocentricPosition(
            elevationDeg = Math.toDegrees(elevRad),
            azimuthDeg = Math.toDegrees(azRad),
            rangeKm = range,
            subLatDeg = subLatDeg,
            subLonDeg = subLonDeg,
            satAltKm = satAltKm,
            velocityKmS = velKmS,
            isSunlit = isSunlit,
            xEcef = xEcef,
            yEcef = yEcef,
            zEcef = zEcef
        )
    }

    /**
     * Exact Earth Shadow (Umbra) Geometry calculation.
     */
    fun checkIssSunlit(jd: Double, gmstDeg: Double, xEcef: Double, yEcef: Double, zEcef: Double): Boolean {
        val sunPos = SunEngine.calculatePosition(jd)
        val raRad = Math.toRadians(sunPos.raDeg)
        val decRad = Math.toRadians(sunPos.decDeg)
        val gmstRad = Math.toRadians(gmstDeg)

        // Sun Unit Vector in ECEF
        val sunX = cos(decRad) * cos(raRad - gmstRad)
        val sunY = cos(decRad) * sin(raRad - gmstRad)
        val sunZ = sin(decRad)

        // Projection of satellite along Sun vector
        val dParallel = xEcef * sunX + yEcef * sunY + zEcef * sunZ

        if (dParallel < 0.0) {
            val dPerpSq = (xEcef * xEcef + yEcef * yEcef + zEcef * zEcef) - (dParallel * dParallel)
            val ReSq = 6378.137 * 6378.137
            if (dPerpSq < ReSq) {
                return false // In Earth's umbra shadow
            }
        }
        return true // In sunlight
    }

    /**
     * Calculates Sun altitude for observer at a specific time.
     */
    fun getObserverSunAltitude(timestampMs: Long, userLatDeg: Double, userLonDeg: Double): Double {
        val jd = TimeEngine.getJulianDate(timestampMs)
        val sunPos = SunEngine.calculatePosition(jd)
        val gmstDeg = TimeEngine.getGMST(jd)
        val lastDeg = gmstDeg + userLonDeg
        val horiz = CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
            lastDeg,
            userLatDeg
        )
        return horiz.altitudeDeg
    }

    /**
     * Determines whether the satellite is sunlit in ECI coordinates using SGP4 position vector s
     * and Sun unit vector u (cylindrical Earth-shadow model with Earth radius 6378.137 km).
     */
    fun isSatelliteSunlitEci(timestampMs: Long, tle: TLEData): Boolean {
        val sgp4Tle = parseToSgp4(tle)
        val teme = sgp4.propagate(sgp4Tle, timestampMs)
        val jd = TimeEngine.getJulianDate(timestampMs)
        val sunPos = SunEngine.calculatePosition(jd)
        val raRad = Math.toRadians(sunPos.raDeg)
        val decRad = Math.toRadians(sunPos.decDeg)

        // Unit vector u from Earth to Sun in ECI (TEME / Equatorial)
        val ux = cos(decRad) * cos(raRad)
        val uy = cos(decRad) * sin(raRad)
        val uz = sin(decRad)

        val sx = teme.xKm
        val sy = teme.yKm
        val sz = teme.zKm

        val proj = sx * ux + sy * uy + sz * uz
        if (proj >= 0.0) {
            return true // Day side hemisphere -> sunlit
        }
        val perpX = sx - proj * ux
        val perpY = sy - proj * uy
        val perpZ = sz - proj * uz
        val perpDist = sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ)
        return perpDist >= 6378.137 // True if outside cylindrical shadow cone
    }

    /**
     * Checks if a single temporal sample satisfies the 3 human visibility conditions:
     * (a) satellite elevation >= 10°
     * (b) observer sky is dark (Sun altitude between -6° and -30°)
     * (c) satellite is sunlit
     */
    fun checkSampleVisibility(
        timestampMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        tle: TLEData
    ): Boolean {
        val topo = calculateTopocentricPos(timestampMs, userLatDeg, userLonDeg, 940.0, tle)
        if (topo.elevationDeg < 10.0) return false
        val sunAlt = getObserverSunAltitude(timestampMs, userLatDeg, userLonDeg)
        if (sunAlt < -30.0 || sunAlt > -6.0) return false
        return isSatelliteSunlitEci(timestampMs, tle)
    }

    /**
     * Two-stage orbital pass prediction:
     * Stage 1: Coarse 30-second scan to detect candidate passes across the scan window.
     * Stage 2: Fine bisection and golden-section optimization for sub-second precision on
     *          Rise (AOS 10°), Peak Max Elevation (TCA), and Set (LOS 10°), as well as shadow boundaries.
     * Evaluates 3-point sample visibility (rise, culmination, set) and filters/sorts passes.
     */
    fun predictPasses(
        userLatDeg: Double,
        userLonDeg: Double,
        startTimestampMs: Long = System.currentTimeMillis(),
        tle: TLEData = cachedTLE,
        scanDays: Int = 7,
        visibleOnly: Boolean = true,
        standardMag: Double = -1.5
    ): List<ISSPass> {
        val effectiveTle = if (tle == cachedTLE && SatelliteEngine.customTleResolver != null) {
            SatelliteEngine.customTleResolver?.invoke(25544) ?: tle
        } else {
            tle
        }
        val sourceLabel = if (effectiveTle != cachedTLE || SatelliteEngine.customTleResolver != null) "network-stored" else "hardcoded-fallback"
        SatelliteEngine.logTleSelection(sourceLabel, effectiveTle)

        val passes = mutableListOf<ISSPass>()
        val scanDurationMs = scanDays * 24 * 3600 * 1000L
        val coarseStepMs = 30 * 1000L
        val endTimeMs = startTimestampMs + scanDurationMs

        var currentTime = startTimestampMs
        var prevTime = startTimestampMs
        var prevElev = calculateTopocentricPos(currentTime, userLatDeg, userLonDeg, 940.0, effectiveTle).elevationDeg
        var inPass = prevElev >= 10.0
        var passStartCoarseMs = if (inPass) currentTime else 0L

        var rawMaxElev = prevElev
        var rawMaxTimeMs = currentTime

        var shadowEntry: Long? = null
        var shadowExit: Long? = null
        var prevSunlit = calculateTopocentricPos(currentTime, userLatDeg, userLonDeg, 940.0, effectiveTle).isSunlit

        while (currentTime <= endTimeMs) {
            currentTime += coarseStepMs
            val pos = calculateTopocentricPos(currentTime, userLatDeg, userLonDeg, 940.0, effectiveTle)
            val elev = pos.elevationDeg

            if (!inPass && elev >= 10.0) {
                inPass = true
                // Fine bisection for exact AOS (crossing 10° threshold)
                passStartCoarseMs = refineElevationThreshold(
                    prevTime,
                    currentTime,
                    10.0,
                    isRising = true,
                    userLatDeg,
                    userLonDeg,
                    effectiveTle
                )
                rawMaxElev = elev
                rawMaxTimeMs = currentTime
                shadowEntry = null
                shadowExit = null
                prevSunlit = pos.isSunlit
            } else if (inPass) {
                if (pos.isSunlit != prevSunlit) {
                    val shadowTransitionTime = refineShadowTransition(
                        prevTime,
                        currentTime,
                        userLatDeg,
                        userLonDeg,
                        effectiveTle
                    )
                    if (!pos.isSunlit && shadowEntry == null) shadowEntry = shadowTransitionTime
                    if (pos.isSunlit && shadowExit == null) shadowExit = shadowTransitionTime
                    prevSunlit = pos.isSunlit
                }

                if (elev > rawMaxElev) {
                    rawMaxElev = elev
                    rawMaxTimeMs = currentTime
                }

                if (elev < 10.0) {
                    inPass = false
                    // Fine bisection for exact LOS (crossing 10° threshold downwards)
                    val refinedEndMs = refineElevationThreshold(
                        prevTime,
                        currentTime,
                        10.0,
                        isRising = false,
                        userLatDeg,
                        userLonDeg,
                        effectiveTle
                    )

                    // Fine golden section search for exact peak time and maximum elevation
                    val (refinedMaxTimeMs, refinedPeakPos) = refinePeakElevation(
                        centerTimeMs = rawMaxTimeMs,
                        searchRadiusMs = coarseStepMs + 5000L,
                        userLatDeg = userLatDeg,
                        userLonDeg = userLonDeg,
                        tle = effectiveTle
                    )

                    val startPos = calculateTopocentricPos(passStartCoarseMs, userLatDeg, userLonDeg, 940.0, effectiveTle)
                    val endPos = calculateTopocentricPos(refinedEndMs, userLatDeg, userLonDeg, 940.0, effectiveTle)

                    // Sample visibility at rise, culmination, and set
                    val isRiseVis = checkSampleVisibility(passStartCoarseMs, userLatDeg, userLonDeg, effectiveTle)
                    val isMaxVis = checkSampleVisibility(refinedMaxTimeMs, userLatDeg, userLonDeg, effectiveTle)
                    val isSetVis = checkSampleVisibility(refinedEndMs, userLatDeg, userLonDeg, effectiveTle)
                    val isPassVisible = isRiseVis || isMaxVis || isSetVis

                    val pass = buildPass(
                        startMs = passStartCoarseMs,
                        maxMs = refinedMaxTimeMs,
                        endMs = refinedEndMs,
                        maxElevDeg = refinedPeakPos.elevationDeg,
                        maxSatAltKm = refinedPeakPos.satAltKm,
                        startAzDeg = startPos.azimuthDeg,
                        maxAzDeg = refinedPeakPos.azimuthDeg,
                        endAzDeg = endPos.azimuthDeg,
                        shadowEntryMs = shadowEntry,
                        shadowExitMs = shadowExit,
                        userLatDeg = userLatDeg,
                        userLonDeg = userLonDeg,
                        tle = effectiveTle,
                        standardMag = standardMag,
                        isVisible = isPassVisible
                    )

                    passes.add(pass)
                }
            }
            prevTime = currentTime
            prevElev = elev
        }

        val filteredPasses = if (visibleOnly) {
            passes.filter { it.isVisible }
        } else {
            passes
        }

        return filteredPasses.sortedWith(
            compareByDescending<ISSPass> { it.isVisible }
                .thenBy { it.startTimeMs }
        )
    }

    /**
     * Refines the timestamp where satellite elevation crosses a threshold (e.g. 10.0°) using bisection.
     */
    private fun refineElevationThreshold(
        t1: Long,
        t2: Long,
        targetElevationDeg: Double,
        isRising: Boolean,
        userLatDeg: Double,
        userLonDeg: Double,
        tle: TLEData
    ): Long {
        var low = minOf(t1, t2)
        var high = maxOf(t1, t2)
        var bestTime = (low + high) / 2

        for (iter in 0..12) {
            val mid = (low + high) / 2
            val elev = calculateTopocentricPos(mid, userLatDeg, userLonDeg, 940.0, tle).elevationDeg
            bestTime = mid
            if (isRising) {
                if (elev < targetElevationDeg) {
                    low = mid
                } else {
                    high = mid
                }
            } else {
                if (elev > targetElevationDeg) {
                    low = mid
                } else {
                    high = mid
                }
            }
        }
        return bestTime
    }

    /**
     * Refines peak elevation and its exact timestamp using golden-section search.
     */
    private fun refinePeakElevation(
        centerTimeMs: Long,
        searchRadiusMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        tle: TLEData
    ): Pair<Long, TopocentricPosition> {
        var a = centerTimeMs - searchRadiusMs
        var b = centerTimeMs + searchRadiusMs
        val invPhi = 0.618033988749895
        val invPhiSq = 0.381966011250105

        var c = a + (invPhiSq * (b - a)).toLong()
        var d = a + (invPhi * (b - a)).toLong()
        var posC = calculateTopocentricPos(c, userLatDeg, userLonDeg, 940.0, tle)
        var posD = calculateTopocentricPos(d, userLatDeg, userLonDeg, 940.0, tle)

        for (iter in 0..12) {
            if (posC.elevationDeg < posD.elevationDeg) {
                a = c
                c = d
                posC = posD
                d = a + (invPhi * (b - a)).toLong()
                posD = calculateTopocentricPos(d, userLatDeg, userLonDeg, 940.0, tle)
            } else {
                b = d
                d = c
                posD = posC
                c = a + (invPhiSq * (b - a)).toLong()
                posC = calculateTopocentricPos(c, userLatDeg, userLonDeg, 940.0, tle)
            }
        }

        val bestTime = (a + b) / 2
        val bestPos = calculateTopocentricPos(bestTime, userLatDeg, userLonDeg, 940.0, tle)
        return Pair(bestTime, bestPos)
    }

    /**
     * Refines shadow entry/exit boundary to 1-second accuracy.
     */
    private fun refineShadowTransition(
        t1: Long,
        t2: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        tle: TLEData
    ): Long {
        var low = minOf(t1, t2)
        var high = maxOf(t1, t2)
        val initialSunlit = calculateTopocentricPos(low, userLatDeg, userLonDeg, 940.0, tle).isSunlit

        for (iter in 0..10) {
            val mid = (low + high) / 2
            val sunlit = calculateTopocentricPos(mid, userLatDeg, userLonDeg, 940.0, tle).isSunlit
            if (sunlit == initialSunlit) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2
    }

    private fun buildPass(
        startMs: Long,
        maxMs: Long,
        endMs: Long,
        maxElevDeg: Double,
        maxSatAltKm: Double,
        startAzDeg: Double,
        maxAzDeg: Double,
        endAzDeg: Double,
        shadowEntryMs: Long?,
        shadowExitMs: Long?,
        userLatDeg: Double,
        userLonDeg: Double,
        tle: TLEData,
        standardMag: Double = -1.5,
        isVisible: Boolean = false
    ): ISSPass {
        val posAtMax = calculateTopocentricPos(maxMs, userLatDeg, userLonDeg, 940.0, tle)
        val sunAlt = getObserverSunAltitude(maxMs, userLatDeg, userLonDeg)

        val rangeFactor = 5.0 * log10(maxOf(0.1, posAtMax.rangeKm / 400.0))
        val extFactor = if (maxElevDeg < 15.0) 0.5 else 0.0
        val estMag = standardMag + rangeFactor + extFactor

        val isDarkness = sunAlt <= -6.0
        val isSunlit = posAtMax.isSunlit

        val durationSec = maxOf(30L, (endMs - startMs) / 1000L)

        val classification: PassClassification
        var score = 0
        val reasonsEn = mutableListOf<String>()
        val reasonsFa = mutableListOf<String>()

        if (sunAlt > -6.0) {
            classification = PassClassification.DAYLIGHT_ONLY
            score = 10
            reasonsEn.add("✕ Pass occurs before civil twilight ends (Sun altitude > -6°)")
            reasonsFa.add("✕ گذر پیش از پایان گرگ و میش شهری (ارتفاع خورشید بالای ۶- درجه) رخ می‌دهد")
        } else if (!isSunlit) {
            classification = PassClassification.INVISIBLE_SHADOW
            score = 15
            reasonsEn.add("✕ Satellite is inside Earth's umbra shadow (Not illuminated)")
            reasonsFa.add("✕ ماهواره در سایه مخروطی زمین قرار دارد (تاریک)")
        } else if (estMag > 4.5) {
            classification = PassClassification.NOT_VISIBLE
            score = 20
            reasonsEn.add("✕ Apparent magnitude (+${String.format("%.1f", estMag)}) is fainter than naked-eye threshold (+4.5)")
            reasonsFa.add("✕ قدر ظاهری (${String.format("%.1f", estMag)}+) کم‌نورتر از آستانه چشم غیرمسلح (۴.۵+) است")
        } else {
            if (sunAlt <= -18.0) {
                score += 40
                reasonsEn.add("✓ Observer in true astronomical darkness (Sun $sunAlt°)")
                reasonsFa.add("✓ ناظر در تاریکی کامل نجومی قرار دارد")
            } else if (sunAlt <= -12.0) {
                score += 35
                reasonsEn.add("✓ Nautical twilight darkness (Sun $sunAlt°)")
                reasonsFa.add("✓ گرگ و میش دریانوردی (شرایط عالی)")
            } else if (sunAlt <= -6.0) {
                score += 25
                reasonsEn.add("✓ Civil twilight ended (Sun $sunAlt°)")
                reasonsFa.add("✓ پایان گرگ و میش شهری (آسمان تاریک)")
            }

            reasonsEn.add("✓ Satellite is brightly illuminated by solar radiation")
            reasonsFa.add("✓ ماهواره در معرض مستقیم نور خورشید است")
            score += 30

            when {
                maxElevDeg >= 45.0 -> {
                    score += 30
                    reasonsEn.add("✓ High peak elevation (${maxElevDeg.toInt()}°)")
                    reasonsFa.add("✓ زاویه اوج بسیار بالا (${maxElevDeg.toInt()} درجه)")
                }
                maxElevDeg >= 25.0 -> {
                    score += 20
                    reasonsEn.add("✓ Good peak elevation (${maxElevDeg.toInt()}°)")
                    reasonsFa.add("✓ زاویه اوج مناسب (${maxElevDeg.toInt()} درجه)")
                }
                maxElevDeg >= 12.0 -> {
                    score += 10
                    reasonsEn.add("⚠ Low peak elevation (${maxElevDeg.toInt()}°)")
                    reasonsFa.add("⚠ زاویه اوج پایین (${maxElevDeg.toInt()} درجه)")
                }
                else -> {
                    score += 2
                    reasonsEn.add("✕ Low horizon pass")
                    reasonsFa.add("✕ گذر نزدیک به افق")
                }
            }

            classification = when {
                score >= 88 -> PassClassification.OUTSTANDING
                score >= 75 -> PassClassification.EXCELLENT
                score >= 65 -> PassClassification.VERY_GOOD
                score >= 50 -> PassClassification.GOOD
                else -> PassClassification.MARGINAL
            }
        }

        val summaryEn = when (classification) {
            PassClassification.OUTSTANDING -> "Brilliant overhead pass illuminated in dark night sky! Mag ${String.format("%.1f", estMag)}"
            PassClassification.EXCELLENT -> "Easily visible naked-eye pass with high elevation (${maxElevDeg.toInt()}°)"
            PassClassification.VERY_GOOD -> "Bright pass visible across dusk/dawn sky"
            PassClassification.GOOD -> "Visible as a moving bright star across open skies"
            PassClassification.MARGINAL -> "Faint or low horizon pass"
            PassClassification.POOR -> "Obscured by atmospheric extinction or low elevation"
            PassClassification.NOT_VISIBLE -> "Not observable by unaided eye"
            PassClassification.INVISIBLE_SHADOW -> "ISS enters Earth shadow during flyover"
            PassClassification.DAYLIGHT_ONLY -> "Sky too bright during daylight hours"
        }

        val summaryFa = when (classification) {
            PassClassification.OUTSTANDING -> "گذر درخشان بالای سر در تاریکی کامل! قدر ${String.format("%.1f", estMag)}"
            PassClassification.EXCELLENT -> "گذر کاملاً واضح با چشم غیرمسلح و ارتفاع بالا (${maxElevDeg.toInt()} درجه)"
            PassClassification.VERY_GOOD -> "گذر پرنور و جذاب در شفق شامگاهی یا سپیده‌دم"
            PassClassification.GOOD -> "مانند یک ستاره پرنور متحرک در آسمان دیده می‌شود"
            PassClassification.MARGINAL -> "گذر کم‌نور یا نزدیک افق"
            PassClassification.POOR -> "تحت تاثیر غبار افق یا ارتفاع بسیار پایین"
            PassClassification.NOT_VISIBLE -> "غیرقابل رویت با چشم غیرمسلح"
            PassClassification.INVISIBLE_SHADOW -> "ایستگاه در طی مسیر وارد سایه زمین می‌شود"
            PassClassification.DAYLIGHT_ONLY -> "روشنایی خورشید مانع دیدن ایستگاه است"
        }

        return ISSPass(
            startTimeMs = startMs,
            maxTimeMs = maxMs,
            endTimeMs = endMs,
            maxElevationDeg = maxElevDeg,
            maxAltitudeKm = maxSatAltKm,
            maxAzimuthDeg = maxAzDeg,
            startAzimuthDeg = startAzDeg,
            endAzimuthDeg = endAzDeg,
            estimatedMagnitude = estMag,
            passDurationSec = durationSec,
            sunAltitudeDegAtMax = sunAlt,
            isObserverInDarkness = isDarkness,
            isIssSunlitAtMax = isSunlit,
            shadowEntryMs = shadowEntryMs,
            shadowExitMs = shadowExitMs,
            classification = classification,
            visibilityScore = score.coerceIn(0, 100),
            summaryReasonEn = summaryEn,
            summaryReasonFa = summaryFa,
            detailedReasonsEn = reasonsEn,
            detailedReasonsFa = reasonsFa
        )
    }

    private fun parseToSgp4(tle: TLEData): SGP4Propagator.TLEData {
        val epochYear = parseDoubleSafe(tle.line1, 18, 20, 24.0).toInt()
        val epochDay = parseDoubleSafe(tle.line1, 20, 32, 225.5)
        val bStar = parseBStar(tle.line1)

        val inc = parseDoubleSafe(tle.line2, 8, 16, 51.64)
        val raan = parseDoubleSafe(tle.line2, 17, 25, 200.0)
        val ecc = parseEccentricity(tle.line2, 26, 33, 0.0005)
        val argp = parseDoubleSafe(tle.line2, 34, 42, 90.0)
        val ma = parseDoubleSafe(tle.line2, 43, 51, 270.0)
        val mm = parseDoubleSafe(tle.line2, 52, 63, 15.49)

        return SGP4Propagator.TLEData(
            epochYear = epochYear,
            epochDay = epochDay,
            inclinationDeg = inc,
            raanDeg = raan,
            eccentricity = ecc,
            argPerigeeDeg = argp,
            meanAnomalyDeg = ma,
            meanMotion = mm,
            bStar = bStar
        )
    }

    private fun parseBStar(line1: String): Double {
        try {
            if (line1.length >= 61) {
                val bStr = line1.substring(53, 61).trim()
                if (bStr.isEmpty() || bStr == "00000-0" || bStr == "00000+0") return 0.0

                var sign = 1.0
                var s = bStr
                if (s.startsWith("-")) {
                    sign = -1.0
                    s = s.substring(1)
                } else if (s.startsWith("+")) {
                    s = s.substring(1)
                }
                val dashIdx = maxOf(s.indexOf('-'), s.indexOf('+'))
                if (dashIdx > 0) {
                    val mantissaStr = "0." + s.substring(0, dashIdx)
                    val expStr = s.substring(dashIdx)
                    val mantissa = mantissaStr.toDouble()
                    val exp = expStr.toDouble()
                    return sign * mantissa * 10.0.pow(exp)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return 0.0001
    }

    private fun parseDoubleSafe(str: String, start: Int, end: Int, defaultVal: Double): Double {
        return try {
            if (end <= str.length) str.substring(start, end).trim().toDouble() else defaultVal
        } catch (e: Exception) {
            defaultVal
        }
    }

    private fun parseEccentricity(str: String, start: Int, end: Int, defaultVal: Double): Double {
        return try {
            if (end <= str.length) ("0." + str.substring(start, end).trim()).toDouble() else defaultVal
        } catch (e: Exception) {
            defaultVal
        }
    }

    companion object {
        val defaultEngine = ISSEngine()

        var cachedTLE: TLEData
            get() = defaultEngine.cachedTLE
            set(value) {
                defaultEngine.cachedTLE = value
            }

        suspend fun fetchLatestTLE(): TLEData = defaultEngine.fetchLatestTLE()

        fun calculateTopocentricPos(
            timestampMs: Long,
            userLatDeg: Double,
            userLonDeg: Double,
            userAltMeters: Double = 940.0,
            tle: TLEData = defaultEngine.cachedTLE
        ): TopocentricPosition = defaultEngine.calculateTopocentricPos(
            timestampMs, userLatDeg, userLonDeg, userAltMeters, tle
        )

        fun checkIssSunlit(jd: Double, gmstDeg: Double, xEcef: Double, yEcef: Double, zEcef: Double): Boolean =
            defaultEngine.checkIssSunlit(jd, gmstDeg, xEcef, yEcef, zEcef)

        fun getObserverSunAltitude(timestampMs: Long, userLatDeg: Double, userLonDeg: Double): Double =
            defaultEngine.getObserverSunAltitude(timestampMs, userLatDeg, userLonDeg)

        fun predictPasses(
            userLatDeg: Double,
            userLonDeg: Double,
            startTimestampMs: Long = System.currentTimeMillis(),
            tle: TLEData = defaultEngine.cachedTLE,
            scanDays: Int = 7,
            visibleOnly: Boolean = true,
            standardMag: Double = -1.5
        ): List<ISSPass> = defaultEngine.predictPasses(
            userLatDeg, userLonDeg, startTimestampMs, tle, scanDays, visibleOnly, standardMag
        )
    }
}

