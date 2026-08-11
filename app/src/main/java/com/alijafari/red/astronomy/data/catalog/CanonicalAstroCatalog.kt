package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.astro_engine.SatelliteCatalog
import com.alijafari.red.astronomy.domain.CanonicalAstroObject
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObservationalInfo
import com.alijafari.red.astronomy.domain.ObjectType
import com.alijafari.red.astronomy.domain.PhysicalProperties
import com.alijafari.red.astronomy.domain.ScientificIdentifiers
import com.alijafari.red.astronomy.domain.StaticPosition

/**
 * Authoritative Canonical Astronomy Catalog for RED Astronomy.
 *
 * Serves as the single source of truth for astronomical object identity, metadata, localized names,
 * parent/child relationships, and legacy ID resolutions.
 */
object CanonicalAstroCatalog {

    // --- 1. SOLAR SYSTEM CANONICAL OBJECTS ---

    val SUN = CanonicalAstroObject(
        canonicalId = "sun",
        legacyIds = listOf("sun_main", "sun_sol"),
        type = ObjectType.SUN,
        nameEn = "Sun (Sol)",
        nameFa = "خورشید (مهر / هور)",
        searchAliasesEn = listOf("sol", "sun", "solar"),
        searchAliasesFa = listOf("خورشید", "مهر", "سولارس", "افتاب"),
        childIds = listOf(
            "planet_mercury", "planet_venus", "planet_earth", "planet_mars",
            "planet_jupiter", "planet_saturn", "planet_uranus", "planet_neptune", "planet_pluto"
        ),
        physicalProperties = PhysicalProperties(
            magnitude = -26.74,
            diameterKm = 1392700.0,
            massKg = 1.989e30,
            surfaceGravityMS2 = 274.0,
            temperatureK = 5778,
            rotationPeriodHours = 600.0,
            diameterDisplayEn = "1,392,700 km (109× Earth)",
            diameterDisplayFa = "۱,۳۹۲,۷۰۰ کیلومتر (۱۰۹ برابر زمین)",
            massDisplayEn = "1.989 × 10³⁰ kg (333,000× Earth)",
            massDisplayFa = "۱.۹۸۹ × ۱۰³⁰ کیلوگرم (۳۳۳,۰۰۰ برابر زمین)",
            gravityDisplayEn = "274.0 m/s² (28.0× Earth)",
            gravityDisplayFa = "۲۷۴.۰ متر بر مجذور ثانیه (۲۸.۰ برابر زمین)",
            distanceDisplayEn = "149,600,000 km | 1.0 AU",
            distanceDisplayFa = "۱۴۹,۶۰۰,۰۰۰ کیلومتر (۱ واحد نجومی)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "G-type Main-Sequence Star (Yellow Dwarf)",
            categoryFa = "ستاره رشته اصلی نوع G (کوتوله زرد)",
            descriptionEn = "The central star of the Solar System, containing 99.86% of all system mass.",
            descriptionFa = "ستاره مرکزی منظومه‌شمسی که ۹۹.۸۶ درصد کل جرم منظومه را در خود جای داده است.",
            observationTipEn = "WARNING: Never view the Sun directly without proper certified solar filters!",
            observationTipFa = "هشدار: هرگز بدون فیلتر مجهز و استاندارد خورشیدی مستقیماً به خورشید نگاه نکنید!"
        )
    )

    val EARTH = CanonicalAstroObject(
        canonicalId = "planet_earth",
        legacyIds = listOf("earth"),
        type = ObjectType.PLANET,
        nameEn = "Earth (Terra)",
        nameFa = "زمین (کره خاک)",
        searchAliasesEn = listOf("earth", "terra", "home planet"),
        searchAliasesFa = listOf("زمین", "گیتی", "سیاره مادری"),
        parentId = "sun",
        childIds = listOf("moon", "sat_25544", "sat_20580", "sat_48274", "sat_25989", "sat_33591", "sat_36508", "sat_20624", "sat_40069"),
        physicalProperties = PhysicalProperties(
            magnitude = -3.8,
            diameterKm = 12742.0,
            massKg = 5.972e24,
            surfaceGravityMS2 = 9.81,
            temperatureK = 288,
            rotationPeriodHours = 23.93,
            orbitalPeriodDays = 365.256,
            diameterDisplayEn = "12,742 km (1.00× Earth)",
            diameterDisplayFa = "۱۲,۷۴۲ کیلومتر (۱.۰۰ برابر زمین)",
            massDisplayEn = "5.972 × 10²⁴ kg (1.00× Earth)",
            massDisplayFa = "۵.۹۷۲ × ۱۰²⁴ کیلوگرم (۱.۰۰ برابر زمین)",
            gravityDisplayEn = "9.81 m/s² (1.00× Earth)",
            gravityDisplayFa = "۹.۸۱ متر بر مجذور ثانیه (۱.۰۰ برابر زمین)",
            distanceDisplayEn = "0 km (Right beneath you)",
            distanceDisplayFa = "۰ کیلومتر (زیر پای شما)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Terrestrial Planet (Home)",
            categoryFa = "سیاره خاکی (خانه ما)",
            descriptionEn = "Our home planet, third planet from the Sun and starting origin for celestial observations.",
            descriptionFa = "سیاره مادری ما، سومین سیاره منظومه شمسی و مبدأ مرجع رصدها و سفرهای فضایی.",
            observationTipEn = "Origin reference for all astronomical calculations.",
            observationTipFa = "مبدأ مرجع تمامی محاسبات نجومی و سفرهای فضایی."
        )
    )

