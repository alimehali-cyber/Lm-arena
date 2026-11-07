package com.alijafari.red.astronomy.ui.monument

import androidx.annotation.DrawableRes
import com.alijafari.red.astronomy.R

/**
 * The two stone monuments the Home landscape stands on, and the full content of their pages.
 *
 * ### Why the content lives here and not in the catalogue
 *
 * These are not celestial objects: they have no right ascension, no rise and no transit, and the
 * object detail modal would have nothing to say about them. They are *places* under the sky, so they
 * get their own page type — a photographic, typographic essay rather than a data sheet — and their
 * text lives in this file instead of in `CanonicalAstroCatalog`.
 *
 * That separation is also what keeps `AstroBitIdentityTest` honest. The catalogue's SHA-256 baseline
 * fingerprints every object's text; a monument page that never enters the catalogue cannot move it.
 *
 * ### The rule for the prose
 *
 * Plain, technical Persian, one idea per sentence, numbers spelled with Persian digits and the
 * typographic separators this app uses (`٫` decimals, `٬` thousands). No sentence claims more than
 * the record supports: where a dating or a function is an attribution rather than a certainty, the
 * text says so — Mil-e Azhdaha famously has no inscription to settle either question.
 *
 * Both languages are carried for every string, because the app ships in both, and because a page of
 * this length cannot be written by translating a screen's labels on the fly.
 */
enum class MonumentId {
    PERSEPOLIS,
    MIL_E_AZHDAHA;

    /**
     * The id the sky canvas's selection state carries for this monument. It is deliberately outside
     * the catalogue's id space, so the callout can tell a monument from a star before it decides
     * where a tap on the name should go.
     */
    val selectionId: String get() = "monument_${name.lowercase()}"

    companion object {
        /** The prefix every monument selection id shares. */
        const val SELECTION_PREFIX = "monument_"

        /** The monument a selection id names, or null when the id belongs to the catalogue. */
        fun fromSelectionId(id: String): MonumentId? = when (id) {
            PERSEPOLIS.selectionId -> PERSEPOLIS
            MIL_E_AZHDAHA.selectionId -> MIL_E_AZHDAHA
            else -> null
        }
    }
}

/** A photograph on a monument page: the drawable, its caption and the credit line under it. */
data class MonumentPhoto(
    @DrawableRes val imageRes: Int,
    val caption: String,
    val credit: String
)

/** One numbered section: a heading, its paragraphs, and optionally a photograph after them. */
data class MonumentSection(
    val number: String,
    val title: String,
    val paragraphs: List<String>,
    val photo: MonumentPhoto? = null
)

/** One entry of the four-column fact strip under the lede. */
data class MonumentFact(val value: String, val label: String)

/** Everything one monument page renders, already resolved into one language. */
data class MonumentPage(
    val overline: String,
    val title: String,
    val subtitle: String,
    val lede: String,
    val hero: MonumentPhoto,
    val facts: List<MonumentFact>,
    val sections: List<MonumentSection>,
    val quote: String,
    val skyTitle: String,
    val skyParagraphs: List<String>,
    val skyAction: String,
    val creditTitle: String,
    val credits: List<String>
)

/** Resolves a monument into the language the app is currently in. */
object MonumentPages {

    fun of(id: MonumentId, isFa: Boolean): MonumentPage = when (id) {
        MonumentId.PERSEPOLIS -> if (isFa) PERSEPOLIS_FA else PERSEPOLIS_EN
        MonumentId.MIL_E_AZHDAHA -> if (isFa) MIL_FA else MIL_EN
    }

    // ------------------------------------------------------------------------------------------
    // تخت جمشید
    // ------------------------------------------------------------------------------------------

