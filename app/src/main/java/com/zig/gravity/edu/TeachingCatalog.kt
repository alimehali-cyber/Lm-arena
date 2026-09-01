package com.zig.gravity.edu

import com.zig.gravity.edu.detectors.SimulationDetectors

/**
 * §3.14 Education — Persian-first, three tiers, opt-in, never blocking.
 *
 * Tier ladder (locked wording):
 *   1. «چه اتفاقی افتاد؟»  — what just happened, one calm sentence
 *   2. «چرا؟»              — the mechanism
 *   3. «بیشتر بدانیم»      — the law, with its formula
 *
 * Tone: friendly, intelligent, beautiful. Never childish, never stiff, never patronising.
 */
data class TeachingCard(
    val concept: String,
    val titleFa: String,
    val titleEn: String,
    /** Tier 1 — «چه اتفاقی افتاد؟» */
    val whatFa: String,
    val whatEn: String,
    /** Tier 2 — «چرا؟» */
    val whyFa: String,
    val whyEn: String,
    /** Tier 3 — «بیشتر بدانیم» */
    val moreFa: String,
    val moreEn: String,
    val formula: String? = null,
    /**
     * §24 — "Try this". The one sentence that turns a card from something you read into something
     * you do. Optional: an event card that fires mid-experiment often has no useful next action,
     * while every preset intro card does.
     */
    val tryThisFa: String? = null,
    val tryThisEn: String? = null
)

enum class TeachingTier { WHAT, WHY, MORE }

object TeachingCatalog {

    // ---- §23 preset intro concepts. Namespaced so they can never collide with the event
    // detectors' concept ids, and looked up from the Preset enum name at load time.
    const val PRESET_PREFIX = "preset_"
    const val PRESET_TWO_BODY_ORBIT = "preset_TWO_BODY_ORBIT"
    const val PRESET_ESCAPE_VELOCITY = "preset_ESCAPE_VELOCITY"
    const val PRESET_MASS_MATTERS = "preset_MASS_MATTERS"
    const val PRESET_COLLISION_LAB = "preset_COLLISION_LAB"
    const val PRESET_PERTURBATION = "preset_PERTURBATION"
    const val PRESET_BLACK_HOLE_ENCOUNTER = "preset_BLACK_HOLE_ENCOUNTER"

    /** The concept id a preset's intro card is filed under, if it has one. */
    fun presetConcept(presetName: String): String = PRESET_PREFIX + presetName

    val TIER_LABEL_FA = mapOf(
        TeachingTier.WHAT to "چه اتفاقی افتاد؟",
        TeachingTier.WHY to "چرا؟",
        TeachingTier.MORE to "بیشتر بدانیم"
    )

    val TIER_LABEL_EN = mapOf(
        TeachingTier.WHAT to "What happened?",
        TeachingTier.WHY to "Why?",
        TeachingTier.MORE to "Go deeper"
    )

