package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.domain.CalendarSystem
import com.alijafari.red.astronomy.util.toPersianDigits
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * High-precision Eclipse prediction and local circumstances engine.
 *
 * Implements:
 * - Meeus (1998) Chapter 54 (Besselian elements & Eclipse predictions)
 * - Meeus (1998) Chapter 49 (Phases of the Moon with ΔT TT→UTC conversion)
 * - NASA Goddard / Fred Espenak 5-Millennium Canon of Solar & Lunar Eclipses
 * - High-precision topocentric local circumstances (Start C1/P1, Max, End C4/P4, Alt/Az, Obscuration %)
 */
class EclipseEngine {

    companion object {
        private const val DEG2RAD = Math.PI / 180.0
        private const val RAD2DEG = 180.0 / Math.PI
        private const val SYNODIC_MONTH_DAYS = 29.530588853
        private const val EARTH_EQUATORIAL_RADIUS_KM = 6378.137
        private const val EARTH_FLATTENING = 1.0 / 298.257223563
        private const val SUN_RADIUS_KM = 696000.0
        private const val MOON_RADIUS_KM = 1737.4
        private const val AU_KM = 149597870.7

        // Shared singleton instance
        val instance = EclipseEngine()

        /**
         * Convenience helper to evaluate next eclipses for a user location.
         */
        fun getNextEclipses(
            nowMs: Long,
            userLatDeg: Double,
            userLonDeg: Double,
            elevationM: Double = 0.0,
            timezoneId: String = "Asia/Tehran",
            calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI
        ): Pair<EclipseResult, EclipseResult> {
            return instance.getNextEclipses(
                nowMs = nowMs,
                userLatDeg = userLatDeg,
                userLonDeg = userLonDeg,
                elevationM = elevationM,
                timezoneId = timezoneId,
                calendarSystem = calendarSystem
            )
        }

        /**
         * Convenience helper for detailed eclipse info.
         */
        fun getDetailedEclipseInfo(
            result: EclipseResult,
            userLatDeg: Double,
            userLonDeg: Double,
            timezoneId: String = "Asia/Tehran"
        ): DetailedEclipseInfo {
            return instance.getDetailedEclipseInfo(result, userLatDeg, userLonDeg, timezoneId)
        }

        fun computeDetailedInfo(
            result: EclipseResult,
            userLatDeg: Double,
            userLonDeg: Double,
            timezoneId: String = "Asia/Tehran"
        ): DetailedEclipseInfo {
            return instance.getDetailedEclipseInfo(result, userLatDeg, userLonDeg, timezoneId)
        }
    }

    enum class EclipseType {
        TOTAL_SOLAR,
        ANNULAR_SOLAR,
        HYBRID_SOLAR,
        PARTIAL_SOLAR,
        TOTAL_LUNAR,
        PARTIAL_LUNAR,
        PENUMBRAL_LUNAR,
        NONE_SOLAR,
        NONE_LUNAR;

        // Aliases for compatibility
        companion object {
            val SOLAR_TOTAL get() = TOTAL_SOLAR
            val SOLAR_ANNULAR get() = ANNULAR_SOLAR
            val SOLAR_PARTIAL get() = PARTIAL_SOLAR
            val LUNAR_TOTAL get() = TOTAL_LUNAR
            val LUNAR_PARTIAL get() = PARTIAL_LUNAR
            val LUNAR_PENUMBRAL get() = PENUMBRAL_LUNAR
        }
    }

    data class EclipseEvent(
        val type: EclipseType,
        val maximumMs: Long,
        val magnitude: Double,
        val saros: Int,
        val gamma: Double,
        val nameEn: String,
        val nameFa: String,
        val durationTotalSeconds: Int,
        val maxTotalityRegionEn: String,
        val maxTotalityRegionFa: String,
        val descriptionEn: String,
        val descriptionFa: String,
        val isSolar: Boolean,
        val penumbralStartMs: Long = maximumMs - 7200000L,
        val umbralStartMs: Long = maximumMs - 3600000L,
        val totalityStartMs: Long = maximumMs - 900000L,
        val totalityEndMs: Long = maximumMs + 900000L,
        val umbralEndMs: Long = maximumMs + 3600000L,
        val penumbralEndMs: Long = maximumMs + 7200000L
    ) {
        val id: String get() = "${if (isSolar) "solar" else "lunar"}_$maximumMs"
        val dateUtcMs: Long get() = maximumMs
        val peakTimeMs: Long get() = maximumMs
        val isTotal: Boolean get() = type == EclipseType.TOTAL_SOLAR || type == EclipseType.TOTAL_LUNAR
    }

    data class EclipseResult(
        val event: EclipseEvent,
        val localNameEn: String,
        val localNameFa: String,
        val isLocallyVisible: Boolean,
        val localObscurationPercent: Int,
        val localMagnitude: Double,
        val localStartTimeMs: Long,
        val localPeakTimeMs: Long,
        val localEndTimeMs: Long,
        val targetAltitudeDeg: Double,
        val targetAzimuthDeg: Double,
        val formattedDateEn: String,
        val formattedDateFa: String,
        val localVisibilityTextEn: String,
        val localVisibilityTextFa: String,
        val daysRemaining: Int
    )

    data class DetailedEclipseInfo(
        val event: EclipseEvent,
        val result: EclipseResult,
        val daysRemaining: Int,
        val localStartTimeStr: String,
        val localPeakTimeStr: String,
        val localEndTimeStr: String,
        val durationTextEn: String,
        val durationTextFa: String,
        val obscurationPercent: Int,
        val targetAltDeg: Double,
        val targetAzDeg: Double,
        val safetyGuideEn: String,
        val safetyGuideFa: String,
        val localMagnitude: Double = result.localMagnitude,
        val maxAltitudeDeg: Double = targetAltDeg,
        val maxAzimuthDeg: Double = targetAzDeg,
        val safetyWarningEn: String = safetyGuideEn,
        val safetyWarningFa: String = safetyGuideFa,
        val observationTipEn: String = "",
        val observationTipFa: String = ""
    )

    private val lunarSolar = LunarSolarEngine()

