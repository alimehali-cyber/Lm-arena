package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

enum class ExplanationLevel {
    LEVEL_1_SIMPLE,      // Children & beginners
    LEVEL_2_MECHANISM,   // Physical mechanism explanation
    LEVEL_3_SCIENTIFIC   // Precise mathematical law & formula
}

enum class TeachingPriority {
    CRITICAL, HIGH, MEDIUM, LOW
}

data class TeachingMoment(
    val id: String,
    val conceptKey: String,
    val titleEn: String,
    val titleFa: String,
    val shortNoteEn: String,
    val shortNoteFa: String,
    val level1En: String,
    val level1Fa: String,
    val level2En: String,
    val level2Fa: String,
    val level3En: String,
    val level3Fa: String,
    val formulaSymbol: String? = null,
    val priority: TeachingPriority = TeachingPriority.MEDIUM,
    val personalityReactionEn: String? = null,
    val personalityReactionFa: String? = null
)

data class ExperimentOption(
    val id: String,
    val textEn: String,
    val textFa: String,
    val isCorrect: Boolean = false
)

data class InteractiveExperiment(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val questionEn: String,
    val questionFa: String,
    val predictionOptions: List<ExperimentOption>,
    val scenario: PresetScenario,
    val instructionEn: String,
    val instructionFa: String,
    val observationGoalEn: String,
    val observationGoalFa: String,
    val explanationMoment: TeachingMoment
)

object GravityTeachingCatalog {

    val MOMENT_MASS_INCREASE = TeachingMoment(
        id = "mass_increase",
        conceptKey = "gravity_mass",
        titleEn = "Mass Increased",
        titleFa = "افزایش جرم",
        shortNoteEn = "More mass creates a stronger gravitational pull.",
        shortNoteFa = "جرم بیشتر؛ کشش گرانشی قوی‌تر ایجاد می‌کند.",
        level1En = "More mass means stronger gravity!",
        level1Fa = "جرم بیشتر یعنی گرانش بیشتر!",
        level2En = "As you increase an object's mass, space warps more around it, pulling surrounding bodies faster.",
        level2Fa = "با افزایش جرم یک جسم، کشش گرانشی آن بر اجرام اطراف بیشتر و شدیدتر می‌شود.",
        level3En = "Gravitational force scales linearly with mass: F = G * (m1 * m2) / r².",
        level3Fa = "نیروی گرانشی میان دو جرم با حاصل‌ضرب جرم‌های آن‌ها نسبت مستقیم دارد: F = G × m₁m₂ / r².",
        formulaSymbol = "F ∝ m₁ · m₂",
        priority = TeachingPriority.HIGH,
        personalityReactionEn = "Mass increased! Now gravity has more to say.",
        personalityReactionFa = "جرم را زیاد کردی؛ حالا گرانش حرف بیشتری برای گفتن دارد."
    )

    val MOMENT_MASS_DECREASE = TeachingMoment(
        id = "mass_decrease",
        conceptKey = "gravity_mass_dec",
        titleEn = "Mass Decreased",
        titleFa = "کاهش جرم",
        shortNoteEn = "Less mass weakens the gravitational pull.",
        shortNoteFa = "کاهش جرم باعث ضعیف شدن کشش گرانشی می‌شود.",
        level1En = "Less mass means weaker gravity.",
        level1Fa = "جرم کمتر یعنی گرانش ضعیف‌تر.",
        level2En = "Reducing an object's mass relaxes its gravitational hold, causing orbiting bodies to drift outward.",
        level2Fa = "کاهش جرم خورشید یا جسم مرکزی باعث می‌شود اجرام مداری به بیرون رانده شوند.",
        level3En = "Lower mass reduces central attraction, shifting bound circular orbits into wider elliptical or hyperbolic escape paths.",
        level3Fa = "کاهش جرم نیروی مرکزگرا را کم کرده و مدارهای دایره‌ای را به بیضوی کشیده یا مسیر فرار تبدیل می‌کند.",
        formulaSymbol = "F_g = G m₁ m₂ / r²",
        priority = TeachingPriority.MEDIUM,
        personalityReactionEn = "Gravity weakened! The orbit is relaxing.",
        personalityReactionFa = "کاهش جرم! مدار بازتر و کشیده‌تر شد."
    )

