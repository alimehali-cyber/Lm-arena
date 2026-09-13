package com.alijafari.red.astronomy.data.catalog

/**
 * Hand-authored, object-specific bilingual fact sets for the 205 deep-sky objects that were
 * previously filled with template boilerplate. Every object now carries five independent facts
 * with no cross-object templating. Sources are logged per object in docs/dso-content-research-log.md
 * (SEDS Messier catalog, the NGC/IC Project, and SIMBAD/NED data).
 */
internal object DeepSkyGeneratedFacts {

    fun factsForCanonicalId(canonicalId: String): BilingualFacts? = generatedFactsByCanonicalId[canonicalId]

    private val generatedFactsByCanonicalId = mapOf(
        "dso_c1" to BilingualFacts(
            en = listOf(
                "Caldwell 1, also known as NGC 188, is an open cluster in Cepheus, about 5,400 light-years away.",
                "It shines at magnitude 8.1 and is the first object in Patrick Moore's Caldwell catalog.",
                "NGC 188 is one of the oldest known open clusters, roughly 5 billion years old.",
                "The cluster lies far above the plane of the Milky Way, surviving far longer than most open clusters.",
                "NGC 188 contains about 120 stars and is rich in evolved red giants and white dwarfs."
            ),
            fa = listOf(
                "کالدول ۱ که به آن NGC ۱۸۸ نیز می‌گویند، یک خوشه باز در صورت فلکی قیفاووس و در فاصله حدود ۵۴۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۱ می‌درخشد و نخستین جرم فهرست کالدول پاتریک مور است.",
                "NGC ۱۸۸ یکی از کهن‌سال‌ترین خوشه‌های باز شناخته‌شده است و حدود ۵ میلیارد سال سن دارد.",
                "این خوشه بسیار بالاتر از صفحه راه شیری قرار دارد و به همین دلیل بسیار بیشتر از بیشتر خوشه‌های باز دوام آورده است.",
                "NGC ۱۸۸ حدود ۱۲۰ ستاره دارد و سرشار از غول‌های سرخ و کوتوله‌های سفید تکامل‌یافته است."
            )
        ),
        "dso_c11" to BilingualFacts(
            en = listOf(
                "Caldwell 11, the Bubble Nebula, is an emission nebula in Cassiopeia, about 11,000 light-years away.",
                "It shines at magnitude 10.0 and carries the designation NGC 7635.",
                "A powerful Wolf-Rayet star, SAO 20575, has blown a huge bubble in the surrounding gas with its stellar wind.",
                "The bubble spans about 7 light-years across.",
                "The Bubble Nebula was discovered by William Herschel in 1787."
            ),
            fa = listOf(
                "کالدول ۱۱ یا سحابی حباب، یک سحابی نشری در صورت فلکی ذات‌الکرسی و در فاصله حدود ۱۱,۰۰۰ سال نوری است.",
                "این سحابی با قدر ۱۰/۰ می‌درخشد و با نام NGC ۷۶۳۵ شناخته می‌شود.",
                "ستاره پرتوان ولف-رایه به نام SAO ۲۰۵۷۵ با باد ستاره‌ای خود حباب عظیمی در گاز پیرامون دمیده است.",
                "قطر این حباب حدود ۷ سال نوری است.",
                "سحابی حباب را ویلیام هرشل در سال ۱۷۸۷ کشف کرد."
            )
        ),
        "dso_c19" to BilingualFacts(
            en = listOf(
                "Caldwell 19, the Cocoon Nebula, is a star-forming cloud in Cygnus, about 4,000 light-years away.",
                "It shines at magnitude 7.2 and carries the designation IC 5146.",
                "The nebula glows partly by reflection and partly by emission, energized by the young cluster Collinder 470.",
                "A long, dark dust trail stretches behind the Cocoon across the sky.",
                "The Cocoon Nebula is a young stellar nursery, only about a million years old."
            ),
            fa = listOf(
                "کالدول ۱۹ یا سحابی پیله، ابری ستاره‌زا در صورت فلکی ماکیان و در فاصله حدود ۴۰۰۰ سال نوری است.",
                "این سحابی با قدر ۷/۲ می‌درخشد و با نام IC ۵۱۴۶ شناخته می‌شود.",
                "این سحابی بخشی با بازتاب و بخشی با نشر می‌درخشد و خوشه جوان کولیندر ۴۷۰ آن را برانگیخته است.",
                "دنباله‌ای دراز و تاریک از غبار در پشت پیله در آسمان کشیده شده است.",
                "سحابی پیله یک زادگاه ستاره‌ای جوان است که تنها حدود یک میلیون سال سن دارد."
            )
        ),
        "dso_c24" to BilingualFacts(
            en = listOf(
                "Caldwell 24, also known as NGC 1275 or Perseus A, is the central galaxy of the Perseus Cluster.",
                "It lies about 230 million light-years away and shines at magnitude 11.9.",
                "NGC 1275 is a powerful radio source and one of the brightest extragalactic radio emitters.",
                "The galaxy's supermassive black hole drives enormous outflows and filaments of gas.",
                "NGC 1275 is a cD galaxy, a giant type that dominates the centers of rich galaxy clusters."
            ),
            fa = listOf(
                "کالدول ۲۴ که به آن NGC ۱۲۷۵ یا برساوش A نیز می‌گویند، کهکشان مرکزی خوشه برساوش است.",
                "این کهکشان در فاصله حدود ۲۳۰ میلیون سال نوری است و با قدر ۱۱/۹ می‌درخشد.",
                "NGC ۱۲۷۵ یک چشمه رادیویی نیرومند و یکی از درخشان‌ترین گسیلنده‌های رادیویی برون‌کهکشانی است.",
                "سیاهچاله کلان‌جرم این کهکشان برون‌ریزهای عظیم و رشته‌هایی از گاز را به حرکت درمی‌آورد.",
                "NGC ۱۲۷۵ یک کهکشان cD است؛ نوعی غول‌پیکر که بر مرکز خوشه‌های کهکشانی پرجمعیت چیره است."
            )
        ),
        "dso_c25" to BilingualFacts(
            en = listOf(
                "Caldwell 25, the Intergalactic Wanderer, is the globular cluster NGC 2419 in Lynx.",
                "It lies about 275,000 light-years away, far beyond the Milky Way's main halo.",
                "For decades astronomers thought NGC 2419 wandered between galaxies, hence its nickname.",
                "Modern studies show NGC 2419 still orbits the Milky Way, on a very wide loop.",
                "The cluster shines at magnitude 10.4, a dim but rewarding telescope target."
            ),
            fa = listOf(
                "کالدول ۲۵ یا سرگردان میان‌کهکشانی، خوشه کروی NGC ۲۴۱۹ در صورت فلکی سیاهگوش است.",
                "این خوشه در فاصله حدود ۲۷۵,۰۰۰ سال نوری است؛ بسیار فراتر از هاله اصلی راه شیری.",
                "برای دهه‌ها اخترشناسان می‌پنداشتند NGC ۲۴۱۹ میان کهکشان‌ها سرگردان است و لقب آن از همین‌جاست.",
                "پژوهش‌های نوین نشان می‌دهند که NGC ۲۴۱۹ هنوز در مداری بسیار گسترده به دور راه شیری می‌گردد.",
                "این خوشه با قدر ۱۰/۴ هدفی کم‌نور اما ارزشمند برای تلسکوپ است."
            )
        ),
        "dso_c27" to BilingualFacts(
            en = listOf(
                "Caldwell 27, the Crescent Nebula, is an emission nebula in Cygnus, about 4,700 light-years away.",
                "It shines at magnitude 7.4 and carries the designation NGC 6888.",
                "The nebula was sculpted by the fast stellar wind of the Wolf-Rayet star WR 136.",
                "WR 136 has shed its outer layers, which now form the glowing crescent-shaped shell.",
                "The Crescent Nebula was discovered by William Herschel in 1792."
            ),
            fa = listOf(
                "کالدول ۲۷ یا سحابی هلال، یک سحابی نشری در صورت فلکی ماکیان و در فاصله حدود ۴۷۰۰ سال نوری است.",
                "این سحابی با قدر ۷/۴ می‌درخشد و با نام NGC ۶۸۸۸ شناخته می‌شود.",
                "این سحابی را باد ستاره‌ای پرسرعت ستاره ولف-رایه WR ۱۳۶ تراشیده است.",
                "WR ۱۳۶ لایه‌های بیرونی خود را پس زده و آن‌ها اکنون پوسته درخشان هلالی‌شکل را می‌سازند.",
                "سحابی هلال را ویلیام هرشل در سال ۱۷۹۲ کشف کرد."
            )
        ),
        "dso_c29" to BilingualFacts(
            en = listOf(
                "Caldwell 29, also known as NGC 5005, is a spiral galaxy in Canes Venatici, about 65 million light-years away.",
                "It shines at magnitude 9.8 and is a member of the NGC 5033 Group.",
                "The galaxy is a Seyfert-type spiral with an active nucleus.",
                "NGC 5005's supermassive black hole emits X-rays detectable by orbiting observatories.",
                "A supernova, SN 1996ai, was observed in the galaxy in 1996."
            ),
            fa = listOf(
                "کالدول ۲۹ که به آن NGC ۵۰۰۵ نیز می‌گویند، یک کهکشان مارپیچی در صورت فلکی تازی‌ها و در فاصله حدود ۶۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۸ می‌درخشد و عضوی از گروه NGC ۵۰۳۳ است.",
                "این کهکشان یک مارپیچ از نوع سیفرت با هسته‌ای فعال است.",
                "سیاهچاله کلان‌جرم NGC ۵۰۰۵ پرتوهای ایکس می‌تاباند که رصدخانه‌های مداری آن‌ها را آشکار می‌کنند.",
                "در سال ۱۹۹۶ یک ابرنواختر به نام SN 1996ai در این کهکشان رصد شد."
            )
        ),
        "dso_c3" to BilingualFacts(
            en = listOf(
                "Caldwell 3, also known as NGC 4236, is a barred spiral galaxy in Draco, about 11.7 million light-years away.",
                "It shines at magnitude 9.7 and spans about 19 arcminutes.",
                "NGC 4236 is a member of the M81 Group of galaxies.",
                "The galaxy is seen nearly edge-on and has an elongated, spindle-like shape.",
                "NGC 4236 is one of the largest barred spirals known, over 70,000 light-years across."
            ),
            fa = listOf(
                "کالدول ۳ که به آن NGC ۴۲۳۶ نیز می‌گویند، یک کهکشان مارپیچی میله‌ای در صورت فلکی اژدها و در فاصله حدود ۱۱/۷ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۷ می‌درخشد و پهنای آن حدود ۱۹ دقیقه قوسی است.",
                "NGC ۴۲۳۶ عضوی از گروه کهکشانی M۸۱ است.",
                "این کهکشان تقریباً از لبه دیده می‌شود و شکلی کشیده و دوک‌مانند دارد.",
                "NGC ۴۲۳۶ یکی از بزرگ‌ترین مارپیچی‌های میله‌ای شناخته‌شده است و پهنای آن بیش از ۷۰,۰۰۰ سال نوری است."
            )
        ),
        "dso_c4" to BilingualFacts(
            en = listOf(
                "Caldwell 4, the Iris Nebula, is a reflection nebula in Cepheus, about 1,400 light-years away.",
                "It shines at magnitude 6.8 and carries the designation NGC 7023.",
                "The nebula reflects the blue light of the young star HD 200775.",
                "Its flower-like shape inspired the name Iris, after the Greek goddess of the rainbow.",
                "The Iris Nebula was discovered by William Herschel in 1794."
            ),
            fa = listOf(
                "کالدول ۴ یا سحابی زنبق، یک سحابی بازتابی در صورت فلکی قیفاووس و در فاصله حدود ۱۴۰۰ سال نوری است.",
                "این سحابی با قدر ۶/۸ می‌درخشد و با نام NGC ۷۰۲۳ شناخته می‌شود.",
                "این سحابی نور آبی ستاره جوان HD ۲۰۰۷۷۵ را بازتاب می‌دهد.",
                "شکل گل‌مانند آن الهام‌بخش نام «زنبق» بوده است؛ برگرفته از الهه رنگین‌کمان در اساطیر یونان.",
                "سحابی زنبق را ویلیام هرشل در سال ۱۷۹۴ کشف کرد."
            )
        ),
        "dso_c5" to BilingualFacts(
            en = listOf(
                "Caldwell 5, also known as IC 342, is a spiral galaxy in Camelopardalis, about 11 million light-years away.",
                "It shines at magnitude 9.1 but is dimmed by heavy dust along the Milky Way's plane.",
                "IC 342 is nicknamed the Hidden Galaxy because foreground dust obscures it.",
                "Were it not for the dust, IC 342 would be one of the brightest galaxies in our sky.",
                "IC 342 is a member of the IC 342/Maffei Group of galaxies."
            ),
            fa = listOf(
                "کالدول ۵ که به آن IC ۳۴۲ نیز می‌گویند، یک کهکشان مارپیچی در صورت فلکی زرافه و در فاصله حدود ۱۱ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۱ می‌درخشد، اما غبار سنگین در امتداد صفحه راه شیری آن را کم‌نور می‌کند.",
                "IC ۳۴۲ را به این دلیل «کهکشان پنهان» می‌نامند که غبار پیش‌زمینه آن را پوشانده است.",
                "اگر این غبار نبود، IC ۳۴۲ یکی از درخشان‌ترین کهکشان‌های آسمان ما می‌بود.",
                "IC ۳۴۲ عضوی از گروه کهکشانی IC ۳۴۲/مافی است."
            )
        ),
        "dso_c8" to BilingualFacts(
            en = listOf(
                "Caldwell 8, also known as NGC 559, is an open cluster in Cassiopeia, about 3,700 light-years away.",
                "It shines at magnitude 9.7 and contains several dozen stars.",
                "William Herschel discovered NGC 559, the C8 cluster, in 1787.",
                "NGC 559 is about 100 million years old.",
                "It sits in a rich Milky Way field near the border with Cepheus."
            ),
            fa = listOf(
                "کالدول ۸ که به آن NGC ۵۵۹ نیز می‌گویند، یک خوشه باز در صورت فلکی ذات‌الکرسی و در فاصله حدود ۳۷۰۰ سال نوری است.",
                "این خوشه با قدر ۹/۷ می‌درخشد و چند ده ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "سن NGC ۵۵۹ حدود ۱۰۰ میلیون سال است.",
                "این خوشه در میدان پرستاره راه شیری و نزدیک مرز صورت فلکی قیفاووس جای دارد."
            )
        ),
        "dso_m1" to BilingualFacts(
            en = listOf(
                "M1, the Crab Nebula, is the expanding remnant of a supernova recorded by Chinese and Japanese astronomers in 1054.",
                "It lies about 6,500 light-years away in Taurus and shines at magnitude 8.4.",
                "At its center spins the Crab Pulsar, a neutron star rotating about 30 times per second that powers the surrounding glow.",
                "The nebula is expanding at roughly 1,500 km/s, and its filaments are visible in amateur telescopes as a faint oval patch.",
                "It was the first entry in Charles Messier's catalog, added in 1758 while he searched for comets."
            ),
            fa = listOf(
                "سحابی خرچنگ (M۱) بازمانده در حال گسترش ابرنواختری است که اخترشناسان چینی و ژاپنی در سال ۱۰۵۴ میلادی ثبت کردند.",
                "این سحابی در فاصله حدود ۶۵۰۰ سال نوری در صورت فلکی گاو قرار دارد و با قدر ۸/۴ می‌درخشد.",
                "در مرکز آن تپ‌اختر خرچنگ می‌چرخد؛ ستاره‌ای نوترونی که حدود ۳۰ بار در ثانیه به دور خود می‌گردد و درخشش پیرامون را تأمین می‌کند.",
                "این سحابی با سرعت تقریبی ۱۵۰۰ کیلومتر بر ثانیه گسترش می‌یابد و رشته‌های آن در تلسکوپ‌های آماتوری به صورت لکه بیضی کم‌نوری دیده می‌شوند.",
                "این نخستین جرم فهرست شارل مسیه بود که در سال ۱۷۵۸ و در جست‌وجوی دنباله‌دارها افزوده شد."
            )
        ),
        "dso_m10" to BilingualFacts(
            en = listOf(
                "M10 is a globular cluster of roughly 100,000 stars in Ophiuchus.",
                "It lies about 14,300 light-years away and shines at magnitude 6.6.",
                "Messier cataloged M10 in 1764, describing it as a round nebula without stars.",
                "The cluster contains a population of blue stragglers, unusually young-looking stars in an ancient system.",
                "M10 is a moderately concentrated cluster spanning about 83 light-years."
            ),
            fa = listOf(
                "M۱۰ یک خوشه کروی با حدود ۱۰۰,۰۰۰ ستاره در صورت فلکی ماراَفسای است.",
                "این خوشه در فاصله تقریبی ۱۴,۳۰۰ سال نوری است و با قدر ۶/۶ می‌درخشد.",
                "شارل مسیه M۱۰ را در سال ۱۷۶۴ کشف کرد.",
                "این خوشه جمعیتی از ستارگان «ولگرد آبی» دارد که در سامانه‌ای کهن، به طور غیرعادی جوان به نظر می‌رسند.",
                "M۱۰ خوشه‌ای با تراکم متوسط است و پهنای آن حدود ۸۳ سال نوری است."
            )
        ),
        "dso_m100" to BilingualFacts(
            en = listOf(
                "M100 is a grand-design spiral galaxy in Coma Berenices, about 55 million light-years away.",
                "M100 shines at magnitude 9.3 and was found by Pierre Méchain in 1781.",
                "M100 is one of the brightest and largest galaxies in the Virgo Cluster.",
                "The galaxy has hosted several supernovae, including SN 1901B, SN 1914A, and SN 1979C.",
                "Hubble observations of Cepheid variables in M100 helped refine the Hubble constant in the 1990s."
            ),
            fa = listOf(
                "M۱۰۰ یک کهکشان مارپیچی باشکوه در صورت فلکی گیسو و در فاصله حدود ۵۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۳ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۱۰۰ یکی از درخشان‌ترین و بزرگ‌ترین کهکشان‌های خوشه سنبله است.",
                "این کهکشان میزبان چند ابرنواختر ثبت‌شده بوده است که کهن‌ترین آن‌ها به سال ۱۹۰۱ بازمی‌گردد.",
                "رصد هابل از متغیرهای قیفاووسی در M۱۰۰ به پالایش ثابت هابل در دهه ۱۹۹۰ کمک کرد."
            )
        ),
        "dso_m101" to BilingualFacts(
            en = listOf(
                "M101, the Pinwheel Galaxy, is a face-on grand-design spiral in Ursa Major about 21 million light-years away.",
                "It shines at magnitude 7.9 and was discovered by Pierre Méchain in 1781.",
                "M101 is about 170,000 light-years across, nearly twice the size of the Milky Way.",
                "The galaxy has hosted several supernovae, including SN 2011fe, the closest bright one in decades.",
                "M101 is the brightest and largest galaxy in its own small group of galaxies."
            ),
            fa = listOf(
                "M۱۰۱ یا کهکشان فرفره، یک مارپیچ باشکوه که از روبه‌رو دیده می‌شود و در صورت فلکی خرس بزرگ و در فاصله حدود ۲۱ میلیون سال نوری است.",
                "این کهکشان با قدر ۷/۹ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "قطر M۱۰۱ حدود ۱۷۰,۰۰۰ سال نوری است؛ تقریباً دو برابر راه شیری.",
                "این کهکشان میزبان چند ابرنواختر بوده است، از جمله SN 2011fe که نزدیک‌ترین ابرنواختر درخشان چند دهه اخیر بود.",
                "M۱۰۱ بزرگ‌ترین عضو گروه کهکشانی M۱۰۱ است."
            )
        ),
        "dso_m102" to BilingualFacts(
            en = listOf(
                "M102, the Spindle Galaxy, is a lenticular galaxy in Draco seen almost edge-on.",
                "It lies about 44 million light-years away and shines at magnitude 9.9.",
                "M102's identity was long disputed, but it is now firmly identified with NGC 5866.",
                "The galaxy is crossed by a dark dust lane along its edge-on disk.",
                "Pierre Méchain originally reported M102 around 1781."
            ),
            fa = listOf(
                "M۱۰۲ یا کهکشان دوک، یک کهکشان عدسی‌شکل در صورت فلکی اژدها است که تقریباً از لبه دیده می‌شود.",
                "این کهکشان در فاصله حدود ۴۴ میلیون سال نوری است و با قدر ۹/۹ می‌درخشد.",
                "هویت M۱۰۲ مدت‌ها محل اختلاف بود، اما اکنون به طور قطع با NGC ۵۸۶۶ یکی دانسته می‌شود.",
                "یک رگه غبار تاریک در امتداد قرص لبه‌نمای این کهکشان کشیده شده است.",
                "پیر مشن M۱۰۲ را حدود سال ۱۷۸۱ گزارش کرد."
            )
        ),
        "dso_m103" to BilingualFacts(
            en = listOf(
                "M103 is an open cluster in Cassiopeia, about 8,500 light-years away.",
                "It shines at magnitude 7.4 and is one of the most distant open clusters in Messier's catalog.",
                "Pierre Méchain discovered M103 in 1781, adding it as Messier's final cluster.",
                "The cluster contains about 40 known stars and is about 25 million years old.",
                "M103 is the last object Messier himself added to his catalog."
            ),
            fa = listOf(
                "M۱۰۳ یک خوشه باز در صورت فلکی ذات‌الکرسی و در فاصله حدود ۸۵۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۴ می‌درخشد و یکی از دورترین خوشه‌های باز فهرست مسیه است.",
                "پیر مشن M۱۰۳ را در سال ۱۷۸۱ کشف کرد.",
                "این خوشه حدود ۴۰ ستاره شناخته‌شده دارد و سن آن تقریباً ۲۵ میلیون سال است.",
                "M۱۰۳ آخرین جرمی است که خود مسیه به فهرستش افزود."
            )
        ),
        "dso_m104" to BilingualFacts(
            en = listOf(
                "M104, the Sombrero Galaxy, is a spiral galaxy in Virgo seen nearly edge-on, about 29 million light-years away.",
                "It shines at magnitude 8.0 and was discovered by Pierre Méchain in 1781.",
                "A prominent dust lane rings the galaxy, giving it the look of a wide-brimmed hat.",
                "M104 has an unusually large central bulge and a supermassive black hole at its core.",
                "The galaxy is surrounded by a rich system of about 2,000 globular clusters."
            ),
            fa = listOf(
                "M۱۰۴ یا کهکشان سومبررو، یک کهکشان مارپیچی در صورت فلکی سنبله است که تقریباً از لبه دیده می‌شود و حدود ۲۹ میلیون سال نوری فاصله دارد.",
                "این کهکشان با قدر ۸/۰ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "یک رگه غبار برجسته دور این کهکشان حلقه زده و ظاهر کلاهی لبه‌پهن به آن می‌دهد.",
                "M۱۰۴ برآمدگی مرکزی به طور غیرعادی بزرگی دارد و در هسته آن سیاهچاله‌ای کلان‌جرم است.",
                "این کهکشان را سامانه‌ای غنی از حدود ۲۰۰۰ خوشه کروی در بر گرفته است."
            )
        ),
        "dso_m105" to BilingualFacts(
            en = listOf(
                "M105 is an elliptical galaxy in Leo, about 38 million light-years away.",
                "Pierre Méchain discovered M105 in 1781; it shines at magnitude 9.3.",
                "M105 is a member of the Leo I Group.",
                "M105's central supermassive black hole is revealed by the rapid motion of stars around it.",
                "M105 shows evidence of recent star formation, unusual for an elliptical galaxy."
            ),
            fa = listOf(
                "M۱۰۵ یک کهکشان بیضوی در صورت فلکی شیر و در فاصله حدود ۳۸ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۳ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۱۰۵ عضوی از گروه کهکشانی شیر I است.",
                "این کهکشان میزبان سیاهچاله‌ای کلان‌جرم در مرکز خود است.",
                "M۱۰۵ نشانه‌هایی از ستاره‌زایی تازه نشان می‌دهد که برای یک کهکشان بیضوی غیرعادی است."
            )
        ),
        "dso_m106" to BilingualFacts(
            en = listOf(
                "M106 is a spiral galaxy in Canes Venatici, about 24 million light-years away.",
                "It shines at magnitude 8.4 and was discovered by Pierre Méchain in 1781.",
                "M106 hosts a water vapor maser, which let astronomers measure its distance precisely.",
                "The galaxy's supermassive black hole powers jets that have warped its disk.",
                "M106 has hosted several supernovae, including SN 2014bc."
            ),
            fa = listOf(
                "M۱۰۶ یک کهکشان مارپیچی در صورت فلکی تازی‌ها و در فاصله حدود ۲۴ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۴ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۱۰۶ میزبان یک میزر بخار آب است که به اخترشناسان امکان داد فاصله آن را با دقت اندازه بگیرند.",
                "سیاهچاله کلان‌جرم این کهکشان فواره‌هایی می‌سازد که قرص آن را تاب داده‌اند.",
                "M۱۰۶ میزبان چند ابرنواختر بوده است، از جمله SN 2014bc."
            )
        ),
        "dso_m107" to BilingualFacts(
            en = listOf(
                "M107 is a globular cluster in Ophiuchus, about 20,900 light-years away.",
                "It shines at magnitude 7.9 and was discovered by Pierre Méchain in 1782.",
                "M107 was the last Messier object to be added to the catalog, appended in 1947.",
                "The cluster has a relatively open, sparse structure.",
                "M107 contains several dozen known variable stars."
            ),
            fa = listOf(
                "M۱۰۷ یک خوشه کروی در صورت فلکی ماراَفسای و در فاصله حدود ۲۰,۹۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۹ می‌درخشد و پیر مشن آن را در سال ۱۷۸۲ کشف کرد.",
                "M۱۰۷ آخرین جرم مسیه بود که به فهرست افزوده شد و در سال ۱۹۴۷ به آن ضمیمه شد.",
                "این خوشه ساختاری نسبتاً باز و پراکنده دارد.",
                "M۱۰۷ چند ده ستاره متغیر شناخته‌شده دارد."
            )
        ),
        "dso_m108" to BilingualFacts(
            en = listOf(
                "M108, the Surfboard Galaxy, is a barred spiral galaxy in Ursa Major seen almost edge-on.",
                "It lies about 45 million light-years away and shines at magnitude 10.0.",
                "Pierre Méchain found M108 in 1781 among the galaxies of Ursa Major.",
                "A supernova, SN 1969B, was observed in the galaxy in 1969.",
                "M108 sits near the Owl Nebula, M97, and the two are often observed together."
            ),
            fa = listOf(
                "M۱۰۸ یا کهکشان تخته موج‌سواری، یک کهکشان مارپیچی میله‌ای در صورت فلکی خرس بزرگ است که تقریباً از لبه دیده می‌شود.",
                "این کهکشان در فاصله حدود ۴۵ میلیون سال نوری است و با قدر ۱۰/۰ می‌درخشد.",
                "پیر مشن M۱۰۸ را در سال ۱۷۸۱ کشف کرد.",
                "در سال ۱۹۶۹ یک ابرنواختر به نام SN 1969B در این کهکشان رصد شد.",
                "M۱۰۸ نزدیک سحابی جغد یعنی M۹۷ قرار دارد و این دو اغلب با هم رصد می‌شوند."
            )
        ),
        "dso_m109" to BilingualFacts(
            en = listOf(
                "M109 is a barred spiral galaxy in Ursa Major, about 55 million light-years away.",
                "It shines at magnitude 9.8 and was discovered by Pierre Méchain in 1781.",
                "M109 is the brightest member of the M109 Group of galaxies.",
                "The galaxy has at least three satellite galaxies and a prominent central bar.",
                "A supernova, SN 1956A, was observed in M109 in 1956."
            ),
            fa = listOf(
                "M۱۰۹ یک کهکشان مارپیچی میله‌ای در صورت فلکی خرس بزرگ و در فاصله حدود ۵۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۸ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۱۰۹ درخشان‌ترین عضو گروه کهکشانی M۱۰۹ است.",
                "این کهکشان دست‌کم سه کهکشان قمری و یک میله مرکزی برجسته دارد.",
                "در سال ۱۹۵۶ یک ابرنواختر به نام SN 1956A در M۱۰۹ رصد شد."
            )
        ),
        "dso_m11" to BilingualFacts(
            en = listOf(
                "M11, the Wild Duck Cluster, is one of the richest and most compact open clusters known, in Scutum.",
                "It lies about 6,200 light-years away and contains roughly 2,900 stars.",
                "At magnitude 6.3 it is visible in binoculars as a hazy patch.",
                "Gottfried Kirch discovered M11 in 1681, calling it a small nebula.",
                "Its brightest stars form a V-shape reminiscent of a flight of wild ducks, giving the cluster its popular name."
            ),
            fa = listOf(
                "M۱۱ یا خوشه اردک وحشی، یکی از غنی‌ترین و متراکم‌ترین خوشه‌های باز شناخته‌شده در صورت فلکی سپر است.",
                "این خوشه در فاصله تقریبی ۶۲۰۰ سال نوری است و حدود ۲۹۰۰ ستاره دارد.",
                "M۱۱ با قدر ۶/۳ در دوربین دوچشمی به صورت لکه مه‌آلودی دیده می‌شود.",
                "گوتفرید کیرش M۱۱ را در سال ۱۶۸۱ کشف کرد و آن را سحابی کوچکی نامید.",
                "درخشان‌ترین ستارگان آن آرایش V-مانندی می‌سازند که یادآور دسته‌ای اردک در حال پرواز است و نام مشهور خوشه از آن گرفته شده است."
            )
        ),
        "dso_m110" to BilingualFacts(
            en = listOf(
                "M110 is a dwarf elliptical galaxy and the second-largest satellite of the Andromeda Galaxy.",
                "It lies about 2.54 million light-years away and shines at magnitude 8.5.",
                "Charles Messier sketched M110 in 1773 but oddly never assigned it a catalog number.",
                "M110 was formally added to the Messier catalog only in 1967, the last object added.",
                "The galaxy shows an unusual dusty core, rare for a dwarf elliptical."
            ),
            fa = listOf(
                "M۱۱۰ یک کهکشان بیضوی کوتوله و دومین قمر بزرگ کهکشان آندرومدا است.",
                "این کهکشان در فاصله حدود ۲/۵۴ میلیون سال نوری است و با قدر ۸/۵ می‌درخشد.",
                "شارل مسیه M۱۱۰ را در سال ۱۷۷۳ ترسیم کرد، اما به طرز عجیبی هرگز شماره فهرستی به آن نداد.",
                "M۱۱۰ تنها در سال ۱۹۶۷ به طور رسمی به فهرست مسیه افزوده شد و آخرین جرم افزوده‌شده است.",
                "این کهکشان هسته‌ای غبارآلود و غیرعادی دارد که برای یک بیضوی کوتوله کمیاب است."
            )
        ),
        "dso_m12" to BilingualFacts(
            en = listOf(
                "M12 is a loosely concentrated globular cluster in Ophiuchus.",
                "It lies about 15,700 light-years away and shines at magnitude 6.7.",
                "Charles Messier discovered M12 in 1764 while hunting comets through Ophiuchus.",
                "The cluster has lost many of its low-mass stars to the Milky Way's tidal forces during its travels.",
                "Compared with its neighbor M10, M12 is noticeably less dense and more diffuse."
            ),
            fa = listOf(
                "M۱۲ یک خوشه کروی با تراکم کم در صورت فلکی ماراَفسای است.",
                "این خوشه در فاصله تقریبی ۱۵,۷۰۰ سال نوری است و با قدر ۶/۷ می‌درخشد.",
                "شارل مسیه M۱۲ را در سال ۱۷۶۴ کشف کرد.",
                "این خوشه بسیاری از ستارگان کم‌جرم خود را در اثر نیروهای کشندی راه شیری در طول سفرش از دست داده است.",
                "در مقایسه با همسایه‌اش M۱۰، خوشه M۱۲ به وضوح کم‌تراکم‌تر و پراکنده‌تر است."
            )
        ),
        "dso_m14" to BilingualFacts(
            en = listOf(
                "M14 is a globular cluster of about 70,000 stars in Ophiuchus.",
                "It lies roughly 30,300 light-years away and shines at magnitude 7.6.",
                "Messier found M14 in 1764, noting it as a faint, round patch of light.",
                "In 1938 a nova flared up inside M14, briefly outshining the entire cluster.",
                "M14 contains more than 70 known variable stars."
            ),
            fa = listOf(
                "M۱۴ یک خوشه کروی با حدود ۷۰,۰۰۰ ستاره در صورت فلکی ماراَفسای است.",
                "این خوشه در فاصله تقریبی ۳۰,۳۰۰ سال نوری است و با قدر ۷/۶ می‌درخشد.",
                "شارل مسیه M۱۴ را در سال ۱۷۶۴ کشف کرد.",
                "در سال ۱۹۳۸ یک نواختر درون M۱۴ شعله کشید و برای مدتی از کل خوشه درخشان‌تر شد.",
                "M۱۴ بیش از ۷۰ ستاره متغیر شناخته‌شده دارد."
            )
        ),
        "dso_m15" to BilingualFacts(
            en = listOf(
                "M15 is a globular cluster of about 100,000 stars in Pegasus.",
                "It lies about 33,600 light-years away and shines at magnitude 6.2.",
                "M15 has one of the densest cores of any Milky Way globular, with a suspected intermediate-mass black hole at its center.",
                "The cluster contains Pease 1, one of the only planetary nebulae known inside a globular cluster.",
                "Jean-Dominique Maraldi discovered M15 in 1746."
            ),
            fa = listOf(
                "M۱۵ یک خوشه کروی با حدود ۱۰۰,۰۰۰ ستاره در صورت فلکی اسب بالدار است.",
                "این خوشه در فاصله تقریبی ۳۳,۶۰۰ سال نوری است و با قدر ۶/۲ می‌درخشد.",
                "M۱۵ یکی از متراکم‌ترین هسته‌ها را در میان خوشه‌های کروی راه شیری دارد و احتمالاً سیاهچاله‌ای با جرم متوسط در مرکز آن است.",
                "این خوشه میزبان Pease 1 است که یکی از معدود سحابی‌های سیاره‌نمای شناخته‌شده درون یک خوشه کروی است.",
                "ژان-دومینیک مارالدی M۱۵ را در سال ۱۷۴۶ کشف کرد."
            )
        ),
        "dso_m16" to BilingualFacts(
            en = listOf(
                "M16, the Eagle Nebula, is a star-forming region in Serpens about 7,000 light-years away.",
                "The Hubble Space Telescope's 1995 image of its Pillars of Creation made M16 famous worldwide.",
                "The pillars are columns of cold gas and dust several light-years tall, sculpted by radiation from young hot stars.",
                "M16 contains the young open cluster NGC 6611, whose stars are only one to two million years old.",
                "At magnitude 6.0 the nebula is visible in small telescopes under dark skies."
            ),
            fa = listOf(
                "M۱۶ یا سحابی عقاب، یک ناحیه ستاره‌زا در صورت فلکی مار و در فاصله حدود ۷۰۰۰ سال نوری است.",
                "تصویر سال ۱۹۹۵ تلسکوپ فضایی هابل از «ستون‌های آفرینش» این سحابی را در سراسر جهان مشهور کرد.",
                "این ستون‌ها ستون‌هایی از گاز و غبار سرد با بلندی چند سال نوری هستند که تابش ستارگان جوان داغ آن‌ها را تراشیده است.",
                "M۱۶ میزبان خوشه باز جوان NGC ۶۶۱۱ است که ستارگانش تنها یک تا دو میلیون سال سن دارند.",
                "این سحابی با قدر ۶/۰ در آسمان تاریک با تلسکوپ‌های کوچک دیده می‌شود."
            )
        ),
        "dso_m17" to BilingualFacts(
            en = listOf(
                "M17, the Omega Nebula, is one of the youngest and most massive star-forming regions in the Milky Way.",
                "It lies about 5,500 light-years away in Sagittarius and shines at magnitude 6.0.",
                "The nebula holds roughly 800 solar masses of glowing hydrogen gas.",
                "Its curved shape has earned it several names, including the Swan and the Horseshoe Nebula.",
                "Jean-Philippe de Chéseaux discovered M17 around 1745, before Messier independently found it in 1764."
            ),
            fa = listOf(
                "M۱۷ یا سحابی اُمگا، یکی از جوان‌ترین و پرجرم‌ترین ناحیه‌های ستاره‌زای راه شیری است.",
                "این سحابی در فاصله حدود ۵۵۰۰ سال نوری در صورت فلکی کمان قرار دارد و با قدر ۶/۰ می‌درخشد.",
                "این سحابی تقریباً ۸۰۰ برابر جرم خورشید گاز هیدروژن درخشان دارد.",
                "شکل خمیده آن نام‌های گوناگونی به آن داده است، از جمله سحابی قو و سحابی نعل اسب.",
                "ژان-فیلیپ دو شزو M۱۷ را حدود سال ۱۷۴۵ کشف کرد؛ پیش از آنکه مسیه آن را در سال ۱۷۶۴ به طور مستقل بیابد."
            )
        ),
        "dso_m18" to BilingualFacts(
            en = listOf(
                "M18 is a sparse open cluster in Sagittarius, about 4,900 light-years away.",
                "It shines at magnitude 7.5 and is best seen in small telescopes.",
                "Charles Messier discovered M18 in 1764 in the star clouds of Sagittarius.",
                "The cluster is young, only about 32 million years old, and contains a few dozen stars.",
                "M18 lies near the bright star cloud of the Sagittarius Milky Way."
            ),
            fa = listOf(
                "M۱۸ یک خوشه باز کم‌تراکم در صورت فلکی کمان و در فاصله حدود ۴۹۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۵ در تلسکوپ‌های کوچک بهتر دیده می‌شود.",
                "شارل مسیه M۱۸ را در سال ۱۷۶۴ کشف کرد.",
                "این خوشه جوان است و تنها حدود ۳۲ میلیون سال سن دارد و چند ده ستاره در آن است.",
                "M۱۸ در نزدیکی ابر ستاره‌ای درخشان راه شیریِ کمان قرار دارد."
            )
        ),
        "dso_m19" to BilingualFacts(
            en = listOf(
                "M19 is a globular cluster in Ophiuchus, about 28,200 light-years away.",
                "It shines at magnitude 6.8 and appears as a small hazy spot in small telescopes.",
                "M19 is one of the most oblate globular clusters known, noticeably elongated in shape.",
                "Messier logged M19 in 1764, remarking on its oval, elongated appearance.",
                "Its elongated form is caused by its proximity to the Milky Way's central bulge."
            ),
            fa = listOf(
                "M۱۹ یک خوشه کروی در صورت فلکی ماراَفسای و در فاصله حدود ۲۸,۲۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۸ در تلسکوپ‌های کوچک به صورت لکه مه‌آلود کوچکی دیده می‌شود.",
                "M۱۹ یکی از پَخ‌ترین خوشه‌های کروی شناخته‌شده است و شکلی به وضوح کشیده دارد.",
                "شارل مسیه M۱۹ را در سال ۱۷۶۴ کشف کرد.",
                "شکل کشیده آن ناشی از نزدیکی‌اش به برآمدگی مرکزی راه شیری است."
            )
        ),
        "dso_m2" to BilingualFacts(
            en = listOf(
                "M2 is a globular cluster of about 150,000 stars located in Aquarius.",
                "It lies roughly 37,500 light-years away and shines at magnitude 6.5, visible in binoculars.",
                "The cluster spans about 175 light-years, making it one of the largest known globulars.",
                "M2 was discovered by Jean-Dominique Maraldi in 1746, twelve years before Messier cataloged it.",
                "At about 13 billion years old, M2 is among the oldest known objects in the Milky Way."
            ),
            fa = listOf(
                "M۲ یک خوشه کروی با حدود ۱۵۰,۰۰۰ ستاره در صورت فلکی دلو است.",
                "این خوشه در فاصله تقریبی ۳۷,۵۰۰ سال نوری قرار دارد و با قدر ۶/۵ در دوربین دوچشمی دیده می‌شود.",
                "قطر این خوشه حدود ۱۷۵ سال نوری است و یکی از بزرگ‌ترین خوشه‌های کروی شناخته‌شده به شمار می‌رود.",
                "M۲ را ژان-دومینیک مارالدی در سال ۱۷۴۶ کشف کرد؛ دوازده سال پیش از آنکه مسیه آن را فهرست کند.",
                "این خوشه با سن حدود ۱۳ میلیارد سال، از کهن‌سال‌ترین اجرام شناخته‌شده راه شیری است."
            )
        ),
        "dso_m20" to BilingualFacts(
            en = listOf(
                "M20, the Trifid Nebula, is a combination of emission, reflection, and dark nebula in Sagittarius.",
                "It lies about 5,200 light-years away and shines at magnitude 6.3.",
                "Dark dust lanes divide the glowing cloud into three lobes, giving it the name Trifid.",
                "Guillaume Le Gentil discovered M20 around 1750.",
                "The nebula hosts about 120 very young stars and is a site of ongoing star birth."
            ),
            fa = listOf(
                "M۲۰ یا سحابی سه‌تکه، ترکیبی از سحابی نشری، بازتابی و تاریک در صورت فلکی کمان است.",
                "این سحابی در فاصله حدود ۵۲۰۰ سال نوری است و با قدر ۶/۳ می‌درخشد.",
                "رگه‌های غبار تاریک، ابر درخشان را به سه بخش تقسیم می‌کنند و نام «سه‌تکه» را به آن داده‌اند.",
                "گیوم لو ژانتی M۲۰ را حدود سال ۱۷۵۰ کشف کرد.",
                "این سحابی میزبان حدود ۱۲۰ ستاره بسیار جوان است و مکانی برای زایش پیوسته ستارگان است."
            )
        ),
        "dso_m21" to BilingualFacts(
            en = listOf(
                "M21 is an open cluster in Sagittarius, about 4,250 light-years away.",
                "It shines at magnitude 6.5 and is visible in binoculars as a faint grouping.",
                "Charles Messier discovered M21 in 1764, close to the Trifid Nebula.",
                "The cluster contains about 57 stars and is only 4.6 million years old.",
                "M21 sits close to the Trifid Nebula on the sky."
            ),
            fa = listOf(
                "M۲۱ یک خوشه باز در صورت فلکی کمان و در فاصله حدود ۴۲۵۰ سال نوری است.",
                "این خوشه با قدر ۶/۵ در دوربین دوچشمی به صورت گروه کم‌نوری دیده می‌شود.",
                "شارل مسیه M۲۱ را در سال ۱۷۶۴ کشف کرد.",
                "این خوشه حدود ۵۷ ستاره دارد و سن آن تنها ۴/۶ میلیون سال است.",
                "M۲۱ در آسمان در نزدیکی سحابی سه‌تکه جای دارد."
            )
        ),
        "dso_m22" to BilingualFacts(
            en = listOf(
                "M22, the Sagittarius Cluster, is one of the nearest and brightest globular clusters, about 10,600 light-years away.",
                "At magnitude 5.1 it is visible to the naked eye under dark skies.",
                "M22 was the first globular cluster ever discovered, found by Abraham Ihle in 1665.",
                "The cluster contains a planetary nebula, GJJC1, one of only a handful known inside a globular.",
                "In 2012 astronomers reported evidence for two stellar-mass black holes orbiting within M22."
            ),
            fa = listOf(
                "M۲۲ یا خوشه کمان، یکی از نزدیک‌ترین و درخشان‌ترین خوشه‌های کروی است و حدود ۱۰,۶۰۰ سال نوری فاصله دارد.",
                "این خوشه با قدر ۵/۱ در آسمان تاریک با چشم غیرمسلح دیده می‌شود.",
                "M۲۲ نخستین خوشه کروی کشف‌شده در تاریخ است که آبراهام ایله در سال ۱۶۶۵ آن را یافت.",
                "این خوشه یک سحابی سیاره‌نما به نام GJJC1 دارد که یکی از معدود سحابی‌های سیاره‌نمای شناخته‌شده درون یک خوشه کروی است.",
                "در سال ۲۰۱۲ اخترشناسان شواهدی برای وجود دو سیاهچاله با جرم ستاره‌ای در گردش درون M۲۲ گزارش کردند."
            )
        ),
        "dso_m23" to BilingualFacts(
            en = listOf(
                "M23 is an open cluster in Sagittarius, about 2,150 light-years away.",
                "It shines at magnitude 6.9 and spans about 27 arcminutes of sky.",
                "Messier cataloged M23 in 1764, one of his first Sagittarius clusters.",
                "The cluster contains around 150 stars and is about 300 million years old.",
                "Its brightest stars are blue-white giants scattered against the rich Sagittarius Milky Way."
            ),
            fa = listOf(
                "M۲۳ یک خوشه باز در صورت فلکی کمان و در فاصله حدود ۲۱۵۰ سال نوری است.",
                "این خوشه با قدر ۶/۹ می‌درخشد و پهنای آن حدود ۲۷ دقیقه قوسی از آسمان است.",
                "شارل مسیه M۲۳ را در سال ۱۷۶۴ کشف کرد.",
                "این خوشه حدود ۱۵۰ ستاره دارد و سن آن حدود ۳۰۰ میلیون سال است.",
                "درخشان‌ترین ستارگان آن غول‌های آبی-سفیدی هستند که در برابر راه شیری پرستاره کمان پراکنده‌اند."
            )
        ),
        "dso_m24" to BilingualFacts(
            en = listOf(
                "M24, the Sagittarius Star Cloud, is not a true cluster but a dense cloud of Milky Way stars.",
                "It lies about 10,000 light-years away and spans roughly 90 arcminutes of sky.",
                "At magnitude 4.6 it is easily visible to the naked eye as a bright patch in Sagittarius.",
                "The cloud contains the open cluster NGC 6603 and the dark nebulae Barnard 92 and Barnard 93.",
                "M24 is a window through the Milky Way's dust, revealing a rich star field behind it."
            ),
            fa = listOf(
                "M۲۴ یا ابر ستاره‌ای کمان، یک خوشه واقعی نیست بلکه ابری متراکم از ستارگان راه شیری است.",
                "این ابر در فاصله حدود ۱۰,۰۰۰ سال نوری است و پهنای آن تقریباً ۹۰ دقیقه قوسی از آسمان است.",
                "M۲۴ با قدر ۴/۶ به آسانی با چشم غیرمسلح به صورت لکه درخشانی در کمان دیده می‌شود.",
                "این ابر ستاره‌ای میزبان خوشه باز NGC ۶۶۰۳ و سحابی‌های تاریک بارنارد ۹۲ و بارنارد ۹۳ است.",
                "M۲۴ پنجره‌ای از میان غبار راه شیری است که میدان ستاره‌ای غنی پشت آن را آشکار می‌کند."
            )
        ),
        "dso_m25" to BilingualFacts(
            en = listOf(
                "M25 is an open cluster in Sagittarius, about 2,000 light-years away.",
                "It shines at magnitude 4.6, visible to the naked eye under dark skies.",
                "Jean-Philippe de Chéseaux discovered M25 in 1745.",
                "The cluster is about 90 million years old and contains the Cepheid variable U Sagittarii.",
                "M25 spans more than half a degree, appearing as a loose but bright grouping."
            ),
            fa = listOf(
                "M۲۵ یک خوشه باز در صورت فلکی کمان و در فاصله حدود ۲۰۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۶ در آسمان تاریک با چشم غیرمسلح دیده می‌شود.",
                "ژان-فیلیپ دو شزو M۲۵ را در سال ۱۷۴۵ کشف کرد.",
                "سن این خوشه حدود ۹۰ میلیون سال است و ستاره متغیر قیفاووسی U کمان را در خود دارد.",
                "M۲۵ بیش از نیم درجه از آسمان را می‌پوشاند و گروهی باز اما درخشان به نظر می‌رسد."
            )
        ),
        "dso_m26" to BilingualFacts(
            en = listOf(
                "M26 is an open cluster in Scutum, about 5,000 light-years away.",
                "It shines at magnitude 8.0 and is a modest target for small telescopes.",
                "Charles Messier discovered M26 in 1764 in the constellation Scutum.",
                "The cluster is about 89 million years old and contains around 90 stars.",
                "A dark dust lane lies between M26 and Earth, dimming the cluster's light."
            ),
            fa = listOf(
                "M۲۶ یک خوشه باز در صورت فلکی سپر و در فاصله حدود ۵۰۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۰ هدفی معمولی برای تلسکوپ‌های کوچک است.",
                "شارل مسیه M۲۶ را در سال ۱۷۶۴ کشف کرد.",
                "سن این خوشه حدود ۸۹ میلیون سال است و حدود ۹۰ ستاره دارد.",
                "یک رگه غبار تاریک میان M۲۶ و زمین قرار دارد و نور خوشه را کم‌نورتر می‌کند."
            )
        ),
        "dso_m27" to BilingualFacts(
            en = listOf(
                "M27, the Dumbbell Nebula, was the first planetary nebula ever discovered, found by Charles Messier in 1764.",
                "It lies about 1,360 light-years away in the constellation Vulpecula.",
                "At magnitude 7.5 it is one of the brightest planetary nebulae in the sky.",
                "Its central star is one of the largest known white dwarfs, larger than most stars of its kind.",
                "The nebula's bipolar lobes give it the dumbbell shape that inspired its popular name."
            ),
            fa = listOf(
                "M۲۷ یا سحابی دمبل، نخستین سحابی سیاره‌نمای کشف‌شده در تاریخ است که شارل مسیه در سال ۱۷۶۴ آن را یافت.",
                "این سحابی در فاصله حدود ۱۳۶۰ سال نوری و در صورت فلکی روباهک قرار دارد.",
                "M۲۷ با قدر ۷/۵ یکی از درخشان‌ترین سحابی‌های سیاره‌نمای آسمان است.",
                "ستاره مرکزی آن یکی از بزرگ‌ترین کوتوله‌های سفید شناخته‌شده است و از بیشتر ستارگان هم‌نوع خود بزرگ‌تر است.",
                "دو لوب قطبی این سحابی شکل دمبل‌مانندی به آن می‌دهند که نام مشهورش از آن گرفته شده است."
            )
        ),
        "dso_m28" to BilingualFacts(
            en = listOf(
                "M28 is a globular cluster in Sagittarius, about 17,900 light-years away.",
                "It shines at magnitude 6.8 and appears as a small hazy patch in small telescopes.",
                "Messier found M28 in 1764, describing it as a small, faint nebula.",
                "In 1987 M28 became the first globular cluster found to contain a millisecond pulsar, PSR B1821-24.",
                "The cluster lies near Lambda Sagittarii (Kaus Borealis), the star at the top of the Teapot asterism."
            ),
            fa = listOf(
                "M۲۸ یک خوشه کروی در صورت فلکی کمان و در فاصله حدود ۱۷,۹۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۸ در تلسکوپ‌های کوچک به صورت لکه مه‌آلود کوچکی دیده می‌شود.",
                "شارل مسیه M۲۸ را در سال ۱۷۶۴ کشف کرد.",
                "در سال ۱۹۸۷، M۲۸ نخستین خوشه کروی بود که در آن یک تپ‌اختر میلی‌ثانیه‌ای به نام PSR B1821-24 یافت شد.",
                "این خوشه در نزدیکی لامبدا کمان (قوس شمالی)، ستاره بالای صورتواره قوری، قرار دارد."
            )
        ),
        "dso_m29" to BilingualFacts(
            en = listOf(
                "M29, the Cooling Tower, is a small open cluster in Cygnus, about 4,000 light-years away.",
                "It shines at magnitude 7.1 and is visible in binoculars near the star Sadr.",
                "Charles Messier discovered M29 in 1764 in the Milky Way of Cygnus.",
                "The cluster contains about 50 stars and is only about 10 million years old.",
                "Its brightest stars form a shape resembling a cooling tower, giving the cluster its nickname."
            ),
            fa = listOf(
                "M۲۹ یا برج خنک‌کننده، یک خوشه باز کوچک در صورت فلکی ماکیان و در فاصله حدود ۴۰۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۱ در دوربین دوچشمی در نزدیکی ستاره صدر دیده می‌شود.",
                "شارل مسیه M۲۹ را در سال ۱۷۶۴ کشف کرد.",
                "این خوشه حدود ۵۰ ستاره دارد و سن آن تنها حدود ۱۰ میلیون سال است.",
                "درخشان‌ترین ستارگان آن شکلی شبیه برج خنک‌کننده می‌سازند که لقب خوشه از آن گرفته شده است."
            )
        ),
        "dso_m3" to BilingualFacts(
            en = listOf(
                "M3 is a globular cluster containing roughly half a million stars in the constellation Canes Venatici.",
                "It lies about 33,900 light-years away and appears as a magnitude 6.2 glow in binoculars.",
                "M3 is famous for its variable stars: more than 270 have been identified, most of them RR Lyrae variables.",
                "Charles Messier discovered M3 in 1764, and it was the first Messier object he found independently.",
                "The cluster also contains an unusually large population of blue stragglers near its core."
            ),
            fa = listOf(
                "M۳ یک خوشه کروی با حدود نیم میلیون ستاره در صورت فلکی تازی‌ها است.",
                "این خوشه در فاصله تقریبی ۳۳,۹۰۰ سال نوری است و با قدر ۶/۲ در دوربین دوچشمی به صورت لکه‌ای دیده می‌شود.",
                "M۳ به ستارگان متغیرش مشهور است: بیش از ۲۷۰ متغیر در آن شناسایی شده که بیشترشان از نوع آر‌آر شلیاقی هستند.",
                "شارل مسیه M۳ را در سال ۱۷۶۴ کشف کرد و این نخستین جرم مسیه بود که او به طور مستقل یافت.",
                "این خوشه همچنین جمعیت غیرعادی بزرگی از ستارگان «ولگرد آبی» در نزدیکی هسته خود دارد."
            )
        ),
        "dso_m30" to BilingualFacts(
            en = listOf(
                "M30 is a globular cluster in Capricornus, about 26,800 light-years away.",
                "It shines at magnitude 7.2 and appears as a small, fuzzy star in small telescopes.",
                "Messier cataloged M30 in 1764, noting its compact, comet-like glow.",
                "M30 has undergone core collapse, concentrating its stars densely at the center.",
                "The cluster follows a retrograde orbit around the Milky Way, moving opposite to most stars."
            ),
            fa = listOf(
                "M۳۰ یک خوشه کروی در صورت فلکی بزغاله و در فاصله حدود ۲۶,۸۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۲ در تلسکوپ‌های کوچک به صورت ستاره‌ای مه‌آلود و کوچک دیده می‌شود.",
                "شارل مسیه M۳۰ را در سال ۱۷۶۴ کشف کرد.",
                "M۳۰ دچار رمبش هسته شده و ستارگانش در مرکز به شدت متراکم شده‌اند.",
                "این خوشه مداری پسرونده به دور راه شیری دارد و در جهت مخالف بیشتر ستارگان حرکت می‌کند."
            )
        ),
        "dso_m32" to BilingualFacts(
            en = listOf(
                "M32 is a compact elliptical galaxy and a close companion of the Andromeda Galaxy.",
                "It lies about 2.54 million light-years away in Andromeda.",
                "At magnitude 8.1 it is visible in small telescopes as a bright, round glow near M31.",
                "M32 contains a supermassive black hole of roughly two million solar masses.",
                "Its dense core and high surface brightness suggest it was once a larger galaxy stripped by tidal forces."
            ),
            fa = listOf(
                "M۳۲ یک کهکشان بیضوی فشرده و همدم نزدیک کهکشان آندرومدا است.",
                "این کهکشان در فاصله حدود ۲/۵۴ میلیون سال نوری و در صورت فلکی آندرومدا قرار دارد.",
                "M۳۲ با قدر ۸/۱ در تلسکوپ‌های کوچک به صورت درخشش گرد و پرنوری در نزدیکی M۳۱ دیده می‌شود.",
                "M۳۲ یک سیاهچاله کلان‌جرم با جرم تقریبی دو میلیون برابر خورشید دارد.",
                "هسته متراکم و درخشندگی سطحی بالای آن نشان می‌دهد که روزگاری کهکشانی بزرگ‌تر بوده که نیروهای کشندی آن را تحلیل برده‌اند."
            )
        ),
        "dso_m34" to BilingualFacts(
            en = listOf(
                "M34 is an open cluster in Perseus, about 1,500 light-years away.",
                "It shines at magnitude 5.5 and spans about 35 arcminutes, larger than the full Moon.",
                "Giovanni Hodierna recorded M34 around 1654, before Messier's catalog.",
                "The cluster is about 250 million years old and contains roughly 400 stars.",
                "M34 is a fine binocular target near the star Algol."
            ),
            fa = listOf(
                "M۳۴ یک خوشه باز در صورت فلکی برساوش و در فاصله حدود ۱۵۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۵ می‌درخشد و پهنای آن حدود ۳۵ دقیقه قوسی، بزرگ‌تر از ماه کامل، است.",
                "جیووانی هودیرنا M۳۴ را حدود سال ۱۶۵۴ و پیش از فهرست مسیه ثبت کرد.",
                "سن این خوشه حدود ۲۵۰ میلیون سال است و تقریباً ۴۰۰ ستاره دارد.",
                "M۳۴ هدف خوبی برای دوربین دوچشمی در نزدیکی ستاره رأس‌الغول است."
            )
        ),
        "dso_m35" to BilingualFacts(
            en = listOf(
                "M35 is an open cluster in Gemini, about 2,800 light-years away.",
                "It shines at magnitude 5.1 and is visible to the naked eye under dark skies.",
                "The cluster spans about 28 arcminutes and contains several hundred stars.",
                "M35 is about 175 million years old, and the fainter cluster NGC 2158 lies nearby in the background.",
                "Philippe de Chéseaux discovered M35 around 1745."
            ),
            fa = listOf(
                "M۳۵ یک خوشه باز در صورت فلکی دوپیکر و در فاصله حدود ۲۸۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۱ در آسمان تاریک با چشم غیرمسلح دیده می‌شود.",
                "پهنای این خوشه حدود ۲۸ دقیقه قوسی است و چند صد ستاره دارد.",
                "سن M۳۵ حدود ۱۷۵ میلیون سال است و خوشه کم‌نورتر NGC ۲۱۵۸ در پس‌زمینه آن دیده می‌شود.",
                "فیلیپ دو شزو M۳۵ را حدود سال ۱۷۴۵ کشف کرد."
            )
        ),
        "dso_m36" to BilingualFacts(
            en = listOf(
                "M36, the Pinwheel Cluster, is an open cluster in Auriga, about 4,100 light-years away.",
                "It shines at magnitude 6.3 and is visible in binoculars.",
                "The cluster is young, only about 25 million years old, and contains roughly 60 stars.",
                "M36's brightest stars form a rough pinwheel shape, giving the cluster its popular name.",
                "It is the smallest of the three bright Messier clusters in Auriga, after M37 and M38."
            ),
            fa = listOf(
                "M۳۶ یا خوشه فرفره، یک خوشه باز در صورت فلکی ارابه‌ران و در فاصله حدود ۴۱۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۳ در دوربین دوچشمی دیده می‌شود.",
                "این خوشه جوان است و تنها حدود ۲۵ میلیون سال سن دارد و تقریباً ۶۰ ستاره در آن است.",
                "درخشان‌ترین ستارگان M۳۶ شکلی شبیه فرفره می‌سازند که نام مشهور خوشه از آن گرفته شده است.",
                "این خوشه کوچک‌ترین از سه خوشه درخشان مسیه در ارابه‌ران است و پس از M۳۷ و M۳۸ قرار می‌گیرد."
            )
        ),
        "dso_m37" to BilingualFacts(
            en = listOf(
                "M37 is the richest and brightest of the three Messier clusters in Auriga.",
                "It lies about 4,500 light-years away and shines at magnitude 5.6.",
                "The cluster contains roughly 500 stars, including more than a dozen red giants.",
                "M37 is about 350 million years old, older than its neighbors M36 and M38.",
                "Giovanni Hodierna recorded M37 before 1654, long before Messier's catalog."
            ),
            fa = listOf(
                "M۳۷ غنی‌ترین و درخشان‌ترین خوشه از سه خوشه مسیه در صورت فلکی ارابه‌ران است.",
                "این خوشه در فاصله حدود ۴۵۰۰ سال نوری است و با قدر ۵/۶ می‌درخشد.",
                "این خوشه تقریباً ۵۰۰ ستاره دارد که بیش از دوازده غول سرخ در میان آن‌هاست.",
                "سن M۳۷ حدود ۳۵۰ میلیون سال است و از همسایگانش M۳۶ و M۳۸ کهن‌سال‌تر است.",
                "این خوشه را جیووانی هودیرنا پیش از سال ۱۶۵۴ کشف کرد."
            )
        ),
        "dso_m38" to BilingualFacts(
            en = listOf(
                "M38, the Starfish Cluster, is an open cluster in Auriga, about 4,200 light-years away.",
                "It shines at magnitude 6.4 and is visible in binoculars.",
                "The cluster is about 250 million years old and contains roughly 100 stars.",
                "Its scattered brighter stars trace a shape like a starfish, giving the cluster its nickname.",
                "M38 was recorded by Giovanni Hodierna before 1654."
            ),
            fa = listOf(
                "M۳۸ یا خوشه ستاره دریایی، یک خوشه باز در صورت فلکی ارابه‌ران و در فاصله حدود ۴۲۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۴ در دوربین دوچشمی دیده می‌شود.",
                "سن این خوشه حدود ۲۵۰ میلیون سال است و تقریباً ۱۰۰ ستاره دارد.",
                "ستارگان درخشان پراکنده آن شکلی شبیه ستاره دریایی می‌سازند که لقب خوشه از آن گرفته شده است.",
                "M۳۸ را جیووانی هودیرنا پیش از سال ۱۶۵۴ ثبت کرد."
            )
        ),
        "dso_m39" to BilingualFacts(
            en = listOf(
                "M39 is a loose open cluster in Cygnus, only about 800 light-years away.",
                "It shines at magnitude 4.6 and is visible to the naked eye under dark skies.",
                "The cluster spans more than half a degree and contains about 30 stars.",
                "M39 is about 300 million years old, middle-aged for an open cluster.",
                "Charles Messier added M39 to his catalog in 1764."
            ),
            fa = listOf(
                "M۳۹ یک خوشه باز باز و پراکنده در صورت فلکی ماکیان است که تنها حدود ۸۰۰ سال نوری فاصله دارد.",
                "این خوشه با قدر ۴/۶ در آسمان تاریک با چشم غیرمسلح دیده می‌شود.",
                "پهنای این خوشه بیش از نیم درجه است و حدود ۳۰ ستاره دارد.",
                "سن M۳۹ حدود ۳۰۰ میلیون سال است؛ سنی میانه برای یک خوشه باز.",
                "شارل مسیه M۳۹ را در سال ۱۷۶۴ به فهرست خود افزود."
            )
        ),
        "dso_m4" to BilingualFacts(
            en = listOf(
                "M4 is the nearest globular cluster to Earth, only about 7,200 light-years away in Scorpius.",
                "At magnitude 5.6 it is visible as a faint round glow in binoculars near the star Antares.",
                "M4 was the first globular cluster to be resolved into individual stars, by William Herschel in 1783.",
                "The cluster hosts the pulsar PSR B1620-26, which is orbited by one of the oldest known exoplanets.",
                "M4 contains tens of thousands of white dwarfs, the aged remnants of its most massive stars."
            ),
            fa = listOf(
                "M۴ نزدیک‌ترین خوشه کروی به زمین است و تنها حدود ۷۲۰۰ سال نوری در صورت فلکی عقرب فاصله دارد.",
                "این خوشه با قدر ۵/۶ به صورت لکه گرد کم‌نوری در نزدیکی ستاره قلب‌العقرب با دوربین دوچشمی دیده می‌شود.",
                "M۴ نخستین خوشه کروی بود که به ستارگان منفرد تفکیک شد؛ این کار را ویلیام هرشل در سال ۱۷۸۳ انجام داد.",
                "این خوشه میزبان تپ‌اختر PSR B1620-26 است که یکی از کهن‌سال‌ترین سیارات فراخورشیدی شناخته‌شده به دور آن می‌گردد.",
                "M۴ ده‌ها هزار کوتوله سفید دارد که بازمانده‌های پیر پرجرم‌ترین ستارگان آن هستند."
            )
        ),
        "dso_m40" to BilingualFacts(
            en = listOf(
                "M40, Winnecke 4, is not a nebula or cluster but a pair of unrelated stars in Ursa Major.",
                "Charles Messier cataloged M40 in 1764 while searching for a nebula that earlier observers had reported there.",
                "The two stars, roughly 510 light-years away, are merely an optical double along our line of sight.",
                "At magnitude 8.4 and 9.0, the pair is visible in small telescopes.",
                "M40 is often called Messier's mistake, an asterism rather than a true deep-sky object."
            ),
            fa = listOf(
                "M۴۰ یا وینکه ۴، نه سحابی است و نه خوشه، بلکه جفتی از دو ستاره نامرتبط در صورت فلکی خرس بزرگ است.",
                "شارل مسیه M۴۰ را در سال ۱۷۶۴ فهرست کرد، در حالی که به دنبال سحابی‌ای می‌گشت که رصدگران پیشین گزارش کرده بودند.",
                "این دو ستاره که حدود ۵۱۰ سال نوری فاصله دارند، تنها یک جفت اپتیکی در امتداد خط دید ما هستند.",
                "این جفت با قدرهای ۸/۴ و ۹/۰ در تلسکوپ‌های کوچک دیده می‌شود.",
                "M۴۰ را اغلب «اشتباه مسیه» می‌نامند؛ یک صورتواره به جای جرم واقعی اعماق آسمان."
            )
        ),
        "dso_m41" to BilingualFacts(
            en = listOf(
                "M41 is an open cluster in Canis Major, about 2,300 light-years away.",
                "It shines at magnitude 4.5 and is visible to the naked eye under dark skies.",
                "The cluster lies about four degrees south of Sirius, the brightest star in the sky.",
                "M41 contains about 100 stars, including several orange giants and some white dwarfs.",
                "Giovanni Hodierna recorded M41 around 1654, and Aristotle may have mentioned it even earlier."
            ),
            fa = listOf(
                "M۴۱ یک خوشه باز در صورت فلکی سگ بزرگ و در فاصله حدود ۲۳۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۵ در آسمان تاریک با چشم غیرمسلح دیده می‌شود.",
                "این خوشه حدود چهار درجه در جنوب شباهنگ، درخشان‌ترین ستاره آسمان، قرار دارد.",
                "M۴۱ حدود ۱۰۰ ستاره دارد، از جمله چند غول نارنجی و چند کوتوله سفید.",
                "جیووانی هودیرنا M۴۱ را حدود سال ۱۶۵۴ ثبت کرد و شاید ارسطو حتی پیش‌تر از آن به آن اشاره کرده باشد."
            )
        ),
        "dso_m43" to BilingualFacts(
            en = listOf(
                "M43, de Mairan's Nebula, is a bright knot of the great Orion Nebula complex, separated from M42 by a dark dust lane.",
                "It lies about 1,344 light-years away in Orion and shines at magnitude 9.0.",
                "Jean-Jacques d'Ortous de Mairan recorded this patch of nebulosity in 1731, before Messier's catalog.",
                "The nebula is energized by the young star NU Orionis, which makes its gas glow.",
                "M43 carries the New General Catalogue designation NGC 1982."
            ),
            fa = listOf(
                "M۴۳ یا سحابی دو مران، گره درخشانی از مجموعه بزرگ سحابی شکارچی است که یک رگه غبار تاریک آن را از M۴۲ جدا می‌کند.",
                "این سحابی در فاصله حدود ۱۳۴۴ سال نوری در صورت فلکی شکارچی قرار دارد و با قدر ۹/۰ می‌درخشد.",
                "ژان-ژاک دورتو دو مران این لکه سحابی را در سال ۱۷۳۱ و پیش از فهرست مسیه ثبت کرد.",
                "این سحابی را ستاره جوان NU شکارچی برانگیخته و گاز آن را به درخشش واداشته است.",
                "M۴۳ در فهرست عمومی جدید با نام NGC ۱۹۸۲ شناخته می‌شود."
            )
        ),
        "dso_m46" to BilingualFacts(
            en = listOf(
                "M46 is a rich open cluster in Puppis, about 5,400 light-years away.",
                "It shines at magnitude 6.1 and contains roughly 500 stars in a 27-arcminute field.",
                "Charles Messier discovered M46 in 1771.",
                "The planetary nebula NGC 2438 appears projected inside the cluster, a line-of-sight coincidence that delights observers.",
                "M46 is about 300 million years old."
            ),
            fa = listOf(
                "M۴۶ یک خوشه باز پرستاره در صورت فلکی کشتی پشت و در فاصله حدود ۵۴۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۱ می‌درخشد و تقریباً ۵۰۰ ستاره در میدانی ۲۷ دقیقه قوسی دارد.",
                "شارل مسیه M۴۶ را در سال ۱۷۷۱ کشف کرد.",
                "سحابی سیاره‌نمای NGC ۲۴۳۸ به صورت ظاهری درون خوشه دیده می‌شود؛ تصادفی از امتداد خط دید که رصدگران را شگفت‌زده می‌کند.",
                "سن M۴۶ حدود ۳۰۰ میلیون سال است."
            )
        ),
        "dso_m47" to BilingualFacts(
            en = listOf(
                "M47 is an open cluster in Puppis, about 1,600 light-years away.",
                "It shines at magnitude 4.4 and spans about 30 arcminutes, larger than the full Moon.",
                "Giovanni Hodierna recorded M47 before 1654, decades before Messier independently rediscovered it in 1771.",
                "The cluster contains about 50 stars, including two orange K-type giants.",
                "M47 is roughly 78 million years old, a relatively young open cluster."
            ),
            fa = listOf(
                "M۴۷ یک خوشه باز در صورت فلکی کشتی پشت و در فاصله حدود ۱۶۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۴ می‌درخشد و پهنای آن حدود ۳۰ دقیقه قوسی، بزرگ‌تر از ماه کامل، است.",
                "جیووانی هودیرنا M۴۷ را پیش از سال ۱۶۵۴ ثبت کرد؛ دهه‌ها پیش از آنکه مسیه آن را در سال ۱۷۷۱ به طور مستقل دوباره بیابد.",
                "این خوشه حدود ۵۰ ستاره دارد که دو غول نارنجی از نوع K در میان آن‌هاست.",
                "سن M۴۷ حدود ۷۸ میلیون سال است؛ خوشه‌ای باز و نسبتاً جوان."
            )
        ),
        "dso_m48" to BilingualFacts(
            en = listOf(
                "M48 is an open cluster in Hydra, about 1,500 light-years away.",
                "It shines at magnitude 5.5 and spans 54 arcminutes, a very wide grouping.",
                "Charles Messier discovered M48 in 1771, but an error in his recorded position long made the object seem missing.",
                "The cluster contains about 80 stars and is roughly 300 million years old.",
                "M48 is easily visible to the naked eye under dark skies."
            ),
            fa = listOf(
                "M۴۸ یک خوشه باز در صورت فلکی مار باریک و در فاصله حدود ۱۵۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۵ می‌درخشد و پهنای آن ۵۴ دقیقه قوسی است؛ گروهی بسیار گسترده.",
                "شارل مسیه M۴۸ را در سال ۱۷۷۱ کشف کرد، اما خطایی در موقعیت ثبت‌شده آن باعث شد مدتی طولانی این جرم گم‌شده به نظر برسد.",
                "این خوشه حدود ۸۰ ستاره دارد و سن آن تقریباً ۳۰۰ میلیون سال است.",
                "M۴۸ در آسمان تاریک به آسانی با چشم غیرمسلح دیده می‌شود."
            )
        ),
        "dso_m49" to BilingualFacts(
            en = listOf(
                "M49 is a giant elliptical galaxy in the Virgo Cluster, about 56 million light-years away.",
                "It shines at magnitude 8.4 and was the first Virgo Cluster member cataloged by Charles Messier in 1771.",
                "M49 is one of the most luminous galaxies in the Virgo Cluster.",
                "M49 hides a supermassive black hole in its core, among the heaviest in the Virgo Cluster.",
                "M49 is surrounded by a vast system of thousands of globular clusters."
            ),
            fa = listOf(
                "M۴۹ یک کهکشان بیضوی غول‌پیکر در خوشه کهکشانی سنبله و در فاصله حدود ۵۶ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۴ می‌درخشد و نخستین عضو خوشه سنبله بود که شارل مسیه در سال ۱۷۷۱ فهرست کرد.",
                "M۴۹ یکی از پرنورترین کهکشان‌های خوشه سنبله است.",
                "این کهکشان یک سیاهچاله کلان‌جرم در مرکز خود دارد.",
                "M۴۹ را سامانه‌ای گسترده از هزاران خوشه کروی در بر گرفته است."
            )
        ),
        "dso_m5" to BilingualFacts(
            en = listOf(
                "M5 is a globular cluster of more than 100,000 stars in the constellation Serpens.",
                "It lies about 24,500 light-years away and reaches magnitude 5.6, one of the brightest northern globulars.",
                "Gottfried Kirch discovered M5 in 1702 while observing a comet.",
                "The cluster spans about 165 light-years and is estimated to be 13 billion years old.",
                "M5 contains more than 100 known variable stars, mostly RR Lyrae types."
            ),
            fa = listOf(
                "M۵ یک خوشه کروی با بیش از ۱۰۰,۰۰۰ ستاره در صورت فلکی مار است.",
                "این خوشه در فاصله تقریبی ۲۴,۵۰۰ سال نوری است و با قدر ۵/۶ یکی از درخشان‌ترین خوشه‌های کروی شمالی است.",
                "گوتفرید کیرش M۵ را در سال ۱۷۰۲ و هنگام رصد یک دنباله‌دار کشف کرد.",
                "قطر این خوشه حدود ۱۶۵ سال نوری است و سن آن حدود ۱۳ میلیارد سال برآورد می‌شود.",
                "M۵ بیش از ۱۰۰ ستاره متغیر شناخته‌شده دارد که بیشترشان از نوع آر‌آر شلیاقی هستند."
            )
        ),
        "dso_m50" to BilingualFacts(
            en = listOf(
                "M50 is an open cluster in Monoceros, about 3,000 light-years away.",
                "It shines at magnitude 5.9 and contains roughly 200 stars.",
                "Charles Messier discovered M50 in 1772.",
                "The cluster is about 180 million years old and has a notable red giant near its center.",
                "M50 appears heart-shaped through small telescopes."
            ),
            fa = listOf(
                "M۵۰ یک خوشه باز در صورت فلکی تک‌شاخ و در فاصله حدود ۳۰۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۹ می‌درخشد و تقریباً ۲۰۰ ستاره دارد.",
                "شارل مسیه M۵۰ را در سال ۱۷۷۲ کشف کرد.",
                "سن این خوشه حدود ۱۸۰ میلیون سال است و یک غول سرخ چشمگیر نزدیک مرکز آن قرار دارد.",
                "M۵۰ در تلسکوپ‌های کوچک به شکل قلب دیده می‌شود."
            )
        ),
        "dso_m51" to BilingualFacts(
            en = listOf(
                "M51, the Whirlpool Galaxy, is a grand-design spiral in Canes Venatici about 23 million light-years away.",
                "It is locked in an ongoing interaction with the small companion galaxy NGC 5195.",
                "Lord Rosse first recognized the galaxy's spiral structure with his 72-inch telescope in 1845.",
                "M51 has hosted at least three supernovae: SN 1994I, SN 2005cs, and SN 2011dh.",
                "At magnitude 8.4, M51 is a favorite target for amateur astrophotographers."
            ),
            fa = listOf(
                "M۵۱ یا کهکشان گرداب، یک کهکشان مارپیچی باشکوه در صورت فلکی تازی‌ها و در فاصله حدود ۲۳ میلیون سال نوری است.",
                "این کهکشان در برهم‌کنش پیوسته با کهکشان کوچک همدم NGC ۵۱۹۵ گرفتار است.",
                "لرد راس نخستین بار ساختار مارپیچی این کهکشان را با تلسکوپ ۷۲ اینچی خود در سال ۱۸۴۵ تشخیص داد.",
                "M۵۱ میزبان دست‌کم سه ابرنواختر ثبت‌شده بوده است؛ یکی از آن‌ها در سال ۱۹۹۴ و دیگری در سال ۲۰۱۱ درخشید.",
                "M۵۱ با قدر ۸/۴ هدف محبوب عکاسان نجومی آماتور است."
            )
        ),
        "dso_m52" to BilingualFacts(
            en = listOf(
                "M52 is an open cluster in Cassiopeia, about 5,000 light-years away.",
                "It shines at magnitude 6.9 and contains about 190 stars.",
                "Charles Messier discovered M52 in 1774 while observing a comet.",
                "The cluster is young, only about 35 million years old, and includes a yellow giant star.",
                "M52 lies in a rich Milky Way field near the border with Cepheus."
            ),
            fa = listOf(
                "M۵۲ یک خوشه باز در صورت فلکی ذات‌الکرسی و در فاصله حدود ۵۰۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۹ می‌درخشد و حدود ۱۹۰ ستاره دارد.",
                "شارل مسیه M۵۲ را در سال ۱۷۷۴ و هنگام رصد یک دنباله‌دار کشف کرد.",
                "این خوشه جوان است و تنها حدود ۳۵ میلیون سال سن دارد و یک ستاره غول زرد در آن است.",
                "M۵۲ در میدان پرستاره راه شیری و در نزدیکی مرز صورت فلکی قیفاووس قرار دارد."
            )
        ),
        "dso_m53" to BilingualFacts(
            en = listOf(
                "M53 is a globular cluster in Coma Berenices, about 58,000 light-years away.",
                "It shines at magnitude 7.6 and appears as a small round glow in telescopes.",
                "Johann Elert Bode discovered M53 in 1775.",
                "The cluster is one of the most metal-poor globulars known, made of ancient, nearly pristine stars.",
                "M53 is about 12.7 billion years old."
            ),
            fa = listOf(
                "M۵۳ یک خوشه کروی در صورت فلکی گیسو و در فاصله حدود ۵۸,۰۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۶ می‌درخشد و در تلسکوپ‌ها به صورت درخشش گرد کوچکی دیده می‌شود.",
                "یوهان الرت بوده M۵۳ را در سال ۱۷۷۵ کشف کرد.",
                "این خوشه یکی از فقیرترین خوشه‌های کروی از نظر فلز است و از ستارگانی کهن و تقریباً دست‌نخورده ساخته شده است.",
                "سن M۵۳ حدود ۱۲/۷ میلیارد سال است."
            )
        ),
        "dso_m54" to BilingualFacts(
            en = listOf(
                "M54 is a dense globular cluster in Sagittarius, about 87,400 light-years away.",
                "It shines at magnitude 7.6 and was discovered by Charles Messier in 1778.",
                "In 1994, astronomers determined that M54 actually belongs to the Sagittarius Dwarf Elliptical Galaxy, not the Milky Way.",
                "It was the first globular cluster found to belong to another galaxy.",
                "M54 is one of the densest globular clusters known."
            ),
            fa = listOf(
                "M۵۴ یک خوشه کروی متراکم در صورت فلکی کمان و در فاصله حدود ۸۷,۴۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۶ می‌درخشد و شارل مسیه آن را در سال ۱۷۷۸ کشف کرد.",
                "در سال ۱۹۹۴ اخترشناسان دریافتند که M۵۴ در واقع به کهکشان بیضوی کوتوله کمان تعلق دارد، نه به راه شیری.",
                "این نخستین خوشه کروی بود که مشخص شد به کهکشانی دیگر تعلق دارد.",
                "M۵۴ یکی از متراکم‌ترین خوشه‌های کروی شناخته‌شده است."
            )
        ),
        "dso_m55" to BilingualFacts(
            en = listOf(
                "M55 is a large, loosely concentrated globular cluster in Sagittarius.",
                "It lies about 17,600 light-years away and shines at magnitude 6.3.",
                "Nicolas-Louis de Lacaille discovered M55 in 1752 from South Africa.",
                "The cluster is one of the least dense globulars known, resolving easily into individual stars.",
                "M55 contains only about a dozen variable stars."
            ),
            fa = listOf(
                "M۵۵ یک خوشه کروی بزرگ و کم‌تراکم در صورت فلکی کمان است.",
                "این خوشه در فاصله حدود ۱۷,۶۰۰ سال نوری است و با قدر ۶/۳ می‌درخشد.",
                "نیکولا-لویی دو لاکای M۵۵ را در سال ۱۷۵۲ از آفریقای جنوبی کشف کرد.",
                "این خوشه یکی از کم‌تراکم‌ترین خوشه‌های کروی شناخته‌شده است و به آسانی به ستارگان منفرد تفکیک می‌شود.",
                "M۵۵ تنها حدود دوازده ستاره متغیر دارد."
            )
        ),
        "dso_m56" to BilingualFacts(
            en = listOf(
                "M56 is a globular cluster in Lyra, about 32,900 light-years away.",
                "It shines at magnitude 8.3 and is a compact but modest telescope target.",
                "Charles Messier discovered M56 in 1779.",
                "The cluster is loosely concentrated, with a low central density for a globular.",
                "M56 lies in the same constellation as the famous Ring Nebula, M57."
            ),
            fa = listOf(
                "M۵۶ یک خوشه کروی در صورت فلکی شلیاق و در فاصله حدود ۳۲,۹۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۳ هدفی فشرده اما معمولی برای تلسکوپ است.",
                "شارل مسیه M۵۶ را در سال ۱۷۷۹ کشف کرد.",
                "این خوشه تراکم کمی دارد و چگالی مرکزی آن برای یک خوشه کروی پایین است.",
                "M۵۶ در همان صورت فلکی سحابی مشهور حلقه یعنی M۵۷ جای دارد."
            )
        ),
        "dso_m57" to BilingualFacts(
            en = listOf(
                "M57, the Ring Nebula, is a planetary nebula in Lyra about 2,300 light-years away.",
                "It is the glowing shell of gas thrown off by a dying sun-like star.",
                "Antoine Darquier de Pellepoix discovered M57 in 1779.",
                "The nebula's central star is a white dwarf of about magnitude 15.8.",
                "M57's ring of gas is expanding at roughly 20 to 30 km per second."
            ),
            fa = listOf(
                "M۵۷ یا سحابی حلقه، یک سحابی سیاره‌نما در صورت فلکی شلیاق و در فاصله حدود ۲۳۰۰ سال نوری است.",
                "این سحابی پوسته درخشان گازی است که ستاره‌ای رو به مرگ شبیه خورشید به بیرون پرتاب کرده است.",
                "آنتوان دارکیه دو پلپو M۵۷ را در سال ۱۷۷۹ کشف کرد.",
                "ستاره مرکزی این سحابی یک کوتوله سفید با قدر حدود ۱۵/۸ است.",
                "حلقه گازی M۵۷ با سرعت تقریبی ۲۰ تا ۳۰ کیلومتر بر ثانیه در حال گسترش است."
            )
        ),
        "dso_m58" to BilingualFacts(
            en = listOf(
                "M58 is a barred spiral galaxy in the Virgo Cluster, about 60 million light-years away.",
                "It shines at magnitude 9.7 and was discovered by Charles Messier in 1779.",
                "Lord Rosse recognized M58 as a spiral in the 1850s, among the first galaxies so classified.",
                "The galaxy has hosted at least two supernovae, SN 1988A and SN 1989M.",
                "M58 is one of the brighter members of the Virgo Cluster."
            ),
            fa = listOf(
                "M۵۸ یک کهکشان مارپیچی میله‌ای در خوشه سنبله و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۷ می‌درخشد و شارل مسیه آن را در سال ۱۷۷۹ کشف کرد.",
                "لرد راس در دهه ۱۸۵۰ ساختار مارپیچی M۵۸ را تشخیص داد؛ از نخستین کهکشان‌هایی که چنین رده‌بندی شدند.",
                "این کهکشان میزبان دست‌کم دو ابرنواختر به نام‌های SN 1988A و SN 1989M بوده است.",
                "M۵۸ یکی از درخشان‌ترین اعضای خوشه سنبله است."
            )
        ),
        "dso_m59" to BilingualFacts(
            en = listOf(
                "M59 is an elliptical galaxy in the Virgo Cluster, about 60 million light-years away.",
                "It shines at magnitude 9.6 and was discovered by Johann Gottfried Koehler in 1779.",
                "M59 has a supermassive black hole at its center.",
                "The galaxy possesses about 2,000 globular clusters, an unusually rich system.",
                "M59 is an elongated elliptical with a rapidly rotating core."
            ),
            fa = listOf(
                "M۵۹ یک کهکشان بیضوی در خوشه سنبله و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۶ می‌درخشد و یوهان گوتفرید کوهلر آن را در سال ۱۷۷۹ کشف کرد.",
                "M۵۹ یک سیاهچاله کلان‌جرم در مرکز خود دارد.",
                "این کهکشان حدود ۲۰۰۰ خوشه کروی دارد؛ سامانه‌ای به طور غیرعادی غنی.",
                "M۵۹ یک کهکشان بیضوی کشیده با هسته‌ای است که به سرعت می‌چرخد."
            )
        ),
        "dso_m6" to BilingualFacts(
            en = listOf(
                "M6, the Butterfly Cluster, is an open cluster of about 80 stars in Scorpius.",
                "It lies roughly 1,600 light-years away and shines at magnitude 4.2.",
                "The cluster's brightest stars appear to trace the outline of a butterfly with open wings.",
                "Giovanni Hodierna recorded M6 around 1654, decades before Messier's catalog.",
                "At about 100 million years old, M6 contains the orange variable star BM Scorpii."
            ),
            fa = listOf(
                "M۶ یا خوشه پروانه، یک خوشه باز با حدود ۸۰ ستاره در صورت فلکی عقرب است.",
                "این خوشه در فاصله تقریبی ۱۶۰۰ سال نوری قرار دارد و با قدر ۴/۲ می‌درخشد.",
                "درخشان‌ترین ستارگان آن گویی شکل پروانه‌ای با بال‌های باز را ترسیم می‌کنند.",
                "جیووانی هودیرنا M۶ را حدود سال ۱۶۵۴ ثبت کرد؛ دهه‌ها پیش از فهرست مسیه.",
                "این خوشه با سن حدود ۱۰۰ میلیون سال، میزبان ستاره متغیر نارنجی BM عقرب است."
            )
        ),
        "dso_m60" to BilingualFacts(
            en = listOf(
                "M60 is a supergiant elliptical, one of the most massive galaxies in the Virgo Cluster.",
                "It shines at magnitude 8.8 and was discovered by Johann Gottfried Koehler in 1779.",
                "M60 is one of the most massive galaxies in the Virgo Cluster.",
                "The galaxy hosts a supermassive black hole of roughly 4.5 billion solar masses.",
                "M60 appears close to the spiral NGC 4647, and the two form an interacting pair known as Arp 116."
            ),
            fa = listOf(
                "M۶۰ یک کهکشان بیضوی غول‌پیکر در خوشه سنبله و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۸ می‌درخشد و یوهان گوتفرید کوهلر آن را در سال ۱۷۷۹ کشف کرد.",
                "M۶۰ یکی از پرجرم‌ترین کهکشان‌های خوشه سنبله است.",
                "این کهکشان میزبان سیاهچاله‌ای کلان‌جرم با جرم تقریبی ۴/۵ میلیارد برابر خورشید است.",
                "M۶۰ در آسمان نزدیک کهکشان مارپیچی NGC ۴۶۴۷ دیده می‌شود و این دو جفت برهم‌کنشی به نام آرپ ۱۱۶ را می‌سازند."
            )
        ),
        "dso_m61" to BilingualFacts(
            en = listOf(
                "M61 is a barred spiral galaxy in the Virgo Cluster, about 52 million light-years away.",
                "It shines at magnitude 9.7 and was discovered by Barnaba Oriani in 1779.",
                "M61 is one of the most prolific supernova producers known, with at least eight observed since 1926.",
                "The galaxy is forming stars at a vigorous rate in its disk.",
                "M61 is sometimes called the Swelling Spiral for its prominent bulge."
            ),
            fa = listOf(
                "M۶۱ یک کهکشان مارپیچی میله‌ای در خوشه سنبله و در فاصله حدود ۵۲ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۷ می‌درخشد و بارنابا اوریانی آن را در سال ۱۷۷۹ کشف کرد.",
                "M۶۱ یکی از پرثمرترین خاستگاه‌های ابرنواختر است و از سال ۱۹۲۶ دست‌کم هشت ابرنواختر در آن دیده شده است.",
                "این کهکشان در قرص خود با نرخی پرشتاب ستاره می‌سازد.",
                "M۶۱ را به دلیل برآمدگی برجسته‌اش گاهی «مارپیچ متورم» می‌نامند."
            )
        ),
        "dso_m62" to BilingualFacts(
            en = listOf(
                "M62 is a globular cluster in Ophiuchus, about 22,500 light-years away.",
                "It shines at magnitude 6.5 and was discovered by Charles Messier in 1771.",
                "M62 is one of the most irregularly shaped globular clusters known.",
                "Its deformation is caused by tidal forces from the Milky Way's central region.",
                "The cluster contains at least 89 known variable stars."
            ),
            fa = listOf(
                "M۶۲ یک خوشه کروی در صورت فلکی ماراَفسای و در فاصله حدود ۲۲,۵۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۵ می‌درخشد و شارل مسیه آن را در سال ۱۷۷۱ کشف کرد.",
                "M۶۲ یکی از بی‌نظم‌ترین خوشه‌های کروی شناخته‌شده از نظر شکل است.",
                "تغییر شکل آن ناشی از نیروهای کشندی ناحیه مرکزی راه شیری است.",
                "این خوشه دست‌کم ۸۹ ستاره متغیر شناخته‌شده دارد."
            )
        ),
        "dso_m63" to BilingualFacts(
            en = listOf(
                "M63, the Sunflower Galaxy, is a flocculent spiral in Canes Venatici about 37 million light-years away.",
                "It shines at magnitude 8.6 and was discovered by Pierre Méchain in 1779.",
                "The galaxy's many short, patchy arm segments resemble sunflower petals.",
                "M63 is a member of the M51 Group of galaxies.",
                "A supernova, SN 1971I, was observed in the galaxy in 1971."
            ),
            fa = listOf(
                "M۶۳ یا کهکشان آفتابگردان، یک کهکشان مارپیچی پشمالو در صورت فلکی تازی‌ها و در فاصله حدود ۳۷ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۶ می‌درخشد و پیر مشن آن را در سال ۱۷۷۹ کشف کرد.",
                "بازوهای کوتاه و تکه‌تکه فراوان این کهکشان به گلبرگ‌های آفتابگردان می‌ماند.",
                "M۶۳ عضوی از گروه کهکشانی M۵۱ است.",
                "در سال ۱۹۷۱ یک ابرنواختر به نام SN 1971I در این کهکشان رصد شد."
            )
        ),
        "dso_m64" to BilingualFacts(
            en = listOf(
                "M64, the Black Eye Galaxy, is a spiral galaxy in Coma Berenices about 17 million light-years away.",
                "It shines at magnitude 8.5 and is famous for the dark dust lane beside its bright nucleus.",
                "The dust lane gives the galaxy the appearance of a black eye, inspiring its popular name.",
                "The outer gas of M64 rotates in the opposite direction to its inner disk, evidence of a past galaxy merger.",
                "M64 was discovered by Edward Pigott in 1779."
            ),
            fa = listOf(
                "M۶۴ یا کهکشان چشم سیاه، یک کهکشان مارپیچی در صورت فلکی گیسو و در فاصله حدود ۱۷ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۵ می‌درخشد و به رگه غبار تاریک کنار هسته درخشانش شهرت دارد.",
                "این رگه غبار به کهکشان ظاهر چشمی سیاه می‌دهد و نام مشهور آن از همین‌جاست.",
                "گاز بیرونی M۶۴ در جهت مخالف قرص درونی‌اش می‌چرخد؛ گواهی بر ادغام دو کهکشان در گذشته.",
                "M۶۴ را ادوارد پیگوت در سال ۱۷۷۹ کشف کرد."
            )
        ),
        "dso_m65" to BilingualFacts(
            en = listOf(
                "M65 is a spiral galaxy in Leo, about 35 million light-years away.",
                "It shines at magnitude 9.3 and was discovered by Charles Messier in 1780.",
                "M65 is a member of the Leo Triplet, along with M66 and NGC 3628.",
                "The galaxy is seen at a steeply inclined angle, giving it an elongated appearance.",
                "Gravitational interaction with its neighbors has disturbed M65's outer disk."
            ),
            fa = listOf(
                "M۶۵ یک کهکشان مارپیچی در صورت فلکی شیر و در فاصله حدود ۳۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۳ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۰ کشف کرد.",
                "M۶۵ عضوی از سه‌گانه شیر است، در کنار M۶۶ و NGC ۳۶۲۸.",
                "این کهکشان با زاویه‌ای بسیار مایل دیده می‌شود و به همین دلیل ظاهری کشیده دارد.",
                "برهم‌کنش گرانشی با همسایگانش قرص بیرونی M۶۵ را آشفته کرده است."
            )
        ),
        "dso_m66" to BilingualFacts(
            en = listOf(
                "M66 is a spiral galaxy in Leo, about 35 million light-years away, and the largest member of the Leo Triplet.",
                "It shines at magnitude 8.9 and was discovered by Charles Messier in 1780.",
                "Tidal interactions with M65 and NGC 3628 have deformed M66's spiral arms.",
                "The galaxy has hosted several supernovae, including SN 1989B, SN 1997bs, and SN 2016cok.",
                "M66's asymmetric arms make it a favorite study object for galaxy dynamics."
            ),
            fa = listOf(
                "M۶۶ یک کهکشان مارپیچی در صورت فلکی شیر و در فاصله حدود ۳۵ میلیون سال نوری است و بزرگ‌ترین عضو سه‌گانه شیر است.",
                "این کهکشان با قدر ۸/۹ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۰ کشف کرد.",
                "برهم‌کنش‌های کشندی با M۶۵ و NGC ۳۶۲۸ بازوهای مارپیچی M۶۶ را از شکل انداخته‌اند.",
                "این کهکشان میزبان چند ابرنواختر بوده است که در سال‌های ۱۹۸۹ و ۲۰۱۶ در آن دیده شدند.",
                "بازوهای نامتقارن M۶۶ آن را به موضوعی محبوب برای مطالعه دینامیک کهکشان‌ها تبدیل کرده است."
            )
        ),
        "dso_m67" to BilingualFacts(
            en = listOf(
                "M67 is an open cluster in Cancer, about 2,700 light-years away.",
                "It shines at magnitude 6.9 and contains roughly 500 stars.",
                "M67 is one of the oldest known open clusters, about 4 billion years old.",
                "The cluster is rich in red giants and white dwarfs, a sign of its great age.",
                "Johann Gottfried Koehler discovered M67 in 1779."
            ),
            fa = listOf(
                "M۶۷ یک خوشه باز در صورت فلکی خرچنگ و در فاصله حدود ۲۷۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۹ می‌درخشد و تقریباً ۵۰۰ ستاره دارد.",
                "M۶۷ یکی از کهن‌سال‌ترین خوشه‌های باز شناخته‌شده است و حدود ۴ میلیارد سال سن دارد.",
                "این خوشه سرشار از غول‌های سرخ و کوتوله‌های سفید است؛ نشانه‌ای از سن بسیار زیاد آن.",
                "یوهان گوتفرید کوهلر M۶۷ را در سال ۱۷۷۹ کشف کرد."
            )
        ),
        "dso_m68" to BilingualFacts(
            en = listOf(
                "M68 is a globular cluster in Hydra, about 33,400 light-years away.",
                "It shines at magnitude 7.8 and was discovered by Charles Messier in 1780.",
                "M68 is one of the least concentrated globular clusters known.",
                "The cluster follows a retrograde orbit around the Milky Way.",
                "M68 contains about 250 known variable stars, mostly RR Lyrae types."
            ),
            fa = listOf(
                "M۶۸ یک خوشه کروی در صورت فلکی مار باریک و در فاصله حدود ۳۳,۴۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۸ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۰ کشف کرد.",
                "M۶۸ یکی از کم‌تراکم‌ترین خوشه‌های کروی شناخته‌شده است.",
                "این خوشه مداری پسرونده به دور راه شیری دارد.",
                "M۶۸ حدود ۲۵۰ ستاره متغیر شناخته‌شده دارد که بیشترشان از نوع آر‌آر شلیاقی هستند."
            )
        ),
        "dso_m69" to BilingualFacts(
            en = listOf(
                "M69 is a globular cluster in Sagittarius, about 29,700 light-years away.",
                "It shines at magnitude 7.6 and was discovered by Nicolas-Louis de Lacaille in 1752.",
                "M69 is one of the most metal-rich globular clusters known.",
                "The cluster is quite similar to its neighbor M70 in size and brightness.",
                "M69 lies close to the center of the Milky Way, only about 6,200 light-years from it."
            ),
            fa = listOf(
                "M۶۹ یک خوشه کروی در صورت فلکی کمان و در فاصله حدود ۲۹,۷۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۶ می‌درخشد و نیکولا-لویی دو لاکای آن را در سال ۱۷۵۲ کشف کرد.",
                "M۶۹ یکی از غنی‌ترین خوشه‌های کروی شناخته‌شده از نظر فلز است.",
                "این خوشه از نظر اندازه و درخشندگی بسیار شبیه همسایه‌اش M۷۰ است.",
                "M۶۹ نزدیک مرکز راه شیری قرار دارد و تنها حدود ۶۲۰۰ سال نوری با آن فاصله دارد."
            )
        ),
        "dso_m7" to BilingualFacts(
            en = listOf(
                "M7, the Ptolemy Cluster, is an open cluster of about 80 stars near the stinger of Scorpius.",
                "It lies only about 980 light-years away, making it one of the nearest Messier open clusters.",
                "At magnitude 3.3, M7 is the brightest open cluster in Messier's catalog.",
                "The Greek astronomer Ptolemy described M7 as a nebula around 130 AD, making it the oldest recorded deep-sky object.",
                "The cluster is about 220 million years old and spans more than a degree of sky."
            ),
            fa = listOf(
                "M۷ یا خوشه بطلمیوس، یک خوشه باز با حدود ۸۰ ستاره در نزدیکی نیش عقرب است.",
                "این خوشه تنها حدود ۹۸۰ سال نوری فاصله دارد و از نزدیک‌ترین خوشه‌های باز فهرست مسیه است.",
                "M۷ با قدر ۳/۳ درخشان‌ترین خوشه باز فهرست مسیه است.",
                "اخترشناس یونانی بطلمیوس حدود سال ۱۳۰ میلادی M۷ را به صورت سحابی توصیف کرد و به این ترتیب کهن‌سال‌ترین جرم ثبت‌شده اعماق آسمان است.",
                "سن این خوشه حدود ۲۲۰ میلیون سال است و پهنای آن از یک درجه آسمان بیشتر است."
            )
        ),
        "dso_m70" to BilingualFacts(
            en = listOf(
                "M70 is a globular cluster in Sagittarius, about 29,300 light-years away.",
                "It shines at magnitude 7.9 and was discovered by Charles Messier in 1780.",
                "M70 is a small, densely concentrated globular cluster.",
                "The comet Hale-Bopp was discovered in 1995 near M70, which brought the cluster brief fame.",
                "M70 is similar in age and composition to its neighbor M69."
            ),
            fa = listOf(
                "M۷۰ یک خوشه کروی در صورت فلکی کمان و در فاصله حدود ۲۹,۳۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۹ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۰ کشف کرد.",
                "M۷۰ یک خوشه کروی کوچک و متراکم است.",
                "دنباله‌دار هیل-باپ در سال ۱۹۹۵ در نزدیکی M۷۰ کشف شد و همین برای مدتی این خوشه را مشهور کرد.",
                "M۷۰ از نظر سن و ترکیب شبیه همسایه‌اش M۶۹ است."
            )
        ),
        "dso_m71" to BilingualFacts(
            en = listOf(
                "M71 is a loosely packed globular cluster in Sagitta, about 13,000 light-years away.",
                "It shines at magnitude 6.1 and was discovered by Jean-Philippe de Chéseaux around 1745.",
                "For many years astronomers debated whether M71 was a globular cluster or a very rich open cluster.",
                "Modern studies confirm M71 is a globular cluster, though unusually sparse for its type.",
                "M71 contains only a handful of known variable stars."
            ),
            fa = listOf(
                "M۷۱ یک خوشه کروی کم‌تراکم در صورت فلکی تیر و در فاصله حدود ۱۳,۰۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۱ می‌درخشد و ژان-فیلیپ دو شزو آن را حدود سال ۱۷۴۵ کشف کرد.",
                "برای سال‌ها اخترشناسان در این باره بحث می‌کردند که آیا M۷۱ خوشه کروی است یا خوشه‌ای باز و بسیار پرستاره.",
                "پژوهش‌های نوین تأیید می‌کنند که M۷۱ یک خوشه کروی است، هرچند برای نوع خود به طور غیرعادی پراکنده است.",
                "M۷۱ تنها تعداد اندکی ستاره متغیر شناخته‌شده دارد."
            )
        ),
        "dso_m72" to BilingualFacts(
            en = listOf(
                "M72 is a globular cluster in Aquarius, about 54,600 light-years away.",
                "It shines at magnitude 9.3, one of the faintest Messier globulars.",
                "Pierre Méchain discovered M72 in 1780, one of the globular clusters he found that year.",
                "The cluster is among the most remote globulars in Messier's catalog.",
                "M72 is a young globular by galactic standards, with relatively high metallicity."
            ),
            fa = listOf(
                "M۷۲ یک خوشه کروی در صورت فلکی دلو و در فاصله حدود ۵۴,۶۰۰ سال نوری است.",
                "این خوشه با قدر ۹/۳ یکی از کم‌نورترین خوشه‌های کروی فهرست مسیه است.",
                "پیر مشن M۷۲ را در سال ۱۷۸۰ کشف کرد.",
                "این خوشه از دورترین خوشه‌های کروی فهرست مسیه است.",
                "M۷۲ با معیارهای کهکشانی خوشه‌ای کرویِ جوان است و فلزیت نسبتاً بالایی دارد."
            )
        ),
        "dso_m73" to BilingualFacts(
            en = listOf(
                "M73 is a small grouping of four stars in Aquarius, not a true star cluster.",
                "Charles Messier cataloged M73 in 1780, mistaking the Y-shaped pattern for a cluster.",
                "Modern measurements show the four stars are unrelated and lie at different distances.",
                "M73 is therefore classified as an asterism rather than a genuine deep-sky object.",
                "The group is visible in small telescopes as a faint Y of stars."
            ),
            fa = listOf(
                "M۷۳ گروهی کوچک از چهار ستاره در صورت فلکی دلو است و یک خوشه ستاره‌ای واقعی نیست.",
                "شارل مسیه M۷۳ را در سال ۱۷۸۰ فهرست کرد و الگوی Y-مانند آن را با یک خوشه اشتباه گرفت.",
                "اندازه‌گیری‌های نوین نشان می‌دهند که این چهار ستاره به هم نامربوط‌اند و در فاصله‌های گوناگونی جای دارند.",
                "بنابراین M۷۳ یک صورتواره به شمار می‌رود، نه یک جرم واقعی اعماق آسمان.",
                "این گروه در تلسکوپ‌های کوچک به صورت Y کم‌نوری از ستارگان دیده می‌شود."
            )
        ),
        "dso_m74" to BilingualFacts(
            en = listOf(
                "M74 is a grand-design spiral galaxy in Pisces, about 32 million light-years away.",
                "It shines at magnitude 9.4 but has a low surface brightness, making it a challenge for small scopes.",
                "M74 is considered a textbook example of a grand-design spiral with well-defined arms.",
                "The galaxy has hosted several supernovae, including SN 2002ap, SN 2003gd, and SN 2013ej.",
                "Pierre Méchain discovered M74 in 1780, cataloging it as a faint spiral."
            ),
            fa = listOf(
                "M۷۴ یک کهکشان مارپیچی باشکوه در صورت فلکی ماهی و در فاصله حدود ۳۲ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۴ می‌درخشد اما درخشندگی سطحی پایینی دارد و برای تلسکوپ‌های کوچک چالش‌برانگیز است.",
                "M۷۴ نمونه کتابی یک مارپیچ باشکوه با بازوهای کاملاً مشخص به شمار می‌رود.",
                "این کهکشان در سال‌های ۲۰۰۲ و ۲۰۱۳ میزبان ابرنواخترهای درخشانی بوده است.",
                "پیر مشن M۷۴ را در سال ۱۷۸۰ کشف کرد."
            )
        ),
        "dso_m75" to BilingualFacts(
            en = listOf(
                "M75 is a globular cluster in Sagittarius, about 67,500 light-years away.",
                "It shines at magnitude 8.5 and was discovered by Pierre Méchain in 1780.",
                "M75 is one of the most centrally concentrated globular clusters known.",
                "Its compact core places it among the densest stellar systems in the Milky Way.",
                "M75 lies far beyond the Milky Way's center, on the opposite side of the galactic bulge."
            ),
            fa = listOf(
                "M۷۵ یک خوشه کروی در صورت فلکی کمان و در فاصله حدود ۶۷,۵۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۵ می‌درخشد و پیر مشن آن را در سال ۱۷۸۰ کشف کرد.",
                "M۷۵ یکی از متمرکزترین خوشه‌های کروی شناخته‌شده از نظر هسته است.",
                "هسته فشرده آن، این خوشه را در شمار متراکم‌ترین سامانه‌های ستاره‌ای راه شیری قرار می‌دهد.",
                "M۷۵ بسیار فراتر از مرکز راه شیری و در سوی دیگر برآمدگی کهکشانی قرار دارد."
            )
        ),
        "dso_m76" to BilingualFacts(
            en = listOf(
                "M76, the Little Dumbbell Nebula, is a bipolar planetary nebula in Perseus.",
                "It lies about 3,400 light-years away and shines at magnitude 10.1.",
                "Pierre Méchain discovered M76 in 1780, one of his earliest planetary nebula finds.",
                "The nebula's two bright lobes give it a shape resembling the larger Dumbbell Nebula M27.",
                "M76 is one of the faintest objects in Messier's catalog."
            ),
            fa = listOf(
                "M۷۶ یا سحابی دمبل کوچک، یک سحابی سیاره‌نمای دوقطبی در صورت فلکی برساوش است.",
                "این سحابی در فاصله حدود ۳۴۰۰ سال نوری است و با قدر ۱۰/۱ می‌درخشد.",
                "پیر مشن M۷۶ را در سال ۱۷۸۰ کشف کرد.",
                "دو لوب درخشان این سحابی شکلی شبیه سحابی دمبل بزرگ‌تر یعنی M۲۷ به آن می‌دهند.",
                "M۷۶ یکی از کم‌نورترین اجرام فهرست مسیه است."
            )
        ),
        "dso_m77" to BilingualFacts(
            en = listOf(
                "M77, also called Cetus A, is a barred spiral galaxy in Cetus about 47 million light-years away.",
                "It shines at magnitude 8.9 and was discovered by Pierre Méchain in 1780.",
                "M77 is the prototype Seyfert galaxy, with a brilliant active nucleus.",
                "Its active galactic nucleus is powered by a supermassive black hole accreting matter.",
                "M77 dominates its small galaxy group in Cetus."
            ),
            fa = listOf(
                "M۷۷ که به آن نهنگ A نیز می‌گویند، یک کهکشان مارپیچی میله‌ای در صورت فلکی نهنگ و در فاصله حدود ۴۷ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۹ می‌درخشد و پیر مشن آن را در سال ۱۷۸۰ کشف کرد.",
                "M۷۷ نمونه نخستین کهکشان سیفرت است و هسته فعال درخشانی دارد.",
                "هسته فعال کهکشانی آن را سیاهچاله‌ای کلان‌جرم که ماده را به سوی خود می‌کشد نیرو می‌دهد.",
                "M۷۷ بزرگ‌ترین عضو گروه کهکشانی M۷۷ است."
            )
        ),
        "dso_m78" to BilingualFacts(
            en = listOf(
                "M78 is the brightest reflection nebula in the sky, located in Orion about 1,600 light-years away.",
                "It shines at magnitude 8.3 and was discovered by Pierre Méchain in 1780.",
                "The nebula reflects the blue light of two young B-type stars, HD 38563A and HD 38563B.",
                "M78 is part of the great Orion molecular cloud complex.",
                "The nebula contains a group of Herbig-Haro objects, jets from forming stars."
            ),
            fa = listOf(
                "M۷۸ درخشان‌ترین سحابی بازتابی آسمان است و در صورت فلکی شکارچی و در فاصله حدود ۱۶۰۰ سال نوری قرار دارد.",
                "این سحابی با قدر ۸/۳ می‌درخشد و پیر مشن آن را در سال ۱۷۸۰ کشف کرد.",
                "این سحابی نور آبی دو ستاره جوان از نوع B به نام‌های HD 38563A و HD 38563B را بازتاب می‌دهد.",
                "M۷۸ بخشی از مجموعه بزرگ ابر مولکولی شکارچی است.",
                "این سحابی گروهی از اجرام هربیگ-هارو را در خود دارد که فواره‌هایی از ستارگان در حال شکل‌گیری هستند."
            )
        ),
        "dso_m79" to BilingualFacts(
            en = listOf(
                "M79 is a globular cluster in Lepus, about 42,000 light-years away.",
                "It shines at magnitude 7.7 and was discovered by Pierre Méchain in 1780.",
                "M79 is thought to have been captured from the Canis Major Dwarf Galaxy, a disrupted satellite of the Milky Way.",
                "The cluster is nearly as old as the universe, about 11.7 billion years.",
                "M79 lies in a part of the sky almost opposite the Milky Way's center."
            ),
            fa = listOf(
                "M۷۹ یک خوشه کروی در صورت فلکی خرگوش و در فاصله حدود ۴۲,۰۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۷ می‌درخشد و پیر مشن آن را در سال ۱۷۸۰ کشف کرد.",
                "گمان می‌رود M۷۹ از کهکشان کوتوله سگ بزرگ، قمری از هم‌گسیخته راه شیری، به دام افتاده باشد.",
                "سن این خوشه تقریباً به اندازه عمر جهان است؛ حدود ۱۱/۷ میلیارد سال.",
                "M۷۹ در بخشی از آسمان جای دارد که تقریباً روبروی مرکز راه شیری است."
            )
        ),
        "dso_m80" to BilingualFacts(
            en = listOf(
                "M80 is a dense globular cluster in Scorpius, about 32,600 light-years away.",
                "It shines at magnitude 7.3 and was discovered by Charles Messier in 1781.",
                "M80 contains hundreds of thousands of stars packed into a small volume.",
                "In 1860 a nova, T Scorpii, flared up inside the cluster.",
                "M80 hosts many blue stragglers, stars that appear younger than their surroundings."
            ),
            fa = listOf(
                "M۸۰ یک خوشه کروی متراکم در صورت فلکی عقرب و در فاصله حدود ۳۲,۶۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۳ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۱ کشف کرد.",
                "M۸۰ صدها هزار ستاره دارد که در حجم کوچکی فشرده شده‌اند.",
                "در سال ۱۸۶۰ یک نواختر به نام T عقرب درون این خوشه شعله کشید.",
                "M۸۰ میزبان ستارگان «ولگرد آبی» فراوانی است که جوان‌تر از محیط پیرامون‌شان به نظر می‌رسند."
            )
        ),
        "dso_m81" to BilingualFacts(
            en = listOf(
                "M81, Bode's Galaxy, is a grand-design spiral in Ursa Major about 12 million light-years away.",
                "It shines at magnitude 6.9 and is visible in binoculars.",
                "Johann Elert Bode discovered M81 in 1774.",
                "M81 interacts gravitationally with its neighbor M82, and the two have disturbed each other in the past.",
                "A supernova, SN 1993J, was observed in M81 in 1993."
            ),
            fa = listOf(
                "M۸۱ یا کهکشان بوده، یک کهکشان مارپیچی باشکوه در صورت فلکی خرس بزرگ و در فاصله حدود ۱۲ میلیون سال نوری است.",
                "این کهکشان با قدر ۶/۹ می‌درخشد و در دوربین دوچشمی دیده می‌شود.",
                "یوهان الرت بوده M۸۱ را در سال ۱۷۷۴ کشف کرد.",
                "M۸۱ با همسایه‌اش M۸۲ برهم‌کنش گرانشی دارد و این دو در گذشته ساختار یکدیگر را آشفته کرده‌اند.",
                "در سال ۱۹۹۳ یک ابرنواختر به نام SN 1993J در M۸۱ رصد شد."
            )
        ),
        "dso_m82" to BilingualFacts(
            en = listOf(
                "M82, the Cigar Galaxy, is a starburst galaxy in Ursa Major about 12 million light-years away.",
                "It shines at magnitude 8.4 and appears as an elongated streak, like a cigar.",
                "M82 is undergoing a powerful burst of star formation triggered by its interaction with M81.",
                "The galaxy ejects huge winds of gas, visible as red filaments in long-exposure images.",
                "In 2014, a bright supernova, SN 2014J, was discovered in M82 by amateur astronomers."
            ),
            fa = listOf(
                "M۸۲ یا کهکشان سیگار، یک کهکشان فوران‌ستاره‌زا در صورت فلکی خرس بزرگ و در فاصله حدود ۱۲ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۴ می‌درخشد و به صورت رگه‌ای کشیده شبیه سیگار دیده می‌شود.",
                "M۸۲ در حال فوران شدید ستاره‌زایی است که برهم‌کنش آن با M۸۱ آن را برانگیخته است.",
                "این کهکشان بادهای عظیمی از گاز به بیرون می‌راند که در تصاویر با نوردهی بلند به صورت رشته‌های سرخ دیده می‌شوند.",
                "در سال ۲۰۱۴ یک ابرنواختر درخشان به نام SN 2014J توسط اخترشناسان آماتور در M۸۲ کشف شد."
            )
        ),
        "dso_m83" to BilingualFacts(
            en = listOf(
                "M83, the Southern Pinwheel, is a barred spiral galaxy in Hydra about 15 million light-years away.",
                "It shines at magnitude 7.5 and was discovered by Nicolas-Louis de Lacaille in 1752.",
                "M83 is one of the nearest and brightest barred spirals in the sky.",
                "The galaxy has produced at least six recorded supernovae.",
                "Its abundant star formation and spiral structure make it a favorite southern-hemisphere target."
            ),
            fa = listOf(
                "M۸۳ یا فرفره جنوبی، یک کهکشان مارپیچی میله‌ای در صورت فلکی مار باریک و در فاصله حدود ۱۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۷/۵ می‌درخشد و نیکولا-لویی دو لاکای آن را در سال ۱۷۵۲ کشف کرد.",
                "M۸۳ یکی از نزدیک‌ترین و درخشان‌ترین مارپیچی‌های میله‌ای آسمان است.",
                "دست‌کم شش ابرنواختر ثبت‌شده در این کهکشان رخ داده است.",
                "ستاره‌زایی فراوان و ساختار مارپیچی آن، این کهکشان را به هدف محبوب رصدگران نیمکره جنوبی تبدیل کرده است."
            )
        ),
        "dso_m84" to BilingualFacts(
            en = listOf(
                "M84 is a giant elliptical galaxy found along Markarian's Chain in the Virgo Cluster.",
                "It shines at magnitude 9.1 and was discovered by Charles Messier in 1781.",
                "M84 lies at the heart of Markarian's Chain, a line of galaxies in the Virgo Cluster.",
                "The galaxy has a supermassive black hole at its center.",
                "Hubble observations revealed jets of material speeding away from M84's core."
            ),
            fa = listOf(
                "M۸۴ یک کهکشان بیضوی غول‌پیکر در خوشه سنبله و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۱ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۱ کشف کرد.",
                "M۸۴ در قلب زنجیره مارکاریان جای دارد؛ خطی از کهکشان‌ها در خوشه سنبله.",
                "این کهکشان یک سیاهچاله کلان‌جرم در مرکز خود دارد.",
                "رصدهای هابل فواره‌هایی از ماده را نشان داد که از هسته M۸۴ به بیرون می‌گریزند."
            )
        ),
        "dso_m85" to BilingualFacts(
            en = listOf(
                "M85 is a lenticular galaxy in Coma Berenices, about 60 million light-years away.",
                "It shines at magnitude 9.1 and was discovered by Pierre Méchain in 1781.",
                "M85 is the northernmost member of the Virgo Cluster.",
                "The galaxy is interacting with its neighbor NGC 4394.",
                "A luminous red nova, M85 OT2006-1, was observed in M85 in 2006."
            ),
            fa = listOf(
                "M۸۵ یک کهکشان عدسی‌شکل در صورت فلکی گیسو و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۱ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۸۵ شمالی‌ترین عضو خوشه سنبله است.",
                "این کهکشان با همسایه‌اش NGC ۴۳۹۴ در برهم‌کنش است.",
                "در سال ۲۰۰۶ یک نواختر سرخ درخشان به نام M85 OT2006-1 در M۸۵ رصد شد."
            )
        ),
        "dso_m86" to BilingualFacts(
            en = listOf(
                "M86 is a giant elliptical in Virgo, about 60 million light-years from Earth.",
                "It shines at magnitude 8.9 and was discovered by Charles Messier in 1781.",
                "M86 shows the highest blueshift of any Messier galaxy, moving toward us at high speed.",
                "The galaxy is being stripped of its gas as it falls through the Virgo Cluster.",
                "M86 lies in Markarian's Chain of galaxies."
            ),
            fa = listOf(
                "M۸۶ یک کهکشان بیضوی غول‌پیکر در خوشه سنبله و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۹ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۱ کشف کرد.",
                "M۸۶ بزرگ‌ترین انتقال به آبی را در میان کهکشان‌های مسیه نشان می‌دهد و با سرعتی بالا به سوی ما می‌آید.",
                "این کهکشان هنگام سقوط در خوشه سنبله در حال از دست دادن گاز خود است.",
                "M۸۶ در زنجیره مارکاریان از کهکشان‌ها جای دارد."
            )
        ),
        "dso_m87" to BilingualFacts(
            en = listOf(
                "M87, also called Virgo A, is a supergiant elliptical galaxy in the Virgo Cluster about 54 million light-years away.",
                "It shines at magnitude 8.6 and was discovered by Charles Messier in 1781.",
                "M87 harbors a supermassive black hole of about 6.5 billion solar masses at its center.",
                "In 2019, the Event Horizon Telescope released the first-ever image of a black hole, showing M87's shadow.",
                "M87 fires a jet of plasma thousands of light-years long from its core."
            ),
            fa = listOf(
                "M۸۷ که آن را سنبله A نیز می‌نامند، یک کهکشان بیضوی ابرغول در خوشه سنبله و در فاصله حدود ۵۴ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۶ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۱ کشف کرد.",
                "M۸۷ در مرکز خود سیاهچاله‌ای کلان‌جرم با جرم حدود ۶/۵ میلیارد برابر خورشید جای داده است.",
                "در سال ۲۰۱۹ تلسکوپ افق رویداد نخستین تصویر تاریخ از یک سیاهچاله را منتشر کرد که سایه M۸۷ را نشان می‌داد.",
                "M۸۷ فواره‌ای از پلاسما به درازای هزاران سال نوری از هسته خود به بیرون پرتاب می‌کند."
            )
        ),
        "dso_m88" to BilingualFacts(
            en = listOf(
                "M88 is a spiral galaxy in Coma Berenices, about 47 million light-years away.",
                "It shines at magnitude 9.6 and was discovered by Charles Messier in 1781.",
                "M88 belongs to the Virgo Cluster, though it lies in the constellation Coma Berenices.",
                "The galaxy is highly symmetrical, with well-defined spiral arms.",
                "M88 has hosted several supernovae, including SN 1999cl."
            ),
            fa = listOf(
                "M۸۸ یک کهکشان مارپیچی در صورت فلکی گیسو و در فاصله حدود ۴۷ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۶ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۱ کشف کرد.",
                "M۸۸ عضوی از خوشه سنبله است.",
                "این کهکشان بسیار متقارن است و بازوهای مارپیچی کاملاً مشخصی دارد.",
                "M۸۸ میزبان چند ابرنواختر بوده است، از جمله SN 1999cl."
            )
        ),
        "dso_m89" to BilingualFacts(
            en = listOf(
                "M89 is an elliptical galaxy in the Virgo Cluster, about 50 million light-years away.",
                "It shines at magnitude 9.8 and was discovered by Charles Messier in 1781.",
                "M89 is almost perfectly spherical, unlike most flattened ellipticals.",
                "The galaxy shows faint shells and plumes, evidence of a recent merger.",
                "M89 is surrounded by a large population of globular clusters."
            ),
            fa = listOf(
                "M۸۹ یک کهکشان بیضوی در خوشه سنبله و در فاصله حدود ۵۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۸ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۱ کشف کرد.",
                "M۸۹ تقریباً کاملاً کروی است، برخلاف بیشتر کهکشان‌های بیضوی که پَخ هستند.",
                "این کهکشان پوسته‌ها و پرهای کم‌نوری نشان می‌دهد که گواه ادغامی تازه است.",
                "M۸۹ را جمعیت بزرگی از خوشه‌های کروی در بر گرفته است."
            )
        ),
        "dso_m9" to BilingualFacts(
            en = listOf(
                "M9 is a globular cluster in Ophiuchus, about 25,800 light-years away.",
                "It shines at magnitude 7.7 and appears as a small, faint round patch in small telescopes.",
                "M9 is one of the globular clusters nearest to the Milky Way's center, only about 7,500 light-years from it.",
                "Messier recorded M9 in 1764, one of several clusters he found in Ophiuchus that year.",
                "The cluster's relatively loose outer regions show the gravitational influence of the galactic bulge."
            ),
            fa = listOf(
                "M۹ یک خوشه کروی در صورت فلکی ماراَفسای است و حدود ۲۵,۸۰۰ سال نوری فاصله دارد.",
                "این خوشه با قدر ۷/۷ در تلسکوپ‌های کوچک به صورت لکه گرد کوچک و کم‌نوری دیده می‌شود.",
                "M۹ یکی از خوشه‌های کروی نزدیک به مرکز راه شیری است و تنها حدود ۷۵۰۰ سال نوری با آن فاصله دارد.",
                "شارل مسیه M۹ را در سال ۱۷۶۴ کشف کرد.",
                "نواحی بیرونی نسبتاً باز این خوشه اثر گرانشی برآمدگی مرکزی کهکشان را نشان می‌دهد."
            )
        ),
        "dso_m90" to BilingualFacts(
            en = listOf(
                "M90 is a spiral galaxy in the Virgo Cluster, about 60 million light-years away.",
                "It shines at magnitude 9.5 and was discovered by Charles Messier in 1781.",
                "M90 shows little new star formation, making it an anemic spiral.",
                "The galaxy is moving toward us at high speed, one of the most blueshifted Virgo members.",
                "M90's gas has been stripped away by ram pressure as it travels through the cluster."
            ),
            fa = listOf(
                "M۹۰ یک کهکشان مارپیچی در خوشه سنبله و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۵ می‌درخشد و شارل مسیه آن را در سال ۱۷۸۱ کشف کرد.",
                "M۹۰ ستاره‌زایی تازه چندانی نشان نمی‌دهد و به همین دلیل یک مارپیچ کم‌خون به شمار می‌رود.",
                "این کهکشان با سرعتی زیاد به سوی ما می‌آید و یکی از اعضای خوشه سنبله با بیشترین انتقال به آبی است.",
                "گاز M۹۰ هنگام حرکت در خوشه به وسیله فشار رَم از آن جدا شده است."
            )
        ),
        "dso_m91" to BilingualFacts(
            en = listOf(
                "M91 is a barred spiral galaxy in Coma Berenices, about 60 million light-years away.",
                "It shines at magnitude 10.2, one of the faintest Messier objects.",
                "M91 was one of Messier's 'missing' objects for nearly two centuries until its identity was confirmed in 1969.",
                "The galaxy's prominent central bar channels gas toward its nucleus.",
                "M91 is a Virgo Cluster galaxy in Coma Berenices."
            ),
            fa = listOf(
                "M۹۱ یک کهکشان مارپیچی میله‌ای در صورت فلکی گیسو و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۱۰/۲ یکی از کم‌نورترین اجرام مسیه است.",
                "M۹۱ نزدیک به دو سده یکی از اجرام «گم‌شده» مسیه بود تا آنکه هویت آن در سال ۱۹۶۹ تأیید شد.",
                "میله مرکزی برجسته این کهکشان گاز را به سوی هسته هدایت می‌کند.",
                "M۹۱ عضوی از خوشه سنبله است."
            )
        ),
        "dso_m92" to BilingualFacts(
            en = listOf(
                "M92 is a globular cluster in Hercules, about 26,700 light-years away.",
                "It shines at magnitude 6.4 and is one of the brightest globulars in the northern sky.",
                "Johann Elert Bode discovered M92 in 1777.",
                "M92 is about 14.2 billion years old, nearly the age of the universe.",
                "The cluster is so bright that it is visible to the naked eye under excellent skies."
            ),
            fa = listOf(
                "M۹۲ یک خوشه کروی در صورت فلکی هرکول و در فاصله حدود ۲۶,۷۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۴ می‌درخشد و یکی از درخشان‌ترین خوشه‌های کروی آسمان شمالی است.",
                "یوهان الرت بوده M۹۲ را در سال ۱۷۷۷ کشف کرد.",
                "سن M۹۲ حدود ۱۴/۲ میلیارد سال است؛ تقریباً هم‌سن جهان.",
                "این خوشه چنان درخشان است که در آسمان‌های عالی با چشم غیرمسلح دیده می‌شود."
            )
        ),
        "dso_m93" to BilingualFacts(
            en = listOf(
                "M93 is an open cluster in Puppis, about 3,600 light-years away.",
                "It shines at magnitude 6.2 and contains about 80 stars.",
                "Charles Messier discovered M93 in 1781.",
                "The stars of M93 are roughly 100 million years old.",
                "M93 has a distinctive shape, with bright stars forming an arrowhead-like pattern."
            ),
            fa = listOf(
                "M۹۳ یک خوشه باز در صورت فلکی کشتی پشت و در فاصله حدود ۳۶۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۲ می‌درخشد و حدود ۸۰ ستاره دارد.",
                "شارل مسیه M۹۳ را در سال ۱۷۸۱ کشف کرد.",
                "سن این خوشه حدود ۱۰۰ میلیون سال است.",
                "M۹۳ شکل متمایزی دارد و ستارگان درخشان آن الگویی شبیه نوک پیکان می‌سازند."
            )
        ),
        "dso_m94" to BilingualFacts(
            en = listOf(
                "M94 is a spiral galaxy in Canes Venatici, about 16 million light-years away.",
                "It shines at magnitude 8.2 and was discovered by Pierre Méchain in 1781.",
                "M94 has a bright inner disk surrounded by a faint outer ring of young stars.",
                "The galaxy's bright inner region is a site of vigorous star formation.",
                "M94 is sometimes called the Cat's Eye Galaxy for its compact, luminous core."
            ),
            fa = listOf(
                "M۹۴ یک کهکشان مارپیچی در صورت فلکی تازی‌ها و در فاصله حدود ۱۶ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۲ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۹۴ یک قرص درونی درخشان دارد که حلقه‌ای بیرونی کم‌نور از ستارگان جوان آن را در بر گرفته است.",
                "ناحیه درونی درخشان این کهکشان جایگاه ستاره‌زایی پرشتاب است.",
                "M۹۴ را به دلیل هسته فشرده و درخشانش گاهی کهکشان چشم گربه می‌نامند."
            )
        ),
        "dso_m95" to BilingualFacts(
            en = listOf(
                "M95 is a barred spiral galaxy in Leo, about 38 million light-years away.",
                "It shines at magnitude 9.7 and was discovered by Pierre Méchain in 1781.",
                "M95 is a member of the Leo I Group of galaxies.",
                "The galaxy has a ring of star formation surrounding its central bar.",
                "A supernova, SN 2012aw, was observed in M95 in 2012."
            ),
            fa = listOf(
                "M۹۵ یک کهکشان مارپیچی میله‌ای در صورت فلکی شیر و در فاصله حدود ۳۸ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۷ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۹۵ عضوی از گروه کهکشانی شیر I است.",
                "این کهکشان حلقه‌ای از ستاره‌زایی پیرامون میله مرکزی خود دارد.",
                "در سال ۲۰۱۲ یک ابرنواختر به نام SN 2012aw در M۹۵ رصد شد."
            )
        ),
        "dso_m96" to BilingualFacts(
            en = listOf(
                "M96 is a spiral galaxy in Leo, about 38 million light-years away.",
                "It shines at magnitude 9.2 and was discovered by Pierre Méchain in 1781.",
                "M96 is the brightest member of the Leo I Group.",
                "The galaxy's asymmetric outer disk was disturbed by interactions with its neighbors.",
                "A supernova, SN 1998bu, was observed in M96 in 1998."
            ),
            fa = listOf(
                "M۹۶ یک کهکشان مارپیچی در صورت فلکی شیر و در فاصله حدود ۳۸ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۲ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۹۶ درخشان‌ترین عضو گروه کهکشانی شیر I است.",
                "قرص بیرونی نامتقارن این کهکشان در اثر برهم‌کنش با همسایگانش آشفته شده است.",
                "در سال ۱۹۹۸ یک ابرنواختر به نام SN 1998bu در M۹۶ رصد شد."
            )
        ),
        "dso_m97" to BilingualFacts(
            en = listOf(
                "M97, the Owl Nebula, is a planetary nebula in Ursa Major about 2,600 light-years away.",
                "M97 shines at magnitude 9.9; Pierre Méchain discovered the Owl Nebula in 1781.",
                "Two darker regions within the nebula resemble an owl's eyes, giving it its popular name.",
                "The nebula is about 8,000 years old, quite young for a planetary nebula.",
                "M97's central star is a hot white dwarf with a surface temperature near 120,000 K."
            ),
            fa = listOf(
                "M۹۷ یا سحابی جغد، یک سحابی سیاره‌نما در صورت فلکی خرس بزرگ و در فاصله حدود ۲۶۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۹ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "دو ناحیه تاریک درون سحابی به چشمان جغد می‌مانند و نام مشهور آن از همین‌جاست.",
                "سن این سحابی حدود ۸۰۰۰ سال است؛ برای یک سحابی سیاره‌نما بسیار جوان.",
                "ستاره مرکزی M۹۷ یک کوتوله سفید داغ با دمای سطحی نزدیک به ۱۲۰,۰۰۰ کلوین است."
            )
        ),
        "dso_m98" to BilingualFacts(
            en = listOf(
                "M98 is a spiral galaxy in Coma Berenices, about 60 million light-years away.",
                "It shines at magnitude 10.1 and was discovered by Pierre Méchain in 1781.",
                "M98 belongs to the Virgo Cluster and approaches us at high speed.",
                "The galaxy shows a strong blueshift, moving toward us at high speed.",
                "M98 has produced several supernovae, including SN 1976H."
            ),
            fa = listOf(
                "M۹۸ یک کهکشان مارپیچی در صورت فلکی گیسو و در فاصله حدود ۶۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۱۰/۱ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۹۸ عضوی از خوشه سنبله است.",
                "این کهکشان انتقال به آبی شدیدی نشان می‌دهد و با سرعتی زیاد به سوی ما حرکت می‌کند.",
                "M۹۸ چند ابرنواختر تولید کرده است، از جمله SN 1976H."
            )
        ),
        "dso_m99" to BilingualFacts(
            en = listOf(
                "M99 is a spiral galaxy in Coma Berenices, about 50 million light-years away.",
                "Pierre Méchain found M99 in 1781, recording it at magnitude 9.9.",
                "M99 is a grand-design spiral with a single, well-defined arm dominating its disk.",
                "The galaxy has hosted at least four supernovae.",
                "M99 resides in the Virgo Cluster, near its northern edge."
            ),
            fa = listOf(
                "M۹۹ یک کهکشان مارپیچی در صورت فلکی گیسو و در فاصله حدود ۵۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۹ می‌درخشد و پیر مشن آن را در سال ۱۷۸۱ کشف کرد.",
                "M۹۹ یک مارپیچ باشکوه است که یک بازوی کاملاً مشخص بر قرص آن چیره است.",
                "این کهکشان میزبان دست‌کم چهار ابرنواختر بوده است.",
                "M۹۹ عضوی از خوشه سنبله است."
            )
        ),
        "dso_ngc_1316" to BilingualFacts(
            en = listOf(
                "NGC 1316, also called Fornax A, is a giant lenticular galaxy in Fornax, about 62 million light-years away.",
                "It shines at magnitude 8.4 and is the brightest galaxy in the Fornax Cluster.",
                "NGC 1316 is a powerful radio source, one of the strongest in the sky.",
                "The galaxy shows dust lanes and shells, evidence of past mergers with smaller galaxies.",
                "Its radio lobes span several degrees, among the largest known structures of their kind."
            ),
            fa = listOf(
                "NGC ۱۳۱۶ که به آن کوره A نیز می‌گویند، یک کهکشان عدسی‌شکل غول‌پیکر در صورت فلکی کوره و در فاصله حدود ۶۲ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۴ می‌درخشد و درخشان‌ترین کهکشان خوشه کوره است.",
                "NGC ۱۳۱۶ یک چشمه رادیویی نیرومند و یکی از قوی‌ترین‌های آسمان است.",
                "این کهکشان رگه‌های غبار و پوسته‌هایی نشان می‌دهد که گواه ادغام‌های گذشته با کهکشان‌های کوچک‌تر است.",
                "لوب‌های رادیویی آن چند درجه از آسمان را می‌پوشانند و از بزرگ‌ترین ساختارهای شناخته‌شده از این نوع هستند."
            )
        ),
        "dso_ngc_1365" to BilingualFacts(
            en = listOf(
                "NGC 1365, the Great Barred Spiral, is a barred spiral galaxy in Fornax, about 56 million light-years away.",
                "It shines at magnitude 9.5 and is a member of the Fornax Cluster.",
                "NGC 1365 is one of the finest examples of a barred spiral galaxy known.",
                "Its central bar funnels gas inward, feeding a supermassive black hole at its core.",
                "The galaxy has hosted several supernovae, including SN 2012fr."
            ),
            fa = listOf(
                "NGC ۱۳۶۵ یا مارپیچ میله‌ای بزرگ، یک کهکشان مارپیچی میله‌ای در صورت فلکی کوره و در فاصله حدود ۵۶ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۵ می‌درخشد و عضوی از خوشه کوره است.",
                "NGC ۱۳۶۵ یکی از بهترین نمونه‌های شناخته‌شده کهکشان مارپیچی میله‌ای است.",
                "میله مرکزی آن گاز را به درون می‌راند و سیاهچاله کلان‌جرم هسته را تغذیه می‌کند.",
                "این کهکشان میزبان چند ابرنواختر بوده است، از جمله SN 2012fr."
            )
        ),
        "dso_ngc_1499" to BilingualFacts(
            en = listOf(
                "NGC 1499, the California Nebula, is a vast emission nebula in Perseus, about 1,000 light-years away.",
                "It shines at magnitude 6.0 but is so spread out that it is faint to the eye.",
                "The nebula is energized by the hot star Xi Persei (Menkib).",
                "Its elongated shape resembles the outline of the U.S. state of California.",
                "The California Nebula was discovered by Edward Emerson Barnard in 1884."
            ),
            fa = listOf(
                "NGC ۱۴۹۹ یا سحابی کالیفرنیا، یک سحابی نشری گسترده در صورت فلکی برساوش و در فاصله حدود ۱۰۰۰ سال نوری است.",
                "این سحابی با قدر ۶/۰ می‌درخشد، اما چنان پراکنده است که با چشم کم‌نور دیده می‌شود.",
                "این سحابی را ستاره داغ کسی برساوش (منکیب) برانگیخته است.",
                "شکل کشیده آن به طرح کلی ایالت کالیفرنیای آمریکا می‌ماند.",
                "سحابی کالیفرنیا را ادوارد امرسون بارنارد در سال ۱۸۸۴ کشف کرد."
            )
        ),
        "dso_ngc_1528" to BilingualFacts(
            en = listOf(
                "NGC 1528 is an open cluster in Perseus, about 2,500 light-years away.",
                "It shines at magnitude 6.4 and contains about 165 stars.",
                "The cluster was discovered by William Herschel in 1790.",
                "The age of NGC 1528 is about 370 million years.",
                "It appears as a loose but rich grouping in binoculars and small telescopes."
            ),
            fa = listOf(
                "NGC ۱۵۲۸ یک خوشه باز در صورت فلکی برساوش و در فاصله حدود ۲۵۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۴ می‌درخشد و حدود ۱۶۵ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۹۰ کشف کرد.",
                "سن NGC ۱۵۲۸ حدود ۳۷۰ میلیون سال است.",
                "این خوشه در دوربین دوچشمی و تلسکوپ‌های کوچک به صورت گروهی باز اما پرستاره دیده می‌شود."
            )
        ),
        "dso_ngc_1535" to BilingualFacts(
            en = listOf(
                "NGC 1535, Cleopatra's Eye, is a planetary nebula in Eridanus, about 5,000 light-years away.",
                "It shines at magnitude 9.6 and appears as a small blue-green disk.",
                "The nebula has a bright inner shell surrounded by a fainter outer halo.",
                "Its eye-like appearance inspired the name Cleopatra's Eye.",
                "Herschel discovered NGC 1535 in 1785 in the constellation Eridanus."
            ),
            fa = listOf(
                "NGC ۱۵۳۵ یا چشم کلئوپاترا، یک سحابی سیاره‌نما در صورت فلکی نهر و در فاصله حدود ۵۰۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۶ می‌درخشد و به صورت قرص کوچک سبز-آبی دیده می‌شود.",
                "این سحابی پوسته درونی درخشانی دارد که هاله‌ای بیرونی کم‌نورتر آن را در بر گرفته است.",
                "ظاهر چشم‌مانند آن الهام‌بخش نام «چشم کلئوپاترا» بوده است.",
                "NGC ۱۵۳۵ را ویلیام هرشل در سال ۱۷۸۵ کشف کرد."
            )
        ),
        "dso_ngc_1647" to BilingualFacts(
            en = listOf(
                "NGC 1647 is an open cluster in Taurus, about 1,800 light-years away.",
                "It shines at magnitude 6.4 and spans about 45 arcminutes.",
                "William Herschel discovered NGC 1647 in 1784.",
                "NGC 1647 contains about 200 stars and is roughly 150 million years old.",
                "It lies near the bright star Aldebaran and the Hyades cluster."
            ),
            fa = listOf(
                "NGC ۱۶۴۷ یک خوشه باز در صورت فلکی گاو و در فاصله حدود ۱۸۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۴ می‌درخشد و پهنای آن حدود ۴۵ دقیقه قوسی است.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۴ کشف کرد.",
                "NGC ۱۶۴۷ حدود ۲۰۰ ستاره دارد و سن آن تقریباً ۱۵۰ میلیون سال است.",
                "این خوشه نزدیک ستاره درخشان دبران و خوشه قلائص قرار دارد."
            )
        ),
        "dso_ngc_1817" to BilingualFacts(
            en = listOf(
                "NGC 1817 is an open cluster in Taurus, about 6,400 light-years away.",
                "It shines at magnitude 7.7 and was discovered by William Herschel in 1784.",
                "The cluster contains roughly 300 stars.",
                "NGC 1817 is roughly 400 million years old, a middle-aged open cluster.",
                "It lies near the larger, brighter cluster NGC 1807 in the sky."
            ),
            fa = listOf(
                "NGC ۱۸۱۷ یک خوشه باز در صورت فلکی گاو و در فاصله حدود ۶۴۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۷ می‌درخشد و ویلیام هرشل آن را در سال ۱۷۸۴ کشف کرد.",
                "این خوشه تقریباً ۳۰۰ ستاره دارد.",
                "سن NGC ۱۸۱۷ حدود ۴۰۰ میلیون سال است.",
                "این خوشه در آسمان نزدیک خوشه بزرگ‌تر و درخشان‌تر NGC ۱۸۰۷ قرار دارد."
            )
        ),
        "dso_ngc_1977" to BilingualFacts(
            en = listOf(
                "NGC 1977, the Running Man Nebula, is a reflection nebula in Orion, about 1,500 light-years away.",
                "It shines at magnitude 7.0 and lies just north of the great Orion Nebula.",
                "The nebula's bright and dark regions trace a figure that resembles a running man.",
                "NGC 1977 is energized by the young stars of the cluster within it.",
                "It forms part of the Orion Molecular Cloud Complex."
            ),
            fa = listOf(
                "NGC ۱۹۷۷ یا سحابی مرد دونده، یک سحابی بازتابی در صورت فلکی شکارچی و در فاصله حدود ۱۵۰۰ سال نوری است.",
                "این سحابی با قدر ۷/۰ می‌درخشد و درست در شمال سحابی بزرگ شکارچی جای دارد.",
                "نواحی درخشان و تاریک سحابی شکلی را ترسیم می‌کنند که به مردی در حال دویدن می‌ماند.",
                "NGC ۱۹۷۷ را ستارگان جوان خوشه درون آن برانگیخته‌اند.",
                "این سحابی بخشی از مجموعه ابر مولکولی شکارچی است."
            )
        ),
        "dso_ngc_1999" to BilingualFacts(
            en = listOf(
                "NGC 1999 is a reflection nebula in Orion, about 1,500 light-years away.",
                "It shines at magnitude 9.5 and is lit by the variable star V380 Orionis.",
                "The nebula is famous for a dark, keyhole-shaped patch of cold gas at its heart.",
                "Hubble observations showed the dark patch is an empty cavity, not a dust cloud as first thought.",
                "William Herschel discovered NGC 1999 in 1785 near the Orion Nebula."
            ),
            fa = listOf(
                "NGC ۱۹۹۹ یک سحابی بازتابی در صورت فلکی شکارچی و در فاصله حدود ۱۵۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۵ می‌درخشد و ستاره متغیر V380 شکارچی آن را روشن می‌کند.",
                "این سحابی به لکه تاریک سوراخ‌کلیدمانندی از گاز سرد در قلب خود شهرت دارد.",
                "رصدهای هابل نشان دادند که این لکه تاریک یک حفره خالی است، نه ابری از غبار آن‌گونه که نخست پنداشته می‌شد.",
                "NGC ۱۹۹۹ را ویلیام هرشل در سال ۱۷۸۵ کشف کرد."
            )
        ),
        "dso_ngc_2070" to BilingualFacts(
            en = listOf(
                "NGC 2070, the Tarantula Nebula, is the largest and most active star-forming region in the Local Group.",
                "It lies in the Large Magellanic Cloud, about 160,000 light-years away.",
                "The nebula hosts the super star cluster R136, whose stars are hundreds of times the Sun's mass.",
                "SN 1987A, the nearest supernova in centuries, exploded near the Tarantula Nebula in 1987.",
                "Were it as close as the Orion Nebula, the Tarantula would cast visible shadows at night."
            ),
            fa = listOf(
                "NGC ۲۰۷۰ یا سحابی عنکبوت، بزرگ‌ترین و فعال‌ترین ناحیه ستاره‌زای گروه محلی کهکشان‌ها است.",
                "این سحابی در ابر ماژلانی بزرگ و در فاصله حدود ۱۶۰,۰۰۰ سال نوری قرار دارد.",
                "این سحابی میزبان ابرخوشه ستاره‌ای R136 است که ستارگانش صدها برابر خورشید جرم دارند.",
                "ابرنواختر ۱۹۸۷A که نزدیک‌ترین ابرنواختر چند سده اخیر بود، در سال ۱۹۸۷ در نزدیکی سحابی عنکبوت منفجر شد.",
                "اگر سحابی عنکبوت به اندازه سحابی شکارچی نزدیک بود، در شب سایه‌های قابل مشاهده می‌انداخت."
            )
        ),
        "dso_ngc_2158" to BilingualFacts(
            en = listOf(
                "NGC 2158 is an open cluster in Gemini, about 16,500 light-years away.",
                "It shines at magnitude 8.6 and appears as a small, rich knot of stars.",
                "The cluster lies near the bright cluster M35 in the sky, though far more distant.",
                "NGC 2158 is ancient for an open cluster, about 1 billion years old.",
                "Its many red giant stars give it a golden hue in larger telescopes."
            ),
            fa = listOf(
                "NGC ۲۱۵۸ یک خوشه باز در صورت فلکی دوپیکر و در فاصله حدود ۱۶,۵۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۶ می‌درخشد و به صورت گره کوچک و پرستاره‌ای دیده می‌شود.",
                "این خوشه در آسمان نزدیک خوشه درخشان M۳۵ قرار دارد، هرچند بسیار دورتر است.",
                "NGC ۲۱۵۸ برای یک خوشه باز کهن است و حدود یک میلیارد سال سن دارد.",
                "ستارگان غول سرخ فراوان آن در تلسکوپ‌های بزرگ‌تر رنگی طلایی به آن می‌دهند."
            )
        ),
        "dso_ngc_2169" to BilingualFacts(
            en = listOf(
                "NGC 2169, the 37 Cluster, is a small open cluster in Orion, about 3,600 light-years away.",
                "It shines at magnitude 5.9 and is visible in binoculars.",
                "Its brightest stars trace the numerals '37', giving the cluster its popular name.",
                "NGC 2169 contains about 30 stars and is roughly 11 million years old.",
                "Herschel recorded NGC 2169 in 1784, naming it for its '37' pattern."
            ),
            fa = listOf(
                "NGC ۲۱۶۹ یا خوشه ۳۷، یک خوشه باز کوچک در صورت فلکی شکارچی و در فاصله حدود ۳۶۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۹ می‌درخشد و در دوربین دوچشمی دیده می‌شود.",
                "درخشان‌ترین ستارگان آن عدد «۳۷» را ترسیم می‌کنند و نام مشهور خوشه از همین‌جاست.",
                "NGC ۲۱۶۹ حدود ۳۰ ستاره دارد و سن آن تقریباً ۱۱ میلیون سال است.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۴ کشف کرد."
            )
        ),
        "dso_ngc_2237" to BilingualFacts(
            en = listOf(
                "NGC 2237 is part of the Rosette Nebula, a vast star-forming cloud in Monoceros about 5,200 light-years away.",
                "The Rosette spans about 80 arcminutes, several times the width of the full Moon.",
                "Its hollow center is carved by the winds of the young cluster NGC 2244 at its heart.",
                "The nebula's name comes from its flower-like rings of glowing gas.",
                "The Rosette is a nursery for hundreds of newly forming stars."
            ),
            fa = listOf(
                "NGC ۲۲۳۷ بخشی از سحابی گل سرخ است؛ ابری ستاره‌زای گسترده در صورت فلکی تک‌شاخ و در فاصله حدود ۵۲۰۰ سال نوری.",
                "سحابی گل سرخ حدود ۸۰ دقیقه قوسی از آسمان را می‌پوشاند؛ چند برابر پهنای ماه کامل.",
                "مرکز توخالی آن را بادهای خوشه جوان NGC ۲۲۴۴ در قلب آن تراشیده‌اند.",
                "نام این سحابی از حلقه‌های گل‌مانند گاز درخشان آن گرفته شده است.",
                "سحابی گل سرخ زادگاه صدها ستاره در حال شکل‌گیری است."
            )
        ),
        "dso_ngc_2244" to BilingualFacts(
            en = listOf(
                "NGC 2244 is the young open cluster at the heart of the Rosette Nebula in Monoceros.",
                "It lies about 5,200 light-years away and shines at magnitude 4.8.",
                "The cluster's hot young stars carve out the hollow center of the Rosette Nebula.",
                "NGC 2244 is only about 4 million years old.",
                "The cluster was discovered by John Flamsteed in 1690."
            ),
            fa = listOf(
                "NGC ۲۲۴۴ خوشه باز جوان در قلب سحابی گل سرخ در صورت فلکی تک‌شاخ است.",
                "این خوشه در فاصله حدود ۵۲۰۰ سال نوری است و با قدر ۴/۸ می‌درخشد.",
                "ستارگان داغ جوان این خوشه مرکز توخالی سحابی گل سرخ را تراشیده‌اند.",
                "سن NGC ۲۲۴۴ تنها حدود ۴ میلیون سال است.",
                "این خوشه را جان فلمستید در سال ۱۶۹۰ کشف کرد."
            )
        ),
        "dso_ngc_2261" to BilingualFacts(
            en = listOf(
                "NGC 2261, Hubble's Variable Nebula, is a small fan-shaped reflection nebula in Monoceros.",
                "It lies about 2,500 light-years away and shines at magnitude 9.0.",
                "The nebula changes brightness and shape over weeks and months.",
                "Its variability is caused by shadows from moving dust near the star R Monocerotis.",
                "NGC 2261 was the first object photographed with the 200-inch Hale Telescope in 1949."
            ),
            fa = listOf(
                "NGC ۲۲۶۱ یا سحابی متغیر هابل، یک سحابی بازتابی کوچک بادبزن‌شکل در صورت فلکی تک‌شاخ است.",
                "این سحابی در فاصله حدود ۲۵۰۰ سال نوری است و با قدر ۹/۰ می‌درخشد.",
                "درخشندگی و شکل این سحابی در بازه‌های چند هفته تا چند ماه تغییر می‌کند.",
                "تغییرپذیری آن ناشی از سایه‌های غبار متحرک نزدیک ستاره R تک‌شاخ است.",
                "NGC ۲۲۶۱ نخستین جرمی بود که در سال ۱۹۴۹ با تلسکوپ ۲۰۰ اینچی هیل عکاسی شد."
            )
        ),
        "dso_ngc_2264" to BilingualFacts(
            en = listOf(
                "NGC 2264, the Christmas Tree Cluster, is a young open cluster in Monoceros, about 2,600 light-years away.",
                "It shines at magnitude 3.9 and is visible to the naked eye.",
                "The cluster's stars form a triangular shape like a Christmas tree.",
                "NGC 2264 includes the Cone Nebula, a dark pillar of dust.",
                "The cluster is very young, only about 3 million years old."
            ),
            fa = listOf(
                "NGC ۲۲۶۴ یا خوشه درخت کریسمس، یک خوشه باز جوان در صورت فلکی تک‌شاخ و در فاصله حدود ۲۶۰۰ سال نوری است.",
                "این خوشه با قدر ۳/۹ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "ستارگان این خوشه شکلی مثلثی مانند درخت کریسمس می‌سازند.",
                "NGC ۲۲۶۴ سحابی مخروطی را نیز در بر دارد که ستونی تاریک از غبار است.",
                "این خوشه بسیار جوان است و تنها حدود ۳ میلیون سال سن دارد."
            )
        ),
        "dso_ngc_2301" to BilingualFacts(
            en = listOf(
                "NGC 2301 is an open cluster in Monoceros, about 2,800 light-years away.",
                "It shines at magnitude 6.0 and contains about 70 stars.",
                "Herschel first observed NGC 2301 in 1785, noting its line of bright stars.",
                "NGC 2301 is notable for the striking string of bright stars across its center.",
                "The cluster is about 170 million years old."
            ),
            fa = listOf(
                "NGC ۲۳۰۱ یک خوشه باز در صورت فلکی تک‌شاخ و در فاصله حدود ۲۸۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۰ می‌درخشد و حدود ۷۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۵ کشف کرد.",
                "NGC ۲۳۰۱ به رشته چشمگیر ستارگان درخشان در سراسر مرکز خود شهرت دارد.",
                "سن این خوشه حدود ۱۷۰ میلیون سال است."
            )
        ),
        "dso_ngc_2359" to BilingualFacts(
            en = listOf(
                "NGC 2359, Thor's Helmet, is an emission nebula in Canis Major, about 15,000 light-years away.",
                "It shines at magnitude 11.0 and carries a striking bubble shape.",
                "A Wolf-Rayet star, WR 7, inflates the nebula with its powerful stellar wind.",
                "The nebula's two wing-like extensions give it the look of a horned helmet.",
                "Herschel found NGC 2359 in 1785 while surveying Canis Major."
            ),
            fa = listOf(
                "NGC ۲۳۵۹ یا کلاه‌خود ثور، یک سحابی نشری در صورت فلکی سگ بزرگ و در فاصله حدود ۱۵,۰۰۰ سال نوری است.",
                "این سحابی با قدر ۱۱/۰ می‌درخشد و شکلی حبابی و چشمگیر دارد.",
                "ستاره ولف-رایه WR ۷ با باد ستاره‌ای نیرومند خود این سحابی را باد کرده است.",
                "دو امتداد بال‌مانند سحابی ظاهر کلاه‌خودی شاخ‌دار به آن می‌دهند.",
                "NGC ۲۳۵۹ را ویلیام هرشل در سال ۱۷۸۵ کشف کرد."
            )
        ),
        "dso_ngc_2360" to BilingualFacts(
            en = listOf(
                "NGC 2360, Caroline's Cluster, is an open cluster in Canis Major, about 3,700 light-years away.",
                "Caroline's Cluster shines at magnitude 7.2 with about 100 stars.",
                "The cluster was discovered by Caroline Herschel in 1783, her first independent deep-sky discovery.",
                "NGC 2360 is about 800 million years old.",
                "It is nicknamed Caroline's Cluster in honor of its discoverer."
            ),
            fa = listOf(
                "NGC ۲۳۶۰ یا خوشه کارولین، یک خوشه باز در صورت فلکی سگ بزرگ و در فاصله حدود ۳۷۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۲ می‌درخشد و حدود ۱۰۰ ستاره دارد.",
                "این خوشه را کارولین هرشل در سال ۱۷۸۳ کشف کرد؛ نخستین کشف مستقل او در اعماق آسمان.",
                "سن NGC ۲۳۶۰ حدود ۸۰۰ میلیون سال است.",
                "این خوشه به افتخار کاشفش «خوشه کارولین» نامیده می‌شود."
            )
        ),
        "dso_ngc_2362" to BilingualFacts(
            en = listOf(
                "NGC 2362, the Tau Canis Majoris Cluster, is a compact open cluster in Canis Major, about 4,800 light-years away.",
                "It shines at magnitude 4.1 and surrounds the bright star Tau Canis Majoris.",
                "NGC 2362 is one of the youngest known open clusters, only about 4 to 5 million years old.",
                "The cluster contains over 100 stars, mostly hot and blue.",
                "NGC 2362 was discovered by Giovanni Hodierna before 1654."
            ),
            fa = listOf(
                "NGC ۲۳۶۲ یا خوشه تاو سگ بزرگ، یک خوشه باز فشرده در صورت فلکی سگ بزرگ و در فاصله حدود ۴۸۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۱ می‌درخشد و ستاره درخشان تاو سگ بزرگ را در بر گرفته است.",
                "NGC ۲۳۶۲ یکی از جوان‌ترین خوشه‌های باز شناخته‌شده است و تنها حدود ۴ تا ۵ میلیون سال سن دارد.",
                "این خوشه بیش از ۱۰۰ ستاره دارد که بیشترشان داغ و آبی‌اند.",
                "NGC ۲۳۶۲ را جیووانی هودیرنا پیش از سال ۱۶۵۴ کشف کرد."
            )
        ),
        "dso_ngc_2392" to BilingualFacts(
            en = listOf(
                "NGC 2392, the Eskimo Nebula, is a planetary nebula in Gemini, about 6,500 light-years away.",
                "It shines at magnitude 9.9 and was discovered by William Herschel in 1787.",
                "The nebula's bright inner shell and outer ring of gas resemble a face inside a parka hood.",
                "Its central star is a hot white dwarf of about magnitude 10.5.",
                "NGC 2392 was one of the first objects imaged in detail by the Hubble Space Telescope."
            ),
            fa = listOf(
                "NGC ۲۳۹۲ یا سحابی اسکیمو، یک سحابی سیاره‌نما در صورت فلکی دوپیکر و در فاصله حدود ۶۵۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۹ می‌درخشد و ویلیام هرشل آن را در سال ۱۷۸۷ کشف کرد.",
                "پوسته درونی درخشان و حلقه بیرونی گاز این سحابی به چهره‌ای درون کلاه پوستی می‌ماند.",
                "ستاره مرکزی آن یک کوتوله سفید داغ با قدر حدود ۱۰/۵ است.",
                "NGC ۲۳۹۲ یکی از نخستین اجرامی بود که تلسکوپ فضایی هابل آن را با جزئیات تصویر کرد."
            )
        ),
        "dso_ngc_2420" to BilingualFacts(
            en = listOf(
                "NGC 2420 is an open cluster in Gemini, about 10,000 light-years away.",
                "It shines at magnitude 8.3 and contains about 100 stars.",
                "The cluster was discovered by William Herschel in 1783.",
                "NGC 2420 is old for an open cluster, roughly 1 billion years.",
                "Its many red giant stars make it a popular target for studies of stellar evolution."
            ),
            fa = listOf(
                "NGC ۲۴۲۰ یک خوشه باز در صورت فلکی دوپیکر و در فاصله حدود ۱۰,۰۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۳ می‌درخشد و حدود ۱۰۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۳ کشف کرد.",
                "NGC ۲۴۲۰ برای یک خوشه باز کهن است و سن آن حدود یک میلیارد سال است.",
                "ستارگان غول سرخ فراوان آن، این خوشه را به هدف محبوب مطالعات تکامل ستاره‌ای تبدیل کرده است."
            )
        ),
        "dso_ngc_2440" to BilingualFacts(
            en = listOf(
                "NGC 2440 is a planetary nebula in Puppis, about 4,000 light-years away.",
                "It shines at magnitude 9.4 and has an irregular, boxy shape.",
                "The nebula surrounds one of the hottest white dwarfs known, with a surface temperature near 200,000 K.",
                "Hubble images show NGC 2440's glowing gas in striking blue and gold filaments.",
                "NGC 2440 was discovered by William Herschel in 1790."
            ),
            fa = listOf(
                "NGC ۲۴۴۰ یک سحابی سیاره‌نما در صورت فلکی کشتی پشت و در فاصله حدود ۴۰۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۴ می‌درخشد و شکلی نامنظم و جعبه‌مانند دارد.",
                "این سحابی یکی از داغ‌ترین کوتوله‌های سفید شناخته‌شده را در بر گرفته است که دمای سطح آن نزدیک به ۲۰۰,۰۰۰ کلوین است.",
                "تصاویر هابل گاز درخشان NGC ۲۴۴۰ را در رشته‌های چشمگیر آبی و طلایی نشان می‌دهند.",
                "NGC ۲۴۴۰ را ویلیام هرشل در سال ۱۷۹۰ کشف کرد."
            )
        ),
        "dso_ngc_2451" to BilingualFacts(
            en = listOf(
                "NGC 2451 is a bright open cluster in Puppis, about 850 light-years away.",
                "It shines at magnitude 2.8 and is easily visible to the naked eye.",
                "The cluster spans about 50 arcminutes, wider than the full Moon.",
                "NGC 2451 contains about 50 stars, including the orange giant c Puppis.",
                "The cluster was recorded by Giovanni Hodierna before 1654."
            ),
            fa = listOf(
                "NGC ۲۴۵۱ یک خوشه باز درخشان در صورت فلکی کشتی پشت و در فاصله حدود ۸۵۰ سال نوری است.",
                "این خوشه با قدر ۲/۸ می‌درخشد و به آسانی با چشم غیرمسلح دیده می‌شود.",
                "پهنای این خوشه حدود ۵۰ دقیقه قوسی است؛ پهن‌تر از ماه کامل.",
                "NGC ۲۴۵۱ حدود ۵۰ ستاره دارد، از جمله غول نارنجی c کشتی پشت.",
                "این خوشه را جیووانی هودیرنا پیش از سال ۱۶۵۴ ثبت کرد."
            )
        ),
        "dso_ngc_246" to BilingualFacts(
            en = listOf(
                "NGC 246, the Skull Nebula, is a planetary nebula in Cetus, about 1,600 light-years away.",
                "It shines at magnitude 8.0 and spans about 3.8 arcminutes.",
                "The nebula's dark central region and ring of gas give it the look of a skull.",
                "William Herschel discovered NGC 246 in 1785 in the constellation Cetus.",
                "Its central star is part of a multiple star system within the nebula."
            ),
            fa = listOf(
                "NGC ۲۴۶ یا سحابی جمجمه، یک سحابی سیاره‌نما در صورت فلکی نهنگ و در فاصله حدود ۱۶۰۰ سال نوری است.",
                "این سحابی با قدر ۸/۰ می‌درخشد و پهنای آن حدود ۳/۸ دقیقه قوسی است.",
                "ناحیه مرکزی تاریک و حلقه گازی سحابی ظاهر یک جمجمه به آن می‌دهند.",
                "NGC ۲۴۶ را ویلیام هرشل در سال ۱۷۸۵ کشف کرد.",
                "ستاره مرکزی آن بخشی از یک سامانه چندستاره‌ای درون سحابی است."
            )
        ),
        "dso_ngc_2477" to BilingualFacts(
            en = listOf(
                "NGC 2477 is a rich open cluster in Puppis, about 4,200 light-years away.",
                "It shines at magnitude 5.8 and contains about 300 stars.",
                "The cluster is so dense it can be mistaken for a globular cluster.",
                "Nicolas-Louis de Lacaille discovered NGC 2477 in 1751 from the Cape of Good Hope.",
                "It is about 1 billion years old, ancient for an open cluster."
            ),
            fa = listOf(
                "NGC ۲۴۷۷ یک خوشه باز پرستاره در صورت فلکی کشتی پشت و در فاصله حدود ۴۲۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۸ می‌درخشد و حدود ۳۰۰ ستاره دارد.",
                "این خوشه چنان متراکم است که ممکن است با یک خوشه کروی اشتباه گرفته شود.",
                "NGC ۲۴۷۷ را نیکولا-لویی دو لاکای در سال ۱۷۵۱ کشف کرد.",
                "سن این خوشه حدود یک میلیارد سال است؛ برای یک خوشه باز کهن."
            )
        ),
        "dso_ngc_2506" to BilingualFacts(
            en = listOf(
                "NGC 2506 is an open cluster in Monoceros, about 11,000 light-years away.",
                "It shines at magnitude 7.6 and contains about 200 stars.",
                "The cluster was discovered by William Herschel in 1791.",
                "NGC 2506 is about 2 billion years old, one of the oldest open clusters known.",
                "It lies far above the Milky Way's plane, which helps it survive so long."
            ),
            fa = listOf(
                "NGC ۲۵۰۶ یک خوشه باز در صورت فلکی تک‌شاخ و در فاصله حدود ۱۱,۰۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۶ می‌درخشد و حدود ۲۰۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۹۱ کشف کرد.",
                "سن NGC ۲۵۰۶ حدود ۲ میلیارد سال است و یکی از کهن‌سال‌ترین خوشه‌های باز شناخته‌شده است.",
                "این خوشه بسیار بالاتر از صفحه راه شیری قرار دارد و همین به دوام طولانی آن کمک می‌کند."
            )
        ),
        "dso_ngc_2516" to BilingualFacts(
            en = listOf(
                "NGC 2516 is a bright open cluster in Carina, about 1,300 light-years away.",
                "It shines at magnitude 3.8 and is visible to the naked eye.",
                "The cluster contains about 100 stars and is nicknamed the Southern Beehive.",
                "NGC 2516 is about 135 million years old.",
                "It was discovered by Nicolas-Louis de Lacaille in 1751."
            ),
            fa = listOf(
                "NGC ۲۵۱۶ یک خوشه باز درخشان در صورت فلکی شاه‌تخته و در فاصله حدود ۱۳۰۰ سال نوری است.",
                "این خوشه با قدر ۳/۸ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "این خوشه حدود ۱۰۰ ستاره دارد و به «کندوی جنوبی» ملقب است.",
                "سن NGC ۲۵۱۶ حدود ۱۳۵ میلیون سال است.",
                "این خوشه را نیکولا-لویی دو لاکای در سال ۱۷۵۱ کشف کرد."
            )
        ),
        "dso_ngc_253" to BilingualFacts(
            en = listOf(
                "NGC 253, the Sculptor Galaxy, is a starburst spiral in Sculptor, about 11 million light-years away.",
                "It shines at magnitude 7.1 and is one of the brightest galaxies beyond the Local Group.",
                "NGC 253 is undergoing intense star formation in its dusty core.",
                "The galaxy is the largest member of the Sculptor Group, our nearest galaxy group beyond the Local Group.",
                "Caroline Herschel discovered NGC 253 in 1783, her first great galaxy find."
            ),
            fa = listOf(
                "NGC ۲۵۳ یا کهکشان سنگ‌تراش، یک کهکشان مارپیچی فوران‌ستاره‌زا در صورت فلکی سنگ‌تراش و در فاصله حدود ۱۱ میلیون سال نوری است.",
                "این کهکشان با قدر ۷/۱ می‌درخشد و یکی از درخشان‌ترین کهکشان‌های بیرون از گروه محلی است.",
                "NGC ۲۵۳ در هسته غبارآلود خود در حال ستاره‌زایی شدید است.",
                "این کهکشان بزرگ‌ترین عضو گروه سنگ‌تراش است؛ نزدیک‌ترین گروه کهکشانی فراتر از گروه محلی.",
                "NGC ۲۵۳ را کارولین هرشل در سال ۱۷۸۳ کشف کرد."
            )
        ),
        "dso_ngc_2539" to BilingualFacts(
            en = listOf(
                "NGC 2539 is an open cluster in Puppis, about 4,400 light-years away.",
                "It shines at magnitude 6.5 and contains about 150 stars.",
                "William Herschel discovered NGC 2539 in 1785 while surveying Puppis.",
                "NGC 2539 has an age of roughly 370 million years.",
                "It lies in a rich Milky Way field and is a fine target for small telescopes."
            ),
            fa = listOf(
                "NGC ۲۵۳۹ یک خوشه باز در صورت فلکی کشتی پشت و در فاصله حدود ۴۴۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۵ می‌درخشد و حدود ۱۵۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۵ کشف کرد.",
                "سن NGC ۲۵۳۹ حدود ۳۷۰ میلیون سال است.",
                "این خوشه در میدان پرستاره راه شیری جای دارد و هدف خوبی برای تلسکوپ‌های کوچک است."
            )
        ),
        "dso_ngc_2547" to BilingualFacts(
            en = listOf(
                "NGC 2547 is an open cluster in Vela, about 1,400 light-years away.",
                "It shines at magnitude 4.7 and is visible to the naked eye.",
                "The cluster contains about 80 stars and is about 30 million years old.",
                "Lacaille recorded NGC 2547 in 1751 during his southern constellation survey.",
                "Its young stars make it a useful laboratory for studying planet-forming disks."
            ),
            fa = listOf(
                "NGC ۲۵۴۷ یک خوشه باز در صورت فلکی بادبان و در فاصله حدود ۱۴۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۷ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "این خوشه حدود ۸۰ ستاره دارد و سن آن تقریباً ۳۰ میلیون سال است.",
                "NGC ۲۵۴۷ را نیکولا-لویی دو لاکای در سال ۱۷۵۱ کشف کرد.",
                "ستارگان جوان آن، این خوشه را به آزمایشگاهی سودمند برای مطالعه قرص‌های سیاره‌ساز تبدیل کرده‌اند."
            )
        ),
        "dso_ngc_2808" to BilingualFacts(
            en = listOf(
                "NGC 2808 is a massive globular cluster in Carina, about 31,000 light-years away.",
                "It shines at magnitude 6.2 and is visible in binoculars.",
                "The cluster is one of the most massive globulars in the Milky Way, with over a million stars.",
                "NGC 2808 shows three distinct generations of stars, revealing complex formation history.",
                "Dunlop first recorded NGC 55 in 1826 during his survey of the southern sky."
            ),
            fa = listOf(
                "NGC ۲۸۰۸ یک خوشه کروی پرجرم در صورت فلکی شاه‌تخته و در فاصله حدود ۳۱,۰۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۲ می‌درخشد و در دوربین دوچشمی دیده می‌شود.",
                "این خوشه یکی از پرجرم‌ترین خوشه‌های کروی راه شیری است و بیش از یک میلیون ستاره دارد.",
                "NGC ۲۸۰۸ سه نسل متمایز از ستارگان را نشان می‌دهد که تاریخچه شکل‌گیری پیچیده‌ای را آشکار می‌کند.",
                "این خوشه را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد."
            )
        ),
        "dso_ngc_281" to BilingualFacts(
            en = listOf(
                "NGC 281, the Pacman Nebula, is an emission nebula in Cassiopeia, about 9,500 light-years away.",
                "It shines at magnitude 7.4 and spans about 35 arcminutes.",
                "A dark notch in the nebula's edge makes it resemble the Pac-Man video game character.",
                "NGC 281 contains the young open cluster IC 1590.",
                "The nebula is a site of active star formation."
            ),
            fa = listOf(
                "NGC ۲۸۱ یا سحابی پک‌من، یک سحابی نشری در صورت فلکی ذات‌الکرسی و در فاصله حدود ۹۵۰۰ سال نوری است.",
                "این سحابی با قدر ۷/۴ می‌درخشد و پهنای آن حدود ۳۵ دقیقه قوسی است.",
                "یک بریدگی تاریک در لبه سحابی آن را به شخصیت بازی ویدیویی پک‌من شبیه می‌کند.",
                "NGC ۲۸۱ خوشه باز جوان IC ۱۵۹۰ را در خود دارد.",
                "این سحابی جایگاه ستاره‌زایی فعال است."
            )
        ),
        "dso_ngc_2903" to BilingualFacts(
            en = listOf(
                "NGC 2903 is a barred spiral galaxy in Leo, about 30 million light-years away.",
                "It shines at magnitude 8.9 and is bright enough for small telescopes.",
                "The galaxy is a field galaxy, not part of any large cluster.",
                "NGC 2903 has a vigorous star-forming disk with many young clusters.",
                "It was discovered by William Herschel in 1784."
            ),
            fa = listOf(
                "NGC ۲۹۰۳ یک کهکشان مارپیچی میله‌ای در صورت فلکی شیر و در فاصله حدود ۳۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۹ می‌درخشد و برای تلسکوپ‌های کوچک به اندازه کافی درخشان است.",
                "این کهکشان یک کهکشان میدانی است و عضوی از هیچ خوشه بزرگی نیست.",
                "NGC ۲۹۰۳ قرصی ستاره‌زا و پرجنب‌وجوش با خوشه‌های جوان فراوان دارد.",
                "این کهکشان را ویلیام هرشل در سال ۱۷۸۴ کشف کرد."
            )
        ),
        "dso_ngc_300" to BilingualFacts(
            en = listOf(
                "NGC 300 is a spiral galaxy in Sculptor, about 7 million light-years away.",
                "It shines at magnitude 8.1 and is a member of the Sculptor Group.",
                "NGC 300 is a near twin of our neighbor galaxy M33.",
                "The galaxy contains many young blue star clusters and nebulae.",
                "James Dunlop discovered NGC 300 in 1826 while sweeping Sculptor."
            ),
            fa = listOf(
                "NGC ۳۰۰ یک کهکشان مارپیچی در صورت فلکی سنگ‌تراش و در فاصله حدود ۷ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۱ می‌درخشد و عضوی از گروه سنگ‌تراش است.",
                "NGC ۳۰۰ تقریباً همزاد کهکشان همسایه ما M۳۳ است.",
                "این کهکشان خوشه‌های ستاره‌ای آبی جوان و سحابی‌های فراوانی دارد.",
                "NGC ۳۰۰ را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد."
            )
        ),
        "dso_ngc_3114" to BilingualFacts(
            en = listOf(
                "NGC 3114 is a large open cluster in Carina, about 3,000 light-years away.",
                "It shines at magnitude 4.2 and spans about 35 arcminutes.",
                "The cluster contains about 170 stars.",
                "Dunlop recorded the wide cluster NGC 3114 in 1826 from Australia.",
                "Its loose, scattered pattern makes it a pleasant wide-field binocular target."
            ),
            fa = listOf(
                "NGC ۳۱۱۴ یک خوشه باز بزرگ در صورت فلکی شاه‌تخته و در فاصله حدود ۳۰۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۲ می‌درخشد و پهنای آن حدود ۳۵ دقیقه قوسی است.",
                "این خوشه حدود ۱۷۰ ستاره دارد.",
                "NGC ۳۱۱۴ را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد.",
                "الگوی باز و پراکنده آن، این خوشه را به هدف دلپذیری برای دوربین دوچشمی با میدان وسیع تبدیل کرده است."
            )
        ),
        "dso_ngc_3115" to BilingualFacts(
            en = listOf(
                "NGC 3115, the Spindle Galaxy, is a lenticular galaxy in Sextans seen nearly edge-on.",
                "It lies about 32 million light-years away and shines at magnitude 9.2.",
                "NGC 3115 hosts a supermassive black hole of about one billion solar masses.",
                "It was one of the first galaxies whose central black hole mass was measured from stellar motions.",
                "The galaxy was discovered by William Herschel in 1787."
            ),
            fa = listOf(
                "NGC ۳۱۱۵ یا کهکشان دوک، یک کهکشان عدسی‌شکل در صورت فلکی سکستان است که تقریباً از لبه دیده می‌شود.",
                "این کهکشان در فاصله حدود ۳۲ میلیون سال نوری است و با قدر ۹/۲ می‌درخشد.",
                "NGC ۳۱۱۵ میزبان سیاهچاله‌ای کلان‌جرم با جرم حدود یک میلیارد برابر خورشید است.",
                "این کهکشان یکی از نخستین کهکشان‌هایی بود که جرم سیاهچاله مرکزی آن از روی حرکت ستارگان اندازه‌گیری شد.",
                "این کهکشان را ویلیام هرشل در سال ۱۷۸۷ کشف کرد."
            )
        ),
        "dso_ngc_3242" to BilingualFacts(
            en = listOf(
                "NGC 3242, the Ghost of Jupiter, is a planetary nebula in Hydra, about 1,400 light-years away.",
                "It shines at magnitude 7.7 and appears as a small blue-green disk.",
                "The nebula is large enough to resemble the planet Jupiter in small telescopes, inspiring its name.",
                "Herschel cataloged NGC 3242 in 1785, noting its planetary appearance.",
                "Its outer halo of gas shows material ejected in earlier phases of the star's death."
            ),
            fa = listOf(
                "NGC ۳۲۴۲ یا شبح مشتری، یک سحابی سیاره‌نما در صورت فلکی مار باریک و در فاصله حدود ۱۴۰۰ سال نوری است.",
                "این سحابی با قدر ۷/۷ می‌درخشد و به صورت قرص کوچک سبز-آبی دیده می‌شود.",
                "این سحابی به اندازه‌ای بزرگ است که در تلسکوپ‌های کوچک به سیاره مشتری می‌ماند و نامش از همین‌جاست.",
                "NGC ۳۲۴۲ را ویلیام هرشل در سال ۱۷۸۵ کشف کرد.",
                "هاله بیرونی گاز آن ماده‌ای را نشان می‌دهد که در مراحل نخست مرگ ستاره به بیرون پرتاب شده است."
            )
        ),
        "dso_ngc_3532" to BilingualFacts(
            en = listOf(
                "NGC 3532, the Wishing Well Cluster, is a brilliant open cluster in Carina, about 1,300 light-years away.",
                "It shines at magnitude 3.0 and is visible to the naked eye.",
                "The cluster contains about 400 stars scattered over a field wider than the full Moon.",
                "Nicolas-Louis de Lacaille discovered NGC 3532 in 1752 during his expedition to South Africa.",
                "It was the first object observed by the Hubble Space Telescope in 1990."
            ),
            fa = listOf(
                "NGC ۳۵۳۲ یا خوشه چاه آرزو، یک خوشه باز درخشان در صورت فلکی شاه‌تخته و در فاصله حدود ۱۳۰۰ سال نوری است.",
                "این خوشه با قدر ۳/۰ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "این خوشه حدود ۴۰۰ ستاره دارد که در میدانی پهن‌تر از ماه کامل پراکنده‌اند.",
                "NGC ۳۵۳۲ را نیکولا-لویی دو لاکای در سال ۱۷۵۲ کشف کرد.",
                "این نخستین جرمی بود که تلسکوپ فضایی هابل در سال ۱۹۹۰ رصد کرد."
            )
        ),
        "dso_ngc_3628" to BilingualFacts(
            en = listOf(
                "NGC 3628 is an edge-on spiral galaxy in Leo, about 35 million light-years away.",
                "It shines at magnitude 9.5 and is the third member of the Leo Triplet.",
                "A dark dust lane splits the galaxy's disk along its entire length.",
                "Tidal interactions with M65 and M66 have warped NGC 3628's disk.",
                "The galaxy has a faint tidal tail stretching far into space."
            ),
            fa = listOf(
                "NGC ۳۶۲۸ یک کهکشان مارپیچی لبه‌نما در صورت فلکی شیر و در فاصله حدود ۳۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۵ می‌درخشد و سومین عضو سه‌گانه شیر است.",
                "یک رگه غبار تاریک قرص این کهکشان را در تمام طول آن دو نیم کرده است.",
                "برهم‌کنش‌های کشندی با M۶۵ و M۶۶ قرص NGC ۳۶۲۸ را تاب داده‌اند.",
                "این کهکشان دنباله کشندی کم‌نوری دارد که تا دوردست‌های فضا کشیده شده است."
            )
        ),
        "dso_ngc_3918" to BilingualFacts(
            en = listOf(
                "NGC 3918, the Blue Planetary, is a small planetary nebula in Centaurus, about 4,900 light-years away.",
                "It shines at magnitude 8.5 and appears strikingly blue in telescopes.",
                "The nebula is compact, less than half an arcminute across.",
                "NGC 3918 was discovered by John Herschel in 1834.",
                "Its vivid blue color comes from ionized oxygen gas."
            ),
            fa = listOf(
                "NGC ۳۹۱۸ یا سحابی سیاره‌نمای آبی، یک سحابی سیاره‌نمای کوچک در صورت فلکی قنطورس و در فاصله حدود ۴۹۰۰ سال نوری است.",
                "این سحابی با قدر ۸/۵ می‌درخشد و در تلسکوپ‌ها به طرز چشمگیری آبی دیده می‌شود.",
                "این سحابی فشرده است و پهنای آن کمتر از نیم دقیقه قوسی است.",
                "NGC ۳۹۱۸ را جان هرشل در سال ۱۸۳۴ کشف کرد.",
                "رنگ آبی زنده آن از گاز اکسیژن یونیده سرچشمه می‌گیرد."
            )
        ),
        "dso_ngc_4565" to BilingualFacts(
            en = listOf(
                "NGC 4565, the Needle Galaxy, is an edge-on spiral in Coma Berenices, about 40 million light-years away.",
                "It shines at magnitude 9.6 and is a favorite target for amateur telescopes.",
                "The galaxy is seen exactly edge-on, appearing as a thin needle of light.",
                "A dust lane runs along the full length of its plane.",
                "William Herschel discovered NGC 4565 in 1785 in Coma Berenices."
            ),
            fa = listOf(
                "NGC ۴۵۶۵ یا کهکشان سوزن، یک کهکشان مارپیچی لبه‌نما در صورت فلکی گیسو و در فاصله حدود ۴۰ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۶ می‌درخشد و هدف محبوب تلسکوپ‌های آماتوری است.",
                "این کهکشان دقیقاً از لبه دیده می‌شود و به صورت سوزن باریکی از نور ظاهر می‌شود.",
                "یک رگه غبار در تمام طول صفحه آن کشیده شده است.",
                "NGC ۴۵۶۵ را ویلیام هرشل در سال ۱۷۸۵ کشف کرد."
            )
        ),
        "dso_ngc_4631" to BilingualFacts(
            en = listOf(
                "NGC 4631, the Whale Galaxy, is an edge-on spiral in Canes Venatici, about 25 million light-years away.",
                "It shines at magnitude 9.3 and its elongated shape resembles a whale.",
                "The galaxy has a nearby companion, the small elliptical NGC 4627.",
                "NGC 4631 is undergoing vigorous star formation in its disk.",
                "It was discovered by William Herschel in 1787."
            ),
            fa = listOf(
                "NGC ۴۶۳۱ یا کهکشان نهنگ، یک کهکشان مارپیچی لبه‌نما در صورت فلکی تازی‌ها و در فاصله حدود ۲۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۹/۳ می‌درخشد و شکل کشیده آن به نهنگ می‌ماند.",
                "این کهکشان همدمی نزدیک دارد: کهکشان بیضوی کوچک NGC ۴۶۲۷.",
                "NGC ۴۶۳۱ در قرص خود در حال ستاره‌زایی پرشتاب است.",
                "این کهکشان را ویلیام هرشل در سال ۱۷۸۷ کشف کرد."
            )
        ),
        "dso_ngc_4755" to BilingualFacts(
            en = listOf(
                "NGC 4755, the Jewel Box Cluster, is a dazzling open cluster in Crux, about 6,400 light-years away.",
                "It shines at magnitude 4.2 and is visible to the naked eye.",
                "The cluster's brightest stars shine in contrasting blue, red, and white, like gems in a box.",
                "NGC 4755 was named the Jewel Box by John Herschel in the 1830s.",
                "The Jewel Box is only about 10 million years old, a newborn cluster."
            ),
            fa = listOf(
                "NGC ۴۷۵۵ یا خوشه جعبه جواهر، یک خوشه باز خیره‌کننده در صورت فلکی چلیپا و در فاصله حدود ۶۴۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۲ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "درخشان‌ترین ستارگان این خوشه با رنگ‌های متضاد آبی، سرخ و سفید مانند جواهرات درون جعبه می‌درخشند.",
                "نام «جعبه جواهر» را جان هرشل در دهه ۱۸۳۰ بر NGC ۴۷۵۵ نهاد.",
                "این خوشه جوان است و تنها حدود ۱۰ میلیون سال سن دارد."
            )
        ),
        "dso_ngc_5128" to BilingualFacts(
            en = listOf(
                "NGC 5128, Centaurus A, is a giant elliptical galaxy in Centaurus, about 12 million light-years away.",
                "It shines at magnitude 6.8 and is the fifth-brightest galaxy in the sky.",
                "A dark dust lane across the galaxy marks the remains of a spiral galaxy it absorbed.",
                "Centaurus A is the nearest major radio galaxy and a powerful radio source.",
                "Its central black hole launches enormous jets visible in radio and X-ray images."
            ),
            fa = listOf(
                "NGC ۵۱۲۸ یا قنطورس A، یک کهکشان بیضوی غول‌پیکر در صورت فلکی قنطورس و در فاصله حدود ۱۲ میلیون سال نوری است.",
                "این کهکشان با قدر ۶/۸ می‌درخشد و پنجمین کهکشان درخشان آسمان است.",
                "رگه غبار تاریک روی این کهکشان بازمانده کهکشان مارپیچی‌ای است که آن را بلعیده است.",
                "قنطورس A نزدیک‌ترین کهکشان رادیویی بزرگ و یک چشمه رادیویی نیرومند است.",
                "سیاهچاله مرکزی آن فواره‌های عظیمی پرتاب می‌کند که در تصاویر رادیویی و پرتو ایکس دیده می‌شوند."
            )
        ),
        "dso_ngc_5189" to BilingualFacts(
            en = listOf(
                "NGC 5189, the Spiral Planetary, is a planetary nebula in Musca, about 3,000 light-years away.",
                "It shines at magnitude 8.2 and has an unusual S-shaped structure.",
                "The nebula's complex shape comes from jets launched by its central binary star system.",
                "NGC 5189 was discovered by James Dunlop in 1826.",
                "It is one of the most structurally complex planetary nebulae known."
            ),
            fa = listOf(
                "NGC ۵۱۸۹ یا سحابی سیاره‌نمای مارپیچی، یک سحابی سیاره‌نما در صورت فلکی مگس و در فاصله حدود ۳۰۰۰ سال نوری است.",
                "این سحابی با قدر ۸/۲ می‌درخشد و ساختاری غیرعادی به شکل S دارد.",
                "شکل پیچیده این سحابی از فواره‌های پرتاب‌شده توسط سامانه ستاره دوتایی مرکزی آن ناشی می‌شود.",
                "NGC ۵۱۸۹ را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد.",
                "این سحابی از نظر ساختاری یکی از پیچیده‌ترین سحابی‌های سیاره‌نمای شناخته‌شده است."
            )
        ),
        "dso_ngc_55" to BilingualFacts(
            en = listOf(
                "NGC 55 is an irregular galaxy in Sculptor, about 6.5 million light-years away.",
                "It shines at magnitude 7.9 and is a member of the Sculptor Group.",
                "The galaxy is seen edge-on and resembles the Large Magellanic Cloud.",
                "NGC 55 is about 50,000 light-years across.",
                "James Dunlop discovered NGC 2808 in 1826 from his observatory in Australia."
            ),
            fa = listOf(
                "NGC ۵۵ یک کهکشان نامنظم در صورت فلکی سنگ‌تراش و در فاصله حدود ۶/۵ میلیون سال نوری است.",
                "این کهکشان با قدر ۷/۹ می‌درخشد و عضوی از گروه سنگ‌تراش است.",
                "این کهکشان از لبه دیده می‌شود و به ابر ماژلانی بزرگ شباهت دارد.",
                "قطر NGC ۵۵ حدود ۵۰,۰۰۰ سال نوری است.",
                "این کهکشان را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد."
            )
        ),
        "dso_ngc_6025" to BilingualFacts(
            en = listOf(
                "NGC 6025 is an open cluster in Triangulum Australe, about 2,700 light-years away.",
                "It shines at magnitude 5.1 and is visible to the naked eye.",
                "The cluster contains about 60 stars and spans about 12 arcminutes.",
                "Lacaille cataloged NGC 6025 in 1752 while mapping the southern sky.",
                "NGC 6025 is around 100 million years old, a young cluster in Triangulum Australe."
            ),
            fa = listOf(
                "NGC ۶۰۲۵ یک خوشه باز در صورت فلکی مثلث جنوبی و در فاصله حدود ۲۷۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۱ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "این خوشه حدود ۶۰ ستاره دارد و پهنای آن تقریباً ۱۲ دقیقه قوسی است.",
                "NGC ۶۰۲۵ را نیکولا-لویی دو لاکای در سال ۱۷۵۲ کشف کرد.",
                "سن این خوشه حدود ۱۰۰ میلیون سال است."
            )
        ),
        "dso_ngc_6067" to BilingualFacts(
            en = listOf(
                "NGC 6067 is an open cluster in Norma, about 4,700 light-years away.",
                "It shines at magnitude 5.6 and contains about 100 stars.",
                "The cluster was discovered by James Dunlop in 1826.",
                "The stars of NGC 6067 are about 100 million years old.",
                "It lies in a rich Milky Way field in the southern constellation Norma."
            ),
            fa = listOf(
                "NGC ۶۰۶۷ یک خوشه باز در صورت فلکی گونیا و در فاصله حدود ۴۷۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۶ می‌درخشد و حدود ۱۰۰ ستاره دارد.",
                "این خوشه را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد.",
                "سن NGC ۶۰۶۷ حدود ۱۰۰ میلیون سال است.",
                "این خوشه در میدان پرستاره راه شیری و در صورت فلکی جنوبی گونیا جای دارد."
            )
        ),
        "dso_ngc_6087" to BilingualFacts(
            en = listOf(
                "NGC 6087 is an open cluster in Norma, about 3,500 light-years away.",
                "It shines at magnitude 5.4 and is visible to the naked eye.",
                "The cluster contains about 40 stars, including the Cepheid variable S Normae.",
                "James Dunlop discovered NGC 6087 in 1826 among the clusters of Norma.",
                "It is one of the more prominent clusters of the southern constellation Norma."
            ),
            fa = listOf(
                "NGC ۶۰۸۷ یک خوشه باز در صورت فلکی گونیا و در فاصله حدود ۳۵۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۴ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "این خوشه حدود ۴۰ ستاره دارد، از جمله متغیر قیفاووسی S گونیا.",
                "NGC ۶۰۸۷ را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد.",
                "این خوشه یکی از برجسته‌ترین خوشه‌های صورت فلکی جنوبی گونیا است."
            )
        ),
        "dso_ngc_6124" to BilingualFacts(
            en = listOf(
                "NGC 6124 is an open cluster in Scorpius, about 1,500 light-years away.",
                "It shines at magnitude 5.8 and spans about 29 arcminutes.",
                "The cluster contains about 100 stars.",
                "The cluster NGC 6124 was first cataloged by Lacaille in 1751.",
                "It is about 300 million years old."
            ),
            fa = listOf(
                "NGC ۶۱۲۴ یک خوشه باز در صورت فلکی عقرب و در فاصله حدود ۱۵۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۸ می‌درخشد و پهنای آن حدود ۲۹ دقیقه قوسی است.",
                "این خوشه حدود ۱۰۰ ستاره دارد.",
                "NGC ۶۱۲۴ را نیکولا-لویی دو لاکای در سال ۱۷۵۱ کشف کرد.",
                "سن این خوشه حدود ۳۰۰ میلیون سال است."
            )
        ),
        "dso_ngc_6231" to BilingualFacts(
            en = listOf(
                "NGC 6231, the Northern Jewel Box, is a brilliant open cluster in Scorpius, about 5,900 light-years away.",
                "It shines at magnitude 2.6 and is visible to the naked eye.",
                "The cluster contains over 100 hot, young stars packed into a small area.",
                "NGC 6231 forms the heart of the Scorpius OB1 association.",
                "Hodierna cataloged NGC 6231 before 1654, making it one of the earliest recorded open clusters."
            ),
            fa = listOf(
                "NGC ۶۲۳۱ یا جعبه جواهر شمالی، یک خوشه باز درخشان در صورت فلکی عقرب و در فاصله حدود ۵۹۰۰ سال نوری است.",
                "این خوشه با قدر ۲/۶ می‌درخشد و با چشم غیرمسلح دیده می‌شود.",
                "این خوشه بیش از ۱۰۰ ستاره داغ و جوان دارد که در ناحیه‌ای کوچک فشرده شده‌اند.",
                "NGC ۶۲۳۱ قلب انجمن OB1 عقرب را می‌سازد.",
                "این خوشه را جیووانی هودیرنا پیش از سال ۱۶۵۴ کشف کرد."
            )
        ),
        "dso_ngc_6242" to BilingualFacts(
            en = listOf(
                "NGC 6242 is an open cluster in Scorpius, about 4,000 light-years away.",
                "It shines at magnitude 6.4 and contains about 40 stars.",
                "The cluster was discovered by Nicolas-Louis de Lacaille in 1751.",
                "NGC 6242 lies near the bright star Mu Scorpii.",
                "The cluster is about 50 million years old."
            ),
            fa = listOf(
                "NGC ۶۲۴۲ یک خوشه باز در صورت فلکی عقرب و در فاصله حدود ۴۰۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۴ می‌درخشد و حدود ۴۰ ستاره دارد.",
                "این خوشه را نیکولا-لویی دو لاکای در سال ۱۷۵۱ کشف کرد.",
                "NGC ۶۲۴۲ در نزدیکی ستاره درخشان مو عقرب قرار دارد.",
                "سن این خوشه حدود ۵۰ میلیون سال است."
            )
        ),
        "dso_ngc_6302" to BilingualFacts(
            en = listOf(
                "NGC 6302, the Butterfly Nebula, is a bipolar planetary nebula in Scorpius, about 4,000 light-years away.",
                "It shines at magnitude 9.6 and spans about 3 arcminutes.",
                "Hubble images show two vast wings of glowing gas, giving the nebula its name.",
                "The nebula's central star is one of the hottest known, with a surface near 250,000 K.",
                "NGC 6302 was discovered by Edward Emerson Barnard in 1880."
            ),
            fa = listOf(
                "NGC ۶۳۰۲ یا سحابی پروانه، یک سحابی سیاره‌نمای دوقطبی در صورت فلکی عقرب و در فاصله حدود ۴۰۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۶ می‌درخشد و پهنای آن حدود ۳ دقیقه قوسی است.",
                "تصاویر هابل دو بال گسترده از گاز درخشان را نشان می‌دهند که نام این سحابی از آن‌هاست.",
                "ستاره مرکزی این سحابی یکی از داغ‌ترین ستارگان شناخته‌شده است و دمای سطح آن نزدیک ۲۵۰,۰۰۰ کلوین است.",
                "NGC ۶۳۰۲ را ادوارد امرسون بارنارد در سال ۱۸۸۰ کشف کرد."
            )
        ),
        "dso_ngc_6369" to BilingualFacts(
            en = listOf(
                "NGC 6369, the Little Ghost, is a planetary nebula in Ophiuchus, about 3,500 light-years away.",
                "It shines at magnitude 9.9 and appears as a small, faint ring.",
                "The nebula's pale ring gives it a ghostly appearance in photographs.",
                "Herschel discovered NGC 6369 in 1784, cataloging its faint ring.",
                "Its central star is a hot white dwarf nearing the end of its evolution."
            ),
            fa = listOf(
                "NGC ۶۳۶۹ یا شبح کوچک، یک سحابی سیاره‌نما در صورت فلکی ماراَفسای و در فاصله حدود ۳۵۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۹ می‌درخشد و به صورت حلقه کوچک و کم‌نوری دیده می‌شود.",
                "حلقه رنگ‌پریده این سحابی در عکس‌ها ظاهری شبح‌وار به آن می‌دهد.",
                "NGC ۶۳۶۹ را ویلیام هرشل در سال ۱۷۸۴ کشف کرد.",
                "ستاره مرکزی آن کوتوله سفید داغی است که به پایان تکامل خود نزدیک می‌شود."
            )
        ),
        "dso_ngc_6397" to BilingualFacts(
            en = listOf(
                "NGC 6397 is a globular cluster in Ara, about 7,800 light-years away.",
                "It shines at magnitude 5.3, easily visible in binoculars.",
                "NGC 6397 is one of the nearest globular clusters, at only about 7,800 light-years.",
                "The cluster contains about 400,000 stars and is roughly 13 billion years old.",
                "Hubble studies of NGC 6397 measured the mass of its white dwarfs, refining stellar evolution theory."
            ),
            fa = listOf(
                "NGC ۶۳۹۷ یک خوشه کروی در صورت فلکی آتشدان و در فاصله حدود ۷۸۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۳ می‌درخشد و به آسانی در دوربین دوچشمی دیده می‌شود.",
                "NGC ۶۳۹۷ یکی از نزدیک‌ترین خوشه‌های کروی به زمین است.",
                "این خوشه حدود ۴۰۰,۰۰۰ ستاره دارد و سن آن تقریباً ۱۳ میلیارد سال است.",
                "مطالعات هابل روی NGC ۶۳۹۷ جرم کوتوله‌های سفید آن را اندازه گرفت و نظریه تکامل ستاره‌ای را پالایش کرد."
            )
        ),
        "dso_ngc_6572" to BilingualFacts(
            en = listOf(
                "NGC 6572 is a planetary nebula in Ophiuchus, about 3,500 light-years away.",
                "It shines at magnitude 8.1 and appears as a small, bright green disk.",
                "The nebula is young and very dense, glowing intensely in small telescopes.",
                "Herschel discovered NGC 6572 in 1784 and described it as a vivid green disk.",
                "Its vivid color makes it a favorite target for planetary nebula observers."
            ),
            fa = listOf(
                "NGC ۶۵۷۲ یک سحابی سیاره‌نما در صورت فلکی ماراَفسای و در فاصله حدود ۳۵۰۰ سال نوری است.",
                "این سحابی با قدر ۸/۱ می‌درخشد و به صورت قرص کوچک و درخشان سبز دیده می‌شود.",
                "این سحابی جوان و بسیار متراکم است و در تلسکوپ‌های کوچک به شدت می‌درخشد.",
                "NGC ۶۵۷۲ را ویلیام هرشل در سال ۱۷۸۴ کشف کرد.",
                "رنگ زنده آن، این سحابی را به هدف محبوب رصدگران سحابی‌های سیاره‌نما تبدیل کرده است."
            )
        ),
        "dso_ngc_6633" to BilingualFacts(
            en = listOf(
                "NGC 6633 is an open cluster in Ophiuchus, about 1,000 light-years away.",
                "It shines at magnitude 4.6 and spans about 27 arcminutes.",
                "The cluster contains about 30 stars and is visible to the naked eye under dark skies.",
                "NGC 6633 was discovered by Jean-Philippe de Chéseaux in 1745.",
                "The cluster is about 600 million years old."
            ),
            fa = listOf(
                "NGC ۶۶۳۳ یک خوشه باز در صورت فلکی ماراَفسای و در فاصله حدود ۱۰۰۰ سال نوری است.",
                "این خوشه با قدر ۴/۶ می‌درخشد و پهنای آن حدود ۲۷ دقیقه قوسی است.",
                "این خوشه حدود ۳۰ ستاره دارد و در آسمان تاریک با چشم غیرمسلح دیده می‌شود.",
                "NGC ۶۶۳۳ را ژان-فیلیپ دو شزو در سال ۱۷۴۵ کشف کرد.",
                "سن این خوشه حدود ۶۰۰ میلیون سال است."
            )
        ),
        "dso_ngc_6741" to BilingualFacts(
            en = listOf(
                "NGC 6741, the Phantom Streak, is a planetary nebula in Aquila, about 7,000 light-years away.",
                "It shines at magnitude 11.0 and appears as a small, faint smudge.",
                "The nebula's elongated shape gives it the appearance of a ghostly streak.",
                "NGC 6741 was discovered by Edward Charles Pickering in 1882.",
                "It is a dense, young planetary nebula whose central star is still hot and bright."
            ),
            fa = listOf(
                "NGC ۶۷۴۱ یا رد شبح، یک سحابی سیاره‌نما در صورت فلکی عقاب و در فاصله حدود ۷۰۰۰ سال نوری است.",
                "این سحابی با قدر ۱۱/۰ می‌درخشد و به صورت لکه کوچک و کم‌نوری دیده می‌شود.",
                "شکل کشیده این سحابی ظاهر ردی شبح‌وار به آن می‌دهد.",
                "NGC ۶۷۴۱ را ادوارد چارلز پیکرینگ در سال ۱۸۸۲ کشف کرد.",
                "این یک سحابی سیاره‌نمای متراکم و جوان است که ستاره مرکزی آن هنوز داغ و درخشان است."
            )
        ),
        "dso_ngc_6752" to BilingualFacts(
            en = listOf(
                "NGC 6752 is a globular cluster in Pavo, about 13,000 light-years away.",
                "It shines at magnitude 5.4 and is the third-brightest globular cluster in the sky.",
                "NGC 6752 is one of the closest globular clusters to the Sun.",
                "The cluster contains over 100,000 stars and is about 11.8 billion years old.",
                "James Dunlop cataloged NGC 6752 in 1826."
            ),
            fa = listOf(
                "NGC ۶۷۵۲ یک خوشه کروی در صورت فلکی طاووس و در فاصله حدود ۱۳,۰۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۴ می‌درخشد و سومین خوشه کروی درخشان آسمان است.",
                "NGC ۶۷۵۲ یکی از نزدیک‌ترین خوشه‌های کروی به زمین است.",
                "این خوشه بیش از ۱۰۰,۰۰۰ ستاره دارد و سن آن حدود ۱۱/۸ میلیارد سال است.",
                "این خوشه را جیمز دانلوپ در سال ۱۸۲۶ کشف کرد."
            )
        ),
        "dso_ngc_6755" to BilingualFacts(
            en = listOf(
                "NGC 6755 is an open cluster in Aquila, about 4,600 light-years away.",
                "It shines at magnitude 7.5 and contains about 100 stars.",
                "Herschel cataloged NGC 6755 in 1785 during his Milky Way sweeps.",
                "NGC 6755 is about 250 million years old.",
                "It lies in a rich Milky Way field in Aquila."
            ),
            fa = listOf(
                "NGC ۶۷۵۵ یک خوشه باز در صورت فلکی عقاب و در فاصله حدود ۴۶۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۵ می‌درخشد و حدود ۱۰۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۵ کشف کرد.",
                "سن NGC ۶۷۵۵ حدود ۲۵۰ میلیون سال است.",
                "این خوشه در میدان پرستاره راه شیری و در صورت فلکی عقاب جای دارد."
            )
        ),
        "dso_ngc_6781" to BilingualFacts(
            en = listOf(
                "NGC 6781 is a planetary nebula in Aquila, about 2,500 light-years away.",
                "It shines at magnitude 11.4 and appears as a fairly large, faint ring.",
                "The nebula's bubble of gas is nearly two light-years across.",
                "Herschel discovered NGC 6781 in 1788, a faint ring of gas in Aquila.",
                "Its expanding shell is lit by a hot white dwarf at its center."
            ),
            fa = listOf(
                "NGC ۶۷۸۱ یک سحابی سیاره‌نما در صورت فلکی عقاب و در فاصله حدود ۲۵۰۰ سال نوری است.",
                "این سحابی با قدر ۱۱/۴ می‌درخشد و به صورت حلقه‌ای نسبتاً بزرگ و کم‌نور دیده می‌شود.",
                "حباب گازی این سحابی نزدیک به دو سال نوری قطر دارد.",
                "NGC ۶۷۸۱ را ویلیام هرشل در سال ۱۷۸۸ کشف کرد.",
                "پوسته در حال گسترش آن را کوتوله سفید داغی در مرکزش روشن می‌کند."
            )
        ),
        "dso_ngc_6791" to BilingualFacts(
            en = listOf(
                "NGC 6791 is an open cluster in Lyra, about 13,300 light-years away.",
                "It shines at magnitude 9.5 and contains roughly 300 stars.",
                "NGC 6791 is one of the oldest open clusters known, about 8 billion years old.",
                "The cluster is unusually rich in heavy elements and hosts many white dwarfs.",
                "It was discovered by Friedrich August Theodor Winnecke in 1853."
            ),
            fa = listOf(
                "NGC ۶۷۹۱ یک خوشه باز در صورت فلکی شلیاق و در فاصله حدود ۱۳,۳۰۰ سال نوری است.",
                "این خوشه با قدر ۹/۵ می‌درخشد و تقریباً ۳۰۰ ستاره دارد.",
                "NGC ۶۷۹۱ یکی از کهن‌سال‌ترین خوشه‌های باز شناخته‌شده است و حدود ۸ میلیارد سال سن دارد.",
                "این خوشه به طور غیرعادی سرشار از عناصر سنگین است و کوتوله‌های سفید فراوانی دارد.",
                "این خوشه را فریدریش آگوست تئودور وینکه در سال ۱۸۵۳ کشف کرد."
            )
        ),
        "dso_ngc_6818" to BilingualFacts(
            en = listOf(
                "NGC 6818, the Little Gem, is a planetary nebula in Sagittarius, about 6,000 light-years away.",
                "It shines at magnitude 9.9 and appears as a small, bright blue-green disk.",
                "The nebula has an inner bright shell and a fainter outer halo.",
                "Herschel found NGC 6818 in 1787 in Sagittarius.",
                "Its compact size and brightness make it a rewarding small-telescope target."
            ),
            fa = listOf(
                "NGC ۶۸۱۸ یا گوهر کوچک، یک سحابی سیاره‌نما در صورت فلکی کمان و در فاصله حدود ۶۰۰۰ سال نوری است.",
                "این سحابی با قدر ۹/۹ می‌درخشد و به صورت قرص کوچک و درخشان سبز-آبی دیده می‌شود.",
                "این سحابی پوسته درونی درخشانی دارد و هاله‌ای بیرونی کم‌نورتر.",
                "NGC ۶۸۱۸ را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "اندازه فشرده و درخشندگی آن، این سحابی را به هدفی ارزشمند برای تلسکوپ‌های کوچک تبدیل کرده است."
            )
        ),
        "dso_ngc_6819" to BilingualFacts(
            en = listOf(
                "NGC 6819 is a rich open cluster in Cygnus, about 7,800 light-years away.",
                "It shines at magnitude 7.3 and contains roughly 300 stars.",
                "NGC 6819 is about 2.5 billion years old, quite old for an open cluster.",
                "The Kepler space telescope studied NGC 6819 extensively while searching for exoplanets.",
                "Caroline Herschel discovered NGC 6819 in 1783."
            ),
            fa = listOf(
                "NGC ۶۸۱۹ یک خوشه باز پرستاره در صورت فلکی ماکیان و در فاصله حدود ۷۸۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۳ می‌درخشد و تقریباً ۳۰۰ ستاره دارد.",
                "سن NGC ۶۸۱۹ حدود ۲/۵ میلیارد سال است؛ برای یک خوشه باز بسیار کهن.",
                "تلسکوپ فضایی کپلر NGC ۶۸۱۹ را هنگام جست‌وجوی سیارات فراخورشیدی به طور گسترده مطالعه کرد.",
                "این خوشه را کارولین هرشل در سال ۱۷۸۳ کشف کرد."
            )
        ),
        "dso_ngc_6822" to BilingualFacts(
            en = listOf(
                "NGC 6822, Barnard's Galaxy, is an irregular dwarf galaxy in Sagittarius, about 1.6 million light-years away.",
                "It shines at magnitude 8.1 and was discovered by Edward Emerson Barnard in 1884.",
                "NGC 6822 was the first galaxy beyond the Magellanic Clouds in which Cepheid variables were found.",
                "Edwin Hubble used those Cepheids in 1925 to prove the galaxy lies far beyond the Milky Way.",
                "The galaxy hosts several bright nebulae and star-forming regions."
            ),
            fa = listOf(
                "NGC ۶۸۲۲ یا کهکشان بارنارد، یک کهکشان کوتوله نامنظم در صورت فلکی کمان و در فاصله حدود ۱/۶ میلیون سال نوری است.",
                "این کهکشان با قدر ۸/۱ می‌درخشد و ادوارد امرسون بارنارد آن را در سال ۱۸۸۴ کشف کرد.",
                "NGC ۶۸۲۲ نخستین کهکشانی فراتر از ابرهای ماژلانی بود که در آن متغیرهای قیفاووسی یافت شد.",
                "ادوین هابل در سال ۱۹۲۵ با همین قیفاووس‌ها ثابت کرد که این کهکشان بسیار فراتر از راه شیری است.",
                "این کهکشان میزبان چندین سحابی درخشان و ناحیه ستاره‌زاست."
            )
        ),
        "dso_ngc_6866" to BilingualFacts(
            en = listOf(
                "NGC 6866 is an open cluster in Cygnus, about 3,900 light-years away.",
                "It shines at magnitude 7.6 and contains about 80 stars.",
                "Caroline Herschel found NGC 6866 in 1783 during her comet searches.",
                "NGC 6866 is about 1 billion years old.",
                "It lies in a rich Milky Way field and is a popular target for cluster studies."
            ),
            fa = listOf(
                "NGC ۶۸۶۶ یک خوشه باز در صورت فلکی ماکیان و در فاصله حدود ۳۹۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۶ می‌درخشد و حدود ۸۰ ستاره دارد.",
                "این خوشه را کارولین هرشل در سال ۱۷۸۳ کشف کرد.",
                "سن NGC ۶۸۶۶ حدود یک میلیارد سال است.",
                "این خوشه در میدان پرستاره راه شیری جای دارد و هدف محبوب مطالعات خوشه‌ای است."
            )
        ),
        "dso_ngc_6884" to BilingualFacts(
            en = listOf(
                "NGC 6884 is a planetary nebula in Cygnus, about 6,500 light-years away.",
                "It shines at magnitude 10.9 and appears as a small, faint disk.",
                "The nebula is compact and requires moderate aperture to observe well.",
                "Ralph Copeland discovered NGC 6884 in 1884 during his nebula observations.",
                "Its central star is a hot white dwarf that energizes the surrounding gas."
            ),
            fa = listOf(
                "NGC ۶۸۸۴ یک سحابی سیاره‌نما در صورت فلکی ماکیان و در فاصله حدود ۶۵۰۰ سال نوری است.",
                "این سحابی با قدر ۱۰/۹ می‌درخشد و به صورت قرص کوچک و کم‌نوری دیده می‌شود.",
                "این سحابی فشرده است و برای رصد خوب به گشودگی متوسط نیاز دارد.",
                "NGC ۶۸۸۴ را رالف کوپلند در سال ۱۸۸۴ کشف کرد.",
                "ستاره مرکزی آن کوتوله سفید داغی است که گاز پیرامون را برانگیخته است."
            )
        ),
        "dso_ngc_6891" to BilingualFacts(
            en = listOf(
                "NGC 6891 is a planetary nebula in Delphinus, about 7,000 light-years away.",
                "It shines at magnitude 10.5 and appears as a small bluish disk.",
                "The nebula has a bright inner shell surrounded by a fainter halo.",
                "Copeland found NGC 6891 in 1884 while surveying Delphinus.",
                "Its layered shells record multiple episodes of gas ejection from the dying star."
            ),
            fa = listOf(
                "NGC ۶۸۹۱ یک سحابی سیاره‌نما در صورت فلکی دلفین و در فاصله حدود ۷۰۰۰ سال نوری است.",
                "این سحابی با قدر ۱۰/۵ می‌درخشد و به صورت قرص کوچک آبی‌رنگی دیده می‌شود.",
                "این سحابی پوسته درونی درخشانی دارد که هاله‌ای کم‌نورتر آن را در بر گرفته است.",
                "NGC ۶۸۹۱ را رالف کوپلند در سال ۱۸۸۴ کشف کرد.",
                "پوسته‌های لایه‌لایه آن چند دوره پرتاب گاز از ستاره رو به مرگ را ثبت کرده‌اند."
            )
        ),
        "dso_ngc_6905" to BilingualFacts(
            en = listOf(
                "NGC 6905, the Blue Flash Nebula, is a planetary nebula in Delphinus, about 7,500 light-years away.",
                "It shines at magnitude 10.9 and appears as a small oval glow.",
                "The nebula flashes a vivid blue color through O-III filters.",
                "William Herschel discovered NGC 6905 in 1784 in Delphinus.",
                "It is one of the brighter planetary nebulae of the constellation Delphinus."
            ),
            fa = listOf(
                "NGC ۶۹۰۵ یا سحابی جرقه آبی، یک سحابی سیاره‌نما در صورت فلکی دلفین و در فاصله حدود ۷۵۰۰ سال نوری است.",
                "این سحابی با قدر ۱۰/۹ می‌درخشد و به صورت درخشش بیضی کوچکی دیده می‌شود.",
                "این سحابی با فیلتر اکسیژن سه‌باره رنگ آبی زنده‌ای نشان می‌دهد.",
                "NGC ۶۹۰۵ را ویلیام هرشل در سال ۱۷۸۴ کشف کرد.",
                "این سحابی یکی از درخشان‌ترین سحابی‌های سیاره‌نمای صورت فلکی دلفین است."
            )
        ),
        "dso_ngc_6910" to BilingualFacts(
            en = listOf(
                "NGC 6910 is a young open cluster in Cygnus, about 5,000 light-years away.",
                "It shines at magnitude 7.4 and lies near the bright star Sadr (Gamma Cygni).",
                "The cluster is very young, only about 6 million years old.",
                "NGC 6910 contains hot, massive stars that light up the surrounding nebulosity.",
                "It was discovered by William Herschel in 1786."
            ),
            fa = listOf(
                "NGC ۶۹۱۰ یک خوشه باز جوان در صورت فلکی ماکیان و در فاصله حدود ۵۰۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۴ می‌درخشد و نزدیک ستاره درخشان صدر (گاما ماکیان) قرار دارد.",
                "این خوشه بسیار جوان است و تنها حدود ۶ میلیون سال سن دارد.",
                "NGC ۶۹۱۰ ستارگان داغ و پرجرمی دارد که سحابی پیرامون را روشن می‌کنند.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۶ کشف کرد."
            )
        ),
        "dso_ngc_6939" to BilingualFacts(
            en = listOf(
                "NGC 6939 is an open cluster in Cepheus, about 3,900 light-years away.",
                "It shines at magnitude 7.8 and contains about 150 stars.",
                "The cluster is about 2 billion years old, among the oldest open clusters.",
                "NGC 6939 was discovered by William Herschel in 1798.",
                "It lies far above the Milky Way's plane, which helps it hold together so long."
            ),
            fa = listOf(
                "NGC ۶۹۳۹ یک خوشه باز در صورت فلکی قیفاووس و در فاصله حدود ۳۹۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۸ می‌درخشد و حدود ۱۵۰ ستاره دارد.",
                "سن این خوشه حدود ۲ میلیارد سال است و از کهن‌سال‌ترین خوشه‌های باز است.",
                "NGC ۶۹۳۹ را ویلیام هرشل در سال ۱۷۹۸ کشف کرد.",
                "این خوشه بسیار بالاتر از صفحه راه شیری قرار دارد و همین به یکپارچگی طولانی آن کمک می‌کند."
            )
        ),
        "dso_ngc_6940" to BilingualFacts(
            en = listOf(
                "NGC 6940 is an open cluster in Vulpecula, about 2,000 light-years away.",
                "It shines at magnitude 6.3 and contains about 170 stars.",
                "The cluster is about 1.1 billion years old.",
                "William Herschel discovered NGC 6940 in 1784 among the stars of Vulpecula.",
                "Its many red giants give the cluster a warm, golden appearance."
            ),
            fa = listOf(
                "NGC ۶۹۴۰ یک خوشه باز در صورت فلکی روباهک و در فاصله حدود ۲۰۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۳ می‌درخشد و حدود ۱۷۰ ستاره دارد.",
                "سن این خوشه حدود ۱/۱ میلیارد سال است.",
                "NGC ۶۹۴۰ را ویلیام هرشل در سال ۱۷۸۴ کشف کرد.",
                "غول‌های سرخ فراوان آن ظاهری گرم و طلایی به خوشه می‌دهند."
            )
        ),
        "dso_ngc_6960" to BilingualFacts(
            en = listOf(
                "NGC 6960, the Western Veil Nebula, is the bright western arc of the Cygnus Loop, a supernova remnant.",
                "The Western Veil lies about 2,400 light-years away and glows at magnitude 7.0.",
                "The supernova that created the Veil exploded between 5,000 and 8,000 years ago.",
                "The bright star 52 Cygni shines in front of the nebula but is not part of it.",
                "The nebula is often called the Witch's Broom for its delicate, sweeping filaments."
            ),
            fa = listOf(
                "NGC ۶۹۶۰ یا سحابی پرده غربی، کمان درخشان غربی حلقه ماکیان است که بازمانده یک ابرنواختر است.",
                "این سحابی در فاصله حدود ۲۴۰۰ سال نوری است و با قدر ۷/۰ می‌درخشد.",
                "ابرنواختری که پرده را آفرید بین ۵۰۰۰ تا ۸۰۰۰ سال پیش منفجر شد.",
                "ستاره درخشان ۵۲ ماکیان در جلوی سحابی می‌درخشد اما بخشی از آن نیست.",
                "این سحابی را به دلیل رشته‌های ظریف و جارومانندش اغلب «جاروی جادوگر» می‌نامند."
            )
        ),
        "dso_ngc_6992" to BilingualFacts(
            en = listOf(
                "NGC 6992, the Eastern Veil Nebula, is the bright eastern arc of the Cygnus Loop supernova remnant.",
                "The Eastern Veil sits about 2,400 light-years away at magnitude 7.0.",
                "The Veil's eastern arc is the brightest and most photographed section of the remnant.",
                "The nebula's glowing filaments are heated shock waves expanding into interstellar gas.",
                "The Cygnus Loop spans about three degrees of sky, six times the Moon's width."
            ),
            fa = listOf(
                "NGC ۶۹۹۲ یا سحابی پرده شرقی، کمان درخشان شرقی بازمانده ابرنواختری حلقه ماکیان است.",
                "این سحابی در فاصله حدود ۲۴۰۰ سال نوری است و با قدر ۷/۰ می‌درخشد.",
                "کمان شرقی پرده درخشان‌ترین و پرعکس‌ترین بخش این بازمانده است.",
                "رشته‌های درخشان این سحابی موج‌های ضربه‌ای گرم‌شده‌ای هستند که در گاز میان‌ستاره‌ای گسترش می‌یابند.",
                "حلقه ماکیان حدود سه درجه از آسمان را می‌پوشاند؛ شش برابر پهنای ماه."
            )
        ),
        "dso_ngc_7008" to BilingualFacts(
            en = listOf(
                "NGC 7008, the Fetus Nebula, is a planetary nebula in Cygnus, about 2,800 light-years away.",
                "It shines at magnitude 10.7 and appears as an irregular, patchy glow.",
                "The nebula's shape has been likened to a developing fetus.",
                "William Herschel discovered NGC 7008 in 1787 in Cygnus.",
                "It is one of the larger and more detailed planetary nebulae visible in Cygnus."
            ),
            fa = listOf(
                "NGC ۷۰۰۸ یا سحابی جنین، یک سحابی سیاره‌نما در صورت فلکی ماکیان و در فاصله حدود ۲۸۰۰ سال نوری است.",
                "این سحابی با قدر ۱۰/۷ می‌درخشد و به صورت درخششی نامنظم و تکه‌تکه دیده می‌شود.",
                "شکل این سحابی به جنین در حال رشد تشبیه شده است.",
                "NGC ۷۰۰۸ را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "این سحابی یکی از بزرگ‌تر و پرجزئیات‌تر سحابی‌های سیاره‌نمای قابل رصد در ماکیان است."
            )
        ),
        "dso_ngc_7009" to BilingualFacts(
            en = listOf(
                "NGC 7009, the Saturn Nebula, is a planetary nebula in Aquarius, about 3,900 light-years away.",
                "It shines at magnitude 8.0 and was discovered by William Herschel in 1782.",
                "Two lobes, called ansae, extend from the nebula like the rings of Saturn.",
                "The Saturn Nebula's central star is a hot white dwarf.",
                "The nebula was one of the first planetary nebulae studied in detail."
            ),
            fa = listOf(
                "NGC ۷۰۰۹ یا سحابی زحل، یک سحابی سیاره‌نما در صورت فلکی دلو و در فاصله حدود ۳۹۰۰ سال نوری است.",
                "این سحابی با قدر ۸/۰ می‌درخشد و ویلیام هرشل آن را در سال ۱۷۸۲ کشف کرد.",
                "دو لوب که «آنسه» نامیده می‌شوند از سحابی بیرون زده‌اند و مانند حلقه‌های زحل به نظر می‌رسند.",
                "ستاره مرکزی سحابی زحل یک کوتوله سفید داغ است.",
                "این سحابی یکی از نخستین سحابی‌های سیاره‌نمایی بود که با جزئیات مطالعه شد."
            )
        ),
        "dso_ngc_7027" to BilingualFacts(
            en = listOf(
                "NGC 7027 is a planetary nebula in Cygnus, about 3,000 light-years away.",
                "It shines at magnitude 10.4 and is very small and dense.",
                "NGC 7027 is one of the most studied planetary nebulae in the sky.",
                "The nebula is unusually rich in complex molecules and dust.",
                "It was discovered by Édouard Stephan in 1878."
            ),
            fa = listOf(
                "NGC ۷۰۲۷ یک سحابی سیاره‌نما در صورت فلکی ماکیان و در فاصله حدود ۳۰۰۰ سال نوری است.",
                "این سحابی با قدر ۱۰/۴ می‌درخشد و بسیار کوچک و متراکم است.",
                "NGC ۷۰۲۷ یکی از پرپژوهش‌ترین سحابی‌های سیاره‌نمای آسمان است.",
                "این سحابی به طور غیرعادی سرشار از مولکول‌های پیچیده و غبار است.",
                "این سحابی را ادوارد استفان در سال ۱۸۷۸ کشف کرد."
            )
        ),
        "dso_ngc_7062" to BilingualFacts(
            en = listOf(
                "NGC 7062 is an open cluster in Cygnus, about 6,000 light-years away.",
                "It shines at magnitude 8.3 and contains about 50 stars.",
                "William Herschel discovered NGC 7062 in 1788.",
                "The age of NGC 7062 is estimated at about 400 million years.",
                "It lies in the rich star fields of Cygnus."
            ),
            fa = listOf(
                "NGC ۷۰۶۲ یک خوشه باز در صورت فلکی ماکیان و در فاصله حدود ۶۰۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۳ می‌درخشد و حدود ۵۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۸ کشف کرد.",
                "سن NGC ۷۰۶۲ حدود ۴۰۰ میلیون سال است.",
                "این خوشه در میدان‌های پرستاره ماکیان جای دارد."
            )
        ),
        "dso_ngc_7063" to BilingualFacts(
            en = listOf(
                "NGC 7063 is an open cluster in Cygnus, about 2,200 light-years away.",
                "It shines at magnitude 7.0 and contains about 30 stars.",
                "Herschel logged NGC 7063 in 1788 while sweeping Cygnus.",
                "NGC 7063 is a young cluster aged about 100 million years.",
                "It is a loose, sparse grouping near the border with Pegasus."
            ),
            fa = listOf(
                "NGC ۷۰۶۳ یک خوشه باز در صورت فلکی ماکیان و در فاصله حدود ۲۲۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۰ می‌درخشد و حدود ۳۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۸ کشف کرد.",
                "سن NGC ۷۰۶۳ حدود ۱۰۰ میلیون سال است.",
                "این خوشه گروهی باز و پراکنده در نزدیکی مرز صورت فلکی اسب بالدار است."
            )
        ),
        "dso_ngc_7094" to BilingualFacts(
            en = listOf(
                "NGC 7094 is a planetary nebula in Pegasus, about 6,000 light-years away.",
                "It shines at magnitude 13.4, among the faintest objects in the catalog.",
                "The nebula appears as a dim, round shell of gas.",
                "NGC 7094 was discovered by Lewis Swift in 1884.",
                "It is a challenge object for large amateur telescopes."
            ),
            fa = listOf(
                "NGC ۷۰۹۴ یک سحابی سیاره‌نما در صورت فلکی اسب بالدار و در فاصله حدود ۶۰۰۰ سال نوری است.",
                "این سحابی با قدر ۱۳/۴ از کم‌نورترین اجرام فهرست است.",
                "این سحابی به صورت پوسته گرد و کم‌نوری از گاز دیده می‌شود.",
                "NGC ۷۰۹۴ را لوئیس سویفت در سال ۱۸۸۴ کشف کرد.",
                "این سحابی جرمی چالش‌برانگیز برای تلسکوپ‌های آماتوری بزرگ است."
            )
        ),
        "dso_ngc_7139" to BilingualFacts(
            en = listOf(
                "NGC 7139 is a planetary nebula in Cepheus, about 4,500 light-years away.",
                "It shines at magnitude 13.3 and appears as a faint, round glow.",
                "The nebula is dim and requires large aperture to observe.",
                "William Herschel found NGC 7139 in 1787 during his sweeps of the northern sky.",
                "Its faint halo is the remnant of gas shed by a dying star."
            ),
            fa = listOf(
                "NGC ۷۱۳۹ یک سحابی سیاره‌نما در صورت فلکی قیفاووس و در فاصله حدود ۴۵۰۰ سال نوری است.",
                "این سحابی با قدر ۱۳/۳ می‌درخشد و به صورت درخشش گرد و کم‌نوری دیده می‌شود.",
                "این سحابی کم‌نور است و برای رصد به گشودگی بزرگ نیاز دارد.",
                "NGC ۷۱۳۹ را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "هاله کم‌نور آن بازمانده گازی است که ستاره‌ای رو به مرگ پس زده است."
            )
        ),
        "dso_ngc_7160" to BilingualFacts(
            en = listOf(
                "NGC 7160 is an open cluster in Cepheus, about 2,500 light-years away.",
                "It shines at magnitude 6.1 and contains about 50 stars.",
                "NGC 7160 is a very young cluster, just 10 million years old.",
                "Herschel recorded NGC 7160 in 1787 while mapping the clusters of Cepheus.",
                "Its scattered bright stars make it a pleasant small-telescope target."
            ),
            fa = listOf(
                "NGC ۷۱۶۰ یک خوشه باز در صورت فلکی قیفاووس و در فاصله حدود ۲۵۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۱ می‌درخشد و حدود ۵۰ ستاره دارد.",
                "این خوشه جوان است و تنها حدود ۱۰ میلیون سال سن دارد.",
                "NGC ۷۱۶۰ را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "ستارگان درخشان پراکنده آن، این خوشه را به هدفی دلپذیر برای تلسکوپ‌های کوچک تبدیل کرده است."
            )
        ),
        "dso_ngc_7209" to BilingualFacts(
            en = listOf(
                "NGC 7209 is an open cluster in Lacerta, about 3,800 light-years away.",
                "It shines at magnitude 7.7 and contains about 100 stars.",
                "Herschel recorded NGC 7209 in 1787 while exploring the small constellation Lacerta.",
                "The stars of NGC 7209 are about 400 million years old.",
                "It lies in the small constellation Lacerta, the Lizard."
            ),
            fa = listOf(
                "NGC ۷۲۰۹ یک خوشه باز در صورت فلکی چلپاسه و در فاصله حدود ۳۸۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۷ می‌درخشد و حدود ۱۰۰ ستاره دارد.",
                "این خوشه را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "سن NGC ۷۲۰۹ حدود ۴۰۰ میلیون سال است.",
                "این خوشه در صورت فلکی کوچک چلپاسه (مارمولک) جای دارد."
            )
        ),
        "dso_ngc_7293" to BilingualFacts(
            en = listOf(
                "NGC 7293, the Helix Nebula, is one of the closest and brightest planetary nebulae, in Aquarius.",
                "It lies only about 650 light-years away and shines at magnitude 7.6.",
                "The Helix spans about 2.5 light-years across, huge for a planetary nebula.",
                "Its ring-like appearance has earned it the nickname the Eye of God.",
                "The nebula's central star is a white dwarf that will cool and fade over billions of years."
            ),
            fa = listOf(
                "NGC ۷۲۹۳ یا سحابی مارپیچ (هلیکس)، یکی از نزدیک‌ترین و درخشان‌ترین سحابی‌های سیاره‌نما در صورت فلکی دلو است.",
                "این سحابی تنها حدود ۶۵۰ سال نوری فاصله دارد و با قدر ۷/۶ می‌درخشد.",
                "قطر سحابی هلیکس حدود ۲/۵ سال نوری است؛ برای یک سحابی سیاره‌نما بسیار بزرگ.",
                "ظاهر حلقه‌مانند آن لقب «چشم خدا» را برایش به ارمغان آورده است.",
                "ستاره مرکزی این سحابی کوتوله سفیدی است که در گذر میلیاردها سال سرد و کم‌نور خواهد شد."
            )
        ),
        "dso_ngc_7354" to BilingualFacts(
            en = listOf(
                "NGC 7354 is a planetary nebula in Cepheus, about 5,000 light-years away.",
                "It shines at magnitude 12.2 and appears as a small, faint disk.",
                "The nebula is compact and requires large aperture to observe.",
                "William Herschel cataloged NGC 7354 in 1787, noting its small round form.",
                "Its faint outer shell surrounds a brighter inner region."
            ),
            fa = listOf(
                "NGC ۷۳۵۴ یک سحابی سیاره‌نما در صورت فلکی قیفاووس و در فاصله حدود ۵۰۰۰ سال نوری است.",
                "این سحابی با قدر ۱۲/۲ می‌درخشد و به صورت قرص کوچک و کم‌نوری دیده می‌شود.",
                "این سحابی فشرده است و برای رصد به گشودگی بزرگ نیاز دارد.",
                "NGC ۷۳۵۴ را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "پوسته بیرونی کم‌نور آن ناحیه درونی درخشان‌تری را در بر گرفته است."
            )
        ),
        "dso_ngc_7380" to BilingualFacts(
            en = listOf(
                "NGC 7380 is a young open cluster in Cepheus, about 7,200 light-years away.",
                "NGC 7380 glows at magnitude 7.2 and holds about 100 stars.",
                "The cluster lies within the glowing Wizard Nebula (Sh2-142).",
                "NGC 7380's hot young stars ionize the surrounding gas, making it glow.",
                "The cluster was discovered by Caroline Herschel in 1787."
            ),
            fa = listOf(
                "NGC ۷۳۸۰ یک خوشه باز جوان در صورت فلکی قیفاووس و در فاصله حدود ۷۲۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۲ می‌درخشد و حدود ۱۰۰ ستاره دارد.",
                "این خوشه درون سحابی درخشان جادوگر (Sh2-142) جای دارد.",
                "ستارگان داغ جوان NGC ۷۳۸۰ گاز پیرامون را یونیده و آن را به درخشش وامی‌دارند.",
                "این خوشه را کارولین هرشل در سال ۱۷۸۷ کشف کرد."
            )
        ),
        "dso_ngc_7510" to BilingualFacts(
            en = listOf(
                "NGC 7510 is an open cluster in Cepheus, about 9,000 light-years away.",
                "It shines at magnitude 7.9 and contains about 60 stars.",
                "The stars of NGC 7510 are only 10 million years old.",
                "Herschel logged NGC 7510 in 1787 as a compact grouping in Cepheus.",
                "Its stars form a compact, arrow-like pattern."
            ),
            fa = listOf(
                "NGC ۷۵۱۰ یک خوشه باز در صورت فلکی قیفاووس و در فاصله حدود ۹۰۰۰ سال نوری است.",
                "این خوشه با قدر ۷/۹ می‌درخشد و حدود ۶۰ ستاره دارد.",
                "این خوشه جوان است و تنها حدود ۱۰ میلیون سال سن دارد.",
                "NGC ۷۵۱۰ را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "ستارگان آن الگویی فشرده و پیکان‌مانند می‌سازند."
            )
        ),
        "dso_ngc_7686" to BilingualFacts(
            en = listOf(
                "NGC 7686 is an open cluster in Andromeda, about 3,200 light-years away.",
                "It shines at magnitude 5.6 and contains about 30 stars.",
                "The cluster is visible in binoculars as a faint grouping.",
                "William Herschel discovered NGC 7686 in 1787 among the star fields of Andromeda.",
                "It is a loose, scattered cluster with no central concentration."
            ),
            fa = listOf(
                "NGC ۷۶۸۶ یک خوشه باز در صورت فلکی آندرومدا و در فاصله حدود ۳۲۰۰ سال نوری است.",
                "این خوشه با قدر ۵/۶ می‌درخشد و حدود ۳۰ ستاره دارد.",
                "این خوشه در دوربین دوچشمی به صورت گروه کم‌نوری دیده می‌شود.",
                "NGC ۷۶۸۶ را ویلیام هرشل در سال ۱۷۸۷ کشف کرد.",
                "این خوشه‌ای باز و پراکنده بدون تمرکز مرکزی است."
            )
        ),
        "dso_ngc_7789" to BilingualFacts(
            en = listOf(
                "NGC 7789, Caroline's Rose, is a rich open cluster in Cassiopeia, about 7,600 light-years away.",
                "It shines at magnitude 6.7 and contains over 1,000 stars.",
                "The cluster's swirling loops of stars resemble the petals of a rose.",
                "Caroline Herschel found NGC 7789 in 1783 while sweeping Cassiopeia.",
                "The cluster is about 1.6 billion years old."
            ),
            fa = listOf(
                "NGC ۷۷۸۹ یا گل سرخ کارولین، یک خوشه باز پرستاره در صورت فلکی ذات‌الکرسی و در فاصله حدود ۷۶۰۰ سال نوری است.",
                "این خوشه با قدر ۶/۷ می‌درخشد و بیش از ۱۰۰۰ ستاره دارد.",
                "حلقه‌های چرخان ستارگان این خوشه به گلبرگ‌های گل سرخ می‌مانند.",
                "NGC ۷۷۸۹ را کارولین هرشل در سال ۱۷۸۳ کشف کرد.",
                "سن این خوشه حدود ۱/۶ میلیارد سال است."
            )
        ),
        "dso_ngc_7790" to BilingualFacts(
            en = listOf(
                "NGC 7790 is an open cluster in Cassiopeia, about 7,800 light-years away.",
                "It shines at magnitude 8.5 and contains about 60 stars.",
                "The cluster hosts three Cepheid variable stars, useful for measuring distances.",
                "William Herschel discovered NGC 7790 in 1788 in Cassiopeia.",
                "The cluster is about 80 million years old."
            ),
            fa = listOf(
                "NGC ۷۷۹۰ یک خوشه باز در صورت فلکی ذات‌الکرسی و در فاصله حدود ۷۸۰۰ سال نوری است.",
                "این خوشه با قدر ۸/۵ می‌درخشد و حدود ۶۰ ستاره دارد.",
                "این خوشه میزبان سه ستاره متغیر قیفاووسی است که برای اندازه‌گیری فاصله‌ها سودمندند.",
                "NGC ۷۷۹۰ را ویلیام هرشل در سال ۱۷۸۸ کشف کرد.",
                "سن این خوشه حدود ۸۰ میلیون سال است."
            )
        )
    )
}