    // NASA / Fred Espenak authoritative canon of eclipses (2024 to 2030) with exact UTC timestamps
    private val canonicalEclipses = listOf(
        EclipseEvent(
            type = EclipseType.PENUMBRAL_LUNAR,
            maximumMs = 1711350780000L, // 2024-03-25 07:13 UTC
            magnitude = 0.956,
            saros = 113,
            gamma = 1.0609,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "خسوف نیم‌سایه‌ای",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "Americas, Antarctica, Alaska, NE Asia",
            maxTotalityRegionFa = "قاره آمریکا، جنوبگان، آلاسکا و شمال شرق آسیا",
            descriptionEn = "A deep penumbral lunar eclipse where 95.6% of the Moon disk passed through Earth's penumbral shadow.",
            descriptionFa = "خسوف عمیق نیم‌سایه‌ای که طی آن ۹۵.۶٪ قرص ماه وارد نیم‌سایه زمین شد.",
            isSolar = false,
            penumbralStartMs = 1711342380000L, // 04:53 UTC
            umbralStartMs = 1711350780000L,
            totalityStartMs = 1711350780000L,
            totalityEndMs = 1711350780000L,
            umbralEndMs = 1711350780000L,
            penumbralEndMs = 1711359120000L // 09:32 UTC
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1712600220000L, // 2024-04-08 18:17 UTC
            magnitude = 1.0566,
            saros = 139,
            gamma = 0.3431,
            nameEn = "Great North American Total Solar Eclipse",
            nameFa = "خورشیدگرفتگی کامل آمریکای شمالی",
            durationTotalSeconds = 268,
            maxTotalityRegionEn = "Mexico, United States (Texas to Maine), Eastern Canada",
            maxTotalityRegionFa = "مکزیک، ایالات متحده آمریکا (تگزاس تا مین)، شرق کانادا",
            descriptionEn = "A major total solar eclipse with a maximum totality duration of 4 minutes and 28 seconds.",
            descriptionFa = "یکی از باشکوه‌ترین کسوف‌های قرن با مدت گرفت کامل ۴ دقیقه و ۲۸ ثانیه.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_LUNAR,
            maximumMs = 1726627440000L, // 2024-09-18 02:44 UTC
            magnitude = 0.0848,
            saros = 118,
            gamma = -0.9792,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی",
            durationTotalSeconds = 3780,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East, Western Asia",
            maxTotalityRegionFa = "آمریکا، اروپا، آفریقا، خاورمیانه و غرب آسیا",
            descriptionEn = "A shallow partial lunar eclipse with 8.5% of the Moon in Earth's dark umbral shadow.",
            descriptionFa = "خسوف جزئی با ورود ۸.۵٪ از قرص ماه به بخش تاریک سایه زمین.",
            isSolar = false,
            penumbralStartMs = 1726620060000L, // 00:41 UTC
            umbralStartMs = 1726625580000L, // 02:13 UTC
            totalityStartMs = 1726627440000L,
            totalityEndMs = 1726627440000L,
            umbralEndMs = 1726629360000L, // 03:16 UTC
            penumbralEndMs = 1726634820000L // 04:47 UTC
        ),
        EclipseEvent(
            type = EclipseType.ANNULAR_SOLAR,
            maximumMs = 1727894700000L, // 2024-10-02 18:45 UTC
            magnitude = 0.9326,
            saros = 144,
            gamma = -0.3509,
            nameEn = "Annular Solar Eclipse",
            nameFa = "خورشیدگرفتگی حلقوی",
            durationTotalSeconds = 445,
            maxTotalityRegionEn = "Pacific Ocean, Southern Chile, Southern Argentina (Patagonia)",
            maxTotalityRegionFa = "اقیانوس آرام، جنوب شیلی و آرژانتین (پاتاگونیا)",
            descriptionEn = "A dramatic 'Ring of Fire' annular solar eclipse visible across the South Pacific and Patagonia.",
            descriptionFa = "کسوف حلقوی زیبا (حلقه آتش) در اقیانوس آرام و جنوب آمریکای جنوبی.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_LUNAR,
            maximumMs = 1741935540000L, // 2025-03-14 06:59 UTC
            magnitude = 1.1784,
            saros = 123,
            gamma = 0.3485,
            nameEn = "Total Lunar Eclipse (Blood Moon)",
            nameFa = "خسوف کامل (ماه سرخ)",
            durationTotalSeconds = 3900,
            maxTotalityRegionEn = "Americas, Pacific, Atlantic, Western Europe, Western Africa",
            maxTotalityRegionFa = "قاره آمریکا، اقیانوس آرام، غرب اروپا و غرب آفریقا",
            descriptionEn = "A total lunar eclipse lasting 65 minutes of totality turning the Moon copper-red.",
            descriptionFa = "ماه گرفتگی کامل به مدت ۶۵ دقیقه که طی آن قرص ماه به رنگ سرخ مسی درمی‌آید.",
            isSolar = false,
            penumbralStartMs = 1741924620000L, // 03:57 UTC
            umbralStartMs = 1741928940000L, // 05:09 UTC
            totalityStartMs = 1741933560000L, // 06:26 UTC
            totalityEndMs = 1741937460000L, // 07:31 UTC
            umbralEndMs = 1741942080000L, // 08:48 UTC
            penumbralEndMs = 1741946400000L // 10:00 UTC
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_SOLAR,
            maximumMs = 1743245280000L, // 2025-03-29 10:48 UTC
            magnitude = 0.9358,
            saros = 149,
            gamma = 1.0405,
            nameEn = "Partial Solar Eclipse",
            nameFa = "خورشیدگرفتگی جزئی",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "North America (NE), Greenland, Iceland, Northern & Western Europe, NW Russia",
            maxTotalityRegionFa = "شمال شرق آمریکا، گرینلند، ایسلند، شمال و غرب اروپا و روسیه",
            descriptionEn = "A deep partial solar eclipse visible across Greenland, Iceland, the UK, and Northern Europe.",
            descriptionFa = "کسوف جزئی عمیق با پوشش تا ۹۳٪ در گرینلند و شمال اروپا.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_LUNAR,
            maximumMs = 1757268720000L, // 2025-09-07 18:12 UTC
            magnitude = 1.3619,
            saros = 128,
            gamma = -0.2752,
            nameEn = "Total Lunar Eclipse (Blood Moon)",
            nameFa = "خسوف کامل (ماه خونین)",
            durationTotalSeconds = 4920,
            maxTotalityRegionEn = "Europe, Africa, Asia, Australia, Middle East, Iran",
            maxTotalityRegionFa = "اروپا، آفریقا، سراسر آسیا، استرالیا، خاورمیانه و ایران",
            descriptionEn = "A spectacular total lunar eclipse with 82 minutes of totality, wonderfully visible from Europe, Africa, Asia, and Iran.",
            descriptionFa = "خسوف کامل تماشایی با ۸۲ دقیقه گرفت کامل که در سراسر ایران، خاورمیانه، اروپا و آسیا عالی دیده می‌شود.",
            isSolar = false,
            penumbralStartMs = 1757258880000L, // 15:28 UTC
            umbralStartMs = 1757262420000L, // 16:27 UTC
            totalityStartMs = 1757266200000L, // 17:30 UTC
            totalityEndMs = 1757271120000L, // 18:52 UTC
            umbralEndMs = 1757274960000L, // 19:56 UTC
            penumbralEndMs = 1757278500000L // 20:55 UTC
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_SOLAR,
            maximumMs = 1758483720000L, // 2025-09-21 19:42 UTC
            magnitude = 0.8550,
            saros = 154,
            gamma = -1.0651,
            nameEn = "Partial Solar Eclipse",
            nameFa = "خورشیدگرفتگی جزئی",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "South Pacific, New Zealand, Antarctica",
            maxTotalityRegionFa = "اقیانوس آرام جنوبی، نیوزیلند و جنوبگان",
            descriptionEn = "A partial solar eclipse visible in the South Pacific, New Zealand, and Antarctica.",
            descriptionFa = "کسوف جزئی قابل مشاهده در نیوزیلند و اقیانوس منجمد جنوبی.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.ANNULAR_SOLAR,
            maximumMs = 1771330380000L, // 2026-02-17 12:13 UTC
            magnitude = 0.9630,
            saros = 121,
            gamma = -0.9738,
            nameEn = "Annular Solar Eclipse",
            nameFa = "خورشیدگرفتگی حلقوی",
            durationTotalSeconds = 140,
            maxTotalityRegionEn = "Antarctica, Southern Indian Ocean; Partial in S Africa, S South America",
            maxTotalityRegionFa = "جنوبگان، اقیانوس هند جنوبی؛ جزئی در جنوب آفریقا و جنوب آمریکای جنوبی",
            descriptionEn = "An annular solar eclipse across Antarctica and the Southern oceans.",
            descriptionFa = "کسوف حلقوی در قاره قطب جنوب و آب‌های اقیانوس منجمد جنوبی.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_LUNAR,
            maximumMs = 1772537640000L, // 2026-03-03 11:34 UTC
            magnitude = 1.1507,
            saros = 133,
            gamma = -0.3765,
            nameEn = "Total Lunar Eclipse (Blood Moon)",
            nameFa = "خسوف کامل (ماه سرخ)",
            durationTotalSeconds = 3540,
            maxTotalityRegionEn = "Asia, Australia, Pacific, Americas",
            maxTotalityRegionFa = "شرق آسیا، استرالیا، اقیانوس آرام و قاره آمریکا",
            descriptionEn = "Total lunar eclipse with 59 minutes of totality visible across East Asia, Australia, and the Americas.",
            descriptionFa = "خسوف کامل با ۵۹ دقیقه گرفتگی کامل در شرق آسیا، استرالیا و آمریکا.",
            isSolar = false,
            penumbralStartMs = 1772527440000L, // 08:44 UTC
            umbralStartMs = 1772531400000L, // 09:50 UTC
            totalityStartMs = 1772535840000L, // 11:04 UTC
            totalityEndMs = 1772539380000L, // 12:03 UTC
            umbralEndMs = 1772543820000L, // 13:17 UTC
            penumbralEndMs = 1772547780000L // 14:23 UTC
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1786556820000L, // 2026-08-12 17:47 UTC
            magnitude = 1.0386,
            saros = 126,
            gamma = 0.8977,
            nameEn = "Total Solar Eclipse (Great European Eclipse)",
            nameFa = "خورشیدگرفتگی کامل اروپا و اسپانیا",
            durationTotalSeconds = 138,
            maxTotalityRegionEn = "Greenland, Western Iceland, Northern Spain (A Coruña to Palma de Mallorca)",
            maxTotalityRegionFa = "گرینلند، ایسلند، شمال و مرکز اسپانیا؛ جزئی در سراسر اروپا و شمال آفریقا",
            descriptionEn = "A magnificent total solar eclipse sweeping across Greenland, Iceland, and Northern Spain. Partial eclipse visible across Europe and North Africa.",
            descriptionFa = "کسوف کامل در گرینلند، ایسلند و اسپانیا. فاز جزئی در سراسر اروپا و شمال آفریقا قابل مشاهده است.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_LUNAR,
            maximumMs = 1787890380000L, // 2026-08-28 04:13 UTC
            magnitude = 0.9299,
            saros = 138,
            gamma = 0.4964,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی عمیق",
            durationTotalSeconds = 11880,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East, Western Asia",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا، خاورمیانه و غرب آسیا",
            descriptionEn = "A very deep partial lunar eclipse with 93% of the Moon covered by Earth's dark umbra.",
            descriptionFa = "خسوف جزئی بسیار عمیق که در آن ۹۳٪ قرص ماه در سایه تاریک زمین قرار می‌گیرد.",
            isSolar = false,
            penumbralStartMs = 1787880240000L, // 01:24 UTC
            umbralStartMs = 1787884440000L, // 02:34 UTC
            totalityStartMs = 1787890380000L,
            totalityEndMs = 1787890380000L,
            umbralEndMs = 1787896320000L, // 05:52 UTC
            penumbralEndMs = 1787900520000L // 07:02 UTC
        ),
        EclipseEvent(
            type = EclipseType.ANNULAR_SOLAR,
            maximumMs = 1801929600000L, // 2027-02-06 16:00 UTC
            magnitude = 0.9281,
            saros = 131,
            gamma = -0.2952,
            nameEn = "Annular Solar Eclipse",
            nameFa = "خورشیدگرفتگی حلقوی",
            durationTotalSeconds = 471,
            maxTotalityRegionEn = "Chile, Argentina, Atlantic Ocean, Ivory Coast, Ghana, Nigeria",
            maxTotalityRegionFa = "شیلی، آرژانتین، اقیانوس اطلس، ساحل عاج، غنا و نیجریه",
            descriptionEn = "An annular solar eclipse spanning South America and West Africa with a duration of 7m 51s.",
            descriptionFa = "کسوف حلقوی زیبا به مدت ۷ دقیقه و ۵۱ ثانیه در آمریکای جنوبی و غرب آفریقا.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PENUMBRAL_LUNAR,
            maximumMs = 1803165180000L, // 2027-02-20 23:13 UTC
            magnitude = 0.952,
            saros = 143,
            gamma = -1.048,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "خسوف نیم‌سایه‌ای",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East, Asia",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا، خاورمیانه و آسیا",
            descriptionEn = "A deep penumbral eclipse visible across Europe, Africa, Iran, and the Middle East.",
            descriptionFa = "خسوف نیم‌سایه‌ای قابل رویت در سراسر ایران، خاورمیانه، اروپا و آفریقا.",
            isSolar = false,
            penumbralStartMs = 1803157920000L, // 21:12 UTC
            umbralStartMs = 1803165180000L,
            totalityStartMs = 1803165180000L,
            totalityEndMs = 1803165180000L,
            umbralEndMs = 1803165180000L,
            penumbralEndMs = 1803172380000L // 01:13 UTC (+1d)
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1817201220000L, // 2027-08-02 10:07 UTC
            magnitude = 1.0790,
            saros = 136,
            gamma = 0.1421,
            nameEn = "Great Eclipse of the Century (Total Solar Eclipse)",
            nameFa = "کسوف قرن (خورشیدگرفتگی کامل خاورمیانه و شمال آفریقا)",
            durationTotalSeconds = 383,
            maxTotalityRegionEn = "Gibraltar, S Spain, Morocco, Algeria, Tunisia, Libya, Egypt (Luxor 6m22s!), Saudi Arabia (Jeddah, Mecca), Yemen, Somalia; Partial across ALL of Europe, Middle East, Iran & Africa",
            maxTotalityRegionFa = "جبل‌طارق، جنوب اسپانیا، مراکش، الجزایر، تونس، لیبی، مصر (اقصر ۶ دقیقه و ۲۲ ثانیه!)، عربستان (جده و مکه)، یمن، سومالی؛ جزئی در سراسر ایران، خاورمیانه، اروپا و آفریقا",
            descriptionEn = "The greatest solar eclipse of the 21st century on land with over 6 minutes and 22 seconds of totality over Luxor Egypt, Mecca, and North Africa! Deep partial eclipse visible across Iran and the Middle East.",
            descriptionFa = "باشکوه‌ترین و طولانی‌ترین خورشیدگرفتگی قرن بیست و یکم با گرفت کامل بیش از ۶ دقیقه و ۲۲ ثانیه در اقصر مصر و مکه! فاز جزئی بسیار عمیق در سراسر ایران و خاورمیانه قابل مشاهده است.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PENUMBRAL_LUNAR,
            maximumMs = 1818486840000L, // 2027-08-17 07:14 UTC
            magnitude = 0.546,
            saros = 148,
            gamma = 1.2797,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "خسوف نیم‌سایه‌ای",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "Pacific Ocean, Americas, Eastern Asia, Australia",
            maxTotalityRegionFa = "اقیانوس آرام، قاره آمریکا، شرق آسیا و استرالیا",
            descriptionEn = "A minor penumbral eclipse visible across the Pacific rim and Americas.",
            descriptionFa = "خسوف نیم‌سایه‌ای در حاشیه اقیانوس آرام و قاره آمریکا.",
            isSolar = false,
            penumbralStartMs = 1818480240000L, // 05:24 UTC
            umbralStartMs = 1818486840000L,
            totalityStartMs = 1818486840000L,
            totalityEndMs = 1818486840000L,
            umbralEndMs = 1818486840000L,
            penumbralEndMs = 1818493440000L // 09:04 UTC
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_LUNAR,
            maximumMs = 1831263180000L, // 2028-01-12 04:13 UTC
            magnitude = 0.066,
            saros = 115,
            gamma = 0.9817,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی",
            durationTotalSeconds = 3360,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا و غرب خاورمیانه",
            descriptionEn = "A shallow partial lunar eclipse with 6.6% umbral grazing at the Moon's northern limb.",
            descriptionFa = "خسوف جزئی کم‌عمق با ورود ۶.۶٪ از لبه ماه به سایه تاریک زمین.",
            isSolar = false,
            penumbralStartMs = 1831255620000L, // 02:07 UTC
            umbralStartMs = 1831261500000L, // 03:45 UTC
            totalityStartMs = 1831263180000L,
            totalityEndMs = 1831263180000L,
            umbralEndMs = 1831264860000L, // 04:41 UTC
            penumbralEndMs = 1831270740000L // 06:19 UTC
        ),
        EclipseEvent(
            type = EclipseType.ANNULAR_SOLAR,
            maximumMs = 1832512080000L, // 2028-01-26 15:08 UTC
            magnitude = 0.9208,
            saros = 141,
            gamma = 0.3901,
            nameEn = "Annular Solar Eclipse",
            nameFa = "خورشیدگرفتگی حلقوی",
            durationTotalSeconds = 627,
            maxTotalityRegionEn = "Ecuador, Colombia, Brazil, Suriname, Portugal, Spain",
            maxTotalityRegionFa = "اکوادور، کلمبیا، برزیل، پرتغال و اسپانیا",
            descriptionEn = "An extraordinary 10-minute annular eclipse extending from South America to the Iberian Peninsula.",
            descriptionFa = "کسوف حلقوی کم‌نظیر ۱۰ دقیقه‌ای از آمریکای جنوبی تا شبه‌جزیره ایبری.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_LUNAR,
            maximumMs = 1846520400000L, // 2028-07-06 18:20 UTC
            magnitude = 0.3892,
            saros = 120,
            gamma = -0.7936,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی",
            durationTotalSeconds = 8400,
            maxTotalityRegionEn = "Europe, Africa, Asia, Australia, Middle East, Iran",
            maxTotalityRegionFa = "اروپا، آفریقا، آسیا، استرالیا، خاورمیانه و ایران",
            descriptionEn = "A partial lunar eclipse covering 39% of the Moon's diameter, well visible across Iran and the Middle East.",
            descriptionFa = "خسوف جزئی با پوشش ۳۹٪ از قطر ماه که در سراسر ایران و خاورمیانه به خوبی قابل رصد است.",
            isSolar = false,
            penumbralStartMs = 1846511040000L, // 15:44 UTC
            umbralStartMs = 1846516200000L, // 17:10 UTC
            totalityStartMs = 1846520400000L,
            totalityEndMs = 1846520400000L,
            umbralEndMs = 1846524600000L, // 19:30 UTC
            penumbralEndMs = 1846529760000L // 20:56 UTC
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1847847360000L, // 2028-07-22 02:56 UTC
            magnitude = 1.0556,
            saros = 146,
            gamma = -0.6056,
            nameEn = "Total Solar Eclipse (Great Australian Eclipse)",
            nameFa = "خورشیدگرفتگی کامل استرالیا و سیدنی",
            durationTotalSeconds = 310,
            maxTotalityRegionEn = "Australia (directly over Sydney!), New Zealand",
            maxTotalityRegionFa = "استرالیا (مستقیماً از فراز شهر سیدنی!) و نیوزیلند",
            descriptionEn = "A spectacular total solar eclipse passing directly over Sydney Harbour with over 5 minutes of totality in NW Australia.",
            descriptionFa = "کسوف کامل بی‌نظیر که مستقیماً از بالای شهر سیدنی عبور می‌کند و بیش از ۵ دقیقه گرفت کامل دارد.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_LUNAR,
            maximumMs = 1861894320000L, // 2028-12-31 16:52 UTC
            magnitude = 1.2461,
            saros = 125,
            gamma = 0.3258,
            nameEn = "New Year's Eve Total Lunar Eclipse (Blood Moon)",
            nameFa = "خسوف کامل شب سال نو (ماه خونین)",
            durationTotalSeconds = 4320,
            maxTotalityRegionEn = "Europe, Africa, Asia, Australia, Middle East, Iran",
            maxTotalityRegionFa = "اروپا، آفریقا، سراسر آسیا، استرالیا، خاورمیانه و ایران",
            descriptionEn = "A breathtaking total lunar eclipse on New Year's Eve 2028 with 72 minutes of totality, completely visible from Iran and the Middle East.",
            descriptionFa = "خسوف کامل رویایی در شب سال نو ۲۰۲۸ با ۷۲ دقیقه گرفتگی کامل و رصد بی‌نقص در سراسر ایران و خاورمیانه.",
            isSolar = false,
            penumbralStartMs = 1861884240000L, // 14:04 UTC
            umbralStartMs = 1861888020000L, // 15:07 UTC
            totalityStartMs = 1861892160000L, // 16:16 UTC
            totalityEndMs = 1861896480000L, // 17:28 UTC
            umbralEndMs = 1861900620000L, // 18:37 UTC
            penumbralEndMs = 1861904400000L // 19:40 UTC
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_SOLAR,
            maximumMs = 1863105180000L, // 2029-01-14 17:13 UTC
            magnitude = 0.8714,
            saros = 151,
            gamma = 1.0553,
            nameEn = "Partial Solar Eclipse",
            nameFa = "خورشیدگرفتگی جزئی",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "North America, Central America",
            maxTotalityRegionFa = "آمریکای شمالی و آمریکای مرکزی",
            descriptionEn = "A deep partial solar eclipse visible across North and Central America.",
            descriptionFa = "کسوف جزئی عمیق قابل رویت در آمریکای شمالی و مرکزی.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_SOLAR,
            maximumMs = 1875931560000L, // 2029-06-12 04:06 UTC
            magnitude = 0.4576,
            saros = 118,
            gamma = 1.2943,
            nameEn = "Partial Solar Eclipse",
            nameFa = "خورشیدگرفتگی جزئی",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "Arctic, Scandinavia, Alaska, Northern Russia, Northern Canada",
            maxTotalityRegionFa = "شمالگان، اسکاندیناوی، آلاسکا، شمال روسیه و شمال کانادا",
            descriptionEn = "A high-latitude partial solar eclipse across the Arctic and Northern Europe.",
            descriptionFa = "کسوف جزئی در عرض‌های جغرافیایی شمالی، اسکاندیناوی و شمالگان.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_LUNAR,
            maximumMs = 1877138520000L, // 2029-06-26 03:22 UTC
            magnitude = 1.8436,
            saros = 130,
            gamma = 0.0124,
            nameEn = "Central Total Lunar Eclipse (Deep Blood Moon)",
            nameFa = "خسوف کامل مرکزی (ماه خونین بسیار عمیق)",
            durationTotalSeconds = 6120,
            maxTotalityRegionEn = "Americas, Europe, Africa, Western Middle East",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا و غرب خاورمیانه",
            descriptionEn = "One of the longest and deepest total lunar eclipses of the century with 102 minutes of totality as the Moon passes dead-center through Earth's shadow.",
            descriptionFa = "یکی از طولانی‌ترین و عمیق‌ترین خسوف‌های کامل قرن با ۱۰۲ دقیقه گرفت کامل و عبور ماه از مرکز سایه زمین.",
            isSolar = false,
            penumbralStartMs = 1877128560000L, // 00:36 UTC
            umbralStartMs = 1877132040000L, // 01:34 UTC
            totalityStartMs = 1877135520000L, // 02:32 UTC
            totalityEndMs = 1877141640000L, // 04:14 UTC
            umbralEndMs = 1877145120000L, // 05:12 UTC
            penumbralEndMs = 1877148600000L // 06:10 UTC
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_SOLAR,
            maximumMs = 1878478620000L, // 2029-07-11 15:37 UTC
            magnitude = 0.2303,
            saros = 156,
            gamma = -1.4191,
            nameEn = "Partial Solar Eclipse",
            nameFa = "خورشیدگرفتگی جزئی",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "Southern Chile, Southern Argentina (Patagonia), Antarctica",
            maxTotalityRegionFa = "جنوب شیلی، جنوب آرژانتین و جنوبگان",
            descriptionEn = "A partial solar eclipse visible across southern Patagonia and the Antarctic Peninsula.",
            descriptionFa = "کسوف جزئی قابل مشاهده در جنوب شیلی و آرژانتین.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_SOLAR,
            maximumMs = 1891177380000L, // 2029-12-05 15:03 UTC
            magnitude = 0.8911,
            saros = 123,
            gamma = -1.0609,
            nameEn = "Partial Solar Eclipse",
            nameFa = "خورشیدگرفتگی جزئی",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "Antarctica, Southern Argentina, Southern Chile",
            maxTotalityRegionFa = "جنوبگان، جنوب آرژانتین و جنوب شیلی",
            descriptionEn = "A deep partial solar eclipse visible across Antarctica and southernmost South America.",
            descriptionFa = "کسوف جزئی عمیق در قاره قطب جنوب و جنوب آمریکای جنوبی.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_LUNAR,
            maximumMs = 1892500920000L, // 2029-12-20 22:42 UTC
            magnitude = 1.1174,
            saros = 135,
            gamma = -0.3811,
            nameEn = "Winter Solstice Total Lunar Eclipse (Blood Moon)",
            nameFa = "خسوف کامل شب یلدا (ماه خونین)",
            durationTotalSeconds = 3240,
            maxTotalityRegionEn = "Europe, Africa, Asia, Middle East, Iran, Americas",
            maxTotalityRegionFa = "سراسر ایران، خاورمیانه، اروپا، آفریقا، آسیا و آمریکا",
            descriptionEn = "A remarkable Yalda / Winter Solstice total lunar eclipse with 54 minutes of totality high in the midnight sky across Iran, the Middle East, and Europe.",
            descriptionFa = "خسوف کامل تماشایی در شب یلدا با ۵۴ دقیقه گرفت کامل در اوج آسمان نیمه‌شب ایران، خاورمیانه و اروپا.",
            isSolar = false,
            penumbralStartMs = 1892490240000L, // 19:44 UTC
            umbralStartMs = 1892494560000L, // 20:56 UTC
            totalityStartMs = 1892499300000L, // 22:15 UTC
            totalityEndMs = 1892502540000L, // 23:09 UTC
            umbralEndMs = 1892507280000L, // 00:28 UTC (+1d)
            penumbralEndMs = 1892511600000L // 01:40 UTC (+1d)
        ),
        EclipseEvent(
            type = EclipseType.ANNULAR_SOLAR,
            maximumMs = 1906525680000L, // 2030-06-01 06:28 UTC
            magnitude = 0.9443,
            saros = 128,
            gamma = 0.5626,
            nameEn = "Eurasian Annular Solar Eclipse",
            nameFa = "خورشیدگرفتگی حلقوی اوراسیا و خاورمیانه",
            durationTotalSeconds = 321,
            maxTotalityRegionEn = "Algeria, Tunisia, Libya, Greece, Turkey, Russia, Kazakhstan, China, Japan; Deep partial across Iran & Europe",
            maxTotalityRegionFa = "الجزایر، تونس، لیبی، یونان، ترکیه، روسیه، قزاقستان، چین و ژاپن؛ جزئی عمیق در سراسر ایران و اروپا",
            descriptionEn = "A major annular solar eclipse crossing the Mediterranean, Turkey, and Eurasia, producing a deep morning partial eclipse across Iran.",
            descriptionFa = "کسوف حلقوی بزرگ در مدیترانه، ترکیه و اوراسیا که در سراسر ایران به صورت خورشیدگرفتگی جزئی عمیق صبحگاهی دیده می‌شود.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_LUNAR,
            maximumMs = 1907778780000L, // 2030-06-15 18:33 UTC
            magnitude = 0.5025,
            saros = 140,
            gamma = 0.7534,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی",
            durationTotalSeconds = 8640,
            maxTotalityRegionEn = "Europe, Africa, Asia, Australia, Middle East, Iran",
            maxTotalityRegionFa = "ایران، خاورمیانه، اروپا، آفریقا، آسیا و استرالیا",
            descriptionEn = "A partial lunar eclipse covering 50% of the Moon's diameter, completely visible throughout Iran and the Middle East.",
            descriptionFa = "خسوف جزئی با پوشش ۵۰٪ از قطر ماه، کاملاً قابل رصد در سراسر ایران و خاورمیانه.",
            isSolar = false,
            penumbralStartMs = 1907770500000L, // 16:15 UTC
            umbralStartMs = 1907774460000L, // 17:21 UTC
            totalityStartMs = 1907778780000L,
            totalityEndMs = 1907778780000L,
            umbralEndMs = 1907783100000L, // 19:45 UTC
            penumbralEndMs = 1907787060000L // 20:51 UTC
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1921819860000L, // 2030-11-25 06:51 UTC
            magnitude = 1.0468,
            saros = 133,
            gamma = -0.3867,
            nameEn = "Total Solar Eclipse",
            nameFa = "خورشیدگرفتگی کامل آفریقا و استرالیا",
            durationTotalSeconds = 224,
            maxTotalityRegionEn = "Namibia, Botswana, South Africa, Indian Ocean, Australia",
            maxTotalityRegionFa = "نامیبیا، بوتسوانا، آفریقای جنوبی، اقیانوس هند و استرالیا",
            descriptionEn = "A total solar eclipse spanning Southern Africa, the Indian Ocean, and Australia.",
            descriptionFa = "کسوف کامل از جنوب آفریقا و اقیانوس هند تا استرالیا.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PENUMBRAL_LUNAR,
            maximumMs = 1923085680000L, // 2030-12-09 22:28 UTC
            magnitude = 0.9347,
            saros = 145,
            gamma = -1.0770,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "خسوف نیم‌سایه‌ای",
            durationTotalSeconds = 0,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East, Iran, Asia",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا، ایران، خاورمیانه و آسیا",
            descriptionEn = "A deep penumbral lunar eclipse visible across Iran, Europe, Africa, and the Americas.",
            descriptionFa = "خسوف نیم‌سایه‌ای عمیق قابل رویت در سراسر ایران، اروپا، آفریقا و قاره آمریکا.",
            isSolar = false,
            penumbralStartMs = 1923077340000L, // 20:09 UTC
            umbralStartMs = 1923085680000L,
            totalityStartMs = 1923085680000L,
            totalityEndMs = 1923085680000L,
            umbralEndMs = 1923085680000L,
            penumbralEndMs = 1923093960000L // 00:46 UTC (+1d)
        )
    )

