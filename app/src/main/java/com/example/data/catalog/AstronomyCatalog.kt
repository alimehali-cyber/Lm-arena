package com.example.data.catalog

import com.example.astro_engine.MoonEngine
import com.example.astro_engine.PlanetEngine
import com.example.astro_engine.SunEngine
import com.example.astro_engine.TimeEngine
import com.example.domain.CelestialObject
import com.example.domain.ConstellationData
import com.example.domain.ObjectType

object AstronomyCatalog {

    val SUN = CelestialObject(
        id = "sun_sol",
        type = ObjectType.SUN,
        nameEn = "Sun (Sol)",
        nameFa = "خورشید",
        raDeg = 60.0,
        decDeg = 20.0,
        magnitude = -26.7,
        constellationEn = "Solar System",
        constellationFa = "منظومه شمسی",
        distanceLightYears = 0.0000158,
        category = "Star / Yellow Dwarf",
        descriptionEn = "The star at the center of our Solar System, providing light and heat to Earth.",
        descriptionFa = "ستاره مرکزی منظومه شمسی که نور و گرمابخش حیات روی زمین است.",
        observationTipEn = "WARNING: Never look directly at the Sun without certified solar filters!",
        observationTipFa = "هشدار: هرگز بدون فیلتر مخصوص خورشیدی مستقیماً به خورشید نگاه نکنید!"
    )

    val MOON = CelestialObject(
        id = "moon_luna",
        type = ObjectType.MOON,
        nameEn = "Moon (Luna)",
        nameFa = "ماه",
        raDeg = 180.0,
        decDeg = 10.0,
        magnitude = -12.7,
        constellationEn = "Earth Satellite",
        constellationFa = "قمر زمین",
        distanceLightYears = 0.00000004,
        category = "Natural Satellite",
        descriptionEn = "Earth's only natural satellite, causing ocean tides and moon phases.",
        descriptionFa = "تنها قمر طبیعی زمین که عامل جزر و مد و فازهای ماهیانه است.",
        observationTipEn = "Best viewed near the terminator line during crescent or quarter phase.",
        observationTipFa = "بهترین رصد گودال‌ها در مرز سایه‌روشن هلال یا تربیع ماه است."
    )

    val ISS = CelestialObject(
        id = "sat_iss",
        type = ObjectType.SATELLITE,
        nameEn = "International Space Station (ISS)",
        nameFa = "ایستگاه فضایی بین‌المللی (ISS)",
        raDeg = 180.0,
        decDeg = 35.0,
        magnitude = -3.2,
        constellationEn = "Low Earth Orbit",
        constellationFa = "مدار زمین",
        distanceLightYears = 0.00000004,
        category = "Habitable Space Station",
        descriptionEn = "Habitable artificial satellite orbiting Earth every 90 minutes at 28,000 km/h.",
        descriptionFa = "ایستگاه فضایی سرنشین‌دار در حال چرخش به دور زمین هر ۹۰ دقیقه با سرعت ۲۸,۰۰۰ کیلومتر بر ساعت.",
        observationTipEn = "Appears as a fast-moving unblinking bright point across twilight skies.",
        observationTipFa = "مانند یک نقطه بسیار پرنور بدون چشمک‌زدن با سرعت بالای افق حرکت می‌کند."
    )

    val MILKY_WAY = CelestialObject(
        id = "galaxy_milky_way",
        type = ObjectType.DEEP_SKY,
        nameEn = "Milky Way Galaxy Center",
        nameFa = "مرکز کهکشان راه شیری",
        raDeg = 266.4,
        decDeg = -29.0,
        magnitude = -5.0,
        constellationEn = "Sagittarius",
        constellationFa = "کمان (قوس)",
        distanceLightYears = 26000.0,
        category = "Barred Spiral Galaxy",
        descriptionEn = "Our home spiral galaxy containing 100-400 billion stars and Sagittarius A*.",
        descriptionFa = "کهکشان خانگی ما شامل ۱۰۰ تا ۴۰۰ میلیارد ستاره و سیاهچاله کلان‌جرم مرکزی.",
        observationTipEn = "Visible as a luminous glowing arch across dark unpolluted summer skies.",
        observationTipFa = "نوار پهن و درخشان نورانی در شب‌های تابستان و مناطق کویری تاریک."
    )

