package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.domain.ConstellationData

object ConstellationCatalog {

    fun getConstellations(): List<ConstellationData> {
        return listOf(
            // 1. Orion
            ConstellationData(
                code = "ORI",
                nameEn = "Orion",
                nameFa = "شکارچی (جبار)",
                latinName = "Orion",
                mainStars = listOf(
                    Pair(88.793, 7.407),    // Betelgeuse
                    Pair(81.283, 6.350),    // Bellatrix
                    Pair(84.050, -0.300),   // Mintaka
                    Pair(84.530, -1.200),   // Alnilam
                    Pair(85.190, -1.940),   // Alnitak
                    Pair(86.939, -9.670),   // Saiph
                    Pair(78.634, -8.202)    // Rigel
                ),
                starIdsLines = listOf(
                    Pair("star_ori_betelgeuse", "star_ori_bellatrix"),
                    Pair("star_ori_bellatrix", "star_ori_mintaka"),
                    Pair("star_ori_mintaka", "star_ori_alnilam"),
                    Pair("star_ori_alnilam", "star_ori_alnitak"),
                    Pair("star_ori_alnitak", "star_ori_saiph"),
                    Pair("star_ori_saiph", "star_ori_rigel"),
                    Pair("star_ori_rigel", "star_ori_alnitak"),
                    Pair("star_ori_betelgeuse", "star_ori_alnitak")
                ),
                areaSqDeg = 594.1,
                seasonEn = "Winter",
                seasonFa = "زمستان",
                hemisphereEn = "Equatorial (Both)",
                hemisphereFa = "استوایی (هر دو نیمکره)",
                bestViewingMonthEn = "January",
                bestViewingMonthFa = "دی‌ماه",
                historicalInfoEn = "The Hunter of Greek and Babylonian mythology, featuring Betelgeuse and Rigel.",
                historicalInfoFa = "اسطوره شکارچی بزرگ در اساطیر یونان و بابلی، دارنده نوار کمربند معروف جبار."
            ),

            // 2. Ursa Major
            ConstellationData(
                code = "UMA",
                nameEn = "Ursa Major",
                nameFa = "خرس بزرگ (دب اکبر)",
                latinName = "Ursa Major",
                mainStars = listOf(
                    Pair(165.932, 61.751),  // Dubhe
                    Pair(165.460, 56.380),  // Merak
                    Pair(178.450, 53.690),  // Phecda
                    Pair(183.850, 57.030),  // Megrez
                    Pair(193.507, 55.960),  // Alioth
                    Pair(206.884, 54.920),  // Mizar
                    Pair(209.800, 49.310)   // Alkaid
                ),
                starIdsLines = listOf(
                    Pair("star_uma_dubhe", "star_uma_merak"),
                    Pair("star_uma_merak", "star_uma_phecda"),
                    Pair("star_uma_phecda", "star_uma_megrez"),
                    Pair("star_uma_megrez", "star_uma_dubhe"),
                    Pair("star_uma_megrez", "star_uma_alioth"),
                    Pair("star_uma_alioth", "star_uma_mizar"),
                    Pair("star_uma_mizar", "star_uma_alkaid")
                ),
                areaSqDeg = 1279.6,
                seasonEn = "Spring",
                seasonFa = "بهار",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "April",
                bestViewingMonthFa = "فروردین‌ماه",
                historicalInfoEn = "Contains the Big Dipper, one of the oldest star patterns recognized by humans.",
                historicalInfoFa = "دربردارنده صورتواره ملاقه بزرگ (هفت‌اورنگ) که راهنمای باستانی قطب شمال بوده است."
            ),

            // 3. Ursa Minor
            ConstellationData(
                code = "UMI",
                nameEn = "Ursa Minor",
                nameFa = "خرس کوچک (دب اصغر)",
                latinName = "Ursa Minor",
                mainStars = listOf(
                    Pair(37.954, 89.264),   // Polaris
                    Pair(222.670, 74.150),  // Yildun
                    Pair(236.040, 71.830),  // Udel
                    Pair(229.870, 71.800),  // Pherkad
                    Pair(222.720, 71.830),  // Kochab
                    Pair(204.000, 77.790)   // Ahfa
                ),
                starIdsLines = listOf(
                    Pair("star_umi_polaris", "star_umi_yildun"),
                    Pair("star_umi_yildun", "star_umi_kochab")
                ),
                areaSqDeg = 255.9,
                seasonEn = "Circumpolar (All Year)",
                seasonFa = "همیشه‌پیدا (تمام سال)",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "June",
                bestViewingMonthFa = "خردادماه",
                historicalInfoEn = "Contains Polaris, the North Star defining Earth's North Celestial Pole.",
                historicalInfoFa = "دربردارنده ستاره قطبی که نشان‌دهنده قطب شمال نجومی زمین است."
            ),

            // 4. Cassiopeia
            ConstellationData(
                code = "CAS",
                nameEn = "Cassiopeia",
                nameFa = "ذات‌الکرسی (خداوند اورنگ)",
                latinName = "Cassiopeia",
                mainStars = listOf(
                    Pair(1.150, 59.150),    // Caph
                    Pair(9.880, 59.150),    // Schedar
                    Pair(14.170, 60.720),   // Gamma Cas
                    Pair(20.200, 60.230),   // Ruchbah
                    Pair(28.590, 63.670)    // Segin
                ),
                starIdsLines = listOf(
                    Pair("star_cas_caph", "star_cas_schedar"),
                    Pair("star_cas_schedar", "star_cas_gamma"),
                    Pair("star_cas_gamma", "star_cas_ruchbah"),
                    Pair("star_cas_ruchbah", "star_cas_segin")
                ),
                areaSqDeg = 598.4,
                seasonEn = "Autumn",
                seasonFa = "پاییز",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "November",
                bestViewingMonthFa = "آبان‌ماه",
                historicalInfoEn = "Queen Cassiopeia of Greek myth, recognizable by its distinct W shape.",
                historicalInfoFa = "ملکه کاثیوپیه در اساطیر یونان که با شکل مشخص W خود در آسمان شناخته می‌شود."
            ),

            // 5. Cygnus
            ConstellationData(
                code = "CYG",
                nameEn = "Cygnus",
                nameFa = "ماکیان (قو)",
                latinName = "Cygnus",
                mainStars = listOf(
                    Pair(310.358, 45.280),  // Deneb
                    Pair(305.557, 40.260),  // Sadr
                    Pair(292.680, 27.960),  // Albireo
                    Pair(312.000, 30.220),  // Gienah
                    Pair(296.000, 45.130)   // Delta Cyg
                ),
                starIdsLines = listOf(
                    Pair("star_cyg_deneb", "star_cyg_sadr"),
                    Pair("star_cyg_sadr", "star_cyg_albireo"),
                    Pair("star_cyg_gienah", "star_cyg_sadr"),
                    Pair("star_cyg_sadr", "star_cyg_delta")
                ),
                areaSqDeg = 804.0,
                seasonEn = "Summer",
                seasonFa = "تابستان",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "September",
                bestViewingMonthFa = "شهریورماه",
                historicalInfoEn = "The Swan flying south along the Milky Way rift, holding the Northern Cross.",
                historicalInfoFa = "قوی سپید پروازکننده در امتداد جاده کاهکشان شامل صورتواره صلیب شمالی."
            ),

            // 6. Scorpius
            ConstellationData(
                code = "SCO",
                nameEn = "Scorpius",
                nameFa = "عقرب (کژدم)",
                latinName = "Scorpius",
                mainStars = listOf(
                    Pair(247.352, -26.432), // Antares
                    Pair(262.690, -37.030), // Shaula
                    Pair(263.400, -37.290), // Lesath
                    Pair(263.900, -42.990)  // Sargas
                ),
                starIdsLines = listOf(
                    Pair("star_sco_antares", "star_sco_sargas"),
                    Pair("star_sco_sargas", "star_sco_shaula")
                ),
                areaSqDeg = 496.8,
                seasonEn = "Summer",
                seasonFa = "تابستان",
                hemisphereEn = "Southern / Equatorial",
                hemisphereFa = "جنوبی / استوایی",
                bestViewingMonthEn = "July",
                bestViewingMonthFa = "تیرماه",
                historicalInfoEn = "Ancient scorpion zodiac constellation featuring red supergiant Antares.",
                historicalInfoFa = "صورت فلکی کژدم با ستاره سرخ درخشان قلب‌العقرب در مرکز آن."
            ),

            // 7. Canis Major
            ConstellationData(
                code = "CMA",
                nameEn = "Canis Major",
                nameFa = "سگ بزرگ (کلب اکبر)",
                latinName = "Canis Major",
                mainStars = listOf(
                    Pair(101.287, -16.716), // Sirius
                    Pair(95.675, -17.956),  // Mirzam
                    Pair(104.656, -28.972), // Adhara
                    Pair(107.098, -26.393)  // Wezen
                ),
                starIdsLines = listOf(
                    Pair("star_cma_sirius", "star_cma_mirzam"),
                    Pair("star_cma_sirius", "star_cma_adhara"),
                    Pair("star_cma_adhara", "star_cma_wezen")
                ),
                areaSqDeg = 380.1,
                seasonEn = "Winter",
                seasonFa = "زمستان",
                hemisphereEn = "Southern / Equatorial",
                hemisphereFa = "جنوبی / استوایی",
                bestViewingMonthEn = "February",
                bestViewingMonthFa = "بهمن‌ماه",
                historicalInfoEn = "Orion's larger hunting dog holding Sirius, the brightest star in the sky.",
                historicalInfoFa = "سگ بزرگ شکارچی جبار شامل درخشان‌ترین ستاره آسمان یعنی شباهنگ."
            ),

            // 8. Taurus
            ConstellationData(
                code = "TAU",
                nameEn = "Taurus",
                nameFa = "گاو (ثور)",
                latinName = "Taurus",
                mainStars = listOf(
                    Pair(68.980, 16.509),   // Aldebaran
                    Pair(81.573, 28.608)    // Elnath
                ),
                starIdsLines = listOf(
                    Pair("star_tau_aldebaran", "star_tau_elnath")
                ),
                areaSqDeg = 797.2,
                seasonEn = "Winter",
                seasonFa = "زمستان",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "January",
                bestViewingMonthFa = "دی‌ماه",
                historicalInfoEn = "Ancient Bull of Heaven containing Pleiades (M45) and Hyades clusters.",
                historicalInfoFa = "گاو مقدس اساطیری دارنده خوشه زیباترین ثریا (پروین) و قلائص."
            ),

            // 9. Gemini
            ConstellationData(
                code = "GEM",
                nameEn = "Gemini",
                nameFa = "دوپیکر (جوزا)",
                latinName = "Gemini",
                mainStars = listOf(
                    Pair(113.650, 31.888), // Castor
                    Pair(116.329, 28.026)  // Pollux
                ),
                starIdsLines = listOf(
                    Pair("star_gem_castor", "star_gem_pollux")
                ),
                areaSqDeg = 513.8,
                seasonEn = "Winter",
                seasonFa = "زمستان",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "February",
                bestViewingMonthFa = "بهمن‌ماه",
                historicalInfoEn = "The Twins Castor and Pollux of Greek mythology.",
                historicalInfoFa = "دو برادر دوقلوی کاستور و پولوکس در نجوم باستان."
            ),

            // 10. Leo
            ConstellationData(
                code = "LEO",
                nameEn = "Leo",
                nameFa = "شیر (اسد)",
                latinName = "Leo",
                mainStars = listOf(
                    Pair(152.093, 11.967), // Regulus
                    Pair(177.260, 14.570)  // Denebola
                ),
                starIdsLines = listOf(
                    Pair("star_leo_regulus", "star_leo_denebola")
                ),
                areaSqDeg = 947.0,
                seasonEn = "Spring",
                seasonFa = "بهار",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "April",
                bestViewingMonthFa = "فروردین‌ماه",
                historicalInfoEn = "Royal Lion constellation featuring Regulus on the ecliptic.",
                historicalInfoFa = "شیر شاهوار آسمانی شامل ستاره پادشاهی قلب‌الاسد."
            ),

            // 11. Boötes
            ConstellationData(
                code = "BOO",
                nameEn = "Boötes",
                nameFa = "عوّاد / نگهبان شمال (عوا)",
                latinName = "Boötes",
                mainStars = listOf(
                    Pair(213.915, 19.182)  // Arcturus
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 906.8,
                seasonEn = "Spring",
                seasonFa = "بهار",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "May",
                bestViewingMonthFa = "اردیبهشت‌ماه",
                historicalInfoEn = "The Herdsman holding red giant Arcturus.",
                historicalInfoFa = "صورت فلکی عواد نگهبان شمال با ستاره درخشان سماک رامح."
            ),

            // 12. Virgo
            ConstellationData(
                code = "VIR",
                nameEn = "Virgo",
                nameFa = "دوشیزه (سنبله)",
                latinName = "Virgo",
                mainStars = listOf(
                    Pair(201.298, -11.161) // Spica
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 1294.4,
                seasonEn = "Spring",
                seasonFa = "بهار",
                hemisphereEn = "Equatorial",
                hemisphereFa = "استوایی",
                bestViewingMonthEn = "May",
                bestViewingMonthFa = "اردیبهشت‌ماه",
                historicalInfoEn = "Second-largest constellation in the sky containing the Virgo Galaxy Cluster.",
                historicalInfoFa = "دومین صورت فلکی بزرگ آسمان شامل ابرخوشه کهکشانی دوشیزه."
            ),

            // 13. Lyra
            ConstellationData(
                code = "LYR",
                nameEn = "Lyra",
                nameFa = "دیگ‌پایه (شلیاق)",
                latinName = "Lyra",
                mainStars = listOf(
                    Pair(279.234, 38.783)  // Vega
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 286.5,
                seasonEn = "Summer",
                seasonFa = "تابستان",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "August",
                bestViewingMonthFa = "مردادماه",
                historicalInfoEn = "Orpheus' Lyre holding blue-white anchor star Vega.",
                historicalInfoFa = "چنگ نغمه‌سرای اساطیری دارنده ستاره نسر واقع."
            ),

            // 14. Aquila
            ConstellationData(
                code = "AQL",
                nameEn = "Aquila",
                nameFa = "عقاب",
                latinName = "Aquila",
                mainStars = listOf(
                    Pair(297.696, 8.868)   // Altair
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 652.5,
                seasonEn = "Summer",
                seasonFa = "تابستان",
                hemisphereEn = "Equatorial",
                hemisphereFa = "استوایی",
                bestViewingMonthEn = "August",
                bestViewingMonthFa = "مردادماه",
                historicalInfoEn = "The Eagle of Zeus holding fast-rotating Altair.",
                historicalInfoFa = "عقاب تیزپرواز آسمانی با ستاره درخشان نسر طائر."
            ),

            // 15. Pegasus
            ConstellationData(
                code = "PEG",
                nameEn = "Pegasus",
                nameFa = "اسب بالدار (فرس اعظم)",
                latinName = "Pegasus",
                mainStars = listOf(
                    Pair(346.190, 15.210), // Markab
                    Pair(345.940, 28.080)  // Scheat
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 1121.0,
                seasonEn = "Autumn",
                seasonFa = "پاییز",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "October",
                bestViewingMonthFa = "مهرماه",
                historicalInfoEn = "Winged Horse carrying the Great Square asterism.",
                historicalInfoFa = "اسب بالدار اساطیری دارنده مربع عظیم چهارستاره‌ای."
            ),

            // 16. Andromeda
            ConstellationData(
                code = "AND",
                nameEn = "Andromeda",
                nameFa = "زن به‌زنجیربسته (آندرومدا)",
                latinName = "Andromeda",
                mainStars = listOf(
                    Pair(2.096, 29.090)   // Alpheratz
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 722.3,
                seasonEn = "Autumn",
                seasonFa = "پاییز",
                hemisphereEn = "Northern",
                hemisphereFa = "شمالی",
                bestViewingMonthEn = "November",
                bestViewingMonthFa = "آبان‌ماه",
                historicalInfoEn = "Princess Andromeda holding M31, the nearest major spiral galaxy.",
                historicalInfoFa = "شاهزاده خانم به‌زنجیربسته دربردارنده کهکشان M31."
            ),

            // 17. Sagittarius
            ConstellationData(
                code = "SGR",
                nameEn = "Sagittarius",
                nameFa = "کمان (قوس)",
                latinName = "Sagittarius",
                mainStars = listOf(
                    Pair(275.600, -29.880) // Kaus Australis
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 867.4,
                seasonEn = "Summer",
                seasonFa = "تابستان",
                hemisphereEn = "Southern",
                hemisphereFa = "جنوبی",
                bestViewingMonthEn = "August",
                bestViewingMonthFa = "مردادماه",
                historicalInfoEn = "Archer Centaur pointing into the dense core of the Milky Way galaxy.",
                historicalInfoFa = "کماندار اساطیری نشانه رفته به سمت مرکز کهکشان راه شیری."
            ),

            // 18. Centaurus
            ConstellationData(
                code = "CEN",
                nameEn = "Centaurus",
                nameFa = "قنطورس",
                latinName = "Centaurus",
                mainStars = listOf(
                    Pair(219.900, -60.833), // Alpha Centauri
                    Pair(210.956, -60.373)  // Beta Centauri
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 1060.4,
                seasonEn = "Spring",
                seasonFa = "بهار",
                hemisphereEn = "Far Southern",
                hemisphereFa = "جنوبی دور",
                bestViewingMonthEn = "May",
                bestViewingMonthFa = "اردیبهشت‌ماه",
                historicalInfoEn = "The Wise Centaur Chiron containing Alpha Centauri and Omega Centauri cluster.",
                historicalInfoFa = "قنطورس دانا دارنده نزدیک‌ترین ستاره و بزرگ‌ترین خوشه کروی."
            ),

            // 19. Carina
            ConstellationData(
                code = "CAR",
                nameEn = "Carina",
                nameFa = "شاه‌تخته (زورق)",
                latinName = "Carina",
                mainStars = listOf(
                    Pair(95.987, -52.695) // Canopus
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 494.2,
                seasonEn = "Winter",
                seasonFa = "زمستان",
                hemisphereEn = "Far Southern",
                hemisphereFa = "جنوبی دور",
                bestViewingMonthEn = "March",
                bestViewingMonthFa = "اسفندماه",
                historicalInfoEn = "Keel of Jason's ship Argo Navis containing brilliant Canopus.",
                historicalInfoFa = "تنه و شاه‌تخته کشتی آرگو شامل ستاره سهیل."
            ),

            // 20. Crux
            ConstellationData(
                code = "CRU",
                nameEn = "Crux (Southern Cross)",
                nameFa = "صلیب جنوبی (چلیپا)",
                latinName = "Crux",
                mainStars = listOf(
                    Pair(186.650, -63.100), // Acrux
                    Pair(191.930, -59.690)  // Mimosa
                ),
                starIdsLines = emptyList(),
                areaSqDeg = 68.4,
                seasonEn = "Spring",
                seasonFa = "بهار",
                hemisphereEn = "Far Southern",
                hemisphereFa = "جنوبی دور",
                bestViewingMonthEn = "May",
                bestViewingMonthFa = "اردیبهشت‌ماه",
                historicalInfoEn = "Smallest of all 88 constellations, legendary navigation anchor for Southern Hemisphere.",
                historicalInfoFa = "کوچک‌ترین صورت فلکی آسمان اما مشهورترین راهنمای ناوبری نیمکره جنوبی."
            )
        )
    }
}
