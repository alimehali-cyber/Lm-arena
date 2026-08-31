package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

enum class BodyType(val titleEn: String, val titleFa: String) {
    STAR("Star", "ستاره"),
    BLACK_HOLE("Black Hole", "سیاه‌چاله"),
    GAS_GIANT("Gas Giant", "غول غازی"),
    TERRESTRIAL_PLANET("Terrestrial Planet", "سیاره خاکی"),
    MOON("Moon", "قمر"),
    COMET("Comet", "دنباله‌دار"),
    ASTEROID("Asteroid", "سیارک")
}

data class CelestialBody(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    var mass: Double,             // kg
    var radius: Double,           // meters
    var posX: Double,             // meters
    var posY: Double,             // meters
    var velX: Double,             // m/s
    var velY: Double,             // m/s
    var accX: Double = 0.0,       // m/s^2
    var accY: Double = 0.0,       // m/s^2
    val colorHex: Long,           // ARGB Color
    val bodyType: BodyType,
    val isFixed: Boolean = false,
    val trailPoints: MutableList<Pair<Double, Double>> = mutableListOf()
) {
    fun deepCopy(): CelestialBody {
        return CelestialBody(
            id = id,
            nameEn = nameEn,
            nameFa = nameFa,
            mass = mass,
            radius = radius,
            posX = posX,
            posY = posY,
            velX = velX,
            velY = velY,
            accX = accX,
            accY = accY,
            colorHex = colorHex,
            bodyType = bodyType,
            isFixed = isFixed,
            trailPoints = trailPoints.map { it.copy() }.toMutableList()
        )
    }
}

data class GravitationalDiagnostics(
    val kineticEnergy: Double,
    val potentialEnergy: Double,
    val totalEnergy: Double,
    val comPosX: Double,
    val comPosY: Double,
    val comVelX: Double,
    val comVelY: Double,
    val bodyCount: Int
)

data class PhysicsLesson(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String,
    val conceptEn: String,
    val conceptFa: String,
    val formulaSymbol: String,
    val presetScenario: PresetScenario,
    val experimentStepsEn: List<String>,
    val experimentStepsFa: List<String>
)

enum class PresetScenario(val titleEn: String, val titleFa: String, val descriptionEn: String, val descriptionFa: String) {
    SOLAR_SYSTEM(
        titleEn = "Inner Solar System",
        titleFa = "منظومه شمسی داخلی",
        descriptionEn = "Sun, Mercury, Venus, Earth-Moon system, and Mars in Keplerian orbits.",
        descriptionFa = "خورشید، عطارد، زهره، سامانه زمین-ماه و مریخ در مدارهای کپلری."
    ),
    BINARY_STAR(
        titleEn = "Binary Star & Circumbinary Planet",
        titleFa = "سامانه دوستاره‌ای و سیاره دوگانه",
        descriptionEn = "Two orbiting stars with a planet in a stable wide orbit around both.",
        descriptionFa = "دو ستاره هم‌جرم در چرخش متقابل و یک سیاره در مدار بیرونی پایدار."
    ),
    FIGURE_EIGHT(
        titleEn = "Figure-8 3-Body Choreography",
        titleFa = "رقص گرانشی ۳-جرم (Figure-8)",
        descriptionEn = "Three equal masses moving indefinitely along a figure-8 loop.",
        descriptionFa = "سه جرم برابر که روی منحنی ۸ انگلیسی به طور پایدار حرکت می‌کنند."
    ),
    LAGRANGE_POINTS(
        titleEn = "Lagrange Equilibrium Points (L4 / L5)",
        titleFa = "نقاط تعادل لاگرانژی L4 و L5",
        descriptionEn = "Sun-Earth system with Trojan test bodies residing at L4 and L5 stability pockets.",
        descriptionFa = "سامانه خورشید-زمین به همراه اجرام تروجان در نقاط تعادل L4 و L5."
    ),
    BLACK_HOLE_SLINGSHOT(
        titleEn = "Black Hole Slingshot",
        titleFa = "قلاب گرانشی سیاهچاله",
        descriptionEn = "Supermassive black hole deflecting incoming comets in hyperbolic slingshot orbits.",
        descriptionFa = "سیاهچاله کلان‌جرم و منحرف کردن دنباله‌دارهای ورودی در مدارهای هذلولی."
    ),
    CHAOTIC_FOUR_BODY(
        titleEn = "4-Body Gravitational Chaos",
        titleFa = "سامانه ۴-جرم آشوبناک",
        descriptionEn = "Four equal star masses undergoing intense chaotic close encounters.",
        descriptionFa = "چهار ستاره با برهم‌کنش‌های گرانشی شدید و رفتار آشوبناک."
    ),
    EMPTY_CANVAS(
        titleEn = "Freeform Sandbox",
        titleFa = "بوم خالی",
        descriptionEn = "Empty space canvas to spawn custom stars, planets, and black holes.",
        descriptionFa = "فضای خالی برای افزودن دست‌ساز ستاره‌ها، سیارات و سیاهچاله‌ها."
    )
}