    private val canonicalCoverageStartMs: Long = 1704067200000L // 2024-01-01 00:00 UTC

    /**
     * Converts JDE (Julian Ephemeris Day in Terrestrial Time TT) to UTC epoch milliseconds.
     */
    private fun jdeToUtcMs(jde: Double): Long {
        val deltaTSec = AstroTime.fromJd(jde).deltaT
        val jdUtc = jde - deltaTSec / 86400.0
        return ((jdUtc - 2440587.5) * 86400000.0).roundToLong()
    }

    /**
     * Calculates time of New Moon for lunation k using Meeus Chapter 49.
     */
    fun newMoonTime(k: Long): Long {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        var jde = 2451550.09766 + 29.530588861 * k + 0.00015437 * t2 - 0.000000150 * t3 + 0.00000000073 * t4

        val E = 1.0 - 0.002516 * t - 0.0000074 * t2
        val M = (2.5534 + 29.10535670 * k - 0.0000014 * t2 - 0.00000011 * t3) * DEG2RAD
        val Mp = (201.5643 + 385.81693528 * k + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4) * DEG2RAD
        val F = (160.7108 + 390.67050284 * k - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4) * DEG2RAD
        val omega = (124.7746 - 1.56375588 * k + 0.0020672 * t2 + 0.00000215 * t3) * DEG2RAD

        jde += -0.40720 * sin(Mp) +
                0.17241 * E * sin(M) +
                0.01608 * sin(2.0 * Mp) +
                0.01039 * sin(2.0 * F) +
                0.00739 * E * sin(Mp - M) -
                0.00514 * E * sin(Mp + M) +
                0.00208 * E * E * sin(2.0 * M) -
                0.00111 * sin(Mp - 2.0 * F) -
                0.00057 * sin(Mp + 2.0 * F) +
                0.00056 * E * sin(2.0 * Mp + M) -
                0.00042 * sin(3.0 * Mp) +
                0.00042 * E * sin(M + 2.0 * F) +
                0.00038 * E * sin(M - 2.0 * F) -
                0.00024 * E * sin(2.0 * Mp - M) -
                0.00017 * sin(omega)

        return jdeToUtcMs(jde)
    }

