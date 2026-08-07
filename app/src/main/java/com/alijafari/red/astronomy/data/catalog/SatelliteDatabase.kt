package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.astro_engine.ISSEngine

data class SatelliteInfo(
    val id: String,
    val noradId: Int,
    val nameEn: String,
    val nameFa: String,
    val categoryEn: String,
    val categoryFa: String,
    val operatorEn: String,
    val operatorFa: String,
    val launchDate: String,
    val country: String,
    val purposeEn: String,
    val purposeFa: String,
    val altitudeKm: Double,
    val inclinationDeg: Double,
    val orbitalPeriodMins: Double,
    val speedKmh: Double,
    val statusEn: String,
    val statusFa: String,
    val tle: ISSEngine.TLEData,
    val interestingFactsEn: List<String>,
    val interestingFactsFa: List<String>
)

object SatelliteDatabase {

    val ISS = SatelliteInfo(
        id = "sat_iss",
        noradId = 25544,
        nameEn = "International Space Station (ISS)",
        nameFa = "ایستگاه فضایی بین‌المللی (ISS)",
        categoryEn = "Space Station / Laboratory",
        categoryFa = "ایستگاه فضایی / آزمایشگاه مداری",
        operatorEn = "NASA / Roscosmos / ESA / JAXA / CSA",
        operatorFa = "ناسا / روسکاسموس / آژانس فضایی اروپا / جاکسا / کانادا",
        launchDate = "1998-11-20",
        country = "International Partnership",
        purposeEn = "Microgravity and space environment research laboratory.",
        purposeFa = "آزمایشگاه تحقیقاتی گرانش ناچیز و محیط فضا.",
        altitudeKm = 420.0,
        inclinationDeg = 51.64,
        orbitalPeriodMins = 92.9,
        speedKmh = 27600.0,
        statusEn = "Operational (Occupied continuously since Nov 2000)",
        statusFa = "فعال (دارای سرنشین مداوم از سال ۲۰۰۰)",
        tle = ISSEngine.TLEData(
            name = "ISS (ZARYA)",
            line1 = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9993",
            line2 = "2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400001"
        ),
        interestingFactsEn = listOf(
            "Orbits the Earth approximately 16 times every 24 hours.",
            "Traverses the distance from the Earth to the Moon and back in less than a day.",
            "Has hosted astronauts from over 20 different countries since November 2000.",
            "Pressurized module volume is comparable to a 6-bedroom house.",
            "Solar arrays span 73 meters, making it brighter in the night sky than Venus at its peak."
        ),
        interestingFactsFa = listOf(
            "هر ۲۴ ساعت تقریباً ۱۶ بار به دور کره زمین می‌چرخد.",
            "فاصله زمین تا ماه را در کمتر از یک روز طی می‌کند.",
            "از سال ۲۰۰۰ تاکنون میزبان فضانوردانی از بیش از ۲۰ کشور مختلف بوده است.",
            "حجم بخش‌های تحت فشار آن معادل یک خانه ۶ خوابه بزرگ است.",
            "صفحات خورشیدی آن ۷۳ متر وسعت دارند و آن را درخشان‌تر از سیاره زهره می‌سازند."
        )
    )

