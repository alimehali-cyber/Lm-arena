package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Calendar
import java.util.TimeZone

object EclipseEngine {

    enum class EclipseType {
        SOLAR_TOTAL,
        SOLAR_ANNULAR,
        SOLAR_PARTIAL,
        LUNAR_TOTAL,
        LUNAR_PARTIAL,
        LUNAR_PENUMBRAL
    }

    data class EclipseEvent(
        val id: String,
        val isSolar: Boolean,
        val type: EclipseType,
        val nameEn: String,
        val nameFa: String,
        val dateUtcMs: Long,
        val descriptionEn: String,
        val descriptionFa: String,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
        val maxTotalityRegionEn: String,
        val maxTotalityRegionFa: String
    )

    data class EclipseResult(
        val event: EclipseEvent,
        val isLocallyVisible: Boolean,
        val localVisibilityTextEn: String,
        val localVisibilityTextFa: String,
        val formattedDateEn: String,
        val formattedDateFa: String
    )

    // Scientific catalog of upcoming real Solar & Lunar Eclipses 2026–2030
    private val UPCOMING_ECLIPSES = listOf(
        // 1. August 12, 2026 - Total Solar Eclipse
        EclipseEvent(
            id = "solar_2026_aug_12",
            isSolar = true,
            type = EclipseType.SOLAR_TOTAL,
            nameEn = "Total Solar Eclipse",
            nameFa = "خورشیدگرفتگی کامل (کسوف)",
            dateUtcMs = createUtcMs(2026, 8, 12, 17, 47),
            descriptionEn = "Totality crosses the Arctic, Greenland, Iceland, Atlantic, and Spain.",
            descriptionFa = "مسیر گرفتگی کامل از قطب شمال، گرینلند، ایسلند، اقیانوس اطلس و اسپانیا می‌گذرد.",
            minLat = 20.0, maxLat = 85.0, minLon = -100.0, maxLon = 65.0,
            maxTotalityRegionEn = "Spain, Iceland, Greenland",
            maxTotalityRegionFa = "اسپانیا، ایسلند، گرینلند"
        ),
        // 2. August 28, 2026 - Partial Lunar Eclipse
        EclipseEvent(
            id = "lunar_2026_aug_28",
            isSolar = false,
            type = EclipseType.LUNAR_PARTIAL,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "ماه گرفتگی جزئی (خسوف)",
            dateUtcMs = createUtcMs(2026, 8, 28, 4, 14),
            descriptionEn = "Moon passes partially into Earth's umbral shadow.",
            descriptionFa = "ماه به طور جزئی وارد سایه اصلی (امبرا) زمین می‌شود.",
            minLat = -60.0, maxLat = 75.0, minLon = -120.0, maxLon = 60.0,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا و خاورمیانه"
        ),
        // 3. February 6, 2027 - Annular Solar Eclipse
        EclipseEvent(
            id = "solar_2027_feb_06",
            isSolar = true,
            type = EclipseType.SOLAR_ANNULAR,
            nameEn = "Annular Solar Eclipse (Ring of Fire)",
            nameFa = "خورشیدگرفتگی حلقوی (حلقه آتش)",
            dateUtcMs = createUtcMs(2027, 2, 6, 16, 0),
            descriptionEn = "Annular ring of fire visible in South America, Atlantic, and West Africa.",
            descriptionFa = "حلقه آتشین خورشید در آمریکای جنوبی، اقیانوس اطلس و غرب آفریقا دیده می‌شود.",
            minLat = -60.0, maxLat = 30.0, minLon = -90.0, maxLon = 30.0,
            maxTotalityRegionEn = "Chile, Argentina, Ivory Coast, Ghana",
            maxTotalityRegionFa = "شیلی، آرژانتین، ساحل عاج، غنا"
        ),
        // 4. February 20, 2027 - Penumbral Lunar Eclipse
        EclipseEvent(
            id = "lunar_2027_feb_20",
            isSolar = false,
            type = EclipseType.LUNAR_PENUMBRAL,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "ماه گرفتگی نیم‌سایه‌ای",
            dateUtcMs = createUtcMs(2027, 2, 20, 23, 13),
            descriptionEn = "Subtle darkening of the lunar surface as it enters Earth's penumbra.",
            descriptionFa = "تاریکی ملایم لبه ماه به دلیل ورود به نیم‌سایه زمین.",
            minLat = -60.0, maxLat = 80.0, minLon = -150.0, maxLon = 60.0,
            maxTotalityRegionEn = "Americas, Europe, Africa, West Asia",
            maxTotalityRegionFa = "آمریکا، اروپا، آفریقا و غرب آسیا"
        ),
        // 5. August 2, 2027 - Total Solar Eclipse (Great Middle East Eclipse)
        EclipseEvent(
            id = "solar_2027_aug_02",
            isSolar = true,
            type = EclipseType.SOLAR_TOTAL,
            nameEn = "Total Solar Eclipse (Great Middle East Eclipse)",
            nameFa = "خورشیدگرفتگی کامل بزرگ خاورمیانه و شمال آفریقا",
            dateUtcMs = createUtcMs(2027, 8, 2, 10, 7),
            descriptionEn = "Spectacular totality lasting over 6 minutes over Egypt (Luxor), Spain, North Africa, Saudi Arabia, and Yemen.",
            descriptionFa = "گرفتگی کامل بی‌نظیر به مدت بیش از ۶ دقیقه روی مصر (الاقصر)، اسپانیا، شمال آفریقا، عربستان و یمن.",
            minLat = -10.0, maxLat = 55.0, minLon = -20.0, maxLon = 80.0,
            maxTotalityRegionEn = "Egypt (Luxor), Spain, Saudi Arabia, North Africa",
            maxTotalityRegionFa = "مصر (الاقصر)، اسپانیا، عربستان، شمال آفریقا"
        ),
        // 6. July 6, 2028 - Partial Lunar Eclipse
        EclipseEvent(
            id = "lunar_2028_jul_06",
            isSolar = false,
            type = EclipseType.LUNAR_PARTIAL,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "ماه گرفتگی جزئی",
            dateUtcMs = createUtcMs(2028, 7, 6, 18, 20),
            descriptionEn = "Moon passes partially into Earth's dark shadow.",
            descriptionFa = "ورود جزئی قرص ماه به سایه تاریک زمین.",
            minLat = -60.0, maxLat = 70.0, minLon = -100.0, maxLon = 80.0,
            maxTotalityRegionEn = "Europe, Africa, Asia, Americas",
            maxTotalityRegionFa = "اروپا، آفریقا، آسیا و قاره آمریکا"
        ),
        // 7. December 31, 2028 - Total Lunar Eclipse (New Year's Blood Moon)
        EclipseEvent(
            id = "lunar_2028_dec_31",
            isSolar = false,
            type = EclipseType.LUNAR_TOTAL,
            nameEn = "Total Lunar Eclipse (Blood Moon)",
            nameFa = "ماه گرفتگی کامل (ماه سرخ)",
            dateUtcMs = createUtcMs(2028, 12, 31, 16, 53),
            descriptionEn = "Total blood moon visible on New Year's Eve across Europe, Asia, Africa, and Australia.",
            descriptionFa = "ماه سرخ کامل در شب سال نو میلادی در سراسر اروپا، آسیا، آفریقا و استرالیا.",
            minLat = -50.0, maxLat = 80.0, minLon = -20.0, maxLon = 160.0,
            maxTotalityRegionEn = "Asia, Europe, Africa, Australia",
            maxTotalityRegionFa = "آسیا، اروپا، آفریقا و استرالیا"
        )
    )

