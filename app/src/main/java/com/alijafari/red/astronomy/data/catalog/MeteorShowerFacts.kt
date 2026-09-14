package com.alijafari.red.astronomy.data.catalog

/**
 * Hand-authored, object-specific bilingual fact sets for the 7 meteor showers in the canonical
 * union list. Five facts each, independent of one another. Sources (parent bodies, peak dates,
 * ZHR, velocities) follow IMO (International Meteor Organization) shower working-list data and
 * are logged in docs/dso-content-research-log.md.
 */
internal object MeteorShowerFacts {

    val map: Map<String, BilingualFacts> = mapOf(
        "shower_perseids" to BilingualFacts(
            en = listOf(
                "The Perseids are debris from comet 109P/Swift-Tuttle, which returns every 133 years.",
                "The shower peaks around August 12-13, when Earth crosses the densest part of the dust stream.",
                "At peak, rates reach about 100 meteors per hour under dark, clear skies.",
                "Perseid meteors are fast, entering the atmosphere at about 59 km/s, and frequently leave persistent glowing trains.",
                "Named for its radiant in Perseus, the shower was known in medieval Europe as the Tears of St. Lawrence."
            ),
            fa = listOf(
                "بارش برساوشی بازمانده ذرات دنباله‌دار سویفت-تاتل (109P) است که هر ۱۳۳ سال یک بار بازمی‌گردد.",
                "اوج این بارش حدود ۲۱ و ۲۲ مرداد رخ می‌دهد، زمانی که زمین از متراکم‌ترین بخش جریان غبار عبور می‌کند.",
                "در اوج بارش و آسمان تاریک و صاف، نرخ رصد به حدود ۱۰۰ شهاب در ساعت می‌رسد.",
                "شهاب‌های برساوشی سریع‌اند و با سرعت حدود ۵۹ کیلومتر بر ثانیه وارد جو می‌شوند و اغلب دنباله نورانی پایداری از خود به جا می‌گذارند.",
                "این بارش که به نام کانون آن در صورت فلکی برساوش نام‌گذاری شده، در اروپای قرون وسطی به «اشک‌های سنت لارنس» مشهور بود."
            )
        ),
        "shower_geminids" to BilingualFacts(
            en = listOf(
                "Unlike most showers, the Geminids originate from an asteroid, 3200 Phaethon, rather than a comet.",
                "The shower peaks around December 13-14 with rates up to 120 meteors per hour, making it the most reliable annual shower.",
                "Geminid meteors are relatively slow, entering at about 35 km/s, and are often bright and colorful.",
                "The radiant lies near the bright star Castor in Gemini, and the shower was first recorded in 1862.",
                "Rates have risen over the past century as Earth passes closer to the core of the dust stream."
            ),
            fa = listOf(
                "برخلاف بیشتر بارش‌ها، بارش دوپیکری از یک سیارک به نام فایتون (3200 Phaethon) سرچشمه می‌گیرد، نه از یک دنباله‌دار.",
                "اوج این بارش حدود ۲۲ و ۲۳ آذر است و نرخ آن به ۱۲۰ شهاب در ساعت می‌رسد؛ به همین دلیل قابل‌اعتمادترین بارش سالانه به شمار می‌رود.",
                "شهاب‌های دوپیکری نسبتاً کُندند و با سرعت حدود ۳۵ کیلومتر بر ثانیه وارد می‌شوند و اغلب درخشان و رنگین‌اند.",
                "کانون این بارش نزدیک ستاره درخشان کاستور در صورت فلکی دوپیکر است و نخستین ثبت آن به سال ۱۸۶۲ بازمی‌گردد.",
                "نرخ این بارش در سده گذشته افزایش یافته است، زیرا زمین از نزدیکی هسته جریان غبار عبور می‌کند."
            )
        ),
        "shower_quadrantids" to BilingualFacts(
            en = listOf(
                "The Quadrantids peak on January 3-4 with rates up to 110 meteors per hour.",
                "Its peak is unusually sharp, lasting only about six hours, because the debris stream is thin and seen nearly edge-on.",
                "The parent body is the near-Earth object 2003 EH1, likely an extinct comet fragment.",
                "The radiant lies in the former constellation Quadrans Muralis, now part of Boötes, which gave the shower its name.",
                "Quadrantid meteors are medium-speed and often produce bright fireballs."
            ),
            fa = listOf(
                "بارش ربعی در ۱۳ و ۱۴ دی به اوج می‌رسد و نرخ آن تا ۱۱۰ شهاب در ساعت است.",
                "اوج این بارش به طور غیرعادی تیز است و تنها حدود شش ساعت طول می‌کشد، زیرا جریان ذرات باریک و تقریباً از لبه دیده می‌شود.",
                "جرم مادر این بارش جسم نزدیک‌زمینی 2003 EH1 است که احتمالاً پاره یک دنباله‌دار خاموش است.",
                "کانون این بارش در صورت فلکی منسوخ‌شده رُبع جداری (Quadrans Muralis) است که اکنون بخشی از عوّاد است و نام بارش از آن گرفته شده است.",
                "شهاب‌های ربعی سرعت متوسطی دارند و اغلب آذرگوی‌های درخشانی می‌سازند."
            )
        ),
        "shower_lyrids" to BilingualFacts(
            en = listOf(
                "The Lyrids are debris from comet C/1861 G1 Thatcher, which orbits the Sun every 415 years.",
                "The shower peaks around April 22-23 with a typical rate of about 18 meteors per hour.",
                "It is one of the oldest recorded showers, with Chinese observations dating back to 687 BCE.",
                "The radiant lies near the bright star Vega in Lyra.",
                "Occasionally the Lyrids produce outbursts of up to 90 meteors per hour, as seen in 1803, 1922, and 1982."
            ),
            fa = listOf(
                "بارش شلیاقی بازمانده ذرات دنباله‌دار تاچر (C/1861 G1) است که هر ۴۱۵ سال یک بار به دور خورشید می‌گردد.",
                "اوج این بارش حدود ۲ اردیبهشت است و نرخ معمول آن حدود ۱۸ شهاب در ساعت است.",
                "این بارش یکی از قدیمی‌ترین بارش‌های ثبت‌شده است و رصدهای چینی آن به سال ۶۸۷ پیش از میلاد بازمی‌گردد.",
                "کانون آن نزدیک ستاره درخشان نسر واقع در صورت فلکی شلیاق قرار دارد.",
                "گاه بارش شلیاقی فوران‌هایی تا ۹۰ شهاب در ساعت نشان داده است؛ مانند سال‌های ۱۸۰۳، ۱۹۲۲ و ۱۹۸۲."
            )
        ),
        "shower_eta_aquariids" to BilingualFacts(
            en = listOf(
                "The Eta Aquariids are debris from Halley's Comet (1P/Halley), along with the October Orionids.",
                "The shower peaks around May 5-6 with rates of about 50 meteors per hour.",
                "Eta Aquariid meteors are fast, entering at about 66 km/s, and often leave glowing trains.",
                "The radiant is near the star Eta Aquarii in Aquarius, and the shower is best seen from the Southern Hemisphere.",
                "Earth crosses this dust stream during Halley's outbound leg of its orbit."
            ),
            fa = listOf(
                "بارش اتا دَلوی بازمانده ذرات دنباله‌دار هالی (1P/Halley) است، همان‌گونه که بارش جباری اکتبر نیز هست.",
                "اوج این بارش حدود ۱۶ اردیبهشت است و نرخ آن حدود ۵۰ شهاب در ساعت است.",
                "شهاب‌های اتا دَلوی سریع‌اند و با سرعت حدود ۶۶ کیلومتر بر ثانیه وارد می‌شوند و اغلب دنباله نورانی به جا می‌گذارند.",
                "کانون آن نزدیک ستاره اتا دلو در صورت فلکی دلو است و این بارش از نیمکره جنوبی بهتر دیده می‌شود.",
                "زمین در بخش بازگشتی مدار هالی از میان این جریان غبار عبور می‌کند."
            )
        ),
        "shower_orionids" to BilingualFacts(
            en = listOf(
                "The Orionids, like the Eta Aquariids, are debris from Halley's Comet (1P/Halley).",
                "The shower peaks around October 21-22 with rates of about 20 meteors per hour.",
                "Orionid meteors are fast, entering at about 66 km/s, and occasionally produce bright fireballs.",
                "The radiant lies in Orion, near the border with Gemini.",
                "The shower is visible from both hemispheres, with best viewing in the hours after midnight."
            ),
            fa = listOf(
                "بارش جباری همانند بارش اتا دَلوی، بازمانده ذرات دنباله‌دار هالی (1P/Halley) است.",
                "اوج این بارش حدود ۲۹ و ۳۰ مهر است و نرخ آن حدود ۲۰ شهاب در ساعت است.",
                "شهاب‌های جباری سریع‌اند و با سرعت حدود ۶۶ کیلومتر بر ثانیه وارد می‌شوند و گاه آذرگوی‌های درخشانی می‌سازند.",
                "کانون آن در صورت فلکی شکارچی و نزدیک مرز دوپیکر قرار دارد.",
                "این بارش از هر دو نیمکره دیده می‌شود و بهترین زمان رصد آن ساعات پس از نیمه‌شب است."
            )
        ),
        "shower_leonids" to BilingualFacts(
            en = listOf(
                "The Leonids are debris from comet 55P/Tempel-Tuttle, which returns every 33 years.",
                "The shower peaks around November 17-18, usually with about 15 meteors per hour.",
                "Leonid meteors are the fastest of the major showers, entering at about 71 km/s.",
                "Roughly every 33 years the Leonids produce meteor storms of thousands of meteors per hour, as in 1833 and 1966.",
                "The radiant lies in Leo, near the bright star Regulus."
            ),
            fa = listOf(
                "بارش اسدی بازمانده ذرات دنباله‌دار تمپل-تاتل (55P) است که هر ۳۳ سال یک بار بازمی‌گردد.",
                "اوج این بارش حدود ۲۶ و ۲۷ آبان است و نرخ معمول آن حدود ۱۵ شهاب در ساعت است.",
                "شهاب‌های اسدی سریع‌ترین شهاب‌های بارش‌های بزرگ‌اند و با سرعت حدود ۷۱ کیلومتر بر ثانیه وارد می‌شوند.",
                "تقریباً هر ۳۳ سال یک بار، بارش اسدی طوفان شهابی با هزاران شهاب در ساعت می‌سازد؛ مانند سال‌های ۱۸۳۳ و ۱۹۶۶.",
                "کانون آن در صورت فلکی شیر و نزدیک ستاره درخشان قلب‌الاسد قرار دارد."
            )
        )
    )
}
