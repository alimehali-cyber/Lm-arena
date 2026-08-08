package com.alijafari.red.astronomy.astro_engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.*

object ISSEngine {

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
        val detailedReasonsFa: List<String>
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

    private var cachedTLE: TLEData = TLEData()

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
     * Calculates topocentric elevation, azimuth, velocity, and ECEF coordinates using SGP4/J2 model.
     */
    fun calculateTopocentricPos(
        timestampMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        userAltMeters: Double = 940.0,
        tle: TLEData = cachedTLE
    ): TopocentricPosition {
        val incDeg = parseDoubleSafe(tle.line2, 8, 16, 51.64)
        val raan0Deg = parseDoubleSafe(tle.line2, 17, 25, 200.0)
        val ecc = parseEccentricity(tle.line2, 26, 33, 0.0005)
        val argPerigee0Deg = parseDoubleSafe(tle.line2, 34, 42, 90.0)
        val meanAnomaly0Deg = parseDoubleSafe(tle.line2, 43, 51, 270.0)
        val meanMotionRevsDay = parseDoubleSafe(tle.line2, 52, 63, 15.49)

        val epochDayVal = parseDoubleSafe(tle.line1, 18, 32, 26213.5)
        val epochYear = 2000 + (epochDayVal / 1000.0).toInt()
        val epochDayOfYear = epochDayVal % 1000.0

        val epochJan1JD = getJan1JulianDate(epochYear)
        val epochJD = epochJan1JD + (epochDayOfYear - 1.0)

        val currentJD = TimeEngine.getJulianDate(timestampMs)
        val dtDays = currentJD - epochJD
        val dtSec = dtDays * 86400.0

        val mu = 398600.4418 // km^3/s^2
        val Re = 6378.137 // km
        val J2 = 1.08263e-3

        val n0RadSec = (meanMotionRevsDay * 2.0 * PI) / 86400.0
        val a = cbrt(mu / (n0RadSec * n0RadSec))

        val incRad = Math.toRadians(incDeg)
        val cosInc = cos(incRad)
        val sinInc = sin(incRad)

        // J2 Secular Rates
        val pFactor = 1.5 * J2 * (Re / a) * (Re / a) * n0RadSec
        val dRaanDtRadSec = -pFactor * cosInc
        val dArgPerigeeDtRadSec = 0.5 * pFactor * (5.0 * cosInc * cosInc - 1.0)

        val raanRad = Math.toRadians(raan0Deg) + dRaanDtRadSec * dtSec
        val argPerigeeRad = Math.toRadians(argPerigee0Deg) + dArgPerigeeDtRadSec * dtSec
        val M_rad = (Math.toRadians(meanAnomaly0Deg) + n0RadSec * dtSec) % (2.0 * PI)

        var E_rad = M_rad
        for (i in 0..10) {
            val f = E_rad - ecc * sin(E_rad) - M_rad
            val fPrime = 1.0 - ecc * cos(E_rad)
            val delta = f / fPrime
            E_rad -= delta
            if (abs(delta) < 1e-8) break
        }

        val sinE = sin(E_rad)
        val cosE = cos(E_rad)
        val sinNu = (sqrt(1.0 - ecc * ecc) * sinE) / (1.0 - ecc * cosE)
        val cosNu = (cosE - ecc) / (1.0 - ecc * cosE)
        val nuRad = atan2(sinNu, cosNu)

        val r = a * (1.0 - ecc * cosE)
        val uRad = argPerigeeRad + nuRad

        val xEci = r * (cos(uRad) * cos(raanRad) - sin(uRad) * sin(raanRad) * cosInc)
        val yEci = r * (cos(uRad) * sin(raanRad) + sin(uRad) * cos(raanRad) * cosInc)
        val zEci = r * (sin(uRad) * sinInc)

        val gmstDeg = TimeEngine.getGMST(currentJD)
        val gmstRad = Math.toRadians(gmstDeg)

        val xEcef = xEci * cos(gmstRad) + yEci * sin(gmstRad)
        val yEcef = -xEci * sin(gmstRad) + yEci * cos(gmstRad)
        val zEcef = zEci

        val subLonRad = atan2(yEcef, xEcef)
        val subLatRad = atan2(zEcef, sqrt(xEcef * xEcef + yEcef * yEcef))
        val subLatDeg = Math.toDegrees(subLatRad)
        val subLonDeg = Math.toDegrees(subLonRad)
        val satAltKm = r - Re
        val velKmS = sqrt(mu * (2.0 / r - 1.0 / a))

        // Check Earth Shadow Geometry (Umbra)
        val isSunlit = checkIssSunlit(currentJD, gmstDeg, xEcef, yEcef, zEcef)

        // Observer ECEF
        val obsLatRad = Math.toRadians(userLatDeg)
        val obsLonRad = Math.toRadians(userLonDeg)
        val obsR = Re + (userAltMeters / 1000.0)

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
     * Computes whether the satellite is illuminated by the Sun or blocked by Earth's cylindrical/conical shadow.
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

        // If satellite is on the night side of Earth relative to the Sun
        if (dParallel < 0.0) {
            val dPerpSq = (xEcef * xEcef + yEcef * yEcef + zEcef * zEcef) - (dParallel * dParallel)
            val ReSq = 6378.137 * 6378.137 // Earth radius squared in km^2
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
     * Scans orbit for 7 full days (168h) and returns predicted passes.
     */
    fun predictPasses(
        userLatDeg: Double,
        userLonDeg: Double,
        startTimestampMs: Long = System.currentTimeMillis(),
        tle: TLEData = cachedTLE,
        scanDays: Int = 7,
        visibleOnly: Boolean = true
    ): List<ISSPass> {
        val passes = mutableListOf<ISSPass>()
        val scanDurationMs = scanDays * 24 * 3600 * 1000L
        val stepMs = 20 * 1000L // 20-second scanning resolution
        val endTimeMs = startTimestampMs + scanDurationMs

        var currentTime = startTimestampMs
        var inPass = false
        var passStartMs = 0L
        var maxElev = -90.0
        var maxElevTimeMs = 0L
        var maxAz = 0.0
        var startAz = 0.0
        var endAz = 0.0
        var maxSatAlt = 415.0

        var shadowEntry: Long? = null
        var shadowExit: Long? = null
        var prevSunlit = true

        while (currentTime <= endTimeMs) {
            val pos = calculateTopocentricPos(currentTime, userLatDeg, userLonDeg, 940.0, tle)
            val elev = pos.elevationDeg

            if (!inPass && elev >= 10.0) {
                inPass = true
                passStartMs = currentTime
                maxElev = elev
                maxElevTimeMs = currentTime
                startAz = pos.azimuthDeg
                maxAz = pos.azimuthDeg
                maxSatAlt = pos.satAltKm
                shadowEntry = null
                shadowExit = null
                prevSunlit = pos.isSunlit
            } else if (inPass) {
                if (pos.isSunlit != prevSunlit) {
                    if (!pos.isSunlit && shadowEntry == null) shadowEntry = currentTime
                    if (pos.isSunlit && shadowExit == null) shadowExit = currentTime
                    prevSunlit = pos.isSunlit
                }

                if (elev > maxElev) {
                    maxElev = elev
                    maxElevTimeMs = currentTime
                    maxAz = pos.azimuthDeg
                    maxSatAlt = pos.satAltKm
                }

                if (elev < 10.0) {
                    inPass = false
                    endAz = pos.azimuthDeg
                    val passEndMs = currentTime

                    // Build & Classify Pass
                    val pass = buildPass(
                        passStartMs,
                        maxElevTimeMs,
                        passEndMs,
                        maxElev,
                        maxSatAlt,
                        startAz,
                        maxAz,
                        endAz,
                        shadowEntry,
                        shadowExit,
                        userLatDeg,
                        userLonDeg,
                        tle
                    )

                    if (!visibleOnly || (pass.classification != PassClassification.NOT_VISIBLE &&
                                pass.classification != PassClassification.INVISIBLE_SHADOW &&
                                pass.classification != PassClassification.DAYLIGHT_ONLY)) {
                        passes.add(pass)
                    }
                }
            }
            currentTime += stepMs
        }

        return passes
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
        tle: TLEData
    ): ISSPass {
        val posAtMax = calculateTopocentricPos(maxMs, userLatDeg, userLonDeg, 940.0, tle)
        val sunAlt = getObserverSunAltitude(maxMs, userLatDeg, userLonDeg)

        // Estimated apparent magnitude
        val baseMag = -1.5
        val rangeFactor = 5.0 * log10(posAtMax.rangeKm / 1000.0).coerceAtLeast(0.0)
        val extFactor = if (maxElevDeg < 15.0) 1.2 else 0.3
        val estMag = baseMag + rangeFactor + extFactor

        val isDarkness = sunAlt < -6.0
        val isSunlit = posAtMax.isSunlit

        val durationSec = maxOf(30L, (endMs - startMs) / 1000L)

        // Evaluation & Classification Logic
        val classification: PassClassification
        var score = 0
        val reasonsEn = mutableListOf<String>()
        val reasonsFa = mutableListOf<String>()

        if (sunAlt >= 0.0) {
            classification = PassClassification.DAYLIGHT_ONLY
            score = 10
            reasonsEn.add("✕ Pass occurs during daylight (Sun altitude > 0°)")
            reasonsFa.add("✕ گذر در طی روز و روشنایی خورشید رخ می‌دهد")
        } else if (!isSunlit) {
            classification = PassClassification.INVISIBLE_SHADOW
            score = 15
            reasonsEn.add("✕ ISS is inside Earth's umbra shadow (Not illuminated)")
            reasonsFa.add("✕ ایستگاه در سایه مخروطی زمین قرار دارد (تاریک)")
        } else {
            // Illuminated ISS in twilight or night sky!
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
                reasonsEn.add("✓ Civil twilight sky (Sun $sunAlt°)")
                reasonsFa.add("✓ گرگ و میش شهری (آسمان نیمه‌تاریک)")
            }

            reasonsEn.add("✓ ISS is brightly illuminated by solar radiation")
            reasonsFa.add("✓ ایستگاه در معرض مستقیم نور خورشید است")
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
                    reasonsEn.add("✕ Very low horizon pass")
                    reasonsFa.add("✕ گذر بسیار نزدیک به افق")
                }
            }

            classification = when {
                score >= 88 -> PassClassification.OUTSTANDING
                score >= 75 -> PassClassification.EXCELLENT
                score >= 65 -> PassClassification.VERY_GOOD
                score >= 50 -> PassClassification.GOOD
                score >= 35 -> PassClassification.MARGINAL
                else -> PassClassification.POOR
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

    private fun getJan1JulianDate(year: Int): Double {
        val y = year - 1
        return 1721424.5 + 365.0 * y + (y / 4) - (y / 100) + (y / 400) + 1.0
    }
}