    /**
     * Calculates time of Full Moon for lunation k using Meeus Chapter 49.
     */
    fun fullMoonTime(k: Long): Long {
        val kFull = k + 0.5
        val t = kFull / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        var jde = 2451550.09766 + 29.530588861 * kFull + 0.00015437 * t2 - 0.000000150 * t3 + 0.00000000073 * t4

        val E = 1.0 - 0.002516 * t - 0.0000074 * t2
        val M = (2.5534 + 29.10535670 * kFull - 0.0000014 * t2 - 0.00000011 * t3) * DEG2RAD
        val Mp = (201.5643 + 385.81693528 * kFull + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4) * DEG2RAD
        val F = (160.7108 + 390.67050284 * kFull - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4) * DEG2RAD
        val omega = (124.7746 - 1.56375588 * kFull + 0.0020672 * t2 + 0.00000215 * t3) * DEG2RAD

        jde += -0.40614 * sin(Mp) +
                0.17302 * E * sin(M) +
                0.01614 * sin(2.0 * Mp) +
                0.01043 * sin(2.0 * F) +
                0.00734 * E * sin(Mp - M) -
                0.00515 * E * sin(Mp + M) +
                0.00209 * E * E * sin(2.0 * M) -
                0.00111 * sin(Mp - 2.0 * F) -
                0.00057 * sin(Mp + 2.0 * F) +
                0.00056 * E * sin(2.0 * Mp + M) -
                0.00042 * sin(3.0 * Mp) +
                0.00042 * E * sin(M + 2.0 * F) +
                0.00038 * E * sin(M - 2.0 * F) -
                0.00024 * E * sin(2.0 * Mp - M) -
                0.00017 * sin(omega)

        return jdeToUtcMs(jde)
    }

    /**
     * Calculates time of First (0.25) or Last (0.75) Quarter Moon for lunation k.
     */
    fun quarterMoonTime(k: Long, phaseOffset: Double): Long {
        val kPhase = k + phaseOffset
        val t = kPhase / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        var jde = 2451550.09766 + 29.530588861 * kPhase + 0.00015437 * t2 - 0.000000150 * t3 + 0.00000000073 * t4

        val E = 1.0 - 0.002516 * t - 0.0000074 * t2
        val M = (2.5534 + 29.10535670 * kPhase - 0.0000014 * t2 - 0.00000011 * t3) * DEG2RAD
        val Mp = (201.5643 + 385.81693528 * kPhase + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4) * DEG2RAD
        val F = (160.7108 + 390.67050284 * kPhase - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4) * DEG2RAD

        val isFirstQuarter = (phaseOffset < 0.5)
        val sign = if (isFirstQuarter) 1.0 else -1.0

        jde += -0.62801 * sin(Mp) +
                0.17172 * E * sin(M) -
                0.01183 * E * sin(Mp + M) +
                0.00862 * sin(2.0 * Mp) +
                0.00804 * sin(2.0 * F) +
                0.00454 * E * sin(Mp - M) +
                0.00204 * E * E * sin(2.0 * M) -
                0.00180 * sin(Mp - 2.0 * F) -
                0.00070 * sin(Mp + 2.0 * F) -
                0.00040 * sin(3.0 * Mp) +
                sign * (0.00306 - 0.00038 * E * cos(M) + 0.00026 * cos(Mp))

        return jdeToUtcMs(jde)
    }

    fun findNextNewMoon(afterMs: Long): Long {
        val baseJd = afterMs / 86400000.0 + 2440587.5
        var k = floor((baseJd - 2451550.09766) / SYNODIC_MONTH_DAYS).toLong() - 1L
        while (true) {
            val t = newMoonTime(k)
            if (t > afterMs) return t
            k++
        }
    }

    fun findNextFullMoon(afterMs: Long): Long {
        val baseJd = afterMs / 86400000.0 + 2440587.5
        var k = floor((baseJd - 2451550.09766) / SYNODIC_MONTH_DAYS).toLong() - 1L
        while (true) {
            val t = fullMoonTime(k)
            if (t > afterMs) return t
            k++
        }
    }

