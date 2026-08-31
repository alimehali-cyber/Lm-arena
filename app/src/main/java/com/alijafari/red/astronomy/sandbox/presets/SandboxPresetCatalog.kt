package com.alijafari.red.astronomy.sandbox.presets

import com.alijafari.red.astronomy.sandbox.model.*
import com.alijafari.red.astronomy.sandbox.physics.AstroPhysicsConstants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Built-in catalog of scientifically accurate orbital mechanics scenarios and theoretical demonstrations.
 */
object SandboxPresetCatalog {

    val SUN_ONLY = SandboxPreset(
        id = "preset_sun_solo",
        titleEn = "Isolated Sun",
        titleFa = "خورشید منفرد",
        subtitleEn = "Single Gravitational Source",
        subtitleFa = "تک‌منبع گرانشی در فضا",
        descriptionEn = "An isolated solar mass at rest at the coordinate origin.",
        descriptionFa = "خورشید در مرکز مختصات با جرم خورشیدی استاندارد و بدون تکانه اولیه.",
        recommendedBaseTimestepSeconds = 3600.0,
        recommendedTimeSpeedMultiplier = 86400.0,
        cameraTargetDistanceMeters = 3.0e11,
        bodyFactory = {
            listOf(
                SandboxBody(
                    id = "sun",
                    type = SandboxBodyType.SUN,
                    nameEn = "Sun (Sol)",
                    nameFa = "خورشید",
                    massKg = AstroPhysicsConstants.SOLAR_MASS_KG,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO
                )
            )
        }
    )

    val SUN_EARTH = SandboxPreset(
        id = "preset_sun_earth",
        titleEn = "Sun – Earth System",
        titleFa = "سامانه خورشید و زمین",
        subtitleEn = "Classic Two-Body Keplerian Orbit",
        subtitleFa = "مدار کپلری دو-جسمی کلاسیک",
        descriptionEn = "Earth orbiting the Sun in a stable circular Keplerian trajectory at 1 AU.",
        descriptionFa = "گردش مداری پایدار زمین به دور خورشید در فاصله ۱ واحد نجومی با سرعت مداری دقیق ۲۹.۷۸ کیلومتر بر ثانیه.",
        recommendedBaseTimestepSeconds = 3600.0, // 1 hour per step
        recommendedTimeSpeedMultiplier = 86400.0 * 10.0, // 10 days per sec
        cameraTargetDistanceMeters = 3.5e11,
        bodyFactory = {
            val rAu = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS
            val mSun = AstroPhysicsConstants.SOLAR_MASS_KG
            val mEarth = AstroPhysicsConstants.EARTH_MASS_KG
            val totalM = mSun + mEarth

            val vOrbital = sqrt(AstroPhysicsConstants.G * totalM / rAu)

            // Barycentric placement
            val rSun = (mEarth / totalM) * rAu
            val rEarth = (mSun / totalM) * rAu
            val vSun = (mEarth / totalM) * vOrbital
            val vEarth = (mSun / totalM) * vOrbital

            listOf(
                SandboxBody(
                    id = "sun",
                    type = SandboxBodyType.SUN,
                    nameEn = "Sun",
                    nameFa = "خورشید",
                    massKg = mSun,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D(-rSun, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, -vSun)
                ),
                SandboxBody(
                    id = "earth",
                    type = SandboxBodyType.EARTH,
                    nameEn = "Earth",
                    nameFa = "زمین",
                    massKg = mEarth,
                    radiusMeters = AstroPhysicsConstants.EARTH_RADIUS_METERS,
                    position = Vector3D(rEarth, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, vEarth)
                )
            )
        }
    )