    val ANDROMEDA = CelestialObject(
        id = "galaxy_andromeda_m31",
        type = ObjectType.DEEP_SKY,
        nameEn = "Andromeda Galaxy (M31)",
        nameFa = "کهکشان آندرومدا (M31)",
        raDeg = 10.684,
        decDeg = 41.269,
        magnitude = 3.44,
        constellationEn = "Andromeda",
        constellationFa = "زن بر زنجیر (آندرومدا)",
        distanceLightYears = 2500000.0,
        category = "Spiral Galaxy",
        descriptionEn = "The nearest major galaxy to the Milky Way, containing over 1 trillion stars.",
        descriptionFa = "نزدیک‌ترین کهکشان بزرگ به کهکشان راه شیری با بیش از ۱۰۰۰ میلیارد ستاره.",
        observationTipEn = "Faint oval cloud visible with naked eye in dark skies; bright in binoculars.",
        observationTipFa = "بیضی محو نورانی با چشم غیرمسلح در تاریکی شب و فوق‌العاده در دوربین دوچشمی."
    )

    val ORION_NEBULA = CelestialObject(
        id = "nebula_orion_m42",
        type = ObjectType.DEEP_SKY,
        nameEn = "Orion Nebula (M42)",
        nameFa = "سحابی جبار (M42)",
        raDeg = 83.822,
        decDeg = -5.391,
        magnitude = 4.0,
        constellationEn = "Orion",
        constellationFa = "شکارچی (جبار)",
        distanceLightYears = 1344.0,
        category = "Emission Nebula",
        descriptionEn = "Vibrant star-forming region glowing in Orion's sword.",
        descriptionFa = "زایشگاه عظیم ستارگان با درخشش گازهای هیدروژنی در شمشیر جبار.",
        observationTipEn = "Glowing greenish-white cloud with Trapezium star cluster at core.",
        observationTipFa = "توده ابر نورانی سبز-سفید با خوشه چهارتایی ستارگان جوان در مرکز آن."
    )

    val PLEIADES = CelestialObject(
        id = "cluster_pleiades_m45",
        type = ObjectType.DEEP_SKY,
        nameEn = "Pleiades Cluster (M45 / Seven Sisters)",
        nameFa = "خوشه پروین / ثریا (M45)",
        raDeg = 56.87,
        decDeg = 24.105,
        magnitude = 1.6,
        constellationEn = "Taurus",
        constellationFa = "گاو (ثور)",
        distanceLightYears = 444.0,
        category = "Open Star Cluster",
        descriptionEn = "Famous open cluster of hot blue luminous stars enveloped in reflection nebulosity.",
        descriptionFa = "خوشه باز و شناخته‌شده از ستارگان آبی داغ و جوان پوشیده در سحابی انعکاسی.",
        observationTipEn = "Resembles a tiny miniature dipper; best seen in binoculars.",
        observationTipFa = "شبیه یک ملاقه مینیاتوری کوچک است که با دوربین دوچشمی جلوه شگفت‌انگیزی دارد."
    )

    val SIRIUS = CelestialObject(
        id = "star_sirius",
        type = ObjectType.STAR,
        nameEn = "Sirius (Dog Star)",
        nameFa = "شباهنگ / شعرای یمانی (Sirius)",
        raDeg = 101.287,
        decDeg = -16.716,
        magnitude = -1.46,
        constellationEn = "Canis Major",
        constellationFa = "سگ بزرگ (کلب اکبر)",
        distanceLightYears = 8.6,
        category = "Binary Star System",
        descriptionEn = "The brightest star in Earth's night sky, a white main-sequence star with a white dwarf companion.",
        descriptionFa = "درخشان‌ترین ستاره در آسمان شب زمین با درخشش سفید-آبی خیره‌کننده.",
        observationTipEn = "Flashes brilliant multicolors when low near horizon due to atmospheric refraction.",
        observationTipFa = "در ارتفاعات پایین افق به دلیل شکست نور جوی مثل الماس رنگارنگ می‌درخشد."
    )