    private val PERSEPOLIS_FA = MonumentPage(
        overline = "میراث جهانی یونسکو · مرودشت، فارس",
        title = "تخت جمشید",
        subtitle = "پارسه، پایتخت تشریفاتی هخامنشیان",
        lede = "سنگ‌های تخت جمشید در دامنهٔ کوه رحمت، بر سکویی نشسته‌اند که نیمی از آن تراشیده و " +
            "نیمی دیگر ساخته شده است. این مجموعه حدود دویست سال پایتخت تشریفاتی هخامنشیان بود. " +
            "هر سال در نوروز، نمایندگان سرزمین‌های زیر فرمان، پیشکش‌ها را از پلکان‌های آن بالا " +
            "می‌بردند. همان پیشکش‌ها بر سنگ‌ها مانده‌اند.",
        hero = MonumentPhoto(
            imageRes = R.drawable.img_monument_persepolis_hero,
            caption = "دروازهٔ ملل در غروب؛ خورشید از میان دهانهٔ دروازه می‌گذرد.",
            credit = "عکس: امین‌حسینی · CC BY-SA 4.0 · ویکی‌مدیا کامانز"
        ),
        facts = listOf(
            MonumentFact("۵۱۸ پ.م.", "آغاز ساخت"),
            MonumentFact("۱۲۵٬۰۰۰ متر مربع", "مساحت سکو"),
            MonumentFact("۱۹٫۲ متر", "بلندترین ستون"),
            MonumentFact("۱۹۷۹", "ثبت جهانی")
        ),
        sections = listOf(
            MonumentSection(
                number = "۰۱",
                title = "سکو، پیش از کاخ",
                paragraphs = listOf(
                    "ساخت تخت جمشید حدود سال ۵۱۸ پیش از میلاد با داریوش یکم آغاز شد. پس از او " +
                        "خشایارشا و اردشیر یکم آن را گسترش دادند. پیش از هر کاخی، سکو ساخته شد: " +
                        "بخشی از دامنهٔ کوه رحمت را تراشیدند و بخشی را با سنگ و خاک پر کردند تا " +
                        "زمینی هموار در حدود ۱۲۵ هزار متر مربع فراهم شود.",
                    "سنگ‌ها را بی‌ملات روی هم چیده‌اند. برای پیوندشان بست فلزی به کار رفته و درزها " +
                        "را با سرب پر کرده‌اند. به همین دلیل، سکو با همهٔ زمین‌لرزه‌های فلات ایران، " +
                        "پس از ۲۵۰۰ سال هنوز بار کاخ‌ها را نگه می‌دارد."
                )
            ),
            MonumentSection(
                number = "۰۲",
                title = "آپادانا",
                paragraphs = listOf(
                    "آپادانا تالار بار عام بود: ۷۲ ستون سنگی در شش ردیف دوازده‌تایی، با بلندی " +
                        "نزدیک به ۱۹ متر و سرستون‌هایی به شکل دو گاو. امروز ۱۳ ستون از آن برجاست و " +
                        "از بقیه تنها پایه‌ها مانده است.",
                    "پلکان‌های شرقی و شمالی آپادانا سه ردیف نقش برجسته دارند. ردیف میانی صف " +
                        "نمایندگان سرزمین‌های زیر فرمان است؛ هر گروه با لباس و پیشکش خودش، از ماد و " +
                        "پارس تا باختر و هند. این نقش‌ها را بیشتر با نوروز می‌شناسند: روزی که " +
                        "سرزمین‌ها پیشکششان را به شاه می‌رساندند.",
                    "سنگ‌نوشته‌های پی‌بنا را در گوشه‌های سکو دفن کرده بودند. بر آن‌ها نام سازنده و " +
                        "فهرست سرزمین‌های زیر فرمان آمده است."
                )
            ),
            MonumentSection(
                number = "۰۳",
                title = "دروازهٔ ملل",
                paragraphs = listOf(
                    "خشایارشا در ورودی شمالی سکو، دروازهٔ ملل را ساخت: تالاری با چهار ستون " +
                        "۱۶٫۵ متری و دو گاو بالدار سنگی در دو سو. هر کس از پلکان بالا می‌آمد، از " +
                        "میان این دو نگهبان می‌گذشت.",
                    "بر دروازه، سنگ‌نوشته‌ای سه‌زبانه از خشایارشا مانده است: پارسی باستان، ایلامی " +
                        "و بابلی. در آن، ساخت دروازه به خواست اهورامزدا نسبت داده شده است."
                ),
                photo = MonumentPhoto(
                    imageRes = R.drawable.img_monument_persepolis_columns,
                    caption = "دو ستون دروازهٔ ملل با سرستون دوسر گاو؛ بلندی ستون‌ها ۱۶٫۵ متر بود.",
                    credit = "عکس: کارول راداتو · CC BY-SA 2.0 · ویکی‌مدیا کامانز"
                )
            ),
            MonumentSection(
                number = "۰۴",
                title = "تالار صد ستون و گنجینه",
                paragraphs = listOf(
                    "تالار صد ستون، تالار تخت بود: سقفی بر صد ستون چوبی که امروز تنها پایه‌های " +
                        "سنگی‌اش مانده است. بر دو ورودی آن، نقش شاه بر تخت و صف سربازان حجاری شده.",
                    "در جنوب شرقی سکو گنجینه قرار داشت. در کاوش‌های دههٔ ۱۹۳۰، هزاران لوح گلی به خط " +
                        "میخی ایلامی در آن پیدا شد: گزارش کارگاه، جیرهٔ کارگران، دستمزد و " +
                        "رفت‌وآمد کالا. این لوح‌ها زندگی روزانهٔ کارگاه را نگه داشته‌اند؛ چیزی که " +
                        "سنگ‌ها به تنهایی از آن نمی‌گویند."
                )
            ),
            MonumentSection(
                number = "۰۵",
                title = "آتش، ۳۳۰ پیش از میلاد",
                paragraphs = listOf(
                    "در سال ۳۳۰ پیش از میلاد، اسکندر مقدونی پس از ورود به پارس، تخت جمشید را آتش " +
                        "زد. منابع باستانی این کار را تلافی آتش زدن آتن می‌دانند. برخی پژوهشگران " +
                        "آن را تصمیم سیاسی برای پایان دادن به نماد شاهنشاهی می‌دانند. از آن روز، " +
                        "لایهٔ سوختگی و ترک‌های حرارت بر سنگ‌ها مانده است."
                )
            ),
            MonumentSection(
                number = "۰۶",
                title = "از خاک تا فهرست جهانی",
                paragraphs = listOf(
                    "کاوش علمی تخت جمشید در ۱۹۳۱ با ارنست هرتسفلد و سپس اریک اشمیت، برای مؤسسهٔ " +
                        "شرق‌شناسی دانشگاه شیکاگو، آغاز شد. پیش از آن، بخش بزرگی از سکو زیر خاک بود " +
                        "و سنگ‌های پراکنده در روستاهای اطراف به کار رفته بود.",
                    "در ۱۹۷۹، تخت جمشید در فهرست میراث جهانی یونسکو ثبت شد؛ شمارهٔ ۱۱۴، با معیارهای " +
                        "یک، سه و شش. امروز کاوش و مرمت آن زیر نظر پایگاه میراث جهانی " +
                        "پارسه‌پاسارگاد ادامه دارد."
                ),
                photo = MonumentPhoto(
                    imageRes = R.drawable.img_monument_persepolis_relief,
                    caption = "پلکان شرقی آپادانا: صف مادها و پارس‌ها، با گل‌های نیلوفر بر حاشیهٔ سنگ.",
                    credit = "عکس: بونتنبال · CC BY-SA 3.0 · ویکی‌مدیا کامانز"
                )
            )
        ),
        quote = "کاخ‌ها رفته‌اند. سکو مانده است.",
        skyTitle = "این شب، از مرودشت",
        skyParagraphs = listOf(
            "در صحنهٔ خانه، تخت جمشید همان‌جایی ایستاده که هر شب می‌ایستد: در دوردست چپ دشت، با " +
                "همان نور خورشید و ماهی که آسمان بالای سرتان را روشن می‌کند. اگر شبی بی‌ماه و صاف " +
                "به مرودشت برسید، کهکشان راه شیری از بالای کوه رحمت بالا می‌آید و ستون‌های آپادانا " +
                "تیره‌ترین چیز قاب می‌شوند.",
            "برای دیدن آسمان همان لحظه، انگشتتان را روی صحنه بکشید: زمان جلو و عقب می‌رود و نور " +
                "روی سنگ‌ها با آن جابه‌جا می‌شود."
        ),
        skyAction = "بازگشت به آسمان",
        creditTitle = "عکس‌ها و منابع",
        credits = listOf(
            "عکس‌های تخت جمشید از ویکی‌مدیا کامانز، با پروانهٔ CC BY-SA؛ نام عکاس زیر هر عکس آمده است.",
            "داده‌های تاریخی: سنگ‌نوشته‌های هخامنشی، گزارش‌های کاوش مؤسسهٔ شرق‌شناسی دانشگاه شیکاگو، " +
                "پروندهٔ میراث جهانی یونسکو (شمارهٔ ۱۱۴) و دانشنامهٔ ایرانیکا."
        )
    )