    val EARTH_MOON = SandboxPreset(
        id = "preset_earth_moon",
        titleEn = "Earth – Moon System",
        titleFa = "سامانه زمین و ماه",
        subtitleEn = "Barycentric Binary Orbit",
        subtitleFa = "مدار گرانیگاهی زمین و ماه",
        descriptionEn = "Moon orbiting Earth around their common center of mass at 384,400 km.",
        descriptionFa = "چرخش دقیق ماه به دور مرکز جرم مشترک با زمین در فاصله ۳۸۴,۴۰۰ کیلومتری.",
        recommendedBaseTimestepSeconds = 300.0, // 5 min
        recommendedTimeSpeedMultiplier = 86400.0 * 2.0, // 2 days per sec
        cameraTargetDistanceMeters = 8.0e8,
        bodyFactory = {
            val d = 3.844e8 // meters
            val mEarth = AstroPhysicsConstants.EARTH_MASS_KG
            val mMoon = AstroPhysicsConstants.MOON_MASS_KG
            val totalM = mEarth + mMoon

            val vRel = sqrt(AstroPhysicsConstants.G * totalM / d)
            val rEarth = (mMoon / totalM) * d
            val rMoon = (mEarth / totalM) * d
            val vEarth = (mMoon / totalM) * vRel
            val vMoon = (mEarth / totalM) * vRel

            listOf(
                SandboxBody(
                    id = "earth",
                    type = SandboxBodyType.EARTH,
                    nameEn = "Earth",
                    nameFa = "زمین",
                    massKg = mEarth,
                    radiusMeters = AstroPhysicsConstants.EARTH_RADIUS_METERS,
                    position = Vector3D(-rEarth, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, -vEarth)
                ),
                SandboxBody(
                    id = "moon",
                    type = SandboxBodyType.MOON,
                    nameEn = "Moon",
                    nameFa = "ماه",
                    massKg = mMoon,
                    radiusMeters = AstroPhysicsConstants.MOON_RADIUS_METERS,
                    position = Vector3D(rMoon, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, vMoon)
                )
            )
        }
    )

    val SUN_EARTH_MOON = SandboxPreset(
        id = "preset_sun_earth_moon",
        titleEn = "Sun – Earth – Moon System",
        titleFa = "سامانه خورشید، زمین و ماه",
        subtitleEn = "Hierarchical Three-Body System",
        subtitleFa = "سامانه سلسله‌مراتبی سه‌جسمی",
        descriptionEn = "Hierarchical three-body system demonstrating lunar orbital perturbations as Earth orbits the Sun.",
        descriptionFa = "سامانه سه‌جسمی واقعی شامل اختلالات گرانشی خورشید بر مدار ماه در حین گردش زمین.",
        recommendedBaseTimestepSeconds = 300.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 2.0,
        cameraTargetDistanceMeters = 3.5e11,
        bodyFactory = {
            val rAu = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS
            val mSun = AstroPhysicsConstants.SOLAR_MASS_KG
            val mEarth = AstroPhysicsConstants.EARTH_MASS_KG
            val mMoon = AstroPhysicsConstants.MOON_MASS_KG

            val vEarthOrbital = sqrt(AstroPhysicsConstants.G * mSun / rAu)
            val dMoon = 3.844e8
            val vMoonRel = sqrt(AstroPhysicsConstants.G * mEarth / dMoon)

            listOf(
                SandboxBody(
                    id = "sun",
                    type = SandboxBodyType.SUN,
                    nameEn = "Sun",
                    nameFa = "خورشید",
                    massKg = mSun,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO
                ),
                SandboxBody(
                    id = "earth",
                    type = SandboxBodyType.EARTH,
                    nameEn = "Earth",
                    nameFa = "زمین",
                    massKg = mEarth,
                    radiusMeters = AstroPhysicsConstants.EARTH_RADIUS_METERS,
                    position = Vector3D(rAu, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, vEarthOrbital)
                ),
                SandboxBody(
                    id = "moon",
                    type = SandboxBodyType.MOON,
                    nameEn = "Moon",
                    nameFa = "ماه",
                    massKg = mMoon,
                    radiusMeters = AstroPhysicsConstants.MOON_RADIUS_METERS,
                    position = Vector3D(rAu + dMoon, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, vEarthOrbital + vMoonRel)
                )
            )
        }
    )

