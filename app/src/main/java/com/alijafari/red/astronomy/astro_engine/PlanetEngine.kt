package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

object PlanetEngine {

    enum class PlanetType(
        val nameEn: String,
        val nameFa: String,
        val magnitudeBase: Double,
        val semiMajorAxisAU: Double,
        val eccentricity: Double,
        val inclinationDeg: Double,
        val meanLongitudeJ2000: Double,
        val longitudePerihelionDeg: Double,
        val longitudeNodeDeg: Double,
        val orbitalPeriodYears: Double,
        val equatorialRadiusKm: Double,
        val descriptionEn: String,
        val descriptionFa: String
    ) {
        MERCURY(
            "Mercury", "تیر (عطارد)", -0.42, 0.387098, 0.205630, 7.00487, 252.25084, 77.45645, 48.33167, 0.240846, 2439.7,
            "Smallest planet, closest to the Sun. Swiftly moves through twilight.",
            "کوچک‌ترین سیاره منظومه شمسی و نزدیک‌ترین سیاره به خورشید."
        ),
        VENUS(
            "Venus", "ناهید (زهره)", -4.40, 0.723332, 0.006773, 3.39471, 181.97973, 131.53298, 76.68069, 0.615197, 6051.8,
            "Brightest planet, often known as the Morning or Evening Star.",
            "درخشان‌ترین سیاره آسمان شب، معروف به ستاره صبحگاهی یا شامگاهی."
        ),
        MARS(
            "Mars", "بهرام (مریخ)", -1.52, 1.523679, 0.093405, 1.84973, 355.45332, 336.04084, 49.55740, 1.880848, 3396.2,
            "The Red Planet with iron-oxide surface dust and polar ice caps.",
            "سیاره سرخ با خاک حاوی اکسید آهن و کلاهک‌های یخی قطبی."
        ),
        JUPITER(
            "Jupiter", "هرمز (مشتری)", -2.70, 5.202603, 0.048498, 1.30300, 34.40438, 14.72848, 100.46444, 11.862615, 71492.0,
            "Gas giant, largest planet with Great Red Spot and 4 Galilean moons.",
            "غول گازی و بزرگ‌ترین سیاره منظومه شمسی همراه با ۴ قمر گالیله‌ای."
        ),
        SATURN(
            "Saturn", "کیوان (زحل)", 0.20, 9.554909, 0.055546, 2.48869, 49.94432, 92.43194, 113.66550, 29.447498, 60268.0,
            "Spectacular ring system visible through telescopes and Titan moon.",
            "سیاره شگفت‌انگیز با حلقه‌های یخی باشکوه و قمر عظیم تیتان."
        ),
        URANUS(
            "Uranus", "اورانوس", 5.52, 19.218446, 0.046296, 0.77319, 313.23218, 170.96424, 74.00595, 84.016846, 25559.0,
            "Ice giant with pale cyan hue, tilted on its orbital side.",
            "غول یخی متمایل به رنگ فیروزه‌ای زیبا با زاویه انحراف محوری شدید."
        ),
        NEPTUNE(
            "Neptune", "نپتون", 7.84, 30.110387, 0.009456, 1.76995, 304.88003, 44.97135, 131.78060, 164.79132, 24764.0,
            "Deep blue ice giant, outermost planet of solar system.",
            "دورترین سیاره شناخته‌شده منظومه شمسی به رنگ آبی نیلی عمیق."
        ),
        PLUTO(
            "Pluto", "پلوتو", 15.1, 39.481686, 0.248807, 17.16, 238.92881, 224.06676, 110.30347, 247.92065, 1188.3,
            "Dwarf planet in the Kuiper belt with large heart-shaped nitrogen glacier.",
            "سیاره کوتوله کمربند کایپر با یخچال نیتروژنی قلبی‌شکل معروف."
        )
    }

    data class PlanetPosition(
        val planet: PlanetType,
        val raDeg: Double,
        val decDeg: Double,
        val distanceAU: Double,
        val heliocentricDistanceAU: Double,
        val magnitude: Double,
        val phaseAngleDeg: Double,
        val illuminatedFraction: Double,
        val angularDiameterArcsec: Double
    )

    /**
     * Solves Kepler's Equation E - e*sin(E) = M using Newton-Raphson iteration.
     */
    private fun solveKepler(M: Double, e: Double): Double {
        var E = M
        for (i in 0 until 20) {
            val dE = (E - e * sin(E) - M) / (1.0 - e * cos(E))
            E -= dE
            if (abs(dE) < 1e-8) break
        }
        return E
    }