object GravitySandboxEngine {

    // Universal Gravitational Constant
    const val G: Double = 6.67430e-11

    // Softening length squared (meters^2) to prevent numerical divergence during close passes
    const val SOFTENING_SQ: Double = 1.0e12

    // Maximum trail history length per body
    const val MAX_TRAIL_POINTS = 150

    val physicsLessons = listOf(
        PhysicsLesson(
            id = "kepler_laws",
            titleEn = "Kepler's Laws of Orbital Motion",
            titleFa = "قوانین کپلر در حرکت مداری",
            subtitleEn = "Ellipses, Equal Areas & Period Harmonics",
            subtitleFa = "مدارهای بیضوی، مساحت‌های برابر و هارمونی زمان چرخش",
            conceptEn = "Planets orbit in ellipses with the central star at one focus. A line connecting the star and planet sweeps out equal areas in equal times, causing planets to accelerate near perihelion and decelerate at aphelion.",
            conceptFa = "مدار سیارات بیضوی است و خورشید در یکی از کانون‌های آن قرار دارد. شعاع حامل سیاره در زمان‌های برابر، مساحت‌های برابری را طی می‌کند، بنابراین سرعت در نزدیک‌ترین نقطه (پری‌هلیون) افزایش می‌یابد.",
            formulaSymbol = "T² = (4π² / GM) a³",
            presetScenario = PresetScenario.SOLAR_SYSTEM,
            experimentStepsEn = listOf(
                "Observe Mercury's rapid orbit compared to Mars's slower, wider orbit.",
                "Toggle Velocity Vectors to visualize kinetic acceleration near the Sun.",
                "Enable Motion Trails to inspect elliptical orbital sweeps."
            ),
            experimentStepsFa = listOf(
                "سرعت چرخش بالای عطارد را با مدار کندتر مریخ مقایسه کنید.",
                "پیکان‌های سرعت را فعال کنید تا شتاب‌گیری در نزدیکی خورشید را مشاهده کنید.",
                "ردپای مداری را روشن کنید تا شکل بیضوی مدارها آشکار شود."
            )
        ),
        PhysicsLesson(
            id = "gravity_assist",
            titleEn = "Gravitational Slingshot & Oberth Effect",
            titleFa = "مانور قلاب گرانشی و اثر اوبرت",
            subtitleEn = "Hyperbolic Deflection & Energy Transfer",
            subtitleFa = "انحراف هذلولی و انتقال انرژی گرانشی",
            conceptEn = "Spacecraft pass closely behind a massive planet or black hole to gain kinetic energy via momentum transfer, entering a high-velocity hyperbolic escape trajectory.",
            conceptFa = "فضاپیماها با عبور نزدیک از پشت یک جرم عظیم یا سیاهچاله، بخشی از تکانه گرانشی آن را دریافت کرده و به سرعت‌های فرار بسیار بالا دست می‌یابند.",
            formulaSymbol = "v_final ≈ v_in + 2 v_planet",
            presetScenario = PresetScenario.BLACK_HOLE_SLINGSHOT,
            experimentStepsEn = listOf(
                "Watch comets approach the black hole on hyperbolic paths.",
                "Observe extreme gravitational bending and velocity boosting during periapser pass.",
                "Toggle Force Vectors to see intense central acceleration arrows."
            ),
            experimentStepsFa = listOf(
                "مسیر حرکت دنباله‌دارها به سمت سیاهچاله را زیر نظر بگیرید.",
                "افزایش شدید سرعت و خمیدگی مسیر در نزدیک‌ترین فاصله را مشاهده کنید.",
                "پیکان‌های نیرو را فعال کنید تا شدت شتاب مرکزگرا مشخص شود."
            )
        ),
        PhysicsLesson(
            id = "lagrange_points",
            titleEn = "Lagrange Equilibrium Points (L4 & L5)",
            titleFa = "نقاط تعادل لاگرانژی L4 و L5",
            subtitleEn = "Orbital Resonance & Trojan Asteroid Pockets",
            subtitleFa = "پایداری گرانشی و نقاط تجمع سیارک‌های تروجان",
            conceptEn = "In a two-body system, five equilibrium points exist where centrifugal force balances gravity. L4 and L5 sit 60 degrees ahead/behind in orbit, acting as stable gravitational wells that trap Trojan asteroids.",
            conceptFa = "در یک سامانه دو جرمی، ۵ نقطه تعادل وجود دارد که نیروی گریز از مرکز با گرانش برابری می‌کند. نقاط L4 و L5 در زاویه ۶۰ درجه جلو و عقب مدار، چاه‌های گرانشی پایدار ایجاد می‌کنند.",
            formulaSymbol = "∇V_eff(r) = 0",
            presetScenario = PresetScenario.LAGRANGE_POINTS,
            experimentStepsEn = listOf(
                "Locate L4 and L5 Trojan asteroids orbiting 60° ahead and behind Earth.",
                "Observe how Trojans oscillate harmonically around these equilibrium pockets.",
                "Try spawning a small asteroid near L4 to test orbital stability."
            ),
            experimentStepsFa = listOf(
                "سیارک‌های تروجان را در زاویه ۶۰ درجه جلو و عقب زمین در نقاط L4 و L5 ببینید.",
                "نوسان هماهنگ سیارک‌ها در اطراف این نقاط تعادلی را مشاهده کنید.",
                "یک سیارک جدید در نزدیکی L4 رها کنید و پایداری آن را بیازمایید."
            )
        ),
        PhysicsLesson(
            id = "binary_barycenter",
            titleEn = "Binary Star Dynamics & Barycenter",
            titleFa = "دینامیک ستارگان دوگانه و مرکز جرم مشترک",
            subtitleEn = "Mutual Orbiting & Circumbinary Planetary Stability",
            subtitleFa = "چرخش متقابل و پایداری مداری سیارات دوخورشیدی",
            conceptEn = "Two stars of comparable mass orbit a shared center of mass (barycenter). Exoplanets can orbit outside both stars in a stable circumbinary orbit if placed beyond the critical resonance threshold.",
            conceptFa = "دو ستاره هم‌جرم به دور مرکز جرم مشترک (باری‌سنتر) می‌چرخند. سیارات می‌توانند در مدارهای بیرونی پایدار به دور هر دو ستاره چرخش کنند.",
            formulaSymbol = "m₁ r₁ = m₂ r₂",
            presetScenario = PresetScenario.BINARY_STAR,
            experimentStepsEn = listOf(
                "Turn ON 'Center of Mass' marker to pinpoint the binary barycenter.",
                "Observe the Tatooine exoplanet smoothly encircling both central stars.",
                "Inspect velocity arrows showing alternating binary star speeds."
            ),
            experimentStepsFa = listOf(
                "نشانگر 'مرکز جرم' را فعال کنید تا مرکز چرخش دو ستاره را ببینید.",
                "چرخش پایدار سیاره تاتوئین در مدار بیرونی هر دو ستاره را مشاهده کنید.",
                "پیکان‌های سرعت متناوب دو ستاره را بررسی کنید."
            )
        ),
        PhysicsLesson(
            id = "three_body_chaos",
            titleEn = "Three-Body Problem & Deterministic Chaos",
            titleFa = "مسئله سه جرم و آشوب گرانشی تعیین‌یافته",
            subtitleEn = "Poincaré Sensitivity & Non-Analytical Orbits",
            subtitleFa = "حساسیت پوانکاره و عدم وجود راه‌حل تحلیلی عمومی",
            conceptEn = "Systems with 3 or more mutually interacting masses lack general closed-form analytical solutions and exhibit extreme sensitivity to initial conditions (chaos), leading to unexpected ejections.",
            conceptFa = "سامانه‌های با ۳ جرم یا بیشتر فاقد فرمول ریاضی بسته بوده و دارای حساسیت شدید به شرایط اولیه (آشوب) هستند که منجر به پرتاب ناگهانی اجرام می‌شود.",
            formulaSymbol = "d²r_i / dt² = -G ∑ m_j r_ij / r_ij³",
            presetScenario = PresetScenario.CHAOTIC_FOUR_BODY,
            experimentStepsEn = listOf(
                "Observe unpredictable close gravitational encounters between the 4 stars.",
                "Notice how tiny perturbations cause dramatic orbit divergences over time.",
                "Enable Kinetic/Potential HUD to monitor energy conservation in chaotic states."
            ),
            experimentStepsFa = listOf(
                "برهم‌کنش‌های غیرقابل پیش‌بینی بین ۴ ستاره را مشاهده کنید.",
                "ببینید چگونه اختلالات کوچک باعث تغییرات شدید در مسیر مدارها می‌شوند.",
                "پانل HUD را روشن کنید تا بقای انرژی کل در رفتار آشوبناک را ببینید."
            )
        )
    )