    val MOON = CanonicalAstroObject(
        canonicalId = "moon",
        legacyIds = listOf("moon_main", "moon_luna"),
        type = ObjectType.MOON,
        nameEn = "Moon (Luna)",
        nameFa = "ماه (لونا / باختر)",
        searchAliasesEn = listOf("moon", "luna", "selene"),
        searchAliasesFa = listOf("ماه", "لونا", "قمر زمین"),
        parentId = "planet_earth",
        physicalProperties = PhysicalProperties(
            magnitude = -12.74,
            diameterKm = 3474.8,
            massKg = 7.342e22,
            surfaceGravityMS2 = 1.62,
            temperatureK = 250,
            rotationPeriodHours = 655.7,
            orbitalPeriodDays = 27.32,
            diameterDisplayEn = "3,475 km (0.273× Earth)",
            diameterDisplayFa = "۳,۴۷۵ کیلومتر (۰.۲۷۳ برابر زمین)",
            massDisplayEn = "7.342 × 10²² kg (0.0123× Earth)",
            massDisplayFa = "۷.۳۴۲ × ۱۰²² کیلوگرم (۰.۰۱۲۳ برابر زمین)",
            gravityDisplayEn = "1.62 m/s² (0.165× Earth)",
            gravityDisplayFa = "۱.۶۲ متر بر مجذور ثانیه (۰.۱۶۵ برابر زمین)",
            distanceDisplayEn = "384,400 km | 0.00000004 light-years",
            distanceDisplayFa = "۳۸۴,۴۰۰ کیلومتر"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Natural Satellite of Earth",
            categoryFa = "تنها قمر طبیعی کره زمین",
            descriptionEn = "Earth's sole natural satellite, influencing tides and serving as a celestial clock.",
            descriptionFa = "تنها قمر طبیعی زمین، عامل ایجاد جزر و مد و درخشان‌ترین جرم شبانه آسمان.",
            observationTipEn = "Best viewed around quarter phases when shadows reveal crater depth.",
            observationTipFa = "بهترین رصد در فاز تربیع اول و دوم است که سایه‌های دهانه‌ها به زیبایی مشخص می‌شوند."
        )
    )

    val MERCURY = CanonicalAstroObject(
        canonicalId = "planet_mercury",
        type = ObjectType.PLANET,
        nameEn = "Mercury",
        nameFa = "عطارد (تیر)",
        searchAliasesEn = listOf("mercury", "hermes"),
        searchAliasesFa = listOf("عطارد", "تیر"),
        parentId = "sun",
        physicalProperties = PhysicalProperties(
            magnitude = -0.4,
            diameterKm = 4879.4,
            massKg = 3.301e23,
            surfaceGravityMS2 = 3.70,
            temperatureK = 440,
            rotationPeriodHours = 1407.6,
            orbitalPeriodDays = 87.97,
            diameterDisplayEn = "4,879 km (0.383× Earth)",
            diameterDisplayFa = "۴,۸۷۹ کیلومتر (۰.۳۸۳ برابر زمین)",
            massDisplayEn = "3.301 × 10²³ kg (0.055× Earth)",
            massDisplayFa = "۳.۳۰۱ × ۱۰²³ کیلوگرم (۰.۰۵۵ برابر زمین)",
            gravityDisplayEn = "3.70 m/s² (0.377× Earth)",
            gravityDisplayFa = "۳.۷۰ متر بر مجذور ثانیه (۰.۳۷۷ برابر زمین)",
            distanceDisplayEn = "91,700,000 km (Average from Earth)",
            distanceDisplayFa = "۹۱,۷۰۰,۰۰۰ کیلومتر (میانگین از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Terrestrial Planet",
            categoryFa = "سیاره خاکی (سنگی)",
            descriptionEn = "Innermost planet of the Solar System, heavily cratered and experiencing extreme temperature swings.",
            descriptionFa = "نزدیک‌ترین سیاره به خورشید با نوسانات دمایی شدید و سطح پر از دهانه‌های برخوردی.",
            observationTipEn = "View near maximum elongation low on twilight horizons.",
            observationTipFa = "تنها در زمان کشیدگی مداری در سپیده‌دم یا غروب آفتاب پایین افق قابل رصد است."
        )
    )

    val VENUS = CanonicalAstroObject(
        canonicalId = "planet_venus",
        type = ObjectType.PLANET,
        nameEn = "Venus",
        nameFa = "زهره (ناهید)",
        searchAliasesEn = listOf("venus", "morning star", "evening star", "aphrodite"),
        searchAliasesFa = listOf("زهره", "ناهید", "ستاره صبحگاهی", "ستاره شامگاهی"),
        parentId = "sun",
        physicalProperties = PhysicalProperties(
            magnitude = -4.4,
            diameterKm = 12103.6,
            massKg = 4.867e24,
            surfaceGravityMS2 = 8.87,
            temperatureK = 737,
            rotationPeriodHours = -5832.5,
            orbitalPeriodDays = 224.7,
            diameterDisplayEn = "12,104 km (0.949× Earth)",
            diameterDisplayFa = "۱۲,۱۰۴ کیلومتر (۰.۹۴۹ برابر زمین)",
            massDisplayEn = "4.867 × 10²⁴ kg (0.815× Earth)",
            massDisplayFa = "۴.۸۶۷ × ۱۰²⁴ کیلوگرم (۰.۸۱۵ برابر زمین)",
            gravityDisplayEn = "8.87 m/s² (0.904× Earth)",
            gravityDisplayFa = "۸.۸۷ متر بر مجذور ثانیه (۰.۹۰۴ برابر زمین)",
            distanceDisplayEn = "41,400,000 km (Closest approach to Earth)",
            distanceDisplayFa = "۴۱,۴۰۰,۰۰۰ کیلومتر (کمترین فاصله از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Terrestrial Planet (Runaway Greenhouse)",
            categoryFa = "سیاره خاکی (گلخانه‌ای شدید)",
            descriptionEn = "Brightest planet in our night sky, shrouded in dense sulfuric acid clouds and runaway greenhouse climate.",
            descriptionFa = "درخشان‌ترین سیاره آسمان شب با جو فوق‌العاده متراکم دی‌اکسید کربن و ابرهای اسید سولفوریک.",
            observationTipEn = "Exhibits crescent and gibbous phases like the Moon in small telescopes.",
            observationTipFa = "حتی در تلسکوپ‌های کوچک فازهای هلال و تثلیث آن ماننده ماه دیده می‌شود."
        )
    )