    val FULL_SOLAR_SYSTEM = SandboxPreset(
        id = "preset_full_solar_system",
        titleEn = "Complete Solar System",
        titleFa = "منظومه کامل خورشیدی",
        subtitleEn = "Sun + 8 Major Planets",
        subtitleFa = "خورشید به همراه ۸ سیاره اصلی",
        descriptionEn = "Accurate pairwise Newtonian simulation of the Sun and all 8 major planets from Mercury to Neptune.",
        descriptionFa = "شبیه‌سازی دقیق برهم‌کنش گرانشی خورشید و هر ۸ سیاره منظومه شمسی از تیر تا نپتون.",
        recommendedBaseTimestepSeconds = 3600.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 30.0,
        cameraTargetDistanceMeters = 9.0e12,
        bodyFactory = {
            val mSun = AstroPhysicsConstants.SOLAR_MASS_KG
            val au = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS

            data class PlanetData(
                val id: String,
                val type: SandboxBodyType,
                val nameEn: String,
                val nameFa: String,
                val mass: Double,
                val radius: Double,
                val distAu: Double,
                val angleRad: Double
            )

            val planets = listOf(
                PlanetData("mercury", SandboxBodyType.MERCURY, "Mercury", "تیر", 3.3011e23, 2.4397e6, 0.387, 0.2),
                PlanetData("venus", SandboxBodyType.VENUS, "Venus", "ناهید", 4.8675e24, 6.0518e6, 0.723, 1.1),
                PlanetData("earth", SandboxBodyType.EARTH, "Earth", "زمین", 5.9722e24, 6.371e6, 1.000, 2.3),
                PlanetData("mars", SandboxBodyType.MARS, "Mars", "بهرام", 6.4171e23, 3.3895e6, 1.524, 3.5),
                PlanetData("jupiter", SandboxBodyType.JUPITER, "Jupiter", "هرمز", 1.89813e27, 6.9911e7, 5.204, 4.2),
                PlanetData("saturn", SandboxBodyType.SATURN, "Saturn", "کیوان", 5.6834e26, 5.8232e7, 9.582, 0.8),
                PlanetData("uranus", SandboxBodyType.URANUS, "Uranus", "اورانوس", 8.6810e25, 2.5362e7, 19.20, 5.1),
                PlanetData("neptune", SandboxBodyType.NEPTUNE, "Neptune", "نپتون", 1.02413e26, 2.4622e7, 30.05, 2.9)
            )

            val list = ArrayList<SandboxBody>()
            list.add(
                SandboxBody(
                    id = "sun",
                    type = SandboxBodyType.SUN,
                    nameEn = "Sun",
                    nameFa = "خورشید",
                    massKg = mSun,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO
                )
            )

            for (p in planets) {
                val distMeters = p.distAu * au
                val vMag = sqrt(AstroPhysicsConstants.G * mSun / distMeters)
                val cosA = cos(p.angleRad)
                val sinA = sin(p.angleRad)

                val posX = distMeters * cosA
                val posZ = distMeters * sinA
                val velX = -vMag * sinA
                val velZ = vMag * cosA

                list.add(
                    SandboxBody(
                        id = p.id,
                        type = p.type,
                        nameEn = p.nameEn,
                        nameFa = p.nameFa,
                        massKg = p.mass,
                        radiusMeters = p.radius,
                        position = Vector3D(posX, 0.0, posZ),
                        velocity = Vector3D(velX, 0.0, velZ)
                    )
                )
            }

            list
        }
    )

