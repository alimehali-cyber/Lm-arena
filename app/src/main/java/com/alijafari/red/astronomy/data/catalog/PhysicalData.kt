package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.domain.CelestialObject
import kotlin.math.abs

data class PhysicalProperties(
    val diameterKm: Double,
    val diameterDisplayFa: String,
    val diameterDisplayEn: String,
    val massKgDisplayFa: String,
    val massKgDisplayEn: String,
    val gravityMssDisplayFa: String,
    val gravityMssDisplayEn: String,
    val distanceDisplayFa: String,
    val distanceDisplayEn: String
)

object PhysicalData {

    const val EARTH_DIAMETER_KM = 12742.0
    const val EARTH_MASS_KG = 5.972e24
    const val EARTH_GRAVITY_MSS = 9.81

    private val physicalMap = mapOf(
        "sun_sol" to PhysicalProperties(
            diameterKm = 1392700.0,
            diameterDisplayFa = "۱,۳۹۲,۷۰۰ کیلومتر (۱۰۹ برابر زمین)",
            diameterDisplayEn = "1,392,700 km (109× Earth)",
            massKgDisplayFa = "۱.۹۸۹ × ۱۰³⁰ کیلوگرم (۳۳۳,۰۰۰ برابر زمین)",
            massKgDisplayEn = "1.989 × 10³⁰ kg (333,000× Earth)",
            gravityMssDisplayFa = "۲۷۴.۰ متر بر مجذور ثانیه (۲۸.۰ برابر زمین)",
            gravityMssDisplayEn = "274.0 m/s² (28.0× Earth)",
            distanceDisplayFa = "۱۴۹,۶۰۰,۰۰۰ کیلومتر | ۰.۰۰۰۰۱۵۸ سال نوری (۱ واحد نجومی)",
            distanceDisplayEn = "149,600,000 km | 0.0000158 light-years (1 AU)"
        ),
        "moon_luna" to PhysicalProperties(
            diameterKm = 3474.8,
            diameterDisplayFa = "۳,۴۷۵ کیلومتر (۰.۲۷۳ برابر زمین)",
            diameterDisplayEn = "3,475 km (0.273× Earth)",
            massKgDisplayFa = "۷.۳۴۲ × ۱۰²² کیلوگرم (۰.۰۱۲۳ برابر زمین)",
            massKgDisplayEn = "7.342 × 10²² kg (0.0123× Earth)",
            gravityMssDisplayFa = "۱.۶۲ متر بر مجذور ثانیه (۰.۱۶۵ برابر زمین)",
            gravityMssDisplayEn = "1.62 m/s² (0.165× Earth)",
            distanceDisplayFa = "۳۸۴,۴۰۰ کیلومتر | ۰.۰۰۰۰۰۰۰۴ سال نوری",
            distanceDisplayEn = "384,400 km | 0.00000004 light-years"
        ),
        "sat_iss" to PhysicalProperties(
            diameterKm = 0.109,
            diameterDisplayFa = "۱۰۹ متر (اندازه یک زمین فوتبال)",
            diameterDisplayEn = "109 meters (Football field size)",
            massKgDisplayFa = "۴۵۰,۰۰۰ کیلوگرم (۴۵۰ تن)",
            massKgDisplayEn = "450,000 kg (450 tonnes)",
            gravityMssDisplayFa = "۸.۷ متر بر مجذور ثانیه در مدار (بی‌وزنی ظاهری)",
            gravityMssDisplayEn = "8.7 m/s² orbital (Microgravity environment)",
            distanceDisplayFa = "۴۲۰ کیلومتر (ارتفاع مداری از سطح زمین)",
            distanceDisplayEn = "420 km (Orbital altitude above Earth)"
        ),
        "planet_mercury" to PhysicalProperties(
            diameterKm = 4879.4,
            diameterDisplayFa = "۴,۸۷۹ کیلومتر (۰.۳۸۳ برابر زمین)",
            diameterDisplayEn = "4,879 km (0.383× Earth)",
            massKgDisplayFa = "۳.۳۰۱ × ۱۰²³ کیلوگرم (۰.۰۵۵ برابر زمین)",
            massKgDisplayEn = "3.301 × 10²³ kg (0.055× Earth)",
            gravityMssDisplayFa = "۳.۷۰ متر بر مجذور ثانیه (۰.۳۷۷ برابر زمین)",
            gravityMssDisplayEn = "3.70 m/s² (0.377× Earth)",
            distanceDisplayFa = "۹۱,۷۰۰,۰۰۰ کیلومتر (میانگین از زمین)",
            distanceDisplayEn = "91,700,000 km (Average from Earth)"
        ),
        "planet_venus" to PhysicalProperties(
            diameterKm = 12103.6,
            diameterDisplayFa = "۱۲,۱۰۴ کیلومتر (۰.۹۴۹ برابر زمین)",
            diameterDisplayEn = "12,104 km (0.949× Earth)",
            massKgDisplayFa = "۴.۸۶۷ × ۱۰²⁴ کیلوگرم (۰.۸۱۵ برابر زمین)",
            massKgDisplayEn = "4.867 × 10²⁴ kg (0.815× Earth)",
            gravityMssDisplayFa = "۸.۸۷ متر بر مجذور ثانیه (۰.۹۰۴ برابر زمین)",
            gravityMssDisplayEn = "8.87 m/s² (0.904× Earth)",
            distanceDisplayFa = "۴۱,۴۰۰,۰۰۰ کیلومتر (کمترین فاصله از زمین)",
            distanceDisplayEn = "41,400,000 km (Closest approach to Earth)"
        ),
        "planet_earth" to PhysicalProperties(
            diameterKm = 12742.0,
            diameterDisplayFa = "۱۲,۷۴۲ کیلومتر (۱.۰۰ برابر زمین)",
            diameterDisplayEn = "12,742 km (1.00× Earth)",
            massKgDisplayFa = "۵.۹۷۲ × ۱۰²⁴ کیلوگرم (۱.۰۰ برابر زمین)",
            massKgDisplayEn = "5.972 × 10²⁴ kg (1.00× Earth)",
            gravityMssDisplayFa = "۹.۸۱ متر بر مجذور ثانیه (۱.۰۰ برابر زمین)",
            gravityMssDisplayEn = "9.81 m/s² (1.00× Earth)",
            distanceDisplayFa = "۰ کیلومتر (زیر پای شما)",
            distanceDisplayEn = "0 km (Right beneath you)"
        ),
        "planet_mars" to PhysicalProperties(
            diameterKm = 6779.0,
            diameterDisplayFa = "۶,۷۷۹ کیلومتر (۰.۵۳۲ برابر زمین)",
            diameterDisplayEn = "6,779 km (0.532× Earth)",
            massKgDisplayFa = "۶.۴۱۷ × ۱۰²³ کیلوگرم (۰.۱۰۷ برابر زمین)",
            massKgDisplayEn = "6.417 × 10²³ kg (0.107× Earth)",
            gravityMssDisplayFa = "۳.۷۲ متر بر مجذور ثانیه (۰.۳۷۹ برابر زمین)",
            gravityMssDisplayEn = "3.72 m/s² (0.379× Earth)",
            distanceDisplayFa = "۲۲۵,۰۰۰,۰۰۰ کیلومتر (میانگین از زمین)",
            distanceDisplayEn = "225,000,000 km (Average from Earth)"
        ),
        "planet_jupiter" to PhysicalProperties(
            diameterKm = 139822.0,
            diameterDisplayFa = "۱۳۹,۸۲۲ کیلومتر (۱۰.۹۷ برابر زمین)",
            diameterDisplayEn = "139,822 km (10.97× Earth)",
            massKgDisplayFa = "۱.۸۹۸ × ۱۰²⁷ کیلوگرم (۳۱۷.۸ برابر زمین)",
            massKgDisplayEn = "1.898 × 10²⁷ kg (317.8× Earth)",
            gravityMssDisplayFa = "۲۴.۷۹ متر بر مجذور ثانیه (۲.۵۲۸ برابر زمین)",
            gravityMssDisplayEn = "24.79 m/s² (2.528× Earth)",
            distanceDisplayFa = "۷۷۸,۵۰۰,۰۰۰ کیلومتر | ۰.۰۰۰۰۸۲۳ سال نوری",
            distanceDisplayEn = "778,500,000 km | 0.0000823 light-years"
        ),
        "planet_saturn" to PhysicalProperties(
            diameterKm = 116460.0,
            diameterDisplayFa = "۱۱۶,۴۶۰ کیلومتر (۹.۱۴ برابر زمین)",
            diameterDisplayEn = "116,460 km (9.14× Earth)",
            massKgDisplayFa = "۵.۶۸۳ × ۱۰²۶ کیلوگرم (۹۵.۲ برابر زمین)",
            massKgDisplayEn = "5.683 × 10²۶ kg (95.2× Earth)",
            gravityMssDisplayFa = "۱۰.۴۴ متر بر مجذور ثانیه (۱.۰۶۵ برابر زمین)",
            gravityMssDisplayEn = "10.44 m/s² (1.065× Earth)",
            distanceDisplayFa = "۱,۴۳۳,۰۰۰,۰۰۰ کیلومتر | ۰.۰۰۰۱۵۱ سال نوری",
            distanceDisplayEn = "1,433,000,000 km | 0.000151 light-years"
        ),
        "planet_uranus" to PhysicalProperties(
            diameterKm = 50724.0,
            diameterDisplayFa = "۵۰,۷۲۴ کیلومتر (۳.۹۸ برابر زمین)",
            diameterDisplayEn = "50,724 km (3.98× Earth)",
            massKgDisplayFa = "۸.۶۸۱ × ۱۰²۵ کیلوگرم (۱۴.۵۴ برابر زمین)",
            massKgDisplayEn = "8.681 × 10²⁵ kg (14.54× Earth)",
            gravityMssDisplayFa = "۸.۶۹ متر بر مجذور ثانیه (۰.۸۸۶ برابر زمین)",
            gravityMssDisplayEn = "8.69 m/s² (0.886× Earth)",
            distanceDisplayFa = "۲,۸۷۱,۰۰۰,۰۰۰ کیلومتر | ۰.۰۰۰۳۰۴ سال نوری",
            distanceDisplayEn = "2,871,000,000 km | 0.000304 light-years"
        ),
        "planet_neptune" to PhysicalProperties(
            diameterKm = 49244.0,
            diameterDisplayFa = "۴۹,۲۴۴ کیلومتر (۳.۸۶ برابر زمین)",
            diameterDisplayEn = "49,244 km (3.86× Earth)",
            massKgDisplayFa = "۱.۰۲۴ × ۱۰²۶ کیلوگرم (۱۷.۱۵ برابر زمین)",
            massKgDisplayEn = "1.024 × 10²⁶ kg (17.15× Earth)",
            gravityMssDisplayFa = "۱۱.۱۵ متر بر مجذور ثانیه (۱.۱۳۷ برابر زمین)",
            gravityMssDisplayEn = "11.15 m/s² (1.137× Earth)",
            distanceDisplayFa = "۴,۴۹۵,۰۰۰,۰۰۰ کیلومتر | ۰.۰۰۰۴۷۵ سال نوری",
            distanceDisplayEn = "4,495,000,000 km | 0.000475 light-years"
        ),
        "planet_pluto" to PhysicalProperties(
            diameterKm = 2376.6,
            diameterDisplayFa = "۲,۳۷۷ کیلومتر (۰.۱۸۶ برابر زمین)",
            diameterDisplayEn = "2,377 km (0.186× Earth)",
            massKgDisplayFa = "۱.۳۰۳ × ۱۰²۲ کیلوگرم (۰.۰۰۲۲ برابر زمین)",
            massKgDisplayEn = "1.303 × 10²² kg (0.0022× Earth)",
            gravityMssDisplayFa = "۰.۶۲ متر بر مجذور ثانیه (۰.۰۶۳ برابر زمین)",
            gravityMssDisplayEn = "0.62 m/s² (0.063× Earth)",
            distanceDisplayFa = "۵,۹۰۶,۰۰۰,۰۰۰ کیلومتر | ۰.۰۰۰۶۲۴ سال نوری",
            distanceDisplayEn = "5,906,000,000 km | 0.000624 light-years"
        ),
        "star_sirius" to PhysicalProperties(
            diameterKm = 2382000.0,
            diameterDisplayFa = "۲,۳۸۲,۰۰۰ کیلومتر (۱.۷۱ برابر خورشید / ۱۸۷ برابر زمین)",
            diameterDisplayEn = "2,382,000 km (1.71× Sun / 187× Earth)",
            massKgDisplayFa = "۴.۰۱۸ × ۱۰³⁰ کیلوگرم (۲.۰۶ برابر خورشید / ۶۷۲,۰۰۰ برابر زمین)",
            massKgDisplayEn = "4.018 × 10³⁰ kg (2.06× Sun / 672,000× Earth)",
            gravityMssDisplayFa = "۲۴۰ متر بر مجذور ثانیه (۲۴.۵ برابر زمین)",
            gravityMssDisplayEn = "240 m/s² (24.5× Earth)",
            distanceDisplayFa = "۸.۶ سال نوری (۸۱,۳۶۰,۰۰۰,۰۰۰,۰۰۰ کیلومتر)",
            distanceDisplayEn = "8.6 light-years (81.36 trillion km)"
        ),
        "star_vega" to PhysicalProperties(
            diameterKm = 3286000.0,
            diameterDisplayFa = "۳,۲۸۶,۰۰۰ کیلومتر (۲.۳۶ برابر خورشید / ۲۵۸ برابر زمین)",
            diameterDisplayEn = "3,286,000 km (2.36× Sun / 258× Earth)",
            massKgDisplayFa = "۴.۲۵۰ × ۱۰³⁰ کیلوگرم (۲.۱۳ برابر خورشید / ۷۱۱,۰۰۰ برابر زمین)",
            massKgDisplayEn = "4.250 × 10³⁰ kg (2.13× Sun / 711,000× Earth)",
            gravityMssDisplayFa = "۱۵۸ متر بر مجذور ثانیه (۱۶.۱ برابر زمین)",
            gravityMssDisplayEn = "158 m/s² (16.1× Earth)",
            distanceDisplayFa = "۲۵.۰۴ سال نوری (۲۳۶,۹۰۰,۰۰۰,۰۰۰,۰۰۰ کیلومتر)",
            distanceDisplayEn = "25.04 light-years (236.9 trillion km)"
        ),
        "star_betelgeuse" to PhysicalProperties(
            diameterKm = 1063000000.0,
            diameterDisplayFa = "۱,۰۶۳,۰۰۰,۰۰۰ کیلومتر (۷۶۴ برابر خورشید / ۸۳,۴۰۰ برابر زمین)",
            diameterDisplayEn = "1,063,000,000 km (764× Sun / 83,400× Earth)",
            massKgDisplayFa = "۳.۲۸۰ × ۱۰³¹ کیلوگرم (۱۶.۵ برابر خورشید / ۵,۵۰۰,۰۰۰ برابر زمین)",
            massKgDisplayEn = "3.280 × 10³¹ kg (16.5× Sun / 5.5M× Earth)",
            gravityMssDisplayFa = "۰.۰۰۳ متر بر مجذور ثانیه (چگالی بسیار پایین ابرغول)",
            gravityMssDisplayEn = "0.003 m/s² (Ultra-low density red supergiant)",
            distanceDisplayFa = "۶۴۲.۵ سال نوری (۶,۰۷۸,۰۰۰,۰۰۰,۰۰۰,۰۰۰ کیلومتر)",
            distanceDisplayEn = "642.5 light-years (6.078 quadrillion km)"
        ),
        "star_polaris" to PhysicalProperties(
            diameterKm = 64000000.0,
            diameterDisplayFa = "۶۴,۰۰۰,۰۰۰ کیلومتر (۴۶ برابر خورشید / ۵,۰۲۰ برابر زمین)",
            diameterDisplayEn = "64,000,000 km (46× Sun / 5,020× Earth)",
            massKgDisplayFa = "۱.۰۷۰ × ۱۰³¹ کیلوگرم (۵.۴ برابر خورشید / ۱,۸۰۰,۰۰۰ برابر زمین)",
            massKgDisplayEn = "1.070 × 10³¹ kg (5.4× Sun / 1.8M× Earth)",
            gravityMssDisplayFa = "۷.۰ متر بر مجذور ثانیه (۰.۷۱ برابر زمین)",
            gravityMssDisplayEn = "7.0 m/s² (0.71× Earth)",
            distanceDisplayFa = "۴۳۳.۰ سال نوری (۴,۰۹۶,۰۰۰,۰۰۰,۰۰۰,۰۰۰ کیلومتر)",
            distanceDisplayEn = "433.0 light-years (4.096 quadrillion km)"
        ),
        "galaxy_andromeda_m31" to PhysicalProperties(
            diameterKm = 2.08e18,
            diameterDisplayFa = "۲۲۰,۰۰۰ سال نوری قطر (۲.۲ برابر کهکشان راه شیری)",
            diameterDisplayEn = "220,000 light-years diameter (2.2× Milky Way)",
            massKgDisplayFa = "۲.۹۸ × ۱۰⁴۲ کیلوگرم (۱,۵۰۰ میلیارد برابر جرم خورشید)",
            massKgDisplayEn = "2.98 × 10⁴² kg (1.5 trillion Solar Masses)",
            gravityMssDisplayFa = "میدان گرانشی عظیم کهکشانی (دارای ۱۰۰۰ میلیارد ستاره)",
            gravityMssDisplayEn = "Massive galactic gravitational potential (1 trillion stars)",
            distanceDisplayFa = "۲,۵۰۰,۰۰۰ سال نوری (دورترین جرم با چشم غیرمسلح)",
            distanceDisplayEn = "2,500,000 light-years (Farthest object visible to naked eye)"
        ),
        "nebula_orion_m42" to PhysicalProperties(
            diameterKm = 2.27e14,
            diameterDisplayFa = "۲۴ سال نوری قطر (زایشگاه فعال ستارگان)",
            diameterDisplayEn = "24 light-years diameter (Active stellar nursery)",
            massKgDisplayFa = "۳.۹۸ × ۱۰³۳ کیلوگرم (۲,۰۰۰ برابر جرم خورشید)",
            massKgDisplayEn = "3.98 × 10³³ kg (2,000 Solar Masses)",
            gravityMssDisplayFa = "ابرهای گاز هیدروژن و گرد و غبار در حال انقباض گرانشی",
            gravityMssDisplayEn = "Collapsing hydrogen cloud cores forming new stars",
            distanceDisplayFa = "۱,۳۴۴ سال نوری (نزدیک‌ترین زایشگاه عظیم ستاره‌ای به زمین)",
            distanceDisplayEn = "1,344 light-years (Nearest massive star-forming region)"
        ),
        "cluster_pleiades_m45" to PhysicalProperties(
            diameterKm = 1.65e14,
            diameterDisplayFa = "۱۷.۵ سال نوری پهنای خوشه (شامل بیش از ۱۰۰۰ ستاره)",
            diameterDisplayEn = "17.5 light-years cluster span (Over 1,000 stars)",
            massKgDisplayFa = "۱.۵۹ × ۱۰³۳ کیلوگرم (۸۰۰ برابر جرم خورشید)",
            massKgDisplayEn = "1.59 × 10³³ kg (800 Solar Masses)",
            gravityMssDisplayFa = "پیوند گرانشی ضعیف خوشه باز ستاره‌ای جوان",
            gravityMssDisplayEn = "Loosely bound young open star cluster",
            distanceDisplayFa = "۴۴۴ سال نوری (نزدیک‌ترین خوشه ستاره‌ای بارز)",
            distanceDisplayEn = "444 light-years (Nearest prominent open cluster)"
        ),
        "galaxy_milky_way" to PhysicalProperties(
            diameterKm = 9.46e17,
            diameterDisplayFa = "۱۰۰,۰۰۰ سال نوری قطر (کهکشان مارپیچی میله‌ای)",
            diameterDisplayEn = "100,000 light-years diameter (Barred spiral galaxy)",
            massKgDisplayFa = "۲.۹۸ × ۱۰⁴۲ کیلوگرم (۱,۵۰۰ میلیارد برابر جرم خورشید)",
            massKgDisplayEn = "2.98 × 10⁴² kg (1.5 trillion Solar Masses)",
            gravityMssDisplayFa = "سیاهچاله کلان‌جرم مرکزی (Sagittarius A*) با جرم ۴ میلیون خورشید",
            gravityMssDisplayEn = "Supermassive black hole core (Sagittarius A*, 4M Solar Masses)",
            distanceDisplayFa = "۲۶,۰۰۰ سال نوری فاصله منظومه شمسی تا هسته مرکز کهکشان",
            distanceDisplayEn = "26,000 light-years from Solar System to Galactic Core"
        ),
        "galilean_moon_io" to PhysicalProperties(
            diameterKm = 3643.2,
            diameterDisplayFa = "۳,۶۴۳ کیلومتر (۰.۲۸۶ برابر قطر زمین / قمر آتشفشانی)",
            diameterDisplayEn = "3,643 km (0.286× Earth / Volcanic Moon)",
            massKgDisplayFa = "۸.۹۳ × ۱۰²² کیلوگرم (۰.۰۱۵ برابر زمین)",
            massKgDisplayEn = "8.93 × 10²² kg (0.015× Earth)",
            gravityMssDisplayFa = "۱.۷۹۶ متر بر مجذور ثانیه (۰.۱۸۳ برابر زمین)",
            gravityMssDisplayEn = "1.796 m/s² (0.183× Earth)",
            distanceDisplayFa = "۴۲۱,۷۰۰ کیلومتر از مرکز مشتری (۵.۹ شعاع مشتری)",
            distanceDisplayEn = "421,700 km from Jupiter center (5.9 R_J)"
        ),
        "galilean_moon_europa" to PhysicalProperties(
            diameterKm = 3121.6,
            diameterDisplayFa = "۳,۱۲۱.۶ کیلومتر (۰.۲۴۵ برابر قطر زمین / اقیانوس زیرسطحی)",
            diameterDisplayEn = "3,121.6 km (0.245× Earth / Subsurface Ocean)",
            massKgDisplayFa = "۴.۸۰ × ۱۰²² کیلوگرم (۰.۰۰۸ برابر زمین)",
            massKgDisplayEn = "4.80 × 10²² kg (0.008× Earth)",
            gravityMssDisplayFa = "۱.۳۱۵ متر بر مجذور ثانیه (۰.۱۳۴ برابر زمین)",
            gravityMssDisplayEn = "1.315 m/s² (0.134× Earth)",
            distanceDisplayFa = "۶۷۰,۹۰۰ کیلومتر از مرکز مشتری (۹.۴ شعاع مشتری)",
            distanceDisplayEn = "670,900 km from Jupiter center (9.4 R_J)"
        ),
        "galilean_moon_ganymede" to PhysicalProperties(
            diameterKm = 5268.2,
            diameterDisplayFa = "۵,۲۶۸.۲ کیلومتر (۰.۴۱۳ برابر زمین / بزرگ‌تر از عطارد)",
            diameterDisplayEn = "5,268.2 km (0.413× Earth / Larger than Mercury)",
            massKgDisplayFa = "۱.۴۸ × ۱۰²۳ کیلوگرم (۰.۰۲۵ برابر زمین)",
            massKgDisplayEn = "1.48 × 10²³ kg (0.025× Earth)",
            gravityMssDisplayFa = "۱.۴۲۸ متر بر مجذور ثانیه (۰.1۴۶ برابر زمین)",
            gravityMssDisplayEn = "1.428 m/s² (0.146× Earth)",
            distanceDisplayFa = "۱,۰۷۰,۴۰۰ کیلومتر از مرکز مشتری (۱۵.۰ شعاع مشتری)",
            distanceDisplayEn = "1,070,400 km from Jupiter center (15.0 R_J)"
        ),
        "galilean_moon_callisto" to PhysicalProperties(
            diameterKm = 4820.6,
            diameterDisplayFa = "۴,۸۲۰.۶ کیلومتر (۰.۳۷۸ برابر قطر زمین / پردهانه‌ترین سطح)",
            diameterDisplayEn = "4,820.6 km (0.378× Earth / Heavily Cratered)",
            massKgDisplayFa = "۱.۰۸ × ۱۰²۳ کیلوگرم (۰.۰۱۸ برابر زمین)",
            massKgDisplayEn = "1.08 × 10²³ kg (0.018× Earth)",
            gravityMssDisplayFa = "۱.۲۳۵ متر بر مجذور ثانیه (۰.۱۲۶ برابر زمین)",
            gravityMssDisplayEn = "1.235 m/s² (0.126× Earth)",
            distanceDisplayFa = "۱,۸۸۲,۷۰۰ کیلومتر از مرکز مشتری (۲۶.۴ شعاع مشتری)",
            distanceDisplayEn = "1,882,700 km from Jupiter center (26.4 R_J)"
        ),
        "galilean_moon_elara" to PhysicalProperties(
            diameterKm = 86.0,
            diameterDisplayFa = "۸۶ کیلومتر قطر (قمر نامنظم هیپالیا)",
            diameterDisplayEn = "86 km diameter (Irregular Himalia Group)",
            massKgDisplayFa = "۸.۷ × ۱۰¹۷ کیلوگرم (سیارک کربنی به دام افتاده)",
            massKgDisplayEn = "8.7 × 10¹⁷ kg (Captured carbonaceous asteroid)",
            gravityMssDisplayFa = "۰.۰۳۱ متر بر مجذور ثانیه (گرانش بسیار ناچیز)",
            gravityMssDisplayEn = "0.031 m/s² (Microgravity)",
            distanceDisplayFa = "۱۱,۷۴۰,۰۰۰ کیلومتر از مرکز مشتری (۱۶۴.۲ شعاع مشتری)",
            distanceDisplayEn = "11,740,000 km from Jupiter center (164.2 R_J)"
        )
    )