    /**
     * Computes gravitational accelerations for all bodies using Newton's Law of Universal Gravitation.
     * a_i = sum_j (G * m_j * (r_j - r_i) / (r_ij^2 + epsilon^2)^(3/2))
     */
    fun computeAccelerations(bodies: List<CelestialBody>) {
        val n = bodies.size
        for (i in 0 until n) {
            bodies[i].accX = 0.0
            bodies[i].accY = 0.0
        }

        for (i in 0 until n) {
            val b1 = bodies[i]
            for (j in i + 1 until n) {
                val b2 = bodies[j]
                val dx = b2.posX - b1.posX
                val dy = b2.posY - b1.posY
                val distSq = dx * dx + dy * dy + SOFTENING_SQ
                val dist = sqrt(distSq)
                val forceFactor = G / (distSq * dist)

                val fX = forceFactor * dx
                val fY = forceFactor * dy

                if (!b1.isFixed) {
                    b1.accX += fX * b2.mass
                    b1.accY += fY * b2.mass
                }
                if (!b2.isFixed) {
                    b2.accX -= fX * b1.mass
                    b2.accY -= fY * b1.mass
                }
            }
        }
    }

    /**
     * Symplectic Velocity Verlet Integration Step.
     * 1. x(t + dt) = x(t) + v(t)*dt + 0.5 * a(t) * dt^2
     * 2. Recompute accelerations a(t + dt)
     * 3. v(t + dt) = v(t) + 0.5 * (a(t) + a(t + dt)) * dt
     */
    fun stepSimulation(
        bodies: MutableList<CelestialBody>,
        dt: Double,
        enableCollisions: Boolean = true,
        trailIntervalSubsteps: Int = 5,
        substepCount: Int = 1
    ) {
        if (bodies.isEmpty()) return

        val subDt = dt / substepCount.toDouble()

        for (sub in 0 until substepCount) {
            if (bodies.size < 2) break

            // Ensure current accelerations are calculated
            computeAccelerations(bodies)

            // Step 1: Position update & store old acceleration
            val oldAccX = DoubleArray(bodies.size)
            val oldAccY = DoubleArray(bodies.size)

            for (i in bodies.indices) {
                val b = bodies[i]
                oldAccX[i] = b.accX
                oldAccY[i] = b.accY

                if (!b.isFixed) {
                    b.posX += b.velX * subDt + 0.5 * b.accX * subDt * subDt
                    b.posY += b.velY * subDt + 0.5 * b.accY * subDt * subDt
                }
            }

            // Step 2: Calculate new accelerations at new positions
            computeAccelerations(bodies)

            // Step 3: Velocity update
            for (i in bodies.indices) {
                val b = bodies[i]
                if (!b.isFixed) {
                    b.velX += 0.5 * (oldAccX[i] + b.accX) * subDt
                    b.velY += 0.5 * (oldAccY[i] + b.accY) * subDt
                }
            }

            // Handle inelastic collisions
            if (enableCollisions) {
                handleInelasticCollisions(bodies)
            }
        }

        // Update motion trails for non-fixed bodies
        for (b in bodies) {
            b.trailPoints.add(Pair(b.posX, b.posY))
            if (b.trailPoints.size > MAX_TRAIL_POINTS) {
                b.trailPoints.removeAt(0)
            }
        }
    }