    val STARLINK_TRAIN = SatelliteInfo(
        id = "sat_starlink_train",
        noradId = 44713,
        nameEn = "Starlink Train (Launch Group)",
        nameFa = "قطار ماهواره‌ای استارلینک (گروه پرتاب)",
        categoryEn = "Mega-Constellation Group",
        categoryFa = "مگاکنستلیشن ارتباطی",
        operatorEn = "SpaceX",
        operatorFa = "اسپیس‌ایکس (SpaceX)",
        launchDate = "2019–Present",
        country = "United States",
        purposeEn = "Low Earth Orbit broadband internet constellation deployment.",
        purposeFa = "پوشش اینترنت پهن‌باند جهانی از مدار پایین زمین.",
        altitudeKm = 550.0,
        inclinationDeg = 53.05,
        orbitalPeriodMins = 95.6,
        speedKmh = 27300.0,
        statusEn = "Active Orbit Raising / Orbit Train Phase",
        statusFa = "فعال (فاز صف‌بندی پس از پرتاب)",
        tle = ISSEngine.TLEData(
            name = "STARLINK TRAIN",
            line1 = "1 44713U 19074A   26213.50000000  .00012000  00000-0  15000-3 0  9991",
            line2 = "2 44713  53.0500 180.0000 0001500  45.0000 315.0000 15.06000000100001"
        ),
        interestingFactsEn = listOf(
            "Shortly after launch, dozens of Starlink satellites line up in a glowing 'train' in the sky.",
            "Equipped with autonomous ion thrusters powered by krypton or argon gas.",
            "Uses optical laser space-links to route data between satellites in orbit.",
            "Designed with custom visors and dark coatings to minimize reflectivity for astronomers.",
            "Re-enters Earth atmosphere and burns up completely at the end of its operational lifecycle."
        ),
        interestingFactsFa = listOf(
            "کمی پس از پرتاب، ده‌ها ماهواره استارلینک به صورت خطی مانند قطاری نورانی در آسمان دیده می‌شوند.",
            "مجهز به پیشران‌های یون خودکار با سوخت گاز کریپتون یا آرگون هستند.",
            "از لیزرهای نوری فضایی برای انتقال داده‌ها بین ماهواره‌ها در مدار استفاده می‌کنند.",
            "دارای آفتابگیرهای خاص برای کاهش انعکاس نور و جلوگیری از اختلال در رصدهای نجومی است.",
            "در پایان عمر مداری، به طور کامل در جو زمین می‌سوزد و هیچ زباله مداری باقی نمی‌گذارد."
        )
    )

    val STARLINK_SINGLE = SatelliteInfo(
        id = "sat_starlink_1007",
        noradId = 44714,
        nameEn = "Starlink-1007",
        nameFa = "استارلینک-۱۰۰۷",
        categoryEn = "Communications Satellite",
        categoryFa = "ماهواره مخابراتی",
        operatorEn = "SpaceX",
        operatorFa = "اسپیس‌ایکس",
        launchDate = "2019-11-11",
        country = "United States",
        purposeEn = "Low-latency satellite internet routing.",
        purposeFa = "مسیریابی اینترنت ماهواره‌ای با تاخیر بسیار کم.",
        altitudeKm = 540.0,
        inclinationDeg = 53.0,
        orbitalPeriodMins = 95.4,
        speedKmh = 27320.0,
        statusEn = "Operational in Shell 1",
        statusFa = "فعال در لایه اول مداری",
        tle = ISSEngine.TLEData(
            name = "STARLINK-1007",
            line1 = "1 44714U 19074B   26213.50000000  .00008000  00000-0  10000-3 0  9992",
            line2 = "2 44714  53.0000 210.0000 0001200  80.0000 280.0000 15.09000000100002"
        ),
        interestingFactsEn = listOf(
            "Flies in a flat-panel design packed tightly inside Falcon 9 fairings during launch.",
            "Maintains automated collision-avoidance maneuver tracking using US Space Force tracking.",
            "Operates at an altitude where natural atmospheric drag cleans up decommissioned units in years.",
            "Employs phased-array antennas to track ground stations seamlessly without moving parts.",
            "Provides high-speed low-Earth-orbit connectivity to remote and maritime regions."
        ),
        interestingFactsFa = listOf(
            "دارای طراحی تخت است تا ده‌ها فروند از آن به صورت فشرده در موشک فالکون ۹ قرار گیرند.",
            "دارای سیستم خودکار اجتناب از برخورد بر اساس داده‌های شبکه مراقبت فضایی است.",
            "در ارتفاعی پرواز می‌کند که پسکشش جوی پس از چند سال آن را به صورت طبیعی پاکسازی می‌کند.",
            "از آنتن‌های آرایه فازی بدون قطعات متحرک برای دنبال کردن ایستگاه‌های زمینی استفاده می‌کند.",
            "ارتباط پرسرعت را برای دورافتاده‌ترین مناطق زمین و اقیانوس‌ها فراهم می‌سازد."
        )
    )