    val MARS = CanonicalAstroObject(
        canonicalId = "planet_mars",
        type = ObjectType.PLANET,
        nameEn = "Mars",
        nameFa = "مریخ (بهرام)",
        searchAliasesEn = listOf("mars", "red planet", "ares"),
        searchAliasesFa = listOf("مریخ", "بهرام", "سیاره سرخ"),
        parentId = "sun",
        childIds = listOf("mars_phobos", "mars_deimos"),
        physicalProperties = PhysicalProperties(
            magnitude = -1.5,
            diameterKm = 6779.0,
            massKg = 6.417e23,
            surfaceGravityMS2 = 3.72,
            temperatureK = 210,
            rotationPeriodHours = 24.62,
            orbitalPeriodDays = 686.98,
            diameterDisplayEn = "6,779 km (0.532× Earth)",
            diameterDisplayFa = "۶,۷۷۹ کیلومتر (۰.۵۳۲ برابر زمین)",
            massDisplayEn = "6.417 × 10²³ kg (0.107× Earth)",
            massDisplayFa = "۶.۴۱۷ × ۱۰²³ کیلوگرم (۰.۱۰۷ برابر زمین)",
            gravityDisplayEn = "3.72 m/s² (0.379× Earth)",
            gravityDisplayFa = "۳.۷۲ متر بر مجذور ثانیه (۰.۳۷۹ برابر زمین)",
            distanceDisplayEn = "78,300,000 km (Closest approach to Earth)",
            distanceDisplayFa = "۷۸,۳۰۰,۰۰۰ کیلومتر (کمترین فاصله از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Terrestrial Planet (Red Planet)",
            categoryFa = "سیاره خاکی (سیاره سرخ)",
            descriptionEn = "The Red Planet, hosting Olympus Mons and Valles Marineris with polar ice caps.",
            descriptionFa = "سیاره سرخ با آتشفشان عظیم المپوس و دره مارینر، پذیرای مریخ‌نوردهای متعدد.",
            observationTipEn = "Look for crisp red hue; polar ice caps visible during favorable opposition.",
            observationTipFa = "رنگ سرخ متمایز آن هویداست؛ در مقابله‌های مداری کلاهک‌های یخی قطبی با تلسکوپ دیده می‌شوند."
        )
    )

    val JUPITER = CanonicalAstroObject(
        canonicalId = "planet_jupiter",
        type = ObjectType.PLANET,
        nameEn = "Jupiter",
        nameFa = "مشتری (برجیس / هرمز)",
        searchAliasesEn = listOf("jupiter", "jove", "zeus"),
        searchAliasesFa = listOf("مشتری", "برجیس", "هرمز"),
        parentId = "sun",
        childIds = listOf("jup_io", "jup_europa", "jup_ganymede", "jup_callisto", "jup_elara"),
        physicalProperties = PhysicalProperties(
            magnitude = -2.7,
            diameterKm = 139820.0,
            massKg = 1.898e27,
            surfaceGravityMS2 = 24.79,
            temperatureK = 165,
            rotationPeriodHours = 9.93,
            orbitalPeriodDays = 4332.59,
            diameterDisplayEn = "139,820 km (11.21× Earth)",
            diameterDisplayFa = "۱۳۹,۸۲۰ کیلومتر (۱۱.۲۱ برابر زمین)",
            massDisplayEn = "1.898 × 10²⁷ kg (317.8× Earth)",
            massDisplayFa = "۱.۸۹۸ × ۱۰²⁷ کیلوگرم (۳۱۷.۸ برابر زمین)",
            gravityDisplayEn = "24.79 m/s² (2.53× Earth)",
            gravityDisplayFa = "۲۴.۷۹ متر بر مجذور ثانیه (۲.۵۳ برابر زمین)",
            distanceDisplayEn = "628,700,000 km (Average from Earth)",
            distanceDisplayFa = "۶۲۸,۷۰۰,۰۰۰ کیلومتر (میانگین از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Gas Giant Planet",
            categoryFa = "سیاره غول گازی",
            descriptionEn = "Largest planet in the Solar System, featuring the Great Red Spot and 95 moons.",
            descriptionFa = "بزرگ‌ترین سیاره منظومه شمسی دارای لکه سرخ بزرگ و حداقل ۹۵ قمر تاییدشده.",
            observationTipEn = "Galilean moons (Io, Europa, Ganymede, Callisto) easily visible in binoculars.",
            observationTipFa = "چهار قمر گالیله‌ای آن (ایو، اروپا، گانیمد، کالیستو) حتی با دوربین دوچشمی کوچک دیده می‌شوند."
        )
    )

    val SATURN = CanonicalAstroObject(
        canonicalId = "planet_saturn",
        type = ObjectType.PLANET,
        nameEn = "Saturn",
        nameFa = "زحل (کیوان)",
        searchAliasesEn = listOf("saturn", "cronus"),
        searchAliasesFa = listOf("زحل", "کیوان"),
        parentId = "sun",
        childIds = listOf("sat_titan", "sat_enceladus"),
        physicalProperties = PhysicalProperties(
            magnitude = 0.5,
            diameterKm = 116460.0,
            massKg = 5.683e26,
            surfaceGravityMS2 = 10.44,
            temperatureK = 134,
            rotationPeriodHours = 10.7,
            orbitalPeriodDays = 10759.22,
            diameterDisplayEn = "116,460 km (9.45× Earth)",
            diameterDisplayFa = "۱۱۶,۴۶۰ کیلومتر (۹.۴۵ برابر زمین)",
            massDisplayEn = "5.683 × 10²⁶ kg (95.2× Earth)",
            massDisplayFa = "۵.۶۸۳ × ۱۰²⁶ کیلوگرم (۹۵.۲ برابر زمین)",
            gravityDisplayEn = "10.44 m/s² (1.06× Earth)",
            gravityDisplayFa = "۱۰.۴۴ متر بر مجذور ثانیه (۱.۰۶ برابر زمین)",
            distanceDisplayEn = "1,275,000,000 km (Average from Earth)",
            distanceDisplayFa = "۱,۲۷۵,۰۰۰,۰۰۰ کیلومتر (میانگین از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Gas Giant Planet (Ringed Planet)",
            categoryFa = "سیاره غول گازی (ارباب حلقه‌ها)",
            descriptionEn = "Famous ringed gas giant planet made of ice particles and rock fragments.",
            descriptionFa = "ارباب حلقه‌های منظومه شمسی با حلقه‌های خیره‌کننده یخ و غبار و قمر شگفت‌انگیز تیتان.",
            observationTipEn = "Rings easily resolved with small telescope; Titan visible as a star-like moon.",
            observationTipFa = "حلقه‌های آن با یک تلسکوپ آماتوری تفکیک شده و قمر تیتان مانند ستاره‌ای نزدیک آن می‌درخشد."
        )
    )

