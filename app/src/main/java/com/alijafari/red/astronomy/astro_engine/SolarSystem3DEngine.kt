package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

object SolarSystem3DEngine {

    enum class WalkingScale(
        val nameEn: String,
        val nameFa: String,
        val kmPerStep: Double,
        val labelEn: String,
        val labelFa: String
    ) {
        ONE_MILLION_KM("1M km / step", "۱ میلیون کیلومتر / گام", 1_000_000.0, "1 step = 1,000,000 km", "هر گام = ۱ میلیون کیلومتر"),
        FIVE_MILLION_KM("5M km / step", "۵ میلیون کیلومتر / گام", 5_000_000.0, "1 step = 5,000,000 km", "هر گام = ۵ میلیون کیلومتر"),
        TEN_MILLION_KM("10M km / step", "۱۰ میلیون کیلومتر / گام", 10_000_000.0, "1 step = 10,000,000 km", "هر گام = ۱۰ میلیون کیلومتر"),
        HUNDRED_MILLION_KM("100M km / step", "۱۰۰ میلیون کیلومتر / گام", 100_000_000.0, "1 step = 100,000,000 km", "هر گام = ۱۰۰ میلیون کیلومتر"),
        ONE_AU("1 AU / step", "۱ واحد نجومی / گام", 149_597_870.7, "1 step = 1 AU (~150M km)", "هر گام = ۱ واحد نجومی (۱۵۰M km)"),
        CUSTOM_SCALE("Custom", "سفارشی", 25_000_000.0, "1 step = 25,000,000 km", "هر گام = ۲۵ میلیون کیلومتر")
    }