    private val cards: Map<String, TeachingCard> = listOf(
        TeachingCard(
            concept = SimulationDetectors.ORBIT_STABILIZED,
            titleFa = "یک مدار پایدار شکل گرفت",
            titleEn = "A stable orbit formed",
            whatFa = "دیدی؟ اگر سرعت و فاصله مناسب باشند، جسم می‌تواند به جای سقوط مستقیم، در مدار بماند.",
            whatEn = "See that? With the right speed at the right distance, a body stays in orbit instead of falling straight in.",
            whyFa = "جسم در حال سقوط است — اما همزمان آن‌قدر به پهلو حرکت می‌کند که همیشه از کنار جرم مرکزی رد می‌شود. مدار یعنی «سقوط بی‌پایان به دور چیزی».",
            whyEn = "It really is falling — but it also moves sideways fast enough to keep missing. An orbit is an endless fall around something.",
            moreFa = "برای مدار دایره‌ای، کشش گرانشی باید دقیقاً همان شتاب مرکزگرای لازم را تأمین کند: GM/r² = v²/r، پس v = √(GM/r). هر چه دورتر، سرعت لازم کمتر و دوره تناوب بیشتر.",
            moreEn = "For a circular orbit gravity must supply exactly the required centripetal acceleration: GM/r² = v²/r, so v = √(GM/r). Farther out means slower, and a longer period.",
            formula = "v = √(GM / r)"
        ),
        TeachingCard(
            concept = SimulationDetectors.BODY_ESCAPED,
            titleFa = "از میدان گرانشی فرار کرد",
            titleEn = "It escaped the gravity well",
            whatFa = "از میدان گرانشی فرار کرد. دیگر برنمی‌گردد.",
            whatEn = "It escaped the gravitational field. It will not come back.",
            whyFa = "انرژی جنبشی‌اش از انرژی پیوند گرانشی بیشتر شد. گرانش هرگز به صفر نمی‌رسد، اما آن‌قدر ضعیف می‌شود که نمی‌تواند جسم را برگرداند.",
            whyEn = "Its kinetic energy exceeded the gravitational binding energy. Gravity never reaches zero, but it weakens too fast to pull the body back.",
            moreFa = "شرط فرار این است که انرژی مکانیکی ویژه منفی نباشد: ε = v²/۲ − GM/r ≥ ۰. از همین‌جا سرعت گریز به دست می‌آید: v_esc = √(2GM/r).",
            moreEn = "Escape happens when the specific mechanical energy is non-negative: ε = v²/2 − GM/r ≥ 0, which gives v_esc = √(2GM/r).",
            formula = "v_esc = √(2GM / r)"
        ),
        TeachingCard(
            concept = SimulationDetectors.BODY_MERGED,
            titleFa = "برخورد و ادغام",
            titleEn = "Collision and merger",
            whatFa = "برخورد رخ داد. تکانه قبل و بعد را مقایسه کن — عدد عوض نشده است.",
            whatEn = "A collision happened. Compare the momentum before and after — the number did not change.",
            whyFa = "در برخورد کاملاً ناکشسان دو جسم یکی می‌شوند. جرم‌ها جمع می‌شوند و سرعت جدید، میانگین وزنی سرعت‌ها بر حسب جرم است.",
            whyEn = "In a perfectly inelastic collision the two bodies become one. Masses add, and the new velocity is the mass-weighted average.",
            moreFa = "پایستگی تکانه خطی: m₁v₁ + m₂v₂ = (m₁+m₂)v′. حجم هم حفظ می‌شود، پس شعاع جسم تازه برابر است با r′ = (r₁³ + r₂³)^⅓ — با فرض چگالی یکسان.",
            moreEn = "Conservation of linear momentum: m₁v₁ + m₂v₂ = (m₁+m₂)v′. Volume is conserved too, so r′ = (r₁³ + r₂³)^⅓ under the equal-density assumption.",
            formula = "m₁v₁ + m₂v₂ = (m₁+m₂)v′"
        ),
        TeachingCard(
            concept = SimulationDetectors.BH_CAPTURE,
            titleFa = "سیاه‌چاله جسم را گرفت",
            titleEn = "The black hole captured it",
            whatFa = "جسم از مرز حلقه گذشت و جذب شد. جرم سیاه‌چاله به همان اندازه بیشتر شد.",
            whatEn = "The body crossed the ring and was absorbed. The hole's mass grew by exactly that amount.",
            whyFa = "در این شبیه‌سازی، سیاه‌چاله فقط یک جرم نیوتنی است؛ نه جاروبرقی جادویی. از دور، گرانش آن دقیقاً مثل هر جرم هم‌اندازه دیگری است.",
            whyEn = "In this sandbox a black hole is just a Newtonian mass — not a magic vacuum. From far away its gravity is identical to any other mass of the same size.",
            moreFa = "حلقه‌ای که می‌بینی یک قرارداد نمایشی است و مرز گرفته‌شدن در همین شبیه‌سازی را نشان می‌دهد. افق رویداد واقعی از شعاع شوارتزشیلد می‌آید: r_s = 2GM/c². این عدد را در بازرس می‌بینی، اما محاسبه‌های ما کاملاً نیوتنی است و نسبیت عام را حل نمی‌کند.",
            moreEn = "The ring you see is a display convention marking this sandbox's capture boundary. A real event horizon comes from the Schwarzschild radius r_s = 2GM/c². The inspector shows that number, but our maths is purely Newtonian and does not solve general relativity.",
            formula = "r_s = 2GM / c²"
        ),
        TeachingCard(
            concept = SimulationDetectors.WORMHOLE_TRAVERSAL,
            titleFa = "عبور از کرم‌چاله (مدل فرضی)",
            titleEn = "Wormhole traversal (hypothetical model)",
            whatFa = "جسم از یک دهانه وارد شد و با همان سرعت از دهانه جفت بیرون آمد.",
            whatEn = "The body entered one mouth and left its partner with exactly the same velocity.",
            whyFa = "این یک انتخاب مدل‌سازی است، نه یک نتیجه فیزیکی. ما تصمیم گرفته‌ایم بردار سرعت دست‌نخورده بماند، چون هندسه واقعی دهانه کرم‌چاله تعریف‌شده نیست.",
            whyEn = "This is a modelling choice, not a physical result. We keep the velocity vector untouched because the real geometry of a wormhole mouth is undefined physics.",
            moreFa = "پل اینشتین-روزن یک جواب ریاضی معتبر در نسبیت عام است، اما هیچ کرم‌چاله قابل‌عبوری هرگز مشاهده نشده و جواب‌های شناخته‌شده به «ماده اگزوتیک» با انرژی منفی نیاز دارند. اینجا کرم‌چاله یک ابزار آموزشی است، نه ادعای علمی.",
            moreEn = "The Einstein–Rosen bridge is a legitimate mathematical solution in general relativity, but no traversable wormhole has ever been observed and the known solutions require exotic matter with negative energy. Here it is a teaching device, not a scientific claim.",
            formula = null
        ),
        TeachingCard(
            concept = SimulationDetectors.ORBIT_DECAYED,
            titleFa = "مدار در حال تنگ‌تر شدن است",
            titleEn = "The orbit is tightening",
            whatFa = "مدار دارد کوچک می‌شود. جسم آرام‌آرام به جرم مرکزی نزدیک‌تر می‌شود.",
            whatEn = "The orbit is shrinking. The body is spiralling slowly inward.",
            whyFa = "در یک سامانه چندجرمی، هر عبور نزدیک کمی انرژی جابه‌جا می‌کند. کافی است یک جسم سوم مدام مدار را هم بزند تا نیم‌قطر بزرگ کم شود.",
            whyEn = "In a many-body system every close pass trades a little energy. A third body nudging the orbit is enough to shrink the semi-major axis.",
            moreFa = "نیم‌قطر بزرگ از انرژی ویژه می‌آید: a = −GM / (2ε). کاهش a یعنی ε منفی‌تر شده، یعنی جسم انرژی از دست داده است.",
            moreEn = "The semi-major axis follows from the specific energy: a = −GM / (2ε). A smaller a means a more negative ε, so the body has lost energy.",
            formula = "a = −GM / (2ε)"
        ),
        TeachingCard(
            concept = SimulationDetectors.TWO_BODY_DANCE,
            titleFa = "رقص دو جرم به دور مرکز مشترک",
            titleEn = "Two bodies dancing around one point",
            whatFa = "هیچ‌کدام دور دیگری نمی‌چرخد؛ هر دو به دور یک نقطه خالی می‌چرخند: مرکز جرم مشترک.",
            whatEn = "Neither one orbits the other: both circle an empty point, their shared barycentre.",
            whyFa = "گرانش دوطرفه است. زمین ماه را می‌کشد و ماه هم دقیقاً با همان نیرو زمین را می‌کشد؛ فقط چون جرم زمین بیشتر است، کمتر جابه‌جا می‌شود.",
            whyEn = "Gravity is mutual. Earth pulls the Moon and the Moon pulls Earth with exactly the same force; Earth simply moves less because it is heavier.",
            moreFa = "مرکز جرم روی خط واصل و طبق m₁r₁ = m₂r₂ قرار می‌گیرد. نشانگر مرکز جرم را روشن کن و ببین که دقیقاً روی مرکز جرم سنگین‌تر نیست.",
            moreEn = "The barycentre sits on the connecting line where m₁r₁ = m₂r₂. Turn the marker on and notice it is not exactly at the heavier body's centre.",
            formula = "m₁r₁ = m₂r₂"
        ),
        TeachingCard(
            concept = SimulationDetectors.MASS_CHANGED,
            titleFa = "جرم را تغییر دادی",
            titleEn = "You changed the mass",
            whatFa = "جرم را تغییر دادی. حالا چه چیزی باید تغییر کند؟ به مسیر پیش‌بینی‌شده نگاه کن.",
            whatEn = "You changed the mass. What should change now? Watch the predicted path.",
            whyFa = "نیروی گرانش با جرم نسبت مستقیم دارد. جرم بیشتر یعنی کشش قوی‌تر، پس مدارها تنگ‌تر و تندتر می‌شوند.",
            whyEn = "Gravitational force is proportional to mass. More mass means a stronger pull, so orbits become tighter and faster.",
            moreFa = "F = G·m₁m₂/r². اما شتابی که هر جسم می‌گیرد به جرم خودش بستگی ندارد: a = GM/r². به همین دلیل پر و سبک با هم می‌افتند.",
            moreEn = "F = G·m₁m₂/r². Yet the acceleration a body receives does not depend on its own mass: a = GM/r². That is why a feather and a stone fall together.",
            formula = "F = G·m₁m₂ / r²"
        ),
        TeachingCard(
            concept = SimulationDetectors.POSITION_MOVED,
            titleFa = "جای جسم را عوض کردی",
            titleEn = "You moved the body",
            whatFa = "فقط جای زمین را عوض کردی؛ سرعتش همان سرعت قبلی است. حالا مدارش چگونه تغییر می‌کند؟",
            whatEn = "You only changed where it is; its speed is exactly what it was. So how will the orbit change?",
            whyFa = "مدار از دو چیز ساخته می‌شود: اینکه کجا هستی و با چه سرعتی حرکت می‌کنی. وقتی فقط مکان عوض شود، همان سرعت قدیمی در فاصله جدید معنای تازه‌ای پیدا می‌کند.",
            whyEn = "An orbit is made of two things: where you are and how fast you are moving. Change only the place, and the old speed means something new at the new distance.",
            moreFa = "سرعت لازم برای مدار دایره‌ای v = √(GM/r) است. اگر جسم را دورتر ببری و سرعتش را دست نزنی، سرعتش برای آن فاصله زیاد است و مدار کشیده می‌شود؛ اگر نزدیک‌تر ببری، کم است و جسم به سمت داخل می‌افتد.",
            moreEn = "A circular orbit needs v = √(GM/r). Move a body outward without touching its speed and it is now moving too fast for that distance, so the orbit stretches; move it inward and it is too slow, so it falls inward.",
            formula = "v = √(GM / r)"
        ),
        TeachingCard(
            concept = SimulationDetectors.VELOCITY_CHANGED,
            titleFa = "سرعت اولیه را تغییر دادی",
            titleEn = "You changed the starting speed",
            whatFa = "سرعت اولیه تغییر کرد؛ ببین مدار جدید چگونه شکل می‌گیرد.",
            whatEn = "The starting velocity changed; watch how the new orbit takes shape.",
            whyFa = "گرانش همان است و مکان همان است؛ تنها چیزی که عوض شده سرعت است. کمی تندتر یعنی مدار کشیده‌تر، خیلی تندتر یعنی فرار، و کندتر یعنی سقوط به سمت مرکز.",
            whyEn = "Gravity is unchanged and the position is unchanged; only the velocity is different. A little faster stretches the orbit, much faster escapes, slower falls inward.",
            moreFa = "مرز فرار در v = √(2GM/r) است — دقیقاً √۲ برابر سرعت مدار دایره‌ای. جهت سرعت هم به اندازه بزرگی آن مهم است.",
            moreEn = "The escape boundary sits at v = √(2GM/r) — exactly √2 times the circular speed. Direction matters just as much as magnitude.",
            formula = "v_esc = √(2GM / r)"
        ),
        TeachingCard(
            concept = SimulationDetectors.IMPACT_ENERGY,
            titleFa = "برخورد",
            titleEn = "Impact",
            whatFa = "دو جرم به هم برخورد کردند. انرژی و تکانه‌ی برخورد تعیین می‌کنند چه اتفاقی بیفتد.",
            whatEn = "Two masses collided. The energy and momentum of the impact decide what happens next.",
            whyFa = "اگر سرعت نزدیک‌شدن کم باشد، برخورد ملایم است. اگر خیلی بیشتر از سرعت فرار آن دو جسم باشد، انرژی جنبشی از انرژی پیوند گرانشی‌شان بیشتر است و برخورد ویرانگر می‌شود.",
            whyEn = "If the closing speed is small the contact is gentle. If it is far above the pair's escape speed, the kinetic energy exceeds their gravitational binding energy and the impact becomes destructive.",
            moreFa = "در هر برخوردی تکانه پایسته است: p = Σmv قبل و بعد یکی است. انرژی جنبشی اما پایسته نیست؛ بخشی از آن به گرما و تغییر شکل می‌رود. معیار ما نسبت v به √(2G(m₁+m₂)/(r₁+r₂)) است.",
            moreEn = "Momentum is conserved in every collision: p = Σmv is the same before and after. Kinetic energy is not; some of it becomes heat and deformation. Our yardstick is v against √(2G(m₁+m₂)/(r₁+r₂)).",
            formula = "p = Σmv"
        ),
        TeachingCard(
            concept = SimulationDetectors.MOON_QUESTION,
            titleFa = "چرا ماه روی زمین نمی‌افتد؟",
            titleEn = "Why doesn't the Moon fall to Earth?",
            whatFa = "چرا ماه با وجود کشش زمین، روی زمین نمی‌افتد؟ حدس بزن، بعد نگاه کن.",
            whatEn = "Earth pulls the Moon constantly — so why doesn't it fall? Make a guess, then watch.",
            whyFa = "حقیقت این است که ماه واقعاً در حال افتادن است. اما همزمان با سرعت زیادی به پهلو حرکت می‌کند، پس همیشه از کنار زمین رد می‌شود و دوباره می‌افتد.",
            whyEn = "The truth is that the Moon is falling. But it also moves sideways so fast that it keeps missing Earth — and falls again, forever.",
            moreFa = "اگر سرعت جانبی ماه را صفر کنی، مستقیم سقوط می‌کند. اگر خیلی زیادش کنی، فرار می‌کند. بین این دو، یک بازه سرعت وجود دارد که مدار می‌سازد: نزدیک v = √(GM/r).",
            moreEn = "Set the Moon's sideways speed to zero and it drops straight in. Make it far too large and it escapes. Between those extremes lies the range that makes an orbit, around v = √(GM/r).",
            formula = "v = √(GM / r)"
        )
        // ---- §22/§23 preset intro cards -------------------------------------------------------
        //
        // One card per educational scene, shown when the scene loads. They follow the §24 shape:
        // what to notice, why it happens, how to go deeper, and something to try. Every claim is
        // about what the integrator will actually do with that scene's initial conditions.
        TeachingCard(
            concept = PRESET_TWO_BODY_ORBIT,
            titleFa = "چرا زمین همچنان می‌چرخد؟",
            titleEn = "Why does it keep orbiting?",
            whatFa = "جسم کوچک مدام به سمت جرم مرکزی سقوط می‌کند — و مدام از کنارش رد می‌شود.",
            whatEn = "The small body keeps falling toward the central mass, and keeps missing it.",
            whyFa = "گرانش آن را به داخل می‌کشد، اما جسم همزمان به پهلو حرکت می‌کند. ترکیب این دو، یک منحنی بسته می‌سازد: مدار.",
            whyEn = "Gravity pulls it inward while it also moves sideways. The two together bend its path into a closed curve: an orbit.",
            moreFa = "برای مدار دایره‌ای، گرانش باید دقیقاً شتاب مرکزگرای لازم را بدهد: GM/r² = v²/r. پس سرعت لازم فقط به جرم مرکزی و فاصله بستگی دارد، نه به جرم خودِ جسم.",
            moreEn = "A circular orbit needs gravity to supply exactly the centripetal acceleration: GM/r² = v²/r. The required speed depends only on the central mass and the distance, never on the orbiting body's own mass.",
            formula = "v = √(GM / r)",
            tryThisFa = "سرعت جسم را کمی زیاد کن و ببین مدار چطور کشیده می‌شود.",
            tryThisEn = "Nudge its speed up and watch the orbit stretch."
        ),
        TeachingCard(
            concept = PRESET_ESCAPE_VELOCITY,
            titleFa = "کِی گرانش دیگر کافی نیست؟",
            titleEn = "When is gravity not enough?",
            whatFa = "سه جسم از یک نقطه و در یک جهت شروع می‌کنند، فقط با سرعت‌های متفاوت. سرنوشتشان کاملاً فرق می‌کند.",
            whatEn = "Three bodies start from the same place in the same direction, differing only in speed. Their fates are completely different.",
            whyFa = "زیر یک سرعت مشخص، انرژی جنبشی از چاه گرانشی کمتر است و جسم برمی‌گردد. بالای آن، انرژی بیشتر است و جسم هرگز برنمی‌گردد.",
            whyEn = "Below a certain speed the kinetic energy is less than the depth of the gravity well, so the body comes back. Above it, the body never returns.",
            moreFa = "مرز دقیقاً جایی است که انرژی کل صفر شود: ½v² = GM/r. سرعت گریز به جرمِ خودِ جسم بستگی ندارد — یک سنگ و یک سفینه سرعت گریز یکسانی دارند.",
            moreEn = "The threshold is exactly where the total energy reaches zero: ½v² = GM/r. Escape speed does not depend on the escaping body's own mass — a pebble and a spacecraft need the same speed.",
            formula = "v_esc = √(2GM / r)",
            tryThisFa = "جسم کندتر را انتخاب کن و سرعتش را کم‌کم زیاد کن تا لحظهٔ گریز را پیدا کنی.",
            tryThisEn = "Select the slowest body and raise its speed until you find the moment it escapes."
        ),
        TeachingCard(
            concept = PRESET_MASS_MATTERS,
            titleFa = "جرم چه چیزی را عوض می‌کند؟",
            titleEn = "What does mass change?",
            whatFa = "دو جسم آزمایشی، در فاصلهٔ یکسان و با سرعت یکسان. الان مسیرشان قرینه است.",
            whatEn = "Two test bodies at the same distance with the same speed. Right now their paths mirror each other.",
            whyFa = "کشش گرانشی به جرم جسم مرکزی بستگی دارد. جرم مرکزی را عوض کن و همان سرعت، دیگر برای مدار دایره‌ای مناسب نیست.",
            whyEn = "The pull depends on the central body's mass. Change it and the same speed is no longer the right speed for a circular orbit.",
            moreFa = "نیرو با حاصل‌ضرب دو جرم متناسب است، اما شتابِ جسم سبک فقط به جرم دیگری بستگی دارد — چون m در F = ma حذف می‌شود. برای همین سرعت مداری به جرم خودِ مدارگرد وابسته نیست.",
            moreEn = "The force scales with the product of both masses, but the light body's acceleration depends only on the other mass, because m cancels in F = ma. That is why orbital speed does not depend on the orbiter's own mass.",
            formula = "F = G·m₁·m₂ / r²",
            tryThisFa = "خورشید را انتخاب کن و جرمش را دو برابر کن. هر دو جسم آزمایشی را با هم تماشا کن.",
            tryThisEn = "Select the star and double its mass. Watch both test bodies at once."
        ),
        TeachingCard(
            concept = PRESET_COLLISION_LAB,
            titleFa = "وقتی دو جسم به هم می‌رسند",
            titleEn = "When two bodies meet",
            whatFa = "دو جسم با جرم متفاوت روی مسیرهای متقاطع. جسم سنگین‌تر کمتر منحرف می‌شود.",
            whatEn = "Two bodies of different mass on intersecting paths. The heavier one is deflected less.",
            whyFa = "در برخورد، تکانهٔ کل حفظ می‌شود. سهم هر جسم از تغییر سرعت، وارونهٔ جرمش است: جرم بیشتر یعنی تغییر سرعت کمتر.",
            whyEn = "Momentum is conserved through the impact. Each body's share of the velocity change is inversely proportional to its mass: more mass, less change.",
            moreFa = "انرژی‌ای که واقعاً در برخورد آزاد می‌شود از جرم کاهش‌یافته می‌آید، نه از انرژی جنبشی کل: E = ½μv²، با μ = m₁m₂/(m₁+m₂). بخش بزرگی از انرژی جنبشی کل فقط حرکت مشترک این جفت است و در برخورد حس نمی‌شود.",
            moreEn = "The energy actually released comes from the reduced mass, not the total kinetic energy: E = ½μv², with μ = m₁m₂/(m₁+m₂). Most of the total kinetic energy is just the pair's shared drift, which no collision can feel.",
            formula = "E = ½·μ·v²  ·  μ = m₁m₂/(m₁+m₂)",
            tryThisFa = "سرعت یکی از دو جسم را نصف کن و ببین شدت برخورد چقدر فرق می‌کند.",
            tryThisEn = "Halve one body's speed and see how much gentler the impact grades."
        ),
        TeachingCard(
            concept = PRESET_PERTURBATION,
            titleFa = "هیچ مداری تنها نیست",
            titleEn = "No orbit is alone",
            whatFa = "زمین روی همان مدار همیشگی شروع می‌کند، اما این بار یک همراه سنگین هم هست. مدار کم‌کم تغییر می‌کند.",
            whatEn = "Earth starts on exactly its usual orbit, but this time there is a heavy companion. The orbit slowly changes.",
            whyFa = "گرانش فقط بین دو جسم نیست؛ هر جسم روی هر جسم دیگری اثر می‌گذارد. اثر کوچک همراه، در طول زمان جمع می‌شود.",
            whyEn = "Gravity is not a private arrangement between two bodies. Everything pulls on everything, and the companion's small tug accumulates over time.",
            moreFa = "به همین دلیل مسئلهٔ چند جسمی راه‌حل بستهٔ ساده ندارد و مدارهای واقعی سیارات را باید عددی محاسبه کرد. کشف نپتون دقیقاً از همین آشفتگی در مدار اورانوس شروع شد.",
            moreEn = "This is why the many-body problem has no simple closed solution and why real planetary orbits are computed numerically. Neptune was discovered precisely from this kind of perturbation in Uranus's orbit.",
            tryThisFa = "«رد حرکت» را روشن کن و چند دور صبر کن تا انحراف مدار دیده شود.",
            tryThisEn = "Turn on trails and wait a few laps until the drift becomes visible."
        ),
        TeachingCard(
            concept = PRESET_BLACK_HOLE_ENCOUNTER,
            titleFa = "سیاه‌چاله جاروبرقی نیست",
            titleEn = "A black hole is not a vacuum cleaner",
            whatFa = "جسم آزمایشی از کنار سیاه‌چاله می‌گذرد، منحرف می‌شود و برمی‌گردد — مثل عبور از کنار هر جرم سنگین دیگری.",
            whatEn = "The test body swings past the black hole, bends, and comes back — just as it would passing any other heavy mass.",
            whyFa = "از فاصلهٔ دور، گرانش سیاه‌چاله فقط به جرمش بستگی دارد. اگر خورشید را با سیاه‌چاله‌ای هم‌جرم عوض کنیم، مدار زمین تغییری نمی‌کند.",
            whyEn = "From far away a black hole's gravity depends only on its mass. Swap the Sun for a black hole of the same mass and Earth's orbit would not change at all.",
            moreFa = "چیزی که سیاه‌چاله را خاص می‌کند فاصله است، نه کشش: جرم در حجمی چنان کوچک جمع شده که می‌توان بسیار نزدیک شد، و آنجا میدان گرانشی به‌شدت قوی می‌شود. این شبیه‌سازی نیوتنی است و اثرهای نسبیت عام را مدل نمی‌کند.",
            moreEn = "What makes a black hole special is how close you can get, not how hard it pulls: the mass sits in such a small volume that you can approach very near, and there the field becomes extreme. This sandbox is Newtonian and does not model general relativity.",
            tryThisFa = "جسم را بکش و نزدیک‌تر رها کن تا ببینی مسیرش چقدر تندتر خم می‌شود.",
            tryThisEn = "Drag the body closer and release it to see how much harder its path bends."
        ),
    ).associateBy { it.concept }

