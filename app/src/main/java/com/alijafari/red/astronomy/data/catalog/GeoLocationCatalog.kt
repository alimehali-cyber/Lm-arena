package com.alijafari.red.astronomy.data.catalog

import kotlin.math.*

/**
 * Authoritative Offline Geographic Database for RED Astronomy.
 * Bundles comprehensive Iranian cities & towns, all world country capitals,
 * and major global/astronomical sites for instant zero-latency offline searches.
 */
data class GeoCity(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 0.0,
    val timezoneId: String = "Asia/Tehran",
    val countryEn: String = "Iran",
    val countryFa: String = "ایران",
    val provinceEn: String = "",
    val provinceFa: String = "",
    val isIran: Boolean = true,
    val isCapital: Boolean = false,
    val alternateNames: List<String> = emptyList()
)

object GeoLocationCatalog {

    val NURABAD_CITY = GeoCity(
        id = "nurabad_nc",
        nameEn = "Nurabad City (NC)",
        nameFa = "نورآباد ممسنی (NC)",
        latitude = 30.1141,
        longitude = 51.5217,
        elevationMeters = 940.0,
        timezoneId = "Asia/Tehran",
        countryEn = "Iran",
        countryFa = "ایران",
        provinceEn = "Fars",
        provinceFa = "فارس",
        isIran = true,
        isCapital = false,
        alternateNames = listOf("Noorabad", "Mamasani", "Noorabad Mamasani", "نورآباد", "ممسنی", "نوراباد", "نوراباد ممسنی")
    )

    private val IRAN_PROVINCIAL_CAPITALS = listOf(
        NURABAD_CITY,
        GeoCity("tehran", "Tehran", "تهران", 35.6892, 51.3890, 1189.0, "Asia/Tehran", "Iran", "ایران", "Tehran", "تهران", isIran = true, isCapital = true, alternateNames = listOf("طهران")),
        GeoCity("shiraz", "Shiraz", "شیراز", 29.5918, 52.5837, 1500.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, isCapital = true),
        GeoCity("isfahan", "Isfahan", "اصفهان", 32.6546, 51.6680, 1574.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, isCapital = true, alternateNames = listOf("Esfahan", "سپاهان")),
        GeoCity("tabriz", "Tabriz", "تبریز", 38.0962, 46.2694, 1340.0, "Asia/Tehran", "Iran", "ایران", "East Azerbaijan", "آذربایجان شرقی", isIran = true, isCapital = true),
        GeoCity("mashhad", "Mashhad", "مشهد", 36.2972, 59.6067, 985.0, "Asia/Tehran", "Iran", "ایران", "Khorasan Razavi", "خراسان رضوی", isIran = true, isCapital = true),
        GeoCity("kerman", "Kerman", "کرمان", 30.2839, 57.0834, 1756.0, "Asia/Tehran", "Iran", "ایران", "Kerman", "کرمان", isIran = true, isCapital = true),
        GeoCity("ahvaz", "Ahvaz", "اهواز", 31.3183, 48.6706, 17.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, isCapital = true, alternateNames = listOf("Ahwaz")),
        GeoCity("rasht", "Rasht", "رشت", 37.2808, 49.5832, 5.0, "Asia/Tehran", "Iran", "ایران", "Gilan", "گیلان", isIran = true, isCapital = true),
        GeoCity("yazd", "Yazd", "یزد", 31.8974, 54.3675, 1216.0, "Asia/Tehran", "Iran", "ایران", "Yazd", "یزد", isIran = true, isCapital = true),
        GeoCity("kermanshah", "Kermanshah", "کرمانشاه", 34.3142, 47.0650, 1350.0, "Asia/Tehran", "Iran", "ایران", "Kermanshah", "کرمانشاه", isIran = true, isCapital = true, alternateNames = listOf("باختران")),
        GeoCity("hamadan", "Hamadan", "همدان", 34.7982, 48.5146, 1850.0, "Asia/Tehran", "Iran", "ایران", "Hamadan", "همدان", isIran = true, isCapital = true, alternateNames = listOf("Hamedan", "هگمتانه")),
        GeoCity("zahedan", "Zahedan", "زاهدان", 29.4963, 60.8629, 1352.0, "Asia/Tehran", "Iran", "ایران", "Sistan and Baluchestan", "سیستان و بلوچستان", isIran = true, isCapital = true),
        GeoCity("bandar_abbas", "Bandar Abbas", "بندرعباس", 27.1832, 56.2666, 9.0, "Asia/Tehran", "Iran", "ایران", "Hormozgan", "هرمزگان", isIran = true, isCapital = true, alternateNames = listOf("بندر عباس", "گمبرون")),
        GeoCity("sanandaj", "Sanandaj", "سنندج", 35.3144, 46.9923, 1538.0, "Asia/Tehran", "Iran", "ایران", "Kurdistan", "کردستان", isIran = true, isCapital = true, alternateNames = listOf("سنه")),
        GeoCity("bushehr", "Bushehr", "بوشهر", 28.9234, 50.8382, 8.0, "Asia/Tehran", "Iran", "ایران", "Bushehr", "بوشهر", isIran = true, isCapital = true, alternateNames = listOf("بوشهر")),
        GeoCity("khorramabad", "Khorramabad", "خرم‌آباد", 33.4878, 48.3558, 1147.0, "Asia/Tehran", "Iran", "ایران", "Lorestan", "لرستان", isIran = true, isCapital = true, alternateNames = listOf("خرم اباد")),
        GeoCity("sari", "Sari", "ساری", 36.5633, 53.0601, 43.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true, isCapital = true),
        GeoCity("gorgan", "Gorgan", "گرگان", 36.8456, 54.4393, 155.0, "Asia/Tehran", "Iran", "ایران", "Golestan", "گلستان", isIran = true, isCapital = true, alternateNames = listOf("استرآباد")),
        GeoCity("bojnord", "Bojnord", "بجنورد", 37.4761, 57.3283, 1070.0, "Asia/Tehran", "Iran", "ایران", "North Khorasan", "خراسان شمالی", isIran = true, isCapital = true, alternateNames = listOf("Bojnourd")),
        GeoCity("birjand", "Birjand", "بیرجند", 32.8663, 59.2211, 1491.0, "Asia/Tehran", "Iran", "ایران", "South Khorasan", "خراسان جنوبی", isIran = true, isCapital = true),
        GeoCity("ilam", "Ilam", "ایلام", 33.6374, 46.4227, 1403.0, "Asia/Tehran", "Iran", "ایران", "Ilam", "ایلام", isIran = true, isCapital = true),
        GeoCity("shahr_e_kord", "Shahr-e Kord", "شهرکرد", 32.3256, 50.8644, 2070.0, "Asia/Tehran", "Iran", "ایران", "Chaharmahal and Bakhtiari", "چهارمحال و بختیاری", isIran = true, isCapital = true, alternateNames = listOf("Shahrekord", "شهر کرد", "دهکرد")),
        GeoCity("yasuj", "Yasuj", "یاسوج", 30.6684, 51.5876, 1870.0, "Asia/Tehran", "Iran", "ایران", "Kohgiluyeh and Boyer-Ahmad", "کهگیلویه و بویراحمد", isIran = true, isCapital = true),
        GeoCity("semnan", "Semnan", "سمنان", 35.5769, 53.3953, 1130.0, "Asia/Tehran", "Iran", "ایران", "Semnan", "سمنان", isIran = true, isCapital = true),
        GeoCity("zanjan", "Zanjan", "زنجان", 36.6736, 48.4787, 1638.0, "Asia/Tehran", "Iran", "ایران", "Zanjan", "زنجان", isIran = true, isCapital = true),
        GeoCity("qazvin", "Qazvin", "قزوین", 36.2688, 50.0041, 1297.0, "Asia/Tehran", "Iran", "ایران", "Qazvin", "قزوین", isIran = true, isCapital = true, alternateNames = listOf("Ghazvin", "کاسپین")),
        GeoCity("qom", "Qom", "قم", 34.6399, 50.8759, 933.0, "Asia/Tehran", "Iran", "ایران", "Qom", "قم", isIran = true, isCapital = true),
        GeoCity("arak", "Arak", "اراک", 34.0954, 49.7013, 1755.0, "Asia/Tehran", "Iran", "ایران", "Markazi", "مرکزی", isIran = true, isCapital = true, alternateNames = listOf("سلطان آباد")),
        GeoCity("ardabil", "Ardabil", "اردبیل", 38.2498, 48.2933, 1351.0, "Asia/Tehran", "Iran", "ایران", "Ardabil", "اردبیل", isIran = true, isCapital = true, alternateNames = listOf("Ardebil")),
        GeoCity("urmia", "Urmia", "ارومیه", 37.5527, 45.0761, 1332.0, "Asia/Tehran", "Iran", "ایران", "West Azerbaijan", "آذربایجان غربی", isIran = true, isCapital = true, alternateNames = listOf("Orumiyeh", "رضائیه")),
        GeoCity("karaj", "Karaj", "کرج", 35.8400, 50.9391, 1312.0, "Asia/Tehran", "Iran", "ایران", "Alborz", "البرز", isIran = true, isCapital = true)
    )

