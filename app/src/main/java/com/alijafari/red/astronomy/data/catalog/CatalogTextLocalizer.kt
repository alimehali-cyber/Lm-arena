package com.alijafari.red.astronomy.data.catalog

internal object CatalogTextLocalizer {

    fun persianCategory(categoryEn: String): String {
        exactCategoryFa[categoryEn]?.let { return it }
        var result = categoryEn
        orderedReplacements.forEach { (en, fa) ->
            result = result.replace(en, fa, ignoreCase = false)
        }
        return result
            .replace(" / ", " / ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val exactCategoryFa = mapOf(
        "Spiral Galaxy (SA(s)b)" to "کهکشان مارپیچی (SA(s)b)",
        "Spiral Galaxy (SA(s)cd)" to "کهکشان مارپیچی (SA(s)cd)",
        "Magellanic Spiral / Satellite Galaxy" to "مارپیچی ماژلانی / کهکشان ماهواره‌ای",
        "Dwarf Irregular Galaxy" to "کهکشان کوتوله نامنظم",
        "Open Cluster" to "خوشه باز",
        "Double Open Cluster" to "خوشه باز دوگانه",
        "Globular Cluster" to "خوشه کروی",
        "Emission / Reflection Nebula" to "سحابی گسیلشی / بازتابی",
        "H II Region / Emission Nebula" to "ناحیه H II / سحابی گسیلشی",
        "Giant Emission Nebula" to "سحابی گسیلشی غول‌آسا",
        "Comet 109P/Swift-Tuttle Debris Stream" to "جریان آوار دنباله‌دار 109P/سوئیفت-تاتل",
        "Asteroid 3200 Phaethon Debris" to "آوار سیارک 3200 فایتون",
        "Asteroid 2003 EH1 Debris Stream" to "جریان آوار سیارک 2003 EH1",
        "Comet C/1861 G1 Thatcher Debris" to "آوار دنباله‌دار C/1861 G1 تاچر",
        "Halley's Comet (1P/Halley) Debris" to "آوار دنباله‌دار هالی (1P)",
        "Comet 55P/Tempel-Tuttle Debris" to "آوار دنباله‌دار 55P/تمپل-تاتل",
        "Famous Northern Asterism" to "صورتواره مشهور شمالی",
        "Seasonal Macro Asterism" to "صورتواره فصلی بزرگ",
        "Constellation Core Asterism" to "صورتواره هسته صورت فلکی",
        "Autumn Landmark Asterism" to "صورتواره شاخص پاییزی",
        "Spring Landmark Asterism" to "صورتواره شاخص بهاری",
        "Constellation Subset Asterism" to "صورتواره زیرمجموعه صورت فلکی"
    )

    private val orderedReplacements = listOf(
        "Rotating Ellipsoidal Binary" to "دوتایی بیضوی چرخان",
        "Eclipsing Binary Star" to "ستاره دوتایی گرفتی",
        "Quadruple Star System" to "سامانه ستاره‌ای چهارتایی",
        "Quadruple System" to "سامانه چهارتایی",
        "Sextuple Star System" to "سامانه ستاره‌ای شش‌تایی",
        "Triple Star System" to "سامانه ستاره‌ای سه‌تایی",
        "Triple Blue Giant" to "غول آبی سه‌گانه",
        "Triple Star" to "ستاره سه‌گانه",
        "Double Star" to "ستاره دوتایی",
        "Multiple Star" to "ستاره چندگانه",
        "Multiple System" to "سامانه چندگانه",
        "Main Sequence Star" to "ستاره رشته اصلی",
        "Main Sequence" to "رشته اصلی",
        "Rapidly Rotating Star" to "ستاره تندچرخ",
        "Peculiar Star" to "ستاره خاص",
        "Supergiant Cepheid Variable" to "ابرغول متغیر قیفاووسی",
        "Blue Supergiant" to "ابرغول آبی",
        "White Supergiant" to "ابرغول سفید",
        "Yellow Supergiant" to "ابرغول زرد",
        "Red Supergiant" to "ابرغول سرخ",
        "Supergiant" to "ابرغول",
        "Bright Giant" to "غول درخشان",
        "Blue Giant" to "غول آبی",
        "Orange Giant" to "غول نارنجی",
        "Red Giant" to "غول سرخ",
        "Giant Star" to "ستاره غول",
        "Yellow Subgiant" to "زیرغول زرد",
        "Subgiant" to "زیرغول",
        "Galaxy" to "کهکشان",
        "Nebula" to "سحابی",
        "Cluster" to "خوشه",
        "Asterism" to "صورتواره",
        "Comet" to "دنباله‌دار",
        "Asteroid" to "سیارک",
        "Debris Stream" to "جریان آوار",
        "Debris" to "آوار"
    )
}