    val URANUS = CanonicalAstroObject(
        canonicalId = "planet_uranus",
        type = ObjectType.PLANET,
        nameEn = "Uranus",
        nameFa = "اورانوس",
        searchAliasesEn = listOf("uranus"),
        searchAliasesFa = listOf("اورانوس"),
        parentId = "sun",
        physicalProperties = PhysicalProperties(
            magnitude = 5.7,
            diameterKm = 50724.0,
            massKg = 8.681e25,
            surfaceGravityMS2 = 8.69,
            temperatureK = 76,
            rotationPeriodHours = -17.24,
            orbitalPeriodDays = 30685.4,
            diameterDisplayEn = "50,724 km (3.98× Earth)",
            diameterDisplayFa = "۵۰,۷۲۴ کیلومتر (۳.۹۸ برابر زمین)",
            massDisplayEn = "8.681 × 10²⁵ kg (14.5× Earth)",
            massDisplayFa = "۸.۶۸۱ × ۱۰²⁵ کیلوگرم (۱۴.۵ برابر زمین)",
            gravityDisplayEn = "8.69 m/s² (0.886× Earth)",
            gravityDisplayFa = "۸.۶۹ متر بر مجذور ثانیه (۰.۸۸۶ برابر زمین)",
            distanceDisplayEn = "2,720,000,000 km (Average from Earth)",
            distanceDisplayFa = "۲,۷۲۰,۰۰۰,۰۰۰ کیلومتر (میانگین از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Ice Giant Planet",
            categoryFa = "سیاره غول یخی",
            descriptionEn = "Ice giant planet orbiting sideways on an extreme 98-degree axial tilt.",
            descriptionFa = "غول یخی با انحراف محوری ۹۸ درجه که عملاً به پهلو در مدار خود می‌چرخد.",
            observationTipEn = "Appears as a faint blue-green dot through small telescopes.",
            observationTipFa = "با تلسکوپ آماتوری مانند دیسک کوچکی به رنگ آبی-فیروزه‌ای دیده می‌شود."
        )
    )

    val NEPTUNE = CanonicalAstroObject(
        canonicalId = "planet_neptune",
        type = ObjectType.PLANET,
        nameEn = "Neptune",
        nameFa = "نپتون",
        searchAliasesEn = listOf("neptune", "poseidon"),
        searchAliasesFa = listOf("نپتون"),
        parentId = "sun",
        childIds = listOf("nep_triton"),
        physicalProperties = PhysicalProperties(
            magnitude = 7.8,
            diameterKm = 49244.0,
            massKg = 1.024e26,
            surfaceGravityMS2 = 11.15,
            temperatureK = 72,
            rotationPeriodHours = 16.11,
            orbitalPeriodDays = 60189.0,
            diameterDisplayEn = "49,244 km (3.86× Earth)",
            diameterDisplayFa = "۴۹,۲۴۴ کیلومتر (۳.۸۶ برابر زمین)",
            massDisplayEn = "1.024 × 10²⁶ kg (17.15× Earth)",
            massDisplayFa = "۱.۰۲۴ × ۱۰²۶ کیلوگرم (۱۷.۱۵ برابر زمین)",
            gravityDisplayEn = "11.15 m/s² (1.14× Earth)",
            gravityDisplayFa = "۱۱.۱۵ متر بر مجذور ثانیه (۱.۱۴ برابر زمین)",
            distanceDisplayEn = "4,350,000,000 km (Average from Earth)",
            distanceDisplayFa = "۴,۳۵۰,۰۰۰,۰۰۰ کیلومتر (میانگین از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Ice Giant Planet",
            categoryFa = "سیاره غول یخی",
            descriptionEn = "Outermost major planet in our solar system with supersonic winds exceeding 2,000 km/h.",
            descriptionFa = "دورترین سیاره اصلی منظومه شمسی دارای شدیدترین بادهای مداری با سرعت ۲۰۰۰ کیلومتر در ساعت.",
            observationTipEn = "Requires dark sky and telescope to distinguish faint deep-blue disk.",
            observationTipFa = "برای دیدن قرص آبی‌رنگ عمیق آن نیاز به تلسکوپ و آسمان کاملاً تاریک دارید."
        )
    )

    val PLUTO = CanonicalAstroObject(
        canonicalId = "planet_pluto",
        type = ObjectType.DWARF_PLANET,
        nameEn = "Pluto (Dwarf Planet)",
        nameFa = "پلوتو (سیاره کوتوله)",
        searchAliasesEn = listOf("pluto", "dwarf planet"),
        searchAliasesFa = listOf("پلوتو", "سیاره کوتوله"),
        parentId = "sun",
        physicalProperties = PhysicalProperties(
            magnitude = 14.2,
            diameterKm = 2376.6,
            massKg = 1.303e22,
            surfaceGravityMS2 = 0.62,
            temperatureK = 44,
            rotationPeriodHours = -153.3,
            orbitalPeriodDays = 90560.0,
            diameterDisplayEn = "2,377 km (0.186× Earth)",
            diameterDisplayFa = "۲,۳۷۷ کیلومتر (۰.۱۸۶ برابر زمین)",
            massDisplayEn = "1.303 × 10²² kg (0.0022× Earth)",
            massDisplayFa = "۱.۳۰۳ × ۱۰²² کیلوگرم (۰.۰۰۲۲ برابر زمین)",
            gravityDisplayEn = "0.62 m/s² (0.063× Earth)",
            gravityDisplayFa = "۰.۶۲ متر بر مجذور ثانیه (۰.۰۶۳ برابر زمین)",
            distanceDisplayEn = "5,900,000,000 km (Average from Earth)",
            distanceDisplayFa = "۵,۹۰۰,۰۰۰,۰۰۰ کیلومتر (میانگین از زمین)"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Dwarf Planet (Kuiper Belt World)",
            categoryFa = "سیاره کوتوله (جهان کمربند کایپر)",
            descriptionEn = "Famous dwarf planet in the Kuiper Belt with heart-shaped nitrogen ice glacier Tombaugh Regio.",
            descriptionFa = "سیاره کوتوله مشهور کمربند کایپر با یخچال نیتروژنی قلبی‌شکل تامبا.",
            observationTipEn = "Extremely faint; visible only in large amateur or professional telescopes.",
            observationTipFa = "بسیار کم‌نور؛ تنها با تلسکوپ‌های بزرگ رصدخانه‌ای یا ابزارهای حرفه‌ای قابل ردیابی است."
        )
    )