    val HUBBLE = SatelliteInfo(
        id = "sat_hubble",
        noradId = 20580,
        nameEn = "Hubble Space Telescope (HST)",
        nameFa = "تلسکوپ فضایی هابل (HST)",
        categoryEn = "Space Observatory",
        categoryFa = "رصدخانه فضایی مداری",
        operatorEn = "NASA / ESA",
        operatorFa = "ناسا / آژانس فضایی اروپا",
        launchDate = "1990-04-24",
        country = "United States / Europe",
        purposeEn = "Deep space astronomical observation across optical, UV, and near-IR wavelengths.",
        purposeFa = "رصد ژرفای کیهان در طول موج‌های فرابنفش، مرئی و فروسرخ نزدیک.",
        altitudeKm = 535.0,
        inclinationDeg = 28.47,
        orbitalPeriodMins = 95.2,
        speedKmh = 27300.0,
        statusEn = "Operational (Serviced 5 times by Space Shuttle)",
        statusFa = "فعال (۵ بار توسط شاتل فضایی سرویس‌دهی شده است)",
        tle = ISSEngine.TLEData(
            name = "HST",
            line1 = "1 20580U 90037B   26213.50000000  .00001200  00000-0  50000-4 0  9998",
            line2 = "2 20580  28.4700 120.0000 0002800 150.0000 210.0000 15.10000000300001"
        ),
        interestingFactsEn = listOf(
            "Has made over 1.5 million astronomical observations since its launch in 1990.",
            "Its primary mirror is 2.4 meters in diameter, polished to a precision within 10 nanometers.",
            "Has no thrusters; uses reaction wheels and gyroscopes to point with sub-arcsecond accuracy.",
            "Helped determine the expansion rate of the universe (Hubble Constant) and age of cosmos.",
            "Discovered that dark energy is accelerating the cosmic expansion rate."
        ),
        interestingFactsFa = listOf(
            "از زمان پرتاب در سال ۱۹۹۰ بیش از ۱.۵ میلیون رصد نجومی انجام داده است.",
            "آینه اصلی آن ۲.۴ متر قطر دارد و با دقت ۱۰ نانومتر صیقل داده شده است.",
            "هیچ موتور پیشرانی ندارد و تنها با چرخ‌های عکس‌العملی با دقت بی‌نظیر نشانه‌گیری می‌کند.",
            "نقش کلیدی در تعیین نرخ انبساط کیهان (ثابت هابل) و سن ۱۴ میلیارد ساله جهان داشت.",
            "کشف نمود که انرژی تاریک موجب شتاب‌گرفتن نرخ انبساط کیهان می‌شود."
        )
    )

    val JWST = SatelliteInfo(
        id = "sat_jwst",
        noradId = 50463,
        nameEn = "James Webb Space Telescope (JWST)",
        nameFa = "تلسکوپ فضایی جیمز وب (JWST)",
        categoryEn = "Infrared Space Telescope",
        categoryFa = "رصدخانه فروسرخ پیشرفته",
        operatorEn = "NASA / ESA / CSA",
        operatorFa = "ناسا / آژانس فضایی اروپا / آژانس فضایی کانادا",
        launchDate = "2021-12-25",
        country = "International Collaboration",
        purposeEn = "Observing the first galaxies after Big Bang and exoplanet atmospheres.",
        purposeFa = "رصد نخستین کهکشان‌های کیهان و مطالعه جو سیارات فراخورشیدی.",
        altitudeKm = 1500000.0, // Sun-Earth L2
        inclinationDeg = 28.5,
        orbitalPeriodMins = 8760.0 * 60, // Halo orbit
        speedKmh = 1000.0,
        statusEn = "Operational at Sun-Earth L2 point",
        statusFa = "فعال در نقطه لاگرانژی L2 (۱.۵ میلیون کیلومتری زمین)",
        tle = ISSEngine.TLEData(
            name = "JWST (L2)",
            line1 = "1 50463U 21130A   26213.50000000  .00000001  00000-0  00000-0 0  9999",
            line2 = "2 50463  28.5000   5.0000 0010000   0.0000   0.0000  0.00100000100001"
        ),
        interestingFactsEn = listOf(
            "Stationed 1.5 million kilometers away from Earth at the Lagrange Point 2 (L2).",
            "Features a 6.5-meter gold-coated primary mirror made of 18 hexagonal beryllium segments.",
            "Protected by a 5-layer tennis-court-sized sunshield maintaining a -233°C cold side.",
            "Can detect the thermal signature of a bumblebee on the Moon from Earth.",
            "Unfolded itself autonomously in deep space during a 30-day complex deployment sequence."
        ),
        interestingFactsFa = listOf(
            "در نقطه لاگرانژی L2 در فاصله ۱.۵ میلیون کیلومتری (۴ برابر فاصله ماه) مستقر است.",
            "دارای آینه ۶.۵ متری با روکش طلا شامل ۱۸ بخش شش‌ضلعی از جنس بریلیوم است.",
            "سایه‌بان ۵ لایه آن به اندازه زمین تنیس، دمای تلسکوپ را در منفی ۲۳۳ درجه سانتی‌گراد نگه می‌دارد.",
            "قدرت آن به حدی است که می‌تواند امضای گرمایی یک زنبور را روی سطح ماه تشخیص دهد.",
            "طی یک فرآیند پیچیده ۳۰ روزه تمام بخش‌هایش خودکار در ژرفای فضا باز شدند."
        )
    )