    val BINARY_STARS = SandboxPreset(
        id = "preset_binary_stars",
        titleEn = "Binary Star System",
        titleFa = "سامانه ستاره دوگانه",
        subtitleEn = "Equal-Mass Stellar Pair",
        subtitleFa = "جفت ستاره‌ای هم‌جرم در چرخش متقابل",
        descriptionEn = "Two solar-mass stars revolving around a shared central barycenter separated by 2 AU.",
        descriptionFa = "دو ستاره هم‌جرم با جرم خورشیدی در فاصله ۲ واحد نجومی که به دور مرکز ثقل مشترک می‌چرخند.",
        recommendedBaseTimestepSeconds = 3600.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 15.0,
        cameraTargetDistanceMeters = 6.0e11,
        bodyFactory = {
            val m = AstroPhysicsConstants.SOLAR_MASS_KG
            val rAu = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS
            val sep = 2.0 * rAu
            val vOrb = sqrt(AstroPhysicsConstants.G * m / (4.0 * rAu))

            listOf(
                SandboxBody(
                    id = "star_a",
                    type = SandboxBodyType.SUN,
                    nameEn = "Star Alpha",
                    nameFa = "ستاره آلفا",
                    massKg = m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D(-rAu, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, -vOrb),
                    colorHex = 0xFFFFD54FL
                ),
                SandboxBody(
                    id = "star_b",
                    type = SandboxBodyType.SUN,
                    nameEn = "Star Beta",
                    nameFa = "ستاره بتا",
                    massKg = m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D(rAu, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, vOrb),
                    colorHex = 0xFFFF8A65L
                )
            )
        }
    )

    val FIGURE_EIGHT_THREE_BODY = SandboxPreset(
        id = "preset_figure_eight",
        titleEn = "Figure-Eight 3-Body Choreography",
        titleFa = "مدار رقص هشت‌لاتین سه‌جسمی",
        subtitleEn = "Chenciner-Montgomery Solution",
        subtitleFa = "راه حل تناوبی چنسینر-مونتگومری",
        descriptionEn = "A miraculous periodic zero-angular-momentum choreography of three equal masses traversing a single figure-eight curve.",
        descriptionFa = "شبیه‌سازی مدار تناوبی شگفت‌انگیز سه جرم برابر که در یک مسیر ۸ شکل پیوسته بدون برخورد یکدیگر را دنبال می‌کنند.",
        recommendedBaseTimestepSeconds = 3600.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 5.0,
        cameraTargetDistanceMeters = 5.0e11,
        bodyFactory = {
            val m = AstroPhysicsConstants.SOLAR_MASS_KG
            val scaleR = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS
            val scaleV = sqrt(AstroPhysicsConstants.G * m / scaleR)

            val x1 = -0.97000436 * scaleR
            val z1 = 0.24308753 * scaleR

            val vx3 = -2.0 * (-0.46620531) * scaleV
            val vz3 = -2.0 * (-0.43236573) * scaleV

            val vx1 = -0.46620531 * scaleV
            val vz1 = -0.43236573 * scaleV

            listOf(
                SandboxBody(
                    id = "body_1",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Body 1 (Alpha)",
                    nameFa = "جرم ۱ (آلفا)",
                    massKg = m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS * 0.5,
                    position = Vector3D(x1, 0.0, z1),
                    velocity = Vector3D(vx1, 0.0, vz1),
                    colorHex = 0xFF42A5F5L
                ),
                SandboxBody(
                    id = "body_2",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Body 2 (Beta)",
                    nameFa = "جرم ۲ (بتا)",
                    massKg = m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS * 0.5,
                    position = Vector3D(-x1, 0.0, -z1),
                    velocity = Vector3D(vx1, 0.0, vz1),
                    colorHex = 0xFFFF7043L
                ),
                SandboxBody(
                    id = "body_3",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Body 3 (Gamma)",
                    nameFa = "جرم ۳ (گاما)",
                    massKg = m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS * 0.5,
                    position = Vector3D(0.0, 0.0, 0.0),
                    velocity = Vector3D(vx3, 0.0, vz3),
                    colorHex = 0xFF66BB6AL
                )
            )
        }
    )

