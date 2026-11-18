package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData
import java.util.concurrent.ConcurrentHashMap

enum class SatelliteCategory(val labelEn: String, val labelFa: String) {
    ALL("All", "همه"),
    ISS("ISS", "ایستگاه فضایی"),
    STARLINK("Starlink", "استارلینک"),
    HUBBLE("Hubble", "هابل"),
    VISIBLE("Naked-eye candidates", "قابل مشاهده با چشم"),
    TIANGONG("Tiangong", "تیانگونگ"),
    EARTH_OBSERVATION("Earth observation", "سنجش از دور")
}

data class SatelliteItem(
    val id: String,
    val noradId: Int,
    val nameEn: String,
    val nameFa: String,
    val category: SatelliteCategory = SatelliteCategory.VISIBLE,
    val designation: String = "",
    val defaultTle: TLEData,
    val isConstellation: Boolean = false,
    val isTrain: Boolean = false,
    val trainCount: Int = 1,
    val starlinkTrainCount: Int = 1,
    val standardMagnitude: Double = 2.0,
    val descriptionEn: String = "",
    val descriptionFa: String = "",
    val isNakedEyeCandidate: Boolean = true,
    val launchDate: String = "",
    val operatorEn: String = "",
    val operatorFa: String = "",
    val missionPurposeEn: String = "",
    val missionPurposeFa: String = "",
    val scientificSignificanceEn: String = "",
    val scientificSignificanceFa: String = "",
    val verifiedFactsEn: List<String> = emptyList(),
    val verifiedFactsFa: List<String> = emptyList()
)

object SatelliteCatalog {

