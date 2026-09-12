package com.alijafari.red.astronomy.data.catalog

import com.alijafari.red.astronomy.astro_engine.DeepSkyCatalog as EngineDeepSkyCatalog

internal data class BilingualFacts(
    val en: List<String>,
    val fa: List<String>
)

internal object DeepSkyContent {

    fun factsForCanonicalId(canonicalId: String): BilingualFacts? =
        factsByCanonicalId[canonicalId] ?: DeepSkyGeneratedFacts.factsForCanonicalId(canonicalId)

    fun typeNameFa(type: EngineDeepSkyCatalog.ObjectType): String = when (type) {
        EngineDeepSkyCatalog.ObjectType.GALAXY -> "کهکشان"
        EngineDeepSkyCatalog.ObjectType.SPIRAL_GALAXY -> "کهکشان مارپیچی"
        EngineDeepSkyCatalog.ObjectType.ELLIPTICAL_GALAXY -> "کهکشان بیضوی"
        EngineDeepSkyCatalog.ObjectType.IRREGULAR_GALAXY -> "کهکشان نامنظم"
        EngineDeepSkyCatalog.ObjectType.LENTICULAR_GALAXY -> "کهکشان عدسی"
        EngineDeepSkyCatalog.ObjectType.DIFFUSE_NEBULA -> "سحابی پخشی"
        EngineDeepSkyCatalog.ObjectType.PLANETARY_NEBULA -> "سحابی سیاره‌نما"
        EngineDeepSkyCatalog.ObjectType.SUPERNOVA_REMNANT -> "بازمانده ابرنواختر"
        EngineDeepSkyCatalog.ObjectType.GLOBULAR_CLUSTER -> "خوشه کروی"
        EngineDeepSkyCatalog.ObjectType.OPEN_CLUSTER -> "خوشه باز"
        EngineDeepSkyCatalog.ObjectType.STAR_CLOUD -> "ابر ستاره‌ای / صورتواره"
        EngineDeepSkyCatalog.ObjectType.DOUBLE_STAR -> "ستاره دوتایی"
        EngineDeepSkyCatalog.ObjectType.STAR -> "ستاره"
    }

    fun persianNameForEngineObject(
        obj: EngineDeepSkyCatalog.DeepSkyObject,
        designationText: String
    ): String {
        val cleanCommon = obj.commonName?.removeSuffix(" Alt")?.trim()
        val commonFa = cleanCommon?.let { commonNameFa[it] ?: transliterateLatinPhrase(it) }
        val base = commonFa ?: typeNameFa(obj.type)
        return if (designationText.isNotBlank()) "$base ($designationText)" else base
    }

    fun persianDescriptionForEngineObject(
        obj: EngineDeepSkyCatalog.DeepSkyObject,
        designationText: String,
        constellationFa: String
    ): String {
        val typeFa = typeNameFa(obj.type)
        val cleanCommon = obj.commonName?.removeSuffix(" Alt")?.trim()
        val named = cleanCommon?.let { commonNameFa[it] ?: transliterateLatinPhrase(it) }
        val subject = named ?: "$typeFa $designationText".trim()
        return if (designationText.isNotBlank()) {
            "$subject یک $typeFa در صورت فلکی $constellationFa است و با شناسه‌های $designationText در فهرست‌های رصدی شناخته می‌شود."
        } else {
            "$subject یک $typeFa در صورت فلکی $constellationFa است."
        }
    }

