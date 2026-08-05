package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType

object AsterismCatalog {

    fun getAsterisms(): List<CelestialObject> {
        return listOf(
            CelestialObject(
                id = "asterism_big_dipper",
                type = ObjectType.ASTERISM,
                nameEn = "Big Dipper (The Plough / Seven Sages)",
                nameFa = "صورتواره ملاقه بزرگ / هفت‌اورنگ (Big Dipper)",
                raDeg = 180.0,
                decDeg = 55.0,
                magnitude = 1.8,
                constellationEn = "Ursa Major",
                constellationFa = "خرس بزرگ (دب اکبر)",
                distanceLightYears = 80.0,
                category = "Famous Northern Asterism",
                descriptionEn = "Iconic seven-star bowl and handle pattern pointing directly to Polaris.",
                descriptionFa = "مشهورترین صورتواره آسمان شمالی شامل ۷ ستاره درخشان که اشاره‌گر ستاره قطبی است.",
                observationTipEn = "Follow pointer stars Dubhe & Merak to locate Polaris.",
                observationTipFa = "امتداد دو ستاره مراق و دبه را برای یافتن ستاره قطبی دنبال کنید."
            ),
            CelestialObject(
                id = "asterism_summer_triangle",
                type = ObjectType.ASTERISM,
                nameEn = "Summer Triangle (Vega - Deneb - Altair)",
                nameFa = "صورتواره مثلث تابستانی (Vega-Deneb-Altair)",
                raDeg = 295.0,
                decDeg = 30.0,
                magnitude = 0.4,
                constellationEn = "Lyra / Cygnus / Aquila",
                constellationFa = "شلیاق / ماکیان / عقاب",
                distanceLightYears = 500.0,
                category = "Seasonal Macro Asterism",
                descriptionEn = "Dominant summer pattern formed by Vega, Deneb, and Altair spanning the Milky Way.",
                descriptionFa = "مثلث عظیم و درخشان آسمان تابستان متشکل از نسرو واقع، دنّب و نسر طائر.",
                observationTipEn = "Milky Way stream flows directly through the center of this triangle.",
                observationTipFa = "نوار جاده کاهکشان (راه شیری) از میان این مثلث عبور می‌کند."
            ),
            CelestialObject(
                id = "asterism_winter_hexagon",
                type = ObjectType.ASTERISM,
                nameEn = "Winter Hexagon (Winter Circle)",
                nameFa = "صورتواره شش‌ضلعی زمستانی (Winter Hexagon)",
                raDeg = 90.0,
                decDeg = 15.0,
                magnitude = 0.1,
                constellationEn = "Multiple Constellations",
                constellationFa = "چندین صورت فلکی",
                distanceLightYears = 200.0,
                category = "Seasonal Macro Asterism",
                descriptionEn = "Vast bright ring connecting Sirius, Procyon, Pollux, Capella, Aldebaran, and Rigel.",
                descriptionFa = "شش‌ضلعی عظیم ستاره‌ای شب‌های زمستان شامل ۶ ستاره قدر اول درخشان.",
                observationTipEn = "Encloses Orion and Gemini within its massive boundaries.",
                observationTipFa = "صورت‌های فلکی جبار و دوپیکر را در میان برمی‌گیرد."
            ),
            CelestialObject(
                id = "asterism_northern_cross",
                type = ObjectType.ASTERISM,
                nameEn = "Northern Cross",
                nameFa = "صورتواره صلیب شمالی (Northern Cross)",
                raDeg = 305.0,
                decDeg = 42.0,
                magnitude = 1.3,
                constellationEn = "Cygnus",
                constellationFa = "ماکیان (قو)",
                distanceLightYears = 1500.0,
                category = "Constellation Subset Asterism",
                descriptionEn = "Cross-shaped backbone of Cygnus the Swan flying down the Milky Way.",
                descriptionFa = "شکل صلیبی‌مانند تنه قو در میان نوار نورانی راه شیری.",
                observationTipEn = "Deneb forms the top of the cross, Albireo the base.",
                observationTipFa = "دنّب در راس بالا و البیرئو در پایه پایین صلیب قرار دارند."
            ),
            CelestialObject(
                id = "asterism_great_square_pegasus",
                type = ObjectType.ASTERISM,
                nameEn = "Great Square of Pegasus",
                nameFa = "صورتواره چهارضلعی بزرگ اسب بالدار (Pegasus Square)",
                raDeg = 345.0,
                decDeg = 20.0,
                magnitude = 2.4,
                constellationEn = "Pegasus / Andromeda",
                constellationFa = "اسب بالدار / آندرومدا",
                distanceLightYears = 140.0,
                category = "Autumn Landmark Asterism",
                descriptionEn = "Prominent geometric diamond/square guiding observers in autumn sky.",
                descriptionFa = "مربع چهارستاره‌ای عظیم شاخص آسمان پاییز.",
                observationTipEn = "Count faint stars inside the square to test local sky quality.",
                observationTipFa = "شمارش ستارگان کم‌نور درون این مربع معیاری برای سنجش تاریکی آسمان است."
            ),
            CelestialObject(
                id = "asterism_teapot",
                type = ObjectType.ASTERISM,
                nameEn = "Teapot of Sagittarius",
                nameFa = "صورتواره قوری (Teapot)",
                raDeg = 280.0,
                decDeg = -28.0,
                magnitude = 2.0,
                constellationEn = "Sagittarius",
                constellationFa = "کمان (قوس)",
                distanceLightYears = 300.0,
                category = "Constellation Core Asterism",
                descriptionEn = "Charming teapot shape where the spout pours steam (the Galactic Center).",
                descriptionFa = "شکل قوری‌مانند قوس که از لوله آن بخار راه شیری (مرکز کهکشان) بیرون می‌زند.",
                observationTipEn = "Look south in summer for the spout pointing right into Galactic Center.",
                observationTipFa = "در تابستان لوله قوری دقیقاً به سمت مرکز کهکشان اشاره دارد."
            ),
            CelestialObject(
                id = "asterism_sickle_leo",
                type = ObjectType.ASTERISM,
                nameEn = "Sickle of Leo",
                nameFa = "صورتواره داس شیر (Sickle of Leo)",
                raDeg = 152.0,
                decDeg = 18.0,
                magnitude = 1.4,
                constellationEn = "Leo",
                constellationFa = "شیر (اسد)",
                distanceLightYears = 90.0,
                category = "Spring Landmark Asterism",
                descriptionEn = "Backward question mark forming head and mane of Leo ending at Regulus.",
                descriptionFa = "شکل علامت سوال معکوس یا داس در سر و یال شیر که به قلب‌الاسد ختم می‌شود.",
                observationTipEn = "Anchored by bright Regulus at the bottom handle.",
                observationTipFa = "ستاره درخشان قلب‌الاسد پایه دسته این داس است."
            )
        )
    }
}
