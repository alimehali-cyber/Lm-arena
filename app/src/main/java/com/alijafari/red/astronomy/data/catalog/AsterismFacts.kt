package com.alijafari.red.astronomy.data.catalog

/**
 * Hand-authored, object-specific bilingual fact sets for the 7 asterisms in the canonical union
 * list. Five facts each, independent of one another. Sources (constituent stars and their data)
 * follow Hipparcos/SIMBAD and standard star-atlas references; logged in the research log.
 */
internal object AsterismFacts {

    val map: Map<String, BilingualFacts> = mapOf(
        "asterism_big_dipper" to BilingualFacts(
            en = listOf(
                "The Big Dipper is formed by the seven brightest stars of Ursa Major: Dubhe, Merak, Phecda, Megrez, Alioth, Mizar, and Alkaid.",
                "The two stars at the end of the bowl, Dubhe and Merak, are the Pointers because a line through them leads to Polaris.",
                "Mizar, the middle star of the handle, forms a famous naked-eye double with its companion Alcor.",
                "The Dipper is not a single physical group: five stars move together as part of the Ursa Major Moving Group, while Dubhe and Alkaid move independently.",
                "It is circumpolar from most of the northern United States, Europe, and Asia, visible on every clear night of the year."
            ),
            fa = listOf(
                "ملاقه بزرگ از هفت ستاره درخشان خرس بزرگ ساخته شده است: دُبه، مراق، فَخد، مَغرز، عناق، مئزر و قائد.",
                "دو ستاره انتهای کاسه ملاقه، یعنی دبه و مراق، ستاره‌های اشاره‌گر نامیده می‌شوند زیرا خط گذرنده از آن‌ها به ستاره قطبی می‌رسد.",
                "مئزر، ستاره میانی دسته ملاقه، به همراه همدمش سُها یک جفت مشهور برای آزمون تیزبینی چشم است.",
                "ملاقه بزرگ یک گروه فیزیکی واحد نیست: پنج ستاره آن به‌عنوان اعضای گروه متحرک خرس بزرگ با هم حرکت می‌کنند، اما دبه و قائد حرکت مستقل دارند.",
                "این صورتواره از بیشتر مناطق شمالی ایالات متحده، اروپا و آسیا دورقطبی است و هر شب صاف سال دیده می‌شود."
            )
        ),
        "asterism_summer_triangle" to BilingualFacts(
            en = listOf(
                "The Summer Triangle is formed by three first-magnitude stars: Vega in Lyra, Deneb in Cygnus, and Altair in Aquila.",
                "The three stars belong to three different constellations but are linked purely by our line of sight.",
                "Altair is the nearest of the three at about 16.7 light-years, while Deneb is so distant that its exact distance is uncertain, on the order of 2,600 light-years.",
                "The Milky Way runs through the triangle, passing between Vega and Altair and through Cygnus near Deneb.",
                "The triangle is prominent from late spring through autumn in the northern hemisphere."
            ),
            fa = listOf(
                "مثلث تابستانی از سه ستاره قدر اول ساخته شده است: نسر واقع در شلیاق، دِنب در ماکیان و نسر طایر در عقاب.",
                "این سه ستاره به سه صورت فلکی متفاوت تعلق دارند و تنها به دلیل امتداد خط دید ما در کنار هم دیده می‌شوند.",
                "نسر طایر با فاصله حدود ۱۶/۷ سال نوری نزدیک‌ترین آن‌هاست، در حالی که دِنب چنان دور است که فاصله دقیقش نامشخص و در حدود ۲۶۰۰ سال نوری است.",
                "راه شیری از درون این مثلث می‌گذرد و از میان نسر واقع و نسر طایر و از کنار دِنب در ماکیان عبور می‌کند.",
                "این مثلث در نیمکره شمالی از اواخر بهار تا پاییز به‌خوبی دیده می‌شود."
            )
        ),
        "asterism_winter_hexagon" to BilingualFacts(
            en = listOf(
                "The Winter Hexagon is a large asterism joining six bright stars: Rigel, Aldebaran, Capella, Pollux, Procyon, and Sirius.",
                "It encloses most of the winter sky and is visible from December through March in the northern hemisphere.",
                "Sirius, at magnitude -1.46, is the brightest star in the hexagon and in the entire night sky.",
                "Betelgeuse, the red supergiant of Orion, sits near the center of the hexagon.",
                "Because the six stars lie at very different distances, the hexagon is a chance alignment, not a physical group."
            ),
            fa = listOf(
                "شش‌ضلعی زمستانی صورتواره بزرگی است که شش ستاره درخشان رِجِل، دبران، عیوق، پولوکس، شعرای شامی و شباهنگ را به هم می‌پیوندد.",
                "این صورتواره بخش بزرگی از آسمان زمستانی را در بر می‌گیرد و در نیمکره شمالی از آذر تا اسفند دیده می‌شود.",
                "شباهنگ با قدر ۱/۴۶- درخشان‌ترین ستاره این شش‌ضلعی و درخشان‌ترین ستاره کل آسمان شب است.",
                "ابط‌الجوزا، ابرغول سرخ شکارچی، نزدیک مرکز این شش‌ضلعی جای دارد.",
                "از آنجا که این شش ستاره در فاصله‌های بسیار متفاوتی از ما قرار دارند، این شش‌ضلعی یک هم‌راستایی اتفاقی است، نه یک گروه فیزیکی."
            )
        ),
        "asterism_northern_cross" to BilingualFacts(
            en = listOf(
                "The Northern Cross is formed by the brightest stars of Cygnus: Deneb, Sadr, Albireo, and the wings Gienah and Delta Cygni.",
                "Deneb, at the top of the cross, marks the tail of the swan, while Albireo at the base marks its head.",
                "The long axis of the cross lies along the Milky Way, making the region rich in star fields.",
                "Despite its cross shape, the Northern Cross is not to be confused with the Southern Cross (Crux), which points to the south celestial pole.",
                "The cross is a useful signpost to the constellation Cygnus and the summer Milky Way."
            ),
            fa = listOf(
                "صلیب شمالی از درخشان‌ترین ستارگان ماکیان ساخته شده است: دِنب، صدر، آلبیرو و دو ستاره بال یعنی جناح و دلتا ماکیان.",
                "دِنب در بالای صلیب نشانگر دم قو است و آلبیرو در پایه آن نشانگر سر قو است.",
                "محور بلند این صلیب در امتداد راه شیری قرار دارد و به همین دلیل این ناحیه سرشار از میدان‌های ستاره‌ای است.",
                "با وجود شکل صلیبی آن، صلیب شمالی را نباید با صلیب جنوبی (چلیپا) اشتباه گرفت که به قطب جنوب آسمان اشاره می‌کند.",
                "این صلیب راهنمای خوبی برای یافتن صورت فلکی ماکیان و راه شیری تابستانی است."
            )
        ),
        "asterism_great_square_pegasus" to BilingualFacts(
            en = listOf(
                "The Great Square of Pegasus is formed by four stars: Alpheratz, Scheat, Markab, and Algenib.",
                "Alpheratz technically belongs to Andromeda, being shared between the two constellations.",
                "The square has no bright stars inside it, making it a classic test of sky transparency.",
                "It is the dominant asterism of the northern autumn sky, riding high around midnight in October.",
                "The northeast side of the square points toward the Andromeda Galaxy (M31)."
            ),
            fa = listOf(
                "چهارضلعی بزرگ اسب بالدار از چهار ستاره ساخته شده است: سِرّه‌الفَرس، شِئات، مَرکَب و جَنب (الغینب).",
                "ستاره سره‌الفرس از نظر تقسیم‌بندی به صورت فلکی آندرومدا تعلق دارد و میان این دو صورت فلکی مشترک است.",
                "درون این چهارضلعی هیچ ستاره درخشانی نیست و به همین دلیل آزمونی کلاسیک برای سنجش شفافیت آسمان به شمار می‌رود.",
                "این صورتواره شاخص آسمان پاییزی شمال است و حدود نیمه‌شب اکتبر در اوج آسمان دیده می‌شود.",
                "ضلع شمال‌شرقی این چهارضلعی به سوی کهکشان آندرومدا (M۳۱) اشاره می‌کند."
            )
        ),
        "asterism_teapot" to BilingualFacts(
            en = listOf(
                "The Teapot is an asterism formed by the eight brightest stars of Sagittarius.",
                "Its spout points toward the center of the Milky Way and the supermassive black hole Sagittarius A*.",
                "The asterism lies in the richest part of the Milky Way, surrounded by the Lagoon, Trifid, and Omega nebulae.",
                "The handle of the teapot is formed by the stars Nunki, Tau, Phi, and Zeta Sagittarii.",
                "The Teapot is best seen from the southern hemisphere and from low northern latitudes during summer."
            ),
            fa = listOf(
                "صورتواره قوری از هشت ستاره درخشان صورت فلکی کمان ساخته شده است.",
                "دهانه این قوری به سوی مرکز راه شیری و سیاهچاله کلان‌جرم کمان A* اشاره می‌کند.",
                "این صورتواره در پرستاره‌ترین بخش راه شیری جای دارد و سحابی‌های مرداب، سه‌تکه و اُمگا گرداگرد آن را گرفته‌اند.",
                "دسته قوری را ستارگان نونکی، تاو، فی و زتا کمان می‌سازند.",
                "قوری در تابستان از نیمکره جنوبی و از عرض‌های پایین شمالی به‌خوبی دیده می‌شود."
            )
        ),
        "asterism_sickle_leo" to BilingualFacts(
            en = listOf(
                "The Sickle is a backwards question-mark asterism outlining the head and mane of Leo the Lion.",
                "Its handle ends at Regulus, the brightest star of Leo and the heart of the lion.",
                "The Sickle's stars are Epsilon, Mu, Zeta, Gamma, and Eta Leonis plus Regulus.",
                "The star Algieba (Gamma Leonis), in the Sickle's curve, is a beautiful double star for small telescopes.",
                "The Sickle rises in the east on spring evenings and is a key signpost of the coming spring sky."
            ),
            fa = listOf(
                "صورتواره داس یک علامت سؤال وارونه است که سر و یال شیر را ترسیم می‌کند.",
                "دسته این داس به قلب‌الاسد، درخشان‌ترین ستاره صورت فلکی شیر و نشانگر قلب شیر، ختم می‌شود.",
                "ستارگان داس عبارت‌اند از اپسیلون، مو، زتا، گاما و اتا شیر به همراه قلب‌الاسد.",
                "ستاره جَبهه (گاما شیر) در قوس داس، یک ستاره دوتایی زیبا برای تلسکوپ‌های کوچک است.",
                "این داس در شب‌های بهاری از شرق طلوع می‌کند و نشانه‌ای بارز برای فرارسیدن آسمان بهاری است."
            )
        )
    )
}