    private val commonNameFa = mapOf(
        "Crab Nebula" to "سحابی خرچنگ",
        "Butterfly Cluster" to "خوشه پروانه",
        "Ptolemy Cluster" to "خوشه بطلمیوس",
        "Lagoon Nebula" to "سحابی مرداب",
        "Wild Duck Cluster" to "خوشه اردک وحشی",
        "Hercules Cluster" to "خوشه هرکول",
        "Eagle Nebula" to "سحابی عقاب",
        "Omega Nebula" to "سحابی اومگا",
        "Trifid Nebula" to "سحابی سه‌تکه",
        "Sagittarius Cluster" to "خوشه کمان",
        "Sagittarius Star Cloud" to "ابر ستاره‌ای کمان",
        "Dumbbell Nebula" to "سحابی دمبل",
        "Cooling Tower" to "خوشه برج خنک‌کننده",
        "Andromeda Galaxy" to "کهکشان آندرومدا",
        "Triangulum Galaxy" to "کهکشان مثلث",
        "Pinwheel Cluster" to "خوشه فرفره‌ای",
        "Starfish Cluster" to "خوشه ستاره‌دریایی",
        "Winnecke 4" to "وینکه ۴",
        "Orion Nebula" to "سحابی جبار",
        "de Mairan's Nebula" to "سحابی دو مایران",
        "Beehive Cluster" to "خوشه کندوی عسل",
        "Pleiades" to "پروین / ثریا",
        "Whirlpool Galaxy" to "کهکشان گرداب",
        "Ring Nebula" to "سحابی حلقه",
        "Sunflower Galaxy" to "کهکشان آفتابگردان",
        "Black Eye Galaxy" to "کهکشان چشم سیاه",
        "M73 Asterism" to "صورتواره M73",
        "Little Dumbbell" to "سحابی دمبل کوچک",
        "Cetus A" to "قیطس ای",
        "Bode's Galaxy" to "کهکشان بوده",
        "Cigar Galaxy" to "کهکشان سیگار",
        "Southern Pinwheel" to "فرفره جنوبی",
        "Virgo A" to "سنبله ای",
        "Owl Nebula" to "سحابی جغد",
        "Pinwheel Galaxy" to "کهکشان فرفره",
        "Spindle Galaxy" to "کهکشان دوک",
        "Needle Galaxy" to "کهکشان سوزن",
        "Whale Galaxy" to "کهکشان نهنگ",
        "Sombrero Galaxy" to "کهکشان کلاه‌مکزیکی",
        "Surfboard Galaxy" to "کهکشان تخته‌موج‌سواری",
        "North America Nebula" to "سحابی آمریکای شمالی",
        "Veil Nebula (West)" to "سحابی حجاب غربی",
        "Veil Nebula (East)" to "سحابی حجاب شرقی",
        "Helix Nebula" to "سحابی مارپیچ",
        "Eskimo Nebula" to "سحابی اسکیمو",
        "Cat's Eye Nebula" to "سحابی چشم گربه",
        "Saturn Nebula" to "سحابی زحل",
        "Blue Snowball" to "سحابی گلوله‌برفی آبی",
        "Double Cluster (West)" to "خوشه دوگانه غربی",
        "Double Cluster (East)" to "خوشه دوگانه شرقی",
        "Rosette Nebula Cluster" to "خوشه سحابی رزت",
        "Rosette Nebula" to "سحابی رزت",
        "Christmas Tree Cluster" to "خوشه درخت کریسمس",
        "Carina Nebula" to "سحابی شاه‌تخته",
        "Tarantula Nebula" to "سحابی رتیل",
        "Omega Centauri" to "امگا قنطورس",
        "47 Tucanae" to "۴۷ توکانا",
        "Wishing Well Cluster" to "خوشه چاه آرزو",
        "Jewel Box Cluster" to "خوشه جعبه جواهر",
        "Northern Jewel Box" to "جعبه جواهر شمالی",
        "Centaurus A" to "قنطورس ای",
        "Sculptor Galaxy" to "کهکشان پیکرتراش",
        "Great Barred Spiral" to "مارپیچی میله‌ای بزرگ",
        "Fornax A" to "کوره ای",
        "Fireworks Galaxy" to "کهکشان آتش‌بازی",
        "Barnard's Galaxy" to "کهکشان بارنارد",
        "Pacman Nebula" to "سحابی پک‌من",
        "California Nebula" to "سحابی کالیفرنیا",
        "Running Man Nebula" to "سحابی مرد دونده",
        "Hubble's Variable Nebula" to "سحابی متغیر هابل",
        "Thor's Helmet" to "کلاه‌خود ثور",
        "Ghost of Jupiter" to "شبح مشتری",
        "Butterfly Nebula" to "سحابی پروانه",
        "Blinking Planetary" to "سحابی سیاره‌نمای چشمک‌زن",
        "Bow-Tie Nebula" to "سحابی پاپیون",
        "Skull Nebula" to "سحابی جمجمه",
        "Cleopatra's Eye" to "چشم کلئوپاترا",
        "Blue Planetary" to "سحابی سیاره‌نمای آبی",
        "Spiral Planetary" to "سحابی سیاره‌نمای مارپیچی",
        "Little Ghost" to "شبح کوچک",
        "Phantom Streak" to "رد شبح",
        "Little Gem" to "گوهر کوچک",
        "Blue Flash" to "درخشش آبی",
        "Fetus Nebula" to "سحابی جنین",
        "Caroline's Rose" to "گل رز کارولین",
        "Owl Cluster" to "خوشه جغد",
        "37 Cluster" to "خوشه ۳۷",
        "Caroline's Cluster" to "خوشه کارولین",
        "Tau Canis Majoris Cluster" to "خوشه تاو سگ بزرگ",
        "Iris Nebula" to "سحابی زنبق",
        "Bubble Nebula" to "سحابی حباب",
        "Cocoon Nebula" to "سحابی پیله",
        "Perseus A" to "برساوش ای",
        "Intergalactic Wanderer" to "سرگردان میان‌کهکشانی",
        "Crescent Nebula" to "سحابی هلال"
    )

    private val translatedWords = mapOf(
        "galaxy" to "کهکشان",
        "nebula" to "سحابی",
        "cluster" to "خوشه",
        "planetary" to "سیاره‌نما",
        "open" to "باز",
        "globular" to "کروی",
        "spiral" to "مارپیچی",
        "elliptical" to "بیضوی",
        "dwarf" to "کوتوله",
        "star" to "ستاره‌ای",
        "cloud" to "ابر",
        "west" to "غربی",
        "east" to "شرقی",
        "north" to "شمالی",
        "south" to "جنوبی",
        "northern" to "شمالی",
        "southern" to "جنوبی",
        "great" to "بزرگ",
        "little" to "کوچک",
        "blue" to "آبی",
        "red" to "سرخ",
        "black" to "سیاه",
        "eye" to "چشم",
        "eyes" to "چشم‌ها"
    )

    private fun transliterateLatinPhrase(value: String): String {
        return value
            .replace("'s", "")
            .replace("(", " ")
            .replace(")", " ")
            .replace("-", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> translatedWords[word.lowercase()] ?: transliterateLatinWord(word) }
    }

    private fun transliterateLatinWord(word: String): String {
        val lower = word.lowercase()
        val special = mapOf(
            "a" to "ا", "b" to "ب", "c" to "ک", "d" to "د", "e" to "ه", "f" to "ف", "g" to "گ",
            "h" to "ه", "i" to "ی", "j" to "ج", "k" to "ک", "l" to "ل", "m" to "م", "n" to "ن",
            "o" to "و", "p" to "پ", "q" to "ک", "r" to "ر", "s" to "س", "t" to "ت", "u" to "و",
            "v" to "و", "w" to "و", "x" to "کس", "y" to "ی", "z" to "ز"
        )
        return lower.map { ch -> special[ch.toString()] ?: ch.toString() }.joinToString("")
    }