    /**
     * 4 naked-eye static satellites.
     * Dynamic Starlink train is added via [getVisibleList].
     *
     * The bundled `defaultTle` element sets are checksum-verified orbital element sets
     * for epoch 2026 day 317.5 (2026-11-13, mid-November 2026). They are strictly the last-resort
     * offline fallback: TleRepository replaces them with live elements whenever the network is
     * available, and both the Satellites and AR screens surface the element age as a TLE ages.
     */
    val satellites: List<SatelliteItem> = listOf(
        SatelliteItem(
            id = "iss_zarya",
            noradId = 25544,
            nameEn = "ISS (International Space Station)",
            nameFa = "ایستگاه فضایی بین‌المللی",
            category = SatelliteCategory.ISS,
            designation = "1998-067A",
            defaultTle = TLEData(
                name = "ISS (ZARYA)",
                line1 = "1 25544U 98067A   26317.50000000  .00005580  00000+0  10980-3 0  9992",
                line2 = "2 25544  51.6317 281.2275 0006755  11.7355 180.0110 15.48879905594899"
            ),
            isConstellation = false,
            isTrain = false,
            trainCount = 1,
            starlinkTrainCount = 1,
            standardMagnitude = -3.8,
            descriptionEn = "The largest artificial body in orbit, observable with the naked eye as a bright gliding star.",
            descriptionFa = "بزرگ‌ترین سازه ساخت بشر در فضا که با چشم غیرمسلح مانند ستاره‌ای بسیار درخشان و روان دیده می‌شود.",
            isNakedEyeCandidate = true,
            launchDate = "November 20, 1998",
            operatorEn = "NASA / Roscosmos / ESA / JAXA / CSA",
            operatorFa = "ناسا / روسکاسموس / آژانس فضایی اروپا / جاکسا / کانادا",
            missionPurposeEn = "Permanent human habitated microgravity research laboratory orbiting Earth.",
            missionPurposeFa = "آزمایشگاه تحقیقاتی ریزگرانش بین‌المللی با حضور دائمی فضانوردان.",
            scientificSignificanceEn = "The largest structure built in space, facilitating continuous scientific breakthroughs in biology, physics, and astronomy since November 2000.",
            scientificSignificanceFa = "بزرگ‌ترین سازه ساخت بشر خارج از کره زمین که از نوامبر ۲۰۰۰ به طور پیوسته میزبان انسان‌ها و آزمایش‌های پیشرو بوده است.",
            verifiedFactsEn = listOf(
                "Orbits Earth every 92.6 minutes at a speed of approximately 27,600 km/h (7.66 km/s).",
                "Solar array wings span 73 meters wide, generating up to 120 kilowatts of electricity.",
                "Shines at magnitude -3.8, often outshining every celestial object except the Sun and Moon."
            ),
            verifiedFactsFa = listOf(
                "هر ۹۲.۶ دقیقه یک بار با سرعت ۲۷۶۰۰ کیلومتر بر ساعت زمین را دور می‌زند.",
                "عرض پنل‌های خورشیدی آن ۷۳ متر است و تا ۱۲۰ کیلووات برق تولید می‌کند.",
                "با درخشندگی قدر ۳.۸- روشن‌تر از تمام ستارگان و سیارات شبانه دیده می‌شود."
            )
        ),
        SatelliteItem(
            id = "tiangong",
            noradId = 48274,
            nameEn = "Tiangong Space Station (CSS)",
            nameFa = "ایستگاه فضایی تیانگونگ (چین)",
            category = SatelliteCategory.TIANGONG,
            designation = "2021-035A",
            defaultTle = TLEData(
                name = "CSS (TIANGONG)",
                line1 = "1 48274U 21035A   26317.50000000  .00024884  00000+0  29331-3 0  9996",
                line2 = "2 48274  41.4694 134.9133 0000855   1.1253 156.7530 15.60787511316489"
            ),
            isConstellation = false,
            isTrain = false,
            trainCount = 1,
            starlinkTrainCount = 1,
            standardMagnitude = -1.5,
            descriptionEn = "China's permanently crewed space station orbiting at ~390 km, bright and easily visible to the naked eye.",
            descriptionFa = "ایستگاه فضایی سرنشین‌دار چین در ارتفاع ۳۹۰ کیلومتری، بسیار درخشان و به راحتی با چشم غیرمسلح قابل رؤیت.",
            isNakedEyeCandidate = true,
            launchDate = "April 29, 2021 (Tianhe Module)",
            operatorEn = "CNSA (China National Space Administration)",
            operatorFa = "آژانس فضایی ملی چین (CNSA)",
            missionPurposeEn = "Long-term modular human orbital outpost and microgravity laboratory.",
            missionPurposeFa = "ایستگاه مداری سرنشین‌دار و آزمایشگاه دائمی فضایی.",
            scientificSignificanceEn = "China's third-generation modular space station, permanently occupied by crews of 3 taikonauts.",
            scientificSignificanceFa = "ایستگاه فضایی ماژولار نسل سوم چین با استقرار دائمی فضانوردان و قابلیت توسعه.",
            verifiedFactsEn = listOf(
                "Features a T-shape layout consisting of Tianhe core module, Wentian, and Mengtian labs.",
                "Orbits at ~390 km altitude at 41.5-degree orbital inclination.",
                "Reaches magnitude -1.5, easily spotted as a bright golden moving point in dusk and dawn."
            ),
            verifiedFactsFa = listOf(
                "دارای ساختار T-شکل شامل ماژول اصلی تیانهه و آزمایشگاه‌های ونتیان و منگتیان.",
                "در ارتفاع ۳۹۰ کیلومتری با زاویه میل ۴۱.۵ درجه به دور زمین در گردش است.",
                "با قدر ۱.۵- به صورت یک نقطه طلایی پرنور در آسمان شب به راحتی دیده می‌شود."
            )
        ),
        SatelliteItem(
            id = "hubble",
            noradId = 20580,
            nameEn = "Hubble Space Telescope (HST)",
            nameFa = "تلسکوپ فضایی هابل",
            category = SatelliteCategory.HUBBLE,
            designation = "1990-037B",
            defaultTle = TLEData(
                name = "HST",
                line1 = "1 20580U 90037B   26317.50000000  .00006511  00000+0  19755-3 0  9990",
                line2 = "2 20580  28.4739 139.4744 0001989  24.3470 118.7352 15.31953866811474"
            ),
            isConstellation = false,
            isTrain = false,
            trainCount = 1,
            starlinkTrainCount = 1,
            standardMagnitude = 2.0,
            descriptionEn = "Legendary optical space telescope orbiting Earth at ~525 km altitude with 28.5° inclination.",
            descriptionFa = "تلسکوپ فضایی افسانه‌ای هابل در ارتفاع ۵۲۵ کیلومتری زمین با زاویه میل مداری ۲۸.۵ درجه.",
            isNakedEyeCandidate = true,
            launchDate = "April 24, 1990",
            operatorEn = "NASA / ESA",
            operatorFa = "ناسا / آژانس فضایی اروپا",
            missionPurposeEn = "Deep-space optical, ultraviolet, and near-infrared astronomy.",
            missionPurposeFa = "رصد عمیق کیهان در طیف‌های مرئی، فرابنفش و فروسرخ نزدیک.",
            scientificSignificanceEn = "Revolutionized astrophysics by confirming the accelerating expansion rate of the Universe and determining cosmic age (~13.8 billion years).",
            scientificSignificanceFa = "تحول بنیادین در کیهان‌شناسی، اثبات شتاب انبساط جهان و تعیین دقیق سن کیهان (۱۳.۸ میلیارد سال).",
            verifiedFactsEn = listOf(
                "Features a 2.4-meter primary glass mirror precision-polished to within 10 nanometers.",
                "Serviced 5 times in orbit by NASA Space Shuttle astronaut crews between 1993 and 2009.",
                "Has completed over 1.5 million scientific observations since its launch."
            ),
            verifiedFactsFa = listOf(
                "دارای آینه اصلی ۲.۴ متری با دقت صیقل‌کاری فوق‌العاده ۱۰ نانومتر.",
                "۵ بار توسط فضانوردان شاتل فضایی در مدار زمین تعمیر و ارتقا داده شد.",
                "تاکنون بیش از ۱.۵ میلیون رصد علمی ارزشمند انجام داده است."
            )
        ),
        SatelliteItem(
            id = "envisat",
            noradId = 27386,
            nameEn = "Envisat / Earth Observation Sat",
            nameFa = "ماهواره سنجش از دور انویسات",
            category = SatelliteCategory.EARTH_OBSERVATION,
            designation = "2002-009A",
            defaultTle = TLEData(
                name = "ENVISAT",
                line1 = "1 27386U 02009A   26317.50000000  .00000084  00000+0  41076-4 0  9994",
                line2 = "2 27386  98.3982 265.6466 0001335   1.9023 182.9019 14.39088056294963"
            ),
            isConstellation = false,
            isTrain = false,
            trainCount = 1,
            starlinkTrainCount = 1,
            standardMagnitude = 1.8,
            descriptionEn = "Massive 8-ton European environmental satellite in polar Sun-synchronous orbit at 760 km.",
            descriptionFa = "ماهواره بزرگ ۸ تنی زیست‌محیطی اروپا در مدار قطبی همگام با خورشید در ارتفاع ۷۶۰ کیلومتری.",
            isNakedEyeCandidate = true,
            launchDate = "March 1, 2002",
            operatorEn = "ESA (European Space Agency)",
            operatorFa = "آژانس فضایی اروپا (ESA)",
            missionPurposeEn = "Earth observation, global environmental, oceanographic, and atmospheric monitoring.",
            missionPurposeFa = "سنجش از دور، پایش تغییرات اقلیمی، اقیانوس‌شناسی و جو کره زمین.",
            scientificSignificanceEn = "The largest civilian Earth observation satellite ever built (8.2 metric tons, 25 meters long).",
            scientificSignificanceFa = "بزرگ‌ترین ماهواره غیرنظامی پایش زمین به وزن ۸.۲ تن و طول ۲۵ متر.",
            verifiedFactsEn = listOf(
                "Carried 10 advanced optical and radar Earth-monitoring instruments.",
                "Provided crucial 10-year environmental dataset before communications ended in 2012.",
                "Orbits in a Sun-synchronous polar orbit at 760 km altitude."
            ),
            verifiedFactsFa = listOf(
                "حامل ۱۰ ابزار سنجش راداری و نوری پیشرفته برای پایش دقیق محیط زیست.",
                "ارائه‌دهنده داده‌های حیاتی اقلیمی در ۱۰ سال فعالیت تا خروج از دسترس در سال ۲۰۱۲.",
                "در مدار قطبی همگام با خورشید در ارتفاع ۷۶۰ کیلومتری قرار دارد."
            )
        )
    )