    fun findNextSolarEclipse(afterMs: Long): EclipseEvent? {
        if (afterMs >= canonicalCoverageStartMs) {
            val canonical = canonicalEclipses.firstOrNull { it.isSolar && it.maximumMs > afterMs }
            if (canonical != null) return canonical
        }
        val dynamic = searchDynamicEclipse(afterMs, isSolar = true)
        if (dynamic != null && afterMs < canonicalCoverageStartMs) {
            val firstCanonical = canonicalEclipses.firstOrNull { it.isSolar && it.maximumMs > afterMs }
            if (firstCanonical != null && firstCanonical.maximumMs <= dynamic.maximumMs + 5 * 86400000L) {
                return firstCanonical
            }
        }
        return dynamic
    }

    fun findNextLunarEclipse(afterMs: Long): EclipseEvent? {
        if (afterMs >= canonicalCoverageStartMs) {
            val canonical = canonicalEclipses.firstOrNull { !it.isSolar && it.maximumMs > afterMs }
            if (canonical != null) return canonical
        }
        val dynamic = searchDynamicEclipse(afterMs, isSolar = false)
        if (dynamic != null && afterMs < canonicalCoverageStartMs) {
            val firstCanonical = canonicalEclipses.firstOrNull { !it.isSolar && it.maximumMs > afterMs }
            if (firstCanonical != null && firstCanonical.maximumMs <= dynamic.maximumMs + 5 * 86400000L) {
                return firstCanonical
            }
        }
        return dynamic
    }

    fun findEclipses(startMs: Long, endMs: Long): List<EclipseEvent> {
        if (endMs < startMs) return emptyList()
        val result = mutableListOf<EclipseEvent>()
        var curMs = startMs - 1L
        while (curMs < endMs) {
            val s = findNextSolarEclipse(curMs)
            val l = findNextLunarEclipse(curMs)
            val next = listOfNotNull(s, l)
                .filter { it.maximumMs in startMs..endMs }
                .minByOrNull { it.maximumMs }
                ?: break
            result.add(next)
            curMs = next.maximumMs + 5 * 86400000L
        }
        return result
    }

    /**
     * Evaluates whether an eclipse is visible from the user's specific location and
     * computes the exact local circumstances: start, max obscuration %, altitude, azimuth, end time.
     */
    fun evaluateEclipse(
        event: EclipseEvent,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double = 0.0,
        nowMs: Long = System.currentTimeMillis(),
        timezoneId: String = "Asia/Tehran",
        calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI
    ): EclipseResult {
        return if (event.isSolar) {
            evaluateSolarEclipseLocal(
                event = event,
                userLatDeg = userLatDeg,
                userLonDeg = userLonDeg,
                elevationM = elevationM,
                nowMs = nowMs,
                timezoneId = timezoneId,
                calendarSystem = calendarSystem
            )
        } else {
            evaluateLunarEclipseLocal(
                event = event,
                userLatDeg = userLatDeg,
                userLonDeg = userLonDeg,
                elevationM = elevationM,
                nowMs = nowMs,
                timezoneId = timezoneId,
                calendarSystem = calendarSystem
            )
        }
    }

    private data class SolarStepEval(
        val sunAltDeg: Double,
        val sunAzDeg: Double,
        val sunRadiusDeg: Double,
        val moonRadiusDeg: Double,
        val sepDeg: Double
    )

    private fun evalSolarAt(
        timeMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double
    ): SolarStepEval {
        val jd = timeMs / 86400000.0 + 2440587.5
        val astroTime = AstroTime.fromJd(jd)
        val lastDeg = TimeEngine.getLAST(jd, userLonDeg)

        val sunGeo = lunarSolar.calculateSun(astroTime)
        val moonGeo = lunarSolar.calculateMoon(astroTime)

        val sunDistKm = sunGeo.distanceAu * AU_KM
        val sunTopoEq = CoordinateEngine.geocentricToTopocentric(
            geocentric = CoordinateEngine.Equatorial(sunGeo.raDeg, sunGeo.decDeg),
            geocentricDistanceKm = sunDistKm,
            lastDeg = lastDeg,
            latitudeDeg = userLatDeg,
            elevationM = elevationM
        )
        val moonTopoEq = CoordinateEngine.geocentricToTopocentric(
            geocentric = CoordinateEngine.Equatorial(moonGeo.raDeg, moonGeo.decDeg),
            geocentricDistanceKm = moonGeo.distanceKm,
            lastDeg = lastDeg,
            latitudeDeg = userLatDeg,
            elevationM = elevationM
        )

        val sunHoriz = CoordinateEngine.equatorialToHorizontal(
            equatorial = sunTopoEq,
            lastDeg = lastDeg,
            latitudeDeg = userLatDeg,
            observerElevationM = elevationM
        )

        // Topocentric lunar distance (augmentation of the Moon's apparent semi-diameter)
        val latRad = userLatDeg * DEG2RAD
        val u = atan((1.0 - EARTH_FLATTENING) * tan(latRad))
        val rhoSinPhiP = (1.0 - EARTH_FLATTENING) * sin(u) + (elevationM / 6378137.0) * sin(latRad)
        val rhoCosPhiP = cos(u) + (elevationM / 6378137.0) * cos(latRad)
        val sinPi = EARTH_EQUATORIAL_RADIUS_KM / moonGeo.distanceKm
        val hRad = (lastDeg - moonGeo.raDeg) * DEG2RAD
        val decRad = moonGeo.decDeg * DEG2RAD
        val dx = cos(decRad) * cos(hRad) - rhoCosPhiP * sinPi
        val dy = cos(decRad) * sin(hRad)
        val dz = sin(decRad) - rhoSinPhiP * sinPi
        val moonTopoDistKm = moonGeo.distanceKm * sqrt(dx * dx + dy * dy + dz * dz)

        val sunRadiusDeg = asin((SUN_RADIUS_KM / sunDistKm).coerceIn(-1.0, 1.0)) * RAD2DEG
        val moonRadiusDeg = asin((MOON_RADIUS_KM / moonTopoDistKm).coerceIn(-1.0, 1.0)) * RAD2DEG

        val sepDeg = calculateAngularSeparation(
            sunTopoEq.raDeg, sunTopoEq.decDeg,
            moonTopoEq.raDeg, moonTopoEq.decDeg
        )

        return SolarStepEval(
            sunAltDeg = sunHoriz.altitudeDeg,
            sunAzDeg = sunHoriz.azimuthDeg,
            sunRadiusDeg = sunRadiusDeg,
            moonRadiusDeg = moonRadiusDeg,
            sepDeg = sepDeg
        )
    }

    /**
     * High-precision local circumstances for Solar Eclipse at user location.
     */
    private fun evaluateSolarEclipseLocal(
        event: EclipseEvent,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double,
        nowMs: Long,
        timezoneId: String,
        calendarSystem: CalendarSystem
    ): EclipseResult {
        val maxMs = event.maximumMs
        val stepMs = 60 * 1000L // 1-minute coarse step
        val scanRadiusMs = (3.5 * 3600 * 1000).toLong()

        var firstContactMs = 0L
        var lastContactMs = 0L
        var peakTimeMs = maxMs
        var maxObscuration = 0.0
        var maxMagnitude = 0.0
        var minVisibleSepDeg = Double.MAX_VALUE
        var peakSunRadiusDeg = 0.266
        var peakMoonRadiusDeg = 0.272
        var peakSunAlt = 0.0
        var peakSunAz = 0.0

        var curTime = maxMs - scanRadiusMs
        while (curTime <= maxMs + scanRadiusMs) {
            val step = evalSolarAt(curTime, userLatDeg, userLonDeg, elevationM)
            if (step.sepDeg < step.sunRadiusDeg + step.moonRadiusDeg && step.sunAltDeg > -0.5) {
                val obscuration = calculateDiskOverlapFraction(
                    step.sunRadiusDeg,
                    step.moonRadiusDeg,
                    step.sepDeg
                )
                val mag = computeLocalSolarMagnitude(step.sunRadiusDeg, step.moonRadiusDeg, step.sepDeg)

                if (firstContactMs == 0L) firstContactMs = curTime
                lastContactMs = curTime

                if (obscuration > maxObscuration || (obscuration == maxObscuration && step.sepDeg < minVisibleSepDeg)) {
                    maxObscuration = obscuration
                    maxMagnitude = mag
                    minVisibleSepDeg = step.sepDeg
                    peakTimeMs = curTime
                    peakSunRadiusDeg = step.sunRadiusDeg
                    peakMoonRadiusDeg = step.moonRadiusDeg
                    peakSunAlt = step.sunAltDeg
                    peakSunAz = step.sunAzDeg
                }
            }
            curTime += stepMs
        }

        // Refine peak with 10-second sub-steps around coarse peakTimeMs
        if (firstContactMs != 0L) {
            var refineTime = peakTimeMs - stepMs
            val refineEnd = peakTimeMs + stepMs
            while (refineTime <= refineEnd) {
                val step = evalSolarAt(refineTime, userLatDeg, userLonDeg, elevationM)
                if (step.sepDeg < step.sunRadiusDeg + step.moonRadiusDeg && step.sunAltDeg > -0.5) {
                    val obscuration = calculateDiskOverlapFraction(
                        step.sunRadiusDeg,
                        step.moonRadiusDeg,
                        step.sepDeg
                    )
                    val mag = computeLocalSolarMagnitude(step.sunRadiusDeg, step.moonRadiusDeg, step.sepDeg)
                    if (step.sepDeg < minVisibleSepDeg) {
                        minVisibleSepDeg = step.sepDeg
                        maxObscuration = obscuration
                        maxMagnitude = mag
                        peakTimeMs = refineTime
                        peakSunRadiusDeg = step.sunRadiusDeg
                        peakMoonRadiusDeg = step.moonRadiusDeg
                        peakSunAlt = step.sunAltDeg
                        peakSunAz = step.sunAzDeg
                    }
                }
                refineTime += 10_000L
            }
        }

        val isVisible = maxObscuration >= 0.001 && firstContactMs != 0L
        val isLocalTotal = isVisible &&
                peakMoonRadiusDeg >= peakSunRadiusDeg &&
                minVisibleSepDeg <= (peakMoonRadiusDeg - peakSunRadiusDeg) + 0.002
        val isLocalAnnular = isVisible &&
                !isLocalTotal &&
                peakSunRadiusDeg > peakMoonRadiusDeg &&
                minVisibleSepDeg <= (peakSunRadiusDeg - peakMoonRadiusDeg) + 0.002

        val obscurationPercent = when {
            !isVisible -> 0
            isLocalTotal -> 100
            else -> (maxObscuration * 100.0).roundToInt().coerceIn(1, 99)
        }

        val maxEval = evalSolarAt(maxMs, userLatDeg, userLonDeg, elevationM)
        if (!isVisible) {
            peakSunAlt = maxEval.sunAltDeg
            peakSunAz = maxEval.sunAzDeg
        }

        val localTypeStrEn: String
        val localTypeStrFa: String
        if (isVisible) {
            when {
                isLocalTotal -> {
                    localTypeStrEn = "Total Solar Eclipse"
                    localTypeStrFa = "خورشیدگرفتگی کامل"
                }
                isLocalAnnular -> {
                    localTypeStrEn = "Annular Solar Eclipse (Ring of Fire)"
                    localTypeStrFa = "خورشیدگرفتگی حلقوی (حلقه آتش)"
                }
                else -> {
                    localTypeStrEn = "Partial Solar Eclipse ($obscurationPercent% coverage)"
                    localTypeStrFa = "خورشیدگرفتگی جزئی (${obscurationPercent.toString().toPersianDigits()}٪ پوشش)"
                }
            }
        } else {
            localTypeStrEn = "Not Visible Locally (${event.nameEn})"
            localTypeStrFa = "عدم رویت در موقعیت شما (${event.nameFa})"
        }

        val timeZone = TimeEngine.resolveTimeZone(timezoneId)
        val (formattedEn, formattedFa) = formatDates(
            ms = if (isVisible) peakTimeMs else event.maximumMs,
            timeZone = timeZone,
            calendarSystem = calendarSystem
        )

        val visibilityTextEn: String
        val visibilityTextFa: String
        if (isVisible) {
            val altStr = "${peakSunAlt.roundToInt()}°"
            visibilityTextEn = "Visible as $localTypeStrEn (Sun Altitude: $altStr at peak, $obscurationPercent% coverage)."
            visibilityTextFa = "قابل رویت به صورت $localTypeStrFa (ارتفاع خورشید در اوج: ${altStr.toPersianDigits()}، پوشش ${"$obscurationPercent%".toPersianDigits()})."
        } else {
            if (maxEval.sunAltDeg <= -0.5) {
                visibilityTextEn = "Not visible directly: Occurs during nighttime at your location (Sun is below the horizon)."
                visibilityTextFa = "عدم رویت مستقیم: این گرفتگی در طول شب رخ می‌دهد (خورشید در موقعیت شما زیر افق قرار دارد)."
            } else {
                visibilityTextEn = "Not visible directly: Your location is outside the Moon's shadow path (0% coverage)."
                visibilityTextFa = "عدم رویت مستقیم: موقعیت شما خارج از مسیر سایه و نیم‌سایه ماه قرار دارد (پوشش ۰٪)."
            }
        }

        val refPeakMs = if (isVisible) peakTimeMs else event.maximumMs
        val daysRemaining = max(0, ((refPeakMs - nowMs) / 86400000.0).roundToInt())

        return EclipseResult(
            event = event,
            localNameEn = localTypeStrEn,
            localNameFa = localTypeStrFa,
            isLocallyVisible = isVisible,
            localObscurationPercent = obscurationPercent,
            localMagnitude = if (isVisible) maxMagnitude else 0.0,
            localStartTimeMs = if (isVisible) firstContactMs else maxMs,
            localPeakTimeMs = if (isVisible) peakTimeMs else maxMs,
            localEndTimeMs = if (isVisible) lastContactMs else maxMs,
            targetAltitudeDeg = peakSunAlt,
            targetAzimuthDeg = peakSunAz,
            formattedDateEn = formattedEn,
            formattedDateFa = formattedFa,
            localVisibilityTextEn = visibilityTextEn,
            localVisibilityTextFa = visibilityTextFa,
            daysRemaining = daysRemaining
        )
    }

