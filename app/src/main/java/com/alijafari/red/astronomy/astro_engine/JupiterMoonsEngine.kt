package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

object JupiterMoonsEngine {

    enum class GalileanMoon(
        val nameEn: String,
        val nameFa: String,
        val meanRadiusKm: Double,
        val orbitalPeriodDays: Double,
        val meanSemiMajorAxisRJ: Double
    ) {
        IO("Io", "آیو", 1821.6, 1.7691377, 5.90),
        EUROPA("Europa", "اروپا", 1560.8, 3.5511810, 9.40),
        GANYMEDE("Ganymede", "گانیمد", 2634.1, 7.1545530, 14.99),
        CALLISTO("Callisto", "کالیستو", 2410.3, 16.6890180, 26.37)
    }

    enum class MoonPhenomenon {
        VISIBLE,
        IN_TRANSIT,       // Moon in front of Jupiter's disk
        OCCULTED,         // Moon behind Jupiter's disk
        IN_ECLIPSE,       // Moon inside Jupiter's shadow cone
        SHADOW_TRANSIT    // Shadow projected onto Jupiter
    }

    data class MoonPosition(
        val moon: GalileanMoon,
        val xRJ: Double,          // X offset in Jupiter radii (+East)
        val yRJ: Double,          // Y offset in Jupiter radii (+North)
        val zRJ: Double,          // Z depth (+away from observer, behind planet)
        val offsetRaArcsec: Double,
        val offsetDecArcsec: Double,
        val angularDistanceArcsec: Double,
        val phenomenon: MoonPhenomenon
    )

    data class JupiterSystem(
        val jupiterPos: PlanetEngine.PlanetPosition,
        val moons: List<MoonPosition>,
        val grsLongitudeDeg: Double,
        val isGrsVisible: Boolean
    )

    /**
     * Calculates positions of Galilean Moons for Julian Date (Meeus Chapter 44).
     */
    fun calculateJupiterMoons(jd: Double): JupiterSystem {
        val jupPos = PlanetEngine.calculatePlanet(PlanetEngine.PlanetType.JUPITER, jd)
        val d = jd - 2451545.0

        // Mean anomalies & Arguments of latitude for Jupiter system
        val V = (172.74 + 0.00111588 * d) % 360.0
        val M = (357.529 + 0.9856003 * d) % 360.0
        val N = (20.00 + 0.0830853 * d + 0.329 * sin(Math.toRadians(V))) % 360.0

        val J1_mean = (163.8067 + 203.4889538 * d) % 360.0
        val J2_mean = (358.4108 + 101.3747231 * d) % 360.0
        val J3_mean = (5.7129 + 50.3176081 * d) % 360.0
        val J4_mean = (224.8169 + 21.5710715 * d) % 360.0

        val rJupKm = 71492.0
        val jupAngularRadiusArcsec = jupPos.angularDiameterArcsec / 2.0

        // Compute moon coordinates relative to Jupiter center in Jupiter Radii (R_J)
        val moonPositions = GalileanMoon.values().map { moon ->
            val uDeg = when (moon) {
                GalileanMoon.IO -> J1_mean
                GalileanMoon.EUROPA -> J2_mean
                GalileanMoon.GANYMEDE -> J3_mean
                GalileanMoon.CALLISTO -> J4_mean
            }
            val uRad = Math.toRadians(uDeg)

            val aRJ = moon.meanSemiMajorAxisRJ
            val xRJ = aRJ * sin(uRad)
            val zRJ = aRJ * cos(uRad) // +z = behind Jupiter
            val yRJ = 0.1 * aRJ * cos(uRad) // Inclination tilt

            val offsetRaArcsec = xRJ * jupAngularRadiusArcsec
            val offsetDecArcsec = yRJ * jupAngularRadiusArcsec
            val angDistArcsec = sqrt(offsetRaArcsec * offsetRaArcsec + offsetDecArcsec * offsetDecArcsec)

            // Determine Phenomenon (Occultation, Transit, Eclipse)
            val isBehindDisk = abs(xRJ) <= 1.0 && zRJ > 0.0
            val isInFrontOfDisk = abs(xRJ) <= 1.0 && zRJ < 0.0
            val isInShadow = xRJ in 1.0..1.5 && zRJ > 0.0 // Shadow cylinder approximation

            val phenomenon = when {
                isInFrontOfDisk -> MoonPhenomenon.IN_TRANSIT
                isBehindDisk -> MoonPhenomenon.OCCULTED
                isInShadow -> MoonPhenomenon.IN_ECLIPSE
                else -> MoonPhenomenon.VISIBLE
            }

            MoonPosition(
                moon = moon,
                xRJ = xRJ,
                yRJ = yRJ,
                zRJ = zRJ,
                offsetRaArcsec = offsetRaArcsec,
                offsetDecArcsec = offsetDecArcsec,
                angularDistanceArcsec = angDistArcsec,
                phenomenon = phenomenon
            )
        }

        // Great Red Spot (GRS) System II Longitude (drift rate ~1.2° / year)
        val yearCurrent = 2000.0 + d / 365.25
        val grsLonDeg = (15.0 + 1.25 * (yearCurrent - 2024.0) * 360.0 / 365.25) % 360.0

        // Central Meridian System II
        val cmIIDeg = (180.0 + 870.27 * d) % 360.0
        val grsDiff = abs(((grsLonDeg - cmIIDeg) % 360.0 + 360.0) % 360.0)
        val isGrsVisible = grsDiff < 85.0 || grsDiff > 275.0

        return JupiterSystem(
            jupiterPos = jupPos,
            moons = moonPositions,
            grsLongitudeDeg = grsLonDeg,
            isGrsVisible = isGrsVisible
        )
    }
}