    private val dynamicStarlinksById = ConcurrentHashMap<String, SatelliteItem>()
    private val dynamicStarlinksByNorad = ConcurrentHashMap<Int, SatelliteItem>()

    /**
     * Registers dynamically discovered Starlink trains and shortlisted visible/overhead
     * individual Starlinks so [getById] and [SatelliteEngine.resolveSatelliteItem] resolve
     * them without touching the immutable static [satellites] list.
     */
    fun clearPrimaryTrain() {
        dynamicStarlinksById.remove("starlink_train")
    }

    fun registerDynamicStarlinks(items: Collection<SatelliteItem>, primaryTrain: SatelliteItem? = null) {
        if (primaryTrain != null) {
            dynamicStarlinksById["starlink_train"] = primaryTrain
            dynamicStarlinksById[primaryTrain.id.lowercase()] = primaryTrain
            if (primaryTrain.noradId > 0) {
                dynamicStarlinksByNorad.putIfAbsent(primaryTrain.noradId, primaryTrain)
            }
        }
        for (item in items) {
            dynamicStarlinksById[item.id.lowercase()] = item
            if (item.noradId > 0) {
                // Individual satellite item takes precedence for direct NORAD lookups unless train-only
                if (!item.isTrain || !dynamicStarlinksByNorad.containsKey(item.noradId)) {
                    dynamicStarlinksByNorad[item.noradId] = item
                }
                dynamicStarlinksById["sat_${item.noradId}"] = item
                dynamicStarlinksById["starlink_${item.noradId}"] = item
            }
        }
    }

