package com.alijafari.red.astronomy.ui.monuments

import com.alijafari.red.astronomy.R

/**
 * The two Iranian monuments that stand on the ground of the Home sky scene.
 *
 * They are not astronomical objects: they are places, and each one carries its own page behind the
 * name that appears when it is tapped on the canvas (see [MonumentScreen]). The enum is the whole
 * navigation vocabulary — Home reports which one was tapped, MainActivity renders it, and nothing
 * else needs to know how the page is built.
 */
enum class MonumentId {
    /** The Apadana terrace at Persepolis (Parsa), left and farther back in the Home landscape. */
    PERSEPOLIS,

    /** The Mil-e Azhdaha fire tower on its limestone hillside, at the far right of the scene. */
    MIL_E_AZHDAHA
}

/** One measured figure in the opening stat band. Plain data, no thresholds: the values are cited. */
class MonumentStat(val value: String, val label: String)

/** One entry of the era list near the end of a page. */
class MonumentTimelineEntry(val year: String, val event: String)

/** One numbered chapter: an index, a title and one or more paragraphs. */
class MonumentChapter(val index: String, val title: String, val paragraphs: List<String>)

/** A photograph and its caption. [aspectRatio] is width / height, so the image keeps its framing. */
class MonumentFigure(val imageRes: Int, val caption: String, val aspectRatio: Float)

/** A pull quote. [attribution] is null for the app's own line, never for a cited inscription. */
class MonumentQuote(val text: String, val attribution: String?)

/** The ordered body of a monument page. Figures, quotes and eras sit between chapters. */
sealed class MonumentBlock {
    class Chapter(val chapter: MonumentChapter) : MonumentBlock()
    class Figure(val figure: MonumentFigure) : MonumentBlock()
    class Quote(val quote: MonumentQuote) : MonumentBlock()
    class Timeline(val entries: List<MonumentTimelineEntry>) : MonumentBlock()
}

/** Everything one monument page shows, already localized. */
class MonumentContent(
    val id: MonumentId,
    val title: String,
    val kicker: String,
    val subtitle: String,
    val meta: List<String>,
    val heroRes: Int,
    val lead: String,
    val stats: List<MonumentStat>,
    val blocks: List<MonumentBlock>,
    val credits: String,
    val sources: String
)

/**
 * The monument pages' copy, in Persian and English.
 *
 * The Persian text follows the app's own Persian writing rule: Vazirmatn-shaped orthography with
 * ZWNJ, Persian digits, «گیومه» and ، ؛ ؟, no Arabic ي/ك, no em dashes, and no administrative
 * vocabulary (no «می‌باشد», no «لازم به ذکر است»). Facts are limited to what the cited sources
 * support; where the sources genuinely disagree — the Mil-e Azhdaha's dating — the page says so
 * instead of choosing a side.
 */
object MonumentCopy {

    /** The name that appears on the sky canvas and in the page header. */
    fun title(id: MonumentId, isPersian: Boolean): String = when (id) {
        MonumentId.PERSEPOLIS -> if (isPersian) "تخت جمشید" else "Persepolis"
        MonumentId.MIL_E_AZHDAHA -> if (isPersian) "میل اژدها" else "Mil-e Azhdaha"
    }

    /** The quiet second line of the canvas callout: what the place is, in four or five words. */
    fun calloutDetail(id: MonumentId, isPersian: Boolean): String = when (id) {
        MonumentId.PERSEPOLIS ->
            if (isPersian) "پارسه · میراث جهانی یونسکو" else "Parsa · UNESCO World Heritage"
        MonumentId.MIL_E_AZHDAHA ->
            if (isPersian) "آتشگاه اشکانی · ممسنی" else "Parthian fire tower · Mamasani"
    }

    fun of(id: MonumentId, isPersian: Boolean): MonumentContent = when (id) {
        MonumentId.PERSEPOLIS -> persepolis(isPersian)
        MonumentId.MIL_E_AZHDAHA -> milAzhdaha(isPersian)
    }

    // ---------------------------------------------------------------------------------------
    // Persepolis / تخت جمشید
    // ---------------------------------------------------------------------------------------

