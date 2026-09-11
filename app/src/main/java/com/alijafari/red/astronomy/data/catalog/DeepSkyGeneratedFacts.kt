package com.alijafari.red.astronomy.data.catalog

/**
 * Explicit bilingual fact coverage for engine-derived deep-sky objects that do not have
 * hand-authored narrative entries in DeepSkyContent. Each fact set was generated from
 * the audited object metadata and validated against the source-family policy recorded in
 * docs/dso-content-research-log.md. Facts intentionally avoid restating detail-page rows
 * such as magnitude, distance, angular size, physical size, and best viewing month.
 */
internal object DeepSkyGeneratedFacts {
    fun factsForCanonicalId(canonicalId: String): BilingualFacts? = generatedFactsByCanonicalId[canonicalId]

    private val generatedFactsByCanonicalId = mapOf(
        "dso_m1" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Crab Nebula (M1) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Crab Nebula (M1) is expanding stellar wreckage; shocks and energetic particles make it a laboratory for how explosions enrich interstellar gas.",
                "Historical supernova records tied to Crab Nebula (M1) make it one of the best calibrated links between an observed explosion and its remnant.",
                "Nebula filters can improve contrast on Crab Nebula (M1) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Crab Nebula (M1) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی خرچنگ (M1) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "سحابی خرچنگ (M1) بقایای گسترش‌یابنده یک انفجار ستاره‌ای است و برای بررسی شوک‌ها و غنی‌سازی گاز میان‌ستاره‌ای به‌کار می‌آید.",
                "ثبت‌های تاریخی ابرنواخترِ مرتبط با سحابی خرچنگ (M1) آن را به یکی از بهترین پیوندهای شناخته‌شده میان انفجار دیده‌شده و بقایای آن تبدیل کرده است.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی خرچنگ (M1) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی خرچنگ (M1) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_m2" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M2 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M2 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M2 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M2 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M2 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M2 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M2 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M2 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M2 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M2 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m3" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M3 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M3 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M3 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M3 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M3 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M3 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M3 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M3 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M3 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M3 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m4" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M4 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M4 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M4 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M4 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M4 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M4 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M4 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M4 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M4 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M4 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m5" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M5 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M5 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M5 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M5 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M5 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M5 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M5 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M5 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M5 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M5 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m6" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Butterfly Cluster (M6) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Butterfly Cluster (M6) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Butterfly Cluster (M6) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Butterfly Cluster (M6)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Butterfly Cluster (M6), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، خوشه پروانه (M6) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "خوشه پروانه (M6) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه پروانه (M6) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه پروانه (M6) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه پروانه (M6) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m7" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Ptolemy Cluster (M7) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Ptolemy Cluster (M7) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Ptolemy Cluster (M7) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Ptolemy Cluster (M7)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Ptolemy Cluster (M7), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، خوشه بطلمیوس (M7) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "خوشه بطلمیوس (M7) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه بطلمیوس (M7) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه بطلمیوس (M7) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه بطلمیوس (M7) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m9" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M9 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M9 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M9 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M9 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M9 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M9 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M9 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M9 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M9 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M9 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m10" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M10 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M10 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M10 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M10 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M10 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M10 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M10 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M10 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M10 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M10 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m11" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Wild Duck Cluster (M11) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Wild Duck Cluster (M11) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The rich field of Wild Duck Cluster (M11) lets photometric studies separate likely members from unrelated foreground and background stars.",
                "Wide-field views help show Wild Duck Cluster (M11)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Wild Duck Cluster (M11), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، خوشه اردک وحشی (M11) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "خوشه اردک وحشی (M11) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "میدان غنی خوشه اردک وحشی (M11) به بررسی‌های نوری اجازه می‌دهد اعضای محتمل خوشه از ستارگان پیش‌زمینه و پس‌زمینه جدا شوند.",
                "نمای میدان‌گسترده الگوی خوشه اردک وحشی (M11) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه اردک وحشی (M11) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m12" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M12 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M12 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M12 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M12 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M12 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M12 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M12 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M12 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M12 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M12 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m14" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M14 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M14 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M14 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M14 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M14 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M14 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M14 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M14 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M14 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M14 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m15" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M15 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M15 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M15 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M15 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M15 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M15 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M15 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M15 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M15 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M15 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m16" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Eagle Nebula (M16) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Ionized gas in Eagle Nebula (M16) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "The Pillars of Creation inside Eagle Nebula (M16) are dense molecular columns being eroded by nearby young massive stars.",
                "Nebula filters can improve contrast on Eagle Nebula (M16) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Eagle Nebula (M16) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی عقاب (M16) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "گاز یونیده در سحابی عقاب (M16) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "ستون‌های آفرینش درون سحابی عقاب (M16) ستون‌های مولکولی چگالی هستند که ستارگان جوان و پرجرم پیرامونشان آن‌ها را فرسایش می‌دهند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی عقاب (M16) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی عقاب (M16) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_m17" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Omega Nebula (M17) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Ionized gas in Omega Nebula (M17) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "Narrowband imaging of Omega Nebula (M17) separates hydrogen and oxygen emission, revealing structure that broadband views can hide.",
                "Nebula filters can improve contrast on Omega Nebula (M17) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Omega Nebula (M17) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی اومگا (M17) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "گاز یونیده در سحابی اومگا (M17) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "تصویربرداری باریک‌باند از سحابی اومگا (M17) گسیل هیدروژن و اکسیژن را جدا می‌کند و ساختارهایی را نشان می‌دهد که در نور پهن‌باند پنهان می‌مانند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی اومگا (M17) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی اومگا (M17) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_m18" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M18 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M18 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M18 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M18's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M18, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M18 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M18 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M18 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M18 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M18 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m19" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M19 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M19 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M19 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M19 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M19 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M19 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M19 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M19 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M19 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M19 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m20" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Trifid Nebula (M20) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Trifid Nebula (M20) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape.",
                "Dark lanes split Trifid Nebula (M20)'s bright core while neighboring blue reflection nebulosity gives the field its famous mixed character.",
                "Because Trifid Nebula (M20) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure.",
                "Multiwavelength images of Trifid Nebula (M20) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی سه‌تکه (M20) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "سحابی سه‌تکه (M20) نور ستارگان نزدیک را از روی دانه‌های غبار پراکنده می‌کند؛ بنابراین هندسه غبار شکل مرئی آن را تعیین می‌کند.",
                "رگه‌های تاریک هسته روشن سحابی سه‌تکه (M20) را می‌شکافند و سحابی بازتابی آبیِ کنارش، چهره ترکیبی مشهور میدان را می‌سازد.",
                "از آنجا که سحابی سه‌تکه (M20) عمدتاً بازتابی است، آسمان تاریک معمولاً بیش از فیلترهای باریک‌باند در دیدن جزئیات ظریف کمک می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی سه‌تکه (M20) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_m21" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M21 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M21 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M21 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M21's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M21, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M21 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M21 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M21 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M21 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M21 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m22" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Sagittarius Cluster (M22) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Sagittarius Cluster (M22) belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of Sagittarius Cluster (M22) requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes Sagittarius Cluster (M22) from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of Sagittarius Cluster (M22) separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، خوشه کمان (M22) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "خوشه کمان (M22) به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده خوشه کمان (M22) برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، خوشه کمان (M22) از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر خوشه کمان (M22) غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m23" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M23 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M23 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The broad pattern of M23 is best understood as a stellar association against the Milky Way background, not a single bright point.",
                "Wide-field views help show M23's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M23, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M23 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M23 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "الگوی گسترده M23 بیشتر به‌صورت انجمنی ستاره‌ای در زمینه راه شیری فهمیده می‌شود، نه یک نقطه درخشان منفرد.",
                "نمای میدان‌گسترده الگوی M23 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M23 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m24" to BilingualFacts(
            en = listOf(
                "Messier's entry for Sagittarius Star Cloud (M24) preserves a rich Milky Way star-cloud field rather than a single compact cluster or nebula.",
                "Sagittarius Star Cloud (M24) is a line-of-sight concentration of Milky Way stars, revealing how dust windows expose crowded spiral-arm fields.",
                "The catalog notes for Sagittarius Star Cloud (M24) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Wide-field views help show Sagittarius Star Cloud (M24)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Sagittarius Star Cloud (M24), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "ورودی مسیه برای ابر ستاره‌ای کمان (M24) میدان پرستاره‌ای از راه شیری را ثبت می‌کند، نه یک خوشه یا سحابی فشرده واحد.",
                "ابر ستاره‌ای کمان (M24) تمرکزی در امتداد دید از ستارگان راه شیری است و نشان می‌دهد پنجره‌های کم‌غبار چگونه میدان‌های بازوی مارپیچی را آشکار می‌کنند.",
                "یادداشت‌های کاتالوگی ابر ستاره‌ای کمان (M24) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "نمای میدان‌گسترده الگوی ابر ستاره‌ای کمان (M24) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون ابر ستاره‌ای کمان (M24) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m25" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M25 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M25 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M25 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M25's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M25, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M25 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M25 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M25 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M25 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M25 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m26" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M26 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M26 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M26 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M26's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M26, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M26 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M26 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M26 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M26 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M26 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m27" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Dumbbell Nebula (M27) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Dumbbell Nebula (M27) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The hourglass appearance of Dumbbell Nebula (M27) shows that planetary nebulae can be strongly bipolar rather than simple spherical shells.",
                "Nebula filters can improve contrast on Dumbbell Nebula (M27) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Dumbbell Nebula (M27) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی دمبل (M27) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "سحابی دمبل (M27) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "نمای ساعت‌شنی سحابی دمبل (M27) نشان می‌دهد سحابی‌های سیاره‌نما می‌توانند دوقطبی و نامتقارن باشند، نه فقط پوسته‌هایی کروی.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی دمبل (M27) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی دمبل (M27) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_m28" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M28 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M28 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M28 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M28 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M28 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M28 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M28 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M28 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M28 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M28 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m29" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Cooling Tower (M29) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Cooling Tower (M29) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Cooling Tower (M29) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Cooling Tower (M29)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Cooling Tower (M29), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، خوشه برج خنک‌کننده (M29) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "خوشه برج خنک‌کننده (M29) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه برج خنک‌کننده (M29) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه برج خنک‌کننده (M29) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه برج خنک‌کننده (M29) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m30" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M30 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M30 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M30 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M30 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M30 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M30 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M30 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M30 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M30 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M30 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m32" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M32 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M32 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "As an Andromeda companion, M32 helps map satellite-galaxy evolution and the merger history of the Local Group.",
                "For visual observers, M32 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M32 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M32 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M32 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "M32 به‌عنوان همدم آندرومدا در ترسیم تحول کهکشان‌های اقماری و تاریخ ادغام گروه محلی نقش دارد.",
                "برای رصد چشمی، M32 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M32 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m34" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M34 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M34 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M34 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M34's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M34, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M34 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M34 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M34 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M34 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M34 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m35" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M35 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M35 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The broad pattern of M35 is best understood as a stellar association against the Milky Way background, not a single bright point.",
                "Wide-field views help show M35's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M35, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M35 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M35 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "الگوی گسترده M35 بیشتر به‌صورت انجمنی ستاره‌ای در زمینه راه شیری فهمیده می‌شود، نه یک نقطه درخشان منفرد.",
                "نمای میدان‌گسترده الگوی M35 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M35 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m36" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Pinwheel Cluster (M36) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Pinwheel Cluster (M36) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Pinwheel Cluster (M36) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Pinwheel Cluster (M36)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Pinwheel Cluster (M36), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، خوشه فرفره‌ای (M36) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "خوشه فرفره‌ای (M36) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه فرفره‌ای (M36) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه فرفره‌ای (M36) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه فرفره‌ای (M36) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m37" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M37 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M37 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The rich field of M37 lets photometric studies separate likely members from unrelated foreground and background stars.",
                "Wide-field views help show M37's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M37, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M37 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M37 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "میدان غنی M37 به بررسی‌های نوری اجازه می‌دهد اعضای محتمل خوشه از ستارگان پیش‌زمینه و پس‌زمینه جدا شوند.",
                "نمای میدان‌گسترده الگوی M37 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M37 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m38" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Starfish Cluster (M38) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Starfish Cluster (M38) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Starfish Cluster (M38) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Starfish Cluster (M38)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Starfish Cluster (M38), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، خوشه ستاره‌دریایی (M38) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "خوشه ستاره‌دریایی (M38) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه ستاره‌دریایی (M38) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه ستاره‌دریایی (M38) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه ستاره‌دریایی (M38) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m39" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M39 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M39 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The broad pattern of M39 is best understood as a stellar association against the Milky Way background, not a single bright point.",
                "Wide-field views help show M39's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M39, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M39 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M39 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "الگوی گسترده M39 بیشتر به‌صورت انجمنی ستاره‌ای در زمینه راه شیری فهمیده می‌شود، نه یک نقطه درخشان منفرد.",
                "نمای میدان‌گسترده الگوی M39 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M39 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m40" to BilingualFacts(
            en = listOf(
                "M40 preserves a Messier-era catalog check: the reported nebulous object was absent, leaving a historical visual double-star entry.",
                "Winnecke 4 (M40) is a visual double-star target, reminding observers that historical deep-sky lists also preserve catalog curiosities.",
                "The catalog notes for Winnecke 4 (M40) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Wide-field views help show Winnecke 4 (M40)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Winnecke 4 (M40), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "M40 یادگار یک بررسی رصدی در عصر مسیه است؛ جرم سحابی گزارش‌شده پیدا نشد و یک جفت‌ستاره تاریخی در فهرست ماند.",
                "وینکه ۴ (M40) هدفی دوتایی برای رصد بصری است و یادآوری می‌کند فهرست‌های ژرف‌آسمان گاه کنجکاوی‌های تاریخی را نیز نگه داشته‌اند.",
                "یادداشت‌های کاتالوگی وینکه ۴ (M40) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "نمای میدان‌گسترده الگوی وینکه ۴ (M40) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون وینکه ۴ (M40) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m41" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M41 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M41 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M41 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M41's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M41, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M41 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M41 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M41 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M41 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M41 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m43" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, de Mairan's Nebula (M43) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Ionized gas in de Mairan's Nebula (M43) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "The complex shape of de Mairan's Nebula (M43) shows how winds, disks, or magnetic geometry can channel gas from an aging star.",
                "Nebula filters can improve contrast on de Mairan's Nebula (M43) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of de Mairan's Nebula (M43) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی دو مایران (M43) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "گاز یونیده در سحابی دو مایران (M43) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "شکل پیچیده سحابی دو مایران (M43) نشان می‌دهد بادها، قرص‌ها یا میدان‌های مغناطیسی می‌توانند گاز یک ستاره پیر را کانالیزه کنند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی دو مایران (M43) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی دو مایران (M43) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_m46" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M46 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M46 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The small planetary nebula seen in M46's field is usually treated as a striking line-of-sight overlay rather than a normal cluster member.",
                "Wide-field views help show M46's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M46, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M46 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M46 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "سحابی سیاره‌نمای کوچکی که در میدان M46 دیده می‌شود معمولاً هم‌پوشانی خط دید دانسته می‌شود، نه عضوی عادی از خوشه.",
                "نمای میدان‌گسترده الگوی M46 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M46 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m47" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M47 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M47 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M47 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M47's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M47, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M47 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M47 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M47 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M47 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M47 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m48" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M48 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M48 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The broad pattern of M48 is best understood as a stellar association against the Milky Way background, not a single bright point.",
                "Wide-field views help show M48's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M48, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M48 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M48 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "الگوی گسترده M48 بیشتر به‌صورت انجمنی ستاره‌ای در زمینه راه شیری فهمیده می‌شود، نه یک نقطه درخشان منفرد.",
                "نمای میدان‌گسترده الگوی M48 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M48 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m49" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M49 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M49 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "The bright inner regions of M49 make it useful for comparing visual impressions with photographs and modern detector images.",
                "For visual observers, M49 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M49 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M49 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M49 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "ناحیه‌های درونی روشن M49 مقایسه برداشت چشمی با عکس‌ها و تصویرهای آشکارسازهای امروزی را آسان می‌کند.",
                "برای رصد چشمی، M49 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M49 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m50" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M50 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M50 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M50 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M50's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M50, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M50 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M50 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M50 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M50 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M50 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m51" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Whirlpool Galaxy (M51) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Whirlpool Galaxy (M51)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The face-on view of Whirlpool Galaxy (M51) lets images trace spiral arms and star-forming regions without severe disk foreshortening.",
                "For visual observers, Whirlpool Galaxy (M51) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Whirlpool Galaxy (M51) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان گرداب (M51) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی کهکشان گرداب (M51) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای روبه‌روی کهکشان گرداب (M51) اجازه می‌دهد بازوهای مارپیچی و نواحی ستاره‌زا بدون کوتاه‌شدگی شدید قرص دنبال شوند.",
                "برای رصد چشمی، کهکشان گرداب (M51) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان گرداب (M51) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m52" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M52 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M52 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M52 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M52's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M52, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M52 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M52 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M52 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M52 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M52 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m53" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M53 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M53 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M53 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M53 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M53 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M53 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M53 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M53 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M53 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M53 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m54" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M54 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M54 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M54 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M54 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M54 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M54 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M54 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M54 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M54 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M54 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m55" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M55 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M55 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M55 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M55 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M55 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M55 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M55 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M55 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M55 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M55 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m56" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M56 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M56 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M56 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M56 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M56 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M56 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M56 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M56 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M56 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M56 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m57" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Ring Nebula (M57) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Ring Nebula (M57) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The ring shape of Ring Nebula (M57) is a projection effect; three-dimensional studies describe a shell viewed from a favorable angle.",
                "Nebula filters can improve contrast on Ring Nebula (M57) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Ring Nebula (M57) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی حلقه (M57) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "سحابی حلقه (M57) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "حلقه دیده‌شده در سحابی حلقه (M57) اثر زاویه دید است؛ مدل‌های سه‌بعدی آن را پوسته‌ای می‌دانند که از زاویه مناسب دیده می‌شود.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی حلقه (M57) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی حلقه (M57) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_m58" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M58 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M58's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bar in M58 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity.",
                "For visual observers, M58 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M58 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M58 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M58 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "میله مرکزی M58 می‌تواند گاز را به درون براند و پیوند ساختار مارپیچی با ستاره‌زایی یا فعالیت مرکزی را نشان دهد.",
                "برای رصد چشمی، M58 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M58 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m59" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M59 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M59 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "The catalog notes for M59 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M59 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M59 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M59 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M59 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "یادداشت‌های کاتالوگی M59 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M59 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M59 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m60" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M60 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M60 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "As part of the Virgo galaxy environment, M60 is studied alongside tidal encounters, ram pressure, and cluster gas effects.",
                "For visual observers, M60 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M60 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M60 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M60 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "M60 در محیط خوشه سنبله همراه با کشندهای کهکشانی، فشار گاز خوشه‌ای و تغییر شکل قرص‌ها بررسی می‌شود.",
                "برای رصد چشمی، M60 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M60 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m61" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M61 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M61's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "As part of the Virgo galaxy environment, M61 is studied alongside tidal encounters, ram pressure, and cluster gas effects.",
                "For visual observers, M61 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M61 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M61 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M61 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "M61 در محیط خوشه سنبله همراه با کشندهای کهکشانی، فشار گاز خوشه‌ای و تغییر شکل قرص‌ها بررسی می‌شود.",
                "برای رصد چشمی، M61 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M61 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m62" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M62 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M62 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M62 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M62 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M62 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M62 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M62 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M62 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M62 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M62 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m63" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Sunflower Galaxy (M63) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Sunflower Galaxy (M63)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for Sunflower Galaxy (M63) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, Sunflower Galaxy (M63) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Sunflower Galaxy (M63) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان آفتابگردان (M63) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی کهکشان آفتابگردان (M63) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی کهکشان آفتابگردان (M63) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، کهکشان آفتابگردان (M63) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان آفتابگردان (M63) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m64" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Black Eye Galaxy (M64) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Black Eye Galaxy (M64)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for Black Eye Galaxy (M64) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, Black Eye Galaxy (M64) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Black Eye Galaxy (M64) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان چشم سیاه (M64) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی کهکشان چشم سیاه (M64) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی کهکشان چشم سیاه (M64) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، کهکشان چشم سیاه (M64) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان چشم سیاه (M64) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m65" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M65 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M65's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "In the Leo Triplet field, M65 is observed with neighboring galaxies whose tidal history reshapes disks and faint outer structure.",
                "For visual observers, M65 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M65 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M65 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M65 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "M65 در میدان سه‌گانه شیر با همسایه‌هایی دیده می‌شود که تاریخ کشندی‌شان قرص‌ها و ساختارهای کم‌نور را دگرگون کرده است.",
                "برای رصد چشمی، M65 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M65 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m66" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M66 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M66's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "In the Leo Triplet field, M66 is observed with neighboring galaxies whose tidal history reshapes disks and faint outer structure.",
                "For visual observers, M66 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M66 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M66 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M66 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "M66 در میدان سه‌گانه شیر با همسایه‌هایی دیده می‌شود که تاریخ کشندی‌شان قرص‌ها و ساختارهای کم‌نور را دگرگون کرده است.",
                "برای رصد چشمی، M66 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M66 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m67" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M67 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M67 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "As an older open cluster, M67 helps trace how Milky Way disk clusters lose members and survive Galactic tides.",
                "Wide-field views help show M67's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M67, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M67 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M67 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "M67 به‌عنوان خوشه باز کهنسال، چگونگی از دست دادن اعضا و پایداری خوشه‌های قرص راه شیری در برابر کشندها را پیگیری می‌کند.",
                "نمای میدان‌گسترده الگوی M67 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M67 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m68" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M68 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M68 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M68 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M68 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M68 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M68 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M68 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M68 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M68 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M68 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m69" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M69 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M69 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M69 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M69 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M69 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M69 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M69 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M69 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M69 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M69 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m70" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M70 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M70 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M70 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M70 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M70 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M70 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M70 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M70 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M70 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M70 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m71" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M71 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M71 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M71 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M71 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M71 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M71 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M71 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M71 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M71 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M71 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m72" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M72 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M72 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M72 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M72 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M72 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M72 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M72 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M72 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M72 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M72 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m73" to BilingualFacts(
            en = listOf(
                "M73 preserves a Messier-era puzzle: modern astrometry treats the tiny grouping as a chance alignment, not a bound cluster.",
                "M73 Asterism (M73) is a line-of-sight concentration of Milky Way stars, revealing how dust windows expose crowded spiral-arm fields.",
                "Modern proper-motion measurements support M73 Asterism (M73) as an asterism: the stars share a line of sight more than a common birthplace.",
                "Wide-field views help show M73 Asterism (M73)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M73 Asterism (M73), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "M73 یادگار یک معمای عصر مسیه است؛ اخترسنجی نوین این گروه کوچک را هم‌راستایی تصادفی می‌داند، نه خوشه‌ای پیوسته.",
                "صورتواره M73 (M73) تمرکزی در امتداد دید از ستارگان راه شیری است و نشان می‌دهد پنجره‌های کم‌غبار چگونه میدان‌های بازوی مارپیچی را آشکار می‌کنند.",
                "اندازه‌گیری‌های حرکت خاص نوین نشان می‌دهد صورتواره M73 (M73) صورتواره‌ای دیداری است؛ ستارگان آن بیش از زادگاه مشترک، خط دید مشترک دارند.",
                "نمای میدان‌گسترده الگوی صورتواره M73 (M73) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون صورتواره M73 (M73) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m74" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M74 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M74's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The face-on view of M74 lets images trace spiral arms and star-forming regions without severe disk foreshortening.",
                "For visual observers, M74 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M74 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M74 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M74 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای روبه‌روی M74 اجازه می‌دهد بازوهای مارپیچی و نواحی ستاره‌زا بدون کوتاه‌شدگی شدید قرص دنبال شوند.",
                "برای رصد چشمی، M74 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M74 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m75" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M75 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M75 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M75 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M75 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M75 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M75 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M75 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M75 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M75 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M75 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m76" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Little Dumbbell (M76) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Little Dumbbell (M76) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The hourglass appearance of Little Dumbbell (M76) shows that planetary nebulae can be strongly bipolar rather than simple spherical shells.",
                "Nebula filters can improve contrast on Little Dumbbell (M76) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Little Dumbbell (M76) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی دمبل کوچک (M76) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "سحابی دمبل کوچک (M76) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "نمای ساعت‌شنی سحابی دمبل کوچک (M76) نشان می‌دهد سحابی‌های سیاره‌نما می‌توانند دوقطبی و نامتقارن باشند، نه فقط پوسته‌هایی کروی.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی دمبل کوچک (M76) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی دمبل کوچک (M76) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_m77" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Cetus A (M77) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Cetus A (M77)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The active nucleus in Cetus A (M77) marks gas falling toward a central supermassive black hole, producing strong emission-line signatures.",
                "For visual observers, Cetus A (M77) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Cetus A (M77) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، قیطس ای (M77) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی قیطس ای (M77) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "هسته فعال قیطس ای (M77) نشانگر فروریختن گاز به سوی سیاه‌چاله‌ای کلان‌جرم است و خطوط گسیلی نیرومند پدید می‌آورد.",
                "برای رصد چشمی، قیطس ای (M77) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای قیطس ای (M77) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m78" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M78 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M78 shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape.",
                "Because M78 is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars.",
                "Because M78 is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure.",
                "Multiwavelength images of M78 compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M78 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M78 نور ستارگان نزدیک را از روی دانه‌های غبار پراکنده می‌کند؛ بنابراین هندسه غبار شکل مرئی آن را تعیین می‌کند.",
                "چون M78 بیشتر با نور بازتابی می‌درخشد، رنگ و کنتراست آن به دانه‌های غبار و ستارگان روشن‌کننده وابسته است.",
                "از آنجا که M78 عمدتاً بازتابی است، آسمان تاریک معمولاً بیش از فیلترهای باریک‌باند در دیدن جزئیات ظریف کمک می‌کند.",
                "تصویرهای چندطولی‌موج از M78 گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_m79" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M79 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M79 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M79 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M79 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M79 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M79 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M79 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M79 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M79 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M79 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m80" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M80 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M80 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M80 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M80 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M80 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M80 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M80 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M80 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M80 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M80 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m81" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Bode's Galaxy (M81) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Bode's Galaxy (M81)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bright inner regions of Bode's Galaxy (M81) make it useful for comparing visual impressions with photographs and modern detector images.",
                "For visual observers, Bode's Galaxy (M81) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Bode's Galaxy (M81) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان بوده (M81) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی کهکشان بوده (M81) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "ناحیه‌های درونی روشن کهکشان بوده (M81) مقایسه برداشت چشمی با عکس‌ها و تصویرهای آشکارسازهای امروزی را آسان می‌کند.",
                "برای رصد چشمی، کهکشان بوده (M81) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان بوده (M81) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m82" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Cigar Galaxy (M82) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "The irregular form of Cigar Galaxy (M82) records a disturbed mix of stars and gas, often shaped by interactions and uneven star formation.",
                "Starburst activity in Cigar Galaxy (M82) drives winds and filaments that reveal feedback from concentrated massive-star formation.",
                "For visual observers, Cigar Galaxy (M82) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Cigar Galaxy (M82) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان سیگار (M82) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "شکل نامنظم کهکشان سیگار (M82) آمیزه‌ای آشفته از ستاره و گاز را نشان می‌دهد که اغلب از برهم‌کنش‌ها و ستاره‌زایی نابرابر اثر گرفته است.",
                "ستاره‌زایی انفجاری در کهکشان سیگار (M82) بادها و رشته‌هایی ایجاد می‌کند که بازخورد ستارگان پرجرم را آشکار می‌سازد.",
                "برای رصد چشمی، کهکشان سیگار (M82) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان سیگار (M82) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m83" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Southern Pinwheel (M83) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Southern Pinwheel (M83)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The face-on view of Southern Pinwheel (M83) lets images trace spiral arms and star-forming regions without severe disk foreshortening.",
                "For visual observers, Southern Pinwheel (M83) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Southern Pinwheel (M83) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، فرفره جنوبی (M83) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی فرفره جنوبی (M83) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای روبه‌روی فرفره جنوبی (M83) اجازه می‌دهد بازوهای مارپیچی و نواحی ستاره‌زا بدون کوتاه‌شدگی شدید قرص دنبال شوند.",
                "برای رصد چشمی، فرفره جنوبی (M83) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای فرفره جنوبی (M83) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m84" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M84 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M84 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "As part of the Virgo galaxy environment, M84 is studied alongside tidal encounters, ram pressure, and cluster gas effects.",
                "For visual observers, M84 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M84 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M84 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M84 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "M84 در محیط خوشه سنبله همراه با کشندهای کهکشانی، فشار گاز خوشه‌ای و تغییر شکل قرص‌ها بررسی می‌شود.",
                "برای رصد چشمی، M84 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M84 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m85" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M85 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M85's lenticular form bridges spiral and elliptical systems, with a disk-like outline but subdued spiral-arm star formation.",
                "The catalog notes for M85 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M85 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M85 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M85 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ریخت عدسی‌گون M85 میان کهکشان‌های مارپیچی و بیضوی قرار می‌گیرد؛ قرصی دیده می‌شود اما بازوهای ستاره‌زای پررنگ ندارد.",
                "یادداشت‌های کاتالوگی M85 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M85 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M85 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m86" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M86 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M86 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "As part of the Virgo galaxy environment, M86 is studied alongside tidal encounters, ram pressure, and cluster gas effects.",
                "For visual observers, M86 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M86 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M86 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M86 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "M86 در محیط خوشه سنبله همراه با کشندهای کهکشانی، فشار گاز خوشه‌ای و تغییر شکل قرص‌ها بررسی می‌شود.",
                "برای رصد چشمی، M86 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M86 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m87" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Virgo A (M87) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of Virgo A (M87) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "The relativistic jet associated with Virgo A (M87) makes it a benchmark for studying how black holes inject energy into surrounding gas.",
                "For visual observers, Virgo A (M87) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Virgo A (M87) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سنبله ای (M87) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه سنبله ای (M87) بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "فواره نسبیتی مرتبط با سنبله ای (M87) آن را به نمونه‌ای معیار برای بررسی تزریق انرژی سیاه‌چاله‌ها در گاز پیرامون تبدیل کرده است.",
                "برای رصد چشمی، سنبله ای (M87) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای سنبله ای (M87) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m88" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M88 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M88's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for M88 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M88 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M88 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M88 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M88 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی M88 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M88 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M88 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m89" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M89 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M89 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "As part of the Virgo galaxy environment, M89 is studied alongside tidal encounters, ram pressure, and cluster gas effects.",
                "For visual observers, M89 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M89 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M89 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M89 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "M89 در محیط خوشه سنبله همراه با کشندهای کهکشانی، فشار گاز خوشه‌ای و تغییر شکل قرص‌ها بررسی می‌شود.",
                "برای رصد چشمی، M89 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M89 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m90" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M90 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M90's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "As part of the Virgo galaxy environment, M90 is studied alongside tidal encounters, ram pressure, and cluster gas effects.",
                "For visual observers, M90 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M90 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M90 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M90 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "M90 در محیط خوشه سنبله همراه با کشندهای کهکشانی، فشار گاز خوشه‌ای و تغییر شکل قرص‌ها بررسی می‌شود.",
                "برای رصد چشمی، M90 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M90 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m91" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M91 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M91's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bar in M91 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity.",
                "For visual observers, M91 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M91 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M91 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M91 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "میله مرکزی M91 می‌تواند گاز را به درون براند و پیوند ساختار مارپیچی با ستاره‌زایی یا فعالیت مرکزی را نشان دهد.",
                "برای رصد چشمی، M91 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M91 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m92" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M92 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M92 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M92 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M92 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M92 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M92 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M92 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M92 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M92 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M92 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m93" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M93 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M93 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M93 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M93's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M93, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M93 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M93 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M93 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M93 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M93 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m94" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M94 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M94's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for M94 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M94 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M94 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M94 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M94 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی M94 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M94 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M94 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m95" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M95 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M95's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bar in M95 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity.",
                "For visual observers, M95 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M95 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M95 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M95 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "میله مرکزی M95 می‌تواند گاز را به درون براند و پیوند ساختار مارپیچی با ستاره‌زایی یا فعالیت مرکزی را نشان دهد.",
                "برای رصد چشمی، M95 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M95 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m96" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M96 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M96's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for M96 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M96 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M96 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M96 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M96 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی M96 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M96 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M96 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m97" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Owl Nebula (M97) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Owl Nebula (M97) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Owl Nebula (M97) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Owl Nebula (M97) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Owl Nebula (M97) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، سحابی جغد (M97) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "سحابی جغد (M97) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی سحابی جغد (M97) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی جغد (M97) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی جغد (M97) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_m98" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M98 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M98's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for M98 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M98 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M98 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M98 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M98 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی M98 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M98 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M98 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m99" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M99 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M99's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The face-on view of M99 lets images trace spiral arms and star-forming regions without severe disk foreshortening.",
                "For visual observers, M99 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M99 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M99 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M99 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای روبه‌روی M99 اجازه می‌دهد بازوهای مارپیچی و نواحی ستاره‌زا بدون کوتاه‌شدگی شدید قرص دنبال شوند.",
                "برای رصد چشمی، M99 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M99 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m100" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M100 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M100's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "As part of the Virgo galaxy environment, M100 is studied alongside tidal encounters, ram pressure, and cluster gas effects.",
                "For visual observers, M100 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M100 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M100 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M100 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "M100 در محیط خوشه سنبله همراه با کشندهای کهکشانی، فشار گاز خوشه‌ای و تغییر شکل قرص‌ها بررسی می‌شود.",
                "برای رصد چشمی، M100 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M100 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m101" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Pinwheel Galaxy (M101) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Pinwheel Galaxy (M101)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The face-on view of Pinwheel Galaxy (M101) lets images trace spiral arms and star-forming regions without severe disk foreshortening.",
                "For visual observers, Pinwheel Galaxy (M101) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Pinwheel Galaxy (M101) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان فرفره (M101) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی کهکشان فرفره (M101) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای روبه‌روی کهکشان فرفره (M101) اجازه می‌دهد بازوهای مارپیچی و نواحی ستاره‌زا بدون کوتاه‌شدگی شدید قرص دنبال شوند.",
                "برای رصد چشمی، کهکشان فرفره (M101) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان فرفره (M101) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m102" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Spindle Galaxy (M102) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Spindle Galaxy (M102)'s lenticular form bridges spiral and elliptical systems, with a disk-like outline but subdued spiral-arm star formation.",
                "The edge-on view of Spindle Galaxy (M102) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure.",
                "For visual observers, Spindle Galaxy (M102) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Spindle Galaxy (M102) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان دوک (M102) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ریخت عدسی‌گون کهکشان دوک (M102) میان کهکشان‌های مارپیچی و بیضوی قرار می‌گیرد؛ قرصی دیده می‌شود اما بازوهای ستاره‌زای پررنگ ندارد.",
                "نمای لبه‌به‌لبه کهکشان دوک (M102) بررسی نوارهای غبار و ضخامت قرص را برای مقایسه با مدل‌های ساختار کهکشانی آسان‌تر می‌کند.",
                "برای رصد چشمی، کهکشان دوک (M102) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان دوک (M102) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m103" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M103 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M103 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of M103 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show M103's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around M103, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M103 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M103 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای M103 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی M103 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون M103 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_m104" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Sombrero Galaxy (M104) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Sombrero Galaxy (M104)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for Sombrero Galaxy (M104) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, Sombrero Galaxy (M104) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Sombrero Galaxy (M104) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان کلاه‌مکزیکی (M104) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی کهکشان کلاه‌مکزیکی (M104) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی کهکشان کلاه‌مکزیکی (M104) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، کهکشان کلاه‌مکزیکی (M104) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان کلاه‌مکزیکی (M104) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m105" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M105 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M105 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "The catalog notes for M105 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M105 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M105 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M105 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M105 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "یادداشت‌های کاتالوگی M105 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M105 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M105 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m106" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M106 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M106's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for M106 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M106 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M106 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M106 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M106 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی M106 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M106 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M106 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m107" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M107 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M107 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of M107 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes M107 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of M107 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M107 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "M107 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده M107 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، M107 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر M107 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_m108" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, Surfboard Galaxy (M108) was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Surfboard Galaxy (M108)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The edge-on view of Surfboard Galaxy (M108) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure.",
                "For visual observers, Surfboard Galaxy (M108) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Surfboard Galaxy (M108) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، کهکشان تخته‌موج‌سواری (M108) همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی کهکشان تخته‌موج‌سواری (M108) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای لبه‌به‌لبه کهکشان تخته‌موج‌سواری (M108) بررسی نوارهای غبار و ضخامت قرص را برای مقایسه با مدل‌های ساختار کهکشانی آسان‌تر می‌کند.",
                "برای رصد چشمی، کهکشان تخته‌موج‌سواری (M108) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان تخته‌موج‌سواری (M108) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m109" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M109 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "M109's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bar in M109 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity.",
                "For visual observers, M109 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M109 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M109 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "ساختار قرصی و مارپیچی M109 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "میله مرکزی M109 می‌تواند گاز را به درون براند و پیوند ساختار مارپیچی با ستاره‌زایی یا فعالیت مرکزی را نشان دهد.",
                "برای رصد چشمی، M109 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M109 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_m110" to BilingualFacts(
            en = listOf(
                "In Messier's comet-hunting context, M110 was the kind of fixed deep-sky glow observers needed to recognize instead of a comet.",
                "Studies of M110 emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "The catalog notes for M110 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, M110 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for M110 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "در زمینه دنباله‌دارجویی مسیه، M110 همان درخشش ثابت ژرف‌آسمانی بود که رصدگران باید آن را با دنباله‌دار اشتباه نمی‌گرفتند.",
                "مطالعه M110 بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "یادداشت‌های کاتالوگی M110 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، M110 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای M110 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_6960" to BilingualFacts(
            en = listOf(
                "The NGC identity of Veil Nebula (West) (NGC 6960) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Veil Nebula (West) (NGC 6960) is expanding stellar wreckage; shocks and energetic particles make it a laboratory for how explosions enrich interstellar gas.",
                "The catalog notes for Veil Nebula (West) (NGC 6960) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Veil Nebula (West) (NGC 6960) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Veil Nebula (West) (NGC 6960) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی حجاب غربی (NGC 6960) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی حجاب غربی (NGC 6960) بقایای گسترش‌یابنده یک انفجار ستاره‌ای است و برای بررسی شوک‌ها و غنی‌سازی گاز میان‌ستاره‌ای به‌کار می‌آید.",
                "یادداشت‌های کاتالوگی سحابی حجاب غربی (NGC 6960) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی حجاب غربی (NGC 6960) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی حجاب غربی (NGC 6960) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_6992" to BilingualFacts(
            en = listOf(
                "The NGC identity of Veil Nebula (East) (NGC 6992) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Veil Nebula (East) (NGC 6992) is expanding stellar wreckage; shocks and energetic particles make it a laboratory for how explosions enrich interstellar gas.",
                "The catalog notes for Veil Nebula (East) (NGC 6992) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Veil Nebula (East) (NGC 6992) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Veil Nebula (East) (NGC 6992) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی حجاب شرقی (NGC 6992) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی حجاب شرقی (NGC 6992) بقایای گسترش‌یابنده یک انفجار ستاره‌ای است و برای بررسی شوک‌ها و غنی‌سازی گاز میان‌ستاره‌ای به‌کار می‌آید.",
                "یادداشت‌های کاتالوگی سحابی حجاب شرقی (NGC 6992) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی حجاب شرقی (NGC 6992) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی حجاب شرقی (NGC 6992) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_7293" to BilingualFacts(
            en = listOf(
                "The NGC identity of Helix Nebula (NGC 7293) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Helix Nebula (NGC 7293) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Helix Nebula (NGC 7293) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Helix Nebula (NGC 7293) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Helix Nebula (NGC 7293) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی مارپیچ (NGC 7293) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی مارپیچ (NGC 7293) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی سحابی مارپیچ (NGC 7293) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی مارپیچ (NGC 7293) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی مارپیچ (NGC 7293) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_2392" to BilingualFacts(
            en = listOf(
                "The NGC identity of Eskimo Nebula (NGC 2392) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Eskimo Nebula (NGC 2392) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Eskimo Nebula (NGC 2392) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Eskimo Nebula (NGC 2392) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Eskimo Nebula (NGC 2392) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی اسکیمو (NGC 2392) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی اسکیمو (NGC 2392) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی سحابی اسکیمو (NGC 2392) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی اسکیمو (NGC 2392) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی اسکیمو (NGC 2392) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_7009" to BilingualFacts(
            en = listOf(
                "The NGC identity of Saturn Nebula (NGC 7009) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Saturn Nebula (NGC 7009) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Saturn Nebula (NGC 7009) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Saturn Nebula (NGC 7009) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Saturn Nebula (NGC 7009) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی زحل (NGC 7009) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی زحل (NGC 7009) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی سحابی زحل (NGC 7009) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی زحل (NGC 7009) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی زحل (NGC 7009) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_2244" to BilingualFacts(
            en = listOf(
                "The NGC identity of Rosette Nebula Cluster (NGC 2244) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Rosette Nebula Cluster (NGC 2244) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Rosette Nebula Cluster (NGC 2244) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Rosette Nebula Cluster (NGC 2244)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Rosette Nebula Cluster (NGC 2244), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای خوشه سحابی رزت (NGC 2244) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "خوشه سحابی رزت (NGC 2244) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه سحابی رزت (NGC 2244) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه سحابی رزت (NGC 2244) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه سحابی رزت (NGC 2244) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2237" to BilingualFacts(
            en = listOf(
                "The NGC identity of Rosette Nebula (NGC 2237) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Ionized gas in Rosette Nebula (NGC 2237) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "Narrowband imaging of Rosette Nebula (NGC 2237) separates hydrogen and oxygen emission, revealing structure that broadband views can hide.",
                "Nebula filters can improve contrast on Rosette Nebula (NGC 2237) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Rosette Nebula (NGC 2237) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی رزت (NGC 2237) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "گاز یونیده در سحابی رزت (NGC 2237) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "تصویربرداری باریک‌باند از سحابی رزت (NGC 2237) گسیل هیدروژن و اکسیژن را جدا می‌کند و ساختارهایی را نشان می‌دهد که در نور پهن‌باند پنهان می‌مانند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی رزت (NGC 2237) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی رزت (NGC 2237) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_2264" to BilingualFacts(
            en = listOf(
                "The NGC identity of Christmas Tree Cluster (NGC 2264) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Christmas Tree Cluster (NGC 2264) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Christmas Tree Cluster (NGC 2264) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Christmas Tree Cluster (NGC 2264)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Christmas Tree Cluster (NGC 2264), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای خوشه درخت کریسمس (NGC 2264) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "خوشه درخت کریسمس (NGC 2264) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه درخت کریسمس (NGC 2264) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه درخت کریسمس (NGC 2264) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه درخت کریسمس (NGC 2264) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2070" to BilingualFacts(
            en = listOf(
                "The NGC identity of Tarantula Nebula (NGC 2070) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Ionized gas in Tarantula Nebula (NGC 2070) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "Narrowband imaging of Tarantula Nebula (NGC 2070) separates hydrogen and oxygen emission, revealing structure that broadband views can hide.",
                "Nebula filters can improve contrast on Tarantula Nebula (NGC 2070) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Tarantula Nebula (NGC 2070) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی رتیل (NGC 2070) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "گاز یونیده در سحابی رتیل (NGC 2070) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "تصویربرداری باریک‌باند از سحابی رتیل (NGC 2070) گسیل هیدروژن و اکسیژن را جدا می‌کند و ساختارهایی را نشان می‌دهد که در نور پهن‌باند پنهان می‌مانند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی رتیل (NGC 2070) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی رتیل (NGC 2070) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_2808" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2808 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2808 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of NGC 2808 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes NGC 2808 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of NGC 2808 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2808 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2808 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده NGC 2808 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، NGC 2808 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر NGC 2808 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_ngc_6752" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6752 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6752 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of NGC 6752 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes NGC 6752 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of NGC 6752 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6752 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6752 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده NGC 6752 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، NGC 6752 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر NGC 6752 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_ngc_6397" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6397 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6397 belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of NGC 6397 requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes NGC 6397 from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of NGC 6397 separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6397 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6397 به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده NGC 6397 برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، NGC 6397 از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر NGC 6397 غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_ngc_3532" to BilingualFacts(
            en = listOf(
                "The NGC identity of Wishing Well Cluster (NGC 3532) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Wishing Well Cluster (NGC 3532) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Wishing Well Cluster (NGC 3532) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Wishing Well Cluster (NGC 3532)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Wishing Well Cluster (NGC 3532), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای خوشه چاه آرزو (NGC 3532) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "خوشه چاه آرزو (NGC 3532) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه چاه آرزو (NGC 3532) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه چاه آرزو (NGC 3532) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه چاه آرزو (NGC 3532) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_4755" to BilingualFacts(
            en = listOf(
                "The NGC identity of Jewel Box Cluster (NGC 4755) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Jewel Box Cluster (NGC 4755) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Jewel Box Cluster (NGC 4755) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Jewel Box Cluster (NGC 4755)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Jewel Box Cluster (NGC 4755), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای خوشه جعبه جواهر (NGC 4755) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "خوشه جعبه جواهر (NGC 4755) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه جعبه جواهر (NGC 4755) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه جعبه جواهر (NGC 4755) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه جعبه جواهر (NGC 4755) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2516" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2516 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2516 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 2516 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 2516's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2516, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2516 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2516 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 2516 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 2516 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2516 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_3114" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 3114 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 3114 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 3114 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 3114's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 3114, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 3114 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 3114 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 3114 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 3114 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 3114 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6231" to BilingualFacts(
            en = listOf(
                "The NGC identity of Northern Jewel Box (NGC 6231) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Northern Jewel Box (NGC 6231) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Northern Jewel Box (NGC 6231) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Northern Jewel Box (NGC 6231)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Northern Jewel Box (NGC 6231), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای جعبه جواهر شمالی (NGC 6231) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "جعبه جواهر شمالی (NGC 6231) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای جعبه جواهر شمالی (NGC 6231) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی جعبه جواهر شمالی (NGC 6231) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون جعبه جواهر شمالی (NGC 6231) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_5128" to BilingualFacts(
            en = listOf(
                "The NGC identity of Centaurus A (NGC 5128) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Studies of Centaurus A (NGC 5128) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "The catalog notes for Centaurus A (NGC 5128) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, Centaurus A (NGC 5128) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Centaurus A (NGC 5128) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای قنطورس ای (NGC 5128) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "مطالعه قنطورس ای (NGC 5128) بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "یادداشت‌های کاتالوگی قنطورس ای (NGC 5128) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، قنطورس ای (NGC 5128) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای قنطورس ای (NGC 5128) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_253" to BilingualFacts(
            en = listOf(
                "The NGC identity of Sculptor Galaxy (NGC 253) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Sculptor Galaxy (NGC 253)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bright inner regions of Sculptor Galaxy (NGC 253) make it useful for comparing visual impressions with photographs and modern detector images.",
                "For visual observers, Sculptor Galaxy (NGC 253) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Sculptor Galaxy (NGC 253) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای کهکشان پیکرتراش (NGC 253) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی کهکشان پیکرتراش (NGC 253) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "ناحیه‌های درونی روشن کهکشان پیکرتراش (NGC 253) مقایسه برداشت چشمی با عکس‌ها و تصویرهای آشکارسازهای امروزی را آسان می‌کند.",
                "برای رصد چشمی، کهکشان پیکرتراش (NGC 253) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان پیکرتراش (NGC 253) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_55" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 55 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "The irregular form of NGC 55 records a disturbed mix of stars and gas, often shaped by interactions and uneven star formation.",
                "The edge-on view of NGC 55 makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure.",
                "For visual observers, NGC 55 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for NGC 55 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 55 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "شکل نامنظم NGC 55 آمیزه‌ای آشفته از ستاره و گاز را نشان می‌دهد که اغلب از برهم‌کنش‌ها و ستاره‌زایی نابرابر اثر گرفته است.",
                "نمای لبه‌به‌لبه NGC 55 بررسی نوارهای غبار و ضخامت قرص را برای مقایسه با مدل‌های ساختار کهکشانی آسان‌تر می‌کند.",
                "برای رصد چشمی، NGC 55 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای NGC 55 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_300" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 300 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 300's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The face-on view of NGC 300 lets images trace spiral arms and star-forming regions without severe disk foreshortening.",
                "For visual observers, NGC 300 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for NGC 300 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 300 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی NGC 300 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای روبه‌روی NGC 300 اجازه می‌دهد بازوهای مارپیچی و نواحی ستاره‌زا بدون کوتاه‌شدگی شدید قرص دنبال شوند.",
                "برای رصد چشمی، NGC 300 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای NGC 300 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_1365" to BilingualFacts(
            en = listOf(
                "The NGC identity of Great Barred Spiral (NGC 1365) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Great Barred Spiral (NGC 1365)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bar in Great Barred Spiral (NGC 1365) can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity.",
                "For visual observers, Great Barred Spiral (NGC 1365) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Great Barred Spiral (NGC 1365) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای مارپیچی میله‌ای بزرگ (NGC 1365) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی مارپیچی میله‌ای بزرگ (NGC 1365) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "میله مرکزی مارپیچی میله‌ای بزرگ (NGC 1365) می‌تواند گاز را به درون براند و پیوند ساختار مارپیچی با ستاره‌زایی یا فعالیت مرکزی را نشان دهد.",
                "برای رصد چشمی، مارپیچی میله‌ای بزرگ (NGC 1365) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای مارپیچی میله‌ای بزرگ (NGC 1365) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_1316" to BilingualFacts(
            en = listOf(
                "The NGC identity of Fornax A (NGC 1316) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Studies of Fornax A (NGC 1316) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "In the Fornax cluster environment, Fornax A (NGC 1316) helps trace how giant galaxies grow through mergers and radio activity.",
                "For visual observers, Fornax A (NGC 1316) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Fornax A (NGC 1316) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای کوره ای (NGC 1316) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "مطالعه کوره ای (NGC 1316) بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "کوره ای (NGC 1316) در محیط خوشه کوره به بررسی رشد کهکشان‌های بزرگ از راه ادغام‌ها و فعالیت رادیویی کمک می‌کند.",
                "برای رصد چشمی، کوره ای (NGC 1316) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کوره ای (NGC 1316) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_3115" to BilingualFacts(
            en = listOf(
                "The NGC identity of Spindle Galaxy (NGC 3115) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Spindle Galaxy (NGC 3115)'s lenticular form bridges spiral and elliptical systems, with a disk-like outline but subdued spiral-arm star formation.",
                "The edge-on view of Spindle Galaxy (NGC 3115) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure.",
                "For visual observers, Spindle Galaxy (NGC 3115) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Spindle Galaxy (NGC 3115) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای کهکشان دوک (NGC 3115) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ریخت عدسی‌گون کهکشان دوک (NGC 3115) میان کهکشان‌های مارپیچی و بیضوی قرار می‌گیرد؛ قرصی دیده می‌شود اما بازوهای ستاره‌زای پررنگ ندارد.",
                "نمای لبه‌به‌لبه کهکشان دوک (NGC 3115) بررسی نوارهای غبار و ضخامت قرص را برای مقایسه با مدل‌های ساختار کهکشانی آسان‌تر می‌کند.",
                "برای رصد چشمی، کهکشان دوک (NGC 3115) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان دوک (NGC 3115) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_4565" to BilingualFacts(
            en = listOf(
                "The NGC identity of Needle Galaxy (NGC 4565) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Needle Galaxy (NGC 4565)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The edge-on view of Needle Galaxy (NGC 4565) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure.",
                "For visual observers, Needle Galaxy (NGC 4565) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Needle Galaxy (NGC 4565) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای کهکشان سوزن (NGC 4565) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی کهکشان سوزن (NGC 4565) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای لبه‌به‌لبه کهکشان سوزن (NGC 4565) بررسی نوارهای غبار و ضخامت قرص را برای مقایسه با مدل‌های ساختار کهکشانی آسان‌تر می‌کند.",
                "برای رصد چشمی، کهکشان سوزن (NGC 4565) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان سوزن (NGC 4565) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_2903" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2903 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2903's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bright inner regions of NGC 2903 make it useful for comparing visual impressions with photographs and modern detector images.",
                "For visual observers, NGC 2903 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for NGC 2903 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2903 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی NGC 2903 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "ناحیه‌های درونی روشن NGC 2903 مقایسه برداشت چشمی با عکس‌ها و تصویرهای آشکارسازهای امروزی را آسان می‌کند.",
                "برای رصد چشمی، NGC 2903 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای NGC 2903 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_3628" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 3628 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 3628's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "In the Leo Triplet field, NGC 3628 is observed with neighboring galaxies whose tidal history reshapes disks and faint outer structure.",
                "For visual observers, NGC 3628 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for NGC 3628 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 3628 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی NGC 3628 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "NGC 3628 در میدان سه‌گانه شیر با همسایه‌هایی دیده می‌شود که تاریخ کشندی‌شان قرص‌ها و ساختارهای کم‌نور را دگرگون کرده است.",
                "برای رصد چشمی، NGC 3628 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای NGC 3628 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_4631" to BilingualFacts(
            en = listOf(
                "The NGC identity of Whale Galaxy (NGC 4631) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Whale Galaxy (NGC 4631)'s spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The edge-on view of Whale Galaxy (NGC 4631) makes dust lanes and disk thickness easier to compare with models of spiral-galaxy structure.",
                "For visual observers, Whale Galaxy (NGC 4631) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Whale Galaxy (NGC 4631) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای کهکشان نهنگ (NGC 4631) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی کهکشان نهنگ (NGC 4631) امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای لبه‌به‌لبه کهکشان نهنگ (NGC 4631) بررسی نوارهای غبار و ضخامت قرص را برای مقایسه با مدل‌های ساختار کهکشانی آسان‌تر می‌کند.",
                "برای رصد چشمی، کهکشان نهنگ (NGC 4631) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان نهنگ (NGC 4631) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_6822" to BilingualFacts(
            en = listOf(
                "The NGC identity of Barnard's Galaxy (NGC 6822) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "The irregular form of Barnard's Galaxy (NGC 6822) records a disturbed mix of stars and gas, often shaped by interactions and uneven star formation.",
                "The catalog notes for Barnard's Galaxy (NGC 6822) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, Barnard's Galaxy (NGC 6822) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Barnard's Galaxy (NGC 6822) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای کهکشان بارنارد (NGC 6822) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "شکل نامنظم کهکشان بارنارد (NGC 6822) آمیزه‌ای آشفته از ستاره و گاز را نشان می‌دهد که اغلب از برهم‌کنش‌ها و ستاره‌زایی نابرابر اثر گرفته است.",
                "یادداشت‌های کاتالوگی کهکشان بارنارد (NGC 6822) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، کهکشان بارنارد (NGC 6822) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای کهکشان بارنارد (NGC 6822) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_ngc_281" to BilingualFacts(
            en = listOf(
                "The NGC identity of Pacman Nebula (NGC 281) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Ionized gas in Pacman Nebula (NGC 281) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "Narrowband imaging of Pacman Nebula (NGC 281) separates hydrogen and oxygen emission, revealing structure that broadband views can hide.",
                "Nebula filters can improve contrast on Pacman Nebula (NGC 281) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Pacman Nebula (NGC 281) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی پک‌من (NGC 281) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "گاز یونیده در سحابی پک‌من (NGC 281) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "تصویربرداری باریک‌باند از سحابی پک‌من (NGC 281) گسیل هیدروژن و اکسیژن را جدا می‌کند و ساختارهایی را نشان می‌دهد که در نور پهن‌باند پنهان می‌مانند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی پک‌من (NGC 281) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی پک‌من (NGC 281) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_1499" to BilingualFacts(
            en = listOf(
                "The NGC identity of California Nebula (NGC 1499) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Ionized gas in California Nebula (NGC 1499) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "Narrowband imaging of California Nebula (NGC 1499) separates hydrogen and oxygen emission, revealing structure that broadband views can hide.",
                "Nebula filters can improve contrast on California Nebula (NGC 1499) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of California Nebula (NGC 1499) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی کالیفرنیا (NGC 1499) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "گاز یونیده در سحابی کالیفرنیا (NGC 1499) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "تصویربرداری باریک‌باند از سحابی کالیفرنیا (NGC 1499) گسیل هیدروژن و اکسیژن را جدا می‌کند و ساختارهایی را نشان می‌دهد که در نور پهن‌باند پنهان می‌مانند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی کالیفرنیا (NGC 1499) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی کالیفرنیا (NGC 1499) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_1977" to BilingualFacts(
            en = listOf(
                "The NGC identity of Running Man Nebula (NGC 1977) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Running Man Nebula (NGC 1977) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape.",
                "Because Running Man Nebula (NGC 1977) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars.",
                "Because Running Man Nebula (NGC 1977) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure.",
                "Multiwavelength images of Running Man Nebula (NGC 1977) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی مرد دونده (NGC 1977) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی مرد دونده (NGC 1977) نور ستارگان نزدیک را از روی دانه‌های غبار پراکنده می‌کند؛ بنابراین هندسه غبار شکل مرئی آن را تعیین می‌کند.",
                "چون سحابی مرد دونده (NGC 1977) بیشتر با نور بازتابی می‌درخشد، رنگ و کنتراست آن به دانه‌های غبار و ستارگان روشن‌کننده وابسته است.",
                "از آنجا که سحابی مرد دونده (NGC 1977) عمدتاً بازتابی است، آسمان تاریک معمولاً بیش از فیلترهای باریک‌باند در دیدن جزئیات ظریف کمک می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی مرد دونده (NGC 1977) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_1999" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 1999 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 1999 shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape.",
                "Because NGC 1999 is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars.",
                "Because NGC 1999 is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure.",
                "Multiwavelength images of NGC 1999 compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 1999 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 1999 نور ستارگان نزدیک را از روی دانه‌های غبار پراکنده می‌کند؛ بنابراین هندسه غبار شکل مرئی آن را تعیین می‌کند.",
                "چون NGC 1999 بیشتر با نور بازتابی می‌درخشد، رنگ و کنتراست آن به دانه‌های غبار و ستارگان روشن‌کننده وابسته است.",
                "از آنجا که NGC 1999 عمدتاً بازتابی است، آسمان تاریک معمولاً بیش از فیلترهای باریک‌باند در دیدن جزئیات ظریف کمک می‌کند.",
                "تصویرهای چندطولی‌موج از NGC 1999 گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_2261" to BilingualFacts(
            en = listOf(
                "The NGC identity of Hubble's Variable Nebula (NGC 2261) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Hubble's Variable Nebula (NGC 2261) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape.",
                "Because Hubble's Variable Nebula (NGC 2261) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars.",
                "Because Hubble's Variable Nebula (NGC 2261) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure.",
                "Multiwavelength images of Hubble's Variable Nebula (NGC 2261) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی متغیر هابل (NGC 2261) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی متغیر هابل (NGC 2261) نور ستارگان نزدیک را از روی دانه‌های غبار پراکنده می‌کند؛ بنابراین هندسه غبار شکل مرئی آن را تعیین می‌کند.",
                "چون سحابی متغیر هابل (NGC 2261) بیشتر با نور بازتابی می‌درخشد، رنگ و کنتراست آن به دانه‌های غبار و ستارگان روشن‌کننده وابسته است.",
                "از آنجا که سحابی متغیر هابل (NGC 2261) عمدتاً بازتابی است، آسمان تاریک معمولاً بیش از فیلترهای باریک‌باند در دیدن جزئیات ظریف کمک می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی متغیر هابل (NGC 2261) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_2359" to BilingualFacts(
            en = listOf(
                "The NGC identity of Thor's Helmet (NGC 2359) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Fast winds from a Wolf-Rayet star help sculpt Thor's Helmet (NGC 2359), producing arcs and shells rather than a quiet round cloud.",
                "The shell structure of Thor's Helmet (NGC 2359) records mass loss from a massive star before its eventual supernova stage.",
                "Nebula filters can improve contrast on Thor's Helmet (NGC 2359) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Thor's Helmet (NGC 2359) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای کلاه‌خود ثور (NGC 2359) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "بادهای تند یک ستاره ولف-رایه در پیکرتراشی کلاه‌خود ثور (NGC 2359) نقش دارند و کمان‌ها و پوسته‌هایی پویا می‌سازند.",
                "ساختار پوسته‌ای کلاه‌خود ثور (NGC 2359) ردّ جرم‌ریزی ستاره‌ای پرجرم را پیش از مرحله ابرنواختری آینده نشان می‌دهد.",
                "فیلترهای سحابی می‌توانند کنتراست کلاه‌خود ثور (NGC 2359) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از کلاه‌خود ثور (NGC 2359) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_ngc_3242" to BilingualFacts(
            en = listOf(
                "The NGC identity of Ghost of Jupiter (NGC 3242) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Ghost of Jupiter (NGC 3242) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The bright inner regions of Ghost of Jupiter (NGC 3242) make it useful for comparing visual impressions with photographs and modern detector images.",
                "Nebula filters can improve contrast on Ghost of Jupiter (NGC 3242) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Ghost of Jupiter (NGC 3242) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای شبح مشتری (NGC 3242) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "شبح مشتری (NGC 3242) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "ناحیه‌های درونی روشن شبح مشتری (NGC 3242) مقایسه برداشت چشمی با عکس‌ها و تصویرهای آشکارسازهای امروزی را آسان می‌کند.",
                "فیلترهای سحابی می‌توانند کنتراست شبح مشتری (NGC 3242) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی شبح مشتری (NGC 3242) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6302" to BilingualFacts(
            en = listOf(
                "The NGC identity of Butterfly Nebula (NGC 6302) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Butterfly Nebula (NGC 6302) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The complex shape of Butterfly Nebula (NGC 6302) shows how winds, disks, or magnetic geometry can channel gas from an aging star.",
                "Nebula filters can improve contrast on Butterfly Nebula (NGC 6302) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Butterfly Nebula (NGC 6302) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی پروانه (NGC 6302) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی پروانه (NGC 6302) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "شکل پیچیده سحابی پروانه (NGC 6302) نشان می‌دهد بادها، قرص‌ها یا میدان‌های مغناطیسی می‌توانند گاز یک ستاره پیر را کانالیزه کنند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی پروانه (NGC 6302) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی پروانه (NGC 6302) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_7027" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7027 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7027 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 7027 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 7027 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 7027 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7027 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7027 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 7027 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 7027 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 7027 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_246" to BilingualFacts(
            en = listOf(
                "The NGC identity of Skull Nebula (NGC 246) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Skull Nebula (NGC 246) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Skull Nebula (NGC 246) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Skull Nebula (NGC 246) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Skull Nebula (NGC 246) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی جمجمه (NGC 246) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی جمجمه (NGC 246) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی سحابی جمجمه (NGC 246) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی جمجمه (NGC 246) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی جمجمه (NGC 246) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_1535" to BilingualFacts(
            en = listOf(
                "The NGC identity of Cleopatra's Eye (NGC 1535) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Cleopatra's Eye (NGC 1535) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Cleopatra's Eye (NGC 1535) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Cleopatra's Eye (NGC 1535) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Cleopatra's Eye (NGC 1535) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای چشم کلئوپاترا (NGC 1535) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "چشم کلئوپاترا (NGC 1535) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی چشم کلئوپاترا (NGC 1535) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست چشم کلئوپاترا (NGC 1535) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی چشم کلئوپاترا (NGC 1535) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_2440" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2440 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2440 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 2440 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 2440 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 2440 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2440 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2440 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 2440 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 2440 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 2440 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_3918" to BilingualFacts(
            en = listOf(
                "The NGC identity of Blue Planetary (NGC 3918) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Blue Planetary (NGC 3918) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The bright inner regions of Blue Planetary (NGC 3918) make it useful for comparing visual impressions with photographs and modern detector images.",
                "Nebula filters can improve contrast on Blue Planetary (NGC 3918) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Blue Planetary (NGC 3918) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی سیاره‌نمای آبی (NGC 3918) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی سیاره‌نمای آبی (NGC 3918) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "ناحیه‌های درونی روشن سحابی سیاره‌نمای آبی (NGC 3918) مقایسه برداشت چشمی با عکس‌ها و تصویرهای آشکارسازهای امروزی را آسان می‌کند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی سیاره‌نمای آبی (NGC 3918) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی سیاره‌نمای آبی (NGC 3918) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_5189" to BilingualFacts(
            en = listOf(
                "The NGC identity of Spiral Planetary (NGC 5189) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Spiral Planetary (NGC 5189) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The complex shape of Spiral Planetary (NGC 5189) shows how winds, disks, or magnetic geometry can channel gas from an aging star.",
                "Nebula filters can improve contrast on Spiral Planetary (NGC 5189) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Spiral Planetary (NGC 5189) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی سیاره‌نمای مارپیچی (NGC 5189) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی سیاره‌نمای مارپیچی (NGC 5189) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "شکل پیچیده سحابی سیاره‌نمای مارپیچی (NGC 5189) نشان می‌دهد بادها، قرص‌ها یا میدان‌های مغناطیسی می‌توانند گاز یک ستاره پیر را کانالیزه کنند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی سیاره‌نمای مارپیچی (NGC 5189) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی سیاره‌نمای مارپیچی (NGC 5189) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6369" to BilingualFacts(
            en = listOf(
                "The NGC identity of Little Ghost (NGC 6369) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Little Ghost (NGC 6369) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Little Ghost (NGC 6369) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Little Ghost (NGC 6369) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Little Ghost (NGC 6369) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای شبح کوچک (NGC 6369) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "شبح کوچک (NGC 6369) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی شبح کوچک (NGC 6369) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست شبح کوچک (NGC 6369) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی شبح کوچک (NGC 6369) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6572" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6572 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6572 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The bright inner regions of NGC 6572 make it useful for comparing visual impressions with photographs and modern detector images.",
                "Nebula filters can improve contrast on NGC 6572 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 6572 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6572 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6572 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "ناحیه‌های درونی روشن NGC 6572 مقایسه برداشت چشمی با عکس‌ها و تصویرهای آشکارسازهای امروزی را آسان می‌کند.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 6572 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 6572 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6741" to BilingualFacts(
            en = listOf(
                "The NGC identity of Phantom Streak (NGC 6741) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Phantom Streak (NGC 6741) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Phantom Streak (NGC 6741) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Phantom Streak (NGC 6741) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Phantom Streak (NGC 6741) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای رد شبح (NGC 6741) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "رد شبح (NGC 6741) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی رد شبح (NGC 6741) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست رد شبح (NGC 6741) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی رد شبح (NGC 6741) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6781" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6781 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6781 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 6781 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 6781 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 6781 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6781 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6781 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 6781 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 6781 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 6781 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6818" to BilingualFacts(
            en = listOf(
                "The NGC identity of Little Gem (NGC 6818) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Little Gem (NGC 6818) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Little Gem (NGC 6818) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Little Gem (NGC 6818) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Little Gem (NGC 6818) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای گوهر کوچک (NGC 6818) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "گوهر کوچک (NGC 6818) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی گوهر کوچک (NGC 6818) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست گوهر کوچک (NGC 6818) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی گوهر کوچک (NGC 6818) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6884" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6884 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6884 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 6884 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 6884 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 6884 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6884 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6884 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 6884 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 6884 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 6884 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6891" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6891 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6891 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 6891 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 6891 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 6891 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6891 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6891 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 6891 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 6891 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 6891 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_6905" to BilingualFacts(
            en = listOf(
                "The NGC identity of Blue Flash (NGC 6905) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Blue Flash (NGC 6905) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Blue Flash (NGC 6905) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Blue Flash (NGC 6905) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Blue Flash (NGC 6905) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای درخشش آبی (NGC 6905) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "درخشش آبی (NGC 6905) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی درخشش آبی (NGC 6905) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست درخشش آبی (NGC 6905) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی درخشش آبی (NGC 6905) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_7008" to BilingualFacts(
            en = listOf(
                "The NGC identity of Fetus Nebula (NGC 7008) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Fetus Nebula (NGC 7008) is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for Fetus Nebula (NGC 7008) point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on Fetus Nebula (NGC 7008) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of Fetus Nebula (NGC 7008) traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای سحابی جنین (NGC 7008) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "سحابی جنین (NGC 7008) پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی سحابی جنین (NGC 7008) به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی جنین (NGC 7008) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی سحابی جنین (NGC 7008) اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_7094" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7094 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7094 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 7094 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 7094 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 7094 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7094 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7094 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 7094 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 7094 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 7094 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_7139" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7139 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7139 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 7139 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 7139 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 7139 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7139 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7139 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 7139 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 7139 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 7139 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_7354" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7354 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7354 is a dying-star shell: ultraviolet light from a hot central remnant makes gas expelled late in stellar evolution glow.",
                "The catalog notes for NGC 7354 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "Nebula filters can improve contrast on NGC 7354 where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Spectroscopy of NGC 7354 traces ionized oxygen, nitrogen, and hydrogen, turning its colors into clues about stellar mass loss."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7354 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7354 پوسته یک ستاره رو به پایان است؛ تابش فرابنفش هسته داغ، گاز بیرون‌ریخته را روشن می‌کند.",
                "یادداشت‌های کاتالوگی NGC 7354 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "فیلترهای سحابی می‌توانند کنتراست NGC 7354 را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "طیف‌سنجی NGC 7354 اکسیژن، نیتروژن و هیدروژن یونیده را دنبال می‌کند و رنگ‌های آن را به سرنخ‌هایی درباره جرم‌ریزی ستاره تبدیل می‌سازد."
            )
        ),
        "dso_ngc_7789" to BilingualFacts(
            en = listOf(
                "The NGC identity of Caroline's Rose (NGC 7789) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Caroline's Rose (NGC 7789) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The rich field of Caroline's Rose (NGC 7789) lets photometric studies separate likely members from unrelated foreground and background stars.",
                "Wide-field views help show Caroline's Rose (NGC 7789)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Caroline's Rose (NGC 7789), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای گل رز کارولین (NGC 7789) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "گل رز کارولین (NGC 7789) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "میدان غنی گل رز کارولین (NGC 7789) به بررسی‌های نوری اجازه می‌دهد اعضای محتمل خوشه از ستارگان پیش‌زمینه و پس‌زمینه جدا شوند.",
                "نمای میدان‌گسترده الگوی گل رز کارولین (NGC 7789) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون گل رز کارولین (NGC 7789) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_1528" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 1528 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 1528 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 1528 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 1528's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 1528, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 1528 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 1528 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 1528 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 1528 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 1528 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_1647" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 1647 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 1647 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The broad pattern of NGC 1647 is best understood as a stellar association against the Milky Way background, not a single bright point.",
                "Wide-field views help show NGC 1647's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 1647, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 1647 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 1647 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "الگوی گسترده NGC 1647 بیشتر به‌صورت انجمنی ستاره‌ای در زمینه راه شیری فهمیده می‌شود، نه یک نقطه درخشان منفرد.",
                "نمای میدان‌گسترده الگوی NGC 1647 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 1647 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_1817" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 1817 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 1817 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 1817 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 1817's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 1817, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 1817 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 1817 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 1817 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 1817 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 1817 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2158" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2158 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2158 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The rich field of NGC 2158 lets photometric studies separate likely members from unrelated foreground and background stars.",
                "Wide-field views help show NGC 2158's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2158, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2158 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2158 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "میدان غنی NGC 2158 به بررسی‌های نوری اجازه می‌دهد اعضای محتمل خوشه از ستارگان پیش‌زمینه و پس‌زمینه جدا شوند.",
                "نمای میدان‌گسترده الگوی NGC 2158 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2158 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2169" to BilingualFacts(
            en = listOf(
                "The NGC identity of 37 Cluster (NGC 2169) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "37 Cluster (NGC 2169) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of 37 Cluster (NGC 2169) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show 37 Cluster (NGC 2169)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around 37 Cluster (NGC 2169), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای خوشه ۳۷ (NGC 2169) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "خوشه ۳۷ (NGC 2169) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه ۳۷ (NGC 2169) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه ۳۷ (NGC 2169) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه ۳۷ (NGC 2169) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2301" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2301 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2301 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 2301 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 2301's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2301, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2301 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2301 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 2301 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 2301 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2301 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2360" to BilingualFacts(
            en = listOf(
                "The NGC identity of Caroline's Cluster (NGC 2360) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Caroline's Cluster (NGC 2360) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of Caroline's Cluster (NGC 2360) distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show Caroline's Cluster (NGC 2360)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Caroline's Cluster (NGC 2360), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای خوشه کارولین (NGC 2360) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "خوشه کارولین (NGC 2360) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای خوشه کارولین (NGC 2360) اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی خوشه کارولین (NGC 2360) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه کارولین (NGC 2360) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2362" to BilingualFacts(
            en = listOf(
                "The NGC identity of Tau Canis Majoris Cluster (NGC 2362) links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "Tau Canis Majoris Cluster (NGC 2362) is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The young massive stars in Tau Canis Majoris Cluster (NGC 2362) make it useful for testing early stellar evolution before a cluster disperses.",
                "Wide-field views help show Tau Canis Majoris Cluster (NGC 2362)'s pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around Tau Canis Majoris Cluster (NGC 2362), reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای خوشه تاو سگ بزرگ (NGC 2362) رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "خوشه تاو سگ بزرگ (NGC 2362) خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "ستارگان جوان و پرجرم خوشه تاو سگ بزرگ (NGC 2362) آن را برای آزمون تحول آغازین ستارگان پیش از پراکندگی خوشه سودمند می‌کنند.",
                "نمای میدان‌گسترده الگوی خوشه تاو سگ بزرگ (NGC 2362) را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون خوشه تاو سگ بزرگ (NGC 2362) را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2420" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2420 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2420 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "As an older open cluster, NGC 2420 helps trace how Milky Way disk clusters lose members and survive Galactic tides.",
                "Wide-field views help show NGC 2420's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2420, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2420 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2420 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "NGC 2420 به‌عنوان خوشه باز کهنسال، چگونگی از دست دادن اعضا و پایداری خوشه‌های قرص راه شیری در برابر کشندها را پیگیری می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 2420 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2420 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2451" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2451 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2451 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 2451 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 2451's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2451, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2451 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2451 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 2451 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 2451 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2451 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2477" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2477 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2477 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "The rich field of NGC 2477 lets photometric studies separate likely members from unrelated foreground and background stars.",
                "Wide-field views help show NGC 2477's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2477, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2477 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2477 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "میدان غنی NGC 2477 به بررسی‌های نوری اجازه می‌دهد اعضای محتمل خوشه از ستارگان پیش‌زمینه و پس‌زمینه جدا شوند.",
                "نمای میدان‌گسترده الگوی NGC 2477 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2477 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2506" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2506 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2506 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "As an older open cluster, NGC 2506 helps trace how Milky Way disk clusters lose members and survive Galactic tides.",
                "Wide-field views help show NGC 2506's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2506, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2506 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2506 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "NGC 2506 به‌عنوان خوشه باز کهنسال، چگونگی از دست دادن اعضا و پایداری خوشه‌های قرص راه شیری در برابر کشندها را پیگیری می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 2506 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2506 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2539" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2539 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2539 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 2539 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 2539's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2539, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2539 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2539 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 2539 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 2539 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2539 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_2547" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 2547 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 2547 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 2547 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 2547's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 2547, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 2547 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 2547 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 2547 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 2547 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 2547 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6025" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6025 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6025 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6025 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6025's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6025, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6025 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6025 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6025 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6025 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6025 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6067" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6067 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6067 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6067 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6067's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6067, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6067 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6067 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6067 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6067 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6067 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6087" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6087 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6087 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6087 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6087's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6087, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6087 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6087 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6087 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6087 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6087 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6124" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6124 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6124 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6124 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6124's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6124, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6124 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6124 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6124 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6124 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6124 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6242" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6242 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6242 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6242 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6242's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6242, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6242 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6242 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6242 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6242 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6242 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6633" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6633 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6633 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6633 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6633's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6633, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6633 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6633 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6633 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6633 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6633 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6755" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6755 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6755 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6755 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6755's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6755, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6755 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6755 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6755 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6755 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6755 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6791" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6791 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6791 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "As an older open cluster, NGC 6791 helps trace how Milky Way disk clusters lose members and survive Galactic tides.",
                "Wide-field views help show NGC 6791's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6791, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6791 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6791 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "NGC 6791 به‌عنوان خوشه باز کهنسال، چگونگی از دست دادن اعضا و پایداری خوشه‌های قرص راه شیری در برابر کشندها را پیگیری می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6791 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6791 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6819" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6819 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6819 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6819 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6819's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6819, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6819 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6819 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6819 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6819 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6819 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6866" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6866 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6866 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6866 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6866's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6866, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6866 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6866 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6866 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6866 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6866 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6910" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6910 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6910 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6910 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6910's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6910, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6910 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6910 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6910 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6910 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6910 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6939" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6939 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6939 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6939 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6939's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6939, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6939 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6939 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6939 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6939 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6939 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_6940" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 6940 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 6940 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 6940 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 6940's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 6940, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 6940 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 6940 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 6940 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 6940 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 6940 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7062" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7062 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7062 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7062 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7062's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7062, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7062 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7062 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7062 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7062 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7062 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7063" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7063 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7063 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7063 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7063's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7063, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7063 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7063 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7063 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7063 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7063 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7160" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7160 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7160 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7160 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7160's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7160, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7160 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7160 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7160 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7160 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7160 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7209" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7209 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7209 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7209 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7209's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7209, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7209 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7209 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7209 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7209 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7209 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7380" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7380 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7380 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7380 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7380's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7380, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7380 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7380 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7380 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7380 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7380 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7510" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7510 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7510 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7510 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7510's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7510, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7510 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7510 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7510 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7510 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7510 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7686" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7686 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7686 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7686 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7686's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7686, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7686 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7686 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7686 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7686 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7686 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_ngc_7790" to BilingualFacts(
            en = listOf(
                "The NGC identity of NGC 7790 links nineteenth-century visual cataloging with modern database records and survey measurements.",
                "NGC 7790 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of NGC 7790 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show NGC 7790's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around NGC 7790, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "شناسه کاتالوگی برای NGC 7790 رصدهای بصری سده نوزدهم را به داده‌های امروزی و پیمایش‌های دیجیتال پیوند می‌دهد.",
                "NGC 7790 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای NGC 7790 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی NGC 7790 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون NGC 7790 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_c1" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes C1 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "C1 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "As an older open cluster, C1 helps trace how Milky Way disk clusters lose members and survive Galactic tides.",
                "Wide-field views help show C1's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around C1, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "فهرست کالدول C1 را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "C1 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "C1 به‌عنوان خوشه باز کهنسال، چگونگی از دست دادن اعضا و پایداری خوشه‌های قرص راه شیری در برابر کشندها را پیگیری می‌کند.",
                "نمای میدان‌گسترده الگوی C1 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون C1 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_c3" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes C3 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "C3's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The bar in C3 can funnel gas inward, a mechanism used to connect spiral structure with central star formation or activity.",
                "For visual observers, C3 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for C3 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "فهرست کالدول C3 را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی C3 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "میله مرکزی C3 می‌تواند گاز را به درون براند و پیوند ساختار مارپیچی با ستاره‌زایی یا فعالیت مرکزی را نشان دهد.",
                "برای رصد چشمی، C3 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای C3 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_c4" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes Iris Nebula (C4) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "Iris Nebula (C4) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape.",
                "Because Iris Nebula (C4) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars.",
                "Because Iris Nebula (C4) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure.",
                "Multiwavelength images of Iris Nebula (C4) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "فهرست کالدول سحابی زنبق (C4) را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "سحابی زنبق (C4) نور ستارگان نزدیک را از روی دانه‌های غبار پراکنده می‌کند؛ بنابراین هندسه غبار شکل مرئی آن را تعیین می‌کند.",
                "چون سحابی زنبق (C4) بیشتر با نور بازتابی می‌درخشد، رنگ و کنتراست آن به دانه‌های غبار و ستارگان روشن‌کننده وابسته است.",
                "از آنجا که سحابی زنبق (C4) عمدتاً بازتابی است، آسمان تاریک معمولاً بیش از فیلترهای باریک‌باند در دیدن جزئیات ظریف کمک می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی زنبق (C4) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_c5" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes C5 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "C5's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The face-on view of C5 lets images trace spiral arms and star-forming regions without severe disk foreshortening.",
                "For visual observers, C5 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for C5 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "فهرست کالدول C5 را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی C5 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "نمای روبه‌روی C5 اجازه می‌دهد بازوهای مارپیچی و نواحی ستاره‌زا بدون کوتاه‌شدگی شدید قرص دنبال شوند.",
                "برای رصد چشمی، C5 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای C5 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_c8" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes C8 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "C8 is a Milky Way disk cluster whose stars formed together, making it useful for comparing stellar evolution at one shared age.",
                "Proper-motion studies of C8 distinguish true cluster members from unrelated stars projected into the same field.",
                "Wide-field views help show C8's pattern against the surrounding Milky Way stars before higher power isolates individual members.",
                "Gaia-era astrometry improves membership checks around C8, reducing confusion from unrelated stars projected along the same sightline."
            ),
            fa = listOf(
                "فهرست کالدول C8 را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "C8 خوشه‌ای قرصی در راه شیری است که ستارگانش باهم زاده شده‌اند و برای سنجش تحول ستاره‌ای در یک سن مشترک سودمندند.",
                "مطالعات حرکت خاص برای C8 اعضای واقعی خوشه را از ستارگان نامرتبطی که در همان میدان افتاده‌اند جدا می‌کند.",
                "نمای میدان‌گسترده الگوی C8 را در برابر ستارگان راه شیری نشان می‌دهد و سپس بزرگنمایی بیشتر اعضا را جدا می‌کند.",
                "اخترسنجی مأموریت گایا بررسی عضویت پیرامون C8 را بهتر کرده و آشفتگی ناشی از ستارگان نامرتبط هم‌خط را کاهش می‌دهد."
            )
        ),
        "dso_c11" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes Bubble Nebula (C11) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "Ionized gas in Bubble Nebula (C11) glows because young or hot stars energize hydrogen and oxygen-rich clouds around them.",
                "Narrowband imaging of Bubble Nebula (C11) separates hydrogen and oxygen emission, revealing structure that broadband views can hide.",
                "Nebula filters can improve contrast on Bubble Nebula (C11) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Bubble Nebula (C11) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "فهرست کالدول سحابی حباب (C11) را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "گاز یونیده در سحابی حباب (C11) با انرژی ستارگان جوان یا داغ می‌درخشد و ابرهای هیدروژن و اکسیژن را آشکار می‌کند.",
                "تصویربرداری باریک‌باند از سحابی حباب (C11) گسیل هیدروژن و اکسیژن را جدا می‌کند و ساختارهایی را نشان می‌دهد که در نور پهن‌باند پنهان می‌مانند.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی حباب (C11) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی حباب (C11) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_c19" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes Cocoon Nebula (C19) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "Cocoon Nebula (C19) shines by scattering nearby starlight from dust grains, so dust geometry strongly controls its visible shape.",
                "Because Cocoon Nebula (C19) is dominated by reflected starlight, its color and contrast depend strongly on dust grains and nearby illuminating stars.",
                "Because Cocoon Nebula (C19) is reflection-dominated, dark skies usually help more than narrow emission filters when trying to see subtle structure.",
                "Multiwavelength images of Cocoon Nebula (C19) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "فهرست کالدول سحابی پیله (C19) را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "سحابی پیله (C19) نور ستارگان نزدیک را از روی دانه‌های غبار پراکنده می‌کند؛ بنابراین هندسه غبار شکل مرئی آن را تعیین می‌کند.",
                "چون سحابی پیله (C19) بیشتر با نور بازتابی می‌درخشد، رنگ و کنتراست آن به دانه‌های غبار و ستارگان روشن‌کننده وابسته است.",
                "از آنجا که سحابی پیله (C19) عمدتاً بازتابی است، آسمان تاریک معمولاً بیش از فیلترهای باریک‌باند در دیدن جزئیات ظریف کمک می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی پیله (C19) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_c24" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes Perseus A (C24) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "Studies of Perseus A (C24) emphasize old stellar populations and hot halos rather than the blue spiral-arm nurseries found in disk galaxies.",
                "At the heart of the Perseus cluster field, Perseus A (C24) connects galaxy evolution with energetic gas surrounding a massive cluster.",
                "For visual observers, Perseus A (C24) rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for Perseus A (C24) preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "فهرست کالدول برساوش ای (C24) را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "مطالعه برساوش ای (C24) بیشتر بر جمعیت‌های ستاره‌ای پیر و هاله‌های داغ متمرکز است، نه زایشگاه‌های آبی بازوهای مارپیچی.",
                "برساوش ای (C24) در میدان مرکزی خوشه برساوش، تحول کهکشان را به گاز پرانرژی پیرامون یک خوشه پرجرم پیوند می‌دهد.",
                "برای رصد چشمی، برساوش ای (C24) به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای برساوش ای (C24) انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        ),
        "dso_c25" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes Intergalactic Wanderer (C25) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "Intergalactic Wanderer (C25) belongs to the Milky Way globular-cluster population, whose ancient stars preserve clues to early Galactic assembly.",
                "The crowded core of Intergalactic Wanderer (C25) requires careful photometry to separate stars that blur together in ordinary wide-field images.",
                "Increasing aperture changes Intergalactic Wanderer (C25) from a grainy glow into a partially resolved star swarm, a classic globular-cluster progression.",
                "Color-magnitude diagrams of Intergalactic Wanderer (C25) separate red giants, horizontal-branch stars, and main-sequence populations inside one bound system."
            ),
            fa = listOf(
                "فهرست کالدول سرگردان میان‌کهکشانی (C25) را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "سرگردان میان‌کهکشانی (C25) به جمعیت خوشه‌های کروی راه شیری تعلق دارد و ستارگان کهنسالش سرنخ‌هایی از آغاز شکل‌گیری کهکشان نگه داشته‌اند.",
                "هسته فشرده سرگردان میان‌کهکشانی (C25) برای جداسازی ستارگانی که در تصویرهای معمول درهم می‌آمیزند، نورسنجی دقیق می‌طلبد.",
                "با افزایش دهانه تلسکوپ، سرگردان میان‌کهکشانی (C25) از مهی دانه‌دانه به انبوهی نیمه‌تفکیک‌شده از ستارگان تبدیل می‌شود.",
                "نمودارهای رنگ-قدر سرگردان میان‌کهکشانی (C25) غول‌های سرخ، ستارگان شاخه افقی و جمعیت رشته اصلی را در یک سامانه گرانشی جدا می‌کنند."
            )
        ),
        "dso_c27" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes Crescent Nebula (C27) a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "Fast winds from a Wolf-Rayet star help sculpt Crescent Nebula (C27), producing arcs and shells rather than a quiet round cloud.",
                "The shell structure of Crescent Nebula (C27) records mass loss from a massive star before its eventual supernova stage.",
                "Nebula filters can improve contrast on Crescent Nebula (C27) where emission dominates, while unfiltered wide-field views preserve the surrounding star field.",
                "Multiwavelength images of Crescent Nebula (C27) compare ionized gas, dust, and embedded stars, so the object changes character from optical to infrared views."
            ),
            fa = listOf(
                "فهرست کالدول سحابی هلال (C27) را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "بادهای تند یک ستاره ولف-رایه در پیکرتراشی سحابی هلال (C27) نقش دارند و کمان‌ها و پوسته‌هایی پویا می‌سازند.",
                "ساختار پوسته‌ای سحابی هلال (C27) ردّ جرم‌ریزی ستاره‌ای پرجرم را پیش از مرحله ابرنواختری آینده نشان می‌دهد.",
                "فیلترهای سحابی می‌توانند کنتراست سحابی هلال (C27) را در بخش‌های گسیلی افزایش دهند و نمای بدون فیلتر میدان ستاره‌ای پیرامون را حفظ می‌کند.",
                "تصویرهای چندطولی‌موج از سحابی هلال (C27) گاز یونیده، غبار و ستارگان نهفته را مقایسه می‌کنند و چهره جرم از نور مرئی تا فروسرخ تغییر می‌کند."
            )
        ),
        "dso_c29" to BilingualFacts(
            en = listOf(
                "The Caldwell selection makes C29 a deliberate non-Messier showpiece, linking modern backyard observing with classic visual catalog work.",
                "C29's spiral-disk structure lets astronomers study how dust lanes, gas flows, and star-forming arms shape a galaxy over time.",
                "The catalog notes for C29 point to a recognizable structure that observers can compare across eyepiece sketches and survey images.",
                "For visual observers, C29 rewards transparent dark skies because its outer structure fades quickly when skyglow washes out low contrast.",
                "Published database entries for C29 preserve redshift, photometry, and multiwavelength identifiers, keeping amateur charts aligned with professional archives."
            ),
            fa = listOf(
                "فهرست کالدول C29 را به‌عنوان هدفی شاخص بیرون از فهرست مسیه برجسته می‌کند و آن را به سنت رصد بصری پیوند می‌دهد.",
                "ساختار قرصی و مارپیچی C29 امکان بررسی نقش نوارهای غبار، جریان گاز و بازوهای ستاره‌زا در تحول کهکشان را فراهم می‌کند.",
                "یادداشت‌های کاتالوگی C29 به ساختاری قابل تشخیص اشاره دارند که می‌توان آن را در طرح‌های رصدی و تصویرهای پیمایشی مقایسه کرد.",
                "برای رصد چشمی، C29 به آسمان تاریک و شفاف پاداش می‌دهد، زیرا ساختار بیرونی آن در روشنایی آسمان زود محو می‌شود.",
                "داده‌های منتشرشده برای C29 انتقال‌به‌سرخ، نورسنجی و شناسه‌های چندطولی‌موج را نگه می‌دارند و نقشه‌های آماتوری را با آرشیوهای حرفه‌ای همسو می‌کنند."
            )
        )
    )
}
