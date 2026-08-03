package com.example.astro_engine

import com.example.domain.CelestialObject
import kotlin.math.atan2
import kotlin.math.sqrt

data class FinderData(
    val targetObject: CelestialObject,
    val targetAzimuthDeg: Double,
    val targetAltitudeDeg: Double,
    val deltaAzimuthDeg: Float,
    val deltaAltitudeDeg: Float,
    val totalAngularDistanceDeg: Float,
    val arrowAngleRad: Float,
    val proximityFraction: Float,
    val instructionFa: String,
    val instructionEn: String,
    val isArrived: Boolean,
    val isBelowHorizon: Boolean
)

object FinderEngine {

    fun calculateFinderData(
        target: CelestialObject,
        phoneAzimuthDeg: Double,
        phoneAltitudeDeg: Double,
        userLat: Double,
        userLon: Double,
        jd: Double = TimeEngine.getJulianDate()
    ): FinderData {
        val lastDeg = TimeEngine.getLAST(jd, userLon)
        val horiz = CoordinateEngine.equatorialToHorizontal(
            CoordinateEngine.Equatorial(target.raDeg, target.decDeg),
            lastDeg,
            userLat
        )

        val targetAz = horiz.azimuthDeg
        val targetAlt = horiz.altitudeDeg

        var dAz = (targetAz - phoneAzimuthDeg).toFloat()
        while (dAz > 180f) dAz -= 360f
        while (dAz < -180f) dAz += 360f

        val dAlt = (targetAlt - phoneAltitudeDeg).toFloat()
        val totalDist = sqrt(dAz * dAz + dAlt * dAlt)

        // Screen angle: 0 points UP, +PI/2 points RIGHT, etc.
        // Screen X is right (+dAz), Screen Y is up (+dAlt)
        val arrowAngleRad = atan2(dAz, dAlt)

        val isBelowHorizon = targetAlt < 0.0
        val isArrived = totalDist <= 2.0f
        val proximityFraction = (1.0f - (totalDist / 60.0f)).coerceIn(0.0f, 1.0f)

        val (instFa, instEn) = when {
            isBelowHorizon -> {
                "⚠️ این جرم در حال حاضر زیر افق قرار دارد (${String.format("%.1f", targetAlt)}°)" to
                        "⚠️ Target is currently below horizon (${String.format("%.1f", targetAlt)}°)"
            }
            isArrived -> {
                "🎯 هدف دقیقاً در مرکز دید شماست!" to "🎯 Target acquired in center reticle!"
            }
            dAz > 15f && dAlt > 10f -> {
                "به سمت راست بچرخید و بالاتر را نگاه کنید" to "Turn Right & Pitch Up"
            }
            dAz > 15f && dAlt < -10f -> {
                "به سمت راست بچرخید و پایین‌تر را نگاه کنید" to "Turn Right & Pitch Down"
            }
            dAz > 15f -> {
                "به سمت راست بچرخید" to "Turn Right"
            }
            dAz < -15f && dAlt > 10f -> {
                "به سمت چپ بچرخید و بالاتر را نگاه کنید" to "Turn Left & Pitch Up"
            }
            dAz < -15f && dAlt < -10f -> {
                "به سمت چپ بچرخید و پایین‌تر را نگاه کنید" to "Turn Left & Pitch Down"
            }
            dAz < -15f -> {
                "به سمت چپ بچرخید" to "Turn Left"
            }
            dAlt > 10f -> {
                "مستقیماً به بالاتر نگاه کنید" to "Pitch Up"
            }
            dAlt < -10f -> {
                "مستقیماً به پایین‌تر نگاه کنید" to "Pitch Down"
            }
            else -> {
                "بسیار نزدیک شدید — مرکز هدف‌گیر را روی نقطه تنظیم کنید" to "Very close — center the reticle on target"
            }
        }

        return FinderData(
            targetObject = target,
            targetAzimuthDeg = targetAz,
            targetAltitudeDeg = targetAlt,
            deltaAzimuthDeg = dAz,
            deltaAltitudeDeg = dAlt,
            totalAngularDistanceDeg = totalDist,
            arrowAngleRad = arrowAngleRad,
            proximityFraction = proximityFraction,
            instructionFa = instFa,
            instructionEn = instEn,
            isArrived = isArrived,
            isBelowHorizon = isBelowHorizon
        )
    }
}