    val VEGA = CelestialObject(
        id = "star_vega",
        type = ObjectType.STAR,
        nameEn = "Vega",
        nameFa = "نسر واقع / ونوشا (Vega)",
        raDeg = 279.234,
        decDeg = 38.783,
        magnitude = 0.03,
        constellationEn = "Lyra",
        constellationFa = "دیگ‌پایه (شلیاق)",
        distanceLightYears = 25.04,
        category = "Main Sequence Star",
        descriptionEn = "Bright blue-white star defining the zero baseline for stellar magnitude scale.",
        descriptionFa = "ستاره آبی-سفید درخشان و رأس مثلث تابستانی که مبنای صفر قدر ظاهری است.",
        observationTipEn = "High overhead near zenith throughout Northern Hemisphere summer nights.",
        observationTipFa = "در شب‌های تابستان درست بالای سر (سمت‌الرأس) در آسمان ایران می‌درخشد."
    )

    val BETELGEUSE = CelestialObject(
        id = "star_betelgeuse",
        type = ObjectType.STAR,
        nameEn = "Betelgeuse",
        nameFa = "ابط‌الجوزا / شبان‌شانه (Betelgeuse)",
        raDeg = 88.793,
        decDeg = 7.407,
        magnitude = 0.50,
        constellationEn = "Orion",
        constellationFa = "شکارچی (جبار)",
        distanceLightYears = 642.5,
        category = "Red Supergiant",
        descriptionEn = "Pulsating red supergiant candidate for a dramatic supernova explosion.",
        descriptionFa = "ابرغول سرخ غول‌پیکر در شانه جبار که آماده انفجار ابرنواختری است.",
        observationTipEn = "Deep orange-red color contrast against surrounding blue stars.",
        observationTipFa = "رنگ نارنجی-سرخ عمیق آن در مقایسه با سایر ستارگان جبار چشمگیر است."
    )

    val POLARIS = CelestialObject(
        id = "star_polaris",
        type = ObjectType.STAR,
        nameEn = "Polaris (North Star)",
        nameFa = "ستاره قطبی (Polaris)",
        raDeg = 37.95,
        decDeg = 89.264,
        magnitude = 1.98,
        constellationEn = "Ursa Minor",
        constellationFa = "خرس کوچک (دب اصغر)",
        distanceLightYears = 433.0,
        category = "Multiple Cepheid Variable",
        descriptionEn = "Northern pole star located nearly aligned with Earth's rotational axis.",
        descriptionFa = "ستاره قطب شمال آسمان که جهت شمال جغرافیایی دقیق را نشان می‌دهد.",
        observationTipEn = "Altitude equals exact geographical latitude of observation site.",
        observationTipFa = "ارتفاع زاویه‌ای آن از افق دقیقاً برابر با عرض جغرافیایی محل رصد شماست."
    )