    val CHAOTIC_THREE_BODY = SandboxPreset(
        id = "preset_chaotic_three_body",
        titleEn = "Chaotic 3-Body (Burrau's Problem)",
        titleFa = "مسئله آشوبناک سه‌جسمی (بوراو)",
        subtitleEn = "Gravitational Slingshots & Chaos",
        subtitleFa = "قلاب‌سنگ گرانشی و عدم قطعیت آشوب",
        descriptionEn = "Three stationary bodies forming a Pythagorean triangle rapidly evolve into violent close encounters and gravitational ejection.",
        descriptionFa = "سه جرم با فواصل مثلث قائم‌الزاویه که برهم‌کنش آشوبناک و پرتاب قلاب‌سنگی گرانشی را به نمایش می‌گذارند.",
        recommendedBaseTimestepSeconds = 1800.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 3.0,
        cameraTargetDistanceMeters = 8.0e11,
        bodyFactory = {
            val m = AstroPhysicsConstants.SOLAR_MASS_KG
            val au = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS

            listOf(
                SandboxBody(
                    id = "body_3m",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Mass 3 (3 M☉)",
                    nameFa = "جرم ۳ (۳ برابر خورشید)",
                    massKg = 3.0 * m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D(1.0 * au, 0.0, 3.0 * au),
                    velocity = Vector3D.ZERO,
                    colorHex = 0xFFFFCA28L
                ),
                SandboxBody(
                    id = "body_4m",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Mass 4 (4 M☉)",
                    nameFa = "جرم ۴ (۴ برابر خورشید)",
                    massKg = 4.0 * m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS * 1.2,
                    position = Vector3D(-2.0 * au, 0.0, -1.0 * au),
                    velocity = Vector3D.ZERO,
                    colorHex = 0xFFAB47BCL
                ),
                SandboxBody(
                    id = "body_5m",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Mass 5 (5 M☉)",
                    nameFa = "جرم ۵ (۵ برابر خورشید)",
                    massKg = 5.0 * m,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS * 1.4,
                    position = Vector3D(1.0 * au, 0.0, -1.0 * au),
                    velocity = Vector3D.ZERO,
                    colorHex = 0xFF26A69AL
                )
            )
        }
    )

    val LAGRANGE_POINTS_TROJAN = SandboxPreset(
        id = "preset_lagrange_trojan",
        titleEn = "Lagrange Points & Trojan Asteroid",
        titleFa = "نقاط لاگرانژی و سیارک تروجان",
        subtitleEn = "Orbital Resonance & L4 Stability",
        subtitleFa = "پایداری گرانشی در نقطه L4",
        descriptionEn = "A Trojan test asteroid placed at the L4 Lagrange point (60° ahead of Earth) in stable orbital equilibrium.",
        descriptionFa = "سیارک تروجان مستقر در نقطه لاگرانژی L4 (۶۰ درجه جلوتر از زمین) که در تعادل گرانشی پایدار گردش می‌کند.",
        recommendedBaseTimestepSeconds = 3600.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 10.0,
        cameraTargetDistanceMeters = 3.5e11,
        bodyFactory = {
            val rAu = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS
            val mSun = AstroPhysicsConstants.SOLAR_MASS_KG
            val mEarth = AstroPhysicsConstants.EARTH_MASS_KG
            val vOrb = sqrt(AstroPhysicsConstants.G * mSun / rAu)

            // L4 is located at 60 degrees ahead of Earth on XZ plane
            val angleL4 = Math.PI / 3.0 // 60 deg
            val posL4X = rAu * cos(angleL4)
            val posL4Z = rAu * sin(angleL4)
            val velL4X = -vOrb * sin(angleL4)
            val velL4Z = vOrb * cos(angleL4)

            listOf(
                SandboxBody(
                    id = "sun",
                    type = SandboxBodyType.SUN,
                    nameEn = "Sun",
                    nameFa = "خورشید",
                    massKg = mSun,
                    radiusMeters = AstroPhysicsConstants.SOLAR_RADIUS_METERS,
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO
                ),
                SandboxBody(
                    id = "earth",
                    type = SandboxBodyType.EARTH,
                    nameEn = "Earth",
                    nameFa = "زمین",
                    massKg = mEarth,
                    radiusMeters = AstroPhysicsConstants.EARTH_RADIUS_METERS,
                    position = Vector3D(rAu, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, vOrb)
                ),
                SandboxBody(
                    id = "trojan_asteroid",
                    type = SandboxBodyType.ASTEROID,
                    nameEn = "Trojan Asteroid (L4)",
                    nameFa = "سیارک تروجان (نقطه L4)",
                    massKg = 9.393e20, // Ceres mass
                    radiusMeters = 4.73e5,
                    position = Vector3D(posL4X, 0.0, posL4Z),
                    velocity = Vector3D(velL4X, 0.0, velL4Z),
                    colorHex = 0xFFFFEE58L
                )
            )
        }
    )