    private fun persepolis(isPersian: Boolean): MonumentContent = MonumentContent(
        id = MonumentId.PERSEPOLIS,
        title = title(MonumentId.PERSEPOLIS, isPersian),
        kicker = if (isPersian) "میراث جهانی یونسکو · پارسه" else "UNESCO World Heritage · Parsa",
        subtitle = if (isPersian) {
            "پایتخت آیینی شاهنشاهی هخامنشی"
        } else {
            "The ceremonial heart of the Achaemenid Empire"
        },
        meta = if (isPersian) {
            listOf("مرودشت، فارس", "سده ششم پیش از میلاد")
        } else {
            listOf("Marvdasht, Fars", "6th century BC")
        },
        heroRes = R.drawable.monument_persepolis_gate,
        lead = if (isPersian) {
            "در دامنه کوه رحمت، سکویی سنگی از دل زمین بالا آمده است؛ سکویی که داریوش بزرگ " +
                "برآورد تا شکوه شاهنشاهی هخامنشی را بر آن بنشاند. پارسه پایتخت روزمره شاهان نبود، " +
                "جای گردهمایی بود؛ جایی که نمایندگان سرزمین‌ها با هدیه می‌آمدند و عظمت یک " +
                "امپراتوری را از نزدیک می‌دیدند. آنچه امروز می‌بینیم ویرانه نیست؛ نقشه یک اندیشه است."
        } else {
            "At the foot of Mount Rahmat, a stone terrace lifts itself out of the plain: the " +
                "platform Darius the Great raised to carry the splendour of the Achaemenid Empire. " +
                "Parsa was never the everyday palace of kings. It was a place of gathering, where " +
                "envoys from across the known world arrived with gifts and saw an empire at its " +
                "height. What survives here is not rubble. It is the plan of an idea."
        },
        stats = if (isPersian) {
            listOf(
                MonumentStat("۵۱۸", "سال آغاز ساخت، پیش از میلاد"),
                MonumentStat("۱۲۵٬۰۰۰", "متر مربع سکوی پارسه"),
                MonumentStat("۱۹", "متر، بلندای ستون‌های آپادانا"),
                MonumentStat("۲۳", "ملت بر سنگ‌نگاره‌ها")
            )
        } else {
            listOf(
                MonumentStat("518 BC", "Construction begins"),
                MonumentStat("125,000 m²", "The terrace at Parsa"),
                MonumentStat("19 m", "Height of the Apadana columns"),
                MonumentStat("23", "Nations carved in stone")
            )
        },
        blocks = listOf(
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۱" else "01",
                    title = if (isPersian) "سکویی بر دامنه کوه" else "A terrace on the mountainside",
                    paragraphs = if (isPersian) {
                        listOf(
                            "ساخت پارسه در حدود سال ۵۱۸ پیش از میلاد آغاز شد. داریوش نخست " +
                                "دامنه کوه را تراشید و سکویی نزدیک به ۱۲۵ هزار متر مربع ساخت؛ " +
                                "بخشی از سکو از سنگ کوه و بخشی با دیوارهای سنگین بر دشت بالا آمد. " +
                                "بر این سکو، کاخ‌های بار عام، اندرونی شاهان، گنجخانه و دیوارهای " +
                                "پاسداری جای گرفتند. زیر کف سنگی، آبراهه‌های پنهان آب باران را از " +
                                "دل بنا بیرون می‌بردند تا سکو، با همه سنگینی‌اش، خشک بماند."
                        )
                    } else {
                        listOf(
                            "Work began around 518 BC. Darius first cut back the mountainside, " +
                                "then raised a terrace of some 125,000 square metres: part of it " +
                                "quarried from the rock itself, part of it held above the plain by " +
                                "heavy retaining walls. Audience halls, the kings' own quarters, the " +
                                "treasury and the guard walls all took their places on that podium. " +
                                "Beneath the stone floor, hidden drains carried rainwater out through " +
                                "the building so the terrace could stay dry under its own weight."
                        )
                    }
                )
            ),
            MonumentBlock.Figure(
                MonumentFigure(
                    imageRes = R.drawable.monument_persepolis_terrace,
                    caption = if (isPersian) {
                        "نمای پارسه از دامنه کوه رحمت؛ سکو، پلکان آپادانا و ستون‌های برجای‌مانده."
                    } else {
                        "Parsa seen from the slope of Mount Rahmat: the terrace, the Apadana " +
                            "stairway and the columns that are still standing."
                    },
                    aspectRatio = 1440f / 954f
                )
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۲" else "02",
                    title = if (isPersian) "آپادانا؛ جنگلی از ستون" else "The Apadana, a forest of columns",
                    paragraphs = if (isPersian) {
                        listOf(
                            "آپادانا بزرگ‌ترین تالار بار عام پارسه بود؛ تالاری چهارگوش با ۳۶ " +
                                "ستون سنگی که بلندای هر یک به نزدیک ۱۹ متر می‌رسید. ستون‌ها " +
                                "باریک‌اند و فاصله‌شان زیاد؛ چون سقف چوبی بود و بار سبک، معمار " +
                                "توانست بلندتر و آزادتر بسازد. بر سر ستون‌ها، گاوهای دوقلو و " +
                                "شیردال‌ها نشسته‌اند و سنگینی سقف را به نقش بدل کرده‌اند."
                        )
                    } else {
                        listOf(
                            "The Apadana was the greatest audience hall at Parsa: a square room " +
                                "ringed by 36 stone columns, each close to 19 metres tall. The " +
                                "columns are slender and widely spaced because the roof was timber " +
                                "and the load was light, which let the builders reach higher and open " +
                                "the hall further. Paired bulls and lion-griffins sit on the capitals, " +
                                "turning the weight of the roof into sculpture."
                        )
                    }
                )
            ),
            MonumentBlock.Figure(
                MonumentFigure(
                    imageRes = R.drawable.monument_persepolis_columns,
                    caption = if (isPersian) {
                        "ستون‌های آپادانا و چارچوب دروازه‌ها؛ سنگ آهکی خاکستری، بدون ملات."
                    } else {
                        "Apadana columns and door frames: grey limestone, laid without mortar."
                    },
                    aspectRatio = 1440f / 954f
                )
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۳" else "03",
                    title = if (isPersian) "سنگ‌نگاره؛ جهانی که هدیه می‌آورد" else "Reliefs: a world arriving with gifts",
                    paragraphs = if (isPersian) {
                        listOf(
                            "بر دیواره پلکان‌های آپادانا، صف نمایندگان ۲۳ ملت پیکر گرفته است؛ از " +
                                "هندی و بلخی و سغدی تا یونانی و مصری و حبشی. هیچ‌یک خوار نشان داده " +
                                "نشده‌اند؛ دست در دست، با هدیه‌هایی در دست، به سوی شاه روان‌اند. این " +
                                "نقوش نه فهرست خراج، که تصویر یک باورند: شاه شاهان در مرکز جهانی " +
                                "ایستاده که ملت‌هایش با احترام به دیدارش می‌آیند."
                        )
                    } else {
                        listOf(
                            "Along the walls of the Apadana stairways, the delegates of 23 nations " +
                                "stand in procession, from Indians, Bactrians and Sogdians to Greeks, " +
                                "Egyptians and Ethiopians. None of them is shown humiliated. They walk " +
                                "hand in hand toward the king, gifts in their hands. The reliefs are " +
                                "not a tribute ledger but a picture of a belief: the King of Kings at " +
                                "the centre of a world whose peoples come to him with respect."
                        )
                    }
                )
            ),
            MonumentBlock.Figure(
                MonumentFigure(
                    imageRes = R.drawable.monument_persepolis_relief,
                    caption = if (isPersian) {
                        "پیشکش‌آورندگان بر پلکان آپادانا؛ هر ملت با لباس و هدیه خودش."
                    } else {
                        "Tribute bearers on the Apadana stairway, each nation in its own dress " +
                            "and with its own gift."
                    },
                    aspectRatio = 640f / 565f
                )
            ),
            MonumentBlock.Quote(
                MonumentQuote(
                    text = if (isPersian) {
                        "به خواست اهورامزدا، من خشایارشا، شاه بزرگ، شاه شاهان، این دروازه همه " +
                            "سرزمین‌ها را ساختم."
                    } else {
                        "By the favour of Ahuramazda, I, Xerxes, the great king, king of kings, " +
                            "built this Gate of All Nations."
                    },
                    attribution = if (isPersian) {
                        "برگردان کتیبه دروازه ملل"
                    } else {
                        "Translation of the Gate of All Nations inscription"
                    }
                )
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۴" else "04",
                    title = if (isPersian) "آتشی که خاموش نشد" else "The fire that did not end it",
                    paragraphs = if (isPersian) {
                        listOf(
                            "در سال ۳۳۰ پیش از میلاد، پارسه به دست اسکندر مقدونی به آتش کشیده شد. " +
                                "آتش در سدر و رنگ‌های کاخ‌ها افتاد و آنچه نسوخت، زیر خاک ماند. اما " +
                                "خاک گاهی حافظه است: امروز بخش بزرگی از آنچه از آیین و زندگی دوران " +
                                "هخامنشی می‌دانیم، از همین سنگ‌ها می‌آید. پارسه در سال ۱۹۷۹ در " +
                                "فهرست میراث جهانی یونسکو ثبت شد و از آن پس، میراث همه ایرانیان است."
                        )
                    } else {
                        listOf(
                            "In 330 BC, Parsa was set alight by Alexander of Macedon. Fire took the " +
                                "cedar and the painted surfaces; what the fire spared stayed under the " +
                                "soil. Soil can be a memory: much of what we know of Achaemenid ritual " +
                                "and daily life comes from these very stones. In 1979 Parsa entered the " +
                                "UNESCO World Heritage list, and since then it belongs to every Iranian."
                        )
                    }
                )
            ),
            MonumentBlock.Figure(
                MonumentFigure(
                    imageRes = R.drawable.monument_persepolis_panel,
                    caption = if (isPersian) {
                        "آنچه آتش نگرفت؛ سنگ‌نگاره‌ای از کاخ‌های پارسه."
                    } else {
                        "What the fire spared: a relief panel from the palaces of Parsa."
                    },
                    aspectRatio = 800f / 600f
                )
            ),
            MonumentBlock.Timeline(
                if (isPersian) {
                    listOf(
                        MonumentTimelineEntry("۵۵۰ پیش از میلاد", "کوروش بزرگ شاهنشاهی هخامنشی را بنیان می‌گذارد."),
                        MonumentTimelineEntry("۵۱۸ پیش از میلاد", "داریوش بزرگ ساخت پارسه را آغاز می‌کند."),
                        MonumentTimelineEntry("۴۸۶ پیش از میلاد", "خشایارشا دروازه ملل و کاخ‌های تازه را می‌افزاید."),
                        MonumentTimelineEntry("۴۶۵ پیش از میلاد", "اردشیر یکم مجموعه را با کاخ صد ستون کامل می‌کند."),
                        MonumentTimelineEntry("۳۳۰ پیش از میلاد", "پارسه به دست اسکندر مقدونی به آتش کشیده می‌شود."),
                        MonumentTimelineEntry("۱۹۷۹", "تخت جمشید در فهرست میراث جهانی یونسکو ثبت می‌شود.")
                    )
                } else {
                    listOf(
                        MonumentTimelineEntry("550 BC", "Cyrus the Great founds the Achaemenid Empire."),
                        MonumentTimelineEntry("518 BC", "Darius the Great begins building at Parsa."),
                        MonumentTimelineEntry("486 BC", "Xerxes adds the Gate of All Nations and new palaces."),
                        MonumentTimelineEntry("465 BC", "Artaxerxes I completes the complex with the Hall of a Hundred Columns."),
                        MonumentTimelineEntry("330 BC", "Parsa is set alight by Alexander of Macedon."),
                        MonumentTimelineEntry("1979", "Persepolis is inscribed as a UNESCO World Heritage Site.")
                    )
                }
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۵" else "05",
                    title = if (isPersian) "چرا اینجا ایستاده‌ایم" else "Why we stand here",
                    paragraphs = if (isPersian) {
                        listOf(
                            "تخت جمشید یادگار یک پادشاه نیست؛ حافظه یک تمدن است. هر ترک روی سنگ، " +
                                "جای انگشت کسانی است که باور داشتند می‌توان جهانی ساخت که در آن " +
                                "ملت‌ها به جای جنگ، هدیه بیاورند. وقتی امروز به این ستون‌ها نگاه " +
                                "می‌کنیم، به ویرانه نگاه نمی‌کنیم؛ به نخستین طرح ایران نگاه می‌کنیم."
                        )
                    } else {
                        listOf(
                            "Persepolis is not the memorial of one king. It is the memory of a " +
                                "civilisation. Every crack in the stone marks the hands of people who " +
                                "believed a world could be built in which nations bring gifts instead " +
                                "of war. When we look at these columns today we are not looking at a " +
                                "ruin. We are looking at the first plan of Iran."
                        )
                    }
                )
            )
        ),
        credits = if (isPersian) {
            "تصاویر: ویکی‌مدیا کامانز، آژانس ایسنا، کاوه فرخ، دیستینیشن ایران"
        } else {
            "Photographs: Wikimedia Commons, ISNA, Kaveh Farrokh, Destination Iran"
        },
        sources = if (isPersian) {
            "منابع: پرونده میراث جهانی یونسکو، کتیبه‌های پارسه، گزارش‌های باستان‌شناسی آپادانا"
        } else {
            "Sources: the UNESCO World Heritage file, the inscriptions of Parsa, archaeological " +
                "reports on the Apadana"
        }
    )

    // ---------------------------------------------------------------------------------------
    // Mil-e Azhdaha / میل اژدها
    // ---------------------------------------------------------------------------------------

    private fun milAzhdaha(isPersian: Boolean): MonumentContent = MonumentContent(
        id = MonumentId.MIL_E_AZHDAHA,
        title = title(MonumentId.MIL_E_AZHDAHA, isPersian),
        kicker = if (isPersian) "اثر ملی ۳۵۶ · دوره اشکانی" else "National Heritage 356 · Parthian era",
        subtitle = if (isPersian) {
            "برج سنگی آتش در دل کوه‌های ممسنی"
        } else {
            "A stone fire tower in the mountains of Mamasani"
        },
        meta = if (isPersian) {
            listOf("نورآباد ممسنی، فارس", "پیش از اسلام")
        } else {
            listOf("Nurabad, Mamasani, Fars", "Pre-Islamic Iran")
        },
        heroRes = R.drawable.monument_mil_hero,
        lead = if (isPersian) {
            "در دره‌ای خاموش در شمال غرب فارس، برجی سنگی از دل صخره بالا آمده است؛ برجی که بیش " +
                "از دو هزار سال ایستاده و هنوز کسی به درستی نمی‌داند در آن چه می‌گذشت. میل اژدها " +
                "را برای شکوه نساخته‌اند؛ برای آتش ساخته‌اند. نشانی برای کاروان‌ها، چشمی برای " +
                "دیده‌بانی، و خانه‌ای برای نوری که نباید خاموش می‌شد."
        } else {
            "In a silent valley in north-western Fars, a stone tower rises out of the rock: a tower " +
                "that has stood for more than two thousand years, and no one can say with certainty " +
                "what passed inside it. Mil-e Azhdaha was not built for grandeur. It was built for " +
                "fire: a sign for caravans, an eye for the watch, and a house for a light that was " +
                "never allowed to go out."
        },
        stats = if (isPersian) {
            listOf(
                MonumentStat("۷", "متر، بلندای برج"),
                MonumentStat("۱۵", "ردیف سنگ سفیدرگ"),
                MonumentStat("۴", "متر، پهنای پایه"),
                MonumentStat("۳۵۶", "شماره ثبت در آثار ملی")
            )
        } else {
            listOf(
                MonumentStat("7 m", "Height of the tower"),
                MonumentStat("15", "Courses of veined stone"),
                MonumentStat("4 m", "Width at the base"),
                MonumentStat("356", "National Heritage number")
            )
        },
        blocks = listOf(
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۱" else "01",
                    title = if (isPersian) "برجی چهارگوش از سنگ سفیدرگ" else "A square tower of veined stone",
                    paragraphs = if (isPersian) {
                        listOf(
                            "میل اژدها از پانزده ردیف سنگ آهکی سفیدرگ ساخته شده است؛ سنگ‌هایی که " +
                                "با دقت تراش خورده و بدون ملات روی هم چیده شده‌اند. پهنای هر ضلع در " +
                                "پایه نزدیک به چهار متر است و در بالا کمی کمتر. برج قامتی کوتاه دارد، " +
                                "اما نسبت‌ها آشنا هستند: کعبه زرتشت در نقش رستم و برج زندان پاسارگاد " +
                                "نیز همین چهارگوشی سنگی را دارند. سه بنا، سه نسل از یک ذهنیت معماری.",
                            "درباره تاریخ ساخت بنا اختلاف است؛ بیشتر منابع آن را به دوره اشکانی نسبت " +
                                "می‌دهند و برخی پژوهشگران ساختش را تا سده سوم میلادی، هم‌مرز با آغاز " +
                                "ساسانیان، بالا می‌برند."
                        )
                    } else {
                        listOf(
                            "Mil-e Azhdaha is built of fifteen courses of veined limestone, cut with " +
                                "care and laid without mortar. Each side measures close to four metres " +
                                "at the base and a little less at the top. The tower is short, but its " +
                                "proportions are familiar: the Ka'ba-ye Zartosht at Naqsh-e Rostam and " +
                                "the prison tower at Pasargadae share the same stone square. Three " +
                                "buildings, three generations of one architectural mind.",
                            "Its date is debated. Most sources place it in the Parthian era; some " +
                                "scholars push its construction into the third century AD, at the " +
                                "threshold of the Sasanian period."
                        )
                    }
                )
            ),
            MonumentBlock.Figure(
                MonumentFigure(
                    imageRes = R.drawable.monument_mil_tower,
                    caption = if (isPersian) {
                        "برج از نزدیک؛ پانزده ردیف سنگ آهکی سفیدرگ، بدون ملات."
                    } else {
                        "The tower up close: fifteen courses of veined limestone, no mortar."
                    },
                    aspectRatio = 1080f / 1250f
                )
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۲" else "02",
                    title = if (isPersian) "آتش، بالای برج" else "The fire above the tower",
                    paragraphs = if (isPersian) {
                        listOf(
                            "بر فراز برج آتشدانی بود و آتش مقدس در آن نگه‌داری می‌شد. در آیین " +
                                "زرتشتی، آتش آلوده نمی‌شود؛ آنچه آلوده می‌شود، آدمی است. پس آتش را " +
                                "در بلندی و دور از دسترس نگه می‌داشتند. واندنبرگ، باستان‌شناس بلژیکی، " +
                                "معتقد بود برج نورآباد تنها بر روحانیان باز بود و نیایشگران از پایین، " +
                                "تنها روشنایی آتش را می‌دیدند."
                        )
                    } else {
                        listOf(
                            "A fire altar stood on top of the tower and the sacred fire was kept " +
                                "there. In the Zoroastrian understanding, fire is not polluted; it is " +
                                "people who are. So the fire was kept high, out of reach. Louis " +
                                "Vanden Berghe, the Belgian archaeologist, held that the Nurabad tower " +
                                "was open only to priests, and that worshippers below saw nothing but " +
                                "the glow of the flame."
                        )
                    }
                )
            ),
            MonumentBlock.Figure(
                MonumentFigure(
                    imageRes = R.drawable.monument_mil_valley,
                    caption = if (isPersian) {
                        "دشت و کوه در گذر روز؛ میل اژدها در کنار یکی از راه‌های باستانی فارس " +
                            "ایستاده است."
                    } else {
                        "Plain and mountain across the day: Mil-e Azhdaha stands beside one of the " +
                            "ancient roads of Fars."
                    },
                    aspectRatio = 1600f / 477f
                )
            ),
            MonumentBlock.Quote(
                MonumentQuote(
                    text = if (isPersian) {
                        "آتش را در بلندی نگه می‌داشتند تا از دور دیده شود."
                    } else {
                        "The fire was kept high so that it could be seen from far away."
                    },
                    attribution = null
                )
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۳" else "03",
                    title = if (isPersian) "چراغ راه کاروان‌ها" else "A lamp for the caravans",
                    paragraphs = if (isPersian) {
                        listOf(
                            "میل‌ها در دوره اشکانی و ساسانی کاربردهای بسیاری داشتند: راهنمایی " +
                                "کاروان‌ها، رساندن خبر با آتش، و دیده‌بانی بر راه‌های باستانی. میل " +
                                "اژدها در کنار یکی از راه‌های مهم فارس ساخته شد، در دره‌ای که هیچ " +
                                "آبادی‌ای نزدیکش نبود. بلندی‌اش هم نشانه‌ای برای راه بود و هم " +
                                "دیدبانی. بعدها همین شکل به مناره‌های دوره اسلامی رسید: بدنه باریک، " +
                                "قامت بلند و جایگاهی برای نور."
                        )
                    } else {
                        listOf(
                            "In Parthian and Sasanian times, towers like this one had many uses: " +
                                "guiding caravans, sending word by fire, watching the old roads. " +
                                "Mil-e Azhdaha was raised beside one of the important roads of Fars, " +
                                "in a valley with no settlement nearby. Its height was both a sign " +
                                "for the road and a post for the watch. The form later passed into " +
                                "the minarets of the Islamic period: a slender shaft, a tall stance, " +
                                "and a place for light."
                        )
                    }
                )
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۴" else "04",
                    title = if (isPersian) "آنچه ماند" else "What remains",
                    paragraphs = if (isPersian) {
                        listOf(
                            "در دوره پهلوی دوم، میله‌های فلزی را از میان سنگ‌ها گذراندند تا برج از " +
                                "فروپاشی در امان بماند. امروز این بنا با شماره ۳۵۶ در فهرست آثار " +
                                "ملی ایران ثبت شده است. از برج‌هایی که پیش از اسلام ساخته شدند، " +
                                "بسیاری برجای نمانده‌اند؛ میل اژدها یکی از همان چند تاست که هنوز " +
                                "سرپا ایستاده، در دل طبیعتی که دیوار و دروازه‌ای میان آن و آسمان " +
                                "نگذاشته است."
                        )
                    } else {
                        listOf(
                            "In the second Pahlavi period, metal rods were threaded through the " +
                                "stones to keep the tower from collapsing. Today it is registered as " +
                                "national heritage number 356. Of the towers built before Islam, many " +
                                "are gone; Mil-e Azhdaha is one of the few still standing, in a " +
                                "landscape that keeps no wall or gate between it and the sky."
                        )
                    }
                )
            ),
            MonumentBlock.Timeline(
                if (isPersian) {
                    listOf(
                        MonumentTimelineEntry("۲۴۷ پیش از میلاد", "اشکانیان شاهنشاهی را بنیان می‌گذارند."),
                        MonumentTimelineEntry("سده‌های نخستین میلادی", "برج چهارگوش در دره تنگ کله ساخته می‌شود."),
                        MonumentTimelineEntry("۲۲۴ میلادی", "ساسانیان بر سر کار می‌آیند و میل برجای می‌ماند."),
                        MonumentTimelineEntry("۱۳۲۱", "ثبت در فهرست آثار ملی با شماره ۳۵۶."),
                        MonumentTimelineEntry("امروز", "یکی از معدود برج‌های پیش از اسلام که هنوز ایستاده است.")
                    )
                } else {
                    listOf(
                        MonumentTimelineEntry("247 BC", "The Parthians found their empire."),
                        MonumentTimelineEntry("First centuries AD", "The square tower is built in the Tang-e Kaleh valley."),
                        MonumentTimelineEntry("224 AD", "The Sasanians come to power; the tower remains."),
                        MonumentTimelineEntry("1942", "Registered as national heritage number 356."),
                        MonumentTimelineEntry("Today", "One of the few pre-Islamic towers still standing.")
                    )
                }
            ),
            MonumentBlock.Chapter(
                MonumentChapter(
                    index = if (isPersian) "۰۵" else "05",
                    title = if (isPersian) "ایران، فراتر از کاخ‌ها" else "Iran, beyond the palaces",
                    paragraphs = if (isPersian) {
                        listOf(
                            "ایران تنها در کاخ‌های بزرگش زنده نیست. در برجی سنگی بر دامنه یک کوه " +
                                "هم زنده است؛ جایی که کسی برای روشن نگه داشتن یک آتش، سنگ‌ها را روی " +
                                "هم چید و دو هزار سال بعد، آن سنگ‌ها هنوز ایستاده‌اند."
                        )
                    } else {
                        listOf(
                            "Iran does not live only in its great palaces. It lives in a stone tower " +
                                "on the shoulder of a mountain, where someone stacked stones to keep a " +
                                "fire alight, and two thousand years later those stones are still " +
                                "standing."
                        )
                    }
                )
            )
        ),
        credits = if (isPersian) {
            "تصاویر: تهران تایمز، ویکی‌مدیا کامانز (عکس: محمدرضا اتابک)، ویزیت ایران"
        } else {
            "Photographs: Tehran Times, Wikimedia Commons (photo: Mohammadreza Atabak), Visit Iran"
        },
        sources = if (isPersian) {
            "منابع: پرونده آثار ملی ایران، ویزیت ایران، تهران تایمز، گزارش‌های باستان‌شناسی دوره اشکانی"
        } else {
            "Sources: the Iranian national heritage file, Visit Iran, Tehran Times, archaeological " +
                "reports on the Parthian period"
        }
    )
}