    val PLANETS_BASE = listOf(
        CelestialObject(
            id = "planet_mercury",
            type = ObjectType.PLANET,
            nameEn = "Mercury",
            nameFa = "تیر (عطارد)",
            raDeg = 120.0,
            decDeg = 18.0,
            magnitude = -0.4,
            constellationEn = "Gemini",
            constellationFa = "دوپیکر (جوزا)",
            distanceLightYears = 0.000015,
            category = "Terrestrial Planet",
            descriptionEn = "Smallest planet in the Solar System, closest to the Sun.",
            descriptionFa = "کوچک‌ترین و نزدیک‌ترین سیاره منظومه شمسی به خورشید.",
            observationTipEn = "Look low on the horizon right after sunset or before sunrise.",
            observationTipFa = "بلافاصله پس از غروب آفتاب یا قبل از طلوع خورشید در افق پایین رصد کنید."
        ),
        CelestialObject(
            id = "planet_venus",
            type = ObjectType.PLANET,
            nameEn = "Venus",
            nameFa = "ناهید (زهره)",
            raDeg = 145.0,
            decDeg = 12.0,
            magnitude = -4.4,
            constellationEn = "Leo",
            constellationFa = "شیر (اسد)",
            distanceLightYears = 0.000007,
            category = "Terrestrial Planet",
            descriptionEn = "Brightest planet in Earth's sky, covered in reflective clouds.",
            descriptionFa = "درخشان‌ترین سیاره آسمان شب پوشیده از ابرهای انعکاسی اسید سولفوریک.",
            observationTipEn = "Extremely bright diamond-like beacon in dusk or dawn.",
            observationTipFa = "مانند الماسی خیره‌کننده در شفق شامگاهی یا طلوع صبحگاهی می‌درخشد."
        ),
        CelestialObject(
            id = "planet_earth",
            type = ObjectType.PLANET,
            nameEn = "Earth (Terra)",
            nameFa = "زمین",
            raDeg = 0.0,
            decDeg = 0.0,
            magnitude = -27.0,
            constellationEn = "Solar System",
            constellationFa = "منظومه شمسی",
            distanceLightYears = 0.0,
            category = "Terrestrial Planet",
            descriptionEn = "Our home planet, the only known world harboring life.",
            descriptionFa = "سیاره مادری ما، تنها جهان شناخته‌شده با حیات و اقیانوس‌های آب مایع.",
            observationTipEn = "Look down! Earth is right beneath your feet.",
            observationTipFa = "به زیر پای خود نگاه کنید! زمین درست تحت رصد شماست."
        ),
        CelestialObject(
            id = "planet_mars",
            type = ObjectType.PLANET,
            nameEn = "Mars",
            nameFa = "بهرام (مریخ)",
            raDeg = 65.0,
            decDeg = 22.0,
            magnitude = -1.2,
            constellationEn = "Taurus",
            constellationFa = "گاو (ثور)",
            distanceLightYears = 0.000018,
            category = "Terrestrial Planet",
            descriptionEn = "The Red Planet with dusty iron-oxide surface and Olympus Mons.",
            descriptionFa = "سیاره سرخ‌رنگ با کوه‌های آتشفشانی غول‌پیکر و خاک حاوی اکسید آهن.",
            observationTipEn = "Distinct reddish hue easily noticed with naked eye.",
            observationTipFa = "رنگ مایل به سرخ متمایز آن با چشم غیرمسلح کاملاً هویداست."
        ),
        CelestialObject(
            id = "planet_jupiter",
            type = ObjectType.PLANET,
            nameEn = "Jupiter",
            nameFa = "هرمز (مشتری)",
            raDeg = 42.0,
            decDeg = 15.0,
            magnitude = -2.6,
            constellationEn = "Aries",
            constellationFa = "بره (حمل)",
            distanceLightYears = 0.000082,
            category = "Gas Giant",
            descriptionEn = "Largest gas giant in Solar System with the Great Red Spot and 4 Galilean moons.",
            descriptionFa = "بزرگ‌ترین غول گازی منظومه شمسی با لکه سرخ بزرگ و ۴ قمر معروف گالیله‌ای.",
            observationTipEn = "Io, Europa, Ganymede and Callisto moons easily visible in small binoculars.",
            observationTipFa = "۴ قمر گالیله‌ای آن حتی با یک دوربین دوچشمی کوچک قابل مشاهده‌اند."
        ),
        CelestialObject(
            id = "planet_saturn",
            type = ObjectType.PLANET,
            nameEn = "Saturn",
            nameFa = "کیوان (زحل)",
            raDeg = 340.0,
            decDeg = -10.0,
            magnitude = 0.3,
            constellationEn = "Aquarius",
            constellationFa = "دلو",
            distanceLightYears = 0.000150,
            category = "Gas Giant",
            descriptionEn = "The Lord of the Rings with spectacular system of ice rings and Titan moon.",
            descriptionFa = "ارباب حلقه‌های منظومه شمسی با سامانه تماشایی حلقه‌های یخی.",
            observationTipEn = "Rings easily resolved with any basic telescope (25x magnification).",
            observationTipFa = "حلقه‌های تماشایی آن با هر تلسکوپ آماتوری تفکیک می‌شوند."
        ),
        CelestialObject(
            id = "planet_uranus",
            type = ObjectType.PLANET,
            nameEn = "Uranus",
            nameFa = "اورانوس",
            raDeg = 48.0,
            decDeg = 16.0,
            magnitude = 5.7,
            constellationEn = "Taurus",
            constellationFa = "گاو (ثور)",
            distanceLightYears = 0.000300,
            category = "Ice Giant",
            descriptionEn = "An ice giant planet with a striking cyan-blue atmosphere tilted on its side.",
            descriptionFa = "غول یخی با جو فیروزه‌ای-آبی که انحراف محوری عجیب ۹۸ درجه‌ای دارد.",
            observationTipEn = "Faint greenish disk visible in small telescopes and binoculars.",
            observationTipFa = "قرص سبز-آبی کم‌نور با تلسکوپ‌های کوچک و دوربین قابل تشخیص است."
        ),
        CelestialObject(
            id = "planet_neptune",
            type = ObjectType.PLANET,
            nameEn = "Neptune",
            nameFa = "نپتون",
            raDeg = 350.0,
            decDeg = -4.0,
            magnitude = 7.8,
            constellationEn = "Pisces",
            constellationFa = "ماهی (حوت)",
            distanceLightYears = 0.000470,
            category = "Ice Giant",
            descriptionEn = "The outermost major planet, deep blue ice giant with supersonic winds.",
            descriptionFa = "دورترین سیاره اصلی منظومه شمسی، غول یخی لاجوردی با بادهای مافوق صوت.",
            observationTipEn = "Requires binoculars or small telescope to resolve as a tiny blue star.",
            observationTipFa = "برای رصد آن به عنوان یک ستاره آبی کوچک نیازمند دوربین دوچشمی یا تلسکوپ هستید."
        ),
        CelestialObject(
            id = "planet_pluto",
            type = ObjectType.PLANET,
            nameEn = "Pluto",
            nameFa = "پلوتو",
            raDeg = 301.0,
            decDeg = -22.0,
            magnitude = 15.1,
            constellationEn = "Capricornus",
            constellationFa = "بزغاله (جدی)",
            distanceLightYears = 0.000620,
            category = "Dwarf Planet",
            descriptionEn = "Famous dwarf planet in the Kuiper Belt with heart-shaped nitrogen glacier Tombaugh Regio.",
            descriptionFa = "سیاره کوتوله معروف کمربند کایپر با یخچال نیتروژنی قلبی شکل تومبا.",
            observationTipEn = "Requires a large aperture telescope (10\"+) and a precise star chart.",
            observationTipFa = "برای رصد آن به تلسکوپ با دهانه بزرگ (۱۰ اینچ به بالا) و نقشه دقیق نیاز دارید."
        )
    )

