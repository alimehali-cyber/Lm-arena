package com.alijafari.red.astronomy.data.catalog

/**
 * Hand-authored, object-specific bilingual fact sets for the 20 constellations in the canonical
 * union list. Each set is 5 facts and is independent (no cross-object templating). Sources are
 * logged per object in docs/dso-content-research-log.md (IAU constellation boundaries/areas,
 * Hipparcos/SIMBAD stellar magnitudes, and NGC/IC project catalog data).
 */
internal object ConstellationFacts {

    val map: Map<String, BilingualFacts> = mapOf(
        "const_ori" to BilingualFacts(
            en = listOf(
                "Orion covers 594 square degrees, making it the 26th largest of the 88 IAU constellations.",
                "Its brightest star is the blue supergiant Rigel (Beta Orionis), shining at magnitude 0.13, while the red supergiant Betelgeuse (Alpha Orionis) is a close second.",
                "The three belt stars Alnitak, Alnilam, and Mintaka point toward Sirius in one direction and Aldebaran in the other.",
                "The Orion Nebula (M42), the nearest major stellar nursery at about 1,350 light-years, lies in Orion's sword below the belt.",
                "Named for the mythological hunter, Orion is the most recognizable winter constellation in both hemispheres."
            ),
            fa = listOf(
                "صورت فلکی شکارچی با ۵۹۴ درجه مربع مساحت، بیست‌وششمین صورت بزرگ در میان ۸۸ صورت فلکی اتحادیه بین‌المللی نجوم است.",
                "درخشان‌ترین ستاره آن ابرغول آبی رِجِل (بتا شکارچی) با قدر ۰/۱۳ است و ابرغول سرخ ابط‌الجوزا (آلفا شکارچی) در جایگاه دوم قرار دارد.",
                "سه ستاره کمربند شکارچی یعنی نطاق، نظام و منطقه، از یک سو به سوی شباهنگ و از سوی دیگر به سوی دبران اشاره می‌کنند.",
                "سحابی جبار (M۴۲)، نزدیک‌ترین زایشگاه ستاره‌ای بزرگ با فاصله حدود ۱۳۵۰ سال نوری، در شمشیر شکارچی زیر کمربند قرار دارد.",
                "این صورت فلکی به نام شکارچی اسطوره‌ای نام‌گذاری شده و شناخته‌شده‌ترین صورت فلکی زمستانی در هر دو نیمکره است."
            )
        ),
        "const_uma" to BilingualFacts(
            en = listOf(
                "Ursa Major spans 1,280 square degrees, the third largest of all 88 constellations.",
                "Its brightest star is Alioth (Epsilon Ursae Majoris), at magnitude 1.76.",
                "The Big Dipper is an asterism of its seven brightest stars, and the two pointer stars Dubhe and Merak point to Polaris.",
                "The constellation hosts the Whirlpool Galaxy (M51), the Pinwheel Galaxy (M101), and the galaxy pair M81/M82.",
                "In Greek myth the Great Bear is Callisto, transformed by Hera and placed in the sky by Zeus."
            ),
            fa = listOf(
                "صورت فلکی خرس بزرگ با ۱۲۸۰ درجه مربع مساحت، سومین صورت بزرگ در میان ۸۸ صورت فلکی است.",
                "درخشان‌ترین ستاره آن عَناق (اپسیلون خرس بزرگ) با قدر ۱/۷۶ است.",
                "هفت ستاره درخشان آن شکل‌دهنده صورتواره ملاقه بزرگ (هفت‌برادران) هستند و دو ستاره اشاره‌گر ظَهر و مِراق به سوی ستاره قطبی اشاره می‌کنند.",
                "این صورت فلکی میزبان کهکشان گرداب (M۵۱)، کهکشان فرفره (M۱۰۱) و جفت کهکشانی M۸۱/M۸۲ است.",
                "در اساطیر یونان، خرس بزرگ همان کالیستو است که هرا او را به خرس تبدیل کرد و زئوس در آسمان جای داد."
            )
        ),
        "const_umi" to BilingualFacts(
            en = listOf(
                "Ursa Minor spans 256 square degrees, ranking 56th in size among the 88 constellations.",
                "Polaris (Alpha Ursae Minoris), the North Star, is its brightest star at magnitude 1.98.",
                "Polaris lies within 0.7 degrees of the north celestial pole, and its altitude equals the observer's latitude.",
                "The Little Dipper is the constellation's core asterism, with Polaris at the tip of the handle.",
                "Named the Lesser Bear, it is home to the northern celestial pole toward which Earth's axis currently points."
            ),
            fa = listOf(
                "صورت فلکی خرس کوچک با ۲۵۶ درجه مربع مساحت، پنجاه‌وششمین صورت بزرگ در میان ۸۸ صورت فلکی است.",
                "ستاره قطبی (آلفا خرس کوچک)، راهنمای شمال، درخشان‌ترین ستاره آن با قدر ۱/۹۸ است.",
                "ستاره قطبی در فاصله کمتر از ۰/۷ درجه از قطب شمال آسمان قرار دارد و ارتفاع آن برابر عرض جغرافیایی رصدگر است.",
                "صورتواره ملاقه کوچک هسته این صورت فلکی است و ستاره قطبی در انتهای دسته آن جای دارد.",
                "این صورت فلکی که خرس کوچک نامیده می‌شود، میزبان قطب شمال آسمان است که محور زمین اکنون به سوی آن اشاره می‌کند."
            )
        ),
        "const_cas" to BilingualFacts(
            en = listOf(
                "Cassiopeia spans 598 square degrees, the 25th largest constellation.",
                "Its brightest star is Schedar (Alpha Cassiopeiae), an orange giant of magnitude 2.24.",
                "The constellation's five bright stars form a distinctive W (or M) shape that is circumpolar from most northern latitudes.",
                "Cassiopeia A is the remnant of a supernova whose light reached Earth about 350 years ago, the strongest radio source beyond the Solar System.",
                "It also contains the open clusters M52 and M103 and borders the rich Milky Way band."
            ),
            fa = listOf(
                "صورت فلکی ذات‌الکرسی با ۵۹۸ درجه مربع مساحت، بیست‌وپنجمین صورت بزرگ آسمان است.",
                "درخشان‌ترین ستاره آن صَدر (آلفا ذات‌الکرسی)، یک غول نارنجی با قدر ۲/۲۴ است.",
                "پنج ستاره درخشان آن شکل W (یا M) مشخصی می‌سازند که از بیشتر عرض‌های شمالی همواره بالای افق است.",
                "ذات‌الکرسی آ، بازمانده انفجار ابرنواختری است که نورش حدود ۳۵۰ سال پیش به زمین رسید و قوی‌ترین چشمه رادیویی خارج از منظومه شمسی است.",
                "این صورت فلکی میزبان خوشه‌های باز M۵۲ و M۱۰۳ است و در امتداد نوار پرستاره راه شیری قرار دارد."
            )
        ),
        "const_cyg" to BilingualFacts(
            en = listOf(
                "Cygnus spans 804 square degrees, the 16th largest constellation.",
                "Deneb (Alpha Cygni), a white supergiant, is its brightest star at magnitude 1.25 and marks the tail of the swan.",
                "Its brightest stars form the Northern Cross, which lies along the band of the Milky Way.",
                "The constellation contains the North America Nebula (NGC 7000), the Veil Nebula, and the X-ray source Cygnus X-1, the first widely accepted stellar-mass black hole.",
                "Albireo (Beta Cygni), at the head of the swan, is one of the finest double stars for small telescopes, showing gold and blue components."
            ),
            fa = listOf(
                "صورت فلکی ماکیان با ۸۰۴ درجه مربع مساحت، شانزدهمین صورت بزرگ آسمان است.",
                "دِنب (آلفا ماکیان)، یک ابرغول سفید با قدر ۱/۲۵، درخشان‌ترین ستاره آن و نشانگر دم قو است.",
                "درخشان‌ترین ستارگان آن شکل‌دهنده صلیب شمالی هستند که در امتداد نوار راه شیری قرار دارد.",
                "این صورت فلکی میزبان سحابی آمریکای شمالی (NGC ۷۰۰۰)، سحابی پرده و چشمه پرتو ایکس ماکیان X-۱ است که نخستین سیاهچاله ستاره‌ای پذیرفته‌شده بود.",
                "آلبیرو (بتا ماکیان) در سر قو، یکی از زیباترین ستاره‌های دوتایی برای تلسکوپ‌های کوچک است که اجزای طلایی و آبی دارد."
            )
        ),
        "const_sco" to BilingualFacts(
            en = listOf(
                "Scorpius spans 497 square degrees, the 33rd largest constellation.",
                "Antares (Alpha Scorpii), a red supergiant, is its brightest star at magnitude 0.96 and rivals Mars in color.",
                "The constellation's hooked star chain clearly resembles a scorpion, with the stinger ending in the pair Shaula and Lesath.",
                "It contains the globular clusters M4 and M80 and the open clusters M6 (Butterfly) and M7 (Ptolemy).",
                "In Greek myth the scorpion was sent to kill Orion, which is why the two constellations sit on opposite sides of the sky."
            ),
            fa = listOf(
                "صورت فلکی عقرب با ۴۹۷ درجه مربع مساحت، سی‌وسومین صورت بزرگ آسمان است.",
                "قَلب‌العقرب (آلفا عقرب)، یک ابرغول سرخ با قدر ۰/۹۶، درخشان‌ترین ستاره آن است و از نظر رنگ با سیاره مریخ رقابت می‌کند.",
                "زنجیره قلاب‌مانند ستارگان این صورت فلکی به روشنی یک عقرب را تداعی می‌کند و نیش آن به جفت ستاره شَوله و لَسَث ختم می‌شود.",
                "این صورت فلکی میزبان خوشه‌های کروی M۴ و M۸۰ و خوشه‌های باز M۶ (پروانه) و M۷ (بطلمیوس) است.",
                "در اساطیر یونان، عقرب برای کشتن شکارچی فرستاده شد و به همین دلیل این دو صورت فلکی در دو سوی مخالف آسمان جای دارند."
            )
        ),
        "const_cma" to BilingualFacts(
            en = listOf(
                "Canis Major spans 380 square degrees, the 43rd largest constellation.",
                "It contains Sirius (Alpha Canis Majoris), the brightest star in Earth's night sky at magnitude -1.46.",
                "Sirius is a binary system whose companion, Sirius B, was the first white dwarf ever discovered, in 1862.",
                "The open cluster M41, visible to the naked eye in dark skies, lies about four degrees south of Sirius.",
                "In Greek myth Canis Major is one of Orion's hunting dogs, Laelaps, chasing the hare Lepus."
            ),
            fa = listOf(
                "صورت فلکی سگ بزرگ با ۳۸۰ درجه مربع مساحت، چهل‌وسومین صورت بزرگ آسمان است.",
                "این صورت فلکی میزبان شباهنگ (آلفا سگ بزرگ)، درخشان‌ترین ستاره آسمان شب زمین با قدر ۱/۴۶- است.",
                "شباهنگ یک سامانه دوتایی است و همدم آن، شباهنگ B، نخستین کوتوله سفید کشف‌شده در تاریخ (سال ۱۸۶۲) بود.",
                "خوشه باز M۴۱ که در آسمان تاریک با چشم غیرمسلح دیده می‌شود، حدود چهار درجه جنوب شباهنگ قرار دارد.",
                "در اساطیر یونان، سگ بزرگ یکی از سگ‌های شکاری شکارچی به نام لائلاپس است که خرگوش (Lepus) را دنبال می‌کند."
            )
        ),
        "const_tau" to BilingualFacts(
            en = listOf(
                "Taurus spans 797 square degrees, the 17th largest constellation.",
                "Aldebaran (Alpha Tauri), an orange giant, is its brightest star at magnitude 0.87 and marks the eye of the bull.",
                "The V-shaped Hyades cluster forms the bull's face, while the Pleiades (M45) sit on its shoulder.",
                "The Crab Nebula (M1), the remnant of the supernova observed in 1054, lies near the bull's southern horn.",
                "Named for the bull that carried Europa in Greek myth, Taurus is a zodiac constellation crossed by the Sun in spring."
            ),
            fa = listOf(
                "صورت فلکی گاو با ۷۹۷ درجه مربع مساحت، هفدهمین صورت بزرگ آسمان است.",
                "دَبَران (آلفا گاو)، یک غول نارنجی با قدر ۰/۸۷، درخشان‌ترین ستاره آن و نشانگر چشم گاو است.",
                "خوشه V-شکل قَلائص چهره گاو را می‌سازد و خوشه پروین (M۴۵) بر شانه آن جای دارد.",
                "سحابی خرچنگ (M۱)، بازمانده ابرنواختر مشاهده‌شده در سال ۱۰۵۴ میلادی، نزدیک شاخ جنوبی گاو قرار دارد.",
                "این صورت فلکی که در اساطیر یونان نام گاوی را دارد که اروپا را بر دوش کشید، از صورت‌های فلکی دایرةالبروجی است که خورشید در بهار از آن می‌گذرد."
            )
        ),
        "const_gem" to BilingualFacts(
            en = listOf(
                "Gemini spans 514 square degrees, the 30th largest constellation.",
                "Pollux (Beta Geminorum), an orange giant, is its brightest star at magnitude 1.14.",
                "The twin stars Castor and Pollux, the heads of the twins, give the constellation its name.",
                "It contains the open cluster M35 and the Eskimo Nebula (NGC 2392), a striking planetary nebula.",
                "Gemini is a zodiac constellation named for the twin brothers Castor and Pollux of Greek myth."
            ),
            fa = listOf(
                "صورت فلکی دوپیکر با ۵۱۴ درجه مربع مساحت، سی‌اُمین صورت بزرگ آسمان است.",
                "پولوکس (بتا دوپیکر)، یک غول نارنجی با قدر ۱/۱۴، درخشان‌ترین ستاره آن است.",
                "دو ستاره کاستور و پولوکس، سرهای دو پیکر، نام این صورت فلکی را ساخته‌اند.",
                "این صورت فلکی میزبان خوشه باز M۳۵ و سحابی سیاره‌نمای اسکیمو (NGC ۲۳۹۲) است.",
                "دوپیکر یک صورت فلکی دایرةالبروجی است که به نام برادران دوقلوی اساطیر یونان، کاستور و پولوکس، نام‌گذاری شده است."
            )
        ),
        "const_leo" to BilingualFacts(
            en = listOf(
                "Leo spans 947 square degrees, the 12th largest constellation.",
                "Regulus (Alpha Leonis), a blue-white main-sequence star, is its brightest star at magnitude 1.36 and marks the lion's heart.",
                "The asterism known as the Sickle, a backwards question mark, outlines the lion's head and mane.",
                "The constellation hosts the Leo Triplet (M65, M66, and NGC 3628) and the galaxy group M95/M96/M105.",
                "In Greek myth Leo is the Nemean lion slain by Heracles as the first of his twelve labors."
            ),
            fa = listOf(
                "صورت فلکی شیر با ۹۴۷ درجه مربع مساحت، دوازدهمین صورت بزرگ آسمان است.",
                "قَلب‌الاسد (آلفا شیر)، ستاره رشته اصلی آبی-سفید با قدر ۱/۳۶، درخشان‌ترین ستاره آن و نشانگر قلب شیر است.",
                "صورتواره داس که شبیه علامت سؤال وارونه است، سر و یال شیر را ترسیم می‌کند.",
                "این صورت فلکی میزبان سه‌قلوی شیر (M۶۵، M۶۶ و NGC ۳۶۲۸) و گروه کهکشانی M۹۵/M۹۶/M۱۰۵ است.",
                "در اساطیر یونان، شیر صورت فلکی همان شیر نیمیایی است که هراکلس در نخستین خان از دوازده‌خان خود آن را کشت."
            )
        ),
        "const_boo" to BilingualFacts(
            en = listOf(
                "Boötes spans 907 square degrees, the 13th largest constellation.",
                "Arcturus (Alpha Boötis), an orange giant, is its brightest star and the brightest star of the northern celestial hemisphere at magnitude -0.05.",
                "The radiant of the January Quadrantid meteor shower lies within Boötes, near the former constellation Quadrans Muralis.",
                "The constellation's kite-like shape stretches between the Big Dipper's handle and the crown Corona Borealis.",
                "Named the Herdsman or Ploughman, Boötes in myth follows the Great Bear around the celestial pole."
            ),
            fa = listOf(
                "صورت فلکی عوّاد با ۹۰۷ درجه مربع مساحت، سیزدهمین صورت بزرگ آسمان است.",
                "سِماک رامح (آلفا عوّاد)، یک غول نارنجی با قدر ۰/۰۵-، درخشان‌ترین ستاره آن و درخشان‌ترین ستاره نیمکره شمالی آسمان است.",
                "کانون بارش شهابی ربعی در دی‌ماه درون این صورت فلکی و نزدیک صورت فلکی منسوخ‌شده رُبع جداری (Quadrans Muralis) قرار دارد.",
                "شکل بادبادک‌مانند این صورت فلکی میان دسته ملاقه بزرگ و صورت فلکی تاج شمالی کشیده شده است.",
                "این صورت فلکی که شبان یا برزگر نامیده می‌شود، در اساطیر همواره در پی خرس بزرگ به دور قطب آسمان می‌گردد."
            )
        ),
        "const_vir" to BilingualFacts(
            en = listOf(
                "Virgo spans 1,294 square degrees, the second largest of all 88 constellations.",
                "Spica (Alpha Virginis), a hot blue binary, is its brightest star at magnitude 0.98.",
                "The constellation contains the heart of the Virgo Cluster, the nearest large galaxy cluster, including the giant elliptical M87 and the Sombrero Galaxy (M104) on its border.",
                "Virgo is the largest zodiac constellation, and the Sun passes through it around the September equinox.",
                "Named for the goddess of harvest and justice, Virgo is often depicted holding an ear of wheat marked by Spica."
            ),
            fa = listOf(
                "صورت فلکی دوشیزه با ۱۲۹۴ درجه مربع مساحت، دومین صورت بزرگ در میان ۸۸ صورت فلکی است.",
                "سِماک اَعزل (آلفا دوشیزه)، یک سامانه دوتایی داغ و آبی با قدر ۰/۹۸، درخشان‌ترین ستاره آن است.",
                "این صورت فلکی میزبان قلب خوشه دوشیزه، نزدیک‌ترین خوشه کهکشانی بزرگ، شامل کهکشان بیضوی غول‌پیکر M۸۷ و در حاشیه آن کهکشان کلاه‌مکزیکی (M۱۰۴) است.",
                "دوشیزه بزرگ‌ترین صورت فلکی دایرةالبروجی است و خورشید حوالی اعتدال پاییزی از آن می‌گذرد.",
                "این صورت فلکی به نام ایزدبانوی برداشت و دادگری نام‌گذاری شده و معمولاً خوشه گندمی را در دست دارد که ستاره سماک اعزل نشانگر آن است."
            )
        ),
        "const_lyr" to BilingualFacts(
            en = listOf(
                "Lyra spans 286 square degrees, the 52nd largest constellation.",
                "Vega (Alpha Lyrae) is its brightest star, at magnitude 0.03, and one of the three stars of the Summer Triangle.",
                "Vega was the historical zero-point of the photometric magnitude scale and will become the northern pole star around 12,000 CE due to axial precession.",
                "The Ring Nebula (M57), a famous planetary nebula, lies between the stars Sheliak and Sulafat.",
                "In Greek myth Lyra is the lyre of Orpheus, placed in the sky after his death."
            ),
            fa = listOf(
                "صورت فلکی شلیاق با ۲۸۶ درجه مربع مساحت، پنجاه‌ودومین صورت بزرگ آسمان است.",
                "نسر واقع (آلفا شلیاق) با قدر ۰/۰۳ درخشان‌ترین ستاره آن و یکی از سه ستاره مثلث تابستانی است.",
                "نسر واقع مبنای تاریخی نقطه صفر مقیاس قدر ظاهری بوده و به دلیل حرکت تقدیمی محور زمین، حدود سال ۱۲۰۰۰ میلادی ستاره قطبی شمال خواهد شد.",
                "سحابی حلقه (M۵۷)، یک سحابی سیاره‌نمای مشهور، میان دو ستاره شلیاق و سُلحفات قرار دارد.",
                "در اساطیر یونان، شلیاق همان چنگ اورفئوس است که پس از مرگ او در آسمان جای گرفت."
            )
        ),
        "const_aql" to BilingualFacts(
            en = listOf(
                "Aquila spans 652 square degrees, the 22nd largest constellation.",
                "Altair (Alpha Aquilae) is its brightest star, at magnitude 0.76, and one of the three stars of the Summer Triangle.",
                "Altair rotates so rapidly, completing a turn in about 9 hours, that it is visibly flattened at its poles.",
                "The constellation lies along the Milky Way and contains the dark nebula Barnard 142/143, the 'E' Nebula.",
                "In Greek myth Aquila is the eagle that carried Zeus's thunderbolts and once abducted Ganymede to Olympus."
            ),
            fa = listOf(
                "صورت فلکی عقاب با ۶۵۲ درجه مربع مساحت، بیست‌ودومین صورت بزرگ آسمان است.",
                "نَسر طایر (آلفا عقاب) با قدر ۰/۷۶ درخشان‌ترین ستاره آن و یکی از سه ستاره مثلث تابستانی است.",
                "نسر طایر چنان سریع به دور خود می‌چرخد که هر چرخش آن حدود ۹ ساعت طول می‌کشد و به همین دلیل در قطب‌های خود به شکل محسوسی پَخ شده است.",
                "این صورت فلکی در امتداد راه شیری قرار دارد و میزبان سحابی تاریک بارنارد ۱۴۲/۱۴۳ معروف به سحابی «E» است.",
                "در اساطیر یونان، عقاب همان پرنده‌ای است که صاعقه‌های زئوس را حمل می‌کرد و زمانی گانیمد را به المپ ربود."
            )
        ),
        "const_peg" to BilingualFacts(
            en = listOf(
                "Pegasus spans 1,121 square degrees, the 7th largest constellation.",
                "Its brightest star is Enif (Epsilon Pegasi), an orange supergiant at magnitude 2.38.",
                "The Great Square of Pegasus, formed by four stars, is one of the most recognizable autumn asterisms.",
                "The constellation contains the globular cluster M15, one of the densest known, with a possible intermediate-mass black hole at its core.",
                "Named for the winged horse of Greek myth, Pegasus was born from the blood of Medusa."
            ),
            fa = listOf(
                "صورت فلکی اسب بالدار با ۱۱۲۱ درجه مربع مساحت، هفتمین صورت بزرگ آسمان است.",
                "درخشان‌ترین ستاره آن عَنَف (اپسیلون اسب بالدار)، یک ابرغول نارنجی با قدر ۲/۳۸ است.",
                "مربع بزرگ اسب بالدار که از چهار ستاره ساخته شده، یکی از شناخته‌شده‌ترین صورتواره‌های پاییزی است.",
                "این صورت فلکی میزبان خوشه کروی M۱۵، یکی از متراکم‌ترین خوشه‌های کروی شناخته‌شده، است که شاید سیاهچاله‌ای با جرم متوسط در هسته خود داشته باشد.",
                "این صورت فلکی به نام اسب بالدار اساطیر یونان نام‌گذاری شده که از خون مدوسا زاده شد."
            )
        ),
        "const_and" to BilingualFacts(
            en = listOf(
                "Andromeda spans 722 square degrees, the 19th largest constellation.",
                "Its brightest star is Alpheratz (Alpha Andromedae), shared with the Great Square of Pegasus, at magnitude 2.07.",
                "The constellation contains the Andromeda Galaxy (M31), the nearest large spiral galaxy to the Milky Way, along with its companions M32 and M110.",
                "The Andromeda Galaxy is visible to the naked eye under dark skies as an elongated smudge of light.",
                "In Greek myth Andromeda was a princess chained to a rock as a sacrifice to a sea monster, then rescued by Perseus."
            ),
            fa = listOf(
                "صورت فلکی آندرومدا با ۷۲۲ درجه مربع مساحت، نوزدهمین صورت بزرگ آسمان است.",
                "درخشان‌ترین ستاره آن سِرّهالفَرَس (آلفا آندرومدا) با قدر ۲/۰۷ است که با مربع بزرگ اسب بالدار مشترک است.",
                "این صورت فلکی میزبان کهکشان آندرومدا (M۳۱)، نزدیک‌ترین کهکشان مارپیچی بزرگ به راه شیری، به همراه دو همدم آن M۳۲ و M۱۱۰ است.",
                "کهکشان آندرومدا در آسمان تاریک با چشم غیرمسلح به شکل لکه نورانی کشیده‌ای دیده می‌شود.",
                "در اساطیر یونان، آندرومدا شاهدختی بود که به صخره زنجیر شد تا قربانی هیولای دریایی شود و سرانجام پرسئوس او را نجات داد."
            )
        ),
        "const_sgr" to BilingualFacts(
            en = listOf(
                "Sagittarius spans 867 square degrees, the 15th largest constellation.",
                "Its brightest star is Kaus Australis (Epsilon Sagittarii), at magnitude 1.79.",
                "The constellation's brightest stars form the Teapot asterism, whose spout points toward the center of the Milky Way.",
                "Sagittarius contains the galactic center, home to the supermassive black hole Sagittarius A*, about 26,000 light-years away.",
                "It also hosts the Lagoon Nebula (M8), the Trifid Nebula (M20), the Omega Nebula (M17), and the bright globular cluster M22."
            ),
            fa = listOf(
                "صورت فلکی کمان با ۸۶۷ درجه مربع مساحت، پانزدهمین صورت بزرگ آسمان است.",
                "درخشان‌ترین ستاره آن قوس جنوبی (اپسیلون کمان) با قدر ۱/۷۹ است.",
                "درخشان‌ترین ستارگان این صورت فلکی صورتواره قوری را می‌سازند که دهانه آن به سوی مرکز راه شیری اشاره می‌کند.",
                "این صورت فلکی میزبان مرکز کهکشان و سیاهچاله کلان‌جرم کمان A* در فاصله حدود ۲۶,۰۰۰ سال نوری است.",
                "همچنین سحابی مرداب (M۸)، سحابی سه‌تکه (M۲۰)، سحابی اُمگا (M۱۷) و خوشه کروی درخشان M۲۲ در آن جای دارند."
            )
        ),
        "const_cen" to BilingualFacts(
            en = listOf(
                "Centaurus spans 1,060 square degrees, the 9th largest constellation.",
                "Its brightest star is Rigil Kentaurus (Alpha Centauri), the closest star system to the Sun at 4.37 light-years.",
                "Alpha Centauri is a triple system whose third member, Proxima Centauri, is the nearest individual star to the Sun.",
                "The constellation contains Omega Centauri (NGC 5139), the largest and brightest globular cluster in the Milky Way, and the peculiar galaxy Centaurus A (NGC 5128).",
                "In Greek myth Centaurus is Chiron, the wise centaur who tutored many heroes."
            ),
            fa = listOf(
                "صورت فلکی قنطورس با ۱۰۶۰ درجه مربع مساحت، نهمین صورت بزرگ آسمان است.",
                "درخشان‌ترین ستاره آن رِجِل قنطورس (آلفا قنطورس)، نزدیک‌ترین سامانه ستاره‌ای به خورشید در فاصله ۴/۳۷ سال نوری است.",
                "آلفا قنطورس یک سامانه سه‌تایی است و عضو سوم آن، پروکسیما قنطورس، نزدیک‌ترین ستاره منفرد به خورشید است.",
                "این صورت فلکی میزبان اُمگا قنطورس (NGC ۵۱۳۹)، بزرگ‌ترین و درخشان‌ترین خوشه کروی راه شیری، و کهکشان شگفت‌انگیز قنطورس A (NGC ۵۱۲۸) است.",
                "در اساطیر یونان، قنطورس همان کیرون، سنتور خردمندی است که آموزگار بسیاری از قهرمانان بود."
            )
        ),
        "const_car" to BilingualFacts(
            en = listOf(
                "Carina spans 494 square degrees, the 34th largest constellation.",
                "Canopus (Alpha Carinae) is its brightest star and the second brightest star in the night sky at magnitude -0.74.",
                "Carina was once part of the giant constellation Argo Navis, the ship of Jason and the Argonauts, later divided into Carina, Puppis, and Vela.",
                "The Carina Nebula (NGC 3372) is one of the largest diffuse nebulae known and surrounds the hypergiant star Eta Carinae.",
                "Eta Carinae is a massive binary that underwent a great eruption in the 1840s and may explode as a supernova in the astronomical near future."
            ),
            fa = listOf(
                "صورت فلکی شاه‌تخته با ۴۹۴ درجه مربع مساحت، سی‌وچهارمین صورت بزرگ آسمان است.",
                "سُهَیل (آلفا شاه‌تخته) درخشان‌ترین ستاره آن و دومین ستاره درخشان آسمان شب با قدر ۰/۷۴- است.",
                "شاه‌تخته روزگاری بخشی از صورت فلکی بزرگ کشتی آرگو، کشتی یاسون و آرگونوت‌ها بود که بعدها به سه بخش شاه‌تخته، کشتی‌دُم و بادبان تقسیم شد.",
                "سحابی شاه‌تخته (NGC ۳۳۷۲) یکی از بزرگ‌ترین سحابی‌های پخشی شناخته‌شده است و ستاره ابرغول اتا شاه‌تخته را در بر می‌گیرد.",
                "اتا شاه‌تخته یک سامانه دوتایی پرجرم است که در دهه ۱۸۴۰ فوران عظیمی داشت و ممکن است در آینده نزدیک نجومی به ابرنواختر تبدیل شود."
            )
        ),
        "const_cru" to BilingualFacts(
            en = listOf(
                "Crux spans only 68 square degrees, the smallest of all 88 constellations.",
                "Its brightest star is Acrux (Alpha Crucis), a multiple star system of magnitude 0.76.",
                "The four main stars of the Southern Cross point south, and an imaginary line through the cross's long axis extended about 4.5 times reaches near the south celestial pole.",
                "The constellation contains the Jewel Box (NGC 4755), a brilliant open cluster, and borders the Coalsack, a prominent dark nebula.",
                "Crux is featured on the flags of Australia, New Zealand, Brazil, and several other Southern Hemisphere nations."
            ),
            fa = listOf(
                "صورت فلکی صلیب جنوبی با تنها ۶۸ درجه مربع مساحت، کوچک‌ترین صورت فلکی در میان ۸۸ صورت فلکی است.",
                "درخشان‌ترین ستاره آن آکراکس (آلفا صلیب جنوبی)، یک سامانه چندستاره‌ای با قدر ۰/۷۶ است.",
                "چهار ستاره اصلی صلیب جنوبی به سوی جنوب اشاره می‌کنند و امتداد محور بلند صلیب به اندازه حدود ۴/۵ برابر آن، به نزدیکی قطب جنوب آسمان می‌رسد.",
                "این صورت فلکی میزبان خوشه باز درخشان جعبه جواهر (NGC ۴۷۵۵) است و در همسایگی سحابی تاریک کیسه زغال قرار دارد.",
                "صلیب جنوبی بر پرچم استرالیا، نیوزیلند، برزیل و چند کشور دیگر نیمکره جنوبی نقش بسته است."
            )
        )
    )
}