    /**
     * Calculates Geocentric Equatorial Position (RA, Dec) for a planet at Julian Ephemeris Date with Light-Time Correction.
     */
    fun calculatePlanet(planet: PlanetType, jd: Double): PlanetPosition {
        val sunPos = SunEngine.calculatePosition(jd)
        val nutation = CoordinateEngine.calculateNutation(jd)
        val epsRad = Math.toRadians(nutation.trueObliquityDeg)

        // Light time iteration
        var lightTimeDays = 0.0
        var deltaAU = planet.semiMajorAxisAU
        var rAU = planet.semiMajorAxisAU

        var raDeg = 0.0
        var decDeg = 0.0
        var phaseAngleDeg = 0.0
        var k = 1.0

        for (iter in 0..2) {
            val targetJd = jd - lightTimeDays
            val d = targetJd - 2451545.0 // Days from J2000.0

            val a = planet.semiMajorAxisAU
            val e = planet.eccentricity
            val I = Math.toRadians(planet.inclinationDeg)
            val N = Math.toRadians(planet.longitudeNodeDeg)
            val w = Math.toRadians(planet.longitudePerihelionDeg - planet.longitudeNodeDeg)
            val M_deg = (planet.meanLongitudeJ2000 - planet.longitudePerihelionDeg + 360.0 / (planet.orbitalPeriodYears * 365.25) * d) % 360.0
            val M_rad = Math.toRadians((M_deg % 360.0 + 360.0) % 360.0)

            val E_rad = solveKepler(M_rad, e)

            val x_orb = a * (cos(E_rad) - e)
            val y_orb = a * sqrt(1 - e * e) * sin(E_rad)
            rAU = sqrt(x_orb * x_orb + y_orb * y_orb)
            val v = atan2(y_orb, x_orb)

            val u = v + w
            val x_h = rAU * (cos(N) * cos(u) - sin(N) * sin(u) * cos(I))
            val y_h = rAU * (sin(N) * cos(u) + cos(N) * sin(u) * cos(I))
            val z_h = rAU * (sin(u) * sin(I))

            // Geocentric coordinates (Earth to Sun vector + Sun to Planet vector)
            val sunRad = Math.toRadians(sunPos.eclipticLongitudeDeg)
            val R_sun = sunPos.distanceAU
            val x_e = x_h + R_sun * cos(sunRad)
            val y_e = y_h + R_sun * sin(sunRad)
            val z_e = z_h

            deltaAU = sqrt(x_e * x_e + y_e * y_e + z_e * z_e)
            lightTimeDays = deltaAU * 0.0057755183 // Light time constant in days/AU

            val x_eq = x_e
            val y_eq = y_e * cos(epsRad) - z_e * sin(epsRad)
            val z_eq = y_e * sin(epsRad) + z_e * cos(epsRad)

            raDeg = Math.toDegrees(atan2(y_eq, x_eq))
            if (raDeg < 0) raDeg += 360.0
            decDeg = Math.toDegrees(asin((z_eq / deltaAU).coerceIn(-1.0, 1.0)))

            // Phase angle
            val cosi = (rAU * rAU + deltaAU * deltaAU - R_sun * R_sun) / (2.0 * rAU * deltaAU)
            phaseAngleDeg = Math.toDegrees(acos(cosi.coerceIn(-1.0, 1.0)))
            k = (1.0 + cos(Math.toRadians(phaseAngleDeg))) / 2.0
        }

        // Apparent magnitude model (Mallama & Hilton 2018)
        val i = phaseAngleDeg
        val mag = when (planet) {
            PlanetType.MERCURY -> -0.613 + 5 * log10(rAU * deltaAU) + 0.063 * i - 0.00063 * i * i + 0.00000225 * i * i * i
            PlanetType.VENUS -> -4.384 + 5 * log10(rAU * deltaAU) + 0.0009 * i + 0.000239 * i * i - 0.00000065 * i * i * i
            PlanetType.MARS -> -1.601 + 5 * log10(rAU * deltaAU) + 0.02267 * i - 0.00013 * i * i
            PlanetType.JUPITER -> -9.395 + 5 * log10(rAU * deltaAU) + 0.0005 * i + 0.000005 * i * i
            PlanetType.SATURN -> -8.914 + 5 * log10(rAU * deltaAU) + 0.018 * i // Base Saturn without ring tilt
            PlanetType.URANUS -> -7.110 + 5 * log10(rAU * deltaAU) + 0.001 * i
            PlanetType.NEPTUNE -> -6.870 + 5 * log10(rAU * deltaAU) + 0.001 * i
            PlanetType.PLUTO -> 15.1 + 5 * log10(rAU * deltaAU)
        }

        val diamArcsec = (2.0 * planet.equatorialRadiusKm / (deltaAU * 149597870.7)) * 206264.8

        return PlanetPosition(
            planet = planet,
            raDeg = raDeg,
            decDeg = decDeg,
            distanceAU = deltaAU,
            heliocentricDistanceAU = rAU,
            magnitude = mag,
            phaseAngleDeg = phaseAngleDeg,
            illuminatedFraction = k,
            angularDiameterArcsec = diamArcsec
        )
    }
}