    private fun computeLocalSolarMagnitude(
        sunRadiusDeg: Double,
        moonRadiusDeg: Double,
        sepDeg: Double
    ): Double {
        if (sepDeg >= sunRadiusDeg + moonRadiusDeg) return 0.0
        if (sepDeg <= abs(sunRadiusDeg - moonRadiusDeg)) {
            return moonRadiusDeg / sunRadiusDeg
        }
        return (sunRadiusDeg + moonRadiusDeg - sepDeg) / (2.0 * sunRadiusDeg)
    }

    /**
     * High-precision local circumstances for Lunar Eclipse at user location.
     */
    private fun evaluateLunarEclipseLocal(
        event: EclipseEvent,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double,
        nowMs: Long,
        timezoneId: String,
        calendarSystem: CalendarSystem
    ): EclipseResult {
        val maxMs = event.maximumMs
        val p1Ms = event.penumbralStartMs
        val p4Ms = event.penumbralEndMs

        // Step through [P1, P4] in 2-minute increments to find the exact local visibility window
        val stepMs = 2 * 60 * 1000L
        var localStartMs = 0L
        var localEndMs = 0L
        var curTime = p1Ms
        while (curTime <= p4Ms) {
            val horiz = getMoonHorizAt(curTime, userLatDeg, userLonDeg, elevationM)
            if (horiz.altitudeDeg > -0.5) {
                if (localStartMs == 0L) localStartMs = curTime
                localEndMs = curTime
            }
            curTime += stepMs
        }

        val maxHoriz = getMoonHorizAt(maxMs, userLatDeg, userLonDeg, elevationM)
        val p1Alt = getMoonAltAt(p1Ms, userLatDeg, userLonDeg, elevationM)
        val p4Alt = getMoonAltAt(p4Ms, userLatDeg, userLonDeg, elevationM)

        val isVisibleAtPeak = maxHoriz.altitudeDeg > -0.5
        val isVisibleAtAnyTime = isVisibleAtPeak || localStartMs != 0L || p1Alt > -0.5 || p4Alt > -0.5

        if (isVisibleAtAnyTime && localStartMs == 0L) {
            localStartMs = if (p1Alt > -0.5) p1Ms else maxMs
            localEndMs = if (p4Alt > -0.5) p4Ms else maxMs
        }

        val localPeakMs = if (isVisibleAtAnyTime) {
            maxMs.coerceIn(localStartMs, localEndMs)
        } else {
            maxMs
        }
        val localPeakHoriz = if (localPeakMs == maxMs) {
            maxHoriz
        } else {
            getMoonHorizAt(localPeakMs, userLatDeg, userLonDeg, elevationM)
        }

        // Compute local magnitude & umbral obscuration at localPeakMs
        val (localMag, obscurationPercent) = if (!isVisibleAtAnyTime) {
            Pair(0.0, 0)
        } else if (isVisibleAtPeak || (event.type == EclipseType.TOTAL_LUNAR && localPeakMs in event.totalityStartMs..event.totalityEndMs)) {
            val obs = when (event.type) {
                EclipseType.TOTAL_LUNAR -> 100
                EclipseType.PARTIAL_LUNAR -> (event.magnitude * 100.0).roundToInt().coerceIn(1, 99)
                EclipseType.PENUMBRAL_LUNAR -> 0
                else -> 0
            }
            Pair(event.magnitude, obs)
        } else {
            // Visible only before moonset or after moonrise (localPeakMs != maxMs)
            val halfPenSpan = max(1L, (p4Ms - p1Ms) / 2L).toDouble()
            val dtFromMax = abs(localPeakMs - maxMs).toDouble()
            val progress = (1.0 - (dtFromMax / halfPenSpan)).coerceIn(0.05, 1.0)
            val scaledMag = event.magnitude * progress
            val inUmbra = (event.type == EclipseType.TOTAL_LUNAR || event.type == EclipseType.PARTIAL_LUNAR) &&
                    localPeakMs in event.umbralStartMs..event.umbralEndMs &&
                    event.umbralEndMs > event.umbralStartMs
            val obs = if (inUmbra) {
                val halfUmbSpan = max(1L, (event.umbralEndMs - event.umbralStartMs) / 2L).toDouble()
                val umbFrac = (1.0 - (dtFromMax / halfUmbSpan)).coerceIn(0.01, 0.99)
                val peakUmbMag = min(1.0, event.magnitude)
                (peakUmbMag * umbFrac * 100.0).roundToInt().coerceIn(1, 99)
            } else {
                0
            }
            Pair(scaledMag, obs)
        }

        val timeZone = TimeEngine.resolveTimeZone(timezoneId)
        val (formattedEn, formattedFa) = formatDates(
            ms = if (isVisibleAtAnyTime) localPeakMs else event.maximumMs,
            timeZone = timeZone,
            calendarSystem = calendarSystem
        )

        val visibilityTextEn: String
        val visibilityTextFa: String
        if (isVisibleAtAnyTime) {
            val altStr = "${localPeakHoriz.altitudeDeg.roundToInt().coerceAtLeast(0)}°"
            if (p1Alt > -0.5 && p4Alt > -0.5 && isVisibleAtPeak) {
                visibilityTextEn = "Fully visible from start to finish! Moon is above the horizon throughout (Altitude: $altStr at peak)."
                visibilityTextFa = "کاملاً قابل رصد در تمام مراحل! ماه در تمام مدت بالای افق قرار دارد (ارتفاع در اوج: ${altStr.toPersianDigits()})."
            } else if (p1Alt <= -0.5) {
                if (isVisibleAtPeak) {
                    visibilityTextEn = "Visible after Moonrise including maximum eclipse (Moon altitude is $altStr at peak)."
                    visibilityTextFa = "قابل رصد پس از طلوع ماه به همراه اوج گرفتگی (ارتفاع ماه در زمان اوج: ${altStr.toPersianDigits()})."
                } else {
                    visibilityTextEn = "Final stages visible at Moonrise (maximum eclipse occurs before Moonrise)."
                    visibilityTextFa = "مراحل پایانی گرفتگی هنگام طلوع ماه قابل رصد است (اوج گرفتگی پیش از طلوع ماه رخ می‌دهد)."
                }
            } else {
                if (isVisibleAtPeak) {
                    visibilityTextEn = "Visible until Moonset including maximum eclipse (Moon altitude is $altStr at peak)."
                    visibilityTextFa = "قابل رصد تا زمان غروب ماه به همراه اوج گرفتگی (ارتفاع ماه در زمان اوج: ${altStr.toPersianDigits()})."
                } else {
                    visibilityTextEn = "Initial stages visible before Moonset (Moon sets before maximum eclipse)."
                    visibilityTextFa = "مراحل آغازین گرفتگی پیش از غروب ماه قابل رصد است (ماه پیش از اوج گرفتگی غروب می‌کند)."
                }
            }
        } else {
            visibilityTextEn = "Not visible directly: Occurs during daytime (Moon is below the horizon at your location)."
            visibilityTextFa = "عدم رویت مستقیم: این خسوف در طول روز رخ می‌دهد (ماه در موقعیت شما زیر افق قرار دارد)."
        }

        val refPeakMs = if (isVisibleAtAnyTime) localPeakMs else event.maximumMs
        val daysRemaining = max(0, ((refPeakMs - nowMs) / 86400000.0).roundToInt())

        return EclipseResult(
            event = event,
            localNameEn = if (isVisibleAtAnyTime) event.nameEn else "Not Visible Locally (${event.nameEn})",
            localNameFa = if (isVisibleAtAnyTime) event.nameFa else "عدم رویت در موقعیت شما (${event.nameFa})",
            isLocallyVisible = isVisibleAtAnyTime,
            localObscurationPercent = obscurationPercent,
            localMagnitude = localMag,
            localStartTimeMs = if (isVisibleAtAnyTime) localStartMs else maxMs,
            localPeakTimeMs = localPeakMs,
            localEndTimeMs = if (isVisibleAtAnyTime) localEndMs else maxMs,
            targetAltitudeDeg = if (isVisibleAtAnyTime) localPeakHoriz.altitudeDeg else maxHoriz.altitudeDeg,
            targetAzimuthDeg = if (isVisibleAtAnyTime) localPeakHoriz.azimuthDeg else maxHoriz.azimuthDeg,
            formattedDateEn = formattedEn,
            formattedDateFa = formattedFa,
            localVisibilityTextEn = visibilityTextEn,
            localVisibilityTextFa = visibilityTextFa,
            daysRemaining = daysRemaining
        )
    }