    // --- 2. JOVIAN MOONS (RESOLVED DUPLICATES) ---

    val IO = CanonicalAstroObject(
        canonicalId = "jup_io",
        legacyIds = listOf("jupiter_io", "io"),
        type = ObjectType.MOON,
        nameEn = "Io (Galilean Moon)",
        nameFa = "ایو (قمر مشتری)",
        parentId = "planet_jupiter",
        physicalProperties = PhysicalProperties(
            magnitude = 5.0,
            diameterKm = 3643.2,
            massKg = 8.93e22,
            surfaceGravityMS2 = 1.796,
            temperatureK = 110,
            diameterDisplayEn = "3,643 km",
            diameterDisplayFa = "۳,۶۴۳ کیلومتر",
            distanceDisplayEn = "421,700 km from Jupiter",
            distanceDisplayFa = "۴۲۱,۷۰۰ کیلومتر از مشتری"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Galilean Volcanic Moon",
            categoryFa = "قمر آتشفشانی گالیله‌ای",
            descriptionEn = "Most volcanically active body in the Solar System, erupting sulfur plumes up to 300 km high.",
            descriptionFa = "فعال‌ترین جرم آتشفشانی منظومه شمسی با فوران‌های گوگردی تا ارتفاع ۳۰۰ کیلومتر."
        )
    )

    val EUROPA = CanonicalAstroObject(
        canonicalId = "jup_europa",
        legacyIds = listOf("jupiter_europa", "europa"),
        type = ObjectType.MOON,
        nameEn = "Europa (Galilean Moon)",
        nameFa = "اروپا (قمر مشتری)",
        parentId = "planet_jupiter",
        physicalProperties = PhysicalProperties(
            magnitude = 5.3,
            diameterKm = 3121.6,
            massKg = 4.80e22,
            surfaceGravityMS2 = 1.315,
            temperatureK = 102,
            diameterDisplayEn = "3,122 km",
            diameterDisplayFa = "۳,۱۲۲ کیلومتر",
            distanceDisplayEn = "670,900 km from Jupiter",
            distanceDisplayFa = "۶۷۰,۹۰۰ کیلومتر از مشتری"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Galilean Ocean Moon",
            categoryFa = "قمر اقیانوسی گالیله‌ای",
            descriptionEn = "Smooth icy world hiding a global liquid water ocean beneath its frozen crust with high potential for habitability.",
            descriptionFa = "جهان یخی با اقیانوس آب مایع سرتاسری زیر پوسته یخی که از کاندیداهای اصلی حیات خارج از زمین است."
        )
    )

    val GANYMEDE = CanonicalAstroObject(
        canonicalId = "jup_ganymede",
        legacyIds = listOf("jupiter_ganymede", "ganymede"),
        type = ObjectType.MOON,
        nameEn = "Ganymede (Galilean Moon)",
        nameFa = "گانیمد (قمر مشتری)",
        parentId = "planet_jupiter",
        physicalProperties = PhysicalProperties(
            magnitude = 4.6,
            diameterKm = 5268.2,
            massKg = 1.48e23,
            surfaceGravityMS2 = 1.428,
            temperatureK = 110,
            diameterDisplayEn = "5,268 km (Larger than Mercury)",
            diameterDisplayFa = "۵,۲۶۸ کیلومتر (بزرگ‌تر از عطارد)",
            distanceDisplayEn = "1,070,400 km from Jupiter",
            distanceDisplayFa = "۱,۰۷۰,۴۰۰ کیلومتر از مشتری"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Galilean Moon (Largest Moon)",
            categoryFa = "بزرگ‌ترین قمر منظومه شمسی",
            descriptionEn = "Largest moon in the Solar System, larger than planet Mercury and possessing its own intrinsic magnetic field.",
            descriptionFa = "بزرگ‌ترین قمر منظومه شمسی، بزرگ‌تر از سیاره عطارد و دارای میدان مغناطیسی اختصاصی."
        )
    )

    val CALLISTO = CanonicalAstroObject(
        canonicalId = "jup_callisto",
        legacyIds = listOf("jupiter_callisto", "callisto"),
        type = ObjectType.MOON,
        nameEn = "Callisto (Galilean Moon)",
        nameFa = "کالیستو (قمر مشتری)",
        parentId = "planet_jupiter",
        physicalProperties = PhysicalProperties(
            magnitude = 5.7,
            diameterKm = 4820.6,
            massKg = 1.08e23,
            surfaceGravityMS2 = 1.235,
            temperatureK = 134,
            diameterDisplayEn = "4,821 km",
            diameterDisplayFa = "۴,۸۲۱ کیلومتر",
            distanceDisplayEn = "1,882,700 km from Jupiter",
            distanceDisplayFa = "۱,۸۸۲,۷۰۰ کیلومتر از مشتری"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Galilean Heavily Cratered Moon",
            categoryFa = "قمر کهنسال با دهانه‌های باستانی",
            descriptionEn = "Heavily cratered, ancient world in the Solar System with minimal surface geological activity.",
            descriptionFa = "یکی از قدیمی‌ترین سطوح دارنده دهانه‌های برخوردی باستان با کمترین فعالیت زمین‌شناختی."
        )
    )