    val IRAN_CITIES = listOf(
        Triple("Nurabad City (NC)", "نورآباد ممسنی (NC)", 30.1141 to 51.5217),
        Triple("Tehran", "تهران", 35.6892 to 51.3890),
        Triple("Shiraz", "شیراز", 29.5918 to 52.5837),
        Triple("Isfahan", "اصفهان", 32.6546 to 51.6680),
        Triple("Tabriz", "تبریز", 38.0962 to 46.2694),
        Triple("Mashhad", "مشهد", 36.2972 to 59.6067),
        Triple("Kerman", "کرمان", 30.2839 to 57.0834),
        Triple("Ahvaz", "اهواز", 31.3183 to 48.6706),
        Triple("Rasht", "رشت", 37.2808 to 49.5832),
        Triple("Yazd", "یزد", 31.8974 to 54.3675),
        Triple("Kermanshah", "کرمانشاه", 34.3142 to 47.0650),
        Triple("Hamadan", "همدان", 34.7982 to 48.5146),
        Triple("Zahedan", "زاهدان", 29.4963 to 60.8629),
        Triple("Bandar Abbas", "بندرعباس", 27.1832 to 56.2666),
        Triple("Sanandaj", "سنندج", 35.3144 to 46.9923),
        Triple("Bushehr", "بوشهر", 28.9234 to 50.8382)
    )