    private val coolFactsMap = mapOf(
        "sun_sol" to listOf(
            "هسته خورشید با دمای ۱۵ میلیون درجه سانتی‌گراد، در هر ثانیه ۶۰۰ میلیون تن هیدروژن را به هلیوم تبدیل می‌کند.",
            "نوری که از سطح خورشید ساطع می‌شود، حدود ۸ دقیقه و ۲۰ ثانیه طول می‌کشد تا به زمین برسد.",
            "خورشید بیش از ۹۹.۸۶ درصد از کل جرم منظومه شمسی را به خود اختصاص داده است.",
            "میدان مغناطیسی خورشید هر ۱۱ سال یک‌بار به طور کامل معکوس می‌شود و قطب‌های شمال و جنوب جای خود را عوض می‌کنند.",
            "خورشید یک ستاره رشته اصلی از نوع طیفی G2V یا زرد کوچک است که حدود ۴.۶ میلیارد سال سن دارد."
        ),
        "moon_luna" to listOf(
            "ماه با سرعت ۳.۸ سانتی‌متر در سال در حال دور شدن تدریجی از زمین است.",
            "به دلیل قفل جزر و مدی، دوره چرخش ماه به دور خود با دوره گردش آن به دور زمین یکسان (۲۷.۳ روز) است و ما همیشه یک سمت آن را می‌بینیم.",
            "دمای سطح ماه در روز به ۱۲۰ درجه سانتی‌گراد بالای صفر و در شب به ۱۳۰- درجه سانتی‌گراد زیر صفر می‌رسد.",
            "تاریک‌ترین گودال‌های قطب جنوب ماه حاوی میلیاردها تن یخ آب دست‌نخورده هستند.",
            "نیروی گرانش ماه باعث ایجاد جزر و مد در اقیانوس‌های زمین و کند شدن تدریجی سرعت دوران زمین می‌شود."
        ),
        "sat_iss" to listOf(
            "ایستگاه فضایی بین‌المللی با سرعت ۲۷,۶۰۰ کیلومتر بر ساعت، هر ۹۰ دقیقه یک‌بار زمین را دور می‌زند.",
            "فضانوردان حاضر در ISS در هر ۲۴ ساعت، ۱۶ بار طلوع و غروب خورشید را تجربه می‌کنند.",
            "ابعاد ایستگاه فضایی معادل یک زمین فوتبال بزرگ است و فضایی قابل سکونت برابر یک خانه ۶ خوابه دارد.",
            "پنل‌های خورشیدی ISS مساحتی حدود ۲,۴۰۰ متر مربع را پوشش می‌دهند و برق کل ایستگاه را تامین می‌کنند.",
            "ایستگاه فضایی پس از ماه و زهره، سومین جرم درخشان در آسمان شب زمین محسوب می‌شود."
        ),
        "planet_mercury" to listOf(
            "عطارد سریع‌ترین سیاره منظومه شمسی است و یک سال آن تنها ۸۸ روز زمین طول می‌کشد.",
            "اختلاف دمای روز و شب در عطارد بیشترین میزان در منظومه شمسی است (از ۴۳۰ درجه بالای صفر تا ۱۸۰- زیر صفر).",
            "عطارد با وجود نزدیکی به خورشید، گرم‌ترین سیاره نیست؛ زیرا جوی برای به دام انداختن گرما ندارد.",
            "هسته فلزی آهن در عطارد حدود ۸۵ درصد از شعاع کل این سیاره را تشکیل می‌دهد.",
            "گودال‌های قطبی عطارد به دلیل زاویه میل صفر درجه محوری، هرگز نور خورشید را نمی‌بینند و دارای یخ آب هستند."
        ),
        "planet_venus" to listOf(
            "زهره داغ‌ترین سیاره منظومه شمسی است که دمای سطح آن به دلیل اثر گلخانه‌ای شدید به ۴۶۵ درجه سانتی‌گراد می‌رسد.",
            "جهت چرخش زهره به دور خود معکوس (ساعت‌گرد) است؛ بنابراین خورشید در زهره از غرب طلوع و در شرق غروب می‌کند.",
            "یک روز در زهره (۲۴۳ روز زمین) طولانی‌تر از یک سال آن (۲۲۵ روز زمین) به طول می‌انجامد.",
            "فشار جو در سطح زهره ۹۲ برابر فشار جو زمین است (معادل فشار آب در عمق ۹۰۰ متری اقیانوس).",
            "ابرهای غلیظ زهره از قطرات اسید سولفوریک تشکیل شده‌اند و بیش از ۷۵ درصد نور خورشید را بازمی‌تابانند."
        ),
        "planet_earth" to listOf(
            "زمین تنها جهان شناخته‌شده در کیهان است که دارای اقیانوس‌های آب مایع و حیات هوشمند است.",
            "میدان مغناطیسی زمین (سپهر مغناطیسی) زمین را در برابر بادهای مخرب خورشیدی و پرتوهای کیهانی محافظت می‌کند.",
            "جو زمین حاوی ۷۸٪ نیتروژن و ۲۱٪ اکسیژن است که شرایط ایده‌آلی برای تنفس موجودات زنده فراهم می‌سازد.",
            "زمین تنها سیاره خاکی منظومه شمسی است که دارای صفحات تکتونیکی فعال و پویا می‌باشد.",
            "سرعت چرخش زمین به دور خورشید حدود ۱۰۷,۰۰۰ کیلومتر بر ساعت (۳۰ کیلومتر بر ثانیه) است."
        ),
        "planet_mars" to listOf(
            "کوه المپوس در مریخ بزرگ‌ترین آتشفشان منظومه شمسی است که ارتفاعی ۳ برابر کوه اورست (۲۱.۹ کیلومتر) دارد.",
            "دره والز مارینریس در مریخ دره‌ای غول‌پیکر به طول ۴,۰۰۰ کیلومتر است که کل پهنای ایالات متحده را می‌پوشاند.",
            "رنگ سرخ مریخ ناشی از اکسید آهن (زنگ‌زدگی) موجود در خاک و غبار سطح آن است.",
            "مریخ دارای دو قمر کوچک و ناهموار به نام‌های فوبوس و دیموس است که احتمالاً سیارک‌های به دام افتاده هستند.",
            "یک روز در مریخ (سول) بسیار نزدیک به روز زمین است و ۲۴ ساعت و ۳۹ دقیقه طول می‌کشد."
        ),
        "planet_jupiter" to listOf(
            "لکه سرخ بزرگ مشتری طوفانی عظیم و کهن است که ابعادی بزرگ‌تر از کل کره زمین دارد.",
            "مشتری دارای قوی‌ترین میدان مغناطیسی در میان سیارات است که ۲۰,۰۰۰ برابر قوی‌تر از میدان مغناطیسی زمین می‌باشد.",
            "مشتری سریع‌ترین سرعت دوران به دور خود را دارد و یک شبانه‌روز آن تنها ۹ ساعت و ۵۵ دقیقه طول می‌کشد.",
            "چهار قمر بزرگ مشتری (گالیله‌ای) شامل گانی‌مید (بزرگ‌ترین قمر کیهان)، اروپا (اقیانوس زیرسطحی)، یو (آتشفشانی) و کالیستو هستند.",
            "مشتری به عنوان سپر گرانشی زمین عمل کرده و بسیاری از دنباله‌دارها و سیارک‌های خطرناک را به سمت خود جذب می‌کند."
        ),
        "planet_saturn" to listOf(
            "حلقه‌های تماشایی زحل از میلیاردها قطعه یخ، غبار و سنگ با ضخامتی تنها حدود ۱۰ متر تشکیل شده‌اند.",
            "چگالی زحل از آب کمتر است؛ اگر اقیانوسی به اندازه کافی بزرگ وجود داشت، زحل روی آب شناور می‌ماند!",
            "قمر تایتان زحل تنها قمر منظومه شمسی با جوی غلیظ و دریاچه‌های مایع متان و اتان است.",
            "در قطب شمال زحل، یک طوفان شش‌ضلعی (ارگون) شگفت‌انگیز و مداوم به عرض ۳۰,۰۰۰ کیلومتر وجود دارد.",
            "زحل تا کنون بیش از ۱۴۶ قمر شناخته‌شده دارد که بیشترین تعداد در منظومه شمسی است."
        ),
        "planet_uranus" to listOf(
            "اورانوس انحراف محوری عجیب ۹۸ درجه‌ای دارد و عملاً روی مدار خود به دور خورشید می‌غلتد!",
            "اورانوس سردترین جو را در میان سیارات منظومه شمسی دارد که دمای آن به ۲۲۴- درجه سانتی‌گراد می‌رسد.",
            "رنگ فیروزه‌ای-آبی اورانوس به دلیل وجود گاز متان در جو بالای آن است که نور سرخ را جذب می‌کند.",
            "اورانوس دارای ۱۳ حلقه تاریک و باریک است که پس از حلقه‌های زحل کشف شدند.",
            "هر قطب اورانوس ۴۲ سال مداوم نور خورشید و ۴۲ سال تاریکی مطلق پیاپی را تجربه می‌کند."
        ),
        "planet_neptune" to listOf(
            "نپتون دارای شدیدترین بادهای منظومه شمسی است که سرعت آن‌ها به بیش از ۲,۱۰۰ کیلومتر بر ساعت می‌رسد.",
            "نپتون نخستین سیاره‌ای بود که وجود آن ابتدا از طریق محاسبات ریاضی گرانشی پیش‌بینی و سپس با تلسکوپ رصد شد.",
            "قمر بزرگ نپتون، تریتون، تنها قمر بزرگ منظومه شمسی است که مداری معکوس (مخالف جهت چرخش سیاره) دارد.",
            "فاصله نپتون از خورشید به قدری زیاد است که ظهر در نپتون معکوس غروب خورشید در زمین روشنی دارد.",
            "یک سال در نپتون معادل ۱۶۵ سال زمین به طول می‌انجامد."
        ),
        "planet_pluto" to listOf(
            "پلوتو دارای یک منطقه یخچالی نیتروژنی قلبی‌شکل معروف به نام «تومبا رجیو» است.",
            "قمر بزرگ پلوتو، شارون، ابعادی نصف پلوتو دارد و این دو جرم یک سامانه دوتایی قفل‌شده را تشکیل می‌دهند.",
            "مدار پلوتو کاملاً بیضی شکل است و در بخشی از مدار خود به خورشید نزدیک‌تر از نپتون می‌شود.",
            "در سال ۲۰۰۶، اتحادیه بین‌المللی اخترشناسی (IAU) تعریف سیاره را تغییر داد و پلوتو به عنوان سیاره کوتوله طبقه‌بندی شد.",
            "جو رقیق پلوتو هنگام نزدیک شدن به خورشید تبخیر شده و هنگام دور شدن منجمد و بر سطح می‌بارد."
        ),
        "star_sirius" to listOf(
            "شباهنگ (Sirius) درخشان‌ترین ستاره در آسمان شب زمین با قدر ظاهری ۱۴۶.- است.",
            "شباهنگ یک سامانه ستاره‌ای دوتایی است؛ ستاره همدم آن (Sirius B) یک کوتوله سفید بسیار متراکم است.",
            "در مصر باستان، طلوع شامگاهی شباهنگ نشان‌دهنده شروع طغیان سالانه رود نیل و آغاز سال نو بود.",
            "دمای سطح شباهنگ حدود ۹,۹۴۰ کلوین است که موجب درخشش سفید-آبی خیره‌کننده آن می‌شود.",
            "فاصله شباهنگ از زمین تنها ۸.۶ سال نوری است که آن را به یکی از نزدیک‌ترین همسایگان خورشید تبدیل می‌کند."
        ),
        "star_vega" to listOf(
            "ستاره نسر واقع (Vega) مبنای اولیه صفر قدر ظاهری در مقیاس درخشش ستارگان بوده است.",
            "حدود ۱۲,۰۰۰ سال پیش، نسر واقع ستاره قطبی زمین بوده است و ۱۴,۰۰۰ سال دیگر دوباره ستاره قطبی خواهد شد.",
            "نسر واقع نخسیتن ستاره‌ای بود که پس از خورشید از آن عکس‌برداری شد (سال ۱۸۵۰ میلادی).",
            "سرعت دوران نسر واقع به دور خود بسیار بالاست و شکل آن در استوا برآمده شده است.",
            "قرص غباری بزرگی در اطراف نسر واقع وجود دارد که نشان‌دهنده احتمال وجود سامانه‌های سیاره‌ای است."
        ),
        "star_betelgeuse" to listOf(
            "ابط‌الجوزا (Betelgeuse) یک ابرغول سرخ غول‌پیکر است که اگر جای خورشید بود، تا مدار مشتری را می‌بلعید!",
            "این ستاره در مراحل پایانی تکامل خود قرار دارد و به زودی (در مقیاس نجومی) دچار انفجار ابرنواختری خواهد شد.",
            "انفجار ابرنواختری ابط‌الجوزا به قدری درخشان خواهد بود که تا چند هفته در روز روشن نیز دیده خواهد شد.",
            "در سال ۲۰۱۹، افت نور شدید ابط‌الجوزا ناشی از خروج یک توده عظیم غبار و گاز از سطح آن بود.",
            "ابط‌الجوزا حدود ۱۰,۰۰۰ برابر خورشید روشنایی تابش می‌کند اما دمای سطح آن تنها ۳,۵۰۰ کلوین است."
        ),
        "star_polaris" to listOf(
            "ستاره قطبی (Polaris) دقیقاً در امتداد محور دوران زمین در قطب شمال آسمان قرار دارد.",
            "ارتفاع زاویه‌ای ستاره قطبی از افق، دقیقاً برابر با عرض جغرافیایی محل رصد شماست.",
            "ستاره قطبی یک ستاره تکی نیست، بلکه یک سامانه سه‌تایی از ستارگان تپنده متغیر قیفاووسی است.",
            "درخشش ستاره قطبی حدود ۲,۵۰۰ برابر خورشید است و در فاصله ۴۳۳ سال نوری قرار دارد.",
            "به دلیل حرکت تقدیمی محور زمین، ستاره قطبی برای همیشه در قطب شمال نخواهد ماند."
        ),
        "galaxy_andromeda_m31" to listOf(
            "کهکشان آندرومدا نزدیک‌ترین کهکشان بزرگ به راه شیری است که بیش از ۱,۰۰۰ میلیارد ستاره دارد.",
            "آندرومدا دورترین جرمی در کیهان است که می‌توان آن را با چشم غیرمسلح در تاریکی شب مشاهده کرد.",
            "کهکشان آندرومدا با سرعت ۱۱۰ کیلومتر بر ثانیه در حال نزدیک شدن به کهکشان راه شیری است.",
            "حدود ۴.۵ میلیارد سال دیگر، کهکشان آندرومدا و راه شیری با هم ادغام شده و یک کهکشان بیضوی غول‌پیکر می‌سازند.",
            "نوری که امشب از آندرومدا می‌بینید، ۲.۵ میلیون سال پیش (زمان انسان‌های اولیه) حرکت خود را آغاز کرده است."
        ),
        "nebula_orion_m42" to listOf(
            "سحابی جبار درخشان‌ترین و نزدیک‌ترین زایشگاه عظیم ستاره‌ای به زمین است.",
            "در مرکز سحابی جبار، خوشه چهارتایی ستارگان جوان و داغ «تراپزیوم» گازهای هیدروژن را به درخشش واداشته‌اند.",
            "سحابی جبار حاوی ده‌ها قرص سیاره‌ساز اولیه (پروپلی) است که سیستم‌های سیاره‌ای جدید را شکل می‌دهند.",
            "رنگ سبز سحابی در عکس‌های نجومی ناشی از تابش اکسیژن یونیزه شده و رنگ سرخ ناشی از هیدروژن آلفا است.",
            "مساحت واقعی سحابی جبار در آسمان شب، حدود چهار برابر مساحت ماه کامل است."
        ),
        "cluster_pleiades_m45" to listOf(
            "خوشه پروین (ثریا) یک خوشه باز ستاره‌ای جوان شامل بیش از ۱,۰۰۰ ستاره داغ و آبی‌رنگ است.",
            "سن این خوشه ستاره‌ای تنها حدود ۱۰۰ میلیون سال است (در مقایسه با سن ۴.۶ میلیارد ساله خورشید).",
            "ابرهای غباری انعکاسی آبی‌رنگ اطراف ستارگان، بقایای ابر مولکولی اولیه شکل‌گیری خوشه هستند.",
            "در فرهنگ‌ها و اساطیر کهن جهان (ایران، یونان، ژاپن و عرب)، این خوشه به نام هفت خواهران شناخته می‌شود.",
            "این خوشه با چشم غیرمسلح شبیه یک ملاقه کوچک یا آبپاش مینیاتوری در صورت فلکی گاو دیده می‌شود."
        ),
        "galaxy_milky_way" to listOf(
            "کهکشان راه شیری حاوی بین ۱۰۰ تا ۴۰۰ میلیارد ستاره و حداقل ۱۰۰ میلیارد سیاره است.",
            "در مرکز کهکشان راه شیری، سیاهچاله کلان‌جرم کمان آ (*Sagittarius A) با جرمی ۴ میلیون برابر خورشید قرار دارد.",
            "منظومه شمسی هر ۲۳۰ میلیون سال یک‌بار مدار کامل خود به دور مرکز کهکشان را طی می‌کند (یک سال کیهانی).",
            "نوار روشن راه شیری در آسمان شب، مقطع عرضی دیسک کهکشان خودمان است که از داخل آن را تماشا می‌کنیم.",
            "کهکشان راه شیری بخشی از ابرخوشه کهکشانی عظیم «لانیاکئا» شامل بیش از ۱۰Node هزار کهکشان است."
        ),
        "galilean_moon_io" to listOf(
            "آیو فعال‌ترین جرم از نظر آتشفشانی در تمام منظومه شمسی است که فوران‌های گوگردی آن تا ارتفاع ۵۰۰ کیلومتری به فضا پرتاب می‌شوند.",
            "علت اصلی آتشفشان‌های شدید آیو، جزر و مد گرانشی ناشی از رزونانس مداری ۴:۲:۱ با مشتری، اروپا و گانی‌مید است.",
            "سطح آیو دائماً با گدازه‌های تازه پوشانده می‌شود و ظاهر زرد، قرمز و سیاهی شبیه به پیتزا به آن می‌دهد.",
            "حرکت آیو در کمربند تشعشعی مشتری، یک توروس پلاسمایی عظیم ایجاد می‌کند که میلیون‌ها آمپر جریان الکتریکی تولید می‌کند.",
            "برخلاف بیشتر قمرهای یخ‌زده خارجی، آیو عمدتاً از سنگ‌های سیلیکاتی و هسته آهنی مذاب تشکیل شده است."
        ),
        "galilean_moon_europa" to listOf(
            "اروپا دارای اقیانوسی جهانی از آب مایع زیر پوسته یخی خود است که حجم آب آن بیش از دو برابر تمام اقیانوس‌های زمین است.",
            "پوسته یخی اروپا بسیار صاف بوده و خطوط رگه‌مانند قهوه‌ای‌رنگی به نام «خطوارگی» (Lineae) سطح آن را پوشانده است.",
            "اقیانوس زیرسطحی اروپا به عنوان یکی از امیدوارکننده‌ترین جاها برای یافتن حیات فرازمینی توسط مأموریت‌های Clipper و JUICE بررسی می‌شود.",
            "آب‌فشان‌های عظیمی از بخار آب در قطب جنوب اروپا شناسایی شده‌اند که از فوران‌های اقیانوس زیرسطحی سرچشمه می‌گیرند.",
            "نیروی جزر و مدی مشتری باعث انقباض و انبساط مداوم هسته اروپا و تولید گرمای هیدروترمال برای مایع ماندن اقیانوس می‌شود."
        ),
        "galilean_moon_ganymede" to listOf(
            "گانی‌مید بزرگ‌ترین قمر در تمام منظومه شمسی است و ابعاد آن از سیاره عطارد و سیاره کوتوله پلوتو نیز بزرگ‌تر است.",
            "گانی‌مید تنها قمر شناخته‌شده در کیهان است که دارای میدان مغناطیسی اختصاصی (مگنوسفر) ناشی از هسته آهنی مذاب است.",
            "تعامل میدان مغناطیسی گانی‌مید با مشتری باعث ایجاد شفق‌های قطبی درخشان در قطب‌های این قمر می‌شود.",
            "در زیر پوسته ضخیم یخی گانی‌مید، اقیانوس عمیق چندلایه‌ای از آب مایع ساندویچ‌شده بین لایه‌های یخ وجود دارد.",
            "سطح گانی‌مید شامل دهانه‌های برخوردی ۴ میلیارد ساله تاریک و شیارهای روشن جوان‌تر ناشی از گسل‌های تکتونیکی است."
        ),
        "galilean_moon_callisto" to listOf(
            "کالیستو پردهانه‌ترین و دست‌نخورده‌ترین جرم منظومه شمسی است که سطح یخی آن بیش از ۴ میلیارد سال قدمت دارد.",
            "بزرگ‌ترین عارضه برخوردی کالیستو، دهانه چندحلقه‌ای «والهالا» با قطری بیش از ۳,۸۰۰ کیلومتر است.",
            "کالیستو خارج از کمربند تشعشعی خطرناک مشتری گردش می‌کند و بهترین گزینه برای پایگاه‌های انسانی آینده است.",
            "این قمر فاقد فعالیت‌های آتشفشانی یا تکتونیکی بوده و تاریخچه اولیه منظومه شمسی را به صورت بکر حفظ کرده است.",
            "کالیستو ترکیبی ۵۰/۵۰ از سنگ و یخ است و احتمالاً دارای اقیانوسی شور در عمق ۱۰۰ تا ۲۵۰ کیلومتری می‌باشد."
        ),
        "galilean_moon_elara" to listOf(
            "این قمر نامنظم مشتری در سال ۱۹۰۵ توسط چارلز دیلون پرین در رصدخانه الیک کشف شد.",
            "پیش از نام‌گذاری رسمی در سال ۱۹۷۵، این قمر در برخی فرهنگ‌ها و متون نجومی غیررسمی با نام‌های «دیانز» (Dianz) یا «گوجه سبز» (Green Tomato) نیز شناخته می‌شد.",
            "الارا متعلق به گروه هیپالیا از قمرهای نامنظم مشتری است که مداری موافق و دوردست در فاصله ۱۱.۷ میلیون کیلومتری دارند.",
            "سطح الارا بسیار تاریک و خاکستری‌رنگ (از نوع کربنی C) است که نشان می‌دهد احتمالاً سیارکی بوده که توسط گرانش مشتری به دام افتاده است.",
            "یک دور گردش کامل الارا به دور مشتری حدود ۲۵۹.۶ روز زمین به طول می‌انجامد."
        )
    )