    val MOMENT_DISTANCE_CLOSE = TeachingMoment(
        id = "distance_close",
        conceptKey = "gravity_distance",
        titleEn = "Close Encounter",
        titleFa = "نزدیک شدن اجرام",
        shortNoteEn = "Gravity intensifies dramatically at closer distances.",
        shortNoteFa = "گرانش در فاصله‌های نزدیک‌تر بسیار شدیدتر می‌شود.",
        level1En = "Closer objects pull each other much harder!",
        level1Fa = "هرچه دو جسم به هم نزدیک‌تر شوند، کشش شدیدتر می‌شود!",
        level2En = "Distance has a massive impact on gravity due to the inverse-square law: halving distance quadruples gravity.",
        level2Fa = "فاصله اثر ملموسی دارد؛ اگر فاصله دو برابر کم شود، نیروی گرانش ۴ برابر افزایش می‌یابد.",
        level3En = "Newton's Inverse-Square Law: Gravitational attraction is inversely proportional to distance squared (1 / r²).",
        level3Fa = "قانون معکوس مجذور فاصله: نیروی گرانش با توان دوم فاصله نسبت عکس دارد (1 / r²).",
        formulaSymbol = "F ∝ 1 / r²",
        priority = TeachingPriority.HIGH,
        personalityReactionEn = "They got really close! Gravity surges rapidly.",
        personalityReactionFa = "خیلی نزدیک شدند! اثر گرانشی جهش یافت."
    )

    val MOMENT_COLLISION = TeachingMoment(
        id = "collision_merger",
        conceptKey = "collision",
        titleEn = "Inelastic Collision",
        titleFa = "برخورد و ادغام گرانشی",
        shortNoteEn = "Two bodies merged obeying momentum conservation.",
        shortNoteFa = "دو جسم برخورد کرده و بر اساس بقای تکانه ادغام شدند.",
        level1En = "Boom! Two objects crashed into one bigger object.",
        level1Fa = "برخورد اتفاق افتاد! دو جسم یکی شدند.",
        level2En = "When objects collide, their masses combine into a single larger body, conserving linear momentum.",
        level2Fa = "در برخورد غیرکشسان، جرم دو جسم جمع شده و سرعت جدید بر اساس قانون بقای تکانه خطی محاسبه می‌شود.",
        level3En = "Conservation of Linear Momentum: m_total * v_final = m1 * v1 + m2 * v2.",
        level3Fa = "بقای تکانه خطی: m_total × v_final = m₁v₁ + m₂v₂.",
        formulaSymbol = "p_total = m₁v₁ + m₂v₂",
        priority = TeachingPriority.CRITICAL,
        personalityReactionEn = "Collision detected! Momentum conserved in merger.",
        personalityReactionFa = "💥 برخورد اتفاق افتاد! دو جسم به هم رسیدند."
    )

    val MOMENT_ESCAPE = TeachingMoment(
        id = "escape_velocity",
        conceptKey = "escape_velocity",
        titleEn = "Escape Trajectory",
        titleFa = "فرار گرانشی",
        shortNoteEn = "Kinetic energy exceeded gravitational binding energy.",
        shortNoteFa = "انرژی جنبشی از انرژی پیوند گرانشی بیشتر شد.",
        level1En = "It escaped! Speed was too high for gravity to hold it.",
        level1Fa = "🚀 فرار کرد! سرعت آن‌قدر زیاد بود که در مدار نماند.",
        level2En = "The body reached escape velocity, entering an open hyperbolic trajectory that breaks free from central gravity.",
        level2Fa = "جسم به سرعت گریز رسید و در یک مسیر باز هذلولی برای همیشه از گرانش جرم مرکزی رها شد.",
        level3En = "Escape Velocity Condition: v_esc = √(2GM / r). Total mechanical energy E = K + U ≥ 0.",
        level3Fa = "شرایط سرعت گریز: v_esc = √(2GM / r). انرژی کل مکانیکی E = K + U ≥ 0 می‌شود.",
        formulaSymbol = "v_esc = √(2GM / r)",
        priority = TeachingPriority.CRITICAL,
        personalityReactionEn = "🚀 It escaped! No longer in a bound orbit.",
        personalityReactionFa = "🚀 این بار جسم فرار کرد!"
    )