    /**
     * Processes inelastic collisions: bodies that touch merge into a combined body
     * obeying conservation of mass and linear momentum.
     */
    fun handleInelasticCollisions(bodies: MutableList<CelestialBody>) {
        var i = 0
        while (i < bodies.size) {
            var j = i + 1
            while (j < bodies.size) {
                val b1 = bodies[i]
                val b2 = bodies[j]

                val dx = b2.posX - b1.posX
                val dy = b2.posY - b1.posY
                val dist = sqrt(dx * dx + dy * dy)
                val minSeparation = (b1.radius + b2.radius) * 0.85 // Slight overlap tolerance

                if (dist < minSeparation) {
                    // Inelastic collision merge: larger mass absorbs smaller mass
                    val primary = if (b1.mass >= b2.mass) b1 else b2
                    val secondary = if (b1.mass >= b2.mass) b2 else b1

                    val newMass = primary.mass + secondary.mass
                    val newVelX = (primary.mass * primary.velX + secondary.mass * secondary.velX) / newMass
                    val newVelY = (primary.mass * primary.velY + secondary.mass * secondary.velY) / newMass

                    // Center of mass position
                    val newPosX = (primary.mass * primary.posX + secondary.mass * secondary.posX) / newMass
                    val newPosY = (primary.mass * primary.posY + secondary.mass * secondary.posY) / newMass

                    // Radius scales assuming constant density V ~ r^3
                    val newRadius = (primary.radius.pow(3) + secondary.radius.pow(3)).pow(1.0 / 3.0)

                    primary.mass = newMass
                    primary.velX = newVelX
                    primary.velY = newVelY
                    primary.posX = newPosX
                    primary.posY = newPosY
                    primary.radius = newRadius

                    // Remove secondary body
                    bodies.remove(secondary)
                    // Reset outer loop to handle potential chain reactions
                    i = -1
                    break
                }
                j++
            }
            i++
        }
    }