    private val PERSEPOLIS_EN = MonumentPage(
        overline = "UNESCO World Heritage · Marvdasht, Fars",
        title = "Persepolis",
        subtitle = "Parseh, the ceremonial capital of the Achaemenid empire",
        lede = "The stone of Persepolis rests against the foot of Mount Rahmat, on a terrace that is " +
            "half cut into the rock and half built up. For nearly two hundred years this was the " +
            "ceremonial capital of the Achaemenids. Every Nowruz, delegations from the lands under " +
            "their rule carried their gifts up its stairways.",
        hero = MonumentPhoto(
            imageRes = R.drawable.img_monument_persepolis_hero,
            caption = "The Gate of All Nations at sunset, with the sun passing through its opening.",
            credit = "Photo: Aminhosseini722 · CC BY-SA 4.0 · Wikimedia Commons"
        ),
        facts = listOf(
            MonumentFact("518 BC", "Construction begins"),
            MonumentFact("125,000 m²", "Terrace area"),
            MonumentFact("19.2 m", "Tallest column"),
            MonumentFact("1979", "World Heritage")
        ),
        sections = listOf(
            MonumentSection(
                number = "01",
                title = "The terrace came first",
                paragraphs = listOf(
                    "Work began around 518 BC under Darius I; Xerxes and Artaxerxes I extended it " +
                        "after him. Before any palace, the terrace was built: part of the slope of " +
                        "Mount Rahmat was cut away, part was filled with stone and earth, until " +
                        "roughly 125,000 square metres of level ground existed.",
                    "The blocks are laid without mortar. Metal clamps tie them together and the " +
                        "joints are sealed with lead — which is why the terrace still carries the " +
                        "weight of the palaces after 2,500 years and every earthquake on the plateau."
                )
            ),
            MonumentSection(
                number = "02",
                title = "The Apadana",
                paragraphs = listOf(
                    "The Apadana was the audience hall: 72 stone columns in six rows of twelve, " +
                        "close to 19 m tall, with capitals shaped as paired bulls. Thirteen columns " +
                        "stand today; of the rest, only the bases remain.",
                    "The eastern and northern stairways carry three registers of relief. The middle " +
                        "one is a procession of delegations — Medes and Persians, Bactrians, Indians " +
                        "and more — each in its own dress, each with its own gift. The scene is " +
                        "usually read as Nowruz: the day the lands brought their tribute to the king.",
                    "The foundation tablets also record the names of those who worked on the terrace, " +
                        "which makes them one of the main sources for how Achaemenid construction was " +
                        "organised."
                )
            ),
            MonumentSection(
                number = "03",
                title = "The Gate of All Nations",
                paragraphs = listOf(
                    "At the northern entrance Xerxes built the Gate of All Nations: a hall of four " +
                        "columns 16.5 m high, flanked by two winged stone bulls. Everyone who came up " +
                        "the stairway passed between those guardians.",
                    "A trilingual inscription of Xerxes survives on the gate — Old Persian, Elamite " +
                        "and Babylonian — attributing the building to the favour of Ahuramazda."
                ),
                photo = MonumentPhoto(
                    imageRes = R.drawable.img_monument_persepolis_columns,
                    caption = "Two columns of the Gate of All Nations, with double-bull capitals. " +
                        "The columns stood 16.5 m high.",
                    credit = "Photo: Carole Raddato · CC BY-SA 2.0 · Wikimedia Commons"
                )
            ),
            MonumentSection(
                number = "04",
                title = "The Hall of a Hundred Columns and the treasury",
                paragraphs = listOf(
                    "The Hall of a Hundred Columns was the throne hall: a roof on a hundred timber " +
                        "columns, of which only the stone bases remain. Its two doorways carry the " +
                        "king on his throne, and ranks of soldiers.",
                    "In the south-east corner stood the treasury. Excavations in the 1930s found " +
                        "thousands of clay tablets there, written in Elamite cuneiform: workshop " +
                        "records, worker rations, wages, goods moving in and out. They preserve the " +
                        "working life of the place, which the stone alone does not tell."
                )
            ),
            MonumentSection(
                number = "05",
                title = "The fire, 330 BC",
                paragraphs = listOf(
                    "In 330 BC, after entering Persia, Alexander of Macedon set Persepolis alight. " +
                        "Ancient sources call it revenge for the burning of Athens; some historians " +
                        "read it as a political decision to end the symbol of the empire. What " +
                        "remains is a layer of burning and the cracks that heat leaves in limestone."
                )
            ),
            MonumentSection(
                number = "06",
                title = "From under the soil to the World Heritage list",
                paragraphs = listOf(
                    "Scientific excavation began in 1931 with Ernst Herzfeld and then Erich Schmidt, " +
                        "for the Oriental Institute of the University of Chicago. Before that, much " +
                        "of the terrace was buried, and loose stone had been carried off to nearby " +
                        "villages.",
                    "In 1979 Persepolis was inscribed on the UNESCO World Heritage list — number 114, " +
                        "criteria i, iii and vi. Excavation and conservation continue today under the " +
                        "Parseh-Pasargadae World Heritage base."
                ),
                photo = MonumentPhoto(
                    imageRes = R.drawable.img_monument_persepolis_relief,
                    caption = "The eastern stairway of the Apadana: Medes and Persians, with lotus " +
                        "rosettes along the border.",
                    credit = "Photo: Bontenbal · CC BY-SA 3.0 · Wikimedia Commons"
                )
            )
        ),
        quote = "The terrace has carried its own weight for 2,500 years. The palaces are gone.",
        skyTitle = "Tonight, from Marvdasht",
        skyParagraphs = listOf(
            "In ZIG's Home scene, Persepolis stands where it stands every night: in the left " +
                "distance of the plain, lit by the same Sun and Moon that light the sky above you. " +
                "On a clear, moonless night in Marvdasht the Milky Way rises over Mount Rahmat, and " +
                "the columns of the Apadana become the darkest thing in the frame.",
            "To see the sky of that same moment, drag a finger across the scene: time runs forward " +
                "and back, and the light moves across the stone with it."
        ),
        skyAction = "Back to the sky",
        creditTitle = "Photographs and sources",
        credits = listOf(
            "Persepolis photographs from Wikimedia Commons under CC BY-SA licences; the photographer " +
                "is credited beneath each image.",
            "Historical data: Achaemenid royal inscriptions, the Oriental Institute excavation " +
                "reports, the UNESCO World Heritage file (114), and Encyclopaedia Iranica."
        )
    )