    fun card(concept: String): TeachingCard? = cards[concept]

    fun text(card: TeachingCard, tier: TeachingTier, isFa: Boolean): String = when (tier) {
        TeachingTier.WHAT -> if (isFa) card.whatFa else card.whatEn
        TeachingTier.WHY -> if (isFa) card.whyFa else card.whyEn
        TeachingTier.MORE -> if (isFa) card.moreFa else card.moreEn
    }
}

/** §3.14 locked glossary — 18 terms, Persian-first. */
data class GlossaryTerm(val fa: String, val en: String, val meaningFa: String, val meaningEn: String)

object Glossary {
    val terms: List<GlossaryTerm> = listOf(
        GlossaryTerm("گرانش", "Gravity", "کششی که هر دو جرم بر یکدیگر وارد می‌کنند.", "The attraction every two masses exert on each other."),
        GlossaryTerm("مدار", "Orbit", "مسیر بسته‌ای که یک جسم زیر کشش جرم دیگر می‌پیماید.", "The closed path a body follows under another body's pull."),
        GlossaryTerm("جرم", "Mass", "مقدار ماده جسم؛ سرچشمه گرانش آن.", "How much matter a body has; the source of its gravity."),
        GlossaryTerm("شعاع", "Radius", "فاصله مرکز جسم تا سطح آن.", "Distance from a body's centre to its surface."),
        GlossaryTerm("سرعت", "Velocity", "تندی همراه با جهت حرکت.", "Speed together with a direction."),
        GlossaryTerm("سرعت گریز", "Escape velocity", "کمترین سرعتی که جسم را برای همیشه از چاه گرانشی بیرون می‌برد.", "The smallest speed that frees a body from a gravity well for good."),
        GlossaryTerm("تکانه", "Momentum", "حاصل‌ضرب جرم در سرعت.", "Mass multiplied by velocity."),
        GlossaryTerm("پایستگی تکانه", "Conservation of momentum", "تکانه کل یک سامانه بسته تغییر نمی‌کند.", "The total momentum of a closed system never changes."),
        GlossaryTerm("انرژی جنبشی", "Kinetic energy", "انرژی ناشی از حرکت: ½mv².", "Energy of motion: ½mv²."),
        GlossaryTerm("انرژی پتانسیل", "Potential energy", "انرژی ذخیره‌شده در چیدمان گرانشی اجسام.", "Energy stored in the gravitational arrangement of bodies."),
        GlossaryTerm("برخورد", "Collision", "لحظه‌ای که دو جسم به هم می‌رسند.", "The moment two bodies touch."),
        GlossaryTerm("ادغام", "Merger", "یکی شدن دو جسم پس از برخورد.", "Two bodies becoming one after a collision."),
        GlossaryTerm("سیاه‌چاله", "Black hole", "جرمی چنان فشرده که در اینجا آن را یک نقطه‌جرم نیوتنی مدل می‌کنیم.", "A mass so compact that we model it here as a Newtonian point mass."),
        GlossaryTerm("افق رویداد", "Event horizon", "مرزی که در فیزیک واقعی حتی نور از آن بیرون نمی‌آید.", "The boundary that, in real physics, not even light escapes."),
        GlossaryTerm("کرم‌چاله", "Wormhole", "میان‌بر نظری میان دو نقطه فضازمان؛ فرضی و اثبات‌نشده.", "A theoretical shortcut between two points of spacetime; hypothetical and unproven."),
        GlossaryTerm("شبیه‌سازی", "Simulation", "بازسازی عددی رفتار یک سامانه فیزیکی.", "A numerical reconstruction of a physical system's behaviour."),
        GlossaryTerm("پیش‌بینی", "Prediction", "حدس آگاهانه درباره نتیجه، پیش از دیدن آن.", "An informed guess about the outcome, made before seeing it."),
        GlossaryTerm("جسم آزمایشی", "Test marble", "جسمی بی‌جرم که گرانش را حس می‌کند اما خودش گرانش ندارد.", "A massless body that feels gravity but exerts none."),
        GlossaryTerm("سیارک", "Asteroid", "سنگ‌آسمانی کوچک با جرم بسیار کم.", "A small rocky body with very little mass.")
    )
}