    private fun getMoonHorizAt(
        timeMs: Long,
        latDeg: Double,
        lonDeg: Double,
        elevationM: Double
    ): CoordinateEngine.Horizontal {
        val jd = timeMs / 86400000.0 + 2440587.5
        val astroTime = AstroTime.fromJd(jd)
        val lastDeg = TimeEngine.getLAST(jd, lonDeg)
        val moonGeo = lunarSolar.calculateMoon(astroTime)
        val moonTopo = CoordinateEngine.geocentricToTopocentric(
            geocentric = CoordinateEngine.Equatorial(moonGeo.raDeg, moonGeo.decDeg),
            geocentricDistanceKm = moonGeo.distanceKm,
            lastDeg = lastDeg,
            latitudeDeg = latDeg,
            elevationM = elevationM
        )
        return CoordinateEngine.equatorialToHorizontal(
            equatorial = moonTopo,
            lastDeg = lastDeg,
            latitudeDeg = latDeg,
            observerElevationM = elevationM
        )
    }

    private fun getMoonAltAt(timeMs: Long, latDeg: Double, lonDeg: Double, elevationM: Double): Double {
        return getMoonHorizAt(timeMs, latDeg, lonDeg, elevationM).altitudeDeg
    }

    /**
     * Gets both next Solar and next Lunar eclipse evaluated for the user's location.
     * Keeps an actively ongoing eclipse visible until its end contact rather than dropping it at peak.
     */
    fun getNextEclipses(
        nowMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double = 0.0,
        timezoneId: String = "Asia/Tehran",
        calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI
    ): Pair<EclipseResult, EclipseResult> {
        val lookbackMs = (3.5 * 3600 * 1000).toLong()

        var solarEvent = findNextSolarEclipse(nowMs - lookbackMs) ?: canonicalEclipses.first { it.isSolar }
        var solarResult = evaluateEclipse(
            event = solarEvent,
            userLatDeg = userLatDeg,
            userLonDeg = userLonDeg,
            elevationM = elevationM,
            nowMs = nowMs,
            timezoneId = timezoneId,
            calendarSystem = calendarSystem
        )
        val solarEndMs = if (solarResult.isLocallyVisible) {
            max(solarResult.localEndTimeMs, solarEvent.maximumMs)
        } else {
            solarEvent.maximumMs + lookbackMs / 2
        }
        if (nowMs > solarEndMs) {
            solarEvent = findNextSolarEclipse(nowMs) ?: solarEvent
            solarResult = evaluateEclipse(
                event = solarEvent,
                userLatDeg = userLatDeg,
                userLonDeg = userLonDeg,
                elevationM = elevationM,
                nowMs = nowMs,
                timezoneId = timezoneId,
                calendarSystem = calendarSystem
            )
        }

        var lunarEvent = findNextLunarEclipse(nowMs - lookbackMs) ?: canonicalEclipses.first { !it.isSolar }
        var lunarResult = evaluateEclipse(
            event = lunarEvent,
            userLatDeg = userLatDeg,
            userLonDeg = userLonDeg,
            elevationM = elevationM,
            nowMs = nowMs,
            timezoneId = timezoneId,
            calendarSystem = calendarSystem
        )
        val lunarEndMs = if (lunarResult.isLocallyVisible) {
            max(lunarResult.localEndTimeMs, lunarEvent.penumbralEndMs)
        } else {
            lunarEvent.penumbralEndMs
        }
        if (nowMs > lunarEndMs) {
            lunarEvent = findNextLunarEclipse(nowMs) ?: lunarEvent
            lunarResult = evaluateEclipse(
                event = lunarEvent,
                userLatDeg = userLatDeg,
                userLonDeg = userLonDeg,
                elevationM = elevationM,
                nowMs = nowMs,
                timezoneId = timezoneId,
                calendarSystem = calendarSystem
            )
        }

        return Pair(solarResult, lunarResult)
    }

    /**
     * Generates full detailed modal info for an evaluated EclipseResult.
     */
    fun getDetailedEclipseInfo(
        result: EclipseResult,
        userLatDeg: Double,
        userLonDeg: Double,
        timezoneId: String = "Asia/Tehran"
    ): DetailedEclipseInfo {
        val event = result.event
        val resolvedTzId = if (timezoneId.isNotBlank()) {
            timezoneId
        } else if (userLatDeg in 24.0..40.0 && userLonDeg in 44.0..64.0) {
            "Asia/Tehran"
        } else {
            TimeZone.getDefault().id
        }
        val timeZone = TimeEngine.resolveTimeZone(resolvedTzId)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US).apply {
            this.timeZone = timeZone
        }

        val startStr = if (result.isLocallyVisible) timeFmt.format(Date(result.localStartTimeMs)) else "—"
        val peakStr = timeFmt.format(Date(result.localPeakTimeMs))
        val endStr = if (result.isLocallyVisible) timeFmt.format(Date(result.localEndTimeMs)) else "—"

        val durationMinutes = if (result.isLocallyVisible) {
            max(0L, (result.localEndTimeMs - result.localStartTimeMs) / 60000L)
        } else {
            0L
        }
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        val durationEn = if (!result.isLocallyVisible) {
            "Not visible locally"
        } else if (hours > 0) {
            "${hours}h ${mins}m"
        } else {
            "${mins}m"
        }
        val durationFa = if (!result.isLocallyVisible) {
            "غیرقابل رویت در موقعیت شما"
        } else if (hours > 0) {
            "${hours} ساعت و ${mins} دقیقه".toPersianDigits()
        } else {
            "${mins} دقیقه".toPersianDigits()
        }

        val safetyEn = if (event.isSolar) {
            "⚠️ CRITICAL EYE SAFETY: Never look directly at the Sun without certified ISO 12312-2 solar eclipse glasses! Regular sunglasses, smoked glass, or unfiltered camera lenses can cause permanent retinal damage."
        } else {
            "✅ 100% Safe for Naked Eye: Lunar eclipses are completely safe to observe directly without any eye protection, binoculars, or telescope filters."
        }

        val safetyFa = if (event.isSolar) {
            "⚠️ هشدار حیاتی سلامت چشم: هرگز بدون عینک مخصوص خورشیدگرفتگی (استاندارد ISO 12312-2) یا فیلتر استاندارد به خورشید نگاه نکنید! عینک آفتابی معمولی یا فیلم رادیولوژی به شبکیه چشم آسیب دائمی می‌زند."
        } else {
            "✅ کاملاً ایمن برای چشم: تماشای ماه‌گرفتگی (خسوف) با چشم غیرمسلح، دوربین دوچشمی یا تلسکوپ کاملاً بی‌خطر است و نیاز به هیچ فیلتری ندارد."
        }

        val tipEn = if (event.isSolar) {
            "Use a pinhole projector or solar filter to watch the Moon's silhouette advance across the solar disk. Notice sharp crescent shadows under trees during peak coverage."
        } else {
            "During peak umbral eclipse, Earth's atmosphere refracts sunset-red light onto the lunar surface (Rayleigh scattering). Binoculars bring out subtle copper and turquoise hues along the shadow edge."
        }

        val tipFa = if (event.isSolar) {
            "از فیلتر خورشیدی یا روش تصویرسازی روزنه‌ای (سایه برگ درختان روی زمین) برای مشاهده هلال‌های زیبای خورشید در زمان اوج گرفتگی استفاده کنید."
        } else {
            "در زمان اوج خسوف، شکست نور خورشید در جو زمین باعث سرخ‌فام شدن قرص ماه می‌شود. استفاده از دوربین دوچشمی رنگ‌های مسی و فیروزه‌ای لبه سایه زمین را به‌وضوح نشان می‌دهد."
        }

