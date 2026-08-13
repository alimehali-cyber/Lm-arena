package com.alijafari.red.astronomy.astro_engine

import kotlin.math.*

/**
 * SGP4/SDP4 orbital propagator for TLE data.
 *
 * Reference: Hoots & Roehrich (1980), Spacetrack Report No. 3
 * Vallado et al. (2006), AIAA 2006-6753
 *
 * Input: TLE data (epoch, inclination, RAAN, eccentricity, arg perigee,
 *        mean anomaly, mean motion, B* drag term)
 * Output: Position (km) and velocity (km/s) in TEME frame
 */
class SGP4Propagator {

    companion object {
        // Constants
        private const val MINUTES_PER_DAY = 1440.0
        private const val TWO_PI = 2.0 * Math.PI
        private const val DEG2RAD = Math.PI / 180.0
        private const val RAD2DEG = 180.0 / Math.PI
        private const val EARTH_RADIUS_KM = 6378.137  // WGS-84 equatorial radius
        private const val J2 = 0.00108262998905
        private const val J3 = -0.00000253215306
        private const val J4 = -0.00000161098761
        private const val CK2 = J2 / 2.0
        private const val CK4 = -3.0 * J4 / 8.0
        private const val XJ3 = J3
        private const val E6A = 1.0e-6
        private const val Q0 = 120.0  // km
        private const val S0 = 78.0   // km
        private const val XKE = 0.0743669161  // sqrt(GM) / (min * Earth Radii^1.5)
        private const val GM = 398600.4418  // km^3/s^2
    }

    data class TLEData(
        val epochYear: Int,
        val epochDay: Double,      // Day of year with fractional day
        val inclinationDeg: Double,
        val raanDeg: Double,       // Right Ascension of Ascending Node
        val eccentricity: Double,
        val argPerigeeDeg: Double,
        val meanAnomalyDeg: Double,
        val meanMotion: Double,    // Revolutions per day
        val bStar: Double          // B* drag term (1/Earth Radii)
    )

    data class TemEState(
        val xKm: Double,
        val yKm: Double,
        val zKm: Double,
        val vxKmS: Double,
        val vyKmS: Double,
        val vzKmS: Double
    )

    /**
     * Propagate TLE to a given time.
     * @param tle The TLE data
     * @param targetTimeMs Target time in UTC milliseconds
     * @return Position and velocity in TEME frame
     */
    fun propagate(tle: TLEData, targetTimeMs: Long): TemEState {
        // Convert TLE epoch to milliseconds
        val epochMs = tleEpochToMs(tle.epochYear, tle.epochDay)
        val deltaMinutes = (targetTimeMs - epochMs) / 60000.0

        return sgp4(tle, deltaMinutes)
    }

