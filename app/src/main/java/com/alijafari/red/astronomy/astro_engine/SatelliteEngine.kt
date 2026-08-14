package com.alijafari.red.astronomy.astro_engine

import android.util.Log
import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class CityLightPoint(
    val nameEn: String,
    val nameFa: String,
    val lat: Double,
    val lon: Double,
    val sizeDp: Float = 3f
)

data class SatelliteLiveState(
    val satellite: SatelliteItem,
    val topocentric: ISSEngine.TopocentricPosition,
    val apparentMagnitude: Double,
    val isNakedEyeVisible: Boolean,
    val visibilityVerdictEn: String,
    val visibilityVerdictFa: String,
    val reasonEn: String,
    val reasonFa: String
)

data class SubSolarPoint(
    val latDeg: Double,
    val lonDeg: Double,
    val gmstDeg: Double
)

object SatelliteEngine {

    val majorCityLights: List<CityLightPoint> = listOf(
        CityLightPoint("Tehran", "تهران", 35.6892, 51.3890, 4.5f),
        CityLightPoint("Mashhad", "مشهد", 36.2972, 59.6062, 3.5f),
        CityLightPoint("Isfahan", "اصفهان", 32.6546, 51.6680, 3.5f),
        CityLightPoint("Shiraz", "شیراز", 29.5926, 52.5836, 3.0f),
        CityLightPoint("Tabriz", "تبریز", 38.0800, 46.2919, 3.0f),
        CityLightPoint("Dubai", "دبی", 25.2048, 55.2708, 4.0f),
        CityLightPoint("Riyadh", "ریاض", 24.7136, 46.6753, 3.5f),
        CityLightPoint("Cairo", "قاهره", 30.0444, 31.2357, 4.5f),
        CityLightPoint("Istanbul", "استانبول", 41.0082, 28.9784, 4.0f),
        CityLightPoint("London", "لندن", 51.5074, -0.1278, 4.5f),
        CityLightPoint("Paris", "پاریس", 48.8566, 2.3522, 4.0f),
        CityLightPoint("Berlin", "برلین", 52.5200, 13.4050, 3.5f),
        CityLightPoint("Moscow", "مسکو", 55.7558, 37.6173, 4.5f),
        CityLightPoint("Tokyo", "توکیو", 35.6762, 139.6503, 5.0f),
        CityLightPoint("Beijing", "پکن", 39.9042, 116.4074, 4.5f),
        CityLightPoint("Shanghai", "شانگهای", 31.2304, 121.4737, 5.0f),
        CityLightPoint("Hong Kong", "هنگ کنگ", 22.3193, 114.1694, 4.0f),
        CityLightPoint("Seoul", "سئول", 37.5665, 126.9780, 4.5f),
        CityLightPoint("Mumbai", "ممبئی", 19.0760, 72.8777, 4.5f),
        CityLightPoint("Delhi", "دهلی", 28.6139, 77.2090, 4.5f),
        CityLightPoint("Bangkok", "بانکوک", 13.7563, 100.5018, 4.0f),
        CityLightPoint("Singapore", "سنگاپور", 1.3521, 103.8198, 4.0f),
        CityLightPoint("Sydney", "سیدنی", -33.8688, 151.2093, 3.5f),
        CityLightPoint("Melbourne", "ملبورن", -37.8136, 144.9631, 3.5f),
        CityLightPoint("New York", "نیویورک", 40.7128, -74.0060, 5.0f),
        CityLightPoint("Los Angeles", "لوس آنجلس", 34.0522, -118.2437, 4.5f),
        CityLightPoint("Chicago", "شیکاگو", 41.8781, -87.6298, 4.0f),
        CityLightPoint("Toronto", "تورنتو", 43.6532, -79.3832, 3.5f),
        CityLightPoint("Mexico City", "مکزیکوسیتی", 19.4326, -99.1332, 4.5f),
        CityLightPoint("Sao Paulo", "سائو پائولو", -23.5505, -46.6333, 4.5f),
        CityLightPoint("Buenos Aires", "بوئنوس آیرس", -34.6037, -58.3816, 3.5f),
        CityLightPoint("Johannesburg", "ژوهانسبورگ", -26.2041, 28.0473, 3.5f),
        CityLightPoint("Lagos", "لاگوس", 6.5244, 3.3792, 3.5f)
    )

    /**
     * Optional custom TLE resolver, dynamically set by TleRepository or app initialization.
     */
    var customTleResolver: ((noradId: Int) -> TLEData?)? = null