    val ELARA = CanonicalAstroObject(
        canonicalId = "jup_elara",
        legacyIds = listOf("elara"),
        type = ObjectType.MOON,
        nameEn = "Elara (Irregular Jovian Moon)",
        nameFa = "الارا (قمر نامنظم مشتری)",
        parentId = "planet_jupiter",
        physicalProperties = PhysicalProperties(
            magnitude = 16.3,
            diameterKm = 86.0,
            massKg = 8.7e17,
            surfaceGravityMS2 = 0.031,
            temperatureK = 124,
            diameterDisplayEn = "86 km",
            diameterDisplayFa = "۸۶ کیلومتر",
            distanceDisplayEn = "11,740,000 km from Jupiter",
            distanceDisplayFa = "۱۱,۷۴۰,۰۰۰ کیلومتر از مشتری"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Prograde Irregular Moon",
            categoryFa = "قمر نامنظم موافق‌گرد مشتری",
            descriptionEn = "Eighth-largest moon of Jupiter, discovered by Charles Dillon Perrine in 1905.",
            descriptionFa = "هشتمین قمر بزرگ مشتری که در سال ۱۹۰۵ کشف شد."
        )
    )

    // --- 3. GALACTIC CENTER & BLACK HOLE (RESOLVED DUPLICATE) ---

    val SAGITTARIUS_A_STAR = CanonicalAstroObject(
        canonicalId = "sagittarius_a_star",
        legacyIds = listOf("galaxy_milky_way", "gal_center", "lab_sgra", "sgra"),
        type = ObjectType.BLACK_HOLE,
        nameEn = "Sagittarius A* (Milky Way Galactic Center)",
        nameFa = "سیاهچاله کمان آ* (مرکز کهکشان راه شیری)",
        searchAliasesEn = listOf("sagittarius a*", "sgr a*", "galactic center", "milky way center"),
        searchAliasesFa = listOf("کمان آ", "مرکز راه شیری", "سیاهچاله راه شیری", "مرکز کهکشان"),
        scientificIdentifiers = ScientificIdentifiers(
            constellationCode = "SGR"
        ),
        staticPosition = StaticPosition(
            raDeg = 266.416,
            decDeg = -29.007,
            distanceLightYears = 26000.0
        ),
        physicalProperties = PhysicalProperties(
            magnitude = -5.0,
            massKg = 8.2e36,
            relativisticGravitationalRatio = 0.99999,
            relativisticKinematicRatio = 0.98,
            massDisplayEn = "4.15 Million Solar Masses",
            massDisplayFa = "۴.۱۵ میلیون برابر جرم خورشید",
            distanceDisplayEn = "26,000 Light Years",
            distanceDisplayFa = "۲۶,۰۰۰ سال نوری"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Supermassive Black Hole / Galactic Nucleus",
            categoryFa = "سیاهچاله کلان‌جرم / هسته کهکشانی",
            descriptionEn = "Supermassive black hole at the dynamical center of the Milky Way galaxy.",
            descriptionFa = "سیاهچاله کلان‌جرم گرانشی در مرکز دینامیکی کهکشان راه شیری با ۴.۱۵ میلیون برابر جرم خورشید.",
            observationTipEn = "Visible as a dense glowing arch of stars across dark summer skies in Sagittarius.",
            observationTipFa = "در شب‌های تابستان به صورت نوار پرنور قوس راه شیری در صورت فلکی قوس دیده می‌شود."
        )
    )

    // --- 4. SATELLITES (RESOLVED DUPLICATE ISS) ---

    val ISS = CanonicalAstroObject(
        canonicalId = "sat_25544",
        legacyIds = listOf("sat_iss", "iss"),
        type = ObjectType.SATELLITE,
        nameEn = "International Space Station (ISS)",
        nameFa = "ایستگاه فضایی بین‌المللی (ISS)",
        searchAliasesEn = listOf("iss", "space station", "sat_iss", "zarya"),
        searchAliasesFa = listOf("ایستگاه فضایی", "ایستگاه بین المللی", "آی اس اس"),
        parentId = "planet_earth",
        scientificIdentifiers = ScientificIdentifiers(
            noradId = 25544,
            constellationCode = "LEO"
        ),
        physicalProperties = PhysicalProperties(
            magnitude = -3.2,
            diameterKm = 0.109,
            massKg = 450000.0,
            surfaceGravityMS2 = 8.7,
            orbitalPeriodDays = 0.064,
            diameterDisplayEn = "109 meters (Football field size)",
            diameterDisplayFa = "۱۰۹ متر (اندازه یک زمین فوتبال)",
            massDisplayEn = "450,000 kg (450 tonnes)",
            massDisplayFa = "۴۵۰,۰۰۰ کیلوگرم (۴۵۰ تن)",
            gravityDisplayEn = "8.7 m/s² orbital (Microgravity environment)",
            gravityDisplayFa = "۸.۷ متر بر مجذور ثانیه در مدار (بی‌وزنی ظاهری)",
            distanceDisplayEn = "420 km altitude",
            distanceDisplayFa = "۴۲۰ کیلومتر ارتفاع مداری"
        ),
        observationalInfo = ObservationalInfo(
            categoryEn = "Habitable Space Station",
            categoryFa = "ایستگاه فضایی سرنشین‌دار",
            descriptionEn = "Habitable artificial satellite orbiting Earth every 90 minutes at 28,000 km/h.",
            descriptionFa = "ایستگاه فضایی سرنشین‌دار در حال چرخش به دور زمین هر ۹۰ دقیقه با سرعت ۲۸,۰۰۰ کیلومتر بر ساعت.",
            observationTipEn = "Appears as a fast-moving unblinking bright point across twilight skies.",
            observationTipFa = "مانند یک نقطه بسیار پرنور بدون چشمک‌زدن با سرعت بالای افق حرکت می‌کند."
        )
    )

    // --- LOOKUP & RESOLUTION ENGINE ---