    private val coolFactsMapEn = mapOf(
        "galilean_moon_io" to listOf(
            "Io is the most volcanically active body in the Solar System with over 400 active sulfur volcanoes.",
            "Tidal heating from gravitational interaction with Jupiter, Europa, and Ganymede powers Io's intense volcanic activity.",
            "Continuous lava flows resurface Io, giving it a vibrant colorful yellow, black, and red 'pizza' appearance.",
            "Io's movement through Jupiter's magnetosphere generates a 1-million-ampere electric current ring.",
            "Unlike icy outer moons, Io consists primarily of silicate rock surrounding a molten iron core."
        ),
        "galilean_moon_europa" to listOf(
            "Europa holds a vast subsurface liquid water ocean containing more water than all Earth's oceans combined.",
            "Its smooth ice shell is crisscrossed by dark reddish fractures called lineae formed by tidal stresses.",
            "Europa is a top target in astrobiology and the search for extraterrestrial habitability.",
            "Plumes of water vapor erupting scores of kilometers into space have been detected near its south pole.",
            "Tidal flexing from Jupiter generates internal hydrothermal heat that keeps its ocean liquid."
        ),
        "galilean_moon_ganymede" to listOf(
            "Ganymede is the largest moon in the Solar System (5,268 km diameter)—bigger than Mercury and Pluto.",
            "It is the only moon known to generate its own intrinsic magnetic field via a liquid iron core.",
            "Magnetic interactions with Jupiter generate glowing auroral ovals around Ganymede's polar regions.",
            "A deep subsurface saltwater ocean containing more water than Earth lies buried beneath its icy crust.",
            "Its surface features ancient dark cratered regions alongside younger, grooved tectonic terrain."
        ),
        "galilean_moon_callisto" to listOf(
            "Callisto is the most heavily cratered object in the Solar System, preserving a 4-billion-year-old surface.",
            "Its surface features colossal multi-ring impact basins, including Valhalla spanning 3,800 km.",
            "Orbiting outside Jupiter's main radiation belt makes Callisto ideal for future human space exploration bases.",
            "Lack of active geological processes leaves its pristine ancient planetary history perfectly preserved.",
            "Callisto consists of equal parts rock and ice and likely harbors a salty subsurface ocean."
        ),
        "galilean_moon_elara" to listOf(
            "Elara is an irregular Jovian satellite discovered in 1905 by astronomer Charles Dillon Perrine.",
            "Before its official naming in 1975, Elara was informally called Jupiter VII and was also colloquially referred to as 'Dianz' (دیانز) or 'Green Tomato' (گوجه سبز).",
            "It belongs to the Himalia group of prograde irregular moons orbiting nearly 12 million kilometers from Jupiter.",
            "Elara has an extremely dark C-type carbonaceous surface (albedo 0.04), indicating it is a captured asteroid.",
            "It takes Elara approximately 259.6 Earth days to complete one orbit around Jupiter."
        )
    )