    private val IRAN_NOTABLE_CITIES = listOf(
        GeoCity("safashahr", "Safashahr (Fars)", "صفاشهر (فارس)", 30.6158, 53.1956, 2300.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("دهبید", "خرمبید", "Safashahr")),
        GeoCity("kazerun", "Kazerun", "کازرون", 29.6195, 51.6541, 860.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("Kazeroun", "کازران")),
        GeoCity("marvdasht", "Marvdasht", "مرودشت", 29.8742, 52.8025, 1595.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("تخت جمشید", "Persepolis")),
        GeoCity("jahrom", "Jahrom", "جهرم", 28.5003, 53.5606, 1050.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("fasa", "Fasa", "فسا", 28.9383, 53.6481, 1450.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("lar", "Lar", "لار", 27.6833, 54.3417, 915.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("لارستان", "Larestan")),
        GeoCity("abadeh", "Abadeh", "آباده", 31.1608, 52.6506, 2011.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("اباده")),
        GeoCity("neyriz", "Neyriz", "نی‌ریز", 29.1986, 54.3278, 1605.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("نی ریز")),
        GeoCity("darab", "Darab", "داراب", 28.7519, 54.5444, 1120.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("eqlid", "Eqlid", "اقلید", 30.8983, 52.6892, 2250.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("firuzabad_fars", "Firuzabad", "فیروزآباد", 28.8438, 52.5708, 1330.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("فیروزاباد", "گور")),
        GeoCity("lamerd", "Lamerd", "لامرد", 27.3339, 53.1789, 415.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("gerash", "Gerash", "گراش", 27.6653, 54.1372, 912.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("sarvestan", "Sarvestan", "سروستان", 29.2731, 53.2217, 1510.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("masiri", "Masiri (Rostam)", "مصیری (رستم)", 30.2458, 51.5244, 910.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("رستم", "Mamasani Rostam")),
        GeoCity("arsanjan", "Arsanjan", "ارسنجان", 29.9142, 53.3089, 1630.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true),
        GeoCity("bavanat", "Bavanat", "بوانات", 30.4633, 53.6494, 2100.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("سوریان")),
        GeoCity("pasargad", "Pasargad", "پاسارگاد", 30.1983, 53.1811, 1900.0, "Asia/Tehran", "Iran", "ایران", "Fars", "فارس", isIran = true, alternateNames = listOf("سعادت شهر", "آرامگاه کوروش")),
        GeoCity("kashan", "Kashan", "کاشان", 33.9850, 51.4100, 950.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("نیاسر", "قمصر", "Kachan")),
        GeoCity("maranjab", "Maranjab Observatory", "رصدخانه کویر مرنجاب", 34.2989, 51.9056, 850.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("مرنجاب", "آران و بیدگل")),
        GeoCity("ino_observatory", "Iranian National Observatory (Mount Gargash)", "رصدخانه ملی ایران (گرگش)", 33.6706, 51.3208, 3600.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("INO", "گرگش", "کامو")),
        GeoCity("najafabad", "Najafabad", "نجف‌آباد", 32.6342, 51.3667, 1600.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("نجف اباد")),
        GeoCity("shahin_shahr", "Shahin Shahr", "شاهین‌شهر", 32.8639, 51.5544, 1590.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("شاهین شهر")),
        GeoCity("khomeini_shahr", "Khomeini Shahr", "خمینی‌شهر", 32.7000, 51.5200, 1590.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("سده", "خمینی شهر")),
        GeoCity("golpayegan", "Golpayegan", "گلپایگان", 33.4539, 50.2883, 1830.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true),
        GeoCity("natanz", "Natanz", "نطنز", 33.5131, 51.9167, 1600.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true),
        GeoCity("nain", "Na'in", "نائین", 32.8600, 53.0800, 1545.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("نایین")),
        GeoCity("shahreza", "Shahreza", "شهرضا", 32.0089, 51.8656, 1825.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true, alternateNames = listOf("قمشه")),
        GeoCity("semirom", "Semirom", "سمیرم", 31.4144, 51.5694, 2400.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true),
        GeoCity("khansar", "Khansar", "خوانسار", 33.2206, 50.3150, 2250.0, "Asia/Tehran", "Iran", "ایران", "Isfahan", "اصفهان", isIran = true),
        GeoCity("nishapur", "Nishapur", "نیشابور", 36.2133, 58.7958, 1250.0, "Asia/Tehran", "Iran", "ایران", "Khorasan Razavi", "خراسان رضوی", isIran = true, alternateNames = listOf("Neyshabur", "خیام")),
        GeoCity("sabzevar", "Sabzevar", "سبزوار", 36.2125, 57.6778, 975.0, "Asia/Tehran", "Iran", "ایران", "Khorasan Razavi", "خراسان رضوی", isIran = true, alternateNames = listOf("بیهق")),
        GeoCity("torbat_heydarieh", "Torbat-e Heydarieh", "تربت حیدریه", 35.2742, 59.2197, 1333.0, "Asia/Tehran", "Iran", "ایران", "Khorasan Razavi", "خراسان رضوی", isIran = true),
        GeoCity("quchan", "Quchan", "قوچان", 37.1061, 58.5097, 1317.0, "Asia/Tehran", "Iran", "ایران", "Khorasan Razavi", "خراسان رضوی", isIran = true),
        GeoCity("kashmar", "Kashmar", "کاشمر", 35.2383, 58.4656, 1052.0, "Asia/Tehran", "Iran", "ایران", "Khorasan Razavi", "خراسان رضوی", isIran = true, alternateNames = listOf("ترشیز")),
        GeoCity("gonabad", "Gonabad", "گناباد", 34.3528, 58.6836, 1096.0, "Asia/Tehran", "Iran", "ایران", "Khorasan Razavi", "خراسان رضوی", isIran = true),
        GeoCity("maragheh", "Maragheh (Observatory)", "مراغه (رصدخانه تاریخی)", 37.3917, 46.2392, 1485.0, "Asia/Tehran", "Iran", "ایران", "East Azerbaijan", "آذربایجان شرقی", isIran = true, alternateNames = listOf("رصدخانه مراغه", "خواجه نصیر")),
        GeoCity("marand", "Marand", "مرند", 38.4328, 45.7747, 1334.0, "Asia/Tehran", "Iran", "ایران", "East Azerbaijan", "آذربایجان شرقی", isIran = true),
        GeoCity("mianeh", "Mianeh", "میانه", 37.4228, 47.7153, 1100.0, "Asia/Tehran", "Iran", "ایران", "East Azerbaijan", "آذربایجان شرقی", isIran = true),
        GeoCity("ahar", "Ahar", "اهر", 38.4772, 47.0697, 1341.0, "Asia/Tehran", "Iran", "ایران", "East Azerbaijan", "آذربایجان شرقی", isIran = true, alternateNames = listOf("ارسباران")),
        GeoCity("bonab", "Bonab", "بناب", 37.3400, 46.0561, 1290.0, "Asia/Tehran", "Iran", "ایران", "East Azerbaijan", "آذربایجان شرقی", isIran = true),
        GeoCity("jolfa", "Jolfa", "جلفا", 38.9392, 45.6308, 710.0, "Asia/Tehran", "Iran", "ایران", "East Azerbaijan", "آذربایجان شرقی", isIran = true, alternateNames = listOf("ارس", "Aras")),
        GeoCity("khoy", "Khoy", "خوی", 38.5503, 44.9581, 1139.0, "Asia/Tehran", "Iran", "ایران", "West Azerbaijan", "آذربایجان غربی", isIran = true),
        GeoCity("bukan", "Bukan", "بوکان", 36.5208, 46.2089, 1370.0, "Asia/Tehran", "Iran", "ایران", "West Azerbaijan", "آذربایجان غربی", isIran = true, alternateNames = listOf("Boukan")),
        GeoCity("mahabad", "Mahabad", "مهاباد", 36.7631, 45.7222, 1320.0, "Asia/Tehran", "Iran", "ایران", "West Azerbaijan", "آذربایجان غربی", isIran = true, alternateNames = listOf("سابلاغ")),
        GeoCity("miandoab", "Miandoab", "میاندوآب", 36.9686, 46.1039, 1314.0, "Asia/Tehran", "Iran", "ایران", "West Azerbaijan", "آذربایجان غربی", isIran = true, alternateNames = listOf("میاندواب")),
        GeoCity("salmas", "Salmas", "سلماس", 38.1972, 44.7653, 1396.0, "Asia/Tehran", "Iran", "ایران", "West Azerbaijan", "آذربایجان غربی", isIran = true, alternateNames = listOf("شاپور")),
        GeoCity("maku", "Maku", "ماکو", 39.2944, 44.5167, 1290.0, "Asia/Tehran", "Iran", "ایران", "West Azerbaijan", "آذربایجان غربی", isIran = true),
        GeoCity("dezful", "Dezful", "دزفول", 32.3811, 48.4058, 143.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, alternateNames = listOf("دژپل")),
        GeoCity("abadan", "Abadan", "آبادان", 30.3392, 48.3042, 3.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, alternateNames = listOf("ابادان")),
        GeoCity("khorramshahr", "Khorramshahr", "خرمشهر", 30.4397, 48.1808, 3.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, alternateNames = listOf("محمره")),
        GeoCity("mahshahr", "Bandar Mahshahr", "بندر ماهشهر", 30.5589, 49.1981, 3.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, alternateNames = listOf("ماهشهر")),
        GeoCity("andimeshk", "Andimeshk", "اندیمشک", 32.4600, 48.3500, 176.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true),
        GeoCity("izeh", "Izeh", "ایذه", 31.8328, 49.8694, 824.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, alternateNames = listOf("مالمیر", "ایذه")),
        GeoCity("behbahan", "Behbahan", "بهبهان", 30.5958, 50.2417, 325.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, alternateNames = listOf("ارجان")),
        GeoCity("shush", "Shush (Susa)", "شوش (باستانی)", 32.1942, 48.2436, 87.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true, alternateNames = listOf("Susa", "چغازنبیل")),
        GeoCity("shushtar", "Shushtar", "شوشتر", 32.0456, 48.8567, 68.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true),
        GeoCity("masjed_soleyman", "Masjed Soleyman", "مسجد سلیمان", 31.9364, 49.3039, 372.0, "Asia/Tehran", "Iran", "ایران", "Khuzestan", "خوزستان", isIran = true),
        GeoCity("bandar_anzali", "Bandar Anzali", "بندر انزلی", 37.4744, 49.4625, -26.0, "Asia/Tehran", "Iran", "ایران", "Gilan", "گیلان", isIran = true, alternateNames = listOf("انزلی", "بندر پهلوی")),
        GeoCity("lahijan", "Lahijan", "لاهیجان", 37.2072, 50.0033, 4.0, "Asia/Tehran", "Iran", "ایران", "Gilan", "گیلان", isIran = true),
        GeoCity("langarud", "Langarud", "لنگرود", 37.1972, 50.1536, 21.0, "Asia/Tehran", "Iran", "ایران", "Gilan", "گیلان", isIran = true),
        GeoCity("talesh", "Talesh", "تالش (هشتپر)", 37.7997, 48.9064, 54.0, "Asia/Tehran", "Iran", "ایران", "Gilan", "گیلان", isIran = true),
        GeoCity("astara", "Astara", "آستارا", 38.4294, 48.8722, -21.0, "Asia/Tehran", "Iran", "ایران", "Gilan", "گیلان", isIran = true),
        GeoCity("fuman", "Fuman", "فومن", 37.2239, 49.3125, 29.0, "Asia/Tehran", "Iran", "ایران", "Gilan", "گیلان", isIran = true, alternateNames = listOf("ماسوله", "قلعه رودخان")),
        GeoCity("babol", "Babol", "بابل", 36.5514, 52.6789, -2.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true, alternateNames = listOf("بارفروش")),
        GeoCity("amol", "Amol", "آمل", 36.4678, 52.3508, 76.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true, alternateNames = listOf("دماوند")),
        GeoCity("qaem_shahr", "Qaem Shahr", "قائم‌شهر", 36.4639, 52.8597, 51.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true, alternateNames = listOf("شاهی", "قائم شهر")),
        GeoCity("behshahr", "Behshahr", "بهشهر", 36.6978, 53.5539, 20.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true, alternateNames = listOf("اشرف")),
        GeoCity("chalus", "Chalus", "چالوس", 36.6550, 51.4206, 23.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true),
        GeoCity("tonekabon", "Tonekabon", "تنکابن (شهسوار)", 36.8164, 50.8739, 20.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true, alternateNames = listOf("شهسوار")),
        GeoCity("ramsar", "Ramsar", "رامسر", 36.9175, 50.6447, -20.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true, alternateNames = listOf("سخت‌سر")),
        GeoCity("noshahr", "Noshahr", "نوشهر", 36.6489, 51.4961, -15.0, "Asia/Tehran", "Iran", "ایران", "Mazandaran", "مازندران", isIran = true),
        GeoCity("rafsanjan", "Rafsanjan", "رفسنجان", 30.4067, 55.9939, 1515.0, "Asia/Tehran", "Iran", "ایران", "Kerman", "کرمان", isIran = true),
        GeoCity("sirjan", "Sirjan", "سیرجان", 29.4522, 55.6814, 1730.0, "Asia/Tehran", "Iran", "ایران", "Kerman", "کرمان", isIran = true),
        GeoCity("jiroft", "Jiroft", "جیرفت", 28.6781, 57.7406, 685.0, "Asia/Tehran", "Iran", "ایران", "Kerman", "کرمان", isIran = true, alternateNames = listOf("تمدن ارت")),
        GeoCity("bam", "Bam", "بم", 29.1061, 58.3572, 1060.0, "Asia/Tehran", "Iran", "ایران", "Kerman", "کرمان", isIran = true, alternateNames = listOf("ارگ بم")),
        GeoCity("zarand", "Zarand", "زرند", 30.8128, 56.5639, 1650.0, "Asia/Tehran", "Iran", "ایران", "Kerman", "کرمان", isIran = true),
        GeoCity("eslamabad_gharb", "Eslamabad-e Gharb", "اسلام‌آباد غرب", 34.1094, 46.5275, 1335.0, "Asia/Tehran", "Iran", "ایران", "Kermanshah", "کرمانشاه", isIran = true, alternateNames = listOf("شاه‌آباد")),
        GeoCity("paveh", "Paveh", "پاوه", 35.0433, 46.3622, 1540.0, "Asia/Tehran", "Iran", "ایران", "Kermanshah", "کرمانشاه", isIran = true, alternateNames = listOf("اورامانات")),
        GeoCity("malayer", "Malayer", "ملایر", 34.2969, 48.8236, 1725.0, "Asia/Tehran", "Iran", "ایران", "Hamadan", "همدان", isIran = true),
        GeoCity("nahavand", "Nahavand", "نهاوند", 34.1886, 48.3769, 1640.0, "Asia/Tehran", "Iran", "ایران", "Hamadan", "همدان", isIran = true),
        GeoCity("tuyserkan", "Tuyserkan", "تویسرکان", 34.5483, 48.4467, 1780.0, "Asia/Tehran", "Iran", "ایران", "Hamadan", "همدان", isIran = true),
        GeoCity("borujerd", "Borujerd", "بروجرد", 33.8972, 48.7517, 1550.0, "Asia/Tehran", "Iran", "ایران", "Lorestan", "لرستان", isIran = true),
        GeoCity("dorud", "Dorud", "دورود", 33.4933, 49.0750, 1450.0, "Asia/Tehran", "Iran", "ایران", "Lorestan", "لرستان", isIran = true, alternateNames = listOf("پایتخت طبیعت ایران", "دریاچه گهر")),
        GeoCity("aligudarz", "Aligudarz", "الیگودرز", 33.4006, 49.6947, 2022.0, "Asia/Tehran", "Iran", "ایران", "Lorestan", "لرستان", isIran = true),
        GeoCity("kuhdasht", "Kuhdasht", "کوهدشت", 33.5336, 47.6061, 1195.0, "Asia/Tehran", "Iran", "ایران", "Lorestan", "لرستان", isIran = true),
        GeoCity("nurabad_delfan", "Nurabad (Delfan)", "نورآباد (دلفان لرستان)", 34.0733, 47.9725, 1850.0, "Asia/Tehran", "Iran", "ایران", "Lorestan", "لرستان", isIran = true, alternateNames = listOf("دلفان")),
        GeoCity("meybod", "Meybod", "میبد", 32.2500, 54.0167, 1109.0, "Asia/Tehran", "Iran", "ایران", "Yazd", "یزد", isIran = true),
        GeoCity("ardakan", "Ardakan", "اردکان", 32.3100, 54.0175, 1035.0, "Asia/Tehran", "Iran", "ایران", "Yazd", "یزد", isIran = true),
        GeoCity("bafq", "Bafq", "بافق", 31.6042, 55.4056, 996.0, "Asia/Tehran", "Iran", "ایران", "Yazd", "یزد", isIran = true),
        GeoCity("minab", "Minab", "میناب", 27.1467, 57.0800, 27.0, "Asia/Tehran", "Iran", "ایران", "Hormozgan", "هرمزگان", isIran = true),
        GeoCity("qeshm", "Qeshm Island", "جزیره قشم", 26.9581, 56.2719, 15.0, "Asia/Tehran", "Iran", "ایران", "Hormozgan", "هرمزگان", isIran = true, alternateNames = listOf("قشم", "ژئوپارک قشم")),
        GeoCity("kish", "Kish Island", "جزیره کیش", 26.5325, 53.9789, 10.0, "Asia/Tehran", "Iran", "ایران", "Hormozgan", "هرمزگان", isIran = true, alternateNames = listOf("کیش")),
        GeoCity("bandar_lengeh", "Bandar Lengeh", "بندر لنگه", 26.5578, 54.8808, 12.0, "Asia/Tehran", "Iran", "ایران", "Hormozgan", "هرمزگان", isIran = true),
        GeoCity("jask", "Jask", "بندر جاسک", 25.6439, 57.7744, 5.0, "Asia/Tehran", "Iran", "ایران", "Hormozgan", "هرمزگان", isIran = true),
        GeoCity("borazjan", "Borazjan", "برازجان", 29.2667, 51.2167, 70.0, "Asia/Tehran", "Iran", "ایران", "Bushehr", "بوشهر", isIran = true, alternateNames = listOf("دشتستان")),
        GeoCity("bandar_ganaveh", "Bandar Ganaveh", "بندر گناوه", 29.5792, 50.5175, 5.0, "Asia/Tehran", "Iran", "ایران", "Bushehr", "بوشهر", isIran = true, alternateNames = listOf("گناوه")),
        GeoCity("kangan", "Kangan", "بندر کنگان", 27.8339, 52.0642, 8.0, "Asia/Tehran", "Iran", "ایران", "Bushehr", "بوشهر", isIran = true),
        GeoCity("asaluyeh", "Asaluyeh", "عسلویه", 27.4761, 52.6078, 5.0, "Asia/Tehran", "Iran", "ایران", "Bushehr", "بوشهر", isIran = true, alternateNames = listOf("پارس جنوبی")),
        GeoCity("iranshahr", "Iranshahr", "ایرانشهر", 27.2025, 60.6847, 565.0, "Asia/Tehran", "Iran", "ایران", "Sistan and Baluchestan", "سیستان و بلوچستان", isIran = true),
        GeoCity("chabahar", "Chabahar", "بندر چابهار", 25.2919, 60.6431, 11.0, "Asia/Tehran", "Iran", "ایران", "Sistan and Baluchestan", "سیستان و بلوچستان", isIran = true, alternateNames = listOf("چابهار", "مکران")),
        GeoCity("zabol", "Zabol", "زابل", 31.0308, 61.4947, 480.0, "Asia/Tehran", "Iran", "ایران", "Sistan and Baluchestan", "سیستان و بلوچستان", isIran = true, alternateNames = listOf("شهر سوخته", "نیمروز")),
        GeoCity("saravan", "Saravan", "سراوان", 27.3711, 62.3342, 1195.0, "Asia/Tehran", "Iran", "ایران", "Sistan and Baluchestan", "سیستان و بلوچستان", isIran = true),
        GeoCity("saqqez", "Saqqez", "سقز", 36.2497, 46.2733, 1493.0, "Asia/Tehran", "Iran", "ایران", "Kurdistan", "کردستان", isIran = true),
        GeoCity("marivan", "Marivan", "مریوان", 35.5269, 46.1764, 1290.0, "Asia/Tehran", "Iran", "ایران", "Kurdistan", "کردستان", isIran = true, alternateNames = listOf("دریاچه زریوار")),
        GeoCity("baneh", "Baneh", "بانه", 35.9975, 45.8853, 1530.0, "Asia/Tehran", "Iran", "ایران", "Kurdistan", "کردستان", isIran = true),
        GeoCity("saveh", "Saveh", "ساوه", 35.0214, 50.3569, 1008.0, "Asia/Tehran", "Iran", "ایران", "Markazi", "مرکزی", isIran = true),
        GeoCity("khomeyn", "Khomeyn", "خمین", 33.6422, 50.0789, 1800.0, "Asia/Tehran", "Iran", "ایران", "Markazi", "مرکزی", isIran = true),
        GeoCity("mahallat", "Mahallat", "محلات", 33.9139, 50.4578, 1750.0, "Asia/Tehran", "Iran", "ایران", "Markazi", "مرکزی", isIran = true),
        GeoCity("parsabad", "Parsabad", "پارس‌آباد مغان", 39.6486, 47.9172, 45.0, "Asia/Tehran", "Iran", "ایران", "Ardabil", "اردبیل", isIran = true, alternateNames = listOf("مغان")),
        GeoCity("meshginshahr", "Meshginshahr", "مشگین‌شهر (سبلان)", 38.3989, 47.6822, 1400.0, "Asia/Tehran", "Iran", "ایران", "Ardabil", "اردبیل", isIran = true, alternateNames = listOf("سبلان", "خیاو")),
        GeoCity("sareyn", "Sareyn", "سرعین", 38.1517, 48.0708, 1650.0, "Asia/Tehran", "Iran", "ایران", "Ardabil", "اردبیل", isIran = true),
        GeoCity("takestan", "Takestan", "تاکستان", 36.0697, 49.6958, 1265.0, "Asia/Tehran", "Iran", "ایران", "Qazvin", "قزوین", isIran = true, alternateNames = listOf("سیادهن")),
        GeoCity("abhar", "Abhar", "ابهر", 36.1467, 49.2181, 1540.0, "Asia/Tehran", "Iran", "ایران", "Zanjan", "زنجان", isIran = true),
        GeoCity("gonbad_kavus", "Gonbad-e Kavus", "گنبد کاووس", 37.2500, 55.1672, 52.0, "Asia/Tehran", "Iran", "ایران", "Golestan", "گلستان", isIran = true, alternateNames = listOf("گنبد قابوس", "جرجان")),
        GeoCity("bandar_torkaman", "Bandar Torkaman", "بندر ترکمن", 36.9000, 54.0333, -20.0, "Asia/Tehran", "Iran", "ایران", "Golestan", "گلستان", isIran = true, alternateNames = listOf("بندر شاه", "جزیره آشوراده")),
        GeoCity("shahroud", "Shahroud", "شاهرود", 36.4181, 54.9764, 1345.0, "Asia/Tehran", "Iran", "ایران", "Semnan", "سمنان", isIran = true, alternateNames = listOf("جنگل ابر", "بسطام")),
        GeoCity("damghan", "Damghan", "دامغان", 36.1681, 54.3467, 1170.0, "Asia/Tehran", "Iran", "ایران", "Semnan", "سمنان", isIran = true, alternateNames = listOf("صد دروازه")),
        GeoCity("borujen", "Borujen", "بروجن", 31.9667, 51.2889, 2220.0, "Asia/Tehran", "Iran", "ایران", "Chaharmahal and Bakhtiari", "چهارمحال و بختیاری", isIran = true, alternateNames = listOf("سقف ایران")),
        GeoCity("dogonbadan", "Dogonbadan (Gachsaran)", "دوگنبدان (گچساران)", 30.3586, 50.7981, 736.0, "Asia/Tehran", "Iran", "ایران", "Kohgiluyeh and Boyer-Ahmad", "کهگیلویه و بویراحمد", isIran = true, alternateNames = listOf("گچساران", "Gachsaran")),
        GeoCity("dehdasht", "Dehdasht", "دهدشت", 30.7936, 50.5647, 850.0, "Asia/Tehran", "Iran", "ایران", "Kohgiluyeh and Boyer-Ahmad", "کهگیلویه و بویراحمد", isIran = true, alternateNames = listOf("بلاد شاپور")),
        GeoCity("sisakht", "Sisakht (Dena)", "سی‌سخت (دنا)", 30.8639, 51.4561, 2250.0, "Asia/Tehran", "Iran", "ایران", "Kohgiluyeh and Boyer-Ahmad", "کهگیلویه و بویراحمد", isIran = true, alternateNames = listOf("دنا", "قله دنا")),
        GeoCity("dehoran", "Dehloran", "دهلران", 32.6942, 47.2678, 230.0, "Asia/Tehran", "Iran", "ایران", "Ilam", "ایلام", isIran = true),
        GeoCity("mehran", "Mehran", "مهران", 33.1222, 46.1647, 136.0, "Asia/Tehran", "Iran", "ایران", "Ilam", "ایلام", isIran = true),
        GeoCity("shirvan", "Shirvan", "شیروان", 37.3967, 57.9294, 1097.0, "Asia/Tehran", "Iran", "ایران", "North Khorasan", "خراسان شمالی", isIran = true),
        GeoCity("esfarayen", "Esfarayen", "اسفراین", 37.0764, 57.5100, 1260.0, "Asia/Tehran", "Iran", "ایران", "North Khorasan", "خراسان شمالی", isIran = true, alternateNames = listOf("شهر بلقیس")),
        GeoCity("qaen", "Qaen", "قائن", 33.7272, 59.1844, 1440.0, "Asia/Tehran", "Iran", "ایران", "South Khorasan", "خراسان جنوبی", isIran = true, alternateNames = listOf("پایتخت زعفران")),
        GeoCity("ferdows", "Ferdows", "فردوس", 34.0186, 58.1722, 1293.0, "Asia/Tehran", "Iran", "ایران", "South Khorasan", "خراسان جنوبی", isIran = true, alternateNames = listOf("تون")),
        GeoCity("tabas", "Tabas", "طبس", 33.5958, 56.9244, 690.0, "Asia/Tehran", "Iran", "ایران", "South Khorasan", "خراسان جنوبی", isIran = true, alternateNames = listOf("عروس کویر")),
        GeoCity("varamin", "Varamin", "ورامین", 35.3242, 51.6481, 915.0, "Asia/Tehran", "Iran", "ایران", "Tehran", "تهران", isIran = true),
        GeoCity("shahriar", "Shahriar", "شهریار", 35.6597, 51.0592, 1160.0, "Asia/Tehran", "Iran", "ایران", "Tehran", "تهران", isIran = true),
        GeoCity("eslamshahr", "Eslamshahr", "اسلامشهر", 35.5392, 51.2339, 1065.0, "Asia/Tehran", "Iran", "ایران", "Tehran", "تهران", isIran = true),
        GeoCity("damavand", "Damavand City", "دماوند", 35.7178, 52.0650, 1960.0, "Asia/Tehran", "Iran", "ایران", "Tehran", "تهران", isIran = true, alternateNames = listOf("قله دماوند")),
        GeoCity("rey", "Shahr-e Rey", "شهر ری", 35.5947, 51.4383, 1060.0, "Asia/Tehran", "Iran", "ایران", "Tehran", "تهران", isIran = true, alternateNames = listOf("ری", "راگا")),
        GeoCity("shemiran", "Shemiranat", "شمیرانات (تجریش)", 35.8053, 51.4342, 1600.0, "Asia/Tehran", "Iran", "ایران", "Tehran", "تهران", isIran = true, alternateNames = listOf("تجریش", "دربند", "نیاوران", "توچال"))
    )

    private val WORLD_CAPITALS_AND_MAJOR_CITIES = listOf(
        // Middle East & Neighbors
        GeoCity("baghdad", "Baghdad", "بغداد", 33.3152, 44.3661, 34.0, "Asia/Baghdad", "Iraq", "عراق", isIran = false, isCapital = true),
        GeoCity("erbil", "Erbil", "اربیل", 36.1901, 44.0091, 390.0, "Asia/Baghdad", "Iraq", "عراق", isIran = false, alternateNames = listOf("Hewlêr", "هولیر")),
        GeoCity("basra", "Basra", "بصره", 30.5085, 47.7804, 5.0, "Asia/Baghdad", "Iraq", "عراق", isIran = false),
        GeoCity("najaf", "Najaf", "نجف اشرف", 31.9961, 44.3314, 70.0, "Asia/Baghdad", "Iraq", "عراق", isIran = false),
        GeoCity("karbala", "Karbala", "کربلا معلی", 32.6160, 44.0249, 30.0, "Asia/Baghdad", "Iraq", "عراق", isIran = false),
        GeoCity("ankara", "Ankara", "آنکارا", 39.9334, 32.8597, 938.0, "Europe/Istanbul", "Turkey", "ترکیه", isIran = false, isCapital = true),
        GeoCity("istanbul", "Istanbul", "استانبول", 41.0082, 28.9784, 30.0, "Europe/Istanbul", "Turkey", "ترکیه", isIran = false, alternateNames = listOf("قسطنطنیه")),
        GeoCity("izmir", "Izmir", "ازمیر", 38.4237, 27.1428, 2.0, "Europe/Istanbul", "Turkey", "ترکیه", isIran = false),
        GeoCity("riyadh", "Riyadh", "ریاض", 24.7136, 46.6753, 612.0, "Asia/Riyadh", "Saudi Arabia", "عربستان سعودی", isIran = false, isCapital = true),
        GeoCity("mecca", "Mecca", "مکه مکرمه", 21.3891, 39.8579, 277.0, "Asia/Riyadh", "Saudi Arabia", "عربستان سعودی", isIran = false),
        GeoCity("medina", "Medina", "مدینه منوره", 24.5247, 39.5692, 608.0, "Asia/Riyadh", "Saudi Arabia", "عربستان سعودی", isIran = false),
        GeoCity("jeddah", "Jeddah", "جده", 21.4858, 39.1925, 12.0, "Asia/Riyadh", "Saudi Arabia", "عربستان سعودی", isIran = false),
        GeoCity("abu_dhabi", "Abu Dhabi", "ابوظبی", 24.4539, 54.3773, 10.0, "Asia/Dubai", "United Arab Emirates", "امارات متحده عربی", isIran = false, isCapital = true),
        GeoCity("dubai", "Dubai", "دبی", 25.2048, 55.2708, 5.0, "Asia/Dubai", "United Arab Emirates", "امارات متحده عربی", isIran = false),
        GeoCity("sharjah", "Sharjah", "شارجه", 25.3463, 55.4209, 3.0, "Asia/Dubai", "United Arab Emirates", "امارات متحده عربی", isIran = false),
        GeoCity("doha", "Doha", "دوحه", 25.2854, 51.5310, 10.0, "Asia/Qatar", "Qatar", "قطر", isIran = false, isCapital = true),
        GeoCity("kuwait_city", "Kuwait City", "کویت", 29.3759, 47.9774, 5.0, "Asia/Kuwait", "Kuwait", "کویت", isIran = false, isCapital = true),
        GeoCity("manama", "Manama", "منامه", 26.2285, 50.5860, 5.0, "Asia/Bahrain", "Bahrain", "بحرین", isIran = false, isCapital = true),
        GeoCity("muscat", "Muscat", "مسقط", 23.5880, 58.3829, 14.0, "Asia/Muscat", "Oman", "عمان", isIran = false, isCapital = true),
        GeoCity("sanaa", "Sana'a", "صنعا", 15.3694, 44.1910, 2250.0, "Asia/Aden", "Yemen", "یمن", isIran = false, isCapital = true),
        GeoCity("damascus", "Damascus", "دمشق", 33.5138, 36.2765, 680.0, "Asia/Damascus", "Syria", "سوریه", isIran = false, isCapital = true, alternateNames = listOf("شام")),
        GeoCity("beirut", "Beirut", "بیروت", 33.8938, 35.5018, 10.0, "Asia/Beirut", "Lebanon", "لبنان", isIran = false, isCapital = true),
        GeoCity("amman", "Amman", "امان", 31.9454, 35.9284, 773.0, "Asia/Amman", "Jordan", "اردن", isIran = false, isCapital = true),
        GeoCity("jerusalem", "Jerusalem", "قدس (بیت المقدس)", 31.7683, 35.2137, 754.0, "Asia/Jerusalem", "Palestine", "فلسطین", isIran = false, isCapital = true, alternateNames = listOf("بیت المقدس", "Al-Quds")),
        GeoCity("cairo", "Cairo", "قاهره", 30.0444, 31.2357, 23.0, "Africa/Cairo", "Egypt", "مصر", isIran = false, isCapital = true),
        GeoCity("alexandria", "Alexandria", "اسکندریه", 31.2001, 29.9187, 5.0, "Africa/Cairo", "Egypt", "مصر", isIran = false),
        GeoCity("baku", "Baku", "باکو", 40.4093, 49.8671, -28.0, "Asia/Baku", "Azerbaijan", "جمهوری آذربایجان", isIran = false, isCapital = true),
        GeoCity("yerevan", "Yerevan", "ایروان", 40.1792, 44.4991, 989.0, "Asia/Yerevan", "Armenia", "ارمنستان", isIran = false, isCapital = true),
        GeoCity("tbilisi", "Tbilisi", "تفلیس", 41.7151, 44.8271, 490.0, "Asia/Tbilisi", "Georgia", "گرجستان", isIran = false, isCapital = true),
        GeoCity("kabul", "Kabul", "کابل", 34.5553, 69.2075, 1790.0, "Asia/Kabul", "Afghanistan", "افغانستان", isIran = false, isCapital = true),
        GeoCity("herat", "Herat", "هرات", 34.3529, 62.2040, 920.0, "Asia/Kabul", "Afghanistan", "افغانستان", isIran = false),
        GeoCity("mazar_i_sharif", "Mazar-i-Sharif", "مزار شریف", 36.7114, 67.1109, 360.0, "Asia/Kabul", "Afghanistan", "افغانستان", isIran = false),
        GeoCity("islamabad", "Islamabad", "اسلام‌آباد", 33.6844, 73.0479, 540.0, "Asia/Karachi", "Pakistan", "پاکستان", isIran = false, isCapital = true),
        GeoCity("karachi", "Karachi", "کراچی", 24.8607, 67.0011, 8.0, "Asia/Karachi", "Pakistan", "پاکستان", isIran = false),
        GeoCity("lahore", "Lahore", "لاهور", 31.5204, 74.3587, 217.0, "Asia/Karachi", "Pakistan", "پاکستان", isIran = false),
        GeoCity("tashkent", "Tashkent", "تاشکند", 41.2995, 69.2401, 455.0, "Asia/Tashkent", "Uzbekistan", "ازبکستان", isIran = false, isCapital = true),
        GeoCity("samarkand", "Samarkand", "سمرقند (رصدخانه الغ‌بیگ)", 39.6542, 66.9597, 702.0, "Asia/Samarkand", "Uzbekistan", "ازبکستان", isIran = false, alternateNames = listOf("Ulugh Beg Observatory", "الغ بیگ")),
        GeoCity("bukhara", "Bukhara", "بخارا", 39.7681, 64.4556, 225.0, "Asia/Tashkent", "Uzbekistan", "ازبکستان", isIran = false),
        GeoCity("ashgabat", "Ashgabat", "عشق‌آباد", 37.9601, 58.3261, 219.0, "Asia/Ashgabat", "Turkmenistan", "ترکمنستان", isIran = false, isCapital = true),
        GeoCity("dushanbe", "Dushanbe", "دوشنبه", 38.5598, 68.7870, 706.0, "Asia/Dushanbe", "Tajikistan", "تاجیکستان", isIran = false, isCapital = true),
        GeoCity("bishkek", "Bishkek", "بیشکک", 42.8746, 74.5698, 800.0, "Asia/Bishkek", "Kyrgyzstan", "قرقیزستان", isIran = false, isCapital = true),
        GeoCity("astana", "Astana", "آستانه (نورسلطان)", 51.1694, 71.4491, 347.0, "Asia/Almaty", "Kazakhstan", "قزاقستان", isIran = false, isCapital = true, alternateNames = listOf("Nur-Sultan")),
        GeoCity("almaty", "Almaty", "آلماتی", 43.2220, 76.8512, 785.0, "Asia/Almaty", "Kazakhstan", "قزاقستان", isIran = false),

        // Asia & Pacific
        GeoCity("tokyo", "Tokyo", "توکیو", 35.6762, 139.6503, 40.0, "Asia/Tokyo", "Japan", "ژاپن", isIran = false, isCapital = true),
        GeoCity("kyoto", "Kyoto", "کیوتو", 35.0116, 135.7681, 50.0, "Asia/Tokyo", "Japan", "ژاپن", isIran = false),
        GeoCity("osaka", "Osaka", "اوساکا", 34.6937, 135.5023, 10.0, "Asia/Tokyo", "Japan", "ژاپن", isIran = false),
        GeoCity("beijing", "Beijing", "پکن", 39.9042, 116.4074, 43.0, "Asia/Shanghai", "China", "چین", isIran = false, isCapital = true),
        GeoCity("shanghai", "Shanghai", "شانگهای", 31.2304, 121.4737, 4.0, "Asia/Shanghai", "China", "چین", isIran = false),
        GeoCity("hong_kong", "Hong Kong", "هنگ کنگ", 22.3193, 114.1694, 9.0, "Asia/Hong_Kong", "Hong Kong", "هنگ کنگ", isIran = false),
        GeoCity("seoul", "Seoul", "سئول", 37.5665, 126.9780, 38.0, "Asia/Seoul", "South Korea", "کره جنوبی", isIran = false, isCapital = true),
        GeoCity("taipei", "Taipei", "تایپه", 25.0330, 121.5654, 10.0, "Asia/Taipei", "Taiwan", "تایوان", isIran = false, isCapital = true),
        GeoCity("new_delhi", "New Delhi", "دهلی نو", 28.6139, 77.2090, 216.0, "Asia/Kolkata", "India", "هند", isIran = false, isCapital = true, alternateNames = listOf("Delhi")),
        GeoCity("mumbai", "Mumbai", "بمبئی (مومبای)", 19.0760, 72.8777, 14.0, "Asia/Kolkata", "India", "هند", isIran = false, alternateNames = listOf("Bombay")),
        GeoCity("bangalore", "Bangalore", "بنگلور", 12.9716, 77.5946, 920.0, "Asia/Kolkata", "India", "هند", isIran = false),
        GeoCity("bangkok", "Bangkok", "بانکوک", 13.7563, 100.5018, 1.5, "Asia/Bangkok", "Thailand", "تایلند", isIran = false, isCapital = true),
        GeoCity("singapore", "Singapore", "سنگاپور", 1.3521, 103.8198, 15.0, "Asia/Singapore", "Singapore", "سنگاپور", isIran = false, isCapital = true),
        GeoCity("kuala_lumpur", "Kuala Lumpur", "کوالالامپور", 3.1390, 101.6869, 66.0, "Asia/Kuala_Lumpur", "Malaysia", "مالزی", isIran = false, isCapital = true),
        GeoCity("jakarta", "Jakarta", "جاکارتا", -6.2088, 106.8456, 8.0, "Asia/Jakarta", "Indonesia", "اندونزی", isIran = false, isCapital = true),
        GeoCity("manila", "Manila", "مانیل", 14.5995, 120.9842, 7.0, "Asia/Manila", "Philippines", "فیلیپین", isIran = false, isCapital = true),
        GeoCity("hanoi", "Hanoi", "هانوی", 21.0285, 105.8542, 19.0, "Asia/Bangkok", "Vietnam", "ویتنام", isIran = false, isCapital = true),
        GeoCity("canberra", "Canberra", "کانبرا", -35.2809, 149.1300, 580.0, "Australia/Sydney", "Australia", "استرالیا", isIran = false, isCapital = true),
        GeoCity("sydney", "Sydney", "سیدنی", -33.8688, 151.2093, 19.0, "Australia/Sydney", "Australia", "استرالیا", isIran = false),
        GeoCity("melbourne", "Melbourne", "ملبورن", -37.8136, 144.9631, 31.0, "Australia/Melbourne", "Australia", "استرالیا", isIran = false),
        GeoCity("wellington", "Wellington", "ولینگتون", -41.2865, 174.7762, 20.0, "Pacific/Auckland", "New Zealand", "نیوزیلند", isIran = false, isCapital = true),
        GeoCity("auckland", "Auckland", "اوکلند", -36.8485, 174.7633, 10.0, "Pacific/Auckland", "New Zealand", "نیوزیلند", isIran = false),

        // Europe
        GeoCity("london", "London (Greenwich Observatory)", "لندن (رصدخانه گرینویچ)", 51.5074, -0.1278, 25.0, "Europe/London", "United Kingdom", "بریتانیا", isIran = false, isCapital = true, alternateNames = listOf("Greenwich", "گرینویچ")),
        GeoCity("edinburgh", "Edinburgh", "ادینبرو", 55.9533, -3.1883, 47.0, "Europe/London", "United Kingdom", "اسکاتلند", isIran = false),
        GeoCity("paris", "Paris", "پاریس", 48.8566, 2.3522, 35.0, "Europe/Paris", "France", "فرانسه", isIran = false, isCapital = true),
        GeoCity("berlin", "Berlin", "برلین", 52.5200, 13.4050, 34.0, "Europe/Berlin", "Germany", "آلمان", isIran = false, isCapital = true),
        GeoCity("munich", "Munich", "مونیخ", 48.1351, 11.5820, 520.0, "Europe/Berlin", "Germany", "آلمان", isIran = false),
        GeoCity("frankfurt", "Frankfurt", "فرانکفورت", 50.1109, 8.6821, 112.0, "Europe/Berlin", "Germany", "آلمان", isIran = false),
        GeoCity("rome", "Rome", "رم", 41.9028, 12.4964, 21.0, "Europe/Rome", "Italy", "ایتالیا", isIran = false, isCapital = true),
        GeoCity("milan", "Milan", "میلان", 45.4642, 9.1900, 120.0, "Europe/Rome", "Italy", "ایتالیا", isIran = false),
        GeoCity("madrid", "Madrid", "مادرید", 40.4168, -3.7038, 667.0, "Europe/Madrid", "Spain", "اسپانیا", isIran = false, isCapital = true),
        GeoCity("barcelona", "Barcelona", "بارسلونا", 41.3879, 2.1699, 12.0, "Europe/Madrid", "Spain", "اسپانیا", isIran = false),
        GeoCity("la_palma", "La Palma (Roque de los Muchachos Observatory)", "لاپالما (رصدخانه جزایر قناری)", 28.7139, -17.8928, 2396.0, "Atlantic/Canary", "Spain", "اسپانیا", isIran = false, alternateNames = listOf("Canary Islands", "قناری")),
        GeoCity("amsterdam", "Amsterdam", "آمستردام", 52.3676, 4.9041, -2.0, "Europe/Amsterdam", "Netherlands", "هلند", isIran = false, isCapital = true),
        GeoCity("brussels", "Brussels", "بروکسل", 50.8503, 4.3517, 13.0, "Europe/Brussels", "Belgium", "بلژیک", isIran = false, isCapital = true),
        GeoCity("bern", "Bern", "برن", 46.9480, 7.4474, 542.0, "Europe/Zurich", "Switzerland", "سوئیس", isIran = false, isCapital = true),
        GeoCity("zurich", "Zurich", "زوریخ", 47.3769, 8.5417, 408.0, "Europe/Zurich", "Switzerland", "سوئیس", isIran = false),
        GeoCity("geneva", "Geneva (CERN)", "ژنو (سرن)", 46.2044, 6.1432, 375.0, "Europe/Zurich", "Switzerland", "سوئیس", isIran = false),
        GeoCity("vienna", "Vienna", "وین", 48.2082, 16.3738, 171.0, "Europe/Vienna", "Austria", "اتریش", isIran = false, isCapital = true),
        GeoCity("athens", "Athens", "آتن", 37.9838, 23.7275, 170.0, "Europe/Athens", "Greece", "یونان", isIran = false, isCapital = true),
        GeoCity("stockholm", "Stockholm", "استکهلم", 59.3293, 18.0686, 28.0, "Europe/Stockholm", "Sweden", "سوئد", isIran = false, isCapital = true),
        GeoCity("oslo", "Oslo", "اسلو", 59.9139, 10.7522, 23.0, "Europe/Oslo", "Norway", "نروژ", isIran = false, isCapital = true),
        GeoCity("tromso", "Tromsø (Northern Lights Aurora)", "ترومسو (شفق قطبی)", 69.6492, 18.9553, 9.0, "Europe/Oslo", "Norway", "نروژ", isIran = false, alternateNames = listOf("Tromso", "Aurora")),
        GeoCity("helsinki", "Helsinki", "هلسینکی", 60.1699, 24.9384, 18.0, "Europe/Helsinki", "Finland", "فنلاند", isIran = false, isCapital = true),
        GeoCity("copenhagen", "Copenhagen", "کپنهاگ", 55.6761, 12.5683, 14.0, "Europe/Copenhagen", "Denmark", "دانمارک", isIran = false, isCapital = true),
        GeoCity("reykjavik", "Reykjavik", "ریکیاویک", 64.1466, -21.9426, 15.0, "Atlantic/Reykjavik", "Iceland", "ایسلند", isIran = false, isCapital = true),
        GeoCity("dublin", "Dublin", "دوبلین", 53.3498, -6.2603, 20.0, "Europe/Dublin", "Ireland", "ایرلند", isIran = false, isCapital = true),
        GeoCity("lisbon", "Lisbon", "لیسبون", 38.7223, -9.1393, 100.0, "Europe/Lisbon", "Portugal", "پرتغال", isIran = false, isCapital = true),
        GeoCity("warsaw", "Warsaw", "ورشو", 52.2297, 21.0122, 100.0, "Europe/Warsaw", "Poland", "لهستان", isIran = false, isCapital = true),
        GeoCity("prague", "Prague", "پراگ", 50.0755, 14.4378, 235.0, "Europe/Prague", "Czech Republic", "جمهوری چک", isIran = false, isCapital = true),
        GeoCity("budapest", "Budapest", "بوداپست", 47.4979, 19.0402, 102.0, "Europe/Budapest", "Hungary", "مجارستان", isIran = false, isCapital = true),
        GeoCity("bucharest", "Bucharest", "بخارست", 44.4268, 26.1025, 75.0, "Europe/Bucharest", "Romania", "رومانی", isIran = false, isCapital = true),
        GeoCity("moscow", "Moscow", "مسکو", 55.7558, 37.6173, 156.0, "Europe/Moscow", "Russia", "روسیه", isIran = false, isCapital = true),
        GeoCity("saint_petersburg", "Saint Petersburg", "سن پترزبورگ", 59.9343, 30.3351, 3.0, "Europe/Moscow", "Russia", "روسیه", isIran = false, alternateNames = listOf("لنینگراد")),
        GeoCity("kyiv", "Kyiv", "کی‌یف", 50.4501, 30.5234, 179.0, "Europe/Kyiv", "Ukraine", "اوکراین", isIran = false, isCapital = true),

        // Americas
        GeoCity("washington_dc", "Washington, D.C.", "واشنگتن دی‌سی", 38.9072, -77.0369, 2.0, "America/New_York", "United States", "ایالات متحده آمریکا", isIran = false, isCapital = true),
        GeoCity("new_york", "New York", "نیویورک", 40.7128, -74.0060, 10.0, "America/New_York", "United States", "ایالات متحده آمریکا", isIran = false, alternateNames = listOf("NYC")),
        GeoCity("los_angeles", "Los Angeles (Griffith Observatory)", "لس آنجلس (رصدخانه گریفیث)", 34.0522, -118.2437, 89.0, "America/Los_Angeles", "United States", "ایالات متحده آمریکا", isIran = false, alternateNames = listOf("Griffith Observatory", "LA")),
        GeoCity("san_francisco", "San Francisco", "سان فرانسیسکو", 37.7749, -122.4194, 16.0, "America/Los_Angeles", "United States", "ایالات متحده آمریکا", isIran = false),
        GeoCity("chicago", "Chicago", "شیکاگو", 41.8781, -87.6298, 181.0, "America/Chicago", "United States", "ایالات متحده آمریکا", isIran = false),
        GeoCity("houston", "Houston (NASA Johnson Space Center)", "هیوستون (مرکز فضایی ناسا)", 29.7604, -95.3698, 15.0, "America/Chicago", "United States", "ایالات متحده آمریکا", isIran = false, alternateNames = listOf("NASA", "ناسا")),
        GeoCity("mauna_kea", "Mauna Kea Observatory (Hawaii)", "رصدخانه مائونا کیا (هاوایی)", 19.8206, -155.4681, 4205.0, "Pacific/Honolulu", "United States", "ایالات متحده آمریکا", isIran = false, alternateNames = listOf("Hawaii", "Keck")),
        GeoCity("ottawa", "Ottawa", "اتاوا", 45.4215, -75.6972, 70.0, "America/Toronto", "Canada", "کانادا", isIran = false, isCapital = true),
        GeoCity("toronto", "Toronto", "تورنتو", 43.6532, -79.3832, 76.0, "America/Toronto", "Canada", "کانادا", isIran = false),
        GeoCity("vancouver", "Vancouver", "ونکوور", 49.2827, -123.1207, 4.0, "America/Vancouver", "Canada", "کانادا", isIran = false),
        GeoCity("montreal", "Montreal", "مونترال", 45.5017, -73.5673, 23.0, "America/Toronto", "Canada", "کانادا", isIran = false),
        GeoCity("mexico_city", "Mexico City", "مکزیکوسیتی", 19.4326, -99.1332, 2240.0, "America/Mexico_City", "Mexico", "مکزیک", isIran = false, isCapital = true),
        GeoCity("brasilia", "Brasília", "برازیلیا", -15.7975, -47.8919, 1172.0, "America/Sao_Paulo", "Brazil", "برزیل", isIran = false, isCapital = true),
        GeoCity("sao_paulo", "São Paulo", "سائو پائولو", -23.5505, -46.6333, 760.0, "America/Sao_Paulo", "Brazil", "برزیل", isIran = false),
        GeoCity("rio_de_janeiro", "Rio de Janeiro", "ریودوژانیرو", -22.9068, -43.1729, 2.0, "America/Sao_Paulo", "Brazil", "برزیل", isIran = false),
        GeoCity("buenos_aires", "Buenos Aires", "بوئنوس آیرس", -34.6037, -58.3816, 25.0, "America/Argentina/Buenos_Aires", "Argentina", "آرژانتین", isIran = false, isCapital = true),
        GeoCity("santiago", "Santiago", "سانتیاگو", -33.4489, -70.6693, 570.0, "America/Santiago", "Chile", "شیلی", isIran = false, isCapital = true),
        GeoCity("atacama", "Atacama ALMA Observatory", "رصدخانه آلما کویر آتاکاما (شیلی)", -23.0228, -67.7548, 5050.0, "America/Santiago", "Chile", "شیلی", isIran = false, alternateNames = listOf("ALMA", "Paranal", "VLT", "آتاکاما")),
        GeoCity("lima", "Lima", "لیما", -12.0464, -77.0428, 154.0, "America/Lima", "Peru", "پرو", isIran = false, isCapital = true),
        GeoCity("bogota", "Bogotá", "بوگوتا", 4.7110, -74.0721, 2640.0, "America/Bogota", "Colombia", "کلمبیا", isIran = false, isCapital = true),

        // Africa
        GeoCity("pretoria", "Pretoria", "پرتوریا", -25.7479, 28.2293, 1339.0, "Africa/Johannesburg", "South Africa", "آفریقای جنوبی", isIran = false, isCapital = true),
        GeoCity("cape_town", "Cape Town", "کیپ‌تاون", -33.9249, 18.4241, 10.0, "Africa/Johannesburg", "South Africa", "آفریقای جنوبی", isIran = false),
        GeoCity("nairobi", "Nairobi", "نایروبی", -1.2921, 36.8219, 1795.0, "Africa/Nairobi", "Kenya", "کنیا", isIran = false, isCapital = true),
        GeoCity("addis_ababa", "Addis Ababa", "آدیس آبابا", 9.0300, 38.7400, 2355.0, "Africa/Addis_Ababa", "Ethiopia", "اتیوپی", isIran = false, isCapital = true),
        GeoCity("rabat", "Rabat", "رباط", 34.0209, -6.8416, 75.0, "Africa/Casablanca", "Morocco", "مراکش", isIran = false, isCapital = true),
        GeoCity("casablanca", "Casablanca", "کازابلانکا", 33.5731, -7.5898, 17.0, "Africa/Casablanca", "Morocco", "مراکش", isIran = false),
        GeoCity("algiers", "Algiers", "الجزیره", 36.7538, 3.0588, 10.0, "Africa/Algiers", "Algeria", "الجزایر", isIran = false, isCapital = true),
        GeoCity("tunis", "Tunis", "تونس", 36.8065, 10.1815, 4.0, "Africa/Tunis", "Tunisia", "تونس", isIran = false, isCapital = true)
    )

    val ALL_CITIES: List<GeoCity> by lazy {
        val list = mutableListOf<GeoCity>()
        list.addAll(IRAN_PROVINCIAL_CAPITALS)
        list.addAll(IRAN_NOTABLE_CITIES)
        list.addAll(WORLD_CAPITALS_AND_MAJOR_CITIES)
        list.distinctBy { it.id }
    }

    /**
     * Normalizes text for robust Persian/Arabic & English searching.
     */
    fun normalize(text: String): String {
        return text.trim().lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('آ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('ؤ', 'و')
            .replace('ئ', 'ی')
            .replace("\u064B", "") // Tanween Fath
            .replace("\u064C", "") // Tanween Damm
            .replace("\u064D", "") // Tanween Kasr
            .replace("\u064E", "") // Fatha
            .replace("\u064F", "") // Damma
            .replace("\u0650", "") // Kasra
            .replace("\u0651", "") // Shadda
            .replace("\u0652", "") // Sukun
            .replace("-", " ")
            .replace("_", " ")
            .replace(",", " ")
            .replace("  ", " ")
    }

    /**
     * Instant zero-latency offline search in local geographic dataset.
     */
    fun search(query: String, limit: Int = 40): List<GeoCity> {
        val q = normalize(query)
        if (q.isEmpty()) {
            return ALL_CITIES.take(limit)
        }

        // Score each city based on relevance
        val scored = ALL_CITIES.mapNotNull { city ->
            val normEn = normalize(city.nameEn)
            val normFa = normalize(city.nameFa)
            val normProvEn = normalize(city.provinceEn)
            val normProvFa = normalize(city.provinceFa)
            val normCountryEn = normalize(city.countryEn)
            val normCountryFa = normalize(city.countryFa)
            val normAlts = city.alternateNames.map { normalize(it) }

            var score = 0

            if (normEn == q || normFa == q) {
                score += 1000
            } else if (normEn.startsWith(q) || normFa.startsWith(q)) {
                score += 800
            } else if (normEn.contains(q) || normFa.contains(q)) {
                score += 600
            } else if (normAlts.any { it == q }) {
                score += 700
            } else if (normAlts.any { it.startsWith(q) || it.contains(q) }) {
                score += 500
            } else if (normProvEn.contains(q) || normProvFa.contains(q)) {
                score += 400
            } else if (normCountryEn.contains(q) || normCountryFa.contains(q)) {
                score += 300
            }

            if (score > 0) {
                if (city.isIran) score += 50
                if (city.isCapital) score += 20
                city to score
            } else null
        }

        return scored.sortedByDescending { it.second }.map { it.first }.take(limit)
    }

    /**
     * Calculates distance between two coordinates in kilometers using Haversine formula.
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Offline nearest-neighbor city lookup for GPS coordinates.
     */
    fun findNearestCity(lat: Double, lon: Double): GeoCity {
        return ALL_CITIES.minByOrNull { distanceKm(lat, lon, it.latitude, it.longitude) } ?: NURABAD_CITY
    }
}