    val ONEWEB = SatelliteInfo(
        id = "sat_oneweb",
        noradId = 44057,
        nameEn = "OneWeb-0128",
        nameFa = "وان‌وب-۰۱۲۸",
        categoryEn = "Polar Orbit Internet Constellation",
        categoryFa = "منظومه اینترنت قطبی",
        operatorEn = "Eutelsat OneWeb",
        operatorFa = "یوتل‌ست وان‌وب",
        launchDate = "2020-02-06",
        country = "United Kingdom / France",
        purposeEn = "Polar orbit global broadband coverage.",
        purposeFa = "پوشش سراسری اینترنت پهن‌باند در مدارهای قطبی.",
        altitudeKm = 1200.0,
        inclinationDeg = 87.4,
        orbitalPeriodMins = 109.0,
        speedKmh = 26100.0,
        statusEn = "Operational in Polar Shell",
        statusFa = "فعال در مدار قطبی",
        tle = ISSEngine.TLEData(
            name = "ONEWEB-0128",
            line1 = "1 44057U 19009A   26213.50000000  .00000500  00000-0  20000-4 0  9995",
            line2 = "2 44057  87.4000  45.0000 0001000  90.0000 270.0000 13.18000000100001"
        ),
        interestingFactsEn = listOf(
            "Operates at 1,200 km altitude, higher than Starlink, providing wider geographic footprint.",
            "Near-polar 87.4° inclination ensures complete coverage over the Arctic and Antarctica.",
            "Designed with electric Hall-effect ion thrusters for orbit maintenance.",
            "Each satellite weighs under 150 kg and delivers multi-gigabit throughput.",
            "Partners with maritime and aviation sectors for continuous global flight tracking."
        ),
        interestingFactsFa = listOf(
            "در ارتفاع ۱۲۰۰ کیلومتری (بالاتر از استارلینک) با پوشش جغرافیایی بسیار گسترده‌تر پرواز می‌کند.",
            "شیب مداری ۸۷.۴ درجه‌ای آن پوشش کامل قطب شمال و جنوب را تضمین می‌کند.",
            "از پیشران‌های یونی اثر هال برای تثبیت موقعیت مداری استفاده می‌کند.",
            "هر ماهواره کمتر از ۱۵۰ کیلوگرم وزن داشته و پهنای باند چندگیگابیتی ارائه می‌دهد.",
            "با خطوط هوایی و کشتیرانی برای ردیابی زنده پروازها در تمام کره زمین همکاری دارد."
        )
    )

    val NOAA19 = SatelliteInfo(
        id = "sat_noaa19",
        noradId = 33591,
        nameEn = "NOAA 19 (Weather)",
        nameFa = "هواشناسی NOAA 19",
        categoryEn = "Sun-Synchronous Meteorological",
        categoryFa = "ماهواره هواشناسی همگام با خورشید",
        operatorEn = "NOAA / NASA",
        operatorFa = "سازمان ملی اقیانوسی و جوی آمریکا (NOAA)",
        launchDate = "2009-02-06",
        country = "United States",
        purposeEn = "Weather forecasting, severe storm tracking, climate monitoring.",
        purposeFa = "پیش‌بینی هواشناسی، ردیابی طوفان‌ها و پایش اقلیم کره زمین.",
        altitudeKm = 850.0,
        inclinationDeg = 98.7,
        orbitalPeriodMins = 102.1,
        speedKmh = 26700.0,
        statusEn = "Operational (Transmits live HRPT weather imagery)",
        statusFa = "فعال (مخابره زنده تصاویر هواشناسی HRPT)",
        tle = ISSEngine.TLEData(
            name = "NOAA 19",
            line1 = "1 33591U 09005A   26213.50000000  .00000150  00000-0  30000-4 0  9996",
            line2 = "2 33591  98.7000  90.0000 0014000 120.0000 240.0000 14.11000000100001"
        ),
        interestingFactsEn = listOf(
            "Flies in a Sun-synchronous polar orbit, passing over every location at the same local time.",
            "Transmits unencrypted analog and digital weather radio signals accessible by amateur antennas.",
            "Carries search and rescue receivers (Cospas-Sarsat) that have saved over 40,000 lives globally.",
            "Measures vertical atmospheric temperature and moisture profiles around the globe.",
            "Continues providing climate data nearly two decades after its launch."
        ),
        interestingFactsFa = listOf(
            "در مدار قطبی همگام با خورشید پرواز کرده و در زمان محلی یکسانی از هر نقطه زمین می‌گذرد.",
            "سیگنال‌های تصویربرداری رادیویی آن توسط آنتن‌های دست‌ساز آماتوری قابل دریافت است.",
            "گیرنده‌های امداد و نجات آن تاکنون جان بیش از ۴۰,۰۰۰ نفر را در سراسر جهان نجات داده‌اند.",
            "پروفایل‌های عمودی دما و رطوبت جوی را در کل کره زمین اندازه‌گیری می‌کند.",
            "پس از نزدیک به دو دهه همچنان داده‌های باارزش اقلیمی به زمین ارسال می‌کند."
        )
    )

