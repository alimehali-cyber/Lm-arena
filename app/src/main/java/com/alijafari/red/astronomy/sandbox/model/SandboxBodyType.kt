package com.alijafari.red.astronomy.sandbox.model

/**
 * Celestial and theoretical body types supported by the ZIG Gravity Sandbox.
 */
enum class SandboxBodyType(
    val nameEn: String,
    val nameFa: String,
    val defaultMassKg: Double,
    val defaultRadiusMeters: Double,
    val defaultColorHex: Long,
    val classification: ScientificClassification
) {
    SUN(
        nameEn = "Sun (Sol)",
        nameFa = "خورشید (مهر)",
        defaultMassKg = 1.98847e30,
        defaultRadiusMeters = 6.957e8,
        defaultColorHex = 0xFFFFD54FL,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    MERCURY(
        nameEn = "Mercury",
        nameFa = "تیر (عطارد)",
        defaultMassKg = 3.3011e23,
        defaultRadiusMeters = 2.4397e6,
        defaultColorHex = 0xFFB0BEC5L,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    VENUS(
        nameEn = "Venus",
        nameFa = "ناهید (زهره)",
        defaultMassKg = 4.8675e24,
        defaultRadiusMeters = 6.0518e6,
        defaultColorHex = 0xFFFFE082L,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    EARTH(
        nameEn = "Earth",
        nameFa = "زمین (گیتی)",
        defaultMassKg = 5.9722e24,
        defaultRadiusMeters = 6.371e6,
        defaultColorHex = 0xFF42A5F5L,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    MOON(
        nameEn = "Moon (Luna)",
        nameFa = "ماه (ماهتاب)",
        defaultMassKg = 7.342e22,
        defaultRadiusMeters = 1.7374e6,
        defaultColorHex = 0xFFCFD8DCL,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    MARS(
        nameEn = "Mars",
        nameFa = "بهرام (مریخ)",
        defaultMassKg = 6.4171e23,
        defaultRadiusMeters = 3.3895e6,
        defaultColorHex = 0xFFFF7043L,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    JUPITER(
        nameEn = "Jupiter",
        nameFa = "هرمز / برجیس (مشتری)",
        defaultMassKg = 1.89813e27,
        defaultRadiusMeters = 6.9911e7,
        defaultColorHex = 0xFFFFB74DL,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    SATURN(
        nameEn = "Saturn",
        nameFa = "کیوان (زحل)",
        defaultMassKg = 5.6834e26,
        defaultRadiusMeters = 5.8232e7,
        defaultColorHex = 0xFFFFE082L,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    URANUS(
        nameEn = "Uranus",
        nameFa = "اورانوس",
        defaultMassKg = 8.6810e25,
        defaultRadiusMeters = 2.5362e7,
        defaultColorHex = 0xFF80DEEAL,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    NEPTUNE(
        nameEn = "Neptune",
        nameFa = "نپتون",
        defaultMassKg = 1.02413e26,
        defaultRadiusMeters = 2.4622e7,
        defaultColorHex = 0xFF29B6F6L,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    BLACK_HOLE(
        nameEn = "Black Hole (Schwarzschild)",
        nameFa = "سیاه‌چاله (شوارتزشیلد)",
        defaultMassKg = 1.98847e31, // 10 Solar Masses default
        defaultRadiusMeters = 2.953e4, // Rs for 10 M_sun ~ 29.53 km
        defaultColorHex = 0xFF7C4DFFL,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    THEORETICAL_WORMHOLE(
        nameEn = "Wormhole (Einstein-Rosen / Theoretical)",
        nameFa = "کرم‌چاله (پل اینشتین-روزن / فرضیه تئوریک)",
        defaultMassKg = 5.9722e24, // Optional geometric effective mass
        defaultRadiusMeters = 5.0e6, // Throat radius
        defaultColorHex = 0xFF00E5FFL,
        classification = ScientificClassification.THEORETICAL_PHYSICS
    ),
    ASTEROID(
        nameEn = "Asteroid / Minor Body",
        nameFa = "سیارک / خرده‌سیاره",
        defaultMassKg = 9.393e20, // Ceres mass
        defaultRadiusMeters = 4.73e5, // Ceres radius
        defaultColorHex = 0xFF90A4AEL,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    ),
    CUSTOM_BODY(
        nameEn = "Custom Mass",
        nameFa = "جرم سفارشی",
        defaultMassKg = 5.9722e24,
        defaultRadiusMeters = 6.371e6,
        defaultColorHex = 0xFFE0E0E0L,
        classification = ScientificClassification.ESTABLISHED_PHYSICS
    )
}
