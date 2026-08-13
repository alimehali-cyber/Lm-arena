package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

/**
 * Planet engine that computes geocentric apparent positions.
 * Delegates to VSOP87Engine for heliocentric coordinates,
 * then applies light-time correction and geometric transformation.
 */
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
    ) {
        val distanceAu: Double get() = distanceAU
        val heliocentricDistanceAu: Double get() = heliocentricDistanceAU
    }

    private val vsop87 = VSOP87Engine()

    private const val DEG2RAD = Math.PI / 180.0
    private const val RAD2DEG = 180.0 / Math.PI

    private fun normalizeAngle(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun meanObliquity(jcTt: Double): Double {
        val t = jcTt
        return 23.43929111 - 0.0130041667 * t - 0.00000016389 * t * t
    }

    /**
     * Calculates Geocentric Equatorial Position (RA, Dec) for a planet at given AstroTime.
     */
    fun calculatePlanet(planet: PlanetType, astroTime: AstroTime): PlanetPosition {
        if (planet == PlanetType.PLUTO) {
            return calculatePlutoFallback(astroTime)
        }

        val vsop87Planet = when (planet) {
            PlanetType.MERCURY -> VSOP87Engine.Planet.MERCURY
            PlanetType.VENUS -> VSOP87Engine.Planet.VENUS
            PlanetType.MARS -> VSOP87Engine.Planet.MARS
            PlanetType.JUPITER -> VSOP87Engine.Planet.JUPITER
            PlanetType.SATURN -> VSOP87Engine.Planet.SATURN
            PlanetType.URANUS -> VSOP87Engine.Planet.URANUS
            PlanetType.NEPTUNE -> VSOP87Engine.Planet.NEPTUNE
            PlanetType.PLUTO -> VSOP87Engine.Planet.NEPTUNE
        }

        // Earth heliocentric coordinates at observation time
        val earthHelio = vsop87.calculate(VSOP87Engine.Planet.EARTH, astroTime)
        val xEarth = earthHelio.distanceAu * cos(earthHelio.latitudeDeg * DEG2RAD) * cos(earthHelio.longitudeDeg * DEG2RAD)
        val yEarth = earthHelio.distanceAu * cos(earthHelio.latitudeDeg * DEG2RAD) * sin(earthHelio.longitudeDeg * DEG2RAD)
        val zEarth = earthHelio.distanceAu * sin(earthHelio.latitudeDeg * DEG2RAD)

        // Light-time iteration
        var targetAstroTime = astroTime
        var geoDistAu = 1.0
        var xGeo = 0.0
        var yGeo = 0.0
        var zGeo = 0.0
        var currentHelio = vsop87.calculate(vsop87Planet, astroTime)

        for (iter in 0..2) {
            currentHelio = vsop87.calculate(vsop87Planet, targetAstroTime)

            val xPlanet = currentHelio.distanceAu * cos(currentHelio.latitudeDeg * DEG2RAD) * cos(currentHelio.longitudeDeg * DEG2RAD)
            val yPlanet = currentHelio.distanceAu * cos(currentHelio.latitudeDeg * DEG2RAD) * sin(currentHelio.longitudeDeg * DEG2RAD)
            val zPlanet = currentHelio.distanceAu * sin(currentHelio.latitudeDeg * DEG2RAD)

            xGeo = xPlanet - xEarth
            yGeo = yPlanet - yEarth
            zGeo = zPlanet - zEarth

            geoDistAu = sqrt(xGeo * xGeo + yGeo * yGeo + zGeo * zGeo)
            val lightTimeDays = geoDistAu * 0.0057755183 // Light-time constant in days/AU
            val targetJd = astroTime.jdTt - lightTimeDays
            targetAstroTime = AstroTime.fromJd(targetJd)
        }

        val geoLonDeg = normalizeAngle(atan2(yGeo, xGeo) * RAD2DEG)
        val geoLatDeg = asin((zGeo / geoDistAu).coerceIn(-1.0, 1.0)) * RAD2DEG

        // Obliquity of date
        val epsDeg = meanObliquity(astroTime.jcTt)
        val epsRad = epsDeg * DEG2RAD

        val lRad = geoLonDeg * DEG2RAD
        val bRad = geoLatDeg * DEG2RAD

        // Convert Ecliptic to Equatorial coordinates
        val sinDec = sin(bRad) * cos(epsRad) + cos(bRad) * sin(epsRad) * sin(lRad)
        val decRad = asin(sinDec.coerceIn(-1.0, 1.0))

        val yRa = sin(lRad) * cos(epsRad) - tan(bRad) * sin(epsRad)
        val xRa = cos(lRad)
        val raRad = atan2(yRa, xRa)

        val raDeg = normalizeAngle(raRad * RAD2DEG)
        val decDeg = decRad * RAD2DEG

        // Phase angle
        val rAU = currentHelio.distanceAu
        val R_earth = earthHelio.distanceAu
        val cosPhase = (rAU * rAU + geoDistAu * geoDistAu - R_earth * R_earth) / (2.0 * rAU * geoDistAu)
        val phaseAngleDeg = acos(cosPhase.coerceIn(-1.0, 1.0)) * RAD2DEG
        val illuminatedFraction = (1.0 + cos(phaseAngleDeg * DEG2RAD)) / 2.0

        // Apparent magnitude (Mallama & Hilton 2018)
        val i = phaseAngleDeg
        val mag = when (planet) {
            PlanetType.MERCURY -> -0.613 + 5 * log10(rAU * geoDistAu) + 0.063 * i - 0.00063 * i * i + 0.00000225 * i * i * i
            PlanetType.VENUS -> -4.384 + 5 * log10(rAU * geoDistAu) + 0.0009 * i + 0.000239 * i * i - 0.00000065 * i * i * i
            PlanetType.MARS -> -1.601 + 5 * log10(rAU * geoDistAu) + 0.02267 * i - 0.00013 * i * i
            PlanetType.JUPITER -> -9.395 + 5 * log10(rAU * geoDistAu) + 0.0005 * i + 0.000005 * i * i
            PlanetType.SATURN -> -8.914 + 5 * log10(rAU * geoDistAu) + 0.018 * i
            PlanetType.URANUS -> -7.110 + 5 * log10(rAU * geoDistAu) + 0.001 * i
            PlanetType.NEPTUNE -> -6.870 + 5 * log10(rAU * geoDistAu) + 0.001 * i
            PlanetType.PLUTO -> 15.1 + 5 * log10(rAU * geoDistAu)
        }

        val diamArcsec = (2.0 * planet.equatorialRadiusKm / (geoDistAu * 149597870.7)) * 206264.8

        return PlanetPosition(
            planet = planet,
            raDeg = raDeg,
            decDeg = decDeg,
            distanceAU = geoDistAu,
            heliocentricDistanceAU = rAU,
            magnitude = mag,
            phaseAngleDeg = phaseAngleDeg,
            illuminatedFraction = illuminatedFraction,
            angularDiameterArcsec = diamArcsec
        )
    }

    /**
     * Compatibility overload accepting raw Julian Date.
     */
    fun calculatePlanet(planet: PlanetType, jd: Double): PlanetPosition {
        return calculatePlanet(planet, AstroTime.fromJd(jd))
    }

    private fun calculatePlutoFallback(astroTime: AstroTime): PlanetPosition {
        val d = astroTime.jdTt - 2451545.0
        val raDeg = normalizeAngle(238.92881 + 0.00397 * d)
        val decDeg = -15.0
        val distAu = 34.0
        return PlanetPosition(
            planet = PlanetType.PLUTO,
            raDeg = raDeg,
            decDeg = decDeg,
            distanceAU = distAu,
            heliocentricDistanceAU = distAu,
            magnitude = 15.1,
            phaseAngleDeg = 1.0,
            illuminatedFraction = 0.99,
            angularDiameterArcsec = 0.1
        )
    }
}