    val GOES16 = SatelliteInfo(
        id = "sat_goes16",
        noradId = 41866,
        nameEn = "GOES 16 (GOES-East)",
        nameFa = "ماهواره پیشرفته هواشناسی GOES-16",
        categoryEn = "Geostationary Environmental Satellite",
        categoryFa = "ماهواره زمین‌ثابت پایش محیط زیست",
        operatorEn = "NOAA / NASA",
        operatorFa = "NOAA / ناسا",
        launchDate = "2016-11-19",
        country = "United States",
        purposeEn = "Real-time geostationary weather imagery and lightning mapping.",
        purposeFa = "تصویربرداری زنده زمین‌ثابت هواشناسی و نقشه صاعقه‌های جوی.",
        altitudeKm = 35786.0,
        inclinationDeg = 0.03,
        orbitalPeriodMins = 1436.1, // 23h 56m
        speedKmh = 11070.0,
        statusEn = "Operational at 75.2° W longitude",
        statusFa = "فعال در مدار زمین‌ثابت (نقطه ۷۵.۲ درجه غربی)",
        tle = ISSEngine.TLEData(
            name = "GOES 16",
            line1 = "1 41866U 16071A   26213.50000000  .00000010  00000-0  00000-0 0  9997",
            line2 = "2 41866   0.0300 280.0000 0001000  10.0000 350.0000  1.00270000100001"
        ),
        interestingFactsEn = listOf(
            "Stationed in geostationary orbit so it appears fixed over the Western Hemisphere.",
            "Scans the entire Western Hemisphere every 10 minutes and severe storm zones every 30 seconds.",
            "Carries the first Geostationary Lightning Mapper (GLM) to detect in-cloud and ground lightning.",
            "Monitors solar flares and coronal mass ejections that trigger geomagnetic space weather storms.",
            "Provides ultra-high-resolution multispectral imagery across 16 optical and infrared channels."
        ),
        interestingFactsFa = listOf(
            "در مدار زمین‌ثابت قرار دارد و نسبت به زمین کاملاً ثابت به نظر می‌رسد.",
            "کل نیمکره غربی را هر ۱۰ دقیقه و مناطق طوفانی شدید را هر ۳۰ ثانیه اسکن می‌کند.",
            "نخستین نقشه‌بردار زمین‌ثابت صاعقه (GLM) را برای ثبت لحظه‌ای رعدوبرق‌ها حمل می‌کند.",
            "شراره‌های خورشیدی و طوفان‌های مغناطیسی فضایی را پایش و پیش‌بینی می‌کند.",
            "تصاویر با وضوح بسیار بالا را در ۱۶ کانال نوری و فروسرخ مخابره می‌کند."
        )
    )

    fun getAllSatellites(): List<SatelliteInfo> {
        return listOf(
            ISS,
            STARLINK_TRAIN,
            STARLINK_SINGLE,
            HUBBLE,
            JWST,
            ONEWEB,
            NOAA19,
            GOES16
        )
    }

    fun getSatelliteById(id: String): SatelliteInfo {
        return getAllSatellites().find { it.id == id } ?: ISS
    }
}