    val ORBITAL_VS_ESCAPE_VELOCITY = SandboxPreset(
        id = "preset_orbital_escape_vel",
        titleEn = "Orbital vs Escape Velocity",
        titleFa = "سرعت مداری در برابر سرعت فرار",
        subtitleEn = "Bound vs Parabolic Trajectories",
        subtitleFa = "مدار بسته دایروی در برابر مدار باز سهموی",
        descriptionEn = "Demonstrates circular orbital speed (v = √(GM/r)) versus parabolic escape speed (v = √(2GM/r)).",
        descriptionFa = "مقایسه زنده سرعت مداری دایروی (۷.۵۵ کیلومتر بر ثانیه) و سرعت فرار از میدان جاذبه (۱۰.۶۸ کیلومتر بر ثانیه).",
        recommendedBaseTimestepSeconds = 1.0,
        recommendedTimeSpeedMultiplier = 60.0, // 1 min per sec
        cameraTargetDistanceMeters = 5.0e7,
        bodyFactory = {
            val mEarth = AstroPhysicsConstants.EARTH_MASS_KG
            val rOrbit = 7.0e6 // 7000 km from center (~630 km altitude)
            val vCirc = sqrt(AstroPhysicsConstants.G * mEarth / rOrbit)
            val vEsc = sqrt(2.0) * vCirc

            listOf(
                SandboxBody(
                    id = "earth",
                    type = SandboxBodyType.EARTH,
                    nameEn = "Earth",
                    nameFa = "زمین",
                    massKg = mEarth,
                    radiusMeters = AstroPhysicsConstants.EARTH_RADIUS_METERS,
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO,
                    isFixed = true
                ),
                SandboxBody(
                    id = "sat_circular",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Satellite A (Circular: v_circ)",
                    nameFa = "ماهواره ۱ (مدار دایروی)",
                    massKg = 1000.0,
                    radiusMeters = 1.0e5,
                    position = Vector3D(rOrbit, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, vCirc),
                    colorHex = 0xFF42A5F5L
                ),
                SandboxBody(
                    id = "sat_escape",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Probe B (Escape: v_esc)",
                    nameFa = "کاوشگر ۲ (سرعت فرار سهموی)",
                    massKg = 1000.0,
                    radiusMeters = 1.0e5,
                    position = Vector3D(0.0, 0.0, rOrbit),
                    velocity = Vector3D(-vEsc, 0.0, 0.0),
                    colorHex = 0xFFFF5252L
                )
            )
        }
    )

