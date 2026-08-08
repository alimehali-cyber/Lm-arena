package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.astro_engine.ISSEngine.TLEData

enum class SatelliteCategory(val labelEn: String, val labelFa: String) {
    ALL("All", "همه"),
    ISS("ISS", "ایستگاه فضایی"),
    STARLINK("Starlink", "استارلینک"),
    HUBBLE("Hubble", "هابل"),
    JWST("JWST", "جیمز وب"),
    VISIBLE("Visible", "قابل مشاهده")
}

data class SatelliteItem(
    val id: String,
    val noradId: Int,
    val nameEn: String,
    val nameFa: String,
    val category: SatelliteCategory,
    val designation: String,
    val defaultTle: TLEData,
    val isConstellation: Boolean = false,
    val starlinkTrainCount: Int = 1,
    val standardMagnitude: Double = 2.0,
    val descriptionEn: String,
    val descriptionFa: String,
    val isNakedEyeCandidate: Boolean = true
)

object SatelliteCatalog {

    val satellites: List<SatelliteItem> = listOf(
        SatelliteItem(
            id = "iss_zarya",
            noradId = 25544,
            nameEn = "ISS (International Space Station)",
            nameFa = "ایستگاه فضایی بین‌المللی (ISS)",
            category = SatelliteCategory.ISS,
            designation = "1998-067A",
            defaultTle = TLEData(
                name = "ISS (ZARYA)",
                line1 = "1 25544U 98067A   26213.50000000  .00016717  00000-0  30000-3 0  9993",
                line2 = "2 25544  51.6400 200.0000 0005000  90.0000 270.0000 15.49000000400001"
            ),
            isConstellation = false,
            starlinkTrainCount = 1,
            standardMagnitude = -3.8,
            descriptionEn = "The largest artificial body in orbit, observable with the naked eye as a bright gliding star.",
            descriptionFa = "بزرگ‌ترین سازه ساخت بشر در فضا که با چشم غیرمسلح مانند ستاره‌ای بسیار درخشان و روان دیده می‌شود.",
            isNakedEyeCandidate = true
        ),
        SatelliteItem(
            id = "starlink_train_g7",
            noradId = 58200,
            nameEn = "Starlink Train (Group 7)",
            nameFa = "قطار ماهواره‌ای استارلینک (گروه ۷)",
            category = SatelliteCategory.STARLINK,
            designation = "2023-165A-O",
            defaultTle = TLEData(
                name = "STARLINK TRAIN (G7-1)",
                line1 = "1 58200U 23165A   26213.50000000  .00021000  00000-0  15000-3 0  9991",
                line2 = "2 58200  53.0500 185.0000 0001200  85.0000 275.0000 15.06000000120002"
            ),
            isConstellation = true,
            starlinkTrainCount = 15,
            standardMagnitude = 1.2,
            descriptionEn = "A newly launched train of 15 Starlink satellites flying in tight formation shortly after deployment.",
            descriptionFa = "صفی از ۱۵ ماهواره استارلینک پرتاب‌شده که در آرایه‌ای خطی و متراکم در آسمان حرکت می‌کنند.",
            isNakedEyeCandidate = true
        ),
        SatelliteItem(
            id = "starlink_1007",
            noradId = 44713,
            nameEn = "Starlink-1007",
            nameFa = "ماهواره استارلینک-۱۰۰۷",
            category = SatelliteCategory.STARLINK,
            designation = "2019-074A",
            defaultTle = TLEData(
                name = "STARLINK-1007",
                line1 = "1 44713U 19074A   26213.50000000  .00001200  00000-0  10000-4 0  9992",
                line2 = "2 44713  53.0000 120.0000 0001500  45.0000 315.0000 15.06000000250001"
            ),
            isConstellation = false,
            starlinkTrainCount = 1,
            standardMagnitude = 4.2,
            descriptionEn = "Operational broadband internet satellite orbiting in Low Earth Orbit at 550 km altitude.",
            descriptionFa = "ماهواره عملیاتی اینترنت پهن‌باند استارلینک در مدار نزدیک زمین (LEO) در ارتفاع ۵۵۰ کیلومتری.",
            isNakedEyeCandidate = false
        ),
        SatelliteItem(
            id = "hubble_space_telescope",
            noradId = 20580,
            nameEn = "Hubble Space Telescope (HST)",
            nameFa = "تلسکوپ فضایی هابل (HST)",
            category = SatelliteCategory.HUBBLE,
            designation = "1990-037B",
            defaultTle = TLEData(
                name = "HST",
                line1 = "1 20580U 90037B   26213.50000000  .00000850  00000-0  45000-4 0  9998",
                line2 = "2 20580  28.4700 310.0000 0002800 210.0000 150.0000 15.09000000890001"
            ),
            isConstellation = false,
            starlinkTrainCount = 1,
            standardMagnitude = 2.0,
            descriptionEn = "Legendary optical space telescope orbiting Earth at ~525 km altitude with 28.5° inclination.",
            descriptionFa = "تلسکوپ فضایی افسانه‌ای هابل در ارتفاع ۵۲۵ کیلومتری زمین با زاویه میل مداری ۲۸.۵ درجه.",
            isNakedEyeCandidate = true
        ),
        SatelliteItem(
            id = "james_webb_space_telescope",
            noradId = 50463,
            nameEn = "James Webb Space Telescope (JWST)",
            nameFa = "تلسکوپ فضایی جیمز وب (JWST)",
            category = SatelliteCategory.JWST,
            designation = "2021-130A",
            defaultTle = TLEData(
                name = "JWST (L2 HALO)",
                line1 = "1 50463U 21130A   26213.50000000  .00000001  00000-0  00000-0 0  9990",
                line2 = "2 50463   0.1500  45.0000 0500000  10.0000 350.0000  0.00270000001001"
            ),
            isConstellation = false,
            starlinkTrainCount = 1,
            standardMagnitude = 14.5,
            descriptionEn = "NASA's flagship infrared observatory operating in a Halo orbit around Sun-Earth Lagrange Point 2 (L2), ~1.5 million km away.",
            descriptionFa = "رصدخانه مادون‌قرمز پیشرو ناسا مستقر در مدار هالو حول نقطه لاگرانژی ۲ (L2) زمین-خورشید در فاصله ۱.۵ میلیون کیلومتری.",
            isNakedEyeCandidate = false
        ),
        SatelliteItem(
            id = "tiangong_space_station",
            noradId = 48274,
            nameEn = "Tiangong Space Station (CSS)",
            nameFa = "ایستگاه فضایی تیانگونگ (چین)",
            category = SatelliteCategory.VISIBLE,
            designation = "2021-035A",
            defaultTle = TLEData(
                name = "CSS (TIANGONG)",
                line1 = "1 48274U 21035A   26213.50000000  .00012000  00000-0  20000-3 0  9994",
                line2 = "2 48274  41.4700 150.0000 0003500 120.0000 240.0000 15.61000000280001"
            ),
            isConstellation = false,
            starlinkTrainCount = 1,
            standardMagnitude = -1.5,
            descriptionEn = "China's permanently crewed space station orbiting at ~390 km, bright and easily visible to the naked eye.",
            descriptionFa = "ایستگاه فضایی سرنشین‌دار چین در ارتفاع ۳۹۰ کیلومتری، بسیار درخشان و به راحتی با چشم غیرمسلح قابل رؤیت.",
            isNakedEyeCandidate = true
        ),
        SatelliteItem(
            id = "cosmos_1457",
            noradId = 28654,
            nameEn = "Envisat / Earth Observation Sat",
            nameFa = "ماهواره سنجش از دور انویسات (Envisat)",
            category = SatelliteCategory.VISIBLE,
            designation = "2002-009A",
            defaultTle = TLEData(
                name = "ENVISAT",
                line1 = "1 28654U 02009A   26213.50000000  .00000300  00000-0  12000-4 0  9995",
                line2 = "2 28654  98.5400  80.0000 0001200  90.0000 270.0000 14.38000000150001"
            ),
            isConstellation = false,
            starlinkTrainCount = 1,
            standardMagnitude = 1.8,
            descriptionEn = "Massive 8-ton European environmental satellite in polar Sun-synchronous orbit at 760 km.",
            descriptionFa = "ماهواره بزرگ ۸ تنی زیست‌محیطی اروپا در مدار قطبی همگام با خورشید در ارتفاع ۷۶۰ کیلومتری.",
            isNakedEyeCandidate = true
        )
    )

    fun getById(id: String): SatelliteItem {
        return satellites.find { it.id == id } ?: satellites.first()
    }
}