    val MOMENT_BLACK_HOLE = TeachingMoment(
        id = "black_hole_nature",
        conceptKey = "black_hole",
        titleEn = "Black Hole Physics",
        titleFa = "ماهیت گرانشی سیاهچاله",
        shortNoteEn = "Black holes are extreme gravitational wells, not magic vacuums.",
        shortNoteFa = "سیاهچاله چاه گرانشی مفرط است، نه جاروبرقی جادویی!",
        level1En = "Black holes pull strongly because they are super dense!",
        level1Fa = "سیاهچاله چگالی مفرط دارد، نه اینکه همه‌چیز را از همه‌جا ببلعد!",
        level2En = "A black hole exerts normal gravity at a distance, but creates an Event Horizon nearby where light cannot escape.",
        level2Fa = "در فواصل دور، گرانش سیاهچاله مانند هر ستاره هم‌جرم دیگری است؛ اما در نزدیکی افق رویداد گرانش مفرط می‌شود.",
        level3En = "General Relativity & Schwarzschild Radius: r_s = 2GM / c². Spacetime curvature diverges at the singularity.",
        level3Fa = "شعاع شوارتزشیلد در نسبیت عام: r_s = 2GM / c².",
        formulaSymbol = "r_s = 2GM / c²",
        priority = TeachingPriority.HIGH,
        personalityReactionEn = "Black hole present. Extreme spacetime bending nearby.",
        personalityReactionFa = "سیاه‌چاله اضافه شد. انحراف شدید فضازمان!"
    )

    val MOMENT_WORMHOLE = TeachingMoment(
        id = "wormhole_theoretical",
        conceptKey = "wormhole",
        titleEn = "Wormhole — Theoretical Model",
        titleFa = "کرم‌چاله — مدل نظری فرضی",
        shortNoteEn = "Traversable wormholes are mathematical solutions in GR, not proven physical objects.",
        shortNoteFa = "کرم‌چاله سازه نظری در نسبیت عام است و اثبات تجربی ندارد.",
        level1En = "Wormholes are theoretical shortcuts through space in science fiction & physics math!",
        level1Fa = "کرم‌چاله یک میان‌بر نظری فرضی در ریاضیات فیزیک است!",
        level2En = "Wormholes represent Einstein-Rosen bridges connecting two distant points in spacetime, requiring exotic negative-energy matter to remain stable.",
        level2Fa = "پل اینشتین-روزن دو نقطه دوردست فضازمان را وصل می‌کند اما وجود تجربی آن نیازمند ماده با انرژی منفی است.",
        level3En = "Einstein Field Equations with Exotic Energy Tensor: G_μν + Λ g_μν = (8πG/c⁴) T_μν (T_μν Violating Weak Energy Condition).",
        level3Fa = "معادلات میدان اینشتین با ماده اگزوتیک (نقض شرط انرژی ضعیف).",
        formulaSymbol = "G_μν = (8πG / c⁴) T_μν",
        priority = TeachingPriority.HIGH,
        personalityReactionEn = "Theoretical wormhole visualization enabled.",
        personalityReactionFa = "کرم‌چاله — مدل نظری فرضی فعال شد."
    )