    data class Vector3D(
        val x: Double,
        val y: Double,
        val z: Double
    ) {
        fun length(): Double = sqrt(x * x + y * y + z * z)
        fun distanceTo(other: Vector3D): Double = sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y) + (z - other.z) * (z - other.z))
    }

    data class Planet3DData(
        val planet: PlanetEngine.PlanetType,
        val heliocentricPosKm: Vector3D,
        val geocentricPosKm: Vector3D,
        val heliocentricDistanceAU: Double,
        val geocentricDistanceAU: Double,
        val orbitalSpeedKmS: Double,
        val trueAnomalyDeg: Double,
        val illuminatedFraction: Double,
        val phaseAngleDeg: Double,
        val scaledPositionMeters: Vector3D, // AR meters relative to Sun origin at (0,0,0)
        val radiusKm: Double,
        val displayRadiusMeters: Double
    )

    data class JovianMoon3DData(
        val moon: JupiterMoonsEngine.GalileanMoon,
        val offsetFromJupiterKm: Vector3D,
        val scaledPositionMeters: Vector3D,
        val phenomenon: JupiterMoonsEngine.MoonPhenomenon
    )

    private const val G_SUN = 1.32712440018e11 // GM_sun in km^3 / s^2
    private const val AU_IN_KM = 149_597_870.7

    /**
     * Calculates heliocentric 3D Cartesian coordinates (X, Y, Z in km) for a planet at Julian Date.
     */
    fun calculateHeliocentricPositionKm(planet: PlanetEngine.PlanetType, jd: Double): Vector3D {
        val d = jd - 2451545.0 // Days from J2000.0
        val a = planet.semiMajorAxisAU * AU_IN_KM
        val e = planet.eccentricity
        val I = Math.toRadians(planet.inclinationDeg)
        val N = Math.toRadians(planet.longitudeNodeDeg)
        val w = Math.toRadians(planet.longitudePerihelionDeg - planet.longitudeNodeDeg)
        val M_deg = (planet.meanLongitudeJ2000 - planet.longitudePerihelionDeg + 360.0 / (planet.orbitalPeriodYears * 365.25) * d) % 360.0
        val M_rad = Math.toRadians((M_deg % 360.0 + 360.0) % 360.0)

        // Solve Kepler's equation
        var E = M_rad
        for (i in 0 until 15) {
            val dE = (E - e * sin(E) - M_rad) / (1.0 - e * cos(E))
            E -= dE
            if (abs(dE) < 1e-8) break
        }

        val xOrb = a * (cos(E) - e)
        val yOrb = a * sqrt(1.0 - e * e) * sin(E)

        val v = atan2(yOrb, xOrb)
        val r = sqrt(xOrb * xOrb + yOrb * yOrb)

        val u = v + w
        val xH = r * (cos(N) * cos(u) - sin(N) * sin(u) * cos(I))
        val yH = r * (sin(N) * cos(u) + cos(N) * sin(u) * cos(I))
        val zH = r * (sin(u) * sin(I))

        return Vector3D(xH, yH, zH)
    }

    /**
     * Calculates instantaneous orbital speed v = sqrt( GM * (2/r - 1/a) ) in km/s
     */
    fun calculateOrbitalSpeedKmS(planet: PlanetEngine.PlanetType, rKm: Double): Double {
        val aKm = planet.semiMajorAxisAU * AU_IN_KM
        val v2 = G_SUN * (2.0 / rKm - 1.0 / aKm)
        return sqrt(max(0.0, v2))
    }

    /**
     * Generates complete 3D planetary dataset for all planets in the solar system.
     */
    fun calculateSolarSystem3D(
        jd: Double,
        scale: WalkingScale,
        stepMeters: Double = 0.8, // 0.8m per human walking step
        radiusExaggerationFactor: Double = 1.0
    ): List<Planet3DData> {
        val earthPosKm = calculateHeliocentricPositionKm(PlanetEngine.PlanetType.MARS, jd) // reference
        val realEarthPosKm = calculateHeliocentricPositionKm(PlanetEngine.PlanetType.MERCURY, jd) // temp

        val kmPerMeter = scale.kmPerStep / stepMeters

        return PlanetEngine.PlanetType.values().map { planet ->
            val posKm = calculateHeliocentricPositionKm(planet, jd)
            val rHelioKm = posKm.length()
            val rHelioAU = rHelioKm / AU_IN_KM

            val planetPosGeo = PlanetEngine.calculatePlanet(planet, jd)

            val scaledX = posKm.x / kmPerMeter
            val scaledY = posKm.y / kmPerMeter
            val scaledZ = posKm.z / kmPerMeter
            val scaledPosMeters = Vector3D(scaledX, scaledY, scaledZ)

            val orbitalSpeed = calculateOrbitalSpeedKmS(planet, rHelioKm)

            val baseRadiusMeters = (planet.equatorialRadiusKm / kmPerMeter).coerceAtLeast(0.02)
            val displayRadiusMeters = baseRadiusMeters * radiusExaggerationFactor

            Planet3DData(
                planet = planet,
                heliocentricPosKm = posKm,
                geocentricPosKm = Vector3D(posKm.x - realEarthPosKm.x, posKm.y - realEarthPosKm.y, posKm.z - realEarthPosKm.z),
                heliocentricDistanceAU = rHelioAU,
                geocentricDistanceAU = planetPosGeo.distanceAU,
                orbitalSpeedKmS = orbitalSpeed,
                trueAnomalyDeg = (planetPosGeo.phaseAngleDeg),
                illuminatedFraction = planetPosGeo.illuminatedFraction,
                phaseAngleDeg = planetPosGeo.phaseAngleDeg,
                scaledPositionMeters = scaledPosMeters,
                radiusKm = planet.equatorialRadiusKm,
                displayRadiusMeters = displayRadiusMeters
            )
        }
    }

    /**
     * Calculates 3D positions of Jupiter's Galilean Moons relative to Jupiter in scaled AR space.
     */
    fun calculateJovianMoons3D(
        jd: Double,
        jupiterPosMeters: Vector3D,
        scale: WalkingScale,
        stepMeters: Double = 0.8
    ): List<JovianMoon3DData> {
        val jupSystem = JupiterMoonsEngine.calculateJupiterMoons(jd)
        val kmPerMeter = scale.kmPerStep / stepMeters
        val rJupiterKm = PlanetEngine.PlanetType.JUPITER.equatorialRadiusKm

        return jupSystem.moons.map { moon ->
            val offsetXKm = moon.xRJ * rJupiterKm
            val offsetYKm = moon.yRJ * rJupiterKm
            val offsetZKm = moon.zRJ * rJupiterKm

            val offsetPosKm = Vector3D(offsetXKm, offsetYKm, offsetZKm)

            val offsetMeters = Vector3D(offsetXKm / kmPerMeter, offsetYKm / kmPerMeter, offsetZKm / kmPerMeter)
            val scaledPosMeters = Vector3D(
                jupiterPosMeters.x + offsetMeters.x,
                jupiterPosMeters.y + offsetMeters.y,
                jupiterPosMeters.z + offsetMeters.z
            )

            JovianMoon3DData(
                moon = moon.moon,
                offsetFromJupiterKm = offsetPosKm,
                scaledPositionMeters = scaledPosMeters,
                phenomenon = moon.phenomenon
            )
        }
    }
}