    private val factsByCanonicalId = mapOf(
        "dso_m31_andromeda" to BilingualFacts(
            en = listOf(
                "Cepheid variables in Andromeda were decisive evidence that spiral nebulae are separate galaxies far beyond the Milky Way.",
                "Its two bright companions, M32 and M110, can be seen in the same wide-field view and are gravitationally bound satellites.",
                "Hubble mosaics show dust lanes, blue star-forming rings, and more than one hundred million resolved stars in part of its disk.",
                "Andromeda and the Milky Way are approaching each other and are expected to merge into a larger galaxy in several billion years.",
                "The 1885 outburst S Andromedae remains the only supernova recorded in Andromeda during the telescopic era."
            ),
            fa = listOf(
                "متغیرهای قیفاووسی آندرومدا شاهد کلیدی بودند که نشان دادند سحابی‌های مارپیچی کهکشان‌هایی مستقل و بسیار دورتر از راه شیری‌اند.",
                "دو همدم پرنور آن، M32 و M110، در نمای میدان‌گسترده کنار آندرومدا دیده می‌شوند و ماهواره‌های گرانشی آن هستند.",
                "موزاییک‌های هابل نوارهای غبار، حلقه‌های آبی ستاره‌زا و بیش از صد میلیون ستاره تفکیک‌شده را در بخشی از قرص آن نشان داده‌اند.",
                "آندرومدا و راه شیری به یکدیگر نزدیک می‌شوند و در چند میلیارد سال آینده در یک کهکشان بزرگ‌تر ادغام خواهند شد.",
                "فوران S Andromedae در سال ۱۸۸۵ تنها ابرنواختر ثبت‌شده آندرومدا در عصر تلسکوپی است."
            )
        ),
        "dso_lmc" to BilingualFacts(
            en = listOf(
                "The Large Magellanic Cloud hosts the Tarantula Nebula, one of the most vigorous star-forming regions in the Local Group.",
                "Supernova 1987A exploded in this galaxy, giving astronomers the closest well-observed supernova of the modern era.",
                "Gas stripped from the Magellanic Clouds forms the Magellanic Stream, a long ribbon trailing around the Milky Way.",
                "Its off-center bar and one-armed structure record repeated tidal encounters with the Small Magellanic Cloud and the Milky Way.",
                "Because its stars have lower heavy-element abundance than the Milky Way, it is a nearby laboratory for metal-poor stellar evolution."
            ),
            fa = listOf(
                "ابر ماژلانی بزرگ میزبان سحابی رتیل است؛ یکی از نیرومندترین نواحی ستاره‌زایی در گروه محلی.",
                "ابرنواختر ۱۹۸۷A در همین کهکشان رخ داد و نزدیک‌ترین ابرنواختر به‌خوبی رصدشده عصر جدید را فراهم کرد.",
                "گاز کنده‌شده از ابرهای ماژلانی، جریان ماژلانی را می‌سازد؛ نواری بلند که پیرامون راه شیری کشیده شده است.",
                "میله نامرکزی و ساختار یک‌بازویی آن اثر برخوردهای کشندی تکرارشونده با ابر ماژلانی کوچک و راه شیری است.",
                "به دلیل فراوانی کمتر عناصر سنگین نسبت به راه شیری، آزمایشگاهی نزدیک برای بررسی تحول ستارگان کم‌فلز است."
            )
        ),
        "dso_smc" to BilingualFacts(
            en = listOf(
                "The Small Magellanic Cloud is linked to the Large Magellanic Cloud by a bridge of gas, young stars, and tidal debris.",
                "Its low metal abundance makes it a useful local stand-in for primitive galaxies in the early universe.",
                "Many Cepheid variables in the SMC helped refine the period-luminosity relation used for cosmic distance measurements.",
                "The nearby-looking globular cluster 47 Tucanae is only a foreground Milky Way object, not part of the SMC.",
                "Tidal stirring has left the SMC stretched along the line of sight rather than shaped like a calm disk."
            ),
            fa = listOf(
                "ابر ماژلانی کوچک با پلی از گاز، ستارگان جوان و آوار کشندی به ابر ماژلانی بزرگ پیوند دارد.",
                "فراوانی کم عناصر سنگین در آن، این کهکشان را نمونه‌ای نزدیک برای کهکشان‌های ابتدایی جهان آغازین می‌کند.",
                "متغیرهای قیفاووسی فراوان در SMC به دقیق‌تر شدن رابطه دوره-درخشندگی برای اندازه‌گیری فاصله‌های کیهانی کمک کرده‌اند.",
                "خوشه کروی ۴۷ توکانا که نزدیک به نظر می‌آید، جرم پیش‌زمینه راه شیری است و عضو SMC نیست.",
                "آشفتگی کشندی باعث شده SMC در راستای دید کشیده‌تر از یک قرص آرام کهکشانی باشد."
            )
        ),
        "dso_m33_triangulum" to BilingualFacts(
            en = listOf(
                "M33 contains NGC 604, a giant star-forming complex far larger than the Orion Nebula.",
                "Its open spiral arms show little sign of a classical central bulge, making it a useful contrast to the Milky Way.",
                "Water maser and stellar measurements have made M33 an important benchmark for Local Group distance work.",
                "The galaxy may be a long-term companion of Andromeda and could take part in the future Local Group merger.",
                "Deep imaging reveals many blue clusters and reddish nebulae scattered through its flocculent arms."
            ),
            fa = listOf(
                "M33 شامل NGC 604 است؛ مجتمع عظیم ستاره‌زایی که بسیار بزرگ‌تر از سحابی جبار است.",
                "بازوهای مارپیچی باز آن نشانه کمی از برآمدگی مرکزی کلاسیک دارند و برای مقایسه با راه شیری ارزشمندند.",
                "اندازه‌گیری‌های میزر آب و ستارگان، M33 را به معیار مهمی برای کارهای فاصله‌سنجی گروه محلی تبدیل کرده‌اند.",
                "این کهکشان احتمالاً همدم دیرینه آندرومداست و شاید در ادغام آینده گروه محلی نقش داشته باشد.",
                "تصویربرداری ژرف، خوشه‌های آبی و سحابی‌های سرخ فراوانی را در بازوهای پرزدار آن نشان می‌دهد."
            )
        ),
        "dso_m45_pleiades" to BilingualFacts(
            en = listOf(
                "The blue haze around the Pleiades is reflected starlight from passing interstellar dust, not leftover birth material.",
                "Its brightest members are hot B-type stars only about a hundred million years old.",
                "Brown dwarfs discovered in the cluster helped test the low-mass end of star formation.",
                "Many cultures count the visible stars differently, so the Seven Sisters often appear as six, seven, or more stars to keen eyes.",
                "The cluster is drifting through a dusty region of Taurus, which is why long exposures reveal delicate reflection nebulosity."
            ),
            fa = listOf(
                "مه آبی پیرامون پروین، نور بازتابیده از غبار میان‌ستاره‌ای گذراست و ماده باقی‌مانده زایش خوشه نیست.",
                "درخشان‌ترین اعضای آن ستارگان داغ نوع B هستند که تنها حدود صد میلیون سال سن دارند.",
                "کوتوله‌های قهوه‌ای کشف‌شده در این خوشه به آزمودن مرز کم‌جرم فرایند ستاره‌زایی کمک کرده‌اند.",
                "فرهنگ‌های گوناگون تعداد ستارگان پیدای آن را متفاوت شمرده‌اند؛ از شش و هفت تا تعداد بیشتر برای چشم‌های تیزبین.",
                "این خوشه در حال گذر از ناحیه‌ای غبارآلود در گاو است، ازاین‌رو نوردهی‌های بلند سحابی بازتابی ظریفی را آشکار می‌کند."
            )
        ),
        "dso_hyades" to BilingualFacts(
            en = listOf(
                "Aldebaran only appears inside the Hyades V; it is a foreground star and not a true cluster member.",
                "Hyades stars share a common space motion, enabling the classic moving-cluster method for distance calibration.",
                "Gaia data reveal long tidal tails of former Hyades members pulled away by the Milky Way's gravity.",
                "Its age, roughly several hundred million years, makes it a key benchmark for testing stellar evolution models.",
                "The cluster's nearness makes it one of the best places to study low-mass stars and white dwarfs in a single population."
            ),
            fa = listOf(
                "دبران فقط درون شکل V قلائص دیده می‌شود؛ ستاره‌ای پیش‌زمینه است و عضو واقعی خوشه نیست.",
                "ستارگان قلائص حرکت فضایی مشترک دارند و همین پایه روش کلاسیک «خوشه متحرک» برای واسنجی فاصله‌هاست.",
                "داده‌های گایا دنباله‌های کشندی بلندی از اعضای پیشین قلائص را نشان می‌دهد که گرانش راه شیری از خوشه بیرون کشیده است.",
                "سن چند صد میلیون ساله آن، خوشه را به معیار مهمی برای آزمودن مدل‌های تحول ستاره‌ای تبدیل می‌کند.",
                "نزدیکی این خوشه آن را به یکی از بهترین میدان‌ها برای بررسی ستارگان کم‌جرم و کوتوله‌های سفید در یک جمعیت واحد بدل کرده است."
            )
        ),
        "dso_m44_beehive" to BilingualFacts(
            en = listOf(
                "Galileo resolved the misty Praesepe patch into dozens of stars with his early telescope.",
                "Its age and motion are close to the Hyades, suggesting both clusters may share a related origin.",
                "The Beehive hosts white dwarfs and low-mass members used to test cluster aging and stellar remnants.",
                "Modern surveys have found planets around stars in M44, showing planetary systems can survive in open clusters.",
                "Ancient skywatchers used the disappearance of this naked-eye cloud as a weather omen."
            ),
            fa = listOf(
                "گالیله لکه مه‌آلود پرسپ را با تلسکوپ آغازین خود به ده‌ها ستاره تفکیک کرد.",
                "سن و حرکت این خوشه به قلائص نزدیک است و نشان می‌دهد شاید هر دو منشأ مرتبطی داشته باشند.",
                "کندوی عسل دارای کوتوله‌های سفید و اعضای کم‌جرمی است که برای آزمودن پیری خوشه و بقایای ستاره‌ای به کار می‌روند.",
                "پیمایش‌های جدید سیاره‌هایی پیرامون ستارگان M44 یافته‌اند و نشان داده‌اند سامانه‌های سیاره‌ای در خوشه‌های باز هم می‌توانند پایدار بمانند.",
                "رصدگران باستان ناپدید شدن این ابر کم‌نور با چشم غیرمسلح را نشانه بدی هوا می‌دانستند."
            )
        ),
        "dso_ngc_869" to BilingualFacts(
            en = listOf(
                "NGC 869 and NGC 884 are twin young open clusters in the Perseus arm, not a single merged cluster.",
                "Their brilliant blue-white stars are accompanied by evolved red supergiants, a signature of very young massive populations.",
                "The pair is embedded in the Perseus OB1 association, tying it to a large-scale star-forming environment.",
                "The Double Cluster was known in antiquity as a misty patch before telescopes split it into two rich star swarms.",
                "Their side-by-side placement makes the pair a classic low-power telescope target rather than a high-magnification object."
            ),
            fa = listOf(
                "NGC 869 و NGC 884 دو خوشه باز جوان دوقلو در بازوی برساوش‌اند، نه یک خوشه واحد ادغام‌شده.",
                "ستارگان آبی-سفید درخشان آن‌ها همراه با ابرغول‌های سرخ تکامل‌یافته دیده می‌شوند؛ نشانه جمعیت‌های جوان و پرجرم.",
                "این جفت در انجمن ستاره‌ای برساوش OB1 جای دارد و به محیطی بزرگ‌مقیاس از ستاره‌زایی پیوند می‌خورد.",
                "خوشه دوگانه از روزگار باستان به صورت لکه‌ای مه‌آلود شناخته می‌شد تا اینکه تلسکوپ‌ها آن را به دو ازدحام ستاره‌ای غنی تفکیک کردند.",
                "قرارگیری کنار هم باعث شده این جفت هدف کلاسیک تلسکوپ‌های کم‌بزرگنمایی باشد، نه بزرگنمایی‌های زیاد."
            )
        ),
        "dso_ngc_884" to BilingualFacts(
            en = listOf(
                "NGC 884 is the eastern half of the Double Cluster and is physically distinct from NGC 869.",
                "Its young massive stars trace the Perseus spiral arm and the Perseus OB1 association.",
                "The cluster contains evolved supergiants whose colors contrast strongly with its hot blue main-sequence stars."
            ),
            fa = listOf(
                "NGC 884 نیمه شرقی خوشه دوگانه است و از نظر فیزیکی از NGC 869 جداست.",
                "ستارگان جوان و پرجرم آن بازوی مارپیچی برساوش و انجمن برساوش OB1 را ردیابی می‌کنند.",
                "این خوشه ابرغول‌های تکامل‌یافته‌ای دارد که رنگشان با ستارگان داغ و آبی رشته اصلی تضاد چشمگیری می‌سازد."
            )
        ),
        "dso_coma_cluster" to BilingualFacts(
            en = listOf(
                "The Coma Star Cluster is a nearby moving group, not the distant Coma Cluster of galaxies.",
                "Its loose members form much of the traditional hair of Berenice visible to the unaided eye.",
                "Gaia astrometry has revealed extended tidal tails that show how the Milky Way is dissolving the cluster.",
                "The cluster's broad spread makes binoculars or unaided-eye viewing more useful than high magnification.",
                "Its intermediate age helps bridge studies between very young open clusters and older loose associations."
            ),
            fa = listOf(
                "خوشه گیسو یک گروه متحرک نزدیک است، نه خوشه بسیار دور کهکشانی گیسو.",
                "اعضای پراکنده آن بخش زیادی از گیسوی سنتی برنیکه را تشکیل می‌دهند که با چشم غیرمسلح دیده می‌شود.",
                "اخترسنجی گایا دنباله‌های کشندی گسترده‌ای را آشکار کرده که نشان می‌دهد راه شیری این خوشه را آرام‌آرام می‌گسلد.",
                "پراکندگی زاویه‌ای بزرگ آن باعث می‌شود دوربین دوچشمی یا چشم غیرمسلح از بزرگنمایی زیاد مناسب‌تر باشد.",
                "سن میانی آن پلی میان مطالعه خوشه‌های باز بسیار جوان و انجمن‌های پراکنده پیرتر فراهم می‌کند."
            )
        ),
        "dso_omega_centauri" to BilingualFacts(
            en = listOf(
                "Omega Centauri contains multiple stellar populations with different chemical abundances, unlike most ordinary globular clusters.",
                "Its great mass and chemical spread suggest it may be the stripped nucleus of a former dwarf galaxy.",
                "Ptolemy cataloged it as a star, and Edmond Halley later recognized it as a nebulous object.",
                "Astronomers continue to test whether its core hides an intermediate-mass black hole.",
                "Under dark southern skies it is visible to the unaided eye as a fuzzy star-like patch."
            ),
            fa = listOf(
                "امگا قنطورس برخلاف بیشتر خوشه‌های کروی عادی، چند جمعیت ستاره‌ای با فراوانی‌های شیمیایی متفاوت دارد.",
                "جرم زیاد و پراکندگی شیمیایی آن نشان می‌دهد شاید هسته برهنه یک کهکشان کوتوله پیشین باشد.",
                "بطلمیوس آن را ستاره ثبت کرده بود و ادموند هالی بعدها ماهیت مه‌آلودش را تشخیص داد.",
                "اخترشناسان هنوز بررسی می‌کنند که آیا هسته آن سیاه‌چاله‌ای با جرم میانی پنهان دارد یا نه.",
                "در آسمان تاریک جنوبی با چشم غیرمسلح مانند لکه‌ای ستاره‌مانند و مه‌آلود دیده می‌شود."
            )
        ),
        "dso_47_tucanae" to BilingualFacts(
            en = listOf(
                "47 Tucanae is a foreground Milky Way globular cluster that only appears projected beside the Small Magellanic Cloud.",
                "It contains an unusually rich population of millisecond pulsars spun up by past binary interactions.",
                "Hubble studies of its crowded core reveal blue stragglers formed through stellar mergers or mass transfer.",
                "The cluster is metal-rich compared with many halo globulars, giving it a distinct color and evolutionary mix.",
                "Its dense core is a natural laboratory for close stellar encounters in old star systems."
            ),
            fa = listOf(
                "۴۷ توکانا یک خوشه کروی پیش‌زمینه در راه شیری است و فقط در کنار ابر ماژلانی کوچک تصویر می‌شود.",
                "این خوشه جمعیتی بسیار غنی از تپ‌اخترهای میلی‌ثانیه‌ای دارد که در برهم‌کنش‌های دوتایی گذشته تندچرخ شده‌اند.",
                "مطالعات هابل از هسته شلوغ آن سرگردان‌های آبی را نشان می‌دهد که از ادغام ستاره‌ای یا انتقال جرم ساخته شده‌اند.",
                "این خوشه نسبت به بسیاری از خوشه‌های هاله‌ای فلزدارتر است و رنگ و ترکیب تکاملی متفاوتی دارد.",
                "هسته متراکم آن آزمایشگاهی طبیعی برای برخوردهای نزدیک ستارگان در سامانه‌های پیر است."
            )
        ),
        "dso_m13_hercules" to BilingualFacts(
            en = listOf(
                "The 1974 Arecibo message was aimed toward M13 as a demonstration of radio technology, though the cluster will move before it arrives.",
                "Edmond Halley recorded M13 in 1714, and Charles Messier later added it to his catalog of comet-like objects.",
                "Large amateur telescopes can resolve its outskirts into hundreds of tiny points while the core remains densely granular.",
                "The cluster's old low-metal stars trace the ancient halo of the Milky Way.",
                "M13 contains unusual blue stragglers that appear younger than the main old cluster population."
            ),
            fa = listOf(
                "پیام آرسیبو در سال ۱۹۷۴ به سوی M13 نشانه رفت تا توان فناوری رادیویی را نشان دهد؛ هرچند خوشه تا رسیدن پیام جابه‌جا خواهد شد.",
                "ادموند هالی M13 را در ۱۷۱۴ ثبت کرد و شارل مسیه بعدها آن را به فهرست اجرام شبیه دنباله‌دار خود افزود.",
                "تلسکوپ‌های آماتوری بزرگ حاشیه‌های آن را به صدها نقطه ریز تفکیک می‌کنند، در حالی که هسته همچنان دانه‌دانه و بسیار فشرده می‌ماند.",
                "ستارگان پیر و کم‌فلز این خوشه ردپای هاله کهن راه شیری را نشان می‌دهند.",
                "M13 دارای سرگردان‌های آبی نامعمولی است که جوان‌تر از جمعیت پیر اصلی خوشه به نظر می‌رسند."
            )
        ),
        "dso_m42_orion_nebula" to BilingualFacts(
            en = listOf(
                "The Trapezium Cluster supplies much of the ultraviolet light that ionizes the visible Orion Nebula.",
                "Hubble images revealed many protoplanetary disks, or proplyds, being sculpted by intense radiation in the nebula.",
                "The bright nebula is only the illuminated surface of a much larger Orion Molecular Cloud complex.",
                "Henry Draper photographed the Orion Nebula in 1880, one of the first successful deep-sky photographs.",
                "Jets and bow shocks around newborn stars show that star formation is still active inside the nebula."
            ),
            fa = listOf(
                "خوشه ذوزنقه بخش بزرگی از تابش فرابنفشی را فراهم می‌کند که سحابی جبار مرئی را یونیده نگه می‌دارد.",
                "تصاویر هابل قرص‌های پیش‌سیاره‌ای فراوانی، یا پروپلیدها، را نشان داد که تابش شدید درون سحابی آن‌ها را می‌تراشد.",
                "سحابی روشن فقط سطح نورانی مجموعه بسیار بزرگ‌تر ابر مولکولی جبار است.",
                "هنری دراپر در سال ۱۸۸۰ از سحابی جبار عکس گرفت؛ یکی از نخستین عکس‌های موفق از اجرام اعماق آسمان.",
                "جت‌ها و شوک‌های کمانی پیرامون ستارگان نوزاد نشان می‌دهند ستاره‌زایی هنوز درون سحابی فعال است."
            )
        ),
        "dso_m8_lagoon" to BilingualFacts(
            en = listOf(
                "The dark lane that cuts through M8 gives the Lagoon Nebula its visual lagoon-like shape.",
                "The embedded cluster NGC 6530 contains young stars that help illuminate and shape the surrounding gas.",
                "The Hourglass region near Herschel 36 is one of the nebula's most intense pockets of massive star formation.",
                "Infrared observations reveal newborn stars still hidden inside dust that visible light cannot penetrate.",
                "Its mixture of emission gas, dust lanes, and young cluster stars makes M8 a textbook example of active star birth."
            ),
            fa = listOf(
                "نوار تاریکی که M8 را قطع می‌کند، شکل مرداب‌مانند مشهور سحابی مرداب را می‌سازد.",
                "خوشه درون‌نشسته NGC 6530 شامل ستارگان جوانی است که گاز پیرامون را روشن و شکل‌دهی می‌کنند.",
                "ناحیه ساعت‌شنی نزدیک هرشل ۳۶ یکی از شدیدترین کانون‌های زایش ستارگان پرجرم در این سحابی است.",
                "رصدهای فروسرخ ستارگان نوزادی را آشکار می‌کنند که هنوز در غباری پنهان‌اند که نور مرئی از آن نمی‌گذرد.",
                "آمیختگی گاز تابشی، نوارهای غبار و ستارگان خوشه‌ای جوان، M8 را نمونه‌ای آموزشی از زایش فعال ستاره‌ها می‌کند."
            )
        ),
        "dso_eta_carinae_nebula" to BilingualFacts(
            en = listOf(
                "Eta Carinae's nineteenth-century Great Eruption produced the bipolar Homunculus Nebula inside the larger Carina complex.",
                "The Carina Nebula contains many O-type stars and several clusters, making it one of the Milky Way's most massive nurseries.",
                "Its Keyhole Nebula is a dark, dusty feature silhouetted against bright emission gas.",
                "Modern Hubble and Webb images resolve pillars, jets, and irradiated cloud edges where massive stars reshape their birthplace.",
                "Because it lies in the southern Milky Way, the nebula is a showcase object for southern-hemisphere observers."
            ),
            fa = listOf(
                "فوران بزرگ اتا شاه‌تخته در سده نوزدهم، سحابی دوقطبی آدمک را درون مجموعه بزرگ‌تر شاه‌تخته پدید آورد.",
                "سحابی شاه‌تخته ستارگان نوع O و چند خوشه دارد و یکی از پرجرم‌ترین زایشگاه‌های راه شیری است.",
                "سحابی سوراخ‌کلید در آن، ساختاری تاریک و غبارآلود است که در برابر گاز تابشی روشن سایه انداخته است.",
                "تصاویر نوین هابل و وب ستون‌ها، جت‌ها و لبه‌های ابرهای تابش‌خورده‌ای را تفکیک می‌کنند که ستارگان پرجرم زادگاه خود را دگرگون می‌سازند.",
                "چون در راه شیری جنوبی جای دارد، این سحابی یکی از شاخص‌ترین اهداف رصدگران نیمکره جنوبی است."
            )
        ),
        "dso_ngc_147" to BilingualFacts(
            en = listOf(
                "NGC 147 is a dwarf spheroidal companion of Andromeda and belongs to the Local Group.",
                "Its old, gas-poor stellar population contrasts with the more visibly dusty neighboring dwarf NGC 185.",
                "The pair NGC 147 and NGC 185 probably share an orbital history around Andromeda."
            ),
            fa = listOf(
                "NGC 147 همدم کوتوله کروی‌گون آندرومدا و عضو گروه محلی است.",
                "جمعیت ستاره‌ای پیر و کم‌گاز آن با کوتوله همسایه، NGC 185، که غبار آشکارتری دارد، تفاوت دارد.",
                "جفت NGC 147 و NGC 185 احتمالاً تاریخچه مداری مشترکی پیرامون آندرومدا دارند."
            )
        ),
        "dso_ngc_185" to BilingualFacts(
            en = listOf(
                "NGC 185 is an Andromeda satellite with dust and recent star-formation traces unusual for a dwarf spheroidal galaxy.",
                "It forms a physical dwarf-galaxy pair with NGC 147 in the Local Group.",
                "Its central region contains gas, dust patches, and young stars that mark a more complex history than a purely old spheroidal."
            ),
            fa = listOf(
                "NGC 185 ماهواره آندرومداست و در آن غبار و نشانه‌های ستاره‌زایی نسبتاً تازه دیده می‌شود؛ ویژگی‌ای نامعمول برای کوتوله‌های کروی‌گون.",
                "این جرم با NGC 147 یک جفت فیزیکی از کهکشان‌های کوتوله در گروه محلی می‌سازد.",
                "ناحیه مرکزی آن دارای گاز، لکه‌های غبار و ستارگان جوان است و تاریخچه‌ای پیچیده‌تر از یک کروی‌گون کاملاً پیر را نشان می‌دهد."
            )
        ),
        "dso_ngc_2403" to BilingualFacts(
            en = listOf(
                "NGC 2403 is a nearby M81-group spiral with many pink H II regions scattered through its arms.",
                "Cepheid measurements in this galaxy helped extend the extragalactic distance scale beyond the Local Group.",
                "Supernova 2004dj made NGC 2403 a well-studied nearby host of a core-collapse explosion."
            ),
            fa = listOf(
                "NGC 2403 مارپیچی نزدیکی در گروه M81 است و نواحی صورتی H II فراوانی در بازوهایش دیده می‌شود.",
                "اندازه‌گیری قیفاووسی‌ها در این کهکشان به گسترش نردبان فاصله‌های فراکهکشانی فراتر از گروه محلی کمک کرد.",
                "ابرنواختر ۲۰۰۴dj باعث شد NGC 2403 میزبان نزدیک و به‌خوبی مطالعه‌شده‌ای برای انفجار فروپاشی هسته‌ای باشد."
            )
        ),
        "dso_ngc_40" to BilingualFacts(
            en = listOf(
                "NGC 40's bow-tie appearance comes from glowing gas ejected by a dying central star.",
                "The hot central star has Wolf-Rayet-like features that drive a strong wind into the older nebular shell.",
                "Multiple shells and knots show that the star lost mass in more than one episode."
            ),
            fa = listOf(
                "ظاهر پاپیونی NGC 40 از گاز درخشانی پدید آمده که ستاره مرکزی رو به مرگ بیرون افکنده است.",
                "ستاره مرکزی داغ ویژگی‌هایی شبیه ولف-رایه دارد و باد نیرومندی را به پوسته قدیمی‌تر سحابی می‌کوبد.",
                "پوسته‌ها و گره‌های چندگانه نشان می‌دهند ستاره در بیش از یک دوره جرم از دست داده است."
            )
        ),
        "dso_ngc_4244" to BilingualFacts(
            en = listOf(
                "NGC 4244 is an almost edge-on late-type spiral in the Canes Venatici cloud of galaxies.",
                "Its thin disk and small central bulge make it a clean example for studying disk structure.",
                "Neutral-hydrogen observations show gas extending beyond the bright optical disk."
            ),
            fa = listOf(
                "NGC 4244 مارپیچی دیرگونه و تقریباً لبه‌نما در ابر کهکشانی سگ‌های شکاری است.",
                "قرص باریک و برآمدگی مرکزی کوچک آن، نمونه‌ای روشن برای مطالعه ساختار قرص کهکشانی فراهم می‌کند.",
                "رصدهای هیدروژن خنثی نشان می‌دهند گاز فراتر از قرص نوری روشن آن امتداد دارد."
            )
        ),
        "dso_ngc_4449" to BilingualFacts(
            en = listOf(
                "NGC 4449 is a Magellanic irregular starburst galaxy with widespread current star formation.",
                "A faint stellar stream around it is evidence that the dwarf galaxy recently accreted a smaller companion.",
                "Its many giant H II regions make it a nearby laboratory for feedback from massive young stars."
            ),
            fa = listOf(
                "NGC 4449 کهکشان نامنظم ماژلانی و ستاره‌فشان است و ستاره‌زایی کنونی گسترده‌ای دارد.",
                "جریان ستاره‌ای کم‌نوری پیرامون آن نشان می‌دهد این کهکشان کوتوله اخیراً همدم کوچک‌تری را بلعیده است.",
                "نواحی غول‌آسای H II فراوان، آن را آزمایشگاهی نزدیک برای بررسی بازخورد ستارگان جوان پرجرم می‌کند."
            )
        ),
        "dso_ngc_457" to BilingualFacts(
            en = listOf(
                "The Owl Cluster's two bright eye stars frame a humanoid or owl-shaped pattern for visual observers.",
                "The brilliant star Phi Cassiopeiae lies in the same line of sight and helps create the cluster's familiar outline.",
                "Its young blue-white members make the cluster stand out against the rich Cassiopeia Milky Way field."
            ),
            fa = listOf(
                "دو ستاره پرنورِ چشم‌های خوشه جغد، طرحی شبیه جغد یا آدمک برای رصدگران می‌سازند.",
                "ستاره درخشان فی ذات‌الکرسی در همان راستای دید قرار دارد و به شکل آشنای خوشه کمک می‌کند.",
                "اعضای جوان آبی-سفید آن در برابر میدان پرستاره راه شیری در ذات‌الکرسی برجسته می‌شوند."
            )
        ),
        "dso_ngc_6543" to BilingualFacts(
            en = listOf(
                "Hubble images of the Cat's Eye show nested shells, knots, jets, and shock fronts from episodic mass loss.",
                "Its central star is rapidly evolving toward a white dwarf while ultraviolet light makes the expelled gas glow.",
                "A much larger faint halo surrounds the bright core, recording older material lost before the compact nebula formed."
            ),
            fa = listOf(
                "تصاویر هابل از چشم گربه پوسته‌های تودرتو، گره‌ها، جت‌ها و جبهه‌های شوک ناشی از جرم‌ریزی دوره‌ای را نشان می‌دهد.",
                "ستاره مرکزی آن به سرعت به سوی کوتوله سفید شدن می‌رود و تابش فرابنفش، گاز بیرون‌ریخته را درخشان می‌کند.",
                "هاله‌ای بسیار کم‌نور و بزرگ‌تر دور هسته روشن قرار دارد که ماده از‌دست‌رفته قدیمی‌تر را ثبت کرده است."
            )
        ),
        "dso_ngc_663" to BilingualFacts(
            en = listOf(
                "NGC 663 is a young Cassiopeia open cluster rich in rapidly rotating Be stars.",
                "Its position in a crowded Milky Way field makes it useful for tracing recent star formation in the Perseus arm.",
                "Several of its massive stars have already evolved away from the simplest main-sequence pattern."
            ),
            fa = listOf(
                "NGC 663 خوشه باز جوانی در ذات‌الکرسی است که ستارگان Be تندچرخ فراوانی دارد.",
                "جایگاه آن در میدان شلوغ راه شیری برای ردیابی ستاره‌زایی تازه در بازوی برساوش سودمند است.",
                "چند ستاره پرجرم آن از الگوی ساده رشته اصلی فاصله گرفته و تکامل یافته‌اند."
            )
        ),
        "dso_ngc_6826" to BilingualFacts(
            en = listOf(
                "The Blinking Planetary seems to blink because direct vision emphasizes its bright central star while averted vision reveals the nebula.",
                "Its compact inner shell is surrounded by fainter outer gas from earlier mass-loss stages.",
                "The hot central star is ionizing material it expelled when it left the red-giant phase."
            ),
            fa = listOf(
                "سیاره‌نمای چشمک‌زن به این دلیل چشمک‌زن به نظر می‌آید که نگاه مستقیم ستاره مرکزی را برجسته می‌کند و نگاه کناری سحابی را آشکارتر می‌سازد.",
                "پوسته درونی فشرده آن با گاز بیرونی کم‌نورتر از دوره‌های قدیمی‌تر جرم‌ریزی احاطه شده است.",
                "ستاره مرکزی داغ، موادی را یونیده می‌کند که هنگام ترک مرحله غول سرخ بیرون افکنده بود."
            )
        ),
        "dso_ngc_6946" to BilingualFacts(
            en = listOf(
                "NGC 6946 earned its Fireworks nickname because observers have recorded an unusually large number of supernovae in it.",
                "It is a face-on spiral seen through foreground Milky Way dust, which dims and reddens its light.",
                "Its many star-forming regions make it a key nearby galaxy for studying massive-star birth and death."
            ),
            fa = listOf(
                "NGC 6946 لقب آتش‌بازی را به سبب شمار نامعمول ابرنواخترهای ثبت‌شده در آن گرفته است.",
                "این کهکشان مارپیچی رو‌به‌رو از پشت غبار پیش‌زمینه راه شیری دیده می‌شود و همین نور آن را کم‌نور و سرخ‌تر می‌کند.",
                "نواحی ستاره‌زایی فراوانش آن را کهکشانی نزدیک و کلیدی برای مطالعه زایش و مرگ ستارگان پرجرم کرده است."
            )
        ),
        "dso_ngc_7000" to BilingualFacts(
            en = listOf(
                "The North America outline is carved mostly by dark foreground dust, not by a hard edge in the glowing gas.",
                "Its brightest ionizing star is heavily obscured, so infrared work was needed to identify the main power source.",
                "Together with the Pelican Nebula, it forms part of a larger star-forming complex in Cygnus."
            ),
            fa = listOf(
                "طرح آمریکای شمالی بیشتر به وسیله غبار تاریک پیش‌زمینه تراشیده شده است، نه مرزی سخت در گاز درخشان.",
                "درخشان‌ترین ستاره یوننده آن به شدت پنهان است و برای شناسایی منبع اصلی انرژی به رصد فروسرخ نیاز بود.",
                "این سحابی همراه با سحابی پلیکان بخشی از مجموعه بزرگ‌تر ستاره‌زایی در ماکیان را می‌سازد."
            )
        ),
        "dso_ngc_7243" to BilingualFacts(
            en = listOf(
                "NGC 7243 is a loose young open cluster in the small northern constellation Lacerta.",
                "Its bright blue-white stars make it easier to recognize than many sparse clusters in rich Milky Way fields.",
                "The cluster's youth means its most massive members have not yet dispersed far from their birth environment."
            ),
            fa = listOf(
                "NGC 7243 خوشه باز جوان و پراکنده‌ای در صورت فلکی کوچک سوسمار است.",
                "ستارگان آبی-سفید پرنور آن باعث می‌شوند در میدان‌های پرستاره راه شیری آسان‌تر از بسیاری خوشه‌های پراکنده شناخته شود.",
                "جوانی خوشه یعنی اعضای پرجرم‌تر آن هنوز از محیط زایش خود چندان دور نشده‌اند."
            )
        ),
        "dso_ngc_7331" to BilingualFacts(
            en = listOf(
                "NGC 7331 is often used as a Milky Way analogue because of its large spiral disk and prominent central bulge.",
                "The nearby-looking Deer Lick group galaxies are much farther background galaxies, not satellites of NGC 7331.",
                "Kinematic studies show complex motion in its central regions, making it more than a simple textbook spiral."
            ),
            fa = listOf(
                "NGC 7331 به دلیل قرص مارپیچی بزرگ و برآمدگی مرکزی آشکار، اغلب همانند راه شیری در نظر گرفته می‌شود.",
                "کهکشان‌های گروه موسوم به گوزن‌لیس که نزدیک به نظر می‌آیند، در واقع پس‌زمینه‌ای بسیار دورترند و ماهواره‌های NGC 7331 نیستند.",
                "مطالعات جنبشی، حرکت‌های پیچیده‌ای را در نواحی مرکزی آن نشان می‌دهد؛ بنابراین بیش از یک مارپیچی ساده کتابی است."
            )
        ),
        "dso_ngc_752" to BilingualFacts(
            en = listOf(
                "NGC 752 is an old open cluster, making it valuable for studying how loosely bound clusters survive in the Galactic disk.",
                "Its evolved red giants and main-sequence turnoff help constrain the cluster's age.",
                "Because it is spread over a wide area, binoculars show its structure better than high-power telescope views."
            ),
            fa = listOf(
                "NGC 752 خوشه باز پیری است و برای بررسی چگونگی بقای خوشه‌های کم‌پیوند در قرص کهکشان ارزش دارد.",
                "غول‌های سرخ تکامل‌یافته و نقطه ترک رشته اصلی آن به محدود کردن سن خوشه کمک می‌کنند.",
                "چون در پهنه‌ای گسترده پخش شده است، دوربین دوچشمی ساختار آن را بهتر از بزرگنمایی زیاد تلسکوپ نشان می‌دهد."
            )
        ),
        "dso_ngc_7662" to BilingualFacts(
            en = listOf(
                "The Blue Snowball's blue-green color comes from strong oxygen emission in highly ionized gas.",
                "It has a bright inner shell with fainter surrounding structure, evidence for changing winds from the central star.",
                "Its high surface brightness makes it one of the easier planetary nebulae to inspect at high magnification."
            ),
            fa = listOf(
                "رنگ آبی-سبز گلوله‌برفی آبی از تابش نیرومند اکسیژن در گاز بسیار یونیده پدید می‌آید.",
                "این جرم پوسته درونی روشن و ساختار بیرونی کم‌نورتر دارد؛ نشانه بادهای متغیر ستاره مرکزی.",
                "روشنایی سطحی زیاد آن باعث می‌شود یکی از سیاره‌نماهای آسان‌تر برای بررسی با بزرگنمایی بالا باشد."
            )
        ),
        "dso_ngc_891" to BilingualFacts(
            en = listOf(
                "NGC 891 is a nearly edge-on spiral whose dark dust lane gives observers a Milky-Way-like perspective from outside.",
                "Deep images show faint halo light and gas above the disk, evidence of material cycling out of the star-forming plane.",
                "Its orientation makes it a benchmark galaxy for studying vertical disk structure and interstellar dust."
            ),
            fa = listOf(
                "NGC 891 مارپیچی تقریباً لبه‌نماست و نوار غبار تاریکش نمایی شبیه دیدن راه شیری از بیرون فراهم می‌کند.",
                "تصاویر ژرف نور هاله‌ای و گاز کم‌نور بالای قرص را نشان می‌دهند؛ نشانه گردش ماده بیرون از صفحه ستاره‌زایی.",
                "جهت‌گیری آن، این کهکشان را معیاری برای مطالعه ساختار عمودی قرص و غبار میان‌ستاره‌ای کرده است."
            )
        )
    )
}
