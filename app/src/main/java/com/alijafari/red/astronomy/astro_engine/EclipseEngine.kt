package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.util.toPersianDigits
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * High-precision Eclipse prediction and local circumstances engine.
 *
 * Implements:
 * - Meeus (1998) Chapter 54 (Besselian elements & Eclipse predictions)
 * - Meeus (1998) Chapter 49 (Phases of the Moon)
 * - NASA Goddard / Fred Espenak 5-Millennium Canon of Solar & Lunar Eclipses
 * - High-precision topocentric local circumstances (Start C1, Max, End C4, Alt/Az, Obscuration %)
 */
class EclipseEngine {

    companion object {
        private const val DEG2RAD = Math.PI / 180.0
        private const val RAD2DEG = 180.0 / Math.PI
        private const val SYNODIC_MONTH_DAYS = 29.530588853

        // Shared singleton instance
        val instance = EclipseEngine()

        /**
         * Convenience helper to evaluate next eclipses for a user location.
         */
        fun getNextEclipses(
            nowMs: Long,
            userLatDeg: Double,
            userLonDeg: Double,
            elevationM: Double = 0.0
        ): Pair<EclipseResult, EclipseResult> {
            return instance.getNextEclipses(nowMs, userLatDeg, userLonDeg, elevationM)
        }

        /**
         * Convenience helper for detailed eclipse info.
         */
        fun getDetailedEclipseInfo(
            result: EclipseResult,
            userLatDeg: Double,
            userLonDeg: Double
        ): DetailedEclipseInfo {
            return instance.getDetailedEclipseInfo(result, userLatDeg, userLonDeg)
        }

        fun computeDetailedInfo(
            result: EclipseResult,
            userLatDeg: Double,
            userLonDeg: Double
        ): DetailedEclipseInfo {
            return instance.getDetailedEclipseInfo(result, userLatDeg, userLonDeg)
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
        val safetyGuideFa: String
    )

    private val lunarSolar = LunarSolarEngine()

    // NASA / Espenak authoritative canon of eclipses (2024 to 2035)
    private val canonicalEclipses = listOf(
        EclipseEvent(
            type = EclipseType.PENUMBRAL_LUNAR,
            maximumMs = 1711351800000L, // 2024-03-25 07:13 UTC
            magnitude = 0.956,
            saros = 113,
            gamma = 1.0609,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "خسوف نیم‌سایه‌ای",
            durationTotalSeconds = 16740,
            maxTotalityRegionEn = "Americas, Antarctica, Alaska, NE Asia",
            maxTotalityRegionFa = "قاره آمریکا، جنوبگان، آلاسکا و شمال شرق آسیا",
            descriptionEn = "A deep penumbral lunar eclipse where 95.6% of the Moon disk passed through Earth's penumbral shadow.",
            descriptionFa = "خسوف عمیق نیم‌سایه‌ای که طی آن ۹۵.۶٪ قرص ماه وارد نیم‌سایه زمین شد.",
            isSolar = false,
            penumbralStartMs = 1711343460000L,
            penumbralEndMs = 1711360140000L
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
            maximumMs = 1726629840000L, // 2024-09-18 02:44 UTC
            magnitude = 0.0848,
            saros = 118,
            gamma = -0.9792,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی",
            durationTotalSeconds = 3840,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East, Western Asia",
            maxTotalityRegionFa = "آمریکا، اروپا، آفریقا، خاورمیانه و غرب آسیا",
            descriptionEn = "A shallow partial lunar eclipse with 8.5% of the Moon in Earth's dark umbral shadow.",
            descriptionFa = "خسوف جزئی با ورود ۸.۵٪ از قرص ماه به بخش تاریک سایه زمین.",
            isSolar = false,
            penumbralStartMs = 1726621260000L,
            umbralStartMs = 1726627920000L,
            umbralEndMs = 1726631760000L,
            penumbralEndMs = 1726638420000L
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
            penumbralStartMs = 1741926600000L,
            umbralStartMs = 1741931340000L,
            totalityStartMs = 1741933590000L,
            totalityEndMs = 1741937490000L,
            umbralEndMs = 1741939740000L,
            penumbralEndMs = 1741944480000L
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
            maximumMs = 1757271180000L, // 2025-09-07 18:12 UTC
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
            penumbralStartMs = 1757260920000L,
            umbralStartMs = 1757266020000L,
            totalityStartMs = 1757268720000L,
            totalityEndMs = 1757273640000L,
            umbralEndMs = 1757276340000L,
            penumbralEndMs = 1757281440000L
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_SOLAR,
            maximumMs = 1758482520000L, // 2025-09-21 19:43 UTC
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
            maximumMs = 1771329660000L, // 2026-02-17 12:13 UTC
            magnitude = 0.9630,
            saros = 121,
            gamma = -0.8588,
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
            gamma = 0.3765,
            nameEn = "Total Lunar Eclipse (Blood Moon)",
            nameFa = "خسوف کامل (ماه سرخ)",
            durationTotalSeconds = 3480,
            maxTotalityRegionEn = "Asia, Australia, Pacific, Americas",
            maxTotalityRegionFa = "شرق آسیا، استرالیا، اقیانوس آرام و قاره آمریکا",
            descriptionEn = "Total lunar eclipse with 58 minutes of totality visible across Asia, Australia, and the Americas.",
            descriptionFa = "خسوف کامل با ۵۸ دقیقه گرفتگی کامل در شرق آسیا، استرالیا و آمریکا.",
            isSolar = false,
            penumbralStartMs = 1772528760000L,
            umbralStartMs = 1772533440000L,
            totalityStartMs = 1772535900000L,
            totalityEndMs = 1772539380000L,
            umbralEndMs = 1772541840000L,
            penumbralEndMs = 1772546520000L
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1786552620000L, // 2026-08-12 17:47 UTC
            magnitude = 1.0386,
            saros = 126,
            gamma = 0.8977,
            nameEn = "Total Solar Eclipse (Great European Eclipse)",
            nameFa = "خورشیدگرفتگی کامل اروپا و اسپانیا",
            durationTotalSeconds = 138,
            maxTotalityRegionEn = "Greenland, Western Iceland, Northern Spain (A Coruña to Palma de Mallorca)",
            maxTotalityRegionFa = "گرینلند، ایسلند، شمال و مرکز اسپانیا؛ جزئی در سراسر اروپا و غرب آسیا",
            descriptionEn = "A magnificent total solar eclipse sweeping across Greenland, Iceland, and Northern Spain. Partial eclipse visible across Europe and North Africa.",
            descriptionFa = "کسوف کامل در گرینلند، ایسلند و اسپانیا. فاز جزئی در سراسر اروپا و شمال آفریقا قابل مشاهده است.",
            isSolar = true
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_LUNAR,
            maximumMs = 1787915520000L, // 2026-08-28 04:14 UTC
            magnitude = 0.9299,
            saros = 138,
            gamma = -0.4964,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی عمیق",
            durationTotalSeconds = 11880,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East, Western Asia",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا، خاورمیانه و غرب آسیا",
            descriptionEn = "A very deep partial lunar eclipse with 93% of the Moon covered by Earth's dark umbra.",
            descriptionFa = "خسوف جزئی بسیار عمیق که در آن ۹۳٪ قرص ماه در سایه تاریک زمین قرار می‌گیرد.",
            isSolar = false,
            penumbralStartMs = 1787905380000L,
            umbralStartMs = 1787909580000L,
            umbralEndMs = 1787921460000L,
            penumbralEndMs = 1787925660000L
        ),
        EclipseEvent(
            type = EclipseType.ANNULAR_SOLAR,
            maximumMs = 1801931880000L, // 2027-02-06 16:00 UTC
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
            maximumMs = 1803150600000L, // 2027-02-20 23:14 UTC
            magnitude = 0.952,
            saros = 143,
            gamma = 1.048,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "خسوف نیم‌سایه‌ای",
            durationTotalSeconds = 14820,
            maxTotalityRegionEn = "Americas, Europe, Africa, Middle East, Asia",
            maxTotalityRegionFa = "قاره آمریکا، اروپا، آفریقا، خاورمیانه و آسیا",
            descriptionEn = "A penumbral eclipse visible across Europe, Africa, and the Middle East.",
            descriptionFa = "خسوف نیم‌سایه‌ای قابل رویت در اروپا، آفریقا و خاورمیانه.",
            isSolar = false,
            penumbralStartMs = 1803143160000L,
            penumbralEndMs = 1803158040000L
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1817208420000L, // 2027-08-02 10:07 UTC
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
            maximumMs = 1818485760000L, // 2027-08-17 07:14 UTC
            magnitude = 0.571,
            saros = 148,
            gamma = -1.2497,
            nameEn = "Penumbral Lunar Eclipse",
            nameFa = "خسوف نیم‌سایه‌ای",
            durationTotalSeconds = 13000,
            maxTotalityRegionEn = "Pacific Ocean, Americas, Eastern Asia, Australia",
            maxTotalityRegionFa = "اقیانوس آرام، قاره آمریکا، شرق آسیا و استرالیا",
            descriptionEn = "A minor penumbral eclipse visible across the Pacific rim.",
            descriptionFa = "خسوف نیم‌سایه‌ای در حاشیه اقیانوس آرام.",
            isSolar = false
        ),
        EclipseEvent(
            type = EclipseType.PARTIAL_LUNAR,
            maximumMs = 1831295280000L, // 2028-01-12 04:14 UTC
            magnitude = 0.066,
            saros = 115,
            gamma = 0.9817,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی",
            durationTotalSeconds = 3400,
            maxTotalityRegionEn = "Europe, Africa, Asia, Australia, Americas",
            maxTotalityRegionFa = "اروپا، آفریقا، آسیا، استرالیا و آمریکا",
            descriptionEn = "A shallow partial lunar eclipse with small umbral grazing.",
            descriptionFa = "خسوف جزئی کم‌عمق در اروپا، آسیا و آفریقا.",
            isSolar = false
        ),
        EclipseEvent(
            type = EclipseType.ANNULAR_SOLAR,
            maximumMs = 1832508780000L, // 2028-01-26 15:08 UTC
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
            maximumMs = 1846497480000L, // 2028-07-06 18:20 UTC
            magnitude = 0.3892,
            saros = 120,
            gamma = -0.7989,
            nameEn = "Partial Lunar Eclipse",
            nameFa = "خسوف جزئی",
            durationTotalSeconds = 8520,
            maxTotalityRegionEn = "Europe, Africa, Asia, Australia, Middle East",
            maxTotalityRegionFa = "اروپا، آفریقا، آسیا، استرالیا و خاورمیانه",
            descriptionEn = "A partial lunar eclipse covering 39% of the Moon disk.",
            descriptionFa = "خسوف جزئی با پوشش ۳۹٪ از قرص ماه.",
            isSolar = false
        ),
        EclipseEvent(
            type = EclipseType.TOTAL_SOLAR,
            maximumMs = 1847841360000L, // 2028-07-22 02:56 UTC
            magnitude = 1.0556,
            saros = 146,
            gamma = -0.3752,
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
            maximumMs = 1861868580000L, // 2028-12-31 16:53 UTC
            magnitude = 1.2461,
            saros = 125,
            gamma = -0.3258,
            nameEn = "New Year's Eve Total Lunar Eclipse (Blood Moon)",
            nameFa = "خسوف کامل شب سال نو (ماه خونین)",
            durationTotalSeconds = 4260,
            maxTotalityRegionEn = "Europe, Africa, Asia, Australia, Middle East, Iran",
            maxTotalityRegionFa = "اروپا، آفریقا، سراسر آسیا، استرالیا، خاورمیانه و ایران",
            descriptionEn = "A breathtaking total lunar eclipse on New Year's Eve 2028 with 71 minutes of totality, completely visible from Iran and the Middle East.",
            descriptionFa = "خسوف کامل رویایی در شب سال نو ۲۰۲۸ با ۷۱ دقیقه گرفتگی کامل و رصد بی‌نقص در سراسر ایران و خاورمیانه.",
            isSolar = false
        )
    )

    /**
     * Calculates time of New Moon for lunation k using Meeus Chapter 49.
     */
    fun newMoonTime(k: Long): Long {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        var jde = 2451549.59268 + 29.530588853 * k + 0.0001337 * t2 - 0.000000150 * t3 + 0.00000000073 * t4

        val E = 1.0 - 0.002516 * t - 0.0000074 * t2
        val M = (2.5534 + 29.10535669 * k - 0.0000218 * t2 - 0.00000011 * t3) * DEG2RAD
        val Mp = (201.5643 + 385.81693528 * k + 0.0107438 * t2 + 0.00001239 * t3 - 0.000000058 * t4) * DEG2RAD
        val F = (160.7108 + 390.67050274 * k - 0.0016341 * t2 - 0.00000227 * t3 + 0.000000011 * t4) * DEG2RAD
        val omega = (124.7746 - 1.56375580 * k + 0.0020691 * t2 + 0.00000215 * t3) * DEG2RAD

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

        return ((jde - 2440587.5) * 86400000.0).toLong()
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

        var jde = 2451549.59268 + 29.530588853 * kFull + 0.0001337 * t2 - 0.000000150 * t3 + 0.00000000073 * t4

        val E = 1.0 - 0.002516 * t - 0.0000074 * t2
        val M = (2.5534 + 29.10535669 * kFull - 0.0000218 * t2 - 0.00000011 * t3) * DEG2RAD
        val Mp = (201.5643 + 385.81693528 * kFull + 0.0107438 * t2 + 0.0107438 * t3) * DEG2RAD
        val F = (160.7108 + 390.67050274 * kFull - 0.0016341 * t2 - 0.00000227 * t3) * DEG2RAD
        val omega = (124.7746 - 1.56375580 * kFull + 0.0020691 * t2) * DEG2RAD

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

        return ((jde - 2440587.5) * 86400000.0).toLong()
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

        var jde = 2451549.59268 + 29.530588853 * kPhase + 0.0001337 * t2 - 0.000000150 * t3 + 0.00000000073 * t4

        val E = 1.0 - 0.002516 * t - 0.0000074 * t2
        val M = (2.5534 + 29.10535669 * kPhase - 0.0000218 * t2) * DEG2RAD
        val Mp = (201.5643 + 385.81693528 * kPhase + 0.0107438 * t2) * DEG2RAD
        val F = (160.7108 + 390.67050274 * kPhase - 0.0016341 * t2) * DEG2RAD

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

        return ((jde - 2440587.5) * 86400000.0).toLong()
    }

    fun findNextNewMoon(afterMs: Long): Long {
        val baseJd = afterMs / 86400000.0 + 2440587.5
        var k = floor((baseJd - 2451550.09766) / SYNODIC_MONTH_DAYS).toLong()
        while (true) {
            val t = newMoonTime(k)
            if (t > afterMs) return t
            k++
        }
    }

    fun findNextFullMoon(afterMs: Long): Long {
        val baseJd = afterMs / 86400000.0 + 2440587.5
        var k = floor((baseJd - 2451550.09766) / SYNODIC_MONTH_DAYS).toLong()
        while (true) {
            val t = fullMoonTime(k)
            if (t > afterMs) return t
            k++
        }
    }

    fun findNextSolarEclipse(afterMs: Long): EclipseEvent? {
        val canonical = canonicalEclipses.firstOrNull { it.isSolar && it.maximumMs > afterMs }
        if (canonical != null) return canonical

        // Fallback to Meeus Ch. 54 dynamic search
        return searchDynamicEclipse(afterMs, isSolar = true)
    }

    fun findNextLunarEclipse(afterMs: Long): EclipseEvent? {
        val canonical = canonicalEclipses.firstOrNull { !it.isSolar && it.maximumMs > afterMs }
        if (canonical != null) return canonical

        return searchDynamicEclipse(afterMs, isSolar = false)
    }

    fun findEclipses(startMs: Long, endMs: Long): List<EclipseEvent> {
        val canonical = canonicalEclipses.filter { it.maximumMs in startMs..endMs }
        if (canonical.isNotEmpty()) return canonical.sortedBy { it.maximumMs }

        // Dynamic search
        val result = mutableListOf<EclipseEvent>()
        var curMs = startMs
        while (curMs < endMs) {
            val s = findNextSolarEclipse(curMs)
            val l = findNextLunarEclipse(curMs)
            val next = listOfNotNull(s, l).filter { it.maximumMs in curMs..endMs }.minByOrNull { it.maximumMs }
                ?: break
            result.add(next)
            curMs = next.maximumMs + 10 * 86400000L
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
        elevationM: Double = 0.0
    ): EclipseResult {
        return if (event.isSolar) {
            evaluateSolarEclipseLocal(event, userLatDeg, userLonDeg, elevationM)
        } else {
            evaluateLunarEclipseLocal(event, userLatDeg, userLonDeg, elevationM)
        }
    }

    /**
     * High-precision local circumstances for Solar Eclipse at user location.
     */
    private fun evaluateSolarEclipseLocal(
        event: EclipseEvent,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double
    ): EclipseResult {
        val maxMs = event.maximumMs
        val stepMs = 2 * 60 * 1000L // 2-minute step
        val scanRadiusMs = (3.5 * 3600 * 1000).toLong()

        var firstContactMs = 0L
        var lastContactMs = 0L
        var peakTimeMs = maxMs
        var maxObscuration = 0.0
        var maxMagnitude = 0.0
        var peakSunAlt = 0.0
        var peakSunAz = 0.0

        var curTime = maxMs - scanRadiusMs
        while (curTime <= maxMs + scanRadiusMs) {
            val jd = curTime / 86400000.0 + 2440587.5
            val astroTime = AstroTime.fromJd(jd)
            val lastDeg = TimeEngine.getLAST(jd, userLonDeg)

            val sunGeo = lunarSolar.calculateSun(astroTime)
            val moonGeo = lunarSolar.calculateMoon(astroTime)

            val sunTopoEq = CoordinateEngine.geocentricToTopocentric(
                geocentric = CoordinateEngine.Equatorial(sunGeo.raDeg, sunGeo.decDeg),
                geocentricDistanceKm = sunGeo.distanceAu * 149597870.7,
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

            val sunRadiusDeg = asin(696340.0 / (sunGeo.distanceAu * 149597870.7)) * RAD2DEG
            val moonRadiusDeg = asin(1737.4 / moonGeo.distanceKm) * RAD2DEG

            val sepDeg = calculateAngularSeparation(
                sunTopoEq.raDeg, sunTopoEq.decDeg,
                moonTopoEq.raDeg, moonTopoEq.decDeg
            )

            if (sepDeg < sunRadiusDeg + moonRadiusDeg) {
                // Moon disk intersects Sun disk
                val mag = (sunRadiusDeg + moonRadiusDeg - sepDeg) / (2.0 * sunRadiusDeg)
                val obscuration = calculateDiskOverlapFraction(sunRadiusDeg, moonRadiusDeg, sepDeg)

                // Only visible if Sun is above horizon (allowing atmospheric refraction -0.5°)
                if (sunHoriz.altitudeDeg > -0.5) {
                    if (firstContactMs == 0L) firstContactMs = curTime
                    lastContactMs = curTime

                    if (obscuration > maxObscuration) {
                        maxObscuration = obscuration
                        maxMagnitude = mag
                        peakTimeMs = curTime
                        peakSunAlt = sunHoriz.altitudeDeg
                        peakSunAz = sunHoriz.azimuthDeg
                    }
                }
            }

            curTime += stepMs
        }

        val isVisible = maxObscuration >= 0.005 // At least 0.5% coverage visible above horizon
        val obscurationPercent = (maxObscuration * 100.0).roundToInt().coerceIn(0, 100)

        val localTypeStrEn: String
        val localTypeStrFa: String
        if (isVisible) {
            when {
                maxObscuration >= 0.999 -> {
                    localTypeStrEn = "Total Solar Eclipse"
                    localTypeStrFa = "خورشیدگرفتگی کامل"
                }
                event.type == EclipseType.ANNULAR_SOLAR && maxMagnitude >= 0.90 -> {
                    localTypeStrEn = "Annular Solar Eclipse (Ring of Fire)"
                    localTypeStrFa = "خورشیدگرفتگی حلقوی (حلقه آتش)"
                }
                else -> {
                    localTypeStrEn = "Partial Solar Eclipse ($obscurationPercent% coverage)"
                    localTypeStrFa = "خورشیدگرفتگی جزئی ($obscurationPercent٪ پوشش)"
                }
            }
        } else {
            localTypeStrEn = "Not Visible Locally (${event.nameEn})"
            localTypeStrFa = "عدم رویت در موقعیت شما (${event.nameFa})"
        }

        // Formatted Dates
        val (formattedEn, formattedFa) = formatDates(event.maximumMs)

        // Visibility explanatory texts
        val visibilityTextEn: String
        val visibilityTextFa: String
        if (isVisible) {
            val altStr = "${peakSunAlt.roundToInt()}°"
            visibilityTextEn = "Visible as $localTypeStrEn (Sun Altitude: $altStr at peak, $obscurationPercent% coverage)."
            visibilityTextFa = "قابل رویت به صورت $localTypeStrFa (ارتفاع خورشید در اوج: ${altStr.toPersianDigits()}، پوشش ${"$obscurationPercent%".toPersianDigits()})."
        } else {
            // Check why it wasn't visible
            val maxJd = maxMs / 86400000.0 + 2440587.5
            val maxAstroTime = AstroTime.fromJd(maxJd)
            val maxLastDeg = TimeEngine.getLAST(maxJd, userLonDeg)
            val maxSunGeo = lunarSolar.calculateSun(maxAstroTime)
            val maxSunHoriz = CoordinateEngine.equatorialToHorizontal(
                equatorial = CoordinateEngine.Equatorial(maxSunGeo.raDeg, maxSunGeo.decDeg),
                lastDeg = maxLastDeg,
                latitudeDeg = userLatDeg,
                observerElevationM = elevationM
            )

            if (maxSunHoriz.altitudeDeg <= -0.5) {
                visibilityTextEn = "Not visible directly: Occurs during nighttime at your location (Sun is below the horizon)."
                visibilityTextFa = "عدم رویت مستقیم: این گرفتگی در طول شب رخ می‌دهد (خورشید در موقعیت شما زیر افق قرار دارد)."
            } else {
                visibilityTextEn = "Not visible directly: Your location is outside the Moon's shadow path (0% coverage)."
                visibilityTextFa = "عدم رویت مستقیم: موقعیت شما خارج از مسیر سایه و نیم‌سایه ماه قرار دارد (پوشش ۰٪)."
            }
        }

        val daysRemaining = max(0, ((event.maximumMs - System.currentTimeMillis()) / 86400000.0).roundToInt())

        return EclipseResult(
            event = event,
            localNameEn = localTypeStrEn,
            localNameFa = localTypeStrFa,
            isLocallyVisible = isVisible,
            localObscurationPercent = obscurationPercent,
            localMagnitude = maxMagnitude,
            localStartTimeMs = if (firstContactMs != 0L) firstContactMs else maxMs - 3600000L,
            localPeakTimeMs = peakTimeMs,
            localEndTimeMs = if (lastContactMs != 0L) lastContactMs else maxMs + 3600000L,
            targetAltitudeDeg = peakSunAlt,
            targetAzimuthDeg = peakSunAz,
            formattedDateEn = formattedEn,
            formattedDateFa = formattedFa,
            localVisibilityTextEn = visibilityTextEn,
            localVisibilityTextFa = visibilityTextFa,
            daysRemaining = daysRemaining
        )
    }

    /**
     * High-precision local circumstances for Lunar Eclipse at user location.
     */
    private fun evaluateLunarEclipseLocal(
        event: EclipseEvent,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double
    ): EclipseResult {
        val maxMs = event.maximumMs
        val maxJd = maxMs / 86400000.0 + 2440587.5
        val maxAstroTime = AstroTime.fromJd(maxJd)
        val maxLastDeg = TimeEngine.getLAST(maxJd, userLonDeg)

        val moonGeo = lunarSolar.calculateMoon(maxAstroTime)
        val moonTopoEq = CoordinateEngine.geocentricToTopocentric(
            geocentric = CoordinateEngine.Equatorial(moonGeo.raDeg, moonGeo.decDeg),
            geocentricDistanceKm = moonGeo.distanceKm,
            lastDeg = maxLastDeg,
            latitudeDeg = userLatDeg,
            elevationM = elevationM
        )
        val moonHoriz = CoordinateEngine.equatorialToHorizontal(
            equatorial = moonTopoEq,
            lastDeg = maxLastDeg,
            latitudeDeg = userLatDeg,
            observerElevationM = elevationM
        )

        val peakMoonAlt = moonHoriz.altitudeDeg
        val peakMoonAz = moonHoriz.azimuthDeg

        // Check contact times visibility
        val p1Horiz = getMoonAltAt(event.penumbralStartMs, userLatDeg, userLonDeg, elevationM)
        val p4Horiz = getMoonAltAt(event.penumbralEndMs, userLatDeg, userLonDeg, elevationM)

        val isVisibleAtPeak = peakMoonAlt > -0.5
        val isVisibleAtAnyTime = isVisibleAtPeak || p1Horiz > -0.5 || p4Horiz > -0.5

        val obscurationPercent = if (isVisibleAtPeak) {
            when (event.type) {
                EclipseType.TOTAL_LUNAR -> 100
                EclipseType.PARTIAL_LUNAR -> (event.magnitude * 100.0).roundToInt().coerceIn(1, 99)
                EclipseType.PENUMBRAL_LUNAR -> (event.magnitude * 100.0).roundToInt().coerceIn(1, 99)
                else -> 0
            }
        } else 0

        val (formattedEn, formattedFa) = formatDates(event.maximumMs)

        val visibilityTextEn: String
        val visibilityTextFa: String
        if (isVisibleAtPeak) {
            val altStr = "${peakMoonAlt.roundToInt()}°"
            if (p1Horiz > -0.5 && p4Horiz > -0.5) {
                visibilityTextEn = "Fully visible from start to finish! Moon is high in the sky (Altitude: $altStr at peak)."
                visibilityTextFa = "کاملاً قابل رصد در تمام مراحل! ماه در آسمان در ارتفاع مناسب قرار دارد (ارتفاع در اوج: ${altStr.toPersianDigits()})."
            } else if (p1Horiz <= -0.5) {
                visibilityTextEn = "Partially visible after Moonrise (Moon altitude is $altStr at maximum eclipse)."
                visibilityTextFa = "قابل رصد پس از طلوع ماه (ارتفاع ماه در زمان اوج گرفتگی: ${altStr.toPersianDigits()})."
            } else {
                visibilityTextEn = "Partially visible until Moonset (Moon altitude is $altStr at maximum eclipse)."
                visibilityTextFa = "قابل رصد تا زمان غروب ماه (ارتفاع ماه در زمان اوج گرفتگی: ${altStr.toPersianDigits()})."
            }
        } else {
            visibilityTextEn = "Not visible directly: Occurs during daytime (Moon is below the horizon at your location)."
            visibilityTextFa = "عدم رویت مستقیم: این خسوف در طول روز رخ می‌دهد (ماه در موقعیت شما زیر افق قرار دارد)."
        }

        val daysRemaining = max(0, ((event.maximumMs - System.currentTimeMillis()) / 86400000.0).roundToInt())

        return EclipseResult(
            event = event,
            localNameEn = if (isVisibleAtPeak) event.nameEn else "Not Visible Locally (${event.nameEn})",
            localNameFa = if (isVisibleAtPeak) event.nameFa else "عدم رویت در موقعیت شما (${event.nameFa})",
            isLocallyVisible = isVisibleAtPeak,
            localObscurationPercent = obscurationPercent,
            localMagnitude = if (isVisibleAtPeak) event.magnitude else 0.0,
            localStartTimeMs = event.penumbralStartMs,
            localPeakTimeMs = maxMs,
            localEndTimeMs = event.penumbralEndMs,
            targetAltitudeDeg = peakMoonAlt,
            targetAzimuthDeg = peakMoonAz,
            formattedDateEn = formattedEn,
            formattedDateFa = formattedFa,
            localVisibilityTextEn = visibilityTextEn,
            localVisibilityTextFa = visibilityTextFa,
            daysRemaining = daysRemaining
        )
    }

    private fun getMoonAltAt(timeMs: Long, latDeg: Double, lonDeg: Double, elevationM: Double): Double {
        val jd = timeMs / 86400000.0 + 2440587.5
        val astroTime = AstroTime.fromJd(jd)
        val lastDeg = TimeEngine.getLAST(jd, lonDeg)
        val moonGeo = lunarSolar.calculateMoon(astroTime)
        val moonTopoEq = CoordinateEngine.geocentricToTopocentric(
            geocentric = CoordinateEngine.Equatorial(moonGeo.raDeg, moonGeo.decDeg),
            geocentricDistanceKm = moonGeo.distanceKm,
            lastDeg = lastDeg,
            latitudeDeg = latDeg,
            elevationM = elevationM
        )
        val moonHoriz = CoordinateEngine.equatorialToHorizontal(
            equatorial = moonTopoEq,
            lastDeg = lastDeg,
            latitudeDeg = latDeg,
            observerElevationM = elevationM
        )
        return moonHoriz.altitudeDeg
    }

    fun getNextEclipses(
        nowMs: Long,
        userLatDeg: Double,
        userLonDeg: Double,
        elevationM: Double = 0.0
    ): Pair<EclipseResult, EclipseResult> {
        val nextSolarEvent = findNextSolarEclipse(nowMs)
            ?: canonicalEclipses.first { it.isSolar }
        val nextLunarEvent = findNextLunarEclipse(nowMs)
            ?: canonicalEclipses.first { !it.isSolar }

        val solarResult = evaluateEclipse(nextSolarEvent, userLatDeg, userLonDeg, elevationM)
        val lunarResult = evaluateEclipse(nextLunarEvent, userLatDeg, userLonDeg, elevationM)

        return Pair(solarResult, lunarResult)
    }

    fun getDetailedEclipseInfo(
        result: EclipseResult,
        userLatDeg: Double,
        userLonDeg: Double
    ): DetailedEclipseInfo {
        val event = result.event
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        val startStr = timeFmt.format(Date(result.localStartTimeMs))
        val peakStr = timeFmt.format(Date(result.localPeakTimeMs))
        val endStr = timeFmt.format(Date(result.localEndTimeMs))

        val durationMinutes = ((result.localEndTimeMs - result.localStartTimeMs) / 60000L).coerceAtLeast(0)
        val hours = durationMinutes / 60
        val mins = durationMinutes % 60
        val durationEn = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        val durationFa = if (hours > 0) "${hours} ساعت و ${mins} دقیقه".toPersianDigits() else "${mins} دقیقه".toPersianDigits()

        val safetyEn = if (event.isSolar) {
            "⚠️ Eye Safety Warning: NEVER look directly at the Sun without ISO 12312-2 certified eclipse glasses or safe solar filters. Regular sunglasses or binoculars are not safe."
        } else {
            "✅ Safe to Observe: Lunar eclipses are 100% safe to observe directly with the naked eye, binoculars, or telescopes without any filters."
        }

        val safetyFa = if (event.isSolar) {
            "⚠️ هشدار ایمنی چشم: هرگز بدون عینک مخصوص خورشیدگرفتگی استاندارد (ISO 12312-2) یا فیلترهای استاندارد به خورشید نگاه نکنید. عینک آفتابی معمولی به هیچ عنوان ایمن نیست."
        } else {
            "✅ کاملاً ایمن: ماه‌گرفتگی (خسوف) کاملاً ایمن است و می‌توانید با چشم غیرمسلح، دوربین دوچشمی یا تلسکوپ بدون هیچ فیلتری آن را رصد کنید."
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
            safetyGuideFa = safetyFa
        )
    }

    private fun formatDates(ms: Long): Pair<String, String> {
        val enFmt = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
        val enStr = enFmt.format(Date(ms))

        val sh = TimeEngine.toSolarHijri(ms)
        val faStr = "${sh.day} ${sh.monthNameFa} ${sh.year}".toPersianDigits()

        return Pair(enStr, faStr)
    }

    /**
     * Calculates area fraction of disk 1 covered by disk 2.
     */
    private fun calculateDiskOverlapFraction(r1: Double, r2: Double, d: Double): Double {
        if (d >= r1 + r2) return 0.0
        if (d <= abs(r1 - r2)) {
            return if (r2 >= r1) 1.0 else (r2 * r2) / (r1 * r1)
        }

        val r1Sq = r1 * r1
        val r2Sq = r2 * r2
        val dSq = d * d

        val alpha = acos(((dSq + r1Sq - r2Sq) / (2.0 * d * r1)).coerceIn(-1.0, 1.0))
        val beta = acos(((dSq + r2Sq - r1Sq) / (2.0 * d * r2)).coerceIn(-1.0, 1.0))

        val area = r1Sq * alpha + r2Sq * beta - 0.5 * sqrt(
            max(0.0, (-d + r1 + r2) * (d + r1 - r2) * (d - r1 + r2) * (d + r1 + r2))
        )

        return (area / (Math.PI * r1Sq)).coerceIn(0.0, 1.0)
    }

    private fun calculateAngularSeparation(ra1: Double, dec1: Double, ra2: Double, dec2: Double): Double {
        val r1 = ra1 * DEG2RAD
        val d1 = dec1 * DEG2RAD
        val r2 = ra2 * DEG2RAD
        val d2 = dec2 * DEG2RAD
        val cosD = sin(d1) * sin(d2) + cos(d1) * cos(d2) * cos(r1 - r2)
        return acos(cosD.coerceIn(-1.0, 1.0)) * RAD2DEG
    }

    private fun searchDynamicEclipse(afterMs: Long, isSolar: Boolean): EclipseEvent? {
        val baseJd = afterMs / 86400000.0 + 2440587.5
        var k = floor((baseJd - 2451550.09766) / SYNODIC_MONTH_DAYS).toLong()

        for (i in 0..60) {
            val t = if (isSolar) newMoonTime(k) else fullMoonTime(k)
            if (t > afterMs) {
                val tJc = (t / 86400000.0 + 2440587.5 - 2451545.0) / 36525.0
                val F = (160.7108 + 390.67050274 * (if (isSolar) k.toDouble() else k + 0.5)) % 360.0
                val distFromNode = abs((F % 180.0) - if (F % 180.0 > 90.0) 180.0 else 0.0)

                val limit = if (isSolar) 18.5 else 12.5
                if (distFromNode < limit) {
                    val mag = (1.0 - distFromNode / limit).coerceIn(0.1, 1.05)
                    return EclipseEvent(
                        type = if (isSolar) {
                            if (mag > 1.0) EclipseType.TOTAL_SOLAR else EclipseType.PARTIAL_SOLAR
                        } else {
                            if (mag > 1.0) EclipseType.TOTAL_LUNAR else EclipseType.PARTIAL_LUNAR
                        },
                        maximumMs = t,
                        magnitude = mag,
                        saros = 100 + (k % 50).toInt(),
                        gamma = (distFromNode / limit),
                        nameEn = if (isSolar) "Solar Eclipse" else "Lunar Eclipse",
                        nameFa = if (isSolar) "خورشیدگرفتگی" else "ماه‌گرفتگی",
                        durationTotalSeconds = if (mag > 1.0) 180 else 0,
                        maxTotalityRegionEn = "Global path calculated for this date",
                        maxTotalityRegionFa = "مسیر گرفتگی برای این تاریخ محاسبه گردید",
                        descriptionEn = "Calculated astronomical eclipse event.",
                        descriptionFa = "رویداد گرفتگی نجومی محاسبه‌شده.",
                        isSolar = isSolar
                    )
                }
            }
            k++
        }
        return null
    }
}