    /**
     * Logs TLE selection with source, epoch, and staleness age.
     */
    fun logTleSelection(source: String, tle: TLEData) {
        try {
            val line1 = tle.line1.trim()
            val epochStr = if (line1.length >= 32) line1.substring(18, 32).trim() else "UNKNOWN"
            val ageDays = computeTleAgeDays(line1)
            Log.i("TleSource", "Source: $source | Epoch: $epochStr | Age: ${String.format(Locale.US, "%.2f", ageDays)} days | Sat: ${tle.name}")
            if (ageDays > 10.0) {
                Log.w("TleSource", "Staleness Warning: TLE for ${tle.name} is ${String.format(Locale.US, "%.2f", ageDays)} days old (> 10 days)")
            }
        } catch (e: Exception) {
            Log.w("TleSource", "Error logging TLE source: ${e.message}")
        }
    }

    /**
     * Computes age in days of a TLE from its line 1 epoch (columns 19-32: YYDDD.DDDDDDDD).
     */
    fun computeTleAgeDays(line1: String): Double {
        try {
            if (line1.length >= 32) {
                val epochStr = line1.substring(18, 32).trim()
                if (epochStr.length >= 5) {
                    val yy = epochStr.substring(0, 2).toInt()
                    val fullYear = if (yy < 57) 2000 + yy else 1900 + yy
                    val epochDay = epochStr.substring(2).toDouble()
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        clear()
                        set(Calendar.YEAR, fullYear)
                        set(Calendar.DAY_OF_YEAR, 1)
                    }
                    val epochMs = cal.timeInMillis + ((epochDay - 1.0) * 86400000.0).toLong()
                    return (System.currentTimeMillis() - epochMs) / 86400000.0
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return 0.0
    }

    /**
     * Resolves the effective TLE for a satellite, checking custom resolver / repository first,
     * then ISS live cache, and falling back to the hardcoded catalog default.
     */
    fun getEffectiveTle(satellite: SatelliteItem, providedTle: TLEData? = null): TLEData {
        if (providedTle != null && providedTle != satellite.defaultTle) {
            logTleSelection("provided-custom", providedTle)
            return providedTle
        }
        val resolved = customTleResolver?.invoke(satellite.noradId)
        if (resolved != null) {
            logTleSelection("network-stored", resolved)
            return resolved
        }
        if (satellite.noradId == 25544 && ISSEngine.cachedTLE.line1 != satellite.defaultTle.line1) {
            logTleSelection("network-stored", ISSEngine.cachedTLE)
            return ISSEngine.cachedTLE
        }
        val fallback = providedTle ?: satellite.defaultTle
        logTleSelection("hardcoded-fallback", fallback)
        return fallback
    }

    /**
     * Calculates current sub-solar point on Earth for day/night terminator.
     */
    fun calculateSubSolarPoint(timestampMs: Long): SubSolarPoint {
        val jd = TimeEngine.getJulianDate(timestampMs)
        val sunPos = SunEngine.calculatePosition(jd)
        val gmstDeg = TimeEngine.getGMST(jd)
        
        var subLon = (sunPos.raDeg - gmstDeg) % 360.0
        if (subLon > 180.0) subLon -= 360.0
        if (subLon < -180.0) subLon += 360.0

        return SubSolarPoint(
            latDeg = sunPos.decDeg,
            lonDeg = subLon,
            gmstDeg = gmstDeg
        )
    }

    /**
     * Calculates satellite position, magnitude, and scientific naked-eye visibility status.
     * Prioritizes live/persisted TLE data over static catalog defaults.
     */
    fun calculateSatelliteState(
        satellite: SatelliteItem,
        timestampMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        tle: TLEData? = null
    ): SatelliteLiveState {
        val effectiveTle = getEffectiveTle(satellite, tle)
        val topo = ISSEngine.calculateTopocentricPos(
            timestampMs = timestampMs,
            userLatDeg = userLatDeg,
            userLonDeg = userLonDeg,
            userAltMeters = 940.0,
            tle = effectiveTle
        )

        // JWST exception (L2 orbit)
        if (satellite.id == "james_webb_space_telescope") {
            return SatelliteLiveState(
                satellite = satellite,
                topocentric = topo,
                apparentMagnitude = 14.5,
                isNakedEyeVisible = false,
                visibilityVerdictEn = "NO",
                visibilityVerdictFa = "خیر (نیاز به تلسکوپ)",
                reasonEn = "JWST is at Sun-Earth L2 Point (~1.5M km) and magnitude +14.5.",
                reasonFa = "جیمز وب در نقطه L2 زمین-خورشید (فاصله ۱.۵ میلیون کیلومتری) و قدر ۱۴.۵ قرار دارد."
            )
        }

        // Distance range ratio magnitude adjustment
        val baseMag = satellite.standardMagnitude
        val distanceRatio = topo.rangeKm / 400.0
        val mag = baseMag + 5.0 * Math.log10(Math.max(0.2, distanceRatio))

        val sunAlt = SunEngine.getSunAltitude(timestampMs, userLatDeg, userLonDeg)
        val isNightOrTwilight = sunAlt < -6.0
        val isAboveHorizon = topo.elevationDeg >= 10.0
        val isSunlit = topo.isSunlit

        val isNakedEye = satellite.isNakedEyeCandidate &&
                isNightOrTwilight &&
                isAboveHorizon &&
                isSunlit &&
                mag <= 4.5

        val verdictEn: String
        val verdictFa: String
        val reasonEn: String
        val reasonFa: String

        if (isNakedEye) {
            verdictEn = "YES (Naked-Eye Visible)"
            verdictFa = "بله (قابل مشاهده با چشم)"
            reasonEn = "Pass meets geometry: sunlit satellite in dark sky at ${String.format("%.1f", topo.elevationDeg)}° elevation."
            reasonFa = "گذر دارای شرایط ایده‌آل است: ماهواره درخشان در تاریکی شب در ارتفاع ${String.format("%.1f", topo.elevationDeg)} درجه."
        } else if (!isAboveHorizon) {
            verdictEn = "NO (Below Horizon)"
            verdictFa = "خیر (زیر افق)"
            reasonEn = "Satellite is below local horizon (${String.format("%.1f", topo.elevationDeg)}°)."
            reasonFa = "ماهواره در حال حاضر زیر افق محلی قرار دارد (${String.format("%.1f", topo.elevationDeg)} درجه)."
        } else if (!isNightOrTwilight) {
            verdictEn = "NO (Daylight Sky)"
            verdictFa = "خیر (روشنایی روز)"
            reasonEn = "Sky is too bright (Sun altitude ${String.format("%.1f", sunAlt)}°)."
            reasonFa = "آسمان روز روشن است (ارتفاع خورشید ${String.format("%.1f", sunAlt)} درجه)."
        } else if (!isSunlit) {
            verdictEn = "NO (In Earth Shadow)"
            verdictFa = "خیر (در سایه زمین)"
            reasonEn = "Satellite is currently eclipsed in Earth's shadow cone."
            reasonFa = "ماهواره در مخروط سایه زمین قرار داشته و تاریک است."
        } else if (mag > 4.5) {
            verdictEn = "Visibility Uncertain (Too Faint)"
            verdictFa = "وضعیت نامشخص (بسیار کم‌نور)"
            reasonEn = "Magnitude +${String.format("%.1f", mag)} is too dim for casual naked-eye observation."
            reasonFa = "درخشندگی ماهواره (قدر ${String.format("%.1f", mag)}+) برای مشاهده با چشم غیرمسلح بسیار کم است."
        } else {
            verdictEn = "Visibility Uncertain"
            verdictFa = "وضعیت نامشخص"
            reasonEn = "Viewing geometry is marginal."
            reasonFa = "شرایط هندسی مشاهده حاشیه‌ای است."
        }

        return SatelliteLiveState(
            satellite = satellite,
            topocentric = topo,
            apparentMagnitude = mag,
            isNakedEyeVisible = isNakedEye,
            visibilityVerdictEn = verdictEn,
            visibilityVerdictFa = verdictFa,
            reasonEn = reasonEn,
            reasonFa = reasonFa
        )
    }

    /**
     * Computes satellite ground track for past 45 mins and future 90 mins.
     */
    fun calculateGroundTrack(
        satellite: SatelliteItem,
        currentTimestampMs: Long,
        tle: TLEData? = null
    ): List<Pair<Double, Double>> {
        val effectiveTle = getEffectiveTle(satellite, tle)
        val points = mutableListOf<Pair<Double, Double>>()
        val stepMs = 3 * 60 * 1000L
        val startMs = currentTimestampMs - 45 * 60 * 1000L
        val endMs = currentTimestampMs + 90 * 60 * 1000L

        var t = startMs
        while (t <= endMs) {
            val topo = ISSEngine.calculateTopocentricPos(t, 0.0, 0.0, tle = effectiveTle)
            points.add(Pair(topo.subLatDeg, topo.subLonDeg))
            t += stepMs
        }
        return points
    }
}
