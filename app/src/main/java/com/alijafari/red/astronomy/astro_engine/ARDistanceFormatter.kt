package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.domain.CalculatedAstroState
import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import java.util.Locale

/**
 * Shared distance-source selection and display formatter for AR cards/details.
 *
 * Solar-system bodies and artificial satellites use dynamic engine ranges when available.
 * Stars and deep-sky objects keep their static catalog distances, which is the scientifically
 * correct behavior on human time scales.
 */
object ARDistanceFormatter {
    const val KM_PER_AU = 149_597_870.7
    const val KM_PER_LIGHT_YEAR = 9.4607304725808e12

    fun usesDynamicRange(obj: CelestialObject): Boolean {
        return when (obj.type) {
            ObjectType.SUN,
            ObjectType.MOON,
            ObjectType.PLANET,
            ObjectType.DWARF_PLANET,
            ObjectType.SATELLITE -> true
            else -> false
        }
    }

    fun selectDistanceKm(obj: CelestialObject, calculatedState: CalculatedAstroState?): Double? {
        val dynamicKm = calculatedState?.distanceKm?.takeIf { it.isFinite() && it >= 0.0 }
        if (usesDynamicRange(obj) && dynamicKm != null) return dynamicKm

        return obj.distanceLightYears
            .takeIf { it.isFinite() && it > 0.0 }
            ?.times(KM_PER_LIGHT_YEAR)
    }

    fun formatDistance(
        obj: CelestialObject,
        isFa: Boolean,
        calculatedState: CalculatedAstroState? = null
    ): String {
        val selectedKm = selectDistanceKm(obj, calculatedState)
        return if (usesDynamicRange(obj) && selectedKm != null) {
            formatKilometers(selectedKm, isFa)
        } else {
            formatStaticLightYears(obj.distanceLightYears, isFa)
        }
    }

    fun formatKilometers(km: Double, isFa: Boolean): String {
        val text = when {
            km < 1_000_000.0 -> {
                val v = String.format(Locale.US, "%,.0f", km)
                "$v km"
            }
            km < 1_000_000_000.0 -> {
                val v = String.format(Locale.US, "%.1f", km / 1_000_000.0)
                val au = String.format(Locale.US, "%.3f", km / KM_PER_AU)
                "$v million km ($au AU)"
            }
            else -> {
                val v = String.format(Locale.US, "%.2f", km / 1_000_000_000.0)
                val au = String.format(Locale.US, "%.2f", km / KM_PER_AU)
                "$v billion km ($au AU)"
            }
        }
        return if (isFa) {
            TimeEngine.formatPersianNumbers(text)
                .replace("million", "میلیون")
                .replace("billion", "میلیارد")
                .replace("km", "کیلومتر")
                .replace("AU", "واحد نجومی")
        } else {
            text
        }
    }

    fun formatStaticLightYears(lightYears: Double, isFa: Boolean): String {
        val ly = lightYears.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
        val text = when {
            ly >= 1_000_000.0 -> String.format(Locale.US, "%.2f million light-years", ly / 1_000_000.0)
            ly >= 1000.0 -> String.format(Locale.US, "%,.0f light-years", ly)
            ly > 0.0 -> String.format(Locale.US, "%.1f light-years", ly)
            else -> "Unknown distance"
        }
        return if (isFa) {
            TimeEngine.formatPersianNumbers(text)
                .replace("million light-years", "میلیون سال نوری")
                .replace("light-years", "سال نوری")
                .replace("Unknown distance", "فاصله نامشخص")
        } else {
            text
        }
    }
}