    val BLACK_HOLE_ACCRETION_DEMO = SandboxPreset(
        id = "preset_black_hole_capture",
        titleEn = "Black Hole Infall & Capture",
        titleFa = "فروریزش و به دام افتادن در سیاه‌چاله",
        subtitleEn = "Extreme Gravitational Attraction",
        subtitleFa = "میدان گرانشی شدید و بلعیدن اجرام",
        descriptionEn = "A 10-solar-mass black hole attracting incoming planetoids and demonstrating gravitational capture and accretion.",
        descriptionFa = "سیاه‌چاله ۱۰ جرم خورشیدی که اجرام عبوری را با انحنای شدید مسیر و جذب تکانه جذب می‌کند.",
        recommendedBaseTimestepSeconds = 600.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 2.0,
        cameraTargetDistanceMeters = 6.0e11,
        bodyFactory = {
            val mBh = 10.0 * AstroPhysicsConstants.SOLAR_MASS_KG
            val au = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS

            listOf(
                SandboxBody(
                    id = "black_hole",
                    type = SandboxBodyType.BLACK_HOLE,
                    nameEn = "Black Hole (10 M☉)",
                    nameFa = "سیاه‌چاله شوارتزشیلد (۱۰ جرم خورشیدی)",
                    massKg = mBh,
                    radiusMeters = 2.953e4, // Rs ~ 29.5 km
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO,
                    collisionPolicy = CollisionPolicy.BLACK_HOLE_ACCEDE
                ),
                SandboxBody(
                    id = "planetoid_1",
                    type = SandboxBodyType.ASTEROID,
                    nameEn = "Infalling Planetoid Alpha",
                    nameFa = "خرده‌سیاره ورودی آلفا",
                    massKg = 5.0e23,
                    radiusMeters = 1.5e6,
                    position = Vector3D(2.0 * au, 0.0, 0.5 * au),
                    velocity = Vector3D(-25000.0, 0.0, 10000.0),
                    colorHex = 0xFFFFB74DL
                ),
                SandboxBody(
                    id = "planetoid_2",
                    type = SandboxBodyType.MARS,
                    nameEn = "Orbiting Body Beta",
                    nameFa = "سیاره مداری بتا",
                    massKg = 6.4171e23,
                    radiusMeters = 3.3895e6,
                    position = Vector3D(0.0, 0.0, 1.5 * au),
                    velocity = Vector3D(sqrt(AstroPhysicsConstants.G * mBh / (1.5 * au)), 0.0, 0.0),
                    colorHex = 0xFFFF7043L
                )
            )
        }
    )

    val THEORETICAL_WORMHOLE_SYSTEM = SandboxPreset(
        id = "preset_wormhole_system",
        titleEn = "Theoretical Wormhole Throat",
        titleFa = "مدل فرضی دهانه کرم‌چاله",
        subtitleEn = "Hypothetical Spatial Topology",
        subtitleFa = "توپولوژی هندسی فرضی فضا-زمان",
        descriptionEn = "A theoretical Morris-Thorne / Einstein-Rosen bridge (clearly classified as speculative physics) demonstrating hypothetical gravitational field interaction.",
        descriptionFa = "شبیه‌سازی فرضی پل اینشتین-روزن (طبقه‌بندی شده به عنوان فرضیه تئوریک اثبات‌نشده) و برهم‌کنش میدان جاذبه فرضی.",
        classification = ScientificClassification.THEORETICAL_PHYSICS,
        recommendedBaseTimestepSeconds = 3600.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 5.0,
        cameraTargetDistanceMeters = 4.0e11,
        bodyFactory = {
            val mSun = AstroPhysicsConstants.SOLAR_MASS_KG
            val au = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS

            listOf(
                SandboxBody(
                    id = "wormhole_throat",
                    type = SandboxBodyType.THEORETICAL_WORMHOLE,
                    nameEn = "Wormhole Mouth A (Theoretical)",
                    nameFa = "دهانه کرم‌چاله الف (فرضی)",
                    massKg = mSun * 0.5,
                    radiusMeters = 5.0e8,
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO,
                    collisionPolicy = CollisionPolicy.PASS_THROUGH,
                    theoreticalMetadata = mapOf(
                        "throat_diameter_km" to "1000000",
                        "metric_type" to "Morris-Thorne Traversible",
                        "status" to "Theoretical Speculation"
                    )
                ),
                SandboxBody(
                    id = "test_probe",
                    type = SandboxBodyType.CUSTOM_BODY,
                    nameEn = "Test Probe",
                    nameFa = "کاوشگر آزمایشی",
                    massKg = 10000.0,
                    radiusMeters = 1.0e6,
                    position = Vector3D(1.2 * au, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, sqrt(AstroPhysicsConstants.G * (mSun * 0.5) / (1.2 * au))),
                    colorHex = 0xFF00E5FFL
                )
            )
        }
    )