    /**
     * Main SGP4 algorithm.
     * @param tle TLE data
     * @param tsince Minutes since TLE epoch
     * @return TEME position and velocity
     */
    private fun sgp4(tle: TLEData, tsince: Double): TemEState {
        // Step 1: Initialization (compute constants from TLE)
        val radInc = tle.inclinationDeg * DEG2RAD
        val radRaan = tle.raanDeg * DEG2RAD
        val radArgp = tle.argPerigeeDeg * DEG2RAD
        val radMa = tle.meanAnomalyDeg * DEG2RAD
        val ecc = tle.eccentricity.coerceIn(1e-6, 0.999999)
        val bstar = tle.bStar

        val n0 = tle.meanMotion * TWO_PI / MINUTES_PER_DAY  // rad/min

        // Recover un-Kozai mean motion (n0dp) and semi-major axis (a0dp)
        val a0 = (XKE / n0).pow(2.0 / 3.0)
        val cosI0 = cos(radInc)
        val theta2 = cosI0 * cosI0
        val eosq = ecc * ecc
        val beta0sq = 1.0 - eosq
        val beta0 = sqrt(beta0sq)

        val del0 = 1.5 * (CK2 / (a0 * a0)) * ((3.0 * theta2 - 1.0) / (beta0 * beta0sq))
        val a1 = a0 * (1.0 - del0 * (1.0 / 3.0 + del0 * (1.0 + del0 * 134.0 / 81.0)))
        val del1 = 1.5 * (CK2 / (a1 * a1)) * ((3.0 * theta2 - 1.0) / (beta0 * beta0sq))
        val n0dp = n0 / (1.0 + del1)
        val a0dp = (XKE / n0dp).pow(2.0 / 3.0)

        // Perigee altitude check & s4, q0 setup
        val perigeeKm = (a0dp * (1.0 - ecc) - 1.0) * EARTH_RADIUS_KM
        var s4 = S0 / EARTH_RADIUS_KM + 1.0
        var q0ms4 = (Q0 - S0) / EARTH_RADIUS_KM

        if (perigeeKm < 156.0) {
            s4 = a0dp * (1.0 - ecc) - q0ms4
            if (perigeeKm < 98.0) {
                s4 = 20.0 / EARTH_RADIUS_KM + 1.0
            }
            q0ms4 = (Q0 - s4 * EARTH_RADIUS_KM + EARTH_RADIUS_KM) / EARTH_RADIUS_KM
            q0ms4 = q0ms4.pow(4.0)
        } else {
            q0ms4 = q0ms4.pow(4.0)
        }

        val pinvsq = 1.0 / (a0dp * a0dp * beta0sq * beta0sq)
        val tsi = 1.0 / (a0dp - s4)
        val eta = a0dp * ecc * tsi
        val etasq = eta * eta
        val eeta = ecc * eta

        val coef1 = q0ms4 * tsi.pow(4.0)
        val coef = coef1 * tsi

        val c2 = coef1 * n0dp * (a0dp * (1.0 + 1.5 * etasq + eeta * (4.0 + etasq)) + 0.375 * CK2 * tsi / beta0sq * (3.0 * theta2 - 1.0) * (8.0 + 3.0 * etasq * (8.0 + etasq)))
        val c1 = bstar * c2
        val c3 = if (ecc > 1e-4) coef * n0dp * XJ3 * sin(radInc) / (CK2 * ecc) else 0.0
        val c4 = 2.0 * n0dp * coef1 * a0dp * beta0sq * (eta * (2.0 + 0.5 * etasq) + ecc * (0.5 + 2.0 * etasq) - 2.0 * CK2 * tsi / (a0dp * beta0sq) * (3.0 * (1.0 - 3.0 * theta2) * (1.0 + 1.5 * etasq - 2.0 * eeta - 0.5 * eeta * etasq) + 0.75 * (1.0 - theta2) * (2.0 * etasq - eeta - eeta * etasq) * cos(2.0 * radArgp)))
        val c5 = 2.0 * coef1 * a0dp * beta0sq * (1.0 + 2.75 * (etasq + eeta) + eeta * etasq)

        // Secular rates
        val temp1 = 3.0 * CK2 * pinvsq * n0dp
        val temp2 = temp1 * CK2 * pinvsq
        val temp3 = 1.25 * CK4 * pinvsq * pinvsq * n0dp

        val xmdot = n0dp + 0.5 * temp1 * beta0 * (3.0 * theta2 - 1.0) + 0.0625 * temp2 * beta0 * (13.0 - 78.0 * theta2 + 137.0 * theta2 * theta2)
        val omgdot = -0.5 * temp1 * (1.0 - 5.0 * theta2) + 0.0625 * temp2 * (7.0 - 114.0 * theta2 + 395.0 * theta2 * theta2) + temp3 * (3.0 - 36.0 * theta2 + 49.0 * theta2 * theta2)
        val xnodot = -temp1 * cosI0 + 0.125 * temp2 * (4.0 * cosI0 - 19.0 * cosI0 * theta2) + 2.0 * temp3 * cosI0 * (3.0 - 7.0 * theta2)

        val d2 = 4.0 * a0dp * tsi * c1 * c1
        val d3 = (4.0 / 3.0) * a0dp * tsi * tsi * (17.0 * a0dp + s4) * c1 * c1 * c1
        val d4 = (2.0 / 3.0) * a0dp * tsi.pow(3.0) * (221.0 * a0dp + 31.0 * s4) * c1.pow(4.0)

        val t2cof = 1.5 * c1
        val t3cof = d2 + 2.0 * c1 * c1
        val t4cof = 0.25 * (3.0 * d3 + 12.0 * c1 * d2 + 3.0 * c1.pow(3.0))
        val t5cof = 0.2 * (3.0 * d4 + 12.0 * c1 * d3 + 6.0 * d2 * d2 + 12.0 * c1 * c1 * d2 + 2.0 * c1.pow(4.0))

        val omgcof = bstar * c3 * cos(radArgp)
        val xmcof = if (ecc > 1e-4) -2.3333333333333333 * coef1 * bstar / eeta else 0.0
        val xnodcf = 3.5 * beta0sq * temp1 * cosI0 * c1

        // Step 2: Update mean elements for time tsince
        val xmdf = radMa + xmdot * tsince
        val omgadf = radArgp + omgdot * tsince
        val xnoddf = radRaan + xnodot * tsince

        var omega = omgadf
        var xmp = xmdf
        val tsq = tsince * tsince
        val tcube = tsq * tsince
        val tfour = tsince * tcube

        val xnode = xnoddf + xnodcf * tsq
        var tempa = 1.0 - c1 * tsince - d2 * tsq - d3 * tcube - d4 * tfour
        var tempe = bstar * c4 * tsince
        val templ = t2cof * tsq + t3cof * tcube + tfour * (t4cof + tsince * t5cof)

        if (perigeeKm >= 156.0) {
            val delomg = omgcof * tsince
            val delm = xmcof * ((1.0 + eta * cos(xmdf)).pow(3.0) - (1.0 + eta * cos(radMa)).pow(3.0))
            val temp = delomg + delm
            xmp = xmdf + temp
            omega = omgadf - temp
            tempe += bstar * c5 * (sin(xmp) - sin(radMa))
        }

        val a = a0dp * tempa * tempa
        val e = (ecc - tempe).coerceIn(1e-6, 0.999999)

        // Step 3: Solve Kepler's Equation for E
        val mm = (xmp) % TWO_PI
        var u = (mm + TWO_PI) % TWO_PI
        val axn = e * cos(omega)
        val ayn = e * sin(omega)

        var epw = u
        for (i in 0..15) {
            val sinEPW = sin(epw)
            val cosEPW = cos(epw)
            val f = epw - axn * sinEPW + ayn * cosEPW - u
            val fPrime = 1.0 - axn * cosEPW - ayn * sinEPW
            val delta = f / fPrime
            epw -= delta
            if (abs(delta) < 1e-12) break
        }

        // Step 4: Short-period periodic terms and osculating elements
        val sinEPW = sin(epw)
        val cosEPW = cos(epw)
        val ecose = axn * cosEPW + ayn * sinEPW
        val esine = axn * sinEPW - ayn * cosEPW
        val el2 = axn * axn + ayn * ayn
        val pl = a * (1.0 - el2)

        val r = a * (1.0 - ecose)
        val rdot = sqrt(a) * esine / r
        val rfdot = sqrt(pl) / r

        val betal = sqrt(1.0 - el2)
        val sinu = (a / r) * (sinEPW - ayn - axn * esine / (1.0 + betal))
        val cosu = (a / r) * (cosEPW - axn + ayn * esine / (1.0 + betal))
        val uVal = atan2(sinu, cosu)

        val sin2u = sin(2.0 * uVal)
        val cos2u = cos(2.0 * uVal)
        val temp = 1.0 / pl
        val temp1a = CK2 * temp
        val temp2a = temp1a * temp

        val rk = r * (1.0 - 1.5 * temp2a * betal * (3.0 * theta2 - 1.0)) + 0.5 * temp1a * (1.0 - theta2) * cos2u
        val uk = uVal - 0.25 * temp2a * (7.0 * theta2 - 1.0) * sin2u
        val xnodek = xnode + 1.5 * temp2a * cosI0 * sin2u
        val xinck = radInc + 1.5 * temp2a * cosI0 * sin(radInc) * cos2u
        val n0Canonical = n0dp / XKE
        val rdotk = rdot - temp1a * n0Canonical * (1.0 - theta2) * sin2u
        val rfdotk = rfdot + temp1a * n0Canonical * ((1.0 - theta2) * cos2u + 1.5 * (3.0 * theta2 - 1.0))

        // Step 5: Convert to TEME position and velocity
        val sinuk = sin(uk)
        val cosuk = cos(uk)
        val sinik = sin(xinck)
        val cosik = cos(xinck)
        val sinnok = sin(xnodek)
        val cosnok = cos(xnodek)

        val mx = -sinnok * cosik
        val my = cosnok * cosik
        val mz = sinik

        val nx = cosnok
        val ny = sinnok

        val ux = nx * cosuk + mx * sinuk
        val uy = ny * cosuk + my * sinuk
        val uz = mz * sinuk

        val vx = -nx * sinuk + mx * cosuk
        val vy = -ny * sinuk + my * cosuk
        val vz = mz * cosuk

        val xKm = rk * ux * EARTH_RADIUS_KM
        val yKm = rk * uy * EARTH_RADIUS_KM
        val zKm = rk * uz * EARTH_RADIUS_KM

        val vFactor = EARTH_RADIUS_KM * XKE / 60.0
        val vxKmS = (rdotk * ux + rfdotk * vx) * vFactor
        val vyKmS = (rdotk * uy + rfdotk * vy) * vFactor
        val vzKmS = (rdotk * uz + rfdotk * vz) * vFactor

        return TemEState(xKm, yKm, zKm, vxKmS, vyKmS, vzKmS)
    }

    /**
     * Convert TLE epoch (year + fractional day) to UTC milliseconds.
     */
    private fun tleEpochToMs(year: Int, dayFrac: Double): Long {
        // Handle two-digit year convention
        val fullYear = if (year < 57) 2000 + year else if (year < 100) 1900 + year else year

        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(java.util.Calendar.YEAR, fullYear)
            set(java.util.Calendar.MONTH, 0)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val jan1Ms = cal.timeInMillis
        val dayMs = (dayFrac - 1.0) * 86400000.0
        return jan1Ms + dayMs.toLong()
    }
}