    // ------------------------------------------------------------------------------------------
    // میل اژدها
    // ------------------------------------------------------------------------------------------

    private val MIL_FA = MonumentPage(
        overline = "اثر ملی · نورآباد ممسنی، فارس",
        title = "میل اژدها",
        subtitle = "برج سنگی بازمانده از دورهٔ اشکانی، در درهٔ تنگ کلاغی",
        lede = "هفت کیلومتر باختر نورآباد ممسنی، در دره‌ای بی‌آبادی کنار کوه تنگ کلاغی، برجی " +
            "چهارگوش از سنگ آهک سفیدرگه ایستاده است. بلندی‌اش از هفت متر بیشتر است و پایه‌اش چهار " +
            "متر پهنا دارد. این شکل ساده، آن را یکی از اندک میل‌های پیش از اسلام کرده است که تا " +
            "امروز برپا مانده‌اند.",
        hero = MonumentPhoto(
            imageRes = R.drawable.img_monument_mil_hero,
            caption = "بدنهٔ برج: رج‌های سنگ آهک سفیدرگه، بازشوی میانه و اتاقک فروریختهٔ سر برج.",
            credit = "عکس: محمدرضا اتابک · CC0 · ویکی‌مدیا کامانز"
        ),
        facts = listOf(
            MonumentFact("اشکانی", "دورهٔ ساخت"),
            MonumentFact("بیش از ۷ متر", "بلندی برج"),
            MonumentFact("۴ متر", "پهنای پایه"),
            MonumentFact("۳۵۶", "شمارهٔ ثبت ملی")
        ),
        sections = listOf(
            MonumentSection(
                number = "۰۱",
                title = "برجی بی‌کتیبه",
                paragraphs = listOf(
                    "میل اژدها کتیبه‌ای ندارد که نام سازنده یا سال ساختش را بگوید. باستان‌شناسان " +
                        "بر پایهٔ شکل بنا، شیوهٔ تراش سنگ و جای آن در کنار یکی از راه‌های باستانی، " +
                        "آن را به دورهٔ اشکانی نسبت می‌دهند؛ دوره‌ای که ساخت میل و برج راهنما در " +
                        "ایران رواج داشت.",
                    "به این بنا نام‌های دیگری هم گفته‌اند: آتشگاه، دیمه‌میل و میل آزاد. در فهرست " +
                        "آثار ملی ایران، شمارهٔ ثبت ۳۵۶ و تاریخ ثبت ۲۰ خرداد ۱۳۲۱ آمده است."
                )
            ),
            MonumentSection(
                number = "۰۲",
                title = "سنگ‌ها، بی‌ملات",
                paragraphs = listOf(
                    "دیواره‌ها از سنگ آهک سفیدرگه ساخته شده‌اند؛ رگه‌های روشن در دل سنگِ تیره‌تر " +
                        "دیده می‌شوند و همین، برج را از فاصلهٔ دور هم قابل تشخیص می‌کند.",
                    "بلندی بلوک‌ها یکسان نیست؛ برخی نزدیک ۴۴ و برخی نزدیک ۱۹ سانتی‌متر است. " +
                        "سنگ‌ها را بی‌ملات و با نظم دقیق روی هم چیده‌اند. پهنای هر دیواره در پایین " +
                        "چهار متر است و در بالا به نزدیک ۳٫۴ متر می‌رسد؛ همین شیب اندک، وزن دیوار " +
                        "را به مرکز برمی‌گرداند."
                ),
                photo = MonumentPhoto(
                    imageRes = R.drawable.img_monument_mil_detail,
                    caption = "برج در درهٔ تنگ کلاغی، بر لبهٔ سکوی سنگی؛ دشت و کوه در پس‌زمینه.",
                    credit = "عکس: ویکی‌مدیا کامانز"
                )
            ),
            MonumentSection(
                number = "۰۳",
                title = "پلکان پنهان",
                paragraphs = listOf(
                    "در بخش بالایی برج، پلکانی به درازای حدود ۱۰٫۵ متر و پهنای ۵۹ سانتی‌متر هست " +
                        "که از سمت جنوب و از ارتفاع نزدیک سه متری آغاز می‌شود. ظاهراً راه رسیدن به " +
                        "آن نردبان بوده است: از بیرون بالا می‌رفتند و از پلکان به بالای برج " +
                        "می‌رسیدند.",
                    "در دورهٔ پهلوی، میله‌ای فلزی از میان سنگ‌های برج گذراندند تا از ریزش بیشتر " +
                        "جلوگیری کنند."
                )
            ),
            MonumentSection(
                number = "۰۴",
                title = "آتش بر فراز",
                paragraphs = listOf(
                    "بر فراز برج جای آتشدان بوده است. بر پایهٔ روایت‌ها، آتش مقدس در همین بخش بالا " +
                        "نگه داشته می‌شد و از آنجا به جاهای دیگر می‌بردند. وان دنبرگ، باستان‌شناس، " +
                        "بر آن بود که برج تنها به روی دین‌مداران باز بود و نیایشگران آتش را از پایین " +
                        "می‌دیدند.",
                    "در کنار کارکرد آیینی، سه کاربرد دیگر هم برای میل گفته شده است: راهنمای " +
                        "کاروان‌ها در شب، دیدبانی، و نشان دادن مسیر راه‌های باستانی که از این دره " +
                        "می‌گذشتند."
                )
            ),
            MonumentSection(
                number = "۰۵",
                title = "شبیه کعبهٔ زرتشت، نه همان",
                paragraphs = listOf(
                    "چهارگوشی و سنگ‌تراشی برج، آن را بارها کنار کعبهٔ زرتشت در نقش رستم و برج زندان " +
                        "پاسارگاد نشانده است. هر سه، برج‌های سنگی چهارگوش ایران پیش از اسلام‌اند؛ " +
                        "اما شباهت ظاهری دلیل یکی بودن کارکردشان نیست و برای هر کدام کاربری " +
                        "جداگانه بحث شده است."
                )
            ),
            MonumentSection(
                number = "۰۶",
                title = "دو بنا، یک استان",
                paragraphs = listOf(
                    "تخت جمشید در دشت مرودشت است و میل اژدها در درهٔ نورآباد ممسنی؛ هر دو در استان " +
                        "فارس و میانشان نزدیک ۱۳۰ کیلومتر راه است. صحنه‌ای که در خانه می‌بینید این " +
                        "دو را در یک قاب گذاشته است: تخت جمشید در دوردست چپ و برج در راست، بر " +
                        "دامنهٔ سنگ‌آهکی، با دشت و رود پلوار میانشان.",
                    "این ترکیب را همین برنامه ساخته است، نه موقعیت واقعی دو بنا نسبت به هم."
                )
            )
        ),
        quote = "برجی که کتیبه ندارد، خودش سند است: سنگ‌هایی که دو هزار سال سر جایشان مانده‌اند.",
        skyTitle = "این شب، از دره",
        skyParagraphs = listOf(
            "در مدل شبانهٔ برنامه، وقتی خورشید به زیر ۲ درجه می‌رسد، در دو بازشوی برج آتش روشن " +
                "می‌شود: یکی در بازشوی میانهٔ بدنه و یکی در دهانهٔ اتاقک سر برج. همان خورشیدی که " +
                "روشنش کرده، خاموشش می‌کند.",
            "آسمان بالای این دره از آسمان شهر بهتر است: نور کم، افق باز و کوهی که بخشی از آلودگی " +
                "نوری دوردست را می‌گیرد. شبی بی‌ماه، راه شیری تا نزدیک افق پایین می‌آید."
        ),
        skyAction = "بازگشت به آسمان",
        creditTitle = "عکس‌ها و منابع",
        credits = listOf(
            "عکس‌های میل اژدها از ویکی‌مدیا کامانز؛ جزئیات پروانه و نام عکاس در پروندهٔ همین " +
                "مخزن، در THIRD_PARTY_LICENSES/monument-photos.md آمده است.",
            "داده‌های تاریخی: پروندهٔ ثبت ملی (شمارهٔ ۳۵۶)، دانشنامهٔ تاریخ معماری و شهرسازی " +
                "ایران‌شهر، و گزارش‌های باستان‌شناسی میل‌های دورهٔ اشکانی."
        )
    )