    private val allCanonicalObjectsList: List<CanonicalAstroObject> = listOf(
        SUN, EARTH, MOON, MERCURY, VENUS, MARS, JUPITER, SATURN, URANUS, NEPTUNE, PLUTO,
        IO, EUROPA, GANYMEDE, CALLISTO, ELARA, SAGITTARIUS_A_STAR, ISS
    )

    // Aggregates core canonical objects with mapped objects from stars, deep sky, satellites, constellations, meteor showers, and asterisms
    private val extendedCanonicalObjectsList: List<CanonicalAstroObject> by lazy {
        val list = mutableListOf<CanonicalAstroObject>()
        list.addAll(allCanonicalObjectsList)

        val coreIds = allCanonicalObjectsList.map { it.canonicalId }.toSet()

        // 1. Satellites from SatelliteCatalog
        for (sat in SatelliteCatalog.satellites) {
            val satCanonId = if (sat.noradId == 25544) "sat_25544" else "sat_${sat.noradId}"
            if (!coreIds.contains(satCanonId) && !list.any { it.canonicalId == satCanonId }) {
                list.add(
                    CanonicalAstroObject(
                        canonicalId = satCanonId,
                        legacyIds = listOf(sat.id, "sat_${sat.id}", "norad_${sat.noradId}"),
                        type = ObjectType.SATELLITE,
                        nameEn = sat.nameEn,
                        nameFa = sat.nameFa,
                        searchAliasesEn = listOf(sat.designation, "norad ${sat.noradId}", sat.category.labelEn, sat.nameEn.lowercase()),
                        searchAliasesFa = listOf(sat.category.labelFa, sat.nameFa),
                        parentId = "planet_earth",
                        scientificIdentifiers = ScientificIdentifiers(noradId = sat.noradId, constellationCode = "LEO"),
                        physicalProperties = PhysicalProperties(magnitude = sat.standardMagnitude),
                        observationalInfo = ObservationalInfo(
                            categoryEn = sat.category.labelEn,
                            categoryFa = sat.category.labelFa,
                            descriptionEn = sat.descriptionEn,
                            descriptionFa = sat.descriptionFa
                        )
                    )
                )
            }
        }

        // 2. Stars, Deep Sky, Meteor Showers, Asterisms
        val extraObjects = StarCatalog.getStars() + DeepSkyCatalog.getDeepSkyObjects() +
                MeteorShowerCatalog.getMeteorShowers() + AsterismCatalog.getAsterisms()

        for (obj in extraObjects) {
            val canonId = obj.id
            if (!coreIds.contains(canonId) && !list.any { it.canonicalId == canonId }) {
                val faWords = obj.nameFa.replace("(", " ").replace(")", " ").replace("/", " ").split(" ").map { it.trim() }.filter { it.length > 1 }
                val enWords = obj.nameEn.replace("(", " ").replace(")", " ").replace("/", " ").split(" ").map { it.trim().lowercase() }.filter { it.length > 1 }
                list.add(
                    CanonicalAstroObject(
                        canonicalId = canonId,
                        legacyIds = listOf(obj.id),
                        type = obj.type,
                        nameEn = obj.nameEn,
                        nameFa = obj.nameFa,
                        searchAliasesEn = (listOf(obj.nameEn.lowercase(), obj.constellationEn.lowercase(), obj.category.lowercase(), obj.bayerDesignation ?: "") + enWords).distinct(),
                        searchAliasesFa = (listOf(obj.nameFa, obj.constellationFa, obj.category) + faWords).distinct(),
                        scientificIdentifiers = ScientificIdentifiers(
                            constellationCode = obj.constellationEn,
                            spectralType = obj.spectralType,
                            hipId = obj.hipId,
                            hdId = obj.hdId,
                            bayerDesignation = obj.bayerDesignation,
                            flamsteedNumber = obj.flamsteedNumber
                        ),
                        staticPosition = StaticPosition(raDeg = obj.raDeg, decDeg = obj.decDeg, distanceLightYears = obj.distanceLightYears),
                        physicalProperties = PhysicalProperties(magnitude = obj.magnitude, temperatureK = obj.temperatureK),
                        observationalInfo = ObservationalInfo(
                            categoryEn = obj.category,
                            categoryFa = obj.category,
                            descriptionEn = obj.descriptionEn,
                            descriptionFa = obj.descriptionFa,
                            observationTipEn = obj.observationTipEn,
                            observationTipFa = obj.observationTipFa
                        )
                    )
                )
            }
        }

        // 3. Constellations
        for (c in ConstellationCatalog.getConstellations()) {
            val constCanonId = "const_${c.code.lowercase()}"
            if (!coreIds.contains(constCanonId) && !list.any { it.canonicalId == constCanonId }) {
                val avgRa = if (c.mainStars.isNotEmpty()) c.mainStars.map { it.first }.average() else 0.0
                val avgDec = if (c.mainStars.isNotEmpty()) c.mainStars.map { it.second }.average() else 0.0
                list.add(
                    CanonicalAstroObject(
                        canonicalId = constCanonId,
                        legacyIds = listOf(c.code.lowercase(), "constellation_${c.code.lowercase()}"),
                        type = ObjectType.CONSTELLATION,
                        nameEn = "${c.nameEn} Constellation",
                        nameFa = "صورت فلکی ${c.nameFa}",
                        searchAliasesEn = listOf(c.nameEn.lowercase(), c.code.lowercase(), c.seasonEn.lowercase()),
                        searchAliasesFa = (listOf(c.nameFa, c.seasonFa, "صورت فلکی " + c.nameFa) +
                                c.nameFa.replace("(", " ").replace(")", " ").split(" ").map { it.trim() }
                                    .filter { it.length > 1 && (c.code == "AND" && it == "آندرومدا").not() }).distinct(),
                        scientificIdentifiers = ScientificIdentifiers(constellationCode = c.code),
                        staticPosition = StaticPosition(raDeg = avgRa, decDeg = avgDec),
                        physicalProperties = PhysicalProperties(magnitude = 2.0),
                        observationalInfo = ObservationalInfo(
                            categoryEn = "Constellation (${c.seasonEn})",
                            categoryFa = "صورت فلکی (${c.seasonFa})",
                            descriptionEn = "${c.historicalInfoEn} Area: ${c.areaSqDeg} sq deg.",
                            descriptionFa = "${c.historicalInfoFa} مساحت: ${c.areaSqDeg} درجه مربع."
                        )
                    )
                )
            }
        }

        list
    }