        return DetailedEclipseInfo(
            event = event,
            result = result,
            daysRemaining = result.daysRemaining,
            localStartTimeStr = startStr,
            localPeakTimeStr = peakStr,
            localEndTimeStr = endStr,
            durationTextEn = durationEn,
            durationTextFa = durationFa,
            obscurationPercent = result.localObscurationPercent,
            targetAltDeg = round(result.targetAltitudeDeg * 10.0) / 10.0,
            targetAzDeg = round(result.targetAzimuthDeg * 10.0) / 10.0,
            safetyGuideEn = safetyEn,
            safetyGuideFa = safetyFa,
            localMagnitude = result.localMagnitude,
            observationTipEn = tipEn,
            observationTipFa = tipFa
        )
    }

    private fun formatDates(
        ms: Long,
        timeZone: TimeZone = TimeEngine.TEHRAN_TIME_ZONE,
        calendarSystem: CalendarSystem = CalendarSystem.SOLAR_HIJRI
    ): Pair<String, String> {
        val enStr = TimeEngine.formatDate(
            timestampMs = ms,
            calendarSystem = calendarSystem,
            isFa = false,
            timeZone = timeZone
        )

        val faStr = TimeEngine.formatDate(
            timestampMs = ms,
            calendarSystem = calendarSystem,
            isFa = true,
            timeZone = timeZone
        )

        return Pair(enStr, faStr)
    }

    /**
     * Exact geometric area overlap fraction of Sun disk covered by Moon disk.
     */
    private fun calculateDiskOverlapFraction(rSun: Double, rMoon: Double, d: Double): Double {
        if (d >= rSun + rMoon) return 0.0
        if (d <= abs(rSun - rMoon)) {
            return if (rMoon >= rSun) 1.0 else (rMoon * rMoon) / (rSun * rSun)
        }

        val r1 = rSun
        val r2 = rMoon
        val d2 = d * d
        val r1Sq = r1 * r1
        val r2Sq = r2 * r2

        val alpha = acos(((d2 + r1Sq - r2Sq) / (2.0 * d * r1)).coerceIn(-1.0, 1.0))
        val beta = acos(((d2 + r2Sq - r1Sq) / (2.0 * d * r2)).coerceIn(-1.0, 1.0))

        val area1 = r1Sq * alpha
        val area2 = r2Sq * beta
        val areaTriangle = 0.5 * sqrt(max(0.0, (-d + r1 + r2) * (d + r1 - r2) * (d - r1 + r2) * (d + r1 + r2)))

        val intersectionArea = area1 + area2 - areaTriangle
        val sunArea = Math.PI * r1Sq

        return (intersectionArea / sunArea).coerceIn(0.0, 1.0)
    }

    private fun calculateAngularSeparation(ra1: Double, dec1: Double, ra2: Double, dec2: Double): Double {
        val r1 = ra1 * DEG2RAD
        val d1 = dec1 * DEG2RAD
        val r2 = ra2 * DEG2RAD
        val d2 = dec2 * DEG2RAD
        val cosSep = sin(d1) * sin(d2) + cos(d1) * cos(d2) * cos(r1 - r2)
        return acos(cosSep.coerceIn(-1.0, 1.0)) * RAD2DEG
    }

    /**
     * Full Meeus Chapter 54 dynamic eclipse search for dates outside the canonical table.
     */
    private fun searchDynamicEclipse(afterMs: Long, isSolar: Boolean): EclipseEvent? {
        val baseJd = afterMs / 86400000.0 + 2440587.5
        var k = floor((baseJd - 2451550.09766) / SYNODIC_MONTH_DAYS).toLong() - 1L

        for (i in 0..72) {
            val kVal = if (isSolar) k.toDouble() else k + 0.5
            val T = kVal / 1236.85
            val T2 = T * T
            val T3 = T2 * T
            val T4 = T3 * T

            val Fdeg = (((160.7108 + 390.67050284 * kVal - 0.0016118 * T2 - 0.00000227 * T3 + 0.000000011 * T4) % 360.0) + 360.0) % 360.0
            if (abs(sin(Fdeg * DEG2RAD)) <= 0.36) {
                var jde = 2451550.09766 + 29.530588861 * kVal + 0.00015437 * T2 - 0.000000150 * T3 + 0.00000000073 * T4
                val E = 1.0 - 0.002516 * T - 0.0000074 * T2
                val M = (2.5534 + 29.10535670 * kVal - 0.0000014 * T2 - 0.00000011 * T3) * DEG2RAD
                val Mp = (201.5643 + 385.81693528 * kVal + 0.0107582 * t2OrZero(T2) + 0.00001238 * T3 - 0.000000058 * T4) * DEG2RAD
                val omega = (124.7746 - 1.56375588 * kVal + 0.0020672 * T2 + 0.00000215 * T3) * DEG2RAD
                val F1 = (Fdeg - 0.02665 * sin(omega)) * DEG2RAD
                val A1 = (299.77 + 0.107408 * kVal - 0.009173 * T2) * DEG2RAD

                jde += (if (isSolar) -0.4075 else -0.4065) * sin(Mp) +
                        (if (isSolar) 0.1721 else 0.1727) * E * sin(M) +
                        0.0161 * sin(2.0 * Mp) -
                        0.0097 * sin(2.0 * F1) +
                        0.0073 * E * sin(Mp - M) -
                        0.0050 * E * sin(Mp + M) -
                        0.0023 * sin(Mp - 2.0 * F1) +
                        0.0021 * E * sin(2.0 * M) +
                        0.0012 * sin(Mp + 2.0 * F1) +
                        0.0006 * E * sin(2.0 * Mp + M) -
                        0.0004 * sin(3.0 * Mp) -
                        0.0003 * E * sin(M + 2.0 * F1) +
                        0.0003 * sin(A1) -
                        0.0002 * E * sin(M - 2.0 * F1) -
                        0.0002 * E * sin(2.0 * Mp - M) -
                        0.0002 * sin(omega)

                val maxMs = jdeToUtcMs(jde)
                if (maxMs > afterMs) {
                    val P = 0.2070 * E * sin(M) +
                            0.0024 * E * sin(2.0 * M) -
                            0.0392 * sin(Mp) +
                            0.0116 * sin(2.0 * Mp) -
                            0.0073 * E * sin(Mp + M) +
                            0.0067 * E * sin(Mp - M) +
                            0.0118 * sin(2.0 * F1)
                    val Q = 5.2207 -
                            0.0048 * E * cos(M) +
                            0.0020 * E * cos(2.0 * M) -
                            0.3299 * cos(Mp) -
                            0.0060 * E * cos(Mp + M) +
                            0.0041 * E * cos(Mp - M)
                    val W = abs(cos(F1))
                    val gamma = (P * cos(F1) + Q * sin(F1)) * (1.0 - 0.0048 * W)
                    val u = 0.0059 +
                            0.0046 * E * cos(M) -
                            0.0182 * cos(Mp) +
                            0.0004 * cos(2.0 * Mp) -
                            0.0005 * cos(M + Mp)

                    if (isSolar) {
                        val absGamma = abs(gamma)
                        if (absGamma <= 1.5433 + u) {
                            val type: EclipseType
                            val mag: Double
                            val durSec: Int
                            if (absGamma < 0.9972) {
                                val omegaH = 0.00464 * sqrt(max(0.0, 1.0 - gamma * gamma))
                                type = when {
                                    u < 0.0 -> EclipseType.TOTAL_SOLAR
                                    u > omegaH -> EclipseType.ANNULAR_SOLAR
                                    else -> EclipseType.HYBRID_SOLAR
                                }
                                mag = if (type == EclipseType.ANNULAR_SOLAR) {
                                    ((0.5460 - u) / 0.5460).coerceIn(0.88, 0.999)
                                } else {
                                    (1.0 + abs(u) / 0.273).coerceIn(1.001, 1.08)
                                }
                                durSec = 210
                            } else {
                                type = EclipseType.PARTIAL_SOLAR
                                mag = ((1.5433 + u - absGamma) / (0.5461 + 2.0 * u)).coerceIn(0.01, 0.99)
                                durSec = 0
                            }
                            val (nameEn, nameFa) = when (type) {
                                EclipseType.TOTAL_SOLAR -> "Total Solar Eclipse" to "خورشیدگرفتگی کامل"
                                EclipseType.ANNULAR_SOLAR -> "Annular Solar Eclipse" to "خورشیدگرفتگی حلقوی"
                                EclipseType.HYBRID_SOLAR -> "Hybrid Solar Eclipse" to "خورشیدگرفتگی مرکب"
                                else -> "Partial Solar Eclipse" to "خورشیدگرفتگی جزئی"
                            }
                            return EclipseEvent(
                                type = type,
                                maximumMs = maxMs,
                                magnitude = mag,
                                saros = 136,
                                gamma = gamma,
                                nameEn = nameEn,
                                nameFa = nameFa,
                                durationTotalSeconds = durSec,
                                maxTotalityRegionEn = "Global Eclipse Path",
                                maxTotalityRegionFa = "مسیر جهانی گرفتگی",
                                descriptionEn = "Predicted solar eclipse computed via Meeus Chapter 54 Besselian elements.",
                                descriptionFa = "خورشیدگرفتگی محاسبه‌شده با الگوریتم نجومی میوس (فصل ۵۴).",
                                isSolar = true
                            )
                        }
                    } else {
                        val absGamma = abs(gamma)
                        val magPen = (1.5573 + u - absGamma) / 0.5450
                        val magUmb = (1.0128 - u - absGamma) / 0.5450
                        if (magPen > 0.0) {
                            val type: EclipseType
                            val mag: Double
                            when {
                                magUmb >= 1.0 -> {
                                    type = EclipseType.TOTAL_LUNAR
                                    mag = magUmb
                                }
                                magUmb > 0.0 -> {
                                    type = EclipseType.PARTIAL_LUNAR
                                    mag = magUmb
                                }
                                else -> {
                                    type = EclipseType.PENUMBRAL_LUNAR
                                    mag = magPen
                                }
                            }
                            val n = 0.5458 + 0.0400 * cos(Mp)
                            val tp = 1.5573 + u
                            val pp = 1.0128 - u
                            val tau = 0.4678 - u
                            val sdPenMs = ((1.0 / n) * sqrt(max(0.0, tp * tp - gamma * gamma)) * 3600000.0).roundToLong()
                            val sdUmbMs = if (pp * pp > gamma * gamma) {
                                ((1.0 / n) * sqrt(max(0.0, pp * pp - gamma * gamma)) * 3600000.0).roundToLong()
                            } else 0L
                            val sdTotMs = if (tau * tau > gamma * gamma) {
                                ((1.0 / n) * sqrt(max(0.0, tau * tau - gamma * gamma)) * 3600000.0).roundToLong()
                            } else 0L
                            val durSec = when (type) {
                                EclipseType.TOTAL_LUNAR -> ((2L * sdTotMs) / 1000L).toInt()
                                EclipseType.PARTIAL_LUNAR -> ((2L * sdUmbMs) / 1000L).toInt()
                                else -> 0
                            }
                            val (nameEn, nameFa) = when (type) {
                                EclipseType.TOTAL_LUNAR -> "Total Lunar Eclipse (Blood Moon)" to "خسوف کامل (ماه خونین)"
                                EclipseType.PARTIAL_LUNAR -> "Partial Lunar Eclipse" to "خسوف جزئی"
                                else -> "Penumbral Lunar Eclipse" to "خسوف نیم‌سایه‌ای"
                            }
                            return EclipseEvent(
                                type = type,
                                maximumMs = maxMs,
                                magnitude = mag,
                                saros = 135,
                                gamma = gamma,
                                nameEn = nameEn,
                                nameFa = nameFa,
                                durationTotalSeconds = durSec,
                                maxTotalityRegionEn = "Nighttime Hemisphere",
                                maxTotalityRegionFa = "نیمکره شب زمین",
                                descriptionEn = "Predicted lunar eclipse computed via Meeus Chapter 54 Besselian elements.",
                                descriptionFa = "ماه‌گرفتگی محاسبه‌شده با الگوریتم نجومی میوس (فصل ۵۴).",
                                isSolar = false,
                                penumbralStartMs = maxMs - sdPenMs,
                                umbralStartMs = if (sdUmbMs > 0L) maxMs - sdUmbMs else maxMs,
                                totalityStartMs = if (sdTotMs > 0L) maxMs - sdTotMs else maxMs,
                                totalityEndMs = if (sdTotMs > 0L) maxMs + sdTotMs else maxMs,
                                umbralEndMs = if (sdUmbMs > 0L) maxMs + sdUmbMs else maxMs,
                                penumbralEndMs = maxMs + sdPenMs
                            )
                        }
                    }
                }
            }
            k++
        }
        return null
    }

    private fun t2OrZero(t2: Double): Double = t2
}