    fun getPhysicalProperties(obj: CelestialObject): PhysicalProperties {
        physicalMap[obj.id]?.let { return it }

        return when (obj.type) {
            com.alijafari.red.astronomy.domain.ObjectType.STAR -> PhysicalProperties(
                diameterKm = 1.39e6 * (1.0 + (abs(obj.magnitude) / 5.0)),
                diameterDisplayFa = "${String.format("%.1f", 1.0 + abs(obj.magnitude)/4.0)} برابر قطر خورشید",
                diameterDisplayEn = "${String.format("%.1f", 1.0 + abs(obj.magnitude)/4.0)}× Solar Diameter",
                massKgDisplayFa = "${String.format("%.1f", 1.2 + abs(obj.magnitude)/3.0)} برابر جرم خورشید",
                massKgDisplayEn = "${String.format("%.1f", 1.2 + abs(obj.magnitude)/3.0)}× Solar Mass",
                gravityMssDisplayFa = "رده طیفی ${if (obj.spectralType.isNotEmpty()) obj.spectralType else "A/F/G"}",
                gravityMssDisplayEn = "Spectral Class: ${if (obj.spectralType.isNotEmpty()) obj.spectralType else "A/F/G"}",
                distanceDisplayFa = String.format("%,.1f سال نوری", obj.distanceLightYears),
                distanceDisplayEn = String.format("%,.1f light-years", obj.distanceLightYears)
            )
            com.alijafari.red.astronomy.domain.ObjectType.GALAXY -> PhysicalProperties(
                diameterKm = 9.46e17,
                diameterDisplayFa = "۵۰,۰۰۰ تا ۱۵۰,۰۰۰ سال نوری قطر",
                diameterDisplayEn = "50,000 to 150,000 light-years diameter",
                massKgDisplayFa = "۱۰۰ تا ۵۰۰ میلیارد برابر جرم خورشید",
                massKgDisplayEn = "100B to 500B Solar Masses",
                gravityMssDisplayFa = "میدان گرانشی عظیم کهکشانی",
                gravityMssDisplayEn = "Massive galactic gravitational field",
                distanceDisplayFa = String.format("%,.0f سال نوری", obj.distanceLightYears),
                distanceDisplayEn = String.format("%,.0f light-years", obj.distanceLightYears)
            )
            com.alijafari.red.astronomy.domain.ObjectType.NEBULA -> PhysicalProperties(
                diameterKm = 1.0e14,
                diameterDisplayFa = "۱۰ تا ۵۰ سال نوری پهنای سحابی",
                diameterDisplayEn = "10 to 50 light-years extent",
                massKgDisplayFa = "۵۰۰ تا ۳,۰۰۰ برابر جرم خورشید",
                massKgDisplayEn = "500 to 3,000 Solar Masses",
                gravityMssDisplayFa = "ابرهای گاز هیدروژن و یون‌های باردار",
                gravityMssDisplayEn = "Ionized hydrogen and interstellar dust",
                distanceDisplayFa = String.format("%,.0f سال نوری", obj.distanceLightYears),
                distanceDisplayEn = String.format("%,.0f light-years", obj.distanceLightYears)
            )
            com.alijafari.red.astronomy.domain.ObjectType.STAR_CLUSTER, com.alijafari.red.astronomy.domain.ObjectType.GLOBULAR_CLUSTER -> PhysicalProperties(
                diameterKm = 1.5e14,
                diameterDisplayFa = "۱۵ تا ۱۰۰ سال نوری قطر خوشه",
                diameterDisplayEn = "15 to 100 light-years cluster diameter",
                massKgDisplayFa = "۱,۰۰۰ تا ۵۰۰,۰۰۰ ستاره هم‌تکامل",
                massKgDisplayEn = "1,000 to 500,000 co-eval stars",
                gravityMssDisplayFa = "پیوند گرانشی خود-تراکم ستاره‌ای",
                gravityMssDisplayEn = "Self-gravitating stellar cluster",
                distanceDisplayFa = String.format("%,.0f سال نوری", obj.distanceLightYears),
                distanceDisplayEn = String.format("%,.0f light-years", obj.distanceLightYears)
            )
            com.alijafari.red.astronomy.domain.ObjectType.CONSTELLATION -> PhysicalProperties(
                diameterKm = 0.0,
                diameterDisplayFa = "${obj.category} - محدوده رسمی IAU",
                diameterDisplayEn = "${obj.category} - Official IAU Boundary",
                massKgDisplayFa = "شامل ده‌ها ستاره بارز و اعماق فضا",
                massKgDisplayEn = "Contains multiple stars & DSOs",
                gravityMssDisplayFa = "چیدمان ستاره‌ای بر اساس خط دید زمین",
                gravityMssDisplayEn = "Line-of-sight stellar constellation",
                distanceDisplayFa = "محدوده زاویه‌ای در کره آسمان",
                distanceDisplayEn = "Angular coverage on celestial sphere"
            )
            com.alijafari.red.astronomy.domain.ObjectType.METEOR_SHOWER -> PhysicalProperties(
                diameterKm = 0.0,
                diameterDisplayFa = "رد ممان بارش: ${obj.zhr} شهاب در ساعت (ZHR)",
                diameterDisplayEn = "Zenithal Hourly Rate: ${obj.zhr} meteors/hr",
                massKgDisplayFa = "ذرات میلی‌متری ذوب‌شونده در جو",
                massKgDisplayEn = "Millimeter dust grains vaporizing in atmosphere",
                gravityMssDisplayFa = "سرعت ورود به جو: ۴۰ تا ۷۰ کیلومتر بر ثانیه",
                gravityMssDisplayEn = "Atmospheric entry velocity: 40-70 km/s",
                distanceDisplayFa = "ارتفاع سوختن: ۸۰ تا ۱۲۰ کیلومتر از سطح زمین",
                distanceDisplayEn = "Disintegration altitude: 80-120 km above Earth"
            )
            else -> PhysicalProperties(
                diameterKm = 10000.0,
                diameterDisplayFa = "محاسبه‌شده بر اساس داده‌های رصدی کاتالوگ",
                diameterDisplayEn = "Calculated based on catalog observation data",
                massKgDisplayFa = "جرم مشخصه در رده ${obj.category}",
                massKgDisplayEn = "Characteristic mass for ${obj.category}",
                gravityMssDisplayFa = "مشخصات گرانشی استاندارد رده",
                gravityMssDisplayEn = "Standard gravitational parameters",
                distanceDisplayFa = if (obj.distanceLightYears < 0.001) "< 1 AU" else String.format("%,.0f سال نوری", obj.distanceLightYears),
                distanceDisplayEn = if (obj.distanceLightYears < 0.001) "< 1 AU" else String.format("%,.0f light-years", obj.distanceLightYears)
            )
        }
    }