    // Map legacy IDs and aliases to canonical object IDs
    private val legacyToCanonicalMap: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        for (obj in extendedCanonicalObjectsList) {
            map[obj.canonicalId.lowercase()] = obj.canonicalId
            for (legacyId in obj.legacyIds) {
                map[legacyId.lowercase()] = obj.canonicalId
            }
            for (aliasEn in obj.searchAliasesEn) {
                if (aliasEn.isNotBlank()) map[aliasEn.lowercase()] = obj.canonicalId
            }
            for (aliasFa in obj.searchAliasesFa) {
                if (aliasFa.isNotBlank()) map[aliasFa.lowercase()] = obj.canonicalId
            }
        }
        map
    }

    /**
     * Resolves any input ID, legacy ID, or alias to its canonical ID.
     */
    fun resolveCanonicalId(idOrLegacyId: String): String {
        val clean = idOrLegacyId.trim().lowercase()
        return legacyToCanonicalMap[clean] ?: idOrLegacyId
    }

    /**
     * Gets a CanonicalAstroObject by canonical ID or legacy ID.
     */
    fun getCanonicalObject(idOrLegacyId: String): CanonicalAstroObject? {
        val canonicalId = resolveCanonicalId(idOrLegacyId)
        return extendedCanonicalObjectsList.find { it.canonicalId == canonicalId }
    }

    /**
     * Gets all canonical objects in the master catalog.
     */
    fun getAllCanonicalObjects(): List<CanonicalAstroObject> {
        return extendedCanonicalObjectsList
    }

    /**
     * Retrieves child objects for a given parent canonical ID (e.g., moons of Jupiter).
     */
    fun getChildrenOf(parentIdOrLegacyId: String): List<CanonicalAstroObject> {
        val pId = resolveCanonicalId(parentIdOrLegacyId)
        return extendedCanonicalObjectsList.filter { it.parentId == pId }
    }

    /**
     * Converts a CanonicalAstroObject to legacy CelestialObject representation without data duplication.
     */
    fun toCelestialObject(
        canonicalObj: CanonicalAstroObject,
        dynamicRa: Double = canonicalObj.staticPosition?.raDeg ?: 0.0,
        dynamicDec: Double = canonicalObj.staticPosition?.decDeg ?: 0.0,
        dynamicMag: Double = canonicalObj.physicalProperties.magnitude
    ): CelestialObject {
        return CelestialObject(
            id = canonicalObj.canonicalId,
            type = canonicalObj.type,
            nameEn = canonicalObj.nameEn,
            nameFa = canonicalObj.nameFa,
            raDeg = dynamicRa,
            decDeg = dynamicDec,
            magnitude = dynamicMag,
            constellationEn = canonicalObj.scientificIdentifiers.constellationCode,
            constellationFa = canonicalObj.scientificIdentifiers.constellationCode,
            distanceLightYears = canonicalObj.staticPosition?.distanceLightYears ?: 0.0,
            category = canonicalObj.observationalInfo.categoryEn,
            descriptionEn = canonicalObj.observationalInfo.descriptionEn,
            descriptionFa = canonicalObj.observationalInfo.descriptionFa,
            observationTipEn = canonicalObj.observationalInfo.observationTipEn,
            observationTipFa = canonicalObj.observationalInfo.observationTipFa,
            spectralType = canonicalObj.scientificIdentifiers.spectralType,
            hipId = canonicalObj.scientificIdentifiers.hipId,
            hdId = canonicalObj.scientificIdentifiers.hdId,
            bayerDesignation = canonicalObj.scientificIdentifiers.bayerDesignation,
            flamsteedNumber = canonicalObj.scientificIdentifiers.flamsteedNumber,
            temperatureK = canonicalObj.physicalProperties.temperatureK
        )
    }

    /**
     * Integrity validation report data structure for Phase 2 verification.
     */
    data class IntegrityReport(
        val totalCanonicalObjects: Int,
        val duplicateCanonicalIds: List<String>,
        val resolvedLegacyAliasesCount: Int,
        val issResolvedCanonicalId: String,
        val sunResolvedCanonicalId: String,
        val moonResolvedCanonicalId: String,
        val sgrAResolvedCanonicalId: String,
        val ioResolvedCanonicalId: String,
        val isPassed: Boolean
    )

    /**
     * Validates that canonical IDs are 100% unique, legacy IDs resolve accurately, and no duplicate identities exist.
     */
    fun verifyIntegrity(): IntegrityReport {
        val canonicalIds = extendedCanonicalObjectsList.map { it.canonicalId }
        val duplicates = canonicalIds.groupBy { it }.filter { it.value.size > 1 }.keys.toList()

        val issRes = resolveCanonicalId("sat_iss")
        val sunRes = resolveCanonicalId("sun_sol")
        val moonRes = resolveCanonicalId("moon_main")
        val sgrARes = resolveCanonicalId("gal_center")
        val ioRes = resolveCanonicalId("jupiter_io")

        val passed = duplicates.isEmpty() &&
                issRes == "sat_25544" &&
                sunRes == "sun" &&
                moonRes == "moon" &&
                sgrARes == "sagittarius_a_star" &&
                ioRes == "jup_io"

        return IntegrityReport(
            totalCanonicalObjects = extendedCanonicalObjectsList.size,
            duplicateCanonicalIds = duplicates,
            resolvedLegacyAliasesCount = legacyToCanonicalMap.size,
            issResolvedCanonicalId = issRes,
            sunResolvedCanonicalId = sunRes,
            moonResolvedCanonicalId = moonRes,
            sgrAResolvedCanonicalId = sgrARes,
            ioResolvedCanonicalId = ioRes,
            isPassed = passed
        )
    }
}
