package com.alijafari.red.astronomy.data.catalog

/**
 * Hand-authored, object-specific bilingual fact sets for the 39 bright stars in the canonical
 * union list that previously had no facts. Five facts each, independent of one another.
 * Magnitudes, distances, and spectral classes follow Hipparcos/SIMBAD data; logged in the
 * research log.
 */
internal object StarFacts {

    val map: Map<String, BilingualFacts> = mapOf(
        "star_cma_adhara" to BilingualFacts(
            en = listOf(
                "Adhara (Epsilon Canis Majoris) is a blue bright giant of spectral class B1.5II shining at magnitude 1.50.",
                "It lies about 430 light-years from Earth in Canis Major, just south of Sirius.",
                "Adhara is a binary system whose primary is one of the hottest bright stars visible to the naked eye.",
                "It was the brightest source of extreme-ultraviolet radiation in the night sky, measured by the EUVE satellite in the 1990s.",
                "The name Adhara comes from the Arabic al-adhara, meaning the virgins."
            ),
            fa = listOf(
                "عَذاری (اپسیلون سگ بزرگ) یک غول درخشان آبی از رده طیفی B1.5II است که با قدر ۱/۵۰ می‌درخشد.",
                "این ستاره در فاصله حدود ۴۳۰ سال نوری از زمین و در صورت فلکی سگ بزرگ، در جنوب شباهنگ قرار دارد.",
                "عذاری یک سامانه دوتایی است و ستاره اصلی آن یکی از داغ‌ترین ستارگان درخشانِ قابل‌رؤیت با چشم غیرمسلح است.",
                "این ستاره در دهه ۱۹۹۰ به عنوان درخشان‌ترین چشمه پرتو فرابنفش شدید آسمان شب توسط ماهواره EUVE اندازه‌گیری شد.",
                "نام عذاری از واژه عربی «العذاری» به معنای «دوشیزگان» گرفته شده است."
            )
        ),
        "star_cma_wezen" to BilingualFacts(
            en = listOf(
                "Wezen (Delta Canis Majoris) is a yellow supergiant of spectral class F8Ia at magnitude 1.83.",
                "It lies about 1,600 light-years away, making it one of the most distant naked-eye stars.",
                "Wezen is among the most intrinsically luminous stars known, with an absolute magnitude near -6.9.",
                "The star has finished core hydrogen fusion and is evolving toward the red supergiant stage.",
                "The name Wezen derives from the Arabic wazn, meaning weight."
            ),
            fa = listOf(
                "وَزن (دلتا سگ بزرگ) یک ابرغول زرد از رده طیفی F8Ia با قدر ۱/۸۳ است.",
                "این ستاره در فاصله حدود ۱۶۰۰ سال نوری قرار دارد و یکی از دورترین ستارگان قابل‌رؤیت با چشم غیرمسلح است.",
                "وزن از نظر ذاتی یکی از پرنورترین ستارگان شناخته‌شده است و قدر مطلق آن نزدیک به ۶/۹- است.",
                "این ستاره همجوشی هیدروژن در هسته خود را به پایان رسانده و در حال تحول به مرحله ابرغول سرخ است.",
                "نام وزن از واژه عربی «وَزن» به معنای «سنگینی» گرفته شده است."
            )
        ),
        "star_cma_mirzam" to BilingualFacts(
            en = listOf(
                "Mirzam (Beta Canis Majoris) is a blue giant of spectral class B1II-III at magnitude 1.98.",
                "It lies about 500 light-years from Earth in Canis Major.",
                "Mirzam is a Beta Cephei variable, pulsating with a period of about six hours.",
                "Its Arabic name means the herald, because it rises just before Sirius.",
                "The star marks one of the front legs of the great dog Canis Major."
            ),
            fa = listOf(
                "مِرزَم (بتا سگ بزرگ) یک غول آبی از رده طیفی B1II-III با قدر ۱/۹۸ است.",
                "این ستاره در فاصله حدود ۵۰۰ سال نوری از زمین و در صورت فلکی سگ بزرگ قرار دارد.",
                "مرزم یک ستاره متغیر از نوع بتا قیفاووسی است که با دوره‌ای حدود شش ساعت می‌تپد.",
                "نام عربی آن به معنای «پیشاهنگ» است، زیرا کمی پیش از شباهنگ طلوع می‌کند.",
                "این ستاره نشانگر یکی از پای‌های جلویی سگ بزرگ است."
            )
        ),
        "star_cmi_procyon" to BilingualFacts(
            en = listOf(
                "Procyon (Alpha Canis Minoris) is a yellow-white subgiant of class F5IV-V, the eighth brightest star in the night sky at magnitude 0.34.",
                "It is one of the nearest bright stars, only about 11.5 light-years away.",
                "Procyon is a binary system whose faint white-dwarf companion, Procyon B, was confirmed visually in 1896.",
                "The name Procyon means before the dog in Greek, because it rises shortly before Sirius.",
                "Together with Sirius and Betelgeuse it forms the Winter Triangle."
            ),
            fa = listOf(
                "شعرای شامی (آلفا سگ کوچک) یک زیرغول زرد-سفید از رده F5IV-V و هشتمین ستاره درخشان آسمان شب با قدر ۰/۳۴ است.",
                "این ستاره یکی از نزدیک‌ترین ستارگان درخشان است و تنها حدود ۱۱/۵ سال نوری فاصله دارد.",
                "شعرای شامی یک سامانه دوتایی است و همدم کوتوله سفید آن، شعرای شامی B، در سال ۱۸۹۶ به طور رصدی تأیید شد.",
                "نام پروکیون در یونانی به معنای «پیش از سگ» است، زیرا کمی پیش از شباهنگ طلوع می‌کند.",
                "این ستاره به همراه شباهنگ و ابط‌الجوزا مثلث زمستانی را می‌سازد."
            )
        ),
        "star_car_canopus" to BilingualFacts(
            en = listOf(
                "Canopus (Alpha Carinae) is the second brightest star in the night sky at magnitude -0.74.",
                "It is a bright giant of class A9II (F0II) lying about 310 light-years away.",
                "Canopus is far more luminous than Sirius, shining with roughly 10,000 times the Sun's light.",
                "It is invisible from latitudes north of about 37 degrees, and served ancient navigators as a southern guide star.",
                "Spacecraft, including several interplanetary probes, have used Canopus as a reference for attitude control."
            ),
            fa = listOf(
                "سُهَیل (آلفا شاه‌تخته) دومین ستاره درخشان آسمان شب با قدر ۰/۷۴- است.",
                "این ستاره یک غول درخشان از رده A9II (F0II) است که در فاصله حدود ۳۱۰ سال نوری قرار دارد.",
                "سهیل بسیار پرنورتر از شباهنگ است و حدود ۱۰,۰۰۰ برابر خورشید نور می‌تاباند.",
                "این ستاره از عرض‌های شمالی‌تر از حدود ۳۷ درجه دیده نمی‌شود و در گذشته راهنمای جنوبی دریانوردان بود.",
                "فضاپیماهای متعددی، از جمله چند کاوشگر بین‌سیاره‌ای، از سهیل به عنوان مرجع کنترل وضعیت استفاده کرده‌اند."
            )
        ),
        "star_car_miaplacidus" to BilingualFacts(
            en = listOf(
                "Miaplacidus (Beta Carinae) is the second brightest star in Carina, at magnitude 1.67.",
                "It is a white giant of spectral class A1III located about 113 light-years away.",
                "The star lies near the border of the old constellation Robur Carolinum, the Oak of Charles II.",
                "Miaplacidus is a relatively fast rotator with a rotation period under two days.",
                "Its name combines the Arabic miyah (waters) with the Latin placidus (placid)."
            ),
            fa = listOf(
                "میاپلاسیدوس (بتا شاه‌تخته) دومین ستاره درخشان صورت فلکی شاه‌تخته با قدر ۱/۶۷ است.",
                "این ستاره یک غول سفید از رده طیفی A1III است که در فاصله حدود ۱۱۳ سال نوری قرار دارد.",
                "این ستاره نزدیک مرز صورت فلکی منسوخ‌شده «بلوط چارلز دوم» (Robur Carolinum) جای دارد.",
                "میاپلاسیدوس چرخش نسبتاً سریعی دارد و دوره چرخش آن کمتر از دو روز است.",
                "نام آن ترکیبی از واژه عربی «مِیاه» (آب‌ها) و واژه لاتین «پلاسیدوس» (آرام) است."
            )
        ),
        "star_cen_rigil_kent" to BilingualFacts(
            en = listOf(
                "Rigil Kentaurus (Alpha Centauri) is the closest star system to the Sun, only 4.37 light-years away.",
                "It appears as the third brightest star in the night sky, with a combined magnitude of -0.01.",
                "The system is triple: the Sun-like pair Alpha Centauri A and B plus the red dwarf Proxima Centauri.",
                "Proxima Centauri, the faint third member, is the nearest individual star to the Sun.",
                "The name Rigil Kentaurus comes from Arabic, meaning the foot of the centaur."
            ),
            fa = listOf(
                "رِجل قنطورس (آلفا قنطورس) نزدیک‌ترین سامانه ستاره‌ای به خورشید است و تنها ۴/۳۷ سال نوری فاصله دارد.",
                "این ستاره با قدر ترکیبی ۰/۰۱- سومین ستاره درخشان آسمان شب به شمار می‌رود.",
                "این سامانه سه‌تایی است: جفت خورشیدمانند آلفا قنطورس A و B به همراه کوتوله سرخ پروکسیما قنطورس.",
                "پروکسیما قنطورس، عضو کم‌نور سوم، نزدیک‌ترین ستاره منفرد به خورشید است.",
                "نام رِجل قنطورس از عربی گرفته شده و به معنای «پای قنطورس» است."
            )
        ),
        "star_cen_hadar" to BilingualFacts(
            en = listOf(
                "Hadar (Beta Centauri) is the second brightest star in Centaurus at magnitude 0.61.",
                "It is a blue giant of spectral class B1III located about 390 light-years away.",
                "Hadar is a triple system: a close spectroscopic pair plus a more distant companion.",
                "Along with Rigil Kentaurus it forms the pair known as the Southern Pointers toward Crux.",
                "The name Hadar comes from an Arabic word meaning ground."
            ),
            fa = listOf(
                "هَدار (بتا قنطورس) دومین ستاره درخشان صورت فلکی قنطورس با قدر ۰/۶۱ است.",
                "این ستاره یک غول آبی از رده طیفی B1III است که در فاصله حدود ۳۹۰ سال نوری قرار دارد.",
                "هدار یک سامانه سه‌تایی است: یک جفت طیف‌سنجی نزدیک به هم به همراه یک همدم دورتر.",
                "این ستاره به همراه رجل قنطورس جفت «اشاره‌گرهای جنوبی» به سوی صلیب جنوبی را می‌سازد.",
                "نام هدار از واژه‌ای عربی به معنای «زمین» گرفته شده است."
            )
        ),
        "star_cyg_deneb" to BilingualFacts(
            en = listOf(
                "Deneb (Alpha Cygni) is a white supergiant of class A2Ia marking the tail of the swan.",
                "At magnitude 1.25 it is the brightest star in Cygnus and one of the three stars of the Summer Triangle.",
                "Deneb is one of the most luminous stars visible to the naked eye, with an absolute magnitude near -8.4.",
                "Its distance is uncertain but is on the order of 2,600 light-years.",
                "Deneb is the prototype of the Alpha Cygni class of pulsating supergiants."
            ),
            fa = listOf(
                "دِنب (آلفا ماکیان) یک ابرغول سفید از رده A2Ia است که نشانگر دم قو است.",
                "این ستاره با قدر ۱/۲۵ درخشان‌ترین ستاره ماکیان و یکی از سه ستاره مثلث تابستانی است.",
                "دنب یکی از پرنورترین ستارگان قابل‌رؤیت با چشم غیرمسلح است و قدر مطلق آن نزدیک به ۸/۴- است.",
                "فاصله آن نامشخص اما در حدود ۲۶۰۰ سال نوری است.",
                "دنب الگوی رده ستارگان متغیر تپنده «آلفا ماکیانی» است."
            )
        ),
        "star_cyg_sadr" to BilingualFacts(
            en = listOf(
                "Sadr (Gamma Cygni) is a yellow-white supergiant of class F8Ib at magnitude 2.23.",
                "It marks the heart of the swan at the center of the Northern Cross.",
                "Sadr lies about 1,800 light-years away in the rich Cygnus star fields of the Milky Way.",
                "It is surrounded by the diffuse emission nebula IC 1318, the Sadr or Butterfly region.",
                "The star is a slow variable whose brightness fluctuates by a few hundredths of a magnitude."
            ),
            fa = listOf(
                "صَدر (گاما ماکیان) یک ابرغول زرد-سفید از رده F8Ib با قدر ۲/۲۳ است.",
                "این ستاره نشانگر قلب قو و در مرکز صلیب شمالی قرار دارد.",
                "صدر در فاصله حدود ۱۸۰۰ سال نوری و در میدان‌های پرستاره راه شیریِ ماکیان جای دارد.",
                "این ستاره در میان سحابی پخشی IC ۱۳۱۸، معروف به ناحیه صدر یا پروانه، قرار گرفته است.",
                "صدر یک ستاره متغیر کُند است که درخشندگی آن چند صدم قدر تغییر می‌کند."
            )
        ),
        "star_cyg_albireo" to BilingualFacts(
            en = listOf(
                "Albireo (Beta Cygni) marks the head of the swan at magnitude 3.05.",
                "It is one of the finest double stars in the sky: a golden K giant and a blue companion.",
                "The two components are separated by about 35 arcseconds, easily split in a small telescope.",
                "The pair lies roughly 430 light-years away, and it is debated whether they are truly gravitationally bound.",
                "The name Albireo is a medieval mistranslation of the Arabic name for the bird."
            ),
            fa = listOf(
                "آلبیرو (بتا ماکیان) نشانگر سر قو است و با قدر ۳/۰۵ می‌درخشد.",
                "این ستاره یکی از زیباترین ستاره‌های دوتایی آسمان است: یک غول طلایی و یک همدم آبی.",
                "دو مؤلفه آن با جدایی حدود ۳۵ ثانیه قوسی به راحتی در یک تلسکوپ کوچک از هم تفکیک می‌شوند.",
                "این جفت در فاصله حدود ۴۳۰ سال نوری قرار دارد و هنوز درباره پیوند گرانشی واقعی آن‌ها بحث است.",
                "نام آلبیرو حاصل ترجمه نادرست قرون وسطایی از نام عربی این پرنده است."
            )
        ),
        "star_aql_altair" to BilingualFacts(
            en = listOf(
                "Altair (Alpha Aquilae) is the brightest star in Aquila and the twelfth brightest in the night sky at magnitude 0.76.",
                "It is a white main-sequence star of class A7V, only about 16.7 light-years away.",
                "Altair spins so fast, once every nine hours, that it is visibly flattened at its poles.",
                "It is one of the nearest stars to the Sun visible to the naked eye.",
                "Altair forms the Summer Triangle with Vega and Deneb."
            ),
            fa = listOf(
                "نسر طایر (آلفا عقاب) درخشان‌ترین ستاره عقاب و دوازدهمین ستاره درخشان آسمان شب با قدر ۰/۷۶ است.",
                "این ستاره از رده A7V و از نوع رشته اصلی سفید است و تنها حدود ۱۶/۷ سال نوری فاصله دارد.",
                "نسر طایر چنان سریع می‌چرخد که هر دور آن ۹ ساعت طول می‌کشد و در قطب‌های خود به شکل محسوسی پَخ شده است.",
                "این ستاره یکی از نزدیک‌ترین ستارگان قابل‌رؤیت با چشم غیرمسلح به خورشید است.",
                "نسر طایر به همراه نسر واقع و دِنب، مثلث تابستانی را می‌سازد."
            )
        ),
        "star_ori_rigel" to BilingualFacts(
            en = listOf(
                "Rigel (Beta Orionis) is a blue supergiant of class B8Ia and the brightest star in Orion at magnitude 0.13.",
                "It lies about 860 light-years away and shines with roughly 120,000 times the Sun's luminosity.",
                "Rigel marks the hunter's left foot and is the seventh brightest star in the night sky.",
                "The star is actually a multiple system whose main component is a hot supergiant of about 21 solar masses.",
                "The name Rigel comes from the Arabic rijl, meaning foot."
            ),
            fa = listOf(
                "رِجل‌الجبار (بتا شکارچی) یک ابرغول آبی از رده B8Ia و درخشان‌ترین ستاره شکارچی با قدر ۰/۱۳ است.",
                "این ستاره در فاصله حدود ۸۶۰ سال نوری است و حدود ۱۲۰,۰۰۰ برابر خورشید نور می‌تاباند.",
                "رجل‌الجبار نشانگر پای چپ شکارچی و هفتمین ستاره درخشان آسمان شب است.",
                "این ستاره در واقع یک سامانه چندتایی است که مؤلفه اصلی آن ابرغولی داغ با جرمی حدود ۲۱ برابر خورشید است.",
                "نام رجل‌الجبار از واژه عربی «رِجل» به معنای «پا» گرفته شده است."
            )
        ),
        "star_ori_bellatrix" to BilingualFacts(
            en = listOf(
                "Bellatrix (Gamma Orionis) is a blue giant of class B2III at magnitude 1.64.",
                "It lies about 250 light-years away and is about 6,400 times more luminous than the Sun.",
                "Bellatrix marks the hunter's western shoulder, and its surface temperature is near 21,500 K.",
                "The name Bellatrix is Latin for female warrior.",
                "It is one of the hottest stars bright enough to be easily seen with the naked eye."
            ),
            fa = listOf(
                "بلاتریکس (گاما شکارچی) یک غول آبی از رده B2III با قدر ۱/۶۴ است.",
                "این ستاره در فاصله حدود ۲۵۰ سال نوری است و حدود ۶۴۰۰ برابر خورشید نور می‌تاباند.",
                "بلاتریکس نشانگر شانه غربی شکارچی است و دمای سطح آن نزدیک به ۲۱,۵۰۰ کلوین است.",
                "نام بلاتریکس واژه‌ای لاتین به معنای «زن جنگجو» است.",
                "این ستاره یکی از داغ‌ترین ستارگانی است که به اندازه کافی درخشان‌اند تا با چشم غیرمسلح به آسانی دیده شوند."
            )
        ),
        "star_ori_saiph" to BilingualFacts(
            en = listOf(
                "Saiph (Kappa Orionis) is a blue supergiant of class B0.5Ia at magnitude 2.07.",
                "It lies about 650 light-years away, marking the hunter's right foot opposite Rigel.",
                "Saiph shines with roughly 57,000 times the Sun's luminosity despite its modest apparent brightness.",
                "Its high surface temperature near 26,000 K gives it a distinctly blue-white color.",
                "The name Saiph comes from the Arabic sayf, meaning sword."
            ),
            fa = listOf(
                "سَیف (کاپا شکارچی) یک ابرغول آبی از رده B0.5Ia با قدر ۲/۰۷ است.",
                "این ستاره در فاصله حدود ۶۵۰ سال نوری است و نشانگر پای راست شکارچی، روبه‌روی رجل‌الجبار است.",
                "سیف با وجود درخشندگی ظاهری کم، حدود ۵۷,۰۰۰ برابر خورشید نور می‌تاباند.",
                "دمای سطح بالای آن نزدیک به ۲۶,۰۰۰ کلوین به آن رنگ آبی-سفید مشخصی می‌دهد.",
                "نام سیف از واژه عربی «سَیف» به معنای «شمشیر» گرفته شده است."
            )
        ),
        "star_ori_alnitak" to BilingualFacts(
            en = listOf(
                "Alnitak (Zeta Orionis) is a hot blue supergiant of class O9.5Iab, the leftmost star of Orion's Belt.",
                "It lies about 1,260 light-years away and is the brightest O-type star visible to the naked eye.",
                "Alnitak is a triple system, with a close companion completing an orbit every 2,687 years.",
                "The star sits beside the Horsehead Nebula (B33) and illuminates the Flame Nebula (NGC 2024).",
                "The name Alnitak comes from the Arabic an-nitaq, meaning the girdle."
            ),
            fa = listOf(
                "نِطاق (زتا شکارچی) یک ابرغول آبی داغ از رده O9.5Iab و چپ‌ترین ستاره کمربند شکارچی است.",
                "این ستاره در فاصله حدود ۱۲۶۰ سال نوری است و درخشان‌ترین ستاره رده O قابل‌رؤیت با چشم غیرمسلح است.",
                "نطاق یک سامانه سه‌تایی است و همدم نزدیک آن هر ۲۶۸۷ سال یک دور کامل می‌زند.",
                "این ستاره در کنار سحابی سر اسب (B۳۳) قرار دارد و سحابی شعله (NGC ۲۰۲۴) را روشن می‌کند.",
                "نام نطاق از واژه عربی «النطاق» به معنای «کمربند» گرفته شده است."
            )
        ),
        "star_ori_alnilam" to BilingualFacts(
            en = listOf(
                "Alnilam (Epsilon Orionis) is the middle star of Orion's Belt, a blue supergiant of class B0Ia.",
                "At magnitude 1.69 it is the brightest of the three belt stars despite being the most distant at about 2,000 light-years.",
                "Alnilam is among the most luminous stars known, radiating several hundred thousand times the Sun's energy.",
                "The star is losing mass through a strong stellar wind at a rate millions of times greater than the Sun's.",
                "The name Alnilam derives from the Arabic an-nizam, meaning string of pearls."
            ),
            fa = listOf(
                "نِظام (اپسیلون شکارچی) ستاره میانی کمربند شکارچی و یک ابرغول آبی از رده B0Ia است.",
                "این ستاره با قدر ۱/۶۹ درخشان‌ترین ستاره از سه ستاره کمربند است، هرچند با فاصله حدود ۲۰۰۰ سال نوری دورترین آن‌هاست.",
                "نظام از پرنورترین ستارگان شناخته‌شده است و چند صد هزار برابر خورشید انرژی می‌تاباند.",
                "این ستاره از طریق باد ستاره‌ای نیرومندی جرم از دست می‌دهد که میلیون‌ها برابر پرشدت‌تر از باد خورشیدی است.",
                "نام نظام از واژه عربی «النظام» به معنای «رشته مروارید» گرفته شده است."
            )
        ),
        "star_ori_mintaka" to BilingualFacts(
            en = listOf(
                "Mintaka (Delta Orionis) is the westernmost star of Orion's Belt, at magnitude 2.23.",
                "It is a hot blue giant of class O9.5II located about 1,200 light-years away.",
                "Mintaka is a multiple system whose brightest pair forms an eclipsing binary.",
                "The star lies almost exactly on the celestial equator, making it visible from nearly everywhere on Earth.",
                "The name Mintaka comes from the Arabic manṭaqa, meaning belt or region."
            ),
            fa = listOf(
                "مِنطَقه (دلتا شکارچی) غربی‌ترین ستاره کمربند شکارچی با قدر ۲/۲۳ است.",
                "این ستاره یک غول آبی داغ از رده O9.5II است که در فاصله حدود ۱۲۰۰ سال نوری قرار دارد.",
                "منطقه یک سامانه چندتایی است که درخشان‌ترین جفت آن یک دوتایی گرفتی را می‌سازد.",
                "این ستاره تقریباً دقیقاً روی استوای آسمان قرار دارد و به همین دلیل از تقریباً همه جای زمین دیده می‌شود.",
                "نام منطقه از واژه عربی «مَنطَقه» به معنای «کمربند» یا «ناحیه» گرفته شده است."
            )
        ),
        "star_tau_aldebaran" to BilingualFacts(
            en = listOf(
                "Aldebaran (Alpha Tauri) is an orange giant of class K5III and the brightest star in Taurus at magnitude 0.85.",
                "It lies about 65 light-years away and marks the fiery eye of the bull.",
                "Aldebaran appears within the V-shaped Hyades cluster but is actually a foreground star, less than half as far.",
                "The star has expanded to about 44 times the Sun's diameter as it evolved off the main sequence.",
                "The name Aldebaran comes from the Arabic al-dabaran, meaning the follower, because it follows the Pleiades."
            ),
            fa = listOf(
                "دَبَران (آلفا گاو) یک غول نارنجی از رده K5III و درخشان‌ترین ستاره گاو با قدر ۰/۸۵ است.",
                "این ستاره در فاصله حدود ۶۵ سال نوری است و چشم آتشین گاو را نشان می‌دهد.",
                "دبران درون خوشه V-شکل قَلائص دیده می‌شود اما در واقع ستاره‌ای پیش‌زمینه‌ای است و فاصله‌اش کمتر از نصف فاصله آن خوشه است.",
                "این ستاره پس از خروج از رشته اصلی به حدود ۴۴ برابر قطر خورشید منبسط شده است.",
                "نام دبران از واژه عربی «الدبران» به معنای «دنباله‌رو» گرفته شده است، زیرا به دنبال خوشه پروین می‌آید."
            )
        ),
        "star_tau_elnath" to BilingualFacts(
            en = listOf(
                "Elnath (Beta Tauri) is a blue-white giant of class B7III at magnitude 1.65.",
                "It lies about 130 light-years away and marks the tip of the bull's northern horn.",
                "Elnath is shared with Auriga, where it was once cataloged as Gamma Aurigae.",
                "The star is a mercury-manganese chemically peculiar star with unusually strong metal lines.",
                "The name Elnath comes from the Arabic an-nath, meaning the butting one."
            ),
            fa = listOf(
                "نَطح (بتا گاو) یک غول آبی-سفید از رده B7III با قدر ۱/۶۵ است.",
                "این ستاره در فاصله حدود ۱۳۰ سال نوری است و نوک شاخ شمالی گاو را نشان می‌دهد.",
                "نطح با صورت فلکی ارابه‌ران مشترک است و روزگاری با نام گاما ارابه‌ران فهرست می‌شد.",
                "این ستاره از نوع ستارگان شیمیایی خاص جیوه-منگنز است و خطوط فلزی به طور غیرعادی قوی‌ای دارد.",
                "نام نطح از واژه عربی «النطح» به معنای «شاخ‌زننده» گرفته شده است."
            )
        ),
        "star_aur_capella" to BilingualFacts(
            en = listOf(
                "Capella (Alpha Aurigae) is the sixth brightest star in the night sky at magnitude 0.08.",
                "It is a yellow giant of class G3III (with a hotter G0III companion) about 42.9 light-years away.",
                "Capella is actually a quadruple system made of two binary pairs.",
                "It is the nearest first-magnitude star to the north celestial pole, making it circumpolar from much of the northern hemisphere.",
                "The name Capella is Latin for little she-goat."
            ),
            fa = listOf(
                "عَیوق (آلفا ارابه‌ران) ششمین ستاره درخشان آسمان شب با قدر ۰/۰۸ است.",
                "این ستاره یک غول زرد از رده G3III (با همدمی داغ‌تر از رده G0III) در فاصله حدود ۴۲/۹ سال نوری است.",
                "عیوق در واقع یک سامانه چهارتایی است که از دو جفت دوتایی ساخته شده است.",
                "این ستاره نزدیک‌ترین ستاره قدر اول به قطب شمال آسمان است و به همین دلیل از بخش بزرگی از نیمکره شمالی دورقطبی است.",
                "نام عیوق واژه‌ای لاتین به معنای «بزغاله کوچک» است."
            )
        ),
        "star_uma_dubhe" to BilingualFacts(
            en = listOf(
                "Dubhe (Alpha Ursae Majoris) is an orange giant of class K0III at magnitude 1.79.",
                "It is one of the two Pointer stars of the Big Dipper's bowl, pointing toward Polaris.",
                "Dubhe lies about 123 light-years away and is a spectroscopic binary.",
                "Unlike most of the Dipper, Dubhe does not share the common motion of the Ursa Major Moving Group.",
                "The name Dubhe comes from the Arabic dubb, meaning bear."
            ),
            fa = listOf(
                "دُبه (آلفا خرس بزرگ) یک غول نارنجی از رده K0III با قدر ۱/۷۹ است.",
                "این ستاره یکی از دو ستاره اشاره‌گر کاسه ملاقه بزرگ است که به سوی ستاره قطبی اشاره می‌کنند.",
                "دبه در فاصله حدود ۱۲۳ سال نوری است و یک دوتایی طیف‌سنجی است.",
                "برخلاف بیشتر ستارگان ملاقه، دبه در حرکت مشترک گروه متحرک خرس بزرگ شرکت ندارد.",
                "نام دبه از واژه عربی «دُبّ» به معنای «خرس» گرفته شده است."
            )
        ),
        "star_uma_merak" to BilingualFacts(
            en = listOf(
                "Merak (Beta Ursae Majoris) is a white main-sequence star of class A1V at magnitude 2.37.",
                "It is the other Pointer star of the Big Dipper, paired with Dubhe.",
                "Merak lies about 79.7 light-years away.",
                "The star is surrounded by a circumstellar debris disk of dust detected at infrared wavelengths.",
                "The name Merak comes from the Arabic maraqq, meaning the loins of the bear."
            ),
            fa = listOf(
                "مِراق (بتا خرس بزرگ) یک ستاره رشته اصلی سفید از رده A1V با قدر ۲/۳۷ است.",
                "این ستاره دومین ستاره اشاره‌گر ملاقه بزرگ است و با دبه جفت شده است.",
                "مراق در فاصله حدود ۷۹/۷ سال نوری قرار دارد.",
                "این ستاره با قرصی از غبار و خرده‌سنگ دورستاره‌ای احاطه شده که در طول‌موج‌های فروسرخ آشکار شده است.",
                "نام مراق از واژه عربی «مَراقّ» به معنای «کمرگاه خرس» گرفته شده است."
            )
        ),
        "star_uma_alioth" to BilingualFacts(
            en = listOf(
                "Alioth (Epsilon Ursae Majoris) is the brightest star of Ursa Major at magnitude 1.77.",
                "It is an Ap star of class A1p, about 82.6 light-years away.",
                "Alioth has one of the strongest magnetic fields measured in a normal star.",
                "The star is a spectroscopic binary and is also a mild variable of the Alpha-2 Canum Venaticorum class.",
                "It is the first star of the Big Dipper's handle, counting from the bowl."
            ),
            fa = listOf(
                "عَناق (اپسیلون خرس بزرگ) درخشان‌ترین ستاره خرس بزرگ با قدر ۱/۷۷ است.",
                "این ستاره از رده A1p و از نوع ستارگان شیمیایی خاص (Ap) است و حدود ۸۲/۶ سال نوری فاصله دارد.",
                "عناق یکی از قوی‌ترین میدان‌های مغناطیسی اندازه‌گیری‌شده در یک ستاره عادی را دارد.",
                "این ستاره یک دوتایی طیف‌سنجی و همچنین متغیری ملایم از نوع آلفا-۲ تازی‌ها است.",
                "عناق نخستین ستاره دسته ملاقه بزرگ است، اگر از سمت کاسه بشماریم."
            )
        ),
        "star_uma_mizar" to BilingualFacts(
            en = listOf(
                "Mizar (Zeta Ursae Majoris) is a star of class A2V at magnitude 2.23, the middle star of the Big Dipper's handle.",
                "In 1650 Giovanni Riccioli discovered that Mizar was the first telescopic double star.",
                "Mizar is itself a four-star system, and with its neighbor Alcor it forms a famous naked-eye pair.",
                "The Mizar-Alcor pair was used in antiquity as an eyesight test.",
                "Mizar lies about 82.9 light-years from Earth."
            ),
            fa = listOf(
                "مئزر (زتا خرس بزرگ) ستاره‌ای از رده A2V با قدر ۲/۲۳ و ستاره میانی دسته ملاقه بزرگ است.",
                "در سال ۱۶۵۰ جووانی ریچیولی کشف کرد که مئزر نخستین ستاره دوتایی تلسکوپی است.",
                "مئزر خود یک سامانه چهارستاره‌ای است و با همسایه‌اش سُها جفت مشهور قابل‌رؤیت با چشم غیرمسلح را می‌سازد.",
                "جفت مئزر-سها در دوران باستان برای آزمون تیزبینی چشم به کار می‌رفت.",
                "مئزر در فاصله حدود ۸۲/۹ سال نوری از زمین قرار دارد."
            )
        ),
        "star_uma_alkaid" to BilingualFacts(
            en = listOf(
                "Alkaid (Eta Ursae Majoris) is the star at the end of the Big Dipper's handle, at magnitude 1.85.",
                "It is a hot blue main-sequence star of class B3V, the hottest of the Dipper stars.",
                "Alkaid lies about 103.9 light-years away and shines with about 700 times the Sun's luminosity.",
                "Unlike most Dipper stars it does not belong to the Ursa Major Moving Group.",
                "The name Alkaid comes from the Arabic al-qa'id, meaning the leader."
            ),
            fa = listOf(
                "قائد (اتا خرس بزرگ) ستاره انتهای دسته ملاقه بزرگ با قدر ۱/۸۵ است.",
                "این ستاره یک ستاره رشته اصلی آبی داغ از رده B3V و داغ‌ترین ستاره ملاقه است.",
                "قائد در فاصله حدود ۱۰۳/۹ سال نوری است و حدود ۷۰۰ برابر خورشید نور می‌تاباند.",
                "برخلاف بیشتر ستارگان ملاقه، قائد عضو گروه متحرک خرس بزرگ نیست.",
                "نام قائد از واژه عربی «القائد» به معنای «پیشرو» گرفته شده است."
            )
        ),
        "star_cas_schedar" to BilingualFacts(
            en = listOf(
                "Schedar (Alpha Cassiopeiae) is the brightest star in Cassiopeia, an orange giant of class K0IIIa at magnitude 2.24.",
                "It lies about 228 light-years away in the W of Cassiopeia.",
                "Schedar is a suspected variable star whose brightness changes by a few hundredths of a magnitude.",
                "The star has expanded to roughly 42 times the Sun's diameter.",
                "The name Schedar comes from the Arabic sadr, meaning breast."
            ),
            fa = listOf(
                "صَدرالکرسی (آلفا ذات‌الکرسی) درخشان‌ترین ستاره ذات‌الکرسی و یک غول نارنجی از رده K0IIIa با قدر ۲/۲۴ است.",
                "این ستاره در فاصله حدود ۲۲۸ سال نوری و در شکل W ذات‌الکرسی قرار دارد.",
                "صدرالکرسی یک ستاره متغیر مشکوک است که درخشندگی آن چند صدم قدر تغییر می‌کند.",
                "این ستاره به حدود ۴۲ برابر قطر خورشید منبسط شده است.",
                "نام صدرالکرسی از واژه عربی «صَدر» به معنای «سینه» گرفته شده است."
            )
        ),
        "star_cas_caph" to BilingualFacts(
            en = listOf(
                "Caph (Beta Cassiopeiae) is a yellow-white giant of class F2III at magnitude 2.28.",
                "It lies about 54.7 light-years away, marking one corner of the W of Cassiopeia.",
                "Caph is a Delta Scuti variable, pulsating with a period of about 2.5 hours.",
                "The star is a rapid rotator, spinning at more than 70 km/s at its equator.",
                "The name Caph comes from the Arabic kaff, meaning palm of the hand."
            ),
            fa = listOf(
                "کَف‌الخضیب (بتا ذات‌الکرسی) یک غول زرد-سفید از رده F2III با قدر ۲/۲۸ است.",
                "این ستاره در فاصله حدود ۵۴/۷ سال نوری است و یکی از گوشه‌های شکل W ذات‌الکرسی را می‌سازد.",
                "کف‌الخضیب یک متغیر از نوع دلتا سپری است که با دوره‌ای حدود ۲/۵ ساعت می‌تپد.",
                "این ستاره چرخش سریعی دارد و سرعت چرخش آن در استوا بیش از ۷۰ کیلومتر بر ثانیه است.",
                "نام کف‌الخضیب از واژه عربی «کَفّ» به معنای «کف دست» گرفته شده است."
            )
        ),
        "star_per_mirfak" to BilingualFacts(
            en = listOf(
                "Mirfak (Alpha Persei) is the brightest star in Perseus, a yellow-white supergiant of class F5Ib at magnitude 1.79.",
                "It lies about 510 light-years away and is the center of the Alpha Persei Cluster, Melotte 20.",
                "The star is surrounded by a loose group of young hot stars visible in binoculars.",
                "Mirfak shines with roughly 5,000 times the Sun's luminosity.",
                "The name Mirfak comes from the Arabic mirfaq, meaning elbow."
            ),
            fa = listOf(
                "مِرفَق‌الثریا (آلفا برساوش) درخشان‌ترین ستاره برساوش و یک ابرغول زرد-سفید از رده F5Ib با قدر ۱/۷۹ است.",
                "این ستاره در فاصله حدود ۵۱۰ سال نوری است و مرکز خوشه آلفا برساوش (Melotte 20) است.",
                "این ستاره را گروهی پراکنده از ستارگان جوان و داغ احاطه کرده که با دوربین دوچشمی دیده می‌شوند.",
                "مرفق‌الثریا حدود ۵۰۰۰ برابر خورشید نور می‌تاباند.",
                "نام مرفق‌الثریا از واژه عربی «مِرفَق» به معنای «آرنج» گرفته شده است."
            )
        ),
        "star_per_algol" to BilingualFacts(
            en = listOf(
                "Algol (Beta Persei) is the prototype eclipsing binary star, at magnitude 2.12 on average.",
                "Every 2.87 days its light dips from magnitude 2.1 to 3.4 for about ten hours as a fainter companion passes in front.",
                "The system consists of a hot B8V primary and a cooler giant companion about 90 light-years away.",
                "Algol's periodic fading was known to ancient observers, and the star was called the Demon Star.",
                "The name Algol derives from the Arabic ra's al-ghul, meaning the demon's head."
            ),
            fa = listOf(
                "رأس‌الغول (بتا برساوش) الگوی دوتایی‌های گرفتی است و به طور میانگین با قدر ۲/۱۲ می‌درخشد.",
                "هر ۲/۸۷ روز یک بار، نورش به مدت حدود ده ساعت از قدر ۲/۱ به ۳/۴ افت می‌کند، زیرا همدم کم‌نورتر از برابر آن می‌گذرد.",
                "این سامانه از یک ستاره اصلی داغ از رده B8V و یک همدم غول سردتر در فاصله حدود ۹۰ سال نوری ساخته شده است.",
                "کاهش دوره‌ای نور رأس‌الغول برای رصدگران باستانی شناخته شده بود و آن را «ستاره شیطان» می‌نامیدند.",
                "نام رأس‌الغول از عبارت عربی «رأس الغول» به معنای «سر دیو» گرفته شده است."
            )
        ),
        "star_boo_arcturus" to BilingualFacts(
            en = listOf(
                "Arcturus (Alpha Boötis) is the brightest star of the northern celestial hemisphere and the fourth brightest overall, at magnitude -0.05.",
                "It is an orange giant of class K1III, about 36.7 light-years away.",
                "Arcturus was the first star other than the Sun to be observed in the daytime with a telescope, by Jean-Baptiste Morin in 1635.",
                "The star has a large proper motion, crossing the sky at more than two arcseconds per year.",
                "Its name means guardian of the bear in Greek, from its position following Ursa Major."
            ),
            fa = listOf(
                "سِماک رامح (آلفا عوّاد) درخشان‌ترین ستاره نیمکره شمالی آسمان و چهارمین ستاره درخشان کل آسمان با قدر ۰/۰۵- است.",
                "این ستاره یک غول نارنجی از رده K1III است و حدود ۳۶/۷ سال نوری فاصله دارد.",
                "سماک رامح نخستین ستاره‌ای بود که پس از خورشید در روشنایی روز با تلسکوپ رصد شد؛ این کار را ژان-باپتیست مورین در سال ۱۶۳۵ انجام داد.",
                "این ستاره حرکت خاص بزرگی دارد و هر سال بیش از دو ثانیه قوسی در آسمان جابه‌جا می‌شود.",
                "نام آن در یونانی به معنای «نگهبان خرس» است و از جایگاهش در پی خرس بزرگ گرفته شده است."
            )
        ),
        "star_vir_spica" to BilingualFacts(
            en = listOf(
                "Spica (Alpha Virginis) is the brightest star in Virgo, at magnitude 0.98.",
                "It is a hot blue binary of class B1III about 250 light-years away.",
                "The two components are so close that they distort each other, making Spica an ellipsoidal variable.",
                "Spica's position provided Hipparchus with data that later helped reveal the precession of the equinoxes.",
                "Its name comes from the Latin spica virginis, meaning the ear of wheat of the maiden."
            ),
            fa = listOf(
                "سِماک اعزل (آلفا دوشیزه) درخشان‌ترین ستاره صورت فلکی دوشیزه با قدر ۰/۹۸ است.",
                "این ستاره یک دوتایی آبی داغ از رده B1III است که حدود ۲۵۰ سال نوری فاصله دارد.",
                "دو مؤلفه آن چنان به هم نزدیک‌اند که یکدیگر را تغییر شکل می‌دهند و سماک اعزل را به متغیری بیضی‌وار تبدیل کرده‌اند.",
                "جایگاه سماک اعزل به هیپارخوس داده‌هایی داد که بعدها به کشف حرکت تقدیمی اعتدالین کمک کرد.",
                "نام آن از عبارت لاتین spica virginis به معنای «خوشه گندم دوشیزه» گرفته شده است."
            )
        ),
        "star_sco_antares" to BilingualFacts(
            en = listOf(
                "Antares (Alpha Scorpii) is a red supergiant of class M1.5Iab and the brightest star in Scorpius at magnitude 0.96.",
                "It lies about 550 light-years away and has expanded to roughly 680 times the Sun's radius.",
                "Antares is a binary system whose companion, Antares B, is a hot blue main-sequence star.",
                "Its reddish color made ancient observers call it the rival of Mars (Ares).",
                "The star marks the heart of the scorpion and is near the end of its life, destined to explode as a supernova."
            ),
            fa = listOf(
                "قَلب‌العقرب (آلفا عقرب) یک ابرغول سرخ از رده M1.5Iab و درخشان‌ترین ستاره عقرب با قدر ۰/۹۶ است.",
                "این ستاره در فاصله حدود ۵۵۰ سال نوری است و به حدود ۶۸۰ برابر شعاع خورشید منبسط شده است.",
                "قلب‌العقرب یک سامانه دوتایی است و همدم آن، قلب‌العقرب B، یک ستاره رشته اصلی آبی داغ است.",
                "رنگ سرخ آن سبب شد رصدگران باستانی آن را «رقیب مریخ» (آرس) بنامند.",
                "این ستاره نشانگر قلب عقرب است و به پایان عمر خود نزدیک می‌شود؛ سرنوشت آن انفجار ابرنواختری است."
            )
        ),
        "star_sco_shaula" to BilingualFacts(
            en = listOf(
                "Shaula (Lambda Scorpii) is the second brightest star in Scorpius, at magnitude 1.62.",
                "It is a hot blue subgiant of class B2IV about 570 light-years away.",
                "Shaula marks the stinger at the tip of the scorpion's tail.",
                "The star is a triple system, with the main pair forming a tight binary.",
                "Its name comes from the Arabic ash-shaulah, meaning the raised tail."
            ),
            fa = listOf(
                "شَوله (لامبدا عقرب) دومین ستاره درخشان صورت فلکی عقرب با قدر ۱/۶۲ است.",
                "این ستاره یک زیرغول آبی داغ از رده B2IV است که حدود ۵۷۰ سال نوری فاصله دارد.",
                "شوله نشانگر نیش در نوک دم عقرب است.",
                "این ستاره یک سامانه سه‌تایی است و جفت اصلی آن دوتایی تنگاتنگی را می‌سازد.",
                "نام آن از واژه عربی «الشولة» به معنای «دم برافراشته» گرفته شده است."
            )
        ),
        "star_leo_regulus" to BilingualFacts(
            en = listOf(
                "Regulus (Alpha Leonis) is the brightest star in Leo and the 21st brightest in the night sky, at magnitude 1.36.",
                "It is a blue-white main-sequence star of class B8IVn, about 79 light-years away.",
                "Regulus spins extremely fast, completing a rotation in under 16 hours, which flattens it at the poles.",
                "The star is actually a quadruple system with three fainter companions.",
                "It lies almost exactly on the ecliptic, so the Moon and planets frequently occult it."
            ),
            fa = listOf(
                "قَلب‌الاسد (آلفا شیر) درخشان‌ترین ستاره شیر و بیست‌ویکمین ستاره درخشان آسمان شب با قدر ۱/۳۶ است.",
                "این ستاره رشته اصلی آبی-سفید از رده B8IVn است و حدود ۷۹ سال نوری فاصله دارد.",
                "قلب‌الاسد چرخش بسیار سریعی دارد و هر دور آن کمتر از ۱۶ ساعت طول می‌کشد و همین آن را در قطب‌ها پَخ کرده است.",
                "این ستاره در واقع یک سامانه چهارتایی با سه همدم کم‌نورتر است.",
                "قلب‌الاسد تقریباً دقیقاً روی دایرةالبروج قرار دارد و به همین دلیل ماه و سیارات بارها از برابر آن می‌گذرند."
            )
        ),
        "star_gem_pollux" to BilingualFacts(
            en = listOf(
                "Pollux (Beta Geminorum) is the brightest star in Gemini at magnitude 1.14, brighter than its twin Castor.",
                "It is an orange giant of class K0III, only about 34 light-years away.",
                "Pollux is the nearest giant star to the Sun.",
                "In 2006 an exoplanet, Pollux b (Thestias), was confirmed orbiting the star with a period of about 590 days.",
                "Pollux has exhausted its core hydrogen and has begun evolving into a red giant."
            ),
            fa = listOf(
                "پولوکس (بتا دوپیکر) درخشان‌ترین ستاره دوپیکر با قدر ۱/۱۴ است و از همتای خود کاستور درخشان‌تر است.",
                "این ستاره یک غول نارنجی از رده K0III است و تنها حدود ۳۴ سال نوری فاصله دارد.",
                "پولوکس نزدیک‌ترین ستاره غول به خورشید است.",
                "در سال ۲۰۰۶ یک سیاره فراخورشیدی به نام پولوکس b (تِستیاس) با دوره مداری حدود ۵۹۰ روز تأیید شد.",
                "پولوکس هیدروژن هسته خود را مصرف کرده و روند تحول به غول سرخ را آغاز کرده است."
            )
        ),
        "star_gem_castor" to BilingualFacts(
            en = listOf(
                "Castor (Alpha Geminorum) is a white star of class A1V at magnitude 1.58.",
                "It is one of the most complex systems known: six stars bound in three binary pairs.",
                "Although labeled Alpha, Castor is dimmer than Beta Geminorum (Pollux), a famous Bayer-designation anomaly.",
                "The Castor system lies about 51.6 light-years away.",
                "In myth, Castor was the mortal twin brother of Pollux, the sons of Leda."
            ),
            fa = listOf(
                "کاستور (آلفا دوپیکر) ستاره‌ای سفید از رده A1V با قدر ۱/۵۸ است.",
                "این ستاره یکی از پیچیده‌ترین سامانه‌های شناخته‌شده است: شش ستاره که در سه جفت دوتایی به هم پیوند خورده‌اند.",
                "با وجود برچسب آلفا، کاستور از بتا دوپیکر (پولوکس) کم‌نورتر است؛ این یک ناهنجاری مشهور در نام‌گذاری بایر است.",
                "سامانه کاستور در فاصله حدود ۵۱/۶ سال نوری قرار دارد.",
                "در اساطیر، کاستور برادر فانی پولوکس بود و هر دو فرزندان لدا بودند."
            )
        ),
        "star_psa_fomalhaut" to BilingualFacts(
            en = listOf(
                "Fomalhaut (Alpha Piscis Austrini) is the brightest star of the Southern Fish, at magnitude 1.17.",
                "It is a young white star of class A3V, only about 25 light-years away and about 440 million years old.",
                "Fomalhaut is surrounded by a broad, sharply defined debris ring of dust imaged by the Hubble Space Telescope.",
                "In 2008 a candidate planet, Fomalhaut b, was the first to be directly imaged in visible light, though its nature is debated.",
                "The name Fomalhaut comes from the Arabic fum al-hut, meaning mouth of the fish."
            ),
            fa = listOf(
                "فَم‌الحوت (آلفا ماهی جنوبی) درخشان‌ترین ستاره ماهی جنوبی با قدر ۱/۱۷ است.",
                "این ستاره سفید جوان از رده A3V است که تنها حدود ۲۵ سال نوری فاصله دارد و سنش حدود ۴۴۰ میلیون سال است.",
                "فم‌الحوت با حلقه‌ای پهن و لبه‌تیز از غبار احاطه شده که تلسکوپ فضایی هابل از آن تصویربرداری کرده است.",
                "در سال ۲۰۰۸ نامزد سیاره فم‌الحوت b نخستین جسمی بود که در نور مرئی به طور مستقیم تصویر شد، هرچند ماهیت آن مورد بحث است.",
                "نام فم‌الحوت از عبارت عربی «فَم الحوت» به معنای «دهان ماهی» گرفته شده است."
            )
        ),
        "star_eri_achernar" to BilingualFacts(
            en = listOf(
                "Achernar (Alpha Eridani) is the ninth brightest star in the night sky at magnitude 0.45.",
                "It is a hot blue main-sequence star of class B6Vep, about 139 light-years away.",
                "Achernar is the most oblate star known: it spins so fast that its equatorial diameter is about 1.5 times its polar diameter.",
                "The star is invisible from latitudes north of about 32 degrees.",
                "The name Achernar comes from the Arabic akhir an-nahr, meaning end of the river."
            ),
            fa = listOf(
                "آخِرالنهر (آلفا نهر) نهمین ستاره درخشان آسمان شب با قدر ۰/۴۵ است.",
                "این ستاره رشته اصلی آبی داغ از رده B6Vep است و حدود ۱۳۹ سال نوری فاصله دارد.",
                "آخرالنهر پَخ‌ترین ستاره شناخته‌شده است: چنان سریع می‌چرخد که قطر استوایی آن حدود ۱/۵ برابر قطر قطبی آن است.",
                "این ستاره از عرض‌های شمالی‌تر از حدود ۳۲ درجه دیده نمی‌شود.",
                "نام آخرالنهر از عبارت عربی «آخر النهر» به معنای «پایان رود» گرفته شده است."
            )
        )
    )
}
