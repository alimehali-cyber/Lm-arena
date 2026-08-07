package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.util.toPersianDigits
import java.util.Locale
import kotlin.math.*

enum class DistanceUnit(val labelEn: String, val labelFa: String) {
    AUTO("Auto", "خودکار"),
    LIGHT_YEARS("Light Years", "سال نوری"),
    AU("AU", "واحد نجومی (AU)"),
    KM("Kilometers", "کیلومتر"),
    METERS("Meters", "متر")
}

object RelativisticEngine {

    const val SPEED_OF_LIGHT_MS = 299792458.0 // m/s
    const val METERS_PER_LY = 9.4607304725808e15 // meters
    const val METERS_PER_AU = 149597870700.0 // meters
    const val AU_PER_LY = 63241.077
    const val STANDARD_G_MS2 = 9.80665 // m/s^2

    /**
     * Parses localized numbers (Persian/Arabic digits, commas, Persian decimal separators)
     * into standard Double values safely in a locale-independent manner.
     */
    fun parseLocalizedDouble(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val sb = StringBuilder()
        for (ch in input.trim()) {
            when (ch) {
                '۰', '٠' -> sb.append('0')
                '۱', '١' -> sb.append('1')
                '۲', '٢' -> sb.append('2')
                '۳', '٣' -> sb.append('3')
                '۴', '٤' -> sb.append('4')
                '۵', '٥' -> sb.append('5')
                '۶', '٦' -> sb.append('6')
                '۷', '٧' -> sb.append('7')
                '۸', '٨' -> sb.append('8')
                '۹', '٩' -> sb.append('9')
                '٫', ',' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString().toDoubleOrNull()
    }

    data class DistanceResult(
        val distanceMeters: Double,
        val distanceLightYears: Double,
        val distanceAu: Double,
        val distanceKm: Double,
        val isTimeDependent: Boolean,
        val noteEn: String,
        val noteFa: String
    )

    data class RelativisticJourneyResult(
        val startObject: CelestialObject,
        val destinationObject: CelestialObject,
        val distance: DistanceResult,
        val requestedSpeedMs: Double,
        val speedFractionOfC: Double, // v / c
        val isSuperluminal: Boolean,
        val isSpeedOfLight: Boolean,
        val isAccelerationOn: Boolean,
        val properAccelerationMs2: Double,
        val maxVelocityReachedMs: Double,
        val maxVelocityFractionOfC: Double,
        val lorentzFactorPeak: Double,
        val earthTimeSeconds: Double,
        val travellerTimeSeconds: Double,
        val timeDifferenceSeconds: Double, // t_earth - t_traveller
        val percentageTimeDifference: Double, // (t - tau)/t * 100
        val isLengthContractionOn: Boolean,
        val contractedDistanceMeters: Double,
        val contractedDistanceLy: Double,
        val accelerationPhaseEarthSeconds: Double = 0.0,
        val accelerationPhaseTravellerSeconds: Double = 0.0,
        val cruisePhaseEarthSeconds: Double = 0.0,
        val cruisePhaseTravellerSeconds: Double = 0.0,
        val decelerationPhaseEarthSeconds: Double = 0.0,
        val decelerationPhaseTravellerSeconds: Double = 0.0
    )

    /**
     * Calculates 3D distance between two celestial objects using catalog coordinates and distances.
     */
    fun calculateDistance(
        objA: CelestialObject,
        objB: CelestialObject
    ): DistanceResult {
        if (objA.id == objB.id) {
            return DistanceResult(
                distanceMeters = 0.0,
                distanceLightYears = 0.0,
                distanceAu = 0.0,
                distanceKm = 0.0,
                isTimeDependent = false,
                noteEn = "Identical location selected.",
                noteFa = "مبدأ و مقصد یکسان انتخاب شده است."
            )
        }

        val isSolarSystemA = isSolarSystemObject(objA)
        val isSolarSystemB = isSolarSystemObject(objB)
        val isTimeDependent = isSolarSystemA || isSolarSystemB

        val noteEn = if (isTimeDependent) {
            "Instantaneous orbital distance at current epoch. Positions vary along Keplerian orbits."
        } else {
            "Static interstellar distance based on reference astronomical catalog."
        }

        val noteFa = if (isTimeDependent) {
            "فاصله لحظه‌ای بر اساس موقعیت فعلی در مدار کپلری (متغیر با زمان)."
        } else {
            "فاصله ثابت بین‌ستاره‌ای بر اساس کاتالوگ نجومی مرجع."
        }

        // Check if one object is Earth
        if (objA.id == "planet_earth") {
            val distLy = objB.distanceLightYears
            val distMeters = distLy * METERS_PER_LY
            return DistanceResult(
                distanceMeters = distMeters,
                distanceLightYears = distLy,
                distanceAu = distMeters / METERS_PER_AU,
                distanceKm = distMeters / 1000.0,
                isTimeDependent = isTimeDependent,
                noteEn = noteEn,
                noteFa = noteFa
            )
        }

        if (objB.id == "planet_earth") {
            val distLy = objA.distanceLightYears
            val distMeters = distLy * METERS_PER_LY
            return DistanceResult(
                distanceMeters = distMeters,
                distanceLightYears = distLy,
                distanceAu = distMeters / METERS_PER_AU,
                distanceKm = distMeters / 1000.0,
                isTimeDependent = isTimeDependent,
                noteEn = noteEn,
                noteFa = noteFa
            )
        }

        // Calculate 3D Cartesian coordinates in Light Years
        val raA = Math.toRadians(objA.raDeg)
        val decA = Math.toRadians(objA.decDeg)
        val dA = objA.distanceLightYears

        val xA = dA * cos(decA) * cos(raA)
        val yA = dA * cos(decA) * sin(raA)
        val zA = dA * sin(decA)

        val raB = Math.toRadians(objB.raDeg)
        val decB = Math.toRadians(objB.decDeg)
        val dB = objB.distanceLightYears

        val xB = dB * cos(decB) * cos(raB)
        val yB = dB * cos(decB) * sin(raB)
        val zB = dB * sin(decB)

        val distLy = sqrt((xB - xA).pow(2) + (yB - yA).pow(2) + (zB - zA).pow(2))
        val distMeters = distLy * METERS_PER_LY

        return DistanceResult(
            distanceMeters = distMeters,
            distanceLightYears = distLy,
            distanceAu = distMeters / METERS_PER_AU,
            distanceKm = distMeters / 1000.0,
            isTimeDependent = isTimeDependent,
            noteEn = noteEn,
            noteFa = noteFa
        )
    }

    private fun isSolarSystemObject(obj: CelestialObject): Boolean {
        return obj.id.startsWith("sun") ||
                obj.id.startsWith("moon") ||
                obj.id.startsWith("planet") ||
                obj.id.startsWith("galilean_moon") ||
                obj.id.startsWith("sat_iss")
    }

    /**
     * Solves relativistic journey kinematics for constant velocity or relativistic proper acceleration.
     */
    fun calculateJourney(
        startObject: CelestialObject,
        destinationObject: CelestialObject,
        speedMs: Double,
        isAccelerationOn: Boolean = false,
        accelerationMs2: Double = STANDARD_G_MS2,
        isLengthContractionOn: Boolean = true
    ): RelativisticJourneyResult {
        val distance = calculateDistance(startObject, destinationObject)
        val beta = speedMs / SPEED_OF_LIGHT_MS
        val isSuperluminal = beta > 1.00000001
        val isSpeedOfLight = abs(beta - 1.0) <= 1e-8

        if (isSuperluminal) {
            return RelativisticJourneyResult(
                startObject = startObject,
                destinationObject = destinationObject,
                distance = distance,
                requestedSpeedMs = speedMs,
                speedFractionOfC = beta,
                isSuperluminal = true,
                isSpeedOfLight = false,
                isAccelerationOn = isAccelerationOn,
                properAccelerationMs2 = accelerationMs2,
                maxVelocityReachedMs = speedMs,
                maxVelocityFractionOfC = beta,
                lorentzFactorPeak = Double.NaN,
                earthTimeSeconds = Double.NaN,
                travellerTimeSeconds = Double.NaN,
                timeDifferenceSeconds = Double.NaN,
                percentageTimeDifference = Double.NaN,
                isLengthContractionOn = isLengthContractionOn,
                contractedDistanceMeters = Double.NaN,
                contractedDistanceLy = Double.NaN
            )
        }

        if (isSpeedOfLight) {
            val earthSec = if (speedMs > 0) distance.distanceMeters / SPEED_OF_LIGHT_MS else 0.0
            return RelativisticJourneyResult(
                startObject = startObject,
                destinationObject = destinationObject,
                distance = distance,
                requestedSpeedMs = SPEED_OF_LIGHT_MS,
                speedFractionOfC = 1.0,
                isSuperluminal = false,
                isSpeedOfLight = true,
                isAccelerationOn = isAccelerationOn,
                properAccelerationMs2 = accelerationMs2,
                maxVelocityReachedMs = SPEED_OF_LIGHT_MS,
                maxVelocityFractionOfC = 1.0,
                lorentzFactorPeak = Double.POSITIVE_INFINITY,
                earthTimeSeconds = earthSec,
                travellerTimeSeconds = 0.0, // Proper time for photon is 0
                timeDifferenceSeconds = earthSec,
                percentageTimeDifference = 100.0,
                isLengthContractionOn = isLengthContractionOn,
                contractedDistanceMeters = 0.0, // Length completely contracted to 0
                contractedDistanceLy = 0.0
            )
        }

        // Relativistic Calculations for beta < 1.0
        val clampedBeta = beta.coerceIn(1e-12, 0.99999999999)
        val gamma = if (clampedBeta < 1e-6) {
            1.0 + 0.5 * clampedBeta.pow(2)
        } else {
            1.0 / sqrt(1.0 - clampedBeta.pow(2))
        }

        if (!isAccelerationOn) {
            // Constant Velocity Mode
            val earthSec = distance.distanceMeters / (clampedBeta * SPEED_OF_LIGHT_MS)
            val travellerSec = earthSec / gamma
            val timeDiff = earthSec - travellerSec
            val percentDiff = if (earthSec > 0) (timeDiff / earthSec) * 100.0 else 0.0
            val contractedMeters = if (isLengthContractionOn) distance.distanceMeters / gamma else distance.distanceMeters

            return RelativisticJourneyResult(
                startObject = startObject,
                destinationObject = destinationObject,
                distance = distance,
                requestedSpeedMs = speedMs,
                speedFractionOfC = clampedBeta,
                isSuperluminal = false,
                isSpeedOfLight = false,
                isAccelerationOn = false,
                properAccelerationMs2 = accelerationMs2,
                maxVelocityReachedMs = speedMs,
                maxVelocityFractionOfC = clampedBeta,
                lorentzFactorPeak = gamma,
                earthTimeSeconds = earthSec,
                travellerTimeSeconds = travellerSec,
                timeDifferenceSeconds = timeDiff,
                percentageTimeDifference = percentDiff,
                isLengthContractionOn = isLengthContractionOn,
                contractedDistanceMeters = contractedMeters,
                contractedDistanceLy = contractedMeters / METERS_PER_LY
            )
        } else {
            // Relativistic Proper Acceleration Mode (Brachistochrone trajectory)
            val a = max(1e-4, accelerationMs2)
            val dHalf = distance.distanceMeters / 2.0
            val c = SPEED_OF_LIGHT_MS

            // Peak Lorentz factor at midpoint if uncapped
            val gammaMidUncapped = 1.0 + (a * dHalf) / (c * c)
            val betaMidUncapped = sqrt(1.0 - 1.0 / (gammaMidUncapped * gammaMidUncapped))

            if (betaMidUncapped <= clampedBeta) {
                // Pure 2-phase journey (Accelerate to midpoint, Decelerate to destination)
                val tAccEarth = (c / a) * sqrt(gammaMidUncapped * gammaMidUncapped - 1.0)
                val tAccTraveller = (c / a) * asinh(sqrt(gammaMidUncapped * gammaMidUncapped - 1.0))

                val totalEarthSec = 2.0 * tAccEarth
                val totalTravellerSec = 2.0 * tAccTraveller
                val timeDiff = totalEarthSec - totalTravellerSec
                val percentDiff = if (totalEarthSec > 0) (timeDiff / totalEarthSec) * 100.0 else 0.0

                val avgGamma = if (totalTravellerSec > 0) totalEarthSec / totalTravellerSec else 1.0
                val contractedMeters = if (isLengthContractionOn) distance.distanceMeters / avgGamma else distance.distanceMeters

                return RelativisticJourneyResult(
                    startObject = startObject,
                    destinationObject = destinationObject,
                    distance = distance,
                    requestedSpeedMs = speedMs,
                    speedFractionOfC = clampedBeta,
                    isSuperluminal = false,
                    isSpeedOfLight = false,
                    isAccelerationOn = true,
                    properAccelerationMs2 = a,
                    maxVelocityReachedMs = betaMidUncapped * c,
                    maxVelocityFractionOfC = betaMidUncapped,
                    lorentzFactorPeak = gammaMidUncapped,
                    earthTimeSeconds = totalEarthSec,
                    travellerTimeSeconds = totalTravellerSec,
                    timeDifferenceSeconds = timeDiff,
                    percentageTimeDifference = percentDiff,
                    isLengthContractionOn = isLengthContractionOn,
                    contractedDistanceMeters = contractedMeters,
                    contractedDistanceLy = contractedMeters / METERS_PER_LY,
                    accelerationPhaseEarthSeconds = tAccEarth,
                    accelerationPhaseTravellerSeconds = tAccTraveller,
                    cruisePhaseEarthSeconds = 0.0,
                    cruisePhaseTravellerSeconds = 0.0,
                    decelerationPhaseEarthSeconds = tAccEarth,
                    decelerationPhaseTravellerSeconds = tAccTraveller
                )
            } else {
                // 3-phase journey (Accelerate to cap, Cruise at cap, Decelerate to destination)
                val gammaCap = gamma
                val dAcc = (c * c / a) * (gammaCap - 1.0)

                if (2.0 * dAcc >= distance.distanceMeters) {
                    // Fallback to 2-phase
                    val tAccEarth = (c / a) * sqrt(gammaMidUncapped * gammaMidUncapped - 1.0)
                    val tAccTraveller = (c / a) * asinh(sqrt(gammaMidUncapped * gammaMidUncapped - 1.0))

                    val totalEarthSec = 2.0 * tAccEarth
                    val totalTravellerSec = 2.0 * tAccTraveller
                    val timeDiff = totalEarthSec - totalTravellerSec
                    val percentDiff = if (totalEarthSec > 0) (timeDiff / totalEarthSec) * 100.0 else 0.0
                    val avgGamma = if (totalTravellerSec > 0) totalEarthSec / totalTravellerSec else 1.0
                    val contractedMeters = if (isLengthContractionOn) distance.distanceMeters / avgGamma else distance.distanceMeters

                    return RelativisticJourneyResult(
                        startObject = startObject,
                        destinationObject = destinationObject,
                        distance = distance,
                        requestedSpeedMs = speedMs,
                        speedFractionOfC = clampedBeta,
                        isSuperluminal = false,
                        isSpeedOfLight = false,
                        isAccelerationOn = true,
                        properAccelerationMs2 = a,
                        maxVelocityReachedMs = betaMidUncapped * c,
                        maxVelocityFractionOfC = betaMidUncapped,
                        lorentzFactorPeak = gammaMidUncapped,
                        earthTimeSeconds = totalEarthSec,
                        travellerTimeSeconds = totalTravellerSec,
                        timeDifferenceSeconds = timeDiff,
                        percentageTimeDifference = percentDiff,
                        isLengthContractionOn = isLengthContractionOn,
                        contractedDistanceMeters = contractedMeters,
                        contractedDistanceLy = contractedMeters / METERS_PER_LY,
                        accelerationPhaseEarthSeconds = tAccEarth,
                        accelerationPhaseTravellerSeconds = tAccTraveller,
                        cruisePhaseEarthSeconds = 0.0,
                        cruisePhaseTravellerSeconds = 0.0,
                        decelerationPhaseEarthSeconds = tAccEarth,
                        decelerationPhaseTravellerSeconds = tAccTraveller
                    )
                }

                val tAccEarth = (c / a) * sqrt(gammaCap * gammaCap - 1.0)
                val tAccTraveller = (c / a) * asinh(sqrt(gammaCap * gammaCap - 1.0))

                val dCruise = distance.distanceMeters - 2.0 * dAcc
                val vCap = clampedBeta * c
                val tCruiseEarth = dCruise / vCap
                val tCruiseTraveller = tCruiseEarth / gammaCap

                val totalEarthSec = 2.0 * tAccEarth + tCruiseEarth
                val totalTravellerSec = 2.0 * tAccTraveller + tCruiseTraveller
                val timeDiff = totalEarthSec - totalTravellerSec
                val percentDiff = if (totalEarthSec > 0) (timeDiff / totalEarthSec) * 100.0 else 0.0

                val avgGamma = if (totalTravellerSec > 0) totalEarthSec / totalTravellerSec else 1.0
                val contractedMeters = if (isLengthContractionOn) distance.distanceMeters / avgGamma else distance.distanceMeters

                return RelativisticJourneyResult(
                    startObject = startObject,
                    destinationObject = destinationObject,
                    distance = distance,
                    requestedSpeedMs = speedMs,
                    speedFractionOfC = clampedBeta,
                    isSuperluminal = false,
                    isSpeedOfLight = false,
                    isAccelerationOn = true,
                    properAccelerationMs2 = a,
                    maxVelocityReachedMs = vCap,
                    maxVelocityFractionOfC = clampedBeta,
                    lorentzFactorPeak = gammaCap,
                    earthTimeSeconds = totalEarthSec,
                    travellerTimeSeconds = totalTravellerSec,
                    timeDifferenceSeconds = timeDiff,
                    percentageTimeDifference = percentDiff,
                    isLengthContractionOn = isLengthContractionOn,
                    contractedDistanceMeters = contractedMeters,
                    contractedDistanceLy = contractedMeters / METERS_PER_LY,
                    accelerationPhaseEarthSeconds = tAccEarth,
                    accelerationPhaseTravellerSeconds = tAccTraveller,
                    cruisePhaseEarthSeconds = tCruiseEarth,
                    cruisePhaseTravellerSeconds = tCruiseTraveller,
                    decelerationPhaseEarthSeconds = tAccEarth,
                    decelerationPhaseTravellerSeconds = tAccTraveller
                )
            }
        }
    }

    /**
     * Formats duration in seconds into human readable time string in a locale-safe manner.
     */
    fun formatDuration(seconds: Double, isFa: Boolean = false): String {
        if (seconds.isNaN()) return "—"
        if (seconds.isInfinite()) return if (isFa) "بی‌نهایت" else "Infinite"
        if (seconds <= 0) return if (isFa) "۰ ثانیه".toPersianDigits() else "0 seconds"

        val secInMin = 60.0
        val secInHour = 3600.0
        val secInDay = 86400.0
        val secInYear = 31557600.0 // Julian year (365.25 days)

        val rawStr = when {
            seconds < 60.0 -> {
                val valStr = String.format(Locale.US, "%.2f", seconds)
                if (isFa) "$valStr ثانیه" else "$valStr sec"
            }
            seconds < secInHour -> {
                val mins = (seconds / secInMin).toInt()
                val secs = (seconds % secInMin).toInt()
                if (isFa) "$mins دقیقه و $secs ثانیه" else "$mins m $secs s"
            }
            seconds < secInDay -> {
                val hrs = (seconds / secInHour).toInt()
                val mins = ((seconds % secInHour) / secInMin).toInt()
                if (isFa) "$hrs ساعت و $mins دقیقه" else "$hrs h $mins m"
            }
            seconds < secInYear -> {
                val days = (seconds / secInDay).toInt()
                val hrs = ((seconds % secInDay) / secInHour).toInt()
                if (isFa) "$days روز و $hrs ساعت" else "$days days $hrs h"
            }
            seconds < 100.0 * secInYear -> {
                val yrs = (seconds / secInYear).toInt()
                val days = ((seconds % secInYear) / secInDay).toInt()
                if (isFa) "$yrs سال و $days روز" else "$yrs yrs $days d"
            }
            seconds < 10000.0 * secInYear -> {
                val yrs = seconds / secInYear
                val valStr = String.format(Locale.US, "%.1f", yrs)
                if (isFa) "$valStr سال" else "$valStr years"
            }
            seconds < 1e6 * secInYear -> {
                val centuries = seconds / (100.0 * secInYear)
                val valStr = String.format(Locale.US, "%.1f", centuries)
                if (isFa) "$valStr قرن" else "$valStr centuries"
            }
            else -> {
                val myr = seconds / (1e6 * secInYear)
                val valStr = String.format(Locale.US, "%.2f", myr)
                if (isFa) "$valStr میلیون سال" else "$valStr Million years"
            }
        }

        return if (isFa) rawStr.toPersianDigits() else rawStr
    }

    fun formatDistance(
        meters: Double,
        unit: DistanceUnit = DistanceUnit.AUTO,
        isFa: Boolean = false
    ): String {
        if (meters.isNaN()) return "—"
        val ly = meters / METERS_PER_LY
        val au = meters / METERS_PER_AU
        val km = meters / 1000.0

        val (formattedVal, unitFa, unitEn) = when (unit) {
            DistanceUnit.AUTO -> {
                when {
                    ly >= 0.01 -> Triple(
                        if (ly >= 100) String.format(Locale.US, "%,.0f", ly) else String.format(Locale.US, "%.3f", ly),
                        "سال نوری", "light-years"
                    )
                    au >= 0.01 -> Triple(
                        String.format(Locale.US, "%.3f", au),
                        "واحد نجومی (AU)", "AU"
                    )
                    else -> Triple(
                        String.format(Locale.US, "%,.0f", km),
                        "کیلومتر", "km"
                    )
                }
            }
            DistanceUnit.LIGHT_YEARS -> Triple(
                if (ly < 0.0001 && ly > 0) String.format(Locale.US, "%.6f", ly)
                else if (ly >= 100) String.format(Locale.US, "%,.1f", ly)
                else String.format(Locale.US, "%.4f", ly),
                "سال نوری", "light-years"
            )
            DistanceUnit.AU -> Triple(
                if (au < 0.001 && au > 0) String.format(Locale.US, "%.6f", au)
                else if (au >= 1000) String.format(Locale.US, "%,.1f", au)
                else String.format(Locale.US, "%.3f", au),
                "واحد نجومی (AU)", "AU"
            )
            DistanceUnit.KM -> Triple(
                if (km >= 1e9) String.format(Locale.US, "%.3e", km)
                else String.format(Locale.US, "%,.0f", km),
                "کیلومتر", "km"
            )
            DistanceUnit.METERS -> Triple(
                if (meters >= 1e9) String.format(Locale.US, "%.3e", meters)
                else String.format(Locale.US, "%,.0f", meters),
                "متر", "m"
            )
        }

        val rawStr = if (isFa) "$formattedVal $unitFa" else "$formattedVal $unitEn"
        return if (isFa) rawStr.toPersianDigits() else rawStr
    }
}