    fun getDynamicByIdOrNorad(idOrNorad: String): SatelliteItem? {
        val key = idOrNorad.trim().lowercase()
        if (key.isEmpty()) return null
        dynamicStarlinksById[key]?.let { return it }
        val norad = when {
            key.startsWith("sat_") -> key.removePrefix("sat_").toIntOrNull()
            key.startsWith("starlink_") -> key.removePrefix("starlink_").toIntOrNull()
            key.startsWith("norad_") -> key.removePrefix("norad_").toIntOrNull()
            else -> key.toIntOrNull()
        }
        return if (norad != null) dynamicStarlinksByNorad[norad] else null
    }

    fun getVisibleList(train: SatelliteItem? = null): List<SatelliteItem> {
        return if (train != null) {
            satellites + train
        } else {
            satellites
        }
    }

    fun getById(id: String, train: SatelliteItem? = null): SatelliteItem {
        if (train != null && (id == "starlink_train" || id == train.id)) {
            return train
        }
        val staticMatch = satellites.find {
            it.id == id ||
            (id == "tiangong_space_station" && it.id == "tiangong") ||
            (id == "hubble_space_telescope" && it.id == "hubble") ||
            (id == "cosmos_1457" && it.id == "envisat")
        }
        if (staticMatch != null) return staticMatch

        getDynamicByIdOrNorad(id)?.let { return it }

        return satellites.first()
    }
}
