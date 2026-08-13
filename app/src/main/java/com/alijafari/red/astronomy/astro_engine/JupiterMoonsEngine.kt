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
        CALLISTO("Callisto", "کالیستو", 2410.3, 16.6890180, 26.37),
        ELARA("Elara", "الارا", 43.0, 259.65, 164.2)
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

    private const val DEG2RAD = Math.PI / 180.0
    private const val RAD2DEG = 180.0 / Math.PI

    /**
     * Calculates positions of Galilean Moons for Julian Date (Meeus Chapter 44, Lieske E5 theory).
     */
    fun calculateJupiterMoons(jd: Double): JupiterSystem {
        val jupPos = PlanetEngine.calculatePlanet(PlanetEngine.PlanetType.JUPITER, jd)
        val d = jd - 2451545.0

        // Fundamental arguments (Meeus Chapter 44)
        val V = (172.74 + 0.00111588 * d) * DEG2RAD
        val M = (357.529 + 0.9856003 * d) * DEG2RAD
        val N = (20.00 + 0.0830853 * d + 0.329 * sin(V)) * DEG2RAD
        val J = (66.115 + 0.904701 * d - 0.329 * sin(V)) * DEG2RAD
        val A = (191.81 + 0.831919 * d) * DEG2RAD
        val B = (21.82 + 0.320807 * d) * DEG2RAD
        val C = (357.10 + 0.208374 * d) * DEG2RAD
        val D = (311.58 + 0.082863 * d) * DEG2RAD
        val E = (341.5 + 0.04350 * d) * DEG2RAD

        // Mean longitudes u1..u4
        val u1 = (163.8067 + 203.4889538 * d + 0.0017371 * sin(V) * RAD2DEG + 0.0031 * sin(M) * RAD2DEG) * DEG2RAD
        val u2 = (358.4108 + 101.3747231 * d + 0.0017371 * sin(V) * RAD2DEG + 0.0031 * sin(M) * RAD2DEG) * DEG2RAD
        val u3 = (5.7129 + 50.3176081 * d + 0.0017371 * sin(V) * RAD2DEG + 0.0031 * sin(M) * RAD2DEG) * DEG2RAD
        val u4 = (224.8169 + 21.5710715 * d + 0.0017371 * sin(V) * RAD2DEG + 0.0031 * sin(M) * RAD2DEG) * DEG2RAD

        // Longitude perturbations (Laplace resonance & eccentricities) in radians
        val dL1 = (0.47259 * sin(2.0 * (u1 - u2)) - 0.0340 * sin(u1 - u2) + 0.0108 * sin(2.0 * (u2 - u3)) -
                   0.0073 * sin(2.0 * (u1 - J)) + 0.0023 * sin(u1 - A) + 0.0021 * sin(u1 - C)) * DEG2RAD
        val dL2 = (1.06476 * sin(2.0 * (u2 - u3)) + 0.04256 * sin(u1 - u2) - 0.0358 * sin(u2 - B) -
                   0.0238 * sin(u2 - C) - 0.0128 * sin(2.0 * (u2 - J)) + 0.0076 * sin(u2 - A)) * DEG2RAD
        val dL3 = (0.16490 * sin(u3 - C) + 0.09073 * sin(u2 - u3) - 0.0690 * sin(u3 - D) +
                   0.0279 * sin(u3 - B) - 0.0068 * sin(2.0 * (u3 - J))) * DEG2RAD
        val dL4 = (0.84287 * sin(u4 - D) + 0.03431 * sin(u4 - E) - 0.0330 * sin(u4 - C)) * DEG2RAD

        // True longitudes l1..l4
        val l1 = u1 + dL1
        val l2 = u2 + dL2
        val l3 = u3 + dL3
        val l4 = u4 + dL4

        // Radial distances r1..r4 in Jupiter Radii (R_J)
        val r1 = 5.9057 * (1.0 - 0.00413 * cos(2.0 * (u1 - u2)) + 0.00006 * cos(u1 - u2))
        val r2 = 9.3970 * (1.0 - 0.00930 * cos(2.0 * (u2 - u3)) - 0.00037 * cos(u1 - u2) + 0.00062 * cos(u2 - B))
        val r3 = 14.9890 * (1.0 - 0.00143 * cos(u3 - C) - 0.00079 * cos(u2 - u3) + 0.00060 * cos(u3 - D))
        val r4 = 26.3640 * (1.0 - 0.00735 * cos(u4 - D) - 0.00030 * cos(u4 - E))

        // Latitudinal terms b1..b4 in radians
        val b1 = (0.0381 * sin(l1 - B) + 0.0031 * sin(l1 - A) + 0.0018 * sin(l1 - C)) * DEG2RAD
        val b2 = (0.4681 * sin(l2 - B) + 0.0282 * sin(l2 - A) + 0.0163 * sin(l2 - C)) * DEG2RAD
        val b3 = (0.1827 * sin(l3 - B) + 0.0264 * sin(l3 - A) + 0.0116 * sin(l3 - C)) * DEG2RAD
        val b4 = (0.2543 * sin(l4 - B) + 0.0503 * sin(l4 - D) + 0.0232 * sin(l4 - C)) * DEG2RAD

        // Inclination of Jupiter's equator to Earth De
        val De = (3.12 * sin(J - 191.81 * DEG2RAD) + 0.43 * sin(J - 21.82 * DEG2RAD)) * DEG2RAD

        val jupAngularRadiusArcsec = jupPos.angularDiameterArcsec / 2.0

        // Compute moon coordinates relative to Jupiter center in Jupiter Radii (R_J)
        val moonPositions = GalileanMoon.values().map { moon ->
            val (r, l, b) = when (moon) {
                GalileanMoon.IO -> Triple(r1, l1, b1)
                GalileanMoon.EUROPA -> Triple(r2, l2, b2)
                GalileanMoon.GANYMEDE -> Triple(r3, l3, b3)
                GalileanMoon.CALLISTO -> Triple(r4, l4, b4)
                GalileanMoon.ELARA -> Triple(164.2, (110.0 + 1.3863 * d) * DEG2RAD, 0.0)
            }

            val U = l - J
            val xRJ = r * sin(U)
            val yRJ = r * cos(U) * sin(De) + r * b * cos(De)
            val zRJ = r * cos(U) * cos(De)

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