    private fun createUtcMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Calculates the next solar and lunar eclipses relative to current time and user position.
     */
    fun getNextEclipses(
        nowMs: Long = System.currentTimeMillis(),
        userLatDeg: Double,
        userLonDeg: Double
    ): Pair<EclipseResult, EclipseResult> {
        val nextSolar = UPCOMING_ECLIPSES.firstOrNull { it.isSolar && it.dateUtcMs > nowMs - 86400000L }
            ?: UPCOMING_ECLIPSES.first { it.isSolar }

        val nextLunar = UPCOMING_ECLIPSES.firstOrNull { !it.isSolar && it.dateUtcMs > nowMs - 86400000L }
            ?: UPCOMING_ECLIPSES.first { !it.isSolar }

        return Pair(
            evaluateEclipse(nextSolar, userLatDeg, userLonDeg),
            evaluateEclipse(nextLunar, userLatDeg, userLonDeg)
        )
    }

    private fun evaluateEclipse(
        event: EclipseEvent,
        userLatDeg: Double,
        userLonDeg: Double
    ): EclipseResult {
        val jd = TimeEngine.getJulianDate(event.dateUtcMs)
        val gmstDeg = TimeEngine.getGMST(jd) * 15.0
        val isLocallyVisible = if (event.isSolar) {
            val sunPos = SunEngine.calculatePosition(jd)
            val horiz = CoordinateEngine.equatorialToHorizontal(
                CoordinateEngine.Equatorial(sunPos.raDeg, sunPos.decDeg),
                gmstDeg,
                userLatDeg
            )
            horiz.altitudeDeg > 0.0 && userLatDeg in event.minLat..event.maxLat && userLonDeg in event.minLon..event.maxLon
        } else {
            val moonPos = MoonEngine.calculateMoon(jd)
            val horiz = CoordinateEngine.equatorialToHorizontal(
                CoordinateEngine.Equatorial(moonPos.raDeg, moonPos.decDeg),
                gmstDeg,
                userLatDeg
            )
            horiz.altitudeDeg > 0.0 && userLatDeg in event.minLat..event.maxLat && userLonDeg in event.minLon..event.maxLon
        }

        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = event.dateUtcMs
        }
        val monthNamesEn = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val dayEn = cal.get(Calendar.DAY_OF_MONTH)
        val monthEn = monthNamesEn[cal.get(Calendar.MONTH)]
        val yearEn = cal.get(Calendar.YEAR)
        val formattedDateEn = "$monthEn $dayEn, $yearEn"

        val sh = TimeEngine.toSolarHijri(event.dateUtcMs)
        val formattedDateFa = "${sh.day} ${sh.monthNameFa} ${sh.year}".toPersianDigits()

        val localTextEn = if (isLocallyVisible) {
            "Locally Visible in your sky (${event.maxTotalityRegionEn})"
        } else {
            "Not Locally Visible (Best in ${event.maxTotalityRegionEn})"
        }

        val localTextFa = if (isLocallyVisible) {
            "قابل رصد در موقعیت جغرافیایی شما (${event.maxTotalityRegionFa})".toPersianDigits()
        } else {
            "در موقعیت شما قابل رصد مستقیم نیست (اصلی: ${event.maxTotalityRegionFa})".toPersianDigits()
        }

        return EclipseResult(
            event = event,
            isLocallyVisible = isLocallyVisible,
            localVisibilityTextEn = localTextEn,
            localVisibilityTextFa = localTextFa,
            formattedDateEn = formattedDateEn,
            formattedDateFa = formattedDateFa
        )
    }
}