    val KERR_ROTATING_BLACK_HOLE = SandboxPreset(
        id = "preset_kerr_black_hole",
        titleEn = "Kerr Rotating Black Hole (Gargantua)",
        titleFa = "سیاه‌چاله چرخان کِر (گارگنتوا)",
        subtitleEn = "Frame Dragging & Ergosphere (a/M = 0.94)",
        subtitleFa = "کشش چارچوب و قرص برافزایشی با چرخش شدید",
        descriptionEn = "An extreme Kerr rotating black hole featuring frame-dragging, asymmetric shadow distortion, relativistic Doppler beaming, and a brilliant multi-order accretion disk.",
        descriptionFa = "سیاه‌چاله چرخان کِر با چرخش فضا-زمان، اعوجاج سایه افق رویداد، تقویت دوپلر نسبیتی و قرص درخشان برافزایشی با دمای بالا.",
        recommendedBaseTimestepSeconds = 300.0,
        recommendedTimeSpeedMultiplier = 86400.0 * 1.5,
        cameraTargetDistanceMeters = 5.0e11,
        bodyFactory = {
            val mBh = 15.0 * AstroPhysicsConstants.SOLAR_MASS_KG
            val au = AstroPhysicsConstants.ASTRONOMICAL_UNIT_METERS

            listOf(
                SandboxBody(
                    id = "kerr_black_hole",
                    type = SandboxBodyType.BLACK_HOLE,
                    nameEn = "Kerr Black Hole (a = 0.94)",
                    nameFa = "سیاه‌چاله چرخان کِر",
                    massKg = mBh,
                    radiusMeters = 4.43e4,
                    position = Vector3D.ZERO,
                    velocity = Vector3D.ZERO,
                    collisionPolicy = CollisionPolicy.BLACK_HOLE_ACCEDE,
                    theoreticalMetadata = mapOf(
                        "spin_parameter_a" to "0.94",
                        "metric" to "Kerr Spacetime",
                        "ergosphere_r_theta" to "M + sqrt(M^2 - a^2 cos^2 theta)",
                        "isco_radius" to "1.5 M"
                    )
                ),
                SandboxBody(
                    id = "accretion_feeder",
                    type = SandboxBodyType.ASTEROID,
                    nameEn = "Accretion Prograde Feeder",
                    nameFa = "جرم تزریق‌کننده ماده مستقیم‌گرد",
                    massKg = 2.0e24,
                    radiusMeters = 2.0e6,
                    position = Vector3D(1.8 * au, 0.0, 0.0),
                    velocity = Vector3D(0.0, 0.0, sqrt(AstroPhysicsConstants.G * mBh / (1.8 * au))),
                    colorHex = 0xFFFFAB40L
                )
            )
        }
    )

    val ALL_PRESETS = listOf(
        SUN_ONLY,
        SUN_EARTH,
        EARTH_MOON,
        SUN_EARTH_MOON,
        FULL_SOLAR_SYSTEM,
        BINARY_STARS,
        FIGURE_EIGHT_THREE_BODY,
        CHAOTIC_THREE_BODY,
        LAGRANGE_POINTS_TROJAN,
        ORBITAL_VS_ESCAPE_VELOCITY,
        BLACK_HOLE_ACCRETION_DEMO,
        KERR_ROTATING_BLACK_HOLE,
        THEORETICAL_WORMHOLE_SYSTEM
    )

    fun getById(id: String): SandboxPreset? {
        return ALL_PRESETS.firstOrNull { it.id == id }
    }
}