    // Interactive "Try It" Experiments Collection
    val experiments = listOf(
        InteractiveExperiment(
            id = "exp_double_earth_mass",
            titleEn = "What if Earth's mass doubled?",
            titleFa = "اگر جرم زمین دو برابر شود چه می‌شود؟",
            questionEn = "What will happen to the Moon's orbit if Earth suddenly becomes twice as massive?",
            questionFa = "اگر جرم زمین ناگهان دو برابر شود، چه اتفافی برای مدار ماه می‌افتد؟",
            predictionOptions = listOf(
                ExperimentOption("p1", "The Moon moves closer in a tighter orbit", "ماه به زمین نزدیک‌تر می‌شود", true),
                ExperimentOption("p2", "The Moon immediately escapes into deep space", "ماه بلافاصله به فضا فرار می‌کند", false),
                ExperimentOption("p3", "Nothing changes", "هیچ تغییری نمی‌کند", false)
            ),
            scenario = PresetScenario.SOLAR_SYSTEM,
            instructionEn = "Double Earth's mass using the inspector or mass slider and observe Moon's orbit.",
            instructionFa = "جرم زمین را دو برابر کنید و تغییر مسیر مدار ماه را ببینید.",
            observationGoalEn = "Notice how the Moon is pulled inward into a tighter, faster ellipse.",
            observationGoalFa = "مشاهده کنید چگونه ماه به سمت داخل کشیده شده و مدار تندتر می‌شود.",
            explanationMoment = MOMENT_MASS_INCREASE
        ),
        InteractiveExperiment(
            id = "exp_move_closer",
            titleEn = "How does distance affect gravity?",
            titleFa = "فاصله چه نقشی در گرانش دارد؟",
            questionEn = "If you drag a planet 2 times closer to the Sun, what happens to the gravitational force?",
            questionFa = "اگر سیاره‌ای را ۲ برابر به خورشید نزدیک‌تر کنیم، نیروی گرانش چند برابر می‌شود؟",
            predictionOptions = listOf(
                ExperimentOption("p1_2", "Force increases by 4 times (2²)", "نیرو ۴ برابر می‌شود (توان دوم)", true),
                ExperimentOption("p2_2", "Force increases by only 2 times", "نیرو ۲ برابر می‌شود", false),
                ExperimentOption("p3_2", "Force decreases", "نیرو کاهش می‌یابد", false)
            ),
            scenario = PresetScenario.SOLAR_SYSTEM,
            instructionEn = "Move Mercury or Venus closer to the Sun and observe force vectors.",
            instructionFa = "عطارد یا زهره را به خورشید نزدیک‌تر کنید و پیکان‌های نیرو را ببینید.",
            observationGoalEn = "Observe the rapid surge in velocity and force arrows at smaller distances.",
            observationGoalFa = "افزایش شدید پیکان‌های سرعت و نیرو در فاصله کمتر را مشاهده کنید.",
            explanationMoment = MOMENT_DISTANCE_CLOSE
        ),
        InteractiveExperiment(
            id = "exp_remove_sun",
            titleEn = "What if the Sun disappears?",
            titleFa = "اگر خورشید ناپدید شود چه می‌شود؟",
            questionEn = "If the Sun instantly vanishes, how will Earth move?",
            questionFa = "اگر خورشید ناگهان ناپدید شود، زمین چگونه حرکت خواهد کرد؟",
            predictionOptions = listOf(
                ExperimentOption("p1_3", "Flies off in a straight line along its tangent velocity", "در خط مستقیم مماس بر مدار ادامه می‌دهد", true),
                ExperimentOption("p2_3", "Stops instantly in space", "بلافاصله در فضا متوقف می‌شود", false),
                ExperimentOption("p3_3", "Spirals inward to where the Sun was", "به سمت مرکز خورشید سابق می‌چرخد", false)
            ),
            scenario = PresetScenario.SOLAR_SYSTEM,
            instructionEn = "Tap the Sun and delete it. Observe Earth's straight inertia path.",
            instructionFa = "روی خورشید تقه زده و آن را حذف کنید. مسیر مستقیم زمین را ببینید.",
            observationGoalEn = "Without central gravity, Earth travels in a straight inertial line (Newton's 1st Law).",
            observationGoalFa = "بدون گرانش خورشید، زمین در یک خط مستقیم اینرسی به حرکت ادامه می‌دهد (قانون اول نیوتن).",
            explanationMoment = MOMENT_ESCAPE
        ),
        InteractiveExperiment(
            id = "exp_earth_moon_barycenter",
            titleEn = "Does Earth wobble around the Moon?",
            titleFa = "آیا زمین هم به دور ماه می‌چرخد؟",
            questionEn = "Where is the true center of rotation between Earth and Moon?",
            questionFa = "مرکز واقعی چرخش میان زمین و ماه در کجا قرار دارد؟",
            predictionOptions = listOf(
                ExperimentOption("p1_4", "At the shared Center of Mass (Barycenter) inside Earth", "در مرکز جرم مشترک (باری‌سنتر) درون زمین", true),
                ExperimentOption("p2_4", "At the exact geometric center of Earth", "دقیقاً در مرکز هندسی زمین", false),
                ExperimentOption("p3_4", "In open space midway between them", "در فضای خالی بین زمین و ماه", false)
            ),
            scenario = PresetScenario.SOLAR_SYSTEM,
            instructionEn = "Turn ON Center of Mass toggle (⊕) and zoom into the Earth-Moon system.",
            instructionFa = "نشانگر مرکز جرم (⊕) را روشن کرده و روی سامانه زمین-ماه زوم کنید.",
            observationGoalEn = "Notice Earth slightly wobbles around the shared barycenter point.",
            observationGoalFa = "تاب خوردن خفیف زمین به دور نقطه باری‌سنتر مشترک را ببینید.",
            explanationMoment = TeachingMoment(
                id = "barycenter_concept",
                conceptKey = "barycenter",
                titleEn = "Earth-Moon Barycenter",
                titleFa = "مرکز جرم مشترک (باری‌سنتر)",
                shortNoteEn = "Both Earth and Moon orbit their shared center of mass.",
                shortNoteFa = "زمین و ماه هر دو به دور مرکز جرم مشترک می‌چرخند.",
                level1En = "Earth wiggles a little bit as the Moon orbits it!",
                level1Fa = "زمین هم هنگام چرخش ماه، کمی تاب می‌خورد!",
                level2En = "The Earth and Moon orbit a shared barycenter, located about 4,670 km from Earth's center.",
                level2Fa = "زمین و ماه به دور باری‌سنتر مشترک که در عمق ۴۶۷۰ کیلومتری درون زمین است می‌چرخند.",
                level3En = "Barycenter location: r_bary = r_total * [m_moon / (m_earth + m_moon)].",
                level3Fa = "موقعیت باری‌سنتر: r_bary = r_total × [m_moon / (m_earth + m_moon)].",
                formulaSymbol = "r_bary = r · m₂ / (m₁ + m₂)"
            )
        )
    )
}