    /**
     * Calculates total system energy and center-of-mass motion diagnostics.
     */
    fun calculateDiagnostics(bodies: List<CelestialBody>): GravitationalDiagnostics {
        if (bodies.isEmpty()) {
            return GravitationalDiagnostics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
        }

        var kineticEnergy = 0.0
        var potentialEnergy = 0.0
        var totalMass = 0.0
        var comX = 0.0
        var comY = 0.0
        var momX = 0.0
        var momY = 0.0

        val n = bodies.size
        for (i in 0 until n) {
            val b1 = bodies[i]
            val speedSq = b1.velX * b1.velX + b1.velY * b1.velY
            kineticEnergy += 0.5 * b1.mass * speedSq

            totalMass += b1.mass
            comX += b1.mass * b1.posX
            comY += b1.mass * b1.posY
            momX += b1.mass * b1.velX
            momY += b1.mass * b1.velY

            for (j in i + 1 until n) {
                val b2 = bodies[j]
                val dx = b2.posX - b1.posX
                val dy = b2.posY - b1.posY
                val dist = sqrt(dx * dx + dy * dy + SOFTENING_SQ)
                potentialEnergy -= (G * b1.mass * b2.mass) / dist
            }
        }

        val comPosX = if (totalMass > 0) comX / totalMass else 0.0
        val comPosY = if (totalMass > 0) comY / totalMass else 0.0
        val comVelX = if (totalMass > 0) momX / totalMass else 0.0
        val comVelY = if (totalMass > 0) momY / totalMass else 0.0

        return GravitationalDiagnostics(
            kineticEnergy = kineticEnergy,
            potentialEnergy = potentialEnergy,
            totalEnergy = kineticEnergy + potentialEnergy,
            comPosX = comPosX,
            comPosY = comPosY,
            comVelX = comVelX,
            comVelY = comVelY,
            bodyCount = n
        )
    }