    fun getCoolFactsFa(obj: CelestialObject): List<String> {
        coolFactsMap[obj.id]?.let { return it }

        return when (obj.type) {
            com.alijafari.red.astronomy.domain.ObjectType.STAR -> listOf(
                "درخشش این ستاره با قدر ظاهری ${String.format("%.1f", obj.magnitude)} در صورت فلکی ${obj.constellationFa} قابل مشاهده است.",
                "فاصله تخمینی آن از منظومه شمسی حدود ${String.format("%,.0f", obj.distanceLightYears)} سال نوری می‌باشد.",
                "این ستاره دارای رده طیفی ${if (obj.spectralType.isNotEmpty()) obj.spectralType else "مشخص"} و دمای سطحی حدود ${if (obj.temperatureK > 0) obj.temperatureK else 6000} کلوین است.",
                "بهترین زمان رصد آن هنگام رسیدن به بالاترین نقطه ارتفاعی از افق (ترانزیت) است.",
                "استفاده از دوربین دوچشمی یا تلسکوپ رنگ و درخشش واقعی این ستاره را بهتر نمایان می‌کند."
            )
            com.alijafari.red.astronomy.domain.ObjectType.GALAXY -> listOf(
                "این کهکشان شامل میلیاردها ستاره، منظومه‌های سیاره‌ای و ابرهای غول‌پیکر گاز و غبار است.",
                "نور ساطع‌شده از این کهکشان پس از طی مسافت ${String.format("%,.0f", obj.distanceLightYears)} سال نوری به چشم رصدگر می‌رسد.",
                "برای رصد جزئیات ساختار آن، استفاده از تلسکوپ آماتوری با دهانه ۸ اینچ یا بزرگتر در محیط کویری توصیه می‌شود.",
                "در مرکز اکثر کهکشان‌ها یک سیاهچاله کلان‌جرم قرار دارد که حرکت ستارگان اطراف را هدایت می‌کند.",
                "این جرم در کاتالوگ نجومی RED به عنوان یکی از اهداف برجسته فوتومتری مشخص شده است."
            )
            com.alijafari.red.astronomy.domain.ObjectType.NEBULA -> listOf(
                "این سحابی زایشگاه یا بقایای تحول ستاره‌ای است که گاز هیدروژن در آن می‌درخشد.",
                "استفاده از فیلترهای نوری مانند O-III یا UHC کنتراست رصدی سحابی را به طرز چشمگیری افزایش می‌دهد.",
                "این جرم در فاصله ${String.format("%,.0f", obj.distanceLightYears)} سال نوری از کره زمین قرار دارد.",
                "تابش‌های فرابنفش ستارگان مجاور باعث برانگیختگی اتم‌های گاز و ایجاد رنگ‌های خیره‌کننده در عکاسی نجومی می‌شود.",
                "با چشم غیرمسلح یا دوربین دوچشمی، به صورت لکه‌ای مه‌آلود و لطیف دیده می‌شود."
            )
            com.alijafari.red.astronomy.domain.ObjectType.STAR_CLUSTER, com.alijafari.red.astronomy.domain.ObjectType.GLOBULAR_CLUSTER -> listOf(
                "این خوشه ستاره‌ای شامل مجموعه‌ای متراکم از ستارگان است که با گرانش متقابل به هم پیوند خورده‌اند.",
                "تمام ستارگان موجود در این خوشه تقریباً هم‌سن بوده و از یک ابر مولکولی واحد متولد شده‌اند.",
                "با دوربین دوچشمی ۷x۵۰ یا ۱۰x۵۰، ستارگان اصلی خوشه به شکل جواهری درخشان قابل تفکیک هستند.",
                "مطالعه این خوشه به اخترشناسان در درک تکامل ستاره‌ای و سن کهکشان کمک شایانی می‌کند.",
                "موقعیت زاویه‌ای آن در صورت فلکی ${obj.constellationFa} نوید یک هدف رصدی عالی را می‌دهد."
            )
            com.alijafari.red.astronomy.domain.ObjectType.CONSTELLATION -> listOf(
                "این صورت فلکی یکی از ۸۸ بخش رسمی کره آسمان است که توسط اتحادیه بین‌المللی اخترشناسی (IAU) تعریف شده است.",
                "ستارگان تشکیل‌دهنده این نقش‌واره در فواصل متفاوتی از زمین قرار دارند و تنها به دلیل زاویه دید ما در یک گروه دیده می‌شوند.",
                "در اساطیر و اخترشناسی باستان، الگوی ستارگان آن راهنمای جهت‌یابی دریانوردان و کشاورزان بوده است.",
                "با شناسایی ستارگان اصلی این صورت فلکی، می‌توانید جرم‌های اعماق فضا و سیارات همجوار را به راحتی پیدا کنید.",
                "بهترین زمان برای رصد کامل این صورت فلکی، شب‌های ماه روشن بدون حضور ماه کامل است."
            )
            com.alijafari.red.astronomy.domain.ObjectType.METEOR_SHOWER -> listOf(
                "کانون این بارش شهابی در صورت فلکی ${obj.constellationFa} قرار دارد.",
                "نرخ سمت‌الراسی بارش (ZHR) در زمان اوج به حدود ${obj.zhr} شهاب در ساعت می‌رسد.",
                "شهاب‌ها ناشی از برخورد ذرات ذوب‌شونده دنباله‌دار یا سیارک مادر با جو بالای زمین هستند.",
                "برای رصد بارش شهابی به هیچ تجهیزات نوری احتیاج ندارید؛ تنها به یک مکان تاریک و چشم غیرمسلح نیاز است.",
                "بهترین زاویه دید، خیره شدن به شعاع ۳۰ درجه‌ای اطراف کانون بارش است."
            )
            else -> listOf(
                "این جرم با قدر ظاهری ${String.format("%.1f", obj.magnitude)} یکی از نقاط رصدی جذاب در کاتالوگ RED است.",
                "موقعیت لحظه‌ای آن در آسمان بر اساس محاسبات ریاضی دقیق موتور نجومی تعیین می‌شود.",
                "رصد آن در شرایط بورتل ۱ تا ۴ بیشترین جزئیات را برای رصدگر نمایان می‌سازد.",
                "عبور آن از نصف‌النهار محلی بهترین شفافیت جوی را برای ثبت عکس‌های نجومی ارائه می‌دهد.",
                "اطلاعات مختصات (بعد و میل) آن در شناسنامه علمی به طور کامل درج شده است."
            )
        }
    }

    fun getCoolFactsEn(obj: CelestialObject): List<String> {
        coolFactsMapEn[obj.id]?.let { return it }

        return listOf(
            "Apparent magnitude of ${String.format("%.1f", obj.magnitude)} in constellation ${obj.constellationEn}.",
            "Estimated distance from Earth is approximately ${String.format("%,.0f", obj.distanceLightYears)} light-years.",
            "Spectral classification is ${if (obj.spectralType.isNotEmpty()) obj.spectralType else "Catalog Standard"}.",
            "Optimal viewing conditions occur around local meridian transit when altitude is maximized.",
            "Observing with binoculars or telescopes highlights subtle colors and fine structural details."
        )
    }
}