class GravityTeachingObserver {

    private val shownConceptCount = mutableMapOf<String, Int>()
    private var lastTeachingTimeMs = 0L
    private val minCooldownMs = 6000L // Minimum 6 seconds between event notifications

    private val prevMasses = mutableMapOf<String, Double>()
    private var prevBodyCount = 0

    fun reset() {
        shownConceptCount.clear()
        lastTeachingTimeMs = 0L
        prevMasses.clear()
        prevBodyCount = 0
    }

    fun observeSimulation(
        bodies: List<CelestialBody>,
        diagnostics: GravitationalDiagnostics,
        currentTimeMs: Long,
        isTeachingModeOn: Boolean
    ): TeachingMoment? {
        if (!isTeachingModeOn) return null
        if (currentTimeMs - lastTeachingTimeMs < minCooldownMs) return null

        // 1. Detect Collision Merger
        if (prevBodyCount > 0 && bodies.size < prevBodyCount) {
            val count = shownConceptCount.getOrDefault("collision", 0)
            if (count < 3) {
                shownConceptCount["collision"] = count + 1
                lastTeachingTimeMs = currentTimeMs
                prevBodyCount = bodies.size
                return GravityTeachingCatalog.MOMENT_COLLISION
            }
        }
        prevBodyCount = bodies.size

        // 2. Detect Mass Changes
        for (b in bodies) {
            val oldMass = prevMasses[b.id]
            if (oldMass != null && oldMass > 0.0) {
                val ratio = b.mass / oldMass
                if (ratio > 1.25 || ratio < 0.75) {
                    val concept = if (ratio > 1.0) "mass_increase" else "mass_decrease"
                    val count = shownConceptCount.getOrDefault(concept, 0)
                    if (count < 3) {
                        shownConceptCount[concept] = count + 1
                        lastTeachingTimeMs = currentTimeMs
                        prevMasses[b.id] = b.mass
                        return if (ratio > 1.0) GravityTeachingCatalog.MOMENT_MASS_INCREASE else GravityTeachingCatalog.MOMENT_MASS_DECREASE
                    }
                }
            }
            prevMasses[b.id] = b.mass
        }

        // 3. Detect Escape Trajectory
        if (bodies.size >= 2) {
            val central = bodies.maxByOrNull { it.mass }
            if (central != null) {
                for (b in bodies) {
                    if (b.id != central.id && !b.isFixed) {
                        val dx = b.posX - central.posX
                        val dy = b.posY - central.posY
                        val r = sqrt(dx * dx + dy * dy)
                        val vSq = b.velX * b.velX + b.velY * b.velY
                        val specificEnergy = 0.5 * vSq - (GravitySandboxEngine.G * central.mass) / (r + 1.0e6)

                        if (specificEnergy >= 0.0 && r > 2.0e11) {
                            val count = shownConceptCount.getOrDefault("escape", 0)
                            if (count < 2) {
                                shownConceptCount["escape"] = count + 1
                                lastTeachingTimeMs = currentTimeMs
                                return GravityTeachingCatalog.MOMENT_ESCAPE
                            }
                        }
                    }
                }
            }
        }

        // 4. Detect Black Hole
        val blackHole = bodies.firstOrNull { it.bodyType == BodyType.BLACK_HOLE }
        if (blackHole != null) {
            val count = shownConceptCount.getOrDefault("black_hole", 0)
            if (count < 2) {
                shownConceptCount["black_hole"] = count + 1
                lastTeachingTimeMs = currentTimeMs
                return GravityTeachingCatalog.MOMENT_BLACK_HOLE
            }
        }

        return null
    }
}