    /**
     * Generates predefined gravitational scenarios.
     */
    fun getPresetBodies(scenario: PresetScenario): List<CelestialBody> {
        val list = mutableListOf<CelestialBody>()

        val sunMass = 1.989e30
        val earthMass = 5.972e24

        when (scenario) {
            PresetScenario.SOLAR_SYSTEM -> {
                // Sun
                list.add(
                    CelestialBody(
                        id = "sun",
                        nameEn = "Sun",
                        nameFa = "خورشید",
                        mass = sunMass,
                        radius = 6.963e8,
                        posX = 0.0,
                        posY = 0.0,
                        velX = 0.0,
                        velY = 0.0,
                        colorHex = 0xFFFFD700, // Gold
                        bodyType = BodyType.STAR,
                        isFixed = false
                    )
                )

                // Mercury (a = 0.387 AU = 5.79e10 m, v = 47.36 km/s)
                val rMerc = 5.791e10
                val vMerc = sqrt(G * sunMass / rMerc)
                list.add(
                    CelestialBody(
                        id = "mercury",
                        nameEn = "Mercury",
                        nameFa = "عطارد",
                        mass = 3.301e23,
                        radius = 2.439e6,
                        posX = rMerc,
                        posY = 0.0,
                        velX = 0.0,
                        velY = vMerc,
                        colorHex = 0xFFA0A0A0,
                        bodyType = BodyType.TERRESTRIAL_PLANET
                    )
                )

                // Venus (a = 0.723 AU = 1.082e11 m, v = 35.02 km/s)
                val rVen = 1.082e11
                val vVen = sqrt(G * sunMass / rVen)
                list.add(
                    CelestialBody(
                        id = "venus",
                        nameEn = "Venus",
                        nameFa = "زهره",
                        mass = 4.867e24,
                        radius = 6.051e6,
                        posX = -rVen,
                        posY = 0.0,
                        velX = 0.0,
                        velY = -vVen,
                        colorHex = 0xFFE3BB76,
                        bodyType = BodyType.TERRESTRIAL_PLANET
                    )
                )

                // Earth (a = 1.0 AU = 1.496e11 m, v = 29.78 km/s)
                val rEarth = 1.496e11
                val vEarth = sqrt(G * sunMass / rEarth)
                list.add(
                    CelestialBody(
                        id = "earth",
                        nameEn = "Earth",
                        nameFa = "زمین",
                        mass = earthMass,
                        radius = 6.371e6,
                        posX = 0.0,
                        posY = rEarth,
                        velX = -vEarth,
                        velY = 0.0,
                        colorHex = 0xFF4A90E2, // Cyan Blue
                        bodyType = BodyType.TERRESTRIAL_PLANET
                    )
                )

                // Moon orbiting Earth (r_moon = 3.844e8 m, v_moon_rel = 1022 m/s)
                val rMoonRel = 3.844e8
                val vMoonRel = sqrt(G * earthMass / rMoonRel)
                list.add(
                    CelestialBody(
                        id = "moon",
                        nameEn = "Moon",
                        nameFa = "ماه",
                        mass = 7.342e22,
                        radius = 1.737e6,
                        posX = 0.0,
                        posY = rEarth + rMoonRel,
                        velX = -vEarth - vMoonRel,
                        velY = 0.0,
                        colorHex = 0xFFD0D0D0,
                        bodyType = BodyType.MOON
                    )
                )

                // Mars (a = 1.524 AU = 2.279e11 m, v = 24.07 km/s)
                val rMars = 2.279e11
                val vMars = sqrt(G * sunMass / rMars)
                list.add(
                    CelestialBody(
                        id = "mars",
                        nameEn = "Mars",
                        nameFa = "مریخ",
                        mass = 6.417e23,
                        radius = 3.389e6,
                        posX = 0.0,
                        posY = -rMars,
                        velX = vMars,
                        velY = 0.0,
                        colorHex = 0xFFE55D42, // Rust Red
                        bodyType = BodyType.TERRESTRIAL_PLANET
                    )
                )
            }

            PresetScenario.BINARY_STAR -> {
                val star1Mass = 1.0 * sunMass
                val star2Mass = 1.0 * sunMass
                val separation = 1.0e11 // 100 million km
                val r1 = separation / 2.0
                val r2 = separation / 2.0
                val orbitalSpeed = sqrt(G * star1Mass / (4.0 * r1))

                // Star A
                list.add(
                    CelestialBody(
                        id = "star_a",
                        nameEn = "Alpha Star",
                        nameFa = "ستاره آلفا",
                        mass = star1Mass,
                        radius = 7.0e8,
                        posX = -r1,
                        posY = 0.0,
                        velX = 0.0,
                        velY = -orbitalSpeed,
                        colorHex = 0xFFFF7043, // Amber
                        bodyType = BodyType.STAR
                    )
                )

                // Star B
                list.add(
                    CelestialBody(
                        id = "star_b",
                        nameEn = "Beta Star",
                        nameFa = "ستاره بتا",
                        mass = star2Mass,
                        radius = 7.0e8,
                        posX = r2,
                        posY = 0.0,
                        velX = 0.0,
                        velY = orbitalSpeed,
                        colorHex = 0xFF42A5F5, // Blue
                        bodyType = BodyType.STAR
                    )
                )

                // Circumbinary Planet at 3x separation (r_planet = 3.0e11 m)
                val rPlanet = 3.0e11
                val vPlanet = sqrt(G * (star1Mass + star2Mass) / rPlanet)
                list.add(
                    CelestialBody(
                        id = "circumbinary_planet",
                        nameEn = "Tatooine Exoplanet",
                        nameFa = "سیاره دوخورشیدی (تاتوئین)",
                        mass = 2.0 * earthMass,
                        radius = 1.2e7,
                        posX = 0.0,
                        posY = rPlanet,
                        velX = -vPlanet,
                        velY = 0.0,
                        colorHex = 0xFF66BB6A, // Green
                        bodyType = BodyType.TERRESTRIAL_PLANET
                    )
                )
            }

            PresetScenario.FIGURE_EIGHT -> {
                // Chenciner & Montgomery Figure-8 choreography initial values normalized
                val m = 1.0e30
                val scaleR = 1.0e11
                val scaleV = sqrt(G * m / scaleR)

                val x1 = -0.97000436 * scaleR
                val y1 = 0.24308753 * scaleR
                val vx1 = 0.46620531 * scaleV
                val vy1 = 0.43236573 * scaleV

                val x2 = 0.97000436 * scaleR
                val y2 = -0.24308753 * scaleR
                val vx2 = 0.46620531 * scaleV
                val vy2 = 0.43236573 * scaleV

                val x3 = 0.0
                val y3 = 0.0
                val vx3 = -2.0 * vx1
                val vy3 = -2.0 * vy1

                list.add(
                    CelestialBody(
                        id = "fig8_body1",
                        nameEn = "Star Alpha",
                        nameFa = "ستاره آلفا",
                        mass = m,
                        radius = 6.0e8,
                        posX = x1,
                        posY = y1,
                        velX = vx1,
                        velY = vy1,
                        colorHex = 0xFFFF5252,
                        bodyType = BodyType.STAR
                    )
                )

                list.add(
                    CelestialBody(
                        id = "fig8_body2",
                        nameEn = "Star Beta",
                        nameFa = "ستاره بتا",
                        mass = m,
                        radius = 6.0e8,
                        posX = x2,
                        posY = y2,
                        velX = vx2,
                        velY = vy2,
                        colorHex = 0xFF448AFF,
                        bodyType = BodyType.STAR
                    )
                )

                list.add(
                    CelestialBody(
                        id = "fig8_body3",
                        nameEn = "Star Gamma",
                        nameFa = "ستاره گاما",
                        mass = m,
                        radius = 6.0e8,
                        posX = x3,
                        posY = y3,
                        velX = vx3,
                        velY = vy3,
                        colorHex = 0xFF69F0AE,
                        bodyType = BodyType.STAR
                    )
                )
            }

            PresetScenario.LAGRANGE_POINTS -> {
                val rOrbit = 1.496e11 // 1 AU
                val vEarth = sqrt(G * sunMass / rOrbit)

                // Sun
                list.add(
                    CelestialBody(
                        id = "sun",
                        nameEn = "Sun",
                        nameFa = "خورشید",
                        mass = sunMass,
                        radius = 6.963e8,
                        posX = 0.0,
                        posY = 0.0,
                        velX = 0.0,
                        velY = 0.0,
                        colorHex = 0xFFFFD700,
                        bodyType = BodyType.STAR,
                        isFixed = true
                    )
                )

                // Earth at (rOrbit, 0)
                list.add(
                    CelestialBody(
                        id = "earth",
                        nameEn = "Earth",
                        nameFa = "زمین",
                        mass = earthMass,
                        radius = 6.371e6,
                        posX = rOrbit,
                        posY = 0.0,
                        velX = 0.0,
                        velY = vEarth,
                        colorHex = 0xFF4A90E2,
                        bodyType = BodyType.TERRESTRIAL_PLANET
                    )
                )

                // L4 Trojan (+60 degrees ahead of Earth)
                val cos60 = cos(Math.PI / 3.0)
                val sin60 = sin(Math.PI / 3.0)
                val l4X = rOrbit * cos60
                val l4Y = rOrbit * sin60
                val l4Vx = -vEarth * sin60
                val l4Vy = vEarth * cos60

                list.add(
                    CelestialBody(
                        id = "trojan_l4",
                        nameEn = "L4 Trojan Asteroid",
                        nameFa = "سیارک تروجان L4",
                        mass = 1.0e18,
                        radius = 1.0e6,
                        posX = l4X,
                        posY = l4Y,
                        velX = l4Vx,
                        velY = l4Vy,
                        colorHex = 0xFFFFAB40,
                        bodyType = BodyType.ASTEROID
                    )
                )

                // L5 Trojan (-60 degrees behind Earth)
                val l5X = rOrbit * cos60
                val l5Y = -rOrbit * sin60
                val l5Vx = vEarth * sin60
                val l5Vy = vEarth * cos60

                list.add(
                    CelestialBody(
                        id = "trojan_l5",
                        nameEn = "L5 Trojan Asteroid",
                        nameFa = "سیارک تروجان L5",
                        mass = 1.0e18,
                        radius = 1.0e6,
                        posX = l5X,
                        posY = l5Y,
                        velX = l5Vx,
                        velY = l5Vy,
                        colorHex = 0xFFE040FB,
                        bodyType = BodyType.ASTEROID
                    )
                )
            }

            PresetScenario.BLACK_HOLE_SLINGSHOT -> {
                val bhMass = 4.0e6 * sunMass // Sag A* mass

                // Central Black Hole
                list.add(
                    CelestialBody(
                        id = "black_hole",
                        nameEn = "Sagittarius A* Black Hole",
                        nameFa = "سیاهچاله کمان A*",
                        mass = bhMass,
                        radius = 1.2e10, // Schwarzschild radius representation
                        posX = 0.0,
                        posY = 0.0,
                        velX = 0.0,
                        velY = 0.0,
                        colorHex = 0xFF000000, // Black
                        bodyType = BodyType.BLACK_HOLE,
                        isFixed = true
                    )
                )

                // Incoming Comet 1
                val rStart = 2.0e12
                val vIn = 3.5e5 // 350 km/s
                list.add(
                    CelestialBody(
                        id = "comet_1",
                        nameEn = "Comet Halley Prime",
                        nameFa = "دنباله‌دار هالی آلفا",
                        mass = 1.0e15,
                        radius = 2.0e6,
                        posX = -rStart,
                        posY = 1.5e11, // Impact parameter
                        velX = vIn,
                        velY = 0.0,
                        colorHex = 0xFF80DEEA,
                        bodyType = BodyType.COMET
                    )
                )

                // Incoming Comet 2
                list.add(
                    CelestialBody(
                        id = "comet_2",
                        nameEn = "Comet Neowise Alpha",
                        nameFa = "دنباله‌دار نئووایز بتا",
                        mass = 1.0e15,
                        radius = 2.0e6,
                        posX = -rStart,
                        posY = -2.2e11,
                        velX = vIn,
                        velY = 0.0,
                        colorHex = 0xFFFFF59D,
                        bodyType = BodyType.COMET
                    )
                )
            }

            PresetScenario.CHAOTIC_FOUR_BODY -> {
                val m = 1.0e30
                val d = 1.0e11
                val v = sqrt(G * m / d) * 0.5

                list.add(
                    CelestialBody(
                        id = "c1",
                        nameEn = "Star 1",
                        nameFa = "ستاره ۱",
                        mass = m,
                        radius = 5.0e8,
                        posX = -d,
                        posY = d,
                        velX = v,
                        velY = -v,
                        colorHex = 0xFFFF1744,
                        bodyType = BodyType.STAR
                    )
                )

                list.add(
                    CelestialBody(
                        id = "c2",
                        nameEn = "Star 2",
                        nameFa = "ستاره ۲",
                        mass = m,
                        radius = 5.0e8,
                        posX = d,
                        posY = d,
                        velX = -v,
                        velY = -v,
                        colorHex = 0xFF2979FF,
                        bodyType = BodyType.STAR
                    )
                )

                list.add(
                    CelestialBody(
                        id = "c3",
                        nameEn = "Star 3",
                        nameFa = "ستاره ۳",
                        mass = m,
                        radius = 5.0e8,
                        posX = d,
                        posY = -d,
                        velX = -v,
                        velY = v,
                        colorHex = 0xFF00E676,
                        bodyType = BodyType.STAR
                    )
                )

                list.add(
                    CelestialBody(
                        id = "c4",
                        nameEn = "Star 4",
                        nameFa = "ستاره ۴",
                        mass = m,
                        radius = 5.0e8,
                        posX = -d,
                        posY = -d,
                        velX = v,
                        velY = v,
                        colorHex = 0xFFFFEA00,
                        bodyType = BodyType.STAR
                    )
                )
            }

            PresetScenario.EMPTY_CANVAS -> {
                // Empty canvas
            }
        }

        return list
    }
}
