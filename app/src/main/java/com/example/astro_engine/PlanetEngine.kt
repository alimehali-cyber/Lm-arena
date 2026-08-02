package com.example.astro_engine

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
        val descriptionEn: String,
        val descriptionFa: String
    ) {
        MERCURY(
            "Mercury", "تیر (عطارد)", -0.4, 0.387098, 0.205630, 7.00487, 252.25084, 77.45645, 48.33167, 0.2408,
            "Smallest planet, closest to the Sun. Swiftly moves through twilight.",
            "کوچک‌ترین سیاره منظومه شمسی و نزدیک‌ترین سیاره به خورشید."
        ),
        VENUS(
            "Venus", "ناهید (زهره)", -4.4, 0.723332, 0.006773, 3.39471, 181.97973, 131.53298, 76.68069, 0.6152,
            "Brightest planet, often known as the Morning or Evening Star.",
            "درخشان‌ترین سیاره آسمان شب، معروف به ستاره صبحگاهی یا شامگاهی."
        ),
        MARS(
            "Mars", "بهرام (مریخ)", -1.5, 1.523679, 0.093405, 1.84973, 355.45332, 336.04084, 49.5574, 1.8808,
            "The Red Planet with iron-oxide surface dust and polar ice caps.",
            "سیاره سرخ با خاک حاوی اکسید آهن و کلاهک‌های یخی قطبی."
        ),
        JUPITER(
            "Jupiter", "هرمز (مشتری)", -2.7, 5.202603, 0.048498, 1.30300, 34.40438, 14.72848, 100.46444, 11.8626,
            "Gas giant, largest planet with Great Red Spot and 4 Galilean moons.",
            "غول گازی و بزرگ‌ترین سیاره منظومه شمسی همراه با ۴ قمر گالیله‌ای."
        ),
        SATURN(
            "Saturn", "کیوان (زحل)", 0.2, 9.554909, 0.055546, 2.48869, 49.94432, 92.43194, 113.6655, 29.4475,
            "Spectacular ring system visible through telescopes and Titan moon.",
            "سیاره شگفت‌انگیز با حلقه‌های یخی باشکوه و قمر عظیم تیتان."
        ),
        URANUS(
            "Uranus", "اورانوس", 5.7, 19.218446, 0.046296, 0.77319, 313.23218, 170.96424, 74.00595, 84.0168,
            "Ice giant with pale cyan hue, tilted on its orbital side.",
            "غول یخی متمایل به رنگ فیروزه‌ای زیبا با زاویه انحراف محوری شدید."
        ),
        NEPTUNE(
            "Neptune", "نپتون", 7.8, 30.110387, 0.009456, 1.76995, 304.88003, 44.97135, 131.7806, 164.7913,
            "Deep blue ice giant, outermost planet of solar system.",
            "دورترین سیاره شناخته‌شده منظومه شمسی به رنگ آبی نیلی عمیق."
        )
    }

    data class PlanetPosition(
        val planet: PlanetType,
        val raDeg: Double,
        val decDeg: Double,
        val distanceAU: Double,
        val magnitude: Double
    )

    /**
     * Solves Kepler's Equation E - e*sin(E) = M using Newton-Raphson iteration.
     */
    private fun solveKepler(M: Double, e: Double): Double {
        var E = M
        for (i in 0 until 20) {
            val dE = (E - e * sin(E) - M) / (1.0 - e * cos(E))
            E -= dE
            if (abs(dE) < 1e-7) break
        }
        return E
    }

    /**
     * Calculates Geocentric Equatorial Position (RA, Dec) for a planet at Julian Date.
     */
    fun calculatePlanet(planet: PlanetType, jd: Double): PlanetPosition {
        val d = jd - 2451545.0 // Days from J2000.0
        val T = d / 36525.0

        // Sun's heliocentric position approximation
        val sunPos = SunEngine.calculatePosition(jd)

        // Heliocentric orbital elements for planet
        val a = planet.semiMajorAxisAU
        val e = planet.eccentricity
        val I = Math.toRadians(planet.inclinationDeg)
        val N = Math.toRadians(planet.longitudeNodeDeg)
        val w = Math.toRadians(planet.longitudePerihelionDeg - planet.longitudeNodeDeg)
        val M_deg = (planet.meanLongitudeJ2000 - planet.longitudePerihelionDeg + 360.0 / (planet.orbitalPeriodYears * 365.25) * d) % 360.0
        val M_rad = Math.toRadians((M_deg % 360.0 + 360.0) % 360.0)

        // Kepler solver
        val E_rad = solveKepler(M_rad, e)

        // True anomaly v and heliocentric distance r
        val x_orb = a * (cos(E_rad) - e)
        val y_orb = a * sqrt(1 - e * e) * sin(E_rad)
        val r = sqrt(x_orb * x_orb + y_orb * y_orb)
        val v = atan2(y_orb, x_orb)

        // Heliocentric ecliptic coordinates (x_h, y_h, z_h)
        val u = v + w
        val x_h = r * (cos(N) * cos(u) - sin(N) * sin(u) * cos(I))
        val y_h = r * (sin(N) * cos(u) + cos(N) * sin(u) * cos(I))
        val z_h = r * (sin(u) * sin(I))

        // Geocentric coordinates (Earth to Sun vector + Sun to Planet vector)
        val sunRad = Math.toRadians(sunPos.eclipticLongitudeDeg)
        val x_e = x_h + cos(sunRad)
        val y_e = y_h + sin(sunRad)
        val z_e = z_h

        val deltaAU = sqrt(x_e * x_e + y_e * y_e + z_e * z_e)

        val epsRad = Math.toRadians(23.439)
        val x_eq = x_e
        val y_eq = y_e * cos(epsRad) - z_e * sin(epsRad)
        val z_eq = y_e * sin(epsRad) + z_e * cos(epsRad)

        var raDeg = Math.toDegrees(atan2(y_eq, x_eq))
        if (raDeg < 0) raDeg += 360.0
        val decDeg = Math.toDegrees(asin((z_eq / deltaAU).coerceIn(-1.0, 1.0)))

        // Magnitude adjustment by distance
        val mag = planet.magnitudeBase + 5.0 * log10(max(0.1, r * deltaAU))

        return PlanetPosition(
            planet = planet,
            raDeg = raDeg,
            decDeg = decDeg,
            distanceAU = deltaAU,
            magnitude = mag
        )
    }
}