    private val MIL_EN = MonumentPage(
        overline = "National monument · Nourabad Mamasani, Fars",
        title = "Mil-e Azhdaha",
        subtitle = "A Parthian stone tower in the Tang-e Kalaghi valley",
        lede = "Seven kilometres west of Nourabad Mamasani, in an empty valley beside Mount " +
            "Tang-e Kalaghi, stands a four-sided tower of white-veined limestone. It is a little " +
            "over seven metres tall and four metres wide at its base. That plain geometry makes it " +
            "one of the few pre-Islamic towers in Iran still standing.",
        hero = MonumentPhoto(
            imageRes = R.drawable.img_monument_mil_hero,
            caption = "The shaft: courses of veined limestone, the mid-wall opening, and the " +
                "collapsed chamber at the summit.",
            credit = "Photo: Mohammadreza Atabak · CC0 · Wikimedia Commons"
        ),
        facts = listOf(
            MonumentFact("Parthian", "Period"),
            MonumentFact("7+ m", "Tower height"),
            MonumentFact("4 m", "Base width"),
            MonumentFact("356", "National register no.")
        ),
        sections = listOf(
            MonumentSection(
                number = "01",
                title = "A tower without an inscription",
                paragraphs = listOf(
                    "Mil-e Azhdaha carries no inscription naming its builder or its date. On the " +
                        "evidence of its form, its masonry and its position beside an ancient road, " +
                        "archaeologists attribute it to the Parthian period — an era in which " +
                        "guide-towers were common across Iran.",
                    "The building has other names too: Atashgah, Deymeh-Mil and Mil-e Azad. In the " +
                        "Iranian national register it is entry 356, recorded on 20 Khordad 1321."
                )
            ),
            MonumentSection(
                number = "02",
                title = "Stone without mortar",
                paragraphs = listOf(
                    "The walls are white-veined limestone: pale veins run through darker stone, " +
                        "which is what makes the tower visible from a distance.",
                    "The blocks are not uniform; some are close to 44 cm, others to 19 cm. They are " +
                        "laid without mortar and with great regularity. Each wall is four metres " +
                        "wide at the base and about 3.4 metres at the top — that slight taper returns " +
                        "the weight of the wall toward its centre."
                ),
                photo = MonumentPhoto(
                    imageRes = R.drawable.img_monument_mil_detail,
                    caption = "The tower in the Tang-e Kalaghi valley, on the edge of its stone " +
                        "platform, with the plain and mountains behind.",
                    credit = "Photo: Wikimedia Commons"
                )
            ),
            MonumentSection(
                number = "03",
                title = "The hidden stair",
                paragraphs = listOf(
                    "In the upper part of the tower there is a stair about 10.5 m long and 59 cm " +
                        "wide, beginning on the south side roughly three metres up. It seems a " +
                        "ladder was the way in: you climbed the outside, then took the stair to the " +
                        "top.",
                    "In the Pahlavi era a metal rod was run through the stones to slow further " +
                        "collapse."
                )
            ),
            MonumentSection(
                number = "04",
                title = "Fire at the summit",
                paragraphs = listOf(
                    "A fire altar stood at the top of the tower. By the usual account the sacred " +
                        "fire was kept in that upper chamber and carried from it to other places. " +
                        "Van den Berg argued that the tower was open only to the religious, and that " +
                        "worshippers saw the fire from below.",
                    "Besides the ritual use, three other functions are proposed: guiding caravans at " +
                        "night, look-out duty, and marking the ancient roads that ran through the " +
                        "valley."
                )
            ),
            MonumentSection(
                number = "05",
                title = "Like the Ka'ba-ye Zartosht, not the same",
                paragraphs = listOf(
                    "Its square plan and ashlar masonry have often placed it beside the Ka'ba-ye " +
                        "Zartosht at Naqsh-e Rustam and the Zendan at Pasargadae. All three are " +
                        "square stone towers of pre-Islamic Iran; a shared shape is not a shared " +
                        "function, and each has been argued separately."
                )
            ),
            MonumentSection(
                number = "06",
                title = "Two monuments, one province",
                paragraphs = listOf(
                    "Persepolis is on the Marvdasht plain; Mil-e Azhdaha is in the Nourabad Mamasani " +
                        "valley. Both are in Fars, about 130 km apart. The scene on ZIG's Home " +
                        "screen puts them in one frame: Persepolis in the left distance, the tower on " +
                        "the right on its limestone slope, with the plain and the Pulvar between them.",
                    "That arrangement is this app's own composition, not the real position of the " +
                        "two sites relative to each other."
                )
            )
        ),
        quote = "A tower with no inscription is its own document: stones that have stayed put for " +
            "two thousand years.",
        skyTitle = "Tonight, from the valley",
        skyParagraphs = listOf(
            "In the app's night model, once the Sun drops below −2° a fire is lit in two openings " +
                "of the tower: the mid-shaft doorway and the mouth of the summit chamber. The same " +
                "Sun that lit it puts it out.",
            "The sky above this valley is better than a city's: little light, an open horizon, and " +
                "a mountain that hides part of the distant glow. On a moonless night the Milky Way " +
                "comes down close to the horizon."
        ),
        skyAction = "Back to the sky",
        creditTitle = "Photographs and sources",
        credits = listOf(
            "Mil-e Azhdaha photographs from Wikimedia Commons; licence and photographer details are " +
                "recorded in THIRD_PARTY_LICENSES/monument-photos.md.",
            "Historical data: the national register file (356), the Encyclopaedia of Iranian " +
                "Architecture and Urbanism (Iranshahr), and archaeological reports on Parthian towers."
        )
    )
}