    /**
     * Dynamically calculates astronomical-grade ephemeris positions for Sun, Moon, and Planets
     * at the given Julian Date [jd], returning exact real-time RA/Dec and Magnitudes.
     */
    fun getAllObjects(jd: Double = TimeEngine.getJulianDate()): List<CelestialObject> {
        val sunPos = SunEngine.calculatePosition(jd)
        val dynamicSun = SUN.copy(raDeg = sunPos.raDeg, decDeg = sunPos.decDeg)

        val moonData = MoonEngine.calculateMoon(jd)
        val dynamicMoon = MOON.copy(raDeg = moonData.raDeg, decDeg = moonData.decDeg)

        val planetMap = mapOf(
            "planet_mercury" to PlanetEngine.PlanetType.MERCURY,
            "planet_venus" to PlanetEngine.PlanetType.VENUS,
            "planet_mars" to PlanetEngine.PlanetType.MARS,
            "planet_jupiter" to PlanetEngine.PlanetType.JUPITER,
            "planet_saturn" to PlanetEngine.PlanetType.SATURN,
            "planet_uranus" to PlanetEngine.PlanetType.URANUS,
            "planet_neptune" to PlanetEngine.PlanetType.NEPTUNE,
            "planet_pluto" to PlanetEngine.PlanetType.PLUTO
        )

        val dynamicPlanets = PLANETS_BASE.map { p ->
            val pType = planetMap[p.id]
            if (pType != null) {
                val pPos = PlanetEngine.calculatePlanet(pType, jd)
                p.copy(raDeg = pPos.raDeg, decDeg = pPos.decDeg, magnitude = pPos.magnitude)
            } else {
                p
            }
        }

        val starsAndDeepSky = listOf(
            MILKY_WAY, ANDROMEDA, ORION_NEBULA, PLEIADES,
            SIRIUS, VEGA, BETELGEUSE, POLARIS
        )

        return listOf(dynamicSun, dynamicMoon, ISS) + dynamicPlanets + starsAndDeepSky
    }

    val DEFAULT_CONSTELLATIONS = listOf(
        ConstellationData(
            code = "ORI",
            nameEn = "Orion",
            nameFa = "صورت فلکی جبار (شکارچی)",
            latinName = "Orion",
            mainStars = listOf(
                88.79 to 7.40,   // Betelgeuse
                78.63 to -8.20,  // Rigel
                81.28 to 6.35,   // Bellatrix
                86.93 to -9.67,  // Saiph
                84.05 to -1.94,  // Mintaka
                84.53 to -1.20,  // Alnilam
                85.19 to -1.98   // Alnitak
            )
        ),
        ConstellationData(
            code = "UMA",
            nameEn = "Ursa Major",
            nameFa = "خرس بزرگ (دب اکبر)",
            latinName = "Ursa Major",
            mainStars = listOf(
                165.93 to 61.75, // Dubhe
                165.46 to 56.38, // Merak
                178.46 to 53.70, // Phecda
                183.14 to 57.03, // Megrez
                193.51 to 55.96, // Alioth
                206.88 to 54.92, // Mizar
                209.80 to 49.31  // Alkaid
            )
        ),
        ConstellationData(
            code = "CAS",
            nameEn = "Cassiopeia",
            nameFa = "ذات‌الکرسی (خداوند اورنگ)",
            latinName = "Cassiopeia",
            mainStars = listOf(
                9.88 to 59.15,   // Schedar
                1.15 to 59.15,   // Caph
                14.18 to 60.72,  // Gamma Cas
                21.45 to 60.23,  // Ruchbah
                28.60 to 63.67   // Segin
            )
        ),
        ConstellationData(
            code = "CYG",
            nameEn = "Cygnus",
            nameFa = "ماکیان (قو)",
            latinName = "Cygnus",
            mainStars = listOf(
                310.36 to 45.28, // Deneb
                292.68 to 27.96, // Albireo
                305.56 to 40.26, // Sadr
                296.24 to 33.97, // Gienah
                311.37 to 51.73  // Delta Cyg
            )
        ),
        ConstellationData(
            code = "SCO",
            nameEn = "Scorpius",
            nameFa = "عقرب (کژدم)",
            latinName = "Scorpius",
            mainStars = listOf(
                247.35 to -26.43, // Antares
                240.28 to -22.62, // Graffias
                241.36 to -19.80, // Dschubba
                252.17 to -37.10, // Sargas
                262.69 to -37.03  // Shaula
            )
        )
    )
}
